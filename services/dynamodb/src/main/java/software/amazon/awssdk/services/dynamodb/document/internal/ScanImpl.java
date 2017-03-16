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

package software.amazon.awssdk.services.dynamodb.document.internal;

import java.util.Collection;
import java.util.Map;
import software.amazon.awssdk.services.dynamodb.DynamoDBClient;
import software.amazon.awssdk.services.dynamodb.document.ItemCollection;
import software.amazon.awssdk.services.dynamodb.document.KeyAttribute;
import software.amazon.awssdk.services.dynamodb.document.ScanFilter;
import software.amazon.awssdk.services.dynamodb.document.ScanOutcome;
import software.amazon.awssdk.services.dynamodb.document.Table;
import software.amazon.awssdk.services.dynamodb.document.api.ScanApi;
import software.amazon.awssdk.services.dynamodb.document.spec.ScanSpec;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;

/**
 * The implementation for <code>ScanApi</code>.
 */
public class ScanImpl extends AbstractImpl implements ScanApi {
    public ScanImpl(DynamoDBClient client, Table table) {
        super(client, table);
    }

    @Override
    public ItemCollection<ScanOutcome> scan(ScanFilter... scanFilters) {
        return doScan(new ScanSpec()
                              .withScanFilters(scanFilters));
    }


    @Override
    public ItemCollection<ScanOutcome> scan(String filterExpression,
                                            Map<String, String> nameMap, Map<String, Object> valueMap) {
        return doScan(new ScanSpec()
                              .withFilterExpression(filterExpression)
                              .withNameMap(nameMap)
                              .withValueMap(valueMap));
    }


    @Override
    public ItemCollection<ScanOutcome> scan(String filterExpression,
                                            String projectionExpression, Map<String, String> nameMap,
                                            Map<String, Object> valueMap) {
        return doScan(new ScanSpec()
                              .withFilterExpression(filterExpression)
                              .withProjectionExpression(projectionExpression)
                              .withNameMap(nameMap)
                              .withValueMap(valueMap));
    }

    @Override
    public ItemCollection<ScanOutcome> scan(ScanSpec spec) {
        return doScan(spec);
    }

    protected ItemCollection<ScanOutcome> doScan(ScanSpec spec) {
        // set the table name
        String tableName = getTable().getTableName();
        ScanRequest request = spec.getRequest().withTableName(tableName);

        // set up the start key, if any
        Collection<KeyAttribute> startKey = spec.getExclusiveStartKey();
        if (startKey != null) {
            request.setExclusiveStartKey(InternalUtils.toAttributeValueMap(startKey));
        }

        // scan filters;
        Collection<ScanFilter> filters = spec.getScanFilters();
        if (filters != null) {
            request.setScanFilter(InternalUtils.toAttributeConditionMap(filters));
        }

        // set up the value map, if any (when expression API is used)
        final Map<String, AttributeValue> attrValMap = InternalUtils.fromSimpleMap(spec.getValueMap());
        // set up expressions, if any
        request.withExpressionAttributeNames(spec.getNameMap())
               .withExpressionAttributeValues(attrValMap);
        return new ScanCollection(getClient(), spec);
    }
}
