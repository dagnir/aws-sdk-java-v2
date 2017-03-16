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

import software.amazon.awssdk.annotation.SdkInternalApi;
import software.amazon.awssdk.services.glacier.GlacierClient;
import software.amazon.awssdk.services.sns.SNSClient;
import software.amazon.awssdk.services.sqs.SQSClient;

/**
 * Internal class to wrap all params needed by {@link ArchiveTransferManager}.  Used by {@link
 * ArchiveTransferManagerBuilder}.
 */
@SdkInternalApi
class ArchiveTransferManagerParams {
    private GlacierClient amazonGlacier;

    private SQSClient amazonSQS;

    private SNSClient amazonSNS;

    public GlacierClient getAmazonGlacier() {
        return amazonGlacier;
    }

    public void setAmazonGlacier(GlacierClient amazonGlacier) {
        this.amazonGlacier = amazonGlacier;
    }

    public ArchiveTransferManagerParams withAmazonGlacier(GlacierClient amazonGlacier) {
        setAmazonGlacier(amazonGlacier);
        return this;
    }

    public SQSClient getAmazonSQS() {
        return amazonSQS;
    }

    public void setAmazonSQS(SQSClient amazonSQS) {
        this.amazonSQS = amazonSQS;
    }

    public ArchiveTransferManagerParams withAmazonSQS(SQSClient amazonSQS) {
        setAmazonSQS(amazonSQS);
        return this;
    }

    public SNSClient getAmazonSNS() {
        return amazonSNS;
    }

    public void setAmazonSNS(SNSClient amazonSNS) {
        this.amazonSNS = amazonSNS;
    }

    public ArchiveTransferManagerParams withAmazonSNS(SNSClient amazonSNS) {
        setAmazonSNS(amazonSNS);
        return this;
    }
}
