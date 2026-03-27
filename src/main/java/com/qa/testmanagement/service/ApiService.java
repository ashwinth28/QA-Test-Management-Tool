package com.qa.testmanagement.service;

import com.qa.testmanagement.model.*;
import com.qa.testmanagement.model.api.TestExecutionRequest;
import com.qa.testmanagement.model.api.TestSuiteExecutionRequest;
import com.qa.testmanagement.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ApiService {

    @Autowired
    private TestCaseRepository testCaseRepository;

    @Autowired
    private ExecutionRepository executionRepository;

    @Autowired
    private TestSuiteRepository testSuiteRepository;

    @Autowired
    private SuiteExecutionRepository suiteExecutionRepository;

    @Autowired
    private TestCaseUpdateService updateService;

    @Transactional
    public Execution executeTestCase(TestExecutionRequest request) {
        TestCase testCase = testCaseRepository.findById(request.getTestCaseId())
                .orElseThrow(() -> new IllegalArgumentException("Test case not found"));

        Execution execution = new Execution();
        execution.setTestCase(testCase);
        execution.setExecutedBy(request.getExecutedBy());
        execution.setStatus(request.getStatus());
        execution.setActualResult(request.getActualResult());
        execution.setRemarks(request.getRemarks());
        execution.setExecutedOn(LocalDateTime.now());
        execution.setExpectedResultVerified("API execution - Actual: " + request.getActualResult());

        executionRepository.save(execution);

        // Update test case
        testCase.setStatus(request.getStatus());
        testCase.setExecutedOn(LocalDateTime.now());
        testCase.setActualResult(request.getActualResult());
        testCaseRepository.save(testCase);

        // Send real-time updates
        updateService.sendTestCaseUpdate(testCase);
        updateService.sendDashboardUpdate();

        return execution;
    }

    @Transactional
    public Map<String, Object> executeTestSuite(TestSuiteExecutionRequest request) {
        TestSuite testSuite = testSuiteRepository.findById(request.getSuiteId())
                .orElseThrow(() -> new IllegalArgumentException("Test suite not found"));

        List<TestCase> testCases = testSuite.getTestCases();

        int passed = 0, failed = 0, blocked = 0, pending = 0;

        for (TestCase testCase : testCases) {
            // Create execution record for each test case
            Execution execution = new Execution();
            execution.setTestCase(testCase);
            execution.setExecutedBy(request.getExecutedBy());
            execution.setStatus(testCase.getStatus());
            execution.setActualResult(testCase.getActualResult());
            execution.setExecutedOn(LocalDateTime.now());
            execution.setExpectedResultVerified("API suite execution");
            executionRepository.save(execution);

            switch (testCase.getStatus()) {
                case PASS:
                    passed++;
                    break;
                case FAIL:
                    failed++;
                    break;
                case BLOCKED:
                    blocked++;
                    break;
                default:
                    pending++;
            }
        }

        // Create suite execution record
        SuiteExecution suiteExecution = new SuiteExecution();
        suiteExecution.setTestSuite(testSuite);
        suiteExecution.setExecutedBy(request.getExecutedBy());
        suiteExecution.setExecutedOn(LocalDateTime.now());
        suiteExecution.setTotalTests(testCases.size());
        suiteExecution.setPassedTests(passed);
        suiteExecution.setFailedTests(failed);
        suiteExecution.setBlockedTests(blocked);
        suiteExecution.setPendingTests(pending);
        suiteExecutionRepository.save(suiteExecution);

        Map<String, Object> result = new HashMap<>();
        result.put("suiteId", testSuite.getId());
        result.put("suiteName", testSuite.getName());
        result.put("totalTests", testCases.size());
        result.put("passed", passed);
        result.put("failed", failed);
        result.put("blocked", blocked);
        result.put("pending", pending);

        // Update dashboard
        updateService.sendDashboardUpdate();

        return result;
    }

    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();

        long total = testCaseRepository.count();
        long passed = testCaseRepository.countByStatus(TestStatus.PASS);
        long failed = testCaseRepository.countByStatus(TestStatus.FAIL);
        long blocked = testCaseRepository.countByStatus(TestStatus.BLOCKED);
        long pending = testCaseRepository.countByStatus(TestStatus.PENDING);

        stats.put("totalTestCases", total);
        stats.put("passed", passed);
        stats.put("failed", failed);
        stats.put("blocked", blocked);
        stats.put("pending", pending);
        stats.put("passRate", total > 0 ? (passed * 100.0 / total) : 0);

        return stats;
    }

    public List<TestCase> getAllTestCases() {
        return testCaseRepository.findAll();
    }

    public List<TestSuite> getAllTestSuites() {
        return testSuiteRepository.findAll();
    }
}