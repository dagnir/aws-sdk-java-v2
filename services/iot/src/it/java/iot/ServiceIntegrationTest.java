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

package iot;

import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.regions.Regions;
import software.amazon.awssdk.services.iotdata.AWSIotDataClient;
import software.amazon.awssdk.services.iotdata.model.DeleteThingShadowRequest;
import software.amazon.awssdk.services.iotdata.model.DeleteThingShadowResult;
import software.amazon.awssdk.services.iotdata.model.GetThingShadowRequest;
import software.amazon.awssdk.services.iotdata.model.GetThingShadowResult;
import software.amazon.awssdk.services.iotdata.model.InternalFailureException;
import software.amazon.awssdk.services.iotdata.model.InvalidRequestException;
import software.amazon.awssdk.services.iotdata.model.PublishRequest;
import software.amazon.awssdk.services.iotdata.model.ResourceNotFoundException;
import software.amazon.awssdk.services.iotdata.model.UpdateThingShadowRequest;
import software.amazon.awssdk.services.iotdata.model.UpdateThingShadowResult;
import software.amazon.awssdk.test.AwsIntegrationTestBase;

public class ServiceIntegrationTest extends AwsIntegrationTestBase {

    private static final String STATE_FIELD_NAME = "state";
    private static final String THING_NAME = "foo";
    private static final String INVALID_THING_NAME = "INVALID_THING_NAME";

    private AWSIotDataClient iot;

    private static ByteBuffer getPayloadAsByteBuffer(String payloadString) {
        return ByteBuffer.wrap(payloadString.getBytes(StandardCharsets.UTF_8));
    }

    private static JsonNode getPaylaodAsJsonNode(ByteBuffer payload) throws IOException {
        return new ObjectMapper().readTree(payload.duplicate().array());
    }

    private static void assertPayloadNonEmpty(ByteBuffer payload) {
        assertThat(payload.capacity(), greaterThan(0));
    }

    /**
     * Asserts that the returned payload has the correct state in the JSON document. It should be
     * exactly what we sent it plus some additional metadata
     *
     * @param originalPayload
     *            ByteBuffer we sent to the service containing just the state
     * @param returnedPayload
     *            ByteBuffer returned by the service containing the state (which should be the same
     *            as what we sent) plus additional metadata in the JSON document
     */
    private static void assertPayloadIsValid(ByteBuffer originalPayload, ByteBuffer returnedPayload) throws Exception {
        JsonNode originalJson = getPaylaodAsJsonNode(originalPayload);
        JsonNode returnedJson = getPaylaodAsJsonNode(returnedPayload);
        assertEquals(originalJson.get(STATE_FIELD_NAME), returnedJson.get(STATE_FIELD_NAME));
    }

    @Before
    public void setup() throws Exception {
        iot = new AWSIotDataClient(getCredentials());
        iot.configureRegion(Regions.US_EAST_1);
    }

    @Test
    public void publish_ValidTopicAndNonEmptyPayload_DoesNotThrowException() {
        iot.publish(new PublishRequest().withTopic(THING_NAME).withPayload(ByteBuffer.wrap(new byte[] {1, 2, 3, 4})));
    }

    @Test
    public void publish_WithValidTopicAndEmptyPayload_DoesNotThrowException() {
        iot.publish(new PublishRequest().withTopic(THING_NAME).withPayload(null));
    }

    @Test(expected = InternalFailureException.class)
    public void publish_WithNullTopic_ThrowsException() {
        iot.publish(new PublishRequest().withTopic(null).withPayload(null));
    }

    @Test(expected = ResourceNotFoundException.class)
    public void updateThingShadow_NullThingName_ThrowsServiceException() throws Exception {
        UpdateThingShadowRequest request = new UpdateThingShadowRequest().withThingName(null).withPayload(null);
        iot.updateThingShadow(request);

    }

    @Test(expected = InvalidRequestException.class)
    public void updateThingShadow_NullPayload_ThrowsServiceException() throws Exception {
        UpdateThingShadowRequest request = new UpdateThingShadowRequest().withThingName(THING_NAME).withPayload(null);
        iot.updateThingShadow(request);
    }

    @Test(expected = InvalidRequestException.class)
    public void updateThingShadow_MalformedPayload_ThrowsServiceException() throws Exception {
        ByteBuffer payload = getPayloadAsByteBuffer("{ }");
        UpdateThingShadowRequest request = new UpdateThingShadowRequest().withThingName(THING_NAME)
                                                                         .withPayload(payload);
        iot.updateThingShadow(request);
    }

    @Test(expected = ResourceNotFoundException.class)
    public void getThingShadow_InvalidThingName_ThrowsException() {
        iot.getThingShadow(new GetThingShadowRequest().withThingName(INVALID_THING_NAME));
    }

    @Test(expected = ResourceNotFoundException.class)
    public void deleteThingShadow_InvalidThing_ThrowsException() {
        DeleteThingShadowResult result = iot
                .deleteThingShadow(new DeleteThingShadowRequest().withThingName(INVALID_THING_NAME));
        assertPayloadNonEmpty(result.getPayload());
    }

    @Test
    public void UpdateReadDeleteThing() throws Exception {
        updateThingShadow_ValidRequest_ReturnsValidResponse(THING_NAME);
        getThingShadow_ValidThing_ReturnsThingData(THING_NAME);
        deleteThingShadow_ValidThing_DeletesSuccessfully(THING_NAME);
    }

    private void updateThingShadow_ValidRequest_ReturnsValidResponse(String thingName) throws Exception {
        ByteBuffer originalPayload = getPayloadAsByteBuffer("{ \"state\": {\"reported\":{ \"r\": {}}}}");
        UpdateThingShadowRequest request = new UpdateThingShadowRequest().withThingName(thingName)
                                                                         .withPayload(originalPayload);
        UpdateThingShadowResult result = iot.updateThingShadow(request);

        // Comes back with some extra metadata so we assert it's bigger than the original
        assertThat(result.getPayload().capacity(), greaterThan(originalPayload.capacity()));
        assertPayloadIsValid(originalPayload, result.getPayload());
    }

    private void getThingShadow_ValidThing_ReturnsThingData(String thingName) {
        GetThingShadowRequest request = new GetThingShadowRequest().withThingName(thingName);
        GetThingShadowResult result = iot.getThingShadow(request);
        assertPayloadNonEmpty(result.getPayload());
    }

    private void deleteThingShadow_ValidThing_DeletesSuccessfully(String thingName) {
        DeleteThingShadowResult result = iot.deleteThingShadow(new DeleteThingShadowRequest().withThingName(thingName));
        assertPayloadNonEmpty(result.getPayload());
    }

}
