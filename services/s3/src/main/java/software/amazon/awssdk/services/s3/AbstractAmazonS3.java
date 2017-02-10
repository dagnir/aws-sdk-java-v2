/*
 * Copyright 2015 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package software.amazon.awssdk.services.s3;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.Date;
import java.util.List;
import software.amazon.awssdk.AmazonServiceException;
import software.amazon.awssdk.AmazonWebServiceRequest;
import software.amazon.awssdk.HttpMethod;
import software.amazon.awssdk.SdkClientException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.AccessControlList;
import software.amazon.awssdk.services.s3.model.Bucket;
import software.amazon.awssdk.services.s3.model.BucketAccelerateConfiguration;
import software.amazon.awssdk.services.s3.model.BucketCrossOriginConfiguration;
import software.amazon.awssdk.services.s3.model.BucketLifecycleConfiguration;
import software.amazon.awssdk.services.s3.model.BucketLoggingConfiguration;
import software.amazon.awssdk.services.s3.model.BucketNotificationConfiguration;
import software.amazon.awssdk.services.s3.model.BucketPolicy;
import software.amazon.awssdk.services.s3.model.BucketReplicationConfiguration;
import software.amazon.awssdk.services.s3.model.BucketTaggingConfiguration;
import software.amazon.awssdk.services.s3.model.BucketVersioningConfiguration;
import software.amazon.awssdk.services.s3.model.BucketWebsiteConfiguration;
import software.amazon.awssdk.services.s3.model.CannedAccessControlList;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadResult;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.CopyObjectResult;
import software.amazon.awssdk.services.s3.model.CopyPartRequest;
import software.amazon.awssdk.services.s3.model.CopyPartResult;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteBucketAnalyticsConfigurationRequest;
import software.amazon.awssdk.services.s3.model.DeleteBucketAnalyticsConfigurationResult;
import software.amazon.awssdk.services.s3.model.DeleteBucketCrossOriginConfigurationRequest;
import software.amazon.awssdk.services.s3.model.DeleteBucketInventoryConfigurationRequest;
import software.amazon.awssdk.services.s3.model.DeleteBucketInventoryConfigurationResult;
import software.amazon.awssdk.services.s3.model.DeleteBucketLifecycleConfigurationRequest;
import software.amazon.awssdk.services.s3.model.DeleteBucketMetricsConfigurationRequest;
import software.amazon.awssdk.services.s3.model.DeleteBucketMetricsConfigurationResult;
import software.amazon.awssdk.services.s3.model.DeleteBucketPolicyRequest;
import software.amazon.awssdk.services.s3.model.DeleteBucketReplicationConfigurationRequest;
import software.amazon.awssdk.services.s3.model.DeleteBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteBucketTaggingConfigurationRequest;
import software.amazon.awssdk.services.s3.model.DeleteBucketWebsiteConfigurationRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectTaggingRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectTaggingResult;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsResult;
import software.amazon.awssdk.services.s3.model.DeleteVersionRequest;
import software.amazon.awssdk.services.s3.model.GeneratePresignedUrlRequest;
import software.amazon.awssdk.services.s3.model.GetBucketAccelerateConfigurationRequest;
import software.amazon.awssdk.services.s3.model.GetBucketAclRequest;
import software.amazon.awssdk.services.s3.model.GetBucketAnalyticsConfigurationRequest;
import software.amazon.awssdk.services.s3.model.GetBucketAnalyticsConfigurationResult;
import software.amazon.awssdk.services.s3.model.GetBucketCrossOriginConfigurationRequest;
import software.amazon.awssdk.services.s3.model.GetBucketInventoryConfigurationRequest;
import software.amazon.awssdk.services.s3.model.GetBucketInventoryConfigurationResult;
import software.amazon.awssdk.services.s3.model.GetBucketLifecycleConfigurationRequest;
import software.amazon.awssdk.services.s3.model.GetBucketLocationRequest;
import software.amazon.awssdk.services.s3.model.GetBucketLoggingConfigurationRequest;
import software.amazon.awssdk.services.s3.model.GetBucketMetricsConfigurationRequest;
import software.amazon.awssdk.services.s3.model.GetBucketMetricsConfigurationResult;
import software.amazon.awssdk.services.s3.model.GetBucketNotificationConfigurationRequest;
import software.amazon.awssdk.services.s3.model.GetBucketPolicyRequest;
import software.amazon.awssdk.services.s3.model.GetBucketReplicationConfigurationRequest;
import software.amazon.awssdk.services.s3.model.GetBucketTaggingConfigurationRequest;
import software.amazon.awssdk.services.s3.model.GetBucketVersioningConfigurationRequest;
import software.amazon.awssdk.services.s3.model.GetBucketWebsiteConfigurationRequest;
import software.amazon.awssdk.services.s3.model.GetObjectAclRequest;
import software.amazon.awssdk.services.s3.model.GetObjectMetadataRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectTaggingRequest;
import software.amazon.awssdk.services.s3.model.GetObjectTaggingResult;
import software.amazon.awssdk.services.s3.model.GetS3AccountOwnerRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketResult;
import software.amazon.awssdk.services.s3.model.InitiateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.InitiateMultipartUploadResult;
import software.amazon.awssdk.services.s3.model.ListBucketAnalyticsConfigurationsRequest;
import software.amazon.awssdk.services.s3.model.ListBucketAnalyticsConfigurationsResult;
import software.amazon.awssdk.services.s3.model.ListBucketInventoryConfigurationsRequest;
import software.amazon.awssdk.services.s3.model.ListBucketInventoryConfigurationsResult;
import software.amazon.awssdk.services.s3.model.ListBucketMetricsConfigurationsRequest;
import software.amazon.awssdk.services.s3.model.ListBucketMetricsConfigurationsResult;
import software.amazon.awssdk.services.s3.model.ListBucketsRequest;
import software.amazon.awssdk.services.s3.model.ListMultipartUploadsRequest;
import software.amazon.awssdk.services.s3.model.ListNextBatchOfObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListNextBatchOfVersionsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Result;
import software.amazon.awssdk.services.s3.model.ListPartsRequest;
import software.amazon.awssdk.services.s3.model.ListVersionsRequest;
import software.amazon.awssdk.services.s3.model.MultipartUploadListing;
import software.amazon.awssdk.services.s3.model.ObjectListing;
import software.amazon.awssdk.services.s3.model.ObjectMetadata;
import software.amazon.awssdk.services.s3.model.Owner;
import software.amazon.awssdk.services.s3.model.PartListing;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResult;
import software.amazon.awssdk.services.s3.model.RestoreObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.model.SetBucketAccelerateConfigurationRequest;
import software.amazon.awssdk.services.s3.model.SetBucketAclRequest;
import software.amazon.awssdk.services.s3.model.SetBucketAnalyticsConfigurationRequest;
import software.amazon.awssdk.services.s3.model.SetBucketAnalyticsConfigurationResult;
import software.amazon.awssdk.services.s3.model.SetBucketCrossOriginConfigurationRequest;
import software.amazon.awssdk.services.s3.model.SetBucketInventoryConfigurationRequest;
import software.amazon.awssdk.services.s3.model.SetBucketInventoryConfigurationResult;
import software.amazon.awssdk.services.s3.model.SetBucketLifecycleConfigurationRequest;
import software.amazon.awssdk.services.s3.model.SetBucketLoggingConfigurationRequest;
import software.amazon.awssdk.services.s3.model.SetBucketMetricsConfigurationRequest;
import software.amazon.awssdk.services.s3.model.SetBucketMetricsConfigurationResult;
import software.amazon.awssdk.services.s3.model.SetBucketNotificationConfigurationRequest;
import software.amazon.awssdk.services.s3.model.SetBucketPolicyRequest;
import software.amazon.awssdk.services.s3.model.SetBucketReplicationConfigurationRequest;
import software.amazon.awssdk.services.s3.model.SetBucketTaggingConfigurationRequest;
import software.amazon.awssdk.services.s3.model.SetBucketVersioningConfigurationRequest;
import software.amazon.awssdk.services.s3.model.SetBucketWebsiteConfigurationRequest;
import software.amazon.awssdk.services.s3.model.SetObjectAclRequest;
import software.amazon.awssdk.services.s3.model.SetObjectTaggingRequest;
import software.amazon.awssdk.services.s3.model.SetObjectTaggingResult;
import software.amazon.awssdk.services.s3.model.StorageClass;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.model.UploadPartResult;
import software.amazon.awssdk.services.s3.model.VersionListing;
import software.amazon.awssdk.services.s3.model.analytics.AnalyticsConfiguration;
import software.amazon.awssdk.services.s3.model.inventory.InventoryConfiguration;
import software.amazon.awssdk.services.s3.model.metrics.MetricsConfiguration;
import software.amazon.awssdk.services.s3.waiters.AmazonS3Waiters;

/**
 * An Abstract class that users needs to extend instead of {@link AmazonS3}
 * interface. This abstract class is provided so we don't break any customers
 * when we introduce new methods in {@link AmazonS3} interface.
 */
