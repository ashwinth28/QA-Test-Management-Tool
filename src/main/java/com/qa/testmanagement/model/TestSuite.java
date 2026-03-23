package com.qa.testmanagement.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "test_suites")
public class TestSuite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 500)
    private String description;

    @ManyToMany
    @JoinTable(name = "test_suite_test_cases", joinColumns = @JoinColumn(name = "suite_id"), inverseJoinColumns = @JoinColumn(name = "test_case_id"))
    private List<TestCase> testCases = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    private TestSuiteStatus status = TestSuiteStatus.PENDING;

    @Column(name = "created_on", nullable = false, updatable = false)
    private LocalDateTime createdOn;

    @Column(name = "executed_on")
    private LocalDateTime executedOn;

    @Column(name = "executed_by")
    private String executedBy;

    @OneToMany(mappedBy = "testSuite", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<SuiteExecution> executions = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdOn = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<TestCase> getTestCases() {
        return testCases;
    }

    public void setTestCases(List<TestCase> testCases) {
        this.testCases = testCases;
    }

    public TestSuiteStatus getStatus() {
        return status;
    }

    public void setStatus(TestSuiteStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedOn() {
        return createdOn;
    }

    public void setCreatedOn(LocalDateTime createdOn) {
        this.createdOn = createdOn;
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

    public List<SuiteExecution> getExecutions() {
        return executions;
    }

    public void setExecutions(List<SuiteExecution> executions) {
        this.executions = executions;
    }
}