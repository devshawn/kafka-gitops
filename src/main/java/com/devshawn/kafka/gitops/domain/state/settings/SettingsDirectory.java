package com.devshawn.kafka.gitops.domain.state.settings;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.inferred.freebuilder.FreeBuilder;

import java.nio.file.Path;
import java.util.Optional;

@FreeBuilder
@JsonDeserialize(builder = SettingsDirectory.Builder.class)
public interface SettingsDirectory {

    Optional<Path> getPath();

    class Builder extends SettingsDirectory_Builder {
    }
}
