package com.devshawn.kafka.gitops.config;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.inferred.freebuilder.FreeBuilder;

import java.util.Map;

@FreeBuilder
@JsonDeserialize(builder = KafkaGitopsConfig.Builder.class)
public interface KafkaGitopsConfig {

    Map<String, Object> getConfig();

    class Builder extends KafkaGitopsConfig_Builder {

    }

}
