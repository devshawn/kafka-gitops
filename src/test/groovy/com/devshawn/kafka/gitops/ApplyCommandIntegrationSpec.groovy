package com.devshawn.kafka.gitops

import org.junit.Rule
import org.junit.contrib.java.lang.system.EnvironmentVariables
import picocli.CommandLine
import spock.lang.Specification
import spock.lang.Unroll

@Unroll
class ApplyCommandIntegrationSpec extends Specification {

    @Rule
    EnvironmentVariables environmentVariables

    void setup() {
        environmentVariables.set("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092")
        environmentVariables.set("KAFKA_SASL_JAAS_USERNAME", "test")
        environmentVariables.set("KAFKA_SASL_JAAS_PASSWORD", "test-secret")
        environmentVariables.set("KAFKA_SASL_MECHANISM", "PLAIN")
        environmentVariables.set("KAFKA_SECURITY_PROTOCOL", "SASL_PLAINTEXT")
        TestUtils.cleanUpCluster()
    }

    void cleanupSpec() {
        TestUtils.cleanUpCluster()
    }

    void 'test various successful applies - #planFile'() {
        setup:
        String file = TestUtils.getResourceFilePath("plans/${planFile}-plan.json")
        MainCommand mainCommand = new MainCommand()
        CommandLine cmd = new CommandLine(mainCommand)

        when:
        int exitCode = cmd.execute("-f", file, "apply", "-p", file)

        then:
        exitCode == 0

        where:
        planFile << [
                "simple",
                "application-service"
        ]
    }

    void 'test reading missing file throws ReadPlanInputException'() {
        setup:
        ByteArrayOutputStream out = new ByteArrayOutputStream()
        PrintStream oldOut = System.out
        System.setOut(new PrintStream(out))
        MainCommand mainCommand = new MainCommand()
        CommandLine cmd = new CommandLine(mainCommand)
        String file = TestUtils.getResourceFilePath("plans/simple.yaml")

        when:
        int exitCode = cmd.execute("-f", file, "apply", "-p", "null")

        then:
        exitCode == 2
        out.toString() == TestUtils.getResourceFileContent("plans/read-input-exception-output.txt")

        cleanup:
        System.setOut(oldOut)
    }

    void 'test reading invalid file throws ReadPlanInputException'() {
        setup:
        ByteArrayOutputStream out = new ByteArrayOutputStream()
        PrintStream oldOut = System.out
        System.setOut(new PrintStream(out))
        MainCommand mainCommand = new MainCommand()
        CommandLine cmd = new CommandLine(mainCommand)
        String file = TestUtils.getResourceFilePath("plans/simple.yaml")
        String planFile = TestUtils.getResourceFilePath("plans/invalid-plan.json")

        when:
        int exitCode = cmd.execute("-f", file, "apply", "-p", planFile)

        then:
        exitCode == 2
        out.toString() == TestUtils.getResourceFileContent("plans/invalid-plan-output.txt")

        cleanup:
        System.setOut(oldOut)
    }

}
