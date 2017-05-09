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

import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;

import java.util.Collections;
import java.util.List;

import software.amazon.awssdk.codegen.model.intermediate.MemberModel;

class MapSetters extends AbstractMemberSetters {
    private final TypeProvider typeProvider;

    MapSetters(MemberModel memberModel, TypeProvider typeProvider) {
        super(memberModel, typeProvider);
        this.typeProvider = typeProvider;
    }

    public List<MethodSpec> fluentDeclarations(TypeName returnType) {
        return Collections.singletonList(fluentSetterDeclaration(memberAsParameter(), returnType).build());
    }

    @Override
    public List<MethodSpec> fluent(TypeName returnType) {
        return Collections.singletonList(fluentSetterBuilder(returnType)
                .addCode(copySetterBody(ParameterizedTypeName.get(typeProvider.mapImplClassName(), keyType(), valueType()))
                        .toBuilder()
                        .addStatement("return this").build())
                .build());
    }

    @Override
    public List<MethodSpec> beanStyle() {
        return Collections.singletonList(beanStyleSetterBuilder()
                .addCode(copySetterBody(ParameterizedTypeName.get(typeProvider.mapImplClassName(), keyType(), valueType())))
                .build());
    }

    private TypeName keyType() {
        return typeProvider.getTypeNameForSimpleType(memberModel().getMapModel().getKeyType());
    }

    private TypeName valueType() {
        if (memberModel().getMapModel().isValueSimple()) {
            return typeProvider.getTypeNameForSimpleType(memberModel().getMapModel().getValueType());
        }
        return typeProvider.type(memberModel().getMapModel().getValueModel());
    }
}
