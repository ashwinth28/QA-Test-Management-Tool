package com.qa.testmanagement.model.api;

public class TestSuiteExecutionRequest {
    private Long suiteId;
    private String executedBy;

    // Getters and Setters
    public Long getSuiteId() {
        return suiteId;
    }

    public void setSuiteId(Long suiteId) {
        this.suiteId = suiteId;
    }

    public String getExecutedBy() {
        return executedBy;
    }

    public void setExecutedBy(String executedBy) {
        this.executedBy = executedBy;
    }
}