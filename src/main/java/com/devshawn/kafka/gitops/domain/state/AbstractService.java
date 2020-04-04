package com.devshawn.kafka.gitops.domain.state;

import java.util.Optional;

public abstract class AbstractService {

    public AclDetails.Builder generateReadAcl(String topic, Optional<String> principal) {
        AclDetails.Builder builder = new AclDetails.Builder()
                .setHost("*")
                .setName(topic)
                .setOperation("READ")
                .setPermission("ALLOW")
                .setPattern("LITERAL")
                .setType("TOPIC");

        principal.ifPresent(builder::setPrincipal);
        return builder;
    }

    public AclDetails.Builder generateWriteACL(String topic, Optional<String> principal) {
        AclDetails.Builder builder = new AclDetails.Builder()
                .setHost("*")
                .setName(topic)
                .setOperation("WRITE")
                .setPermission("ALLOW")
                .setPattern("LITERAL")
                .setType("TOPIC");

        principal.ifPresent(builder::setPrincipal);
        return builder;
    }

    public AclDetails.Builder generateConsumerGroupAcl(String consumerGroupId, Optional<String> principal) {
        AclDetails.Builder builder = new AclDetails.Builder()
                .setHost("*")
                .setName(consumerGroupId)
                .setOperation("READ")
                .setPermission("ALLOW")
                .setPattern("LITERAL")
                .setType("GROUP");

        principal.ifPresent(builder::setPrincipal);
        return builder;
    }
}
