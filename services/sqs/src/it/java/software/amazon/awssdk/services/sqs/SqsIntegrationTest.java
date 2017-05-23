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

package software.amazon.awssdk.services.sqs;

import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.util.Map;
import org.junit.Test;
import software.amazon.awssdk.SdkGlobalConfiguration;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest;
import software.amazon.awssdk.services.sqs.model.ListQueuesRequest;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;

/**
 * Integration tests for the SQS Java client.
 *
 * @author Jason Fulghum fulghum@amazon.com
 */
public class SqsIntegrationTest extends IntegrationTestBase {

    private String getQueueCreationDate(SQSAsyncClient sqs, String queueURL) {
        GetQueueAttributesRequest request = GetQueueAttributesRequest.builder().queueUrl(queueURL)
                .attributeNames(QueueAttributeName.CreatedTimestamp.toString())
                .build();
        Map<String, String> attributes = sqs.getQueueAttributes(request).join().attributes();
        return attributes.get(QueueAttributeName.CreatedTimestamp.toString());
    }

    /**
     * In the following test, we purposely setting the time offset to trigger a clock skew error.
     * The time offset must be fixed and then we validate the global value for time offset has been
     * update.
     */
    @Test
    public void clockSkewFailure_CorrectsGlobalTimeOffset() throws Exception {
        final int originalOffset = SdkGlobalConfiguration.getGlobalTimeOffset();
        final int skew = 3600;

        SdkGlobalConfiguration.setGlobalTimeOffset(skew);
        assertEquals(skew, SdkGlobalConfiguration.getGlobalTimeOffset());
        SQSAsyncClient sqsClient = createSqsAyncClient();
        sqsClient.listQueues(ListQueuesRequest.builder().build());
        assertThat("Clockskew is fixed!", SdkGlobalConfiguration.getGlobalTimeOffset(), lessThan(skew));
        // subsequent changes to the global time offset won't affect existing client
        SdkGlobalConfiguration.setGlobalTimeOffset(skew);
        sqsClient.listQueues(ListQueuesRequest.builder().build());
        assertEquals(skew, SdkGlobalConfiguration.getGlobalTimeOffset());
        sqsClient.close();

        SdkGlobalConfiguration.setGlobalTimeOffset(originalOffset);
    }
}
