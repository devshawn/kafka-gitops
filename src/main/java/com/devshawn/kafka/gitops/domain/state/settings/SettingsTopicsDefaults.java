package com.devshawn.kafka.gitops.domain.state.settings;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.inferred.freebuilder.FreeBuilder;

import java.util.Optional;

@FreeBuilder
@JsonDeserialize(builder = SettingsTopicsDefaults.Builder.class)
public interface SettingsTopicsDefaults {

    Optional<Integer> getReplication();

    class Builder extends SettingsTopicsDefaults_Builder {
    }
}
