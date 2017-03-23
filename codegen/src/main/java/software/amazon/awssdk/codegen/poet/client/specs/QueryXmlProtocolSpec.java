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
import com.squareup.javapoet.TypeSpec;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.lang.model.element.Modifier;

import org.w3c.dom.Node;

import software.amazon.awssdk.AmazonServiceException;
import software.amazon.awssdk.AmazonWebServiceResponse;
import software.amazon.awssdk.client.ClientExecutionParams;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.poet.PoetUtils;
import software.amazon.awssdk.http.DefaultErrorResponseHandler;
import software.amazon.awssdk.http.StaxResponseHandler;
import software.amazon.awssdk.runtime.transform.Unmarshaller;

public class QueryXmlProtocolSpec implements ProtocolSpec {
    protected final String basePackage;

    public QueryXmlProtocolSpec(String basePackage) {
        this.basePackage = basePackage;
    }

    @Override
    public FieldSpec protocolFactory(IntermediateModel model) {
        TypeName unmarshaller = ParameterizedTypeName.get(Unmarshaller.class, AmazonServiceException.class, Node.class);
        TypeName listOfUnmarshallers = ParameterizedTypeName.get(ClassName.get("java.util", "List"), unmarshaller);
        return FieldSpec.builder(listOfUnmarshallers, "exceptionUnmarshallers")
                .addModifiers(Modifier.PRIVATE)
                .initializer("new $T<$T>()", ArrayList.class, unmarshaller)
                .build();
    }

    @Override
    public MethodSpec initProtocolFactory(IntermediateModel model) {
        MethodSpec.Builder methodSpec = MethodSpec.methodBuilder("init")
                .returns(List.class)
                .addModifiers(Modifier.PRIVATE);

        methodSpec.addStatement("$T unmarshallers = new $T()", List.class, ArrayList.class);
        errorUnmarshallers(model).forEach(methodSpec::addCode);
        methodSpec.addStatement("return $N", "unmarshallers");

        return methodSpec.build();
    }

    @Override
    public CodeBlock responseHandler(OperationModel opModel) {
        ClassName unmarshaller = PoetUtils.getTransformClass(
                basePackage, opModel.getReturnType().getReturnType() + "Unmarshaller");
        ClassName returnType = PoetUtils.getModelClass(basePackage, opModel.getReturnType().getReturnType());

        return CodeBlock.builder().add("\n\n$T<$T> responseHandler = new $T<$T>(new $T());",
                StaxResponseHandler.class,
                returnType,
                StaxResponseHandler.class,
                returnType,
                unmarshaller)
                .build();
    }

    @Override
    public CodeBlock errorResponseHandler(OperationModel opModel) {
        return CodeBlock.builder().add("\n\n$T errorResponseHandler = new $T($N);",
                DefaultErrorResponseHandler.class,
                DefaultErrorResponseHandler.class,
                "exceptionUnmarshallers")
                .build();
    }

    @Override
    public CodeBlock executionHandler(OperationModel opModel) {
        ClassName returnType = PoetUtils.getModelClass(basePackage, opModel.getReturnType().getReturnType());
        ClassName requestType = PoetUtils.getModelClass(basePackage, opModel.getInput().getVariableType());
        ClassName marshaller = PoetUtils.getTransformClass(basePackage, opModel.getInputShape().getShapeName() + "Marshaller");
        return CodeBlock.builder().add("\n\nreturn clientHandler.execute(new $T<$T, $T<$T>>()" +
                ".withMarshaller(new $T()).withResponseHandler($N).withErrorResponseHandler($N).withInput($L)).getResult();",
                ClientExecutionParams.class,
                requestType,
                AmazonWebServiceResponse.class,
                returnType,
                marshaller,
                "responseHandler",
                "errorResponseHandler",
                opModel.getInput().getVariableName())
                .build();
    }

    @Override
    public Optional<MethodSpec> createErrorResponseHandler() {
        return Optional.empty();
    }

    @Override
    public List<CodeBlock> errorUnmarshallers(IntermediateModel model) {
        List<ShapeModel> exceptions = model.getShapes()
                .values()
                .stream()
                .filter(s -> s.getType().equals("Exception"))
                .collect(Collectors.toList());

        return exceptions.stream().map(s -> {
            ClassName exceptionClass = PoetUtils.getTransformClass(basePackage, s.getShapeName() + "Unmarshaller");
            return CodeBlock.builder()
                    .add("unmarshallers.add(new $T());", exceptionClass).build();
        }).collect(Collectors.toList());
    }
}
