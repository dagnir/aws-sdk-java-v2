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

package software.amazon.awssdk.codegen.poet.client;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeSpec.Builder;
import java.util.List;
import java.util.stream.Collectors;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.auth.presign.PresignerParams;
import software.amazon.awssdk.client.AwsSyncClientParams;
import software.amazon.awssdk.client.ClientHandler;
import software.amazon.awssdk.client.ClientHandlerParams;
import software.amazon.awssdk.codegen.emitters.GeneratorTaskParams;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.codegen.model.intermediate.Protocol;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.PoetExtensions;
import software.amazon.awssdk.codegen.poet.PoetUtils;
import software.amazon.awssdk.codegen.poet.client.specs.ApiGatewayProtocolSpec;
import software.amazon.awssdk.codegen.poet.client.specs.Ec2ProtocolSpec;
import software.amazon.awssdk.codegen.poet.client.specs.JsonProtocolSpec;
import software.amazon.awssdk.codegen.poet.client.specs.ProtocolSpec;
import software.amazon.awssdk.codegen.poet.client.specs.QueryXmlProtocolSpec;

public class SyncClientClass implements ClassSpec {

    private final IntermediateModel model;
    private final String basePackage;
    private final PoetExtensions poetExtensions;
    private final ClassName className;
    private final ProtocolSpec protocolSpec;

    public SyncClientClass(GeneratorTaskParams taskParams) {
        this.model = taskParams.getModel();
        this.basePackage = model.getMetadata().getFullClientPackageName();
        this.poetExtensions = taskParams.getPoetExtensions();
        this.className = poetExtensions.getClientClass(model.getMetadata().getSyncClient());
        this.protocolSpec = getProtocolSpecs(model.getMetadata().getProtocol());
    }

    @Override
    public TypeSpec poetSpec() {
        ClassName interfaceClass = poetExtensions.getClientClass(model.getMetadata().getSyncInterface());

        Builder classBuilder = PoetUtils.createClassBuilder(className)
                .addSuperinterface(interfaceClass)
                .addField(ClientHandler.class, "clientHandler", Modifier.PRIVATE, Modifier.FINAL)
                .addField(protocolSpec.protocolFactory(model))
                .addField(clientParamsField());

        if (model.getCustomizationConfig().getServiceSpecificClientConfigClass() != null) {
            ClassName advancedConfiguration = ClassName.get(basePackage,
                model.getCustomizationConfig().getServiceSpecificClientConfigClass());
            classBuilder.addField(advancedConfiguration, "advancedConfiguration", Modifier.PRIVATE, Modifier.FINAL);
            classBuilder.addMethod(constructor());
            classBuilder.addMethod(constructorWithAdvancedConfiguration());
        } else {
            classBuilder.addMethod(constructor());
        }

        classBuilder.addMethods(operations());

        protocolSpec.createErrorResponseHandler().ifPresent(classBuilder::addMethod);

        classBuilder.addMethod(protocolSpec.initProtocolFactory(model));

        if (model.getHasWaiters()) {
            ClassName waiters = poetExtensions.getWaiterClass(model.getMetadata().getSyncInterface() + "Waiters");

            classBuilder.addField(FieldSpec.builder(waiters, "waiters")
                    .addModifiers(Modifier.PRIVATE, Modifier.VOLATILE)
                    .build());
            classBuilder.addMethod(waiters());
        }
        if (model.getCustomizationConfig().getPresignersFqcn() != null) {
            classBuilder.addMethod(presigners());
        }

        classBuilder.addMethod(shutdown());

        classBuilder.addMethods(protocolSpec.additionalMethods());

        return classBuilder.build();
    }

    private FieldSpec clientParamsField() {
        return FieldSpec.builder(AwsSyncClientParams.class, "clientParams")
                .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                .build();
    }

    @Override
    public ClassName className() {
        return className;
    }

    private MethodSpec constructor() {
        if (model.getCustomizationConfig().getServiceSpecificClientConfigClass() != null) {
            return MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PROTECTED)
                .addParameter(AwsSyncClientParams.class, "clientParams")
                .addStatement(
                        "this($N, null)",
                        "clientParams")
                .build();
        }

