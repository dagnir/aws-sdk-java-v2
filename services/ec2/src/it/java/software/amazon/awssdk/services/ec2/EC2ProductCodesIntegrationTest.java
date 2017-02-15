/*
 * Copyright 2015-2017 Amazon Technologies, Inc.
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

        List<Image> images = ec2.describeImages(new DescribeImagesRequest()
                                                        .withFilters(
                                                                new Filter()
                                                                        .withName("is-public")
                                                                        .withValues("true"),
                                                                new Filter()
                                                                        .withName("product-code.type")
                                                                        .withValues(type))
                                               ).getImages();

        assertThat("Cannot find a public AMI with product-code.type=" + type,
                   images, not(empty()));

        return images.get(0).getImageId();
    }

    @Test
    public void testDevpayImage() {
        List<Image> images = ec2.describeImages(new DescribeImagesRequest()
                                                        .withImageIds(DEVPAY_AMI)
                                               ).getImages();
        assertEquals(1, images.size());

        List<ProductCode> codes = images.get(0).getProductCodes();
        assertEquals(1, codes.size());

        ProductCode devpayCode = codes.get(0);
        assertEquals("devpay", devpayCode.getProductCodeType());
        assertStringNotEmpty(devpayCode.getProductCodeId());
    }

    @Test
    public void testDescribeMarketplaceImage() {
        List<Image> images = ec2.describeImages(new DescribeImagesRequest()
                                                        .withImageIds(MARKETPLACE_AMI)
                                               ).getImages();
        assertEquals(1, images.size());

        List<ProductCode> codes = images.get(0).getProductCodes();
        assertEquals(1, codes.size());

        ProductCode marketPlaceCode = codes.get(0);
        assertEquals("marketplace", marketPlaceCode.getProductCodeType());
        assertStringNotEmpty(marketPlaceCode.getProductCodeId());
    }
}
