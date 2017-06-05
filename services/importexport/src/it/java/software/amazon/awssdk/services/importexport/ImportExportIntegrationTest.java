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

package software.amazon.awssdk.services.importexport;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Test;
import software.amazon.awssdk.AmazonServiceException.ErrorType;
import software.amazon.awssdk.services.importexport.model.CancelJobRequest;
import software.amazon.awssdk.services.importexport.model.CreateJobRequest;
import software.amazon.awssdk.services.importexport.model.CreateJobResult;
import software.amazon.awssdk.services.importexport.model.GetStatusRequest;
import software.amazon.awssdk.services.importexport.model.GetStatusResult;
import software.amazon.awssdk.services.importexport.model.InvalidParameterException;
import software.amazon.awssdk.services.importexport.model.Job;
import software.amazon.awssdk.services.importexport.model.JobType;
import software.amazon.awssdk.services.importexport.model.ListJobsRequest;
import software.amazon.awssdk.services.importexport.model.ListJobsResult;
import software.amazon.awssdk.services.importexport.model.UpdateJobRequest;
import software.amazon.awssdk.services.s3.model.Bucket;
import software.amazon.awssdk.services.s3.model.ObjectListing;
import software.amazon.awssdk.services.s3.model.S3ObjectSummary;

/**
 * Integration tests for the AWS ImportExport Java client.
 */
public class ImportExportIntegrationTest extends IntegrationTestBase {

    private static final String IMPORT_EXPORT_BUCKET_PREFIX = "import-export-test-bucket";
    private static final String IMPORT_MANIFEST_PATH =
            "/software/amazon/awssdk/services/importexport/sampleImportManifest.yml";
    private static final String EXPORT_MANIFEST_PATH =
            "/software/amazon/awssdk/services/importexport/sampleExportManifest.yml";
    /** ID of the job we create, so we can cancel it later. */
    private String createdJobId;

    @AfterClass
    public static void cleanup() {
        for (Bucket b : s3.listBuckets()) {
            if (b.getName().startsWith(IMPORT_EXPORT_BUCKET_PREFIX)) {
                deleteBucketContents(b.getName());
                s3.deleteBucket(b.getName());
            }
        }
    }

    private static void deleteBucketContents(String bucket) {
        ObjectListing result = s3.listObjects(bucket);
        for (S3ObjectSummary o : result.getObjectSummaries()) {
            s3.deleteObject(bucket, o.getKey());
        }
        if (result.isTruncated()) {
            deleteBucketContents(bucket);
        }
    }

    /** Releases resources used by tests. */
    @After
    public void tearDown() throws Exception {
        if (createdJobId != null) {
            try {
                ie.cancelJob(CancelJobRequest.builder().jobId(createdJobId).build());
            } catch (Exception e) {
                // Ignored or expected.
            }
        }
    }

    /**
     * Tests that service error responses are handled appropriately in the
     * ImportExport Java client
     */
    @Test
    public void testExceptionHandling() throws Exception {
        try {
            ie.createJob(CreateJobRequest.builder()
                                 .jobType("InvalidJobType")
                                 .manifest(sampleManifestText(IMPORT_MANIFEST_PATH))
                                 .validateOnly(true).build());
            fail("Expected an InvalidParameterException");
        } catch (InvalidParameterException e) {
            assertEquals("InvalidParameterException", e.getErrorCode());
            assertEquals(ErrorType.Client, e.getErrorType());
            assertNotNull(e.getMessage());
            assertNotNull(e.getRequestId());
            assertNotNull(e.getServiceName());
            assertTrue(e.getStatusCode() >= 400 && e.getStatusCode() <= 500);
        }
    }

