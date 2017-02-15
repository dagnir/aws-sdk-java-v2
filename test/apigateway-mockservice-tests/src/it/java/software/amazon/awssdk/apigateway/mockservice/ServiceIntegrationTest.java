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

package software.amazon.awssdk.apigateway.mockservice;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.log4j.BasicConfigurator;
import org.hamcrest.CoreMatchers;
import org.hamcrest.core.IsInstanceOf;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.SdkBaseException;
import software.amazon.awssdk.auth.AWSStaticCredentialsProvider;
import software.amazon.awssdk.auth.BasicAWSCredentials;
import software.amazon.awssdk.services.apigateway.mockservice.MyService;
import software.amazon.awssdk.services.apigateway.mockservice.model.AnonListsInput;
import software.amazon.awssdk.services.apigateway.mockservice.model.AnonMapInput;
import software.amazon.awssdk.services.apigateway.mockservice.model.AnonObjectInput;
import software.amazon.awssdk.services.apigateway.mockservice.model.AnonymousListItem;
import software.amazon.awssdk.services.apigateway.mockservice.model.AnonymousObject;
import software.amazon.awssdk.services.apigateway.mockservice.model.GetNoauthErrorsRequest;
import software.amazon.awssdk.services.apigateway.mockservice.model.GetNoauthScalarsRequest;
import software.amazon.awssdk.services.apigateway.mockservice.model.GetNoauthScalarsResult;
import software.amazon.awssdk.services.apigateway.mockservice.model.InlineRefMember;
import software.amazon.awssdk.services.apigateway.mockservice.model.InternalServerErrorException;
import software.amazon.awssdk.services.apigateway.mockservice.model.LowercaseOperationNameRequest;
import software.amazon.awssdk.services.apigateway.mockservice.model.LowercaseOperationNameResult;
import software.amazon.awssdk.services.apigateway.mockservice.model.MyServiceException;
import software.amazon.awssdk.services.apigateway.mockservice.model.NotFoundException;
import software.amazon.awssdk.services.apigateway.mockservice.model.PutApikeyRequest;
import software.amazon.awssdk.services.apigateway.mockservice.model.PutApikeyResult;
import software.amazon.awssdk.services.apigateway.mockservice.model.PutComplexContainersInput;
import software.amazon.awssdk.services.apigateway.mockservice.model.PutContainersInput;
import software.amazon.awssdk.services.apigateway.mockservice.model.PutCustomauthScalarsRequest;
import software.amazon.awssdk.services.apigateway.mockservice.model.PutCustomauthScalarsResult;
import software.amazon.awssdk.services.apigateway.mockservice.model.PutDefinitionsInput;
import software.amazon.awssdk.services.apigateway.mockservice.model.PutIamauthScalarsRequest;
import software.amazon.awssdk.services.apigateway.mockservice.model.PutIamauthScalarsResult;
import software.amazon.awssdk.services.apigateway.mockservice.model.PutNoauthAnonListsRequest;
import software.amazon.awssdk.services.apigateway.mockservice.model.PutNoauthAnonListsResult;
import software.amazon.awssdk.services.apigateway.mockservice.model.PutNoauthAnonMapRequest;
import software.amazon.awssdk.services.apigateway.mockservice.model.PutNoauthAnonMapResult;
import software.amazon.awssdk.services.apigateway.mockservice.model.PutNoauthAnonObjectRequest;
import software.amazon.awssdk.services.apigateway.mockservice.model.PutNoauthAnonObjectResult;
import software.amazon.awssdk.services.apigateway.mockservice.model.PutNoauthContainersComplexRequest;
import software.amazon.awssdk.services.apigateway.mockservice.model.PutNoauthContainersComplexResult;
import software.amazon.awssdk.services.apigateway.mockservice.model.PutNoauthContainersSimpleRequest;
import software.amazon.awssdk.services.apigateway.mockservice.model.PutNoauthContainersSimpleResult;
import software.amazon.awssdk.services.apigateway.mockservice.model.PutNoauthDefsRequest;
import software.amazon.awssdk.services.apigateway.mockservice.model.PutNoauthDefsResult;
import software.amazon.awssdk.services.apigateway.mockservice.model.PutNoauthMultiLocationRequest;
import software.amazon.awssdk.services.apigateway.mockservice.model.PutNoauthRefsCanonListOfScalarsRequest;
import software.amazon.awssdk.services.apigateway.mockservice.model.PutNoauthRefsCanonListOfScalarsResult;
import software.amazon.awssdk.services.apigateway.mockservice.model.PutNoauthRefsCanonListRequest;
import software.amazon.awssdk.services.apigateway.mockservice.model.PutNoauthRefsCanonListResult;
import software.amazon.awssdk.services.apigateway.mockservice.model.PutNoauthRefsCanonMapRequest;
import software.amazon.awssdk.services.apigateway.mockservice.model.PutNoauthRefsCanonMapResult;
import software.amazon.awssdk.services.apigateway.mockservice.model.PutNoauthRefsCanonPrimitiveRequest;
import software.amazon.awssdk.services.apigateway.mockservice.model.PutNoauthRefsCanonPrimitiveResult;
import software.amazon.awssdk.services.apigateway.mockservice.model.PutNoauthScalarsRequest;
import software.amazon.awssdk.services.apigateway.mockservice.model.PutNoauthScalarsResult;
import software.amazon.awssdk.services.apigateway.mockservice.model.ScalarTypes;
import software.amazon.awssdk.test.AWSIntegrationTestBase;
import software.amazon.awssdk.util.BinaryUtils;
import software.amazon.awssdk.util.ImmutableMapParameter;

