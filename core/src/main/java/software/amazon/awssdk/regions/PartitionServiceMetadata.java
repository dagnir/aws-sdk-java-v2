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

import static java.util.stream.Collectors.toList;

import java.net.URI;
import java.util.List;
import java.util.Map;

import software.amazon.awssdk.internal.region.model.Endpoint;
import software.amazon.awssdk.internal.region.model.Partition;
import software.amazon.awssdk.internal.region.model.Service;

public class PartitionServiceMetadata implements ServiceMetadata {

    private static final String SERVICE = "{service}";
    private static final String REGION = "{region}";
    private static final String DNS_SUFFIX = "{dnsSuffix}";

    private final String service;
    private final Map<String, Partition> servicePartitionData;

    public PartitionServiceMetadata(String service,
                                    Map<String, Partition> servicePartitionData) {
        this.service = service;
        this.servicePartitionData = servicePartitionData;
    }

    @Override
    public URI endpointFor(Region region) {
        RegionMetadata regionMetadata = RegionMetadata.of(region);
        Partition partitionData = servicePartitionData.get(regionMetadata.getPartition());
        Service serviceData = partitionData.getServices().get(service);

        // Return endpoint from partition heuristics if endpoint data is unavailable
        if (serviceData == null || serviceData.getEndpoints().get(region.value()) == null) {
            return endpointFromPartitionRegex(partitionData, partitionData.getDefaults().getHostname(), regionMetadata);
        }

        // Check if there is a hostname for this service in the given region
        Endpoint endpointData = serviceData.getEndpoints().get(region.value());
        if (endpointData.getHostname() != null) {
            return URI.create(endpointData.getHostname());
        }

        // Check if there is a default hostname for this service in this partition
        if (serviceData.getDefaults() != null && serviceData.getDefaults().getHostname() != null) {
            endpointData = serviceData.getDefaults();
            return endpointFromPartitionRegex(partitionData, endpointData.getHostname(), regionMetadata);
        }

        return endpointFromPartitionRegex(partitionData, partitionData.getDefaults().getHostname(), regionMetadata);
    }

    @Override
    public List<Region> regions() {
        return servicePartitionData.values().stream()
                .filter(s -> s.getServices().containsKey(service))
                .flatMap(s -> s.getRegions().keySet()
                                            .stream()
                                            .map(Region::of))
                .collect(toList());
    }

    private URI endpointFromPartitionRegex(Partition partitionData, String hostName, RegionMetadata region) {
        return URI.create(hostName
                .replace(SERVICE, service)
                .replace(REGION, region.getName())
                .replace(DNS_SUFFIX, partitionData.getDnsSuffix()));
    }
}
