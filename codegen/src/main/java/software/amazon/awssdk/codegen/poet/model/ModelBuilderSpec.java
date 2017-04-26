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

import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.lang.model.element.Modifier;

import software.amazon.awssdk.codegen.model.config.customization.ConvenienceTypeOverload;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.ListModel;
import software.amazon.awssdk.codegen.model.intermediate.MemberModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.PoetExtensions;
import software.amazon.awssdk.codegen.poet.PoetUtils;

/**
 * Provides the Poet specs for model class builders.
 */
class ModelBuilderSpec implements ClassSpec {
    private final IntermediateModel intermediateModel;
    private final ShapeModel shapeModel;
    private final ShapeModelSpec shapeModelSpec;
    private final TypeProvider typeProvider;
    private final PoetExtensions poetExtensions;

    public ModelBuilderSpec(IntermediateModel intermediateModel,
                            ShapeModel shapeModel,
                            ShapeModelSpec shapeModelSpec,
                            TypeProvider typeProvider,
                            PoetExtensions poetExtensions) {
        this.intermediateModel = intermediateModel;
        this.shapeModel = shapeModel;
        this.shapeModelSpec = shapeModelSpec;
        this.typeProvider = typeProvider;
        this.poetExtensions = poetExtensions;
    }

    @Override
    public TypeSpec poetSpec() {
        TypeSpec.Builder builderClassBuilder = TypeSpec.classBuilder(className())
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC);

        builderClassBuilder.addFields(shapeModelSpec.fields());

