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
import ais.database.model.rab.ChecklistLaporanDetailDefault;

/**
 * Tipe khusus untuk checklist laporan detail default tree model. Kelas ini memberi nama dan batas
 * tanggung jawab yang eksplisit pada perilaku yang diwarisi atau kontrak yang
 * diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * AbstractTreeModel}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah pembacaan/pencarian ({@code getChildren()}, {@code
 * getChildren()}, {@code getChild()}, {@code getChildCount()}, {@code getParentCount()}, {@code
 * getParentSet()}); penghapusan/pembatalan ({@code deleteChilds()}); operasi domain lain ({@code
 * generateAllChildren()}, {@code isLeaf()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface
 * yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see AbstractTreeModel
 */
public class ChecklistLaporanDetailDefaultTreeModel extends AbstractTreeModel {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5115651721345571411L;

	/**
	 * Constructor
	 * 
	 * @param tree
	 *            the list is contained all data of nodes.
	 */
	public ChecklistLaporanDetailDefaultTreeModel() {
		super(null);
	}

	/**
	 * Constructor
	 * 
	 * @param tree
	 *            the list is contained all data of nodes.
	 */
	public ChecklistLaporanDetailDefaultTreeModel(
			ChecklistLaporanDetailDefault parentChecklistLaporanDetailDefault) {
		super(parentChecklistLaporanDetailDefault);
	}

	public List<ChecklistLaporanDetailDefault> getChildren(
			ChecklistLaporanDetailDefault parentChecklistLaporanDetailDefault) {
		return getChildren(parentChecklistLaporanDetailDefault, null);
	}

	@SuppressWarnings("unchecked")
	public List<ChecklistLaporanDetailDefault> getChildren(
			ChecklistLaporanDetailDefault parentChecklistLaporanDetailDefault,
			Integer index) {
		Session session = HibernateUtil.currentSession();
		List<ChecklistLaporanDetailDefault> checklistLaporanDetailDefaults = session
				.createCriteria(ChecklistLaporanDetailDefault.class)

				.setMaxResults(index == null ? 10000 : 1)
				.setFirstResult(index == null ? 0 : index)
				.add(parentChecklistLaporanDetailDefault == null ? Restrictions
						.isNull("parent") : Restrictions.eq("parent",
						parentChecklistLaporanDetailDefault))
				.addOrder(Order.asc("kode")).addOrder(Order.asc("nama"))
				.addOrder(Order.desc("id")).list();
		return checklistLaporanDetailDefaults;
	}

	public void generateAllChildren(
			ChecklistLaporanDetailDefault parentChecklistLaporanDetailDefault,
			Set<ChecklistLaporanDetailDefault> checklistLaporanDetailDefaults) {
		if (!isLeaf(parentChecklistLaporanDetailDefault)) {
			List<ChecklistLaporanDetailDefault> kerjas = getChildren(parentChecklistLaporanDetailDefault);
			for (ChecklistLaporanDetailDefault checklistLaporanDetailDefault : kerjas) {
				checklistLaporanDetailDefaults
						.add(checklistLaporanDetailDefault);
				generateAllChildren(checklistLaporanDetailDefault,
						checklistLaporanDetailDefaults);
			}
		}
	}

	// TreeModel //
	public Object getChild(Object parent, int index) {
		ChecklistLaporanDetailDefault parentChecklistLaporanDetailDefault = (ChecklistLaporanDetailDefault) parent;
		List<ChecklistLaporanDetailDefault> checklistLaporanDetailDefaults = getChildren(
				parentChecklistLaporanDetailDefault, index);
		ChecklistLaporanDetailDefault checklistLaporanDetailDefault = checklistLaporanDetailDefaults
				.size() > 0 ? checklistLaporanDetailDefaults.get(0) : null;
		// System.out.println("index = " + index
		// + ", checklistLaporanDetailDefault = "
		// + checklistLaporanDetailDefault);
		return checklistLaporanDetailDefault;
	}

