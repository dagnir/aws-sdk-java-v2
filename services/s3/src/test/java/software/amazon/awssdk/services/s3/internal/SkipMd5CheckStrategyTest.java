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

package software.amazon.awssdk.services.s3.internal;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.UnsupportedEncodingException;
import org.junit.After;
import org.junit.Test;
import software.amazon.awssdk.services.s3.Headers;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ObjectMetadata;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3ObjectId;
import software.amazon.awssdk.services.s3.model.SSEAwsKeyManagementParams;
import software.amazon.awssdk.services.s3.model.SSECustomerKey;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.util.StringInputStream;

public class SkipMd5CheckStrategyTest {

    private final SkipMd5CheckStrategy strategy = SkipMd5CheckStrategy.INSTANCE;

    @After
    public void tearDown() {
        System.clearProperty(SkipMd5CheckStrategy.DISABLE_GET_OBJECT_MD5_VALIDATION_PROPERTY);
        System.clearProperty(SkipMd5CheckStrategy.DISABLE_PUT_OBJECT_MD5_VALIDATION_PROPERTY);
    }

    @Test
    public void getObject_NormalRequest_ShouldNotSkipClientSideValidation() {
        GetObjectRequest request = newGetObjectRequest();
        assertFalse(strategy.skipClientSideValidationPerRequest(request));
    }

    @Test
    public void getObject_NormalRequestAndNormalResponse_ShouldNotSkipClientSideValidation() {
        GetObjectRequest request = newGetObjectRequest();
        assertFalse(strategy.skipClientSideValidation(request, newObjectMetadata()));
    }

    @Test
    public void getObject_NormalRequestAndEmptyResponse_ShouldSkipClientSideValidation() {
        GetObjectRequest request = newGetObjectRequest();
        // Etag in Object Metadata will be null so nothing to validate with
        assertTrue(strategy.skipClientSideValidation(request, new ObjectMetadata()));
    }

    @Test
    public void getObject_NormalRequestAndNullResponse_ShouldSkipClientSideValidation() {
        GetObjectRequest request = newGetObjectRequest();
        assertTrue(strategy.skipClientSideValidation(request, null));
    }

    @Test
    public void getObject_RangeGet_ShouldSkipClientSideValidation() {
        GetObjectRequest request = newGetObjectRequest();
        request.setRange(0, Long.MAX_VALUE);
        assertTrue(strategy.skipClientSideValidation(request, newObjectMetadata()));
    }

    @Test
    public void getObject_GetObjectValidationDisabledByProperty_ShouldSkipClientSideValidation() {
        System.setProperty(SkipMd5CheckStrategy.DISABLE_GET_OBJECT_MD5_VALIDATION_PROPERTY, "true");
        GetObjectRequest request = newGetObjectRequest();
        assertTrue(strategy.skipClientSideValidationPerRequest(request));
    }

    @Test
    public void getObject_GetObjectValidationDisabledByProperty_ShouldSkipClientSideValidationPerResponse() {
        System.setProperty(SkipMd5CheckStrategy.DISABLE_GET_OBJECT_MD5_VALIDATION_PROPERTY, "true");
        assertTrue(strategy.skipClientSideValidationPerGetResponse(newObjectMetadata()));
    }

    @Test
    public void getObject_InvolvesSseC_ShouldSkipClientSideValidation() {
        GetObjectRequest request = newGetObjectRequest();
        request.setSSECustomerKey(new SSECustomerKey("some-key"));
        assertTrue(strategy.skipClientSideValidationPerRequest(request));
    }

    @Test
    public void putObject_NormalRequest_ShouldNotSkipClientSideValidation() throws UnsupportedEncodingException {
        PutObjectRequest request = newPutObjectRequest();
        assertFalse(strategy.skipClientSideValidationPerRequest(request));
    }

    @Test
    public void putObject_NormalResponse_ShouldNotSkipClientSideValidation() throws UnsupportedEncodingException {
        assertFalse(strategy.skipClientSideValidationPerPutResponse(newObjectMetadata()));
    }

    @Test
    public void putObject_NormalRequest_ShouldNotSkipServerSideValidation() throws UnsupportedEncodingException {
        PutObjectRequest request = newPutObjectRequest();
        assertFalse(strategy.skipServerSideValidation(request));
    }

