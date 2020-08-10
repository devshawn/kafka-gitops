package com.devshawn.kafka.gitops


import picocli.CommandLine
import spock.lang.Specification
import spock.lang.Unroll

@Unroll
class ValidateCommandIntegrationSpec extends Specification {

    void 'test validate state files - #planName'() {
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

    void 'test invalid state files - #planName'() {
        setup:
        ByteArrayOutputStream out = new ByteArrayOutputStream()
        PrintStream oldOut = System.out
        System.setOut(new PrintStream(out))
        String file = TestUtils.getResourceFilePath("plans/${planName}.yaml")
        String output = TestUtils.getResourceFileContent("plans/${planName}-validate-output.txt")
        MainCommand mainCommand = new MainCommand()
        CommandLine cmd = new CommandLine(mainCommand)

        when:
        int exitCode = cmd.execute("-f", file, "validate")

        then:
        exitCode == 2
        out.toString() == output

        cleanup:
        System.setOut(oldOut)

        where:
        planName << [
                "invalid-custom-service-acls-1",
                "invalid-custom-service-acls-2",
                "invalid-custom-user-acls-1",
                "invalid-custom-user-acls-2"
        ]
    }
}
