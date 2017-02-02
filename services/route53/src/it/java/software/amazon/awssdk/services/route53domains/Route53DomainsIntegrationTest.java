package software.amazon.awssdk.services.route53domains;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.junit.BeforeClass;
import org.junit.Test;

import software.amazon.awssdk.services.route53domains.model.CheckDomainAvailabilityRequest;
import software.amazon.awssdk.services.route53domains.model.CheckDomainAvailabilityResult;
import software.amazon.awssdk.services.route53domains.model.ContactDetail;
import software.amazon.awssdk.services.route53domains.model.ContactType;
import software.amazon.awssdk.services.route53domains.model.CountryCode;
import software.amazon.awssdk.services.route53domains.model.DomainAvailability;
import software.amazon.awssdk.services.route53domains.model.GetDomainDetailRequest;
import software.amazon.awssdk.services.route53domains.model.GetDomainDetailResult;
import software.amazon.awssdk.services.route53domains.model.GetOperationDetailRequest;
import software.amazon.awssdk.services.route53domains.model.GetOperationDetailResult;
import software.amazon.awssdk.services.route53domains.model.ListDomainsResult;
import software.amazon.awssdk.services.route53domains.model.OperationStatus;
import software.amazon.awssdk.services.route53domains.model.RegisterDomainRequest;
import software.amazon.awssdk.services.route53domains.model.RegisterDomainResult;
import software.amazon.awssdk.services.route53domains.model.UnsupportedTLDException;
import software.amazon.awssdk.services.route53domains.model.UpdateDomainContactRequest;
import software.amazon.awssdk.services.route53domains.model.UpdateDomainContactResult;
import software.amazon.awssdk.test.AWSTestBase;

public class Route53DomainsIntegrationTest extends AWSTestBase {

    /** Reference to the route 53 domains client.*/
    private static AmazonRoute53Domains route53Domains = null;

    /** Domain name with an invalid tld used for testing.*/
    private static final String INVAILD_DOMAIN_NAME = System
            .currentTimeMillis() + "-javasdkdomain.invalidtld";

    /** Name of the domain used for testing.*/
    private static final String DOMAIN_NAME = System.currentTimeMillis()
            + "-javasdkdomain.com";

    /** admin contact details used for registering a domain.*/
    private static ContactDetail adminContact = null;

    /** Sleep time in milliseconds to check if the operation is completed.*/
    private static final long SLEEP_TIME_IN_MILLIS = 5000;

    /**
     * Registers a new domain that is used for testing purposes.
     */
    @BeforeClass
    public static void setUp() throws FileNotFoundException, IOException,
            InterruptedException {
        setUpCredentials();
        route53Domains = new AmazonRoute53DomainsClient(credentials);

        adminContact = new ContactDetail().withFirstName("testFirstName")
                .withLastName("testLastName").withEmail("testEmail@email.com");
        adminContact.withCountryCode(CountryCode.US)
                .withContactType(ContactType.PERSON)
                .withAddressLine1("testAddress").withCity("Seattle")
                .withState("WA").withZipCode("12345")
                .withPhoneNumber("+1.5712058202");

        RegisterDomainResult registerResult = route53Domains
                .registerDomain(new RegisterDomainRequest()
                        .withDomainName(DOMAIN_NAME)
                        .withAdminContact(adminContact)
                        .withTechContact(adminContact).withDurationInYears(1)
                        .withRegistrantContact(adminContact)
                        .withAutoRenew(Boolean.FALSE));
        waitUntilOperationCompletes(registerResult.getOperationId());
    }

    /**
     * This test checks to see if a domain name is available to use. The domain
     * name used in this test case should not be present and the API must return
     * AVAILABLE.
     */
    @Test
    public void testCheckDomainNameAvailability() {
        CheckDomainAvailabilityResult checkDomainAvailabilityResult = route53Domains
                .checkDomainAvailability(new CheckDomainAvailabilityRequest()
                        .withDomainName("domain-not-exists.com"));
        assertEquals(checkDomainAvailabilityResult.getAvailability(),
                DomainAvailability.AVAILABLE.toString());
    }

    /**
     * This test case performs the ListDomains API call. Checks to see if we
     * recieve a succesfull response.
     */
    @Test
    public void testListDomains() {
        ListDomainsResult listDomainsResult = route53Domains.listDomains();

        assertTrue(listDomainsResult.getDomains().size() >= 0);
    }

    /**
     * This test case tries to register a domain with an invalid TLD. An
     * UnSupportedTLDException must be thrown by the AWS Service.
     */
    @Test
    public void testRegsiterDomainWithInvalidDomainName() {

        try {
            route53Domains.registerDomain(new RegisterDomainRequest()
                    .withDomainName(INVAILD_DOMAIN_NAME)
                    .withAdminContact(adminContact)
                    .withTechContact(adminContact).withDurationInYears(1)
                    .withRegistrantContact(adminContact)
                    .withAutoRenew(Boolean.FALSE));
            fail("An error must be thrown as the tld in the domain name is invalid");
        } catch (Exception e) {
            assertTrue(e instanceof UnsupportedTLDException);
        }
    }

    /**
     * This test case tries to update some of the domain details. Asserts by
     * retrieving the domain details and checking if they are the same.
     *
     * @throws InterruptedException
     */
    @Test
    public void testUpdateDomain() throws InterruptedException {
        adminContact.setCity("Redmond");
        UpdateDomainContactResult updateResult = route53Domains
                .updateDomainContact(new UpdateDomainContactRequest()
                        .withDomainName(DOMAIN_NAME).withAdminContact(
                                adminContact));
        assertNotNull(updateResult.getOperationId());
        waitUntilOperationCompletes(updateResult.getOperationId());
        GetDomainDetailResult getResult = route53Domains
                .getDomainDetail(new GetDomainDetailRequest()
                        .withDomainName(DOMAIN_NAME));
        ContactDetail contact = getResult.getAdminContact();
        assertNotNull(contact);
        assertEquals(contact.getAddressLine1(), adminContact.getAddressLine1());
        assertEquals(contact.getCity(), adminContact.getCity());
        assertEquals(contact.getState(), adminContact.getState());
        assertEquals(contact.getCountryCode(), adminContact.getCountryCode());
        assertEquals(contact.getPhoneNumber(), adminContact.getPhoneNumber());

    }

    /**
     * Waits until the given operation is success or failed.
     */
    public static void waitUntilOperationCompletes(String operationId)
            throws InterruptedException {
        while (true) {
            GetOperationDetailResult getOperationDetails = route53Domains
                    .getOperationDetail(new GetOperationDetailRequest()
                            .withOperationId(operationId));

            String status = getOperationDetails.getStatus();

            if ((status.equals(OperationStatus.ERROR.toString()))
                    || (status.equals(OperationStatus.FAILED.toString()))) {
                fail("Test case cannot proceed as the operation " + operationId
                        + " on the domain "
                        + getOperationDetails.getDomainName() + " failed.");
            } else if (status.equals(OperationStatus.SUCCESSFUL.toString())) {
                return;
            }

            Thread.sleep(SLEEP_TIME_IN_MILLIS);
        }
    }
}
