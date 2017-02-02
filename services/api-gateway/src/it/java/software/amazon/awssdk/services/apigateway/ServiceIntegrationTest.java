package software.amazon.awssdk.services.apigateway;

import java.util.List;

import junit.framework.Assert;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import software.amazon.awssdk.services.apigateway.model.CreateApiKeyRequest;
import software.amazon.awssdk.services.apigateway.model.CreateApiKeyResult;
import software.amazon.awssdk.services.apigateway.model.CreateResourceRequest;
import software.amazon.awssdk.services.apigateway.model.CreateResourceResult;
import software.amazon.awssdk.services.apigateway.model.CreateRestApiRequest;
import software.amazon.awssdk.services.apigateway.model.CreateRestApiResult;
import software.amazon.awssdk.services.apigateway.model.DeleteRestApiRequest;
import software.amazon.awssdk.services.apigateway.model.GetApiKeyRequest;
import software.amazon.awssdk.services.apigateway.model.GetApiKeyResult;
import software.amazon.awssdk.services.apigateway.model.GetMethodRequest;
import software.amazon.awssdk.services.apigateway.model.GetMethodResult;
import software.amazon.awssdk.services.apigateway.model.GetResourceRequest;
import software.amazon.awssdk.services.apigateway.model.GetResourceResult;
import software.amazon.awssdk.services.apigateway.model.GetResourcesRequest;
import software.amazon.awssdk.services.apigateway.model.GetResourcesResult;
import software.amazon.awssdk.services.apigateway.model.GetRestApiRequest;
import software.amazon.awssdk.services.apigateway.model.GetRestApiResult;
import software.amazon.awssdk.services.apigateway.model.IntegrationType;
import software.amazon.awssdk.services.apigateway.model.PatchOperation;
import software.amazon.awssdk.services.apigateway.model.PutIntegrationRequest;
import software.amazon.awssdk.services.apigateway.model.PutIntegrationResult;
import software.amazon.awssdk.services.apigateway.model.PutMethodRequest;
import software.amazon.awssdk.services.apigateway.model.PutMethodResult;
import software.amazon.awssdk.services.apigateway.model.Resource;
import software.amazon.awssdk.services.apigateway.model.UpdateApiKeyRequest;
import software.amazon.awssdk.services.apigateway.model.UpdateResourceRequest;
import software.amazon.awssdk.services.apigateway.model.UpdateRestApiRequest;
import software.amazon.awssdk.services.apigateway.model.Op;

public class ServiceIntegrationTest extends IntegrationTestBase {

    private static final String NAME = "java-sdk-integration-"
            + System.currentTimeMillis();
    private static final String DESCRIPTION = "fooDesc";

    private static String restApiId = null;

    @BeforeClass
    public static void createRestApi() {
        CreateRestApiResult createRestApiResult = apiGateway.createRestApi(
                new CreateRestApiRequest().withName(NAME)
                .withDescription(DESCRIPTION));

        Assert.assertNotNull(createRestApiResult);
        Assert.assertNotNull(createRestApiResult.getDescription());
        Assert.assertNotNull(createRestApiResult.getId());
        Assert.assertNotNull(createRestApiResult.getName());
        Assert.assertNotNull(createRestApiResult.getCreatedDate());
        Assert.assertEquals(createRestApiResult.getName(), NAME);
        Assert.assertEquals(createRestApiResult.getDescription(), DESCRIPTION);

        restApiId = createRestApiResult.getId();
    }

    @Test
    public void testUpdateRetrieveRestApi() {
        PatchOperation patch = new PatchOperation().withOp(Op.Replace)
                .withPath("/description").withValue("updatedDesc");
        apiGateway.updateRestApi(new UpdateRestApiRequest().withRestApiId(restApiId)
                .withPatchOperations(patch));

        GetRestApiResult getRestApiResult = apiGateway
                .getRestApi(new GetRestApiRequest().withRestApiId(restApiId));

        Assert.assertNotNull(getRestApiResult);
        Assert.assertNotNull(getRestApiResult.getDescription());
        Assert.assertNotNull(getRestApiResult.getId());
        Assert.assertNotNull(getRestApiResult.getName());
        Assert.assertNotNull(getRestApiResult.getCreatedDate());
        Assert.assertEquals(getRestApiResult.getName(), NAME);
        Assert.assertEquals(getRestApiResult.getDescription(), "updatedDesc");
    }

