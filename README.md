# Spring Data: Extending the JPA specification executor #

> [Spring Data JPA takes the concept of a specification from Eric Evans' book "Domain Driven Design", following the same semantics and providing an API to define such specifications using the JPA criteria API. To support specifications you can extend your repository interface with the JpaSpecificationExecutor interface.](http://docs.spring.io/spring-data/jpa/docs/1.8.0.RELEASE/reference/html/#specifications)

The [JpaSpecificationExecutor](http://docs.spring.io/spring-data/jpa/docs/current/api/org/springframework/data/jpa/repository/JpaSpecificationExecutor.html) has only a limited set of search methods. Sometimes we want to create complex queries which are not supported out of the box.
In this post I will describe how to add generic behaviour to our repositories without breaking the Spring Data way of working. 
## Problem Description ##

Lets assume the following domain model:

	@Entity
	@Table
	public class Employee {

    	@Id
    	private Long id;

    	@Column
    	private String firstName;

    	@Column
    	private String lastName;

    	@Column
    	@Type(type = "org.jadira.usertype.dateandtime.threeten.PersistentLocalDateTime")
    	private LocalDateTime dateOfBirth;

    	@Enumerated(EnumType.STRING)
    	private Department department;
			
		// Getters ..
	}
	
	public enum Department {
    	IT, HR, COMMUNICATION
	}

We want to count the amount of employees, born after 1975 and group the results by department.
This can be done by a simple SQL query, but how would we solve this using the specifications only approach?
Furthermore, we want to add this behaviour to all repositories.

## Solution ##

### Create your own JpaSpecificationExecutor ###

We want too add functionality to the [JpaSpecificationExecutor](http://docs.spring.io/spring-data/jpa/docs/current/api/org/springframework/data/jpa/repository/JpaSpecificationExecutor.html), we do this by extending this interface and add our desired methods.
We don’t want Spring Data to implement this interface so we annotate it with the [NoRepositoryBean](http://docs.spring.io/spring-data/commons/docs/current/api/org/springframework/data/repository/NoRepositoryBean.html) annotation.

	@NoRepositoryBean
	public interface MyJpaSpecificationExecutor<ENTITY> extends JpaSpecificationExecutor<ENTITY> {

		<KEY> Map<KEY, Long> groupAndCount(SingularAttribute<ENTITY, KEY> singularAttribute, Specification<ENTITY> where);
	}


### Create your own SimpleJpaRepository ###

After create our custom interface, we should implement our desired functionality. We don’t want to recreate all existing functionality so we extend the [SimpleJpaRepository](http://docs.spring.io/spring-data/jpa/docs/current/api/org/springframework/data/jpa/repository/support/SimpleJpaRepository.html).

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


### Create your own JpaRepositoryFactoryBean ###

By default, Spring will implement our repositories using the [SimpleJpaRepository](http://docs.spring.io/spring-data/jpa/docs/current/api/org/springframework/data/jpa/repository/support/SimpleJpaRepository.html). We will change this behaviour by creating a new factory.
This factory extends [JpaRepositoryFactoryBean](http://docs.spring.io/spring-data/jpa/docs/current/api/org/springframework/data/jpa/repository/support/JpaRepositoryFactoryBean.html). Define your custom repository as the repository base class.

	public class MyRepositoryFactoryBean<ENTITY, ID extends Serializable, REPO extends JpaRepository<ENTITY, ID>>
			extends JpaRepositoryFactoryBean<REPO, ENTITY, ID> {

		protected RepositoryFactorySupport createRepositoryFactory(EntityManager em) {
			return new MyRepositoryFactory(em);
		}

		private static class MyRepositoryFactory<ENTITY, ID extends Serializable>
				extends JpaRepositoryFactory {

			private final EntityManager em;

			public MyRepositoryFactory(EntityManager em) {
				super(em);
				this.em = em;
			}

			protected Object getTargetRepository(RepositoryMetadata metadata) {
				return new MySimpleJpaRepository<ENTITY, ID>((Class<ENTITY>) metadata.getDomainType(), em);
			}

			protected Class<?> getRepositoryBaseClass(RepositoryMetadata metadata) {
				return MySimpleJpaRepository.class;
			}
		}

	}

### Let Spring know it should use your factory ###

Add your factory within the [EnableJpaRepositories](http://docs.spring.io/spring-data/jpa/docs/current/api/org/springframework/data/jpa/repository/config/EnableJpaRepositories.html) annotation.

	@SpringBootApplication
	@EnableJpaRepositories(repositoryFactoryBeanClass = MyRepositoryFactoryBean.class)
	public class App {
	}

### Use your new JpaSpecificationExecutor ###

Repository:

	public interface EmployeeRepository extends JpaRepository<Employee, Long>, MyJpaSpecificationExecutor<Employee> {
	}

Test:

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


## Conclusion ##

As you can see we have added our custom behaviour and made it available for all repositories that implement our custom [JpaSpecificationExecutor](http://docs.spring.io/spring-data/jpa/docs/current/api/org/springframework/data/jpa/repository/JpaSpecificationExecutor.html).
Using this approach, you can easily add additional behaviour without code duplication.

## References ##

http://docs.spring.io/spring-data/jpa/docs/1.8.0.RELEASE/reference/html/
