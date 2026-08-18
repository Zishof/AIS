package ais.action.master.library.util;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zul.AbstractTreeModel;

import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.library.Item;
import ais.database.model.library.TipeItem;
import ais.database.model.rab.SatuanKerja;

public class ItemTreeModel extends AbstractTreeModel {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5115651721345571411L;
	private Boolean tampilkanSemua;
	private TipeItem tipeItem;
	private Set<SatuanKerja> satuanKerjas;
	private SatuanKerjaTreeModel satuanKerjaTreeModel;
	private Boolean folderSaja = false;

	/**
	 * Constructor
	 * 
	 * @param tree
	 *            the list is contained all data of nodes.
	 */
	public ItemTreeModel(Boolean tampilkanSemua, TipeItem tipeItem,
			SatuanKerja satuanKerja, Item parent, Boolean folderSaja) {
		super(parent);
		this.folderSaja = folderSaja;
		satuanKerjaTreeModel = new SatuanKerjaTreeModel(satuanKerja, false);
		this.satuanKerjas = new HashSet<SatuanKerja>();
		this.satuanKerjas.add(satuanKerja);
		satuanKerjaTreeModel.generateAllChildren(satuanKerja, satuanKerjas);
		this.tipeItem = tipeItem;
		this.tampilkanSemua = tampilkanSemua;
	}

	/**
	 * Constructor
	 * 
	 * @param tree
	 *            the list is contained all data of nodes.
	 */
	public ItemTreeModel(Boolean tampilkanSemua, TipeItem tipeItem,
			SatuanKerja satuanKerja) {
		super(null);
		satuanKerjaTreeModel = new SatuanKerjaTreeModel(satuanKerja, false);
		this.satuanKerjas = new HashSet<SatuanKerja>();
		this.satuanKerjas.add(satuanKerja);
		satuanKerjaTreeModel.generateAllChildren(satuanKerja, satuanKerjas);
		this.tipeItem = tipeItem;
		this.tampilkanSemua = tampilkanSemua;
	}

	/**
	 * Constructor
	 * 
	 * @param tree
	 *            the list is contained all data of nodes.
	 */
	public ItemTreeModel(Item parentItem, Boolean tampilkanSemua,
			TipeItem tipeItem, SatuanKerja satuanKerja) {
		super(parentItem);
		satuanKerjaTreeModel = new SatuanKerjaTreeModel(satuanKerja, false);
		this.satuanKerjas = new HashSet<SatuanKerja>();
		this.satuanKerjas.add(satuanKerja);
		satuanKerjaTreeModel.generateAllChildren(satuanKerja, satuanKerjas);
		this.tipeItem = tipeItem;
		this.tampilkanSemua = tampilkanSemua;
	}

	public List<Item> getChildren(Item parentItem) {
		return getChildren(parentItem, null);
	}

	@SuppressWarnings("unchecked")
	public List<Item> getChildren(Item parentItem, Integer index) {
		Session session = HibernateUtil.currentSession();
		List<Item> items = session
				.createCriteria(Item.class)
				.add(folderSaja ? Restrictions.eq("folder", true)
						: Restrictions.sqlRestriction("1=1"))
				.add(Restrictions.in("defaultSatuanKerja", satuanKerjas))
				.add(tipeItem == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("tipeItem", tipeItem))
				.setMaxResults(index == null ? 10000 : 1)
				.setFirstResult(index == null ? 0 : index)
				.add(tampilkanSemua ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("defaultItem", true))
				.add(parentItem == null ? Restrictions.and(
						Restrictions.isNull("parent"),
						Restrictions.eq("folder", true)) : Restrictions.eq(
						"parent", parentItem)).addOrder(Order.asc("urutan"))
				.addOrder(Order.asc("nama")).addOrder(Order.desc("id")).list();
		return items;
	}

	public void generateAllChildren(Item parentItem, Set<Item> items) {
		if (!isLeaf(parentItem)) {
			List<Item> kerjas = getChildren(parentItem);
			for (Item item : kerjas) {
				items.add(item);
				generateAllChildren(item, items);
			}
		}
	}

	// TreeModel //
	public Object getChild(Object parent, int index) {
		Item parentItem = (Item) parent;
		List<Item> items = getChildren(parentItem, index);
		Item item = items.size() > 0 ? items.get(0) : null;
		return item;
	}

	public int getChildCount(Object parent) {
		Item parentItem = (Item) parent;
		Session session = HibernateUtil.currentNativeSession();
		Integer count = ((Number) session
				.createCriteria(Item.class)
				.add(folderSaja ? Restrictions.eq("folder", true)
						: Restrictions.sqlRestriction("1=1"))
				.add(Restrictions.in("defaultSatuanKerja", satuanKerjas))

				.add(tipeItem == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("tipeItem", tipeItem))
				.add(tampilkanSemua ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("defaultItem", true))
				.add(parentItem == null ? Restrictions.isNull("parent")
						: Restrictions.eq("parent", parentItem))
				.setProjection(Projections.rowCount()).uniqueResult())
				.intValue();

		HibernateUtil.closeSession();
		return count;
	}

	public void deleteChilds(Object parent) {
		Item parentItem = (Item) parent;
		Session session = HibernateUtil.currentSession();
		List<Item> items = getChildren(parentItem);
		for (Item item : items) {
			if (getChildCount(item) == 0 && item.getParent().getFolder()) {
				session.delete(item);
			} else {
				deleteChilds(item);
			}
		}
	}

	public boolean isLeaf(Object node) {
		return (getChildCount(node) == 0);
	}

	public void getParentCount(Item item, Item obj, List<Long> longs) {
		Session session = HibernateUtil.currentSession();
		if (item.getParent() == null) {
			obj.setDeep(longs.size());
			Common.refreshUpdate(session,(obj));
		} else {
			Item parentItem = (Item) session
					.createCriteria(Item.class)
					.add(folderSaja ? Restrictions.eq("folder", true)
							: Restrictions.sqlRestriction("1=1"))
					.add(Restrictions.in("defaultSatuanKerja", satuanKerjas))

					.add(tipeItem == null ? Restrictions.sqlRestriction("1=1")
							: Restrictions.eq("tipeItem", tipeItem))
					.add(tampilkanSemua ? Restrictions.sqlRestriction("1=1")
							: Restrictions.eq("defaultItem", true))
					.add(Restrictions.idEq(item.getParent().getId()))
					.uniqueResult();
			longs.add(parentItem.getId());
			getParentCount(parentItem, obj, longs);
		}

	}

	public void getParentSet(Item item, List<Item> items) {
		Session session = HibernateUtil.currentSession();
		if (item.getParent() != null) {
			Item parentItem = (Item) session
					.createCriteria(Item.class)
					.add(folderSaja ? Restrictions.eq("folder", true)
							: Restrictions.sqlRestriction("1=1"))
					.add(Restrictions.in("defaultSatuanKerja", satuanKerjas))
					.add(tipeItem == null ? Restrictions.sqlRestriction("1=1")
							: Restrictions.eq("tipeItem", tipeItem))
					.add(tampilkanSemua ? Restrictions.sqlRestriction("1=1")
							: Restrictions.eq("defaultItem", true))
					.add(Restrictions.idEq(item.getParent().getId()))
					.uniqueResult();
			if (parentItem != null) {
				items.add(parentItem);
				getParentSet(parentItem, items);
			}
		}

	}

	public void getChildsSet(Item item, Set<Item> items) {
		List<Item> childs = getChildren(item);
		for (Item myItem : childs) {
			items.add(myItem);
			if (!isLeaf(myItem)) {
				getChildsSet(myItem, items);
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