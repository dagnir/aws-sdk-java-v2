package software.amazon.awssdk.services.simpledb.model.transform;

import static org.junit.Assert.assertTrue;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;

import org.junit.Test;

import software.amazon.awssdk.services.simpledb.model.Attribute;
import software.amazon.awssdk.services.simpledb.model.GetAttributesResult;
import software.amazon.awssdk.transform.StaxUnmarshallerContext;

public class GetAttributesResultUnmarshallerTest {

    /**
     * Test method for GetAttributesResultUnmarshaller
     */
    @Test
    public final void testUnmarshall() throws Exception {
        XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
        XMLEventReader eventReader = xmlInputFactory.createXMLEventReader(DomainMetadataResultUnmarshallerTest.class
                .getResourceAsStream("GetAttributesResponse.xml"));
        StaxUnmarshallerContext unmarshallerContext = new StaxUnmarshallerContext(eventReader);
        GetAttributesResult result = (GetAttributesResult) new GetAttributesResultStaxUnmarshaller()
                .unmarshall(unmarshallerContext);

        assertTrue(!result.getAttributes().isEmpty());
        assertTrue(result.getAttributes().size() == 2);
        assertTrue(((Attribute) result.getAttributes().get(0)).getName().equals("Color"));
        assertTrue(((Attribute) result.getAttributes().get(0)).getValue().equals("Blue"));
        assertTrue(((Attribute) result.getAttributes().get(1)).getName().equals("Price"));
        assertTrue(((Attribute) result.getAttributes().get(1)).getValue().equals("$2.50"));
    }

}
