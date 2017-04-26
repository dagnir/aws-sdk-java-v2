package software.amazon.awssdk.services.jsonprotocoltests.model;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.AmazonWebServiceRequest;

public class AllTypesRequest extends AmazonWebServiceRequest implements Serializable, Cloneable {
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

    private AllTypesRequest(Builder builder) {
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
     * turn
     */
    public String getStringMember() {
        return stringMember;
    }

    /**
     * turn
     */
    public Integer getIntegerMember() {
        return integerMember;
    }

    /**
     * turn
     */
    public Boolean getBooleanMember() {
        return booleanMember;
    }

    /**
     * turn
     */
    public Float getFloatMember() {
        return floatMember;
    }

    /**
     * turn
     */
    public Double getDoubleMember() {
        return doubleMember;
    }

    /**
     * turn
     */
    public Long getLongMember() {
        return longMember;
    }

    /**
     * turn
     */
    public List<String> getSimpleList() {
        return simpleList;
    }

    /**
     * turn
     */
    public List<Map<String, String>> getListOfMaps() {
        return listOfMaps;
    }

    /**
     * turn
     */
    public List<SimpleStruct> getListOfStructs() {
        return listOfStructs;
    }

    /**
     * turn
     */
    public Map<String, List<Integer>> getMapOfStringToIntegerList() {
        return mapOfStringToIntegerList;
    }

    /**
     * turn
     */
    public Map<String, String> getMapOfStringToString() {
        return mapOfStringToString;
    }

    /**
     * turn
     */
    public Map<String, SimpleStruct> getMapOfStringToStruct() {
        return mapOfStringToStruct;
    }

    /**
     * turn
     */
    public Date getTimestampMember() {
        return timestampMember;
    }

    /**
     * turn
     */
    public StructWithTimestamp getStructWithNestedTimestampMember() {
        return structWithNestedTimestampMember;
    }

    /**
     *
     * {@code ByteBuffer}s are stateful. Calling their {@code get} methods changes their {@code position}. We recommend
     * using {@link java.nio.ByteBuffer#asReadOnlyBuffer()} to create a read-only view of the buffer with an independent
     * {@code position}, and calling {@code get} methods on this rather than directly on the returned {@code ByteBuffer}
     * . Doing so will ensure that anyone else using the {@code ByteBuffer} will not be affected by changes to the
     * {@code position}. </p>
     * 
     * @return
     */
    public ByteBuffer getBlobArg() {
        return blobArg;
    }

    /**
     * turn
     */
    public StructWithNestedBlobType getStructWithNestedBlob() {
        return structWithNestedBlob;
    }

    /**
     * turn
     */
    public Map<String, ByteBuffer> getBlobMap() {
        return blobMap;
    }

    /**
     * turn
     */
    public List<ByteBuffer> getListOfBlobs() {
        return listOfBlobs;
    }

    /**
     * turn
     */
    public RecursiveStructType getRecursiveStruct() {
        return recursiveStruct;
    }

    /**
     * turn
     */
    public BaseType getPolymorphicTypeWithSubTypes() {
        return polymorphicTypeWithSubTypes;
    }

    /**
     * turn
     */
    public SubTypeOne getPolymorphicTypeWithoutSubTypes() {
        return polymorphicTypeWithoutSubTypes;
    }

    public static Builder builder_() {
        return new Builder();
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    @Override
    public int hashCode() {
        int hashCode = 1;
        hashCode = 31 * hashCode + ((getStringMember() == null) ? 0 : getStringMember().hashCode());
        hashCode = 31 * hashCode + ((getIntegerMember() == null) ? 0 : getIntegerMember().hashCode());
        hashCode = 31 * hashCode + ((getBooleanMember() == null) ? 0 : getBooleanMember().hashCode());
        hashCode = 31 * hashCode + ((getFloatMember() == null) ? 0 : getFloatMember().hashCode());
        hashCode = 31 * hashCode + ((getDoubleMember() == null) ? 0 : getDoubleMember().hashCode());
        hashCode = 31 * hashCode + ((getLongMember() == null) ? 0 : getLongMember().hashCode());
        hashCode = 31 * hashCode + ((getSimpleList() == null) ? 0 : getSimpleList().hashCode());
        hashCode = 31 * hashCode + ((getListOfMaps() == null) ? 0 : getListOfMaps().hashCode());
        hashCode = 31 * hashCode + ((getListOfStructs() == null) ? 0 : getListOfStructs().hashCode());
        hashCode = 31 * hashCode + ((getMapOfStringToIntegerList() == null) ? 0 : getMapOfStringToIntegerList().hashCode());
        hashCode = 31 * hashCode + ((getMapOfStringToString() == null) ? 0 : getMapOfStringToString().hashCode());
        hashCode = 31 * hashCode + ((getMapOfStringToStruct() == null) ? 0 : getMapOfStringToStruct().hashCode());
        hashCode = 31 * hashCode + ((getTimestampMember() == null) ? 0 : getTimestampMember().hashCode());
        hashCode = 31 * hashCode
                + ((getStructWithNestedTimestampMember() == null) ? 0 : getStructWithNestedTimestampMember().hashCode());
        hashCode = 31 * hashCode + ((getBlobArg() == null) ? 0 : getBlobArg().hashCode());
        hashCode = 31 * hashCode + ((getStructWithNestedBlob() == null) ? 0 : getStructWithNestedBlob().hashCode());
        hashCode = 31 * hashCode + ((getBlobMap() == null) ? 0 : getBlobMap().hashCode());
        hashCode = 31 * hashCode + ((getListOfBlobs() == null) ? 0 : getListOfBlobs().hashCode());
        hashCode = 31 * hashCode + ((getRecursiveStruct() == null) ? 0 : getRecursiveStruct().hashCode());
        hashCode = 31 * hashCode + ((getPolymorphicTypeWithSubTypes() == null) ? 0 : getPolymorphicTypeWithSubTypes().hashCode());
        hashCode = 31 * hashCode
                + ((getPolymorphicTypeWithoutSubTypes() == null) ? 0 : getPolymorphicTypeWithoutSubTypes().hashCode());
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
        if (other.getStringMember() == null ^ this.getStringMember() == null) {
            return false;
        }
        if (other.getStringMember() != null && !other.getStringMember().equals(this.getStringMember())) {
            return false;
        }
        if (other.getIntegerMember() == null ^ this.getIntegerMember() == null) {
            return false;
        }
        if (other.getIntegerMember() != null && !other.getIntegerMember().equals(this.getIntegerMember())) {
            return false;
        }
        if (other.getBooleanMember() == null ^ this.getBooleanMember() == null) {
            return false;
        }
        if (other.getBooleanMember() != null && !other.getBooleanMember().equals(this.getBooleanMember())) {
            return false;
        }
        if (other.getFloatMember() == null ^ this.getFloatMember() == null) {
            return false;
        }
        if (other.getFloatMember() != null && !other.getFloatMember().equals(this.getFloatMember())) {
            return false;
        }
        if (other.getDoubleMember() == null ^ this.getDoubleMember() == null) {
            return false;
        }
        if (other.getDoubleMember() != null && !other.getDoubleMember().equals(this.getDoubleMember())) {
            return false;
        }
        if (other.getLongMember() == null ^ this.getLongMember() == null) {
            return false;
        }
        if (other.getLongMember() != null && !other.getLongMember().equals(this.getLongMember())) {
            return false;
        }
        if (other.getSimpleList() == null ^ this.getSimpleList() == null) {
            return false;
        }
        if (other.getSimpleList() != null && !other.getSimpleList().equals(this.getSimpleList())) {
            return false;
        }
        if (other.getListOfMaps() == null ^ this.getListOfMaps() == null) {
            return false;
        }
        if (other.getListOfMaps() != null && !other.getListOfMaps().equals(this.getListOfMaps())) {
            return false;
        }
        if (other.getListOfStructs() == null ^ this.getListOfStructs() == null) {
            return false;
        }
        if (other.getListOfStructs() != null && !other.getListOfStructs().equals(this.getListOfStructs())) {
            return false;
        }
        if (other.getMapOfStringToIntegerList() == null ^ this.getMapOfStringToIntegerList() == null) {
            return false;
        }
        if (other.getMapOfStringToIntegerList() != null
                && !other.getMapOfStringToIntegerList().equals(this.getMapOfStringToIntegerList())) {
            return false;
        }
        if (other.getMapOfStringToString() == null ^ this.getMapOfStringToString() == null) {
            return false;
        }
        if (other.getMapOfStringToString() != null && !other.getMapOfStringToString().equals(this.getMapOfStringToString())) {
            return false;
        }
        if (other.getMapOfStringToStruct() == null ^ this.getMapOfStringToStruct() == null) {
            return false;
        }
        if (other.getMapOfStringToStruct() != null && !other.getMapOfStringToStruct().equals(this.getMapOfStringToStruct())) {
            return false;
        }
        if (other.getTimestampMember() == null ^ this.getTimestampMember() == null) {
            return false;
        }
        if (other.getTimestampMember() != null && !other.getTimestampMember().equals(this.getTimestampMember())) {
            return false;
        }
        if (other.getStructWithNestedTimestampMember() == null ^ this.getStructWithNestedTimestampMember() == null) {
            return false;
        }
        if (other.getStructWithNestedTimestampMember() != null
                && !other.getStructWithNestedTimestampMember().equals(this.getStructWithNestedTimestampMember())) {
            return false;
        }
        if (other.getBlobArg() == null ^ this.getBlobArg() == null) {
            return false;
        }
        if (other.getBlobArg() != null && !other.getBlobArg().equals(this.getBlobArg())) {
            return false;
        }
        if (other.getStructWithNestedBlob() == null ^ this.getStructWithNestedBlob() == null) {
            return false;
        }
        if (other.getStructWithNestedBlob() != null && !other.getStructWithNestedBlob().equals(this.getStructWithNestedBlob())) {
            return false;
        }
        if (other.getBlobMap() == null ^ this.getBlobMap() == null) {
            return false;
        }
        if (other.getBlobMap() != null && !other.getBlobMap().equals(this.getBlobMap())) {
            return false;
        }
        if (other.getListOfBlobs() == null ^ this.getListOfBlobs() == null) {
            return false;
        }
        if (other.getListOfBlobs() != null && !other.getListOfBlobs().equals(this.getListOfBlobs())) {
            return false;
        }
        if (other.getRecursiveStruct() == null ^ this.getRecursiveStruct() == null) {
            return false;
        }
        if (other.getRecursiveStruct() != null && !other.getRecursiveStruct().equals(this.getRecursiveStruct())) {
            return false;
        }
        if (other.getPolymorphicTypeWithSubTypes() == null ^ this.getPolymorphicTypeWithSubTypes() == null) {
            return false;
        }
        if (other.getPolymorphicTypeWithSubTypes() != null
                && !other.getPolymorphicTypeWithSubTypes().equals(this.getPolymorphicTypeWithSubTypes())) {
            return false;
        }
        if (other.getPolymorphicTypeWithoutSubTypes() == null ^ this.getPolymorphicTypeWithoutSubTypes() == null) {
            return false;
        }
        if (other.getPolymorphicTypeWithoutSubTypes() != null
                && !other.getPolymorphicTypeWithoutSubTypes().equals(this.getPolymorphicTypeWithoutSubTypes())) {
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
        if (getStringMember() != null) {
            sb.append("StringMember: ").append(getStringMember()).append(",");
        }
        if (getIntegerMember() != null) {
            sb.append("IntegerMember: ").append(getIntegerMember()).append(",");
        }
        if (getBooleanMember() != null) {
            sb.append("BooleanMember: ").append(getBooleanMember()).append(",");
        }
        if (getFloatMember() != null) {
            sb.append("FloatMember: ").append(getFloatMember()).append(",");
        }
        if (getDoubleMember() != null) {
            sb.append("DoubleMember: ").append(getDoubleMember()).append(",");
        }
        if (getLongMember() != null) {
            sb.append("LongMember: ").append(getLongMember()).append(",");
        }
        if (getSimpleList() != null) {
            sb.append("SimpleList: ").append(getSimpleList()).append(",");
        }
        if (getListOfMaps() != null) {
            sb.append("ListOfMaps: ").append(getListOfMaps()).append(",");
        }
        if (getListOfStructs() != null) {
            sb.append("ListOfStructs: ").append(getListOfStructs()).append(",");
        }
        if (getMapOfStringToIntegerList() != null) {
            sb.append("MapOfStringToIntegerList: ").append(getMapOfStringToIntegerList()).append(",");
        }
        if (getMapOfStringToString() != null) {
            sb.append("MapOfStringToString: ").append(getMapOfStringToString()).append(",");
        }
        if (getMapOfStringToStruct() != null) {
            sb.append("MapOfStringToStruct: ").append(getMapOfStringToStruct()).append(",");
        }
        if (getTimestampMember() != null) {
            sb.append("TimestampMember: ").append(getTimestampMember()).append(",");
        }
        if (getStructWithNestedTimestampMember() != null) {
            sb.append("StructWithNestedTimestampMember: ").append(getStructWithNestedTimestampMember()).append(",");
        }
        if (getBlobArg() != null) {
            sb.append("BlobArg: ").append(getBlobArg()).append(",");
        }
        if (getStructWithNestedBlob() != null) {
            sb.append("StructWithNestedBlob: ").append(getStructWithNestedBlob()).append(",");
        }
        if (getBlobMap() != null) {
            sb.append("BlobMap: ").append(getBlobMap()).append(",");
        }
        if (getListOfBlobs() != null) {
            sb.append("ListOfBlobs: ").append(getListOfBlobs()).append(",");
        }
        if (getRecursiveStruct() != null) {
            sb.append("RecursiveStruct: ").append(getRecursiveStruct()).append(",");
        }
        if (getPolymorphicTypeWithSubTypes() != null) {
            sb.append("PolymorphicTypeWithSubTypes: ").append(getPolymorphicTypeWithSubTypes()).append(",");
        }
        if (getPolymorphicTypeWithoutSubTypes() != null) {
            sb.append("PolymorphicTypeWithoutSubTypes: ").append(getPolymorphicTypeWithoutSubTypes()).append(",");
        }
        sb.append("}");
        return sb.toString();
    }

    public static class Builder {
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

        private Builder() {
        }

        private Builder(AllTypesRequest model) {
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

        /**
         *
         * @param stringMember
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        public Builder setStringMember(String stringMember) {
            this.stringMember = stringMember;
            return this;
        }

        /**
         *
         * @param integerMember
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        public Builder setIntegerMember(Integer integerMember) {
            this.integerMember = integerMember;
            return this;
        }

        /**
         *
         * @param booleanMember
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        public Builder setBooleanMember(Boolean booleanMember) {
            this.booleanMember = booleanMember;
            return this;
        }

        /**
         *
         * @param floatMember
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        public Builder setFloatMember(Float floatMember) {
            this.floatMember = floatMember;
            return this;
        }

        /**
         *
         * @param doubleMember
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        public Builder setDoubleMember(Double doubleMember) {
            this.doubleMember = doubleMember;
            return this;
        }

        /**
         *
         * @param longMember
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        public Builder setLongMember(Long longMember) {
            this.longMember = longMember;
            return this;
        }

        /**
         *
         * @param simpleList
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        public Builder setSimpleList(List<String> simpleList) {
            if (simpleList == null) {
                this.simpleList = null;
            } else {
                this.simpleList = new ArrayList<String>(simpleList);
            }
            return this;
        }

        /**
         *
         * @param simpleList
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        public Builder simpleList(String... simpleList) {
            if (this.simpleList == null) {
                this.simpleList = new ArrayList<>(simpleList.length);
            }
            for (String ele : simpleList) {
                this.simpleList.add(ele);
            }
            return this;
        }

        /**
         *
         * @param listOfMaps
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        public Builder setListOfMaps(List<Map<String, String>> listOfMaps) {
            if (listOfMaps == null) {
                this.listOfMaps = null;
            } else {
                this.listOfMaps = new ArrayList<Map<String, String>>(listOfMaps);
            }
            return this;
        }

        /**
         *
         * @param listOfMaps
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        public Builder listOfMaps(Map<String, String>... listOfMaps) {
            if (this.listOfMaps == null) {
                this.listOfMaps = new ArrayList<>(listOfMaps.length);
            }
            for (Map<String, String> ele : listOfMaps) {
                this.listOfMaps.add(ele);
            }
            return this;
        }

        /**
         *
         * @param listOfStructs
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        public Builder setListOfStructs(List<SimpleStruct> listOfStructs) {
            if (listOfStructs == null) {
                this.listOfStructs = null;
            } else {
                this.listOfStructs = new ArrayList<SimpleStruct>(listOfStructs);
            }
            return this;
        }

        /**
         *
         * @param listOfStructs
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        public Builder listOfStructs(SimpleStruct... listOfStructs) {
            if (this.listOfStructs == null) {
                this.listOfStructs = new ArrayList<>(listOfStructs.length);
            }
            for (SimpleStruct ele : listOfStructs) {
                this.listOfStructs.add(ele);
            }
            return this;
        }

        /**
         *
         * @param mapOfStringToIntegerList
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        public Builder setMapOfStringToIntegerList(Map<String, List<Integer>> mapOfStringToIntegerList) {
            this.mapOfStringToIntegerList = new HashMap<>(mapOfStringToIntegerList);
            return this;
        }

        /**
         *
         * @param mapOfStringToString
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        public Builder setMapOfStringToString(Map<String, String> mapOfStringToString) {
            this.mapOfStringToString = new HashMap<>(mapOfStringToString);
            return this;
        }

        /**
         *
         * @param mapOfStringToStruct
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        public Builder setMapOfStringToStruct(Map<String, SimpleStruct> mapOfStringToStruct) {
            this.mapOfStringToStruct = new HashMap<>(mapOfStringToStruct);
            return this;
        }

        /**
         *
         * @param timestampMember
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        public Builder setTimestampMember(Date timestampMember) {
            this.timestampMember = timestampMember;
            return this;
        }

        /**
         *
         * @param structWithNestedTimestampMember
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        public Builder setStructWithNestedTimestampMember(StructWithTimestamp structWithNestedTimestampMember) {
            this.structWithNestedTimestampMember = structWithNestedTimestampMember;
            return this;
        }

        /**
         *
         * @param blobArg
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        public Builder setBlobArg(ByteBuffer blobArg) {
            this.blobArg = blobArg;
            return this;
        }

        /**
         *
         * @param structWithNestedBlob
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        public Builder setStructWithNestedBlob(StructWithNestedBlobType structWithNestedBlob) {
            this.structWithNestedBlob = structWithNestedBlob;
            return this;
        }

        /**
         *
         * @param blobMap
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        public Builder setBlobMap(Map<String, ByteBuffer> blobMap) {
            this.blobMap = new HashMap<>(blobMap);
            return this;
        }

        /**
         *
         * @param listOfBlobs
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        public Builder setListOfBlobs(List<ByteBuffer> listOfBlobs) {
            if (listOfBlobs == null) {
                this.listOfBlobs = null;
            } else {
                this.listOfBlobs = new ArrayList<ByteBuffer>(listOfBlobs);
            }
            return this;
        }

        /**
         *
         * @param listOfBlobs
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        public Builder listOfBlobs(ByteBuffer... listOfBlobs) {
            if (this.listOfBlobs == null) {
                this.listOfBlobs = new ArrayList<>(listOfBlobs.length);
            }
            for (ByteBuffer ele : listOfBlobs) {
                this.listOfBlobs.add(ele);
            }
            return this;
        }

        /**
         *
         * @param recursiveStruct
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        public Builder setRecursiveStruct(RecursiveStructType recursiveStruct) {
            this.recursiveStruct = recursiveStruct;
            return this;
        }

        /**
         *
         * @param polymorphicTypeWithSubTypes
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        public Builder setPolymorphicTypeWithSubTypes(BaseType polymorphicTypeWithSubTypes) {
            this.polymorphicTypeWithSubTypes = polymorphicTypeWithSubTypes;
            return this;
        }

        /**
         *
         * @param polymorphicTypeWithoutSubTypes
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        public Builder setPolymorphicTypeWithoutSubTypes(SubTypeOne polymorphicTypeWithoutSubTypes) {
            this.polymorphicTypeWithoutSubTypes = polymorphicTypeWithoutSubTypes;
            return this;
        }

        public AllTypesRequest build_() {
            return new AllTypesRequest(this);
        }
    }
}
