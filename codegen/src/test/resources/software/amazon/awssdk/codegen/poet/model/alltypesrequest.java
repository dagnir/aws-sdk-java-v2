package software.amazon.awssdk.services.jsonprotocoltests.model;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.AmazonWebServiceRequest;

public class AllTypesRequest extends AmazonWebServiceRequest implements Cloneable {
    private final String stringMember;

    private final Integer integerMember;

    private final Boolean booleanMember;

    private final Float floatMember;

    private final Double doubleMember;

    private final Long longMember;

    private final List<String> simpleList;

    private final List<Map<String, String>> listOfMaps;

    private final List<SimpleStruct> listOfStructs;

    private final Map<String, List<Integer>> mapOfStringToIntegerList;

    private final Map<String, String> mapOfStringToString;

    private final Map<String, SimpleStruct> mapOfStringToStruct;

    private final Date timestampMember;

    private final StructWithTimestamp structWithNestedTimestampMember;

    private final ByteBuffer blobArg;

    private final StructWithNestedBlobType structWithNestedBlob;

    private final Map<String, ByteBuffer> blobMap;

    private final List<ByteBuffer> listOfBlobs;

    private final RecursiveStructType recursiveStruct;

    private final BaseType polymorphicTypeWithSubTypes;

    private final SubTypeOne polymorphicTypeWithoutSubTypes;

    private AllTypesRequest(BeanStyleBuilder builder) {
        this.stringMember = builder.stringMember;
        this.integerMember = builder.integerMember;
        this.booleanMember = builder.booleanMember;
        this.floatMember = builder.floatMember;
        this.doubleMember = builder.doubleMember;
        this.longMember = builder.longMember;
        this.simpleList = builder.simpleList;
        this.listOfMaps = builder.listOfMaps;
        this.listOfStructs = builder.listOfStructs;
        this.mapOfStringToIntegerList = builder.mapOfStringToIntegerList;
        this.mapOfStringToString = builder.mapOfStringToString;
        this.mapOfStringToStruct = builder.mapOfStringToStruct;
        this.timestampMember = builder.timestampMember;
        this.structWithNestedTimestampMember = builder.structWithNestedTimestampMember;
        this.blobArg = builder.blobArg;
        this.structWithNestedBlob = builder.structWithNestedBlob;
        this.blobMap = builder.blobMap;
        this.listOfBlobs = builder.listOfBlobs;
        this.recursiveStruct = builder.recursiveStruct;
        this.polymorphicTypeWithSubTypes = builder.polymorphicTypeWithSubTypes;
        this.polymorphicTypeWithoutSubTypes = builder.polymorphicTypeWithoutSubTypes;
    }

    /**
     *
     * @return
     */
    public String stringMember() {
        return stringMember;
    }

    /**
     *
     * @return
     */
    public Integer integerMember() {
        return integerMember;
    }

    /**
     *
     * @return
     */
    public Boolean booleanMember() {
        return booleanMember;
    }

    /**
     *
     * @return
     */
    public Float floatMember() {
        return floatMember;
    }

    /**
     *
     * @return
     */
    public Double doubleMember() {
        return doubleMember;
    }

    /**
     *
     * @return
     */
    public Long longMember() {
        return longMember;
    }

    /**
     *
     * @return
     */
    public List<String> simpleList() {
        return simpleList;
    }

    /**
     *
     * @return
     */
    public List<Map<String, String>> listOfMaps() {
        return listOfMaps;
    }

    /**
     *
     * @return
     */
    public List<SimpleStruct> listOfStructs() {
        return listOfStructs;
    }

    /**
     *
     * @return
     */
    public Map<String, List<Integer>> mapOfStringToIntegerList() {
        return mapOfStringToIntegerList;
    }

    /**
     *
     * @return
     */
    public Map<String, String> mapOfStringToString() {
        return mapOfStringToString;
    }

    /**
     *
     * @return
     */
    public Map<String, SimpleStruct> mapOfStringToStruct() {
        return mapOfStringToStruct;
    }

    /**
     *
     * @return
     */
    public Date timestampMember() {
        return timestampMember;
    }

    /**
     *
     * @return
     */
    public StructWithTimestamp structWithNestedTimestampMember() {
        return structWithNestedTimestampMember;
    }

