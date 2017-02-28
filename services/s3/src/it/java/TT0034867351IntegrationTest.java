import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import software.amazon.awssdk.services.s3.AmazonS3Client;
import software.amazon.awssdk.services.s3.categories.S3Categories;
import software.amazon.awssdk.services.s3.model.S3Object;

@Category(S3Categories.Slow.class)
public class TT0034867351IntegrationTest {
    private static AmazonS3Client s3;

    @BeforeClass
    public static void setup() throws Exception {
        s3 = new AmazonS3Client();
    }

    @AfterClass
    public static void tearDown() {
        s3.shutdown();
    }

    @Test
    public void test() throws Exception {
        final String bucket = "xingwu-emr-dev";
        final String key = "public/tmp/event-prod-dmp01.s3Sink1.1405448123352.gz";
        S3Object object = s3.getObject(bucket, key);
        GZIPInputStream is = new GZIPInputStream(object.getObjectContent());
        try {
            byte[] buffer = new byte[1024 * 1024];
            int len;
            long total = 0;
            while (true) {
                len = is.read(buffer);
                if (len < 0) {
                    break;
                }
                total += len;
            }
            assertTrue(178688428, total);
        } finally {
            is.close();
        }
    }

    private void assertTrue(long expected, long actual) {
        Assert.assertTrue("expected: " + expected + " but actual: " + actual, expected == actual);
    }

    @Test
    public void testDownloadGZFile() throws Exception {
        final String bucket = "xingwu-emr-dev";
        final String key = "public/tmp/event-prod-dmp01.s3Sink1.1405448123352.gz";
        S3Object object = s3.getObject(bucket, key);
        byte[] data = IOUtils.toByteArray(object.getObjectContent());
        File destFile = new File("/tmp/event-prod-dmp01.s3Sink1.1405448123352.gz");
        FileUtils.writeByteArrayToFile(destFile, data);
        assertTrue(16507003, destFile.length());
    }

    @Test
    public void testDownloadGZFile_Unzip() throws Exception {
      final String bucket = "xingwu-emr-dev";
      final String key = "public/tmp/event-prod-dmp01.s3Sink1.1405448123352.gz";
      S3Object object = s3.getObject(bucket, key);
      InputStream is = object.getObjectContent();
      byte[] data = IOUtils.toByteArray(new GZIPInputStream(is));
      File destFile = new File("/tmp/event-prod-dmp01.s3Sink1.1405448123352");
      FileUtils.writeByteArrayToFile(destFile, data);
      assertTrue(178688428, destFile.length());
   }

    // A local experiment
//    @Test
    public void testUnzip() throws Exception {
        byte[] data = IOUtils.toByteArray(new GZIPInputStream(new FileInputStream("/tmp/event-prod-dmp01.s3Sink1.1405448123352.gz")));
        File destFile = new File("/tmp/event-prod-dmp01.s3Sink1.1405448123352.unzipFromLocalFile");
        FileUtils.writeByteArrayToFile(destFile, data);
        assertTrue(178688428, destFile.length());
    }
}
