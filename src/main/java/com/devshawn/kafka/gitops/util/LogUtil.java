package com.devshawn.kafka.gitops.util;

import com.devshawn.kafka.gitops.domain.plan.AclPlan;
import com.devshawn.kafka.gitops.domain.plan.DesiredPlan;
import com.devshawn.kafka.gitops.domain.plan.PlanOverview;
import com.devshawn.kafka.gitops.domain.plan.TopicConfigPlan;
import com.devshawn.kafka.gitops.domain.plan.TopicDetailsPlan;
import com.devshawn.kafka.gitops.domain.plan.TopicPlan;
import com.devshawn.kafka.gitops.domain.state.AclDetails;
import com.devshawn.kafka.gitops.enums.PlanAction;
import com.devshawn.kafka.gitops.exception.KafkaExecutionException;
import com.devshawn.kafka.gitops.exception.WritePlanOutputException;
import picocli.CommandLine;

public class LogUtil {

    public static void printPlan(DesiredPlan desiredPlan, boolean deleteDisabled, boolean skipAclsDisabled) {
        PlanOverview planOverview = PlanUtil.getOverview(desiredPlan, deleteDisabled, skipAclsDisabled);

        printLegend(planOverview);

        printTopicOverview(desiredPlan, deleteDisabled);
        desiredPlan.getTopicPlans().forEach(LogUtil::printTopicPlan);

        printAclOverview(desiredPlan, deleteDisabled);
        desiredPlan.getAclPlans().forEach(LogUtil::printAclPlan);

        printOverview(desiredPlan, deleteDisabled, skipAclsDisabled);
    }

    public static void printValidationResult(String message, boolean success) {
        String status = success ? green("VALID") : red("INVALID");
        System.out.println(String.format("[%s] %s", status, message));
    }

    /*
     * Plan
     */

    private static void printTopicPlan(TopicPlan topicPlan) {
        switch (topicPlan.getAction()) {
            case ADD:
                System.out.println(green(String.format("+ [TOPIC] %s", topicPlan.getName())));
                printTopicConfigPlanForNewTopics(topicPlan);
                System.out.println("\n");
                break;
            case UPDATE:
                System.out.println(yellow(String.format("~ [TOPIC] %s", topicPlan.getName())));
                if(topicPlan.getTopicDetailsPlan().isPresent()) {
                    LogUtil.printTopicDetailsPlan(topicPlan.getTopicDetailsPlan().get());
                }
                if(!topicPlan.getTopicConfigPlans().isEmpty()) {
                    System.out.println(yellow("\t~ configs:"));
                    topicPlan.getTopicConfigPlans().forEach(LogUtil::printTopicConfigPlan);
                }
                System.out.println("\n");
                break;
            case REMOVE:
                System.out.println(red(String.format("- [TOPIC] %s", topicPlan.getName())));
                System.out.println("\n");
                break;
        }
    }

    private static void printTopicConfigPlanForNewTopics(TopicPlan topicPlan) {
        System.out.println(green(String.format("\t+ partitions: %s", topicPlan.getTopicDetailsPlan().get().getPartitions().get())));
        System.out.println(green(String.format("\t+ replication: %s", topicPlan.getTopicDetailsPlan().get().getReplication().get())));
        if (! topicPlan.getTopicConfigPlans().isEmpty()) {
            System.out.println(green("\t+ configs:"));
            topicPlan.getTopicConfigPlans().forEach(topicConfigPlan -> System.out.println(green(String.format("\t\t+ %s: %s", topicConfigPlan.getKey(), topicConfigPlan.getValue().get()))));
        }
    }

    private static void printTopicDetailsPlan(TopicDetailsPlan topicDetailsPlan) {
        switch (topicDetailsPlan.getPartitionsAction()) {
            case ADD:
                System.out.println(green(String.format("\t+ partitions: %s", topicDetailsPlan.getPartitions().get())));
                break;
            case UPDATE:
                System.out.println(yellow(String.format("\t~ partitions: %s (%s)", topicDetailsPlan.getPartitions().get(), topicDetailsPlan.getPreviousPartitions().get())));
                break;
            case REMOVE:
                System.out.println(red(String.format("\t- partitions (%s)", topicDetailsPlan.getPreviousPartitions().get())));
                break;
            case NO_CHANGE:
                break;
        }
        switch (topicDetailsPlan.getReplicationAction()) {
          case ADD:
              System.out.println(green(String.format("\t+ replication: %s", topicDetailsPlan.getReplication().get())));
              break;
          case UPDATE:
              System.out.println(yellow(String.format("\t~ replication: %s (%s)", topicDetailsPlan.getReplication().get(), topicDetailsPlan.getPreviousReplication().get())));
              break;
          case REMOVE:
              System.out.println(red(String.format("\t- replication (%s)", topicDetailsPlan.getPreviousReplication().get())));
              break;
          case NO_CHANGE:
              break;
      }
    }

