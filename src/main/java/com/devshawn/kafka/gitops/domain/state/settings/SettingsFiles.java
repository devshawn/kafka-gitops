package com.devshawn.kafka.gitops.domain.state.settings;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.inferred.freebuilder.FreeBuilder;

import java.util.Optional;

@FreeBuilder
@JsonDeserialize(builder = SettingsFiles.Builder.class)
public interface SettingsFiles {

    Optional<String> getServices();

    Optional<String> getTopics();

    Optional<String> getUsers();
    
    Optional<String> getSchemas();

    class Builder extends SettingsFiles_Builder {
    }
}
