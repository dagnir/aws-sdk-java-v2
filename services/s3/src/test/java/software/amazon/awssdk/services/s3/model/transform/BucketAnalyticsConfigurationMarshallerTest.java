/*
 * Copyright 2011-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *    http://aws.amazon.com/apache2.0
 *
 * This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and
 * limitations under the License.
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
import software.amazon.awssdk.services.s3.model.SetBucketAnalyticsConfigurationRequest;
import software.amazon.awssdk.services.s3.model.Tag;
import software.amazon.awssdk.services.s3.model.analytics.AnalyticsAndOperator;
import software.amazon.awssdk.services.s3.model.analytics.AnalyticsConfiguration;
import software.amazon.awssdk.services.s3.model.analytics.AnalyticsExportDestination;
import software.amazon.awssdk.services.s3.model.analytics.AnalyticsFilter;
import software.amazon.awssdk.services.s3.model.analytics.AnalyticsFilterPredicate;
import software.amazon.awssdk.services.s3.model.analytics.AnalyticsPrefixPredicate;
import software.amazon.awssdk.services.s3.model.analytics.AnalyticsS3BucketDestination;
import software.amazon.awssdk.services.s3.model.analytics.AnalyticsS3ExportFileFormat;
import software.amazon.awssdk.services.s3.model.analytics.AnalyticsTagPredicate;
import software.amazon.awssdk.services.s3.model.analytics.StorageClassAnalysis;
import software.amazon.awssdk.services.s3.model.analytics.StorageClassAnalysisDataExport;
import software.amazon.awssdk.services.s3.model.analytics.StorageClassAnalysisSchemaVersion;
import utils.http.S3WireMockTestBase;

public class BucketAnalyticsConfigurationMarshallerTest extends S3WireMockTestBase {

    private static final String BUCKET_NAME = "destination-bucket";

    private static final String BUCKET_ARN = "arn:aws:s3:::destination-bucket";

    private AmazonS3 s3;

    @Before
    public void setUp() throws Exception {
        s3 = buildClient();
    }

    @Test
    public void setBucketAnalyticsConfiguration() {
        String configId = "analytics-id";
        List<AnalyticsFilterPredicate> operands = new ArrayList<AnalyticsFilterPredicate>();
        operands.add(new AnalyticsPrefixPredicate("documents/"));
        operands.add(new AnalyticsTagPredicate(new Tag("foo", "bar")));

        AnalyticsConfiguration config = new AnalyticsConfiguration()
                .withId(configId)
                .withFilter(new AnalyticsFilter(new AnalyticsAndOperator(operands)))
                .withStorageClassAnalysis(getStorageClassAnalysis());

        try {
            s3.setBucketAnalyticsConfiguration(new SetBucketAnalyticsConfigurationRequest(BUCKET_NAME, config));
        } catch (Exception expected) {
        }

        verify(putRequestedFor(urlEqualTo(String.format("/%s/?analytics&id=%s", BUCKET_NAME, configId)))
                .withRequestBody(equalToXml(getExpectedMarshalledXml("AnalyticsConfiguration.xml"))));
    }

    private StorageClassAnalysis getStorageClassAnalysis() {
        AnalyticsS3BucketDestination s3BucketDestination = new AnalyticsS3BucketDestination()
                .withBucketArn(BUCKET_ARN)
                .withFormat(AnalyticsS3ExportFileFormat.CSV)
                .withBucketAccountId("123456789")
                .withPrefix("destination-prefix");

        return new StorageClassAnalysis().withDataExport(
                new StorageClassAnalysisDataExport()
                        .withOutputSchemaVersion(StorageClassAnalysisSchemaVersion.V_1)
                        .withDestination(new AnalyticsExportDestination().withS3BucketDestination(s3BucketDestination)));
    }
}
