package com.devshawn.kafka.dsf.config;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.inferred.freebuilder.FreeBuilder;

import java.util.Map;

@FreeBuilder
@JsonDeserialize(builder = KafkaDsfConfig.Builder.class)
public abstract class KafkaDsfConfig {

    public abstract Map<String, Object> getConfig();

    public static class Builder extends KafkaDsfConfig_Builder {

    }

}
