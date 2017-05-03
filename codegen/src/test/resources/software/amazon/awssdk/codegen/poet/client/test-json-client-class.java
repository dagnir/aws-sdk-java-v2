package software.amazon.awssdk.services.acm;

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
import software.amazon.awssdk.services.acm.presign.AcmClientPresigners;
import software.amazon.awssdk.services.acm.transform.AddTagsToCertificateRequestMarshaller;
import software.amazon.awssdk.services.acm.transform.AddTagsToCertificateResultUnmarshaller;
import software.amazon.awssdk.services.acm.transform.DeleteCertificateRequestMarshaller;
import software.amazon.awssdk.services.acm.transform.DeleteCertificateResultUnmarshaller;
import software.amazon.awssdk.services.acm.transform.DescribeCertificateRequestMarshaller;
import software.amazon.awssdk.services.acm.transform.DescribeCertificateResultUnmarshaller;
import software.amazon.awssdk.services.acm.transform.GetCertificateRequestMarshaller;
import software.amazon.awssdk.services.acm.transform.GetCertificateResultUnmarshaller;
import software.amazon.awssdk.services.acm.transform.ImportCertificateRequestMarshaller;
import software.amazon.awssdk.services.acm.transform.ImportCertificateResultUnmarshaller;
import software.amazon.awssdk.services.acm.transform.ListCertificatesRequestMarshaller;
import software.amazon.awssdk.services.acm.transform.ListCertificatesResultUnmarshaller;
import software.amazon.awssdk.services.acm.transform.ListTagsForCertificateRequestMarshaller;
import software.amazon.awssdk.services.acm.transform.ListTagsForCertificateResultUnmarshaller;
import software.amazon.awssdk.services.acm.transform.RemoveTagsFromCertificateRequestMarshaller;
import software.amazon.awssdk.services.acm.transform.RemoveTagsFromCertificateResultUnmarshaller;
import software.amazon.awssdk.services.acm.transform.RequestCertificateRequestMarshaller;
import software.amazon.awssdk.services.acm.transform.RequestCertificateResultUnmarshaller;
import software.amazon.awssdk.services.acm.transform.ResendValidationEmailRequestMarshaller;
import software.amazon.awssdk.services.acm.transform.ResendValidationEmailResultUnmarshaller;

@Generated("software.amazon.awssdk:aws-java-sdk-code-generator")
public class DefaultACMClient implements ACMClient {
    private final ClientHandler clientHandler;

    private final SdkJsonProtocolFactory protocolFactory;

    private final AwsSyncClientParams clientParams;

    protected DefaultACMClient(AwsSyncClientParams clientParams) {
        this.clientHandler = new SdkClientHandler(new ClientHandlerParams().withClientParams(clientParams)
                .withCalculateCrc32FromCompressedDataEnabled(false));
        this.clientParams = clientParams;
        this.protocolFactory = init();
    }

    @Override
    public AddTagsToCertificateResult addTagsToCertificate(AddTagsToCertificateRequest addTagsToCertificateRequest) {

        HttpResponseHandler<AmazonWebServiceResponse<AddTagsToCertificateResult>> responseHandler = protocolFactory
                .createResponseHandler(new JsonOperationMetadata().withPayloadJson(true).withHasStreamingSuccessResponse(false),
                        new AddTagsToCertificateResultUnmarshaller());

        HttpResponseHandler<AmazonServiceException> errorResponseHandler = createErrorResponseHandler();

        return clientHandler.execute(
                new ClientExecutionParams<AddTagsToCertificateRequest, AmazonWebServiceResponse<AddTagsToCertificateResult>>()
                        .withMarshaller(new AddTagsToCertificateRequestMarshaller(protocolFactory))
                        .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                        .withInput(addTagsToCertificateRequest)).getResult();
    }

