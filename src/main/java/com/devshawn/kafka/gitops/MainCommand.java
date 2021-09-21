package com.devshawn.kafka.gitops;

import com.devshawn.kafka.gitops.cli.AccountCommand;
import com.devshawn.kafka.gitops.cli.ApplyCommand;
import com.devshawn.kafka.gitops.cli.PlanCommand;
import com.devshawn.kafka.gitops.cli.ValidateCommand;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.File;
import java.util.concurrent.Callable;

@Command(name = "kafka-gitops",
        version = "0.2.15",
        exitCodeOnInvalidInput = 0,
        subcommands = {
                AccountCommand.class,
                ApplyCommand.class,
                PlanCommand.class,
                ValidateCommand.class
        },
        description = "Manage Kafka resources with a desired state file.")
public class MainCommand implements Callable<Integer> {

    @Option(names = {"-c", "--command-config"}, paramLabel = "<file>",
            description = "Command config properties file.")
    private File configFile;

    @Option(names = {"-f", "--file"}, paramLabel = "<file>",
            description = "Specify the desired state file.", defaultValue = "state.yaml")
    private File stateFile;

    @Option(names = {"--no-delete"}, description = "Disable the ability to delete resources.")
    private boolean deleteDisabled = false;

    @Option(names = {"--skip-acls"}, description = "Do not take ACLs into account during plans or applies.")
    private boolean skipAcls = false;

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

    public File getConfigFile() {
        return configFile;
    }

    public File getStateFile() {
        return stateFile;
    }

    public boolean isDeleteDisabled() {
        return deleteDisabled;
    }

    public boolean areAclsDisabled() {
        return skipAcls;
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new MainCommand()).execute(args);
        System.exit(exitCode);
    }
}