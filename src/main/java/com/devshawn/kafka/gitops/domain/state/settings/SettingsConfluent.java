package com.devshawn.kafka.gitops.domain.state.settings;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.inferred.freebuilder.FreeBuilder;

@FreeBuilder
@JsonDeserialize(builder = SettingsConfluent.Builder.class)
public interface SettingsConfluent {

    boolean isEnabled();

    class Builder extends SettingsConfluent_Builder {
    }
}
