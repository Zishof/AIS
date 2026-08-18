package ais.action.master.rab.util;

import java.util.List;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zul.AbstractTreeModel;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.rab.ChecklistLaporan;
import ais.database.model.rab.ChecklistLaporanDetail;

public class ChecklistLaporanDetailTreeModel extends AbstractTreeModel {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5115651721345571411L;
	private ChecklistLaporan checklistLaporan;

	/**
	 * Constructor
	 * 
	 * @param tree
	 *            the list is contained all data of nodes.
	 */
	public ChecklistLaporanDetailTreeModel(ChecklistLaporan checklistLaporan) {
		super(null);
		this.checklistLaporan = checklistLaporan;
	}

	/**
	 * Constructor
	 * 
	 * @param tree
	 *            the list is contained all data of nodes.
	 */
	public ChecklistLaporanDetailTreeModel(
			ChecklistLaporanDetail parentChecklistLaporanDetail) {
		super(parentChecklistLaporanDetail);
		this.checklistLaporan = parentChecklistLaporanDetail
				.getChecklistLaporan();
	}

	public List<ChecklistLaporanDetail> getChildren(
			ChecklistLaporanDetail parentChecklistLaporanDetail) {
		return getChildren(parentChecklistLaporanDetail, null);
	}

	@SuppressWarnings("unchecked")
	public List<ChecklistLaporanDetail> getChildren(
			ChecklistLaporanDetail parentChecklistLaporanDetail, Integer index) {
		Session session = HibernateUtil.currentSession();
		List<ChecklistLaporanDetail> checklistLaporanDetails = session
				.createCriteria(ChecklistLaporanDetail.class)
				.add(Restrictions.eq("checklistLaporan", checklistLaporan))
				.setMaxResults(index == null ? 10000 : 1)
				.setFirstResult(index == null ? 0 : index)
				.add(parentChecklistLaporanDetail == null ? Restrictions
						.isNull("parent") : Restrictions.eq("parent",
						parentChecklistLaporanDetail))
				.addOrder(Order.asc("kode")).addOrder(Order.asc("nama"))
				.addOrder(Order.desc("id")).list();
		return checklistLaporanDetails;
	}

	public void generateAllChildren(
			ChecklistLaporanDetail parentChecklistLaporanDetail,
			Set<ChecklistLaporanDetail> checklistLaporanDetails) {
		if (!isLeaf(parentChecklistLaporanDetail)) {
			List<ChecklistLaporanDetail> kerjas = getChildren(parentChecklistLaporanDetail);
			for (ChecklistLaporanDetail checklistLaporanDetail : kerjas) {
				checklistLaporanDetails.add(checklistLaporanDetail);
				generateAllChildren(checklistLaporanDetail,
						checklistLaporanDetails);
			}
		}
	}

	// TreeModel //
	public Object getChild(Object parent, int index) {
		ChecklistLaporanDetail parentChecklistLaporanDetail = (ChecklistLaporanDetail) parent;
		List<ChecklistLaporanDetail> checklistLaporanDetails = getChildren(
				parentChecklistLaporanDetail, index);
		ChecklistLaporanDetail checklistLaporanDetail = checklistLaporanDetails
				.size() > 0 ? checklistLaporanDetails.get(0) : null;
		// System.out.println("index = " + index + ", checklistLaporanDetail = "
		// + checklistLaporanDetail);
		return checklistLaporanDetail;
	}

	public int getChildCount(Object parent) {
		ChecklistLaporanDetail parentChecklistLaporanDetail = (ChecklistLaporanDetail) parent;
		Session session = HibernateUtil.currentNativeSession();
		Integer count = ((Number) session
				.createCriteria(ChecklistLaporanDetail.class)
				.add(Restrictions.eq("checklistLaporan", checklistLaporan))
				.add(parentChecklistLaporanDetail == null ? Restrictions
						.isNull("parent") : Restrictions.eq("parent",
						parentChecklistLaporanDetail))
				.setProjection(Projections.rowCount()).uniqueResult())
				.intValue();

		HibernateUtil.closeSession();
		return count;
	}

	public void deleteChilds(Object parent) {
		ChecklistLaporanDetail parentChecklistLaporanDetail = (ChecklistLaporanDetail) parent;
		Session session = HibernateUtil.currentSession();
		List<ChecklistLaporanDetail> checklistLaporanDetails = getChildren(parentChecklistLaporanDetail);
		for (ChecklistLaporanDetail checklistLaporanDetail : checklistLaporanDetails) {
			if (getChildCount(checklistLaporanDetail) == 0) {
				session.delete(checklistLaporanDetail);
			} else {
				deleteChilds(checklistLaporanDetail);
			}
		}
	}

	public boolean isLeaf(Object node) {
		return (getChildCount(node) == 0);
	}

	public void getParentCount(ChecklistLaporanDetail checklistLaporanDetail,
			ChecklistLaporanDetail obj, List<Long> longs) {
		Session session = HibernateUtil.currentSession();
		if (checklistLaporanDetail.getParent() == null) {
			obj.setDeep(longs.size());
			Common.refreshUpdate(session,(obj));
		} else {
			ChecklistLaporanDetail parentChecklistLaporanDetail = (ChecklistLaporanDetail) session
					.createCriteria(ChecklistLaporanDetail.class)
					.add(Restrictions.eq("checklistLaporan", checklistLaporan))
					.add(Restrictions.idEq(checklistLaporanDetail.getParent()
							.getId())).uniqueResult();
			longs.add(parentChecklistLaporanDetail.getId());
			getParentCount(parentChecklistLaporanDetail, obj, longs);
		}

	}

	public void getParentSet(ChecklistLaporanDetail checklistLaporanDetail,
			List<ChecklistLaporanDetail> checklistLaporanDetails) {
		Session session = HibernateUtil.currentSession();
		if (checklistLaporanDetail.getParent() != null) {
			ChecklistLaporanDetail parentChecklistLaporanDetail = (ChecklistLaporanDetail) session
					.createCriteria(ChecklistLaporanDetail.class)
					.add(Restrictions.eq("checklistLaporan", checklistLaporan))
					.add(Restrictions.idEq(checklistLaporanDetail.getParent()
							.getId())).uniqueResult();
			if (parentChecklistLaporanDetail != null) {
				checklistLaporanDetails.add(parentChecklistLaporanDetail);
				getParentSet(parentChecklistLaporanDetail,
						checklistLaporanDetails);
			}
		}

	}

	public void getChildsSet(ChecklistLaporanDetail checklistLaporanDetail,
			Set<ChecklistLaporanDetail> checklistLaporanDetails) {
		List<ChecklistLaporanDetail> childs = getChildren(checklistLaporanDetail);
		for (ChecklistLaporanDetail myChecklistLaporanDetail : childs) {
			checklistLaporanDetails.add(myChecklistLaporanDetail);
			if (!isLeaf(myChecklistLaporanDetail)) {
				getChildsSet(myChecklistLaporanDetail, checklistLaporanDetails);
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