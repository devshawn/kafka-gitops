package com.devshawn.kafka.dsf.cli;

import com.devshawn.kafka.dsf.MainCommand;
import com.devshawn.kafka.dsf.StateManager;
import com.devshawn.kafka.dsf.domain.plan.DesiredPlan;
import com.devshawn.kafka.dsf.exception.KafkaExecutionException;
import com.devshawn.kafka.dsf.exception.MissingConfigurationException;
import com.devshawn.kafka.dsf.exception.ValidationException;
import com.devshawn.kafka.dsf.exception.WritePlanOutputException;
import com.devshawn.kafka.dsf.util.LogUtil;
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
            StateManager stateManager = new StateManager(parent.isVerboseRequested(), parent.getFile(), outputFile);
            DesiredPlan desiredPlan = stateManager.plan();
            stateManager.validatePlanHasChanges(desiredPlan);
            LogUtil.printPlan(desiredPlan);
            stateManager.writePlanToFile(desiredPlan);
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
}
