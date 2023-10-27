package org.tonvanbart.wikipedia.connect;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.config.AbstractConfig;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.connect.connector.ConnectRecord;
import org.apache.kafka.connect.transforms.predicates.Predicate;
import org.tonvanbart.wikipedia.eventstream.EditEvent;

import java.util.Map;

/**
 * A Predicate to select {@link org.tonvanbart.wikipedia.eventstream.EditEvent}s on their language.
 */
@Slf4j
public class EditEventPredicate implements Predicate {

    private static final String WIKI_LANGUAGE = "select.language";

    private static final ConfigDef CONFIG_DEF = new ConfigDef()
            .define(WIKI_LANGUAGE,
                    ConfigDef.Type.STRING,
                    ConfigDef.Importance.MEDIUM,
                    "The wiki language for which to keep events");

    private String selectedLanguage;

    @Override
    public ConfigDef config() {
        return new ConfigDef(CONFIG_DEF);
    }

    @Override
    public boolean test(ConnectRecord connectRecord) {
        try {
            EditEvent editEvent = (EditEvent) connectRecord.value();
            return !editEvent.getMeta().getDomain().startsWith(selectedLanguage);
        } catch (ClassCastException e) {
            log.warn("Wrong type in connectRecord, skipping", e);
            return true;
        }
    }

    @Override
    public void close() {
        log.debug("close()");
        // no action
    }

    @Override
    public void configure(Map<String, ?> map) {
        log.info("configure({})", map);
        var config = new AbstractConfig(config(), map);
        selectedLanguage = config.getString(WIKI_LANGUAGE);
        log.info("Selecting wiki updates for '{}'", selectedLanguage);
    }
}
