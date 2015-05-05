package foo.bar.infra;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;

import javax.persistence.EntityManager;
import javax.persistence.Tuple;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;
import javax.persistence.metamodel.SingularAttribute;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toMap;

public class MySimpleJpaRepository<ENTITY, ID extends Serializable>
        extends SimpleJpaRepository<ENTITY, ID>
        implements MyJpaSpecificationExecutor<ENTITY> {

    private final EntityManager em;

    public MySimpleJpaRepository(Class<ENTITY> entityClass, EntityManager em) {
        super(entityClass, em);
        this.em = em;
    }

    @Override
    public <KEY> Map<KEY, Long> groupAndCount(SingularAttribute<ENTITY, KEY> singularAttribute, Specification<ENTITY> where) {
        final CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
        final CriteriaQuery<Tuple> query = criteriaBuilder.createQuery(Tuple.class);

        final Root<ENTITY> root = query.from(getDomainClass());
        final Path<KEY> expression = root.get(singularAttribute);

        query.multiselect(expression, criteriaBuilder.count(root));
        query.select(criteriaBuilder.tuple(expression, criteriaBuilder.count(root)));
        query.where(where.toPredicate(root, query, criteriaBuilder));
        query.groupBy(expression);

        final List<Tuple> resultList = em.createQuery(query).getResultList();

        return resultList.stream()
                .collect(toMap(
                                t -> t.get(0, singularAttribute.getJavaType()),
                                t -> t.get(1, Long.class))
                );
    }
}
