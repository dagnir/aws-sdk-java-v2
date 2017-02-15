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

package software.amazon.awssdk.services.dynamodbv2.xspec;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class ArrayIndexElementTest {

    @Test
    public void test() {
        ArrayIndexElement e1 = new ArrayIndexElement(1);
        ArrayIndexElement e2 = new ArrayIndexElement(1);
        ArrayIndexElement e3 = new ArrayIndexElement(2);
        assertEquals(e1, e2);
        assertFalse(e1.equals(e3));

    }

    @Test(expected = IllegalStateException.class)
    public void testIllegalOp() {
        new ArrayIndexElement(1).asToken(null);
    }
}
