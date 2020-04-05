<div align="center">
    <h1>GitOps for Apache Kafka</h1>
    <img src="https://i.imgur.com/eAIAv0w.png"/>
    <span>Manage Apache Kafka topics, services, and ACLs through a desired state file.</span>
</div>

## Overview

Kafka GitOps is an Apache Kafka resources-as-code tool which allows you to automate the management of your Apache Kafka topics and ACLs from version controlled code. It allows you to define topics and services through the use of a desired state file, much like Terraform and other infrastructure-as-code tools.

Topics and services get defined in a YAML file. When run, `kafka-gitops` compares your desired state to the actual state of the cluster and generates a plan to execute against the cluster. This will make your topics and ACLs match your desired state.

This tool also generates the needed ACLs for each type of application. There is no need to manually create a bunch of ACLs for Kafka Connect, Kafka Streams, etc. By defining your services, `kafka-gitops` will build the necessary ACLs.

## Features

- 🚀  **Built For CI/CD**: Made for CI/CD pipelines to automate the management of topics & ACLs.
- 🔥  **Configuration as code**: Describe your desired state and manage it from a version-controlled declarative file.
- 👍  **Easy to use**: Deep knowledge of Kafka administration or ACL management is **NOT** required. 
- ⚡️️  **Plan & Apply**: Generate and view a plan with or without executing it against your cluster.
- 💻  **Portable**: Works across self-hosted clusters, managed clusters, and even Confluent Cloud clusters.
- 🦄  **Idempotency**: Executing the same desired state file on an up-to-date cluster will yield the same result.
- ☀️  **Continue from failures**: If a specific step fails during an apply, you can fix your desired state and re-run the command. You can execute `kafka-gitops` again without needing to rollback any partial successes.

## Getting Started

Check out the **[Quick Start](/quick-start.md)** documentation to install `kafka-gitops` and get started.

## Contributing

Contributions are very welcome. See [CONTRIBUTING.md][contributing] for details.

## License

Copyright (c) 2020 Shawn Seymour.

Licensed under the [Apache 2.0 license][license].

[contributing]: https://github.com/devshawn/kafka-gitops/blob/master/CONTRIBUTING.md
[license]: https://github.com/devshawn/kafka-gitops/blob/master/LICENSE
