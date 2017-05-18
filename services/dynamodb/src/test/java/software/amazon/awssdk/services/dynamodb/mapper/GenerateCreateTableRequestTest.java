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
import org.junit.Ignore;
import org.junit.Test;
import software.amazon.awssdk.auth.AnonymousCredentialsProvider;
import software.amazon.awssdk.regions.Regions;
import software.amazon.awssdk.services.dynamodb.DynamoDBClient;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDBMapper;
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
@Ignore // FIXME: setup fails with "region cannot be null"
public class GenerateCreateTableRequestTest extends DynamoDBTestBase {

    private static DynamoDBMapper mapper;

    @BeforeClass
    public static void setUp() {
        dynamo = DynamoDBClient.builder()
                .credentialsProvider(new AnonymousCredentialsProvider())
                .region(Regions.US_WEST_2.getName())
                .build();
        mapper = new DynamoDBMapper(dynamo);
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

        assertEquals("aws-java-sdk-index-range-test", request.tableName());
        List<KeySchemaElement> expectedKeyElements = Arrays.asList(
                KeySchemaElement.builder_().attributeName("key").keyType(KeyType.HASH).build_(),
                KeySchemaElement.builder_().attributeName("rangeKey").keyType(KeyType.RANGE).build_()
                                                                  );
        assertEquals(expectedKeyElements, request.keySchema());

        List<AttributeDefinition> expectedAttrDefinitions = Arrays.asList(
                AttributeDefinition.builder_().attributeName("key").attributeType(ScalarAttributeType.N).build_(),
                AttributeDefinition.builder_().attributeName("rangeKey").attributeType(ScalarAttributeType.N).build_(),
                AttributeDefinition.builder_().attributeName("indexFooRangeKey").attributeType(ScalarAttributeType.N).build_(),
                AttributeDefinition.builder_().attributeName("indexBarRangeKey").attributeType(ScalarAttributeType.N).build_(),
                AttributeDefinition.builder_().attributeName("multipleIndexRangeKey").attributeType(ScalarAttributeType.N).build_()
                                                                         );
        assertTrue(UnorderedCollectionComparator.equalUnorderedCollections(
                expectedAttrDefinitions,
                request.attributeDefinitions()));

        List<LocalSecondaryIndex> expectedLsi = Arrays.asList(
                LocalSecondaryIndex.builder_()
                        .indexName("index_foo")
                        .keySchema(
                                KeySchemaElement.builder_().attributeName("key").keyType(KeyType.HASH).build_(),
                                KeySchemaElement.builder_().attributeName("indexFooRangeKey").keyType(KeyType.RANGE).build_()).build_(),
                LocalSecondaryIndex.builder_()
                        .indexName("index_bar")
                        .keySchema(
                                KeySchemaElement.builder_().attributeName("key").keyType(KeyType.HASH).build_(),
                                KeySchemaElement.builder_().attributeName("indexBarRangeKey").keyType(KeyType.RANGE).build_()).build_(),
                LocalSecondaryIndex.builder_()
                        .indexName("index_foo_copy")
                        .keySchema(
                                KeySchemaElement.builder_().attributeName("key").keyType(KeyType.HASH).build_(),
                                KeySchemaElement.builder_().attributeName("multipleIndexRangeKey").keyType(KeyType.RANGE).build_()).build_(),
                LocalSecondaryIndex.builder_()
                        .indexName("index_bar_copy")
                        .keySchema(
                                KeySchemaElement.builder_().attributeName("key").keyType(KeyType.HASH).build_(),
                                KeySchemaElement.builder_().attributeName("multipleIndexRangeKey").keyType(KeyType.RANGE).build_()).build_());
        assertTrue(equalLsi(expectedLsi, request.localSecondaryIndexes()));

        assertNull(request.globalSecondaryIndexes());
        assertNull(request.provisionedThroughput());
    }

