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
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import java.util.ArrayList;
import java.util.List;
import javax.lang.model.element.Modifier;

import software.amazon.awssdk.AwsRequestOverrideConfig;
import software.amazon.awssdk.AwsResponseMetadata;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
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
    private final AccessorsFactory accessorsFactory;

    ModelBuilderSpecs(IntermediateModel intermediateModel,
                      ShapeModel shapeModel,
                      ShapeModelSpec shapeModelSpec,
                      TypeProvider typeProvider) {
        this.intermediateModel = intermediateModel;
        this.shapeModel = shapeModel;
        this.shapeModelSpec = shapeModelSpec;
        this.typeProvider = typeProvider;
        this.poetExtensions = new PoetExtensions(this.intermediateModel);
        this.accessorsFactory = new AccessorsFactory(this.shapeModel, this.intermediateModel, this.typeProvider);
    }

    public ClassName builderInterfaceName() {
        return classToBuild().nestedClass("Builder");
    }

    public ClassName builderImplName() {
        return classToBuild().nestedClass("BuilderImpl");
    }

    public TypeSpec builderInterface() {
        TypeSpec.Builder builder = TypeSpec.interfaceBuilder(builderInterfaceName())
                .addSuperinterface(builderSuperInterface())
                .addModifiers(Modifier.PUBLIC);

        shapeModel.getNonStreamingMembers()
                  .forEach(m -> builder.addMethods(accessorsFactory.fluentSetterDeclarations(m, builderInterfaceName())));

        if (isException()) {
            builder.addMethod(MethodSpec.methodBuilder("message")
                    .returns(builderInterfaceName())
                    .addParameter(String.class, "message")
                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT).build());
        }

        if (isRequest()) {
            builder.addMethod(MethodSpec.methodBuilder("requestOverrideConfig")
                    .returns(builderInterfaceName())
                    .addAnnotation(Override.class)
                    .addParameter(AwsRequestOverrideConfig.class, "awsRequestOverrideConfig")
                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                    .build());
        }

        if (isResponse()) {
            builder.addMethod(MethodSpec.methodBuilder("responseMetadata")
                    .returns(builderInterfaceName())
                    .addAnnotation(Override.class)
                    .addParameter(AwsResponseMetadata.class, "awsResponseMetadata")
                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                    .build());
        }

        if (isRequest() || isResponse()) {
            builder.addMethod(MethodSpec.methodBuilder("build")
                    .returns(classToBuild())
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                    .build());
        }

        return builder.build();
    }

    public TypeSpec beanStyleBuilder() {
        TypeSpec.Builder builderClassBuilder = TypeSpec.classBuilder(builderImplName())
                .addSuperinterface(builderInterfaceName())
                .superclass(builderImplSuperClass())
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL);

        builderClassBuilder.addFields(fields());
        builderClassBuilder.addMethod(noargConstructor());
        builderClassBuilder.addMethod(modelCopyConstructor());
        builderClassBuilder.addMethods(accessors());
        builderClassBuilder.addMethod(buildMethod());

        return builderClassBuilder.build();
    }

    private TypeName builderImplSuperClass() {
        if (shapeModel.getShapeType() == ShapeType.Request) {
            return new AwsServiceBaseRequestSpec(intermediateModel).className().nestedClass("BuilderImpl");
        }

        if (shapeModel.getShapeType() == ShapeType.Response) {
            return new AwsServiceBaseResponseSpec(intermediateModel).className().nestedClass("BuilderImpl");
        }

        return ClassName.OBJECT;
    }

    private List<FieldSpec> fields() {
        List<FieldSpec> fields = shapeModelSpec.fields(Modifier.PRIVATE);

        // Inject a message member for the isException message
        if (isException()) {
            fields = new ArrayList<>(fields);
            fields.add(FieldSpec.builder(String.class, "message", Modifier.PRIVATE).build());
        }

        return fields;
    }

    private MethodSpec noargConstructor() {
        MethodSpec.Builder ctorBuilder = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PRIVATE);
        return ctorBuilder.build();
    }

    private MethodSpec modelCopyConstructor() {
        MethodSpec.Builder copyBuilderCtor = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PRIVATE)
                .addParameter(classToBuild(), "model");

        if (builderImplSuperClass() != ClassName.OBJECT) {
            copyBuilderCtor.addStatement("super(model)");
        }

        shapeModel.getNonStreamingMembers().forEach(m -> {
            String name = m.getVariable().getVariableName();
            copyBuilderCtor.addStatement("$N(model.$N)", m.getSetterMethodName(), name);
        });

        if (isException()) {
            copyBuilderCtor.addStatement("this.message = model.getMessage()");
        }

        return copyBuilderCtor.build();
    }

    private List<MethodSpec> accessors() {
        List<MethodSpec> accessors = new ArrayList<>();
        shapeModel.getNonStreamingMembers().stream()
                  .forEach(m -> {
                      accessors.add(accessorsFactory.beanStyleGetters(m));
                      accessors.addAll(accessorsFactory.fluentSetters(m, builderInterfaceName()));
                      accessors.addAll(accessorsFactory.beanStyleSetters(m));
                  });

        if (isException()) {
            accessors.addAll(exceptionMessageGetters());
            accessors.addAll(exceptionMessageSetters());
        }

        if (isRequest()) {
            accessors.add(MethodSpec.methodBuilder("requestOverrideConfig")
                    .addAnnotation(Override.class)
                    .returns(builderInterfaceName())
                    .addParameter(AwsRequestOverrideConfig.class, "awsRequestOverrideConfig")
                    .addModifiers(Modifier.PUBLIC)
                    .addStatement("super.requestOverrideConfig($N)", "awsRequestOverrideConfig")
                    .addStatement("return this")
                    .build());
        }

        if (isResponse()) {
            accessors.add(MethodSpec.methodBuilder("responseMetadata")
                    .addAnnotation(Override.class)
                    .returns(builderInterfaceName())
                    .addParameter(AwsResponseMetadata.class, "awsResponseMetadata")
                    .addModifiers(Modifier.PUBLIC)
                    .addStatement("super.responseMetadata($N)", "awsResponseMetadata")
                    .addStatement("return this")
                    .build());
        }

        return accessors;
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

    private boolean isException() {
        return shapeModel.getShapeType() == ShapeType.Exception;
    }

    private boolean isRequest() {
        return shapeModel.getShapeType() == ShapeType.Request;
    }

    private boolean isResponse() {
        return shapeModel.getShapeType() == ShapeType.Response;
    }

    private TypeName builderSuperInterface() {
        ClassName superInterface;
        switch (shapeModel.getShapeType()) {
            case Request:
                return new AwsServiceBaseRequestSpec(intermediateModel).className().nestedClass("Builder");
            case Response:
                return new AwsServiceBaseResponseSpec(intermediateModel).className().nestedClass("Builder");
            default:
                superInterface = ClassName.get(CopyableBuilder.class);
                break;
        }
        return ParameterizedTypeName.get(superInterface, classToBuild().nestedClass("Builder"), classToBuild());
    }

    private List<MethodSpec> exceptionMessageGetters() {
        List<MethodSpec> getters = new ArrayList<>();

        // bean style
        getters.add(MethodSpec.methodBuilder("getMessage")
                .addModifiers(Modifier.PUBLIC)
                .returns(String.class)
                .addStatement("return message")
                .build());

        getters.add(MethodSpec.methodBuilder("message")
                .addModifiers(Modifier.PUBLIC)
                .returns(String.class)
                .addStatement("return message")
                .build());

        return getters;
    }

    private List<MethodSpec> exceptionMessageSetters() {
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