    @Test
    public void testCreateUpdateRetrieveApiKey() {
        CreateApiKeyResult createApiKeyResult = apiGateway
                .createApiKey(new CreateApiKeyRequest().withName(NAME)
                        .withDescription(DESCRIPTION));

        Assert.assertNotNull(createApiKeyResult);
        Assert.assertNotNull(createApiKeyResult.getDescription());
        Assert.assertNotNull(createApiKeyResult.getId());
        Assert.assertNotNull(createApiKeyResult.getName());
        Assert.assertNotNull(createApiKeyResult.getCreatedDate());
        Assert.assertNotNull(createApiKeyResult.getEnabled());
        Assert.assertNotNull(createApiKeyResult.getLastUpdatedDate());
        Assert.assertNotNull(createApiKeyResult.getStageKeys());

        String apiKeyId = createApiKeyResult.getId();
        Assert.assertEquals(createApiKeyResult.getName(), NAME);
        Assert.assertEquals(createApiKeyResult.getDescription(), DESCRIPTION);

        PatchOperation patch = new PatchOperation().withOp(Op.Replace)
                .withPath("/description").withValue("updatedDesc");
        apiGateway.updateApiKey(new UpdateApiKeyRequest().withApiKey(apiKeyId)
                .withPatchOperations(patch));

        GetApiKeyResult getApiKeyResult = apiGateway
                .getApiKey(new GetApiKeyRequest().withApiKey(apiKeyId));

        Assert.assertNotNull(getApiKeyResult);
        Assert.assertNotNull(getApiKeyResult.getDescription());
        Assert.assertNotNull(getApiKeyResult.getId());
        Assert.assertNotNull(getApiKeyResult.getName());
        Assert.assertNotNull(getApiKeyResult.getCreatedDate());
        Assert.assertNotNull(getApiKeyResult.getEnabled());
        Assert.assertNotNull(getApiKeyResult.getLastUpdatedDate());
        Assert.assertNotNull(getApiKeyResult.getStageKeys());
        Assert.assertEquals(getApiKeyResult.getId(), apiKeyId);
        Assert.assertEquals(getApiKeyResult.getName(), NAME);
        Assert.assertEquals(getApiKeyResult.getDescription(), "updatedDesc");
    }

    @Test
    public void testResourceOperations() {
        GetResourcesResult resourcesResult = apiGateway
                .getResources(new GetResourcesRequest()
                        .withRestApiId(restApiId));
        List<Resource> resources = resourcesResult.getItems();
        Assert.assertEquals(resources.size(), 1);
        Resource rootResource = resources.get(0);
        Assert.assertNotNull(rootResource);
        Assert.assertEquals(rootResource.getPath(), "/");
        String rootResourceId = rootResource.getId();

        CreateResourceResult createResourceResult = apiGateway
                .createResource(new CreateResourceRequest()
                        .withRestApiId(restApiId)
                        .withPathPart("fooPath")
                        .withParentId(rootResourceId));
        Assert.assertNotNull(createResourceResult);
        Assert.assertNotNull(createResourceResult.getId());
        Assert.assertNotNull(createResourceResult.getParentId());
        Assert.assertNotNull(createResourceResult.getPath());
        Assert.assertNotNull(createResourceResult.getPathPart());
        Assert.assertEquals(createResourceResult.getPathPart(), "fooPath");
        Assert.assertEquals(createResourceResult.getParentId(), rootResourceId);

        PatchOperation patch = new PatchOperation().withOp(Op.Replace)
                .withPath("/pathPart").withValue("updatedPath");
        apiGateway.updateResource(new UpdateResourceRequest()
                .withRestApiId(restApiId)
                .withResourceId(createResourceResult.getId())
                .withPatchOperations(patch));

        GetResourceResult getResourceResult = apiGateway
                .getResource(new GetResourceRequest()
                        .withRestApiId(restApiId)
                        .withResourceId(createResourceResult.getId()));
        Assert.assertNotNull(getResourceResult);
        Assert.assertNotNull(getResourceResult.getId());
        Assert.assertNotNull(getResourceResult.getParentId());
        Assert.assertNotNull(getResourceResult.getPath());
        Assert.assertNotNull(getResourceResult.getPathPart());
        Assert.assertEquals(getResourceResult.getPathPart(), "updatedPath");
        Assert.assertEquals(getResourceResult.getParentId(), rootResourceId);

        PutMethodResult putMethodResult = apiGateway
                .putMethod(new PutMethodRequest().withRestApiId(restApiId)
                        .withResourceId(createResourceResult.getId())
                        .withAuthorizationType("AWS_IAM").withHttpMethod("PUT"));
        Assert.assertNotNull(putMethodResult);
        Assert.assertNotNull(putMethodResult.getAuthorizationType());
        Assert.assertNotNull(putMethodResult.getApiKeyRequired());
        Assert.assertNotNull(putMethodResult.getHttpMethod());
        Assert.assertEquals(putMethodResult.getAuthorizationType(), "AWS_IAM");
        Assert.assertEquals(putMethodResult.getHttpMethod(), "PUT");

        PutIntegrationResult putIntegrationResult = apiGateway
                .putIntegration(new PutIntegrationRequest()
                        .withRestApiId(restApiId)
                        .withResourceId(createResourceResult.getId())
                        .withHttpMethod("PUT").withType(IntegrationType.MOCK)
                        .withUri("http://foo.bar")
                        .withIntegrationHttpMethod("GET"));
        Assert.assertNotNull(putIntegrationResult);
        Assert.assertNotNull(putIntegrationResult.getCacheNamespace());
        Assert.assertNotNull(putIntegrationResult.getType());
        Assert.assertEquals(putIntegrationResult.getType(),
                IntegrationType.MOCK.toString());
    }

    @AfterClass
    public static void deleteRestApiKey() {
        if (restApiId != null) {
            apiGateway.deleteRestApi(new DeleteRestApiRequest().withRestApiId(restApiId));
        }
    }
}
