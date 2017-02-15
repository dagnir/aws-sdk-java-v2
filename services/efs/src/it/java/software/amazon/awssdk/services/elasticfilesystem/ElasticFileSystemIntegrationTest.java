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

package software.amazon.awssdk.services.elasticfilesystem;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.UUID;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.regions.Regions;
import software.amazon.awssdk.services.elasticfilesystem.model.CreateFileSystemRequest;
import software.amazon.awssdk.services.elasticfilesystem.model.DeleteFileSystemRequest;
import software.amazon.awssdk.services.elasticfilesystem.model.DescribeFileSystemsRequest;
import software.amazon.awssdk.services.elasticfilesystem.model.FileSystemAlreadyExistsException;
import software.amazon.awssdk.services.elasticfilesystem.model.FileSystemNotFoundException;
import software.amazon.awssdk.test.AWSIntegrationTestBase;
import software.amazon.awssdk.util.StringUtils;

public class ElasticFileSystemIntegrationTest extends AWSIntegrationTestBase {

    private static AmazonElasticFileSystemClient client;
    private String fileSystemId;

    @BeforeClass
    public static void setupFixture() throws Exception {
        client = new AmazonElasticFileSystemClient(getCredentials());
        client.configureRegion(Regions.US_WEST_2);
    }

    @After
    public void tearDown() {
        if (!StringUtils.isNullOrEmpty(fileSystemId)) {
            client.deleteFileSystem(new DeleteFileSystemRequest().withFileSystemId(fileSystemId));
        }
    }

    @Test
    public void describeFileSystems_ReturnsNonNull() {
        assertNotNull(client.describeFileSystems());
    }

    @Test
    public void describeFileSystem_NonExistentFileSystem_ThrowsException() {
        try {
            client.describeFileSystems(new DescribeFileSystemsRequest().withFileSystemId("fs-00000000"));
        } catch (FileSystemNotFoundException e) {
            assertEquals("FileSystemNotFound", e.getErrorCode());
        }
    }

    /**
     * Tests that an exception with a member in it is serialized properly. See TT0064111680
     */
    @Test
    public void createFileSystem_WithDuplicateCreationToken_ThrowsExceptionWithFileSystemIdPresent() {
        String creationToken = UUID.randomUUID().toString();
        this.fileSystemId = client.createFileSystem(new CreateFileSystemRequest().withCreationToken(creationToken))
                                  .getFileSystemId();
        try {
            client.createFileSystem(new CreateFileSystemRequest().withCreationToken(creationToken)).getFileSystemId();
        } catch (FileSystemAlreadyExistsException e) {
            assertEquals(fileSystemId, e.getFileSystemId());
        }
    }
}
