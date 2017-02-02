package software.amazon.awssdk.services.edp;

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
import software.amazon.awssdk.services.datapipeline.model.ListPipelinesResult;
import software.amazon.awssdk.services.datapipeline.model.PipelineObject;
import software.amazon.awssdk.services.datapipeline.model.PutPipelineDefinitionRequest;
import software.amazon.awssdk.services.datapipeline.model.PutPipelineDefinitionResult;
import software.amazon.awssdk.services.datapipeline.model.ValidatePipelineDefinitionRequest;
import software.amazon.awssdk.services.datapipeline.model.ValidatePipelineDefinitionResult;

public class ElasticDatePipelineIntegrationTest extends IntegrationTestBase {

    private static final String PIPELINE_NAME = "my-pipeline";
    private static final String PIPELINE_ID = "my-pipeline" + System.currentTimeMillis();
    private static final String PIPELINE_DESCRIPTION = "my pipeline";
    private static final String OBJECT_ID = "123";
    private static final String OBJECT_NAME = "object";
    private static final String VALID_KEY = "startDateTime";
    private static final String INVALID_KEY = "radom_key";
    private static final String FIELD_VALUE ="2012-09-25T17:00:00";
    private static String pipelineId;

    @AfterClass
    public static void tearDown() {
        try {
            edp.deletePipeline(new DeletePipelineRequest().withPipelineId(pipelineId));
        } catch (Exception e) {
            // Do nothing.
        }
    }

