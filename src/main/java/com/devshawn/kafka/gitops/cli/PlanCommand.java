package com.devshawn.kafka.gitops.cli;

import com.devshawn.kafka.gitops.MainCommand;
import com.devshawn.kafka.gitops.StateManager;
import com.devshawn.kafka.gitops.config.ManagerConfig;
import com.devshawn.kafka.gitops.domain.plan.DesiredPlan;
import com.devshawn.kafka.gitops.exception.KafkaExecutionException;
import com.devshawn.kafka.gitops.exception.MissingConfigurationException;
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

    @CommandLine.ParentCommand
    private MainCommand parent;

    @Override
    public Integer call() {
        try {
            System.out.println("Generating execution plan...\n");
            ParserService parserService = new ParserService(parent.getFile());
            StateManager stateManager = new StateManager(generateStateManagerConfig(), parserService);
            DesiredPlan desiredPlan = stateManager.plan();
            LogUtil.printPlan(desiredPlan);
            return 0;
        } catch (MissingConfigurationException ex) {
            LogUtil.printGenericError(ex);
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
                .setNullablePlanFile(outputFile)
                .build();
    }
}
