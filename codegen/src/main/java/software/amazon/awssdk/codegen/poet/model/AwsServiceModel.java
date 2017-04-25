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

import static software.amazon.awssdk.codegen.poet.PoetUtils.escapeJavadoc;
import static software.amazon.awssdk.codegen.poet.PoetUtils.makeJavadocPoetFriendly;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.lang.model.element.Modifier;

import software.amazon.awssdk.annotation.SdkInternalApi;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.MemberModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.PoetExtensions;
import software.amazon.awssdk.protocol.ProtocolMarshaller;
import software.amazon.awssdk.protocol.StructuredPojo;

/**
 * Provides the Poet specs for AWS Service models.
 */
public class AwsServiceModel implements ClassSpec {
    private static final MethodSpec EXCEPTION_CTOR = MethodSpec.constructorBuilder()
            .addModifiers(Modifier.PUBLIC)
            .addParameter(String.class, "message")
            .addStatement("super($N)", "message")
            .build();

    private final IntermediateModel intermediateModel;
    private final ShapeModel shapeModel;
    private final PoetExtensions poetExtensions;
    private final TypeProvider typeProvider;
    private final ShapeModelSpec shapeModelSpec;
    private final ShapeInterfaceProvider interfaceProvider;
    private final ModelMethodOverrides modelMethodOverrides;

    public AwsServiceModel(IntermediateModel intermediateModel, ShapeModel shapeModel) {
        this.intermediateModel = intermediateModel;
        this.shapeModel = shapeModel;
        this.poetExtensions = new PoetExtensions(this.intermediateModel);
        this.typeProvider = new TypeProvider(this.poetExtensions);
        this.shapeModelSpec = new ShapeModelSpec(this.shapeModel, typeProvider);
        this.interfaceProvider = new AwsShapeInterfaceProvider(this.intermediateModel, this.shapeModel);
        this.modelMethodOverrides = new ModelMethodOverrides(this.poetExtensions);
    }

    @Override
    public TypeSpec poetSpec() {
        TypeSpec.Builder specBuilder = TypeSpec.classBuilder(shapeModel.getShapeName())
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterfaces(modelSuperInterfaces())
                .superclass(modelSuperClass())
                .addMethods(modelClassMethods())
                .addFields(shapeModelSpec.fields())
                .addTypes(nestedModelClassTypes());

        if (shapeModel.getDocumentation() != null) {
            specBuilder.addJavadoc(escapeJavadoc(shapeModel.getDocumentation()));
        }

        return specBuilder.build();
    }

    @Override
    public ClassName className() {
        return poetExtensions.getModelClass(shapeModel.getShapeName());
    }

    private List<TypeName> modelSuperInterfaces() {
        return interfaceProvider.interfacesToImplement().stream()
                .map(ClassName::get)
                .collect(Collectors.toList());
    }

    private TypeName modelSuperClass() {
        return ClassName.get(interfaceProvider.baseClassToExtend());
    }

    private List<MethodSpec> modelClassMethods() {
        List<MethodSpec> methodSpecs = new ArrayList<>();

        switch (shapeModel.getShapeType()) {
            case Exception:
                methodSpecs.add(EXCEPTION_CTOR);
                break;
            default:
                methodSpecs.addAll(memberGetters());
                methodSpecs.add(builderCtor());
                methodSpecs.add(builderMethod());
                methodSpecs.add(toBuilderMethod());
                methodSpecs.add(modelMethodOverrides.hashCodeMethod(shapeModel));
                methodSpecs.add(modelMethodOverrides.equalsMethod(shapeModel));
                methodSpecs.add(modelMethodOverrides.cloneMethod(className()));
                methodSpecs.add(modelMethodOverrides.toStringMethod(shapeModel));
                break;
        }

        if (implementStructuredPojoInterface()) {
            methodSpecs.add(structuredPojoMarshallMethod(shapeModel));
        }

        return methodSpecs;
    }

    private List<MethodSpec> memberGetters() {
        return members().stream()
                .map(m -> {
                    String javadoc = makeJavadocPoetFriendly(m.getGetterDocumentation());
                    return MethodSpec
                            .methodBuilder(m.getGetterMethodName())
                            .addJavadoc(makeJavadocPoetFriendly(javadoc))
                            .returns(typeProvider.getStorageType(m))
                            .addModifiers(Modifier.PUBLIC)
                            .addStatement("return $N", m.getVariable().getVariableName())
                            .build();
                })
                .collect(Collectors.toList());
    }

    private List<TypeSpec> nestedModelClassTypes() {
        List<TypeSpec> nestedClasses = new ArrayList<>();
        switch (shapeModel.getShapeType()) {
            case Model:
            case Request:
            case Response:
                nestedClasses.add(builderClass());
                break;
            default:
                break;
        }
        return nestedClasses;
    }

    private TypeSpec builderClass() {
        return new ModelBuilderSpec(intermediateModel, shapeModel, shapeModelSpec, typeProvider, poetExtensions).poetSpec();
    }

    private boolean implementStructuredPojoInterface() {
        return interfaceProvider.shouldImplementInterface(StructuredPojo.class);
    }

    private MethodSpec structuredPojoMarshallMethod(ShapeModel shapeModel) {
        return MethodSpec.methodBuilder("marshall")
                .addAnnotation(SdkInternalApi.class)
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ProtocolMarshaller.class, "protocolMarshaller")
                .addStatement("$T.getInstance().marshall(this, $N)",
                        poetExtensions.getTransformClass(shapeModel.getShapeName() + "Marshaller"),
                        "protocolMarshaller")
                .build();
    }

    private MethodSpec builderCtor() {
        MethodSpec.Builder ctorBuilder = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PRIVATE)
                .addParameter(className().nestedClass("Builder"), "builder");

        shapeModelSpec.fields().forEach(f -> ctorBuilder.addStatement("this.$N = builder.$N", f, f));

        return ctorBuilder.build();
    }

    private MethodSpec builderMethod() {
        ClassName builderClassName = className().nestedClass("Builder");
        return MethodSpec.methodBuilder("builder_")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(builderClassName)
                .addStatement("return new $T()", builderClassName)
                .build();
    }

    private MethodSpec toBuilderMethod() {
        ClassName builderClassName = className().nestedClass("Builder");
        return MethodSpec.methodBuilder("toBuilder")
                .addModifiers(Modifier.PUBLIC)
                .returns(builderClassName)
                .addStatement("return new $T(this)", builderClassName)
                .build();
    }

    private List<MemberModel> members() {
        if (shapeModel.getMembers() != null) {
            return shapeModel.getMembers();
        }
        return Collections.emptyList();
    }
}