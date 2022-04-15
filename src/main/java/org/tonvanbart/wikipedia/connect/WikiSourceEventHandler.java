package org.tonvanbart.wikipedia.connect;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.sse.InboundSseEvent;
import javax.ws.rs.sse.SseEventSource;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WikiSourceEventHandler implements Consumer<InboundSseEvent> {

    private final BlockingQueue<String> incomingEvents = new LinkedBlockingQueue<>();
    private static final String EDIT_STREAM_URL = "https://stream.wikimedia.org/v2/stream/recentchange";
    private final SseEventSource eventSource;

    public WikiSourceEventHandler() {
        eventSource = createEventSource();
        eventSource.register(this);
    }

    public void start() {
        eventSource.open();
    }

    public void stop() {
        eventSource.close();
    }

    @Override
    public void accept(InboundSseEvent inboundEvent) {
        log.info("handleEvent({})", inboundEvent.getName());
        try {
            if ("message".equals(inboundEvent.getName())) {
                incomingEvents.put(inboundEvent.readData());
            }
        } catch (InterruptedException e) {
            log.error("Error queueing event, stopping task", e);
        }
    }

    public void drainTo(List<String> destination) {
        log.info("drainTo({} events)", incomingEvents.size());
        incomingEvents.drainTo(destination);
    }

    /**
     * Create event source instance for this task.
     * This method is package protected to be able to inject a mock.
     * @return
     */
    SseEventSource createEventSource() {
        log.debug("createEventSource()");
        Client client = ClientBuilder.newClient();
        WebTarget webTarget = client.target(EDIT_STREAM_URL);
        return SseEventSource.target(webTarget).build();
    }
}
