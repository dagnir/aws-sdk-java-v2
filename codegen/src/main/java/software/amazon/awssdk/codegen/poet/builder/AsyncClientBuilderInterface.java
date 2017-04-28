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

package software.amazon.awssdk.codegen.poet.builder;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import software.amazon.awssdk.client.builder.AsyncClientBuilder;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.PoetUtils;

public class AsyncClientBuilderInterface implements ClassSpec {
    private final ClassName builderInterfaceName;
    private final ClassName clientInterfaceName;
    private final ClassName baseBuilderInterfaceName;

    public AsyncClientBuilderInterface(IntermediateModel model) {
        String basePackage = model.getMetadata().getPackageName();
        this.clientInterfaceName = ClassName.get(basePackage, model.getMetadata().getAsyncInterface());
        this.builderInterfaceName = ClassName.get(basePackage, model.getMetadata().getAsyncBuilderInterface());
        this.baseBuilderInterfaceName = ClassName.get(basePackage, model.getMetadata().getBaseBuilderInterface());
    }

    @Override
    public TypeSpec poetSpec() {
        return PoetUtils.createInterfaceBuilder(builderInterfaceName)
                        .addSuperinterface(ParameterizedTypeName.get(ClassName.get(AsyncClientBuilder.class),
                                                                     builderInterfaceName, clientInterfaceName))
                        .addSuperinterface(ParameterizedTypeName.get(baseBuilderInterfaceName,
                                                                     builderInterfaceName, clientInterfaceName))
                        .build();
    }

    @Override
    public ClassName className() {
        return builderInterfaceName;
    }
}
