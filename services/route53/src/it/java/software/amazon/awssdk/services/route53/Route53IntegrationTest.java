package software.amazon.awssdk.services.route53;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.UUID;

import org.junit.AfterClass;
import org.junit.Test;

import software.amazon.awssdk.AmazonServiceException;
import software.amazon.awssdk.ResponseMetadata;
import software.amazon.awssdk.SDKGlobalTime;
import software.amazon.awssdk.services.route53.model.Change;
import software.amazon.awssdk.services.route53.model.ChangeAction;
import software.amazon.awssdk.services.route53.model.ChangeBatch;
import software.amazon.awssdk.services.route53.model.ChangeInfo;
import software.amazon.awssdk.services.route53.model.ChangeResourceRecordSetsRequest;
import software.amazon.awssdk.services.route53.model.CreateHealthCheckRequest;
import software.amazon.awssdk.services.route53.model.CreateHealthCheckResult;
import software.amazon.awssdk.services.route53.model.CreateHostedZoneRequest;
import software.amazon.awssdk.services.route53.model.CreateHostedZoneResult;
import software.amazon.awssdk.services.route53.model.DelegationSet;
import software.amazon.awssdk.services.route53.model.DeleteHealthCheckRequest;
import software.amazon.awssdk.services.route53.model.DeleteHostedZoneRequest;
import software.amazon.awssdk.services.route53.model.DeleteHostedZoneResult;
import software.amazon.awssdk.services.route53.model.GetChangeRequest;
import software.amazon.awssdk.services.route53.model.GetHealthCheckRequest;
import software.amazon.awssdk.services.route53.model.GetHealthCheckResult;
import software.amazon.awssdk.services.route53.model.GetHostedZoneRequest;
import software.amazon.awssdk.services.route53.model.GetHostedZoneResult;
import software.amazon.awssdk.services.route53.model.HealthCheck;
import software.amazon.awssdk.services.route53.model.HealthCheckConfig;
import software.amazon.awssdk.services.route53.model.HostedZone;
import software.amazon.awssdk.services.route53.model.HostedZoneConfig;
import software.amazon.awssdk.services.route53.model.ListHostedZonesRequest;
import software.amazon.awssdk.services.route53.model.ListResourceRecordSetsRequest;
import software.amazon.awssdk.services.route53.model.RRType;
import software.amazon.awssdk.services.route53.model.ResourceRecord;
import software.amazon.awssdk.services.route53.model.ResourceRecordSet;

/**
 * Integration tests that run through the various operations available in the
 * Route 53 API.
 */
public class Route53IntegrationTest extends IntegrationTestBase {

    private static final String COMMENT = "comment";
    private static final String ZONE_NAME = "java.sdk.com.";
    private static final String CALLER_REFERENCE = UUID.randomUUID().toString();
    private static final int PORT_NUM = 22;
    private static final String TYPE = "TCP";
    private static final String IP_ADDRESS = "12.12.12.12";

    /** The ID of the zone we created in this test */
    private static String createdZoneId;

    /** The ID of the change that created our test zone */
    private String createdZoneChangeId;

    /** the ID of the health check */
    private String healthCheckId;


    /** Ensures the HostedZone we create during this test is correctly released. */
    @AfterClass
    public static void tearDown() {
        try {
            route53.deleteHostedZone(new DeleteHostedZoneRequest(createdZoneId));
        } catch (Exception e) { }
    }

