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
import static software.amazon.awssdk.config.AdvancedClientOption.ENABLE_DEFAULT_REGION_DETECTION;
import static software.amazon.awssdk.config.AdvancedClientOption.SIGNER_PROVIDER;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.Arrays;
import java.util.Optional;
import org.junit.Test;
import software.amazon.awssdk.auth.AnonymousCredentialsProvider;
import software.amazon.awssdk.auth.Aws4Signer;
import software.amazon.awssdk.auth.StaticSignerProvider;
import software.amazon.awssdk.config.ClientOverrideConfiguration;
import software.amazon.awssdk.config.ImmutableAsyncClientConfiguration;
import software.amazon.awssdk.config.ImmutableSyncClientConfiguration;
import software.amazon.awssdk.config.defaults.ClientConfigurationDefaults;
import software.amazon.awssdk.regions.Region;

/**
 * Validate the functionality of the {@link DefaultClientBuilder}.
 */
public class DefaultClientBuilderTest {
    private static final String ENDPOINT_PREFIX = "prefix";
    private static final StaticSignerProvider TEST_SIGNER_PROVIDER = new StaticSignerProvider(new Aws4Signer());
    private static final URI ENDPOINT = URI.create("https://example.com");

    @Test
    public void buildIncludesServiceDefaults() {
        TestClient client = testClientBuilder().region(Region.US_WEST_1).build();
        assertThat(client.syncClientConfiguration.overrideConfiguration().advancedOption(SIGNER_PROVIDER))
                .isEqualTo(TEST_SIGNER_PROVIDER);
        assertThat(client.asyncClientConfiguration.overrideConfiguration().advancedOption(SIGNER_PROVIDER))
                .isEqualTo(TEST_SIGNER_PROVIDER);
        assertThat(client.signingRegion).isNotNull();
    }

    @Test
    public void buildWithRegionShouldHaveCorrectEndpointAndSigningRegion() {
        TestClient client = testClientBuilder().region(Region.US_WEST_1).build();

        assertThat(client.syncClientConfiguration.endpoint())
                .hasToString("https://" + ENDPOINT_PREFIX + ".us-west-1.amazonaws.com");
        assertThat(client.signingRegion).isEqualTo(Region.US_WEST_1);
    }

    @Test
    public void buildWithEndpointShouldHaveCorrectEndpointAndSigningRegion() {
        TestClient client = testClientBuilder().region(Region.US_WEST_1).endpointOverride(ENDPOINT).build();

        assertThat(client.syncClientConfiguration.endpoint()).isEqualTo(ENDPOINT);
        assertThat(client.signingRegion).isEqualTo(Region.US_WEST_1);
    }

    @Test
    public void buildWithoutRegionOrEndpointOrDefaultProviderThrowsException() {
        assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> {
            testClientBuilder().build();
        });
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
                assertThat(property.getReadMethod()).as(propertyName + " getter").isNull();
                assertThat(property.getWriteMethod()).as(propertyName + " setter").isNotNull();
            });
        });

    }

    private ClientBuilder<TestClientBuilder, TestClient> testClientBuilder() {
        ClientOverrideConfiguration overrideConfig =
                ClientOverrideConfiguration.builder()
                                           .advancedOption(SIGNER_PROVIDER, TEST_SIGNER_PROVIDER)
                                           .advancedOption(ENABLE_DEFAULT_REGION_DETECTION, false)
                                           .build();

        return new TestClientBuilder().credentialsProvider(new AnonymousCredentialsProvider())
                                      .overrideConfiguration(overrideConfig);
    }

    private static class TestClient {
        private final ImmutableSyncClientConfiguration syncClientConfiguration;
        private final ImmutableAsyncClientConfiguration asyncClientConfiguration;
        private final Region signingRegion;

        public TestClient(ImmutableSyncClientConfiguration syncClientConfiguration,
                          ImmutableAsyncClientConfiguration asyncClientConfiguration,
                          Region signingRegion) {
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
                protected void applyOverrideDefaults(ClientOverrideConfiguration.Builder builder) {
                    ClientOverrideConfiguration config = builder.build();
                    builder.advancedOption(SIGNER_PROVIDER, applyDefault(config.advancedOption(SIGNER_PROVIDER), () -> null));
                }
            };
        }
    }
}
