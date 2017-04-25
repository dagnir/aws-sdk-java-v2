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

import com.squareup.javapoet.FieldSpec;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.lang.model.element.Modifier;

import software.amazon.awssdk.codegen.model.intermediate.MemberModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;

/**
 * Provides Poet specs related to shape models.
 */
class ShapeModelSpec {
    private final ShapeModel shapeModel;
    private final TypeProvider typeProvider;

    public ShapeModelSpec(ShapeModel shapeModel, TypeProvider typeProvider) {
        this.shapeModel = shapeModel;
        this.typeProvider = typeProvider;
    }

    public List<FieldSpec> fields() {
        return members().stream()
                .map(this::asField)
                .collect(Collectors.toList());
    }


    public FieldSpec asField(MemberModel memberModel) {
        return FieldSpec.builder(typeProvider.getStorageType(memberModel), memberModel.getVariable().getVariableName())
                .addModifiers(Modifier.PRIVATE)
                .build();
    }

    private List<MemberModel> members() {
        if (shapeModel.getMembers() != null) {
            return shapeModel.getMembers();
        }
        return Collections.emptyList();
    }
}
