/*
 * Copyright 2015 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package software.amazon.awssdk.services.s3.notif;

import java.io.InputStream;
import org.junit.Assert;
import org.junit.Test;
import software.amazon.awssdk.services.s3.model.BucketNotificationConfiguration;
import software.amazon.awssdk.services.s3.model.Filter;
import software.amazon.awssdk.services.s3.model.FilterRule;
import software.amazon.awssdk.services.s3.model.transform.BucketNotificationConfigurationStaxUnmarshaller;

public class BucketNotificationUnmarshallerTest {

    /**
     * Testing a very specific bug where the {@link BucketNotificationConfiguration} unmarshaller
     * was not clearing the state of {@link Filter} and {@link FilterRule} so if a configuration
     * without a filter follows one with a filter they will both share the same filter objects
     * instead of the second configuration having a null filter. Currently can't be verified with an
     * integration test since S3 doesn't support overlapping prefixes yet and a configuration
     * without a filter is treated as a match all and overlaps with everything
     */
    @Test
    public void parseNotificationConfiguration_UnmarshallerClearsStateWhenNewConfigurationStarted() throws Exception {
        InputStream stream = getClass().getResourceAsStream("UnmarshallerClearsStateWhenNewConfigurationStarted.xml");
        BucketNotificationConfiguration config = BucketNotificationConfigurationStaxUnmarshaller.getInstance()
                .unmarshall(stream);
        Assert.assertNotNull(config.getConfigurationByName("notif-one").getFilter());
        Assert.assertNull(config.getConfigurationByName("notif-two").getFilter());
    }
}
