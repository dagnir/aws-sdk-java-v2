<#macro content operation>

@Override
<#if operation.deprecated>
  @Deprecated
</#if>
public ${operation.asyncFutureType} ${operation.methodName}(
        ${operation.input.variableType} request) {

    throw new java.lang.UnsupportedOperationException();
}
</#macro>
