/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

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
import software.amazon.awssdk.services.apigateway.model.GetResourceRequest;
import software.amazon.awssdk.services.apigateway.model.GetResourceResult;
import software.amazon.awssdk.services.apigateway.model.GetResourcesRequest;
import software.amazon.awssdk.services.apigateway.model.GetResourcesResult;
import software.amazon.awssdk.services.apigateway.model.GetRestApiRequest;
import software.amazon.awssdk.services.apigateway.model.GetRestApiResult;
import software.amazon.awssdk.services.apigateway.model.IntegrationType;
import software.amazon.awssdk.services.apigateway.model.Op;
import software.amazon.awssdk.services.apigateway.model.PatchOperation;
import software.amazon.awssdk.services.apigateway.model.PutIntegrationRequest;
import software.amazon.awssdk.services.apigateway.model.PutIntegrationResult;
import software.amazon.awssdk.services.apigateway.model.PutMethodRequest;
import software.amazon.awssdk.services.apigateway.model.PutMethodResult;
import software.amazon.awssdk.services.apigateway.model.Resource;
import software.amazon.awssdk.services.apigateway.model.UpdateApiKeyRequest;
import software.amazon.awssdk.services.apigateway.model.UpdateResourceRequest;
import software.amazon.awssdk.services.apigateway.model.UpdateRestApiRequest;

public class ServiceIntegrationTest extends IntegrationTestBase {

    private static final String NAME = "java-sdk-integration-"
                                       + System.currentTimeMillis();
    private static final String DESCRIPTION = "fooDesc";

    private static String restApiId = null;

    @BeforeClass
    public static void createRestApi() {
        CreateRestApiResult createRestApiResult = apiGateway.createRestApi(
                CreateRestApiRequest.builder_().name(NAME)
                                          .description(DESCRIPTION).build_());

        Assert.assertNotNull(createRestApiResult);
        Assert.assertNotNull(createRestApiResult.description());
        Assert.assertNotNull(createRestApiResult.id());
        Assert.assertNotNull(createRestApiResult.name());
        Assert.assertNotNull(createRestApiResult.createdDate());
        Assert.assertEquals(createRestApiResult.name(), NAME);
        Assert.assertEquals(createRestApiResult.description(), DESCRIPTION);

        restApiId = createRestApiResult.id();
    }

    @AfterClass
    public static void deleteRestApiKey() {
        if (restApiId != null) {
            apiGateway.deleteRestApi(DeleteRestApiRequest.builder_().restApiId(restApiId).build_());
        }
    }

    @Test
    public void testUpdateRetrieveRestApi() {
        PatchOperation patch = PatchOperation.builder_().op(Op.Replace)
                                                   .path("/description").value("updatedDesc").build_();
        apiGateway.updateRestApi(UpdateRestApiRequest.builder_().restApiId(restApiId)
                                                           .patchOperations(patch).build_());

        GetRestApiResult getRestApiResult = apiGateway
                .getRestApi(GetRestApiRequest.builder_().restApiId(restApiId).build_());

        Assert.assertNotNull(getRestApiResult);
        Assert.assertNotNull(getRestApiResult.description());
        Assert.assertNotNull(getRestApiResult.id());
        Assert.assertNotNull(getRestApiResult.name());
        Assert.assertNotNull(getRestApiResult.createdDate());
        Assert.assertEquals(getRestApiResult.name(), NAME);
        Assert.assertEquals(getRestApiResult.description(), "updatedDesc");
    }

    @Test
    public void testCreateUpdateRetrieveApiKey() {
        CreateApiKeyResult createApiKeyResult = apiGateway
                .createApiKey(CreateApiKeyRequest.builder_().name(NAME)
                                                       .description(DESCRIPTION).build_());

        Assert.assertNotNull(createApiKeyResult);
        Assert.assertNotNull(createApiKeyResult.description());
        Assert.assertNotNull(createApiKeyResult.id());
        Assert.assertNotNull(createApiKeyResult.name());
        Assert.assertNotNull(createApiKeyResult.createdDate());
        Assert.assertNotNull(createApiKeyResult.enabled());
        Assert.assertNotNull(createApiKeyResult.lastUpdatedDate());
        Assert.assertNotNull(createApiKeyResult.stageKeys());

        String apiKeyId = createApiKeyResult.id();
        Assert.assertEquals(createApiKeyResult.name(), NAME);
        Assert.assertEquals(createApiKeyResult.description(), DESCRIPTION);

        PatchOperation patch = PatchOperation.builder_().op(Op.Replace)
                                                   .path("/description").value("updatedDesc").build_();
        apiGateway.updateApiKey(UpdateApiKeyRequest.builder_().apiKey(apiKeyId)
                                                         .patchOperations(patch).build_());

        GetApiKeyResult getApiKeyResult = apiGateway
                .getApiKey(GetApiKeyRequest.builder_().apiKey(apiKeyId).build_());

        Assert.assertNotNull(getApiKeyResult);
        Assert.assertNotNull(getApiKeyResult.description());
        Assert.assertNotNull(getApiKeyResult.id());
        Assert.assertNotNull(getApiKeyResult.name());
        Assert.assertNotNull(getApiKeyResult.createdDate());
        Assert.assertNotNull(getApiKeyResult.enabled());
        Assert.assertNotNull(getApiKeyResult.lastUpdatedDate());
        Assert.assertNotNull(getApiKeyResult.stageKeys());
        Assert.assertEquals(getApiKeyResult.id(), apiKeyId);
        Assert.assertEquals(getApiKeyResult.name(), NAME);
        Assert.assertEquals(getApiKeyResult.description(), "updatedDesc");
    }

