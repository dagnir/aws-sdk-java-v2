package software.amazon.awssdk.protocol.tests.crc32;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

import com.github.tomakehurst.wiremock.common.SingleRootFileSource;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.apache.log4j.BasicConfigurator;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import software.amazon.awssdk.AmazonClientException;
import software.amazon.awssdk.ClientConfiguration;
import software.amazon.awssdk.auth.AWSCredentialsProvider;
import software.amazon.awssdk.auth.AWSStaticCredentialsProvider;
import software.amazon.awssdk.auth.BasicAWSCredentials;
import software.amazon.awssdk.services.protocol.jsonrpc.AmazonProtocolJsonRpc;
import software.amazon.awssdk.services.protocol.jsonrpc.AmazonProtocolJsonRpcClient;
import software.amazon.awssdk.services.protocol.jsonrpc.model.AllTypesRequest;
import software.amazon.awssdk.services.protocol.jsonrpc.model.AllTypesResult;

public class AwsJsonCRC32ChecksumTests {

    @Rule
    public WireMockRule mockServer = new WireMockRule(WireMockConfiguration.wireMockConfig()
            .port(0)
            .fileSource(new SingleRootFileSource("src/test/resources")));

    private static final String JSON_BODY = "{\"StringMember\":\"foo\"}";
    private static final String JSON_BODY_GZIP = "compressed_json_body.gz";
    private static final String JSON_BODY_CRC32_CHECKSUM = "3049587505";
    private static final String JSON_BODY_GZIP_CRC32_CHECKSUM = "3023995622";

    private static final AWSCredentialsProvider FAKE_CREDENTIALS_PROVIDER = new AWSStaticCredentialsProvider(
            new BasicAWSCredentials("foo", "bar"));

    @BeforeClass
    public static void setup() {
        BasicConfigurator.configure();
    }

    @Test
    public void clientCalculatesCRC32FromCompressedData_WhenCRC32IsValid() {
        stubFor(post(urlEqualTo("/")).willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Encoding", "gzip")
                .withHeader("x-amz-crc32", JSON_BODY_GZIP_CRC32_CHECKSUM)
                .withBodyFile(JSON_BODY_GZIP)));
        AmazonProtocolJsonRpc jsonRpc = new AmazonProtocolJsonRpcCRC32TestClient(FAKE_CREDENTIALS_PROVIDER,
                                                                                 new ClientConfiguration().withGzip(true));
        jsonRpc.setEndpoint("http://localhost:" + mockServer.port());
        AllTypesResult result =
                jsonRpc.allTypes(new AllTypesRequest());
        Assert.assertEquals("foo", result.getStringMember());
    }

    @Test(expected = AmazonClientException.class)
    public void clientCalculatesCRC32FromCompressedData_WhenCRC32IsInvalid_ThrowsException() {
        stubFor(post(urlEqualTo("/")).willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Encoding", "gzip")
                .withHeader("x-amz-crc32", JSON_BODY_CRC32_CHECKSUM)
                .withBodyFile(JSON_BODY_GZIP)));
        AmazonProtocolJsonRpc jsonRpc = new AmazonProtocolJsonRpcCRC32TestClient(FAKE_CREDENTIALS_PROVIDER,
                new ClientConfiguration().withGzip(true));
        jsonRpc.setEndpoint("http://localhost:" + mockServer.port());
        jsonRpc.allTypes(new AllTypesRequest());
    }

    private static class AmazonProtocolJsonRpcCRC32TestClient extends AmazonProtocolJsonRpcClient {

        public AmazonProtocolJsonRpcCRC32TestClient(AWSCredentialsProvider credentialsProvider, ClientConfiguration config) {
            super(credentialsProvider, config);
        }

        @Override
        public final boolean calculateCRC32FromCompressedData() {
            return true;
        }
    }

    @Test
    public void clientCalculatesCRC32FromDecompressedData_WhenCRC32IsValid() {
        stubFor(post(urlEqualTo("/")).willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Encoding", "gzip")
                .withHeader("x-amz-crc32", JSON_BODY_CRC32_CHECKSUM)
                .withBodyFile(JSON_BODY_GZIP)));
        AmazonProtocolJsonRpc jsonRpc = new AmazonProtocolJsonRpcClient(FAKE_CREDENTIALS_PROVIDER,
                new ClientConfiguration().withGzip(true));
        jsonRpc.setEndpoint("http://localhost:" + mockServer.port());
        AllTypesResult result =
                jsonRpc.allTypes(new AllTypesRequest());
        Assert.assertEquals("foo", result.getStringMember());
    }

    @Test(expected = AmazonClientException.class)
    public void clientCalculatesCRC32FromDecompressedData_WhenCRC32IsInvalid_ThrowsException() {
        stubFor(post(urlEqualTo("/")).willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Encoding", "gzip")
                .withHeader("x-amz-crc32", JSON_BODY_GZIP_CRC32_CHECKSUM)
                .withBodyFile(JSON_BODY_GZIP)));
        AmazonProtocolJsonRpc jsonRpc = new AmazonProtocolJsonRpcClient(FAKE_CREDENTIALS_PROVIDER,
                new ClientConfiguration().withGzip(true));
        jsonRpc.setEndpoint("http://localhost:" + mockServer.port());
        jsonRpc.allTypes(new AllTypesRequest());
    }

    @Test
    public void useGzipFalse_WhenCRC32IsValid() {
        stubFor(post(urlEqualTo("/")).willReturn(aResponse()
                .withStatus(200)
                .withHeader("x-amz-crc32", JSON_BODY_CRC32_CHECKSUM)
                .withBody(JSON_BODY)));
        AmazonProtocolJsonRpc jsonRpc = new AmazonProtocolJsonRpcClient(FAKE_CREDENTIALS_PROVIDER,
                new ClientConfiguration().withGzip(false));
        jsonRpc.setEndpoint("http://localhost:" + mockServer.port());
        AllTypesResult result =
                jsonRpc.allTypes(new AllTypesRequest());
        Assert.assertEquals("foo", result.getStringMember());
    }

    @Test(expected = AmazonClientException.class)
    public void useGzipFalse_WhenCRC32IsInvalid_ThrowException() {
        stubFor(post(urlEqualTo("/")).willReturn(aResponse()
                .withStatus(200)
                .withHeader("x-amz-crc32", JSON_BODY_GZIP_CRC32_CHECKSUM)
                .withBody(JSON_BODY)));
        AmazonProtocolJsonRpc jsonRpc = new AmazonProtocolJsonRpcClient(FAKE_CREDENTIALS_PROVIDER,
                new ClientConfiguration().withGzip(false));
        jsonRpc.setEndpoint("http://localhost:" + mockServer.port());
        jsonRpc.allTypes(new AllTypesRequest());
    }

}
