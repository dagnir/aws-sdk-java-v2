package software.amazon.awssdk.services.s3.util;

import java.io.IOException;
import org.junit.Test;
import software.amazon.awssdk.services.kms.AWSKMSClient;
import software.amazon.awssdk.services.kms.model.CreateKeyResult;
import software.amazon.awssdk.services.s3.S3IntegrationTestBase;
import software.amazon.awssdk.services.s3.internal.crypto.CryptoRuntime;

public class KMSCreateKeyIntegrationTest extends S3IntegrationTestBase {

    @Test
    public void test() throws IOException {
        if (true)
            return;

        // System.setProperty("software.amazon.awssdk.sdk.disableCertChecking", "false");
        // This is necessary to make it work under JDK1.6
        // http://www.masterthought.net/section/blog_article/article/connecting-to-db2-with-java-sun-jdk-1-6-from-osx#.VECRp4vF8ik
        CryptoRuntime.enableBouncyCastle();
        setUpCredentials();

        AWSKMSClient kms = new AWSKMSClient(credentials);
//        kms.setEndpoint(KMS_END_POINT);
//        kms.setServiceNameIntern(KMS_SERVICE_NAME);
        CreateKeyResult result = kms.createKey();
        System.err.println(result);
        // {KeyMetadata: {AWSAccountId: 814230673633,KeyId: d3fd2273-4ca0-4da5-b1c2-c89742ec6a26,Arn: arn:aws:trent-sandbox:us-west-2:814230673633:key/d3fd2273-4ca0-4da5-b1c2-c89742ec6a26,CreationDate: Tue Oct 14 11:30:35 PDT 2014,Enabled: true,Description: ,KeyUsage: ENCRYPT_DECRYPT}}
        // {KeyMetadata: {AWSAccountId: 814230673633,KeyId: 50ec4214-f829-483b-9def-fb95aba9bfd2,Arn: arn:aws:trent-sandbox:us-west-2:814230673633:key/50ec4214-f829-483b-9def-fb95aba9bfd2,CreationDate: Thu Oct 16 17:43:53 PDT 2014,Enabled: true,Description: ,KeyUsage: ENCRYPT_DECRYPT}}
        kms.shutdown();
    }

    // {KeyMetadata: {AWSAccountId: 814230673633,KeyId: a986ff87-7dcc-4726-8275-9356a465533a,Arn: arn:aws:kms:us-east-1:814230673633:key/a986ff87-7dcc-4726-8275-9356a465533a,CreationDate: Tue Nov 11 15:15:13 PST 2014,Enabled: true,Description: ,KeyUsage: ENCRYPT_DECRYPT}}
    // {KeyMetadata: {AWSAccountId: 814230673633,KeyId: 927c6ab6-8fe6-418c-bfa1-ce19dfb20171,Arn: arn:aws:kms:us-east-1:814230673633:key/927c6ab6-8fe6-418c-bfa1-ce19dfb20171,CreationDate: Tue Nov 11 15:17:21 PST 2014,Enabled: true,Description: ,KeyUsage: ENCRYPT_DECRYPT}}
}
