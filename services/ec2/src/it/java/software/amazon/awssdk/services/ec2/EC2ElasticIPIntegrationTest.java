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

import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.AfterClass;
import org.junit.Test;
import software.amazon.awssdk.services.ec2.model.Address;
import software.amazon.awssdk.services.ec2.model.AllocateAddressRequest;
import software.amazon.awssdk.services.ec2.model.AllocateAddressResult;
import software.amazon.awssdk.services.ec2.model.DescribeAddressesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeAddressesResult;
import software.amazon.awssdk.services.ec2.model.Filter;
import software.amazon.awssdk.services.ec2.model.ReleaseAddressRequest;

/**
 * Integration tests for the Elastic IP operations in the Amazon EC2 Java client
 * library.
 *
 * @author fulghum@amazon.com
 */
public class EC2ElasticIPIntegrationTest extends EC2IntegrationTestBase {

    private static String allocationId;

    private static String elasticIp;

    /**
     * Ensures that all testing resources are correctly released.
     */
    @AfterClass
    public static void tearDown() {
        if (allocationId != null) {
            ec2.releaseAddress(ReleaseAddressRequest.builder().allocationId(allocationId).build());
            assertFalse(doAddressesContainIp(ec2.describeAddresses(DescribeAddressesRequest.builder().build()).addresses(),
                                             elasticIp));
        }
    }

    /**
     * Tests if the specified expected IP address is contained in the list of
     * address objects. If the expected IP address is not found in any of the
     * address objects, this method will return false.
     *
     * @param addresses
     *            The list of Address objects to check.
     * @param expectedIp
     *            The IP expected to be in the list of addresses.
     *
     * @return True if the list of addresses contains an address with the
     *         specified Elastic IP, otherwise false.
     */
    private static boolean doAddressesContainIp(List<Address> addresses, String expectedIp) {
        assertNotNull(addresses);

        for (Address address : addresses) {
            String ip = address.publicIp();

            // Bail out early if we see the IP we're expecting.
            if (ip.equals(expectedIp)) {
                return true;
            }
        }

        return false;
    }


    /*
     * Test Helper Methods
     */

    /**
     * Tests that the various EC2 Elastic IP operations work correctly by going
     * through a series of calls that hit all of the Elastic IP operations.
     */
    @Test
    public void testElasticIpOperations() {

        // Allocate an address
        AllocateAddressResult allocateAddressResult = ec2.allocateAddress(AllocateAddressRequest.builder().build());

        elasticIp = allocateAddressResult.publicIp();
        allocationId = allocateAddressResult.allocationId();

        assertNotNull(elasticIp);
        assertThat(elasticIp.length(), greaterThan(5));

        // Describe addresses
        DescribeAddressesResult describeAddressesResult = ec2.describeAddresses(DescribeAddressesRequest.builder().build());
        assertNotNull(describeAddressesResult);
        List<Address> addresses = describeAddressesResult.addresses();
        assertTrue(doAddressesContainIp(addresses, elasticIp));

        // Describe with a filter
        addresses = ec2.describeAddresses(DescribeAddressesRequest.builder()
                                                                  .filters(Filter.builder()
                                                                                 .name("public-ip")
                                                                                 .values(elasticIp).build()).build()
                                         ).addresses();
        assertEquals(1, addresses.size());
        assertTrue(doAddressesContainIp(addresses, elasticIp));
    }

}
