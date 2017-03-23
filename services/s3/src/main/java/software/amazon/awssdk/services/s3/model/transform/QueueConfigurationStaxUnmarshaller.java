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

package software.amazon.awssdk.services.s3.model.transform;

import software.amazon.awssdk.runtime.transform.SimpleTypeStaxUnmarshallers.StringUnmarshaller;
import software.amazon.awssdk.runtime.transform.StaxUnmarshallerContext;
import software.amazon.awssdk.services.s3.model.QueueConfiguration;

class QueueConfigurationStaxUnmarshaller extends NotificationConfigurationStaxUnmarshaller<QueueConfiguration> {

    private static QueueConfigurationStaxUnmarshaller instance = new QueueConfigurationStaxUnmarshaller();

    private QueueConfigurationStaxUnmarshaller() {
    }

    public static QueueConfigurationStaxUnmarshaller getInstance() {
        return instance;
    }

    protected boolean handleXmlEvent(QueueConfiguration queueConfig, StaxUnmarshallerContext context, int targetDepth)
            throws Exception {
        if (context.testExpression("Queue", targetDepth)) {
            queueConfig.setQueueArn(StringUnmarshaller.getInstance().unmarshall(context));
            return true;
        }
        return false;

    }

    protected QueueConfiguration createConfiguration() {
        return new QueueConfiguration();
    }

}
