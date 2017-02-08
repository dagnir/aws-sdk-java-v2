package software.amazon.awssdk.services.ets;

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
    private final static String PIPE_NAME = "java-sdk-pipeline" + System.currentTimeMillis();
    private String pipelineId;
    private static String jobId;
    private PipelineOutputConfig contentConfig;
    private PipelineOutputConfig thumbnailConfig;
    private static String presetId;
    private AudioParameters audio;
    private String presetName;
    private String presetDescription;
    private static String topicArn;

    private final static String INPUT_BUCKET_NAME = "java-integration-test-input-bucket" + System.currentTimeMillis();
    private final static String OUTPUT_BUCKET_NAME = "java-integration-test-output-bucket";
    private final static String INPUT_KEY = "123";
    private final static String RATIO = "4:3";
    private final static String CONTAINER = "mp3";
    private final static String RATE = "15";
    private final static String RESOLUTION = "auto";
    private final static String INTERLACED = "auto";
    private final static String ROTATION = "270";
    private final static String OUTPUT_KEY = "321";


    private final String STORAGE_CLASS = "ReducedRedundancy";
    private final String ROLE = "arn:aws:iam::599169622985:role/role-java-data-pipeline-test";

    @BeforeClass
    public static void setUp() throws FileNotFoundException, IOException {

        IntegrationTestBase.setUp();


        ListPipelinesResult listPipelineResult = ets.listPipelines();
        for (Pipeline pipeline : listPipelineResult.getPipelines()) {
            if (pipeline.getName().startsWith("java-sdk-pipeline")) {
                try {
                    Thread.sleep(1000 * 1);
                    System.out.println(pipeline.getName());
                    ets.deletePipeline(new DeletePipelineRequest().withId(pipeline.getId()));

                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }
            }
        }
        s3.createBucket(INPUT_BUCKET_NAME);
        s3.createBucket(OUTPUT_BUCKET_NAME);
        String topicName = "java-sns-policy-integ-test-" + System.currentTimeMillis();
        topicArn = sns.createTopic(new CreateTopicRequest(topicName)).getTopicArn();
    }

    @AfterClass
    public static void teardown() {

        try {
          ets.cancelJob(new CancelJobRequest().withId(jobId));
        } catch(Exception e) {

        }

        try {
        ets.deletePreset(new DeletePresetRequest().withId(presetId));
        } catch(Exception e) {

        }

        ListPipelinesResult listPipelineResult = ets.listPipelines();
        for (Pipeline pipeline : listPipelineResult.getPipelines()) {
            if (pipeline.getName().startsWith("java-sdk-pipeline")) {
                try {
                    ets.deletePipeline(new DeletePipelineRequest().withId(pipeline.getId()));
                } catch (Exception e) {

                }
            }
        }

        try {s3.deleteBucket(INPUT_BUCKET_NAME);} catch (Exception e) {}

        try {s3.deleteBucket(OUTPUT_BUCKET_NAME);} catch (Exception e) {}

        try {
            sns.deleteTopic(new DeleteTopicRequest().withTopicArn(topicArn));
        } catch (Exception e) {
            // Do nothing
        }
    }

   @Test
   public void testAudioOnlyOperation() throws InterruptedException {
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

       Thread.sleep(1000 * 5);

        JobInput input = new JobInput()
                              .withKey(INPUT_KEY)
                              .withAspectRatio(RATIO)
                              .withContainer(CONTAINER)
                              .withFrameRate(RATE)
                              .withResolution(RESOLUTION)
                              .withInterlaced(INTERLACED);


        initializePresetParameters();



        // Create preset
        CreatePresetResult createPresetResult = ets.createPreset(new CreatePresetRequest()
                                        .withContainer("mp3")
                                        .withDescription(presetDescription)
                                        .withName(presetName)
                                        .withAudio(audio)
                                       );

        presetId = createPresetResult.getPreset().getId();

        Artwork artwork = new Artwork();
        artwork.setAlbumArtFormat("jpg");
        artwork.setMaxHeight("auto");
        artwork.setMaxWidth("auto");
        artwork.setPaddingPolicy("Pad");
        artwork.setSizingPolicy("Fit");
        artwork.setInputKey("test.jpg");

        JobAlbumArt albumArt = new JobAlbumArt();
        albumArt.setMergePolicy("Append");
        List<Artwork> artworks = new LinkedList<Artwork>();
        artworks.add(artwork);
        albumArt.setArtwork(artworks);

        final String startTime = "123.000";
        final String duration = "12345.000";
        TimeSpan timeSpan = new TimeSpan();
        timeSpan.setStartTime(startTime);
        timeSpan.setDuration(duration);
        Clip clip = new Clip();
        clip.setTimeSpan(timeSpan);

        CreateJobOutput output = new CreateJobOutput()
                                       .withKey(OUTPUT_KEY)
                                       .withAlbumArt(albumArt)
                                       .withThumbnailPattern("")
                                       .withPresetId(presetId)
                                       .withRotate(ROTATION)
                                       .withComposition(clip);

        List<CreateJobOutput> outputs = new LinkedList<CreateJobOutput>();
        outputs.add(output);


        // Create job
        CreateJobResult createJobResult = ets.createJob(new CreateJobRequest()
                                             .withPipelineId(pipelineId)
                                             .withInput(input)
                                             .withOutputs(outputs));

        jobId = createJobResult.getJob().getId();
        assertNotNull(jobId);
        assertEquals(pipelineId, createJobResult.getJob().getPipelineId());
        assertEquals(1, createJobResult.getJob().getOutputs().get(0).getComposition().size());
        assertEquals(startTime, createJobResult.getJob().getOutputs().get(0).getComposition().get(0).getTimeSpan().getStartTime());
        assertEquals(duration, createJobResult.getJob().getOutputs().get(0).getComposition().get(0).getTimeSpan().getDuration());

        ets.cancelJob(new CancelJobRequest().withId(jobId));

        // Read the job
        ReadJobResult readJobResult = ets.readJob(new ReadJobRequest().withId(jobId));

        assertEquals(jobId, readJobResult.getJob().getId());
        assertEquals(input, readJobResult.getJob().getInput());
        assertEquals(pipelineId, readJobResult.getJob().getPipelineId());
        assertEquals(albumArt, readJobResult.getJob().getOutputs().get(0).getAlbumArt());
        assertEquals(presetId, readJobResult.getJob().getOutputs().get(0).getPresetId());
        assertEquals(ROTATION, readJobResult.getJob().getOutputs().get(0).getRotate());
    }

    private void initializePresetParameters() {
        presetName = "my-preset";
        presetDescription = "my-preset";

        audio = new AudioParameters()
                     .withCodec("mp3")
                     .withSampleRate("auto")
                     .withBitRate("70")
                     .withChannels("auto");
    }
}

