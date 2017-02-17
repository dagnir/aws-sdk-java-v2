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

package software.amazon.awssdk.services.s3.error;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Map;
import javax.xml.stream.XMLStreamException;
import org.junit.Test;
import software.amazon.awssdk.AmazonServiceException.ErrorType;
import software.amazon.awssdk.DefaultRequest;
import software.amazon.awssdk.Request;
import software.amazon.awssdk.http.HttpMethodName;
import software.amazon.awssdk.http.HttpResponse;
import software.amazon.awssdk.services.s3.Headers;
import software.amazon.awssdk.services.s3.internal.S3ErrorResponseHandler;
import software.amazon.awssdk.services.s3.model.AmazonS3Exception;

public class ErrorResponseParseTest {

    private static final S3ErrorResponseHandler errorHandler = new S3ErrorResponseHandler();

    /**
     * Loads the error xmls from the resources directory and returns it as a
     * stream.
     */
    private static InputStream loadFileAsStream(String fileName)
            throws FileNotFoundException {
        File errorResponseFile = new File(ErrorResponseParseTest.class.getResource("/resources/errorResponse/" + fileName).getFile());
        return new FileInputStream(errorResponseFile);
    }

    /**
     * Tests by setting only the headers and no response stream. Asserts that
     * the values returned from the AmazonS3Exception is same as the one that
     * was set.
     */
    @Test
    public void testErrorParsingWithOnlyHeadersNullInputStream()
            throws Exception {

        final String code = "TestCode";
        final String message = "TestErrorMessage";
        final int statusCode = 501;
        final String requestId = "requestId";
        final String extendedRequestId = "extendedRequestId";

        HttpResponse response = new HttpResponse(null, null);
        response.addHeader("Code", code);
        response.addHeader(Headers.REQUEST_ID, requestId);
        response.addHeader(Headers.EXTENDED_REQUEST_ID, extendedRequestId);
        response.setStatusText(message);
        response.setStatusCode(statusCode);

        AmazonS3Exception s3Exception = (AmazonS3Exception) errorHandler
                .handle(response);

        assertNotNull(s3Exception);
        assertEquals(statusCode + " " + message, s3Exception.getErrorCode());
        assertEquals(message, s3Exception.getErrorMessage());
        assertEquals(statusCode, s3Exception.getStatusCode());
        assertEquals(ErrorType.Service, s3Exception.getErrorType());
        assertEquals(requestId, s3Exception.getRequestId());
        assertEquals(extendedRequestId, s3Exception.getExtendedRequestId());

    }

    /**
     * Tests Error parsing for a HEAD request in which the response has only
     * headers. Asserts that the values returned from the AmazonS3Exception is
     * same as the one that was set.
     */
    @Test
    public void testErrorResponseWithHEADRequest() throws Exception {
        final String code = "TestCode";
        final String message = "TestErrorMessage";
        final int statusCode = 501;
        final String requestId = "requestId";
        final String extendedRequestId = "extendedRequestId";

        Request request = new DefaultRequest("testService");
        request.setHttpMethod(HttpMethodName.HEAD);
        HttpResponse response = new HttpResponse(request, null);
        response.addHeader("Code", code);
        response.addHeader(Headers.REQUEST_ID, requestId);
        response.addHeader(Headers.EXTENDED_REQUEST_ID, extendedRequestId);
        response.setStatusText(message);
        response.setStatusCode(statusCode);
        response.setContent(new ByteArrayInputStream(new byte[0]));

        AmazonS3Exception s3Exception = (AmazonS3Exception) errorHandler
                .handle(response);

        assertNotNull(s3Exception);
        assertEquals(statusCode + " " + message, s3Exception.getErrorCode());
        assertEquals(message, s3Exception.getErrorMessage());
        assertEquals(statusCode, s3Exception.getStatusCode());
        assertEquals(ErrorType.Service, s3Exception.getErrorType());
        assertEquals(requestId, s3Exception.getRequestId());
        assertEquals(extendedRequestId, s3Exception.getExtendedRequestId());
    }

