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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.services.elastictranscoder.model.Artwork;
import software.amazon.awssdk.services.elastictranscoder.model.AudioParameters;
import software.amazon.awssdk.services.elastictranscoder.model.CancelJobRequest;
import software.amazon.awssdk.services.elastictranscoder.model.Clip;
import software.amazon.awssdk.services.elastictranscoder.model.CreateJobOutput;
import software.amazon.awssdk.services.elastictranscoder.model.CreateJobRequest;
import software.amazon.awssdk.services.elastictranscoder.model.CreateJobResult;
import software.amazon.awssdk.services.elastictranscoder.model.CreatePipelineRequest;
import software.amazon.awssdk.services.elastictranscoder.model.CreatePipelineResult;
import software.amazon.awssdk.services.elastictranscoder.model.CreatePresetRequest;
import software.amazon.awssdk.services.elastictranscoder.model.CreatePresetResult;
import software.amazon.awssdk.services.elastictranscoder.model.DeletePipelineRequest;
import software.amazon.awssdk.services.elastictranscoder.model.DeletePresetRequest;
import software.amazon.awssdk.services.elastictranscoder.model.JobAlbumArt;
import software.amazon.awssdk.services.elastictranscoder.model.JobInput;
import software.amazon.awssdk.services.elastictranscoder.model.ListPipelinesRequest;
import software.amazon.awssdk.services.elastictranscoder.model.ListPipelinesResult;
import software.amazon.awssdk.services.elastictranscoder.model.Notifications;
import software.amazon.awssdk.services.elastictranscoder.model.Permission;
import software.amazon.awssdk.services.elastictranscoder.model.Pipeline;
import software.amazon.awssdk.services.elastictranscoder.model.PipelineOutputConfig;
import software.amazon.awssdk.services.elastictranscoder.model.ReadJobRequest;
import software.amazon.awssdk.services.elastictranscoder.model.ReadJobResult;
import software.amazon.awssdk.services.elastictranscoder.model.TimeSpan;
import software.amazon.awssdk.services.sns.model.CreateTopicRequest;
import software.amazon.awssdk.services.sns.model.DeleteTopicRequest;

public class ETSAudioOnlyIntegrationTest extends IntegrationTestBase {
    private static final String PIPE_NAME = "java-sdk-pipeline" + System.currentTimeMillis();
    private static final String INPUT_BUCKET_NAME = "java-integration-test-input-bucket" + System.currentTimeMillis();
    private static final String OUTPUT_BUCKET_NAME = "java-integration-test-output-bucket";
    private static final String INPUT_KEY = "123";
    private static final String RATIO = "4:3";
    private static final String CONTAINER = "mp3";
    private static final String RATE = "15";
    private static final String RESOLUTION = "auto";
    private static final String INTERLACED = "auto";
    private static final String ROTATION = "270";
    private static final String OUTPUT_KEY = "321";
    private static String jobId;
    private static String presetId;
    private static String topicArn;
    private static final String STORAGE_CLASS = "ReducedRedundancy";
    private static final String ROLE = "arn:aws:iam::599169622985:role/role-java-data-pipeline-test";
    private String pipelineId;
    private PipelineOutputConfig contentConfig;
    private PipelineOutputConfig thumbnailConfig;
    private AudioParameters audio;
    private String presetName;
    private String presetDescription;

    @BeforeClass
    public static void setUp() throws FileNotFoundException, IOException {

        IntegrationTestBase.setUp();


        ListPipelinesResult listPipelineResult = ets.listPipelines(ListPipelinesRequest.builder().build());
        for (Pipeline pipeline : listPipelineResult.pipelines()) {
            if (pipeline.name().startsWith("java-sdk-pipeline")) {
                try {
                    Thread.sleep(1000);
                    System.out.println(pipeline.name());
                    ets.deletePipeline(DeletePipelineRequest.builder().id(pipeline.id()).build());

                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }
            }
        }
        s3.createBucket(INPUT_BUCKET_NAME);
        s3.createBucket(OUTPUT_BUCKET_NAME);
        String topicName = "java-sns-policy-integ-test-" + System.currentTimeMillis();
        topicArn = sns.createTopic(CreateTopicRequest.builder().name(topicName).build()).topicArn();
    }

