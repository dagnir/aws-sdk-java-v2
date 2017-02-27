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
package software.amazon.awssdk.internal.config;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

import org.junit.Test;

/**
 * This is an InternalTest that is not meant to be published in GitHub
 */
public class InternalConfigLoaderInternalTest {

    /*
     * There are three copies of the awssdk_config_default.json config file on the classpath
     * (1) the real version located at {classpath-root}/com/amazonaws/internal/config/awssdk_config_default.json
     * (2) a fake version located at {classpath-root}
     * (3) a jarjar-ed version located at {classpath-root}/jarjar/com/amazonaws/internal/config/awssdk_config_default.json
     */

    @Test
    public void testConfigLoader_RealCoreLoadFromRealConfig() {
        InternalConfig loaded = InternalConfig.Factory.getInternalConfig();

        assertThat(
                loaded.getDefaultConfigFileLocation().getPath(),
                endsWith("/software/amazon/awssdk/internal/config/awssdk_config_default.json"));
        assertThat(
                loaded.getDefaultConfigFileLocation().getPath(),
                not(endsWith("/jarjar/software/amazon/awssdk/internal/config/awssdk_config_default.json")));
    }

    // FIXME(dongie): commented this out for now. Should we just delete?
//    @Test
//    public void testConfigLoader_JarjarCoreLoadFromJarjarConfig() {
//        jarjar.com.amazonaws.internal.config.InternalConfig loaded = jarjar.com.amazonaws.internal.config.InternalConfig.Factory
//                .getInternalConfig();
//
//        assertThat(
//                loaded.getDefaultConfigFileLocation().getPath(),
//                endsWith("/jarjar/com/amazonaws/internal/config/awssdk_config_default.json"));
//    }

}