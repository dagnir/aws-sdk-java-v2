package software.amazon.awssdk.services.jsonprotocoltests.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.annotation.Generated;
import software.amazon.awssdk.AmazonWebServiceResult;
import software.amazon.awssdk.ResponseMetadata;
import software.amazon.awssdk.builder.CopyableBuilder;
import software.amazon.awssdk.builder.ToCopyableBuilder;

/**
 */
@Generated("software.amazon.awssdk:codegen")
public class NestedContainersResult extends AmazonWebServiceResult<ResponseMetadata> implements
        ToCopyableBuilder<NestedContainersResult.Builder, NestedContainersResult> {
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

    public interface Builder extends CopyableBuilder<Builder, NestedContainersResult> {
        /**
         *
         * @param listOfListsOfStrings
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder listOfListsOfStrings(Collection<? extends Collection<String>> listOfListsOfStrings);

        /**
         *
         * <p>
         * <b>NOTE:</b> This method appends the values to the existing list (if any). Use
         * {@link #setListOfListsOfStrings(java.util.Collection)} or
         * {@link #withListOfListsOfStrings(java.util.Collection)} if you want to override the existing values.
         * </p>
         * 
         * @param listOfListsOfStrings
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder listOfListsOfStrings(Collection<String>... listOfListsOfStrings);

        /**
         *
         * @param listOfListOfListsOfStrings
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder listOfListOfListsOfStrings(
                Collection<? extends Collection<? extends Collection<String>>> listOfListOfListsOfStrings);

        /**
         *
         * <p>
         * <b>NOTE:</b> This method appends the values to the existing list (if any). Use
         * {@link #setListOfListOfListsOfStrings(java.util.Collection)} or
         * {@link #withListOfListOfListsOfStrings(java.util.Collection)} if you want to override the existing values.
         * </p>
         * 
         * @param listOfListOfListsOfStrings
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder listOfListOfListsOfStrings(Collection<? extends Collection<String>>... listOfListOfListsOfStrings);

        /**
         *
         * @param mapOfStringToListOfListsOfStrings
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder mapOfStringToListOfListsOfStrings(
                Map<String, ? extends Collection<? extends Collection<String>>> mapOfStringToListOfListsOfStrings);
    }

    private static final class BeanStyleBuilder implements Builder {
        private List<List<String>> listOfListsOfStrings;

        private List<List<List<String>>> listOfListOfListsOfStrings;

        private Map<String, List<List<String>>> mapOfStringToListOfListsOfStrings;

        private BeanStyleBuilder() {
        }

        private BeanStyleBuilder(NestedContainersResult model) {
            setListOfListsOfStrings(model.listOfListsOfStrings);
            setListOfListOfListsOfStrings(model.listOfListOfListsOfStrings);
            setMapOfStringToListOfListsOfStrings(model.mapOfStringToListOfListsOfStrings);
        }

        @Override
        public final Builder listOfListsOfStrings(Collection<? extends Collection<String>> listOfListsOfStrings) {
            this.listOfListsOfStrings = ListOfListsOfStringsCopier.copy(listOfListsOfStrings);
            return this;
        }

        @Override
        @SafeVarargs
        public final Builder listOfListsOfStrings(Collection<String>... listOfListsOfStrings) {
            if (this.listOfListsOfStrings == null) {
                this.listOfListsOfStrings = new ArrayList<>(listOfListsOfStrings.length);
            }
            for (Collection<String> e : listOfListsOfStrings) {
                this.listOfListsOfStrings.add(ListOfStringsCopier.copy(e));
            }
            return this;
        }

        public final void setListOfListsOfStrings(Collection<? extends Collection<String>> listOfListsOfStrings) {
            this.listOfListsOfStrings = ListOfListsOfStringsCopier.copy(listOfListsOfStrings);
        }

        @SafeVarargs
        public final void setListOfListsOfStrings(Collection<String>... listOfListsOfStrings) {
            if (this.listOfListsOfStrings == null) {
                this.listOfListsOfStrings = new ArrayList<>(listOfListsOfStrings.length);
            }
            for (Collection<String> e : listOfListsOfStrings) {
                this.listOfListsOfStrings.add(ListOfStringsCopier.copy(e));
            }
        }

        @Override
        public final Builder listOfListOfListsOfStrings(
                Collection<? extends Collection<? extends Collection<String>>> listOfListOfListsOfStrings) {
            this.listOfListOfListsOfStrings = ListOfListOfListsOfStringsCopier.copy(listOfListOfListsOfStrings);
            return this;
        }

        @Override
        @SafeVarargs
        public final Builder listOfListOfListsOfStrings(Collection<? extends Collection<String>>... listOfListOfListsOfStrings) {
            if (this.listOfListOfListsOfStrings == null) {
                this.listOfListOfListsOfStrings = new ArrayList<>(listOfListOfListsOfStrings.length);
            }
            for (Collection<? extends Collection<String>> e : listOfListOfListsOfStrings) {
                this.listOfListOfListsOfStrings.add(ListOfListsOfStringsCopier.copy(e));
            }
            return this;
        }

        public final void setListOfListOfListsOfStrings(
                Collection<? extends Collection<? extends Collection<String>>> listOfListOfListsOfStrings) {
            this.listOfListOfListsOfStrings = ListOfListOfListsOfStringsCopier.copy(listOfListOfListsOfStrings);
        }

        @SafeVarargs
        public final void setListOfListOfListsOfStrings(Collection<? extends Collection<String>>... listOfListOfListsOfStrings) {
            if (this.listOfListOfListsOfStrings == null) {
                this.listOfListOfListsOfStrings = new ArrayList<>(listOfListOfListsOfStrings.length);
            }
            for (Collection<? extends Collection<String>> e : listOfListOfListsOfStrings) {
                this.listOfListOfListsOfStrings.add(ListOfListsOfStringsCopier.copy(e));
            }
        }

        @Override
        public final Builder mapOfStringToListOfListsOfStrings(
                Map<String, ? extends Collection<? extends Collection<String>>> mapOfStringToListOfListsOfStrings) {
            this.mapOfStringToListOfListsOfStrings = MapOfStringToListOfListsOfStringsCopier
                    .copy(mapOfStringToListOfListsOfStrings);
            return this;
        }

        public final void setMapOfStringToListOfListsOfStrings(
                Map<String, ? extends Collection<? extends Collection<String>>> mapOfStringToListOfListsOfStrings) {
            this.mapOfStringToListOfListsOfStrings = MapOfStringToListOfListsOfStringsCopier
                    .copy(mapOfStringToListOfListsOfStrings);
        }

        @Override
        public NestedContainersResult build() {
            return new NestedContainersResult(this);
        }
    }
}

