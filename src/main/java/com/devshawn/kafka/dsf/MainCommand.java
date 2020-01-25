package com.devshawn.kafka.dsf;

import com.devshawn.kafka.dsf.cli.ApplyCommand;
import com.devshawn.kafka.dsf.cli.PlanCommand;
import com.devshawn.kafka.dsf.cli.ValidateCommand;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.File;
import java.util.concurrent.Callable;

@Command(name = "kafka-dsf",
        version = "0.0.1",
        exitCodeOnInvalidInput = 0,
        subcommands = {
                ApplyCommand.class,
                PlanCommand.class,
                ValidateCommand.class
        },
        description = "Manage Kafka resources with a desired state file.")
public class MainCommand implements Callable<Integer> {

    @Option(names = {"-f", "--file"}, paramLabel = "<file>",
            description = "Specify the desired state file.", defaultValue = "state.yaml")
    private File file;

    @Option(names = {"-h", "--help"}, usageHelp = true, description = "Display this help message.")
    private boolean helpRequested = false;

    @Option(names = {"-v", "--verbose"}, description = "Show more detail during execution.")
    private boolean verboseRequested = false;

    @Option(names = {"-V", "--version"}, versionHelp = true, description = "Print the current version of this tool.")
    private boolean versionRequested = false;

    @CommandLine.Spec
    CommandLine.Model.CommandSpec spec;

    @Override
    public Integer call() {
        CommandLine commandLine = spec.commandLine();
        if (commandLine.isVersionHelpRequested()) {
            spec.commandLine().printVersionHelp(commandLine.getOut());
            return 0;
        }

        throw new CommandLine.ParameterException(spec.commandLine(), "");
    }

    public boolean isVerboseRequested() {
        return verboseRequested;
    }

    public File getFile() {
        return file;
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new MainCommand()).execute(args);
        System.exit(exitCode);
    }
}