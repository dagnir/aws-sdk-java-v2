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

package software.amazon.awssdk.codegen.emitters.tasks;

import static software.amazon.awssdk.codegen.internal.DocumentationUtils.createLinkToServiceDocumentation;
import static software.amazon.awssdk.codegen.utils.FunctionalUtils.safeFunction;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import software.amazon.awssdk.codegen.emitters.FreemarkerGeneratorTask;
import software.amazon.awssdk.codegen.emitters.GeneratorTask;
import software.amazon.awssdk.codegen.emitters.GeneratorTaskParams;
import software.amazon.awssdk.codegen.emitters.PoetGeneratorTask;
import software.amazon.awssdk.codegen.model.intermediate.Metadata;
import software.amazon.awssdk.codegen.model.intermediate.Protocol;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeType;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.common.EnumClass;
import software.amazon.awssdk.util.ImmutableMapParameter;

class ModelClassGeneratorTasks extends BaseGeneratorTasks {

    private final String modelClassDir;
    private final String basePackageDir;

    ModelClassGeneratorTasks(GeneratorTaskParams dependencies) {
        super(dependencies);
        this.modelClassDir = dependencies.getPathProvider().getModelDirectory();
        this.basePackageDir = dependencies.getPathProvider().getBasePackageDirectory();
    }

    @Override
    protected List<GeneratorTask> createTasks() throws Exception {
        info("Emitting model classes");
        return model.getShapes().entrySet().stream()
                    .filter(e -> shouldGenerateShape(e.getValue()))
                    .map(safeFunction(e -> createTask(e.getKey(), e.getValue())))
                    .collect(Collectors.toList());
    }

    private boolean shouldGenerateShape(ShapeModel shapeModel) {
        if (shapeModel.getCustomization().isSkipGeneratingModelClass()) {
            System.out.println("Skip generating class " + shapeModel.getShapeName());
            return false;
        }
        return true;
    }

    private GeneratorTask createTask(String javaShapeName, ShapeModel shapeModel) throws IOException {
        Metadata metadata = model.getMetadata();

        if (shapeModel.getShapeType() != ShapeType.Enum) {
            Map<String, Object> dataModel = ImmutableMapParameter.<String, Object>builder()
                .put("fileHeader", model.getFileHeader())
                .put("shape", shapeModel)
                .put("metadata", metadata)
                .put("baseClassFqcn", getModelBaseClassFqcn(shapeModel.getShapeType()))
                .put("customConfig", model.getCustomizationConfig())
                .put("shouldGenerateSdkRequestConfigSetter", shouldGenerateSdkRequestConfigSetter(shapeModel))
                .put("awsDocsUrl", createLinkToServiceDocumentation(metadata, shapeModel))
                .put("shouldEmitStructuredPojoInterface", model.getMetadata().isJsonProtocol()
                                                          && shapeModel.getShapeType() == ShapeType.Model)
                .put("transformPackage", model.getTransformPackage())
                .build();

            // Submit task for generating the
            // model/request/response/enum/exception class.
            return new FreemarkerGeneratorTask(modelClassDir,
                                               javaShapeName,
                                               freemarker.getShapeTemplate(shapeModel),
                                               dataModel);
        } else {
            ClassSpec enumClass = new EnumClass(metadata.getPackageName(), shapeModel);
            return new PoetGeneratorTask(basePackageDir, model.getFileHeader(), enumClass);
        }
    }

    /**
     * For API Gateway request classes we override the sdkRequestConfig fluent setter to return the correct concrete request type
     * for better method chaining.
     *
     * @return True if sdkRequestConfig should be overriden by template. False if not.
     */
    private boolean shouldGenerateSdkRequestConfigSetter(ShapeModel shape) {
        return model.getMetadata().getProtocol() == Protocol.API_GATEWAY && shape.getShapeType() == ShapeType.Request;
    }

    /**
     * @param shapeType Shape type to get base class for.
     *
     * @return Correct base type for the type of model. May depend on protocol and customizations. Null if model has no base type.
     */
    private String getModelBaseClassFqcn(ShapeType shapeType) {
        switch (shapeType) {
            case Exception:
                return model.getSdkModeledExceptionBaseFqcn();
            case Response:
                return model.getSdkBaseResponseFqcn();
            case Request:
                return model.getMetadata().getRequestBaseFqcn();
            default:
                return null;
        }
    }
}
