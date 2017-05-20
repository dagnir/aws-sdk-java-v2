package software.amazon.awssdk.metrics.internal.cloudwatch.provider.transform;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Test;
import software.amazon.awssdk.DefaultRequest;
import software.amazon.awssdk.Request;
import software.amazon.awssdk.Response;
import software.amazon.awssdk.metrics.internal.cloudwatch.transform.DynamoDbRequestMetricTransformer;
import software.amazon.awssdk.services.cloudwatch.model.MetricDatum;
import software.amazon.awssdk.services.dynamodb.metrics.DynamoDbRequestMetric;
import software.amazon.awssdk.services.dynamodb.model.ConsumedCapacity;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemResult;

public class DynamoDbRequestMetricTransformerTest {
    @Test
    public void trival() {
        DynamoDbRequestMetricTransformer t = new DynamoDbRequestMetricTransformer();
        assertNull(t.toMetricData(null, null, null));
    }

    @Test
    public void noConsumedCapacity() {
        DynamoDbRequestMetricTransformer t = new DynamoDbRequestMetricTransformer();
        PutItemRequest pi_req = PutItemRequest.builder().build();
        Request<PutItemRequest> req = new DefaultRequest<PutItemRequest>(pi_req, "test");
        PutItemResult pi_res = PutItemResult.builder().build();
        Response<PutItemResult> res = new Response<PutItemResult>(pi_res, null);
        List<MetricDatum> list = t.toMetricData(DynamoDbRequestMetric.DynamoDBConsumedCapacity, req, res);
        assertTrue(list.size() == 0);
    }

    @Test
    public void consumedCapacity() {
        DynamoDbRequestMetricTransformer t = new DynamoDbRequestMetricTransformer();
        PutItemRequest pi_req = PutItemRequest.builder().build();
        Request<PutItemRequest> req = new DefaultRequest<PutItemRequest>(pi_req, "test");
        PutItemResult pi_res = PutItemResult.builder()
                .consumedCapacity(ConsumedCapacity.builder()
                        .capacityUnits(1.0)
                        .tableName("testTable")
                        .build())
                .build();
        Response<PutItemResult> res = new Response<PutItemResult>(pi_res, null);
        List<MetricDatum> list = t.toMetricData(
                DynamoDbRequestMetric.DynamoDBConsumedCapacity, req, res);
        assertTrue(list.size() == 1);
    }
}
