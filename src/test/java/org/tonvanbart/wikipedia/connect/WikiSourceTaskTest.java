package org.tonvanbart.wikipedia.connect;

import org.glassfish.jersey.media.sse.EventSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
class WikiSourceTaskTest {

    @Mock
    private EventSource eventSource;

    private WikiSourceTask wikiSourceTask;

    @BeforeEach
    void createFixture() {
        wikiSourceTask = new WikiSourceTask() {
            @Override
            EventSource createEventSource() {
                return eventSource;
            }
        };
    }

    @Test
    void testTaskCreation() {
        assertNotNull(wikiSourceTask);
    }
}
