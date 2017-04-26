package software.amazon.awssdk.services.jsonprotocoltests.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.AmazonWebServiceRequest;

public class NestedContainersRequest extends AmazonWebServiceRequest implements Cloneable, Serializable {
    private List<List<String>> listOfListsOfStrings;

    private List<List<List<String>>> listOfListOfListsOfStrings;

    private Map<String, List<List<String>>> mapOfStringToListOfListsOfStrings;

    private NestedContainersRequest(Builder builder) {
        this.listOfListsOfStrings = builder.listOfListsOfStrings;
        this.listOfListOfListsOfStrings = builder.listOfListOfListsOfStrings;
        this.mapOfStringToListOfListsOfStrings = builder.mapOfStringToListOfListsOfStrings;
    }

    /**
     * turn
     */
    public List<List<String>> getListOfListsOfStrings() {
        return listOfListsOfStrings;
    }

    /**
     * turn
     */
    public List<List<List<String>>> getListOfListOfListsOfStrings() {
        return listOfListOfListsOfStrings;
    }

    /**
     * turn
     */
    public Map<String, List<List<String>>> getMapOfStringToListOfListsOfStrings() {
        return mapOfStringToListOfListsOfStrings;
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
        hashCode = 31 * hashCode + ((getListOfListsOfStrings() == null) ? 0 : getListOfListsOfStrings().hashCode());
        hashCode = 31 * hashCode + ((getListOfListOfListsOfStrings() == null) ? 0 : getListOfListOfListsOfStrings().hashCode());
        hashCode = 31 * hashCode
                + ((getMapOfStringToListOfListsOfStrings() == null) ? 0 : getMapOfStringToListOfListsOfStrings().hashCode());
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
        if (!(obj instanceof NestedContainersRequest)) {
            return false;
        }
        NestedContainersRequest other = (NestedContainersRequest) obj;
        if (other.getListOfListsOfStrings() == null ^ this.getListOfListsOfStrings() == null) {
            return false;
        }
        if (other.getListOfListsOfStrings() != null && !other.getListOfListsOfStrings().equals(this.getListOfListsOfStrings())) {
            return false;
        }
        if (other.getListOfListOfListsOfStrings() == null ^ this.getListOfListOfListsOfStrings() == null) {
            return false;
        }
        if (other.getListOfListOfListsOfStrings() != null
                && !other.getListOfListOfListsOfStrings().equals(this.getListOfListOfListsOfStrings())) {
            return false;
        }
        if (other.getMapOfStringToListOfListsOfStrings() == null ^ this.getMapOfStringToListOfListsOfStrings() == null) {
            return false;
        }
        if (other.getMapOfStringToListOfListsOfStrings() != null
                && !other.getMapOfStringToListOfListsOfStrings().equals(this.getMapOfStringToListOfListsOfStrings())) {
            return false;
        }
        return true;
    }

    @Override
    public NestedContainersRequest clone() {
        try {
            return (NestedContainersRequest) super.clone();
        } catch (Exception e) {
            throw new IllegalStateException("Got a CloneNotSupportedException from Object.clone() even though we're Cloneable!",
                    e);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        if (getListOfListsOfStrings() != null) {
            sb.append("ListOfListsOfStrings: ").append(getListOfListsOfStrings()).append(",");
        }
        if (getListOfListOfListsOfStrings() != null) {
            sb.append("ListOfListOfListsOfStrings: ").append(getListOfListOfListsOfStrings()).append(",");
        }
        if (getMapOfStringToListOfListsOfStrings() != null) {
            sb.append("MapOfStringToListOfListsOfStrings: ").append(getMapOfStringToListOfListsOfStrings()).append(",");
        }
        sb.append("}");
        return sb.toString();
    }

    public static class Builder {
        private List<List<String>> listOfListsOfStrings;

        private List<List<List<String>>> listOfListOfListsOfStrings;

        private Map<String, List<List<String>>> mapOfStringToListOfListsOfStrings;

        private Builder() {
        }

        private Builder(NestedContainersRequest model) {
            this.listOfListsOfStrings = model.listOfListsOfStrings;
            this.listOfListOfListsOfStrings = model.listOfListOfListsOfStrings;
            this.mapOfStringToListOfListsOfStrings = model.mapOfStringToListOfListsOfStrings;
        }

        /**
         *
         * @param listOfListsOfStrings
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        public Builder setListOfListsOfStrings(List<List<String>> listOfListsOfStrings) {
            if (listOfListsOfStrings == null) {
                this.listOfListsOfStrings = null;
            } else {
                this.listOfListsOfStrings = new ArrayList<List<String>>(listOfListsOfStrings);
            }
            return this;
        }

        /**
         *
         * @param listOfListsOfStrings
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        public Builder listOfListsOfStrings(List<String>... listOfListsOfStrings) {
            if (this.listOfListsOfStrings == null) {
                this.listOfListsOfStrings = new ArrayList<>(listOfListsOfStrings.length);
            }
            for (List<String> ele : listOfListsOfStrings) {
                this.listOfListsOfStrings.add(ele);
            }
            return this;
        }

        /**
         *
         * @param listOfListOfListsOfStrings
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        public Builder setListOfListOfListsOfStrings(List<List<List<String>>> listOfListOfListsOfStrings) {
            if (listOfListOfListsOfStrings == null) {
                this.listOfListOfListsOfStrings = null;
            } else {
                this.listOfListOfListsOfStrings = new ArrayList<List<List<String>>>(listOfListOfListsOfStrings);
            }
            return this;
        }

        /**
         *
         * @param listOfListOfListsOfStrings
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        public Builder listOfListOfListsOfStrings(List<List<String>>... listOfListOfListsOfStrings) {
            if (this.listOfListOfListsOfStrings == null) {
                this.listOfListOfListsOfStrings = new ArrayList<>(listOfListOfListsOfStrings.length);
            }
            for (List<List<String>> ele : listOfListOfListsOfStrings) {
                this.listOfListOfListsOfStrings.add(ele);
            }
            return this;
        }

        /**
         *
         * @param mapOfStringToListOfListsOfStrings
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        public Builder setMapOfStringToListOfListsOfStrings(Map<String, List<List<String>>> mapOfStringToListOfListsOfStrings) {
            this.mapOfStringToListOfListsOfStrings = new HashMap<>(mapOfStringToListOfListsOfStrings);
            return this;
        }

        public NestedContainersRequest build_() {
            return new NestedContainersRequest(this);
        }
    }
}
