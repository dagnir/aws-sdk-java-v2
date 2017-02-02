package software.amazon.awssdk.services.devicefarm;

import static org.junit.Assert.assertNotNull;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import software.amazon.awssdk.AmazonServiceException;
import software.amazon.awssdk.services.devicefarm.model.CreateProjectRequest;
import software.amazon.awssdk.services.devicefarm.model.CreateProjectResult;
import software.amazon.awssdk.services.devicefarm.model.ListDevicePoolsRequest;
import software.amazon.awssdk.services.devicefarm.model.Project;
import software.amazon.awssdk.test.AWSTestBase;

/**
 * Smoke tests for device farm service.
 */
public class AWSDeviceFarmIntegrationTest extends AWSTestBase {

    private static AWSDeviceFarm client;

    private static final String PROJECT_NAME = "df-java-project-"
            + System.currentTimeMillis();

    @BeforeClass
    public static void setup() throws Exception {
        setUpCredentials();
        client = new AWSDeviceFarmClient(credentials);
    }

    @AfterClass
    public static void tearDown() {
        if (client != null) {
            client.shutdown();
        }
    }

    @Test
    public void testCreateProject() {
        CreateProjectResult result = client
                .createProject(new CreateProjectRequest()
                        .withName(PROJECT_NAME));
        final Project project = result.getProject();
        assertNotNull(project);
        assertNotNull(project.getArn());
    }

    @Test(expected = AmazonServiceException.class)
    public void testExceptionHandling() {
        client.listDevicePools(new ListDevicePoolsRequest()
                .withNextToken("fake-token"));
    }
}
