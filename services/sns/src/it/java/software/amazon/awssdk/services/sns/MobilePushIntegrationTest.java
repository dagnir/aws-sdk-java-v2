package software.amazon.awssdk.services.sns;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.junit.Test;

import software.amazon.awssdk.services.sns.model.CreatePlatformApplicationRequest;
import software.amazon.awssdk.services.sns.model.CreatePlatformApplicationResult;
import software.amazon.awssdk.services.sns.model.CreatePlatformEndpointRequest;
import software.amazon.awssdk.services.sns.model.CreatePlatformEndpointResult;
import software.amazon.awssdk.services.sns.model.CreateTopicRequest;
import software.amazon.awssdk.services.sns.model.CreateTopicResult;
import software.amazon.awssdk.services.sns.model.DeleteEndpointRequest;
import software.amazon.awssdk.services.sns.model.DeletePlatformApplicationRequest;
import software.amazon.awssdk.services.sns.model.DeleteTopicRequest;
import software.amazon.awssdk.services.sns.model.Endpoint;
import software.amazon.awssdk.services.sns.model.GetEndpointAttributesRequest;
import software.amazon.awssdk.services.sns.model.GetEndpointAttributesResult;
import software.amazon.awssdk.services.sns.model.GetPlatformApplicationAttributesRequest;
import software.amazon.awssdk.services.sns.model.GetPlatformApplicationAttributesResult;
import software.amazon.awssdk.services.sns.model.ListEndpointsByPlatformApplicationRequest;
import software.amazon.awssdk.services.sns.model.ListEndpointsByPlatformApplicationResult;
import software.amazon.awssdk.services.sns.model.ListPlatformApplicationsResult;
import software.amazon.awssdk.services.sns.model.PlatformApplication;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResult;
import software.amazon.awssdk.services.sns.model.SetEndpointAttributesRequest;
import software.amazon.awssdk.services.sns.model.SetPlatformApplicationAttributesRequest;

public class MobilePushIntegrationTest extends IntegrationTestBase {

    private String platformAppName = "JavaSDKTestApp" + new Random().nextInt();
    private String platformCredential = "AIzaSyD-4pBAk6M7eveE9dwRFyGv-cfYBPiHRmk";
    private String token = "APA91bHXHl9bxaaNvHHWNXKwzzaeAjJnBP3g6ieaGta1aPMgrilr0H-QL4AxUZUJ-1mk0gnLpmeXF0Kg7-9fBXfXHTKzPGlCyT6E6oOfpdwLpcRMxQp5vCPFiFeru9oQylc22HvZSwQTDgmmw9WdNlXMerUPzmoX0w";

