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

    public static Optional<TopicDetailsPlan> toChangesOnlyPlan(Optional<TopicDetailsPlan> topicDetailsPlan) {
        if(! topicDetailsPlan.isPresent()) {
          return topicDetailsPlan;
        }
        TopicDetailsPlan.Builder builder = new TopicDetailsPlan.Builder();
        builder.setReplicationAction(topicDetailsPlan.get().getReplicationAction());
        builder.setPartitionsAction(topicDetailsPlan.get().getPartitionsAction());
        boolean nochanges = true;
        if ( topicDetailsPlan.get().getReplicationAction() != null && ! topicDetailsPlan.get().getReplicationAction().equals(PlanAction.NO_CHANGE)) {
            builder.setReplication(topicDetailsPlan.get().getReplication());
            builder.setPreviousReplication(topicDetailsPlan.get().getPreviousReplication());
            nochanges = false;
        }
        if (topicDetailsPlan.get().getPartitionsAction() != null && ! topicDetailsPlan.get().getPartitionsAction().equals(PlanAction.NO_CHANGE)) {
          builder.setPartitions(topicDetailsPlan.get().getPartitions());
            builder.setPreviousPartitions(topicDetailsPlan.get().getPreviousPartitions());
            nochanges = false;
        }
        if(nochanges) {
          return Optional.<TopicDetailsPlan>empty();
        }
        return Optional.of(builder.build());
    }
    class Builder extends TopicDetailsPlan_Builder {
    }
}
