package com.devshawn.kafka.gitops.domain.state.settings;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.inferred.freebuilder.FreeBuilder;

import java.net.URL;
import java.util.Optional;

@FreeBuilder
@JsonDeserialize(builder = SettingsRegistry.Builder.class)
public interface SettingsRegistry {

    Optional<URL> getUrl();

    class Builder extends SettingsRegistry_Builder {
    }
}
