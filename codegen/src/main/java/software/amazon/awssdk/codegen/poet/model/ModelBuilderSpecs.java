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
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;

import javax.lang.model.element.Modifier;

import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.poet.PoetExtensions;

/**
 * Provides the Poet specs for model class builders.
 */
class ModelBuilderSpecs {
    private final ShapeModel shapeModel;
    private final ShapeModelSpec shapeModelSpec;
    private final TypeProvider typeProvider;
    private final PoetExtensions poetExtensions;
    private final SettersFactory settersFactory;

    public ModelBuilderSpecs(ShapeModel shapeModel,
                             ShapeModelSpec shapeModelSpec,
                             TypeProvider typeProvider,
                             PoetExtensions poetExtensions) {
        this.shapeModel = shapeModel;
        this.shapeModelSpec = shapeModelSpec;
        this.typeProvider = typeProvider;
        this.poetExtensions = poetExtensions;
        this.settersFactory = new SettersFactory(this.typeProvider);
    }

    public ClassName builderInterfaceName() {
        return classToBuild().nestedClass("Builder");
    }

    public ClassName builderImplName() {
        return classToBuild().nestedClass("BeanStyleBuilder");
    }

    public TypeSpec builderInterface() {
        TypeSpec.Builder builder = TypeSpec.interfaceBuilder(builderInterfaceName())
                .addModifiers(Modifier.PUBLIC);

        shapeModel.getMembers().forEach(m -> builder.addMethods(
                settersFactory.fluentSetterDeclarations(m, builderInterfaceName())));

        builder.addMethod(MethodSpec.methodBuilder("build_")
                .returns(classToBuild())
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .build());

        return builder.build();
    }


    public TypeSpec beanStyleBuilder() {
        TypeSpec.Builder builderClassBuilder = TypeSpec.classBuilder(builderImplName())
                .addSuperinterface(builderInterfaceName())
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC);

        builderClassBuilder.addFields(shapeModelSpec.fields(Modifier.PRIVATE));

        // No-arg constructor
        builderClassBuilder.addMethod(MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PRIVATE)
                .build());

        // Accepts Model for copying
        MethodSpec.Builder copyBuilderCtor = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PRIVATE)
                .addParameter(classToBuild(), "model");

        shapeModel.getMembers().forEach(m -> {
            String name = m.getVariable().getVariableName();
            copyBuilderCtor.addStatement("this.$N = model.$N", name, name);
        });

        shapeModel.getMembers().forEach(m -> {
            builderClassBuilder.addMethods(settersFactory.fluentSetters(m, builderInterfaceName()));
            builderClassBuilder.addMethods(settersFactory.beanStyleSetters(m));

        });

        builderClassBuilder.addMethod(copyBuilderCtor.build());

        // FIXME: using 'build_' to avoid clashing with models that have a 'build' property
        builderClassBuilder.addMethod(MethodSpec.methodBuilder("build_")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(classToBuild())
                .addStatement("return new $T(this)", classToBuild())
                .build());

        return builderClassBuilder.build();
    }

    private ClassName classToBuild() {
        return poetExtensions.getModelClass(shapeModel.getShapeName());
    }
}
