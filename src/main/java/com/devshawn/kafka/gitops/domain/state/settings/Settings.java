package com.devshawn.kafka.gitops.domain.state.settings;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.inferred.freebuilder.FreeBuilder;

import java.util.Optional;

@FreeBuilder
@JsonDeserialize(builder = Settings.Builder.class)
public interface Settings {

    Optional<SettingsCCloud> getCcloud();

    Optional<SettingsTopics> getTopics();

    Optional<SettingsServices> getServices();

    Optional<SettingsFiles> getFiles();

    Optional<SettingsSchema> getSchema();

    class Builder extends Settings_Builder {
    }
}
