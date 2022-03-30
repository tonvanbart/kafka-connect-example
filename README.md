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
   
### Checking the original SSE stream
```shell
curl -N https://stream.wikimedia.org/v2/stream/recentchange
```