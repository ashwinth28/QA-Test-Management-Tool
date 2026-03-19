package com.qa.testmanagement.repository;

import com.qa.testmanagement.model.Execution;
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

@Repository
public interface ExecutionRepository extends JpaRepository<Execution, Long> {

    List<Execution> findByTestCaseOrderByExecutedOnDesc(TestCase testCase);

    Page<Execution> findByTestCase(TestCase testCase, Pageable pageable);

    @Query("SELECT e FROM Execution e WHERE e.testCase.id = :testCaseId ORDER BY e.executedOn DESC")
    List<Execution> findByTestCaseId(@Param("testCaseId") Long testCaseId);

    @Query("SELECT e.status, COUNT(e) FROM Execution e GROUP BY e.status")
    List<Object[]> getExecutionStatusCount();

    @Query("SELECT DATE(e.executedOn), COUNT(e) FROM Execution e " +
            "WHERE e.executedOn BETWEEN :startDate AND :endDate " +
            "GROUP BY DATE(e.executedOn) ORDER BY DATE(e.executedOn)")
    List<Object[]> getDailyExecutionCount(@Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    long countByExecutedOnAfter(LocalDateTime date);

    long countByStatusAndExecutedOnBetween(TestStatus status, LocalDateTime start, LocalDateTime end);

    @Query("SELECT e.executedBy, COUNT(e) FROM Execution e " +
            "GROUP BY e.executedBy ORDER BY COUNT(e) DESC")
    List<Object[]> getTopExecutors(Pageable pageable);
}