    /**
     * Runs through all the ImportExport client operations to test that we can
     * correctly send requests and process responses.
     */
    @Test
    public void testImportExport() throws Exception {
        // CreateJob
        CreateJobRequest createJobRequest = CreateJobRequest.builder()
                .jobType(JobType.Import.toString())
                .manifest(sampleManifestText(IMPORT_MANIFEST_PATH)).build();
        CreateJobResult createJobResult = ie.createJob(createJobRequest);
        createdJobId = createJobResult.jobId();
        assertNotNull(createdJobId);
        assertEquals(JobType.Import.toString(), createJobResult.jobType());
        assertNotNull(createJobResult.signature());
        assertNotNull(createJobResult.signatureFileContents());
        assertNotNull(createJobResult.warningMessage());


        // UpdateJob
        UpdateJobRequest updateJobRequest = UpdateJobRequest.builder()
                .jobId(createdJobId)
                .jobType(JobType.Export.toString())
                .manifest(sampleManifestText(EXPORT_MANIFEST_PATH)).build();
        ie.updateJob(updateJobRequest);


        // ListJobs
        ListJobsResult listJobsResult = ie.listJobs(ListJobsRequest.builder().maxJobs(100).build());
        assertNotNull(listJobsResult.isTruncated());
        Job job = findJob(createdJobId, listJobsResult.jobs());
        assertNotNull(job);
        assertNotNull(job.creationDate());
        assertFalse(job.isCanceled());
        assertEquals(createdJobId, job.jobId());
        assertEquals(JobType.Export.toString(), job.jobType());
        assertFalse(job.isCanceled());


        // GetStatus
        GetStatusResult statusResult = ie.getStatus(GetStatusRequest.builder().jobId(createdJobId).build());
        assertNotNull(statusResult.creationDate());
        assertNotNull(statusResult.currentManifest());
        assertEquals(createdJobId, statusResult.jobId());
        assertEquals(JobType.Export.toString(), statusResult.jobType());
        assertNotNull(statusResult.progressMessage());
        assertNotNull(statusResult.locationMessage());
        assertNotNull(statusResult.locationCode());
        assertNotNull(statusResult.signature());
        assertEquals(0, statusResult.errorCount().intValue());
        assertNotNull(statusResult.progressCode());
        assertNotNull(statusResult.signatureFileContents());
        assertNull(statusResult.carrier());
        assertNull(statusResult.trackingNumber());
        assertNull(statusResult.logBucket());
        assertNull(statusResult.logKey());


        // Cancel our test job
        ie.cancelJob(CancelJobRequest.builder().jobId(createdJobId).build());
        assertJobIsCancelled(createdJobId);
        createdJobId = null;
    }

    /*
     * Test Helper Methods
     */

    private Job findJob(String jobId, List<Job> jobs) {
        for (Job job : jobs) {
            if (job.jobId().equals(jobId)) {
                return job;
            }
        }
        fail("Expected to find a job with ID '" + jobId + "', but didn't");
        return null;
    }

    private void assertJobIsCancelled(String jobId) {
        Job job = findJob(jobId, ie.listJobs(ListJobsRequest.builder().build()).jobs());
        assertTrue(job.isCanceled());
    }

    private String sampleManifestText(String manifestPath) throws Exception {
        String manifest = IOUtils.toString(getClass().getResourceAsStream(manifestPath));

        String existingBucketName = findAppropriateBucket();
        ensureBucketHasAtLeastOneFileBecauseOfSomeWeirdImportExportRequirement(existingBucketName);

        manifest = manifest.replaceAll("@BUCKET@", existingBucketName);
        manifest = manifest.replaceAll("@ACCESS_KEY_ID@", credentials.accessKeyId());

        return manifest;
    }

    private String findAppropriateBucket() {
        for (Bucket b : s3.listBuckets()) {
            if (b.getName().startsWith(IMPORT_EXPORT_BUCKET_PREFIX)) {
                return b.getName();
            }
        }
        return s3.createBucket(IMPORT_EXPORT_BUCKET_PREFIX + "-" + System.currentTimeMillis()).getName();
    }

    private void ensureBucketHasAtLeastOneFileBecauseOfSomeWeirdImportExportRequirement(String bucketName) {
        if (s3.listObjects(bucketName).getObjectSummaries().isEmpty()) {
            s3.putObject(bucketName, "random-object", "random-content");
        }
    }

}
