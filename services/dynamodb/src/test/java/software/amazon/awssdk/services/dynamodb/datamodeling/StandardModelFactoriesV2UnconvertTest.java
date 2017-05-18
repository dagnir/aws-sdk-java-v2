/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.services.dynamodb.datamodeling;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import org.junit.Assert;
import org.junit.Test;
import software.amazon.awssdk.auth.AwsCredentialsProvider;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.pojos.SubClass;
import software.amazon.awssdk.services.dynamodb.pojos.TestClass;
import software.amazon.awssdk.services.dynamodb.pojos.UnannotatedSubClass;
import software.amazon.awssdk.services.s3.model.Region;

public class StandardModelFactoriesV2UnconvertTest {

    protected static final DynamoDbMapperConfig CONFIG = new DynamoDbMapperConfig.Builder()
            .withTypeConverterFactory(DynamoDbMapperConfig.DEFAULT.getTypeConverterFactory())
            .withConversionSchema(ConversionSchemas.V2)
            .build();

    private static final DynamoDbMapperModelFactory factory = StandardModelFactories.of(new S3Link.Factory(new S3ClientCache((AwsCredentialsProvider) null)));
    private static final DynamoDbMapperModelFactory.TableFactory models = factory.getTableFactory(CONFIG);

    protected <T> Object unconvert(Class<T> clazz, Method getter, Method setter, AttributeValue value) {
        final StandardAnnotationMaps.FieldMap<Object> map = StandardAnnotationMaps.of(getter, null);
        return models.getTable(clazz).field(map.attributeName()).unconvert(value);
    }

    @Test
    public void testBoolean() {
        assertEquals(false, unconvert("getBoolean", "setBoolean",
                                      AttributeValue.builder_().n("0").build_()));

        assertEquals(true, unconvert("getBoolean", "setBoolean",
                                     AttributeValue.builder_().n("1").build_()));

        assertEquals(false, unconvert("getBoolean", "setBoolean",
                                      AttributeValue.builder_().bool(false).build_()));

        assertEquals(true, unconvert("getBoolean", "setBoolean",
                                     AttributeValue.builder_().bool(true).build_()));
        
        assertEquals(false, unconvert("getBoxedBoolean", "setBoxedBoolean",
                                      AttributeValue.builder_().n("0").build_()));

        assertEquals(true, unconvert("getBoxedBoolean", "setBoxedBoolean",
                                     AttributeValue.builder_().n("1").build_()));

        assertEquals(false, unconvert("getBoxedBoolean", "setBoxedBoolean",
                                      AttributeValue.builder_().bool(false).build_()));

        assertEquals(true, unconvert("getBoxedBoolean", "setBoxedBoolean",
                                     AttributeValue.builder_().bool(true).build_()));
    }

    @Test
    public void testString() {
        assertEquals("test", unconvert("getString", "setString",
                                       AttributeValue.builder_().s("test").build_()));

        Assert.assertNull(unconvert("getCustomString", "setCustomString",
                                    AttributeValue.builder_().s("ignoreme").build_()));
    }

    @Test
    public void testUuid() {
        UUID uuid = UUID.randomUUID();
        assertEquals(uuid, unconvert("getUuid", "setUuid",
                                     AttributeValue.builder_().s(uuid.toString()).build_()));
    }

    @Test
    public void testDate() {
        assertEquals(new Date(0), unconvert("getDate", "setDate",
                                            AttributeValue.builder_().s("1970-01-01T00:00:00.000Z").build_()));

        Calendar c = GregorianCalendar.getInstance();
        c.setTimeInMillis(0);

        assertEquals(c, unconvert("getCalendar", "setCalendar",
                                  AttributeValue.builder_().s("1970-01-01T00:00:00.000Z").build_()));
    }

