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
public class WikiSourceTextTask extends SourceTask {

    private String outputTopic;

    private final BlockingQueue<String> incomingEvents = new LinkedBlockingQueue<>();

    private long lastPoll = 0L;

    private Thread sseThread;

    public static final String TASK_ID = "task.id";

    private long index = 0;

    @Override
    public String version() {
        log.debug("version()");
        return WikiSourceConfig.getVersionAndDate();
    }

    @Override
    public void start(Map<String, String> configProps) {
        log.debug("start({})", configProps);
        var wikiSourceConfig = new WikiSourceConfig(configProps);
        outputTopic = wikiSourceConfig.getTargetTopicConfig();
        sseThread = new Thread(this::runSse);
        sseThread.start();
    }

    private void runSse() {
        Stream<String> sseEvents = null;
        try {
            var uri = new URI("https://stream.wikimedia.org/v2/stream/recentchange");
            var httpClient = HttpClient.newHttpClient();
            var httpRequest = HttpRequest.newBuilder(uri).GET().build();
            sseEvents = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofLines()).body();
            log.debug("got an SSE event stream");
            sseEvents.forEach(incomingEvents::offer);

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
        return linesToSend.stream()
                .map(this::convertToSourceRecord)
                .collect(Collectors.toList());

    }

    @Override
    public void stop() {
        log.debug("stop()");
        if (sseThread != null) {
            sseThread.interrupt();
        }
    }

    private SourceRecord convertToSourceRecord(String text) {
        Map<String, String> sourcePartition = Collections.singletonMap("source", "https://stream.wikimedia.org/v2/stream/recentchange");
        Map<String, Long> sourceOffset = Collections.singletonMap("index", index++);
        return new SourceRecord(sourcePartition, sourceOffset, outputTopic, Schema.STRING_SCHEMA, text);

    }
}