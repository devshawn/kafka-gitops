package com.devshawn.kafka.gitops

import java.nio.file.Path
import java.nio.file.Paths
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
        environmentVariables.set("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092,localhost:9093,localhost:9094")
        environmentVariables.set("KAFKA_SASL_JAAS_USERNAME", "test")
        environmentVariables.set("KAFKA_SASL_JAAS_PASSWORD", "test-secret")
        environmentVariables.set("KAFKA_SASL_MECHANISM", "PLAIN")
        environmentVariables.set("KAFKA_SECURITY_PROTOCOL", "SASL_PLAINTEXT")
        environmentVariables.set("SCHEMA_REGISTRY_URL", "http://localhost:8082")
        Path resourceDirectory = Paths.get("src","test","resources", "plans", "schema_registry", "schemas");
        String absolutePath = resourceDirectory.toFile().getAbsolutePath();
        environmentVariables.set("SCHEMA_DIRECTORY", absolutePath)
        TestUtils.cleanUpAll()
    }

    void cleanupSpec() {
//        TestUtils.cleanUpAll()
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
                "topics-and-services",
                "multi-file",
                "simple-users",
                "custom-service-acls",
                "custom-user-acls",
                "custom-group-id-application",
                "custom-group-id-connect",
                "custom-application-id-streams",
                "custom-storage-topic",
                "custom-storage-topics",
                "default-replication",
                "default-replication-multiple",
                "describe-topic-acl-disabled",
                "describe-topic-acl-enabled"
        ]
    }

    void 'test skip-acls flag'() {
        setup:
        String planOutputFile = "/tmp/plan.json"
        String file = TestUtils.getResourceFilePath("plans/${planName}.yaml")
        MainCommand mainCommand = new MainCommand()
        CommandLine cmd = new CommandLine(mainCommand)

        when:
        int exitCode = cmd.execute("-f", file, "--skip-acls", "plan", "-o", planOutputFile)

        then:
        exitCode == 0

        when:
        String actualPlan = TestUtils.getFileContent(planOutputFile)
        String expectedPlan = TestUtils.getResourceFileContent("plans/${planName}-plan.json")

        then:
        JSONAssert.assertEquals(expectedPlan, actualPlan, true)

        where:
        planName << [
                "skip-acls"
        ]
    }

    void 'test various valid plans with seed - #planName'() {
        setup:
        TestUtils.cleanUpKafkaCluster()
        TestUtils.seedKafkaCluster()
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
        "seed-topic-modification-2"         | false
        "seed-topic-modification-3"         | false
        "seed-topic-modification-no-delete" | true
        "seed-acl-exists"                   | true
        "seed-blacklist-topics"             | false
        "seed-topic-add-replicas"           | false
        "seed-topic-remove-replicas"        | false
        "seed-topic-add-partitions"         | false
    }

    void 'test include unchanged flag - #planNam #includeUnchanged'() {
        setup:
        TestUtils.cleanUpKafkaCluster()
        TestUtils.seedKafkaCluster()
        String planOutputFile = "/tmp/plan.json"
        String file = TestUtils.getResourceFilePath("plans/${planName}.yaml")
        MainCommand mainCommand = new MainCommand()
        CommandLine cmd = new CommandLine(mainCommand)

        when:
        int exitCode = -1
        if (includeUnchanged) {
            exitCode = cmd.execute("-f", file, "plan", "--include-unchanged", "-o", planOutputFile)
        } else {
            exitCode = cmd.execute("-f", file, "plan", "-o", planOutputFile)
        }

        then:
        exitCode == 0

        when:
        String expected = includeUnchanged ? "${planName}-include-unchanged" : planName
        String actualPlan = TestUtils.getFileContent(planOutputFile)
        String expectedPlan = TestUtils.getResourceFileContent("plans/${expected}-plan.json")

        then:
        JSONAssert.assertEquals(expectedPlan, actualPlan, true)

        where:
        planName     | includeUnchanged
        "seed-basic" | false
        "seed-basic" | true
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
                "invalid-topic",
                "unrecognized-property",
                "invalid-format",
                "invalid-missing-user-principal",
                "invalid-storage-topics",
                "invalid-default-replication-1",
                "invalid-default-replication-2",
                "invalid-topic-remove-partitions"
        ]
    }

    void 'test file that does not exist - #planName'() {
        setup:
        ByteArrayOutputStream err = new ByteArrayOutputStream()
        ByteArrayOutputStream out = new ByteArrayOutputStream()
        PrintStream oldErr = System.err
        PrintStream oldOut = System.out
        System.setErr(new PrintStream(err))
        System.setOut(new PrintStream(out))
        MainCommand mainCommand = new MainCommand()
        CommandLine cmd = new CommandLine(mainCommand)

        when:
        int exitCode = cmd.execute("-f", "null", "plan")

        then:
        exitCode == 2
        out.toString() == TestUtils.getResourceFileContent("plans/null-file-output.txt")

        cleanup:
        System.setErr(oldErr)
        System.setOut(oldOut)
    }

    void 'test writing file throws a WritePlanOutputException'() {
        setup:
        ByteArrayOutputStream out = new ByteArrayOutputStream()
        PrintStream oldOut = System.out
        System.setOut(new PrintStream(out))
        MainCommand mainCommand = new MainCommand()
        CommandLine cmd = new CommandLine(mainCommand)
        String file = TestUtils.getResourceFilePath("plans/simple.yaml")

        when:
        int exitCode = cmd.execute("-f", file, "plan", "-o", "/test.json")

        then:
        exitCode == 2
        out.toString().contains("Permission denied") \
          || out.toString().contains("Permission non accordée") \
          || out.toString().contains("Read-only") \
          || out.toString().contains("Lecture seule")

        cleanup:
        System.setOut(oldOut)
    }

    void 'test plan that has no changes - #includeUnchanged'() {
        setup:
        TestUtils.cleanUpKafkaCluster()
        TestUtils.seedKafkaCluster()
        ByteArrayOutputStream out = new ByteArrayOutputStream()
        PrintStream oldOut = System.out
        System.setOut(new PrintStream(out))
        String planOutputFile = "/tmp/plan.json"
        String file = TestUtils.getResourceFilePath("plans/${planName}.yaml")
        MainCommand mainCommand = new MainCommand()
        CommandLine cmd = new CommandLine(mainCommand)

        when:
        int exitCode = -1
        if (includeUnchanged) {
            exitCode = cmd.execute("-f", file, "plan", "--include-unchanged", "-o", planOutputFile)
        } else {
            exitCode = cmd.execute("-f", file, "plan", "-o", planOutputFile)
        }

        then:
        exitCode == 0
        out.toString() == TestUtils.getResourceFileContent("plans/no-changes-output.txt")

        when:
        String expected = includeUnchanged ? "${planName}-include-unchanged" : planName
        String actualPlan = TestUtils.getFileContent(planOutputFile)
        String expectedPlan = TestUtils.getResourceFileContent("plans/${expected}-plan.json")

        then:
        JSONAssert.assertEquals(expectedPlan, actualPlan, true)

        cleanup:
        System.setOut(oldOut)

        where:
        planName     | includeUnchanged
        "no-changes" | false
        "no-changes" | true
    }

    void 'test various valid schema registry plans - #planName'() {
        setup:
        TestUtils.cleanUpAll()
        String planOutputFile = "/tmp/plan.json"
        String file = TestUtils.getResourceFilePath("plans/schema_registry/${planName}.yaml")
        MainCommand mainCommand = new MainCommand()
        CommandLine cmd = new CommandLine(mainCommand)
  
        when:
        int exitCode = cmd.execute("-f", file, "plan", "-o", planOutputFile)
  
        then:
        exitCode == 0
  
        when:
        String actualPlan = TestUtils.getFileContent(planOutputFile)
        String expectedPlan = TestUtils.getResourceFileContent("plans/schema_registry/${planName}-plan.json")
  
        then:
        JSONAssert.assertEquals(expectedPlan, actualPlan, true)
  
        where:
        planName << [
                "schema-registry-new-json",
                "schema-registry-new-avro",
                "schema-registry-new-proto",
                "schema-registry-default",
                "schema-registry-mix"
        ]
    }

    void 'test various valid schema registry plans with seed - #planName'() {
          setup:
          TestUtils.seedSchemaRegistry()
          String planOutputFile = "/tmp/plan.json"
          String file = TestUtils.getResourceFilePath("plans/schema_registry/${planName}.yaml")
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
          String expectedPlan = TestUtils.getResourceFileContent("plans/schema_registry/${planName}-plan.json")
  
          then:
          JSONAssert.assertEquals(expectedPlan, actualPlan, true)
  
          where:
          planName                             | deleteDisabled
          "seed-schema-modification"           | false
          "seed-schema-modification-2"         | false
          "seed-schema-modification-3"         | false
          "seed-schema-modification-4"         | false
          "seed-schema-modification-no-delete" | true
    }

    void 'test schema registry plan that has no changes - #includeUnchanged'() {
        setup:
        TestUtils.seedSchemaRegistry()
        ByteArrayOutputStream out = new ByteArrayOutputStream()
        PrintStream oldOut = System.out
        System.setOut(new PrintStream(out))
        String planOutputFile = "/tmp/plan.json"
        String file = TestUtils.getResourceFilePath("plans/schema_registry/${planName}.yaml")
        MainCommand mainCommand = new MainCommand()
        CommandLine cmd = new CommandLine(mainCommand)

        when:
        int exitCode = -1
        if (includeUnchanged) {
            exitCode = cmd.execute("-f", file, "plan", "--include-unchanged", "-o", planOutputFile)
        } else {
            exitCode = cmd.execute("-f", file, "plan", "-o", planOutputFile)
        }

        then:
        exitCode == 0
        out.toString() == TestUtils.getResourceFileContent("plans/schema_registry/no-changes-output.txt")

        when:
        String expected = includeUnchanged ? "${planName}-include-unchanged" : planName
        String actualPlan = TestUtils.getFileContent(planOutputFile)
        String expectedPlan = TestUtils.getResourceFileContent("plans/schema_registry/${expected}-plan.json")

        then:
        JSONAssert.assertEquals(expectedPlan, actualPlan, true)

        cleanup:
        System.setOut(oldOut)

        where:
        planName     | includeUnchanged
        "no-changes" | false
        "no-changes" | true
    }

    void 'test invalid schema registry plans - #planName'() {
        setup:
        ByteArrayOutputStream err = new ByteArrayOutputStream()
        ByteArrayOutputStream out = new ByteArrayOutputStream()
        PrintStream oldErr = System.err
        PrintStream oldOut = System.out
        System.setErr(new PrintStream(err))
        System.setOut(new PrintStream(out))
        String file = TestUtils.getResourceFilePath("plans/schema_registry/${planName}.yaml")
        MainCommand mainCommand = new MainCommand()
        CommandLine cmd = new CommandLine(mainCommand)

        when:
        int exitCode = cmd.execute("-f", file, "plan")

        then:
        exitCode == 2
        out.toString() == TestUtils.getResourceFileContent("plans/schema_registry/${planName}-output.txt")

        cleanup:
        System.setErr(oldErr)
        System.setOut(oldOut)

        where:
        planName << [
                "invalid-type",
                "invalid-missing-type",
                "invalid-compatibility",
                "invalid-missing-compatibility",
                "invalid-unrecognized-property",
                "invalid-missing-file-and-schema",
                "invalid-both-file-and-schema"
        ]
    }

    void 'test various invalid schema registry plans with seed - #planName'() {
        setup:
        TestUtils.seedSchemaRegistry()
        ByteArrayOutputStream err = new ByteArrayOutputStream()
        ByteArrayOutputStream out = new ByteArrayOutputStream()
        PrintStream oldErr = System.err
        PrintStream oldOut = System.out
        System.setErr(new PrintStream(err))
        System.setOut(new PrintStream(out))
        String file = TestUtils.getResourceFilePath("plans/schema_registry/${planName}.yaml")
        MainCommand mainCommand = new MainCommand()
        CommandLine cmd = new CommandLine(mainCommand)

        when:
        int exitCode = cmd.execute("-f", file, "plan")

        then:
        exitCode == 2
        out.toString() == TestUtils.getResourceFileContent("plans/schema_registry/${planName}-output.txt")

        cleanup:
        System.setErr(oldErr)
        System.setOut(oldOut)

        where:
        planName << [
                "invalid-modify-type",
                "invalid-modify-compatibility",
                "invalid-modify-not-compatible"
        ]
    }
    
    void 'test various invalid schema registry plans with seed (regex) - #planName'() {
        setup:
        TestUtils.seedSchemaRegistry()
        ByteArrayOutputStream err = new ByteArrayOutputStream()
        ByteArrayOutputStream out = new ByteArrayOutputStream()
        PrintStream oldErr = System.err
        PrintStream oldOut = System.out
        System.setErr(new PrintStream(err))
        System.setOut(new PrintStream(out))
        String file = TestUtils.getResourceFilePath("plans/schema_registry/${planName}.yaml")
        MainCommand mainCommand = new MainCommand()
        CommandLine cmd = new CommandLine(mainCommand)
  
        when:
        int exitCode = cmd.execute("-f", file, "plan")
        String pattern = TestUtils.getResourceFileContent("plans/schema_registry/${planName}-output.txt")
  
        then:
        exitCode == 2
        out.toString().matches(pattern)
  
        cleanup:
        System.setErr(oldErr)
        System.setOut(oldOut)
  
        where:
        planName << [
                "invalid-reference"
        ]
    }
}
