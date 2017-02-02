/*
 * Copyright 2015-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import software.amazon.awssdk.AmazonServiceException;
import software.amazon.awssdk.services.ec2.model.AssociateDhcpOptionsRequest;
import software.amazon.awssdk.services.ec2.model.CreateDhcpOptionsRequest;
import software.amazon.awssdk.services.ec2.model.CreateDhcpOptionsResult;
import software.amazon.awssdk.services.ec2.model.DeleteDhcpOptionsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeDhcpOptionsRequest;
import software.amazon.awssdk.services.ec2.model.DhcpConfiguration;
import software.amazon.awssdk.services.ec2.model.DhcpOptions;
import software.amazon.awssdk.services.ec2.model.Filter;

public class EC2DhcpOptionsIntegrationTest extends EC2IntegrationTestBase {

    private String dhcpOptionsId;
    private String domainNameValue;

    private static final String DOMAIN_NAME_KEY = "domain-name";

    /**
     * Create a new DhcpOptions with unique value for the "domain-name" option
     */
    @Before
    public void createDhcpOptions() {

        domainNameValue = "testdomain-" + System.currentTimeMillis();

        DhcpOptions dhcpOptions = createDhcpOptions(
                DOMAIN_NAME_KEY, domainNameValue
                ).getDhcpOptions();
        dhcpOptionsId = dhcpOptions.getDhcpOptionsId();

        assertTrue(findDhcpOption(dhcpOptions,
                DOMAIN_NAME_KEY, domainNameValue));
    }

    /**
     * Release resources used in testing
     */
    @After
    public void tearDown() {
        if (dhcpOptionsId != null) {
            ec2.deleteDhcpOptions(new DeleteDhcpOptionsRequest(dhcpOptionsId));
        }
    }

    /**
     * Test DescribeDhcpOptions API with and without filter parameter
     */
    @Test
    public void testDescribeDhcpOptions() {

        // Add tags to the created DhcpOptions
        tagResource(dhcpOptionsId, TAGS);

        List<DhcpOptions> dhcpOptionsList = ec2.describeDhcpOptions(new DescribeDhcpOptionsRequest()
                    .withDhcpOptionsIds(dhcpOptionsId)
                ).getDhcpOptions();
        assertEquals(1, dhcpOptionsList.size());
        DhcpOptions dhcpOptions = dhcpOptionsList.get(0);

        assertTrue(findDhcpOptoin(dhcpOptionsList,
                DOMAIN_NAME_KEY, domainNameValue));
        assertEqualUnorderedTagLists(TAGS, dhcpOptions.getTags());

        // Query by filtering
        dhcpOptionsList = ec2.describeDhcpOptions(new DescribeDhcpOptionsRequest()
                    .withFilters(new Filter()
                        .withName("dhcp-options-id")
                        .withValues(dhcpOptionsId)
                    )
                ).getDhcpOptions();
        assertEquals(1, dhcpOptionsList.size());
        dhcpOptions = dhcpOptionsList.get(0);

        assertTrue(findDhcpOptoin(dhcpOptionsList,
                DOMAIN_NAME_KEY, domainNameValue));
        assertEqualUnorderedTagLists(TAGS, dhcpOptions.getTags());
    }

    /**
     * This test associates DHCP options with Bogus vpc id (this is
     * so we can verify that Vpc Id parameter is submitted correctly
     */
    @Test
    public void testAssociateDhcpOptions() {

        String vpcId = "BogusId";
        try {
            ec2.associateDhcpOptions(new AssociateDhcpOptionsRequest()
                    .withDhcpOptionsId(dhcpOptionsId)
                    .withVpcId(vpcId));

        } catch (AmazonServiceException e) {
            assertTrue("InvalidVpcID.NotFound".equals(e.getErrorCode()));
            assertTrue(e.getMessage().contains(vpcId));
        }
    }

    /**
     * This test immediately deletes created options and verifies by
     * calling describe options that deleted options are really gone
     */
    @Test
    public void testDeleteOptions() {

        ec2.deleteDhcpOptions(new DeleteDhcpOptionsRequest(dhcpOptionsId));

        List<DhcpOptions> dhcpOptionsList = ec2.describeDhcpOptions()
                .getDhcpOptions();

        assertFalse(findDhcpOptoin(dhcpOptionsList,
                DOMAIN_NAME_KEY, domainNameValue));

        dhcpOptionsId = null; // so that tearDown won't delete it again
    }

    private static CreateDhcpOptionsResult createDhcpOptions(
            String optionKey, String... optionValue) {

        DhcpConfiguration configurationOne = new DhcpConfiguration()
            .withKey(optionKey).withValues(optionValue);

        CreateDhcpOptionsRequest request = new CreateDhcpOptionsRequest()
            .withDhcpConfigurations(configurationOne);

        return ec2.createDhcpOptions(request);
    }

    private static boolean findDhcpOptoin(
            Collection<DhcpOptions> optionsList, String opKey, String opValue) {
        for (DhcpOptions options : optionsList) {
            if (findDhcpOption(options, opKey, opValue)) {
                return true;
            }
        }
        return false;
    }

    private static boolean findDhcpOption(
            DhcpOptions options, String opKey, String opValue) {
        for (DhcpConfiguration config : options.getDhcpConfigurations()) {
            if (opKey.equals(config.getKey()) &&
                opValue.equals(config.getValues().get(0))) {
                return true;
            }
        }
        return false;
    }

}
