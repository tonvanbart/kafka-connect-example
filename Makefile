.PHONY: add-connector
add-connector:
	curl -X POST -H "Content-Type: application/json" -d @src/test/resources/add-source-connector.json localhost:9001/connectors | jq

.PHONY: log-debug
log-debug:
	curl -X PUT -H "Content-Type: application/json" http://localhost:9001/admin/loggers/org.tonvanbart -d '{"level":"DEBUG"}' | jq

.PHONY: log-info
log-info:
	curl -X PUT -H "Content-Type: application/json" http://localhost:9001/admin/loggers/org.tonvanbart -d '{"level":"INFO"}' | jq

.PHONY: update-config
update-config:
	curl -X PUT -H "Content-Type: application/json" @src/test/resources/update-config.json http://localhost:9001/connectors/wikipedia-source-connector | jq

.PHONY: list-status
list-status:
	curl localhost:9001/connectors/wikipedia-source-connector | jq

.PHONY: list-plugins
list-plugins:
	curl localhost:9001/connector-plugins | jq