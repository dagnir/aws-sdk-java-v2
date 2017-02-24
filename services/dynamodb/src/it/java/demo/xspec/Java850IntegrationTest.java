package demo.xspec;

import static software.amazon.awssdk.services.dynamodbv2.xspec.ExpressionSpecBuilder.N;
import static software.amazon.awssdk.services.dynamodbv2.xspec.ExpressionSpecBuilder.S;
import static software.amazon.awssdk.services.dynamodbv2.xspec.ExpressionSpecBuilder.attribute_not_exists;
import static software.amazon.awssdk.services.dynamodbv2.xspec.ExpressionSpecBuilder.parenthesize;

import org.junit.Test;

import software.amazon.awssdk.services.dynamodbv2.document.Table;
import software.amazon.awssdk.services.dynamodbv2.xspec.ExpressionSpecBuilder;
import software.amazon.awssdk.services.dynamodbv2.xspec.UpdateItemExpressionSpec;

// https://issues.amazon.com/JAVA-850
public class Java850IntegrationTest extends DemoIntegrationTestBase {
    /**
     * <pre>
     *  (attribute_not_exists(item_version) AND attribute_not_exists(config_id) AND attribute_not_exists(config_version)) OR
     *  (item_version < 123) OR
     *  (item_version = 123 AND config_id < 456) OR
     *  (item_version = 123 AND config_id = 456 AND config_version < 999)
     * </pre>
     */
    @Test
    public void example850() {
        // How to use expressions with document API.
        Table table = dynamo.getTable(RANGE_TABLE_NAME);

        // Combines all update expressions. generates a single name and value
        // map that can be used for request.
        UpdateItemExpressionSpec xspec = new ExpressionSpecBuilder()
                // SET num1 = num1 + 20
                .addUpdate(
                    N("num1").set(N("num1").plus(20)))
                // SET string-attr = "value"
                .addUpdate(
                    S("string-attr").set("string-value")
                )
                // num BETWEEN 0 AND 100
                .withCondition(
                    // add explicit parenthesis
                    parenthesize( attribute_not_exists("item_version")
                        .and( attribute_not_exists("config_id") )
                        .and( attribute_not_exists("config_version") )
                    ).or( N("item_version").lt(123) )
                     .or( N("item_version").eq(123)
                         .and( N("config_id").lt(456) ) )
                     .or( N("item_version").eq(123)
                         .and( N("config_id").eq(456) )
                         .and( N("config_version").lt(999) ))
                ).buildForUpdate();
        

        table.updateItem(HASH_KEY_NAME, "hashKeyValue", RANGE_KEY_NAME, 0,
                xspec);
    }
}
