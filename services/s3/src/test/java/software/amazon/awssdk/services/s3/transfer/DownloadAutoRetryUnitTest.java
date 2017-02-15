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

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.amazon.awssdk.services.s3.internal.Constants.KB;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;
import java.util.Date;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import software.amazon.awssdk.AmazonClientException;
import software.amazon.awssdk.services.s3.AmazonS3;
import software.amazon.awssdk.services.s3.model.GetObjectMetadataRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.ObjectListing;
import software.amazon.awssdk.services.s3.model.ObjectMetadata;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.model.S3ObjectSummary;
import software.amazon.awssdk.services.s3.transfer.internal.S3ProgressListener;

/**
 * Unit tests for
 * {@link TransferManager#downloadDirectory(String, String, File)} and
 * {@link TransferManager#download(GetObjectRequest, File, S3ProgressListener, long, boolean)} .
 */
public class DownloadAutoRetryUnitTest {
    private static final String BUCKET_NAME = "java-1614-test-download-bucket";
    private static final String PREFIX = "prefix";
    private static final String OBJ_KEY = "object";

    private static final ObjectListing OBJECT_LISTING = new ObjectListing();
    private static final S3ObjectSummary OBJECT_SUMMARY = new S3ObjectSummary();
    private static final ObjectMetadata OBJECT_METADATA = new ObjectMetadata();

    private static final File DOWNLOAD_DIRECTORY = new File(System.getProperty("java.io.tmpdir") + "/tm-auto-retry-testDownloadDir-" + System.currentTimeMillis());
    private static final File DOWNLOAD_FILE = new File(System.getProperty("java.io.tmpdir") + "/tm-auto-retry-testFile-" + System.currentTimeMillis() + ".bin");

    private static final int SIZE = KB;
    private static final byte[] DATA = new byte[SIZE];
    private static final int THROW_AT_POS = SIZE / 2;

    private AmazonS3 mockS3;

    private TransferManager tm;

    private S3Object s3Obj;

    @BeforeClass
    public static void classSetUp() {
        OBJECT_LISTING.setBucketName(BUCKET_NAME);
        OBJECT_LISTING.setPrefix(PREFIX);

        OBJECT_SUMMARY.setBucketName(BUCKET_NAME);
        OBJECT_SUMMARY.setKey(OBJ_KEY);

        OBJECT_LISTING.getObjectSummaries().add(OBJECT_SUMMARY);

        OBJECT_METADATA.setLastModified(new Date());
        OBJECT_METADATA.setContentLength(SIZE);
    }

    @After
    public void teardown() {
        FileUtils.deleteQuietly(DOWNLOAD_FILE);
        FileUtils.deleteQuietly(DOWNLOAD_DIRECTORY);
    }

    @Before
    public void testSetUp() {
        mockS3 = createMockS3();
        tm = new TransferManager(mockS3);
        s3Obj = new S3Object();
        s3Obj.setBucketName(BUCKET_NAME);
        s3Obj.setKey(OBJ_KEY);
    }

    /**
     * Test that {@link TransferManager#downloadDirectory(String, String, File)}
     * will automatically retry an object download if it is interrupted by a
     * "Connection reset" exception.
     */
    @Test
    public void testDownloadDirectoryDownloadRetriedOnConnectionReset() throws InterruptedException {
        s3Obj.setObjectContent(new ConnectionResetInputStream(DATA, 0));

        when(mockS3.getObject(any(GetObjectRequest.class))).thenReturn(s3Obj);

        try {
            tm.downloadDirectory(BUCKET_NAME, "", DOWNLOAD_DIRECTORY).waitForCompletion();
        } catch (AmazonClientException ignored) {
        }
        verify(mockS3, times(2)).getObject(any(GetObjectRequest.class));
    }

    /**
     * Test that {@link TransferManager#downloadDirectory(String, String, File)}
     * will not retry an object download if it is interrupted by a "Connection
     * reset by peer" exception.
     */
    @Test
    public void testDownloadDirectoryDownloadNotRetriedOnConnectionResetByPeer() throws InterruptedException {
        // "Connection reset by peer" means other end did an abortive close
        s3Obj.setObjectContent(new InputStream() {
            @Override
            public int read() throws IOException {
                throw new SocketException("Connection reset by peer");
            }
        });
        when(mockS3.getObject(any(GetObjectRequest.class))).thenReturn(s3Obj);

        try {
            tm.downloadDirectory(BUCKET_NAME, "", DOWNLOAD_DIRECTORY).waitForCompletion();
        } catch (AmazonClientException ignored) {
        }
        verify(mockS3, times(1)).getObject(any(GetObjectRequest.class));
    }

