package software.amazon.awssdk.services.route53;

import java.util.List;
import java.util.Map;
import junit.framework.Assert;
import org.junit.Test;
import software.amazon.awssdk.Request;
import software.amazon.awssdk.services.route53.model.GetHealthCheckLastFailureReasonRequest;
import software.amazon.awssdk.services.route53.model.ListHealthChecksRequest;
import software.amazon.awssdk.services.route53.model.transform.GetHealthCheckLastFailureReasonRequestMarshaller;
import software.amazon.awssdk.services.route53.model.transform.ListHealthChecksRequestMarshaller;

public class QueryParamBindingTest {

    /**
     * Make sure the marshaller is able to handle @UriLabel parameter values
     * containing special characters.
     *
     * https://tt.amazon.com/0048388339
     * https://tt.amazon.com/0048675980
     */
    @Test
    public void testReservedCharInParamValue() {

        final String VALUE_WITH_SEMICOLON = ";foo";
        final String VALUE_WITH_AMPERSAND = "&bar";
        final String VALUE_WITH_QUESTION_MARK = "?charlie";

        // https://tt.amazon.com/0048388339

        ListHealthChecksRequest listReq = new ListHealthChecksRequest()
                .withMarker(VALUE_WITH_SEMICOLON)
                .withMaxItems(VALUE_WITH_AMPERSAND);

        Request<ListHealthChecksRequest> httpReq_List = new ListHealthChecksRequestMarshaller().marshall(listReq);
        Assert.assertEquals("/2013-04-01/healthcheck", httpReq_List.getResourcePath());

        Map<String, List<String>> queryParams = httpReq_List.getParameters();
        Assert.assertEquals(2, queryParams.size());
        Assert.assertEquals(VALUE_WITH_SEMICOLON, queryParams.get("marker").get(0));
        Assert.assertEquals(VALUE_WITH_AMPERSAND, queryParams.get("maxitems").get(0));

        // https://tt.amazon.com/0048675980

        GetHealthCheckLastFailureReasonRequest getFailureReq = new GetHealthCheckLastFailureReasonRequest();
        getFailureReq.setHealthCheckId(VALUE_WITH_QUESTION_MARK);

        Request<GetHealthCheckLastFailureReasonRequest> httpReq_GetFailure =
                new GetHealthCheckLastFailureReasonRequestMarshaller().marshall(getFailureReq);
        System.out.println(httpReq_GetFailure);
        // parameter value should be URL encoded
        Assert.assertEquals(
                "/2013-04-01/healthcheck/%3Fcharlie/lastfailurereason",
                httpReq_GetFailure.getResourcePath());

        queryParams = httpReq_GetFailure.getParameters();
        Assert.assertEquals(0, queryParams.size());
    }
}
