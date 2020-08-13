# Permissions

When running against a secured Kafka cluster, `kafka-gitops` needs to be authorized to perform actions against the cluster. This can either be a super user defined by the Kafka cluster or a custom user with specific permissions.

## Example

For the purposes of this example, we'll assume we have a user principal named `gitops-user`. 

Full usage of kafka-gitops means you are managing topics, services, ACLs, and users. If you plan to make use of our ACL management features, the `gitops-user` principal must have the ability to create and manage ACLs.

If you do not want to use a super user, you can create a `gitops-user` principal and a current super user can make them an *ACL Administrator*. An ACL administrator has the `ALTER --cluster` access control entry. This entry allows the user to create and delete ACLs for the given cluster. 

!> **Caution**: An ACL administrator can then create ACLs for any other principal, including themselves.  

### Manually Add ACLs
Add the alter cluster ACL to the `gitops-user` principal:

```bash
kafka-acls --bootstrap-server localhost:9092 --command-config admin.properties \
--add --allow-principal User:gitops-user \
--operation  ALTER --cluster
```

Add the ACLs needed to manage topics to the `gitops-user` principal:

```bash
kafka-acls --bootstrap-server localhost:9092 --command-config admin.properties --add \
--allow-principal User:gitops-user --operation Create --operation Delete \
--operation DescribeConfigs --operation AlterConfigs --operation Alter \
--operation Describe --topic '*'
```

The above configs allow the `gitops-user` to manage ACLs, topics, and topic configurations.

### State File Definition
You can also create the ACLs using kafka-gitops. Run it once with super admin credentials using the state file below, and then switch to using your `gitops-user` credentials.

```yaml
users:
  gitops-user:
    principal: User:gitops-user

customUserAcls:
  gitops-user:
    alter-cluster:
      name: kafka-cluster
      type: CLUSTER
      pattern: LITERAL
      host: "*"
      operation: ALTER
      permission: ALLOW
    create-topics:
      name: "*"
      type: TOPIC
      pattern: LITERAL
      host: "*"
      operation: CREATE
      permission: ALLOW
    alter-topics:
      name: "*"
      type: TOPIC
      pattern: LITERAL
      host: "*"
      operation: ALTER
      permission: ALLOW
    describe-topics:
      name: "*"
      type: TOPIC
      pattern: LITERAL
      host: "*"
      operation: DESCRIBE
      permission: ALLOW
    delete-topics:
      name: "*"
      type: TOPIC
      pattern: LITERAL
      host: "*"
      operation: DELETE
      permission: ALLOW
    describe-topic-configs:
      name: "*"
      type: TOPIC
      pattern: LITERAL
      host: "*"
      operation: DESCRIBE_CONFIGS
      permission: ALLOW
    alter-topic-configs:
      name: "*"
      type: TOPIC
      pattern: LITERAL
      host: "*"
      operation: ALTER_CONFIGS
      permission: ALLOW
```

