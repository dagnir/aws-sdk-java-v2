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

package software.amazon.awssdk.services.ecr;

import junit.framework.Assert;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.services.ecr.model.CreateRepositoryRequest;
import software.amazon.awssdk.services.ecr.model.CreateRepositoryResult;
import software.amazon.awssdk.services.ecr.model.DeleteRepositoryRequest;
import software.amazon.awssdk.services.ecr.model.DescribeRepositoriesRequest;
import software.amazon.awssdk.services.ecr.model.Repository;
import software.amazon.awssdk.test.AWSIntegrationTestBase;

public class ECRIntegrationTest extends AWSIntegrationTestBase {

    private static final String REPO_NAME = "java-sdk-test-repo-" + System.currentTimeMillis();
    private static AmazonECR ecr;

    @BeforeClass
    public static void setUpClient() throws Exception {
        setUpCredentials();
        ecr = new AmazonECRClient(getCredentials());
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        if (ecr != null) {
            ecr.deleteRepository(new DeleteRepositoryRequest()
                                         .withRepositoryName(REPO_NAME));
        }
    }

    @Test
    public void basicTest() {
        CreateRepositoryResult result = ecr.createRepository(
                new CreateRepositoryRequest().withRepositoryName(REPO_NAME));

        Assert.assertNotNull(result.getRepository());
        Assert.assertEquals(result.getRepository().getRepositoryName(), REPO_NAME);
        Assert.assertNotNull(result.getRepository().getRepositoryArn());
        Assert.assertNotNull(result.getRepository().getRegistryId());

        String repoArn = result.getRepository().getRepositoryArn();
        String registryId = result.getRepository().getRegistryId();

        Repository repo = ecr.describeRepositories(new DescribeRepositoriesRequest()
                                                           .withRepositoryNames(REPO_NAME)).getRepositories().get(0);

        Assert.assertEquals(repo.getRegistryId(), registryId);
        Assert.assertEquals(repo.getRepositoryName(), REPO_NAME);
        Assert.assertEquals(repo.getRepositoryArn(), repoArn);
    }

}