    /**
     * Tests for mobile push API
     * 
     * @throws InterruptedException
     */
    @Test
    public void testMobilePushOperations() throws InterruptedException {

        String platformApplicationArn = null;
        String endpointArn = null;
        String topicArn = null;

        try {
            CreateTopicResult createTopicResult = sns.createTopic(new CreateTopicRequest("TestTopic"));
            topicArn = createTopicResult.getTopicArn();

            // List platform applications
            ListPlatformApplicationsResult listPlatformAppsResult = sns.listPlatformApplications();
            int platformAppsCount = listPlatformAppsResult.getPlatformApplications().size();
            for (PlatformApplication platformApp : listPlatformAppsResult.getPlatformApplications()) {
                assertNotNull(platformApp.getPlatformApplicationArn());
                validateAttributes(platformApp.getAttributes());
            }

            // Create a platform application for GCM.
            Map<String, String> attributes = new HashMap<String, String>();
            attributes.put("PlatformCredential", platformCredential);
            attributes.put("PlatformPrincipal", "NA");
            attributes.put("EventEndpointCreated", topicArn);
            attributes.put("EventEndpointDeleted", topicArn);
            attributes.put("EventEndpointUpdated", topicArn);
            attributes.put("EventDeliveryAttemptFailure", topicArn);
            attributes.put("EventDeliveryFailure", "");
            CreatePlatformApplicationResult createPlatformAppResult = sns
                    .createPlatformApplication(new CreatePlatformApplicationRequest().withName(platformAppName)
                            .withPlatform("GCM").withAttributes(attributes));
            assertNotNull(createPlatformAppResult.getPlatformApplicationArn());
            platformApplicationArn = createPlatformAppResult.getPlatformApplicationArn();

            Thread.sleep(5 * 1000);
            listPlatformAppsResult = sns.listPlatformApplications();
            assertEquals(platformAppsCount + 1, listPlatformAppsResult.getPlatformApplications().size());

            // Get attributes
            GetPlatformApplicationAttributesResult getPlatformAttributesResult = sns.getPlatformApplicationAttributes(
                    new GetPlatformApplicationAttributesRequest().withPlatformApplicationArn(platformApplicationArn));
            validateAttributes(getPlatformAttributesResult.getAttributes());

            // Set attributes
            attributes.clear();
            attributes.put("EventDeliveryFailure", topicArn);

            sns.setPlatformApplicationAttributes(new SetPlatformApplicationAttributesRequest()
                    .withPlatformApplicationArn(platformApplicationArn).withAttributes(attributes));

            Thread.sleep(1 * 1000);
            // Verify attribute change
            getPlatformAttributesResult = sns.getPlatformApplicationAttributes(
                    new GetPlatformApplicationAttributesRequest().withPlatformApplicationArn(platformApplicationArn));
            validateAttribute(getPlatformAttributesResult.getAttributes(), "EventDeliveryFailure", topicArn);

            // Create platform endpoint
            CreatePlatformEndpointResult createPlatformEndpointResult = sns.createPlatformEndpoint(
                    new CreatePlatformEndpointRequest().withPlatformApplicationArn(platformApplicationArn)
                            .withCustomUserData("Custom Data").withToken(token));
            assertNotNull(createPlatformEndpointResult.getEndpointArn());
            endpointArn = createPlatformEndpointResult.getEndpointArn();

            // List platform endpoints
            Thread.sleep(5 * 1000);
            ListEndpointsByPlatformApplicationResult listEndpointsResult = sns.listEndpointsByPlatformApplication(
                    new ListEndpointsByPlatformApplicationRequest().withPlatformApplicationArn(platformApplicationArn));
            assertTrue(listEndpointsResult.getEndpoints().size() == 1);
            for (Endpoint endpoint : listEndpointsResult.getEndpoints()) {
                assertNotNull(endpoint.getEndpointArn());
                validateAttributes(endpoint.getAttributes());
            }

            // Publish to the endpoint
            PublishResult publishResult = sns.publish(new PublishRequest().withMessage("Mobile push test message")
                    .withSubject("Mobile Push test subject").withTargetArn(endpointArn));
            assertNotNull(publishResult.getMessageId());

            // Get endpoint attributes
            GetEndpointAttributesResult getEndpointAttributesResult = sns
                    .getEndpointAttributes(new GetEndpointAttributesRequest().withEndpointArn(endpointArn));
            validateAttributes(getEndpointAttributesResult.getAttributes());

            // Set endpoint attributes
            attributes.clear();
            attributes.put("CustomUserData", "Updated Custom Data");
            sns.setEndpointAttributes(
                    new SetEndpointAttributesRequest().withEndpointArn(endpointArn).withAttributes(attributes));

            Thread.sleep(1 * 1000);
            // Validate set endpoint attributes
            getEndpointAttributesResult = sns
                    .getEndpointAttributes(new GetEndpointAttributesRequest().withEndpointArn(endpointArn));
            validateAttribute(getEndpointAttributesResult.getAttributes(), "CustomUserData", "Updated Custom Data");

        } finally {
            if (platformApplicationArn != null) {
                if (endpointArn != null) {
                    // Delete endpoint
                    sns.deleteEndpoint(new DeleteEndpointRequest().withEndpointArn(endpointArn));
                }
                // Delete application platform
                sns.deletePlatformApplication(
                        new DeletePlatformApplicationRequest().withPlatformApplicationArn(platformApplicationArn));
            }
            if (topicArn != null) {
                // Delete the topic
                sns.deleteTopic(new DeleteTopicRequest(topicArn));
            }
        }
    }

    private void validateAttributes(Map<String, String> attributes) {
        for (Map.Entry<String, String> attribute : attributes.entrySet()) {
            assertNotNull(attribute.getKey());
            assertNotNull(attribute.getValue());
        }
    }

    private void validateAttribute(Map<String, String> attributes, String key, String expectedValue) {
        if (attributes.containsKey(key)) {
            if (attributes.get(key).equals(expectedValue)) {
                return;
            }
            fail(String.format("The key %s didn't have the expected value %s. Actual value : %s ", key, expectedValue,
                    attributes.get(key)));
        }
        fail(String.format("The key %s wasn't present in the Map.", key));
    }
}
