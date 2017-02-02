package software.amazon.awssdk.services.email;

import static org.junit.Assert.fail;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.junit.BeforeClass;

import software.amazon.awssdk.services.simpleemail.AmazonSimpleEmailService;
import software.amazon.awssdk.services.simpleemail.AmazonSimpleEmailServiceClient;
import software.amazon.awssdk.services.simpleemail.model.ListVerifiedEmailAddressesResult;
import software.amazon.awssdk.services.simpleemail.model.VerifyEmailAddressRequest;
import software.amazon.awssdk.test.AWSTestBase;

/**
 * Base class for AWS Email integration tests; responsible for loading AWS account credentials for
 * running the tests, instantiating clients, etc.
 */
public abstract class IntegrationTestBase extends AWSTestBase {

    public static final String HUDSON_EMAIL_LIST = "aws-dr-tools-scripts@amazon.com";

    public static String DESTINATION;
    public static String SOURCE;
    protected static final String RAW_MESSAGE_FILE_PATH = "/com/amazonaws/services/email/rawMimeMessage.txt";

    protected static AmazonSimpleEmailService email;

    /**
     * Loads the AWS account info for the integration tests and creates client objects for tests to
     * use.
     */
    @BeforeClass
    public static void setUp() throws FileNotFoundException, IOException {
        setUpCredentials();

        if (DESTINATION == null) {
            DESTINATION = System.getProperty("user.name").equals("webuser") ? HUDSON_EMAIL_LIST : System
                    .getProperty("user.name") + "@amazon.com";
            SOURCE = DESTINATION;
        }

        email = new AmazonSimpleEmailServiceClient(credentials);
    }

    protected static void sendVerificationEmail() {
        email.setEndpoint("https://email.us-east-1.amazonaws.com");
        ListVerifiedEmailAddressesResult verifiedEmails = email.listVerifiedEmailAddresses();
        for (String email : verifiedEmails.getVerifiedEmailAddresses()) {
            if (email.equals(DESTINATION)) {
                return;
            }
        }

        email.verifyEmailAddress(new VerifyEmailAddressRequest().withEmailAddress(DESTINATION));
        fail("Please check your email and verify your email address.");
    }

    protected String loadRawMessage(String messagePath) throws Exception {
        String rawMessage = IOUtils.toString(getClass().getResourceAsStream(messagePath));
        rawMessage = rawMessage.replace("@DESTINATION@", DESTINATION);
        rawMessage = rawMessage.replace("@SOURCE@", SOURCE);
        return rawMessage;
    }

    protected InputStream loadRawMessageAsStream(String messagePath) throws Exception {
        return IOUtils.toInputStream(loadRawMessage(messagePath));
    }
}
