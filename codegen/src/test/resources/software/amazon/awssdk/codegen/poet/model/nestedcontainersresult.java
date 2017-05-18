package software.amazon.awssdk.services.jsonprotocoltests.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.AmazonWebServiceResult;
import software.amazon.awssdk.ResponseMetadata;

public class NestedContainersResult extends AmazonWebServiceResult<ResponseMetadata> implements Cloneable {
    private final List<List<String>> listOfListsOfStrings;

    private final List<List<List<String>>> listOfListOfListsOfStrings;

    private final Map<String, List<List<String>>> mapOfStringToListOfListsOfStrings;

    private NestedContainersResult(BeanStyleBuilder builder) {
        this.listOfListsOfStrings = builder.listOfListsOfStrings;
        this.listOfListOfListsOfStrings = builder.listOfListOfListsOfStrings;
        this.mapOfStringToListOfListsOfStrings = builder.mapOfStringToListOfListsOfStrings;
    }

    /**
     *
     * @return
     */
    public List<List<String>> listOfListsOfStrings() {
        return listOfListsOfStrings;
    }

    /**
     *
     * @return
     */
    public List<List<List<String>>> listOfListOfListsOfStrings() {
        return listOfListOfListsOfStrings;
    }

    /**
     *
     * @return
     */
    public Map<String, List<List<String>>> mapOfStringToListOfListsOfStrings() {
        return mapOfStringToListOfListsOfStrings;
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
        hashCode = 31 * hashCode + ((listOfListsOfStrings() == null) ? 0 : listOfListsOfStrings().hashCode());
        hashCode = 31 * hashCode + ((listOfListOfListsOfStrings() == null) ? 0 : listOfListOfListsOfStrings().hashCode());
        hashCode = 31 * hashCode
                + ((mapOfStringToListOfListsOfStrings() == null) ? 0 : mapOfStringToListOfListsOfStrings().hashCode());
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
        if (!(obj instanceof NestedContainersResult)) {
            return false;
        }
        NestedContainersResult other = (NestedContainersResult) obj;
        if (other.listOfListsOfStrings() == null ^ this.listOfListsOfStrings() == null) {
            return false;
        }
        if (other.listOfListsOfStrings() != null && !other.listOfListsOfStrings().equals(this.listOfListsOfStrings())) {
            return false;
        }
        if (other.listOfListOfListsOfStrings() == null ^ this.listOfListOfListsOfStrings() == null) {
            return false;
        }
        if (other.listOfListOfListsOfStrings() != null
                && !other.listOfListOfListsOfStrings().equals(this.listOfListOfListsOfStrings())) {
            return false;
        }
        if (other.mapOfStringToListOfListsOfStrings() == null ^ this.mapOfStringToListOfListsOfStrings() == null) {
            return false;
        }
        if (other.mapOfStringToListOfListsOfStrings() != null
                && !other.mapOfStringToListOfListsOfStrings().equals(this.mapOfStringToListOfListsOfStrings())) {
            return false;
        }
        return true;
    }

