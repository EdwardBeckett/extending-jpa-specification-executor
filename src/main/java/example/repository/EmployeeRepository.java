package example.repository;

import foo.bar.infra.MyJpaSpecificationExecutor;
import org.springframework.data.jpa.repository.JpaRepository;
import example.domain.Employee;

public interface EmployeeRepository extends JpaRepository<Employee, Long>, MyJpaSpecificationExecutor<Employee> {
}
