/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.core.util;

import java.util.List;

/**
 * A {@link List} that distinguishes whether it was auto constructed; i.e. it
 * was constructed using its default, no-arg constructor.
 *
 * @param <T> The element type.
 */
public interface SdkAutoConstructAwareList<T> extends List<T> {
    /**
     * @return {@code true} if this list was auto constructed.
     */
    boolean isAutoConstructed();
}
