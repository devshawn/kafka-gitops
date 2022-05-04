# Desired State Specification

This document describes the specification for how to write your Kafka cluster's desired state file. This currently must be a `YAML` file. 

?> Current version: `1.0.3`

The desired state file consists of:

- **Settings** [Optional]: Specific settings for configuring `kafka-gitops`.
- **Topics** [Optional]: Topic and topic configuration definitions.
- **Services** [Optional]: Service definitions for generating ACLs.
- **Users** [Optional]: User definitions for generating ACLs.
- **Custom Service ACLs** [Optional]: Definitions for custom, non-generated ACLs.
- **Custom User ACLs** [Optional]: Definitions for custom, non-generated ACLs.

## Settings

**Synopsis**: These are specific settings for configuring `kafka-gitops`. 

**Options**:

- **confluent** [Optional]: An object which contains an `enabled` field. Set this to true if using a Confluent Cloud cluster. 
- **topics** [Optional]: 
    - **defaults** [Optional]: Specify topic defaults so you don't need to specify them for every topic in the state file. Currently, only replication is supported. 
    - **blacklist** [Optional]: Add a prefixed topic blacklist for ignoring specific topics when using `kafka-gitops`. This allows topics to be ignored from being deleted if they are not defined in the desired state file.

**Example**:
```yaml
settings:
  confluent:
    enabled: true
  topics:
    defaults:
      replication: 3
    blacklist:
      prefixed:
        - _confluent
```

## Topics

**Synopsis**: Define the topics you would like on your cluster and their configuration.

?> Each topic is defined as a key-value pair, with the key being the topic name and the value being an object of settings.

**Example**:

```yaml
topics:
  my-topic-name:
    partitions: 6
    replication: 3
    configs:
      cleanup.policy: compact
      segment.bytes: 1000000
```

If a default `replication` value is supplied in the `settings` block, then the `replication` field can be omitted. If a default `replication` value is provided and the `replication` field in the topic definition is set, the default will be overridden for that topic.

## Services

**Synopsis**: Define the services that will utilize your Kafka cluster. These service definitions allow `kafka-gitops` to generate ACLs for you. Yay!

?> Each service has a `type`. This defines its structure.

There are currently three service types:

- `application`
- `kafka-connect`
- `kafka-streams`

!> **NOTE**: If using Confluent Cloud, omit the `principal` fields.

**Example application**:

?> **NOTE**: The `group-id` property is optional and defaults to the service name.

```yaml
services:
  my-application-name:
    type: application
    principal: User:my-application-principal
    group-id: optional-group-id-override
    produces:
      - topic-name-one
    consumes:
      - topic-name-two
      - topic-name-three
```

**Example kafka connect cluster**:

?> **NOTE**: The `group-id` property is optional and defaults to the service name. The `storage-topics` property is also optional; the defaults can be found on the [services][services] page.

```yaml
services:
  my-kafka-connect-cluster-name:
    type: kafka-connect
    principal: User:my-connect-principal
    group-id: optional-group-id-override
    storage-topics:
      config: optional-custom-config-topic
      offset: optional-custom-offset-topic
      status: optional-custom-status-topic
    connectors:
      my-source-connector-name:
        produces:
          - topic-name-one
      my-sink-connector-name:
        consumes:
          - topic-name-two
```

**Example kafka streams application**:

?> **NOTE**: The `application-id` property is optional and defaults to the service name.

```yaml
services:
  my-kafka-streams-name:
    type: kafka-streams
    principal: User:my-streams-principal
    application-id: optional-application-id-override
    produces:
      - topic-name-one
    consumes:
      - topic-name-two
```

Behind the scenes, `kafka-gitops` generates ACLs based on these definitions.

## Users

**Synopsis**: Define the users that will utilize your Kafka cluster. These user definitions allow `kafka-gitops` to generate ACLs for you. Yay!

?> **NOTE**: If using Confluent Cloud, users are service accounts that are prefixed with `user-`.

```yaml
users:
  my-user:
    principal: User:my-user
    roles:
      - writer
      - reader
      - operator
```

Currently, three predefined roles exist:

- **writer**: access to write to all topics
- **reader**: access to read all topics using any consumer group
- **operator**: access to view topics, topic configs, and to read topics and move their offsets

Outside of these very simple roles, you can define custom ACLs per-user by using the `customUserAcls` block.


## Custom Service ACLs

**Synopsis**: Define custom ACLs for a specific service. 

For example, if a specific application needs to produce to all topics prefixed with `kafka.` and `service.`, you may not want to define them all in your desired state file. 

If you have a service `my-test-service` defined, you can define custom ACLs as so:

```yaml
customServiceAcls:
  my-test-service:
    read-all-kafka:
      name: kafka.
      type: TOPIC
      pattern: PREFIXED
      host: "*"
      principal: User:my-test-service
      operation: READ
      permission: ALLOW
    read-all-service:
      name: service.
      type: TOPIC
      pattern: PREFIXED
      host: "*"
      principal: User:my-test-service
      operation: READ
      permission: ALLOW
```

## Custom User ACLs

**Synopsis**: Define custom ACLs for a specific user. 

For example, if a specific user needs to produce to all topics prefixed with `kafka.` and `service.`, you may not want to define them all in your desired state file. 

If you have a user `my-test-user` defined, you can define custom ACLs as so:

```yaml
customUserAcls:
  my-test-user:
    read-all-kafka:
      name: kafka.
      type: TOPIC
      pattern: PREFIXED
      host: "*"
      operation: READ
      permission: ALLOW
```

?> **NOTE**: The `principal` field can be left out here and it will be inherited from the user definition.

[services]: /services.md