    /**
     *
     * <p>
     * {@code ByteBuffer}s are stateful. Calling their {@code get} methods changes their {@code position}. We recommend
     * using {@link java.nio.ByteBuffer#asReadOnlyBuffer()} to create a read-only view of the buffer with an independent
     * {@code position}, and calling {@code get} methods on this rather than directly on the returned {@code ByteBuffer}
     * . Doing so will ensure that anyone else using the {@code ByteBuffer} will not be affected by changes to the
     * {@code position}.
     * </p>
     *
     * @return
     */
    public ByteBuffer blobArg() {
        return blobArg;
    }

    /**
     *
     * @return
     */
    public StructWithNestedBlobType structWithNestedBlob() {
        return structWithNestedBlob;
    }

    /**
     *
     * @return
     */
    public Map<String, ByteBuffer> blobMap() {
        return blobMap;
    }

    /**
     *
     * @return
     */
    public List<ByteBuffer> listOfBlobs() {
        return listOfBlobs;
    }

    /**
     *
     * @return
     */
    public RecursiveStructType recursiveStruct() {
        return recursiveStruct;
    }

    /**
     *
     * @return
     */
    public BaseType polymorphicTypeWithSubTypes() {
        return polymorphicTypeWithSubTypes;
    }

    /**
     *
     * @return
     */
    public SubTypeOne polymorphicTypeWithoutSubTypes() {
        return polymorphicTypeWithoutSubTypes;
    }

    public Builder toBuilder() {
        return new BeanStyleBuilder(this);
    }

    public static Builder builder_() {
        return new BeanStyleBuilder();
    }

    public static Class<? extends Builder> beanStyleBuilderClass() {
        return BeanStyleBuilder.class;
    }