public abstract class AbstractAmazonS3 implements AmazonS3 {

    @Override
    public void setEndpoint(String endpoint) {
        throw new UnsupportedOperationException("Extend AbstractAmazonS3 to provide an implementation");

    }

    @Override
    public void setRegion(Region region) throws IllegalArgumentException {
        throw new UnsupportedOperationException("Extend AbstractAmazonS3 to provide an implementation");
    }

    @Override
    public void setS3ClientOptions(S3ClientOptions clientOptions) {
        throw new UnsupportedOperationException("Extend AbstractAmazonS3 to provide an implementation");

    }

    @Override
    public void changeObjectStorageClass(String bucketName, String key,
            StorageClass newStorageClass) throws SdkClientException,
            AmazonServiceException {
        throw new UnsupportedOperationException("Extend AbstractAmazonS3 to provide an implementation");
    }

    @Override
    public void setObjectRedirectLocation(String bucketName, String key,
            String newRedirectLocation) throws SdkClientException,
            AmazonServiceException {
        throw new UnsupportedOperationException("Extend AbstractAmazonS3 to provide an implementation");

    }

    @Override
    public ObjectListing listObjects(String bucketName)
            throws SdkClientException, AmazonServiceException {
        return listObjects(new ListObjectsRequest(bucketName, null, null, null, null));
    }

    @Override
    public ObjectListing listObjects(String bucketName, String prefix)
            throws SdkClientException, AmazonServiceException {
        return listObjects(new ListObjectsRequest(bucketName, prefix, null, null, null));
    }

    @Override
    public ObjectListing listObjects(ListObjectsRequest listObjectsRequest)
            throws SdkClientException, AmazonServiceException {
        throw new UnsupportedOperationException("Extend AbstractAmazonS3 to provide an implementation");

    }

    @Override
    public ListObjectsV2Result listObjectsV2(String bucketName)
            throws SdkClientException, AmazonServiceException {
        ListObjectsV2Request request = new ListObjectsV2Request();
        request.setBucketName(bucketName);
        return listObjectsV2(request);
    }

    @Override
    public ListObjectsV2Result listObjectsV2(String bucketName, String prefix)
            throws SdkClientException, AmazonServiceException {
        ListObjectsV2Request request = new ListObjectsV2Request();
        request.setBucketName(bucketName);
        request.setPrefix(prefix);
        return listObjectsV2(request);
    }

    @Override
    public ListObjectsV2Result listObjectsV2(ListObjectsV2Request listObjectsV2Request)
            throws SdkClientException, AmazonServiceException {
        throw new UnsupportedOperationException("Extend AbstractAmazonS3 to provide an implementation");

    }

    @Override
    public ObjectListing listNextBatchOfObjects(
            ObjectListing previousObjectListing) throws SdkClientException,
            AmazonServiceException {
        return listNextBatchOfObjects(new ListNextBatchOfObjectsRequest(previousObjectListing));
    }

    @Override
    public ObjectListing listNextBatchOfObjects(
            ListNextBatchOfObjectsRequest listNextBatchOfObjectsRequest)
            throws SdkClientException, AmazonServiceException {
        throw new UnsupportedOperationException("Extend AbstractAmazonS3 to provide an implementation");
    }

    @Override
    public VersionListing listVersions(String bucketName, String prefix)
            throws SdkClientException, AmazonServiceException {
        return listVersions(new ListVersionsRequest(bucketName, prefix, null, null, null, null));
    }

