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
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.lang.model.element.Modifier;

import software.amazon.awssdk.AmazonWebServiceRequest;
import software.amazon.awssdk.AmazonWebServiceResult;
import software.amazon.awssdk.ResponseMetadata;
import software.amazon.awssdk.annotation.SdkInternalApi;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.MemberModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeType;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.PoetExtensions;
import software.amazon.awssdk.codegen.poet.PoetUtils;
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

    private static final Set<String> RESERVED_METHOD_NAMES = Arrays.stream(Object.class.getMethods())
            .map(Method::getName)
            .collect(Collectors.toSet());

    private final IntermediateModel intermediateModel;
    private final ShapeModel shapeModel;
    private final PoetExtensions poetExtensions;
    private final TypeProvider typeProvider;
    private final ShapeModelSpec shapeModelSpec;

    public AwsServiceModel(IntermediateModel intermediateModel, ShapeModel shapeModel) {
        this.intermediateModel = intermediateModel;
        this.shapeModel = shapeModel;
        this.poetExtensions = new PoetExtensions(this.intermediateModel);
        this.typeProvider = new TypeProvider(this.poetExtensions);
        this.shapeModelSpec = new ShapeModelSpec(this.shapeModel, typeProvider);
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

        errorIfGetterMethodsConflict();

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
        List<TypeName> superInterfaces = new ArrayList<>();

        switch (shapeModel.getShapeType()) {
            case Request:
            case Model:
                Stream.of(Serializable.class, Cloneable.class)
                        .map(ClassName::get)
                        .forEach(superInterfaces::add);
                break;
            default:
                break;
        }

        if (implementStructuredPojoInterface()) {
            superInterfaces.add(ClassName.get(StructuredPojo.class));
        }

        return superInterfaces;
    }

    private TypeName modelSuperClass() {
        switch (shapeModel.getShapeType()) {
            case Request:
                return ClassName.get(AmazonWebServiceRequest.class);
            case Response: {
                String responseClassFqcn;
                if ((responseClassFqcn = intermediateModel.getCustomizationConfig()
                        .getCustomResponseMetadataClassName()) != null) {
                    return PoetUtils.classNameFromFqcn(responseClassFqcn);
                }
                return ParameterizedTypeName.get(ClassName.get(AmazonWebServiceResult.class),
                        ClassName.get(ResponseMetadata.class));
            }
            case Exception: {
                String customExceptionBase;
                if ((customExceptionBase = intermediateModel.getCustomizationConfig()
                        .getSdkModeledExceptionBaseClassName()) != null) {
                    return poetExtensions.getModelClass(customExceptionBase);
                }
                return poetExtensions.getModelClass(intermediateModel.getMetadata().getSyncInterface() + "Exception");
            }
            case Model:
            default:
                return TypeName.OBJECT;
        }
    }

    private List<MethodSpec> modelClassMethods() {
        List<MethodSpec> methodSpecs = new ArrayList<>();

        switch (shapeModel.getShapeType()) {
            case Model:
            case Request:
            case Response:
                methodSpecs.addAll(memberGetters());
                methodSpecs.add(builderCtor());
                methodSpecs.add(builderMethod());
                methodSpecs.add(toBuilderMethod());
                methodSpecs.add(hashCodeMethod());
                methodSpecs.add(equalsMethod());
                methodSpecs.add(cloneMethod());
                methodSpecs.add(toStringMethod());
                break;
            case Exception:
                methodSpecs.add(EXCEPTION_CTOR);
                break;
            default:
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

    private void errorIfGetterMethodsConflict() {
        List<String> conflictingMethods = memberGetters().stream()
                .map(m -> m.name)
                .filter(this::isMemberMethodConflicting)
                .collect(Collectors.toList());

        if (!conflictingMethods.isEmpty()) {
            String msg = String.format("Shape '%s' contains members whose generated getters would conflict with" +
                            " member methods from its superclass and/or superinterfaces: [%s]. Address this by customizing the" +
                            " member name(s) in the customization file for this service module.",
                    shapeModel.getShapeName(),
                    conflictingMethods.stream().collect(Collectors.joining(", ")));

            throw new IllegalStateException(msg);
        }
    }

    private TypeSpec builderClass() {
        return new ModelBuilderSpec(intermediateModel, shapeModel, shapeModelSpec, typeProvider, poetExtensions).poetSpec();
    }

    private boolean implementStructuredPojoInterface() {
        return shapeModel.getShapeType() == ShapeType.Model && intermediateModel.getMetadata().isJsonProtocol();
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

    private MethodSpec hashCodeMethod() {
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("hashCode")
                .returns(int.class)
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .addStatement("int hashCode = 1");

        members().forEach(m ->
                methodBuilder.addStatement("hashCode = 31 * hashCode + (($N() == null)? 0 : $N().hashCode())",
                m.getGetterMethodName(), m.getGetterMethodName()));


        methodBuilder.addStatement("return hashCode");

        return methodBuilder.build();
    }

    private MethodSpec equalsMethod() {
        ClassName className = className();
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("equals")
                .returns(boolean.class)
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(Object.class, "obj")

                .beginControlFlow("if (this == obj)")
                .addStatement("return true")
                .endControlFlow()

                .beginControlFlow("if (obj == null)")
                .addStatement("return false")
                .endControlFlow()

                .beginControlFlow("if (!(obj instanceof $T))", className)
                .addStatement("return false")
                .endControlFlow()

                .addStatement("$T other = ($T) obj", className, className);

        members().forEach(m -> {
            String getterName = m.getGetterMethodName();
            methodBuilder.beginControlFlow("if (other.$N() == null ^ this.$N() == null)", getterName, getterName)
                    .addStatement("return false")
                    .endControlFlow()

                    .beginControlFlow("if (other.$N() != null && !other.$N().equals(this.$N()))", getterName, getterName,
                            getterName)
                    .addStatement("return false")
                    .endControlFlow();
        });

        methodBuilder.addStatement("return true");

        return methodBuilder.build();
    }

    private MethodSpec toStringMethod() {
        MethodSpec.Builder toStringMethodBuilder = MethodSpec.methodBuilder("toString")
                .returns(String.class)
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .addStatement("$T sb = new $T()", StringBuilder.class, StringBuilder.class)
                .addStatement("sb.append(\"{\")");

        members().forEach(m -> {
            String getterName = m.getGetterMethodName();
            toStringMethodBuilder.beginControlFlow("if ($N() != null)", getterName)
                    .addStatement("sb.append(\"$N: \").append($N()).append(\",\")", m.getName(), getterName)
                    .endControlFlow();
        });

        toStringMethodBuilder.addStatement("sb.append(\"}\")");
        toStringMethodBuilder.addStatement("return sb.toString()");

        return toStringMethodBuilder.build();
    }

    private MethodSpec cloneMethod() {
        ClassName className = className();
        return MethodSpec.methodBuilder("clone")
                .returns(className)
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .beginControlFlow("try")
                .addStatement("return ($T) super.clone()", className)
                .endControlFlow()
                .beginControlFlow("catch ($T e)", Exception.class)
                .addStatement("throw new IllegalStateException(\"Got a $N from Object.clone() even though we're Cloneable!\", e)",
                        CloneNotSupportedException.class.getSimpleName())
                .endControlFlow()
                .build();
    }

    private List<MemberModel> members() {
        if (shapeModel.getMembers() != null) {
            return shapeModel.getMembers();
        }
        return Collections.emptyList();
    }

    private boolean isMemberMethodConflicting(String methodName) {
        return RESERVED_METHOD_NAMES.contains(methodName);
    }
}