    private static void printTopicConfigPlan(TopicConfigPlan topicConfigPlan) {
        switch (topicConfigPlan.getAction()) {
            case ADD:
                System.out.println(green(String.format("\t\t+ %s: %s", topicConfigPlan.getKey(), topicConfigPlan.getValue().get())));
                break;
            case UPDATE:
                System.out.println(yellow(String.format("\t\t~ %s: %s ( %s )", topicConfigPlan.getKey(), topicConfigPlan.getValue().get(), topicConfigPlan.getPreviousValue().get())));
                break;
            case REMOVE:
                System.out.println(red(String.format("\t\t- %s (%s)", topicConfigPlan.getKey(), topicConfigPlan.getPreviousValue().get())));
                break;
            case NO_CHANGE:
              break;
        }
    }

    private static void printAclPlan(AclPlan aclPlan) {
        AclDetails aclDetails = aclPlan.getAclDetails();

        switch (aclPlan.getAction()) {
            case ADD:
                System.out.println(green(String.format("+ [ACL] %s", aclPlan.getName())));
                System.out.println(green(String.format("\t + resource_name: %s", aclDetails.getName())));
                System.out.println(green(String.format("\t + resource_type: %s", aclDetails.getType())));
                System.out.println(green(String.format("\t + resource_pattern: %s", aclDetails.getPattern())));
                System.out.println(green(String.format("\t + resource_principal: %s", aclDetails.getPrincipal())));
                System.out.println(green(String.format("\t + host: %s", aclDetails.getHost())));
                System.out.println(green(String.format("\t + operation: %s", aclDetails.getOperation())));
                System.out.println(green(String.format("\t + permission: %s", aclDetails.getPermission())));
                System.out.println("\n");
                break;
            case REMOVE:
                System.out.println(red(String.format("- [ACL] %s", aclPlan.getName())));
                System.out.println(red(String.format("\t - resource_name: %s", aclDetails.getName())));
                System.out.println(red(String.format("\t - resource_type: %s", aclDetails.getType())));
                System.out.println(red(String.format("\t - resource_pattern: %s", aclDetails.getPattern())));
                System.out.println(red(String.format("\t - resource_principal: %s", aclDetails.getPrincipal())));
                System.out.println(red(String.format("\t - host: %s", aclDetails.getHost())));
                System.out.println(red(String.format("\t - operation: %s", aclDetails.getOperation())));
                System.out.println(red(String.format("\t - permission: %s", aclDetails.getPermission())));
                System.out.println("\n");
                break;
        }
    }

    /*
     * Apply
     */

    public static void printTopicPreApply(TopicPlan topicPlan) {
        System.out.println(String.format("Applying: [%s]\n", toAction(topicPlan.getAction())));
        printTopicPlan(topicPlan);
    }

    public static void printAclPreApply(AclPlan aclPlan) {
        System.out.println(String.format("Applying: [%s]\n", toAction(aclPlan.getAction())));
        printAclPlan(aclPlan);
    }

    public static void printPostApply() {
        System.out.println("Successfully applied.\n");
    }

    /*
     * Helpers
     */

    private static void printOverview(DesiredPlan desiredPlan, boolean deleteDisabled, boolean skipAclsDisabled) {
        PlanOverview planOverview = PlanUtil.getOverview(desiredPlan, deleteDisabled, skipAclsDisabled);
        System.out.println(String.format("%s: %s, %s, %s.", bold("Plan"), toCreate(planOverview.getAdd()),
                toUpdate(planOverview.getUpdate()), toDelete(planOverview.getRemove())));
    }

    private static void printTopicOverview(DesiredPlan desiredPlan, boolean deleteDisabled) {
        PlanOverview topicPlanOverview = PlanUtil.getTopicPlanOverview(desiredPlan, deleteDisabled);
        System.out.println(String.format("Topics: %s, %s, %s.\n", toCreate(topicPlanOverview.getAdd()),
                toUpdate(topicPlanOverview.getUpdate()), toDelete(topicPlanOverview.getRemove())));
    }

