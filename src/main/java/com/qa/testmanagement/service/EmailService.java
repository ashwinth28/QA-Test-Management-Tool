package com.qa.testmanagement.service;

import com.qa.testmanagement.model.Execution;
import com.qa.testmanagement.model.TestCase;
import com.qa.testmanagement.model.User;
import com.qa.testmanagement.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private TemplateEngine templateEngine;

    @Autowired
    private UserRepository userRepository;

    @Value("${app.email.from}")
    private String fromEmail;

    @Value("${app.email.enabled}")
    private boolean emailEnabled;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /**
     * Send execution notification to testers
     */
    public void sendExecutionNotification(TestCase testCase, Execution execution) {
        if (!emailEnabled)
            return;

        try {
            // Get all testers and admins
            List<User> recipients = userRepository.findAll().stream()
                    .filter(u -> u.isEnabled() &&
                            (u.getRole().name().equals("TESTER") || u.getRole().name().equals("ADMIN")))
                    .collect(Collectors.toList());

            for (User user : recipients) {
                sendExecutionEmail(user, testCase, execution);
            }
        } catch (Exception e) {
            System.err.println("Failed to send execution notification: " + e.getMessage());
        }
    }

    /**
     * Send test case assignment notification
     */
    public void sendAssignmentNotification(TestCase testCase, User assignedTo) {
        if (!emailEnabled)
            return;

        try {
            Context context = new Context();
            context.setVariable("testCase", testCase);
            context.setVariable("user", assignedTo);
            context.setVariable("currentDate", LocalDateTime.now().format(DATE_FORMATTER));

            String htmlContent = templateEngine.process("email/test-case-assignment", context);

            sendEmail(assignedTo.getEmail(), "Test Case Assigned: " + testCase.getTestCaseId(), htmlContent);
        } catch (Exception e) {
            System.err.println("Failed to send assignment notification: " + e.getMessage());
        }
    }

    /**
     * Send daily summary report
     */
    public void sendDailySummary() {
        if (!emailEnabled)
            return;

        try {
            List<User> admins = userRepository.findAll().stream()
                    .filter(u -> u.isEnabled() && u.getRole().name().equals("ADMIN"))
                    .collect(Collectors.toList());

            Context context = new Context();
            // You can add stats here - removed unused yesterday variable
            LocalDateTime startDate = LocalDateTime.now().minusDays(1);
            LocalDateTime endDate = LocalDateTime.now();

            context.setVariable("startDate", startDate.format(DATE_FORMATTER));
            context.setVariable("endDate", endDate.format(DATE_FORMATTER));
            // Add more stats as needed

            String htmlContent = templateEngine.process("email/daily-summary", context);

            for (User admin : admins) {
                sendEmail(admin.getEmail(), "Daily Test Execution Summary", htmlContent);
            }
        } catch (Exception e) {
            System.err.println("Failed to send daily summary: " + e.getMessage());
        }
    }

    /**
     * Send password reset email
     */
    public void sendPasswordResetEmail(User user, String resetToken) {
        if (!emailEnabled)
            return;

        try {
            Context context = new Context();
            context.setVariable("user", user);
            context.setVariable("resetToken", resetToken);
            context.setVariable("resetLink", "https://yourdomain.com/reset-password?token=" + resetToken);

            String htmlContent = templateEngine.process("email/password-reset", context);

            sendEmail(user.getEmail(), "Password Reset Request", htmlContent);
        } catch (Exception e) {
            System.err.println("Failed to send password reset email: " + e.getMessage());
        }
    }

    private void sendExecutionEmail(User user, TestCase testCase, Execution execution) {
        Context context = new Context();
        context.setVariable("user", user);
        context.setVariable("testCase", testCase);
        context.setVariable("execution", execution);
        context.setVariable("status", execution.getStatus().getDisplayName());
        context.setVariable("executedBy", execution.getExecutedBy());
        context.setVariable("executedOn", execution.getExecutedOn().format(DATE_FORMATTER));
        context.setVariable("remarks", execution.getRemarks());

        String htmlContent = templateEngine.process("email/execution-notification", context);

        sendEmail(user.getEmail(),
                "Test Case Executed: " + testCase.getTestCaseId() + " - " + execution.getStatus().getDisplayName(),
                htmlContent);
    }

    private void sendEmail(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Failed to send email to " + to + ": " + e.getMessage());
        }
    }
}