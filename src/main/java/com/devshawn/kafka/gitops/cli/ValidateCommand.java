package com.devshawn.kafka.gitops.cli;

import com.devshawn.kafka.gitops.MainCommand;
import com.devshawn.kafka.gitops.StateManager;
import com.devshawn.kafka.gitops.config.ManagerConfig;
import com.devshawn.kafka.gitops.exception.ValidationException;
import com.devshawn.kafka.gitops.service.ParserService;
import com.devshawn.kafka.gitops.util.LogUtil;
import picocli.CommandLine;
import picocli.CommandLine.Command;

import java.util.concurrent.Callable;


@Command(name = "validate", description = "Validates the desired state file.")
public class ValidateCommand implements Callable<Integer> {

    @CommandLine.ParentCommand
    private MainCommand parent;

    @Override
    public Integer call() {
        try {
            ParserService parserService = new ParserService(parent.getStateFile());
            StateManager stateManager = new StateManager(generateStateManagerConfig(), parserService);
            stateManager.getAndValidateStateFile();
            LogUtil.printValidationResult("Successfully validated the desired state file.", true);
            return 0;
        } catch (ValidationException ex) {
            LogUtil.printValidationResult(ex.getMessage(), false);
        }
        return 2;
    }

    private ManagerConfig generateStateManagerConfig() {
        return new ManagerConfig.Builder()
                .setVerboseRequested(parent.isVerboseRequested())
                .setDeleteDisabled(parent.isDeleteDisabled())
                .setIncludeUnchangedEnabled(false)
                .setSkipAclsDisabled(parent.areAclsDisabled())
                .setConfigFile(parent.getConfigFile())
                .setStateFile(parent.getStateFile())
                .build();
    }
}
