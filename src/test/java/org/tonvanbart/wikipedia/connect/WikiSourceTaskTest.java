package org.tonvanbart.wikipedia.connect;

import org.apache.kafka.connect.source.SourceRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.ws.rs.sse.InboundSseEvent;
import javax.ws.rs.sse.SseEventSource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
@ExtendWith(MockitoExtension.class)
class WikiSourceTaskTest {

    @Mock
    private SseEventSource eventSource;

    @Mock
    private InboundSseEvent inboundEvent;

    private WikiSourceTask wikiSourceTask;

    @Test
    @Disabled
    void testTaskCreation() {
        assertNotNull(wikiSourceTask);
    }

    @Test
    void eventIsForwarded() throws Exception {
        wikiSourceTask = new WikiSourceTask();
        Map<String, String> props= new HashMap<>();
        props.put("wiki.language", "en");
        props.put("target.topic", "en-edits");
        wikiSourceTask.start(props);
        Thread.sleep(4000);
        List<SourceRecord> sourceRecords = wikiSourceTask.poll();
        log.info("sourceRecords.size() = {}", sourceRecords.size());
        assertTrue(sourceRecords.size() > 0, "There should be some sourcerecords");
        wikiSourceTask.stop();
        sourceRecords.forEach(System.out::println);
    }

}
