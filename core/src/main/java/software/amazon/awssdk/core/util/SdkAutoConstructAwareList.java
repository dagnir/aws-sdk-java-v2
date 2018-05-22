package software.amazon.awssdk.core.util;

import java.util.List;

/**
 * A {@link List} that distinguishes whether it was auto constructed; i.e. it
 * was constructed using its default, no-arg constructor.
 *
 * @param <T> The element type.
 */
public interface SdkAutoConstructAwareList<T> extends List<T> {
    /**
     * @return {@code true} if this list was auto constructed.
     */
    boolean isAutoConstructed();
}
