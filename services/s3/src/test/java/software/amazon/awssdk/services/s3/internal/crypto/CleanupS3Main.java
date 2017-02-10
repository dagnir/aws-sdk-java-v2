package software.amazon.awssdk.services.s3.internal.crypto;

import java.io.File;
import java.io.IOException;
import java.util.List;
import software.amazon.awssdk.auth.AWSCredentials;
import software.amazon.awssdk.auth.PropertiesCredentials;
import software.amazon.awssdk.services.s3.AmazonS3Client;
import software.amazon.awssdk.services.s3.model.AmazonS3Exception;
import software.amazon.awssdk.services.s3.model.Bucket;

public class CleanupS3Main {
    /** Returns the test AWS credential. */
    private static AWSCredentials awsCredentials() throws IOException {
        return new PropertiesCredentials(new File(
                "/Users/hchar/.aws/awsTestAccount.properties"));
//    "/Users/hchar/.aws/awsTestAccount.properties-glacier"));
    }

    public static void main(String[] args) throws Exception {
        AmazonS3Client s3 = new AmazonS3Client(awsCredentials());
        List<Bucket> list = s3.listBuckets();
        for (Bucket b: list) {
            String name = b.getName();
            if (!name.startsWith("hanson") && !name.startsWith("hchar")) {
                try {
                CryptoTestUtils.deleteBucketAndAllContents(s3, name);
                } catch(AmazonS3Exception ex) {
                    System.err.println("bucket: " + name);
                    ex.printStackTrace(System.err);
                }
            }
        }
    }
}