    @Override
    public VersionListing listVersions(String bucketName, String prefix,
            String keyMarker, String versionIdMarker, String delimiter,
            Integer maxResults) throws SdkClientException,
            AmazonServiceException {

        ListVersionsRequest request = new ListVersionsRequest()
            .withBucketName(bucketName)
            .withPrefix(prefix)
            .withDelimiter(delimiter)
            .withKeyMarker(keyMarker)
            .withVersionIdMarker(versionIdMarker)
            .withMaxResults(maxResults);
        return listVersions(request);
    }

    @Override
    public VersionListing listVersions(ListVersionsRequest listVersionsRequest)
            throws SdkClientException, AmazonServiceException {
        throw new UnsupportedOperationException("Extend AbstractAmazonS3 to provide an implementation");

    }

    @Override
    public VersionListing listNextBatchOfVersions(
            VersionListing previousVersionListing)
            throws SdkClientException, AmazonServiceException {
        return listNextBatchOfVersions(new ListNextBatchOfVersionsRequest(previousVersionListing));
    }

    @Override
    public VersionListing listNextBatchOfVersions(
            ListNextBatchOfVersionsRequest listNextBatchOfVersionsRequest)
            throws SdkClientException, AmazonServiceException {
        throw new UnsupportedOperationException("Extend AbstractAmazonS3 to provide an implementation");
    }

    @Override
    public Owner getS3AccountOwner() throws SdkClientException,
            AmazonServiceException {
        return getS3AccountOwner(new GetS3AccountOwnerRequest());
    }

    @Override
    public Owner getS3AccountOwner(GetS3AccountOwnerRequest getS3AccountOwnerRequest)
            throws SdkClientException, AmazonServiceException {
        throw new UnsupportedOperationException("Extend AbstractAmazonS3 to provide an implementation");
    }

    @Override
    public boolean doesBucketExist(String bucketName)
            throws SdkClientException, AmazonServiceException {
        throw new UnsupportedOperationException("Extend AbstractAmazonS3 to provide an implementation");
    }

    @Override
    public HeadBucketResult headBucket(HeadBucketRequest headBucketRequest)
            throws SdkClientException, AmazonServiceException {
        throw new UnsupportedOperationException("Extend AbstractAmazonS3 to provide an implementation");
    }

    @Override
    public List<Bucket> listBuckets() throws SdkClientException,
            AmazonServiceException {
        return listBuckets(new ListBucketsRequest());
    }

    @Override
    public List<Bucket> listBuckets(ListBucketsRequest listBucketsRequest)
            throws SdkClientException, AmazonServiceException {
        throw new UnsupportedOperationException("Extend AbstractAmazonS3 to provide an implementation");

    }

    @Override
    public String getBucketLocation(String bucketName)
            throws SdkClientException, AmazonServiceException {
        return getBucketLocation(new GetBucketLocationRequest(bucketName));
    }

    @Override
    public String getBucketLocation(
            GetBucketLocationRequest getBucketLocationRequest)
            throws SdkClientException, AmazonServiceException {
        throw new UnsupportedOperationException("Extend AbstractAmazonS3 to provide an implementation");

    }

    @Override
    public Bucket createBucket(CreateBucketRequest createBucketRequest)
            throws SdkClientException, AmazonServiceException {
        throw new UnsupportedOperationException("Extend AbstractAmazonS3 to provide an implementation");

    }

    @Override
    public Bucket createBucket(String bucketName) throws SdkClientException,
            AmazonServiceException {
        return createBucket(new CreateBucketRequest(bucketName));
    }

    @Override
    public Bucket createBucket(String bucketName,
            software.amazon.awssdk.services.s3.model.Region region)
            throws SdkClientException, AmazonServiceException {
        return createBucket(new CreateBucketRequest(bucketName, region));
    }

    @Override
    public Bucket createBucket(String bucketName, String region)
            throws SdkClientException, AmazonServiceException {
        return createBucket(new CreateBucketRequest(bucketName, region));
    }

    @Override
    public AccessControlList getObjectAcl(String bucketName, String key)
            throws SdkClientException, AmazonServiceException {
        return getObjectAcl(new GetObjectAclRequest(bucketName, key));
    }

    @Override
    public AccessControlList getObjectAcl(String bucketName, String key,
            String versionId) throws SdkClientException,
            AmazonServiceException {
        return getObjectAcl(new GetObjectAclRequest(bucketName, key, versionId));
    }

    @Override
    public AccessControlList getObjectAcl(GetObjectAclRequest getObjectAclRequest)
            throws SdkClientException, AmazonServiceException {
        throw new UnsupportedOperationException("Extend AbstractAmazonS3 to provide an implementation");
    }

    @Override
    public void setObjectAcl(String bucketName, String key,
            AccessControlList acl) throws SdkClientException,
            AmazonServiceException {
        setObjectAcl(bucketName, key, null, acl);
    }

    @Override
    public void setObjectAcl(String bucketName, String key,
            CannedAccessControlList acl) throws SdkClientException,
            AmazonServiceException {
        setObjectAcl(bucketName, key, null, acl);
    }

    @Override
    public void setObjectAcl(String bucketName, String key, String versionId,
            AccessControlList acl) throws SdkClientException,
            AmazonServiceException {
        setObjectAcl(new SetObjectAclRequest(bucketName, key, versionId, acl));
    }

    @Override
    public void setObjectAcl(String bucketName, String key, String versionId,
            CannedAccessControlList acl) throws SdkClientException,
            AmazonServiceException {
        setObjectAcl(new SetObjectAclRequest(bucketName, key, versionId, acl));
    }

    @Override
    public void setObjectAcl(SetObjectAclRequest setObjectAclRequest)
            throws SdkClientException, AmazonServiceException {
        throw new UnsupportedOperationException("Extend AbstractAmazonS3 to provide an implementation");

    }

    @Override
    public AccessControlList getBucketAcl(String bucketName)
            throws SdkClientException, AmazonServiceException {
        return getBucketAcl(new GetBucketAclRequest(bucketName));
    }

    @Override
    public AccessControlList getBucketAcl(
            GetBucketAclRequest getBucketAclRequest)
            throws SdkClientException, AmazonServiceException {
        throw new UnsupportedOperationException("Extend AbstractAmazonS3 to provide an implementation");

    }

