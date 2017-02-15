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

package software.amazon.awssdk.services.s3.internal;

import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadResult;
import software.amazon.awssdk.services.s3.model.CopyObjectResult;
import software.amazon.awssdk.services.s3.model.PutObjectResult;

/**
 * Any operation output which has the versionId field should implement this interface.
 * This field will only be present if object versioning has been enabled for the bucket.
 */
public interface S3VersionResult {

    /**
     * Gets the version ID of the newly copied object. This field is only
     * present if object versioning has been enabled for the bucket the
     * object was copied to.
     *
     * @return The version ID of the newly copied object.
     *
     * @see CopyObjectResult#setVersionId(String)
     * @see CompleteMultipartUploadResult#getVersionId()
     * @see PutObjectResult#getVersionId()
     */
    public String getVersionId();

    /**
     * Sets the version ID of the newly copied object.
     *
     * @param versionId
     *            The version ID of the newly copied object.
     *
     * @see CopyObjectResult#getVersionId()
     * @see CompleteMultipartUploadResult#getVersionId()
     * @see PutObjectResult#getVersionId()
     */
    public void setVersionId(String versionId);

}
