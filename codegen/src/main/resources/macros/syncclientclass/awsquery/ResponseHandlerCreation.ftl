<#macro content operationModel metadata unmarshallerReference outputType>
    <#if metadata.syncInterface == "AmazonSimpleDB">
        <#assign responseHandler="software.amazon.awssdk.services.simpledb.internal.SimpleDBStaxResponseHandler" />
    <#else>
        <#assign responseHandler="StaxResponseHandler" />
    </#if>

    StaxResponseHandler<${outputType}> responseHandler = new ${responseHandler}<${outputType}>(${unmarshallerReference});
</#macro>
