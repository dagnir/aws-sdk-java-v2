<#macro content dataModel>
<#local shapes = dataModel.shapes/>
<#local metadata = dataModel.metadata/>
<#local shapeName = dataModel.shapeName/>
<#local customConfig = dataModel.customConfig/>

${dataModel.fileHeader}
package ${dataModel.transformPackage};

import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.Map;
import java.util.List;
import java.util.regex.Pattern;
import javax.annotation.Generated;

import software.amazon.awssdk.SdkClientException;
import software.amazon.awssdk.Request;
import software.amazon.awssdk.DefaultRequest;
import software.amazon.awssdk.http.HttpMethodName;
import ${metadata.packageName}.model.*;
import software.amazon.awssdk.runtime.transform.Marshaller;
import software.amazon.awssdk.util.BinaryUtils;
import software.amazon.awssdk.util.StringUtils;
import software.amazon.awssdk.util.IdempotentUtils;
import software.amazon.awssdk.util.StringInputStream;
import software.amazon.awssdk.protocol.json.*;



/**
 * ${shapeName} Marshaller
 */
@Generated("software.amazon.awssdk:aws-java-sdk-code-generator")
public class ${shapeName}Marshaller implements Marshaller<Request<${shapeName}>, ${shapeName}> {

    private final SdkJsonMarshallerFactory protocolFactory;

    public ${shapeName}Marshaller(SdkJsonMarshallerFactory protocolFactory) {
        this.protocolFactory = protocolFactory;
    }

    <#assign shape = shapes[shapeName]/>
    public Request<${shapeName}> marshall(${shape.variable.variableType} ${shape.variable.variableName}) {

        if (${shape.variable.variableName} == null) {
            throw new SdkClientException("Invalid argument passed to marshall(...)");
        }

        <@RequiredParameterValidationInvocationMacro.content dataModel.customConfig shape/>

        <#assign serviceNameForRequest = customConfig.customServiceNameForRequest!metadata.syncInterface />

        <@DefaultRequestCreation.content shape serviceNameForRequest/>
        request.addHeader("X-Amz-Target", "${shape.marshaller.target}");

        <#assign httpVerb = (shape.marshaller.verb)!POST/>
        request.setHttpMethod(HttpMethodName.${httpVerb});

        request.setResourcePath("");

        try {
          final StructuredJsonGenerator jsonGenerator = protocolFactory.createGenerator();

          jsonGenerator.writeStartObject();

          <@MemberMarshallerMacro.content customConfig shapeName shape.variable.variableName shapes/>

          jsonGenerator.writeEndObject();

          byte[] content = jsonGenerator.getBytes();
          request.setContent(new ByteArrayInputStream(content));
          request.addHeader("Content-Length", Integer.toString(content.length));
          request.addHeader("Content-Type", protocolFactory.getContentType());
        } catch(Throwable t) {
            throw new SdkClientException("Unable to marshall request to JSON: " + t.getMessage(), t);
        }

        return request;
    }

    <@RequiredParameterValidationFunctionMacro.content dataModel.customConfig shape/>
}

</#macro>