    /**
     * Tests parsing of an error xml which has only Code, Message, RequestId,
     * HostId tags.
     */
    @Test
    public void testErrorResponseWithBasicErrorXml() throws Exception {
        final String fileName = "ErrorResponseBasicTags.xml";
        final int statusCode = 501;
        Request request = new DefaultRequest("testService");
        request.setHttpMethod(HttpMethodName.POST);
        HttpResponse response = new HttpResponse(request, null);
        response.setContent(loadFileAsStream(fileName));
        response.setStatusCode(statusCode);

        AmazonS3Exception s3Exception = (AmazonS3Exception) errorHandler
                .handle(response);

        assertNotNull(s3Exception);
        assertNotNull(s3Exception.getErrorResponseXml());
        assertEquals("TestCode", s3Exception.getErrorCode());
        assertEquals("TestMessage", s3Exception.getErrorMessage());
        assertEquals(statusCode, s3Exception.getStatusCode());
        assertEquals(ErrorType.Service, s3Exception.getErrorType());
        assertEquals("TestRequestId", s3Exception.getRequestId());
        assertEquals("TestExtendedRequestId",
                     s3Exception.getExtendedRequestId());
    }

    /**
     * Tests with an error xml that has tags other than the default tags.
     * Asserts that these tags go into additional details.
     */
    @Test
    public void testErrorResponseWithAdditionalDetailsMentioned()
            throws Exception {
        final String fileName = "ErrorResponseWithAdditionalDetails.xml";
        final int statusCode = 501;
        Request request = new DefaultRequest("testService");
        request.setHttpMethod(HttpMethodName.POST);
        HttpResponse response = new HttpResponse(request, null);
        response.setContent(loadFileAsStream(fileName));
        response.setStatusCode(statusCode);

        AmazonS3Exception s3Exception = (AmazonS3Exception) errorHandler
                .handle(response);

        assertNotNull(s3Exception);
        assertNotNull(s3Exception.getErrorResponseXml());
        assertEquals("TestCode", s3Exception.getErrorCode());
        assertEquals("TestMessage", s3Exception.getErrorMessage());
        assertEquals(statusCode, s3Exception.getStatusCode());
        assertEquals(ErrorType.Service, s3Exception.getErrorType());
        assertEquals("TestRequestId", s3Exception.getRequestId());
        assertEquals("TestExtendedRequestId",
                     s3Exception.getExtendedRequestId());
        Map<String, String> additionalDetails = s3Exception
                .getAdditionalDetails();
        assertNotNull(additionalDetails);
        assertNotNull(additionalDetails.size() == 4);
        assertTrue(additionalDetails.get("AdditionalDetails1").equals(
                "AdditionalDetails1"));
        assertTrue(additionalDetails.get("AdditionalDetails2").equals(
                "AdditionalDetails2"));
        assertTrue(additionalDetails.get("AdditionalDetails3").equals(
                "AdditionalDetails3"));
        assertTrue(additionalDetails.get("AdditionalDetails4").equals(
                "AdditionalDetails4"));
    }

    /**
     * Tests error parsing for an invalid xml. In this case, the exception must
     * be formed from the headers.
     */
    @Test
    public void testErrorResponseWithXMLNotInProperFormat() throws Exception {
        final String fileName = "ErrorResponseXMLNotProperFormat.xml";
        final int statusCode = 501;
        Request request = new DefaultRequest("testService");
        HttpResponse response = new HttpResponse(request, null);
        response.setContent(loadFileAsStream(fileName));
        response.setStatusCode(statusCode);

        AmazonS3Exception s3Exception = (AmazonS3Exception) errorHandler
                .handle(response);

        assertNotNull(s3Exception);
        assertNotNull(s3Exception.getErrorResponseXml());
        assertNotNull(s3Exception.getErrorCode());
        assertNull(s3Exception.getErrorMessage());
        assertNotNull(s3Exception.getStatusCode());
        assertNotNull(s3Exception.getErrorType());
        assertNull(s3Exception.getAdditionalDetails());
    }

    /**
     * Tests error parsing for an invalid xml in which no Error Tag is present.
     * In this case, the exception must be formed from the headers.
     */
    @Test
    public void testErrorResponseWithNoErrorTagInXML() throws Exception {
        final String fileName = "ErrorResponseNoErrorTag.xml";
        final int statusCode = 501;
        Request request = new DefaultRequest("testService");
        request.setHttpMethod(HttpMethodName.POST);
        HttpResponse response = new HttpResponse(request, null);
        response.setContent(loadFileAsStream(fileName));
        response.setStatusCode(statusCode);

        AmazonS3Exception s3Exception = (AmazonS3Exception) errorHandler
                .handle(response);

        assertNotNull(s3Exception);
        assertNotNull(s3Exception.getErrorResponseXml());
        assertNotNull(s3Exception.getErrorCode());
        assertNull(s3Exception.getErrorMessage());
        assertNotNull(s3Exception.getStatusCode());
        assertNotNull(s3Exception.getErrorType());
        assertNull(s3Exception.getAdditionalDetails());
    }

