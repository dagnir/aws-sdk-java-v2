package software.amazon.awssdk.core.util;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Default implementation of {@link SdkAutoConstructAwareList}.
 *
 * @param <T> The element type.
 */
public class DefaultSdkAutoConstructAwareList<T> extends ArrayList<T> implements SdkAutoConstructAwareList<T> {
    private final boolean isAutoConstructed;

    public DefaultSdkAutoConstructAwareList() {
        super();
        isAutoConstructed = true;
    }

    public DefaultSdkAutoConstructAwareList(Collection<? extends T> other) {
        super(other);
        isAutoConstructed = false;
    }

    public DefaultSdkAutoConstructAwareList(int initialCapacity) {
        super(initialCapacity);
        isAutoConstructed = false;
    }

    @Override
    public boolean isAutoConstructed() {
        return isAutoConstructed;
    }
}
