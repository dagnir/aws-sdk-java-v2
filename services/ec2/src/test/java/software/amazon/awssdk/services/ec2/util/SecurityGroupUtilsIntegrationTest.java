package software.amazon.awssdk.services.ec2.util;

import java.util.UUID;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import software.amazon.awssdk.AmazonServiceException;
import software.amazon.awssdk.auth.BasicAWSCredentials;
import software.amazon.awssdk.services.ec2.AmazonEC2;
import software.amazon.awssdk.services.ec2.AmazonEC2Client;
import software.amazon.awssdk.services.ec2.model.CreateSecurityGroupRequest;
import software.amazon.awssdk.services.ec2.model.DeleteSecurityGroupRequest;

public class SecurityGroupUtilsIntegrationTest {

    private static AmazonEC2 ec2;
    private static String GROUP_NAME = UUID.randomUUID().toString();
    private static String NON_EXISTENT_GROUP_NAME = UUID.randomUUID().toString();

    @BeforeClass
    public static void setup() {
        ec2 = new AmazonEC2Client();

        ec2.createSecurityGroup(new CreateSecurityGroupRequest(
                GROUP_NAME, "my security group"));
    }

    @AfterClass
    public static void teardown() {
        ec2.deleteSecurityGroup(new DeleteSecurityGroupRequest(GROUP_NAME));
    }

    @Test
    public void testSecurityGroupExist() {
        Assert.assertTrue(SecurityGroupUtils.doesSecurityGroupExist(ec2, GROUP_NAME));
    }

    @Test
    public void testSecurityGroupNotExist() {
        Assert.assertFalse(SecurityGroupUtils.doesSecurityGroupExist(ec2, NON_EXISTENT_GROUP_NAME));
    }

    /**
     * Make sure the API does not swallow service-side exceptions unrelated to
     * the existence of the security group
     */
    @Test(expected = AmazonServiceException.class)
    public void testServiceSideException() {
        AmazonEC2Client clientWithWrongKeys = new AmazonEC2Client(new BasicAWSCredentials("key", "key"));
        SecurityGroupUtils.doesSecurityGroupExist(clientWithWrongKeys, GROUP_NAME);
    }
}
