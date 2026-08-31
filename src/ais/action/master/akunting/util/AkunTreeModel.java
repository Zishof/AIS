package ais.action.master.akunting.util;

import java.util.List;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zul.AbstractTreeModel;

import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Tbmuser;
import ais.database.model.akunting.Akun;
import ais.database.model.akunting.GrupAkun;
import ais.database.model.rab.SatuanKerja;

/**
 * Tipe khusus untuk akun tree model. Kelas ini memberi nama dan batas tanggung jawab yang
 * eksplisit pada perilaku yang diwarisi atau kontrak yang diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * AbstractTreeModel}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Integer debetCredit}, {@code
 * SatuanKerja satuanKerja}, {@code SatuanKerjaTreeModel satuanKerjaTreeModel}, {@code AmbilDataSatuanKerjaBanbox
 * ambilDataSatuanKerjaBanbox}, {@code GrupAkun grupAkun}, {@code String dimulai}; pembacaan/pencarian ({@code
 * getChildDeepSet()}, {@code getChildren()}, {@code getChild()}, {@code getChildCount()}, {@code
 * getIndexOfChild()}); operasi domain lain ({@code generateAllChildren()}, {@code isLeaf()}); konfigurasi
 * constructor: {@code satuanKerja}, {@code satuanKerjaTreeModel}. Bagian lain dari kontrak tetap mengikuti kelas
 * induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see AbstractTreeModel
 */
public class AkunTreeModel extends AbstractTreeModel {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5115651721345571411L;
	private Integer debetCredit;
	private SatuanKerja satuanKerja = null;
	private SatuanKerjaTreeModel satuanKerjaTreeModel;
	private AmbilDataSatuanKerjaBanbox ambilDataSatuanKerjaBanbox;
	private GrupAkun grupAkun = null;
	private String dimulai = null;

