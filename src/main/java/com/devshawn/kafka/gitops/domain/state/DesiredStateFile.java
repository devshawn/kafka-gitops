package com.devshawn.kafka.gitops.domain.state;


import com.devshawn.kafka.gitops.domain.state.settings.Settings;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.inferred.freebuilder.FreeBuilder;

import java.util.Map;
import java.util.Optional;

@FreeBuilder
@JsonDeserialize(builder = DesiredStateFile.Builder.class)
public interface DesiredStateFile {

    Optional<Settings> getSettings();

    Map<String, ServiceDetails> getServices();

    Map<String, TopicDetails> getTopics();

    Map<String, SchemaDetails> getSchemas();

    Map<String, UserDetails> getUsers();

    Map<String, Map<String, CustomAclDetails>> getCustomServiceAcls();

    Map<String, Map<String, CustomAclDetails>> getCustomUserAcls();

    class Builder extends DesiredStateFile_Builder {
    }
}
