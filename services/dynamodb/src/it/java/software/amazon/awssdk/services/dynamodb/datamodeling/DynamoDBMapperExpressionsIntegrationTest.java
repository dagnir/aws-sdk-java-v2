/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.services.dynamodb.datamodeling;

import static org.junit.Assert.assertTrue;

import java.io.FileNotFoundException;
import java.io.IOException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.services.dynamodb.DynamoDBClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ComparisonOperator;
import software.amazon.awssdk.services.dynamodb.model.Condition;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DeleteTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableResult;
import software.amazon.awssdk.services.dynamodb.model.ExpectedAttributeValue;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.ResourceInUseException;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;
import software.amazon.awssdk.test.AwsTestBase;
import software.amazon.awssdk.util.ImmutableMapParameter;
import software.amazon.awssdk.util.ImmutableMapParameter.Builder;

public class DynamoDBMapperExpressionsIntegrationTest extends AwsTestBase {

    /**
     * Reference to the mapper used for this testing
     */
    protected static DynamoDBMapper mapper;

    /**
     * Reference to the client being used by the mapper.
     */
    protected static DynamoDBClient client;

    /**
     * Table name to be used for this testing
     */
    static final String TABLENAME = "java-sdk-mapper-customer";

    /**
     * Attribute name of the hash key
     */
    private static final String HASH_KEY = "customerId";

    /**
     * Attribute name of the range key
     */
    private static final String RANGE_KEY = "addressType";

    /**
     * Status of the table
     */
    private static final String TABLE_STATUS_ACTIVE = "ACTIVE";

    /**
     * Sleep time in milli seconds for the table to become active.
     */
    private static final long SLEEP_TIME_IN_MILLIS = 5000;

    /**
     * Provisioned Throughput read capacity for the table.
     */
    private static final long READ_CAPACITY = 10;

    /**
     * Provisioned Throughput write capacity for the table.
     */
    private static final long WRITE_CAPACITY = 10;

    private static final String FIRST_CUSTOMER_ID = "1000";
    private static final String ADDRESS_TYPE_HOME = "home";
    private static final String ADDRESS_TYPE_WORK = "work";

    @BeforeClass
    public static void setUp() throws FileNotFoundException, IOException,
                                      InterruptedException {
        setUpCredentials();
        client = DynamoDBClient.builder().credentialsProvider(CREDENTIALS_PROVIDER_CHAIN).build();
        mapper = new DynamoDBMapper(client);
        try {
            client.createTable(CreateTableRequest.builder_()
                                       .tableName(TABLENAME)
                                       .keySchema(KeySchemaElement.builder_().attributeName(HASH_KEY).keyType(KeyType.HASH).build_(),
                                                      KeySchemaElement.builder_().attributeName(RANGE_KEY).keyType(KeyType.RANGE).build_())
                                       .attributeDefinitions(
                                               AttributeDefinition.builder_().attributeName(HASH_KEY).attributeType(ScalarAttributeType.N).build_(),
                                               AttributeDefinition.builder_().attributeName(RANGE_KEY).attributeType(ScalarAttributeType.S).build_())
                                       .provisionedThroughput(ProvisionedThroughput.builder_()
                                               .readCapacityUnits(READ_CAPACITY)
                                               .writeCapacityUnits(WRITE_CAPACITY)
                                               .build_())
                    .build_());
        } catch (ResourceInUseException ex) {
            ex.printStackTrace();
        }
        waitForTableCreation();
        fillInData();
    }

