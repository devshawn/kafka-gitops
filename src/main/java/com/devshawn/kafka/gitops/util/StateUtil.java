package com.devshawn.kafka.gitops.util;

import com.devshawn.kafka.gitops.domain.state.DesiredStateFile;
import com.devshawn.kafka.gitops.enums.SchemaCompatibility;
import java.util.Optional;

public class StateUtil {

    public static Optional<Integer> fetchDefaultTopicsReplication(DesiredStateFile desiredStateFile) {
        if (desiredStateFile.getSettings().isPresent() && desiredStateFile.getSettings().get().getTopics().isPresent()
                && desiredStateFile.getSettings().get().getTopics().get().getDefaults().isPresent()) {
            return desiredStateFile.getSettings().get().getTopics().get().getDefaults().get().getReplication();
        }
        return Optional.empty();
    }

    public static boolean isDescribeTopicAclEnabled(DesiredStateFile desiredStateFile) {
        return desiredStateFile.getSettings().isPresent() && desiredStateFile.getSettings().get().getServices().isPresent()
                && desiredStateFile.getSettings().get().getServices().get().getAcls().isPresent()
                && desiredStateFile.getSettings().get().getServices().get().getAcls().get().getDescribeTopicEnabled().isPresent()
                && desiredStateFile.getSettings().get().getServices().get().getAcls().get().getDescribeTopicEnabled().get();
    }

    public static Optional<SchemaCompatibility> fetchDefaultSchemasCompatibility(DesiredStateFile desiredStateFile) {
      if (desiredStateFile.getSettings().isPresent() && desiredStateFile.getSettings().get().getSchema().isPresent()
              && desiredStateFile.getSettings().get().getSchema().get().getDefaults().isPresent()) {
          return desiredStateFile.getSettings().get().getSchema().get().getDefaults().get().getCompatibility();
      }
      return Optional.empty();
  }
}
