/*
 * Copyright 2013-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.services.cognitoidentity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.AmazonServiceException;
import software.amazon.awssdk.services.cognitoidentity.model.CreateIdentityPoolRequest;
import software.amazon.awssdk.services.cognitoidentity.model.CreateIdentityPoolResult;
import software.amazon.awssdk.services.cognitoidentity.model.DeleteIdentityPoolRequest;
import software.amazon.awssdk.services.cognitoidentity.model.DescribeIdentityPoolRequest;
import software.amazon.awssdk.services.cognitoidentity.model.DescribeIdentityPoolResult;
import software.amazon.awssdk.services.cognitoidentity.model.GetIdRequest;
import software.amazon.awssdk.services.cognitoidentity.model.GetIdResult;
import software.amazon.awssdk.services.cognitoidentity.model.GetOpenIdTokenRequest;
import software.amazon.awssdk.services.cognitoidentity.model.GetOpenIdTokenResult;
import software.amazon.awssdk.services.cognitoidentity.model.IdentityPoolShortDescription;
import software.amazon.awssdk.services.cognitoidentity.model.ListIdentitiesRequest;
import software.amazon.awssdk.services.cognitoidentity.model.ListIdentitiesResult;
import software.amazon.awssdk.services.cognitoidentity.model.ListIdentityPoolsRequest;
import software.amazon.awssdk.services.cognitoidentity.model.ListIdentityPoolsResult;
import software.amazon.awssdk.services.cognitoidentity.model.UnlinkIdentityRequest;
import software.amazon.awssdk.services.cognitoidentity.model.UpdateIdentityPoolRequest;
import software.amazon.awssdk.services.cognitoidentity.model.UpdateIdentityPoolResult;
import software.amazon.awssdk.test.AWSTestBase;

/**
 * Test suite for Amazon Cognito identity Java Client.
 */
public class CognitoIdentityIntegrationTest extends AWSTestBase {

    /**
     * Name of the identity pool created for testing.
     */
    private static final String IDENTITY_POOL_NAME = "javasdkpool"
                                                     + System.currentTimeMillis();
    /**
     * AWS account id used for generating the identity id.
     */
    private static final String awsAccountId = "599169622985";
    /**
     * Provider supported by the identity pool and associated with the
     * identities.
     */
    private static final String PROVIDER = "foo";
    /** APP id for the provider associated with the identity pool. */
    private static final String APP_ID = "fooId";
    /** login access token for the provider. */
    private static final String ACCESS_TOKEN = "accessToken";
    /**
     * Reference to the Amazon Cognito identity client.
     */
    private static AmazonCognitoIdentity identity;
    /**
     * Identity pool id generated by the service.
     */
    private static String identityPoolId = null;
    /**
     * Identity id generated by the service for the given AWS account id.
     */
    private static String identityId = null;

    /**
     * Sets up the client with the credentials.
     */
    @BeforeClass
    public static void setUp() throws FileNotFoundException, IOException {
        setUpCredentials();
        identity = new AmazonCognitoIdentityClient(credentials);
    }

    /**
     * Deletes the identity pool created as part of the testing.
     */
    @AfterClass
    public static void tearDown() {
        if (identityPoolId != null) {
            identity.deleteIdentityPool(new DeleteIdentityPoolRequest()
                                                .withIdentityPoolId(identityPoolId));
        }
    }

    /**
     * Tests the different functionalities of the cognito identity service.
     */
    @Test
    public void testCognitoIdentityFunctions() {
        testCreateIdentityPool();
        testUpdateIdentityPool();
        testDescribeIdentityPool();
        testListIdentityPool();
        testGetId();
        testGetOpenId();
        testListIdentities();
        testUnlinkIdentity();
    }

    /**
     * Tests the create identity pool functionality. The identity pool is
     * created with access to un authenticated identities. Asserts that the pool
     * created allows un authenticated identities. Asserts that the identity
     * pool created has an identity id.
     */
    public void testCreateIdentityPool() {
        CreateIdentityPoolResult result = identity
                .createIdentityPool(new CreateIdentityPoolRequest()
                                            .withIdentityPoolName(IDENTITY_POOL_NAME)
                                            .withAllowUnauthenticatedIdentities(true));
        assertEquals(result.getIdentityPoolName(), IDENTITY_POOL_NAME);
        assertNotNull(result.getIdentityPoolId());
        assertTrue(result.getAllowUnauthenticatedIdentities());
        identityPoolId = result.getIdentityPoolId();
    }

