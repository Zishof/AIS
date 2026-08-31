package ais.common;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.Konfigurasi;
import ais.database.model.akunting.Devisi;
import ais.database.model.sirs.AlatMedis;
import ais.database.model.sirs.Asuransi;
import ais.database.model.sirs.Bagian;
import ais.database.model.sirs.BahanBakuItem;
import ais.database.model.sirs.BiayaAlatMedisPerKelas;
import ais.database.model.sirs.BiayaTindakanPerKelas;
import ais.database.model.sirs.Diskon;
import ais.database.model.sirs.DiskonDetail;
import ais.database.model.sirs.Dokter;
import ais.database.model.sirs.Dusun;
import ais.database.model.sirs.GenerikItem;
import ais.database.model.sirs.Gudang;
import ais.database.model.sirs.Icd;
import ais.database.model.sirs.Instalasi;
import ais.database.model.sirs.ItemMedis;
import ais.database.model.sirs.JadwalDokter;
import ais.database.model.sirs.JenisAlatMedis;
import ais.database.model.sirs.JenisBiaya;
import ais.database.model.sirs.JenisBiayaLain;
import ais.database.model.sirs.JenisItemMedis;
import ais.database.model.sirs.JenisPasien;
import ais.database.model.sirs.JenisPenyakit;
import ais.database.model.sirs.JenisRacikan;
import ais.database.model.sirs.JenisTindakan;
import ais.database.model.sirs.Kamar;
import ais.database.model.sirs.Kecamatan;
import ais.database.model.sirs.KelasItem;
import ais.database.model.sirs.KelasPerawatan;
import ais.database.model.sirs.KelompokItem;
import ais.database.model.sirs.Kelurahan;
import ais.database.model.sirs.KodeTransaksiMedis;
import ais.database.model.sirs.Komunitas;
import ais.database.model.sirs.KonversiSatuanItem;
import ais.database.model.sirs.PajakDetail;
import ais.database.model.sirs.PajakMedis;
import ais.database.model.sirs.PaketPerawatanDetail;
import ais.database.model.sirs.Pasien;
import ais.database.model.sirs.Pemeriksaan;
import ais.database.model.sirs.Pendaftaran;
import ais.database.model.sirs.Poly;
import ais.database.model.sirs.PrioritasPasien;
import ais.database.model.sirs.Produksi;
import ais.database.model.sirs.ProduksiDetail;
import ais.database.model.sirs.Racikan;
import ais.database.model.sirs.Satker;
import ais.database.model.sirs.SatuanItem;
import ais.database.model.sirs.Shift;
import ais.database.model.sirs.StatusPulang;
import ais.database.model.sirs.StatusTempatTidur;
import ais.database.model.sirs.TarifKhusus;
import ais.database.model.sirs.TarifKhususPunyaAlatMedis;
import ais.database.model.sirs.TarifKhususPunyaItem;
import ais.database.model.sirs.TarifKhususPunyaTindakan;
import ais.database.model.sirs.TempatTidur;
import ais.database.model.sirs.Tindakan;
import ais.database.model.sirs.TindakanLabDetail;

