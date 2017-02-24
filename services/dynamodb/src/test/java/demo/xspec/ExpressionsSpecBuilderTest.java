package demo.xspec;

import static software.amazon.awssdk.services.dynamodbv2.xspec.ExpressionSpecBuilder.l;
import static software.amazon.awssdk.services.dynamodbv2.xspec.ExpressionSpecBuilder.s;
import static software.amazon.awssdk.services.dynamodbv2.xspec.ExpressionSpecBuilder.ss;
import static software.amazon.awssdk.services.dynamodbv2.xspec.ExpressionSpecBuilder.attribute;
import static org.junit.Assert.assertEquals;

import java.util.Map;

import org.junit.Test;

import software.amazon.awssdk.services.dynamodbv2.xspec.ExpressionSpecBuilder;
import software.amazon.awssdk.services.dynamodbv2.xspec.UpdateItemExpressionSpec;

public class ExpressionsSpecBuilderTest {

    @Test
    public void test() {
        UpdateItemExpressionSpec xspec = new ExpressionSpecBuilder()
            .addUpdate(s("mapAttr.colors[0]").set("red"))
            .addUpdate(s("mapAttr.colors[1]").set("blue"))
            .addUpdate(l("mapAttr.members").set(
                    l("mapAttr.members").listAppend("marry", "liza")))
            .addUpdate(ss("mapAttr.countries").append("cn", "uk"))
            .addUpdate(ss("mapAttr.brands").delete("Facebook", "LinkedIn"))
            .addUpdate(attribute("mapAttr.foo").remove())
            .buildForUpdate();
        
        String expr = xspec.getUpdateExpression();
        Map<String,String> nm = xspec.getNameMap();
        Map<String,Object> vm = xspec.getValueMap();
        System.out.println(expr);
        System.out.println(nm);
        System.out.println(vm);

        assertEquals("SET #0.#1[0] = :0, #0.#1[1] = :1, #0.#2 = list_append(#0.#2, :2) ADD #0.#3 :3 DELETE #0.#4 :4 REMOVE #0.#5", expr);
    }

}
