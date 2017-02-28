<#macro content serviceModelRoot>
    /**
     * Checks whether you have the required permissions for the provided
     * ${serviceModelRoot.metadata.serviceAbbreviation} operation, without actually running it. The returned
     * DryRunResult object contains the information of whether the dry-run was
     * successful. This method will throw exception when the service response
     * does not clearly indicate whether you have the permission.
     *
     * @param request
     *            The request object for any ${serviceModelRoot.metadata.serviceAbbreviation} operation supported with
     *            dry-run.
     *
     * @return A DryRunResult object that contains the information of whether
     *         the dry-run was successful.
     *
     * @throws AmazonClientException
     *             If any internal errors are encountered inside the client
     *             while attempting to make the request or handle the response.
     *             Or if the service response does not clearly indicate whether
     *             you have the permission.
     * @throws AmazonServiceException
     *             If an error response is returned by ${serviceModelRoot.metadata.serviceAbbreviation} indicating
     *             either a problem with the data in the request, or a server
     *             side issue.
     */
    public <X extends AmazonWebServiceRequest> DryRunResult<X> dryRun(DryRunSupportedRequest<X> request) throws AmazonServiceException, AmazonClientException {
        Request<X> dryRunRequest = request.getDryRunRequest();
        ExecutionContext executionContext = createExecutionContext(dryRunRequest);
        try {
            invoke(dryRunRequest, new StaxResponseHandler<Void>(new VoidStaxUnmarshaller<Void>()), executionContext);
            throw new AmazonClientException("Unrecognized service response for the dry-run request.");
        } catch (AmazonServiceException ase) {
            if (ase.getErrorCode().equals("DryRunOperation") && ase.getStatusCode() == 412) {
                return new DryRunResult<X>(true, request, ase.getMessage(), ase);
            } else if (ase.getErrorCode().equals("UnauthorizedOperation") && ase.getStatusCode() == 403) {
                return new DryRunResult<X>(false, request, ase.getMessage(), ase);
            }
            throw new AmazonClientException("Unrecognized service response for the dry-run request.", ase);
        }
    }
</#macro>
