package software.amazon.awssdk.services.iam;

import static org.junit.Assert.*;

import org.junit.Test;

import software.amazon.awssdk.services.identitymanagement.model.DeleteServerCertificateRequest;
import software.amazon.awssdk.services.identitymanagement.model.GetServerCertificateRequest;
import software.amazon.awssdk.services.identitymanagement.model.GetServerCertificateResult;
import software.amazon.awssdk.services.identitymanagement.model.ListServerCertificatesRequest;
import software.amazon.awssdk.services.identitymanagement.model.ListServerCertificatesResult;
import software.amazon.awssdk.services.identitymanagement.model.ServerCertificateMetadata;
import software.amazon.awssdk.services.identitymanagement.model.UpdateServerCertificateRequest;
import software.amazon.awssdk.services.identitymanagement.model.UploadServerCertificateRequest;
import software.amazon.awssdk.services.identitymanagement.model.UploadServerCertificateResult;

/**
 * Integration tests of the server certificate APIs of IAM.
 * 
 * Adapted by jimfl@'s C# tests:
 * 
 * https://brazil-subversion.amazon.com/brazil/src/appgroup/awsdr/sdk/AWSCSharpSDKFactory/mainline/Beta.NET.SDK/AWSSDKTests/IntegrationTests/IAM/ServerCertTests.cs
 */
public class ServerCertIntegrationTest extends IntegrationTestBase {
	
	private static final int MILLISECONDS_IN_DAY = 1000 * 60 * 60 * 24;
	
	public static final String PUBLIC_SERVER_KEY = "-----BEGIN CERTIFICATE-----\n"
			+ "MIIDWTCCAsKgAwIBAgIJANgRVq/2z7jfMA0GCSqGSIb3DQEBBQUAMHwxCzAJBgNV\n"
			+ "BAYTAkdCMRIwEAYDVQQIEwlCZXJrc2hpcmUxEDAOBgNVBAcTB05ld2J1cnkxFzAV\n"
			+ "BgNVBAoTDk15IENvbXBhbnkgTHRkMQ0wCwYDVQQDEwRob21lMR8wHQYJKoZIhvcN\n"
			+ "AQkBFhBub3JtakBhbWF6b24uY29tMB4XDTEwMDkzMDIzMjYwOFoXDTEyMDkyOTIz\n"
			+ "MjYwOFowfDELMAkGA1UEBhMCR0IxEjAQBgNVBAgTCUJlcmtzaGlyZTEQMA4GA1UE\n"
			+ "BxMHTmV3YnVyeTEXMBUGA1UEChMOTXkgQ29tcGFueSBMdGQxDTALBgNVBAMTBGhv\n"
			+ "bWUxHzAdBgkqhkiG9w0BCQEWEG5vcm1qQGFtYXpvbi5jb20wgZ8wDQYJKoZIhvcN\n"
			+ "AQEBBQADgY0AMIGJAoGBAOOQH8UKlqWQEit6jWYPFs5w3UJcbJQsK7dXyl+2Pug+\n"
			+ "hSrqpeMXET0BJBHbIi6QQ/EVTIUWyBg9nizuzuPhVq+y58V1YPc5cmnTPWnncoY6\n"
			+ "kTRTU/Ur67Y3kYnNYWeQdXS37EAnBffgqQNP3PSIRdnHuQmpG3gQQl+bXQQEHCr9\n"
			+ "AgMBAAGjgeIwgd8wHQYDVR0OBBYEFMJCfM4jUOassmmWUx6vlmhUtwccMIGvBgNV\n"
			+ "HSMEgacwgaSAFMJCfM4jUOassmmWUx6vlmhUtwccoYGApH4wfDELMAkGA1UEBhMC\n"
			+ "R0IxEjAQBgNVBAgTCUJlcmtzaGlyZTEQMA4GA1UEBxMHTmV3YnVyeTEXMBUGA1UE\n"
			+ "ChMOTXkgQ29tcGFueSBMdGQxDTALBgNVBAMTBGhvbWUxHzAdBgkqhkiG9w0BCQEW\n"
			+ "EG5vcm1qQGFtYXpvbi5jb22CCQDYEVav9s+43zAMBgNVHRMEBTADAQH/MA0GCSqG\n"
			+ "SIb3DQEBBQUAA4GBAKFzoqAIEZyMFgLEU+lCCSITFcDhMYzcZcEykAjMO20ahYXi\n"
			+ "dNizI8fApMQppcTfJj4iYpaD0KKPRQpyLAttT/V9jmcWbpULWVY9T3ASJ92ZBxYf\n"
			+ "juf9VmRmTuL784RKKxOSnKKWj4M5an7xSf1tA5mV6okFDyyN0lCsmKL0c03E\n"
			+ "-----END CERTIFICATE-----\n";

