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
import ais.database.model.library.KategoriItem;

public class KategoriItemTreeModel extends AbstractTreeModel {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5115651721345571411L;
	private Boolean tampilkanSemua;

	/**
	 * Constructor
	 * 
	 * @param tree
	 *            the list is contained all data of nodes.
	 */
	public KategoriItemTreeModel(Boolean tampilkanSemua) {
		super(null);
		this.tampilkanSemua = tampilkanSemua;
	}

	/**
	 * Constructor
	 * 
	 * @param tree
	 *            the list is contained all data of nodes.
	 */
	public KategoriItemTreeModel(KategoriItem parentKategoriItem,
			Boolean tampilkanSemua) {
		super(parentKategoriItem);
		this.tampilkanSemua = tampilkanSemua;
	}

	public List<KategoriItem> getChildren(KategoriItem parentKategoriItem) {
		return getChildren(parentKategoriItem, null);
	}

	@SuppressWarnings("unchecked")
	public List<KategoriItem> getChildren(KategoriItem parentKategoriItem,
			Integer index) {
		Session session = HibernateUtil.currentSession();
		List<KategoriItem> kategoriItems = session
				.createCriteria(KategoriItem.class)
				.setMaxResults(index == null ? 10000 : 1)
				.setFirstResult(index == null ? 0 : index)
				.add(tampilkanSemua ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("defaultItem", true))
				.add(parentKategoriItem == null ? Restrictions.isNull("parent")
						: Restrictions.eq("parent", parentKategoriItem))
				.addOrder(Order.asc("nama")).addOrder(Order.desc("id")).list();
		return kategoriItems;
	}

	public void generateAllChildren(KategoriItem parentKategoriItem,
			Set<KategoriItem> kategoriItems) {
		if (!isLeaf(parentKategoriItem)) {
			List<KategoriItem> kerjas = getChildren(parentKategoriItem);
			for (KategoriItem kategoriItem : kerjas) {
				kategoriItems.add(kategoriItem);
				generateAllChildren(kategoriItem, kategoriItems);
			}
		}
	}

	// TreeModel //
	public Object getChild(Object parent, int index) {
		KategoriItem parentKategoriItem = (KategoriItem) parent;
		List<KategoriItem> kategoriItems = getChildren(parentKategoriItem,
				index);
		KategoriItem kategoriItem = kategoriItems.size() > 0 ? kategoriItems
				.get(0) : null;
		return kategoriItem;
	}

	public int getChildCount(Object parent) {
		KategoriItem parentKategoriItem = (KategoriItem) parent;
		Session session = HibernateUtil.currentNativeSession();
		Integer count = ((Number) session
				.createCriteria(KategoriItem.class)
				.add(tampilkanSemua ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("defaultItem", true))

				.add(parentKategoriItem == null ? Restrictions.isNull("parent")
						: Restrictions.eq("parent", parentKategoriItem))
				.setProjection(Projections.rowCount()).uniqueResult())
				.intValue();

		HibernateUtil.closeSession();
		return count;
	}

	public void deleteChilds(Object parent) {
		KategoriItem parentKategoriItem = (KategoriItem) parent;
		Session session = HibernateUtil.currentSession();
		List<KategoriItem> kategoriItems = getChildren(parentKategoriItem);
		for (KategoriItem kategoriItem : kategoriItems) {
			if (getChildCount(kategoriItem) == 0) {
				session.delete(kategoriItem);
			} else {
				deleteChilds(kategoriItem);
			}
		}
	}

	public boolean isLeaf(Object node) {
		return (getChildCount(node) == 0);
	}

	public void getParentCount(KategoriItem kategoriItem, KategoriItem obj,
			List<Long> longs) {
		Session session = HibernateUtil.currentSession();
		if (kategoriItem.getParent() == null) {
			obj.setDeep(longs.size());
			Common.refreshUpdate(session,(obj));
		} else {
			KategoriItem parentKategoriItem = (KategoriItem) session
					.createCriteria(KategoriItem.class)
					.add(tampilkanSemua ? Restrictions.sqlRestriction("1=1")
							: Restrictions.eq("defaultItem", true))
					.add(Restrictions.idEq(kategoriItem.getParent().getId()))
					.uniqueResult();
			longs.add(parentKategoriItem.getId());
			getParentCount(parentKategoriItem, obj, longs);
		}

	}

	public void getParentSet(KategoriItem kategoriItem,
			List<KategoriItem> kategoriItems) {
		Session session = HibernateUtil.currentSession();
		if (kategoriItem.getParent() != null) {
			KategoriItem parentKategoriItem = (KategoriItem) session
					.createCriteria(KategoriItem.class)
					.add(tampilkanSemua ? Restrictions.sqlRestriction("1=1")
							: Restrictions.eq("defaultItem", true))
					.add(Restrictions.idEq(kategoriItem.getParent().getId()))
					.uniqueResult();
			if (parentKategoriItem != null) {
				kategoriItems.add(parentKategoriItem);
				getParentSet(parentKategoriItem, kategoriItems);
			}
		}

	}

	public void getChildsSet(KategoriItem kategoriItem,
			Set<KategoriItem> kategoriItems) {
		List<KategoriItem> childs = getChildren(kategoriItem);
		for (KategoriItem myKategoriItem : childs) {
			kategoriItems.add(myKategoriItem);
			if (!isLeaf(myKategoriItem)) {
				getChildsSet(myKategoriItem, kategoriItems);
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