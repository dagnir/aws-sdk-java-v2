package software.amazon.awssdk.services.s3.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Test;
import software.amazon.awssdk.AmazonClientException;
import software.amazon.awssdk.services.s3.event.S3EventNotification.S3EventNotificationRecord;
import software.amazon.awssdk.util.SdkHttpUtils;
import software.amazon.awssdk.util.StringUtils;

public class S3EventNotificationTest {
    private List<String> KEYS_REQUIRING_URL_ENCODE = Arrays.asList("foo bar.jpg", "foo/bar.csv", "foo<>bar");

    private static final String jsonBodySampleFile =
            "/software/amazon/awssdk/services/s3/event/eventNotificationBodyData.json";

    /**
     * Tests that we can parse event notification JSON correctly.
     */
    @Test
    public void testParseS3EventNotification() throws Exception {
        String json = this.readAllLinesFromFile(jsonBodySampleFile);

        S3EventNotification item = S3EventNotification.parseJson(json);

        S3EventNotification.S3EventNotificationRecord record = item.getRecords().get(0);

        InputStream jsonSampleStream = this.getClass().getResourceAsStream(jsonBodySampleFile);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode eventNode = mapper.readValue(jsonSampleStream, JsonNode.class).get("Records").get(0);

        // Validate the record level properties.
        Assert.assertEquals(record.getEventVersion(), eventNode.get("eventVersion").asText());
        Assert.assertEquals(record.getEventSource(), eventNode.get("eventSource").asText());
        Assert.assertEquals(record.getAwsRegion(), eventNode.get("awsRegion").asText());
        Assert.assertEquals(record.getEventTime(), DateTime.parse(eventNode.get("eventTime").asText()));
        Assert.assertEquals(record.getEventName(), eventNode.get("eventName").asText());
        Assert.assertEquals(record.getUserIdentity().getPrincipalId(), eventNode.get("userIdentity").get("principalId").asText());
        Assert.assertEquals(record.getRequestParameters().getSourceIPAddress(), eventNode.get("requestParameters").get("sourceIPAddress").asText());
        Assert.assertEquals(record.getResponseElements().getxAmzRequestId(), eventNode.get("responseElements").get("x-amz-request-id").asText());
        Assert.assertEquals(record.getResponseElements().getxAmzId2(), eventNode.get("responseElements").get("x-amz-id-2").asText());

        // Validate S3 object inside the record.
        JsonNode s3Node = eventNode.get("s3");

        Assert.assertEquals(record.getS3().getS3SchemaVersion(), s3Node.get("s3SchemaVersion").asText());
        Assert.assertEquals(record.getS3().getConfigurationId(), s3Node.get("configurationId").asText());

        JsonNode bucketNode = s3Node.get("bucket");
        Assert.assertEquals(record.getS3().getBucket().getArn(), bucketNode.get("arn").asText());
        Assert.assertEquals(record.getS3().getBucket().getName(), bucketNode.get("name").asText());
        Assert.assertEquals(record.getS3().getBucket().getOwnerIdentity().getPrincipalId(), bucketNode.get("ownerIdentity").get("principalId").asText());

        JsonNode objectnode = s3Node.get("object");
        Assert.assertEquals(record.getS3().getObject().getKey(), objectnode.get("key").asText());
        Assert.assertEquals((int) record.getS3().getObject().getSize(), objectnode.get("size").asInt());
        Assert.assertEquals((long) record.getS3().getObject().getSizeAsLong(), objectnode.get("size").asLong());
        Assert.assertEquals(record.getS3().getObject().geteTag(), objectnode.get("eTag").asText());
        Assert.assertEquals(record.getS3().getObject().getVersionId(), objectnode.get("versionId").asText());
    }

    /**
     * Tests that we throw when parsing malformed JSON.
     */
    @Test (expected=AmazonClientException.class)
    public void testParseMalformedJSON()
    {
        String malformedJSON = "this is not JSON";
        S3EventNotification.parseJson(malformedJSON);
    }

