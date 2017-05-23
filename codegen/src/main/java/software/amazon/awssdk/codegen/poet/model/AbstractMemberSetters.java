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

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;

import javax.lang.model.element.Modifier;

import software.amazon.awssdk.codegen.model.intermediate.MemberModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.poet.PoetUtils;

/**
 * Abstract implementation of {@link MemberSetters} to share common functionality.
 */
abstract class AbstractMemberSetters implements MemberSetters {
    private ShapeModel shapeModel;
    private MemberModel memberModel;
    private TypeProvider typeProvider;

    public AbstractMemberSetters(ShapeModel shapeModel, MemberModel memberModel, TypeProvider typeProvider) {
        this.shapeModel = shapeModel;
        this.memberModel = memberModel;
        this.typeProvider = typeProvider;
    }

    protected MethodSpec.Builder fluentSetterDeclaration(ParameterSpec parameter, TypeName returnType) {
        return MethodSpec.methodBuilder(memberModel().getFluentSetterMethodName())
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addParameter(parameter)
                .returns(returnType)
                .addJavadoc(PoetUtils.makeJavadocPoetFriendly(memberModel.getFluentSetterDocumentation()));

    }

    protected MethodSpec.Builder fluentSetterBuilder(TypeName returnType) {
        return fluentSetterBuilder(memberAsParameter(), returnType);
    }

    protected MethodSpec.Builder fluentSetterBuilder(ParameterSpec setterParam, TypeName returnType) {
        return MethodSpec.methodBuilder(memberModel().getFluentSetterMethodName())
                .addParameter(setterParam)
                .addAnnotation(Override.class)
                .returns(returnType)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL);
    }

    protected MethodSpec.Builder beanStyleSetterBuilder() {
        return beanStyleSetterBuilder(memberAsParameter());
    }

    protected MethodSpec.Builder beanStyleSetterBuilder(ParameterSpec setterParam) {
        return MethodSpec.methodBuilder(memberModel().getBeanStyleSetterMethodName())
                .addParameter(setterParam)
                .addJavadoc(PoetUtils.makeJavadocPoetFriendly(memberModel.getSetterDocumentation()))
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL);
    }

    protected CodeBlock copySetterBody() {
        return CodeBlock.builder()
                .addStatement("this.$N = $N.$N($N)", fieldName(), MemberCopierSpec.copierClassName(memberModel),
                        MemberCopierSpec.copyMethodName(memberModel), fieldName())
                .build();
    }

    protected ParameterSpec memberAsParameter() {
        return ParameterSpec.builder(typeProvider.parameterType(memberModel), fieldName()).build();
    }

    protected ShapeModel shapeModel() {
        return shapeModel;
    }

    protected MemberModel memberModel() {
        return memberModel;
    }

    protected String fieldName() {
        return memberModel.getVariable().getVariableName();
    }
}
