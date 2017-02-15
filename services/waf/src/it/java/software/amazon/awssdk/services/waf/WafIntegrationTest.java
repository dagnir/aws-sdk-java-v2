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

package software.amazon.awssdk.services.waf;

import java.io.IOException;
import java.util.List;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.services.waf.model.ChangeAction;
import software.amazon.awssdk.services.waf.model.CreateIPSetRequest;
import software.amazon.awssdk.services.waf.model.CreateIPSetResult;
import software.amazon.awssdk.services.waf.model.DeleteIPSetRequest;
import software.amazon.awssdk.services.waf.model.GetChangeTokenRequest;
import software.amazon.awssdk.services.waf.model.GetChangeTokenResult;
import software.amazon.awssdk.services.waf.model.GetIPSetRequest;
import software.amazon.awssdk.services.waf.model.GetIPSetResult;
import software.amazon.awssdk.services.waf.model.IPSet;
import software.amazon.awssdk.services.waf.model.IPSetDescriptor;
import software.amazon.awssdk.services.waf.model.IPSetDescriptorType;
import software.amazon.awssdk.services.waf.model.IPSetUpdate;
import software.amazon.awssdk.services.waf.model.ListIPSetsRequest;
import software.amazon.awssdk.services.waf.model.ListIPSetsResult;
import software.amazon.awssdk.services.waf.model.UpdateIPSetRequest;
import software.amazon.awssdk.test.AWSTestBase;

public class WafIntegrationTest extends AWSTestBase {

    private static final String IP_SET_NAME = "java-sdk-ipset-" + System.currentTimeMillis();
    private static final long SLEEP_TIME_MILLIS = 5000;
    private static final String IP_ADDRESS_RANGE = "192.0.2.0/24";
    private static AWSWAF client = null;
    private static String ipSetId = null;

    @BeforeClass
    public static void setup() throws IOException {
        setUpCredentials();
        client = new AWSWAFClient(credentials);

    }

    @AfterClass
    public static void tearDown() throws IOException {
        if (client != null) {

            deleteIpSet();

            client.shutdown();
        }
    }

    private static void deleteIpSet() {
        if (ipSetId != null) {
            final String changeToken = getNewChangeToken();
            client.deleteIPSet(new DeleteIPSetRequest()
                                       .withIPSetId(ipSetId)
                                       .withChangeToken(changeToken));
        }
    }

    private static String getNewChangeToken() {

        GetChangeTokenResult result = client.getChangeToken(new GetChangeTokenRequest());
        return result.getChangeToken();

    }

    @Test
    public void testOperations() throws InterruptedException {

        ipSetId = testCreateIpSet();
        testGetIpSet();
        testUpdateIpSet();
    }

    private String testCreateIpSet() {
        final String changeToken = getNewChangeToken();
        CreateIPSetResult createResult = client.createIPSet(new CreateIPSetRequest()
                                                                    .withChangeToken(changeToken)
                                                                    .withName(IP_SET_NAME));

        Assert.assertEquals(changeToken, createResult.getChangeToken());

        final IPSet ipSet = createResult.getIPSet();
        Assert.assertNotNull(ipSet);
        Assert.assertEquals(IP_SET_NAME, ipSet.getName());
        Assert.assertTrue(ipSet.getIPSetDescriptors().isEmpty());
        Assert.assertNotNull(ipSet.getIPSetId());

        return ipSet.getIPSetId();
    }

    private void testGetIpSet() {
        GetIPSetResult getResult = client.getIPSet(new GetIPSetRequest()
                                                           .withIPSetId(ipSetId));
        IPSet ipSet = getResult.getIPSet();
        Assert.assertNotNull(ipSet);
        Assert.assertEquals(IP_SET_NAME, ipSet.getName());
        Assert.assertTrue(ipSet.getIPSetDescriptors().isEmpty());
        Assert.assertNotNull(ipSet.getIPSetId());
        Assert.assertEquals(ipSetId, ipSet.getIPSetId());

        ListIPSetsResult listResult = client.listIPSets(new ListIPSetsRequest().withLimit(1));
        Assert.assertNotNull(listResult.getIPSets());
        Assert.assertFalse(listResult.getIPSets().isEmpty());
    }

    private void testUpdateIpSet() {
        final IPSetDescriptor ipSetDescriptor = new IPSetDescriptor()
                .withType(IPSetDescriptorType.IPV4)
                .withValue(IP_ADDRESS_RANGE);
        final IPSetUpdate ipToInsert = new IPSetUpdate()
                .withIPSetDescriptor(ipSetDescriptor)
                .withAction(ChangeAction.INSERT);


        client.updateIPSet(new UpdateIPSetRequest()
                                   .withIPSetId(ipSetId)
                                   .withChangeToken(getNewChangeToken())
                                   .withUpdates(ipToInsert));
        GetIPSetResult getResult = client.getIPSet(new GetIPSetRequest()
                                                           .withIPSetId(ipSetId));

        IPSet ipSet = getResult.getIPSet();
        Assert.assertNotNull(ipSet);
        Assert.assertEquals(IP_SET_NAME, ipSet.getName());
        Assert.assertNotNull(ipSet.getIPSetId());
        Assert.assertEquals(ipSetId, ipSet.getIPSetId());

        List<IPSetDescriptor> actualList = ipSet.getIPSetDescriptors();

        Assert.assertFalse(actualList.isEmpty());
        Assert.assertEquals(1, actualList.size());
        IPSetDescriptor actualIpSetDescriptor = actualList.get(0);
        Assert.assertEquals(ipSetDescriptor.getType(), actualIpSetDescriptor.getType());
        Assert.assertEquals(ipSetDescriptor.getValue(), actualIpSetDescriptor.getValue());

        final IPSetUpdate ipToDelete = new IPSetUpdate()
                .withIPSetDescriptor(ipSetDescriptor)
                .withAction(ChangeAction.DELETE);

        client.updateIPSet(new UpdateIPSetRequest()
                                   .withIPSetId(ipSetId)
                                   .withChangeToken(getNewChangeToken())
                                   .withUpdates(ipToDelete));
    }
}
