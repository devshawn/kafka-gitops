package com.devshawn.kafka.gitops.domain.plan;

import java.util.Optional;
import org.inferred.freebuilder.FreeBuilder;
import com.devshawn.kafka.gitops.enums.PlanAction;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@FreeBuilder
@JsonDeserialize(builder = TopicDetailsPlan.Builder.class)
public interface TopicDetailsPlan {
    Optional<Integer> getPartitions();
    Optional<Integer> getPreviousPartitions();
    PlanAction getPartitionsAction();

    Optional<Integer> getReplication();
    Optional<Integer> getPreviousReplication();
    PlanAction getReplicationAction();

    public static TopicDetailsPlan toChangesOnlyPlan(TopicDetailsPlan topicDetailsPlan) {
        TopicDetailsPlan.Builder builder = new TopicDetailsPlan.Builder();
        builder.setReplicationAction(topicDetailsPlan.getReplicationAction());
        builder.setPartitionsAction(topicDetailsPlan.getPartitionsAction());
        if ( topicDetailsPlan.getReplicationAction() != null && ! topicDetailsPlan.getReplicationAction().equals(PlanAction.NO_CHANGE)) {
            builder.setReplication(topicDetailsPlan.getReplication());
            builder.setPreviousReplication(topicDetailsPlan.getPreviousReplication());
        }
        if (topicDetailsPlan.getPartitionsAction() != null && ! topicDetailsPlan.getPartitionsAction().equals(PlanAction.NO_CHANGE)) {
          builder.setPartitions(topicDetailsPlan.getPartitions());
            builder.setPreviousPartitions(topicDetailsPlan.getPreviousPartitions());
        }
        return builder.build();
    }
    class Builder extends TopicDetailsPlan_Builder {
    }
}
