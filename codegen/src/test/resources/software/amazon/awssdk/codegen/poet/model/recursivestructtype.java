package software.amazon.awssdk.services.jsonprotocoltests.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.annotation.Generated;
import software.amazon.awssdk.annotation.SdkInternalApi;
import software.amazon.awssdk.builder.CopyableBuilder;
import software.amazon.awssdk.builder.ToCopyableBuilder;
import software.amazon.awssdk.protocol.ProtocolMarshaller;
import software.amazon.awssdk.protocol.StructuredPojo;
import software.amazon.awssdk.services.jsonprotocoltests.transform.RecursiveStructTypeMarshaller;

@Generated("software.amazon.awssdk:codegen")
public class RecursiveStructType implements ToCopyableBuilder<RecursiveStructType.Builder, RecursiveStructType>, StructuredPojo {
    private final String noRecurse;

    private final RecursiveStructType recursiveStruct;

    private final List<RecursiveStructType> recursiveList;

    private final Map<String, RecursiveStructType> recursiveMap;

    private RecursiveStructType(BeanStyleBuilder builder) {
        this.noRecurse = builder.noRecurse;
        this.recursiveStruct = builder.recursiveStruct;
        this.recursiveList = builder.recursiveList;
        this.recursiveMap = builder.recursiveMap;
    }

