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

package software.amazon.awssdk.services.json;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import javax.annotation.Generated;
import software.amazon.awssdk.client.AwsAsyncClientParams;
import software.amazon.awssdk.services.json.model.APostOperationRequest;
import software.amazon.awssdk.services.json.model.APostOperationResult;
import software.amazon.awssdk.services.json.model.APostOperationWithOutputRequest;
import software.amazon.awssdk.services.json.model.APostOperationWithOutputResult;

@Generated("software.amazon.awssdk:codegen")
public class DefaultJsonAsyncClient implements JsonAsyncClient {
    private final JsonClient syncClient;

    private final ExecutorService executor;

    DefaultJsonAsyncClient(AwsAsyncClientParams asyncClientParams) {
        this.syncClient = new DefaultJsonClient(asyncClientParams);
        this.executor = asyncClientParams.getExecutor();
    }

    /**
     * <p>
     * Performs a post operation to the query service and has no output
     * </p>
     *
     * @param aPostOperationRequest
     * @return A Java Future containing the result of the APostOperation operation returned by the service.
     * @sample JsonAsyncClient.APostOperation
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/APostOperation" target="_top">AWS
     *      API Documentation</a>
     */
    @Override
    public CompletableFuture<APostOperationResult> aPostOperation(APostOperationRequest aPostOperationRequest) {
        return CompletableFuture.supplyAsync(() -> syncClient.aPostOperation(aPostOperationRequest), executor);
    }

    /**
     * <p>
     * Performs a post operation to the query service and has modelled output
     * </p>
     *
     * @param aPostOperationWithOutputRequest
     * @return A Java Future containing the result of the APostOperationWithOutput operation returned by the service.
     * @sample JsonAsyncClient.APostOperationWithOutput
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/APostOperationWithOutput"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public CompletableFuture<APostOperationWithOutputResult> aPostOperationWithOutput(
        APostOperationWithOutputRequest aPostOperationWithOutputRequest) {
        return CompletableFuture
            .supplyAsync(() -> syncClient.aPostOperationWithOutput(aPostOperationWithOutputRequest), executor);
    }

    @Override
    public void close() throws Exception {
        syncClient.close();
    }
}
