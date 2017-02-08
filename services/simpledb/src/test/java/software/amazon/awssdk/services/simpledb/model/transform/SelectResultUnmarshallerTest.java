package software.amazon.awssdk.services.simpledb.model.transform;

import static org.junit.Assert.assertTrue;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import org.junit.Test;
import software.amazon.awssdk.runtime.transform.StaxUnmarshallerContext;
import software.amazon.awssdk.services.simpledb.model.Item;
import software.amazon.awssdk.services.simpledb.model.SelectResult;

public class SelectResultUnmarshallerTest {

    /**
     * Test method for SelectResultUnmarshaller
     */
    @Test
    public final void testUnmarshall() throws Exception {
        XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
        XMLEventReader eventReader = xmlInputFactory.createXMLEventReader(DomainMetadataResultUnmarshallerTest.class
                .getResourceAsStream("SelectResponse.xml"));
        StaxUnmarshallerContext unmarshallerContext = new StaxUnmarshallerContext(eventReader);
        SelectResult result = (SelectResult) new SelectResultStaxUnmarshaller().unmarshall(unmarshallerContext);

        assertTrue(!result.getItems().isEmpty());
        assertTrue(result.getItems().size() == 2);
        assertTrue(((Item) result.getItems().get(0)).getName().equals("ItemOne"));
        assertTrue(((Item) result.getItems().get(1)).getName().equals("ItemTwo"));
    }

}