/**
 * Rutin inisialisasi data awal (seed/bootstrap) untuk modul Sistem Informasi Rumah Sakit (SIRS)
 * di AIS. Dijalankan sekali pada titik startup aplikasi (atau pemicu sejenis) untuk memastikan
 * seluruh entitas master data rumah sakit sudah terdaftar di {@code InitDataHelper} dan bahwa
 * konstanta acuan yang dipakai luas oleh modul SIRS (mis. jenis instalasi, status tempat tidur
 * baku, jenis pasien baku, kode transaksi medis) sudah tersedia sebagai referensi statis di
 * {@link ConstantValues} — mengikuti pola "get-or-create": baca dulu dari database, dan hanya
 * buat baris baru bila belum ada.
 *
 * <p>
 * Modul SIRS bersifat opsional per instalasi AIS (mis. hanya dipakai institusi yang mempunyai unit
 * kesehatan/rumah sakit) — seluruh inisialisasi di kelas ini hanya berjalan bila konfigurasi
 * {@code aktifkan_modul_rumah_sakit} bernilai aktif, sehingga instalasi AIS tanpa modul rumah sakit
 * tidak dibebani data/konstanta yang tidak relevan.
 * </p>
 *
 * <p>
 * Kelas ini murni statis (tidak ada instance state) dan sepenuhnya merupakan kode infrastruktur/
 * bootstrap — seluruh method privat di dalamnya adalah helper generik Hibernate (get-by-id,
 * get-by-property, save-dalam-transaksi) yang secara sengaja ditulis kompatibel dengan
 * Java 1.7 (generic method sederhana, tanpa lambda/stream).
 * </p>
 */
public class InitSirs {

	/**
	 * Titik masuk utama inisialisasi modul SIRS. Membaca konfigurasi
	 * {@code aktifkan_modul_rumah_sakit}; bila aktif, menjalankan inisialisasi master data
	 * (lewat {@link #initMasterDataClasses()}) diikuti inisialisasi parameter/konstanta khusus
	 * (lewat {@link #initDataParam()}). Bila konfigurasi tidak ditemukan atau tidak aktif,
	 * method ini tidak melakukan apa pun (no-op) — aman dipanggil pada instalasi AIS yang tidak
	 * memakai modul rumah sakit.
	 */
	public static void initData() {
		// Cek konfigurasi sebelum inisialisasi
		Konfigurasi config = Common.getKonfigurasi("aktifkan_modul_rumah_sakit", Konfigurasi.TIDAK_AKTIF);
		if (config != null && Konfigurasi.AKTIF.equals(config.getNilai())) {

			// Inisialisasi Master Data Dasar
			initMasterDataClasses();

			// Inisialisasi Parameter Khusus
			initDataParam();
		}
	}

	/**
	 * Mendaftarkan seluruh kelas entitas master data SIRS (kamar, tempat tidur, alat medis,
	 * tindakan, item medis, dsb. — lebih dari 50 kelas) ke {@link InitDataHelper#initData(Class)}
	 * agar mekanisme inisialisasi data generik (mis. pembuatan struktur/data dasar per entitas)
	 * milik {@code InitDataHelper} berjalan untuk masing-masing kelas tersebut. Method ini
	 * murni daftar deklaratif class literal yang diiterasi; logika inisialisasi sesungguhnya
	 * ada di {@link InitDataHelper}, bukan di sini.
	 */
	private static void initMasterDataClasses() {
		Class<?>[] classesToInit = { Kamar.class, TempatTidur.class, JenisAlatMedis.class, AlatMedis.class,
				KelasPerawatan.class, Asuransi.class, Devisi.class, Bagian.class, JenisBiaya.class,
				BiayaTindakanPerKelas.class, Tindakan.class, TarifKhususPunyaTindakan.class, TarifKhusus.class,
				JenisTindakan.class, BiayaAlatMedisPerKelas.class, TarifKhususPunyaAlatMedis.class, Komunitas.class,
				Pasien.class, Poly.class, Dokter.class, Shift.class, JadwalDokter.class, StatusPulang.class,
				JenisBiayaLain.class, JenisPasien.class, Kecamatan.class, Kelurahan.class, PrioritasPasien.class,
				Satker.class, SatuanItem.class, JenisItemMedis.class, KodeTransaksiMedis.class, ItemMedis.class,
				JenisPenyakit.class, Icd.class, Instalasi.class, GenerikItem.class, TarifKhususPunyaItem.class,
				KelompokItem.class, KelasItem.class, Racikan.class, JenisRacikan.class, PaketPerawatanDetail.class,
				StatusTempatTidur.class, TindakanLabDetail.class, Gudang.class, Pemeriksaan.class,
				KonversiSatuanItem.class, Diskon.class, DiskonDetail.class, BahanBakuItem.class, Produksi.class,
				ProduksiDetail.class, Dusun.class, PajakMedis.class, PajakDetail.class };

		for (Class<?> clazz : classesToInit) {
			InitDataHelper.initData(clazz);
		}
	}

