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

package software.amazon.awssdk.client.builder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.Arrays;
import java.util.Optional;
import org.junit.Test;
import software.amazon.awssdk.auth.AnonymousAwsCredentials;
import software.amazon.awssdk.auth.Aws4Signer;
import software.amazon.awssdk.auth.AwsStaticCredentialsProvider;
import software.amazon.awssdk.auth.StaticSignerProvider;
import software.amazon.awssdk.config.ClientSecurityConfiguration;
import software.amazon.awssdk.config.ImmutableAsyncClientConfiguration;
import software.amazon.awssdk.config.ImmutableSyncClientConfiguration;
import software.amazon.awssdk.config.defaults.ClientConfigurationDefaults;
import software.amazon.awssdk.http.SdkHttpClient;

/**
 * Validate the functionality of the {@link DefaultClientBuilder}.
 */
public class DefaultClientBuilderTest {
    private static final String ENDPOINT_PREFIX = "prefix";
    private static final StaticSignerProvider SIGNER_PROVIDER = new StaticSignerProvider(new Aws4Signer());
    private static final URI ENDPOINT = URI.create("https://example.com");

    @Test
    public void buildIncludesServiceDefaults() {
        TestClient client = testClientBuilder().region("us-west-1").build();
        assertThat(client.syncClientConfiguration.securityConfiguration().signerProvider()).hasValue(SIGNER_PROVIDER);
        assertThat(client.asyncClientConfiguration.securityConfiguration().signerProvider()).hasValue(SIGNER_PROVIDER);
        assertThat(client.signingRegion).isNotNull();
    }

    @Test
    public void buildWithRegionShouldHaveCorrectEndpointAndSigningRegion() {
        TestClient client = testClientBuilder().region("us-west-1").build();

        assertThat(client.syncClientConfiguration.endpoint())
                .hasToString("https://" + ENDPOINT_PREFIX + ".us-west-1.amazonaws.com");
        assertThat(client.signingRegion).isEqualTo("us-west-1");
    }

    @Test
    public void buildWithEndpointShouldHaveCorrectEndpointAndSigningRegion() {
        TestClient client = testClientBuilder().region("us-west-1").endpointOverride(ENDPOINT).build();

        assertThat(client.syncClientConfiguration.endpoint()).isEqualTo(ENDPOINT);
        assertThat(client.signingRegion).isEqualTo("us-west-1");
    }

    @Test
    public void buildWithoutRegionOrEndpointOrDefaultProviderThrowsException() {
        assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> {
            testClientBuilder().build();
        });
    }

    @Test
    public void noClientProvided_DefaultHttpClientIsManagedBySdk() {
        TestClient client = testClientBuilder().region("us-west-1").build();
        assertThat(client.syncClientConfiguration.httpClient())
                .isNotInstanceOf(DefaultClientBuilder.NonManagedSdkHttpClient.class);
    }

    @Test
    public void clientFactoryProvided_ClientIsManagedBySdk() {
        TestClient client = testClientBuilder()
                .region("us-west-1")
                .httpConfiguration(ClientHttpConfiguration.builder()
                                                          .httpClientFactory(serviceDefaults -> mock(SdkHttpClient.class))
                                                          .build())
                .build();
        assertThat(client.syncClientConfiguration.httpClient())
                .isNotInstanceOf(DefaultClientBuilder.NonManagedSdkHttpClient.class);
    }

    @Test
    public void explicitClientProvided_ClientIsNotManagedBySdk() {
        TestClient client = testClientBuilder()
                .region("us-west-1")
                .httpConfiguration(ClientHttpConfiguration.builder()
                                                          .httpClient(mock(SdkHttpClient.class))
                                                          .build())
                .build();
        assertThat(client.syncClientConfiguration.httpClient())
                .isInstanceOf(DefaultClientBuilder.NonManagedSdkHttpClient.class);
    }

    @Test
    public void clientBuilderFieldsHaveBeanEquivalents() throws Exception {
        ClientBuilder<TestClientBuilder, TestClient> builder = testClientBuilder();

        BeanInfo beanInfo = Introspector.getBeanInfo(builder.getClass());
        Method[] clientBuilderMethods = ClientBuilder.class.getDeclaredMethods();

        Arrays.stream(clientBuilderMethods).forEach(builderMethod -> {
            String propertyName = builderMethod.getName();

            Optional<PropertyDescriptor> propertyForMethod =
                    Arrays.stream(beanInfo.getPropertyDescriptors())
                          .filter(property -> property.getName().equals(propertyName))
                          .findFirst();

            assertThat(propertyForMethod).as(propertyName + " property").hasValueSatisfying(property -> {
                assertThat(property.getReadMethod()).as(propertyName + " getter").isNotNull();
                assertThat(property.getWriteMethod()).as(propertyName + " setter").isNotNull();
            });
        });

    }

    private ClientBuilder<TestClientBuilder, TestClient> testClientBuilder() {
        return new TestClientBuilder().credentialsProvider(new AwsStaticCredentialsProvider(new AnonymousAwsCredentials()))
                                      .defaultRegionDetectionEnabled(false);
    }

    private static class TestClient {
        private final ImmutableSyncClientConfiguration syncClientConfiguration;
        private final ImmutableAsyncClientConfiguration asyncClientConfiguration;
        private final String signingRegion;

        public TestClient(ImmutableSyncClientConfiguration syncClientConfiguration,
                          ImmutableAsyncClientConfiguration asyncClientConfiguration,
                          String signingRegion) {
            this.syncClientConfiguration = syncClientConfiguration;
            this.asyncClientConfiguration = asyncClientConfiguration;
            this.signingRegion = signingRegion;
        }
    }

    private static class TestClientBuilder extends DefaultClientBuilder<TestClientBuilder, TestClient>
            implements ClientBuilder<TestClientBuilder, TestClient> {

        @Override
        protected TestClient buildClient() {
            return new TestClient(super.syncClientConfiguration(),
                                  super.asyncClientConfiguration(),
                                  super.signingRegion());
        }

        @Override
        protected String serviceEndpointPrefix() {
            return ENDPOINT_PREFIX;
        }

        @Override
        protected ClientConfigurationDefaults serviceDefaults() {
            return new ClientConfigurationDefaults() {
                @Override
                protected void applySecurityDefaults(ClientSecurityConfiguration.Builder builder) {
                    builder.signerProvider(SIGNER_PROVIDER);
                }
            };
        }
    }
}
