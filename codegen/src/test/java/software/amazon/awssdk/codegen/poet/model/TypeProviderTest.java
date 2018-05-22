/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.codegen.C2jModels;
import software.amazon.awssdk.codegen.IntermediateModelBuilder;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.service.ServiceModel;
import software.amazon.awssdk.codegen.utils.ModelLoaderUtils;
import software.amazon.awssdk.core.util.DefaultSdkAutoConstructAwareList;

import java.io.File;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * Tests for {@link TypeProvider}
 */
public class TypeProviderTest {
    private static IntermediateModel intermediateModel;

    @BeforeClass
    public static void setUp() throws IOException {
        File serviceModelFile = new File(AwsModelSpecTest.class.getResource("service-2.json").getFile());
        File customizationConfigFile = new File(AwsModelSpecTest.class.getResource("customization.config").getFile());
        CustomizationConfig customizationConfig = ModelLoaderUtils.loadModel(CustomizationConfig.class, customizationConfigFile);
        customizationConfig.setUseAutoConstructList(true);

        intermediateModel = new IntermediateModelBuilder(
                C2jModels.builder()
                        .serviceModel(ModelLoaderUtils.loadModel(ServiceModel.class, serviceModelFile))
                        .customizationConfig(customizationConfig)
                        .build())
                .build();
    }
    @Test
    public void returnsSdkAutoConstructListIfCustomizationEnabled() {
        TypeProvider typeProvider = new TypeProvider(intermediateModel);
        assertThat(typeProvider.listImplClassName()).isEqualTo(ClassName.get(DefaultSdkAutoConstructAwareList.class));
    }
}
