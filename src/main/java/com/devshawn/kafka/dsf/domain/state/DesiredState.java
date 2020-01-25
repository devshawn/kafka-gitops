package com.devshawn.kafka.dsf.domain.state;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.inferred.freebuilder.FreeBuilder;

import java.util.Map;

@FreeBuilder
@JsonDeserialize(builder = DesiredState.Builder.class)
public interface DesiredState {

    Map<String, TopicDetails> getTopics();

    Map<String, AclDetails> getAcls();

    class Builder extends DesiredState_Builder {
    }
}
