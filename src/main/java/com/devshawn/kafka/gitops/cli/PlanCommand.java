package com.devshawn.kafka.gitops.cli;

import com.devshawn.kafka.gitops.MainCommand;
import com.devshawn.kafka.gitops.StateManager;
import com.devshawn.kafka.gitops.config.ManagerConfig;
import com.devshawn.kafka.gitops.domain.plan.DesiredPlan;
import com.devshawn.kafka.gitops.exception.KafkaExecutionException;
import com.devshawn.kafka.gitops.exception.MissingConfigurationException;
import com.devshawn.kafka.gitops.exception.PlanIsUpToDateException;
import com.devshawn.kafka.gitops.exception.ValidationException;
import com.devshawn.kafka.gitops.exception.WritePlanOutputException;
import com.devshawn.kafka.gitops.service.ParserService;
import com.devshawn.kafka.gitops.util.LogUtil;
import picocli.CommandLine;

import java.io.File;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "plan", description = "Generate an execution plan of changes to Kafka resources.")
public class PlanCommand implements Callable<Integer> {

    @CommandLine.Option(names = {"-o", "--output"}, paramLabel = "<file>",
            description = "Specify the output file for the plan.")
    private File outputFile;

    @CommandLine.Option(names = {"--include-unchanged"}, description = "Include unchanged resources in the plan file.")
    private boolean includeUnchanged = false;

    @CommandLine.ParentCommand
    private MainCommand parent;

    @Override
    public Integer call() {
        try {
            System.out.println("Generating execution plan...\n");
            ParserService parserService = new ParserService(parent.getStateFile());
            StateManager stateManager = new StateManager(generateStateManagerConfig(), parserService);
            DesiredPlan desiredPlan = stateManager.plan();
            LogUtil.printPlan(desiredPlan, parent.isDeleteDisabled(), parent.areAclsDisabled());
            return 0;
        } catch (PlanIsUpToDateException ex) {
            LogUtil.printNoChangesMessage();
            return 0;
        } catch (MissingConfigurationException ex) {
            LogUtil.printGenericError(ex);
        } catch (ValidationException ex) {
            LogUtil.printValidationResult(ex.getMessage(), false);
        } catch (KafkaExecutionException ex) {
            LogUtil.printKafkaExecutionError(ex);
        } catch (WritePlanOutputException ex) {
            LogUtil.printPlanOutputError(ex);
        }
        return 2;
    }

    private ManagerConfig generateStateManagerConfig() {
        return new ManagerConfig.Builder()
                .setVerboseRequested(parent.isVerboseRequested())
                .setDeleteDisabled(parent.isDeleteDisabled())
                .setIncludeUnchangedEnabled(includeUnchanged)
                .setConfigFile(parent.getConfigFile())
                .setStateFile(parent.getStateFile())
                .setSkipAclsDisabled(parent.areAclsDisabled())
                .setNullablePlanFile(outputFile)
                .build();
    }
}