	/**
	 * Menginisialisasi seluruh konstanta acuan modul SIRS (instalasi, poli, satuan/status
	 * default, status tempat tidur, jenis pasien, jenis tindakan baku, kode transaksi medis)
	 * ke field statis {@link ConstantValues} dengan urutan pemanggilan {@link #initInstalasi},
	 * {@link #initPoly}, {@link #initSatuanDanStatus}, {@link #initStatusTempatTidur},
	 * {@link #initJenisPasien}, {@link #initTindakan}, {@link #initKodeTransaksi}.
	 *
	 * <p>
	 * Membuka {@link Session} Hibernate BARU dan terisolasi lewat
	 * {@link HibernateUtil#getSessionFactory()}{@code .openSession()} — sengaja tidak memakai
	 * sesi thread-local/HTTP-request yang biasa dipakai kode aplikasi lain, karena inisialisasi
	 * ini dijalankan pada konteks startup yang mungkin belum/tidak terikat pada satu request
	 * HTTP. Sesi ini SELALU ditutup di blok {@code finally} (disconnect lalu close, plus
	 * {@link HibernateUtil#closeSession()} untuk sesi thread-local bila ada), dan setiap
	 * kegagalan pada seluruh proses inisialisasi ditangkap serta dicatat ke
	 * {@link ErrorAuditUtil} tanpa dilempar ulang — kegagalan inisialisasi SIRS tidak boleh
	 * menggagalkan keseluruhan startup aplikasi.
	 * </p>
	 */
	private static void initDataParam() {
		// 1. Buka Session Baru (Isolated Session)
		// Menggunakan openSession() agar terpisah dari session HTTP request
		Session session = HibernateUtil.getSessionFactory().openSession();

		try {
			initInstalasi(session);
			initPoly(session);
			initSatuanDanStatus(session);
			initStatusTempatTidur(session);
			initJenisPasien(session);
			initTindakan(session);
			initKodeTransaksi(session);
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/InitSirs.java:117");
		} finally {
			// Pastikan session selalu ditutup
			if (session != null && session.isOpen()) {
				// session.disconnect();
				if (session.isOpen()) {session.disconnect();session.close();}
			}
			HibernateUtil.closeSession();
			// Jika HibernateUtil punya mekanisme close khusus, panggil di sini
			// HibernateUtil.closeSession();
		}
	}

	// --- Helper Methods untuk Logika Bisnis ---

	/**
	 * Mengisi konstanta jenis {@link Instalasi} baku ({@code RAWAT_JALAN}, {@code RAWAT_INAP},
	 * {@code UGD}, {@code PENUNJANG_MEDIK}) di {@link ConstantValues} berdasarkan id tetap yang
	 * diasumsikan sudah ada di data referensi (id 2-5). Tidak membuat baris baru bila tidak
	 * ditemukan — murni pembacaan.
	 *
	 * @param session sesi Hibernate aktif yang dipakai untuk query
	 */
	private static void initInstalasi(Session session) {
		ConstantValues.RAWAT_JALAN = getById(session, Instalasi.class, 2L);
		ConstantValues.RAWAT_INAP = getById(session, Instalasi.class, 3L);
		ConstantValues.UGD = getById(session, Instalasi.class, 4L);
		ConstantValues.PENUNJANG_MEDIK = getById(session, Instalasi.class, 5L);
	}

