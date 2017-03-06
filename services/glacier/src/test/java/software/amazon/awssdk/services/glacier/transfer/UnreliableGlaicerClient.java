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

import software.amazon.awssdk.AmazonClientException;
import software.amazon.awssdk.AmazonServiceException;
import software.amazon.awssdk.LegacyClientConfiguration;
import software.amazon.awssdk.auth.AwsCredentials;
import software.amazon.awssdk.services.glacier.AmazonGlacierClient;
import software.amazon.awssdk.services.glacier.model.GetJobOutputRequest;
import software.amazon.awssdk.services.glacier.model.GetJobOutputResult;
import software.amazon.awssdk.test.util.UnreliableRandomInputStream;

/**
 * The customized AWS Glacier java client which has the ability to trigger IO exception and
 * bad checksum for the test.
 *
 */
public class UnreliableGlaicerClient extends AmazonGlacierClient {

    private static int count = 0;
    private static int M = 1024 * 1024;
    // If the recoverable is false, the client will never return correct result.
    boolean recoverable = true;
    private boolean reliable = false;

    /**
     * Set the an unreliable AWS Glacier java client.
     */
    public UnreliableGlaicerClient(AwsCredentials awsCredentials) {
        super(awsCredentials, new LegacyClientConfiguration());
    }

    /**
     * Set the an unreliable AWS Glacier java client, and denote it whether is recoverable or not.
     */
    public UnreliableGlaicerClient(AwsCredentials awsCredentials, boolean recoverable) {
        super(awsCredentials, new LegacyClientConfiguration());
        this.recoverable = recoverable;
    }

    public void setReliable(boolean reliable) {
        this.reliable = reliable;
    }

    public void setRecoverable(boolean recoverable) {
        this.recoverable = recoverable;
    }

    /**
     *  Override the getJobOutput method in AWS Glacier Java Client to have the
     *  ability to trigger IO exception and bad checksum.
     */
    @Override
    public GetJobOutputResult getJobOutput(GetJobOutputRequest getJobOutputRequest)
            throws AmazonServiceException, AmazonClientException {
        GetJobOutputResult result = super.getJobOutput(getJobOutputRequest);
        if (reliable == true) {
            return result;
        }

        if (recoverable == false) {
            System.out.println("trigger a bad checksum!");
            result.setChecksum("123");
            return result;
        }

        if (count % 4 == 0) {
            System.out.println("trigger a bad checksum!");
            result.setChecksum("123");

        }

        if (count % 4 == 1) {
            System.out.println("trigger an unreliable input stream!");
            result.setBody(new UnreliableRandomInputStream(M));
        }

        if (count % 4 == 2) {
            System.out.println("trigger an unreliable input stream!");
            result.setBody(new UnreliableRandomInputStream(M));
        }

        count++;
        // prevent count overflow
        if (count == 4) {
            count = 0;
        }

        return result;
    }
}
