package com.qa.testmanagement.controller;

import com.qa.testmanagement.model.Execution;
import com.qa.testmanagement.model.TestCase;
import com.qa.testmanagement.model.TestStatus;
import com.qa.testmanagement.repository.ExecutionRepository;
import com.qa.testmanagement.repository.TestCaseRepository;
import com.qa.testmanagement.service.TestCaseUpdateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/executions")
public class TestExecutionController {

    @Autowired
    private ExecutionRepository executionRepository;

    @Autowired
    private TestCaseRepository testCaseRepository;

    @Autowired
    private TestCaseUpdateService updateService;

    @GetMapping("/history/{testCaseId}")
    public String executionHistory(@PathVariable Long testCaseId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model,
            RedirectAttributes redirectAttributes) {

        TestCase testCase = testCaseRepository.findById(testCaseId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid test case Id:" + testCaseId));

        Pageable pageable = PageRequest.of(page, size, Sort.by("executedOn").descending());
        Page<Execution> executionPage = executionRepository.findByTestCase(testCase, pageable);

        // Calculate statistics
        List<Execution> allExecutions = executionRepository.findByTestCaseOrderByExecutedOnDesc(testCase);

        // Status distribution for this test case
        Map<String, Long> statusCounts = allExecutions.stream()
                .collect(Collectors.groupingBy(
                        e -> e.getStatus().getDisplayName(),
                        Collectors.counting()));

        // Last 7 days execution trend
        Map<String, Long> last7Days = new HashMap<>();
        LocalDate today = LocalDate.now();
        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            long count = allExecutions.stream()
                    .filter(e -> e.getExecutedOn().toLocalDate().equals(date))
                    .count();
            last7Days.put(date.format(DateTimeFormatter.ofPattern("MMM dd")), count);
        }

        // Calculate pass rate for this test case
        long totalExecutions = allExecutions.size();
        long passedExecutions = allExecutions.stream()
                .filter(e -> e.getStatus() == TestStatus.PASS)
                .count();
        double passRate = totalExecutions > 0 ? (passedExecutions * 100.0 / totalExecutions) : 0;

        // Get latest execution
        Execution latestExecution = allExecutions.isEmpty() ? null : allExecutions.get(0);

        model.addAttribute("testCase", testCase);
        model.addAttribute("executions", executionPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", executionPage.getTotalPages());
        model.addAttribute("totalItems", executionPage.getTotalElements());
        model.addAttribute("statusCounts", statusCounts);
        model.addAttribute("last7Days", last7Days);
        model.addAttribute("totalExecutions", totalExecutions);
        model.addAttribute("passRate", String.format("%.1f", passRate));
        model.addAttribute("latestExecution", latestExecution);

        return "execution-history";
    }

    @GetMapping("/recent")
    public String recentExecutions(Model model) {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("executedOn").descending());
        List<Execution> recentExecutions = executionRepository.findAll(pageable).getContent();

        model.addAttribute("executions", recentExecutions);
        return "fragments/recent-executions :: recentExecutions";
    }