    @Test
    public void testResourceOperations() {
        GetResourcesResult resourcesResult = apiGateway
                .getResources(GetResourcesRequest.builder_()
                                      .restApiId(restApiId).build_());
        List<Resource> resources = resourcesResult.items();
        Assert.assertEquals(resources.size(), 1);
        Resource rootResource = resources.get(0);
        Assert.assertNotNull(rootResource);
        Assert.assertEquals(rootResource.path(), "/");
        String rootResourceId = rootResource.id();

        CreateResourceResult createResourceResult = apiGateway
                .createResource(CreateResourceRequest.builder_()
                                        .restApiId(restApiId)
                                        .pathPart("fooPath")
                                        .parentId(rootResourceId).build_());
        Assert.assertNotNull(createResourceResult);
        Assert.assertNotNull(createResourceResult.id());
        Assert.assertNotNull(createResourceResult.parentId());
        Assert.assertNotNull(createResourceResult.path());
        Assert.assertNotNull(createResourceResult.pathPart());
        Assert.assertEquals(createResourceResult.pathPart(), "fooPath");
        Assert.assertEquals(createResourceResult.parentId(), rootResourceId);

        PatchOperation patch = PatchOperation.builder_().op(Op.Replace)
                                                   .path("/pathPart").value("updatedPath").build_();
        apiGateway.updateResource(UpdateResourceRequest.builder_()
                                          .restApiId(restApiId)
                                          .resourceId(createResourceResult.id())
                                          .patchOperations(patch).build_());

        GetResourceResult getResourceResult = apiGateway
                .getResource(GetResourceRequest.builder_()
                                     .restApiId(restApiId)
                                     .resourceId(createResourceResult.id()).build_());
        Assert.assertNotNull(getResourceResult);
        Assert.assertNotNull(getResourceResult.id());
        Assert.assertNotNull(getResourceResult.parentId());
        Assert.assertNotNull(getResourceResult.path());
        Assert.assertNotNull(getResourceResult.pathPart());
        Assert.assertEquals(getResourceResult.pathPart(), "updatedPath");
        Assert.assertEquals(getResourceResult.parentId(), rootResourceId);

        PutMethodResult putMethodResult = apiGateway
                .putMethod(PutMethodRequest.builder_().restApiId(restApiId)
                                                 .resourceId(createResourceResult.id())
                                                 .authorizationType("AWS_IAM").httpMethod("PUT").build_());
        Assert.assertNotNull(putMethodResult);
        Assert.assertNotNull(putMethodResult.authorizationType());
        Assert.assertNotNull(putMethodResult.apiKeyRequired());
        Assert.assertNotNull(putMethodResult.httpMethod());
        Assert.assertEquals(putMethodResult.authorizationType(), "AWS_IAM");
        Assert.assertEquals(putMethodResult.httpMethod(), "PUT");

        PutIntegrationResult putIntegrationResult = apiGateway
                .putIntegration(PutIntegrationRequest.builder_()
                                        .restApiId(restApiId)
                                        .resourceId(createResourceResult.id())
                                        .httpMethod("PUT").type(IntegrationType.MOCK)
                                        .uri("http://foo.bar")
                                        .integrationHttpMethod("GET").build_());
        Assert.assertNotNull(putIntegrationResult);
        Assert.assertNotNull(putIntegrationResult.cacheNamespace());
        Assert.assertNotNull(putIntegrationResult.type());
        Assert.assertEquals(putIntegrationResult.type(),
                            IntegrationType.MOCK.toString());
    }
}
