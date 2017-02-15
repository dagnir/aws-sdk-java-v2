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

package software.amazon.awssdk.services.s3;

import static org.junit.Assert.assertEquals;

import java.util.Date;
import java.util.List;
import org.junit.After;
import org.junit.Test;
import software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils;
import software.amazon.awssdk.services.s3.model.ObjectListing;
import software.amazon.awssdk.services.s3.model.ObjectMetadata;
import software.amazon.awssdk.services.s3.model.S3ObjectSummary;
import software.amazon.awssdk.test.util.RandomInputStream;

/**
 * Integration test covering the use of carriage return and other odd characters
 * in key names.
 *
 * @author Jason Fulghum <fulghum@amazon.com>
 */
public class XmlSanitizingIntegrationTest extends S3IntegrationTestBase {

    /** The bucket name created and used by this test */
    private String bucketName = "xml-sanitizing-integ-test-" + new Date().getTime();

    /** The offending key containing a carriage return character */
    private String key = "first<line\rsecond>line";

    /** Releases all resources used by this test */
    @After
    public void tearDown() {
        CryptoTestUtils.deleteBucketAndAllContents(s3, bucketName);
    }

    /**
     * Tests that the S3 Java client can correctly upload and then list an
     * object whose key contains a carriage return character.
     */
    @Test
    public void testXmlSanitizing() throws Exception {
        s3.createBucket(bucketName);
        s3.putObject(bucketName, key, new RandomInputStream(125L), new ObjectMetadata());

        ObjectListing objectListing = s3.listObjects(bucketName);
        List<S3ObjectSummary> objectSummaries = objectListing.getObjectSummaries();

        assertEquals(1, objectSummaries.size());
        S3ObjectSummary objectSummary = (S3ObjectSummary) objectSummaries.get(0);
        assertEquals(key, objectSummary.getKey());
    }

}