	public static final String PRIVATE_SERVER_KEY = "-----BEGIN RSA PRIVATE KEY-----\n"
			+ "MIICXgIBAAKBgQDjkB/FCpalkBIreo1mDxbOcN1CXGyULCu3V8pftj7oPoUq6qXj\n"
			+ "FxE9ASQR2yIukEPxFUyFFsgYPZ4s7s7j4VavsufFdWD3OXJp0z1p53KGOpE0U1P1\n"
			+ "K+u2N5GJzWFnkHV0t+xAJwX34KkDT9z0iEXZx7kJqRt4EEJfm10EBBwq/QIDAQAB\n"
			+ "AoGBAJQkNwpnIjsV1z5GwdY27HnoL6IL2QN83di1ZiF42usGCFsv9l4nnilAnOKi\n"
			+ "7VWyxQgk/XOGqSxesKI/tJ/VCkCMRrQtrqap7CQQYDELtUEDadJ77w+ZemNKScDD\n"
			+ "CXlIZ1z1W739tYYgNTZ8HLA7w0uVrfZHoMZxH0WD5LPSLLlhAkEA/8NnI6Set64T\n"
			+ "DsH0byUQv/O6VgQvBvoogYlb60/C7DWn4WC2PgNiOf/ryawcOLci27nriuWxOtWR\n"
			+ "dLhN8wSQIwJBAOPGCjEToxmhsV69Bb5ZYiE3b0xdAGquFKrdcDHJ2kw1NRJ/IkNU\n"
			+ "pY26F8GZdyZl0eTJbTGDUCitCp2hKSL+el8CQG2MndXXgiA80G7mxrMAlk8Rr0N+\n"
			+ "oUIEzmrFkfiVfnE8fj779LNVMbKUGsOUE7Z7QtQIq4of3izMI3RyKPkpgC8CQQDE\n"
			+ "6wsWoMaKO1tP75VOmpIW64kieOkKUdP2YJlFwiAjcICgrB8gHMdAP1mYe6giHzcW\n"
			+ "V/o6Ky+a6vdZjeI1qdJ7AkEArbLU9LfQmAq33t1K1Pv9Yq78sqwLpwR7gDhwGPGd\n"
			+ "US3NZ8NTQUlWVi4QB7qoLNWDpqY9BaqFIyh83FIYqP/eYQ==\n"
			+ "-----END RSA PRIVATE KEY-----\n";

	@Test
	public void UploadServerCert() {
		
		String certName = this.getClass().getName() + System.currentTimeMillis();
		String updateCertName = null;

		UploadServerCertificateRequest uploadRequest = new UploadServerCertificateRequest()
				.withCertificateBody(PUBLIC_SERVER_KEY)
				.withPrivateKey(PRIVATE_SERVER_KEY).withPath("/mycerts/")
				.withServerCertificateName(certName);

		UploadServerCertificateResult uploadResult = iam
				.uploadServerCertificate(uploadRequest);
		try {
			assertNotNull(uploadResult);
			ServerCertificateMetadata metadata = uploadResult
					.getServerCertificateMetadata();
			assertEquals(certName, metadata.getServerCertificateName());
			assertEquals("/mycerts/", metadata.getPath());
			assertNotNull(metadata.getServerCertificateId());
			assertEquals(System.currentTimeMillis() / MILLISECONDS_IN_DAY, metadata.getUploadDate().getTime() / MILLISECONDS_IN_DAY);
			assertNotNull(metadata.getArn());

			GetServerCertificateRequest getRequest = new GetServerCertificateRequest()
					.withServerCertificateName(certName);
			GetServerCertificateResult getResult = iam
					.getServerCertificate(getRequest);
			assertNotNull(getResult);
			assertNotNull(getResult.getServerCertificate().getCertificateBody());

			metadata = getResult.getServerCertificate()
					.getServerCertificateMetadata();
			assertEquals(certName, metadata.getServerCertificateName());
			assertEquals("/mycerts/", metadata.getPath());
			assertNotNull(metadata.getServerCertificateId());
			assertEquals(System.currentTimeMillis() / MILLISECONDS_IN_DAY, metadata.getUploadDate().getTime() / MILLISECONDS_IN_DAY);
			assertNotNull(metadata.getArn());

			ListServerCertificatesResult listResult = iam
					.listServerCertificates(new ListServerCertificatesRequest());
			assertNotNull(listResult);
			assertTrue(listResult.getServerCertificateMetadataList().size() > 0);

			metadata = null;
			for (ServerCertificateMetadata m : listResult
					.getServerCertificateMetadataList()) {
				if (m.getServerCertificateName().equals(certName)) {
					metadata = m;
					break;
				}
			}

			assertNotNull(metadata);
			assertEquals(certName, metadata.getServerCertificateName());
			assertEquals("/mycerts/", metadata.getPath());
			assertNotNull(metadata.getServerCertificateId());
			assertEquals(System.currentTimeMillis() / MILLISECONDS_IN_DAY, metadata.getUploadDate().getTime() / MILLISECONDS_IN_DAY);
			assertNotNull(metadata.getArn());

			updateCertName = certName + "-updated";
			UpdateServerCertificateRequest updateRequest = new UpdateServerCertificateRequest()
					.withServerCertificateName(certName)
					.withNewPath("/mycerts/sub/")
					.withNewServerCertificateName(updateCertName);
			iam.updateServerCertificate(updateRequest);

			getRequest = new GetServerCertificateRequest()
					.withServerCertificateName(updateCertName);

			getResult = iam.getServerCertificate(getRequest);
			assertNotNull(getResult);
			assertNotNull(getResult.getServerCertificate().getCertificateBody());

			metadata = getResult.getServerCertificate()
					.getServerCertificateMetadata();
			assertEquals(updateCertName, metadata.getServerCertificateName());
			assertEquals("/mycerts/sub/", metadata.getPath());
			assertNotNull(metadata.getServerCertificateId());
			assertEquals(System.currentTimeMillis() / MILLISECONDS_IN_DAY, metadata.getUploadDate().getTime() / MILLISECONDS_IN_DAY);
			assertNotNull(metadata.getArn());

		} finally {
			if (updateCertName == null) {
				iam.deleteServerCertificate(new DeleteServerCertificateRequest()
						.withServerCertificateName(certName));
			}

			if (updateCertName != null) {
				iam.deleteServerCertificate(new DeleteServerCertificateRequest()
						.withServerCertificateName(updateCertName));
			}
		}
	}
}
