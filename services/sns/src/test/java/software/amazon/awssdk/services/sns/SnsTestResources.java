package software.amazon.awssdk.services.sns;

/**
 * Constants for test resource locations
 */
public class SnsTestResources {

    public static final String PACKAGE_ROOT = "/software/amazon/awssdk/services/sns/";

    /**
     * A sample notification message from SNS
     */
    public static final String SAMPLE_MESSAGE = PACKAGE_ROOT + "sample-message.json";

    /**
     * Public cert used to verify message authenticity
     */
    public static final String PUBLIC_CERT = PACKAGE_ROOT + "sns-public-cert.pem";
}
