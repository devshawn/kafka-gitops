package com.devshawn.kafka.gitops.cli;

import ch.qos.logback.classic.Logger;
import com.devshawn.kafka.gitops.MainCommand;
import com.devshawn.kafka.gitops.exception.KafkaExecutionException;
import com.devshawn.kafka.gitops.exception.MissingConfigurationException;
import com.devshawn.kafka.gitops.exception.ValidationException;
import com.devshawn.kafka.gitops.exception.WritePlanOutputException;
import com.devshawn.kafka.gitops.util.LogUtil;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.util.concurrent.Callable;

@CommandLine.Command(name = "account", description = "Create Confluent Cloud service accounts.")
public class AccountCommand implements Callable<Integer> {

    @CommandLine.ParentCommand
    private MainCommand parent;

    @Override
    public Integer call() {
        try {
            Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);

            System.out.println("Generating execution plan...\n");
//            new StateManager(false, null, false, null).createServiceAccount("TestAccount7");
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
