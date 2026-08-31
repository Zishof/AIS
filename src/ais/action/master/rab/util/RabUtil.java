package ais.action.master.rab.util;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.ProjectionList;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;

import ais.action.master.akunting.helper.AmbilDataPegawaiBanbox;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Pegawai;
import ais.database.model.Tbmuser;
import ais.database.model.akunting.AcaraHasTransaksi;
import ais.database.model.akunting.Transaksi;
import ais.database.model.rab.Acara;
import ais.database.model.rab.JenisInformasiRab;
import ais.database.model.rab.Proyek;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.rab.SumberDana;
import ais.database.model.rab.Tugas;
import ais.database.model.rab.TugasPunyaPegawai;
import ais.database.model.rab.Workspace;
import ais.database.model.rab.WorkspacePunyaIndikator;
import ais.database.model.rab.WorkspacePunyaJenisParameter;
import ais.database.model.rab.WorkspacePunyaPegawai;
import ais.database.model.rab.WorkspacePunyaSasaran;
import ais.ui.util.MyMessageboxConfig;
import org.hibernate.EntityMode;

/**
 * Kumpulan utilitas statis lintas layar untuk modul RAB (Rencana Anggaran dan Belanja/perencanaan
 * anggaran & kegiatan): manajemen status revisi {@link Workspace} (unit RAB), penyalinan data
 * antar-revisi/antar-tahun-anggaran (deep copy pohon {@link Workspace} dan {@link Tugas}/
 * {@link Proyek} beserta seluruh relasi anaknya), pembentukan ringkasan detail relasi generik, dan
 * beberapa helper kecil (pemilihan pegawai default, pengaitan {@link Transaksi} ke {@link Acara}).
 *
 * <h2>Konstanta dan singleton referensi</h2>
 * <p>
 * {@link #DEFAULT_REVISI} ({@code -1}, menandai revisi "draft" sebelum dinomori ulang menjadi 1
 * oleh {@link #ubahSemuaStatus}) dan {@link #DEFAULT_SATUAN_KERJA} adalah nilai baku dipakai
 * lintas layar RAB. Blok statis kelas ini memuat/membuat tiga baris referensi
 * {@link JenisInformasiRab} tetap ({@link #INFORMASI}, {@link #PENGUMUMAN}, {@link #PERINGATAN}) —
 * pola cari-atau-buat (find-or-create) dijalankan sekali saat kelas dimuat.
 * </p>
 *
 * <h2>Manajemen status revisi</h2>
 * <p>
 * {@link #ubahSemuaStatus} menormalkan flag {@code aktif} pada baris {@link Workspace} suatu
 * kombinasi tahun+satuan kerja+sumber dana: hanya revisi dengan nomor tertinggi (atau baris
 * {@code carryOver=true}) yang tetap aktif, revisi lain dinonaktifkan, dan baris berrevisi
 * {@link #DEFAULT_REVISI} (draft) diaktifkan lalu dinomori ulang menjadi revisi 1. Dijalankan
 * lewat kueri SQL native dalam transaksi eksplisit.
 * </p>
 *
 * <h2>Penyalinan revisi/tahun</h2>
 * <p>
 * Dua jalur paralel — satu untuk pohon {@link Workspace} ({@link #createNewRevisi(Integer, Integer,
 * Integer, Integer, SatuanKerja, SumberDana, SatuanKerja, SumberDana, EventListener)} +
 * {@link #executeCopy(Integer, Integer, Integer, Integer, SatuanKerja, SumberDana, SatuanKerja,
 * SumberDana)} + {@link #checkForChildsCopy(Workspace, WorkspaceTreeModel, Integer, Integer,
 * SatuanKerja, SumberDana, SatuanKerja, SumberDana)}), satu lagi untuk pohon {@link Proyek}/
 * {@link Tugas} ({@link #createNewRevisi(Integer, Integer, Proyek, EventListener)} +
 * {@link #executeCopy(Integer, Integer, Proyek)} + {@link #checkForChildsCopy(Tugas, TugasTreeModel,
 * Integer, Proyek)}) — mengimplementasikan alur "buat revisi baru dari revisi lama": bila target
 * revisi sudah memiliki data, pengguna diminta konfirmasi (akan ditimpa) sebelum penyalinan
 * dilakukan; penyalinan sendiri men-{@code clone()} setiap baris berikut seluruh relasi anak
 * langsungnya (pegawai, sasaran, indikator, jenis parameter untuk workspace) secara rekursif
 * menuruni pohon, menandai baris hasil salinan lewat {@code merupakanHasilCopy}/{@code copyForm}
 * agar dapat dilacak balik ke sumbernya.
 * </p>
 */
public class RabUtil {

	public static final Integer DEFAULT_REVISI = -1;
	public static final Integer DEFAULT_SATUAN_KERJA = 99999;

	public static JenisInformasiRab INFORMASI;
	public static JenisInformasiRab PENGUMUMAN;
	public static JenisInformasiRab PERINGATAN;

