/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights
 * Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is
 * distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either
 * express or implied. See the License for the specific language
 * governing
 * permissions and limitations under the License.
 */
package software.amazon.awssdk.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/** Unit tests for the utility methods that parse information from AWS URLs. */
public class AwsHostNameUtilsInternalTest {

    /**
     * Tests the s3 specific regions for internal end points.
     */
    @Test
    public void testS3SpecialRegions() {
        assertEquals("us-east-1", AwsHostNameUtils.parseRegionName(
                "s3-external-2.amazon.com", null));
        assertEquals("us-east-1", AwsHostNameUtils.parseRegionName(
                "bucket.name.with.periods.s3-external-2.amazon.com", null));
        assertEquals("us-east-1", AwsHostNameUtils.parseRegionName(
                "s3-internal-iad.amazon.com", null));
        assertEquals("us-east-1", AwsHostNameUtils.parseRegionName(
                "bucket.name.with.periods.s3-internal-iad.amazon.com", null));
        assertEquals("us-east-1", AwsHostNameUtils.parseRegionName(
                "s3-internal-sea.amazon.com", null));
        assertEquals("us-east-1", AwsHostNameUtils.parseRegionName(
                "bucket.name.with.periods.s3-internal-sea.amazon.com", null));

        assertEquals("eu-west-1", AwsHostNameUtils.parseRegionName(
                "s3-internal-dub.amazon.com", null));
        assertEquals("eu-west-1", AwsHostNameUtils.parseRegionName(
                "bucket.name.with.periods.s3-internal-dub.amazon.com", null));
    }

    @Test
    public void testMVP() {
        // Verify that MVP endpoints parse correctly even though they're
        // non-standard.
        assertEquals("us-iso-east-1", AwsHostNameUtils.parseRegionName(
                "iam.us-iso-east-1.c2s.ic.gov", "iam"));
        assertEquals("us-iso-east-1", AwsHostNameUtils.parseRegionName(
                "ec2.us-iso-east-1.c2s.ic.gov", "ec2"));
        assertEquals("us-iso-east-1", AwsHostNameUtils.parseRegionName(
                "s3.us-iso-east-1.c2s.ic.gov", "s3"));
        assertEquals("us-iso-east-1", AwsHostNameUtils.parseRegionName(
                "bucket.name.with.periods.s3.us-iso-east-1.c2s.ic.gov", "s3"));

        assertEquals("us-iso-east-1", AwsHostNameUtils.parseRegionName(
                "cloudsearch.us-iso-east-1.c2s.ic.gov", "cloudsearch"));
        assertEquals("us-iso-east-1", AwsHostNameUtils.parseRegionName(
                "domain.us-iso-east-1.cloudsearch.c2s.ic.gov", "cloudsearch"));
    }

    @Test
    public void testStandardNoHint() {
        // Verify that standard endpoints parse correctly without a service hint
        assertEquals("us-east-1", AwsHostNameUtils.parseRegionName(
                "s3-internal-iad.amazon.com", null));
    }

    @Test
    public void testParseRegionS3SpecialRegions() {
        assertEquals("us-east-1", AwsHostNameUtils.parseRegion(
                "s3-external-2.amazon.com", null));
        assertEquals("us-east-1", AwsHostNameUtils.parseRegion(
                "bucket.name.with.periods.s3-external-2.amazon.com", null));
        assertEquals("us-east-1", AwsHostNameUtils.parseRegion(
                "s3-internal-iad.amazon.com", null));
        assertEquals("us-east-1", AwsHostNameUtils.parseRegion(
                "bucket.name.with.periods.s3-internal-iad.amazon.com", null));
        assertEquals("us-east-1", AwsHostNameUtils.parseRegion(
                "s3-internal-sea.amazon.com", null));
        assertEquals("us-east-1", AwsHostNameUtils.parseRegion(
                "bucket.name.with.periods.s3-internal-sea.amazon.com", null));

        assertEquals("eu-west-1", AwsHostNameUtils.parseRegion(
                "s3-internal-dub.amazon.com", null));
        assertEquals("eu-west-1", AwsHostNameUtils.parseRegion(
                "bucket.name.with.periods.s3-internal-dub.amazon.com", null));
    }

    @Test
    public void testParseRegionWithMVPEndpoints() {
        // Verify that MVP endpoints parse correctly even though they're
        // non-standard.
        assertEquals("us-iso-east-1", AwsHostNameUtils.parseRegion(
                "iam.us-iso-east-1.c2s.ic.gov", "iam"));
        assertEquals("us-iso-east-1", AwsHostNameUtils.parseRegion(
                "ec2.us-iso-east-1.c2s.ic.gov", "ec2"));
        assertEquals("us-iso-east-1", AwsHostNameUtils.parseRegion(
                "s3.us-iso-east-1.c2s.ic.gov", "s3"));
        assertEquals("us-iso-east-1", AwsHostNameUtils.parseRegion(
                "bucket.name.with.periods.s3.us-iso-east-1.c2s.ic.gov", "s3"));

        assertEquals("us-iso-east-1", AwsHostNameUtils.parseRegion(
                "cloudsearch.us-iso-east-1.c2s.ic.gov", "cloudsearch"));
        assertEquals("us-iso-east-1", AwsHostNameUtils.parseRegion(
                "domain.us-iso-east-1.cloudsearch.c2s.ic.gov", "cloudsearch"));
    }

    @Test
    public void testParseRegionStandardNoHint() {
        // Verify that standard endpoints parse correctly without a service hint
        assertEquals("us-east-1", AwsHostNameUtils.parseRegion(
                "s3-internal-iad.amazon.com", null));
    }
}
