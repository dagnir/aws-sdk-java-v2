package software.amazon.awssdk.services.lambda.invoke;


import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import software.amazon.awssdk.services.lambda.AWSLambda;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResult;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests the default function name resolution and the ability to customize it through
 * {@link LambdaInvokerFactoryConfig}.
 */
public class LambdaInvokerFactoryNameResolutionTest {

    /**
     * Name overridden in {@link LambdaFunction} functionName attribute.
     */
    private static final String OVERRIDDEN_NAME = "OverriddenFunctionName";

    /**
     * Dummy name returned by custom implementation of {@link LambdaFunctionNameResolver}.
     */
    private static final String STATIC_FUNCTION_NAME = "StaticFunctionName";

    @Mock
    private AWSLambda lambda;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        stubSucessfulInvokeResponse();
    }

    /**
     * These tests are only concerned with the setting of function name so we always return a
     * successful response
     */
    private void stubSucessfulInvokeResponse() {
        InvokeResult result = new InvokeResult();
        result.setPayload(ByteBuffer.wrap(new byte[] {}));
        result.setStatusCode(200);
        when(lambda.invoke(any(InvokeRequest.class))).thenReturn(result);
    }

    @Test
    public void functionNameOverridenInAnnotation_UsesOverridenNameAsFunctionName() {
        UnitTestInterface proxy = LambdaInvokerFactory.builder()
                .lambdaClient(lambda)
                .build(UnitTestInterface.class);
        proxy.functionNameOverridenInAnnotation();
        InvokeRequest capturedRequest = captureInvokeRequestArgument();
        assertEquals(OVERRIDDEN_NAME, capturedRequest.getFunctionName());
    }

    @Test
    public void functionNameNotSetInAnnotation_UsesMethodNameAsFunctionName() {
        UnitTestInterface proxy = LambdaInvokerFactory.builder()
                .lambdaClient(lambda)
                .build(UnitTestInterface.class);
        proxy.functionNameNotSetInAnnotation();
        InvokeRequest capturedRequest = captureInvokeRequestArgument();
        assertEquals("functionNameNotSetInAnnotation", capturedRequest.getFunctionName());
    }

    private InvokeRequest captureInvokeRequestArgument() {
        ArgumentCaptor<InvokeRequest> argument = ArgumentCaptor.forClass(InvokeRequest.class);
        verify(lambda).invoke(argument.capture());
        InvokeRequest value = argument.getValue();
        return value;
    }

    @Test
    public void customFunctionNameResolver_DoesNotUseOverrideInAnnotation() {
        UnitTestInterface proxy = LambdaInvokerFactory.builder()
                .lambdaClient(lambda)
                .lambdaFunctionNameResolver(new StaticFunctionNameResolver())
                .build(UnitTestInterface.class);
        proxy.functionNameOverridenInAnnotation();
        InvokeRequest capturedRequest = captureInvokeRequestArgument();
        assertEquals(STATIC_FUNCTION_NAME, capturedRequest.getFunctionName());
    }

    @Test
    public void customFunctionNameResolver_DoesNotUseMethodName() {
        UnitTestInterface proxy = LambdaInvokerFactory.builder()
                .lambdaClient(lambda)
                .lambdaFunctionNameResolver(new StaticFunctionNameResolver())
                .build(UnitTestInterface.class);
        proxy.functionNameNotSetInAnnotation();
        InvokeRequest capturedRequest = captureInvokeRequestArgument();
        assertEquals(STATIC_FUNCTION_NAME, capturedRequest.getFunctionName());
    }

    /**
     * Interface to proxy in unit tests
     */
    private static interface UnitTestInterface {

        @LambdaFunction(functionName = OVERRIDDEN_NAME)
        public void functionNameOverridenInAnnotation();

        @LambdaFunction
        public void functionNameNotSetInAnnotation();
    }

    /**
     * Dummy implementation of {@link LambdaFunctionNameResolver} that always returns the same
     * function name regardless of context.
     */
    public static class StaticFunctionNameResolver implements LambdaFunctionNameResolver {

        @Override
        public String getFunctionName(Method method, LambdaFunction annotation, LambdaInvokerFactoryConfig config) {
            return STATIC_FUNCTION_NAME;
        }

    }
}
