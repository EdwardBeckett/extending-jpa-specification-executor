package foo.bar.infra;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.NoRepositoryBean;

import javax.persistence.metamodel.SingularAttribute;
import java.util.Map;

@NoRepositoryBean
public interface MyJpaSpecificationExecutor<ENTITY> extends JpaSpecificationExecutor<ENTITY> {

    <KEY> Map<KEY, Long> groupAndCount(SingularAttribute<ENTITY, KEY> singularAttribute, Specification<ENTITY> where);
}
