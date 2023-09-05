package org.tonvanbart.wikipedia.connect;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import javax.ws.rs.RuntimeType;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.sse.InboundSseEvent;
import javax.ws.rs.sse.SseEventSource;

import lombok.extern.slf4j.Slf4j;
import org.apache.cxf.jaxrs.impl.ConfigurationImpl;

@Slf4j
public class WikiSourceEventHandler implements Consumer<InboundSseEvent> {

    private final BlockingQueue<String> incomingEvents = new LinkedBlockingQueue<>();
    private static final String EDIT_STREAM_URL = "https://stream.wikimedia.org/v2/stream/recentchange";
    private final SseEventSource eventSource;

    public WikiSourceEventHandler() {
        log.debug("enter WikiSourceEventHandler constructor");
        eventSource = createEventSource();
//        eventSource.register(this);
    }

    public void start() {
        log.info("start()");
//        log.info("before open(), isOpen() is {}", eventSource.isOpen());
//        eventSource.open();
//        log.info("after open(), isOpen() is {}", eventSource.isOpen());
    }

    public void stop() {
        log.info("stop()");
        eventSource.close();
    }

    @Override
    public void accept(InboundSseEvent inboundEvent) {
        log.debug("handleEvent({})", inboundEvent.getName());
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
//        ConfigurationImpl configuration = new ConfigurationImpl(RuntimeType.CLIENT);
//        configuration.setProperty("scheduledExecutorService", Executors.newScheduledThreadPool(2));
//        Client client = ClientBuilder.newClient(configuration);
//        Client client = ClientBuilder.newBuilder().newClient();
        Client client = ClientBuilder.newClient();
        WebTarget webTarget = client.target(EDIT_STREAM_URL);
//        SseEventSource.Builder builder = SseEventSource.target(webTarget);
//        log.debug("builder is a {}", builder.getClass().getName());
//        builder = builder.reconnectingEvery(1, TimeUnit.SECONDS);
//        return builder.build();


        SseEventSource sseEventSource = SseEventSource.target(webTarget).build();
        sseEventSource.register(this);
        sseEventSource.open();
        log.debug("end of createEventSource, isOpen={}", sseEventSource.isOpen());
        return sseEventSource;
    }
}