	static {
		Session session = HibernateUtil.getSessionFactory().openSession();

		try {
			PENGUMUMAN = (JenisInformasiRab) session.createCriteria(JenisInformasiRab.class)
					.add(Restrictions.eq("nama", "Pengumuman")).setMaxResults(1).uniqueResult();
			if (PENGUMUMAN == null) {
				PENGUMUMAN = new JenisInformasiRab();
				PENGUMUMAN.setKeterangan("Pengumuman");
				PENGUMUMAN.setNama("Pengumuman");
				session.getTransaction().begin();
				session.save(PENGUMUMAN);
				session.getTransaction().commit();
			}

			INFORMASI = (JenisInformasiRab) session.createCriteria(JenisInformasiRab.class)
					.add(Restrictions.eq("nama", "Informasi")).setMaxResults(1).uniqueResult();
			if (INFORMASI == null) {
				INFORMASI = new JenisInformasiRab();
				INFORMASI.setKeterangan("Informasi");
				INFORMASI.setNama("Informasi");
				session.getTransaction().begin();
				session.save(INFORMASI);
				session.getTransaction().commit();
			}

			PERINGATAN = (JenisInformasiRab) session.createCriteria(JenisInformasiRab.class)
					.add(Restrictions.eq("nama", "Peringatan")).setMaxResults(1).uniqueResult();
			if (PERINGATAN == null) {
				PERINGATAN = new JenisInformasiRab();
				PERINGATAN.setKeterangan("Peringatan");
				PERINGATAN.setNama("Peringatan");
				session.getTransaction().begin();
				session.save(PERINGATAN);
				session.getTransaction().commit();
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/rab/util/RabUtil.java:85");
		} finally {
			try {
				session.disconnect();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/rab/util/RabUtil.java:89");
				// TODO: handle exception
			}
			try {
				session.close();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/rab/util/RabUtil.java:94");
				// TODO: handle exception
			}
		}

	}

	/**
	 * Menormalkan flag {@code aktif} pada {@link Workspace} untuk kombinasi tahun+satuan
	 * kerja+sumber dana: revisi bernomor tertinggi (atau {@code carryOver=true}) tetap/menjadi
	 * aktif, revisi lain dinonaktifkan, dan baris berrevisi {@link #DEFAULT_REVISI} diaktifkan lalu
	 * dinomori ulang menjadi revisi 1. Tidak melakukan apa pun bila {@code satuanKerja} atau
	 * {@code sumberDana} {@code null}.
	 *
	 * @param tahunWorkspace tahun anggaran target
	 * @param revisi         nomor revisi yang statusnya sedang dievaluasi
	 * @param satuanKerja    satuan kerja target
	 * @param sumberDana     sumber dana target
	 */
	public static void ubahSemuaStatus(Integer tahunWorkspace, Integer revisi, SatuanKerja satuanKerja,
			SumberDana sumberDana) {
		if (satuanKerja == null || sumberDana == null) {
			return;
		}
		Session session = HibernateUtil.currentSession();
		Integer maxrevisi = (Integer) session.createCriteria(Workspace.class)
				.add(Restrictions.or(Restrictions.eq("carryOver", true),
						Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))))
				.add(Restrictions.eq("satuanKerja", satuanKerja))
				.add(sumberDana == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("sumberDana", sumberDana))
				.add(Restrictions.eq("tahunWorkspace", tahunWorkspace)).setProjection(Projections.max("revisi"))
				.uniqueResult();
		maxrevisi = maxrevisi == null ? 1 : maxrevisi;

		Integer count = ((Number) session.createSQLQuery("select count(*) from rab.workspace where parent_id = id;")
				.uniqueResult()).intValue();
		if (!count.equals(0)) {
			session.createSQLQuery("delete from rab.workspace where parent_id = id;").executeUpdate();
		}

