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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.function.Consumer;

public final class FunctionalUtils {

    /**
     * A wrapper around a Consumer that throws a checked exception
     *
     * @param unsafeConsumer - something that acts like a consumer but throws an exception
     * @return A consumer that is wrapped in a try-catch converting the checked exception into a runtime exception
     * @throws RuntimeException     If any checked {@link Exception} is thrown by the supplier.
     * @throws UncheckedIOException If any {@link IOException} is thrown by the supplier.
     */
    public static <I> Consumer<I> safely(UnsafeConsumer<I> unsafeConsumer) {
        return (input) -> {
            try {
                unsafeConsumer.accept(input);
            } catch (Exception exception) {
                throw asRuntimeException(exception);
            }
        };
    }

    /**
     * Invoke the given supplier and return the value. Will convert any checked exceptions to runtime exceptions.
     *
     * @param supplier Supplier to invoke.
     * @param <T>      Type to return.
     * @return Value provided by supplier function.
     * @throws RuntimeException     If any checked {@link Exception} is thrown by the supplier.
     * @throws UncheckedIOException If any {@link IOException} is thrown by the supplier.
     */
    public static <T> T invokeSafely(UnsafeSupplier<T> supplier) {
        try {
            return supplier.get();
        } catch (Exception exception) {
            throw asRuntimeException(exception);
        }
    }

    private static RuntimeException asRuntimeException(Exception exception) {
        if (exception instanceof RuntimeException) {
            return (RuntimeException) exception;
        }
        if (exception instanceof IOException) {
            return new UncheckedIOException((IOException) exception);
        }
        return new RuntimeException(exception);
    }

    /**
     * {@link Consumer} that can throw checked exceptions.
     *
     * @param <I> Type of thing being consumed.
     */
    @FunctionalInterface
    public interface UnsafeConsumer<I> {
        void accept(I input) throws Exception;
    }

    /**
     * {@link java.util.function.Supplier} that can throw checked exceptions.
     *
     * @param <I> Type of thing being supplied.
     */
    @FunctionalInterface
    public interface UnsafeSupplier<I> {
        I get() throws Exception;
    }
}
