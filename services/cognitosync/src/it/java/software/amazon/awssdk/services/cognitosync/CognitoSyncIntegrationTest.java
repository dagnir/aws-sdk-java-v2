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

package software.amazon.awssdk.services.cognitosync;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.auth.AwsStaticCredentialsProvider;
import software.amazon.awssdk.services.cognitoidentity.CognitoIdentityClient;
import software.amazon.awssdk.services.cognitoidentity.model.CreateIdentityPoolRequest;
import software.amazon.awssdk.services.cognitoidentity.model.CreateIdentityPoolResult;
import software.amazon.awssdk.services.cognitoidentity.model.DeleteIdentityPoolRequest;
import software.amazon.awssdk.services.cognitoidentity.model.GetIdRequest;
import software.amazon.awssdk.services.cognitoidentity.model.GetIdResult;
import software.amazon.awssdk.services.cognitosync.model.Dataset;
import software.amazon.awssdk.services.cognitosync.model.DeleteDatasetRequest;
import software.amazon.awssdk.services.cognitosync.model.DescribeDatasetRequest;
import software.amazon.awssdk.services.cognitosync.model.DescribeDatasetResult;
import software.amazon.awssdk.services.cognitosync.model.DescribeIdentityPoolUsageRequest;
import software.amazon.awssdk.services.cognitosync.model.DescribeIdentityPoolUsageResult;
import software.amazon.awssdk.services.cognitosync.model.DescribeIdentityUsageRequest;
import software.amazon.awssdk.services.cognitosync.model.DescribeIdentityUsageResult;
import software.amazon.awssdk.services.cognitosync.model.IdentityPoolUsage;
import software.amazon.awssdk.services.cognitosync.model.IdentityUsage;
import software.amazon.awssdk.services.cognitosync.model.ListRecordsRequest;
import software.amazon.awssdk.services.cognitosync.model.ListRecordsResult;
import software.amazon.awssdk.services.cognitosync.model.Operation;
import software.amazon.awssdk.services.cognitosync.model.Record;
import software.amazon.awssdk.services.cognitosync.model.RecordPatch;
import software.amazon.awssdk.services.cognitosync.model.UpdateRecordsRequest;
import software.amazon.awssdk.services.cognitosync.model.UpdateRecordsResult;
import software.amazon.awssdk.test.AwsTestBase;

public class CognitoSyncIntegrationTest extends AwsTestBase {

    /**
     * Identity pool name created for testing.
     */
    private static final String IDENTITY_POOL_NAME = "javasdkpool"
                                                     + System.currentTimeMillis();
    /**
     * Name of the data set associated with an identity used for testing.
     */
    private static final String DATASET_NAME = "dataset"
                                               + System.currentTimeMillis();
    /**
     * AWS account id for which the identity id's are generated.
     */
    private static final String AWS_ACCOUNT_ID = "599169622985";
    /**
     * Provider supported by the identity pool and associated with the
     * identities.
     */
    private static final String PROVIDER = "foo";
    /** APP id for the provider associated with the identity pool. */
    private static final String APP_ID = "fooId";
    /**
     * Reference to the Amazon Cognito Identity client
     */
    private static CognitoIdentityClient identity;
    /**
     * Reference to the Amazon Cognito Sync client
     */
    private static CognitoSyncClient sync;
    /**
     * Identity pool id generated by the Amazon Cognito service.
     */
    private static String identityPoolId = null;
    /**
     * Identity id generated by the Amazon Cognito Service.
     */
    private static String identityId = null;

    /**
     * Sets up the Amazon Cognito Identity and Sync client. Creates a new
     * identity pool and an Cognito Identity for the pool.
     */
    @BeforeClass
    public static void setUp() throws FileNotFoundException, IOException {
        setUpCredentials();
        identity = CognitoIdentityClient.builder().withCredentials(new AwsStaticCredentialsProvider(credentials)).build();
        sync = CognitoSyncClient.builder().withCredentials(new AwsStaticCredentialsProvider(credentials)).build();

        CreateIdentityPoolRequest createRequest = new CreateIdentityPoolRequest()
                .withIdentityPoolName(IDENTITY_POOL_NAME)
                .withAllowUnauthenticatedIdentities(true);
        createRequest.addSupportedLoginProvidersEntry(PROVIDER, APP_ID);
        CreateIdentityPoolResult result = identity
                .createIdentityPool(createRequest);
        identityPoolId = result.getIdentityPoolId();

        GetIdResult getIdResult = identity
                .getId(new GetIdRequest().withIdentityPoolId(identityPoolId)
                                         .withAccountId(AWS_ACCOUNT_ID));
        identityId = getIdResult.getIdentityId();

    }

    /**
     * Deletes the data set associated with the identity. Also deletes the
     * identity pool created for testing.
     */
    @AfterClass
    public static void tearDown() {

        if (DATASET_NAME != null) {
            sync.deleteDataset(new DeleteDatasetRequest()
                                       .withDatasetName(DATASET_NAME).withIdentityId(identityId)
                                       .withIdentityPoolId(identityPoolId));
        }

        if (identityPoolId != null) {
            identity.deleteIdentityPool(new DeleteIdentityPoolRequest()
                                                .withIdentityPoolId(identityPoolId));
        }
    }

