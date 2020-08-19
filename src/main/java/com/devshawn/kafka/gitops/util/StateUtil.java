package com.devshawn.kafka.gitops.util;

import com.devshawn.kafka.gitops.domain.state.DesiredStateFile;

import java.util.Optional;

public class StateUtil {

    public static Optional<Integer> fetchReplication(DesiredStateFile desiredStateFile) {
        if (desiredStateFile.getSettings().isPresent() && desiredStateFile.getSettings().get().getTopics().isPresent()
                && desiredStateFile.getSettings().get().getTopics().get().getDefaults().isPresent()) {
            return desiredStateFile.getSettings().get().getTopics().get().getDefaults().get().getReplication();
        }
        return Optional.empty();
    }
}
