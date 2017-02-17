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

package software.amazon.awssdk.services.ets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.AmazonServiceException;
import software.amazon.awssdk.services.elastictranscoder.model.AudioCodecOptions;
import software.amazon.awssdk.services.elastictranscoder.model.AudioParameters;
import software.amazon.awssdk.services.elastictranscoder.model.CancelJobRequest;
import software.amazon.awssdk.services.elastictranscoder.model.CreateJobOutput;
import software.amazon.awssdk.services.elastictranscoder.model.CreateJobPlaylist;
import software.amazon.awssdk.services.elastictranscoder.model.CreateJobRequest;
import software.amazon.awssdk.services.elastictranscoder.model.CreateJobResult;
import software.amazon.awssdk.services.elastictranscoder.model.CreatePipelineRequest;
import software.amazon.awssdk.services.elastictranscoder.model.CreatePipelineResult;
import software.amazon.awssdk.services.elastictranscoder.model.CreatePresetRequest;
import software.amazon.awssdk.services.elastictranscoder.model.CreatePresetResult;
import software.amazon.awssdk.services.elastictranscoder.model.DeletePipelineRequest;
import software.amazon.awssdk.services.elastictranscoder.model.DeletePresetRequest;
import software.amazon.awssdk.services.elastictranscoder.model.Job;
import software.amazon.awssdk.services.elastictranscoder.model.JobInput;
import software.amazon.awssdk.services.elastictranscoder.model.JobWatermark;
import software.amazon.awssdk.services.elastictranscoder.model.ListJobsByPipelineRequest;
import software.amazon.awssdk.services.elastictranscoder.model.ListJobsByPipelineResult;
import software.amazon.awssdk.services.elastictranscoder.model.ListPipelinesResult;
import software.amazon.awssdk.services.elastictranscoder.model.Notifications;
import software.amazon.awssdk.services.elastictranscoder.model.Permission;
import software.amazon.awssdk.services.elastictranscoder.model.Pipeline;
import software.amazon.awssdk.services.elastictranscoder.model.PipelineOutputConfig;
import software.amazon.awssdk.services.elastictranscoder.model.Preset;
import software.amazon.awssdk.services.elastictranscoder.model.PresetWatermark;
import software.amazon.awssdk.services.elastictranscoder.model.ReadJobRequest;
import software.amazon.awssdk.services.elastictranscoder.model.ReadJobResult;
import software.amazon.awssdk.services.elastictranscoder.model.ReadPipelineRequest;
import software.amazon.awssdk.services.elastictranscoder.model.ReadPipelineResult;
import software.amazon.awssdk.services.elastictranscoder.model.ReadPresetRequest;
import software.amazon.awssdk.services.elastictranscoder.model.ReadPresetResult;
import software.amazon.awssdk.services.elastictranscoder.model.Thumbnails;
import software.amazon.awssdk.services.elastictranscoder.model.UpdatePipelineNotificationsRequest;
import software.amazon.awssdk.services.elastictranscoder.model.UpdatePipelineRequest;
import software.amazon.awssdk.services.elastictranscoder.model.UpdatePipelineResult;
import software.amazon.awssdk.services.elastictranscoder.model.UpdatePipelineStatusRequest;
import software.amazon.awssdk.services.elastictranscoder.model.VideoParameters;
import software.amazon.awssdk.services.sns.model.CreateTopicRequest;
import software.amazon.awssdk.services.sns.model.DeleteTopicRequest;

public class ETSIntegrationTest extends IntegrationTestBase {

