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

package software.amazon.awssdk.services.s3;

import software.amazon.awssdk.ServiceSpecificConfig;
import software.amazon.awssdk.annotation.Immutable;
import software.amazon.awssdk.annotation.NotThreadSafe;
import software.amazon.awssdk.annotation.ThreadSafe;
import software.amazon.awssdk.services.s3.model.PutBucketAccelerateConfigurationRequest;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

@Immutable
@ThreadSafe
public final class S3AdvancedConfiguration implements
                                           ServiceSpecificConfig,
                                           ToCopyableBuilder<S3AdvancedConfiguration.Builder, S3AdvancedConfiguration> {

    /**
     * The default setting for use of chunked encoding
     */
    public static final boolean DEFAULT_PATH_STYLE_ACCESS_ENABLED = false;

    /**
     * The default setting for use of payload signing
     */
    public static final boolean DEFAULT_PAYLOAD_SIGNING_ENABLED = false;

    /**
     * S3 accelerate is by default not enabled
     */
    public static final boolean DEFAULT_ACCELERATE_MODE_ENABLED = false;

    /**
     * S3 dualstack endpoint is by default not enabled
     */
    public static final boolean DEFAULT_DUALSTACK_ENABLED = false;

    private final Boolean pathStyleAccessEnabled;
    private final Boolean accelerateModeEnabled;
    private final Boolean payloadSigningEnabled;
    private final Boolean dualstackEnabled;

    private S3AdvancedConfiguration(DefaultS3AdvancedConfigurationBuilder builder) {
        this.dualstackEnabled = builder.dualstackEnabled;
        this.payloadSigningEnabled = builder.payloadSigningEnabled;
        this.accelerateModeEnabled = builder.accelerateModeEnabled;
        this.pathStyleAccessEnabled = builder.pathStyleAccessEnabled;
    }

    /**
     * Create a {@link Builder}, used to create a {@link S3AdvancedConfiguration}.
     */
    public static Builder builder() {
        return new DefaultS3AdvancedConfigurationBuilder();
    }

    /**
     * <p>
     * Returns whether the client uses path-style access for all requests.
     * </p>
     * <p>
     * Amazon S3 supports virtual-hosted-style and path-style access in all
     * Regions. The path-style syntax, however, requires that you use the
     * region-specific endpoint when attempting to access a bucket.
     * </p>
     * <p>
     * The default behaviour is to detect which access style to use based on
     * the configured endpoint (an IP will result in path-style access) and
     * the bucket being accessed (some buckets are not valid DNS names).
     * Setting this flag will result in path-style access being used for all
     * requests.
     * </p>
     *
     * @return True is the client should always use path-style access
     */
    public boolean pathStyleAccessEnabled() {
        return resolvePathStyleAccessEnabled();
    }

    /**
     * <p>
     * Returns whether the client has enabled accelerate mode for getting and putting objects.
     * </p>
     * <p>
     * The default behavior is to disable accelerate mode for any operations (GET, PUT, DELETE). You need to call
     * {@link DefaultS3Client#putBucketAccelerateConfiguration(PutBucketAccelerateConfigurationRequest)}
     * first to use this feature.
     * </p>
     *
     * @return True if accelerate mode is enabled.
     */
    public boolean accelerateModeEnabled() {
        return resolveAccelerateModeEnabled();
    }

    /**
     * <p>
     * Returns whether the client is configured to sign payloads in all situations.
     * </p>
     * <p>
     * Payload signing is optional when chunked encoding is not used and requests are made
     * against an HTTPS endpoint.  Under these conditions the client will by default
     * opt to not sign payloads to optimize performance.  If this flag is set to true the
     * client will instead always sign payloads.
     * </p>
     * <p>
     * <b>Note:</b> Payload signing can be expensive, particularly if transferring
     * large payloads in a single chunk.  Enabling this option will result in a performance
     * penalty.
     * </p>
     *
     * @return True if body signing is explicitly enabled for all requests
     */
    public boolean payloadSigningEnabled() {
        return resolvePayloadSigningEnabled();
    }

    /**
     * <p>
     * Returns whether the client is configured to use dualstack mode for
     * accessing S3. If you want to use IPv6 when accessing S3, dualstack
     * must be enabled.
     * </p>
     *
     * @return True if the client will use the dualstack endpoints
     */
    public boolean dualstackEnabled() {
        return resolveDualstackEnabled();
    }

    private boolean resolveDualstackEnabled() {
        return dualstackEnabled == null ? DEFAULT_DUALSTACK_ENABLED : dualstackEnabled;
    }

    private boolean resolvePayloadSigningEnabled() {
        return payloadSigningEnabled == null ? DEFAULT_PAYLOAD_SIGNING_ENABLED : payloadSigningEnabled;
    }

    private boolean resolveAccelerateModeEnabled() {
        return accelerateModeEnabled == null ? DEFAULT_ACCELERATE_MODE_ENABLED : accelerateModeEnabled;
    }

    private boolean resolvePathStyleAccessEnabled() {
        return pathStyleAccessEnabled == null ? DEFAULT_PATH_STYLE_ACCESS_ENABLED : pathStyleAccessEnabled;
    }

    @Override
    public Builder toBuilder() {
        return builder()
                .dualstackEnabled(dualstackEnabled)
                .accelerateModeEnabled(accelerateModeEnabled)
                .pathStyleAccessEnabled(pathStyleAccessEnabled)
                .payloadSigningEnabled(payloadSigningEnabled);
    }

    @NotThreadSafe
    public interface Builder extends CopyableBuilder<Builder, S3AdvancedConfiguration> { // (8)
        /**
         * Option to enable using the dualstack endpoints when accessing S3. Dualstack
         * should be enabled if you want to use IPv6.
         *
         * @see S3AdvancedConfiguration#dualstackEnabled().
         */
        Builder dualstackEnabled(boolean dualstackEnabled);

        /**
         * Option to enable signing of payloads when accessing S3. Payload signing
         * is optional when chunked encoding is disabled and access is over HTTPS.
         * Enabling this option will result in a performance hit
         *
         * @see S3AdvancedConfiguration#payloadSigningEnabled().
         */
        Builder payloadSigningEnabled(boolean payloadSigningEnabled);

        /**
         * Option to enable using the accelerate enedpoint when accessing S3. Accelerate
         * endpoints allow faster transfer of objects by using Amazon CloudFront's
         * globally distributed edge locations.
         *
         * @see S3AdvancedConfiguration#accelerateModeEnabled().
         */
        Builder accelerateModeEnabled(boolean accelerateModeEnabled);

        /**
         * Option to enable using path style access for accessing S3 objects
         * instead of DNS style access. DNS style access is preferred as it
         * will result in better load balancing when accessing S3.
         *
         * @see S3AdvancedConfiguration#pathStyleAccessEnabled().
         */
        Builder pathStyleAccessEnabled(boolean pathStyleAccessEnabled);
    }

    private static final class DefaultS3AdvancedConfigurationBuilder implements Builder {

        private Boolean dualstackEnabled;
        private Boolean payloadSigningEnabled;
        private Boolean accelerateModeEnabled;
        private Boolean pathStyleAccessEnabled;

        public Builder dualstackEnabled(boolean dualstackEnabled) {
            this.dualstackEnabled = dualstackEnabled;
            return this;
        }

        public void setDualstackEnabled(boolean dualstackEnabled) {
            dualstackEnabled(dualstackEnabled);
        }

        public Builder payloadSigningEnabled(boolean payloadSigningEnabled) {
            this.payloadSigningEnabled = payloadSigningEnabled;
            return this;
        }

        public void setPayloadSigningEnabled(Boolean payloadSigningEnabled) {
            payloadSigningEnabled(payloadSigningEnabled);
        }

        public Builder accelerateModeEnabled(boolean accelerateModeEnabled) {
            this.accelerateModeEnabled = accelerateModeEnabled;
            return this;
        }

        public void setAccelerateModeEnabled(Boolean accelerateModeEnabled) {
            accelerateModeEnabled(accelerateModeEnabled);
        }

        public Builder pathStyleAccessEnabled(boolean pathStyleAccessEnabled) {
            this.pathStyleAccessEnabled = pathStyleAccessEnabled;
            return this;
        }

        public void setPathStyleAccessEnabled(Boolean pathStyleAccessEnabled) {
            pathStyleAccessEnabled(pathStyleAccessEnabled);
        }

        public S3AdvancedConfiguration build() {
            return new S3AdvancedConfiguration(this);
        }
    }
}
