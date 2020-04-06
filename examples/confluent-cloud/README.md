# Confluent Cloud Example

This is a basic example of using `kafka-gitops` with Confluent Cloud.

## Files
- `services.yaml` defines service accounts + generates ACLs
- `topics.yaml` defines topics and their configs
- `users.yaml` defines "user" service accounts and roles (prefixed with `user-{name}`)

## Configuration

The following environment variables need to be set locally or in the build job:

```bash
# For service account creation / listing
export XX_CCLOUD_EMAIL="Your Confluent Cloud email address"
export XX_CCLOUD_PASSWORD="Your Confluent Cloud password"

# For executing against the cluster
export KAFKA_BOOTSTRAP_SERVERS="Your Confluent Cloud cluster URL"
export KAFKA_SASL_JAAS_USERNAME"Your Confluent Cloud API key"
export KAFKA_SASL_JAAS_PASSWORD="Your Confluent Cloud API secret"
export KAFKA_SECURITY_PROTOCOL="SASL_SSL"
export KAFKA_SASL_MECHANISM="PLAIN"
export KAFKA_SSL_ENDPOINT_IDENTIFICATION_ALGORITHM="HTTPS"

# For colored output
export CLICOLOR_FORCE="true"
```

## Service Accounts

Once defining services and users, you can generate service accounts. 

**NOTE**: Before running `accounts` or `plan`, ensure you are logged in to Confluent Cloud. Use `ccloud login`.

Create accounts:

```bash
kafka-gitops -f state.yaml accounts
```

## Plan

Generate an execution plan against your Confluent Cloud cluster as so:

```bash
kafka-gitops -f state.yaml plan -o plan.json
```

To disable deletes, you can use:

```bash
kafka-gitops -f state.yaml --no-delete plan -o plan.json
```

This will output a plan file as well as pretty print the plan to standard out.

## Apply

Apply the plan against your Confluent Cloud cluster as so:

```bash
kafka-gitops -f state.yaml apply -p plan.json
```


To disable deletes, you can use:

```bash
kafka-gitops -f state.yaml --no-delete apply -p plan.json
```