package com.qa.testmanagement.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Entity
@Table(name = "test_cases")
public class TestCase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @NotBlank(message = "Test Case ID is required")
    @Column(name = "test_case_id", unique = true, nullable = false)
    private String testCaseId;

    @NotBlank(message = "Priority is required")
    private String priority;

    @NotBlank(message = "Test Type is required")
    @Column(name = "test_type")
    private String testType;

    @NotBlank(message = "Test Case Name is required")
    @Column(name = "test_case_name")
    private String testCaseName;

    @Column(length = 1000)
    private String precondition;

    @Column(name = "test_steps", length = 5000)
    private String testSteps;

    @Column(name = "expected_result", length = 2000)
    private String expectedResult;

    @Column(name = "actual_result", length = 2000)
    private String actualResult;

    @Enumerated(EnumType.STRING)
    private TestStatus status = TestStatus.PENDING;

    @Column(length = 1000)
    private String remarks;

    @Column(name = "created_on", nullable = false, updatable = false)
    private LocalDateTime createdOn;

    @Column(name = "executed_on")
    private LocalDateTime executedOn;

    @OneToMany(mappedBy = "testCase", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("executedOn DESC")
    @JsonIgnore // ADD THIS - Prevents circular reference
    private List<Execution> executions = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdOn = LocalDateTime.now();
    }

    @Column(name = "category")
    private String category;

    @ElementCollection
    @CollectionTable(name = "test_case_tags", joinColumns = @JoinColumn(name = "test_case_id"))
    @Column(name = "tag")
    private Set<String> tags = new HashSet<>();

    // Add these helper methods
    @Transient
    public void setTagsFromString(String tagsJson) {
        if (tagsJson != null && !tagsJson.isEmpty()) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                this.tags = mapper.readValue(tagsJson, new TypeReference<Set<String>>() {
                });
            } catch (Exception e) {
                // If JSON parsing fails, try comma-separated
                String[] parts = tagsJson.split(",");
                this.tags = new HashSet<>();
                for (String part : parts) {
                    this.tags.add(part.trim());
                }
            }
        }
    }

    @Transient
    public String getTagsAsString() {
        if (tags == null || tags.isEmpty())
            return "";
        return String.join(", ", tags);
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public String getTestCaseId() {
        return testCaseId;
    }

    public void setTestCaseId(String testCaseId) {
        this.testCaseId = testCaseId;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getTestType() {
        return testType;
    }

    public void setTestType(String testType) {
        this.testType = testType;
    }

    public String getTestCaseName() {
        return testCaseName;
    }

    public void setTestCaseName(String testCaseName) {
        this.testCaseName = testCaseName;
    }

    public String getPrecondition() {
        return precondition;
    }

    public void setPrecondition(String precondition) {
        this.precondition = precondition;
    }

    public String getTestSteps() {
        return testSteps;
    }

    public void setTestSteps(String testSteps) {
        this.testSteps = testSteps;
    }

    public String getExpectedResult() {
        return expectedResult;
    }

    public void setExpectedResult(String expectedResult) {
        this.expectedResult = expectedResult;
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

    public List<Execution> getExecutions() {
        return executions;
    }

    public void setExecutions(List<Execution> executions) {
        this.executions = executions;
    }

    public void addExecution(Execution execution) {
        executions.add(execution);
        execution.setTestCase(this);
    }

    public void removeExecution(Execution execution) {
        executions.remove(execution);
        execution.setTestCase(null);
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Set<String> getTags() {
        return tags;
    }

    public void setTags(Set<String> tags) {
        this.tags = tags;
    }
}