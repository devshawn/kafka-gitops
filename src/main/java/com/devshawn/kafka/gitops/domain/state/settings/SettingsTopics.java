package com.devshawn.kafka.gitops.domain.state.settings;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.inferred.freebuilder.FreeBuilder;

import java.util.Optional;

@FreeBuilder
@JsonDeserialize(builder = SettingsTopics.Builder.class)
public interface SettingsTopics {

    Optional<SettingsTopicsBlacklist> getBlacklist();

    class Builder extends SettingsTopics_Builder {
    }
}