    /**
     * Tests the Get identity functionality. For the given aws account id, it
     * generates a identity and returns the id associated with it.
     */
    public void testGetId() {

        GetIdResult result = identity
                .getId(new GetIdRequest().withIdentityPoolId(identityPoolId)
                                         .withAccountId(awsAccountId));
        assertNotNull(result.getIdentityId());
        identityId = result.getIdentityId();
    }

    /**
     * Tests the List identity functionality. There must be at-least one
     * identity in the identity pool.
     */
    public void testListIdentities() {
        ListIdentitiesResult result = identity
                .listIdentities(new ListIdentitiesRequest().withIdentityPoolId(
                        identityPoolId).withMaxResults(60));
        assertTrue(result.getIdentities().size() == 1);
    }

    /**
     * Tries to fetch an Open Id for the given Amazon Cognito Identity. Asserts
     * that the identity id and access token is not null.
     */
    public void testGetOpenId() {
        GetOpenIdTokenResult result = identity
                .getOpenIdToken(new GetOpenIdTokenRequest()
                                        .withIdentityId(identityId));
        assertNotNull(result.getIdentityId());
        assertNotNull(result.getToken());
    }

    /**
     * Performs a describe identity pool operation. Asserts that the
     * configuration associated with the identity pool is correct.
     */
    public void testDescribeIdentityPool() {
        DescribeIdentityPoolResult result = identity
                .describeIdentityPool(new DescribeIdentityPoolRequest()
                                              .withIdentityPoolId(identityPoolId));

        assertEquals(result.getIdentityPoolName(), IDENTITY_POOL_NAME);
        assertTrue(result.getAllowUnauthenticatedIdentities());
        Map<String, String> supportedLoginProviders = result
                .getSupportedLoginProviders();
        assertTrue(supportedLoginProviders.containsKey(PROVIDER));
        assertTrue(supportedLoginProviders.containsValue(APP_ID));
    }

    /**
     * Tries to Unlink an identity from the identity pool. Should throw an
     * exception as there are no logins associated with the identity.
     */
    public void testUnlinkIdentity() {
        Map<String, String> associatedLogins = new HashMap<String, String>();
        associatedLogins.put(PROVIDER, ACCESS_TOKEN);

        try {
            identity.unlinkIdentity(new UnlinkIdentityRequest()
                                            .withIdentityId(identityId).withLoginsToRemove("foo")
                                            .withLogins(associatedLogins));
            fail("Should fail as the provider and the identity doesn't have a login associated with it.");
        } catch (AmazonServiceException ase) {
            assertEquals(ase.getStatusCode(), 400);
        }

    }

    /**
     * Updates the configuration of identity pool with the login providers and
     * asserts to see if it is updated.
     */
    public void testUpdateIdentityPool() {

        Map<String, String> supportedLoginProviders = new HashMap<String, String>();
        supportedLoginProviders.put(PROVIDER, APP_ID);

        UpdateIdentityPoolResult result = identity
                .updateIdentityPool(new UpdateIdentityPoolRequest()
                                            .withIdentityPoolId(identityPoolId)
                                            .withIdentityPoolName(IDENTITY_POOL_NAME)
                                            .withAllowUnauthenticatedIdentities(true)
                                            .withSupportedLoginProviders(supportedLoginProviders));

        assertTrue(result.getAllowUnauthenticatedIdentities());
        Map<String, String> returnedLoginProviders = result
                .getSupportedLoginProviders();
        assertEquals(returnedLoginProviders.size(), 1);
        assertEquals(returnedLoginProviders.get(PROVIDER), APP_ID);

    }

    /**
     * Tests the list identity pool functionality. Asserts that the number of
     * identity pools is greater than or equal to 1.
     */
    public void testListIdentityPool() {
        ListIdentityPoolsResult result = identity
                .listIdentityPools(new ListIdentityPoolsRequest()
                                           .withMaxResults(60));

        List<IdentityPoolShortDescription> identityPools = result
                .getIdentityPools();
        assertTrue(identityPools.size() >= 1);
    }
}
