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

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import org.junit.Test;
import software.amazon.awssdk.ClientConfiguration;
import software.amazon.awssdk.DefaultRequest;
import software.amazon.awssdk.auth.NoOpSigner;
import software.amazon.awssdk.auth.Signer;
import software.amazon.awssdk.services.s3.internal.AWSS3V4Signer;
import software.amazon.awssdk.services.s3.internal.Constants;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

public class S3ClientConfigSignerTest {

    @Test
    public void clientConfigWithoutSigner() throws IOException {
        AmazonS3Client client = createTestClient(new ClientConfiguration());
        DefaultRequest<GetObjectRequest> req =
                new DefaultRequest<GetObjectRequest>(new GetObjectRequest("bucket", "key"),
                                                     Constants.S3_SERVICE_DISPLAY_NAME);
        req.setEndpoint(client.getEndpoint());
        final Signer signer = client.createSigner(req, "bucket", "key");
        client.shutdown();
        assertTrue(signer instanceof AWSS3V4Signer);
    }

    @Test
    public void clientConfigWithSigner() throws IOException {
        AmazonS3Client client = createTestClient(
                new ClientConfiguration().withSignerOverride("NoOpSignerType"));
        DefaultRequest<GetObjectRequest> req =
                new DefaultRequest<GetObjectRequest>(new GetObjectRequest("bucket", "key"),
                                                     Constants.S3_SERVICE_DISPLAY_NAME);
        req.setEndpoint(client.getEndpoint());
        final Signer signer = client.createSigner(req, "bucket", "key");
        client.shutdown();
        assertTrue(signer instanceof NoOpSigner);
    }

    private AmazonS3Client createTestClient(ClientConfiguration config) throws IOException {
        AmazonS3Client client = new AmazonS3Client(config);
        client.setEndpoint("s3.nonstandard.test.endpoint");
        return client;
    }

}