		count = ((Number) session.createCriteria(Workspace.class)
				.add(Restrictions.or(Restrictions.eq("carryOver", true),
						Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))))
				.add(sumberDana == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("sumberDana", sumberDana))
				.add(Restrictions.eq("tahunWorkspace", tahunWorkspace))
				.add(Restrictions.sqlRestriction(
						"revisi = " + revisi + " and (aktif is null or aktif != " + (maxrevisi.equals(revisi)) + ")"))
				.setProjection(Projections.rowCount()).uniqueResult()).intValue();

		if (!count.equals(0)) {
			try {
				Session mySession = HibernateUtil.currentNativeSession();
				mySession.getTransaction().begin();
				String sql = "update rab.workspace set aktif = " + (maxrevisi.equals(revisi)) + " where satuan_kerja = "
						+ satuanKerja.getId() + (sumberDana == null ? "" : " and sumber_dana = " + sumberDana.getId())
						+ " and tahun_workspace = " + tahunWorkspace + " and revisi = " + revisi
						+ " and (aktif is null or aktif != " + (maxrevisi.equals(revisi)) + ");";
				// System.out.println("Ubah status sql = " + sql);
				mySession.createSQLQuery(sql).executeUpdate();

				sql = "update rab.workspace set aktif = false where satuan_kerja = " + satuanKerja.getId()
						+ (sumberDana == null ? "" : " and sumber_dana = " + sumberDana.getId())
						+ " and tahun_workspace = " + tahunWorkspace + " and revisi != -1 and revisi != " + maxrevisi
						+ " and (aktif is null or aktif = true);";
				// System.out.println("Ubah status sql = " + sql);
				mySession.createSQLQuery(sql).executeUpdate();

				sql = "update rab.workspace set aktif = true where revisi = -1 and (aktif is null or aktif = false);";
				mySession.createSQLQuery(sql).executeUpdate();

				sql = "update rab.workspace set revisi = 1 where id > 0 and revisi = -1;";
				mySession.createSQLQuery(sql).executeUpdate();
				mySession.getTransaction().commit();

				HibernateUtil.closeSession();
			} catch (Exception e) {
				HibernateUtil.rollbackTransaction();
				// TODO Auto-generated catch block
				Common.tampilErrorJikaAdmin(e);
			}

		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	/**
	 * Membangun ringkasan teks generik dari baris-baris {@code clazz} yang terkait ke
	 * {@code workspace}: mengambil {@code properti} yang diminta lewat proyeksi Hibernate dan
	 * menggabungkannya menjadi string berformat {@code "[nilai1][nilai2], [nilai1][nilai2], ..."}.
	 *
	 * @param clazz     kelas entitas anak yang direlasikan ke {@code workspace}
	 * @param alias     alias join (saat ini tidak dipakai langsung pada kueri properti tunggal/ganda)
	 * @param properti  nama-nama properti yang diproyeksikan per baris
	 * @param workspace workspace induk yang menjadi filter relasi
	 * @return array dua elemen: {@code [0]} jumlah baris (Integer), {@code [1]} ringkasan teks (String)
	 */
	public static Serializable[] getDetailWorkspace(Class clazz, String alias, String[] properti, Workspace workspace) {
		Session session = HibernateUtil.currentSession();

		String isi = "";
		Integer size = 0;
		if (properti.length > 1) {

			ProjectionList projectionList = Projections.projectionList();
			for (String p : properti) {
				projectionList.add(Projections.property(p));
			}

			List<Object[]> strings = (session.createCriteria(clazz).createAlias(alias, "a1")
					.setProjection(projectionList).add(Restrictions.eq("workspace", workspace)).list());
			size = strings.size();
			for (Object[] s : strings) {
				String sub = "";
				for (Object c : s) {
					sub += "[" + (c == null ? "" : c) + "]";
				}

				isi += isi.equals("") ? sub : ", " + sub;
			}
		} else if (properti.length == 1) {

			List<String> strings = (session.createCriteria(clazz).createAlias(alias, "a1")
					.setProjection(Projections.property(properti[0])).add(Restrictions.eq("workspace", workspace))
					.list());
			size = strings.size();
			for (String s : strings) {
				s = "[" + s + "]";
				isi += isi.equals("") ? s : ", " + s;
			}
		}
		Serializable[] serializable = new Serializable[] { size, isi };
		return serializable;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	/** Seperti {@link #getDetailWorkspace(Class, String, String[], Workspace)}, difilter terhadap {@link Tugas} sebagai induk. */
	public static Serializable[] getDetailTugas(Class clazz, String alias, String[] properti, Tugas tugas) {
		Session session = HibernateUtil.currentSession();

		String isi = "";
		Integer size = 0;
		if (properti.length > 1) {

			ProjectionList projectionList = Projections.projectionList();
			for (String p : properti) {
				projectionList.add(Projections.property(p));
			}

			List<Object[]> strings = (session.createCriteria(clazz).createAlias(alias, "a1")
					.setProjection(projectionList).add(Restrictions.eq("tugas", tugas)).list());
			size = strings.size();
			for (Object[] s : strings) {
				String sub = "";
				for (Object c : s) {
					sub += "[" + (c == null ? "" : c) + "]";
				}

				isi += isi.equals("") ? sub : ", " + sub;
			}
		} else if (properti.length == 1) {

			List<String> strings = (session.createCriteria(clazz).createAlias(alias, "a1")
					.setProjection(Projections.property(properti[0])).add(Restrictions.eq("tugas", tugas)).list());
			size = strings.size();
			for (String s : strings) {
				s = "[" + s + "]";
				isi += isi.equals("") ? s : ", " + s;
			}
		}
		Serializable[] serializable = new Serializable[] { size, isi };
		return serializable;
	}

	/** Mengisi {@code bandbox} dengan pegawai milik user yang sedang login, bila ada. */
	public static void setDefaultPegawai(AmbilDataPegawaiBanbox bandbox) {
		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser != null && tbmuser.ambilPegawai() != null) {
			Pegawai pegawai = tbmuser.ambilPegawai();
			bandbox.setAttribute("pegawai", tbmuser.ambilPegawai());
			bandbox.setAttribute("myValue", tbmuser.ambilPegawai());
			bandbox.setValue(pegawai == null ? "" : (pegawai.getCode() + " - " + pegawai.getNama()));
			bandbox.setDisabled(true);
		} else if (tbmuser != null && tbmuser.ambilDosen() != null
				&& (tbmuser.hakAkses().getRoleName().toLowerCase().contains("dosen"))) {
			Pegawai pegawai = Pegawai.createDataPegawaiDariDosen(tbmuser.ambilDosen());
			bandbox.setAttribute("pegawai", pegawai);
			bandbox.setAttribute("myValue", pegawai);
			bandbox.setValue(pegawai == null ? "" : (pegawai.getCode() + " - " + pegawai.getNama()));
			bandbox.setDisabled(true);
		} else if (tbmuser != null && tbmuser.ambilGuru() != null
				&& (tbmuser.hakAkses().getRoleName().toLowerCase().contains("guru"))) {
			Pegawai pegawai = Pegawai.createDataPegawaiDariGuru(tbmuser.ambilGuru());
			bandbox.setAttribute("pegawai", pegawai);
			bandbox.setAttribute("myValue", pegawai);
			bandbox.setValue(pegawai == null ? "" : (pegawai.getCode() + " - " + pegawai.getNama()));
			bandbox.setDisabled(true);
		}
	}

	// public static void bersihkanSimpanTransaksi() {
	//
	// Session session = HibernateUtil.currentSession();
	//
	// Integer count = ((Number) session.createCriteria(Transaksi.class)
	// .add(Restrictions.eq("simpan", false))
	// .setProjection(Projections.rowCount()).uniqueResult())
	// .intValue();
	//
	// if (!count.equals(0)) {
	// session.createSQLQuery(
	// "delete from akunting.transaksi where simpan = false")
	// .executeUpdate();
	// }
	// }

//	public static GrupTransaksi checkSimpanWorkspace1(Transaksi transaksi, Workspace workspace) {
//		return checkSimpanWorkspace1(transaksi, workspace, null, null, 0);
//	}
//
//	public static GrupTransaksi checkSimpanWorkspace1(Transaksi transaksi, Workspace workspace,
//			SatuanKerja satuanKerja, SumberDana sumberDana, int coint) {
//		GrupTransaksi grupTransaksi = null;
//		try {
//			System.out.println(
//					"satuanKerja = " + satuanKerja + ", sumberDana = " + sumberDana + ",workspace = " + workspace);
//
//			Session mySession = HibernateUtil.currentSession();
//			if (satuanKerja != null && sumberDana != null && (workspace == null || workspace.getId() == null)) {
//				workspace = (Workspace) mySession.createCriteria(Workspace.class).add(Restrictions.or(Restrictions.eq("carryOver", true),Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))))
//						.add(Restrictions.eq("copyForm", transaksi.getId()))
//						.add(Restrictions.eq("satuanKerja", satuanKerja)).add(Restrictions.eq("sumberDana", sumberDana))
//						.add(Restrictions.eq("parentId", Long.MAX_VALUE))
//						.add(Restrictions.eq("tahunWorkspace", transaksi.getTahun())).setMaxResults(1).uniqueResult();
//
//				System.out.println("workspace = " + workspace);
//				if (workspace == null || !workspace.getAktif()) {
//					workspace = new Workspace();
//					workspace.setAkun(transaksi.getAkun());
//					workspace.setCopyForm(transaksi.getId());
//					workspace.setDeep(null);
//					workspace.setDurasi(0);
//					workspace.setHargaSatuan(0.0);
//					workspace.setHargaTotal(0.0);
//					workspace.setJenisWorkspace(null);
//					workspace.setJmlDipakai(0L);
//					workspace.setJmlWaktu(0.0);
//					workspace.setKeterangan("");
//					workspace.setKode("");
//					workspace.setLeaf(true);
//					workspace.setMerupakanHasilCopy(false);
//					workspace.setMulai(null);
//					workspace.setNama("");
//					workspace.setParentId(Long.MAX_VALUE);
//					workspace.setPersenKomplit(0.0);
//					workspace.setQty(0.0);
//					workspace.setRealisasiTotal(0.0);
//					workspace.setRevisi(-2);
//					workspace.setSatuan(null);
//					workspace.setSatuan1(null);
//					workspace.setSatuanKerja(satuanKerja);
//					workspace.setSatuanVolume("");
//					workspace.setSelesai(null);
//					workspace.setSumberDana(sumberDana);
//					workspace.setTahunWorkspace(transaksi.getTahun());
//					workspace.setUnitOrganisasi(null);
//					workspace.setVolume(0.0);
//					mySession.save(workspace);
//				}
//				System.out.println("workspace = " + workspace);
//
//				grupTransaksi = transaksi.getGrupTransaksi();
//
//				grupTransaksi.setWorkspace(workspace);
//				Common.refreshUpdate(mySession, grupTransaksi);
//			} else if (workspace == null || workspace.getId() == null || transaksi == null
//					|| transaksi.getId() == null) {
//				return null;
//			}
//
//			Integer count = ((Number) mySession.createCriteria(GrupTransaksi.class)
//					.add(Restrictions.eq("transaksi", transaksi)).add(Restrictions.eq("workspace", workspace))
//					.setProjection(Projections.rowCount()).setMaxResults(1).uniqueResult()).intValue();
//			if (count.equals(0)) {
//				grupTransaksi = new GrupTransaksi();
//				grupTransaksi.setWorkspace(workspace);
//				mySession.save(grupTransaksi);
//			}
//			// else {
//			// // initParents(grupTransaksi, workspace);
//			// mySession.update(mySession.merge(grupTransaksi));
//			// }
//		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/rab/util/RabUtil.java:362");
//			Common.tampilErrorJikaAdmin(e); 
//			try {
//				// Session session = HibernateUtil.currentSession();
//				// session.saveOrUpdate(transaksi);
//				Thread.sleep(1000);
//			} catch (Exception e1) {
//				e1.printStackTrace();
//			}
//			if (coint < 3) {
//				checkSimpanWorkspace1(transaksi, workspace, satuanKerja, sumberDana, ++coint);
//			}
//		}
//		return grupTransaksi;
//	}

	// public static void initParents(
	// GrupTransaksi grupTransaksi, Workspace workspace) {
	// ClassMetadata classMetadata = HibernateUtil
	// .getClassMetadata(GrupTransaksi.class);
	// List<Workspace> workspaces = new ArrayList<Workspace>();
	// getParentSet(workspace, workspaces);
	// for (int i = 0; i < workspaces.size(); i++) {
	// classMetadata.setPropertyValue(grupTransaksi, "parent" + i, // workspaces.get(i), EntityMode.POJO);
	// }
	// }

	/** Menelusuri rantai {@code parentId} milik {@code workspace} ke atas secara rekursif, menambahkan setiap workspace induk (aktif/carry-over) yang ditemukan ke {@code workspaces}. */
	public static void getParentSet(Workspace workspace, List<Workspace> workspaces) {
		Session session = HibernateUtil.currentSession();
		if (workspace.getParentId() != null) {
			Workspace parentWorkspace = (Workspace) session.createCriteria(Workspace.class)
					.add(Restrictions.or(Restrictions.eq("carryOver", true),
							Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))))
					.add(Restrictions.idEq(workspace.getParentId())).uniqueResult();
			if (parentWorkspace != null) {
				workspaces.add(parentWorkspace);
				getParentSet(parentWorkspace, workspaces);
			}
		}

	}

	/**
	 * Memastikan relasi {@link AcaraHasTransaksi} antara {@code transaksi} dan {@code acara} ada,
	 * membuatnya bila belum ada. Tidak melakukan apa pun (mengembalikan {@code null}) bila
	 * {@code acara} belum tersimpan.
	 *
	 * @return relasi yang sudah ada atau baru dibuat, atau {@code null} bila {@code acara} tidak valid
	 */
	public static AcaraHasTransaksi checkSimpanAcara(Transaksi transaksi, Acara acara) {
		if (acara == null || acara.getId() == null) {
			return null;
		}
		Session session = HibernateUtil.currentSession();
		AcaraHasTransaksi acaraHasTransaksi = (AcaraHasTransaksi) session.createCriteria(AcaraHasTransaksi.class)
				.add(Restrictions.eq("transaksi", transaksi)).add(Restrictions.eq("acara", acara)).setMaxResults(1)
				.uniqueResult();
		if (acaraHasTransaksi == null) {
			acaraHasTransaksi = new AcaraHasTransaksi();
			acaraHasTransaksi.setTransaksi(transaksi);
			acaraHasTransaksi.setAcara(acara);
			session.save(acaraHasTransaksi);
		}
		return acaraHasTransaksi;
	}

