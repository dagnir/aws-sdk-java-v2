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
package software.amazon.awssdk.codegen.poet.client;

import org.junit.Test;
import software.amazon.awssdk.codegen.C2jModels;
import software.amazon.awssdk.codegen.IntermediateModelBuilder;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.service.ServiceModel;
import software.amazon.awssdk.codegen.model.service.Waiters;
import software.amazon.awssdk.codegen.utils.ModelLoaderUtils;

import java.io.File;

import static org.hamcrest.MatcherAssert.assertThat;
import static software.amazon.awssdk.codegen.poet.PoetMatchers.generatesTo;


public class SyncClientClassTest {

    @Test
    public void syncClientClassJson() throws Exception {
        File serviceModel = new File(getClass().getResource("c2j/json/service-2.json").getFile());
        File customizationModel = new File(getClass().getResource("c2j/json/customization.config").getFile());
        C2jModels models = C2jModels.builder().serviceModel(getServiceModel(serviceModel)).customizationConfig(getCustomizationConfig(customizationModel)).build();

        IntermediateModel model = new IntermediateModelBuilder(models).build();

        SyncClientClass syncClientClass = new SyncClientClass(model);
        assertThat(syncClientClass, generatesTo("test-json-client-class.java"));
    }

    @Test
    public void syncClientClassQuery() throws Exception {

        File serviceModel = new File(getClass().getResource("c2j/query/service-2.json").getFile());
        File customizationModel = new File(getClass().getResource("c2j/query/customization.config").getFile());
        File waitersModel = new File(getClass().getResource("c2j/query/waiters-2.json").getFile());

        C2jModels models = C2jModels
                .builder()
                .serviceModel(getServiceModel(serviceModel))
                .customizationConfig(getCustomizationConfig(customizationModel))
                .waitersModel(getWaiters(waitersModel))
                .build();

        IntermediateModel model = new IntermediateModelBuilder(models).build();

        SyncClientClass syncClientClass = new SyncClientClass(model);
        assertThat(syncClientClass, generatesTo("test-query-client-class.java"));
    }

    private ServiceModel getServiceModel(File file) {
        return ModelLoaderUtils.loadModel(ServiceModel.class, file);
    }

    private CustomizationConfig getCustomizationConfig(File file) {
        return ModelLoaderUtils.loadModel(CustomizationConfig.class, file);
    }

    private Waiters getWaiters(File file) {
        return ModelLoaderUtils.loadModel(Waiters.class, file);
    }
}
