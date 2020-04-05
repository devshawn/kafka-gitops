package com.devshawn.kafka.gitops.cli;

import com.devshawn.kafka.gitops.MainCommand;
import com.devshawn.kafka.gitops.StateManager;
import com.devshawn.kafka.gitops.config.ManagerConfig;
import com.devshawn.kafka.gitops.exception.*;
import com.devshawn.kafka.gitops.service.ParserService;
import com.devshawn.kafka.gitops.util.LogUtil;
import picocli.CommandLine;

import java.util.concurrent.Callable;

@CommandLine.Command(name = "account", description = "Create Confluent Cloud service accounts.")
public class AccountCommand implements Callable<Integer> {

    @CommandLine.ParentCommand
    private MainCommand parent;

    @Override
    public Integer call() {
        try {
            System.out.println("Creating service accounts...\n");
            ParserService parserService = new ParserService(parent.getFile());
            StateManager stateManager = new StateManager(generateStateManagerConfig(), parserService);
            stateManager.createServiceAccounts();
            return 0;
        } catch (MissingConfigurationException | ConfluentCloudException ex) {
            LogUtil.printSimpleError(ex.getMessage());
            return 2;
        } catch (ValidationException ex) {
            LogUtil.printValidationResult(ex.getMessage(), false);
            return 2;
        } catch (KafkaExecutionException ex) {
            LogUtil.printKafkaExecutionError(ex);
            return 2;
        } catch (WritePlanOutputException ex) {
            LogUtil.printPlanOutputError(ex);
            return 2;
        }
    }

    private ManagerConfig generateStateManagerConfig() {
        return new ManagerConfig.Builder()
                .setVerboseRequested(parent.isVerboseRequested())
                .setDeleteDisabled(parent.isDeleteDisabled())
                .setStateFile(parent.getFile())
                .build();
    }
}
