package software.amazon.awssdk.services.ec2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.util.List;
import java.util.Map;

import org.junit.Test;

import software.amazon.awssdk.Request;
import software.amazon.awssdk.services.ec2.model.GroupIdentifier;
import software.amazon.awssdk.services.ec2.model.LaunchSpecification;
import software.amazon.awssdk.services.ec2.model.RequestSpotInstancesRequest;
import software.amazon.awssdk.services.ec2.model.transform.EC2RequestHandler;
import software.amazon.awssdk.services.ec2.model.transform.RequestSpotInstancesRequestMarshaller;

/** Unit tests for the EC2RequestHandler */
public class EC2RequestHandlerTest {
    private final EC2RequestHandler handler = new EC2RequestHandler();

    /**
     * Tests that RequestSpotInstanceRequests are correctly handled and any
     * incorrect security group query params are correctly converted.
     */
    @Test
    public void testRequestSpotInstancesRequestHandling() throws Exception {
        RequestSpotInstancesRequest requestSpotInstancesRequest = new RequestSpotInstancesRequest();
        requestSpotInstancesRequest.setLaunchSpecification(new LaunchSpecification()
                .withAllSecurityGroups(newGroupIdentifier("groupId1", "groupName1"),
                                       newGroupIdentifier("groupId2", "groupName2")));

        Request<RequestSpotInstancesRequest> request = new RequestSpotInstancesRequestMarshaller().marshall(requestSpotInstancesRequest);
        handler.beforeRequest(request);

        Map<String,List<String>> requestParams = request.getParameters();
        List<String> securityGroup1 = requestParams.get("LaunchSpecification.SecurityGroupId.1");
        List<String> securityGroup2 = requestParams.get("LaunchSpecification.SecurityGroupId.2");

        assertNotNull(securityGroup1);
        assertNotNull(securityGroup2);
        assertEquals(1, securityGroup1.size());
        assertEquals(1, securityGroup2.size());
        assertEquals("groupId1", securityGroup1.iterator().next());
        assertEquals("groupId2", securityGroup2.iterator().next());

        // Assert that none of the invalid query params for security groups
        // in a LaunchSpecification are present in the marshalled request
        for (String key : request.getParameters().keySet()) {
            assertFalse(key.startsWith("LaunchSpecification.GroupSet."));
        }
    }

    private GroupIdentifier newGroupIdentifier(String id, String name) {
        return new GroupIdentifier().withGroupId(id).withGroupName(name);
    }
}