	/**
	 * Mengisi konstanta {@link ConstantValues#POLI_UGD}: mencari {@link Poly} bernama
	 * "Unit Gawat Darurat"; bila belum ada, membuat baris baru (kode {@code "UGD"}) dan
	 * menyimpannya lewat {@link #saveObject(Session, Object)}.
	 *
	 * @param session sesi Hibernate aktif yang dipakai untuk query/simpan
	 */
	private static void initPoly(Session session) {
		ConstantValues.POLI_UGD = (Poly) getByProperty(session, Poly.class, "nama", "Unit Gawat Darurat");
		if (ConstantValues.POLI_UGD == null) {
			ConstantValues.POLI_UGD = new Poly();
			ConstantValues.POLI_UGD.setNama("Unit Gawat Darurat");
			ConstantValues.POLI_UGD.setKode("UGD");
			saveObject(session, ConstantValues.POLI_UGD);
		}
	}

	/**
	 * Mengisi {@link ConstantValues#DEFAULT_SATUAN} (satuan item baku, id {@code -3}) dan
	 * memastikan status pulang baku "Pindah"/"Meninggal" ({@link ConstantValues#STATUS_PINDAH}/
	 * {@link ConstantValues#STATUS_MENINGGAL}) tersedia lewat
	 * {@link #checkAndCreateStatusPulang(Session, String)}.
	 *
	 * @param session sesi Hibernate aktif yang dipakai untuk query/simpan
	 */
	private static void initSatuanDanStatus(Session session) {
		ConstantValues.DEFAULT_SATUAN = getById(session, SatuanItem.class, -3L);

		ConstantValues.STATUS_PINDAH = checkAndCreateStatusPulang(session, Pendaftaran.PINDAH);
		ConstantValues.STATUS_MENINGGAL = checkAndCreateStatusPulang(session, Pendaftaran.MENINGGAL);
	}

	/**
	 * Mencari {@link StatusPulang} berdasarkan nama; bila belum ada, membuat dan menyimpan baris
	 * baru dengan nama tersebut. Pola get-or-create yang dipakai berulang di kelas ini untuk
	 * data referensi yang harus selalu tersedia.
	 *
	 * @param session sesi Hibernate aktif
	 * @param nama    nama status pulang yang dicari/dibuat
	 * @return status pulang yang sudah ada atau baru saja dibuat
	 */
	private static StatusPulang checkAndCreateStatusPulang(Session session, String nama) {
		StatusPulang status = (StatusPulang) getByProperty(session, StatusPulang.class, "nama", nama);
		if (status == null) {
			status = new StatusPulang();
			status.setNama(nama);
			saveObject(session, status);
		}
		return status;
	}

