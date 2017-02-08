/*
 * Copyright (c) 2016. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 * http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.services.dynamodbv2.xspec;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PathTest {

    @Test
    public void namedPaths() {
        Path p = new Path("a.b");
        assertTrue(p.getElements().size() == 2);
        assertEquals("a.b", p.toString());

        SubstitutionContext context = new SubstitutionContext();
        assertEquals("#0.#1", p.asSubstituted(context));
        assertTrue(context.numNameTokens() == 2);
    }

    @Test
    public void namedIndexedPaths() {
        Path p = new Path("a.b[2].a");
        assertTrue(p.getElements().size() == 4);
        assertEquals("a.b[2].a", p.toString());

        SubstitutionContext context = new SubstitutionContext();
        assertEquals("#0.#1[2].#0", p.asSubstituted(context));
        assertTrue(context.numNameTokens() == 2);
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void testBogusPath() {
        new Path("[");
    }

    @Test(expected=NullPointerException.class)
    public void testNull() {
        new Path(null);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testMissingBracket() {
        new Path("a[");
    }

    @Test(expected=NumberFormatException.class)
    public void testEmptyIndex() {
        new Path("a[]");
    }

    @Test(expected=IllegalArgumentException.class)
    public void testNegativeIndex() {
        new Path("a[-1]");
    }

    @Test(expected=IllegalArgumentException.class)
    public void testBogusIndexes() {
        new Path("a[1]bbb[2]");
    }

    @Test
    public void testNestedIndexes() {
        new Path("a[1][2]");
    }

    @Test(expected=IllegalArgumentException.class)
    public void testBogusChars() {
        new Path("a[1]b");
    }
}