    private static void printAclOverview(DesiredPlan desiredPlan, boolean deleteDisabled) {
        PlanOverview aclPlanOverview = PlanUtil.getAclPlanOverview(desiredPlan, deleteDisabled);
        System.out.println(String.format("ACLs: %s, %s, %s.\n", toCreate(aclPlanOverview.getAdd()),
                toUpdate(aclPlanOverview.getUpdate()), toDelete(aclPlanOverview.getRemove())));
    }

    private static void printLegend(PlanOverview planOverview) {
        System.out.println("An execution plan has been generated and is shown below.");
        System.out.println("Resource actions are indicated with the following symbols:");

        if (planOverview.getAdd() > 0) {
            System.out.println(green("  + create"));
        }

        if (planOverview.getUpdate() > 0) {
            System.out.println(yellow("  ~ update"));
        }

        if (planOverview.getRemove() > 0) {
            System.out.println(red("  - delete"));
        }
        System.out.println("\nThe following actions will be performed:\n");

    }

    public static void printNoChangesMessage() {
        System.out.println(String.format("[%s] There are no necessary changes; the actual state matches the desired state.", green("SUCCESS")));
    }

    public static void printApplyOverview(PlanOverview planOverview) {
        System.out.println(String.format("[%s] Apply complete! Resources: %s created, %s updated, %s deleted.", green("SUCCESS"),
                planOverview.getAdd(), planOverview.getUpdate(), planOverview.getRemove()));
    }

    public static void printSimpleSuccess(String message) {
        System.out.println(String.format("[%s] %s\n", green("SUCCESS"), message));
    }

    public static void printSimpleError(String message) {
        System.out.println(String.format("[%s] %s\n", red("ERROR"), message));
    }

    public static void printGenericError(RuntimeException ex) {
        printGenericError(ex, false);
    }

    public static void printGenericError(RuntimeException ex, boolean apply) {
        System.out.println(String.format("[%s] %s\n", red("ERROR"), ex.getMessage()));
        if (apply) {
            printApplyErrorMessage();
        } else {
            printPlanErrorMessage();
        }
    }

    public static void printKafkaExecutionError(KafkaExecutionException ex) {
        printKafkaExecutionError(ex, false);
    }

    public static void printKafkaExecutionError(KafkaExecutionException ex, boolean apply) {
        System.out.println(String.format("[%s] %s:\n%s\n", red("ERROR"), ex.getMessage(), ex.getExceptionMessage()));
        if (apply) {
            printApplyErrorMessage();
        } else {
            printPlanErrorMessage();
        }
    }

    public static void printPlanOutputError(WritePlanOutputException ex) {
        System.out.println(String.format("[%s] %s", red("ERROR"), ex.getMessage()));
    }

    private static void printPlanErrorMessage() {
        System.out.println(String.format("[%s] An error has occurred during the planning process. No plan was created.", red("ERROR")));
    }

    private static void printApplyErrorMessage() {
        System.out.println(String.format("[%s] An error has occurred during the apply process.", red("ERROR")));
        System.out.println(String.format("[%s] The apply process has stopped in place. There is no rollback.", red("ERROR")));
        System.out.println(String.format("[%s] Fix the error, re-create a plan, and apply the new plan to continue.", red("ERROR")));
    }

    private static String green(String message) {
        return CommandLine.Help.Ansi.AUTO.string(String.format("@|green %s|@", message));
    }

    private static String yellow(String message) {
        return CommandLine.Help.Ansi.AUTO.string(String.format("@|yellow %s|@", message));
    }

    private static String red(String message) {
        return CommandLine.Help.Ansi.AUTO.string(String.format("@|red %s|@", message));
    }

    private static String bold(String message) {
        return CommandLine.Help.Ansi.AUTO.string(String.format("@|bold %s|@", message));
    }

    private static String toCreate(long num) {
        return green(String.format("%s to create", num));
    }

    private static String toUpdate(long num) {
        return yellow(String.format("%s to update", num));
    }

    private static String toDelete(long num) {
        return red(String.format("%s to delete", num));
    }

    private static String toAction(PlanAction planAction) {
        switch (planAction) {
            case ADD:
                return green("CREATE");
            case UPDATE:
                return yellow("UPDATE");
            case REMOVE:
                return red("DELETE");
        }
        return null;
    }
}
