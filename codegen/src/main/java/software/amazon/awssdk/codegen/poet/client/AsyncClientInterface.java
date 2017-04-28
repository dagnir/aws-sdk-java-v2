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
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.PoetUtils;

public class AsyncClientInterface implements ClassSpec {

    private final IntermediateModel model;
    private final ClassName className;
    private final String basePackage;

    public AsyncClientInterface(IntermediateModel model) {
        this.basePackage = model.getMetadata().getPackageName();
        this.model = model;
        this.className = ClassName.get(basePackage, model.getMetadata().getAsyncInterface());
    }

    @Override
    public TypeSpec poetSpec() {
        return PoetUtils.createInterfaceBuilder(className)
                .addSuperinterface(AutoCloseable.class)
                .addField(FieldSpec.builder(String.class, "ENDPOINT_PREFIX")
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                        .initializer("$S", model.getMetadata().getEndpointPrefix())
                        .build())
                .addMethods(operations())
                .addMethod(builder())
                .build();
    }

    @Override
    public ClassName className() {
        return className;
    }

    private Iterable<MethodSpec> operations() {
        return model.getOperations().values().stream().map(this::toMethodSpec).collect(Collectors.toList());
    }

    private MethodSpec toMethodSpec(OperationModel opModel) {
        ClassName returnTypeClass = PoetUtils.getModelClass(basePackage, opModel.getReturnType().getReturnType());
        TypeName returnType = ParameterizedTypeName.get(ClassName.get(CompletableFuture.class), returnTypeClass);
        ClassName requestType = PoetUtils.getModelClass(basePackage, opModel.getInput().getVariableType());

        return MethodSpec.methodBuilder(opModel.getMethodName())
                .returns(returnType)
                .addModifiers(Modifier.DEFAULT, Modifier.PUBLIC)
                .addParameter(requestType, opModel.getInput().getVariableName())
                .addStatement("throw new $T()", UnsupportedOperationException.class)
                .addJavadoc(opModel.getAsyncDocumentation(model.getMetadata()))
                .build();
    }

    private MethodSpec builder() {
        ClassName builderClass = ClassName.get(basePackage, model.getMetadata().getAsyncBuilder());
        ClassName builderInterface = ClassName.get(basePackage, model.getMetadata().getAsyncBuilderInterface());
        return MethodSpec.methodBuilder("builder")
                         .returns(builderInterface)
                         .addModifiers(Modifier.STATIC, Modifier.PUBLIC)
                         .addStatement("return new $T()", builderClass)
                         .build();
    }
}
