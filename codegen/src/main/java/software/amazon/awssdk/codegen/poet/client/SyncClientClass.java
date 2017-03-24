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
import software.amazon.awssdk.client.SdkClientHandler;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.codegen.model.intermediate.Protocol;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.PoetUtils;
import software.amazon.awssdk.codegen.poet.client.specs.ApiGatewayProtocolSpec;
import software.amazon.awssdk.codegen.poet.client.specs.Ec2ProtocolSpec;
import software.amazon.awssdk.codegen.poet.client.specs.JsonProtocolSpec;
import software.amazon.awssdk.codegen.poet.client.specs.ProtocolSpec;
import software.amazon.awssdk.codegen.poet.client.specs.QueryXmlProtocolSpec;

public class SyncClientClass implements ClassSpec {

    private final IntermediateModel model;
    private final ClassName className;
    private final String basePackage;
    private final ProtocolSpec protocolSpec;

    public SyncClientClass(IntermediateModel model) {
        this.basePackage = model.getMetadata().getPackageName();
        this.model = model;
        this.className = ClassName.get(basePackage, model.getMetadata().getSyncClient());
        this.protocolSpec = getProtocolSpecs(model.getMetadata().getProtocol());
    }

    @Override
    public TypeSpec poetSpec() {
        ClassName interfaceClass = ClassName.get(basePackage, model.getMetadata().getSyncInterface());

        Builder classBuilder = PoetUtils.createClassBuilder(className)
                .addSuperinterface(interfaceClass)
                .addField(FieldSpec.builder(ClientHandler.class, "clientHandler")
                        .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                        .build())
                .addField(protocolSpec.protocolFactory(model))
                .addMethod(constructor())
                .addMethods(operations());

        protocolSpec.createErrorResponseHandler().ifPresent(classBuilder::addMethod);

        classBuilder.addMethod(protocolSpec.initProtocolFactory(model));

        if (model.getHasWaiters()) {
            ClassName waiters = ClassName.get(basePackage + ".waiters", model.getMetadata().getSyncInterface() + "Waiters");

            classBuilder.addField(FieldSpec.builder(waiters, "waiters")
                    .addModifiers(Modifier.PRIVATE, Modifier.VOLATILE)
                    .build());
            classBuilder.addMethod(waiters());
        }

        classBuilder.addMethod(shutdown());

        classBuilder.addMethods(protocolSpec.additionalMethods());

        return classBuilder.build();
    }

    @Override
    public ClassName className() {
        return className;
    }

    private MethodSpec constructor() {
        return MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PROTECTED)
                .addParameter(AwsSyncClientParams.class, "clientParams")
                .addStatement(
                        "this.$N = new $T(new $T().withClientParams($N))",
                        "clientHandler",
                        protocolSpec.getClientHandlerClass(),
                        ClientHandlerParams.class,
                        "clientParams")
                .addStatement("this.$N = init()", protocolSpec.protocolFactory(model).name)
                .build();
    }

    private List<MethodSpec> operations() {
        return model.getOperations().values().stream().map(this::operationMethodSpec).collect(Collectors.toList());
    }

    private MethodSpec operationMethodSpec(OperationModel opModel) {
        ClassName returnType = PoetUtils.getModelClass(basePackage, opModel.getReturnType().getReturnType());
        ClassName requestType = PoetUtils.getModelClass(basePackage, opModel.getInput().getVariableType());
        return MethodSpec.methodBuilder(opModel.getMethodName())
                .returns(returnType)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .addParameter(requestType, opModel.getInput().getVariableName())
                .addCode(protocolSpec.responseHandler(opModel))
                .addCode(protocolSpec.errorResponseHandler(opModel))
                .addCode(protocolSpec.executionHandler(opModel))
                .build();
    }

    private MethodSpec waiters() {
        ClassName waiters = ClassName.get(basePackage + ".waiters", model.getMetadata().getSyncInterface() + "Waiters");
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
        ClassName presigners = ClassName.get(basePackage + "presign", model.getMetadata().getSyncInterface() + "Presigners");
        return MethodSpec.methodBuilder("presigners")
                .returns(presigners)
                .addStatement("return new $T($T.builder().endpoint($L).credentialsProvider($L).signerProvier($L()).build())",
                        presigners,
                        PresignerParams.class,
                        "endpoint",
                        "awsCredentialsProvider",
                        "getSignerProvider")
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
                return new QueryXmlProtocolSpec(basePackage);
            case EC2:
                return new Ec2ProtocolSpec(basePackage);
            case AWS_JSON:
            case REST_JSON:
            case CBOR:
            case ION:
                return new JsonProtocolSpec(basePackage);
            case API_GATEWAY:
                return new ApiGatewayProtocolSpec(basePackage);
            default:
                throw new RuntimeException("Unknown protocol: " + protocol.name());
        }
    }
}
