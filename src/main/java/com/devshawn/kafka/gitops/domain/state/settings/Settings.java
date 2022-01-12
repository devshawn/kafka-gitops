package com.devshawn.kafka.gitops.domain.state.settings;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.inferred.freebuilder.FreeBuilder;

import java.util.Optional;

@FreeBuilder
@JsonDeserialize(builder = Settings.Builder.class)
public interface Settings {

    Optional<SettingsConfluent> getConfluent();

    Optional<SettingsTopics> getTopics();

    Optional<SettingsServices> getServices();

    Optional<SettingsFiles> getFiles();

    class Builder extends Settings_Builder {
    }
}
