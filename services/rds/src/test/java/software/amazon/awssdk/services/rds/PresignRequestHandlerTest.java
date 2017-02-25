/*
 * Copyright 2011-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance with
 * the License. A copy of the License is located at
 *
 * http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */
package software.amazon.awssdk.services.rds;

import software.amazon.awssdk.Request;
import software.amazon.awssdk.auth.BasicAwsCredentials;
import software.amazon.awssdk.handlers.HandlerContextKey;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.RegionUtils;
import software.amazon.awssdk.runtime.SdkInternalList;
import software.amazon.awssdk.services.rds.model.CopyDBSnapshotRequest;
import software.amazon.awssdk.services.rds.model.Tag;
import software.amazon.awssdk.services.rds.model.transform.CopyDBSnapshotRequestMarshaller;
import org.junit.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit Tests for {@link PresignRequestHandler}
 */
public class PresignRequestHandlerTest {
    private static final BasicAwsCredentials CREDENTIALS = new BasicAwsCredentials("foo", "bar");
    private static final Region DESTINATION_REGION = RegionUtils.getRegion("us-west-2");

    private static PresignRequestHandler<CopyDBSnapshotRequest> presignHandler = new CopyDbSnapshotPresignHandler();
    private final CopyDBSnapshotRequestMarshaller marshaller = new CopyDBSnapshotRequestMarshaller();

    @Test
    public void testSetsPresignedUrl() throws URISyntaxException {
        CopyDBSnapshotRequest request = makeTestRequest();
        presignHandler.beforeRequest(marshallRequest(request));

        assertNotNull(request.getPreSignedUrl());
    }

    @Test
    public void testComputesPresignedUrlCorrectly() throws URISyntaxException {
        // Note: test data was baselined by performing actual calls, with real
        // credentials to RDS and checking that they succeeded. Then the
        // request was recreated with all the same parameters but with test
        // credentials.
        final CopyDBSnapshotRequest request = new CopyDBSnapshotRequest()
                .withSourceDBSnapshotIdentifier("arn:aws:rds:us-east-1:123456789012:snapshot:rds:test-instance-ss-2016-12-20-23-19")
                .withTargetDBSnapshotIdentifier("test-instance-ss-copy-2")
                .withSourceRegion("us-east-1")
                .withKmsKeyId("arn:aws:kms:us-west-2:123456789012:key/11111111-2222-3333-4444-555555555555");

        Calendar c = new GregorianCalendar();
        c.setTimeZone(TimeZone.getTimeZone("UTC"));
        // 20161221T180735Z
        // Note: month is 0-based
        c.set(2016, 11, 21, 18, 7, 35);

        PresignRequestHandler<CopyDBSnapshotRequest> handler = new CopyDbSnapshotPresignHandler(c.getTime());

        handler.beforeRequest(marshallRequest(request));

        final String expectedPreSignedUrl = "https://rds.us-east-1.amazonaws.com/?" +
                "Action=CopyDBSnapshot" +
                "&Version=2014-10-31" +
                "&SourceDBSnapshotIdentifier=arn%3Aaws%3Ards%3Aus-east-1%3A123456789012%3Asnapshot%3Ards%3Atest-instance-ss-2016-12-20-23-19" +
                "&TargetDBSnapshotIdentifier=test-instance-ss-copy-2" +
                "&KmsKeyId=arn%3Aaws%3Akms%3Aus-west-2%3A123456789012%3Akey%2F11111111-2222-3333-4444-555555555555" +
                "&DestinationRegion=us-west-2" +
                "&X-Amz-Algorithm=AWS4-HMAC-SHA256" +
                "&X-Amz-Date=20161221T180735Z" +
                "&X-Amz-SignedHeaders=host" +
                "&X-Amz-Expires=604800" +
                "&X-Amz-Credential=foo%2F20161221%2Fus-east-1%2Frds%2Faws4_request" +
                "&X-Amz-Signature=f839ca3c728dc96e7c978befeac648296b9f778f6724073de4217173859d13d9";

        assertEquals(expectedPreSignedUrl, request.getPreSignedUrl());
    }

    @Test
    public void testSkipsPresigningIfUrlSet() throws URISyntaxException {
        CopyDBSnapshotRequest mockRequest = mock(CopyDBSnapshotRequest.class);
        when(mockRequest.getTags()).thenReturn(new SdkInternalList<Tag>());
        when(mockRequest.getPreSignedUrl()).thenReturn("PRESIGNED");

        presignHandler.beforeRequest(marshallRequest(mockRequest));

        verify(mockRequest, never()).setPreSignedUrl(anyString());
    }

    @Test
    public void testSkipsPresigningIfSourceRegionNotSet() throws URISyntaxException {
        CopyDBSnapshotRequest mockRequest = mock(CopyDBSnapshotRequest.class);
        when(mockRequest.getTags()).thenReturn(new SdkInternalList<Tag>());

        presignHandler.beforeRequest(marshallRequest(mockRequest));

        verify(mockRequest, never()).setPreSignedUrl(anyString());
    }

    @Test
    public void testParsesDestinationRegionfromRequestEndpoint() throws URISyntaxException {
        CopyDBSnapshotRequest request = new CopyDBSnapshotRequest()
                .withSourceRegion("us-east-1");
        Region destination = RegionUtils.getRegion("us-west-2");
        Request<?> marshalled = marshallRequest(request);
        marshalled.setEndpoint(new URI("https://" + destination.getServiceEndpoint("rds")));

        presignHandler.beforeRequest(marshalled);

        final URI presignedUrl = new URI(request.getPreSignedUrl());
        assertTrue(presignedUrl.toString().contains("DestinationRegion=" + destination.getName()));
    }

    @Test
    public void testSourceRegionRemovedFromOriginalRequest() throws URISyntaxException {
        Request<?> marshalled = marshallRequest(makeTestRequest());

        presignHandler.beforeRequest(marshalled);

        assertFalse(marshalled.getParameters().containsKey("SourceRegion"));
    }

    private Request<?> marshallRequest(CopyDBSnapshotRequest request) throws URISyntaxException {
        Request<?> marshalled = marshaller.marshall(request);
        marshalled.setEndpoint(new URI("https://" + DESTINATION_REGION.getServiceEndpoint("rds")));
        marshalled.addHandlerContext(HandlerContextKey.AWS_CREDENTIALS, CREDENTIALS);
        return marshalled;
    }

    private CopyDBSnapshotRequest makeTestRequest() {
        return new CopyDBSnapshotRequest()
                .withSourceDBSnapshotIdentifier("arn:aws:rds:us-east-1:123456789012:snapshot:rds:test-instance-ss-2016-12-20-23-19")
                .withTargetDBSnapshotIdentifier("test-instance-ss-copy-2")
                .withSourceRegion("us-east-1")
                .withKmsKeyId("arn:aws:kms:us-west-2:123456789012:key/11111111-2222-3333-4444-555555555555");
    }
}
