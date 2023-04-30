package com.devshawn.kafka.gitops.domain.state.settings;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.inferred.freebuilder.FreeBuilder;

import java.util.List;

@FreeBuilder
@JsonDeserialize(builder = SettingsSchemasBlacklist.Builder.class)
public interface SettingsSchemasBlacklist {

    List<String> getPrefixed();

    class Builder extends SettingsSchemasBlacklist_Builder {
    }
}