        return MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PROTECTED)
                .addParameter(AwsSyncClientParams.class, "clientParams")
                .addStatement(
                        "this.$N = new $T(new $T().withClientParams($N).withCalculateCrc32FromCompressedDataEnabled($L))",
                        "clientHandler",
                        protocolSpec.getClientHandlerClass(),
                        ClientHandlerParams.class,
                        "clientParams",
                        model.getCustomizationConfig().isCalculateCrc32FromCompressedData())
                .addStatement("this.clientParams = clientParams")
                .addStatement("this.$N = init()", protocolSpec.protocolFactory(model).name)
                .build();
    }

    private MethodSpec constructorWithAdvancedConfiguration() {
        ClassName advancedConfiguration = ClassName.get(basePackage,
                model.getCustomizationConfig().getServiceSpecificClientConfigClass());
        return MethodSpec.constructorBuilder()
                         .addModifiers(Modifier.PROTECTED)
                         .addParameter(AwsSyncClientParams.class, "clientParams")
                         .addParameter(advancedConfiguration, "advancedConfiguration")
                         .addStatement(
                                 "this.$N = new $T(new $T().withClientParams($N).withServiceAdvancedConfiguration($N)" +
                                 ".withCalculateCrc32FromCompressedDataEnabled($L))",
                                 "clientHandler",
                                 protocolSpec.getClientHandlerClass(),
                                 ClientHandlerParams.class,
                                 "clientParams",
                                 "advancedConfiguration",
                                 model.getCustomizationConfig().isCalculateCrc32FromCompressedData())
                         .addStatement("this.clientParams = clientParams")
                         .addStatement("this.advancedConfiguration = advancedConfiguration")
                         .addStatement("this.$N = init()", protocolSpec.protocolFactory(model).name)
                         .build();
    }

    private List<MethodSpec> operations() {
        return model.getOperations().values().stream().map(this::operationMethodSpec).collect(Collectors.toList());
    }

    private MethodSpec operationMethodSpec(OperationModel opModel) {
        ClassName returnType = poetExtensions.getModelClass(opModel.getReturnType().getReturnType());
        ClassName requestType = poetExtensions.getModelClass(opModel.getInput().getVariableType());
        return MethodSpec.methodBuilder(opModel.getMethodName())
                .returns(returnType)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .addParameter(requestType, opModel.getInput().getVariableName())
                .addCode(protocolSpec.responseHandler(opModel))
                .addCode(protocolSpec.errorResponseHandler(opModel))
                .addCode(protocolSpec.executionHandler(opModel, model))
                .build();
    }

    private MethodSpec waiters() {
        ClassName waiters = poetExtensions.getWaiterClass(model.getMetadata().getSyncInterface() + "Waiters");
        return MethodSpec.methodBuilder("waiters")
                .returns(waiters)
                .addModifiers(Modifier.PUBLIC)
                .beginControlFlow("if ($N == null)", "waiters")
                .beginControlFlow("synchronized(this)")
                .beginControlFlow("if ($N == null)", "waiters")
                .addStatement("waiters = new $T(this)", waiters)
                .endControlFlow()
                .endControlFlow()
                .endControlFlow()
                .addStatement("return $N", "waiters")
                .build();
    }

    private MethodSpec presigners() {
        ClassName presigners = PoetUtils.classNameFromFqcn(model.getCustomizationConfig().getPresignersFqcn());
        return MethodSpec.methodBuilder("presigners")
                .returns(presigners)
                .addModifiers(Modifier.PUBLIC)
                .addStatement("return new $T($T.builder()" +
                              ".endpoint(clientParams.getEndpoint())" +
                              ".credentialsProvider(clientParams.getCredentialsProvider())" +
                              ".signerProvider(clientParams.getSignerProvider())" +
                              ".build())",
                              presigners,
                              PresignerParams.class)
                .build();
    }

    private MethodSpec shutdown() {
        return MethodSpec.methodBuilder("close")
                .addAnnotation(Override.class)
                .addStatement("clientHandler.close()")
                .addModifiers(Modifier.PUBLIC)
                .addException(Exception.class)
                .build();
    }

    private ProtocolSpec getProtocolSpecs(Protocol protocol) {
        switch (protocol) {
            case QUERY:
            case REST_XML:
                return new QueryXmlProtocolSpec(poetExtensions);
            case EC2:
                return new Ec2ProtocolSpec(poetExtensions);
            case AWS_JSON:
            case REST_JSON:
            case CBOR:
            case ION:
                return new JsonProtocolSpec(poetExtensions);
            case API_GATEWAY:
                return new ApiGatewayProtocolSpec(poetExtensions);
            default:
                throw new RuntimeException("Unknown protocol: " + protocol.name());
        }
    }
}