public class ServiceIntegrationTest extends AWSIntegrationTestBase {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private MyService client;

    private static void assertJsonEquals(Object expected, Object actual) throws IOException {
        assertEquals(toTree(expected), toTree(actual));
    }

    private static JsonNode toTree(Object result) throws IOException {
        return MAPPER.readTree(MAPPER.writeValueAsBytes(result));
    }

    @Before
    public void setup() {
        BasicConfigurator.configure();
        client = MyService.builder()
                          .iamCredentials(new AWSStaticCredentialsProvider(getCredentials()))
                          .signer(request -> "allow")
                          .apiKey("pcJeQMyqaZ4UIedQcnmN84ln4PGIzXWE8KugLPhI")
                          .build();
    }

    @Test
    public void putScalarTypes() throws IOException {
        final PutNoauthScalarsRequest request = new PutNoauthScalarsRequest()
                .scalarTypes(new ScalarTypes()
                                     .stringMember("strMember")
                                     .integerMember(4238)
                                     .numberMember(900.1)
                                     .booleanMember(true)
                                     .memberNameWithDashes("memberDashesVal")
                                     .memberNameWithUnderscores("memberUnderscoresVal")
                                     .memberNameWithBangs("memberBangsVal")
                                     .foo123("foo123val")
                                     .fooBar("foobarval")
                                     .foo("foo"));
        final PutNoauthScalarsResult result = client.putNoauthScalars(request);
        assertJsonEquals(request, result);
    }

    @Test
    public void putAnonymousLists() throws IOException {
        final PutNoauthAnonListsRequest request = new PutNoauthAnonListsRequest()
                .anonListsInput(new AnonListsInput()
                                        .anonymousList(new AnonymousListItem().strProperty("foo"),
                                                       new AnonymousListItem().strProperty("bar")));
        final PutNoauthAnonListsResult result = client.putNoauthAnonLists(request);
        assertJsonEquals(request, result);
    }

    /**
     * Anonymous maps not supported in exporter yet
     */
    @Test
    public void putAnonymousMaps() throws IOException {
        final PutNoauthAnonMapRequest request = new PutNoauthAnonMapRequest()
                .anonMapInput(new AnonMapInput()
                                      .addAnonMapStringToStringEntry("keyOne", "valOne")
                                      .addAnonMapStringToStringEntry("keyTwo", "valTwo"));
        final PutNoauthAnonMapResult result = client.putNoauthAnonMap(request);
        assertJsonEquals(request, result);
    }

    @Test
    public void putAnonymousObject() throws IOException {
        final PutNoauthAnonObjectRequest request = new PutNoauthAnonObjectRequest()
                .anonObjectInput(new AnonObjectInput()
                                         .anonymousObject(new AnonymousObject().strProperty("foo")));
        final PutNoauthAnonObjectResult result = client.putNoauthAnonObject(request);
        assertJsonEquals(request, result);
    }

