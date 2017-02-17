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

package software.amazon.awssdk.services.dynamodbv2.datamodeling.unmarshallers;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import software.amazon.awssdk.services.dynamodbv2.model.AttributeValue;

/**
 * An unmarshaller that unmarshals sets of UUIDs as sets of
 * Java {@code UUID} objects.
 *
 * @author Sergei Egorov
 */
public class UUIDSetUnmarshaller extends SSUnmarshaller {

    private static final UUIDSetUnmarshaller INSTANCE =
            new UUIDSetUnmarshaller();

    private UUIDSetUnmarshaller() {
    }

    public static UUIDSetUnmarshaller instance() {
        return INSTANCE;
    }

    @Override
    public Set<UUID> unmarshall(AttributeValue value) {
        Set<UUID> result = new HashSet<UUID>();

        for (String s : value.getSS()) {
            result.add(UUID.fromString(s));
        }

        return result;
    }
}
