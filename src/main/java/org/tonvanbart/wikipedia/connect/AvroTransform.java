package org.tonvanbart.wikipedia.connect;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.avro.Schema;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.connect.connector.ConnectRecord;
import org.apache.kafka.connect.source.SourceRecord;
import org.apache.kafka.connect.transforms.Transformation;
import org.tonvanbart.wikipedia.eventstream.EditEvent;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

/**
 * Transform the incoming JSON events from Wikipedia to AVRO records.
 */
@Slf4j
public class AvroTransform implements Transformation {

    private ObjectMapper objectMapper = new ObjectMapper();

    private long index = 0;

//    private static Schema SCHEMA;

//    static {
//        try {
//            final var schemaString = Files.readString(Paths.get(AvroTransform.class.getResource("wikiupdate.avsc").toURI()));
//            SCHEMA = new Schema.Parser().parse(schemaString);
//        } catch (URISyntaxException | IOException e) {
//             should never happen
//            log.error("Failed to read schema", e);
//        }
//    }

    /**
     * Convert a source record with a String containing `data :` and a JSON payload
     * to a record containing a {@link WikiUpdate}, AVRO encoded
     * @param connectRecord
     * @return
     */
    @Override
    public ConnectRecord apply(ConnectRecord connectRecord) {
        final String value = (String) connectRecord.value();
        if (!value.startsWith("data: ")) {
            log.error("skipping malformed record: '{}'", value);
            return null;
        }
        final String json = value.substring("data: ".length());
        try {
            EditEvent editEvent = objectMapper.readValue(json, EditEvent.class);
            final var wikiUpdate = WikiUpdate.builder().bot(editEvent.getBot())
                    .timestamp(editEvent.getMeta().getDt())
                    .sizeNew(editEvent.newLength())
                    .sizeOld(editEvent.oldLength())
                    .user(editEvent.getUser())
                    .comment(editEvent.getComment())
                    .title(editEvent.getTitle())
                    .build();
            Map<String, String> sourcePartition = Collections.singletonMap("source", "https://stream.wikimedia.org/v2/stream/recentchange");
            Map<String, Long> sourceOffset = Collections.singletonMap("index", index++);

//            return new SourceRecord(sourcePartition, sourceOffset, "wikievents", SCHEMA, wikiUpdate);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize JSON into EditEvent, skipping", e);
            return null;
        }

        return null;
    }

    @Override
    public ConfigDef config() {
        // no configs for now
        return new ConfigDef();
    }

    @Override
    public void close() {
        // no action
    }

    @Override
    public void configure(Map<String, ?> configs) {
        // no config for now
    }
}