    @Test
    public void putSimpleContainers() throws IOException {
        final PutNoauthContainersSimpleRequest request = new PutNoauthContainersSimpleRequest()
                .putContainersInput(new PutContainersInput()
                                            .listOfStrings("foo", "bar", "baz")
                                            .listOfIntegers(1, 2, 3, 4, 42)
                                            .listOfDoubles(to_d(123.456), to_d(9000.1), to_d(42.42))
                                            .listOfBooleans(true, false, true)
                                            .mapOfStringToString(ImmutableMapParameter.of("keyOne", "foo", "keyTwo", "bar"))
                                            .mapOfStringToInteger(ImmutableMapParameter.of("keyOne", 42, "keyTwo", 9001))
                                            .mapOfStringToDouble(ImmutableMapParameter.of("keyOne", 123.4, "keyTwo", 456.7))
                                            .mapOfStringToBoolean(ImmutableMapParameter.of("keyOne", true, "keyTwo", false)));
        final PutNoauthContainersSimpleResult result = client.putNoauthContainersSimple(request);
        assertJsonEquals(request, result);
    }

    /**
     * TODO maps of maps not working
     */
    @Test
    public void putComplexContainers() throws Exception {
        Map<String, List<String>> mapStringToStringList = ImmutableMapParameter.of(
                "keyOne", Arrays.asList("foo", "bar", "baz"),
                "keyTwo", Arrays.asList("one", "two", "three"));
        Map<String, List<ScalarTypes>> mapStringToScalarTypesList = ImmutableMapParameter.of(
                "keyOne", Arrays.asList(new ScalarTypes().stringMember("foo"), new ScalarTypes().booleanMember(true)),
                "keyTwo", Collections.singletonList(new ScalarTypes().integerMember(42)));
        PutNoauthContainersComplexRequest request = new PutNoauthContainersComplexRequest()
                .putComplexContainersInput(new PutComplexContainersInput()
                                                   .listOfStructures(new ScalarTypes().stringMember("foobar"),
                                                                     new ScalarTypes().integerMember(42))
                                                   .mapOfStringToListOfString(mapStringToStringList)
                                                   .mapOfStringToStructure(ImmutableMapParameter.of(
                                                           "keyOne", new ScalarTypes().stringMember("baz"),
                                                           "keyTwo", new ScalarTypes().numberMember(9000.1)))
                                                   .mapOfStringToListOfStructure(mapStringToScalarTypesList)
                                          );
        final PutNoauthContainersComplexResult result = client.putNoauthContainersComplex(request);
        assertJsonEquals(request, result);
    }

    @Test
    public void putInlineDefinitions() throws IOException {
        final PutNoauthDefsRequest request = new PutNoauthDefsRequest()
                .putDefinitionsInput(new PutDefinitionsInput()
                                             .inlineRefMember(new InlineRefMember().strProperty("foo")));
        final PutNoauthDefsResult result = client.putNoauthDefs(request);
        assertJsonEquals(request, result);
    }

    @Test
    public void canonicalListReferenceAsPayloadMember() throws IOException {
        final PutNoauthRefsCanonListRequest request = new PutNoauthRefsCanonListRequest()
                .listOfStrings("foo", "bar", "baz");
        final PutNoauthRefsCanonListResult result = client.putNoauthRefsCanonList(request);
        assertJsonEquals(request, result);
    }

    @Test
    public void primitiveStringMemberAsPayloadMember() throws IOException {
        final PutNoauthRefsCanonPrimitiveRequest request = new PutNoauthRefsCanonPrimitiveRequest()
                .string("foobar");
        final PutNoauthRefsCanonPrimitiveResult result = client.putNoauthRefsCanonPrimitive(request);
        assertJsonEquals(request, result);
    }

    @Test
    public void canonicalMapReferenceAsPayloadMember() throws IOException {
        final PutNoauthRefsCanonMapRequest request = new PutNoauthRefsCanonMapRequest()
                .addMapOfStringToStringEntry("KeyOne", "valOne")
                .addMapOfStringToStringEntry("KeyTwo", "valTwo");
        final PutNoauthRefsCanonMapResult result = client.putNoauthRefsCanonMap(request);
        assertJsonEquals(request, result);
    }

    @Test
    public void apiGatewayMetadataIsLoadedIntoBaseResult() {
        GetNoauthScalarsResult result = client.getNoauthScalars(new GetNoauthScalarsRequest());
        assertThat(result.sdkResponseMetadata().requestId(),
                   CoreMatchers.not(isEmptyOrNullString()));
    }

