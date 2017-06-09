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

package software.amazon.awssdk.http.loader;

import java.util.Optional;
import software.amazon.awssdk.SdkClientException;
import software.amazon.awssdk.http.SdkHttpService;
import software.amazon.awssdk.utils.SdkSystemSetting;

/**
 * Attempts to load the default implementation from the system property {@link SdkSystemSetting#HTTP_SERVICE_IMPL}. The property
 * value should be the fully qualified class name of the factory to use.
 */
class SystemPropertyHttpServiceProvider implements SdkHttpServiceProvider {

    @Override
    public Optional<SdkHttpService> loadService() {
        return SdkSystemSetting.HTTP_SERVICE_IMPL
                .getStringValue()
                .map(this::createServiceFromProperty);
    }

    private SdkHttpService createServiceFromProperty(String httpImplFqcn) {
        try {
            return (SdkHttpService) Class.forName(httpImplFqcn).newInstance();
        } catch (Exception e) {
            throw new SdkClientException(String.format(
                    "Unable to load the HTTP factory implementation from the %s system property. " +
                    "Ensure the class '%s' is present on the classpath and has a no-arg constructor",
                    SdkSystemSetting.HTTP_SERVICE_IMPL.property(), httpImplFqcn), e);
        }
    }

}
