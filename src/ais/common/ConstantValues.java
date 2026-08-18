package ais.common;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.TypeMismatchException;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.metadata.ClassMetadata;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.IkatanKerjaDosen;
import ais.database.model.ItemBiaya;
import ais.database.model.Jabatan;
import ais.database.model.JenisAktfitasMahasiswa;
import ais.database.model.JenisEvaluasi;
import ais.database.model.JenisKegiatan;
import ais.database.model.JenisPembayaran;
import ais.database.model.JenisPembiayaanMahasiswa;
import ais.database.model.JenisRekonsiliasiHostToHost;
import ais.database.model.JenisSekolahMahasiswaBaru;
import ais.database.model.Jenjang;
import ais.database.model.Jurusan;
import ais.database.model.JurusanSekolahMahasiswaBaru;
import ais.database.model.Konfigurasi;
import ais.database.model.Mahasiswa;
import ais.database.model.Menu;
import ais.database.model.Negara;
import ais.database.model.NilaiHuruf;
import ais.database.model.Paket;
import ais.database.model.PembombotanNilai;
import ais.database.model.Program;
import ais.database.model.Staff;
import ais.database.model.StatusAwalMahasiswa;
import ais.database.model.StatusKewajibanBebanDosen;
import ais.database.model.StatusMahasiswa;
import ais.database.model.StatusPegawai;
import ais.database.model.StatusPertemuan;
import ais.database.model.Statusabsensi;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.database.model.TemplateFormatBimbingan;
import ais.database.model.akunting.Akun;
import ais.database.model.inventory.JenisProduk;
import ais.database.model.koperasi.CaraPembayaranKoperasi;
import ais.database.model.koperasi.JenisAnggotaKoperasi;
import ais.database.model.koperasi.JenisIdentitasAnggotaKoperasi;
import ais.database.model.koperasi.JenisTransaksiKoperasi;
import ais.database.model.koperasi.TipeAnggotaKoperasi;
import ais.database.model.koperasi.TipeProdukKoperasi;
import ais.database.model.penelitiandanpengabdian.TipePenelitianDanPengabdian;
import ais.database.model.sekolah.NilaiHurufSekolah;
import ais.database.model.sekolah.Siswa;
import ais.database.model.sekolah.StatusAwalSiswa;
import ais.database.model.sirs.Instalasi;
import ais.database.model.sirs.JenisPasien;
import ais.database.model.sirs.JenisTindakan;
import ais.database.model.sirs.KelasPerawatan;
import ais.database.model.sirs.KodeTransaksiMedis;
import ais.database.model.sirs.Poly;
import ais.database.model.sirs.SatuanItem;
import ais.database.model.sirs.StatusPulang;
import ais.database.model.sirs.StatusTempatTidur;
import ais.database.model.sirs.Tindakan;
import ais.ui.util.WaktuUtil;
import nl.captcha.backgrounds.GradiatedBackgroundProducer;
import nl.captcha.gimpy.FishEyeGimpyRenderer;
import nl.captcha.noise.StraightLineNoiseProducer;
import nl.captcha.text.producer.DefaultTextProducer;

public class ConstantValues {

