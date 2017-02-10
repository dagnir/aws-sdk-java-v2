package software.amazon.awssdk.services.s3.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Map;
import org.junit.Test;
import software.amazon.awssdk.services.s3.model.ExtraMaterialsDescription.ConflictResolution;
import software.amazon.awssdk.util.StringMapBuilder;

public class ExtraMaterialsDescriptionTest {
    @SuppressWarnings("unchecked")
    @Test
    public void testIdentities() {
        ExtraMaterialsDescription extra = new ExtraMaterialsDescription(
                new StringMapBuilder("k1", "v1").build());
        Map<String,String> core = new StringMapBuilder("k2", "v2").build();
        assertEquals(extra.getMaterialDescription(), extra.mergeInto(null));
        assertEquals(core, ExtraMaterialsDescription.NONE.mergeInto(core));
        // check unmodifiablility
        checkUnModifiability(extra.getMaterialDescription(), core);
    }
    
    private void checkUnModifiability(Map<String,String> ...maps) {
        for (Map<String,String> map: maps) {
            try {
                map.put("foo", "bar");
                fail();
            } catch(UnsupportedOperationException expected) {
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testNoOverlaps() {
        ExtraMaterialsDescription extra = new ExtraMaterialsDescription(
                new StringMapBuilder("k1", "v1").build());
        Map<String,String> core = new StringMapBuilder("k2", "v2").build();
        Map<String,String> merged = extra.mergeInto(core);
        assertTrue(merged.size() == 2);
        // check unmodifiablility
        checkUnModifiability(extra.getMaterialDescription(), core, merged);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testFailFast() {
        ExtraMaterialsDescription extra = new ExtraMaterialsDescription(
                new StringMapBuilder("k1", "v1").build());
        Map<String,String> core = extra.getMaterialDescription();
        try {
            extra.mergeInto(core);
            fail();
        } catch(IllegalArgumentException expected) {
        }
        assertTrue(extra.getMaterialDescription().size() == 1);
        assertTrue(core.size() == 1);
        // check unmodifiablility
        checkUnModifiability(extra.getMaterialDescription(), core);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testOverride() {
        ExtraMaterialsDescription extra = new ExtraMaterialsDescription(
                new StringMapBuilder("k1", "extra").build(),
                ConflictResolution.OVERRIDE);
        Map<String,String> core = new StringMapBuilder("k1", "core")
                                    .put("k2", "v2")
                                    .build();
        Map<String,String> merged = extra.mergeInto(core);
        assertTrue(extra.getMaterialDescription().size() == 1);
        assertTrue(core.size() == 2);
        assertTrue(merged.size() == 2);
        assertEquals("extra", merged.get("k1"));
        // check unmodifiablility
        checkUnModifiability(extra.getMaterialDescription(), core, merged);
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testOverridden() {
        ExtraMaterialsDescription extra = new ExtraMaterialsDescription(
                new StringMapBuilder("k1", "extra").build(),
                ConflictResolution.OVERRIDDEN);
        Map<String,String> core = new StringMapBuilder("k1", "core")
                                    .put("k2", "v2")
                                    .build();
        Map<String,String> merged = extra.mergeInto(core);
        assertTrue(extra.getMaterialDescription().size() == 1);
        assertTrue(core.size() == 2);
        assertTrue(merged.size() == 2);
        assertEquals("core", merged.get("k1"));
        // check unmodifiablility
        checkUnModifiability(extra.getMaterialDescription(), core, merged);
    }
}
