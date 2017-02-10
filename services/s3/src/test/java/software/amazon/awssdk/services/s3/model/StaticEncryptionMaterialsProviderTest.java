package software.amazon.awssdk.services.s3.model;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.security.KeyPair;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

public class StaticEncryptionMaterialsProviderTest {
    @Test
    public void nullMaterialDesc() {
        EncryptionMaterials m = new EncryptionMaterials((KeyPair)null);
        EncryptionMaterials m2 = new StaticEncryptionMaterialsProvider(m).getEncryptionMaterials(null);
        assertSame(m, m2);
    }

    @Test
    public void emptyMaterialDesc() {
        EncryptionMaterials m = new EncryptionMaterials((KeyPair)null);
        Map<String,String> empty = Collections.emptyMap();
        EncryptionMaterials m2 = new StaticEncryptionMaterialsProvider(m).getEncryptionMaterials(empty);
        assertSame(m, m2);
    }

    @Test
    public void nonEmptyClientMaterialDesc() {
        final Map<String,String> map = new HashMap<String,String>();
        map.put("Foo", "Bar");
        EncryptionMaterials m = new EncryptionMaterials((KeyPair)null) {
            @Override public java.util.Map<String,String> getMaterialsDescription() {
                return map;
            };
        };
        Map<String,String> empty = Collections.emptyMap();
        StaticEncryptionMaterialsProvider p = new StaticEncryptionMaterialsProvider(m);
        assertNull(p.getEncryptionMaterials(empty));
        assertNull(p.getEncryptionMaterials(null));
        EncryptionMaterials m2 = p.getEncryptionMaterials(map);
        assertSame(m, m2);
    }

    @Test
    public void materialAccessor() {
        final EncryptionMaterials m4null = new EncryptionMaterials((KeyPair)null);
        EncryptionMaterials m = new EncryptionMaterials((KeyPair)null) {
            @Override public EncryptionMaterialsAccessor getAccessor() {
                return new EncryptionMaterialsAccessor() {
                    @Override
                    public EncryptionMaterials getEncryptionMaterials(
                            Map<String, String> materialsDescription) {
                        return materialsDescription == null ? m4null : null;
                    }
                };
            }
        };
        Map<String,String> empty = Collections.emptyMap();
        StaticEncryptionMaterialsProvider p = new StaticEncryptionMaterialsProvider(m);
        assertSame(m, p.getEncryptionMaterials(empty));
        assertSame(m4null, p.getEncryptionMaterials(null));
        final Map<String,String> map = new HashMap<String,String>();
        map.put("Foo", "Bar");
        assertNull(p.getEncryptionMaterials(map));
    }
}
