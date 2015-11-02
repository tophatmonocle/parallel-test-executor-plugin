package org.jenkinsci.plugins.parallel_test_executor;

import com.google.inject.Inject;
import hudson.Extension;
import hudson.model.Run;
import hudson.model.TaskListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousStepExecution;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

/**
 * Allows the splitting logic to be accessed from a workflow.
 */
public final class SplitStep extends AbstractStepImpl {

    private final Parallelism parallelism;

    private boolean generateInclusions;
    private TestCaseCentricFormat caseCentricFormat;

    @DataBoundConstructor public SplitStep(Parallelism parallelism) {
        this.parallelism = parallelism;
    }

    public Parallelism getParallelism() {
        return parallelism;
    }

    public boolean isGenerateInclusions() { return generateInclusions; }

    @DataBoundSetter
    public void setGenerateInclusions(boolean generateInclusions) {
        this.generateInclusions = generateInclusions;
    }
    
    public TestCaseCentricFormat getCaseCentricFormat() {
        if (caseCentricFormat == null) {
            return TestCaseCentricFormat.DISABLED;
        } else {
            return caseCentricFormat;
        }
    }
    
    @DataBoundSetter
    public void setCaseCentricFormat(TestCaseCentricFormat format) {
        this.caseCentricFormat = format;
    }

    @Extension public static final class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(Execution.class);
        }

        @Override public String getFunctionName() {
            return "splitTests";
        }

        @Override public String getDisplayName() {
            return "Split Test Runs";
        }

    }

    public static final class Execution extends AbstractSynchronousStepExecution<List<?>> {

        @Inject private SplitStep step;
        @StepContextParameter private Run<?,?> build;
        @StepContextParameter private TaskListener listener;

        @Override protected List<?> run() throws Exception {
            if (step.generateInclusions) {
                return ParallelTestExecutor.findTestSplits(step.parallelism, build, listener, step.generateInclusions, step.caseCentricFormat);
            } else {
                List<List<String>> result = new ArrayList<List<String>>();
                for (InclusionExclusionPattern pattern : ParallelTestExecutor.findTestSplits(step.parallelism, build, listener, step.generateInclusions, step.caseCentricFormat)) {
                    result.add(pattern.getList());
                }
                return result;
            }
        }

    }

}
