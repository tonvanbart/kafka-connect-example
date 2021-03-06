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
import org.tonvanbart.wikipedia.eventstream.EditEvent;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.sse.InboundSseEvent;
import javax.ws.rs.sse.SseEventSource;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * The task contains the actual logic to handle records coming in from Wikipedia.
 */
@Slf4j
public class WikiSourceTask extends SourceTask {

    private WikiSourceConfig config;

    private String languageToSelect;

    private String outputTopic;

    private ObjectMapper objectMapper;

    public static final String TASK_ID = "task.id";

    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    private final AtomicLong taskThreadId = new AtomicLong(0);

    private final AtomicLong lastPoll = new AtomicLong(0);

    private final WikiSourceEventHandler eventHandler = new WikiSourceEventHandler();

    @Override
    public void start(Map<String, String> props) {
        log.info("start({})", props);
        config = new WikiSourceConfig(props);
        objectMapper = new ObjectMapper();
        languageToSelect = config.getWikiLanguageConfig();
        outputTopic = config.getTargetTopicConfig();

        eventHandler.start();
        isRunning.set(true);
        taskThreadId.set(Thread.currentThread().getId());
    }

    @Override
    public List<SourceRecord> poll() throws InterruptedException {
        log.debug("poll() - isRunning = {}", isRunning.get());
        if (isRunning.get()) {
            long nextPoll = lastPoll.longValue() + 3000;
            long now = System.currentTimeMillis();
            long sleepInterval = Math.min(nextPoll - now, 3000);
            if (sleepInterval > 0) {
                log.info("Pausing {} ms until next poll", sleepInterval);
                try {
                    Thread.sleep(sleepInterval);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            lastPoll.set(System.currentTimeMillis());
            List<String> eventsToSend = new ArrayList<>();
            eventHandler.drainTo(eventsToSend);
            log.debug("processing {} events", eventsToSend.size());
            return eventsToSend.stream()
                    .map(this::convertToSourceRecord)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toList());
        } else {
            log.info("Closing resources");
            eventHandler.stop();
            return null;
        }
    }

    @Override
    public void stop() {
        log.info("stop()");
        isRunning.set(false);
        if (taskThreadId.longValue() == Thread.currentThread().getId()) {
            eventHandler.stop();
        }
    }

    @Override
    public String version() {
        // TODO get from Maven POM.
        return "0.0.1";
    }

    private Optional<SourceRecord> convertToSourceRecord(String editEventJson) {
        try {
            EditEvent editEvent = objectMapper.readValue(editEventJson, EditEvent.class);
            log.debug("Got an event for {}", editEvent.getMeta().getDomain());
            if (editEvent.getMeta().getDomain().startsWith(languageToSelect)) {
                log.debug("select event for forwarding");
                SourceRecord sourceRecord = sourcerecord()
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
            String payload = mapper.writeValueAsString(new Payload(user, title, comment));
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
