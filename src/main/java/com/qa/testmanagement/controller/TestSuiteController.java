package com.qa.testmanagement.controller;

import com.qa.testmanagement.model.*;
import com.qa.testmanagement.repository.*;
import com.qa.testmanagement.service.TestCaseUpdateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/suites")
public class TestSuiteController {

    @Autowired
    private TestSuiteRepository testSuiteRepository;

    @Autowired
    private TestCaseRepository testCaseRepository;

    @Autowired
    private SuiteExecutionRepository suiteExecutionRepository;

    @Autowired
    private ExecutionRepository executionRepository;

    @Autowired
    private TestCaseUpdateService updateService;

    @GetMapping
    public String listSuites(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<TestSuite> suitePage = testSuiteRepository.findAll(pageable);

        model.addAttribute("suites", suitePage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", suitePage.getTotalPages());
        model.addAttribute("totalItems", suitePage.getTotalElements());

        return "suites/list";
    }

    @GetMapping("/new")
    public String createSuiteForm(Model model) {
        model.addAttribute("testSuite", new TestSuite());
        model.addAttribute("allTestCases", testCaseRepository.findAll());
        return "suites/form";
    }

    @GetMapping("/edit/{id}")
    public String editSuiteForm(@PathVariable Long id, Model model) {
        TestSuite testSuite = testSuiteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid suite Id:" + id));
        model.addAttribute("testSuite", testSuite);
        model.addAttribute("allTestCases", testCaseRepository.findAll());
        return "suites/form";
    }

    @PostMapping("/save")
    public String saveSuite(@ModelAttribute TestSuite testSuite,
            @RequestParam(required = false) List<Long> testCaseIds,
            RedirectAttributes redirectAttributes) {
        try {
            if (testCaseIds != null && !testCaseIds.isEmpty()) {
                List<TestCase> selectedTestCases = testCaseRepository.findAllById(testCaseIds);
                testSuite.setTestCases(selectedTestCases);
            } else {
                testSuite.setTestCases(List.of());
            }

            testSuiteRepository.save(testSuite);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Test suite saved successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Error saving test suite: " + e.getMessage());
        }
        return "redirect:/suites";
    }

    @GetMapping("/execute/{id}")
    public String executeSuitePage(@PathVariable Long id, Model model) {
        TestSuite testSuite = testSuiteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid suite Id:" + id));

        List<TestCase> testCases = testSuite.getTestCases();
        if (testCases.isEmpty()) {
            model.addAttribute("errorMessage", "This suite has no test cases to execute!");
            return "redirect:/suites";
        }

        model.addAttribute("testSuite", testSuite);
        model.addAttribute("testCases", testCases);
        model.addAttribute("statuses", TestStatus.values());

        return "suites/execute";
    }

    @PostMapping("/execute-all")
    public String executeAllTestCases(@RequestParam Long suiteId,
            @RequestParam Map<String, String> allParams,
            RedirectAttributes redirectAttributes) {
        try {
            TestSuite testSuite = testSuiteRepository.findById(suiteId)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid suite Id:" + suiteId)); // FIXED

            String currentUser = SecurityContextHolder.getContext().getAuthentication().getName();

            int passed = 0, failed = 0, blocked = 0, pending = 0;
            List<Execution> executions = new ArrayList<>();

            // Process each test case
            for (TestCase testCase : testSuite.getTestCases()) {
                Long testCaseId = testCase.getId();
                String statusKey = "status_" + testCaseId;
                String actualResultKey = "actualResult_" + testCaseId;
                String remarksKey = "remarks_" + testCaseId;
                String expectedResultKey = "expectedResultVerified_" + testCaseId;

                String statusStr = allParams.get(statusKey);
                String actualResult = allParams.get(actualResultKey);
                String remarks = allParams.get(remarksKey);
                String expectedResultVerified = allParams.get(expectedResultKey);

                if (statusStr != null && !statusStr.isEmpty()) {
                    TestStatus status = TestStatus.valueOf(statusStr);

                    // Create execution record
                    Execution execution = new Execution();
                    execution.setTestCase(testCase);
                    execution.setExecutedBy(currentUser);
                    execution.setExecutedOn(LocalDateTime.now());
                    execution.setStatus(status);
                    execution.setActualResult(actualResult != null ? actualResult : "");
                    execution.setRemarks(remarks != null ? remarks : "");
                    execution.setExpectedResultVerified(
                            expectedResultVerified != null ? expectedResultVerified : "Executed via suite");

                    executions.add(execution);

                    // Update test case status
                    testCase.setStatus(status);
                    testCase.setExecutedOn(LocalDateTime.now());
                    testCase.setActualResult(actualResult);
                    testCaseRepository.save(testCase);

                    // Count for suite stats
                    switch (status) {
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

                    // Send real-time update
                    updateService.sendTestCaseUpdate(testCase);
                }
            }

            // Save all executions
            executionRepository.saveAll(executions);

            // Create suite execution record
            SuiteExecution suiteExecution = new SuiteExecution();
            suiteExecution.setTestSuite(testSuite);
            suiteExecution.setExecutedBy(currentUser);
            suiteExecution.setExecutedOn(LocalDateTime.now());
            suiteExecution.setTotalTests(testSuite.getTestCases().size());
            suiteExecution.setPassedTests(passed);
            suiteExecution.setFailedTests(failed);
            suiteExecution.setBlockedTests(blocked);
            suiteExecution.setPendingTests(pending);
            suiteExecutionRepository.save(suiteExecution);

            // Update suite status
            if (passed == testSuite.getTestCases().size()) {
                testSuite.setStatus(TestSuiteStatus.PASSED);
            } else if (failed > 0 && passed == 0 && blocked == 0) {
                testSuite.setStatus(TestSuiteStatus.FAILED);
            } else if (passed > 0 || failed > 0 || blocked > 0) {
                testSuite.setStatus(TestSuiteStatus.PARTIAL);
            }
            testSuiteRepository.save(testSuite);

            // Update dashboard
            updateService.sendDashboardUpdate();

            redirectAttributes.addFlashAttribute("successMessage",
                    String.format("Suite executed: %d passed, %d failed, %d blocked, %d pending",
                            passed, failed, blocked, pending));

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Error executing suite: " + e.getMessage());
            e.printStackTrace();
        }

        return "redirect:/suites";
    }

    @GetMapping("/history/{id}")
    public String suiteHistory(@PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {
        TestSuite testSuite = testSuiteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid suite Id:" + id));

        Pageable pageable = PageRequest.of(page, size, Sort.by("executedOn").descending());
        Page<SuiteExecution> executions = suiteExecutionRepository.findByTestSuite(testSuite, pageable);

        model.addAttribute("testSuite", testSuite);
        model.addAttribute("executions", executions.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", executions.getTotalPages());
        model.addAttribute("totalItems", executions.getTotalElements());

        return "suites/history";
    }

    @PostMapping("/delete/{id}")
    public String deleteSuite(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            testSuiteRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("successMessage", "Test suite deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting test suite: " + e.getMessage());
        }
        return "redirect:/suites";
    }
}