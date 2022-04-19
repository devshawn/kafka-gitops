package com.devshawn.kafka.gitops.domain.state.settings;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.inferred.freebuilder.FreeBuilder;

import java.util.Optional;

@FreeBuilder
@JsonDeserialize(builder = SettingsSchema.Builder.class)
public interface SettingsSchema {

    Optional<SettingsRegistry> getRegistry();

    Optional<SettingsDirectory> getDirectory();

    Optional<SettingsSchemasDefaults> getDefaults();

    class Builder extends SettingsSchema_Builder {
    }
}
