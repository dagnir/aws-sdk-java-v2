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

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.util.List;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.services.ec2.model.DescribeImagesRequest;
import software.amazon.awssdk.services.ec2.model.Filter;
import software.amazon.awssdk.services.ec2.model.Image;
import software.amazon.awssdk.services.ec2.model.ProductCode;

public class EC2ProductCodesIntegrationTest extends EC2IntegrationTestBase {

    private static String DEVPAY_AMI;
    private static String MARKETPLACE_AMI;

    @BeforeClass
    public static void setup() {
        DEVPAY_AMI = findPublicAmiWithProductCodeType("devpay");
        MARKETPLACE_AMI = findPublicAmiWithProductCodeType("marketplace");
    }

    private static String findPublicAmiWithProductCodeType(String type) {

        List<Image> images = ec2.describeImages(DescribeImagesRequest.builder()
                                                                     .filters(Filter.builder()
                                                                                    .name("is-public")
                                                                                    .values("true").build(),
                                                                              Filter.builder()
                                                                                    .name("product-code.type")
                                                                                    .values(type).build()).build()
                                               ).images();

        assertThat("Cannot find a public AMI with product-code.type=" + type,
                   images, not(empty()));

        return images.get(0).imageId();
    }

    @Test
    public void testDevpayImage() {
        List<Image> images = ec2.describeImages(DescribeImagesRequest.builder()
                                                                     .imageIds(DEVPAY_AMI).build()
                                               ).images();
        assertEquals(1, images.size());

        List<ProductCode> codes = images.get(0).productCodes();
        assertEquals(1, codes.size());

        ProductCode devpayCode = codes.get(0);
        assertEquals("devpay", devpayCode.productCodeType());
        assertStringNotEmpty(devpayCode.productCodeId());
    }

    @Test
    public void testDescribeMarketplaceImage() {
        List<Image> images = ec2.describeImages(DescribeImagesRequest.builder()
                                                                     .imageIds(MARKETPLACE_AMI).build()
                                               ).images();
        assertEquals(1, images.size());

        List<ProductCode> codes = images.get(0).productCodes();
        assertEquals(1, codes.size());

        ProductCode marketPlaceCode = codes.get(0);
        assertEquals("marketplace", marketPlaceCode.productCodeType());
        assertStringNotEmpty(marketPlaceCode.productCodeId());
    }
}
