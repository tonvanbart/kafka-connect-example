package org.tonvanbart.wikipedia.connect;

import org.apache.kafka.connect.source.SourceRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.ws.rs.sse.InboundSseEvent;
import javax.ws.rs.sse.SseEventSource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class WikiSourceTaskTest {

    @Mock
    private SseEventSource eventSource;

    @Mock
    private InboundSseEvent inboundEvent;

    private WikiSourceTask wikiSourceTask;

    @BeforeEach
    void createFixture() {
        wikiSourceTask = new WikiSourceTask() {
            @Override
            SseEventSource createEventSource() {
                return eventSource;
            }
        };
    }

    @Test
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
        List<SourceRecord> sourceRecords1 = wikiSourceTask.poll();
        System.out.println("sourceRecords1.size() = " + sourceRecords1.size());
        assertTrue(sourceRecords1.size() > 0, "There should be some sourcerecords");
        Thread.sleep(4000);
        wikiSourceTask.stop();
//        List<SourceRecord> sourceRecords = wikiSourceTask.poll();
//        System.out.println("Got "+sourceRecords.size() + " source records");
//        sourceRecords.forEach(System.out::println);
    }

}
