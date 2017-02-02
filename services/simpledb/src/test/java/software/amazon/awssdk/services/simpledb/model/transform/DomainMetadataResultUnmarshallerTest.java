package software.amazon.awssdk.services.simpledb.model.transform;

import static org.junit.Assert.assertTrue;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;

import org.junit.Test;

import software.amazon.awssdk.services.simpledb.model.DomainMetadataResult;
import software.amazon.awssdk.transform.StaxUnmarshallerContext;

public class DomainMetadataResultUnmarshallerTest {

    /**
     * Test method for DomainMetadataResultXpathUnmarshaller
     */
    @Test
    public final void testXpathUnmarshaller() throws Exception {
        XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
        XMLEventReader eventReader = xmlInputFactory.createXMLEventReader(DomainMetadataResultUnmarshallerTest.class
                .getResourceAsStream("DomainMetadataResponse.xml"));
        StaxUnmarshallerContext unmarshallerContext = new StaxUnmarshallerContext(eventReader);
        DomainMetadataResult result = (DomainMetadataResult) new DomainMetadataResultStaxUnmarshaller()
                .unmarshall(unmarshallerContext);

        assertTrue(result.getItemCount() == 25);
        assertTrue(result.getItemNamesSizeBytes() == 12345);
        assertTrue(result.getAttributeNameCount() == 20);
        assertTrue(result.getAttributeNamesSizeBytes() == 2345);
        assertTrue(result.getAttributeValueCount() == 25);
        assertTrue(result.getAttributeValuesSizeBytes() == 1234);
        assertTrue(result.getTimestamp() == 5555);
    }

}
