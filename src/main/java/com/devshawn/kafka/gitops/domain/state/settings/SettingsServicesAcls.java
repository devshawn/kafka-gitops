package com.devshawn.kafka.gitops.domain.state.settings;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.inferred.freebuilder.FreeBuilder;

import java.util.Optional;

@FreeBuilder
@JsonDeserialize(builder = SettingsServicesAcls.Builder.class)
public interface SettingsServicesAcls {

    Optional<Boolean> getDescribeTopicEnabled();

    class Builder extends SettingsServicesAcls_Builder {
    }
}
