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

import software.amazon.awssdk.codegen.internal.Constants;
import software.amazon.awssdk.codegen.internal.Utils;
import software.amazon.awssdk.codegen.model.config.BasicCodeGenConfig;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.model.intermediate.Metadata;
import software.amazon.awssdk.codegen.model.intermediate.Protocol;
import software.amazon.awssdk.codegen.model.service.AuthType;
import software.amazon.awssdk.codegen.model.service.Operation;
import software.amazon.awssdk.codegen.model.service.ServiceMetadata;
import software.amazon.awssdk.codegen.model.service.ServiceModel;

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

        metadata.withApiVersion(serviceMetadata.getApiVersion())
                .withAsyncClient(String.format(Constants.ASYNC_CLIENT_CLASS_NAME_PATTERN, serviceName))
                .withAsyncInterface(String.format(Constants.ASYNC_CLIENT_INTERFACE_NAME_PATTERN, serviceName))
                .withAsyncBuilder(String.format(Constants.ASYNC_BUILDER_CLASS_NAME_PATTERN, serviceName))
                .withAsyncBuilderInterface(String.format(Constants.ASYNC_BUILDER_INTERFACE_NAME_PATTERN, serviceName))
                .withBaseBuilderInterface(String.format(Constants.BASE_BUILDER_INTERFACE_NAME_PATTERN, serviceName))
                .withBaseBuilder(String.format(Constants.BASE_BUILDER_CLASS_NAME_PATTERN, serviceName))
                .withDocumentation(serviceModel.getDocumentation())
                .withPackageName(packageName)
                .withPackagePath(packageName.replace(".", "/"))
                .withServiceAbbreviation(serviceMetadata.getServiceAbbreviation())
                .withServiceFullName(serviceMetadata.getServiceFullName())
                .withSyncClient(String.format(Constants.SYNC_CLIENT_CLASS_NAME_PATTERN, serviceName))
                .withSyncInterface(String.format(Constants.SYNC_CLIENT_INTERFACE_NAME_PATTERN, serviceName))
                .withSyncBuilder(String.format(Constants.SYNC_BUILDER_CLASS_NAME_PATTERN, serviceName))
                .withSyncBuilderInterface(String.format(Constants.SYNC_BUILDER_INTERFACE_NAME_PATTERN, serviceName))
                .withBaseExceptionName(String.format(Constants.BASE_EXCEPTION_NAME_PATTERN, serviceName))
                .withProtocol(Protocol.fromValue(serviceMetadata.getProtocol()))
                .withJsonVersion(serviceMetadata.getJsonVersion())
                .withEndpointPrefix(serviceMetadata.getEndpointPrefix())
                .withSigningName(serviceMetadata.getSigningName())
                .withAuthType(AuthType.fromValue(serviceMetadata.getSignatureVersion()))
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
