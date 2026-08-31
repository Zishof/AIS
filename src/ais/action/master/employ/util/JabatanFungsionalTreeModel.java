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
import ais.database.model.employ.JabatanFungsional;

/**
 * Tipe khusus untuk jabatan fungsional tree model. Kelas ini memberi nama dan batas tanggung jawab
 * yang eksplisit pada perilaku yang diwarisi atau kontrak yang diimplementasikannya.
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
public class JabatanFungsionalTreeModel extends AbstractTreeModel {

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
	public JabatanFungsionalTreeModel(Boolean tampilkanSemua) {
		super(null);
		this.tampilkanSemua = tampilkanSemua;
	}

	/**
	 * Constructor
	 * 
	 * @param tree
	 *            the list is contained all data of nodes.
	 */
	public JabatanFungsionalTreeModel(
			JabatanFungsional parentJabatanFungsional, Boolean tampilkanSemua) {
		super(parentJabatanFungsional);
		this.tampilkanSemua = tampilkanSemua;
	}

	public List<JabatanFungsional> getChildren(
			JabatanFungsional parentJabatanFungsional) {
		return getChildren(parentJabatanFungsional, null);
	}

	@SuppressWarnings("unchecked")
	public List<JabatanFungsional> getChildren(
			JabatanFungsional parentJabatanFungsional, Integer index) {
		Session session = HibernateUtil.currentSession();
		List<JabatanFungsional> jabatanFungsionals = session
				.createCriteria(JabatanFungsional.class)
				.setMaxResults(index == null ? 10000 : 1)
				.setFirstResult(index == null ? 0 : index)
				.add(tampilkanSemua ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("defaultItem", true))
				.add(parentJabatanFungsional == null ? Restrictions
						.isNull("parent") : Restrictions.eq("parent",
						parentJabatanFungsional)).addOrder(Order.asc("nama"))
				.addOrder(Order.desc("id")).list();
		return jabatanFungsionals;
	}

	public void generateAllChildren(JabatanFungsional parentJabatanFungsional,
			Set<JabatanFungsional> jabatanFungsionals) {
		if (!isLeaf(parentJabatanFungsional)) {
			List<JabatanFungsional> kerjas = getChildren(parentJabatanFungsional);
			for (JabatanFungsional jabatanFungsional : kerjas) {
				jabatanFungsionals.add(jabatanFungsional);
				generateAllChildren(jabatanFungsional, jabatanFungsionals);
			}
		}
	}

	// TreeModel //
	public Object getChild(Object parent, int index) {
		JabatanFungsional parentJabatanFungsional = (JabatanFungsional) parent;
		List<JabatanFungsional> jabatanFungsionals = getChildren(
				parentJabatanFungsional, index);
		JabatanFungsional jabatanFungsional = jabatanFungsionals.size() > 0 ? jabatanFungsionals
				.get(0) : null;
		return jabatanFungsional;
	}

	public int getChildCount(Object parent) {
		JabatanFungsional parentJabatanFungsional = (JabatanFungsional) parent;
		Session session = HibernateUtil.currentNativeSession();
		Integer count = ((Number) session
				.createCriteria(JabatanFungsional.class)
				.add(tampilkanSemua ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("defaultItem", true))

				.add(parentJabatanFungsional == null ? Restrictions
						.isNull("parent") : Restrictions.eq("parent",
						parentJabatanFungsional))
				.setProjection(Projections.rowCount()).uniqueResult())
				.intValue();

		
		HibernateUtil.closeSession();
		
		return count;
	}

	public void deleteChilds(Object parent) {
		JabatanFungsional parentJabatanFungsional = (JabatanFungsional) parent;
		Session session = HibernateUtil.currentSession();
		List<JabatanFungsional> jabatanFungsionals = getChildren(parentJabatanFungsional);
		for (JabatanFungsional jabatanFungsional : jabatanFungsionals) {
			if (getChildCount(jabatanFungsional) == 0) {
				session.delete(jabatanFungsional);
			} else {
				deleteChilds(jabatanFungsional);
			}
		}
	}

	public boolean isLeaf(Object node) {
		return (getChildCount(node) == 0);
	}

	public void getParentCount(JabatanFungsional jabatanFungsional,
			JabatanFungsional obj, List<Long> longs) {
		Session session = HibernateUtil.currentSession();
		if (jabatanFungsional.getParent() == null) {
			obj.setDeep(longs.size());
			Common.refreshUpdate(session,(obj));
		} else {
			JabatanFungsional parentJabatanFungsional = (JabatanFungsional) session
					.createCriteria(JabatanFungsional.class)
					.add(tampilkanSemua ? Restrictions.sqlRestriction("1=1")
							: Restrictions.eq("defaultItem", true))
					.add(Restrictions.idEq(jabatanFungsional.getParent()
							.getId())).uniqueResult();
			longs.add(parentJabatanFungsional.getId());
			getParentCount(parentJabatanFungsional, obj, longs);
		}

	}

	public void getParentSet(JabatanFungsional jabatanFungsional,
			List<JabatanFungsional> jabatanFungsionals) {
		Session session = HibernateUtil.currentSession();
		if (jabatanFungsional.getParent() != null) {
			JabatanFungsional parentJabatanFungsional = (JabatanFungsional) session
					.createCriteria(JabatanFungsional.class)
					.add(tampilkanSemua ? Restrictions.sqlRestriction("1=1")
							: Restrictions.eq("defaultItem", true))
					.add(Restrictions.idEq(jabatanFungsional.getParent()
							.getId())).uniqueResult();
			if (parentJabatanFungsional != null) {
				jabatanFungsionals.add(parentJabatanFungsional);
				getParentSet(parentJabatanFungsional, jabatanFungsionals);
			}
		}

	}

	public void getChildsSet(JabatanFungsional jabatanFungsional,
			Set<JabatanFungsional> jabatanFungsionals) {
		List<JabatanFungsional> childs = getChildren(jabatanFungsional);
		for (JabatanFungsional myJabatanFungsional : childs) {
			jabatanFungsionals.add(myJabatanFungsional);
			if (!isLeaf(myJabatanFungsional)) {
				getChildsSet(myJabatanFungsional, jabatanFungsionals);
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