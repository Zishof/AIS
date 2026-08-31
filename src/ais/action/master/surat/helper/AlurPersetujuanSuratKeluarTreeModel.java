package ais.action.master.surat.helper;


import ais.common.CommonSearchFilterHelper;
import java.util.List;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zul.AbstractTreeModel;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Combobox;

import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.surat.AlurPersetujuanSuratKeluar;

/**
 * Tipe khusus untuk alur persetujuan surat keluar tree model. Kelas ini memberi nama dan batas
 * tanggung jawab yang eksplisit pada perilaku yang diwarisi atau kontrak yang
 * diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * AbstractTreeModel}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Boolean tampilkanSemua}, {@code
 * Combobox searchfakultas}, {@code Combobox searchjurusan}, {@code Combobox searchyayasan}, {@code Combobox
 * searchsekolah}, {@code AmbilDataSatuanKerjaBanbox satuanKerja}, {@code SatuanKerjaTreeModel
 * satuanKerjaTreeModel}, {@code Checkbox searchaktif}; pembacaan/pencarian ({@code getChildren()}, {@code
 * getChildren()}, {@code getChild()}, {@code getChildCount()}, {@code getParentCount()}, {@code
 * getParentSet()}); penghapusan/pembatalan ({@code deleteChilds()}); operasi domain lain ({@code
 * generateAllChildren()}, {@code isLeaf()}); konfigurasi constructor: {@code satuanKerjaTreeModel}. Bagian lain
 * dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see AbstractTreeModel
 */
public class AlurPersetujuanSuratKeluarTreeModel extends AbstractTreeModel {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5115651721345571411L;
	private Boolean tampilkanSemua;
	private Combobox searchfakultas;
	private Combobox searchjurusan;
	private Combobox searchyayasan;
	private Combobox searchsekolah;
	private AmbilDataSatuanKerjaBanbox satuanKerja;
	private SatuanKerjaTreeModel satuanKerjaTreeModel;
	private Checkbox searchaktif;
	private String tipe;

