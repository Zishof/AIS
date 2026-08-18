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
import ais.database.model.library.DdcItem;
import ais.database.model.library.VersiDdcItem;

public class DdcItemTreeModel extends AbstractTreeModel {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5115651721345571411L;
	private Boolean tampilkanSemua;
	private VersiDdcItem versiDdcItem;

	/**
	 * Constructor
	 * 
	 * @param tree
	 *            the list is contained all data of nodes.
	 */
	public DdcItemTreeModel(Boolean tampilkanSemua) {
		super(null);
		this.tampilkanSemua = tampilkanSemua;
	}

	/**
	 * Constructor
	 * 
	 * @param tree
	 *            the list is contained all data of nodes.
	 */
	public DdcItemTreeModel(DdcItem parentDdcItem, Boolean tampilkanSemua) {
		super(parentDdcItem);
		this.tampilkanSemua = tampilkanSemua;
	}

	/**
	 * Constructor
	 * 
	 * @param tree
	 *            the list is contained all data of nodes.
	 */
	public DdcItemTreeModel(Boolean tampilkanSemua, VersiDdcItem versiDdcItem) {
		super(null);
		this.tampilkanSemua = tampilkanSemua;
		this.versiDdcItem = versiDdcItem;
	}

	/**
	 * Constructor
	 * 
	 * @param tree
	 *            the list is contained all data of nodes.
	 */
	public DdcItemTreeModel(DdcItem parentDdcItem, Boolean tampilkanSemua, VersiDdcItem versiDdcItem) {
		super(parentDdcItem);
		this.tampilkanSemua = tampilkanSemua;
		this.versiDdcItem = versiDdcItem;
	}

	public List<DdcItem> getChildren(DdcItem parentDdcItem) {
		return getChildren(parentDdcItem, null);
	}

	@SuppressWarnings("unchecked")
	public List<DdcItem> getChildren(DdcItem parentDdcItem, Integer index) {
		Session session = HibernateUtil.currentSession();
		List<DdcItem> ddcItems = session.createCriteria(DdcItem.class).setMaxResults(index == null ? 10000 : 1)
				.setFirstResult(index == null ? 0 : index)
				.add(tampilkanSemua ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("defaultItem", true))

				.add(versiDdcItem == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("versiDdcItem", versiDdcItem))

				.add(parentDdcItem == null ? Restrictions.isNull("parent") : Restrictions.eq("parent", parentDdcItem))
				.addOrder(Order.asc("kode")).addOrder(Order.asc("nama")).addOrder(Order.asc("id")).list();
		return ddcItems;
	}

	public void generateAllChildren(DdcItem parentDdcItem, Set<DdcItem> ddcItems) {
		if (!isLeaf(parentDdcItem)) {
			List<DdcItem> kerjas = getChildren(parentDdcItem);
			for (DdcItem ddcItem : kerjas) {
				ddcItems.add(ddcItem);
				generateAllChildren(ddcItem, ddcItems);
			}
		}
	}

	// TreeModel //
	public Object getChild(Object parent, int index) {
		DdcItem parentDdcItem = (DdcItem) parent;
		List<DdcItem> ddcItems = getChildren(parentDdcItem, index);
		DdcItem ddcItem = ddcItems.size() > 0 ? ddcItems.get(0) : null;
		return ddcItem;
	}

	public int getChildCount(Object parent) {
		DdcItem parentDdcItem = (DdcItem) parent;
		Session session = HibernateUtil.currentNativeSession();
		Integer count = ((Number) session.createCriteria(DdcItem.class)
				.add(tampilkanSemua ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("defaultItem", true))
				.add(versiDdcItem == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("versiDdcItem", versiDdcItem))
				.add(parentDdcItem == null ? Restrictions.isNull("parent") : Restrictions.eq("parent", parentDdcItem))
				.setProjection(Projections.rowCount()).uniqueResult()).intValue();

		HibernateUtil.closeSession();
		return count;
	}

	public void deleteChilds(Object parent) {
		DdcItem parentDdcItem = (DdcItem) parent;
		Session session = HibernateUtil.currentSession();
		List<DdcItem> ddcItems = getChildren(parentDdcItem);
		for (DdcItem ddcItem : ddcItems) {
			if (getChildCount(ddcItem) == 0) {
				session.delete(ddcItem);
			} else {
				deleteChilds(ddcItem);
			}
		}
	}

	public boolean isLeaf(Object node) {
		return (getChildCount(node) == 0);
	}

	public void getParentCount(DdcItem ddcItem, DdcItem obj, List<Long> longs) {
		Session session = HibernateUtil.currentSession();
		if (ddcItem.getParent() == null) {
			obj.setDeep(longs.size());
			Common.refreshUpdate(session, (obj));
		} else {
			DdcItem parentDdcItem = (DdcItem) session.createCriteria(DdcItem.class)
					.add(tampilkanSemua ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("defaultItem", true))
					.add(versiDdcItem == null ? Restrictions.sqlRestriction("1=1")
							: Restrictions.eq("versiDdcItem", versiDdcItem))
					.add(Restrictions.idEq(ddcItem.getParent().getId())).uniqueResult();
			longs.add(parentDdcItem.getId());
			getParentCount(parentDdcItem, obj, longs);
		}

	}

	public void getParentSet(DdcItem ddcItem, List<DdcItem> ddcItems) {
		Session session = HibernateUtil.currentSession();
		if (ddcItem.getParent() != null) {
			DdcItem parentDdcItem = (DdcItem) session.createCriteria(DdcItem.class)
					.add(tampilkanSemua ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("defaultItem", true))
					.add(versiDdcItem == null ? Restrictions.sqlRestriction("1=1")
							: Restrictions.eq("versiDdcItem", versiDdcItem))
					.add(Restrictions.idEq(ddcItem.getParent().getId())).uniqueResult();
			if (parentDdcItem != null) {
				ddcItems.add(parentDdcItem);
				getParentSet(parentDdcItem, ddcItems);
			}
		}

	}

	public void getChildsSet(DdcItem ddcItem, Set<DdcItem> ddcItems) {
		List<DdcItem> childs = getChildren(ddcItem);
		for (DdcItem myDdcItem : childs) {
			ddcItems.add(myDdcItem);
			if (!isLeaf(myDdcItem)) {
				getChildsSet(myDdcItem, ddcItems);
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