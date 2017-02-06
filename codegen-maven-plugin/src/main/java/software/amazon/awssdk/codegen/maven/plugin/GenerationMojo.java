/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights
 * Reserved.
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
package software.amazon.awssdk.codegen.maven.plugin;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import software.amazon.awssdk.codegen.C2jModels;
import software.amazon.awssdk.codegen.CodeGenerator;
import software.amazon.awssdk.codegen.internal.Utils;
import software.amazon.awssdk.codegen.model.config.BasicCodeGenConfig;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.model.intermediate.ServiceExamples;
import software.amazon.awssdk.codegen.model.service.ServiceModel;
import software.amazon.awssdk.codegen.model.service.Waiters;
import software.amazon.awssdk.codegen.utils.ModelLoaderUtils;

import java.io.File;
import java.util.Optional;

/**
 * The Maven mojo to generate Java client code using aws-java-sdk-codegen-resourceserator.
 */
@Mojo(name = "generate")
public class GenerationMojo extends AbstractMojo {

    @Parameter(property = "modelFile", defaultValue = "codegen-resources/service-2.json")
    private String serviceModelLocation;

    @Parameter(property = "codeGenConfigFile", defaultValue = "codegen-resources/codegen.config")
    private String codeGenConfigLocation;

    @Parameter(property = "customizationConfigFile", defaultValue = "codegen-resources/customization.config")
    private String customizationConfigLocation;

    @Parameter(property = "examplesFile", defaultValue = "codegen-resources/examples-1.json")
    private String serviceExamplesLocation;

    @Parameter(property = "waitersFile", defaultValue = "codegen-resources/waiters-2.json")
    private String waitersLocation;

    @Parameter(property = "outputDirectory", defaultValue = "${project.build.directory}/generated-src")
    private String outputDirectory;

    @Parameter(property = "resourcesDirectory", defaultValue = "${basedir}/src/main/resources")
    private String resourcesDirectory;

    @Parameter(property = "codeGenDirectory", defaultValue = "${session.executionRootDirectory}/codegen/src/main/resources/")
    private String codeGenDirectory;

    @Component
    private MavenProject project;

    public void execute() throws MojoExecutionException {
        generateCode(C2jModels.builder()
                             .codeGenConfig(loadCodeGenConfig())
                             .customizationConfig(loadCustomizationConfig())
                             .serviceModel(loadServiceModel())
                             // Maven plugin doesn't support Waiters
                             .waitersModel(loadWaiterModel())
                             .examplesModel(loadExamplesModel())
                             .build());
        project.addCompileSourceRoot(outputDirectory);
    }

    private void generateCode(C2jModels models) {
        new CodeGenerator(models, outputDirectory, codeGenDirectory, Utils.getFileNamePrefix(models.serviceModel())).execute();
    }

    private BasicCodeGenConfig loadCodeGenConfig() throws MojoExecutionException {
        return loadRequiredModel(BasicCodeGenConfig.class, codeGenConfigLocation);
    }

    private CustomizationConfig loadCustomizationConfig() {
        return loadOptionalModel(CustomizationConfig.class, customizationConfigLocation).orElse(CustomizationConfig.DEFAULT);
    }

    private ServiceModel loadServiceModel() throws MojoExecutionException {
        return loadRequiredModel(ServiceModel.class, serviceModelLocation);
    }

    private ServiceExamples loadExamplesModel() {
        return loadOptionalModel(ServiceExamples.class, serviceExamplesLocation).orElse(ServiceExamples.NONE);
    }

    private Waiters loadWaiterModel() {
        return loadOptionalModel(Waiters.class, waitersLocation).orElse(Waiters.NONE);
    }

    /**
     * Load required model from the project resources.
     */
    private <T> T loadRequiredModel(Class<T> clzz, String location) throws MojoExecutionException {
        return ModelLoaderUtils.loadModel(clzz, getResourceLocation(location));
    }

    /**
     * Load an optional model from the project resources.
     *
     * @return Model or empty optional if not present.
     */
    private <T> Optional<T> loadOptionalModel(Class<T> clzz, String location) {
        return ModelLoaderUtils.loadOptionalModel(clzz, getResourceLocation(location));
    }

    private File getResourceLocation(String location) {
        return new File(resourcesDirectory, location);
    }

}
