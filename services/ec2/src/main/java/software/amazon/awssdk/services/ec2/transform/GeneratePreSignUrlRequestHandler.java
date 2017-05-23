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

import java.net.URI;
import java.net.URISyntaxException;
import software.amazon.awssdk.AmazonClientException;
import software.amazon.awssdk.AmazonWebServiceRequest;
import software.amazon.awssdk.Protocol;
import software.amazon.awssdk.Request;
import software.amazon.awssdk.auth.Aws4Signer;
import software.amazon.awssdk.handlers.HandlerContextKey;
import software.amazon.awssdk.handlers.RequestHandler2;
import software.amazon.awssdk.http.HttpMethodName;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.RegionUtils;
import software.amazon.awssdk.services.ec2.model.CopySnapshotRequest;
import software.amazon.awssdk.util.AwsHostNameUtils;
import software.amazon.awssdk.util.ImmutableObjectUtils;
import software.amazon.awssdk.util.SdkHttpUtils;
import software.amazon.awssdk.util.StringUtils;

/**
 * RequestHandler that generates a pre-signed URL for copying encrypted
 * snapshots
 */
public class GeneratePreSignUrlRequestHandler extends RequestHandler2 {

    @Override
    public void beforeRequest(Request<?> request) {

        AmazonWebServiceRequest originalRequest = request.getOriginalRequest();

        if (originalRequest instanceof CopySnapshotRequest) {

            CopySnapshotRequest originalCopySnapshotRequest = (CopySnapshotRequest) originalRequest;

            // Return if presigned url is already specified by the user.
            if (originalCopySnapshotRequest.presignedUrl() != null) {
                return;
            }

            String serviceName = "ec2";

            // The source regions where the snapshot currently resides.
            String sourceRegion = originalCopySnapshotRequest.sourceRegion();
            String sourceSnapshotId = originalCopySnapshotRequest
                    .sourceSnapshotId();

            /*
             * The region where the snapshot has to be copied from the source.
             * The original copy snap shot request will have the end point set
             * as the destination region in the client before calling this
             * request.
             */

            URI endPointDestination = request.getEndpoint();
            String destinationRegion = originalCopySnapshotRequest
                                               .destinationRegion() != null ? originalCopySnapshotRequest
                                               .destinationRegion() : AwsHostNameUtils
                                               .parseRegionName(endPointDestination.getHost(), serviceName);

            URI endPointSource = createEndpoint(sourceRegion, serviceName);

            Request<CopySnapshotRequest> requestForPresigning = generateRequestForPresigning(
                    sourceSnapshotId, sourceRegion, destinationRegion);

            requestForPresigning.setEndpoint(endPointSource);
            requestForPresigning.setHttpMethod(HttpMethodName.GET);

            Aws4Signer signer = new Aws4Signer();
            signer.setServiceName(serviceName);

            signer.presignRequest(requestForPresigning, request.getHandlerContext(HandlerContextKey.AWS_CREDENTIALS), null);

            ImmutableObjectUtils.setObjectMember(originalCopySnapshotRequest, "presignedUrl", generateUrl(requestForPresigning));
            ImmutableObjectUtils.setObjectMember(originalCopySnapshotRequest, "destinationRegion", destinationRegion);

            request.addParameter("DestinationRegion", StringUtils
                    .fromString(originalCopySnapshotRequest
                                        .destinationRegion()));
            request.addParameter("PresignedUrl", StringUtils
                    .fromString(originalCopySnapshotRequest.presignedUrl()));


        }

    }

    /**
     * Generates a Request object for the pre-signed URL.
     */
    private Request<CopySnapshotRequest> generateRequestForPresigning(
            String sourceSnapshotId, String sourceRegion,
            String destinationRegion) {

        CopySnapshotRequest copySnapshotRequest = CopySnapshotRequest.builder()
                .sourceSnapshotId(sourceSnapshotId)
                .sourceRegion(sourceRegion)
                .destinationRegion(destinationRegion)
                .build();

        return new CopySnapshotRequestMarshaller()
                .marshall(copySnapshotRequest);

    }

    private String generateUrl(Request<?> request) {

        URI endpoint = request.getEndpoint();
        String uri = SdkHttpUtils.appendUri(endpoint.toString(),
                                            request.getResourcePath(), true);
        String encodedParams = SdkHttpUtils.encodeParameters(request);

        if (encodedParams != null) {
            uri += "?" + encodedParams;
        }

        return uri;

    }

    private URI createEndpoint(String regionName, String serviceName) {

        final Region region = RegionUtils.getRegion(regionName);

        if (region == null) {
            throw new AmazonClientException("{" + serviceName + ", " + regionName + "} was not "
                                            + "found in region metadata. Update to latest version of SDK and try again.");
        }

        return toUri(region.getServiceEndpoint(serviceName));
    }

    /** Returns the endpoint as a URI. */
    private URI toUri(String endpoint) throws IllegalArgumentException {

        if (endpoint.contains("://") == false) {
            endpoint = Protocol.HTTPS + "://" + endpoint;
        }

        try {
            return new URI(endpoint);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
