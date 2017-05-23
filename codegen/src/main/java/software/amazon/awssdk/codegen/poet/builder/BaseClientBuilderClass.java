/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.codegen.poet.builder;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.annotation.SdkInternalApi;
import software.amazon.awssdk.auth.Aws4Signer;
import software.amazon.awssdk.auth.QueryStringSigner;
import software.amazon.awssdk.auth.StaticSignerProvider;
import software.amazon.awssdk.client.builder.ClientBuilder;
import software.amazon.awssdk.client.builder.DefaultClientBuilder;
import software.amazon.awssdk.codegen.internal.Utils;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.service.AuthType;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.PoetUtils;
import software.amazon.awssdk.config.ClientListenerConfiguration;
import software.amazon.awssdk.config.ClientSecurityConfiguration;
import software.amazon.awssdk.config.defaults.ClientConfigurationDefaults;
import software.amazon.awssdk.handlers.HandlerChainFactory;
import software.amazon.awssdk.http.SdkHttpConfigurationOptions;
import software.amazon.awssdk.runtime.auth.SignerProvider;

public class BaseClientBuilderClass implements ClassSpec {
    private final IntermediateModel model;
    private final ClassName builderInterfaceName;
    private final ClassName builderClassName;

    public BaseClientBuilderClass(IntermediateModel model) {
        this.model = model;

        String basePackage = model.getMetadata().getFullClientPackageName();
        this.builderInterfaceName = ClassName.get(basePackage, model.getMetadata().getBaseBuilderInterface());
        this.builderClassName = ClassName.get(basePackage, model.getMetadata().getBaseBuilder());
    }

    @Override
    public TypeSpec poetSpec() {
        final TypeSpec.Builder builder =
                PoetUtils.createClassBuilder(builderClassName)
                         .addModifiers(Modifier.ABSTRACT)
                         .addAnnotation(SdkInternalApi.class)
                         .addTypeVariable(PoetUtils.createBoundedTypeVariableName("B", builderInterfaceName, "B", "C"))
                         .addTypeVariable(TypeVariableName.get("C"))
                         .superclass(PoetUtils.createParameterizedTypeName(DefaultClientBuilder.class, "B", "C"))
                         .addSuperinterface(PoetUtils.createParameterizedTypeName(ClientBuilder.class, "B", "C"))
                         .addMethod(getServiceEndpointPrefixMethod())
                         .addMethod(applyServiceDefaultsMethod());
        if (model.getCustomizationConfig().getServiceSpecificHttpConfig() != null) {
            builder.addMethod(applyServiceSpecificHttpConfigMethod());
        }
        return builder.build();
    }

    private MethodSpec getServiceEndpointPrefixMethod() {
        return MethodSpec.methodBuilder("serviceEndpointPrefix")
                         .addAnnotation(Override.class)
                         .addModifiers(Modifier.PROTECTED, Modifier.FINAL)
                         .returns(String.class)
                         .addCode("return \"$L\";", model.getMetadata().getEndpointPrefix())
                         .build();
    }

    private MethodSpec applyServiceDefaultsMethod() {
        return MethodSpec.methodBuilder("serviceDefaults")
                         .addAnnotation(Override.class)
                         .addModifiers(Modifier.PROTECTED, Modifier.FINAL)
                         .returns(ClientConfigurationDefaults.class)
                         .addCode("return new $T() {\n", ClientConfigurationDefaults.class)
                         .addCode(applySecurityDefaultsMethod())
                         .addCode(applyListenerDefaultsMethod())
                         .addCode("};")
                         .build();
    }

    private MethodSpec applyServiceSpecificHttpConfigMethod() {
        return MethodSpec.methodBuilder("serviceSpecificHttpConfig")
                         .addAnnotation(Override.class)
                         .addModifiers(Modifier.PROTECTED, Modifier.FINAL)
                         .returns(SdkHttpConfigurationOptions.class)
                         .addCode("return $L;", model.getCustomizationConfig().getServiceSpecificHttpConfig())
                         .build();
    }

    private CodeBlock applySecurityDefaultsMethod() {
        return CodeBlock.builder()
                        .add("@Override\n" +
                             "protected void applySecurityDefaults($T builder) {\n" +
                             "    builder.signerProvider(builder.signerProvider().orElseGet(this::defaultSignerProvider));\n" +
                             "}\n",
                             ClientSecurityConfiguration.Builder.class)
                        .add("private $T defaultSignerProvider() {\n", SignerProvider.class)
                        .add(signerDefinitionMethodBody())
                        .add("}\n")
                        .build();
    }

    private CodeBlock signerDefinitionMethodBody() {
        AuthType authType = model.getMetadata().getAuthType();
        switch (authType) {
            case V4:
                return v4SignerDefinitionMethodBody();
            case V2:
                return v2SignerDefinitionMethodBody();
            default:
                throw new UnsupportedOperationException("Unsupported signer type: " + authType);
        }
    }

    private CodeBlock v4SignerDefinitionMethodBody() {
        return CodeBlock.of("$T signer = new $T();\n" +
                            "signer.setServiceName(\"$L\");\n" +
                            "signer.setRegionName(signingRegion());\n" +
                            "return new $T(signer);\n",
                            Aws4Signer.class,
                            Aws4Signer.class,
                            model.getMetadata().getSigningName(),
                            StaticSignerProvider.class);
    }

    private CodeBlock v2SignerDefinitionMethodBody() {
        return CodeBlock.of("return new $T(new $T());\n",
                            StaticSignerProvider.class,
                            QueryStringSigner.class);
    }

    private CodeBlock applyListenerDefaultsMethod() {
        String requestHandlerDirectory = Utils.packageToDirectory(model.getMetadata().getFullClientPackageName());
        String requestHandlerPath = String.format("/%s/request.handlers", requestHandlerDirectory);
        String requestHandler2Path = String.format("/%s/request.handler2s", requestHandlerDirectory);
        return CodeBlock.builder()
                        .add("@Override\n" +
                             "protected void applyListenerDefaults($T builder) {\n", ClientListenerConfiguration.Builder.class)
                        .addStatement("$1T chainFactory = new $1T()", HandlerChainFactory.class)
                        .addStatement("chainFactory.newRequestHandlerChain($S).forEach(builder::addRequestListener)",
                                      requestHandlerPath)
                        .addStatement("chainFactory.newRequestHandler2Chain($S).forEach(builder::addRequestListener)",
                                      requestHandler2Path)
                        .add("}\n")
                        .build();
    }

    @Override
    public ClassName className() {
        return builderClassName;
    }
}