    @Override
    public void setBucketAcl(String bucketName, AccessControlList acl)
            throws SdkClientException, AmazonServiceException {
        setBucketAcl(new SetBucketAclRequest(bucketName, acl));
    }

    @Override
    public void setBucketAcl(String bucketName, CannedAccessControlList cannedAcl)
            throws SdkClientException, AmazonServiceException {
        setBucketAcl(new SetBucketAclRequest(bucketName, cannedAcl));
    }

    @Override
    public void setBucketAcl(SetBucketAclRequest setBucketAclRequest)
            throws SdkClientException, AmazonServiceException {
        throw new UnsupportedOperationException("Extend AbstractAmazonS3 to provide an implementation");

    }

    @Override
    public ObjectMetadata getObjectMetadata(String bucketName, String key)
            throws SdkClientException, AmazonServiceException {
        return getObjectMetadata(new GetObjectMetadataRequest(bucketName, key));
    }

    @Override
    public ObjectMetadata getObjectMetadata(
            GetObjectMetadataRequest getObjectMetadataRequest)
            throws SdkClientException, AmazonServiceException {
        throw new UnsupportedOperationException("Extend AbstractAmazonS3 to provide an implementation");

    }

    @Override
    public S3Object getObject(String bucketName, String key)
            throws SdkClientException, AmazonServiceException {
        return getObject(new GetObjectRequest(bucketName, key));
    }

    @Override
    public S3Object getObject(GetObjectRequest getObjectRequest)
            throws SdkClientException, AmazonServiceException {
        throw new UnsupportedOperationException("Extend AbstractAmazonS3 to provide an implementation");

    }

    @Override
    public ObjectMetadata getObject(GetObjectRequest getObjectRequest,
            File destinationFile) throws SdkClientException,
            AmazonServiceException {
        throw new UnsupportedOperationException("Extend AbstractAmazonS3 to provide an implementation");

    }

    @Override
    public String getObjectAsString(String bucketName, String key)
            throws AmazonServiceException, SdkClientException {
        throw new UnsupportedOperationException("Extend AbstractAmazonS3 to provide an implementation");
    }

    @Override
    public GetObjectTaggingResult getObjectTagging(GetObjectTaggingRequest objectTaggingRequest) {
        throw new UnsupportedOperationException("Extend AbstractAmazonS3 to provide an implementation");
    }

    @Override
    public SetObjectTaggingResult setObjectTagging(SetObjectTaggingRequest setObjectTaggingRequest) {
        throw new UnsupportedOperationException("Extend AbstractAmazonS3 to provide an implementation");

    }

    @Override
    public DeleteObjectTaggingResult deleteObjectTagging(DeleteObjectTaggingRequest deleteObjectTaggingRequest) {
        throw new UnsupportedOperationException("Extend AbstractAmazonS3 to provide an implementation");

    }

    @Override
    public void deleteBucket(String bucketName) throws SdkClientException,
            AmazonServiceException {
        deleteBucket(new DeleteBucketRequest(bucketName));
    }

    @Override
    public void deleteBucket(DeleteBucketRequest deleteBucketRequest)
            throws SdkClientException, AmazonServiceException {
        throw new UnsupportedOperationException("Extend AbstractAmazonS3 to provide an implementation");

    }

    @Override
    public PutObjectResult putObject(String bucketName, String key, File file)
            throws SdkClientException, AmazonServiceException {
        return putObject(new PutObjectRequest(bucketName, key, file)
            .withMetadata(new ObjectMetadata()));
    }

    @Override
    public PutObjectResult putObject(String bucketName, String key,
            InputStream input, ObjectMetadata metadata)
            throws SdkClientException, AmazonServiceException {
        return putObject(new PutObjectRequest(bucketName, key, input, metadata));
    }

    @Override
    public PutObjectResult putObject(PutObjectRequest putObjectRequest)
            throws SdkClientException, AmazonServiceException {
        throw new UnsupportedOperationException("Extend AbstractAmazonS3 to provide an implementation");

    }

    @Override
    public PutObjectResult putObject(String bucketName, String key, String content)
            throws AmazonServiceException, SdkClientException {
        throw new UnsupportedOperationException("Extend AbstractAmazonS3 to provide an implementation");

    }

    @Override
    public CopyObjectResult copyObject(String sourceBucketName,
            String sourceKey, String destinationBucketName,
            String destinationKey) throws SdkClientException,
            AmazonServiceException {
        return copyObject(new CopyObjectRequest(sourceBucketName, sourceKey,
                destinationBucketName, destinationKey));
    }

    @Override
    public CopyObjectResult copyObject(CopyObjectRequest copyObjectRequest)
            throws SdkClientException, AmazonServiceException {
        throw new UnsupportedOperationException("Extend AbstractAmazonS3 to provide an implementation");

    }

    @Override
    public CopyPartResult copyPart(CopyPartRequest copyPartRequest)
            throws SdkClientException, AmazonServiceException {
        throw new UnsupportedOperationException("Extend AbstractAmazonS3 to provide an implementation");

    }

    @Override
    public void deleteObject(String bucketName, String key)
            throws SdkClientException, AmazonServiceException {
        deleteObject(new DeleteObjectRequest(bucketName, key));
    }

    @Override
    public void deleteObject(DeleteObjectRequest deleteObjectRequest)
            throws SdkClientException, AmazonServiceException {
        throw new UnsupportedOperationException("Extend AbstractAmazonS3 to provide an implementation");

    }

    @Override
    public DeleteObjectsResult deleteObjects(
            DeleteObjectsRequest deleteObjectsRequest)
            throws SdkClientException, AmazonServiceException {
        throw new UnsupportedOperationException("Extend AbstractAmazonS3 to provide an implementation");

    }

    @Override
    public void deleteVersion(String bucketName, String key, String versionId)
            throws SdkClientException, AmazonServiceException {
        deleteVersion(new DeleteVersionRequest(bucketName, key, versionId));
    }

    @Override
    public void deleteVersion(DeleteVersionRequest deleteVersionRequest)
            throws SdkClientException, AmazonServiceException {
        throw new UnsupportedOperationException("Extend AbstractAmazonS3 to provide an implementation");

    }

    @Override
    public BucketLoggingConfiguration getBucketLoggingConfiguration(
            String bucketName) throws SdkClientException,
            AmazonServiceException {
        return getBucketLoggingConfiguration(new GetBucketLoggingConfigurationRequest(bucketName));
    }

