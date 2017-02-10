/*
 * Copyright 2010-2012 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import java.io.Serializable;
import software.amazon.awssdk.services.s3.AmazonS3;

/**
 * Represents the versioning configuration for a bucket.
 * <p>
 * A bucket's versioning configuration can be in one of three possible states:
 *  <ul>
 *      <li>{@link BucketVersioningConfiguration#OFF}
 *      <li>{@link BucketVersioningConfiguration#ENABLED}
 *      <li>{@link BucketVersioningConfiguration#SUSPENDED}
 *  </ul>
 * </p>
 * <p>
 * By default, new buckets are in the
 * {@link BucketVersioningConfiguration#OFF off} state. Once versioning is
 * enabled for a bucket the status can never be reverted to
 * {@link BucketVersioningConfiguration#OFF off}.
 * </p>
 * <p>
 * In addition to enabling versioning, a bucket's versioning configuration can
 * also enable Multi-Factor Authentication (MFA) Delete, which restricts the
 * ability to permanently delete a version of an object. When MFA Delete is
 * enabled, only requests from the bucket owner which include an MFA token
 * generated by the hardware authentication device associated with the bucket
 * owner's AWS account can permanently delete an object version. For more
 * information on AWS Multi-Factor Authentication see <a
 * href="http://aws.amazon.com/mfa/">http://aws.amazon.com/mfa/</a>
 * </p>
 * <p>
 * The versioning configuration of a bucket has different implications for each
 * operation performed on that bucket or for objects within that bucket. For
 * instance, when versioning is enabled, a PutObject operation creates a unique
 * object version-id for the object being uploaded. The PutObject API guarantees
 * that, if versioning is enabled for a bucket at the time of the request, the
 * new object can only be permanently deleted using the DeleteVersion operation.
 * It can never be overwritten. Additionally, PutObject guarantees that, if
 * versioning is enabled for a bucket the request, no other object will be
 * overwritten by that request. Refer to the documentation sections for each API
 * for information on how versioning status affects the semantics of that
 * particular API.
 * <p>
 * S3 is eventually consistent. It may take time for the versioning status of a
 * bucket to be propagated throughout the system.
 * 
 * @see AmazonS3#getBucketVersioningConfiguration(String)
 * @see AmazonS3#setBucketVersioningConfiguration(SetBucketVersioningConfigurationRequest)
 */
public class BucketVersioningConfiguration implements Serializable {

    /**
     * S3 bucket versioning status indicating that versioning is off for a
     * bucket. By default, all buckets start off with versioning off. Once you
     * enable versioning for a bucket, you can never set the status back to
     * "Off". You can only suspend versioning on a bucket once you've enabled.
     */
    public static final String OFF = "Off";

    /**
     * S3 bucket versioning status indicating that versioning is suspended for a
     * bucket. Use the "Suspended" status when you want to disable versioning on
     * a bucket that has versioning enabled.
     */
    public static final String SUSPENDED = "Suspended";

    /**
     * S3 bucket versioning status indicating that versioning is enabled for a
     * bucket.
     */
    public static final String ENABLED = "Enabled";
   
    
    /** The current status of versioning */
    private String status;

    /**
     * Indicates if the optional Multi-Factor Authentication Delete control is
     * enabled for this bucket versioning configuration.
     */
    private Boolean isMfaDeleteEnabled = null;

    
    /**
     * Creates a new bucket versioning configuration object which defaults to
     * {@link #OFF} status.
     */
    public BucketVersioningConfiguration() {
        setStatus(OFF);
    }

    /**
     * Creates a new bucket versioning configuration object with the specified
     * status.
     * <p>
     * Note that once versioning has been enabled for a bucket, its status can
     * only be {@link #SUSPENDED suspended} and can never be set back to
     * {@link #OFF off}.
     * 
     * @param status
     *            The desired bucket versioning status for the new configuration
     *            object.
     * 
     * @see #ENABLED
     * @see #SUSPENDED
     */
    public BucketVersioningConfiguration(String status) {
        setStatus(status);
    }

