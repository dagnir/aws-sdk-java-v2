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

package software.amazon.awssdk.services.query;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Generated;
import org.w3c.dom.Node;
import software.amazon.awssdk.AmazonServiceException;
import software.amazon.awssdk.AmazonWebServiceResponse;
import software.amazon.awssdk.client.AwsSyncClientParams;
import software.amazon.awssdk.client.ClientExecutionParams;
import software.amazon.awssdk.client.ClientHandler;
import software.amazon.awssdk.client.ClientHandlerParams;
import software.amazon.awssdk.client.SdkClientHandler;
import software.amazon.awssdk.http.DefaultErrorResponseHandler;
import software.amazon.awssdk.http.StaxResponseHandler;
import software.amazon.awssdk.runtime.transform.StandardErrorUnmarshaller;
import software.amazon.awssdk.runtime.transform.Unmarshaller;
import software.amazon.awssdk.services.query.model.APostOperationRequest;
import software.amazon.awssdk.services.query.model.APostOperationResult;
import software.amazon.awssdk.services.query.model.APostOperationWithOutputRequest;
import software.amazon.awssdk.services.query.model.APostOperationWithOutputResult;
import software.amazon.awssdk.services.query.model.QueryException;
import software.amazon.awssdk.services.query.transform.APostOperationRequestMarshaller;
import software.amazon.awssdk.services.query.transform.APostOperationResultUnmarshaller;
import software.amazon.awssdk.services.query.transform.APostOperationWithOutputRequestMarshaller;
import software.amazon.awssdk.services.query.transform.APostOperationWithOutputResultUnmarshaller;
import software.amazon.awssdk.services.query.transform.InvalidInputExceptionUnmarshaller;
import software.amazon.awssdk.services.query.waiters.QueryClientWaiters;

@Generated("software.amazon.awssdk:codegen")
public class DefaultQueryClient implements QueryClient {
    private final ClientHandler clientHandler;

    private final List<Unmarshaller<AmazonServiceException, Node>> exceptionUnmarshallers;

    private final AwsSyncClientParams clientParams;

    private volatile QueryClientWaiters waiters;

    protected DefaultQueryClient(AwsSyncClientParams clientParams) {
        this.clientHandler = new SdkClientHandler(new ClientHandlerParams().withClientParams(clientParams)
                                                                           .withCalculateCrc32FromCompressedDataEnabled(false));
        this.clientParams = clientParams;
        this.exceptionUnmarshallers = init();
    }

    @Override
    public APostOperationResult aPostOperation(APostOperationRequest aPostOperationRequest) {

        StaxResponseHandler<APostOperationResult> responseHandler = new StaxResponseHandler<APostOperationResult>(
            new APostOperationResultUnmarshaller());

        DefaultErrorResponseHandler errorResponseHandler = new DefaultErrorResponseHandler(exceptionUnmarshallers);

        return clientHandler.execute(
            new ClientExecutionParams<APostOperationRequest, AmazonWebServiceResponse<APostOperationResult>>()
                .withMarshaller(new APostOperationRequestMarshaller()).withResponseHandler(responseHandler)
                .withErrorResponseHandler(errorResponseHandler).withInput(aPostOperationRequest)).getResult();
    }

    @Override
    public APostOperationWithOutputResult aPostOperationWithOutput(APostOperationWithOutputRequest aPostOperationWithOutputRequest) {

        StaxResponseHandler<APostOperationWithOutputResult> responseHandler = new StaxResponseHandler<APostOperationWithOutputResult>(
            new APostOperationWithOutputResultUnmarshaller());

        DefaultErrorResponseHandler errorResponseHandler = new DefaultErrorResponseHandler(exceptionUnmarshallers);

        return clientHandler
            .execute(
                new ClientExecutionParams<APostOperationWithOutputRequest, AmazonWebServiceResponse<APostOperationWithOutputResult>>()
                    .withMarshaller(new APostOperationWithOutputRequestMarshaller())
                    .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                    .withInput(aPostOperationWithOutputRequest)).getResult();
    }

    private List<Unmarshaller<AmazonServiceException, Node>> init() {
        List<Unmarshaller<AmazonServiceException, Node>> unmarshallers = new ArrayList<>();
        unmarshallers.add(new InvalidInputExceptionUnmarshaller());
        unmarshallers.add(new StandardErrorUnmarshaller(QueryException.class));
        return unmarshallers;
    }

    public QueryClientWaiters waiters() {
        if (waiters == null) {
            synchronized (this) {
                if (waiters == null) {
                    waiters = new QueryClientWaiters(this);
                }
            }
        }
        return waiters;
    }

    @Override
    public void close() throws Exception {
        clientHandler.close();
    }
}
