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

package software.amazon.awssdk.services.dynamodbv2.datamodeling;

import software.amazon.awssdk.annotation.SdkInternalApi;

/**
 * {@link DynamoDbMapper} table model factory.
 */
@SdkInternalApi
public interface DynamoDBMapperModelFactory {

    /**
     * Gets/creates the mapper's model factory.
     */
    public TableFactory getTableFactory(DynamoDBMapperConfig config);

    /**
     * {@link DynamoDBMapperModelFactory} factory.
     */
    public static interface TableFactory {
        /**
         * Gets the table model for the given type and configuration.
         */
        public <T> DynamoDBMapperTableModel<T> getTable(Class<T> clazz);
    }

}
