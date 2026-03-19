package com.qa.testmanagement.model;

public enum TestStatus {
    PASS("Pass", "success"),
    FAIL("Fail", "danger"),
    BLOCKED("Blocked", "warning"),
    PENDING("Pending", "secondary"),
    ACTIVE("Active", "primary"),
    SKIPPED("Skipped", "info"),
    RETEST("Retest", "dark");

    private final String displayName;
    private final String bootstrapClass;

    TestStatus(String displayName, String bootstrapClass) {
        this.displayName = displayName;
        this.bootstrapClass = bootstrapClass;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getBootstrapClass() {
        return bootstrapClass;
    }

    public static TestStatus fromString(String text) {
        for (TestStatus status : TestStatus.values()) {
            if (status.displayName.equalsIgnoreCase(text)) {
                return status;
            }
        }
        return null;
    }
}