    @GetMapping("/analytics")
    public String executionAnalytics(Model model) {
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
        LocalDateTime monthAgo = LocalDateTime.now().minusDays(30);
        LocalDateTime quarterAgo = LocalDateTime.now().minusDays(90);

        // Weekly stats
        long weeklyPassed = executionRepository.countByStatusAndExecutedOnBetween(
                TestStatus.PASS, weekAgo, LocalDateTime.now());
        long weeklyFailed = executionRepository.countByStatusAndExecutedOnBetween(
                TestStatus.FAIL, weekAgo, LocalDateTime.now());
        long weeklyBlocked = executionRepository.countByStatusAndExecutedOnBetween(
                TestStatus.BLOCKED, weekAgo, LocalDateTime.now());
        long weeklyTotal = executionRepository.countByExecutedOnAfter(weekAgo);

        // Monthly stats
        long monthlyTotal = executionRepository.countByExecutedOnAfter(monthAgo);
        long monthlyPassed = executionRepository.countByStatusAndExecutedOnBetween(
                TestStatus.PASS, monthAgo, LocalDateTime.now());

        // Quarterly stats (using quarterAgo)
        long quarterlyTotal = executionRepository.countByExecutedOnAfter(quarterAgo);
        long quarterlyPassed = executionRepository.countByStatusAndExecutedOnBetween(
                TestStatus.PASS, quarterAgo, LocalDateTime.now());
        long quarterlyFailed = executionRepository.countByStatusAndExecutedOnBetween(
                TestStatus.FAIL, quarterAgo, LocalDateTime.now());

        // Status distribution all time
        List<Object[]> statusDistribution = executionRepository.getExecutionStatusCount();

        // Daily trend for last 30 days
        List<Object[]> dailyTrend = executionRepository.getDailyExecutionCount(
                LocalDateTime.now().minusDays(30), LocalDateTime.now());

        // Top executors
        List<Object[]> topExecutors = executionRepository.getTopExecutors(
                PageRequest.of(0, 5));

        // Test case execution frequency
        List<Object[]> mostExecutedTests = getMostExecutedTests();

        // Calculate trends
        double weeklyPassRate = weeklyTotal > 0 ? (weeklyPassed * 100.0 / weeklyTotal) : 0;
        double monthlyPassRate = monthlyTotal > 0 ? (monthlyPassed * 100.0 / monthlyTotal) : 0;
        double quarterlyPassRate = quarterlyTotal > 0 ? (quarterlyPassed * 100.0 / quarterlyTotal) : 0;

        model.addAttribute("weeklyPassed", weeklyPassed);
        model.addAttribute("weeklyFailed", weeklyFailed);
        model.addAttribute("weeklyBlocked", weeklyBlocked);
        model.addAttribute("weeklyTotal", weeklyTotal);
        model.addAttribute("weeklyPassRate", String.format("%.1f", weeklyPassRate));

        model.addAttribute("monthlyTotal", monthlyTotal);
        model.addAttribute("monthlyPassed", monthlyPassed);
        model.addAttribute("monthlyPassRate", String.format("%.1f", monthlyPassRate));

        model.addAttribute("quarterlyTotal", quarterlyTotal);
        model.addAttribute("quarterlyPassed", quarterlyPassed);
        model.addAttribute("quarterlyFailed", quarterlyFailed);
        model.addAttribute("quarterlyPassRate", String.format("%.1f", quarterlyPassRate));

        model.addAttribute("statusDistribution", statusDistribution);
        model.addAttribute("dailyTrend", dailyTrend);
        model.addAttribute("topExecutors", topExecutors);
        model.addAttribute("mostExecutedTests", mostExecutedTests);

        // Send real-time analytics update via WebSocket (using the service)
        sendAnalyticsUpdate();

        return "execution-analytics";
    }

