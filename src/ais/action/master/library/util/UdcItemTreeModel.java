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
import ais.database.model.library.UdcItem;

/**
 * Tipe khusus untuk udc item tree model. Kelas ini memberi nama dan batas tanggung jawab yang
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
public class UdcItemTreeModel extends AbstractTreeModel {

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
	public UdcItemTreeModel(Boolean tampilkanSemua) {
		super(null);
		this.tampilkanSemua = tampilkanSemua;
	}

	/**
	 * Constructor
	 * 
	 * @param tree
	 *            the list is contained all data of nodes.
	 */
	public UdcItemTreeModel(UdcItem parentUdcItem, Boolean tampilkanSemua) {
		super(parentUdcItem);
		this.tampilkanSemua = tampilkanSemua;
	}

	public List<UdcItem> getChildren(UdcItem parentUdcItem) {
		return getChildren(parentUdcItem, null);
	}

	@SuppressWarnings("unchecked")
	public List<UdcItem> getChildren(UdcItem parentUdcItem, Integer index) {
		Session session = HibernateUtil.currentSession();
		List<UdcItem> udcItems = session
				.createCriteria(UdcItem.class)
				.setMaxResults(index == null ? 10000 : 1)
				.setFirstResult(index == null ? 0 : index)
				.add(tampilkanSemua ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("defaultItem", true))
				.add(parentUdcItem == null ? Restrictions.isNull("parent")
						: Restrictions.eq("parent", parentUdcItem))
				.addOrder(Order.asc("kode")).addOrder(Order.asc("nama"))
				.addOrder(Order.desc("id")).list();
		return udcItems;
	}

	public void generateAllChildren(UdcItem parentUdcItem, Set<UdcItem> udcItems) {
		if (!isLeaf(parentUdcItem)) {
			List<UdcItem> kerjas = getChildren(parentUdcItem);
			for (UdcItem udcItem : kerjas) {
				udcItems.add(udcItem);
				generateAllChildren(udcItem, udcItems);
			}
		}
	}

	// TreeModel //
	public Object getChild(Object parent, int index) {
		UdcItem parentUdcItem = (UdcItem) parent;
		List<UdcItem> udcItems = getChildren(parentUdcItem, index);
		UdcItem udcItem = udcItems.size() > 0 ? udcItems.get(0) : null;
		return udcItem;
	}

	public int getChildCount(Object parent) {
		UdcItem parentUdcItem = (UdcItem) parent;
		Session session = HibernateUtil.currentNativeSession();
		Integer count = ((Number) session
				.createCriteria(UdcItem.class)
				.add(tampilkanSemua ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("defaultItem", true))

				.add(parentUdcItem == null ? Restrictions.isNull("parent")
						: Restrictions.eq("parent", parentUdcItem))
				.setProjection(Projections.rowCount()).uniqueResult())
				.intValue();

		HibernateUtil.closeSession();
		return count;
	}

	public void deleteChilds(Object parent) {
		UdcItem parentUdcItem = (UdcItem) parent;
		Session session = HibernateUtil.currentSession();
		List<UdcItem> udcItems = getChildren(parentUdcItem);
		for (UdcItem udcItem : udcItems) {
			if (getChildCount(udcItem) == 0) {
				session.delete(udcItem);
			} else {
				deleteChilds(udcItem);
			}
		}
	}

	public boolean isLeaf(Object node) {
		return (getChildCount(node) == 0);
	}

	public void getParentCount(UdcItem udcItem, UdcItem obj, List<Long> longs) {
		Session session = HibernateUtil.currentSession();
		if (udcItem.getParent() == null) {
			obj.setDeep(longs.size());
			Common.refreshUpdate(session,(obj));
		} else {
			UdcItem parentUdcItem = (UdcItem) session
					.createCriteria(UdcItem.class)
					.add(tampilkanSemua ? Restrictions.sqlRestriction("1=1")
							: Restrictions.eq("defaultItem", true))
					.add(Restrictions.idEq(udcItem.getParent().getId()))
					.uniqueResult();
			longs.add(parentUdcItem.getId());
			getParentCount(parentUdcItem, obj, longs);
		}

	}

	public void getParentSet(UdcItem udcItem, List<UdcItem> udcItems) {
		Session session = HibernateUtil.currentSession();
		if (udcItem.getParent() != null) {
			UdcItem parentUdcItem = (UdcItem) session
					.createCriteria(UdcItem.class)
					.add(tampilkanSemua ? Restrictions.sqlRestriction("1=1")
							: Restrictions.eq("defaultItem", true))
					.add(Restrictions.idEq(udcItem.getParent().getId()))
					.uniqueResult();
			if (parentUdcItem != null) {
				udcItems.add(parentUdcItem);
				getParentSet(parentUdcItem, udcItems);
			}
		}
	}

	public void getChildsSet(UdcItem udcItem, Set<UdcItem> udcItems) {
		List<UdcItem> childs = getChildren(udcItem);
		for (UdcItem myUdcItem : childs) {
			udcItems.add(myUdcItem);
			if (!isLeaf(myUdcItem)) {
				getChildsSet(myUdcItem, udcItems);
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