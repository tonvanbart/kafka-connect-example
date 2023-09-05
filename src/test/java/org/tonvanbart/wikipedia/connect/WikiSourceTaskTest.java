package org.tonvanbart.wikipedia.connect;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.connect.source.SourceRecord;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
@ExtendWith(MockitoExtension.class)
class WikiSourceTaskTest {

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

    @Test
    void testPlainJavaClient() throws Exception {
        try {
            var uri = new URI("https://stream.wikimedia.org/v2/stream/recentchange");
            var httpClient = HttpClient.newHttpClient();
            var httpRequest = HttpRequest.newBuilder(uri).GET().build();
            var lines = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofLines()).body();
//        lines.forEach(System.out::println);
            lines.map(line -> line.split("\n"))
                    .flatMap(linearr -> Arrays.stream(linearr))
                    .filter(line -> line.startsWith("data: "))
                    .map(line -> line.substring("data: ".length()))
                    .forEach(System.out::println);
        } finally {
            System.out.println("---\nwrapping up...\n---");
        }
//        lines.close();
    }

}