    private static final String PIPE_NAME = "java-sdk-pipeline" + System.currentTimeMillis();
    private static final String INPUT_BUCKET_NAME = "java-integration-test-input-bucket" + System.currentTimeMillis();
    private static final String OUTPUT_BUCKET_NAME = "java-integration-test-output-bucket";
    private static final String INPUT_KEY = "123";
    private static final String RATIO = "4:3";
    private static final String CONTAINER = "auto";
    private static final String RATE = "15";
    private static final String RESOLUTION = "auto";
    private static final String INTERLACED = "auto";
    private static final String ROTATION = "270";
    private static final String OUTPUT_KEY = "321";
    private static final String SEGMENT_DURATION = "30.0";
    private static final String PLAY_LIST_NAME = "my-playlist";
    private static final String FORMAT = "HLSv3";
    private static final String WATERMARK_ID = "my-watermark";
    private static String topicArn;
    private static final String STORAGE_CLASS = "ReducedRedundancy";
    private static final String ROLE = "arn:aws:iam::369666460722:role/data-pipeline-test";
    private String pipelineId;
    private String jobId;
    private PipelineOutputConfig contentConfig;
    private PipelineOutputConfig thumbnailConfig;
    private String presetId;
    private AudioParameters audio;
    private VideoParameters video;
    private Thumbnails thumbnail;
    private String presetName;
    private String presetDescription;
    private CreateJobPlaylist createJobPlayList;
    private JobWatermark jobWatermark;

    @BeforeClass
    public static void setUp() throws FileNotFoundException, IOException {
        IntegrationTestBase.setUp();
        s3.createBucket(INPUT_BUCKET_NAME);
        s3.createBucket(OUTPUT_BUCKET_NAME);
        String topicName = "java-sns-policy-integ-test-" + System.currentTimeMillis();
        topicArn = sns.createTopic(new CreateTopicRequest(topicName)).getTopicArn();
    }

    @AfterClass
    public static void teardown() {
        ListPipelinesResult listPipelineResult = ets.listPipelines();
        for (Pipeline pipeline : listPipelineResult.getPipelines()) {
            if (pipeline.getName().startsWith("java-sdk-pipeline")) {
                try {
                    ets.deletePipeline(new DeletePipelineRequest().withId(pipeline.getId()));
                } catch (Exception e) {
                    // Ignored or expected.
                }
            }
        }

        try {
            s3.deleteBucket(INPUT_BUCKET_NAME);
        } catch (Exception e) {
            // Ignored or expected.
        }

        try {
            s3.deleteBucket(OUTPUT_BUCKET_NAME);
        } catch (Exception e) {
            // Ignored or expected.
        }

        try {
            sns.deleteTopic(new DeleteTopicRequest().withTopicArn(topicArn));
        } catch (Exception e) {
            // Do nothing
        }
    }


