package software.amazon.awssdk.services.sts;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import software.amazon.awssdk.SDKGlobalConfiguration;
import software.amazon.awssdk.services.securitytoken.AWSSecurityTokenServiceClient;
import software.amazon.awssdk.services.securitytoken.model.GetFederationTokenRequest;
import software.amazon.awssdk.services.securitytoken.model.GetFederationTokenResult;
import software.amazon.awssdk.services.securitytoken.model.GetSessionTokenRequest;
import software.amazon.awssdk.services.securitytoken.model.GetSessionTokenResult;
// import software.amazon.awssdk.services.ec2.AmazonEC2Client;
// import software.amazon.awssdk.services.ec2.model.StartInstancesRequest;
// import software.amazon.awssdk.services.s3.AmazonS3Client;
// import software.amazon.awssdk.services.s3.model.Bucket;

public class SecurityTokenServiceIntegrationTest extends IntegrationTestBase {

    private static final int SESSION_DURATION = 60 * 60;
    private static final String ROLE_ARN = "arn:aws:iam::599169622985:role/java-test-role";
    private static final String USER_NAME  = "user-" + System.currentTimeMillis();

    /** Tests that we can call GetSession to start a session. */
    @Test
    public void testGetSessionToken() throws Exception {
        GetSessionTokenRequest request = new GetSessionTokenRequest().withDurationSeconds(SESSION_DURATION);
        GetSessionTokenResult result = sts.getSessionToken(request);

        assertNotNull(result.getCredentials().getAccessKeyId());
        assertNotNull(result.getCredentials().getExpiration());
        assertNotNull(result.getCredentials().getSecretAccessKey());
        assertNotNull(result.getCredentials().getSessionToken());
    }

    // To run this test, you need to provide the IAM access key and secret key.
    // For security reason, you do not put these credentials in the test, but
    // still a good test to have.
    //
    // This test requires us to add a test dependency on S3 and EC2, which
    // have more useful tests which require them to have a dependency on STS.
    // We can't have circular dependencies, so commenting this out until we
    // can refactor the integration tests out of the client packages to avoid
    // such circular dependencies.
    //
    // @Test
    // public void testDecodeAuthorizationMessage() {
    //
    //     final String iamUserAccessKey = "IAM-USER-WITH-NO-EC2-PERMISSIONS-ACCESS-KEY";
    //     final String iamUserSecretKey = "IAM-USER-WITH-NO-EC2-PERMISSIONS-SECRET-KEY";
    //     BasicAWSCredentials iamUserCredentials = new BasicAWSCredentials(
    //            iamUserAccessKey, iamUserSecretKey);
    //     final String encodedAuthorizationMessageText = "Encoded authorization failure message: ";
    //
    //     AmazonS3Client s3client = new AmazonS3Client(iamUserCredentials);
    //     List<Bucket> buckets = s3client.listBuckets();
    //     assertNotNull(buckets);
    //     assertTrue(buckets.size() > 0);
    //
    //     try {
    //         AmazonEC2Client ec2client = new AmazonEC2Client(iamUserCredentials);
    //         ec2client.startInstances(new StartInstancesRequest()
    //                 .withInstanceIds("i-de5d42a7"));
    //
    //     } catch (AmazonServiceException e) {
    //         System.out.println(e.toString());
    //         int encodedMessageIndex = e.getMessage().indexOf(
    //                 encodedAuthorizationMessageText);
    //         if (encodedMessageIndex >= 0) {
    //             String encodedMessage = e.getMessage().substring(
    //                     encodedMessageIndex
    //                             + encodedAuthorizationMessageText.length());
    //
    //             AWSSecurityTokenServiceClient stsClient = new AWSSecurityTokenServiceClient(
    //                     iamUserCredentials);
    //             String decodedMessage = stsClient.decodeAuthorizationMessage(
    //
    //                     new DecodeAuthorizationMessageRequest()
    //                             .withEncodedMessage(encodedMessage))
    //                     .getDecodedMessage();
    //
    //             assertNotNull(decodedMessage);
    //             assertTrue(decodedMessage.length() > 0);
    //             System.out.println(decodedMessage);
    //
    //         }
    //
    //    }
    //
    // }

    /** Tests that we can call GetFederatedSession to start a federated session. */
    @Test
    public void testGetFederatedSessionToken() throws Exception {
        GetFederationTokenRequest request = new GetFederationTokenRequest()
                .withDurationSeconds(SESSION_DURATION)
                .withName("Name");
        GetFederationTokenResult result = sts.getFederationToken(request);

        assertNotNull(result.getCredentials().getAccessKeyId());
        assertNotNull(result.getCredentials().getExpiration());
        assertNotNull(result.getCredentials().getSecretAccessKey());
        assertNotNull(result.getCredentials().getSessionToken());

        assertNotNull(result.getFederatedUser().getArn());
        assertNotNull(result.getFederatedUser().getFederatedUserId());


    }

    /**
     * In the following test, we purposely setting the time offset to trigger a clock skew error.
     * The time offset must be fixed and then we validate the global value for time offset has been
     * update.
     */
    @Test
    public void testClockSkew() {
        SDKGlobalConfiguration.setGlobalTimeOffset(3600);
        assertTrue(SDKGlobalConfiguration.getGlobalTimeOffset() == 3600);
        sts.shutdown();
        sts = new AWSSecurityTokenServiceClient(credentials);
        sts.getSessionToken();
        assertTrue("Clockskew is fixed!", SDKGlobalConfiguration.getGlobalTimeOffset() < 3600);
        // subsequent changes to the global time offset won't affect existing client
        SDKGlobalConfiguration.setGlobalTimeOffset(3600);
        sts.getSessionToken();
        assertTrue(SDKGlobalConfiguration.getGlobalTimeOffset() == 3600);
    }
 }
