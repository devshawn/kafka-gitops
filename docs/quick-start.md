# Quick Start

Getting started with `kafka-gitops` is simple. For this tutorial, we will assume:

- You have installed the `kafka-gitops` command as [described here](/installation.md).
- You have a kafka cluster running on `localhost:9092`. 

!> **NOTE**: If you desire to use this with Confluent Cloud, read our [Confluent Cloud page][ccloud].

## Desired State File

First, create a desired state file named `state.yaml`. In this file, we'll define a topic. To showcase how to set topic configs, we'll make it a compacted topic.

```yaml
topics:
  my-example-topic:
    partitions: 6
    replication: 1
    configs:
      cleanup.policy: compact
```

## Configuration

Currently, configuring `kafka-gitops` is done via environment variables. To configure properties, prefix them with `KAFKA_`. For example:

* `KAFKA_BOOTSTRAP_SERVERS`: Injects as `bootstrap.servers`
* `KAFKA_CLIENT_ID`: Injects as `client.id`

For our quick start example, open a terminal where your `state.yaml` file is located and set the bootstrap servers:

```bash
export KAFKA_BOOTSTRAP_SERVERS=localhost:9092
```

!> **NOTE**: If running `kafka-gitops` against a secured Kafka cluster, it must be run with super user credentials or a user with special ACLs. [Read more on our permissions page][permissions].

## Validate
We can validate the desired state file conforms to the [specification][specification]. To do this, run:

```bash
kafka-gitops validate
```

By default, it will look for `state.yaml` in the current directory. To specify a different file name, run:

```bash
kafka-gitops -f my-state-file.yaml validate
```

An example success message would look like:

```text
[VALID] Successfully validated the desired state file.
```

## Plan

We're now ready to generate a plan to execute against the cluster. By using the plan command, we are **NOT** changing the cluster.

Generate a plan by running:

```bash
kafka-gitops plan
```

This will generate an execution plan that looks like:


```text
Generating execution plan...

An execution plan has been generated and is shown below.
Resource actions are indicated with the following symbols:
  + create

The following actions will be performed:

Topics: 1 to create, 0 to update, 0 to delete.

+ [TOPIC] my-example-topic
	+ cleanup.policy: compact


ACLs: 0 to create, 0 to update, 0 to delete.

Plan: 1 to create, 0 to update, 0 to delete.
```

In most cases, you will want to output the plan to a file which can then be passed to the apply command. Plan files are `JSON` files. This can be done by running:

```bash
kafka-gitops plan -o plan.json
```

If running against a Kafka cluster with no authorizer configured or if you simply want to only manage topics, you can ignore ACLs completely. This can be done by running:

```bash
kafka-gitops --skip-acls plan
```

## Apply

To execute a plan against the cluster, we use the apply command.

!> **WARNING**: This will apply changes to the cluster. This can be potentially destructive if you do not have all topics and ACLs defined.

Run:

```bash
kafka-gitops apply
```

By default, the apply command will generate a plan and then apply it. For most situations, **you should output a plan file from the plan command and pass it to the apply command**.

Example:

```bash
kafka-gitops apply -p plan.json
```

An output of a successful apply would look like:

```text
Executing apply...

Applying: [CREATE]

+ [TOPIC] my-example-topic
	+ cleanup.policy: compact

Successfully applied.

[SUCCESS] Apply complete! Resources: 1 created, 0 updated, 0 deleted.
```

If there is a partial failure, successes will not be rolled back. A failure error would look something like:

```text
[ERROR] Error thrown when attempting to create a Kafka topic:
org.apache.kafka.common.errors.PolicyViolationException: Topic replication factor must be 3

[ERROR] An error has occurred during the apply process.
[ERROR] The apply process has stopped in place. There is no rollback.
[ERROR] Fix the error, re-create a plan, and apply the new plan to continue.
```

Congrats! You've successfully started using GitOps strategies to manage your cluster. If you have security on your cluster, read the [services][services] page to start defining services. 

[ccloud]: /confluent-cloud.md
[permissions]: /permissions.md
[specification]: /specification.md
[services]: /services.md