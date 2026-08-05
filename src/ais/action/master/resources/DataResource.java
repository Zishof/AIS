package ais.action.master.resources;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.type.Type;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;

import com.sun.jersey.api.NotFoundException;

public abstract class DataResource<T> {

	@SuppressWarnings("rawtypes")
	private Class persistentClass;

	@SuppressWarnings("rawtypes")
	public DataResource(Class class1) {
		this.persistentClass = class1;
	}

	public long getSystemTime() {
		return System.currentTimeMillis();
	}

	@SuppressWarnings("unchecked")
	public T getData(String username, String password, String id) {

		if (!Common.checkLogin(username, password))
			throw new NotFoundException("fobidden access");

		try {
			Session session = HibernateUtil.currentNativeSession();
			T generalValueObject = (T) session.createCriteria(persistentClass)
					.add(Restrictions.idEq(Long.parseLong(id.trim())))
					.uniqueResult();
			
			HibernateUtil.closeSession();
			return generalValueObject;
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e); 
			throw new NotFoundException("Terjadi kesalahan internal");
		}
	}

	@SuppressWarnings({ "unchecked" })
	public List<T> getAllData(String username, String password) {

		if (!Common.checkLogin(username, password))
			throw new NotFoundException("fobidden access");

		try {
			Session session = HibernateUtil.currentNativeSession();
			List<T> generalValueObjects = (List<T>) session
					.createCriteria(persistentClass).addOrder(Order.desc("id"))
					.setMaxResults(Common.MAX_RESULT).list();
			
			HibernateUtil.closeSession();
			return generalValueObjects;
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e); 
			throw new NotFoundException("Terjadi kesalahan internal");
		}
	}

	@SuppressWarnings({ "unchecked" })
	public List<T> getAllData(String username, String password, String search) {

		if (!Common.checkLogin(username, password))
			throw new NotFoundException("fobidden access");

		try {

			ClassMetadata classMetadata = HibernateUtil
					.getClassMetadata(persistentClass);
			String[] strings = classMetadata.getPropertyNames();
			Type[] types = classMetadata.getPropertyTypes();

			Criterion criterion = Restrictions.sqlRestriction("1!=1");
			int i = 0;
			for (String s : strings) {
				Type type = types[i++];

				if (type.getReturnedClass().getName()
						.equals(String.class.getName())) {
					criterion = Restrictions.or(criterion, Restrictions.ilike(
							s, search.trim(), MatchMode.ANYWHERE));
				}

			}

			Session session = HibernateUtil.currentNativeSession();
			List<T> generalValueObjects = (List<T>) session
					.createCriteria(persistentClass).add(criterion)
					.addOrder(Order.desc("id"))
					.setMaxResults(Common.MAX_RESULT).list();
			
			HibernateUtil.closeSession();
			return generalValueObjects;
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e); 
			throw new NotFoundException("Terjadi kesalahan internal");
		}
	}

	@SuppressWarnings({ "unchecked" })
	public List<T> getAllData(String username, String password, String search,
			String search1) {

		if (!Common.checkLogin(username, password))
			throw new NotFoundException("fobidden access");

		try {

			ClassMetadata classMetadata = HibernateUtil
					.getClassMetadata(persistentClass);
			String[] strings = classMetadata.getPropertyNames();
			Type[] types = classMetadata.getPropertyTypes();

			Criterion criterion = Restrictions.sqlRestriction("1!=1");
			Criterion criterion1 = Restrictions.sqlRestriction("1!=1");
			int i = 0;
			for (String s : strings) {
				Type type = types[i++];

				if (type.getReturnedClass().getName()
						.equals(String.class.getName())) {
					criterion = Restrictions.or(criterion, Restrictions.ilike(
							s, search.trim(), MatchMode.ANYWHERE));
					criterion1 = Restrictions.or(criterion1, Restrictions
							.ilike(s, search1.trim(), MatchMode.ANYWHERE));
				}

			}

			Session session = HibernateUtil.currentNativeSession();
			List<T> generalValueObjects = (List<T>) session
					.createCriteria(persistentClass).add(criterion)
					.add(criterion1).addOrder(Order.desc("id"))
					.setMaxResults(Common.MAX_RESULT).list();
			
			HibernateUtil.closeSession();
			return generalValueObjects;
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e); 
			throw new NotFoundException("Terjadi kesalahan internal");
		}
	}
}