    /**
     * Runs through each of the APIs in the Route 53 client to make sure we can
     * correct send requests and unmarshall responses.
     */
    @Test
    public void testRoute53() throws Exception {
        // Create Hosted Zone
        CreateHostedZoneResult result = route53.createHostedZone(new CreateHostedZoneRequest()
            .withName(ZONE_NAME)
            .withCallerReference(CALLER_REFERENCE)
            .withHostedZoneConfig(new HostedZoneConfig()
                .withComment(COMMENT))
        );

        createdZoneId       = result.getHostedZone().getId();
        createdZoneChangeId = result.getChangeInfo().getId();

        assertValidCreatedHostedZone(result.getHostedZone());
        assertValidDelegationSet(result.getDelegationSet());
        assertValidChangeInfo(result.getChangeInfo());
        assertNotNull(result.getLocation());


        // Get Hosted Zone
        GetHostedZoneRequest getHostedZoneRequest = new GetHostedZoneRequest(createdZoneId);
        GetHostedZoneResult getHostedZoneResult = route53.getHostedZone(getHostedZoneRequest);
        assertValidDelegationSet(getHostedZoneResult.getDelegationSet());
        assertValidCreatedHostedZone(getHostedZoneResult.getHostedZone());
        ResponseMetadata metadata = route53.getCachedResponseMetadata(getHostedZoneRequest);
        assertNotNull(metadata);
        assertNotNull(metadata.getRequestId());

        // Create a health check
        HealthCheckConfig config = new HealthCheckConfig().withType("TCP").withPort(PORT_NUM).withIPAddress(IP_ADDRESS);
        CreateHealthCheckResult createHealthCheckResult = route53.createHealthCheck(new CreateHealthCheckRequest().withHealthCheckConfig(config).withCallerReference(CALLER_REFERENCE));
        healthCheckId = createHealthCheckResult.getHealthCheck().getId();
        assertNotNull(createHealthCheckResult.getLocation());
        assertValidHealthCheck(createHealthCheckResult.getHealthCheck());

        // Get the health check back
        GetHealthCheckResult getHealthCheckResult = route53.getHealthCheck(new GetHealthCheckRequest().withHealthCheckId(healthCheckId));
        assertValidHealthCheck(getHealthCheckResult.getHealthCheck());

        // Delete the health check
        route53.deleteHealthCheck(new DeleteHealthCheckRequest().withHealthCheckId(healthCheckId));

        // Get the health check back
        try {
            getHealthCheckResult = route53.getHealthCheck(new GetHealthCheckRequest().withHealthCheckId(healthCheckId));
            fail();
        } catch (AmazonServiceException e) {
            assertNotNull(e.getMessage());
            assertNotNull(e.getErrorCode());
            assertNotNull(e.getErrorType());
        }

        // List Hosted Zones
        List<HostedZone> hostedZones = route53.listHostedZones(new ListHostedZonesRequest()).getHostedZones();
        assertTrue(hostedZones.size() > 0);
        for (HostedZone hostedZone : hostedZones) {
            assertNotNull(hostedZone.getCallerReference());
            assertNotNull(hostedZone.getId());
            assertNotNull(hostedZone.getName());
        }


        // List Resource Record Sets
        List<ResourceRecordSet> resourceRecordSets = route53.listResourceRecordSets(
                new ListResourceRecordSetsRequest(createdZoneId)).getResourceRecordSets();
        assertTrue(resourceRecordSets.size() > 0);
        ResourceRecordSet existingResourceRecordSet = resourceRecordSets.get(0);
        for (ResourceRecordSet rrset : resourceRecordSets) {
            assertNotNull(rrset.getName());
            assertNotNull(rrset.getType());
            assertNotNull(rrset.getTTL());
            assertTrue(rrset.getResourceRecords().size() > 0);
        }


        // Get Change
        ChangeInfo changeInfo = route53.getChange(new GetChangeRequest(createdZoneChangeId)).getChangeInfo();
        assertTrue(changeInfo.getId().endsWith(createdZoneChangeId));
        assertValidChangeInfo(changeInfo);


        // Change Resource Record Sets
        ResourceRecordSet newResourceRecordSet = new ResourceRecordSet()
            .withName(ZONE_NAME)
            .withResourceRecords(existingResourceRecordSet.getResourceRecords())
            .withTTL(existingResourceRecordSet.getTTL() + 100)
            .withType(existingResourceRecordSet.getType());

        changeInfo = route53.changeResourceRecordSets(new ChangeResourceRecordSetsRequest()
            .withHostedZoneId(createdZoneId)
            .withChangeBatch(new ChangeBatch().withComment(COMMENT)
                    .withChanges(new Change().withAction(ChangeAction.DELETE).withResourceRecordSet(existingResourceRecordSet),
                                 new Change().withAction(ChangeAction.CREATE).withResourceRecordSet(newResourceRecordSet))
            )).getChangeInfo();
        assertValidChangeInfo(changeInfo);

        // Add a weighted Resource Record Set so we can reproduce the bug reported by customers
        // when they provide SetIdentifier containing special characters.
        String specialChars = "&<>'\"";
        newResourceRecordSet = new ResourceRecordSet()
                .withName("weighted." + ZONE_NAME)
                .withType(RRType.CNAME)
                .withSetIdentifier(specialChars)
                .withWeight(0L)
                .withTTL(1000L)
                .withResourceRecords(
                        new ResourceRecord().withValue("www.example.com"));
        changeInfo = route53.changeResourceRecordSets(new ChangeResourceRecordSetsRequest()
            .withHostedZoneId(createdZoneId)
            .withChangeBatch(
                        new ChangeBatch().withComment(COMMENT).withChanges(
                                new Change().withAction(ChangeAction.CREATE)
                                            .withResourceRecordSet(newResourceRecordSet)))
            ).getChangeInfo();
        assertValidChangeInfo(changeInfo);

        // Clear up the RR Set
        changeInfo = route53.changeResourceRecordSets(new ChangeResourceRecordSetsRequest()
            .withHostedZoneId(createdZoneId)
            .withChangeBatch(
                    new ChangeBatch().withComment(COMMENT).withChanges(
                            new Change().withAction(ChangeAction.DELETE)
                                        .withResourceRecordSet(newResourceRecordSet)))
            ).getChangeInfo();

        // Delete Hosted Zone
        DeleteHostedZoneResult deleteHostedZoneResult = route53.deleteHostedZone(new DeleteHostedZoneRequest(createdZoneId));
        assertValidChangeInfo(deleteHostedZoneResult.getChangeInfo());
    }