    public static void fillInData() {
        final Builder<String, AttributeValue> record1 = ImmutableMapParameter
                .builder();
        record1.put(HASH_KEY, AttributeValue.builder_().n(FIRST_CUSTOMER_ID).build_())
               .put(RANGE_KEY, AttributeValue.builder_().s(ADDRESS_TYPE_WORK).build_())
               .put("AddressLine1",
                    AttributeValue.builder_().s("1918 8th Aven").build_())
               .put("city", AttributeValue.builder_().s("seattle").build_())
               .put("state", AttributeValue.builder_().s("WA").build_())
               .put("zipcode", AttributeValue.builder_().n("98104").build_());
        final Builder<String, AttributeValue> record2 = ImmutableMapParameter
                .builder();
        record2.put(HASH_KEY, AttributeValue.builder_().n(FIRST_CUSTOMER_ID).build_())
               .put(RANGE_KEY, AttributeValue.builder_().s(ADDRESS_TYPE_HOME).build_())
               .put("AddressLine1",
                    AttributeValue.builder_().s("15606 NE 40th ST").build_())
               .put("city", AttributeValue.builder_().s("redmond").build_())
               .put("state", AttributeValue.builder_().s("WA").build_())
               .put("zipcode", AttributeValue.builder_().n("98052").build_());

        client.putItem(PutItemRequest.builder_().tableName(TABLENAME).item(record1.build()).build_());
        client.putItem(PutItemRequest.builder_().tableName(TABLENAME).item(record2.build()).build_());
    }

    public static void waitForTableCreation() throws InterruptedException {
        while (true) {
            DescribeTableResult describeResult = client
                    .describeTable(DescribeTableRequest.builder_().tableName(TABLENAME).build_());
            if (TABLE_STATUS_ACTIVE.equals(describeResult.table()
                                                         .tableStatus())) {
                break;
            }
            Thread.sleep(SLEEP_TIME_IN_MILLIS);
        }
    }

    @AfterClass
    public static void tearDown() throws Exception {
        try {
            if (client != null) {
                client.deleteTable(DeleteTableRequest.builder_().tableName(TABLENAME).build_());
            }
        } catch (Exception e) {
            // Ignored or expected.
        } finally {
            if (client != null) {
                client.close();
            }
        }
    }

    /**
     * Queries for a record based on hash and range key. Provider a filter
     * expression that filters results.
     */
    @Test
    public void testQueryFilterExpression() {
        Customer customer = new Customer();
        customer.setCustomerId(Long.valueOf(FIRST_CUSTOMER_ID));

        DynamoDBQueryExpression<Customer> queryExpression =
                new DynamoDBQueryExpression<Customer>()
                        .withHashKeyValues(customer)
                        .withRangeKeyCondition(RANGE_KEY, Condition.builder_()
                                .comparisonOperator(ComparisonOperator.EQ)
                                .attributeValueList(AttributeValue.builder_().s(ADDRESS_TYPE_HOME).build_()).build_());
        PaginatedQueryList<Customer> results = mapper.query(Customer.class,
                                                            queryExpression);
        assertTrue(results.size() == 1);

        final Builder<String, AttributeValue> builder = ImmutableMapParameter
                .builder();
        builder.put(":zipcode", AttributeValue.builder_().n("98109").build_());

        queryExpression = queryExpression
                .withFilterExpression("zipcode = :zipcode")
                .withExpressionAttributeValues(builder.build());
        results = mapper.query(Customer.class, queryExpression);
        assertTrue(results.size() == 0);
    }

    /**
     * Queries using key condition expression.
     */
    @Test
    public void testKeyConditionExpression() {
        Customer customer = new Customer();
        customer.setCustomerId(Long.valueOf(FIRST_CUSTOMER_ID));

        DynamoDBQueryExpression<Customer> query =
                new DynamoDBQueryExpression<Customer>()
                        .withKeyConditionExpression(
                                "customerId = :customerId AND addressType = :addressType");
        final Builder<String, AttributeValue> builder =
                ImmutableMapParameter.builder();
        builder.put(":customerId", AttributeValue.builder_().n(FIRST_CUSTOMER_ID).build_())
               .put(":addressType", AttributeValue.builder_().s(ADDRESS_TYPE_HOME).build_())
        ;
        query.withExpressionAttributeValues(builder.build());

        PaginatedQueryList<Customer> results = mapper.query(Customer.class, query);
        assertTrue(results.size() == 1);

        builder.put(":zipcode", AttributeValue.builder_().n("98109").build_());
        query.withFilterExpression("zipcode = :zipcode")
             .withExpressionAttributeValues(builder.build());

        results = mapper.query(Customer.class, query);
        assertTrue(results.size() == 0);
    }

