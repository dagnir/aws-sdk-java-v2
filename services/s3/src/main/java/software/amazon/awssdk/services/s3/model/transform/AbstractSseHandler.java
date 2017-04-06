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

package software.amazon.awssdk.services.s3.model.transform;

import software.amazon.awssdk.services.s3.internal.ServerSideEncryptionResult;

/**
 * Package private abstract base for all abstract handler that has server side
 * encryption (SSE) information.
 */
abstract class AbstractSseHandler extends AbstractHandler implements ServerSideEncryptionResult {
    /**
     * Used to get access to the specific server side encryption (SSE) result
     * from the subclass.
     */
    protected abstract ServerSideEncryptionResult sseResult();

    @Override
    public final String getSseAlgorithm() {
        ServerSideEncryptionResult result = sseResult();
        return result == null ? null : result.getSseAlgorithm();
    }

    @Override
    public final void setSseAlgorithm(String serverSideEncryption) {
        ServerSideEncryptionResult result = sseResult();
        if (result != null) {
            result.setSseAlgorithm(serverSideEncryption);
        }
    }

    @Override
    public final String getSseCustomerAlgorithm() {
        ServerSideEncryptionResult result = sseResult();
        return result == null ? null : result.getSseCustomerAlgorithm();
    }

    @Override
    public final void setSseCustomerAlgorithm(String algorithm) {
        ServerSideEncryptionResult result = sseResult();
        if (result != null) {
            result.setSseCustomerAlgorithm(algorithm);
        }
    }

    @Override
    public final String getSseCustomerKeyMd5() {
        ServerSideEncryptionResult result = sseResult();
        return result == null ? null : result.getSseCustomerKeyMd5();
    }

    @Override
    public final void setSseCustomerKeyMd5(String md5Digest) {
        ServerSideEncryptionResult result = sseResult();
        if (result != null) {
            result.setSseCustomerKeyMd5(md5Digest);
        }
    }
}