    @Override
    public BucketLoggingConfiguration getBucketLoggingConfiguration(
            GetBucketLoggingConfigurationRequest getBucketLoggingConfigurationRequest)
            throws SdkClientException, AmazonServiceException {
        throw new UnsupportedOperationException("Extend AbstractAmazonS3 to provide an implementation");
    }

    @Override
    public void setBucketLoggingConfiguration(
            SetBucketLoggingConfigurationRequest setBucketLoggingConfigurationRequest)
            throws SdkClientException, AmazonServiceException {
        throw new UnsupportedOperationException("Extend AbstractAmazonS3 to provide an implementation");

    }

    @Override
    public BucketVersioningConfiguration getBucketVersioningConfiguration(
            String bucketName) throws SdkClientException,
            AmazonServiceException {
        return getBucketVersioningConfiguration(new GetBucketVersioningConfigurationRequest(bucketName));
    }

    @Override
    public BucketVersioningConfiguration getBucketVersioningConfiguration(
            GetBucketVersioningConfigurationRequest getBucketVersioningConfigurationRequest) throws SdkClientException, AmazonServiceException {
        throw new UnsupportedOperationException("Extend AbstractAmazonS3 to provide an implementation");
    }

    @Override
    public void setBucketVersioningConfiguration(
            SetBucketVersioningConfigurationRequest setBucketVersioningConfigurationRequest)
            throws SdkClientException, AmazonServiceException {
        throw new UnsupportedOperationException("Extend AbstractAmazonS3 to provide an implementation");

    }

    @Override
    public BucketLifecycleConfiguration getBucketLifecycleConfiguration(
            String bucketName) {
        return getBucketLifecycleConfiguration(new GetBucketLifecycleConfigurationRequest(bucketName));
    }

    @Override
    public BucketLifecycleConfiguration getBucketLifecycleConfiguration(GetBucketLifecycleConfigurationRequest getBucketLifecycleConfigurationRequest) {
        throw new UnsupportedOperationException("Extend AbstractAmazonS3 to provide an implementation");
    }

    @Override
    public void setBucketLifecycleConfiguration(String bucketName,
            BucketLifecycleConfiguration bucketLifecycleConfiguration) {
        setBucketLifecycleConfiguration(new SetBucketLifecycleConfigurationRequest(bucketName, bucketLifecycleConfiguration));
    }

    @Override
    public void setBucketLifecycleConfiguration(
            SetBucketLifecycleConfigurationRequest setBucketLifecycleConfigurationRequest) {
        throw new UnsupportedOperationException("Extend AbstractAmazonS3 to provide an implementation");

    }

    @Override
    public void deleteBucketLifecycleConfiguration(String bucketName) {
        deleteBucketLifecycleConfiguration(new DeleteBucketLifecycleConfigurationRequest(bucketName));
    }

    @Override
    public void deleteBucketLifecycleConfiguration(
            DeleteBucketLifecycleConfigurationRequest deleteBucketLifecycleConfigurationRequest) {
        throw new UnsupportedOperationException("Extend AbstractAmazonS3 to provide an implementation");

    }

    @Override
    public BucketCrossOriginConfiguration getBucketCrossOriginConfiguration(
            String bucketName) {
        return getBucketCrossOriginConfiguration(new GetBucketCrossOriginConfigurationRequest(bucketName));
    }

    @Override
    public BucketCrossOriginConfiguration getBucketCrossOriginConfiguration(
            GetBucketCrossOriginConfigurationRequest getBucketCrossOriginConfigurationRequest) {
        throw new UnsupportedOperationException("Extend AbstractAmazonS3 to provide an implementation");

    }

    @Override
    public void setBucketCrossOriginConfiguration(String bucketName,
            BucketCrossOriginConfiguration bucketCrossOriginConfiguration) {
        setBucketCrossOriginConfiguration(new SetBucketCrossOriginConfigurationRequest(bucketName, bucketCrossOriginConfiguration));
    }

    @Override
    public void setBucketCrossOriginConfiguration(
            SetBucketCrossOriginConfigurationRequest setBucketCrossOriginConfigurationRequest) {
        throw new UnsupportedOperationException("Extend AbstractAmazonS3 to provide an implementation");

    }

    @Override
    public void deleteBucketCrossOriginConfiguration(String bucketName) {
        deleteBucketCrossOriginConfiguration(new DeleteBucketCrossOriginConfigurationRequest(bucketName));
    }

    @Override
    public void deleteBucketCrossOriginConfiguration(
            DeleteBucketCrossOriginConfigurationRequest deleteBucketCrossOriginConfigurationRequest) {
        throw new UnsupportedOperationException("Extend AbstractAmazonS3 to provide an implementation");

    }

    @Override
    public BucketTaggingConfiguration getBucketTaggingConfiguration(
            String bucketName) {
        return getBucketTaggingConfiguration(new GetBucketTaggingConfigurationRequest(bucketName));
    }

    @Override
    public BucketTaggingConfiguration getBucketTaggingConfiguration(
            GetBucketTaggingConfigurationRequest getBucketTaggingConfigurationRequest) {
        throw new UnsupportedOperationException("Extend AbstractAmazonS3 to provide an implementation");

    }

    @Override
    public void setBucketTaggingConfiguration(String bucketName,
            BucketTaggingConfiguration bucketTaggingConfiguration) {
        setBucketTaggingConfiguration(new SetBucketTaggingConfigurationRequest(bucketName, bucketTaggingConfiguration));
    }

    @Override
    public void setBucketTaggingConfiguration(
            SetBucketTaggingConfigurationRequest setBucketTaggingConfigurationRequest) {
        throw new UnsupportedOperationException("Extend AbstractAmazonS3 to provide an implementation");

    }

    @Override
    public void deleteBucketTaggingConfiguration(String bucketName) {
        deleteBucketTaggingConfiguration(new DeleteBucketTaggingConfigurationRequest(bucketName));
    }

    @Override
    public void deleteBucketTaggingConfiguration(
            DeleteBucketTaggingConfigurationRequest deleteBucketTaggingConfigurationRequest) {
        throw new UnsupportedOperationException("Extend AbstractAmazonS3 to provide an implementation");

    }