    @Test
    public void httpMetadataIsLoadedIntoBaseResult() {
        GetNoauthScalarsResult result = client.getNoauthScalars(new GetNoauthScalarsRequest());
        assertEquals(200, result.sdkResponseMetadata().httpStatusCode());
        assertTrue(result.sdkResponseMetadata().header("X-Amz-Cf-Id").isPresent());
    }

    @Test
    public void customAuth_AccessAllowed_ReturnsSuccess() throws IOException {
        PutCustomauthScalarsRequest request = new PutCustomauthScalarsRequest()
                .scalarTypes(new ScalarTypes().stringMember("foo"));
        final PutCustomauthScalarsResult result = client.putCustomauthScalars(request);
        assertJsonEquals(request, result);
    }

    @Test
    public void customAuth_AccessDenied_ThrowsException() {
        final MyService customAuthClient = MyService.builder()
                                                    .signer(request -> "deny")
                                                    .build();
        assertForbiddenException(() -> customAuthClient
                .putCustomauthScalars(new PutCustomauthScalarsRequest().scalarTypes(
                        new ScalarTypes().stringMember("foo"))));
    }

    @Test
    public void iamAuth_ValidCredentials_ReturnsSuccess() throws IOException {
        final PutIamauthScalarsRequest request = new PutIamauthScalarsRequest().scalarTypes(
                new ScalarTypes().stringMember("foo"));
        final PutIamauthScalarsResult result = client.putIamauthScalars(request);
        assertJsonEquals(request, result);
    }

    @Test
    public void iamAuth_InvalidCredentials_ThrowsException() {
        final MyService iamAuthClient = MyService.builder()
                                                 .iamCredentials(new AWSStaticCredentialsProvider(
                                                         new BasicAWSCredentials("akid", "skid")))
                                                 .build();
        assertForbiddenException(() -> iamAuthClient.putIamauthScalars(
                new PutIamauthScalarsRequest().scalarTypes(
                        new ScalarTypes().stringMember("foo"))));
    }

    @Test
    public void apiKeyRequired_ValidApiKey_ReturnsSuccess() throws IOException {
        final PutApikeyRequest request = new PutApikeyRequest().scalarTypes(
                new ScalarTypes().stringMember("foo"));
        final PutApikeyResult result = client.putApikey(request);
        assertJsonEquals(request, result);
    }

    @Test
    public void apiKeyRequired_MissingApiKey_ThrowsException() throws IOException {
        final MyService missingApiKeyClient = MyService.builder()
                                                       .build();
        final PutApikeyRequest request = new PutApikeyRequest().scalarTypes(
                new ScalarTypes().stringMember("foo"));
        assertForbiddenException(() -> missingApiKeyClient.putApikey(request));
    }

    @Test
    public void apiKeyRequired_InvalidApiKey_ThrowsException() throws IOException {
        final MyService invalidApiKeyClient = MyService.builder()
                                                       .apiKey("invalidApiKey")
                                                       .build();
        final PutApikeyRequest request = new PutApikeyRequest().scalarTypes(
                new ScalarTypes().stringMember("foo"));
        assertForbiddenException(() -> invalidApiKeyClient.putApikey(request));
    }

    @Test
    public void membersBoundToPathHeadersAndQuery_MarshallsCorrectly() {
        final PutNoauthMultiLocationRequest request = new PutNoauthMultiLocationRequest()
                .multiLocation("pathvalue")
                .queryParamOne("queryOneVal")
                .queryParamTwo("queryTwoVal")
                .headerOne("headerOneVal")
                .headerTwo("headerTwoVal")
                .scalarTypes(new ScalarTypes().stringMember("foobar"));
        client.putNoauthMultiLocation(request);
    }

    @Test
    public void getScalars() {
        GetNoauthScalarsResult result = client.getNoauthScalars(new GetNoauthScalarsRequest());
        // This actually is hardcoded in the mock integration in APIG. We could map query params and/or
        // headers to the Lambda echo function but there hasn't been any motivation to do so.
        assertEquals("foo", result.getScalarTypes().getStringMember());
        assertEquals(Double.valueOf(9000.1), result.getScalarTypes().getNumberMember());
        assertEquals(Integer.valueOf(42), result.getScalarTypes().getIntegerMember());
        assertEquals(Boolean.FALSE, result.getScalarTypes().getBooleanMember());
    }

    @Test
    public void lowercaseOperationName() throws IOException {
        LowercaseOperationNameRequest request = new LowercaseOperationNameRequest()
                .scalarTypes(new ScalarTypes().stringMember("foo"));
        final LowercaseOperationNameResult result = client.lowercaseOperationName(request);
        assertJsonEquals(request, result);
    }

