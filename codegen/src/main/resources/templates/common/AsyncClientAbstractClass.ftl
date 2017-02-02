${fileHeader}
package ${metadata.packageName};

import javax.annotation.Generated;

import ${metadata.packageName}.model.*;

/**
 * Abstract implementation of {@code ${metadata.asyncInterface}}. Convenient
 * method forms pass through to the corresponding overload that takes a
 * request object and an {@code AsyncHandler}, which throws an
 * {@code UnsupportedOperationException}.
 */
@Generated("software.amazon.awssdk:aws-java-sdk-code-generator")
public class ${metadata.asyncAbstractClass} extends ${metadata.syncAbstractClass}
        implements ${metadata.asyncInterface} {

    protected ${metadata.asyncAbstractClass}() {
    }
  <#list operations?values as operationModel>
    <@AsyncClientMethodForUnsupportedOperation.content operationModel />
    <@AsyncClientMethodForOperationWithSimpleForm.content operationModel />
  </#list>

  <#if AdditionalClientMethodsMacro?has_content>
    <@AdditionalClientMethodsMacro.content .data_model />
  </#if>
}
