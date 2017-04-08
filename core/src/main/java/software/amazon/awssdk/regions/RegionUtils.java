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

import java.util.List;

/**
 * Utilities for working with regions.
 */
public class RegionUtils {

    private static volatile RegionMetadata regionMetadata;

    /*
     * Convenience methods that access the singleton RegionMetadata instance
     * maintained by this class, lazily initializing it if need be.
     */

    /**
     * Returns the current set of region metadata for this process,
     * initializing it if it has not yet been explicitly initialized before.
     *
     * @return the current set of region metadata
     */
    public static RegionMetadata getRegionMetadata() {
        RegionMetadata rval = regionMetadata;
        if (rval != null) {
            return rval;
        }

        synchronized (RegionUtils.class) {
            if (regionMetadata == null) {
                initialize();
            }
        }

        return regionMetadata;
    }

    /**
     * Initializes the region metadata by loading from the default hierarchy
     * of region metadata locations.
     */
    public static void initialize() {
        regionMetadata = RegionMetadataFactory.create();
    }

    /**
     * Directly sets the singleton {@code RegionMetadata} instance.
     *
     * @param metadata the new region metadata object
     */
    public static void initializeWithMetadata(final RegionMetadata metadata) {
        if (metadata == null) {
            throw new IllegalArgumentException("metadata cannot be null");
        }
        regionMetadata = metadata;
    }

    /**
     * Returns a list of the available AWS regions.
     */
    public static List<Region> getRegions() {
        return getRegionMetadata().getRegions();
    }

    /**
     * Returns a list of the regions that support the service given.
     *
     * @param serviceAbbreviation
     *         The service endpoint prefix which can be retrieved from the
     *         constant ENDPOINT_PREFIX of the specific service client interface.
     *
     */
    public static List<Region> getRegionsForService(
            String serviceAbbreviation) {

        return getRegionMetadata().getRegionsForService(serviceAbbreviation);
    }

    /**
     * Returns the region with the id given, if it exists. Otherwise, returns
     * null.
     */
    public static Region getRegion(String regionName) {
        return getRegionMetadata().getRegion(regionName);
    }

}
