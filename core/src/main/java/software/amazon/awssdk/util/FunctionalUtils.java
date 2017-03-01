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

package software.amazon.awssdk.util;

import java.util.function.Consumer;

public final class FunctionalUtils {

    /**
     * A wrapper around a Consumer that throws a checked exception
     * @param unsafeConsumer - something that acts like a consumer but throws an exception
     * @return a consumer that is wrapped in a try-catch converting the checked exception into a runtime exception
     */
    public static <I> Consumer<I> safely(UnsafeConsumer<I> unsafeConsumer) {
        return (input) -> {
            try {
                unsafeConsumer.accept(input);
            } catch (Exception throwable) {
                throw new RuntimeException(throwable);
            }
        };
    }

    public interface UnsafeConsumer<I> {
        void accept(I input) throws Exception;
    }
}