    @Override
    public BucketNotificationConfiguration getBucketNotificationConfiguration(
            String bucketName) throws SdkClientException,
            AmazonServiceException {
        return getBucketNotificationConfiguration(new GetBucketNotificationConfigurationRequest(bucketName));
    }

    @Override
    public BucketNotificationConfiguration getBucketNotificationConfiguration(
            GetBucketNotificationConfigurationRequest getBucketNotificationConfigurationRequest)
            throws SdkClientException, AmazonServiceException {
        throw new UnsupportedOperationException("Extend AbstractAmazonS3 to provide an implementation");
    }

    @Override
    public void setBucketNotificationConfiguration(String bucketName,
            BucketNotificationConfiguration bucketNotificationConfiguration)
            throws SdkClientException, AmazonServiceException {
        setBucketNotificationConfiguration(new SetBucketNotificationConfigurationRequest(bucketName, bucketNotificationConfiguration));
    }

    @Override
    public void setBucketNotificationConfiguration(
            SetBucketNotificationConfigurationRequest setBucketNotificationConfigurationRequest)
            throws SdkClientException, AmazonServiceException {
        throw new UnsupportedOperationException("Extend AbstractAmazonS3 to provide an implementation");

    }

    @Override
    public BucketWebsiteConfiguration getBucketWebsiteConfiguration(
            String bucketName) throws SdkClientException,
            AmazonServiceException {
        return getBucketWebsiteConfiguration(new GetBucketWebsiteConfigurationRequest(bucketName));
    }

    @Override
    public BucketWebsiteConfiguration getBucketWebsiteConfiguration(
            GetBucketWebsiteConfigurationRequest getBucketWebsiteConfigurationRequest)
            throws SdkClientException, AmazonServiceException {
        throw new UnsupportedOperationException("Extend AbstractAmazonS3 to provide an implementation");
    }

    @Override
    public void setBucketWebsiteConfiguration(String bucketName,
            BucketWebsiteConfiguration configuration)
            throws SdkClientException, AmazonServiceException {
        setBucketWebsiteConfiguration(new SetBucketWebsiteConfigurationRequest(bucketName, configuration));
    }

    @Override
    public void setBucketWebsiteConfiguration(
            SetBucketWebsiteConfigurationRequest setBucketWebsiteConfigurationRequest)
            throws SdkClientException, AmazonServiceException {
        throw new UnsupportedOperationException("Extend AbstractAmazonS3 to provide an implementation");

    }

    @Override
    public void deleteBucketWebsiteConfiguration(String bucketName)
            throws SdkClientException, AmazonServiceException {
        deleteBucketWebsiteConfiguration(new DeleteBucketWebsiteConfigurationRequest(bucketName));
    }

    @Override
    public void deleteBucketWebsiteConfiguration(
            DeleteBucketWebsiteConfigurationRequest deleteBucketWebsiteConfigurationRequest)
            throws SdkClientException, AmazonServiceException {
        throw new UnsupportedOperationException("Extend AbstractAmazonS3 to provide an implementation");

    }

    @Override
    public BucketPolicy getBucketPolicy(String bucketName)
            throws SdkClientException, AmazonServiceException {
        return getBucketPolicy(new GetBucketPolicyRequest(bucketName));
    }

    @Override
    public BucketPolicy getBucketPolicy(
            GetBucketPolicyRequest getBucketPolicyRequest)
            throws SdkClientException, AmazonServiceException {
        throw new UnsupportedOperationException("Extend AbstractAmazonS3 to provide an implementation");
    }

    @Override
    public void setBucketPolicy(String bucketName, String policyText)
            throws SdkClientException, AmazonServiceException {
        throw new UnsupportedOperationException("Extend AbstractAmazonS3 to provide an implementation");

    }

    @Override
    public void setBucketPolicy(SetBucketPolicyRequest setBucketPolicyRequest)
            throws SdkClientException, AmazonServiceException {
        throw new UnsupportedOperationException("Extend AbstractAmazonS3 to provide an implementation");

    }

    @Override
    public void deleteBucketPolicy(String bucketName)
            throws SdkClientException, AmazonServiceException {
        deleteBucketPolicy(new DeleteBucketPolicyRequest(bucketName));
    }

    @Override
    public void deleteBucketPolicy(
            DeleteBucketPolicyRequest deleteBucketPolicyRequest)
            throws SdkClientException, AmazonServiceException {
        throw new UnsupportedOperationException("Extend AbstractAmazonS3 to provide an implementation");

    }

    @Override
    public URL generatePresignedUrl(String bucketName, String key,
            Date expiration) throws SdkClientException {
        return generatePresignedUrl(bucketName, key, expiration, HttpMethod.GET);
    }

    @Override
    public URL generatePresignedUrl(String bucketName, String key,
            Date expiration, HttpMethod method) throws SdkClientException {
        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucketName, key, method);
        request.setExpiration(expiration);