    @Test
    public void lists() throws IOException {
        PutNoauthRefsCanonListOfScalarsRequest request = new PutNoauthRefsCanonListOfScalarsRequest()
                .listOfScalars(new ScalarTypes().foo("val1"), new ScalarTypes().foo("val2"));
        final PutNoauthRefsCanonListOfScalarsResult result = client.putNoauthRefsCanonListOfScalars(request);
        assertJsonEquals(request, result);
    }

    @Test
    public void listBoundToPayload_NullValueForList_RoundtripsNull() throws IOException {
        PutNoauthRefsCanonListOfScalarsRequest request = new PutNoauthRefsCanonListOfScalarsRequest();
        final PutNoauthRefsCanonListOfScalarsResult result = client.putNoauthRefsCanonListOfScalars(request);
        assertJsonEquals(request, result);
    }

    @Test
    public void objectBoundToPayload_NullValueForObject_RoundtripsNull() throws IOException {
        PutNoauthScalarsRequest request = new PutNoauthScalarsRequest();
        final PutNoauthScalarsResult result = client.putNoauthScalars(request);
        assertJsonEquals(request, result);
    }

    @Test
    public void primitiveBoundToPayload_NullValueForPrimitive_RoundtripsNull() throws IOException {
        PutNoauthRefsCanonPrimitiveRequest request = new PutNoauthRefsCanonPrimitiveRequest();
        final PutNoauthRefsCanonPrimitiveResult result = client.putNoauthRefsCanonPrimitive(request);
        assertJsonEquals(request, result);
    }

    @Test
    public void mapBoundToPayload_NullValueForMap_RoundtripsNull() throws IOException {
        PutNoauthRefsCanonMapRequest request = new PutNoauthRefsCanonMapRequest();
        final PutNoauthRefsCanonMapResult result = client.putNoauthRefsCanonMap(request);
        assertJsonEquals(request, result);
    }

    @Test
    public void resourceNotFound() {
        try {
            client.getNoauthErrors(
                    new GetNoauthErrorsRequest().errorType("ResourceNotFoundException"));
        } catch (NotFoundException e) {
            assertEquals(e.getResourceName(), "foo");
            assertEquals(e.sdkHttpMetadata().httpStatusCode(), 404);
        }
    }

    @Test
    public void resourceNotFound_CatchServiceBaseException() {
        try {
            client.getNoauthErrors(
                    new GetNoauthErrorsRequest().errorType("ResourceNotFoundException"));
        } catch (MyServiceException e) {
            assertThat(e, IsInstanceOf.instanceOf(NotFoundException.class));
            assertEquals(e.sdkHttpMetadata().httpStatusCode(), 404);
            System.out.println("Class = " + e.getClass());
            System.out.println("Message = " + e.getMessage());
            System.out.println("Status Code = " + e.sdkHttpMetadata().httpStatusCode());
            System.out.println(
                    "Raw Body = " +
                    new String(BinaryUtils.copyAllBytesFrom(
                            e.sdkHttpMetadata().responseContent())));
            e.printStackTrace();
        }
    }

    private void assertForbiddenException(Runnable runnable) {
        try {
            runnable.run();
        } catch (MyServiceException expected) {
            assertEquals(403, expected.sdkHttpMetadata().httpStatusCode());
        }
    }

    /**
     * Handle the exception by catching the SdkBaseException which handles all service and client exceptions returned by the SDK.
     *
     * Its recommended to catch the exact exception type or base service exception as they contain useful information like status
     * code, request id etc.
     */
    @Test
    public void resourceNotFound_CatchSdkBaseException() {
        try {
            client.getNoauthErrors(
                    new GetNoauthErrorsRequest().errorType("ResourceNotFoundException"));
        } catch (SdkBaseException e) {
            assertThat(e, IsInstanceOf.instanceOf(NotFoundException.class));
            System.out.println("Class = " + e.getClass());
            System.out.println("Message = " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Test
    public void internalErrorException() {
        try {
            client.getNoauthErrors(new GetNoauthErrorsRequest().errorType("InternalError"));
        } catch (InternalServerErrorException e) {
            assertEquals(e.getHostId(), "abcd-1234");
            assertEquals(e.sdkHttpMetadata().httpStatusCode(), 500);
        }
    }

    private Double to_d(double d) {
        return d;
    }
}
