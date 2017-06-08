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

package software.amazon.awssdk.http;


import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.http.SdkHttpConfigurationOption.CONNECTION_TIMEOUT;
import static software.amazon.awssdk.http.SdkHttpConfigurationOption.MAX_CONNECTIONS;
import static software.amazon.awssdk.http.SdkHttpConfigurationOption.SOCKET_TIMEOUT;
import static software.amazon.awssdk.http.SdkHttpConfigurationOption.USE_STRICT_HOSTNAME_VERIFICATION;
import static software.amazon.awssdk.http.SdkHttpConfigurationOptions.GLOBAL_DEFAULTS;

import java.time.Duration;
import org.junit.Test;

public class SdkHttpConfigurationOptionsTest {

    @Test
    public void copyCreatesNewOptionsObject() {
        SdkHttpConfigurationOptions orig =
                SdkHttpConfigurationOptions.builder()
                                           .option(SOCKET_TIMEOUT, Duration.ofSeconds(60))
                                           .build();
        assertThat(orig).isNotEqualTo(orig.copy());
        assertThat(orig.option(SOCKET_TIMEOUT)).isEqualTo(orig.copy().option(SOCKET_TIMEOUT));
    }

    @Test
    public void mergeTreatsThisObjectWithHigherPrecedence() {
        SdkHttpConfigurationOptions orig =
                SdkHttpConfigurationOptions.builder()
                                           .option(SOCKET_TIMEOUT, Duration.ofSeconds(60))
                                           .build();
        SdkHttpConfigurationOptions merged = orig.merge(
                SdkHttpConfigurationOptions.builder()
                                           .option(SOCKET_TIMEOUT, Duration.ofSeconds(10))
                                           .option(CONNECTION_TIMEOUT, Duration.ofSeconds(5))
                                           .build());
        assertThat(merged.option(SOCKET_TIMEOUT)).isEqualTo(Duration.ofSeconds(60));
        // Connection timeout not specified in 'orig' so the options being merged in should be used.
        assertThat(merged.option(CONNECTION_TIMEOUT)).isEqualTo(Duration.ofSeconds(5));
    }

    /**
     * Options are optional.
     */
    @Test
    public void mergeWithOptionNotPresentInBoth_DoesNotThrow() {
        SdkHttpConfigurationOptions orig =
                SdkHttpConfigurationOptions.builder()
                                           .option(SOCKET_TIMEOUT, Duration.ofSeconds(60))
                                           .build();
        SdkHttpConfigurationOptions merged = orig.merge(
                SdkHttpConfigurationOptions.builder()
                                           .option(SOCKET_TIMEOUT, Duration.ofSeconds(10))
                                           .build());

        assertThat(merged.option(CONNECTION_TIMEOUT)).isNull();
    }

    @Test
    public void mergeWithGlobalDefaults_TreatsThisObjectWithHigherPrecedence() {
        SdkHttpConfigurationOptions orig =
                SdkHttpConfigurationOptions.builder()
                                           .option(SOCKET_TIMEOUT, Duration.ofSeconds(60))
                                           .build();
        SdkHttpConfigurationOptions merged = orig.mergeGlobalDefaults();

        // 'orig' takes precedence over global defaults
        assertThat(merged.option(SOCKET_TIMEOUT)).isEqualTo(Duration.ofSeconds(60));

        // Not specified in 'orig' so should be global defaults
        assertThat(merged.option(CONNECTION_TIMEOUT)).isEqualTo(GLOBAL_DEFAULTS.option(CONNECTION_TIMEOUT));
        assertThat(merged.option(MAX_CONNECTIONS)).isEqualTo(GLOBAL_DEFAULTS.option(MAX_CONNECTIONS));
        assertThat(merged.option(USE_STRICT_HOSTNAME_VERIFICATION))
                .isEqualTo(GLOBAL_DEFAULTS.option(USE_STRICT_HOSTNAME_VERIFICATION));

    }

}