    /**
     * Returns the current status of versioning for this bucket versioning
     * configuration object, indicating if versioning is enabled or not for a
     * bucket.
     * 
     * @return The current status of versioning for this bucket versioning
     *         configuration.
     * 
     * @see #OFF
     * @see #ENABLED
     * @see #SUSPENDED
     */
    public String getStatus() {
        return status;
    }

    /**
     * Sets the desired status of versioning for this bucket versioning
     * configuration object.
     * <p>
     * Note that once versioning has been enabled for a bucket, its status can
     * only be {@link #SUSPENDED suspended} and can never be set back to
     * {@link #OFF off}.
     * 
     * @param status
     *            The desired status of versioning for this bucket versioning
     *            configuration.
     * 
     * @see #ENABLED
     * @see #SUSPENDED
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * Sets the desired status of versioning for this bucket versioning
     * configuration object, and returns this object so that additional method
     * calls may be chained together.
     * <p>
     * Note that once versioning has been enabled for a bucket, its status can
     * only be {@link #SUSPENDED suspended} and can never be set back to
     * {@link #OFF off}.
     * 
     * @param status
     *            The desired status of versioning for this bucket versioning
     *            configuration.
     * 
     * @return The updated S3BucketVersioningConfiguration object so that
     *         additional method calls may be chained together.
     * 
     * @see #ENABLED
     * @see #SUSPENDED
     */
    public BucketVersioningConfiguration withStatus(String status) {
        setStatus(status);
        return this;
    }

    /**
     * Returns true if Multi-Factor Authentication (MFA) Delete is enabled for
     * this bucket versioning configuration, false if it isn't enabled, and null
     * if no information is available about the status of MFADelete.
     * <p>
     * When MFA Delete is enabled, object versions can only be permanently
     * deleted when the bucket owner passes in, as part of a delete version
     * request, an MFA token from the hardware token generator associated with
     * their AWS account.
     * <p>
     * By default, MFA Delete is <b>not</b> enabled.
     * <p>
     * When enabling or disabling MFA Delete controls, you <b>must</b> also
     * supply an MFA token from the hardware token generator.
     * 
     * @return True if the Multi-Factor Authentication (MFA) Delete is enabled
     *         for this bucket versioning configuration, false if it isn't
     *         enabled, and null if no information is present on the status of
     *         MFA Delete.
     */
    public Boolean isMfaDeleteEnabled() {
        return isMfaDeleteEnabled;
    }

    /**
     * Sets the status of Multi-Factor Authentication (MFA) Delete for a bucket.
     * When MFA Delete is enabled, object versions can only be permanently
     * deleted when the bucket owner passes in, as part of a delete version
     * request, an MFA token from the hardware token generator associated with
     * their AWS account.
     * <p>
     * By default, MFA Delete is <b>not</b> enabled.
     * <p>
     * When enabling or disabling MFA Delete controls, you <b>must</b> also
     * supply an MFA token from the hardware token generator as part of the
     * request.
     * 
     * @param mfaDeleteEnabled
     *            True if the Multi-Factor Authentication (MFA) Delete is being
     *            enabled enabled, false if it is being disabled.
     */
    public void setMfaDeleteEnabled(Boolean mfaDeleteEnabled) {
        isMfaDeleteEnabled = mfaDeleteEnabled;
    }

    /**
     * Sets the status of Multi-Factor Authentication (MFA) Delete for a bucket,
     * and returns this object so that additional method calls may be chained
     * together. When MFA Delete is enabled, object versions can only be
     * permanently deleted when the bucket owner passes in, as part of a delete
     * version request, an MFA token from the hardware token generator associated
     * with their AWS account.
     * <p>
     * By default, MFA Delete is <b>not</b> enabled.
     * <p>
     * When enabling or disabling MFA Delete controls, you <b>must</b> also
     * supply an MFA token from the hardware token generator as part of the
     * request.
     * 
     * @param mfaDeleteEnabled
     *            True if the Multi-Factor Authentication (MFA) Delete is being
     *            enabled enabled, false if it is being disabled.
     * 
     * @return The updated S3BucketVersioningConfiguration object so that
     *         additional method calls may be chained together.
     */
    public BucketVersioningConfiguration withMfaDeleteEnabled(Boolean mfaDeleteEnabled) {
        setMfaDeleteEnabled(mfaDeleteEnabled);
        return this;
    }
    
}
