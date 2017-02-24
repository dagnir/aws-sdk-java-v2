package demo;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;

import software.amazon.awssdk.services.kms.AWSKMSClient;
import software.amazon.awssdk.services.kms.model.DecryptRequest;
import software.amazon.awssdk.services.kms.model.DecryptResult;
import software.amazon.awssdk.services.kms.model.GenerateDataKeyRequest;
import software.amazon.awssdk.services.kms.model.GenerateDataKeyResult;
import software.amazon.awssdk.services.kms.model.ListKeysResult;

public class KMSListKeyDemo {

    public static void main(String[] args) throws IOException {
        if (false) {
            AWSKMSClient kms = new AWSKMSClient();
                // .withRegion(Region.getRegion(Regions.US_WEST_2));
            ListKeysResult result = kms.listKeys();
            System.err.println(result);
            kms.shutdown();
        } else {
            testDataKey("927c6ab6-8fe6-418c-bfa1-ce19dfb20171");
        }
    }

    private static void testDataKey(String keyId) {
        AWSKMSClient kms = new AWSKMSClient();
        Map<String,String> ctx = new HashMap<String,String>();
        ctx.put("foo", "bar");
        GenerateDataKeyResult result = kms.generateDataKey(new GenerateDataKeyRequest()
            .withKeyId(keyId)
            .withKeySpec("AES_128")
            .withEncryptionContext(ctx)
        );
        ByteBuffer blob = result.getCiphertextBlob();
        ByteBuffer plaintext = result.getPlaintext();
        DecryptResult dresult = kms.decrypt(new DecryptRequest().withCiphertextBlob(blob).withEncryptionContext(ctx));
        System.out.println(keyId);
        System.out.println(dresult.getKeyId());
        ByteBuffer bb = dresult.getPlaintext();
        Assert.assertTrue(Arrays.equals(bb.array(), plaintext.array()));
    }
}
