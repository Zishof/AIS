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
import ais.database.model.employ.UnitKerja;

public class UnitSatkerTreeModel extends AbstractTreeModel {

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
	public UnitSatkerTreeModel(Boolean tampilkanSemua) {
		super(null);
		this.tampilkanSemua = tampilkanSemua;
	}

	/**
	 * Constructor
	 * 
	 * @param tree the list is contained all data of nodes.
	 */
	public UnitSatkerTreeModel(UnitKerja parentUnitKerja, Boolean tampilkanSemua) {
		super(parentUnitKerja);
		this.tampilkanSemua = tampilkanSemua;
	}

	public List<UnitKerja> getChildren(UnitKerja parentUnitKerja) {
		return getChildren(parentUnitKerja, null);
	}

	@SuppressWarnings("unchecked")
	public List<UnitKerja> getChildren(UnitKerja parentUnitKerja, Integer index) {
		Session session = HibernateUtil.currentSession();
		List<UnitKerja> unitKerjas = session.createCriteria(UnitKerja.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.setMaxResults(index == null ? 10000 : 1).setFirstResult(index == null ? 0 : index)
				.add(tampilkanSemua ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("defaultItem", true))
				.add(parentUnitKerja == null ? Restrictions.isNull("parent")
						: Restrictions.eq("parent", parentUnitKerja))
				.addOrder(Order.asc("nama")).addOrder(Order.desc("id")).list();
		return unitKerjas;
	}

	public void generateAllChildren(UnitKerja parentUnitKerja, Set<UnitKerja> unitKerjas) {
		if (!isLeaf(parentUnitKerja)) {
			List<UnitKerja> kerjas = getChildren(parentUnitKerja);
			for (UnitKerja unitKerja : kerjas) {
				unitKerjas.add(unitKerja);
				generateAllChildren(unitKerja, unitKerjas);
			}
		}
	}

	// TreeModel //
	public Object getChild(Object parent, int index) {
		UnitKerja parentUnitKerja = (UnitKerja) parent;
		List<UnitKerja> unitKerjas = getChildren(parentUnitKerja, index);
		UnitKerja unitKerja = unitKerjas.size() > 0 ? unitKerjas.get(0) : null;
		return unitKerja;
	}

	public int getChildCount(Object parent) {
		UnitKerja parentUnitKerja = (UnitKerja) parent;
		Session session = HibernateUtil.currentNativeSession();
		Integer count = ((Number) session.createCriteria(UnitKerja.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(tampilkanSemua ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("defaultItem", true))

				.add(parentUnitKerja == null ? Restrictions.isNull("parent")
						: Restrictions.eq("parent", parentUnitKerja))
				.setProjection(Projections.rowCount()).uniqueResult()).intValue();

		HibernateUtil.closeSession();
		return count;
	}

	public void deleteChilds(Object parent) {
		UnitKerja parentUnitKerja = (UnitKerja) parent;
		Session session = HibernateUtil.currentSession();
		List<UnitKerja> unitKerjas = getChildren(parentUnitKerja);
		for (UnitKerja unitKerja : unitKerjas) {
			if (getChildCount(unitKerja) == 0) {
				session.delete(unitKerja);
			} else {
				deleteChilds(unitKerja);
			}
		}
	}

	public boolean isLeaf(Object node) {
		return (getChildCount(node) == 0);
	}

	public void getParentCount(UnitKerja unitKerja, UnitKerja obj, List<Long> longs) {
		Session session = HibernateUtil.currentSession();
		if (unitKerja.getParent() == null) {
			obj.setDeep(longs.size());
			Common.refreshUpdate(session, (obj));
		} else {
			UnitKerja parentUnitKerja = (UnitKerja) session.createCriteria(UnitKerja.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(tampilkanSemua ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("defaultItem", true))
					.add(Restrictions.idEq(unitKerja.getParent().getId())).uniqueResult();
			longs.add(parentUnitKerja.getId());
			getParentCount(parentUnitKerja, obj, longs);
		}

	}

	public void getParentSet(UnitKerja unitKerja, List<UnitKerja> unitKerjas) {
		Session session = HibernateUtil.currentSession();
		if (unitKerja.getParent() != null) {
			UnitKerja parentUnitKerja = (UnitKerja) session.createCriteria(UnitKerja.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(tampilkanSemua ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("defaultItem", true))
					.add(Restrictions.idEq(unitKerja.getParent().getId())).uniqueResult();
			if (parentUnitKerja != null) {
				unitKerjas.add(parentUnitKerja);
				getParentSet(parentUnitKerja, unitKerjas);
			}
		}

	}

	public void getChildsSet(UnitKerja unitKerja, Set<UnitKerja> unitKerjas) {
		List<UnitKerja> childs = getChildren(unitKerja);
		for (UnitKerja myUnitKerja : childs) {
			unitKerjas.add(myUnitKerja);
			if (!isLeaf(myUnitKerja)) {
				getChildsSet(myUnitKerja, unitKerjas);
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