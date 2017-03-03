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

import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.function.Consumer;
import org.junit.Test;

public class FunctionalUtilsTest {

    @Test
    public void safeConsumer_ThrowsCheckedException_ConvertsToRuntimeException() {
        final Consumer<String> safeConsumer = FunctionalUtils.safely(FunctionalUtilsTest::consumeAndThrow);

        try {
            safeConsumer.accept("foo");
            fail("Should have thrown exception.");
        } catch (RuntimeException expected) {
        }
    }

    @Test
    public void safeConsumer_ThrowsCheckedIOException_ConvertsToUncheckedIOException() {
        final Consumer<String> safeConsumer = FunctionalUtils.safely(FunctionalUtilsTest::consumeAndThrowIo);

        try {
            safeConsumer.accept("foo");
            fail("Should have thrown exception.");
        } catch (UncheckedIOException expected) {
        }
    }

    @Test
    public void safeConsumer_ThrowsRuntimeException_DoesNotWrap() {
        final Consumer<String> safeConsumer = FunctionalUtils.safely(FunctionalUtilsTest::consumeAndThrowRuntime);

        try {
            safeConsumer.accept("foo");
            fail("Should have thrown exception.");
        } catch (IllegalArgumentException expected) {
        }
    }

    @Test
    public void invokeSafely_ThrowsCheckedException_ConvertsToRuntimeException() {
        try {
            FunctionalUtils.invokeSafely(FunctionalUtilsTest::supplierThatThrows);
            fail("Should have thrown exception.");
        } catch (RuntimeException expected) {
        }
    }

    @Test
    public void invokeSafely_ThrowsCheckedIOException_ConvertsToUncheckedIOException() {
        try {
            FunctionalUtils.invokeSafely(FunctionalUtilsTest::supplierThatThrowsIo);
            fail("Should have thrown exception.");
        } catch (UncheckedIOException expected) {
        }
    }

    @Test
    public void invokeSafely_ThrowsRuntimeException_DoesNotWrap() {
        try {
            FunctionalUtils.invokeSafely(FunctionalUtilsTest::supplierThatThrowsRuntime);
            fail("Should have thrown exception.");
        } catch (IllegalArgumentException expected) {
        }
    }

    private static void consumeAndThrow(String value) throws Exception {
        throw new Exception("checked");
    }

    private static void consumeAndThrowIo(String value) throws IOException {
        throw new IOException("checked");
    }

    private static void consumeAndThrowRuntime(String value) throws IOException {
        throw new IllegalArgumentException("runtime");
    }

    private static String supplierThatThrows() throws Exception {
        throw new Exception("checked");
    }

    private static String supplierThatThrowsIo() throws IOException {
        throw new IOException("checked");
    }

    private static String supplierThatThrowsRuntime() {
        throw new IllegalArgumentException("runtime");
    }
}