    @Test
    public void testNumbers() {
        assertEquals((byte) 1, unconvert("getByte", "setByte",
                                         AttributeValue.builder_().n("1").build_()));
        assertEquals((byte) 1, unconvert("getBoxedByte", "setBoxedByte",
                                         AttributeValue.builder_().n("1").build_()));

        assertEquals((short) 1, unconvert("getShort", "setShort",
                                          AttributeValue.builder_().n("1").build_()));
        assertEquals((short) 1, unconvert("getBoxedShort", "setBoxedShort",
                                          AttributeValue.builder_().n("1").build_()));

        assertEquals(1, unconvert("getInt", "setInt",
                                  AttributeValue.builder_().n("1").build_()));
        assertEquals(1, unconvert("getBoxedInt", "setBoxedInt",
                                  AttributeValue.builder_().n("1").build_()));

        assertEquals(1l, unconvert("getLong", "setLong",
                                   AttributeValue.builder_().n("1").build_()));
        assertEquals(1l, unconvert("getBoxedLong", "setBoxedLong",
                                   AttributeValue.builder_().n("1").build_()));

        assertEquals(BigInteger.ONE, unconvert("getBigInt", "setBigInt",
                                               AttributeValue.builder_().n("1").build_()));

        assertEquals(1.5f, unconvert("getFloat", "setFloat",
                                     AttributeValue.builder_().n("1.5").build_()));
        assertEquals(1.5f, unconvert("getBoxedFloat", "setBoxedFloat",
                                     AttributeValue.builder_().n("1.5").build_()));

        assertEquals(1.5d, unconvert("getDouble", "setDouble",
                                     AttributeValue.builder_().n("1.5").build_()));
        assertEquals(1.5d, unconvert("getBoxedDouble", "setBoxedDouble",
                                     AttributeValue.builder_().n("1.5").build_()));

        assertEquals(BigDecimal.ONE, unconvert("getBigDecimal", "setBigDecimal",
                                               AttributeValue.builder_().n("1").build_()));
    }

    @Test
    public void testBinary() {
        ByteBuffer test = ByteBuffer.wrap("test".getBytes());
        Assert.assertTrue(Arrays.equals("test".getBytes(), (byte[]) unconvert(
                "getByteArray", "setByteArray",
                AttributeValue.builder_().b(test.slice()).build_())));

        assertEquals(test.slice(), unconvert("getByteBuffer", "setByteBuffer",
                                             AttributeValue.builder_().b(test.slice()).build_()));
    }

    @Test
    public void testBooleanSet() {
        assertEquals(new HashSet<Boolean>() {{
                         add(true);
                     }},
                     unconvert("getBooleanSet", "setBooleanSet",
                               AttributeValue.builder_().ns("1").build_()));

        assertEquals(new HashSet<Boolean>() {{
                         add(false);
                     }},
                     unconvert("getBooleanSet", "setBooleanSet",
                               AttributeValue.builder_().ns("0").build_()));

        assertEquals(new HashSet<Boolean>() {{
                         add(true);
                         add(false);
                     }},
                     unconvert("getBooleanSet", "setBooleanSet",
                               AttributeValue.builder_().ns("0", "1").build_()));

        assertEquals(new HashSet<Boolean>() {{
                         add(true);
                     }},
                     unconvert("getBooleanSet", "setBooleanSet",
                               AttributeValue.builder_().l(
                                       AttributeValue.builder_().bool(true).build_()).build_()));

        assertEquals(new HashSet<Boolean>() {{
                         add(false);
                     }},
                     unconvert("getBooleanSet", "setBooleanSet",
                               AttributeValue.builder_().l(
                                       AttributeValue.builder_().bool(false).build_()).build_()));

        assertEquals(new HashSet<Boolean>() {{
                         add(false);
                         add(true);
                     }},
                     unconvert("getBooleanSet", "setBooleanSet",
                               AttributeValue.builder_().l(
                                       AttributeValue.builder_().bool(false).build_(),
                                       AttributeValue.builder_().bool(true).build_()).build_()));

        assertEquals(new HashSet<Boolean>() {{
                         add(null);
                     }},
                     unconvert("getBooleanSet", "setBooleanSet",
                               AttributeValue.builder_().l(
                                       AttributeValue.builder_().nul(true).build_()).build_()));
    }

