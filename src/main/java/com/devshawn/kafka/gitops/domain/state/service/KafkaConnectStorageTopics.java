package com.devshawn.kafka.gitops.domain.state.service;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.inferred.freebuilder.FreeBuilder;

import java.util.Optional;

@FreeBuilder
@JsonDeserialize(builder = KafkaConnectStorageTopics.Builder.class)
public interface KafkaConnectStorageTopics {

    Optional<String> getConfig();

    Optional<String> getOffset();

    Optional<String> getStatus();

    class Builder extends KafkaConnectStorageTopics_Builder {
    }
}
