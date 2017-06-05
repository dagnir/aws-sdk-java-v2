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

package software.amazon.awssdk.services.elastictranscoder;

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
import software.amazon.awssdk.services.elastictranscoder.model.ListPipelinesRequest;
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
        topicArn = sns.createTopic(CreateTopicRequest.builder().name(topicName).build()).topicArn();
    }

    @AfterClass
    public static void teardown() {
        ListPipelinesResult listPipelineResult = ets.listPipelines(ListPipelinesRequest.builder().build());
        for (Pipeline pipeline : listPipelineResult.pipelines()) {
            if (pipeline.name().startsWith("java-sdk-pipeline")) {
                try {
                    ets.deletePipeline(DeletePipelineRequest.builder().id(pipeline.id()).build());
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
            sns.deleteTopic(DeleteTopicRequest.builder().topicArn(topicArn).build());
        } catch (Exception e) {
            // Do nothing
        }
    }


    @Test
    public void pipelineOperationIntegrationTest() throws InterruptedException {
        // Create Pipeline
        Notifications.Builder notifications = Notifications.builder();
        notifications.completed("");
        notifications.error("");
        notifications.progressing("");
        notifications.warning("");

        Permission permission = Permission.builder()
                .grantee("AllUsers")
                .granteeType("Group")
                .access("Read").build();

        contentConfig = PipelineOutputConfig.builder()
                .bucket(OUTPUT_BUCKET_NAME)
                .storageClass(STORAGE_CLASS)
                .permissions(permission).build();

        thumbnailConfig = PipelineOutputConfig.builder()
                .bucket(OUTPUT_BUCKET_NAME)
                .storageClass(STORAGE_CLASS)
                .permissions(permission).build();

        CreatePipelineResult createPipelienResult = ets.createPipeline(
                CreatePipelineRequest.builder()
                        .name(PIPE_NAME)
                        .inputBucket(INPUT_BUCKET_NAME)
                        .notifications(notifications.build())
                        .role(ROLE)
                        .thumbnailConfig(thumbnailConfig)
                        .contentConfig(contentConfig).build());

        Pipeline pipeline = createPipelienResult.pipeline();
        pipelineId = pipeline.id();
        isValidPipeline(pipeline);

        Thread.sleep(1000 * 5);

        // List Pipeline
        ListPipelinesResult listPipelineResult = ets.listPipelines(ListPipelinesRequest.builder().build());
        assertTrue(listPipelineResult.pipelines().size() > 0);
        listPipelineResult.pipelines().contains(pipeline);

        // Get Pipeline
        ReadPipelineResult readPipelineResult = ets.readPipeline(ReadPipelineRequest.builder().id(pipelineId).build());
        assertEquals(readPipelineResult.pipeline(), pipeline);

        // Update pipleline, this should not remove the permissions in content
        // config since permission field is null instead of an empty array.
        PipelineOutputConfig newContentConfig = PipelineOutputConfig.builder()
                .bucket(OUTPUT_BUCKET_NAME)
                .storageClass(STORAGE_CLASS).build();

        UpdatePipelineResult updatePipelineResult = ets.updatePipeline(UpdatePipelineRequest.builder()
                                                                               .id(pipelineId)
                                                                               .contentConfig(newContentConfig).build());

        isValidPipeline(updatePipelineResult.pipeline());


        // Update pipleline, this should remove the permissions in content
        // config since permission field is null instead of an empty array.

        contentConfig = contentConfig.toBuilder().permissions(new ArrayList<>()).build();

        updatePipelineResult = ets.updatePipeline(UpdatePipelineRequest.builder()
                                                          .id(pipelineId)
                                                          .contentConfig(contentConfig).build());

        isValidPipeline(updatePipelineResult.pipeline());

        // Get the pipeline back
        readPipelineResult = ets.readPipeline(ReadPipelineRequest.builder().id(pipelineId).build());
        isValidPipeline(readPipelineResult.pipeline());


        // Get a invalid pipeline to check the exception handling.
        try {
            readPipelineResult = ets.readPipeline(ReadPipelineRequest.builder().id("fake").build());
        } catch (AmazonServiceException e) {
            // The error type problem has not been fixed on the server side yet.
            assertNotNull(e.getErrorType());
            assertNotNull(e.getMessage());
        }

        // Update pipeline status
        ets.updatePipelineStatus(UpdatePipelineStatusRequest.builder().id(pipelineId).status("Paused").build());

        // Get pipeline back
        readPipelineResult = ets.readPipeline(ReadPipelineRequest.builder().id(pipelineId).build());
        isValidPipeline(readPipelineResult.pipeline());
        assertEquals("Paused".toLowerCase(), readPipelineResult.pipeline().status().toLowerCase());


        // update pipeline notification
        notifications.completed(topicArn);
        ets.updatePipelineNotifications(
                UpdatePipelineNotificationsRequest.builder().id(pipelineId).notifications(notifications.build()).build());

        // Get pipeline back
        readPipelineResult = ets.readPipeline(ReadPipelineRequest.builder().id(pipelineId).build());
        isValidPipeline(readPipelineResult.pipeline());
        assertEquals(topicArn, readPipelineResult.pipeline().notifications().completed());

        JobInput input = JobInput.builder()
                .key(INPUT_KEY)
                .aspectRatio(RATIO)
                .container(CONTAINER)
                .frameRate(RATE)
                .resolution(RESOLUTION)
                .interlaced(INTERLACED).build();

        ets.listJobsByPipeline(ListJobsByPipelineRequest.builder().pipelineId(pipelineId).build());

        initializePresetParameters();

        // Create preset
        CreatePresetResult createPresetResult = ets.createPreset(CreatePresetRequest.builder()
                                                                         .container("ts")
                                                                         .audio(audio)
                                                                         .description(presetDescription)
                                                                         .video(video)
                                                                         .name(presetName)
                                                                         .thumbnails(thumbnail).build());

        isValidPreset(createPresetResult.preset());
        presetId = createPresetResult.preset().id();

        // Read preset
        ReadPresetResult readPresetResult = ets.readPreset(ReadPresetRequest.builder().id(presetId).build());
        assertEquals(createPresetResult.preset(), readPresetResult.preset());

        // Specify a waterwark for output1
        jobWatermark = JobWatermark.builder()
                .inputKey("watermark.jpg")
                .presetWatermarkId(WATERMARK_ID).build();

        CreateJobOutput output1 = CreateJobOutput.builder()
                .key(OUTPUT_KEY)
                .thumbnailPattern("")
                .presetId(presetId)
                .rotate(ROTATION)
                .segmentDuration("10")
                .segmentDuration(SEGMENT_DURATION)
                .watermarks(jobWatermark).build();

        CreateJobOutput output2 = CreateJobOutput.builder()
                .key(OUTPUT_KEY + "new")
                .thumbnailPattern("")
                .presetId(presetId)
                .rotate(ROTATION)
                .segmentDuration(SEGMENT_DURATION).build();

        List<CreateJobOutput> outputs = new LinkedList<>();
        outputs.add(output1);
        outputs.add(output2);

        createJobPlayList = CreateJobPlaylist.builder()
                .name(PLAY_LIST_NAME)
                .format(FORMAT)
                .outputKeys(OUTPUT_KEY).build();

        // Create job
        CreateJobResult createJobResult = ets.createJob(CreateJobRequest.builder()
                                                                .pipelineId(pipelineId)
                                                                .input(input)
                                                                .outputs(outputs)
                                                                .playlists(createJobPlayList).build());

        jobId = createJobResult.job().id();
        assertNotNull(jobId);
        assertEquals(pipelineId, createJobResult.job().pipelineId());
        isValidJob(createJobResult.job());

        // Read the job
        ReadJobResult readJobResult = ets.readJob(ReadJobRequest.builder().id(jobId).build());
        isValidJob(readJobResult.job());

        // List jobs
        ListJobsByPipelineResult listJobsByPipelineResult =
                ets.listJobsByPipeline(ListJobsByPipelineRequest.builder().pipelineId(pipelineId).build());
        assertEquals(1, listJobsByPipelineResult.jobs().size());
        isValidJob(listJobsByPipelineResult.jobs().get(0));

        // Remove the job
        ets.cancelJob(CancelJobRequest.builder().id(jobId).build());

        // Get the job again
        readJobResult = ets.readJob(ReadJobRequest.builder().id(jobId).build());
        assertEquals(readJobResult.job().output().status().toLowerCase(), "canceled");

        // Remove pipeline
        ets.deletePipeline(DeletePipelineRequest.builder().id(pipelineId).build());

        // Get the pipeline again
        try {
            readPipelineResult = ets.readPipeline(ReadPipelineRequest.builder().id(pipelineId).build());
            fail();
        } catch (AmazonServiceException e) {
            // Ignored or expected.
        }

        // Delete preset
        ets.deletePreset(DeletePresetRequest.builder().id(presetId).build());

        // Get the preset back
        try {
            ets.readPreset(ReadPresetRequest.builder().id(presetId).build());
            fail();
        } catch (AmazonServiceException e) {
            // Ignored or expected.
        }

    }

    private void initializePresetParameters() {
        presetName = "my-preset";
        presetDescription = "my-preset";

        audio = AudioParameters.builder()
                .codec("AAC")
                .sampleRate("auto")
                .bitRate("70")
                .channels("auto")
                .codecOptions(AudioCodecOptions.builder().profile("AAC-LC").build()).build();

        Map<String, String> codecOptions = new HashMap<String, String>();
        codecOptions.put("Profile", "baseline");
        codecOptions.put("Level", "1");
        codecOptions.put("MaxReferenceFrames", "2");
        codecOptions.put("MaxBitRate", "10000");
        codecOptions.put("BufferSize", "100000");

        PresetWatermark watermark = PresetWatermark.builder()
                .id(WATERMARK_ID)
                .maxHeight("20%")
                .maxWidth("20%")
                .sizingPolicy("ShrinkToFit")
                .horizontalAlign("Left")
                .horizontalOffset("10%")
                .verticalAlign("Top")
                .verticalOffset("10%")
                .opacity("50")
                .target("Content").build();

        video = VideoParameters.builder()
                .codecOptions(codecOptions)
                .codec("H.264")
                .bitRate("auto")
                .frameRate("auto")
                .maxFrameRate("60")
                .maxHeight("auto")
                .maxWidth("auto")
                .keyframesMaxDist("1")
                .sizingPolicy("Fit")
                .paddingPolicy("Pad")
                .displayAspectRatio("auto")
                .fixedGOP("true")
                .codecOptions(codecOptions)
                .watermarks(watermark).build();


        thumbnail = Thumbnails.builder()
                .format("png")
                .interval("1")
                .maxHeight("auto")
                .maxWidth("auto")
                .paddingPolicy("Pad")
                .sizingPolicy("Fit").build();
    }

    private void isValidJob(Job job) {
        assertEquals(pipelineId, job.pipelineId());
        assertEquals(RATIO, job.input().aspectRatio());
        assertEquals(CONTAINER, job.input().container());
        assertEquals(RATE, job.input().frameRate());
        assertEquals(INTERLACED, job.input().interlaced());
        assertEquals(INPUT_KEY, job.input().key());
        assertEquals(RESOLUTION, job.input().resolution());
        // Check create job ouput
        assertEquals(OUTPUT_KEY, job.output().key());
        assertEquals(presetId, job.output().presetId());
        assertEquals(ROTATION, job.output().rotate());
        // Check create job outputs
        assertEquals(2, job.outputs().size());
        assertEquals(OUTPUT_KEY, job.outputs().get(0).key());
        assertEquals(presetId, job.outputs().get(0).presetId());
        assertEquals(ROTATION, job.outputs().get(0).rotate());
        assertEquals(SEGMENT_DURATION, job.outputs().get(0).segmentDuration());
        assertEquals(1, job.outputs().get(0).watermarks().size());
        assertEquals(jobWatermark, job.outputs().get(0).watermarks().get(0));
        assertEquals(OUTPUT_KEY + "new", job.outputs().get(1).key());
        assertEquals(presetId, job.outputs().get(1).presetId());
        assertEquals(ROTATION, job.outputs().get(1).rotate());
        assertEquals(SEGMENT_DURATION, job.outputs().get(1).segmentDuration());
        assertEquals(1, job.playlists().size());
        assertEquals(PLAY_LIST_NAME, job.playlists().get(0).name());
        assertEquals(FORMAT, job.playlists().get(0).format());
        assertEquals(OUTPUT_KEY, job.playlists().get(0).outputKeys().get(0));
    }

    private void isValidPipeline(Pipeline pipeline) {
        assertEquals(pipelineId, pipeline.id());
        assertEquals(PIPE_NAME, pipeline.name());
        assertEquals(INPUT_BUCKET_NAME, pipeline.inputBucket());
        assertEquals(contentConfig, pipeline.contentConfig());
        assertEquals(thumbnailConfig, pipeline.thumbnailConfig());
        assertNotNull(pipeline.status());
        assertEquals(ROLE, pipeline.role());
    }

    private void isValidPreset(Preset preset) {
        assertNotNull(preset.id());
        assertEquals(audio, preset.audio());
        assertEquals(video, preset.video());
        assertEquals(presetName, preset.name());
        assertEquals(presetDescription, preset.description());
        assertEquals(thumbnail, preset.thumbnails());
        assertEquals("ts", preset.container());
    }
}