	/**
	 * Constructor
	 * 
	 * @param tree the list is contained all data of nodes.
	 */
	public AlurPersetujuanSuratKeluarTreeModel(Boolean tampilkanSemua, Combobox searchfakultas, Combobox searchjurusan,
			Combobox searchyayasan, Combobox searchsekolah, AmbilDataSatuanKerjaBanbox satuanKerja,
			Checkbox searchaktif, String tipe) {
		super(null);
		this.tipe = tipe;
		this.tampilkanSemua = tampilkanSemua;
		this.searchfakultas = searchfakultas;
		this.searchjurusan = searchjurusan;
		this.searchyayasan = searchyayasan;
		this.searchsekolah = searchsekolah;
		this.satuanKerja = satuanKerja;
		this.searchaktif = searchaktif;
		this.tampilkanSemua = tampilkanSemua;
		satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);
	}

	/**
	 * Constructor
	 * 
	 * @param tree the list is contained all data of nodes.
	 */
	public AlurPersetujuanSuratKeluarTreeModel(AlurPersetujuanSuratKeluar parentAlurPersetujuanSuratKeluar,
			Boolean tampilkanSemua, Combobox searchfakultas, Combobox searchjurusan, Combobox searchyayasan,
			Combobox searchsekolah, AmbilDataSatuanKerjaBanbox satuanKerja, Checkbox searchaktif, String tipe) {
		super(parentAlurPersetujuanSuratKeluar);
		this.tipe = tipe;
		this.tampilkanSemua = tampilkanSemua;
		this.searchfakultas = searchfakultas;
		this.searchjurusan = searchjurusan;
		this.searchyayasan = searchyayasan;
		this.searchsekolah = searchsekolah;
		this.satuanKerja = satuanKerja;
		this.searchaktif = searchaktif;
		this.tampilkanSemua = tampilkanSemua;
		satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);
	}

	public List<AlurPersetujuanSuratKeluar> getChildren(AlurPersetujuanSuratKeluar parentAlurPersetujuanSuratKeluar) {
		return getChildren(parentAlurPersetujuanSuratKeluar, null);
	}

	@SuppressWarnings("unchecked")
	public List<AlurPersetujuanSuratKeluar> getChildren(AlurPersetujuanSuratKeluar parentAlurPersetujuanSuratKeluar,
			Integer index) {
		Session session = HibernateUtil.currentSession();

		SatuanKerja parent = (SatuanKerja) satuanKerja.getAttribute("satuanKerja");
		Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
		if (parent != null) {
			satuanKerjas.clear();
			satuanKerjas.add(parent);
			satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);
		}

		List<AlurPersetujuanSuratKeluar> alurPersetujuanSuratKeluars = ConstantValues
				.simpleList(session.createCriteria(AlurPersetujuanSuratKeluar.class)

						.add(Restrictions.or(Restrictions.isNull("tipe"), Restrictions.eq("tipe", tipe)))

						.add(parentAlurPersetujuanSuratKeluar != null || searchaktif != null && searchaktif.isChecked()
								? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
								: Restrictions.sqlRestriction("true"))

						.add(parentAlurPersetujuanSuratKeluar != null || satuanKerjas.size() == 0
								? Restrictions.sqlRestriction("1=1")
								: Restrictions.or(
										parent == null ? Restrictions.isNull("satuanKerja")
												: Restrictions.sqlRestriction("false"),
										Restrictions.in("satuanKerja", satuanKerjas)))

						.add(parentAlurPersetujuanSuratKeluar != null || searchjurusan.getSelectedItem() == null
								|| searchjurusan.getSelectedItem().getValue() == null
										? Restrictions.sqlRestriction("1=1")
										: Restrictions.or(Restrictions.isNull("jurusan"),
												CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false)))

						.add(parentAlurPersetujuanSuratKeluar != null || searchfakultas.getSelectedItem() == null
								|| searchfakultas.getSelectedItem().getValue() == null
										? Restrictions.sqlRestriction("1=1")
										: Restrictions.or(Restrictions.isNull("fakultas"),
												CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false)))

						.add(parentAlurPersetujuanSuratKeluar != null || searchsekolah.getSelectedItem() == null
								|| searchsekolah.getSelectedItem().getValue() == null
										? Restrictions.sqlRestriction("1=1")
										: Restrictions.or(Restrictions.isNull("sekolah"),
												CommonSearchFilterHelper.eqSelectedWithId("sekolah", searchsekolah, false)))

						.add(parentAlurPersetujuanSuratKeluar != null || searchyayasan.getSelectedItem() == null
								|| searchyayasan.getSelectedItem().getValue() == null
										? Restrictions.sqlRestriction("1=1")
										: Restrictions.or(Restrictions.isNull("yayasan"),
												CommonSearchFilterHelper.eqSelectedWithId("yayasan", searchyayasan, false)))

						.setMaxResults(index == null ? 10000 : 1).setFirstResult(index == null ? 0 : index)
						.add(tampilkanSemua ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("defaultItem", true))

						.add(parentAlurPersetujuanSuratKeluar == null ? Restrictions.isNull("parent")
								: Restrictions.eq("parent", parentAlurPersetujuanSuratKeluar))
						.addOrder(Order.asc("nama")).addOrder(Order.desc("id")), AlurPersetujuanSuratKeluar.class);
		return alurPersetujuanSuratKeluars;
	}

	public void generateAllChildren(AlurPersetujuanSuratKeluar parentAlurPersetujuanSuratKeluar,
			Set<AlurPersetujuanSuratKeluar> alurPersetujuanSuratKeluars) {
		if (!isLeaf(parentAlurPersetujuanSuratKeluar)) {
			List<AlurPersetujuanSuratKeluar> kerjas = getChildren(parentAlurPersetujuanSuratKeluar);
			for (AlurPersetujuanSuratKeluar alurPersetujuanSuratKeluar : kerjas) {
				alurPersetujuanSuratKeluars.add(alurPersetujuanSuratKeluar);
				generateAllChildren(alurPersetujuanSuratKeluar, alurPersetujuanSuratKeluars);
			}
		}
	}

	// TreeModel //
	public Object getChild(Object parent, int index) {
		AlurPersetujuanSuratKeluar parentAlurPersetujuanSuratKeluar = (AlurPersetujuanSuratKeluar) parent;
		List<AlurPersetujuanSuratKeluar> alurPersetujuanSuratKeluars = getChildren(parentAlurPersetujuanSuratKeluar,
				index);
		AlurPersetujuanSuratKeluar alurPersetujuanSuratKeluar = alurPersetujuanSuratKeluars.size() > 0
				? alurPersetujuanSuratKeluars.get(0)
				: null;
		return alurPersetujuanSuratKeluar;
	}

	public int getChildCount(Object parent) {
		AlurPersetujuanSuratKeluar parentAlurPersetujuanSuratKeluar = (AlurPersetujuanSuratKeluar) parent;
		Session session = HibernateUtil.currentNativeSession();
		Integer count = ((Number) session.createCriteria(AlurPersetujuanSuratKeluar.class)
				.add(tampilkanSemua ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("defaultItem", true))

				.add(Restrictions.or(Restrictions.isNull("tipe"), Restrictions.eq("tipe", tipe)))

				.add(parentAlurPersetujuanSuratKeluar == null ? Restrictions.isNull("parent")
						: Restrictions.eq("parent", parentAlurPersetujuanSuratKeluar))
				.setProjection(Projections.rowCount()).uniqueResult()).intValue();

		HibernateUtil.closeSession();
		return count;
	}

	public void deleteChilds(Object parent) {
		AlurPersetujuanSuratKeluar parentAlurPersetujuanSuratKeluar = (AlurPersetujuanSuratKeluar) parent;
		Session session = HibernateUtil.currentSession();
		List<AlurPersetujuanSuratKeluar> alurPersetujuanSuratKeluars = getChildren(parentAlurPersetujuanSuratKeluar);
		for (AlurPersetujuanSuratKeluar alurPersetujuanSuratKeluar : alurPersetujuanSuratKeluars) {
			if (getChildCount(alurPersetujuanSuratKeluar) == 0) {
				session.delete(alurPersetujuanSuratKeluar);
			} else {
				deleteChilds(alurPersetujuanSuratKeluar);
			}
		}
	}

	public boolean isLeaf(Object node) {
		return (getChildCount(node) == 0);
	}

	public void getParentCount(AlurPersetujuanSuratKeluar alurPersetujuanSuratKeluar, AlurPersetujuanSuratKeluar obj,
			List<Long> longs) {
		Session session = HibernateUtil.currentSession();
		if (alurPersetujuanSuratKeluar.getParent() == null) {
			obj.setDeep(longs.size());
			Common.refreshUpdate(session, (obj));
		} else {
			AlurPersetujuanSuratKeluar parentAlurPersetujuanSuratKeluar = (AlurPersetujuanSuratKeluar) ConstantValues
					.simpleObject(
							session.createCriteria(AlurPersetujuanSuratKeluar.class)
									.add(Restrictions.or(Restrictions.isNull("tipe"), Restrictions.eq("tipe", tipe)))
									.add(tampilkanSemua ? Restrictions.sqlRestriction("1=1")
											: Restrictions.eq("defaultItem", true))
									.add(Restrictions.idEq(alurPersetujuanSuratKeluar.getParent().getId())),
							AlurPersetujuanSuratKeluar.class);
			longs.add(parentAlurPersetujuanSuratKeluar.getId());
			getParentCount(parentAlurPersetujuanSuratKeluar, obj, longs);
		}

	}

	public void getParentSet(AlurPersetujuanSuratKeluar alurPersetujuanSuratKeluar,
			List<AlurPersetujuanSuratKeluar> alurPersetujuanSuratKeluars) {
		Session session = HibernateUtil.currentSession();
		if (alurPersetujuanSuratKeluar.getParent() != null) {
			AlurPersetujuanSuratKeluar parentAlurPersetujuanSuratKeluar = (AlurPersetujuanSuratKeluar) ConstantValues
					.simpleObject(
							session.createCriteria(AlurPersetujuanSuratKeluar.class)
									.add(Restrictions.or(Restrictions.isNull("tipe"), Restrictions.eq("tipe", tipe)))
									.add(tampilkanSemua ? Restrictions.sqlRestriction("1=1")
											: Restrictions.eq("defaultItem", true))
									.add(Restrictions.idEq(alurPersetujuanSuratKeluar.getParent().getId())),
							AlurPersetujuanSuratKeluar.class);

			if (parentAlurPersetujuanSuratKeluar != null) {
				alurPersetujuanSuratKeluars.add(parentAlurPersetujuanSuratKeluar);
				getParentSet(parentAlurPersetujuanSuratKeluar, alurPersetujuanSuratKeluars);
			}
		}

	}

	public void getChildsSet(AlurPersetujuanSuratKeluar alurPersetujuanSuratKeluar,
			Set<AlurPersetujuanSuratKeluar> alurPersetujuanSuratKeluars) {
		List<AlurPersetujuanSuratKeluar> childs = getChildren(alurPersetujuanSuratKeluar);
		for (AlurPersetujuanSuratKeluar myAlurPersetujuanSuratKeluar : childs) {
			alurPersetujuanSuratKeluars.add(myAlurPersetujuanSuratKeluar);
			if (!isLeaf(myAlurPersetujuanSuratKeluar)) {
				getChildsSet(myAlurPersetujuanSuratKeluar, alurPersetujuanSuratKeluars);
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