    /**
     * Tests error parsing for an invalid xml in which there are sub tags.
     * In this case, the exception must be formed from the headers.
     */
    @Test
    public void testErrorResponseWithChildTags() throws Exception {
        final String fileName = "ErrorResponseChildTags.xml";
        final int statusCode = 501;
        Request request = new DefaultRequest("testService");
        request.setHttpMethod(HttpMethodName.POST);
        HttpResponse response = new HttpResponse(request, null);
        response.setContent(loadFileAsStream(fileName));
        response.setStatusCode(statusCode);

        AmazonS3Exception s3Exception = (AmazonS3Exception) errorHandler
                .handle(response);

        assertNotNull(s3Exception);
        assertNotNull(s3Exception.getErrorResponseXml());
        assertNotNull(s3Exception.getErrorCode());
        assertNull(s3Exception.getErrorMessage());
        assertNotNull(s3Exception.getStatusCode());
        assertNotNull(s3Exception.getErrorType());
        assertNull(s3Exception.getAdditionalDetails());
    }

    /**
     * Tests error parsing where the root element is not a Error Tag. In this
     * the exception must be formed from the headers.
     */
    @Test
    public void testErrorResponseWithRootElementAsNotErrorTag()
            throws FileNotFoundException, XMLStreamException {
        final String fileName = "ErrorResponseRootElementAsFoo.xml";
        final int statusCode = 501;
        Request request = new DefaultRequest("testService");
        request.setHttpMethod(HttpMethodName.POST);
        HttpResponse response = new HttpResponse(request, null);
        response.setContent(loadFileAsStream(fileName));
        response.setStatusCode(statusCode);

        AmazonS3Exception s3Exception = (AmazonS3Exception) errorHandler
                .handle(response);

        assertNotNull(s3Exception);
        assertNotNull(s3Exception.getErrorResponseXml());
        assertNotNull(s3Exception.getErrorCode());
        assertNull(s3Exception.getErrorMessage());
        assertNotNull(s3Exception.getStatusCode());
        assertNotNull(s3Exception.getErrorType());
        assertNull(s3Exception.getAdditionalDetails());
    }

    /**
     * Tests error parsing where the xml is repeated in the response. In this
     * the exception must be formed from the headers.
     */
    @Test
    public void testErrorResponseWithRepeatedXmls()
            throws FileNotFoundException, XMLStreamException {
        final String fileName = "ErrorResponseRepeatedXml.xml";
        final int statusCode = 501;
        Request request = new DefaultRequest("testService");
        request.setHttpMethod(HttpMethodName.POST);
        HttpResponse response = new HttpResponse(request, null);
        response.setContent(loadFileAsStream(fileName));
        response.setStatusCode(statusCode);

        AmazonS3Exception s3Exception = (AmazonS3Exception) errorHandler
                .handle(response);

        assertNotNull(s3Exception);
        assertNotNull(s3Exception.getErrorResponseXml());
        assertNotNull(s3Exception.getErrorCode());
        assertNull(s3Exception.getErrorMessage());
        assertNotNull(s3Exception.getStatusCode());
        assertNotNull(s3Exception.getErrorType());
        assertNull(s3Exception.getAdditionalDetails());
    }

    /**
     * Tests error parsing where the xml is having an element that is same as
     * the &lt;Error&gt; tag.
     */
    @Test
    public void testErrorResponseWithChildTagError()
            throws FileNotFoundException, XMLStreamException {
        final String fileName = "ErrorResponseChildTagError.xml";
        final int statusCode = 501;
        Request request = new DefaultRequest("testService");
        request.setHttpMethod(HttpMethodName.POST);
        HttpResponse response = new HttpResponse(request, null);
        response.setContent(loadFileAsStream(fileName));
        response.setStatusCode(statusCode);

        AmazonS3Exception s3Exception = (AmazonS3Exception) errorHandler
                .handle(response);

        assertNotNull(s3Exception);
        assertNotNull(s3Exception.getErrorResponseXml());
        assertEquals("TestCode", s3Exception.getErrorCode());
        assertEquals("TestMessage", s3Exception.getErrorMessage());
        assertEquals(statusCode, s3Exception.getStatusCode());
        assertEquals(ErrorType.Service, s3Exception.getErrorType());
        assertEquals("TestRequestId", s3Exception.getRequestId());
        assertEquals("TestExtendedRequestId",
                     s3Exception.getExtendedRequestId());
        assertNotNull(s3Exception.getAdditionalDetails());
        assertEquals("TestErrorResponse", s3Exception.getAdditionalDetails().get("Error"));
    }

}
