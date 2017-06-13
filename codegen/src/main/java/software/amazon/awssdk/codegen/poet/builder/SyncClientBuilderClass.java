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
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.annotation.SdkInternalApi;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.PoetUtils;
import software.amazon.awssdk.config.ImmutableClientConfiguration;
import software.amazon.awssdk.config.ImmutableSyncClientConfiguration;

public class SyncClientBuilderClass implements ClassSpec {
    private final String basePackage;
    private final IntermediateModel model;
    private final ClassName clientInterfaceName;
    private final ClassName clientClassName;
    private final ClassName builderInterfaceName;
    private final ClassName builderClassName;
    private final ClassName builderBaseClassName;

    public SyncClientBuilderClass(IntermediateModel model) {
        this.model = model;
        this.basePackage = model.getMetadata().getFullClientPackageName();
        this.clientInterfaceName = ClassName.get(basePackage, model.getMetadata().getSyncInterface());
        this.clientClassName = ClassName.get(basePackage, model.getMetadata().getSyncClient());
        this.builderInterfaceName = ClassName.get(basePackage, model.getMetadata().getSyncBuilderInterface());
        this.builderClassName = ClassName.get(basePackage, model.getMetadata().getSyncBuilder());
        this.builderBaseClassName = ClassName.get(basePackage, model.getMetadata().getBaseBuilder());
    }

    @Override
    public TypeSpec poetSpec() {
        TypeSpec.Builder builder =
                PoetUtils.createClassBuilder(builderClassName)
                         .addAnnotation(SdkInternalApi.class)
                         .addModifiers(Modifier.FINAL)
                         .superclass(ParameterizedTypeName.get(builderBaseClassName, builderInterfaceName, clientInterfaceName))
                         .addSuperinterface(builderInterfaceName)
                         .addMethod(buildClientMethod());

        if (model.getCustomizationConfig().getServiceSpecificClientConfigClass() != null) {
            builder.addMethod(buildServiceClientMethod());
        }

        return builder.build();
    }

    private MethodSpec buildClientMethod() {
        if (model.getCustomizationConfig().getServiceSpecificClientConfigClass() != null) {
            return MethodSpec.methodBuilder("buildClient")
                             .addAnnotation(Override.class)
                             .addModifiers(Modifier.PROTECTED, Modifier.FINAL)
                             .returns(clientInterfaceName)
                             .addCode("return buildServiceClient(super.syncClientConfiguration(), advancedConfiguration());")
                             .build();
        }

        return MethodSpec.methodBuilder("buildClient")
                             .addAnnotation(Override.class)
                             .addModifiers(Modifier.PROTECTED, Modifier.FINAL)
                             .returns(clientInterfaceName)
                             .addCode("return new $T(super.syncClientConfiguration().asLegacySyncClientParams());",
                                      clientClassName)
                             .build();
    }

    private MethodSpec buildServiceClientMethod() {
        ClassName advancedConfiguration = ClassName.get(basePackage,
                model.getCustomizationConfig().getServiceSpecificClientConfigClass());
        return MethodSpec.methodBuilder("buildServiceClient")
                         .addAnnotation(Override.class)
                         .addModifiers(Modifier.PROTECTED, Modifier.FINAL)
                         .returns(clientInterfaceName)
                         .addParameter(ImmutableClientConfiguration.class, "clientConfiguration")
                         .addParameter(advancedConfiguration, "advancedConfiguration")
                         .addStatement("$T syncClientConfiguration = ($T) clientConfiguration",
                                 ImmutableSyncClientConfiguration.class, ImmutableSyncClientConfiguration.class)
                         .addStatement("return new $T(syncClientConfiguration().asLegacySyncClientParams(), " +
                                         "advancedConfiguration)", clientClassName)
                         .build();
    }

    @Override
    public ClassName className() {
        return builderClassName;
    }
}
