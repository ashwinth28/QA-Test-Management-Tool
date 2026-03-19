package com.qa.testmanagement.controller;

import com.qa.testmanagement.model.TestCase;
import com.qa.testmanagement.model.TestStatus;
import com.qa.testmanagement.repository.TestCaseRepository;
import com.qa.testmanagement.service.TestCaseUpdateService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Controller
@RequestMapping("/upload")
public class ExcelUploadController {

    @Autowired
    private TestCaseRepository testCaseRepository;

    @Autowired
    private TestCaseUpdateService updateService;

    @GetMapping
    public String uploadPage() {
        return "upload";
    }

    @PostMapping("/excel")
    public String uploadExcelFile(@RequestParam("file") MultipartFile file,
            RedirectAttributes redirectAttributes) {

        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("message", "Please select a file to upload");
            return "redirect:/upload";
        }

        try {
            List<TestCase> testCases = parseExcelFile(file);
            int imported = 0;
            int updated = 0;
            int skipped = 0;

            for (TestCase testCase : testCases) {
                try {
                    TestCase existing = testCaseRepository.findByTestCaseId(testCase.getTestCaseId()).orElse(null);

                    if (existing == null) {
                        // New test case
                        testCase.setCreatedOn(LocalDateTime.now());
                        testCase.setStatus(TestStatus.PENDING);
                        testCaseRepository.save(testCase);
                        imported++;
                    } else {
                        // Update existing
                        updateExistingTestCase(existing, testCase);
                        testCaseRepository.save(existing);
                        updated++;
                    }

                    // Send real-time update
                    updateService.sendTestCaseUpdate(testCase);

                } catch (Exception e) {
                    skipped++;
                }
            }

            // Update dashboard
            updateService.sendDashboardUpdate();

            redirectAttributes.addFlashAttribute("successMessage",
                    String.format("Import completed: %d new, %d updated, %d skipped", imported, updated, skipped));

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Error processing file: " + e.getMessage());
        }

        return "redirect:/testcases/list";
    }

    private List<TestCase> parseExcelFile(MultipartFile file) throws IOException {
        List<TestCase> testCases = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rows = sheet.iterator();

            // Skip header row
            if (rows.hasNext()) {
                rows.next();
            }

            while (rows.hasNext()) {
                Row currentRow = rows.next();
                TestCase testCase = new TestCase();

                // Expected columns: TestCaseId, Priority, TestType, TestCaseName,
                // Precondition, TestSteps, ExpectedResult, ActualResult, Remarks

                testCase.setTestCaseId(getCellValueAsString(currentRow.getCell(0)));
                testCase.setPriority(getCellValueAsString(currentRow.getCell(1)));
                testCase.setTestType(getCellValueAsString(currentRow.getCell(2)));
                testCase.setTestCaseName(getCellValueAsString(currentRow.getCell(3)));
                testCase.setPrecondition(getCellValueAsString(currentRow.getCell(4)));
                testCase.setTestSteps(getCellValueAsString(currentRow.getCell(5)));
                testCase.setExpectedResult(getCellValueAsString(currentRow.getCell(6)));
                testCase.setActualResult(getCellValueAsString(currentRow.getCell(7)));
                testCase.setRemarks(getCellValueAsString(currentRow.getCell(8)));

                // Validate required fields
                if (testCase.getTestCaseId() != null && !testCase.getTestCaseId().isEmpty() &&
                        testCase.getTestCaseName() != null && !testCase.getTestCaseName().isEmpty()) {
                    testCases.add(testCase);
                }
            }
        }

        return testCases;
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    return String.valueOf((long) cell.getNumericCellValue());
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }

    private void updateExistingTestCase(TestCase existing, TestCase newData) {
        existing.setPriority(newData.getPriority());
        existing.setTestType(newData.getTestType());
        existing.setTestCaseName(newData.getTestCaseName());
        existing.setPrecondition(newData.getPrecondition());
        existing.setTestSteps(newData.getTestSteps());
        existing.setExpectedResult(newData.getExpectedResult());
        existing.setActualResult(newData.getActualResult());
        existing.setRemarks(newData.getRemarks());
    }
}