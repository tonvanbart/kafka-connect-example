## Wikipedia  source connector  

Example source connector which sends Wikipedia edits into Kafka.

### Prerequistes
- Java 11 or up
- Docker desktop
- `jq` (optional, but convenient for formatting json output)
- `kafkacat` (or `kcat` if you are on a Mac; can be installed by Homebrew)

### Building and running
Instructions in telegram style:

1. Build the project
   ```shell
   mvn clean verify
   ```
   Check that `target/connect-plugin` contains a `-jar-with-dependencies.jar` which will contain the plugin.
2. Start the docker compose
   ```shell
   docker-compose up # or 'docker compose up', depending on your docker desktop version
   ```
3. Verify that the connect plugin is found, in another terminal
   ```shell
    curl localhost:8082/connector-plugins | jq
   ```
   You should see `org.tonvanbart.wikipedia.connect.WikiSourceConnector` among the listed classes.
4. Try to create the connector
   ```shell
   curl -X POST -H "Content-Type: application/json" -d @src/test/resources/add-source-connector.json localhost:8082/connectors | jq
   ```
   
#### Makefile targets
For convenience, there is a Makefile which can perform some tasks:

| Command | result |
|---------|--------|
| `make add-connector` | installs the compiled connector |
| `make log-debug` | set log level to DEBUG for the connector classes |
| `make log-info` | set log level to INFO for the connector classes |
| `make update-config` | Update connector config using values in [`update-config.json`](src/test/resources/update-config.json) |
| `make list-status` | list connector status (needs `jq`) |