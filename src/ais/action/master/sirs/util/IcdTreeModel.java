package ais.action.master.sirs.util;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zul.AbstractTreeModel;

import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.sirs.Icd;

public class IcdTreeModel extends AbstractTreeModel {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5115651721345571411L;

	/**
	 * Constructor
	 * 
	 * @param tree the list is contained all data of nodes.
	 */
	public IcdTreeModel() {
		super(new Icd(0L));
	}

	@SuppressWarnings("unchecked")
	public List<Icd> getChildren(Icd parentIcd) {
		Session session = HibernateUtil.currentSession();
		List<Icd> icds = ConstantValues.simpleList(
				session.createCriteria(Icd.class).add(Restrictions.eq("parentId", parentIcd.getChild()))
						.addOrder(Order.asc("child")).addOrder(Order.asc("parentId")).addOrder(Order.asc("id")),
				Icd.class);
		return icds;
	}

	// TreeModel //
	public Object getChild(Object parent, int index) {
		Icd parentIcd = (Icd) parent;

		List<Icd> icds = getChildren(parentIcd);

		Icd icd = null;

		try {
			if (icds.size() < index) {
				icd = null;
			} else {
				icd = icds.get(index);
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sirs/util/IcdTreeModel.java:55");

		}

		return icd;
	}

	public int getChildCount(Object parent) {
		Icd parentIcd = (Icd) parent;

		Session session = HibernateUtil.currentSession();

		Integer count = ((Number) session.createCriteria(Icd.class)
				.add(Restrictions.eq("parentId", parentIcd.getChild())).setProjection(Projections.rowCount())
				.uniqueResult()).intValue();

		// System.out.println("parentIcd = " + parentIcd.getChild() +
		// " count = " + count);

		return count;
	}

	public boolean isLeaf(Object node) {
		return (getChildCount(node) == 0);
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