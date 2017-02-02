package software.amazon.awssdk.services.simpledb;

import static org.junit.Assert.fail;

import org.junit.Test;

import software.amazon.awssdk.services.simpledb.model.CreateDomainRequest;
import software.amazon.awssdk.services.simpledb.model.InvalidParameterValueException;
import software.amazon.awssdk.services.simpledb.model.MissingParameterException;

/**
 * Integration tests for the exceptional cases of the SimpleDB CreateDomain operation.
 * 
 * @author fulghum@amazon.com
 */
public class CreateDomainIntegrationTest extends IntegrationTestBase {

    /**
     * Tests that calling CreateDomain with an invalid domain name throws an
     * InvalidParameterValueException.
     */
    @Test
    public void testCreateDomainInvalidParameterValueException() {
        CreateDomainRequest request = new CreateDomainRequest();
        request.setDomainName("''''''''''````````^^**&&@@!!??;;::[[{{]]}}||\\``''''");
        try {
            sdb.createDomain(request);
            fail("Expected InvalidParameterValueException, but wasn't thrown");
        } catch (InvalidParameterValueException e) {
            assertValidException(e);
        }
    }

    /**
     * Tests that calling CreateDomain without specifying a domain name throws a
     * MissingParameterException.
     */
    @Test
    public void testCreateDomainMissingParameterException() {
        CreateDomainRequest request = new CreateDomainRequest();
        try {
            sdb.createDomain(request);
            fail("Expected MissingParameterException, but wasn't thrown");
        } catch (MissingParameterException e) {
            assertValidException(e);
        }
    }

}