	/**
	 * Memastikan seluruh status baku {@link StatusTempatTidur} tersedia: "Tersedia",
	 * "Tidak Tersedia", "Rusak", "Telah Dipindahkan", "Sedang dalam perbaikan", diisi ke
	 * field terkait di {@link ConstantValues}.
	 *
	 * <p>
	 * Kasus khusus untuk status "Tersedia": bila status ini BELUM ada di database (dianggap
	 * sebagai indikasi ini adalah inisialisasi pertama kali modul SIRS pada instalasi
	 * bersangkutan), method ini SEKALIGUS mengupdate SEMUA baris {@link TempatTidur} yang sudah
	 * ada agar berstatus "Tersedia", dibungkus dalam transaksi eksplisit dengan rollback manual
	 * bila gagal. Efek samping ini TIDAK terjadi lagi pada pemanggilan berikutnya begitu status
	 * "Tersedia" sudah tercatat di database.
	 * </p>
	 *
	 * @param session sesi Hibernate aktif yang dipakai untuk query/simpan/update
	 */
	private static void initStatusTempatTidur(Session session) {
		// Tersedia
		ConstantValues.TERSEDIA = (StatusTempatTidur) getByProperty(session, StatusTempatTidur.class, "nama",
				"Tersedia");
		if (ConstantValues.TERSEDIA == null) {
			ConstantValues.TERSEDIA = new StatusTempatTidur();
			ConstantValues.TERSEDIA.setNama("Tersedia");
			ConstantValues.TERSEDIA.setKeterangan("Tempat tidur ini tersedia untuk pasien");
			saveObject(session, ConstantValues.TERSEDIA);

			// Logika khusus: Update semua tempat tidur ke status Tersedia saat pertama kali
			// init
			Transaction tx = session.beginTransaction();
			try {
				List<TempatTidur> tempatTidurs = ConstantValues.simpleList(session.createCriteria(TempatTidur.class),
						TempatTidur.class);
				for (TempatTidur tt : tempatTidurs) {
					tt.setStatusTempatTidur(ConstantValues.TERSEDIA);
					session.update(tt);
				}
				tx.commit();
			} catch (Exception e) {
				if (tx != null)
					tx.rollback();
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/InitSirs.java:190");
			}
		}

		// Status Lainnya
		ConstantValues.TIDAK_TERSEDIA = checkAndCreateStatusTT(session, "Tidak Tersedia",
				"Tempat tidur ini tidak tersedia untuk pasien");
		ConstantValues.RUSAK = checkAndCreateStatusTT(session, "Rusak",
				"Tempat tidur ini rusak, sehingga tidak tersedia untuk pasien");
		ConstantValues.TELAH_DIPINDAHKAN = checkAndCreateStatusTT(session, "Telah Dipindahkan",
				"Tempat tidur ini telah dipindahkan, sehingga tidak tersedia untuk pasien");
		ConstantValues.SEDANG_DALAM_PERBAIKAN = checkAndCreateStatusTT(session, "Sedang dalam perbaikan",
				"Tempat tidur ini sedang dalam perbaikan, sehingga tidak tersedia untuk pasien");
	}

	/**
	 * Mencari {@link StatusTempatTidur} berdasarkan nama; bila belum ada, membuat dan menyimpan
	 * baris baru dengan nama dan keterangan yang diberikan. Sama seperti
	 * {@link #checkAndCreateStatusPulang(Session, String)} namun untuk entitas
	 * {@link StatusTempatTidur} yang punya field keterangan tambahan.
	 *
	 * @param session    sesi Hibernate aktif
	 * @param nama       nama status tempat tidur yang dicari/dibuat
	 * @param keterangan deskripsi status, hanya dipakai bila baris baru harus dibuat
	 * @return status tempat tidur yang sudah ada atau baru saja dibuat
	 */
	private static StatusTempatTidur checkAndCreateStatusTT(Session session, String nama, String keterangan) {
		StatusTempatTidur status = (StatusTempatTidur) getByProperty(session, StatusTempatTidur.class, "nama", nama);
		if (status == null) {
			status = new StatusTempatTidur();
			status.setNama(nama);
			status.setKeterangan(keterangan);
			saveObject(session, status);
		}
		return status;
	}

	/**
	 * Mengisi konstanta jenis pasien baku ({@code PASIEN_UMUM}, {@code PASIEN_ASURANSI},
	 * {@code PASIEN_DINAS}, {@code PASIEN_SISWA}) di {@link ConstantValues} berdasarkan id tetap
	 * yang diasumsikan sudah ada di data referensi. Murni pembacaan, tidak membuat data baru.
	 *
	 * @param session sesi Hibernate aktif yang dipakai untuk query
	 */
	private static void initJenisPasien(Session session) {
		ConstantValues.PASIEN_UMUM = getById(session, JenisPasien.class, 1L);
		ConstantValues.PASIEN_ASURANSI = getById(session, JenisPasien.class, 8L);
		ConstantValues.PASIEN_DINAS = getById(session, JenisPasien.class, 6L);
		ConstantValues.PASIEN_SISWA = getById(session, JenisPasien.class, 7L);
	}