    @GetMapping("/export/{testCaseId}")
    @ResponseBody
    public List<Execution> exportExecutionHistory(@PathVariable Long testCaseId) {
        TestCase testCase = testCaseRepository.findById(testCaseId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid test case Id:" + testCaseId));
        return executionRepository.findByTestCaseOrderByExecutedOnDesc(testCase);
    }

    @DeleteMapping("/{executionId}")
    @ResponseBody
    public Map<String, Object> deleteExecution(@PathVariable Long executionId) {
        Map<String, Object> response = new HashMap<>();
        try {
            Execution execution = executionRepository.findById(executionId)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid execution Id"));

            TestCase testCase = execution.getTestCase();

            executionRepository.delete(execution);

            // Update test case status based on latest execution
            List<Execution> latestExecutions = executionRepository
                    .findByTestCaseOrderByExecutedOnDesc(testCase);

            if (!latestExecutions.isEmpty()) {
                testCase.setStatus(latestExecutions.get(0).getStatus());
            } else {
                testCase.setStatus(TestStatus.PENDING);
            }
            testCaseRepository.save(testCase);

            // Send real-time updates via the service
            updateService.sendTestCaseUpdate(testCase);
            updateService.sendDashboardUpdate();

            // FIXED: Send execution update with all 9 parameters including actualResult and
            // expectedResultVerified
            TestCaseUpdateService.ExecutionDTO executionDTO = new TestCaseUpdateService.ExecutionDTO(
                    execution.getId(),
                    testCase.getId(),
                    testCase.getTestCaseName(),
                    execution.getStatus().getDisplayName(),
                    execution.getExecutedBy(),
                    execution.getExecutedOn(),
                    execution.getRemarks(),
                    execution.getActualResult(),
                    execution.getExpectedResultVerified());
            updateService.sendExecutionUpdate(executionDTO);

            response.put("status", "success");
            response.put("message", "Execution deleted successfully");
            response.put("timestamp", LocalDateTime.now().toString());

        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
        }
        return response;
    }

    @PostMapping("/bulk-update")
    @ResponseBody
    public Map<String, Object> bulkUpdateExecutions(@RequestBody List<Long> executionIds,
            @RequestParam TestStatus status) {
        Map<String, Object> response = new HashMap<>();
        try {
            List<Execution> executions = executionRepository.findAllById(executionIds);
            int updated = 0;

            for (Execution execution : executions) {
                execution.setStatus(status);
                executionRepository.save(execution);

                TestCase testCase = execution.getTestCase();
                testCase.setStatus(status);
                testCase.setExecutedOn(LocalDateTime.now());
                testCaseRepository.save(testCase);

                // Send real-time update for each test case
                updateService.sendTestCaseUpdate(testCase);
                updated++;
            }

            // Send dashboard update
            updateService.sendDashboardUpdate();

            response.put("status", "success");
            response.put("message", updated + " executions updated successfully");
            response.put("updated", updated);

        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
        }
        return response;
    }

    @GetMapping("/statistics")
    @ResponseBody
    public Map<String, Object> getExecutionStatistics() {
        Map<String, Object> stats = new HashMap<>();

        // Overall statistics
        long totalExecutions = executionRepository.count();
        long passedExecutions = executionRepository.countByStatusAndExecutedOnBetween(
                TestStatus.PASS, LocalDateTime.MIN, LocalDateTime.MAX);
        long failedExecutions = executionRepository.countByStatusAndExecutedOnBetween(
                TestStatus.FAIL, LocalDateTime.MIN, LocalDateTime.MAX);

        stats.put("totalExecutions", totalExecutions);
        stats.put("passedExecutions", passedExecutions);
        stats.put("failedExecutions", failedExecutions);
        stats.put("overallPassRate", totalExecutions > 0 ? (passedExecutions * 100.0 / totalExecutions) : 0);

        // Today's statistics
        LocalDateTime today = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        long todayExecutions = executionRepository.countByExecutedOnAfter(today);
        long todayPassed = executionRepository.countByStatusAndExecutedOnBetween(
                TestStatus.PASS, today, LocalDateTime.now());

        stats.put("todayExecutions", todayExecutions);
        stats.put("todayPassed", todayPassed);
        stats.put("todayPassRate", todayExecutions > 0 ? (todayPassed * 100.0 / todayExecutions) : 0);

        // Weekly statistics
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
        long weekExecutions = executionRepository.countByExecutedOnAfter(weekAgo);
        long weekPassed = executionRepository.countByStatusAndExecutedOnBetween(
                TestStatus.PASS, weekAgo, LocalDateTime.now());

        stats.put("weekExecutions", weekExecutions);
        stats.put("weekPassed", weekPassed);
        stats.put("weekPassRate", weekExecutions > 0 ? (weekPassed * 100.0 / weekExecutions) : 0);

        // Monthly statistics
        LocalDateTime monthAgo = LocalDateTime.now().minusDays(30);
        long monthExecutions = executionRepository.countByExecutedOnAfter(monthAgo);
        stats.put("avgDailyExecutions", String.format("%.1f", monthExecutions / 30.0));

        // Quarterly statistics
        LocalDateTime quarterAgo = LocalDateTime.now().minusDays(90);
        long quarterExecutions = executionRepository.countByExecutedOnAfter(quarterAgo);
        long quarterPassed = executionRepository.countByStatusAndExecutedOnBetween(
                TestStatus.PASS, quarterAgo, LocalDateTime.now());

        stats.put("quarterExecutions", quarterExecutions);
        stats.put("quarterPassed", quarterPassed);
        stats.put("quarterPassRate", quarterExecutions > 0 ? (quarterPassed * 100.0 / quarterExecutions) : 0);

        return stats;
    }

    @PostMapping("/{executionId}/remark")
    @ResponseBody
    public Map<String, String> addRemark(@PathVariable Long executionId,
            @RequestParam String remark) {
        Map<String, String> response = new HashMap<>();
        try {
            Execution execution = executionRepository.findById(executionId)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid execution Id"));

            execution.setRemarks(remark);
            executionRepository.save(execution);

            response.put("status", "success");
            response.put("message", "Remark added successfully");

        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
        }
        return response;
    }

    @GetMapping("/testcase/{testCaseId}/trend")
    @ResponseBody
    public Map<String, Object> getTestCaseExecutionTrend(@PathVariable Long testCaseId) {
        Map<String, Object> trend = new HashMap<>();

        TestCase testCase = testCaseRepository.findById(testCaseId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid test case Id"));

        List<Execution> executions = executionRepository.findByTestCaseOrderByExecutedOnDesc(testCase);

        // Group by date for the last 30 days
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        Map<String, Long> dailyExecutions = executions.stream()
                .filter(e -> e.getExecutedOn().isAfter(thirtyDaysAgo))
                .collect(Collectors.groupingBy(
                        e -> e.getExecutedOn().toLocalDate().toString(),
                        Collectors.counting()));

        // Status progression
        List<Map<String, String>> progression = executions.stream()
                .limit(20)
                .map(e -> {
                    Map<String, String> point = new HashMap<>();
                    point.put("date", e.getExecutedOn().format(DateTimeFormatter.ISO_LOCAL_DATE));
                    point.put("status", e.getStatus().getDisplayName());
                    point.put("executedBy", e.getExecutedBy());
                    return point;
                })
                .collect(Collectors.toList());

        trend.put("dailyExecutions", dailyExecutions);
        trend.put("progression", progression);
        trend.put("totalExecutions", executions.size());

        return trend;
    }

    private List<Object[]> getMostExecutedTests() {
        return executionRepository.findAll(PageRequest.of(0, 5, Sort.by("executedOn").descending()))
                .stream()
                .collect(Collectors.groupingBy(
                        e -> e.getTestCase().getId(),
                        Collectors.counting()))
                .entrySet()
                .stream()
                .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
                .limit(5)
                .map(entry -> {
                    TestCase tc = testCaseRepository.findById(entry.getKey()).orElse(null);
                    return new Object[] {
                            tc != null ? tc.getTestCaseName() : "Unknown",
                            entry.getValue()
                    };
                })
                .collect(Collectors.toList());
    }

    private void sendAnalyticsUpdate() {
        // Create analytics DTO and send via WebSocket
        Map<String, Object> analyticsUpdate = new HashMap<>();

        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
        LocalDateTime monthAgo = LocalDateTime.now().minusDays(30);
        LocalDateTime quarterAgo = LocalDateTime.now().minusDays(90);

        long weeklyTotal = executionRepository.countByExecutedOnAfter(weekAgo);
        long weeklyPassed = executionRepository.countByStatusAndExecutedOnBetween(
                TestStatus.PASS, weekAgo, LocalDateTime.now());

        long monthlyTotal = executionRepository.countByExecutedOnAfter(monthAgo);
        long monthlyPassed = executionRepository.countByStatusAndExecutedOnBetween(
                TestStatus.PASS, monthAgo, LocalDateTime.now());

        long quarterlyTotal = executionRepository.countByExecutedOnAfter(quarterAgo);
        long quarterlyPassed = executionRepository.countByStatusAndExecutedOnBetween(
                TestStatus.PASS, quarterAgo, LocalDateTime.now());

        analyticsUpdate.put("type", "ANALYTICS_UPDATE");
        analyticsUpdate.put("weeklyTotal", weeklyTotal);
        analyticsUpdate.put("weeklyPassed", weeklyPassed);
        analyticsUpdate.put("weeklyPassRate", weeklyTotal > 0 ? (weeklyPassed * 100.0 / weeklyTotal) : 0);

        analyticsUpdate.put("monthlyTotal", monthlyTotal);
        analyticsUpdate.put("monthlyPassed", monthlyPassed);
        analyticsUpdate.put("monthlyPassRate", monthlyTotal > 0 ? (monthlyPassed * 100.0 / monthlyTotal) : 0);

        analyticsUpdate.put("quarterlyTotal", quarterlyTotal);
        analyticsUpdate.put("quarterlyPassed", quarterlyPassed);
        analyticsUpdate.put("quarterlyPassRate", quarterlyTotal > 0 ? (quarterlyPassed * 100.0 / quarterlyTotal) : 0);

        analyticsUpdate.put("timestamp", LocalDateTime.now().toString());

        // FIXED: Send the analytics update via WebSocket with all 9 parameters
        // Since this is just an analytics update and not a real execution, we can use
        // empty strings for the new fields
        updateService.sendExecutionUpdate(
                new TestCaseUpdateService.ExecutionDTO(
                        null, null, null, null, null, null, null, null, null));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public String handleIllegalArgumentException(IllegalArgumentException e,
            RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        return "redirect:/testcases/list";
    }
}