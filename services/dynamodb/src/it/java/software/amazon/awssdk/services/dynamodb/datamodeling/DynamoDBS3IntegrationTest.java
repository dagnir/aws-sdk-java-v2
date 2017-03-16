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

package software.amazon.awssdk.services.dynamodb.datamodeling;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.util.UUID;
import org.junit.Test;
import software.amazon.awssdk.auth.AwsStaticCredentialsProvider;
import software.amazon.awssdk.services.dynamodb.pojos.S3LinksTestClass;
import software.amazon.awssdk.services.s3.model.CannedAccessControlList;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.Region;
import software.amazon.awssdk.test.util.RandomTempFile;

public class DynamoDBS3IntegrationTest extends DynamoDBS3IntegrationTestBase {

    private static final long OBJECT_SIZE = 123;

    @Test
    public void testCredentialContext() throws Exception {
        tryCreateItem(new DynamoDbMapper(dynamo, new AwsStaticCredentialsProvider(credentials)));
    }

    @Test
    public void testManuallyFilledContext() throws Exception {
        DynamoDbMapper mapper = new DynamoDbMapper(dynamo, new AwsStaticCredentialsProvider(credentials));
        S3ClientCache s3cc = mapper.getS3ClientCache();
        s3cc.useClient(s3East);
        s3cc.useClient(s3West);
        tryCreateItem(mapper);
    }

    public void tryCreateItem(DynamoDbMapper mapper) throws Exception {
        String westKey = UUID.randomUUID().toString();
        String eastKey = UUID.randomUUID().toString();

        S3LinksTestClass obj = new S3LinksTestClass();
        obj.setKey("" + ++startKey);
        S3Link linkWest = mapper.createS3Link(Region.US_West_2, DynamoDBS3IntegrationTestBase.WEST_BUCKET, westKey);
        obj.setS3LinkWest(linkWest);
        mapper.save(obj);
        obj = mapper.load(S3LinksTestClass.class, obj.getKey());

        assertObjectDoesntExist(s3West, obj.getS3LinkWest().getBucketName(), westKey);
        assertObjectDoesntExist(s3East, obj.getS3LinkWest().getBucketName(), eastKey);

        PutObjectRequest pubObjectReq = new PutObjectRequest(linkWest.getBucketName(), linkWest.getKey(),
                                                             new RandomTempFile(westKey, OBJECT_SIZE));
        linkWest.getAmazonS3Client().putObject(pubObjectReq);

        assertObjectExists(s3West, obj.getS3LinkWest().getBucketName(), westKey);

        S3Link linkEast = mapper.createS3Link(Region.US_Standard, DynamoDBS3IntegrationTestBase.EAST_BUCKET, eastKey);
        obj.setS3LinkEast(linkEast);
        assertObjectDoesntExist(s3East, obj.getS3LinkEast().getBucketName(), eastKey);
        pubObjectReq = new PutObjectRequest(linkEast.getBucketName(), linkEast.getKey(),
                                            new RandomTempFile(westKey, OBJECT_SIZE));
        linkEast.getAmazonS3Client().putObject(pubObjectReq);
        mapper.save(obj);

        assertObjectExists(s3West, obj.getS3LinkWest().getBucketName(), westKey);
        assertObjectExists(s3East, obj.getS3LinkEast().getBucketName(), eastKey);

        obj = mapper.load(S3LinksTestClass.class, obj.getKey());

        assertEquals(westKey, obj.getS3LinkWest().getKey());
        assertEquals(eastKey, obj.getS3LinkEast().getKey());
        System.err.println(obj.getS3LinkWest().toJson());
        System.err.println(obj.getS3LinkEast().toJson());
        mapper.delete(obj);

        // Test the convenience methods on S3Link
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        linkEast.downloadTo(baos);
        assertEquals(OBJECT_SIZE, baos.toByteArray().length);

        linkEast.setAcl(CannedAccessControlList.PublicRead);

        URL url = linkEast.getUrl();
        assertTrue(url.getHost().startsWith(linkEast.getBucketName()));
        assertTrue(url.getPath().contains(linkEast.getKey()));
    }
}