    @Test
    public void pipelineOperationIntegrationTest() throws InterruptedException {
        // Create Pipeline
        Notifications notifications = new Notifications();
        notifications.setCompleted("");
        notifications.setError("");
        notifications.setProgressing("");
        notifications.setWarning("");

        Permission permission = new Permission()
                .withGrantee("AllUsers")
                .withGranteeType("Group")
                .withAccess("Read");

        contentConfig = new PipelineOutputConfig()
                .withBucket(OUTPUT_BUCKET_NAME)
                .withStorageClass(STORAGE_CLASS)
                .withPermissions(permission);

        thumbnailConfig = new PipelineOutputConfig()
                .withBucket(OUTPUT_BUCKET_NAME)
                .withStorageClass(STORAGE_CLASS)
                .withPermissions(permission);

        CreatePipelineResult createPipelienResult = ets.createPipeline(
                new CreatePipelineRequest()
                        .withName(PIPE_NAME)
                        .withInputBucket(INPUT_BUCKET_NAME)
                        .withNotifications(notifications)
                        .withRole(ROLE)
                        .withThumbnailConfig(thumbnailConfig)
                        .withContentConfig(contentConfig));

        Pipeline pipeline = createPipelienResult.getPipeline();
        pipelineId = pipeline.getId();
        isValidPipeline(pipeline);

        Thread.sleep(1000 * 5);

        // List Pipeline
        ListPipelinesResult listPipelineResult = ets.listPipelines();
        assertTrue(listPipelineResult.getPipelines().size() > 0);
        listPipelineResult.getPipelines().contains(pipeline);

        // Get Pipeline
        ReadPipelineResult readPipelineResult = ets.readPipeline(new ReadPipelineRequest().withId(pipelineId));
        assertEquals(readPipelineResult.getPipeline(), pipeline);

        // Update pipleline, this should not remove the permissions in content
        // config since permission field is null instead of an empty array.
        PipelineOutputConfig newContentConfig = new PipelineOutputConfig()
                .withBucket(OUTPUT_BUCKET_NAME)
                .withStorageClass(STORAGE_CLASS);

        UpdatePipelineResult updatePipelineResult = ets.updatePipeline(new UpdatePipelineRequest()
                                                                               .withId(pipelineId)
                                                                               .withContentConfig(newContentConfig));

        isValidPipeline(updatePipelineResult.getPipeline());


        // Update pipleline, this should remove the permissions in content
        // config since permission field is null instead of an empty array.
        contentConfig.setPermissions(new ArrayList<Permission>());

        updatePipelineResult = ets.updatePipeline(new UpdatePipelineRequest()
                                                          .withId(pipelineId)
                                                          .withContentConfig(contentConfig));

        isValidPipeline(updatePipelineResult.getPipeline());

        // Get the pipeline back
        readPipelineResult = ets.readPipeline(new ReadPipelineRequest().withId(pipelineId));
        isValidPipeline(readPipelineResult.getPipeline());


        // Get a invalid pipeline to check the exception handling.
        try {
            readPipelineResult = ets.readPipeline(new ReadPipelineRequest().withId("fake"));
        } catch (AmazonServiceException e) {
            // The error type problem has not been fixed on the server side yet.
            assertNotNull(e.getErrorType());
            assertNotNull(e.getMessage());
        }

        // Update pipeline status
        ets.updatePipelineStatus(new UpdatePipelineStatusRequest().withId(pipelineId).withStatus("Paused"));

        // Get pipeline back
        readPipelineResult = ets.readPipeline(new ReadPipelineRequest().withId(pipelineId));
        isValidPipeline(readPipelineResult.getPipeline());
        assertEquals("Paused".toLowerCase(), readPipelineResult.getPipeline().getStatus().toLowerCase());


        // update pipeline notification
        notifications.setCompleted(topicArn);
        ets.updatePipelineNotifications(
                new UpdatePipelineNotificationsRequest().withId(pipelineId).withNotifications(notifications));

        // Get pipeline back
        readPipelineResult = ets.readPipeline(new ReadPipelineRequest().withId(pipelineId));
        isValidPipeline(readPipelineResult.getPipeline());
        assertEquals(topicArn, readPipelineResult.getPipeline().getNotifications().getCompleted());

        JobInput input = new JobInput()
                .withKey(INPUT_KEY)
                .withAspectRatio(RATIO)
                .withContainer(CONTAINER)
                .withFrameRate(RATE)
                .withResolution(RESOLUTION)
                .withInterlaced(INTERLACED);

        ets.listJobsByPipeline(new ListJobsByPipelineRequest().withPipelineId(pipelineId));

        initializePresetParameters();

        // Create preset
        CreatePresetResult createPresetResult = ets.createPreset(new CreatePresetRequest()
                                                                         .withContainer("ts")
                                                                         .withAudio(audio)
                                                                         .withDescription(presetDescription)
                                                                         .withVideo(video)
                                                                         .withName(presetName)
                                                                         .withThumbnails(thumbnail));

        isValidPreset(createPresetResult.getPreset());
        presetId = createPresetResult.getPreset().getId();

        // Read preset
        ReadPresetResult readPresetResult = ets.readPreset(new ReadPresetRequest().withId(presetId));
        assertEquals(createPresetResult.getPreset(), readPresetResult.getPreset());

        // Specify a waterwark for output1
        jobWatermark = new JobWatermark()
                .withInputKey("watermark.jpg")
                .withPresetWatermarkId(WATERMARK_ID);

        CreateJobOutput output1 = new CreateJobOutput()
                .withKey(OUTPUT_KEY)
                .withThumbnailPattern("")
                .withPresetId(presetId)
                .withRotate(ROTATION)
                .withSegmentDuration("10")
                .withSegmentDuration(SEGMENT_DURATION)
                .withWatermarks(jobWatermark);

        CreateJobOutput output2 = new CreateJobOutput()
                .withKey(OUTPUT_KEY + "new")
                .withThumbnailPattern("")
                .withPresetId(presetId)
                .withRotate(ROTATION)
                .withSegmentDuration(SEGMENT_DURATION);

        List<CreateJobOutput> outputs = new LinkedList<CreateJobOutput>();
        outputs.add(output1);
        outputs.add(output2);

        createJobPlayList = new CreateJobPlaylist()
                .withName(PLAY_LIST_NAME)
                .withFormat(FORMAT)
                .withOutputKeys(OUTPUT_KEY);

        // Create job
        CreateJobResult createJobResult = ets.createJob(new CreateJobRequest()
                                                                .withPipelineId(pipelineId)
                                                                .withInput(input)
                                                                .withOutputs(outputs)
                                                                .withPlaylists(createJobPlayList));

        jobId = createJobResult.getJob().getId();
        assertNotNull(jobId);
        assertEquals(pipelineId, createJobResult.getJob().getPipelineId());
        isValidJob(createJobResult.getJob());

        // Read the job
        ReadJobResult readJobResult = ets.readJob(new ReadJobRequest().withId(jobId));
        isValidJob(readJobResult.getJob());

        // List jobs
        ListJobsByPipelineResult listJobsByPipelineResult =
                ets.listJobsByPipeline(new ListJobsByPipelineRequest().withPipelineId(pipelineId));
        assertEquals(1, listJobsByPipelineResult.getJobs().size());
        isValidJob(listJobsByPipelineResult.getJobs().get(0));

        // Remove the job
        ets.cancelJob(new CancelJobRequest().withId(jobId));

        // Get the job again
        readJobResult = ets.readJob(new ReadJobRequest().withId(jobId));
        assertEquals(readJobResult.getJob().getOutput().getStatus().toLowerCase(), "canceled");

        // Remove pipeline
        ets.deletePipeline(new DeletePipelineRequest().withId(pipelineId));

        // Get the pipeline again
        try {
            readPipelineResult = ets.readPipeline(new ReadPipelineRequest().withId(pipelineId));
            fail();
        } catch (AmazonServiceException e) {
            // Ignored or expected.
        }

        // Delete preset
        ets.deletePreset(new DeletePresetRequest().withId(presetId));

        // Get the preset back
        try {
            ets.readPreset(new ReadPresetRequest().withId(presetId));
            fail();
        } catch (AmazonServiceException e) {
            // Ignored or expected.
        }

    }