    @Test
    public void testStringSet() {
        Assert.assertNull(unconvert("getStringSet", "setStringSet",
                                    AttributeValue.builder_().nul(true).build_()));

        assertEquals(new HashSet<String>() {{
                         add("a");
                         add("b");
                     }},
                     unconvert("getStringSet", "setStringSet",
                               AttributeValue.builder_().ss("a", "b").build_()));
    }

    @Test
    public void testUuidSet() {
        Assert.assertNull(unconvert("getUuidSet", "setUuidSet",
                                    AttributeValue.builder_().nul(true).build_()));

        final UUID one = UUID.randomUUID();
        final UUID two = UUID.randomUUID();

        assertEquals(new HashSet<UUID>() {{
                         add(one);
                         add(two);
                     }},
                     unconvert("getUuidSet", "setUuidSet",
                               AttributeValue.builder_().ss(
                                       one.toString(),
                                       two.toString()).build_()));
    }

    @Test
    public void testDateSet() {
        assertEquals(Collections.singleton(new Date(0)),
                     unconvert("getDateSet", "setDateSet", AttributeValue.builder_()
                             .ss("1970-01-01T00:00:00.000Z").build_()));

        Calendar c = GregorianCalendar.getInstance();
        c.setTimeInMillis(0);

        assertEquals(Collections.singleton(c),
                     unconvert("getCalendarSet", "setCalendarSet",
                               AttributeValue.builder_()
                                       .ss("1970-01-01T00:00:00.000Z").build_()));
    }

    @Test
    public void testNumberSet() {
        Assert.assertNull(unconvert("getByteSet", "setByteSet",
                                    AttributeValue.builder_().nul(true).build_()));
        Assert.assertNull(unconvert("getShortSet", "setShortSet",
                                    AttributeValue.builder_().nul(true).build_()));
        Assert.assertNull(unconvert("getIntSet", "setIntSet",
                                    AttributeValue.builder_().nul(true).build_()));
        Assert.assertNull(unconvert("getLongSet", "setLongSet",
                                    AttributeValue.builder_().nul(true).build_()));
        Assert.assertNull(unconvert("getBigIntegerSet", "setBigIntegerSet",
                                    AttributeValue.builder_().nul(true).build_()));
        Assert.assertNull(unconvert("getFloatSet", "setFloatSet",
                                    AttributeValue.builder_().nul(true).build_()));
        Assert.assertNull(unconvert("getDoubleSet", "setDoubleSet",
                                    AttributeValue.builder_().nul(true).build_()));
        Assert.assertNull(unconvert("getBigDecimalSet", "setBigDecimalSet",
                                    AttributeValue.builder_().nul(true).build_()));


        assertEquals(new HashSet<Byte>() {{
                         add((byte) 1);
                     }},
                     unconvert("getByteSet", "setByteSet",
                               AttributeValue.builder_().ns("1").build_()));

        assertEquals(new HashSet<Short>() {{
                         add((short) 1);
                     }},
                     unconvert("getShortSet", "setShortSet",
                               AttributeValue.builder_().ns("1").build_()));

        assertEquals(new HashSet<Integer>() {{
                         add(1);
                     }},
                     unconvert("getIntSet", "setIntSet",
                               AttributeValue.builder_().ns("1").build_()));

        assertEquals(new HashSet<Long>() {{
                         add(1l);
                     }},
                     unconvert("getLongSet", "setLongSet",
                               AttributeValue.builder_().ns("1").build_()));

        assertEquals(new HashSet<BigInteger>() {{
                         add(BigInteger.ONE);
                     }},
                     unconvert("getBigIntegerSet", "setBigIntegerSet",
                               AttributeValue.builder_().ns("1").build_()));

        assertEquals(new HashSet<Float>() {{
                         add(1.5f);
                     }},
                     unconvert("getFloatSet", "setFloatSet",
                               AttributeValue.builder_().ns("1.5").build_()));

        assertEquals(new HashSet<Double>() {{
                         add(1.5d);
                     }},
                     unconvert("getDoubleSet", "setDoubleSet",
                               AttributeValue.builder_().ns("1.5").build_()));

        assertEquals(new HashSet<BigDecimal>() {{
                         add(BigDecimal.ONE);
                     }},
                     unconvert("getBigDecimalSet", "setBigDecimalSet",
                               AttributeValue.builder_().ns("1").build_()));
    }

