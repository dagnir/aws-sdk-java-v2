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
import software.amazon.awssdk.regions.ServiceMetadata;

public final class SyncClientInterface implements ClassSpec {

    private final IntermediateModel model;
    private final ClassName className;
    private final String clientPackageName;

    public SyncClientInterface(IntermediateModel model) {
        this.model = model;
        this.clientPackageName = model.getMetadata().getFullClientPackageName();
        this.className = ClassName.get(clientPackageName, model.getMetadata().getSyncInterface());
    }

    @Override
    public TypeSpec poetSpec() {
        Builder classBuilder = PoetUtils.createInterfaceBuilder(className)
                .addField(FieldSpec.builder(String.class, "SERVICE_NAME")
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                        .initializer("$S", model.getMetadata().getSigningName())
                        .build())
                .addSuperinterface(AutoCloseable.class)
                .addMethods(operations())
                .addMethod(builder())
                .addMethod(create())
                .addMethod(serviceMetadata());

        if (model.getHasWaiters()) {
            classBuilder.addMethod(waiters());
        }
        if (model.getCustomizationConfig().getPresignersFqcn() != null) {
            classBuilder.addMethod(presigners());
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
        ClassName builderClass = ClassName.get(clientPackageName, model.getMetadata().getSyncBuilder());
        ClassName builderInterface = ClassName.get(clientPackageName, model.getMetadata().getSyncBuilderInterface());
        return MethodSpec.methodBuilder("builder")
                .returns(builderInterface)
                .addModifiers(Modifier.STATIC, Modifier.PUBLIC)
                .addStatement("return new $T()", builderClass)
                .build();
    }

    private MethodSpec create() {
        return MethodSpec.methodBuilder("create")
                .returns(className)
                .addModifiers(Modifier.STATIC, Modifier.PUBLIC)
                .addStatement("return builder().build()")
                .build();
    }

    private MethodSpec serviceMetadata() {
        return MethodSpec.methodBuilder("serviceMetadata")
                .returns(ServiceMetadata.class)
                .addModifiers(Modifier.STATIC, Modifier.PUBLIC)
                .addStatement("return $T.of($S)", ServiceMetadata.class, model.getMetadata().getEndpointPrefix())
                .build();
    }

    private MethodSpec operationMethodSpec(OperationModel opModel) {
        ClassName returnType = ClassName.get(model.getMetadata().getFullModelPackageName(),
                                             opModel.getReturnType().getReturnType());
        ClassName requestType = ClassName.get(model.getMetadata().getFullModelPackageName(),
                                              opModel.getInput().getVariableType());

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
                .returns(ClassName.get(model.getMetadata().getFullWaitersPackageName(),
                                       model.getMetadata().getSyncInterface() + "Waiters"))
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .build();
    }

    private MethodSpec presigners() {
        ClassName presignerClassName = PoetUtils.classNameFromFqcn(model.getCustomizationConfig().getPresignersFqcn());
        return MethodSpec.methodBuilder("presigners")
                .returns(presignerClassName)
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .build();
    }
}
