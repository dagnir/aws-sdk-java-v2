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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import software.amazon.awssdk.services.dynamodb.DynamoDBMapperIntegrationTestBase;

/**
 * Status tests for {@code EnumMarshaller}.
 */
public class EnumMarshallerIntegrationTest extends DynamoDBMapperIntegrationTestBase {

    @Test
    public void testNullEnumValue() {
        final DynamoDbMapper mapper = new DynamoDbMapper(dynamo);

        final TestObject object1 = new TestObject();

        assertNull(object1.status());

        mapper.save(object1);

        final TestObject object2 = mapper.load(TestObject.class, object1.getKey());

        assertNull(object2.status());
    }

    @Test
    public void testMarshalling() {
        final DynamoDbMapper mapper = new DynamoDbMapper(dynamo);

        final TestObject object1 = new TestObject();

        object1.setStatus(TestObject.Status.Y);

        mapper.save(object1);

        assertNotNull(object1.getKey());
        assertNotNull(object1.status());

        final TestObject object2 = mapper.load(TestObject.class, object1.getKey());

        assertEquals(object1.getKey(), object2.getKey());
        assertEquals(object1.status(), object2.status());
    }

    @DynamoDbTable(tableName = "aws-java-sdk-util")
    public static class TestObject {
        private String key;


        private Status status;



        @DynamoDbHashKey
        @DynamoDbAutoGeneratedKey
        public String getKey() {
            return this.key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        @DynamoDbMarshalling(marshallerClass = StatusEnumMarshaller.class)
        public Status status() {
            return this.status;
        }

        public void setStatus(Status status) {
            this.status = status;
        }

        public static enum Status {
            X,
            Y,
            Z
        }

        public static class StatusEnumMarshaller extends AbstractEnumMarshaller<Status> {
        }
    }

}
