package com.qa.testmanagement.model;

public enum UserRole {
    ADMIN("Admin", "Full system access"),
    TESTER("Tester", "Can create and execute test cases"),
    VIEWER("Viewer", "View only access");

    private final String displayName;
    private final String description;

    UserRole(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}