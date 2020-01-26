package com.devshawn.kafka.dsf.domain.state;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.inferred.freebuilder.FreeBuilder;

import java.util.Optional;

@FreeBuilder
@JsonDeserialize(builder = Settings.Builder.class)
public interface Settings {

    Optional<SettingsTopics> getTopics();

    class Builder extends Settings_Builder {
    }
}