    @Override
    public int hashCode() {
        int hashCode = 1;
        hashCode = 31 * hashCode + ((stringMember() == null) ? 0 : stringMember().hashCode());
        hashCode = 31 * hashCode + ((integerMember() == null) ? 0 : integerMember().hashCode());
        hashCode = 31 * hashCode + ((booleanMember() == null) ? 0 : booleanMember().hashCode());
        hashCode = 31 * hashCode + ((floatMember() == null) ? 0 : floatMember().hashCode());
        hashCode = 31 * hashCode + ((doubleMember() == null) ? 0 : doubleMember().hashCode());
        hashCode = 31 * hashCode + ((longMember() == null) ? 0 : longMember().hashCode());
        hashCode = 31 * hashCode + ((simpleList() == null) ? 0 : simpleList().hashCode());
        hashCode = 31 * hashCode + ((listOfMaps() == null) ? 0 : listOfMaps().hashCode());
        hashCode = 31 * hashCode + ((listOfStructs() == null) ? 0 : listOfStructs().hashCode());
        hashCode = 31 * hashCode + ((mapOfStringToIntegerList() == null) ? 0 : mapOfStringToIntegerList().hashCode());
        hashCode = 31 * hashCode + ((mapOfStringToString() == null) ? 0 : mapOfStringToString().hashCode());
        hashCode = 31 * hashCode + ((mapOfStringToStruct() == null) ? 0 : mapOfStringToStruct().hashCode());
        hashCode = 31 * hashCode + ((timestampMember() == null) ? 0 : timestampMember().hashCode());
        hashCode = 31 * hashCode
                + ((structWithNestedTimestampMember() == null) ? 0 : structWithNestedTimestampMember().hashCode());
        hashCode = 31 * hashCode + ((blobArg() == null) ? 0 : blobArg().hashCode());
        hashCode = 31 * hashCode + ((structWithNestedBlob() == null) ? 0 : structWithNestedBlob().hashCode());
        hashCode = 31 * hashCode + ((blobMap() == null) ? 0 : blobMap().hashCode());
        hashCode = 31 * hashCode + ((listOfBlobs() == null) ? 0 : listOfBlobs().hashCode());
        hashCode = 31 * hashCode + ((recursiveStruct() == null) ? 0 : recursiveStruct().hashCode());
        hashCode = 31 * hashCode + ((polymorphicTypeWithSubTypes() == null) ? 0 : polymorphicTypeWithSubTypes().hashCode());
        hashCode = 31 * hashCode + ((polymorphicTypeWithoutSubTypes() == null) ? 0 : polymorphicTypeWithoutSubTypes().hashCode());
        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof AllTypesRequest)) {
            return false;
        }
        AllTypesRequest other = (AllTypesRequest) obj;
        if (other.stringMember() == null ^ this.stringMember() == null) {
            return false;
        }
        if (other.stringMember() != null && !other.stringMember().equals(this.stringMember())) {
            return false;
        }
        if (other.integerMember() == null ^ this.integerMember() == null) {
            return false;
        }
        if (other.integerMember() != null && !other.integerMember().equals(this.integerMember())) {
            return false;
        }
        if (other.booleanMember() == null ^ this.booleanMember() == null) {
            return false;
        }
        if (other.booleanMember() != null && !other.booleanMember().equals(this.booleanMember())) {
            return false;
        }
        if (other.floatMember() == null ^ this.floatMember() == null) {
            return false;
        }
        if (other.floatMember() != null && !other.floatMember().equals(this.floatMember())) {
            return false;
        }
        if (other.doubleMember() == null ^ this.doubleMember() == null) {
            return false;
        }
        if (other.doubleMember() != null && !other.doubleMember().equals(this.doubleMember())) {
            return false;
        }
        if (other.longMember() == null ^ this.longMember() == null) {
            return false;
        }
        if (other.longMember() != null && !other.longMember().equals(this.longMember())) {
            return false;
        }
        if (other.simpleList() == null ^ this.simpleList() == null) {
            return false;
        }
        if (other.simpleList() != null && !other.simpleList().equals(this.simpleList())) {
            return false;
        }
        if (other.listOfMaps() == null ^ this.listOfMaps() == null) {
            return false;
        }
        if (other.listOfMaps() != null && !other.listOfMaps().equals(this.listOfMaps())) {
            return false;
        }
        if (other.listOfStructs() == null ^ this.listOfStructs() == null) {
            return false;
        }
        if (other.listOfStructs() != null && !other.listOfStructs().equals(this.listOfStructs())) {
            return false;
        }
        if (other.mapOfStringToIntegerList() == null ^ this.mapOfStringToIntegerList() == null) {
            return false;
        }
        if (other.mapOfStringToIntegerList() != null && !other.mapOfStringToIntegerList().equals(this.mapOfStringToIntegerList())) {
            return false;
        }
        if (other.mapOfStringToString() == null ^ this.mapOfStringToString() == null) {
            return false;
        }
        if (other.mapOfStringToString() != null && !other.mapOfStringToString().equals(this.mapOfStringToString())) {
            return false;
        }
        if (other.mapOfStringToStruct() == null ^ this.mapOfStringToStruct() == null) {
            return false;
        }
        if (other.mapOfStringToStruct() != null && !other.mapOfStringToStruct().equals(this.mapOfStringToStruct())) {
            return false;
        }
        if (other.timestampMember() == null ^ this.timestampMember() == null) {
            return false;
        }
        if (other.timestampMember() != null && !other.timestampMember().equals(this.timestampMember())) {
            return false;
        }
        if (other.structWithNestedTimestampMember() == null ^ this.structWithNestedTimestampMember() == null) {
            return false;
        }
        if (other.structWithNestedTimestampMember() != null
                && !other.structWithNestedTimestampMember().equals(this.structWithNestedTimestampMember())) {
            return false;
        }
        if (other.blobArg() == null ^ this.blobArg() == null) {
            return false;
        }
        if (other.blobArg() != null && !other.blobArg().equals(this.blobArg())) {
            return false;
        }
        if (other.structWithNestedBlob() == null ^ this.structWithNestedBlob() == null) {
            return false;
        }
        if (other.structWithNestedBlob() != null && !other.structWithNestedBlob().equals(this.structWithNestedBlob())) {
            return false;
        }
        if (other.blobMap() == null ^ this.blobMap() == null) {
            return false;
        }
        if (other.blobMap() != null && !other.blobMap().equals(this.blobMap())) {
            return false;
        }
        if (other.listOfBlobs() == null ^ this.listOfBlobs() == null) {
            return false;
        }
        if (other.listOfBlobs() != null && !other.listOfBlobs().equals(this.listOfBlobs())) {
            return false;
        }
        if (other.recursiveStruct() == null ^ this.recursiveStruct() == null) {
            return false;
        }
        if (other.recursiveStruct() != null && !other.recursiveStruct().equals(this.recursiveStruct())) {
            return false;
        }
        if (other.polymorphicTypeWithSubTypes() == null ^ this.polymorphicTypeWithSubTypes() == null) {
            return false;
        }
        if (other.polymorphicTypeWithSubTypes() != null
                && !other.polymorphicTypeWithSubTypes().equals(this.polymorphicTypeWithSubTypes())) {
            return false;
        }
        if (other.polymorphicTypeWithoutSubTypes() == null ^ this.polymorphicTypeWithoutSubTypes() == null) {
            return false;
        }
        if (other.polymorphicTypeWithoutSubTypes() != null
                && !other.polymorphicTypeWithoutSubTypes().equals(this.polymorphicTypeWithoutSubTypes())) {
            return false;
        }
        return true;
    }

    @Override
    public AllTypesRequest clone() {
        try {
            return (AllTypesRequest) super.clone();
        } catch (Exception e) {
            throw new IllegalStateException("Got a CloneNotSupportedException from Object.clone() even though we're Cloneable!",
                    e);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        if (stringMember() != null) {
            sb.append("StringMember: ").append(stringMember()).append(",");
        }
        if (integerMember() != null) {
            sb.append("IntegerMember: ").append(integerMember()).append(",");
        }
        if (booleanMember() != null) {
            sb.append("BooleanMember: ").append(booleanMember()).append(",");
        }
        if (floatMember() != null) {
            sb.append("FloatMember: ").append(floatMember()).append(",");
        }
        if (doubleMember() != null) {
            sb.append("DoubleMember: ").append(doubleMember()).append(",");
        }
        if (longMember() != null) {
            sb.append("LongMember: ").append(longMember()).append(",");
        }
        if (simpleList() != null) {
            sb.append("SimpleList: ").append(simpleList()).append(",");
        }
        if (listOfMaps() != null) {
            sb.append("ListOfMaps: ").append(listOfMaps()).append(",");
        }
        if (listOfStructs() != null) {
            sb.append("ListOfStructs: ").append(listOfStructs()).append(",");
        }
        if (mapOfStringToIntegerList() != null) {
            sb.append("MapOfStringToIntegerList: ").append(mapOfStringToIntegerList()).append(",");
        }
        if (mapOfStringToString() != null) {
            sb.append("MapOfStringToString: ").append(mapOfStringToString()).append(",");
        }
        if (mapOfStringToStruct() != null) {
            sb.append("MapOfStringToStruct: ").append(mapOfStringToStruct()).append(",");
        }
        if (timestampMember() != null) {
            sb.append("TimestampMember: ").append(timestampMember()).append(",");
        }
        if (structWithNestedTimestampMember() != null) {
            sb.append("StructWithNestedTimestampMember: ").append(structWithNestedTimestampMember()).append(",");
        }
        if (blobArg() != null) {
            sb.append("BlobArg: ").append(blobArg()).append(",");
        }
        if (structWithNestedBlob() != null) {
            sb.append("StructWithNestedBlob: ").append(structWithNestedBlob()).append(",");
        }
        if (blobMap() != null) {
            sb.append("BlobMap: ").append(blobMap()).append(",");
        }
        if (listOfBlobs() != null) {
            sb.append("ListOfBlobs: ").append(listOfBlobs()).append(",");
        }
        if (recursiveStruct() != null) {
            sb.append("RecursiveStruct: ").append(recursiveStruct()).append(",");
        }
        if (polymorphicTypeWithSubTypes() != null) {
            sb.append("PolymorphicTypeWithSubTypes: ").append(polymorphicTypeWithSubTypes()).append(",");
        }
        if (polymorphicTypeWithoutSubTypes() != null) {
            sb.append("PolymorphicTypeWithoutSubTypes: ").append(polymorphicTypeWithoutSubTypes()).append(",");
        }
        sb.append("}");
        return sb.toString();
    }

    public interface Builder {
        /**
         *
         * @param stringMember
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder stringMember(String stringMember);

        /**
         *
         * @param integerMember
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder integerMember(Integer integerMember);

        /**
         *
         * @param booleanMember
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder booleanMember(Boolean booleanMember);

        /**
         *
         * @param floatMember
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder floatMember(Float floatMember);

        /**
         *
         * @param doubleMember
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder doubleMember(Double doubleMember);

        /**
         *
         * @param longMember
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder longMember(Long longMember);

        /**
         *
         * @param simpleList
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder simpleList(Collection<String> simpleList);

        /**
         *
         * @param simpleList
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder simpleList(String... simpleList);

        /**
         *
         * @param listOfMaps
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder listOfMaps(Collection<Map<String, String>> listOfMaps);

        /**
         *
         * @param listOfMaps
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder listOfMaps(Map<String, String>... listOfMaps);

        /**
         *
         * @param listOfStructs
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder listOfStructs(Collection<SimpleStruct> listOfStructs);

        /**
         *
         * @param listOfStructs
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder listOfStructs(SimpleStruct... listOfStructs);

        /**
         *
         * @param mapOfStringToIntegerList
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder mapOfStringToIntegerList(Map<String, List<Integer>> mapOfStringToIntegerList);

        /**
         *
         * @param mapOfStringToString
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder mapOfStringToString(Map<String, String> mapOfStringToString);

        /**
         *
         * @param mapOfStringToStruct
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder mapOfStringToStruct(Map<String, SimpleStruct> mapOfStringToStruct);

        /**
         *
         * @param timestampMember
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder timestampMember(Date timestampMember);

        /**
         *
         * @param structWithNestedTimestampMember
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder structWithNestedTimestampMember(StructWithTimestamp structWithNestedTimestampMember);

        /**
         *
         * @param blobArg
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder blobArg(ByteBuffer blobArg);

        /**
         *
         * @param structWithNestedBlob
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder structWithNestedBlob(StructWithNestedBlobType structWithNestedBlob);

        /**
         *
         * @param blobMap
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder blobMap(Map<String, ByteBuffer> blobMap);

        /**
         *
         * @param listOfBlobs
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder listOfBlobs(Collection<ByteBuffer> listOfBlobs);

        /**
         *
         * @param listOfBlobs
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder listOfBlobs(ByteBuffer... listOfBlobs);

        /**
         *
         * @param recursiveStruct
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder recursiveStruct(RecursiveStructType recursiveStruct);

        /**
         *
         * @param polymorphicTypeWithSubTypes
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder polymorphicTypeWithSubTypes(BaseType polymorphicTypeWithSubTypes);

        /**
         *
         * @param polymorphicTypeWithoutSubTypes
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder polymorphicTypeWithoutSubTypes(SubTypeOne polymorphicTypeWithoutSubTypes);

        AllTypesRequest build_();
    }

    private static final class BeanStyleBuilder implements Builder {
        private String stringMember;

        private Integer integerMember;

        private Boolean booleanMember;

        private Float floatMember;

        private Double doubleMember;

        private Long longMember;

        private List<String> simpleList;

        private List<Map<String, String>> listOfMaps;

        private List<SimpleStruct> listOfStructs;

        private Map<String, List<Integer>> mapOfStringToIntegerList;

        private Map<String, String> mapOfStringToString;

        private Map<String, SimpleStruct> mapOfStringToStruct;

        private Date timestampMember;

        private StructWithTimestamp structWithNestedTimestampMember;

        private ByteBuffer blobArg;

        private StructWithNestedBlobType structWithNestedBlob;

        private Map<String, ByteBuffer> blobMap;

        private List<ByteBuffer> listOfBlobs;

        private RecursiveStructType recursiveStruct;

        private BaseType polymorphicTypeWithSubTypes;

        private SubTypeOne polymorphicTypeWithoutSubTypes;

        private BeanStyleBuilder() {
        }

        private BeanStyleBuilder(AllTypesRequest model) {
            this.stringMember = model.stringMember;
            this.integerMember = model.integerMember;
            this.booleanMember = model.booleanMember;
            this.floatMember = model.floatMember;
            this.doubleMember = model.doubleMember;
            this.longMember = model.longMember;
            this.simpleList = model.simpleList;
            this.listOfMaps = model.listOfMaps;
            this.listOfStructs = model.listOfStructs;
            this.mapOfStringToIntegerList = model.mapOfStringToIntegerList;
            this.mapOfStringToString = model.mapOfStringToString;
            this.mapOfStringToStruct = model.mapOfStringToStruct;
            this.timestampMember = model.timestampMember;
            this.structWithNestedTimestampMember = model.structWithNestedTimestampMember;
            this.blobArg = model.blobArg;
            this.structWithNestedBlob = model.structWithNestedBlob;
            this.blobMap = model.blobMap;
            this.listOfBlobs = model.listOfBlobs;
            this.recursiveStruct = model.recursiveStruct;
            this.polymorphicTypeWithSubTypes = model.polymorphicTypeWithSubTypes;
            this.polymorphicTypeWithoutSubTypes = model.polymorphicTypeWithoutSubTypes;
        }

        @Override
        public Builder stringMember(String stringMember) {
            this.stringMember = stringMember;
            return this;
        }

        /**
         *
         * @param stringMember
         */
        public void setStringMember(String stringMember) {
            this.stringMember = stringMember;
        }

        @Override
        public Builder integerMember(Integer integerMember) {
            this.integerMember = integerMember;
            return this;
        }

        /**
         *
         * @param integerMember
         */
        public void setIntegerMember(Integer integerMember) {
            this.integerMember = integerMember;
        }

        @Override
        public Builder booleanMember(Boolean booleanMember) {
            this.booleanMember = booleanMember;
            return this;
        }

        /**
         *
         * @param booleanMember
         */
        public void setBooleanMember(Boolean booleanMember) {
            this.booleanMember = booleanMember;
        }

        @Override
        public Builder floatMember(Float floatMember) {
            this.floatMember = floatMember;
            return this;
        }

        /**
         *
         * @param floatMember
         */
        public void setFloatMember(Float floatMember) {
            this.floatMember = floatMember;
        }

        @Override
        public Builder doubleMember(Double doubleMember) {
            this.doubleMember = doubleMember;
            return this;
        }

        /**
         *
         * @param doubleMember
         */
        public void setDoubleMember(Double doubleMember) {
            this.doubleMember = doubleMember;
        }

        @Override
        public Builder longMember(Long longMember) {
            this.longMember = longMember;
            return this;
        }

        /**
         *
         * @param longMember
         */
        public void setLongMember(Long longMember) {
            this.longMember = longMember;
        }

        @Override
        public Builder simpleList(Collection simpleList) {
            if (simpleList == null) {
                this.simpleList = null;
            } else {
                this.simpleList = new ArrayList<String>(simpleList);
            }
            return this;
        }

        @Override
        public Builder simpleList(String... simpleList) {
            if (this.simpleList == null) {
                this.simpleList = new ArrayList<String>(simpleList.length);
            }
            for (String ele : simpleList) {
                this.simpleList.add(ele);
            }
            return this;
        }

        /**
         *
         * @param simpleList
         */
        public void setSimpleList(Collection simpleList) {
            if (simpleList == null) {
                this.simpleList = null;
            } else {
                this.simpleList = new ArrayList<String>(simpleList);
            }
        }

        /**
         *
         * @param simpleList
         */
        public void setSimpleList(String... simpleList) {
            if (this.simpleList == null) {
                this.simpleList = new ArrayList<String>(simpleList.length);
            }
            for (String ele : simpleList) {
                this.simpleList.add(ele);
            }
        }

        @Override
        public Builder listOfMaps(Collection listOfMaps) {
            if (listOfMaps == null) {
                this.listOfMaps = null;
            } else {
                this.listOfMaps = new ArrayList<Map<String, String>>(listOfMaps);
            }
            return this;
        }

        @Override
        public Builder listOfMaps(Map<String, String>... listOfMaps) {
            if (this.listOfMaps == null) {
                this.listOfMaps = new ArrayList<Map<String, String>>(listOfMaps.length);
            }
            for (Map<String, String> ele : listOfMaps) {
                this.listOfMaps.add(ele);
            }
            return this;
        }

        /**
         *
         * @param listOfMaps
         */
        public void setListOfMaps(Collection listOfMaps) {
            if (listOfMaps == null) {
                this.listOfMaps = null;
            } else {
                this.listOfMaps = new ArrayList<Map<String, String>>(listOfMaps);
            }
        }

        /**
         *
         * @param listOfMaps
         */
        public void setListOfMaps(Map<String, String>... listOfMaps) {
            if (this.listOfMaps == null) {
                this.listOfMaps = new ArrayList<Map<String, String>>(listOfMaps.length);
            }
            for (Map<String, String> ele : listOfMaps) {
                this.listOfMaps.add(ele);
            }
        }

        @Override
        public Builder listOfStructs(Collection listOfStructs) {
            if (listOfStructs == null) {
                this.listOfStructs = null;
            } else {
                this.listOfStructs = new ArrayList<SimpleStruct>(listOfStructs);
            }
            return this;
        }

        @Override
        public Builder listOfStructs(SimpleStruct... listOfStructs) {
            if (this.listOfStructs == null) {
                this.listOfStructs = new ArrayList<SimpleStruct>(listOfStructs.length);
            }
            for (SimpleStruct ele : listOfStructs) {
                this.listOfStructs.add(ele);
            }
            return this;
        }

        /**
         *
         * @param listOfStructs
         */
        public void setListOfStructs(Collection listOfStructs) {
            if (listOfStructs == null) {
                this.listOfStructs = null;
            } else {
                this.listOfStructs = new ArrayList<SimpleStruct>(listOfStructs);
            }
        }

        /**
         *
         * @param listOfStructs
         */
        public void setListOfStructs(SimpleStruct... listOfStructs) {
            if (this.listOfStructs == null) {
                this.listOfStructs = new ArrayList<SimpleStruct>(listOfStructs.length);
            }
            for (SimpleStruct ele : listOfStructs) {
                this.listOfStructs.add(ele);
            }
        }

        @Override
        public Builder mapOfStringToIntegerList(Map<String, List<Integer>> mapOfStringToIntegerList) {
            if (mapOfStringToIntegerList == null) {
                this.mapOfStringToIntegerList = null;
            } else {
                this.mapOfStringToIntegerList = new HashMap<String, List<Integer>>(mapOfStringToIntegerList);
            }
            return this;
        }

        /**
         *
         * @param mapOfStringToIntegerList
         */
        public void setMapOfStringToIntegerList(Map<String, List<Integer>> mapOfStringToIntegerList) {
            if (mapOfStringToIntegerList == null) {
                this.mapOfStringToIntegerList = null;
            } else {
                this.mapOfStringToIntegerList = new HashMap<String, List<Integer>>(mapOfStringToIntegerList);
            }
        }

        @Override
        public Builder mapOfStringToString(Map<String, String> mapOfStringToString) {
            if (mapOfStringToString == null) {
                this.mapOfStringToString = null;
            } else {
                this.mapOfStringToString = new HashMap<String, String>(mapOfStringToString);
            }
            return this;
        }

        /**
         *
         * @param mapOfStringToString
         */
        public void setMapOfStringToString(Map<String, String> mapOfStringToString) {
            if (mapOfStringToString == null) {
                this.mapOfStringToString = null;
            } else {
                this.mapOfStringToString = new HashMap<String, String>(mapOfStringToString);
            }
        }

        @Override
        public Builder mapOfStringToStruct(Map<String, SimpleStruct> mapOfStringToStruct) {
            if (mapOfStringToStruct == null) {
                this.mapOfStringToStruct = null;
            } else {
                this.mapOfStringToStruct = new HashMap<String, SimpleStruct>(mapOfStringToStruct);
            }
            return this;
        }

        /**
         *
         * @param mapOfStringToStruct
         */
        public void setMapOfStringToStruct(Map<String, SimpleStruct> mapOfStringToStruct) {
            if (mapOfStringToStruct == null) {
                this.mapOfStringToStruct = null;
            } else {
                this.mapOfStringToStruct = new HashMap<String, SimpleStruct>(mapOfStringToStruct);
            }
        }

        @Override
        public Builder timestampMember(Date timestampMember) {
            this.timestampMember = timestampMember;
            return this;
        }

        /**
         *
         * @param timestampMember
         */
        public void setTimestampMember(Date timestampMember) {
            this.timestampMember = timestampMember;
        }

        @Override
        public Builder structWithNestedTimestampMember(StructWithTimestamp structWithNestedTimestampMember) {
            this.structWithNestedTimestampMember = structWithNestedTimestampMember;
            return this;
        }

        /**
         *
         * @param structWithNestedTimestampMember
         */
        public void setStructWithNestedTimestampMember(StructWithTimestamp structWithNestedTimestampMember) {
            this.structWithNestedTimestampMember = structWithNestedTimestampMember;
        }

        @Override
        public Builder blobArg(ByteBuffer blobArg) {
            this.blobArg = blobArg;
            return this;
        }

        /**
         * <p>
         * AWS SDK for Java performs a Base64 encoding on this field before sending this request to AWS service by
         * default. Users of the SDK should not perform Base64 encoding on this field.
         * </p>
         * <p>
         * Warning: ByteBuffers returned by the SDK are mutable. Changes to the content or position of the byte buffer
         * will be seen by all objects that have a reference to this object. It is recommended to call
         * ByteBuffer.duplicate() or ByteBuffer.asReadOnlyBuffer() before using or reading from the buffer. This
         * behavior will be changed in a future major version of the SDK.
         * </p>
         *
         * @param blobArg
         */
        public void setBlobArg(ByteBuffer blobArg) {
            this.blobArg = blobArg;
        }

        @Override
        public Builder structWithNestedBlob(StructWithNestedBlobType structWithNestedBlob) {
            this.structWithNestedBlob = structWithNestedBlob;
            return this;
        }

        /**
         *
         * @param structWithNestedBlob
         */
        public void setStructWithNestedBlob(StructWithNestedBlobType structWithNestedBlob) {
            this.structWithNestedBlob = structWithNestedBlob;
        }

        @Override
        public Builder blobMap(Map<String, ByteBuffer> blobMap) {
            if (blobMap == null) {
                this.blobMap = null;
            } else {
                this.blobMap = new HashMap<String, ByteBuffer>(blobMap);
            }
            return this;
        }

        /**
         *
         * @param blobMap
         */
        public void setBlobMap(Map<String, ByteBuffer> blobMap) {
            if (blobMap == null) {
                this.blobMap = null;
            } else {
                this.blobMap = new HashMap<String, ByteBuffer>(blobMap);
            }
        }

        @Override
        public Builder listOfBlobs(Collection listOfBlobs) {
            if (listOfBlobs == null) {
                this.listOfBlobs = null;
            } else {
                this.listOfBlobs = new ArrayList<ByteBuffer>(listOfBlobs);
            }
            return this;
        }

        @Override
        public Builder listOfBlobs(ByteBuffer... listOfBlobs) {
            if (this.listOfBlobs == null) {
                this.listOfBlobs = new ArrayList<ByteBuffer>(listOfBlobs.length);
            }
            for (ByteBuffer ele : listOfBlobs) {
                this.listOfBlobs.add(ele);
            }
            return this;
        }

        /**
         *
         * @param listOfBlobs
         */
        public void setListOfBlobs(Collection listOfBlobs) {
            if (listOfBlobs == null) {
                this.listOfBlobs = null;
            } else {
                this.listOfBlobs = new ArrayList<ByteBuffer>(listOfBlobs);
            }
        }

        /**
         *
         * @param listOfBlobs
         */
        public void setListOfBlobs(ByteBuffer... listOfBlobs) {
            if (this.listOfBlobs == null) {
                this.listOfBlobs = new ArrayList<ByteBuffer>(listOfBlobs.length);
            }
            for (ByteBuffer ele : listOfBlobs) {
                this.listOfBlobs.add(ele);
            }
        }

        @Override
        public Builder recursiveStruct(RecursiveStructType recursiveStruct) {
            this.recursiveStruct = recursiveStruct;
            return this;
        }

        /**
         *
         * @param recursiveStruct
         */
        public void setRecursiveStruct(RecursiveStructType recursiveStruct) {
            this.recursiveStruct = recursiveStruct;
        }

        @Override
        public Builder polymorphicTypeWithSubTypes(BaseType polymorphicTypeWithSubTypes) {
            this.polymorphicTypeWithSubTypes = polymorphicTypeWithSubTypes;
            return this;
        }

        /**
         *
         * @param polymorphicTypeWithSubTypes
         */
        public void setPolymorphicTypeWithSubTypes(BaseType polymorphicTypeWithSubTypes) {
            this.polymorphicTypeWithSubTypes = polymorphicTypeWithSubTypes;
        }

        @Override
        public Builder polymorphicTypeWithoutSubTypes(SubTypeOne polymorphicTypeWithoutSubTypes) {
            this.polymorphicTypeWithoutSubTypes = polymorphicTypeWithoutSubTypes;
            return this;
        }

        /**
         *
         * @param polymorphicTypeWithoutSubTypes
         */
        public void setPolymorphicTypeWithoutSubTypes(SubTypeOne polymorphicTypeWithoutSubTypes) {
            this.polymorphicTypeWithoutSubTypes = polymorphicTypeWithoutSubTypes;
        }

        @Override
        public AllTypesRequest build_() {
            return new AllTypesRequest(this);
        }
    }
}
