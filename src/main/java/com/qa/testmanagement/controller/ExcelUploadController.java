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
import java.util.*;

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
                        // Update existing - preserve created date and don't overwrite category/tags if
                        // empty
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

            // Read header row to determine column mapping
            if (!rows.hasNext()) {
                return testCases;
            }

            Row headerRow = rows.next();
            Map<String, Integer> columnMap = getColumnMap(headerRow);

            while (rows.hasNext()) {
                Row currentRow = rows.next();
                TestCase testCase = new TestCase();

                // Map columns based on header names
                testCase.setTestCaseId(
                        getCellValueAsString(currentRow.getCell(columnMap.getOrDefault("TestCaseId", 0))));
                testCase.setPriority(getCellValueAsString(currentRow.getCell(columnMap.getOrDefault("Priority", 1))));
                testCase.setTestType(getCellValueAsString(currentRow.getCell(columnMap.getOrDefault("TestType", 2))));
                testCase.setTestCaseName(
                        getCellValueAsString(currentRow.getCell(columnMap.getOrDefault("TestCaseName", 3))));
                testCase.setPrecondition(
                        getCellValueAsString(currentRow.getCell(columnMap.getOrDefault("Precondition", 4))));
                testCase.setTestSteps(getCellValueAsString(currentRow.getCell(columnMap.getOrDefault("TestSteps", 5))));
                testCase.setExpectedResult(
                        getCellValueAsString(currentRow.getCell(columnMap.getOrDefault("ExpectedResult", 6))));
                testCase.setActualResult(
                        getCellValueAsString(currentRow.getCell(columnMap.getOrDefault("ActualResult", 7))));
                testCase.setRemarks(getCellValueAsString(currentRow.getCell(columnMap.getOrDefault("Remarks", 8))));

                // Category - only set if column exists and has value
                Integer categoryCol = columnMap.get("Category");
                if (categoryCol != null) {
                    String category = getCellValueAsString(currentRow.getCell(categoryCol));
                    if (category != null && !category.isEmpty()) {
                        testCase.setCategory(category);
                    }
                }

                // Tags - only set if column exists and has value
                Integer tagsCol = columnMap.get("Tags");
                if (tagsCol != null) {
                    String tagsStr = getCellValueAsString(currentRow.getCell(tagsCol));
                    if (tagsStr != null && !tagsStr.isEmpty()) {
                        Set<String> tags = new HashSet<>();
                        String[] tagArray = tagsStr.split(",");
                        for (String tag : tagArray) {
                            String trimmedTag = tag.trim();
                            if (!trimmedTag.isEmpty()) {
                                tags.add(trimmedTag);
                            }
                        }
                        testCase.setTags(tags);
                    }
                }

                // Validate required fields
                if (testCase.getTestCaseId() != null && !testCase.getTestCaseId().isEmpty() &&
                        testCase.getTestCaseName() != null && !testCase.getTestCaseName().isEmpty()) {
                    testCases.add(testCase);
                }
            }
        }

        return testCases;
    }

    private Map<String, Integer> getColumnMap(Row headerRow) {
        Map<String, Integer> columnMap = new HashMap<>();

        for (Cell cell : headerRow) {
            String headerValue = getCellValueAsString(cell).trim();

            // Map common header variations
            switch (headerValue.toLowerCase()) {
                case "testcaseid":
                case "test case id":
                case "id":
                    columnMap.put("TestCaseId", cell.getColumnIndex());
                    break;
                case "priority":
                    columnMap.put("Priority", cell.getColumnIndex());
                    break;
                case "testtype":
                case "test type":
                case "type":
                    columnMap.put("TestType", cell.getColumnIndex());
                    break;
                case "testcasename":
                case "test case name":
                case "name":
                    columnMap.put("TestCaseName", cell.getColumnIndex());
                    break;
                case "precondition":
                    columnMap.put("Precondition", cell.getColumnIndex());
                    break;
                case "teststeps":
                case "test steps":
                case "steps":
                    columnMap.put("TestSteps", cell.getColumnIndex());
                    break;
                case "expectedresult":
                case "expected result":
                    columnMap.put("ExpectedResult", cell.getColumnIndex());
                    break;
                case "actualresult":
                case "actual result":
                    columnMap.put("ActualResult", cell.getColumnIndex());
                    break;
                case "remarks":
                case "remark":
                    columnMap.put("Remarks", cell.getColumnIndex());
                    break;
                case "category":
                case "categories":
                    columnMap.put("Category", cell.getColumnIndex());
                    break;
                case "tags":
                case "tag":
                    columnMap.put("Tags", cell.getColumnIndex());
                    break;
                default:
                    break;
            }
        }

        return columnMap;
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
        // Update basic fields
        existing.setPriority(newData.getPriority());
        existing.setTestType(newData.getTestType());
        existing.setTestCaseName(newData.getTestCaseName());
        existing.setPrecondition(newData.getPrecondition());
        existing.setTestSteps(newData.getTestSteps());
        existing.setExpectedResult(newData.getExpectedResult());
        existing.setActualResult(newData.getActualResult());
        existing.setRemarks(newData.getRemarks());

        // Update category only if provided in Excel (non-empty)
        if (newData.getCategory() != null && !newData.getCategory().isEmpty()) {
            existing.setCategory(newData.getCategory());
        }
        // If category is empty in Excel, DO NOT overwrite existing category

        // Update tags only if provided in Excel (non-empty)
        if (newData.getTags() != null && !newData.getTags().isEmpty()) {
            existing.setTags(newData.getTags());
        }
        // If tags are empty in Excel, DO NOT overwrite existing tags
    }
}