package com.billdesk.hrmsportal.repository;


import com.billdesk.hrmsportal.entity.Payroll;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PayrollRepository extends JpaRepository<Payroll, Long> {

    // Employee — own payroll history
    List<Payroll> findByEmployee_EmployeeIdOrderByYearDescMonthDesc(Long employeeId);

    Optional<Payroll> findByEmployee_EmployeeIdAndMonthAndYear(Long employeeId, Integer month, Integer year);

    // Duplicate guard — one payroll per employee per month
    boolean existsByEmployee_EmployeeIdAndMonthAndYear(Long employeeId, Integer month, Integer year);

    // HR — filter by period
    List<Payroll> findByMonthAndYear(Integer month, Integer year);

    List<Payroll> findByEmployee_Department_DepartmentIdAndMonthAndYear(Long departmentId, Integer month, Integer year);

    // HR dashboard — payroll generated this month
    long countByMonthAndYear(Integer month, Integer year);
}