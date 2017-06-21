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

import javax.annotation.Generated;
import software.amazon.awssdk.AmazonServiceException;
import software.amazon.awssdk.AmazonWebServiceResponse;
import software.amazon.awssdk.auth.presign.PresignerParams;
import software.amazon.awssdk.client.AwsSyncClientParams;
import software.amazon.awssdk.client.ClientExecutionParams;
import software.amazon.awssdk.client.ClientHandler;
import software.amazon.awssdk.client.ClientHandlerParams;
import software.amazon.awssdk.client.SdkClientHandler;
import software.amazon.awssdk.http.HttpResponseHandler;
import software.amazon.awssdk.protocol.json.JsonClientMetadata;
import software.amazon.awssdk.protocol.json.JsonErrorResponseMetadata;
import software.amazon.awssdk.protocol.json.JsonErrorShapeMetadata;
import software.amazon.awssdk.protocol.json.JsonOperationMetadata;
import software.amazon.awssdk.protocol.json.SdkJsonProtocolFactory;
import software.amazon.awssdk.services.acm.presign.AcmClientPresigners;
import software.amazon.awssdk.services.json.model.APostOperationRequest;
import software.amazon.awssdk.services.json.model.APostOperationResponse;
import software.amazon.awssdk.services.json.model.APostOperationWithOutputRequest;
import software.amazon.awssdk.services.json.model.APostOperationWithOutputResponse;
import software.amazon.awssdk.services.json.model.InvalidInputException;
import software.amazon.awssdk.services.json.transform.APostOperationRequestMarshaller;
import software.amazon.awssdk.services.json.transform.APostOperationResponseUnmarshaller;
import software.amazon.awssdk.services.json.transform.APostOperationWithOutputRequestMarshaller;
import software.amazon.awssdk.services.json.transform.APostOperationWithOutputResponseUnmarshaller;

@Generated("software.amazon.awssdk:codegen")
class DefaultJsonClient implements JsonClient {
    private final ClientHandler clientHandler;

    private final SdkJsonProtocolFactory protocolFactory;

    private final AwsSyncClientParams clientParams;

    protected DefaultJsonClient(AwsSyncClientParams clientParams) {
        this.clientHandler = new SdkClientHandler(new ClientHandlerParams().withClientParams(clientParams)
                                                                           .withCalculateCrc32FromCompressedDataEnabled(false));
        this.clientParams = clientParams;
        this.protocolFactory = init();
    }

    @Override
    public APostOperationResponse aPostOperation(APostOperationRequest aPostOperationRequest) {

        HttpResponseHandler<AmazonWebServiceResponse<APostOperationResponse>> responseHandler = protocolFactory
            .createResponseHandler(new JsonOperationMetadata().withPayloadJson(true).withHasStreamingSuccessResponse(false),
                                   new APostOperationResponseUnmarshaller());

        HttpResponseHandler<AmazonServiceException> errorResponseHandler = createErrorResponseHandler();

        return clientHandler.execute(
            new ClientExecutionParams<APostOperationRequest, AmazonWebServiceResponse<APostOperationResponse>>()
                .withMarshaller(new APostOperationRequestMarshaller(protocolFactory))
                .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                .withInput(aPostOperationRequest)).getResult();
    }

    @Override
    public APostOperationWithOutputResponse aPostOperationWithOutput(APostOperationWithOutputRequest aPostOperationWithOutputRequest) {

        HttpResponseHandler<AmazonWebServiceResponse<APostOperationWithOutputResponse>> responseHandler = protocolFactory
            .createResponseHandler(new JsonOperationMetadata().withPayloadJson(true).withHasStreamingSuccessResponse(false),
                                   new APostOperationWithOutputResponseUnmarshaller());

        HttpResponseHandler<AmazonServiceException> errorResponseHandler = createErrorResponseHandler();

        return clientHandler
            .execute(
                new ClientExecutionParams<APostOperationWithOutputRequest, AmazonWebServiceResponse<APostOperationWithOutputResponse>>()
                    .withMarshaller(new APostOperationWithOutputRequestMarshaller(protocolFactory))
                    .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                    .withInput(aPostOperationWithOutputRequest)).getResult();
    }

    private HttpResponseHandler<AmazonServiceException> createErrorResponseHandler() {
        return protocolFactory.createErrorResponseHandler(new JsonErrorResponseMetadata());
    }

    private software.amazon.awssdk.protocol.json.SdkJsonProtocolFactory init() {
        return new SdkJsonProtocolFactory(new JsonClientMetadata()
                                              .withProtocolVersion("1.1")
                                              .withSupportsCbor(false)
                                              .withSupportsIon(false)
                                              .withBaseServiceExceptionClass(software.amazon.awssdk.services.json.model.JsonException.class)
                                              .addErrorMetadata(
                                                  new JsonErrorShapeMetadata().withErrorCode("InvalidInput").withModeledClass(InvalidInputException.class)));
    }

    public AcmClientPresigners presigners() {
        return new AcmClientPresigners(PresignerParams.builder().endpoint(clientParams.getEndpoint())
                                                      .credentialsProvider(clientParams.getCredentialsProvider()).signerProvider(clientParams.getSignerProvider())
                                                      .build());
    }

    @Override
    public void close() throws Exception {
        clientHandler.close();
    }
}
