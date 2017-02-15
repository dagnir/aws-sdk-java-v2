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

package software.amazon.awssdk.services.s3.iterable;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;


public class S3ObjectsWithPrefixTest extends S3ObjectsTestCommon {

    @Before
    public void setUp() throws Exception {
        s3Objects = S3Objects.withPrefix(s3, "my-bucket", "photos/");
    }

    @Test
    public void testSetsBucket() throws Exception {
        assertEquals("my-bucket", s3Objects.getBucketName());
    }

    @Test
    public void testSetsPrefix() throws Exception {
        assertEquals("photos/", s3Objects.getPrefix());
    }

}
