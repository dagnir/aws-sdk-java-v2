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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Optional;
import java.util.stream.Stream;

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

/**
 * The Maven mojo to generate Java client code using software.amazon.awssdk:codegen module.
 */
@Mojo(name = "generate")
public class GenerationMojo extends AbstractMojo {

    private static final String modelFile = "service-2.json";
    private static final String codeGenConfigFile = "codegen.config";
    private static final String customizationConfigFile = "customization.config";
    private static final String examplesFile = "examples-1.json";
    private static final String waitersFile = "waiters-2.json";

    @Parameter(property = "codeGenResources", defaultValue = "${basedir}/src/main/resources/codegen-resources/")
    private File codeGenResources;

    @Parameter(property = "outputDirectory", defaultValue = "${project.build.directory}/generated-sources/aws")
    private String outputDirectory;

    @Component
    private MavenProject project;

    public void execute() throws MojoExecutionException {
        findModelRoots().forEach(p -> {
            try {
                getLog().info("Loading from: " + p.toString());
                generateCode(C2jModels.builder()
                        .codeGenConfig(loadCodeGenConfig(p))
                        .customizationConfig(loadCustomizationConfig(p))
                        .serviceModel(loadServiceModel(p))
                        .waitersModel(loadWaiterModel(p))
                        .examplesModel(loadExamplesModel(p))
                        .build());
            } catch (MojoExecutionException e) {
                throw new RuntimeException(e);
            }
        });
        project.addCompileSourceRoot(outputDirectory);
    }

    private Stream<Path> findModelRoots() throws MojoExecutionException {
        try {
            return Files.find(codeGenResources.toPath(), 10, this::isModelFile).map(Path::getParent);
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to find '" + modelFile + "' files in " + codeGenResources, e);
        }
    }

    private boolean isModelFile(Path p, BasicFileAttributes a) {
        return p.toString().endsWith(modelFile);
    }

    private void generateCode(C2jModels models) {
        new CodeGenerator(models, outputDirectory, Utils.getFileNamePrefix(models.serviceModel())).execute();
    }

    private BasicCodeGenConfig loadCodeGenConfig(Path root) throws MojoExecutionException {
        return loadRequiredModel(BasicCodeGenConfig.class, root.resolve(codeGenConfigFile));
    }

    private CustomizationConfig loadCustomizationConfig(Path root) {
        return loadOptionalModel(CustomizationConfig.class, root.resolve(customizationConfigFile))
                .orElse(CustomizationConfig.DEFAULT);
    }

    private ServiceModel loadServiceModel(Path root) throws MojoExecutionException {
        return loadRequiredModel(ServiceModel.class, root.resolve(modelFile));
    }

    private ServiceExamples loadExamplesModel(Path root) {
        return loadOptionalModel(ServiceExamples.class, root.resolve(examplesFile)).orElse(ServiceExamples.NONE);
    }

    private Waiters loadWaiterModel(Path root) {
        return loadOptionalModel(Waiters.class, root.resolve(waitersFile)).orElse(Waiters.NONE);
    }

    /**
     * Load required model from the project resources.
     */
    private <T> T loadRequiredModel(Class<T> clzz, Path location) throws MojoExecutionException {
        return ModelLoaderUtils.loadModel(clzz, location.toFile());
    }

    /**
     * Load an optional model from the project resources.
     *
     * @return Model or empty optional if not present.
     */
    private <T> Optional<T> loadOptionalModel(Class<T> clzz, Path location) {
        return ModelLoaderUtils.loadOptionalModel(clzz, location.toFile());
    }
}
