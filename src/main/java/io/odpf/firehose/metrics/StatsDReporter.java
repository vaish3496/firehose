package io.odpf.firehose.metrics;

import io.odpf.firehose.util.Clock;
import com.timgroup.statsd.StatsDClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

/**
 * Statsd reporter for firehose.
 */
public class StatsDReporter implements Closeable {

    private StatsDClient client;
    private String globalTags;
    private static final Logger LOGGER = LoggerFactory.getLogger(StatsDReporter.class);

    private Clock clock;

    public StatsDReporter(StatsDClient client, Clock clock, String... globalTags) {
        this.client = client;
        this.globalTags = String.join(",", globalTags).replaceAll(":", "=");
        this.clock = clock;
    }

    public Clock getClock() {
        return clock;
    }

    public StatsDClient getClient() {
        return client;
    }

    public void captureCount(String metric, Integer delta, String... tags) {
        client.count(withTags(metric, tags), delta);
    }

    public void captureCount(String metric, Integer delta) {
        client.count(withGlobalTags(metric), delta);
    }

    public void captureHistogramWithTags(String metric, long delta, String... tags) {
        client.time(withTags(metric, tags), delta);
    }

    public void captureHistogram(String metric, long delta) {
        client.time(withGlobalTags(metric), delta);
    }

    public void captureDurationSince(String metric, Instant startTime, String... tags) {
        client.recordExecutionTime(withTags(metric, tags), Duration.between(startTime, clock.now()).toMillis());
    }

    public void gauge(String metric, Integer value) {
        client.gauge(withGlobalTags(metric), value);
    }

    public void increment(String metric, String... tags) {
        captureCount(metric, 1, tags);
    }

    public void increment(String metric) {
        captureCount(metric, 1);
    }

    public void recordEvent(String metric, String eventName, String... tags) {
        client.recordSetEvent(withTags(metric, tags), eventName);
    }

    private String withGlobalTags(String metric) {
        return metric + "," + this.globalTags;
    }

    private String withTags(String metric, String... tags) {
        return metric + "," + this.globalTags + "," + String.join(",", tags);
    }

    @Override
    public void close() throws IOException {
        LOGGER.info("StatsD connection closed");
        client.stop();
    }

}
