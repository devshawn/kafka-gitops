package com.devshawn.kafka.gitops.domain.plan;

import com.devshawn.kafka.gitops.domain.state.TopicDetails;
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

    Optional<TopicDetails> getTopicDetails();

    List<TopicConfigPlan> getTopicConfigPlans();

    default TopicPlan toChangesOnlyPlan() {
        TopicPlan.Builder builder = new TopicPlan.Builder().setName(getName()).setAction(getAction()).setTopicDetails(getTopicDetails());
        getTopicConfigPlans().stream().filter(it -> !it.getAction().equals(PlanAction.NO_CHANGE)).forEach(builder::addTopicConfigPlans);
        return builder.build();
    }

    class Builder extends TopicPlan_Builder {
    }
}
