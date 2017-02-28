package demo.xspec;


import static software.amazon.awssdk.services.dynamodbv2.xspec.ExpressionSpecBuilder.l;
import static software.amazon.awssdk.services.dynamodbv2.xspec.ExpressionSpecBuilder.n;
import static software.amazon.awssdk.services.dynamodbv2.xspec.ExpressionSpecBuilder.s;
import static software.amazon.awssdk.services.dynamodbv2.xspec.ExpressionSpecBuilder.ss;
import static software.amazon.awssdk.services.dynamodbv2.xspec.ExpressionSpecBuilder.attribute;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import software.amazon.awssdk.services.dynamodbv2.document.Item;
import software.amazon.awssdk.services.dynamodbv2.document.Table;
import software.amazon.awssdk.services.dynamodbv2.xspec.ExpressionSpecBuilder;
import software.amazon.awssdk.services.dynamodbv2.xspec.UpdateItemExpressionSpec;

public class DemoIntegrationTest extends DemoIntegrationTestBase {
    @Test
    public void example1() {
        // How to use expressions with document API.
        Table table = dynamo.getTable(RANGE_TABLE_NAME);

        // Combines all update expressions. generates a single name and value
        // map that can be used for request.
        UpdateItemExpressionSpec xspec = new ExpressionSpecBuilder()
                // SET num1 = num1 + 20
                .addUpdate(
                    n("num1").set(n("num1").plus(20)))
                // SET string-attr = "value"
                .addUpdate(
                    s("string-attr").set("string-value")
                )
                // num BETWEEN 0 AND 100
                .withCondition(
                    n("num2").between(0, 100)
                ).buildForUpdate();
        

        assertEquals("SET #0 = #0 + :0, #1 = :1", xspec.getUpdateExpression());
        assertEquals("#2 BETWEEN :2 AND :3", xspec.getConditionExpression());
        
        Map<String,String> nameMap = xspec.getNameMap();
        assertEquals(nameMap.get("#0"), "num1");
        assertEquals(nameMap.get("#1"), "string-attr");
        assertEquals(nameMap.get("#2"), "num2");

        Map<String,Object> valueMap = xspec.getValueMap();
        assertTrue(valueMap.get(":0").equals(20));
        assertTrue(valueMap.get(":1").equals("string-value"));
        assertTrue(valueMap.get(":2").equals(0));
        assertTrue(valueMap.get(":3").equals(100));

        table.updateItem(HASH_KEY_NAME, "hashKeyValue", RANGE_KEY_NAME, 0,
                xspec);
    }

    @Test
    public void addColors() {
        final String hashkey = "addColors";
        Table table = dynamo.getTable(RANGE_TABLE_NAME);
        table.deleteItem(HASH_KEY_NAME, hashkey, RANGE_KEY_NAME, 0);
        table.putItem(new Item()
            .withPrimaryKey(HASH_KEY_NAME, hashkey, RANGE_KEY_NAME, 0)
        );
        // Create a string set of 2 colors
        UpdateItemExpressionSpec xspec = new ExpressionSpecBuilder()
                .addUpdate(ss("Colors").set("red", "blue"))
                .buildForUpdate();

        table.updateItem(HASH_KEY_NAME, hashkey, RANGE_KEY_NAME, 0,
                xspec);
        Item item = table.getItem(HASH_KEY_NAME, hashkey, RANGE_KEY_NAME, 0);
        System.out.println(item);
        Set<String> colors = item.getStringSet("Colors");
        assertTrue(colors.size() == 2);

        // Append 2 more colors
        xspec = new ExpressionSpecBuilder()
            .addUpdate(ss("Colors").append("orange", "green"))
            .buildForUpdate();
        table.updateItem(HASH_KEY_NAME, hashkey, RANGE_KEY_NAME, 0,
                xspec);
        item = table.getItem(HASH_KEY_NAME, hashkey, RANGE_KEY_NAME, 0);
        System.out.println(item);
        colors = item.getStringSet("Colors");
        assertTrue(colors.size() == 4);
    }

    @Test
    public void addRemoveDeleteColors() {
        final String hashkey = "addRemoveDeleteColors";
        Table table = dynamo.getTable(RANGE_TABLE_NAME);
        table.deleteItem(HASH_KEY_NAME, hashkey, RANGE_KEY_NAME, 0);
        Map<String,Object> map = new HashMap<String,Object>();
        map.put("colors", Arrays.asList("black", "white", "grey")); // L
        map.put("members", Arrays.asList("john", "david")); // L
        map.put("countries", new HashSet<String>(Arrays.asList("us", "au"))); // SS
        map.put("brands", new HashSet<String>(Arrays.asList("Amazon", "Facebook", "Google", "LinkedIn"))); // L
        map.put("foo", null); // NULL
        table.putItem(new Item()
            .withPrimaryKey(HASH_KEY_NAME, hashkey, RANGE_KEY_NAME, 0)
            .withMap("mapAttr", map)
        );
        Item item = table.getItem(HASH_KEY_NAME, hashkey, RANGE_KEY_NAME, 0);
        System.out.println(item);
        assertTrue(item.getMap("mapAttr").containsKey("foo"));

        UpdateItemExpressionSpec xspec = new ExpressionSpecBuilder()
                // replace the first color in the color list with red
                .addUpdate(s("mapAttr.colors[0]").set("red"))
                // replace the second color with blue
                .addUpdate(s("mapAttr.colors[1]").set("blue"))
                // append a list (of two) to an existing list
                .addUpdate(l("mapAttr.members").set(
                        l("mapAttr.members").listAppend("marry", "liza")))
                // append two values to a string set
                .addUpdate(ss("mapAttr.countries").append("cn", "uk"))
                // delete two values from a string set
                .addUpdate(ss("mapAttr.brands").delete("Facebook", "LinkedIn"))
                // remove a (nested) attribute
                .addUpdate(attribute("mapAttr.foo").remove())
                .buildForUpdate();

        String c = xspec.getUpdateExpression();
        Map<String,String> nm = xspec.getNameMap();
        Map<String,Object> vm = xspec.getValueMap();
        System.out.println(c);
        System.out.println(nm);
        System.out.println(vm);
        
        assertEquals("SET #0.#1[0] = :0, #0.#1[1] = :1, #0.#2 = list_append(#0.#2, :2) ADD #0.#3 :3 DELETE #0.#4 :4 REMOVE #0.#5", c);

        table.updateItem(HASH_KEY_NAME, hashkey, RANGE_KEY_NAME, 0,
                xspec);
        
        item = table.getItem(HASH_KEY_NAME, hashkey, RANGE_KEY_NAME, 0);
        System.out.println(item);
        Map<String,Object> mapAttr = item.getMap("mapAttr");
        assertEquals(Arrays.asList("red", "blue", "grey"), mapAttr.get("colors"));
        assertEquals(Arrays.asList("john", "david", "marry", "liza"), mapAttr.get("members"));
        assertEquals(new HashSet<String>(Arrays.asList("us", "au", "cn", "uk")), mapAttr.get("countries"));
        assertEquals(new HashSet<String>(Arrays.asList("Amazon", "Google")), mapAttr.get("brands"));
        assertFalse(mapAttr.containsKey("foo"));
    }
}
