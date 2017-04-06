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

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import software.amazon.awssdk.codegen.emitters.FreemarkerGeneratorTask;
import software.amazon.awssdk.codegen.emitters.GeneratorTask;
import software.amazon.awssdk.codegen.emitters.GeneratorTaskParams;
import software.amazon.awssdk.codegen.emitters.PoetGeneratorTask;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.client.SyncClientClass;
import software.amazon.awssdk.codegen.poet.client.SyncClientInterface;

public class SyncClientGeneratorTasks extends BaseGeneratorTasks {

    private final GeneratorTaskParams generatorTaskParams;
    private final String baseDirectory;

    public SyncClientGeneratorTasks(GeneratorTaskParams dependencies) {
        super(dependencies);
        this.generatorTaskParams = dependencies;
        this.baseDirectory = dependencies.getPathProvider().getBasePackageDirectory();
    }

    @Override
    protected List<GeneratorTask> createTasks() throws Exception {
        info("Emitting Sync client classes");
        return Arrays.asList(createClientClassTask(),
                             createClientBuilderTask(),
                             createClientInterfaceTask());
    }

    private GeneratorTask createClientClassTask() throws IOException {
        ClassSpec syncClientClass = new SyncClientClass(generatorTaskParams);
        return new PoetGeneratorTask(baseDirectory, model.getFileHeader(), syncClientClass);
    }

    private GeneratorTask createClientBuilderTask() throws IOException {
        return new FreemarkerGeneratorTask(
                baseDirectory,
                model.getMetadata().getSyncClientBuilderClassName(),
                freemarker.getSyncClientBuilderTemplate(),
                model);
    }

    private GeneratorTask createClientInterfaceTask() throws IOException {
        ClassSpec syncClientClass = new SyncClientInterface(model);
        return new PoetGeneratorTask(baseDirectory, model.getFileHeader(), syncClientClass);
    }
}
