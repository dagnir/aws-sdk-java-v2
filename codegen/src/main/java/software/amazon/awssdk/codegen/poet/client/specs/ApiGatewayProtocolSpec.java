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

package software.amazon.awssdk.codegen.poet.client.specs;

import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import java.util.Arrays;
import java.util.Optional;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.SdkBaseException;
import software.amazon.awssdk.client.ClientExecutionParams;
import software.amazon.awssdk.client.ClientHandler;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.codegen.poet.PoetUtils;
import software.amazon.awssdk.http.HttpResponseHandler;
import software.amazon.awssdk.opensdk.protect.client.SdkClientHandler;
import software.amazon.awssdk.protocol.json.JsonClientMetadata;
import software.amazon.awssdk.protocol.json.JsonErrorResponseMetadata;
import software.amazon.awssdk.protocol.json.JsonErrorShapeMetadata;
import software.amazon.awssdk.protocol.json.JsonOperationMetadata;

public class ApiGatewayProtocolSpec extends JsonProtocolSpec {

    public ApiGatewayProtocolSpec(String basePackage) {
        super(basePackage);
    }

    @Override
    public FieldSpec protocolFactory(IntermediateModel model) {
        ClassName protocolFactory = ClassName.get(basePackage, model.getMetadata().getProtocolFactory());
        return FieldSpec.builder(protocolFactory, "protocolFactory")
                .addModifiers(Modifier.PRIVATE, Modifier.FINAL).build();
    }

    @Override
    public MethodSpec initProtocolFactory(IntermediateModel model) {
        String exceptionPath = model.getSdkModeledExceptionBaseFqcn()
                .substring(0, model.getSdkModeledExceptionBaseFqcn().lastIndexOf("."));

        ClassName baseException = ClassName.get(exceptionPath, model.getSdkModeledExceptionBaseClassName());

        ClassName protocolFactory = ClassName.get(basePackage, model.getMetadata().getProtocolFactory());

        MethodSpec.Builder methodSpec = MethodSpec.methodBuilder("init")
                .returns(protocolFactory)
                .addModifiers(Modifier.PRIVATE)
                .addCode("return new $T(new $T().withProtocolVersion($S).withSupportsCbor($L).withSupportsIon($L)" +
                                ".withBaseServiceExceptionClass($L.class)",
                        protocolFactory,
                        JsonClientMetadata.class,
                        model.getMetadata().getJsonVersion(),
                        model.getMetadata().isCborProtocol(),
                        model.getMetadata().isIonProtocol(), baseException);

        if (model.getMetadata().getContentType() != null) {
            methodSpec.addCode(".withContentTypeOverride($S)", model.getMetadata().getContentType());
        }

        methodSpec.addCode(");");

        return methodSpec.build();
    }

    @Override
    public CodeBlock responseHandler(OperationModel opModel) {
        boolean isStreamingBody = opModel.getOutputShape() != null && opModel.getOutputShape().isHasStreamingMember();
        ClassName unmarshaller = PoetUtils.getTransformClass(
                basePackage, opModel.getReturnType().getReturnType() + "Unmarshaller");
        ClassName returnType = PoetUtils.getModelClass(basePackage, opModel.getReturnType().getReturnType());

        return CodeBlock
                .builder()
                .add("\n\n$T<$T> responseHandler = $N.createResponseHandler(new $T().withPayloadJson($L)" +
                                ".withHasStreamingSuccessResponse($L), new $T());",
                        HttpResponseHandler.class,
                        returnType,
                        "protocolFactory",
                        JsonOperationMetadata.class,
                        !opModel.getHasBlobMemberAsPayload(),
                        isStreamingBody,
                        unmarshaller)
                .build();
    }

    @Override
    public CodeBlock errorResponseHandler(OperationModel opModel) {
        return CodeBlock
                .builder()
                .add("\n\n$T<$T> errorResponseHandler = createErrorResponseHandler();",
                        HttpResponseHandler.class, SdkBaseException.class)
                .build();
    }

    @Override
    public CodeBlock executionHandler(OperationModel opModel) {
        ClassName returnType = PoetUtils.getModelClass(basePackage, opModel.getReturnType().getReturnType());
        ClassName requestType = PoetUtils.getModelClass(basePackage, opModel.getInput().getVariableType());
        ClassName marshaller = PoetUtils.getTransformClass(
                basePackage, opModel.getInputShape().getShapeName() + "Marshaller");

        return CodeBlock.builder().add("\n\nreturn clientHandler.execute(new $T<$T, $T>().withMarshaller(new $T($N))" +
                ".withResponseHandler($N).withErrorResponseHandler($N).withInput($L));",
                ClientExecutionParams.class,
                requestType,
                returnType,
                marshaller,
                "protocolFactory",
                "responseHandler",
                "errorResponseHandler",
                opModel.getInput().getVariableName())
                .build();
    }

    @Override
    public Class<? extends ClientHandler> getClientHandlerClass() {
        return SdkClientHandler.class;
    }

    @Override
    public Optional<MethodSpec> createErrorResponseHandler() {
        ClassName httpResponseHandler = ClassName.get(HttpResponseHandler.class);
        ClassName sdkBaseException = ClassName.get(SdkBaseException.class);
        TypeName responseHandlerOfException = ParameterizedTypeName.get(httpResponseHandler, sdkBaseException);
        TypeName errorMetadataArray = ArrayTypeName.get(JsonErrorShapeMetadata[].class);

        return Optional.of(MethodSpec.methodBuilder("createErrorResponseHandler")
                .returns(responseHandlerOfException)
                .addParameter(errorMetadataArray, "errorShapeMetadata")
                .varargs()
                .addModifiers(Modifier.PRIVATE)
                .addStatement("return protocolFactory.createErrorResponseHandler(" +
                        "new $T().withErrorShapes($T.asList(errorShapeMetadata)))",
                        JsonErrorResponseMetadata.class,
                        Arrays.class)
                .build());
    }
}
