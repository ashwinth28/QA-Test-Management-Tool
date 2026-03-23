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
import java.util.List;

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
    public String executeSuite(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            TestSuite testSuite = testSuiteRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid suite Id:" + id));

            List<TestCase> testCases = testSuite.getTestCases();
            if (testCases.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage",
                        "This suite has no test cases to execute!");
                return "redirect:/suites";
            }

            String currentUser = SecurityContextHolder.getContext().getAuthentication().getName();

            // Update suite status to RUNNING
            testSuite.setStatus(TestSuiteStatus.RUNNING);
            testSuite.setExecutedBy(currentUser);
            testSuite.setExecutedOn(LocalDateTime.now());
            testSuiteRepository.save(testSuite);

            // Create suite execution record
            SuiteExecution suiteExecution = new SuiteExecution();
            suiteExecution.setTestSuite(testSuite);
            suiteExecution.setExecutedBy(currentUser);
            suiteExecution.setTotalTests(testCases.size());

            long startTime = System.currentTimeMillis();

            int passed = 0, failed = 0, blocked = 0, pending = 0;
            // Execute each test case in the suite
            for (TestCase testCase : testCases) {
                // Create an execution record for the test case
                Execution execution = new Execution();
                execution.setTestCase(testCase);
                execution.setExecutedBy(currentUser);
                execution.setExecutedOn(LocalDateTime.now());
                execution.setStatus(testCase.getStatus()); // Keep existing status
                execution.setActualResult(testCase.getActualResult());

                executionRepository.save(execution);
                suiteExecution.getTestCaseResults().put(testCase.getId(),
                        testCase.getStatus().getDisplayName());

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

            long executionTime = System.currentTimeMillis() - startTime;

            // Update suite execution record
            suiteExecution.setPassedTests(passed);
            suiteExecution.setFailedTests(failed);
            suiteExecution.setBlockedTests(blocked);
            suiteExecution.setPendingTests(pending);
            suiteExecution.setExecutionTimeMs(executionTime);
            suiteExecutionRepository.save(suiteExecution);

            // Update suite status
            if (passed == testCases.size()) {
                testSuite.setStatus(TestSuiteStatus.PASSED);
            } else if (failed > 0 && passed == 0 && blocked == 0) {
                testSuite.setStatus(TestSuiteStatus.FAILED);
            } else if (passed > 0 || failed > 0 || blocked > 0) {
                testSuite.setStatus(TestSuiteStatus.PARTIAL);
            }
            testSuiteRepository.save(testSuite);

            // Send real-time updates
            updateService.sendDashboardUpdate();

            redirectAttributes.addFlashAttribute("successMessage",
                    String.format("Suite executed: %d passed, %d failed, %d blocked in %.2f seconds",
                            passed, failed, blocked, executionTime / 1000.0));

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Error executing suite: " + e.getMessage());
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