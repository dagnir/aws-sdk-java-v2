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

import com.squareup.javapoet.ClassName;
import software.amazon.awssdk.codegen.internal.Constants;
import software.amazon.awssdk.codegen.internal.Utils;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;

/**
 * Extension and convenience methods to Poet that use the intermediate model.
 */
public class PoetExtensions {

    private final IntermediateModel model;

    public PoetExtensions(IntermediateModel model) {
        this.model = model;
    }

    /**
     * @param className Simple name of class in model package.
     * @return A Poet {@link ClassName} for the given class in the model package.
     */
    public ClassName getModelClass(String className) {
        return ClassName.get(appendPackageComponents(Constants.PACKAGE_NAME_MODEL_SUFFIX), className);
    }

    /**
     * @param className Simple name of class in transform package.
     * @return A Poet {@link ClassName} for the given class in the transform package.
     */
    public ClassName getTransformClass(String className) {
        final String transformPackage = appendPackageComponents(
                Constants.PACKAGE_NAME_MODEL_SUFFIX,
                Utils.directoryToPackage(model.getCustomizationConfig().getTransformDirectory()));
        return ClassName.get(transformPackage, className);
    }

    /**
     * @param className Simple name of class in waiters package.
     * @return A Poet {@link ClassName} for the given class in the waiters package.
     */
    public ClassName getWaiterClass(String className) {
        return ClassName.get(appendPackageComponents(Constants.PACKAGE_NAME_WAITERS_SUFFIX), className);
    }

    /**
     * @param className Simple name of class in base service package (i.e. software.amazon.awssdk.services.dynamodb).
     * @return A Poet {@link ClassName} for the given class in the base service package.
     */
    public ClassName getTopLevelClass(String className) {
        return ClassName.get(model.getMetadata().getPackageName(), className);
    }

    /**
     * Append the package components to the base service package.
     *
     * @param components Package components to add
     */
    private String appendPackageComponents(String... components) {
        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(model.getMetadata().getPackageName());
        for (String component : components) {
            stringBuilder.append(".").append(component);
        }
        return stringBuilder.toString();
    }
}
