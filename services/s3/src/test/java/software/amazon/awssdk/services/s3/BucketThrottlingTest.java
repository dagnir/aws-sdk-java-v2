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

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.AmazonServiceException;
import software.amazon.awssdk.LegacyClientConfiguration;
import software.amazon.awssdk.auth.AwsStaticCredentialsProvider;
import software.amazon.awssdk.auth.BasicAwsCredentials;
import software.amazon.awssdk.metrics.spi.AwsRequestMetrics;
import utils.http.S3WireMockTestBase;
import utils.metrics.MockRequestMetricsCollector;

/**
 * Unit test to assert that S3 throttling exceptions are reported correctly in the metrics system.
 */
public class BucketThrottlingTest extends S3WireMockTestBase {

    private static final int MAX_RETRY = 2;

    private AmazonS3Client s3client;
    private MockRequestMetricsCollector metricsCollector;

    @Before
    public void setup() {
        metricsCollector = new MockRequestMetricsCollector();
        s3client = new AmazonS3Client(new AwsStaticCredentialsProvider(new BasicAwsCredentials("akid", "skid")),
                                      new LegacyClientConfiguration().withMaxErrorRetry(MAX_RETRY),
                                      metricsCollector);
        s3client.setEndpoint(getEndpoint());
    }

    @Test
    public void slowDownErrorReportedAsThrottlingException() throws JsonProcessingException {
        stubSlowDownException();

        try {
            s3client.listBuckets();
        } catch (AmazonServiceException ignored) {
            // Ignored.
        }
        assertThat(metricsCollector.getMetrics(), hasSize(1));
        AwsRequestMetrics metric = metricsCollector.getMetrics().get(0);
        Number counter = metric.getTimingInfo()
                               .getCounter(AwsRequestMetrics.Field.ThrottleException.name());
        assertEquals(counter, Long.valueOf(MAX_RETRY + 1));

    }

    private void stubSlowDownException() {
        final String slowDownBody = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Error><Code>SlowDown</Code><Message>Please " +
                                    "reduce your request rate.</Message><RequestId>36E5C81B8463E101</RequestId><HostId>FJKd" +
                                    "bo9Vbfb+MGbciAgKQ+Dy8mQ70rKNaz7PHvoCNKiZuh0OcKJd9Y9a6g8v1Oec</HostId></Error>";
        stubFor(get(urlEqualTo("/")).willReturn(
                stubS3ResponseCommon(aResponse().withStatus(503).withBody(slowDownBody))));
    }

}
