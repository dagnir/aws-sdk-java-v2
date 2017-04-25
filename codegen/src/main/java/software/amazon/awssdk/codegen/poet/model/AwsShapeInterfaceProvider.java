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

package software.amazon.awssdk.codegen.poet.model;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import software.amazon.awssdk.AmazonWebServiceRequest;
import software.amazon.awssdk.AmazonWebServiceResult;
import software.amazon.awssdk.SdkClientException;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeType;
import software.amazon.awssdk.protocol.StructuredPojo;

public class AwsShapeInterfaceProvider implements ShapeInterfaceProvider {
    private final IntermediateModel intermediateModel;
    private final ShapeModel shapeModel;

    public AwsShapeInterfaceProvider(IntermediateModel intermediateModel, ShapeModel shapeModel) {
        this.intermediateModel = intermediateModel;
        this.shapeModel = shapeModel;
    }

    @Override
    public boolean shouldImplementInterface(Class<?> iface) {
        return interfacesToImplement().contains(iface);
    }

    @Override
    public Set<Class<?>> interfacesToImplement() {
        Set<Class<?>> superInterfaces = new HashSet<>();

        switch (shapeModel.getShapeType()) {
            case Request:
            case Model:
            case Response:
                Stream.of(Serializable.class, Cloneable.class)
                        .forEach(superInterfaces::add);
                break;
            default:
                break;
        }

        if (implementStructuredPojoInterface()) {
            superInterfaces.add(StructuredPojo.class);
        }

        return superInterfaces;

    }

    @Override
    public Class<?> baseClassToExtend() {
        switch (shapeModel.getShapeType()) {
            case Request:
                return AmazonWebServiceRequest.class;
            case Response:
                return AmazonWebServiceResult.class;
            case Exception:
                return SdkClientException.class;
            case Model:
            default:
                return Object.class;
        }
    }

    boolean implementStructuredPojoInterface() {
        return intermediateModel.getMetadata().isJsonProtocol() && shapeModel.getShapeType() == ShapeType.Model;
    }
}