    @Override
    public DeleteCertificateResult deleteCertificate(DeleteCertificateRequest deleteCertificateRequest) {

        HttpResponseHandler<AmazonWebServiceResponse<DeleteCertificateResult>> responseHandler = protocolFactory
                .createResponseHandler(new JsonOperationMetadata().withPayloadJson(true).withHasStreamingSuccessResponse(false),
                        new DeleteCertificateResultUnmarshaller());

        HttpResponseHandler<AmazonServiceException> errorResponseHandler = createErrorResponseHandler();

        return clientHandler.execute(
                new ClientExecutionParams<DeleteCertificateRequest, AmazonWebServiceResponse<DeleteCertificateResult>>()
                        .withMarshaller(new DeleteCertificateRequestMarshaller(protocolFactory))
                        .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                        .withInput(deleteCertificateRequest)).getResult();
    }

    @Override
    public DescribeCertificateResult describeCertificate(DescribeCertificateRequest describeCertificateRequest) {

        HttpResponseHandler<AmazonWebServiceResponse<DescribeCertificateResult>> responseHandler = protocolFactory
                .createResponseHandler(new JsonOperationMetadata().withPayloadJson(true).withHasStreamingSuccessResponse(false),
                        new DescribeCertificateResultUnmarshaller());

        HttpResponseHandler<AmazonServiceException> errorResponseHandler = createErrorResponseHandler();

        return clientHandler.execute(
                new ClientExecutionParams<DescribeCertificateRequest, AmazonWebServiceResponse<DescribeCertificateResult>>()
                        .withMarshaller(new DescribeCertificateRequestMarshaller(protocolFactory))
                        .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                        .withInput(describeCertificateRequest)).getResult();
    }

    @Override
    public GetCertificateResult getCertificate(GetCertificateRequest getCertificateRequest) {

        HttpResponseHandler<AmazonWebServiceResponse<GetCertificateResult>> responseHandler = protocolFactory
                .createResponseHandler(new JsonOperationMetadata().withPayloadJson(true).withHasStreamingSuccessResponse(false),
                        new GetCertificateResultUnmarshaller());

        HttpResponseHandler<AmazonServiceException> errorResponseHandler = createErrorResponseHandler();

        return clientHandler.execute(
                new ClientExecutionParams<GetCertificateRequest, AmazonWebServiceResponse<GetCertificateResult>>()
                        .withMarshaller(new GetCertificateRequestMarshaller(protocolFactory))
                        .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                        .withInput(getCertificateRequest)).getResult();
    }

    @Override
    public ImportCertificateResult importCertificate(ImportCertificateRequest importCertificateRequest) {

        HttpResponseHandler<AmazonWebServiceResponse<ImportCertificateResult>> responseHandler = protocolFactory
                .createResponseHandler(new JsonOperationMetadata().withPayloadJson(true).withHasStreamingSuccessResponse(false),
                        new ImportCertificateResultUnmarshaller());

        HttpResponseHandler<AmazonServiceException> errorResponseHandler = createErrorResponseHandler();

        return clientHandler.execute(
                new ClientExecutionParams<ImportCertificateRequest, AmazonWebServiceResponse<ImportCertificateResult>>()
                        .withMarshaller(new ImportCertificateRequestMarshaller(protocolFactory))
                        .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                        .withInput(importCertificateRequest)).getResult();
    }

    @Override
    public ListCertificatesResult listCertificates(ListCertificatesRequest listCertificatesRequest) {

        HttpResponseHandler<AmazonWebServiceResponse<ListCertificatesResult>> responseHandler = protocolFactory
                .createResponseHandler(new JsonOperationMetadata().withPayloadJson(true).withHasStreamingSuccessResponse(false),
                        new ListCertificatesResultUnmarshaller());

        HttpResponseHandler<AmazonServiceException> errorResponseHandler = createErrorResponseHandler();

        return clientHandler.execute(
                new ClientExecutionParams<ListCertificatesRequest, AmazonWebServiceResponse<ListCertificatesResult>>()
                        .withMarshaller(new ListCertificatesRequestMarshaller(protocolFactory))
                        .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                        .withInput(listCertificatesRequest)).getResult();
    }