    private void initializePresetParameters() {
        presetName = "my-preset";
        presetDescription = "my-preset";

        audio = new AudioParameters()
                .withCodec("AAC")
                .withSampleRate("auto")
                .withBitRate("70")
                .withChannels("auto")
                .withCodecOptions(new AudioCodecOptions().withProfile("AAC-LC"));

        Map<String, String> codecOptions = new HashMap<String, String>();
        codecOptions.put("Profile", "baseline");
        codecOptions.put("Level", "1");
        codecOptions.put("MaxReferenceFrames", "2");
        codecOptions.put("MaxBitRate", "10000");
        codecOptions.put("BufferSize", "100000");

        PresetWatermark watermark = new PresetWatermark()
                .withId(WATERMARK_ID)
                .withMaxHeight("20%")
                .withMaxWidth("20%")
                .withSizingPolicy("ShrinkToFit")
                .withHorizontalAlign("Left")
                .withHorizontalOffset("10%")
                .withVerticalAlign("Top")
                .withVerticalOffset("10%")
                .withOpacity("50")
                .withTarget("Content");

        video = new VideoParameters()
                .withCodecOptions(codecOptions)
                .withCodec("H.264")
                .withBitRate("auto")
                .withFrameRate("auto")
                .withMaxFrameRate("60")
                .withMaxHeight("auto")
                .withMaxWidth("auto")
                .withKeyframesMaxDist("1")
                .withSizingPolicy("Fit")
                .withPaddingPolicy("Pad")
                .withDisplayAspectRatio("auto")
                .withFixedGOP("true")
                .withCodecOptions(codecOptions)
                .withWatermarks(watermark);


        thumbnail = new Thumbnails()
                .withFormat("png")
                .withInterval("1")
                .withMaxHeight("auto")
                .withMaxWidth("auto")
                .withPaddingPolicy("Pad")
                .withSizingPolicy("Fit");
    }

