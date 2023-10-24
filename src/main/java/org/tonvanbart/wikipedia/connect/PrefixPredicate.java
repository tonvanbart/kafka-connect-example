package org.tonvanbart.wikipedia.connect;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.config.AbstractConfig;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.connect.connector.ConnectRecord;
import org.apache.kafka.connect.transforms.predicates.Predicate;

import java.util.Map;

/**
 * An example predicate which can be used to drop messages that do not start
 * with a given substring.
 */
@Slf4j
public class PrefixPredicate implements Predicate {

    private static final String FILTER_PREFIX = "filter.prefix";

    private static final ConfigDef CONFIG_DEF = new ConfigDef()
                    .define(FILTER_PREFIX,
                            ConfigDef.Type.STRING,
                            ConfigDef.Importance.MEDIUM,
                            "The prefix string to filter on");

    private String filterPrefix;

    @Override
    public ConfigDef config() {
        return new ConfigDef(CONFIG_DEF);
    }

    @Override
    public boolean test(ConnectRecord connectRecord) {
        String value = (String) connectRecord.value();
        return !value.startsWith(filterPrefix);
    }

    @Override
    public void configure(Map<String, ?> map) {
        log.debug("configure({})", map);
        var config = new AbstractConfig(config(), map);
        filterPrefix = config.getString(FILTER_PREFIX);
        log.info("keeping only records starting '{}'", filterPrefix);
    }

    @Override
    public void close() {
        // no action
    }
}
