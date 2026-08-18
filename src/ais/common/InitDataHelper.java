package ais.common;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.beanutils.BeanUtilsBean;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;
import org.reflections.Reflections;

import ais.action.master.helper.DefaultJenisParsingReconsile;
import ais.action.master.sekolah.psb.form.PPDB;
import ais.action.servlet.Api;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.AlatTransportasiMahasiswa;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.BiodataMahasiswa;
import ais.database.model.BlacklistIp;
import ais.database.model.DetailKelompokKegiatanKedosenan;
import ais.database.model.DetailKelompokKegiatanKemahasiswaan;
import ais.database.model.GeneralValueObject;
import ais.database.model.GolonganPns;
import ais.database.model.IkatanKerjaDosen;
import ais.database.model.ItemBiaya;
import ais.database.model.Jabatan;
import ais.database.model.JabatanFungsionalDosen;
import ais.database.model.JabatanKegiatanKedosenan;
import ais.database.model.JabatanKegiatanKemahasiswaan;
import ais.database.model.JenisAktfitasMahasiswa;
import ais.database.model.JenisEvaluasi;
import ais.database.model.JenisKelompokKegiatanKedosenan;
import ais.database.model.JenisKelompokKegiatanKemahasiswaan;
import ais.database.model.JenisPembayaran;
import ais.database.model.JenisPembiayaanMahasiswa;
import ais.database.model.JenisPendidikDanTenagaKependidikan;
import ais.database.model.JenisRekonsiliasiHostToHost;
import ais.database.model.JenisSekolahMahasiswaBaru;
import ais.database.model.JenisTinggalMahasiswa;
import ais.database.model.Jenjang;
import ais.database.model.JurusanSekolahMahasiswaBaru;
import ais.database.model.KelompokKegiatanKedosenan;
import ais.database.model.KelompokKegiatanKemahasiswaan;
import ais.database.model.Konfigurasi;
import ais.database.model.LabelBahasa;
import ais.database.model.LembagaPengangkat;
import ais.database.model.Mahasiswa;
import ais.database.model.Menu;
import ais.database.model.Negara;
import ais.database.model.OperatorSeluler;
import ais.database.model.Paket;
import ais.database.model.Pegawai;
import ais.database.model.Pekerjaan;
import ais.database.model.PembombotanNilai;
import ais.database.model.PendidikanOrangTua;
import ais.database.model.Penghasilan;
import ais.database.model.Perkuliahan;
import ais.database.model.Program;
import ais.database.model.SkalaKegiatanKedosenan;
import ais.database.model.SkalaKegiatanKemahasiswaan;
import ais.database.model.Staff;
import ais.database.model.StatusAwalMahasiswa;
import ais.database.model.StatusKeluar;
import ais.database.model.StatusKepegawaian;
import ais.database.model.StatusKewajibanBebanDosen;
import ais.database.model.StatusMahasiswa;
import ais.database.model.StatusPegawai;
import ais.database.model.Statusabsensi;
import ais.database.model.StatuskehadiranKaryawanHarian;
import ais.database.model.SumberGaji;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.database.model.akunting.Akun;
import ais.database.model.akunting.DaftarPengajuanTransfer;
import ais.database.model.asset.JenisPemesananPengadaanAsset;
import ais.database.model.asset.PermintaanPengadaanMasterAssetDetail;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.employ.KenaikanPangkat;
import ais.database.model.employ.TipeMasaKerja;
import ais.database.model.employ.TipePegawai;
import ais.database.model.inventory.JenisProduk;
import ais.database.model.koperasi.CaraPembayaranKoperasi;
import ais.database.model.koperasi.JenisAnggotaKoperasi;
import ais.database.model.koperasi.JenisIdentitasAnggotaKoperasi;
import ais.database.model.koperasi.JenisTransaksiKoperasi;
import ais.database.model.koperasi.TipeAnggotaKoperasi;
import ais.database.model.koperasi.TipeProdukKoperasi;
import ais.database.model.library.Item;
import ais.database.model.penelitiandanpengabdian.TipePenelitianDanPengabdian;
import ais.database.model.sekolah.AlatTransportasiSiswa;
import ais.database.model.sekolah.CalonSiswa;
import ais.database.model.sekolah.DetailKelompokKegiatanKesiswaan;
import ais.database.model.sekolah.JabatanKegiatanKesiswaan;
import ais.database.model.sekolah.JenisCatatanSiswa;
import ais.database.model.sekolah.JenisKelompokKegiatanKesiswaan;
import ais.database.model.sekolah.JenisTinggalSiswa;
import ais.database.model.sekolah.KelompokKegiatanKesiswaan;
import ais.database.model.sekolah.KelompokParameterTambahanCatatanSiswa;
import ais.database.model.sekolah.NominalBiaya;
import ais.database.model.sekolah.PekerjaanOrtuSiswa;
import ais.database.model.sekolah.PendidikanOrangTuaSiswa;
import ais.database.model.sekolah.Siswa;
import ais.database.model.sekolah.SkalaKegiatanKesiswaan;
import ais.database.model.sekolah.StatusAwalSiswa;
import ais.database.model.sekolah.StatusKeluarSiswa;
import ais.database.model.sop.AlurSop;
import ais.database.model.sop.DokumenAlurSop;
import ais.database.model.sop.KelompokParameterTambahanAlurSop;
import ais.database.model.spmi.JenisSPMI;
import ais.database.model.surat.SifatSurat;
import ais.ui.util.FormSop;

public class InitDataHelper {

	private static final Object LOCK_CLASS_INIT = new Object();

	private static void rollbackActiveTransaction(Session session) {
		try {
			if (session != null && session.getTransaction() != null && session.getTransaction().isActive()) {
				session.getTransaction().rollback();
			}
		} catch (Exception e) {
			try {
				Common.tampilErrorJikaAdmin(e);
			} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/InitDataHelper.java:142");
			}
		}
	}

	private static void beginTransactionIfNeeded(Session session) {
		if (session != null && session.getTransaction() != null && !session.getTransaction().isActive()) {
			session.getTransaction().begin();
		}
	}

	private static void commitTransactionIfActive(Session session) {
		try {
			if (session != null && session.getTransaction() != null && session.getTransaction().isActive()) {
				session.getTransaction().commit();
			}
		} catch (Exception e) {
			rollbackActiveTransaction(session);
			throw new RuntimeException(e);
		}
	}

	private static void closeOpenedSession(Session session) {
		try {
			if (session != null && session.isOpen()) {
				try {
					session.clear();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/InitDataHelper.java:169");
				}
				try {
					session.disconnect();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/InitDataHelper.java:173");
				}
				try {
					session.close();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/InitDataHelper.java:177");
				}
			}
		} catch (Exception e) {
			try {
				Common.tampilErrorJikaAdmin(e);
			} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/InitDataHelper.java:183");
			}
		}
	}

	@SuppressWarnings({ "rawtypes" })
	private static int executeHqlUpdateIsolated(String hql, Map parameters) {
		Session isolatedSession = null;
		Transaction transaction = null;
		try {
			isolatedSession = HibernateUtil.getSessionFactory().openSession();
			transaction = isolatedSession.beginTransaction();
			org.hibernate.Query query = isolatedSession.createQuery(hql);
			if (parameters != null) {
				java.util.Iterator iterator = parameters.keySet().iterator();
				while (iterator.hasNext()) {
					Object key = iterator.next();
					if (key != null) {
						query.setParameter(key.toString(), parameters.get(key));
					}
				}
			}
			int affected = query.executeUpdate();
			transaction.commit();
			return affected;
		} catch (Exception e) {
			try {
				if (transaction != null && transaction.isActive()) {
					transaction.rollback();
				}
			} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/InitDataHelper.java:213");
			}
			try {
				Common.tampilErrorJikaAdmin(e);
			} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/InitDataHelper.java:217");
			}
			return 0;
		} finally {
			closeOpenedSession(isolatedSession);
		}
	}

	private static String[] splitInitData(String value) {
		if (value == null) {
			return new String[0];
		}
		String[] result = value.split(";", -1);
		for (int i = 0; i < result.length; i++) {
			result[i] = result[i] == null ? "" : result[i].trim();
		}
		return result;
	}

	private static String[] splitConfigSemicolon(String value) {
		if (value == null || value.trim().length() == 0) {
			return new String[0];
		}
		String[] values = value.split(";", -1);
		List<String> result = new java.util.ArrayList<String>();
		for (int i = 0; i < values.length; i++) {
			String item = values[i] == null ? "" : values[i].trim();
			if (item.length() > 0) {
				result.add(item);
			}
		}
		return result.toArray(new String[result.size()]);
	}

	private static void addLogAbsensiChunk(TreeSet<String> treeSet, String logAbsensi) {
		if (treeSet == null || logAbsensi == null || logAbsensi.trim().length() == 0) {
			return;
		}
		String value = logAbsensi.trim();
		if (value.indexOf(";") >= 0 || value.indexOf("|") >= 0 || value.indexOf(",") >= 0 || value.indexOf("\n") >= 0
				|| value.indexOf("\r") >= 0) {
			String[] data = value.split("[;|,\\r\\n]+", -1);
			for (int i = 0; i < data.length; i++) {
				String item = data[i] == null ? "" : data[i].trim();
				if (item.length() > 0) {
					treeSet.add(item);
				}
			}
			return;
		}
		if (value.length() % 12 == 0) {
			for (int i = 0; i < value.length(); i += 12) {
				treeSet.add(value.substring(i, i + 12));
			}
		} else {
			treeSet.add(value);
		}
	}

	private static String[] splitKeteranganAbsensi(String value) {
		if (value == null || value.trim().length() == 0) {
			return new String[0];
		}
		return value.split("[|\\r\\n]+", -1);
	}

	// Set untuk mencegah double loading class yang sama
//	public static Set<String> udahData = new HashSet<String>();

	@SuppressWarnings("rawtypes")
	public static void initData(final Class clazz) {
		if (clazz == null) {
			return;
		}

		if (InitData.executor == null || InitData.executor.isShutdown() || InitData.executor.isTerminated()) {
			doInitData(clazz);
			return;
		}

		InitData.executor.submit(new Runnable() {
			@Override
			public void run() {
				doInitData(clazz);
			}
		});
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void initMaster() {
		// 1. Buka Session Baru (Isolated Session)
		// Menggunakan openSession() agar terpisah dari session HTTP request
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {

			ConstantValues.tbmroleMahasiswa = (Tbmrole) session.createCriteria(Tbmrole.class)
					.add(Restrictions.eq("roleId", Tbmrole.MAHASISWA)).setMaxResults(1).uniqueResult();

			ConstantValues.tbmroleSiswa = (Tbmrole) session.createCriteria(Tbmrole.class)
					.add(Restrictions.eq("roleId", Tbmrole.SISWA)).setMaxResults(1).uniqueResult();
			if (ConstantValues.tbmroleSiswa == null) {
				ConstantValues.tbmroleSiswa = new Tbmrole();
				ConstantValues.tbmroleSiswa.setRoleId(Tbmrole.SISWA);
				ConstantValues.tbmroleSiswa.setNama("Siswa");
				session.getTransaction().begin();
				session.save(ConstantValues.tbmroleSiswa);
				session.getTransaction().commit();
			}

			ConstantValues.tbmrolePenyedia = (Tbmrole) session.createCriteria(Tbmrole.class)
					.add(Restrictions.eq("roleId", Tbmrole.PENYEDIA)).setMaxResults(1).uniqueResult();
			if (ConstantValues.tbmrolePenyedia == null) {
				ConstantValues.tbmrolePenyedia = new Tbmrole();
				ConstantValues.tbmrolePenyedia.setRoleId(Tbmrole.PENYEDIA);
				ConstantValues.tbmrolePenyedia.setNama("Penyedia / Perusahaan");
				session.getTransaction().begin();
				session.save(ConstantValues.tbmrolePenyedia);
				session.getTransaction().commit();
			}

			ConstantValues.tbmroleUmum = (Tbmrole) session.createCriteria(Tbmrole.class)
					.add(Restrictions.eq("roleId", "Umum")).setMaxResults(1).uniqueResult();
			if (ConstantValues.tbmroleUmum == null) {
				ConstantValues.tbmroleUmum = new Tbmrole();
				ConstantValues.tbmroleUmum.setRoleId("Umum");
				ConstantValues.tbmroleUmum.setNama("Umum");
				session.getTransaction().begin();
				session.save(ConstantValues.tbmroleUmum);
				session.getTransaction().commit();
			}

			ConstantValues.tbmroleCalonPegawai = (Tbmrole) session.createCriteria(Tbmrole.class)
					.add(Restrictions.eq("roleId", Tbmrole.CALON_PEGAWAI)).setMaxResults(1).uniqueResult();
			if (ConstantValues.tbmroleCalonPegawai == null) {
				ConstantValues.tbmroleCalonPegawai = new Tbmrole();
				ConstantValues.tbmroleCalonPegawai.setRoleId(Tbmrole.CALON_PEGAWAI);
				ConstantValues.tbmroleCalonPegawai.setNama("Calon Pegawai / Karyawan");
				session.getTransaction().begin();
				session.save(ConstantValues.tbmroleCalonPegawai);
				session.getTransaction().commit();
			}

			String[] operator = new String[] { "Telkomsel", "XL", "Axis", "Indosat", "Smartfren", "Tri", "IM3" };
			for (String s : operator) {
				OperatorSeluler operatorSeluler = (OperatorSeluler) session.createCriteria(OperatorSeluler.class)
						.add(Restrictions.ilike("nama", s)).setMaxResults(1).uniqueResult();
				if (operatorSeluler == null) {
					operatorSeluler = new OperatorSeluler();
					operatorSeluler.setKode(s);
					operatorSeluler.setNama(s);
					session.getTransaction().begin();
					session.save(operatorSeluler);
					session.getTransaction().commit();
				}
			}

			System.out.println("tbmroleMahasiswa -> " + ConstantValues.tbmroleMahasiswa);

			ConstantValues.AKTIF = (StatusMahasiswa) session.createCriteria(StatusMahasiswa.class).setMaxResults(1)
					.add(Restrictions.eq("kodeEpsbed", "A")).addOrder(Order.asc("id")).uniqueResult();

			ConstantValues.CUTI = (StatusMahasiswa) session.createCriteria(StatusMahasiswa.class).setMaxResults(1)
					.add(Restrictions.eq("kodeEpsbed", "C")).addOrder(Order.asc("id")).uniqueResult();

			ConstantValues.LULUS = (StatusMahasiswa) session.createCriteria(StatusMahasiswa.class).setMaxResults(1)
					.add(Restrictions.eq("kodeEpsbed", "L")).addOrder(Order.asc("id")).uniqueResult();

			ConstantValues.KELUAR = (StatusMahasiswa) session.createCriteria(StatusMahasiswa.class)
					.add(Restrictions.eq("kodeEpsbed", "K")).addOrder(Order.asc("id")).setMaxResults(1).uniqueResult();

			ConstantValues.DROP_OUT = (StatusMahasiswa) session.createCriteria(StatusMahasiswa.class)
					.add(Restrictions.eq("kodeEpsbed", "D")).addOrder(Order.asc("id")).setMaxResults(1).uniqueResult();

			if (ConstantValues.DROP_OUT == null) {
				ConstantValues.DROP_OUT = new StatusMahasiswa();
				ConstantValues.DROP_OUT.setNama("DROP-OUT/PUTUS STUDI");
				ConstantValues.DROP_OUT.setKodeEpsbed("D");
				session.getTransaction().begin();
				session.save(ConstantValues.DROP_OUT);
				session.getTransaction().commit();
			}

			ConstantValues.TIDAK_AKTIF = (StatusMahasiswa) session.createCriteria(StatusMahasiswa.class)
					.add(Restrictions.eq("kodeEpsbed", "N")).addOrder(Order.asc("id")).setMaxResults(1).uniqueResult();

			ConstantValues.KAMPUS_MERDEKA = (StatusMahasiswa) session.createCriteria(StatusMahasiswa.class)
					.add(Restrictions.eq("kodeEpsbed", "M")).addOrder(Order.asc("id")).setMaxResults(1).uniqueResult();

			if (ConstantValues.KAMPUS_MERDEKA == null) {
				ConstantValues.KAMPUS_MERDEKA = new StatusMahasiswa();
				ConstantValues.KAMPUS_MERDEKA.setNama("Kampus Merdeka");
				ConstantValues.KAMPUS_MERDEKA.setKodeEpsbed("M");
				session.getTransaction().begin();
				session.save(ConstantValues.KAMPUS_MERDEKA);
				session.getTransaction().commit();
			}

			ConstantValues.MENUNGGU_UJI_KOPETENSI = (StatusMahasiswa) session.createCriteria(StatusMahasiswa.class)
					.add(Restrictions.eq("kodeEpsbed", "U")).addOrder(Order.asc("id")).setMaxResults(1).uniqueResult();

			if (ConstantValues.MENUNGGU_UJI_KOPETENSI == null) {
				ConstantValues.MENUNGGU_UJI_KOPETENSI = new StatusMahasiswa();
				ConstantValues.MENUNGGU_UJI_KOPETENSI.setNama("Menunggu Uji Kompetensi");
				ConstantValues.MENUNGGU_UJI_KOPETENSI.setKodeEpsbed("U");
				session.getTransaction().begin();
				session.save(ConstantValues.MENUNGGU_UJI_KOPETENSI);
				session.getTransaction().commit();
			}

			ConstantValues.initKehadiran(session);

			BlacklistIp blacklistIp = (BlacklistIp) session.createCriteria(BlacklistIp.class)
					.add(Restrictions.eq("kode", "23.106.*")).setMaxResults(1).uniqueResult();

			if (blacklistIp == null) {
				blacklistIp = new BlacklistIp();
				blacklistIp.setKode("23.106.*");
				blacklistIp.setNama("Blacklist 23.106.*");
				blacklistIp.setKeterangan("Blacklist 23.106.*");
				session.getTransaction().begin();
				session.save(blacklistIp);
				session.getTransaction().commit();
			}

			ConstantValues.REGULER = (Program) session.createCriteria(Program.class)
					.add(Restrictions.eq("nama", "Reguler")).setMaxResults(1).uniqueResult();

			if (ConstantValues.REGULER == null) {
				ConstantValues.REGULER = new Program();
				ConstantValues.REGULER.setNama("Reguler");
				ConstantValues.REGULER.setKeterangan("Reguler");
				session.getTransaction().begin();
				session.save(ConstantValues.REGULER);
				session.getTransaction().commit();
			}

			ConstantValues.NON_REGULER = (Program) session.createCriteria(Program.class)
					.add(Restrictions.eq("nama", "Non Reguler")).setMaxResults(1).uniqueResult();

			if (ConstantValues.NON_REGULER == null) {
				ConstantValues.NON_REGULER = new Program();
				ConstantValues.NON_REGULER.setNama("Non Reguler");
				ConstantValues.NON_REGULER.setKeterangan("Non Reguler");
				session.getTransaction().begin();
				session.save(ConstantValues.NON_REGULER);
				session.getTransaction().commit();
			}

			ConstantValues.aktifkanIntegrasiGoogle = Common.bolehKonfigurasi("aktifkan_integrasi_google");

			ConstantValues.aktifkan_akun_demo = Common.bolehKonfigurasi("aktifkan_akun_demo", Konfigurasi.TIDAK_AKTIF);

			ConstantValues.pegawai_non_aktif_otomatis_tidak_bisa_login = Common.bolehKonfigurasi("pegawai_non_aktif_otomatis_tidak_bisa_login");

//			ConstantValues.fingerprint_hanya_gunakan_finger = Common
//					.getKonfigurasi("fingerprint_hanya_gunakan_finger", Konfigurasi.TIDAK_AKTIF).getNilai()
//					.equals(Konfigurasi.AKTIF);

			ConstantValues.aktifkanIntegrasiTwitter = Common.bolehKonfigurasi("aktifkan_integrasi_twitter");

			ConstantValues.aktifkanIntegrasiLinkedin = Common.bolehKonfigurasi("aktifkan_integrasi_linkedin");

			ConstantValues.aktifkanIntegrasiFacebook = Common.bolehKonfigurasi("aktifkan_integrasi_facebook");

			ConstantValues.aktifkanFingerPrintOtomatisDariKeterangan = Common
					.getKonfigurasi("aktifkan_finger_print_otomatis_dari_keterangan", Konfigurasi.TIDAK_AKTIF)
					.getNilai().trim().equals(Konfigurasi.AKTIF);

			ConstantValues.DefaultRekonsiliasi = ((JenisRekonsiliasiHostToHost) session
					.createCriteria(JenisRekonsiliasiHostToHost.class)
					.add(Restrictions.eq("namaKelas", DefaultJenisParsingReconsile.class.getName())).setMaxResults(1)
					.uniqueResult());
			if (ConstantValues.DefaultRekonsiliasi == null) {
				ConstantValues.DefaultRekonsiliasi = new JenisRekonsiliasiHostToHost();
				ConstantValues.DefaultRekonsiliasi.setNama("Default Rekonsiliasi");
				ConstantValues.DefaultRekonsiliasi.setNamaKelas(DefaultJenisParsingReconsile.class.getName());
				session.getTransaction().begin();
				session.save(ConstantValues.DefaultRekonsiliasi);
				session.getTransaction().commit();
			}

			try {
				ConstantValues.pembayaranSemesterGanjilMulaiDiBulan = Integer.parseInt(
						Common.getKonfigurasi("pembayaran_semester_ganjil_mulai_di_bulan", "9").getNilai().trim());
				ConstantValues.pembayaranSemesterGenapMulaiDiBulan = Integer.parseInt(
						Common.getKonfigurasi("pembayaran_semester_genap_mulai_di_bulan", "3").getNilai().trim());
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/InitDataHelper.java:503");

			}

			ConstantValues.aktifkanTahapanKurikulum = Common.bolehKonfigurasi("aktifkan_tahapan_kurikulum_dalam_satu_tahun_akademik", Konfigurasi.TIDAK_AKTIF);

			ConstantValues.ABSEN_DOSEN_TERINTEGRASI_DENGAN_FINGER_PRINT = Common.bolehKonfigurasi("ABSEN_DOSEN_TERINTEGRASI_DENGAN_FINGER_PRINT");

			ConstantValues.ABSEN_GURU_TERINTEGRASI_DENGAN_FINGER_PRINT = Common.bolehKonfigurasi("ABSEN_GURU_TERINTEGRASI_DENGAN_FINGER_PRINT");

			ConstantValues.filter_tidak_boleh_ada = Common
					.getKonfigurasi("filter_tidak_boleh_ada", ConstantValues.filter_tidak_boleh_ada).getNilai();

			ConstantValues.ABSEN_MAHASISWA_TERINTEGRASI_DENGAN_FINGER_PRINT = Common.bolehKonfigurasi("ABSEN_MAHASISWA_TERINTEGRASI_DENGAN_FINGER_PRINT");

			ConstantValues.ABSEN_SISWA_TERINTEGRASI_DENGAN_FINGER_PRINT = Common.bolehKonfigurasi("ABSEN_SISWA_TERINTEGRASI_DENGAN_FINGER_PRINT");

			ConstantValues.aktifkanTahapan = Common.bolehKonfigurasi("aktifkan_tahapan_perkuliahan_dalam_satu_tahun_akademik", Konfigurasi.TIDAK_AKTIF);

			ConstantValues.aktifkanTahapanTerhubungKeKeuangan = Common.bolehKonfigurasi("aktifkan_tahapan_perkuliahan_terhubung_kebagian_keuangan", Konfigurasi.TIDAK_AKTIF);

			try {
				ConstantValues.JUMLAH_DIGIT_DIBELAKANG_KOMA = Integer
						.parseInt(Common.getKonfigurasi("JUMLAH_DIGIT_DIBELAKANG_KOMA",
								ConstantValues.JUMLAH_DIGIT_DIBELAKANG_KOMA.toString()).getNilai());
				Common.numberFormat.get().setMaximumFractionDigits(ConstantValues.JUMLAH_DIGIT_DIBELAKANG_KOMA);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/InitDataHelper.java:529");
				// TODO: handle exception
			}

			ConstantValues.aktifkanLoginHanyaViaMediaSocial = Common.bolehKonfigurasi("login_harus_via_media_sosial", Konfigurasi.TIDAK_AKTIF);

			ConstantValues.aktifkanRememeberMe = Common.bolehKonfigurasi("aktifkanRememeberMe");

			ConstantValues.aktifkanRememeberMeOtomatisTerpilih = Common.bolehKonfigurasi("aktifkanRememeberMeOtomatisTerpilih", Konfigurasi.TIDAK_AKTIF);

			ConstantValues.kehadiranHarusMulaiDanSampai = Common.bolehKonfigurasi("kehadiran_harus_mulai_dan_sampai", Konfigurasi.TIDAK_AKTIF);

			ConstantValues.aktifkanRecapcha = Common.bolehKonfigurasi("login_harus_menggunakan_capcha", Konfigurasi.TIDAK_AKTIF);

			ConstantValues.googleBookAktif = Common.bolehKonfigurasi("google_book_aktif");

			ConstantValues.aktifkanHariLibur = Common.bolehKonfigurasi("aktifkan_tidak_dihitung_hari_libur");

			ConstantValues.aktifkanHariLiburMingguSaja = Common.bolehKonfigurasi("aktifkan_tidak_dihitung_hari_minggu");

			ConstantValues.aktifkanHariLiburSabtuDanMingguSaja = Common.bolehKonfigurasi("aktifkan_tidak_dihitung_hari_sabtu_minggu");

			ConstantValues.aktifkanApakahJumlahLoginDibatasi = Common.bolehKonfigurasi("apakah_jumlah_login_dibatasi", Konfigurasi.TIDAK_AKTIF);
			ConstantValues.sop_alur_terakhir_otomatis_jadi_persetujuan = Common.bolehKonfigurasi("sop_alur_terakhir_otomatis_jadi_persetujuan", Konfigurasi.TIDAK_AKTIF);

			ConstantValues.satuperangkat = Common.bolehKonfigurasi("satuperangkat", Konfigurasi.TIDAK_AKTIF);

			ConstantValues.passwordKuat = Common.bolehKonfigurasi("password_kuat", Konfigurasi.TIDAK_AKTIF);

			ConstantValues.satuperangkat_mahasiswa = Common.bolehKonfigurasi("satuperangkat_mahasiswa", Konfigurasi.TIDAK_AKTIF);

			ConstantValues.satuperangkatipygbeda = Common.bolehKonfigurasi("satuperangkatipygbeda", Konfigurasi.TIDAK_AKTIF);

			BacaTulisUtil.flagDataMenggunakandatabase = Common.bolehKonfigurasi("flag_data_menggunakan_database", Konfigurasi.TIDAK_AKTIF);

			// Cache ENTITY (ambilData/masukkanData GeneralValueObject): DEFAULT ON pakai MapDB. Konsistensi
			// objek per-(kelas,id) dijamin identity map baik ON maupun OFF. OFF (set konfig
			// "cache_entity_menggunakan_mapdb" = TIDAK AKTIF) menghindari ketidakstabilan MapDB tetapi
			// membuat loop ambilData per-item (mis. timeline e-Learning ribuan Pertemuan) jadi ribuan query
			// DB → halaman menggantung; pakai OFF hanya bila paham konsekuensinya.
			ais.common.DataUtil.setPakaiMapDbEntity(
					Common.bolehKonfigurasi("cache_entity_menggunakan_mapdb", Konfigurasi.AKTIF));

			if (MemoryCacheUtil.lokasi_file_temporary_data != null
					&& !MemoryCacheUtil.lokasi_file_temporary_data.trim().isEmpty()) {
				ConstantValues.lokasiFileTemproraryTemp = MemoryCacheUtil.lokasi_file_temporary_data;
			}

			else {
				ConstantValues.lokasiFileTemproraryTemp = Common
						.getKonfigurasi("lokasi_file_temporary_data", ConstantValues.lokasiFileTemproraryTemp)
						.getNilai();
			}

			System.out.println("ConstantValues.lokasiFileTemproraryTemp -> " + ConstantValues.lokasiFileTemproraryTemp);

			ConstantValues.reloadTemp();
//			ConstantValues.panjangLokasiFileTemprorary = ConstantValues.lokasiFileTemprorary.length();

			ConstantValues.aktifkanApakahJumlahLoginDosenDibatasi = Common.bolehKonfigurasi("apakah_jumlah_login_dosen_dibatasi", Konfigurasi.TIDAK_AKTIF);

			ConstantValues.ketikaUbahDataPenggunaKirimke = Common.bolehKonfigurasi("ketikaUbahDataPenggunaKirimke", Konfigurasi.TIDAK_AKTIF);

			ConstantValues.ketikaUbahDataPenggunaKirimkeLink = Common.getKonfigurasi(
					"ketikaUbahDataPenggunaKirimkeLink", ConstantValues.ketikaUbahDataPenggunaKirimkeLink).getNilai();

			ConstantValues.ketikaUbahSemuaDataKirimke = Common.bolehKonfigurasi("ketikaUbahSemuaDataKirimke", Konfigurasi.TIDAK_AKTIF);

			ConstantValues.ketikaUbahSemuaDataKirimkeLink = Common
					.getKonfigurasi("ketikaUbahSemuaDataKirimkeLink", ConstantValues.ketikaUbahSemuaDataKirimkeLink)
					.getNilai();

			ConstantValues.aktifkanCaptchaLokal = Common.bolehKonfigurasi("login_harus_menggunakan_capcha_lokal", Konfigurasi.TIDAK_AKTIF);

			ConstantValues.aktifkanCaptchaLokalNoice = Common
					.getKonfigurasi("jenis_noice_capcha_lokal", ConstantValues.aktifkanCaptchaLokalNoice).getNilai();

			ConstantValues.aktifkanCaptchaLokalLebar = Common
					.getKonfigurasi("lebar_capcha_lokal", ConstantValues.aktifkanCaptchaLokalLebar).getNilai();

			ConstantValues.aktifkanCaptchaLokalTinggi = Common
					.getKonfigurasi("tinggi_capcha_lokal", ConstantValues.aktifkanCaptchaLokalTinggi).getNilai();

			ConstantValues.aktifkanCaptchaLokalRender = Common
					.getKonfigurasi("jenis_render_capcha_lokal", ConstantValues.aktifkanCaptchaLokalRender).getNilai();

			ConstantValues.aktifkanCaptchaLokalBackground = Common
					.getKonfigurasi("jenis_background_capcha_lokal", ConstantValues.aktifkanCaptchaLokalBackground)
					.getNilai();

			ConstantValues.aktifkanCaptchaLokalText = Common
					.getKonfigurasi("jenis_text_capcha_lokal", ConstantValues.aktifkanCaptchaLokalText).getNilai();

			try {
				ConstantValues.nilaiJumlahLoginDibatasi = Integer.parseInt(Common
						.getKonfigurasi("nilai_jumlah_login_dibatasi", ConstantValues.nilaiJumlahLoginDibatasi + "")
						.getNilai());
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}

			try {
				ConstantValues.nilaiJumlahLoginDosenDibatasi = Integer
						.parseInt(Common.getKonfigurasi("nilai_jumlah_dosen_dibatasi",
								ConstantValues.nilaiJumlahLoginDosenDibatasi + "").getNilai());
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}

			ConstantValues.grupPenggunaBlok = Common
					.getKonfigurasi("grup_pengguna_blok", ConstantValues.grupPenggunaBlok).getNilai();

			ConstantValues.DSPACE_UI = Common.getKonfigurasi("dspace_ui", ConstantValues.DSPACE_UI).getNilai();
			ConstantValues.DSPACE_URL_PUBLIK = Common.getKonfigurasi("dspace_url", ConstantValues.DSPACE_URL_PUBLIK)
					.getNilai();
			ConstantValues.DSPACE_URL_PRIVATE = Common
					.getKonfigurasi("dspace_private_url", ConstantValues.DSPACE_URL_PRIVATE).getNilai();

			ConstantValues.recapchaKey = Common
					.getKonfigurasi("key_server_untuk_google_capcha", ConstantValues.recapchaKey).getNilai();
			ConstantValues.recapchaClientKey = Common
					.getKonfigurasi("key_client_untuk_google_capcha", ConstantValues.recapchaClientKey).getNilai();
			ConstantValues.recapchaHome = Common.getKonfigurasi("recapcha_home", ConstantValues.recapchaHome)
					.getNilai();

			ConstantValues.EvaluasiAkademik = (JenisEvaluasi) session.createCriteria(JenisEvaluasi.class)
					.add(Restrictions.eq("nama", "Evaluasi Akademik")).setMaxResults(1).uniqueResult();

			if (ConstantValues.EvaluasiAkademik == null) {
				ConstantValues.EvaluasiAkademik = new JenisEvaluasi();
				ConstantValues.EvaluasiAkademik.setNama("Evaluasi Akademik");
				ConstantValues.EvaluasiAkademik.setKeterangan("Evaluasi Akademik");
				ConstantValues.EvaluasiAkademik.setFeeder(1L);
				session.getTransaction().begin();
				session.save(ConstantValues.EvaluasiAkademik);
				session.getTransaction().commit();

			}

			ConstantValues.AktivitasPartisipatif = (JenisEvaluasi) session.createCriteria(JenisEvaluasi.class)
					.add(Restrictions.eq("nama", "Aktivitas Partisipatif")).setMaxResults(1).uniqueResult();

			if (ConstantValues.AktivitasPartisipatif == null) {
				ConstantValues.AktivitasPartisipatif = new JenisEvaluasi();
				ConstantValues.AktivitasPartisipatif.setNama("Aktivitas Partisipatif");
				ConstantValues.AktivitasPartisipatif.setKeterangan("Aktivitas Partisipatif");
				ConstantValues.AktivitasPartisipatif.setFeeder(2L);
				session.getTransaction().begin();
				session.save(ConstantValues.AktivitasPartisipatif);
				session.getTransaction().commit();

			}

			ConstantValues.HasilProyek = (JenisEvaluasi) session.createCriteria(JenisEvaluasi.class)
					.add(Restrictions.eq("nama", "Hasil Proyek")).setMaxResults(1).uniqueResult();

			if (ConstantValues.HasilProyek == null) {
				ConstantValues.HasilProyek = new JenisEvaluasi();
				ConstantValues.HasilProyek.setNama("Hasil Proyek");
				ConstantValues.HasilProyek.setKeterangan("Hasil Proyek");
				ConstantValues.HasilProyek.setFeeder(3L);
				session.getTransaction().begin();
				session.save(ConstantValues.HasilProyek);
				session.getTransaction().commit();

			}

			ConstantValues.KognitifPengetahuan = (JenisEvaluasi) session.createCriteria(JenisEvaluasi.class)
					.add(Restrictions.eq("nama", "Kognitif/ Pengetahuan")).setMaxResults(1).uniqueResult();

			if (ConstantValues.KognitifPengetahuan == null) {
				ConstantValues.KognitifPengetahuan = new JenisEvaluasi();
				ConstantValues.KognitifPengetahuan.setNama("Kognitif/ Pengetahuan");
				ConstantValues.KognitifPengetahuan.setKeterangan("Kognitif/ Pengetahuan");
				ConstantValues.KognitifPengetahuan.setFeeder(4L);
				session.getTransaction().begin();
				session.save(ConstantValues.KognitifPengetahuan);
				session.getTransaction().commit();

			}

			ConstantValues.Tugas = (JenisEvaluasi) session.createCriteria(JenisEvaluasi.class)
					.add(Restrictions.eq("nama", "Tugas")).setMaxResults(1).uniqueResult();

			if (ConstantValues.Tugas == null) {
				ConstantValues.Tugas = new JenisEvaluasi();
				ConstantValues.Tugas.setNama("Tugas");
				ConstantValues.Tugas.setAktif(false);
				ConstantValues.Tugas.setKeterangan("Tugas");
				ConstantValues.Tugas.setFeeder(4L);
				session.getTransaction().begin();
				session.save(ConstantValues.Tugas);
				session.getTransaction().commit();

			}

			ConstantValues.KAS = (Akun) session.createCriteria(Akun.class)
					.add(Restrictions.eq("nama", "KAS DAN SETARA KAS")).setMaxResults(1).uniqueResult();

			if (ConstantValues.KAS == null) {
				ConstantValues.KAS = (Akun) session.createCriteria(Akun.class).add(Restrictions.eq("nama", "Kas Besar"))
						.setMaxResults(1).uniqueResult();
			}

			ConstantValues.TUNAI = (JenisPembayaran) session.createCriteria(JenisPembayaran.class)
					.add(Restrictions.eq("nama", "Tunai")).add(Restrictions.eq("akun", ConstantValues.KAS))
					.setMaxResults(1).uniqueResult();

			if (ConstantValues.TUNAI == null && ConstantValues.KAS != null) {
				ConstantValues.TUNAI = new JenisPembayaran();
				ConstantValues.TUNAI.setAkun(ConstantValues.KAS);
				ConstantValues.TUNAI.setNama("Tunai");
				ConstantValues.TUNAI.setDeskripsi("Bayar Tunai");

				session.getTransaction().begin();
				session.save(ConstantValues.TUNAI);
				session.getTransaction().commit();
			}

			ConstantValues.PRODUK_UMUM = (JenisProduk) session.createCriteria(JenisProduk.class)
					.add(Restrictions.eq("nama", "Umum")).setMaxResults(1).uniqueResult();

			if (ConstantValues.PRODUK_UMUM == null) {
				ConstantValues.PRODUK_UMUM = new JenisProduk();
				ConstantValues.PRODUK_UMUM.setNama("Umum");
				ConstantValues.PRODUK_UMUM.setKeterangan("Umum");

				session.getTransaction().begin();
				session.save(ConstantValues.PRODUK_UMUM);
				session.getTransaction().commit();
			}

			ConstantValues.MENU_MANAJEMEN_KRS_DOSEN = (Menu) session.createCriteria(Menu.class)
					.add(Restrictions.eq("url", "../master/monitor_krs_mahasiswa.zul")).setMaxResults(1).uniqueResult();

			ConstantValues.MENU_UTAMA_DOSEN = (Menu) session.createCriteria(Menu.class)
					.add(Restrictions.ilike("url", "master/pertemuan.zul", MatchMode.ANYWHERE)).setMaxResults(1)
					.uniqueResult();

			ConstantValues.MENU_UTAMA_MAHASISWA = (Menu) session.createCriteria(Menu.class)
					.add(Restrictions.ilike("url", "master/pertemuan.zul", MatchMode.ANYWHERE)).setMaxResults(1)
					.uniqueResult();

			ConstantValues.ANGGOTA_KOPERASI_REGULER = (JenisAnggotaKoperasi) session
					.createCriteria(JenisAnggotaKoperasi.class).add(Restrictions.ilike("nama", "Biasa"))
					.setMaxResults(1).uniqueResult();
			if (ConstantValues.ANGGOTA_KOPERASI_REGULER == null) {
				ConstantValues.ANGGOTA_KOPERASI_REGULER = new JenisAnggotaKoperasi();
				ConstantValues.ANGGOTA_KOPERASI_REGULER.setKeterangan("Biasa");
				ConstantValues.ANGGOTA_KOPERASI_REGULER.setNama("Biasa");

				session.getTransaction().begin();
				session.save(ConstantValues.ANGGOTA_KOPERASI_REGULER);
				session.getTransaction().commit();
			}

			ConstantValues.EMAIL = (JenisIdentitasAnggotaKoperasi) session
					.createCriteria(JenisIdentitasAnggotaKoperasi.class).add(Restrictions.ilike("nama", "Email"))
					.setMaxResults(1).uniqueResult();
			if (ConstantValues.EMAIL == null) {
				ConstantValues.EMAIL = new JenisIdentitasAnggotaKoperasi();
				ConstantValues.EMAIL.setKeterangan("Email");
				ConstantValues.EMAIL.setNama("Email");

				session.getTransaction().begin();
				session.save(ConstantValues.EMAIL);
				session.getTransaction().commit();
			}

			ConstantValues.NIM = (JenisIdentitasAnggotaKoperasi) session
					.createCriteria(JenisIdentitasAnggotaKoperasi.class).add(Restrictions.ilike("nama", "NIM"))
					.setMaxResults(1).uniqueResult();
			if (ConstantValues.NIM == null) {
				ConstantValues.NIM = new JenisIdentitasAnggotaKoperasi();
				ConstantValues.NIM.setKeterangan("NIM");
				ConstantValues.NIM.setNama("NIM");

				session.getTransaction().begin();
				session.save(ConstantValues.NIM);
				session.getTransaction().commit();
			}

			ConstantValues.NIDN = (JenisIdentitasAnggotaKoperasi) session
					.createCriteria(JenisIdentitasAnggotaKoperasi.class).add(Restrictions.ilike("nama", "NIDN"))
					.setMaxResults(1).uniqueResult();
			if (ConstantValues.NIDN == null) {
				ConstantValues.NIDN = new JenisIdentitasAnggotaKoperasi();
				ConstantValues.NIDN.setKeterangan("NIDN");
				ConstantValues.NIDN.setNama("NIDN");

				session.getTransaction().begin();
				session.save(ConstantValues.NIDN);
				session.getTransaction().commit();
			}

			ConstantValues.NUPTK = (JenisIdentitasAnggotaKoperasi) session
					.createCriteria(JenisIdentitasAnggotaKoperasi.class).add(Restrictions.ilike("nama", "NUPTK"))
					.setMaxResults(1).uniqueResult();
			if (ConstantValues.NUPTK == null) {
				ConstantValues.NUPTK = new JenisIdentitasAnggotaKoperasi();
				ConstantValues.NUPTK.setKeterangan("NUPTK");
				ConstantValues.NUPTK.setNama("NUPTK");

				session.getTransaction().begin();
				session.save(ConstantValues.NUPTK);
				session.getTransaction().commit();
			}

			ConstantValues.KTP = (JenisIdentitasAnggotaKoperasi) session
					.createCriteria(JenisIdentitasAnggotaKoperasi.class).add(Restrictions.ilike("nama", "KTP"))
					.setMaxResults(1).uniqueResult();
			if (ConstantValues.KTP == null) {
				ConstantValues.KTP = new JenisIdentitasAnggotaKoperasi();
				ConstantValues.KTP.setKeterangan("KTP");
				ConstantValues.KTP.setNama("KTP");

				session.getTransaction().begin();
				session.save(ConstantValues.KTP);
				session.getTransaction().commit();
			}

			ConstantValues.UMUM = (TipeAnggotaKoperasi) session.createCriteria(TipeAnggotaKoperasi.class)
					.add(Restrictions.ilike("nama", "Umum")).setMaxResults(1).uniqueResult();
			if (ConstantValues.UMUM == null) {
				ConstantValues.UMUM = new TipeAnggotaKoperasi();
				ConstantValues.UMUM.setKeterangan("Umum");
				ConstantValues.UMUM.setNama("Umum");

				session.getTransaction().begin();
				session.save(ConstantValues.UMUM);
				session.getTransaction().commit();
			}

			ConstantValues.BAYAR_TUNAI = (CaraPembayaranKoperasi) session.createCriteria(CaraPembayaranKoperasi.class)
					.add(Restrictions.ilike("nama", "Tunai")).setMaxResults(1).uniqueResult();
			if (ConstantValues.BAYAR_TUNAI == null) {
				ConstantValues.BAYAR_TUNAI = new CaraPembayaranKoperasi();
				ConstantValues.BAYAR_TUNAI.setKeterangan("Tunai");
				ConstantValues.BAYAR_TUNAI.setNama("Tunai");
				ConstantValues.BAYAR_TUNAI.setKode("002");

				session.getTransaction().begin();
				session.save(ConstantValues.BAYAR_TUNAI);
				session.getTransaction().commit();
			}

			ConstantValues.BAYAR_TRANSFER = (CaraPembayaranKoperasi) session
					.createCriteria(CaraPembayaranKoperasi.class).add(Restrictions.ilike("nama", "Transfer"))
					.setMaxResults(1).uniqueResult();
			if (ConstantValues.BAYAR_TRANSFER == null) {
				ConstantValues.BAYAR_TRANSFER = new CaraPembayaranKoperasi();
				ConstantValues.BAYAR_TRANSFER.setKeterangan("Transfer");
				ConstantValues.BAYAR_TRANSFER.setNama("Transfer");
				ConstantValues.BAYAR_TRANSFER.setKode("001");
				session.getTransaction().begin();
				session.save(ConstantValues.BAYAR_TRANSFER);
				session.getTransaction().commit();
			}

			ConstantValues.PINJAMAN = (TipeProdukKoperasi) session.createCriteria(TipeProdukKoperasi.class)
					.add(Restrictions.ilike("nama", "Pinjaman")).setMaxResults(1).uniqueResult();
			if (ConstantValues.PINJAMAN == null) {
				ConstantValues.PINJAMAN = new TipeProdukKoperasi();
				ConstantValues.PINJAMAN.setKeterangan("Pinjaman");
				ConstantValues.PINJAMAN.setNama("Pinjaman");

				session.getTransaction().begin();
				session.save(ConstantValues.PINJAMAN);
				session.getTransaction().commit();
			}

			ConstantValues.SIMPANAN = (TipeProdukKoperasi) session.createCriteria(TipeProdukKoperasi.class)
					.add(Restrictions.ilike("nama", "Simpanan")).setMaxResults(1).uniqueResult();
			if (ConstantValues.SIMPANAN == null) {
				ConstantValues.SIMPANAN = new TipeProdukKoperasi();
				ConstantValues.SIMPANAN.setKeterangan("Simpanan");
				ConstantValues.SIMPANAN.setNama("Simpanan");

				session.getTransaction().begin();
				session.save(ConstantValues.SIMPANAN);
				session.getTransaction().commit();
			}

//			

			ConstantValues.SETORAN = (JenisTransaksiKoperasi) session.createCriteria(JenisTransaksiKoperasi.class)
					.add(Restrictions.ilike("nama", "SETORAN")).setMaxResults(1).uniqueResult();
			if (ConstantValues.SETORAN == null) {
				ConstantValues.SETORAN = new JenisTransaksiKoperasi();
				ConstantValues.SETORAN.setKeterangan("SETORAN");
				ConstantValues.SETORAN.setNama("SETORAN");
				ConstantValues.SETORAN.setTipeProdukKoperasi(ConstantValues.SIMPANAN);

				session.getTransaction().begin();
				session.save(ConstantValues.SETORAN);
				session.getTransaction().commit();
			}

			ConstantValues.PENARIKAN = (JenisTransaksiKoperasi) session.createCriteria(JenisTransaksiKoperasi.class)
					.add(Restrictions.ilike("nama", "PENARIKAN")).setMaxResults(1).uniqueResult();
			if (ConstantValues.PENARIKAN == null) {
				ConstantValues.PENARIKAN = new JenisTransaksiKoperasi();
				ConstantValues.PENARIKAN.setKeterangan("PENARIKAN");
				ConstantValues.PENARIKAN.setNama("PENARIKAN");
				ConstantValues.PENARIKAN.setTipeProdukKoperasi(ConstantValues.SIMPANAN);

				session.getTransaction().begin();
				session.save(ConstantValues.PENARIKAN);
				session.getTransaction().commit();
			}

			ConstantValues.BAGI_HASIL = (JenisTransaksiKoperasi) session.createCriteria(JenisTransaksiKoperasi.class)
					.add(Restrictions.ilike("nama", "BAGI HASIL")).setMaxResults(1).uniqueResult();
			if (ConstantValues.BAGI_HASIL == null) {
				ConstantValues.BAGI_HASIL = new JenisTransaksiKoperasi();
				ConstantValues.BAGI_HASIL.setKeterangan("BAGI HASIL");
				ConstantValues.BAGI_HASIL.setNama("BAGI HASIL");
				ConstantValues.BAGI_HASIL.setTipeProdukKoperasi(ConstantValues.SIMPANAN);

				session.getTransaction().begin();
				session.save(ConstantValues.BAGI_HASIL);
				session.getTransaction().commit();
			}

			ConstantValues.BAGI_HASIL_BERJANGKA = (JenisTransaksiKoperasi) session
					.createCriteria(JenisTransaksiKoperasi.class)
					.add(Restrictions.ilike("nama", "BAGI HASIL BERJANGKA")).setMaxResults(1).uniqueResult();
			if (ConstantValues.BAGI_HASIL_BERJANGKA == null) {
				ConstantValues.BAGI_HASIL_BERJANGKA = new JenisTransaksiKoperasi();
				ConstantValues.BAGI_HASIL_BERJANGKA.setKeterangan("BAGI HASIL BERJANGKA");
				ConstantValues.BAGI_HASIL_BERJANGKA.setNama("BAGI HASIL BERJANGKA");
				ConstantValues.BAGI_HASIL_BERJANGKA.setTipeProdukKoperasi(ConstantValues.SIMPANAN);

				session.getTransaction().begin();
				session.save(ConstantValues.BAGI_HASIL_BERJANGKA);
				session.getTransaction().commit();
			}

			ConstantValues.BIAYA_ADMIN = (JenisTransaksiKoperasi) session.createCriteria(JenisTransaksiKoperasi.class)
					.add(Restrictions.ilike("nama", "BIAYA ADMIN")).setMaxResults(1).uniqueResult();
			if (ConstantValues.BIAYA_ADMIN == null) {
				ConstantValues.BIAYA_ADMIN = new JenisTransaksiKoperasi();
				ConstantValues.BIAYA_ADMIN.setKeterangan("BIAYA ADMIN");
				ConstantValues.BIAYA_ADMIN.setNama("BIAYA ADMIN");
				ConstantValues.BIAYA_ADMIN.setTipeProdukKoperasi(ConstantValues.SIMPANAN);

				session.getTransaction().begin();
				session.save(ConstantValues.BIAYA_ADMIN);
				session.getTransaction().commit();
			}

			ConstantValues.SIMPANAN_POKOK = (JenisTransaksiKoperasi) session
					.createCriteria(JenisTransaksiKoperasi.class).add(Restrictions.ilike("nama", "SIMPANAN POKOK"))
					.setMaxResults(1).uniqueResult();
			if (ConstantValues.SIMPANAN_POKOK == null) {
				ConstantValues.SIMPANAN_POKOK = new JenisTransaksiKoperasi();
				ConstantValues.SIMPANAN_POKOK.setKeterangan("SIMPANAN POKOK");
				ConstantValues.SIMPANAN_POKOK.setNama("SIMPANAN POKOK");
				ConstantValues.SIMPANAN_POKOK.setTipeProdukKoperasi(ConstantValues.SIMPANAN);

				session.getTransaction().begin();
				session.save(ConstantValues.SIMPANAN_POKOK);
				session.getTransaction().commit();
			}

			ConstantValues.SIMPANAN_WAJIB = (JenisTransaksiKoperasi) session
					.createCriteria(JenisTransaksiKoperasi.class).add(Restrictions.ilike("nama", "SIMPANAN WAJIB"))
					.setMaxResults(1).uniqueResult();
			if (ConstantValues.SIMPANAN_WAJIB == null) {
				ConstantValues.SIMPANAN_WAJIB = new JenisTransaksiKoperasi();
				ConstantValues.SIMPANAN_WAJIB.setKeterangan("SIMPANAN WAJIB");
				ConstantValues.SIMPANAN_WAJIB.setNama("SIMPANAN WAJIB");
				ConstantValues.SIMPANAN_WAJIB.setTipeProdukKoperasi(ConstantValues.SIMPANAN);

				session.getTransaction().begin();
				session.save(ConstantValues.SIMPANAN_WAJIB);
				session.getTransaction().commit();
			}

			ConstantValues.SIMPANAN_KHUSUS = (JenisTransaksiKoperasi) session
					.createCriteria(JenisTransaksiKoperasi.class).add(Restrictions.ilike("nama", "SIMPANAN KHUSUS"))
					.setMaxResults(1).uniqueResult();
			if (ConstantValues.SIMPANAN_KHUSUS == null) {
				ConstantValues.SIMPANAN_KHUSUS = new JenisTransaksiKoperasi();
				ConstantValues.SIMPANAN_KHUSUS.setKeterangan("SIMPANAN KHUSUS");
				ConstantValues.SIMPANAN_KHUSUS.setNama("SIMPANAN KHUSUS");
				ConstantValues.SIMPANAN_KHUSUS.setTipeProdukKoperasi(ConstantValues.SIMPANAN);

				session.getTransaction().begin();
				session.save(ConstantValues.SIMPANAN_KHUSUS);
				session.getTransaction().commit();
			}

			ConstantValues.KOREKSI_SETORAN = (JenisTransaksiKoperasi) session
					.createCriteria(JenisTransaksiKoperasi.class).add(Restrictions.ilike("nama", "KOREKSI SETORAN"))
					.setMaxResults(1).uniqueResult();
			if (ConstantValues.KOREKSI_SETORAN == null) {
				ConstantValues.KOREKSI_SETORAN = new JenisTransaksiKoperasi();
				ConstantValues.KOREKSI_SETORAN.setKeterangan("KOREKSI SETORAN");
				ConstantValues.KOREKSI_SETORAN.setNama("KOREKSI SETORAN");
				ConstantValues.KOREKSI_SETORAN.setTipeProdukKoperasi(ConstantValues.SIMPANAN);

				session.getTransaction().begin();
				session.save(ConstantValues.KOREKSI_SETORAN);
				session.getTransaction().commit();
			}

			ConstantValues.KOREKSI_PENARIKAN = (JenisTransaksiKoperasi) session
					.createCriteria(JenisTransaksiKoperasi.class).add(Restrictions.ilike("nama", "KOREKSI PENARIKAN"))
					.setMaxResults(1).uniqueResult();
			if (ConstantValues.KOREKSI_PENARIKAN == null) {
				ConstantValues.KOREKSI_PENARIKAN = new JenisTransaksiKoperasi();
				ConstantValues.KOREKSI_PENARIKAN.setKeterangan("KOREKSI PENARIKAN");
				ConstantValues.KOREKSI_PENARIKAN.setNama("KOREKSI PENARIKAN");
				ConstantValues.KOREKSI_PENARIKAN.setTipeProdukKoperasi(ConstantValues.SIMPANAN);

				session.getTransaction().begin();
				session.save(ConstantValues.KOREKSI_PENARIKAN);
				session.getTransaction().commit();
			}

			ConstantValues.KOREKSI_BAGI_HASIL = (JenisTransaksiKoperasi) session
					.createCriteria(JenisTransaksiKoperasi.class).add(Restrictions.ilike("nama", "KOREKSI BAGI HASIL"))
					.setMaxResults(1).uniqueResult();
			if (ConstantValues.KOREKSI_BAGI_HASIL == null) {
				ConstantValues.KOREKSI_BAGI_HASIL = new JenisTransaksiKoperasi();
				ConstantValues.KOREKSI_BAGI_HASIL.setKeterangan("KOREKSI BAGI HASIL");
				ConstantValues.KOREKSI_BAGI_HASIL.setNama("KOREKSI BAGI HASIL");
				ConstantValues.KOREKSI_BAGI_HASIL.setTipeProdukKoperasi(ConstantValues.SIMPANAN);

				session.getTransaction().begin();
				session.save(ConstantValues.KOREKSI_BAGI_HASIL);
				session.getTransaction().commit();
			}

			ConstantValues.SALDO_SIMPANAN = (JenisTransaksiKoperasi) session
					.createCriteria(JenisTransaksiKoperasi.class).add(Restrictions.ilike("nama", "SALDO SIMPANAN"))
					.setMaxResults(1).uniqueResult();
			if (ConstantValues.SALDO_SIMPANAN == null) {
				ConstantValues.SALDO_SIMPANAN = new JenisTransaksiKoperasi();
				ConstantValues.SALDO_SIMPANAN.setKeterangan("SALDO SIMPANAN");
				ConstantValues.SALDO_SIMPANAN.setNama("SALDO SIMPANAN");
				ConstantValues.SALDO_SIMPANAN.setTipeProdukKoperasi(ConstantValues.SIMPANAN);

				session.getTransaction().begin();
				session.save(ConstantValues.SALDO_SIMPANAN);
				session.getTransaction().commit();
			}

			ConstantValues.PINDAH_BUKU = (JenisTransaksiKoperasi) session.createCriteria(JenisTransaksiKoperasi.class)
					.add(Restrictions.ilike("nama", "PINDAH BUKU")).setMaxResults(1).uniqueResult();
			if (ConstantValues.PINDAH_BUKU == null) {
				ConstantValues.PINDAH_BUKU = new JenisTransaksiKoperasi();
				ConstantValues.PINDAH_BUKU.setKeterangan("PINDAH BUKU");
				ConstantValues.PINDAH_BUKU.setNama("PINDAH BUKU");
				ConstantValues.PINDAH_BUKU.setTipeProdukKoperasi(ConstantValues.SIMPANAN);

				session.getTransaction().begin();
				session.save(ConstantValues.PINDAH_BUKU);
				session.getTransaction().commit();
			}

			ConstantValues.REALISASI = (JenisTransaksiKoperasi) session.createCriteria(JenisTransaksiKoperasi.class)
					.add(Restrictions.ilike("nama", "REALISASI")).setMaxResults(1).uniqueResult();
			if (ConstantValues.REALISASI == null) {
				ConstantValues.REALISASI = new JenisTransaksiKoperasi();
				ConstantValues.REALISASI.setKeterangan("REALISASI");
				ConstantValues.REALISASI.setNama("REALISASI");
				ConstantValues.REALISASI.setTipeProdukKoperasi(ConstantValues.PINJAMAN);

				session.getTransaction().begin();
				session.save(ConstantValues.REALISASI);
				session.getTransaction().commit();
			}

			ConstantValues.ANSURAN = (JenisTransaksiKoperasi) session.createCriteria(JenisTransaksiKoperasi.class)
					.add(Restrictions.ilike("nama", "ANSURAN")).setMaxResults(1).uniqueResult();
			if (ConstantValues.ANSURAN == null) {
				ConstantValues.ANSURAN = new JenisTransaksiKoperasi();
				ConstantValues.ANSURAN.setKeterangan("ANSURAN");
				ConstantValues.ANSURAN.setNama("ANSURAN");
				ConstantValues.ANSURAN.setTipeProdukKoperasi(ConstantValues.PINJAMAN);

				session.getTransaction().begin();
				session.save(ConstantValues.ANSURAN);
				session.getTransaction().commit();
			}

			ConstantValues.MAHASISWA = (TipeAnggotaKoperasi) session.createCriteria(TipeAnggotaKoperasi.class)
					.add(Restrictions.ilike("nama", "Mahasiswa")).setMaxResults(1).uniqueResult();
			if (ConstantValues.MAHASISWA == null) {
				ConstantValues.MAHASISWA = new TipeAnggotaKoperasi();
				ConstantValues.MAHASISWA.setKeterangan("Mahasiswa");
				ConstantValues.MAHASISWA.setNama("Mahasiswa");

				session.getTransaction().begin();
				session.save(ConstantValues.MAHASISWA);
				session.getTransaction().commit();
			}

			ConstantValues.DOSEN = (TipeAnggotaKoperasi) session.createCriteria(TipeAnggotaKoperasi.class)
					.add(Restrictions.ilike("nama", "Dosen")).setMaxResults(1).uniqueResult();
			if (ConstantValues.DOSEN == null) {
				ConstantValues.DOSEN = new TipeAnggotaKoperasi();
				ConstantValues.DOSEN.setKeterangan("Dosen");
				ConstantValues.DOSEN.setNama("Dosen");

				session.getTransaction().begin();
				session.save(ConstantValues.DOSEN);
				session.getTransaction().commit();
			}

			ConstantValues.PEGAWAI = (TipeAnggotaKoperasi) session.createCriteria(TipeAnggotaKoperasi.class)
					.add(Restrictions.ilike("nama", "Pegawai")).setMaxResults(1).uniqueResult();
			if (ConstantValues.PEGAWAI == null) {
				ConstantValues.PEGAWAI = new TipeAnggotaKoperasi();
				ConstantValues.PEGAWAI.setKeterangan("Pegawai");
				ConstantValues.PEGAWAI.setNama("Pegawai");

				session.getTransaction().begin();
				session.save(ConstantValues.PEGAWAI);
				session.getTransaction().commit();
			}

			ConstantValues.SISWA = (TipeAnggotaKoperasi) session.createCriteria(TipeAnggotaKoperasi.class)
					.add(Restrictions.ilike("nama", "Siswa")).setMaxResults(1).uniqueResult();
			if (ConstantValues.SISWA == null) {
				ConstantValues.SISWA = new TipeAnggotaKoperasi();
				ConstantValues.SISWA.setKeterangan("Siswa");
				ConstantValues.SISWA.setNama("Siswa");

				session.getTransaction().begin();
				session.save(ConstantValues.SISWA);
				session.getTransaction().commit();
			}

			ConstantValues.GURU = (TipeAnggotaKoperasi) session.createCriteria(TipeAnggotaKoperasi.class)
					.add(Restrictions.ilike("nama", "Guru")).setMaxResults(1).uniqueResult();
			if (ConstantValues.GURU == null) {
				ConstantValues.GURU = new TipeAnggotaKoperasi();
				ConstantValues.GURU.setKeterangan("Guru");
				ConstantValues.GURU.setNama("Guru");

				session.getTransaction().begin();
				session.save(ConstantValues.GURU);
				session.getTransaction().commit();
			}

			ConstantValues.DOSEN_BIASA = (StatusKewajibanBebanDosen) session
					.createCriteria(StatusKewajibanBebanDosen.class).add(Restrictions.ilike("kode", "001"))
					.setMaxResults(1).uniqueResult();
			if (ConstantValues.DOSEN_BIASA == null) {
				ConstantValues.DOSEN_BIASA = new StatusKewajibanBebanDosen();
				ConstantValues.DOSEN_BIASA.setKode("001");
				ConstantValues.DOSEN_BIASA.setNama("Dosen biasa");

				session.getTransaction().begin();
				session.save(ConstantValues.DOSEN_BIASA);
				session.getTransaction().commit();
			}

			ConstantValues.DOSEN_PROFESOR = (StatusKewajibanBebanDosen) session
					.createCriteria(StatusKewajibanBebanDosen.class).add(Restrictions.ilike("kode", "002"))
					.setMaxResults(1).uniqueResult();
			if (ConstantValues.DOSEN_PROFESOR == null) {
				ConstantValues.DOSEN_PROFESOR = new StatusKewajibanBebanDosen();
				ConstantValues.DOSEN_PROFESOR.setKode("002");
				ConstantValues.DOSEN_PROFESOR.setNama("Dosen profesor");

				session.getTransaction().begin();
				session.save(ConstantValues.DOSEN_PROFESOR);
				session.getTransaction().commit();
			}

			ConstantValues.DOSEN_BIASA_DENGAN_TUGAS_TAMBAHAN = (StatusKewajibanBebanDosen) session
					.createCriteria(StatusKewajibanBebanDosen.class).add(Restrictions.ilike("kode", "003"))
					.setMaxResults(1).uniqueResult();
			if (ConstantValues.DOSEN_BIASA_DENGAN_TUGAS_TAMBAHAN == null) {
				ConstantValues.DOSEN_BIASA_DENGAN_TUGAS_TAMBAHAN = new StatusKewajibanBebanDosen();
				ConstantValues.DOSEN_BIASA_DENGAN_TUGAS_TAMBAHAN.setKode("003");
				ConstantValues.DOSEN_BIASA_DENGAN_TUGAS_TAMBAHAN.setNama("Dosen biasa dengan tugas tambahan");

				session.getTransaction().begin();
				session.save(ConstantValues.DOSEN_BIASA_DENGAN_TUGAS_TAMBAHAN);
				session.getTransaction().commit();
			}

			ConstantValues.DOSEN_PROFESOR_DENGAN_TUGAS_TAMBAHAN = (StatusKewajibanBebanDosen) session
					.createCriteria(StatusKewajibanBebanDosen.class).add(Restrictions.ilike("kode", "004"))
					.setMaxResults(1).uniqueResult();
			if (ConstantValues.DOSEN_PROFESOR_DENGAN_TUGAS_TAMBAHAN == null) {
				ConstantValues.DOSEN_PROFESOR_DENGAN_TUGAS_TAMBAHAN = new StatusKewajibanBebanDosen();
				ConstantValues.DOSEN_PROFESOR_DENGAN_TUGAS_TAMBAHAN.setKode("004");
				ConstantValues.DOSEN_PROFESOR_DENGAN_TUGAS_TAMBAHAN.setNama("Dosen profesor dengan tugas tambahan");

				session.getTransaction().begin();
				session.save(ConstantValues.DOSEN_PROFESOR_DENGAN_TUGAS_TAMBAHAN);
				session.getTransaction().commit();
			}

			ConstantValues.DOSEN_DENGAN_JABATAN_STRUKTURAL = (StatusKewajibanBebanDosen) session
					.createCriteria(StatusKewajibanBebanDosen.class).add(Restrictions.ilike("kode", "005"))
					.setMaxResults(1).uniqueResult();
			if (ConstantValues.DOSEN_DENGAN_JABATAN_STRUKTURAL == null) {
				ConstantValues.DOSEN_DENGAN_JABATAN_STRUKTURAL = new StatusKewajibanBebanDosen();
				ConstantValues.DOSEN_DENGAN_JABATAN_STRUKTURAL.setKode("005");
				ConstantValues.DOSEN_DENGAN_JABATAN_STRUKTURAL.setNama("Dosen dengan jabatan struktural");

				session.getTransaction().begin();
				session.save(ConstantValues.DOSEN_DENGAN_JABATAN_STRUKTURAL);
				session.getTransaction().commit();
			}

			ConstantValues.AKTIF_PEGAWAI = (StatusPegawai) session.createCriteria(StatusPegawai.class)
					.add(Restrictions.ilike("nama", "Aktif")).setMaxResults(1).uniqueResult();
			if (ConstantValues.AKTIF_PEGAWAI == null) {
				ConstantValues.AKTIF_PEGAWAI = new StatusPegawai();
				ConstantValues.AKTIF_PEGAWAI.setNama("Aktif");

				session.getTransaction().begin();
				session.save(ConstantValues.AKTIF_PEGAWAI);
				session.getTransaction().commit();
			}

			ConstantValues.CUTI_PEGAWAI = (StatusPegawai) session.createCriteria(StatusPegawai.class)
					.add(Restrictions.ilike("nama", "Cuti")).setMaxResults(1).uniqueResult();
			if (ConstantValues.CUTI_PEGAWAI == null) {
				ConstantValues.CUTI_PEGAWAI = new StatusPegawai();
				ConstantValues.CUTI_PEGAWAI.setNama("Cuti");

				session.getTransaction().begin();
				session.save(ConstantValues.CUTI_PEGAWAI);
				session.getTransaction().commit();
			}

			ConstantValues.CUTI_HAJI = (StatusPegawai) session.createCriteria(StatusPegawai.class)
					.add(Restrictions.ilike("nama", "Cuti Haji")).setMaxResults(1).uniqueResult();
			if (ConstantValues.CUTI_HAJI == null) {
				ConstantValues.CUTI_HAJI = new StatusPegawai();
				ConstantValues.CUTI_HAJI.setNama("Cuti Haji");

				session.getTransaction().begin();
				session.save(ConstantValues.CUTI_HAJI);
				session.getTransaction().commit();
			}

			ConstantValues.TIDAK_AKTIF_PEGAWAI = (StatusPegawai) session.createCriteria(StatusPegawai.class)
					.add(Restrictions.ilike("nama", "Tidak Aktif")).setMaxResults(1).uniqueResult();
			if (ConstantValues.TIDAK_AKTIF_PEGAWAI == null) {
				ConstantValues.TIDAK_AKTIF_PEGAWAI = new StatusPegawai();
				ConstantValues.TIDAK_AKTIF_PEGAWAI.setNama("Tidak Aktif");

				session.getTransaction().begin();
				session.save(ConstantValues.TIDAK_AKTIF_PEGAWAI);
				session.getTransaction().commit();
			}

			ConstantValues.KELUAR_PEGAWAI = (StatusPegawai) session.createCriteria(StatusPegawai.class)
					.add(Restrictions.ilike("nama", "Keluar")).setMaxResults(1).uniqueResult();
			if (ConstantValues.KELUAR_PEGAWAI == null) {
				ConstantValues.KELUAR_PEGAWAI = new StatusPegawai();
				ConstantValues.KELUAR_PEGAWAI.setNama("Keluar");

				session.getTransaction().begin();
				session.save(ConstantValues.KELUAR_PEGAWAI);
				session.getTransaction().commit();
			}

			ConstantValues.PENSIUN_PEGAWAI = (StatusPegawai) session.createCriteria(StatusPegawai.class)
					.add(Restrictions.ilike("nama", "Pensiun")).setMaxResults(1).uniqueResult();
			if (ConstantValues.PENSIUN_PEGAWAI == null) {
				ConstantValues.PENSIUN_PEGAWAI = new StatusPegawai();
				ConstantValues.PENSIUN_PEGAWAI.setNama("Pensiun");

				session.getTransaction().begin();
				session.save(ConstantValues.PENSIUN_PEGAWAI);
				session.getTransaction().commit();
			}

			ConstantValues.MENINGGAL_PEGAWAI = (StatusPegawai) session.createCriteria(StatusPegawai.class).add(
					Restrictions.or(Restrictions.ilike("nama", "Meninggal"), Restrictions.ilike("nama", "ALMARHUM")))
					.setMaxResults(1).uniqueResult();
			if (ConstantValues.MENINGGAL_PEGAWAI == null) {
				ConstantValues.MENINGGAL_PEGAWAI = new StatusPegawai();
				ConstantValues.MENINGGAL_PEGAWAI.setNama("Meninggal");

				session.getTransaction().begin();
				session.save(ConstantValues.MENINGGAL_PEGAWAI);
				session.getTransaction().commit();
			}

			ConstantValues.TUGAS_BELAJAR = (Statusabsensi) session.createCriteria(Statusabsensi.class)
					.add(Restrictions.idEq(20L)).uniqueResult();

			if (ConstantValues.TUGAS_BELAJAR == null) {
				ConstantValues.TUGAS_BELAJAR = new Statusabsensi(20L);
				ConstantValues.TUGAS_BELAJAR.setKode("TB");
				ConstantValues.TUGAS_BELAJAR.setNama("Tugas Belajar");

				session.getTransaction().begin();
				session.save(ConstantValues.TUGAS_BELAJAR);
				session.getTransaction().commit();
			}

			ConstantValues.CUTI_TAHUNAN = (Statusabsensi) session.createCriteria(Statusabsensi.class)
					.add(Restrictions.idEq(21L)).uniqueResult();

			if (ConstantValues.CUTI_TAHUNAN == null) {
				ConstantValues.CUTI_TAHUNAN = new Statusabsensi(21L);
				ConstantValues.CUTI_TAHUNAN.setKode("CT");
				ConstantValues.CUTI_TAHUNAN.setNama("Cuti Tahunan");

				session.getTransaction().begin();
				session.save(ConstantValues.CUTI_TAHUNAN);
				session.getTransaction().commit();
			}

			ConstantValues.CUTI_SAKIT = (Statusabsensi) session.createCriteria(Statusabsensi.class)
					.add(Restrictions.idEq(22L)).uniqueResult();

			if (ConstantValues.CUTI_SAKIT == null) {
				ConstantValues.CUTI_SAKIT = new Statusabsensi(22L);
				ConstantValues.CUTI_SAKIT.setKode("CS");
				ConstantValues.CUTI_SAKIT.setNama("Cuti Sakit");

				session.getTransaction().begin();
				session.save(ConstantValues.CUTI_SAKIT);
				session.getTransaction().commit();
			}

			ConstantValues.CUTI_ALASAN_PENTING = (Statusabsensi) session.createCriteria(Statusabsensi.class)
					.add(Restrictions.idEq(23L)).uniqueResult();

			if (ConstantValues.CUTI_ALASAN_PENTING == null) {
				ConstantValues.CUTI_ALASAN_PENTING = new Statusabsensi(23L);
				ConstantValues.CUTI_ALASAN_PENTING.setKode("CKAP");
				ConstantValues.CUTI_ALASAN_PENTING.setNama("Cuti Karena Alasan Penting");

				session.getTransaction().begin();
				session.save(ConstantValues.CUTI_ALASAN_PENTING);
				session.getTransaction().commit();
			}

			ConstantValues.CUTI_BESAR = (Statusabsensi) session.createCriteria(Statusabsensi.class)
					.add(Restrictions.idEq(24L)).uniqueResult();

			if (ConstantValues.CUTI_BESAR == null) {
				ConstantValues.CUTI_BESAR = new Statusabsensi(24L);
				ConstantValues.CUTI_BESAR.setKode("CB");
				ConstantValues.CUTI_BESAR.setNama("Cuti Besar Pertama");

				session.getTransaction().begin();
				session.save(ConstantValues.CUTI_BESAR);
				session.getTransaction().commit();
			}

			ConstantValues.CUTI_BESAR_II = (Statusabsensi) session.createCriteria(Statusabsensi.class)
					.add(Restrictions.idEq(124L)).uniqueResult();

			if (ConstantValues.CUTI_BESAR_II == null) {
				ConstantValues.CUTI_BESAR_II = new Statusabsensi(124L);
				ConstantValues.CUTI_BESAR_II.setKode("CB2");
				ConstantValues.CUTI_BESAR_II.setNama("Cuti Besar Ke 2");

				session.getTransaction().begin();
				session.save(ConstantValues.CUTI_BESAR_II);
				session.getTransaction().commit();
			}

			ConstantValues.CUTI_DILUAR_TANGGUNGAN = (Statusabsensi) session.createCriteria(Statusabsensi.class)
					.add(Restrictions.idEq(25L)).uniqueResult();

			if (ConstantValues.CUTI_DILUAR_TANGGUNGAN == null) {
				ConstantValues.CUTI_DILUAR_TANGGUNGAN = new Statusabsensi(25L);
				ConstantValues.CUTI_DILUAR_TANGGUNGAN.setKode("CDLT");
				ConstantValues.CUTI_DILUAR_TANGGUNGAN.setNama("Cuti di Luar Tanggungan");

				session.getTransaction().begin();
				session.save(ConstantValues.CUTI_DILUAR_TANGGUNGAN);
				session.getTransaction().commit();
			}

			ConstantValues.CUTI_STUDI_ATAU_PENELITIAN = (Statusabsensi) session.createCriteria(Statusabsensi.class)
					.add(Restrictions.idEq(26L)).uniqueResult();

			if (ConstantValues.CUTI_STUDI_ATAU_PENELITIAN == null) {
				ConstantValues.CUTI_STUDI_ATAU_PENELITIAN = new Statusabsensi(26L);
				ConstantValues.CUTI_STUDI_ATAU_PENELITIAN.setKode("CSP");
				ConstantValues.CUTI_STUDI_ATAU_PENELITIAN.setNama("Cuti Studi atau Penelitian Dosen");

				session.getTransaction().begin();
				session.save(ConstantValues.CUTI_STUDI_ATAU_PENELITIAN);
				session.getTransaction().commit();
			}

			if (ConstantValues.MASUK == null) {
				ConstantValues.MASUK = new Statusabsensi(1L);
				ConstantValues.MASUK.setKode("M");
				ConstantValues.MASUK.setNama("Hadir");

				session.getTransaction().begin();
				session.save(ConstantValues.MASUK);
				session.getTransaction().commit();
			}
			ConstantValues.MASUK.setNama("Hadir");

			if (ConstantValues.TIDAK_ADA_ALASAN == null) {
				ConstantValues.TIDAK_ADA_ALASAN = new Statusabsensi(2L);
				ConstantValues.TIDAK_ADA_ALASAN.setKode("A");
				ConstantValues.TIDAK_ADA_ALASAN.setNama("Tidak Ada Alasan");
				session.getTransaction().begin();
				session.save(ConstantValues.TIDAK_ADA_ALASAN);
				session.getTransaction().commit();
			}

			if (ConstantValues.SAKIT == null) {
				ConstantValues.SAKIT = new Statusabsensi(3L);
				ConstantValues.SAKIT.setKode("S");
				ConstantValues.SAKIT.setNama("Sakit");
				session.getTransaction().begin();
				session.save(ConstantValues.SAKIT);
				session.getTransaction().commit();
			}

			if (ConstantValues.IZIN == null) {
				ConstantValues.IZIN = new Statusabsensi(4L);
				ConstantValues.IZIN.setKode("I");
				ConstantValues.IZIN.setNama("Izin");
				session.getTransaction().begin();
				session.save(ConstantValues.IZIN);
				session.getTransaction().commit();
			}

			if (ConstantValues.STATUS_CUTI == null) {
				ConstantValues.STATUS_CUTI = new Statusabsensi(6L);
				ConstantValues.STATUS_CUTI.setKode("C");
				ConstantValues.STATUS_CUTI.setNama("Cuti");
				session.getTransaction().begin();
				session.save(ConstantValues.STATUS_CUTI);
				session.getTransaction().commit();
			}

			if (ConstantValues.CUTI_HAMIL == null) {
				ConstantValues.CUTI_HAMIL = new Statusabsensi(7L);
				ConstantValues.CUTI_HAMIL.setKode("H");
				ConstantValues.CUTI_HAMIL.setNama("Cuti Hamil");
				session.getTransaction().begin();
				session.save(ConstantValues.CUTI_HAMIL);
				session.getTransaction().commit();
			}

			if (ConstantValues.DINAS_LUAR == null) {
				ConstantValues.DINAS_LUAR = new Statusabsensi(8L);
				ConstantValues.DINAS_LUAR.setKode("D");
				ConstantValues.DINAS_LUAR.setNama("Dinas Luar");
				session.getTransaction().begin();
				session.save(ConstantValues.DINAS_LUAR);
				session.getTransaction().commit();
			}

			if (ConstantValues.BELUM_ABSEN == null) {
				ConstantValues.BELUM_ABSEN = new Statusabsensi(5L);
				ConstantValues.BELUM_ABSEN.setKode("-");
				ConstantValues.BELUM_ABSEN.setNama("-");
				session.getTransaction().begin();
				session.save(ConstantValues.BELUM_ABSEN);
				session.getTransaction().commit();
			}

			ConstantValues.CUTI_MELAHIRKAN = (Statusabsensi) session.createCriteria(Statusabsensi.class)
					.add(Restrictions.idEq(117L)).uniqueResult();

			if (ConstantValues.CUTI_MELAHIRKAN == null) {
				ConstantValues.CUTI_MELAHIRKAN = new Statusabsensi(117L);
				ConstantValues.CUTI_MELAHIRKAN.setKode("CM");
				ConstantValues.CUTI_MELAHIRKAN.setNama("Cuti Melahirkan");
				session.getTransaction().begin();
				session.save(ConstantValues.CUTI_MELAHIRKAN);
				session.getTransaction().commit();
			}

			ConstantValues.CUTI_HAJI_PEGAWAI = (Statusabsensi) session.createCriteria(Statusabsensi.class)
					.add(Restrictions.idEq(118L)).uniqueResult();

			if (ConstantValues.CUTI_HAJI_PEGAWAI == null) {
				ConstantValues.CUTI_HAJI_PEGAWAI = new Statusabsensi(118L);
				ConstantValues.CUTI_HAJI_PEGAWAI.setKode("CH");
				ConstantValues.CUTI_HAJI_PEGAWAI.setNama("Cuti Haji");
				session.getTransaction().begin();
				session.save(ConstantValues.CUTI_HAJI_PEGAWAI);
				session.getTransaction().commit();
			}

			ConstantValues.CUTI_UMROH_PEGAWAI = (Statusabsensi) session.createCriteria(Statusabsensi.class)
					.add(Restrictions.idEq(1118L)).uniqueResult();

			if (ConstantValues.CUTI_UMROH_PEGAWAI == null) {
				ConstantValues.CUTI_UMROH_PEGAWAI = new Statusabsensi(1118L);
				ConstantValues.CUTI_UMROH_PEGAWAI.setKode("CU");
				ConstantValues.CUTI_UMROH_PEGAWAI.setNama("Cuti Umroh");
				session.getTransaction().begin();
				session.save(ConstantValues.CUTI_UMROH_PEGAWAI);
				session.getTransaction().commit();
			}

			ConstantValues.CUTI_PENTING = (Statusabsensi) session.createCriteria(Statusabsensi.class)
					.add(Restrictions.idEq(119L)).uniqueResult();

			if (ConstantValues.CUTI_PENTING == null) {
				ConstantValues.CUTI_PENTING = new Statusabsensi(119L);
				ConstantValues.CUTI_PENTING.setKode("CP");
				ConstantValues.CUTI_PENTING.setNama("Cuti Penting");
				session.getTransaction().begin();
				session.save(ConstantValues.CUTI_PENTING);
				session.getTransaction().commit();
			}

			Common.reloadJenisKegiatans(session);

			ConstantValues.DEFAULT_PEMBOBOTAN_NILAI = (PembombotanNilai) session.createCriteria(PembombotanNilai.class)
					.setMaxResults(1).add(Restrictions.eq("defaultPembobotan", true)).uniqueResult();
			System.out.print("DEFAULT_PEMBOBOTAN_NILAI = " + ConstantValues.DEFAULT_PEMBOBOTAN_NILAI);
			if (ConstantValues.DEFAULT_PEMBOBOTAN_NILAI == null) {
				ConstantValues.DEFAULT_PEMBOBOTAN_NILAI = new PembombotanNilai();
				ConstantValues.DEFAULT_PEMBOBOTAN_NILAI.setDefaultPembobotan(true);
				ConstantValues.DEFAULT_PEMBOBOTAN_NILAI.setForm(20.0);
				ConstantValues.DEFAULT_PEMBOBOTAN_NILAI.setUts(30.0);
				ConstantValues.DEFAULT_PEMBOBOTAN_NILAI.setUas(50.0);

				session.getTransaction().begin();
				session.save(ConstantValues.DEFAULT_PEMBOBOTAN_NILAI);
				session.getTransaction().commit();

			}

			ConstantValues.KAPRODI = (Jabatan) session.createCriteria(Jabatan.class)
					.add(Restrictions.eq("nama", Staff.KAPRODI)).uniqueResult();
			if (ConstantValues.KAPRODI == null) {
				ConstantValues.KAPRODI = new Jabatan();
				ConstantValues.KAPRODI.setNama(Staff.KAPRODI);
				ConstantValues.KAPRODI.setKeterangan("Ketua " + Common.getBahasaConfig("Jurusan"));
				ConstantValues.KAPRODI.setEq_sks(0);

				session.getTransaction().begin();
				session.save(ConstantValues.KAPRODI);
				session.getTransaction().commit();
			}

			ConstantValues.d3 = (Jenjang) session.createCriteria(Jenjang.class).setMaxResults(1)
					.add((Restrictions.or(Restrictions.eq("nama", "D3"), Restrictions.eq("nama", "Diploma Tiga (D3)"))))
					.uniqueResult();

			ConstantValues.s1 = (Jenjang) session.createCriteria(Jenjang.class)
					.add(Restrictions.or(Restrictions.ilike("nama", "Strata Satu (S1)"),
							Restrictions.or(Restrictions.ilike("nama", "S1"), Restrictions.ilike("nama", "Strata 1"))))
					.setMaxResults(1).uniqueResult();
			ConstantValues.s2 = (Jenjang) session.createCriteria(Jenjang.class)
					.add(Restrictions.or(Restrictions.ilike("nama", "Strata Dua (S2)"),
							Restrictions.or(Restrictions.ilike("nama", "S2"), Restrictions.ilike("nama", "Strata 2"))))
					.setMaxResults(1).uniqueResult();
			ConstantValues.s3 = (Jenjang) session.createCriteria(Jenjang.class)
					.add(Restrictions.or(Restrictions.ilike("nama", "Strata Tiga (S3)"),
							Restrictions.or(Restrictions.ilike("nama", "S3"), Restrictions.ilike("nama", "Strata 3"))))
					.setMaxResults(1).uniqueResult();
			ConstantValues.sekolahS1 = (JenisSekolahMahasiswaBaru) session
					.createCriteria(JenisSekolahMahasiswaBaru.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.idEq(8L)).uniqueResult();
			ConstantValues.sekolahS2 = (JenisSekolahMahasiswaBaru) session
					.createCriteria(JenisSekolahMahasiswaBaru.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.idEq(9L)).uniqueResult();
			ConstantValues.s1SemuaJurusan = (JurusanSekolahMahasiswaBaru) session
					.createCriteria(JurusanSekolahMahasiswaBaru.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.idEq(28L)).uniqueResult();
			ConstantValues.s2SemuaJurusan = (JurusanSekolahMahasiswaBaru) session
					.createCriteria(JurusanSekolahMahasiswaBaru.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.idEq(29L)).uniqueResult();
			ConstantValues.paketA = (Paket) session.createCriteria(Paket.class).add(Restrictions.idEq(1L))
					.uniqueResult();
			ConstantValues.paketD = (Paket) session.createCriteria(Paket.class).add(Restrictions.idEq(7L))
					.uniqueResult();
			ConstantValues.paketE = (Paket) session.createCriteria(Paket.class).add(Restrictions.idEq(8L))
					.uniqueResult();

			ConstantValues.s2T = (Jenjang) session.createCriteria(Jenjang.class).setMaxResults(1)
					.add(Restrictions.eq("nama", "S-2T")).uniqueResult();
			if (ConstantValues.s2T == null) {
				ConstantValues.s2T = new Jenjang();
				ConstantValues.s2T.setAktif(false);
				ConstantValues.s2T.setNama("S-2T");
				ConstantValues.s2T.setFeeder(4L);

				session.getTransaction().begin();
				session.save(ConstantValues.s2T);
				session.getTransaction().commit();
			}

			ConstantValues.s3T = (Jenjang) session.createCriteria(Jenjang.class).setMaxResults(1)
					.add(Restrictions.eq("nama", "S-3T")).uniqueResult();
			if (ConstantValues.s3T == null) {
				ConstantValues.s3T = new Jenjang();
				ConstantValues.s3T.setAktif(false);
				ConstantValues.s3T.setNama("S-3T");
				ConstantValues.s3T.setFeeder(6L);

				session.getTransaction().begin();
				session.save(ConstantValues.s3T);
				session.getTransaction().commit();

			}

			ConstantValues.PENELITIAN = (TipePenelitianDanPengabdian) session
					.createCriteria(TipePenelitianDanPengabdian.class).add(Restrictions.eq("kode", "001.000"))
					.setMaxResults(1).uniqueResult();
			if (ConstantValues.PENELITIAN == null) {
				ConstantValues.PENELITIAN = new TipePenelitianDanPengabdian();
				ConstantValues.PENELITIAN.setKode("001.000");
				ConstantValues.PENELITIAN.setIsi("Penelitian Ilmiah");

				session.getTransaction().begin();
				session.save(ConstantValues.PENELITIAN);
				session.getTransaction().commit();

			}

			ConstantValues.PENGABDIAN = (TipePenelitianDanPengabdian) session
					.createCriteria(TipePenelitianDanPengabdian.class).add(Restrictions.eq("kode", "002.000"))
					.setMaxResults(1).uniqueResult();
			if (ConstantValues.PENGABDIAN == null) {
				ConstantValues.PENGABDIAN = new TipePenelitianDanPengabdian();
				ConstantValues.PENGABDIAN.setKode("002.000");
				ConstantValues.PENGABDIAN.setIsi("Pengabdian Masyarakat");

				session.getTransaction().begin();
				session.save(ConstantValues.PENGABDIAN);
				session.getTransaction().commit();
			}

			ConstantValues.PENELITIAN_LAINNYA = (TipePenelitianDanPengabdian) session
					.createCriteria(TipePenelitianDanPengabdian.class).add(Restrictions.eq("kode", "003.000"))
					.setMaxResults(1).uniqueResult();
			if (ConstantValues.PENELITIAN_LAINNYA == null) {
				ConstantValues.PENELITIAN_LAINNYA = new TipePenelitianDanPengabdian();
				ConstantValues.PENELITIAN_LAINNYA.setKode("003.000");
				ConstantValues.PENELITIAN_LAINNYA.setIsi("Lainnya");

				session.getTransaction().begin();
				session.save(ConstantValues.PENELITIAN_LAINNYA);
				session.getTransaction().commit();
			}

			ConstantValues.roleDosen = (Tbmrole) session.createCriteria(Tbmrole.class)
					.add(Restrictions.idEq(Tbmrole.DOSEN)).uniqueResult();

			ConstantValues.rolePeserta = (Tbmrole) session.createCriteria(Tbmrole.class)
					.add(Restrictions.idEq("peserta")).uniqueResult();

			if (ConstantValues.rolePeserta == null) {
				ConstantValues.rolePeserta = new Tbmrole();
				ConstantValues.rolePeserta.setRoleId("peserta");
				ConstantValues.rolePeserta.setRoleName("Peserta");

				session.getTransaction().begin();
				session.save(ConstantValues.rolePeserta);
				session.getTransaction().commit();
			}

			ConstantValues.roleAdminFakultas = (Tbmrole) session.createCriteria(Tbmrole.class)
					.add(Restrictions.idEq("admfak")).setMaxResults(1).uniqueResult();
			ConstantValues.roleAdminJurusan = (Tbmrole) session.createCriteria(Tbmrole.class).setMaxResults(1)
					.add(Restrictions.idEq("admprd")).uniqueResult();

			ConstantValues.Akademik = (Tbmrole) session.createCriteria(Tbmrole.class).setMaxResults(1)
					.add(Restrictions.idEq("Akademik")).uniqueResult();

			ConstantValues.rolePegawai = (Tbmrole) session.createCriteria(Tbmrole.class)
					.add(Restrictions.idEq(Tbmrole.PEGAWAI)).uniqueResult();

			if (ConstantValues.rolePegawai == null) {
				ConstantValues.rolePegawai = new Tbmrole();
				ConstantValues.rolePegawai.setRoleId(Tbmrole.PEGAWAI);
				ConstantValues.rolePegawai.setRoleName("Pegawai");

				session.getTransaction().begin();
				session.save(ConstantValues.rolePegawai);
				session.getTransaction().commit();
			}

			if (ConstantValues.roleDosen == null) {
				ConstantValues.roleDosen = new Tbmrole();
				ConstantValues.roleDosen.setRoleId(Tbmrole.DOSEN);
				ConstantValues.roleDosen.setRoleName("Dosen");
				session.getTransaction().begin();
				session.save(ConstantValues.roleDosen);
				session.getTransaction().commit();
			}

			ConstantValues.roleKomunitas = (Tbmrole) session.createCriteria(Tbmrole.class)
					.add(Restrictions.idEq(Tbmrole.KOMUNITAS)).uniqueResult();

			if (ConstantValues.roleKomunitas == null) {
				ConstantValues.roleKomunitas = new Tbmrole();
				ConstantValues.roleKomunitas.setRoleId(Tbmrole.KOMUNITAS);
				ConstantValues.roleKomunitas.setRoleName(Tbmrole.KOMUNITAS);
				session.getTransaction().begin();
				session.save(ConstantValues.roleKomunitas);
				session.getTransaction().commit();
			}

			ConstantValues.roleAnggotaPerpustakaan = (Tbmrole) session.createCriteria(Tbmrole.class)
					.add(Restrictions.idEq(Tbmrole.ANGGOTA_PERPUSTAKAAN)).uniqueResult();

			if (ConstantValues.roleAnggotaPerpustakaan == null) {
				ConstantValues.roleAnggotaPerpustakaan = new Tbmrole();
				ConstantValues.roleAnggotaPerpustakaan.setRoleId(Tbmrole.ANGGOTA_PERPUSTAKAAN);
				ConstantValues.roleAnggotaPerpustakaan.setRoleName("Anggota Perpustakaan");
				session.getTransaction().begin();
				session.save(ConstantValues.roleAnggotaPerpustakaan);
				session.getTransaction().commit();
			}

			ConstantValues.roleAnggotaKoperasi = (Tbmrole) session.createCriteria(Tbmrole.class)
					.add(Restrictions.idEq(Tbmrole.ANGGOTA_KOPERASI)).uniqueResult();

			if (ConstantValues.roleAnggotaKoperasi == null) {
				ConstantValues.roleAnggotaKoperasi = new Tbmrole();
				ConstantValues.roleAnggotaKoperasi.setRoleId(Tbmrole.ANGGOTA_KOPERASI);
				ConstantValues.roleAnggotaKoperasi.setRoleName("Anggota Koperasi");
				session.getTransaction().begin();
				session.save(ConstantValues.roleAnggotaKoperasi);
				session.getTransaction().commit();
			}

			ConstantValues.rolePesertaKursusPerpustakaan = (Tbmrole) session.createCriteria(Tbmrole.class)
					.add(Restrictions.idEq(Tbmrole.PESERTA_KURSUS)).uniqueResult();

			if (ConstantValues.rolePesertaKursusPerpustakaan == null) {
				ConstantValues.rolePesertaKursusPerpustakaan = new Tbmrole();
				ConstantValues.rolePesertaKursusPerpustakaan.setRoleId(Tbmrole.PESERTA_KURSUS);
				ConstantValues.rolePesertaKursusPerpustakaan.setRoleName("Peserta Kursus");
				session.getTransaction().begin();
				session.save(ConstantValues.rolePesertaKursusPerpustakaan);
				session.getTransaction().commit();
			}

			ConstantValues.DENDA = (ItemBiaya) session.createCriteria(ItemBiaya.class)
					.add(Restrictions.eq("nama", "Denda")).add(Restrictions.eq("nama", "Denda")).setMaxResults(1)
					.uniqueResult();
			if (ConstantValues.DENDA == null) {
				ItemBiaya itemBiaya = new ItemBiaya();
				itemBiaya.setDeskripsi("Denda keterlambatan pembayaran");
				itemBiaya.setKode("1000");
				itemBiaya.setNama("Denda");
				session.getTransaction().begin();
				session.save(itemBiaya);
				session.getTransaction().commit();
				ConstantValues.DENDA = itemBiaya;
			}
			ConstantValues.SPP = (ItemBiaya) session.createCriteria(ItemBiaya.class).add(Restrictions.eq("nama", "SPP"))
					.setMaxResults(1).uniqueResult();

			ConstantValues.CUTI_ITEM_BIAYA = (ItemBiaya) session.createCriteria(ItemBiaya.class)
					.add(Restrictions.eq("nama", "Cuti")).setMaxResults(1).uniqueResult();

			ConstantValues.BARU = (StatusAwalMahasiswa) session.createCriteria(StatusAwalMahasiswa.class)
					.add(Restrictions.or(Restrictions.ilike("nama", "Baru"),
							Restrictions.ilike("nama", "Peserta didik baru")))
					.setMaxResults(1).uniqueResult();
			if (ConstantValues.BARU == null) {
				ConstantValues.BARU = (StatusAwalMahasiswa) session.createCriteria(StatusAwalMahasiswa.class)
						.add(Restrictions.ilike("nama", "Baru", MatchMode.START)).addOrder(Order.asc("nama"))
						.setMaxResults(1).uniqueResult();
			}

			ConstantValues.BARU_SISWA = (StatusAwalSiswa) session.createCriteria(StatusAwalSiswa.class).add(Restrictions
					.or(Restrictions.ilike("nama", "Baru"), Restrictions.ilike("nama", "Peserta didik baru")))
					.setMaxResults(1).uniqueResult();
			if (ConstantValues.BARU_SISWA == null) {
				ConstantValues.BARU_SISWA = new StatusAwalSiswa();
				ConstantValues.BARU_SISWA.setKode("");
				ConstantValues.BARU_SISWA.setNama("Peserta didik baru");

				session.getTransaction().begin();
				session.save(ConstantValues.BARU_SISWA);
				session.getTransaction().commit();
			}

			ConstantValues.BARU_BEASISWA = (StatusAwalMahasiswa) session
					.createCriteria(StatusAwalMahasiswa.class).add(Restrictions
							.or(Restrictions.ilike("nama", "Baru-Beasiswa"), Restrictions.ilike("nama", "Beasiswa")))
					.setMaxResults(1).uniqueResult();

			ConstantValues.BARU_SISWA_BEASISWA = (StatusAwalSiswa) session
					.createCriteria(StatusAwalSiswa.class).add(Restrictions
							.or(Restrictions.ilike("nama", "Baru-Beasiswa"), Restrictions.ilike("nama", "Beasiswa")))
					.setMaxResults(1).uniqueResult();
			if (ConstantValues.BARU_SISWA_BEASISWA == null) {
				ConstantValues.BARU_SISWA_BEASISWA = new StatusAwalSiswa();
				ConstantValues.BARU_SISWA_BEASISWA.setKode("");
				ConstantValues.BARU_SISWA_BEASISWA.setNama("Beasiswa");

				session.getTransaction().begin();
				session.save(ConstantValues.BARU_SISWA_BEASISWA);
				session.getTransaction().commit();
			}

			ConstantValues.PINDAHAN = (StatusAwalMahasiswa) session.createCriteria(StatusAwalMahasiswa.class)
					.add(Restrictions.ilike("nama", "Pindahan", MatchMode.EXACT)).setMaxResults(1).uniqueResult();

			if (ConstantValues.PINDAHAN == null) {
				ConstantValues.PINDAHAN = new StatusAwalMahasiswa();
				ConstantValues.PINDAHAN.setKode("");
				ConstantValues.PINDAHAN.setNama("Pindahan");

				session.getTransaction().begin();
				session.save(ConstantValues.PINDAHAN);
				session.getTransaction().commit();
			}

			ConstantValues.PINDAHAN_SISWA = (StatusAwalSiswa) session.createCriteria(StatusAwalSiswa.class)
					.add(Restrictions.ilike("nama", "Pindahan", MatchMode.EXACT)).setMaxResults(1).uniqueResult();

			if (ConstantValues.PINDAHAN_SISWA == null) {
				ConstantValues.PINDAHAN_SISWA = new StatusAwalSiswa();
				ConstantValues.PINDAHAN_SISWA.setKode("");
				ConstantValues.PINDAHAN_SISWA.setNama("Pindahan");

				session.getTransaction().begin();
				session.save(ConstantValues.PINDAHAN_SISWA);
				session.getTransaction().commit();
			}

			if (ConstantValues.BARU_BEASISWA == null) {
				ConstantValues.BARU_BEASISWA = new StatusAwalMahasiswa();
				ConstantValues.BARU_BEASISWA.setKode("1");
				ConstantValues.BARU_BEASISWA.setNama("Baru-Beasiswa");

				session.getTransaction().begin();
				session.save(ConstantValues.BARU_BEASISWA);
				session.getTransaction().commit();
			}

			ConstantValues.ALIH_PRODI = (StatusAwalMahasiswa) session.createCriteria(StatusAwalMahasiswa.class)
					.add(Restrictions.ilike("nama", "Alih Prodi", MatchMode.EXACT)).setMaxResults(1).uniqueResult();
			if (ConstantValues.ALIH_PRODI == null) {
				ConstantValues.ALIH_PRODI = new StatusAwalMahasiswa();
				ConstantValues.ALIH_PRODI.setKode("");
				ConstantValues.ALIH_PRODI.setNama("Alih Prodi");

				session.getTransaction().begin();
				session.save(ConstantValues.ALIH_PRODI);
				session.getTransaction().commit();
			}

			ConstantValues.INDONESIA = (Negara) session.createCriteria(Negara.class)
					.add(Restrictions.ilike("namaNegara", "Indonesia", MatchMode.EXACT)).setMaxResults(1)
					.uniqueResult();

			if (ConstantValues.INDONESIA == null) {
				ConstantValues.INDONESIA = new Negara();
				ConstantValues.INDONESIA.setNamaNegara("Indonesia");
				session.getTransaction().begin();
				session.save(ConstantValues.INDONESIA);
				session.getTransaction().commit();
			}

			// HibernateUtil.closeSession();

			// new Thread(runnable).start();

//		checkStaff("Kepala Biro Administrasi Akademik dan Kemahasiswaan", "Drs. H. Abd. Shomad, MA",
//				"150231353");

			try {
				Common.reloadRencanaTahunAkademik(session);
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}

			try {

				String data = "1;Bersama orang tua|" + "2;Wali|" + "3;Kost|" + "4;Asrama|" + "5;Panti asuhan|"
						+ "99;Lainnya|";

				for (String subData : StringUtils.split(data, "|")) {
					if (!subData.trim().isEmpty()) {
						Long feederId = Long.parseLong(splitInitData(subData)[0]);
						String nama = splitInitData(subData)[1];
						JenisTinggalMahasiswa jenisTinggalMahasiswa = (JenisTinggalMahasiswa) session
								.createCriteria(JenisTinggalMahasiswa.class).add(Restrictions.eq("feeder", feederId))
								.setMaxResults(1).uniqueResult();
						if (jenisTinggalMahasiswa == null) {
							jenisTinggalMahasiswa = new JenisTinggalMahasiswa();
							jenisTinggalMahasiswa.setFeeder(feederId);
							jenisTinggalMahasiswa.setNama(nama);

							session.getTransaction().begin();
							session.save(jenisTinggalMahasiswa);
							session.getTransaction().commit();

						}
					}
				}

			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}

			try {

				String data = "1;Mandiri|" + "2;Beasiswa Tidak Penuh|" + "3;Beasiswa Penuh|" + "4;Bidikmisi|"
						+ "4;Kartu Indonesia Pintar|";

				for (String subData : StringUtils.split(data, "|")) {
					if (!subData.trim().isEmpty()) {
						Long feederId = Long.parseLong(splitInitData(subData)[0]);
						String nama = splitInitData(subData)[1];
						JenisPembiayaanMahasiswa jenisPembiayaanMahasiswa = (JenisPembiayaanMahasiswa) session
								.createCriteria(JenisPembiayaanMahasiswa.class).add(Restrictions.eq("feeder", feederId))
								.add(Restrictions.eq("nama", nama)).setMaxResults(1).uniqueResult();
						if (jenisPembiayaanMahasiswa == null) {
							jenisPembiayaanMahasiswa = new JenisPembiayaanMahasiswa();
							jenisPembiayaanMahasiswa.setFeeder(feederId);
							jenisPembiayaanMahasiswa.setNama(nama);

							session.getTransaction().begin();
							session.save(jenisPembiayaanMahasiswa);
							session.getTransaction().commit();
						}

						if (feederId.equals(1L)) {
							ConstantValues.MANDIRI = jenisPembiayaanMahasiswa;
						}
					}
				}

			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}

			try {

				String data = "1;Jalan kaki|" + "2;Kendaraan pribadi|" + "3;Angkutan umum/bus/pete-pete|"
						+ "4;Mobil/bus antar jemput|" + "5;Kereta api|" + "6;Ojek|"
						+ "7;Andong/bendi/sado/dokar/delman/becak|" + "8;Perahu penyeberangan/rakit/getek|" + "11;Kuda|"
						+ "12;Sepeda|" + "13;Sepeda motor|" + "14;Mobil pribadi|" + "99;Lainnya|";

				for (String subData : StringUtils.split(data, "|")) {
					if (!subData.trim().isEmpty()) {
						Long feederId = Long.parseLong(splitInitData(subData)[0]);
						String nama = splitInitData(subData)[1];
						AlatTransportasiMahasiswa alatTransportasiMahasiswa = (AlatTransportasiMahasiswa) session
								.createCriteria(AlatTransportasiMahasiswa.class)
								.add(Restrictions.eq("feeder", feederId)).setMaxResults(1).uniqueResult();
						if (alatTransportasiMahasiswa == null) {
							alatTransportasiMahasiswa = new AlatTransportasiMahasiswa();
							alatTransportasiMahasiswa.setFeeder(feederId);
							alatTransportasiMahasiswa.setNama(nama);

							session.getTransaction().begin();
							session.save(alatTransportasiMahasiswa);
							session.getTransaction().commit();
						}
					}
				}

			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}

			try {

				String data = "1;Tidak bekerja|" + "2;Nelayan|" + "3;Petani|" + "4;Peternak|" + "5;PNS/TNI/Polri|"
						+ "6;Karyawan Swasta|" + "7;Pedagang Kecil|" + "8;Pedagang Besar|" + "9;Wiraswasta|"
						+ "10;Wirausaha|" + "11;Buruh|" + "12;Pensiunan|" + "98;Sudah Meninggal|" + "99;Lainnya";

				for (String subData : StringUtils.split(data, "|")) {
					if (!subData.trim().isEmpty()) {
						Long feederId = Long.parseLong(splitInitData(subData)[0]);
						String nama = splitInitData(subData)[1];
						Pekerjaan pekerjaan = (Pekerjaan) session.createCriteria(Pekerjaan.class)
								.add(Restrictions.eq("feeder", feederId)).setMaxResults(1).uniqueResult();
						if (pekerjaan == null) {
							pekerjaan = new Pekerjaan();
							pekerjaan.setFeeder(feederId);
							pekerjaan.setNama(nama);

							session.getTransaction().begin();
							session.save(pekerjaan);
							session.getTransaction().commit();
						}
					}
				}

			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}

			try {

				String data = "11;Kurang dari Rp. 500,000;499999;0|" + "12;Rp. 500,000 - Rp. 999,999;999999;500000|"
						+ "13;Rp. 1,000,000 - Rp. 1,999,999;1999999;1000000|"
						+ "14;Rp. 2,000,000 - Rp. 4,999,999;4999999;2000000|"
						+ "15;Rp. 5,000,000 - Rp. 20,000,000;20000000;5000000|"
						+ "16;Lebih dari Rp. 20,000,000;0;20000001|";

				for (String subData : StringUtils.split(data, "|")) {
					if (!subData.trim().isEmpty()) {
						String[] a = splitInitData(subData);
						Long feederId = Long.parseLong(a[0]);
						String nama = a[1];
						Double mulai = Double.parseDouble(a[2]);
						Double sampai = Double.parseDouble(a[3]);
						Penghasilan penghasilan = (Penghasilan) session.createCriteria(Penghasilan.class)
								.add(Restrictions.eq("feeder", feederId)).setMaxResults(1).uniqueResult();
						if (penghasilan == null) {
							penghasilan = new Penghasilan();
							penghasilan.setFeeder(feederId);
							penghasilan.setNama(nama);
							penghasilan.setBatasAtas(mulai);
							penghasilan.setBatasBawah(sampai);

							session.getTransaction().begin();
							session.save(penghasilan);
							session.getTransaction().commit();
						}
					}
				}

			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}

			try {

				String data = "1;Lulus|" + "2;Mutasi|" + "3;Dikeluarkan|" + "4;Mengundurkan diri|" + "5;Putus Sekolah|"
						+ "6;Wafat|" + "7;Hilang|" + "8;Alih Fungsi|" + "9;Pensiun|" + "Z;Lainnya";

				for (String subData : StringUtils.split(data, "|")) {
					if (!subData.trim().isEmpty()) {
						String feederId = (splitInitData(subData)[0]);
						String nama = splitInitData(subData)[1];
						StatusKeluar statusKeluar = (StatusKeluar) session.createCriteria(StatusKeluar.class)
								.add(Restrictions.eq("feeder", feederId)).setMaxResults(1).uniqueResult();
						if (statusKeluar == null) {
							statusKeluar = new StatusKeluar();
							statusKeluar.setFeeder(feederId);
							statusKeluar.setNama(nama);

							session.getTransaction().begin();
							session.save(statusKeluar);
							session.getTransaction().commit();
						}
					}
				}

			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}

			try {
				String data = "0;Tidak sekolah|" + "1;PAUD|" + "2;TK / sederajat|" + "3;Putus SD|" + "4;SD / sederajat|"
						+ "5;SMP / sederajat|" + "6;SMA / sederajat|" + "7;Paket A|" + "8;Paket B|" + "9;Paket C|"
						+ "20;D1|" + "21;D2|" + "22;D3|" + "23;D4|" + "25;Profesi|" + "30;S1|" + "32;Sp-1|" + "35;S2|"
						+ "37;Sp-2|" + "40;S3|" + "90;Non formal|" + "91;Informal|" + "99;Lainnya";

				for (String subData : StringUtils.split(data, "|")) {
					if (!subData.trim().isEmpty()) {
						String[] values = splitInitData(subData);
						if (values.length < 2 || values[0].trim().length() == 0) {
							continue;
						}

						Long feederId = Long.valueOf(values[0].trim());
						String nama = values[1];

						/*
						 * Jangan gunakan saveOrUpdate() untuk master Jenjang existing.
						 *
						 * Pada beberapa instalasi lama, Jenjang yang sudah pernah tersentuh proses
						 * cache/startup dapat membawa PersistentCollection dari session lain. Ketika
						 * dipaksa saveOrUpdate(), Hibernate 3.6 bisa gagal dengan:
						 * "Illegal attempt to associate a collection with two open sessions".
						 *
						 * Solusi aman: - cari ID saja agar object graph/collection tidak ikut
						 * di-attach, - existing data di-update memakai HQL bulk by ID, - data baru
						 * tetap disimpan sebagai entity baru.
						 */
						Long jenjangId = (Long) session.createCriteria(Jenjang.class)
								.setProjection(Projections.property("id")).add(Restrictions
										.or(Restrictions.like("nama", nama), Restrictions.eq("feeder", feederId)))
								.setMaxResults(1).uniqueResult();

						if (jenjangId == null) {
							beginTransactionIfNeeded(session);
							Jenjang jenjang = new Jenjang();
							jenjang.setNama(nama);
							jenjang.setFeeder(feederId);
							session.save(jenjang);
							commitTransactionIfActive(session);
						} else {
							/*
							 * Jangan jalankan bulk HQL pada session initMaster utama. Pada Hibernate 3.6,
							 * executeUpdate() memicu autoFlush terhadap semua entity yang masih managed di
							 * session tersebut. Jika salah satu entity menyimpan collection yang sudah
							 * tersentuh session lain, akan muncul: Illegal attempt to associate a
							 * collection with two open sessions.
							 */
							java.util.Map parameters = new java.util.HashMap();
							parameters.put("feeder", feederId);
							parameters.put("id", jenjangId);
							executeHqlUpdateIsolated("update Jenjang set feeder = :feeder where id = :id", parameters);
						}
					}
				}
			} catch (Exception e) {
				rollbackActiveTransaction(session);
				Common.tampilErrorJikaAdmin(e);
			}

			try {
				String data = "A;DOSEN TETAP;-|" + "B;DOSEN PNS DPK;-|" + "C; ; |" + "D;DOSEN HONORER;-|"
						+ "E;DOSEN SP RUMAH SAKIT;-|" + "F;DOSEN TETAP BHMN;-|" + "G;DOSEN TIDAK TETAP;-|" + "P; ; |"
						+ "X; ;LAINNYA|";

				for (String subData : StringUtils.split(data, "|")) {
					if (!subData.trim().isEmpty()) {
						String feederId = splitInitData(subData)[0];
						String nama = splitInitData(subData)[1];
						String keterangan = splitInitData(subData)[2];
						IkatanKerjaDosen ikatanKerjaDosen = (IkatanKerjaDosen) session
								.createCriteria(IkatanKerjaDosen.class).add(Restrictions.eq("feeder", feederId))
								.setMaxResults(1).uniqueResult();
						if (ikatanKerjaDosen == null) {
							ikatanKerjaDosen = new IkatanKerjaDosen();
							ikatanKerjaDosen.setNama(nama);
							ikatanKerjaDosen.setKeterangan(keterangan);
							ikatanKerjaDosen.setFeeder(feederId);

							session.getTransaction().begin();
							session.save(ikatanKerjaDosen);
							session.getTransaction().commit();
						}

						if (nama.trim().equalsIgnoreCase("DOSEN TETAP")) {
							ConstantValues.DOSEN_TETAP = ikatanKerjaDosen;
						} else if (nama.trim().equalsIgnoreCase("DOSEN HONORER")) {
							ConstantValues.DOSEN_HONORER = ikatanKerjaDosen;
						}
					}
				}
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}

			try {
				String data = "1;PNS|" + "2;PNS Diperbantukan|" + "3;PNS Depag|" + "4;GTY/PTY|" + "5;GTT/PTT Provinsi|"
						+ "6;GTT/PTT Kab/Kota|" + "7;Guru Bantu Pusat|" + "8;Guru Honor Sekolah|"
						+ "9;Tenaga Honor Sekolah|" + "10;NON PNS|" + "11;TNI|" + "99;Lainnya";

				for (String subData : StringUtils.split(data, "|")) {
					if (!subData.trim().isEmpty()) {
						String feederId = splitInitData(subData)[0];
						String nama = splitInitData(subData)[1];
						StatusKepegawaian statusKepegawaian = (StatusKepegawaian) session
								.createCriteria(StatusKepegawaian.class).add(Restrictions.eq("feeder", feederId))
								.setMaxResults(1).uniqueResult();
						if (statusKepegawaian == null) {
							statusKepegawaian = new StatusKepegawaian();
							statusKepegawaian.setNama(nama);
							statusKepegawaian.setFeeder(feederId);

							session.getTransaction().begin();
							session.save(statusKepegawaian);
							session.getTransaction().commit();
						}
					}
				}
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}

			try {
				String data = "0;(tidak diisi)|" + "1;Pemerintah Pusat|" + "2;Pemerintah Propinsi|"
						+ "3;Pemerintah Kab/Kota|" + "4;Ketua Yayasan|" + "5;Kepala Sekolah|" + "6;Komite Sekolah|"
						+ "7;Lainnya|" + "20;Kemenag|" + "40;Kemenkes|" + "99;Lainnya";

				for (String subData : StringUtils.split(data, "|")) {
					if (!subData.trim().isEmpty()) {
						String feederId = splitInitData(subData)[0];
						String nama = splitInitData(subData)[1];
						LembagaPengangkat lembagaPengangkat = (LembagaPengangkat) session
								.createCriteria(LembagaPengangkat.class).add(Restrictions.eq("feeder", feederId))
								.setMaxResults(1).uniqueResult();
						if (lembagaPengangkat == null) {
							lembagaPengangkat = new LembagaPengangkat();
							lembagaPengangkat.setNama(nama);
							lembagaPengangkat.setFeeder(feederId);

							session.getTransaction().begin();
							session.save(lembagaPengangkat);
							session.getTransaction().commit();
						}
					}
				}
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}

			try {
				String data = "3;Guru Kelas|" + "4;Guru Mapel|" + "5;Guru BK|" + "6;Guru Inklusi|"
						+ "7;Pengawas Satuan Pendidikan|" + "8;Pengawas PLB|" + "9;Pengawas Metpel|"
						+ "10;Pengawas Bidang|" + "11;Tenaga Administrasi Sekolah|" + "99;Lainnya";

				for (String subData : StringUtils.split(data, "|")) {
					if (!subData.trim().isEmpty()) {
						String feederId = splitInitData(subData)[0];
						String nama = splitInitData(subData)[1];
						JenisPendidikDanTenagaKependidikan jenisPendidikDanTenagaKependidikan = (JenisPendidikDanTenagaKependidikan) session
								.createCriteria(JenisPendidikDanTenagaKependidikan.class)
								.add(Restrictions.eq("feeder", feederId)).setMaxResults(1).uniqueResult();
						if (jenisPendidikDanTenagaKependidikan == null) {
							jenisPendidikDanTenagaKependidikan = new JenisPendidikDanTenagaKependidikan();
							jenisPendidikDanTenagaKependidikan.setNama(nama);
							jenisPendidikDanTenagaKependidikan.setFeeder(feederId);

							session.getTransaction().begin();
							session.save(jenisPendidikDanTenagaKependidikan);
							session.getTransaction().commit();
						}
					}
				}
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}

			try {
				String data = "1;APBN|" + "2;APBD Provinsi|" + "3;APBD Kabupaten/Kota|" + "4;Yayasan|" + "5;Sekolah|"
						+ "6;Lembaga Donor|" + "99;Lainnya";

				for (String subData : StringUtils.split(data, "|")) {
					if (!subData.trim().isEmpty()) {
						String feederId = splitInitData(subData)[0];
						String nama = splitInitData(subData)[1];
						SumberGaji sumberGaji = (SumberGaji) session.createCriteria(SumberGaji.class)
								.add(Restrictions.eq("feeder", feederId)).setMaxResults(1).uniqueResult();
						if (sumberGaji == null) {
							sumberGaji = new SumberGaji();
							sumberGaji.setNama(nama);
							sumberGaji.setFeeder(feederId);

							session.getTransaction().begin();
							session.save(sumberGaji);
							session.getTransaction().commit();
						}
					}
				}
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}

			try {
				String data = "0;Tenaga Pengajar/Calon Dosen|1;Asisten Ahli|2;Lektor|3;Lektor Kepala|4;Profesor";

				for (String subData : StringUtils.split(data, "|")) {
					if (!subData.trim().isEmpty()) {
						String feederId = splitInitData(subData)[0];
						String nama = splitInitData(subData)[1];
						JabatanFungsionalDosen jabatanFungsionalDosen = (JabatanFungsionalDosen) session
								.createCriteria(JabatanFungsionalDosen.class).add(Restrictions.eq("feeder", feederId))
								.setMaxResults(1).uniqueResult();
						if (jabatanFungsionalDosen == null) {
							jabatanFungsionalDosen = new JabatanFungsionalDosen();
							jabatanFungsionalDosen.setNama(nama);
							jabatanFungsionalDosen.setFeeder(feederId);

							session.getTransaction().begin();
							session.save(jabatanFungsionalDosen);
							session.getTransaction().commit();
						}
					}
				}
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}

			try {
				String data = "1;Aktif|" + "2;Tidak Aktif|" + "20;CUTI|" + "21;KELUAR|" + "22;ALMARHUM|" + "23;PENSIUN|"
						+ "24;IJIN BELAJAR|" + "25;TUGAS DI INSTANSI LAIN|" + "26;GANTI NIDN|" + "27;TUGAS BELAJAR|"
						+ "28;HAPUS NIDN|" + "99;Lainnya";

				for (String subData : StringUtils.split(data, "|")) {
					if (!subData.trim().isEmpty()) {
						String feederId = splitInitData(subData)[0].trim();
						String nama = splitInitData(subData)[1].trim();
						StatusPegawai statusPegawai = (StatusPegawai) session.createCriteria(StatusPegawai.class)
								.add(Restrictions.eq("feeder", feederId)).setMaxResults(1).uniqueResult();
						if (statusPegawai == null) {
							statusPegawai = (StatusPegawai) session.createCriteria(StatusPegawai.class)
									.add(Restrictions.ilike("nama", nama)).setMaxResults(1).uniqueResult();
						}

						if (statusPegawai == null && nama.equalsIgnoreCase("ALMARHUM")) {
							statusPegawai = (StatusPegawai) session.createCriteria(StatusPegawai.class)
									.add(Restrictions.ilike("nama", "Meninggal")).setMaxResults(1).uniqueResult();
						}

						if (statusPegawai == null) {
							statusPegawai = new StatusPegawai();
							statusPegawai.setNama(nama);
							statusPegawai.setFeeder(feederId);

							session.getTransaction().begin();
							session.save(statusPegawai);
							session.getTransaction().commit();
						}
					}
				}
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}

			ConstantValues.selesaiInit = true;
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/InitDataHelper.java:2356");
		}

		try {
			Common.reloadJenisKegiatans(session);
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/InitDataHelper.java:2362");
		}

		try {

			List<Konfigurasi> konfigurasis = session.createCriteria(Konfigurasi.class).addOrder(Order.desc("id"))
					.add(Restrictions.isNotNull("nama")).list();
			System.out.println("loading data " + Konfigurasi.class.getName() + " sebanyak " + konfigurasis.size());
			for (Konfigurasi konfigurasi : konfigurasis) {
				if (konfigurasi.getNama() != null) {
					MemoryDbUtil.getKonfigurasi().put(konfigurasi.getNama(), konfigurasi);

					try {
						InitDataHelper.reInitDataBaru(konfigurasi);
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/InitDataHelper.java:2377");
					}

				}
			}
			konfigurasis = null;

			String d = "{\"data\":[{\"id_jns_akt_mhs\":\"1\",\"a_kegiatan_kampus_merdeka\":\"0\",\"ket_jns_akt_mhs\":\"\",\"nm_jns_akt_mhs\":\"Laporan akhir studi\"},{\"id_jns_akt_mhs\":\"2\",\"a_kegiatan_kampus_merdeka\":\"0\",\"ket_jns_akt_mhs\":\"\",\"nm_jns_akt_mhs\":\"Tugas akhir\"},{\"id_jns_akt_mhs\":\"3\",\"a_kegiatan_kampus_merdeka\":\"0\",\"ket_jns_akt_mhs\":\"\",\"nm_jns_akt_mhs\":\"Tesis\"},{\"id_jns_akt_mhs\":\"4\",\"a_kegiatan_kampus_merdeka\":\"0\",\"ket_jns_akt_mhs\":\"\",\"nm_jns_akt_mhs\":\"Disertasi\"},{\"id_jns_akt_mhs\":\"5\",\"a_kegiatan_kampus_merdeka\":\"0\",\"ket_jns_akt_mhs\":\"\",\"nm_jns_akt_mhs\":\"Kuliah kerja nyata\"},{\"id_jns_akt_mhs\":\"6\",\"a_kegiatan_kampus_merdeka\":\"0\",\"ket_jns_akt_mhs\":\"\",\"nm_jns_akt_mhs\":\"Kerja praktek\\/PKL\"},{\"id_jns_akt_mhs\":\"7\",\"a_kegiatan_kampus_merdeka\":\"0\",\"ket_jns_akt_mhs\":\"\",\"nm_jns_akt_mhs\":\"Bimbingan akademis\"},{\"id_jns_akt_mhs\":\"10\",\"a_kegiatan_kampus_merdeka\":\"0\",\"ket_jns_akt_mhs\":\"\",\"nm_jns_akt_mhs\":\"Aktivitas kemahasiswaan\"},{\"id_jns_akt_mhs\":\"11\",\"a_kegiatan_kampus_merdeka\":\"0\",\"ket_jns_akt_mhs\":\"\",\"nm_jns_akt_mhs\":\"Program kreativitas mahasiswa\"},{\"id_jns_akt_mhs\":\"12\",\"a_kegiatan_kampus_merdeka\":\"0\",\"ket_jns_akt_mhs\":\"\",\"nm_jns_akt_mhs\":\"Kompetisi\"},{\"id_jns_akt_mhs\":\"13\",\"a_kegiatan_kampus_merdeka\":\"1\",\"ket_jns_akt_mhs\":\"\",\"nm_jns_akt_mhs\":\"Magang\\/Praktik Kerja\"},{\"id_jns_akt_mhs\":\"14\",\"a_kegiatan_kampus_merdeka\":\"1\",\"ket_jns_akt_mhs\":\"\",\"nm_jns_akt_mhs\":\"Asistensi Mengajar di Satuan Pendidikan\"},{\"id_jns_akt_mhs\":\"15\",\"a_kegiatan_kampus_merdeka\":\"1\",\"ket_jns_akt_mhs\":\"\",\"nm_jns_akt_mhs\":\"Penelitian\\/Riset\"},{\"id_jns_akt_mhs\":\"16\",\"a_kegiatan_kampus_merdeka\":\"1\",\"ket_jns_akt_mhs\":\"\",\"nm_jns_akt_mhs\":\"Proyek Kemanusiaan\"},{\"id_jns_akt_mhs\":\"17\",\"a_kegiatan_kampus_merdeka\":\"1\",\"ket_jns_akt_mhs\":\"\",\"nm_jns_akt_mhs\":\"Kegiatan Wirausaha\"},{\"id_jns_akt_mhs\":\"18\",\"a_kegiatan_kampus_merdeka\":\"1\",\"ket_jns_akt_mhs\":\"\",\"nm_jns_akt_mhs\":\"Studi\\/Proyek Independen\"},{\"id_jns_akt_mhs\":\"19\",\"a_kegiatan_kampus_merdeka\":\"1\",\"ket_jns_akt_mhs\":\"\",\"nm_jns_akt_mhs\":\"Membangun Desa\\/Kuliah Kerja Nyata Tematik\"}],\"table\":\"jenis_aktivitas_mahasiswa\"}";
			JSONObject o = new JSONObject(d);
			JSONArray data = o.getJSONArray("data");
			for (int i = 0; i < data.length(); i++) {
				try {
					JSONObject jsonObject = data.getJSONObject(i);
					JenisAktfitasMahasiswa jenisAktfitasMahasiswa = (JenisAktfitasMahasiswa) session
							.createCriteria(JenisAktfitasMahasiswa.class).setMaxResults(1)
							.add(Restrictions.eq("feeder", Long.parseLong(jsonObject.getString("id_jns_akt_mhs"))))
							.uniqueResult();

					if (jenisAktfitasMahasiswa == null) {
						jenisAktfitasMahasiswa = new JenisAktfitasMahasiswa();
						jenisAktfitasMahasiswa.setFeeder(Long.parseLong(jsonObject.getString("id_jns_akt_mhs")));
						jenisAktfitasMahasiswa.setKampusMerderka(
								jsonObject.getString("a_kegiatan_kampus_merdeka").trim().equals("1"));
						jenisAktfitasMahasiswa.setKeterangan(jsonObject.getString("ket_jns_akt_mhs"));
						jenisAktfitasMahasiswa.setNama(jsonObject.getString("nm_jns_akt_mhs"));
						session.getTransaction().begin();
						session.save(jenisAktfitasMahasiswa);
						session.getTransaction().commit();
					}

					if (jenisAktfitasMahasiswa != null
							&& jenisAktfitasMahasiswa.getNama().equalsIgnoreCase("Kuliah kerja nyata")) {
						ConstantValues.KKN = jenisAktfitasMahasiswa;
					} else if (jenisAktfitasMahasiswa != null
							&& jenisAktfitasMahasiswa.getNama().equalsIgnoreCase("Kerja praktek/PKL")) {
						ConstantValues.PKL = jenisAktfitasMahasiswa;
					} else if (jenisAktfitasMahasiswa != null
							&& jenisAktfitasMahasiswa.getNama().equalsIgnoreCase("Aktivitas kemahasiswaan")) {
						ConstantValues.KEGIATAN_KEMAHASISWAAN = jenisAktfitasMahasiswa;
					} else if (jenisAktfitasMahasiswa != null
							&& jenisAktfitasMahasiswa.getNama().equalsIgnoreCase("Kompetisi")) {
						ConstantValues.KOMPETENSI = jenisAktfitasMahasiswa;
					} else if (jenisAktfitasMahasiswa != null
							&& jenisAktfitasMahasiswa.getNama().equalsIgnoreCase("Program kreativitas mahasiswa")) {
						ConstantValues.KREATIFITAS = jenisAktfitasMahasiswa;
					}
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/InitDataHelper.java:2424");
				}
			}

			TipePegawai.initData(session);
			TipeMasaKerja.initData(session);
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/InitDataHelper.java:2431");
		}

		JenisSPMI.initData(session);

		int sa = ((Number) session.createCriteria(PekerjaanOrtuSiswa.class).setProjection(Projections.rowCount())
				.uniqueResult()).intValue();
		if (sa == 0) {
			List<Pekerjaan> pekerjaans = session.createCriteria(Pekerjaan.class).list();
			for (Pekerjaan pekerjaan : pekerjaans) {
				PekerjaanOrtuSiswa pekerjaanOrtuSiswa = new PekerjaanOrtuSiswa();
				pekerjaanOrtuSiswa.setNama(pekerjaan.getNama());
				pekerjaanOrtuSiswa.setKode(pekerjaan.getKode());
				session.getTransaction().begin();
				session.save(pekerjaanOrtuSiswa);
				session.getTransaction().commit();
			}
		}

		sa = ((Number) session.createCriteria(AlatTransportasiSiswa.class).setProjection(Projections.rowCount())
				.uniqueResult()).intValue();
		if (sa == 0) {
			List<AlatTransportasiMahasiswa> alatTransportasiMahasiswas = session
					.createCriteria(AlatTransportasiMahasiswa.class).list();
			for (AlatTransportasiMahasiswa alatTransportasiMahasiswa : alatTransportasiMahasiswas) {
				AlatTransportasiSiswa alatTransportasiSiswa = new AlatTransportasiSiswa();
				alatTransportasiSiswa.setNama(alatTransportasiMahasiswa.getNama());
				alatTransportasiSiswa.setKode(alatTransportasiMahasiswa.getFeeder() + "");
				session.getTransaction().begin();
				session.save(alatTransportasiSiswa);
				session.getTransaction().commit();
			}
		}

		sa = ((Number) session.createCriteria(JenisTinggalSiswa.class).setProjection(Projections.rowCount())
				.uniqueResult()).intValue();
		if (sa == 0) {
			List<JenisTinggalMahasiswa> jenisTinggalMahasiswas = session.createCriteria(JenisTinggalMahasiswa.class)
					.list();
			for (JenisTinggalMahasiswa jenisTinggalMahasiswa : jenisTinggalMahasiswas) {
				JenisTinggalSiswa jenisTinggalSiswa = new JenisTinggalSiswa();
				jenisTinggalSiswa.setNama(jenisTinggalMahasiswa.getNama());
				jenisTinggalSiswa.setKode(jenisTinggalMahasiswa.getFeeder() + "");
				session.getTransaction().begin();
				session.save(jenisTinggalSiswa);
				session.getTransaction().commit();
			}
		}

		sa = ((Number) session.createCriteria(StatusKeluarSiswa.class).setProjection(Projections.rowCount())
				.uniqueResult()).intValue();
		if (sa == 0) {
			List<StatusKeluar> statusKeluars = session.createCriteria(StatusKeluar.class).list();
			for (StatusKeluar statusKeluar : statusKeluars) {
				StatusKeluarSiswa statusKeluarSiswa = new StatusKeluarSiswa();
				statusKeluarSiswa.setNama(statusKeluar.getNama());
				statusKeluarSiswa.setKode(statusKeluar.getFeeder() + "");
				session.getTransaction().begin();
				session.save(statusKeluarSiswa);
				session.getTransaction().commit();
			}
		}

		sa = ((Number) session.createCriteria(StatusAwalSiswa.class).setProjection(Projections.rowCount())
				.uniqueResult()).intValue();
		if (sa == 0) {
			List<StatusAwalMahasiswa> statusAwals = session.createCriteria(StatusAwalMahasiswa.class).list();
			for (StatusAwalMahasiswa statusAwal : statusAwals) {
				StatusAwalSiswa statusAwalSiswa = new StatusAwalSiswa();
				statusAwalSiswa.setNama(statusAwal.getNama());
				statusAwalSiswa.setKode(statusAwal.getFeeder() + "");
				session.getTransaction().begin();
				session.save(statusAwalSiswa);
				session.getTransaction().commit();
			}
		}

		sa = ((Number) session.createCriteria(PendidikanOrangTuaSiswa.class).setProjection(Projections.rowCount())
				.uniqueResult()).intValue();
		if (sa == 0) {
			List<PendidikanOrangTua> pendidikanOrangTuas = session.createCriteria(PendidikanOrangTua.class).list();
			for (PendidikanOrangTua pendidikanOrangTua : pendidikanOrangTuas) {
				PendidikanOrangTuaSiswa pendidikanOrangTuaSiswa = new PendidikanOrangTuaSiswa();
				pendidikanOrangTuaSiswa.setNama(pendidikanOrangTua.getNama());
				pendidikanOrangTuaSiswa.setKode(pendidikanOrangTua.getKode() + "");
				session.getTransaction().begin();
				session.save(pendidikanOrangTuaSiswa);
				session.getTransaction().commit();
			}
		}

		int i = ((Number) session.createCriteria(JenisPemesananPengadaanAsset.class)
				.setProjection(Projections.rowCount()).uniqueResult()).intValue();
		if (i == 0) {
			String[] d = new String[] { "INV;Investasi", "CON;Consumable", "OPS;Operasional" };
			for (String s : d) {
				String[] ss = splitInitData(s);
				try {
					JenisPemesananPengadaanAsset jenisPemesananPengadaanAsset = (JenisPemesananPengadaanAsset) ConstantValues
							.simpleObject(session.createCriteria(JenisPemesananPengadaanAsset.class)
									.addOrder(Order.desc("id")).add(Restrictions.eq("kode", ss[0])).setMaxResults(1),
									JenisPemesananPengadaanAsset.class);
					if (jenisPemesananPengadaanAsset == null) {
						session.getTransaction().begin();
						jenisPemesananPengadaanAsset = new JenisPemesananPengadaanAsset();
						jenisPemesananPengadaanAsset.setNama(ss[1]);
						jenisPemesananPengadaanAsset.setKode(ss[0]);
						Common.refreshSaveOrUpdate(session, jenisPemesananPengadaanAsset);
						session.getTransaction().commit();
					}

				} catch (Exception e) {
					HibernateUtil.rollbackTransaction();
				}
			}
		}

		i = ((Number) session.createCriteria(SifatSurat.class).setProjection(Projections.rowCount()).uniqueResult())
				.intValue();
		if (i == 0 || i == 1) {
			String[] d = splitConfigSemicolon(
					Common.getKonfigurasi("sifat_klasifikasi_surat_masuk", "Terbatas;Penting;Rahasia;Biasa/Terbuka")
							.getNilai());
			for (String s : d) {
				try {
					SifatSurat sifatSurat = (SifatSurat) ConstantValues
							.simpleObject(session.createCriteria(SifatSurat.class).addOrder(Order.desc("id"))
									.add(Restrictions.eq("nama", s)).setMaxResults(1), SifatSurat.class);
					if (sifatSurat == null) {
						session.getTransaction().begin();
						sifatSurat = new SifatSurat();
						sifatSurat.setNama(s);
						sifatSurat.setKode(s);
						Common.refreshSaveOrUpdate(session, sifatSurat);
						session.getTransaction().commit();
					}
				} catch (Exception e) {
					HibernateUtil.rollbackTransaction();
				}
			}
		}

		InitData.executor.submit(new Runnable() {
			@Override
			public void run() {
				Session session = HibernateUtil.getSessionFactory().openSession();

				try {
					List<DaftarPengajuanTransfer> daftarPengajuanTransfers = session
							.createCriteria(DaftarPengajuanTransfer.class).add(Restrictions.isNull("satuanKerja"))
							.list();

					System.out.println("daftarPengajuanTransfers -> " + daftarPengajuanTransfers.size());
					for (DaftarPengajuanTransfer daftarPengajuanTransfer : daftarPengajuanTransfers) {
						// KE-3: TAHAN-DEADLOCK -- lihat javadoc updateSatuanKerjaBackfillTahanDeadlock.
						// Hygiene rollback & fallback silent-skip (self-healing di restart berikutnya bila
						// bukan deadlock/percobaan habis) sudah ditangani di dalam helper.
						updateSatuanKerjaBackfillTahanDeadlock(session, daftarPengajuanTransfer);
					}

					List<PermintaanPengadaanMasterAssetDetail> permintaanPengadaanMasterAssetDetails = session
							.createCriteria(PermintaanPengadaanMasterAssetDetail.class)
							.add(Restrictions.isNull("satuanKerja")).list();

					System.out.println(
							"permintaanPengadaanMasterAssetDetails -> " + permintaanPengadaanMasterAssetDetails.size());
					for (PermintaanPengadaanMasterAssetDetail permintaanPengadaanMasterAssetDetail : permintaanPengadaanMasterAssetDetails) {
						// KE-3: sama dengan loop DaftarPengajuanTransfer di atas -- pola backfill identik,
						// sama-sama rawan deadlock antar-proses yang bersaing mengunci baris yang sama.
						updateSatuanKerjaBackfillTahanDeadlock(session, permintaanPengadaanMasterAssetDetail);
					}
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/InitDataHelper.java:2603");
				} finally {
					// 2. WAJIB Tutup Session
					if (session != null && session.isOpen()) {
						// session.disconnect();
						if (session.isOpen()) {
							session.disconnect();
							session.close();
						}
					}
					HibernateUtil.closeSession();
				}
			}
		});

		try {
			File fileOut = new File("/opt/tanya");
			FileUtils.deleteDirectory(fileOut);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/InitDataHelper.java:2621");
			// TODO: handle exception
		}

		InitData.executor.submit(new Runnable() {
			@Override
			public void run() {

				String[] d = new String[] { "I/a;Juru Muda", "I/b;Juru Muda Tk.I", "I/c;Juru", "I/d;Juru Tk.I",
						"II/a;Pengatur Muda", "II/b;Pengatur Muda Tk.I", "II/c;Pengatur", "II/d;Pengatur Tk.I",
						"III/a;Penata Muda", "III/b;Penata Muda Tk.I", "III/c;Penata", "III/d;Penata Tk.I",
						"IV/a;Pembina", "IV/b;Pembina Tk.I", "IV/c;Pembina Utama Muda", "IV/d;Pembina Utama Madya",
						"IV/e;Pembina Utama" };

				for (String s : d) {
					String[] ss = splitInitData(s);

					Session session = HibernateUtil.getSessionFactory().openSession();
					try {
						GolonganPns golonganPns = (GolonganPns) ConstantValues
								.simpleObject(
										session.createCriteria(GolonganPns.class).addOrder(Order.desc("id"))
												.add(Restrictions.eq("kode", ss[0])).setMaxResults(1),
										GolonganPns.class);
						if (golonganPns == null) {
							session.getTransaction().begin();
							golonganPns = new GolonganPns();
							golonganPns.setNama(ss[1]);
							golonganPns.setKode(ss[0]);
							Common.refreshSaveOrUpdate(session, golonganPns);
							session.getTransaction().commit();
						}

					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/InitDataHelper.java:2654");

					} finally {
						// 2. WAJIB Tutup Session
						if (session != null && session.isOpen()) {
							// session.disconnect();
							if (session.isOpen()) {
								session.disconnect();
								session.close();
							}
						}
						HibernateUtil.closeSession();
					}
				}

			}
		});

		InitDataHelper.reInitClass();

		ConstantValues.otomatisTerposting = Common.bolehKonfigurasi("otomatis_terposting");

		InitData.executor.submit(new Runnable() {
			@Override
			public void run() {
				Api.initTokens();
			}
		});

		try {

			InitData.executor.submit(new Runnable() {
				@Override
				public void run() {
					ConstantValues.penggunaanLabelBahasa = Common.bolehKonfigurasi("apakah_menggunakan_label_bahasa");
					System.out.println("penggunaanLabelBahasa " + ConstantValues.penggunaanLabelBahasa);
					if (ConstantValues.penggunaanLabelBahasa) {
						Session session = HibernateUtil.getSessionFactory().openSession();
						try {
							List<LabelBahasa> labelBahasas = session.createCriteria(LabelBahasa.class).list();
							System.out.println(
									"loading data " + LabelBahasa.class.getName() + " sebanyak " + labelBahasas.size());
							for (LabelBahasa labelBahasa : labelBahasas) {
								try {
									MemoryDbUtil.getBahasaIndonesias().put(labelBahasa.getNama(),
											labelBahasa.getIndonesia());
									MemoryDbUtil.getBahasaEnglishs().put(labelBahasa.getNama(),
											labelBahasa.getEnglish());
									MemoryDbUtil.getBahasaArabs().put(labelBahasa.getNama(), labelBahasa.getArab());
								} catch (Exception e) {
									e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/InitDataHelper.java:2704");
								}
							}
						} catch (Exception e) {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/InitDataHelper.java:2708");
						} finally {
							// 2. WAJIB Tutup Session
							if (session != null && session.isOpen()) {
								// session.disconnect();
								if (session.isOpen()) {
									session.disconnect();
									session.close();
								}
							}
							HibernateUtil.closeSession();
						}

					}
				}
			});

			InitData.executor.submit(new Runnable() {
				@Override
				public void run() {
					initPembersihanFile();
				}
			});

		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/InitDataHelper.java:2733");
		} finally {
			rollbackActiveTransaction(session);
			closeOpenedSession(session);
			try {
				HibernateUtil.closeSession();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/InitDataHelper.java:2739");
			}
		}
	}

	@SuppressWarnings("unchecked")
	private static void initPembersihanFile() {
		Session sessionda = HibernateUtil.getSessionFactory().openSession();

		try {
			List<Long> longs = sessionda.createCriteria(StatuskehadiranKaryawanHarian.class)
					.setProjection(Projections.property("id"))
					.add(Restrictions.ilike("keterangan", "Fingerprint", MatchMode.ANYWHERE))
					.add(Restrictions.or(Restrictions.isNull("logAbsensi"), Restrictions.eq("logAbsensi", ""))).list();

			for (Long id : longs) {
				try {

					StatuskehadiranKaryawanHarian statuskehadiranKaryawanHarian = (StatuskehadiranKaryawanHarian) sessionda
							.createCriteria(StatuskehadiranKaryawanHarian.class).add(Restrictions.idEq(id))
							.uniqueResult();
					if (statuskehadiranKaryawanHarian != null) {

						TreeSet<String> treeSet = new TreeSet<String>();
						addLogAbsensiChunk(treeSet, statuskehadiranKaryawanHarian.getLogAbsensi());

						String[] ds = splitKeteranganAbsensi(statuskehadiranKaryawanHarian.getKeterangan());
						for (String s : ds) {
							try {
								if (s.trim().contains("Fingerprint")) {
									String ss = s.split(",")[1].trim();
									Date dateD = Common.dateFormat3.get().parse(ss);
									treeSet.add(Common.dateFormat84.get().format(dateD));
								}
							} catch (Exception e) {
								e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/InitDataHelper.java:2774");
							}
						}

						String logAbsensi = "";
						for (String s : treeSet) {
							logAbsensi += logAbsensi.isEmpty() ? s : "" + s;
						}
						statuskehadiranKaryawanHarian.setLogAbsensi(logAbsensi);

						sessionda.getTransaction().begin();
						Common.refreshUpdate(sessionda, statuskehadiranKaryawanHarian);
						sessionda.getTransaction().commit();

						System.out.println("statuskehadiranKaryawanHarian -> " + logAbsensi);
					}
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/InitDataHelper.java:2791");
				}

			}

		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/InitDataHelper.java:2797");
		} finally {
			// 2. WAJIB Tutup Session
			if (sessionda != null && sessionda.isOpen()) {
				sessionda.close();
			}
		}

		// ====================================================================
		// PEMBERSIHAN FILE TEMPORARY (mv/rm/find)
		// DI-GATE konfigurasi: rangkaian perintah OS ini dapat MENGGANTUNG bootstrap
		// bila folder /backup1/temporary sangat besar/lambat (rm -rf, find full-tree)
		// atau karena pipa stderr penuh. Default MATI agar startup aman.
		// Aktifkan dengan konfigurasi "aktifkan_pembersihan_file_temporary" = AKTIF (1).
		// Catatan: pembersihan "find ... *data_tagihans*/*data_bulans* -delete"
		// memang dinonaktifkan permanen (atas permintaan) dan tidak diikutkan lagi.
		// ====================================================================
		if (!Common.getKonfigurasi("aktifkan_pembersihan_file_temporary", Konfigurasi.TIDAK_AKTIF).getNilai().trim()
				.equalsIgnoreCase(Konfigurasi.AKTIF)) {
			System.out.println(
					"Pembersihan file temporary DILEWATI (konfigurasi 'aktifkan_pembersihan_file_temporary' tidak aktif).");
			return;
		}

		runOsCleanupCommand(
				"mv " + ConstantValues.lokasiFileTemproraryTemp + "VirtualAccountBank -> VirtualAccountBank_Old",
				new String[] { "mv", ConstantValues.lokasiFileTemproraryTemp + "VirtualAccountBank",
						ConstantValues.lokasiFileTemproraryTemp + "VirtualAccountBank_Old" });

		runOsCleanupCommand("rm -rf " + ConstantValues.lokasiFileTemproraryTemp + "VirtualAccountBank_Old",
				new String[] { "rm", "-rf", ConstantValues.lokasiFileTemproraryTemp + "VirtualAccountBank_Old" });

		{
			String conf = Common.getKonfigurasi("hapus_temporary_lama", "2000").getNilai().trim();
			// Perbaikan: terminator -exec dulu salah ("\\"). Pakai -type f -delete yang valid.
			runOsCleanupCommand("find " + ConstantValues.lokasiFileTemproraryTemp + " -mtime +" + conf + " -type f -delete",
					new String[] { "find", ConstantValues.lokasiFileTemproraryTemp, "-mtime", "+" + conf, "-type", "f",
							"-delete" });
		}

		runOsCleanupCommand("mv " + ConstantValues.lokasiFileTemproraryTemp + "LampiranLain -> Old",
				new String[] { "mv", ConstantValues.lokasiFileTemproraryTemp + "LampiranLain",
						ConstantValues.lokasiFileTemproraryTemp + "Old" });

		runOsCleanupCommand("mv " + ConstantValues.lokasiFileTemproraryTemp + "DetailKegiatan -> Old_d",
				new String[] { "mv", ConstantValues.lokasiFileTemproraryTemp + "DetailKegiatan",
						ConstantValues.lokasiFileTemproraryTemp + "Old_d" });

		runOsCleanupCommand("rm -rf " + ConstantValues.lokasiFileTemproraryTemp + "Old",
				new String[] { "rm", "-rf", ConstantValues.lokasiFileTemproraryTemp + "Old" });

		runOsCleanupCommand("rm -rf " + ConstantValues.lokasiFileTemproraryTemp + "Old_d",
				new String[] { "rm", "-rf", ConstantValues.lokasiFileTemproraryTemp + "Old_d" });

		runOsCleanupCommand("rm -rf " + ConstantValues.lokasiFileTemproraryTemp + "LampiranLain",
				new String[] { "rm", "-rf", ConstantValues.lokasiFileTemproraryTemp + "LampiranLain" });

		runOsCleanupCommand("rm -rf " + ConstantValues.lokasiFileTemproraryTemp + "Foto*",
				new String[] { "rm", "-rf", ConstantValues.lokasiFileTemproraryTemp + "Foto*" });
	}

	/**
	 * Menjalankan perintah OS untuk pembersihan file temporary secara AMAN:
	 * <ul>
	 * <li>redirectErrorStream(true) — gabung stderr ke stdout agar buffer pipa tidak
	 * penuh (penyebab deadlock/hang klasik ProcessBuilder).</li>
	 * <li>output selalu dikuras sampai EOF.</li>
	 * <li>watchdog timeout (konfigurasi "timeout_pembersihan_file_temporary_detik",
	 * default 120) menghentikan paksa proses yang menggantung agar bootstrap tidak
	 * terkunci.</li>
	 * </ul>
	 * Gaya Java 1.6/1.7 (tanpa lambda / try-with-resources / API Java 8).
	 */
	private static void runOsCleanupCommand(String label, String[] command) {
		System.out.println("Jalankan perintah ->  " + label);

		int timeoutDetik = 120;
		try {
			timeoutDetik = Integer.parseInt(
					Common.getKonfigurasi("timeout_pembersihan_file_temporary_detik", "120").getNilai().trim());
		} catch (Exception eIgnore) { ais.common.ErrorAuditUtil.record(eIgnore, "auto-audit(empty-catch) src/ais/common/InitDataHelper.java:2877");
		}

		Process p = null;
		Thread watchdog = null;
		try {
			ProcessBuilder process = new ProcessBuilder(command);
			process.redirectErrorStream(true);
			p = process.start();

			final Process pFinal = p;
			final int timeoutFinal = timeoutDetik;
			final String labelFinal = label;
			watchdog = new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						Thread.sleep(timeoutFinal * 1000L);
					} catch (InterruptedException ie) {
						return; // proses sudah selesai normal; watchdog dibatalkan
					}
					try {
						System.out.println("Perintah '" + labelFinal + "' melewati batas " + timeoutFinal
								+ " detik, dihentikan paksa.");
						pFinal.destroy();
					} catch (Exception eIgnore) { ais.common.ErrorAuditUtil.record(eIgnore, "auto-audit(empty-catch) src/ais/common/InitDataHelper.java:2902");
					}
				}
			});
			watchdog.setDaemon(true);
			watchdog.start();

			BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
			StringBuilder builder = new StringBuilder();
			String line = null;
			while ((line = reader.readLine()) != null) {
				builder.append(line);
				builder.append(System.getProperty("line.separator"));
			}
			try {
				reader.close();
			} catch (Exception eIgnore) { ais.common.ErrorAuditUtil.record(eIgnore, "auto-audit(empty-catch) src/ais/common/InitDataHelper.java:2918");
			}

			p.waitFor();
			watchdog.interrupt(); // batalkan watchdog bila proses selesai duluan
			System.out.println("Hasil " + label + " -> " + builder.toString());

		} catch (Exception e) {
			if (p != null) {
				try {
					p.destroy();
				} catch (Exception eIgnore) { ais.common.ErrorAuditUtil.record(eIgnore, "auto-audit(empty-catch) src/ais/common/InitDataHelper.java:2929");
				}
			}
			if (watchdog != null) {
				try {
					watchdog.interrupt();
				} catch (Exception eIgnore) { ais.common.ErrorAuditUtil.record(eIgnore, "auto-audit(empty-catch) src/ais/common/InitDataHelper.java:2935");
				}
			}
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/InitDataHelper.java:2938");
		}
	}

	@SuppressWarnings({ "rawtypes" })
	/**
	 * Update satu entity dengan TAHAN-DEADLOCK (retry singkat + backoff) — dipakai oleh loop backfill
	 * satuanKerja null (DaftarPengajuanTransfer/PermintaanPengadaanMasterAssetDetail) yang berjalan
	 * di background thread saat boot. KE-3: PostgreSQL dapat melaporkan deadlock antar-proses yang
	 * bersaing mengunci baris yang sama (mis. dua boot/restart tumpang-tindih atau proses lain
	 * menyentuh tabel yang sama); tanpa retry, baris itu gagal diperbarui pada boot ini dan baru
	 * terisi di restart berikutnya (silent, tapi membanjiri log error tiap kali terjadi). Dengan
	 * retry singkat, deadlock TRANSIENT langsung pulih dalam boot yang sama. Error NON-deadlock
	 * (mis. constraint lain) TETAP dilewati diam-diam setelah 1x percobaan — konsisten dengan
	 * perilaku lama (self-healing di restart berikutnya via filter Restrictions.isNull di query),
	 * retry hanya untuk deadlock/serialization-failure yang genuinely dapat pulih dengan diulang.
	 */
	private static boolean updateSatuanKerjaBackfillTahanDeadlock(Session session, GeneralValueObject entity) {
		if (entity == null || entity.getId() == null) {
			return false;
		}
		// KE-3 lanjutan: JANGAN pakai Common.refreshUpdate (flush ENTITY PENUH) di sini. Flush
		// penuh memaksa Hibernate memanggil SEMUA getter entity via reflection (dirty-check) --
		// utk DaftarPengajuanTransfer ini TERMASUK getNominal(), yang punya EFEK SAMPING
		// menjalankan query TERPISAH (hitungTotalPphSaldoAwal, lihat komentar di sana). Saat loop
		// backfill boot memproses banyak baris berturut-turut, query ekstra tak terkait ini
		// memperberat tekanan pool c3p0 dan berisiko gagal checkout koneksi (InterruptedException
		// saat pool padat). Fix: UPDATE HQL TERARAH -- hanya kolom satuanKerja, TIDAK memicu
		// dirty-check/flush properti lain sama sekali. Nilai satuanKerja tetap diturunkan lewat
		// getter khusus tiap entity (navigasi asosiasi lokal, bukan query baru) sebelum di-UPDATE.
		SatuanKerja satuanKerja;
		String namaEntitas;
		if (entity instanceof DaftarPengajuanTransfer) {
			satuanKerja = ((DaftarPengajuanTransfer) entity).getSatuanKerja();
			namaEntitas = "DaftarPengajuanTransfer";
		} else if (entity instanceof PermintaanPengadaanMasterAssetDetail) {
			satuanKerja = ((PermintaanPengadaanMasterAssetDetail) entity).getSatuanKerja();
			namaEntitas = "PermintaanPengadaanMasterAssetDetail";
		} else {
			return false;
		}
		if (satuanKerja == null || satuanKerja.getId() == null) {
			return false;
		}

		int maksimal = 3;
		for (int percobaan = 1; percobaan <= maksimal; percobaan++) {
			try {
				session.getTransaction().begin();
				session.createQuery("update " + namaEntitas + " set satuanKerja = :sk where id = :entId")
						.setEntity("sk", satuanKerja).setLong("entId", entity.getId()).executeUpdate();
				session.getTransaction().commit();
				return true;
			} catch (Exception e) {
				// Hygiene transaksi: rollback bila masih aktif agar begin() percobaan/iterasi berikutnya
				// tidak gagal "Transaction already active".
				try {
					if (session.getTransaction() != null && session.getTransaction().isActive()) {
						session.getTransaction().rollback();
					}
				} catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/common/InitDataHelper.java:2998");
				}
				if (isDeadlockAtauSerializationFailure(e) && percobaan < maksimal) {
					try {
						Thread.sleep(150L * percobaan + (long) (Math.random() * 100));
					} catch (InterruptedException ie) {
						Thread.currentThread().interrupt();
						return false;
					}
					continue;
				}
				return false;
			}
		}
		return false;
	}

	/**
	 * SQLState/pesan deadlock &amp; serialization-failure PostgreSQL (pola sama dgn
	 * TagihanUtil.isKonflikKunci) — DITAMBAH {@link InterruptedException} dari checkout koneksi
	 * c3p0 (mis. {@code BasicResourcePool.prelimCheckoutResource}) yang MUNCUL saat pool sedang
	 * padat di boot (banyak thread backfill/preload bersaing) lalu thread terinterupsi sebelum
	 * dapat koneksi. Kondisi ini TRANSIENT (sesaat, pool biasanya longgar lagi setelah jeda
	 * singkat) sehingga layak diulang, sama seperti deadlock/lock-timeout.
	 */
	private static boolean isDeadlockAtauSerializationFailure(Throwable e) {
		Throwable c = e;
		while (c != null) {
			if (c instanceof InterruptedException) {
				return true;
			}
			String state = (c instanceof java.sql.SQLException) ? ((java.sql.SQLException) c).getSQLState() : null;
			if ("40P01".equals(state) || "40001".equals(state) || "55P03".equals(state) || "57014".equals(state)
					|| "25P02".equals(state)) {
				return true;
			}
			String msg = c.getMessage();
			if (msg != null) {
				String m = msg.toLowerCase();
				if (m.indexOf("deadlock detected") >= 0 || m.indexOf("could not serialize") >= 0
						|| m.indexOf("lock timeout") >= 0 || m.indexOf("current transaction is aborted") >= 0
						|| m.indexOf("checkout") >= 0 || m.indexOf("resourcepool") >= 0) {
					return true;
				}
			}
			c = c.getCause();
		}
		return false;
	}

	private static void doInitData(Class clazz) {
		if (clazz == null) {
			return;
		}

		String className = StringUtils.split(clazz.getName(), "_")[0];
		boolean sukses = false;

		System.out.println("doInitData MASUK -> " + clazz.getName());

		synchronized (LOCK_CLASS_INIT) {
			Map<String, String> dataClass = MemoryDbUtil.getDataClass();
			if (dataClass != null && dataClass.containsKey(className)) {
				System.out.println("Data " + className + " sudah ada di cache");
				return;
			}
			if (dataClass != null) {
				dataClass.put(className, className);
			}
		}

		System.out.println("doInitData CACHE-CHECK OK -> " + className + " (akan openSession)");

		// Tandai thread ini SEDANG PRELOAD sebelum membuka session: AuditTimestampInterceptor.onLoad
		// mengecek flag ini agar entity yang dimuat SELAMA preload TIDAK ikut tercatat ke
		// EntityAccessCache sbg "baru diakses" -- mencegah preload menandai-ulang id yg baru saja
		// dimuatnya sendiri (self-reinforcing: krn aplikasi restart otomatis tiap hari, riwayat 3-hari
		// tak akan pernah menyusut bila load preload dihitung sbg akses nyata).
		ais.common.EntityAccessCache.tandaiPreload(true);

		// 1. Buka Session Baru (Isolated Session)
		// Menggunakan openSession() agar terpisah dari session HTTP request
		Session session = HibernateUtil.getSessionFactory().openSession();

		System.out.println("doInitData SESSION OK -> " + className);

		try {
			// Batasi statement_timeout untuk thread init ini. Sebelumnya 0 (tak terbatas)
			// sehingga COUNT(*)/load pada tabel BESAR bisa MENGGANTUNG bootstrap selamanya.
			// Dengan batas waktu, query yang kelamaan dibatalkan PostgreSQL → kelas di-skip
			// oleh catch di bawah dan bootstrap tetap lanjut. Konfig "statement_timeout_init_ms".
			int initTimeoutMsTmp = 120000;
			try {
				initTimeoutMsTmp = Integer.parseInt(
						Common.getKonfigurasi("statement_timeout_init_ms", "120000").getNilai().trim());
			} catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/common/InitDataHelper.java:3093");
			}
			final int initTimeoutMs = initTimeoutMsTmp;

			System.out.println("MULAI init " + className + " (statement_timeout=" + initTimeoutMs + "ms) ...");

			session.doWork(new org.hibernate.jdbc.Work() {
				@Override
				public void execute(java.sql.Connection conn) throws java.sql.SQLException {
					conn.createStatement().execute("SET statement_timeout = " + initTimeoutMs);
				}
			});

			// Cek jumlah data — fallback ke MAX_VALUE jika query dibatalkan
			Number jumlah;
			try {
				jumlah = (Number) session.createCriteria(clazz).setProjection(Projections.rowCount()).uniqueResult();
				if (jumlah == null) jumlah = 0;
			} catch (Exception countEx) {
				System.out.println("[InitDataHelper] COUNT(*) gagal untuk " + className
						+ " — asumsikan tabel besar, skip load. Penyebab: " + countEx.getMessage());
				jumlah = Integer.MAX_VALUE;
			}

			// --- BLOK LOGIKA KHUSUS ---

			if (className.equals(Tbmrole.class.getName())) {
				handleTbmRole(session, clazz);
			} else if (className.equals(AlurSop.class.getName())) {
				handleAlurSop(session, clazz);
			} else if (className.equals(JenisCatatanSiswa.class.getName())) {
				handleJenisCatatanSiswa(session, clazz);
			} else if (className.equals(Pegawai.class.getName())) {
				handlePegawai(session, clazz);
			}

			// Tabel BESAR (Tbmuser/Dosen): JANGAN full-load saat bootstrap — menyebabkan
			// startup menggantung (criteria.list() seluruh baris). Lewati preload penuh;
			// data dimuat on-demand dari DB (konsisten dgn cabang "terlalu banyak" di bawah).
			// KECUALI ada riwayat akses nyata 3-hari-terakhir (EntityAccessCache) dari proses
			// sebelum restart — warm-start HANYA id yang benar-benar pernah dipakai, tetap
			// jauh lebih murah daripada full-load tabel besar.
			else if (className.equals(ais.database.model.Tbmuser.class.getName())
					|| className.equals(ais.database.model.Dosen.class.getName())) {
				List<Long> idTerakhirBesar = EntityAccessCache.ambilIdTerakhir(clazz);
				if (!idTerakhirBesar.isEmpty()) {
					System.out.println("Warm-start " + className + " dari riwayat akses 3 hari: "
							+ idTerakhirBesar.size() + " id.");
					Criteria cBesar = session.createCriteria(clazz).add(Restrictions.in("id", idTerakhirBesar));
					loadAndInitWithCriteria(cBesar, clazz);
				} else {
					System.out.println("Skip preload memory untuk " + className
							+ " (tabel besar, belum ada riwayat akses) — dimuat on-demand. Jumlah=" + jumlah);
					String key = className.split("_")[0];
					ais.common.MemoryCacheUtil.get(key);
				}
			}

			// --- BLOK LOGIKA UMUM (Menggunakan Helper) ---

			else {
				Criteria c = null;

				// RIWAYAT AKSES NYATA (3 hari terakhir, dari EntityAccessCache) menggantikan heuristik
				// tanggal di bawah bila tersedia — id yang benar-benar dipakai lebih presisi daripada
				// tebakan "3 tahun angkatan terakhir" dsb, dan bisa jauh lebih sedikit/lebih banyak
				// tergantung pola pakai kampus. Kosong (mis. boot pertama setelah deploy fitur ini,
				// atau kelas tak dilacak) -> fallback ke heuristik lama, TIDAK ADA perubahan perilaku.
				List<Long> idTerakhir = EntityAccessCache.ambilIdTerakhir(clazz);
				if (!idTerakhir.isEmpty()) {
					System.out.println(
							"Warm-start " + className + " dari riwayat akses 3 hari: " + idTerakhir.size() + " id.");
					c = session.createCriteria(clazz).add(Restrictions.in("id", idTerakhir));
				} else if (className.equals(BiodataMahasiswa.class.getName())) {
					c = session.createCriteria(clazz).createAlias("mahasiswa", "mahasiswa")
							.add(Restrictions.gt("mahasiswa.tahunangkatan",
									Calendar.getInstance().get(Calendar.YEAR) - 3))
							.addOrder(Order.desc("mahasiswa.id")).add(Restrictions.isNull("mahasiswa.statusKeluar"));
				} else if (className.equals(Mahasiswa.class.getName())) {
					c = session.createCriteria(clazz).addOrder(Order.desc("id"))
							.add(Restrictions.isNull("statusKeluar"))
							.add(Restrictions.gt("tahunangkatan", Calendar.getInstance().get(Calendar.YEAR) - 3));
				} else if (className.equals(BiodataCalonMahasiswa.class.getName())) {
					c = session.createCriteria(clazz).addOrder(Order.desc("id"))
							.add(Restrictions.gt("tahun", Calendar.getInstance().get(Calendar.YEAR) - 1));
				} else if (className.equals(Siswa.class.getName())) {
					c = session.createCriteria(clazz).addOrder(Order.desc("id"))
							.add(Restrictions.gt("tahunMasuk", Calendar.getInstance().get(Calendar.YEAR) - 6));
				} else if (className.equals(CalonSiswa.class.getName())) {
					c = session.createCriteria(clazz).addOrder(Order.desc("id"))
							.add(Restrictions.gt("tahunMasuk", Calendar.getInstance().get(Calendar.YEAR) - 1));
				} else if (className.equals(Perkuliahan.class.getName())) {
					Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
					String current = (calendar.get(Calendar.MONTH) > 5)
							? calendar.get(Calendar.YEAR) + "/" + (calendar.get(Calendar.YEAR) + 1)
							: (calendar.get(Calendar.YEAR) - 1) + "/" + (calendar.get(Calendar.YEAR));

					c = session.createCriteria(clazz).addOrder(Order.desc("id")).setMaxResults(5000)
							.add(Restrictions.eq("tahunAjaran", current));
				} else if (className.equals(Item.class.getName())) {
					c = session.createCriteria(clazz).addOrder(Order.desc("id")).setMaxResults(500);
				}

				// Eksekusi Criteria jika sudah didefinisikan di atas
				if (c != null) {
					loadAndInitWithCriteria(c, clazz);
				}
				// Fallback: Default load jika data sedikit atau ditandai 'jangan dibersihkan'
				else if (GeneralValueObject.merupakanJanganDibersihkan(clazz) || jumlah.intValue() < 100) {
					loadAndInitStandardData(session, clazz);
				}
				// Data terlalu banyak dan tidak ada penanganan khusus
				else {
					String key = className;
					String[] split = key.split("_");
					if (split.length > 0)
						key = split[0];

					System.out.println("Data " + className + " terlalu banyak (" + jumlah + "), skip init memory.");
					ais.common.MemoryCacheUtil.get(key);
				}
			}
			sukses = true;

		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/InitDataHelper.java:3218");
			if (!sukses) {
				synchronized (LOCK_CLASS_INIT) {
					Map<String, String> dataClass = MemoryDbUtil.getDataClass();
					if (dataClass != null) {
						dataClass.remove(className);
					}
				}
			}
		} finally {

			try {
				session.disconnect();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/InitDataHelper.java:3231");
				// TODO: handle exception
			}
			try {
				session.close();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/InitDataHelper.java:3236");
				// TODO: handle exception
			}
			try {
				ais.common.EntityAccessCache.tandaiPreload(false);
			} catch (Throwable ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/InitDataHelper.java:3241");
			}

		}
	}

	// ---------------------------------------------------------
	// HELPER METHODS (Refactoring)
	// ---------------------------------------------------------

	@SuppressWarnings("rawtypes")
	private static void loadAndInitStandardData(Session session, Class clazz) {
		Criteria criteria = session.createCriteria(clazz);
		loadAndInitWithCriteria(criteria, clazz);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static void loadAndInitWithCriteria(Criteria criteria, Class clazz) {
		System.out.println("MULAI load " + clazz.getName() + " ...");
		long mulaiLoad = System.currentTimeMillis();
		List<GeneralValueObject> list = criteria.list();
		System.out.println("loading data " + clazz.getName() + " sebanyak " + list.size() + " (query "
				+ (System.currentTimeMillis() - mulaiLoad) + " ms)");
		for (GeneralValueObject dd : list) {
			try {
				reInitDataBaru(dd);
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/InitDataHelper.java:3268");
			}
		}
	}

	// --- Specific Class Handlers ---

	@SuppressWarnings({ "unchecked", "unused", "rawtypes" })
	private static void handleTbmRole(Session session, Class clazz) {
		System.out.println("MULAI load " + clazz.getName() + " ...");
		Criteria criteria = session.createCriteria(clazz);
		List<Tbmrole> d = criteria.addOrder(Order.desc("id")).list();
		System.out.println("loading data " + clazz.getName() + " sebanyak " + d.size());

		for (Tbmrole dd : d) {
			try {
				if (dd.getAktif()) {
					// Trigger Lazy Loading di dalam session yang valid
					for (Menu menu : dd.getMenus()) {
						/* Loop dummy */ }
				}
				reInitDataBaru(dd);

				String rId = dd.getRoleId();
				if (rId != null) {
					if (rId.equalsIgnoreCase(Tbmrole.MAHASISWA))
						ConstantValues.tbmroleMahasiswa = dd;
					else if (rId.equalsIgnoreCase(Tbmrole.SISWA))
						ConstantValues.tbmroleSiswa = dd;
					else if (rId.equalsIgnoreCase(Tbmrole.PENDUDUK))
						ConstantValues.tbmrolePenduduk = dd;
					else if (rId.equalsIgnoreCase(Tbmrole.DOSEN))
						ConstantValues.roleDosen = dd;
					else if (rId.equalsIgnoreCase(Tbmrole.GURU))
						ConstantValues.roleGuru = dd;
					else if (rId.equalsIgnoreCase(Tbmrole.KANTIN))
						ConstantValues.roleKantin = dd;
					else if (rId.equalsIgnoreCase(Tbmrole.ORANG_TUA_KODE))
						ConstantValues.roleOrangTua = dd;
				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/InitDataHelper.java:3309");
			}
		}

		// Logic Insert Default Role (Membutuhkan Transaksi)
		Transaction tx = null;
		try {
			boolean butuhCommit = false;

			if (ConstantValues.tbmrolePenduduk == null) {
				if (tx == null) {
					tx = session.beginTransaction();
					butuhCommit = true;
				}
				ConstantValues.tbmrolePenduduk = new Tbmrole();
				ConstantValues.tbmrolePenduduk.setRoleId(Tbmrole.PENDUDUK);
				ConstantValues.tbmrolePenduduk.setRoleName("Penduduk");
				session.save(ConstantValues.tbmrolePenduduk);
			}

			if (ConstantValues.roleKantin == null) {
				if (tx == null) {
					tx = session.beginTransaction();
					butuhCommit = true;
				}
				ConstantValues.roleKantin = new Tbmrole();
				ConstantValues.roleKantin.setRoleId(Tbmrole.KANTIN);
				ConstantValues.roleKantin.setRoleName("Kantin");
				ConstantValues.roleKantin.setKantin(true);
				ConstantValues.roleKantin.setHalamanUtama("/WEB-INF/baru/modul/kantin/index.jsp");
				ConstantValues.roleKantin.setEbisnisMenu(ais.common.EbisnisMenuKatalog.defaultMenuKantinJson());
				session.save(ConstantValues.roleKantin);
			}

			if (ConstantValues.roleGuru == null) {
				if (tx == null) {
					tx = session.beginTransaction();
					butuhCommit = true;
				}
				ConstantValues.roleGuru = new Tbmrole();
				ConstantValues.roleGuru.setRoleId(Tbmrole.GURU);
				ConstantValues.roleGuru.setRoleName("Guru");
				session.save(ConstantValues.roleGuru);
			}

			if (ConstantValues.roleOrangTua == null) {
				if (tx == null) {
					tx = session.beginTransaction();
					butuhCommit = true;
				}

				String sql = "INSERT INTO tbmrole(roleid, rolename) VALUES ('" + Tbmrole.ORANG_TUA_KODE + "','"
						+ Tbmrole.ORANG_TUA + "')";
				int u = Common.updateSql(sql);
				System.out.println("Insert Role OrangTua sql -> " + sql + ", result ->" + u);

				ConstantValues.roleOrangTua = (Tbmrole) session.createCriteria(Tbmrole.class)
						.add(Restrictions.idEq(Tbmrole.ORANG_TUA_KODE)).uniqueResult();
			}

			if (butuhCommit && tx != null) {
				session.flush();
				tx.commit();
			}
		} catch (Exception e) {
			if (tx != null)
				tx.rollback();
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/InitDataHelper.java:3375");
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static void handleAlurSop(Session session, Class clazz) {
		System.out.println("MULAI load " + clazz.getName() + " ...");
		List<AlurSop> d = session.createCriteria(clazz).addOrder(Order.desc("id")).list();
		System.out.println("loading data " + clazz.getName() + " sebanyak " + d.size());
		for (AlurSop dd : d) {
			try {
				Set<DokumenAlurSop> docs = new HashSet<DokumenAlurSop>();
				for (DokumenAlurSop menu : dd.getDokumenAlurSops()) {
					if (menu.getAktif())
						docs.add(menu);
				}
				AlurSop.mapDokumens.put(dd.getId(), docs);

				Set<KelompokParameterTambahanAlurSop> params = new TreeSet<KelompokParameterTambahanAlurSop>();
				for (KelompokParameterTambahanAlurSop menu : dd.getKelompokParameterTambahanAlurSops()) {
					params.add(menu);
				}
				AlurSop.mapParameters.put(dd.getId(), params);

				reInitDataBaru(dd);
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/InitDataHelper.java:3401");
			}
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static void handleJenisCatatanSiswa(Session session, Class clazz) {
		System.out.println("MULAI load " + clazz.getName() + " ...");
		List<JenisCatatanSiswa> d = session.createCriteria(clazz).addOrder(Order.desc("id")).list();
		System.out.println("loading data " + clazz.getName() + " sebanyak " + d.size());
		for (JenisCatatanSiswa dd : d) {
			try {
				Set<KelompokParameterTambahanCatatanSiswa> params = new TreeSet<KelompokParameterTambahanCatatanSiswa>();
				for (KelompokParameterTambahanCatatanSiswa menu : dd.getKelompokParameterTambahanCatatanSiswas()) {
					params.add(menu);
				}
				JenisCatatanSiswa.mapParameters.put(dd.getId(), params);
				reInitDataBaru(dd);
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/InitDataHelper.java:3420");
			}
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static void handlePegawai(Session session, Class clazz) {
		System.out.println("MULAI load " + clazz.getName() + " ...");
		int size = 1;
		int mulai = 0;
		while (size > 0) {
			Criteria c = session.createCriteria(clazz).setFirstResult(mulai).setMaxResults(50);
			List<GeneralValueObject> d = c.list();
			size = d.size();
			mulai += 50;
			System.out.println("loading data " + clazz.getName() + " sebanyak " + size + ", mulai -> " + mulai);
			for (GeneralValueObject dd : d) {
				try {
					reInitDataBaru(dd);
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/InitDataHelper.java:3440");
				}
			}
			try {
				session.clear();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/InitDataHelper.java:3445");
			}
		}
	}

	// ---------------------------------------------------------
	// UTILITY METHODS (Cache Management)
	// ---------------------------------------------------------

	@SuppressWarnings("unchecked")
	public static void reInitDataBaru(GeneralValueObject data) {
		if (data == null) {
			return;
		}

		// Daftarkan ke EntityIdentityMap sebelum masuk MapDB.
		// MapDB menyerialkan objek (salinan baru tiap deserialisasi), sehingga
		// canonical di sini adalah instance Hibernate asli yang selalu terkini.
		EntityIdentityMap.canonical(data);

		String clazz = StringUtils.split(data.getClass().getName(), "_")[0];
		Map<Serializable, GeneralValueObject> dataJsonMasterSimple = ais.common.MemoryCacheUtil.get(clazz);
		if (dataJsonMasterSimple == null) {
			return;
		}

		Serializable key = null;
		try {
			if (data instanceof Tbmuser) {
				key = ((Tbmuser) data).getUserId();
			} else if (data instanceof Tbmrole) {
				key = ((Tbmrole) data).getRoleId();
			} else {
				key = data.getId();
			}
		} catch (Exception e) {
			key = null;
		}

		if (key != null) {
			synchronized (dataJsonMasterSimple) {
				dataJsonMasterSimple.put(key, data);
			}
		}
	}

	public static void reInitDataUpdate(GeneralValueObject generalValueObjectBaru) {
		if (generalValueObjectBaru == null)
			return;

		// Pastikan entity yang baru disimpan terdaftar sebagai canonical
		EntityIdentityMap.canonical(generalValueObjectBaru);

		GeneralValueObject data = null;
		if (generalValueObjectBaru instanceof Tbmuser) {
			Tbmuser tbmuser = (Tbmuser) generalValueObjectBaru;
			data = ConstantValues.ambil(Tbmuser.class.getName(), tbmuser.getUserId(), false);
		} else if (generalValueObjectBaru instanceof Tbmrole) {
			Tbmrole tbmrole = (Tbmrole) generalValueObjectBaru;
			data = ConstantValues.ambil(Tbmrole.class.getName(), tbmrole.getRoleId(), false);

		} else {
			data = ConstantValues.ambil(generalValueObjectBaru.getClass().getName(), generalValueObjectBaru.getId(),
					false);
		}

		if (data != null) {

			try {
				BeanUtilsBean.getInstance().copyProperties(data, generalValueObjectBaru);
			} catch (Exception e) {
				reInitDataBaru(generalValueObjectBaru);
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/InitDataHelper.java:3517");
			}
		} else {
			// FIX: Gunakan objek baru jika data lama tidak ditemukan di cache
			reInitDataBaru(generalValueObjectBaru);
		}
	}

	public static void initDataKegiatanKemahasiswan() {
		// 1. Buka Session Baru (Isolated Session)
		// Menggunakan openSession() agar terpisah dari session HTTP request
		Session session = HibernateUtil.getSessionFactory().openSession();

		try {

			int count = ((Number) session.createCriteria(SkalaKegiatanKemahasiswaan.class)
					.setProjection(Projections.rowCount()).uniqueResult()).intValue();
			session.getTransaction().begin();
			SkalaKegiatanKemahasiswaan fakJur = null;
			SkalaKegiatanKemahasiswaan Institut = null;
			SkalaKegiatanKemahasiswaan Regional = null;
			SkalaKegiatanKemahasiswaan Nasional = null;
			SkalaKegiatanKemahasiswaan Internasional = null;
			SkalaKegiatanKemahasiswaan TAHUN_1 = null;
			SkalaKegiatanKemahasiswaan SMESTER_1 = null;
			SkalaKegiatanKemahasiswaan BULAN_3 = null;
			SkalaKegiatanKemahasiswaan BULAN_2 = null;
			SkalaKegiatanKemahasiswaan BULAN_1 = null;
			SkalaKegiatanKemahasiswaan MUNGGU_2 = null;
			SkalaKegiatanKemahasiswaan HARI_10 = null;
			SkalaKegiatanKemahasiswaan HARI_9 = null;
			SkalaKegiatanKemahasiswaan HARI_5 = null;
			SkalaKegiatanKemahasiswaan HARI_2 = null;
			SkalaKegiatanKemahasiswaan DITAMPILKAN_DI_LUAR_KAMPUS = null;
			SkalaKegiatanKemahasiswaan DITAMPILKAN_DI_KAMPUS = null;
			SkalaKegiatanKemahasiswaan TIDAK_DITAMPILKAN = null;
			SkalaKegiatanKemahasiswaan Penerjemah = null;

			SkalaKegiatanKemahasiswaan Editor_Penerjemah = null;
			SkalaKegiatanKemahasiswaan Juru_tulis = null;
			SkalaKegiatanKemahasiswaan Pengarang_perorangan = null;
			SkalaKegiatanKemahasiswaan Pengarang_berkelompok = null;
			SkalaKegiatanKemahasiswaan Editor_buku = null;
			SkalaKegiatanKemahasiswaan Ketua_kelompok = null;
			SkalaKegiatanKemahasiswaan Anggota_kelompok = null;
			SkalaKegiatanKemahasiswaan Perorangan = null;
			SkalaKegiatanKemahasiswaan KETUA = null;
			SkalaKegiatanKemahasiswaan PENGURUS = null;
			SkalaKegiatanKemahasiswaan ANGGOTA = null;

			SkalaKegiatanKemahasiswaan KALI = null;

			if (count == 0) {
				fakJur = new SkalaKegiatanKemahasiswaan();
				fakJur.setNama("Fak./Jur");
				fakJur.setNomorUrut(5);
				fakJur.setKeterangan("Skala Kegiatan Kemahasiswaan Fak./Jur");
				session.save(fakJur);

				Institut = new SkalaKegiatanKemahasiswaan();
				Institut.setNama("Institut");
				Institut.setNomorUrut(4);
				Institut.setKeterangan("Skala Kegiatan Kemahasiswaan Institut");
				session.save(Institut);

				Regional = new SkalaKegiatanKemahasiswaan();
				Regional.setNama("Regional");
				Regional.setNomorUrut(3);
				Regional.setKeterangan("Skala Kegiatan Kemahasiswaan Regional");
				session.save(Regional);

				Nasional = new SkalaKegiatanKemahasiswaan();
				Nasional.setNama("Nasional");
				Nasional.setNomorUrut(2);
				Nasional.setKeterangan("Skala Kegiatan Kemahasiswaan Nasional");
				session.save(Nasional);

				Internasional = new SkalaKegiatanKemahasiswaan();
				Internasional.setNama("Internasional");
				Internasional.setNomorUrut(1);
				Internasional.setKeterangan("Skala Kegiatan Kemahasiswaan Internasional");
				session.save(Internasional);

				TAHUN_1 = new SkalaKegiatanKemahasiswaan();
				TAHUN_1.setNama("1 TAHUN");
				TAHUN_1.setNomorUrut(6);
				TAHUN_1.setKeterangan("Skala Kegiatan Kemahasiswaan 1 TAHUN (12 bulan)");
				session.save(TAHUN_1);

				SMESTER_1 = new SkalaKegiatanKemahasiswaan();
				SMESTER_1.setNama("1 SMESTER");
				SMESTER_1.setNomorUrut(7);
				SMESTER_1.setKeterangan("Skala Kegiatan Kemahasiswaan 6 bulan");
				session.save(SMESTER_1);

				BULAN_3 = new SkalaKegiatanKemahasiswaan();
				BULAN_3.setNama("3 BULAN - < 1 SMESTER");
				BULAN_3.setNomorUrut(8);
				BULAN_3.setKeterangan("Skala Kegiatan Kemahasiswaan 3 bulan");
				session.save(BULAN_3);

				BULAN_2 = new SkalaKegiatanKemahasiswaan();
				BULAN_2.setNama("2 - < 3 BULAN");
				BULAN_2.setNomorUrut(9);
				BULAN_2.setKeterangan("Skala Kegiatan Kemahasiswaan 2 bulan");
				session.save(BULAN_2);

				BULAN_1 = new SkalaKegiatanKemahasiswaan();
				BULAN_1.setNama("1 - < 2 BULAN");
				BULAN_1.setNomorUrut(10);
				BULAN_1.setKeterangan("Skala Kegiatan Kemahasiswaan 1 bulan");
				session.save(BULAN_1);

				MUNGGU_2 = new SkalaKegiatanKemahasiswaan();
				MUNGGU_2.setNama(">2 Minggu");
				MUNGGU_2.setNomorUrut(11);
				MUNGGU_2.setKeterangan("Skala Kegiatan Kemahasiswaan > 2 Minggu");
				session.save(MUNGGU_2);

				HARI_10 = new SkalaKegiatanKemahasiswaan();
				HARI_10.setNama("10 hari - 2 minggu");
				HARI_10.setNomorUrut(12);
				HARI_10.setKeterangan("Skala Kegiatan Kemahasiswaan 10 hari - 2 minggu");
				session.save(HARI_10);

				HARI_9 = new SkalaKegiatanKemahasiswaan();
				HARI_9.setNama("6 - 9 hari");
				HARI_9.setNomorUrut(13);
				HARI_9.setKeterangan("Skala Kegiatan Kemahasiswaan 6 - 9 hari");
				session.save(HARI_9);

				HARI_5 = new SkalaKegiatanKemahasiswaan();
				HARI_5.setNama("3 - 5 hari");
				HARI_5.setNomorUrut(14);
				HARI_5.setKeterangan("Skala Kegiatan Kemahasiswaan 3 - 5 hari");
				session.save(HARI_5);

				HARI_2 = new SkalaKegiatanKemahasiswaan();
				HARI_2.setNama("1-2 hari");
				HARI_2.setNomorUrut(15);
				HARI_2.setKeterangan("Skala Kegiatan Kemahasiswaan 3 - 5 hari");
				session.save(HARI_2);

				DITAMPILKAN_DI_LUAR_KAMPUS = new SkalaKegiatanKemahasiswaan();
				DITAMPILKAN_DI_LUAR_KAMPUS.setNama("DITAMPILKAN DI LUAR KAMPUS");
				DITAMPILKAN_DI_LUAR_KAMPUS.setNomorUrut(16);
				DITAMPILKAN_DI_LUAR_KAMPUS.setKeterangan("Skala Kegiatan Kemahasiswaan DITAMPILKAN DI LUAR KAMPUS");
				session.save(DITAMPILKAN_DI_LUAR_KAMPUS);

				DITAMPILKAN_DI_KAMPUS = new SkalaKegiatanKemahasiswaan();
				DITAMPILKAN_DI_KAMPUS.setNama("DITAMPILKAN DI KAMPUS");
				DITAMPILKAN_DI_KAMPUS.setNomorUrut(17);
				DITAMPILKAN_DI_KAMPUS.setKeterangan("Skala Kegiatan Kemahasiswaan DITAMPILKAN DI KAMPUS");
				session.save(DITAMPILKAN_DI_KAMPUS);

				TIDAK_DITAMPILKAN = new SkalaKegiatanKemahasiswaan();
				TIDAK_DITAMPILKAN.setNama("TIDAK DITAMPILKAN");
				TIDAK_DITAMPILKAN.setNomorUrut(18);
				TIDAK_DITAMPILKAN.setKeterangan("Skala Kegiatan Kemahasiswaan TIDAK DITAMPILKAN");
				session.save(TIDAK_DITAMPILKAN);

				Penerjemah = new SkalaKegiatanKemahasiswaan();
				Penerjemah.setNama("Penerjemah");
				Penerjemah.setNomorUrut(19);
				Penerjemah.setKeterangan("Skala Kegiatan Kemahasiswaan Penerjemah");
				session.save(Penerjemah);

				Editor_Penerjemah = new SkalaKegiatanKemahasiswaan();
				Editor_Penerjemah.setNama("Editor Penerjemah");
				Editor_Penerjemah.setNomorUrut(20);
				Editor_Penerjemah.setKeterangan("Skala Kegiatan Kemahasiswaan Editor");
				session.save(Editor_Penerjemah);

				Juru_tulis = new SkalaKegiatanKemahasiswaan();
				Juru_tulis.setNama("Juru tulis");
				Juru_tulis.setNomorUrut(21);
				Juru_tulis.setKeterangan("Skala Kegiatan Kemahasiswaan Juru tulis");
				session.save(Juru_tulis);

				Pengarang_perorangan = new SkalaKegiatanKemahasiswaan();
				Pengarang_perorangan.setNama("Pengarang perorangan");
				Pengarang_perorangan.setNomorUrut(22);
				Pengarang_perorangan.setKeterangan("Skala Kegiatan Kemahasiswaan Pengarang perorangan");
				session.save(Pengarang_perorangan);

				Pengarang_berkelompok = new SkalaKegiatanKemahasiswaan();
				Pengarang_berkelompok.setNama("Pengarang berkelompok");
				Pengarang_berkelompok.setNomorUrut(23);
				Pengarang_berkelompok.setKeterangan("Skala Kegiatan Kemahasiswaan Pengarang berkelompok");
				session.save(Pengarang_berkelompok);

				Editor_buku = new SkalaKegiatanKemahasiswaan();
				Editor_buku.setNama("Editor buku");
				Editor_buku.setNomorUrut(24);
				Editor_buku.setKeterangan("Skala Kegiatan Kemahasiswaan Editor buku");
				session.save(Editor_buku);

				Ketua_kelompok = new SkalaKegiatanKemahasiswaan();
				Ketua_kelompok.setNama("Ketua kelompok");
				Ketua_kelompok.setNomorUrut(25);
				Ketua_kelompok.setKeterangan("Skala Kegiatan Kemahasiswaan Ketua kelompok");
				session.save(Ketua_kelompok);

				Anggota_kelompok = new SkalaKegiatanKemahasiswaan();
				Anggota_kelompok.setNama("Anggota kelompok");
				Anggota_kelompok.setNomorUrut(26);
				Anggota_kelompok.setKeterangan("Skala Kegiatan Kemahasiswaan Ketua kelompok");
				session.save(Anggota_kelompok);

				Perorangan = new SkalaKegiatanKemahasiswaan();
				Perorangan.setNama("Perorangan");
				Perorangan.setNomorUrut(27);
				Perorangan.setKeterangan("Skala Kegiatan Kemahasiswaan Perorangan");
				session.save(Perorangan);

				KETUA = new SkalaKegiatanKemahasiswaan();
				KETUA.setNama("KETUA");
				KETUA.setNomorUrut(28);
				KETUA.setKeterangan("Skala Kegiatan Kemahasiswaan KETUA");
				session.save(KETUA);

				PENGURUS = new SkalaKegiatanKemahasiswaan();
				PENGURUS.setNama("PENGURUS/PANITIA INTI");
				PENGURUS.setNomorUrut(29);
				PENGURUS.setKeterangan("Skala Kegiatan Kemahasiswaan PENGURUS/PANITIA INTI");
				session.save(PENGURUS);

				ANGGOTA = new SkalaKegiatanKemahasiswaan();
				ANGGOTA.setNama("ANGGOTA PENGURUS/PANITIA LAINNYA");
				ANGGOTA.setNomorUrut(30);
				ANGGOTA.setKeterangan("Skala Kegiatan Kemahasiswaan ANGGOTA PENGURUS/PANITIA LAINNYA");
				session.save(ANGGOTA);

				KALI = new SkalaKegiatanKemahasiswaan();
				KALI.setNama("Setiap kali tampil");
				KALI.setNomorUrut(31);
				KALI.setKeterangan("Skala Kegiatan Kemahasiswaan setiap kali tampil");
				session.save(KALI);
			}

			count = ((Number) session.createCriteria(JabatanKegiatanKemahasiswaan.class)
					.setProjection(Projections.rowCount()).uniqueResult()).intValue();
			JabatanKegiatanKemahasiswaan panitia = null;
			JabatanKegiatanKemahasiswaan peserta = null;
			JabatanKegiatanKemahasiswaan narasumber = null;
			JabatanKegiatanKemahasiswaan juaraI = null;
			JabatanKegiatanKemahasiswaan juaraII = null;
			JabatanKegiatanKemahasiswaan juaraIII = null;
			JabatanKegiatanKemahasiswaan beregu = null;
			if (count == 0) {
				panitia = new JabatanKegiatanKemahasiswaan();
				panitia.setNama("Panitia");
				panitia.setNomorUrut(2);
				panitia.setKeterangan("Jabatan/Status/Tugas Kegiatan Kemahasiswaan Sebagai Panitia");
				session.save(panitia);

				peserta = new JabatanKegiatanKemahasiswaan();
				peserta.setNama("Peserta");
				peserta.setNomorUrut(1);
				peserta.setKeterangan("Jabatan/Status/Tugas Kegiatan Kemahasiswaan Sebagai Peserta");
				session.save(peserta);

				narasumber = new JabatanKegiatanKemahasiswaan();
				narasumber.setNama("Narasumber");
				narasumber.setNomorUrut(3);
				narasumber.setKeterangan("Jabatan/Status/Tugas Kegiatan Kemahasiswaan Sebagai Narasumber");
				session.save(narasumber);

				juaraI = new JabatanKegiatanKemahasiswaan();
				juaraI.setNama("Juara I");
				juaraI.setNomorUrut(4);
				juaraI.setKeterangan("Jabatan/Status/Tugas Kegiatan Kemahasiswaan Sebagai Juara I");
				session.save(juaraI);

				juaraII = new JabatanKegiatanKemahasiswaan();
				juaraII.setNama("Juara II");
				juaraII.setNomorUrut(5);
				juaraII.setKeterangan("Jabatan/Status/Tugas Kegiatan Kemahasiswaan Sebagai Juara II");
				session.save(juaraII);

				juaraIII = new JabatanKegiatanKemahasiswaan();
				juaraIII.setNama("Juara III");
				juaraIII.setNomorUrut(6);
				juaraIII.setKeterangan("Jabatan/Status/Tugas Kegiatan Kemahasiswaan Sebagai Juara II");
				session.save(juaraIII);

				beregu = new JabatanKegiatanKemahasiswaan();
				beregu.setNama("Beregu/perorangan");
				beregu.setNomorUrut(7);
				beregu.setKeterangan("Jabatan/Status/Tugas Kegiatan Kemahasiswaan Sebagai Beregu/perorangan");
				session.save(beregu);

			}

			count = ((Number) session.createCriteria(JenisKelompokKegiatanKemahasiswaan.class)
					.setProjection(Projections.rowCount()).uniqueResult()).intValue();
			JenisKelompokKegiatanKemahasiswaan utama = null;
			JenisKelompokKegiatanKemahasiswaan penunjang = null;
			if (count == 0) {
				utama = new JenisKelompokKegiatanKemahasiswaan();
				utama.setNama("Kelompok Utama");
				utama.setKeterangan("Jenis Kelompok Kegiatan Kemahasiswaan yang masuk Kelompok Utama");
				session.save(utama);

				penunjang = new JenisKelompokKegiatanKemahasiswaan();
				penunjang.setNama("Kelompok Penunjang");

				penunjang.setKeterangan("Jenis Kelompok Kegiatan Kemahasiswaan yang masuk Kelompok Penunjang");
				session.save(penunjang);
			}

			count = ((Number) session.createCriteria(KelompokKegiatanKemahasiswaan.class)
					.setProjection(Projections.rowCount()).uniqueResult()).intValue();
			if (count == 0) {

				KelompokKegiatanKemahasiswaan kelompokKegiatanKemahasiswaan = new KelompokKegiatanKemahasiswaan();
				kelompokKegiatanKemahasiswaan.setNama("Keagamaan dan moral pancasila");
				kelompokKegiatanKemahasiswaan.setNomorUrut(1);
				kelompokKegiatanKemahasiswaan.setJenisKelompokKegiatanKemahasiswaan(utama);
				kelompokKegiatanKemahasiswaan
						.setKeterangan("Kelompok Kegiatan Kemahasiswaan keagamaan dan moral pancasila");
				session.save(kelompokKegiatanKemahasiswaan);

				DetailKelompokKegiatanKemahasiswaan detailKelompokKegiatanKemahasiswaan = new DetailKelompokKegiatanKemahasiswaan();
				detailKelompokKegiatanKemahasiswaan.setNama("PHBI");
				detailKelompokKegiatanKemahasiswaan.setNomorUrut(1);
				detailKelompokKegiatanKemahasiswaan.setKelompokKegiatanKemahasiswaan(kelompokKegiatanKemahasiswaan);
				detailKelompokKegiatanKemahasiswaan.getJabatanKegiatanKemahasiswaans().add(panitia);
				detailKelompokKegiatanKemahasiswaan.getJabatanKegiatanKemahasiswaans().add(narasumber);

				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(Internasional);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(Nasional);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(Regional);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(Institut);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(fakJur);

				session.save(detailKelompokKegiatanKemahasiswaan);

				detailKelompokKegiatanKemahasiswaan = new DetailKelompokKegiatanKemahasiswaan();
				detailKelompokKegiatanKemahasiswaan.setNama("PHBN");
				detailKelompokKegiatanKemahasiswaan.setNomorUrut(2);
				detailKelompokKegiatanKemahasiswaan.setKelompokKegiatanKemahasiswaan(kelompokKegiatanKemahasiswaan);
				detailKelompokKegiatanKemahasiswaan.getJabatanKegiatanKemahasiswaans().add(panitia);
				detailKelompokKegiatanKemahasiswaan.getJabatanKegiatanKemahasiswaans().add(narasumber);

				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(Internasional);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(Nasional);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(Regional);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(Institut);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(fakJur);

				session.save(detailKelompokKegiatanKemahasiswaan);

				kelompokKegiatanKemahasiswaan = new KelompokKegiatanKemahasiswaan();
				kelompokKegiatanKemahasiswaan.setNomorUrut(2);
				kelompokKegiatanKemahasiswaan.setNama("Penalaran dan Ilmiah");
				kelompokKegiatanKemahasiswaan.setJenisKelompokKegiatanKemahasiswaan(utama);
				kelompokKegiatanKemahasiswaan.setKeterangan("Kelompok Kegiatan Kemahasiswaan penalaran dan Ilmiah");
				session.save(kelompokKegiatanKemahasiswaan);

				detailKelompokKegiatanKemahasiswaan = new DetailKelompokKegiatanKemahasiswaan();
				detailKelompokKegiatanKemahasiswaan
						.setNama("Diskusi,seminar,workshop,lokakarya,symposium dan ceramah ilmiah");
				detailKelompokKegiatanKemahasiswaan.setNomorUrut(1);
				detailKelompokKegiatanKemahasiswaan.setKelompokKegiatanKemahasiswaan(kelompokKegiatanKemahasiswaan);
				detailKelompokKegiatanKemahasiswaan.getJabatanKegiatanKemahasiswaans().add(panitia);
				detailKelompokKegiatanKemahasiswaan.getJabatanKegiatanKemahasiswaans().add(narasumber);
				detailKelompokKegiatanKemahasiswaan.getJabatanKegiatanKemahasiswaans().add(peserta);

				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(Internasional);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(Nasional);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(Regional);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(Institut);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(fakJur);

				session.save(detailKelompokKegiatanKemahasiswaan);

				detailKelompokKegiatanKemahasiswaan = new DetailKelompokKegiatanKemahasiswaan();
				detailKelompokKegiatanKemahasiswaan.setNama("Tulisan ilmiah/karya tulis");
				detailKelompokKegiatanKemahasiswaan.setNomorUrut(2);
				detailKelompokKegiatanKemahasiswaan.setKelompokKegiatanKemahasiswaan(kelompokKegiatanKemahasiswaan);

				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(Internasional);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(Nasional);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(Regional);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(Institut);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(fakJur);

				session.save(detailKelompokKegiatanKemahasiswaan);

				detailKelompokKegiatanKemahasiswaan = new DetailKelompokKegiatanKemahasiswaan();
				detailKelompokKegiatanKemahasiswaan.setNama("Lomba ilmiah (cerdas Cermat dan sejenisnya)");
				detailKelompokKegiatanKemahasiswaan.setNomorUrut(3);
				detailKelompokKegiatanKemahasiswaan.setKelompokKegiatanKemahasiswaan(kelompokKegiatanKemahasiswaan);

				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(Internasional);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(Nasional);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(Regional);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(Institut);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(fakJur);

				session.save(detailKelompokKegiatanKemahasiswaan);

				kelompokKegiatanKemahasiswaan = new KelompokKegiatanKemahasiswaan();
				kelompokKegiatanKemahasiswaan.setNomorUrut(3);
				kelompokKegiatanKemahasiswaan.setNama("Pelatihan");
				kelompokKegiatanKemahasiswaan.setJenisKelompokKegiatanKemahasiswaan(utama);
				kelompokKegiatanKemahasiswaan.setKeterangan("Kelompok Kegiatan Kemahasiswaan pelatihan");
				session.save(kelompokKegiatanKemahasiswaan);

				detailKelompokKegiatanKemahasiswaan = new DetailKelompokKegiatanKemahasiswaan();
				detailKelompokKegiatanKemahasiswaan.setNama("Pelatihan/penataran kegiatan ilmiah/akademik");
				detailKelompokKegiatanKemahasiswaan.setNomorUrut(1);
				detailKelompokKegiatanKemahasiswaan.setKelompokKegiatanKemahasiswaan(kelompokKegiatanKemahasiswaan);
				detailKelompokKegiatanKemahasiswaan.getJabatanKegiatanKemahasiswaans().add(panitia);
				detailKelompokKegiatanKemahasiswaan.getJabatanKegiatanKemahasiswaans().add(narasumber);
				detailKelompokKegiatanKemahasiswaan.getJabatanKegiatanKemahasiswaans().add(peserta);

				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(Internasional);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(Nasional);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(Regional);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(Institut);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(fakJur);

				session.save(detailKelompokKegiatanKemahasiswaan);

				detailKelompokKegiatanKemahasiswaan = new DetailKelompokKegiatanKemahasiswaan();
				detailKelompokKegiatanKemahasiswaan.setNama("Pelatihan/penataran kegiatan keagamaan");
				detailKelompokKegiatanKemahasiswaan.setNomorUrut(2);
				detailKelompokKegiatanKemahasiswaan.setKelompokKegiatanKemahasiswaan(kelompokKegiatanKemahasiswaan);
				detailKelompokKegiatanKemahasiswaan.getJabatanKegiatanKemahasiswaans().add(panitia);
				detailKelompokKegiatanKemahasiswaan.getJabatanKegiatanKemahasiswaans().add(narasumber);
				detailKelompokKegiatanKemahasiswaan.getJabatanKegiatanKemahasiswaans().add(peserta);

				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(Internasional);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(Nasional);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(Regional);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(Institut);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(fakJur);

				session.save(detailKelompokKegiatanKemahasiswaan);

				detailKelompokKegiatanKemahasiswaan = new DetailKelompokKegiatanKemahasiswaan();
				detailKelompokKegiatanKemahasiswaan.setNama("Pelatihan/penataran kemahasiswaan/kepemudaan");
				detailKelompokKegiatanKemahasiswaan.setNomorUrut(3);
				detailKelompokKegiatanKemahasiswaan.setKelompokKegiatanKemahasiswaan(kelompokKegiatanKemahasiswaan);
				detailKelompokKegiatanKemahasiswaan.getJabatanKegiatanKemahasiswaans().add(panitia);
				detailKelompokKegiatanKemahasiswaan.getJabatanKegiatanKemahasiswaans().add(narasumber);
				detailKelompokKegiatanKemahasiswaan.getJabatanKegiatanKemahasiswaans().add(peserta);

				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(Internasional);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(Nasional);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(Regional);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(Institut);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(fakJur);

				session.save(detailKelompokKegiatanKemahasiswaan);

				detailKelompokKegiatanKemahasiswaan = new DetailKelompokKegiatanKemahasiswaan();
				detailKelompokKegiatanKemahasiswaan
						.setNama("Pelatihan/penataran pembinaan karakter kebangsaan/nasionalisme");
				detailKelompokKegiatanKemahasiswaan.setNomorUrut(4);
				detailKelompokKegiatanKemahasiswaan.setKelompokKegiatanKemahasiswaan(kelompokKegiatanKemahasiswaan);
				detailKelompokKegiatanKemahasiswaan.getJabatanKegiatanKemahasiswaans().add(panitia);
				detailKelompokKegiatanKemahasiswaan.getJabatanKegiatanKemahasiswaans().add(narasumber);
				detailKelompokKegiatanKemahasiswaan.getJabatanKegiatanKemahasiswaans().add(peserta);

				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(Internasional);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(Nasional);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(Regional);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(Institut);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(fakJur);

				session.save(detailKelompokKegiatanKemahasiswaan);

				detailKelompokKegiatanKemahasiswaan = new DetailKelompokKegiatanKemahasiswaan();
				detailKelompokKegiatanKemahasiswaan.setNama("Pelatihan /penataran kegiatan keolahragaan");
				detailKelompokKegiatanKemahasiswaan.setNomorUrut(5);
				detailKelompokKegiatanKemahasiswaan.setKelompokKegiatanKemahasiswaan(kelompokKegiatanKemahasiswaan);
				detailKelompokKegiatanKemahasiswaan.getJabatanKegiatanKemahasiswaans().add(panitia);
				detailKelompokKegiatanKemahasiswaan.getJabatanKegiatanKemahasiswaans().add(narasumber);
				detailKelompokKegiatanKemahasiswaan.getJabatanKegiatanKemahasiswaans().add(peserta);

				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(Internasional);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(Nasional);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(Regional);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(Institut);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(fakJur);

				session.save(detailKelompokKegiatanKemahasiswaan);

				detailKelompokKegiatanKemahasiswaan = new DetailKelompokKegiatanKemahasiswaan();
				detailKelompokKegiatanKemahasiswaan.setNama("Pelatihan/penataran kegiatan seni");
				detailKelompokKegiatanKemahasiswaan.setNomorUrut(6);
				detailKelompokKegiatanKemahasiswaan.setKelompokKegiatanKemahasiswaan(kelompokKegiatanKemahasiswaan);
				detailKelompokKegiatanKemahasiswaan.getJabatanKegiatanKemahasiswaans().add(panitia);
				detailKelompokKegiatanKemahasiswaan.getJabatanKegiatanKemahasiswaans().add(narasumber);
				detailKelompokKegiatanKemahasiswaan.getJabatanKegiatanKemahasiswaans().add(peserta);

				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(Internasional);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(Nasional);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(Regional);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(Institut);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(fakJur);

				session.save(detailKelompokKegiatanKemahasiswaan);

				kelompokKegiatanKemahasiswaan = new KelompokKegiatanKemahasiswaan();
				kelompokKegiatanKemahasiswaan.setNomorUrut(4);
				kelompokKegiatanKemahasiswaan.setNama("Bakat dan minat");
				kelompokKegiatanKemahasiswaan.setJenisKelompokKegiatanKemahasiswaan(utama);
				kelompokKegiatanKemahasiswaan.setKeterangan("Kelompok Kegiatan Kemahasiswaan bakat dan minat");
				session.save(kelompokKegiatanKemahasiswaan);

				detailKelompokKegiatanKemahasiswaan = new DetailKelompokKegiatanKemahasiswaan();
				detailKelompokKegiatanKemahasiswaan.setNama("Keterlibatan pertandingan olah raga/seni");
				detailKelompokKegiatanKemahasiswaan.setNomorUrut(1);
				detailKelompokKegiatanKemahasiswaan.setKelompokKegiatanKemahasiswaan(kelompokKegiatanKemahasiswaan);
				detailKelompokKegiatanKemahasiswaan.getJabatanKegiatanKemahasiswaans().add(panitia);
				detailKelompokKegiatanKemahasiswaan.getJabatanKegiatanKemahasiswaans().add(peserta);

				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(Internasional);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(Nasional);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(Regional);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(Institut);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(fakJur);

				session.save(detailKelompokKegiatanKemahasiswaan);

				detailKelompokKegiatanKemahasiswaan = new DetailKelompokKegiatanKemahasiswaan();
				detailKelompokKegiatanKemahasiswaan.setNama("Kejuaraan olah raga/seni berkelompok/perorangan");
				detailKelompokKegiatanKemahasiswaan.setNomorUrut(2);
				detailKelompokKegiatanKemahasiswaan.setKelompokKegiatanKemahasiswaan(kelompokKegiatanKemahasiswaan);
				detailKelompokKegiatanKemahasiswaan.getJabatanKegiatanKemahasiswaans().add(juaraI);
				detailKelompokKegiatanKemahasiswaan.getJabatanKegiatanKemahasiswaans().add(juaraII);
				detailKelompokKegiatanKemahasiswaan.getJabatanKegiatanKemahasiswaans().add(juaraIII);

				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(Internasional);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(Nasional);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(Regional);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(Institut);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(fakJur);

				session.save(detailKelompokKegiatanKemahasiswaan);

				detailKelompokKegiatanKemahasiswaan = new DetailKelompokKegiatanKemahasiswaan();
				detailKelompokKegiatanKemahasiswaan.setNama("Pementasan olah raga dan seni");
				detailKelompokKegiatanKemahasiswaan.setNomorUrut(3);
				detailKelompokKegiatanKemahasiswaan.setKelompokKegiatanKemahasiswaan(kelompokKegiatanKemahasiswaan);
				detailKelompokKegiatanKemahasiswaan.getJabatanKegiatanKemahasiswaans().add(beregu);

				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(Internasional);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(Nasional);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(Regional);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(Institut);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(fakJur);

				session.save(detailKelompokKegiatanKemahasiswaan);

				kelompokKegiatanKemahasiswaan = new KelompokKegiatanKemahasiswaan();
				kelompokKegiatanKemahasiswaan.setNomorUrut(5);
				kelompokKegiatanKemahasiswaan.setNama("Pengabdian di lingkungan kampus");
				kelompokKegiatanKemahasiswaan.setJenisKelompokKegiatanKemahasiswaan(utama);
				kelompokKegiatanKemahasiswaan
						.setKeterangan("Kelompok Kegiatan Kemahasiswaan pengabdian di lingkungan kampus");
				session.save(kelompokKegiatanKemahasiswaan);

				detailKelompokKegiatanKemahasiswaan = new DetailKelompokKegiatanKemahasiswaan();
				detailKelompokKegiatanKemahasiswaan.setNama(
						"Pelatih/Pembina Kegiatan Keagamaan, Kemasyarakatan,Kepemudaan, Kesiswaan, Kemahasiswaan Atau Keolahragaan Dan Seni");
				detailKelompokKegiatanKemahasiswaan.setNomorUrut(1);
				detailKelompokKegiatanKemahasiswaan.setKelompokKegiatanKemahasiswaan(kelompokKegiatanKemahasiswaan);

				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(TAHUN_1);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(SMESTER_1);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(BULAN_3);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(BULAN_2);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(BULAN_1);

				session.save(detailKelompokKegiatanKemahasiswaan);

				detailKelompokKegiatanKemahasiswaan = new DetailKelompokKegiatanKemahasiswaan();
				detailKelompokKegiatanKemahasiswaan.setNama("Asisten Laboratorium");
				detailKelompokKegiatanKemahasiswaan.setNomorUrut(2);
				detailKelompokKegiatanKemahasiswaan.setKelompokKegiatanKemahasiswaan(kelompokKegiatanKemahasiswaan);

				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(TAHUN_1);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(SMESTER_1);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(BULAN_3);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(BULAN_2);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(BULAN_1);

				session.save(detailKelompokKegiatanKemahasiswaan);

				detailKelompokKegiatanKemahasiswaan = new DetailKelompokKegiatanKemahasiswaan();
				detailKelompokKegiatanKemahasiswaan
						.setNama("Bakti social atas penugasan institusi kampus dan lembaga kemahasiswaan");
				detailKelompokKegiatanKemahasiswaan.setNomorUrut(3);
				detailKelompokKegiatanKemahasiswaan.setKelompokKegiatanKemahasiswaan(kelompokKegiatanKemahasiswaan);

				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(MUNGGU_2);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(HARI_10);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(HARI_9);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(HARI_5);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(HARI_2);

				session.save(detailKelompokKegiatanKemahasiswaan);

				kelompokKegiatanKemahasiswaan = new KelompokKegiatanKemahasiswaan();
				kelompokKegiatanKemahasiswaan.setNomorUrut(6);
				kelompokKegiatanKemahasiswaan.setNama("Penalaran dan Ilmiah Lainnya");
				kelompokKegiatanKemahasiswaan.setJenisKelompokKegiatanKemahasiswaan(utama);
				kelompokKegiatanKemahasiswaan
						.setKeterangan("Kelompok Kegiatan Kemahasiswaan penalaran dan Ilmiah Lainnya");
				session.save(kelompokKegiatanKemahasiswaan);

				detailKelompokKegiatanKemahasiswaan = new DetailKelompokKegiatanKemahasiswaan();
				detailKelompokKegiatanKemahasiswaan.setNama("Karya seni");
				detailKelompokKegiatanKemahasiswaan.setNomorUrut(1);
				detailKelompokKegiatanKemahasiswaan.setKelompokKegiatanKemahasiswaan(kelompokKegiatanKemahasiswaan);

				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(DITAMPILKAN_DI_LUAR_KAMPUS);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(DITAMPILKAN_DI_KAMPUS);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(TIDAK_DITAMPILKAN);

				session.save(detailKelompokKegiatanKemahasiswaan);

				detailKelompokKegiatanKemahasiswaan = new DetailKelompokKegiatanKemahasiswaan();
				detailKelompokKegiatanKemahasiswaan.setNama("Karya tulis");
				detailKelompokKegiatanKemahasiswaan.setNomorUrut(2);
				detailKelompokKegiatanKemahasiswaan.setKelompokKegiatanKemahasiswaan(kelompokKegiatanKemahasiswaan);

				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(DITAMPILKAN_DI_LUAR_KAMPUS);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(DITAMPILKAN_DI_KAMPUS);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(TIDAK_DITAMPILKAN);

				session.save(detailKelompokKegiatanKemahasiswaan);

				detailKelompokKegiatanKemahasiswaan = new DetailKelompokKegiatanKemahasiswaan();
				detailKelompokKegiatanKemahasiswaan.setNama("Skenario");
				detailKelompokKegiatanKemahasiswaan.setNomorUrut(3);
				detailKelompokKegiatanKemahasiswaan.setKelompokKegiatanKemahasiswaan(kelompokKegiatanKemahasiswaan);

				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(DITAMPILKAN_DI_LUAR_KAMPUS);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(DITAMPILKAN_DI_KAMPUS);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(TIDAK_DITAMPILKAN);

				session.save(detailKelompokKegiatanKemahasiswaan);

				detailKelompokKegiatanKemahasiswaan = new DetailKelompokKegiatanKemahasiswaan();
				detailKelompokKegiatanKemahasiswaan.setNama("Penerjemahan buku");
				detailKelompokKegiatanKemahasiswaan.setNomorUrut(4);
				detailKelompokKegiatanKemahasiswaan.setKelompokKegiatanKemahasiswaan(kelompokKegiatanKemahasiswaan);

				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(Penerjemah);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(Editor_Penerjemah);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(Juru_tulis);

				session.save(detailKelompokKegiatanKemahasiswaan);

				detailKelompokKegiatanKemahasiswaan = new DetailKelompokKegiatanKemahasiswaan();
				detailKelompokKegiatanKemahasiswaan.setNama("Penerjemahan naskah");
				detailKelompokKegiatanKemahasiswaan.setNomorUrut(5);
				detailKelompokKegiatanKemahasiswaan.setKelompokKegiatanKemahasiswaan(kelompokKegiatanKemahasiswaan);

				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(Penerjemah);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(Editor_Penerjemah);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(Juru_tulis);

				session.save(detailKelompokKegiatanKemahasiswaan);

				detailKelompokKegiatanKemahasiswaan = new DetailKelompokKegiatanKemahasiswaan();
				detailKelompokKegiatanKemahasiswaan.setNama("Membuat buku");
				detailKelompokKegiatanKemahasiswaan.setNomorUrut(6);
				detailKelompokKegiatanKemahasiswaan.setKelompokKegiatanKemahasiswaan(kelompokKegiatanKemahasiswaan);

				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(Pengarang_perorangan);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(Pengarang_berkelompok);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(Editor_buku);

				session.save(detailKelompokKegiatanKemahasiswaan);

				detailKelompokKegiatanKemahasiswaan = new DetailKelompokKegiatanKemahasiswaan();
				detailKelompokKegiatanKemahasiswaan.setNama("Penelitian");
				detailKelompokKegiatanKemahasiswaan.setNomorUrut(7);
				detailKelompokKegiatanKemahasiswaan.setKelompokKegiatanKemahasiswaan(kelompokKegiatanKemahasiswaan);

				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(Ketua_kelompok);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(Anggota_kelompok);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(Perorangan);

				session.save(detailKelompokKegiatanKemahasiswaan);

				kelompokKegiatanKemahasiswaan = new KelompokKegiatanKemahasiswaan();
				kelompokKegiatanKemahasiswaan.setNomorUrut(7);
				kelompokKegiatanKemahasiswaan.setJenisKelompokKegiatanKemahasiswaan(penunjang);
				kelompokKegiatanKemahasiswaan.setNama("Kepemimpinan dan keorganisasian intra kampus");
				kelompokKegiatanKemahasiswaan
						.setKeterangan("Kelompok Kegiatan Kemahasiswaan Kepemimpinan dan keorganisasian intra kampus");
				session.save(kelompokKegiatanKemahasiswaan);

				detailKelompokKegiatanKemahasiswaan = new DetailKelompokKegiatanKemahasiswaan();
				detailKelompokKegiatanKemahasiswaan.setNama("Pengurus Dema-I/Sema-I");
				detailKelompokKegiatanKemahasiswaan.setNomorUrut(1);
				detailKelompokKegiatanKemahasiswaan.setKelompokKegiatanKemahasiswaan(kelompokKegiatanKemahasiswaan);

				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(KETUA);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(PENGURUS);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(ANGGOTA);

				session.save(detailKelompokKegiatanKemahasiswaan);

				detailKelompokKegiatanKemahasiswaan = new DetailKelompokKegiatanKemahasiswaan();
				detailKelompokKegiatanKemahasiswaan.setNama("Pengurus DEMA");
				detailKelompokKegiatanKemahasiswaan.setNomorUrut(2);
				detailKelompokKegiatanKemahasiswaan.setKelompokKegiatanKemahasiswaan(kelompokKegiatanKemahasiswaan);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(KETUA);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(PENGURUS);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(ANGGOTA);
				session.save(detailKelompokKegiatanKemahasiswaan);

				detailKelompokKegiatanKemahasiswaan = new DetailKelompokKegiatanKemahasiswaan();
				detailKelompokKegiatanKemahasiswaan.setNama("Pengurus HMJ");
				detailKelompokKegiatanKemahasiswaan.setNomorUrut(3);
				detailKelompokKegiatanKemahasiswaan.setKelompokKegiatanKemahasiswaan(kelompokKegiatanKemahasiswaan);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(KETUA);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(PENGURUS);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(ANGGOTA);
				session.save(detailKelompokKegiatanKemahasiswaan);

				detailKelompokKegiatanKemahasiswaan = new DetailKelompokKegiatanKemahasiswaan();
				detailKelompokKegiatanKemahasiswaan.setNama("Pengurus HMPS");
				detailKelompokKegiatanKemahasiswaan.setNomorUrut(4);
				detailKelompokKegiatanKemahasiswaan.setKelompokKegiatanKemahasiswaan(kelompokKegiatanKemahasiswaan);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(KETUA);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(PENGURUS);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(ANGGOTA);
				session.save(detailKelompokKegiatanKemahasiswaan);

				detailKelompokKegiatanKemahasiswaan = new DetailKelompokKegiatanKemahasiswaan();
				detailKelompokKegiatanKemahasiswaan.setNama("Pengurus UKM");
				detailKelompokKegiatanKemahasiswaan.setNomorUrut(5);
				detailKelompokKegiatanKemahasiswaan.setKelompokKegiatanKemahasiswaan(kelompokKegiatanKemahasiswaan);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(KETUA);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(PENGURUS);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(ANGGOTA);
				session.save(detailKelompokKegiatanKemahasiswaan);

				detailKelompokKegiatanKemahasiswaan = new DetailKelompokKegiatanKemahasiswaan();
				detailKelompokKegiatanKemahasiswaan.setNama("Pengurus UKK");
				detailKelompokKegiatanKemahasiswaan.setNomorUrut(6);
				detailKelompokKegiatanKemahasiswaan.setKelompokKegiatanKemahasiswaan(kelompokKegiatanKemahasiswaan);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(KETUA);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(PENGURUS);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(ANGGOTA);
				session.save(detailKelompokKegiatanKemahasiswaan);

				detailKelompokKegiatanKemahasiswaan = new DetailKelompokKegiatanKemahasiswaan();
				detailKelompokKegiatanKemahasiswaan.setNama("Kepanitiaan/Tim, Tingkat Institut");
				detailKelompokKegiatanKemahasiswaan.setNomorUrut(7);
				detailKelompokKegiatanKemahasiswaan.setKelompokKegiatanKemahasiswaan(kelompokKegiatanKemahasiswaan);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(KETUA);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(PENGURUS);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(ANGGOTA);
				session.save(detailKelompokKegiatanKemahasiswaan);

				detailKelompokKegiatanKemahasiswaan = new DetailKelompokKegiatanKemahasiswaan();
				detailKelompokKegiatanKemahasiswaan.setNama("Kepanitiaan/Tim, Tingkat Fakultas");
				detailKelompokKegiatanKemahasiswaan.setNomorUrut(8);
				detailKelompokKegiatanKemahasiswaan.setKelompokKegiatanKemahasiswaan(kelompokKegiatanKemahasiswaan);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(KETUA);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(PENGURUS);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(ANGGOTA);
				session.save(detailKelompokKegiatanKemahasiswaan);

				detailKelompokKegiatanKemahasiswaan = new DetailKelompokKegiatanKemahasiswaan();
				detailKelompokKegiatanKemahasiswaan.setNama("Kepanitiaan/Tim, Tingkat Jurusan/Prodi");
				detailKelompokKegiatanKemahasiswaan.setNomorUrut(9);
				detailKelompokKegiatanKemahasiswaan.setKelompokKegiatanKemahasiswaan(kelompokKegiatanKemahasiswaan);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(KETUA);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(PENGURUS);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(ANGGOTA);
				session.save(detailKelompokKegiatanKemahasiswaan);

				kelompokKegiatanKemahasiswaan = new KelompokKegiatanKemahasiswaan();
				kelompokKegiatanKemahasiswaan.setNomorUrut(8);
				kelompokKegiatanKemahasiswaan.setJenisKelompokKegiatanKemahasiswaan(penunjang);
				kelompokKegiatanKemahasiswaan.setNama("Kepemimpinan dan keorganisasian ekstra");
				kelompokKegiatanKemahasiswaan
						.setKeterangan("Kelompok Kegiatan Kemahasiswaan kepemimpinan dan keorganisasian ekstra");
				session.save(kelompokKegiatanKemahasiswaan);

				detailKelompokKegiatanKemahasiswaan = new DetailKelompokKegiatanKemahasiswaan();
				detailKelompokKegiatanKemahasiswaan.setNama("Pengurus Tingkat Nasional");
				detailKelompokKegiatanKemahasiswaan.setNomorUrut(1);
				detailKelompokKegiatanKemahasiswaan.setKelompokKegiatanKemahasiswaan(kelompokKegiatanKemahasiswaan);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(KETUA);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(PENGURUS);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(ANGGOTA);
				session.save(detailKelompokKegiatanKemahasiswaan);

				detailKelompokKegiatanKemahasiswaan = new DetailKelompokKegiatanKemahasiswaan();
				detailKelompokKegiatanKemahasiswaan
						.setNama("Pengurus Tk.Regional/Provinsi/Satu Tingkat Dibawah Nasional");
				detailKelompokKegiatanKemahasiswaan.setNomorUrut(2);
				detailKelompokKegiatanKemahasiswaan.setKelompokKegiatanKemahasiswaan(kelompokKegiatanKemahasiswaan);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(KETUA);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(PENGURUS);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(ANGGOTA);
				session.save(detailKelompokKegiatanKemahasiswaan);

				detailKelompokKegiatanKemahasiswaan = new DetailKelompokKegiatanKemahasiswaan();
				detailKelompokKegiatanKemahasiswaan
						.setNama("Pengurus tingkat kabupaten/kota/dua tingkat dibawah nasional");
				detailKelompokKegiatanKemahasiswaan.setNomorUrut(3);
				detailKelompokKegiatanKemahasiswaan.setKelompokKegiatanKemahasiswaan(kelompokKegiatanKemahasiswaan);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(KETUA);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(PENGURUS);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(ANGGOTA);
				session.save(detailKelompokKegiatanKemahasiswaan);

				detailKelompokKegiatanKemahasiswaan = new DetailKelompokKegiatanKemahasiswaan();
				detailKelompokKegiatanKemahasiswaan.setNama("Pengurus tk.perguruan tinggi/kecamatan");
				detailKelompokKegiatanKemahasiswaan.setNomorUrut(4);
				detailKelompokKegiatanKemahasiswaan.setKelompokKegiatanKemahasiswaan(kelompokKegiatanKemahasiswaan);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(KETUA);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(PENGURUS);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(ANGGOTA);
				session.save(detailKelompokKegiatanKemahasiswaan);

				detailKelompokKegiatanKemahasiswaan = new DetailKelompokKegiatanKemahasiswaan();
				detailKelompokKegiatanKemahasiswaan.setNama("Kepanitiaan Tk. Nasional");
				detailKelompokKegiatanKemahasiswaan.setNomorUrut(5);
				detailKelompokKegiatanKemahasiswaan.setKelompokKegiatanKemahasiswaan(kelompokKegiatanKemahasiswaan);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(KETUA);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(PENGURUS);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(ANGGOTA);
				session.save(detailKelompokKegiatanKemahasiswaan);

				detailKelompokKegiatanKemahasiswaan = new DetailKelompokKegiatanKemahasiswaan();
				detailKelompokKegiatanKemahasiswaan
						.setNama("Kepanitiaan tk.regional/provinsi/satu tingkat dibawah nasional");
				detailKelompokKegiatanKemahasiswaan.setNomorUrut(6);
				detailKelompokKegiatanKemahasiswaan.setKelompokKegiatanKemahasiswaan(kelompokKegiatanKemahasiswaan);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(KETUA);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(PENGURUS);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(ANGGOTA);
				session.save(detailKelompokKegiatanKemahasiswaan);

				detailKelompokKegiatanKemahasiswaan = new DetailKelompokKegiatanKemahasiswaan();
				detailKelompokKegiatanKemahasiswaan
						.setNama("Kepanitiaan tingkat kabupaten/kota/dua tingkat dibawah nasional");
				detailKelompokKegiatanKemahasiswaan.setNomorUrut(7);
				detailKelompokKegiatanKemahasiswaan.setKelompokKegiatanKemahasiswaan(kelompokKegiatanKemahasiswaan);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(KETUA);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(PENGURUS);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(ANGGOTA);
				session.save(detailKelompokKegiatanKemahasiswaan);

				detailKelompokKegiatanKemahasiswaan = new DetailKelompokKegiatanKemahasiswaan();
				detailKelompokKegiatanKemahasiswaan.setNama("Kepanitiaan tk.perguruan tinggi/kecamatan");
				detailKelompokKegiatanKemahasiswaan.setNomorUrut(7);
				detailKelompokKegiatanKemahasiswaan.setKelompokKegiatanKemahasiswaan(kelompokKegiatanKemahasiswaan);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(KETUA);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(PENGURUS);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(ANGGOTA);
				session.save(detailKelompokKegiatanKemahasiswaan);

				kelompokKegiatanKemahasiswaan = new KelompokKegiatanKemahasiswaan();
				kelompokKegiatanKemahasiswaan.setNomorUrut(9);
				kelompokKegiatanKemahasiswaan.setJenisKelompokKegiatanKemahasiswaan(penunjang);
				kelompokKegiatanKemahasiswaan.setNama("Pengabdian di luar kampus");
				kelompokKegiatanKemahasiswaan
						.setKeterangan("Kelompok Kegiatan Kemahasiswaan pengabdian di luar kampus");
				session.save(kelompokKegiatanKemahasiswaan);

				detailKelompokKegiatanKemahasiswaan = new DetailKelompokKegiatanKemahasiswaan();
				detailKelompokKegiatanKemahasiswaan.setNama(
						"Pelatih/Pembina/kegiatan keagamaan,kemasyarakatan,kepemudaan,kesiswaan/kemahasiswaan,atau keolahragaan dan seni");
				detailKelompokKegiatanKemahasiswaan.setNomorUrut(1);
				detailKelompokKegiatanKemahasiswaan.setKelompokKegiatanKemahasiswaan(kelompokKegiatanKemahasiswaan);

				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(TAHUN_1);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(SMESTER_1);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(BULAN_3);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(BULAN_2);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(BULAN_1);

				session.save(detailKelompokKegiatanKemahasiswaan);

				detailKelompokKegiatanKemahasiswaan = new DetailKelompokKegiatanKemahasiswaan();
				detailKelompokKegiatanKemahasiswaan.setNama("Penyuluhan, khutbah, dakwah");
				detailKelompokKegiatanKemahasiswaan.setNomorUrut(2);
				detailKelompokKegiatanKemahasiswaan.setKelompokKegiatanKemahasiswaan(kelompokKegiatanKemahasiswaan);
				detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans().add(KALI);
				session.save(detailKelompokKegiatanKemahasiswaan);

			}
			session.getTransaction().commit();
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/InitDataHelper.java:4417");
		} finally {
			// 2. WAJIB Tutup Session
			if (session != null && session.isOpen()) {
				// session.disconnect();
				if (session.isOpen()) {
					session.disconnect();
					session.close();
				}
			}
			HibernateUtil.closeSession();
		}
	}

	public static void initDataKegiatanKesiswan() {
		// 1. Buka Session Baru (Isolated Session)
		// Menggunakan openSession() agar terpisah dari session HTTP request
		Session session = HibernateUtil.getSessionFactory().openSession();

		try {

			int count = ((Number) session.createCriteria(SkalaKegiatanKesiswaan.class)
					.setProjection(Projections.rowCount()).uniqueResult()).intValue();
			session.getTransaction().begin();
			SkalaKegiatanKesiswaan fakJur = null;
			SkalaKegiatanKesiswaan Institut = null;
			SkalaKegiatanKesiswaan Regional = null;
			SkalaKegiatanKesiswaan Nasional = null;
			SkalaKegiatanKesiswaan Internasional = null;
			SkalaKegiatanKesiswaan TAHUN_1 = null;
			SkalaKegiatanKesiswaan SMESTER_1 = null;
			SkalaKegiatanKesiswaan BULAN_3 = null;
			SkalaKegiatanKesiswaan BULAN_2 = null;
			SkalaKegiatanKesiswaan BULAN_1 = null;
			SkalaKegiatanKesiswaan MUNGGU_2 = null;
			SkalaKegiatanKesiswaan HARI_10 = null;
			SkalaKegiatanKesiswaan HARI_9 = null;
			SkalaKegiatanKesiswaan HARI_5 = null;
			SkalaKegiatanKesiswaan HARI_2 = null;
			SkalaKegiatanKesiswaan DITAMPILKAN_DI_LUAR_KAMPUS = null;
			SkalaKegiatanKesiswaan DITAMPILKAN_DI_KAMPUS = null;
			SkalaKegiatanKesiswaan TIDAK_DITAMPILKAN = null;
			SkalaKegiatanKesiswaan Penerjemah = null;

			SkalaKegiatanKesiswaan Editor_Penerjemah = null;
			SkalaKegiatanKesiswaan Juru_tulis = null;
			SkalaKegiatanKesiswaan Pengarang_perorangan = null;
			SkalaKegiatanKesiswaan Pengarang_berkelompok = null;
			SkalaKegiatanKesiswaan Editor_buku = null;
			SkalaKegiatanKesiswaan Ketua_kelompok = null;
			SkalaKegiatanKesiswaan Anggota_kelompok = null;
			SkalaKegiatanKesiswaan Perorangan = null;
			SkalaKegiatanKesiswaan KETUA = null;
			SkalaKegiatanKesiswaan PENGURUS = null;
			SkalaKegiatanKesiswaan ANGGOTA = null;

			SkalaKegiatanKesiswaan KALI = null;

			if (count == 0) {
				fakJur = new SkalaKegiatanKesiswaan();
				fakJur.setNama("Fak./Jur");
				fakJur.setNomorUrut(5);
				fakJur.setKeterangan("Skala Kegiatan Kesiswaan Fak./Jur");
				session.save(fakJur);

				Institut = new SkalaKegiatanKesiswaan();
				Institut.setNama("Institut");
				Institut.setNomorUrut(4);
				Institut.setKeterangan("Skala Kegiatan Kesiswaan Institut");
				session.save(Institut);

				Regional = new SkalaKegiatanKesiswaan();
				Regional.setNama("Regional");
				Regional.setNomorUrut(3);
				Regional.setKeterangan("Skala Kegiatan Kesiswaan Regional");
				session.save(Regional);

				Nasional = new SkalaKegiatanKesiswaan();
				Nasional.setNama("Nasional");
				Nasional.setNomorUrut(2);
				Nasional.setKeterangan("Skala Kegiatan Kesiswaan Nasional");
				session.save(Nasional);

				Internasional = new SkalaKegiatanKesiswaan();
				Internasional.setNama("Internasional");
				Internasional.setNomorUrut(1);
				Internasional.setKeterangan("Skala Kegiatan Kesiswaan Internasional");
				session.save(Internasional);

				TAHUN_1 = new SkalaKegiatanKesiswaan();
				TAHUN_1.setNama("1 TAHUN");
				TAHUN_1.setNomorUrut(6);
				TAHUN_1.setKeterangan("Skala Kegiatan Kesiswaan 1 TAHUN (12 bulan)");
				session.save(TAHUN_1);

				SMESTER_1 = new SkalaKegiatanKesiswaan();
				SMESTER_1.setNama("1 SMESTER");
				SMESTER_1.setNomorUrut(7);
				SMESTER_1.setKeterangan("Skala Kegiatan Kesiswaan 6 bulan");
				session.save(SMESTER_1);

				BULAN_3 = new SkalaKegiatanKesiswaan();
				BULAN_3.setNama("3 BULAN - < 1 SMESTER");
				BULAN_3.setNomorUrut(8);
				BULAN_3.setKeterangan("Skala Kegiatan Kesiswaan 3 bulan");
				session.save(BULAN_3);

				BULAN_2 = new SkalaKegiatanKesiswaan();
				BULAN_2.setNama("2 - < 3 BULAN");
				BULAN_2.setNomorUrut(9);
				BULAN_2.setKeterangan("Skala Kegiatan Kesiswaan 2 bulan");
				session.save(BULAN_2);

				BULAN_1 = new SkalaKegiatanKesiswaan();
				BULAN_1.setNama("1 - < 2 BULAN");
				BULAN_1.setNomorUrut(10);
				BULAN_1.setKeterangan("Skala Kegiatan Kesiswaan 1 bulan");
				session.save(BULAN_1);

				MUNGGU_2 = new SkalaKegiatanKesiswaan();
				MUNGGU_2.setNama(">2 Minggu");
				MUNGGU_2.setNomorUrut(11);
				MUNGGU_2.setKeterangan("Skala Kegiatan Kesiswaan > 2 Minggu");
				session.save(MUNGGU_2);

				HARI_10 = new SkalaKegiatanKesiswaan();
				HARI_10.setNama("10 hari - 2 minggu");
				HARI_10.setNomorUrut(12);
				HARI_10.setKeterangan("Skala Kegiatan Kesiswaan 10 hari - 2 minggu");
				session.save(HARI_10);

				HARI_9 = new SkalaKegiatanKesiswaan();
				HARI_9.setNama("6 - 9 hari");
				HARI_9.setNomorUrut(13);
				HARI_9.setKeterangan("Skala Kegiatan Kesiswaan 6 - 9 hari");
				session.save(HARI_9);

				HARI_5 = new SkalaKegiatanKesiswaan();
				HARI_5.setNama("3 - 5 hari");
				HARI_5.setNomorUrut(14);
				HARI_5.setKeterangan("Skala Kegiatan Kesiswaan 3 - 5 hari");
				session.save(HARI_5);

				HARI_2 = new SkalaKegiatanKesiswaan();
				HARI_2.setNama("1-2 hari");
				HARI_2.setNomorUrut(15);
				HARI_2.setKeterangan("Skala Kegiatan Kesiswaan 3 - 5 hari");
				session.save(HARI_2);

				DITAMPILKAN_DI_LUAR_KAMPUS = new SkalaKegiatanKesiswaan();
				DITAMPILKAN_DI_LUAR_KAMPUS.setNama("DITAMPILKAN DI LUAR KAMPUS");
				DITAMPILKAN_DI_LUAR_KAMPUS.setNomorUrut(16);
				DITAMPILKAN_DI_LUAR_KAMPUS.setKeterangan("Skala Kegiatan Kesiswaan DITAMPILKAN DI LUAR KAMPUS");
				session.save(DITAMPILKAN_DI_LUAR_KAMPUS);

				DITAMPILKAN_DI_KAMPUS = new SkalaKegiatanKesiswaan();
				DITAMPILKAN_DI_KAMPUS.setNama("DITAMPILKAN DI KAMPUS");
				DITAMPILKAN_DI_KAMPUS.setNomorUrut(17);
				DITAMPILKAN_DI_KAMPUS.setKeterangan("Skala Kegiatan Kesiswaan DITAMPILKAN DI KAMPUS");
				session.save(DITAMPILKAN_DI_KAMPUS);

				TIDAK_DITAMPILKAN = new SkalaKegiatanKesiswaan();
				TIDAK_DITAMPILKAN.setNama("TIDAK DITAMPILKAN");
				TIDAK_DITAMPILKAN.setNomorUrut(18);
				TIDAK_DITAMPILKAN.setKeterangan("Skala Kegiatan Kesiswaan TIDAK DITAMPILKAN");
				session.save(TIDAK_DITAMPILKAN);

				Penerjemah = new SkalaKegiatanKesiswaan();
				Penerjemah.setNama("Penerjemah");
				Penerjemah.setNomorUrut(19);
				Penerjemah.setKeterangan("Skala Kegiatan Kesiswaan Penerjemah");
				session.save(Penerjemah);

				Editor_Penerjemah = new SkalaKegiatanKesiswaan();
				Editor_Penerjemah.setNama("Editor Penerjemah");
				Editor_Penerjemah.setNomorUrut(20);
				Editor_Penerjemah.setKeterangan("Skala Kegiatan Kesiswaan Editor");
				session.save(Editor_Penerjemah);

				Juru_tulis = new SkalaKegiatanKesiswaan();
				Juru_tulis.setNama("Juru tulis");
				Juru_tulis.setNomorUrut(21);
				Juru_tulis.setKeterangan("Skala Kegiatan Kesiswaan Juru tulis");
				session.save(Juru_tulis);

				Pengarang_perorangan = new SkalaKegiatanKesiswaan();
				Pengarang_perorangan.setNama("Pengarang perorangan");
				Pengarang_perorangan.setNomorUrut(22);
				Pengarang_perorangan.setKeterangan("Skala Kegiatan Kesiswaan Pengarang perorangan");
				session.save(Pengarang_perorangan);

				Pengarang_berkelompok = new SkalaKegiatanKesiswaan();
				Pengarang_berkelompok.setNama("Pengarang berkelompok");
				Pengarang_berkelompok.setNomorUrut(23);
				Pengarang_berkelompok.setKeterangan("Skala Kegiatan Kesiswaan Pengarang berkelompok");
				session.save(Pengarang_berkelompok);

				Editor_buku = new SkalaKegiatanKesiswaan();
				Editor_buku.setNama("Editor buku");
				Editor_buku.setNomorUrut(24);
				Editor_buku.setKeterangan("Skala Kegiatan Kesiswaan Editor buku");
				session.save(Editor_buku);

				Ketua_kelompok = new SkalaKegiatanKesiswaan();
				Ketua_kelompok.setNama("Ketua kelompok");
				Ketua_kelompok.setNomorUrut(25);
				Ketua_kelompok.setKeterangan("Skala Kegiatan Kesiswaan Ketua kelompok");
				session.save(Ketua_kelompok);

				Anggota_kelompok = new SkalaKegiatanKesiswaan();
				Anggota_kelompok.setNama("Anggota kelompok");
				Anggota_kelompok.setNomorUrut(26);
				Anggota_kelompok.setKeterangan("Skala Kegiatan Kesiswaan Ketua kelompok");
				session.save(Anggota_kelompok);

				Perorangan = new SkalaKegiatanKesiswaan();
				Perorangan.setNama("Perorangan");
				Perorangan.setNomorUrut(27);
				Perorangan.setKeterangan("Skala Kegiatan Kesiswaan Perorangan");
				session.save(Perorangan);

				KETUA = new SkalaKegiatanKesiswaan();
				KETUA.setNama("KETUA");
				KETUA.setNomorUrut(28);
				KETUA.setKeterangan("Skala Kegiatan Kesiswaan KETUA");
				session.save(KETUA);

				PENGURUS = new SkalaKegiatanKesiswaan();
				PENGURUS.setNama("PENGURUS/PANITIA INTI");
				PENGURUS.setNomorUrut(29);
				PENGURUS.setKeterangan("Skala Kegiatan Kesiswaan PENGURUS/PANITIA INTI");
				session.save(PENGURUS);

				ANGGOTA = new SkalaKegiatanKesiswaan();
				ANGGOTA.setNama("ANGGOTA PENGURUS/PANITIA LAINNYA");
				ANGGOTA.setNomorUrut(30);
				ANGGOTA.setKeterangan("Skala Kegiatan Kesiswaan ANGGOTA PENGURUS/PANITIA LAINNYA");
				session.save(ANGGOTA);

				KALI = new SkalaKegiatanKesiswaan();
				KALI.setNama("Setiap kali tampil");
				KALI.setNomorUrut(31);
				KALI.setKeterangan("Skala Kegiatan Kesiswaan setiap kali tampil");
				session.save(KALI);
			}

			count = ((Number) session.createCriteria(JabatanKegiatanKesiswaan.class)
					.setProjection(Projections.rowCount()).uniqueResult()).intValue();
			JabatanKegiatanKesiswaan panitia = null;
			JabatanKegiatanKesiswaan peserta = null;
			JabatanKegiatanKesiswaan narasumber = null;
			JabatanKegiatanKesiswaan juaraI = null;
			JabatanKegiatanKesiswaan juaraII = null;
			JabatanKegiatanKesiswaan juaraIII = null;
			JabatanKegiatanKesiswaan beregu = null;
			if (count == 0) {
				panitia = new JabatanKegiatanKesiswaan();
				panitia.setNama("Panitia");
				panitia.setNomorUrut(2);
				panitia.setKeterangan("Jabatan/Status/Tugas Kegiatan Kesiswaan Sebagai Panitia");
				session.save(panitia);

				peserta = new JabatanKegiatanKesiswaan();
				peserta.setNama("Peserta");
				peserta.setNomorUrut(1);
				peserta.setKeterangan("Jabatan/Status/Tugas Kegiatan Kesiswaan Sebagai Peserta");
				session.save(peserta);

				narasumber = new JabatanKegiatanKesiswaan();
				narasumber.setNama("Narasumber");
				narasumber.setNomorUrut(3);
				narasumber.setKeterangan("Jabatan/Status/Tugas Kegiatan Kesiswaan Sebagai Narasumber");
				session.save(narasumber);

				juaraI = new JabatanKegiatanKesiswaan();
				juaraI.setNama("Juara I");
				juaraI.setNomorUrut(4);
				juaraI.setKeterangan("Jabatan/Status/Tugas Kegiatan Kesiswaan Sebagai Juara I");
				session.save(juaraI);

				juaraII = new JabatanKegiatanKesiswaan();
				juaraII.setNama("Juara II");
				juaraII.setNomorUrut(5);
				juaraII.setKeterangan("Jabatan/Status/Tugas Kegiatan Kesiswaan Sebagai Juara II");
				session.save(juaraII);

				juaraIII = new JabatanKegiatanKesiswaan();
				juaraIII.setNama("Juara III");
				juaraIII.setNomorUrut(6);
				juaraIII.setKeterangan("Jabatan/Status/Tugas Kegiatan Kesiswaan Sebagai Juara II");
				session.save(juaraIII);

				beregu = new JabatanKegiatanKesiswaan();
				beregu.setNama("Beregu/perorangan");
				beregu.setNomorUrut(7);
				beregu.setKeterangan("Jabatan/Status/Tugas Kegiatan Kesiswaan Sebagai Beregu/perorangan");
				session.save(beregu);

			}

			JenisKelompokKegiatanKesiswaan utama = (JenisKelompokKegiatanKesiswaan) session
					.createCriteria(JenisKelompokKegiatanKesiswaan.class).add(Restrictions.eq("nama", "Kelompok Utama"))
					.setMaxResults(1).uniqueResult();

			if (utama == null) {
				utama = new JenisKelompokKegiatanKesiswaan();
				utama.setNama("Kelompok Utama");
				utama.setKeterangan("Jenis Kelompok Kegiatan Kesiswaan yang masuk Kelompok Utama");
				session.save(utama);
				session.flush();
			}

			JenisKelompokKegiatanKesiswaan penunjang = (JenisKelompokKegiatanKesiswaan) session
					.createCriteria(JenisKelompokKegiatanKesiswaan.class).add(Restrictions.eq("nama", "Kelompok Utama"))
					.setMaxResults(1).uniqueResult();
			if (penunjang == null) {

				penunjang = new JenisKelompokKegiatanKesiswaan();
				penunjang.setNama("Kelompok Penunjang");

				penunjang.setKeterangan("Jenis Kelompok Kegiatan Kesiswaan yang masuk Kelompok Penunjang");
				session.save(penunjang);
				session.flush();
			}

			System.out.println(utama + " " + penunjang);

			count = ((Number) session.createCriteria(KelompokKegiatanKesiswaan.class)
					.setProjection(Projections.rowCount()).uniqueResult()).intValue();
			if (count == 0) {

				KelompokKegiatanKesiswaan kelompokKegiatanKesiswaan = new KelompokKegiatanKesiswaan();
				kelompokKegiatanKesiswaan.setNama("Keagamaan dan moral pancasila");
				kelompokKegiatanKesiswaan.setNomorUrut(1);
				kelompokKegiatanKesiswaan.setJenisKelompokKegiatanKesiswaan(utama);
				kelompokKegiatanKesiswaan.setKeterangan("Kelompok Kegiatan Kesiswaan keagamaan dan moral pancasila");
				session.save(kelompokKegiatanKesiswaan);

				DetailKelompokKegiatanKesiswaan detailKelompokKegiatanKesiswaan = new DetailKelompokKegiatanKesiswaan();
				detailKelompokKegiatanKesiswaan.setNama("PHBI");
				detailKelompokKegiatanKesiswaan.setNomorUrut(1);
				detailKelompokKegiatanKesiswaan.setKelompokKegiatanKesiswaan(kelompokKegiatanKesiswaan);
				detailKelompokKegiatanKesiswaan.getJabatanKegiatanKesiswaans().add(panitia);
				detailKelompokKegiatanKesiswaan.getJabatanKegiatanKesiswaans().add(narasumber);

				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(Internasional);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(Nasional);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(Regional);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(Institut);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(fakJur);

				session.save(detailKelompokKegiatanKesiswaan);

				detailKelompokKegiatanKesiswaan = new DetailKelompokKegiatanKesiswaan();
				detailKelompokKegiatanKesiswaan.setNama("PHBN");
				detailKelompokKegiatanKesiswaan.setNomorUrut(2);
				detailKelompokKegiatanKesiswaan.setKelompokKegiatanKesiswaan(kelompokKegiatanKesiswaan);
				detailKelompokKegiatanKesiswaan.getJabatanKegiatanKesiswaans().add(panitia);
				detailKelompokKegiatanKesiswaan.getJabatanKegiatanKesiswaans().add(narasumber);

				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(Internasional);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(Nasional);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(Regional);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(Institut);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(fakJur);

				session.save(detailKelompokKegiatanKesiswaan);

				kelompokKegiatanKesiswaan = new KelompokKegiatanKesiswaan();
				kelompokKegiatanKesiswaan.setNomorUrut(2);
				kelompokKegiatanKesiswaan.setNama("Penalaran dan Ilmiah");
				kelompokKegiatanKesiswaan.setJenisKelompokKegiatanKesiswaan(utama);
				kelompokKegiatanKesiswaan.setKeterangan("Kelompok Kegiatan Kesiswaan penalaran dan Ilmiah");
				session.save(kelompokKegiatanKesiswaan);

				detailKelompokKegiatanKesiswaan = new DetailKelompokKegiatanKesiswaan();
				detailKelompokKegiatanKesiswaan
						.setNama("Diskusi,seminar,workshop,lokakarya,symposium dan ceramah ilmiah");
				detailKelompokKegiatanKesiswaan.setNomorUrut(1);
				detailKelompokKegiatanKesiswaan.setKelompokKegiatanKesiswaan(kelompokKegiatanKesiswaan);
				detailKelompokKegiatanKesiswaan.getJabatanKegiatanKesiswaans().add(panitia);
				detailKelompokKegiatanKesiswaan.getJabatanKegiatanKesiswaans().add(narasumber);
				detailKelompokKegiatanKesiswaan.getJabatanKegiatanKesiswaans().add(peserta);

				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(Internasional);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(Nasional);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(Regional);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(Institut);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(fakJur);

				session.save(detailKelompokKegiatanKesiswaan);

				detailKelompokKegiatanKesiswaan = new DetailKelompokKegiatanKesiswaan();
				detailKelompokKegiatanKesiswaan.setNama("Tulisan ilmiah/karya tulis");
				detailKelompokKegiatanKesiswaan.setNomorUrut(2);
				detailKelompokKegiatanKesiswaan.setKelompokKegiatanKesiswaan(kelompokKegiatanKesiswaan);

				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(Internasional);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(Nasional);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(Regional);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(Institut);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(fakJur);

				session.save(detailKelompokKegiatanKesiswaan);

				detailKelompokKegiatanKesiswaan = new DetailKelompokKegiatanKesiswaan();
				detailKelompokKegiatanKesiswaan.setNama("Lomba ilmiah (cerdas Cermat dan sejenisnya)");
				detailKelompokKegiatanKesiswaan.setNomorUrut(3);
				detailKelompokKegiatanKesiswaan.setKelompokKegiatanKesiswaan(kelompokKegiatanKesiswaan);

				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(Internasional);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(Nasional);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(Regional);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(Institut);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(fakJur);

				session.save(detailKelompokKegiatanKesiswaan);

				kelompokKegiatanKesiswaan = new KelompokKegiatanKesiswaan();
				kelompokKegiatanKesiswaan.setNomorUrut(3);
				kelompokKegiatanKesiswaan.setNama("Pelatihan");
				kelompokKegiatanKesiswaan.setJenisKelompokKegiatanKesiswaan(utama);
				kelompokKegiatanKesiswaan.setKeterangan("Kelompok Kegiatan Kesiswaan pelatihan");
				session.save(kelompokKegiatanKesiswaan);

				detailKelompokKegiatanKesiswaan = new DetailKelompokKegiatanKesiswaan();
				detailKelompokKegiatanKesiswaan.setNama("Pelatihan/penataran kegiatan ilmiah/akademik");
				detailKelompokKegiatanKesiswaan.setNomorUrut(1);
				detailKelompokKegiatanKesiswaan.setKelompokKegiatanKesiswaan(kelompokKegiatanKesiswaan);
				detailKelompokKegiatanKesiswaan.getJabatanKegiatanKesiswaans().add(panitia);
				detailKelompokKegiatanKesiswaan.getJabatanKegiatanKesiswaans().add(narasumber);
				detailKelompokKegiatanKesiswaan.getJabatanKegiatanKesiswaans().add(peserta);

				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(Internasional);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(Nasional);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(Regional);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(Institut);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(fakJur);

				session.save(detailKelompokKegiatanKesiswaan);

				detailKelompokKegiatanKesiswaan = new DetailKelompokKegiatanKesiswaan();
				detailKelompokKegiatanKesiswaan.setNama("Pelatihan/penataran kegiatan keagamaan");
				detailKelompokKegiatanKesiswaan.setNomorUrut(2);
				detailKelompokKegiatanKesiswaan.setKelompokKegiatanKesiswaan(kelompokKegiatanKesiswaan);
				detailKelompokKegiatanKesiswaan.getJabatanKegiatanKesiswaans().add(panitia);
				detailKelompokKegiatanKesiswaan.getJabatanKegiatanKesiswaans().add(narasumber);
				detailKelompokKegiatanKesiswaan.getJabatanKegiatanKesiswaans().add(peserta);

				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(Internasional);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(Nasional);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(Regional);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(Institut);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(fakJur);

				session.save(detailKelompokKegiatanKesiswaan);

				detailKelompokKegiatanKesiswaan = new DetailKelompokKegiatanKesiswaan();
				detailKelompokKegiatanKesiswaan.setNama("Pelatihan/penataran kesiswaan/kepemudaan");
				detailKelompokKegiatanKesiswaan.setNomorUrut(3);
				detailKelompokKegiatanKesiswaan.setKelompokKegiatanKesiswaan(kelompokKegiatanKesiswaan);
				detailKelompokKegiatanKesiswaan.getJabatanKegiatanKesiswaans().add(panitia);
				detailKelompokKegiatanKesiswaan.getJabatanKegiatanKesiswaans().add(narasumber);
				detailKelompokKegiatanKesiswaan.getJabatanKegiatanKesiswaans().add(peserta);

				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(Internasional);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(Nasional);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(Regional);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(Institut);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(fakJur);

				session.save(detailKelompokKegiatanKesiswaan);

				detailKelompokKegiatanKesiswaan = new DetailKelompokKegiatanKesiswaan();
				detailKelompokKegiatanKesiswaan
						.setNama("Pelatihan/penataran pembinaan karakter kebangsaan/nasionalisme");
				detailKelompokKegiatanKesiswaan.setNomorUrut(4);
				detailKelompokKegiatanKesiswaan.setKelompokKegiatanKesiswaan(kelompokKegiatanKesiswaan);
				detailKelompokKegiatanKesiswaan.getJabatanKegiatanKesiswaans().add(panitia);
				detailKelompokKegiatanKesiswaan.getJabatanKegiatanKesiswaans().add(narasumber);
				detailKelompokKegiatanKesiswaan.getJabatanKegiatanKesiswaans().add(peserta);

				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(Internasional);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(Nasional);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(Regional);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(Institut);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(fakJur);

				session.save(detailKelompokKegiatanKesiswaan);

				detailKelompokKegiatanKesiswaan = new DetailKelompokKegiatanKesiswaan();
				detailKelompokKegiatanKesiswaan.setNama("Pelatihan /penataran kegiatan keolahragaan");
				detailKelompokKegiatanKesiswaan.setNomorUrut(5);
				detailKelompokKegiatanKesiswaan.setKelompokKegiatanKesiswaan(kelompokKegiatanKesiswaan);
				detailKelompokKegiatanKesiswaan.getJabatanKegiatanKesiswaans().add(panitia);
				detailKelompokKegiatanKesiswaan.getJabatanKegiatanKesiswaans().add(narasumber);
				detailKelompokKegiatanKesiswaan.getJabatanKegiatanKesiswaans().add(peserta);

				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(Internasional);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(Nasional);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(Regional);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(Institut);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(fakJur);

				session.save(detailKelompokKegiatanKesiswaan);

				detailKelompokKegiatanKesiswaan = new DetailKelompokKegiatanKesiswaan();
				detailKelompokKegiatanKesiswaan.setNama("Pelatihan/penataran kegiatan seni");
				detailKelompokKegiatanKesiswaan.setNomorUrut(6);
				detailKelompokKegiatanKesiswaan.setKelompokKegiatanKesiswaan(kelompokKegiatanKesiswaan);
				detailKelompokKegiatanKesiswaan.getJabatanKegiatanKesiswaans().add(panitia);
				detailKelompokKegiatanKesiswaan.getJabatanKegiatanKesiswaans().add(narasumber);
				detailKelompokKegiatanKesiswaan.getJabatanKegiatanKesiswaans().add(peserta);

				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(Internasional);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(Nasional);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(Regional);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(Institut);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(fakJur);

				session.save(detailKelompokKegiatanKesiswaan);

				kelompokKegiatanKesiswaan = new KelompokKegiatanKesiswaan();
				kelompokKegiatanKesiswaan.setNomorUrut(4);
				kelompokKegiatanKesiswaan.setNama("Bakat dan minat");
				kelompokKegiatanKesiswaan.setJenisKelompokKegiatanKesiswaan(utama);
				kelompokKegiatanKesiswaan.setKeterangan("Kelompok Kegiatan Kesiswaan bakat dan minat");
				session.save(kelompokKegiatanKesiswaan);

				detailKelompokKegiatanKesiswaan = new DetailKelompokKegiatanKesiswaan();
				detailKelompokKegiatanKesiswaan.setNama("Keterlibatan pertandingan olah raga/seni");
				detailKelompokKegiatanKesiswaan.setNomorUrut(1);
				detailKelompokKegiatanKesiswaan.setKelompokKegiatanKesiswaan(kelompokKegiatanKesiswaan);
				detailKelompokKegiatanKesiswaan.getJabatanKegiatanKesiswaans().add(panitia);
				detailKelompokKegiatanKesiswaan.getJabatanKegiatanKesiswaans().add(peserta);

				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(Internasional);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(Nasional);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(Regional);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(Institut);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(fakJur);

				session.save(detailKelompokKegiatanKesiswaan);

				detailKelompokKegiatanKesiswaan = new DetailKelompokKegiatanKesiswaan();
				detailKelompokKegiatanKesiswaan.setNama("Kejuaraan olah raga/seni berkelompok/perorangan");
				detailKelompokKegiatanKesiswaan.setNomorUrut(2);
				detailKelompokKegiatanKesiswaan.setKelompokKegiatanKesiswaan(kelompokKegiatanKesiswaan);
				detailKelompokKegiatanKesiswaan.getJabatanKegiatanKesiswaans().add(juaraI);
				detailKelompokKegiatanKesiswaan.getJabatanKegiatanKesiswaans().add(juaraII);
				detailKelompokKegiatanKesiswaan.getJabatanKegiatanKesiswaans().add(juaraIII);

				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(Internasional);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(Nasional);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(Regional);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(Institut);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(fakJur);

				session.save(detailKelompokKegiatanKesiswaan);

				detailKelompokKegiatanKesiswaan = new DetailKelompokKegiatanKesiswaan();
				detailKelompokKegiatanKesiswaan.setNama("Pementasan olah raga dan seni");
				detailKelompokKegiatanKesiswaan.setNomorUrut(3);
				detailKelompokKegiatanKesiswaan.setKelompokKegiatanKesiswaan(kelompokKegiatanKesiswaan);
				detailKelompokKegiatanKesiswaan.getJabatanKegiatanKesiswaans().add(beregu);

				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(Internasional);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(Nasional);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(Regional);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(Institut);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(fakJur);

				session.save(detailKelompokKegiatanKesiswaan);

				kelompokKegiatanKesiswaan = new KelompokKegiatanKesiswaan();
				kelompokKegiatanKesiswaan.setNomorUrut(5);
				kelompokKegiatanKesiswaan.setNama("Pengabdian di lingkungan kampus");
				kelompokKegiatanKesiswaan.setJenisKelompokKegiatanKesiswaan(utama);
				kelompokKegiatanKesiswaan.setKeterangan("Kelompok Kegiatan Kesiswaan pengabdian di lingkungan kampus");
				session.save(kelompokKegiatanKesiswaan);

				detailKelompokKegiatanKesiswaan = new DetailKelompokKegiatanKesiswaan();
				detailKelompokKegiatanKesiswaan.setNama(
						"Pelatih/Pembina Kegiatan Keagamaan, Kemasyarakatan,Kepemudaan, Kesiswaan, Kesiswaan Atau Keolahragaan Dan Seni");
				detailKelompokKegiatanKesiswaan.setNomorUrut(1);
				detailKelompokKegiatanKesiswaan.setKelompokKegiatanKesiswaan(kelompokKegiatanKesiswaan);

				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(TAHUN_1);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(SMESTER_1);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(BULAN_3);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(BULAN_2);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(BULAN_1);

				session.save(detailKelompokKegiatanKesiswaan);

				detailKelompokKegiatanKesiswaan = new DetailKelompokKegiatanKesiswaan();
				detailKelompokKegiatanKesiswaan.setNama("Asisten Laboratorium");
				detailKelompokKegiatanKesiswaan.setNomorUrut(2);
				detailKelompokKegiatanKesiswaan.setKelompokKegiatanKesiswaan(kelompokKegiatanKesiswaan);

				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(TAHUN_1);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(SMESTER_1);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(BULAN_3);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(BULAN_2);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(BULAN_1);

				session.save(detailKelompokKegiatanKesiswaan);

				detailKelompokKegiatanKesiswaan = new DetailKelompokKegiatanKesiswaan();
				detailKelompokKegiatanKesiswaan
						.setNama("Bakti social atas penugasan institusi kampus dan lembaga kesiswaan");
				detailKelompokKegiatanKesiswaan.setNomorUrut(3);
				detailKelompokKegiatanKesiswaan.setKelompokKegiatanKesiswaan(kelompokKegiatanKesiswaan);

				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(MUNGGU_2);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(HARI_10);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(HARI_9);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(HARI_5);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(HARI_2);

				session.save(detailKelompokKegiatanKesiswaan);

				kelompokKegiatanKesiswaan = new KelompokKegiatanKesiswaan();
				kelompokKegiatanKesiswaan.setNomorUrut(6);
				kelompokKegiatanKesiswaan.setNama("Penalaran dan Ilmiah Lainnya");
				kelompokKegiatanKesiswaan.setJenisKelompokKegiatanKesiswaan(utama);
				kelompokKegiatanKesiswaan.setKeterangan("Kelompok Kegiatan Kesiswaan penalaran dan Ilmiah Lainnya");
				session.save(kelompokKegiatanKesiswaan);

				detailKelompokKegiatanKesiswaan = new DetailKelompokKegiatanKesiswaan();
				detailKelompokKegiatanKesiswaan.setNama("Karya seni");
				detailKelompokKegiatanKesiswaan.setNomorUrut(1);
				detailKelompokKegiatanKesiswaan.setKelompokKegiatanKesiswaan(kelompokKegiatanKesiswaan);

				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(DITAMPILKAN_DI_LUAR_KAMPUS);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(DITAMPILKAN_DI_KAMPUS);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(TIDAK_DITAMPILKAN);

				session.save(detailKelompokKegiatanKesiswaan);

				detailKelompokKegiatanKesiswaan = new DetailKelompokKegiatanKesiswaan();
				detailKelompokKegiatanKesiswaan.setNama("Karya tulis");
				detailKelompokKegiatanKesiswaan.setNomorUrut(2);
				detailKelompokKegiatanKesiswaan.setKelompokKegiatanKesiswaan(kelompokKegiatanKesiswaan);

				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(DITAMPILKAN_DI_LUAR_KAMPUS);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(DITAMPILKAN_DI_KAMPUS);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(TIDAK_DITAMPILKAN);

				session.save(detailKelompokKegiatanKesiswaan);

				detailKelompokKegiatanKesiswaan = new DetailKelompokKegiatanKesiswaan();
				detailKelompokKegiatanKesiswaan.setNama("Skenario");
				detailKelompokKegiatanKesiswaan.setNomorUrut(3);
				detailKelompokKegiatanKesiswaan.setKelompokKegiatanKesiswaan(kelompokKegiatanKesiswaan);

				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(DITAMPILKAN_DI_LUAR_KAMPUS);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(DITAMPILKAN_DI_KAMPUS);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(TIDAK_DITAMPILKAN);

				session.save(detailKelompokKegiatanKesiswaan);

				detailKelompokKegiatanKesiswaan = new DetailKelompokKegiatanKesiswaan();
				detailKelompokKegiatanKesiswaan.setNama("Penerjemahan buku");
				detailKelompokKegiatanKesiswaan.setNomorUrut(4);
				detailKelompokKegiatanKesiswaan.setKelompokKegiatanKesiswaan(kelompokKegiatanKesiswaan);

				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(Penerjemah);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(Editor_Penerjemah);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(Juru_tulis);

				session.save(detailKelompokKegiatanKesiswaan);

				detailKelompokKegiatanKesiswaan = new DetailKelompokKegiatanKesiswaan();
				detailKelompokKegiatanKesiswaan.setNama("Penerjemahan naskah");
				detailKelompokKegiatanKesiswaan.setNomorUrut(5);
				detailKelompokKegiatanKesiswaan.setKelompokKegiatanKesiswaan(kelompokKegiatanKesiswaan);

				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(Penerjemah);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(Editor_Penerjemah);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(Juru_tulis);

				session.save(detailKelompokKegiatanKesiswaan);

				detailKelompokKegiatanKesiswaan = new DetailKelompokKegiatanKesiswaan();
				detailKelompokKegiatanKesiswaan.setNama("Membuat buku");
				detailKelompokKegiatanKesiswaan.setNomorUrut(6);
				detailKelompokKegiatanKesiswaan.setKelompokKegiatanKesiswaan(kelompokKegiatanKesiswaan);

				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(Pengarang_perorangan);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(Pengarang_berkelompok);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(Editor_buku);

				session.save(detailKelompokKegiatanKesiswaan);

				detailKelompokKegiatanKesiswaan = new DetailKelompokKegiatanKesiswaan();
				detailKelompokKegiatanKesiswaan.setNama("Penelitian");
				detailKelompokKegiatanKesiswaan.setNomorUrut(7);
				detailKelompokKegiatanKesiswaan.setKelompokKegiatanKesiswaan(kelompokKegiatanKesiswaan);

				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(Ketua_kelompok);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(Anggota_kelompok);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(Perorangan);

				session.save(detailKelompokKegiatanKesiswaan);

				kelompokKegiatanKesiswaan = new KelompokKegiatanKesiswaan();
				kelompokKegiatanKesiswaan.setNomorUrut(7);
				kelompokKegiatanKesiswaan.setJenisKelompokKegiatanKesiswaan(penunjang);
				kelompokKegiatanKesiswaan.setNama("Kepemimpinan dan keorganisasian intra kampus");
				kelompokKegiatanKesiswaan
						.setKeterangan("Kelompok Kegiatan Kesiswaan Kepemimpinan dan keorganisasian intra kampus");
				session.save(kelompokKegiatanKesiswaan);

				detailKelompokKegiatanKesiswaan = new DetailKelompokKegiatanKesiswaan();
				detailKelompokKegiatanKesiswaan.setNama("Pengurus Dema-I/Sema-I");
				detailKelompokKegiatanKesiswaan.setNomorUrut(1);
				detailKelompokKegiatanKesiswaan.setKelompokKegiatanKesiswaan(kelompokKegiatanKesiswaan);

				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(KETUA);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(PENGURUS);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(ANGGOTA);

				session.save(detailKelompokKegiatanKesiswaan);

				detailKelompokKegiatanKesiswaan = new DetailKelompokKegiatanKesiswaan();
				detailKelompokKegiatanKesiswaan.setNama("Pengurus DEMA");
				detailKelompokKegiatanKesiswaan.setNomorUrut(2);
				detailKelompokKegiatanKesiswaan.setKelompokKegiatanKesiswaan(kelompokKegiatanKesiswaan);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(KETUA);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(PENGURUS);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(ANGGOTA);
				session.save(detailKelompokKegiatanKesiswaan);

				detailKelompokKegiatanKesiswaan = new DetailKelompokKegiatanKesiswaan();
				detailKelompokKegiatanKesiswaan.setNama("Pengurus HMJ");
				detailKelompokKegiatanKesiswaan.setNomorUrut(3);
				detailKelompokKegiatanKesiswaan.setKelompokKegiatanKesiswaan(kelompokKegiatanKesiswaan);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(KETUA);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(PENGURUS);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(ANGGOTA);
				session.save(detailKelompokKegiatanKesiswaan);

				detailKelompokKegiatanKesiswaan = new DetailKelompokKegiatanKesiswaan();
				detailKelompokKegiatanKesiswaan.setNama("Pengurus HMPS");
				detailKelompokKegiatanKesiswaan.setNomorUrut(4);
				detailKelompokKegiatanKesiswaan.setKelompokKegiatanKesiswaan(kelompokKegiatanKesiswaan);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(KETUA);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(PENGURUS);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(ANGGOTA);
				session.save(detailKelompokKegiatanKesiswaan);

				detailKelompokKegiatanKesiswaan = new DetailKelompokKegiatanKesiswaan();
				detailKelompokKegiatanKesiswaan.setNama("Pengurus UKM");
				detailKelompokKegiatanKesiswaan.setNomorUrut(5);
				detailKelompokKegiatanKesiswaan.setKelompokKegiatanKesiswaan(kelompokKegiatanKesiswaan);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(KETUA);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(PENGURUS);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(ANGGOTA);
				session.save(detailKelompokKegiatanKesiswaan);

				detailKelompokKegiatanKesiswaan = new DetailKelompokKegiatanKesiswaan();
				detailKelompokKegiatanKesiswaan.setNama("Pengurus UKK");
				detailKelompokKegiatanKesiswaan.setNomorUrut(6);
				detailKelompokKegiatanKesiswaan.setKelompokKegiatanKesiswaan(kelompokKegiatanKesiswaan);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(KETUA);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(PENGURUS);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(ANGGOTA);
				session.save(detailKelompokKegiatanKesiswaan);

				detailKelompokKegiatanKesiswaan = new DetailKelompokKegiatanKesiswaan();
				detailKelompokKegiatanKesiswaan.setNama("Kepanitiaan/Tim, Tingkat Institut");
				detailKelompokKegiatanKesiswaan.setNomorUrut(7);
				detailKelompokKegiatanKesiswaan.setKelompokKegiatanKesiswaan(kelompokKegiatanKesiswaan);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(KETUA);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(PENGURUS);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(ANGGOTA);
				session.save(detailKelompokKegiatanKesiswaan);

				detailKelompokKegiatanKesiswaan = new DetailKelompokKegiatanKesiswaan();
				detailKelompokKegiatanKesiswaan.setNama("Kepanitiaan/Tim, Tingkat Fakultas");
				detailKelompokKegiatanKesiswaan.setNomorUrut(8);
				detailKelompokKegiatanKesiswaan.setKelompokKegiatanKesiswaan(kelompokKegiatanKesiswaan);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(KETUA);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(PENGURUS);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(ANGGOTA);
				session.save(detailKelompokKegiatanKesiswaan);

				detailKelompokKegiatanKesiswaan = new DetailKelompokKegiatanKesiswaan();
				detailKelompokKegiatanKesiswaan.setNama("Kepanitiaan/Tim, Tingkat Jurusan/Prodi");
				detailKelompokKegiatanKesiswaan.setNomorUrut(9);
				detailKelompokKegiatanKesiswaan.setKelompokKegiatanKesiswaan(kelompokKegiatanKesiswaan);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(KETUA);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(PENGURUS);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(ANGGOTA);
				session.save(detailKelompokKegiatanKesiswaan);

				kelompokKegiatanKesiswaan = new KelompokKegiatanKesiswaan();
				kelompokKegiatanKesiswaan.setNomorUrut(8);
				kelompokKegiatanKesiswaan.setJenisKelompokKegiatanKesiswaan(penunjang);
				kelompokKegiatanKesiswaan.setNama("Kepemimpinan dan keorganisasian ekstra");
				kelompokKegiatanKesiswaan
						.setKeterangan("Kelompok Kegiatan Kesiswaan kepemimpinan dan keorganisasian ekstra");
				session.save(kelompokKegiatanKesiswaan);

				detailKelompokKegiatanKesiswaan = new DetailKelompokKegiatanKesiswaan();
				detailKelompokKegiatanKesiswaan.setNama("Pengurus Tingkat Nasional");
				detailKelompokKegiatanKesiswaan.setNomorUrut(1);
				detailKelompokKegiatanKesiswaan.setKelompokKegiatanKesiswaan(kelompokKegiatanKesiswaan);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(KETUA);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(PENGURUS);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(ANGGOTA);
				session.save(detailKelompokKegiatanKesiswaan);

				detailKelompokKegiatanKesiswaan = new DetailKelompokKegiatanKesiswaan();
				detailKelompokKegiatanKesiswaan.setNama("Pengurus Tk.Regional/Provinsi/Satu Tingkat Dibawah Nasional");
				detailKelompokKegiatanKesiswaan.setNomorUrut(2);
				detailKelompokKegiatanKesiswaan.setKelompokKegiatanKesiswaan(kelompokKegiatanKesiswaan);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(KETUA);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(PENGURUS);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(ANGGOTA);
				session.save(detailKelompokKegiatanKesiswaan);

				detailKelompokKegiatanKesiswaan = new DetailKelompokKegiatanKesiswaan();
				detailKelompokKegiatanKesiswaan.setNama("Pengurus tingkat kabupaten/kota/dua tingkat dibawah nasional");
				detailKelompokKegiatanKesiswaan.setNomorUrut(3);
				detailKelompokKegiatanKesiswaan.setKelompokKegiatanKesiswaan(kelompokKegiatanKesiswaan);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(KETUA);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(PENGURUS);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(ANGGOTA);
				session.save(detailKelompokKegiatanKesiswaan);

				detailKelompokKegiatanKesiswaan = new DetailKelompokKegiatanKesiswaan();
				detailKelompokKegiatanKesiswaan.setNama("Pengurus tk.perguruan tinggi/kecamatan");
				detailKelompokKegiatanKesiswaan.setNomorUrut(4);
				detailKelompokKegiatanKesiswaan.setKelompokKegiatanKesiswaan(kelompokKegiatanKesiswaan);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(KETUA);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(PENGURUS);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(ANGGOTA);
				session.save(detailKelompokKegiatanKesiswaan);

				detailKelompokKegiatanKesiswaan = new DetailKelompokKegiatanKesiswaan();
				detailKelompokKegiatanKesiswaan.setNama("Kepanitiaan Tk. Nasional");
				detailKelompokKegiatanKesiswaan.setNomorUrut(5);
				detailKelompokKegiatanKesiswaan.setKelompokKegiatanKesiswaan(kelompokKegiatanKesiswaan);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(KETUA);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(PENGURUS);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(ANGGOTA);
				session.save(detailKelompokKegiatanKesiswaan);

				detailKelompokKegiatanKesiswaan = new DetailKelompokKegiatanKesiswaan();
				detailKelompokKegiatanKesiswaan
						.setNama("Kepanitiaan tk.regional/provinsi/satu tingkat dibawah nasional");
				detailKelompokKegiatanKesiswaan.setNomorUrut(6);
				detailKelompokKegiatanKesiswaan.setKelompokKegiatanKesiswaan(kelompokKegiatanKesiswaan);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(KETUA);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(PENGURUS);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(ANGGOTA);
				session.save(detailKelompokKegiatanKesiswaan);

				detailKelompokKegiatanKesiswaan = new DetailKelompokKegiatanKesiswaan();
				detailKelompokKegiatanKesiswaan
						.setNama("Kepanitiaan tingkat kabupaten/kota/dua tingkat dibawah nasional");
				detailKelompokKegiatanKesiswaan.setNomorUrut(7);
				detailKelompokKegiatanKesiswaan.setKelompokKegiatanKesiswaan(kelompokKegiatanKesiswaan);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(KETUA);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(PENGURUS);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(ANGGOTA);
				session.save(detailKelompokKegiatanKesiswaan);

				detailKelompokKegiatanKesiswaan = new DetailKelompokKegiatanKesiswaan();
				detailKelompokKegiatanKesiswaan.setNama("Kepanitiaan tk.perguruan tinggi/kecamatan");
				detailKelompokKegiatanKesiswaan.setNomorUrut(7);
				detailKelompokKegiatanKesiswaan.setKelompokKegiatanKesiswaan(kelompokKegiatanKesiswaan);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(KETUA);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(PENGURUS);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(ANGGOTA);
				session.save(detailKelompokKegiatanKesiswaan);

				kelompokKegiatanKesiswaan = new KelompokKegiatanKesiswaan();
				kelompokKegiatanKesiswaan.setNomorUrut(9);
				kelompokKegiatanKesiswaan.setJenisKelompokKegiatanKesiswaan(penunjang);
				kelompokKegiatanKesiswaan.setNama("Pengabdian di luar kampus");
				kelompokKegiatanKesiswaan.setKeterangan("Kelompok Kegiatan Kesiswaan pengabdian di luar kampus");
				session.save(kelompokKegiatanKesiswaan);

				detailKelompokKegiatanKesiswaan = new DetailKelompokKegiatanKesiswaan();
				detailKelompokKegiatanKesiswaan.setNama(
						"Pelatih/Pembina/kegiatan keagamaan,kemasyarakatan,kepemudaan,kesiswaan/kesiswaan,atau keolahragaan dan seni");
				detailKelompokKegiatanKesiswaan.setNomorUrut(1);
				detailKelompokKegiatanKesiswaan.setKelompokKegiatanKesiswaan(kelompokKegiatanKesiswaan);

				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(TAHUN_1);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(SMESTER_1);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(BULAN_3);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(BULAN_2);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(BULAN_1);

				session.save(detailKelompokKegiatanKesiswaan);

				detailKelompokKegiatanKesiswaan = new DetailKelompokKegiatanKesiswaan();
				detailKelompokKegiatanKesiswaan.setNama("Penyuluhan, khutbah, dakwah");
				detailKelompokKegiatanKesiswaan.setNomorUrut(2);
				detailKelompokKegiatanKesiswaan.setKelompokKegiatanKesiswaan(kelompokKegiatanKesiswaan);
				detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans().add(KALI);
				session.save(detailKelompokKegiatanKesiswaan);

			}
			session.getTransaction().commit();
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/InitDataHelper.java:5327");
		} finally {
			// 2. WAJIB Tutup Session
			if (session != null && session.isOpen()) {
				// session.disconnect();
				if (session.isOpen()) {
					session.disconnect();
					session.close();
				}
			}
			HibernateUtil.closeSession();
		}

	}

	public static void initDataKegiatanKedosenan() {
		// 1. Buka Session Baru (Isolated Session)
		// Menggunakan openSession() agar terpisah dari session HTTP request
		Session session = HibernateUtil.getSessionFactory().openSession();

		try {
			session.getTransaction().begin();
			int count = ((Number) session.createCriteria(SkalaKegiatanKedosenan.class)
					.setProjection(Projections.rowCount()).uniqueResult()).intValue();
			session.getTransaction().begin();
			SkalaKegiatanKedosenan fakJur = (SkalaKegiatanKedosenan) session
					.createCriteria(SkalaKegiatanKedosenan.class).add(Restrictions.eq("nama", "Fak./Jur"))
					.setMaxResults(1).uniqueResult();
			SkalaKegiatanKedosenan Institut = (SkalaKegiatanKedosenan) session
					.createCriteria(SkalaKegiatanKedosenan.class).add(Restrictions.eq("nama", "Institut"))
					.setMaxResults(1).uniqueResult();
			SkalaKegiatanKedosenan Regional = (SkalaKegiatanKedosenan) session
					.createCriteria(SkalaKegiatanKedosenan.class).add(Restrictions.eq("nama", "Regional"))
					.setMaxResults(1).uniqueResult();
			SkalaKegiatanKedosenan Nasional = (SkalaKegiatanKedosenan) session
					.createCriteria(SkalaKegiatanKedosenan.class).add(Restrictions.eq("nama", "Nasional"))
					.setMaxResults(1).uniqueResult();
			SkalaKegiatanKedosenan Internasional = (SkalaKegiatanKedosenan) session
					.createCriteria(SkalaKegiatanKedosenan.class).add(Restrictions.eq("nama", "Internasional"))
					.setMaxResults(1).uniqueResult();

			SkalaKegiatanKedosenan KALI = null;

			if (count == 0) {
				fakJur = new SkalaKegiatanKedosenan();
				fakJur.setNama("Fak./Jur");
				fakJur.setNomorUrut(5);
				fakJur.setKeterangan("Skala Kegiatan Dosen Fak./Jur");
				session.save(fakJur);

				Institut = new SkalaKegiatanKedosenan();
				Institut.setNama("Institut");
				Institut.setNomorUrut(4);
				Institut.setKeterangan("Skala Kegiatan Dosen Institut");
				session.save(Institut);

				Regional = new SkalaKegiatanKedosenan();
				Regional.setNama("Regional");
				Regional.setNomorUrut(3);
				Regional.setKeterangan("Skala Kegiatan Dosen Regional");
				session.save(Regional);

				Nasional = new SkalaKegiatanKedosenan();
				Nasional.setNama("Nasional");
				Nasional.setNomorUrut(2);
				Nasional.setKeterangan("Skala Kegiatan Dosen Nasional");
				session.save(Nasional);

				Internasional = new SkalaKegiatanKedosenan();
				Internasional.setNama("Internasional");
				Internasional.setNomorUrut(1);
				Internasional.setKeterangan("Skala Kegiatan Dosen Internasional");
				session.save(Internasional);

				KALI = new SkalaKegiatanKedosenan();
				KALI.setNama("Setiap kali tampil");
				KALI.setNomorUrut(31);
				KALI.setKeterangan("Skala Kegiatan Dosen setiap kali tampil");
				session.save(KALI);
			}

			count = ((Number) session.createCriteria(JabatanKegiatanKedosenan.class)
					.setProjection(Projections.rowCount()).uniqueResult()).intValue();
			JabatanKegiatanKedosenan peserta = (JabatanKegiatanKedosenan) session
					.createCriteria(JabatanKegiatanKedosenan.class).add(Restrictions.eq("nama", "Peserta"))
					.setMaxResults(1).uniqueResult();
			JabatanKegiatanKedosenan narasumber = (JabatanKegiatanKedosenan) session
					.createCriteria(JabatanKegiatanKedosenan.class).add(Restrictions.eq("nama", "Narasumber"))
					.setMaxResults(1).uniqueResult();

			if (count == 0) {

				peserta = new JabatanKegiatanKedosenan();
				peserta.setNama("Peserta");
				peserta.setNomorUrut(1);
				peserta.setKeterangan("Jabatan/Status/Tugas Kegiatan Dosen Sebagai Peserta");
				session.save(peserta);

				narasumber = new JabatanKegiatanKedosenan();
				narasumber.setNama("Narasumber");
				narasumber.setNomorUrut(3);
				narasumber.setKeterangan("Jabatan/Status/Tugas Kegiatan Dosen Sebagai Narasumber");
				session.save(narasumber);

			}

			JabatanKegiatanKedosenan mandiri = (JabatanKegiatanKedosenan) session
					.createCriteria(JabatanKegiatanKedosenan.class).add(Restrictions.eq("nama", "Mandiri"))
					.setMaxResults(1).uniqueResult();
			if (mandiri == null) {

				mandiri = new JabatanKegiatanKedosenan();
				mandiri.setNama("Mandiri");
				mandiri.setNomorUrut(10);
				mandiri.setKeterangan("Jabatan/Status/Tugas Kegiatan Dosen Mandiri");
				session.save(mandiri);
			}

			JabatanKegiatanKedosenan pembina = (JabatanKegiatanKedosenan) session
					.createCriteria(JabatanKegiatanKedosenan.class).add(Restrictions.eq("nama", "Pembina"))
					.setMaxResults(1).uniqueResult();
			if (pembina == null) {

				pembina = new JabatanKegiatanKedosenan();
				pembina.setNama("Pembina");
				pembina.setNomorUrut(14);
				pembina.setKeterangan("Jabatan/Status/Tugas Kegiatan Dosen Pembina");
				session.save(pembina);
			}

			JabatanKegiatanKedosenan pembimbing = (JabatanKegiatanKedosenan) session
					.createCriteria(JabatanKegiatanKedosenan.class).add(Restrictions.eq("nama", "Pembimbing"))
					.setMaxResults(1).uniqueResult();
			if (pembimbing == null) {

				pembimbing = new JabatanKegiatanKedosenan();
				pembimbing.setNama("Pembimbing");
				pembimbing.setNomorUrut(15);
				pembimbing.setKeterangan("Jabatan/Status/Tugas Kegiatan Dosen Pembimbing");
				session.save(pembimbing);
			}

			JabatanKegiatanKedosenan ketuaTim = (JabatanKegiatanKedosenan) session
					.createCriteria(JabatanKegiatanKedosenan.class).add(Restrictions.eq("nama", "Ketua Tim"))
					.setMaxResults(1).uniqueResult();
			if (ketuaTim == null) {

				ketuaTim = new JabatanKegiatanKedosenan();
				ketuaTim.setNama("Ketua Tim");
				ketuaTim.setNomorUrut(11);
				ketuaTim.setKeterangan("Jabatan/Status/Tugas Kegiatan Dosen sebagai Ketua Tim");
				session.save(ketuaTim);
			}

			count = ((Number) session.createCriteria(JabatanKegiatanKedosenan.class)
					.add(Restrictions.eq("nama", "Ketua")).setProjection(Projections.rowCount()).uniqueResult())
					.intValue();
			JabatanKegiatanKedosenan ketua = (JabatanKegiatanKedosenan) session
					.createCriteria(JabatanKegiatanKedosenan.class).add(Restrictions.eq("nama", "Ketua"))
					.setMaxResults(1).uniqueResult();
			JabatanKegiatanKedosenan wakilKetua = (JabatanKegiatanKedosenan) session
					.createCriteria(JabatanKegiatanKedosenan.class).add(Restrictions.eq("nama", "Wakil Ketua"))
					.setMaxResults(1).uniqueResult();
			JabatanKegiatanKedosenan anggota = (JabatanKegiatanKedosenan) session
					.createCriteria(JabatanKegiatanKedosenan.class).add(Restrictions.eq("nama", "Anggota"))
					.setMaxResults(1).uniqueResult();

			if (count == 0) {

				ketua = new JabatanKegiatanKedosenan();
				ketua.setNama("Ketua");
				ketua.setNomorUrut(10);
				ketua.setKeterangan("Jabatan/Status/Tugas Kegiatan Dosen Sebagai Ketua");
				session.save(ketua);

				wakilKetua = new JabatanKegiatanKedosenan();
				wakilKetua.setNama("Wakil Ketua");
				wakilKetua.setNomorUrut(11);
				wakilKetua.setKeterangan("Jabatan/Status/Tugas Kegiatan Dosen Sebagai Wakil Ketua");
				session.save(wakilKetua);

				anggota = new JabatanKegiatanKedosenan();
				anggota.setNama("Anggota");
				anggota.setNomorUrut(11);
				anggota.setKeterangan("Jabatan/Status/Tugas Kegiatan Dosen Sebagai Anggota");
				session.save(anggota);

			}

			JabatanKegiatanKedosenan penulisUtama = (JabatanKegiatanKedosenan) session
					.createCriteria(JabatanKegiatanKedosenan.class).add(Restrictions.eq("nama", "Penulis"))
					.setMaxResults(1).uniqueResult();

			if (penulisUtama == null) {

				penulisUtama = new JabatanKegiatanKedosenan();
				penulisUtama.setNama("Penulis");
				penulisUtama.setNomorUrut(0);
				penulisUtama.setKeterangan("Jabatan/Status/Tugas Kegiatan Dosen Sebagai Penulis");
				session.save(penulisUtama);

			}

			JenisKelompokKegiatanKedosenan utama = (JenisKelompokKegiatanKedosenan) session
					.createCriteria(JenisKelompokKegiatanKedosenan.class).add(Restrictions.eq("nama", "Kelompok Utama"))
					.setMaxResults(1).uniqueResult();
			JenisKelompokKegiatanKedosenan penunjang = null;
			if (utama == null) {
				utama = new JenisKelompokKegiatanKedosenan();
				utama.setNama("Kelompok Utama");
				utama.setKeterangan("Jenis Kelompok Kegiatan Dosen yang masuk Kelompok Utama");
				session.save(utama);

				penunjang = new JenisKelompokKegiatanKedosenan();
				penunjang.setNama("Kelompok Penunjang");

				penunjang.setKeterangan("Jenis Kelompok Kegiatan Dosen yang masuk Kelompok Penunjang");
				session.save(penunjang);
			}

			count = ((Number) session.createCriteria(KelompokKegiatanKedosenan.class)
					.setProjection(Projections.rowCount()).uniqueResult()).intValue();
			if (count == 0) {

				KelompokKegiatanKedosenan kelompokKegiatanKedosenan = new KelompokKegiatanKedosenan();
				kelompokKegiatanKedosenan.setNomorUrut(2);
				kelompokKegiatanKedosenan.setNama("Penalaran dan Ilmiah");
				kelompokKegiatanKedosenan.setJenisKelompokKegiatanKedosenan(utama);
				kelompokKegiatanKedosenan.setKeterangan("Kelompok Kegiatan Dosen penalaran dan Ilmiah");
				session.save(kelompokKegiatanKedosenan);

				DetailKelompokKegiatanKedosenan detailKelompokKegiatanKedosenan = new DetailKelompokKegiatanKedosenan();
				detailKelompokKegiatanKedosenan.setNama("Seminar ilmiah");
				detailKelompokKegiatanKedosenan.setNomorUrut(1);
				detailKelompokKegiatanKedosenan.setKelompokKegiatanKedosenan(kelompokKegiatanKedosenan);
				detailKelompokKegiatanKedosenan.getJabatanKegiatanKedosenans().add(narasumber);
				detailKelompokKegiatanKedosenan.getJabatanKegiatanKedosenans().add(peserta);

				detailKelompokKegiatanKedosenan.getSkalaKegiatanKedosenans().add(Internasional);
				detailKelompokKegiatanKedosenan.getSkalaKegiatanKedosenans().add(Nasional);
				detailKelompokKegiatanKedosenan.getSkalaKegiatanKedosenans().add(Regional);
				detailKelompokKegiatanKedosenan.getSkalaKegiatanKedosenans().add(Institut);
				detailKelompokKegiatanKedosenan.getSkalaKegiatanKedosenans().add(fakJur);

				session.save(detailKelompokKegiatanKedosenan);

				detailKelompokKegiatanKedosenan = new DetailKelompokKegiatanKedosenan();
				detailKelompokKegiatanKedosenan.setNama("Lokakarya");
				detailKelompokKegiatanKedosenan.setNomorUrut(2);
				detailKelompokKegiatanKedosenan.setKelompokKegiatanKedosenan(kelompokKegiatanKedosenan);
				detailKelompokKegiatanKedosenan.getJabatanKegiatanKedosenans().add(narasumber);
				detailKelompokKegiatanKedosenan.getJabatanKegiatanKedosenans().add(peserta);

				detailKelompokKegiatanKedosenan.getSkalaKegiatanKedosenans().add(Internasional);
				detailKelompokKegiatanKedosenan.getSkalaKegiatanKedosenans().add(Nasional);
				detailKelompokKegiatanKedosenan.getSkalaKegiatanKedosenans().add(Regional);
				detailKelompokKegiatanKedosenan.getSkalaKegiatanKedosenans().add(Institut);
				detailKelompokKegiatanKedosenan.getSkalaKegiatanKedosenans().add(fakJur);

				session.save(detailKelompokKegiatanKedosenan);

				detailKelompokKegiatanKedosenan = new DetailKelompokKegiatanKedosenan();
				detailKelompokKegiatanKedosenan.setNama("Penataran/Pelatihan");
				detailKelompokKegiatanKedosenan.setNomorUrut(3);
				detailKelompokKegiatanKedosenan.setKelompokKegiatanKedosenan(kelompokKegiatanKedosenan);
				detailKelompokKegiatanKedosenan.getJabatanKegiatanKedosenans().add(narasumber);
				detailKelompokKegiatanKedosenan.getJabatanKegiatanKedosenans().add(peserta);

				detailKelompokKegiatanKedosenan.getSkalaKegiatanKedosenans().add(Internasional);
				detailKelompokKegiatanKedosenan.getSkalaKegiatanKedosenans().add(Nasional);
				detailKelompokKegiatanKedosenan.getSkalaKegiatanKedosenans().add(Regional);
				detailKelompokKegiatanKedosenan.getSkalaKegiatanKedosenans().add(Institut);
				detailKelompokKegiatanKedosenan.getSkalaKegiatanKedosenans().add(fakJur);

				session.save(detailKelompokKegiatanKedosenan);

				detailKelompokKegiatanKedosenan = new DetailKelompokKegiatanKedosenan();
				detailKelompokKegiatanKedosenan.setNama("Pagelaran");
				detailKelompokKegiatanKedosenan.setNomorUrut(4);
				detailKelompokKegiatanKedosenan.setKelompokKegiatanKedosenan(kelompokKegiatanKedosenan);
				detailKelompokKegiatanKedosenan.getJabatanKegiatanKedosenans().add(narasumber);
				detailKelompokKegiatanKedosenan.getJabatanKegiatanKedosenans().add(peserta);

				detailKelompokKegiatanKedosenan.getSkalaKegiatanKedosenans().add(Internasional);
				detailKelompokKegiatanKedosenan.getSkalaKegiatanKedosenans().add(Nasional);
				detailKelompokKegiatanKedosenan.getSkalaKegiatanKedosenans().add(Regional);
				detailKelompokKegiatanKedosenan.getSkalaKegiatanKedosenans().add(Institut);
				detailKelompokKegiatanKedosenan.getSkalaKegiatanKedosenans().add(fakJur);

				session.save(detailKelompokKegiatanKedosenan);

				detailKelompokKegiatanKedosenan = new DetailKelompokKegiatanKedosenan();
				detailKelompokKegiatanKedosenan.setNama("Pameran");
				detailKelompokKegiatanKedosenan.setNomorUrut(5);
				detailKelompokKegiatanKedosenan.setKelompokKegiatanKedosenan(kelompokKegiatanKedosenan);
				detailKelompokKegiatanKedosenan.getJabatanKegiatanKedosenans().add(narasumber);
				detailKelompokKegiatanKedosenan.getJabatanKegiatanKedosenans().add(peserta);

				detailKelompokKegiatanKedosenan.getSkalaKegiatanKedosenans().add(Internasional);
				detailKelompokKegiatanKedosenan.getSkalaKegiatanKedosenans().add(Nasional);
				detailKelompokKegiatanKedosenan.getSkalaKegiatanKedosenans().add(Regional);
				detailKelompokKegiatanKedosenan.getSkalaKegiatanKedosenans().add(Institut);
				detailKelompokKegiatanKedosenan.getSkalaKegiatanKedosenans().add(fakJur);

				session.save(detailKelompokKegiatanKedosenan);

				detailKelompokKegiatanKedosenan = new DetailKelompokKegiatanKedosenan();
				detailKelompokKegiatanKedosenan.setNama("Peragaan");
				detailKelompokKegiatanKedosenan.setNomorUrut(6);
				detailKelompokKegiatanKedosenan.setKelompokKegiatanKedosenan(kelompokKegiatanKedosenan);
				detailKelompokKegiatanKedosenan.getJabatanKegiatanKedosenans().add(narasumber);
				detailKelompokKegiatanKedosenan.getJabatanKegiatanKedosenans().add(peserta);

				detailKelompokKegiatanKedosenan.getSkalaKegiatanKedosenans().add(Internasional);
				detailKelompokKegiatanKedosenan.getSkalaKegiatanKedosenans().add(Nasional);
				detailKelompokKegiatanKedosenan.getSkalaKegiatanKedosenans().add(Regional);
				detailKelompokKegiatanKedosenan.getSkalaKegiatanKedosenans().add(Institut);
				detailKelompokKegiatanKedosenan.getSkalaKegiatanKedosenans().add(fakJur);

				session.save(detailKelompokKegiatanKedosenan);

				kelompokKegiatanKedosenan = new KelompokKegiatanKedosenan();
				kelompokKegiatanKedosenan.setNomorUrut(3);
				kelompokKegiatanKedosenan.setNama("Pelatihan");
				kelompokKegiatanKedosenan.setJenisKelompokKegiatanKedosenan(utama);
				kelompokKegiatanKedosenan.setKeterangan("Kelompok Kegiatan Dosen pelatihan");
				session.save(kelompokKegiatanKedosenan);

				detailKelompokKegiatanKedosenan = new DetailKelompokKegiatanKedosenan();
				detailKelompokKegiatanKedosenan.setNama("Pelatihan/penataran kegiatan ilmiah/akademik");
				detailKelompokKegiatanKedosenan.setNomorUrut(1);
				detailKelompokKegiatanKedosenan.setKelompokKegiatanKedosenan(kelompokKegiatanKedosenan);
				detailKelompokKegiatanKedosenan.getJabatanKegiatanKedosenans().add(narasumber);
				detailKelompokKegiatanKedosenan.getJabatanKegiatanKedosenans().add(peserta);

				detailKelompokKegiatanKedosenan.getSkalaKegiatanKedosenans().add(Internasional);
				detailKelompokKegiatanKedosenan.getSkalaKegiatanKedosenans().add(Nasional);
				detailKelompokKegiatanKedosenan.getSkalaKegiatanKedosenans().add(Regional);
				detailKelompokKegiatanKedosenan.getSkalaKegiatanKedosenans().add(Institut);
				detailKelompokKegiatanKedosenan.getSkalaKegiatanKedosenans().add(fakJur);

				session.save(detailKelompokKegiatanKedosenan);

				detailKelompokKegiatanKedosenan = new DetailKelompokKegiatanKedosenan();
				detailKelompokKegiatanKedosenan.setNama("Pelatihan/penataran kegiatan keagamaan");
				detailKelompokKegiatanKedosenan.setNomorUrut(2);
				detailKelompokKegiatanKedosenan.setKelompokKegiatanKedosenan(kelompokKegiatanKedosenan);
				detailKelompokKegiatanKedosenan.getJabatanKegiatanKedosenans().add(narasumber);
				detailKelompokKegiatanKedosenan.getJabatanKegiatanKedosenans().add(peserta);

				detailKelompokKegiatanKedosenan.getSkalaKegiatanKedosenans().add(Internasional);
				detailKelompokKegiatanKedosenan.getSkalaKegiatanKedosenans().add(Nasional);
				detailKelompokKegiatanKedosenan.getSkalaKegiatanKedosenans().add(Regional);
				detailKelompokKegiatanKedosenan.getSkalaKegiatanKedosenans().add(Institut);
				detailKelompokKegiatanKedosenan.getSkalaKegiatanKedosenans().add(fakJur);

				session.save(detailKelompokKegiatanKedosenan);

				detailKelompokKegiatanKedosenan = new DetailKelompokKegiatanKedosenan();
				detailKelompokKegiatanKedosenan.setNama("Pelatihan/penataran/kepemudaan");
				detailKelompokKegiatanKedosenan.setNomorUrut(3);
				detailKelompokKegiatanKedosenan.setKelompokKegiatanKedosenan(kelompokKegiatanKedosenan);
				detailKelompokKegiatanKedosenan.getJabatanKegiatanKedosenans().add(narasumber);
				detailKelompokKegiatanKedosenan.getJabatanKegiatanKedosenans().add(peserta);

				detailKelompokKegiatanKedosenan.getSkalaKegiatanKedosenans().add(Internasional);
				detailKelompokKegiatanKedosenan.getSkalaKegiatanKedosenans().add(Nasional);
				detailKelompokKegiatanKedosenan.getSkalaKegiatanKedosenans().add(Regional);
				detailKelompokKegiatanKedosenan.getSkalaKegiatanKedosenans().add(Institut);
				detailKelompokKegiatanKedosenan.getSkalaKegiatanKedosenans().add(fakJur);

				session.save(detailKelompokKegiatanKedosenan);

				detailKelompokKegiatanKedosenan = new DetailKelompokKegiatanKedosenan();
				detailKelompokKegiatanKedosenan
						.setNama("Pelatihan/penataran pembinaan karakter kebangsaan/nasionalisme");
				detailKelompokKegiatanKedosenan.setNomorUrut(4);
				detailKelompokKegiatanKedosenan.setKelompokKegiatanKedosenan(kelompokKegiatanKedosenan);
				detailKelompokKegiatanKedosenan.getJabatanKegiatanKedosenans().add(narasumber);
				detailKelompokKegiatanKedosenan.getJabatanKegiatanKedosenans().add(peserta);

				detailKelompokKegiatanKedosenan.getSkalaKegiatanKedosenans().add(Internasional);
				detailKelompokKegiatanKedosenan.getSkalaKegiatanKedosenans().add(Nasional);
				detailKelompokKegiatanKedosenan.getSkalaKegiatanKedosenans().add(Regional);
				detailKelompokKegiatanKedosenan.getSkalaKegiatanKedosenans().add(Institut);
				detailKelompokKegiatanKedosenan.getSkalaKegiatanKedosenans().add(fakJur);

				session.save(detailKelompokKegiatanKedosenan);

				detailKelompokKegiatanKedosenan = new DetailKelompokKegiatanKedosenan();
				detailKelompokKegiatanKedosenan.setNama("Pelatihan /penataran kegiatan keolahragaan");
				detailKelompokKegiatanKedosenan.setNomorUrut(5);
				detailKelompokKegiatanKedosenan.setKelompokKegiatanKedosenan(kelompokKegiatanKedosenan);
				detailKelompokKegiatanKedosenan.getJabatanKegiatanKedosenans().add(narasumber);
				detailKelompokKegiatanKedosenan.getJabatanKegiatanKedosenans().add(peserta);

				detailKelompokKegiatanKedosenan.getSkalaKegiatanKedosenans().add(Internasional);
				detailKelompokKegiatanKedosenan.getSkalaKegiatanKedosenans().add(Nasional);
				detailKelompokKegiatanKedosenan.getSkalaKegiatanKedosenans().add(Regional);
				detailKelompokKegiatanKedosenan.getSkalaKegiatanKedosenans().add(Institut);
				detailKelompokKegiatanKedosenan.getSkalaKegiatanKedosenans().add(fakJur);

				session.save(detailKelompokKegiatanKedosenan);

				detailKelompokKegiatanKedosenan = new DetailKelompokKegiatanKedosenan();
				detailKelompokKegiatanKedosenan.setNama("Pelatihan/penataran kegiatan seni");
				detailKelompokKegiatanKedosenan.setNomorUrut(6);
				detailKelompokKegiatanKedosenan.setKelompokKegiatanKedosenan(kelompokKegiatanKedosenan);
				detailKelompokKegiatanKedosenan.getJabatanKegiatanKedosenans().add(narasumber);
				detailKelompokKegiatanKedosenan.getJabatanKegiatanKedosenans().add(peserta);

				detailKelompokKegiatanKedosenan.getSkalaKegiatanKedosenans().add(Internasional);
				detailKelompokKegiatanKedosenan.getSkalaKegiatanKedosenans().add(Nasional);
				detailKelompokKegiatanKedosenan.getSkalaKegiatanKedosenans().add(Regional);
				detailKelompokKegiatanKedosenan.getSkalaKegiatanKedosenans().add(Institut);
				detailKelompokKegiatanKedosenan.getSkalaKegiatanKedosenans().add(fakJur);

				session.save(detailKelompokKegiatanKedosenan);

				kelompokKegiatanKedosenan = new KelompokKegiatanKedosenan();
				kelompokKegiatanKedosenan.setNomorUrut(4);
				kelompokKegiatanKedosenan.setNama("Bakat dan minat");
				kelompokKegiatanKedosenan.setJenisKelompokKegiatanKedosenan(utama);
				kelompokKegiatanKedosenan.setKeterangan("Kelompok Kegiatan Dosen bakat dan minat");
				session.save(kelompokKegiatanKedosenan);

				detailKelompokKegiatanKedosenan = new DetailKelompokKegiatanKedosenan();
				detailKelompokKegiatanKedosenan.setNama("Keterlibatan pertandingan olah raga/seni");
				detailKelompokKegiatanKedosenan.setNomorUrut(1);
				detailKelompokKegiatanKedosenan.setKelompokKegiatanKedosenan(kelompokKegiatanKedosenan);
				detailKelompokKegiatanKedosenan.getJabatanKegiatanKedosenans().add(peserta);

				detailKelompokKegiatanKedosenan.getSkalaKegiatanKedosenans().add(Internasional);
				detailKelompokKegiatanKedosenan.getSkalaKegiatanKedosenans().add(Nasional);
				detailKelompokKegiatanKedosenan.getSkalaKegiatanKedosenans().add(Regional);
				detailKelompokKegiatanKedosenan.getSkalaKegiatanKedosenans().add(Institut);
				detailKelompokKegiatanKedosenan.getSkalaKegiatanKedosenans().add(fakJur);

				session.save(detailKelompokKegiatanKedosenan);

			}

			count = ((Number) session.createCriteria(KelompokKegiatanKedosenan.class)
					.add(Restrictions.eq("nama", "Panitia / Badan pada Lembaga Pemerintah"))
					.setProjection(Projections.rowCount()).uniqueResult()).intValue();
			if (count == 0) {

				KelompokKegiatanKedosenan kelompokKegiatanKedosenan = new KelompokKegiatanKedosenan();
				kelompokKegiatanKedosenan.setNomorUrut(122);
				kelompokKegiatanKedosenan.setNama("Panitia / Badan pada Lembaga Pemerintah");
				kelompokKegiatanKedosenan.setJenisKelompokKegiatanKedosenan(utama);
				kelompokKegiatanKedosenan
						.setKeterangan("Kelompok Kegiatan Dosen menjadi Panitia / Badan pada Lembaga Pemerintah");
				session.save(kelompokKegiatanKedosenan);

				DetailKelompokKegiatanKedosenan detailKelompokKegiatanKedosenan = new DetailKelompokKegiatanKedosenan();
				detailKelompokKegiatanKedosenan.setNama("Panitia Pusat");
				detailKelompokKegiatanKedosenan.setNomorUrut(1);
				detailKelompokKegiatanKedosenan.setKelompokKegiatanKedosenan(kelompokKegiatanKedosenan);
				detailKelompokKegiatanKedosenan.getJabatanKegiatanKedosenans().add(ketua);
				detailKelompokKegiatanKedosenan.getJabatanKegiatanKedosenans().add(wakilKetua);
				detailKelompokKegiatanKedosenan.getJabatanKegiatanKedosenans().add(anggota);

				detailKelompokKegiatanKedosenan.getSkalaKegiatanKedosenans().add(Internasional);
				detailKelompokKegiatanKedosenan.getSkalaKegiatanKedosenans().add(Nasional);
				detailKelompokKegiatanKedosenan.getSkalaKegiatanKedosenans().add(Regional);

				session.save(detailKelompokKegiatanKedosenan);

				detailKelompokKegiatanKedosenan = new DetailKelompokKegiatanKedosenan();
				detailKelompokKegiatanKedosenan.setNama("Panitia Daerah");
				detailKelompokKegiatanKedosenan.setNomorUrut(2);
				detailKelompokKegiatanKedosenan.setKelompokKegiatanKedosenan(kelompokKegiatanKedosenan);
				detailKelompokKegiatanKedosenan.getJabatanKegiatanKedosenans().add(ketua);
				detailKelompokKegiatanKedosenan.getJabatanKegiatanKedosenans().add(wakilKetua);
				detailKelompokKegiatanKedosenan.getJabatanKegiatanKedosenans().add(anggota);

				detailKelompokKegiatanKedosenan.getSkalaKegiatanKedosenans().add(Internasional);
				detailKelompokKegiatanKedosenan.getSkalaKegiatanKedosenans().add(Nasional);
				detailKelompokKegiatanKedosenan.getSkalaKegiatanKedosenans().add(Regional);

				session.save(detailKelompokKegiatanKedosenan);
			}

			count = ((Number) session.createCriteria(KelompokKegiatanKedosenan.class).add(Restrictions.eq("nama",
					"Menulis artikel, kritik, opini dan sebagainya pada media massa (koran/majalah populer/umum)"))
					.setProjection(Projections.rowCount()).uniqueResult()).intValue();
			if (count == 0) {

				KelompokKegiatanKedosenan kelompokKegiatanKedosenan = new KelompokKegiatanKedosenan();
				kelompokKegiatanKedosenan.setNomorUrut(123);
				kelompokKegiatanKedosenan.setNama(
						"Menulis artikel, kritik, opini dan sebagainya pada media massa (koran/majalah populer/umum)");
				kelompokKegiatanKedosenan.setJenisKelompokKegiatanKedosenan(utama);
				kelompokKegiatanKedosenan.setKeterangan(
						"Kelompok Kegiatan Dosen menjadi penulis artikel, kritik, opini dan sebagainya pada media massa (koran/majalah populer/umum)");
				session.save(kelompokKegiatanKedosenan);

				DetailKelompokKegiatanKedosenan detailKelompokKegiatanKedosenan = new DetailKelompokKegiatanKedosenan();
				detailKelompokKegiatanKedosenan.setNama("Penulis");
				detailKelompokKegiatanKedosenan.setNomorUrut(1);
				detailKelompokKegiatanKedosenan.setKelompokKegiatanKedosenan(kelompokKegiatanKedosenan);
				detailKelompokKegiatanKedosenan.getJabatanKegiatanKedosenans().add(penulisUtama);

				detailKelompokKegiatanKedosenan.getSkalaKegiatanKedosenans().add(Internasional);
				detailKelompokKegiatanKedosenan.getSkalaKegiatanKedosenans().add(Nasional);
				detailKelompokKegiatanKedosenan.getSkalaKegiatanKedosenans().add(Regional);

				session.save(detailKelompokKegiatanKedosenan);
			}

			count = ((Number) session.createCriteria(KelompokKegiatanKedosenan.class).add(Restrictions.eq("nama",
					"Melakukan penelitian atau hasil pemikiran yang tidak dipublikasikan (tersimpan di perpustakaan perguruan tinggi)"))
					.setProjection(Projections.rowCount()).uniqueResult()).intValue();
			if (count == 0) {

				KelompokKegiatanKedosenan kelompokKegiatanKedosenan = new KelompokKegiatanKedosenan();
				kelompokKegiatanKedosenan.setNomorUrut(125);
				kelompokKegiatanKedosenan.setNama(
						"Melakukan penelitian atau hasil pemikiran yang tidak dipublikasikan (tersimpan di perpustakaan perguruan tinggi)");
				kelompokKegiatanKedosenan.setJenisKelompokKegiatanKedosenan(utama);
				kelompokKegiatanKedosenan.setKeterangan(
						"Kelompok Kegiatan Dosen melakukan penelitian atau hasil pemikiran yang tidak dipublikasikan (tersimpan di perpustakaan perguruan tinggi)");
				session.save(kelompokKegiatanKedosenan);

				DetailKelompokKegiatanKedosenan detailKelompokKegiatanKedosenan = new DetailKelompokKegiatanKedosenan();
				detailKelompokKegiatanKedosenan.setNama("Peneliti atau pemikir");
				detailKelompokKegiatanKedosenan.setNomorUrut(1);
				detailKelompokKegiatanKedosenan.setKelompokKegiatanKedosenan(kelompokKegiatanKedosenan);
				detailKelompokKegiatanKedosenan.getJabatanKegiatanKedosenans().add(ketua);
				detailKelompokKegiatanKedosenan.getJabatanKegiatanKedosenans().add(anggota);

				detailKelompokKegiatanKedosenan.getSkalaKegiatanKedosenans().add(Internasional);
				detailKelompokKegiatanKedosenan.getSkalaKegiatanKedosenans().add(Nasional);
				detailKelompokKegiatanKedosenan.getSkalaKegiatanKedosenans().add(Regional);

				session.save(detailKelompokKegiatanKedosenan);
			}

			count = ((Number) session.createCriteria(KelompokKegiatanKedosenan.class)
					.add(Restrictions.eq("nama", "Menduduki Jabatan Pimpinan")).setProjection(Projections.rowCount())
					.uniqueResult()).intValue();
			if (count == 0) {

				KelompokKegiatanKedosenan kelompokKegiatanKedosenan = new KelompokKegiatanKedosenan();
				kelompokKegiatanKedosenan.setNomorUrut(126);
				kelompokKegiatanKedosenan.setNama("Menduduki Jabatan Pimpinan");
				kelompokKegiatanKedosenan.setJenisKelompokKegiatanKedosenan(utama);
				kelompokKegiatanKedosenan.setKeterangan(
						"Menduduki jabatan pimpinan pada lembaga pemerintahan/pejabat negara yang harus dibebaskan dari jabatan organiknya");
				session.save(kelompokKegiatanKedosenan);

				DetailKelompokKegiatanKedosenan detailKelompokKegiatanKedosenan = new DetailKelompokKegiatanKedosenan();
				detailKelompokKegiatanKedosenan
						.setNama("Menduduki jabatan pimpinan pada lembaga pemerintahan/pejabat negara");
				detailKelompokKegiatanKedosenan.setNomorUrut(1);
				detailKelompokKegiatanKedosenan.setKelompokKegiatanKedosenan(kelompokKegiatanKedosenan);
				detailKelompokKegiatanKedosenan.getJabatanKegiatanKedosenans().add(ketua);

				detailKelompokKegiatanKedosenan.getSkalaKegiatanKedosenans().add(Nasional);
				detailKelompokKegiatanKedosenan.getSkalaKegiatanKedosenans().add(Regional);

				session.save(detailKelompokKegiatanKedosenan);
			}

			count = ((Number) session.createCriteria(KelompokKegiatanKedosenan.class)
					.add(Restrictions.eq("nama", "Memberi latihan / penyuluhan / penataran / ceramah pada masyarakat"))
					.setProjection(Projections.rowCount()).uniqueResult()).intValue();
			if (count == 0) {

				KelompokKegiatanKedosenan kelompokKegiatanKedosenan = new KelompokKegiatanKedosenan();
				kelompokKegiatanKedosenan.setNomorUrut(126);
				kelompokKegiatanKedosenan.setNama("Memberi latihan / penyuluhan / penataran / ceramah pada masyarakat");
				kelompokKegiatanKedosenan.setJenisKelompokKegiatanKedosenan(utama);
				kelompokKegiatanKedosenan.setKeterangan(
						"Kelompok Kegiatan Dosen yang memberikan latihan / penyuluhan / penataran / ceramah pada masyarakat");
				session.save(kelompokKegiatanKedosenan);

				DetailKelompokKegiatanKedosenan detailKelompokKegiatanKedosenan = new DetailKelompokKegiatanKedosenan();
				detailKelompokKegiatanKedosenan.setNama("Terjadwal/terprogram dalam satu semester atau lebih");
				detailKelompokKegiatanKedosenan.setNomorUrut(1);
				detailKelompokKegiatanKedosenan.setKelompokKegiatanKedosenan(kelompokKegiatanKedosenan);
				detailKelompokKegiatanKedosenan.getJabatanKegiatanKedosenans().add(narasumber);

				detailKelompokKegiatanKedosenan.getSkalaKegiatanKedosenans().add(Internasional);
				detailKelompokKegiatanKedosenan.getSkalaKegiatanKedosenans().add(Nasional);
				detailKelompokKegiatanKedosenan.getSkalaKegiatanKedosenans().add(Regional);
				detailKelompokKegiatanKedosenan.getSkalaKegiatanKedosenans().add(Institut);
				detailKelompokKegiatanKedosenan.getSkalaKegiatanKedosenans().add(fakJur);
				session.save(detailKelompokKegiatanKedosenan);

				detailKelompokKegiatanKedosenan = new DetailKelompokKegiatanKedosenan();
				detailKelompokKegiatanKedosenan.setNama("Terjadwal/terprogram kurang dalam satu semester atau lebih");
				detailKelompokKegiatanKedosenan.setNomorUrut(2);
				detailKelompokKegiatanKedosenan.setKelompokKegiatanKedosenan(kelompokKegiatanKedosenan);
				detailKelompokKegiatanKedosenan.getJabatanKegiatanKedosenans().add(narasumber);

				detailKelompokKegiatanKedosenan.getSkalaKegiatanKedosenans().add(Internasional);
				detailKelompokKegiatanKedosenan.getSkalaKegiatanKedosenans().add(Nasional);
				detailKelompokKegiatanKedosenan.getSkalaKegiatanKedosenans().add(Regional);
				detailKelompokKegiatanKedosenan.getSkalaKegiatanKedosenans().add(Institut);
				detailKelompokKegiatanKedosenan.getSkalaKegiatanKedosenans().add(fakJur);

				session.save(detailKelompokKegiatanKedosenan);
			}

			count = ((Number) session.createCriteria(KelompokKegiatanKedosenan.class)
					.add(Restrictions.eq("nama",
							"Memberikan jasa konsultan yang relevan dengan kepakarannya dan disetujui oleh pimpinan"))
					.setProjection(Projections.rowCount()).uniqueResult()).intValue();
			if (count == 0) {

				KelompokKegiatanKedosenan kelompokKegiatanKedosenan = new KelompokKegiatanKedosenan();
				kelompokKegiatanKedosenan.setNomorUrut(128);
				kelompokKegiatanKedosenan.setNama(
						"Memberikan jasa konsultan yang relevan dengan kepakarannya dan disetujui oleh pimpinan");
				kelompokKegiatanKedosenan.setJenisKelompokKegiatanKedosenan(utama);
				kelompokKegiatanKedosenan.setKeterangan(
						"Memberikan jasa konsultan yang relevan dengan kepakarannya dan disetujui oleh pimpinan");
				session.save(kelompokKegiatanKedosenan);

				DetailKelompokKegiatanKedosenan detailKelompokKegiatanKedosenan = new DetailKelompokKegiatanKedosenan();
				detailKelompokKegiatanKedosenan.setNama("Memberikan jasa konsultan");
				detailKelompokKegiatanKedosenan.setNomorUrut(1);
				detailKelompokKegiatanKedosenan.setKelompokKegiatanKedosenan(kelompokKegiatanKedosenan);
				detailKelompokKegiatanKedosenan.getJabatanKegiatanKedosenans().add(mandiri);
				detailKelompokKegiatanKedosenan.getJabatanKegiatanKedosenans().add(ketuaTim);
				detailKelompokKegiatanKedosenan.getJabatanKegiatanKedosenans().add(anggota);

				detailKelompokKegiatanKedosenan.getSkalaKegiatanKedosenans().add(Internasional);
				detailKelompokKegiatanKedosenan.getSkalaKegiatanKedosenans().add(Nasional);
				detailKelompokKegiatanKedosenan.getSkalaKegiatanKedosenans().add(Regional);
				detailKelompokKegiatanKedosenan.getSkalaKegiatanKedosenans().add(Institut);
				detailKelompokKegiatanKedosenan.getSkalaKegiatanKedosenans().add(fakJur);

				session.save(detailKelompokKegiatanKedosenan);
			}

			count = ((Number) session.createCriteria(KelompokKegiatanKedosenan.class)
					.add(Restrictions.eq("nama", "Membina kegiatan mahasiswa")).setProjection(Projections.rowCount())
					.uniqueResult()).intValue();
			if (count == 0) {

				KelompokKegiatanKedosenan kelompokKegiatanKedosenan = new KelompokKegiatanKedosenan();
				kelompokKegiatanKedosenan.setNomorUrut(129);
				kelompokKegiatanKedosenan.setNama("Membina kegiatan mahasiswa");
				kelompokKegiatanKedosenan.setJenisKelompokKegiatanKedosenan(utama);
				kelompokKegiatanKedosenan
						.setKeterangan("Melakukan pembinaan kegiatan mahasiswa di bidang Akademik dan kemahasiswaan");
				session.save(kelompokKegiatanKedosenan);

				DetailKelompokKegiatanKedosenan detailKelompokKegiatanKedosenan = new DetailKelompokKegiatanKedosenan();
				detailKelompokKegiatanKedosenan.setNama("Kegiatan mahasiswa di bidang Akademik dan kemahasiswaan");
				detailKelompokKegiatanKedosenan.setNomorUrut(1);
				detailKelompokKegiatanKedosenan.setKelompokKegiatanKedosenan(kelompokKegiatanKedosenan);
				detailKelompokKegiatanKedosenan.getJabatanKegiatanKedosenans().add(pembina);

				detailKelompokKegiatanKedosenan.getSkalaKegiatanKedosenans().add(Internasional);
				detailKelompokKegiatanKedosenan.getSkalaKegiatanKedosenans().add(Nasional);
				detailKelompokKegiatanKedosenan.getSkalaKegiatanKedosenans().add(Regional);
				detailKelompokKegiatanKedosenan.getSkalaKegiatanKedosenans().add(Institut);
				detailKelompokKegiatanKedosenan.getSkalaKegiatanKedosenans().add(fakJur);

				session.save(detailKelompokKegiatanKedosenan);
			}

			count = ((Number) session.createCriteria(KelompokKegiatanKedosenan.class)
					.add(Restrictions.eq("nama", "Membimbing Akademik Dosen")).setProjection(Projections.rowCount())
					.uniqueResult()).intValue();
			if (count == 0) {

				KelompokKegiatanKedosenan kelompokKegiatanKedosenan = new KelompokKegiatanKedosenan();
				kelompokKegiatanKedosenan.setNomorUrut(130);
				kelompokKegiatanKedosenan.setNama("Membimbing Akademik Dosen");
				kelompokKegiatanKedosenan.setJenisKelompokKegiatanKedosenan(utama);
				kelompokKegiatanKedosenan
						.setKeterangan("Melakukan pembimbingan akademik dosen yang lebih rendah jabatannya");
				session.save(kelompokKegiatanKedosenan);

				DetailKelompokKegiatanKedosenan detailKelompokKegiatanKedosenan = new DetailKelompokKegiatanKedosenan();
				detailKelompokKegiatanKedosenan.setNama("Kegiatan Membimbing Akademik Dosen");
				detailKelompokKegiatanKedosenan.setNomorUrut(1);
				detailKelompokKegiatanKedosenan.setKelompokKegiatanKedosenan(kelompokKegiatanKedosenan);
				detailKelompokKegiatanKedosenan.getJabatanKegiatanKedosenans().add(pembimbing);

				detailKelompokKegiatanKedosenan.getSkalaKegiatanKedosenans().add(Institut);
				detailKelompokKegiatanKedosenan.getSkalaKegiatanKedosenans().add(fakJur);

				session.save(detailKelompokKegiatanKedosenan);
			}

			JabatanKegiatanKedosenan Rektor = (JabatanKegiatanKedosenan) session
					.createCriteria(JabatanKegiatanKedosenan.class).add(Restrictions.eq("nama", "Rektor"))
					.setMaxResults(1).uniqueResult();
			if (Rektor == null) {

				Rektor = new JabatanKegiatanKedosenan();
				Rektor.setNama("Rektor");
				Rektor.setNomorUrut(40);
				Rektor.setKeterangan("Jabatan/Status/Tugas Rektor");
				session.save(Rektor);
			}

			JabatanKegiatanKedosenan WakilRektor = (JabatanKegiatanKedosenan) session
					.createCriteria(JabatanKegiatanKedosenan.class).add(Restrictions.eq("nama", "Wakil Rektor"))
					.setMaxResults(1).uniqueResult();
			if (WakilRektor == null) {

				WakilRektor = new JabatanKegiatanKedosenan();
				WakilRektor.setNama("Wakil Rektor");
				WakilRektor.setNomorUrut(41);
				WakilRektor.setKeterangan("Jabatan/Status/Tugas Wakil Rektor");
				session.save(WakilRektor);
			}

			JabatanKegiatanKedosenan Dekan = (JabatanKegiatanKedosenan) session
					.createCriteria(JabatanKegiatanKedosenan.class)
					.add(Restrictions.eq("nama", "Dekan, Direktur Pascasarjana, Ketua Lembaga, Ketua Senat"))
					.setMaxResults(1).uniqueResult();
			if (Dekan == null) {
				Dekan = new JabatanKegiatanKedosenan();
				Dekan.setNama("Dekan, Direktur Pascasarjana, Ketua Lembaga, Ketua Senat");
				Dekan.setNomorUrut(42);
				Dekan.setKeterangan("Jabatan/Status/Tugas Dekan, Direktur Pascasarjana, Ketua Lembaga, Ketua Senat");
				session.save(Dekan);
			}

			JabatanKegiatanKedosenan WakilDekan = (JabatanKegiatanKedosenan) session
					.createCriteria(JabatanKegiatanKedosenan.class)
					.add(Restrictions.eq("nama",
							"Wakil Direktur, Wakil Dekan, Sekretaris Lembaga, Sekretaris Senat Institut"))
					.setMaxResults(1).uniqueResult();
			if (WakilDekan == null) {
				WakilDekan = new JabatanKegiatanKedosenan();
				WakilDekan.setNama("Wakil Direktur, Wakil Dekan, Sekretaris Lembaga, Sekretaris Senat Institut");
				WakilDekan.setNomorUrut(43);
				WakilDekan.setKeterangan(
						"Jabatan/Status/Tugas Wakil Direktur, Wakil Dekan, Sekretaris Lembaga, Sekretaris Senat Institut");
				session.save(WakilDekan);
			}

			JabatanKegiatanKedosenan KAJUR = (JabatanKegiatanKedosenan) session
					.createCriteria(JabatanKegiatanKedosenan.class)
					.add(Restrictions.eq("nama",
							"Kepala Pusat, Ketua Jurusan/Prodi, Kepala Unit, Ketua Senat Fakultas, Sekretaris Jurusan"))
					.setMaxResults(1).uniqueResult();
			if (KAJUR == null) {
				KAJUR = new JabatanKegiatanKedosenan();
				KAJUR.setNama(
						"Kepala Pusat, Ketua Jurusan/Prodi, Kepala Unit, Ketua Senat Fakultas, Sekretaris Jurusan");
				KAJUR.setNomorUrut(44);
				KAJUR.setKeterangan(
						"Jabatan/Status/Tugas Kepala Pusat, Ketua Jurusan/Prodi, Kepala Unit, Ketua Senat Fakultas, Sekretaris Jurusan");
				session.save(KAJUR);
			}

			JabatanKegiatanKedosenan sekjur = (JabatanKegiatanKedosenan) session
					.createCriteria(JabatanKegiatanKedosenan.class)
					.add(Restrictions.eq("nama", "Sekretaris Senat Fakutas")).setMaxResults(1).uniqueResult();
			if (sekjur == null) {
				sekjur = new JabatanKegiatanKedosenan();
				sekjur.setNama("Sekretaris Senat Fakutas");
				sekjur.setNomorUrut(45);
				sekjur.setKeterangan("Jabatan/Status/Tugas Sekretaris Senat Fakutas");
				session.save(sekjur);
			}

			JabatanKegiatanKedosenan anggotaSenat = (JabatanKegiatanKedosenan) session
					.createCriteria(JabatanKegiatanKedosenan.class)
					.add(Restrictions.eq("nama", "Anggota Senat Institut")).setMaxResults(1).uniqueResult();
			if (anggotaSenat == null) {
				anggotaSenat = new JabatanKegiatanKedosenan();
				anggotaSenat.setNama("Anggota Senat Institut");
				anggotaSenat.setNomorUrut(46);
				anggotaSenat.setKeterangan("Jabatan/Status/Tugas Anggota Senat Institut");
				session.save(anggotaSenat);
			}

			count = ((Number) session.createCriteria(KelompokKegiatanKedosenan.class)
					.add(Restrictions.eq("nama", "Dosen Mendapatkan Tugas Tambahan"))
					.setProjection(Projections.rowCount()).uniqueResult()).intValue();
			if (count == 0) {

				KelompokKegiatanKedosenan kelompokKegiatanKedosenan = new KelompokKegiatanKedosenan();
				kelompokKegiatanKedosenan.setNomorUrut(130);
				kelompokKegiatanKedosenan.setNama("Dosen Mendapatkan Tugas Tambahan");
				kelompokKegiatanKedosenan.setJenisKelompokKegiatanKedosenan(utama);
				kelompokKegiatanKedosenan.setKeterangan("Dosen Mendapatkan Tugas Tambahan");
				session.save(kelompokKegiatanKedosenan);

				DetailKelompokKegiatanKedosenan detailKelompokKegiatanKedosenan = new DetailKelompokKegiatanKedosenan();
				detailKelompokKegiatanKedosenan.setNama("Tugas Tambahan");
				detailKelompokKegiatanKedosenan.setNomorUrut(1);
				detailKelompokKegiatanKedosenan.setKelompokKegiatanKedosenan(kelompokKegiatanKedosenan);
				detailKelompokKegiatanKedosenan.getJabatanKegiatanKedosenans().add(Rektor);
				detailKelompokKegiatanKedosenan.getJabatanKegiatanKedosenans().add(WakilRektor);
				detailKelompokKegiatanKedosenan.getJabatanKegiatanKedosenans().add(Dekan);
				detailKelompokKegiatanKedosenan.getJabatanKegiatanKedosenans().add(WakilDekan);
				detailKelompokKegiatanKedosenan.getJabatanKegiatanKedosenans().add(KAJUR);
				detailKelompokKegiatanKedosenan.getJabatanKegiatanKedosenans().add(sekjur);
				detailKelompokKegiatanKedosenan.getJabatanKegiatanKedosenans().add(anggotaSenat);

				detailKelompokKegiatanKedosenan.getSkalaKegiatanKedosenans().add(Institut);
				detailKelompokKegiatanKedosenan.getSkalaKegiatanKedosenans().add(fakJur);

				session.save(detailKelompokKegiatanKedosenan);
			}
			session.getTransaction().commit();
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/InitDataHelper.java:6136");
		} finally {
			// 2. WAJIB Tutup Session
			if (session != null && session.isOpen()) {
				// session.disconnect();
				if (session.isOpen()) {
					session.disconnect();
					session.close();
				}
			}
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void initEksekusiQuery() {
		org.hibernate.Session session = null;

		try {
			// 1. Buka Session Baru (Isolated Session)
			session = ais.database.hibernate.HibernateUtil.getSessionFactory().openSession();

			// ---------------------------------------------------------
			// A. Inisialisasi Konfigurasi Dasar
			// ---------------------------------------------------------
			String[] konfigurasis = new String[] { "label_huruf;Huruf", "label_nilai;Nilai", "label_angkatan;Angkatan",
					"label_hari_jam;Hari/Waktu", "label_ruang;Ruang", "label_kelas_singkatan;Kelas",
					"label_kelas;Kelas", "label_sks;SKS", "label_matakuliah;Matakuliah", "label_kode;Kode",
					"label_no;No.", "label_semester;Semester", "label_tahun_akademik;Tahun Akademik",
					"label_jenjang;Jenjang", "label_nim;NIM", "label_nama;Nama" };

			for (String s : konfigurasis) {
				String[] ss = splitInitData(s);
				if (ss.length >= 2) {
					ais.common.Common.getKonfigurasi(ss[0], ss[1]);
				}
			}

			// Menghindari duplikasi deklarasi di versi sebelumnya
			String[] persetujuanUts = new String[] { "nama_persetujuan_uts" };
			for (String s : persetujuanUts) {
				ais.database.model.Konfigurasi k = ais.common.Common.getKonfigurasi(s, s);
				k.setNama("label_" + s);

				if (!session.getTransaction().isActive())
					session.getTransaction().begin();
				ais.common.Common.refreshUpdate(session, k);
				session.getTransaction().commit();
			}

			ais.common.ConstantValues.realoadNilaiHuruf(session);
			ais.common.ConstantValues.realoadNilaiHurufSekolah(session);

			// ---------------------------------------------------------
			// B. Proses Sinkronisasi Tabel Pertemuan (Memori Terkontrol)
			// ---------------------------------------------------------
			java.util.Calendar lo = java.util.Calendar.getInstance();
			lo.set(java.util.Calendar.MONTH, lo.get(java.util.Calendar.MONTH) - 2);

			java.util.Calendar hi = java.util.Calendar.getInstance();
			hi.set(java.util.Calendar.MONTH, hi.get(java.util.Calendar.MONTH) + 2);

			java.util.List<ais.database.model.Pertemuan> pertemuans = session
					.createCriteria(ais.database.model.Pertemuan.class)
					.add(org.hibernate.criterion.Restrictions.or(org.hibernate.criterion.Restrictions.isNull("aktif"),
							org.hibernate.criterion.Restrictions.eq("aktif", true)))
					.add(org.hibernate.criterion.Restrictions.between("tanggal", lo.getTime(), hi.getTime())).list();

			System.out.println(
					"loading data " + ais.database.model.Pertemuan.class.getName() + " sebanyak " + pertemuans.size());
			for (ais.database.model.Pertemuan pertemuan : pertemuans) {
				if (pertemuan.getAktif() != null && pertemuan.getAktif()) {
					ais.database.model.GeneralValueObject.masukkanData(ais.database.model.Pertemuan.class, pertemuan);
				}
			}
			pertemuans.clear();
			pertemuans = null; // Bantu GC

			// ---------------------------------------------------------
			// C. Sinkronisasi Tabel Nominal Biaya
			// ---------------------------------------------------------
			java.util.List<Long> tags = session.createCriteria(NominalBiaya.class)
					.setProjection(org.hibernate.criterion.Projections.property("id"))
					.add(org.hibernate.criterion.Restrictions.isNull("baru")).list();

			if (tags != null && !tags.isEmpty()) {
				for (Long tagid : tags) {
					if (tagid != null) {
						try {
							/*
							 * Data NominalBiaya diambil sebagai ID saja dan tidak dipaksa session.update().
							 * Object yang sudah persistent tidak perlu di-update ulang. Pola lama bisa
							 * memicu konflik collection dua session pada Hibernate 3.6 jika relasi lazy
							 * sudah pernah terasosiasi session lain.
							 */
							java.util.Map parameters = new java.util.HashMap();
							parameters.put("id", tagid);
							executeHqlUpdateIsolated("update NominalBiaya set baru = baru where id = :id", parameters);
						} catch (Exception e) {
							rollbackActiveTransaction(session);
							Common.tampilErrorJikaAdmin(e);
						}
					}
				}
				tags.clear();
			}
			tags = null; // Bantu GC

		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/InitDataHelper.java:6244");
		} finally {
			// WAJIB Tutup Session Induk
			if (session != null) {
				try {
					if (session.isOpen())
						session.clear();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/InitDataHelper.java:6251");
				}
				try {
					session.disconnect();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/InitDataHelper.java:6255");
				}
				try {
					if (session.isOpen())
						session.close();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/InitDataHelper.java:6260");
				}
			}
			ais.database.hibernate.HibernateUtil.closeSession();
		}

		// ===================================================================================
		// BLOK KEDUA: EKSEKUSI INDEKS DATABASE DAN PEMBERSIHAN DATA SQL
		// Dijalankan di luar sesi Hibernate utama untuk mencegah transaction conflict
		// ===================================================================================
		try {

			// 5. CLEANUP / DATA MIGRATION QUERY
			String[] migrationQueries = {
					"update kegiatan set aktif=false where (aktif=true or aktif is null) and (kodeunik,tanggal_dirubah) in (select kodeunik,min(tanggal_dirubah) as tanggal_dirubah from kegiatan where (aktif=true or aktif is null) and kodeunik is not null group by kodeunik having count(*)>1)",
					"update asset.permintaan_pengadaan_master_asset_detail set hargatotal = (hargabeli * jumlah) where hargatotal is null",
					"update akunting.posting_history set posting = true where posting is null",
					"update nilai_huruf set kodemk='' where kodemk is null",
					"update detail_setting_biaya set bayarke=1 where bayarke is null",
					"update detail_biaya set bayarke=1 where bayarke is null",
					"update cicilan_pembayaran set bayarke=1 where bayarke is null",
					"update setting_biaya set jumlahpembayaran=1 where jumlahpembayaran is null",
					"update detailperkuliahan set detail_nilai_kunci=detail_nilai_baru_lagi where detail_nilai_kunci is null",
					"update detailperkuliahan set detail_nilai_tambahan_kunci=detail_nilai_tambahan_baru_lagi where detail_nilai_tambahan_kunci is null",
					"update detailperkuliahan set total_nilai_kunci=total_nilai where total_nilai_kunci is null",
					"update detailperkuliahan set nilai_huruf_kunci=nilai_huruf where nilai_huruf_kunci is null",
					"update detailperkuliahan set nilai_ip_kunci=nilai_ip where nilai_ip_kunci is null",
					"update detailperkuliahan set lulus_kunci=lulus where lulus_kunci is null",
					"update employ.kenaikan_pangkat set jenisperubahan='" + KenaikanPangkat.UBAH_JABATAN_DAN_GOLONGAN
							+ "' where jenisperubahan is null or jenisperubahan = 'Ubah Jabatan dan Golangan' or jenisperubahan = 'Jabatan dan Golangan'",
					"update payroll.libur_nasional set sampai=tanggal where sampai is null",
					"update setting_biaya set minsmt=0 where minsmt is null",
					"update setting_biaya set maxsmt=30 where maxsmt is null",
					"update paket_perkuliahan set minsmt=0 where minsmt is null",
					"update paket_perkuliahan set maxsmt=30 where maxsmt is null",
					"update perkuliahan set aktif=true where aktif is null",
					"update propinsi set aktif=true where aktif is null",
					"update kota set aktif=true where aktif is null",
					"update wilayah set aktif=true where aktif is null",
					"update mahasiswa set aktif=true where aktif is null",
					"update sekolah.siswa set aktif=true where aktif is null",
					"update jenjang set aktif=true where aktif is null",
					"update jurusan set aktif=true where aktif is null",
					"update pegawai set aktif=true where aktif is null",
					"update perguruan_tinggi set aktif=true where aktif is null",
					"update dosen set aktif=true where aktif is null",
					"update sekolah.guru set aktif=true where aktif is null",
					"update matakuliah set aktif=true where aktif is null",
					"update negara set aktif=true where aktif is null", "update menu set root=2 where root=220",
					"update menu set url=replace(url,'..','/pages') where url ilike '%..%'",
					"update menu set child=6000000 where id=363",
					"update menu set aktif=false where id in (175, 1862111111, 1862111112)",
					"update menu set nomorurut=0 where nomorurut is null",
					"delete from label_bahasa where indonesia ~ '^[0-9\\.]+$'",
					"delete from label_bahasa where replace(indonesia,'.','') ~ '^[0-9\\.]+$'",
					"delete from label_bahasa where replace(indonesia,',','') ~ '^[0-9\\.]+$'",
					"delete from label_bahasa where indonesia ilike '%(Mahasiswa)%'",
					"delete from label_bahasa where indonesia ilike '%(Dosen)%'",
					"delete from label_bahasa where replace(indonesia,'/','') ~ '^[0-9\\.]+$'",
					"delete from label_bahasa where indonesia ilike '%(Ganjil)%'",
					"delete from label_bahasa where indonesia ilike '%(Genap)%'",
					"delete from label_bahasa where indonesia in (select nama from mahasiswa)",
					"delete from label_bahasa where indonesia in (select nama from dosen)",
					"delete from label_bahasa where nama SIMILAR TO '%[0-9]{2,}%'",
					"delete from label_bahasa where (nama ~* '[a-z]') is false",
					"delete from label_bahasa where indonesia ilike '%.jpg%'",
					"delete from label_bahasa where indonesia ilike '%.jpeg%'",
					"delete from label_bahasa where indonesia ilike '%.png%'",
					"delete from label_bahasa where indonesia ilike '%.pdf%'",
					"delete from label_bahasa where indonesia ilike '%.doc%'",
					"delete from label_bahasa where indonesia ilike '%.docx%'",
					"delete from label_bahasa where indonesia ilike '%.ppt%'",
					"delete from label_bahasa where indonesia ilike '%.pptx%'",
					"delete from label_bahasa where indonesia ilike '%.xls%'",
					"delete from label_bahasa where indonesia ilike '%.xlsx%'",
					"delete from label_bahasa where nama ilike '%..%'",
					"delete from label_bahasa where olehid ilike '%DashboardPustaka%'",
					"delete from label_bahasa where olehid ilike '%PendaftaranWisudaMahasiswaAction%'",
					"delete from label_bahasa where olehid ilike '%TugasMandiriHelper%'",
					"delete from label_bahasa where olehid ilike '%ais.database.model.BiodataCalonMahasiswa%'",
					"delete from label_bahasa where olehid ilike '%ais.database.model.Mahasiswa%'",
					"delete from label_bahasa where olehid ilike '%ais.database.model.Dosen%'",
					"delete from label_bahasa where olehid ilike '%ais.action.master.helper.StudiMahasiswaHelper%'",
					"delete from label_bahasa where olehid ilike '%ais.action.master.helper.DetailperkuliahanHelper%'",
					"delete from label_bahasa where olehid ilike '%ais.action.master.dashboard.helper%'",
					"delete from label_bahasa where olehid ilike '%external_update;ais.common.Common%'",
					"delete from konfigurasi where (nilai ='' or nilai is null) and nama ilike '%ang:%'",
					"update setting_biaya set ta=0 where ta is null",
					"update mahasiswa aa set kelas=(select max(kelas) from new_audit.mahasiswa__audit where id=aa.id and kelas != '' and kelas is not null) where kelas='' or kelas is null",
					"delete from biodata_mahasiswa where mahasiswa is null"

//					,
//					"TRUNCATE TABLE new_audit.detail_kegiatan__audit",
//					"TRUNCATE TABLE new_audit.rekap_angket_dosen__audit", "TRUNCATE TABLE new_audit.error_log__audit",
//					"TRUNCATE TABLE new_audit.checklist_baru_penilaian_dosen_oleh_mahasiswa__audit",
//					"TRUNCATE TABLE new_audit.notifikasi__audit", "TRUNCATE TABLE new_audit.label_bahasa__audit"

			};

			for (String sql : migrationQueries) {
				try {
					ais.common.Common.updateSql(sql, 600, true);
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/InitDataHelper.java:6357");
				}
			}

			// Duplicate deletion for RAB Penggunaan Anggaran
			for (int ii = 0; ii < 10; ii++) {
				try {
					ais.common.Common.updateSql10Menit(
							"delete from rab.penggunaan_anggaran where id in (select max(id) from rab.penggunaan_anggaran group by ref having count(*)>1)");
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/InitDataHelper.java:6366");
				}
			}

			// Notifikasi Deletion
//			java.util.Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
//			calendar.set(java.util.Calendar.DATE, calendar.get(java.util.Calendar.DATE) - 7);
//			try {
//				int jumlahHapusNotif = ais.common.Common.updateSql("delete from notifikasi where date(waktu) < date('"
//						+ ais.common.Common.databaseDateFormat.get().format(calendar.getTime()) + "')");
//				System.out.println("hapus jumlahHapusNotif " + jumlahHapusNotif);
//			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/InitDataHelper.java:6377");
//			}

		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/InitDataHelper.java:6381");
		}

		// ===================================================================================
		// BLOK KETIGA: PEMBERSIHAN DATA MULTIMEDIA / FILE (MENGGUNAKAN STREAMING
		// SESSION)
		// ===================================================================================
		try {
			int pertemuan_file_content = ais.common.Common.updateSqlStreaming(
					"delete from pertemuan_file_content where filecontent is null and copy_dari is null and (gdrive is null or trim(gdrive)='') and (link is null or trim(link)='') and (google_book is null or trim(google_book)='') and id not in (select copy_dari from pertemuan_file_content where copy_dari is not null group by copy_dari)");
			int video_pertemuan = ais.common.Common.updateSqlStreaming(
					"delete from video_pertemuan where filecontent is null and copy_dari is null and (gdrive is null or trim(gdrive)='') and (link is null or trim(link)='') and id not in (select copy_dari from video_pertemuan where copy_dari is not null group by copy_dari)");
			int audio_pertemuan = ais.common.Common.updateSqlStreaming(
					"delete from audio_pertemuan where filecontent is null and copy_dari is null and (gdrive is null or trim(gdrive)='') and (link is null or trim(link)='') and id not in (select copy_dari from audio_pertemuan where copy_dari is not null group by copy_dari)");
			int lampiran_lain = ais.common.Common.updateSqlStreaming(
					"delete from lampiran_lain where foto is null and copy_dari is null and (gdrive is null or trim(gdrive)='') and (link is null or trim(link)='') and id not in (select copy_dari from lampiran_lain where copy_dari is not null group by copy_dari)");

			System.out.println("hapus pertemuan_file_content " + pertemuan_file_content);
			System.out.println("hapus video_pertemuan " + video_pertemuan);
			System.out.println("hapus audio_pertemuan " + audio_pertemuan);
			System.out.println("hapus lampiran_lain " + lampiran_lain);

		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/InitDataHelper.java:6404");
		}
	}

	public static void reInitClass() {
		try {
			Reflections reflections = new Reflections("ais.action.master");
			Set<Class<? extends FormSop>> allClasses = reflections.getSubTypesOf(FormSop.class);

			for (Class<? extends FormSop> c : allClasses) {
				FormSop cs = c.newInstance();
				ConstantValues.treeMapFormSop.put(c.getName(), cs.istilah());
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/InitDataHelper.java:6418");
		}

		try {

			Reflections reflections = new Reflections("ais.action.master.sekolah.psb.form");
			Set<Class<? extends PPDB>> allClasses = reflections.getSubTypesOf(PPDB.class);

			for (Class<? extends PPDB> c : allClasses) {
				ConstantValues.treeMapFormPpdb.put(c.getName(), c.getSimpleName());
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/InitDataHelper.java:6430");
		}
	}
}
