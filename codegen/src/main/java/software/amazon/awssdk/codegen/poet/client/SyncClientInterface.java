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

import java.util.stream.Collectors;
import javax.lang.model.element.Modifier;

import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.PoetUtils;

public final class SyncClientInterface implements ClassSpec {

    private final IntermediateModel model;
    private final ClassName className;
    private final String basePackage;

    public SyncClientInterface(IntermediateModel model) {
        this.basePackage = model.getMetadata().getPackageName();
        this.model = model;
        this.className = ClassName.get(basePackage, model.getMetadata().getSyncInterface());
    }

    @Override
    public TypeSpec poetSpec() {
        Builder classBuilder = PoetUtils.createInterfaceBuilder(className)
                .addField(FieldSpec.builder(String.class, "ENDPOINT_PREFIX")
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                        .initializer("$S", model.getMetadata().getEndpointPrefix())
                        .build())
                .addMethods(operations())
                .addMethod(builder())
                .addMethod(create())
                .addSuperinterface(AutoCloseable.class);

        if (model.getHasWaiters()) {
            classBuilder.addMethod(waiters());
        }

        return classBuilder.build();
    }

    @Override
    public ClassName className() {
        return className;
    }

    private Iterable<MethodSpec> operations() {
        return model.getOperations().values().stream().map(this::operationMethodSpec).collect(Collectors.toList());
    }

    private MethodSpec builder() {
        ClassName builder = ClassName.get(basePackage, model.getMetadata().getSyncInterface() + "Builder");
        return MethodSpec.methodBuilder("builder")
                .returns(builder)
                .addModifiers(Modifier.STATIC, Modifier.PUBLIC)
                .addStatement("return $T.standard()", builder)
                .build();
    }

    private MethodSpec create() {
        ClassName builder = ClassName.get(basePackage, model.getMetadata().getSyncInterface() + "Builder");
        return MethodSpec.methodBuilder("create")
                .returns(className)
                .addModifiers(Modifier.STATIC, Modifier.PUBLIC)
                .addStatement("return $T.standard().build()", builder)
                .build();
    }

    private MethodSpec operationMethodSpec(OperationModel opModel) {
        ClassName returnType = PoetUtils.getModelClass(basePackage, opModel.getReturnType().getReturnType());
        ClassName requestType = PoetUtils.getModelClass(basePackage, opModel.getInput().getVariableType());

        return MethodSpec.methodBuilder(opModel.getMethodName())
                .returns(returnType)
                .addModifiers(Modifier.DEFAULT, Modifier.PUBLIC)
                .addParameter(requestType, opModel.getInput().getVariableName())
                .addStatement("throw new $T()", UnsupportedOperationException.class)
                .addJavadoc(opModel.getSyncDocumentation(model.getMetadata()))
                .build();
    }


    private MethodSpec waiters() {
        return MethodSpec.methodBuilder("waiters")
                .returns(ClassName.get(basePackage + ".waiters", model.getMetadata().getSyncInterface() + "Waiters"))
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .build();
    }

    private MethodSpec presigners() {
        String presignerFqcn = model.getCustomizationConfig().getPresignersFqcn();
        String basePath = presignerFqcn.substring(0, presignerFqcn.lastIndexOf("."));
        String className = presignerFqcn.substring(presignerFqcn.lastIndexOf(".") + 1);

        return MethodSpec.methodBuilder("presigners")
                .returns(ClassName.get(basePath, className))
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .build();
    }
}
