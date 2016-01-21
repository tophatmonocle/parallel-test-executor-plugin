package org.jenkinsci.plugins.parallel_test_executor;

import java.util.List;
import org.jenkinsci.plugins.parallel_test_executor.ParallelTestExecutor.Knapsack;

/**
 * Represents a result of the test parallelization granularity of interest.
 */
public abstract class TestEntity implements Comparable<TestEntity> {
    long duration;
    /**
     * Knapsack that this test class belongs to.
     */
    Knapsack knapsack;
    
    TestEntity() {}
    
    @Override
    public int compareTo(TestEntity that) {
        long l = this.duration - that.duration;
        // sort them in the descending order
        if (l>0)    return -1;
        if (l<0)    return 1;
        return 0;
    }
    
    public abstract List<String> getOutputString();
}
