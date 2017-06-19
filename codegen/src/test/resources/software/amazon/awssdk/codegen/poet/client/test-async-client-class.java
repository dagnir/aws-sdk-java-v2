package software.amazon.awssdk.services.json;

import java.util.concurrent.CompletableFuture;
import javax.annotation.Generated;
import software.amazon.awssdk.AmazonServiceException;
import software.amazon.awssdk.client.AsyncClientHandler;
import software.amazon.awssdk.client.AwsAsyncClientParams;
import software.amazon.awssdk.client.ClientExecutionParams;
import software.amazon.awssdk.client.ClientHandlerParams;
import software.amazon.awssdk.client.SdkAsyncClientHandler;
import software.amazon.awssdk.http.HttpResponseHandler;
import software.amazon.awssdk.protocol.json.JsonClientMetadata;
import software.amazon.awssdk.protocol.json.JsonErrorResponseMetadata;
import software.amazon.awssdk.protocol.json.JsonErrorShapeMetadata;
import software.amazon.awssdk.protocol.json.JsonOperationMetadata;
import software.amazon.awssdk.protocol.json.SdkJsonProtocolFactory;
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
public class DefaultJsonAsyncClient implements JsonAsyncClient {
    private final AsyncClientHandler clientHandler;

    private final SdkJsonProtocolFactory protocolFactory;

    protected DefaultJsonAsyncClient(AwsAsyncClientParams clientParams) {
        this.clientHandler = new SdkAsyncClientHandler(new ClientHandlerParams().withAsyncClientParams(clientParams)
                                                                                .withClientParams(clientParams).withCalculateCrc32FromCompressedDataEnabled(false));
        this.protocolFactory = init();
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
    public CompletableFuture<APostOperationResponse> aPostOperation(APostOperationRequest aPostOperationRequest) {

        HttpResponseHandler<APostOperationResponse> responseHandler = protocolFactory.createResponseHandler(
                new JsonOperationMetadata().withPayloadJson(true).withHasStreamingSuccessResponse(false),
                new APostOperationResponseUnmarshaller());

        HttpResponseHandler<AmazonServiceException> errorResponseHandler = createErrorResponseHandler();

        return clientHandler.execute(new ClientExecutionParams<APostOperationRequest, APostOperationResponse>()
                                             .withMarshaller(new APostOperationRequestMarshaller(protocolFactory)).withResponseHandler(responseHandler)
                                             .withErrorResponseHandler(errorResponseHandler).withInput(aPostOperationRequest));
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
    public CompletableFuture<APostOperationWithOutputResponse> aPostOperationWithOutput(
            APostOperationWithOutputRequest aPostOperationWithOutputRequest) {

        HttpResponseHandler<APostOperationWithOutputResponse> responseHandler = protocolFactory.createResponseHandler(
                new JsonOperationMetadata().withPayloadJson(true).withHasStreamingSuccessResponse(false),
                new APostOperationWithOutputResponseUnmarshaller());

        HttpResponseHandler<AmazonServiceException> errorResponseHandler = createErrorResponseHandler();

        return clientHandler
                .execute(new ClientExecutionParams<APostOperationWithOutputRequest, APostOperationWithOutputResponse>()
                                 .withMarshaller(new APostOperationWithOutputRequestMarshaller(protocolFactory))
                                 .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                                 .withInput(aPostOperationWithOutputRequest));
    }

    @Override
    public void close() throws Exception {
        clientHandler.close();
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

    private HttpResponseHandler<AmazonServiceException> createErrorResponseHandler() {
        return protocolFactory.createErrorResponseHandler(new JsonErrorResponseMetadata());
    }
}
