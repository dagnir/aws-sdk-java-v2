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

/**
 * Common abstract base class for result that contains server side encryption
 * (SSE) information.
 */
public abstract class SseResultBase implements ServerSideEncryptionResult {
    private String sseAlgorithm;
    private String sseCustomerAlgorithm;
    private String sseCustomerKeyMD5;

    @Override
    public final String getSseAlgorithm() {
        return sseAlgorithm;
    }

    @Override
    public final void setSseAlgorithm(String algorithm) {
        this.sseAlgorithm = algorithm;
    }

    @Override
    public final String getSseCustomerAlgorithm() {
        return sseCustomerAlgorithm;
    }

    @Override
    public final void setSseCustomerAlgorithm(String algorithm) {
        this.sseCustomerAlgorithm = algorithm;
    }

    @Override
    public final String getSseCustomerKeyMd5() {
        return sseCustomerKeyMD5;
    }

    @Override
    public final void setSseCustomerKeyMd5(String md5) {
        this.sseCustomerKeyMD5 = md5;
    }

    /**
     * @deprecated Replaced by {@link #getSseAlgorithm()}
     */
    @Deprecated
    public final String getServerSideEncryption() {
        return sseAlgorithm;
    }
}
