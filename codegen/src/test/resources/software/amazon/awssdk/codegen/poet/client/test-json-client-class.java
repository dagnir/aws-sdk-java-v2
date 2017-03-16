package software.amazon.awssdk.services.acm;

import javax.annotation.Generated;
import software.amazon.awssdk.AmazonServiceException;
import software.amazon.awssdk.AmazonWebServiceResponse;
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
import software.amazon.awssdk.services.acm.model.AddTagsToCertificateRequest;
import software.amazon.awssdk.services.acm.model.AddTagsToCertificateResult;
import software.amazon.awssdk.services.acm.model.DeleteCertificateRequest;
import software.amazon.awssdk.services.acm.model.DeleteCertificateResult;
import software.amazon.awssdk.services.acm.model.DescribeCertificateRequest;
import software.amazon.awssdk.services.acm.model.DescribeCertificateResult;
import software.amazon.awssdk.services.acm.model.GetCertificateRequest;
import software.amazon.awssdk.services.acm.model.GetCertificateResult;
import software.amazon.awssdk.services.acm.model.ImportCertificateRequest;
import software.amazon.awssdk.services.acm.model.ImportCertificateResult;
import software.amazon.awssdk.services.acm.model.InvalidArnException;
import software.amazon.awssdk.services.acm.model.InvalidDomainValidationOptionsException;
import software.amazon.awssdk.services.acm.model.InvalidStateException;
import software.amazon.awssdk.services.acm.model.InvalidTagException;
import software.amazon.awssdk.services.acm.model.LimitExceededException;
import software.amazon.awssdk.services.acm.model.ListCertificatesRequest;
import software.amazon.awssdk.services.acm.model.ListCertificatesResult;
import software.amazon.awssdk.services.acm.model.ListTagsForCertificateRequest;
import software.amazon.awssdk.services.acm.model.ListTagsForCertificateResult;
import software.amazon.awssdk.services.acm.model.RemoveTagsFromCertificateRequest;
import software.amazon.awssdk.services.acm.model.RemoveTagsFromCertificateResult;
import software.amazon.awssdk.services.acm.model.RequestCertificateRequest;
import software.amazon.awssdk.services.acm.model.RequestCertificateResult;
import software.amazon.awssdk.services.acm.model.RequestInProgressException;
import software.amazon.awssdk.services.acm.model.ResendValidationEmailRequest;
import software.amazon.awssdk.services.acm.model.ResendValidationEmailResult;
import software.amazon.awssdk.services.acm.model.ResourceInUseException;
import software.amazon.awssdk.services.acm.model.ResourceNotFoundException;
import software.amazon.awssdk.services.acm.model.TooManyTagsException;
import software.amazon.awssdk.services.acm.model.transform.AddTagsToCertificateRequestMarshaller;
import software.amazon.awssdk.services.acm.model.transform.AddTagsToCertificateResultUnmarshaller;
import software.amazon.awssdk.services.acm.model.transform.DeleteCertificateRequestMarshaller;
import software.amazon.awssdk.services.acm.model.transform.DeleteCertificateResultUnmarshaller;
import software.amazon.awssdk.services.acm.model.transform.DescribeCertificateRequestMarshaller;
import software.amazon.awssdk.services.acm.model.transform.DescribeCertificateResultUnmarshaller;
import software.amazon.awssdk.services.acm.model.transform.GetCertificateRequestMarshaller;
import software.amazon.awssdk.services.acm.model.transform.GetCertificateResultUnmarshaller;
import software.amazon.awssdk.services.acm.model.transform.ImportCertificateRequestMarshaller;
import software.amazon.awssdk.services.acm.model.transform.ImportCertificateResultUnmarshaller;
import software.amazon.awssdk.services.acm.model.transform.ListCertificatesRequestMarshaller;
import software.amazon.awssdk.services.acm.model.transform.ListCertificatesResultUnmarshaller;
import software.amazon.awssdk.services.acm.model.transform.ListTagsForCertificateRequestMarshaller;
import software.amazon.awssdk.services.acm.model.transform.ListTagsForCertificateResultUnmarshaller;
import software.amazon.awssdk.services.acm.model.transform.RemoveTagsFromCertificateRequestMarshaller;
import software.amazon.awssdk.services.acm.model.transform.RemoveTagsFromCertificateResultUnmarshaller;
import software.amazon.awssdk.services.acm.model.transform.RequestCertificateRequestMarshaller;
import software.amazon.awssdk.services.acm.model.transform.RequestCertificateResultUnmarshaller;
import software.amazon.awssdk.services.acm.model.transform.ResendValidationEmailRequestMarshaller;
import software.amazon.awssdk.services.acm.model.transform.ResendValidationEmailResultUnmarshaller;

