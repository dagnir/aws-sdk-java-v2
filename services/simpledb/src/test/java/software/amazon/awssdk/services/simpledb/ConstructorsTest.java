package software.amazon.awssdk.services.simpledb;

import org.junit.Test;

import software.amazon.awssdk.services.simpledb.model.BatchPutAttributesRequest;
import software.amazon.awssdk.services.simpledb.model.CreateDomainRequest;
import software.amazon.awssdk.services.simpledb.model.DeleteAttributesRequest;
import software.amazon.awssdk.services.simpledb.model.DeleteDomainRequest;
import software.amazon.awssdk.services.simpledb.model.DomainMetadataRequest;
import software.amazon.awssdk.services.simpledb.model.GetAttributesRequest;
import software.amazon.awssdk.services.simpledb.model.PutAttributesRequest;
import software.amazon.awssdk.services.simpledb.model.SelectRequest;

/**
 * Tests that constructors provided by the SimpleDB model classes are available. This test is
 * primarily intended to help us ensure we aren't making backwards incompatible changes. We aren't
 * testing any specific behavior in the constructors, just that the constructors we expect to exist
 * are available. Backwards incompatible changes would show up as compilation errors, but it's still
 * convenient to model this a test class.
 * <p>
 * A more automated solution for detecting backwards incompatible changes would be a lot better,
 * especially considering all the Coral models we don't own.
 */
public class ConstructorsTest {

    @Test
    public void testConstructors() {
        new BatchPutAttributesRequest(null, null);
        new CreateDomainRequest(null);
        new DeleteAttributesRequest(null, null);
        new DeleteDomainRequest(null);
        new DomainMetadataRequest(null);
        new GetAttributesRequest(null, null);
        new PutAttributesRequest(null, null, null);
        new SelectRequest(null);
    }
}