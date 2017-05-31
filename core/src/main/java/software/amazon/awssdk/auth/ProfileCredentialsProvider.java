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

package software.amazon.awssdk.auth;

import software.amazon.awssdk.AwsSystemSetting;
import software.amazon.awssdk.auth.profile.ProfilesConfigFile;

/**
 * Credentials provider based on AWS configuration profiles. This provider vends AWSCredentials from the profile configuration
 * file for the default profile, or for a specific, named profile.
 *
 * <p>AWS credential profiles allow you to share multiple sets of AWS security credentials between different tools like the AWS
 * SDK for Java and the AWS CLI.</p>
 *
 * <p>See http://docs.aws.amazon.com/cli/latest/userguide/cli-chap-getting-started.html</p>
 *
 * @see ProfilesConfigFile
 */
public class ProfileCredentialsProvider extends FileSystemCredentialsProvider {
    private final ProfilesConfigFile profilesConfigFile;
    private final String profileName;

    /**
     * Create a {@link ProfileCredentialsProvider} using the default profile and configuration file. Use {@link #builder()} for
     * defining a custom {@link ProfileCredentialsProvider}.
     */
    public ProfileCredentialsProvider() {
        this(new Builder());
    }

    /**
     * @see #builder()
     */
    private ProfileCredentialsProvider(Builder builder) {
        this.profilesConfigFile = builder.profilesConfigFile != null ? builder.profilesConfigFile : new ProfilesConfigFile();
        this.profileName = builder.profileName != null ? builder.profileName
                                                       : AwsSystemSetting.AWS_DEFAULT_PROFILE.getStringValueOrThrow();
    }

    /**
     * Get a builder for creating a custom {@link ProfileCredentialsProvider}.
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    protected AwsCredentials loadCredentials() {
        return profilesConfigFile.getCredentials(profileName);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + profilesConfigFile + ", " + profileName + ")";
    }

    /**
     * A builder for creating a custom {@link ProfileCredentialsProvider}.
     */
    public static final class Builder {
        private ProfilesConfigFile profilesConfigFile;
        private String profileName;

        private Builder() {}

        /**
         * Define the profile configuration file that should be used by this credentials provider.
         *
         * By default, the result of {@link ProfilesConfigFile#ProfilesConfigFile()} is used.
         */
        public Builder profilesConfigFile(ProfilesConfigFile profilesConfigFile) {
            this.profilesConfigFile = profilesConfigFile;
            return this;
        }

        /**
         * Define the name of the profile that should be used by this credentials provider.
         *
         * By default, the result of {@link AwsProfileNameLoader#loadProfileName()} is used.
         */
        public Builder profileName(String profileName) {
            this.profileName = profileName;
            return this;
        }

        /**
         * Create a {@link ProfileCredentialsProvider} using the configuration applied to this builder.
         */
        public ProfileCredentialsProvider build() {
            return new ProfileCredentialsProvider(this);
        }
    }
}
