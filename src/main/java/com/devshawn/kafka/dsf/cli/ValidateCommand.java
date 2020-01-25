package com.devshawn.kafka.dsf.cli;

import com.devshawn.kafka.dsf.MainCommand;
import com.devshawn.kafka.dsf.StateManager;
import com.devshawn.kafka.dsf.exception.ValidationException;
import com.devshawn.kafka.dsf.util.LogUtil;
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
            new StateManager(parent.isVerboseRequested(), parent.getFile()).validate();
            LogUtil.printValidationResult("Successfully validated the desired state file.", true);
            return 0;
        } catch (ValidationException ex) {
            LogUtil.printValidationResult(ex.getMessage(), false);
            return 2;
        }
    }
}
