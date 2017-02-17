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

package software.amazon.awssdk.services.s3.model.transform;

import static com.github.tomakehurst.wiremock.client.WireMock.equalToXml;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;

import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.services.s3.AmazonS3;
import software.amazon.awssdk.services.s3.model.SetBucketMetricsConfigurationRequest;
import software.amazon.awssdk.services.s3.model.Tag;
import software.amazon.awssdk.services.s3.model.metrics.MetricsAndOperator;
import software.amazon.awssdk.services.s3.model.metrics.MetricsConfiguration;
import software.amazon.awssdk.services.s3.model.metrics.MetricsFilter;
import software.amazon.awssdk.services.s3.model.metrics.MetricsFilterPredicate;
import software.amazon.awssdk.services.s3.model.metrics.MetricsPrefixPredicate;
import software.amazon.awssdk.services.s3.model.metrics.MetricsTagPredicate;
import utils.http.S3WireMockTestBase;

public class BucketMetricsConfigurationMarshallerTest extends S3WireMockTestBase {

    private static final String BUCKET_NAME = "destination-bucket";

    private AmazonS3 s3;

    @Before
    public void setUp() throws Exception {
        s3 = buildClient();
    }

    @Test
    public void testSetBucketMetricsConfiguration() {
        String configId = "metrics-id";
        List<MetricsFilterPredicate> operands = new ArrayList<MetricsFilterPredicate>();
        operands.add(new MetricsPrefixPredicate("documents/"));
        operands.add(new MetricsTagPredicate(new Tag("foo", "bar")));
        operands.add(new MetricsTagPredicate(new Tag("", "")));

        MetricsConfiguration config = new MetricsConfiguration()
                .withId(configId)
                .withFilter(new MetricsFilter(new MetricsAndOperator(operands)));

        try {
            s3.setBucketMetricsConfiguration(new SetBucketMetricsConfigurationRequest(BUCKET_NAME, config));
        } catch (Exception expected) {
            // Expected.
        }

        verify(putRequestedFor(urlEqualTo(String.format("/%s/?metrics&id=%s", BUCKET_NAME, configId)))
                       .withRequestBody(equalToXml(getExpectedMarshalledXml("MetricsConfiguration.xml"))));
    }
}