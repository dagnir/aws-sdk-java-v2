package software.amazon.awssdk.services.iam;

import org.junit.Assert;
import org.junit.Test;
import software.amazon.awssdk.AmazonServiceException;
import software.amazon.awssdk.services.identitymanagement.model.CreateSAMLProviderRequest;
import software.amazon.awssdk.services.identitymanagement.model.DeleteSAMLProviderRequest;
import software.amazon.awssdk.services.identitymanagement.model.GetSAMLProviderRequest;
import software.amazon.awssdk.services.identitymanagement.model.ListSAMLProvidersResult;

/** Integration tests of the SAML provider APIs */
public class SAMLProviderIntegrationTest extends IntegrationTestBase {

    private static final String fakeProviderARN = "arn:aws:sts::599169622985:saml-provider/ImaginaryProvider";
    private static String fakeIdPMetadataDocumentContent;

    static {
        int DOCUMENT_LENGTH = 1200; // The api requires a metadata document with
        // minimum length of 1000 characters.
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < DOCUMENT_LENGTH; i++) {
            sb.append("a");
        }
        fakeIdPMetadataDocumentContent = sb.toString();
    }

    @Test
    public void testOperations() {
        // Currently there is no easy way to test creating a new SAML
        // provider, as it requires we setup an IdP and generate a valid
        // metadata xml file which could be used by IAM. Here we just submit
        // an invalid metadata file, and expect the service to return 
        // InvalidInput error
        try {
            iam.createSAMLProvider(new CreateSAMLProviderRequest().withName(
                    "fake-idp").withSAMLMetadataDocument(
                    fakeIdPMetadataDocumentContent));
            Assert.fail("Service exception is expected.");
        } catch (AmazonServiceException ase) {
            Assert.assertEquals("InvalidInput", ase.getErrorCode());
            Assert.assertTrue(ase.getMessage().contains("Could not parse metadata"));
        }

        // No SAML providers
        ListSAMLProvidersResult listSAMLProvidersResult = iam.listSAMLProviders();
        Assert.assertEquals(0, listSAMLProvidersResult.getSAMLProviderList().size());

        // Get/delete the imaginary SAML provider
        try {
            iam.getSAMLProvider(new GetSAMLProviderRequest().withSAMLProviderArn(fakeProviderARN));
        } catch (AmazonServiceException ase) {
            Assert.assertEquals("NoSuchEntity", ase.getErrorCode());
        }

        try {
            iam.deleteSAMLProvider(new DeleteSAMLProviderRequest().withSAMLProviderArn(fakeProviderARN));
        } catch (AmazonServiceException ase) {
            Assert.assertEquals("NoSuchEntity", ase.getErrorCode());
        }

    }
}