    @AfterClass
    public static void teardown() {

        try {
            ets.cancelJob(CancelJobRequest.builder().id(jobId).build());
        } catch (Exception e) {
            // Ignored or expected.
        }

        try {
            ets.deletePreset(DeletePresetRequest.builder().id(presetId).build());
        } catch (Exception e) {
            // Ignored or expected.
        }

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
    public void testAudioOnlyOperation() throws InterruptedException {
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

        Thread.sleep(1000 * 5);


        initializePresetParameters();


        // Create preset
        CreatePresetResult createPresetResult = ets.createPreset(CreatePresetRequest.builder()
                                                                                    .container("mp3")
                                                                                    .description(presetDescription)
                                                                                    .name(presetName)
                                                                                    .audio(audio).build());

        presetId = createPresetResult.preset().id();

        Artwork.Builder artwork = Artwork.builder();
        artwork.albumArtFormat("jpg");
        artwork.maxHeight("auto");
        artwork.maxWidth("auto");
        artwork.paddingPolicy("Pad");
        artwork.sizingPolicy("Fit");
        artwork.inputKey("test.jpg");

        JobAlbumArt.Builder albumArt = JobAlbumArt.builder();
        albumArt.mergePolicy("Append");
        List<Artwork> artworks = new LinkedList<Artwork>();
        artworks.add(artwork.build());
        albumArt.artwork(artworks);

        final String startTime = "123.000";
        final String duration = "12345.000";
        TimeSpan.Builder timeSpan = TimeSpan.builder();
        timeSpan.startTime(startTime);
        timeSpan.duration(duration);
        Clip.Builder clip = Clip.builder();
        clip.timeSpan(timeSpan.build());

        CreateJobOutput output = CreateJobOutput.builder()
                                                .key(OUTPUT_KEY)
                                                .albumArt(albumArt.build())
                                                .thumbnailPattern("")
                                                .presetId(presetId)
                                                .rotate(ROTATION)
                                                .composition(clip.build()).build();

        List<CreateJobOutput> outputs = new LinkedList<CreateJobOutput>();
        outputs.add(output);


        // Create job
        JobInput input = JobInput.builder()
                                 .key(INPUT_KEY)
                                 .aspectRatio(RATIO)
                                 .container(CONTAINER)
                                 .frameRate(RATE)
                                 .resolution(RESOLUTION)
                                 .interlaced(INTERLACED).build();

        CreateJobResult createJobResult = ets.createJob(CreateJobRequest.builder()
                                                                        .pipelineId(pipelineId)
                                                                        .input(input)
                                                                        .outputs(outputs).build());

        jobId = createJobResult.job().id();
        assertNotNull(jobId);
        assertEquals(pipelineId, createJobResult.job().pipelineId());
        assertEquals(1, createJobResult.job().outputs().get(0).composition().size());
        assertEquals(startTime, createJobResult.job().outputs().get(0).composition().get(0).timeSpan().startTime());
        assertEquals(duration, createJobResult.job().outputs().get(0).composition().get(0).timeSpan().duration());

        ets.cancelJob(CancelJobRequest.builder().id(jobId).build());

        // Read the job
        ReadJobResult readJobResult = ets.readJob(ReadJobRequest.builder().id(jobId).build());

        assertEquals(jobId, readJobResult.job().id());
        assertEquals(input, readJobResult.job().input());
        assertEquals(pipelineId, readJobResult.job().pipelineId());
        assertEquals(albumArt, readJobResult.job().outputs().get(0).albumArt());
        assertEquals(presetId, readJobResult.job().outputs().get(0).presetId());
        assertEquals(ROTATION, readJobResult.job().outputs().get(0).rotate());
    }

    private void initializePresetParameters() {
        presetName = "my-preset";
        presetDescription = "my-preset";

        audio = AudioParameters.builder()
                               .codec("mp3")
                               .sampleRate("auto")
                               .bitRate("70")
                               .channels("auto").build();
    }
}

