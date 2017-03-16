${fileHeader}
package ${transformPackage};

import java.util.Map;
import java.util.Map.Entry;
import java.math.*;
import java.nio.ByteBuffer;
import javax.annotation.Generated;

import ${metadata.packageName}.model.*;
import software.amazon.awssdk.runtime.transform.SimpleTypeJsonUnmarshallers.*;
import software.amazon.awssdk.runtime.transform.*;

import com.fasterxml.jackson.core.JsonToken;
import static com.fasterxml.jackson.core.JsonToken.*;

/**
 * ${shape.shapeName} JSON Unmarshaller
 */
@Generated("software.amazon.awssdk:aws-java-sdk-code-generator")
public class ${shape.shapeName}Unmarshaller implements Unmarshaller<${shape.shapeName}, JsonUnmarshallerContext> {

    public ${shape.shapeName} unmarshall(JsonUnmarshallerContext context) throws Exception {
        ${shape.shapeName} ${shape.variable.variableName} = new ${shape.shapeName}();

<#if shape.hasHeaderMember >
        if (context.isStartOfDocument()) {
    <#list shape.members as memberModel>
        <#if memberModel.http.isHeader() >
            if (context.getHeader("${memberModel.http.unmarshallLocationName}") != null) {
                context.setCurrentHeader("${memberModel.http.unmarshallLocationName}");
                <#if memberModel.variable.simpleType == "Date">
                    ${shape.variable.variableName}.${memberModel.setterMethodName}(software.amazon.awssdk.util.DateUtils.parseRfc822Date(context.readText()));
                <#else>
                    ${shape.variable.variableName}.${memberModel.setterMethodName}(<@MemberUnmarshallerDeclarationMacro.content memberModel />.unmarshall(context));
                </#if>
            }
        </#if>
    </#list>
        }
</#if>

<#if shape.hasStatusCodeMember >
    <#list shape.members as memberModel>
        <#if memberModel.http.isStatusCode() >
        ${shape.variable.variableName}.${memberModel.setterMethodName}(context.getHttpResponse().getStatusCode());
        </#if>
    </#list>
</#if>

<#if shape.hasPayloadMember>
    <#assign explicitPayloadMember=shape.payloadMember />
    <#if explicitPayloadMember.http.isStreaming>
        ${shape.variable.variableName}.${explicitPayloadMember.setterMethodName}(context.getHttpResponse().getContent());
    <#elseif explicitPayloadMember.variable.variableType == "java.nio.ByteBuffer">
        java.io.InputStream is = context.getHttpResponse().getContent();
        if(is != null) {
            try {
                ${shape.variable.variableName}.${explicitPayloadMember.setterMethodName}(java.nio.ByteBuffer.wrap(software.amazon.awssdk.util.IoUtils.toByteArray(is)));
            } finally {
                software.amazon.awssdk.util.IoUtils.closeQuietly(is, null);
            }
        }
    <#else>
        <@PayloadUnmarshallerMacro.content shape />
     </#if>
<#elseif shape.unboundMembers?has_content>
    <@PayloadUnmarshallerMacro.content shape />
</#if>

        return ${shape.variable.variableName};
    }

    private static ${shape.shapeName}Unmarshaller INSTANCE;
    public static ${shape.shapeName}Unmarshaller getInstance() {
        if (INSTANCE == null) INSTANCE = new ${shape.shapeName}Unmarshaller();
        return INSTANCE;
    }
}
