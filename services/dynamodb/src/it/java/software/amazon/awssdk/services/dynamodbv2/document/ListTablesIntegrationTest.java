package software.amazon.awssdk.services.dynamodbv2.document;

import org.junit.Test;

public class ListTablesIntegrationTest extends IntegrationTestBase {

    @Test
    public void testListZeroTables() {
        TableCollection<?> col = dynamo.listTables(0);
        int count = 0;
        for (Table t : col) {
            count++;
            System.out.println(t);
        }
        assertTrue(0, count);
    }

    @Test
    public void testListOneTable() {
        TableCollection<?> col = dynamo.listTables(1);
        int count = 0;
        for (Table t : col) {
            count++;
            System.out.println(t);
        }
        assertTrue(1, count);
    }

    @Test
    public void testListGTOne() {
        TableCollection<?> col = dynamo.listTables();
        int count = 0;
        for (Table t : col) {
            count++;
            System.out.println(t);
        }
        assertGT(count, 1);
    }

    @Test
    public void testListGTOne_Old() {
        TableCollection<?> col = dynamoOld.listTables();
        int count = 0;
        for (Table t : col) {
            count++;
            System.out.println(t);
        }
        assertGT(count, 1);
    }

}
