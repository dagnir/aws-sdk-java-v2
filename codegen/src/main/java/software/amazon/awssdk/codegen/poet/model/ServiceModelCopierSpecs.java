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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.MapModel;
import software.amazon.awssdk.codegen.model.intermediate.MemberModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.PoetExtensions;

public class ServiceModelCopierSpecs {
    private final IntermediateModel intermediateModel;
    private final PoetExtensions poetExtensions;
    private final TypeProvider typeProvider;

    public ServiceModelCopierSpecs(IntermediateModel intermediateModel) {
        this.intermediateModel = intermediateModel;
        this.poetExtensions = new PoetExtensions(intermediateModel);
        this.typeProvider = new TypeProvider(this.poetExtensions);
    }

    public Collection<ClassSpec> copierSpecs() {
        Map<ClassName, ClassSpec> specs = new HashMap<>();
        allShapeMembers().values().stream()
                .map(m -> new MemberCopierSpec(typeProvider, m, poetExtensions))
                .forEach(spec -> specs.put(spec.className(), spec));

        return specs.values();
    }

    private Map<String, MemberModel> allShapeMembers() {
        Map<String, MemberModel> shapeMembers = new HashMap<>();
        intermediateModel.getShapes().values().stream()
                .flatMap(s -> s.getMembersAsMap().values().stream())
                .forEach(m -> shapeMembers.put(m.getC2jShape(), m));

        // Step above only gives us the top level shape members.
        // List and Map members have members of their own, so find those too.
        Map<String, MemberModel> allMembers = new HashMap<>(shapeMembers);

        shapeMembers.values().forEach(m -> putMembersOfMember(m, allMembers));

        return allMembers;
    }

    private void putMembersOfMember(MemberModel memberModel, Map<String, MemberModel> allMembers) {
        if (memberModel.isList()) {
            MemberModel listMember = memberModel.getListModel().getListMemberModel();
            allMembers.put(listMember.getC2jShape(), listMember);
            putMembersOfMember(listMember, allMembers);
        } else if (memberModel.isMap()) {
            MapModel mapModel = memberModel.getMapModel();
            // NOTE: keys are always simple, so don't bother checking
            if (!mapModel.isValueSimple()) {
                MemberModel valueMember = mapModel.getValueModel();
                allMembers.put(valueMember.getC2jShape(), valueMember);
                putMembersOfMember(valueMember, allMembers);
            }
        }
    }
}
