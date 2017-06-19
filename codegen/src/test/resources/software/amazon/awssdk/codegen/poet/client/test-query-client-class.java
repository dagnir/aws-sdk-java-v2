package software.amazon.awssdk.services.query;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Generated;
import org.w3c.dom.Node;
import software.amazon.awssdk.AmazonServiceException;
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
import software.amazon.awssdk.services.query.model.APostOperationResponse;
import software.amazon.awssdk.services.query.model.APostOperationWithOutputRequest;
import software.amazon.awssdk.services.query.model.APostOperationWithOutputResponse;
import software.amazon.awssdk.services.query.model.QueryException;
import software.amazon.awssdk.services.query.transform.APostOperationRequestMarshaller;
import software.amazon.awssdk.services.query.transform.APostOperationResponseUnmarshaller;
import software.amazon.awssdk.services.query.transform.APostOperationWithOutputRequestMarshaller;
import software.amazon.awssdk.services.query.transform.APostOperationWithOutputResponseUnmarshaller;
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
    public APostOperationResponse aPostOperation(APostOperationRequest aPostOperationRequest) {

        StaxResponseHandler<APostOperationResponse> responseHandler = new StaxResponseHandler<APostOperationResponse>(
                new APostOperationResponseUnmarshaller());

        DefaultErrorResponseHandler errorResponseHandler = new DefaultErrorResponseHandler(exceptionUnmarshallers);

        return clientHandler.execute(
                new ClientExecutionParams<APostOperationRequest, APostOperationResponse>()
                        .withMarshaller(new APostOperationRequestMarshaller()).withResponseHandler(responseHandler)
                        .withErrorResponseHandler(errorResponseHandler).withInput(aPostOperationRequest));
    }

    @Override
    public APostOperationWithOutputResponse aPostOperationWithOutput(
            APostOperationWithOutputRequest aPostOperationWithOutputRequest) {

        StaxResponseHandler<APostOperationWithOutputResponse> responseHandler = new StaxResponseHandler<APostOperationWithOutputResponse>(
                new APostOperationWithOutputResponseUnmarshaller());

        DefaultErrorResponseHandler errorResponseHandler = new DefaultErrorResponseHandler(exceptionUnmarshallers);

        return clientHandler
                .execute(
                        new ClientExecutionParams<APostOperationWithOutputRequest, APostOperationWithOutputResponse>()
                                .withMarshaller(new APostOperationWithOutputRequestMarshaller())
                                .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                                .withInput(aPostOperationWithOutputRequest));
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

