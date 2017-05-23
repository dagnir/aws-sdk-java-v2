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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;

import java.util.ArrayList;
import java.util.List;

import software.amazon.awssdk.codegen.model.intermediate.MemberModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeType;

class ListSetters extends AbstractMemberSetters {
    private TypeProvider typeProvider;

    public ListSetters(ShapeModel shapeModel, MemberModel memberModel, TypeProvider typeProvider) {
        super(shapeModel, memberModel, typeProvider);
        this.typeProvider = typeProvider;
    }

    public List<MethodSpec> fluentDeclarations(TypeName returnType) {
        List<MethodSpec> fluentDeclarations = new ArrayList<>();

        fluentDeclarations.add(fluentSetterDeclaration(memberAsParameter(), returnType).build());
        fluentDeclarations.add(fluentSetterDeclaration(
                ParameterSpec.builder(asArray(), fieldName()).build(), returnType).varargs(true).build());
        if (memberModel().getEnumType() != null) {
            fluentDeclarations.add(fluentSetterDeclaration(ParameterSpec.builder(
                    asArrayOfModeledElement(), fieldName()).build(), returnType).varargs(true).build());
        }

        return fluentDeclarations;
    }

    @Override
    public List<MethodSpec> fluent(TypeName returnType) {
        List<MethodSpec> fluent = new ArrayList<>();

        fluent.add(fluentCopySetter(returnType));
        fluent.add(fluentVarargToListSetter(returnType));

        if (memberModel().getEnumType() != null) {
            fluent.add(fluentEnumVarargToListSetter(returnType));
        }

        return fluent;
    }

    @Override
    public List<MethodSpec> beanStyle() {
        List<MethodSpec> beanStyle = new ArrayList<>();

        beanStyle.add(beanStyleCopySetter());
        beanStyle.add(beanStyleVarargToListSetter());

        if (memberModel().getEnumType() != null) {
            beanStyle.add(beanStyleEnumVarargToListSetter());
        }

        return beanStyle;
    }

    private MethodSpec fluentCopySetter(TypeName returnType) {
        return fluentSetterBuilder(returnType)
                .addCode(copySetterBody()
                        .toBuilder()
                        .addStatement("return this").build())
                .build();
    }

    private MethodSpec beanStyleCopySetter() {
        MethodSpec.Builder builder = beanStyleSetterBuilder()
                .addCode(copySetterBody());

        if (shapeModel().getShapeType() == ShapeType.Exception) {
            builder.addAnnotation(
                    AnnotationSpec.builder(JsonProperty.class)
                            .addMember("value", "$S", memberModel().getHttp().getMarshallLocationName()).build());
        }

        return builder.build();

    }

    private MethodSpec fluentVarargToListSetter(TypeName returnType) {
        return fluentSetterBuilder(ParameterSpec.builder(asArray(), fieldName()).build(), returnType)
                .varargs(true)
                .addAnnotation(SafeVarargs.class)
                .addCode(varargToListBody().toBuilder().addStatement("return this").build())
                .build();
    }

    private MethodSpec beanStyleVarargToListSetter() {
        return beanStyleSetterBuilder(ParameterSpec.builder(asArray(), fieldName()).build())
                .varargs(true)
                .addAnnotation(SafeVarargs.class)
                .addCode(varargToListBody())
                .build();
    }

    private MethodSpec fluentEnumVarargToListSetter(TypeName returnType) {
        return fluentSetterBuilder(ParameterSpec.builder(asArrayOfModeledElement(), fieldName()).build(), returnType)
                .varargs(true)
                .addAnnotation(SafeVarargs.class)
                .addCode(enumVarargToListBody().toBuilder().addStatement("return this").build())
                .build();
    }

    private MethodSpec beanStyleEnumVarargToListSetter() {
        return beanStyleSetterBuilder(ParameterSpec.builder(asArrayOfModeledElement(), fieldName()).build())
                .varargs(true)
                .addAnnotation(SafeVarargs.class)
                .addCode(enumVarargToListBody())
                .build();
    }

    private CodeBlock varargToListBody() {
        return CodeBlock.builder()
                .beginControlFlow("if (this.$N == null)", fieldName())
                .addStatement("this.$N = new $T<>($N.length)", fieldName(),
                        typeProvider.listImplClassName(),
                        fieldName())
                .endControlFlow()
                .beginControlFlow("for ($T e: $N)", listElementType(), fieldName())
                .addStatement("this.$N.add($N.$N(e))", fieldName(), MemberCopierSpec.copierClassName(elementModel()),
                        MemberCopierSpec.copyMethodName(elementModel()))
                .endControlFlow()
                .build();
    }

    private CodeBlock enumVarargToListBody() {
        return CodeBlock.builder()
                .beginControlFlow("if (this.$N == null)", fieldName())
                .addStatement("this.$N = new $T($N.length)", fieldName(),
                        ParameterizedTypeName.get(typeProvider.listImplClassName(),
                                ClassName.get(String.class)),
                        fieldName())
                .endControlFlow()
                .beginControlFlow("for ($T ele : $N)", modeledElement(), fieldName())
                .addStatement("this.$N.add(ele.toString())", fieldName())
                .endControlFlow()
                .build();
    }

    private MemberModel elementModel() {
        return memberModel().getListModel().getListMemberModel();
    }

    private TypeName modeledElement() {
        return typeProvider.getModelClass(memberModel().getEnumType());
    }

    private TypeName listElementType() {
        return typeProvider.parameterType(elementModel());
    }

    @Override
    protected ParameterSpec memberAsParameter() {
        return ParameterSpec.builder(typeProvider.parameterType(memberModel()), fieldName()).build();
    }

    private ArrayTypeName asArray() {
        return ArrayTypeName.of(listElementType());
    }

    private ArrayTypeName asArrayOfModeledElement() {
        return ArrayTypeName.of(modeledElement());
    }
}
