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

import java.util.Iterator;
import java.util.Optional;
import java.util.ServiceLoader;
import software.amazon.awssdk.SdkClientException;
import software.amazon.awssdk.annotation.ReviewBeforeRelease;
import software.amazon.awssdk.http.SdkHttpService;
import software.amazon.awssdk.utils.SdkSystemSetting;

/**
 * {@link SdkHttpServiceProvider} implementation that uses {@link ServiceLoader} to find HTTP implementations on the
 * classpath. If more than one implementation is found on the classpath then an exception is thrown.
 */
class ClasspathSdkHttpServiceProvider implements SdkHttpServiceProvider {

    private final SdkServiceLoader serviceLoader;

    ClasspathSdkHttpServiceProvider() {
        this(new SdkServiceLoader());
    }

    ClasspathSdkHttpServiceProvider(SdkServiceLoader serviceLoader) {
        this.serviceLoader = serviceLoader;
    }

    @Override
    @ReviewBeforeRelease("Add some links to doc topics on configuring HTTP impl")
    public Optional<SdkHttpService> loadService() {
        final Iterator<SdkHttpService> httpServices = serviceLoader.loadServices(SdkHttpService.class);
        if (!httpServices.hasNext()) {
            return Optional.empty();
        }
        SdkHttpService httpService = httpServices.next();

        if (httpServices.hasNext()) {
            throw new SdkClientException(
                    String.format(
                            "Multiple HTTP implementations were found on the classpath. To avoid non-determinstic loading " +
                            "implementations, please explicitly provide an HTTP client via the client builders, set the %s " +
                            "system property with the FQCN of the HTTP service to use as the default, or remove all but one " +
                            "HTTP implementation from the classpath", SdkSystemSetting.HTTP_SERVICE_IMPL.property()));
        }
        return Optional.of(httpService);
    }

}
