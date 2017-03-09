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

package software.amazon.awssdk.services.dynamodbv2.pojos;

import software.amazon.awssdk.services.dynamodbv2.datamodeling.DynamoDbAutoGeneratedKey;
import software.amazon.awssdk.services.dynamodbv2.datamodeling.DynamoDbHashKey;
import software.amazon.awssdk.services.dynamodbv2.datamodeling.DynamoDbTable;

/**
 * Auto-generated string key and value.
 */
@DynamoDbTable(tableName = "aws-java-sdk-util")
public class AutoKeyAndVal<V> extends KeyAndVal<String, V> {
    @Override
    @DynamoDbHashKey
    @DynamoDbAutoGeneratedKey
    public String getKey() {
        return super.getKey();
    }

    @Override
    public void setKey(final String key) {
        super.setKey(key);
    }

    @Override
    public V getVal() {
        return super.getVal();
    }

    @Override
    public void setVal(final V val) {
        super.setVal(val);
    }
}
