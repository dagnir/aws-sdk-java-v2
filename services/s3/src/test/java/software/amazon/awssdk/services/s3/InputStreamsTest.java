package software.amazon.awssdk.services.s3;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import org.junit.Test;
import software.amazon.awssdk.runtime.io.ResettableInputStream;
import software.amazon.awssdk.services.s3.internal.InputSubstream;
import software.amazon.awssdk.services.s3.internal.crypto.ByteRangeCapturingInputStream;
import software.amazon.awssdk.util.CountingInputStream;

/**
 * Unit tests for various InputStream subclasses.
 */
public class InputStreamsTest {

	String sampleData =
		  "__________1234567890__________12345678901234567890"
		+ "12345678901234567890123456789012345678901234567890"
		+ "12345678901234567890123456789012345678901234567890"
		+ "12345678901234567890123456789012345678901234567890"
		+ "12345678901234567890123456789012345678909876543210";


	/**
	 * Tests that we can properly capture a byte range with the ByteRangeCapturingInputStream class.
	 */
	@Test
	public void testByteRangeCapturingInputStream() throws Exception {
		int sampleDataLength = sampleData.length();
		ByteRangeCapturingInputStream in = new ByteRangeCapturingInputStream(new ByteArrayInputStream(sampleData.getBytes()), sampleDataLength - 10, sampleDataLength);
		in.mark(100);
		in.read(new byte[sampleDataLength - 10]);
		in.reset();
		in.read(new byte[sampleDataLength]);
		assertEquals("9876543210", new String(in.getBlock()));

		in = new ByteRangeCapturingInputStream(new ByteArrayInputStream(sampleData.getBytes()), 10, 20);
		in.read(new byte[sampleDataLength]);
		assertEquals("1234567890", new String(in.getBlock()));
	}

	/** Tests the simple use case for InputSubstream */
	@Test
	public void testSimple() throws Exception {
		InputSubstream in = new InputSubstream(new ByteArrayInputStream(sampleData.getBytes()), 10, 10, true);
		assertEquals(10, in.available());
		byte[] buffer = new byte[10];
		assertEquals(10, in.read(buffer));
		assertEquals("1234567890", new String(buffer));
		assertEquals(0, in.available());


		CountingInputStream countingStream = new CountingInputStream(new InputSubstream(new ByteArrayInputStream(sampleData.getBytes()), 10, 10, true));
		int c;
		System.out.print("Data: ");
		while ((c = countingStream.read()) > -1) {
			System.out.print((char)c);
		}
		System.out.println();
		assertEquals((long)10, countingStream.getByteCount());


		countingStream = new CountingInputStream(new InputSubstream(new ByteArrayInputStream(sampleData.getBytes()), 10, 10, true));
		byte[] bytes = new byte[1];
		System.out.print("Data: ");
		while ((c = countingStream.read(bytes)) > -1) {
			System.out.print((char)bytes[0]);
		}
		System.out.println();
		assertEquals((long)10, countingStream.getByteCount());
	}

	/**
	 * Tests that we can combine InputSubstream with RepeatableFileInputStream
	 * and correctly mark/reset the streams.
	 */
	@Test
	public void testMarkReset() throws Exception {
        File tempFile = File.createTempFile("aws-java-sdk-inputsubstream-test", ".dat");
		FileOutputStream outputStream = new FileOutputStream(tempFile);
		outputStream.write(sampleData.getBytes());
		outputStream.close();

		ResettableInputStream repeatableFileInputStream = new ResettableInputStream(tempFile);
		InputSubstream in = new InputSubstream(repeatableFileInputStream, 10, 10, true);
		assertEquals(10, in.available());
		byte[] buffer = new byte[5];


		in.mark(1024);
		assertEquals(5, in.read(buffer));
		assertEquals("12345", new String(buffer));
		assertEquals(5, in.available());

		in.reset();
		assertEquals(10, in.available());
		assertEquals(5, in.read(buffer));
		assertEquals("12345", new String(buffer));
		assertEquals(5, in.available());

		assertEquals(5, in.read(buffer));
		assertEquals("67890", new String(buffer));
		assertEquals(0, in.available());
	}
}
