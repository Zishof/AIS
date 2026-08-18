package ais.action.master.library.util;

import java.util.List;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zul.AbstractTreeModel;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.library.DomainPenelitian;
import ais.database.model.library.Penerbit;

public class DomainPenelitianTreeModel extends AbstractTreeModel {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5115651721345571411L;
	private Boolean tampilkanSemua;
	private Penerbit penerbit;

	/**
	 * Constructor
	 * 
	 * @param tree
	 *            the list is contained all data of nodes.
	 */
	public DomainPenelitianTreeModel(Boolean tampilkanSemua, Penerbit penerbit) {
		super(null);
		this.penerbit = penerbit;
		this.tampilkanSemua = tampilkanSemua;
	}

	/**
	 * Constructor
	 * 
	 * @param tree
	 *            the list is contained all data of nodes.
	 */
	public DomainPenelitianTreeModel(DomainPenelitian parentDomainPenelitian,
			Boolean tampilkanSemua) {
		super(parentDomainPenelitian);
		this.tampilkanSemua = tampilkanSemua;
	}

	public List<DomainPenelitian> getChildren(
			DomainPenelitian parentDomainPenelitian) {
		return getChildren(parentDomainPenelitian, null);
	}

	@SuppressWarnings("unchecked")
	public List<DomainPenelitian> getChildren(
			DomainPenelitian parentDomainPenelitian, Integer index) {
		Session session = HibernateUtil.currentSession();
		List<DomainPenelitian> domainPenelitians = session
				.createCriteria(DomainPenelitian.class)
				.setMaxResults(index == null ? 10000 : 1)
				.setFirstResult(index == null ? 0 : index)
				.add(penerbit == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("penerbit", penerbit))
				.add(tampilkanSemua ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("defaultItem", true))
				.add(parentDomainPenelitian == null ? Restrictions
						.isNull("parent") : Restrictions.eq("parent",
						parentDomainPenelitian)).addOrder(Order.asc("nama"))
				.addOrder(Order.desc("id")).list();
		return domainPenelitians;
	}

	public void generateAllChildren(DomainPenelitian parentDomainPenelitian,
			Set<DomainPenelitian> domainPenelitians) {
		if (!isLeaf(parentDomainPenelitian)) {
			List<DomainPenelitian> kerjas = getChildren(parentDomainPenelitian);
			for (DomainPenelitian domainPenelitian : kerjas) {
				domainPenelitians.add(domainPenelitian);
				generateAllChildren(domainPenelitian, domainPenelitians);
			}
		}
	}

	// TreeModel //
	public Object getChild(Object parent, int index) {
		DomainPenelitian parentDomainPenelitian = (DomainPenelitian) parent;
		List<DomainPenelitian> domainPenelitians = getChildren(
				parentDomainPenelitian, index);
		DomainPenelitian domainPenelitian = domainPenelitians.size() > 0 ? domainPenelitians
				.get(0) : null;
		return domainPenelitian;
	}

	public int getChildCount(Object parent) {
		DomainPenelitian parentDomainPenelitian = (DomainPenelitian) parent;
		Session session = HibernateUtil.currentNativeSession();
		Integer count = ((Number) session
				.createCriteria(DomainPenelitian.class)
				.add(tampilkanSemua ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("defaultItem", true))
				.add(penerbit == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("penerbit", penerbit))
				.add(parentDomainPenelitian == null ? Restrictions
						.isNull("parent") : Restrictions.eq("parent",
						parentDomainPenelitian))
				.setProjection(Projections.rowCount()).uniqueResult())
				.intValue();

		HibernateUtil.closeSession();
		return count;
	}

	public void deleteChilds(Object parent) {
		DomainPenelitian parentDomainPenelitian = (DomainPenelitian) parent;
		Session session = HibernateUtil.currentSession();
		List<DomainPenelitian> domainPenelitians = getChildren(parentDomainPenelitian);
		for (DomainPenelitian domainPenelitian : domainPenelitians) {
			if (getChildCount(domainPenelitian) == 0) {
				session.delete(domainPenelitian);
			} else {
				deleteChilds(domainPenelitian);
			}
		}
	}

	public boolean isLeaf(Object node) {
		return (getChildCount(node) == 0);
	}

	public void getParentCount(DomainPenelitian domainPenelitian,
			DomainPenelitian obj, List<Long> longs) {
		Session session = HibernateUtil.currentSession();
		if (domainPenelitian.getParent() == null) {
			obj.setDeep(longs.size());
			Common.refreshUpdate(session,(obj));
		} else {
			DomainPenelitian parentDomainPenelitian = (DomainPenelitian) session
					.createCriteria(DomainPenelitian.class)
					.add(tampilkanSemua ? Restrictions.sqlRestriction("1=1")
							: Restrictions.eq("defaultItem", true))
					.add(penerbit == null ? Restrictions.sqlRestriction("1=1")
							: Restrictions.eq("penerbit", penerbit))
					.add(Restrictions
							.idEq(domainPenelitian.getParent().getId()))
					.uniqueResult();
			longs.add(parentDomainPenelitian.getId());
			getParentCount(parentDomainPenelitian, obj, longs);
		}

	}

	public void getParentSet(DomainPenelitian domainPenelitian,
			List<DomainPenelitian> domainPenelitians) {
		Session session = HibernateUtil.currentSession();
		if (domainPenelitian.getParent() != null) {
			DomainPenelitian parentDomainPenelitian = (DomainPenelitian) session
					.createCriteria(DomainPenelitian.class)
					.add(tampilkanSemua ? Restrictions.sqlRestriction("1=1")
							: Restrictions.eq("defaultItem", true))
					.add(penerbit == null ? Restrictions.sqlRestriction("1=1")
							: Restrictions.eq("penerbit", penerbit))
					.add(Restrictions
							.idEq(domainPenelitian.getParent().getId()))
					.uniqueResult();
			if (parentDomainPenelitian != null) {
				domainPenelitians.add(parentDomainPenelitian);
				getParentSet(parentDomainPenelitian, domainPenelitians);
			}
		}

	}

	public void getChildsSet(DomainPenelitian domainPenelitian,
			Set<DomainPenelitian> domainPenelitians) {
		List<DomainPenelitian> childs = getChildren(domainPenelitian);
		for (DomainPenelitian myDomainPenelitian : childs) {
			domainPenelitians.add(myDomainPenelitian);
			if (!isLeaf(myDomainPenelitian)) {
				getChildsSet(myDomainPenelitian, domainPenelitians);
			}
		}
	}

	/**
	 * @since 5.0.6
	 * @see org.zkoss.zul.TreeModel#getIndexOfChild(java.lang.Object,
	 *      java.lang.Object)
	 */
	@Override
	public int getIndexOfChild(Object arg0, Object arg1) {
		return 0;
	}

}