	private static Session openReadOnlySession() {
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			session.setDefaultReadOnly(true);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/ConstantValues.java:87");
		}
		try {
			session.setFlushMode(org.hibernate.FlushMode.MANUAL);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/ConstantValues.java:91");
		}
		return session;
	}

	private static void closeOpenedSession(Session session) {
		if (session != null) {
			try {
				session.clear();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/ConstantValues.java:100");
			}
			try {
				session.disconnect();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/ConstantValues.java:104");
			}
			try {
				if (session.isOpen()) {
					session.close();
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/ConstantValues.java:110");
			}
		}
	}


	public static final String NO_URUT = "no_urut_data";

	public static Integer UKURAN_BATAS_MOBILE = 800;

	public static Integer JUMLAH_DIGIT_DIBELAKANG_KOMA = 3;

	public static JenisEvaluasi EvaluasiAkademik;
	public static JenisEvaluasi AktivitasPartisipatif;
	public static JenisEvaluasi HasilProyek;
	public static JenisEvaluasi KognitifPengetahuan;
	public static JenisEvaluasi Tugas;

	public static PembombotanNilai DEFAULT_PEMBOBOTAN_NILAI;

	public static TreeMap<String, String> treeMapFormSop = new TreeMap<String, String>();
	public static TreeMap<String, String> treeMapFormPpdb = new TreeMap<String, String>();

	// public static JenisSeleksi spmbUndangan;
	// public static JenisSeleksi spmbUjian;
	// public static JenisSeleksi spmbTandaUjian;

	public static Jenjang d3;
	public static Jenjang d4;
	public static Jenjang s1;
	public static Jenjang s2;
	public static Jenjang s3;
	public static JenisSekolahMahasiswaBaru sekolahS1;
	public static JenisSekolahMahasiswaBaru sekolahS2;

	public static JurusanSekolahMahasiswaBaru s1SemuaJurusan;
	public static JurusanSekolahMahasiswaBaru s2SemuaJurusan;
	public static Paket paketA;
	public static Paket paketD;
	public static Paket paketE;

	public static StatusMahasiswa AKTIF;
	public static StatusMahasiswa CUTI;
	public static StatusMahasiswa LULUS;
	public static StatusMahasiswa KELUAR;
	public static StatusMahasiswa DROP_OUT;
	public static StatusMahasiswa TIDAK_AKTIF = null;
	public static StatusMahasiswa KAMPUS_MERDEKA;
	public static StatusMahasiswa MENUNGGU_UJI_KOPETENSI;

	public static Boolean udahSelesai = false;

	public static StatusPegawai AKTIF_PEGAWAI;
	public static StatusPegawai CUTI_PEGAWAI;
	public static StatusPegawai CUTI_HAJI;
	public static StatusPegawai TIDAK_AKTIF_PEGAWAI;
	public static StatusPegawai KELUAR_PEGAWAI;
	public static StatusPegawai PENSIUN_PEGAWAI;
	public static StatusPegawai MENINGGAL_PEGAWAI;

	public static StatusAwalMahasiswa BARU;
	public static StatusAwalMahasiswa BARU_BEASISWA;
	public static StatusAwalMahasiswa PINDAHAN;
	// public static StatusAwalMahasiswa PINDAHAN1;
	// public static StatusAwalMahasiswa PINDAHAN2;
	// public static StatusAwalMahasiswa PINDAHAN3;
	// public static StatusAwalMahasiswa PINDAHAN4;
	// public static StatusAwalMahasiswa PINDAHAN5;
	// public static StatusAwalMahasiswa PINDAHAN6;
	// public static StatusAwalMahasiswa PINDAHAN7;
	// public static StatusAwalMahasiswa PINDAHAN8;
	public static StatusAwalMahasiswa ALIH_PRODI;

	public static StatusAwalSiswa BARU_SISWA;
	public static StatusAwalSiswa BARU_SISWA_BEASISWA;
	public static StatusAwalSiswa PINDAHAN_SISWA;

	public static Negara INDONESIA;

	public static Tbmrole rolePeserta;
	public static Tbmrole roleDosen;
	public static Tbmrole roleOrangTua;
	public static Tbmrole roleKantin;
	public static Tbmrole roleGuru = null;
	public static Tbmrole rolePegawai;
	public static Tbmrole roleKomunitas;
	public static Tbmrole roleAdminFakultas;
	public static Tbmrole roleAdminJurusan;
	public static Tbmrole roleAnggotaPerpustakaan;
	public static Tbmrole roleAnggotaKoperasi;
	public static Tbmrole rolePesertaKursusPerpustakaan;
	public static Tbmrole Akademik;

	public static ItemBiaya DENDA;
	public static ItemBiaya SPP;
	public static ItemBiaya CUTI_ITEM_BIAYA;

	public static Jabatan KAPRODI;

	public static String BIG_ICON = "BIG_ICON";

	public static JenisKegiatan PENDAFTARAN_MAHASISWA_LAMA;
	public static JenisKegiatan PENDAFTARAN_CALON_MAHASISWA;
	public static JenisKegiatan PENDAFTARAN_WISUDA;
	public static JenisKegiatan PENDAFTARAN_ULANG_MAHASISWA_BARU;
	public static JenisKegiatan PEMBAYARAN_SP;

	public static Statusabsensi MASUK;
	public static Statusabsensi TIDAK_ADA_ALASAN;
	public static Statusabsensi SAKIT;
	public static Statusabsensi IZIN;
	public static Statusabsensi STATUS_CUTI;
	public static Statusabsensi CUTI_MELAHIRKAN;
	public static Statusabsensi CUTI_HAJI_PEGAWAI;
	public static Statusabsensi CUTI_UMROH_PEGAWAI;
	public static Statusabsensi CUTI_HAMIL;
	public static Statusabsensi CUTI_TAHUNAN;
	public static Statusabsensi CUTI_SAKIT;
	public static Statusabsensi CUTI_ALASAN_PENTING;
	public static Statusabsensi CUTI_PENTING;
	public static Statusabsensi CUTI_BESAR;
	public static Statusabsensi CUTI_BESAR_II;
	public static Statusabsensi CUTI_DILUAR_TANGGUNGAN;
	public static Statusabsensi CUTI_STUDI_ATAU_PENELITIAN;
	public static Statusabsensi DINAS_LUAR;
	public static Statusabsensi TUGAS_BELAJAR;
	public static Statusabsensi BELUM_ABSEN;

	public final static List<Statusabsensi> listAbsenMahasiswa = new ArrayList<Statusabsensi>();

	public static Menu MENU_UTAMA_DOSEN;
	public static Menu MENU_UTAMA_MAHASISWA;

	public static Menu MENU_MANAJEMEN_KRS_DOSEN;

	public static Program REGULER;
	public static Program NON_REGULER;

	public static KodeTransaksiMedis produksi;
	public static KodeTransaksiMedis bahanBaku;
	public static KodeTransaksiMedis saldoAwal;
	public static KodeTransaksiMedis beliMasuk;
	public static KodeTransaksiMedis beliRetur;
	public static KodeTransaksiMedis transferItemDari;
	public static KodeTransaksiMedis transferItemKe;
	public static KodeTransaksiMedis transferItemSelisih;
	public static KodeTransaksiMedis apotikJual;
	public static KodeTransaksiMedis jasaRacik;
	public static KodeTransaksiMedis kunjunganDokter;
	public static KodeTransaksiMedis transaksiBed;

	public static KodeTransaksiMedis lab;
	public static KodeTransaksiMedis operasi;
	public static KodeTransaksiMedis radiologi;
	public static KodeTransaksiMedis vk;
	public static KodeTransaksiMedis renalUnit;
	public static KodeTransaksiMedis gizi;
	public static KodeTransaksiMedis ugd;
	public static KodeTransaksiMedis lain;

	public static KodeTransaksiMedis apotikRetur;
	public static KodeTransaksiMedis adjustmentPenambahan;
	public static KodeTransaksiMedis adjustmentPengurangan;
	public static KodeTransaksiMedis pemakaianBarang;
	public static KodeTransaksiMedis returPemakaianBarang;

	public static SatuanItem DEFAULT_SATUAN;

	public static Akun KAS;
	public static JenisPembayaran TUNAI;
//	public static JenisPembayaran DEPOSIT;

	public static IkatanKerjaDosen DOSEN_TETAP;
	public static IkatanKerjaDosen DOSEN_HONORER;

	public static JenisPembiayaanMahasiswa MANDIRI;

	public static volatile boolean hasbeeninit = false;

	public static Integer pembayaranSemesterGanjilMulaiDiBulan = 7;
	public static Integer pembayaranSemesterGenapMulaiDiBulan = 1;
	public static Map<String, Integer> jumlahTahapan = new HashMap<String, Integer>();

	public static Integer getJumlahTahapan(String program, Jurusan jurusan) {
		Integer tahap = 0;
		try {
			if (jurusan != null && jurusan.getId() != null) {
				tahap = jumlahTahapan.get(program + "_" + jurusan.getId());
			}
			if (tahap == null) {
				tahap = jumlahTahapan.get(program);
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/ConstantValues.java:300");
			// TODO: handle exception
		}
		// Nilai ini sering dibandingkan dengan literal int. Jangan pernah kembalikan
		// null karena auto-unboxing akan melempar NullPointerException saat cache
		// ringkasan memproses mahasiswa yang mapping tahapannya belum tersedia.
		return tahap == null ? Integer.valueOf(0) : tahap;
	}

	public static boolean ABSEN_DOSEN_TERINTEGRASI_DENGAN_FINGER_PRINT = true;
	public static boolean ABSEN_GURU_TERINTEGRASI_DENGAN_FINGER_PRINT = true;
	public static boolean ABSEN_MAHASISWA_TERINTEGRASI_DENGAN_FINGER_PRINT = true;
	public static boolean ABSEN_SISWA_TERINTEGRASI_DENGAN_FINGER_PRINT = true;

	public static String filter_tidak_boleh_ada = "MARQUEE;SCRIPT;FUNCTION;JAVA:ALERT;<BODY";

	public static boolean aktifkanTahapan = false;
	public static boolean aktifkanTahapanKurikulum = false;
	public static boolean aktifkanTahapanTerhubungKeKeuangan = false;
	public static boolean aktifkanIntegrasiFacebook = true;

	public static boolean aktifkanFingerPrintOtomatisDariKeterangan = false;

	public static JenisRekonsiliasiHostToHost DefaultRekonsiliasi;

	// public static boolean terlambarLangsungTidakAktif = false;

	public static boolean aktifkanIntegrasiGoogle = true;

	public static boolean aktifkan_akun_demo = false;

//	public static boolean fingerprint_hanya_gunakan_finger = false;

	public static boolean pegawai_non_aktif_otomatis_tidak_bisa_login = true;

	public static boolean aktifkanIntegrasiTwitter = true;

	public static boolean aktifkanIntegrasiLinkedin = true;

	public static boolean aktifkanLoginHanyaViaMediaSocial = false;
	public static boolean aktifkanRememeberMe = true;
	public static boolean aktifkanRememeberMeOtomatisTerpilih = false;
	public static boolean aktifkanRecapcha = false;

	public static boolean kehadiranHarusMulaiDanSampai = false;

	public static String recapchaKey = "6LfVPQgUAAAAADijfpzVsK_l9F7b61iyvjg8mFuV";
	public static String recapchaClientKey = "6LfVPQgUAAAAAMHwCF5fB2lHkQHPuGJxOTZ2ibN4";
	public static String recapchaHome = "login";

	public static boolean aktifkanCaptchaLokal = false;
	public static String aktifkanCaptchaLokalTinggi = "70";
	public static String aktifkanCaptchaLokalLebar = "300";
	public static String aktifkanCaptchaLokalNoice = StraightLineNoiseProducer.class.getName();
	public static String aktifkanCaptchaLokalRender = FishEyeGimpyRenderer.class.getName();
	public static String aktifkanCaptchaLokalBackground = GradiatedBackgroundProducer.class.getName();
	public static String aktifkanCaptchaLokalText = DefaultTextProducer.class.getName();
	// public static String aktifkanCaptchaLokalWordRenderer = "";

	public static String grupPenggunaBlok = "";

	public static String DSPACE_URL_PUBLIK = "http://demo.ecampus.id/rest";
	public static String DSPACE_URL_PRIVATE = "http://demo.ecampus.id/rest";
	public static String DSPACE_UI = "xmlui";

	public static boolean sop_alur_terakhir_otomatis_jadi_persetujuan = false;

	public static boolean aktifkanApakahJumlahLoginDibatasi = true;
	public static boolean satuperangkat = false;
	public static boolean passwordKuat = false;

	public static boolean satuperangkat_mahasiswa = false;
	public static boolean satuperangkatipygbeda = false;

	public static int nilaiJumlahLoginDibatasi = 300;

	public static boolean penggunaanLabelBahasa = true;

	public static boolean otomatisTerposting = true;

	public static boolean aktifkanApakahJumlahLoginDosenDibatasi = true;

	public static boolean ketikaUbahDataPenggunaKirimke = false;
	public static String ketikaUbahDataPenggunaKirimkeLink = "";

	public static boolean ketikaUbahSemuaDataKirimke = false;
	public static String ketikaUbahSemuaDataKirimkeLink = "";

	public static int nilaiJumlahLoginDosenDibatasi = 300;

	public static boolean aktifkanHariLiburMingguSaja = true;
	public static boolean aktifkanHariLiburSabtuDanMingguSaja = true;
	public static boolean aktifkanHariLibur = true;

	public static TipePenelitianDanPengabdian PENELITIAN;

	public static TipePenelitianDanPengabdian PENGABDIAN;

	public static TipePenelitianDanPengabdian PENELITIAN_LAINNYA;

	public static StatusKewajibanBebanDosen DOSEN_BIASA;
	public static StatusKewajibanBebanDosen DOSEN_PROFESOR;
	public static StatusKewajibanBebanDosen DOSEN_BIASA_DENGAN_TUGAS_TAMBAHAN;
	public static StatusKewajibanBebanDosen DOSEN_PROFESOR_DENGAN_TUGAS_TAMBAHAN;
	public static StatusKewajibanBebanDosen DOSEN_DENGAN_JABATAN_STRUKTURAL;

	public static JenisAnggotaKoperasi ANGGOTA_KOPERASI_REGULER;
	public static JenisIdentitasAnggotaKoperasi EMAIL;
	public static JenisIdentitasAnggotaKoperasi NIM;
	public static JenisIdentitasAnggotaKoperasi NIDN;
	public static JenisIdentitasAnggotaKoperasi NUPTK;
	public static JenisIdentitasAnggotaKoperasi KTP;
	public static JenisIdentitasAnggotaKoperasi NIS;

	public static TipeAnggotaKoperasi UMUM;
	public static TipeAnggotaKoperasi MAHASISWA;
	public static TipeAnggotaKoperasi SISWA;
	public static TipeAnggotaKoperasi GURU;
	public static TipeAnggotaKoperasi PEGAWAI;
	public static TipeAnggotaKoperasi DOSEN;

	public static TipeProdukKoperasi PINJAMAN;
	public static TipeProdukKoperasi SIMPANAN;

	public static JenisTransaksiKoperasi SETORAN;
	public static JenisTransaksiKoperasi PENARIKAN;
	public static JenisTransaksiKoperasi BAGI_HASIL;
	public static JenisTransaksiKoperasi BAGI_HASIL_BERJANGKA;
	public static JenisTransaksiKoperasi BIAYA_ADMIN;
	public static JenisTransaksiKoperasi SIMPANAN_POKOK;
	public static JenisTransaksiKoperasi SIMPANAN_WAJIB;
	public static JenisTransaksiKoperasi SIMPANAN_KHUSUS;
	public static JenisTransaksiKoperasi KOREKSI_SETORAN;
	public static JenisTransaksiKoperasi KOREKSI_PENARIKAN;
	public static JenisTransaksiKoperasi KOREKSI_BAGI_HASIL;
	public static JenisTransaksiKoperasi SALDO_SIMPANAN;
	public static JenisTransaksiKoperasi PINDAH_BUKU;
	public static JenisTransaksiKoperasi REALISASI;
	public static JenisTransaksiKoperasi ANSURAN;

	public static CaraPembayaranKoperasi BAYAR_TUNAI;
	public static CaraPembayaranKoperasi BAYAR_TRANSFER;

	public static JenisProduk PRODUK_UMUM;

	public static Instalasi RAWAT_JALAN;
	public static Instalasi RAWAT_INAP;
	public static Instalasi UGD;
	public static Instalasi PENUNJANG_MEDIK;

	@SuppressWarnings("rawtypes")
	public static boolean classExist(Class clazzx) {

		String clazzs = StringUtils.split(clazzx.getName(), "_")[0];
		boolean ada = MemoryCacheUtil.exists(clazzs);
		return ada || Tbmuser.class.getName().equals(clazzs) || Tbmrole.class.getName().equals(clazzs);
	}

	public static boolean classExist(String clazzx) {
		String clazzs = StringUtils.split(clazzx, "_")[0];
		boolean ada = MemoryCacheUtil.exists(clazzs);
		return ada || Tbmuser.class.getName().equals(clazzs) || Tbmrole.class.getName().equals(clazzs);
	}

	@SuppressWarnings({})
	public static <T> List<T> simpleList(Criteria criteria, Class<T> clazz) {
		return simpleList(criteria, clazz, true);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static <T> List<T> simpleList(Criteria criteria, Class<T> clazzx, boolean projection) {
		try {
			if (clazzx != null) {
				// Biasanya untuk memotong nama class proxy Hibernate yang mengandung "_"
				String clazzs = StringUtils.split(clazzx.getName(), "_")[0];

				if (ConstantValues.classExist(clazzs)) {
					if (projection) {
						if (clazzs.equals(Tbmuser.class.getName())) {
							criteria.setProjection(Projections.property("userId"));
						} else if (clazzs.equals(Tbmrole.class.getName())) {
							criteria.setProjection(Projections.property("roleId"));
						} else {
							criteria.setProjection(Projections.property("id"));
						}
					}

					List<Serializable> ids = criteria.list();
					List<T> hasil = new ArrayList<T>();

					for (Serializable id : ids) {
						GeneralValueObject valueObject = ConstantValues.ambil(clazzs, id);

						// PERBAIKAN: Gunakan clazzx.isInstance(valueObject) sebagai pengganti instanceof
						if (valueObject != null && clazzx.isInstance(valueObject) && !hasil.contains(valueObject)) {
							// Lakukan casting menggunakan clazzx.cast()
							hasil.add(clazzx.cast(valueObject));
						}
					}
					ids = null;
					return hasil;
				} else {

					if (!projection) {
						// Gunakan raw List untuk menghindari ClassCastException (Long vs String vs Object)
						List serializables = criteria.list();
						return (List<T>) ambilBanyak(clazzs, serializables);
					} else {
						return (List<T>) criteria.list();
					}
				}
			} else {
				return (List<T>) criteria.list();
			}
		} catch (Exception e) {
			try {
				// Pastikan clazzx tidak null sebelum memanggil getName()
				if (!projection && clazzx != null) {
					List serializables = criteria.list();
					return (List<T>) ambilBanyak(StringUtils.split(clazzx.getName(), "_")[0], serializables);
				} else {
					return (List<T>) criteria.list();
				}
			} catch (Exception ee) {
				return new ArrayList<T>();
			}
		}
	}
	
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static List ambilBanyak(String clazz, List<Long> ids) {
		List<GeneralValueObject> generalValueObjects = new ArrayList<GeneralValueObject>();
		if (ids == null || ids.isEmpty()) {
			return generalValueObjects;
		}
		try {

			clazz = ambilSegmenAwalClazz(clazz);
			Map<Serializable, GeneralValueObject> dataJsonMasterSimple = ais.common.MemoryCacheUtil.get(clazz);
			List<Long> idsNggakAda = new ArrayList<Long>();
			for (Long id : ids) {
				GeneralValueObject generalValueObject = getDariCacheAman(dataJsonMasterSimple, id);
				if (generalValueObject == null) {
					idsNggakAda.add(id);
				} else {
					generalValueObjects.add(generalValueObject);
				}
			}
			if (!idsNggakAda.isEmpty()) {
				// System.out.println("clazz " + clazz + " idsNggakAda -> " + idsNggakAda);

				Session session = openReadOnlySession();

				try {

					List<GeneralValueObject> generalValueObjectsData = session.createCriteria(clazz)
							.add(Restrictions.in("id", idsNggakAda)).list();
					for (GeneralValueObject valueObject : generalValueObjectsData) {
						dataJsonMasterSimple.put(valueObject.getId(), valueObject);
						generalValueObjects.add(valueObject);
					}
					generalValueObjectsData = null;

				} catch (Exception e) {

					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/ConstantValues.java:563");
				} finally {
					closeOpenedSession(session);
				}

			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/ConstantValues.java:570");
		}
		return generalValueObjects;
	}
	
	
	@SuppressWarnings({ "rawtypes" })
	public static GeneralValueObject simpleObject(Criteria criteria, Class clazz) {
		return simpleObject(criteria, clazz, true);
	}

	@SuppressWarnings({ "rawtypes" })
	public static GeneralValueObject simpleObject(Criteria criteria, Class clazzx, boolean projection) {
		try {
			if (clazzx != null) {
				String clazzs = StringUtils.split(clazzx.getName(), "_")[0];
				if (ConstantValues.classExist(clazzs)) {
					if (projection) {
						if (clazzs.equals(Tbmuser.class.getName())) {
							criteria.setProjection(Projections.property("userId"));
						} else if (clazzs.equals(Tbmrole.class.getName())) {
							criteria.setProjection(Projections.property("roleId"));
						} else {
							criteria.setProjection(Projections.property("id"));
						}
					}
					Serializable id = (Serializable) criteria.uniqueResult();
					return ConstantValues.ambil(clazzs, id);
				} else {
					Object res = criteria.uniqueResult();
					if (res instanceof GeneralValueObject) {
						return (GeneralValueObject) res;
					}
					return null;
				}
			} else {
				Object res = criteria.uniqueResult();
				if (res instanceof GeneralValueObject) {
					return (GeneralValueObject) res;
				}
				return null;
			}
		} catch (Exception e) {
			// Jangan retry query yang sudah gagal dengan koneksi/state rusak
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	public static Mahasiswa ambilByNim(String nim) {
		// Query LANGSUNG ke DB (2026-08-06, insiden BSI): versi lama HANYA scan cache
		// in-memory (MemoryCacheUtil), yang di-warm-start terbatas mahasiswa 3 tahun
		// angkatan terakhir (lihat InitDataHelper -- Restrictions.gt("tahunangkatan",
		// tahunSekarang-3)). Mahasiswa lebih lama (mis. angkatan 2022 semester 9) TAK
		// PERNAH masuk cache ini sehingga selalu "tidak ditemukan" di inquiry H2H bank,
		// padahal datanya valid di DB. Query langsung dibuat cepat via index fungsional
		// idx_mhs_lower_nim/idx_mhs_lower_nama (InitIndex.initIndexAmbilByNimFallbackSuperFast).
		try {
			if (nim == null || nim.trim().isEmpty()) {
				return null;
			}

			Session session = null;
			try {
				session = HibernateUtil.openSession();
				Mahasiswa mahasiswa = (Mahasiswa) session.createCriteria(Mahasiswa.class)
						.add(Restrictions.eq("nim", nim).ignoreCase()).setMaxResults(1).uniqueResult();
				if (mahasiswa != null) {
					return mahasiswa;
				}
				return (Mahasiswa) session.createCriteria(Mahasiswa.class)
						.add(Restrictions.eq("nama", nim.trim()).ignoreCase()).setMaxResults(1).uniqueResult();
			} finally {
				HibernateUtil.closeSessionQuietly(session);
			}

		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/ConstantValues.java:650");
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public static Siswa ambilByNis(String nis) {
		try {

			if (nis == null || nis.trim().isEmpty()) {
				return null;
			}

			Map<String, GeneralValueObject> b = ais.common.MemoryCacheUtil.get(Siswa.class.getName());
			for (GeneralValueObject generalValueObject : b.values()) {

				try {
					Siswa siswa = (Siswa) generalValueObject;
					if (siswa != null && siswa.getNomorIndukNasional().equalsIgnoreCase(nis)) {
						return siswa;
					}
					if (siswa != null && siswa.getNomorInduk().equalsIgnoreCase(nis)) {
						return siswa;
					}
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/ConstantValues.java:675");
				}
			}

			for (GeneralValueObject generalValueObject : b.values()) {
				try {
					Siswa siswa = (Siswa) generalValueObject;
					if (siswa != null && siswa.getNamaSiswa().equalsIgnoreCase(nis)) {
						return siswa;
					}
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/ConstantValues.java:686");
				}
			}

		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/ConstantValues.java:691");
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public static Siswa ambilByNisAja(String nis) {
		try {

			if (nis == null || nis.trim().isEmpty()) {
				return null;
			}

			Map<String, GeneralValueObject> b = ais.common.MemoryCacheUtil.get(Siswa.class.getName());
			for (GeneralValueObject generalValueObject : b.values()) {

				try {
					Siswa siswa = (Siswa) generalValueObject;
					if (siswa != null && siswa.getNomorInduk().equalsIgnoreCase(nis)) {
						return siswa;
					}
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/ConstantValues.java:713");
				}
			}

		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/ConstantValues.java:718");
		}
		return null;
	}

	/**
	 * Ambil segmen awal nama kelas sebelum tanda "_" dengan AMAN. {@code StringUtils.split("","_")}
	 * mengembalikan array KOSONG sehingga {@code [0]} melempar ArrayIndexOutOfBoundsException: 0
	 * (mis. dipicu notifikasi lonceng dengan {@code classData.name = ""}). Helper ini mengembalikan
	 * {@code clazz} apa adanya bila kosong/null/tanpa segmen — pemanggil akan menemukan "tidak ada"
	 * lewat cache (MemoryCacheUtil.get selalu mengembalikan map non-null) lalu berakhir null,
	 * bukan crash. Dipakai oleh seluruh overload {@code ambil(...)}.
	 */
	private static String ambilSegmenAwalClazz(String clazz) {
		if (clazz == null) {
			return clazz;
		}
		String[] parts = StringUtils.split(clazz, "_");
		return (parts == null || parts.length == 0) ? clazz : parts[0];
	}

	/**
	 * Ambil dari cache MapDB dengan aman. Bila store MapDB SUDAH ditutup (reInit / shutdown),
	 * MapDB melempar {@code java.lang.IllegalAccessError} "already closed" — sebuah {@code Error}
	 * (bukan {@code Exception}) sehingga lolos dari {@code catch (Exception)} biasa dan meng-crash
	 * getter entitas (mis. LogLogin.satuanKerja). Di sini diperlakukan sebagai cache-miss → null,
	 * agar pemanggil memakai nilai lama / fallback DB, bukan gagal fatal.
	 */
	private static GeneralValueObject getDariCacheAman(java.util.Map<Serializable, GeneralValueObject> map,
			Serializable id) {
		if (map == null) {
			return null;
		}
		try {
			return map.get(id);
		} catch (Throwable t) {
			return null;
		}
	}

	/**
	 * BUG FIX (TypeMismatchException): method ambil(...) di bawah ini generik untuk banyak
	 * entity (Tbmuser dsb) dan menerima id sebagai Serializable apa saja (sering Long dari
	 * pemanggil JSON), padahal primary key sebagian entity (mis. Tbmuser.userId) bertipe
	 * String. session.get(clazz, id) dengan tipe id yang salah melempar
	 * org.hibernate.TypeMismatchException. Di sini id dikonversi otomatis ke tipe yang
	 * diharapkan classMetadata sebelum dipassing ke session.get().
	 */
	private static Serializable konversiIdSesuaiTipe(String clazz, Serializable id) {
		if (id == null || clazz == null) {
			return id;
		}
		try {
			ClassMetadata classMetadata = HibernateUtil.getSessionFactory().getClassMetadata(clazz);
			if (classMetadata == null) {
				return id;
			}
			Class expected = classMetadata.getIdentifierType().getReturnedClass();
			if (expected == null || expected.isInstance(id)) {
				return id;
			}
			if (String.class.equals(expected)) {
				return String.valueOf(id);
			}
			if (id instanceof String) {
				String s = ((String) id).trim();
				if (s.length() == 0) {
					return id;
				}
				if (Long.class.equals(expected) || Long.TYPE.equals(expected)) {
					return Long.valueOf(s);
				}
				if (Integer.class.equals(expected) || Integer.TYPE.equals(expected)) {
					return Integer.valueOf(s);
				}
				if (Short.class.equals(expected) || Short.TYPE.equals(expected)) {
					return Short.valueOf(s);
				}
			} else if (id instanceof Number) {
				Number n = (Number) id;
				if (Long.class.equals(expected) || Long.TYPE.equals(expected)) {
					return Long.valueOf(n.longValue());
				}
				if (Integer.class.equals(expected) || Integer.TYPE.equals(expected)) {
					return Integer.valueOf(n.intValue());
				}
				if (Short.class.equals(expected) || Short.TYPE.equals(expected)) {
					return Short.valueOf(n.shortValue());
				}
			}
		} catch (Exception e) {
			// Konversi gagal (mis. format id tak sesuai) -> kembalikan id apa adanya,
			// biar tetap ditangani fallback TypeMismatchException di pemanggil.
			ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/ConstantValues.java:konversiIdSesuaiTipe");
		}
		return id;
	}

	public static GeneralValueObject ambil(String clazz, Serializable id) {
		return ambil(clazz, id, true);
	}

	@SuppressWarnings("unchecked")
	public static GeneralValueObject ambil(String clazz, Serializable id, boolean diambilJikaTidakAda) {
		try {

			if (id == null) {
				return null;
			}
			clazz = ambilSegmenAwalClazz(clazz);
			// Nama entity kosong/blank (mis. notifikasi lonceng dengan classData.name = "")
			// membuat session.get("", id) melempar MappingException "Unknown entity:".
			// Hentikan lebih awal agar tidak ada query bermasalah & log tidak terkotori.
			if (clazz == null || clazz.trim().length() == 0) {
				return null;
			}
			Map<Serializable, GeneralValueObject> dataJsonMasterSimple = ais.common.MemoryCacheUtil.get(clazz);
			// Tbmrole di-preload penuh → cukup ambil dari cache. Tbmuser TIDAK lagi di-preload
			// penuh (mencegah bootstrap hang), jadi WAJIB punya fallback DB di bawah agar login
			// tetap bisa menemukan user yang belum ada di cache.
			if (clazz.equals(Tbmrole.class.getName())) {
				GeneralValueObject generalValueObject = getDariCacheAman(dataJsonMasterSimple, id);
				return generalValueObject;
			} else {
				GeneralValueObject generalValueObject = getDariCacheAman(dataJsonMasterSimple, id);
				if (diambilJikaTidakAda && generalValueObject == null) {

					Session session = openReadOnlySession();

					try {

						Serializable idUntukGet = konversiIdSesuaiTipe(clazz, id);
						try {
							generalValueObject = (GeneralValueObject) session.get(clazz, idUntukGet);
						} catch (TypeMismatchException tme) {
							// Fallback terakhir: id tetap tak cocok tipe setelah konversi -> jangan block
							// proses pemanggil, catat warning dan anggap data tidak ditemukan.
							ais.common.ErrorAuditUtil.record(tme, "auto-audit src/ais/common/ConstantValues.java:ambil-typeMismatch-1");
							generalValueObject = null;
						}
						if (generalValueObject != null) {
							dataJsonMasterSimple.put(id, generalValueObject);
						}

					} catch (Exception e) {

						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/ConstantValues.java:798");
					} finally {
						closeOpenedSession(session);
					}

				}
				return generalValueObject;
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/ConstantValues.java:807");
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public static GeneralValueObject ambil(String clazz, Serializable id, boolean diambilJikaTidakAda,
			Session session) {
		try {

			if (id == null) {
				return null;
			}
			clazz = ambilSegmenAwalClazz(clazz);
			// Lihat catatan keamanan di overload ambil(clazz,id,boolean): cegah MappingException
			// "Unknown entity:" saat nama entity kosong/blank.
			if (clazz == null || clazz.trim().length() == 0) {
				return null;
			}
			Map<Serializable, GeneralValueObject> dataJsonMasterSimple = ais.common.MemoryCacheUtil.get(clazz);
			// Lihat catatan di overload ambil(clazz,id,boolean): Tbmuser kini perlu fallback DB.
			if (clazz.equals(Tbmrole.class.getName())) {
				GeneralValueObject generalValueObject = getDariCacheAman(dataJsonMasterSimple, id);
				return generalValueObject;
			} else {
				GeneralValueObject generalValueObject = getDariCacheAman(dataJsonMasterSimple, id);
				if (diambilJikaTidakAda && generalValueObject == null) {
					if (session == null || !session.isOpen()) {
						return null;
					}
					try {
						Serializable idUntukGet = konversiIdSesuaiTipe(clazz, id);
						try {
							generalValueObject = (GeneralValueObject) session.get(clazz, idUntukGet);
						} catch (TypeMismatchException tme) {
							ais.common.ErrorAuditUtil.record(tme, "auto-audit src/ais/common/ConstantValues.java:ambil-typeMismatch-2");
							generalValueObject = null;
						}
						if (generalValueObject != null) {
							dataJsonMasterSimple.put(id, generalValueObject);
						}
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/ConstantValues.java:843");
					}

				}
				return generalValueObject;
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/ConstantValues.java:850");
		}
		return null;
	}

	public static GeneralValueObject ambilByKode(String clazz, String kode) {
		return ambilByKode(clazz, kode, false);
	}

	@SuppressWarnings("unchecked")
	public static GeneralValueObject ambilByKode(String clazz, String kode, boolean diambilJikaTidakAda) {
		try {

			if (kode == null) {
				return null;
			}
			clazz = ambilSegmenAwalClazz(clazz);
			Map<Serializable, GeneralValueObject> dataJsonMasterSimple = ais.common.MemoryCacheUtil.get(clazz);
			GeneralValueObject generalValueObject = null;
			for (GeneralValueObject a : dataJsonMasterSimple.values()) {
				if (a != null && a.getKode() != null && a.getKode().trim().equalsIgnoreCase(kode.trim())) {
					generalValueObject = a;
					break;
				}
			}

			if (diambilJikaTidakAda) {
				if (generalValueObject == null) {

					Session session = openReadOnlySession();

					try {

						generalValueObject = (GeneralValueObject) session.createCriteria(Class.forName(clazz))
								.setMaxResults(1).addOrder(Order.desc("id")).add(Restrictions.eq("kode", kode))
								.uniqueResult();
						if (generalValueObject != null) {
							dataJsonMasterSimple.put(generalValueObject.getId(), generalValueObject);
						}

					} catch (Exception e) {

						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/ConstantValues.java:892");
					} finally {
						closeOpenedSession(session);

					}
				}
			}

			return generalValueObject;

		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/ConstantValues.java:903");
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public static GeneralValueObject ambilByNama(String clazz, String nama) {
		try {

			if (nama == null) {
				return null;
			}
			clazz = ambilSegmenAwalClazz(clazz);
			Map<Serializable, GeneralValueObject> dataJsonMasterSimple = ais.common.MemoryCacheUtil.get(clazz);
			GeneralValueObject generalValueObject = null;
			for (GeneralValueObject a : dataJsonMasterSimple.values()) {
				if (a != null && a.getNama() != null && a.getNama().trim().equalsIgnoreCase(nama.trim())) {
					generalValueObject = a;
					break;
				}
			}

			return generalValueObject;

		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/ConstantValues.java:928");
		}
		return null;
	}

	

	@SuppressWarnings("unchecked")
	public static void hapus(String clazz, Serializable id) {
		try {

			if (id == null) {
				return;
			}

			clazz = ambilSegmenAwalClazz(clazz);

			Map<Serializable, GeneralValueObject> dataJsonMasterSimple = ais.common.MemoryCacheUtil.get(clazz);
			if (clazz.equals(Tbmuser.class.getName()) || clazz.equals(Tbmrole.class.getName())) {
				GeneralValueObject generalValueObject = dataJsonMasterSimple.remove(id);
				System.out.println("hapus generalValueObject -> " + generalValueObject);
				generalValueObject = null;
				return;
			} else {
				GeneralValueObject generalValueObject = dataJsonMasterSimple.remove(id);
				System.out.println("hapus generalValueObject -> " + generalValueObject);
				generalValueObject = null;
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/ConstantValues.java:957");
		}
	}

	@SuppressWarnings("rawtypes")
	public static Map ambilBerdasarClass(Class clazzx) {
		try {
			String clazzs = StringUtils.split(clazzx.getName(), "_")[0];
			Map map = ais.common.MemoryCacheUtil.get(clazzs);
			if (map == null || map.isEmpty()) {
				return Collections.EMPTY_MAP;
			}
			return sanitizeCacheMap(map);
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/ConstantValues.java:971");
		}
		return Collections.EMPTY_MAP;
	}

	/**
	 * Memastikan nilai map cache berupa entity (GeneralValueObject), bukan
	 * pembungkus internal cache level-2 Hibernate/EhCache
	 * (AbstractReadWriteEhcacheAccessStrategy$Item) yang akan menyebabkan
	 * ClassCastException saat pemanggil meng-cast value ke entity, mis.
	 * SuratUtil.initDefaultKop: (Fakultas) o.
	 *
	 * Pada kondisi sehat (semua value sudah entity) map asli dikembalikan apa
	 * adanya tanpa menyalin sehingga tanpa overhead. Hanya bila terdeteksi value
	 * non-entity, map dibersihkan: nilai Item di-unwrap via refleksi getValue(),
	 * value yang tetap bukan entity dibuang.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static Map sanitizeCacheMap(Map map) {
		boolean perluBersih = false;
		for (Object v : map.values()) {
			if (v != null && !(v instanceof GeneralValueObject)) {
				perluBersih = true;
				break;
			}
		}
		if (!perluBersih) {
			return map;
		}
		java.util.LinkedHashMap clean = new java.util.LinkedHashMap();
		for (Object e : map.entrySet()) {
			java.util.Map.Entry entry = (java.util.Map.Entry) e;
			Object real = unwrapCacheValue(entry.getValue());
			if (real != null) {
				clean.put(entry.getKey(), real);
			}
		}
		return clean;
	}

	private static Object unwrapCacheValue(Object v) {
		if (v == null) {
			return null;
		}
		if (v instanceof GeneralValueObject) {
			return v;
		}
		// EhCache read-write strategy membungkus value dalam Item; ambil isinya
		// lewat refleksi agar tidak bergantung pada tipe internal EhCache.
		try {
			java.lang.reflect.Method m = v.getClass().getMethod("getValue", new Class[0]);
			Object inner = m.invoke(v, new Object[0]);
			if (inner instanceof GeneralValueObject) {
				return inner;
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/ConstantValues.java:1026");
		}
		return null;
	}

	/**
	 * Helper ringan untuk akses values dari MemoryCacheUtil tanpa membuat copy list baru.
	 * Dipakai oleh proses yang sering dipanggil seperti SopUtil.tampilAktor agar tidak
	 * perlu memanggil ambilBerdasarClass(...).values() berulang dengan risiko null.
	 */
	@SuppressWarnings("rawtypes")
	public static Collection ambilBerdasarClassValues(Class clazzx) {
		Map map = ambilBerdasarClass(clazzx);
		return map == null ? Collections.EMPTY_LIST : map.values();
	}

	/**
	 * Ambil Tbmuser langsung dari cache berdasarkan userId. Tidak membuka session baru.
	 * Cocok untuk lookup aktor SOP yang dipanggil berkali-kali pada tampilan workflow.
	 */
	public static Tbmuser ambilTbmuser(String userId) {
		try {
			if (userId == null || userId.trim().isEmpty()) {
				return null;
			}
			// Sebelumnya false (cache-only). Karena Tbmuser tidak lagi di-preload penuh,
			// pakai true agar fallback ke DB lalu di-cache (pemanggilan berikutnya tetap cepat).
			return (Tbmuser) ambil(Tbmuser.class.getName(), userId.trim(), true);
		} catch (Exception e) {
			return null;
		}
	}

	public static void initJumlahTahapan() {

		// Menggunakan openSession() agar terpisah dari session HTTP request
		Session session = HibernateUtil.getSessionFactory().openSession();

		try {
			List<Jurusan> jurusans = ConstantValues.simpleList(session.createCriteria(Jurusan.class), Jurusan.class);

			for (Jurusan jurusan : jurusans) {
				for (String p : Common.programs.keySet()) {
					try {
						Konfigurasi konfigurasi = Common.getKonfigurasi(
								"jumlah_tahapan_perkuliahan_dalam_satu_tahun_akademik" + jurusan.getId() + p, "", p, "",
								"");
						if (!konfigurasi.getNilai().trim().isEmpty()
								&& Common.isNumber(konfigurasi.getNilai().trim())) {
							int tahapan = Integer.parseInt(konfigurasi.getNilai().trim());
							jumlahTahapan.put(p + "_" + jurusan.getId(), tahapan);
						}
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/ConstantValues.java:1078");

					}
				}
			}

			for (String p : Common.programs.keySet()) {
				jumlahTahapan.put(p, 2);
				try {
					Konfigurasi konfigurasi = Common
							.getKonfigurasi("jumlah_tahapan_perkuliahan_dalam_satu_tahun_akademik" + p, "2", p, "", "");
					int tahapan = Integer.parseInt(konfigurasi.getNilai().trim());
					jumlahTahapan.put(p, tahapan);
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/ConstantValues.java:1091");

				}
			}
			jurusans = null;
			System.out.println("jumlahTahapan = " + jumlahTahapan);
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/ConstantValues.java:1098");
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

	public static StatusPertemuan ABSEN;
	public static StatusPertemuan FORM;
	public static StatusPertemuan UTS;
	public static StatusPertemuan UAS;
	public static StatusPertemuan TUGAS_1;
	public static StatusPertemuan TUGAS_2;
	public static StatusPertemuan TUGAS_3;
	public static StatusPertemuan TUGAS_4;
	public static StatusPertemuan TUGAS_5;

	public static StatusPertemuan QUIZ_1;
	public static StatusPertemuan QUIZ_2;
	public static StatusPertemuan QUIZ_3;
	public static StatusPertemuan QUIZ_4;
	public static StatusPertemuan QUIZ_5;
	public static StatusPertemuan UJIAN_ONLINE;
	public static StatusPertemuan TIDAK_HADIR;
	public static StatusPertemuan TATAP_MUKA;
	public static StatusPertemuan DARING;

	public static String lokasiFileTemproraryTemp = "/opt/temporary/" + Common.randLong() + "/";

//	private static int panjangLokasiFileTemprorary = ConstantValues.lokasiFileTemproraryTemp.length();
	private static Map<String, String> lokasiFileTemproraryLain = null;

	public static void reloadTemp() {
		lokasiFileTemproraryLain = new HashMap<String, String>();

		String lokasi_file_temprorary_lain = Common.getKonfigurasi("lokasi_file_temprorary_lain", "").getNilai().trim();
		if (!lokasi_file_temprorary_lain.trim().isEmpty()) {
			for (String s : lokasi_file_temprorary_lain.split(";")) {
				try {
					String[] dataS = StringUtils.split(s, "=");
					String key = dataS[0];
					if (!key.trim().isEmpty()) {
						String path = lokasiFileTemproraryTemp;
						try {
							path = dataS[1];
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/ConstantValues.java:1150");
						}
						lokasiFileTemproraryLain.put(key, path);
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/ConstantValues.java:1154");
					// TODO: handle exception
				}
			}
		}
	}

	@SuppressWarnings("rawtypes")
	public static String ambilLokasiFileTemprorary(Class clazz) {
		String clazzName = clazz == null ? "" : clazz.getSimpleName();

		if (lokasiFileTemproraryLain == null) {
			reloadTemp();
		}

		String path = lokasiFileTemproraryLain.get(clazzName);
		if (path == null || path.trim().isEmpty()) {
			path = lokasiFileTemproraryTemp;
		}
		return path;
	}

	public static Jenjang s2T;

	public static Jenjang s3T;

	public static Poly POLI_UGD;

	public static JenisPasien PASIEN_ASURANSI;
	public static JenisPasien PASIEN_UMUM;
	public static JenisPasien PASIEN_DINAS;
	public static JenisPasien PASIEN_SISWA;

	public static StatusTempatTidur TERSEDIA;
	public static StatusTempatTidur TIDAK_TERSEDIA;
	public static StatusTempatTidur RUSAK;
	public static StatusTempatTidur TELAH_DIPINDAHKAN;
	public static StatusTempatTidur SEDANG_DALAM_PERBAIKAN;

	public static StatusPulang STATUS_PINDAH;
	public static StatusPulang STATUS_MENINGGAL;

	public static JenisTindakan KUNJUNGAN_DOKTER;

	public static Tindakan KUNJUNGAN_RUTIN;

	public static Tindakan PEMBUATAN_KARTU;

	public static Tindakan PENDAFTARAN_PASIEN;

	public static KelasPerawatan kelasNormal;

	/**
	 * Id kelas perawatan "normal" secara AMAN dari null. {@link #kelasNormal} bisa {@code null} bila
	 * data master Kelas Perawatan belum lengkap (mis. instalasi baru) — mengaksesnya langsung
	 * (<code>kelasNormal.getId()</code>) memicu {@link NullPointerException} (mis. di banbox/monitor
	 * tempat tidur, pendaftaran rawat inap, laporan stok). Method ini mengembalikan {@code -1L} bila
	 * belum ada, sehingga kondisi {@code Restrictions.ne("id", ...)} / filter SQL tetap valid (tidak
	 * mengecualikan baris apa pun) tanpa crash.
	 */
	public static Long kelasNormalId() {
		return (kelasNormal != null && kelasNormal.getId() != null) ? kelasNormal.getId() : Long.valueOf(-1L);
	}

	public static void initPembobotanNilai() {
		// 1. Buka Session Baru (Isolated Session)
		// Menggunakan openSession() agar terpisah dari session HTTP request
		Session session = HibernateUtil.getSessionFactory().openSession();

		try {
			session.getTransaction().begin();

			String ob = "Observasi (Praktek / Tugas)";
			String uk = "Unjuk Kerja (Presentasi/Tugas)";
			String uts = "Tes Tulis (UTS)";
			String uas = "Tes Tulis (UAS)";
			String ul = "Tes Lisan (Tugas Kelompok)";

			String[] OBES = new String[] { ob, uk, uts, uas, ul };

			for (String o : OBES) {
				// Cek apakah status pertemuan dengan nama tersebut sudah ada di database
				Number count = (Number) session.createCriteria(StatusPertemuan.class).add(Restrictions.eq("nama", o))
						.setProjection(Projections.rowCount()).uniqueResult();

				// Hanya lakukan proses simpan jika data belum ada (count == 0)
				if (count == null || count.intValue() == 0) {
					StatusPertemuan obeData = new StatusPertemuan();
					obeData.setId(Common.randLong());
					obeData.setNama(o);
					obeData.setObe(true);
					try {
						session.getTransaction().begin();
						session.save(obeData);
						session.getTransaction().commit();
					} catch (Exception e) {
						if (session.getTransaction().isActive()) {
							session.getTransaction().rollback();
						}
						// e.printStackTrace();
					}
				}
			}

			ABSEN = (StatusPertemuan) session.createCriteria(StatusPertemuan.class).add(Restrictions.idEq(1L))
					.uniqueResult();

			FORM = (StatusPertemuan) session.createCriteria(StatusPertemuan.class).add(Restrictions.idEq(2L))
					.uniqueResult();

			UTS = (StatusPertemuan) session.createCriteria(StatusPertemuan.class).add(Restrictions.idEq(3L))
					.uniqueResult();
			UAS = (StatusPertemuan) session.createCriteria(StatusPertemuan.class).add(Restrictions.idEq(4L))
					.uniqueResult();

			if (ABSEN == null) {
				ABSEN = new StatusPertemuan();
				ABSEN.setId(1L);
				ABSEN.setNama("Absen");
				// session.getTransaction().begin();
				session.save(ABSEN);
				// session.getTransaction().commit();
			}

			if (ABSEN.getAutoCreate() == null) {
				ABSEN.setAutoCreate(true);
				session.update(ABSEN);
			}

			if (ABSEN.getAktif() == null) {
				ABSEN.setAktif(false);
				session.update(ABSEN);
			}

			if (FORM == null) {
				FORM = new StatusPertemuan();
				FORM.setId(2L);
				FORM.setNama("Formatif");
				// session.getTransaction().begin();
				session.save(FORM);
				// session.getTransaction().commit();
			}

			if (FORM.getAutoCreate() == null) {
				FORM.setAutoCreate(true);
				session.update(FORM);
			}

			if (FORM.getAktif() == null) {
				FORM.setAktif(true);
				session.update(FORM);
			}

			if (UTS == null) {
				UTS = new StatusPertemuan();
				UTS.setId(3L);
				UTS.setNama("UTS");
				// session.getTransaction().begin();
				session.save(UTS);
				// session.getTransaction().commit();
			}

			if (UTS.getAutoCreate() == null) {
				UTS.setAutoCreate(true);
				session.update(UTS);
			}

			if (UTS.getAktif() == null) {
				UTS.setAktif(true);
				session.update(UTS);
			}

			if (UAS == null) {
				UAS = new StatusPertemuan();
				UAS.setId(4L);
				UAS.setNama("UAS");
				// session.getTransaction().begin();
				session.save(UAS);
				// session.getTransaction().commit();
			}

			if (UAS.getAutoCreate() == null) {
				UAS.setAutoCreate(true);
				session.update(UAS);
			}

			if (UAS.getAktif() == null) {
				UAS.setAktif(true);
				session.update(UAS);
			}

			TUGAS_1 = (StatusPertemuan) session.createCriteria(StatusPertemuan.class).add(Restrictions.idEq(21L))
					.uniqueResult();
			if (TUGAS_1 == null) {
				TUGAS_1 = new StatusPertemuan();
				TUGAS_1.setId(21L);
				TUGAS_1.setNama("Tugas 1");
				// session.getTransaction().begin();
				session.save(TUGAS_1);
				// session.getTransaction().commit();
			}

			if (TUGAS_1.getAutoCreate() == null) {
				TUGAS_1.setAutoCreate(true);
				session.update(TUGAS_1);
			}

			if (TUGAS_1.getAktif() == null) {
				TUGAS_1.setAktif(false);
				session.update(TUGAS_1);
			}

			TUGAS_2 = (StatusPertemuan) session.createCriteria(StatusPertemuan.class).add(Restrictions.idEq(22L))
					.uniqueResult();
			if (TUGAS_2 == null) {
				TUGAS_2 = new StatusPertemuan();
				TUGAS_2.setId(22L);
				TUGAS_2.setNama("Tugas 2");
				// session.getTransaction().begin();
				session.save(TUGAS_2);
				// session.getTransaction().commit();
			}

			if (TUGAS_2.getAutoCreate() == null) {
				TUGAS_2.setAutoCreate(true);
				session.update(TUGAS_2);
			}

			if (TUGAS_2.getAktif() == null) {
				TUGAS_2.setAktif(false);
				session.update(TUGAS_2);
			}

			TUGAS_3 = (StatusPertemuan) session.createCriteria(StatusPertemuan.class).add(Restrictions.idEq(23L))
					.uniqueResult();
			if (TUGAS_3 == null) {
				TUGAS_3 = new StatusPertemuan();
				TUGAS_3.setId(23L);
				TUGAS_3.setNama("Tugas 3");
				// session.getTransaction().begin();
				session.save(TUGAS_3);
				// session.getTransaction().commit();
			}

			if (TUGAS_3.getAutoCreate() == null) {
				TUGAS_3.setAutoCreate(true);
				session.update(TUGAS_3);
			}

			if (TUGAS_3.getAktif() == null) {
				TUGAS_3.setAktif(false);
				session.update(TUGAS_3);
			}

			TUGAS_4 = (StatusPertemuan) session.createCriteria(StatusPertemuan.class).add(Restrictions.idEq(24L))
					.uniqueResult();
			if (TUGAS_4 == null) {
				TUGAS_4 = new StatusPertemuan();
				TUGAS_4.setId(24L);
				TUGAS_4.setNama("Tugas 4");
				// session.getTransaction().begin();
				session.save(TUGAS_4);
				// session.getTransaction().commit();
			}

			if (TUGAS_4.getAutoCreate() == null) {
				TUGAS_4.setAutoCreate(true);
				session.update(TUGAS_4);
			}

			if (TUGAS_4.getAktif() == null) {
				TUGAS_4.setAktif(false);
				session.update(TUGAS_4);
			}

			TUGAS_5 = (StatusPertemuan) session.createCriteria(StatusPertemuan.class).add(Restrictions.idEq(25L))
					.uniqueResult();
			if (TUGAS_5 == null) {
				TUGAS_5 = new StatusPertemuan();
				TUGAS_5.setId(25L);
				TUGAS_5.setNama("Tugas 5");
				// session.getTransaction().begin();
				session.save(TUGAS_5);
				// session.getTransaction().commit();
			}

			if (TUGAS_5.getAutoCreate() == null) {
				TUGAS_5.setAutoCreate(true);
				session.update(TUGAS_5);
			}

			if (TUGAS_5.getAktif() == null) {
				TUGAS_5.setAktif(false);
				session.update(TUGAS_5);
			}

			QUIZ_1 = (StatusPertemuan) session.createCriteria(StatusPertemuan.class).add(Restrictions.idEq(31L))
					.uniqueResult();
			if (QUIZ_1 == null) {
				QUIZ_1 = new StatusPertemuan();
				QUIZ_1.setId(31L);
				QUIZ_1.setNama("Quiz 1");
				// session.getTransaction().begin();
				session.save(QUIZ_1);
				// session.getTransaction().commit();
			}

			if (QUIZ_1.getAutoCreate() == null) {
				QUIZ_1.setAutoCreate(true);
				session.update(QUIZ_1);
			}

			if (QUIZ_1.getAktif() == null) {
				QUIZ_1.setAktif(false);
				session.update(QUIZ_1);
			}

			QUIZ_2 = (StatusPertemuan) session.createCriteria(StatusPertemuan.class).add(Restrictions.idEq(32L))
					.uniqueResult();
			if (QUIZ_2 == null) {
				QUIZ_2 = new StatusPertemuan();
				QUIZ_2.setId(32L);
				QUIZ_2.setNama("Quiz 2");
				// session.getTransaction().begin();
				session.save(QUIZ_2);
				// session.getTransaction().commit();
			}

			if (QUIZ_2.getAutoCreate() == null) {
				QUIZ_2.setAutoCreate(true);
				session.update(QUIZ_2);
			}

			if (QUIZ_2.getAktif() == null) {
				QUIZ_2.setAktif(false);
				session.update(QUIZ_2);
			}

			QUIZ_3 = (StatusPertemuan) session.createCriteria(StatusPertemuan.class).add(Restrictions.idEq(33L))
					.uniqueResult();
			if (QUIZ_3 == null) {
				QUIZ_3 = new StatusPertemuan();
				QUIZ_3.setId(33L);
				QUIZ_3.setNama("Quiz 3");
				// session.getTransaction().begin();
				session.save(QUIZ_3);
				// session.getTransaction().commit();
			}

			if (QUIZ_3.getAutoCreate() == null) {
				QUIZ_3.setAutoCreate(true);
				session.update(QUIZ_3);
			}

			if (QUIZ_3.getAktif() == null) {
				QUIZ_3.setAktif(false);
				session.update(QUIZ_3);
			}

			QUIZ_4 = (StatusPertemuan) session.createCriteria(StatusPertemuan.class).add(Restrictions.idEq(34L))
					.uniqueResult();
			if (QUIZ_4 == null) {
				QUIZ_4 = new StatusPertemuan();
				QUIZ_4.setId(34L);
				QUIZ_4.setNama("Quiz 4");
				// session.getTransaction().begin();
				session.save(QUIZ_4);
				// session.getTransaction().commit();
			}

			if (QUIZ_4.getAutoCreate() == null) {
				QUIZ_4.setAutoCreate(true);
				session.update(QUIZ_4);
			}

			if (QUIZ_4.getAktif() == null) {
				QUIZ_4.setAktif(false);
				session.update(QUIZ_4);
			}

			QUIZ_5 = (StatusPertemuan) session.createCriteria(StatusPertemuan.class).add(Restrictions.idEq(35L))
					.uniqueResult();
			if (QUIZ_5 == null) {
				QUIZ_5 = new StatusPertemuan();
				QUIZ_5.setId(35L);
				QUIZ_5.setNama("Quiz 5");
				// session.getTransaction().begin();
				session.save(QUIZ_5);
				// session.getTransaction().commit();
			}

			if (QUIZ_5.getAutoCreate() == null) {
				QUIZ_5.setAutoCreate(true);
				session.update(QUIZ_5);
			}

			if (QUIZ_5.getAktif() == null) {
				QUIZ_5.setAktif(false);
				session.update(QUIZ_5);
			}

			UJIAN_ONLINE = (StatusPertemuan) session.createCriteria(StatusPertemuan.class).add(Restrictions.idEq(135L))
					.uniqueResult();
			if (UJIAN_ONLINE == null) {
				UJIAN_ONLINE = new StatusPertemuan();
				UJIAN_ONLINE.setId(135L);
				UJIAN_ONLINE.setNama("Ujian Online");
				UJIAN_ONLINE.setAktif(true);
				// session.getTransaction().begin();
				session.save(UJIAN_ONLINE);
				// session.getTransaction().commit();
			}

			if (UJIAN_ONLINE.getAutoCreate() == null) {
				UJIAN_ONLINE.setAutoCreate(true);
				session.update(UJIAN_ONLINE);
			}

			TIDAK_HADIR = (StatusPertemuan) session.createCriteria(StatusPertemuan.class).add(Restrictions.idEq(235L))
					.uniqueResult();
			if (TIDAK_HADIR == null) {
				TIDAK_HADIR = new StatusPertemuan();
				TIDAK_HADIR.setId(235L);
				TIDAK_HADIR.setNama("Tidak Hadir");
				TIDAK_HADIR.setAktif(true);
				// session.getTransaction().begin();
				session.save(TIDAK_HADIR);
				// session.getTransaction().commit();
			}

			if (TIDAK_HADIR.getAutoCreate() == null) {
				TIDAK_HADIR.setAutoCreate(true);
				session.update(TIDAK_HADIR);
			}

			TATAP_MUKA = (StatusPertemuan) session.createCriteria(StatusPertemuan.class).add(Restrictions.idEq(236L))
					.uniqueResult();
			if (TATAP_MUKA == null) {
				TATAP_MUKA = new StatusPertemuan();
				TATAP_MUKA.setId(236L);
				TATAP_MUKA.setNama("Tatap Muka");
				TATAP_MUKA.setAktif(true);
				// session.getTransaction().begin();
				session.save(TATAP_MUKA);
				// session.getTransaction().commit();
			}

			if (TATAP_MUKA.getAutoCreate() == null) {
				TATAP_MUKA.setAutoCreate(true);
				session.update(TATAP_MUKA);
			}

			DARING = (StatusPertemuan) session.createCriteria(StatusPertemuan.class).add(Restrictions.idEq(1236L))
					.uniqueResult();
			if (DARING == null) {
				DARING = new StatusPertemuan();
				DARING.setId(1236L);
				DARING.setNama("Daring");
				DARING.setAktif(true);
				// session.getTransaction().begin();
				session.save(DARING);
				// session.getTransaction().commit();
			}

			if (DARING.getAutoCreate() == null) {
				DARING.setAutoCreate(true);
				session.update(DARING);
			}
			session.getTransaction().commit();
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/ConstantValues.java:1612");
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

//	public static void init() {
//		init(HibernateUtil.currentSession());
//	}

	public static String tampilkanUkurantemporary() {
		String string = "";

		System.out.println("string => " + string);

		return string;
	}

	public static volatile boolean hasInitdata = false;

	public static List<NilaiHuruf> nilaiHurufs = new ArrayList<NilaiHuruf>();

	// Indeks O(1) untuk lulusDariNilaiHuruf/nilaiHurufTerkait (DIPANGGIL SANGAT SERING di banyak tempat).
	// Dibangun SEKALI saat realoadNilaiHuruf (dan lazy bila belum siap). key = huruf UPPERCASE -> Bucket
	// {perJurusan[jurusanId], perFakultas[fakultasId], global}. Snapshot immutable, dipublish via volatile.
	private static final Object nilaiHurufIndexLock = new Object();
	private static volatile Map<String, NilaiHurufBucket> nilaiHurufIndex = Collections.emptyMap();

	private static final class NilaiHurufBucket {
		Map<Long, NilaiHuruf> perJurusan;
		Map<Long, NilaiHuruf> perFakultas;
		NilaiHuruf global;
	}

	@SuppressWarnings("unchecked")
	public static void realoadNilaiHuruf(Session session) {
		nilaiHurufs = session.createCriteria(NilaiHuruf.class).addOrder(Order.desc("kodeMk"))
				.addOrder(Order.desc("tahunAngkatan")).addOrder(Order.desc("ta")).addOrder(Order.desc("mulai")).list();
		bangunIndexNilaiHuruf();

		System.out.println("loading data " + NilaiHuruf.class.getName() + " sebanyak " + nilaiHurufs.size());
	}

	/**
	 * Bangun indeks huruf->Bucket dari nilaiHurufs. Entri PERTAMA per (huruf,jurusan)/(huruf,fakultas)/
	 * (huruf,global) yang menang -> selaras urutan list (kodeMk/angkatan/ta/mulai desc) & scan-linear lama.
	 * Dipanggil saat reload + lazy. Thread-safe (synchronized); publish volatile.
	 */
	private static void bangunIndexNilaiHuruf() {
		synchronized (nilaiHurufIndexLock) {
			List<NilaiHuruf> list = nilaiHurufs;
			Map<String, NilaiHurufBucket> idx = new HashMap<String, NilaiHurufBucket>();
			if (list != null) {
				for (NilaiHuruf h : list) {
					try {
						if (h == null || h.getNilaiHuruf() == null) {
							continue;
						}
						String key = h.getNilaiHuruf().trim().toUpperCase();
						if (key.isEmpty()) {
							continue;
						}
						NilaiHurufBucket b = idx.get(key);
						if (b == null) {
							b = new NilaiHurufBucket();
							idx.put(key, b);
						}
						Long jurId = h.getJurusan() != null ? h.getJurusan().getId() : null;
						Long fakId = h.getFakultas() != null ? h.getFakultas().getId() : null;
						if (jurId != null) {
							if (b.perJurusan == null) {
								b.perJurusan = new HashMap<Long, NilaiHuruf>();
							}
							if (!b.perJurusan.containsKey(jurId)) {
								b.perJurusan.put(jurId, h);
							}
						}
						if (fakId != null) {
							if (b.perFakultas == null) {
								b.perFakultas = new HashMap<Long, NilaiHuruf>();
							}
							if (!b.perFakultas.containsKey(fakId)) {
								b.perFakultas.put(fakId, h);
							}
						}
						if (jurId == null && fakId == null && b.global == null) {
							b.global = h;
						}
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/ConstantValues.java:1708");
					}
				}
			}
			nilaiHurufIndex = idx;
		}
	}

	/**
	 * Flag lulus dari KONFIGURASI NilaiHuruf yang cocok dengan huruf yang diperoleh untuk mahasiswa tsb
	 * (prioritas per Jurusan -> Fakultas -> global). null bila huruf kosong / tak ada konfigurasi cocok.
	 * Lookup O(1) via indeks in-memory (tanpa query DB). Dipakai getLulus() entitas nilai agar status
	 * lulus mengikuti master Nilai Huruf (checkbox Lulus), bukan tebakan string. Ringan (dipanggil sering).
	 */
	public static Boolean lulusDariNilaiHuruf(String nilaiHuruf, Mahasiswa mahasiswa) {
		NilaiHuruf cfg = nilaiHurufTerkait(nilaiHuruf, mahasiswa);
		return cfg == null ? null : cfg.getLulus();
	}

	/**
	 * Apakah status Lulus/Tidak Lulus untuk huruf ini BOLEH DITAMPILKAN (checkbox master NilaiHuruf
	 * "Tampilkan status lulus/tidak lulus") bagi mahasiswa tsb. Prioritas konfigurasi per
	 * Jurusan -&gt; Fakultas -&gt; global, lookup O(1) via indeks in-memory. Mengembalikan
	 * {@code true} secara DEFAULT (status lulus TAMPIL) kecuali admin secara eksplisit mengosongkan
	 * centang pada konfigurasi Nilai Huruf yang cocok (mis. untuk jenjang tertentu). Tanpa
	 * konfigurasi cocok pun tetap {@code true}. Dipakai kode tampilan (Dasbor Studi, Daftar
	 * Historis MK, dll.).
	 */
	public static Boolean tampilkanStatusLulusDariNilaiHuruf(String nilaiHuruf, Mahasiswa mahasiswa) {
		NilaiHuruf cfg = nilaiHurufTerkait(nilaiHuruf, mahasiswa);
		// DEFAULT TAMPIL (true): status lulus tetap tampil KECUALI admin secara eksplisit
		// mengosongkan centang pada konfigurasi Nilai Huruf yang cocok. Tanpa konfigurasi cocok
		// (cfg null) atau nilai belum diset (null) -> tetap TAMPIL.
		return cfg == null || cfg.getTampilkanStatusLulus() == null ? Boolean.TRUE : cfg.getTampilkanStatusLulus();
	}

	/**
	 * NilaiHuruf (konfigurasi) yang cocok dgn huruf ini utk mahasiswa: per Jurusan -> Fakultas -> global.
	 * Lookup O(1) memakai indeks (nilaiHurufIndex); lazy-build bila indeks belum siap.
	 */
	public static NilaiHuruf nilaiHurufTerkait(String nilaiHuruf, Mahasiswa mahasiswa) {
		if (nilaiHuruf == null) {
			return null;
		}
		String key = nilaiHuruf.trim().toUpperCase();
		if (key.isEmpty()) {
			return null;
		}
		Map<String, NilaiHurufBucket> idx = nilaiHurufIndex;
		if (idx.isEmpty() && nilaiHurufs != null && !nilaiHurufs.isEmpty()) {
			bangunIndexNilaiHuruf();
			idx = nilaiHurufIndex;
		}
		NilaiHurufBucket b = idx.get(key);
		if (b == null) {
			return null;
		}
		Long jurId = null;
		Long fakId = null;
		try {
			if (mahasiswa != null && mahasiswa.getJurusan() != null) {
				jurId = mahasiswa.getJurusan().getId();
				if (mahasiswa.getJurusan().getFakultas() != null) {
					fakId = mahasiswa.getJurusan().getFakultas().getId();
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/ConstantValues.java:1774");
		}
		if (jurId != null && b.perJurusan != null) {
			NilaiHuruf nh = b.perJurusan.get(jurId);
			if (nh != null) {
				return nh;
			}
		}
		if (fakId != null && b.perFakultas != null) {
			NilaiHuruf nh = b.perFakultas.get(fakId);
			if (nh != null) {
				return nh;
			}
		}
		return b.global;
	}

	public static List<NilaiHurufSekolah> nilaiHurufSekolahs = new ArrayList<NilaiHurufSekolah>();

	@SuppressWarnings("unchecked")
	public static void realoadNilaiHurufSekolah(Session session) {
		nilaiHurufSekolahs = session.createCriteria(NilaiHurufSekolah.class).addOrder(Order.desc("tahunAngkatan"))
				.addOrder(Order.desc("ta")).addOrder(Order.desc("mulai")).list();

		System.out.println(
				"loading data " + NilaiHurufSekolah.class.getName() + " sebanyak " + nilaiHurufSekolahs.size());
	}

	public static Tbmrole tbmroleMahasiswa;
	public static Tbmrole tbmrolePenyedia;
	public static Tbmrole tbmroleUmum;
	public static Tbmrole tbmroleSiswa;
	public static Tbmrole tbmrolePenduduk;
	public static Tbmrole tbmroleCalonPegawai;

	public static JenisAktfitasMahasiswa KEGIATAN_KEMAHASISWAAN;
	public static JenisAktfitasMahasiswa KOMPETENSI;
	public static JenisAktfitasMahasiswa KREATIFITAS;
	public static JenisAktfitasMahasiswa KKN;
	public static JenisAktfitasMahasiswa PKL;

	public static volatile boolean selesaiInit = false;

	private static final Object INIT_DATA_LOCK = new Object();
	private static volatile boolean initDataWorkerStarted = false;
	private static volatile long lastInitDataRequestMillis = 0L;
	private static final long INIT_DATA_REQUEST_INTERVAL_MILLIS = 5000L;

	public static void initKehadiran(Session session) {
		MASUK = (Statusabsensi) session.createCriteria(Statusabsensi.class).add(Restrictions.idEq(1L)).uniqueResult();
		TIDAK_ADA_ALASAN = (Statusabsensi) session.createCriteria(Statusabsensi.class).add(Restrictions.idEq(2L))
				.uniqueResult();
		SAKIT = (Statusabsensi) session.createCriteria(Statusabsensi.class).add(Restrictions.idEq(3L)).uniqueResult();
		IZIN = (Statusabsensi) session.createCriteria(Statusabsensi.class).add(Restrictions.idEq(4L)).uniqueResult();
		BELUM_ABSEN = (Statusabsensi) session.createCriteria(Statusabsensi.class).add(Restrictions.idEq(5L))
				.uniqueResult();
		STATUS_CUTI = (Statusabsensi) session.createCriteria(Statusabsensi.class).add(Restrictions.idEq(6L))
				.uniqueResult();
		CUTI_HAMIL = (Statusabsensi) session.createCriteria(Statusabsensi.class).add(Restrictions.idEq(7L))
				.uniqueResult();

		DINAS_LUAR = (Statusabsensi) session.createCriteria(Statusabsensi.class).add(Restrictions.idEq(8L))
				.uniqueResult();

		listAbsenMahasiswa.clear();
		listAbsenMahasiswa.add(MASUK);
		listAbsenMahasiswa.add(SAKIT);
		listAbsenMahasiswa.add(IZIN);
		listAbsenMahasiswa.add(TIDAK_ADA_ALASAN);
		listAbsenMahasiswa.add(BELUM_ABSEN);
	}

	private static volatile String chekStatic = "";

	public static boolean googleBookAktif = true;

	private static boolean shouldStartInitData(boolean paksa) {
		if (paksa) {
			return !InitData.isRunning();
		}
		if (initDataWorkerStarted || InitData.isRunning()) {
			return false;
		}
		if (selesaiInit || InitData.isInitialized()) {
			return false;
		}
		long now = System.currentTimeMillis();
		return (now - lastInitDataRequestMillis) > INIT_DATA_REQUEST_INTERVAL_MILLIS;
	}

	private static void startInitDataAsync(final boolean tetapProses) {
		synchronized (INIT_DATA_LOCK) {
			if (!shouldStartInitData(tetapProses)) {
				return;
			}
			initDataWorkerStarted = true;
			lastInitDataRequestMillis = System.currentTimeMillis();
		}

		Thread thread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					InitData.initData(tetapProses);
				} catch (Throwable e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/ConstantValues.java:1879");
				} finally {
					synchronized (INIT_DATA_LOCK) {
						initDataWorkerStarted = false;
					}
				}
			}
		}, tetapProses ? "ais-cache-reinit" : "ais-cache-init");
		thread.setDaemon(true);
		thread.start();
	}

	public static void reInitDataDiMemory(final boolean tetapProses) {
		String wkt = Common.timeFormat2.get().format(WaktuUtil.getDate());
		boolean lanjut = false;

		synchronized (INIT_DATA_LOCK) {
			if (tetapProses || !wkt.equalsIgnoreCase(chekStatic)) {
				chekStatic = wkt;
				lanjut = true;
			}
		}

		if (!lanjut && !tetapProses) {
			return;
		}

		if (tetapProses) {
			try {
				Map<String, String> dataClass = MemoryDbUtil.getDataClass();
				if (dataClass != null) {
					dataClass.clear();
				}
				MemoryDbUtil.resetLocalReferences();
				InitData.resetInitFlag();
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/ConstantValues.java:1915");
			}
		}

		startInitDataAsync(tetapProses);
	}

	public static void initData(Session session) {
		if (hasInitdata) {
			return;
		}
		synchronized (INIT_DATA_LOCK) {
			if (hasInitdata) {
				return;
			}
			hasInitdata = true;
		}
		reInitDataDiMemory(false);
	}

	public static void init() {
		startInitDataAsync(false);
	}

	public static void init(Session session) {
		if (hasbeeninit) {
			return;
		}
		synchronized (INIT_DATA_LOCK) {
			if (hasbeeninit) {
				return;
			}
			hasbeeninit = true;
		}

		try {
			Common.getRequestHostWithProtocol();
			Common.getRequestHostWithProtocolSimple();
			if (session == null) {
				session = HibernateUtil.currentSession();
			}

			initData(session);
			TemplateFormatBimbingan.initDefault(session);
		} catch (Exception e) {
			hasbeeninit = false;
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public static void initNativeSesion() {
		Session session = null;
		Transaction transaction = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			transaction = session.beginTransaction();
			ConstantValues.init(session);
			transaction.commit();
		} catch (Exception e) {
			if (transaction != null && transaction.isActive()) {
				try {
					transaction.rollback();
				} catch (Exception rollbackEx) {
					rollbackEx.printStackTrace(); ais.common.ErrorAuditUtil.record(rollbackEx, "auto-audit src/ais/common/ConstantValues.java:1978");
				}
			}
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/ConstantValues.java:1981");
		} finally {
			if (session != null && session.isOpen()) {
				try {
					session.disconnect();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/ConstantValues.java:1986");
				}
				try {
					session.close();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/ConstantValues.java:1990");
				}
			}
			HibernateUtil.closeSession();
		}
	}

	public static void checkStaff(String jenis, String nama, String nip) {
		Session session = HibernateUtil.currentSession();
		Staff staff = (Staff) session.createCriteria(Staff.class).add(Restrictions.eq("staff", jenis)).setMaxResults(1)
				.uniqueResult();
		if (staff == null) {
			staff = new Staff();
			staff.setStaff(jenis);
			staff.setNama(nama);
			staff.setNip(nip);
			session.save(staff);
		}

	}

}
