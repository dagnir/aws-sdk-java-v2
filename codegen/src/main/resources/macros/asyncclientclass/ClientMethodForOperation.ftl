<#macro content operation>

@Override
<#if operation.deprecated>
  @Deprecated
</#if>
public ${operation.asyncFutureType} ${operation.methodName}(final ${operation.input.variableType} request) {
    return CompletableFuture.supplyAsync(() -> syncClient.${operation.methodName}(request), executor);
}
</#macro>
