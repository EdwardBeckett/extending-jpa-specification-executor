package example.repository;

import example.App;
import example.domain.Department;
import example.domain.Employee;
import example.domain.Employee_;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.data.jpa.domain.Specifications;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.time.LocalDateTime;
import java.util.Map;

import static java.time.LocalDateTime.of;
import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.MapAssert.entry;
import static org.springframework.data.jpa.domain.Specifications.where;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = App.class)
public class EmployeeRepositoryTest {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Test
    public void countEmployeesOlderThan40GroupedByDepartment() {
        final LocalDateTime olderThanDate = of(1975, 1, 1, 0, 0);

        final Specifications<Employee> where = where(
                (root, query, cb) ->
                        cb.lessThan(root.get(Employee_.dateOfBirth), olderThanDate)
        );

        final Map<Department, Long> result = employeeRepository.groupAndCount(Employee_.department, where);

        assertThat(result).includes(
                entry(Department.HR, 15L),
                entry(Department.IT, 16L),
                entry(Department.COMMUNICATION, 14L)
        );
    }

}