    /**
     *
     * @return
     */
    public String noRecurse() {
        return noRecurse;
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
    public List<RecursiveStructType> recursiveList() {
        return recursiveList;
    }

    /**
     *
     * @return
     */
    public Map<String, RecursiveStructType> recursiveMap() {
        return recursiveMap;
    }

    @Override
    public Builder toBuilder() {
        return new BeanStyleBuilder(this);
    }

    public static Builder builder() {
        return new BeanStyleBuilder();
    }

    public static Class<? extends Builder> beanStyleBuilderClass() {
        return BeanStyleBuilder.class;
    }

    @Override
    public int hashCode() {
        int hashCode = 1;
        hashCode = 31 * hashCode + ((noRecurse() == null) ? 0 : noRecurse().hashCode());
        hashCode = 31 * hashCode + ((recursiveStruct() == null) ? 0 : recursiveStruct().hashCode());
        hashCode = 31 * hashCode + ((recursiveList() == null) ? 0 : recursiveList().hashCode());
        hashCode = 31 * hashCode + ((recursiveMap() == null) ? 0 : recursiveMap().hashCode());
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
        if (!(obj instanceof RecursiveStructType)) {
            return false;
        }
        RecursiveStructType other = (RecursiveStructType) obj;
        if (other.noRecurse() == null ^ this.noRecurse() == null) {
            return false;
        }
        if (other.noRecurse() != null && !other.noRecurse().equals(this.noRecurse())) {
            return false;
        }
        if (other.recursiveStruct() == null ^ this.recursiveStruct() == null) {
            return false;
        }
        if (other.recursiveStruct() != null && !other.recursiveStruct().equals(this.recursiveStruct())) {
            return false;
        }
        if (other.recursiveList() == null ^ this.recursiveList() == null) {
            return false;
        }
        if (other.recursiveList() != null && !other.recursiveList().equals(this.recursiveList())) {
            return false;
        }
        if (other.recursiveMap() == null ^ this.recursiveMap() == null) {
            return false;
        }
        if (other.recursiveMap() != null && !other.recursiveMap().equals(this.recursiveMap())) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        if (noRecurse() != null) {
            sb.append("NoRecurse: ").append(noRecurse()).append(",");
        }
        if (recursiveStruct() != null) {
            sb.append("RecursiveStruct: ").append(recursiveStruct()).append(",");
        }
        if (recursiveList() != null) {
            sb.append("RecursiveList: ").append(recursiveList()).append(",");
        }
        if (recursiveMap() != null) {
            sb.append("RecursiveMap: ").append(recursiveMap()).append(",");
        }
        sb.append("}");
        return sb.toString();
    }

    @SdkInternalApi
    @Override
    public void marshall(ProtocolMarshaller protocolMarshaller) {
        RecursiveStructTypeMarshaller.getInstance().marshall(this, protocolMarshaller);
    }

    public interface Builder extends CopyableBuilder<Builder, RecursiveStructType> {
        /**
         *
         * @param noRecurse
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder noRecurse(String noRecurse);

        /**
         *
         * @param recursiveStruct
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder recursiveStruct(RecursiveStructType recursiveStruct);

        /**
         *
         * @param recursiveList
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder recursiveList(Collection<RecursiveStructType> recursiveList);

        /**
         *
         * @param recursiveList
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder recursiveList(RecursiveStructType... recursiveList);

        /**
         *
         * @param recursiveMap
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder recursiveMap(Map<String, RecursiveStructType> recursiveMap);
    }

    private static final class BeanStyleBuilder implements Builder {
        private String noRecurse;

        private RecursiveStructType recursiveStruct;

        private List<RecursiveStructType> recursiveList;

        private Map<String, RecursiveStructType> recursiveMap;

        private BeanStyleBuilder() {
        }

        private BeanStyleBuilder(RecursiveStructType model) {
            setNoRecurse(model.noRecurse);
            setRecursiveStruct(model.recursiveStruct);
            setRecursiveList(model.recursiveList);
            setRecursiveMap(model.recursiveMap);
        }

        @Override
        public Builder noRecurse(String noRecurse) {
            this.noRecurse = StringCopier.copyString(noRecurse);
            return this;
        }

        /**
         *
         * @param noRecurse
         */
        public void setNoRecurse(String noRecurse) {
            this.noRecurse = StringCopier.copyString(noRecurse);
        }

        @Override
        public Builder recursiveStruct(RecursiveStructType recursiveStruct) {
            this.recursiveStruct = RecursiveStructTypeCopier.copyRecursiveStructType(recursiveStruct);
            return this;
        }

        /**
         *
         * @param recursiveStruct
         */
        public void setRecursiveStruct(RecursiveStructType recursiveStruct) {
            this.recursiveStruct = RecursiveStructTypeCopier.copyRecursiveStructType(recursiveStruct);
        }

        @Override
        public Builder recursiveList(Collection<RecursiveStructType> recursiveList) {
            this.recursiveList = RecursiveListTypeCopier.copyRecursiveListType(recursiveList);
            return this;
        }

        @Override
        @SafeVarargs
        public Builder recursiveList(RecursiveStructType... recursiveList) {
            if (this.recursiveList == null) {
                this.recursiveList = new ArrayList<>(recursiveList.length);
            }
            for (RecursiveStructType e : recursiveList) {
                this.recursiveList.add(RecursiveStructTypeCopier.copyRecursiveStructType(e));
            }
            return this;
        }

        /**
         *
         * @param recursiveList
         */
        public void setRecursiveList(Collection<RecursiveStructType> recursiveList) {
            this.recursiveList = RecursiveListTypeCopier.copyRecursiveListType(recursiveList);
        }

        /**
         *
         * @param recursiveList
         */
        @SafeVarargs
        public void setRecursiveList(RecursiveStructType... recursiveList) {
            if (this.recursiveList == null) {
                this.recursiveList = new ArrayList<>(recursiveList.length);
            }
            for (RecursiveStructType e : recursiveList) {
                this.recursiveList.add(RecursiveStructTypeCopier.copyRecursiveStructType(e));
            }
        }

        @Override
        public Builder recursiveMap(Map<String, RecursiveStructType> recursiveMap) {
            this.recursiveMap = RecursiveMapTypeCopier.copyRecursiveMapType(recursiveMap);
            return this;
        }

        /**
         *
         * @param recursiveMap
         */
        public void setRecursiveMap(Map<String, RecursiveStructType> recursiveMap) {
            this.recursiveMap = RecursiveMapTypeCopier.copyRecursiveMapType(recursiveMap);
        }

        @Override
        public RecursiveStructType build() {
            return new RecursiveStructType(this);
        }
    }
}

