/*
 * Copyright 2016-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import static software.amazon.awssdk.services.s3.internal.Constants.KB;

import java.io.File;
import java.util.Arrays;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils;
import software.amazon.awssdk.services.s3.model.BucketVersioningConfiguration;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectTaggingRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectTaggingResult;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectTaggingRequest;
import software.amazon.awssdk.services.s3.model.GetObjectTaggingResult;
import software.amazon.awssdk.services.s3.model.ObjectTagging;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResult;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.model.SetBucketVersioningConfigurationRequest;
import software.amazon.awssdk.services.s3.model.SetObjectTaggingRequest;
import software.amazon.awssdk.services.s3.model.SetObjectTaggingResult;
import software.amazon.awssdk.services.s3.model.Tag;
import software.amazon.awssdk.test.util.RandomTempFile;

/**
 * Integration tests for object tagging support.
 */
public class ObjectTaggingIntegrationTest extends S3IntegrationTestBase {
    private static final String KEY_PREFIX = "tagged-object-";
    private static final String BUCKET = "java-object-tagging-bucket-" + System.currentTimeMillis();

    private static File testFile;

    @BeforeClass
    public static void setUp() throws Exception {
        S3IntegrationTestBase.setUp();
        testFile = new RandomTempFile("s3-test-file", 4 * KB);
        s3.createBucket(new CreateBucketRequest(BUCKET));
        s3.setBucketVersioningConfiguration(new SetBucketVersioningConfigurationRequest(
                BUCKET,
                new BucketVersioningConfiguration(BucketVersioningConfiguration.ENABLED)
        ));
    }

    @AfterClass
    public static void tearDown() {
        CryptoTestUtils.deleteBucketAndAllContents(s3, BUCKET);
    }

    @Test
    public void testGetObjectCorrectTaggingCountNoTags() {
        String key = makeNewKey();
        putTestObject(key, null);
        assertEquals(null, s3.getObject(BUCKET, key).getTaggingCount());
    }

    @Test
    public void testGetObjectCorrectTaggingCountHasTags() {
        ObjectTagging tags = new ObjectTagging(Arrays.asList(
            new Tag("foo", "1"),
            new Tag("bar", "2"),
            new Tag("baz", "3")
        ));

        String key = makeNewKey();
        putTestObject(key, tags);

        assertEquals(tags.getTagSet().size(), s3.getObject(new GetObjectRequest(BUCKET, key)).getTaggingCount().intValue());
    }

    @Test
    public void testGetObjectTags() {
        ObjectTagging tagging = new ObjectTagging(Arrays.asList(
            new Tag("foo", "1"),
            new Tag("bar", "2"),
            new Tag("baz", "3")
        ));

        String key = makeNewKey();
        PutObjectResult putObjectResult = putTestObject(key, tagging);

        GetObjectTaggingResult getTaggingResult = s3.getObjectTagging(new GetObjectTaggingRequest(BUCKET, key));
        ObjectTaggingTestUtil.assertTagSetsAreEquals(tagging.getTagSet(), getTaggingResult.getTagSet());
        assertEquals(putObjectResult.getVersionId(), getTaggingResult.getVersionId());
    }

    @Test
    public void testPutObjectWithTags() {
        ObjectTagging tags = new ObjectTagging(Arrays.asList(
            new Tag("foo", "1"),
            new Tag("bar", "2"),
            new Tag("baz", "3")
        ));

        String key = makeNewKey();
        putTestObject(key, tags);

        assertSetTagsAreEqual(BUCKET, key, null, tags);
    }

    @Test
    public void testPutObjectWithTagsTagsAreUrlEncoded() {
        ObjectTagging tags = new ObjectTagging(Arrays.asList(
            new Tag("foo", "bar baz"),
            new Tag("foo bar", "baz"),
            new Tag("foo/bar", "baz")
        ));
        String key = makeNewKey();
        putTestObject(key, tags);

        assertSetTagsAreEqual(BUCKET, key, null, tags);
    }

    @Test
    public void testSetObjectTagging() {
        String key = makeNewKey();
        PutObjectResult putObjectResult = putTestObject(key, null);

        S3Object obj = s3.getObject(BUCKET, key);
        assertEquals(null, obj.getTaggingCount());

        ObjectTagging tags = new ObjectTagging(Arrays.asList(
            new Tag("foo", "1")
        ));

        SetObjectTaggingResult putTaggingResult = s3.setObjectTagging(new SetObjectTaggingRequest(BUCKET,
                key, tags));

        assertSetTagsAreEqual(BUCKET, key, null, tags);
        assertEquals(putObjectResult.getVersionId(), putTaggingResult.getVersionId());
    }

    @Test
    public void testGetObjectTaggingForDifferentVersions() {
        String key = makeNewKey();
        ObjectTagging tags1 = new ObjectTagging(Arrays.asList(
            new Tag("foo", "1")
        ));
        PutObjectResult putResult1 = putTestObject(key, tags1);

        ObjectTagging tags2 = new ObjectTagging(Arrays.asList(
            new Tag("foo", "1"),
            new Tag("bar", "2")
        ));
        PutObjectResult putResult2 = putTestObject(key, tags2);

        assertSetTagsAreEqual(BUCKET, key, putResult1.getVersionId(), tags1);
        assertSetTagsAreEqual(BUCKET, key, putResult2.getVersionId(), tags2);

    }

