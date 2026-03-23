package com.qa.testmanagement.repository;

import com.qa.testmanagement.model.SuiteExecution;
import com.qa.testmanagement.model.TestSuite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SuiteExecutionRepository extends JpaRepository<SuiteExecution, Long> {

    List<SuiteExecution> findByTestSuiteOrderByExecutedOnDesc(TestSuite testSuite);

    Page<SuiteExecution> findByTestSuite(TestSuite testSuite, Pageable pageable);
}