    /**
     * Scan the table and filters the results based on the filter expression
     * provided.
     */
    @Test
    public void testScanFilterExpression() {
        Customer customer = new Customer();
        customer.setCustomerId(Long.valueOf(FIRST_CUSTOMER_ID));

        DynamoDBScanExpression scanExpression = new DynamoDBScanExpression();

        PaginatedScanList<Customer> results = mapper.scan(Customer.class,
                                                          scanExpression);
        assertTrue(results.size() == 2);

        final Builder<String, AttributeValue> attributeValueMapBuilder = ImmutableMapParameter
                .builder();
        attributeValueMapBuilder
                .put(":state", AttributeValue.builder_().s("WA").build_());

        final Builder<String, String> attributeNameMapBuilder = ImmutableMapParameter
                .builder();
        attributeNameMapBuilder.put("#statename", "state");

        scanExpression = scanExpression
                .withFilterExpression("#statename = :state")
                .withExpressionAttributeValues(attributeValueMapBuilder.build())
                .withExpressionAttributeNames(attributeNameMapBuilder.build());
        results = mapper.scan(Customer.class, scanExpression);
        assertTrue(results.size() == 2);
    }

    /**
     * Performs delete operation with a condition expression specified. Delete
     * should fail as the condition in the conditional expression evaluates to
     * false.
     */
    @Test
    public void testDeleteConditionalExpression() {
        Customer customer = new Customer();
        customer.setCustomerId(Long.valueOf(FIRST_CUSTOMER_ID));
        customer.setAddressType(ADDRESS_TYPE_WORK);

        Builder<String, ExpectedAttributeValue> expectedMapBuilder = ImmutableMapParameter
                .builder();
        expectedMapBuilder.put("zipcode", ExpectedAttributeValue.builder_()
                .attributeValueList(AttributeValue.builder_().n("98052").build_())
                .comparisonOperator(ComparisonOperator.EQ).build_());

        DynamoDBDeleteExpression deleteExpression = new DynamoDBDeleteExpression();
        deleteExpression.setConditionExpression("zipcode = :zipcode");

        final Builder<String, AttributeValue> attributeValueMapBuilder = ImmutableMapParameter
                .builder();
        attributeValueMapBuilder.put(":zipcode",
                                     AttributeValue.builder_().n("98052").build_());
        deleteExpression.setExpressionAttributeValues(attributeValueMapBuilder
                                                              .build());
        try {
            mapper.delete(customer, deleteExpression);
        } catch (Exception e) {
            assertTrue(e instanceof ConditionalCheckFailedException);
        }
    }

    // Note don't move Customer to top level, or else it would break the release
    // pipeline, as the integration test will not be copied over causing
    // compilation failure
    @DynamoDBTable(tableName = DynamoDBMapperExpressionsIntegrationTest.TABLENAME)
    public static class Customer {

        private long customerId;

        private String addressType;

        private String addressLine1;

        private String city;

        private String state;

        private int zipcode;

        @DynamoDBAttribute(attributeName = "customerId")
        @DynamoDBHashKey(attributeName = "customerId")
        public long getCustomerId() {
            return customerId;
        }

        public void setCustomerId(long customerId) {
            this.customerId = customerId;
        }

        @DynamoDBAttribute(attributeName = "addressType")
        @DynamoDBRangeKey(attributeName = "addressType")
        public String getAddressType() {
            return addressType;
        }

        public void setAddressType(String addressType) {
            this.addressType = addressType;
        }

        @DynamoDBAttribute(attributeName = "AddressLine1")
        public String getAddressLine1() {
            return addressLine1;
        }

        public void setAddressLine1(String addressLine1) {
            this.addressLine1 = addressLine1;
        }

        @DynamoDBAttribute(attributeName = "city")
        public String getCity() {
            return city;
        }

        public void setCity(String city) {
            this.city = city;
        }

        @DynamoDBAttribute(attributeName = "state")
        public String state() {
            return state;
        }

        public void setState(String state) {
            this.state = state;
        }

        @DynamoDBAttribute(attributeName = "zipcode")
        public int getZipcode() {
            return zipcode;
        }

        public void setZipcode(int zipcode) {
            this.zipcode = zipcode;
        }
    }
}
