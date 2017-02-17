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

package software.amazon.awssdk.services.s3.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import software.amazon.awssdk.AmazonWebServiceRequest;
import software.amazon.awssdk.Request;
import software.amazon.awssdk.RequestClientOptions;
import software.amazon.awssdk.RequestClientOptions.Marker;
import software.amazon.awssdk.Response;
import software.amazon.awssdk.auth.AwsCredentials;
import software.amazon.awssdk.auth.BasicAwsCredentials;
import software.amazon.awssdk.event.ProgressEvent;
import software.amazon.awssdk.event.ProgressListener;
import software.amazon.awssdk.event.SyncProgressListener;
import software.amazon.awssdk.metrics.RequestMetricCollector;

public class EncryptedPutObjectRequestTest {

    public static void verifyBaseBeforeCopy(final AmazonWebServiceRequest to) {
        assertNull(to.getCustomRequestHeaders());
        assertSame(ProgressListener.NOOP, to.getGeneralProgressListener());
        assertNull(to.getRequestCredentials());
        assertNull(to.getRequestMetricCollector());

        assertTrue(RequestClientOptions.DEFAULT_STREAM_BUFFER_SIZE == to
                .getReadLimit());
        RequestClientOptions toOptions = to.getRequestClientOptions();
        assertNull(toOptions.getClientMarker(Marker.USER_AGENT));
        assertTrue(RequestClientOptions.DEFAULT_STREAM_BUFFER_SIZE == toOptions
                .getReadLimit());
    }

    private static void verifyBaseAfterCopy(final ProgressListener listener,
                                            final AwsCredentials credentials,
                                            final RequestMetricCollector collector,
                                            final AmazonWebServiceRequest from, final AmazonWebServiceRequest to) {
        RequestClientOptions toOptions;
        Map<String, String> headers = to.getCustomRequestHeaders();
        assertTrue(2 == headers.size());
        assertEquals("v1", headers.get("k1"));
        assertEquals("v2", headers.get("k2"));
        assertSame(listener, to.getGeneralProgressListener());
        assertSame(credentials, to.getRequestCredentials());
        assertSame(collector, to.getRequestMetricCollector());

        assertTrue(1234 == to.getReadLimit());
        toOptions = to.getRequestClientOptions();
        assertEquals(
                from.getRequestClientOptions().getClientMarker(
                        Marker.USER_AGENT),
                toOptions.getClientMarker(Marker.USER_AGENT));
        assertTrue(1234 == toOptions.getReadLimit());
    }

    @Test
    public void testClone() {
        File file = new File("somefile");
        EncryptedPutObjectRequest from = new EncryptedPutObjectRequest(
                "bucket", "key", file);
        assertNull(from.getAccessControlList());
        assertEquals("bucket", from.getBucketName());
        assertNull(from.getCannedAcl());
        assertEquals(file, from.getFile());
        assertNull(from.getInputStream());
        assertNull(from.getRedirectLocation());
        assertNull(from.getStorageClass());

        assertNull(from.getMaterialsDescription());

        verifyBaseBeforeCopy(from);

        // Fill it up - the base
        final ProgressListener listener = new SyncProgressListener() {
            @Override
            public void progressChanged(ProgressEvent progressEvent) {
            }
        };
        final AwsCredentials credentials = new BasicAwsCredentials("accesskey",
                                                                   "accessid");
        final RequestMetricCollector collector = new RequestMetricCollector() {
            @Override
            public void collectMetrics(Request<?> request, Response<?> response) {
            }
        };

        from.setGeneralProgressListener(listener);
        from.setRequestCredentials(credentials);
        from.setRequestMetricCollector(collector);
        from.putCustomRequestHeader("k1", "v1");
        from.putCustomRequestHeader("k2", "v2");
        from.getRequestClientOptions().setReadLimit(1234);

        AccessControlList accessControlList = new AccessControlList();
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.addUserMetadata("um_k1", "um_v1");
        metadata.addUserMetadata("um_k2", "um_v2");
        SseCustomerKey sseKey = new SseCustomerKey(new byte[32]);

        // Fill it up - the rest
        from.setAccessControlList(accessControlList);
        from.setCannedAcl(CannedAccessControlList.Private);
        from.setInputStream(System.in);
        from.setMetadata(metadata);
        from.setSseCustomerKey(sseKey);
        from.setRedirectLocation("redirectLocation");
        from.setStorageClass(StorageClass.Standard);

        Map<String, String> matdesc = new HashMap<String, String>();
        matdesc.put("md_k1", "md_v1");
        matdesc.put("md_k2", "md_v2");
        from.setMaterialsDescription(matdesc);
        assertNotSame(matdesc, from.getMaterialsDescription());
        assertEquals(matdesc, from.getMaterialsDescription());

        assertSame(metadata, from.getMetadata());

        // Clone it
        EncryptedPutObjectRequest to = from.clone();
        verifyBaseAfterCopy(listener, credentials, collector, from, to);

        // Verify the rest
        assertSame(accessControlList, to.getAccessControlList());
        assertSame(CannedAccessControlList.Private, to.getCannedAcl());
        assertSame(System.in, to.getInputStream());

        assertNotSame(from.getMetadata(), to.getMetadata());
        ObjectMetadata toOMD = to.getMetadata();
        assertEquals("um_v1", toOMD.getUserMetaDataOf("um_k1"));
        assertEquals("um_v2", toOMD.getUserMetaDataOf("um_k2"));

        assertSame(sseKey, to.getSSECustomerKey());
        assertEquals("redirectLocation", to.getRedirectLocation());
        assertEquals(StorageClass.Standard.toString(), to.getStorageClass());

        assertNotSame(from.getMaterialsDescription(), to.getMaterialsDescription());
        assertEquals(from.getMaterialsDescription(), to.getMaterialsDescription());

    }

}
