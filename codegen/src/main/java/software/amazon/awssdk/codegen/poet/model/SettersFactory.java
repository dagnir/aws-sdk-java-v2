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
import com.squareup.javapoet.TypeName;

import java.util.Collections;
import java.util.List;

import software.amazon.awssdk.codegen.model.intermediate.MemberModel;

class SettersFactory {
    private final TypeProvider typeProvider;

    public SettersFactory(TypeProvider typeProvider) {
        this.typeProvider = typeProvider;
    }

    public List<MethodSpec> fluentSetterDeclarations(MemberModel memberModel, TypeName returnType) {
        if (memberModel.isList()) {
            return new ListSetters(memberModel, typeProvider).fluentDeclarations(returnType);
        } else if (memberModel.isMap()) {
            return Collections.emptyList();
        }

        return new NonCollectionSetters(memberModel, typeProvider).fluentDeclarations(returnType);
    }

    public List<MethodSpec> fluentSetters(MemberModel memberModel, TypeName returnType) {
        if (memberModel.isList()) {
            return new ListSetters(memberModel, typeProvider).fluent(returnType);
        }

        if (memberModel.isMap()) {
            return new MapSetters(memberModel, typeProvider).fluent(returnType);
        }

        return new NonCollectionSetters(memberModel, typeProvider).fluent(returnType);
    }

    public List<MethodSpec> beanStyleSetters(MemberModel memberModel) {
        if (memberModel.isList()) {
            return new ListSetters(memberModel, typeProvider).beanStyle();
        }

        if (memberModel.isMap()) {
            return new MapSetters(memberModel, typeProvider).beanStyle();
        }

        return new NonCollectionSetters(memberModel, typeProvider).beanStyle();
    }
}
