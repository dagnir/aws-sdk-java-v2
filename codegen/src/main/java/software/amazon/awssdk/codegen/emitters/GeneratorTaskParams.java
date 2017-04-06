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

package software.amazon.awssdk.codegen.emitters;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import software.amazon.awssdk.codegen.internal.Freemarker;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.poet.PoetExtensions;

/**
 * Parameters for generator tasks.
 */
public class GeneratorTaskParams {

    private final Freemarker freemarker;
    private final IntermediateModel model;
    private final GeneratorPathProvider pathProvider;
    private final PoetExtensions poetExtensions;
    private final Log log = LogFactory.getLog(GeneratorTaskParams.class);

    private GeneratorTaskParams(Freemarker freemarker,
                                IntermediateModel model,
                                GeneratorPathProvider pathProvider) {
        this.freemarker = freemarker;
        this.model = model;
        this.pathProvider = pathProvider;
        this.poetExtensions = new PoetExtensions(model);
    }

    public static GeneratorTaskParams create(IntermediateModel model, String sourceDirectory, String testDirectory) {
        final GeneratorPathProvider pathProvider = new GeneratorPathProvider(model, sourceDirectory, testDirectory);
        return new GeneratorTaskParams(Freemarker.create(model), model, pathProvider);
    }

    /**
     * @return Freemarker processing engine
     */
    public Freemarker getFreemarker() {
        return freemarker;
    }

    /**
     * @return Built intermediate model
     */
    public IntermediateModel getModel() {
        return model;
    }

    /**
     * @return Provider for common paths.
     */
    public GeneratorPathProvider getPathProvider() {
        return pathProvider;
    }

    /**
     * @return Extensions and convenience methods for Java Poet.
     */
    public PoetExtensions getPoetExtensions() {
        return poetExtensions;
    }

    /**
     * @return Logger
     */
    public Log getLog() {
        return log;
    }
}
