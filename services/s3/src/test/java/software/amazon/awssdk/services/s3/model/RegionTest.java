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

import java.util.regex.Matcher;
import org.junit.Assert;
import org.junit.Test;

public class RegionTest {

    @Test
    public void bogusRegion() {
        Matcher m = software.amazon.awssdk.services.s3.model.Region.S3_REGIONAL_ENDPOINT_PATTERN
                .matcher("s3-bucket.amazonaws.com.s3-us-west-2.amazonaws.com");
        Assert.assertFalse(m.matches());
    }

    @Test
    public void fromValue_usEast1String_ReturnsUsStandard() {
        assertEquals(Region.US_Standard, Region.fromValue("us-east-1"));
    }

    @Test
    public void toAWSRegion_UsStandard_ReturnsUsEast1Region() {
        assertEquals("us-east-1", Region.US_Standard.toAwsRegion().value());
    }

}
