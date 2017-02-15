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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.FileEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.HttpMethod;
import software.amazon.awssdk.auth.AWSCredentialsProviderChain;
import software.amazon.awssdk.auth.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.auth.PropertiesFileCredentialsProvider;
import software.amazon.awssdk.auth.SystemPropertiesCredentialsProvider;
import software.amazon.awssdk.auth.profile.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Regions;
import software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils;
import software.amazon.awssdk.services.s3.model.AccessControlList;
import software.amazon.awssdk.services.s3.model.Bucket;
import software.amazon.awssdk.services.s3.model.CannedAccessControlList;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.GeneratePresignedUrlRequest;
import software.amazon.awssdk.services.s3.model.GroupGrantee;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.Permission;
import software.amazon.awssdk.services.s3.model.S3ObjectSummary;
import software.amazon.awssdk.services.s3.model.SSEAlgorithm;
import software.amazon.awssdk.services.s3.model.SSECustomerKey;
import software.amazon.awssdk.util.Base64;
import software.amazon.awssdk.util.Md5Utils;

public class S3DualstackIntegrationTest extends S3IntegrationTestBase {

    /** Default Properties Credentials file path */
    private static final String propertiesFilePath = System.getProperty("user.home")
                                                     + "/.aws/awsTestAccount.properties";

    private static final String TEST_CREDENTIALS_PROFILE_NAME = "aws-java-sdk-test";

    private static final AWSCredentialsProviderChain chain = new AWSCredentialsProviderChain(
            new PropertiesFileCredentialsProvider(propertiesFilePath),
            new ProfileCredentialsProvider(TEST_CREDENTIALS_PROFILE_NAME), new EnvironmentVariableCredentialsProvider(),
            new SystemPropertiesCredentialsProvider());

    /** Object contents to use/expect when we get/put a test object */
    private static final String EXPECTED_OBJECT_CONTENTS = "Hello S3 Java client world!!!";

    /** Name of the test bucket these tests will create, test, delete, etc */
    private static final String expectedBucketName = "integ-test-bucket-" + new Date().getTime();

    /** Name of the test key these tests will create, test, delete, etc */
    private static final String expectedKey = "integ-test-key-" + new Date().getTime();
    protected static AmazonS3 s3dualstack;
    private static File file;

    /**
     * Tests that we can correctly create an S3 bucket in the default location
     * for these tests to use.
     */
    @BeforeClass
    public static void createBucket() {
        s3dualstack = AmazonS3ClientBuilder.standard()
                                           .withCredentials(chain)
                                           .withRegion(Regions.US_WEST_2)
                                           .withDualstackEnabled(true)
                                           .build();

        CreateBucketRequest request = new CreateBucketRequest(expectedBucketName);
        request.setCannedAcl(CannedAccessControlList.AuthenticatedRead);
        Bucket bucket = s3dualstack.createBucket(request);
        assertNotNull(bucket);
        assertEquals(expectedBucketName, bucket.getName());
        S3ResponseMetadata responseMetadata = s3dualstack.getCachedResponseMetadata(request);
        assertNotNull(responseMetadata.getHostId());
        assertNotNull(responseMetadata.getRequestId());
        s3dualstack.putObject(expectedBucketName, expectedKey, EXPECTED_OBJECT_CONTENTS);

        AccessControlList bucketAcl = s3dualstack.getBucketAcl(bucket.getName());
        assertTrue(doesAclContainGroupGrant(bucketAcl, GroupGrantee.AuthenticatedUsers, Permission.Read));
    }

    /**
     * Ensures that any created test resources are correctly released.
     */
    @AfterClass
    public static void tearDown() {
        if (expectedBucketName != null) {
            s3.setS3ClientOptions(S3ClientOptions.builder().setAccelerateModeEnabled(false).build());
            deleteBucketAndAllContents(expectedBucketName);
        }
    }

    @Before
    public void setup() throws Exception {
        file = CryptoTestUtils.generateRandomAsciiFile(100);
    }

    @Test
    public void getObject() throws IOException {
        System.out.println(s3dualstack.getBucketLocation(expectedBucketName));
        InputStream inputStream = s3dualstack.getObject(expectedBucketName, expectedKey).getObjectContent();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        assertEquals(EXPECTED_OBJECT_CONTENTS, reader.readLine());
    }

    @Test
    public void getBucket() {
        assertEquals(s3dualstack.getRegion().toAWSRegion().getName(), s3dualstack.getBucketLocation(expectedBucketName));
    }

    @Test
    public void listObjects() {
        List<S3ObjectSummary> objects = s3dualstack.listObjects(expectedBucketName).getObjectSummaries();
        assertTrue(objectListContainsKey(objects, expectedKey));

        objects = s3dualstack.listObjects(expectedBucketName, "non-existant-object-key-prefix-").getObjectSummaries();
        assertNotNull(objects);

        objects = s3dualstack.listObjects(new ListObjectsRequest(expectedBucketName, null, null, null, 0)).getObjectSummaries();
        assertTrue(objects.size() == 0);
    }

    @Test
    public void listBuckets() {
        List<Bucket> buckets = s3dualstack.listBuckets();
        assertTrue(buckets.size() > 0);
    }

    @Test
    public void presignedSSECURL_Succeeds_When_Dualstack() throws IOException {
        String KEY = "SSE_C_withKey-" + file.getName();
        byte[] secretKey = new byte[32];
        // Generate presigned PUT URL with use of SSE-C with the actual
        // customer key
        GeneratePresignedUrlRequest genreq = new GeneratePresignedUrlRequest(
                expectedBucketName, KEY, HttpMethod.PUT);
        genreq.setSSECustomerKey(new SSECustomerKey(secretKey));
        URL url = s3dualstack.generatePresignedUrl(genreq);
        System.err.println("Presigned PUT URL for SSE-C (with key): " + url);

        // Puts object using presigned url with SSE-C
        HttpPut putreq = new HttpPut(URI.create(url.toExternalForm()));
        putreq.addHeader(new BasicHeader(
                Headers.SERVER_SIDE_ENCRYPTION_CUSTOMER_ALGORITHM,
                SSEAlgorithm.AES256.getAlgorithm()));
        putreq.addHeader(new BasicHeader(
                Headers.SERVER_SIDE_ENCRYPTION_CUSTOMER_KEY, Base64
                .encodeAsString(secretKey)));
        putreq.addHeader(new BasicHeader(
                Headers.SERVER_SIDE_ENCRYPTION_CUSTOMER_KEY_MD5, Md5Utils
                .md5AsBase64(secretKey)));

        putreq.setEntity(new FileEntity(file));
        CloseableHttpClient httpclient = HttpClients.createDefault();
        httpclient.execute(putreq);
        httpclient.close();
    }

    /**
     * Returns true if the list of objects contains an object with the expected
     * key.
     *
     * @param objects
     *            The list of objects to check.
     * @param expectedKey
     *            The object key to search for in the list of objects.
     * @return True if the list of objects contains an object with the specified
     *         key.
     */
    private boolean objectListContainsKey(List<S3ObjectSummary> objects, String expectedKey) {
        for (Iterator<S3ObjectSummary> iterator = objects.iterator(); iterator.hasNext(); ) {
            S3ObjectSummary obj = iterator.next();
            if (obj.getKey().equals(expectedKey)) {
                return true;
            }
        }

        return false;
    }
}
