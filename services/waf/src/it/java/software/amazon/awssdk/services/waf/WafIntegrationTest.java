package software.amazon.awssdk.services.waf;

import software.amazon.awssdk.services.waf.model.*;
import software.amazon.awssdk.test.AWSTestBase;
import org.junit.Assert;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

public class WafIntegrationTest extends AWSTestBase {

    private static AWSWAF client = null;

    private static final String IP_SET_NAME = "java-sdk-ipset-" + System.currentTimeMillis();

    private static String ipSetId = null;

    private static final long SLEEP_TIME_MILLIS = 5000;

    private static final String IP_ADDRESS_RANGE = "192.0.2.0/24";

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

    private void testUpdateIpSet(){
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

    private static String getNewChangeToken() {

        GetChangeTokenResult result = client.getChangeToken(new GetChangeTokenRequest());
        return result.getChangeToken();

    }
}
