package software.amazon.awssdk.services.s3.util;

import java.io.IOException;
import org.junit.Test;
import software.amazon.awssdk.services.kms.AWSKMSClient;
import software.amazon.awssdk.services.kms.model.ListKeysResult;
import software.amazon.awssdk.services.s3.S3IntegrationTestBase;
import software.amazon.awssdk.services.s3.internal.crypto.CryptoRuntime;

public class KMSListKeyIntegrationTest extends S3IntegrationTestBase {

    @Test
    public void test() throws IOException {
    	setUpCredentials();
        // System.setProperty("software.amazon.awssdk.sdk.disableCertChecking", "false");
        // This is necessary to make it work under JDK1.6
        // http://www.masterthought.net/section/blog_article/article/connecting-to-db2-with-java-sun-jdk-1-6-from-osx#.VECRp4vF8ik
        CryptoRuntime.enableBouncyCastle();

        AWSKMSClient kms = new AWSKMSClient(credentials);
//        kms.setEndpoint(KMS_END_POINT);
//        kms.setServiceNameIntern(KMS_SERVICE_NAME);
        ListKeysResult result = kms.listKeys();
        System.err.println(result);
        // {KeyMetadata: {AWSAccountId: 814230673633,KeyId: d3fd2273-4ca0-4da5-b1c2-c89742ec6a26,Arn: arn:aws:trent-sandbox:us-west-2:814230673633:key/d3fd2273-4ca0-4da5-b1c2-c89742ec6a26,CreationDate: Tue Oct 14 11:30:35 PDT 2014,Enabled: true,Description: ,KeyUsage: ENCRYPT_DECRYPT}}

        // {Keys: [{KeyId: 50ec4214-f829-483b-9def-fb95aba9bfd2,KeyArn: arn:aws:trent-sandbox:us-west-2:814230673633:key/50ec4214-f829-483b-9def-fb95aba9bfd2,AliasList: []}, {KeyId: d3fd2273-4ca0-4da5-b1c2-c89742ec6a26,KeyArn: arn:aws:trent-sandbox:us-west-2:814230673633:key/d3fd2273-4ca0-4da5-b1c2-c89742ec6a26,AliasList: []}],Truncated: false}
        kms.shutdown();
    }
// {Keys: [{KeyId: 014154c2-f4b4-4330-a7c8-ddcc202e0f6f,KeyArn: arn:aws:kms:us-east-1:814230673633:key/014154c2-f4b4-4330-a7c8-ddcc202e0f6f}, {KeyId: 195c75ad-38e2-4c84-91c2-5619f5292865,KeyArn: arn:aws:kms:us-east-1:814230673633:key/195c75ad-38e2-4c84-91c2-5619f5292865}, {KeyId: 4743d169-0444-44a2-9675-b787648d3b97,KeyArn: arn:aws:kms:us-east-1:814230673633:key/4743d169-0444-44a2-9675-b787648d3b97}, {KeyId: 6bd93d47-6180-47d2-aaf7-c946cff86570,KeyArn: arn:aws:kms:us-east-1:814230673633:key/6bd93d47-6180-47d2-aaf7-c946cff86570}, {KeyId: 98a57b60-2f5a-47c9-adc9-180fe2209b98,KeyArn: arn:aws:kms:us-east-1:814230673633:key/98a57b60-2f5a-47c9-adc9-180fe2209b98}, {KeyId: a1bc2155-c899-4c5f-9e4a-e07aaa51056b,KeyArn: arn:aws:kms:us-east-1:814230673633:key/a1bc2155-c899-4c5f-9e4a-e07aaa51056b}, {KeyId: a986ff87-7dcc-4726-8275-9356a465533a,KeyArn: arn:aws:kms:us-east-1:814230673633:key/a986ff87-7dcc-4726-8275-9356a465533a}, {KeyId: ad02194a-be5d-4ffe-a833-8d59c2ebbb22,KeyArn: arn:aws:kms:us-east-1:814230673633:key/ad02194a-be5d-4ffe-a833-8d59c2ebbb22}, {KeyId: d6e79d12-5e27-48e8-8f2c-aa3995a44311,KeyArn: arn:aws:kms:us-east-1:814230673633:key/d6e79d12-5e27-48e8-8f2c-aa3995a44311}, {KeyId: ee6323a2-d7eb-4145-8689-756546ca7fd9,KeyArn: arn:aws:kms:us-east-1:814230673633:key/ee6323a2-d7eb-4145-8689-756546ca7fd9}, {KeyId: fef2a7f6-4beb-45ed-8f9f-7083d5fd4d48,KeyArn: arn:aws:kms:us-east-1:814230673633:key/fef2a7f6-4beb-45ed-8f9f-7083d5fd4d48}],Truncated: false}[11/11/14 15:15:40:040 PST] DEBUG conn.PoolingClientConnectionManager [main]: Connection manager is shutting down
}
