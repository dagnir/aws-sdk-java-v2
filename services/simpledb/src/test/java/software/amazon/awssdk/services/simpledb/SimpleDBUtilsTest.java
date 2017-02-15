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

package software.amazon.awssdk.services.simpledb;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import software.amazon.awssdk.services.simpledb.util.SimpleDBUtils;

/**
 * Unit tests for the extra utilities packaged with the SimpleDB client.
 */
public class SimpleDBUtilsTest {

    private static final int MAX_DIGITS = 20;

    /**
     * Tests that we can encode/decode zero padded, positive numbers.
     */
    @Test
    public void testEncodeZeroPadding() {
        long expectedLong = 1234567890;
        String encodedLong = SimpleDBUtils.encodeZeroPadding(expectedLong, MAX_DIGITS);
        assertEquals("00000000001234567890", encodedLong);
        assertEquals(expectedLong, SimpleDBUtils.decodeZeroPaddingLong(encodedLong));
    }

    /**
     * Tests that we can encode/decode zero padded real numbers.
     */
    @Test
    public void testEncodeRealNumberRange() {
        long expectedLong = -1234567890;
        long offsetValue = Math.abs(expectedLong) + 1;
        String encodedLong = SimpleDBUtils.encodeRealNumberRange(expectedLong, MAX_DIGITS, offsetValue);
        assertEquals(expectedLong, SimpleDBUtils.decodeRealNumberRangeLong(encodedLong, offsetValue));
    }

    /** Tests that the quoting util functions work properly. */
    @Test
    public void testQuoting() {
        assertEquals("`foo`", SimpleDBUtils.quoteName("foo"));
        assertEquals("`f``o``o`", SimpleDBUtils.quoteName("f`o`o"));
        assertEquals("'foo'", SimpleDBUtils.quoteValue("foo"));
        assertEquals("'f''o''o'", SimpleDBUtils.quoteValue("f'o'o"));

        List<String> emptyList = Arrays.asList(new String[] {});
        List<String> oneElementList = Arrays.asList(new String[] {"foo"});
        List<String> multiElementList = Arrays.asList(new String[] {"foo", "bar"});
        List<String> needsEscapingList = Arrays.asList(new String[] {"foo", "b'a'r"});

        assertEquals("", SimpleDBUtils.quoteValues(emptyList));
        assertEquals("'foo'", SimpleDBUtils.quoteValues(oneElementList));
        assertEquals("'foo','bar'", SimpleDBUtils.quoteValues(multiElementList));
        assertEquals("'foo','b''a''r'", SimpleDBUtils.quoteValues(needsEscapingList));
    }

}
