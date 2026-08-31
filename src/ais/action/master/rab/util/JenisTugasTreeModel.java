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
import ais.database.model.rab.JenisTugas;

/**
 * Tipe khusus untuk jenis tugas tree model. Kelas ini memberi nama dan batas tanggung jawab yang
 * eksplisit pada perilaku yang diwarisi atau kontrak yang diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * AbstractTreeModel}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Boolean tampilkanSemua};
 * pembacaan/pencarian ({@code getChildren()}, {@code getChildren()}, {@code getChild()}, {@code
 * getChildCount()}, {@code getParentCount()}, {@code getParentSet()}); penghapusan/pembatalan ({@code
 * deleteChilds()}); operasi domain lain ({@code generateAllChildren()}, {@code isLeaf()}). Bagian lain dari
 * kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see AbstractTreeModel
 */
public class JenisTugasTreeModel extends AbstractTreeModel {

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
	public JenisTugasTreeModel(Boolean tampilkanSemua) {
		super(null);
		this.tampilkanSemua = tampilkanSemua;
	}

	/**
	 * Constructor
	 * 
	 * @param tree
	 *            the list is contained all data of nodes.
	 */
	public JenisTugasTreeModel(JenisTugas parentJenisTugas,
			Boolean tampilkanSemua) {
		super(parentJenisTugas);
		this.tampilkanSemua = tampilkanSemua;
	}

	public List<JenisTugas> getChildren(JenisTugas parentJenisTugas) {
		return getChildren(parentJenisTugas, null);
	}

	@SuppressWarnings("unchecked")
	public List<JenisTugas> getChildren(JenisTugas parentJenisTugas,
			Integer index) {
		Session session = HibernateUtil.currentSession();
		List<JenisTugas> jenisTugass = session
				.createCriteria(JenisTugas.class)
				.setMaxResults(index == null ? 10000 : 1)
				.setFirstResult(index == null ? 0 : index)
				.add(tampilkanSemua ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("defaultItem", true))
				.add(parentJenisTugas == null ? Restrictions.isNull("parent")
						: Restrictions.eq("parent", parentJenisTugas))
				.addOrder(Order.asc("nama")).addOrder(Order.desc("id")).list();
		return jenisTugass;
	}

	public void generateAllChildren(JenisTugas parentJenisTugas,
			Set<JenisTugas> jenisTugass) {
		if (!isLeaf(parentJenisTugas)) {
			List<JenisTugas> kerjas = getChildren(parentJenisTugas);
			for (JenisTugas jenisTugas : kerjas) {
				jenisTugass.add(jenisTugas);
				generateAllChildren(jenisTugas, jenisTugass);
			}
		}
	}

	// TreeModel //
	public Object getChild(Object parent, int index) {
		JenisTugas parentJenisTugas = (JenisTugas) parent;
		List<JenisTugas> jenisTugass = getChildren(parentJenisTugas, index);
		JenisTugas jenisTugas = jenisTugass.size() > 0 ? jenisTugass.get(0)
				: null;
		// System.out.println("index = " + index + ", jenisTugas = " + jenisTugas);
		return jenisTugas;
	}

	public int getChildCount(Object parent) {
		JenisTugas parentJenisTugas = (JenisTugas) parent;
		Session session = HibernateUtil.currentNativeSession();
		Integer count = ((Number) session
				.createCriteria(JenisTugas.class)
				.add(tampilkanSemua ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("defaultItem", true))

				.add(parentJenisTugas == null ? Restrictions.isNull("parent")
						: Restrictions.eq("parent", parentJenisTugas))
				.setProjection(Projections.rowCount()).uniqueResult())
				.intValue();

		HibernateUtil.closeSession();
		return count;
	}

	public void deleteChilds(Object parent) {
		JenisTugas parentJenisTugas = (JenisTugas) parent;
		Session session = HibernateUtil.currentSession();
		List<JenisTugas> jenisTugass = getChildren(parentJenisTugas);
		for (JenisTugas jenisTugas : jenisTugass) {
			if (getChildCount(jenisTugas) == 0) {
				session.delete(jenisTugas);
			} else {
				deleteChilds(jenisTugas);
			}
		}
	}

	public boolean isLeaf(Object node) {
		return (getChildCount(node) == 0);
	}

	public void getParentCount(JenisTugas jenisTugas, JenisTugas obj,
			List<Long> longs) {
		Session session = HibernateUtil.currentSession();
		if (jenisTugas.getParent() == null) {
			obj.setDeep(longs.size());
			Common.refreshUpdate(session,(obj));
		} else {
			JenisTugas parentJenisTugas = (JenisTugas) session
					.createCriteria(JenisTugas.class)
					.add(tampilkanSemua ? Restrictions.sqlRestriction("1=1")
							: Restrictions.eq("defaultItem", true))
					.add(Restrictions.idEq(jenisTugas.getParent().getId()))
					.uniqueResult();
			longs.add(parentJenisTugas.getId());
			getParentCount(parentJenisTugas, obj, longs);
		}

	}

	public void getParentSet(JenisTugas jenisTugas, List<JenisTugas> jenisTugass) {
		Session session = HibernateUtil.currentSession();
		if (jenisTugas.getParent() != null) {
			JenisTugas parentJenisTugas = (JenisTugas) session
					.createCriteria(JenisTugas.class)
					.add(tampilkanSemua ? Restrictions.sqlRestriction("1=1")
							: Restrictions.eq("defaultItem", true))
					.add(Restrictions.idEq(jenisTugas.getParent().getId()))
					.uniqueResult();
			if (parentJenisTugas != null) {
				jenisTugass.add(parentJenisTugas);
				getParentSet(parentJenisTugas, jenisTugass);
			}
		}

	}

	public void getChildsSet(JenisTugas jenisTugas, Set<JenisTugas> jenisTugass) {
		List<JenisTugas> childs = getChildren(jenisTugas);
		for (JenisTugas myJenisTugas : childs) {
			jenisTugass.add(myJenisTugas);
			if (!isLeaf(myJenisTugas)) {
				getChildsSet(myJenisTugas, jenisTugass);
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