    /**
     * Tests the Amazon Cognito sync functions.
     */
    @Test
    public void testCognitoSync() {
        testUpdateRecordPatches();
        testIdentityPoolUsage();
        testIdentityUsage();
    }

    /**
     * Tests the identity pool usage operation. Asserts
     */
    public void testIdentityPoolUsage() {
        DescribeIdentityPoolUsageResult describeIdentityPoolUsageResult = sync
                .describeIdentityPoolUsage(new DescribeIdentityPoolUsageRequest()
                                                   .withIdentityPoolId(identityPoolId));
        IdentityPoolUsage identityPoolUsage = describeIdentityPoolUsageResult
                .getIdentityPoolUsage();
        assertEquals(identityPoolUsage.getIdentityPoolId(), identityPoolId);
    }

    /**
     * Tests the describe identity usage operation for a given identity id.
     * Asserts that the number of data sets associated with the identity is 1.
     */
    public void testIdentityUsage() {
        DescribeIdentityUsageRequest describeIdentityUsageRequest = new DescribeIdentityUsageRequest()
                .withIdentityId(identityId).withIdentityPoolId(identityPoolId);
        DescribeIdentityUsageResult describeIdentityUsageResult = sync
                .describeIdentityUsage(describeIdentityUsageRequest);
        IdentityUsage identityUsage = describeIdentityUsageResult
                .getIdentityUsage();
        assertEquals(identityUsage.getDatasetCount(), Integer.valueOf(1));
        assertTrue(identityUsage.getDataStorage() >= Long.valueOf(0));
    }

    /**
     * Tests the record creations, record updates and record removal for a data
     * set in the given identity.
     *
     * Asserts that sync count at each case is incremented every time a record
     * is updated. Also asserts that the number of records in data set after
     * removal.
     */
    public void testUpdateRecordPatches() {
        RecordPatch record1 = new RecordPatch().withKey("foo1")
                                               .withValue("bar1").withOp(Operation.Replace).withSyncCount(0L);
        RecordPatch record2 = new RecordPatch().withKey("foo2")
                                               .withValue("bar2").withOp(Operation.Replace).withSyncCount(0L);

        ListRecordsRequest listRecordsRequest = new ListRecordsRequest()
                .withDatasetName(DATASET_NAME)
                .withIdentityPoolId(identityPoolId).withIdentityId(identityId);

        ListRecordsResult listRecordsResult = sync
                .listRecords(listRecordsRequest);

        assertEquals(listRecordsResult.getCount(), Integer.valueOf(0));
        assertNotNull(listRecordsResult.getSyncSessionToken());

        UpdateRecordsRequest updateRequest = new UpdateRecordsRequest()
                .withIdentityPoolId(identityPoolId).withIdentityId(identityId)
                .withDatasetName(DATASET_NAME)
                .withRecordPatches(record1, record2)
                .withSyncSessionToken(listRecordsResult.getSyncSessionToken());

        UpdateRecordsResult updateResult = sync.updateRecords(updateRequest);
        List<Record> records = updateResult.getRecords();
        assertEquals(records.size(), 2);
        assertEquals(records.get(0).getSyncCount(), Long.valueOf(1));
        assertEquals(records.get(1).getSyncCount(), Long.valueOf(1));

        record1.setValue("bar3");
        record1.setSyncCount(1L);

        listRecordsResult = sync.listRecords(listRecordsRequest);

        updateRequest = new UpdateRecordsRequest()
                .withIdentityPoolId(identityPoolId).withIdentityId(identityId)
                .withDatasetName(DATASET_NAME).withRecordPatches(record1)
                .withSyncSessionToken(listRecordsResult.getSyncSessionToken());

        updateResult = sync.updateRecords(updateRequest);
        records = updateResult.getRecords();
        assertEquals(records.size(), 1);
        assertEquals(records.get(0).getSyncCount(), Long.valueOf(2));

        DescribeDatasetRequest describeDatasetRequest = new DescribeDatasetRequest()
                .withDatasetName(DATASET_NAME).withIdentityId(identityId)
                .withIdentityPoolId(identityPoolId);

        DescribeDatasetResult describeDatasetResult = sync
                .describeDataset(describeDatasetRequest);

        Dataset dataset = describeDatasetResult.getDataset();

        assertEquals(dataset.getDatasetName(), DATASET_NAME);
        assertEquals(dataset.getNumRecords(), Long.valueOf(2));

        record1.setOp(Operation.Remove);
        record1.setSyncCount(2L);

        updateRequest = new UpdateRecordsRequest()
                .withIdentityPoolId(identityPoolId).withIdentityId(identityId)
                .withDatasetName(DATASET_NAME).withRecordPatches(record1)
                .withSyncSessionToken(listRecordsResult.getSyncSessionToken());

        updateResult = sync.updateRecords(updateRequest);
        records = updateResult.getRecords();
        assertEquals(records.size(), 1);
    }
}
