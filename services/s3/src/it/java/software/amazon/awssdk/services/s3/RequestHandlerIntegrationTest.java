/*
 * Copyright 2011-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *    http://aws.amazon.com/apache2.0
 *
 * This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and
 * limitations under the License.
 */
package software.amazon.awssdk.services.s3;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import software.amazon.awssdk.AmazonServiceException;
import software.amazon.awssdk.Request;
import software.amazon.awssdk.handlers.RequestHandler;
import software.amazon.awssdk.regions.Regions;
import software.amazon.awssdk.services.s3.model.DeleteBucketRequest;
import software.amazon.awssdk.util.TimingInfo;

/**
 * Tests that new request handlers can be registered for a client, and that
 * they're correctly run during a request's lifecyle.
 */
public class RequestHandlerIntegrationTest extends S3IntegrationTestBase {

    @Test
    public void testRequestHandlersWithoutRegion() throws Exception {
        TestRequestHandler requestHandler = new TestRequestHandler();
        s3.addRequestHandler(requestHandler);

        try {
            s3.deleteBucket(new DeleteBucketRequest("asdfasdasdfasdf"));
            fail("Expected exception not thrown");
        } catch (AmazonServiceException ase) {
            requestHandler.assertCallCounts(2, 0, 2);
        }

        requestHandler.resetCallCounts();
        s3.listBuckets();
        requestHandler.assertCallCounts(1, 1, 0);
    }

    @Test
    public void testRequestHandlersWithRegion() throws Exception {
        TestRequestHandler requestHandler = new TestRequestHandler();
        s3.addRequestHandler(requestHandler);
        s3.configureRegion(Regions.US_EAST_1);

        try {
            s3.deleteBucket(new DeleteBucketRequest("asdfasdasdfasdf"));
            fail("Expected exception not thrown");
        } catch (AmazonServiceException ase) {
            requestHandler.assertCallCounts(1, 0, 1);
        }

        requestHandler.resetCallCounts();
        s3.listBuckets();
        requestHandler.assertCallCounts(1, 1, 0);
    }


    /*
     * TODO: Duplicated in the SimpleDB tests for RequestHandler support
     *       in the generated clients.  It'd be nice to have a shared
     *       package that all SDK clients could declare a test-dependency on
     *       to share utilities like this.
     */
    private final class TestRequestHandler implements RequestHandler {
        public int beforeRequestCallCount = 0;
        public int afterResponseCallCount = 0;
        public int afterErrorCallCount    = 0;

        @Override
        public void beforeRequest(Request<?> request) {
            assertNotNull(request);
            beforeRequestCallCount++;
        }

        @Override
        public void afterResponse(Request<?> request, Object response, TimingInfo timingInfo) {
            assertNotNull(request);
            assertNotNull(response);
            assertNotNull(timingInfo);
            assertTrue(timingInfo.getStartEpochTimeMilli() > 0);
            assertTrue(timingInfo.getStartTimeNano() > 0);
            assertTrue(timingInfo.getEndTimeNano() > timingInfo.getStartTimeNano());
            assertTrue(timingInfo.getEndEpochTimeMilli() >= timingInfo.getStartEpochTimeMilli());
            afterResponseCallCount++;
        }

        @Override
        public void afterError(Request<?> request, Exception ace) {
            assertNotNull(request);
            assertNotNull(ace);
            afterErrorCallCount++;
        }

        public void resetCallCounts() {
            beforeRequestCallCount = 0;
            afterResponseCallCount = 0;
            afterErrorCallCount    = 0;
        }

        public void assertCallCounts(int expectedBeforeRequestCount,
                                     int expectedAfterResponseCount,
                                     int expectedAfterErrorCount) {
            assertEquals(expectedBeforeRequestCount, beforeRequestCallCount);
            assertEquals(expectedAfterResponseCount, afterResponseCallCount);
            assertEquals(expectedAfterErrorCount,    afterErrorCallCount);
        }
    }
}
