package software.amazon.awssdk.metrics.internal.cloudwatch;

import software.amazon.awssdk.Request;
import software.amazon.awssdk.Response;
import software.amazon.awssdk.metrics.MetricCollector;
import software.amazon.awssdk.metrics.RequestMetricCollector;
import software.amazon.awssdk.metrics.ServiceMetricCollector;
import software.amazon.awssdk.metrics.spi.AwsRequestMetrics;

public class LoggingMetricCollector extends MetricCollector {
    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public RequestMetricCollector getRequestMetricCollector() {
        return new RequestMetricCollector() {
            @Override
            public void collectMetrics(Request<?> request, Response<?> response) {
              AwsRequestMetrics metrics = request.getAWSRequestMetrics();
              metrics.log();
            }
        };
    }

    @Override
    public ServiceMetricCollector getServiceMetricCollector() {
        return ServiceMetricCollector.NONE;
    }
}
