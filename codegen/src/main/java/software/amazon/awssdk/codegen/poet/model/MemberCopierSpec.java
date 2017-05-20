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
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.util.Locale;
import javax.lang.model.element.Modifier;

import software.amazon.awssdk.codegen.internal.Utils;
import software.amazon.awssdk.codegen.model.intermediate.MapModel;
import software.amazon.awssdk.codegen.model.intermediate.MemberModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.PoetExtensions;
import software.amazon.awssdk.codegen.poet.PoetUtils;

class MemberCopierSpec implements ClassSpec {
    private final TypeProvider typeProvider;
    private final MemberModel memberModel;
    private final PoetExtensions poetExtensions;

    MemberCopierSpec(TypeProvider typeProvider, MemberModel memberModel, PoetExtensions poetExtensions) {
        this.typeProvider = typeProvider;
        this.memberModel = memberModel;
        this.poetExtensions = poetExtensions;
    }

    @Override
    public TypeSpec poetSpec() {
        return TypeSpec.classBuilder(className())
                .addModifiers(Modifier.FINAL)
                .addAnnotation(PoetUtils.GENERATED)
                .addMethod(copyMethod())
                .build();
    }

    @Override
    public ClassName className() {
        return poetExtensions.getModelClass(copierClassName(memberModel));
    }

    private MethodSpec copyMethod() {
        return copyMethodProto().addCode(copyMethodBody()).build();
    }

    private MethodSpec.Builder copyMethodProto() {
        TypeName parameterType = typeProvider.parameterType(memberModel);
        return MethodSpec.methodBuilder(copyMethodName(memberModel))
                .addModifiers(Modifier.STATIC)
                .addParameter(ParameterSpec.builder(parameterType, memberParamName()).build())
                .returns(typeProvider.fieldType(memberModel));
    }

    private CodeBlock copyMethodBody() {
        if (memberModel.isSimple()) {
            return simpleTypeCopyBody(memberModel);
        }

        if (memberModel.isMap()) {
            return mapCopyBody();
        }

        if (memberModel.isList()) {
            return listCopyBody();
        }

        return copyBuildableCopyBody();
    }

    private CodeBlock simpleTypeCopyBody(MemberModel memberModel) {
        final String paramName = memberParamName();
        final String simpleType = memberModel.getVariable().getSimpleType();
        switch (simpleType) {
            case "Date":
                return CodeBlock.builder()
                        .beginControlFlow("if ($N == null)", paramName)
                        .addStatement("return null")
                        .endControlFlow()
                        .addStatement("return new Date($N.getTime())", paramName).build();
            case "ByteBuffer":
                return CodeBlock.builder()
                        .beginControlFlow("if ($N == null)", paramName)
                        .addStatement("return null")
                        .endControlFlow()
                        .addStatement("return $N.duplicate()", paramName).build();
            case "Short":
            case "Integer":
            case "Long":
            case "Float":
            case "Double":
            case "Boolean":
            case "String":
            case "InputStream":
            default:
                return CodeBlock.builder().addStatement("return $N", paramName).build();
        }
    }

    private CodeBlock listCopyBody() {
        String paramName = memberParamName();
        MemberModel listMember = memberModel.getListModel().getListMemberModel();
        String copyName = paramName + "Copy";
        return CodeBlock.builder()
                .beginControlFlow("if ($N == null)", memberParamName())
                .addStatement("return null")
                .endControlFlow()
                .addStatement("$T $N = new $T<>($N.size())", typeProvider.fieldType(memberModel), copyName,
                        typeProvider.listImplClassName(), paramName)
                .beginControlFlow("for ($T e : $N)", typeProvider.parameterType(listMember), paramName)
                .addStatement("$N.add($N.$N(e))", copyName, copierClassName(listMember), copyMethodName(listMember))
                .endControlFlow()
                .addStatement("return $N", copyName)
                .build();
    }

    private CodeBlock mapCopyBody() {
        MapModel mapModel = memberModel.getMapModel();
        String paramName = memberParamName();
        String copyName = paramName + "Copy";
        return CodeBlock.builder()
                .beginControlFlow("if ($N == null)", memberParamName())
                .addStatement("return null")
                .endControlFlow()
                .addStatement("$T $N = new $T<>($N.size())", typeProvider.fieldType(memberModel),
                        copyName, typeProvider.mapImplClassName(), memberParamName())
                .beginControlFlow("for ($T e : $N.entrySet())", typeProvider.mapEntryType(mapModel), paramName)
                .addStatement("$N.put($N.$N(e.getKey()), $N.$N(e.getValue()))", copyName,
                        copierClassName(mapModel.getKeyType()),
                        simpleTypeCopyMethodName(mapModel.getKeyType()),
                        copierClassName(mapModel.getValueModel()),
                        copyMethodName(mapModel.getValueModel()))
                .endControlFlow()
                .addStatement("return $N", copyName)
                .build();
    }

    private CodeBlock copyBuildableCopyBody() {
        return CodeBlock.builder()
                .beginControlFlow("if ($N == null)", memberParamName())
                .addStatement("return null")
                .endControlFlow()
                .addStatement("return $N.toBuilder().build()", memberParamName())
                .build();
    }

    private String memberParamName() {
        if (memberModel.isSimple()) {
            return Utils.unCapitialize(memberModel.getVariable().getSimpleType()) + "Param";
        }
        return Utils.unCapitialize(memberModel.getC2jShape()) + "Param";
    }

    public static String simpleTypeCopyMethodName(String simpleType) {
        return "copy" + simpleType;
    }

    public static String copierClassName(MemberModel memberModel) {
        if (memberModel.isSimple()) {
            return copierClassName(memberModel.getVariable().getSimpleType());
        }
        return copierClassName(memberModel.getC2jShape());
    }

    public static String copierClassName(String type) {
        // TODO: Ugly hack, but some services (Health) have shapes with the
        // same name, only differing in case of the first letter
        String first = type.substring(0, 1);
        if (first.toLowerCase(Locale.ENGLISH).equals(first)) {
            return "_" + type + "Copier";
        }
        return type + "Copier";
    }

    public static String copyMethodName(MemberModel memberModel) {
        if (memberModel.isSimple()) {
            return simpleTypeCopyMethodName(memberModel.getVariable().getSimpleType());
        }
        return "copy" + memberModel.getC2jShape();
    }
}
