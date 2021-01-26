package com.devshawn.kafka.gitops.domain.plan;

import com.devshawn.kafka.gitops.enums.PlanAction;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.inferred.freebuilder.FreeBuilder;

import java.util.List;
import java.util.Optional;

@FreeBuilder
@JsonDeserialize(builder = TopicPlan.Builder.class)
public interface TopicPlan {

    String getName();

    PlanAction getAction();

    Optional<TopicDetailsPlan> getTopicDetailsPlan();

    List<TopicConfigPlan> getTopicConfigPlans();

    default TopicPlan toChangesOnlyPlan() {
        TopicPlan.Builder builder = new TopicPlan.Builder().setName(getName()).setAction(getAction());
        
        builder.setTopicDetailsPlan(TopicDetailsPlan.toChangesOnlyPlan(getTopicDetailsPlan()));

        getTopicConfigPlans().stream().filter(it -> !it.getAction().equals(PlanAction.NO_CHANGE)).forEach(builder::addTopicConfigPlans);
        return builder.build();
    }

    class Builder extends TopicPlan_Builder {
    }
}
