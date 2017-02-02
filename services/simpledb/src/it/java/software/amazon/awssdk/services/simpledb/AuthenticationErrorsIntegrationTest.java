package software.amazon.awssdk.services.simpledb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import software.amazon.awssdk.AmazonServiceException;
import software.amazon.awssdk.auth.BasicAWSCredentials;
import software.amazon.awssdk.services.simpledb.model.ListDomainsRequest;

/**
 * Tests for client authentication errors.
 * 
 * @author fulghum@amazon.com
 */
public class AuthenticationErrorsIntegrationTest extends IntegrationTestBase {

    /**
     * Tests that using an invalid access key and secret key throw an AmazonServiceException with
     * the InvalidClientTokenId error code.
     */
    @Test
    public void testInvalidClientTokenId() {
        AmazonSimpleDBClient client = new AmazonSimpleDBClient(new BasicAWSCredentials("accessKey", "secretKey"));

        try {
            client.listDomains(new ListDomainsRequest());
            fail("Expected exception not thrown");
        } catch (AmazonServiceException e) {
            assertEquals("InvalidClientTokenId", e.getErrorCode());
            assertTrue(e.getMessage().length() > 10);
            assertTrue(e.getRequestId().length() > 10);
        }
    }

    /**
     * Tests that using a valid access key with an invalid secret key throw an
     * AmazonServiceException with the SignatureDoesNotMatch error code.
     */
    @Test
    public void testSignatureDoesNotMatch() {
        String accessKey = credentials.getAWSAccessKeyId();
        AmazonSimpleDBClient client = new AmazonSimpleDBClient(new BasicAWSCredentials(accessKey, "secretKey"));

        try {
            client.listDomains(new ListDomainsRequest());
            fail("Expected exception not thrown");
        } catch (AmazonServiceException e) {
            assertEquals("SignatureDoesNotMatch", e.getErrorCode());
            assertTrue(e.getMessage().length() > 10);
            assertTrue(e.getRequestId().length() > 10);
        }
    }

}