    @Test
    public void testBinarySet() {
        Assert.assertNull(unconvert("getByteArraySet", "setByteArraySet",
                                    AttributeValue.builder_().nul(true).build_()));
        Assert.assertNull(unconvert("getByteBufferSet", "setByteBufferSet",
                                    AttributeValue.builder_().nul(true).build_()));

        ByteBuffer test = ByteBuffer.wrap("test".getBytes());

        Set<byte[]> result = (Set<byte[]>) unconvert(
                "getByteArraySet", "setByteArraySet",
                AttributeValue.builder_().bs(test.slice()).build_());

        assertEquals(1, result.size());
        Assert.assertTrue(Arrays.equals(
                "test".getBytes(),
                result.iterator().next()));

        Assert.assertEquals(Collections.singleton(test.slice()),
                            unconvert("getByteBufferSet", "setByteBufferSet",
                                      AttributeValue.builder_().bs(test.slice()).build_()));
    }

    @Test
    public void testObjectSet() {
        Object result = unconvert("getObjectSet", "setObjectSet",
                                  AttributeValue.builder_().l(AttributeValue.builder_().m(
                                          new HashMap<String, AttributeValue>() {{
                                              put("name", AttributeValue.builder_().s("name").build_());
                                              put("value", AttributeValue.builder_().n("123").build_());
                                              put("null", AttributeValue.builder_().nul(true).build_());
                                          }}
                                                                                       )
                                          .build_())
                                          .build_());

        assertEquals(Collections.singleton(new SubClass()), result);

        result = unconvert("getObjectSet", "setObjectSet",
                           AttributeValue.builder_().l(
                               AttributeValue.builder_()
                               .nul(true)
                               .build_())
                           .build_());

        assertEquals(Collections.<SubClass>singleton(null), result);
    }

    @Test
    public void testList() {
        Assert.assertNull(unconvert("getList", "setList",
                                    AttributeValue.builder_().nul(true).build_()));

        assertEquals(Arrays.asList("a", "b", "c"),
                     unconvert("getList", "setList", AttributeValue.builder_().l(
                             AttributeValue.builder_().s("a").build_(),
                             AttributeValue.builder_().s("b").build_(),
                             AttributeValue.builder_().s("c").build_())
                         .build_()));

        assertEquals(Arrays.asList("a", null),
                     unconvert("getList", "setList", AttributeValue.builder_().l(
                             AttributeValue.builder_().s("a").build_(),
                             AttributeValue.builder_().nul(true).build_()).build_()));
    }

    @Test
    public void testObjectList() {
        Assert.assertNull(unconvert("getObjectList", "setObjectList",
                                    AttributeValue.builder_().nul(true).build_()));

        assertEquals(Arrays.asList(new SubClass(), null),
                     unconvert("getObjectList", "setObjectList",
                               AttributeValue.builder_().l(
                                       AttributeValue.builder_().m(new HashMap<String, AttributeValue>() {{
                                           put("name", AttributeValue.builder_().s("name").build_());
                                           put("value", AttributeValue.builder_().n("123").build_());
                                           put("null", AttributeValue.builder_().nul(true).build_());
                                       }}).build_(),
                                       AttributeValue.builder_().nul(true).build_()).build_()));
    }

    @Test
    public void testSetList() {
        Assert.assertNull(unconvert("getSetList", "setSetList",
                                    AttributeValue.builder_().nul(true).build_()));

        assertEquals(Arrays.asList(new Set[] {null}),
                     unconvert("getSetList", "setSetList", AttributeValue.builder_().l(
                             AttributeValue.builder_().nul(true).build_()).build_()));

        assertEquals(Arrays.asList(Collections.singleton("a")),
                     unconvert("getSetList", "setSetList", AttributeValue.builder_().l(
                             AttributeValue.builder_().ss("a").build_()).build_()));
    }

