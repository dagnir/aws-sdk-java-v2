package s3tests;

import static software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils.getTestKeyPair;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import org.junit.Test;

import software.amazon.awssdk.auth.AWSCredentials;
import software.amazon.awssdk.auth.PropertiesCredentials;
import software.amazon.awssdk.services.s3.AmazonS3;
import software.amazon.awssdk.services.s3.AmazonS3EncryptionClient;
import software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils;
import software.amazon.awssdk.services.s3.model.EncryptionMaterials;
import software.amazon.awssdk.services.s3.transfer.Download;
import software.amazon.awssdk.services.s3.transfer.TransferManager;
import software.amazon.awssdk.services.s3.transfer.TransferManagerConfiguration;
import software.amazon.awssdk.services.s3.transfer.Upload;

/**
 * See https://tt.amazon.com/0033479095
 * 
 * Here's test code that reproduces the problem as it happened to us.
 * 
 * If you change the number of threads from 10 to 1, then the test succeed.
 * 
 * The TransferManager has a test checking if AmazonS3 client that was passed in
 * the constructor is specifically an instance of AmazonS3EncryptionClient, to
 * revert to single threaded usage. But this test will not succeed if the client
 * was wrapped by libraries such as Spring AOP (or other oddities running in the
 * wild).
 * 
 * Our request would be to make it more clear that AmazonS3EncryptionClient is
 * not thread safe and, if possible to actually throw an exception if it's used
 * in an unsafe context.
 */
public class TestS3EncryptionClientMultithreaded {

    @Test
    public void testMultiThreadedUpload() throws Exception {
        File inputFile = createFileWithRandomContent(20 * 1024 * 1024);
        final AmazonS3EncryptionClient s3Client = createS3EncryptionClient();
        // In our service this was a Spring AOP defined proxy dealing with
        // retries
        AmazonS3 s3Proxy = (AmazonS3) Proxy.newProxyInstance(this.getClass()
                .getClassLoader(), new Class[] { AmazonS3.class },
                new InvocationHandler() {

                    @Override
                    public Object invoke(Object proxy, Method method,
                            Object[] args) throws Throwable {
                        // TODO Auto-generated method stub
                        return method.invoke(s3Client, args);
                    }
                });

        TransferManager mgr = new TransferManager(s3Proxy,
                (ThreadPoolExecutor) Executors.newFixedThreadPool(4));
        TransferManagerConfiguration tmgrConfig = new TransferManagerConfiguration();
        tmgrConfig.setMultipartUploadThreshold(1024);
        mgr.setConfiguration(tmgrConfig);
        Upload upload = mgr.upload(TestS3BucketName, inputFile.getName(),
                inputFile);
        upload.waitForCompletion();

        File outputFile = new File(inputFile.getParent(), inputFile.getName()
                + ".out");

        Download download = mgr.download(TestS3BucketName, inputFile.getName(),
                outputFile);
        download.waitForCompletion();

        assertFilesAreEqual(inputFile, outputFile);
    }

    private static void assertFilesAreEqual(File f1, File f2) throws Exception {
        BufferedInputStream is1 = null;
        BufferedInputStream is2 = null;
        try {
            if (f1.length() != f2.length())
                throw new RuntimeException("Files differ in length");
            is1 = new BufferedInputStream(new FileInputStream(f1), 2048);
            is2 = new BufferedInputStream(new FileInputStream(f2), 2048);
            int c1, c2;
            long pos = 0;
            do {
                c1 = is1.read();
                c2 = is2.read();
                if (c1 != c2)
                    throw new RuntimeException(
                            "The two files differ at byte offset: " + pos);
                pos++;
            } while ((c1 != -1) && (c2 != -1));
            // at the end of the loop both file should reach end of file
            // together
            if (c1 != -1)
                throw new IllegalStateException("Unexpected end of file: " + f1);
            if (c1 != -1)
                throw new IllegalStateException("Unexpected end of file: " + f2);

            System.out.println("Successfully verified " + (pos / 1024 / 1024)
                    + "mb");
        } finally {
            if (is1 != null)
                is1.close();
            if (is2 != null)
                is2.close();
        }
    }

    private File createFileWithRandomContent(int size) throws Exception {
        File result = File.createTempFile("testupload", ".txt",
                new File(System.getProperty("java.io.tmpdir", "/tmp")));
        byte[] bytes = new byte[size];
        new Random().nextBytes(bytes);
        // String base64= Base64.encodeBytes(bytes);
        FileOutputStream fOS = new FileOutputStream(result);
        fOS.write(bytes);
        fOS.close();
        return result;
    }

    public static AWSCredentials awsTestCredentials() throws IOException {
        return new PropertiesCredentials(new File(
                System.getProperty("user.home")
                        + "/.aws/awsTestAccount.properties"));
    }

    private AmazonS3EncryptionClient createS3EncryptionClient()
            throws Exception {
        // // OdinKeyPair keyPair = new OdinKeyPair(EncryptionMSName);
        // // AWSCredentialsProvider credentialProvider = new
        // OdinAWSCredentialsProvider(
        // // AccessMSName);
        // //
        // // OdinEncryptionMaterials keyPairMaterials = new
        // OdinEncryptionMaterials(
        // // keyPair);
        // EncryptionMaterialsProvider encryption = new
        // StaticEncryptionMaterialsProvider(
        // keyPairMaterials);
        // AmazonS3EncryptionClient s3Client = new AmazonS3EncryptionClient(
        // credentialProvider, encryption);
        EncryptionMaterials kekMaterial = new EncryptionMaterials(
                getTestKeyPair());
        AmazonS3EncryptionClient s3Client = new AmazonS3EncryptionClient(
                awsTestCredentials(), kekMaterial);

        // s3Client.setEndpoint(TestS3EndPoint);
        return s3Client;
    }

    // private static final String AccessMSName =
    // "com.amazon.aws.billpresentation.bdlis.backfill";
    // private static final String EncryptionMSName =
    // "com.amazon.aws.billing.awsb3.detailed_line_items.s3encryption.prod";
    // private static final String TestS3EndPoint =
    // "https://s3-external-1.amazonaws.com";
    // private static final String TestS3BucketName =
    // "amazon.aws.billpresentation.tests_useast";
    private static final String TestS3BucketName = CryptoTestUtils
            .tempBucketName(TestS3EncryptionClientMultithreaded.class);
}
