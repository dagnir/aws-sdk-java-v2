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

package software.amazon.awssdk.services.s3.internal;

import org.junit.Assert;
import org.junit.Test;
import software.amazon.awssdk.DefaultRequest;
import software.amazon.awssdk.Request;

public class RestUtilsTest {
    @Test
    public void testNullHeaderValue() {
        Request<?> request = new DefaultRequest<Object>("s3");
        request.addHeader("x-amz-test", null);

        String canonicalString = RestUtils.makeS3CanonicalString(
                "PUT", "/bucket/key", request, null);

        Assert.assertEquals(
                "PUT\n\n\nx-amz-test:\n/bucket/key",
                canonicalString);
    }
}
