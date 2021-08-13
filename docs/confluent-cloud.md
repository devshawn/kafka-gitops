# Confluent Cloud

This tool was designed to work with Confluent Cloud. It can manage service accounts, topics, and ACLs for Confluent Cloud clusters.

## Getting Started

Ensure you have installed `kafka-gitops` or are using the `kafka-gitops` docker image as described in the [installation][installation] instructions.

You must have the `ccloud` command line tools installed if you wish to auto-populate the `principal` fields on services.

## Desired State File

Create a basic desired state file, `state.yaml`, such as:

```yaml
settings:
  ccloud:
    enabled: true

topics:
  test-topic:
    partitions: 6
    replication: 3

services:
  test-service:
    type: application
    produces:
      - test-topic
```

To give an overview, throughout this guide, this will create:

- A topic named `test-topic`
- A service account named `test-service`
- An `WRITE` ACL for topic `test-topic` tied to the service account `test-service`

## Configuration

To use `kafka-gitops` with Confluent Cloud, you'll need to set a few environment variables.

* `KAFKA_BOOTSTRAP_SERVERS`: Your Confluent Cloud cluster URL
* `KAFKA_SASL_JAAS_USERNAME`: Your Confluent Cloud API key
* `KAFKA_SASL_JAAS_PASSWORD`: Your Confluent Cloud API secret
* `KAFKA_SECURITY_PROTOCOL`: `SASL_SSL`
* `KAFKA_SASL_MECHANISM`: `PLAIN`
* `KAFKA_SSL_ENDPOINT_IDENTIFICATION_ALGORITHM`: `HTTPS`

Additionally, you'll need to login to the `ccloud` tool. You can automate this by setting the following environment variables:

* `XX_CCLOUD_EMAIL`: Your Confluent Cloud administrator email
* `XX_CCLOUD_PASSWORD`: Your Confluent Cloud administrator password

Then, you can run `ccloud login` and it will run without a prompt. This is great for CI builds.

You can optionally specify a path to a `ccloud` executable:

* `CCLOUD_EXECUTABLE_PATH`: `/full/path/to/ccloud`

Otherwise, `ccloud` must be on your path.

## Validate

First, validate your state file is correct by running:

```bash
kafka-gitops -f state.yaml validate
```

An example success message would look like:

```text
[VALID] Successfully validated the desired state file.
```

## Accounts

Before generating an execution plan, you will need to create the service accounts. This can be done by running:

```bash
kafka-gitops -f state.yaml account
```

This currently only creates service accounts; it will not delete any.

## Plan

We're now ready to generate a plan to execute against the cluster. By using the plan command, we are **NOT** changing the cluster.

Generate a plan by running:

```bash
kafka-gitops -f state.yaml plan -o plan.json
```

This will generate an execution plan to run against the cluster and save the output to `plan.json`. 

The command will also pretty-print what changes it wants to make to the cluster. 

## Apply

To execute a plan against the cluster, we use the apply command.

!> **WARNING**: This will apply changes to the cluster. This can be potentially destructive if you do not have all topics and ACLs defined.

By default, the apply command will generate a plan and then apply it. For most situations, **you should output a plan file from the plan command and pass it to the apply command**.

Example:

```bash
kafka-gitops -f state.yaml apply -p plan.json
```

Congrats! You're now using `kafka-gitops` to manage your Confluent Cloud cluster. Once you've practiced locally, you should commit your state file to a repository and create CI/CD pipelines to create plans and execute them against the cluster. 

Welcome to GitOps! 

[installation]: /installation.md
