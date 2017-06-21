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

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.AmazonServiceException;
import software.amazon.awssdk.AmazonWebServiceResponse;
import software.amazon.awssdk.client.ClientExecutionParams;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeType;
import software.amazon.awssdk.codegen.poet.PoetExtensions;
import software.amazon.awssdk.http.HttpResponseHandler;
import software.amazon.awssdk.protocol.json.JsonClientMetadata;
import software.amazon.awssdk.protocol.json.JsonErrorResponseMetadata;
import software.amazon.awssdk.protocol.json.JsonErrorShapeMetadata;
import software.amazon.awssdk.protocol.json.JsonOperationMetadata;
import software.amazon.awssdk.protocol.json.SdkJsonProtocolFactory;

public class JsonProtocolSpec implements ProtocolSpec {

    private final PoetExtensions poetExtensions;

    public JsonProtocolSpec(PoetExtensions poetExtensions) {
        this.poetExtensions = poetExtensions;
    }

    @Override
    public FieldSpec protocolFactory(IntermediateModel model) {
        return FieldSpec.builder(SdkJsonProtocolFactory.class, "protocolFactory")
                .addModifiers(Modifier.PRIVATE, Modifier.FINAL).build();
    }

    @Override
    public MethodSpec initProtocolFactory(IntermediateModel model) {
        String exceptionPath = model.getSdkModeledExceptionBaseFqcn()
                .substring(0, model.getSdkModeledExceptionBaseFqcn().lastIndexOf("."));

        ClassName baseException = ClassName.get(exceptionPath, model.getSdkModeledExceptionBaseClassName());

        ClassName protocolFactory = poetExtensions.getClientClass(model.getMetadata().getProtocolFactory());

        MethodSpec.Builder methodSpec = MethodSpec.methodBuilder("init")
                .returns(protocolFactory)
                .addModifiers(Modifier.PRIVATE)
                .addCode("return new $T(new $T().withProtocolVersion($S).withSupportsCbor($L).withSupportsIon($L)" +
                                ".withBaseServiceExceptionClass($L.class)",
                        SdkJsonProtocolFactory.class,
                        JsonClientMetadata.class,
                        model.getMetadata().getJsonVersion(),
                        model.getMetadata().isCborProtocol(),
                        model.getMetadata().isIonProtocol(), baseException);

        if (model.getMetadata().getContentType() != null) {
            methodSpec.addCode(".withContentTypeOverride($S)", model.getMetadata().getContentType());
        }

        errorUnmarshallers(model).forEach(methodSpec::addCode);

        methodSpec.addCode(");");

        return methodSpec.build();
    }

    @Override
    public CodeBlock responseHandler(OperationModel opModel) {
        boolean isStreamingBody = opModel.getOutputShape() != null && opModel.getOutputShape().isHasStreamingMember();
        ClassName unmarshaller = poetExtensions.getTransformClass(opModel.getReturnType().getReturnType() + "Unmarshaller");
        ClassName returnType = poetExtensions.getModelClass(opModel.getReturnType().getReturnType());

        return CodeBlock
                .builder()
                .add("\n\n$T<$T<$T>> responseHandler = $L.createResponseHandler(new $T().withPayloadJson($L)" +
                        ".withHasStreamingSuccessResponse($L), new $T());",
                HttpResponseHandler.class,
                AmazonWebServiceResponse.class,
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
                        HttpResponseHandler.class, AmazonServiceException.class)
                .build();
    }

    @Override
    public CodeBlock executionHandler(OperationModel opModel) {
        ClassName returnType = poetExtensions.getModelClass(opModel.getReturnType().getReturnType());
        ClassName requestType = poetExtensions.getModelClass(opModel.getInput().getVariableType());
        ClassName marshaller = poetExtensions.getRequestTransformClass(opModel.getInputShape().getShapeName() + "Marshaller");

        return CodeBlock.builder().add("\n\nreturn clientHandler.execute(new $T<$T, $T<$T>>().withMarshaller(new $T($N))" +
                ".withResponseHandler($N).withErrorResponseHandler($N).withInput($L)).getResult();",
                ClientExecutionParams.class,
                requestType,
                AmazonWebServiceResponse.class,
                returnType,
                marshaller,
                "protocolFactory",
                "responseHandler",
                "errorResponseHandler",
                opModel.getInput().getVariableName())
                .build();
    }

    @Override
    public Optional<MethodSpec> createErrorResponseHandler() {
        ClassName httpResponseHandler = ClassName.get(HttpResponseHandler.class);
        ClassName sdkBaseException = ClassName.get(AmazonServiceException.class);
        TypeName responseHandlerOfException = ParameterizedTypeName.get(httpResponseHandler, sdkBaseException);

        return Optional.of(MethodSpec.methodBuilder("createErrorResponseHandler")
                .returns(responseHandlerOfException)
                .addModifiers(Modifier.PRIVATE)
                .addStatement("return protocolFactory.createErrorResponseHandler(new $T())", JsonErrorResponseMetadata.class)
                .build());
    }

    @Override
    public List<CodeBlock> errorUnmarshallers(IntermediateModel model) {
        List<ShapeModel> exceptions = model.getShapes().values().stream()
                .filter(s -> s.getShapeType().equals(ShapeType.Exception))
                .collect(Collectors.toList());

        return exceptions.stream().map(s -> {
            ClassName exceptionClass = poetExtensions.getModelClass(s.getShapeName());
            return CodeBlock.builder().add(".addErrorMetadata(new $T().withErrorCode($S).withModeledClass($T.class))",
                    JsonErrorShapeMetadata.class,
                    s.getErrorCode(),
                    exceptionClass)
                    .build();
        }).collect(Collectors.toList());
    }
}
