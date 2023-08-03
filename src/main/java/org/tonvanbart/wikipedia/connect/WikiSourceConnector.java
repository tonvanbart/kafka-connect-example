package org.tonvanbart.wikipedia.connect;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.connect.connector.Task;
import org.apache.kafka.connect.source.SourceConnector;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
public class WikiSourceConnector extends SourceConnector {

    private final Map<String, String> connectorConfig = new HashMap<>();

    @Override
    public void start(Map<String, String> props) {
        log.debug("start({})", props);
        connectorConfig.clear();
        connectorConfig.putAll(props);
    }

    @Override
    public Class<? extends Task> taskClass() {
        log.debug("taskClass()");
        return WikiSourceTask.class;
    }

    @Override
    public List<Map<String, String>> taskConfigs(int maxTasks) {
        log.debug("taskConfigs({})", maxTasks);
        return IntStream.range(0, maxTasks)
                .mapToObj(this::createTaskConfig)
                .collect(Collectors.toList());
    }

    @Override
    public void stop() {
        log.debug("stop()");
    }

    @Override
    public ConfigDef config() {
        return WikiSourceConfig.configDef();
    }

    @Override
    public String version() {
        return WikiSourceConfig.getProjectVersion() + " built:" + WikiSourceConfig.getBuildDate();
    }

    private Map<String, String> createTaskConfig(int taskNumber) {
        HashMap<String, String> taskConfig = new HashMap<>(connectorConfig);
        taskConfig.put(WikiSourceTask.TASK_ID, Integer.toString(taskNumber));
        return taskConfig;
    }
}
