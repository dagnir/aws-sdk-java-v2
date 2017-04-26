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

public class RecursiveStructType implements Cloneable, Serializable, StructuredPojo {
    private String noRecurse;

    private RecursiveStructType recursiveStruct;

    private List<RecursiveStructType> recursiveList;

    private Map<String, RecursiveStructType> recursiveMap;

    private RecursiveStructType(Builder builder) {
        this.noRecurse = builder.noRecurse;
        this.recursiveStruct = builder.recursiveStruct;
        this.recursiveList = builder.recursiveList;
        this.recursiveMap = builder.recursiveMap;
    }

    /**
     * turn
     */
    public String getNoRecurse() {
        return noRecurse;
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
    public List<RecursiveStructType> getRecursiveList() {
        return recursiveList;
    }

    /**
     * turn
     */
    public Map<String, RecursiveStructType> getRecursiveMap() {
        return recursiveMap;
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
        hashCode = 31 * hashCode + ((getNoRecurse() == null) ? 0 : getNoRecurse().hashCode());
        hashCode = 31 * hashCode + ((getRecursiveStruct() == null) ? 0 : getRecursiveStruct().hashCode());
        hashCode = 31 * hashCode + ((getRecursiveList() == null) ? 0 : getRecursiveList().hashCode());
        hashCode = 31 * hashCode + ((getRecursiveMap() == null) ? 0 : getRecursiveMap().hashCode());
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
        if (other.getNoRecurse() == null ^ this.getNoRecurse() == null) {
            return false;
        }
        if (other.getNoRecurse() != null && !other.getNoRecurse().equals(this.getNoRecurse())) {
            return false;
        }
        if (other.getRecursiveStruct() == null ^ this.getRecursiveStruct() == null) {
            return false;
        }
        if (other.getRecursiveStruct() != null && !other.getRecursiveStruct().equals(this.getRecursiveStruct())) {
            return false;
        }
        if (other.getRecursiveList() == null ^ this.getRecursiveList() == null) {
            return false;
        }
        if (other.getRecursiveList() != null && !other.getRecursiveList().equals(this.getRecursiveList())) {
            return false;
        }
        if (other.getRecursiveMap() == null ^ this.getRecursiveMap() == null) {
            return false;
        }
        if (other.getRecursiveMap() != null && !other.getRecursiveMap().equals(this.getRecursiveMap())) {
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
        if (getNoRecurse() != null) {
            sb.append("NoRecurse: ").append(getNoRecurse()).append(",");
        }
        if (getRecursiveStruct() != null) {
            sb.append("RecursiveStruct: ").append(getRecursiveStruct()).append(",");
        }
        if (getRecursiveList() != null) {
            sb.append("RecursiveList: ").append(getRecursiveList()).append(",");
        }
        if (getRecursiveMap() != null) {
            sb.append("RecursiveMap: ").append(getRecursiveMap()).append(",");
        }
        sb.append("}");
        return sb.toString();
    }

    @SdkInternalApi
    @Override
    public void marshall(ProtocolMarshaller protocolMarshaller) {
        RecursiveStructTypeMarshaller.getInstance().marshall(this, protocolMarshaller);
    }

    public static class Builder {
        private String noRecurse;

        private RecursiveStructType recursiveStruct;

        private List<RecursiveStructType> recursiveList;

        private Map<String, RecursiveStructType> recursiveMap;

        private Builder() {
        }

        private Builder(RecursiveStructType model) {
            this.noRecurse = model.noRecurse;
            this.recursiveStruct = model.recursiveStruct;
            this.recursiveList = model.recursiveList;
            this.recursiveMap = model.recursiveMap;
        }

        /**
         *
         * @param noRecurse
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        public Builder setNoRecurse(String noRecurse) {
            this.noRecurse = noRecurse;
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
         * @param recursiveList
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        public Builder setRecursiveList(List<RecursiveStructType> recursiveList) {
            if (recursiveList == null) {
                this.recursiveList = null;
            } else {
                this.recursiveList = new ArrayList<RecursiveStructType>(recursiveList);
            }
            return this;
        }

        /**
         *
         * @param recursiveList
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        public Builder setRecursiveList(RecursiveStructType... recursiveList) {
            if (this.recursiveList == null) {
                this.recursiveList = new ArrayList<>(recursiveList.length);
            }
            for (RecursiveStructType ele : recursiveList) {
                this.recursiveList.add(ele);
            }
            return this;
        }

        /**
         *
         * @param recursiveMap
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        public Builder setRecursiveMap(Map<String, RecursiveStructType> recursiveMap) {
            this.recursiveMap = new HashMap<>(recursiveMap);
            return this;
        }

        public RecursiveStructType build_() {
            return new RecursiveStructType(this);
        }
    }
}
