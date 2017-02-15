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

import software.amazon.awssdk.internal.partitions.PartitionsLoader;

/**
 * A factory to create {@link RegionMetadata}
 */
public class RegionMetadataFactory {

    private RegionMetadataFactory() {
    }

    public static RegionMetadata create() {
        RegionMetadata metadata = createLegacyXmlRegionMetadata();
        if (metadata == null) {
            metadata = new RegionMetadata(new PartitionsLoader().build());
        }
        return metadata;
    }

    private static RegionMetadata createLegacyXmlRegionMetadata() {
        return new LegacyRegionXmlMetadataBuilder().build();
    }
}
