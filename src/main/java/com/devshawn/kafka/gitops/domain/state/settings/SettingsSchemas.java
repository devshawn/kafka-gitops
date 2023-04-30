package com.devshawn.kafka.gitops.domain.state.settings;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.inferred.freebuilder.FreeBuilder;

import java.util.Optional;

@FreeBuilder
@JsonDeserialize(builder = SettingsSchemas.Builder.class)
public interface SettingsSchemas {

    Optional<SettingsRegistry> getRegistry();

    Optional<SettingsDirectory> getDirectory();

    Optional<SettingsSchemasBlacklist> getBlacklist();

    Optional<SettingsSchemasDefaults> getDefaults();

    class Builder extends SettingsSchemas_Builder {
    }
}
