/*
 * Copyright 2011-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *    http://aws.amazon.com/apache2.0
 *
 * This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and
 * limitations under the License.
 */
package software.amazon.awssdk.services.s3.internal;

import static software.amazon.awssdk.services.s3.internal.Constants.KB;
import static software.amazon.awssdk.services.s3.internal.Constants.MB;
import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import software.amazon.awssdk.services.s3.S3IntegrationTestBase;
import software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.SSECustomerKey;
import software.amazon.awssdk.services.s3.transfer.TransferManager;
import software.amazon.awssdk.services.s3.transfer.TransferManagerConfiguration;
import software.amazon.awssdk.services.s3.transfer.Upload;
import software.amazon.awssdk.test.util.RandomTempFile;
import software.amazon.awssdk.test.util.SdkAsserts;
import software.amazon.awssdk.util.IOUtils;

public class ServiceUtilsIntegrationTest extends S3IntegrationTestBase {

    /** Logger to log events */
    private static final Log LOG = LogFactory.getLog(ServiceUtilsIntegrationTest.class);

    /** Reference to the Transfer manager instance used for testing */
    private static TransferManager tm;

    /** The bucket used for these tests */
    private final static String BUCKET_NAME = "java-serviceutils-integ-test-" + new Date().getTime();

    /** The key used for testing multipart object */
    private final static String MULTIPART_OBJECT_KEY = "multiPartkey";

    /** The key used for testing non multipart object */
    private final static String NON_MULTIPART_OBJECT_KEY = "nonMultiPartkey";

    /** The key used for testing multipart object with Server-side encryption */
    private final static String MULTIPART_OBJECT_KEY_WITH_SSE = "multiPartkey-sse";

    /** The key used for testing non multipart object with Server-side encryption */
    private final static String NON_MULTIPART_OBJECT_KEY_WITH_SSE = "nonMultiPartkey-sse";

    /** The customer provided server-side encryption key */
    private static final SSECustomerKey SSE_KEY = new SSECustomerKey(CryptoTestUtils.getTestSecretKey());

    /** The size of the multipart object uploaded to S3 */
    private final static long MULTIPART_OBJECT_SIZE = 7 * MB;

    /** The size of the non multipart object uploaded to S3 */
    private final static long NON_MULTIPART_OBJECT_SIZE = 2 * MB;

    /** Default upload threshold for multipart uploads */
    protected static final long DEFAULT_MULTIPART_UPLOAD_THRESHOLD = 5 * MB;

    /** Default part size for multipart uploads */
    protected static final long DEFAULT_MULTIPART_UPLOAD_PART_SIZE = 5 * MB;

    /** Number of parts in the multi part object */
    private static final int PARTCOUNT = (int) Math.ceil((double) MULTIPART_OBJECT_SIZE / DEFAULT_MULTIPART_UPLOAD_PART_SIZE);

    /** File that contains the multipart data uploaded to S3 */
    private static File multiPartFile;

    /** File that contains the non multipart data uploaded to S3 */
    private static File nonMultiPartFile;

    /** Name of the source file used for testing append function */
    private static File srcFile;

    /** Name of the source file used for testing append function */
    private static File dstFile;

    /** Name of the source file used for testing append function */
    private final static String SRC_FILE_NAME = "src";

    /** Name of the destination file used for testing append function */
    private final static String DST_FILE_NAME = "dst";

    /** Size of the source file used for testing append function */
    private final static long SRC_FILE_SIZE = 1 * KB;

    /** Size of the destination file used for testing append function */
    private final static long DST_FILE_SIZE = 2 * KB;

    /** Inputstream resource to read data from files */
    private static InputStream inputStream;

    @BeforeClass
    public static void setup() throws Exception {
        S3IntegrationTestBase.setUp();
        tm = new TransferManager(s3);

        TransferManagerConfiguration tmConfig = new TransferManagerConfiguration();
        tmConfig.setMultipartUploadThreshold(DEFAULT_MULTIPART_UPLOAD_THRESHOLD);
        tmConfig.setMinimumUploadPartSize(DEFAULT_MULTIPART_UPLOAD_PART_SIZE);
        tm.setConfiguration(tmConfig);

        s3.createBucket(BUCKET_NAME);

        uploadNonMultiPartFiles();
        uploadMultiPartFiles();
    }

    /**
     * Upload non multipart objects to s3.
     */
    private static void uploadNonMultiPartFiles() throws Exception {
        nonMultiPartFile = new RandomTempFile("nonmultipart", NON_MULTIPART_OBJECT_SIZE);
        assertEquals(NON_MULTIPART_OBJECT_SIZE, nonMultiPartFile.length());
        Upload myUpload = tm.upload(new PutObjectRequest(BUCKET_NAME, NON_MULTIPART_OBJECT_KEY, nonMultiPartFile));
        myUpload.waitForCompletion();

        // Upload object with Server-side encryption
        myUpload = tm.upload(new PutObjectRequest(BUCKET_NAME, NON_MULTIPART_OBJECT_KEY_WITH_SSE, nonMultiPartFile).withSSECustomerKey(SSE_KEY));
        myUpload.waitForCompletion();
    }

