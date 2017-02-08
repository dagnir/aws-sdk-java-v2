package software.amazon.awssdk.services.codecommit;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.UUID;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.services.codecommit.AWSCodeCommit;
import software.amazon.awssdk.services.codecommit.AWSCodeCommitClient;
import software.amazon.awssdk.services.codecommit.model.CreateRepositoryRequest;
import software.amazon.awssdk.services.codecommit.model.DeleteRepositoryRequest;
import software.amazon.awssdk.services.codecommit.model.GetRepositoryRequest;
import software.amazon.awssdk.services.codecommit.model.RepositoryDoesNotExistException;
import software.amazon.awssdk.services.codecommit.model.RepositoryMetadata;
import software.amazon.awssdk.test.AWSTestBase;

/**
 * Smoke test for {@link AWSCodeCommitClient}
 */
public class AWSCodeCommitServiceIntegrationTest extends AWSTestBase {

    private static AWSCodeCommit client;

    private static final String REPO_NAME = "java-sdk-test-repo-" + System.currentTimeMillis();

    @BeforeClass
    public static void setup() throws FileNotFoundException, IOException {
        setUpCredentials();
        client = new AWSCodeCommitClient(credentials);
    }

    @AfterClass
    public static void cleanup() {
        try {
            client.deleteRepository(new DeleteRepositoryRequest()
                    .withRepositoryName(REPO_NAME));
        } catch (Exception ignored) {
            System.err.println("Failed to delete repository " + ignored);
        }
    }

    @Test
    public void testOperations() {

        // CreateRepository
        client.createRepository(new CreateRepositoryRequest()
                .withRepositoryName(REPO_NAME).withRepositoryDescription(
                        "My test repo"));

        // GetRepository
        RepositoryMetadata repoMd = client.getRepository(
                new GetRepositoryRequest().withRepositoryName(REPO_NAME))
                .getRepositoryMetadata();
        Assert.assertEquals(REPO_NAME, repoMd.getRepositoryName());
        assertValid_RepositoryMetadata(repoMd);

        // Can't perform any branch-related operations since we need to create
        // the first branch by pushing a commit via git.

        // DeleteRepository
        client.deleteRepository(new DeleteRepositoryRequest()
                .withRepositoryName(REPO_NAME));

    }

    @Test(expected = RepositoryDoesNotExistException.class)
    public void testExceptionHandling() {
        String nonExistentRepoName = UUID.randomUUID().toString();
        client.getRepository(new GetRepositoryRequest()
                .withRepositoryName(nonExistentRepoName));
    }

    private void assertValid_RepositoryMetadata(RepositoryMetadata md) {
        Assert.assertNotNull(md.getAccountId());
        Assert.assertNotNull(md.getArn());
        Assert.assertNotNull(md.getCloneUrlHttp());
        Assert.assertNotNull(md.getCloneUrlSsh());
        Assert.assertNotNull(md.getRepositoryDescription());
        Assert.assertNotNull(md.getRepositoryId());
        Assert.assertNotNull(md.getRepositoryName());
        Assert.assertNotNull(md.getCreationDate());
        Assert.assertNotNull(md.getLastModifiedDate());
    }

}
