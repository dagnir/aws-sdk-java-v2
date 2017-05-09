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
        DescribeWorkspacesResult result = client.describeWorkspaces(DescribeWorkspacesRequest.builder_().build_());
        assertTrue(result.workspaces().isEmpty());
    }

    @Test
    public void describeWorkspaceBundles() {
        DescribeWorkspaceBundlesResult result = client.describeWorkspaceBundles(DescribeWorkspaceBundlesRequest.builder_().build_());
        assertTrue(result.bundles().isEmpty());
    }

    @Test
    public void describeWorkspaceDirectories() {
        DescribeWorkspaceDirectoriesResult result = client.describeWorkspaceDirectories(DescribeWorkspaceDirectoriesRequest.builder_().build_());
        assertTrue(result.directories().isEmpty());
    }

    @Test
    public void createWorkspaces() {
        CreateWorkspacesResult result = client.createWorkspaces(CreateWorkspacesRequest.builder_()
                .workspaces(WorkspaceRequest.builder_()
                        .userName("hchar")
                        .bundleId("wsb-12345678")
                        .directoryId("d-12345678")
                        .build_())
                .build_());
        assertTrue(result.failedRequests().size() == 1);
    }
}
