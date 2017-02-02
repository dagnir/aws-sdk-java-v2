package software.amazon.awssdk.services.lambda;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.AfterClass;
import org.junit.BeforeClass;

import software.amazon.awssdk.services.identitymanagement.AmazonIdentityManagement;
import software.amazon.awssdk.services.identitymanagement.AmazonIdentityManagementClient;
import software.amazon.awssdk.services.identitymanagement.model.AttachRolePolicyRequest;
import software.amazon.awssdk.services.identitymanagement.model.CreatePolicyRequest;
import software.amazon.awssdk.services.identitymanagement.model.CreateRoleRequest;
import software.amazon.awssdk.services.identitymanagement.model.CreateRoleResult;
import software.amazon.awssdk.services.identitymanagement.model.DeletePolicyRequest;
import software.amazon.awssdk.services.identitymanagement.model.DeleteRoleRequest;
import software.amazon.awssdk.services.identitymanagement.model.DetachRolePolicyRequest;
import software.amazon.awssdk.services.kinesis.AmazonKinesis;
import software.amazon.awssdk.services.kinesis.AmazonKinesisClient;
import software.amazon.awssdk.services.kinesis.model.CreateStreamRequest;
import software.amazon.awssdk.services.kinesis.model.StreamDescription;
import software.amazon.awssdk.services.kinesis.model.StreamStatus;
import software.amazon.awssdk.test.AWSTestBase;

public class IntegrationTestBase extends AWSTestBase {

    protected static AWSLambdaClient lambda;
    protected static File cloudFuncZip;
    protected static String lambdaServiceRoleArn;
    private static String roleExecutionPolicyArn;

    private static final String HELLOWORLD_JS = "helloworld.js";

    private static AmazonIdentityManagement iam;

    private static final String LAMBDA_SERVICE_ROLE_NAME = "lambda-java-sdk-test-role-" + System.currentTimeMillis();

    private static final String LAMBDA_SERVICE_ROLE_POLICY_NAME = LAMBDA_SERVICE_ROLE_NAME + "-policy";

    private static final String LAMBDA_ROLE_EXECUTION_POLICY = "{" + "\"Version\": \"2012-10-17\","
            + "\"Statement\": [" + "{" + "\"Sid\": \"\"," + "\"Effect\": \"Allow\"," + "\"Action\": \"kinesis:*\","
            + "\"Resource\": \"*\"" + "}" + "]" + "}";

    private static final String LAMBDA_ASSUME_ROLE_POLICY = "{" + "\"Version\": \"2012-10-17\"," + "\"Statement\": ["
            + "{" + "\"Sid\": \"\"," + "\"Effect\": \"Allow\"," + "\"Principal\": {"
            + "\"Service\": \"lambda.amazonaws.com\"" + "}," + "\"Action\": \"sts:AssumeRole\"" + "}" + "]" + "}";

    private static AmazonKinesis kinesis;

    protected static String streamArn;

    private static final String KINESIS_STREAM_NAME = "lambda-java-sdk-test-kinesis-stream-"
            + System.currentTimeMillis();

    @BeforeClass
    public static void setup() throws IOException {
        setUpCredentials();
        lambda = new AWSLambdaClient(credentials);

        cloudFuncZip = setupFunctionZip(HELLOWORLD_JS);

        createLambdaServiceRole();
    }

    @AfterClass
    public static void tearDown() {
        iam.detachRolePolicy(new DetachRolePolicyRequest().withRoleName(LAMBDA_SERVICE_ROLE_NAME).withPolicyArn(
                roleExecutionPolicyArn));

        iam.deletePolicy(new DeletePolicyRequest().withPolicyArn(roleExecutionPolicyArn));

        iam.deleteRole(new DeleteRoleRequest().withRoleName(LAMBDA_SERVICE_ROLE_NAME));

        if (kinesis != null) {
            kinesis.deleteStream(KINESIS_STREAM_NAME);
        }
    }

    private static File setupFunctionZip(String jsFile) throws IOException {
        InputStream in = IntegrationTestBase.class.getResourceAsStream(jsFile);

        File zipFile = File.createTempFile("lambda-cloud-function", ".zip");
        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zipFile));

        out.putNextEntry(new ZipEntry(jsFile));

        byte[] b = new byte[1024];
        int count;

        while ((count = in.read(b)) != -1) {
            out.write(b, 0, count);
        }

        out.close();
        in.close();

        return zipFile;
    }

    private static void createLambdaServiceRole() {
        iam = new AmazonIdentityManagementClient(credentials);

        CreateRoleResult result = iam.createRole(new CreateRoleRequest().withRoleName(LAMBDA_SERVICE_ROLE_NAME)
                .withAssumeRolePolicyDocument(LAMBDA_ASSUME_ROLE_POLICY));

        lambdaServiceRoleArn = result.getRole().getArn();

        roleExecutionPolicyArn = iam
                .createPolicy(
                        new CreatePolicyRequest().withPolicyName(LAMBDA_SERVICE_ROLE_POLICY_NAME).withPolicyDocument(
                                LAMBDA_ROLE_EXECUTION_POLICY)).getPolicy().getArn();

        iam.attachRolePolicy(new AttachRolePolicyRequest().withRoleName(LAMBDA_SERVICE_ROLE_NAME).withPolicyArn(
                roleExecutionPolicyArn));
    }

    protected static void createKinesisStream() {
        kinesis = new AmazonKinesisClient(credentials);

        kinesis.createStream(new CreateStreamRequest().withStreamName(KINESIS_STREAM_NAME).withShardCount(1));

        StreamDescription description = kinesis.describeStream(KINESIS_STREAM_NAME).getStreamDescription();
        streamArn = description.getStreamARN();

        // Wait till stream is active (less than a minute)
        while (!StreamStatus.ACTIVE.toString().equals(description.getStreamStatus())) {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException ignored) {
            }

            description = kinesis.describeStream(KINESIS_STREAM_NAME).getStreamDescription();
        }
    }

    protected static byte[] read(InputStream stream) throws IOException {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];

        int len;
        while ((len = stream.read(buffer)) >= 0) {
            result.write(buffer, 0, len);
        }

        return result.toByteArray();
    }
}
