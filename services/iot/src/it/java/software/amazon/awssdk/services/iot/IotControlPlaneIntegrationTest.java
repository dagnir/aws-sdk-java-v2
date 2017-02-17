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

package software.amazon.awssdk.services.iot;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.regions.Regions;
import software.amazon.awssdk.services.iot.model.AttributePayload;
import software.amazon.awssdk.services.iot.model.CertificateStatus;
import software.amazon.awssdk.services.iot.model.CreateCertificateFromCsrRequest;
import software.amazon.awssdk.services.iot.model.CreateKeysAndCertificateRequest;
import software.amazon.awssdk.services.iot.model.CreateKeysAndCertificateResult;
import software.amazon.awssdk.services.iot.model.CreatePolicyRequest;
import software.amazon.awssdk.services.iot.model.CreatePolicyResult;
import software.amazon.awssdk.services.iot.model.CreateThingRequest;
import software.amazon.awssdk.services.iot.model.CreateThingResult;
import software.amazon.awssdk.services.iot.model.DeleteCertificateRequest;
import software.amazon.awssdk.services.iot.model.DeletePolicyRequest;
import software.amazon.awssdk.services.iot.model.DeleteThingRequest;
import software.amazon.awssdk.services.iot.model.DescribeThingRequest;
import software.amazon.awssdk.services.iot.model.DescribeThingResult;
import software.amazon.awssdk.services.iot.model.GetPolicyVersionRequest;
import software.amazon.awssdk.services.iot.model.GetPolicyVersionResult;
import software.amazon.awssdk.services.iot.model.InvalidRequestException;
import software.amazon.awssdk.services.iot.model.ListThingsRequest;
import software.amazon.awssdk.services.iot.model.ListThingsResult;
import software.amazon.awssdk.services.iot.model.UpdateCertificateRequest;
import software.amazon.awssdk.test.AwsTestBase;

/**
 * Integration tests for Iot control plane APIs.
 */
public class IotControlPlaneIntegrationTest extends AwsTestBase {

    private static final String THING_NAME = "java-sdk-thing-" + System.currentTimeMillis();
    private static final Map<String, String> THING_ATTRIBUTES = new HashMap<String, String>();
    private static final String ATTRIBUTE_NAME = "foo";
    private static final String ATTRIBUTE_VALUE = "bar";
    private static final String POLICY_NAME = "java-sdk-iot-policy-" + System.currentTimeMillis();
    private static final String POLICY_DOC = "{\n" +
                                             "  \"Version\": \"2012-10-17\",\n" +
                                             "  \"Statement\": [\n" +
                                             "    {\n" +
                                             "      \"Sid\": \"Stmt1443818583140\",\n" +
                                             "      \"Action\": \"iot:*\",\n" +
                                             "      \"Effect\": \"Deny\",\n" +
                                             "      \"Resource\": \"*\"\n" +
                                             "    }\n" +
                                             "  ]\n" +
                                             "}";
    private static AWSIotClient client;
    private static String certificateId = null;

    @BeforeClass
    public static void setup() throws IOException {
        setUpCredentials();
        client = new AWSIotClient(credentials);
        client.configureRegion(Regions.US_WEST_2);
        THING_ATTRIBUTES.put(ATTRIBUTE_NAME, ATTRIBUTE_VALUE);
    }

    @AfterClass
    public static void tearDown() throws IOException {
        if (client != null) {
            client.deleteThing(new DeleteThingRequest().withThingName(THING_NAME));
            client.deletePolicy(new DeletePolicyRequest().withPolicyName(POLICY_NAME));
            if (certificateId != null) {
                client.deleteCertificate(new DeleteCertificateRequest().withCertificateId(certificateId));
            }
            client.shutdown();
        }
    }

    @Test
    public void describe_and_list_thing_returns_created_thing() {

        final CreateThingRequest createReq = new CreateThingRequest()
                .withThingName(THING_NAME)
                .withAttributePayload(new AttributePayload()
                                              .withAttributes(THING_ATTRIBUTES));
        CreateThingResult result = client.createThing(createReq);
        Assert.assertNotNull(result.getThingArn());
        Assert.assertEquals(THING_NAME, result.getThingName());

        final DescribeThingRequest descRequest = new DescribeThingRequest()
                .withThingName(THING_NAME);

        DescribeThingResult descResult = client.describeThing(descRequest);
        Map<String, String> actualAttributes = descResult.getAttributes();
        Assert.assertEquals(THING_ATTRIBUTES.size(), actualAttributes.size());
        Assert.assertTrue(actualAttributes.containsKey(ATTRIBUTE_NAME));
        Assert.assertEquals(THING_ATTRIBUTES.get(ATTRIBUTE_NAME), actualAttributes.get(ATTRIBUTE_NAME));

        ListThingsResult listResult = client.listThings(new ListThingsRequest());
        Assert.assertFalse(listResult.getThings().isEmpty());
    }

    @Test
    public void get_policy_returns_created_policy() {

        final CreatePolicyRequest createReq = new CreatePolicyRequest()
                .withPolicyName(POLICY_NAME)
                .withPolicyDocument(POLICY_DOC);

        CreatePolicyResult createResult = client.createPolicy(createReq);
        Assert.assertNotNull(createResult.getPolicyArn());
        Assert.assertNotNull(createResult.getPolicyVersionId());


        final GetPolicyVersionRequest getRequest = new GetPolicyVersionRequest()
                .withPolicyName(POLICY_NAME)
                .withPolicyVersionId(createResult.getPolicyVersionId());

        GetPolicyVersionResult getResult = client.getPolicyVersion(getRequest);
        Assert.assertEquals(createResult.getPolicyArn(), getResult.getPolicyArn());
        Assert.assertEquals(createResult.getPolicyVersionId(), getResult.getPolicyVersionId());
    }

    @Test
    public void createCertificate_Returns_success() {
        final CreateKeysAndCertificateRequest createReq = new CreateKeysAndCertificateRequest()
                .withSetAsActive(true);
        CreateKeysAndCertificateResult createResult = client.createKeysAndCertificate(createReq);
        Assert.assertNotNull(createResult.getCertificateArn());
        Assert.assertNotNull(createResult.getCertificateId());
        Assert.assertNotNull(createResult.getCertificatePem());
        Assert.assertNotNull(createResult.getKeyPair());

        certificateId = createResult.getCertificateId();

        client.updateCertificate(new UpdateCertificateRequest()
                                         .withCertificateId(certificateId)
                                         .withNewStatus(CertificateStatus.REVOKED));
    }

    @Test(expected = InvalidRequestException.class)
    public void create_certificate_from_invalid_csr_throws_exception() {
        client.createCertificateFromCsr(new CreateCertificateFromCsrRequest()
                                                .withCertificateSigningRequest("invalid-csr-string"));
    }
}
