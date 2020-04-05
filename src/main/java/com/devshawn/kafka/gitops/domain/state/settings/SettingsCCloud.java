package com.devshawn.kafka.gitops.domain.state.settings;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.inferred.freebuilder.FreeBuilder;

@FreeBuilder
@JsonDeserialize(builder = SettingsCCloud.Builder.class)
public interface SettingsCCloud {

    boolean isEnabled();

    class Builder extends SettingsCCloud_Builder {
    }
}
