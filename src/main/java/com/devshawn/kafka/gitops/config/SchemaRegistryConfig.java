package com.devshawn.kafka.gitops.config;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.inferred.freebuilder.FreeBuilder;

import java.util.Map;

@FreeBuilder
@JsonDeserialize(builder = SchemaRegistryConfig.Builder.class)
public interface SchemaRegistryConfig {

    Map<String, Object> getConfig();

    class Builder extends SchemaRegistryConfig_Builder {

    }

}
