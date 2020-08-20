package com.devshawn.kafka.gitops.domain.state.settings;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.inferred.freebuilder.FreeBuilder;

import java.util.Optional;

@FreeBuilder
@JsonDeserialize(builder = SettingsServices.Builder.class)
public interface SettingsServices {

    Optional<SettingsServicesAcls> getAcls();

    class Builder extends SettingsServices_Builder {
    }
}