    /**
     * Upload multipart objects to s3.
     */
    private static void uploadMultiPartFiles() throws Exception {
        multiPartFile = new RandomTempFile("multipart", MULTIPART_OBJECT_SIZE);
        assertEquals(MULTIPART_OBJECT_SIZE, multiPartFile.length());
        Upload myUpload = tm.upload(new PutObjectRequest(BUCKET_NAME, MULTIPART_OBJECT_KEY, multiPartFile));
        myUpload.waitForCompletion();

        // Upload object with Server-side encryption
        myUpload = tm.upload(new PutObjectRequest(BUCKET_NAME, MULTIPART_OBJECT_KEY_WITH_SSE, multiPartFile).withSSECustomerKey(SSE_KEY));
        myUpload.waitForCompletion();
    }

    /**
     * Tests getPartCount on non multi part object, returns null.
     */
    @Test
    public void getPartCountOnNonMultiPartObjectReturnsNull() {
        GetObjectRequest req = new GetObjectRequest(BUCKET_NAME, NON_MULTIPART_OBJECT_KEY);
        assertNull(ServiceUtils.getPartCount(req, s3));
    }

    /**
     * Tests getPartCount on multi part object, returns actual part count.
     */
    @Test
    public void getPartCountOnMultiPartObjectReturnsPartCount() {
        GetObjectRequest req = new GetObjectRequest(BUCKET_NAME, MULTIPART_OBJECT_KEY);
        assertEquals(PARTCOUNT, ServiceUtils.getPartCount(req, s3).intValue());
    }

    /**
     * Tests getPartCount on non-multi part object when request has SSECustomerKey, returns null.
     */
    @Test
    public void getPartCountWithSseKeyOnNonMultiPartObjectReturnsNull() {
        GetObjectRequest req = new GetObjectRequest(BUCKET_NAME, NON_MULTIPART_OBJECT_KEY_WITH_SSE).withSSECustomerKey(SSE_KEY);
        assertNull(ServiceUtils.getPartCount(req, s3));
    }

    /**
     * Tests getPartCount on multi part object when request has SSECustomerKey, returns actual part count.
     */
    @Test
    public void getPartCountWithSseKeyOnMultiPartObjectReturnsPartCount() {
        GetObjectRequest req = new GetObjectRequest(BUCKET_NAME, MULTIPART_OBJECT_KEY_WITH_SSE).withSSECustomerKey(SSE_KEY);
        assertEquals(PARTCOUNT, ServiceUtils.getPartCount(req, s3).intValue());
    }

    /**
     * Tests getLastByteInPart on first part of multi part object, returns a
     * value equal to default part size less 1.
     */
    @Test
    public void getLastByteInPartOnMultiPartObjectGivenPartNumberAsOneReturnsDefaultPartSizeMinusOne() {
        GetObjectRequest req = new GetObjectRequest(BUCKET_NAME, MULTIPART_OBJECT_KEY);
        assertEquals(DEFAULT_MULTIPART_UPLOAD_PART_SIZE - 1, ServiceUtils.getLastByteInPart(s3, req, 1));
    }

    /**
     * Tests appendFileToAnother with non binary data, when given empty src file
     * and empty dst file, asserts the dst file remains empty.
     */
    @Test
    public void appendFileToDestinationFileGivenEmptySrcFileAndEmptyDstFileReturnsEmptyDstFile() throws Exception {
        srcFile = new RandomTempFile(SRC_FILE_NAME, 0);
        dstFile = new RandomTempFile(DST_FILE_NAME, 0);

        ServiceUtils.appendFile(srcFile, dstFile);
        assertEquals(0, dstFile.length());
    }

    /**
     * Tests appendFileToAnother with non binary data, when given empty src file
     * and non-empty dst file, the dst file remains unchanged.f
     */
    @Test
    public void appendFileToDestinationFileGivenEmptySrcFileReturnsSameDstFile() throws Exception {
        srcFile = new RandomTempFile(SRC_FILE_NAME, 0);
        dstFile = new RandomTempFile(DST_FILE_NAME, DST_FILE_SIZE);

        inputStream = new FileInputStream(dstFile);
        ServiceUtils.appendFile(srcFile, dstFile);

        SdkAsserts.assertFileEqualsStream(dstFile, inputStream);
    }

    /**
     * Tests appendFileToAnother with non binary data, when given non-empty src
     * file and empty dst file, the dst file data after append should be equal
     * to src file data.
     */
    @Test
    public void appendFileToDestinationFileGivenEmptyDstFileAssertsDstFileDataAfterMergeEqualsSrcFileData()
            throws Exception {
        srcFile = new RandomTempFile(SRC_FILE_NAME, SRC_FILE_SIZE);
        dstFile = new RandomTempFile(DST_FILE_NAME, 0);

        inputStream = new FileInputStream(srcFile);
        ServiceUtils.appendFile(srcFile, dstFile);

        SdkAsserts.assertFileEqualsStream(dstFile, inputStream);
    }

