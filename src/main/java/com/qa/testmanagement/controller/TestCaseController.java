package com.qa.testmanagement.controller;

import com.qa.testmanagement.model.TestCase;
import com.qa.testmanagement.model.TestStatus;
import com.qa.testmanagement.model.Execution;
import com.qa.testmanagement.repository.TestCaseRepository;
import com.qa.testmanagement.repository.ExecutionRepository;
import com.qa.testmanagement.service.EmailService;
import com.qa.testmanagement.service.TestCaseUpdateService;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
@RequestMapping("/testcases")
public class TestCaseController {

    private static final Logger logger = LoggerFactory.getLogger(TestCaseController.class);

    @Autowired
    private TestCaseRepository testCaseRepository;

    @Autowired
    private ExecutionRepository executionRepository;

    @Autowired
    private TestCaseUpdateService updateService;

    @Autowired
    private EmailService emailService;

    @ModelAttribute("allStatuses")
    public List<TestStatus> populateStatuses() {
        return Arrays.asList(TestStatus.values());
    }

    @ModelAttribute("priorities")
    public List<String> populatePriorities() {
        return Arrays.asList("High", "Medium", "Low", "Critical");
    }

    @ModelAttribute("testTypes")
    public List<String> populateTestTypes() {
        return Arrays.asList("Functional", "Regression", "Integration", "Smoke", "Performance", "Security");
    }

    @GetMapping("/new")
    public String createTestCaseForm(Model model) {
        model.addAttribute("testCase", new TestCase());
        model.addAttribute("action", "Create");
        return "create-testcase";
    }

