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

package software.amazon.awssdk.services.datapipeline;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import org.junit.AfterClass;
import org.junit.Test;
import software.amazon.awssdk.AmazonServiceException;
import software.amazon.awssdk.services.datapipeline.model.ActivatePipelineRequest;
import software.amazon.awssdk.services.datapipeline.model.ActivatePipelineResult;
import software.amazon.awssdk.services.datapipeline.model.CreatePipelineRequest;
import software.amazon.awssdk.services.datapipeline.model.CreatePipelineResult;
import software.amazon.awssdk.services.datapipeline.model.DeletePipelineRequest;
import software.amazon.awssdk.services.datapipeline.model.DescribeObjectsRequest;
import software.amazon.awssdk.services.datapipeline.model.DescribeObjectsResult;
import software.amazon.awssdk.services.datapipeline.model.DescribePipelinesRequest;
import software.amazon.awssdk.services.datapipeline.model.DescribePipelinesResult;
import software.amazon.awssdk.services.datapipeline.model.Field;
import software.amazon.awssdk.services.datapipeline.model.GetPipelineDefinitionRequest;
import software.amazon.awssdk.services.datapipeline.model.GetPipelineDefinitionResult;
import software.amazon.awssdk.services.datapipeline.model.ListPipelinesRequest;
import software.amazon.awssdk.services.datapipeline.model.ListPipelinesResult;
import software.amazon.awssdk.services.datapipeline.model.PipelineObject;
import software.amazon.awssdk.services.datapipeline.model.PutPipelineDefinitionRequest;
import software.amazon.awssdk.services.datapipeline.model.PutPipelineDefinitionResult;
import software.amazon.awssdk.services.datapipeline.model.ValidatePipelineDefinitionRequest;
import software.amazon.awssdk.services.datapipeline.model.ValidatePipelineDefinitionResult;

public class DataPipelineIntegrationTest extends IntegrationTestBase {

    private static final String PIPELINE_NAME = "my-pipeline";
    private static final String PIPELINE_ID = "my-pipeline" + System.currentTimeMillis();
    private static final String PIPELINE_DESCRIPTION = "my pipeline";
    private static final String OBJECT_ID = "123";
    private static final String OBJECT_NAME = "object";
    private static final String VALID_KEY = "startDateTime";
    private static final String INVALID_KEY = "radom_key";
    private static final String FIELD_VALUE = "2012-09-25T17:00:00";
    private static String pipelineId;

    @AfterClass
    public static void tearDown() {
        try {
            dataPipeline.deletePipeline(DeletePipelineRequest.builder_().pipelineId(pipelineId).build_());
        } catch (Exception e) {
            // Do nothing.
        }
    }

