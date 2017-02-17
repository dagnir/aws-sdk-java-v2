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
import static org.junit.Assert.assertNull;

import java.util.HashMap;
import java.util.Map;
import org.junit.After;
import org.junit.Test;
import software.amazon.awssdk.services.s3.model.BucketTaggingConfiguration;
import software.amazon.awssdk.services.s3.model.TagSet;

/**
 * Integration tests for S3 bucket tagging configuration.
 */
public class BucketTaggingIntegrationTest extends S3IntegrationTestBase {

    private static final String BUCKET_NAME = "java-sdk-tagging-test-" + System.currentTimeMillis();

    /** Releases all resources created by this test. */
    @After
    public void tearDown() throws Exception {
        deleteBucketAndAllContents(BUCKET_NAME);
    }

    /** Tests that we can get, set, and delete a bucket's website configuration. */
    @Test
    public void testBucketWebsites() throws Exception {

        // create a test bucket
        s3.createBucket(BUCKET_NAME);

        // get tagging config for new bucket
        BucketTaggingConfiguration bucketTaggingConfiguration = s3.getBucketTaggingConfiguration(BUCKET_NAME);
        System.out.println("config: " + bucketTaggingConfiguration);

        Map<String, String> tags = new HashMap<String, String>(1);
        tags.put("User", "Foo");
        tags.put("Group", "Bar");
        TagSet tagSet = new TagSet(tags);

        // set tagging configuration
        s3.setBucketTaggingConfiguration(BUCKET_NAME, new BucketTaggingConfiguration().withTagSets(tagSet));

        // get again
        bucketTaggingConfiguration = s3.getBucketTaggingConfiguration(BUCKET_NAME);
        TagSet remoteTagSet = bucketTaggingConfiguration.getTagSet();
        assertEquals(remoteTagSet.getAllTags().size(), tags.size());
        assertEquals(remoteTagSet.getTag("User"), tags.get("User"));
        assertEquals(remoteTagSet.getTag("Group"), tags.get("Group"));

        // delete
        s3.deleteBucketTaggingConfiguration(BUCKET_NAME);
        Thread.sleep(1000 * 15 * 1);

        // get again
        bucketTaggingConfiguration = s3.getBucketTaggingConfiguration(BUCKET_NAME);
        assertNull(bucketTaggingConfiguration);
    }
}
