package com.qa.testmanagement.service;

import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.qa.testmanagement.model.Execution;
import com.qa.testmanagement.model.TestCase;
import com.qa.testmanagement.model.TestStatus;
import com.qa.testmanagement.repository.ExecutionRepository;
import com.qa.testmanagement.repository.TestCaseRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReportService {

    @Autowired
    private TestCaseRepository testCaseRepository;

    @Autowired
    private ExecutionRepository executionRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public byte[] generateExcelReport() throws IOException {
        List<TestCase> testCases = testCaseRepository.findAll();
        List<Execution> executions = executionRepository.findAll();

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // Sheet 1: Test Cases
            Sheet testCaseSheet = workbook.createSheet("Test Cases");
            createTestCaseSheet(testCaseSheet, testCases);

            // Sheet 2: Executions
            Sheet executionSheet = workbook.createSheet("Executions");
            createExecutionSheet(executionSheet, executions);

            // Sheet 3: Summary
            Sheet summarySheet = workbook.createSheet("Summary");
            createSummarySheet(summarySheet, testCases, executions);

            workbook.write(out);
            return out.toByteArray();
        }
    }

    private void createTestCaseSheet(Sheet sheet, List<TestCase> testCases) {
        String[] headers = { "ID", "Test Case ID", "Name", "Priority", "Type", "Status", "Created On" };

        org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(0);
        org.apache.poi.ss.usermodel.CellStyle headerStyle = createHeaderStyle(sheet.getWorkbook());

        for (int i = 0; i < headers.length; i++) {
            org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        int rowNum = 1;
        for (TestCase tc : testCases) {
            org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(tc.getId());
            row.createCell(1).setCellValue(tc.getTestCaseId());
            row.createCell(2).setCellValue(tc.getTestCaseName());
            row.createCell(3).setCellValue(tc.getPriority());
            row.createCell(4).setCellValue(tc.getTestType());
            row.createCell(5).setCellValue(tc.getStatus().getDisplayName());
            row.createCell(6).setCellValue(tc.getCreatedOn() != null ? tc.getCreatedOn().format(DATE_FORMATTER) : "");
        }

        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private void createExecutionSheet(Sheet sheet, List<Execution> executions) {
        String[] headers = { "ID", "Test Case ID", "Status", "Executed By", "Executed On", "Remarks" };

        org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(0);
        org.apache.poi.ss.usermodel.CellStyle headerStyle = createHeaderStyle(sheet.getWorkbook());

        for (int i = 0; i < headers.length; i++) {
            org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        int rowNum = 1;
        for (Execution exec : executions) {
            org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(exec.getId());
            row.createCell(1).setCellValue(exec.getTestCase() != null ? exec.getTestCase().getId() : 0);
            row.createCell(2).setCellValue(exec.getStatus().getDisplayName());
            row.createCell(3).setCellValue(exec.getExecutedBy());
            row.createCell(4)
                    .setCellValue(exec.getExecutedOn() != null ? exec.getExecutedOn().format(DATE_FORMATTER) : "");
            row.createCell(5).setCellValue(exec.getRemarks() != null ? exec.getRemarks() : "");
        }

        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private void createSummarySheet(Sheet sheet, List<TestCase> testCases, List<Execution> executions) {
        // Summary statistics
        long total = testCases.size();
        long passed = testCases.stream().filter(tc -> tc.getStatus() == TestStatus.PASS).count();
        long failed = testCases.stream().filter(tc -> tc.getStatus() == TestStatus.FAIL).count();
        long blocked = testCases.stream().filter(tc -> tc.getStatus() == TestStatus.BLOCKED).count();
        long pending = testCases.stream().filter(tc -> tc.getStatus() == TestStatus.PENDING).count();

        double passRate = total > 0 ? (passed * 100.0 / total) : 0;

        // Priority distribution
        Map<String, Long> priorityCount = testCases.stream()
                .collect(Collectors.groupingBy(TestCase::getPriority, Collectors.counting()));

        // Status distribution for executions
        Map<String, Long> executionStatus = executions.stream()
                .collect(Collectors.groupingBy(e -> e.getStatus().getDisplayName(), Collectors.counting()));

        int rowNum = 0;
        org.apache.poi.ss.usermodel.CellStyle boldStyle = createBoldStyle(sheet.getWorkbook());

        org.apache.poi.ss.usermodel.Row titleRow = sheet.createRow(rowNum++);
        org.apache.poi.ss.usermodel.Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("QA TEST MANAGEMENT SUMMARY");
        titleCell.setCellStyle(boldStyle);

        // Merge cells for title
        sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 1));

        rowNum++;

        org.apache.poi.ss.usermodel.Row summaryRow = sheet.createRow(rowNum++);
        summaryRow.createCell(0).setCellValue("Generated on:");
        summaryRow.createCell(1).setCellValue(LocalDateTime.now().format(DATE_FORMATTER));

        rowNum++;

        // Test Case Summary
        org.apache.poi.ss.usermodel.Row testCaseSummaryHeader = sheet.createRow(rowNum++);
        testCaseSummaryHeader.createCell(0).setCellValue("TEST CASE SUMMARY");
        testCaseSummaryHeader.getCell(0).setCellStyle(boldStyle);

        addSummaryRow(sheet, rowNum++, "Total Test Cases", total);
        addSummaryRow(sheet, rowNum++, "Passed", passed);
        addSummaryRow(sheet, rowNum++, "Failed", failed);
        addSummaryRow(sheet, rowNum++, "Blocked", blocked);
        addSummaryRow(sheet, rowNum++, "Pending", pending);
        addSummaryRow(sheet, rowNum++, "Pass Rate", String.format("%.1f%%", passRate));

        rowNum++;

        // Priority Distribution
        org.apache.poi.ss.usermodel.Row priorityHeader = sheet.createRow(rowNum++);
        priorityHeader.createCell(0).setCellValue("PRIORITY DISTRIBUTION");
        priorityHeader.getCell(0).setCellStyle(boldStyle);

        for (Map.Entry<String, Long> entry : priorityCount.entrySet()) {
            addSummaryRow(sheet, rowNum++, entry.getKey(), entry.getValue());
        }

        rowNum++;

        // Execution Summary
        org.apache.poi.ss.usermodel.Row executionHeader = sheet.createRow(rowNum++);
        executionHeader.createCell(0).setCellValue("EXECUTION SUMMARY");
        executionHeader.getCell(0).setCellStyle(boldStyle);

        addSummaryRow(sheet, rowNum++, "Total Executions", executions.size());
        for (Map.Entry<String, Long> entry : executionStatus.entrySet()) {
            addSummaryRow(sheet, rowNum++, "  " + entry.getKey(), entry.getValue());
        }

        for (int i = 0; i < 2; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private void addSummaryRow(Sheet sheet, int rowNum, String label, Object value) {
        org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowNum);
        row.createCell(0).setCellValue(label);
        row.createCell(1).setCellValue(String.valueOf(value));
    }

    private org.apache.poi.ss.usermodel.CellStyle createHeaderStyle(Workbook workbook) {
        org.apache.poi.ss.usermodel.CellStyle style = workbook.createCellStyle();
        org.apache.poi.ss.usermodel.Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);

        // Set background color
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        // Add borders
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);

        return style;
    }

    private org.apache.poi.ss.usermodel.CellStyle createBoldStyle(Workbook workbook) {
        org.apache.poi.ss.usermodel.CellStyle style = workbook.createCellStyle();
        org.apache.poi.ss.usermodel.Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);

        // Add bottom border for headers
        style.setBorderBottom(BorderStyle.THIN);

        return style;
    }

    public byte[] generatePDFReport() throws IOException {
        List<TestCase> testCases = testCaseRepository.findAll();
        List<Execution> executions = executionRepository.findAll();

        // Calculate all statistics first
        long total = testCases.size();
        long passed = testCases.stream().filter(tc -> tc.getStatus() == TestStatus.PASS).count();
        long failed = testCases.stream().filter(tc -> tc.getStatus() == TestStatus.FAIL).count();
        long blocked = testCases.stream().filter(tc -> tc.getStatus() == TestStatus.BLOCKED).count();
        long pending = testCases.stream().filter(tc -> tc.getStatus() == TestStatus.PENDING).count();
        double passRate = total > 0 ? (passed * 100.0 / total) : 0;

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(out);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc, PageSize.A4);
            document.setMargins(50, 50, 50, 50);

            // Title
            document.add(new Paragraph("QA Test Management Report")
                    .setFontSize(20)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setBold());

            document.add(new Paragraph("Generated on: " + LocalDateTime.now().format(DATE_FORMATTER))
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(20));

            // Summary Section
            document.add(new Paragraph("Summary")
                    .setFontSize(16)
                    .setBold()
                    .setMarginTop(20));

            float[] columnWidths = { 200, 100 };
            Table summaryTable = new Table(UnitValue.createPercentArray(columnWidths));
            summaryTable.setWidth(UnitValue.createPercentValue(80));

            addPDFSummaryRow(summaryTable, "Total Test Cases", String.valueOf(total));
            addPDFSummaryRow(summaryTable, "Passed", String.valueOf(passed));
            addPDFSummaryRow(summaryTable, "Failed", String.valueOf(failed));
            addPDFSummaryRow(summaryTable, "Blocked", String.valueOf(blocked));
            addPDFSummaryRow(summaryTable, "Pending", String.valueOf(pending));
            addPDFSummaryRow(summaryTable, "Pass Rate", String.format("%.1f%%", passRate));

            document.add(summaryTable);

            // Priority Distribution
            document.add(new Paragraph("Priority Distribution")
                    .setFontSize(14)
                    .setBold()
                    .setMarginTop(20));

            Table priorityTable = new Table(UnitValue.createPercentArray(new float[] { 200, 100 }));
            priorityTable.setWidth(UnitValue.createPercentValue(80));

            Map<String, Long> priorityCount = testCases.stream()
                    .collect(Collectors.groupingBy(TestCase::getPriority, Collectors.counting()));

            for (Map.Entry<String, Long> entry : priorityCount.entrySet()) {
                addPDFSummaryRow(priorityTable, entry.getKey(), String.valueOf(entry.getValue()));
            }

            document.add(priorityTable);

            // Test Cases Table
            document.add(new Paragraph("Recent Test Cases")
                    .setFontSize(14)
                    .setBold()
                    .setMarginTop(20));

            float[] caseColumns = { 50, 100, 150, 80, 80 };
            Table caseTable = new Table(UnitValue.createPercentArray(caseColumns));
            caseTable.setWidth(UnitValue.createPercentValue(100));

            addPDFHeaderRow(caseTable, "ID", "Test Case ID", "Name", "Priority", "Status");

            testCases.stream().limit(20).forEach(tc -> {
                caseTable.addCell(new Cell().add(new Paragraph(String.valueOf(tc.getId()))));
                caseTable.addCell(new Cell().add(new Paragraph(tc.getTestCaseId())));
                caseTable.addCell(new Cell().add(new Paragraph(tc.getTestCaseName())));
                caseTable.addCell(new Cell().add(new Paragraph(tc.getPriority())));
                caseTable.addCell(new Cell().add(new Paragraph(tc.getStatus().getDisplayName())));
            });

            document.add(caseTable);

            // Recent Executions
            document.add(new Paragraph("Recent Executions")
                    .setFontSize(14)
                    .setBold()
                    .setMarginTop(20));

            float[] execColumns = { 50, 100, 80, 100, 120 };
            Table execTable = new Table(UnitValue.createPercentArray(execColumns));
            execTable.setWidth(UnitValue.createPercentValue(100));

            addPDFHeaderRow(execTable, "ID", "Status", "Executed By", "Executed On", "Test Case");

            executions.stream().limit(20).forEach(exec -> {
                execTable.addCell(new Cell().add(new Paragraph(String.valueOf(exec.getId()))));
                execTable.addCell(new Cell().add(new Paragraph(exec.getStatus().getDisplayName())));
                execTable.addCell(new Cell().add(new Paragraph(exec.getExecutedBy())));
                execTable.addCell(new Cell().add(new Paragraph(
                        exec.getExecutedOn() != null ? exec.getExecutedOn().format(DATE_FORMATTER) : "")));
                execTable.addCell(new Cell()
                        .add(new Paragraph(exec.getTestCase() != null ? exec.getTestCase().getTestCaseName() : "N/A")));
            });

            document.add(execTable);

            document.close();
            return out.toByteArray();
        }
    }

    private void addPDFSummaryRow(Table table, String label, String value) {
        table.addCell(new Cell().add(new Paragraph(label)));
        table.addCell(new Cell().add(new Paragraph(value)));
    }

    private void addPDFHeaderRow(Table table, String... headers) {
        for (String header : headers) {
            table.addHeaderCell(new Cell().add(new Paragraph(header)).setBold());
        }
    }
}