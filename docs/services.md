# Services

If you have security on your cluster, you can use `kafka-gitops` to manage ACLs. Kafka GitOps automatically builds the necessary ACLs for most use cases. 

## Application Example

A basic example shown below defines one topic, `test-topic`, and one service, `my-application`. 

The service `my-application` both consumes from and produces to `test-topic`. This will generate the necessary ACLs for `my-application` to do this.

?> **Note**: If using Confluent Cloud, omit the principal field.

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

!> Currently, consumer `group.id` must match the service name.

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

## Kafka Connect Example

A basic example which defines one Kafka Connect cluster that has one connector running. 

**Connect Topics**
Currently, kafka connect's internal topics must be defined in the following format:

- `connect-configs-{service-name}`
- `connect-offsets-{service-name}`
- `connect-status-{service-name}`

In this example, the connect cluster `group.id` should be `my-connect-cluster`. 

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