        // No-arg constructor
        builderClassBuilder.addMethod(MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PRIVATE)
                .build());

        // Accepts Model for copying
        MethodSpec.Builder copyBuilderCtor = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PRIVATE)
                .addParameter(classToBuild(), "model");

        if (shapeModel.getMembers() != null) {
            shapeModel.getMembers().forEach(m -> {
                String name = m.getVariable().getVariableName();
                copyBuilderCtor.addStatement("this.$N = model.$N", name, name);
                builderClassBuilder.addMethods(fluentSetters(m));
            });
        }

        builderClassBuilder.addMethod(copyBuilderCtor.build());

        // FIXME: using 'build_' to avoid clashing with models that have a 'build' property
        builderClassBuilder.addMethod(MethodSpec.methodBuilder("build_")
                .addModifiers(Modifier.PUBLIC)
                .returns(classToBuild())
                .addStatement("return new $T(this)", classToBuild())
                .build());

        return builderClassBuilder.build();
    }

    @Override
    public ClassName className() {
        return classToBuild().nestedClass("Builder");
    }

    private ClassName classToBuild() {
        return poetExtensions.getModelClass(shapeModel.getShapeName());
    }

    private List<MethodSpec> fluentSetters(MemberModel memberModel) {
        if (memberModel.isList()) {
            return listMemberSetters(memberModel);
        } else if (memberModel.isMap()) {
            return mapMemberSetters(memberModel);
        }
        return nonCollectionSetters(memberModel);
    }

    private List<MethodSpec> listMemberSetters(MemberModel memberModel) {
        List<MethodSpec> setters = new ArrayList<>();
        ListModel listModel = memberModel.getListModel();
        TypeName listMemberType = typeProvider.getStorageType(listModel.getListMemberModel());
        String javadoc = PoetUtils.makeJavadocPoetFriendly(memberModel.getFluentSetterDocumentation());

        setters.add(copySetter(memberModel, ParameterizedTypeName.get(typeProvider.listImplClassName(), listMemberType),
                javadoc));

        setters.add(varargToListSetter(memberModel, listMemberType, javadoc));

        // If this is a list of enums
        if (memberModel.getEnumType() != null) {
            TypeName enumType = poetExtensions.getModelClass(memberModel.getEnumType());
            setters.add(enumVarargToListSetter(memberModel, enumType, javadoc));
        }

        return setters;
    }

    private List<MethodSpec> mapMemberSetters(MemberModel memberModel) {
        String javadoc = PoetUtils.makeJavadocPoetFriendly(memberModel.getFluentSetterDocumentation());
        String memberName = memberModel.getVariable().getVariableName();
        MethodSpec.Builder assignmentSetter = MethodSpec.methodBuilder(memberModel.getSetterMethodName())
                .addJavadoc(javadoc)
                .addModifiers(Modifier.PUBLIC)
                .returns(className())
                .addParameter(typeProvider.getStorageType(memberModel), memberName)
                .addStatement("this.$N = new $T<>($N)", memberName, typeProvider.mapImplClassName(), memberName)
                .addStatement("return this");

        return Collections.singletonList(assignmentSetter.build());
    }

    private List<MethodSpec> nonCollectionSetters(MemberModel memberModel) {
        List<MethodSpec> setters = new ArrayList<>();
        String methodName = memberModel.getSetterMethodName();
        String javadoc = PoetUtils.makeJavadocPoetFriendly(memberModel.getFluentSetterDocumentation());
        String memberName = memberModel.getVariable().getVariableName();
        TypeName memberTypeName = typeProvider.getStorageType(memberModel);

        setters.add(MethodSpec.methodBuilder(methodName)
                .addJavadoc(javadoc)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(memberTypeName, memberName)
                .returns(className())
                .addStatement("this.$N = $N", memberName, memberName)
                .addStatement("return this")
                .build());

        if (memberModel.getEnumType() != null) {
            setters.add(MethodSpec.methodBuilder(methodName)
                    .addJavadoc(javadoc)
                    .addModifiers(Modifier.PUBLIC)
                    .returns(className())
                    .addParameter(poetExtensions.getModelClass(memberModel.getEnumType()), memberName)
                    .addStatement("this.$N = $N.toString()", memberName, memberName)
                    .addStatement("return this")
                    .build());
        }

        List<ConvenienceTypeOverload> convenienceOverloads;
        if ((convenienceOverloads = intermediateModel.getCustomizationConfig()
                .getConvenienceTypeOverloads()) != null) {
            for (ConvenienceTypeOverload c : convenienceOverloads) {
                if (!c.accepts(shapeModel, memberModel)) {
                    continue;
                }

                TypeName convenienceParamType = typeProvider.getTypeNameForSimpleType(c.getConvenienceType());
                setters.add(MethodSpec.methodBuilder(methodName)
                        .addJavadoc(javadoc)
                        .addModifiers(Modifier.PUBLIC)
                        .returns(className())
                        .addParameter(convenienceParamType, memberName)
                        .addStatement("this.$N = new $T().adapt($N)", memberName,
                                PoetUtils.classNameFromFqcn(c.getTypeAdapterFqcn()), memberName)
                        .addStatement("return this")
                        .build());
            }
        }

        return setters;
    }

    private MethodSpec enumVarargToListSetter(MemberModel memberModel, TypeName enumTypeName, String javadoc) {
        String fieldName = memberModel.getVariable().getVariableName();
        return MethodSpec.methodBuilder(fieldName)
                .addJavadoc(javadoc)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ArrayTypeName.of(enumTypeName), fieldName).varargs(true)
                .returns(className())
                .beginControlFlow("if (this.$N == null)", fieldName)
                .addStatement("this.$N = new $T($N.length)", fieldName,
                        ParameterizedTypeName.get(typeProvider.listImplClassName(),
                        ClassName.get(String.class)),
                        fieldName)
                .endControlFlow()
                .beginControlFlow("for ($T ele : $N)", enumTypeName, fieldName)
                .addStatement("this.$N.add(ele.toString())", fieldName)
                .endControlFlow()
                .addStatement("return this")
                .build();
    }

    private MethodSpec copySetter(MemberModel memberModel, TypeName copyTypeImpl, String javadoc) {
        String fieldName = memberModel.getVariable().getVariableName();
        return MethodSpec.methodBuilder(memberModel.getSetterMethodName())
                .addModifiers(Modifier.PUBLIC)
                .returns(className())
                .addParameter(typeProvider.getStorageType(memberModel), fieldName)
                .addJavadoc(javadoc)
                .beginControlFlow("if ($N == null)", fieldName)
                .addStatement("this.$N = null", fieldName)
                .endControlFlow()
                .beginControlFlow("else")
                .addStatement("this.$N = new $T($N)", fieldName, copyTypeImpl, fieldName)
                .endControlFlow()
                .addStatement("return this")
                .build();
    }

    private MethodSpec varargToListSetter(MemberModel memberModel, TypeName listMemberType, String javadoc) {
        String fieldName = memberModel.getVariable().getVariableName();
        return MethodSpec.methodBuilder(memberModel.getSetterMethodName())
                .addJavadoc(javadoc)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ArrayTypeName.of(listMemberType), fieldName).varargs(true)
                .returns(className())
                .beginControlFlow("if (this.$N == null)", fieldName)
                .addStatement("this.$N = new $T<>($N.length)", fieldName, typeProvider.listImplClassName(), fieldName)
                .endControlFlow()
                .beginControlFlow("for ($T ele : $N)", listMemberType, fieldName)
                .addStatement("this.$N.add(ele)", fieldName)
                .endControlFlow()
                .addStatement("return this")
                .build();
    }
}