    /**
     * Tests appendFileToAnother with non binary data, when given non-empty src
     * file and non-empty dst file, the dst file data after append should
     * consists combined data.
     */
    @Test
    public void appendFileToDestinationFileGivenNonEmptyFilesAppendsDataProperly() throws Exception {
        srcFile = new RandomTempFile(SRC_FILE_NAME, SRC_FILE_SIZE);
        dstFile = new RandomTempFile(DST_FILE_NAME, DST_FILE_SIZE);
        byte[] expectedData = getAppendedByteArray(srcFile, dstFile);

        ServiceUtils.appendFile(srcFile, dstFile);
        byte[] actualData = IOUtils.toByteArray(new FileInputStream(dstFile));

        assertArrayEquals(expectedData, actualData);
    }

    /**
     * Tests appendFileToAnother with binary data, when given empty src file and
     * empty dst file, asserts the dst file remains empty.
     */
    @Test
    public void appendFileToDestinationFileWithBinaryDataGivenEmptySrcFileAndEmptyDstFileReturnsEmptyDstFile()
            throws Exception {
        srcFile = new RandomTempFile(SRC_FILE_NAME, 0, true);
        dstFile = new RandomTempFile(DST_FILE_NAME, 0, true);

        ServiceUtils.appendFile(srcFile, dstFile);
        assertEquals(0, dstFile.length());
    }

    /**
     * Tests appendFileToAnother with binary data, when given empty src file and
     * non-empty dst file, the dst file remains unchanged.
     */
    @Test
    public void appendFileToDestinationFileWithBinaryDataGivenEmptySrcFileReturnsSameDstFile() throws Exception {
        srcFile = new RandomTempFile(SRC_FILE_NAME, 0, true);
        dstFile = new RandomTempFile(DST_FILE_NAME, DST_FILE_SIZE, true);

        inputStream = new FileInputStream(dstFile);
        ServiceUtils.appendFile(srcFile, dstFile);

        SdkAsserts.assertFileEqualsStream(dstFile, inputStream);
    }

    /**
     * Tests appendFileToAnother with binary data, when given non-empty src file
     * and empty dst file, the dst file data after append should be equal to src
     * file data.
     */
    @Test
    public void appendFileToDestinationFileWithBinaryDataGivenEmptyDstFileAssertsDstFileDataAfterMergeEqualsSrcFileData()
            throws Exception {
        srcFile = new RandomTempFile(SRC_FILE_NAME, SRC_FILE_SIZE, true);
        dstFile = new RandomTempFile(DST_FILE_NAME, 0, true);

        inputStream = new FileInputStream(srcFile);
        ServiceUtils.appendFile(srcFile, dstFile);

        SdkAsserts.assertFileEqualsStream(dstFile, inputStream);
    }

    /**
     * Tests appendFileToAnother with binary data, when given non-empty src file
     * and non-empty dst file, the dst file data after append should consists
     * combined data.
     */
    @Test
    public void appendFileToDestinationFileWithBinaryDataGivenNonEmptyFilesAppendsDataProperly() throws Exception {
        srcFile = new RandomTempFile(SRC_FILE_NAME, SRC_FILE_SIZE);
        dstFile = new RandomTempFile(DST_FILE_NAME, DST_FILE_SIZE);
        byte[] expectedData = getAppendedByteArray(srcFile, dstFile);

        ServiceUtils.appendFile(srcFile, dstFile);
        byte[] actualData = IOUtils.toByteArray(new FileInputStream(dstFile));

        assertArrayEquals(expectedData, actualData);
    }

    /**
     * Given a source file and destination file, converts the files into byte
     * arrays and returns a byte array that contains destination data followed
     * by source data.
     */
    private byte[] getAppendedByteArray(File src, File dst) throws Exception {
        byte[] srcdata = IOUtils.toByteArray(new FileInputStream(src));
        byte[] dstdata = IOUtils.toByteArray(new FileInputStream(dst));
        byte[] combined = new byte[srcdata.length + dstdata.length];
        System.arraycopy(dstdata, 0, combined, 0, dstdata.length);
        System.arraycopy(srcdata, 0, combined, dstdata.length, srcdata.length);
        return combined;
    }

    /**
     * Release the resources after each test case.
     */
    @After
    public void tearDownAfterEachTest() throws Exception {
        if (srcFile != null) {
            srcFile.delete();
        }
        if (dstFile != null) {
            dstFile.delete();
        }
        IOUtils.closeQuietly(inputStream, LOG);
    }

    /**
     * Relases the resources at the end of all tests.
     */
    @AfterClass
    public static void tearDown() throws Exception {
        CryptoTestUtils.deleteBucketAndAllContents(s3, BUCKET_NAME);
        if (multiPartFile != null) {
            multiPartFile.deleteOnExit();
        }
        if (nonMultiPartFile != null) {
            nonMultiPartFile.deleteOnExit();
        }
    }
}
