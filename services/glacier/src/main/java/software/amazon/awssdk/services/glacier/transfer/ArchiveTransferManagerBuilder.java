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

import software.amazon.awssdk.services.glacier.GlacierClient;
import software.amazon.awssdk.services.sns.SNSClient;
import software.amazon.awssdk.services.sqs.SQSClient;

/**
 * Fluent builder for {@link ArchiveTransferManager}. Use of the builder is preferred over
 * constructors for {@link ArchiveTransferManager}.
 */
public final class ArchiveTransferManagerBuilder {
    private GlacierClient glacierClient;

    private SQSClient sqsClient;

    private SNSClient snsClient;

    /**
     * @return The client used for uploading and downloading data to and from Amazon Glacier.
     */
    public GlacierClient getGlacierClient() {
        return glacierClient;
    }

    /**
     * Set the client for uploading and downloading data to and from Amazon Glacier.
     *
     * @param glacierClient The Amazon Glacier client.
     */
    public void setGlacierClient(GlacierClient glacierClient) {
        this.glacierClient = glacierClient;
    }

    /**
     * Set the client for uploading and downloading data to and from Amazon Glacier.
     *
     * @param glacierClient The Amazon Glacier client.
     *
     * @return This object for chaining.
     */
    public ArchiveTransferManagerBuilder withGlacierClient(GlacierClient glacierClient) {
        setGlacierClient(glacierClient);
        return this;
    }

    /**
     * @return The client for working with Amazon SQS when polling for the archive retrieval job
     *     status.
     */
    public SQSClient getSqsClient() {
        return sqsClient;
    }

    /**
     * Set the client for working with Amazon SQS when polling for the archive retrieval job status.
     *
     * @param sqsClient The SQS client.
     */
    public void setSqsClient(SQSClient sqsClient) {
        this.sqsClient = sqsClient;
    }

    /**
     * Set the SQS client for working with Amazon SQS when polling for the archive retrieval job
     * status.
     *
     * @param sqsClient The SQS client.
     *
     * @return This object for chaining.
     */
    public ArchiveTransferManagerBuilder withSqsClient(SQSClient sqsClient) {
        setSqsClient(sqsClient);
        return this;
    }

    /**
     * @return The client for working with Amazon SNS when polling for the archive retrieval job
     *     status.
     */
    public SNSClient getSnsClient() {
        return snsClient;
    }

    /**
     * Set the client for working with Amazon SNS when polling for the archive retrieval job status.
     *
     * @param snsClient The SNS client.
     */
    public void setSnsClient(SNSClient snsClient) {
        this.snsClient = snsClient;
    }

    /**
     * Set the client for working with Amazon SNS when polling for the archive retrieval job status.
     *
     * @param snsClient The SNS client.
     *
     * @return This object for chaining.
     */
    public ArchiveTransferManagerBuilder withSnsClient(SNSClient snsClient) {
        setSnsClient(snsClient);
        return this;
    }

    private GlacierClient resolveGlacierClient() {
        return glacierClient == null ? GlacierClient.create() : glacierClient;
    }

    private SNSClient resolveSnsClient() {
        return snsClient == null ? SNSClient.create() : snsClient;
    }

    private SQSClient resolveSqsClient() {
        return sqsClient == null ? SQSClient.create() : sqsClient;
    }

    private ArchiveTransferManagerParams getParams() {
        return new ArchiveTransferManagerParams()
                .withAmazonGlacier(resolveGlacierClient())
                .withAmazonSqs(resolveSqsClient())
                .withAmazonSns(resolveSnsClient());
    }

    /**
     * @return An instance of {@link ArchiveTransferManager} using the configured options.
     */
    public ArchiveTransferManager build() {
        return new ArchiveTransferManager(getParams());
    }
}
