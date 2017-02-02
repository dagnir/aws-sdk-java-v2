package software.amazon.awssdk.services.simpledb;

import static org.junit.Assert.fail;

import java.util.ArrayList;

import org.junit.Test;

import software.amazon.awssdk.services.simpledb.model.MissingParameterException;
import software.amazon.awssdk.services.simpledb.model.NoSuchDomainException;
import software.amazon.awssdk.services.simpledb.model.PutAttributesRequest;
import software.amazon.awssdk.services.simpledb.model.ReplaceableAttribute;

/**
 * Integration tests for the exceptional cases of the SimpleDB PutAttributes operation.
 * 
 * @author fulghum@amazon.com
 */
public class PutAttributesIntegrationTest extends IntegrationTestBase {

    /**
     * Tests that PutAttributes throws a MissingParameterException when the request is missing
     * required parameters.
     */
    @Test
    public void testPutAttributesMissingParameterException() {
        PutAttributesRequest request = new PutAttributesRequest();
        try {
            sdb.putAttributes(request.withDomainName("foo"));
            fail("Expected MissingParameterException, but wasn't thrown");
        } catch (MissingParameterException e) {
            assertValidException(e);
        }

        request = new PutAttributesRequest();
        try {
            sdb.putAttributes(request.withItemName("foo"));
            fail("Expected MissingParameterException, but wasn't thrown");
        } catch (MissingParameterException e) {
            assertValidException(e);
        }
    }

    /**
     * Tests that the PutAttributes operations throws a NoSuchDomainException if a non-existent
     * domain name is specified.
     */
    @Test
    public void testPutAttributesNoSuchDomainException() {
        PutAttributesRequest request = new PutAttributesRequest();
        request.setItemName("foobarbazbarbashbar");
        request.setDomainName("foobarbazbarbashbar");
        ArrayList<ReplaceableAttribute> attributes = new ArrayList<ReplaceableAttribute>();

        ReplaceableAttribute attribute = new ReplaceableAttribute();
        attribute.setName("foo");
        attribute.setValue("bar");
        attributes.add(attribute);
        request.setAttributes(attributes);

        try {
            sdb.putAttributes(request);
            fail("Expected NoSuchDomainException, but wasn't thrown");
        } catch (NoSuchDomainException e) {
            assertValidException(e);
        }
    }

}
