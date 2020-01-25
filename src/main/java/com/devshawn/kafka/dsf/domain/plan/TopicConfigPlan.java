package com.devshawn.kafka.dsf.domain.plan;

import com.devshawn.kafka.dsf.enums.PlanAction;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.inferred.freebuilder.FreeBuilder;

import java.util.Optional;

@FreeBuilder
@JsonDeserialize(builder = TopicConfigPlan.Builder.class)
public interface TopicConfigPlan {

    String getKey();

    Optional<String> getValue();

    PlanAction getAction();

    class Builder extends TopicConfigPlan_Builder {
    }
}