    private void isValidJob(Job job) {
        assertEquals(pipelineId, job.getPipelineId());
        assertEquals(RATIO, job.getInput().getAspectRatio());
        assertEquals(CONTAINER, job.getInput().getContainer());
        assertEquals(RATE, job.getInput().getFrameRate());
        assertEquals(INTERLACED, job.getInput().getInterlaced());
        assertEquals(INPUT_KEY, job.getInput().getKey());
        assertEquals(RESOLUTION, job.getInput().getResolution());
        // Check create job ouput
        assertEquals(OUTPUT_KEY, job.getOutput().getKey());
        assertEquals(presetId, job.getOutput().getPresetId());
        assertEquals(ROTATION, job.getOutput().getRotate());
        // Check create job outputs
        assertEquals(2, job.getOutputs().size());
        assertEquals(OUTPUT_KEY, job.getOutputs().get(0).getKey());
        assertEquals(presetId, job.getOutputs().get(0).getPresetId());
        assertEquals(ROTATION, job.getOutputs().get(0).getRotate());
        assertEquals(SEGMENT_DURATION, job.getOutputs().get(0).getSegmentDuration());
        assertEquals(1, job.getOutputs().get(0).getWatermarks().size());
        assertEquals(jobWatermark, job.getOutputs().get(0).getWatermarks().get(0));
        assertEquals(OUTPUT_KEY + "new", job.getOutputs().get(1).getKey());
        assertEquals(presetId, job.getOutputs().get(1).getPresetId());
        assertEquals(ROTATION, job.getOutputs().get(1).getRotate());
        assertEquals(SEGMENT_DURATION, job.getOutputs().get(1).getSegmentDuration());
        assertEquals(1, job.getPlaylists().size());
        assertEquals(PLAY_LIST_NAME, job.getPlaylists().get(0).getName());
        assertEquals(FORMAT, job.getPlaylists().get(0).getFormat());
        assertEquals(OUTPUT_KEY, job.getPlaylists().get(0).getOutputKeys().get(0));
    }

    private void isValidPipeline(Pipeline pipeline) {
        assertEquals(pipelineId, pipeline.getId());
        assertEquals(PIPE_NAME, pipeline.getName());
        assertEquals(INPUT_BUCKET_NAME, pipeline.getInputBucket());
        assertEquals(contentConfig, pipeline.getContentConfig());
        assertEquals(thumbnailConfig, pipeline.getThumbnailConfig());
        assertNotNull(pipeline.getStatus());
        assertEquals(ROLE, pipeline.getRole());
    }

    private void isValidPreset(Preset preset) {
        assertNotNull(preset.getId());
        assertEquals(audio, preset.getAudio());
        assertEquals(video, preset.getVideo());
        assertEquals(presetName, preset.getName());
        assertEquals(presetDescription, preset.getDescription());
        assertEquals(thumbnail, preset.getThumbnails());
        assertEquals("ts", preset.getContainer());
    }
}