	public AkunTreeModel(Integer debetCredit, AmbilDataSatuanKerjaBanbox ambilDataSatuanKerjaBanbox) {
		super(null);
		this.debetCredit = debetCredit;
		this.ambilDataSatuanKerjaBanbox = ambilDataSatuanKerjaBanbox;
		satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);
	}

	public AkunTreeModel(Integer debetCredit, AmbilDataSatuanKerjaBanbox ambilDataSatuanKerjaBanbox,
			GrupAkun grupAkun) {
		super(null);
		this.debetCredit = debetCredit;
		this.grupAkun = grupAkun;
		this.ambilDataSatuanKerjaBanbox = ambilDataSatuanKerjaBanbox;
		satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);
	}

	public AkunTreeModel(Integer debetCredit, AmbilDataSatuanKerjaBanbox ambilDataSatuanKerjaBanbox, GrupAkun grupAkun,
			String dimulai) {
		super(null);
		this.debetCredit = debetCredit;
		this.dimulai = dimulai;
		this.grupAkun = grupAkun;
		this.ambilDataSatuanKerjaBanbox = ambilDataSatuanKerjaBanbox;
		satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);
	}

	/**
	 * Constructor
	 * 
	 * @param tree the list is contained all data of nodes.
	 */
	public AkunTreeModel(Integer debetCredit) {
		super(null);
		this.debetCredit = debetCredit;
		satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);
		PerguruanTinggi pt = PerguruanTinggiUtil.getPerguruanTinggi();
		satuanKerja = pt == null ? null : pt.getSatuanKerja();
	}

	public AkunTreeModel() {
		super(null);
		this.debetCredit = null;
		PerguruanTinggi pt = PerguruanTinggiUtil.getPerguruanTinggi();
		satuanKerja = pt == null ? null : pt.getSatuanKerja();
		satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);
	}

	public AkunTreeModel(Akun parentAkun) {
		super(parentAkun);
		this.debetCredit = null;
		satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);
		Tbmuser tbmuser = Common.getCurrentUser();
		satuanKerja = tbmuser == null ? null : tbmuser.ambilSatuanKerja();
	}

	@SuppressWarnings("unchecked")
	public void getChildDeepSet(Long parent, List<Long> longs) {

		if (ambilDataSatuanKerjaBanbox != null && ambilDataSatuanKerjaBanbox.getAttribute("satuanKerja") != null) {
			satuanKerja = (SatuanKerja) ambilDataSatuanKerjaBanbox.getAttribute("satuanKerja");
		}

		Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
		if (satuanKerja != null) {
			satuanKerjas.add(satuanKerja);
			satuanKerjaTreeModel.getChildsSet(satuanKerja, satuanKerjas);
		}

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(Akun.class)

				.add(grupAkun != null ? Restrictions.eq("grupAkun", grupAkun) : Restrictions.sqlRestriction("true"))

				.add(Restrictions.eq("parent.id", parent))

				.add(satuanKerjas == null || satuanKerjas.size() == 0 ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.isNull("satuanKerja"),
								Restrictions.or(
										parent == null ? Restrictions.isNull("satuanKerja")
												: Restrictions.sqlRestriction("false"),
										Restrictions.in("satuanKerja", satuanKerjas))))

				.setProjection(Projections.property("id"));

		if (dimulai != null && !dimulai.trim().isEmpty()) {
			Criterion criterion = Restrictions.sqlRestriction("false");
			for (String s : dimulai.split(";")) {
				criterion = Restrictions.or(criterion, Restrictions.ilike("kode", s, MatchMode.START));
			}
			criteria.add(criterion);
		}

		List<Long> values = criteria.list();

		if (values.size() > 0) {
			longs.add(values.get(0));
			getChildDeepSet(values.get(0), longs);
		}
	}

	@SuppressWarnings("unchecked")
	public List<Akun> getChildren(Akun parentAkun) {
		if (ambilDataSatuanKerjaBanbox != null && ambilDataSatuanKerjaBanbox.getAttribute("satuanKerja") != null) {
			satuanKerja = (SatuanKerja) ambilDataSatuanKerjaBanbox.getAttribute("satuanKerja");
		}

		Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
		if (satuanKerja != null) {
			satuanKerjas.add(satuanKerja);
			satuanKerjaTreeModel.getChildsSet(satuanKerja, satuanKerjas);
		}

		Session session = HibernateUtil.currentSession();

		Criteria criteria = session.createCriteria(Akun.class)
				.add(grupAkun != null ? Restrictions.eq("grupAkun", grupAkun) : Restrictions.sqlRestriction("true"))
				.add(debetCredit == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("debetCredit", debetCredit))
				.add(parentAkun == null ? Restrictions.isNull("parent") : Restrictions.eq("parent", parentAkun))

				.add(satuanKerjas == null || satuanKerjas.size() == 0 ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.isNull("satuanKerja"),
								Restrictions.in("satuanKerja", satuanKerjas)))

				.addOrder(Order.asc("kode"));

		if (dimulai != null && !dimulai.trim().isEmpty()) {
			Criterion criterion = Restrictions.sqlRestriction("false");
			for (String s : dimulai.split(";")) {
				criterion = Restrictions.or(criterion, Restrictions.ilike("kode", s, MatchMode.START));
			}
			criteria.add(criterion);
		}

		List<Akun> akuns = ConstantValues.simpleList(criteria, Akun.class);
		return akuns;
	}

	public void generateAllChildren(Akun parentAkun, Set<Akun> akuns) {
		if (!isLeaf(parentAkun)) {
			List<Akun> kerjas = getChildren(parentAkun);
			for (Akun akun : kerjas) {
				akuns.add(akun);
				generateAllChildren(akun, akuns);
			}
		}
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

		if (ambilDataSatuanKerjaBanbox != null && ambilDataSatuanKerjaBanbox.getAttribute("satuanKerja") != null) {
			satuanKerja = (SatuanKerja) ambilDataSatuanKerjaBanbox.getAttribute("satuanKerja");
		}

		Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
		if (satuanKerja != null) {
			satuanKerjas.add(satuanKerja);
			satuanKerjaTreeModel.getChildsSet(satuanKerja, satuanKerjas);
		}

		Akun parentAkun = (Akun) parent;
		Session session = HibernateUtil.currentSession();

		Criteria criteria = session.createCriteria(Akun.class)
				.add(grupAkun != null ? Restrictions.eq("grupAkun", grupAkun) : Restrictions.sqlRestriction("true"))
				.add(debetCredit == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("debetCredit", debetCredit))
				.add(parentAkun == null ? Restrictions.isNull("parent") : Restrictions.eq("parent", parentAkun))

				.add(satuanKerjas == null || satuanKerjas.size() == 0 ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.isNull("satuanKerja"),
								Restrictions.or(
										parent == null ? Restrictions.isNull("satuanKerja")
												: Restrictions.sqlRestriction("false"),
										Restrictions.in("satuanKerja", satuanKerjas))))

				.setProjection(Projections.rowCount());

		if (dimulai != null && !dimulai.trim().isEmpty()) {
			Criterion criterion = Restrictions.sqlRestriction("false");
			for (String s : dimulai.split(";")) {
				criterion = Restrictions.or(criterion, Restrictions.ilike("kode", s, MatchMode.START));
			}
			criteria.add(criterion);
		}

		Integer count = ((Number) criteria.uniqueResult()).intValue();

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