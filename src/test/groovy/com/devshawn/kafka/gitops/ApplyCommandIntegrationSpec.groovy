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
//        TestUtils.cleanUpCluster()
    }

    void 'test various successful applies - #planFile'() {
        setup:
        ByteArrayOutputStream out = new ByteArrayOutputStream()
        PrintStream oldOut = System.out
        System.setOut(new PrintStream(out))
        String file = TestUtils.getResourceFilePath("plans/${planFile}-plan.json")
        MainCommand mainCommand = new MainCommand()
        CommandLine cmd = new CommandLine(mainCommand)

        when:
        int exitCode = cmd.execute("-f", file, "apply", "-p", file)

        then:
        exitCode == 0
        out.toString() == TestUtils.getResourceFileContent("plans/${planFile}-apply-output.txt")

        cleanup:
        System.setOut(oldOut)

        where:
        planFile << [
                "simple",
                "application-service",
                "kafka-streams-service",
                "kafka-connect-service",
                "multi-file",
                "simple-users",
                "custom-service-acls",
                "custom-user-acls",
                "custom-group-id-application",
                "custom-group-id-connect",
                "custom-application-id-streams",
                "custom-storage-topic",
                "custom-storage-topics"
        ]
    }

    void 'test skip-acls flag'() {
        setup:
        ByteArrayOutputStream out = new ByteArrayOutputStream()
        PrintStream oldOut = System.out
        System.setOut(new PrintStream(out))
        String file = TestUtils.getResourceFilePath("plans/${planFile}-plan.json")
        MainCommand mainCommand = new MainCommand()
        CommandLine cmd = new CommandLine(mainCommand)

        when:
        int exitCode = cmd.execute("-f", file, "--skip-acls", "apply", "-p", file)

        then:
        exitCode == 0
        out.toString() == TestUtils.getResourceFileContent("plans/${planFile}-apply-output.txt")

        cleanup:
        System.setOut(oldOut)

        where:
        planFile << [
                "skip-acls-apply"
        ]
    }

    void 'test various valid applies with seed - #planFile #deleteDisabled'() {
        setup:
        TestUtils.seedCluster()
        ByteArrayOutputStream out = new ByteArrayOutputStream()
        PrintStream oldOut = System.out
        System.setOut(new PrintStream(out))
        String file = TestUtils.getResourceFilePath("plans/${planFile}-plan.json")
        MainCommand mainCommand = new MainCommand()
        CommandLine cmd = new CommandLine(mainCommand)

        when:
        int exitCode = -1
        if (deleteDisabled) {
            exitCode = cmd.execute("-f", file, "--no-delete", "apply", "-p", file)
        } else {
            exitCode = cmd.execute("-f", file, "apply", "-p", file)
        }

        then:
        exitCode == 0
        if (deleteDisabled) {
            assert out.toString() == TestUtils.getResourceFileContent("plans/${planFile}-no-delete-apply-output.txt")
        } else {
            assert out.toString() == TestUtils.getResourceFileContent("plans/${planFile}-apply-output.txt")
        }

        cleanup:
        System.setOut(oldOut)

        where:
        planFile                         | deleteDisabled
        "seed-topic-modification"        | true
        "seed-topic-modification"        | false
        "seed-topic-modification-3"      | true
        "seed-topic-modification-3"      | false
        "seed-topic-add-replicas"        | false
        "seed-topic-add-partitions"      | false
        "seed-acl-exists"                | false
        "no-changes"                     | false
        "no-changes-include-unchanged"   | false
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
