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

}
