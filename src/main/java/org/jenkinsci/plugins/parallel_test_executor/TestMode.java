package org.jenkinsci.plugins.parallel_test_executor;

public enum TestMode  {
    JAVA ("Parallelize on test classes (Java)"),
    TESTCASENAME ("Parallelize on test case name (Generic)"),
    CLASSANDTESTCASENAME ("Parallelize on class and test case name (Generic)"),
    CLASSANDTESTCASENAMEJSON ("Parallelize on class and test case name (Generic, JSON output)");

    private final String description;

    TestMode(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
