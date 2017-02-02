/*
 * Copyright 2011-2017 Amazon Technologies, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *    http://aws.amazon.com/apache2.0
 *
 * This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and
 * limitations under the License.
 */
package software.amazon.awssdk.services.cloudformation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import software.amazon.awssdk.AmazonServiceException;
import software.amazon.awssdk.AmazonServiceException.ErrorType;
import software.amazon.awssdk.services.cloudformation.model.EstimateTemplateCostRequest;
import software.amazon.awssdk.services.cloudformation.model.EstimateTemplateCostResult;
import software.amazon.awssdk.services.cloudformation.model.TemplateParameter;
import software.amazon.awssdk.services.cloudformation.model.ValidateTemplateRequest;
import software.amazon.awssdk.services.cloudformation.model.ValidateTemplateResult;

/**
 * Integration tests of the template-related API of CloudFormation.
 */
public class TemplateIntegrationTests extends CloudFormationIntegrationTestBase {

    private static final String TEMPLATE_DESCRIPTION = "Template Description";
    public static final String TEMPLATE_URL = "https://s3.amazonaws.com/cloudformation-templates/sampleTemplate";

    @Test
    public void testValidateTemplateURL() {
        ValidateTemplateResult response = cf.validateTemplate(new ValidateTemplateRequest()
                .withTemplateURL(templateUrlForCloudFormationIntegrationTests));

        assertTrue(response.getParameters().size() > 0);
        for (TemplateParameter tp : response.getParameters()) {
            assertNotNull(tp.getParameterKey());
        }
    }

    @Test
    public void testValidateTemplateBody() throws Exception {
        String templateText = FileUtils.readFileToString(new File("tst/" + templateForCloudFormationIntegrationTests));
        ValidateTemplateResult response = cf.validateTemplate(new ValidateTemplateRequest()
                .withTemplateBody(templateText));
        assertEquals(TEMPLATE_DESCRIPTION, response.getDescription());
        assertEquals(3, response.getParameters().size());

        Set<String> expectedTemplateKeys = new HashSet<String>();
        expectedTemplateKeys.add("InstanceType");
        expectedTemplateKeys.add("WebServerPort");
        expectedTemplateKeys.add("KeyPair");

        for (TemplateParameter tp : response.getParameters()) {
            assertTrue(expectedTemplateKeys.remove(tp.getParameterKey()));
            assertNotNull(tp.getDefaultValue());
            assertNotEmpty(tp.getDescription());
        }

        assertTrue("expected parameter not found", expectedTemplateKeys.isEmpty());
    }

    @Test
    public void testInvalidTemplate() {
        try {
            cf.validateTemplate(new ValidateTemplateRequest().withTemplateBody("{\"Foo\" : \"Bar\"}"));
            fail("Should have thrown an exception");
        } catch (AmazonServiceException acfx) {
            assertEquals("ValidationError", acfx.getErrorCode());
            assertEquals(ErrorType.Client, acfx.getErrorType());
        } catch (Exception e) {
            fail("Should have thrown an AmazonCloudFormation Exception");
        }
    }

    @Test
    public void testEstimateCost() throws Exception {
        String templateText = FileUtils.readFileToString(new File("tst/" + templateForCloudFormationIntegrationTests));
        EstimateTemplateCostResult estimateTemplateCost = cf.estimateTemplateCost(new EstimateTemplateCostRequest()
                .withTemplateBody(templateText));
        assertNotNull(estimateTemplateCost.getUrl());

        estimateTemplateCost = cf.estimateTemplateCost(new EstimateTemplateCostRequest()
                .withTemplateURL(templateUrlForCloudFormationIntegrationTests));
        assertNotNull(estimateTemplateCost.getUrl());
    }

}
