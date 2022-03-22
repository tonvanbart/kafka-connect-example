package org.tonvanbart.wikipedia.eventstream;

import lombok.extern.slf4j.Slf4j;
import org.glassfish.jersey.media.sse.*;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

@Slf4j
public class WikiClient {

    public void consumeEventStream(String url) throws Exception {
        log.debug("consumeEventStream()");
        Client client = ClientBuilder.newBuilder().register(new SseFeature()).build();
        WebTarget target = client.target(url);
        EventInput eventInput = null;
        while (true) {
            Thread.sleep(100);
            if (eventInput == null || eventInput.isClosed()) {
                // (re)connect
                eventInput = target.request().get(EventInput.class);
                eventInput.setChunkType("text/event-stream");
            }

            final InboundEvent inboundEvent = eventInput.read();
            log.info("InboundEvent={}", inboundEvent);
            if (inboundEvent == null) {
                break;
            }
            else {
                String data = inboundEvent.readData();
                log.info("got data: {}", data);
                // do something here - notify observers, parse json etc
            }

        }
        log.info("connection closed");
    }

    void consumeAsync(String url) throws InterruptedException {
        log.info("consumeAsync({})", url);
        Client client = ClientBuilder.newBuilder()
                .register(SseFeature.class)
                .build();
        WebTarget webTarget = client.target(url);
        EventSource eventSource = EventSource.target(webTarget).build();
        EventListener eventListener = inboundEvent -> log.info("name={}, data={}", inboundEvent.getName(), inboundEvent.readData());
        eventSource.register(eventListener);
        log.info("opening event source");
        eventSource.open();
        log.info("sleeping thread");
        Thread.sleep(3000);
        log.info("closing event source");
        eventSource.close();
    }

}
