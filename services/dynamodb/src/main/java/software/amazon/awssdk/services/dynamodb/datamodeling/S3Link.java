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

package software.amazon.awssdk.services.dynamodb.datamodeling;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import software.amazon.awssdk.SdkClientException;
import software.amazon.awssdk.auth.AwsCredentialsProvider;
import software.amazon.awssdk.metrics.RequestMetricCollector;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.AmazonS3;
import software.amazon.awssdk.services.s3.AmazonS3Client;
import software.amazon.awssdk.services.s3.internal.BucketNameUtils;
import software.amazon.awssdk.services.s3.model.AccessControlList;
import software.amazon.awssdk.services.s3.model.CannedAccessControlList;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ObjectMetadata;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResult;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.model.S3ObjectInputStream;
import software.amazon.awssdk.services.s3.model.SetObjectAclRequest;
import software.amazon.awssdk.util.json.Jackson;

/**
 * An S3 Link that works with {@link DynamoDbMapper}.
 * An S3 link is persisted as a JSON string in DynamoDB.
 * This link object can be used directly to upload/download files to S3.
 * Alternatively, the underlying
 * {@link AmazonS3Client} and {@link TransferManager} can be retrieved to
 * provide full access API to S3.
 * <p>
 * For example:
 * <pre class="brush: java">
 * AWSCredentialsProvider s3CredentialProvider = ...;
 * DynamoDBMapper mapper = new DynamoDBMapper(..., s3CredentialProvider);
 * String username = "jamestkirk";
 *
 * User user = new User();
 * user.setUsername(username);
 *
 * // S3 region can be specified, but is optional
 * S3Link s3link = mapper.createS3Link("my-company-user-avatars", username + ".jpg");
 * user.setAvatar(s3link);
 *
 * // All meta information of the S3 resource is persisted in DynamoDB, including
 * // region, bucket, and key
 * mapper.save(user);
 *
 * // Upload file to S3 with the link saved in DynamoDB
 * s3link.uploadFrom(new File("/path/to/all/those/user/avatars/" + username + ".jpg"));
 * // Download file from S3 via an S3Link
 * s3link.downloadTo(new File("/path/to/downloads/" + username + ".jpg"));
 *
 * // Full S3 API is available via the canonical AmazonS3Client and TransferManager API.
 * // For example:
 * AmazonS3Client s3 = s3link.getAmazonS3Client();
 * TransferManager s3m = s3link.getTransferManager();
 * // etc.
 * </pre>The User pojo class used above:<pre class="brush: java">
 * &commat;DynamoDBTable(tableName = "user-table")
 * public class User {
 *     private String username;
 *     private S3Link avatar;
 *
 *     &commat;DynamoDBHashKey
 *     public String getUsername() {
 *         return username;
 *     }
 *
 *     public void setUsername(String username) {
 *         this.username = username;
 *     }
 *
 *     public S3Link getAvatar() {
 *         return avatar;
 *     }
 *
 *     public void setAvatar(S3Link avatar) {
 *         this.avatar = avatar;
 *     }
 * }
 * </pre>
 */
public class S3Link {
    private final S3ClientCache s3cc;
    private final Id id;

    S3Link(S3ClientCache s3cc, String bucketName, String key) {
        this(s3cc, new Id(bucketName, key));
    }

    S3Link(S3ClientCache s3cc, String region, String bucketName, String key) {
        this(s3cc, new Id(region, bucketName, key));
    }

    private S3Link(S3ClientCache s3cc, Id id) {
        this.s3cc = s3cc;
        this.id = id;

        if (s3cc == null) {
            throw new IllegalArgumentException("S3ClientCache must be configured for use with S3Link");
        }
        if (id == null || id.bucket() == null || id.getKey() == null) {
            throw new IllegalArgumentException("Bucket and key must be specified for S3Link");
        }
    }

    /**
     * Deserializes from a JSON string.
     */
    public static S3Link fromJson(S3ClientCache s3cc, String json) {
        Id id = Jackson.fromJsonString(json, Id.class);
        return new S3Link(s3cc, id);
    }