    @Test
    public void testPipelineOperations() throws InterruptedException {
        // Create a pipeline.
        CreatePipelineResult createPipelineResult = dataPipeline.createPipeline(
                CreatePipelineRequest.builder_()
                        .name(PIPELINE_NAME)
                        .uniqueId(PIPELINE_ID)
                        .description(PIPELINE_DESCRIPTION)
                        .build_());
        pipelineId = createPipelineResult.pipelineId();
        assertNotNull(pipelineId);


        // Invalid field
        PipelineObject pipelineObject = PipelineObject.builder_()
                .id(OBJECT_ID + "1")
                .name(OBJECT_NAME)
                .fields(Field.builder_()
                        .key(INVALID_KEY)
                        .stringValue(FIELD_VALUE)
                        .build_())
                .build_();

        ValidatePipelineDefinitionResult validatePipelineDefinitionResult =
                dataPipeline.validatePipelineDefinition(ValidatePipelineDefinitionRequest.builder_()
                        .pipelineId(pipelineId)
                        .pipelineObjects(pipelineObject)
                        .build_());
        assertTrue(validatePipelineDefinitionResult.errored());
        assertNotNull(validatePipelineDefinitionResult.validationErrors());
        assertTrue(validatePipelineDefinitionResult.validationErrors().size() > 0);
        assertNotNull(validatePipelineDefinitionResult.validationErrors().get(0));
        assertNotNull(validatePipelineDefinitionResult.validationWarnings());
        assertEquals(0, validatePipelineDefinitionResult.validationWarnings().size());

        // Valid field
        pipelineObject = PipelineObject.builder_()
                .id(OBJECT_ID)
                .name(OBJECT_NAME)
                .fields(Field.builder_()
                        .key(VALID_KEY)
                        .stringValue(FIELD_VALUE)
                        .build_())
                .build_();

        // Validate pipeline definition.
        validatePipelineDefinitionResult =
                dataPipeline.validatePipelineDefinition(ValidatePipelineDefinitionRequest.builder_()
                        .pipelineId(pipelineId)
                        .pipelineObjects(pipelineObject)
                        .build_());
        assertFalse(validatePipelineDefinitionResult.errored());
        assertNotNull(validatePipelineDefinitionResult.validationErrors());
        assertEquals(0, validatePipelineDefinitionResult.validationErrors().size());
        assertNotNull(validatePipelineDefinitionResult.validationWarnings());
        assertEquals(0, validatePipelineDefinitionResult.validationWarnings().size());

        // Put pipeline definition.
        PutPipelineDefinitionResult putPipelineDefinitionResult =
                dataPipeline.putPipelineDefinition(PutPipelineDefinitionRequest.builder_()
                        .pipelineId(pipelineId)
                        .pipelineObjects(pipelineObject)
                        .build_());
        assertFalse(putPipelineDefinitionResult.errored());
        assertNotNull(putPipelineDefinitionResult.validationErrors());
        assertEquals(0, putPipelineDefinitionResult.validationErrors().size());
        assertNotNull(putPipelineDefinitionResult.validationWarnings());
        assertEquals(0, putPipelineDefinitionResult.validationWarnings().size());

        // Get pipeline definition.
        GetPipelineDefinitionResult pipelineDefinitionResult =
                dataPipeline.getPipelineDefinition(GetPipelineDefinitionRequest.builder_().pipelineId(pipelineId).build_());
        assertEquals(1, pipelineDefinitionResult.pipelineObjects().size());
        assertEquals(OBJECT_ID, pipelineDefinitionResult.pipelineObjects().get(0).id());
        assertEquals(OBJECT_NAME, pipelineDefinitionResult.pipelineObjects().get(0).name());
        assertEquals(1, pipelineDefinitionResult.pipelineObjects().get(0).fields().size());
        assertTrue(pipelineDefinitionResult.pipelineObjects().get(0).fields()
                                              .contains(Field.builder_().key(VALID_KEY).stringValue(FIELD_VALUE)));

        // Activate a pipeline.
        ActivatePipelineResult activatePipelineResult =
                dataPipeline.activatePipeline(ActivatePipelineRequest.builder_().pipelineId(pipelineId).build_());
        assertNotNull(activatePipelineResult);

        // List pipeline.
        ListPipelinesResult listPipelinesResult = dataPipeline.listPipelines(ListPipelinesRequest.builder_().build_());
        assertTrue(listPipelinesResult.pipelineIdList().size() > 0);
        assertNotNull(pipelineId, listPipelinesResult.pipelineIdList().get(0).id());
        assertNotNull(PIPELINE_NAME, listPipelinesResult.pipelineIdList().get(0).name());

        Thread.sleep(1000 * 5);

        // Describe objects.
        DescribeObjectsResult describeObjectsResult =
                dataPipeline.describeObjects(DescribeObjectsRequest.builder_().pipelineId(pipelineId).objectIds(OBJECT_ID).build_());
        assertEquals(1, describeObjectsResult.pipelineObjects().size());
        assertEquals(OBJECT_ID, describeObjectsResult.pipelineObjects().get(0).id());
        assertEquals(OBJECT_NAME, describeObjectsResult.pipelineObjects().get(0).name());
        assertTrue(describeObjectsResult.pipelineObjects().get(0).fields()
                                        .contains(Field.builder_().key(VALID_KEY).stringValue(FIELD_VALUE)));
        assertTrue(describeObjectsResult.pipelineObjects().get(0).fields()
                                        .contains(Field.builder_().key("@pipelineId").stringValue(pipelineId)));

        // Describe a pipeline.
        DescribePipelinesResult describepipelinesResult =
                dataPipeline.describePipelines(DescribePipelinesRequest.builder_().pipelineIds(pipelineId).build_());
        assertEquals(1, describepipelinesResult.pipelineDescriptionList().size());
        assertEquals(PIPELINE_NAME, describepipelinesResult.pipelineDescriptionList().get(0).name());
        assertEquals(pipelineId, describepipelinesResult.pipelineDescriptionList().get(0).pipelineId());
        assertEquals(PIPELINE_DESCRIPTION, describepipelinesResult.pipelineDescriptionList().get(0).description());
        assertTrue(describepipelinesResult.pipelineDescriptionList().get(0).fields().size() > 0);
        assertTrue(describepipelinesResult.pipelineDescriptionList().get(0).fields()
                                          .contains(Field.builder_().key("name").stringValue(PIPELINE_NAME)));
        assertTrue(describepipelinesResult.pipelineDescriptionList().get(0).fields()
                                          .contains(Field.builder_().key("@id").stringValue(pipelineId)));
        assertTrue(describepipelinesResult.pipelineDescriptionList().get(0).fields()
                                          .contains(Field.builder_().key("uniqueId").stringValue(PIPELINE_ID)));

        // Delete a pipeline.
        dataPipeline.deletePipeline(DeletePipelineRequest.builder_().pipelineId(pipelineId).build_());
        Thread.sleep(1000 * 5);
        try {
            describepipelinesResult = dataPipeline.describePipelines(DescribePipelinesRequest.builder_().pipelineIds(pipelineId).build_());
            if (describepipelinesResult.pipelineDescriptionList().size() > 0) {
                fail();
            }
        } catch (AmazonServiceException e) {
            // Ignored or expected.
        }
    }
}