    @Test
    public void testMap() {
        Assert.assertNull(unconvert("getMap", "setMap",
                                    AttributeValue.builder_().nul(true).build_()));

        assertEquals(new HashMap<String, String>() {{
                         put("a", "b");
                         put("c", "d");
                     }},
                     unconvert("getMap", "setMap", AttributeValue.builder_().m(
                             new HashMap<String, AttributeValue>() {{
                                 put("a", AttributeValue.builder_().s("b").build_());
                                 put("c", AttributeValue.builder_().s("d").build_());
                             }}).build_()));

        assertEquals(new HashMap<String, String>() {{
                         put("a", null);
                     }},
                     unconvert("getMap", "setMap", AttributeValue.builder_().m(
                             new HashMap<String, AttributeValue>() {{
                                 put("a", AttributeValue.builder_().nul(true).build_());
                             }}).build_()));
    }

    @Test
    public void testSetMap() {
        Assert.assertNull(unconvert("getSetMap", "setSetMap",
                                    AttributeValue.builder_().nul(true).build_()));

        assertEquals(new HashMap<String, Set<String>>() {{
                         put("a", null);
                         put("b", new TreeSet<String>(Arrays.asList("a", "b")));
                     }},
                     unconvert("getSetMap", "setSetMap", AttributeValue.builder_().m(
                             new HashMap<String, AttributeValue>() {{
                                 put("a", AttributeValue.builder_().nul(true).build_());
                                 put("b", AttributeValue.builder_().ss("a", "b").build_());
                             }}).build_()));
    }

    @Test
    public void testObject() {
        Assert.assertNull(unconvert("getObject", "setObject",
                                    AttributeValue.builder_().nul(true).build_()));

        assertEquals(new SubClass(), unconvert("getObject", "setObject",
                                               AttributeValue.builder_().m(new HashMap<String, AttributeValue>() {{
                                                   put("name", AttributeValue.builder_().s("name").build_());
                                                   put("value", AttributeValue.builder_().n("123").build_());
                                               }}).build_()));

        assertEquals(new SubClass(), unconvert("getObject", "setObject",
                                               AttributeValue.builder_().m(new HashMap<String, AttributeValue>() {{
                                                   put("name", AttributeValue.builder_().s("name").build_());
                                                   put("value", AttributeValue.builder_().n("123").build_());
                                                   put("null", AttributeValue.builder_().nul(true).build_());
                                               }}).build_()));
    }

    @Test
    public void testUnannotatedObject() throws Exception {
        Method getter = UnannotatedSubClass.class.getMethod("getChild");
        Method setter = UnannotatedSubClass.class
                .getMethod("setChild", UnannotatedSubClass.class);

        try {
            unconvert(UnannotatedSubClass.class, getter, setter, AttributeValue.builder_().s("").build_());
            Assert.fail("Expected DynamoDBMappingException");
        } catch (DynamoDbMappingException e) {
            // Ignored or expected.
        }
    }

    @Test
    public void testS3Link() {
        S3Link link = (S3Link) unconvert("getS3Link", "setS3Link",
                                         AttributeValue.builder_().s("{\"s3\":{"
                                                            + "\"bucket\":\"bucket\","
                                                            + "\"key\":\"key\","
                                                            + "\"region\":null}}").build_());

        assertEquals("bucket", link.bucketName());
        assertEquals("key", link.getKey());
        assertEquals(Region.US_Standard, link.s3Region());
    }

    public Object unconvert(String getter, String setter, AttributeValue value) {
        try {

            Method gm = TestClass.class.getMethod(getter);
            Method sm = TestClass.class.getMethod(setter, gm.getReturnType());
            return unconvert(TestClass.class, gm, sm, value);

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("BOOM", e);
        }
    }

}