    private static String convertRegionToString(Region region, String bucketName) {
        String regionAsString;
        if (region == null) {
            if (BucketNameUtils.isDnsBucketName(bucketName)) {
                regionAsString = Region.US_EAST_1.value();
            } else {
                throw new IllegalArgumentException("Region must be specified for bucket that cannot be addressed using " +
                                                   "virtual host style");
            }
        } else {
            regionAsString = region.value();
        }
        return regionAsString;
    }

    public String getKey() {
        return id.getKey();
    }

    public String bucketName() {
        return id.bucket();
    }

    /**
     * Returns the S3 region in {@link Region} format.
     * <p>
     *     Do not use this method if {@link S3Link} is created with a region not in {@link Region} enum.
     *     Use {@link #getRegion()} instead.
     * </p>
     *
     * @return S3 region.
     */
    public Region s3Region() {
        return Region.of(getRegion());
    }

    /**
     * Returns the S3 region as string.
     *
     * @return region provided when creating the S3Link object.
     *         If no region is provided during S3Link creation, returns us-east-1.
     */
    public String getRegion() {
        return id.getRegionId() == null ? "us-east-1" : id.getRegionId();
    }

    /**
     * Serializes into a JSON string.
     *
     * @return The string representation of the link to the S3 resource.
     */
    public String toJson() {
        return id.toJson();
    }

    public AmazonS3 getAmazonS3Client() {
        return s3cc.getClient(getRegion());
    }

    /**
     * Convenience method to synchronously upload from the given file to the
     * Amazon S3 object represented by this S3Link.
     *
     * @param source
     *            source file to upload from
     *
     * @return A {@link PutObjectResult} object containing the information
     *         returned by Amazon S3 for the newly created object.
     */
    public PutObjectResult uploadFrom(final File source) {
        return uploadFrom0(source, null);
    }

    /**
     * Same as {@link #uploadFrom(File)} but allows specifying a
     * request metric collector.
     */
    public PutObjectResult uploadFrom(final File source,
                                      RequestMetricCollector requestMetricCollector) {
        return uploadFrom0(source, requestMetricCollector);
    }

    private PutObjectResult uploadFrom0(final File source,
                                        RequestMetricCollector requestMetricCollector) {
        PutObjectRequest req = new PutObjectRequest(bucketName(), getKey(),
                                                    source).withRequestMetricCollector(requestMetricCollector);
        return getAmazonS3Client().putObject(req);
    }

    /**
     * Convenience method to synchronously upload from the given buffer to the
     * Amazon S3 object represented by this S3Link.
     *
     * @param buffer
     *            The buffer containing the data to upload.
     *
     * @return A {@link PutObjectResult} object containing the information
     *         returned by Amazon S3 for the newly created object.
     */
    public PutObjectResult uploadFrom(final byte[] buffer) {
        return uploadFrom0(buffer, null);
    }

    /**
     * Same as {@link #uploadFrom(byte[])} but allows specifying a
     * request metric collector.
     */
    public PutObjectResult uploadFrom(final byte[] buffer,
                                      RequestMetricCollector requestMetricCollector) {
        return uploadFrom0(buffer, requestMetricCollector);
    }

    private PutObjectResult uploadFrom0(final byte[] buffer,
                                        RequestMetricCollector requestMetricCollector) {
        ObjectMetadata objectMetadata = new ObjectMetadata();
        objectMetadata.setContentLength(buffer.length);
        PutObjectRequest req = new PutObjectRequest(bucketName(), getKey(),
                                                    new ByteArrayInputStream(buffer), objectMetadata)
                .withRequestMetricCollector(requestMetricCollector);
        return getAmazonS3Client().putObject(req);
    }

    /**
     * Sets the access control list for the object represented by this S3Link.
     *
     * Note: Executing this method requires that the object already exists in
     * Amazon S3.
     *
     * @param acl
     *            The access control list describing the new permissions for the
     *            object represented by this S3Link.
     */
    public void setAcl(CannedAccessControlList acl) {
        setAcl0(acl, null);
    }

    public void setAcl(CannedAccessControlList acl, RequestMetricCollector col) {
        setAcl0(acl, col);
    }

    private void setAcl0(CannedAccessControlList acl, RequestMetricCollector col) {
        SetObjectAclRequest setObjectAclRequest = new SetObjectAclRequest(bucketName(), getKey(), acl)
                .withRequestMetricCollector(col);
        getAmazonS3Client().setObjectAcl(setObjectAclRequest);
    }

