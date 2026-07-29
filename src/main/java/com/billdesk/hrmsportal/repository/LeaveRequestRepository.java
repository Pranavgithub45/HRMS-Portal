package com.billdesk.hrmsportal.repository;


import com.billdesk.hrmsportal.entity.LeaveRequest;
import com.billdesk.hrmsportal.entity.enums.LeaveStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {

    // Employee — own leave history
    List<LeaveRequest> findByEmployee_EmployeeIdOrderByAppliedDateDesc(Long employeeId);

    List<LeaveRequest> findByEmployee_EmployeeIdAndStatus(Long employeeId, LeaveStatus status);

    // Manager — team leave requests
    List<LeaveRequest> findByEmployee_Manager_EmployeeIdOrderByAppliedDateDesc(Long managerId);

    // Manager — pending queue
    List<LeaveRequest> findByEmployee_Manager_EmployeeIdAndStatus(Long managerId, LeaveStatus status);

    // HR — org-wide
    List<LeaveRequest> findByStatus(LeaveStatus status);

    long countByStatus(LeaveStatus status);

    long countByEmployee_EmployeeIdAndStatus(Long employeeId, LeaveStatus status);

    long countByEmployee_Manager_EmployeeIdAndStatus(Long managerId, LeaveStatus status);

    // Overlap check before applying (Phase 4)
    @Query("""
           SELECT COUNT(l) > 0
           FROM LeaveRequest l
           WHERE l.employee.employeeId = :employeeId
             AND l.status IN (:statuses)
             AND l.startDate <= :endDate
             AND l.endDate   >= :startDate
           """)
    boolean existsOverlappingLeave(@Param("employeeId") Long employeeId,
                                   @Param("statuses") List<LeaveStatus> statuses,
                                   @Param("startDate") LocalDate startDate,
                                   @Param("endDate") LocalDate endDate);
}