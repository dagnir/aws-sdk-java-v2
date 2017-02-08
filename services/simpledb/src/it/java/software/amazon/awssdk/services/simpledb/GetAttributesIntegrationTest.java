package software.amazon.awssdk.services.simpledb;

import static org.junit.Assert.fail;

import org.junit.Test;
import software.amazon.awssdk.services.simpledb.model.GetAttributesRequest;
import software.amazon.awssdk.services.simpledb.model.MissingParameterException;
import software.amazon.awssdk.services.simpledb.model.NoSuchDomainException;

/**
 * Integration tests for the exceptional cases of the SimpleDB GetAttributes operation.
 * 
 * @author fulghum@amazon.com
 */
public class GetAttributesIntegrationTest extends IntegrationTestBase {

    /**
     * Tests that the GetAttributes operation throws a NoSuchDomainException when an invalid domain
     * is specified.
     */
    @Test
    public void testGetAttributesNoSuchDomainException() {
        GetAttributesRequest request = new GetAttributesRequest();

        request.setItemName("foobar");
        request.setDomainName("foobarbazbarbashbar");

        try {
            sdb.getAttributes(request);
            fail("Expected NoSuchDomainException, but wasn't thrown");
        } catch (NoSuchDomainException e) {
            assertValidException(e);
        }
    }

    /**
     * Tests that the GetAttributes operation throws a MissingParameterException if either domain
     * name or item name aren't specified.
     */
    @Test
    public void testGetAttributesMissingParameterException() {
        GetAttributesRequest request = new GetAttributesRequest();
        try {
            sdb.getAttributes(request.withItemName("foobar"));
            fail("Expected MissingParameterException, but wasn't thrown");
        } catch (MissingParameterException e) {
            assertValidException(e);
        }

        request = new GetAttributesRequest();
        try {
            sdb.getAttributes(request.withDomainName("foobar"));
            fail("Expected MissingParameterException, but wasn't thrown");
        } catch (MissingParameterException e) {
            assertValidException(e);
        }
    }

}