    /**
     * Sets the access control list for the object represented by this S3Link.
     *
     * Note: Executing this method requires that the object already exists in
     * Amazon S3.
     *
     * @param acl
     *            The access control list describing the new permissions for the
     *            object represented by this S3Link.
     */
    public void setAcl(AccessControlList acl) {
        setAcl0(acl, null);
    }

    /**
     * Same as {@link #setAcl(AccessControlList)} but allows specifying a
     * request metric collector.
     */
    public void setAcl(AccessControlList acl,
                       RequestMetricCollector requestMetricCollector) {
        setAcl0(acl, requestMetricCollector);
    }

    private void setAcl0(AccessControlList acl, RequestMetricCollector requestMetricCollector) {
        SetObjectAclRequest setObjectAclRequest = new SetObjectAclRequest(bucketName(), getKey(), acl)
                .withRequestMetricCollector(requestMetricCollector);
        getAmazonS3Client().setObjectAcl(setObjectAclRequest);
    }

    /**
     * Returns a URL for the location of the object represented by this S3Link.
     * <p>
     * If the object represented by this S3Link has public read permissions (ex:
     * {@link CannedAccessControlList#PublicRead}), then this URL can be
     * directly accessed to retrieve the object data.
     *
     * @return A URL for the location of the object represented by this S3Link.
     */
    public URL getUrl() {
        return getAmazonS3Client().getUrl(bucketName(), getKey());
    }

    /**
     * Convenient method to synchronously download to the specified file from
     * the S3 object represented by this S3Link.
     *
     * @param destination destination file to download to
     *
     * @return All S3 object metadata for the specified object.
     *     Returns null if constraints were specified but not met.
     */
    public ObjectMetadata downloadTo(final File destination) {
        return downloadTo0(destination, null);
    }

    /**
     * Same as {@link #downloadTo(File)} but allows specifying a
     * request metric collector.
     */
    public ObjectMetadata downloadTo(final File destination,
                                     RequestMetricCollector requestMetricCollector) {
        return downloadTo0(destination, requestMetricCollector);
    }

    private ObjectMetadata downloadTo0(final File destination,
                                       RequestMetricCollector requestMetricCollector) {
        GetObjectRequest req = new GetObjectRequest(bucketName(), getKey())
                .withRequestMetricCollector(requestMetricCollector);
        return getAmazonS3Client().getObject(req, destination);
    }

    /**
     * Downloads the data from the object represented by this S3Link to the
     * specified output stream.
     *
     * @param output
     *            The output stream to write the object's data to.
     *
     * @return The object's metadata.
     */
    public ObjectMetadata downloadTo(final OutputStream output) {
        return downloadTo0(output, null);
    }

    /**
     * Same as {@link #downloadTo(OutputStream)} but allows specifying a
     * request metric collector.
     */
    public ObjectMetadata downloadTo(final OutputStream output,
                                     RequestMetricCollector requestMetricCollector) {
        return downloadTo0(output, requestMetricCollector);
    }

    private ObjectMetadata downloadTo0(final OutputStream output,
                                       RequestMetricCollector requestMetricCollector) {
        GetObjectRequest req = new GetObjectRequest(bucketName(), getKey())
                .withRequestMetricCollector(requestMetricCollector);
        S3Object s3Object = getAmazonS3Client().getObject(req);
        S3ObjectInputStream objectContent = s3Object.getObjectContent();

        try {
            byte[] buffer = new byte[1024 * 10];
            int bytesRead = -1;
            while ((bytesRead = objectContent.read(buffer)) > -1) {
                output.write(buffer, 0, bytesRead);
            }
        } catch (IOException ioe) {
            objectContent.abort();
            throw new SdkClientException("Unable to transfer content from Amazon S3 to the output stream", ioe);
        } finally {
            try {
                objectContent.close();
            } catch (IOException ioe) {
                // Ignored or expected.
            }
        }

        return s3Object.getObjectMetadata();
    }