    /**
     * Test that {@link TransferManager#download(GetObjectRequest, File,
     * S3ProgressListener, long, boolean)} will automatically retry if it is
     * interrupted by "Connection reset" exception.
     */
    @Test
    public void testDownloadObjectDownloadRetriedOnConnectionReset() throws InterruptedException {
        s3Obj.setObjectContent(new ConnectionResetInputStream(DATA, 0));

        when(mockS3.getObject(any(GetObjectRequest.class))).thenReturn(s3Obj);

        GetObjectRequest req = new GetObjectRequest(BUCKET_NAME, OBJ_KEY);
        req.setRange(0);
        try {
            tm.download(req, DOWNLOAD_FILE, null, Long.MAX_VALUE).waitForCompletion();
        } catch (AmazonClientException ignored) {
        }
        verify(mockS3, times(2)).getObject(any(GetObjectRequest.class));
    }

    /**
     * Test that {@link TransferManager#download(GetObjectRequest, File,
     * S3ProgressListener, long, boolean)} will not retry if it is interrupted
     * by "Connection reset by peer" exception.
     */
    @Test
    public void testDownloadObjectDownloadNotRetriedOnConnectionResetByPeer() throws InterruptedException {
        s3Obj.setObjectContent(new InputStream() {
            @Override
            public int read() throws IOException {
                throw new SocketException("Connection reset by peer");
            }
        });

        when(mockS3.getObject(any(GetObjectRequest.class))).thenReturn(s3Obj);

        GetObjectRequest req = new GetObjectRequest(BUCKET_NAME, OBJ_KEY);
        try {
            tm.download(req, DOWNLOAD_FILE, null, Long.MAX_VALUE).waitForCompletion();
        } catch (AmazonClientException ignored) {
        }
        verify(mockS3, times(1)).getObject(any(GetObjectRequest.class));
    }


    /**
     * Test that if resumeOnRetry is true, when {@link
     * TransferManager#download(GetObjectRequest, File, S3ProgressListener,
     * long, boolean)} retries the download, the request starts where the
     * previous download left off rather restarting from the beginning.
     */
    @Test
    public void testDownloadObjectRetryAdjustsRange() throws InterruptedException {
        s3Obj.setObjectContent(new ConnectionResetInputStream(DATA, THROW_AT_POS));
        final long rangeStarts[] = {-1L, -1L};
        when(mockS3.getObject(any(GetObjectRequest.class))).thenAnswer(new Answer<S3Object>() {
            @Override
            public S3Object answer(InvocationOnMock inv) {
                int idx = rangeStarts[0] == -1 ? 0 : 1;
                rangeStarts[idx] = (inv.getArgumentAt(0, GetObjectRequest.class).getRange()[0]);
                return s3Obj;
            }
        });
        GetObjectRequest req = new GetObjectRequest(BUCKET_NAME, OBJ_KEY);
        req.setRange(0, SIZE - 1);
        try {
            tm.download(req, DOWNLOAD_FILE, null, Long.MAX_VALUE, true).waitForCompletion();
        } catch (AmazonClientException ignored) {
        }
        assertEquals(0, rangeStarts[0]);
        assertEquals(THROW_AT_POS, rangeStarts[1]);
    }

