package com.devshawn.kafka.gitops.domain.state.settings;

import java.util.Optional;
import org.inferred.freebuilder.FreeBuilder;
import com.devshawn.kafka.gitops.enums.SchemaCompatibility;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@FreeBuilder
@JsonDeserialize(builder = SettingsSchemasDefaults.Builder.class)
public interface SettingsSchemasDefaults {

    Optional<SchemaCompatibility> getCompatibility();

    class Builder extends SettingsSchemasDefaults_Builder {
    }
}
