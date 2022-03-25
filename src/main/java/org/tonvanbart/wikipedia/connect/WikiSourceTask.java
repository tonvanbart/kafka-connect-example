package org.tonvanbart.wikipedia.connect;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.source.SourceRecord;
import org.apache.kafka.connect.source.SourceTask;
import org.glassfish.jersey.media.sse.EventSource;
import org.glassfish.jersey.media.sse.InboundEvent;
import org.glassfish.jersey.media.sse.SseFeature;
import org.tonvanbart.wikipedia.eventstream.EditEvent;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

/**
 * The task contains the actual logic to handle records coming in from Wikipedia.
 */
@Slf4j
public class WikiSourceTask extends SourceTask {

    private BlockingQueue<String> incomingEvents = new LinkedBlockingQueue<>();

    private EventSource eventSource;

    private WikiSourceConfig config;

    private String languageToSelect;

    private String outputTopic;

    private ObjectMapper objectMapper;

    public static final String TASK_ID = "task.id";

    private static final String EDIT_STREAM_URL = "https://stream.wikimedia.org/v2/stream/recentchange";

    @Override
    public void start(Map<String, String> props) {
        log.debug("start({})", props);
        config = new WikiSourceConfig(props);
        objectMapper = new ObjectMapper();
        languageToSelect = config.getWikiLanguageConfig();
        outputTopic = config.getTargetTopicConfig();

        eventSource = createEventSource();
        eventSource.register(this::handleEvent);
        eventSource.open();
    }

    @Override
    public List<SourceRecord> poll() throws InterruptedException {
        log.debug("poll({} events)", incomingEvents.size());
        List<String> eventsToSend = new ArrayList<>();
        incomingEvents.drainTo(eventsToSend);
        log.debug("processing {} events", eventsToSend.size());
        return eventsToSend.stream()
                .map(this::convertToSourceRecord)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    @Override
    public void stop() {
        log.debug("stop()");
        eventSource.close();
    }

    @Override
    public String version() {
        // TODO get from Maven POM.
        return "0.0.1";
    }

    /**
     * Create event source instance for this task.
     * This method is package protected to be able to inject a mock.
     * @return
     */
    EventSource createEventSource() {
        log.debug("createEventSource()");
        Client client = ClientBuilder.newBuilder()
                .register(SseFeature.class)
                .build();
        WebTarget webTarget = client.target(EDIT_STREAM_URL);
        return EventSource.target(webTarget).build();
    }

    void handleEvent(InboundEvent inboundEvent) {
        log.debug("handleEvent({})", inboundEvent.getName());
        try {
            if ("message".equals(inboundEvent.getName())) {
                incomingEvents.put(inboundEvent.readData());
            }
        } catch (InterruptedException e) {
            log.error("Error queueing event, stopping task", e);
            this.stop();
        }
    }

    private Optional<SourceRecord> convertToSourceRecord(String editEventJson) {
        try {
            var editEvent = objectMapper.readValue(editEventJson, EditEvent.class);
            log.debug("Got an event for {}", editEvent.getMeta().getDomain());
            if (editEvent.getMeta().getDomain().startsWith(languageToSelect)) {
                log.debug("select event for forwarding");
                var sourceRecord = sourcerecord()
                        .topic(editEvent.getMeta().getTopic())
                        .partition(editEvent.getMeta().getPartition())
                        .domain(editEvent.getMeta().getDomain())
                        .offset(editEvent.getMeta().getOffset())
                        .user(editEvent.getUser())
                        .title(editEvent.getTitle())
                        .comment(editEvent.getComment())
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
    private SourceRecord buildSourceRecord(String topic, Integer partition, String domain, Long offset, String user, String title, String comment) {
        Map<String, Object> sourcePartition = new HashMap<>();
        sourcePartition.put("topic", topic);
        sourcePartition.put("partition", partition);
        sourcePartition.put("domain", domain);

        Map<String, Object> sourceOffset = new HashMap<>();
        sourceOffset.put("offset", offset);

        ObjectMapper mapper = new ObjectMapper();
        try {
            var payload = mapper.writeValueAsString(new Payload(user, title, comment));
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
        private String user;
        private String title;
        private String comment;

    }
}
