package org.tonvanbart.wikipedia;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.connect.connector.ConnectRecord;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.source.SourceRecord;
import org.apache.kafka.connect.transforms.Transformation;
import org.tonvanbart.wikipedia.eventstream.EditEvent;

import java.util.Map;

import lombok.extern.slf4j.Slf4j;

/**
 * Converts a SourceRecord containing JSON data from Wikipedia
 * to an instance of {@link org.tonvanbart.wikipedia.eventstream.EditEvent}.
 */
@Slf4j
public class EditEventTransform implements Transformation {

    private final static String DATA_PREFIX =  "data: ";

    private ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public ConnectRecord apply(ConnectRecord record) {
        log.debug("apply({})", record);
        final var schema = record.valueSchema();
        String value = (String) record.value();
        if (!value.startsWith(DATA_PREFIX)) {
            // should have been filtered before
            log.warn("Skipping unparseable value: '{}'", value);
            return null;
        }
        String jsonString = value.substring(DATA_PREFIX.length());
        try {
            EditEvent editEvent = objectMapper.readValue(jsonString, EditEvent.class);
            return record.newRecord(record.topic(), record.kafkaPartition(), null, record.key(), null, editEvent,System.currentTimeMillis());
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse event, skipping", e);
            return null;
        }
    }

    @Override
    public ConfigDef config() {
        // being lazy, no configs
        return new ConfigDef();
    }

    @Override
    public void close() {
        // no action
    }

    @Override
    public void configure(Map<String, ?> configs) {
        // no configs for now
    }
}