@Generated("software.amazon.awssdk:aws-java-sdk-code-generator")
public class DefaultAcmClient implements AcmClient, AutoCloseable {
    private final ClientHandler clientHandler;

    private final SdkJsonProtocolFactory protocolFactory;

    protected DefaultAcmClient(AwsSyncClientParams clientParams) {
        this.clientHandler = new SdkClientHandler(new ClientHandlerParams().withClientParams(clientParams));
        this.protocolFactory = init();
    }

    @Override
    public AddTagsToCertificateResult addTagsToCertificate(AddTagsToCertificateRequest addTagsToCertificateRequest) {

        HttpResponseHandler<AmazonWebServiceResponse<AddTagsToCertificateResult>> responseHandler = protocolFactory
                .createResponseHandler(new JsonOperationMetadata().withPayloadJson(true).withHasStreamingSuccessResponse(false),
                        new AddTagsToCertificateResultUnmarshaller());

        HttpResponseHandler<AmazonServiceException> errorResponseHandler = createErrorResponseHandler();

        return clientHandler.execute(new ClientExecutionParams<AddTagsToCertificateRequest, AddTagsToCertificateResult>()
                .withMarshaller(new AddTagsToCertificateRequestMarshaller(protocolFactory)).withResponseHandler(responseHandler)
                .withErrorResponseHandler(errorResponseHandler).withInput(addTagsToCertificateRequest));
    }

    @Override
    public DeleteCertificateResult deleteCertificate(DeleteCertificateRequest deleteCertificateRequest) {

        HttpResponseHandler<AmazonWebServiceResponse<DeleteCertificateResult>> responseHandler = protocolFactory
                .createResponseHandler(new JsonOperationMetadata().withPayloadJson(true).withHasStreamingSuccessResponse(false),
                        new DeleteCertificateResultUnmarshaller());

        HttpResponseHandler<AmazonServiceException> errorResponseHandler = createErrorResponseHandler();

        return clientHandler.execute(new ClientExecutionParams<DeleteCertificateRequest, DeleteCertificateResult>()
                .withMarshaller(new DeleteCertificateRequestMarshaller(protocolFactory)).withResponseHandler(responseHandler)
                .withErrorResponseHandler(errorResponseHandler).withInput(deleteCertificateRequest));
    }

    @Override
    public DescribeCertificateResult describeCertificate(DescribeCertificateRequest describeCertificateRequest) {

        HttpResponseHandler<AmazonWebServiceResponse<DescribeCertificateResult>> responseHandler = protocolFactory
                .createResponseHandler(new JsonOperationMetadata().withPayloadJson(true).withHasStreamingSuccessResponse(false),
                        new DescribeCertificateResultUnmarshaller());

        HttpResponseHandler<AmazonServiceException> errorResponseHandler = createErrorResponseHandler();

        return clientHandler.execute(new ClientExecutionParams<DescribeCertificateRequest, DescribeCertificateResult>()
                .withMarshaller(new DescribeCertificateRequestMarshaller(protocolFactory)).withResponseHandler(responseHandler)
                .withErrorResponseHandler(errorResponseHandler).withInput(describeCertificateRequest));
    }

