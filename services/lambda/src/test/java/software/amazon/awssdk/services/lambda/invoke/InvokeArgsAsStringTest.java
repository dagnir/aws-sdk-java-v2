package software.amazon.awssdk.services.lambda.invoke;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import org.junit.Assert;
import org.junit.Test;
import software.amazon.awssdk.services.lambda.model.InvokeAsyncRequest;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.util.IOUtils;
import software.amazon.awssdk.util.StringUtils;

public class InvokeArgsAsStringTest {

    private static final String ARGS = "{ a : 'a', b : 'b' }";

    @Test
    public void testInvokeAsyncArgsAsString() throws IOException {
        InvokeAsyncRequest request = new InvokeAsyncRequest();
        request.setInvokeArgs(ARGS);

        InputStream stream = request.getInvokeArgs();

        String decoded = new String(IOUtils.toByteArray(stream), StringUtils.UTF8);

        Assert.assertEquals(ARGS, decoded);
    }

    @Test
    public void testInvokeArgsAsString() {
        InvokeRequest request = new InvokeRequest();
        request.setPayload(ARGS);

        ByteBuffer bb = request.getPayload();
        String decoded = StringUtils.UTF8.decode(bb).toString();

        Assert.assertEquals(ARGS, decoded);
    }
}
