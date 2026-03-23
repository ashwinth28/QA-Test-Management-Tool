package com.qa.testmanagement.model;

public enum TestSuiteStatus {
    PENDING("Pending", "secondary"),
    RUNNING("Running", "primary"),
    PASSED("Passed", "success"),
    FAILED("Failed", "danger"),
    PARTIAL("Partial Pass", "warning");

    private final String displayName;
    private final String bootstrapClass;

    TestSuiteStatus(String displayName, String bootstrapClass) {
        this.displayName = displayName;
        this.bootstrapClass = bootstrapClass;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getBootstrapClass() {
        return bootstrapClass;
    }
}