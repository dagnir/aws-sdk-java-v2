package software.amazon.awssdk.services.kms;

import junit.framework.Assert;
import org.junit.Test;
import software.amazon.awssdk.services.kms.model.CreateAliasRequest;
import software.amazon.awssdk.services.kms.model.CreateKeyRequest;
import software.amazon.awssdk.services.kms.model.CreateKeyResult;
import software.amazon.awssdk.services.kms.model.DescribeKeyRequest;
import software.amazon.awssdk.services.kms.model.DescribeKeyResult;
import software.amazon.awssdk.services.kms.model.DisableKeyRequest;
import software.amazon.awssdk.services.kms.model.EnableKeyRequest;
import software.amazon.awssdk.services.kms.model.GetKeyPolicyRequest;
import software.amazon.awssdk.services.kms.model.GetKeyPolicyResult;
import software.amazon.awssdk.services.kms.model.KeyMetadata;
import software.amazon.awssdk.services.kms.model.KeyUsageType;
import software.amazon.awssdk.services.kms.model.ListKeysResult;

public class ServiceIntegrationTest extends IntegrationTestBase {

    @Test
    public void testKeyOperations() {

        // CreateKey
        CreateKeyResult createKeyResult = kms.createKey(new CreateKeyRequest().withDescription("My KMS Key")
                .withKeyUsage(KeyUsageType.ENCRYPT_DECRYPT));
        checkValid_KeyMetadata(createKeyResult.getKeyMetadata());

        final String keyId = createKeyResult.getKeyMetadata().getKeyId();

        // DescribeKey
        DescribeKeyResult describeKeyResult = kms.describeKey(new DescribeKeyRequest().withKeyId(keyId));
        checkValid_KeyMetadata(describeKeyResult.getKeyMetadata());

        // Enable/DisableKey
        kms.enableKey(new EnableKeyRequest().withKeyId(keyId));
        kms.disableKey(new DisableKeyRequest().withKeyId(keyId));

        // ListKeys
        ListKeysResult listKeysResult = kms.listKeys();
        Assert.assertFalse(listKeysResult.getKeys().isEmpty());

        // CreateAlias
        kms.createAlias(new CreateAliasRequest().withAliasName("alias/my_key" + System.currentTimeMillis())
                .withTargetKeyId(keyId));

        GetKeyPolicyResult getKeyPolicyResult = kms.getKeyPolicy(new GetKeyPolicyRequest().withKeyId(keyId)
                .withPolicyName("default"));
        Assert.assertNotNull(getKeyPolicyResult.getPolicy());

    }

    private static void checkValid_KeyMetadata(KeyMetadata kmd) {
        Assert.assertNotNull(kmd);

        Assert.assertNotNull(kmd.getArn());
        Assert.assertNotNull(kmd.getAWSAccountId());
        Assert.assertNotNull(kmd.getDescription());
        Assert.assertNotNull(kmd.getKeyId());
        Assert.assertNotNull(kmd.getKeyUsage());
        Assert.assertNotNull(kmd.getCreationDate());
        Assert.assertNotNull(kmd.getEnabled());
    }
}