    /**
     * The Content-MD5 header is still valid to send in requests involving SSE as the plaintext will
     * be validated server side before it's encrypted
     */
    @Test
    public void putObject_InvolvesSse_ShouldNotSkipServerSideValidation() throws UnsupportedEncodingException {
        PutObjectRequest request = newPutObjectRequest();
        request.setMetadata(new ObjectMetadata());
        request.getMetadata().setSSEAlgorithm("some-algorithm");
        assertFalse(strategy.skipServerSideValidation(request));
    }

    /**
     * The Content-MD5 header is still valid to send in requests involving SSE as the plaintext will
     * be validated server side before it's encrypted
     */
    @Test
    public void putObject_InvolvesSseC_ShouldNotSkipServerSideValidation() throws UnsupportedEncodingException {
        PutObjectRequest request = newPutObjectRequest();
        request.setSSECustomerKey(new SSECustomerKey("some-key"));
        assertFalse(strategy.skipServerSideValidation(request));
    }

    /**
     * The Content-MD5 header is still valid to send in requests involving SSE as the plaintext will
     * be validated server side before it's encrypted
     */
    @Test
    public void putObject_InvolvesSseKms_ShouldNotSkipServerSideValidation() throws UnsupportedEncodingException {
        PutObjectRequest request = newPutObjectRequest();
        request.setSSEAwsKeyManagementParams(new SSEAwsKeyManagementParams());
        assertFalse(strategy.skipServerSideValidation(request));
    }

    /**
     * Pure SSE (with S3 managed keys) will return an Etag that matches the MD5 of the plaintext so
     * it's okay to do client side validation
     */
    @Test
    public void putObject_InvolvesSse_ShouldNotSkipClientSideValidation() throws UnsupportedEncodingException {
        PutObjectRequest request = newPutObjectRequest();
        request.setMetadata(new ObjectMetadata());
        request.getMetadata().setSSEAlgorithm("some-algorithm");
        assertFalse(strategy.skipClientSideValidationPerRequest(request));
    }

    @Test
    public void putObject_InvolvesSseC_ShouldSkipClientSideValidation() throws UnsupportedEncodingException {
        PutObjectRequest request = newPutObjectRequest();
        request.setSSECustomerKey(new SSECustomerKey("some-key"));
        assertTrue(strategy.skipClientSideValidationPerRequest(request));
    }

    /**
     * The Content-MD5 header is still valid to send in requests involving SSE as the plaintext will
     * be validated server side before it's encrypted
     */
    @Test
    public void putObject_InvolvesSseKms_ShouldSkipClientSideValidation() throws UnsupportedEncodingException {
        PutObjectRequest request = newPutObjectRequest();
        request.setSSEAwsKeyManagementParams(new SSEAwsKeyManagementParams());
        assertTrue(strategy.skipClientSideValidationPerRequest(request));
    }

    @Test
    public void putObject_PutObjectValidationDisabledByProperty_ShouldSkipClientSideValidation()
            throws UnsupportedEncodingException {
        System.setProperty(SkipMd5CheckStrategy.DISABLE_PUT_OBJECT_MD5_VALIDATION_PROPERTY, "true");
        PutObjectRequest request = newPutObjectRequest();
        assertTrue(strategy.skipClientSideValidationPerRequest(request));
    }

    @Test
    public void putObject_PutObjectValidationDisabledByProperty_ShouldSkipServerSideValidation()
            throws UnsupportedEncodingException {
        System.setProperty(SkipMd5CheckStrategy.DISABLE_PUT_OBJECT_MD5_VALIDATION_PROPERTY, "true");
        PutObjectRequest request = newPutObjectRequest();
        assertTrue(strategy.skipServerSideValidation(request));
    }

    @Test
    public void putObject_PutObjectValidationDisabledByProperty_ShouldSkipClientSideValidationPerResponse()
            throws UnsupportedEncodingException {
        System.setProperty(SkipMd5CheckStrategy.DISABLE_PUT_OBJECT_MD5_VALIDATION_PROPERTY, "true");
        assertTrue(strategy.skipClientSideValidationPerPutResponse(newObjectMetadata()));
    }

    @Test
    public void uploadPart_NormalRequest_ShouldNotSkipClientSideValidation() {
        UploadPartRequest request = new UploadPartRequest();
        assertFalse(strategy.skipClientSideValidationPerRequest(request));
    }

    @Test
    public void uploadPart_NormalResponse_ShouldNotSkipClientSideValidation() {
        assertFalse(strategy.skipClientSideValidationPerUploadPartResponse(newObjectMetadata()));
    }