//	@SuppressWarnings("unchecked")
//	public static void pindahkanRealisasiKeRevisiBaru(Integer tahun, final Integer oldRevisi,
//			final Integer newRevisi, SatuanKerja satuanKerja, SumberDana sumberDana) {
//		Session session = HibernateUtil.currentSession();
//		List<GrupTransaksi> grupTransaksis = session.createCriteria(GrupTransaksi.class)
//				.createAlias("workspace", "workspace", Criteria.INNER_JOIN)
//				.add(Restrictions.eq("workspace.satuanKerja", satuanKerja))
//				.add(Restrictions.eq("workspace.sumberDana", sumberDana))
//				.add(Restrictions.eq("workspace.tahunWorkspace", tahun))
//				.add(Restrictions.eq("workspace.revisi", oldRevisi)).list();
//
//		for (GrupTransaksi grupTransaksi : grupTransaksis) {
//
//			Transaksi transaksi = grupTransaksi.getTransaksi();
//			Workspace newWorkspace = (Workspace) session.createCriteria(Workspace.class).add(Restrictions.or(Restrictions.eq("carryOver", true),Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))))
//					.add(Restrictions.eq("copyForm", grupTransaksi.getWorkspace().getId())).setMaxResults(1)
//					.uniqueResult();
//			if (newWorkspace == null) {
//				continue;
//			}
//
//			GrupTransaksi newGrupTransaksi = (GrupTransaksi) session.createCriteria(GrupTransaksi.class)
//					.add(Restrictions.eq("transaksi", transaksi))
//					.createAlias("workspace", "workspace", Criteria.INNER_JOIN)
//					.add(Restrictions.eq("workspace.satuanKerja", satuanKerja))
//					.add(Restrictions.eq("workspace.sumberDana", sumberDana))
//					.add(Restrictions.eq("workspace.tahunWorkspace", tahun))
//					.add(Restrictions.eq("workspace.revisi", newRevisi)).setMaxResults(1).uniqueResult();
//
//			if (newGrupTransaksi == null) {
//				newGrupTransaksi = new GrupTransaksi();
//				newGrupTransaksi.setTransaksi(transaksi);
//				newGrupTransaksi.setWorkspace(newWorkspace);
//				// initParents(newGrupTransaksi, newWorkspace);
//				session.save(newGrupTransaksi);
//			} else {
//				// initParents(newGrupTransaksi, newWorkspace);
//				Common.refreshUpdate(session, (newGrupTransaksi));
//			}
//		}
//	}

	@SuppressWarnings("unchecked")
	/**
	 * Memindahkan pengaitan {@link Acara}-{@link Transaksi} dari {@link Workspace} revisi lama ke
	 * workspace hasil salinannya pada revisi baru (workspace baru dicari lewat {@code copyForm}
	 * yang mengarah ke workspace lama), untuk kombinasi tahun+satuan kerja+sumber dana tertentu.
	 */
	public static void pindahkanAcaraKeRevisiBaru(Integer tahun, final Integer oldRevisi, final Integer newRevisi,
			SatuanKerja satuanKerja, SumberDana sumberDana) {
		Session session = HibernateUtil.currentSession();
		List<AcaraHasTransaksi> acaraHasTransaksis = session.createCriteria(AcaraHasTransaksi.class)
				.createAlias("acara", "acara", Criteria.INNER_JOIN)
				.createAlias("acara.workspace", "workspace", Criteria.INNER_JOIN)
				.add(Restrictions.eq("workspace.satuanKerja", satuanKerja))
				.add(Restrictions.eq("workspace.sumberDana", sumberDana))
				.add(Restrictions.eq("workspace.tahunWorkspace", tahun))
				.add(Restrictions.eq("workspace.revisi", newRevisi)).list();

		for (AcaraHasTransaksi acaraHasTransaksi : acaraHasTransaksis) {

			Transaksi transaksi = acaraHasTransaksi.getTransaksi();
			Acara newAcara = (Acara) session.createCriteria(Acara.class)
					.add(Restrictions.eq("copyForm", acaraHasTransaksi.getAcara().getId())).setMaxResults(1)
					.uniqueResult();

			AcaraHasTransaksi newAcaraHasTransaksi = (AcaraHasTransaksi) session.createCriteria(AcaraHasTransaksi.class)
					.add(Restrictions.eq("transaksi", transaksi)).createAlias("acara", "acara", Criteria.INNER_JOIN)
					.createAlias("acara.workspace", "workspace", Criteria.INNER_JOIN)
					.add(Restrictions.eq("workspace.satuanKerja", satuanKerja))
					.add(Restrictions.eq("workspace.sumberDana", sumberDana))
					.add(Restrictions.eq("workspace.tahunWorkspace", tahun))
					.add(Restrictions.eq("workspace.revisi", newRevisi)).setMaxResults(1).uniqueResult();

			if (newAcaraHasTransaksi == null) {
				newAcaraHasTransaksi = new AcaraHasTransaksi();
				newAcaraHasTransaksi.setTransaksi(transaksi);
				newAcaraHasTransaksi.setAcara(newAcara);
				session.save(newAcaraHasTransaksi);
			}
		}
	}

	@SuppressWarnings("unchecked")
	/**
	 * Menyalin secara rekursif seluruh anak {@link Workspace} (beserta relasi
	 * {@link WorkspacePunyaPegawai}, {@link WorkspacePunyaSasaran}, {@link WorkspacePunyaIndikator},
	 * {@link WorkspacePunyaJenisParameter}) dari workspace sumber ({@code copyForm}) ke bawah
	 * {@code workspace} hasil salinan, hanya dijalankan bila {@code workspace} ditandai
	 * {@code merupakanHasilCopy=true}; flag tersebut dimatikan di akhir setelah penyalinan anak
	 * selesai agar tidak diproses ulang.
	 */
	public static void checkForChildsCopy(Workspace workspace, WorkspaceTreeModel workspaceTreeModel,
			Integer tahunTujuan, final Integer newRevisi, SatuanKerja satuanKerja, SumberDana sumberDana,
			SatuanKerja satuanKerjaTujuan, SumberDana sumberDanaTujuan) {

		if (workspace.getMerupakanHasilCopy() != null && workspace.getMerupakanHasilCopy()) {
			Session session = HibernateUtil.currentSession();
			Workspace copyFrom = (Workspace) session.createCriteria(Workspace.class)
					.add(Restrictions.or(Restrictions.eq("carryOver", true),
							Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))))
					.add(Restrictions.idEq(workspace.getCopyForm())).uniqueResult();

			int childCount = workspaceTreeModel.getChildCount(copyFrom);

			if (childCount != 0) {
				List<Workspace> workspaces = workspaceTreeModel.getChildren(copyFrom);

				for (Workspace myWorkspace : workspaces) {

					Workspace copyWorkspace = (Workspace) myWorkspace.clone();
					copyWorkspace.setDeep(null);
					copyWorkspace.setSatuanKerja(satuanKerjaTujuan);
					copyWorkspace.setSumberDana(sumberDanaTujuan);
					copyWorkspace.setCopyForm(myWorkspace.getId());
					copyWorkspace.setMerupakanHasilCopy(true);
					copyWorkspace.setParentId(workspace.getId());
					copyWorkspace.setTahunWorkspace(tahunTujuan);
					copyWorkspace.setRevisi(newRevisi);
					copyWorkspace.setId(null);
					session.save(copyWorkspace);

					List<WorkspacePunyaPegawai> workspacePunyaPegawais = session
							.createCriteria(WorkspacePunyaPegawai.class).add(Restrictions.eq("workspace", myWorkspace))
							.list();
					for (WorkspacePunyaPegawai workspacePunyaPegawai : workspacePunyaPegawais) {
						WorkspacePunyaPegawai copyWorkspacePunyaPegawai = (WorkspacePunyaPegawai) workspacePunyaPegawai
								.clone();
						copyWorkspacePunyaPegawai.setId(null);
						copyWorkspacePunyaPegawai.setWorkspace(copyWorkspace);
						session.save(copyWorkspacePunyaPegawai);
					}

					List<WorkspacePunyaSasaran> workspacePunyaSasarans = session
							.createCriteria(WorkspacePunyaSasaran.class).add(Restrictions.eq("workspace", myWorkspace))
							.list();
					for (WorkspacePunyaSasaran workspacePunyaSasaran : workspacePunyaSasarans) {
						WorkspacePunyaSasaran copyWorkspacePunyaSasaran = (WorkspacePunyaSasaran) workspacePunyaSasaran
								.clone();
						copyWorkspacePunyaSasaran.setId(null);
						copyWorkspacePunyaSasaran.setWorkspace(copyWorkspace);
						session.save(copyWorkspacePunyaSasaran);
					}

					List<WorkspacePunyaIndikator> workspacePunyaIndikators = session
							.createCriteria(WorkspacePunyaIndikator.class)
							.add(Restrictions.eq("workspace", myWorkspace)).list();
					for (WorkspacePunyaIndikator workspacePunyaIndikator : workspacePunyaIndikators) {
						WorkspacePunyaIndikator copyWorkspacePunyaIndikator = (WorkspacePunyaIndikator) workspacePunyaIndikator
								.clone();
						copyWorkspacePunyaIndikator.setId(null);
						copyWorkspacePunyaIndikator.setWorkspace(copyWorkspace);
						session.save(copyWorkspacePunyaIndikator);
					}

					List<WorkspacePunyaJenisParameter> workspacePunyaJenisParameters = session
							.createCriteria(WorkspacePunyaJenisParameter.class)
							.add(Restrictions.eq("workspace", myWorkspace)).list();
					for (WorkspacePunyaJenisParameter workspacePunyaJenisParameter : workspacePunyaJenisParameters) {
						WorkspacePunyaJenisParameter copyWorkspacePunyaJenisParameter = (WorkspacePunyaJenisParameter) workspacePunyaJenisParameter
								.clone();
						copyWorkspacePunyaJenisParameter.setId(null);
						copyWorkspacePunyaJenisParameter.setWorkspace(copyWorkspace);
						session.save(copyWorkspacePunyaJenisParameter);
					}

					checkForChildsCopy(copyWorkspace, workspaceTreeModel, tahunTujuan, newRevisi, satuanKerja,
							sumberDana, satuanKerjaTujuan, sumberDanaTujuan);
				}
			}
			workspace.setMerupakanHasilCopy(false);
			Common.refreshUpdate(session, (workspace));
		}
	}

	/**
	 * Titik masuk alur "buat revisi RAB baru dari revisi lama" untuk pohon {@link Workspace}. Bila
	 * revisi/tahun/satuan-kerja/sumber-dana tujuan sudah memiliki data, pengguna diminta konfirmasi
	 * (data lama akan ditimpa) lewat {@link MyMessageboxConfig} sebelum {@link #executeCopy(Integer,
	 * Integer, Integer, Integer, SatuanKerja, SumberDana, SatuanKerja, SumberDana)} dijalankan;
	 * bila belum ada data, penyalinan langsung dijalankan. Setelah proses, status revisi lama
	 * dinormalkan lewat {@link #ubahSemuaStatus}, dan {@code eventListener} dipanggil sebagai
	 * callback selesai.
	 */
	public static void createNewRevisi(final Integer tahun, final Integer oldRevisi, final Integer newRevisi,
			final Integer tahunLama, final SatuanKerja satuanKerja, final SumberDana sumberDana,
			final SatuanKerja satuanKerjaTujuan, final SumberDana sumberDanaTujuan, final EventListener eventListener) {
		try {

			Session session = HibernateUtil.currentSession();
			Integer count = ((Number) session.createCriteria(Workspace.class)
					.add(Restrictions.or(Restrictions.eq("carryOver", true),
							Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))))
					.add(Restrictions.eq("satuanKerja", satuanKerjaTujuan))
					.add(sumberDanaTujuan == null ? Restrictions.sqlRestriction("true")
							: Restrictions.eq("sumberDana", sumberDanaTujuan))
					.add(Restrictions.eq("revisi", newRevisi)).setProjection(Projections.rowCount())
					.add(Restrictions.eq("tahunWorkspace", tahun)).uniqueResult()).intValue();

			if (!count.equals(0)) {
				MyMessageboxConfig.show("Perencanaan anggaran tahun " + tahun + " revisi ke " + newRevisi
						+ " sudah memiliki data, apakah anda ingin menimpa data yang sudah ada ?\n\n\nCatatan: Semua data anggaran tahun "
						+ tahun + " revisi ke " + newRevisi
						+ " akan terhapus dan diganti dengan hasil copy dari revisi tahun " + tahunLama + " revisi ke "
						+ oldRevisi, "Question", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
						MyMessageboxConfig.QUESTION, new EventListener() {

							@Override
							public void onEvent(Event event) throws Exception {
								int i = Integer.parseInt(event.getData().toString());
								if (i == MyMessageboxConfig.OK) {
									try {
										executeCopy(tahun, oldRevisi, newRevisi, tahunLama, satuanKerja, sumberDana,
												satuanKerjaTujuan, sumberDanaTujuan);
										eventListener.onEvent(null);
									} catch (Exception e) {
										Common.tampilErrorJikaAdmin(e);
										MyMessageboxConfig
												.show("Data ini tidak dapat dicopy .., error-nya adalah sbagai berikut:"
														+ e.getMessage());
									}

								}

							}
						});
			} else {
				executeCopy(tahun, oldRevisi, newRevisi, tahunLama, satuanKerja, sumberDana, satuanKerjaTujuan,
						sumberDanaTujuan);
				eventListener.onEvent(null);
			}

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

		ubahSemuaStatus(tahunLama, oldRevisi, satuanKerja, sumberDana);
	}

	@SuppressWarnings("unchecked")
	/**
	 * Menjalankan penyalinan aktual pohon {@link Workspace} akar (tanpa parent) dari
	 * tahun/satuan-kerja/sumber-dana/revisi sumber ke tahun/satuan-kerja/sumber-dana/revisi tujuan:
	 * menghapus data tujuan yang sudah ada, meng-clone setiap workspace akar sumber, lalu
	 * melanjutkan penyalinan anak-anaknya secara rekursif lewat
	 * {@link #checkForChildsCopy(Workspace, WorkspaceTreeModel, Integer, Integer, SatuanKerja,
	 * SumberDana, SatuanKerja, SumberDana)}.
	 */
	public static void executeCopy(Integer tahun, final Integer oldRevisi, final Integer newRevisi,
			Integer workspace_copy, SatuanKerja satuanKerja, SumberDana sumberDana, SatuanKerja satuanKerjaTujuan,
			SumberDana sumberDanaTujuan) {
		Session session = HibernateUtil.currentSession();

		Long parentId = WorkspaceTreeModel.checkForParent(workspace_copy, satuanKerja, oldRevisi);

		List<Workspace> willCopied = session.createCriteria(Workspace.class)
				.add(Restrictions.or(Restrictions.eq("carryOver", true),
						Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))))
				.add(Restrictions.eq("satuanKerja", satuanKerja))
				.add(sumberDana == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("sumberDana", sumberDana))
				.add(Restrictions.eq("revisi", oldRevisi)).addOrder(Order.asc("kode")).addOrder(Order.asc("nama"))
				.add(Restrictions.eq("tahunWorkspace", workspace_copy)).add(Restrictions.eq("parentId", parentId))
				.list();

		// System.out.println("willCopied = " + willCopied.size()
		// + " workspace_copy = " + workspace_copy);

		session.createSQLQuery("delete from rab.workspace where tahun_workspace = " + tahun + " and revisi = "
				+ newRevisi + " and satuan_kerja = " + satuanKerjaTujuan.getId() + " and sumber_dana = "
				+ sumberDanaTujuan.getId()).executeUpdate();

		WorkspaceTreeModel willCopiedWorkspaceTreeModel = new WorkspaceTreeModel(workspace_copy, oldRevisi, satuanKerja,
				sumberDana);
		for (Workspace workspace : willCopied) {

			Long childParentId = WorkspaceTreeModel.checkForParent(tahun, satuanKerjaTujuan, newRevisi);

			Workspace newWorkspace = (Workspace) workspace.clone();
			newWorkspace.setDeep(null);
			newWorkspace.setSatuanKerja(satuanKerjaTujuan);
			newWorkspace.setSumberDana(sumberDanaTujuan);
			newWorkspace.setCopyForm(workspace.getId());
			newWorkspace.setMerupakanHasilCopy(true);
			newWorkspace.setRevisi(newRevisi);
			newWorkspace.setParentId(childParentId);

			newWorkspace.setTahunWorkspace(tahun);
			newWorkspace.setId(null);
			session.save(newWorkspace);

			List<WorkspacePunyaPegawai> workspacePunyaPegawais = session.createCriteria(WorkspacePunyaPegawai.class)
					.add(Restrictions.eq("workspace", workspace)).list();
			for (WorkspacePunyaPegawai workspacePunyaPegawai : workspacePunyaPegawais) {
				WorkspacePunyaPegawai copyWorkspacePunyaPegawai = (WorkspacePunyaPegawai) workspacePunyaPegawai.clone();
				copyWorkspacePunyaPegawai.setId(null);
				copyWorkspacePunyaPegawai.setWorkspace(newWorkspace);
				session.save(copyWorkspacePunyaPegawai);
			}

			List<WorkspacePunyaSasaran> workspacePunyaSasarans = session.createCriteria(WorkspacePunyaSasaran.class)
					.add(Restrictions.eq("workspace", workspace)).list();
			for (WorkspacePunyaSasaran workspacePunyaSasaran : workspacePunyaSasarans) {
				WorkspacePunyaSasaran copyWorkspacePunyaSasaran = (WorkspacePunyaSasaran) workspacePunyaSasaran.clone();
				copyWorkspacePunyaSasaran.setId(null);
				copyWorkspacePunyaSasaran.setWorkspace(newWorkspace);
				session.save(copyWorkspacePunyaSasaran);
			}

			List<WorkspacePunyaIndikator> workspacePunyaIndikators = session
					.createCriteria(WorkspacePunyaIndikator.class).add(Restrictions.eq("workspace", workspace)).list();
			for (WorkspacePunyaIndikator workspacePunyaIndikator : workspacePunyaIndikators) {
				WorkspacePunyaIndikator copyWorkspacePunyaIndikator = (WorkspacePunyaIndikator) workspacePunyaIndikator
						.clone();
				copyWorkspacePunyaIndikator.setId(null);
				copyWorkspacePunyaIndikator.setWorkspace(newWorkspace);
				session.save(copyWorkspacePunyaIndikator);
			}

			List<WorkspacePunyaJenisParameter> workspacePunyaJenisParameters = session
					.createCriteria(WorkspacePunyaJenisParameter.class).add(Restrictions.eq("workspace", workspace))
					.list();
			for (WorkspacePunyaJenisParameter workspacePunyaJenisParameter : workspacePunyaJenisParameters) {
				WorkspacePunyaJenisParameter copyWorkspacePunyaJenisParameter = (WorkspacePunyaJenisParameter) workspacePunyaJenisParameter
						.clone();
				copyWorkspacePunyaJenisParameter.setId(null);
				copyWorkspacePunyaJenisParameter.setWorkspace(newWorkspace);
				session.save(copyWorkspacePunyaJenisParameter);
			}

			RabUtil.checkForChildsCopy(newWorkspace, willCopiedWorkspaceTreeModel, tahun, newRevisi, satuanKerja,
					sumberDana, satuanKerjaTujuan, sumberDanaTujuan);
		}

	}

	@SuppressWarnings("unchecked")
	/** Analog {@link Tugas} dari {@link #checkForChildsCopy(Workspace, WorkspaceTreeModel, Integer, Integer, SatuanKerja, SumberDana, SatuanKerja, SumberDana)}: menyalin rekursif seluruh anak tugas dari tugas sumber ke tugas hasil salinan. */
	public static void checkForChildsCopy(Tugas tugas, TugasTreeModel tugasTreeModel, final Integer newRevisi,
			Proyek proyek, Proyek proyekTujuan) {

		if (tugas.getMerupakanHasilCopy() != null && tugas.getMerupakanHasilCopy()) {
			Session session = HibernateUtil.currentSession();
			Tugas copyFrom = (Tugas) session.createCriteria(Tugas.class).add(Restrictions.idEq(tugas.getCopyForm()))
					.uniqueResult();

			int childCount = tugasTreeModel.getChildCount(copyFrom);

			if (childCount != 0) {
				List<Tugas> tugass = tugasTreeModel.getChildren(copyFrom);

				for (Tugas myTugas : tugass) {

					Session mySession = HibernateUtil.currentNativeSession();

					Set<Tugas> tugs = ((Tugas) mySession.createCriteria(Tugas.class)
							.add(Restrictions.idEq(myTugas.getId())).uniqueResult()).getTugases();
					Set<Tugas> newTugases = new HashSet<Tugas>();
					for (Tugas t : tugs) {
						newTugases.add(new Tugas(t.getId()));
					}

					HibernateUtil.closeSession();

					Tugas copyTugas = (Tugas) myTugas.clone();
					copyTugas.setTugases(newTugases);
					copyTugas.setProyek(proyekTujuan);
					copyTugas.setCopyForm(myTugas.getId());
					copyTugas.setMerupakanHasilCopy(true);
					copyTugas.setParent(tugas);
					copyTugas.setRevisi(newRevisi);
					copyTugas.setId(null);
					session.save(copyTugas);

					List<TugasPunyaPegawai> tugasPunyaPegawais = session.createCriteria(TugasPunyaPegawai.class)
							.add(Restrictions.eq("tugas", myTugas)).list();
					for (TugasPunyaPegawai tugasPunyaPegawai : tugasPunyaPegawais) {
						TugasPunyaPegawai copyTugasPunyaPegawai = (TugasPunyaPegawai) tugasPunyaPegawai.clone();
						copyTugasPunyaPegawai.setId(null);
						copyTugasPunyaPegawai.setTugas(copyTugas);
						session.save(copyTugasPunyaPegawai);
					}

					checkForChildsCopy(copyTugas, tugasTreeModel, newRevisi, proyek, proyekTujuan);
				}
			}
			tugas.setMerupakanHasilCopy(false);
			Common.refreshUpdate(session, (tugas));
		}
	}

	/**
	 * Analog {@link Proyek}/{@link Tugas} dari {@link #createNewRevisi(Integer, Integer, Integer,
	 * Integer, SatuanKerja, SumberDana, SatuanKerja, SumberDana, EventListener)}: bila revisi
	 * tujuan pada {@code proyekTujuan} sudah memiliki data tugas, pengguna diminta konfirmasi
	 * sebelum {@link #executeCopy(Integer, Integer, Proyek, Proyek)} dijalankan.
	 */
	public static void createNewRevisi(final Integer oldRevisi, final Integer newRevisi, final Proyek proyek,
			final Proyek proyekTujuan, final EventListener eventListener) {
		try {

			Session session = HibernateUtil.currentSession();
			Integer count = ((Number) session.createCriteria(Tugas.class).add(Restrictions.eq("proyek", proyekTujuan))
					.add(Restrictions.eq("revisi", newRevisi)).setProjection(Projections.rowCount()).uniqueResult())
					.intValue();

			if (!count.equals(0)) {
				MyMessageboxConfig.show("Proyek tahun " + proyek.getNama() + " revisi ke " + newRevisi
						+ " sudah memiliki data, apakah anda ingin menimpa data yang sudah ada ?\n\n\nCatatan: Semua data proyek "
						+ proyek.getNama() + " revisi ke " + newRevisi + " akan terhapus dan diganti", "Question",
						MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
						new EventListener() {

							@Override
							public void onEvent(Event event) throws Exception {
								int i = Integer.parseInt(event.getData().toString());
								if (i == MyMessageboxConfig.OK) {
									try {
										executeCopy(oldRevisi, newRevisi, proyek, proyekTujuan);
										eventListener.onEvent(null);
									} catch (Exception e) {
										Common.tampilErrorJikaAdmin(e);
										MyMessageboxConfig
												.show("Data ini tidak dapat dicopy .., error-nya adalah sbagai berikut:"
														+ e.getMessage());
									}

								}

							}
						});
			} else {
				executeCopy(oldRevisi, newRevisi, proyek, proyekTujuan);
				eventListener.onEvent(null);
			}

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	@SuppressWarnings("unchecked")
	/**
	 * Menjalankan penyalinan aktual pohon {@link Tugas} akar (tanpa parent) milik {@code proyek}
	 * pada {@code oldRevisi} ke {@code proyekTujuan} pada {@code newRevisi}: menghapus data tujuan
	 * yang sudah ada, meng-clone setiap tugas akar sumber, lalu melanjutkan penyalinan anak-anaknya
	 * secara rekursif lewat {@link #checkForChildsCopy(Tugas, TugasTreeModel, Integer, Proyek)}.
	 */
	public static void executeCopy(final Integer oldRevisi, final Integer newRevisi, Proyek proyek,
			Proyek proyekTujuan) {
		Session session = HibernateUtil.currentSession();
		List<Tugas> willCopied = session.createCriteria(Tugas.class).add(Restrictions.eq("proyek", proyek))
				.add(Restrictions.eq("revisi", oldRevisi)).addOrder(Order.asc("nama"))
				.add(Restrictions.isNull("parent")).list();

		session.createSQLQuery(
				"delete from rab.tugas where revisi = " + newRevisi + " and proyek = " + proyekTujuan.getId())
				.executeUpdate();

		TugasTreeModel willCopiedTugasTreeModel = new TugasTreeModel(oldRevisi, proyek);
		for (Tugas tugas : willCopied) {

			Session mySession = HibernateUtil.currentNativeSession();

			Set<Tugas> tugs = ((Tugas) mySession.createCriteria(Tugas.class).add(Restrictions.idEq(tugas.getId()))
					.uniqueResult()).getTugases();

			HibernateUtil.closeSession();

			Set<Tugas> newTugases = new HashSet<Tugas>();
			for (Tugas t : tugs) {
				newTugases.add(new Tugas(t.getId()));
			}

			Tugas newTugas = (Tugas) tugas.clone();
			newTugas.setTugases(newTugases);
			newTugas.setProyek(proyekTujuan);
			newTugas.setCopyForm(tugas.getId());
			newTugas.setMerupakanHasilCopy(true);
			newTugas.setRevisi(newRevisi);
			newTugas.setParent(null);
			newTugas.setId(null);
			session.save(newTugas);

			List<TugasPunyaPegawai> tugasPunyaPegawais = session.createCriteria(TugasPunyaPegawai.class)
					.add(Restrictions.eq("tugas", tugas)).list();
			for (TugasPunyaPegawai tugasPunyaPegawai : tugasPunyaPegawais) {
				TugasPunyaPegawai copyTugasPunyaPegawai = (TugasPunyaPegawai) tugasPunyaPegawai.clone();
				copyTugasPunyaPegawai.setId(null);
				copyTugasPunyaPegawai.setTugas(newTugas);
				session.save(copyTugasPunyaPegawai);
			}

			RabUtil.checkForChildsCopy(newTugas, willCopiedTugasTreeModel, newRevisi, proyek, proyekTujuan);
		}

	}
	// @SuppressWarnings("unchecked")
	// public synchronized static void executeCopyPegawaiTugas(
	// final Integer newRevisi, Proyek proyek, Tugas parent) {
	// Session session = HibernateUtil.currentNativeSession();
	// List<Tugas> willCopied = session
	// .createCriteria(Tugas.class)
	// .add(Restrictions.eq("proyek", proyek))
	// .add(Restrictions.eq("revisi", newRevisi))
	// .addOrder(Order.asc("nama"))
	// .add(parent == null ? Restrictions.isNull("parent")
	// : Restrictions.eq("parent", parent)).list();
	//
	// HibernateUtil.closeSession();
	//
	// for (Tugas tugas : willCopied) {
	// if (tugas.getCopyForm() == null) {
	// continue;
	// }
	//
	// Session mySession = HibernateUtil.currentNativeSession();
	// Set<Pegawai> pegawais = ((Tugas) mySession
	// .createCriteria(Tugas.class)
	// .add(Restrictions.idEq(tugas.getCopyForm())).uniqueResult())
	// .getPegawais();
	//
	// Set<Pegawai> newPegawais = new HashSet<Pegawai>();
	// for (Pegawai pegawai : pegawais) {
	// newPegawais.add(new Pegawai(pegawai.getId()));
	// }
	//
	// my
	// HibernateUtil.closeSession();
	//
	// mySession = HibernateUtil.currentNativeSession();
	// tugas.setPegawais(newPegawais);
	// mySession.getTransaction().begin();
	// mySession.update(tugas);
	// mySession.getTransaction().commit();
	// my
	// HibernateUtil.closeSession();
	//
	// RabUtil.executeCopyPegawaiTugas(newRevisi, proyek, tugas);
	// }
	// }

}
