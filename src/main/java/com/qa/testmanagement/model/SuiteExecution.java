package com.qa.testmanagement.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "suite_executions")
public class SuiteExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "suite_id", nullable = false)
    private TestSuite testSuite;

    @Column(name = "executed_on", nullable = false)
    private LocalDateTime executedOn;

    @Column(name = "executed_by")
    private String executedBy;

    private int totalTests;
    private int passedTests;
    private int failedTests;
    private int blockedTests;
    private int pendingTests;

    @Column(name = "execution_time_ms")
    private long executionTimeMs;

    @ElementCollection
    @CollectionTable(name = "suite_execution_results", joinColumns = @JoinColumn(name = "execution_id"))
    @MapKeyColumn(name = "test_case_id")
    @Column(name = "status")
    private Map<Long, String> testCaseResults = new HashMap<>();

    @PrePersist
    protected void onCreate() {
        executedOn = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public TestSuite getTestSuite() {
        return testSuite;
    }

    public void setTestSuite(TestSuite testSuite) {
        this.testSuite = testSuite;
    }

    public LocalDateTime getExecutedOn() {
        return executedOn;
    }

    public void setExecutedOn(LocalDateTime executedOn) {
        this.executedOn = executedOn;
    }

    public String getExecutedBy() {
        return executedBy;
    }

    public void setExecutedBy(String executedBy) {
        this.executedBy = executedBy;
    }

    public int getTotalTests() {
        return totalTests;
    }

    public void setTotalTests(int totalTests) {
        this.totalTests = totalTests;
    }

    public int getPassedTests() {
        return passedTests;
    }

    public void setPassedTests(int passedTests) {
        this.passedTests = passedTests;
    }

    public int getFailedTests() {
        return failedTests;
    }

    public void setFailedTests(int failedTests) {
        this.failedTests = failedTests;
    }

    public int getBlockedTests() {
        return blockedTests;
    }

    public void setBlockedTests(int blockedTests) {
        this.blockedTests = blockedTests;
    }

    public int getPendingTests() {
        return pendingTests;
    }

    public void setPendingTests(int pendingTests) {
        this.pendingTests = pendingTests;
    }

    public long getExecutionTimeMs() {
        return executionTimeMs;
    }

    public void setExecutionTimeMs(long executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
    }

    public Map<Long, String> getTestCaseResults() {
        return testCaseResults;
    }

    public void setTestCaseResults(Map<Long, String> testCaseResults) {
        this.testCaseResults = testCaseResults;
    }
}