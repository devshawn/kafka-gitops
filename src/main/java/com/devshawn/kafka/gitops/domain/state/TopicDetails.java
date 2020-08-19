package com.devshawn.kafka.gitops.domain.state;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.inferred.freebuilder.FreeBuilder;

import java.util.Map;
import java.util.Optional;

@FreeBuilder
@JsonDeserialize(builder = TopicDetails.Builder.class)
public interface TopicDetails {

    Integer getPartitions();

    Optional<Integer> getReplication();

    Map<String, String> getConfigs();

    class Builder extends TopicDetails_Builder {
    }
}
