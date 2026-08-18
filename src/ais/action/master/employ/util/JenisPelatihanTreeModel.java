package ais.action.master.employ.util;

import java.util.List;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zul.AbstractTreeModel;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.employ.JenisPelatihan;

public class JenisPelatihanTreeModel extends AbstractTreeModel {

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
	public JenisPelatihanTreeModel(Boolean tampilkanSemua) {
		super(null);
		this.tampilkanSemua = tampilkanSemua;
	}

	/**
	 * Constructor
	 * 
	 * @param tree
	 *            the list is contained all data of nodes.
	 */
	public JenisPelatihanTreeModel(JenisPelatihan parentJenisPelatihan,
			Boolean tampilkanSemua) {
		super(parentJenisPelatihan);
		this.tampilkanSemua = tampilkanSemua;
	}

	public List<JenisPelatihan> getChildren(JenisPelatihan parentJenisPelatihan) {
		return getChildren(parentJenisPelatihan, null);
	}

	@SuppressWarnings("unchecked")
	public List<JenisPelatihan> getChildren(JenisPelatihan parentJenisPelatihan,
			Integer index) {
		Session session = HibernateUtil.currentSession();
		List<JenisPelatihan> kategoriItems = session
				.createCriteria(JenisPelatihan.class)
				.setMaxResults(index == null ? 10000 : 1)
				.setFirstResult(index == null ? 0 : index)
				.add(tampilkanSemua ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("defaultItem", true))
				.add(parentJenisPelatihan == null ? Restrictions.isNull("parent")
						: Restrictions.eq("parent", parentJenisPelatihan))
				.addOrder(Order.asc("nama")).addOrder(Order.desc("id")).list();
		return kategoriItems;
	}

	public void generateAllChildren(JenisPelatihan parentJenisPelatihan,
			Set<JenisPelatihan> kategoriItems) {
		if (!isLeaf(parentJenisPelatihan)) {
			List<JenisPelatihan> kerjas = getChildren(parentJenisPelatihan);
			for (JenisPelatihan kategoriItem : kerjas) {
				kategoriItems.add(kategoriItem);
				generateAllChildren(kategoriItem, kategoriItems);
			}
		}
	}

	// TreeModel //
	public Object getChild(Object parent, int index) {
		JenisPelatihan parentJenisPelatihan = (JenisPelatihan) parent;
		List<JenisPelatihan> kategoriItems = getChildren(parentJenisPelatihan,
				index);
		JenisPelatihan kategoriItem = kategoriItems.size() > 0 ? kategoriItems
				.get(0) : null;
		return kategoriItem;
	}

	public int getChildCount(Object parent) {
		JenisPelatihan parentJenisPelatihan = (JenisPelatihan) parent;
		Session session = HibernateUtil.currentNativeSession();
		Integer count = ((Number) session
				.createCriteria(JenisPelatihan.class)
				.add(tampilkanSemua ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("defaultItem", true))

				.add(parentJenisPelatihan == null ? Restrictions.isNull("parent")
						: Restrictions.eq("parent", parentJenisPelatihan))
				.setProjection(Projections.rowCount()).uniqueResult())
				.intValue();

		
		HibernateUtil.closeSession();
		
		return count;
	}

	public void deleteChilds(Object parent) {
		JenisPelatihan parentJenisPelatihan = (JenisPelatihan) parent;
		Session session = HibernateUtil.currentSession();
		List<JenisPelatihan> kategoriItems = getChildren(parentJenisPelatihan);
		for (JenisPelatihan kategoriItem : kategoriItems) {
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

	public void getParentCount(JenisPelatihan kategoriItem, JenisPelatihan obj,
			List<Long> longs) {
		Session session = HibernateUtil.currentSession();
		if (kategoriItem.getParent() == null) {
			obj.setDeep(longs.size());
			Common.refreshUpdate(session,(obj));
		} else {
			JenisPelatihan parentJenisPelatihan = (JenisPelatihan) session
					.createCriteria(JenisPelatihan.class)
					.add(tampilkanSemua ? Restrictions.sqlRestriction("1=1")
							: Restrictions.eq("defaultItem", true))
					.add(Restrictions.idEq(kategoriItem.getParent().getId()))
					.uniqueResult();
			longs.add(parentJenisPelatihan.getId());
			getParentCount(parentJenisPelatihan, obj, longs);
		}

	}

	public void getParentSet(JenisPelatihan kategoriItem,
			List<JenisPelatihan> kategoriItems) {
		Session session = HibernateUtil.currentSession();
		if (kategoriItem.getParent() != null) {
			JenisPelatihan parentJenisPelatihan = (JenisPelatihan) session
					.createCriteria(JenisPelatihan.class)
					.add(tampilkanSemua ? Restrictions.sqlRestriction("1=1")
							: Restrictions.eq("defaultItem", true))
					.add(Restrictions.idEq(kategoriItem.getParent().getId()))
					.uniqueResult();
			if (parentJenisPelatihan != null) {
				kategoriItems.add(parentJenisPelatihan);
				getParentSet(parentJenisPelatihan, kategoriItems);
			}
		}

	}

	public void getChildsSet(JenisPelatihan kategoriItem,
			Set<JenisPelatihan> kategoriItems) {
		List<JenisPelatihan> childs = getChildren(kategoriItem);
		for (JenisPelatihan myJenisPelatihan : childs) {
			kategoriItems.add(myJenisPelatihan);
			if (!isLeaf(myJenisPelatihan)) {
				getChildsSet(myJenisPelatihan, kategoriItems);
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