    @Test
    public void testPipelineOperations() throws InterruptedException {
        // Create a pipeline.
        CreatePipelineResult createPipelineResult = edp.createPipeline(new CreatePipelineRequest().withName(PIPELINE_NAME).withUniqueId(PIPELINE_ID).withDescription(PIPELINE_DESCRIPTION));
        pipelineId = createPipelineResult.getPipelineId();
        assertNotNull(pipelineId);


        // Invalid field
        PipelineObject pipelineObject = new PipelineObject();
        pipelineObject.setId(OBJECT_ID + "1");
        pipelineObject.setName(OBJECT_NAME);
        Field field = new Field();
        field.setKey(INVALID_KEY);
        field.setStringValue(FIELD_VALUE);
        pipelineObject.setFields(Arrays.asList(field));

        ValidatePipelineDefinitionResult validatePipelineDefinitionResult = edp.validatePipelineDefinition(new ValidatePipelineDefinitionRequest().withPipelineId(pipelineId)
                .withPipelineObjects(pipelineObject));
        assertTrue(validatePipelineDefinitionResult.getErrored());
        assertNotNull(validatePipelineDefinitionResult.getValidationErrors());
        assertTrue(validatePipelineDefinitionResult.getValidationErrors().size() > 0);
        assertNotNull(validatePipelineDefinitionResult.getValidationErrors().get(0));
        assertNotNull(validatePipelineDefinitionResult.getValidationWarnings());
        assertEquals(0, validatePipelineDefinitionResult.getValidationWarnings().size());

        // Valid field
        pipelineObject = new PipelineObject();
        pipelineObject.setId(OBJECT_ID);
        pipelineObject.setName(OBJECT_NAME);
        field = new Field();
        field.setKey(VALID_KEY);
        field.setStringValue(FIELD_VALUE);
        pipelineObject.setFields(Arrays.asList(field));

        // Validate pipeline definition.
        validatePipelineDefinitionResult = edp.validatePipelineDefinition(new ValidatePipelineDefinitionRequest().withPipelineId(pipelineId)
                .withPipelineObjects(pipelineObject));
        assertFalse(validatePipelineDefinitionResult.getErrored());
        assertNotNull(validatePipelineDefinitionResult.getValidationErrors());
        assertEquals(0, validatePipelineDefinitionResult.getValidationErrors().size());
        assertNotNull(validatePipelineDefinitionResult.getValidationWarnings());
        assertEquals(0, validatePipelineDefinitionResult.getValidationWarnings().size());

        // Put pipeline definition.
        PutPipelineDefinitionResult putPipelineDefinitionResult = edp.putPipelineDefinition(new PutPipelineDefinitionRequest().withPipelineId(pipelineId)
                .withPipelineObjects(pipelineObject));
        assertFalse(putPipelineDefinitionResult.getErrored());
        assertNotNull(putPipelineDefinitionResult.getValidationErrors());
        assertEquals(0, putPipelineDefinitionResult.getValidationErrors().size());
        assertNotNull(putPipelineDefinitionResult.getValidationWarnings());
        assertEquals(0, putPipelineDefinitionResult.getValidationWarnings().size());

        // Get pipeline definition.
        GetPipelineDefinitionResult getPipelineDefinitionResult = edp.getPipelineDefinition(new GetPipelineDefinitionRequest().withPipelineId(pipelineId));
        assertEquals(1, getPipelineDefinitionResult.getPipelineObjects().size());
        assertEquals(OBJECT_ID, getPipelineDefinitionResult.getPipelineObjects().get(0).getId());
        assertEquals(OBJECT_NAME, getPipelineDefinitionResult.getPipelineObjects().get(0).getName());
        assertEquals(1, getPipelineDefinitionResult.getPipelineObjects().get(0).getFields().size());
        assertTrue(getPipelineDefinitionResult.getPipelineObjects().get(0).getFields().contains(new Field().withKey(VALID_KEY).withStringValue(FIELD_VALUE)));

        // Activate a pipeline.
        ActivatePipelineResult activatePipelineResult = edp.activatePipeline(new ActivatePipelineRequest().withPipelineId(pipelineId));
        assertNotNull(activatePipelineResult);

        // List pipeline.
        ListPipelinesResult listPipelinesResult = edp.listPipelines();
        assertTrue(listPipelinesResult.getPipelineIdList().size() > 0);
        assertNotNull(pipelineId, listPipelinesResult.getPipelineIdList().get(0).getId());
        assertNotNull(PIPELINE_NAME, listPipelinesResult.getPipelineIdList().get(0).getName());

        Thread.sleep(1000 * 5);

        // Describe objects.
        DescribeObjectsResult describeObjectsResult = edp.describeObjects(new DescribeObjectsRequest().withPipelineId(pipelineId).withObjectIds(OBJECT_ID));
        assertEquals(1, describeObjectsResult.getPipelineObjects().size());
        assertEquals(OBJECT_ID, describeObjectsResult.getPipelineObjects().get(0).getId());
        assertEquals(OBJECT_NAME, describeObjectsResult.getPipelineObjects().get(0).getName());
        assertTrue(describeObjectsResult.getPipelineObjects().get(0).getFields().contains(new Field().withKey(VALID_KEY).withStringValue(FIELD_VALUE)));
        assertTrue(describeObjectsResult.getPipelineObjects().get(0).getFields().contains(new Field().withKey("@pipelineId").withStringValue(pipelineId)));

        // Describe a pipeline.
        DescribePipelinesResult describepipelinesResult = edp.describePipelines(new DescribePipelinesRequest().withPipelineIds(pipelineId));
        assertEquals(1, describepipelinesResult.getPipelineDescriptionList().size());
        assertEquals(PIPELINE_NAME, describepipelinesResult.getPipelineDescriptionList().get(0).getName());
        assertEquals(pipelineId, describepipelinesResult.getPipelineDescriptionList().get(0).getPipelineId());
        assertEquals(PIPELINE_DESCRIPTION, describepipelinesResult.getPipelineDescriptionList().get(0).getDescription());
        assertTrue(describepipelinesResult.getPipelineDescriptionList().get(0).getFields().size() > 0);
        assertTrue(describepipelinesResult.getPipelineDescriptionList().get(0).getFields().contains(new Field().withKey("name").withStringValue(PIPELINE_NAME)));
        assertTrue(describepipelinesResult.getPipelineDescriptionList().get(0).getFields().contains(new Field().withKey("@id").withStringValue(pipelineId)));
        assertTrue(describepipelinesResult.getPipelineDescriptionList().get(0).getFields().contains(new Field().withKey("uniqueId").withStringValue(PIPELINE_ID)));

        // Delete a pipeline.
        edp.deletePipeline(new DeletePipelineRequest().withPipelineId(pipelineId));
        Thread.sleep(1000 * 5);
        try {
            describepipelinesResult = edp.describePipelines(new DescribePipelinesRequest().withPipelineIds(pipelineId));
            if (describepipelinesResult.getPipelineDescriptionList().size() > 0) {
                fail();
            }
        } catch (AmazonServiceException e) {

        }
    }
}
