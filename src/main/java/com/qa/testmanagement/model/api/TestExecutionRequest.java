package com.qa.testmanagement.model.api;

import com.qa.testmanagement.model.TestStatus;

public class TestExecutionRequest {
    private Long testCaseId;
    private String executedBy;
    private String actualResult;
    private TestStatus status;
    private String remarks;

    // Getters and Setters
    public Long getTestCaseId() {
        return testCaseId;
    }

    public void setTestCaseId(Long testCaseId) {
        this.testCaseId = testCaseId;
    }

    public String getExecutedBy() {
        return executedBy;
    }

    public void setExecutedBy(String executedBy) {
        this.executedBy = executedBy;
    }

    public String getActualResult() {
        return actualResult;
    }

    public void setActualResult(String actualResult) {
        this.actualResult = actualResult;
    }

    public TestStatus getStatus() {
        return status;
    }

    public void setStatus(TestStatus status) {
        this.status = status;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }
}