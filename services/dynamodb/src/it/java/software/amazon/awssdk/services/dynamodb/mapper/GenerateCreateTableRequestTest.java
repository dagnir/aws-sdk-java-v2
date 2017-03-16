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

package software.amazon.awssdk.services.dynamodb.mapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.auth.AnonymousAwsCredentials;
import software.amazon.awssdk.services.dynamodb.AmazonDynamoDBClient;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDbMapper;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.GlobalSecondaryIndex;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.LocalSecondaryIndex;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;
import software.amazon.awssdk.test.util.UnorderedCollectionComparator;
import utils.test.util.DynamoDBTestBase;

/**
 * Tests on the DynamoDBMapper.generateCreateTableRequest method.
 */
public class GenerateCreateTableRequestTest extends DynamoDBTestBase {

    private static DynamoDbMapper mapper;

    @BeforeClass
    public static void setUp() {
        dynamo = new AmazonDynamoDBClient(new AnonymousAwsCredentials());
        mapper = new DynamoDbMapper(dynamo);
    }

    private static boolean equalLsi(Collection<LocalSecondaryIndex> a, Collection<LocalSecondaryIndex> b) {
        return UnorderedCollectionComparator.equalUnorderedCollections(a, b, new LocalSecondaryIndexDefinitionComparator());
    }

    private static boolean equalGsi(Collection<GlobalSecondaryIndex> a, Collection<GlobalSecondaryIndex> b) {
        return UnorderedCollectionComparator.equalUnorderedCollections(a, b, new GlobalSecondaryIndexDefinitionComparator());
    }

    @Test
    public void testParseIndexRangeKeyClass() {
        CreateTableRequest request = mapper.generateCreateTableRequest(IndexRangeKeyClass.class);

        assertEquals("aws-java-sdk-index-range-test", request.getTableName());
        List<KeySchemaElement> expectedKeyElements = Arrays.asList(
                new KeySchemaElement("key", KeyType.HASH),
                new KeySchemaElement("rangeKey", KeyType.RANGE)
                                                                  );
        assertEquals(expectedKeyElements, request.getKeySchema());

        List<AttributeDefinition> expectedAttrDefinitions = Arrays.asList(
                new AttributeDefinition("key", ScalarAttributeType.N),
                new AttributeDefinition("rangeKey", ScalarAttributeType.N),
                new AttributeDefinition("indexFooRangeKey", ScalarAttributeType.N),
                new AttributeDefinition("indexBarRangeKey", ScalarAttributeType.N),
                new AttributeDefinition("multipleIndexRangeKey", ScalarAttributeType.N)
                                                                         );
        assertTrue(UnorderedCollectionComparator.equalUnorderedCollections(
                expectedAttrDefinitions,
                request.getAttributeDefinitions()));

        List<LocalSecondaryIndex> expectedLsi = Arrays.asList(
                new LocalSecondaryIndex()
                        .withIndexName("index_foo")
                        .withKeySchema(
                                new KeySchemaElement("key", KeyType.HASH),
                                new KeySchemaElement("indexFooRangeKey", KeyType.RANGE)),
                new LocalSecondaryIndex()
                        .withIndexName("index_bar")
                        .withKeySchema(
                                new KeySchemaElement("key", KeyType.HASH),
                                new KeySchemaElement("indexBarRangeKey", KeyType.RANGE)),
                new LocalSecondaryIndex()
                        .withIndexName("index_foo_copy")
                        .withKeySchema(
                                new KeySchemaElement("key", KeyType.HASH),
                                new KeySchemaElement("multipleIndexRangeKey", KeyType.RANGE)),
                new LocalSecondaryIndex()
                        .withIndexName("index_bar_copy")
                        .withKeySchema(
                                new KeySchemaElement("key", KeyType.HASH),
                                new KeySchemaElement("multipleIndexRangeKey", KeyType.RANGE)));
        assertTrue(equalLsi(expectedLsi, request.getLocalSecondaryIndexes()));

        assertNull(request.getGlobalSecondaryIndexes());
        assertNull(request.getProvisionedThroughput());
    }

