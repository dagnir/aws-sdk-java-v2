package software.amazon.awssdk.services.simpledb;

import static org.junit.Assert.fail;

import org.junit.Test;
import software.amazon.awssdk.services.simpledb.model.InvalidQueryExpressionException;
import software.amazon.awssdk.services.simpledb.model.MissingParameterException;
import software.amazon.awssdk.services.simpledb.model.NoSuchDomainException;
import software.amazon.awssdk.services.simpledb.model.SelectRequest;

/**
 * Integration tests for the exceptional cases of the SimpleDB Select operation.
 * 
 * @author fulghum@amazon.com
 */
public class SelectIntegrationTest extends IntegrationTestBase {

    /**
     * Tests that the Select operation throws a MissingParameterException when DomainName isn't
     * specified.
     */
    @Test
    public void testSelectMissingParameterException() {
        SelectRequest request = new SelectRequest();
        try {
            sdb.select(request);
            fail("Excepted MissingParameterException, but wasn't thrown");
        } catch (MissingParameterException e) {
            assertValidException(e);
        }
    }

    /**
     * Tests that the Select operation throws a NoSuchDomainException when a non-existent domain
     * name is specified.
     */
    @Test
    public void testSelectNoSuchDomainException() {
        SelectRequest request = new SelectRequest();
        try {
            sdb.select(request.withSelectExpression("select * from foobarbazbarbashbar"));
            fail("Expected NoSuchDomainException, but wasn't thrown");
        } catch (NoSuchDomainException e) {
            assertValidException(e);
        }
    }

    /**
     * Tests that the Select operation throws an InvalidQueryExpressionException when an invalid
     * query is passed.
     */
    @Test
    public void testSelectInvalidQueryExpressionException() {
        SelectRequest request = new SelectRequest();
        try {
            sdb.select(request.withSelectExpression("foobarbazbar"));
            fail("Expected InvalidQueryExpressionException, but wasn't thrown");
        } catch (InvalidQueryExpressionException e) {
            assertValidException(e);
        }
    }

}
