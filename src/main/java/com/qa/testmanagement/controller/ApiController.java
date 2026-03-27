package com.qa.testmanagement.controller;

import com.qa.testmanagement.model.*;
import com.qa.testmanagement.model.api.*;
import com.qa.testmanagement.repository.*;
import com.qa.testmanagement.service.ApiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@Tag(name = "QA Test Management API", description = "REST API for CI/CD Integration")
public class ApiController {

    @Autowired
    private ApiService apiService;

    @Autowired
    private TestCaseRepository testCaseRepository;

    @Autowired
    private TestSuiteRepository testSuiteRepository;

    @Autowired
    private ExecutionRepository executionRepository; // ADD THIS - was missing

    @GetMapping("/health")
    @Operation(summary = "Health check endpoint")
    public ResponseEntity<ApiResponse<String>> healthCheck() {
        return ResponseEntity.ok(new ApiResponse<>(true, "Service is running", "OK"));
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Get dashboard statistics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboardStats() {
        Map<String, Object> stats = apiService.getDashboardStats();
        return ResponseEntity.ok(new ApiResponse<>(true, "Dashboard stats retrieved", stats));
    }

    @GetMapping("/testcases")
    @Operation(summary = "Get all test cases")
    public ResponseEntity<ApiResponse<List<TestCase>>> getAllTestCases() {
        List<TestCase> testCases = apiService.getAllTestCases();
        return ResponseEntity.ok(new ApiResponse<>(true, "Test cases retrieved", testCases));
    }

    @GetMapping("/testcases/{id}")
    @Operation(summary = "Get test case by ID")
    public ResponseEntity<ApiResponse<TestCase>> getTestCase(@PathVariable Long id) {
        TestCase testCase = testCaseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Test case not found"));
        return ResponseEntity.ok(new ApiResponse<>(true, "Test case retrieved", testCase));
    }

    @PostMapping("/execute")
    @Operation(summary = "Execute a single test case")
    public ResponseEntity<ApiResponse<Execution>> executeTestCase(@RequestBody TestExecutionRequest request) {
        try {
            Execution execution = apiService.executeTestCase(request);
            return ResponseEntity.ok(new ApiResponse<>(true, "Test case executed successfully", execution));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(false, e.getMessage(), "EXECUTION_ERROR"));
        }
    }

    @PostMapping("/suites/execute")
    @Operation(summary = "Execute a test suite")
    public ResponseEntity<ApiResponse<Map<String, Object>>> executeTestSuite(
            @RequestBody TestSuiteExecutionRequest request) {
        try {
            Map<String, Object> result = apiService.executeTestSuite(request);
            return ResponseEntity.ok(new ApiResponse<>(true, "Test suite executed successfully", result));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(false, e.getMessage(), "SUITE_EXECUTION_ERROR"));
        }
    }

    @GetMapping("/suites")
    @Operation(summary = "Get all test suites")
    public ResponseEntity<ApiResponse<List<TestSuite>>> getAllTestSuites() {
        List<TestSuite> suites = apiService.getAllTestSuites();
        return ResponseEntity.ok(new ApiResponse<>(true, "Test suites retrieved", suites));
    }

    @GetMapping("/suites/{id}")
    @Operation(summary = "Get test suite by ID")
    public ResponseEntity<ApiResponse<TestSuite>> getTestSuite(@PathVariable Long id) {
        TestSuite suite = testSuiteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Test suite not found"));
        return ResponseEntity.ok(new ApiResponse<>(true, "Test suite retrieved", suite));
    }

    @GetMapping("/executions/testcase/{testCaseId}")
    @Operation(summary = "Get execution history for a test case")
    public ResponseEntity<ApiResponse<List<Execution>>> getExecutionsByTestCase(@PathVariable Long testCaseId) {
        TestCase testCase = testCaseRepository.findById(testCaseId)
                .orElseThrow(() -> new IllegalArgumentException("Test case not found"));
        List<Execution> executions = executionRepository.findByTestCaseOrderByExecutedOnDesc(testCase);
        return ResponseEntity.ok(new ApiResponse<>(true, "Executions retrieved", executions));
    }
}