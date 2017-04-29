package software.amazon.awssdk.services.jsonprotocoltests.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.annotation.SdkInternalApi;
import software.amazon.awssdk.protocol.ProtocolMarshaller;
import software.amazon.awssdk.protocol.StructuredPojo;
import software.amazon.awssdk.services.jsonprotocoltests.model.transform.RecursiveStructTypeMarshaller;

public class RecursiveStructType implements Serializable, Cloneable, StructuredPojo {
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

    public Builder toBuilder() {
        return new BeanStyleBuilder(this);
    }

    public static Builder builder_() {
        return new BeanStyleBuilder();
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
    public RecursiveStructType clone() {
        try {
            return (RecursiveStructType) super.clone();
        } catch (Exception e) {
            throw new IllegalStateException("Got a CloneNotSupportedException from Object.clone() even though we're Cloneable!",
                    e);
        }
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

    public interface Builder {
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
        Builder recursiveList(List<RecursiveStructType> recursiveList);

        /**
         *
         * @param recursiveList
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder recursiveList(RecursiveStructType... recursiveList);

        RecursiveStructType build_();
    }

    public static class BeanStyleBuilder implements Builder {
        private String noRecurse;

        private RecursiveStructType recursiveStruct;

        private List<RecursiveStructType> recursiveList;

        private Map<String, RecursiveStructType> recursiveMap;

        private BeanStyleBuilder() {
        }

        private BeanStyleBuilder(RecursiveStructType model) {
            this.noRecurse = model.noRecurse;
            this.recursiveStruct = model.recursiveStruct;
            this.recursiveList = model.recursiveList;
            this.recursiveMap = model.recursiveMap;
        }

        @Override
        public Builder noRecurse(String noRecurse) {
            this.noRecurse = noRecurse;
            return this;
        }

        /**
         *
         * @param noRecurse
         */
        public void setNoRecurse(String noRecurse) {
            this.noRecurse = noRecurse;
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
        public Builder recursiveList(List<RecursiveStructType> recursiveList) {
            if (recursiveList == null) {
                this.recursiveList = null;
            } else {
                this.recursiveList = new ArrayList<RecursiveStructType>(recursiveList);
            }
            return this;
        }

        @Override
        public Builder recursiveList(RecursiveStructType... recursiveList) {
            if (this.recursiveList == null) {
                this.recursiveList = new ArrayList<RecursiveStructType>(recursiveList.length);
            }
            for (RecursiveStructType ele : recursiveList) {
                this.recursiveList.add(ele);
            }
            return this;
        }

        /**
         *
         * @param recursiveList
         */
        public void setRecursiveList(List<RecursiveStructType> recursiveList) {
            if (recursiveList == null) {
                this.recursiveList = null;
            } else {
                this.recursiveList = new ArrayList<RecursiveStructType>(recursiveList);
            }
        }

        /**
         *
         * @param recursiveList
         */
        public void setRecursiveList(RecursiveStructType... recursiveList) {
            if (this.recursiveList == null) {
                this.recursiveList = new ArrayList<RecursiveStructType>(recursiveList.length);
            }
            for (RecursiveStructType ele : recursiveList) {
                this.recursiveList.add(ele);
            }
        }

        @Override
        public Builder recursiveMap(Map<String, RecursiveStructType> recursiveMap) {
            if (recursiveMap == null) {
                this.recursiveMap = null;
            } else {
                this.recursiveMap = new HashMap<String, RecursiveStructType>(recursiveMap);
            }
            return this;
        }

        /**
         *
         * @param recursiveMap
         */
        public void setRecursiveMap(Map<String, RecursiveStructType> recursiveMap) {
            if (recursiveMap == null) {
                this.recursiveMap = null;
            } else {
                this.recursiveMap = new HashMap<String, RecursiveStructType>(recursiveMap);
            }
        }

        @Override
        public RecursiveStructType build_() {
            return new RecursiveStructType(this);
        }
    }
}
