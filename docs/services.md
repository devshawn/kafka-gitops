# Services

If you have security on your cluster, you can use `kafka-gitops` to manage ACLs. Kafka GitOps automatically builds the necessary ACLs for most use cases. 

## Application Example

A basic example shown below defines one topic, `test-topic`, and one service, `my-application`. 

The service `my-application` both consumes from and produces to `test-topic`. This will generate the necessary ACLs for `my-application` to do this.

?> **NOTE**: If using Confluent Cloud, omit the principal field.

```yaml
topics:
  test-topic:
    partitions: 6
    replication: 1

services:
  my-application:
    type: application
    principal: User:myapp
    consumes:
      - test-topic
    produces:
      - test-topic
```

Behind the scenes, this will generate three ACLs:

- `READ` for topic `test-topic`
- `WRITE` for topic `test-topic`
- `READ` for consumer group `my-application`

#### Group ID

The `group.id` used for consumer ACLs defaults to the service name. You can override this by specifying the `group-id` property, as shown below:

```yaml
services:
  my-application:
    type: application
    principal: User:myapp
    group-id: my-application-service
    consumes:
      - test-topic
    produces:
      - test-topic
```

This would allow your consumer group to access kafka using `my-application-service` as the `group.id`.

## Kafka Streams Example

A basic example shown below defines one topic, `test-topic`, and one kafka streams application, `my-stream`. 

The service `my-stream` both consumes from and produces to `test-topic`. This will generate the necessary ACLs for `my-stream` to do this.

```yaml
topics:
  test-topic:
    partitions: 6
    replication: 1

services:
  my-stream:
    type: kafka-streams
    principal: User:mystream
    consumes:
      - test-topic
    produces:
      - test-topic
```

Behind the scenes, this generates ACLs such as:

- `READ` for topic `test-topic`
- `WRITE` for topic `test-topic`
- `READ` for consumer group `my-stream`
- Various ACLs for Kafka streams internal topic management

#### Application ID

The `application.id` used for streams ACLs defaults to the service name. You can override this by specifying the `application-id` property, as shown below:

```yaml
services:
  my-stream:
    type: kafka-streams
    principal: User:mystream
    application-id: my-stream-application
    consumes:
      - test-topic
    produces:
      - test-topic
```

This would allow your streams application to access kafka using `my-stream-application` as the `application.id`.

## Kafka Connect Example

A basic example which defines one Kafka Connect cluster that has one connector running. 

```yaml
topics:
  connect-configs-my-connect-cluster:
    partitions: 1
    replication: 1
    configs:
      cleanup.policy: compact

  connect-offsets-my-connect-cluster:
    partitions: 25
    replication: 1
    configs:
      cleanup.policy: compact

  connect-status-my-connect-cluster:
    partitions: 5
    replication: 1
    configs:
      cleanup.policy: compact
      
  rabbitmq-data:
    partitions: 6
    replication: 1

services:
  my-connect-cluster:
    type: kafka-connect
    principal: User:myconnectcluster
    connectors:
      rabbitmq-sink:
        consumes:
          - rabbitmq-data
```

Behind the scenes, this generates ACLs such as:

- `READ` for topic `rabbitmq-data`
- `READ` for the consumer group `connect-rabbitmq-sink`
- `READ` and `WRITE` for the internal kafka connect topics
- `READ` for the consumer group `my-connect-cluster`

#### Storage Topics

By default, `kafka-gitops` generates ACLs for the internal storage topics following this format:

- `connect-configs-{service-name}`
- `connect-offsets-{service-name}`
- `connect-status-{service-name}`

You can specify custom internal storage topics using the `storage-topics` property:

```yaml
services:
  my-connect-cluster:
    type: kafka-connect
    principal: User:myconnectcluster
    storage-topics:
      config: custom-config-topic
      offset: custom-offset-topic
      status: custom-status-topic
    connectors:
      rabbitmq-sink:
        consumes:
          - rabbitmq-data
```

#### Group ID

The `group.id` used for the connect cluster ACLs defaults to the service name. You can override this by specifying the `group-id` property, as shown below:

```yaml
services:
  my-connect-cluster:
    type: kafka-connect
    principal: User:myconnectcluster
    group-id: kafka-connect-cluster
    connectors:
      rabbitmq-sink:
        consumes:
          - rabbitmq-data
```

This allows your connect cluster to access kafka using `kafka-connect-cluster` as the `group.id`. 

!> **NOTE**: The `group-id` setting only affects the connect cluster `group.id`, and not any sink connector group IDs.
