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

package software.amazon.awssdk.services.s3.model;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class BucketAccelerateStatusTest {

    @Test
    public void fromString_EnabledString_ReturnsEnabledEnum() {
        assertEquals(BucketAccelerateStatus.Enabled, BucketAccelerateStatus.fromValue("Enabled"));
    }

    @Test
    public void fromString_SuspendedString_ReturnsSuspendedEnum() {
        assertEquals(BucketAccelerateStatus.Suspended, BucketAccelerateStatus.fromValue("Suspended"));
    }

    @Test
    public void toString_SuspendedEnum_ReturnsSuspendedString() {
        assertEquals("Suspended", BucketAccelerateStatus.Suspended.toString());
    }

    @Test
    public void toString_EnabledEnum_ReturnsEnabledString() {
        assertEquals("Enabled", BucketAccelerateStatus.Enabled.toString());
    }

    @Test(expected = IllegalArgumentException.class)
    public void fromString_InvalidStatus_ThrowsException() {
        BucketAccelerateStatus.fromValue("InvalidStatus");
    }
}
