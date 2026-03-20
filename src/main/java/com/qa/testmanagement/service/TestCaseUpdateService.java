package com.qa.testmanagement.service;

import com.qa.testmanagement.model.TestCase;
import com.qa.testmanagement.model.TestStatus;
import com.qa.testmanagement.repository.TestCaseRepository;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Service
public class TestCaseUpdateService {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private TestCaseRepository testCaseRepository;

    private static final String TESTCASE_TOPIC = "/topic/testcase-updates";
    private static final String DASHBOARD_TOPIC = "/topic/dashboard-updates";
    private static final String EXECUTION_TOPIC = "/topic/execution-updates";

    /**
     * Send updates for a single test case
     */
    public void sendTestCaseUpdate(TestCase testCase) {
        TestCaseDTO dto = new TestCaseDTO(testCase);
        messagingTemplate.convertAndSend(TESTCASE_TOPIC, dto);
    }

    /**
     * Send full dashboard metrics
     */
    public void sendDashboardUpdate() {
        DashboardDTO dashboardDTO = calculateDashboardMetrics();
        messagingTemplate.convertAndSend(DASHBOARD_TOPIC, dashboardDTO);
    }

    /**
     * Send execution update
     */
    public void sendExecutionUpdate(ExecutionDTO executionDTO) {
        messagingTemplate.convertAndSend(EXECUTION_TOPIC, executionDTO);
    }

    /**
     * Calculate all dashboard metrics
     */
    private DashboardDTO calculateDashboardMetrics() {
        long total = testCaseRepository.count();
        long passed = testCaseRepository.countByStatus(TestStatus.PASS);
        long failed = testCaseRepository.countByStatus(TestStatus.FAIL);
        long blocked = testCaseRepository.countByStatus(TestStatus.BLOCKED);
        long pending = testCaseRepository.countByStatus(TestStatus.PENDING);
        long active = testCaseRepository.countByStatus(TestStatus.ACTIVE);
        long skipped = testCaseRepository.countByStatus(TestStatus.SKIPPED);

        // Calculate pass rate
        double passRate = total > 0 ? (passed * 100.0 / total) : 0;

        // Get status distribution
        Map<String, Long> statusDistribution = new HashMap<>();
        List<Object[]> statusCounts = testCaseRepository.getStatusCounts();
        for (Object[] row : statusCounts) {
            statusDistribution.put(((TestStatus) row[0]).getDisplayName(), (Long) row[1]);
        }

        return new DashboardDTO(
                total, passed, failed, blocked, pending, active, skipped,
                passRate, statusDistribution);
    }

    // DTO Classes
    public static class TestCaseDTO {
        public Long id;
        public String testCaseId;
        public String testCaseName;
        public String status;
        public String statusClass;
        public String priority;
        public LocalDateTime updatedOn;

        public TestCaseDTO(TestCase testCase) {
            this.id = testCase.getId();
            this.testCaseId = testCase.getTestCaseId();
            this.testCaseName = testCase.getTestCaseName();
            this.status = testCase.getStatus().getDisplayName();
            this.statusClass = testCase.getStatus().getBootstrapClass();
            this.priority = testCase.getPriority();
            this.updatedOn = LocalDateTime.now();
        }
    }

    public static class ExecutionDTO {
        public Long executionId;
        public Long testCaseId;
        public String testCaseName;
        public String status;
        public String executedBy;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        public LocalDateTime executedOn;
        public String remarks;
        public String actualResult;
        public String expectedResultVerified;

        public ExecutionDTO(Long executionId, Long testCaseId, String testCaseName,
                String status, String executedBy, LocalDateTime executedOn,
                String remarks, String actualResult, String expectedResultVerified) {
            this.executionId = executionId;
            this.testCaseId = testCaseId;
            this.testCaseName = testCaseName;
            this.status = status;
            this.executedBy = executedBy;
            this.executedOn = executedOn;
            this.remarks = remarks;
            this.actualResult = actualResult;
            this.expectedResultVerified = expectedResultVerified;
        }
    }

    public static class DashboardDTO {
        public long totalTestCases;
        public long passedCount;
        public long failedCount;
        public long blockedCount;
        public long pendingCount;
        public long activeCount;
        public long skippedCount;
        public double passRate;
        public Map<String, Long> statusDistribution;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        public LocalDateTime lastUpdated;

        public DashboardDTO(long total, long passed, long failed, long blocked,
                long pending, long active, long skipped, double passRate,
                Map<String, Long> statusDistribution) {
            this.totalTestCases = total;
            this.passedCount = passed;
            this.failedCount = failed;
            this.blockedCount = blocked;
            this.pendingCount = pending;
            this.activeCount = active;
            this.skippedCount = skipped;
            this.passRate = passRate;
            this.statusDistribution = statusDistribution;
            this.lastUpdated = LocalDateTime.now();
        }
    }

    @Autowired
    private ActiveUserService activeUserService;

    public void sendActiveUsersUpdate() {
        messagingTemplate.convertAndSend("/topic/active-users",
                Map.of("activeUsers", activeUserService.getActiveUserCount()));
    }
}