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

package software.amazon.awssdk.codegen.poet.client;

import static com.squareup.javapoet.TypeSpec.Builder;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.client.AwsAsyncClientParams;
import software.amazon.awssdk.codegen.emitters.GeneratorTaskParams;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.codegen.poet.PoetExtensions;
import software.amazon.awssdk.codegen.poet.PoetUtils;

public final class AsyncClientClass extends AsyncClientInterface {
    private final PoetExtensions poetExtensions;
    private final ClassName className;

    public AsyncClientClass(GeneratorTaskParams dependencies) {
        super(dependencies.getModel());
        this.poetExtensions = dependencies.getPoetExtensions();
        this.className = poetExtensions.getClientClass(model.getMetadata().getAsyncClient());
    }

    @Override
    public TypeSpec poetSpec() {
        ClassName interfaceClass = poetExtensions.getClientClass(model.getMetadata().getAsyncInterface());
        ClassName syncInterface = poetExtensions.getClientClass(model.getMetadata().getSyncInterface());
        Builder classBuilder = PoetUtils.createClassBuilder(className)
                                        .addSuperinterface(interfaceClass)
                                        .addField(syncInterface, "syncClient", Modifier.PRIVATE, Modifier.FINAL)
                                        .addField(ExecutorService.class, "executor", Modifier.PRIVATE, Modifier.FINAL)
                                        .addMethod(createConstructor())
                                        .addMethods(operations())
                                        .addMethod(closeMethod());

        return classBuilder.build();
    }

    private MethodSpec closeMethod() {
        return MethodSpec.methodBuilder("close")
                         .addAnnotation(Override.class)
                         .addException(Exception.class)
                         .addModifiers(Modifier.PUBLIC)
                         .addStatement("$N.close()", "syncClient")
                         .build();
    }

    @Override
    protected MethodSpec.Builder operationBody(MethodSpec.Builder builder, OperationModel opModel) {
        return builder.addModifiers(Modifier.PUBLIC)
                      .addAnnotation(Override.class)
                      .addStatement("return $T.supplyAsync(() -> $N.$N($N), $N)",
                                    CompletableFuture.class,
                                    "syncClient",
                                    opModel.getMethodName(),
                                    opModel.getInput().getVariableName(),
                                    "executor");
    }

    private MethodSpec createConstructor() {
        ClassName syncClient = poetExtensions.getClientClass(model.getMetadata().getSyncClient());
        return MethodSpec.constructorBuilder()
                         .addParameter(AwsAsyncClientParams.class, "asyncClientParams")
                         .addStatement("this.$N = new $T($N)", "syncClient", syncClient, "asyncClientParams")
                         .addStatement("this.$N = $N", "executor", "asyncClientParams.getExecutor()")
                         .build();
    }

    @Override
    public ClassName className() {
        return className;
    }
}
