package ais.action.master.akunting.util;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zul.AbstractTreeModel;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.akunting.Akun;

/**
 * Tipe khusus untuk old akun tree model. Kelas ini memberi nama dan batas tanggung jawab yang
 * eksplisit pada perilaku yang diwarisi atau kontrak yang diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * AbstractTreeModel}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Integer debetCredit};
 * pembacaan/pencarian ({@code getChildren()}, {@code getChild()}, {@code getChildCount()}, {@code
 * getIndexOfChild()}); operasi domain lain ({@code isLeaf()}). Bagian lain dari kontrak tetap mengikuti kelas
 * induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see AbstractTreeModel
 */
public class OldAkunTreeModel extends AbstractTreeModel {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5115651721345571411L;
	private Integer debetCredit;

	/**
	 * Constructor
	 * 
	 * @param tree
	 *            the list is contained all data of nodes.
	 */
	public OldAkunTreeModel(Integer debetCredit) {
		super(new Akun("-1"));
		this.debetCredit = debetCredit;
	}

	public OldAkunTreeModel() {
		super(new Akun("-1"));
		this.debetCredit = null;
	}

	@SuppressWarnings("unchecked")
	public List<Akun> getChildren(Akun parentAkun) {
		String parentKode = parentAkun.getKode().toLowerCase().trim();
		Session session = HibernateUtil.currentSession();
		List<Akun> akuns = session
				.createCriteria(Akun.class)
				.add(debetCredit == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("debetCredit", debetCredit))
				.add(Restrictions.sqlRestriction("trim(kode) != '" + parentKode
						+ "' and ((trim(kode) ilike '" + parentKode
						+ "%' and char_length(trim(kode)) = "
						+ (parentKode.length() + 2) + ") or ('-1' = '"
						+ parentKode + "' and char_length(trim(kode)) = 2))"))
				.addOrder(Order.asc("kode")).list();
		return akuns;
	}

	// TreeModel //
	public Object getChild(Object parent, int index) {
		Akun parentAkun = (Akun) parent;

		List<Akun> akuns = getChildren(parentAkun);

		Akun akun = null;

		if (akuns.size() < index) {
			akun = null;
		} else {
			akun = akuns.get(index);
		}

		return akun;
	}

	public int getChildCount(Object parent) {
		Akun parentAkun = (Akun) parent;
		String parentKode = parentAkun.getKode().toLowerCase().trim();

		Session session = HibernateUtil.currentSession();

		Integer count = ((Number) session
				.createCriteria(Akun.class)
				.add(debetCredit == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("debetCredit", debetCredit))
				.add(Restrictions.sqlRestriction("trim(kode) != '" + parentKode
						+ "' and ((trim(kode) ilike '" + parentKode
						+ "%' and char_length(trim(kode)) = "
						+ (parentKode.length() + 2) + ") or ('-1' = '"
						+ parentKode + "' and char_length(trim(kode)) = 2))"))
				.setProjection(Projections.rowCount()).uniqueResult())
				.intValue();

		return count;
	}

	public boolean isLeaf(Object node) {
		return (getChildCount(node) == 0);
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