package com.devshawn.kafka.gitops.domain.plan;

import com.devshawn.kafka.gitops.enums.PlanAction;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.inferred.freebuilder.FreeBuilder;

import java.util.Optional;

@FreeBuilder
@JsonDeserialize(builder = TopicConfigPlan.Builder.class)
public interface TopicConfigPlan {

    String getKey();

    Optional<String> getValue();
    
    Optional<String> getPreviousValue();

    PlanAction getAction();

    class Builder extends TopicConfigPlan_Builder {
    }
}