    /**
     * Asserts that the specified HostedZone is valid and represents the same
     * HostedZone that we initially created at the very start of this test.
     *
     * @param hostedZone The hosted zone to test.
     */
    private void assertValidCreatedHostedZone(HostedZone hostedZone) {
        assertEquals(CALLER_REFERENCE, hostedZone.getCallerReference());
        assertEquals(ZONE_NAME, hostedZone.getName());
        assertNotNull(hostedZone.getId());
        assertEquals(COMMENT, hostedZone.getConfig().getComment());
    }

    /**
     * Asserts that the specified DelegationSet is valid.
     * @param delegationSet The delegation set to test.
     */
    private void assertValidDelegationSet(DelegationSet delegationSet) {
        assertTrue(delegationSet.getNameServers().size() > 0);
        for (String server : delegationSet.getNameServers()) {
            assertNotNull(server);
        }
    }

    /**
     * Asserts that the specified ChangeInfo is valid.
     * @param change The ChangeInfo object to test.
     */
    private void assertValidChangeInfo(ChangeInfo change) {
        assertNotNull(change.getId());
        assertNotNull(change.getStatus());
        assertNotNull(change.getSubmittedAt());
    }

    private void assertValidHealthCheck(HealthCheck healthCheck) {
        assertNotNull(CALLER_REFERENCE, healthCheck.getCallerReference());
        assertNotNull(healthCheck.getId());
        assertEquals(PORT_NUM, healthCheck.getHealthCheckConfig().getPort().intValue());
        assertEquals(TYPE, healthCheck.getHealthCheckConfig().getType());
        assertEquals(IP_ADDRESS, healthCheck.getHealthCheckConfig().getIPAddress());
    }

    /**
     * In the following test, we purposely setting the time offset to trigger a clock skew error.
     * The time offset must be fixed and then we validate the global value for time offset has been
     * update.
     */
    @Test
    public void testClockSkew() throws AmazonServiceException {
        SDKGlobalTime.setGlobalTimeOffset(3600);
        AmazonRoute53Client clockSkewClient = new AmazonRoute53Client(credentials);
        clockSkewClient.listHostedZones();
        assertTrue("Clockskew is fixed!", SDKGlobalTime.getGlobalTimeOffset() < 60);
    }
}
