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

package software.amazon.awssdk.services.ec2;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Test;
import software.amazon.awssdk.services.ec2.model.AccountAttribute;
import software.amazon.awssdk.services.ec2.model.DescribeAccountAttributesRequest;

public class EC2AccountAttributesIntegrationTest extends EC2IntegrationTestBase {

    @Test
    public void testDescribeAccountAttributes() {

        List<AccountAttribute> attributes = ec2.describeAccountAttributes(
                new DescribeAccountAttributesRequest()
                                                                         ).getAccountAttributes();

        assertNotNull(attributes);
        assertTrue(attributes.size() > 0);

        for (AccountAttribute attribute : attributes) {
            assertStringNotEmpty(attribute.getAttributeName());
            assertNotNull(attribute.getAttributeValues());
        }
    }
}
