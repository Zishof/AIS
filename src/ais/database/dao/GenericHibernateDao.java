package ais.database.dao;

/**
 * Created by IntelliJ IDEA.
 * User: 
 * Date: Jan 3, 2007
 * Time: 3:56:57 PM
 */

import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.LockMode;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Example;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.type.Type;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;

public abstract class GenericHibernateDao<T, ID extends Serializable, DaoImpl extends GenericDao<T, ID>>
		implements GenericDao<T, ID> {

	private Class<T> persistentClass;
	private Session session;
	private Integer maxRowCount = 500;

	@SuppressWarnings("unchecked")
	public GenericHibernateDao() {
		this.persistentClass = (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass())
				.getActualTypeArguments()[0];
	}

	@SuppressWarnings("unchecked")
	public DaoImpl setSession(Session s) {
		session = s;
		return (DaoImpl) this;
	}

	public Criteria getCriteria() {
		Criteria crit = getSession().createCriteria(getPersistentClass());
		return crit;
	}

	public Session getCurrentSession() {
		return getSession();
	}

	protected Session getSession() {
		if (session == null || !session.isOpen() || !session.isOpen()) {
			session = HibernateUtil.currentSession();
		}
		return session;
	}

	protected Class<T> getPersistentClass() {
		return persistentClass;
	}

	@SuppressWarnings({ "unchecked", "deprecation" })
	public T findById(ID id, Boolean lock) {
		T entity;
		if (lock) {
			entity = (T) getSession().load(getPersistentClass(), id, LockMode.UPGRADE);
		} else {
			entity = (T) getSession().load(getPersistentClass(), id);
		}

		return entity;
	}

	public Boolean isExist(Serializable id) {
		ClassMetadata classMetadata = HibernateUtil.getClassMetadata(getPersistentClass());
		String s = classMetadata.getIdentifierPropertyName();
		Criteria crit = getSession().createCriteria(getPersistentClass());
		crit.setMaxResults(1);
		crit.add(Restrictions.eq(s, id));
		@SuppressWarnings("rawtypes")
		List list = crit.setMaxResults(ais.common.Common.MAX_RESULT).list();
		return list.size() != 0;
	}

	public Boolean isExist(Serializable id, String propertyName) {
		Criteria crit = getSession().createCriteria(getPersistentClass());
		crit.setMaxResults(1);
		crit.add(Restrictions.eq(propertyName, id));
		@SuppressWarnings("rawtypes")
		List list = crit.setMaxResults(ais.common.Common.MAX_RESULT).list();
		return list.size() != 0;
	}

	public void persis(T obj) {
		getSession().persist(obj);
	}

	public List<T> findAll() {
		return findByCriteria();
	}

	@SuppressWarnings("unchecked")
	public List<T> findAll(Order order) {
		Criteria crit = getSession().createCriteria(getPersistentClass());
		crit.setMaxResults(maxRowCount);
		crit.addOrder(order);
		return crit.setMaxResults(ais.common.Common.MAX_RESULT).list();
	}

	@SuppressWarnings("unchecked")
	public List<T> findAll(Order... order) {
		Criteria crit = getSession().createCriteria(getPersistentClass());
		crit.setMaxResults(maxRowCount);
		for (Order odr : order) {
			crit.addOrder(odr);
		}
		return crit.setMaxResults(ais.common.Common.MAX_RESULT).list();
	}

	public List<T> findPageByPage(int firstResult, int maxResults) {
		return findByCriteriaPageByPage(firstResult, maxResults);
	}

	@SuppressWarnings("unchecked")
	public List<T> findByExample(T exampleInstance, String... excludeProperty) {
		Criteria crit = getSession().createCriteria(getPersistentClass());
		crit.setMaxResults(maxRowCount);
		Example example = Example.create(exampleInstance);
		example = example.enableLike(MatchMode.ANYWHERE);
		for (String exclude : excludeProperty) {
			example.excludeProperty(exclude);
		}
		crit.add(example);
		return crit.setMaxResults(ais.common.Common.MAX_RESULT).list();
	}

	protected Criteria createCriteria() {
		return getSession().createCriteria(getPersistentClass());
	}

	@SuppressWarnings("unchecked")
	public List<T> search(Hashtable<String, ? extends Object> like) {
		Criteria crit = getSession().createCriteria(getPersistentClass());
		crit.setMaxResults(maxRowCount);
		Enumeration<String> enu = like.keys();
		while (enu.hasMoreElements()) {
			String col = enu.nextElement();
			crit.add(Restrictions.like(col, String.valueOf(like.get(col)), MatchMode.ANYWHERE));
		}
		return crit.setMaxResults(ais.common.Common.MAX_RESULT).list();
	}

	@SuppressWarnings("unchecked")
	public List<T> search(String search) {
		Criteria crit = getSession().createCriteria(getPersistentClass());
		crit.setMaxResults(maxRowCount);
		ClassMetadata classMetadata = HibernateUtil.getClassMetadata(persistentClass);
		String[] property = classMetadata.getPropertyNames();

		Type[] types = classMetadata.getPropertyTypes();
		List<Criterion> criterions = new ArrayList<Criterion>();
		String indentifier = classMetadata.getIdentifierPropertyName();
		String indentifierType = classMetadata.getIdentifierType().getName();
		if (indentifierType.equalsIgnoreCase("String")) {
			criterions.add(Restrictions.ilike(indentifier, search, MatchMode.ANYWHERE));
		}
		int i = 0;
		for (String aProperty : property) {
			Type type = types[i];
			if (type.getName().equalsIgnoreCase("String")) {
				criterions.add(Restrictions.ilike(aProperty, search, MatchMode.ANYWHERE));
			}
			i++;
		}

		if (criterions.size() == 0) {
			return crit.setMaxResults(ais.common.Common.MAX_RESULT).list();
		}

		Criterion acriterion = criterions.get(0);
		for (Criterion criterion : criterions) {
			acriterion = Restrictions.or(acriterion, criterion);
		}
		crit.add(acriterion);

		return crit.setMaxResults(ais.common.Common.MAX_RESULT).list();
	}

	@SuppressWarnings("unchecked")
	public List<T> search(String search, Order[] order, Criterion... criterionses) {
		Criteria crit = getSession().createCriteria(getPersistentClass());
		crit.setMaxResults(maxRowCount);
		ClassMetadata classMetadata = HibernateUtil.getClassMetadata(persistentClass);
		String[] property = classMetadata.getPropertyNames();

		Type[] types = classMetadata.getPropertyTypes();
		List<Criterion> criterions = new ArrayList<Criterion>();
		String indentifier = classMetadata.getIdentifierPropertyName();
		String indentifierType = classMetadata.getIdentifierType().getName();
		if (indentifierType.equalsIgnoreCase("String")) {
			criterions.add(Restrictions.ilike(indentifier, search, MatchMode.ANYWHERE));
		}
		int i = 0;
		for (String aProperty : property) {
			Type type = types[i];
			if (type.getName().equalsIgnoreCase("String")) {
				criterions.add(Restrictions.ilike(aProperty, search, MatchMode.ANYWHERE));
			}
			i++;
		}

		if (criterions.size() != 0) {
			Criterion acriterion = criterions.get(0);
			for (Criterion criterion : criterions) {
				acriterion = Restrictions.or(acriterion, criterion);
			}
			crit.add(acriterion);
		}
		for (Criterion criterion : criterionses) {
			if (criterion != null) {
				crit.add(criterion);
			}
		}
		for (Order o : order) {
			crit.addOrder(o);
		}
		return crit.setMaxResults(ais.common.Common.MAX_RESULT).list();
	}

	@SuppressWarnings("unchecked")
	public List<T> search(String search, Criterion... criterionses) {
		Criteria crit = getSession().createCriteria(getPersistentClass());
		crit.setMaxResults(maxRowCount);
		ClassMetadata classMetadata = HibernateUtil.getClassMetadata(persistentClass);
		String[] property = classMetadata.getPropertyNames();

		Type[] types = classMetadata.getPropertyTypes();
		List<Criterion> criterions = new ArrayList<Criterion>();
		String indentifier = classMetadata.getIdentifierPropertyName();
		String indentifierType = classMetadata.getIdentifierType().getName();
		if (indentifierType.equalsIgnoreCase("String")) {
			criterions.add(Restrictions.ilike(indentifier, search, MatchMode.ANYWHERE));
		}
		int i = 0;
		for (String aProperty : property) {
			Type type = types[i];
			if (type.getName().equalsIgnoreCase("String")) {
				criterions.add(Restrictions.ilike(aProperty, search, MatchMode.ANYWHERE));
			}
			i++;
		}

		if (criterions.size() != 0) {
			Criterion acriterion = criterions.get(0);
			for (Criterion criterion : criterions) {
				acriterion = Restrictions.or(acriterion, criterion);
			}
			crit.add(acriterion);
		}
		for (Criterion criterion : criterionses) {
			if (criterion != null) {
				crit.add(criterion);
			}
		}
		return crit.setMaxResults(ais.common.Common.MAX_RESULT).list();
	}

	@SuppressWarnings("unchecked")
	public List<T> search(String search, Order... order) {
		Criteria crit = getSession().createCriteria(getPersistentClass());
		crit.setMaxResults(maxRowCount);
		ClassMetadata classMetadata = HibernateUtil.getClassMetadata(persistentClass);
		String[] property = classMetadata.getPropertyNames();

		Type[] types = classMetadata.getPropertyTypes();
		List<Criterion> criterions = new ArrayList<Criterion>();
		String indentifier = classMetadata.getIdentifierPropertyName();
		String indentifierType = classMetadata.getIdentifierType().getName();
		if (indentifierType.equalsIgnoreCase("String")) {
			criterions.add(Restrictions.ilike(indentifier, search, MatchMode.ANYWHERE));
		}
		int i = 0;
		for (String aProperty : property) {
			Type type = types[i];
			if (type.getName().equalsIgnoreCase("String")) {
				criterions.add(Restrictions.ilike(aProperty, search, MatchMode.ANYWHERE));
			}
			i++;
		}

		if (criterions.size() != 0) {
			Criterion acriterion = criterions.get(0);
			for (Criterion criterion : criterions) {
				acriterion = Restrictions.or(acriterion, criterion);
			}
			crit.add(acriterion);
		}

		for (Order o : order) {
			crit.addOrder(o);
		}

		return crit.setMaxResults(ais.common.Common.MAX_RESULT).list();
	}

	@SuppressWarnings("unchecked")
	public List<T> search(Hashtable<String, ? extends Object> like, Order... orders) {
		Criteria crit = getSession().createCriteria(getPersistentClass());
		crit.setMaxResults(maxRowCount);
		for (Order order : orders) {
			crit.addOrder(order);
		}
		Enumeration<String> enu = like.keys();
		while (enu.hasMoreElements()) {
			String col = enu.nextElement();
			crit.add(Restrictions.like(col, String.valueOf(like.get(col)), MatchMode.ANYWHERE));
		}
		return crit.setMaxResults(ais.common.Common.MAX_RESULT).list();
	}

	@SuppressWarnings("unchecked")
	public List<T> search(List<? extends String> like, Order... orders) {
		Criteria crit = getSession().createCriteria(getPersistentClass());
		crit.setMaxResults(maxRowCount);
		for (Order order : orders) {
			crit.addOrder(order);
		}
		for (String s : like) {
			crit.add(Restrictions.sqlRestriction(s));
		}
		return crit.setMaxResults(ais.common.Common.MAX_RESULT).list();
	}

	public T makePersistent(T entity) {
		getSession().saveOrUpdate(entity);
		return entity;
	}

	public void makeTransient(T entity) {
		getSession().delete(entity);
	}

	public void refresh(T entity) {
		getSession().refresh(entity);
	}

	public void refresh(T entity, LockMode lockMode) {
		getSession().refresh(entity, lockMode);
	}

	public void save(T entity) {
		getSession().save(entity);
	}

	@SuppressWarnings("unchecked")
	public T load(Serializable id) {
		return (T) getSession().load(getPersistentClass(), id);
	}

	public void update(T entity) {
		Common.refreshUpdate(getSession(), (GeneralValueObject) entity); 
	}

	public void update(String identityName, T entity) {
		getSession().update(identityName, entity);
	}

	public void saveOrUpdate(T entity, Serializable id) {
		if (isExist(id)) {
			getSession().update(merge(entity));
		} else {
			getSession().save(entity);
		}
	}

	public void saveOrUpdate(String identityName, T entity) {
		getSession().saveOrUpdate(identityName, entity);
	}

	public void delete(T entity) {
		getSession().delete((entity));
	}

	public void flush() {
		getSession().flush();
	}

	public void evict(T entity) {
		getSession().evict((entity));
	}

	@SuppressWarnings("unchecked")
	public T merge(T entity) {
		return (T) getSession().merge(entity);
	}

	@SuppressWarnings("unchecked")
	public List<T> findByCriteria(Criterion... criterion) {
		Criteria crit = getSession().createCriteria(getPersistentClass());
		crit.setMaxResults(maxRowCount);
		for (Criterion c : criterion) {
			crit.add(c);
		}
		return crit.setMaxResults(ais.common.Common.MAX_RESULT).list();
	}

	@SuppressWarnings("unchecked")
	public List<T> findByCriteria() {
		Criteria crit = getSession().createCriteria(getPersistentClass());
		crit.setMaxResults(maxRowCount);
		return crit.setMaxResults(ais.common.Common.MAX_RESULT).list();
	}

	@SuppressWarnings("unchecked")
	public List<T> findByCriteria(Order order) {
		Criteria crit = getSession().createCriteria(getPersistentClass());
		crit.setMaxResults(maxRowCount);
		crit.addOrder(order);
		return crit.setMaxResults(ais.common.Common.MAX_RESULT).list();
	}

	@SuppressWarnings("unchecked")
	public List<T> findByCriteria(Order order, Criterion... criterion) {
		Criteria crit = getSession().createCriteria(getPersistentClass());
		crit.setMaxResults(maxRowCount);
		crit.addOrder(order);
		for (Criterion c : criterion) {
			crit.add(c);
		}
		return crit.setMaxResults(ais.common.Common.MAX_RESULT).list();
	}

	@SuppressWarnings("unchecked")
	public List<T> findByCriteriaPageByPage(int firstResult, int maxResults, Criterion... criterion) {
		Criteria criteria = getSession().createCriteria(getPersistentClass());
		criteria.setMaxResults(maxRowCount);
		for (Criterion c : criterion) {
			criteria.add(c);
		}
		criteria.setFirstResult(firstResult);
		criteria.setMaxResults(maxResults);
		return criteria.setMaxResults(ais.common.Common.MAX_RESULT).list();
	}

	@SuppressWarnings("unchecked")
	public List<T> findByCriteriaPageByPage(int firstResult, int maxResults) {
		Criteria criteria = getSession().createCriteria(getPersistentClass());
		criteria.setMaxResults(maxRowCount);
		criteria.setFirstResult(firstResult);
		criteria.setMaxResults(maxResults);
		return criteria.setMaxResults(ais.common.Common.MAX_RESULT).list();
	}

}