    /**
     * Test that if resumeOnRetry is false, when {@link
     * TransferManager#download(GetObjectRequest, File, S3ProgressListener,
     * long, boolean)} retries the download, the request starts from the
     * beginning.
     */
    @Test
    public void testDownloadObjectRetryNoResumeRangeNotAdjusted() throws InterruptedException {
        s3Obj.setObjectContent(new ConnectionResetInputStream(DATA, THROW_AT_POS));
        final long rangeStarts[] = {-1L, -1L};
        when(mockS3.getObject(any(GetObjectRequest.class))).thenAnswer(new Answer<S3Object>() {
            @Override
            public S3Object answer(InvocationOnMock inv) {
                int idx = rangeStarts[0] == -1 ? 0 : 1;
                rangeStarts[idx] = (inv.getArgumentAt(0, GetObjectRequest.class).getRange()[0]);
                return s3Obj;
            }
        });
        GetObjectRequest req = new GetObjectRequest(BUCKET_NAME, OBJ_KEY);
        req.setRange(0, SIZE - 1);
        try {
            tm.download(req, DOWNLOAD_FILE, null, Long.MAX_VALUE, false).waitForCompletion();
        } catch (AmazonClientException ignored) {
        }
        assertEquals(0, rangeStarts[0]);
        assertEquals(0, rangeStarts[1]);
    }

    /**
     * Test that if resumeOnRetry is true, when {@link
     * TransferManager##downloadDirectory(String, String, File, boolean)} retries
     * an object download, the request starts where the previous download left
     * off rather restarting from the beginning.
     */
    @Test
    public void testDownloadDirectoryRetryAdjustsRange() throws InterruptedException {
        s3Obj.setObjectContent(new ConnectionResetInputStream(DATA, THROW_AT_POS));

        final long rangeStarts[] = {-1L, -1L};
        when(mockS3.getObject(any(GetObjectRequest.class))).thenAnswer(new Answer<S3Object>() {
            @Override
            public S3Object answer(InvocationOnMock inv) {
                GetObjectRequest req = inv.getArgumentAt(0, GetObjectRequest.class);
                int idx = rangeStarts[0] == -1 ? 0 : 1;
                rangeStarts[idx] = (inv.getArgumentAt(0, GetObjectRequest.class).getRange()[0]);
                return s3Obj;
            }
        });

        try {
            tm.downloadDirectory(BUCKET_NAME, "", DOWNLOAD_DIRECTORY, true).waitForCompletion();
        } catch (AmazonClientException ignored) {
        }

        assertEquals(0, rangeStarts[0]);
        assertEquals(THROW_AT_POS, rangeStarts[1]);
    }

    /**
     * Test that if resumeOnRetry is false, when {@link
     * TransferManager##downloadDirectory(String, String, File, boolean)} retries
     * the object download, the request starts from the beginning.
     */
    @Test
    public void testDownloadDirectoryRetryNoResumeRangeNotAdjusted() throws InterruptedException {
        s3Obj.setObjectContent(new ConnectionResetInputStream(DATA, THROW_AT_POS));

        final long rangeStarts[] = {-1L, -1L};
        when(mockS3.getObject(any(GetObjectRequest.class))).thenAnswer(new Answer<S3Object>() {
            @Override
            public S3Object answer(InvocationOnMock inv) {
                GetObjectRequest req = inv.getArgumentAt(0, GetObjectRequest.class);
                int idx = rangeStarts[0] == -1 ? 0 : 1;
                rangeStarts[idx] = (inv.getArgumentAt(0, GetObjectRequest.class).getRange()[0]);
                return s3Obj;
            }
        });

        try {
            tm.downloadDirectory(BUCKET_NAME, "", DOWNLOAD_DIRECTORY, false).waitForCompletion();
        } catch (AmazonClientException ignored) {
        }

        assertEquals(0, rangeStarts[0]);
        assertEquals(0, rangeStarts[1]);
    }

    private AmazonS3 createMockS3() {
        AmazonS3 mock = mock(AmazonS3.class);
        when(mock.listObjects(any(ListObjectsRequest.class))).thenReturn(OBJECT_LISTING);
        when(mock.getObjectMetadata(any(GetObjectMetadataRequest.class))).thenReturn(OBJECT_METADATA);
        return mock;
    }


    private static class ConnectionResetInputStream extends InputStream {
        private final byte[] data;
        private final int throwAfter;
        private int pos = 0;

        public ConnectionResetInputStream(byte[] data, int throwAfter) {
            this.data = data;
            this.throwAfter = throwAfter;
        }

        @Override
        public int available() {
            return data.length;
        }

        @Override
        public int read() throws IOException {
            if (pos >= throwAfter) {
                throw new SocketException("Connection reset");
            }
            return data[pos++];
        }
    }
}
