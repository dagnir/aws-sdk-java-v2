/*
 * Copyright 2011-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.services.dynamodbv2;

import static org.junit.Assert.assertEquals;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.ByteBuffer;
import org.junit.Test;
import software.amazon.awssdk.Request;
import software.amazon.awssdk.protocol.json.JsonClientMetadata;
import software.amazon.awssdk.protocol.json.SdkJsonProtocolFactory;
import software.amazon.awssdk.services.dynamodbv2.model.AttributeValue;
import software.amazon.awssdk.services.dynamodbv2.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodbv2.model.transform.PutItemRequestMarshaller;
import software.amazon.awssdk.util.BinaryUtils;
import software.amazon.awssdk.util.ImmutableMapParameter;

public class PutItemRequestMarshallerTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final PutItemRequestMarshaller marshaller = new PutItemRequestMarshaller(
            new SdkJsonProtocolFactory(new JsonClientMetadata().withProtocolVersion("1.0")));

    /**
     * Regression test for TT0075355961
     */
    @Test
    public void onlyRemainingByteBufferDataIsMarshalled() throws IOException {
        final ByteBuffer byteBuffer = ByteBuffer.wrap(new byte[]{0, 0, 0, 1, 1, 1});
        // Consume some of the byte buffer
        byteBuffer.position(3);
        Request<PutItemRequest> marshalled = marshaller.marshall(new PutItemRequest().withItem(
                ImmutableMapParameter.of("binaryProp", new AttributeValue().withB(byteBuffer))));
        JsonNode marshalledContent = MAPPER.readTree(marshalled.getContent());
        String base64Binary = marshalledContent.get("Item").get("binaryProp").get("B").asText();
        // Only the remaining data in the byte buffer should have been read and marshalled.
        assertEquals(BinaryUtils.toBase64(new byte[]{1, 1, 1}), base64Binary);
    }
}
