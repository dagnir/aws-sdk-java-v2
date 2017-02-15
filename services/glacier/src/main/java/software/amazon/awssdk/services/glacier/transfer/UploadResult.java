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

package software.amazon.awssdk.services.glacier.transfer;

/**
 * The result of uploading an archive to Amazon Glacier using
 * {@link ArchiveTransferManager}.
 *
 * @see ArchiveTransferManager
 */
public class UploadResult {

    /** The ID of the uploaded archive */
    private final String archiveId;

    /**
     * Constructs a new UploadResult with the specified archive ID.
     *
     * @param archiveId
     *            The ID of the uploaded artifact.
     */
    public UploadResult(String archiveId) {
        this.archiveId = archiveId;
    }

    /**
     * Returns the ID of the uploaded archive.
     *
     * @return The ID of the uploaded archive.
     */
    public String getArchiveId() {
        return archiveId;
    }
}