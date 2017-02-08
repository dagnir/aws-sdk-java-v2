package software.amazon.awssdk.services.dynamodbv2;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.security.SecureRandom;
import org.junit.Test;
import org.mockito.Mockito;
import software.amazon.awssdk.ClientConfiguration;
import software.amazon.awssdk.test.AWSIntegrationTestBase;

public class CustomSecureRandomIntegrationTest extends AWSIntegrationTestBase {

    private static class CustomSecureRandomImpl extends SecureRandom {

        private static final long serialVersionUID = -4970552916858838974L;
    }

    @Test
    public void customSecureRandomConfigured_UsesCustomImplementation() {
        CustomSecureRandomImpl customSecureRandom = spy(new CustomSecureRandomImpl());
        AmazonDynamoDB ddb = new AmazonDynamoDBClient(getCredentials(),
                new ClientConfiguration().withSecureRandom(customSecureRandom));
        ddb.listTables();
        verify(customSecureRandom, atLeastOnce()).nextBytes((byte[]) Mockito.any());
    }

}