    @Override
    public GetCertificateResult getCertificate(GetCertificateRequest getCertificateRequest) {

        HttpResponseHandler<AmazonWebServiceResponse<GetCertificateResult>> responseHandler = protocolFactory
                .createResponseHandler(new JsonOperationMetadata().withPayloadJson(true).withHasStreamingSuccessResponse(false),
                        new GetCertificateResultUnmarshaller());

        HttpResponseHandler<AmazonServiceException> errorResponseHandler = createErrorResponseHandler();

        return clientHandler.execute(new ClientExecutionParams<GetCertificateRequest, GetCertificateResult>()
                .withMarshaller(new GetCertificateRequestMarshaller(protocolFactory)).withResponseHandler(responseHandler)
                .withErrorResponseHandler(errorResponseHandler).withInput(getCertificateRequest));
    }

    @Override
    public ImportCertificateResult importCertificate(ImportCertificateRequest importCertificateRequest) {

        HttpResponseHandler<AmazonWebServiceResponse<ImportCertificateResult>> responseHandler = protocolFactory
                .createResponseHandler(new JsonOperationMetadata().withPayloadJson(true).withHasStreamingSuccessResponse(false),
                        new ImportCertificateResultUnmarshaller());

        HttpResponseHandler<AmazonServiceException> errorResponseHandler = createErrorResponseHandler();

        return clientHandler.execute(new ClientExecutionParams<ImportCertificateRequest, ImportCertificateResult>()
                .withMarshaller(new ImportCertificateRequestMarshaller(protocolFactory)).withResponseHandler(responseHandler)
                .withErrorResponseHandler(errorResponseHandler).withInput(importCertificateRequest));
    }

    @Override
    public ListCertificatesResult listCertificates(ListCertificatesRequest listCertificatesRequest) {

        HttpResponseHandler<AmazonWebServiceResponse<ListCertificatesResult>> responseHandler = protocolFactory
                .createResponseHandler(new JsonOperationMetadata().withPayloadJson(true).withHasStreamingSuccessResponse(false),
                        new ListCertificatesResultUnmarshaller());

        HttpResponseHandler<AmazonServiceException> errorResponseHandler = createErrorResponseHandler();

        return clientHandler.execute(new ClientExecutionParams<ListCertificatesRequest, ListCertificatesResult>()
                .withMarshaller(new ListCertificatesRequestMarshaller(protocolFactory)).withResponseHandler(responseHandler)
                .withErrorResponseHandler(errorResponseHandler).withInput(listCertificatesRequest));
    }

    @Override
    public ListTagsForCertificateResult listTagsForCertificate(ListTagsForCertificateRequest listTagsForCertificateRequest) {

        HttpResponseHandler<AmazonWebServiceResponse<ListTagsForCertificateResult>> responseHandler = protocolFactory
                .createResponseHandler(new JsonOperationMetadata().withPayloadJson(true).withHasStreamingSuccessResponse(false),
                        new ListTagsForCertificateResultUnmarshaller());

        HttpResponseHandler<AmazonServiceException> errorResponseHandler = createErrorResponseHandler();

        return clientHandler.execute(new ClientExecutionParams<ListTagsForCertificateRequest, ListTagsForCertificateResult>()
                .withMarshaller(new ListTagsForCertificateRequestMarshaller(protocolFactory))
                .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                .withInput(listTagsForCertificateRequest));
    }

    @Override
    public RemoveTagsFromCertificateResult removeTagsFromCertificate(
            RemoveTagsFromCertificateRequest removeTagsFromCertificateRequest) {

        HttpResponseHandler<AmazonWebServiceResponse<RemoveTagsFromCertificateResult>> responseHandler = protocolFactory
                .createResponseHandler(new JsonOperationMetadata().withPayloadJson(true).withHasStreamingSuccessResponse(false),
                        new RemoveTagsFromCertificateResultUnmarshaller());

        HttpResponseHandler<AmazonServiceException> errorResponseHandler = createErrorResponseHandler();

        return clientHandler
                .execute(new ClientExecutionParams<RemoveTagsFromCertificateRequest, RemoveTagsFromCertificateResult>()
                        .withMarshaller(new RemoveTagsFromCertificateRequestMarshaller(protocolFactory))
                        .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                        .withInput(removeTagsFromCertificateRequest));
    }

