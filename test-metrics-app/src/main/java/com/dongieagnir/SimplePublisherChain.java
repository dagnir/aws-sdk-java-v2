package com.dongieagnir;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.metrics.MetricCollection;
import software.amazon.awssdk.metrics.MetricPublisher;
import software.amazon.awssdk.utils.FunctionalUtils;

public final class SimplePublisherChain implements MetricPublisher {
    private static final Logger LOG = LoggerFactory.getLogger(SimplePublisherChain.class);

    private final MetricPublisher[] publishers;

    private SimplePublisherChain(MetricPublisher[] publishers) {
        this.publishers = publishers;
    }

    @Override
    public void publish(MetricCollection metricCollection) {
        for (MetricPublisher publisher : publishers) {
            publisher.publish(metricCollection);
        }
    }

    @Override
    public void close() {
        for (MetricPublisher publisher : publishers) {
            FunctionalUtils.runAndLogError(LOG, "Error closing publisher", publisher::close);
        }
    }

    public static SimplePublisherChain create(MetricPublisher... publishers) {
        return new SimplePublisherChain(publishers);
    }
}
