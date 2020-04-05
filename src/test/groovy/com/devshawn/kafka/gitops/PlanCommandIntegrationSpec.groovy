package com.devshawn.kafka.gitops

import org.junit.ClassRule
import org.junit.contrib.java.lang.system.EnvironmentVariables
import org.skyscreamer.jsonassert.JSONAssert
import picocli.CommandLine
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

@Unroll
class PlanCommandIntegrationSpec extends Specification {

    @Shared
    @ClassRule
    EnvironmentVariables environmentVariables

    void setupSpec() {
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

    void 'test various valid plans - #planName'() {
        setup:
        String planOutputFile = "/tmp/plan.json"
        String file = TestUtils.getResourceFilePath("plans/${planName}.yaml")
        MainCommand mainCommand = new MainCommand()
        CommandLine cmd = new CommandLine(mainCommand)

        when:
        int exitCode = cmd.execute("-f", file, "plan", "-o", planOutputFile)

        then:
        exitCode == 0

        when:
        String actualPlan = TestUtils.getFileContent(planOutputFile)
        String expectedPlan = TestUtils.getResourceFileContent("plans/${planName}-plan.json")

        then:
        JSONAssert.assertEquals(expectedPlan, actualPlan, true)

        where:
        planName << [
                "simple",
                "application-service",
                "kafka-connect-service",
                "kafka-streams-service",
                "topics-and-services"
        ]
    }

    void 'test various valid plans with seed - #planName'() {
        setup:
        TestUtils.cleanUpCluster()
        TestUtils.seedCluster()
        String planOutputFile = "/tmp/plan.json"
        String file = TestUtils.getResourceFilePath("plans/${planName}.yaml")
        MainCommand mainCommand = new MainCommand()
        CommandLine cmd = new CommandLine(mainCommand)

        when:
        int exitCode
        if (deleteDisabled) {
            exitCode = cmd.execute("-f", file, "--no-delete", "plan", "-o", planOutputFile)
        } else {
            exitCode = cmd.execute("-f", file, "plan", "-o", planOutputFile)
        }

        then:
        exitCode == 0

        when:
        String actualPlan = TestUtils.getFileContent(planOutputFile)
        String expectedPlan = TestUtils.getResourceFileContent("plans/${planName}-plan.json")

        then:
        JSONAssert.assertEquals(expectedPlan, actualPlan, true)

        where:
        planName                            | deleteDisabled
        "seed-topic-modification"           | false
        "seed-topic-modification-no-delete" | true
    }

    void 'test invalid plans - #planName'() {
        setup:
        ByteArrayOutputStream err = new ByteArrayOutputStream()
        ByteArrayOutputStream out = new ByteArrayOutputStream()
        PrintStream oldErr = System.err
        PrintStream oldOut = System.out
        System.setErr(new PrintStream(err))
        System.setOut(new PrintStream(out))
        String file = TestUtils.getResourceFilePath("plans/${planName}.yaml")
        MainCommand mainCommand = new MainCommand()
        CommandLine cmd = new CommandLine(mainCommand)

        when:
        int exitCode = cmd.execute("-f", file, "plan")

        then:
        exitCode == 2
        out.toString() == TestUtils.getResourceFileContent("plans/${planName}-output.txt")

        cleanup:
        System.setErr(oldErr)
        System.setOut(oldOut)

        where:
        planName << [
                "invalid-missing-principal",
                "invalid-topic"
        ]
    }
}
