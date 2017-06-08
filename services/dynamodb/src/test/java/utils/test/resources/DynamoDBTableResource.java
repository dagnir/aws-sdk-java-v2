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

package utils.test.resources;

import java.util.List;
import software.amazon.awssdk.AmazonServiceException;
import software.amazon.awssdk.services.dynamodb.DynamoDBClient;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DeleteTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableRequest;
import software.amazon.awssdk.services.dynamodb.model.GlobalSecondaryIndex;
import software.amazon.awssdk.services.dynamodb.model.GlobalSecondaryIndexDescription;
import software.amazon.awssdk.services.dynamodb.model.LocalSecondaryIndex;
import software.amazon.awssdk.services.dynamodb.model.LocalSecondaryIndexDescription;
import software.amazon.awssdk.services.dynamodb.model.Projection;
import software.amazon.awssdk.services.dynamodb.model.TableDescription;
import software.amazon.awssdk.services.dynamodb.model.TableStatus;
import software.amazon.awssdk.services.dynamodb.util.TableUtils;
import software.amazon.awssdk.test.util.UnorderedCollectionComparator;
import utils.resources.TestResource;
import utils.test.util.DynamoDBTestBase;

public abstract class DynamoDBTableResource implements TestResource {

    /**
     * Returns true if the two lists of GlobalSecondaryIndex and
     * GlobalSecondaryIndexDescription share the same set of:
     * 1) indexName
     * 2) projection
     * 3) keySchema (compared as unordered lists)
     */
    static boolean equalUnorderedGsiLists(List<GlobalSecondaryIndex> listA, List<GlobalSecondaryIndexDescription> listB) {
        return UnorderedCollectionComparator.equalUnorderedCollections(
                listA, listB,
                new UnorderedCollectionComparator.CrossTypeComparator<GlobalSecondaryIndex, GlobalSecondaryIndexDescription>() {
                    @Override
                    public boolean equals(GlobalSecondaryIndex a, GlobalSecondaryIndexDescription b) {
                        return a.indexName().equals(b.indexName())
                               && equalProjections(a.projection(), b.projection())
                               && UnorderedCollectionComparator.equalUnorderedCollections(a.keySchema(), b.keySchema());
                    }
                });
    }

    /**
     * Returns true if the two lists of LocalSecondaryIndex and
     * LocalSecondaryIndexDescription share the same set of:
     * 1) indexName
     * 2) projection
     * 3) keySchema (compared as unordered lists)
     */
    static boolean equalUnorderedLsiLists(List<LocalSecondaryIndex> listA, List<LocalSecondaryIndexDescription> listB) {
        return UnorderedCollectionComparator.equalUnorderedCollections(
                listA, listB,
                new UnorderedCollectionComparator.CrossTypeComparator<LocalSecondaryIndex, LocalSecondaryIndexDescription>() {
                    @Override
                    public boolean equals(LocalSecondaryIndex a, LocalSecondaryIndexDescription b) {
                        // Project parameter might not be specified in the
                        // CreateTableRequest. But it should be treated as equal
                        // to the default projection type - KEYS_ONLY.
                        return a.indexName().equals(b.indexName())
                               && equalProjections(a.projection(), b.projection())
                               && UnorderedCollectionComparator.equalUnorderedCollections(a.keySchema(), b.keySchema());
                    }
                });
    }

    /**
     * Compares the Projection parameter included in the CreateTableRequest,
     * with the one returned from DescribeTableResult.
     */
    static boolean equalProjections(Projection fromCreateTableRequest, Projection fromDescribeTableResult) {
        if (fromCreateTableRequest == null || fromDescribeTableResult == null) {
            throw new IllegalStateException("The projection parameter should never be null.");
        }

        return fromCreateTableRequest.projectionType().equals(
                fromDescribeTableResult.projectionType())
               && UnorderedCollectionComparator.equalUnorderedCollections(
                fromCreateTableRequest.nonKeyAttributes(),
                fromDescribeTableResult.nonKeyAttributes());
    }

    protected abstract DynamoDBClient getClient();

    protected abstract CreateTableRequest getCreateTableRequest();

    /**
     * Implementation of TestResource interfaces
     */

    @Override
    public void create(boolean waitTillFinished) {
        System.out.println("Creating " + this + "...");
        getClient().createTable(getCreateTableRequest());

        if (waitTillFinished) {
            System.out.println("Waiting for " + this + " to become active...");
            try {
                TableUtils.waitUntilActive(getClient(), getCreateTableRequest().tableName());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public void delete(boolean waitTillFinished) {
        System.out.println("Deleting " + this + "...");
        getClient().deleteTable(DeleteTableRequest.builder().tableName(getCreateTableRequest().tableName()).build());

        if (waitTillFinished) {
            System.out.println("Waiting for " + this + " to become deleted...");
            DynamoDBTestBase.waitForTableToBecomeDeleted(getClient(), getCreateTableRequest().tableName());
        }
    }

    @Override
    public ResourceStatus getResourceStatus() {
        CreateTableRequest createRequest = getCreateTableRequest();
        TableDescription table = null;
        try {
            table = getClient().describeTable(DescribeTableRequest.builder().tableName(
                    createRequest.tableName()).build()).table();
        } catch (AmazonServiceException ase) {
            if (ase.getErrorCode().equalsIgnoreCase("ResourceNotFoundException")) {
                return ResourceStatus.NOT_EXIST;
            }
        }

        String tableStatus = table.tableStatus();

        if (tableStatus.equals(TableStatus.ACTIVE.toString())) {
            // returns AVAILABLE only if table KeySchema + LSIs + GSIs all match.
            if (UnorderedCollectionComparator.equalUnorderedCollections(createRequest.keySchema(), table.keySchema())
                && equalUnorderedGsiLists(createRequest.globalSecondaryIndexes(), table.globalSecondaryIndexes())
                && equalUnorderedLsiLists(createRequest.localSecondaryIndexes(), table.localSecondaryIndexes())) {
                return ResourceStatus.AVAILABLE;
            } else {
                return ResourceStatus.EXIST_INCOMPATIBLE_RESOURCE;
            }
        } else if (tableStatus.equals(TableStatus.CREATING.toString())
                   || tableStatus.equals(TableStatus.UPDATING.toString())
                   || tableStatus.equals(TableStatus.DELETING.toString())) {
            return ResourceStatus.TRANSIENT;
        } else {
            return ResourceStatus.NOT_EXIST;
        }
    }

    /**
     * Object interfaces
     */
    @Override
    public String toString() {
        return "DynamoDB Table [" + getCreateTableRequest().tableName() + "]";
    }

    @Override
    public int hashCode() {
        return getCreateTableRequest().hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof DynamoDBTableResource)) {
            return false;
        }
        return getCreateTableRequest().equals(
                ((DynamoDBTableResource) other).getCreateTableRequest());
    }
}
