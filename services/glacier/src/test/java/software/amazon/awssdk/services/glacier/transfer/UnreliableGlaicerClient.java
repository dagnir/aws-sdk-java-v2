package software.amazon.awssdk.services.glacier.transfer;

import software.amazon.awssdk.AmazonClientException;
import software.amazon.awssdk.AmazonServiceException;
import software.amazon.awssdk.ClientConfiguration;
import software.amazon.awssdk.auth.AWSCredentials;
import software.amazon.awssdk.services.glacier.AmazonGlacierClient;
import software.amazon.awssdk.services.glacier.model.GetJobOutputRequest;
import software.amazon.awssdk.services.glacier.model.GetJobOutputResult;
import software.amazon.awssdk.test.util.UnreliableRandomInputStream;

/**
 * The customized AWS Glacier java client which has the ability to trigger IO exception and
 * bad checksum for the test.
 *
 */
public class UnreliableGlaicerClient extends AmazonGlacierClient {

    private static int count = 0;
    private static int M = 1024 * 1024;
    private boolean reliable = false;
    // If the recoverable is false, the client will never return correct result.
    boolean recoverable = true;

    public void setReliable(boolean reliable) {
        this.reliable = reliable;
    }

    public void setRecoverable(boolean recoverable) {
        this.recoverable = recoverable;
    }

    /**
     * Set the an unreliable AWS Glacier java client.
     */
    public UnreliableGlaicerClient(AWSCredentials awsCredentials) {
        super(awsCredentials, new ClientConfiguration());
    }

    /**
     * Set the an unreliable AWS Glacier java client, and denote it whether is recoverable or not.
     */
    public UnreliableGlaicerClient(AWSCredentials awsCredentials, boolean recoverable) {
        super(awsCredentials, new ClientConfiguration());
        this.recoverable = recoverable;
    }

    /**
     *  Override the getJobOutput method in AWS Glacier Java Client to have the
     *  ability to trigger IO exception and bad checksum.
     */
    @Override
    public GetJobOutputResult getJobOutput(GetJobOutputRequest getJobOutputRequest)
            throws AmazonServiceException, AmazonClientException {
        GetJobOutputResult result = super.getJobOutput(getJobOutputRequest);
        if (reliable == true) {
            return result;
        }

        if (recoverable == false) {
            System.out.println("trigger a bad checksum!");
            result.setChecksum("123");
            return result;
        }

        if (count % 4 == 0) {
            System.out.println("trigger a bad checksum!");
            result.setChecksum("123");

        }

        if (count % 4 == 1) {
            System.out.println("trigger an unreliable input stream!");
            result.setBody(new UnreliableRandomInputStream(M));
        }

        if (count % 4 == 2) {
            System.out.println("trigger an unreliable input stream!");
            result.setBody(new UnreliableRandomInputStream(M));
        }

        count++;
        // prevent count overflow
        if (count == 4) {
            count = 0;
        }

        return result;
    }
}
