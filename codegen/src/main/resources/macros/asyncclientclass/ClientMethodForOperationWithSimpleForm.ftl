<#macro content operation>
<#if operation.simpleMethodForms??>
    /**
     * Simplified method form for invoking the ${operation.operationName}
     * operation.
     *
     * @see #${operation.methodName}Async(${operation.input.variableType})
     */
    @Override
    <#if operation.deprecated>
      @Deprecated
    </#if>
    public ${operation.asyncFutureType} ${operation.methodName}() {
        return ${operation.methodName}(new ${operation.input.variableType}());
    }
</#if>
</#macro>
