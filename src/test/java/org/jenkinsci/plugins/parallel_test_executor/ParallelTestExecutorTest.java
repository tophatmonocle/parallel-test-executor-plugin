package org.jenkinsci.plugins.parallel_test_executor;

import hudson.model.FreeStyleProject;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.cps.SnippetizerTester;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.recipes.LocalData;

import static org.junit.Assert.assertTrue;

public class ParallelTestExecutorTest {

    @Rule
    public JenkinsRule jenkinsRule = new JenkinsRule();

    @Test
    @LocalData
    public void xmlWithNoAddJUnitPublisherIsLoadedCorrectly() throws Exception {
        FreeStyleProject p = (FreeStyleProject) jenkinsRule.jenkins.getItem("old");
        ParallelTestExecutor trigger = (ParallelTestExecutor) p.getBuilders().get(0);

        assertTrue(trigger.isArchiveTestResults());
    }

    @Test
    public void workflowGenerateInclusions() throws Exception {
        new SnippetizerTester(jenkinsRule).assertRoundTrip(new SplitStep(new CountDrivenParallelism(5)), "splitTests count(5)");
        SplitStep step = new SplitStep(new TimeDrivenParallelism(3));
        step.setGenerateInclusions(true);
        new SnippetizerTester(jenkinsRule).assertRoundTrip(step, "splitTests generateInclusions: true, parallelism: time(3)");
        WorkflowJob p = jenkinsRule.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
            "def splits = splitTests parallelism: count(2), generateInclusions: true\n" +
            "echo \"splits.size=${splits.size()}\"; for (int i = 0; i < splits.size(); i++) {\n" +
            "  def split = splits[i]; echo \"splits[${i}]: includes=${split.includes} list=${split.list}\"\n" +
            "}\n" +
            "node {\n" +
            "  writeFile file: 'TEST-1.xml', text: '<testsuite name=\"one\"><testcase name=\"x\"/></testsuite>'\n" +
            "  writeFile file: 'TEST-2.xml', text: '<testsuite name=\"two\"><testcase name=\"y\"/></testsuite>'\n" +
            "  junit 'TEST-*.xml'\n" +
            "}", true));
        WorkflowRun b1 = jenkinsRule.assertBuildStatusSuccess(p.scheduleBuild2(0));
        jenkinsRule.assertLogContains("splits.size=1", b1);
        jenkinsRule.assertLogContains("splits[0]: includes=false list=[]", b1);
        WorkflowRun b2 = jenkinsRule.assertBuildStatusSuccess(p.scheduleBuild2(0));
        jenkinsRule.assertLogContains("splits.size=2", b2);
        jenkinsRule.assertLogContains("splits[0]: includes=false list=[two.java, two.class]", b2);
        jenkinsRule.assertLogContains("splits[1]: includes=true list=[two.java, two.class]", b2);
    }

    @Test
    public void workflowIncludeSubsetOfFiles() throws Exception {
        WorkflowJob p = jenkinsRule.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
            "def splits = splitTests parallelism: count(2), generateInclusions: true, testMode: \"CLASSANDTESTCASENAME\", junitFilesToConsider: \".*TEST-[13].*\"\n" +
            "echo \"splits.size=${splits.size()}\"; for (int i = 0; i < splits.size(); i++) {\n" +
            "  def split = splits[i]; echo \"splits[${i}]: includes=${split.includes} list=${split.list}\"\n" +
            "}\n" +
            "node {\n" +
            "  writeFile file: 'TEST-1.xml', text: '<testsuite name=\"one\"><testcase name=\"x\"/></testsuite>'\n" +
            "  writeFile file: 'TEST-2.xml', text: '<testsuite name=\"two\"><testcase name=\"y\"/></testsuite>'\n" +
            "  writeFile file: 'TEST-3.xml', text: '<testsuite name=\"three\"><testcase name=\"z\"/></testsuite>'\n" +
            "  junit 'TEST-*.xml'\n" +
            "}", true));
        WorkflowRun b1 = jenkinsRule.assertBuildStatusSuccess(p.scheduleBuild2(0));
        jenkinsRule.assertLogContains("splits.size=1", b1);
        jenkinsRule.assertLogContains("splits[0]: includes=false list=[]", b1);
        WorkflowRun b2 = jenkinsRule.assertBuildStatusSuccess(p.scheduleBuild2(0));
        jenkinsRule.assertLogContains("splits.size=2", b2);
        jenkinsRule.assertLogContains("2 test cases", b2);
        jenkinsRule.assertLogContains("splits[0]: includes=false list=[three.z]", b2);
        jenkinsRule.assertLogContains("splits[1]: includes=true list=[three.z]", b2);
    }

    @Test
    public void workflowFindSiblingResults() throws Exception {
        WorkflowJob master = jenkinsRule.jenkins.createProject(WorkflowJob.class, "master");
        master.setDefinition(new CpsFlowDefinition(
            "node {\n" +
            "  writeFile file: 'TEST-1.xml', text: '<testsuite name=\"one\"><testcase name=\"x\"/></testsuite>'\n" +
            "  writeFile file: 'TEST-2.xml', text: '<testsuite name=\"two\"><testcase name=\"y\"/></testsuite>'\n" +
            "  writeFile file: 'TEST-3.xml', text: '<testsuite name=\"three\"><testcase name=\"z\"/></testsuite>'\n" +
            "  junit 'TEST-*.xml'\n" +
            "}", true));
        WorkflowJob feature_branch = jenkinsRule.jenkins.createProject(WorkflowJob.class, "feature_branch");
        feature_branch.setDefinition(new CpsFlowDefinition(
            "def splits = splitTests parallelism: count(2), generateInclusions: true, testMode: \"CLASSANDTESTCASENAME\", siblingBranchName: \"master\"\n" +
            "echo \"splits.size=${splits.size()}\"; for (int i = 0; i < splits.size(); i++) {\n" +
            "  def split = splits[i]; echo \"splits[${i}]: includes=${split.includes} list=${split.list}\"\n" +
            "}\n" +
            "node {} \n", true));

        jenkinsRule.assertBuildStatusSuccess(master.scheduleBuild2(0));
        WorkflowRun b2 = jenkinsRule.assertBuildStatusSuccess(feature_branch.scheduleBuild2(0));
        jenkinsRule.assertLogContains("3 test cases", b2);
    }

    @Test
    public void workflowJsonOutput() throws Exception {
        WorkflowJob p = jenkinsRule.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
            "def splits = splitTests parallelism: count(2), generateInclusions: true, testMode: \"CLASSANDTESTCASENAMEJSON\"\n" +
            "echo \"splits.size=${splits.size()}\"; for (int i = 0; i < splits.size(); i++) {\n" +
            "  def split = splits[i]; echo \"splits[${i}]: includes=${split.includes} list=${split.list}\"\n" +
            "}\n" +
            "node {\n" +
            "  writeFile file: 'TEST-1.xml', text: '<testsuite name=\"one\"><testcase classname=\"FooPackage.BarClass\" name=\"x\"/></testsuite>'\n" +
            "  writeFile file: 'TEST-2.xml', text: '<testsuite name=\"two\"><testcase classname=\"FooPackage.BarClass\" name=\"y\"/></testsuite>'\n" +
            "  junit 'TEST-*.xml'\n" +
            "}", true));
        jenkinsRule.assertBuildStatusSuccess(p.scheduleBuild2(0));
        WorkflowRun b2 = jenkinsRule.assertBuildStatusSuccess(p.scheduleBuild2(0));
        jenkinsRule.assertLogContains("splits.size=2", b2);
        jenkinsRule.assertLogContains("2 test cases", b2);
        jenkinsRule.assertLogContains("splits[0]: includes=false list=[{\"suiteName\":\"two\",\"packageName\":\"FooPackage\",\"className\":\"FooPackage.BarClass\",\"simpleClassName\":\"BarClass\",\"testName\":\"y\"}]", b2);
        jenkinsRule.assertLogContains("splits[1]: includes=true list=[{\"suiteName\":\"two\",\"packageName\":\"FooPackage\",\"className\":\"FooPackage.BarClass\",\"simpleClassName\":\"BarClass\",\"testName\":\"y\"}]", b2);
    }

}