    @GetMapping("/edit/{id}")
    public String editTestCaseForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        TestCase testCase = testCaseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid test case Id:" + id));
        model.addAttribute("testCase", testCase);
        model.addAttribute("action", "Edit");
        return "create-testcase";
    }

    @PostMapping("/save")
    public String saveTestCase(@Valid @ModelAttribute TestCase testCase,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (testCase.getId() == null && testCaseRepository.existsByTestCaseId(testCase.getTestCaseId())) {
            result.rejectValue("testCaseId", "error.testCase", "Test Case ID already exists");
        }

        if (result.hasErrors()) {
            model.addAttribute("action", testCase.getId() == null ? "Create" : "Edit");
            return "create-testcase";
        }

        try {
            if (testCase.getId() == null) {
                testCase.setCreatedOn(LocalDateTime.now());
                testCase.setStatus(TestStatus.PENDING);
            } else {
                TestCase existing = testCaseRepository.findById(testCase.getId())
                        .orElseThrow(() -> new IllegalArgumentException("Invalid test case Id"));
                testCase.setCreatedOn(existing.getCreatedOn());
            }

            testCaseRepository.save(testCase);
            updateService.sendTestCaseUpdate(testCase);
            updateService.sendDashboardUpdate();

            redirectAttributes.addFlashAttribute("successMessage",
                    "Test case " + (testCase.getId() == null ? "created" : "updated") + " successfully!");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error saving test case: " + e.getMessage());
            return "redirect:/testcases/new";
        }

        return "redirect:/testcases/list";
    }

    @GetMapping("/list")
    public String listTestCases(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) TestStatus status,
            Model model) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<TestCase> testCasePage;

        if (keyword != null && !keyword.isEmpty()) {
            testCasePage = testCaseRepository.search(keyword, pageable);
            model.addAttribute("keyword", keyword);
        } else if (status != null) {
            testCasePage = testCaseRepository.findByStatus(status, pageable);
            model.addAttribute("selectedStatus", status);
        } else {
            testCasePage = testCaseRepository.findAll(pageable);
        }

        // Get status counts for the stats bar
        long passedCount = testCaseRepository.countByStatus(TestStatus.PASS);
        long failedCount = testCaseRepository.countByStatus(TestStatus.FAIL);
        long blockedCount = testCaseRepository.countByStatus(TestStatus.BLOCKED);
        long pendingCount = testCaseRepository.countByStatus(TestStatus.PENDING);
        long totalCount = testCaseRepository.count();

        // Calculate pass rate
        double passRate = totalCount > 0 ? (passedCount * 100.0 / totalCount) : 0;

        model.addAttribute("testcases", testCasePage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", testCasePage.getTotalPages());
        model.addAttribute("totalItems", testCasePage.getTotalElements());
        model.addAttribute("pageSize", size);
        model.addAttribute("statuses", TestStatus.values());

        // Add stats to model
        model.addAttribute("passedCount", passedCount);
        model.addAttribute("failedCount", failedCount);
        model.addAttribute("blockedCount", blockedCount);
        model.addAttribute("pendingCount", pendingCount);
        model.addAttribute("passRate", String.format("%.1f", passRate));

        return "testcase-list";
    }

    @GetMapping("/execute/{id}")
    public String executeTestCaseForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        TestCase testCase = testCaseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid test case Id:" + id));

        model.addAttribute("testcase", testCase);
        model.addAttribute("statuses", TestStatus.values());

        return "execute-testcase";
    }

    // FIXED: Updated saveExecution with correct mandatory fields
    @PostMapping("/execute/save")
    public String saveExecution(
            @RequestParam(value = "testCaseId", required = false) Long testCaseId,
            @RequestParam(value = "executedBy", required = false) String executedBy,
            @RequestParam(value = "actualResult", required = false) String actualResult,
            @RequestParam(value = "status", required = false) String statusStr,
            @RequestParam(value = "remarks", required = false) String remarks,
            RedirectAttributes redirectAttributes) {

        // Debug logging
        logger.info("========== SAVE EXECUTION DEBUG ==========");
        logger.info("testCaseId: {}", testCaseId);
        logger.info("executedBy: {}", executedBy);
        logger.info("actualResult: {}", actualResult);
        logger.info("statusStr: {}", statusStr);
        logger.info("remarks: {}", remarks);
        logger.info("==========================================");

        // Validate required fields
        List<String> missingFields = new ArrayList<>();

        if (testCaseId == null) {
            missingFields.add("Test Case ID");
            logger.error("testCaseId is NULL!");
        }
        if (executedBy == null || executedBy.trim().isEmpty()) {
            missingFields.add("Executed By");
            logger.error("executedBy is NULL or empty!");
        }
        if (actualResult == null || actualResult.trim().isEmpty()) {
            missingFields.add("Actual Result");
            logger.error("actualResult is NULL or empty!");
        }
        if (statusStr == null || statusStr.trim().isEmpty()) {
            missingFields.add("Status");
            logger.error("statusStr is NULL or empty!");
        }

        if (!missingFields.isEmpty()) {
            String errorMsg = "Please fill all required fields: " + String.join(", ", missingFields) + " are mandatory";
            logger.error(errorMsg);
            redirectAttributes.addFlashAttribute("errorMessage", errorMsg);
            return "redirect:/testcases/execute/" + (testCaseId != null ? testCaseId : "");
        }

        try {
            // Convert status string to enum
            TestStatus status = TestStatus.valueOf(statusStr);
            logger.info("Converted status to enum: {}", status);

            // Fetch the test case
            TestCase testCase = testCaseRepository.findById(testCaseId)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid test case Id: " + testCaseId));

            logger.info("Found test case with ID: {}, Name: {}", testCase.getId(), testCase.getTestCaseName());

            // Create and save execution
            Execution execution = new Execution();
            execution.setTestCase(testCase);
            execution.setExecutedBy(executedBy);
            execution.setStatus(status);
            execution.setRemarks(remarks != null ? remarks : "");
            execution.setActualResult(actualResult);
            execution.setExpectedResultVerified("Executed: " + actualResult);
            execution.setExecutedOn(LocalDateTime.now());

            logger.info("Saving execution with testCase ID: {}", execution.getTestCase().getId());

            executionRepository.save(execution);
            logger.info("Execution saved successfully with ID: {}", execution.getId());

            // Email notification after execution is saved
            emailService.sendExecutionNotification(testCase, execution);

            // Update test case status and actual result
            testCase.setStatus(status);
            testCase.setExecutedOn(LocalDateTime.now());
            testCase.setActualResult(actualResult);
            testCaseRepository.save(testCase);
            logger.info("Test case updated with new status: {}", status);

            // Send real-time updates
            updateService.sendTestCaseUpdate(testCase);
            updateService.sendDashboardUpdate();

            redirectAttributes.addFlashAttribute("successMessage",
                    "✅ Execution saved successfully! Status: " + status.getDisplayName());

        } catch (IllegalArgumentException e) {
            logger.error("Invalid status value: {}", statusStr, e);
            redirectAttributes.addFlashAttribute("errorMessage", "❌ Invalid status value: " + statusStr);
            return "redirect:/testcases/execute/" + testCaseId;
        } catch (Exception e) {
            logger.error("Error saving execution", e);
            redirectAttributes.addFlashAttribute("errorMessage", "❌ Error saving execution: " + e.getMessage());
            e.printStackTrace();
            return "redirect:/testcases/execute/" + testCaseId;
        }

        return "redirect:/testcases/list";
    }

    @PostMapping("/status/{id}")
    @ResponseBody
    public TestCase quickStatusUpdatePost(@PathVariable Long id, @RequestParam TestStatus status) {
        return quickStatusUpdate(id, status);
    }

    @GetMapping("/status/{id}")
    @ResponseBody
    public TestCase quickStatusUpdateGet(@PathVariable Long id, @RequestParam TestStatus status) {
        return quickStatusUpdate(id, status);
    }

    private TestCase quickStatusUpdate(Long id, TestStatus status) {
        TestCase testCase = testCaseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid test case Id:" + id));

        testCase.setStatus(status);
        testCase.setExecutedOn(LocalDateTime.now());
        testCaseRepository.save(testCase);

        updateService.sendTestCaseUpdate(testCase);
        updateService.sendDashboardUpdate();

        return testCase;
    }

    @PostMapping("/delete/{id}")
    public String deleteTestCasePost(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        return deleteTestCase(id, redirectAttributes);
    }

    @GetMapping("/delete/{id}")
    public String deleteTestCaseGet(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        return deleteTestCase(id, redirectAttributes);
    }

    private String deleteTestCase(Long id, RedirectAttributes redirectAttributes) {
        try {
            TestCase testCase = testCaseRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid test case Id:" + id));

            List<Execution> executions = executionRepository.findByTestCaseOrderByExecutedOnDesc(testCase);
            if (executions != null && !executions.isEmpty()) {
                executionRepository.deleteAll(executions);
            }

            testCaseRepository.delete(testCase);
            updateService.sendDashboardUpdate();
            redirectAttributes.addFlashAttribute("successMessage", "Test case deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting test case: " + e.getMessage());
        }
        return "redirect:/testcases/list";
    }

    @GetMapping("/view/{id}")
    public String viewTestCase(@PathVariable Long id, Model model) {
        TestCase testCase = testCaseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid test case Id:" + id));

        List<Execution> executions = executionRepository.findByTestCaseOrderByExecutedOnDesc(testCase);

        model.addAttribute("testcase", testCase);
        model.addAttribute("executions", executions);

        return "view-testcase";
    }

    @GetMapping("/export")
    @ResponseBody
    public void exportAllTestCases(HttpServletResponse response) throws IOException {
        // Set response headers
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=all_testcases_" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".csv");

        // Get all test cases (unpaged)
        List<TestCase> allTestCases = testCaseRepository.findAll(Sort.by("id").descending());

        // Create CSV writer
        PrintWriter writer = response.getWriter();

        // Write CSV header
        writer.println("ID,Test Case ID,Name,Priority,Type,Status");

        // Write data rows
        for (TestCase tc : allTestCases) {
            writer.println(String.format("%d,%s,%s,%s,%s,%s",
                    tc.getId(),
                    escapeCsv(tc.getTestCaseId()),
                    escapeCsv(tc.getTestCaseName()),
                    escapeCsv(tc.getPriority()),
                    escapeCsv(tc.getTestType()),
                    escapeCsv(tc.getStatus().getDisplayName())));
        }

        writer.flush();
        writer.close();
    }

    // Helper method to escape CSV special characters
    private String escapeCsv(String value) {
        if (value == null)
            return "";
        // If the value contains commas, quotes, or newlines, wrap in quotes and escape
        // quotes
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    @GetMapping("/categories")
    @ResponseBody
    public List<String> getAllCategories() {
        return testCaseRepository.findAllCategories();
    }

    @GetMapping("/tags")
    @ResponseBody
    public List<String> getAllTags() {
        return testCaseRepository.findAllTags();
    }

    @GetMapping("/list/filtered")
    public String listTestCasesFiltered(@RequestParam(required = false) String category,
            @RequestParam(required = false) String tag,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<TestCase> testCasePage;

        if ((category != null && !category.isEmpty()) || (tag != null && !tag.isEmpty())) {
            testCasePage = testCaseRepository.findByCategoryAndTag(category, tag, pageable);
            model.addAttribute("selectedCategory", category);
            model.addAttribute("selectedTag", tag);
        } else {
            testCasePage = testCaseRepository.findAll(pageable);
        }

        model.addAttribute("testcases", testCasePage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", testCasePage.getTotalPages());
        model.addAttribute("totalItems", testCasePage.getTotalElements());
        model.addAttribute("pageSize", size);
        model.addAttribute("statuses", TestStatus.values());
        model.addAttribute("categories", testCaseRepository.findAllCategories());
        model.addAttribute("tags", testCaseRepository.findAllTags());

        return "testcase-list";
    }
}