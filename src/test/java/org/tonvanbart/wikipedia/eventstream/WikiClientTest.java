package org.tonvanbart.wikipedia.eventstream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class WikiClientTest {

    private WikiClient wikiClient;

    @BeforeEach
    void setupClient() throws Exception {
        wikiClient = new WikiClient();
    }

    @Test
    @Disabled("manual run only, endless loop!")
    void testConsuming() throws Exception {
        wikiClient.consumeEventStream("https://stream.wikimedia.org/v2/stream/recentchange");
    }

    @Test
    void testAsync() throws Exception {
        wikiClient.consumeAsync("https://stream.wikimedia.org/v2/stream/recentchange");
    }
}
