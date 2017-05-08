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

package software.amazon.awssdk.config;

import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.Test;
import software.amazon.awssdk.builder.ToCopyableBuilder;

/**
 * Validate the functionality of the Client*Configuration classes
 */
public class ConfigurationBuilderTest {
    private static final Class<?>[] CONFIGURATION_CLASSES = {
            ClientHttpConfiguration.class,
            ClientHttpProxyConfiguration.class,
            ClientIpConfiguration.class,
            ClientListenerConfiguration.class,
            ClientMarshallerConfiguration.class,
            ClientMetricsConfiguration.class,
            ClientHttpProxyConfiguration.class,
            ClientRetryConfiguration.class,
            ClientSecurityConfiguration.class,
            ClientTcpConfiguration.class,
            ClientTimeoutConfiguration.class,
    };

    private static final Set<String> IGNORED_METHODS = new HashSet<>(Arrays.stream(ToCopyableBuilder.class.getMethods())
                                                                           .map(Method::getName)
                                                                           .collect(Collectors.toList()));

    @Test
    public void configurationClassesAndBuildersHaveExpectedMethods() throws Exception {
        for(Class<?> configurationClass : CONFIGURATION_CLASSES) {
            assertConfigurationClassIsValid(configurationClass);
        }
    }

    private void assertConfigurationClassIsValid(Class<?> configurationClass) throws Exception {
        // Builders should be instantiable from the configuration class
        Method createBuilderMethod = configurationClass.getMethod("builder");
        Object builder = createBuilderMethod.invoke(null);

        // Builders should implement the configuration class's builder interface
        Class<?> builderInterface = Class.forName(configurationClass.getName() + "$Builder");
        assertThat(builder).isInstanceOf(builderInterface);

        Class<?> builderClass = builder.getClass();
        Method[] builderMethods = builderClass.getDeclaredMethods();

        // Builder's build methods should return the configuration object
        Optional<Method> buildMethod = Arrays.stream(builderMethods).filter(m -> m.getName().equals("build")).findFirst();
        assertThat(buildMethod).isPresent();
        Object builtObject = buildMethod.get().invoke(builder);
        assertThat(builtObject).isInstanceOf(configurationClass);

        // Analyze the builder for compliance with the bean specification
        BeanInfo builderBeanInfo = Introspector.getBeanInfo(builderClass);

        Map<String, PropertyDescriptor> builderProperties = Arrays.stream(builderBeanInfo.getPropertyDescriptors())
                                                                  .collect(toMap(PropertyDescriptor::getName, p -> p));

        // Validate method behavior
        for(Method configMethod : configurationClass.getDeclaredMethods()) {
            // Don't validate static methods
            if(Modifier.isStatic(configMethod.getModifiers()) || IGNORED_METHODS.contains(configMethod.getName())) {
                continue;
            }

            // Builders should have bean-style methods for reading and writing properties
            String builderPropertyName = builderClass.getSimpleName() + "'s " + configMethod.getName() + " property";
            PropertyDescriptor builderProperty = builderProperties.get(configMethod.getName());

            assertThat(builderProperty).as(builderPropertyName).isNotNull();
            assertThat(builderProperty.getReadMethod()).as(builderPropertyName + "'s read method").isNotNull();
            assertThat(builderProperty.getWriteMethod()).as(builderPropertyName + "'s write method").isNotNull();

            // Builders should have a fluent read method matching the configuration object
            Method builderFluentReadMethod =
                    Arrays.stream(builderMethods)
                          .filter(builderMethod -> matchesSignature(configMethod, builderMethod))
                          .findAny()
                          .orElseThrow(() -> new AssertionError(builderClass + " can't read " + configMethod.getName()));

            // Builders should have a fluent write method for each property
            Method builderFluentWriteMethod =
                Arrays.stream(builderMethods)
                      .filter(builderMethod -> matchesSignature(configMethod.getName(), builderProperty, builderMethod))
                      .findAny()
                      .orElseThrow(() -> new AssertionError(builderClass + " can't write " + configMethod.getName()));

            // Builder's bean read methods should return a value that can be written to its write methods without
            // raising an exception
            Object beanReadValue = builderProperty.getReadMethod().invoke(builder);
            builderProperty.getWriteMethod().invoke(builder, beanReadValue);
            builderFluentWriteMethod.invoke(builder, beanReadValue);

            // Builder's should be readable via fluent methods without raising an exception
            Object fluentReadValue = builderFluentReadMethod.invoke(builder);

            // Objects built from the builder should be readable without raising an exception
            configMethod.invoke(builtObject);
        }
    }

    private boolean matchesSignature(Method configMethod, Method builderMethod) {
        return builderMethod.getName().equals(configMethod.getName()) &&
               Arrays.equals(builderMethod.getParameters(), configMethod.getTypeParameters());
    }

    private boolean matchesSignature(String methodName, PropertyDescriptor property, Method builderMethod) {
        return builderMethod.getName().equals(methodName) &&
               builderMethod.getParameters().length == 1 &&
               builderMethod.getParameters()[0].getType().equals(property.getPropertyType());
    }
}
