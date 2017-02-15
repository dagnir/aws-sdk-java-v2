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

package software.amazon.awssdk.services.s3.transfer;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import software.amazon.awssdk.services.s3.AmazonS3;
import software.amazon.awssdk.services.s3.internal.Constants;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadResult;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.CopyPartRequest;
import software.amazon.awssdk.services.s3.model.CopyPartResult;
import software.amazon.awssdk.services.s3.model.GetObjectMetadataRequest;
import software.amazon.awssdk.services.s3.model.InitiateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.InitiateMultipartUploadResult;
import software.amazon.awssdk.services.s3.model.ObjectMetadata;

/**
 * Unit tests for {@link TransferManager#copy(CopyObjectRequest)}
 */
public class TransferManagerCopyObjectUnitTest {
    private static final String BUCKET = "test-bucket";
    private static final String KEY = "test-key";
    private AmazonS3 mockS3;

    private TransferManager tm;

    @Before
    public void setUp() {
        mockS3 = mock(AmazonS3.class);
        tm = new TransferManager(mockS3);

        TransferManagerConfiguration config = new TransferManagerConfiguration();
        config.setMultipartCopyThreshold(1 * Constants.GB);
        tm.setConfiguration(config);
    }

    @Test
    public void testPreservesRequesterPaysWhenCopyPromotedToMultipart() throws InterruptedException {
        ObjectMetadata md = new ObjectMetadata();
        md.setContentLength(5 * Constants.GB);
        when(mockS3.getObjectMetadata(any(GetObjectMetadataRequest.class))).thenReturn(md);

        InitiateMultipartUploadResult initResult = new InitiateMultipartUploadResult();
        initResult.setUploadId("UPLOAD-ID");
        when(mockS3.initiateMultipartUpload(any(InitiateMultipartUploadRequest.class))).thenReturn(initResult);

        when(mockS3.completeMultipartUpload(any(CompleteMultipartUploadRequest.class))).thenReturn(new CompleteMultipartUploadResult());

        final List<Boolean> requesterPaysFlags = new ArrayList<Boolean>();
        when(mockS3.copyPart(any(CopyPartRequest.class))).thenAnswer(new Answer<CopyPartResult>() {
            @Override
            public CopyPartResult answer(InvocationOnMock invocation) throws Throwable {
                CopyPartRequest req = invocation.getArgumentAt(0, CopyPartRequest.class);
                synchronized (requesterPaysFlags) {
                    requesterPaysFlags.add(req.isRequesterPays());
                }
                return new CopyPartResult();
            }
        });

        tm.copy(new CopyObjectRequest(BUCKET, KEY, BUCKET, KEY + 2).withRequesterPays(true)).waitForCompletion();

        boolean requesterPaysSet = true;
        for (boolean f : requesterPaysFlags) {
            requesterPaysSet = requesterPaysSet && f;
        }

        verify(mockS3, atLeastOnce()).copyPart(any(CopyPartRequest.class));
        assertTrue(requesterPaysSet);
        verify(mockS3, never()).copyObject(any(CopyObjectRequest.class));
    }
}