    @Test
    public void testParseIncorrectJSON()
    {
        String incorrectJson = "{ \"foo\": \"bar\" }";
        S3EventNotification item = S3EventNotification.parseJson(incorrectJson);
        Assert.assertNull(item.getRecords());
    }

    @Test
    public void testToJson() throws Exception {
        S3EventNotification notification = S3EventNotification.parseJson(
                readAllLinesFromFile(jsonBodySampleFile));

        String json = notification.toJson();

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readValue(json, JsonNode.class);
        Assert.assertTrue(root.isObject());

        JsonNode records = root.get("Records");
        Assert.assertNotNull(records);
        Assert.assertTrue(records.isArray());
        Assert.assertEquals(1, records.size());

        JsonNode eventNode = records.get(0);
        Assert.assertNotNull(eventNode);
        Assert.assertTrue(eventNode.isObject());

        S3EventNotificationRecord record = notification.getRecords().get(0);

        // Validate the record level properties.
        Assert.assertEquals(record.getEventVersion(), eventNode.get("eventVersion").asText());
        Assert.assertEquals(record.getEventSource(), eventNode.get("eventSource").asText());
        Assert.assertEquals(record.getAwsRegion(), eventNode.get("awsRegion").asText());
        Assert.assertEquals(record.getEventTime(), DateTime.parse(eventNode.get("eventTime").asText()));
        Assert.assertEquals(record.getEventName(), eventNode.get("eventName").asText());
        Assert.assertEquals(record.getUserIdentity().getPrincipalId(), eventNode.get("userIdentity").get("principalId").asText());
        Assert.assertEquals(record.getRequestParameters().getSourceIPAddress(), eventNode.get("requestParameters").get("sourceIPAddress").asText());
        Assert.assertEquals(record.getResponseElements().getxAmzRequestId(), eventNode.get("responseElements").get("x-amz-request-id").asText());
        Assert.assertEquals(record.getResponseElements().getxAmzId2(), eventNode.get("responseElements").get("x-amz-id-2").asText());

        // Validate S3 object inside the record.
        JsonNode s3Node = eventNode.get("s3");

        Assert.assertEquals(record.getS3().getS3SchemaVersion(), s3Node.get("s3SchemaVersion").asText());
        Assert.assertEquals(record.getS3().getConfigurationId(), s3Node.get("configurationId").asText());

        JsonNode bucketNode = s3Node.get("bucket");
        Assert.assertEquals(record.getS3().getBucket().getArn(), bucketNode.get("arn").asText());
        Assert.assertEquals(record.getS3().getBucket().getName(), bucketNode.get("name").asText());
        Assert.assertEquals(record.getS3().getBucket().getOwnerIdentity().getPrincipalId(), bucketNode.get("ownerIdentity").get("principalId").asText());

        JsonNode objectnode = s3Node.get("object");
        Assert.assertEquals(record.getS3().getObject().getKey(), objectnode.get("key").asText());
        Assert.assertEquals((long) record.getS3().getObject().getSizeAsLong(), objectnode.get("size").asLong());
        Assert.assertEquals(record.getS3().getObject().geteTag(), objectnode.get("eTag").asText());
        Assert.assertEquals(record.getS3().getObject().getVersionId(), objectnode.get("versionId").asText());
    }

    @Test
    public void testGetUrlDecodedKey() {
        for (String testKey : KEYS_REQUIRING_URL_ENCODE) {
            String urlEncoded = SdkHttpUtils.urlEncode(testKey, false);
            S3EventNotification.S3ObjectEntity entity = new S3EventNotification.S3ObjectEntity(urlEncoded,
                    1L, "E-Tag", "versionId");
            Assert.assertEquals(testKey, entity.getUrlDecodedKey());
        }
    }

    private String readAllLinesFromFile(String file) throws IOException
    {
        InputStream jsonSampleStream = this.getClass().getResourceAsStream(file);
        List<String> lines = IOUtils.readLines(jsonSampleStream);
        return StringUtils.join("\n", lines.toArray(new String[0]));
    }
}
