/*
 * Copyright 2011-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package software.amazon.awssdk.protocol.reflect;

import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.MemberModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.util.StringInputStream;
import software.amazon.awssdk.util.ValidationUtils;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Transforms a JSON representation (using C2J member names) of a modeled POJO into that POJO.
 */
public class ShapeModelReflector {

    private final IntermediateModel model;
    private final String shapeName;
    private final JsonNode input;

    public ShapeModelReflector(IntermediateModel model, String shapeName, JsonNode input) {
        this.model = ValidationUtils.assertNotNull(model, "model");
        this.shapeName = ValidationUtils.assertNotNull(shapeName, "shapeName");
        this.input = input;
    }

    private Object createStructure(ShapeModel structureShape, JsonNode input) throws Exception {
        String fqcn = getFullyQualifiedModelClassName(structureShape.getShapeName());
        Object shapeObject = Class.forName(fqcn).newInstance();
        if (input != null) {
            initializeFields(structureShape, input, shapeObject);
        }
        return shapeObject;
    }

    private void initializeFields(ShapeModel structureShape, JsonNode input,
                                  Object shapeObject) throws Exception {
        Iterator<String> fieldNames = input.fieldNames();
        while (fieldNames.hasNext()) {
            String memberName = fieldNames.next();
            MemberModel memberModel = structureShape.getMemberByC2jName(memberName);
            if (memberModel == null) {
                throw new IllegalArgumentException("Member " + memberName + " was not found in the " + structureShape.getC2jName() + " shape.");
            }
            Method setter = getMemberSetter(shapeObject.getClass(), memberModel);
            final Object toSet = getMemberValue(input.get(memberName), memberModel);
            setter.invoke(shapeObject, toSet);
        }
    }

    private String getFullyQualifiedModelClassName(String modelClassName) {
        return String.format("%s.model.%s", model.getMetadata().getPackageName(), modelClassName);
    }

    /**
     * Find the corresponding setter method for the member. Assumes only simple types are
     * supported.
     *
     * @param currentMember Member to get setter for.
     * @return Setter Method object.
     */
    private Method getMemberSetter(Class<?> containingClass, MemberModel currentMember) throws
                                                                                        Exception {
        return containingClass.getMethod("set" + currentMember.getName(),
                                         Class.forName(getFullyQualifiedType(currentMember)));
    }

    private String getFullyQualifiedType(MemberModel memberModel) {
        if (memberModel.isSimple()) {
            switch (memberModel.getVariable().getSimpleType()) {
                case "Date":
                case "ByteBuffer":
                case "InputStream":
                    return memberModel.getSetterModel().getVariableSetterType();
                default:
                    return "java.lang." + memberModel.getSetterModel().getVariableSetterType();
            }
        } else if (memberModel.isList()) {
            return "java.util.Collection";
        } else if (memberModel.isMap()) {
            return "java.util.Map";
        } else {
            return getFullyQualifiedModelClassName(
                    memberModel.getSetterModel().getVariableSetterType());
        }
    }

    /**
     * Get the value of the member as specified in the test description. Only supports simple types
     * at the moment.
     *
     * @param currentNode JsonNode containing value of member.
     */
    private Object getMemberValue(JsonNode currentNode, MemberModel memberModel) {
        if (currentNode.isNull()) {
            return null;
        }
        if (memberModel.isSimple()) {
            return getSimpleMemberValue(currentNode, memberModel);
        } else if (memberModel.isList()) {
            return getListMemberValue(currentNode, memberModel);
        } else if (memberModel.isMap()) {
            return getMapMemberValue(currentNode, memberModel);
        } else {
            ShapeModel structureShape = model.getShapes()
                    .get(memberModel.getVariable().getVariableType());
            try {
                return createStructure(structureShape, currentNode);
            } catch (Exception e) {
                throw new TestCaseReflectionException(e);
            }
        }
    }

    private Object getMapMemberValue(JsonNode currentNode, MemberModel memberModel) {
        Map<String, Object> map = new HashMap<>();
        currentNode.fields().forEachRemaining(e -> {
            map.put(e.getKey(),
                    getMemberValue(e.getValue(), memberModel.getMapModel().getValueModel()));
        });
        return map;
    }

    private Object getListMemberValue(JsonNode currentNode, MemberModel memberModel) {
        ArrayList<Object> list = new ArrayList<>();
        currentNode.elements().forEachRemaining(e -> {
            list.add(getMemberValue(e, memberModel.getListModel().getListMemberModel()));
        });
        return list;
    }

    private Object getSimpleMemberValue(JsonNode currentNode, MemberModel memberModel) {
        switch (memberModel.getVariable().getSimpleType()) {
            case "Long":
                return currentNode.asLong();
            case "Integer":
                return currentNode.asInt();
            case "String":
                return currentNode.asText();
            case "Boolean":
                return currentNode.asBoolean();
            case "Double":
                return currentNode.asDouble();
            case "Date":
                return new Date(currentNode.asLong());
            case "ByteBuffer":
                return ByteBuffer.wrap(currentNode.asText().getBytes(StandardCharsets.UTF_8));
            case "Float":
                return Float.valueOf((float) currentNode.asDouble());
            case "Character":
                return asCharacter(currentNode);
            case "InputStream":
                return toInputStream(currentNode);
            default:
                throw new IllegalArgumentException(
                        "Unsupported type " + memberModel.getVariable().getSimpleType());
        }
    }

    private Object toInputStream(JsonNode currentNode) {
        try {
            return new StringInputStream(currentNode.asText());
        } catch (UnsupportedEncodingException e) {
            throw new TestCaseReflectionException(e);
        }
    }

    private Character asCharacter(JsonNode currentNode) {
        String text = currentNode.asText();
        if (text != null && text.length() > 1) {
            throw new IllegalArgumentException("Invalid character " + currentNode.asText());
        } else if (text != null && text.length() == 1) {
            return Character.valueOf(currentNode.asText().charAt(0));
        } else {
            return null;
        }
    }

    public Object createShapeObject() {
        try {
            return createStructure(model.getShapes().get(shapeName), input);
        } catch (Exception e) {
            throw new TestCaseReflectionException(e);
        }
    }


}