    @Test
    public void testPutObjectDifferentTagsOnEachVersion() {
        String key = makeNewKey();
        ObjectTagging tags1 = new ObjectTagging(Arrays.asList(
            new Tag("foo", "1")
        ));
        PutObjectResult putResult1 = putTestObject(key, tags1);

        ObjectTagging tags2 = new ObjectTagging(Arrays.asList(
            new Tag("foo", "1"),
            new Tag("bar", "2")
        ));
        PutObjectResult putResult2 = putTestObject(key, tags2);

        assertSetTagsAreEqual(BUCKET, key, putResult1.getVersionId(), tags1);
        assertSetTagsAreEqual(BUCKET, key, putResult2.getVersionId(), tags2);
    }

    @Test
    public void testSetTagsOnDifferentObjectVersion() {
        String key = makeNewKey();
        ObjectTagging tags1 = new ObjectTagging(Arrays.asList(
            new Tag("foo", "1")
        ));
        PutObjectResult putResult1 = putTestObject(key, tags1);

        // put a second time so we have two versions in S3
        PutObjectResult putResult2 = putTestObject(key, tags1);

        ObjectTagging tags2 = new ObjectTagging(Arrays.asList(
            new Tag("foo", "1"),
            new Tag("bar", "2")
        ));

        // set a different set of tags on the latest version
        s3.setObjectTagging(new SetObjectTaggingRequest(BUCKET, key, putResult2.getVersionId(), tags2));

        assertSetTagsAreEqual(BUCKET, key, putResult1.getVersionId(), tags1);
        assertSetTagsAreEqual(BUCKET, key, putResult2.getVersionId(), tags2);
    }

    @Test
    public void testCopyObjectWithNewTags() {
        ObjectTagging tags = new ObjectTagging(Arrays.asList(
            new Tag("foo", "bar")
        ));
        String sourceKey = makeNewKey();
        putTestObject(sourceKey, tags);

        String destKey = makeNewKey();
        ObjectTagging destTags = new ObjectTagging(Arrays.asList(
            new Tag("foo1", "bar"),
            new Tag("foo2", "baz")
        ));

        s3.copyObject(new CopyObjectRequest(BUCKET, sourceKey, BUCKET, destKey).withNewObjectTagging(destTags));

        assertSetTagsAreEqual(BUCKET, destKey, null, destTags);
    }

    @Test

    public void testCopyObjectNoNewTags() {
        ObjectTagging tags = new ObjectTagging(Arrays.asList(
            new Tag("foo", "bar")
        ));
        String sourceKey = makeNewKey();
        putTestObject(sourceKey, tags);

        String destKey = makeNewKey();

        s3.copyObject(new CopyObjectRequest(BUCKET, sourceKey, BUCKET, destKey));

        assertSetTagsAreEqual(BUCKET, destKey, null, tags);
    }

    @Test
    public void testDeleteObjectTagging() {
        ObjectTagging tags = new ObjectTagging(Arrays.asList(
            new Tag("foo", "bar")
        ));
        String key = makeNewKey();
        putTestObject(key, tags);

        S3Object objBeforeTagsDeletion = s3.getObject(BUCKET, key);
        assertEquals(tags.getTagSet().size(),
                objBeforeTagsDeletion.getTaggingCount().intValue());

        s3.deleteObjectTagging(new DeleteObjectTaggingRequest(
                BUCKET,
                key)
                .withVersionId(objBeforeTagsDeletion.getObjectMetadata().getVersionId()));

        S3Object objAfterTagsDeletion = s3.getObject(BUCKET, key);
        assertNull(objAfterTagsDeletion.getTaggingCount());
    }

    @Test
    public void testDeleteObjectTaggingOnVersion() {
        ObjectTagging v1Tags = new ObjectTagging(Arrays.asList(
            new Tag("foo", "bar")
        ));
        String key = makeNewKey();
        String v1Id = putTestObject(key, v1Tags).getVersionId();

        ObjectTagging v2Tags = new ObjectTagging(Arrays.asList(
            new Tag("foo", "baz")
        ));
        String v2Id = putTestObject(key, v2Tags).getVersionId();

        DeleteObjectTaggingResult result = s3.deleteObjectTagging(new DeleteObjectTaggingRequest(
            BUCKET,
            key)
            .withVersionId(v2Id));
        assertNull(s3.getObject(new GetObjectRequest(BUCKET, key, v2Id)).getTaggingCount());
        assertEquals(v2Id, result.getVersionId());
        assertSetTagsAreEqual(BUCKET, key, v1Id, v1Tags);
    }

    private String makeNewKey() {
        return KEY_PREFIX + System.currentTimeMillis();
    }

    private PutObjectResult putTestObject(String key, ObjectTagging tagging) {
        return s3.putObject(new PutObjectRequest(BUCKET, key, testFile).withTagging(tagging));
    }

    private void assertSetTagsAreEqual(String bucket, String key, String version, ObjectTagging expectedTagging) {
        GetObjectTaggingResult result = s3.getObjectTagging(new GetObjectTaggingRequest(bucket, key, version));
       ObjectTaggingTestUtil.assertTagSetsAreEquals(expectedTagging.getTagSet(), result.getTagSet());
    }
}
