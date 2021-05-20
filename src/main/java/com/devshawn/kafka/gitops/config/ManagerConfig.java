package com.devshawn.kafka.gitops.config;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.inferred.freebuilder.FreeBuilder;

import java.io.File;
import java.util.Optional;

@FreeBuilder
@JsonDeserialize(builder = ManagerConfig.Builder.class)
public interface ManagerConfig {

    boolean isVerboseRequested();

    boolean isDeleteDisabled();

    boolean isIncludeUnchangedEnabled();

    boolean isSkipAclsDisabled();

    File getConfigFile();

    File getStateFile();

    Optional<File> getPlanFile();

    class Builder extends ManagerConfig_Builder {
    }
}
