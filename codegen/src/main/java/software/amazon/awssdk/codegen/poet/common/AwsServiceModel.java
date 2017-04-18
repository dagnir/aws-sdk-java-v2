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

package software.amazon.awssdk.codegen.poet.common;

import static software.amazon.awssdk.codegen.poet.PoetUtils.escapeJavadoc;
import static software.amazon.awssdk.codegen.poet.PoetUtils.makeJavadocPoetFriendly;

import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.lang.model.element.Modifier;

import software.amazon.awssdk.AmazonWebServiceRequest;
import software.amazon.awssdk.AmazonWebServiceResult;
import software.amazon.awssdk.ResponseMetadata;
import software.amazon.awssdk.annotation.SdkInternalApi;
import software.amazon.awssdk.codegen.model.config.customization.ConvenienceTypeOverload;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.ListModel;
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

    private final IntermediateModel intermediateModel;
    private final ShapeModel shapeModel;
    private final PoetExtensions poetExtensions;
    private final TypeNameProvider typeNameProvider;

    private List<FieldSpec> fields = null;

    public AwsServiceModel(IntermediateModel intermediateModel, ShapeModel shapeModel) {
        this.intermediateModel = intermediateModel;
        this.shapeModel = shapeModel;
        poetExtensions = new PoetExtensions(this.intermediateModel);
        this.typeNameProvider = new TypeNameProvider(poetExtensions);
    }


    @Override
    public TypeSpec poetSpec() {
        TypeSpec.Builder specBuilder = TypeSpec.classBuilder(className())
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterfaces(modelSuperInterfaces())
                .superclass(modelSuperClass())
                .addMethods(modelClassMethods())
                .addFields(fields())
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

        if (implementStructuredPojoInterface(shapeModel)) {
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
                methodSpecs.addAll(memberGetters());
                break;
            default:
                break;
        }

        if (implementStructuredPojoInterface(shapeModel)) {
            methodSpecs.add(structuredPojoMarshallMethod(shapeModel));
        }

        return methodSpecs;
    }

    private List<FieldSpec> fields() {
        if (fields == null) {
            fields = members().stream()
                    .map(this::field)
                    .collect(Collectors.toList());
        }
        return fields;
    }

    private List<TypeSpec> nestedModelClassTypes() {
        switch (shapeModel.getShapeType()) {
            case Model:
            case Request:
            case Response:
                return Collections.singletonList(builderClass());
            default:
                break;
        }
        return Collections.emptyList();
    }

    private List<MethodSpec> memberGetters() {
        return members().stream().map(this::getter).collect(Collectors.toList());
    }

    private FieldSpec field(MemberModel memberModel) {
        return FieldSpec.builder(typeNameProvider.memberType(memberModel), memberModel.getVariable().getVariableName())
                .addModifiers(Modifier.PRIVATE)
                .build();
    }

    private TypeSpec builderClass() {
        ClassName builderClassName = className().nestedClass("Builder");
        TypeSpec.Builder builderClassBuilder = TypeSpec.classBuilder(builderClassName)
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC);

        builderClassBuilder.addFields(fields());

        // No-arg constructor
        builderClassBuilder.addMethod(MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PRIVATE)
                .build());

        // Accepts Model for copying
        MethodSpec.Builder copyBuilderCtor = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PRIVATE)
                .addParameter(className(), "model");

        members().forEach(m -> {
            String name = m.getVariable().getVariableName();
            copyBuilderCtor.addStatement("this.$N = model.$N()", name, name);
            builderClassBuilder.addMethod(getter(m));
            builderClassBuilder.addMethods(setters(m, builderClassName));
        });

        builderClassBuilder.addMethod(copyBuilderCtor.build());

        // FIXME: using 'build_' to avoid clashing with models that have a 'build' property
        builderClassBuilder.addMethod(MethodSpec.methodBuilder("build_")
                .addModifiers(Modifier.PUBLIC)
                .returns(className())
                .addStatement("return new $T(this)", className())
                .build());

        return builderClassBuilder.build();
    }

    private boolean implementStructuredPojoInterface(ShapeModel shapeModel) {
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

        fields().forEach(f -> ctorBuilder.addStatement("this.$N = builder.$N()", f, f));

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

        fields().forEach(field -> methodBuilder.addStatement("hashCode = 31 * hashCode + (($N() == null)? 0 : $N().hashCode())",
                field, field));


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

        fields().forEach(field -> {
            methodBuilder.beginControlFlow("if (other.$N() == null ^ this.$N() == null)", field, field)
                    .addStatement("return false")
                    .endControlFlow()

                    .beginControlFlow("if (other.$N() != null && !other.$N().equals(this.$N()))", field, field, field)
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
            String name = m.getVariable().getVariableName();
            toStringMethodBuilder.beginControlFlow("if ($N() != null)", name)
                    .addStatement("sb.append(\"$N: \").append($N()).append(\",\")", m.getName(), name)
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

    private MethodSpec getter(MemberModel memberModel) {
        final String name = memberModel.getVariable().getVariableName();
        return MethodSpec.methodBuilder(name)
                .addJavadoc(makeJavadocPoetFriendly(memberModel.getGetterDocumentation()))
                .addModifiers(Modifier.PUBLIC)
                .returns(typeNameProvider.memberType(memberModel))
                .addStatement("return $N", name)
                .build();
    }

    private List<MethodSpec> setters(MemberModel memberModel, TypeName returnType) {
        final String name = memberModel.getVariable().getVariableName();
        final FieldSpec field = field(memberModel);
        TypeName memberType = typeNameProvider.memberType(memberModel);
        List<MethodSpec> memberSetters = new ArrayList<>();

        final String javadoc = makeJavadocPoetFriendly(memberModel.getFluentSetterDocumentation());

        if (memberModel.isList()) {
            ListModel listModel = memberModel.getListModel();
            TypeName listMemberType = typeNameProvider.memberType(listModel.getListMemberModel());
            memberSetters.add(copySetter(field, ParameterizedTypeName.get(listImplClassName(), listMemberType), returnType,
                    javadoc));

            memberSetters.add(varargToListSetter(name, listMemberType, returnType, javadoc));

            if (memberModel.getEnumType() != null) {
                TypeName enumType = poetExtensions.getModelClass(memberModel.getEnumType());
                memberSetters.add(enumVarargToListSetter(name, enumType, returnType, javadoc));
            }
        } else if (memberModel.isMap()) {
            MethodSpec.Builder assignmentSetter = MethodSpec.methodBuilder(name)
                    .addJavadoc(javadoc)
                    .addModifiers(Modifier.PUBLIC)
                    .returns(returnType)
                    .addParameter(memberType, name)
                    .addStatement("this.$N = new $T<>($N)", name, mapImplClassName(), name)
                    .addStatement("return this");

            memberSetters.add(assignmentSetter.build());

        } else {
            // Direct assignment
            memberSetters.add(MethodSpec.methodBuilder(name)
                    .addJavadoc(javadoc)
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(memberType, name)
                    .returns(returnType)
                    .addStatement("this.$N = $N", name, name)
                    .addStatement("return this")
                    .build());

            // Enum param
            if (memberModel.getEnumType() != null) {
                memberSetters.add(MethodSpec.methodBuilder(name)
                        .addJavadoc(javadoc)
                        .addModifiers(Modifier.PUBLIC)
                        .returns(returnType)
                        .addParameter(poetExtensions.getModelClass(memberModel.getEnumType()), name)
                        .addStatement("this.$N = $N.toString()", name, name)
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

                    TypeName convenienceParamType = typeNameProvider.typeNameForSimpleType(c.getConvenienceType());
                    memberSetters.add(MethodSpec.methodBuilder(name)
                            .addJavadoc(javadoc)
                            .addModifiers(Modifier.PUBLIC)
                            .returns(returnType)
                            .addParameter(convenienceParamType, name)
                            .addStatement("this.$N = new $T().adapt($N)", name,
                                    PoetUtils.classNameFromFqcn(c.getTypeAdapterFqcn()), name)
                            .addStatement("return this")
                            .build());
                }
            }
        }

        return memberSetters;
    }

    private MethodSpec enumVarargToListSetter(String fieldName, TypeName enumTypeName, TypeName returnType, String javadoc) {
        return MethodSpec.methodBuilder(fieldName)
                .addJavadoc(javadoc)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ArrayTypeName.of(enumTypeName), fieldName).varargs(true)
                .returns(returnType)
                .beginControlFlow("if (this.$N == null)", fieldName)
                .addStatement("this.$N = new $T($N.length)", fieldName, ParameterizedTypeName.get(listImplClassName(),
                        ClassName.get(String.class)),
                        fieldName)
                .endControlFlow()
                .beginControlFlow("for ($T ele : $N)", enumTypeName, fieldName)
                .addStatement("this.$N.add(ele.toString())", fieldName)
                .endControlFlow()
                .addStatement("return this")
                .build();
    }

    private MethodSpec copySetter(FieldSpec field, TypeName copyTypeImpl, TypeName returnType, String javadoc) {
        return MethodSpec.methodBuilder(field.name)
                .addModifiers(Modifier.PUBLIC)
                .returns(returnType)
                .addParameter(field.type, field.name)
                .addJavadoc(javadoc)
                .beginControlFlow("if ($N == null)", field.name)
                .addStatement("this.$N = null", field.name)
                .endControlFlow()
                .beginControlFlow("else")
                .addStatement("this.$N = new $T($N)", field.name, copyTypeImpl, field.name)
                .endControlFlow()
                .addStatement("return this")
                .build();
    }

    private MethodSpec varargToListSetter(String fieldName, TypeName listMemberType, TypeName returnType, String javadoc) {
        return MethodSpec.methodBuilder(fieldName)
                .addJavadoc(javadoc)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ArrayTypeName.of(listMemberType), fieldName).varargs(true)
                .returns(returnType)
                .beginControlFlow("if (this.$N == null)", fieldName)
                .addStatement("this.$N = new $T<>($N.length)", fieldName, listImplClassName(), fieldName)
                .endControlFlow()
                .beginControlFlow("for ($T ele : $N)", listMemberType, fieldName)
                .addStatement("this.$N.add(ele)", fieldName)
                .endControlFlow()
                .addStatement("return this")
                .build();
    }

    private ClassName listImplClassName() {
        return ClassName.get(ArrayList.class);
    }

    private ClassName mapImplClassName() {
        return ClassName.get(HashMap.class);
    }

    private List<MemberModel> members() {
        if (shapeModel.getMembers() != null) {
            return shapeModel.getMembers();
        }
        return Collections.emptyList();
    }
}
