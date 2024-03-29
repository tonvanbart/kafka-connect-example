## Wikipedia  source connector  

Example source connector which sends Wikipedia edits into Kafka.
There are two implementations:
1. `WikiSourceConnector`/`WikiSourceTask` which is a all-in-one solution: all the logic is in the task
2. `WikiSourceTextConnector`/`WikiSourceTextTask` which only copies records from SSE; the filtering on 
   data records and Wiki language is delegated to Single Message Transforms.
   <br>This second example will store the edits in AVRO format; the AVRO schema is autogenerated and stored in the 
   schema registry which is provided in the Docker compose.

The JSON payloads to install the plugins are in `src/test/resources` and show how the connectors and transforms are
configured.

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
   Check that `target/connect-plugin` contains a `-jar-with-dependencies.jar` which will contain the plugins.
2. Start the docker compose
   ```shell
   docker-compose up # or 'docker compose up', depending on your docker desktop version
   ```
3. Verify that the connect plugins are found, in another terminal
   ```shell
    curl localhost:9001/connector-plugins | jq
   ```
   You should see `org.tonvanbart.wikipedia.connect.WikiSourceConnector` and `org.tonvanbart.wikipedia.connect.WikiSourceTextConnector` among the listed classes.
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
6. To create the "text only" connector and its transform chain, post the second payload:
   ```shell
   curl -X POST -H "Content-Type: application/json" -d @src/test/resources/add-source-text-connector.json localhost:9001/connectors | jq
   ```
   In this case, open the included Kowl in a browser on http://localhost:8080 - the "schema registry" tab should show
   a schema `wikievents-value` being registered, on the "topics" tab you should see a topic `wikievents` with some update events.
   <br>_Note:_ schema registration won't happen until an event is received; if you picked a quiet Wiki this might take a few soconds!
  
#### Kafka message format
Messages produced to Kafka by `WikiSourceConnector` are JSON formatted and contain the old and new page size (note: can be 0 in some cases), 
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