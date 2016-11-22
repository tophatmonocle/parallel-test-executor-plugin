package org.jenkinsci.plugins.parallel_test_executor;

import hudson.tasks.junit.CaseResult;
import net.sf.json.JSONObject;

import java.util.List;

/**
 * Execution time of a specific test case.
 */
public class TestCase extends TestEntity {
    String output;
    
    public TestCase(CaseResult cr, TestMode testMode) {
        if (testMode == TestMode.CLASSANDTESTCASENAME) {
            this.output = cr.getFullName();
        } else if (testMode == TestMode.CLASSANDTESTCASENAMEJSON) {
            JSONObject json = new JSONObject();
            json.put("suiteName", cr.getSuiteResult().getName());
            json.put("packageName", cr.getPackageName());
            json.put("className", cr.getClassName());
            json.put("simpleClassName", cr.getSimpleName());
            json.put("testName", cr.getName());
            this.output = json.toString();
        } else {
            this.output = cr.getName();
        }
        this.duration = (long)(cr.getDuration()*1000);  // milliseconds is a good enough precision for us
    }

    @Override
    public List<String> getOutputString() {
        return java.util.Collections.singletonList(output);
    }
    
    @Override
    public String toString() {
        return output;
    }
}
