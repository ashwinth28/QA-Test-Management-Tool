package com.qa.testmanagement.controller;

import com.qa.testmanagement.model.Execution;
import com.qa.testmanagement.model.TestStatus;
import com.qa.testmanagement.repository.ExecutionRepository;
import com.qa.testmanagement.repository.TestCaseRepository;
import com.qa.testmanagement.service.TestCaseUpdateService;
import com.qa.testmanagement.service.ActiveUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class DashboardController {

        @Autowired
        private TestCaseRepository testCaseRepository;

        @Autowired
        private ExecutionRepository executionRepository;

        @Autowired
        private TestCaseUpdateService updateService;

        @Autowired
        private SimpMessagingTemplate messagingTemplate;

        @Autowired
        private ActiveUserService activeUserService; // ADD THIS LINE

        @GetMapping("/")
        public String index() {
                return "redirect:/dashboard";
        }

        @GetMapping("/dashboard")
        public String dashboard(Model model) {
                // Basic counts
                long totalTestCases = testCaseRepository.count();
                long passedTests = testCaseRepository.countByStatus(TestStatus.PASS);
                long failedTests = testCaseRepository.countByStatus(TestStatus.FAIL);
                long blockedTests = testCaseRepository.countByStatus(TestStatus.BLOCKED);
                long pendingTests = testCaseRepository.countByStatus(TestStatus.PENDING);

                // Calculate pass rate
                double passRate = totalTestCases > 0 ? (passedTests * 100.0 / totalTestCases) : 0;

                // Format to 1 decimal place
                String formattedPassRateS = String.format("%.1f", passRate);

                // Recent executions (last 7 days)
                LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
                long recentExecutions = executionRepository.countByExecutedOnAfter(weekAgo);

                // Status distribution for chart
                List<Object[]> statusCounts = testCaseRepository.getStatusCounts();
                Map<String, Long> statusMap = statusCounts.stream()
                                .collect(Collectors.toMap(
                                                arr -> ((TestStatus) arr[0]).getDisplayName(),
                                                arr -> (Long) arr[1]));

                // Priority distribution
                List<Object[]> priorityCounts = testCaseRepository.getPriorityDistribution();
                Map<String, Long> priorityMap = priorityCounts.stream()
                                .collect(Collectors.toMap(
                                                arr -> (String) arr[0],
                                                arr -> (Long) arr[1]));

                // Daily execution trend (last 7 days)
                LocalDateTime startDate = LocalDateTime.now().minusDays(7);
                List<Object[]> dailyExecutions = executionRepository.getDailyExecutionCount(
                                startDate, LocalDateTime.now());

                model.addAttribute("totalTestCases", totalTestCases);
                model.addAttribute("passedTests", passedTests);
                model.addAttribute("failedTests", failedTests);
                model.addAttribute("blockedTests", blockedTests);
                model.addAttribute("pendingTests", pendingTests);
                model.addAttribute("passRate", formattedPassRateS); // Use formatted value
                model.addAttribute("recentExecutions", recentExecutions);
                model.addAttribute("statusDistribution", statusMap);
                model.addAttribute("priorityDistribution", priorityMap);
                model.addAttribute("dailyExecutions", dailyExecutions);
                model.addAttribute("lastUpdated",
                                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                model.addAttribute("activeUsers", activeUserService.getActiveUserCount()); // ADD THIS LINE

                // Trigger a dashboard update via WebSocket (using the service)
                updateService.sendDashboardUpdate();

                return "dashboard";
        }

        @GetMapping("/dashboard/metrics")
        @ResponseBody
        public Map<String, Object> getDashboardMetrics() {
                Map<String, Object> metrics = new HashMap<>();

                // Basic metrics
                long total = testCaseRepository.count();
                long passed = testCaseRepository.countByStatus(TestStatus.PASS);
                long failed = testCaseRepository.countByStatus(TestStatus.FAIL);
                long blocked = testCaseRepository.countByStatus(TestStatus.BLOCKED);
                long pending = testCaseRepository.countByStatus(TestStatus.PENDING);
                long active = testCaseRepository.countByStatus(TestStatus.ACTIVE);
                long skipped = testCaseRepository.countByStatus(TestStatus.SKIPPED);

                // Round pass rate to 1 decimal place
                double passRate = total > 0 ? (passed * 100.0 / total) : 0;
                double roundedPassRate = Math.round(passRate * 10) / 10.0;

                metrics.put("total", total);
                metrics.put("passed", passed);
                metrics.put("failed", failed);
                metrics.put("blocked", blocked);
                metrics.put("pending", pending);
                metrics.put("active", active);
                metrics.put("skipped", skipped);
                metrics.put("passRate", roundedPassRate); // Use rounded value

                // Recent activity
                LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
                metrics.put("recentExecutions", executionRepository.countByExecutedOnAfter(weekAgo));

                // Status distribution
                List<Object[]> statusCounts = testCaseRepository.getStatusCounts();
                Map<String, Long> statusMap = new HashMap<>();
                for (Object[] row : statusCounts) {
                        statusMap.put(((TestStatus) row[0]).getDisplayName(), (Long) row[1]);
                }
                metrics.put("statusDistribution", statusMap);

                // Priority distribution
                List<Object[]> priorityCounts = testCaseRepository.getPriorityDistribution();
                Map<String, Long> priorityMap = new HashMap<>();
                for (Object[] row : priorityCounts) {
                        priorityMap.put((String) row[0], (Long) row[1]);
                }
                metrics.put("priorityDistribution", priorityMap);

                // Weekly trend
                LocalDateTime[] last7Days = getLast7DaysRange();
                List<Object[]> dailyExecutions = executionRepository.getDailyExecutionCount(
                                last7Days[0], last7Days[1]);
                metrics.put("dailyTrend", dailyExecutions);

                metrics.put("lastUpdated", LocalDateTime.now().toString());

                // Send real-time update via WebSocket (using the service)
                TestCaseUpdateService.DashboardDTO dashboardDTO = new TestCaseUpdateService.DashboardDTO(
                                total, passed, failed, blocked, pending, active, skipped,
                                total > 0 ? (passed * 100.0 / total) : 0,
                                statusMap);
                messagingTemplate.convertAndSend("/topic/dashboard-updates", dashboardDTO);

                return metrics;
        }

        @GetMapping("/dashboard/refresh")
        @ResponseBody
        public Map<String, String> refreshDashboard() {
                Map<String, String> response = new HashMap<>();
                try {
                        // Force a dashboard update via the service
                        updateService.sendDashboardUpdate();

                        response.put("status", "success");
                        response.put("message", "Dashboard refreshed successfully");
                        response.put("timestamp", LocalDateTime.now().toString());
                } catch (Exception e) {
                        response.put("status", "error");
                        response.put("message", "Error refreshing dashboard: " + e.getMessage());
                }
                return response;
        }

        @GetMapping("/dashboard/health")
        @ResponseBody
        public Map<String, Object> healthCheck() {
                Map<String, Object> health = new HashMap<>();
                health.put("status", "UP");
                health.put("timestamp", LocalDateTime.now().toString());

                // Check database connectivity
                try {
                        long count = testCaseRepository.count();
                        health.put("database", "CONNECTED");
                        health.put("totalTestCases", count);
                } catch (Exception e) {
                        health.put("database", "DISCONNECTED");
                        health.put("databaseError", e.getMessage());
                        health.put("status", "DEGRADED");
                }

                // Check WebSocket connectivity (via service)
                try {
                        updateService.sendDashboardUpdate();
                        health.put("websocket", "OPERATIONAL");
                } catch (Exception e) {
                        health.put("websocket", "DEGRADED");
                }

                return health;
        }

        // FIXED: This method now returns actual executions instead of top executors
        @GetMapping("/dashboard/widgets/recent-executions")
        public String getRecentExecutionsWidget(Model model) {
                Pageable pageable = PageRequest.of(0, 5, Sort.by("executedOn").descending());
                List<Execution> recentExecutions = executionRepository.findAll(pageable).getContent();
                model.addAttribute("executions", recentExecutions);
                return "fragments/recent-executions :: recentExecutions";
        }

        @GetMapping("/dashboard/widgets/status-summary")
        @ResponseBody
        public Map<String, Object> getStatusSummary() {
                Map<String, Object> summary = new HashMap<>();

                // Get counts by status
                for (TestStatus status : TestStatus.values()) {
                        long count = testCaseRepository.countByStatus(status);
                        summary.put(status.name().toLowerCase(), count);
                }

                // Calculate percentages
                long total = testCaseRepository.count();
                if (total > 0) {
                        for (TestStatus status : TestStatus.values()) {
                                long count = testCaseRepository.countByStatus(status);
                                double percentage = (count * 100.0 / total);
                                summary.put(status.name().toLowerCase() + "_percentage",
                                                Math.round(percentage * 10.0) / 10.0);
                        }
                }

                return summary;
        }

        private LocalDateTime[] getLast7DaysRange() {
                LocalDateTime endDate = LocalDateTime.now();
                LocalDateTime startDate = endDate.minusDays(7);
                return new LocalDateTime[] { startDate, endDate };
        }

        @GetMapping("/dashboard/broadcast")
        @ResponseBody
        public Map<String, String> broadcastMessage(@RequestParam String message) {
                Map<String, String> response = new HashMap<>();
                try {
                        // Use the service to broadcast a custom message
                        messagingTemplate.convertAndSend("/topic/notifications",
                                        Map.of(
                                                        "type", "info",
                                                        "message", message,
                                                        "timestamp", LocalDateTime.now().toString()));

                        response.put("status", "success");
                        response.put("message", "Broadcast sent successfully");
                } catch (Exception e) {
                        response.put("status", "error");
                        response.put("message", e.getMessage());
                }
                return response;
        }

        // Add this method to get active users count
        @GetMapping("/dashboard/active-users")
        @ResponseBody
        public Map<String, Object> getActiveUsers() {
                Map<String, Object> response = new HashMap<>();
                response.put("activeUsers", activeUserService.getActiveUserCount());
                response.put("timestamp", LocalDateTime.now().toString());
                return response;
        }
}