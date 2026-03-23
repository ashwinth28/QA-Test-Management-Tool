package com.qa.testmanagement.repository;

import com.qa.testmanagement.model.TestSuite;
import com.qa.testmanagement.model.TestSuiteStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TestSuiteRepository extends JpaRepository<TestSuite, Long> {

    Page<TestSuite> findByNameContainingIgnoreCase(String name, Pageable pageable);

    List<TestSuite> findByStatus(TestSuiteStatus status);

    @Query("SELECT COUNT(s) FROM TestSuite s")
    long countTotalSuites();

    @Query("SELECT s.status, COUNT(s) FROM TestSuite s GROUP BY s.status")
    List<Object[]> getStatusCounts();
}