package software.amazon.awssdk.services.simpledb;

import static org.junit.Assert.fail;

import org.junit.Test;
import software.amazon.awssdk.services.simpledb.model.DeleteDomainRequest;
import software.amazon.awssdk.services.simpledb.model.MissingParameterException;

/**
 * Integration tests for the exceptional cases of the SimpleDB DeleteDomain operation.
 * 
 * @author fulghum@amazon.com
 */
public class DeleteDomainIntegrationTest extends IntegrationTestBase {

    /**
     * Tests that the DeleteDomain operation throws a MissingParameterException if the domain name
     * isn't specified.
     */
    @Test
    public void testDeleteDomainMissingParameterException() {
        DeleteDomainRequest request = new DeleteDomainRequest();

        try {
            sdb.deleteDomain(request);
            fail("Expected MissingParameterException, but wasn't thrown");
        } catch (MissingParameterException e) {
            assertValidException(e);
        }
    }

}
