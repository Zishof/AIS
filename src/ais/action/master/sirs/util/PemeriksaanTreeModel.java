package ais.action.master.sirs.util;

import java.util.List;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zul.AbstractTreeModel;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.sirs.Pemeriksaan;

public class PemeriksaanTreeModel extends AbstractTreeModel {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5115651721345571411L;
	private Boolean tampilkanSemua;

	/**
	 * Constructor
	 * 
	 * @param tree the list is contained all data of nodes.
	 */
	public PemeriksaanTreeModel(Boolean tampilkanSemua) {
		super(null);
		this.tampilkanSemua = tampilkanSemua;
	}

	/**
	 * Constructor
	 * 
	 * @param tree the list is contained all data of nodes.
	 */
	public PemeriksaanTreeModel(Pemeriksaan parentPemeriksaan, Boolean tampilkanSemua) {
		super(parentPemeriksaan);
		this.tampilkanSemua = tampilkanSemua;
	}

	public List<Pemeriksaan> getChildren(Pemeriksaan parentPemeriksaan) {
		return getChildren(parentPemeriksaan, null);
	}

	@SuppressWarnings("unchecked")
	public List<Pemeriksaan> getChildren(Pemeriksaan parentPemeriksaan, Integer index) {
		Session session = HibernateUtil.currentSession();
		List<Pemeriksaan> pemeriksaans = ConstantValues.simpleList(session.createCriteria(Pemeriksaan.class)
				.setMaxResults(index == null ? 10000 : 1).setFirstResult(index == null ? 0 : index)
				.add(tampilkanSemua ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("aktif", true))
				.add(parentPemeriksaan == null ? Restrictions.isNull("parent")
						: Restrictions.eq("parent", parentPemeriksaan))
				.addOrder(Order.asc("nama")).addOrder(Order.desc("id")), Pemeriksaan.class);
		return pemeriksaans;
	}

	public void generateAllChildren(Pemeriksaan parentPemeriksaan, Set<Pemeriksaan> pemeriksaans) {
		if (!isLeaf(parentPemeriksaan)) {
			List<Pemeriksaan> kerjas = getChildren(parentPemeriksaan);
			for (Pemeriksaan pemeriksaan : kerjas) {
				pemeriksaans.add(pemeriksaan);
				generateAllChildren(pemeriksaan, pemeriksaans);
			}
		}
	}

	// TreeModel //
	public Object getChild(Object parent, int index) {
		Pemeriksaan parentPemeriksaan = (Pemeriksaan) parent;
		List<Pemeriksaan> pemeriksaans = getChildren(parentPemeriksaan, index);
		Pemeriksaan pemeriksaan = pemeriksaans.size() > 0 ? pemeriksaans.get(0) : null;
		// System.out.println("index = " + index + ", pemeriksaan = "
		// + pemeriksaan);
		return pemeriksaan;
	}

	public int getChildCount(Object parent) {
		Pemeriksaan parentPemeriksaan = (Pemeriksaan) parent;
		Session session = HibernateUtil.currentSession();
		Integer count = ((Number) session.createCriteria(Pemeriksaan.class)
				.add(tampilkanSemua ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("aktif", true))

				.add(parentPemeriksaan == null ? Restrictions.isNull("parent")
						: Restrictions.eq("parent", parentPemeriksaan))
				.setProjection(Projections.rowCount()).uniqueResult()).intValue();

		return count;
	}

	public void deleteChilds(Object parent) {
		Pemeriksaan parentPemeriksaan = (Pemeriksaan) parent;
		Session session = HibernateUtil.currentSession();
		List<Pemeriksaan> pemeriksaans = getChildren(parentPemeriksaan);
		for (Pemeriksaan pemeriksaan : pemeriksaans) {
			if (getChildCount(pemeriksaan) == 0) {
				session.delete(pemeriksaan);
			} else {
				deleteChilds(pemeriksaan);
			}
		}
	}

	public boolean isLeaf(Object node) {
		return (getChildCount(node) == 0);
	}

	public void getParentCount(Pemeriksaan pemeriksaan, Pemeriksaan obj, List<Long> longs) {
		Session session = HibernateUtil.currentSession();
		if (pemeriksaan.getParent() == null) {
			obj.setDeep(longs.size());
			Common.refreshUpdate(session, obj); 
		} else {
			Pemeriksaan parentPemeriksaan = (Pemeriksaan) ConstantValues
					.simpleObject(session.createCriteria(Pemeriksaan.class)
							.add(tampilkanSemua ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("aktif", true))
							.add(Restrictions.idEq(pemeriksaan.getParent().getId())), Pemeriksaan.class);
			longs.add(parentPemeriksaan.getId());
			getParentCount(parentPemeriksaan, obj, longs);
		}

	}

	public void getParentSet(Pemeriksaan pemeriksaan, List<Pemeriksaan> pemeriksaans) {
		Session session = HibernateUtil.currentSession();
		if (pemeriksaan.getParent() != null) {
			Pemeriksaan parentPemeriksaan = (Pemeriksaan) ConstantValues
					.simpleObject(session.createCriteria(Pemeriksaan.class)
							.add(tampilkanSemua ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("aktif", true))
							.add(Restrictions.idEq(pemeriksaan.getParent().getId())), Pemeriksaan.class);
			if (parentPemeriksaan != null) {
				pemeriksaans.add(parentPemeriksaan);
				getParentSet(parentPemeriksaan, pemeriksaans);
			}
		}

	}

	public void getChildsSet(Pemeriksaan pemeriksaan, Set<Pemeriksaan> pemeriksaans) {
		List<Pemeriksaan> childs = getChildren(pemeriksaan);
		for (Pemeriksaan myPemeriksaan : childs) {
			pemeriksaans.add(myPemeriksaan);
			if (!isLeaf(myPemeriksaan)) {
				getChildsSet(myPemeriksaan, pemeriksaans);
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