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
import static software.amazon.awssdk.utils.FunctionalUtils.safeFunction;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
import software.amazon.awssdk.codegen.poet.model.AwsServiceModel;
import software.amazon.awssdk.codegen.poet.model.ServiceModelCopierSpecs;
import software.amazon.awssdk.util.ImmutableMapParameter;

class ModelClassGeneratorTasks extends BaseGeneratorTasks {

    private final String modelClassDir;

    ModelClassGeneratorTasks(GeneratorTaskParams dependencies) {
        super(dependencies);
        this.modelClassDir = dependencies.getPathProvider().getModelDirectory();
    }

    @Override
    protected List<GeneratorTask> createTasks() throws Exception {
        info("Emitting model classes");
        List<GeneratorTask> tasks = new ArrayList<>();

        model.getShapes().entrySet().stream()
                    .filter(e -> shouldGenerateShape(e.getValue()))
                    .map(safeFunction(e -> createTask(e.getKey(), e.getValue())))
                    .forEach(tasks::add);

        if (model.getMetadata().isJsonProtocol()) {
            new ServiceModelCopierSpecs(model).copierSpecs().stream()
                    .map(safeFunction(spec -> new PoetGeneratorTask(modelClassDir, model.getFileHeader(), spec)))
                    .forEach(tasks::add);
        }

        return tasks;
    }

    private boolean shouldGenerateShape(ShapeModel shapeModel) {
        if (shapeModel.getCustomization().isSkipGeneratingModelClass()) {
            info("Skip generating class " + shapeModel.getShapeName());
            return false;
        }
        return true;
    }

    private GeneratorTask createTask(String javaShapeName, ShapeModel shapeModel) throws IOException {
        Metadata metadata = model.getMetadata();

        if (shapeModel.getShapeType() != ShapeType.Enum) {
            // TODO: remove this check when all (un)marshallers are moved over to new model style
            if (metadata.isJsonProtocol()) {
                ClassSpec modelSpec = new AwsServiceModel(model, shapeModel);
                return new PoetGeneratorTask(modelClassDir, model.getFileHeader(), modelSpec);
            }

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
                .put("transformPackage", model.getMetadata().getFullTransformPackageName())
                .build();

            // Submit task for generating the
            // model/request/response/enum/exception class.
            return new FreemarkerGeneratorTask(modelClassDir,
                                               javaShapeName,
                                               freemarker.getShapeTemplate(shapeModel),
                                               dataModel);
        } else {
            ClassSpec enumClass = new EnumClass(metadata.getFullModelPackageName(), shapeModel);
            return new PoetGeneratorTask(modelClassDir, model.getFileHeader(), enumClass);
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