	public int getChildCount(Object parent) {
		ChecklistLaporanDetailDefault parentChecklistLaporanDetailDefault = (ChecklistLaporanDetailDefault) parent;
		Session session = HibernateUtil.currentNativeSession();
		Integer count = ((Number) session
				.createCriteria(ChecklistLaporanDetailDefault.class)

				.add(parentChecklistLaporanDetailDefault == null ? Restrictions
						.isNull("parent") : Restrictions.eq("parent",
						parentChecklistLaporanDetailDefault))
				.setProjection(Projections.rowCount()).uniqueResult())
				.intValue();

		HibernateUtil.closeSession();
		return count;
	}

	public void deleteChilds(Object parent) {
		ChecklistLaporanDetailDefault parentChecklistLaporanDetailDefault = (ChecklistLaporanDetailDefault) parent;
		Session session = HibernateUtil.currentSession();
		List<ChecklistLaporanDetailDefault> checklistLaporanDetailDefaults = getChildren(parentChecklistLaporanDetailDefault);
		for (ChecklistLaporanDetailDefault checklistLaporanDetailDefault : checklistLaporanDetailDefaults) {
			if (getChildCount(checklistLaporanDetailDefault) == 0) {
				session.delete(checklistLaporanDetailDefault);
			} else {
				deleteChilds(checklistLaporanDetailDefault);
			}
		}
	}

	public boolean isLeaf(Object node) {
		return (getChildCount(node) == 0);
	}

	public void getParentCount(
			ChecklistLaporanDetailDefault checklistLaporanDetailDefault,
			ChecklistLaporanDetailDefault obj, List<Long> longs) {
		Session session = HibernateUtil.currentSession();
		if (checklistLaporanDetailDefault.getParent() == null) {
			obj.setDeep(longs.size());
			Common.refreshUpdate(session,(obj));
		} else {
			ChecklistLaporanDetailDefault parentChecklistLaporanDetailDefault = (ChecklistLaporanDetailDefault) session
					.createCriteria(ChecklistLaporanDetailDefault.class)

					.add(Restrictions.idEq(checklistLaporanDetailDefault
							.getParent().getId())).uniqueResult();
			longs.add(parentChecklistLaporanDetailDefault.getId());
			getParentCount(parentChecklistLaporanDetailDefault, obj, longs);
		}

	}

	public void getParentSet(
			ChecklistLaporanDetailDefault checklistLaporanDetailDefault,
			List<ChecklistLaporanDetailDefault> checklistLaporanDetailDefaults) {
		Session session = HibernateUtil.currentSession();
		if (checklistLaporanDetailDefault.getParent() != null) {
			ChecklistLaporanDetailDefault parentChecklistLaporanDetailDefault = (ChecklistLaporanDetailDefault) session
					.createCriteria(ChecklistLaporanDetailDefault.class)
					.add(Restrictions.idEq(checklistLaporanDetailDefault
							.getParent().getId())).uniqueResult();
			if (parentChecklistLaporanDetailDefault != null) {
				checklistLaporanDetailDefaults
						.add(parentChecklistLaporanDetailDefault);
				getParentSet(parentChecklistLaporanDetailDefault,
						checklistLaporanDetailDefaults);
			}
		}

	}

	public void getChildsSet(
			ChecklistLaporanDetailDefault checklistLaporanDetailDefault,
			Set<ChecklistLaporanDetailDefault> checklistLaporanDetailDefaults) {
		List<ChecklistLaporanDetailDefault> childs = getChildren(checklistLaporanDetailDefault);
		for (ChecklistLaporanDetailDefault myChecklistLaporanDetailDefault : childs) {
			checklistLaporanDetailDefaults.add(myChecklistLaporanDetailDefault);
			if (!isLeaf(myChecklistLaporanDetailDefault)) {
				getChildsSet(myChecklistLaporanDetailDefault,
						checklistLaporanDetailDefaults);
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