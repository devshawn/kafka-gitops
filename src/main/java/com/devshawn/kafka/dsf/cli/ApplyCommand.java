package com.devshawn.kafka.dsf.cli;

import com.devshawn.kafka.dsf.MainCommand;
import com.devshawn.kafka.dsf.StateManager;
import com.devshawn.kafka.dsf.domain.plan.DesiredPlan;
import com.devshawn.kafka.dsf.exception.KafkaExecutionException;
import com.devshawn.kafka.dsf.exception.MissingConfigurationException;
import com.devshawn.kafka.dsf.exception.ValidationException;
import com.devshawn.kafka.dsf.util.LogUtil;
import com.devshawn.kafka.dsf.util.PlanUtil;
import picocli.CommandLine;

import java.io.File;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "apply", description = "Apply changes to Kafka resources.")
public class ApplyCommand implements Callable<Integer> {

    @CommandLine.Option(names = {"-p", "--plan"}, paramLabel = "<file>",
            description = "Specify the plan file to use.")
    private File planFile;

    @CommandLine.ParentCommand
    private MainCommand parent;

    @Override
    public Integer call() {
        try {
            System.out.println("Executing apply...\n");
            DesiredPlan desiredPlan = new StateManager(parent.isVerboseRequested(), parent.getFile(), parent.isDeleteDisabled(), planFile).apply();
            LogUtil.printApplyOverview(PlanUtil.getOverview(desiredPlan));
            return 0;
        } catch (MissingConfigurationException ex) {
            LogUtil.printGenericError(ex, true);
            return 2;
        } catch (ValidationException ex) {
            LogUtil.printValidationResult(ex.getMessage(), false);
            return 2;
        } catch (KafkaExecutionException ex) {
            LogUtil.printKafkaExecutionError(ex, true);
            return 2;
        }
    }
}