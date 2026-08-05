package ais.database.dao;

import java.io.Serializable;
import java.util.Hashtable;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.LockMode;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Order;

/**
 * A super interface for common CRUD functionality. based on <a
 * href='http://www.hibernate.org/328.html'>"Generic Data Access Objects"</a>.
 * 
 * @see <a href='http://www.hibernate.org/328.html'>Generic Data Access
 *      Objects</a>
 */
public interface GenericDao<T, ID extends Serializable> {

	public Session getCurrentSession();

	public Criteria getCriteria();

	/**
	 * @param id
	 * @return Boolean
	 */
	public Boolean isExist(Serializable id);

	/**
	 * @param id
	 * @param propertyName
	 * @return kembaliannya
	 */
	public Boolean isExist(Serializable id, String propertyName);

	/**
	 * @param id
	 * @param lock
	 * @return kembaliannya
	 */
	public T findById(ID id, Boolean lock);

	public void persis(T obj);

	/**
	 * @return kembaliannya
	 */
	public List<T> findAll();

	/**
	 * @param order
	 * @return kembaliannya
	 */
	public List<T> findAll(Order order);

	/**
	 * @param order
	 * @return kembaliannya
	 */
	public List<T> findAll(Order... order);

	/**
	 * @param firstResult
	 * @param maxResults
	 * @return kembaliannya
	 */
	public List<T> findPageByPage(int firstResult, int maxResults);

	/**
	 * @param exampleInstance
	 * @param excludeProperty
	 * @return kembaliannya
	 */
	public List<T> findByExample(T exampleInstance, String... excludeProperty);

	/**
	 * @param entity
	 * @return kembaliannya
	 */
	public T makePersistent(T entity);

	/**
	 * @param entity
	 */
	public void makeTransient(T entity);

	/**
	 * @param entity
	 */
	public void refresh(T entity);

	/**
	 * @param entity
	 * @param lockMode
	 */
	public void refresh(T entity, LockMode lockMode);

	/**
	 * @param like
	 * @return kembaliannya
	 */
	public List<T> search(Hashtable<String, ? extends Object> like);

	/**
	 * @param search
	 * @return kembaliannya
	 */
	public List<T> search(String search);

	public List<T> search(String search, Order[] order,
			Criterion... criterionses);

	public List<T> search(String search, Criterion... criterionses);

	public List<T> search(String search, Order... order);

	/**
	 * @param like
	 * @param orders
	 * @return kembaliannya
	 */
	public List<T> search(Hashtable<String, ? extends Object> like,
			Order... orders);

	/**
	 * @param like
	 * @param orders
	 * @return kembaliannya
	 */
	public List<T> search(List<? extends String> like, Order... orders);

	/**
	 * @param entity
	 */
	public void save(T entity);

	/**
     *
     */
	// public void beginTransaction();
	//
	//
	// public void rollbackTransaction();
	//
	// /**
	// *
	// */
	// public void commitTransaction();

	/**
	 * @param id
	 * @return kembaliannya
	 */
	public T load(Serializable id);

	/**
	 * @param entity
	 */
	public void update(T entity);

	/**
	 * @param entity
	 * @param id
	 */
	public void saveOrUpdate(T entity, Serializable id);

	public void flush();

	public void evict(T entity);

	/**
	 * @param entity
	 */
	public void delete(T entity);

	/**
	 * @param identityName
	 * @param entity
	 */
	public void saveOrUpdate(String identityName, T entity);

	/**
	 * @param identityName
	 * @param entity
	 */
	public void update(String identityName, T entity);

	/**
	 * @param entity
	 * @return kembaliannya
	 */
	public T merge(T entity);

	/**
	 * @param criterion
	 * @return kembaliannya
	 */
	public List<T> findByCriteria(Criterion... criterion);

	public List<T> findByCriteria(Order order, Criterion... criterion);

	/**
	 * @param firstResult
	 * @param maxResults
	 * @param criterion
	 * @return kembaliannya
	 */
	public List<T> findByCriteriaPageByPage(int firstResult, int maxResults,
			Criterion... criterion);

}