	/**
	 * Memastikan jenis tindakan "Kunjungan Dokter" dan tindakan "Kunjungan Rutin" baku tersedia
	 * (get-or-create untuk keduanya), lalu mengisi konstanta tindakan tetap lain (pembuatan
	 * kartu, pendaftaran pasien, kelas perawatan normal) di {@link ConstantValues} berdasarkan
	 * id tetap.
	 *
	 * @param session sesi Hibernate aktif yang dipakai untuk query/simpan
	 */
	private static void initTindakan(Session session) {
		ConstantValues.KUNJUNGAN_DOKTER = (JenisTindakan) getByProperty(session, JenisTindakan.class, "nama",
				JenisTindakan.KUNJUNGAN_DOKTER);
		if (ConstantValues.KUNJUNGAN_DOKTER == null) {
			ConstantValues.KUNJUNGAN_DOKTER = new JenisTindakan();
			ConstantValues.KUNJUNGAN_DOKTER.setNama(JenisTindakan.KUNJUNGAN_DOKTER);
			saveObject(session, ConstantValues.KUNJUNGAN_DOKTER);
		}

		ConstantValues.KUNJUNGAN_RUTIN = (Tindakan) session.createCriteria(Tindakan.class)
				.add(Restrictions.eq("nama", Tindakan.KUNJUNGAN_RUTIN))
				.add(Restrictions.eq("jenisTindakan", ConstantValues.KUNJUNGAN_DOKTER)).setMaxResults(1).uniqueResult();

		if (ConstantValues.KUNJUNGAN_RUTIN == null) {
			ConstantValues.KUNJUNGAN_RUTIN = new Tindakan();
			ConstantValues.KUNJUNGAN_RUTIN.setJenisTindakan(ConstantValues.KUNJUNGAN_DOKTER);
			ConstantValues.KUNJUNGAN_RUTIN.setNama(Tindakan.KUNJUNGAN_RUTIN);
			saveObject(session, ConstantValues.KUNJUNGAN_RUTIN);
		}

		ConstantValues.PEMBUATAN_KARTU = getById(session, Tindakan.class, 104755L);
		ConstantValues.PENDAFTARAN_PASIEN = getById(session, Tindakan.class, 104778L);
		ConstantValues.kelasNormal = getById(session, KelasPerawatan.class, 0L);
	}

	/**
	 * Mengisi seluruh konstanta {@link KodeTransaksiMedis} baku (lebih dari 20 kode: produksi,
	 * bahan baku, pemakaian/retur barang, adjustment, transfer item, apotik, lab, operasi,
	 * radiologi, VK, renal unit, gizi, UGD, jasa racik, kunjungan dokter, transaksi bed, dsb.)
	 * di {@link ConstantValues} lewat pencarian berdasarkan kolom {@code kode} (via
	 * {@link #getKode(Session, String)}). Murni pembacaan — kode transaksi diasumsikan sudah
	 * ada sebagai data referensi baku, tidak dibuat otomatis oleh method ini.
	 *
	 * @param session sesi Hibernate aktif yang dipakai untuk query
	 */
	private static void initKodeTransaksi(Session session) {
		ConstantValues.produksi = getKode(session, "PROD");
		ConstantValues.bahanBaku = getKode(session, "BB");
		ConstantValues.pemakaianBarang = getKode(session, "PB");
		ConstantValues.returPemakaianBarang = getKode(session, "PR");
		ConstantValues.adjustmentPenambahan = getKode(session, "ADT");
		ConstantValues.adjustmentPengurangan = getKode(session, "ADK");
		ConstantValues.transferItemDari = getKode(session, "TRD");
		ConstantValues.transferItemSelisih = getKode(session, "TRS");
		ConstantValues.apotikJual = getKode(session, "AJ");
		ConstantValues.lab = getKode(session, "TLAB");
		ConstantValues.operasi = getKode(session, "TOPR");
		ConstantValues.radiologi = getKode(session, "TRAD");
		ConstantValues.vk = getKode(session, "TVK");
		ConstantValues.renalUnit = getKode(session, "TRU");
		ConstantValues.gizi = getKode(session, "TGIZ");
		ConstantValues.ugd = getKode(session, "TUGD");
		ConstantValues.jasaRacik = getKode(session, "RAC");
		ConstantValues.kunjunganDokter = getKode(session, "KUNJ");
		ConstantValues.transaksiBed = getKode(session, "BED");
		ConstantValues.lain = getKode(session, "TLAIN");
		ConstantValues.apotikRetur = getKode(session, "AR");
		ConstantValues.transferItemKe = getKode(session, "TRK");
		ConstantValues.saldoAwal = getKode(session, "AW");
		ConstantValues.beliMasuk = getKode(session, "BM");
		ConstantValues.beliRetur = getKode(session, "BR");
	}

