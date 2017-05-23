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

package software.amazon.awssdk.http.pipeline.stages;

import static software.amazon.awssdk.http.AmazonHttpClient.HEADER_SDK_TRANSACTION_ID;
import static software.amazon.awssdk.http.AmazonHttpClient.checkInterrupted;

import java.util.Random;
import java.util.UUID;
import software.amazon.awssdk.Request;
import software.amazon.awssdk.RequestExecutionContext;
import software.amazon.awssdk.http.pipeline.RequestToRequestPipeline;

/**
 * Generates a unique identifier for the request that is consistent across retries.
 */
public class ApplyTransactionIdStage implements RequestToRequestPipeline {

    /**
     * Used to generate UUID's for client transaction id. This gives a higher probability of id
     * clashes but is more performant then using {@link UUID#randomUUID()} which uses SecureRandom
     * internally.
     **/
    private final Random random = new Random();

    @Override
    public Request<?> execute(Request<?> request, RequestExecutionContext context) throws Exception {
        checkInterrupted();
        request.addHeader(HEADER_SDK_TRANSACTION_ID, new UUID(random.nextLong(), random.nextLong()).toString());
        return request;
    }
}