    /**
     * JSON wrapper of an {@link S3Link} identifier,
     * which consists of the S3 region id, bucket name and key.
     * Sample JSON serialized form:
     * <pre>
     * {"s3":{"bucket":"mybucket","key":"mykey","region":"us-west-2"}}
     * {"s3":{"bucket":"mybucket","key":"mykey","region":null}}
     * </pre>
     * Note for S3 a null region means US standard.
     * <p>
     *  @see Region#US_Standard
     */
    static class Id {
        @JsonProperty("s3")
        private S3 s3;

        Id() {
        } // used by Jackson to unmarshall

        Id(String bucketName, String key) {
            this.s3 = new S3(bucketName, key);
        }

        Id(String region, String bucketName, String key) {
            this.s3 = new S3(region, bucketName, key);
        }

        Id(S3 s3) {
            this.s3 = s3;
        }

        @JsonProperty("s3")
        public S3 s3() {
            return s3;
        }

        @JsonIgnore
        public String getRegionId() {
            return s3.getRegionId();
        }

        @JsonIgnore
        public String bucket() {
            return s3.bucket();
        }

        @JsonIgnore
        public String getKey() {
            return s3.getKey();
        }

        String toJson() {
            return Jackson.toJsonString(this);
        }
    }

    /**
     * Internal class for JSON serialization purposes.
     * <p>
     * @see Id
     */
    private static class S3 {

        /**
         * The name of the S3 bucket containing the object to retrieve.
         */
        @JsonProperty("bucket")
        private String bucket;

        /**
         * The key under which the desired S3 object is stored.
         */
        @JsonProperty("key")
        private String key;

        /**
         * The region id of {@link Region} where the S3 object is stored.
         */
        @JsonProperty("region")
        private String regionId;

        @SuppressWarnings("unused")
        S3() {
        }  // used by Jackson to unmarshall

        /**
         * Constructs a new {@link S3} with all the required parameters.
         *
         * @param bucket
         *            The name of the bucket containing the desired object.
         * @param key
         *            The key in the specified bucket under which the object is
         *            stored.
         */
        S3(String bucket, String key) {
            this(null, bucket, key);
        }

        /**
         * Constructs a new {@link S3} with all the required parameters.
         *
         * @param region
         *            The region where the S3 object is stored.
         * @param bucket
         *            The name of the bucket containing the desired object.
         * @param key
         *            The key in the specified bucket under which the object is
         *            stored.
         */
        S3(String region, String bucket, String key) {
            this.regionId = region;
            this.bucket = bucket;
            this.key = key;
        }

        /**
         * Gets the name of the bucket containing the object to be downloaded.
         *
         * @return The name of the bucket containing the object to be downloaded.
         */
        @JsonProperty("bucket")
        public String bucket() {
            return bucket;
        }

        /**
         * Gets the key under which the object to be downloaded is stored.
         *
         * @return The key under which the object to be downloaded is stored.
         */
        @JsonProperty("key")
        public String getKey() {
            return key;
        }

        @JsonProperty("region")
        public String getRegionId() {
            return regionId;
        }
    }

    /**
     * {@link S3Link} factory.
     */
    public static final class Factory implements DynamoDbTypeConverter<String, S3Link> {
        static final Factory DEFAULT = new Factory((S3ClientCache) null);
        private final S3ClientCache s3cc;

        public Factory(final S3ClientCache s3cc) {
            this.s3cc = s3cc;
        }

        public static final Factory of(final AwsCredentialsProvider provider) {
            return provider == null ? DEFAULT : new Factory(new S3ClientCache(provider));
        }

        public S3Link createS3Link(Region s3region, String bucketName, String key) {
            return createS3Link(convertRegionToString(s3region, bucketName), bucketName, key);
        }

        public S3Link createS3Link(String s3region, String bucketName, String key) {
            if (s3ClientCache() == null) {
                throw new IllegalStateException("Mapper must be constructed with S3 AWS Credentials to create S3Link");
            }
            return new S3Link(s3ClientCache(), s3region, bucketName, key);
        }

        public S3ClientCache s3ClientCache() {
            return this.s3cc;
        }

        @Override
        public String convert(final S3Link o) {
            return o.bucketName() == null || o.getKey() == null ? null : o.toJson();
        }

        @Override
        public S3Link unconvert(final String o) {
            return S3Link.fromJson(s3ClientCache(), o);
        }
    }

}
