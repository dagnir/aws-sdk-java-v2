/*
 * Copyright 2011-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.codegen.emitters;

import software.amazon.awssdk.codegen.internal.Constants;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;

/**
 * Common paths used by generator tasks.
 */
public class GeneratorPathProvider {

    private final IntermediateModel model;
    private final String sourceDirectory;
    private final String testDirectory;

    public GeneratorPathProvider(IntermediateModel model, String sourceDirectory, String testDirectory) {
        this.model = model;
        this.sourceDirectory = sourceDirectory;
        this.testDirectory = testDirectory;
    }

    public String getModelDirectory() {
        return getBasePackageDirectory() + "/" + Constants.PACKAGE_NAME_MODEL_SUFFIX;
    }

    public String getTransformDirectory() {
        return getModelDirectory() + "/" + model.getCustomizationConfig().getTransformDirectory();
    }

    public String getBasePackageDirectory() {
        return sourceDirectory + "/" + getPackagePath();
    }

    public String getSmokeTestDirectory() {
        return String.format("%s/%s/%s",
                             testDirectory,
                             getPackagePath(),

                             Constants.SMOKE_TESTS_DIR_NAME);
    }

    public String getWaitersDirectory() {
        return getBasePackageDirectory() + "/" + Constants.PACKAGE_NAME_WAITERS_SUFFIX;
    }

    public String getPolicyEnumDirectory() {
        return sourceDirectory + "/" + Constants.AUTH_POLICY_ENUM_CLASS_DIR;
    }

    public String getAuthorizerDirectory() {
        return getBasePackageDirectory() + "/" + Constants.PACKAGE_NAME_CUSTOM_AUTH_SUFFIX;
    }

    private String getPackagePath() {
        return model.getMetadata().getPackagePath();
    }
}
