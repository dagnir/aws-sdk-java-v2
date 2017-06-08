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

package software.amazon.awssdk.codegen.poet.model;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.MemberModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeType;
import software.amazon.awssdk.codegen.poet.PoetExtensions;
import software.amazon.awssdk.utils.builder.CopyableBuilder;

/**
 * Provides the Poet specs for model class builders.
 */
class ModelBuilderSpecs {
    private final IntermediateModel intermediateModel;
    private final ShapeModel shapeModel;
    private final ShapeModelSpec shapeModelSpec;
    private final TypeProvider typeProvider;
    private final PoetExtensions poetExtensions;
    private final SettersFactory settersFactory;

    public ModelBuilderSpecs(IntermediateModel intermediateModel, ShapeModel shapeModel,
                             ShapeModelSpec shapeModelSpec,
                             TypeProvider typeProvider) {
        this.intermediateModel = intermediateModel;
        this.shapeModel = shapeModel;
        this.shapeModelSpec = shapeModelSpec;
        this.typeProvider = typeProvider;
        this.poetExtensions = new PoetExtensions(this.intermediateModel);
        this.settersFactory = new SettersFactory(this.shapeModel, this.intermediateModel, this.typeProvider);
    }

    public ClassName builderInterfaceName() {
        return classToBuild().nestedClass("Builder");
    }

    public ClassName builderImplName() {
        return classToBuild().nestedClass("BeanStyleBuilder");
    }

    public TypeSpec builderInterface() {
        TypeSpec.Builder builder = TypeSpec.interfaceBuilder(builderInterfaceName())
                .addSuperinterface(copyableBuilderSuperInterface())
                .addModifiers(Modifier.PUBLIC);

        shapeModel.getMembers().forEach(m -> builder.addMethods(
                settersFactory.fluentSetterDeclarations(m, builderInterfaceName())));

        if (exception()) {
            builder.addMethod(MethodSpec.methodBuilder("message")
                    .returns(builderInterfaceName())
                    .addParameter(String.class, "message")
                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT).build());
        }

        return builder.build();
    }


    public TypeSpec beanStyleBuilder() {
        TypeSpec.Builder builderClassBuilder = TypeSpec.classBuilder(builderImplName())
                .addSuperinterface(builderInterfaceName())
                // TODO: Uncomment this once property shadowing is fixed
                //.addSuperinterface(copyableBuilderSuperInterface())
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL);

        builderClassBuilder.addFields(fields());
        builderClassBuilder.addMethod(noargConstructor());
        builderClassBuilder.addMethod(modelCopyConstructor());
        builderClassBuilder.addMethods(setters());
        builderClassBuilder.addMethod(buildMethod());

        return builderClassBuilder.build();
    }

    private List<FieldSpec> fields() {
        List<FieldSpec> fields = shapeModelSpec.fields(Modifier.PRIVATE);

        Map<String, MemberModel> members = shapeModel.getMembers().stream()
                .collect(Collectors.toMap(m -> shapeModelSpec.asField(m).name, m -> m));

        // Auto initialize any auto construct containers
        fields = fields.stream()
                .map(f -> {
                    MemberModel m = members.get(f.name);

                    if (intermediateModel.getCustomizationConfig().isUseAutoConstructList() && m.isList()) {
                        return f.toBuilder().initializer(CodeBlock.builder()
                                .add("new $T<>()", typeProvider.listImplClassName())
                                .build())
                                .build();
                    }

                    if (intermediateModel.getCustomizationConfig().isUseAutoConstructMap() && m.isMap()) {
                        return f.toBuilder().initializer(CodeBlock.builder()
                                .add("new $T<>()", typeProvider.mapImplClassName())
                                .build())
                                .build();
                    }

                    return f;
                })
                .collect(Collectors.toList());

        // Inject a message member for the exception message
        if (exception()) {
            fields = new ArrayList<>(fields);
            fields.add(FieldSpec.builder(String.class, "message", Modifier.PRIVATE).build());
        }

        return fields;
    }

    private MethodSpec noargConstructor() {
        return MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PRIVATE)
                .build();
    }

    private MethodSpec modelCopyConstructor() {
        MethodSpec.Builder copyBuilderCtor = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PRIVATE)
                .addParameter(classToBuild(), "model");

        shapeModel.getMembers().forEach(m -> {
            String name = m.getVariable().getVariableName();
            copyBuilderCtor.addStatement("$N(model.$N)", m.getSetterMethodName(), name);
        });

        if (exception()) {
            copyBuilderCtor.addStatement("this.message = model.getMessage()");
        }

        return copyBuilderCtor.build();
    }

    private List<MethodSpec> setters() {
        List<MethodSpec> setters = new ArrayList<>();
        shapeModel.getMembers().stream().flatMap(m ->
            Stream.concat(settersFactory.fluentSetters(m, builderInterfaceName()).stream(),
                settersFactory.beanStyleSetters(m).stream()))
                .forEach(setters::add);

        if (exception()) {
            setters.addAll(exceptionMessageSetters());
        }

        return setters;
    }

    private MethodSpec buildMethod() {
        return MethodSpec.methodBuilder("build")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(classToBuild())
                .addStatement("return new $T(this)", classToBuild())
                .build();
    }

    private ClassName classToBuild() {
        return poetExtensions.getModelClass(shapeModel.getShapeName());
    }

    private boolean exception() {
        return shapeModel.getShapeType() == ShapeType.Exception;
    }

    private TypeName copyableBuilderSuperInterface() {
        return ParameterizedTypeName.get(ClassName.get(CopyableBuilder.class),
                classToBuild().nestedClass("Builder"),
                classToBuild());
    }

    List<MethodSpec> exceptionMessageSetters() {
        List<MethodSpec> setters = new ArrayList<>();

        // bean style
        setters.add(MethodSpec.methodBuilder("setMessage")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(String.class, "message")
                .addStatement("this.message = message")
                .build());

        setters.add(MethodSpec.methodBuilder("message")
                .addModifiers(Modifier.PUBLIC)
                .returns(builderInterfaceName())
                .addAnnotation(Override.class)
                .addParameter(String.class, "message")
                .addStatement("this.message = message")
                .addStatement("return this")
                .build());

        return setters;
    }
}
