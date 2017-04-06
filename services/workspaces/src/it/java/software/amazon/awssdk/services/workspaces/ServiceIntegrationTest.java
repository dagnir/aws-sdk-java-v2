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

package software.amazon.awssdk.services.workspaces;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import software.amazon.awssdk.services.workspaces.model.CreateWorkspacesRequest;
import software.amazon.awssdk.services.workspaces.model.CreateWorkspacesResult;
import software.amazon.awssdk.services.workspaces.model.DescribeWorkspaceBundlesRequest;
import software.amazon.awssdk.services.workspaces.model.DescribeWorkspaceBundlesResult;
import software.amazon.awssdk.services.workspaces.model.DescribeWorkspaceDirectoriesRequest;
import software.amazon.awssdk.services.workspaces.model.DescribeWorkspaceDirectoriesResult;
import software.amazon.awssdk.services.workspaces.model.DescribeWorkspacesRequest;
import software.amazon.awssdk.services.workspaces.model.DescribeWorkspacesResult;
import software.amazon.awssdk.services.workspaces.model.WorkspaceRequest;

public class ServiceIntegrationTest extends IntegrationTestBase {

    @Test
    public void describeWorkspaces() {
        DescribeWorkspacesResult result = client.describeWorkspaces(new DescribeWorkspacesRequest());
        assertTrue(result.getWorkspaces().isEmpty());
    }

    @Test
    public void describeWorkspaceBundles() {
        DescribeWorkspaceBundlesResult result = client.describeWorkspaceBundles(new DescribeWorkspaceBundlesRequest());
        assertTrue(result.getBundles().isEmpty());
    }

    @Test
    public void describeWorkspaceDirectories() {
        DescribeWorkspaceDirectoriesResult result = client.describeWorkspaceDirectories(new DescribeWorkspaceDirectoriesRequest());
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