        return generatePresignedUrl(request);
    }

    @Override
    public URL generatePresignedUrl(
            GeneratePresignedUrlRequest generatePresignedUrlRequest)
            throws SdkClientException {
        throw new UnsupportedOperationException("Extend AbstractAmazonS3 to provide an implementation");
    }

    @Override
    public InitiateMultipartUploadResult initiateMultipartUpload(
            InitiateMultipartUploadRequest request)
            throws SdkClientException, AmazonServiceException {
        throw new UnsupportedOperationException("Extend AbstractAmazonS3 to provide an implementation");
    }

    @Override
    public UploadPartResult uploadPart(UploadPartRequest request)
            throws SdkClientException, AmazonServiceException {
        throw new UnsupportedOperationException("Extend AbstractAmazonS3 to provide an implementation");
    }

    @Override
    public PartListing listParts(ListPartsRequest request)
            throws SdkClientException, AmazonServiceException {
        throw new UnsupportedOperationException("Extend AbstractAmazonS3 to provide an implementation");
    }

    @Override
    public void abortMultipartUpload(AbortMultipartUploadRequest request)
            throws SdkClientException, AmazonServiceException {
        throw new UnsupportedOperationException("Extend AbstractAmazonS3 to provide an implementation");

    }

    @Override
    public CompleteMultipartUploadResult completeMultipartUpload(
            CompleteMultipartUploadRequest request)
            throws SdkClientException, AmazonServiceException {
        throw new UnsupportedOperationException("Extend AbstractAmazonS3 to provide an implementation");
    }

    @Override
    public MultipartUploadListing listMultipartUploads(
            ListMultipartUploadsRequest request) throws SdkClientException,
            AmazonServiceException {
        throw new UnsupportedOperationException("Extend AbstractAmazonS3 to provide an implementation");
    }

    @Override
    public S3ResponseMetadata getCachedResponseMetadata(
            AmazonWebServiceRequest request) {
        throw new UnsupportedOperationException("Extend AbstractAmazonS3 to provide an implementation");
    }

    @Override
    public void restoreObject(String bucketName, String key,
            int expirationInDays) throws AmazonServiceException {
        restoreObject(new RestoreObjectRequest(bucketName, key, expirationInDays));
    }


    @Override
    public void restoreObject(RestoreObjectRequest request)
            throws AmazonServiceException {
        throw new UnsupportedOperationException("Extend AbstractAmazonS3 to provide an implementation");

    }

    @Override
    public void enableRequesterPays(String bucketName)
            throws AmazonServiceException, SdkClientException {
        throw new UnsupportedOperationException("Extend AbstractAmazonS3 to provide an implementation");

    }

    @Override
    public void disableRequesterPays(String bucketName)
            throws AmazonServiceException, SdkClientException {
        throw new UnsupportedOperationException("Extend AbstractAmazonS3 to provide an implementation");

    }

    @Override
    public boolean isRequesterPaysEnabled(String bucketName)
            throws AmazonServiceException, SdkClientException {
        throw new UnsupportedOperationException("Extend AbstractAmazonS3 to provide an implementation");
    }

    @Override
    public void setBucketReplicationConfiguration(String bucketName,
            BucketReplicationConfiguration configuration)
            throws AmazonServiceException, SdkClientException {
        setBucketReplicationConfiguration(new SetBucketReplicationConfigurationRequest(
                bucketName, configuration));
    }

    @Override
    public void setBucketReplicationConfiguration(
            SetBucketReplicationConfigurationRequest setBucketReplicationConfigurationRequest)
            throws AmazonServiceException, SdkClientException {
        throw new UnsupportedOperationException("Extend AbstractAmazonS3 to provide an implementation");

    }

    @Override
    public BucketReplicationConfiguration getBucketReplicationConfiguration(
            String bucketName) throws AmazonServiceException,
            SdkClientException {
        return getBucketReplicationConfiguration(new GetBucketReplicationConfigurationRequest(bucketName));
    }

    @Override
    public BucketReplicationConfiguration getBucketReplicationConfiguration(
            GetBucketReplicationConfigurationRequest getBucketReplicationConfigurationRequest)
            throws AmazonServiceException, SdkClientException {
        throw new UnsupportedOperationException("Extend AbstractAmazonS3 to provide an implementation");
    }

    @Override
    public void deleteBucketReplicationConfiguration(String bucketName)
            throws AmazonServiceException, SdkClientException {
        deleteBucketReplicationConfiguration(new
                DeleteBucketReplicationConfigurationRequest(bucketName));
    }

    @Override
    public void deleteBucketReplicationConfiguration(DeleteBucketReplicationConfigurationRequest request)
            throws AmazonServiceException, SdkClientException {
        throw new UnsupportedOperationException("Extend AbstractAmazonS3 to provide an implementation");
    }

    @Override
    public boolean doesObjectExist(String bucketName, String objectName)
            throws AmazonServiceException, SdkClientException {
        throw new UnsupportedOperationException("Extend AbstractAmazonS3 to provide an implementation");
    }

    @Override
    public BucketAccelerateConfiguration getBucketAccelerateConfiguration(
            String bucketName) throws AmazonServiceException,
            SdkClientException {
        return getBucketAccelerateConfiguration(new GetBucketAccelerateConfigurationRequest(
                bucketName));
    }

    @Override
    public BucketAccelerateConfiguration getBucketAccelerateConfiguration(
            GetBucketAccelerateConfigurationRequest getBucketAccelerateConfigurationRequest)
            throws AmazonServiceException, SdkClientException {
        throw new UnsupportedOperationException(
                "Extend AbstractAmazonS3 to provide an implementation");
    }

    @Override
    public void setBucketAccelerateConfiguration(String bucketName,
            BucketAccelerateConfiguration accelerateConfiguration)
            throws AmazonServiceException, SdkClientException {
        setBucketAccelerateConfiguration(new SetBucketAccelerateConfigurationRequest(
                bucketName, accelerateConfiguration));
    }

    @Override
    public void setBucketAccelerateConfiguration(
            SetBucketAccelerateConfigurationRequest setBucketAccelerateConfigurationRequest)
            throws AmazonServiceException, SdkClientException {
        throw new UnsupportedOperationException(
                "Extend AbstractAmazonS3 to provide an implementation");
    }

    @Override
    public DeleteBucketMetricsConfigurationResult deleteBucketMetricsConfiguration(
            String bucketName, String id) throws AmazonServiceException, SdkClientException {
        return deleteBucketMetricsConfiguration(new DeleteBucketMetricsConfigurationRequest(bucketName, id));
    }

    @Override
    public DeleteBucketMetricsConfigurationResult deleteBucketMetricsConfiguration(
            DeleteBucketMetricsConfigurationRequest deleteBucketMetricsConfigurationRequest)
            throws AmazonServiceException, SdkClientException {
        throw new UnsupportedOperationException("Extend AbstractAmazonS3 to provide an implementation");
    }

    @Override
    public GetBucketMetricsConfigurationResult getBucketMetricsConfiguration(
            String bucketName, String id) throws AmazonServiceException, SdkClientException {
        return getBucketMetricsConfiguration(new GetBucketMetricsConfigurationRequest(bucketName, id));
    }

    @Override
    public GetBucketMetricsConfigurationResult getBucketMetricsConfiguration(
            GetBucketMetricsConfigurationRequest getBucketMetricsConfigurationRequest)
            throws AmazonServiceException, SdkClientException {
        throw new UnsupportedOperationException("Extend AbstractAmazonS3 to provide an implementation");
    }

    public SetBucketMetricsConfigurationResult setBucketMetricsConfiguration(
            String bucketName, MetricsConfiguration metricsConfiguration)
            throws AmazonServiceException, SdkClientException {
        return setBucketMetricsConfiguration(new SetBucketMetricsConfigurationRequest(bucketName, metricsConfiguration));
    }

    @Override
    public SetBucketMetricsConfigurationResult setBucketMetricsConfiguration(SetBucketMetricsConfigurationRequest setBucketMetricsConfigurationRequest)
            throws AmazonServiceException, SdkClientException {
        throw new UnsupportedOperationException("Extend AbstractAmazonS3 to provide an implementation");
    }

    @Override
    public ListBucketMetricsConfigurationsResult listBucketMetricsConfigurations(
            ListBucketMetricsConfigurationsRequest listBucketMetricsConfigurationsRequest)
            throws AmazonServiceException, SdkClientException {
        throw new UnsupportedOperationException("Extend AbstractAmazonS3 to provide an implementation");
    }

    @Override
    public DeleteBucketAnalyticsConfigurationResult deleteBucketAnalyticsConfiguration(
            String bucketName, String id) throws AmazonServiceException, SdkClientException {
        return deleteBucketAnalyticsConfiguration(new DeleteBucketAnalyticsConfigurationRequest(bucketName, id));
    }

    @Override
    public DeleteBucketAnalyticsConfigurationResult deleteBucketAnalyticsConfiguration(
            DeleteBucketAnalyticsConfigurationRequest deleteBucketAnalyticsConfigurationRequest)
            throws AmazonServiceException, SdkClientException {
        throw new UnsupportedOperationException("Extend AbstractAmazonS3 to provide an implementation");
    }

    @Override
    public GetBucketAnalyticsConfigurationResult getBucketAnalyticsConfiguration(
            String bucketName, String id) throws AmazonServiceException, SdkClientException {
        return getBucketAnalyticsConfiguration(new GetBucketAnalyticsConfigurationRequest(bucketName, id));
    }

    @Override
    public GetBucketAnalyticsConfigurationResult getBucketAnalyticsConfiguration(
            GetBucketAnalyticsConfigurationRequest getBucketAnalyticsConfigurationRequest)
            throws AmazonServiceException, SdkClientException {
        throw new UnsupportedOperationException("Extend AbstractAmazonS3 to provide an implementation");
    }

    @Override
    public SetBucketAnalyticsConfigurationResult setBucketAnalyticsConfiguration(
            String bucketName, AnalyticsConfiguration analyticsConfiguration)
            throws AmazonServiceException, SdkClientException {
        return setBucketAnalyticsConfiguration(
                new SetBucketAnalyticsConfigurationRequest(bucketName, analyticsConfiguration));
    }

    @Override
    public SetBucketAnalyticsConfigurationResult setBucketAnalyticsConfiguration(
            SetBucketAnalyticsConfigurationRequest setBucketAnalyticsConfigurationRequest)
            throws AmazonServiceException, SdkClientException {
        throw new UnsupportedOperationException("Extend AbstractAmazonS3 to provide an implementation");
    }

    @Override
    public ListBucketAnalyticsConfigurationsResult listBucketAnalyticsConfigurations(
            ListBucketAnalyticsConfigurationsRequest listBucketAnalyticsConfigurationsRequest)
            throws AmazonServiceException, SdkClientException {
        throw new UnsupportedOperationException("Extend AbstractAmazonS3 to provide an implementation");
    }

    @Override
    public DeleteBucketInventoryConfigurationResult deleteBucketInventoryConfiguration(
            String bucketName, String id) throws AmazonServiceException, SdkClientException {
        return deleteBucketInventoryConfiguration(
                new DeleteBucketInventoryConfigurationRequest(bucketName, id));
    }

    @Override
    public DeleteBucketInventoryConfigurationResult deleteBucketInventoryConfiguration(
            DeleteBucketInventoryConfigurationRequest deleteBucketInventoryConfigurationRequest)
            throws AmazonServiceException, SdkClientException {
        throw new UnsupportedOperationException("Extend AbstractAmazonS3 to provide an implementation");
    }

    @Override
    public GetBucketInventoryConfigurationResult getBucketInventoryConfiguration(
            String bucketName, String id) throws AmazonServiceException, SdkClientException {
        return getBucketInventoryConfiguration(
                new GetBucketInventoryConfigurationRequest(bucketName, id));
    }

    @Override
    public GetBucketInventoryConfigurationResult getBucketInventoryConfiguration(
            GetBucketInventoryConfigurationRequest getBucketInventoryConfigurationRequest)
            throws AmazonServiceException, SdkClientException {
        throw new UnsupportedOperationException("Extend AbstractAmazonS3 to provide an implementation");
    }

    @Override
    public SetBucketInventoryConfigurationResult setBucketInventoryConfiguration(
            String bucketName, InventoryConfiguration inventoryConfiguration)
            throws AmazonServiceException, SdkClientException {
        return setBucketInventoryConfiguration(
                new SetBucketInventoryConfigurationRequest(bucketName, inventoryConfiguration));
    }

    @Override
    public SetBucketInventoryConfigurationResult setBucketInventoryConfiguration(
            SetBucketInventoryConfigurationRequest setBucketInventoryConfigurationRequest)
            throws AmazonServiceException, SdkClientException {
        throw new UnsupportedOperationException("Extend AbstractAmazonS3 to provide an implementation");
    }

    @Override
    public ListBucketInventoryConfigurationsResult listBucketInventoryConfigurations(
            ListBucketInventoryConfigurationsRequest listBucketInventoryConfigurationsRequest)
            throws AmazonServiceException, SdkClientException {
        throw new UnsupportedOperationException("Extend AbstractAmazonS3 to provide an implementation");
    }

    @Override
    public software.amazon.awssdk.services.s3.model.Region getRegion() {
        throw new UnsupportedOperationException("Extend AbstractAmazonS3 to provide an implementation");
    }

    @Override
    public String getRegionName() {
        throw new UnsupportedOperationException("Extend AbstractAmazonS3 to provide an implementation");
    }

    @Override
    public URL getUrl(String bucketName, String key) {
        throw new UnsupportedOperationException("Extend AbstractAmazonS3 to provide an implementation");
    }

    @Override
    public AmazonS3Waiters waiters() {
        throw new UnsupportedOperationException("Extend AbstractAmazonS3 to provide an implementation");
    }
}
