package com.qa.testmanagement.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Entity
@Table(name = "executions")
public class Execution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_case_id", nullable = false)
    private TestCase testCase;

    @Enumerated(EnumType.STRING)
    @NotNull(message = "Status is required")
    @Column(name = "status", nullable = false)
    private TestStatus status;

    @NotBlank(message = "Executed by is required")
    @Column(name = "executed_by", nullable = false)
    private String executedBy;

    @Column(name = "executed_on", nullable = false)
    private LocalDateTime executedOn;

    @Column(name = "remarks", length = 1000)
    private String remarks;

    @Column(name = "actual_result", length = 2000)
    private String actualResult;

    @NotBlank(message = "Expected Results is required")
    @Column(name = "expected_result_verified", length = 2000, nullable = false)
    private String expectedResultVerified;

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

    public TestCase getTestCase() {
        return testCase;
    }

    public void setTestCase(TestCase testCase) {
        this.testCase = testCase;
    }

    public TestStatus getStatus() {
        return status;
    }

    public void setStatus(TestStatus status) {
        this.status = status;
    }

    public String getExecutedBy() {
        return executedBy;
    }

    public void setExecutedBy(String executedBy) {
        this.executedBy = executedBy;
    }

    public LocalDateTime getExecutedOn() {
        return executedOn;
    }

    public void setExecutedOn(LocalDateTime executedOn) {
        this.executedOn = executedOn;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public String getActualResult() {
        return actualResult;
    }

    public void setActualResult(String actualResult) {
        this.actualResult = actualResult;
    }

    public String getExpectedResultVerified() {
        return expectedResultVerified;
    }

    public void setExpectedResultVerified(String expectedResultVerified) {
        this.expectedResultVerified = expectedResultVerified;
    }
}