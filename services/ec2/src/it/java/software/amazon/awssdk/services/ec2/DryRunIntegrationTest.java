/*
 * Copyright 2016-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import org.junit.Assert;
import org.junit.Test;
import software.amazon.awssdk.services.ec2.model.DescribeRegionsRequest;

public class DryRunIntegrationTest extends EC2IntegrationTestBase {
    
    @Test
    public void dry_run_returns_success() {
        Assert.assertNotNull(ec2.dryRun(new DescribeRegionsRequest()));
    }
}
