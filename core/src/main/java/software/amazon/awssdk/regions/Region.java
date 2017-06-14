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

package software.amazon.awssdk.regions;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import software.amazon.awssdk.utils.AbstractEnum;

/**
 * Class holding all public AWS regions at the time each version of the SDK is released.
 * {@link Region#of(String)} can be used to allow the SDK to be forward compatible with future
 * regions that were not available at the time this version of the SDK was released.
 * <p>
 * The {@link Region#value()} will be used as the signing region for all requests to AWS
 * services unless an explicit region override is available in {@link RegionMetadata}. This value
 * will also be used to construct the endpoint for accessing a service unless an explicit endpoint
 * is available for that region in {@link RegionMetadata}.
 * <p>
 * Additional metadata about a region can be discovered using {@link RegionMetadata#of(Region)}
 */
public class Region extends AbstractEnum {

    /**
     * AWS Partition Regions
     */
    public static final Region AP_NORTHEAST_1 = Region.of("ap-northeast-1");
    public static final Region AP_NORTHEAST_2 = Region.of("ap-northeast-2");
    public static final Region AP_SOUTH_1 = Region.of("ap-south-1");
    public static final Region AP_SOUTHEAST_1 = Region.of("ap-southeast-1");
    public static final Region AP_SOUTHEAST_2 = Region.of("ap-southeast-2");

    public static final Region CA_CENTRAL_1 = Region.of("ca-central-1");

    public static final Region EU_CENTRAL_1 = Region.of("eu-central-1");
    public static final Region EU_WEST_1 = Region.of("eu-west-1");
    public static final Region EU_WEST_2 = Region.of("eu-west-2");

    public static final Region SA_EAST_1 = Region.of("sa-east-1");

    public static final Region US_EAST_1 = Region.of("us-east-1");
    public static final Region US_EAST_2 = Region.of("us-east-2");
    public static final Region US_WEST_1 = Region.of("us-west-1");
    public static final Region US_WEST_2 = Region.of("us-west-2");

    public static final Region AWS_GLOBAL = Region.of("aws-global");

    /**
     * AWS CN Partition Regions
     */
    public static final Region CN_NORTH_1 = Region.of("cn-north-1");
    public static final Region CN_NORTHWEST_1 = Region.of("cn-northwest-1");
    public static final Region AWS_CN_GLOBAL = Region.of("aws-cn-global");

    /**
     * AWS Gov Cloud Partition Regions
     */
    public static final class GovCloud {
        public static final Region US_GOV_WEST_1 = Region.of("us-gov-west-1");
        public static final Region AWS_US_GOV_GLOBAL = Region.of("aws-us-gov-global");

        public static final List<Region> REGIONS = Collections.unmodifiableList(Arrays.asList(
                US_GOV_WEST_1,
                AWS_US_GOV_GLOBAL
        ));

        public static List<Region> getRegions() {
            return REGIONS;
        }
    }

    public static final List<Region> REGIONS = Collections.unmodifiableList(Arrays.asList(
            AP_NORTHEAST_1,
            AP_NORTHEAST_2,
            AP_SOUTH_1,
            AP_SOUTHEAST_1,
            AP_SOUTHEAST_2,
            CA_CENTRAL_1,
            EU_CENTRAL_1,
            EU_WEST_1,
            EU_WEST_2,
            SA_EAST_1,
            US_EAST_1,
            US_EAST_2,
            US_WEST_1,
            US_WEST_2,
            AWS_GLOBAL,
            CN_NORTH_1,
            CN_NORTHWEST_1,
            AWS_CN_GLOBAL,
            GovCloud.US_GOV_WEST_1,
            GovCloud.AWS_US_GOV_GLOBAL));

    private Region(String value) {
        super(value);
    }

    public static Region of(String value) {
        return AbstractEnum.value(value, Region.class, Region::new);
    }

    public static List<Region> getRegions() {
        return REGIONS;
    }
}
