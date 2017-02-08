package software.amazon.awssdk.services.swf;

import java.util.UUID;
import org.junit.Test;
import software.amazon.awssdk.services.simpleworkflow.model.DescribeDomainRequest;
import software.amazon.awssdk.services.simpleworkflow.model.RegisterDomainRequest;
import software.amazon.awssdk.services.simpleworkflow.model.UnknownResourceException;

/**
 * Integration tests for SWF
 */
public class SWFIntegrationTest extends IntegrationTestBase {

    /**
     * Simple smoke test to demonstrate a working client
     */
    @Test
    public void testCallReturningVoid() throws Exception {
        swf.registerDomain(new RegisterDomainRequest().withName(UUID.randomUUID().toString()).withDescription("blah")
                .withWorkflowExecutionRetentionPeriodInDays("1"));
        System.out.println("Domain registered successfully!");
    }

    @Test(expected = UnknownResourceException.class)
    public void testCallThrowingFault() throws Exception {
        swf.describeDomain(new DescribeDomainRequest().withName(UUID.randomUUID().toString()));
    }

}
