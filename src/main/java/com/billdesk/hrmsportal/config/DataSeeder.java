package com.billdesk.hrmsportal.config;

import com.billdesk.hrmsportal.entity.Department;
import com.billdesk.hrmsportal.entity.Employee;
import com.billdesk.hrmsportal.entity.enums.EmployeeStatus;
import com.billdesk.hrmsportal.entity.enums.Role;
import com.billdesk.hrmsportal.repository.DepartmentRepository;
import com.billdesk.hrmsportal.repository.EmployeeRepository;
import com.billdesk.hrmsportal.util.PasswordUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final DepartmentRepository departmentRepository;
    private final EmployeeRepository employeeRepository;
    private final PasswordUtil passwordUtil;

    @Override
    public void run(String... args) {

        // idempotent — skip if data already exists
        if (employeeRepository.count() > 0) {
            System.out.println(">>> Seed skipped (data already present)");
            return;
        }

        // ---------- Departments ----------
        Department engineering = new Department();
        engineering.setDepartmentName("Engineering");
        engineering.setLocation("Mumbai");
        engineering.setDescription("Product engineering team");
        departmentRepository.save(engineering);

        Department humanResources = new Department();
        humanResources.setDepartmentName("Human Resources");
        humanResources.setLocation("Mumbai");
        humanResources.setDescription("HR and people operations");
        departmentRepository.save(humanResources);

        // ---------- HR (top of chain, no manager) ----------
        Employee hr = new Employee();
        hr.setName("HR Admin");
        hr.setEmail("hr@hrms.com");
        hr.setPassword(passwordUtil.hash("hr@123"));
        hr.setPhone("9000000001");
        hr.setDesignation("HR Manager");
        hr.setJoinDate(LocalDate.of(2024, 1, 10));
        hr.setRole(Role.HR);
        hr.setStatus(EmployeeStatus.ACTIVE);
        hr.setBaseSalary(new BigDecimal("90000.00"));
        hr.setDepartment(humanResources);
        hr.setManager(null);
        employeeRepository.save(hr);

        // ---------- Manager (reports to HR) ----------
        Employee manager = new Employee();
        manager.setName("Team Manager");
        manager.setEmail("manager@hrms.com");
        manager.setPassword(passwordUtil.hash("mgr@123"));
        manager.setPhone("9000000002");
        manager.setDesignation("Engineering Manager");
        manager.setJoinDate(LocalDate.of(2024, 3, 15));
        manager.setRole(Role.MANAGER);
        manager.setStatus(EmployeeStatus.ACTIVE);
        manager.setBaseSalary(new BigDecimal("75000.00"));
        manager.setDepartment(engineering);
        manager.setManager(hr);
        employeeRepository.save(manager);

        // ---------- Employee 1 (reports to manager) ----------
        Employee emp1 = new Employee();
        emp1.setName("Tanmay");
        emp1.setEmail("emp@hrms.com");
        emp1.setPassword(passwordUtil.hash("emp@123"));
        emp1.setPhone("9000000003");
        emp1.setDesignation("Android Developer");
        emp1.setJoinDate(LocalDate.of(2025, 11, 1));
        emp1.setRole(Role.EMPLOYEE);
        emp1.setStatus(EmployeeStatus.ACTIVE);
        emp1.setBaseSalary(new BigDecimal("60000.00"));
        emp1.setDepartment(engineering);
        emp1.setManager(manager);
        employeeRepository.save(emp1);

        // ---------- Employee 2 (same manager — needed to test team scoping) ----------
        Employee emp2 = new Employee();
        emp2.setName("Rohit Sharma");
        emp2.setEmail("emp2@hrms.com");
        emp2.setPassword(passwordUtil.hash("emp@123"));
        emp2.setPhone("9000000004");
        emp2.setDesignation("Backend Developer");
        emp2.setJoinDate(LocalDate.of(2026, 2, 5));
        emp2.setRole(Role.EMPLOYEE);
        emp2.setStatus(EmployeeStatus.ACTIVE);
        emp2.setBaseSalary(new BigDecimal("65000.00"));
        emp2.setDepartment(engineering);
        emp2.setManager(manager);
        employeeRepository.save(emp2);

        // ---------- Employee 3 (NO manager — for 403 negative tests) ----------
        Employee emp3 = new Employee();
        emp3.setName("Priya Desai");
        emp3.setEmail("emp3@hrms.com");
        emp3.setPassword(passwordUtil.hash("emp@123"));
        emp3.setPhone("9000000005");
        emp3.setDesignation("QA Engineer");
        emp3.setJoinDate(LocalDate.of(2026, 7, 1));
        emp3.setRole(Role.EMPLOYEE);
        emp3.setStatus(EmployeeStatus.ACTIVE);
        emp3.setBaseSalary(new BigDecimal("55000.00"));
        emp3.setDepartment(engineering);
        emp3.setManager(hr);
        employeeRepository.save(emp3);

        System.out.println("""
                >>> Seed data inserted
                    HR       : hr@hrms.com      / hr@123
                    MANAGER  : manager@hrms.com / mgr@123
                    EMPLOYEE : emp@hrms.com     / emp@123
                    EMPLOYEE : emp2@hrms.com    / emp@123
                    EMPLOYEE : emp3@hrms.com    / emp@123
                """);
    }
}