<#macro content shape>
<#if shape.members?has_content>
<#list shape.members as member>
    <#local variable = member.variable/>
    <#if variable.documentation?has_content>/** ${variable.documentation} */</#if>
    <#if member.deprecated>
    @Deprecated
    </#if>
    private ${variable.variableDeclarationType} ${variable.variableName};
    <#if member.http?has_content && member.http.isStreaming>
    <#if member.c2jShape != "BlobStream">
    private java.io.InputStream ${variable.variableName}InputStream;
    </#if>
    private java.io.File ${variable.variableName}File;
    </#if>
</#list>
</#if>
</#macro>
