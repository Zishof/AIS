package ais.common;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
// Asumsi pakai ZK Framework
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Filedownload; // Asumsi pakai ZK Framework
import org.zkoss.zul.Messagebox;

import ais.action.master.helper.PembayaranUtilHelper;
import ais.action.master.helper.KegiatanPersistenceHelper;
import ais.action.ws.util.ConstantUtil;
import ais.action.ws.util.PembayaranUtil;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.DetailBiaya;
import ais.database.model.Dosen;
import ais.database.model.ErrorLog;
import ais.database.model.FormatNilaiProposalSkripsi;
import ais.database.model.FormatNilaiSkripsi;
import ais.database.model.FormulirKegiatan;
import ais.database.model.JenisKegiatan;
import ais.database.model.Kegiatan;
import ais.database.model.Konfigurasi;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.Matakuliah;
import ais.database.model.MatakuliahEkivalen;
import ais.database.model.PendaftaranCutiMahasiswa;
import ais.database.model.PengaturanPembayaranBulanan;
import ais.database.model.PengecualianJadwalPengisianKRSMahasiswa;
import ais.database.model.PengecualianJadwalPenilaianDosen;
import ais.database.model.Perkuliahan;
import ais.database.model.Tbmuser;
import ais.database.model.Tbmrole;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.WaktuUtil;
// Import Hibernate & FileUtils (Apache Commons) diasumsikan sudah ada

/**
 * Kelas utilitas statis <b>grab-bag</b> (kumpulan fungsi bantu lintas-topik yang tidak cukup besar
 * untuk masing-masing punya kelas sendiri) untuk modul akademik AIS. Isinya sengaja beragam —
 * bukan cacat desain, melainkan pola umum pada codebase legacy berukuran besar: fungsi-fungsi
 * kecil yang dipakai berulang dari banyak layar/aksi dikumpulkan di satu tempat agar mudah
 * ditemukan, alih-alih tersebar sebagai duplikat di tiap pemanggil. Berdasarkan isinya, kelas ini
 * dapat dibagi menjadi beberapa kelompok fungsi:
 *
 * <h2>1. Pelaporan galat (error handling)</h2>
 * <p>
 * {@link #tampilErrorJikaAdmin(Exception)}/{@link #tampilErrorJikaAdmin(Exception, String,
 * boolean)}, {@link #downloadError(Exception)}, {@link #getStackTraceAsString(Throwable)}, serta
 * dua helper privat {@link #saveAndDownloadErrorFile(String)} dan
 * {@link #saveErrorToDatabase(String)} — menyediakan jalur seragam untuk menampilkan dialog galat
 * ke pengguna (dengan opsi copy detail teknis untuk dikirim ke admin), menyimpan stack trace ke
 * tabel {@link ErrorLog}, dan mengunduhnya sebagai berkas teks bila diperlukan.
 * </p>
 *
 * <h2>2. Query string</h2>
 * <p>
 * {@link #convertToQueryString(Map)} beserta helper privatnya
 * ({@link #appendQueryParam(StringBuilder, String, String)}, {@link #encodeUTF8(String)}) —
 * mengubah {@link Map} parameter (termasuk nilai berbentuk larik {@code String[]}) menjadi string
 * query URL yang sudah di-encode UTF-8.
 * </p>
 *
 * <h2>3. Cache jenis kegiatan (in-memory, lintas-request)</h2>
 * <p>
 * Enam field statis publik bertipe {@code TreeSet<JenisKegiatan>}
 * ({@link #jenisKegiatansTanpaDaftarUlang}, {@link #jenisKegiatansAktif},
 * {@link #jenisKegiatansUntukKrs}, {@link #jenisKegiatansUntukNilai},
 * {@link #jenisKegiatansUntukSyaratAktif}, {@link #jenisKegiatansUntukSyaratUjian}) menyimpan hasil
 * query {@link JenisKegiatan} yang sudah difilter/diurutkan untuk berbagai konteks (KRS, penilaian,
 * syarat keaktifan, syarat ujian, dsb.), diisi ulang lewat {@link #reloadJenisKegiatans()} /
 * {@link #reloadJenisKegiatans(Session)}. Karena bersifat statis, cache ini dibagi oleh SELURUH
 * pengguna aplikasi pada satu JVM — dirancang untuk data referensi yang jarang berubah, bukan data
 * spesifik per-pengguna.
 * </p>
 *
 * <h2>4. Pembangun komponen UI ZK (Combobox)</h2>
 * <p>
 * {@link #initJenisPembayaranMahasiswa(Combobox)},
 * {@link #initJenisPembayaranBiodataCalonMahasiswa(Combobox)},
 * {@link #initJenisPembayaranMahasiswaDanBiodataCalonMahasiswa(Combobox)},
 * {@link #initJenisSemester(Combobox)}/{@link #initJenisSemester(Combobox, boolean)},
 * {@link #createComboKonfigurasi(Combobox)}, {@link #createComboJenisPembayaranDanSemua(Combobox)},
 * {@link #createComboJenisPembayaran(Combobox)} — masing-masing menerima {@link Combobox} ZK yang
 * sudah ada (atau membuat baru bila {@code null}) dan mengisinya dengan pilihan yang relevan
 * (jenis kegiatan/pembayaran, jenis semester, daftar konfigurasi), sekaligus menentukan item yang
 * terpilih secara default.
 * </p>
 *
 * <h2>5. Perhitungan status semester/tahapan mahasiswa</h2>
 * <p>
 * {@link #getSemesterString()}, {@link #generateStatusSemester(Mahasiswa)},
 * {@link #generateSemestersForGrid(Mahasiswa, int, int, Integer)},
 * {@link #generateSemestersForGridTahapan(Mahasiswa, int)} — menghasilkan riwayat
 * semester/tahapan studi seorang mahasiswa (tahun akademik, status, nomor tahap) untuk ditampilkan
 * pada grid UI, dengan logika percabangan berbeda bergantung pada jumlah "tahapan" pembayaran yang
 * dikonfigurasi untuk program studi mahasiswa tersebut (2, 3, atau 4 tahap per tahun akademik).
 * </p>
 *
 * <h2>6. Gerbang syarat pembayaran (payment gate) — kelompok terbesar</h2>
 * <p>
 * Keluarga method {@code checkStatusPembayaran*}/{@code checkPembayaran*} adalah inti bisnis
 * kelas ini: masing-masing memutuskan apakah seorang {@link Mahasiswa} SUDAH memenuhi syarat
 * pembayaran minimum untuk melanjutkan suatu aksi akademik — mengambil KRS
 * ({@link #checkPembayaranSebelumKRSSudahMemenuhi}), memenuhi syarat KRS berdasarkan pelunasan
 * semester sebelumnya ({@link #checkStatusPembayaranMahasiswaSebelumnya(Integer, Integer,
 * Mahasiswa, boolean)}), mengikuti suatu {@link FormulirKegiatan}
 * ({@link #checkStatusPembayaranKegiatanMahasiswa}), melihat nilai
 * ({@link #checkStatusPembayaranMahasiswaSebelumnyaUntukPenilaian}), mengajukan proposal skripsi
 * ({@link #checkStatusPembayaranMahasiswaPengajuanSkripsi}), mengajukan sidang
 * ({@link #checkStatusPembayaranMahasiswaPengajuanSidang}), atau mendaftar wisuda
 * ({@link #checkStatusPembayaranMahasiswaPengajuanWisuda}). Setiap method membaca ambang batas
 * persentase pelunasan dari {@link ais.database.model.Konfigurasi} (berbeda-beda per konteks,
 * dapat dipersempit per jurusan/program/angkatan), menghormati pengecualian (dispensasi cuti,
 * pengecualian jadwal KRS via {@link #checkApakahMahasiswaBolehAmbilKrsLewatPengecualian}, dan
 * jalur "baypass" lewat {@code Common.checkBaypassStatusPembayaranMahasiswa}), lalu menghitung
 * persentase pemenuhan tagihan aktual lewat
 * {@link KegiatanPersistenceHelper#hitungPersentasePemenuhanTagihan}. Method pendukung
 * {@link #hitungTagihanMahasiswaSebagaiSyaratKrs(Session, Mahasiswa, Integer)} menjumlahkan total
 * nominal tagihan (termasuk skema cicilan bulanan) untuk satu semester sebagai syarat KRS.
 * </p>
 *
 * <h2>7. Lain-lain</h2>
 * <p>
 * {@link #getMatakuliahApakahEkivalen(Matakuliah, String, boolean)} — resolusi mata kuliah
 * ekivalen untuk seorang mahasiswa. {@link #checkApakahDosenBolehMenilai} — memeriksa apakah dosen
 * (atau petugas pengelola yang bertindak atas nama dosen) berhak memberi nilai pada suatu periode
 * berdasarkan pengecualian jadwal penilaian yang berlaku. {@link #checkUsername(String, String,
 * Long)} — memvalidasi keunikan username/NIM/username-ortu calon akun baru lintas empat entitas
 * berbeda.
 * </p>
 *
 * <p>
 * <b>Catatan umum:</b> banyak method di kelas ini membuka dan menutup {@link Session} Hibernate
 * secara manual (bukan lewat pengelolaan transaksi terpusat), serta memanggil
 * {@link MyMessageboxConfig} untuk menampilkan pesan penolakan langsung ke pengguna dari dalam
 * logika bisnis — pola ini dipertahankan apa adanya sesuai cakupan pekerjaan dokumentasi (hanya
 * menambah Javadoc, tidak mengubah logika).
 * </p>
 */
public class CommonHelperClass {

	/**
	 * Mengubah {@link Map} pasangan kunci-nilai menjadi string query URL (format
	 * {@code key1=val1&key2=val2}), dengan setiap nilai di-encode UTF-8 lewat
	 * {@link java.net.URLEncoder}. Entri dengan kunci atau nilai {@code null} dilewati. Nilai
	 * bertipe {@code String[]} diperlakukan sebagai multi-value: setiap elemen larik menghasilkan
	 * satu pasangan {@code key=value} terpisah dengan kunci yang sama.
	 *
	 * @param map peta parameter; boleh {@code null} atau kosong
	 * @return string query URL (tanpa {@code "?"} di depan), atau string kosong bila {@code map}
	 *         {@code null}/kosong
	 */
	public static String convertToQueryString(Map<String, Object> map) {
		if (map == null || map.isEmpty()) {
			return "";
		}

		StringBuilder sb = new StringBuilder();

		for (Map.Entry<String, Object> entry : map.entrySet()) {
			String key = entry.getKey();
			Object value = entry.getValue();

			// Cek null safety agar tidak NullPointerException
			if (key == null || value == null)
				continue;

			// Handle jika value adalah Array String (sesuai kode asli)
			if (value instanceof String[]) {
				String[] values = (String[]) value;
				for (String val : values) {
					appendQueryParam(sb, key, val);
				}
			}
			// IMPROVEMENT: Handle jika value cuma String biasa/Object lain (bukan array)
			// agar code lebih fleksibel
			else {
				appendQueryParam(sb, key, value.toString());
			}
		}

		return sb.toString();
	}

	/** Menambahkan satu pasangan {@code key=value} (nilai sudah di-encode) ke {@code sb}, dengan {@code "&"} pemisah bila {@code sb} sudah berisi data. */
	private static void appendQueryParam(StringBuilder sb, String key, String val) {
		if (sb.length() > 0) {
			sb.append("&");
		}
		sb.append(key).append("=").append(encodeUTF8(val));
	}

	/** Membungkus {@link java.net.URLEncoder#encode(String, String)} dengan UTF-8; mengembalikan {@code s} apa adanya (fallback) bila encoding gagal, dan string kosong bila {@code s} {@code null}. */
	private static String encodeUTF8(String s) {
		try {
			return s != null ? URLEncoder.encode(s, "UTF-8") : "";
		} catch (UnsupportedEncodingException e) {
			return s; // Fallback jika gagal encode
		}
	}

	/** Seperti {@link #tampilErrorJikaAdmin(Exception, String, boolean)} tanpa info tambahan dan tanpa opsi unduh berkas ({@code info=""}, {@code download=false}). */
	public static String tampilErrorJikaAdmin(Exception ex) {
		return tampilErrorJikaAdmin(ex, "", false);
	}

	/**
	 * Implementasi kanonik penanganan galat terpusat: mengambil stack trace {@code ex} (lewat
	 * {@link #getStackTraceAsString(Throwable)}), menggabungkannya dengan {@code info} tambahan
	 * bila ada, secara opsional menawarkan unduhan berkas teks berisi stack trace tersebut
	 * ({@link #saveAndDownloadErrorFile(String)} bila {@code download=true}), SELALU menyimpan
	 * catatan galat ke tabel {@link ErrorLog} ({@link #saveErrorToDatabase(String)}), lalu
	 * menampilkan dialog {@link MyMessageboxConfig#showDetail} ke pengguna dengan tombol "Detail"
	 * untuk menyalin informasi teknis (disertai langkah perbaikan yang disarankan bila {@code info}
	 * kosong).
	 *
	 * @param ex       exception yang akan dilaporkan
	 * @param info     konteks tambahan yang disisipkan di awal stack trace (mis. nama aksi/layar
	 *                 yang sedang berjalan), boleh kosong
	 * @param download {@code true} untuk juga menawarkan unduhan berkas teks berisi stack trace
	 * @return string stack trace lengkap (termasuk {@code info}) yang sudah dicatat/ditampilkan
	 */
	public static String tampilErrorJikaAdmin(Exception ex, String info, boolean download) {
		// 2. Mengambil stack trace menggunakan helper method (DRY)
		String stackTrace = getStackTraceAsString(ex);

		// Gabungkan info tambahan jika ada
		if (info != null && !info.isEmpty()) {
			stackTrace = info + "\n" + stackTrace;
		}

		// Fitur Download File
		if (download) {
			saveAndDownloadErrorFile(stackTrace);
		}

		// Simpan ke Database (Hibernate)
		saveErrorToDatabase(stackTrace);

		try {
			MyMessageboxConfig.showDetail(
					"Terjadi kesalahan pada sistem. Silakan klik Detail lalu copy informasi teknis untuk dikirim ke admin.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.ERROR, ex,
					(info == null || info.trim().isEmpty()
							? "Langkah perbaikan yang disarankan:\n"
									+ "1. Ulangi proses setelah memastikan data/form sudah lengkap.\n"
									+ "2. Jika error tetap muncul, klik Copy Detail lalu kirim ke admin/teknis.\n"
									+ "3. Admin dapat mencari log error berdasarkan waktu kejadian pada detail ini."
							: info));
		} catch (Exception alertEx) {
			ais.common.ErrorAuditUtil.record(alertEx,
					"auto-audit(empty-catch) CommonHelperClass.tampilErrorJikaAdmin alert-detail");
		}

		return stackTrace;
	}

