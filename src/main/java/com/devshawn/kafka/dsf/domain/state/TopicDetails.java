package com.devshawn.kafka.dsf.domain.state;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.inferred.freebuilder.FreeBuilder;

import java.util.Map;

@FreeBuilder
@JsonDeserialize(builder = TopicDetails.Builder.class)
public interface TopicDetails {

    Integer getPartitions();

    Integer getReplication();

    Map<String, String> getConfigs();

    class Builder extends TopicDetails_Builder {
    }
}
