package com.billdesk.hrmsportal.service.impl;

import com.billdesk.hrmsportal.dto.request.PayrollBulkRequest;
import com.billdesk.hrmsportal.dto.request.PayrollGenerateRequest;
import com.billdesk.hrmsportal.dto.response.PayrollResponse;
import com.billdesk.hrmsportal.entity.Employee;
import com.billdesk.hrmsportal.entity.Payroll;
import com.billdesk.hrmsportal.entity.enums.EmployeeStatus;
import com.billdesk.hrmsportal.entity.enums.Role;
import com.billdesk.hrmsportal.mapper.PayrollMapper;
import com.billdesk.hrmsportal.repository.EmployeeRepository;
import com.billdesk.hrmsportal.repository.PayrollRepository;
import com.billdesk.hrmsportal.service.PayrollService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PayrollServiceImpl implements PayrollService {

    private final PayrollRepository payrollRepository;
    private final EmployeeRepository employeeRepository;
    private final PayrollMapper payrollMapper;

    // ---------------- GENERATE ----------------

    @Override
    @Transactional
    public PayrollResponse generate(PayrollGenerateRequest request, Role callerRole) {

        requireHr(callerRole);
        validatePeriod(request.getMonth(), request.getYear());

        Employee employee = employeeRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Employee not found with id: " + request.getEmployeeId()));

        if (employee.getStatus() == EmployeeStatus.INACTIVE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot generate payroll for an inactive employee");
        }

        if (payrollRepository.existsByEmployee_EmployeeIdAndMonthAndYear(
                employee.getEmployeeId(), request.getMonth(), request.getYear())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Payroll already generated for this employee for "
                            + request.getMonth() + "/" + request.getYear());
        }

        BigDecimal baseSalary = request.getBaseSalary() != null
                ? request.getBaseSalary()
                : employee.getBaseSalary();

        if (baseSalary == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "No base salary on record for this employee. Provide baseSalary explicitly.");
        }

        return payrollMapper.toResponse(
                payrollRepository.save(buildPayroll(employee, request.getMonth(), request.getYear(),
                        baseSalary, request.getBonus())));
    }

    @Override
    @Transactional
    public Map<String, Object> generateBulk(PayrollBulkRequest request, Role callerRole) {

        requireHr(callerRole);
        validatePeriod(request.getMonth(), request.getYear());

        List<Employee> employees = request.getDepartmentId() != null
                ? employeeRepository.findByDepartment_DepartmentId(request.getDepartmentId())
                : employeeRepository.findByStatus(EmployeeStatus.ACTIVE);

        List<PayrollResponse> generated = new ArrayList<>();
        List<String> skipped = new ArrayList<>();

        for (Employee employee : employees) {

            if (employee.getStatus() == EmployeeStatus.INACTIVE) {
                skipped.add(employee.getName() + " — inactive");
                continue;
            }

            if (payrollRepository.existsByEmployee_EmployeeIdAndMonthAndYear(
                    employee.getEmployeeId(), request.getMonth(), request.getYear())) {
                skipped.add(employee.getName() + " — already generated");
                continue;
            }

            if (employee.getBaseSalary() == null) {
                skipped.add(employee.getName() + " — no base salary on record");
                continue;
            }

            generated.add(payrollMapper.toResponse(
                    payrollRepository.save(buildPayroll(employee, request.getMonth(),
                            request.getYear(), employee.getBaseSalary(), null))));
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("generatedCount", generated.size());
        result.put("skippedCount", skipped.size());
        result.put("generated", generated);
        result.put("skipped", skipped);
        return result;
    }

    // ---------------- READ ----------------

    @Override
    @Transactional(readOnly = true)
    public List<PayrollResponse> getMyPayroll(Long callerId) {
        return payrollMapper.toResponseList(
                payrollRepository.findByEmployee_EmployeeIdOrderByYearDescMonthDesc(callerId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PayrollResponse> getByEmployee(Long employeeId, Role callerRole) {

        requireHr(callerRole);

        if (!employeeRepository.existsById(employeeId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Employee not found with id: " + employeeId);
        }

        return payrollMapper.toResponseList(
                payrollRepository.findByEmployee_EmployeeIdOrderByYearDescMonthDesc(employeeId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PayrollResponse> getAll(Integer month, Integer year, Role callerRole) {

        requireHr(callerRole);

        if (month != null && year != null) {
            return payrollMapper.toResponseList(payrollRepository.findByMonthAndYear(month, year));
        }
        return payrollMapper.toResponseList(payrollRepository.findAll());
    }

    // ---------------- HELPERS ----------------

    private Payroll buildPayroll(Employee employee, Integer month, Integer year,
                                 BigDecimal baseSalary, BigDecimal bonus) {

        BigDecimal safeBonus = bonus != null ? bonus : BigDecimal.ZERO;

        Payroll payroll = new Payroll();
        payroll.setEmployee(employee);
        payroll.setMonth(month);
        payroll.setYear(year);
        payroll.setBaseSalary(baseSalary);
        payroll.setBonus(safeBonus);
        payroll.setNetSalary(baseSalary.add(safeBonus));   // server-side, always
        payroll.setGeneratedDate(LocalDate.now());
        return payroll;
    }

    private void validatePeriod(Integer month, Integer year) {
        if (YearMonth.of(year, month).isAfter(YearMonth.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cannot generate payroll for a future month");
        }
    }

    private void requireHr(Role callerRole) {
        if (callerRole != Role.HR) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied. HR role required");
        }
    }
}