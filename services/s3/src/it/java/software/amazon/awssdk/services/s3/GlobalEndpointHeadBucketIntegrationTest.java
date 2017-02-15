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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringEndsWith.endsWith;
import static software.amazon.awssdk.test.util.hamcrest.Matchers.containsOnly;
import static software.amazon.awssdk.test.util.hamcrest.Matchers.containsOnlyInOrder;

import java.util.ArrayList;
import java.util.List;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.ClientConfiguration;
import software.amazon.awssdk.services.s3.model.AmazonS3Exception;
import software.amazon.awssdk.services.s3.model.Region;
import utils.http.RecordingSocketFactory;

@SuppressWarnings("unchecked")
public class GlobalEndpointHeadBucketIntegrationTest extends S3IntegrationTestBase {

    private static final String STANDARD_BUCKET = "global-endpoint-test-bucket-" + System.currentTimeMillis();
    private static final String US_WEST_BUCKET = "us-west-1-test-bucket-" + System.currentTimeMillis();
    private static final String IRISH_BUCKET = "eu-west-1-test-bucket-" + System.currentTimeMillis();
    private static final String STANDARD_ENDPOINT = "s3.amazonaws.com";
    private static final String IRISH_ENDPOINT = "s3-eu-west-1.amazonaws.com";
    private final RecordingSocketFactory socketRecorder = new RecordingSocketFactory();

    private AmazonS3Client client;

    @BeforeClass
    public static void createBucket() {
        s3.createBucket(STANDARD_BUCKET, Region.US_Standard);
        s3.createBucket(IRISH_BUCKET, Region.EU_Ireland);
        s3.createBucket(US_WEST_BUCKET, Region.US_West);
        AmazonS3Client.getBucketRegionCache().clear();
    }

    @AfterClass
    public static void deleteBucket() {
        s3.deleteBucket(STANDARD_BUCKET);
        s3.deleteBucket(IRISH_BUCKET);
        s3.deleteBucket(US_WEST_BUCKET);
    }

    @Before
    public void clearCache() {
        AmazonS3Client.getBucketRegionCache().clear();
        socketRecorder.clear();
        client = new AmazonS3Client(credentials, createConfig());
    }

    @Test
    public void callsToDetermineBucketSigningRegionHitStandardEndpoint() {
        client.setEndpoint(STANDARD_ENDPOINT);

        client.listObjects(STANDARD_BUCKET);

        assertThat(getHostsHit(), containsOnlyInOrder(endsWith(STANDARD_ENDPOINT)));
    }

    @Test
    public void callsToBucketLocatedInDifferentRegionWillBeRerouted() {
        client.setEndpoint(STANDARD_ENDPOINT);

        client.listObjects(IRISH_BUCKET);

        assertThat(getHostsHit(), containsOnlyInOrder(endsWith(STANDARD_ENDPOINT), endsWith(IRISH_ENDPOINT)));
    }

    @Test
    public void clientConfiguredToNonStandardEndpointDoesntMakeHeadBucketCall() {
        client.setEndpoint(IRISH_ENDPOINT);

        client.listObjects(IRISH_BUCKET);

        assertThat(getHostsHit(), containsOnly(endsWith(IRISH_ENDPOINT)));
    }

    @Test(expected = AmazonS3Exception.class)
    public void cannotHitObjectsNotInTheSameRegionAsTheConfiguredEndpoint() {
        client.setEndpoint(IRISH_ENDPOINT);
        client.listObjects(US_WEST_BUCKET);
    }

    private List<String> getHostsHit() {
        List<String> hosts = new ArrayList<String>();
        for (RecordingSocketFactory.ConnectSocketRequest r : socketRecorder.getConnectSocketRequests()) {
            hosts.add(r.host.getHostName());
        }
        return hosts;
    }

    private ClientConfiguration createConfig() {
        ClientConfiguration c = new ClientConfiguration();
        c.getApacheHttpClientConfig().setSslSocketFactory(socketRecorder);
        return c;
    }

}
