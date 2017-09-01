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
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.AwsResponse;
import software.amazon.awssdk.AwsResponseMetadata;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.PoetExtensions;
import software.amazon.awssdk.codegen.poet.PoetUtils;

public class AwsServiceBaseResponseSpec implements ClassSpec {
    private final IntermediateModel intermediateModel;
    private final PoetExtensions poetExtensions;

    public AwsServiceBaseResponseSpec(IntermediateModel intermediateModel) {
        this.intermediateModel = intermediateModel;
        this.poetExtensions = new PoetExtensions(this.intermediateModel);
    }

    @Override
    public TypeSpec poetSpec() {
        TypeSpec.Builder builder = TypeSpec.classBuilder(className())
                .addJavadoc("Base response class for all requests to " + intermediateModel.getMetadata().getServiceFullName())
                .addAnnotation(PoetUtils.GENERATED)
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .superclass(ClassName.get(AwsResponse.class))
                .addMethod(MethodSpec.constructorBuilder()
                        .addModifiers(Modifier.PROTECTED)
                        .addParameter(className().nestedClass("Builder"), "builder")
                        .addStatement("super(builder)")
                        .build())
                .addType(builderInterfaceSpec())
                .addType(builderImplSpec());
        return builder.build();
    }

    @Override
    public ClassName className() {
        return poetExtensions.getModelClass(intermediateModel.getMetadata().getServiceName().replace(" ", "") + "Response");
    }

    private TypeSpec builderInterfaceSpec() {
        return TypeSpec.interfaceBuilder("Builder")
                .addSuperinterface(ClassName.get(AwsResponse.class).nestedClass("Builder"))

                .addMethod(MethodSpec.methodBuilder("build")
                        .addAnnotation(Override.class)
                        .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                        .returns(className())
                        .build())

                .addMethod(MethodSpec.methodBuilder("responseMetadata")
                        .addAnnotation(Override.class)
                        .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                        .addParameter(AwsResponseMetadata.class, "awsResponseMetadata")
                        .returns(className().nestedClass("Builder"))
                        .build())
                .build();
    }

    private TypeSpec builderImplSpec() {
        return TypeSpec.classBuilder("BuilderImpl")
                .addModifiers(Modifier.PROTECTED, Modifier.STATIC, Modifier.ABSTRACT)
                .addSuperinterface(className().nestedClass("Builder"))
                .superclass(ClassName.get(AwsResponse.class).nestedClass("BuilderImpl"))

                .addMethod(MethodSpec.constructorBuilder()
                        .addModifiers(Modifier.PROTECTED)
                        .build())

                .addMethod(MethodSpec.constructorBuilder()
                        .addModifiers(Modifier.PROTECTED)
                        .addParameter(className(), "request")
                        .addStatement("super(request)")
                        .build())

                .addMethod(MethodSpec.methodBuilder("responseMetadata")
                        .addAnnotation(Override.class)
                        .addModifiers(Modifier.PUBLIC)
                        .returns(className().nestedClass("Builder"))
                        .addParameter(AwsResponseMetadata.class, "awsResponseMetadata")
                        .addStatement("super.responseMetadata($N)", "awsResponseMetadata")
                        .addStatement("return this")
                        .build())

                .build();
    }
}
