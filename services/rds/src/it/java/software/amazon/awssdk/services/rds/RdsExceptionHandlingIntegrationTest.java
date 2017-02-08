package software.amazon.awssdk.services.rds;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;
import software.amazon.awssdk.AmazonServiceException.ErrorType;
import software.amazon.awssdk.services.rds.model.DBInstanceNotFoundException;
import software.amazon.awssdk.services.rds.model.DescribeDBInstancesRequest;

/**
 * Integration test for the typed exception handling in RDS.
 * 
 * @author Jason Fulghum <fulghum@amazon.com>
 */
public class RdsExceptionHandlingIntegrationTest extends IntegrationTestBase {

    /**
     * Tests that a custom RDS exception is thrown as expected, and correctly
     * populated.
     */
    @Test
    public void testExceptionHandling() throws Exception {
        try {
            rds.describeDBInstances(new DescribeDBInstancesRequest().withDBInstanceIdentifier("non-existant-db-identifier"));
            fail("Expected exception not thrown");
        } catch (DBInstanceNotFoundException nfe) {
            assertNotEmpty(nfe.getErrorCode());
            assertEquals(ErrorType.Client, nfe.getErrorType());
            assertNotEmpty(nfe.getMessage());
            assertNotEmpty(nfe.getRequestId());
            assertNotEmpty(nfe.getServiceName());
            assertTrue(nfe.getStatusCode() >= 400);
        }
    }
}
