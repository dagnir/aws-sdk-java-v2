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

package software.amazon.awssdk.services.ec2.transform;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.apache.commons.codec.Charsets;
import software.amazon.awssdk.AmazonWebServiceRequest;
import software.amazon.awssdk.Request;
import software.amazon.awssdk.handlers.AbstractRequestHandler;
import software.amazon.awssdk.metrics.spi.TimingInfo;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesResult;
import software.amazon.awssdk.services.ec2.model.DescribeSpotInstanceRequestsResult;
import software.amazon.awssdk.services.ec2.model.GroupIdentifier;
import software.amazon.awssdk.services.ec2.model.ImportKeyPairRequest;
import software.amazon.awssdk.services.ec2.model.LaunchSpecification;
import software.amazon.awssdk.services.ec2.model.ModifyReservedInstancesRequest;
import software.amazon.awssdk.services.ec2.model.RequestSpotInstancesRequest;
import software.amazon.awssdk.services.ec2.model.RequestSpotInstancesResult;
import software.amazon.awssdk.services.ec2.model.Reservation;
import software.amazon.awssdk.services.ec2.model.RunInstancesRequest;
import software.amazon.awssdk.services.ec2.model.RunInstancesResult;
import software.amazon.awssdk.services.ec2.model.SpotInstanceRequest;
import software.amazon.awssdk.utils.Base64Utils;

public class EC2RequestHandler extends AbstractRequestHandler {
    @Override
    public void beforeRequest(Request<?> request) {
        AmazonWebServiceRequest originalRequest = request.getOriginalRequest();
        if (originalRequest instanceof ImportKeyPairRequest) {
            ImportKeyPairRequest importKeyPairRequest = (ImportKeyPairRequest) originalRequest;
            String publicKeyMaterial = importKeyPairRequest.getPublicKeyMaterial();
            String encodedKeyMaterial = Base64Utils.encodeAsString(publicKeyMaterial.getBytes(Charsets.UTF_8));
            request.addParameter("PublicKeyMaterial", encodedKeyMaterial);
        } else if (originalRequest instanceof RequestSpotInstancesRequest) {
            // Request -> Query string marshalling for RequestSpotInstancesRequest is a little tricky since
            // the query string params follow a different form than the XML responses, so we manually set the parameters here.
            RequestSpotInstancesRequest requestSpotInstancesRequest = (RequestSpotInstancesRequest) originalRequest;

            // Marshall the security groups specified only by name
            int groupNameCount = 1;
            for (String groupName : requestSpotInstancesRequest.getLaunchSpecification().getSecurityGroups()) {
                request.addParameter("LaunchSpecification.SecurityGroup." + groupNameCount++, groupName);
            }

            // Then loop through the GroupIdentifier objects and marshall any specified IDs
            // and any additional group names
            int groupIdCount = 1;
            for (GroupIdentifier group : requestSpotInstancesRequest.getLaunchSpecification().getAllSecurityGroups()) {
                if (group.getGroupId() != null) {
                    request.addParameter("LaunchSpecification.SecurityGroupId." + groupIdCount++, group.getGroupId());
                }

                if (group.getGroupName() != null) {
                    request.addParameter("LaunchSpecification.SecurityGroup." + groupNameCount++, group.getGroupName());
                }
            }

            // Remove any of the incorrect parameters.
            List<String> keysToRemove = new ArrayList<String>();
            for (String parameter : request.getParameters().keySet()) {
                if (parameter.startsWith("LaunchSpecification.GroupSet.")) {
                    keysToRemove.add(parameter);
                }
            }
            for (String key : keysToRemove) {
                request.getParameters().remove(key);
            }
        } else if (originalRequest instanceof RunInstancesRequest) {
            // If a RunInstancesRequest doesn't specify a ClientToken, fill one in, otherwise
            // retries could result in unwanted instances being launched in the customer's account.
            RunInstancesRequest runInstancesRequest = (RunInstancesRequest) originalRequest;
            if (runInstancesRequest.getClientToken() == null) {
                request.addParameter("ClientToken", UUID.randomUUID().toString());
            }
        } else if (originalRequest instanceof ModifyReservedInstancesRequest) {
            // If a ModifyReservedInstancesRequest doesn't specify a ClientToken, fill one in, otherwise
            // retries could result in duplicate requests.
            ModifyReservedInstancesRequest modifyReservedInstancesRequest = (ModifyReservedInstancesRequest) originalRequest;
            if (modifyReservedInstancesRequest.getClientToken() == null) {
                request.addParameter("ClientToken", UUID.randomUUID().toString());
            }
        }
    }

    @Override
    public void afterResponse(Request<?> request, Object response, TimingInfo timingInfo) {
        /*
         * For backwards compatibility, we preserve the existing List<String> of
         * security group names by explicitly populating it from the full list
         * of security group info.
         */
        if (response instanceof DescribeSpotInstanceRequestsResult) {
            DescribeSpotInstanceRequestsResult result = (DescribeSpotInstanceRequestsResult) response;
            for (SpotInstanceRequest spotInstanceRequest : result.getSpotInstanceRequests()) {
                LaunchSpecification launchSpecification = spotInstanceRequest.getLaunchSpecification();
                populateLaunchSpecificationSecurityGroupNames(launchSpecification);
            }
        } else if (response instanceof RequestSpotInstancesResult) {
            RequestSpotInstancesResult result = (RequestSpotInstancesResult) response;
            for (SpotInstanceRequest spotInstanceRequest : result.getSpotInstanceRequests()) {
                LaunchSpecification launchSpecification = spotInstanceRequest.getLaunchSpecification();
                populateLaunchSpecificationSecurityGroupNames(launchSpecification);
            }
        } else if (response instanceof DescribeInstancesResult) {
            DescribeInstancesResult result = (DescribeInstancesResult) response;
            for (Reservation reservation : result.getReservations()) {
                populateReservationSecurityGroupNames(reservation);
            }
        } else if (response instanceof RunInstancesResult) {
            RunInstancesResult result = (RunInstancesResult) response;
            populateReservationSecurityGroupNames(result.getReservation());
        }
    }

    private void populateReservationSecurityGroupNames(Reservation reservation) {
        List<String> groupNames = new ArrayList<String>();
        for (GroupIdentifier group : reservation.getGroups()) {
            groupNames.add(group.getGroupName());
        }
        reservation.setGroupNames(groupNames);
    }

    private void populateLaunchSpecificationSecurityGroupNames(LaunchSpecification launchSpecification) {
        List<String> groupNames = new ArrayList<String>();
        for (GroupIdentifier group : launchSpecification.getAllSecurityGroups()) {
            groupNames.add(group.getGroupName());
        }
        launchSpecification.setSecurityGroups(groupNames);
    }
}
