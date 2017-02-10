package software.amazon.awssdk.services.s3.util;

import java.io.IOException;
import java.io.InputStream;

public class StreamUtils {
    public static void consumeInputStream(InputStream in) throws IOException {
        byte[] buffer = new byte[1024 * 10];
        while (in.read(buffer) > -1) {
        }
    }
}
