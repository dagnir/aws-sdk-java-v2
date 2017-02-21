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

package software.amazon.awssdk.codegen.poet;

import static software.amazon.awssdk.util.StringUtils.hasNonWhitespaceCharacter;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import java.util.function.Consumer;
import javax.annotation.Generated;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.codegen.model.intermediate.DocumentationModel;
import software.amazon.awssdk.codegen.model.intermediate.HasDeprecation;

public final class Utils {

    public static AnnotationSpec GENERATED = AnnotationSpec.builder(Generated.class)
                                                           .addMember("value",
                                                                      "$S",
                                                                      "software.amazon.awssdk:aws-java-sdk-code-generator")
                                                           .build();

    private Utils() {
    }

    public static MethodSpec.Builder toStringBuilder() {
        return MethodSpec.methodBuilder("toString")
                         .returns(String.class)
                         .addModifiers(Modifier.PUBLIC)
                         .addAnnotation(Override.class);
    }

    public static void addDeprecated(Consumer<Class<?>> builder, HasDeprecation deprecation) {
        if (deprecation.isDeprecated()) {
            addDeprecated(builder);
        }
    }

    public static void addDeprecated(Consumer<Class<?>> builder) {
        builder.accept(Deprecated.class);
    }

    public static void addJavadoc(Consumer<String> builder, String javadoc) {
        if (hasNonWhitespaceCharacter(javadoc)) {
            builder.accept(javadoc + (javadoc.endsWith("\n") ? "" : "\n"));
        }
    }

    public static void addJavadoc(Consumer<String> builder, DocumentationModel docModel) {
        addJavadoc(builder, docModel.getDocumentation());
    }

    public static TypeSpec.Builder createEnumBuilder(ClassName name) {
        return TypeSpec.enumBuilder(name).addAnnotation(GENERATED).addModifiers(Modifier.PUBLIC);
    }

    public static JavaFile buildJavaFile(ClassSpec spec) {
        JavaFile.Builder builder = JavaFile.builder(spec.className().packageName(), spec.poetSpec()).skipJavaLangImports(true);
        spec.staticImports().forEach(i -> i.memberNames().forEach(m -> builder.addStaticImport(i.className(), m)));
        return builder.build();
    }
}
