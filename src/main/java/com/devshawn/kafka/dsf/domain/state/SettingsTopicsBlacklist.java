package com.devshawn.kafka.dsf.domain.state;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.inferred.freebuilder.FreeBuilder;

import java.util.List;

@FreeBuilder
@JsonDeserialize(builder = SettingsTopicsBlacklist.Builder.class)
public interface SettingsTopicsBlacklist {

    List<String> getPrefixed();

    class Builder extends SettingsTopicsBlacklist_Builder {
    }
}
