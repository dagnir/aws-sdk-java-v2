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

    protected final IntermediateModel model;
    protected final ClassName className;
    protected final String clientPackageName;
    private final String modelPackage;

    public AsyncClientInterface(IntermediateModel model) {
        this.modelPackage = model.getMetadata().getFullModelPackageName();
        this.clientPackageName = model.getMetadata().getFullClientPackageName();
        this.model = model;
        this.className = ClassName.get(model.getMetadata().getFullClientPackageName(),
                                       model.getMetadata().getAsyncInterface());
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
                        .addMethod(builderMethod())
                        .build();
    }

    @Override
    public ClassName className() {
        return className;
    }

    protected final Iterable<MethodSpec> operations() {
        return model.getOperations().values().stream()
                    .map(this::operationSignatureAndJavaDoc)
                    .map(b -> this.operationBody(b.builder, b.opModel))
                    .map(MethodSpec.Builder::build)
                    .collect(Collectors.toList());
    }

    protected MethodSpec.Builder operationBody(MethodSpec.Builder builder, OperationModel operationModel) {
        return builder.addModifiers(Modifier.DEFAULT, Modifier.PUBLIC)
                      .addStatement("throw new $T()", UnsupportedOperationException.class);
    }

    private BuilderModelBag operationSignatureAndJavaDoc(OperationModel opModel) {
        ClassName returnTypeClass = ClassName.get(modelPackage, opModel.getReturnType().getReturnType());
        TypeName returnType = ParameterizedTypeName.get(ClassName.get(CompletableFuture.class), returnTypeClass);
        ClassName requestType = ClassName.get(modelPackage, opModel.getInput().getVariableType());

        return new BuilderModelBag(MethodSpec.methodBuilder(opModel.getMethodName())
                         .returns(returnType)
                         .addParameter(requestType, opModel.getInput().getVariableName())
                         .addJavadoc(opModel.getAsyncDocumentation(model.getMetadata())), opModel);
    }

    private MethodSpec builderMethod() {
        ClassName builderClass = ClassName.get(clientPackageName, model.getMetadata().getAsyncBuilder());
        ClassName builderInterface = ClassName.get(clientPackageName, model.getMetadata().getAsyncBuilderInterface());
        return MethodSpec.methodBuilder("builder")
                         .returns(builderInterface)
                         .addModifiers(Modifier.STATIC, Modifier.PUBLIC)
                         .addStatement("return new $T()", builderClass)
                         .build();
    }

    private static class BuilderModelBag {
        private final MethodSpec.Builder builder;
        private final OperationModel opModel;

        private BuilderModelBag(MethodSpec.Builder builder, OperationModel opModel) {
            this.builder = builder;
            this.opModel = opModel;
        }
    }
}
