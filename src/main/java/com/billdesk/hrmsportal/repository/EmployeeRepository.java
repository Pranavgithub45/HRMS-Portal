package com.billdesk.hrmsportal.repository;

import com.billdesk.hrmsportal.entity.Employee;
import com.billdesk.hrmsportal.entity.enums.EmployeeStatus;
import com.billdesk.hrmsportal.entity.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    Optional<Employee> findByEmail(String email);

    boolean existsByEmail(String email);

    // Manager's direct reports
    List<Employee> findByManager_EmployeeId(Long managerId);

    List<Employee> findByDepartment_DepartmentId(Long departmentId);

    List<Employee> findByStatus(EmployeeStatus status);

    List<Employee> findByRole(Role role);

    long countByStatus(EmployeeStatus status);

    // HR dashboard — employees joined this month
    List<Employee> findByJoinDateBetween(LocalDate startDate, LocalDate endDate);

    // HR dashboard — department-wise employee count
    @Query("""
           SELECT d.departmentName, COUNT(e)
           FROM Employee e
           JOIN e.department d
           GROUP BY d.departmentName
           """)
    List<Object[]> countEmployeesByDepartment();

    // Bulk payroll (Phase 5) — active employees only
    @Query("SELECT e FROM Employee e WHERE e.status = :status")
    List<Employee> findAllActive(@Param("status") EmployeeStatus status);
}