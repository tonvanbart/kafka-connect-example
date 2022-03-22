package org.tonvanbart.wikipedia.connect;

import org.apache.kafka.common.config.AbstractConfig;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.ConfigException;

import java.util.Map;

/**
 * Example configuration for the Wiki edits connector.
 */
public class WikiSourceConfig extends AbstractConfig {

    static final String WIKI_LANGUAGE_CONFIG = "wiki.language";
    static final String WIKI_LANGUAGE_CONFIG_DEFAULT = "nl";
    static final String WIKI_LANGUAGE_CONFIG_DOC = "The two letter language code of the wikipedia entries we want to follow";

    static final String TARGET_TOPIC_CONFIG = "target.topic";
    static final String TARGET_TOPIC_DOC = "The Kafka topic where edit events are sent to.";
    private static final ConfigDef CONFIG_DEF;

    static {
        CONFIG_DEF = new ConfigDef()
                .define(WIKI_LANGUAGE_CONFIG,
                        ConfigDef.Type.STRING,
                        WIKI_LANGUAGE_CONFIG_DEFAULT,
                        new LanguageCodeValidator(),
                        ConfigDef.Importance.MEDIUM,
                        WIKI_LANGUAGE_CONFIG_DOC).
                define(TARGET_TOPIC_CONFIG,
                        ConfigDef.Type.STRING,
                        ConfigDef.NO_DEFAULT_VALUE,
                        ConfigDef.CompositeValidator.of(new ConfigDef.NonNullValidator(), new ConfigDef.NonEmptyString()),
                        ConfigDef.Importance.MEDIUM,
                        TARGET_TOPIC_DOC)
            ;
    }

    public static ConfigDef configDef() {
        return new ConfigDef(CONFIG_DEF);
    }

    public WikiSourceConfig(Map<?, ?> originals) {
        super(configDef(), originals);
    }

    /**
     * Return the configured language code, or the default if none set.
     * @return
     */
    public String getWikiLanguageConfig() {
        return getString(WIKI_LANGUAGE_CONFIG);
    }

    /**
     * Return the configured target topic.
     * @return
     */
    public String getTargetTopicConfig() {
        return getString(TARGET_TOPIC_CONFIG);
    }

    /**
     * Example Validator implementation for the wiki language code.
     */
    public static class LanguageCodeValidator implements ConfigDef.Validator {

        @Override
        public void ensureValid(String name, Object value) {
            if (!(value instanceof String)) {
                throw new ConfigException(name, value.getClass(), "Value should be a two letter string");
            }
            if (((String) value).length() != 2) {
                throw new ConfigException(name, value, "Value should be a two letter string");
            }
        }

        @Override
        public String toString() {
            return "A two letter language code";
        }
    }
}
