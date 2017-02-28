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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.net.URL;

import org.junit.BeforeClass;
import org.junit.Test;

import software.amazon.awssdk.util.ClassLoaderHelper;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

public class InternalConfigOverrideInternalTest {

    private static InternalConfig config = null;

    private static final String S3_SIGNER_TYPE = "S3SignerType";

    private static final String S3_SERVICE_NAME = "s3";

    @BeforeClass
    public static void setUp() throws JsonParseException, JsonMappingException,
            IOException {
        config = InternalConfig.Factory.getInternalConfig();
        assertNotNull(config);
    }

    @Test
    public void loadFromOverrideFile() throws Exception {
        loadFrom(InternalConfig.CONFIG_OVERRIDE_RESOURCE);
    }

    /**
     * This test case tests the Amazon S3 specific signers for internal regions.
     */
    @Test
    public void testS3Signers() {
        final String region = "aws-master";
        // https://tt.amazon.com/0043987527
        SignerConfig signer = config.getSignerConfig(S3_SERVICE_NAME, region);
        assertEquals("Service Signer validation failed for " + S3_SERVICE_NAME
                + " in region " + region, S3_SIGNER_TYPE,
                signer.getSignerType());
    }

    @Test
    public void testSDBSigners() {

        // SDB is internally available in cn-north-1 and uses a homebrew
        // SigV2 implementation; we've got it configured as such in
        // AWSJavaClientRuntimeConfigOverride, overriding the SigV4-only
        // configuration for cn-north-1 from the public SDK.
        SignerConfig signer = config.getSignerConfig("sdb", "cn-north-1");
        assertEquals("QueryStringSignerType", signer.getSignerType());
    }

    private void loadFrom(String resource) throws Exception {
        URL url = ClassLoaderHelper.getResource(resource);
        assertNotNull(url);
        InternalConfigJsonHelper config = InternalConfig.loadfrom(url);
        assertNotNull(config);
    }
}
