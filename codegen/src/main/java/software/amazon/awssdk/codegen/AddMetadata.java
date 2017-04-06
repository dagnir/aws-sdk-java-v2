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

package software.amazon.awssdk.codegen;

import software.amazon.awssdk.codegen.internal.Utils;
import software.amazon.awssdk.codegen.model.config.BasicCodeGenConfig;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.model.intermediate.Metadata;
import software.amazon.awssdk.codegen.model.intermediate.Protocol;
import software.amazon.awssdk.codegen.model.service.Operation;
import software.amazon.awssdk.codegen.model.service.ServiceMetadata;
import software.amazon.awssdk.codegen.model.service.ServiceModel;
import software.amazon.awssdk.util.StringUtils;
import software.amazon.awssdk.util.json.Jackson;

/**
 * Constructs the metadata that is required for generating the java client from the service meta data.
 */
final class AddMetadata {

    private static final String AWS_PACKAGE_PREFIX = "software.amazon.awssdk.services.";

    public static Metadata constructMetadata(ServiceModel serviceModel,
                                             BasicCodeGenConfig codeGenConfig,
                                             CustomizationConfig customizationConfig) {

        final Metadata metadata = new Metadata();

        final ServiceMetadata serviceMetadata = serviceModel.getMetadata();

        final String serviceName;
        final String packageName;

        // API Gateway uses additional codegen.config settings
        if (serviceMetadata.getProtocol().equals(Protocol.API_GATEWAY.getValue())) {
            serviceName = codeGenConfig.getInterfaceName();
            packageName = codeGenConfig.getPackageName();

            metadata.withDefaultEndpoint(codeGenConfig.getEndpoint())
                    .withDefaultEndpointWithoutHttpProtocol(
                            Utils.getDefaultEndpointWithoutHttpProtocol(codeGenConfig.getEndpoint()))
                    .withDefaultRegion(codeGenConfig.getDefaultRegion());
        } else {
            serviceName = Utils.getServiceName(serviceMetadata);
            packageName = AWS_PACKAGE_PREFIX + Utils.getPackageName(serviceName, customizationConfig);
        }

        final String syncInterfaceName = Utils.getInterfaceName(serviceName);
        final String asyncInterfaceName = Utils.getAsyncInterfaceName(serviceName);

        metadata.withApiVersion(serviceMetadata.getApiVersion())
                .withAsyncClient(Utils.getClientName(asyncInterfaceName))
                .withAsyncInterface(asyncInterfaceName)
                .withDocumentation(serviceModel.getDocumentation())
                .withPackageName(packageName)
                .withPackagePath(packageName.replace(".", "/"))
                .withServiceAbbreviation(serviceMetadata.getServiceAbbreviation())
                .withServiceFullName(serviceMetadata.getServiceFullName())
                .withSyncClient(Utils.getClientName(syncInterfaceName))
                .withSyncInterface(syncInterfaceName)
                .withProtocol(Protocol.fromValue(serviceMetadata.getProtocol()))
                .withJsonVersion(serviceMetadata.getJsonVersion())
                .withEndpointPrefix(serviceMetadata.getEndpointPrefix())
                .withSigningName(serviceMetadata.getSigningName())
                .withRequiresApiKey(requiresApiKey(serviceModel))
                .withUid(serviceMetadata.getUid());

        final String jsonVersion = getJsonVersion(metadata, serviceMetadata);
        metadata.setJsonVersion(jsonVersion);

        // TODO: iterate through all the operations and check whether any of
        // them accept stream input
        metadata.setHasApiWithStreamInput(false);
        return metadata;
    }

    private static String getJsonVersion(Metadata metadata, ServiceMetadata serviceMetadata) {
        // TODO this should be defaulted in the C2J build tool
        if (serviceMetadata.getJsonVersion() == null && metadata.isJsonProtocol()) {
            return "1.1";
        } else {
            return serviceMetadata.getJsonVersion();
        }
    }

    /**
     * If any operation requires an API key we generate a setter on the builder.
     *
     * @return True if any operation requires an API key. False otherwise.
     */
    private static boolean requiresApiKey(ServiceModel serviceModel) {
        return serviceModel.getOperations().values().stream()
                           .anyMatch(Operation::requiresApiKey);
    }
}
