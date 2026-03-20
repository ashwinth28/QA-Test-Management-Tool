package com.qa.testmanagement.repository;

import com.qa.testmanagement.model.TestCase;
import com.qa.testmanagement.model.TestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TestCaseRepository extends JpaRepository<TestCase, Long> {

        Optional<TestCase> findByTestCaseId(String testCaseId);

        long countByStatus(TestStatus status);

        List<TestCase> findByStatus(TestStatus status);

        Page<TestCase> findByStatus(TestStatus status, Pageable pageable);

        @Query("SELECT tc FROM TestCase tc WHERE " +
                        "LOWER(tc.testCaseId) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                        "LOWER(tc.testCaseName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                        "LOWER(tc.priority) LIKE LOWER(CONCAT('%', :keyword, '%'))")
        Page<TestCase> search(@Param("keyword") String keyword, Pageable pageable);

        @Query("SELECT tc.status, COUNT(tc) FROM TestCase tc GROUP BY tc.status")
        List<Object[]> getStatusCounts();

        @Query("SELECT COUNT(tc) FROM TestCase tc WHERE tc.createdOn BETWEEN :startDate AND :endDate")
        long countByCreatedDateBetween(@Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate);

        @Query("SELECT tc.priority, COUNT(tc) FROM TestCase tc GROUP BY tc.priority")
        List<Object[]> getPriorityDistribution();

        boolean existsByTestCaseId(String testCaseId);

        // Add these methods to TestCaseRepository.java

        @Query("SELECT DISTINCT tc.category FROM TestCase tc WHERE tc.category IS NOT NULL AND tc.category != ''")
        List<String> findAllCategories();

        @Query("SELECT DISTINCT t FROM TestCase tc JOIN tc.tags t")
        List<String> findAllTags();

        @Query("SELECT tc FROM TestCase tc WHERE " +
                        "(:category IS NULL OR tc.category = :category) AND " +
                        "(:tag IS NULL OR :tag MEMBER OF tc.tags)")
        Page<TestCase> findByCategoryAndTag(@Param("category") String category,
                        @Param("tag") String tag,
                        Pageable pageable);
}