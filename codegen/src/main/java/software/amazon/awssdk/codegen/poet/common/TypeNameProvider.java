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

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;

import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import software.amazon.awssdk.codegen.model.intermediate.ListModel;
import software.amazon.awssdk.codegen.model.intermediate.MapModel;
import software.amazon.awssdk.codegen.model.intermediate.MemberModel;
import software.amazon.awssdk.codegen.poet.PoetExtensions;

/**
 * Helper class to map the type of a model member to a Poet {@link TypeName}.
 */
public class TypeNameProvider {
    private final PoetExtensions poetExtensions;

    /**
     * Construct an instance of this class.
     *
     * @param poetExtensions The extensions for resolving names.
     */
    public TypeNameProvider(PoetExtensions poetExtensions) {
        this.poetExtensions = poetExtensions;
    }

    /**
     * Get the {@link TypeName} of a member model.
     *
     * @param memberModel The member model.
     *
     * @return The TypeName.
     */
    public TypeName memberType(MemberModel memberModel) {
        if (memberModel.isSimple()) {
            return typeNameForSimpleType(memberModel.getVariable().getSimpleType());
        } else if (memberModel.isList()) {
            ListModel listModel = memberModel.getListModel();
            return ParameterizedTypeName.get(ClassName.get(List.class), memberType(listModel.getListMemberModel()));
        } else if (memberModel.isMap()) {
            MapModel mapModel = memberModel.getMapModel();
            TypeName keyType;
            if (mapModel.isKeySimple()) {
                keyType = typeNameForSimpleType(mapModel.getKeyType());
            } else {
                keyType = memberType(mapModel.getKeyModel());
            }

            TypeName valueType;
            if (mapModel.isValueSimple()) {
                valueType = typeNameForSimpleType(mapModel.getValueType());
            } else {
                valueType = memberType(mapModel.getValueModel());
            }
            return ParameterizedTypeName.get(ClassName.get(Map.class), keyType, valueType);
        }
        return poetExtensions.getModelClass(memberModel.getC2jShape());
    }

    /**
     * Get the {@code TypeName} of a simple type.
     *
     * @param simpleType The name (fully qualified or simple) of the simple type class.
     *
     * @return The TypeName.
     */
    public TypeName typeNameForSimpleType(String simpleType) {
        return Stream.of(Object.class,
                String.class,
                Boolean.class,
                Integer.class,
                Long.class,
                Short.class,
                Byte.class,
                BigInteger.class,
                Double.class,
                Float.class,
                BigDecimal.class,
                ByteBuffer.class,
                InputStream.class,
                Date.class)
                .filter(cls -> cls.getSimpleName().equals(simpleType) || cls.getName().equals(simpleType))
                .map(ClassName::get)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Unsupported simple type: " + simpleType));
    }
}
