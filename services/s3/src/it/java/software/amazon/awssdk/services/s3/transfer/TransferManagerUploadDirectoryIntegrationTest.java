package software.amazon.awssdk.services.s3.transfer;

import software.amazon.awssdk.services.s3.AmazonS3;
import software.amazon.awssdk.services.s3.ObjectTaggingTestUtil;
import software.amazon.awssdk.services.s3.model.ObjectTagging;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResult;
import software.amazon.awssdk.services.s3.model.Tag;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static software.amazon.awssdk.services.s3.internal.Constants.KB;
import static org.mockito.Mockito.*;

/**
 * Tests for upload directory functionality.
 */
public class TransferManagerUploadDirectoryIntegrationTest extends TransferManagerTestBase {
    private static final String BUCKET = "bucket";

    private AmazonS3 mockS3;

    @BeforeClass
    public static void setUp() throws Exception {
        TransferManagerTestBase.setUp();
    }

    @Before
    public void methodSetUp() throws IOException {
        mockS3 = mock(AmazonS3.class);
        tm = TransferManagerBuilder.standard().withS3Client(mockS3).build();
        createTempDirectory();
        createTempFile(1 * KB);
    }

    @After
    public void methodTearDown() {
        deleteDirectoryAndItsContents(directory);
    }

    @Test
    public void delegatesToObjectTaggingProvider() throws InterruptedException, IOException {
        final Map<String, ObjectTagging> fileToTags = new HashMap<String,ObjectTagging>() {{
            put("foo", new ObjectTagging(Arrays.asList(new Tag("test-tag", "foo"))));
            put("bar", new ObjectTagging(Arrays.asList(new Tag("test-tag", "bar"))));
            put("baz", new ObjectTagging(Arrays.asList(new Tag("test-tag", "baz"))));

        }};

        List<File> fileList = new ArrayList<File>();
        for (String k : fileToTags.keySet()) {
            File f = new File(directory, k);
            FileUtils.copyFile(tempFile, new File(directory, k));
            fileList.add(f);
        }

        ObjectTaggingProvider taggingProvider = new ObjectTaggingProvider() {
            @Override
            public ObjectTagging provideObjectTags(UploadContext ctx) {
                return fileToTags.get(ctx.getFile().getName());
            }
        };

        when(mockS3.putObject(any(PutObjectRequest.class))).thenReturn(new PutObjectResult());

        MultipleFileUpload upload = tm.uploadFileList(BUCKET, "", directory, fileList, null, taggingProvider);
        upload.waitForCompletion();

        ArgumentCaptor<PutObjectRequest> putObjectCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(mockS3, atLeastOnce()).putObject(putObjectCaptor.capture());

        for (PutObjectRequest r : putObjectCaptor.getAllValues()) {
            ObjectTagging sentTags = r.getTagging();
            ObjectTaggingTestUtil.assertTagSetsAreEquals(
                    fileToTags.get(r.getKey()).getTagSet(),
                    sentTags.getTagSet());
        }
    }
}
