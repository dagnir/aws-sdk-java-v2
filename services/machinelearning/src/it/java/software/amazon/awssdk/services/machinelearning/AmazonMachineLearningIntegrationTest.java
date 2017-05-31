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

package software.amazon.awssdk.services.machinelearning;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.regions.Regions;
import software.amazon.awssdk.services.machinelearning.model.CreateDataSourceFromS3Request;
import software.amazon.awssdk.services.machinelearning.model.CreateDataSourceFromS3Result;
import software.amazon.awssdk.services.machinelearning.model.CreateMLModelRequest;
import software.amazon.awssdk.services.machinelearning.model.CreateMLModelResult;
import software.amazon.awssdk.services.machinelearning.model.CreateRealtimeEndpointRequest;
import software.amazon.awssdk.services.machinelearning.model.DeleteDataSourceRequest;
import software.amazon.awssdk.services.machinelearning.model.DeleteMLModelRequest;
import software.amazon.awssdk.services.machinelearning.model.DeleteRealtimeEndpointRequest;
import software.amazon.awssdk.services.machinelearning.model.EntityStatus;
import software.amazon.awssdk.services.machinelearning.model.GetDataSourceRequest;
import software.amazon.awssdk.services.machinelearning.model.GetDataSourceResult;
import software.amazon.awssdk.services.machinelearning.model.GetMLModelRequest;
import software.amazon.awssdk.services.machinelearning.model.GetMLModelResult;
import software.amazon.awssdk.services.machinelearning.model.MLModelType;
import software.amazon.awssdk.services.machinelearning.model.PredictRequest;
import software.amazon.awssdk.services.machinelearning.model.Prediction;
import software.amazon.awssdk.services.machinelearning.model.RealtimeEndpointInfo;
import software.amazon.awssdk.services.machinelearning.model.RealtimeEndpointStatus;
import software.amazon.awssdk.services.machinelearning.model.S3DataSpec;
import software.amazon.awssdk.services.s3.AmazonS3Client;
import software.amazon.awssdk.services.s3.model.CannedAccessControlList;
import software.amazon.awssdk.services.s3.model.ObjectMetadata;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.test.AwsTestBase;

public class AmazonMachineLearningIntegrationTest extends AwsTestBase {

    private static final String BUCKET_NAME =
            "aws-java-sdk-eml-test-" + System.currentTimeMillis();

    private static final String KEY = "data.csv";

    private static final String DATA_LOCATION_S3 =
            "s3://" + BUCKET_NAME + "/" + KEY;


    private static final String DATA = "0, 42, foo, bar\n1, 38, alice, bob";

    private static final String DATA_SCHEMA = "{"
                                              + "\"version\":\"1.0\","
                                              + "\"recordAnnotationFieldName\":null,"
                                              + "\"recordWeightFieldName\":null,"
                                              + "\"targetFieldName\":\"a\","
                                              + "\"dataFormat\":\"CSV\","
                                              + "\"dataFileContainsHeader\":false,"
                                              + "\"variables\": ["
                                              + "{"
                                              + "\"fieldName\":\"a\","
                                              + "\"fieldType\":\"BINARY\""
                                              + "},{"
                                              + "\"fieldName\":\"b\","
                                              + "\"fieldType\":\"NUMERIC\""
                                              + "},{"
                                              + "\"fieldName\":\"c\","
                                              + "\"fieldType\":\"CATEGORICAL\""
                                              + "},{"
                                              + "\"fieldName\":\"d\","
                                              + "\"fieldType\":\"TEXT\""
                                              + "}"
                                              + "]"
                                              + "}";

    private static AmazonS3Client s3;
    private static MachineLearningClient client;

    private static String dataSourceId;
    private static String mlModelId;

    @BeforeClass
    public static void setUp() throws IOException {
        setUpCredentials();
        setUpS3();

        System.setProperty("software.amazon.awssdk.sdk.disableCertChecking", "true");

        client = MachineLearningClient.builder()
                .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                .region(Regions.US_EAST_1.getName())
                .build();
    }

    private static void setUpS3() {
        s3 = new AmazonS3Client(credentials);
        s3.configureRegion(Regions.US_WEST_2);

        s3.createBucket(BUCKET_NAME);

        s3.putObject(new PutObjectRequest(
                BUCKET_NAME,
                KEY,
                new ByteArrayInputStream(DATA.getBytes()),
                new ObjectMetadata())
                             .withCannedAcl(CannedAccessControlList.PublicRead));
    }

