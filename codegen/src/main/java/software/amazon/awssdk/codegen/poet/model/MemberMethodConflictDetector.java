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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.MemberModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;

public class MemberMethodConflictDetector {
    private final IntermediateModel intermediateModel;

    public MemberMethodConflictDetector(IntermediateModel intermediateModel) {
        this.intermediateModel = intermediateModel;
    }

    List<String> conflictingMembers(ShapeModel shapeModel) {
        Set<String> superMethods = superMethods(shapeModel);

        List<String> conflictingMethods = new ArrayList<>();

        if (shapeModel.getMembers() != null) {
            for (MemberModel m : shapeModel.getMembers()) {
                String setterName = m.getSetterMethodName();
                String getterName = m.getGetterMethodName();

                if (superMethods.contains(setterName)) {
                    conflictingMethods.add(setterName);
                }

                if (superMethods.contains(getterName)) {
                    conflictingMethods.add(getterName);
                }
            }
        }

        return conflictingMethods;
    }

    private Set<String> superMethods(ShapeModel shapeModel) {
        ShapeInterfaceProvider shapeInterfaceProvider = shapeInterfaceProviderFor(shapeModel);

        Set<Class<?>> superInterfaces = shapeInterfaceProvider.interfacesToImplement();
        Class<?> baseClass = shapeInterfaceProvider.baseClassToExtend();

        return Stream.concat(
                superInterfaces.stream().flatMap(iface -> Arrays.stream(iface.getMethods())),
                Arrays.stream(baseClass.getMethods()))
                .map(Method::getName)
                .collect(Collectors.toSet());
    }

    private ShapeInterfaceProvider shapeInterfaceProviderFor(ShapeModel shapeModel) {
        return new AwsShapeInterfaceProvider(intermediateModel, shapeModel);
    }
}
