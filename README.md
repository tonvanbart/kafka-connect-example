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
    curl localhost:9001/connector-plugins | jq
   ```
   You should see `org.tonvanbart.wikipedia.connect.WikiSourceConnector` among the listed classes.
4. Try to create the connector
   ```shell
   curl -X POST -H "Content-Type: application/json" -d @src/test/resources/add-source-connector.json localhost:9001/connectors | jq
   ```
5. Check if messages are produced; updates to be published will be logged at DEBUG level in the `bitconnect` container log:
   ```shell
   docker logs bitconnect -f 
   ```
   You can use `kafkacat` (`kcat`) to check the topic:
   ```shell
   kafkacat -b localhost:9092 -C -t wikipedia_edits
   ```
  
#### Kafka message format
Messages produced to Kafka are JSON formatted and contain the old and new page size (note: can be 0 in some cases), 
the username of the editing user, Wikipedia page title and edit comment and an indication if the user is a bot.
<br>
See example payload below:
```json
{
  "bot": false,
  "sizeOld": 5139,
  "sizeNew": 5163,
  "timestamp": "2023-09-19T21:43:55Z",
  "user": "Guss",
  "title": "Gebruiker:Guss/Kladblok",
  "comment": "/* Ontspoorde vriendschap / Beschuldiging van misbruik (titel??) */Stijl"
}
```
#### Configuration
The wiki language to follow, and topic to produce to can be configured. 

| property | meaning | default |
|----------|---------|---------|
| `wiki.language` | Two letter language code of the wiki to follow | `nl` |
| `target.topic` | Topic where wiki edits are produced to | _none_ |

#### Makefile targets
For convenience, there is a Makefile which can perform some of the `curl` commands for you:

| Command | result |
|---------|--------|
| `make add-connector` | installs the compiled connector |
| `make log-debug` | set log level to DEBUG for the connector classes |
| `make log-info` | set log level to INFO for the connector classes |
| `make update-config` | Update connector config using values in [`update-config.json`](src/test/resources/update-config.json) |
| `make list-status` | list connector status (needs `jq`) |