    @AfterClass
    public static void cleanUp() {
        if (client != null) {
            if (mlModelId != null) {
                try {
                    client.deleteRealtimeEndpoint(DeleteRealtimeEndpointRequest.builder()
                                                          .mlModelId(mlModelId).build());
                } catch (Exception e) {
                    e.printStackTrace();
                }

                try {
                    client.deleteMLModel(DeleteMLModelRequest.builder()
                                                 .mlModelId(mlModelId).build());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if (dataSourceId != null) {
                try {
                    client.deleteDataSource(DeleteDataSourceRequest.builder()
                                                    .dataSourceId(dataSourceId).build());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        if (s3 != null) {
            try {
                s3.deleteObject(BUCKET_NAME, KEY);
            } catch (Exception e) {
                e.printStackTrace();
            }

            try {
                s3.deleteBucket(BUCKET_NAME);
            } catch (Exception e) {
                e.printStackTrace();
            }

            s3.shutdown();
        }
    }

    @Test
    public void testBinary() throws Exception {
        CreateDataSourceFromS3Result result =
                client.createDataSourceFromS3(CreateDataSourceFromS3Request.builder()
                        .dataSpec(S3DataSpec.builder()
                                .dataLocationS3(DATA_LOCATION_S3)
                                .dataSchema(DATA_SCHEMA).build())
                        .computeStatistics(true)
                        .dataSourceId("data_" + System.currentTimeMillis())
                        .build());

        dataSourceId = result.dataSourceId();

        Assert.assertEquals("COMPLETED", waitForDataSource());

        CreateMLModelResult result2 =
                client.createMLModel(CreateMLModelRequest.builder()
                        .trainingDataSourceId(dataSourceId)
                        .mlModelType(MLModelType.BINARY)
                        .mlModelId("mlid_" + System.currentTimeMillis())
                        .build());

        mlModelId = result2.mlModelId();

        Assert.assertEquals("COMPLETED", waitForMlModel());


        client.createRealtimeEndpoint(CreateRealtimeEndpointRequest.builder()
                .mlModelId(mlModelId)
                .build());

        String uri = waitUntilMounted();

        // Apparently just because the endpoint is ready doesn't mean
        // it's necessarily ready. :(
        Thread.sleep(60000);

        HashMap<String, String> record = new HashMap<String, String>() {
            {
                put("b", "123");
                put("c", "oop");
                put("d", "goop");
            }
        };

        Prediction prediction = client.predict(PredictRequest.builder()
                .predictEndpoint(uri)
                .mlModelId(mlModelId)
                .record(record).build())
                .prediction();
        System.out.println(prediction.predictedLabel());
        System.out.println(prediction.predictedValue());
        System.out.println(prediction.predictedScores());
        System.out.println(prediction.details());
    }

    private String waitForDataSource() throws InterruptedException {
        for (int i = 0; i < 100; ++i) {
            GetDataSourceResult result =
                    client.getDataSource(GetDataSourceRequest.builder()
                            .dataSourceId(dataSourceId)
                            .build());

            System.out.println(result);

            String status = result.status();
            switch (EntityStatus.valueOf(status)) {
                case PENDING:
                case INPROGRESS:
                    Thread.sleep(10000);
                    break;

                case FAILED:
                case COMPLETED:
                case DELETED:
                    return status;

                default:
                    Assert.fail("Unrecognized data source validation status: "
                                + status);
            }
        }

        Assert.fail("Timed out waiting for data source validation");
        return null;
    }

    private String waitForMlModel() throws InterruptedException {
        for (int i = 0; i < 100; ++i) {
            GetMLModelResult result =
                    client.getMLModel(GetMLModelRequest.builder()
                            .mlModelId(mlModelId)
                            .build());

            System.out.println(result);

            String status = result.status();
            switch (EntityStatus.valueOf(status)) {
                case PENDING:
                case INPROGRESS:
                    Thread.sleep(10000);
                    continue;

                case FAILED:
                case COMPLETED:
                case DELETED:
                    return status;

                default:
                    throw new IllegalStateException("Unrecognized status: "
                                                    + status);
            }
        }

        Assert.fail("Timed out waiting for ML Model to be ready");
        return null;
    }

    private String waitUntilMounted() throws InterruptedException {
        for (int i = 0; i < 100; ++i) {
            GetMLModelResult result =
                    client.getMLModel(GetMLModelRequest.builder()
                            .mlModelId(mlModelId)
                            .build());

            System.out.println(result);

            RealtimeEndpointInfo info = result.endpointInfo();
            if (info == null) {
                Thread.sleep(1000);
                continue;
            }

            String status = info.endpointStatus();
            switch (RealtimeEndpointStatus.valueOf(status)) {
                case READY:
                    return info.endpointUrl();

                case UPDATING:
                    Thread.sleep(1000);
                    continue;

                case NONE:
                default:
                    Assert.fail("Not mounted!");
            }
        }

        Assert.fail("Timed out waiting for ML model to get mounted");
        return null;
    }
}
