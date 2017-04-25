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
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;

import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import software.amazon.awssdk.codegen.model.intermediate.ListModel;
import software.amazon.awssdk.codegen.model.intermediate.MapModel;
import software.amazon.awssdk.codegen.model.intermediate.MemberModel;
import software.amazon.awssdk.codegen.poet.PoetExtensions;

/**
 * Helper class for resolving Poet TypeNames for use in model classes.
 */
class TypeProvider {

    private final PoetExtensions poetExtensions;

    public TypeProvider(PoetExtensions poetExtensions) {
        this.poetExtensions = poetExtensions;
    }

    public ClassName listImplClassName() {
        return ClassName.get(ArrayList.class);
    }

    public ClassName mapImplClassName() {
        return ClassName.get(HashMap.class);
    }

    public TypeName getStorageType(MemberModel memberModel) {
        if (memberModel.isSimple()) {
            return getTypeNameForSimpleType(memberModel.getVariable().getSimpleType());
        } else if (memberModel.isList()) {
            ListModel listModel = memberModel.getListModel();
            return ParameterizedTypeName.get(ClassName.get(List.class), getStorageType(listModel.getListMemberModel()));
        } else if (memberModel.isMap()) {
            MapModel mapModel = memberModel.getMapModel();
            TypeName keyType;
            if (mapModel.isKeySimple()) {
                keyType = getTypeNameForSimpleType(mapModel.getKeyType());
            } else {
                keyType = getStorageType(mapModel.getKeyModel());
            }

            TypeName valueType;
            if (mapModel.isValueSimple()) {
                valueType = getTypeNameForSimpleType(mapModel.getValueType());
            } else {
                valueType = getStorageType(mapModel.getValueModel());
            }
            return ParameterizedTypeName.get(ClassName.get(Map.class), keyType, valueType);
        }
        return poetExtensions.getModelClass(memberModel.getC2jShape());
    }

    public TypeName getTypeNameForSimpleType(String simpleType) {
        switch (simpleType) {
            case "Object":
                return TypeName.OBJECT;
            case "String":
                return ClassName.get(String.class);
            case "Boolean":
                return ClassName.get(Boolean.class);
            case "Integer":
                return ClassName.get(Integer.class);
            case "Long":
                return ClassName.get(Long.class);
            case "Short":
                return ClassName.get(Short.class);
            case "Byte":
                return ClassName.get(Byte.class);
            case "BigInteger":
                return ClassName.get(BigInteger.class);
            case "Double":
                return ClassName.get(Double.class);
            case "Float":
                return ClassName.get(Float.class);
            case "BigDecimal":
                return ClassName.get(BigDecimal.class);
            case "ByteBuffer":
                return ClassName.get(ByteBuffer.class);
            case "InputStream":
                return ClassName.get(InputStream.class);
            case "Date":
                return ClassName.get(Date.class);
            default:
                throw new RuntimeException("Unsupported simple type " + simpleType);
        }
    }
}