    @Test
    public void testComplexIndexedHashRangeClass() {
        CreateTableRequest request = mapper.generateCreateTableRequest(MapperQueryExpressionTest.HashRangeClass.class);

        assertEquals("table_name", request.tableName());
        List<KeySchemaElement> expectedKeyElements = Arrays.asList(
                KeySchemaElement.builder_().attributeName("primaryHashKey").keyType(KeyType.HASH).build_(),
                KeySchemaElement.builder_().attributeName("primaryRangeKey").keyType(KeyType.RANGE).build_()
                                                                  );
        assertEquals(expectedKeyElements, request.keySchema());

        List<AttributeDefinition> expectedAttrDefinitions = Arrays.asList(
                AttributeDefinition.builder_().attributeName("primaryHashKey").attributeType(ScalarAttributeType.S).build_(),
                AttributeDefinition.builder_().attributeName("indexHashKey").attributeType(ScalarAttributeType.S).build_(),
                AttributeDefinition.builder_().attributeName("primaryRangeKey").attributeType(ScalarAttributeType.S).build_(),
                AttributeDefinition.builder_().attributeName("indexRangeKey").attributeType(ScalarAttributeType.S).build_(),
                AttributeDefinition.builder_().attributeName("anotherIndexRangeKey").attributeType(ScalarAttributeType.S).build_()
                                                                         );
        assertTrue(UnorderedCollectionComparator.equalUnorderedCollections(
                expectedAttrDefinitions,
                request.attributeDefinitions()));

        List<LocalSecondaryIndex> expectedLsi = Arrays.asList(
                LocalSecondaryIndex.builder_()
                        .indexName("LSI-primary-range")
                        .keySchema(
                                KeySchemaElement.builder_().attributeName("primaryHashKey").keyType(KeyType.HASH).build_(),
                                KeySchemaElement.builder_().attributeName("primaryRangeKey").keyType(KeyType.RANGE).build_()).build_(),
                LocalSecondaryIndex.builder_()
                        .indexName("LSI-index-range-1")
                        .keySchema(
                                KeySchemaElement.builder_().attributeName("primaryHashKey").keyType(KeyType.HASH).build_(),
                                KeySchemaElement.builder_().attributeName("indexRangeKey").keyType(KeyType.RANGE).build_()).build_(),
                LocalSecondaryIndex.builder_()
                        .indexName("LSI-index-range-2")
                        .keySchema(
                                KeySchemaElement.builder_().attributeName("primaryHashKey").keyType(KeyType.HASH).build_(),
                                KeySchemaElement.builder_().attributeName("indexRangeKey").keyType(KeyType.RANGE).build_()).build_(),
                LocalSecondaryIndex.builder_()
                        .indexName("LSI-index-range-3")
                        .keySchema(
                                KeySchemaElement.builder_().attributeName("primaryHashKey").keyType(KeyType.HASH).build_(),
                                KeySchemaElement.builder_().attributeName("anotherIndexRangeKey").keyType(KeyType.RANGE).build_()).build_());
        assertTrue(equalLsi(expectedLsi, request.localSecondaryIndexes()));

        List<GlobalSecondaryIndex> expectedGsi = Arrays.asList(
                GlobalSecondaryIndex.builder_()
                        .indexName("GSI-primary-hash-index-range-1")
                        .keySchema(
                                KeySchemaElement.builder_().attributeName("primaryHashKey").keyType(KeyType.HASH).build_(),
                                KeySchemaElement.builder_().attributeName("indexRangeKey").keyType(KeyType.RANGE).build_()).build_(),
                GlobalSecondaryIndex.builder_()
                        .indexName("GSI-primary-hash-index-range-2")
                        .keySchema(
                                KeySchemaElement.builder_().attributeName("primaryHashKey").keyType(KeyType.HASH).build_(),
                                KeySchemaElement.builder_().attributeName("anotherIndexRangeKey").keyType(KeyType.RANGE).build_()).build_(),
                GlobalSecondaryIndex.builder_()
                        .indexName("GSI-index-hash-primary-range")
                        .keySchema(
                                KeySchemaElement.builder_().attributeName("indexHashKey").keyType(KeyType.HASH).build_(),
                                KeySchemaElement.builder_().attributeName("primaryRangeKey").keyType(KeyType.RANGE).build_()).build_(),
                GlobalSecondaryIndex.builder_()
                        .indexName("GSI-index-hash-index-range-1")
                        .keySchema(
                                KeySchemaElement.builder_().attributeName("indexHashKey").keyType(KeyType.HASH).build_(),
                                KeySchemaElement.builder_().attributeName("indexRangeKey").keyType(KeyType.RANGE).build_()).build_(),
                GlobalSecondaryIndex.builder_()
                        .indexName("GSI-index-hash-index-range-2")
                        .keySchema(
                                KeySchemaElement.builder_().attributeName("indexHashKey").keyType(KeyType.HASH).build_(),
                                KeySchemaElement.builder_().attributeName("indexRangeKey").keyType(KeyType.RANGE).build_()).build_());
        assertTrue(equalGsi(expectedGsi, request.globalSecondaryIndexes()));

        assertNull(request.provisionedThroughput());
    }

    private static class LocalSecondaryIndexDefinitionComparator
            implements
            UnorderedCollectionComparator.CrossTypeComparator<LocalSecondaryIndex, LocalSecondaryIndex> {

        @Override
        public boolean equals(LocalSecondaryIndex a, LocalSecondaryIndex b) {
            return a.indexName().equals(b.indexName())
                   && a.keySchema().equals(b.keySchema());
        }

    }

    private static class GlobalSecondaryIndexDefinitionComparator
            implements
            UnorderedCollectionComparator.CrossTypeComparator<GlobalSecondaryIndex, GlobalSecondaryIndex> {

        @Override
        public boolean equals(GlobalSecondaryIndex a, GlobalSecondaryIndex b) {
            return a.indexName().equals(b.indexName())
                   && a.keySchema().equals(b.keySchema());
        }
    }
}