    @Override
    public RequestCertificateResult requestCertificate(RequestCertificateRequest requestCertificateRequest) {

        HttpResponseHandler<AmazonWebServiceResponse<RequestCertificateResult>> responseHandler = protocolFactory
                .createResponseHandler(new JsonOperationMetadata().withPayloadJson(true).withHasStreamingSuccessResponse(false),
                        new RequestCertificateResultUnmarshaller());

        HttpResponseHandler<AmazonServiceException> errorResponseHandler = createErrorResponseHandler();

        return clientHandler.execute(new ClientExecutionParams<RequestCertificateRequest, RequestCertificateResult>()
                .withMarshaller(new RequestCertificateRequestMarshaller(protocolFactory)).withResponseHandler(responseHandler)
                .withErrorResponseHandler(errorResponseHandler).withInput(requestCertificateRequest));
    }

    @Override
    public ResendValidationEmailResult resendValidationEmail(ResendValidationEmailRequest resendValidationEmailRequest) {

        HttpResponseHandler<AmazonWebServiceResponse<ResendValidationEmailResult>> responseHandler = protocolFactory
                .createResponseHandler(new JsonOperationMetadata().withPayloadJson(true).withHasStreamingSuccessResponse(false),
                        new ResendValidationEmailResultUnmarshaller());

        HttpResponseHandler<AmazonServiceException> errorResponseHandler = createErrorResponseHandler();

        return clientHandler.execute(new ClientExecutionParams<ResendValidationEmailRequest, ResendValidationEmailResult>()
                .withMarshaller(new ResendValidationEmailRequestMarshaller(protocolFactory)).withResponseHandler(responseHandler)
                .withErrorResponseHandler(errorResponseHandler).withInput(resendValidationEmailRequest));
    }

    private HttpResponseHandler<AmazonServiceException> createErrorResponseHandler() {
        return protocolFactory.createErrorResponseHandler(new JsonErrorResponseMetadata());
    }

    private software.amazon.awssdk.protocol.json.SdkJsonProtocolFactory init() {
        return new SdkJsonProtocolFactory(new JsonClientMetadata()
                .withProtocolVersion("1.1")
                .withSupportsCbor(false)
                .withSupportsIon(false)
                .withBaseServiceExceptionClass(software.amazon.awssdk.services.acm.model.AcmClientException.class)
                .addErrorMetadata(
                        new JsonErrorShapeMetadata().withErrorCode("InvalidTagException").withModeledClass(
                                InvalidTagException.class))
                .addErrorMetadata(
                        new JsonErrorShapeMetadata().withErrorCode("TooManyTagsException").withModeledClass(
                                TooManyTagsException.class))
                .addErrorMetadata(
                        new JsonErrorShapeMetadata().withErrorCode("ResourceInUseException").withModeledClass(
                                ResourceInUseException.class))
                .addErrorMetadata(
                        new JsonErrorShapeMetadata().withErrorCode("ResourceNotFoundException").withModeledClass(
                                ResourceNotFoundException.class))
                .addErrorMetadata(
                        new JsonErrorShapeMetadata().withErrorCode("RequestInProgressException").withModeledClass(
                                RequestInProgressException.class))
                .addErrorMetadata(
                        new JsonErrorShapeMetadata().withErrorCode("InvalidStateException").withModeledClass(
                                InvalidStateException.class))
                .addErrorMetadata(
                        new JsonErrorShapeMetadata().withErrorCode("InvalidDomainValidationOptionsException").withModeledClass(
                                InvalidDomainValidationOptionsException.class))
                .addErrorMetadata(
                        new JsonErrorShapeMetadata().withErrorCode("InvalidArnException").withModeledClass(
                                InvalidArnException.class))
                .addErrorMetadata(
                        new JsonErrorShapeMetadata().withErrorCode("LimitExceededException").withModeledClass(
                                LimitExceededException.class)));
    }

    @Override
    public void close() {
        clientHandler.shutdown();
    }
}