    @Test
    public void uploadPart_NormalRequest_ShouldNotSkipServerSideValidation() {
        UploadPartRequest request = new UploadPartRequest();
        assertFalse(strategy.skipServerSideValidation(request));
    }

    /**
     * The Content-MD5 header is still valid to send in requests involving SSE as the plaintext will
     * be validated server side before it's encrypted
     */
    @Test
    public void uploadPart_InvolvesSseC_ShouldSkipClientSideValidation() {
        UploadPartRequest request = new UploadPartRequest();
        request.setSSECustomerKey(new SSECustomerKey("some-key"));
        assertTrue(strategy.skipClientSideValidationPerRequest(request));
    }

    @Test
    public void uploadPart_PutObjectValidationDisabledByProperty_ShouldSkipClientSideValidation() {
        System.setProperty(SkipMd5CheckStrategy.DISABLE_PUT_OBJECT_MD5_VALIDATION_PROPERTY, "true");
        UploadPartRequest request = new UploadPartRequest();
        assertTrue(strategy.skipClientSideValidationPerRequest(request));
    }

    @Test
    public void uploadPart_PutObjectValidationDisabledByProperty_ShouldSkipServerSideValidation() {
        System.setProperty(SkipMd5CheckStrategy.DISABLE_PUT_OBJECT_MD5_VALIDATION_PROPERTY, "true");
        UploadPartRequest request = new UploadPartRequest();
        assertTrue(strategy.skipServerSideValidation(request));
    }

    @Test
    public void skipValidationPerResponse_MetadataIsNull_ShouldSkipClientSideValidation() {
        assertTrue(strategy.skipClientSideValidationPerGetResponse(null));
    }

    @Test
    public void skipValidationPerResponse_MetadataIsEmpty_ShouldSkipClientSideValidation() {
        assertTrue(strategy.skipClientSideValidationPerGetResponse(new ObjectMetadata()));
    }

    /**
     * Pure SSE (with S3 managed keys) will return an Etag that matches the MD5 of the plaintext so
     * it's okay to do client side validation
     */
    @Test
    public void skipValidationPerResponse_InvolvesSse_ShouldNotSkipClientSideValidation() {
        ObjectMetadata metadata = newObjectMetadata();
        metadata.setSSEAlgorithm("some-algorithm");
        assertFalse(strategy.skipClientSideValidationPerGetResponse(metadata));
    }

    /**
     * The Etag returned in the metadata of a response involving SSE is not the MD5 of the plaintext
     * so we can't validate after the fact client side
     */
    @Test
    public void skipValidationPerResponse_InvolvesSseC_ShouldSkipClientSideValidation() {
        ObjectMetadata metadata = newObjectMetadata();
        metadata.setSSECustomerAlgorithm("some-algorithim");
        metadata.setSSECustomerKeyMd5("md5digest");
        assertTrue(strategy.skipClientSideValidationPerGetResponse(metadata));
    }

    /**
     * The Etag returned in the metadata of a response involving SSE is not the MD5 of the plaintext
     * so we can't validate after the fact client side
     */
    @Test
    public void skipValidationPerResponse_InvolvesSseKms_ShouldSkipClientSideValidation() {
        ObjectMetadata metadata = newObjectMetadata();
        metadata.setSSEAlgorithm("some-kms-algoritim");
        metadata.setHeader(Headers.SERVER_SIDE_ENCRYPTION_AWS_KMS_KEYID, "some-kms-key-id");
        assertTrue(strategy.skipClientSideValidationPerGetResponse(metadata));
    }

    @Test
    public void skipValidationPerResponse_InvolvesMultiPartUpload_ShouldSkipClientSideValidation() {
        ObjectMetadata metadata = newObjectMetadata();
        metadata.setHeader(Headers.ETAG, "cccccccccccccccc-100");
        assertTrue(strategy.skipClientSideValidationPerGetResponse(metadata));
    }

    private PutObjectRequest newPutObjectRequest() throws UnsupportedEncodingException {
        return new PutObjectRequest("some-bucket", "some-key", new StringInputStream("some-content"), null);
    }

    private GetObjectRequest newGetObjectRequest() {
        return new GetObjectRequest(new S3ObjectId("some-bucket", "some-key"));
    }

    private ObjectMetadata newObjectMetadata() {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setHeader(Headers.ETAG, "ccccccccccccccccccc");
        return metadata;
    }
}
