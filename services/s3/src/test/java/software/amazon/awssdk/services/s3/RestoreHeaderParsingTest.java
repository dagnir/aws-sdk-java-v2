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

import java.util.Date;
import org.junit.Assert;
import org.junit.Test;
import software.amazon.awssdk.http.HttpResponse;
import software.amazon.awssdk.services.s3.internal.ObjectRestoreHeaderHandler;
import software.amazon.awssdk.services.s3.internal.ObjectRestoreResult;

public class RestoreHeaderParsingTest {

    @Test
    public void testParseEmptyRestoreHeader() {
        MockObjectRestoreResult result = new MockObjectRestoreResult();

        ObjectRestoreHeaderHandler<MockObjectRestoreResult> handler =
                new ObjectRestoreHeaderHandler<MockObjectRestoreResult>();

        HttpResponse response = new HttpResponse(null, null);

        handler.handle(result, response);

        Assert.assertNull(result.getRestoreExpirationTime());
        Assert.assertNull(result.getOngoingRestore());
    }

    @Test
    public void testParseRestoreHeader1() {
        MockObjectRestoreResult result = new MockObjectRestoreResult();

        ObjectRestoreHeaderHandler<MockObjectRestoreResult> handler =
                new ObjectRestoreHeaderHandler<MockObjectRestoreResult>();

        HttpResponse response = new HttpResponse(null, null);
        response.addHeader(
                "x-amz-restore", "ongoing-request=\"true\""
                          );

        handler.handle(result, response);

        Assert.assertTrue(result.getOngoingRestore());
    }

    @Test
    public void testParseRestoreHeader2() {
        MockObjectRestoreResult result = new MockObjectRestoreResult();

        ObjectRestoreHeaderHandler<MockObjectRestoreResult> handler =
                new ObjectRestoreHeaderHandler<MockObjectRestoreResult>();

        HttpResponse response = new HttpResponse(null, null);
        response.addHeader(
                "x-amz-restore",
                "ongoing-request=\"false\", "
                + "expiry-date=\"Tue, 01 Jan 2013 00:00:00 GMT\""
                          );

        handler.handle(result, response);

        Assert.assertFalse(result.getOngoingRestore());
        Assert.assertEquals(1356998400000L,
                            result.getRestoreExpirationTime().getTime());
    }

    @Test
    public void testParseRestoreHeader3() {
        MockObjectRestoreResult result = new MockObjectRestoreResult();

        ObjectRestoreHeaderHandler<MockObjectRestoreResult> handler =
                new ObjectRestoreHeaderHandler<MockObjectRestoreResult>();

        HttpResponse response = new HttpResponse(null, null);
        response.addHeader(
                "x-amz-restore",
                "expiry-date=\"Tue, 01 Jan 2013 00:00:00 GMT\", "
                + "ongoing-request=\"false\""
                          );

        handler.handle(result, response);

        Assert.assertFalse(result.getOngoingRestore());
        Assert.assertEquals(1356998400000L,
                            result.getRestoreExpirationTime().getTime());
    }

    private static class MockObjectRestoreResult
            implements ObjectRestoreResult {

        private Date restoreTime;
        private Boolean ongoingRestore;

        @Override
        public Date getRestoreExpirationTime() {
            return restoreTime;
        }

        @Override
        public void setRestoreExpirationTime(final Date value) {
            restoreTime = value;
        }

        @Override
        public Boolean getOngoingRestore() {
            return ongoingRestore;
        }

        @Override
        public void setOngoingRestore(final boolean value) {
            ongoingRestore = value;
        }
    }
}
