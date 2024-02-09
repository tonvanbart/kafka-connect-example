package org.tonvanbart.wikipedia.connect;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.MetricName;
import org.apache.kafka.common.metrics.*;
import org.apache.kafka.common.metrics.stats.CumulativeSum;
import org.apache.kafka.common.metrics.stats.Value;
import org.apache.kafka.common.utils.Time;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.source.SourceRecord;
import org.apache.kafka.connect.source.SourceTask;
import org.tonvanbart.wikipedia.eventstream.EditEvent;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class WikiSourceTask extends SourceTask {

    private String languageToSelect;

    private String outputTopic;

    private final BlockingQueue<String> incomingEvents = new LinkedBlockingQueue<>();

    private ObjectMapper objectMapper;

    private long lastPoll = 0L;

    private Thread sseThread;

    public static final String TASK_ID = "task.id";

    private Metrics metrics;

    private static final String WIKI_IN = "wiki.incoming";
    private static final String WIKI_OUT = "wiki.written";

    @Override
    public String version() {
        log.debug("version()");
        return WikiSourceConfig.getVersionAndDate();
    }

    @Override
    public void start(Map<String, String> configProps) {
        log.debug("start({})", configProps);
        var wikiSourceConfig = new WikiSourceConfig(configProps);
        languageToSelect = wikiSourceConfig.getWikiLanguageConfig();
        outputTopic = wikiSourceConfig.getTargetTopicConfig();
        objectMapper = new ObjectMapper();
        setupMetrics();
        sseThread = new Thread(this::runSse);
        sseThread.start();
    }

    /**
     * Try to set up metrics with a sensor for number of events emitted.
     */
    private void setupMetrics() {
        log.info("setupMetrics()");
        MetricConfig mConfig = new MetricConfig();
        mConfig.timeWindow(100, TimeUnit.MILLISECONDS);
        mConfig.recordLevel(Sensor.RecordingLevel.INFO);

        List<MetricsReporter> reporters = new ArrayList<>();
        JmxReporter jmxReporter = new JmxReporter();
        jmxReporter.configure(new HashMap<>());
        reporters.add(jmxReporter);

        Map<String, Object> labels = Map.of("wiki.name","example","wiki.foo","bar");
        MetricsContext metricsContext = new KafkaMetricsContext("wiki.connectexample", labels);

        this.metrics = new Metrics(mConfig, reporters, Time.SYSTEM, metricsContext);
        MetricName mName = new MetricName("wiki.dummy","wiki.dummygroup","example dummy metric", Map.of("tag","example"));
        Measurable dummy = new Value();
        metrics.addMetric(mName, dummy);

        var wikiCountIn = metrics.sensor(WIKI_IN);
        MetricName mName2 = metrics.metricName("read","wiki.group","wiki records in");
        wikiCountIn.add(mName2, new CumulativeSum());

        var wikiCountOut = metrics.sensor(WIKI_OUT);
        MetricName mName3 = metrics.metricName("written","wiki.group","wiki records out");
        wikiCountOut.add(mName3, new CumulativeSum());
    }

    /**
     * SSE event listener method, running in a separate thread.
     */
    private void runSse() {
        Stream<String> sseEvents = null;
        try {
            var uri = new URI("https://stream.wikimedia.org/v2/stream/recentchange");
            var httpClient = HttpClient.newHttpClient();
            var httpRequest = HttpRequest.newBuilder(uri).GET().build();
            sseEvents = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofLines()).body();
            log.debug("got an SSE event stream");
            sseEvents.filter(line -> line.startsWith("data: "))
                    .map(line -> line.substring("data: ".length()))
                    .forEach(incomingEvents::offer);

        } catch (URISyntaxException | IOException e) {
            log.error("Source task failed to start", e);
            throw new KafkaException(e);
        } catch (InterruptedException e) {
            log.warn("SSE thread interrupt", e);
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public List<SourceRecord> poll() {
        var now = System.currentTimeMillis();
        if (now - lastPoll < 100) {
            // give the event thread a second to get some events
            return Collections.emptyList();
        }
        lastPoll = now;
        List<String> linesToSend = new ArrayList<>();
        incomingEvents.drainTo(linesToSend);
        metrics.getSensor(WIKI_IN).record(linesToSend.size());
        log.trace("poll(): Got {} lines", linesToSend.size());
        var filtered = linesToSend.stream()
                .map(this::convertToSourceRecord)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
        metrics.getSensor(WIKI_OUT).record(filtered.size());
        return filtered;
    }

    @Override
    public void stop() {
        log.debug("stop()");
        if (sseThread != null) {
            sseThread.interrupt();
        }
    }

    private Optional<SourceRecord> convertToSourceRecord(String editEventJson) {
        try {
            EditEvent editEvent = objectMapper.readValue(editEventJson, EditEvent.class);
            log.trace("Got an event for {}", editEvent.getMeta().getDomain());
            if (editEvent.getMeta().getDomain().startsWith(languageToSelect)) {
                log.debug("select event for forwarding\n{}", editEvent);
                SourceRecord sourceRecord = sourcerecord()
                        .topic(editEvent.getMeta().getTopic())
                        .partition(editEvent.getMeta().getPartition())
                        .domain(editEvent.getMeta().getDomain())
                        .offset(editEvent.getMeta().getOffset())
                        .user(editEvent.getUser())
                        .title(editEvent.getTitle())
                        .comment(editEvent.getComment())
                        .bot(editEvent.getBot())
                        .sizeOld(editEvent.oldLength())
                        .sizeNew(editEvent.newLength())
                        .timestamp(editEvent.getMeta().getDt())
                        .build();
                return Optional.ofNullable(sourceRecord);
            } else {
                return Optional.empty();
            }
        } catch (JsonProcessingException e) {
            log.warn("Failed to process payload, skipping record:\n{}", editEventJson);
            return Optional.empty();
        }
    }

    @Builder(builderMethodName = "sourcerecord")
    private SourceRecord buildSourceRecord(String topic, Integer partition, String domain, Long offset, String user, String title, String comment, boolean bot, Integer sizeOld, Integer sizeNew, String timestamp) {
        Map<String, Object> sourcePartition = new HashMap<>();
        sourcePartition.put("topic", topic);
        sourcePartition.put("partition", partition);
        sourcePartition.put("domain", domain);

        Map<String, Object> sourceOffset = new HashMap<>();
        sourceOffset.put("offset", offset);

        ObjectMapper mapper = new ObjectMapper();
        try {
            String payload = mapper.writeValueAsString(new Payload(bot, sizeOld, sizeNew, timestamp, user, title, comment));
            return new SourceRecord(sourcePartition, sourceOffset, outputTopic, Schema.STRING_SCHEMA, payload);
        } catch (JsonProcessingException e) {
            log.error("Failed to generate payload, skipping record");
            return null;
        }
    }

    /**
     * Class for JSON payload generation.
     */
    @Data
    @AllArgsConstructor
    private static class Payload {
        private Boolean bot;
        private Integer sizeOld;
        private Integer sizeNew;
        private String timestamp;
        private String user;
        private String title;
        private String comment;
    }

}