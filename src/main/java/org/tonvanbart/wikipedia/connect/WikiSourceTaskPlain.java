package org.tonvanbart.wikipedia.connect;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.KafkaException;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class WikiSourceTaskPlain extends SourceTask {

    private String languageToSelect;

    private String outputTopic;

    private Stream<String> sseEvents;

    private final BlockingQueue<String> incomingEvents = new LinkedBlockingQueue<>();

    private ObjectMapper objectMapper;

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
        try {
            var uri = new URI("https://stream.wikimedia.org/v2/stream/recentchange");
            var httpClient = HttpClient.newHttpClient();
            var httpRequest = HttpRequest.newBuilder(uri).GET().build();
            this.sseEvents = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofLines()).body();
        } catch (URISyntaxException | IOException | InterruptedException e) {
            log.error("Source task failed to start", e);
            throw new KafkaException(e);
        }
        if (sseEvents != null) {
            log.debug("got an SSE event stream");
            sseEvents.filter(line -> line.startsWith("data: "))
                    .map(line -> line.substring("data: ".length()))
                    .forEach(incomingEvents::offer);
        }
    }

    @Override
    public List<SourceRecord> poll() throws InterruptedException {
        log.debug("poll()");
        List<String> linesToSend = new ArrayList<>();
        incomingEvents.drainTo(linesToSend);
        return linesToSend.stream()
                .map(this::convertToSourceRecord)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    @Override
    public void stop() {
        if (sseEvents != null) {
            sseEvents.close();
        }
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
