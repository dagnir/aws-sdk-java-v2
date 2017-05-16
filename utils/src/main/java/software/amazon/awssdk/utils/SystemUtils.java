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

package software.amazon.awssdk.utils;

import java.util.Optional;

/**
 * A set of static utility methods for simplifying use of {@link System}.
 */
public class SystemUtils {
    private static final Logger LOG = Logger.loggerFor(SystemUtils.class);

    private SystemUtils() {}

    /**
     * Attempt to load a system setting from {@link System#getProperty(String)} and {@link System#getenv(String)}. This should be
     * used in favor of those methods because the SDK should support both methods of configuration.
     *
     * {@link System#getProperty(String)} takes precedent over {@link System#getenv(String)} if both are specified.
     *
     * @param settingKey The environment variable or property that should be checked.
     * @return The requested setting, or {@link Optional#empty()} if the values were not set, or the security manager did not
     *         allow reading the setting.
     */
    public static Optional<String> getSetting(String settingKey) {
        try {
            String propertySetting = System.getProperty(settingKey);
            String result = propertySetting != null ? propertySetting : System.getenv(settingKey);
            return Optional.ofNullable(result);
        } catch (SecurityException e) {
            LOG.debug(() -> "Unable to load system setting '" + settingKey + "' because the security manager did not allow the "
                            + "SDK to read this system property. This setting will be assumed to be null.");
            return Optional.empty();
        }
    }
}
