package org.tonvanbart.wikipedia.eventstream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.sse.InboundSseEvent;
import javax.ws.rs.sse.SseEventSource;
import java.util.Optional;

@Slf4j
public class WikiClient {

    private ObjectMapper objectMapper;

    public WikiClient() {
        objectMapper = new ObjectMapper();
    }

//    public void consumeEventStream(String url) throws Exception {
//        log.debug("consumeEventStream()");
//        Client client = ClientBuilder.newBuilder().register(new SseFeature()).build();
//        WebTarget target = client.target(url);
//        EventInput eventInput = null;
//        while (true) {
//            Thread.sleep(100);
//            if (eventInput == null || eventInput.isClosed()) {
//                // (re)connect
//                eventInput = target.request().get(EventInput.class);
//                eventInput.setChunkType("text/event-stream");
//            }
//
//            final InboundEvent inboundEvent = eventInput.read();
//            log.info("InboundEvent={}", inboundEvent);
//            if (inboundEvent == null) {
//                break;
//            }
//            else {
//                String data = inboundEvent.readData();
//                log.info("got data: {}", data);
//                // do something here - notify observers, parse json etc
//            }
//
//        }
//        log.info("connection closed");
//    }

    void consumeAsync(String url) throws InterruptedException {
        log.info("consumeAsync({})", url);
        Client client = ClientBuilder.newClient();
        WebTarget webTarget = client.target(url);
        SseEventSource eventSource = SseEventSource.target(webTarget).build();
//        EventListener eventListener = inboundEvent -> log.info("name={}, data={}", inboundEvent.getName(), inboundEvent.readData());
        eventSource.register(this::handleEvent);
        log.info("opening event source");
        eventSource.open();
        log.info("sleeping thread");
        Thread.sleep(3000);
        log.info("closing event source");
        eventSource.close();
    }

    Optional<EditEvent> handleEvent(InboundSseEvent inboundEvent) {
        log.debug("handleEvent({})", inboundEvent);
        if ( ! "message".equals(inboundEvent.getName())) {
            return Optional.empty();
        }
        try {
            EditEvent editEvent = objectMapper.readValue(inboundEvent.readData(), EditEvent.class);
            log.info("got event {}", editEvent);
            return Optional.of(editEvent);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse event data {}", inboundEvent.readData(), e);
            return Optional.empty();
        }
    }

}
