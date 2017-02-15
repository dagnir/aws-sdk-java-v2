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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.services.s3.model.ListVersionsRequest;

public class S3VersionsInBucketTest extends S3VersionsTestCommon {

    @Before
    public void setUp() throws Exception {
        s3Versions = S3Versions.inBucket(s3, "my-bucket");
    }

    @Test
    public void testSetsBucket() throws Exception {
        assertEquals("my-bucket", s3Versions.getBucketName());
    }

    @Test
    public void testPrefixIsNull() throws Exception {
        assertNull(s3Versions.getPrefix());
    }

    @Test
    public void testSetsS3Client() throws Exception {
        assertSame(s3, s3Versions.getS3());
    }

    @Test
    public void testSetsNullPrefixOnRequest() throws Exception {
        s3Versions.iterator().hasNext();

        ArgumentCaptor<ListVersionsRequest> listCaptor = ArgumentCaptor.forClass(ListVersionsRequest.class);
        verify(s3).listVersions(listCaptor.capture());
        assertNull(listCaptor.getValue().getPrefix());
    }

}
