package software.amazon.awssdk.services.securitytoken.auth;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;

import java.util.concurrent.Callable;
import org.junit.Test;
import software.amazon.awssdk.AmazonClientException;
import software.amazon.awssdk.AmazonServiceException;
import software.amazon.awssdk.function.SdkPredicate;

public class RefreshableTaskExceptionHandlingTest {

    private static final SdkPredicate<Void> ALWAYS_TRUE_PREDICATE = new SdkPredicate<Void>() {
        @Override
        public boolean test(Void aVoid) {
            return true;
        }
    };

    @Test
    public void callableThrowsAmazonServiceExceptionDoesNotWrapException() {
        final String aseMessage = "foo ase";
        RefreshableTask<?> refreshableTask = buildRefreshableTask(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                throw new AmazonServiceException(aseMessage);
            }
        });
        try {
            refreshableTask.getValue();
        } catch (AmazonServiceException ase) {
            assertThat(ase.getMessage(), containsString(aseMessage));
        }
    }

    @Test
    public void callableThrowsAmazonClientExceptionDoesNotWrapException() {
        final String aceMessage = "foo ace";
        RefreshableTask<?> refreshableTask = buildRefreshableTask(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                throw new AmazonClientException(aceMessage);
            }
        });
        try {
            refreshableTask.getValue();
        } catch (AmazonClientException ace) {
            assertThat(ace.getMessage(), containsString(aceMessage));
        }
    }

    @Test
    public void callableThrowsRuntimeExceptionWrapsInAmazonClientException() {
        final String exceptionMessage = "foo runtime exception";
        RefreshableTask<?> refreshableTask = buildRefreshableTask(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                throw new AmazonClientException(new RuntimeException(exceptionMessage));
            }
        });
        try {
            refreshableTask.getValue();
        } catch (AmazonClientException ace) {
            assertThat(ace.getCause(), instanceOf(RuntimeException.class));
            assertThat(ace.getCause().getMessage(), containsString(exceptionMessage));
        }
    }

    private RefreshableTask<Void> buildRefreshableTask(Callable<Void> callable) {
        return new RefreshableTask.Builder().withRefreshCallable(callable)
                .withAsyncRefreshPredicate(ALWAYS_TRUE_PREDICATE)
                .withBlockingRefreshPredicate(ALWAYS_TRUE_PREDICATE).build();
    }
}
