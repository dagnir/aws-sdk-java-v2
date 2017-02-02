${fileHeader}
package ${metadata.packageName};

import javax.annotation.Generated;

import ${metadata.packageName}.model.*;
import software.amazon.awssdk.*;
<#if hasWaiters>
import ${metadata.packageName}.waiters.${metadata.syncInterface}Waiters;
</#if>

/**
 * Abstract implementation of {@code ${metadata.syncInterface}}. Convenient
 * method forms pass through to the corresponding overload that takes a
 * request object, which throws an {@code UnsupportedOperationException}.
 */
@Generated("software.amazon.awssdk:aws-java-sdk-code-generator")
public class ${metadata.syncAbstractClass} implements ${metadata.syncInterface} {

    protected ${metadata.syncAbstractClass}() {
    }

    @Override
    public void setEndpoint(String endpoint) {
        throw new java.lang.UnsupportedOperationException();
    }

    @Override
    public void setRegion(software.amazon.awssdk.regions.Region region) {
        throw new java.lang.UnsupportedOperationException();
    }

  <#list operations?values as operationModel>
    <@ClientMethodForUnsupportedOperation.content operationModel />
    <@ClientMethodForOperationWithSimpleForm.content operationModel/>
  </#list>

  <#if AdditionalClientMethodsMacro?has_content>
    <@AdditionalClientMethodsMacro.content .data_model />
  </#if>

  <#if customizationConfig.skipInterfaceAdditions == false>
    @Override
    public void shutdown() {
        throw new java.lang.UnsupportedOperationException();
    }

    <#assign responseMetadataClassName=customizationConfig.customResponseMetadataClassName!"software.amazon.awssdk.ResponseMetadata" />
    @Override
    public ${responseMetadataClassName} getCachedResponseMetadata(software.amazon.awssdk.AmazonWebServiceRequest request) {
        throw new java.lang.UnsupportedOperationException();
    }
  </#if>

  <#if hasWaiters>
      @Override
      public ${metadata.syncInterface}Waiters waiters() {
           throw new java.lang.UnsupportedOperationException();
      }
  </#if>

    <#if customizationConfig.presignersFqcn??>
    @Override
    public ${customizationConfig.presignersFqcn} presigners() {
        throw new java.lang.UnsupportedOperationException();
    }
    </#if>
}
