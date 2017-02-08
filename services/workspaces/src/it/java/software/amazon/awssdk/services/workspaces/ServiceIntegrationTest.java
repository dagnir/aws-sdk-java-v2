package software.amazon.awssdk.services.workspaces;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import software.amazon.awssdk.services.workspaces.model.CreateWorkspacesRequest;
import software.amazon.awssdk.services.workspaces.model.CreateWorkspacesResult;
import software.amazon.awssdk.services.workspaces.model.DescribeWorkspaceBundlesResult;
import software.amazon.awssdk.services.workspaces.model.DescribeWorkspaceDirectoriesResult;
import software.amazon.awssdk.services.workspaces.model.DescribeWorkspacesResult;
import software.amazon.awssdk.services.workspaces.model.WorkspaceRequest;

public class ServiceIntegrationTest extends IntegrationTestBase {

    @Test
    public void describeWorkspaces() {
        DescribeWorkspacesResult result = client.describeWorkspaces();
        assertTrue(result.getWorkspaces().isEmpty());
    }

    @Test
    public void describeWorkspaceBundles() {
        DescribeWorkspaceBundlesResult result = client.describeWorkspaceBundles();
        assertTrue(result.getBundles().isEmpty());
    }

    @Test
    public void describeWorkspaceDirectories() {
        DescribeWorkspaceDirectoriesResult result = client.describeWorkspaceDirectories();
        assertTrue(result.getDirectories().isEmpty());
    }

    @Test
    public void createWorkspaces() {
        CreateWorkspacesResult result = client.createWorkspaces(new CreateWorkspacesRequest()
                .withWorkspaces(new WorkspaceRequest().withUserName("hchar").withBundleId("wsb-12345678")
                        .withDirectoryId("d-12345678")));
        assertTrue(result.getFailedRequests().size() == 1);
    }
}
