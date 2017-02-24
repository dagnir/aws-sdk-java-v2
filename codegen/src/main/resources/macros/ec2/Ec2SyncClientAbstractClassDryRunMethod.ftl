<#macro content serviceModelRoot>
    @Override
     public <X extends AmazonWebServiceRequest> DryRunResult<X> dryRun
     (DryRunSupportedRequest<X> request) throws AmazonServiceException,
     AmazonClientException {
     	throw new java.lang.UnsupportedOperationException();
     }
</#macro>