    @Override
    public NestedContainersResult clone() {
        try {
            return (NestedContainersResult) super.clone();
        } catch (Exception e) {
            throw new IllegalStateException("Got a CloneNotSupportedException from Object.clone() even though we're Cloneable!",
                    e);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        if (listOfListsOfStrings() != null) {
            sb.append("ListOfListsOfStrings: ").append(listOfListsOfStrings()).append(",");
        }
        if (listOfListOfListsOfStrings() != null) {
            sb.append("ListOfListOfListsOfStrings: ").append(listOfListOfListsOfStrings()).append(",");
        }
        if (mapOfStringToListOfListsOfStrings() != null) {
            sb.append("MapOfStringToListOfListsOfStrings: ").append(mapOfStringToListOfListsOfStrings()).append(",");
        }
        sb.append("}");
        return sb.toString();
    }

    public interface Builder {
        /**
         *
         * @param listOfListsOfStrings
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder listOfListsOfStrings(Collection<List<String>> listOfListsOfStrings);

        /**
         *
         * @param listOfListsOfStrings
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder listOfListsOfStrings(List<String>... listOfListsOfStrings);

        /**
         *
         * @param listOfListOfListsOfStrings
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder listOfListOfListsOfStrings(Collection<List<List<String>>> listOfListOfListsOfStrings);

        /**
         *
         * @param listOfListOfListsOfStrings
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder listOfListOfListsOfStrings(List<List<String>>... listOfListOfListsOfStrings);

        /**
         *
         * @param mapOfStringToListOfListsOfStrings
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder mapOfStringToListOfListsOfStrings(Map<String, List<List<String>>> mapOfStringToListOfListsOfStrings);

        NestedContainersResult build_();
    }

    private static final class BeanStyleBuilder implements Builder {
        private List<List<String>> listOfListsOfStrings;

        private List<List<List<String>>> listOfListOfListsOfStrings;

        private Map<String, List<List<String>>> mapOfStringToListOfListsOfStrings;

        private BeanStyleBuilder() {
        }

        private BeanStyleBuilder(NestedContainersResult model) {
            this.listOfListsOfStrings = model.listOfListsOfStrings;
            this.listOfListOfListsOfStrings = model.listOfListOfListsOfStrings;
            this.mapOfStringToListOfListsOfStrings = model.mapOfStringToListOfListsOfStrings;
        }

        @Override
        public Builder listOfListsOfStrings(Collection listOfListsOfStrings) {
            if (listOfListsOfStrings == null) {
                this.listOfListsOfStrings = null;
            } else {
                this.listOfListsOfStrings = new ArrayList<List<String>>(listOfListsOfStrings);
            }
            return this;
        }

        @Override
        public Builder listOfListsOfStrings(List<String>... listOfListsOfStrings) {
            if (this.listOfListsOfStrings == null) {
                this.listOfListsOfStrings = new ArrayList<List<String>>(listOfListsOfStrings.length);
            }
            for (List<String> ele : listOfListsOfStrings) {
                this.listOfListsOfStrings.add(ele);
            }
            return this;
        }

        /**
         *
         * @param listOfListsOfStrings
         */
        public void setListOfListsOfStrings(Collection listOfListsOfStrings) {
            if (listOfListsOfStrings == null) {
                this.listOfListsOfStrings = null;
            } else {
                this.listOfListsOfStrings = new ArrayList<List<String>>(listOfListsOfStrings);
            }
        }

        /**
         *
         * @param listOfListsOfStrings
         */
        public void setListOfListsOfStrings(List<String>... listOfListsOfStrings) {
            if (this.listOfListsOfStrings == null) {
                this.listOfListsOfStrings = new ArrayList<List<String>>(listOfListsOfStrings.length);
            }
            for (List<String> ele : listOfListsOfStrings) {
                this.listOfListsOfStrings.add(ele);
            }
        }

        @Override
        public Builder listOfListOfListsOfStrings(Collection listOfListOfListsOfStrings) {
            if (listOfListOfListsOfStrings == null) {
                this.listOfListOfListsOfStrings = null;
            } else {
                this.listOfListOfListsOfStrings = new ArrayList<List<List<String>>>(listOfListOfListsOfStrings);
            }
            return this;
        }

        @Override
        public Builder listOfListOfListsOfStrings(List<List<String>>... listOfListOfListsOfStrings) {
            if (this.listOfListOfListsOfStrings == null) {
                this.listOfListOfListsOfStrings = new ArrayList<List<List<String>>>(listOfListOfListsOfStrings.length);
            }
            for (List<List<String>> ele : listOfListOfListsOfStrings) {
                this.listOfListOfListsOfStrings.add(ele);
            }
            return this;
        }

        /**
         *
         * @param listOfListOfListsOfStrings
         */
        public void setListOfListOfListsOfStrings(Collection listOfListOfListsOfStrings) {
            if (listOfListOfListsOfStrings == null) {
                this.listOfListOfListsOfStrings = null;
            } else {
                this.listOfListOfListsOfStrings = new ArrayList<List<List<String>>>(listOfListOfListsOfStrings);
            }
        }

        /**
         *
         * @param listOfListOfListsOfStrings
         */
        public void setListOfListOfListsOfStrings(List<List<String>>... listOfListOfListsOfStrings) {
            if (this.listOfListOfListsOfStrings == null) {
                this.listOfListOfListsOfStrings = new ArrayList<List<List<String>>>(listOfListOfListsOfStrings.length);
            }
            for (List<List<String>> ele : listOfListOfListsOfStrings) {
                this.listOfListOfListsOfStrings.add(ele);
            }
        }

        @Override
        public Builder mapOfStringToListOfListsOfStrings(Map<String, List<List<String>>> mapOfStringToListOfListsOfStrings) {
            if (mapOfStringToListOfListsOfStrings == null) {
                this.mapOfStringToListOfListsOfStrings = null;
            } else {
                this.mapOfStringToListOfListsOfStrings = new HashMap<String, List<List<String>>>(
                        mapOfStringToListOfListsOfStrings);
            }
            return this;
        }

        /**
         *
         * @param mapOfStringToListOfListsOfStrings
         */
        public void setMapOfStringToListOfListsOfStrings(Map<String, List<List<String>>> mapOfStringToListOfListsOfStrings) {
            if (mapOfStringToListOfListsOfStrings == null) {
                this.mapOfStringToListOfListsOfStrings = null;
            } else {
                this.mapOfStringToListOfListsOfStrings = new HashMap<String, List<List<String>>>(
                        mapOfStringToListOfListsOfStrings);
            }
        }

        @Override
        public NestedContainersResult build_() {
            return new NestedContainersResult(this);
        }
    }
}