	/**
	 * Membangun {@link EventListener} ZK yang, saat dipicu, mengambil stack trace {@code ex} dan
	 * langsung menawarkan unduhannya sebagai berkas teks lewat
	 * {@link #saveAndDownloadErrorFile(String)}. Dipakai untuk memasang tombol/tautan "Unduh
	 * Detail Galat" terpisah dari dialog {@link #tampilErrorJikaAdmin} utama.
	 *
	 * @param ex exception yang stack trace-nya akan diunduh saat listener dipicu
	 * @return {@link EventListener} siap dipasang pada komponen ZK
	 */
	public static EventListener downloadError(final Exception ex) {
		// Java 1.7 belum support Lambda, jadi pakai Anonymous Class
		return new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				String stackTrace = getStackTraceAsString(ex);
				saveAndDownloadErrorFile(stackTrace);
			}
		};
	}

	// --- PRIVATE HELPER METHODS (Agar kode utama lebih bersih) ---

	/**
	 * Mengonversi stack trace {@link Throwable} menjadi string, memakai
	 * {@link Throwable#printStackTrace(PrintWriter)} ke {@link StringWriter}.
	 *
	 * @param t throwable yang akan dikonversi; boleh {@code null}
	 * @return representasi teks stack trace, string kosong bila {@code t} {@code null}, atau pesan
	 *         galat singkat bila proses konversi sendiri gagal
	 */
	public static String getStackTraceAsString(Throwable t) {
		if (t == null)
			return "";
		// Try-with-resources (Java 1.7+) menutup writer otomatis
		try {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			t.printStackTrace(pw);
			return sw.toString();
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CommonHelperClass.java:159");
			return "Error getting stack trace: " + e.getMessage();
		}
	}

	/**
	 * Menulis {@code content} ke berkas teks sementara bernama acak di direktori
	 * {@code <REAL_PATH>/tmp/} lalu memicu unduhan berkas tersebut ke browser pengguna lewat
	 * {@link Filedownload#save(File, String)}. Kegagalan (mis. galat tulis berkas) dicatat dan
	 * diabaikan agar tidak mengganggu alur penanganan galat utama yang memanggilnya.
	 *
	 * @param content isi berkas yang akan diunduh (biasanya stack trace)
	 */
	private static void saveAndDownloadErrorFile(String content) {
		try {
			// Gunakan File.separator agar aman di Windows/Linux
			String fileName = "error_" + Common.randLong() + ".txt";
			File fileErr = new File(Common.REAL_PATH + File.separator + "tmp" + File.separator + fileName);

			if (!fileErr.getParentFile().exists()) {
				fileErr.getParentFile().mkdirs();
			}

			FileUtils.writeStringToFile(fileErr, content); // Apache Commons IO
			Filedownload.save(fileErr, "text/plain"); // ZK Framework

		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonHelperClass.java:177");
			// e.printStackTrace(); // Log error jika gagal download, jangan sampai
			// menghentikan flow utama
		}
	}

	/**
	 * Menyimpan {@code errContent} sebagai satu baris {@link ErrorLog} baru, memakai sesi
	 * Hibernate terisolasi ({@code openSession()}, terpisah dari sesi request HTTP) agar
	 * pencatatan galat tidak ikut gagal bila transaksi/sesi request sedang dalam keadaan tidak
	 * valid. Sesi selalu ditutup di blok {@code finally}; kegagalan penyimpanan sendiri dicatat
	 * lewat {@link ais.common.ErrorAuditUtil} dan tidak dilempar ulang.
	 *
	 * @param errContent isi galat (biasanya stack trace, opsional digabung info tambahan) yang
	 *                    disimpan ke kolom {@code keterangan}
	 */
	private static void saveErrorToDatabase(String errContent) {
		// 1. Buka Session Baru (Isolated Session)
		// Menggunakan openSession() agar terpisah dari session HTTP request
		Session session = HibernateUtil.getSessionFactory().openSession();

		try {
			session.getTransaction().begin();

			ErrorLog errorLog = new ErrorLog();
			// Batasi panjang karakter jika kolom database terbatas, opsional
			// if (errContent.length() > 5000) errContent = errContent.substring(0, 5000);
			errorLog.setKeterangan(errContent);

			session.save(errorLog);
			session.getTransaction().commit();

		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonHelperClass.java:199");
			// e.printStackTrace();
		} finally {
			// 2. WAJIB Tutup Session
			if (session != null && session.isOpen()) {
				try {
					session.disconnect();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonHelperClass.java:206");
					// TODO: handle exception
				}
				try {
					session.close();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonHelperClass.java:211");
					// TODO: handle exception
				}

			}
		}
	}

	/**
	 * Menentukan apakah suatu {@link Matakuliah} memiliki padanan ekivalen untuk mahasiswa
	 * ({@code nim}) tertentu (mis. akibat perubahan kurikulum), dengan opsi memaksa muat ulang
	 * relasi ekivalensi lebih dulu.
	 *
	 * @param matakuliah mata kuliah yang diperiksa; boleh {@code null}
	 * @param nim        NIM mahasiswa, dipakai untuk resolusi ekivalensi spesifik-mahasiswa
	 * @param refresh    {@code true} untuk memanggil {@link Matakuliah#reInitEkivalen()} lebih
	 *                   dulu sebelum membaca relasi ekivalensi
	 * @return larik dua elemen {@code [matakuliahEkivalenAtauAsli, matakuliahAsli]} — elemen
	 *         pertama adalah mata kuliah ekivalen bila ditemukan, atau {@code matakuliah} itu
	 *         sendiri bila tidak ada ekivalensi; {@code {null, null}} bila {@code matakuliah}
	 *         {@code null}
	 */
	public static Matakuliah[] getMatakuliahApakahEkivalen(Matakuliah matakuliah, String nim, boolean refresh) {
		if (matakuliah != null) {
			if (refresh) {
				matakuliah.reInitEkivalen();
			}

			List<MatakuliahEkivalen> matakuliahEkivalens = matakuliah.ambilEkivalen(nim);

			return new Matakuliah[] { (matakuliahEkivalens != null && !matakuliahEkivalens.isEmpty()
					? matakuliahEkivalens.get(0).getMatakuliahEkivalen()
					: matakuliah), matakuliah };
		} else {
			return new Matakuliah[] { null, null };
		}
	}

	/** Cache statis {@link JenisKegiatan} aktif yang bukan pendaftaran/daftar ulang mahasiswa baru; diisi lewat {@link #reloadJenisKegiatans()}. */
	public static TreeSet<JenisKegiatan> jenisKegiatansTanpaDaftarUlang = null;
	/** Cache statis seluruh {@link JenisKegiatan} aktif dan bukan kegiatan default; diisi lewat {@link #reloadJenisKegiatans()}. */
	public static TreeSet<JenisKegiatan> jenisKegiatansAktif = null;
	/** Cache statis {@link JenisKegiatan} yang dipakai sebagai syarat pengambilan KRS; diisi lewat {@link #reloadJenisKegiatans()}. */
	public static TreeSet<JenisKegiatan> jenisKegiatansUntukKrs = null;
	/** Cache statis {@link JenisKegiatan} yang dipakai sebagai syarat melihat nilai; diisi lewat {@link #reloadJenisKegiatans()}. */
	public static TreeSet<JenisKegiatan> jenisKegiatansUntukNilai = null;
	/** Cache statis {@link JenisKegiatan} yang dipakai sebagai syarat status keaktifan mahasiswa; diisi lewat {@link #reloadJenisKegiatans()}. */
	public static TreeSet<JenisKegiatan> jenisKegiatansUntukSyaratAktif = null;
	/** Cache statis {@link JenisKegiatan} yang dipakai sebagai syarat mengikuti ujian; diisi lewat {@link #reloadJenisKegiatans()}. */
	public static TreeSet<JenisKegiatan> jenisKegiatansUntukSyaratUjian = null;

	/**
	 * Memuat ulang seluruh cache {@code jenisKegiatans*} memakai sesi Hibernate native baru
	 * (dibuka dan ditutup sendiri oleh method ini). Delegasi ke
	 * {@link #reloadJenisKegiatans(Session)}.
	 */
	public static void reloadJenisKegiatans() {
		Session session = null;
		try {
			session = HibernateUtil.currentNativeSession();
			reloadJenisKegiatans(session);
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CommonHelperClass.java:248");
		} finally {
			HibernateUtil.closeSession();
		}
	}

	/**
	 * Implementasi kanonik pemuatan ulang keenam cache statis {@code jenisKegiatans*} (lihat
	 * Javadoc kelas, kelompok "Cache jenis kegiatan") dari database memakai {@code session} yang
	 * diberikan pemanggil (tidak dibuka/ditutup oleh method ini). Setiap cache diisi lewat query
	 * Hibernate Criteria dengan filter berbeda (nama tidak kosong, status aktif, penanda
	 * penggunaan seperti {@code digunakanUntukPengecekanKrs}/{@code digunakanUntukPengecekanNilai}),
	 * dan beberapa cache memiliki fallback ke {@link ConstantValues#PENDAFTARAN_MAHASISWA_LAMA}
	 * bila hasil query kosong agar selalu ada minimal satu jenis kegiatan rujukan. Juga memanggil
	 * {@link ConstantUtil#initIstilahPendaftaran(Session)} dan me-refresh beberapa entitas
	 * {@link ConstantValues} terkait pendaftaran. Tidak melakukan apa pun (return langsung) bila
	 * {@code session} {@code null} atau sudah tertutup. Kegagalan dicatat ke
	 * {@link ais.common.ErrorAuditUtil} dan tidak dilempar ulang — cache yang gagal dimuat akan
	 * tetap bernilai {@code null}/nilai lama.
	 *
	 * <p><b>Kontrak khusus cache syarat keaktifan.</b> Kolom
	 * {@code JenisKegiatan.digunakanSyaratKeaktifan} mempunyai tiga keadaan di database, tetapi
	 * bukan boolean tiga keadaan pada aturan bisnis. Nilai {@code true} adalah opt-in eksplisit,
	 * {@code false} adalah opt-out eksplisit, sedangkan {@code NULL} pada data legacy secara umum
	 * juga berarti tidak dipakai. Pengecualian hanya diberikan oleh getter domain
	 * {@link JenisKegiatan#getDigunakanSyaratKeaktifan()} kepada jenis pendaftaran kanonik. Karena
	 * itu query cache {@link #jenisKegiatansUntukSyaratAktif} wajib memilih hanya nilai
	 * {@code true}; dua jenis kanonik kemudian ditambahkan melalui
	 * {@link #tambahkanJenisKegiatanSyaratAktifDefault(Set, JenisKegiatan)}. Jangan mengubah filter
	 * ini menjadi {@code IS NULL OR = true}. Pola tersebut memasukkan semua kegiatan legacy ke
	 * daftar kewajiban pembayaran dan dapat membuat mahasiswa tetap Nonaktif meskipun tagihan
	 * Daftar Ulang yang benar sudah dibayar.</p>
	 *
	 * <p>Cache ini statis dan berlaku lintas request. Setelah perubahan konfigurasi jenis
	 * kegiatan, panggil method ini atau restart node aplikasi supaya set dibangun ulang. Mesin
	 * status tetap melakukan validasi kedua melalui getter domain pada setiap kegiatan; validasi
	 * itu sengaja dipertahankan agar cache yang sempat stale saat hot-deploy tidak dapat mengubah
	 * kegiatan non-syarat menjadi syarat aktif.</p>
	 *
	 * @param session sesi Hibernate native aktif yang dipakai untuk seluruh query pada method ini
	 */
	@SuppressWarnings("unchecked")
	public static void reloadJenisKegiatans(Session session) {
		try {
			if (session == null || !session.isOpen())
				return;

			ConstantUtil.initIstilahPendaftaran(session);

			jenisKegiatansTanpaDaftarUlang = new TreeSet<JenisKegiatan>((Collection<JenisKegiatan>) session
					.createCriteria(JenisKegiatan.class).addOrder(Order.asc("namaKegiatan"))
					.add(Restrictions.isNotNull("namaKegiatan")).add(Restrictions.ne("namaKegiatan", ""))
					.add(Restrictions.not(Restrictions.in("id",
							new Long[] {
									ConstantValues.PENDAFTARAN_CALON_MAHASISWA == null ? -1L
											: ConstantValues.PENDAFTARAN_CALON_MAHASISWA.getId(),
									ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU == null ? -1L
											: ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU.getId() })))
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).list());

			jenisKegiatansAktif = new TreeSet<JenisKegiatan>((Collection<JenisKegiatan>) session
					.createCriteria(JenisKegiatan.class).addOrder(Order.asc("namaKegiatan"))
					.add(Restrictions.isNotNull("namaKegiatan")).add(Restrictions.ne("namaKegiatan", ""))
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).add(Restrictions
							.or(Restrictions.isNull("defaultKegiatan"), Restrictions.eq("defaultKegiatan", false)))
					.list());

			if (ConstantValues.PENDAFTARAN_MAHASISWA_LAMA != null
					&& ConstantValues.PENDAFTARAN_MAHASISWA_LAMA.getId() != null) {
				session.refresh(ConstantValues.PENDAFTARAN_MAHASISWA_LAMA);
				if (!jenisKegiatansAktif.contains(ConstantValues.PENDAFTARAN_MAHASISWA_LAMA)) {
					jenisKegiatansAktif.add(ConstantValues.PENDAFTARAN_MAHASISWA_LAMA);
				}
			}

			if (ConstantValues.PENDAFTARAN_CALON_MAHASISWA != null
					&& ConstantValues.PENDAFTARAN_CALON_MAHASISWA.getId() != null) {
				session.refresh(ConstantValues.PENDAFTARAN_CALON_MAHASISWA);
				if (!jenisKegiatansAktif.contains(ConstantValues.PENDAFTARAN_CALON_MAHASISWA)) {
					jenisKegiatansAktif.add(ConstantValues.PENDAFTARAN_CALON_MAHASISWA);
				}
			}

			if (ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU != null
					&& ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU.getId() != null) {
				session.refresh(ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU);
				if (!jenisKegiatansAktif.contains(ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU)) {
					jenisKegiatansAktif.add(ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU);
				}
			}

			if (ConstantValues.PENDAFTARAN_WISUDA != null && ConstantValues.PENDAFTARAN_WISUDA.getId() != null) {
				session.refresh(ConstantValues.PENDAFTARAN_WISUDA);
			}

			jenisKegiatansUntukKrs = new TreeSet<JenisKegiatan>((Collection<JenisKegiatan>) session
					.createCriteria(JenisKegiatan.class).add(Restrictions.isNotNull("namaKegiatan"))
					.add(Restrictions.ne("namaKegiatan", "")).addOrder(Order.asc("namaKegiatan"))
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.eq("digunakanUntukPengecekanKrs", true)).list());

			if (jenisKegiatansUntukKrs.isEmpty() && ConstantValues.PENDAFTARAN_MAHASISWA_LAMA != null)
				jenisKegiatansUntukKrs.add(ConstantValues.PENDAFTARAN_MAHASISWA_LAMA);

			jenisKegiatansUntukNilai = new TreeSet<JenisKegiatan>(
					(Collection<JenisKegiatan>) session.createCriteria(JenisKegiatan.class)
							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
							.add(Restrictions.eq("digunakanUntukPengecekanNilai", true)).list());

			if (jenisKegiatansUntukNilai.isEmpty() && ConstantValues.PENDAFTARAN_MAHASISWA_LAMA != null)
				jenisKegiatansUntukNilai.add(ConstantValues.PENDAFTARAN_MAHASISWA_LAMA);

			/*
			 * PENTING: NULL pada digunakanSyaratKeaktifan bukan sinonim TRUE. Getter
			 * JenisKegiatan.getDigunakanSyaratKeaktifan() mengembalikan false untuk
			 * hampir semua data lama yang bernilai NULL; hanya jenis pendaftaran
			 * kanonik tertentu yang memiliki default domain khusus. Query lama memakai
			 * "IS NULL OR = true", sehingga seluruh jenis kegiatan legacy masuk ke set
			 * ini. Dampaknya sangat serius: setelah cicilan dihapus, evaluasi status
			 * melihat kegiatan lain yang sebenarnya bukan syarat aktif sebagai kewajiban
			 * belum bayar. Saat cicilan yang benar dibayar kembali, kewajiban palsu itu
			 * tetap menahan transisi Nonaktif -> Aktif.
			 *
			 * Ambil hanya opt-in eksplisit dari DB. Jenis kanonik yang memang mempunyai
			 * default domain ditambahkan sesudah query melalui getter-nya, agar aturan
			 * Java dan isi cache selalu mempunyai semantik yang sama.
			 */
			jenisKegiatansUntukSyaratAktif = new TreeSet<JenisKegiatan>((Collection<JenisKegiatan>) session
					.createCriteria(JenisKegiatan.class).addOrder(Order.asc("namaKegiatan"))
					.add(Restrictions.isNotNull("namaKegiatan")).add(Restrictions.ne("namaKegiatan", ""))
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.eq("digunakanSyaratKeaktifan", true)).list());

			tambahkanJenisKegiatanSyaratAktifDefault(jenisKegiatansUntukSyaratAktif,
					ConstantValues.PENDAFTARAN_MAHASISWA_LAMA);
			tambahkanJenisKegiatanSyaratAktifDefault(jenisKegiatansUntukSyaratAktif,
					ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU);

			jenisKegiatansUntukSyaratUjian = new TreeSet<JenisKegiatan>((Collection<JenisKegiatan>) session
					.createCriteria(JenisKegiatan.class).add(Restrictions.eq("digunakanUntukPengecekanUjian", true))
					.add(Restrictions.isNotNull("namaKegiatan")).add(Restrictions.ne("namaKegiatan", ""))
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).list());

			if (jenisKegiatansUntukSyaratUjian.isEmpty() && ConstantValues.PENDAFTARAN_MAHASISWA_LAMA != null) {
				jenisKegiatansUntukSyaratUjian.add(ConstantValues.PENDAFTARAN_MAHASISWA_LAMA);
			}

		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CommonHelperClass.java:347");
		}
	}

	/**
	 * Menambahkan jenis kegiatan kanonik yang getter domainnya menetapkan syarat
	 * keaktifan meskipun kolom database lama masih {@code NULL}. Method ini sengaja
	 * memanggil getter domain, bukan menganggap semua {@code NULL} sebagai aktif.
	 */
	private static void tambahkanJenisKegiatanSyaratAktifDefault(Set<JenisKegiatan> tujuan,
			JenisKegiatan jenisKegiatan) {
		if (tujuan != null && jenisKegiatan != null
				&& Boolean.TRUE.equals(jenisKegiatan.getDigunakanSyaratKeaktifan())) {
			tujuan.add(jenisKegiatan);
		}
	}

	/**
	 * Mengisi {@code jenisPembayaranMahasiswa} dengan pilihan {@link #jenisKegiatansTanpaDaftarUlang}
	 * (memuat ulang cache lebih dulu lewat {@link #reloadJenisKegiatans()}), melewati kegiatan
	 * yang tidak aktif, lalu memilih {@link ConstantValues#PENDAFTARAN_MAHASISWA_LAMA} sebagai
	 * item terpilih default.
	 *
	 * @param jenisPembayaranMahasiswa combobox yang akan diisi; dibuat baru bila {@code null}
	 * @return combobox yang sama (atau baru) setelah diisi
	 */
	public static Combobox initJenisPembayaranMahasiswa(Combobox jenisPembayaranMahasiswa) {
		if (jenisPembayaranMahasiswa == null) {
			jenisPembayaranMahasiswa = new Combobox();
		}

		jenisPembayaranMahasiswa.getItems().clear();
		reloadJenisKegiatans();

		if (jenisKegiatansTanpaDaftarUlang != null) {
			for (JenisKegiatan jenisKegiatan : jenisKegiatansTanpaDaftarUlang) {
				if (jenisKegiatan == null || Boolean.FALSE.equals(jenisKegiatan.getAktif())) {
					continue;
				}
				Comboitem comboitem = new Comboitem(jenisKegiatan.getNamaKegiatan());
				comboitem.setValue(jenisKegiatan);
				jenisPembayaranMahasiswa.appendChild(comboitem);
			}
		}

		Common.selectComboItem(jenisPembayaranMahasiswa, ConstantValues.PENDAFTARAN_MAHASISWA_LAMA);
		return jenisPembayaranMahasiswa;
	}

	/**
	 * Mengisi {@code jenisPembayaranMahasiswa} khusus dengan dua pilihan pendaftaran calon
	 * mahasiswa ({@link ConstantValues#PENDAFTARAN_CALON_MAHASISWA} sebagai default terpilih, dan
	 * {@link ConstantValues#PENDAFTARAN_ULANG_MAHASISWA_BARU}); memastikan cache
	 * {@link #jenisKegiatansAktif} sudah dimuat (tanpa memaksanya dimuat ulang bila sudah ada).
	 *
	 * @param jenisPembayaranMahasiswa combobox yang akan diisi; dibuat baru bila {@code null}
	 * @return combobox yang sama (atau baru) setelah diisi
	 */
	public static Combobox initJenisPembayaranBiodataCalonMahasiswa(Combobox jenisPembayaranMahasiswa) {
		if (jenisPembayaranMahasiswa == null) {
			jenisPembayaranMahasiswa = new Combobox();
		}

		jenisPembayaranMahasiswa.getItems().clear();
		MyComboitemConfig comboitem = new MyComboitemConfig(ConstantUtil.PENDAFTARAN_CALON_MAHASISWA);
		comboitem.setValue(ConstantValues.PENDAFTARAN_CALON_MAHASISWA);
		jenisPembayaranMahasiswa.appendChild(comboitem);
		jenisPembayaranMahasiswa.setSelectedItem(comboitem);

		comboitem = new MyComboitemConfig(ConstantUtil.PENDAFTARAN_ULANG_MAHASISWA_BARU);
		comboitem.setValue(ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU);
		jenisPembayaranMahasiswa.appendChild(comboitem);

		if (jenisKegiatansAktif == null) {
			reloadJenisKegiatans();
		}

		return jenisPembayaranMahasiswa;
	}

	/**
	 * Gabungan {@link #initJenisPembayaranMahasiswa(Combobox)} dan
	 * {@link #initJenisPembayaranBiodataCalonMahasiswa(Combobox)}: mengisi combobox dengan seluruh
	 * {@link #jenisKegiatansTanpaDaftarUlang} DITAMBAH dua pilihan pendaftaran calon mahasiswa,
	 * dengan {@link ConstantValues#PENDAFTARAN_MAHASISWA_LAMA} sebagai item terpilih akhir.
	 *
	 * @param jenisPembayaranMahasiswa combobox yang akan diisi; dibuat baru bila {@code null}
	 * @return combobox yang sama (atau baru) setelah diisi
	 */
	public static Combobox initJenisPembayaranMahasiswaDanBiodataCalonMahasiswa(Combobox jenisPembayaranMahasiswa) {
		if (jenisPembayaranMahasiswa == null) {
			jenisPembayaranMahasiswa = new Combobox();
		}

		jenisPembayaranMahasiswa.getItems().clear();
		reloadJenisKegiatans();

		if (jenisKegiatansTanpaDaftarUlang != null) {
			for (JenisKegiatan jenisKegiatan : jenisKegiatansTanpaDaftarUlang) {
				if (jenisKegiatan == null || Boolean.FALSE.equals(jenisKegiatan.getAktif())) {
					continue;
				}
				Comboitem comboitem = new Comboitem(jenisKegiatan.getNamaKegiatan());
				comboitem.setValue(jenisKegiatan);
				jenisPembayaranMahasiswa.appendChild(comboitem);
			}
		}

		MyComboitemConfig comboitem = new MyComboitemConfig(ConstantUtil.PENDAFTARAN_CALON_MAHASISWA);
		comboitem.setValue(ConstantValues.PENDAFTARAN_CALON_MAHASISWA);
		jenisPembayaranMahasiswa.appendChild(comboitem);
		jenisPembayaranMahasiswa.setSelectedItem(comboitem);

		comboitem = new MyComboitemConfig(ConstantUtil.PENDAFTARAN_ULANG_MAHASISWA_BARU);
		comboitem.setValue(ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU);
		jenisPembayaranMahasiswa.appendChild(comboitem);

		Common.selectComboItem(jenisPembayaranMahasiswa, ConstantValues.PENDAFTARAN_MAHASISWA_LAMA);

		return jenisPembayaranMahasiswa;
	}

	/** Seperti {@link #initJenisSemester(Combobox, boolean)} tanpa pilihan Semester Pendek ({@code sp=false}). */
	public static Combobox initJenisSemester(Combobox jenisSemester) {
		return initJenisSemester(jenisSemester, false);
	}

	/**
	 * Mengisi {@code jenisSemester} (dijadikan read-only) dengan pilihan Ganjil/Genap, dan
	 * opsional Semester Pendek ({@link Perkuliahan#SP}) bila {@code sp=true}; item terpilih default
	 * mengikuti semester berjalan saat ini ({@link Common#isNowSemensterGanjil()}).
	 *
	 * @param jenisSemester combobox yang akan diisi; dibuat baru bila {@code null}
	 * @param sp            {@code true} untuk menyertakan pilihan Semester Pendek
	 * @return combobox yang sama (atau baru) setelah diisi
	 */
	public static Combobox initJenisSemester(Combobox jenisSemester, boolean sp) {
		if (jenisSemester == null) {
			jenisSemester = new Combobox();
		}
		jenisSemester.setReadonly(true);

		MyComboitemConfig comboitem = new MyComboitemConfig(Perkuliahan.GANJIL);
		comboitem.setValue(Perkuliahan.GANJIL);
		jenisSemester.appendChild(comboitem);

		comboitem = new MyComboitemConfig(Perkuliahan.GENAP);
		comboitem.setValue(Perkuliahan.GENAP);
		jenisSemester.appendChild(comboitem);

		if (sp) {
			comboitem = new MyComboitemConfig(Perkuliahan.SP);
			comboitem.setValue(Perkuliahan.SP);
			jenisSemester.appendChild(comboitem);
		}
		Boolean ganjil = Common.isNowSemensterGanjil();
		Common.selectComboItem(jenisSemester, ganjil != null && ganjil ? Perkuliahan.GANJIL : Perkuliahan.GENAP);

		return jenisSemester;
	}

	/** @return {@link Perkuliahan#GANJIL} atau {@link Perkuliahan#GENAP} sesuai semester berjalan saat ini ({@link Common#isNowSemensterGanjil()}). */
	public static String getSemesterString() {
		Boolean ganjil = Common.isNowSemensterGanjil();
		return (ganjil != null && ganjil) ? Perkuliahan.GANJIL : Perkuliahan.GENAP;
	}

	/**
	 * Membangun riwayat status per-semester seorang mahasiswa, dari semester mulainya (disesuaikan
	 * bila pernah pindah kampus lewat {@code getPindahKeKampusIniMasukSemester()}) sampai tahun
	 * akademik berjalan (atau sampai statusnya menjadi "lulus"/"keluar", yang menghentikan
	 * perulangan lebih awal). Untuk program studi dengan {@code jumlahTahapan == 3}, kolom status
	 * dikosongkan (ditentukan lewat mekanisme tahapan yang berbeda oleh pemanggil lain).
	 *
	 * @param mahasiswa mahasiswa yang riwayat semesternya dihitung; bila {@code null} atau data
	 *                  semester mulai/tahun angkatan belum lengkap, mengembalikan peta kosong
	 * @return peta terurut nomor semester → larik {@code [tahunAkademik, nomorSemester, status,
	 *         "", namaSemester]}
	 */
	public static TreeMap<Integer, String[]> generateStatusSemester(Mahasiswa mahasiswa) {
		TreeMap<Integer, String[]> treeMap = new TreeMap<Integer, String[]>();
		if (mahasiswa == null || mahasiswa.getSemesterMulai() == null || mahasiswa.getTahunangkatan() == null) {
			return treeMap;
		}

		boolean mulaiGanjil = mahasiswa.getSemesterMulai().equals(Perkuliahan.GANJIL);
		int semester = mulaiGanjil ? 1 : 0;
		int tahunAkademik = 8;
		boolean berhenti = false;

		String ta = Common.getCurrentTahunAkademik();
		Integer tahunAsli = 0;

		if (ta != null && ta.contains("/")) {
			try {
				tahunAsli = Integer.parseInt(StringUtils.split(ta, "/")[0]);
			} catch (NumberFormatException e) {
				// Fallback value if parse fails
				tahunAsli = Calendar.getInstance().get(Calendar.YEAR);
			}
		}

		Integer tahun = (tahunAsli + tahunAkademik);
		int startIndex = (mahasiswa.getTahunangkatan() - ((int) (mahasiswa.getPindahKeKampusIniMasukSemester() / 2.0)));

		for (int i = startIndex; i < tahun; i++) {
			if (semester >= 50 || berhenti)
				break;

			String status = "";
			String tahunAkademikIndex = i + "/" + (i + 1);
			Integer thp = ConstantValues.getJumlahTahapan(mahasiswa.getProgram(), mahasiswa.getJurusan());

			KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa, semester, null, null);

			// Loop dieksekusi 2x untuk Ganjil & Genap per tahun akademik
			for (int j = 0; j < 2; j++) {
				if (semester > 0) {
					String namaSmt = mulaiGanjil ? (semester % 2 == 0 ? Perkuliahan.GANJIL : Perkuliahan.GENAP)
							: (semester % 2 == 1 ? Perkuliahan.GANJIL : Perkuliahan.GENAP);

					status = (thp != null && thp == 3) ? ""
							: (krsMahasiswa != null && ais.action.master.helper.HistoryStatusMahasiswaUtil.currentStatus(krsMahasiswa) != null
									? ais.action.master.helper.HistoryStatusMahasiswaUtil.currentStatus(krsMahasiswa).getStatusMahasiswa().getNama()
									: "");

					treeMap.put(semester, new String[] { tahunAkademikIndex, String.valueOf(semester),
							(status == null ? "" : status), "", namaSmt });

					if (status != null && (status.equalsIgnoreCase("lulus") || status.equalsIgnoreCase("keluar"))) {
						berhenti = true;
						break;
					}
				}
				semester++;
			}
		}
		return treeMap;
	}

	/**
	 * Menyusun daftar baris untuk grid UI riwayat semester/tahapan mahasiswa dalam rentang
	 * {@code [mulai, sampai]}, dibangun di atas {@link #generateStatusSemester(Mahasiswa)} dengan
	 * pemrosesan tambahan bergantung pada jumlah tahapan pembayaran program studi mahasiswa
	 * ({@link ConstantValues#getJumlahTahapan}): tanpa tahapan atau 2 tahap → satu baris per
	 * semester apa adanya; 3 tahap → logika penggabungan semester genap-ganjil menjadi satu
	 * tahap gabungan (khusus mahasiswa yang mulai di semester genap) beserta baris "Tanpa Tahap";
	 * 4 tahap → setiap semester dipecah menjadi dua baris tahap berurutan. Baris "Konversi"
	 * ditambahkan di akhir bila {@code semesterPendek} {@code null}. Kegagalan pemrosesan dicatat
	 * ke {@link ais.common.ErrorAuditUtil} dan data yang sudah terkumpul sejauh itu dikembalikan
	 * apa adanya (bukan dilempar ulang).
	 *
	 * @param mahasiswa      mahasiswa yang datanya diproses
	 * @param mulai          nomor semester awal rentang yang disertakan
	 * @param sampai         nomor semester akhir rentang yang disertakan
	 * @param semesterPendek bila {@code null}, baris "Konversi" ditambahkan di akhir daftar
	 * @return daftar baris grid, masing-masing larik string dengan kolom bergantung skema tahapan
	 *         yang berlaku; daftar kosong bila {@code mahasiswa} {@code null}/tahun angkatan kosong
	 */
	public static List<String[]> generateSemestersForGrid(Mahasiswa mahasiswa, int mulai, int sampai,
			Integer semesterPendek) {
		List<String[]> data = new ArrayList<String[]>();
		try {
			if (mahasiswa == null || mahasiswa.getTahunangkatan() == null) {
				return data;
			}

			if (ConstantValues.jumlahTahapan == null || ConstantValues.jumlahTahapan.isEmpty()) {
				ConstantValues.initJumlahTahapan();
			}

			int tahap = 1;
			Integer thp = ConstantValues.getJumlahTahapan(mahasiswa.getProgram(), mahasiswa.getJurusan());
			TreeMap<Integer, String[]> treeMap = generateStatusSemester(mahasiswa);

			if (!ConstantValues.aktifkanTahapan || (thp != null && thp == 2)) {
				for (Integer semester : treeMap.keySet()) {
					if (semester >= mulai && semester <= sampai) {
						String[] d = Arrays.copyOf(treeMap.get(semester), 5);
						d[3] = "0";
						data.add(d);
					}
					tahap++;
				}
			} else if (thp != null && thp == 3) {
				// (Logika disederhanakan tanpa mengubah tujuan aslinya)
				for (Integer semester : treeMap.keySet()) {
					// Konfigurasi ini dibiarkan serupa agar tidak memutus legacy UI grid
					if (mahasiswa.getSemesterMulai().equals(Perkuliahan.GANJIL)) {
						if (semester % 2 == 1) {
							if (semester >= mulai && semester <= sampai) {
								String[] d = Arrays.copyOf(treeMap.get(semester), 5);
								d[3] = String.valueOf(tahap);
								d[2] = ais.action.master.helper.HistoryStatusMahasiswaUtil.currentStatus(mahasiswa, tahap).getStatusMahasiswa().getNama();
								data.add(d);
							}
							tahap++;
						} else if (semester % 2 == 0) {
							if (semester >= mulai && semester <= sampai) {
								String[] d1 = Arrays.copyOf(treeMap.get(semester), 5);
								d1[1] = (semester - 1) + "," + semester;
								d1[3] = String.valueOf(tahap);
								d1[2] = ais.action.master.helper.HistoryStatusMahasiswaUtil.currentStatus(mahasiswa, tahap).getStatusMahasiswa().getNama();
								data.add(d1);

								tahap++;

								String[] d2 = Arrays.copyOf(treeMap.get(semester), 5);
								d2[3] = String.valueOf(tahap);
								d2[2] = ais.action.master.helper.HistoryStatusMahasiswaUtil.currentStatus(mahasiswa, tahap).getStatusMahasiswa().getNama();
								data.add(d2);
							}
							tahap++;
						}
					} else {
						if (semester.equals(1)) {
							for (int k = 0; k < 3; k++) {
								if (semester >= mulai && semester <= sampai) {
									String[] d = Arrays.copyOf(treeMap.get(semester), 5);
									d[3] = String.valueOf(tahap);
									d[2] = ais.action.master.helper.HistoryStatusMahasiswaUtil.currentStatus(mahasiswa, tahap).getStatusMahasiswa().getNama();
									data.add(d);
								}
								if (k < 2)
									tahap++;
							}
						} else if (semester % 2 == 1) {
							tahap++;
							if (semester >= mulai && semester <= sampai) {
								String[] d1 = Arrays.copyOf(treeMap.get(semester), 5);
								d1[1] = (semester - 1) + "," + semester;
								d1[3] = String.valueOf(tahap);
								d1[2] = ais.action.master.helper.HistoryStatusMahasiswaUtil.currentStatus(mahasiswa, tahap).getStatusMahasiswa().getNama();
								data.add(d1);
							}
							tahap++;
							if (semester >= mulai && semester <= sampai) {
								String[] d2 = Arrays.copyOf(treeMap.get(semester), 5);
								d2[3] = String.valueOf(tahap);
								d2[2] = ais.action.master.helper.HistoryStatusMahasiswaUtil.currentStatus(mahasiswa, tahap).getStatusMahasiswa().getNama();
								data.add(d2);
							}
						} else if (semester % 2 == 0) {
							tahap++;
							if (semester >= mulai && semester <= sampai) {
								String[] d = Arrays.copyOf(treeMap.get(semester), 5);
								d[3] = String.valueOf(tahap);
								d[2] = ais.action.master.helper.HistoryStatusMahasiswaUtil.currentStatus(mahasiswa, tahap).getStatusMahasiswa().getNama();
								data.add(d);
							}
						}
					}
				}
				data.add(new String[] { "Tanpa Tahap", "", "", "-1" });

			} else if ((thp != null && thp == 4)) {
				for (Integer semester : treeMap.keySet()) {
					if (semester >= mulai && semester <= sampai) {
						String[] d1 = Arrays.copyOf(treeMap.get(semester), 4);
						d1[3] = String.valueOf(tahap);
						data.add(d1);
					}
					tahap++;

					if (semester >= mulai && semester <= sampai) {
						String[] d2 = Arrays.copyOf(treeMap.get(semester), 4);
						d2[3] = String.valueOf(tahap);
						data.add(d2);
					}
					tahap++;
				}
				data.add(new String[] { "Tanpa Tahap", "", "", "-1" });
			}

			if (semesterPendek == null) {
				data.add(new String[] { "Konversi", "" });
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CommonHelperClass.java:638");
		}
		return data;
	}

	/**
	 * Varian sederhana {@link #generateSemestersForGrid} yang menghasilkan baris grid berdasarkan
	 * {@code jumlahTahapan} eksplisit yang diberikan pemanggil (bukan dibaca dari konfigurasi
	 * program studi), tanpa memuat status pembayaran per semester — hanya kombinasi
	 * (tahun akademik, nomor tahap). Nomor semester dinaikkan dua per tahun akademik.
	 *
	 * @param mahasiswa     mahasiswa yang riwayat tahunnya dihitung
	 * @param jumlahTahapan jumlah tahap yang dibuat per tahun akademik
	 * @return daftar baris {@code [tahunAkademik, nomorTahap, ""]} diakhiri baris "Konversi";
	 *         daftar kosong bila {@code mahasiswa} {@code null}/tahun angkatan kosong
	 */
	public static List<String[]> generateSemestersForGridTahapan(Mahasiswa mahasiswa, int jumlahTahapan) {
		List<String[]> data = new ArrayList<String[]>();
		if (mahasiswa == null || mahasiswa.getTahunangkatan() == null) {
			return data;
		}

		int semester = 1;
		String ta = Common.getCurrentTahunAkademik();
		Integer tahun = 0;
		if (ta != null && ta.contains("/")) {
			try {
				tahun = Integer.parseInt(StringUtils.split(ta, "/")[0]);
			} catch (NumberFormatException e) {
				tahun = Calendar.getInstance().get(Calendar.YEAR);
			}
		}
		tahun = (tahun + 1);

		int startIndex = (mahasiswa.getTahunangkatan() - ((int) (mahasiswa.getPindahKeKampusIniMasukSemester() / 2.0)));
		for (int i = startIndex; i < tahun; i++) {
			if (semester >= 20)
				break;
			semester += 2; // Naik 2 sekaligus

			for (int tahap = 1; tahap <= jumlahTahapan; tahap++) {
				data.add(new String[] { i + "/" + (i + 1), String.valueOf(tahap), "" });
			}
		}

		data.add(new String[] { "Konversi", "" });
		return data;
	}

	/**
	 * Mengisi {@code conf} dengan seluruh entri {@link Konfigurasi#konfigurasi} (peta kunci
	 * konfigurasi yang diketahui aplikasi), label tampil berupa nilai peta dan {@code value}
	 * berupa kunci peta.
	 *
	 * @param conf combobox yang akan diisi; dibuat baru bila {@code null}
	 * @return combobox yang sama (atau baru) setelah diisi
	 */
	public static Combobox createComboKonfigurasi(Combobox conf) {
		if (conf == null) {
			conf = new Combobox();
		}
		if (Konfigurasi.konfigurasi != null) {
			for (String h : Konfigurasi.konfigurasi.keySet()) {
				org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
				comboitem.setLabel(Konfigurasi.konfigurasi.get(h));
				comboitem.setValue(h);
				conf.appendChild(comboitem);
			}
		}
		return conf;
	}

	/**
	 * Memeriksa apakah ada baris {@link PengecualianJadwalPenilaianDosen} yang berlaku HARI INI
	 * (rentang {@code tanggalMulai}..{@code tanggalSampai}) dan berstatus disetujui (atau
	 * {@code null}, ditafsirkan sebagai "Disetujui" untuk data lama), untuk {@code tahunAkademik}
	 * dan {@code jenisSemester} yang diberikan, yang mengizinkan dosen/pengelola bersangkutan
	 * memberi nilai di luar jadwal penilaian normal.
	 *
	 * <p>
	 * Identitas pemeriksa ditentukan oleh peran akun yang login: bila {@code tbmuser} berperan
	 * {@link Tbmrole#DOSEN}, pengecualian dicocokkan lewat relasi {@code dosen}; bila akun tidak
	 * punya hak akses eksplisit tapi entitas {@code dosen} tersedia, jalur yang sama juga dipakai.
	 * Untuk peran pengelola lain, pencocokan memakai {@code userId} pada relasi {@code tbmuser}
	 * pengecualian (menangani kasus satu akun petugas yang juga terhubung ke master Dosen, di mana
	 * izin tersimpan pada kolom {@code tbmuser}, bukan {@code dosen}).
	 * </p>
	 *
	 * @param dosen         entitas dosen yang diperiksa, boleh {@code null} bila pemeriksa bukan dosen
	 * @param tbmuser       akun pengguna yang sedang login
	 * @param tahunAkademik tahun akademik yang diperiksa (format {@code "YYYY/YYYY"})
	 * @param jenisSemester {@link Perkuliahan#GANJIL} atau {@link Perkuliahan#GENAP}
	 * @return {@code true} bila ditemukan pengecualian yang berlaku dan disetujui; {@code false}
	 *         bila parameter tidak lengkap, tidak ditemukan pengecualian, atau terjadi galat
	 */
	public static Boolean checkApakahDosenBolehMenilai(Dosen dosen, Tbmuser tbmuser, String tahunAkademik,
			String jenisSemester) {
		Session session = null;
		try {
			if (tahunAkademik == null || tahunAkademik.trim().isEmpty() || jenisSemester == null
					|| jenisSemester.trim().isEmpty()) {
				return false;
			}
			tahunAkademik = tahunAkademik.trim();
			jenisSemester = jenisSemester.trim();
			Calendar hariIni = Calendar.getInstance();
			hariIni.setTime(WaktuUtil.getDate());
			hariIni.set(Calendar.HOUR_OF_DAY, 0);
			hariIni.set(Calendar.MINUTE, 0);
			hariIni.set(Calendar.SECOND, 0);
			hariIni.set(Calendar.MILLISECOND, 0);
			session = HibernateUtil.currentNativeSession();
			/*
			 * Satu akun petugas/admin dapat sekaligus terhubung ke master Dosen. Identitas
			 * pengecualian harus mengikuti ROLE yang sedang login: role Dosen memakai kolom
			 * dosen, sedangkan seluruh role pengelola memakai kolom tbmuser. Pemeriksaan lama
			 * hanya melihat keberadaan relasi dosen sehingga izin Admin Prodi tersimpan pada
			 * tbmuser tetapi dibaca dari kolom dosen dan selalu dianggap tidak ada.
			 */
			boolean hakAksesDosen = tbmuser != null && tbmuser.hakAkses() != null
					&& Tbmrole.DOSEN.equalsIgnoreCase(tbmuser.hakAkses().getRoleId());
			Criterion identitas = null;
			if (hakAksesDosen || (tbmuser != null && tbmuser.hakAkses() == null && dosen != null)) {
				identitas = dosen == null || dosen.getId() == null ? Restrictions.sqlRestriction("1=0")
						: Restrictions.eq("dosen", dosen);
			}
			Criteria izin = session.createCriteria(PengecualianJadwalPenilaianDosen.class)
					/*
					 * Data versi lama menyimpan status NULL dan getStatus() menampilkannya sebagai
					 * "Disetujui". Tetap baca data lama tersebut, sedangkan data baru selalu
					 * disimpan eksplisit sebagai "Pengajuan" sampai disetujui role Admin (am).
					 */
					.add(Restrictions.or(Restrictions.eq("status", PengecualianJadwalPenilaianDosen.DISETUJU),
							Restrictions.isNull("status")))
					.add(Restrictions.eq("tahunAkademik", tahunAkademik))
					.add(Restrictions.eq("jenisSemester", jenisSemester))
					.add(Restrictions.le("tanggalMulai", hariIni.getTime()))
					.add(Restrictions.ge("tanggalSampai", hariIni.getTime()));
			if (identitas != null) {
				izin.add(identitas);
			} else if (tbmuser != null && tbmuser.getUserId() != null
					&& !tbmuser.getUserId().trim().isEmpty()) {
				/*
				 * Cocokkan akun pengelola dengan userid, bukan instance entity dari HttpSession.
				 * Ini menangani referensi Tbmuser lama/berbeda yang masih menyimpan userid sama.
				 */
				izin.createAlias("tbmuser", "izinUser");
				izin.add(Restrictions.eq("izinUser.userId", tbmuser.getUserId().trim()));
			} else {
				izin.add(Restrictions.sqlRestriction("1=0"));
			}
			Integer count = ((Number) izin.setProjection(Projections.rowCount()).uniqueResult()).intValue();
			return count > 0;
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CommonHelperClass.java:708");
			return false;
		} finally {
			Common.closeNativeSessionQuietly(session);
		}
	}

	/**
	 * Memeriksa apakah ada baris {@link PengecualianJadwalPengisianKRSMahasiswa} yang berlaku HARI
	 * INI untuk {@code mahasiswa}, {@code tahunAkademik}, dan {@code jenisSemester} yang diberikan
	 * — bila ada, mahasiswa diizinkan mengambil/mengubah KRS di luar jadwal pengisian normal
	 * maupun tanpa memenuhi syarat pembayaran (dipakai sebagai salah satu jalur "lolos" pada
	 * berbagai method {@code checkStatusPembayaran*}/{@code checkPembayaran*} di kelas ini).
	 *
	 * @param mahasiswa     mahasiswa yang diperiksa
	 * @param tahunAkademik tahun akademik yang diperiksa (format {@code "YYYY/YYYY"})
	 * @param jenisSemester {@link Perkuliahan#GANJIL} atau {@link Perkuliahan#GENAP}
	 * @return {@code true} bila ditemukan pengecualian yang berlaku hari ini; {@code false} bila
	 *         tidak ditemukan atau terjadi galat
	 */
	public static Boolean checkApakahMahasiswaBolehAmbilKrsLewatPengecualian(Mahasiswa mahasiswa, String tahunAkademik,
			String jenisSemester) {
		Session session = null;
		try {
			session = HibernateUtil.currentNativeSession();
			Integer count = ((Number) session.createCriteria(PengecualianJadwalPengisianKRSMahasiswa.class)
					.add(Restrictions.eq("tahunAkademik", tahunAkademik))
					.add(Restrictions.eq("jenisSemester", jenisSemester)).add(Restrictions.eq("mahasiswa", mahasiswa))
					.add(Restrictions.sqlRestriction("date('" + Common.databaseDateFormat1.get().format(WaktuUtil.getDate())
							+ "') between this_.tanggal_mulai and this_.tanggal_sampai"))
					.setProjection(Projections.rowCount()).uniqueResult()).intValue();
			return count > 0;
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CommonHelperClass.java:732");
			return false;
		} finally {
			HibernateUtil.closeSession();
		}
	}

	/**
	 * Memvalidasi apakah {@code username} SUDAH dipakai oleh akun/entitas lain, diperiksa
	 * berurutan (berhenti pada kecocokan pertama untuk menghemat query) terhadap empat sumber:
	 * kolom {@code userId} pada {@link Tbmuser}, {@code userOrtu} dan {@code nim} pada
	 * {@link Mahasiswa} aktif, serta {@code username} pada {@link BiodataCalonMahasiswa} aktif.
	 * Parameter {@code userId}/{@code id} dipakai untuk MENGECUALIKAN baris milik entitas yang
	 * sedang diedit sendiri dari pemeriksaan (agar mengedit tanpa mengubah username tidak dianggap
	 * bentrok dengan dirinya sendiri).
	 *
	 * @param username username yang akan diperiksa keunikannya
	 * @param userId   {@code userId} {@link Tbmuser} milik entitas yang sedang diedit (dikecualikan
	 *                 dari pemeriksaan pada tabel Tbmuser); {@code null} berarti tidak ada
	 *                 pengecualian (mode tambah baru)
	 * @param id       id baris {@link Mahasiswa}/{@link BiodataCalonMahasiswa} yang sedang diedit
	 *                 (dikecualikan dari pemeriksaan pada kedua tabel tersebut); {@code null}
	 *                 berarti tidak ada pengecualian
	 * @return {@code true} bila {@code username} sudah dipakai entitas lain (bentrok); {@code false}
	 *         bila {@code username} kosong, tersedia, atau terjadi galat saat pemeriksaan
	 */
	public static Boolean checkUsername(String username, String userId, Long id) {
		if (username == null || username.trim().isEmpty()) {
			return false;
		}

		Session session = null;
		try {
			session = HibernateUtil.currentNativeSession();
			String trimmedUsername = username.trim();
			String trimmedUserId = (userId != null) ? userId.trim() : null;

			// Cek secara berurutan, jika salah satu ada, langsung kembalikan true
			// (menghemat query database)
			// userId adalah primary key Tbmuser. Akun nonaktif tetap memakai userId
			// tersebut dan tidak boleh dianggap tersedia untuk insert akun baru.
			Integer userCount = ((Number) session.createCriteria(Tbmuser.class)
					.setProjection(Projections.rowCount()).add(Restrictions.eq("userId", trimmedUsername))
					.add(userId == null ? Restrictions.sqlRestriction("1=1") : Restrictions.ne("userId", trimmedUserId))
					.uniqueResult()).intValue();
			if (userCount > 0)
				return true;

			Integer userOrtuCount = ((Number) session.createCriteria(Mahasiswa.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.setProjection(Projections.rowCount()).add(Restrictions.eq("userOrtu", trimmedUsername))
					.add(id == null ? Restrictions.sqlRestriction("1=1") : Restrictions.ne("id", id)).uniqueResult())
					.intValue();
			if (userOrtuCount > 0)
				return true;

			Integer userNimCount = ((Number) session.createCriteria(Mahasiswa.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.setProjection(Projections.rowCount()).add(Restrictions.eq("nim", trimmedUsername))
					.add(id == null ? Restrictions.sqlRestriction("1=1") : Restrictions.ne("id", id)).uniqueResult())
					.intValue();
			if (userNimCount > 0)
				return true;

			Integer userCalonMhsCount = ((Number) session.createCriteria(BiodataCalonMahasiswa.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.setProjection(Projections.rowCount()).add(Restrictions.eq("username", trimmedUsername))
					.add(id == null ? Restrictions.sqlRestriction("1=1") : Restrictions.ne("id", id)).uniqueResult())
					.intValue();
			if (userCalonMhsCount > 0)
				return true;

			return false;
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CommonHelperClass.java:786");
			return false; // Anggap false jika terjadi exception, atau bisa disesuaikan dengan kebutuhan
		} finally {
			HibernateUtil.closeSession();
		}
	}

	/**
	 * Mengisi {@code jenisPembayaran} dengan opsi "Semua" (dipilih default, {@code value=null})
	 * DITAMBAH seluruh jenis pendaftaran baku ({@link ConstantValues#PENDAFTARAN_MAHASISWA_LAMA},
	 * {@code PENDAFTARAN_CALON_MAHASISWA}, {@code PENDAFTARAN_WISUDA},
	 * {@code PENDAFTARAN_ULANG_MAHASISWA_BARU}) dan seluruh {@link #jenisKegiatansAktif}. Cocok
	 * untuk filter pencarian di mana "tanpa filter jenis" adalah pilihan valid.
	 *
	 * @param jenisPembayaran combobox yang akan diisi; dibuat baru bila {@code null}
	 * @return combobox yang sama (atau baru) setelah diisi
	 */
	public static Combobox createComboJenisPembayaranDanSemua(Combobox jenisPembayaran) {
		if (jenisPembayaran == null) {
			jenisPembayaran = new Combobox();
		}

		MyComboitemConfig comboitem = new MyComboitemConfig("Semua");
		comboitem.setValue(null);
		jenisPembayaran.appendChild(comboitem);
		jenisPembayaran.setSelectedItem(comboitem);

		comboitem = new MyComboitemConfig(ConstantUtil.PENDAFTARAN_MAHASISWA_LAMA);
		comboitem.setValue(ConstantValues.PENDAFTARAN_MAHASISWA_LAMA);
		jenisPembayaran.appendChild(comboitem);

		comboitem = new MyComboitemConfig(ConstantUtil.PENDAFTARAN_CALON_MAHASISWA);
		comboitem.setValue(ConstantValues.PENDAFTARAN_CALON_MAHASISWA);
		jenisPembayaran.appendChild(comboitem);

		comboitem = new MyComboitemConfig(ConstantUtil.PENDAFTARAN_WISUDA);
		comboitem.setValue(ConstantValues.PENDAFTARAN_WISUDA);
		jenisPembayaran.appendChild(comboitem);

		comboitem = new MyComboitemConfig(ConstantUtil.PENDAFTARAN_ULANG_MAHASISWA_BARU);
		comboitem.setValue(ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU);
		jenisPembayaran.appendChild(comboitem);

		if (jenisKegiatansAktif == null) {
			reloadJenisKegiatans();
		}
		for (JenisKegiatan jenisKegiatan : jenisKegiatansAktif) {
			comboitem = new MyComboitemConfig(jenisKegiatan.getNamaKegiatan());
			comboitem.setValue(jenisKegiatan);
			jenisPembayaran.appendChild(comboitem);
		}
		return jenisPembayaran;
	}

	/**
	 * Seperti {@link #createComboJenisPembayaranDanSemua(Combobox)} tetapi TANPA opsi "Semua" —
	 * item terpilih default adalah {@link ConstantValues#PENDAFTARAN_MAHASISWA_LAMA}. Cocok untuk
	 * form entri di mana suatu jenis pembayaran WAJIB dipilih.
	 *
	 * @param jenisPembayaran combobox yang akan diisi; dibuat baru bila {@code null}
	 * @return combobox yang sama (atau baru) setelah diisi
	 */
	public static Combobox createComboJenisPembayaran(Combobox jenisPembayaran) {
		if (jenisPembayaran == null) {
			jenisPembayaran = new Combobox();
		}
		MyComboitemConfig comboitem = new MyComboitemConfig(ConstantUtil.PENDAFTARAN_MAHASISWA_LAMA);
		comboitem.setValue(ConstantValues.PENDAFTARAN_MAHASISWA_LAMA);
		jenisPembayaran.appendChild(comboitem);
		jenisPembayaran.setSelectedItem(comboitem);

		comboitem = new MyComboitemConfig(ConstantUtil.PENDAFTARAN_CALON_MAHASISWA);
		comboitem.setValue(ConstantValues.PENDAFTARAN_CALON_MAHASISWA);
		jenisPembayaran.appendChild(comboitem);

		comboitem = new MyComboitemConfig(ConstantUtil.PENDAFTARAN_WISUDA);
		comboitem.setValue(ConstantValues.PENDAFTARAN_WISUDA);
		jenisPembayaran.appendChild(comboitem);

		comboitem = new MyComboitemConfig(ConstantUtil.PENDAFTARAN_ULANG_MAHASISWA_BARU);
		comboitem.setValue(ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU);
		jenisPembayaran.appendChild(comboitem);

		if (jenisKegiatansAktif == null) {
			reloadJenisKegiatans();
		}
		for (JenisKegiatan jenisKegiatan : jenisKegiatansAktif) {
			comboitem = new MyComboitemConfig(jenisKegiatan.getNamaKegiatan());
			comboitem.setValue(jenisKegiatan);
			jenisPembayaran.appendChild(comboitem);
		}
		return jenisPembayaran;
	}

	/**
	 * Menjumlahkan total nominal tagihan {@code mahasiswa} pada {@code semester} tertentu untuk
	 * seluruh {@link #jenisKegiatansUntukKrs} (memuat ulang cache lebih dulu bila belum ada),
	 * termasuk tagihan yang dijadwalkan lewat skema cicilan bulanan
	 * ({@link PengaturanPembayaranBulanan}) — bila mahasiswa/kegiatan memiliki pengaturan cicilan
	 * bulanan aktif ({@code countBulanan > 0}), detail biaya diambil ulang khusus untuk skema
	 * tersebut. Hasil ini menjadi penyebut dasar untuk menentukan apakah seorang mahasiswa
	 * "punya tagihan sama sekali" pada suatu semester (dipakai method {@code checkStatusPembayaran*}
	 * lain untuk melewatkan pemeriksaan bila total tagihan mendekati nol).
	 *
	 * @param session   sesi Hibernate aktif yang dipakai untuk query pengaturan cicilan bulanan
	 * @param mahasiswa mahasiswa yang tagihannya dihitung
	 * @param semester  nomor semester yang diperiksa
	 * @return total nominal tagihan (dari {@link DetailBiaya} dan/atau
	 *         {@link PengaturanPembayaranBulanan}) untuk seluruh jenis kegiatan syarat KRS
	 */
	@SuppressWarnings({ "rawtypes" })
	public static Double hitungTagihanMahasiswaSebagaiSyaratKrs(Session session, Mahasiswa mahasiswa,
			Integer semester) {

		if (jenisKegiatansUntukKrs == null) {
			reloadJenisKegiatans();
		}
		Double totaltagihan = 0.0;
		for (JenisKegiatan jenisKegiatan : jenisKegiatansUntukKrs) {
			PembayaranUtil.getInstance();
			Collection detailBiayas = PembayaranUtilHelper.getDetailBiayaMahasiswa(mahasiswa, semester,
					jenisKegiatan, false);
			PembayaranUtil.getInstance();
			int countPengaturanBulanan = PembayaranUtilHelper.countBulanan(session, mahasiswa, jenisKegiatan,
					semester, detailBiayas, false, false);

			if (countPengaturanBulanan > 0) {
				PembayaranUtil.getInstance();
				detailBiayas = PembayaranUtilHelper.getDetailBiayaMahasiswa(mahasiswa, semester, jenisKegiatan,
						"-1", true, false);
			}

			for (Object o : detailBiayas) {
				if (o instanceof DetailBiaya) {
					DetailBiaya biaya = (DetailBiaya) o;
					totaltagihan += biaya.getNilaiBiaya();
				} else if (o instanceof PengaturanPembayaranBulanan) {
					PengaturanPembayaranBulanan biaya = (PengaturanPembayaranBulanan) o;
					totaltagihan += biaya.getNominal();
				}
			}
			detailBiayas = null;
		}
		System.out
				.println("totaltagihan untuk pengambilan KRS => " + totaltagihan + ", " + mahasiswa + ", " + semester);
		return totaltagihan;
	}

	/**
	 * Gerbang syarat pembayaran UTAMA untuk pengambilan/persetujuan KRS pada semester berjalan.
	 * Mengembalikan {@code true} (lolos) melalui salah satu dari beberapa jalur, diperiksa
	 * berurutan: (1) status baypass eksplisit ({@code Common.checkBaypassStatusPembayaranMahasiswa});
	 * (2) pengecualian jadwal KRS yang berlaku ({@link
	 * #checkApakahMahasiswaBolehAmbilKrsLewatPengecualian}); (3) ambang batas persentase pembayaran
	 * dikonfigurasi {@code 0} (berarti syarat pembayaran dimatikan untuk konteks
	 * jurusan/program/angkatan/status mahasiswa tersebut); (4) total tagihan mahasiswa pada
	 * semester ini mendekati nol ({@link #hitungTagihanMahasiswaSebagaiSyaratKrs} {@code < 0.01});
	 * atau (5) persentase pemenuhan tagihan aktual mahasiswa pada SALAH SATU
	 * {@link #jenisKegiatansUntukKrs} sudah mencapai ambang batas. Bila mahasiswa baru
	 * (semester {@code <= 1}) belum lolos dan konfigurasi
	 * {@code mahasiswa_baru_mengikuti_persyaratan_krs_spt_mahasiswa} TIDAK aktif, pemeriksaan
	 * tambahan dilakukan terhadap data {@link BiodataCalonMahasiswa} terkait (jalur pembayaran
	 * pendaftaran, bukan KRS mahasiswa aktif). Bila akhirnya tetap tidak lolos, dialog peringatan
	 * ditampilkan langsung ke pengguna lewat {@link MyMessageboxConfig#showFormat} yang menyebutkan
	 * NIM, konteks (semester/tahap), dan daftar jenis pembayaran yang harus diselesaikan.
	 *
	 * @param mahasiswa  mahasiswa yang diperiksa
	 * @param semester   nomor semester yang akan diambil KRS-nya
	 * @param tahap      nomor tahap (dipakai hanya untuk teks pesan bila
	 *                   {@code aktifkanTahapanTerhubungKeKeuangan} aktif), boleh {@code null}
	 * @param persetujuan {@code true} bila pemeriksaan ini untuk alur PERSETUJUAN KRS oleh dosen
	 *                    wali (memakai kunci konfigurasi ambang batas yang berbeda dari
	 *                    pengambilan KRS oleh mahasiswa sendiri)
	 * @return {@code true} bila syarat pembayaran terpenuhi (atau tidak berlaku); {@code false}
	 *         bila belum terpenuhi
	 */
	public static Boolean checkPembayaranSebelumKRSSudahMemenuhi(Mahasiswa mahasiswa, Integer semester, Integer tahap,
			boolean persetujuan) {

		if (jenisKegiatansUntukKrs == null) {
			reloadJenisKegiatans();
		}

		if (Common.checkBaypassStatusPembayaranMahasiswa(semester, tahap, mahasiswa, jenisKegiatansUntukKrs)) {
			return true;
		}

		Integer tahunAngkatanMhs = mahasiswa.getTahunangkatan();
		Integer semesterMulai = mahasiswa.getPindahKeKampusIniMasukSemester();
		Integer tahunAkademikMulai = Common.getTahunAkademik(semester, tahunAngkatanMhs, semesterMulai,
				mahasiswa.getSemesterMulai());

		String tahunAkademik = tahunAkademikMulai + "/" + (tahunAkademikMulai + 1);
		if (Common.checkApakahMahasiswaBolehAmbilKrsLewatPengecualian(mahasiswa, tahunAkademik,
				semester % 2 == 0 ? Perkuliahan.GENAP : Perkuliahan.GANJIL)) {
			return true;
		}

		String batasTerendahPersen = Common.getKonfigurasi(
				persetujuan ? "batas_terendah_persen_pembayaran_boleh_persetujuan_krs"
						: "batas_terendah_persen_pembayaran_boleh_ambil_krs",
				"0", semester, mahasiswa.getTahunangkatan(), mahasiswa.getJurusan(), mahasiswa.getProgram(),
				mahasiswa.getStatusAwalMahasiswa()).getNilai();

		Double batas = 0.0;
		try {
			batas = Double.parseDouble(batasTerendahPersen.trim());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonHelperClass.java:931");

		}

		// System.out.println(
		// "Check Pembayaran KRS -> batasTerendahPersen = " +
		// batasTerendahPersen + "%, batas = " + batas);

		if (batas.intValue() == 0) {
			return true;
		}

		Session session = HibernateUtil.currentNativeSession();

		Double tagihanSyaratKrs = hitungTagihanMahasiswaSebagaiSyaratKrs(session, mahasiswa, semester);
		if (tagihanSyaratKrs < 0.01) {
			// session.disconnect();
			if (session.isOpen()) {
				session.disconnect();
				session.close();
			}
			HibernateUtil.closeSession();
			return true;
		}

		Double prosenYangSudahDibayar = 0.0;
		Boolean hasil = false;
		Kegiatan kegiatan = null;
		for (JenisKegiatan jenisKegiatan : jenisKegiatansUntukKrs) {
			kegiatan = mahasiswa.ambilKegiatans(semester, jenisKegiatan, true);

			prosenYangSudahDibayar = KegiatanPersistenceHelper.hitungPersentasePemenuhanTagihan(kegiatan);
			hasil = prosenYangSudahDibayar >= batas;

			// System.out.println("Check Pembayaran KRS -> Mahasiswa " +
			// mahasiswa + ", jenisKegiatan => " + jenisKegiatan
			// + ", kegiatan = " + kegiatan + ", semester " + semester + ",
			// batasTerendahPersen = "
			// + batasTerendahPersen + "%, prosenYangSudahDibayar = " +
			// prosenYangSudahDibayar + "%, hasil = "
			// + hasil + ", tagihanSyaratKrs = " + tagihanSyaratKrs);

			if (hasil) {
				break;
			}
		}

		if (!hasil) {

			boolean mahasiswabaruMengikutipersyaratanKrsSptMahasiswa = Common.bolehKonfigurasi("mahasiswa_baru_mengikuti_persyaratan_krs_spt_mahasiswa", Konfigurasi.TIDAK_AKTIF);

			if (!mahasiswabaruMengikutipersyaratanKrsSptMahasiswa && semester <= 1) {

				BiodataCalonMahasiswa calonMahasiswa = (BiodataCalonMahasiswa) ConstantValues
						.simpleObject(
								session.createCriteria(BiodataCalonMahasiswa.class)
										.add(Restrictions.or(Restrictions.isNull("aktif"),
												Restrictions.eq("aktif", true)))
										.add(Restrictions.eq("mahasiswa", mahasiswa)).setMaxResults(1),
								BiodataCalonMahasiswa.class);

				if (calonMahasiswa != null) {
					jenisKegiatansUntukKrs.add(ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU);
					kegiatan = calonMahasiswa.ambilKegiatans(semester,
							ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU, true);
					prosenYangSudahDibayar = KegiatanPersistenceHelper
							.hitungPersentasePemenuhanTagihan(kegiatan);
					hasil = prosenYangSudahDibayar >= batas;
				}
				// System.out.println("Check Pembayaran KRS -> calonMahasiswa "
				// + calonMahasiswa + ", semester " + semester
				// + ", batasTerendahPersen = " + batasTerendahPersen + "%,
				// prosenYangSudahDibayar = "
				// + prosenYangSudahDibayar + "%, hasil = " + hasil + ",
				// tagihanSyaratKrs = " + tagihanSyaratKrs);

			}

			if (!hasil) {
				String n = "";
				for (JenisKegiatan jenisKegiatan : jenisKegiatansUntukKrs) {
					n += n.isEmpty() ? jenisKegiatan.getNamaKegiatan() : ", atau " + jenisKegiatan.getNamaKegiatan();
				}

				try {
					MyMessageboxConfig.showFormat(
							"Mohon maaf, Bapak/Ibu. Mahasiswa dengan NIM {V1} belum dapat {V2} KRS, karena belum melakukan pembayaran pada{V3}. Jenis pembayaran yang harus diselesaikan antara lain {V4}. Langkah yang dapat dilakukan: (1) periksa kembali kewajiban pembayaran mahasiswa; (2) lakukan pembayaran sesuai jenis yang disyaratkan; (3) ulangi proses setelah pembayaran diselesaikan.",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION, mahasiswa.getNim(),
							(persetujuan ? "disetujui" : "mengambil"),
							((ConstantValues.aktifkanTahapanTerhubungKeKeuangan && tahap != null && tahap > 0)
									? " tahap " + tahap
									: " semester " + semester),
							n);
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonHelperClass.java:1022");

				}
			}

		}
		// session.disconnect();
		if (session.isOpen()) {
			session.disconnect();
			session.close();
		}
		HibernateUtil.closeSession();
		return hasil;
	}

	/** Seperti {@link #checkStatusPembayaranMahasiswaSebelumnya(Integer, Integer, Mahasiswa, boolean)} untuk alur pengambilan KRS oleh mahasiswa sendiri ({@code persetujuan=false}). */
	public static boolean checkStatusPembayaranMahasiswaSebelumnya(Integer semester, Integer tahap,
			Mahasiswa mahasiswa) {
		return checkStatusPembayaranMahasiswaSebelumnya(semester, tahap, mahasiswa, false);
	}

	/**
	 * Gerbang syarat "semester SEBELUMNYA harus lunas" sebelum mahasiswa boleh mengambil/disetujui
	 * KRS semester berjalan — hanya aktif bila konfigurasi
	 * {@code mahasiswa_harus_lunas_semester_sebelumnya_sebelum_[mengambil|persetujuan]_krs} bernilai
	 * AKTIF (default TIDAK AKTIF, sehingga method ini mengembalikan {@code true} langsung pada
	 * kebanyakan instalasi). Dilewati untuk mahasiswa baru (empat kondisi semester awal terkait
	 * {@code pindahKeKampusIniMasukSemester}) dan untuk tahap pertama saat
	 * {@code aktifkanTahapanTerhubungKeKeuangan} aktif. Bila aktif, memeriksa SEMESTER SEBELUMNYA
	 * ({@code semester - 1}): dilewati bila ada pengecualian jadwal KRS yang berlaku atau mahasiswa
	 * sedang cuti disetujui pada semester tersebut; bila total tagihan semester sebelumnya
	 * mendekati nol, otomatis lolos; jika tidak, persentase pemenuhan tagihan (atau, bila
	 * {@code tahap} diberikan, perhitungan berbasis cicilan bulanan per tahap) dibandingkan
	 * terhadap ambang batas ({@code batas_terendah_persen_pembayaran_semester_yang_lalu_boleh_*},
	 * default 90%).
	 *
	 * @param semester    nomor semester yang akan diambil KRS-nya (diperiksa adalah
	 *                    {@code semester - 1})
	 * @param tahap       nomor tahap (bila memakai skema tahapan terhubung keuangan), boleh
	 *                    {@code null} untuk memakai perhitungan berbasis semester penuh
	 * @param mahasiswa   mahasiswa yang diperiksa
	 * @param persetujuan {@code true} untuk alur persetujuan KRS oleh dosen wali (ambang batas
	 *                    konfigurasi berbeda)
	 * @return {@code true} bila syarat pelunasan semester sebelumnya terpenuhi atau tidak berlaku
	 */
	@SuppressWarnings("unchecked")
	public static boolean checkStatusPembayaranMahasiswaSebelumnya(Integer semester, Integer tahap, Mahasiswa mahasiswa,
			boolean persetujuan) {
		if (semester == null || semester.intValue() <= 1
				|| (semester.equals(1) || semester.equals(mahasiswa.getPindahKeKampusIniMasukSemester() + 1)
						|| semester.equals(mahasiswa.getPindahKeKampusIniMasukSemester() + 2)
						|| semester.equals(mahasiswa.getPindahKeKampusIniMasukSemester()))) {
			return true;
		}

		if (ConstantValues.aktifkanTahapanTerhubungKeKeuangan && tahap != null && tahap.equals(1)) {
			return true;
		}

		Konfigurasi konfigurasi = Common
				.getKonfigurasi(
						persetujuan ? "mahasiswa_harus_lunas_semester_sebelumnya_sebelum_persetujuan_krs"
								: "mahasiswa_harus_lunas_semester_sebelumnya_sebelum_mengambil_krs",
						Konfigurasi.TIDAK_AKTIF);

		if (konfigurasi.getNilai().equals(Konfigurasi.AKTIF)) {

			semester = semester - 1;
			if (ConstantValues.aktifkanTahapanTerhubungKeKeuangan && tahap != null && tahap > 1) {
				tahap = tahap - 1;
			}

			Integer tahunAngkatanMhs = mahasiswa.getTahunangkatan();
			Integer semesterMulai = mahasiswa.getPindahKeKampusIniMasukSemester();
			Integer tahunAkademikMulai = Common.getTahunAkademik(semester, tahunAngkatanMhs, semesterMulai,
					mahasiswa.getSemesterMulai());

			String tahunAkademik = tahunAkademikMulai + "/" + (tahunAkademikMulai + 1);
			if (Common.checkApakahMahasiswaBolehAmbilKrsLewatPengecualian(mahasiswa, tahunAkademik,
					semester % 2 == 0 ? Perkuliahan.GENAP : Perkuliahan.GANJIL)) {
				return true;
			}

			PendaftaranCutiMahasiswa pendaftaranCutiMahasiswa = mahasiswa.ambilCuti(semester, tahap, false);
			int countCuti = pendaftaranCutiMahasiswa != null && pendaftaranCutiMahasiswa.getPersetujuan() ? 1 : 0;

			if (countCuti > 0) {
				return true;
			}
			Session session = HibernateUtil.currentNativeSession();
			try {
			Double tagihanSyaratKrs = hitungTagihanMahasiswaSebagaiSyaratKrs(session, mahasiswa, semester);
			if (tagihanSyaratKrs < 0.01) {
				return true;
			}

			Double harusLunas = 90.0;
			try {
				harusLunas = Double.parseDouble(Common
						.getKonfigurasi(
								persetujuan ? "batas_terendah_persen_pembayaran_semester_yang_lalu_boleh_disetujui_krs"
										: "batas_terendah_persen_pembayaran_semester_yang_lalu_boleh_mengisi_krs",
								"90")
						.getNilai().trim().replace(',', '.'));
				if (harusLunas.doubleValue() < 0.0 || harusLunas.doubleValue() > 100.0) {
					harusLunas = 90.0;
				}
			} catch (Exception e) {
				// Nilai konfigurasi tidak valid: gunakan default historis tanpa membanjiri audit.
				harusLunas = 90.0;
			}

			if (jenisKegiatansUntukKrs == null) {
				reloadJenisKegiatans();
			}

			boolean hasil = true;
			if (tahap == null || tahap.equals(0)) {
				if (!Common.checkBaypassStatusPembayaranMahasiswa(semester, tahap, mahasiswa, jenisKegiatansUntukKrs)) {
					List<Kegiatan> kegiatanDibayars = mahasiswa.ambilKegiatans(semester, jenisKegiatansUntukKrs);
					for (Kegiatan kegiatanDibayar : kegiatanDibayars) {
						boolean lunas = KegiatanPersistenceHelper
								.hitungPersentasePemenuhanTagihan(kegiatanDibayar) >= harusLunas;
						System.out.println("kegiatanDibayar -> " + kegiatanDibayar + ", lunas " + lunas);
						hasil &= lunas;
					}
				}
			} else {

				if (!Common.checkBaypassStatusPembayaranMahasiswa(semester, tahap, mahasiswa, jenisKegiatansUntukKrs)) {

					PembayaranUtil.getInstance();
					Collection<DetailBiaya> detailBiayas = PembayaranUtilHelper.getDetailBiayaMahasiswa(
							mahasiswa, semester, ConstantValues.PENDAFTARAN_MAHASISWA_LAMA, null, true);

					PembayaranUtil.getInstance();
					int countPengaturanBulanan = PembayaranUtilHelper.countBulanan(session, mahasiswa,
							ConstantValues.PENDAFTARAN_MAHASISWA_LAMA, semester, detailBiayas, false, true);
					if (countPengaturanBulanan > 0) {
						Collection<PengaturanPembayaranBulanan> pengaturanPembayaranBulanans = PembayaranUtil
								.getInstance().getPengaturanPembayaranSemua(mahasiswa, session, semester,
										ConstantValues.PENDAFTARAN_MAHASISWA_LAMA, detailBiayas, false, true);
						Double tagihan = 0.0;
						for (PengaturanPembayaranBulanan pengaturanPembayaranBulanan : pengaturanPembayaranBulanans) {
							if (pengaturanPembayaranBulanan.hitungTahap(mahasiswa, semester).equals(tahap)
									|| pengaturanPembayaranBulanan.hitungTahap(mahasiswa, semester + 1).equals(tahap)) {
								tagihan += pengaturanPembayaranBulanan.ambilNominalModifikasi(mahasiswa, semester);
							}
						}

						Double sumCiciclan = mahasiswa.hitungTotalCicilanPembayaranPengecekanKrs(semester, tahap);

						Number pengurangan = 0.0;

						if (sumCiciclan == null) {
							sumCiciclan = 0.0;
						}

						Double prosenYangSudahDibayar = (sumCiciclan.doubleValue()
								/ (tagihan - Math.abs(pengurangan.doubleValue())));
						hasil = prosenYangSudahDibayar >= (harusLunas / 100.0);
						// System.out.println(
						// "mahasiswa " + mahasiswa + " semester " + semester +
						// " tahap " + tahap + ", tagihan = "
						// + tagihan + ", harusLunas = " + harusLunas + ",
						// prosenYangSudahDibayar "
						// + prosenYangSudahDibayar + ", sumCiciclan = " +
						// sumCiciclan);
					}
				}
			}

			return hasil;
			} finally {
				HibernateUtil.closeSession();
			}
		} else {
			return true;
		}

	}

	/**
	 * Gerbang syarat pembayaran untuk pendaftaran suatu {@link FormulirKegiatan} (mis. kegiatan
	 * kemahasiswaan/organisasi yang perlu formulir pendaftaran), memeriksa DUA syarat independen
	 * yang masing-masing dapat diaktifkan terpisah pada {@code formulirKegiatan}: (1)
	 * {@code getHarusBayarLunasSmtLalu()} — semester SEBELUMNYA harus lunas &ge;99%, dilewati bila
	 * mahasiswa sedang cuti disetujui pada semester tersebut; (2)
	 * {@code getHarusBayarLunasSmtSaatIni()} — semester BERJALAN (dihitung dari
	 * {@code formulirKegiatan.getSemester()}) harus lunas &ge;99% dengan aturan cuti yang sama.
	 * Kedua pemeriksaan memakai {@link #jenisKegiatansUntukKrs} sebagai acuan jenis tagihan dan
	 * menghormati baypass ({@code Common.checkBaypassStatusPembayaranMahasiswa}). Bila salah satu
	 * syarat gagal, dialog informasi ditampilkan langsung ke pengguna dan pemeriksaan syarat kedua
	 * TIDAK dijalankan (short-circuit begitu syarat pertama gagal).
	 *
	 * @param formulirKegiatan formulir kegiatan yang berisi saklar kedua syarat pelunasan di atas
	 * @param mahasiswa        mahasiswa pendaftar
	 * @return {@code true} bila kedua syarat yang aktif terpenuhi (atau tidak diaktifkan pada
	 *         {@code formulirKegiatan})
	 */
	public static boolean checkStatusPembayaranKegiatanMahasiswa(FormulirKegiatan formulirKegiatan,
			Mahasiswa mahasiswa) {
		// Integer semester = mahasiswa.currentSemester();

		String jenisSemester = formulirKegiatan.getSemester();
		Integer semester = Common.getSemester(mahasiswa.getTahunangkatan(), jenisSemester,
				mahasiswa.getPindahKeKampusIniMasukSemester(), mahasiswa.getSemesterMulai());

		Integer tahap = null;
		boolean hasil = true;

		if (formulirKegiatan.getHarusBayarLunasSmtLalu()) {

			Double harusLunas = 99.0;

			semester = semester - 1;

			if (semester < 1) {
				return true;
			}

			// Session session = HibernateUtil.currentNativeSession();

			if (jenisKegiatansUntukKrs == null) {
				reloadJenisKegiatans();
			}

			PendaftaranCutiMahasiswa pendaftaranCutiMahasiswa = mahasiswa.ambilCuti(semester, tahap, false);
			int countCuti = pendaftaranCutiMahasiswa != null && pendaftaranCutiMahasiswa.getPersetujuan() ? 1 : 0;

			if (countCuti == 0) {

				// if (tahap == null || tahap.equals(0)) {
				if (!Common.checkBaypassStatusPembayaranMahasiswa(semester, tahap, mahasiswa, jenisKegiatansUntukKrs)) {
					List<Kegiatan> kegiatanDibayars = mahasiswa.ambilKegiatans(semester, jenisKegiatansUntukKrs);
					for (Kegiatan kegiatanDibayar : kegiatanDibayars) {
						hasil &= KegiatanPersistenceHelper
								.hitungPersentasePemenuhanTagihan(kegiatanDibayar) >= harusLunas;
					}
				}

				if (!hasil) {
					try {
						MyMessageboxConfig.showFormat(
								"Mohon maaf, Bapak/Ibu. Mahasiswa dengan NIM {V1} atas nama {V2} harus melunasi biaya semester {V3} terlebih dahulu. Langkah yang dapat dilakukan: (1) periksa tagihan pembayaran mahasiswa yang bersangkutan; (2) lakukan pelunasan biaya semester yang masih tertunggak; (3) ulangi proses setelah pembayaran diselesaikan.",
								"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, mahasiswa.getNim(),
								mahasiswa.getNama(), semester);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						Common.tampilErrorJikaAdmin(e);
					}
				}

			}

			// HibernateUtil.closeSession();

		}

		if (!hasil) {
			return hasil;
		}

		semester = Common.getSemester(mahasiswa.getTahunangkatan(), jenisSemester,
				mahasiswa.getPindahKeKampusIniMasukSemester(), mahasiswa.getSemesterMulai());
		if (formulirKegiatan.getHarusBayarLunasSmtSaatIni()) {

			if (semester < 1) {
				return true;
			}

			Double harusLunas = 99.0;

			// Session session = HibernateUtil.currentNativeSession();

			if (jenisKegiatansUntukKrs == null) {
				reloadJenisKegiatans();
			}

			PendaftaranCutiMahasiswa pendaftaranCutiMahasiswa = mahasiswa.ambilCuti(semester, tahap, false);
			int countCuti = pendaftaranCutiMahasiswa != null && pendaftaranCutiMahasiswa.getPersetujuan() ? 1 : 0;

			if (countCuti == 0) {

				// if (tahap == null || tahap.equals(0)) {
				if (!Common.checkBaypassStatusPembayaranMahasiswa(semester, tahap, mahasiswa, jenisKegiatansUntukKrs)) {
					List<Kegiatan> kegiatanDibayars = mahasiswa.ambilKegiatans(semester, jenisKegiatansUntukKrs);
					for (Kegiatan kegiatanDibayar : kegiatanDibayars) {
						hasil &= KegiatanPersistenceHelper
								.hitungPersentasePemenuhanTagihan(kegiatanDibayar) >= harusLunas;
					}
				}

				if (!hasil) {
					try {
						MyMessageboxConfig.showFormat(
								"Mohon maaf, Bapak/Ibu. Mahasiswa dengan NIM {V1} atas nama {V2} harus melunasi biaya semester {V3} terlebih dahulu. Langkah yang dapat dilakukan: (1) periksa tagihan pembayaran mahasiswa yang bersangkutan; (2) lakukan pelunasan biaya semester yang masih tertunggak; (3) ulangi proses setelah pembayaran diselesaikan.",
								"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, mahasiswa.getNim(),
								mahasiswa.getNama(), semester);
					} catch (Exception e) {

						Common.tampilErrorJikaAdmin(e);
					}
				}

			}

			// HibernateUtil.closeSession();

		}

		return hasil;

	}

	/**
	 * Gerbang syarat pembayaran sebelum mahasiswa/dosen boleh MELIHAT NILAI semester sebelumnya —
	 * hanya aktif bila konfigurasi
	 * {@code mahasiswa_harus_lunas_semester_sebelumnya_sebelum_melihat_nilai} AKTIF. Dilewati untuk
	 * semester {@code <= 0}, serta (bila {@code termasukSmt1=true}) untuk kondisi semester awal
	 * terkait {@code pindahKeKampusIniMasukSemester}. Bila aktif, memeriksa SEMESTER SEBELUMNYA:
	 * dilewati bila mahasiswa sedang cuti disetujui pada semester tersebut, atau bila baypass
	 * berlaku, atau bila tidak ada {@link Kegiatan} tertagih sama sekali pada
	 * {@link #jenisKegiatansUntukNilai} (dianggap lolos); jika ada, SELURUH kegiatan tertagih harus
	 * memenuhi ambang batas {@code harusLunas} yang diberikan pemanggil (bukan dibaca dari
	 * konfigurasi tersendiri seperti method sejenis lainnya).
	 *
	 * @param semester      nomor semester yang nilainya ingin dilihat (diperiksa adalah
	 *                      {@code semester - 1})
	 * @param tahap         nomor tahap (dikurangi 1 bila skema tahapan terhubung keuangan aktif)
	 * @param mahasiswa     mahasiswa yang diperiksa
	 * @param harusLunas    ambang batas persentase pelunasan (0-100) yang harus dipenuhi setiap
	 *                      kegiatan tertagih
	 * @param termasukSmt1  {@code true} untuk ikut menerapkan pengecualian semester awal
	 *                      (mahasiswa baru/pindahan) di samping pengecualian {@code semester <= 0}
	 * @return {@code true} bila syarat terpenuhi, tidak berlaku, atau tidak ada tagihan
	 */
	public static boolean checkStatusPembayaranMahasiswaSebelumnyaUntukPenilaian(Integer semester, Integer tahap,
			Mahasiswa mahasiswa, Double harusLunas, boolean termasukSmt1) {
		if (semester == null || semester.intValue() <= 0
				|| (termasukSmt1
						&& (semester.equals(1) || semester.equals(mahasiswa.getPindahKeKampusIniMasukSemester() + 1)
								|| semester.equals(mahasiswa.getPindahKeKampusIniMasukSemester() + 2)
								|| semester.equals(mahasiswa.getPindahKeKampusIniMasukSemester())))) {
			return true;
		}
		Konfigurasi konfigurasi = Common.getKonfigurasi(
				"mahasiswa_harus_lunas_semester_sebelumnya_sebelum_melihat_nilai", Konfigurasi.TIDAK_AKTIF);

		if (konfigurasi.getNilai().equals(Konfigurasi.AKTIF)) {

			semester = semester - 1;
			if (ConstantValues.aktifkanTahapanTerhubungKeKeuangan && tahap != null && tahap > 1) {
				tahap = tahap - 1;
			}

			PendaftaranCutiMahasiswa pendaftaranCutiMahasiswa = mahasiswa.ambilCuti(semester, tahap, false);
			int countCuti = pendaftaranCutiMahasiswa != null && pendaftaranCutiMahasiswa.getPersetujuan() ? 1 : 0;
			if (countCuti > 0) {
				return true;
			}

			if (jenisKegiatansUntukNilai == null) {
				reloadJenisKegiatans();
			}

			boolean hasil = true;

			if (!Common.checkBaypassStatusPembayaranMahasiswa(semester, tahap, mahasiswa, jenisKegiatansUntukNilai)) {
				List<Kegiatan> kegiatanDibayars = mahasiswa.ambilKegiatans(semester, jenisKegiatansUntukNilai);

				if (kegiatanDibayars.isEmpty()) {
					hasil = true;
				} else {
					for (Kegiatan kegiatanDibayar : kegiatanDibayars) {
						if (kegiatanDibayar != null) {
							double persenPemenuhan = KegiatanPersistenceHelper
									.hitungPersentasePemenuhanTagihan(kegiatanDibayar);
							hasil &= persenPemenuhan >= harusLunas;

							System.out.println("mahasiswa " + mahasiswa + " semester " + semester + " tahap " + tahap
									+ ", kegiatanDibayar = " + kegiatanDibayar + ", harusLunas = " + harusLunas
									+ ", prosenYangSudahDibayar " + persenPemenuhan + ", hasil "
									+ hasil);
						}
					}
				}

			}

			return hasil;
		} else {
			return true;
		}

	}

	/**
	 * Gerbang syarat pembayaran sebelum mahasiswa boleh mengajukan PROPOSAL SKRIPSI, hanya
	 * diberlakukan bila {@code formatNilaiProposalSkripsi.getHarusLunas()} bernilai {@code true}.
	 * Dilewati untuk kondisi semester awal (mahasiswa baru/pindahan). Ambang batas persentase
	 * pelunasan diambil dari {@code formatNilaiProposalSkripsi.getProsentaseLunas()} (per format
	 * penilaian, bukan konfigurasi global), diperiksa terhadap seluruh {@link Kegiatan} tertagih
   * pada {@link #jenisKegiatansUntukKrs} (menghormati baypass bila berlaku).
	 *
	 * @param formatNilaiProposalSkripsi format nilai yang menentukan aktif/tidaknya syarat lunas
	 *                                   dan ambang batas persentasenya
	 * @param semester                   nomor semester berjalan mahasiswa
	 * @param mahasiswa                  mahasiswa pengaju
	 * @return {@code true} bila syarat lunas tidak diaktifkan, tidak berlaku (semester awal), atau
	 *         seluruh kegiatan tertagih sudah memenuhi ambang batas
	 */
	public static boolean checkStatusPembayaranMahasiswaPengajuanSkripsi(
			FormatNilaiProposalSkripsi formatNilaiProposalSkripsi, Integer semester, Mahasiswa mahasiswa) {
		if (semester == null || semester.intValue() <= 1
				|| (semester.equals(1) || semester.equals(mahasiswa.getPindahKeKampusIniMasukSemester() + 1)
						|| semester.equals(mahasiswa.getPindahKeKampusIniMasukSemester() + 2)
						|| semester.equals(mahasiswa.getPindahKeKampusIniMasukSemester()))) {
			return true;
		}

		if (formatNilaiProposalSkripsi.getHarusLunas()) {

			Double harusLunas = formatNilaiProposalSkripsi.getProsentaseLunas();

			boolean hasil = true;

			if (jenisKegiatansUntukKrs == null) {
				reloadJenisKegiatans();
			}
			if (!Common.checkBaypassStatusPembayaranMahasiswa(semester, null, mahasiswa, jenisKegiatansUntukKrs)) {
				List<Kegiatan> kegiatanDibayars = mahasiswa.ambilKegiatans(semester, jenisKegiatansUntukKrs);
				for (Kegiatan kegiatanDibayar : kegiatanDibayars) {
				hasil &= KegiatanPersistenceHelper.hitungPersentasePemenuhanTagihan(kegiatanDibayar) >= harusLunas;
				}
			}

			return hasil;
		} else {
			return true;
		}

	}

	/**
	 * Padanan {@link #checkStatusPembayaranMahasiswaPengajuanSkripsi} untuk pengajuan SIDANG
	 * SKRIPSI: syarat lunas diambil dari {@code formatNilaiSkripsi.getHarusLunas()}/
	 * {@code getProsentaseLunas()} (bukan {@code formatNilaiProposalSkripsi}), logika pemeriksaan
	 * lainnya identik (dilewati untuk semester awal, diperiksa terhadap
	 * {@link #jenisKegiatansUntukKrs}, menghormati baypass).
	 *
	 * @param formatNilaiSkripsi format nilai skripsi yang menentukan aktif/tidaknya syarat lunas
	 *                           dan ambang batas persentasenya
	 * @param semester           nomor semester berjalan mahasiswa
	 * @param mahasiswa          mahasiswa pengaju
	 * @return {@code true} bila syarat lunas tidak diaktifkan, tidak berlaku, atau terpenuhi
	 */
	public static boolean checkStatusPembayaranMahasiswaPengajuanSidang(FormatNilaiSkripsi formatNilaiSkripsi,
			Integer semester, Mahasiswa mahasiswa) {
		if (semester == null || semester.intValue() <= 1
				|| (semester.equals(1) || semester.equals(mahasiswa.getPindahKeKampusIniMasukSemester() + 1)
						|| semester.equals(mahasiswa.getPindahKeKampusIniMasukSemester() + 2)
						|| semester.equals(mahasiswa.getPindahKeKampusIniMasukSemester()))) {
			return true;
		}

		if (formatNilaiSkripsi.getHarusLunas()) {

			Double harusLunas = formatNilaiSkripsi.getProsentaseLunas();

			boolean hasil = true;

			if (jenisKegiatansUntukKrs == null) {
				reloadJenisKegiatans();
			}
			if (!Common.checkBaypassStatusPembayaranMahasiswa(semester, null, mahasiswa, jenisKegiatansUntukKrs)) {
				List<Kegiatan> kegiatanDibayars = mahasiswa.ambilKegiatans(semester, jenisKegiatansUntukKrs);
				for (Kegiatan kegiatanDibayar : kegiatanDibayars) {
				hasil &= KegiatanPersistenceHelper.hitungPersentasePemenuhanTagihan(kegiatanDibayar) >= harusLunas;
				}
			}

			return hasil;
		} else {
			return true;
		}

	}

	/**
	 * Gerbang syarat pembayaran sebelum mahasiswa boleh mendaftar WISUDA, hanya diberlakukan bila
	 * konfigurasi {@code mahasiswa_harus_lunas_sebelum_wisuda} AKTIF (default TIDAK AKTIF).
	 * Dilewati untuk kondisi semester awal (mahasiswa baru/pindahan). Ambang batas persentase
	 * pelunasan diambil dari konfigurasi {@code batas_terendah_persen_pembayaran_sebelum_wisuda}
	 * (default 90%), diperiksa terhadap seluruh {@link Kegiatan} tertagih pada
	 * {@link #jenisKegiatansUntukKrs} (menghormati baypass bila berlaku).
	 *
	 * @param semester  nomor semester berjalan mahasiswa
	 * @param mahasiswa mahasiswa pendaftar wisuda
	 * @return {@code true} bila syarat lunas tidak diaktifkan, tidak berlaku, atau terpenuhi
	 */
	public static boolean checkStatusPembayaranMahasiswaPengajuanWisuda(Integer semester, Mahasiswa mahasiswa) {
		if (semester == null || semester.intValue() <= 1
				|| (semester.equals(1) || semester.equals(mahasiswa.getPindahKeKampusIniMasukSemester() + 1)
						|| semester.equals(mahasiswa.getPindahKeKampusIniMasukSemester() + 2)
						|| semester.equals(mahasiswa.getPindahKeKampusIniMasukSemester()))) {
			return true;
		}
		Konfigurasi konfigurasi = Common.getKonfigurasi("mahasiswa_harus_lunas_sebelum_wisuda",
				Konfigurasi.TIDAK_AKTIF);

		if (konfigurasi.getNilai().equals(Konfigurasi.AKTIF)) {

			Double harusLunas = 90.0;
			try {
				harusLunas = Double.parseDouble(Common
						.getKonfigurasi("batas_terendah_persen_pembayaran_sebelum_wisuda", "90").getNilai().trim());
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonHelperClass.java:1424");

			}

			boolean hasil = true;
			if (jenisKegiatansUntukKrs == null) {
				reloadJenisKegiatans();
			}
			if (!Common.checkBaypassStatusPembayaranMahasiswa(semester, null, mahasiswa, jenisKegiatansUntukKrs)) {
				List<Kegiatan> kegiatanDibayars = mahasiswa.ambilKegiatans(semester, jenisKegiatansUntukKrs);
				for (Kegiatan kegiatanDibayar : kegiatanDibayars) {
				hasil &= KegiatanPersistenceHelper.hitungPersentasePemenuhanTagihan(kegiatanDibayar) >= harusLunas;
				}
			}

			return hasil;
		} else {
			return true;
		}

	}

}