    @Override
    public ListTagsForCertificateResult listTagsForCertificate(ListTagsForCertificateRequest listTagsForCertificateRequest) {

        HttpResponseHandler<AmazonWebServiceResponse<ListTagsForCertificateResult>> responseHandler = protocolFactory
                .createResponseHandler(new JsonOperationMetadata().withPayloadJson(true).withHasStreamingSuccessResponse(false),
                        new ListTagsForCertificateResultUnmarshaller());

        HttpResponseHandler<AmazonServiceException> errorResponseHandler = createErrorResponseHandler();

        return clientHandler
                .execute(
                        new ClientExecutionParams<ListTagsForCertificateRequest, AmazonWebServiceResponse<ListTagsForCertificateResult>>()
                                .withMarshaller(new ListTagsForCertificateRequestMarshaller(protocolFactory))
                                .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                                .withInput(listTagsForCertificateRequest)).getResult();
    }

    @Override
    public RemoveTagsFromCertificateResult removeTagsFromCertificate(
            RemoveTagsFromCertificateRequest removeTagsFromCertificateRequest) {

        HttpResponseHandler<AmazonWebServiceResponse<RemoveTagsFromCertificateResult>> responseHandler = protocolFactory
                .createResponseHandler(new JsonOperationMetadata().withPayloadJson(true).withHasStreamingSuccessResponse(false),
                        new RemoveTagsFromCertificateResultUnmarshaller());

        HttpResponseHandler<AmazonServiceException> errorResponseHandler = createErrorResponseHandler();

        return clientHandler
                .execute(
                        new ClientExecutionParams<RemoveTagsFromCertificateRequest, AmazonWebServiceResponse<RemoveTagsFromCertificateResult>>()
                                .withMarshaller(new RemoveTagsFromCertificateRequestMarshaller(protocolFactory))
                                .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                                .withInput(removeTagsFromCertificateRequest)).getResult();
    }

    @Override
    public RequestCertificateResult requestCertificate(RequestCertificateRequest requestCertificateRequest) {

        HttpResponseHandler<AmazonWebServiceResponse<RequestCertificateResult>> responseHandler = protocolFactory
                .createResponseHandler(new JsonOperationMetadata().withPayloadJson(true).withHasStreamingSuccessResponse(false),
                        new RequestCertificateResultUnmarshaller());

        HttpResponseHandler<AmazonServiceException> errorResponseHandler = createErrorResponseHandler();

        return clientHandler.execute(
                new ClientExecutionParams<RequestCertificateRequest, AmazonWebServiceResponse<RequestCertificateResult>>()
                        .withMarshaller(new RequestCertificateRequestMarshaller(protocolFactory))
                        .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                        .withInput(requestCertificateRequest)).getResult();
    }

    @Override
    public ResendValidationEmailResult resendValidationEmail(ResendValidationEmailRequest resendValidationEmailRequest) {

        HttpResponseHandler<AmazonWebServiceResponse<ResendValidationEmailResult>> responseHandler = protocolFactory
                .createResponseHandler(new JsonOperationMetadata().withPayloadJson(true).withHasStreamingSuccessResponse(false),
                        new ResendValidationEmailResultUnmarshaller());

        HttpResponseHandler<AmazonServiceException> errorResponseHandler = createErrorResponseHandler();

        return clientHandler.execute(
                new ClientExecutionParams<ResendValidationEmailRequest, AmazonWebServiceResponse<ResendValidationEmailResult>>()
                        .withMarshaller(new ResendValidationEmailRequestMarshaller(protocolFactory))
                        .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                        .withInput(resendValidationEmailRequest)).getResult();
    }

    private HttpResponseHandler<AmazonServiceException> createErrorResponseHandler() {
        return protocolFactory.createErrorResponseHandler(new JsonErrorResponseMetadata());
    }

    private software.amazon.awssdk.protocol.json.SdkJsonProtocolFactory init() {
        return new SdkJsonProtocolFactory(new JsonClientMetadata()
                .withProtocolVersion("1.1")
                .withSupportsCbor(false)
                .withSupportsIon(false)
                .withBaseServiceExceptionClass(software.amazon.awssdk.services.acm.model.ACMException.class)
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
