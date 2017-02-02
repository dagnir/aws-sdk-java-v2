package software.amazon.awssdk.services.simpledb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import software.amazon.awssdk.services.simpledb.model.DomainMetadataRequest;
import software.amazon.awssdk.services.simpledb.model.MissingParameterException;
import software.amazon.awssdk.services.simpledb.model.NoSuchDomainException;

/**
 * Integration tests for the exceptional cases of the SimpleDB DomainMetadata operation.
 * 
 * @author fulghum@amazon.com
 */
public class DomainMetadataIntegrationTest extends IntegrationTestBase {

    /**
     * Tests that a NoSuchDomainException is thrown when the client calls the domainMetadata service
     * method with a non-existent domain name.
     */
    @Test
    public void testDomainMetadataInvalidParameterException() {
        final String imaginaryDomainName = "AnImaginaryDomainNameThatDoesntExist";

        DomainMetadataRequest request = new DomainMetadataRequest();
        request.setDomainName(imaginaryDomainName);

        try {
            sdb.domainMetadata(request);
            fail("Expected NoSuchDomainException, but wasn't thrown");
        } catch (NoSuchDomainException e) {
            assertValidException(e);

            assertNotNull(e.getBoxUsage());
            assertTrue(e.getBoxUsage().floatValue() > 0);

            assertEquals(400, e.getStatusCode());
        }
    }

    /**
     * Tests that a MissingParameterException is thrown when the client calls the domainMetadata
     * service method without specifying a domain name.
     */
    @Test
    public void testDomainMetadataMissingParameterException() {
        DomainMetadataRequest request = new DomainMetadataRequest();

        try {
            sdb.domainMetadata(request);
            fail("Expected NoSuchDomainException, but wasn't thrown");
        } catch (MissingParameterException e) {
            assertValidException(e);

            assertNotNull(e.getBoxUsage());
            assertTrue(e.getBoxUsage().floatValue() > 0);

            assertEquals(400, e.getStatusCode());
        }
    }

}