    @Test
    public void testComplexIndexedHashRangeClass() {
        CreateTableRequest request = mapper.generateCreateTableRequest(MapperQueryExpressionTest.HashRangeClass.class);

        assertEquals("table_name", request.getTableName());
        List<KeySchemaElement> expectedKeyElements = Arrays.asList(
                new KeySchemaElement("primaryHashKey", KeyType.HASH),
                new KeySchemaElement("primaryRangeKey", KeyType.RANGE)
                                                                  );
        assertEquals(expectedKeyElements, request.getKeySchema());

        List<AttributeDefinition> expectedAttrDefinitions = Arrays.asList(
                new AttributeDefinition("primaryHashKey", ScalarAttributeType.S),
                new AttributeDefinition("indexHashKey", ScalarAttributeType.S),
                new AttributeDefinition("primaryRangeKey", ScalarAttributeType.S),
                new AttributeDefinition("indexRangeKey", ScalarAttributeType.S),
                new AttributeDefinition("anotherIndexRangeKey", ScalarAttributeType.S)
                                                                         );
        assertTrue(UnorderedCollectionComparator.equalUnorderedCollections(
                expectedAttrDefinitions,
                request.getAttributeDefinitions()));

        List<LocalSecondaryIndex> expectedLsi = Arrays.asList(
                new LocalSecondaryIndex()
                        .withIndexName("LSI-primary-range")
                        .withKeySchema(
                                new KeySchemaElement("primaryHashKey", KeyType.HASH),
                                new KeySchemaElement("primaryRangeKey", KeyType.RANGE)),
                new LocalSecondaryIndex()
                        .withIndexName("LSI-index-range-1")
                        .withKeySchema(
                                new KeySchemaElement("primaryHashKey", KeyType.HASH),
                                new KeySchemaElement("indexRangeKey", KeyType.RANGE)),
                new LocalSecondaryIndex()
                        .withIndexName("LSI-index-range-2")
                        .withKeySchema(
                                new KeySchemaElement("primaryHashKey", KeyType.HASH),
                                new KeySchemaElement("indexRangeKey", KeyType.RANGE)),
                new LocalSecondaryIndex()
                        .withIndexName("LSI-index-range-3")
                        .withKeySchema(
                                new KeySchemaElement("primaryHashKey", KeyType.HASH),
                                new KeySchemaElement("anotherIndexRangeKey", KeyType.RANGE)));
        assertTrue(equalLsi(expectedLsi, request.getLocalSecondaryIndexes()));

        List<GlobalSecondaryIndex> expectedGsi = Arrays.asList(
                new GlobalSecondaryIndex()
                        .withIndexName("GSI-primary-hash-index-range-1")
                        .withKeySchema(
                                new KeySchemaElement("primaryHashKey", KeyType.HASH),
                                new KeySchemaElement("indexRangeKey", KeyType.RANGE)),
                new GlobalSecondaryIndex()
                        .withIndexName("GSI-primary-hash-index-range-2")
                        .withKeySchema(
                                new KeySchemaElement("primaryHashKey", KeyType.HASH),
                                new KeySchemaElement("anotherIndexRangeKey", KeyType.RANGE)),
                new GlobalSecondaryIndex()
                        .withIndexName("GSI-index-hash-primary-range")
                        .withKeySchema(
                                new KeySchemaElement("indexHashKey", KeyType.HASH),
                                new KeySchemaElement("primaryRangeKey", KeyType.RANGE)),
                new GlobalSecondaryIndex()
                        .withIndexName("GSI-index-hash-index-range-1")
                        .withKeySchema(
                                new KeySchemaElement("indexHashKey", KeyType.HASH),
                                new KeySchemaElement("indexRangeKey", KeyType.RANGE)),
                new GlobalSecondaryIndex()
                        .withIndexName("GSI-index-hash-index-range-2")
                        .withKeySchema(
                                new KeySchemaElement("indexHashKey", KeyType.HASH),
                                new KeySchemaElement("indexRangeKey", KeyType.RANGE)));
        assertTrue(equalGsi(expectedGsi, request.getGlobalSecondaryIndexes()));

        assertNull(request.getProvisionedThroughput());
    }

    private static class LocalSecondaryIndexDefinitionComparator
            implements
            UnorderedCollectionComparator.CrossTypeComparator<LocalSecondaryIndex, LocalSecondaryIndex> {

        @Override
        public boolean equals(LocalSecondaryIndex a, LocalSecondaryIndex b) {
            return a.getIndexName().equals(b.getIndexName())
                   && a.getKeySchema().equals(b.getKeySchema());
        }

    }

    private static class GlobalSecondaryIndexDefinitionComparator
            implements
            UnorderedCollectionComparator.CrossTypeComparator<GlobalSecondaryIndex, GlobalSecondaryIndex> {

        @Override
        public boolean equals(GlobalSecondaryIndex a, GlobalSecondaryIndex b) {
            return a.getIndexName().equals(b.getIndexName())
                   && a.getKeySchema().equals(b.getKeySchema());
        }
    }
}
