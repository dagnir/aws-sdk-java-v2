/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.codegen.customization.processors;

import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.codegen.customization.CodegenCustomizationProcessor;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.MemberModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.model.service.ServiceModel;

/**
 * Applies the "marshallAutoConstructListMembers" customization.
 *
 * @see CustomizationConfig#setMarshallAutoConstructListMembers(Map)
 */
public class MarshallAutoConstructListMembersProcessor implements CodegenCustomizationProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(MarshallAutoConstructListMembersProcessor.class);

    @Override
    public void preprocess(ServiceModel serviceModel) {
        // nothing to do
    }

    @Override
    public void postprocess(final IntermediateModel intermediateModel) {
        CustomizationConfig cfg = intermediateModel.getCustomizationConfig();

        Map<String, List<String>> shapeMembers = cfg.getMarshallAutoConstructListMembers();

        if (shapeMembers == null || shapeMembers.isEmpty()) {
            return;
        }

        shapeMembers.forEach((s,lm) -> {
            if (lm == null) {
                return;
            }

            ShapeModel shapeModel = intermediateModel.getShapes().get(s);

            if (shapeModel == null) {
                LOG.warn("Shape \"{}\" does not exist in the model.", s);
                return;
            }

            lm.forEach(mName -> {
                MemberModel member = shapeModel.getMemberByName(mName);
                if (member == null || member.getListModel() == null) {
                    LOG.warn("\"{}\" member of shape \"{}\" is not a list. Skipping 'marshallAutoConstructList' customization"
                            + " for this member.", mName, s);
                    return;
                }
                member.getListModel().setMarshallAutoConstructList(true);
            });
        });
    }
}
