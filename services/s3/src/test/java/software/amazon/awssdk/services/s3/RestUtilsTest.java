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

package software.amazon.awssdk.services.s3;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import org.junit.Test;
import software.amazon.awssdk.DefaultRequest;
import software.amazon.awssdk.services.s3.internal.RestUtils;

public class RestUtilsTest {

    /**
     * Tests that we correctly include all appropriate parameters in the canonical string to sign.
     */
    @Test
    public void testCanonicalStringToSignParameters() throws Exception {
        DefaultRequest<Void> request = new DefaultRequest<Void>("service");
        request.addParameter("x-amz-foo", "bar");
        request.addParameter("logging", "true");
        request.addParameter("fake", "fake");
        String canonicalString = RestUtils.makeS3CanonicalString("GET", "resource", request, null);
        assertTrue(canonicalString.contains("x-amz-foo:bar"));
        assertTrue(canonicalString.contains("logging=true"));
        assertFalse(canonicalString.contains("fake="));
    }

    /**
     * Tests that the canonicalized request includes all the query parameters
     * when signAllQueryParams is set to true.
     */
    @Test
    public void testCanonicalStringIncludingAllParameters() throws Exception {
        DefaultRequest<Void> request = new DefaultRequest<Void>("service");
        request.addParameter("x-amz-foo", "bar");
        request.addParameter("logging", "true");
        request.addParameter("fake", "fake");

        // additionalQueryParamsToSign = ["fake"]
        String canonicalString = RestUtils.makeS3CanonicalString("GET",
                                                                 "resource", request, null, Arrays.asList("fake"));

        assertTrue(canonicalString.contains("x-amz-foo:bar"));
        assertTrue(canonicalString.contains("logging=true"));
        assertTrue(canonicalString.contains("fake="));

        // additionalQueryParamsToSign = ["non-existent-param"]
        canonicalString = RestUtils.makeS3CanonicalString("GET",
                                                          "resource", request, null, Arrays.asList("non-existent-param"));

        assertTrue(canonicalString.contains("x-amz-foo:bar"));
        assertTrue(canonicalString.contains("logging=true"));
        assertFalse(canonicalString.contains("fake="));
    }
}
