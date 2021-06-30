# kafka-gitops

![Java CI](https://github.com/devshawn/kafka-gitops/workflows/Java%20CI/badge.svg) [![Maintainability](https://api.codeclimate.com/v1/badges/373371aac3f69c292031/maintainability)](https://codeclimate.com/github/devshawn/kafka-gitops/maintainability) [![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

Manage Apache Kafka topics and ACLs through a desired state file.

<p align="center">
    <img src="https://i.imgur.com/jnDwYp8.png"/>
</p>

## Overview

Kafka GitOps is an Apache Kafka resources-as-code tool which allows you to automate the management of your Apache Kafka topics and ACLs from version controlled code. It allows you to define topics and services through the use of a desired state file, much like Terraform and other infrastructure-as-code tools.

Topics and services get defined in a YAML file. When run, `kafka-gitops` compares your desired state to the actual state of the cluster and generates a plan to execute against the cluster. This will make your topics and ACLs match your desired state.

This tool also generates the needed ACLs for each type of application. There is no need to manually create a bunch of ACLs for Kafka Connect, Kafka Streams, etc. By defining your services, `kafka-gitops` will build the necessary ACLs.

This tool supports self-hosted Kafka, managed Kafka, and Confluent Cloud clusters.

## Features

- 🚀  **Built For CI/CD**: Made for CI/CD pipelines to automate the management of topics & ACLs.
- 🔥  **Configuration as code**: Describe your desired state and manage it from a version-controlled declarative file.
- 👍  **Easy to use**: Deep knowledge of Kafka administration or ACL management is **NOT** required. 
- ⚡️️  **Plan & Apply**: Generate and view a plan with or without executing it against your cluster.
- 💻  **Portable**: Works across self-hosted clusters, managed clusters, and even Confluent Cloud clusters.
- 🦄  **Idempotency**: Executing the same desired state file on an up-to-date cluster will yield the same result.
- ☀️  **Continue from failures**: If a specific step fails during an apply, you can fix your desired state and re-run the command. You can execute `kafka-gitops` again without needing to rollback any partial successes.

## Getting Started

Documentation on how to install and use this tool can be found on our [documentation site][documentation].

## Usage

Run `kafka-gitops` to view the help output.

```bash
Usage: kafka-gitops [-hvV] [--no-delete] [--skip-acls] [-f=<file>] [COMMAND]
Manage Kafka resources with a desired state file.
  -f, --file=<file>   Specify the desired state file.
  -h, --help          Display this help message.
      --no-delete     Disable the ability to delete resources.
      --skip-acls     Do not take ACLs into account during plans or applies.
  -v, --verbose       Show more detail during execution.
  -V, --version       Print the current version of this tool.
Commands:
  account   Create Confluent Cloud service accounts.
  apply     Apply changes to Kafka resources.
  plan      Generate an execution plan of changes to Kafka resources.
  validate  Validates the desired state file.
```

## Configuration

Currently, configuring bootstrap servers and other properties is done via environment variables:

To configure properties, prefix them with `KAFKA_`. For example:

* `KAFKA_BOOTSTRAP_SERVERS`: Injects as `bootstrap.servers`
* `KAFKA_CLIENT_ID`: Injects as `client.id`

Additionally, we provide helpers for setting the `sasl.jaas.config` for clusters such as Confluent Cloud.

By setting:

* `KAFKA_SASL_JAAS_USERNAME`: Username to use
* `KAFKA_SASL_JAAS_PASSWORD`: Password to use

The following configuration is generated:

* `sasl.jaas.config`: `org.apache.kafka.common.security.plain.PlainLoginModule required username="USERNAME" password="PASSWORD";`

## State File

By default, `kafka-gitops` looks for `state.yaml` in the current directory. You can also use `kafka-gitops -f` to pass a file.

An example desired state file:

```yaml
topics:
  example-topic:
    partitions: 6
    replication: 3
    configs:
      cleanup.policy: compact

services:
  example-service:
    type: application
    produces:
      - example-topic
    consumes:
      - example-topic
```

## Contributing

Contributions are very welcome. See [CONTRIBUTING.md][contributing] for details.

## License

Copyright (c) 2020 Shawn Seymour.

Licensed under the [Apache 2.0 license][license].

[documentation]: https://devshawn.github.io/kafka-gitops
[contributing]: ./CONTRIBUTING.md
[license]: ./LICENSE
