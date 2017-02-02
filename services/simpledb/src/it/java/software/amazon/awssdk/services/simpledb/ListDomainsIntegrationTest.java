package software.amazon.awssdk.services.simpledb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.List;

import org.junit.Test;

import software.amazon.awssdk.services.simpledb.model.InvalidParameterValueException;
import software.amazon.awssdk.services.simpledb.model.ListDomainsRequest;
import software.amazon.awssdk.services.simpledb.model.ListDomainsResult;

/**
 * Integration tests for the SimpleDB ListDomains operation. This test currently requires at least
 * two domains to exist in the current users account, otherwise they won't be able to run correctly
 * and will exit early instead of failing. TODO: Eventually we should update this integration test
 * so that it creates any data that it needs.
 * 
 * @author fulghum@amazon.com
 */
public class ListDomainsIntegrationTest extends IntegrationTestBase {

    /**
     * Tests that the listDomains operation returns a list of domain names, correctly limited by the
     * max domains parameter.
     */
    @Test
    public void testListDomainsMaxDomains() throws Exception {
        ListDomainsRequest request = new ListDomainsRequest();
        request.setMaxNumberOfDomains(new Integer(1));

        ListDomainsResult listDomains = sdb.listDomains(request);
        assertNotNull(listDomains);

        if (listDomains.getDomainNames().size() < 2) {
            System.err.println("Unable to run " + this.getClass().getName()
                    + " integration test with the current AWS account"
                    + " because not enough domains are present to test list pagination");
            return;
        }

        List<String> domainNames = listDomains.getDomainNames();
        assertNotNull(domainNames);
        assertEquals(1, domainNames.size());
        assertNotNull(listDomains.getNextToken());
    }

    /**
     * Tests that the nextToken parameter works correctly with the listDomains operation by asking
     * for one domain name, then calling listDomains again with the nextToken from the previous
     * result and verifying that a different domain name is returned.
     */
    @Test
    public void testListDomainsNextToken() throws Exception {
        ListDomainsRequest request = new ListDomainsRequest();
        request.setMaxNumberOfDomains(new Integer(1));

        ListDomainsResult listDomainsResult = sdb.listDomains(request);

        if (listDomainsResult.getDomainNames().size() < 2) {
            System.err.println("Unable to run " + this.getClass().getName()
                    + " integration test with the current AWS account"
                    + " because not enough domains are present to test list pagination");
            return;
        }

        String firstDomainName = (String) listDomainsResult.getDomainNames().get(0);

        request.setNextToken(listDomainsResult.getNextToken());
        String secondDomainName = (String) sdb.listDomains(request).getDomainNames().get(0);

        assertFalse(firstDomainName.equals(secondDomainName));
    }

    /**
     * Tests that an InvalidParameterValueException is thrown when the client calls the listDomains
     * service method with an invalid parameter value.
     */
    @Test
    public void testListDomainsInvalidParameterValueException() throws Exception {
        ListDomainsRequest request = new ListDomainsRequest();
        request.setMaxNumberOfDomains(new Integer(-1));

        try {
            sdb.listDomains(request);
            fail("Expected InvalidParameterValueException, but wasn't thrown");
        } catch (InvalidParameterValueException e) {
            assertValidException(e);
        }
    }

    /*
     * TODO: Not sure how to trigger the InvalidNextTokenException. What makes a token invalid?
     */

}
