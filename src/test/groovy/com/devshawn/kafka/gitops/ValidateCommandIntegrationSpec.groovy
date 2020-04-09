package com.devshawn.kafka.gitops


import picocli.CommandLine
import spock.lang.Specification

class ValidateCommandIntegrationSpec extends Specification {

    void 'test validate state files'() {
        setup:
        ByteArrayOutputStream out = new ByteArrayOutputStream()
        PrintStream oldOut = System.out
        System.setOut(new PrintStream(out))
        String file = TestUtils.getResourceFilePath("plans/${planName}.yaml")
        MainCommand mainCommand = new MainCommand()
        CommandLine cmd = new CommandLine(mainCommand)

        when:
        int exitCode = cmd.execute("-f", file, "validate")

        then:
        exitCode == 0
        out.toString() == "[VALID] Successfully validated the desired state file.\n"

        cleanup:
        System.setOut(oldOut)

        where:
        planName << [
                "simple",
                "application-service",
                "kafka-connect-service",
                "kafka-streams-service",
                "topics-and-services",
                "multi-file"
        ]
    }
}