	// --- Generic Hibernate Helpers (Java 1.7 Compatible) ---

	/**
	 * Helper generik: mengambil satu entitas berdasarkan id primary key-nya lewat Hibernate
	 * Criteria API.
	 *
	 * @param <T>     tipe entitas
	 * @param session sesi Hibernate aktif
	 * @param clazz   kelas entitas yang dicari
	 * @param id      id primary key yang dicari
	 * @return entitas yang cocok, atau {@code null} bila tidak ditemukan
	 */
	@SuppressWarnings("unchecked")
	private static <T> T getById(Session session, Class<T> clazz, Long id) {
		return (T) session.createCriteria(clazz).add(Restrictions.idEq(id)).uniqueResult();
	}

	/**
	 * Helper generik: mengambil satu entitas berdasarkan kecocokan nilai satu properti (kolom)
	 * tertentu, dibatasi maksimal satu hasil.
	 *
	 * @param <T>          tipe entitas
	 * @param session      sesi Hibernate aktif
	 * @param clazz        kelas entitas yang dicari
	 * @param propertyName nama properti/kolom yang dibandingkan
	 * @param value        nilai yang dicari kecocokannya (perbandingan {@code =})
	 * @return entitas pertama yang cocok, atau {@code null} bila tidak ditemukan
	 */
	@SuppressWarnings("unchecked")
	private static <T> T getByProperty(Session session, Class<T> clazz, String propertyName, Object value) {
		return (T) session.createCriteria(clazz).add(Restrictions.eq(propertyName, value)).setMaxResults(1)
				.uniqueResult();
	}

	/**
	 * Mengambil {@link KodeTransaksiMedis} berdasarkan nilai kolom {@code kode}-nya. Sekadar
	 * pembungkus tipis di atas {@link #getByProperty(Session, Class, String, Object)} untuk
	 * memperjelas maksud pemanggilan di {@link #initKodeTransaksi(Session)}.
	 *
	 * @param session sesi Hibernate aktif
	 * @param kode    nilai kode transaksi medis yang dicari
	 * @return kode transaksi medis yang cocok, atau {@code null} bila tidak ditemukan
	 */
	private static KodeTransaksiMedis getKode(Session session, String kode) {
		return (KodeTransaksiMedis) getByProperty(session, KodeTransaksiMedis.class, "kode", kode);
	}

	/**
	 * Menyimpan satu objek entitas baru dalam transaksi Hibernate sendiri, dengan rollback
	 * eksplisit bila terjadi kegagalan. Kegagalan penyimpanan ditangkap dan dicatat ke
	 * {@link ErrorAuditUtil}, tidak dilempar ulang ke pemanggil — konsisten dengan sifat
	 * "best effort, jangan gagalkan startup" dari seluruh proses inisialisasi di kelas ini.
	 *
	 * @param session sesi Hibernate aktif tempat transaksi dibuka
	 * @param object  entitas baru yang akan disimpan
	 */
	private static void saveObject(Session session, Object object) {
		Transaction tx = null;
		try {
			tx = session.beginTransaction();
			session.save(object);
			tx.commit();
		} catch (Exception e) {
			if (tx != null)
				tx.rollback();
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/InitSirs.java:302");
		}
	}
}