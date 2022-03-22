package org.tonvanbart.wikipedia.connect;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.connect.source.SourceRecord;
import org.apache.kafka.connect.source.SourceTask;
import org.glassfish.jersey.media.sse.EventListener;
import org.glassfish.jersey.media.sse.EventSource;
import org.glassfish.jersey.media.sse.InboundEvent;
import org.glassfish.jersey.media.sse.SseFeature;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * The task contains the actual logic to handle records coming in from Wikipedia.
 */
@Slf4j
public class WikiSourceTask extends SourceTask {

    private BlockingQueue<String> incomingEvents = new LinkedBlockingQueue<>();

    private EventSource eventSource;

    private WikiSourceConfig config;

    public static final String TASK_ID = "task.id";

    private static final String EDIT_STREAM_URL = "https://stream.wikimedia.org/v2/stream/recentchange";

    @Override
    public void start(Map<String, String> props) {
        config = new WikiSourceConfig(props);
        Client client = ClientBuilder.newBuilder()
                .register(SseFeature.class)
                .build();
        WebTarget webTarget = client.target(EDIT_STREAM_URL);
        eventSource = EventSource.target(webTarget).build();
        eventSource.register(this::handleEvent);
        eventSource.open();
    }

    @Override
    public List<SourceRecord> poll() throws InterruptedException {
        List<String> eventsToSend = new ArrayList<>();
        incomingEvents.drainTo(eventsToSend);
        return null;
    }

    @Override
    public void stop() {
        eventSource.close();
    }

    @Override
    public String version() {
        // TODO get from Maven POM.
        return "0.0.1";
    }

    private void handleEvent(InboundEvent inboundEvent) {
        try {
            incomingEvents.put(inboundEvent.readData());
        } catch (InterruptedException e) {
            log.error("Error queueing event, stopping task", e);
            this.stop();
        }
    }

    private SourceRecord convertToSourceRecord(String editEvent) {
//        return new SourceRecord()
        return null;
    }
}
