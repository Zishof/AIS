package ais.action.master.helper;

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.poi.xssf.usermodel.XSSFCell;
import org.zkoss.poi.xssf.usermodel.XSSFCellStyle;
import org.zkoss.poi.xssf.usermodel.XSSFColor;
import org.zkoss.poi.xssf.usermodel.XSSFRow;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zss.ui.Spreadsheet;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Toolbar;

import ais.action.ws.util.PembayaranUtil;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.CicilanPembayaran;
import ais.database.model.DetailBiaya;
import ais.database.model.DetailKegiatan;
import ais.database.model.Fakultas;
import ais.database.model.GeneralValueObject;
import ais.database.model.HistoryStatusMahasiswa;
import ais.database.model.ItemBiaya;
import ais.database.model.JadwalPembayaran;
import ais.database.model.JenisKegiatan;
import ais.database.model.Jurusan;
import ais.database.model.Kegiatan;
import ais.database.model.Konfigurasi;
import ais.database.model.Mahasiswa;
import ais.database.model.PengaturanPembayaranBulanan;
import ais.database.model.Perkuliahan;
import ais.database.model.SettingBiaya;
import ais.database.model.SettingBiayaDetail;
import ais.database.model.StatusMahasiswa;
import ais.database.model.Tbmuser;
import ais.ui.util.DataCriteria;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Utilitas statis inti untuk domain {@link Kegiatan} (tagihan/aktivitas berbayar mahasiswa &amp;
 * calon mahasiswa) di AIS: membuat/menyinkronkan baris {@code Kegiatan} beserta rincian
 * {@link DetailKegiatan}-nya sesuai {@link JadwalPembayaran} dan {@link SettingBiaya}/
 * {@link DetailBiaya}/{@link PengaturanPembayaranBulanan} (tagihan bulanan), dan menyediakan
 * fungsi ekspor/impor Excel massal untuk tagihan.
 *
 * <p><b>Dua sisi mahasiswa.</b> Hampir setiap operasi punya sepasang varian: satu untuk
 * {@link Mahasiswa} (mahasiswa aktif) dan satu untuk {@link BiodataCalonMahasiswa} (calon
 * mahasiswa, sebelum her-registrasi) — mis. {@link #checkKegiatanMahasiswa} vs.
 * {@link #checkKegiatanCalonMahasiswa}, {@link #dataTagihanMahasiswa} vs.
 * {@link #dataTagihanCalonMahasiswa}. Keduanya secara struktural mirip (kode nyaris duplikat)
 * karena field entity sumbernya berbeda (Mahasiswa vs. BiodataCalonMahasiswa punya API yang
 * berbeda meski konsep serupa) — bukan bug, tapi keterbatasan model data yang belum disatukan.</p>
 *
 * <p><b>Ketahanan transaksi PostgreSQL ({@code *Safe} methods).</b> Sebagian besar kompleksitas
 * kelas ini bukan logika bisnis, melainkan lapisan ketahanan terhadap kontensi database saat
 * banyak proses (interaktif + batch "Proses Tagihan"/{@code KegiatanProsesHeper}) menyentuh baris
 * {@code Kegiatan}/{@code DetailKegiatan} yang sama nyaris bersamaan: lock timeout (55P03),
 * statement timeout (57014), deadlock (40P01), transaksi ter-abort (25P02), constraint violation
 * (23505/23503), dan koneksi c3p0 basi (kelas SQLState 08). {@link #saveEntitySafe} dan
 * {@link #updateEntitySafe} mendeteksi pola ini ({@link #isLockTimeout}/{@link #isTransactionAborted}/
 * {@link #isConstraintViolation}/{@link #isStaleState}), memulihkan transaksi pemanggil yang sudah
 * mati ({@link #pulihkanTransaksiTerabort}), dan retry di sesi terisolasi baru
 * ({@link #openIsolatedSession}) dengan backoff+jitter — lihat Javadoc masing-masing method untuk
 * riwayat insiden produksi (diberi kode "KE-nn") yang melatarbelakangi tiap cabang penanganan.</p>
 *
 * <p><b>Bukan tanggung jawab kelas ini:</b> mengubah {@link HistoryStatusMahasiswa} secara
 * langsung (didelegasikan ke {@code AuditListener}/{@link HistoryStatusMahasiswaUtil} setelah
 * commit — komentar di {@link #checkKegiatanMahasiswa} menegaskan ini eksplisit), maupun logika
 * pembayaran/cicilan itu sendiri (di {@code PembayaranUtil}/{@code PembayaranUtilHelper}/
 * {@code KegiatanPersistenceHelper}). Pemanggil baru sebaiknya memakai method publik yang sudah
 * ada (mis. {@link #checkKegiatanMahasiswa}), bukan menyalin query/transaksi manual ke Action lain
 * — supaya penanganan lock timeout dan aturan tagihan tetap satu sumber kebenaran.</p>
 */
public class KegiatanHelper {
	/**
	 * Flag global (statis, dibagi seluruh JVM/semua sesi — bukan per-request) yang dimaksudkan
	 * sebagai penanda "sedang berlangsung proses tagihan massal" (mis. dari
	 * {@code KegiatanProsesHeper}/{@code TagihanProcessor}, untuk digerbangi
	 * {@code KegiatanAction}). <b>Kuirk saat ini: SEMUA titik baca/tulis field ini di codebase
	 * (di {@code KegiatanProsesHeper}, {@code KegiatanAction}, {@code TagihanProcessor}) sudah
	 * DIKOMENTARI/dinonaktifkan</b> — field ini efektif TIDAK PERNAH diset {@code true} oleh kode
	 * aktif manapun saat ini, hanya dideklarasikan dan selalu bernilai default {@code false}.
	 * Dipertahankan apa adanya (bukan dihapus — di luar lingkup tugas Javadoc ini) sebagai
	 * dokumentasi kondisi nyata, bukan asumsi bahwa flag ini masih berfungsi.
	 */
	public static boolean prosestagihan = false;

	// ===================================================================================
	// TRANSACTION & SESSION SAFE UTILITIES (MENCEGAH ASSERTION FAILURE & MEMORY
	// LEAK)
	// ===================================================================================


	/** {@code true} bila {@code session} non-null dan {@code isOpen()} tidak melempar/mengembalikan false — dipakai sebagai penjaga sebelum operasi Hibernate lain agar tidak melempar {@code SessionException} mentah. */
	private static boolean isUsableSession(Session session) {
		try {
			return session != null && session.isOpen();
		} catch (Exception e) {
			return false;
		}
	}

	/** Membuka session Hibernate BARU yang independen dari session request/pemanggil manapun — dipakai di seluruh kelas ini untuk operasi "isolasi" (retry, cek keberadaan baris, recovery transaksi ter-abort) yang sengaja tidak boleh ikut tercemar state session lain. Pemanggil bertanggung jawab menutupnya (lihat {@link #closeOpenedSessionQuietly}/{@link #closeLocalSessionSafely}). */
	private static Session openIsolatedSession() {
		return HibernateUtil.getSessionFactory().openSession();
	}

	/**
	 * Konfigurasi pembayaran bulanan hanya menjadi referensi saat tagihan dihitung ulang.
	 * Beberapa getter model tersebut menghitung ulang kolom turunannya (real bulan/nama bulan),
	 * sehingga Hibernate dapat menganggap konfigurasi ikut berubah ketika sesi di-flush. Akibatnya
	 * Envers mencoba menulis PengaturanPembayaranBulanan__aud walaupun pengguna tidak mengubah
	 * konfigurasi, dan seluruh transaksi hitung ulang gagal bila skema audit tenant tertinggal.
	 *
	 * Tandai instance yang dikelola sesi sebagai read-only. DetailKegiatan, CicilanPembayaran, dan
	 * Kegiatan tetap writable; hanya master konfigurasi yang dilindungi dari dirty-check semu.
	 */
	private static void tandaiPengaturanBulananReadOnly(Session session,
			PengaturanPembayaranBulanan pengaturanPembayaranBulanan) {
		if (!isUsableSession(session) || pengaturanPembayaranBulanan == null) {
			return;
		}
		try {
			PengaturanPembayaranBulanan managed = pengaturanPembayaranBulanan;
			if (!session.contains(managed) && managed.getId() != null) {
				managed = (PengaturanPembayaranBulanan) session.get(PengaturanPembayaranBulanan.class,
						managed.getId());
			}
			if (managed != null && session.contains(managed)) {
				session.setReadOnly(managed, true);
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e,
					"KegiatanHelper:tandai-pengaturan-pembayaran-bulanan-read-only");
		}
	}

	/**
	 * Menyapu dua koleksi kerja hitung-ulang tagihan dan menandai setiap
	 * {@link PengaturanPembayaranBulanan} yang ditemukan sebagai read-only lewat
	 * {@link #tandaiPengaturanBulananReadOnly}, sehingga master konfigurasi pembayaran bulanan
	 * tidak ikut ter-dirty-check saat sesi di-flush.
	 *
	 * <p>Ini adalah penerapan massal dari pelindung yang dijelaskan pada
	 * {@link #tandaiPengaturanBulananReadOnly}: getter turunan pada
	 * {@code PengaturanPembayaranBulanan} (real bulan/nama bulan) menulis balik ke field-nya
	 * sendiri saat dibaca — instance dari pola getter-yang-memutasi-field yang tersebar di
	 * {@code ais/database/model/}. Konsekuensinya Hibernate menganggap master konfigurasi ikut
	 * berubah dan Envers menulis revisi audit palsu; bila skema {@code __aud} tenant tertinggal,
	 * seluruh transaksi hitung ulang gagal. Method ini dipanggil di awal jalur hitung ulang
	 * supaya seluruh konfigurasi yang akan tersentuh sudah read-only sebelum flush pertama.</p>
	 *
	 * <p>Dua koleksi diperlakukan berbeda karena bentuk datanya berbeda: {@code detailKegiatans}
	 * berisi {@link DetailKegiatan} sehingga konfigurasi diambil lewat
	 * {@code getPengaturanPembayaranBulanan()}, sedangkan {@code detailBiayas} dapat berisi
	 * {@link PengaturanPembayaranBulanan} secara langsung. Keduanya disaring dengan
	 * {@code instanceof} karena parameter sengaja bertipe {@link Collection} mentah (raw) —
	 * pemanggil mengirim koleksi hasil query yang tipenya tidak seragam. Elemen bertipe lain
	 * diabaikan diam-diam, dan {@code null} pada salah satu koleksi bukan error.</p>
	 *
	 * @param session         session Hibernate yang mengelola instance; boleh tidak usable/{@code null} (pelindung akan no-op)
	 * @param detailKegiatans koleksi {@link DetailKegiatan} yang akan dihitung ulang; boleh {@code null}
	 * @param detailBiayas    koleksi yang boleh memuat {@link PengaturanPembayaranBulanan} langsung; boleh {@code null}
	 */
	@SuppressWarnings("rawtypes")
	private static void lindungiKonfigurasiBulananSaatHitungUlang(Session session, Collection detailKegiatans,
			Collection detailBiayas) {
		if (detailKegiatans != null) {
			for (Object value : detailKegiatans) {
				if (value instanceof DetailKegiatan) {
					tandaiPengaturanBulananReadOnly(session,
							((DetailKegiatan) value).getPengaturanPembayaranBulanan());
				}
			}
		}
		if (detailBiayas != null) {
			for (Object value : detailBiayas) {
				if (value instanceof PengaturanPembayaranBulanan) {
					tandaiPengaturanBulananReadOnly(session, (PengaturanPembayaranBulanan) value);
				}
			}
		}
	}

	/**
	 * Menerapkan {@code SET LOCAL lock_timeout = '5000ms'} pada transaksi PostgreSQL yang sedang
	 * berjalan, agar UPDATE yang menunggu baris {@code kegiatan}/{@code detail_kegiatan} terkunci
	 * transaksi lain GAGAL CEPAT (SQLState 55P03) alih-alih menggantung sampai
	 * {@code statement_timeout} memicu 57014.
	 *
	 * <p><b>Kenapa ini penting.</b> Tanpa {@code lock_timeout}, request interaktif yang bertabrakan
	 * dengan batch "Proses Tagihan" akan menahan koneksi c3p0 selama seluruh durasi
	 * {@code statement_timeout}; error yang muncul pun berbentuk "canceling statement due to
	 * statement timeout ... while updating tuple ... in relation kegiatan", yang secara SQLState
	 * tidak dapat dibedakan dari timeout query biasa sehingga retry menjadi sia-sia. Dengan
	 * gagal-cepat 55P03, {@link #isLockTimeout} dapat mengenalinya dan {@link #updateEntitySafe}
	 * melakukan retry di sesi terisolasi bersih ({@link #openIsolatedSession}) dengan
	 * backoff+jitter — koneksi pun cepat dibebaskan.</p>
	 *
	 * <p><b>FlushMode sementara MANUAL.</b> {@code SET LOCAL} tidak menyentuh state entity apa pun,
	 * tetapi Hibernate akan melakukan auto-flush seluruh persistence context sebelum menjalankan
	 * SQL query native. Pada jalur ini persistence context pemanggil sering memuat proxy lazy dari
	 * session lama, sehingga auto-flush prematur justru memicu error yang hendak dicegah. Karena
	 * itu {@link FlushMode} disimpan, diganti {@link FlushMode#MANUAL} selama statement, lalu
	 * <i>selalu</i> dipulihkan di blok {@code finally} (termasuk bila statement gagal). Flush yang
	 * memang dibutuhkan tetap dilakukan eksplisit oleh {@link #saveEntitySafe}/
	 * {@link #updateEntitySafe} setelah entity target disimpan.</p>
	 *
	 * <p><b>Best-effort.</b> Seluruh kegagalan ditelan: {@code SET LOCAL} tidak dikenal di luar
	 * PostgreSQL, dan tanpa transaksi aktif statement ini tidak berefek. Kegagalan di sini bukan
	 * error aplikasi — jalur pemanggil tetap berjalan, hanya kehilangan properti gagal-cepat.
	 * Pemulihan FlushMode juga dilindungi {@code try/catch} sendiri; session yang sudah rusak
	 * akan ditangani jalur retry {@link #updateEntitySafe}.</p>
	 *
	 * @param session session Hibernate pemanggil yang transaksinya akan diberi batas tunggu lock
	 */
	private static void terapkanLockTimeout(Session session) {
		// Batasi WAKTU TUNGGU LOCK (bukan durasi query) pada transaksi berjalan. Tanpa ini, UPDATE
		// yang menunggu baris "kegiatan" terkunci transaksi lain akan MENGGANTUNG sampai
		// statement_timeout memicu "canceling statement due to statement timeout ... while updating
		// tuple ... in relation kegiatan" (57014) — request lama & koneksi c3p0 tertahan. Dengan
		// lock_timeout, tunggu lock dibatasi lalu GAGAL CEPAT (55P03) sehingga retry di sesi bersih
		// (lihat updateEntitySafe) menjadi efektif & koneksi cepat bebas. SET LOCAL hanya berlaku
		// untuk transaksi ini. Best-effort (diam bila bukan PostgreSQL / tanpa transaksi aktif).
		FlushMode flushModeSebelumnya = null;
		try {
			/*
			 * SET LOCAL tidak membaca state entity. Jangan biarkan Hibernate melakukan
			 * auto-flush seluruh persistence context sebelum statement konfigurasi ini;
			 * context pemanggil dapat memuat proxy lazy dari session lama. Flush yang
			 * memang diperlukan tetap dijalankan eksplisit sesudah entity target disimpan.
			 */
			flushModeSebelumnya = session.getFlushMode();
			session.setFlushMode(FlushMode.MANUAL);
			session.createSQLQuery("SET LOCAL lock_timeout = '5000ms'").executeUpdate();
		} catch (Exception ignore) {
			// Best-effort saja. Jika koneksi sudah ditutup atau database bukan PostgreSQL,
			// jangan jadikan SET LOCAL lock_timeout sebagai error aplikasi.
		} finally {
			if (flushModeSebelumnya != null && session != null && session.isOpen()) {
				try {
					session.setFlushMode(flushModeSebelumnya);
				} catch (Exception ignore) {
					// Session yang rusak akan ditangani jalur retry updateEntitySafe.
				}
			}
		}
	}

	/** Penutupan session tiga-langkah ({@code clear}/{@code disconnect}/{@code close}) yang menelan exception di tiap langkah — dipakai untuk menutup session yang DIBUKA method ini sendiri (bukan session pemanggil). Fungsinya identik dengan {@link #closeLocalSessionSafely}; dua method paralel ini adalah duplikasi historis, dipertahankan apa adanya (bukan lingkup tugas Javadoc untuk konsolidasi). */
	private static void closeOpenedSessionQuietly(Session session) {
		if (session != null && session.isOpen()) {
			try {
				session.clear();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/KegiatanHelper.java:126");
			}
			try {
				session.disconnect();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/KegiatanHelper.java:130");
			}
			try {
				session.close();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/KegiatanHelper.java:134");
			}
		}
	}

	/**
	 * {@code session.save(entity)} + {@code flush()} yang tahan kontensi: bila {@code session}
	 * tidak usable, membuka &amp; menutup session terisolasi sendiri; bila transaksi pemanggil
	 * belum aktif, membuka &amp; commit transaksi baru di sini (transaksi milik pemanggil
	 * — bila sudah aktif — dibiarkan apa adanya, TIDAK di-commit oleh method ini). Saat gagal:
	 * transaksi baru (jika dibuka di sini) di-rollback; bila memakai transaksi pemanggil dan
	 * penyebabnya lock timeout/transaksi-ter-abort/constraint-violation (KE-19: satu statement
	 * gagal meng-ABORT seluruh transaksi PostgreSQL), transaksi pemanggil dipulihkan lewat
	 * {@link #pulihkanTransaksiTerabort} agar pemanggil (mis. {@code checkKegiatanCalonMahasiswa}
	 * yang langsung query ulang di sesi yang sama) tidak jatuh ke "current transaction is
	 * aborted". Entity di-{@code clear()}/{@code evict()} dari session sebelum exception
	 * dilempar ulang ke pemanggil.
	 *
	 * @param session session Hibernate pemanggil, atau {@code null}/tidak usable untuk memakai session terisolasi baru
	 * @param entity  entity baru yang akan disimpan
	 * @throws Exception exception asli dari {@code save}/{@code flush} setelah upaya pemulihan di atas
	 */
	private static void saveEntitySafe(Session session, Object entity) throws Exception {
		boolean closeLocalSession = false;
		if (!isUsableSession(session)) {
			session = openIsolatedSession();
			closeLocalSession = true;
		}
		Transaction tx = null;
		boolean isNewTx = true;
		try {
			tx = session.getTransaction();
			isNewTx = (tx == null || !tx.isActive());
			if (isNewTx) {
				tx = session.beginTransaction();
			}
			session.save(entity);
			session.flush();
			if (isNewTx && tx != null && tx.isActive()) {
				tx.commit();
			}
		} catch (Exception e) {
			if (isNewTx) {
				if (tx != null && tx.isActive()) {
					try { tx.rollback(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/KegiatanHelper.java:160");}
				}
			} else if (isLockTimeout(e) || isTransactionAborted(e) || isConstraintViolation(e)) {
				// KE-19: insert gagal (mis. unique kegiatan_kodeunik_key / lock timeout) meng-ABORT
				// transaksi milik pemanggil. Pemanggil (checkKegiatanCalonMahasiswa) langsung
				// menjalankan createCriteria di sesi yang sama untuk mengambil baris yang sudah ada
				// -- tanpa rollback, query itu pasti ditolak "current transaction is aborted".
				// isConstraintViolation() ditambahkan: unique-constraint (kodeunik_key) JUGA meng-ABORT
				// transaksi PostgreSQL persis seperti lock timeout -- sebelumnya hanya
				// isLockTimeout/isTransactionAborted yang memicu pemulihan, jadi race double-klik yang
				// gagal karena ConstraintViolationException murni (bukan timeout) meninggalkan transaksi
				// pemanggil tetap ter-abort dan retry "ambil ulang" di checkKegiatanCalonMahasiswa gagal
				// beruntun dengan "current transaction is aborted".
				pulihkanTransaksiTerabort(session, tx);
			}
			try { if (isUsableSession(session)) session.clear(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/KegiatanHelper.java:162");}
			try { if (isUsableSession(session)) session.evict(entity); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/KegiatanHelper.java:163");}
			throw e;
		} finally {
			if (closeLocalSession && isUsableSession(session)) {
				closeOpenedSessionQuietly(session);
			}
		}
	}

	/** Sinonim {@code updateEntitySafe(session, entity, 0)} — titik masuk publik dalam kelas (percobaan pertama, hingga 3x retry di dalam). */
	private static void updateEntitySafe(Session session, Object entity) throws Exception {
		updateEntitySafe(session, entity, 0);
	}

	/**
	 * {@code session.saveOrUpdate(entity)} + {@code flush()} versi paling tahan-banting di kelas
	 * ini — inti dari mayoritas penulisan {@code Kegiatan}/{@code DetailKegiatan}/
	 * {@code CicilanPembayaran}. Menangani berlapis:
	 * <ul>
	 * <li>{@link org.hibernate.NonUniqueObjectException} (entity dengan id sama sudah dikenal
	 * session lain): di-{@code merge()} di session terisolasi terpisah agar tidak memicu
	 * auto-flush entity besar lain di session utama (mis. {@code BiodataCalonMahasiswa} 1664
	 * kolom).</li>
	 * <li>{@link org.hibernate.SessionException} ("Session is closed!" — bisa terjadi saat
	 * dipanggil dari task async/thread pool terpisah yang session pemiliknya sudah ditutup):
	 * bila {@code session} BUKAN session lokal method ini (dioper dari pemanggil), coba
	 * {@code merge()} di session terisolasi; kegagalan merge di jalur ini SENGAJA DITELAN
	 * (dicatat ke {@code ErrorAuditUtil}, tidak dilempar ulang) — didokumentasikan eksplisit di
	 * kode sebagai perilaku recovery best-effort yang dipertahankan apa adanya.</li>
	 * <li><b>Retry otomatis</b> (maksimal 3x, {@code attempt} parameter rekursi): dipicu oleh
	 * {@link #isLockTimeout}, {@link #isTransactionAborted}, atau {@link #isStaleState}. Backoff
	 * {@code 500ms * (attempt+1)} + jitter acak 0-250ms (mencegah dua transaksi yang sama-sama
	 * kena deadlock retry bersamaan dan bertabrakan lagi), lalu retry di SESSION TERISOLASI BARU
	 * (bukan session asli — session asli transaksinya sudah mati, retry di situ akan gagal
	 * "current transaction is aborted"). Rekursi berikutnya SELALU dipanggil dengan
	 * {@code session=null} agar percobaan baru pasti mendapat session+transaksi bersih.</li>
	 * <li>Bila transaksi pemanggil (bukan transaksi lokal method ini) perlu dipulihkan (lock
	 * timeout/transaksi-ter-abort/constraint-violation/stale-state-dengan-baris-sudah-hilang),
	 * {@link #pulihkanTransaksiTerabort} dipanggil sebelum melempar/retry.</li>
	 * <li>Kasus khusus "stale row hilang": {@link org.hibernate.StaleStateException} DAN baris
	 * entity ternyata memang sudah tidak ada di DB ({@link #entityMasihAda} false) dianggap
	 * idempoten (baris sudah dihapus proses paralel lain), bukan kegagalan — method kembali
	 * tanpa exception.</li>
	 * <li>Setelah 3x percobaan tetap gagal karena lock/koneksi, update DILEWATI (bukan
	 * dilempar) dan dicatat sebagai info-audit — asumsinya sinkronisasi berikutnya akan
	 * menghitung ulang entity ini, jadi lebih aman diam daripada melempar error ke UI.</li>
	 * </ul>
	 *
	 * @param session session Hibernate pemanggil, atau {@code null} untuk memaksa session baru (dipakai rekursi retry)
	 * @param entity  entity yang akan di-{@code saveOrUpdate}
	 * @param attempt nomor percobaan saat ini (0 = pertama); rekursi berhenti retry setelah percobaan ke-3
	 * @throws Exception exception asli bila bukan salah satu pola transien di atas, atau retry ke-3 tetap gagal dengan error non-transien
	 */
	private static void updateEntitySafe(Session session, Object entity, int attempt) throws Exception {
		boolean closeLocalSession = false;
		if (!isUsableSession(session)) {
			session = openIsolatedSession();
			closeLocalSession = true;
		}
		Transaction tx = null;
		boolean isNewTx = true;
		try {
			tx = session.getTransaction();
			isNewTx = (tx == null || !tx.isActive());
			if (isNewTx) {
				tx = session.beginTransaction();
			}
			terapkanLockTimeout(session);
			try {
				session.saveOrUpdate(entity);
			} catch (org.hibernate.NonUniqueObjectException e) {
				// Gunakan sesi terpisah untuk merge agar tidak memicu auto-flush
				// entity lain (misal BiodataCalonMahasiswa 1664 kolom) dari sesi utama
				Session isoSession = null;
				Transaction isoTx = null;
				try {
					isoSession = openIsolatedSession();
					isoTx = isoSession.beginTransaction();
					isoSession.merge(entity);
					isoSession.flush();
					isoTx.commit();
				} catch (Exception mergeEx) {
					if (isoTx != null && isoTx.isActive()) {
						try { isoTx.rollback(); } catch (Exception ex2) { ais.common.ErrorAuditUtil.record(ex2, "auto-audit(empty-catch) src/ais/action/master/helper/KegiatanHelper.java:207");}
					}
					throw mergeEx;
				} finally {
					if (isoSession != null && isoSession.isOpen()) {
						closeOpenedSessionQuietly(isoSession);
					}
				}
			} catch (org.hibernate.SessionException e) {
				if (!closeLocalSession) {
					Session isoSession2 = null;
					Transaction isoTx2 = null;
					try {
						isoSession2 = openIsolatedSession();
						isoTx2 = isoSession2.beginTransaction();
						terapkanLockTimeout(isoSession2);
						isoSession2.merge(entity);
						isoSession2.flush();
						isoTx2.commit();
						return;
					} catch (Exception mergeEx) {
						if (isoTx2 != null && isoTx2.isActive()) {
							try { isoTx2.rollback(); } catch (Exception ex2) { ais.common.ErrorAuditUtil.record(ex2, "auto-audit(empty-catch) src/ais/action/master/helper/KegiatanHelper.java:234");}
						}
						// Error G: sebelumnya mergeEx (mis. LazyInitializationException saat cascade
						// merge menyentuh proxy relasi -- Kegiatan.mahasiswa(cascade=MERGE) ->
						// Mahasiswa.dosenPa(cascade=MERGE) -> Pegawai.alamatJalan -> BiodataDosen
						// terikat sesi lain yang sudah tertutup) DITELAN TANPA JEJAK di cabang recovery
						// ini (beda dgn cabang merge saudaranya di atas yang me-rethrow). Entity gagal
						// tersimpan tanpa audit apa pun. Catat agar terlacak; TIDAK rethrow di sini
						// (perilaku lama dipertahankan: cabang ini memang recovery best-effort setelah
						// SessionException, method boleh lanjut idempoten pada pemanggilan berikutnya).
						ais.common.ErrorAuditUtil.record(mergeEx,
								"KegiatanHelper.updateEntitySafe: gagal merge entity pada sesi isolasi recovery (SessionException) - entity="
								+ (entity == null ? "null" : entity.getClass().getName()));
						return;
					} finally {
						if (isoSession2 != null && isoSession2.isOpen()) {
							closeOpenedSessionQuietly(isoSession2);
						}
					}
				} else {
					throw e;
				}
			}
			session.flush();
			if (isNewTx && tx != null && tx.isActive()) {
				tx.commit();
			}
		} catch (Exception e) {
			boolean staleState = isStaleState(e);
			boolean staleRowHilang = staleState && entity instanceof GeneralValueObject
					&& ((GeneralValueObject) entity).getId() != null
					&& !entityMasihAda(entity.getClass(), ((GeneralValueObject) entity).getId());
			// KE-19 ("current transaction is aborted, commands ignored until end of transaction
			// block" / SQLState 25P02): begitu sebuah statement GAGAL di PostgreSQL (di sini
			// flush() kena 55P03 "canceling statement due to lock timeout" saat update baris
			// kegiatan), SELURUH transaksi ditandai ABORT -- statement berikutnya di transaksi
			// yang sama SELALU ditolak sampai transaksi di-rollback. Sebelumnya rollback HANYA
			// dijalankan bila transaksi dibuka di method ini (isNewTx). Untuk sesi milik pemanggil
			// (mis. batch CetakRegistrasiAction.singkronkanDenganPembayaran yang mengoper `session`
			// + transaksi panjangnya) transaksi dibiarkan menggantung dalam kondisi abort, sehingga
			// semua operasi sesudahnya di sesi itu gagal beruntun & koneksi c3p0 tertahan. Kini
			// transaksi yang sudah dipastikan mati SELALU di-rollback lalu dibuka ulang agar sesi
			// pemanggil kembali bisa dipakai (data yang belum commit memang sudah hilang saat
			// PostgreSQL meng-abort transaksi -- rollback tidak menambah kehilangan apa pun).
			boolean transaksiMati = isLockTimeout(e) || isTransactionAborted(e) || staleState;
			// isConstraintViolation() (unique/FK, mis. Mahasiswa.nimkey) JUGA meng-ABORT transaksi
			// PostgreSQL persis seperti lock timeout, jadi transaksi milik pemanggil tetap harus
			// dipulihkan -- tapi TIDAK ikut transaksiMati (dipisah dari kondisi retry di bawah): retry
			// merge 3x pada pelanggaran unique constraint yang genuinely permanen (bukan kontensi
			// sesaat) hanya membuang waktu karena akan gagal identik setiap kali.
			boolean transaksiPerluDipulihkan = transaksiMati || isConstraintViolation(e) || staleRowHilang;
			if (isNewTx) {
				if (tx != null && tx.isActive()) {
					try { tx.rollback(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/KegiatanHelper.java:251");}
				}
			} else if (transaksiPerluDipulihkan) {
				pulihkanTransaksiTerabort(session, tx);
			}
			try { if (isUsableSession(session)) session.clear(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/KegiatanHelper.java:253");}
			// Recalculation dan reversal dapat beradu: bila baris yang sedang disinkronkan
			// memang sudah dihapus transaksi lain, row-count 0 adalah hasil idempoten,
			// bukan kegagalan. Ini tidak hanya terjadi pada Kegiatan, tetapi juga pada
			// DetailKegiatan/CicilanPembayaran yang dibangun ulang saat hitung tagihan.
			if (staleRowHilang) {
				return;
			}
			// Lock timeout (PostgreSQL 55P03: "canceling statement due to lock timeout") = kontensi
			// sesaat pada baris. Statement timeout (57014) sering muncul ketika update menunggu
			// lock lebih lama dari batas database. Deadlock (40P01: "deadlock detected") terjadi saat
			// 2+ transaksi saling menunggu lock milik satu sama lain (mis. dua request memproses
			// Kegiatan/BiodataCalonMahasiswa calon mahasiswa yang SAMA nyaris bersamaan, misalnya karena
			// onInfo ZK terpicu dobel) -- PostgreSQL otomatis membatalkan salah satu transaksi, jadi
			// aman & BENAR untuk dicoba ulang setelah rollback (bukan error data, murni kontensi).
			// isLockTimeout() sekarang mendeteksi ketiganya (55P03/57014/40P01) DAN JUGA kegagalan
			// koneksi transien (JDBCConnectionException / SQLState 08000/08003/08006 / SocketException
			// / SSLException "Socket closed" -- mis. koneksi c3p0 basi krn DB restart/network blip)
			// agar semuanya kena retry yang sama. Coba ulang terbatas dengan sesi bersih.
			if (attempt < 3 && transaksiMati) {
				if (closeLocalSession && isUsableSession(session)) {
					closeOpenedSessionQuietly(session);
				}
				try {
					// Jitter kecil ditambahkan pada backoff agar 2 transaksi yang sama-sama kena
					// deadlock TIDAK retry pada waktu yang persis sama (yang bisa memicu deadlock
					// berulang lagi jika keduanya bangun bersamaan dan berebut lock yang sama).
					long jitter = (long) (Math.random() * 250L);
					Thread.sleep(500L * (attempt + 1) + jitter);
				} catch (InterruptedException ie) {
					Thread.currentThread().interrupt();
				}
				// Retry di sesi TERISOLASI (transaksi BERSIH). Sesi/tx request bisa SUDAH ter-abort oleh
				// lock timeout tadi; mengulang di sesi yang sama akan gagal "current transaction is
				// aborted". merge+flush di sesi baru menuntaskan update entity ini secara mandiri.
				Session retrySession = null;
				Transaction retryTx = null;
				try {
					retrySession = openIsolatedSession();
					retryTx = retrySession.beginTransaction();
					terapkanLockTimeout(retrySession);
					retrySession.merge(entity);
					retrySession.flush();
					retryTx.commit();
					return;
				} catch (Exception retryEx) {
					if (retryTx != null && retryTx.isActive()) {
						try { retryTx.rollback(); } catch (Exception ex2) { ais.common.ErrorAuditUtil.record(ex2, "auto-audit(empty-catch) src/ais/action/master/helper/KegiatanHelper.java:281");}
					}
					if (isLockTimeout(retryEx) || isTransactionAborted(retryEx) || isStaleState(retryEx)) {
						// KE-19 (PENYEBAB UTAMA error yang dilaporkan): percobaan berikutnya JANGAN
						// memakai `sessionAsli` lagi. Sesi itu transaksinya SUDAH ter-abort oleh lock
						// timeout pada percobaan pertama, dan isUsableSession() cuma memeriksa
						// isOpen() -- sesi abort tetap "open" & tx-nya tetap isActive(), sehingga
						// rekursi lama masuk kembali ke transaksi mati itu: saveOrUpdate/flush
						// langsung ditolak "current transaction is aborted, commands ignored until
						// end of transaction block", errornya bukan lagi lock timeout sehingga tidak
						// dikenali sebagai transien dan langsung dilempar ke pemanggil (persis pola
						// updateEntitySafe:257 <- 309 <- 309 <- 309 <- 174 di stack trace).
						// Oper null agar percobaan berikutnya SELALU membuka sesi + transaksi bersih.
						updateEntitySafe(null, entity, attempt + 1);
						return;
					}
					throw retryEx;
				} finally {
					if (retrySession != null && retrySession.isOpen()) {
						closeOpenedSessionQuietly(retrySession);
					}
				}
			}
			if (transaksiMati) {
				ais.common.ErrorAuditUtil.record(e,
						"info-audit KegiatanHelper.updateEntitySafe: update dilewati setelah retry karena lock/koneksi masih sibuk; proses sinkron berikutnya akan menghitung ulang entity="
								+ (entity == null ? "null" : entity.getClass().getName()));
				return;
			}
			throw e;
		} finally {
			if (closeLocalSession && isUsableSession(session)) {
				closeOpenedSessionQuietly(session);
			}
		}
	}

	/**
	 * Mendeteksi exception yang bersifat SEMENTARA/TRANSIEN (bukan error data/logika), sehingga
	 * aman untuk di-retry di sesi/transaksi baru:
	 * - 55P03 "canceling statement due to lock timeout"
	 * - 57014 "canceling statement due to statement timeout" (biasanya juga akibat menunggu lock)
	 * - 40P01 "deadlock detected" (2+ transaksi saling menunggu lock satu sama lain; PostgreSQL
	 *   membatalkan salah satu SECARA OTOMATIS -- retry di transaksi baru valid & aman).
	 * Ketiganya dipicu oleh pola yang sama: beberapa proses (mis. double-submit/double onInfo,
	 * atau proses interaktif+batch) menyentuh baris Kegiatan/BiodataCalonMahasiswa yang sama nyaris
	 * bersamaan.
	 * - JDBCConnectionException / SQLState kelas "08" (mis. 08000/08003/08006 "connection
	 *   failure"/"connection does not exist"/"connection failure") -- koneksi fisik di pool c3p0
	 *   basi/terputus (DB restart, network blip, idle timeout, SSL "Socket closed"). Ini BUKAN
	 *   error data: koneksi berikutnya yang ditarik dari pool pada percobaan baru umumnya sudah
	 *   pulih/tersambung ulang, jadi retry dgn sesi/koneksi baru aman dilakukan (sama seperti pola
	 *   lock-timeout di atas). JANGAN retry untuk error koneksi lain di luar pola ini (mis. auth
	 *   gagal) -- itu bukan transien dan akan gagal lagi tanpa guna.
	 */
	private static boolean isLockTimeout(Throwable e) {
		Throwable t = e;
		while (t != null) {
			if (t instanceof org.hibernate.exception.JDBCConnectionException) {
				return true;
			}
			if (t instanceof java.sql.SQLException) {
				String state = ((java.sql.SQLException) t).getSQLState();
				if ("55P03".equalsIgnoreCase(state) || "57014".equalsIgnoreCase(state)
						|| "40P01".equalsIgnoreCase(state) || "08000".equalsIgnoreCase(state)
						|| "08003".equalsIgnoreCase(state) || "08006".equalsIgnoreCase(state)) {
					return true;
				}
			}
			if (t instanceof java.net.SocketException || t instanceof javax.net.ssl.SSLException) {
				return true;
			}
			String m = t.getMessage();
			if (m != null) {
				String low = m.toLowerCase();
				if (low.indexOf("lock timeout") >= 0
						|| low.indexOf("canceling statement due to lock") >= 0
						|| low.indexOf("statement timeout") >= 0
						|| low.indexOf("canceling statement due to statement timeout") >= 0
						|| low.indexOf("canceling statement due to user request") >= 0
						|| low.indexOf("deadlock detected") >= 0
						|| low.indexOf("55p03") >= 0
						|| low.indexOf("57014") >= 0
						|| low.indexOf("40p01") >= 0
						|| low.indexOf("socket closed") >= 0
						|| low.indexOf("connection reset") >= 0
						|| low.indexOf("broken pipe") >= 0
						|| low.indexOf("i/o error") >= 0
						|| low.indexOf("connection is closed") >= 0
						|| low.indexOf("connection has been closed") >= 0
						// KE-FIX (GenericJDBCException "could not update: [Kegiatan#...]" <-
						// PSQLException "This statement has been closed."): terjadi pada isoSession
						// TERISOLASI yang BARU dibuka (lihat cabang recovery NonUniqueObjectException/
						// SessionException di updateEntitySafe) -- bukan error data, melainkan resource
						// JDBC (PreparedStatement/koneksi) yang sudah tak berlaku begitu dipakai (mis.
						// koneksi dari pool c3p0 yang sudah basi/di-reclaim). Tanpa dikenali di sini,
						// exception ini LOLOS dari cabang retry (attempt<3 && transaksiMati) dan langsung
						// dilempar ke checkKegiatanCalonMahasiswa alih-alih dicoba ulang di sesi bersih
						// seperti pola "connection is closed" di atas.
						|| low.indexOf("statement has been closed") >= 0
						|| low.indexOf("statement is closed") >= 0
						|| low.indexOf("statement already closed") >= 0
						|| low.indexOf("08006") >= 0 || low.indexOf("08003") >= 0
						|| low.indexOf("08000") >= 0) {
					return true;
				}
			}
			t = t.getCause();
		}
		return false;
	}

	/**
	 * Mendeteksi transaksi PostgreSQL yang sudah TER-ABORT: SQLState 25P02 /
	 * "current transaction is aborted, commands ignored until end of transaction block".
	 *
	 * Ini BUKAN error data, melainkan AKIBAT susulan dari statement lain yang gagal lebih dulu di
	 * transaksi yang sama (mis. lock timeout 55P03 saat update baris kegiatan, atau pelanggaran FK).
	 * Selama transaksi belum di-rollback, SEMUA statement berikutnya ditolak dengan error ini. Jadi
	 * penanganannya sama seperti error transien lain: rollback transaksi yang sudah mati, lalu ulangi
	 * pekerjaan pada SESI + TRANSAKSI BARU yang bersih (lihat updateEntitySafe). Dipisah dari
	 * isLockTimeout() agar maksudnya jelas: yang satu "gagal karena kontensi lock", yang ini "gagal
	 * karena transaksinya sudah terlanjur mati".
	 */
	private static boolean isTransactionAborted(Throwable e) {
		Throwable t = e;
		while (t != null) {
			if (t instanceof java.sql.SQLException) {
				String state = ((java.sql.SQLException) t).getSQLState();
				if ("25P02".equalsIgnoreCase(state)) {
					return true;
				}
			}
			String m = t.getMessage();
			if (m != null) {
				String low = m.toLowerCase();
				if (low.indexOf("current transaction is aborted") >= 0
						|| low.indexOf("commands ignored until end of transaction block") >= 0
						|| low.indexOf("25p02") >= 0) {
					return true;
				}
			}
			t = t.getCause();
		}
		return false;
	}

	/**
	 * Mendeteksi pelanggaran unique/FK constraint (mis. kegiatan_kodeunik_key saat dua request
	 * hampir bersamaan lolos cek "belum ada" lalu sama-sama insert). Berbeda dari isLockTimeout()/
	 * isTransactionAborted(), tapi efeknya di PostgreSQL SAMA: begitu satu statement gagal, seluruh
	 * transaksi ikut ter-abort sampai di-rollback. Dipakai agar saveEntitySafe() juga memulihkan
	 * transaksi pemanggil untuk kasus ini, bukan cuma untuk lock timeout.
	 */
	private static boolean isConstraintViolation(Throwable e) {
		Throwable t = e;
		while (t != null) {
			if (t instanceof org.hibernate.exception.ConstraintViolationException) {
				return true;
			}
			if (t instanceof java.sql.SQLException) {
				String state = ((java.sql.SQLException) t).getSQLState();
				if ("23505".equalsIgnoreCase(state) || "23503".equalsIgnoreCase(state)) {
					return true;
				}
			}
			t = t.getCause();
		}
		return false;
	}

	/**
	 * Memulihkan sesi MILIK PEMANGGIL yang transaksinya sudah dipastikan mati (ter-abort PostgreSQL).
	 * Rollback -> clear persistence context -> buka transaksi baru, semuanya best-effort.
	 *
	 * Kenapa perlu: `session` di sini kerap dioper dari proses batch berumur panjang
	 * (CetakRegistrasiAction.singkronkanDenganPembayaran) yang membuka SATU transaksi untuk banyak
	 * calon mahasiswa. Bila satu update kena lock timeout, transaksi itu abort; tanpa rollback, sisa
	 * pekerjaan batch (dan koneksi c3p0 yang dipegangnya) ikut mati beruntun. Data yang belum commit
	 * memang sudah dibuang oleh PostgreSQL saat abort, jadi rollback tidak menambah kehilangan data.
	 * clear() dipanggil sebelum transaksi baru dibuka supaya entity kotor sisa transaksi lama tidak
	 * ikut ter-auto-flush ke transaksi baru.
	 */
	private static void pulihkanTransaksiTerabort(Session session, Transaction tx) {
		if (session == null) {
			return;
		}
		try {
			if (tx != null && tx.isActive()) {
				tx.rollback();
			}
		} catch (Exception ex) {
			if (!ais.common.Common.isTransientKoneksiError(ex)) {
				ais.common.ErrorAuditUtil.record(ex,
						"KegiatanHelper.pulihkanTransaksiTerabort: gagal rollback transaksi ter-abort milik pemanggil");
			}
		}
		try {
			if (isUsableSession(session)) {
				session.clear();
			}
		} catch (Exception ex) {
			ais.common.ErrorAuditUtil.record(ex,
					"KegiatanHelper.pulihkanTransaksiTerabort: gagal clear session setelah rollback");
		}
		try {
			if (isUsableSession(session)) {
				Transaction txBaru = session.getTransaction();
				if (txBaru == null || !txBaru.isActive()) {
					session.beginTransaction();
				}
			}
		} catch (Exception ex) {
			if (!ais.common.Common.isTransientKoneksiError(ex)) {
				ais.common.ErrorAuditUtil.record(ex,
						"KegiatanHelper.pulihkanTransaksiTerabort: gagal membuka transaksi baru di sesi pemanggil");
			}
		}
	}

	/**
	 * Menjalankan SQL native ({@code executeUpdate}, mis. {@code DELETE FROM detail_kegiatan ...}
	 * dipakai {@link #checkKegiatanCalonMahasiswa}/{@link #checkKegiatanMahasiswa} saat
	 * {@code rst=true} untuk membuang rincian tagihan lama sebelum dihitung ulang) dalam transaksi
	 * yang aktif atau baru. Berbeda dari {@link #saveEntitySafe}/{@link #updateEntitySafe}: TIDAK
	 * pernah melempar exception ke pemanggil (semua exception ditangkap, di-audit, dan bila
	 * transien di-passthrough ke {@link #pulihkanTransaksiTerabort}) — dipakai untuk operasi yang
	 * boleh gagal diam-diam karena hasil akhirnya akan disinkronkan ulang oleh proses pemanggil.
	 *
	 * @param session session Hibernate aktif (transaksi baru dibuka di sini bila belum ada)
	 * @param sql     SQL native dengan named parameter (mis. {@code :kegId})
	 * @param params  nilai named parameter, boleh {@code null}
	 */
	private static void executeUpdateSafe(Session session, String sql, Map<String, Object> params) {
		if (!isUsableSession(session)) {
			return;
		}
		Transaction tx = session.getTransaction();
		boolean isNewTx = (tx == null || !tx.isActive());
		if (isNewTx)
			tx = session.beginTransaction();
		try {
			org.hibernate.Query query = session.createSQLQuery(sql);
			if (params != null) {
				for (Map.Entry<String, Object> entry : params.entrySet()) {
					query.setParameter(entry.getKey(), entry.getValue());
				}
			}
			query.executeUpdate();
			if (isNewTx)
				tx.commit();
		} catch (Exception e) {
			if (isNewTx) {
				if (tx != null && tx.isActive()) {
					try {
						tx.rollback();
					} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/KegiatanHelper.java:348");
					}
				}
			} else if (isLockTimeout(e) || isTransactionAborted(e)) {
				// KE-19: DELETE detail_kegiatan yang kena lock timeout meng-ABORT transaksi milik
				// pemanggil. Exception di sini hanya dicatat (tidak dilempar), jadi tanpa rollback
				// alur berlanjut ke saveEntitySafe/updateEntitySafe di transaksi yang sudah mati dan
				// semuanya gagal dengan "current transaction is aborted".
				pulihkanTransaksiTerabort(session, tx);
			}
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/KegiatanHelper.java:351");
		}
	}

	/** Penutupan session tiga-langkah ({@code clear}/{@code disconnect}/{@code close}), masing-masing try-catch terpisah — padanan {@link #closeOpenedSessionQuietly} (lihat catatan kuirk redundansi {@code disconnect()} pada {@code closeSession} di {@code HistoryStatusMahasiswaUtil}, pola yang sama berlaku di sini). Aman dipanggil dengan {@code null}. */
	private static void closeLocalSessionSafely(Session session) {
		if (session != null) {
			try {
				session.clear(); // Bersihkan object cache
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/KegiatanHelper.java:359");
			}
			try {
				session.disconnect(); // Putus dari connection pool
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/KegiatanHelper.java:363");
			}
			try {
				if (session.isOpen()) {
					session.close(); // Tutup penuh session lokal
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/KegiatanHelper.java:369");
			}
		}
	}

	/** Mendeteksi {@link org.hibernate.StaleStateException}/{@link org.hibernate.StaleObjectStateException} (baris berubah/hilang antara baca dan tulis versi optimistic-locking Hibernate) di sepanjang rantai {@code cause} — dipakai {@link #updateEntitySafe} untuk memutuskan retry vs. anggap idempoten lewat {@link #entityMasihAda}. */
	private static boolean isStaleState(Throwable error) {
		Throwable current = error;
		while (current != null) {
			if (current instanceof org.hibernate.StaleStateException
					|| current instanceof org.hibernate.StaleObjectStateException) return true;
			current = current.getCause();
		}
		return false;
	}

	/**
	 * Mengecek keberadaan baris {@code clazz} ber-id {@code id} lewat session TERISOLASI baru
	 * (bukan cache session pemanggil yang mungkin sudah basi) — dipakai {@link #updateEntitySafe}
	 * untuk membedakan {@link org.hibernate.StaleStateException} akibat baris memang sudah DIHAPUS
	 * proses lain (idempoten, aman diabaikan) dari stale state akibat sebab lain (perlu di-retry/
	 * dilempar). Pada exception saat pengecekan, SENGAJA mengembalikan {@code true} ("anggap masih
	 * ada") — sikap konservatif agar kegagalan pengecekan tidak salah menyimpulkan baris hilang
	 * dan diam-diam melewatkan update yang sebenarnya perlu di-retry.
	 *
	 * @param clazz kelas entity yang dicek
	 * @param id    id baris yang dicek
	 * @return {@code true} bila baris ditemukan (atau pengecekan gagal — default aman); {@code false} hanya bila positif tidak ditemukan
	 */
	private static boolean entityMasihAda(Class clazz, Serializable id) {
		Session cek = null;
		try {
			cek = openIsolatedSession();
			return cek.get(clazz, id) != null;
		} catch (Exception e) {
			return true;
		} finally {
			closeOpenedSessionQuietly(cek);
		}
	}

	/**
	 * Membaca pemenang race kodeunik melalui sesi baru. Sesi pemanggil dapat sudah berada
	 * dalam transaksi PostgreSQL yang abort setelah pelanggaran unique constraint.
	 */
	private static Kegiatan ambilKegiatanKodeunikTerisolasi(String kodeunik) {
		if (kodeunik == null || kodeunik.trim().isEmpty()) return null;
		Session localSession = null;
		try {
			localSession = openIsolatedSession();
			return (Kegiatan) localSession.createCriteria(Kegiatan.class)
					.add(Restrictions.eq("kodeunik", kodeunik)).setMaxResults(1)
					.addOrder(Order.asc("id")).uniqueResult();
		} finally {
			closeLocalSessionSafely(localSession);
		}
	}

	// ===================================================================================
	// PUBLIC BUSINESS LOGIC METHODS
	// ===================================================================================

	/**
	 * Overload yang menyelesaikan {@link JadwalPembayaran} sendiri lewat
	 * {@code PembayaranUtil.getJadwalPembayaranDanDendaBerdasarkanTahunAkademik} sebelum
	 * mendelegasikan ke {@link #checkKegiatanCalonMahasiswa(Kegiatan, JenisKegiatan, BiodataCalonMahasiswa, Integer, String, Boolean, JadwalPembayaran, boolean, boolean, ItemBiaya, Session)}.
	 * Ganjil/genap ditentukan dari paritas {@code smt}, KECUALI untuk jenis kegiatan
	 * "Pendaftaran Calon Mahasiswa" ({@link ConstantValues#PENDAFTARAN_CALON_MAHASISWA}) yang
	 * memakai {@code biodataCalonMahasiswa.getSemesterMulai()} sebagai acuan (calon mahasiswa
	 * belum tentu masuk di semester sesuai paritas nomor semesternya).
	 *
	 * @param jenisKegiatan          jenis kegiatan/tagihan yang dicari
	 * @param biodataCalonMahasiswa calon mahasiswa target
	 * @param smt                   semester tagihan
	 * @param ta                    tahun akademik
	 * @param hitungUlang           {@code true} untuk memaksa hitung ulang tagihan dari {@code SettingBiaya}
	 * @param rst                   {@code true} untuk reset (hapus detail lama yang belum dibayar/dikunci) sebelum hitung ulang
	 * @param item                  batasi ke satu {@link ItemBiaya} tertentu, atau {@code null} untuk semua
	 * @param session               session Hibernate aktif
	 * @return {@link Kegiatan} yang sudah ada atau baru dibuat, bisa {@code null} bila jadwal tidak ditemukan dan tidak ada baris existing
	 */
	public static Kegiatan checkKegiatanCalonMahasiswa(JenisKegiatan jenisKegiatan,
			BiodataCalonMahasiswa biodataCalonMahasiswa, Integer smt, String ta, Boolean hitungUlang, boolean rst,
			ItemBiaya item, Session session) {

		boolean ganjil = smt.intValue() % 2 != 0;
		if (jenisKegiatan != null && ConstantValues.PENDAFTARAN_CALON_MAHASISWA != null
				&& jenisKegiatan.getId().equals(ConstantValues.PENDAFTARAN_CALON_MAHASISWA.getId())) {
			ganjil = biodataCalonMahasiswa.getSemesterMulai().equalsIgnoreCase(Perkuliahan.GANJIL);
		}

		Serializable[] serializables = PembayaranUtil.getInstance().getJadwalPembayaranDanDendaBerdasarkanTahunAkademik(
				null, jenisKegiatan, biodataCalonMahasiswa.getJenjang(), ta, ganjil,
				biodataCalonMahasiswa.getJenisSeleksi(), biodataCalonMahasiswa.getProgram(),
				biodataCalonMahasiswa.getNoRegistrasi(), biodataCalonMahasiswa.getGelombangPendaftaran());

		JadwalPembayaran j = (JadwalPembayaran) (serializables != null && serializables.length > 0 ? serializables[0]
				: null);

		return checkKegiatanCalonMahasiswa(jenisKegiatan, biodataCalonMahasiswa, smt, ta, hitungUlang, j, rst, item,
				session);
	}

	/** Sinonim {@code checkKegiatanCalonMahasiswa(null, jenisKegiatan, ..., jadwal, rst, false, item, session)} dengan {@code jadwal} sudah diketahui pemanggil (tidak perlu di-resolve ulang) dan tanpa {@code Kegiatan} existing yang diketahui. */
	public static Kegiatan checkKegiatanCalonMahasiswa(JenisKegiatan jenisKegiatan,
			BiodataCalonMahasiswa biodataCalonMahasiswa, Integer smt, String ta, Boolean hitungUlang,
			JadwalPembayaran jadwal, boolean rst, ItemBiaya item, Session session) {

		return checkKegiatanCalonMahasiswa(null, jenisKegiatan, biodataCalonMahasiswa, smt, ta, hitungUlang, jadwal,
				rst, false, item, session);
	}

	/**
	 * Implementasi inti "get-or-create tagihan calon mahasiswa": no-op (kembalikan {@code kegiatan}
	 * apa adanya) bila {@code jadwal == null && !hitungUlang}. Selain itu, alur:
	 * <ol>
	 * <li>Jenis kegiatan "Pendaftaran Calon Mahasiswa" untuk {@code smt > 0} langsung
	 * dikembalikan {@code null} (kegiatan ini hanya valid di semester 0 — pendaftaran awal).</li>
	 * <li>Bila {@code kegiatan} belum diketahui, dicari lewat
	 * {@code biodataCalonMahasiswa.ambilKegiatans(...)}, lalu fallback query {@code kodeunik}
	 * deterministik ({@link Kegiatan#generateKodeUnik}) bila masih kosong.</li>
	 * <li>Bila {@code rst=true} dan kegiatan ditemukan: cache
	 * {@link Kegiatan#batalkanCacheDetailKegiatan(Long)} dibersihkan, dan {@link DetailKegiatan}
	 * yang belum dibayar/dikunci/di-posting DIHAPUS
	 * langsung via SQL ({@link #executeUpdateSafe}) — dibatasi ke {@code item} tertentu bila
	 * diisi, atau (bila {@code diubahSaatpembayaran}) hanya item yang nilainya TIDAK bisa
	 * diubah manual.</li>
	 * <li>Bila belum ada baris sama sekali dan {@code jadwal} tersedia: {@link Kegiatan} baru
	 * dibuat (status AKTIF, amount 0) dan disimpan ({@link #saveEntitySafe}), totalnya dihitung
	 * lewat {@link #dataTagihanCalonMahasiswa} lalu di-update. Kegagalan simpan karena
	 * pelanggaran unique constraint {@code kodeunik} (race dua request nyaris bersamaan) ditangani
	 * dengan mengambil ulang baris pemenang race lewat {@link #ambilKegiatanKodeunikTerisolasi}.</li>
	 * <li>Bila baris sudah ada dan {@code hitungUlang=true}: seluruh {@link CicilanPembayaran}
	 * dijumlah ulang ({@code amount}/{@code denda}/tanggal bayar awal-akhir), FK
	 * {@code kegiatan.mahasiswa} DIVALIDASI lewat SQL native langsung ke tabel (BUKAN
	 * {@code session.get()}, untuk menghindari L2 cache yang bisa mengembalikan mahasiswa yang
	 * sudah dihapus) dan di-null-kan bila stale (mencegah FK violation), lalu totalTagihan dihitung
	 * ulang via {@link #dataTagihanCalonMahasiswa} dan diupdate. Kegagalan update di jalur ini
	 * DITELAN (dicatat + ditampilkan ke admin) agar halaman sukses login calon mahasiswa tidak
	 * menampilkan error mentah — akan disinkronkan ulang oleh proses batch.</li>
	 * </ol>
	 * Session lokal disinkronkan ulang ({@code HibernateUtil.ensureOpenSession}) setelah memanggil
	 * {@link #dataTagihanCalonMahasiswa} karena method itu (lewat helper bersarangnya) dapat
	 * mengganti native session ThreadLocal, sehingga variabel {@code session} lokal method ini bisa
	 * menunjuk session yang sudah tertutup bila tidak disegarkan.
	 *
	 * @param kegiatan               baris existing bila sudah diketahui pemanggil, atau {@code null} untuk dicari
	 * @param jenisKegiatan          jenis kegiatan/tagihan
	 * @param biodataCalonMahasiswa calon mahasiswa target
	 * @param smt                   semester tagihan
	 * @param ta                    tahun akademik (dipakai saat membuat baris baru)
	 * @param hitungUlang           {@code true} untuk memicu hitung ulang meski {@code jadwal} kosong
	 * @param jadwal                jadwal pembayaran; {@code null} berarti baris baru tidak akan dibuat
	 * @param rst                   {@code true} untuk menghapus detail lama yang masih bebas (belum dibayar/dikunci/diposting) sebelum hitung ulang
	 * @param diubahSaatpembayaran  saat {@code rst=true} tanpa {@code item}, batasi penghapusan ke item yang nilainya tidak bisa diubah manual
	 * @param item                  batasi operasi ke satu {@link ItemBiaya}, atau {@code null} untuk semua
	 * @param session               session Hibernate aktif (bisa disinkronkan ulang secara internal — lihat catatan di atas)
	 * @return {@link Kegiatan} yang sudah ada/baru dibuat, atau {@code null}/{@code kegiatan} apa adanya sesuai kondisi di atas
	 */
	public static Kegiatan checkKegiatanCalonMahasiswa(Kegiatan kegiatan, JenisKegiatan jenisKegiatan,
			BiodataCalonMahasiswa biodataCalonMahasiswa, Integer smt, String ta, Boolean hitungUlang,
			JadwalPembayaran jadwal, boolean rst, boolean diubahSaatpembayaran, ItemBiaya item, Session session) {

		if (jadwal != null || hitungUlang) {
			if (jenisKegiatan != null && ConstantValues.PENDAFTARAN_CALON_MAHASISWA != null && smt > 0
					&& jenisKegiatan.getId().equals(ConstantValues.PENDAFTARAN_CALON_MAHASISWA.getId())) {
				return null;
			}

			if (kegiatan == null || kegiatan.getId() == null) {
				kegiatan = biodataCalonMahasiswa.ambilKegiatans(smt, jenisKegiatan, true);
			}

			if (kegiatan == null) {
				String tambahanKodeUnik = "";
				String kodeunik = Kegiatan.generateKodeUnik(null, biodataCalonMahasiswa, jenisKegiatan, smt,
						tambahanKodeUnik, null);
				kegiatan = (Kegiatan) session.createCriteria(Kegiatan.class).add(Restrictions.eq("kodeunik", kodeunik))
						.setMaxResults(1).addOrder(Order.asc("id")).uniqueResult();
			}

			if (kegiatan != null && kegiatan.getId() != null && rst) {
				Kegiatan.batalkanCacheDetailKegiatan(kegiatan.getId());

				java.util.HashMap<String, Object> params = new java.util.HashMap<String, Object>();
				params.put("kegId", kegiatan.getId());

				String sql;
				if (item != null) {
					sql = "delete from detail_kegiatan where kegiatan = :kegId "
							+ "and (batalkandenda is null or batalkandenda=false) "
							+ "and (menggunakandendacustom is null or menggunakandendacustom=false) "
							+ "and posting_history is null and kunci is null " + "and item_biaya = :itemId "
							+ "and (bukantagihan is null or bukantagihan=false)";
					params.put("itemId", item.getId());
				} else {
					sql = "delete from detail_kegiatan where kegiatan = :kegId "
							+ "and (batalkandenda is null or batalkandenda=false) "
							+ "and (menggunakandendacustom is null or menggunakandendacustom=false) "
							+ "and posting_history is null and kunci is null "
							+ "and (bukantagihan is null or bukantagihan=false) "
							+ (diubahSaatpembayaran
									? "and item_biaya in (select id from item_biaya where nilaibisadiubah = false)"
									: "");
				}
				System.out.println("Hapus -> " + sql);
				executeUpdateSafe(session, sql, params);
			}

			if (jadwal != null && (kegiatan == null || kegiatan.getId() == null)) {
				kegiatan = new Kegiatan();
				kegiatan.setAmount(0.0);
				kegiatan.setJadwalPembayaran(jadwal);
				kegiatan.setCalonMahasiswa(biodataCalonMahasiswa);
				kegiatan.setTahunAkademik(biodataCalonMahasiswa.getTahunAkademik());
				kegiatan.setSemster(smt);
				kegiatan.setProgram(biodataCalonMahasiswa.getProgram());
				kegiatan.setTanggal(jadwal.getStartDate());
				kegiatan.setValidated(1);
				kegiatan.setStatusMahasiswa(ConstantValues.AKTIF);
				kegiatan.setJenisKegiatan(jenisKegiatan);
				kegiatan.setDenda(0.0);
				kegiatan.setJumlahTelahDibayar(0.0);

				try {
					saveEntitySafe(session, kegiatan);

					Double totalTagihan = dataTagihanCalonMahasiswa(biodataCalonMahasiswa, kegiatan.getSemster(),
							jenisKegiatan, session, kegiatan, rst, hitungUlang, null);
					kegiatan.setAmountTerhutang(totalTagihan);

					// FIX "Illegal attempt to associate a collection with two open sessions":
					// dataTagihanCalonMahasiswa() bisa menutup LALU membuka ulang native session
					// ThreadLocal secara internal (helper bersarang di dalamnya, mis.
					// PembayaranUtilHelper.getDetailBiayaCalonMahasiswa/countBulanan, kadang menutup
					// currentNativeSession() sebelum selesai) -- reassignment `session` di dalam method
					// itu HANYA lokal (Java pass-by-value), jadi variabel `session` DI SINI tetap
					// menunjuk sesi LAMA yang sudah tertutup, padahal koleksi kegiatan
					// (kegiatan.ambilDetailKegiatan()/DetailKegiatan) yang disentuh di dalam
					// dataTagihanCalonMahasiswa sudah terikat ke sesi BARU. Kalau updateEntitySafe di
					// bawah lalu membuka sesi KETIGA (openIsolatedSession, karena `session` lokal ini
					// closed), entity & koleksinya jadi "dimiliki" dua sesi terbuka sekaligus ->
					// HibernateException dari WrapVisitor. Samakan `session` dengan sesi yang benar-benar
					// aktif sekarang sebelum dipakai lagi -- no-op bila `session` masih usable (mis.
					// konteks ZK yang mengoper currentSession()), fallback ke currentNativeSession() bila
					// sudah closed (konteks JSP/native thread yang ditutup nested helper di atas).
					session = HibernateUtil.ensureOpenSession(session);

					updateEntitySafe(session, kegiatan);
				} catch (Exception e) {
					// FIX (23505 kegiatan_kodeunik_key): kodeunik dihasilkan deterministik dari
					// (calonMahasiswa, jenisKegiatan, semester) -- kalau ADA request lain (mis.
					// double-klik, atau proses lain berjalan hampir bersamaan) yang lolos cek
					// "kegiatan == null" di atas lebih dulu dan sudah commit baris yang SAMA,
					// insert kita di sini gagal karena unique constraint. saveEntitySafe() sudah
					// rollback transaksi lokal & clear/evict entity, jadi sesi aman dipakai lagi
					// -- ambil ulang baris yang SUDAH ADA (dibuat pihak lain) alih-alih
					// membiarkan variabel kegiatan menunjuk objek transient gagal simpan (id=null)
					// yang akan bikin NPE/state rusak di kode pemanggil.
					try {
						String kodeunikRetry = Kegiatan.generateKodeUnik(null, biodataCalonMahasiswa, jenisKegiatan,
								smt, "", null);
						Kegiatan kegiatanSudahAda = ambilKegiatanKodeunikTerisolasi(kodeunikRetry);
						if (kegiatanSudahAda != null) {
							kegiatan = kegiatanSudahAda;
						} else {
							Common.tampilErrorJikaAdmin(e);
							ais.common.ErrorAuditUtil.record(e,
									"checkKegiatanCalonMahasiswa: gagal simpan Kegiatan baru & tidak ditemukan "
											+ "baris existing utk kodeunik=" + kodeunikRetry);
						}
					} catch (Exception eRetry) {
						Common.tampilErrorJikaAdmin(e);
						ais.common.ErrorAuditUtil.record(eRetry,
								"checkKegiatanCalonMahasiswa: gagal ambil ulang Kegiatan setelah constraint violation");
					}
				}

			} else if (kegiatan != null && hitungUlang) {
				try {
					// FIX SessionException "Session is closed!": dipanggil dari task async (thread
					// pool terpisah, lihat KegiatanProsesHeper$5$2$2$2.call) -- session bisa sudah
					// ditutup thread pemiliknya sebelum task ini jalan. Cek isOpen() dulu supaya
					// tidak memicu exception dari session.contains() pada session yang sudah closed.
					if (session != null && session.isOpen() && session.contains(kegiatan))
						session.refresh(kegiatan);
					biodataCalonMahasiswa.populateKegiatan(kegiatan.getId());
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/KegiatanHelper.java:492");
				}

				Double amountTotal = 0.0;
				Double denda = 0.0;
				kegiatan.resetBulans();
				// Pemanggil masih memegang transaksi yang akan menyimpan Kegiatan ini. Hitung
				// snapshot cicilan secara lokal agar tidak membuka transaksi kedua yang meng-update
				// baris sama dan menunggu lock milik transaksi ini sendiri (55P03), lalu berujung
				// retry merge koleksi ke dua session berbeda.
				List<CicilanPembayaran> cicilanPembayarans = KegiatanPersistenceHelper.ambilCicilan(kegiatan,
						hitungUlang, false);

				Date tglMax = null;
				Date tglMin = null;

				for (CicilanPembayaran cicilanPembayaran : cicilanPembayarans) {
					if (cicilanPembayaran != null && cicilanPembayaran.getId() != null
							&& cicilanPembayaran.getKegiatan() != null
							&& kegiatan.getId().equals(cicilanPembayaran.getKegiatan().getId())) {
						if (cicilanPembayaran.getNilai() > 0.0 || !kegiatan.getJenisKegiatan().getAbaikanNilaiMinus()) {
							amountTotal += cicilanPembayaran.getNilai();
							denda += cicilanPembayaran.getDenda();
						}
					}

					if (cicilanPembayaran != null && cicilanPembayaran.getNilai().intValue() != 0) {
						if (tglMax == null || cicilanPembayaran.getTanggal().after(tglMax)) {
							tglMax = cicilanPembayaran.getTanggal();
						}
						if (tglMin == null || cicilanPembayaran.getTanggal().before(tglMin)) {
							tglMin = cicilanPembayaran.getTanggal();
						}
					}
				}

				kegiatan.setTanggalBayarAwal(tglMin);
				kegiatan.setTanggalBayarTerakhir(tglMax);
				kegiatan.setAmount(amountTotal);
				kegiatan.setJumlahTelahDibayar(amountTotal);

				try {
					Double totalTagihan = dataTagihanCalonMahasiswa(biodataCalonMahasiswa, kegiatan.getSemster(),
							jenisKegiatan, session, kegiatan, rst, hitungUlang, cicilanPembayarans);
					kegiatan.setAmountTerhutang(totalTagihan - amountTotal);

					// FIX "Illegal attempt to associate a collection with two open sessions": lihat
					// catatan sama di cabang (kegiatan==null) di atas -- dataTagihanCalonMahasiswa() bisa
					// mengganti native session ThreadLocal secara internal; sinkronkan kembali `session`
					// lokal di sini (no-op bila masih usable) sebelum dipakai lagi di bawah
					// (updateEntitySafe(session, kegiatan)), supaya tidak berujung entity yang koleksinya
					// sudah terikat sesi lain diserahkan ke sesi ketiga yang berbeda lagi.
					session = HibernateUtil.ensureOpenSession(session);

					// KE-17/18: cegah FK violation "kegiatan.mahasiswa not present". Bila FK mahasiswa menunjuk
					// baris yang sudah tidak ada (calon yang mahasiswanya stale/dihapus), null-kan agar update tak
					// menabrak fkffabc55cc20aa61f (yang juga meng-abort tx -> KE-18 "transaction aborted"). Cek di
					// sesi ISOLASI agar tidak memicu auto-flush kegiatan yang sedang kotor.
					if (kegiatan.getMahasiswa() != null) {
						Long idMhsCek = kegiatan.getMahasiswa().getId();
						boolean mahasiswaValid;
						if (idMhsCek == null) {
							// KE-7: Mahasiswa TRANSIENT (belum tersimpan, id == null). Kegiatan CALON mahasiswa
							// TIDAK boleh mereferensi Mahasiswa transient: saat session.flush() Hibernate mencoba
							// ambil identifier FK -> "TransientObjectException: object references an unsaved
							// transient instance: ais.database.model.Mahasiswa". Kegiatan ini milik calonMahasiswa
							// (di-set via setCalonMahasiswa), jadi referensi mahasiswa dinull-kan saja.
							mahasiswaValid = false;
						} else {
							mahasiswaValid = true;
							Session cekMhs = null;
							try {
								cekMhs = openIsolatedSession();
								// Cek keberadaan baris LANGSUNG ke tabel via SQL native — BUKAN session.get()
								// yang dapat mengembalikan Mahasiswa BASI dari L2/persistence cache. Bila memakai
								// get(), mahasiswa yang sudah DIHAPUS (mis. id 172040) masih "ada" menurut cache →
								// FK stale lolos → update menabrak fkffabc55cc20aa61f (dan meng-abort transaksi).
								Object adaMhs = cekMhs.createSQLQuery("SELECT id FROM mahasiswa WHERE id = :id")
										.setParameter("id", idMhsCek).setMaxResults(1).uniqueResult();
								mahasiswaValid = (adaMhs != null);
							} catch (Exception eCekMhs) {
								mahasiswaValid = true;
							} finally {
								if (cekMhs != null && cekMhs.isOpen()) { closeOpenedSessionQuietly(cekMhs); }
							}
						}
						if (!mahasiswaValid) { kegiatan.setMahasiswa(null); }
					}

					updateEntitySafe(session, kegiatan);
				} catch (Exception e) {
					// Lock timeout (55P03) yang masih gagal setelah retry di updateEntitySafe (mis. saat
					// banyak calon mahasiswa login bersamaan di _sukses_login.jsp) TIDAK BOLEH bikin
					// halaman sukses login tampil error mentah ke calon mahasiswa. Tetap CATAT ke
					// ErrorAudit (sebelumnya update ini gagal TANPA jejak audit, cuma tampil ke admin)
					// supaya bisa diselaraskan ulang oleh CetakRegistrasiAction.singkronkanDenganPembayaran
					// (idempoten) tanpa perlu di-retry manual; proses login tetap lanjut memakai
					// kegiatan versi lama (belum ter-update) daripada melempar exception ke pemanggil.
					ais.common.ErrorAuditUtil.record(e,
							"checkKegiatanCalonMahasiswa: gagal updateEntitySafe Kegiatan id="
									+ (kegiatan != null && kegiatan.getId() != null ? kegiatan.getId() : "null")
									+ " (kemungkinan lock timeout 55P03/57014 atau deadlock 40P01 setelah retry habis"
									+ " -- kontensi baris, bukan error data) - lanjut tanpa update, akan disinkronkan ulang batch");
					Common.tampilErrorJikaAdmin(e);
				}
			}
		}
		return kegiatan;
	}

	/**
	 * Padanan {@link #checkKegiatanCalonMahasiswa(JenisKegiatan, BiodataCalonMahasiswa, Integer, String, Boolean, boolean, ItemBiaya, Session)}
	 * untuk mahasiswa aktif: menyelesaikan {@link JadwalPembayaran} sendiri (ganjil/genap dari
	 * paritas {@code smt}) sebelum mendelegasikan ke overload inti.
	 *
	 * @param j           jenis kegiatan/tagihan
	 * @param mahasiswa   mahasiswa target
	 * @param smt         semester tagihan
	 * @param ta          tahun akademik
	 * @param hitungUlang {@code true} untuk memaksa hitung ulang tagihan
	 * @param rst         {@code true} untuk reset detail lama sebelum hitung ulang
	 * @param item        batasi ke satu {@link ItemBiaya}, atau {@code null} untuk semua
	 * @param session     session Hibernate aktif
	 * @return {@link Kegiatan} yang sudah ada atau baru dibuat
	 */
	public static Kegiatan checkKegiatanMahasiswa(JenisKegiatan j, Mahasiswa mahasiswa, Integer smt, String ta,
			Boolean hitungUlang, boolean rst, ItemBiaya item, Session session) {

		Serializable[] serializables = PembayaranUtil.getInstance().getJadwalPembayaranDanDendaBerdasarkanTahunAkademik(
				null, j, mahasiswa.getJenjang(), ta, smt.intValue() % 2 != 0, mahasiswa.getJenisSeleksi(),
				mahasiswa.getProgram(), mahasiswa.getNim(), null);

		JadwalPembayaran jadwalPembayaran = (JadwalPembayaran) (serializables != null && serializables.length > 0
				? serializables[0]
				: null);
		return checkKegiatanMahasiswa(j, mahasiswa, smt, ta, hitungUlang, jadwalPembayaran, rst, item, session);
	}

	/** Sinonim {@code updateBatasStudiMahasiswa(mahasiswa, session, smt, checkStatusPembayaranMahasiswa, false)} — hanya memutasi field {@link Mahasiswa#getBatasStudi()} di memori, TIDAK menyimpan ke DB (lihat parameter {@code simpan} di overload lengkap). */
	public static void updateBatasStudiMahasiswa(Mahasiswa mahasiswa, Session session, Integer smt,
			boolean checkStatusPembayaranMahasiswa) {
		updateBatasStudiMahasiswa(mahasiswa, session, smt, checkStatusPembayaranMahasiswa, false);
	}

	/**
	 * Memelihara daftar {@link Mahasiswa#getBatasStudi()} (semester "batas studi", dipisah koma —
	 * mis. "3,5,7") berdasarkan status pembayaran kegiatan bersyarat-aktif. Hanya bertindak bila
	 * {@code checkStatusPembayaranMahasiswa} bernilai {@code true} (artinya "mahasiswa TERBUKTI
	 * sudah bayar semester ini") — dalam kasus itu, semester {@code smt} DIHAPUS dari daftar batas
	 * studi (bila ada) karena tidak lagi relevan sebagai batas. Bila {@code false} (belum bayar),
	 * method ini SENGAJA tidak menambahkan semester ke daftar — penambahan batas studi ditangani
	 * di jalur lain (mis. {@code HistoryStatusMahasiswaUtil}/proses tagihan), method ini murni
	 * "membersihkan" entri yang sudah tidak relevan.
	 * <p>Bila {@code simpan=true} dan ada perubahan, {@link Mahasiswa} disegarkan
	 * ({@code session.refresh}, hanya jika sudah dikenal session) lalu diupdate lewat
	 * {@code Common.refreshUpdate} dalam session lokal (dibuka sendiri bila {@code session}
	 * {@code null}); bila {@code simpan=false}, perubahan HANYA di objek Java in-memory —
	 * pemanggil bertanggung jawab menyimpannya sendiri (mis. sebagai bagian transaksi yang lebih
	 * besar). Seluruh method dibungkus try-catch tunggal: kegagalan dicatat ke
	 * {@code ErrorAuditUtil} dan ditelan, tidak dilempar ke pemanggil.
	 *
	 * @param mahasiswa                       mahasiswa yang batas studinya dievaluasi
	 * @param session                         session Hibernate, boleh {@code null} (dibuka sendiri bila {@code simpan=true})
	 * @param smt                             semester yang dievaluasi/dihapus dari daftar batas studi
	 * @param checkStatusPembayaranMahasiswa  {@code true} bila mahasiswa terbukti sudah bayar (memicu penghapusan dari daftar)
	 * @param simpan                          {@code true} untuk langsung menyimpan perubahan ke DB; {@code false} untuk hanya memutasi objek di memori
	 */
	public static void updateBatasStudiMahasiswa(Mahasiswa mahasiswa, Session session, Integer smt,
			boolean checkStatusPembayaranMahasiswa, boolean simpan) {

		try {
			StringBuilder batasStudiBaru = new StringBuilder();

			if (checkStatusPembayaranMahasiswa && mahasiswa.getBatasStudi() != null) {
				for (String s : mahasiswa.getBatasStudi().split(",")) {
					if (!s.equalsIgnoreCase(String.valueOf(smt))) {
						if (batasStudiBaru.length() > 0) {
							batasStudiBaru.append(",");
						}
						batasStudiBaru.append(s);
					}
				}

				if (!mahasiswa.getBatasStudi().equalsIgnoreCase(batasStudiBaru.toString())) {
					if (simpan) {
						boolean closeSessionHere = false;
						Session localSession = session;
						if (localSession == null) {
							localSession = HibernateUtil.getSessionFactory().openSession();
							closeSessionHere = true;
						}
						try {
							if (localSession.contains(mahasiswa))
								localSession.refresh(mahasiswa);
							mahasiswa.setBatasStudi(batasStudiBaru.toString());

							Common.refreshUpdate(localSession, mahasiswa);
						} catch (Exception ex) {
							ex.printStackTrace(); ais.common.ErrorAuditUtil.record(ex, "auto-audit src/ais/action/master/helper/KegiatanHelper.java:627");
						} finally {
							if (closeSessionHere) {
								closeLocalSessionSafely(localSession);
							}
						}
					} else {
						mahasiswa.setBatasStudi(batasStudiBaru.toString());
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/KegiatanHelper.java:639");
		}
	}

	/** Sinonim {@code checkKegiatanMahasiswa(null, jenisKegiatan, ..., jadwal, rst, false, item, session)} dengan {@code jadwal} sudah diketahui pemanggil dan tanpa {@code Kegiatan} existing yang diketahui. */
	public static Kegiatan checkKegiatanMahasiswa(JenisKegiatan jenisKegiatan, Mahasiswa mahasiswa, Integer smt,
			String ta, Boolean hitungUlang, JadwalPembayaran jadwal, boolean rst, ItemBiaya item, Session session) {

		return checkKegiatanMahasiswa(null, jenisKegiatan, mahasiswa, smt, ta, hitungUlang, jadwal, rst, false, item,
				session);
	}

	/**
	 * Implementasi inti "get-or-create tagihan mahasiswa" — padanan
	 * {@link #checkKegiatanCalonMahasiswa(Kegiatan, JenisKegiatan, BiodataCalonMahasiswa, Integer, String, Boolean, JadwalPembayaran, boolean, boolean, ItemBiaya, Session)}
	 * untuk mahasiswa aktif, dengan alur yang sangat mirip PLUS dua perbedaan penting:
	 * <ul>
	 * <li><b>Pagar alumni</b>: bila mahasiswa sudah punya {@link Mahasiswa#getSemesterLulus()}
	 * dan {@code smt} melewatinya, {@link Kegiatan} baru TIDAK dibuat — KECUALI jenis kegiatan
	 * memang ditandai {@code tagihanJugaUntukAlumni} (mis. tagihan Wisuda yang memang berlaku
	 * setelah lulus).</li>
	 * <li><b>Status mahasiswa pada Kegiatan baru</b> diambil dari {@code Common.currentStatusSp}
	 * (bukan hardcode AKTIF seperti pada versi calon mahasiswa), memakai mode Semester Pendek
	 * bila {@code jenisKegiatan.getUntukBayarSP()}.</li>
	 * </ul>
	 * Sama seperti versi calon mahasiswa: {@code rst=true} menghapus {@link DetailKegiatan} lama
	 * yang masih bebas via SQL langsung; hitung ulang total tagihan lewat
	 * {@link #dataTagihanMahasiswa}; kegagalan simpan karena constraint violation diselesaikan
	 * dengan mengambil ulang baris pemenang race. SETELAH kegiatan diproses, bila
	 * {@code jenisKegiatan.getDigunakanSyaratKeaktifan()} dan konfigurasi
	 * {@code mhs_all_lambat_bayar_langsung_tidak_aktif} aktif, {@link #updateBatasStudiMahasiswa}
	 * dipanggil untuk mencatat batas studi — TAPI status {@link HistoryStatusMahasiswa} itu
	 * sendiri SENGAJA TIDAK diubah di sini (lihat komentar inline): keputusan status harus
	 * berdasarkan SELURUH tagihan bersyarat-aktif dari DB, bukan satu {@code Kegiatan} saja,
	 * sehingga didelegasikan ke {@code AuditListener} setelah commit.
	 *
	 * @param kegiatan               baris existing bila sudah diketahui, atau {@code null} untuk dicari
	 * @param jenisKegiatan          jenis kegiatan/tagihan
	 * @param mahasiswa              mahasiswa target
	 * @param smt                    semester tagihan
	 * @param ta                     tahun akademik
	 * @param hitungUlang            {@code true} untuk memicu hitung ulang meski {@code jadwal} kosong
	 * @param jadwal                 jadwal pembayaran; {@code null} berarti baris baru tidak akan dibuat
	 * @param rst                    {@code true} untuk menghapus detail lama yang masih bebas sebelum hitung ulang
	 * @param diubahSaatpembayaran   saat {@code rst=true} tanpa {@code item}, batasi penghapusan ke item yang nilainya tidak bisa diubah manual
	 * @param item                   batasi operasi ke satu {@link ItemBiaya}, atau {@code null} untuk semua
	 * @param session                session Hibernate aktif
	 * @return {@link Kegiatan} yang sudah ada/baru dibuat, atau {@code kegiatan} apa adanya bila dilewati aturan alumni/jadwal kosong
	 */
	public static Kegiatan checkKegiatanMahasiswa(Kegiatan kegiatan, JenisKegiatan jenisKegiatan, Mahasiswa mahasiswa,
			Integer smt, String ta, Boolean hitungUlang, JadwalPembayaran jadwal, boolean rst,
			boolean diubahSaatpembayaran, ItemBiaya item, Session session) {

		if (jadwal != null || hitungUlang) {
			if (kegiatan == null || kegiatan.getId() == null) {
				kegiatan = mahasiswa.ambilKegiatans(smt, jenisKegiatan, true);
			}

			if (kegiatan != null && kegiatan.getId() != null && rst) {
				Kegiatan.batalkanCacheDetailKegiatan(kegiatan.getId());

				java.util.HashMap<String, Object> params = new java.util.HashMap<String, Object>();
				params.put("kegId", kegiatan.getId());

				String sql;
				if (item != null) {
					sql = "delete from detail_kegiatan where kegiatan = :kegId "
							+ "and (menggunakandendacustom is null or menggunakandendacustom=false) "
							+ "and (batalkandenda is null or batalkandenda=false) "
							+ "and posting_history is null and kunci is null " + "and item_biaya = :itemId "
							+ "and (bukantagihan is null or bukantagihan=false)";
					params.put("itemId", item.getId());
				} else {
					sql = "delete from detail_kegiatan where kegiatan = :kegId "
							+ "and (menggunakandendacustom is null or menggunakandendacustom=false) "
							+ "and (batalkandenda is null or batalkandenda=false) "
							+ "and posting_history is null and kunci is null "
							+ "and (bukantagihan is null or bukantagihan=false) "
							+ (diubahSaatpembayaran
									? "and item_biaya in (select id from item_biaya where nilaibisadiubah = false)"
									: "");
				}
				System.out.println("Hapus -> " + sql);
				executeUpdateSafe(session, sql, params);
			}

			// Jangan buat Kegiatan baru bila semester melewati semester lulus mahasiswa,
			// kecuali jenis kegiatan memang diperuntukkan alumni (flag tagihanJugaUntukAlumni).
			if (jadwal != null && (kegiatan == null || kegiatan.getId() == null) && mahasiswa != null) {
				try {
					Integer smtLulusMhs = mahasiswa.getSemesterLulus();
					if (smtLulusMhs != null && smtLulusMhs > 0 && smt > smtLulusMhs
							&& (jenisKegiatan == null
									|| !Boolean.TRUE.equals(jenisKegiatan.getTagihanJugaUntukAlumni()))) {
						return kegiatan; // skip — jangan buat Kegiatan melewati semester lulus
					}
				} catch (Exception eignore) { ais.common.ErrorAuditUtil.record(eignore, "auto-audit(empty-catch) src/ais/action/master/helper/KegiatanHelper.java:699");
				}
			}

			if (jadwal != null && (kegiatan == null || kegiatan.getId() == null)) {
				kegiatan = new Kegiatan();
				kegiatan.setAmount(0.0);
				kegiatan.setJadwalPembayaran(jadwal);
				kegiatan.setMahasiswa(mahasiswa);
				kegiatan.setTahunAkademik(ta);
				kegiatan.setSemster(smt);
				kegiatan.setProgram(mahasiswa.getProgram());
				StatusMahasiswa statusMahasiswa = Common.currentStatusSp(mahasiswa,
						jenisKegiatan != null && jenisKegiatan.getUntukBayarSP() ? Perkuliahan.SEMESTER_PENDEK : null)
						.getStatusMahasiswa();
				statusMahasiswa = PembayaranUtilHelper.statusMahasiswaPembayaranEfektif(statusMahasiswa);

				kegiatan.setTanggal(jadwal.getStartDate());
				kegiatan.setValidated(1);
				kegiatan.setStatusMahasiswa(statusMahasiswa);
				kegiatan.setJenisKegiatan(jenisKegiatan);
				kegiatan.setDenda(0.0);
				kegiatan.setJumlahTelahDibayar(0.0);

				try {
					saveEntitySafe(session, kegiatan);

					Double totalTagihan = KegiatanHelper.dataTagihanMahasiswa(mahasiswa, smt, jenisKegiatan, session,
							kegiatan, rst, hitungUlang, null);
					kegiatan.setAmountTerhutang(totalTagihan);

					updateEntitySafe(session, kegiatan);
				} catch (Exception e) {
					try {
						String kodeunikRetry = Kegiatan.generateKodeUnik(mahasiswa, null, jenisKegiatan, smt, "",
								null);
						Kegiatan kegiatanSudahAda = ambilKegiatanKodeunikTerisolasi(kodeunikRetry);
						if (kegiatanSudahAda != null) {
							kegiatan = kegiatanSudahAda;
						} else {
							Common.tampilErrorJikaAdmin(e);
							ais.common.ErrorAuditUtil.record(e,
									"checkKegiatanMahasiswa: gagal simpan Kegiatan baru untuk mahasiswa="
											+ (mahasiswa == null ? "null" : mahasiswa.getId()));
						}
					} catch (Exception eRetry) {
						Common.tampilErrorJikaAdmin(e);
						ais.common.ErrorAuditUtil.record(eRetry,
								"checkKegiatanMahasiswa: gagal ambil ulang Kegiatan setelah constraint violation");
					}
				}

			} else if (kegiatan != null && hitungUlang) {
				try {
					if (session != null && session.isOpen() && session.contains(kegiatan))
						session.refresh(kegiatan);
					mahasiswa.populateKegiatan(kegiatan.getId());
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/KegiatanHelper.java:739");
				}

				// Sama dengan alur calon mahasiswa: rekap ikut disimpan oleh transaksi pemanggil,
				// sehingga tidak ada transaksi terisolasi yang berebut lock baris Kegiatan sendiri.
				List<CicilanPembayaran> cicilanPembayarans = KegiatanPersistenceHelper.ambilCicilan(kegiatan,
						hitungUlang, false);

				Double amountTotal = 0.0;
				Double denda = 0.0;
				kegiatan.resetBulans();
				Date tglMax = null;
				Date tglMin = null;

				for (CicilanPembayaran cicilanPembayaran : cicilanPembayarans) {
					if (cicilanPembayaran != null && cicilanPembayaran.getId() != null
							&& cicilanPembayaran.getKegiatan() != null
							&& kegiatan.getId().equals(cicilanPembayaran.getKegiatan().getId())) {
						if (cicilanPembayaran.getNilai() > 0.0 || !kegiatan.getJenisKegiatan().getAbaikanNilaiMinus()) {
							amountTotal += cicilanPembayaran.getNilai();
							denda += cicilanPembayaran.getDenda();
						}
					}
					if (cicilanPembayaran != null && cicilanPembayaran.getNilai().intValue() != 0) {
						if (tglMax == null || cicilanPembayaran.getTanggal().after(tglMax)) {
							tglMax = cicilanPembayaran.getTanggal();
						}
						if (tglMin == null || cicilanPembayaran.getTanggal().before(tglMin)) {
							tglMin = cicilanPembayaran.getTanggal();
						}
					}
				}

				kegiatan.setTanggalBayarAwal(tglMin);
				kegiatan.setTanggalBayarTerakhir(tglMax);
				kegiatan.setDenda(denda);
				kegiatan.setAmount(amountTotal);
				kegiatan.setJumlahTelahDibayar(amountTotal);

				try {
					Double totaltagihan = KegiatanHelper.dataTagihanMahasiswa(mahasiswa, smt, jenisKegiatan, session,
							kegiatan, rst, hitungUlang, cicilanPembayarans);
					kegiatan.setAmountTerhutang(totaltagihan - amountTotal);

					updateEntitySafe(session, kegiatan);
				} catch (Exception e) {
					// FIX (KE-32): sama seperti blok simpan Kegiatan baru di atas -- catat agar
					// gagal update (mis. constraint violation) tidak hilang tanpa jejak.
					Common.tampilErrorJikaAdmin(e);
					ais.common.ErrorAuditUtil.record(e,
							"checkKegiatanMahasiswa: gagal update Kegiatan untuk mahasiswa="
									+ (mahasiswa == null ? "null" : mahasiswa.getId()));
				}
			}

			if (kegiatan != null && kegiatan.getId() != null && kegiatan.getJenisKegiatan() != null
					&& kegiatan.getJenisKegiatan().getDigunakanSyaratKeaktifan()) {
				Konfigurasi konfigLambat = Common.getKonfigurasi("mhs_all_lambat_bayar_langsung_tidak_aktif", "",
						kegiatan.getSemster(), mahasiswa.getTahunangkatan(), mahasiswa.getJurusan(),
						mahasiswa.getProgram(), mahasiswa.getStatusAwalMahasiswa());

				boolean terlambarLangsungTidakAktif = konfigLambat != null
						&& Konfigurasi.AKTIF.equals(konfigLambat.getNilai());

				if (terlambarLangsungTidakAktif) {
					double harusLunas = 0.1;
					boolean checkStatusPembayaranMahasiswa = (kegiatan.getPersentaseLunas() >= harusLunas)
							|| Common.checkBaypassStatusPembayaranMahasiswa(kegiatan.getSemster(), null, mahasiswa,
									kegiatan.getJenisKegiatan());

					KegiatanHelper.updateBatasStudiMahasiswa(mahasiswa, session, kegiatan.getSemster(),
							checkStatusPembayaranMahasiswa);
					/*
					 * Status tidak boleh diputuskan dari satu Kegiatan dan tidak boleh hanya
					 * dimutasi pada object cache. AuditListener akan menghitung seluruh tagihan
					 * bersyarat-aktif dari DB dan menyimpan HistoryStatusMahasiswa setelah commit.
					 */
				}
			}
		}
		return kegiatan;
	}

	/**
	 * Menghitung total tagihan ({@code amountTerhutang}) sebuah {@link Kegiatan} milik calon
	 * mahasiswa, DAN sebagai efek samping menulis/memelihara rincian {@link DetailKegiatan}-nya
	 * (satu per {@link DetailBiaya} atau per bulan {@link PengaturanPembayaranBulanan}). Sumber
	 * item biaya: untuk jenis "Pendaftaran Ulang Mahasiswa Baru"/"Pendaftaran Calon Mahasiswa"
	 * dipetakan dari {@code PembayaranUtilHelper.getDetailBiayaCalonMahasiswa} (prodi lulus bila
	 * ada, jika tidak prodi pilihan pertama); jenis lain tidak menghasilkan tagihan (fungsi ini
	 * memang khusus dua jenis kegiatan pendaftaran calon mahasiswa tsb — {@code mydetailBiayas}
	 * tetap {@code null} untuk jenis lain). Bila ada pengaturan tagihan bulanan
	 * ({@code countPengaturanBulanan > 0}), sumber diganti ke
	 * {@code PembayaranUtil.getPengaturanPembayaranSemua} (pecahan per bulan, bukan nominal
	 * gabungan). {@link #lindungiKonfigurasiBulananSaatHitungUlang} dipanggil agar entity
	 * {@link PengaturanPembayaranBulanan} yang hanya dibaca tidak memicu Envers audit palsu.
	 * <p>
	 * Bila {@code cicilanPembayarans} disediakan, method ini JUGA mencocokkan cicilan yang sudah
	 * dibayar ke {@link DetailKegiatan}/{@link DetailBiaya} yang sesuai (mengisi
	 * {@code bayarKe}/{@code detailBiaya}/{@code tanggalTagihan} pada {@link CicilanPembayaran}
	 * bila belum terisi atau tidak sinkron) — efek samping tulis ke DB lewat
	 * {@link #updateEntitySafe}, ditelan diam-diam bila gagal (tidak menggagalkan penghitungan
	 * total). Untuk tiap item biaya/pengaturan bulanan: {@link DetailKegiatan} yang belum ada
	 * dibuat &amp; disimpan ({@link #saveEntitySafe}, gagal-simpan dilewati dengan {@code continue}
	 * tanpa menggagalkan item lain); item bertanda {@code bukanTagihan} dilewati dari total. Bila
	 * {@code rst=true}, diskon dihitung ({@code Kegiatan.hitungDiskon}) dan dikurangkan dari
	 * nominal per-item sebelum dijumlahkan (nilai "hasil reset ke billing"); bila {@code false},
	 * dipakai {@code Kegiatan.ambilJumlahTagihan} (nilai tersimpan/sudah disesuaikan).
	 * <p>
	 * <b>Kuirk penting</b> (lihat komentar inline "FIX Illegal attempt..."): helper bersarang di
	 * sini (mis. {@code PembayaranUtilHelper.countBulanan}) dapat menutup lalu membuka ulang
	 * native session ThreadLocal secara internal; karena Java pass-by-value, variabel
	 * {@code session} LOKAL milik pemanggil ({@link #checkKegiatanCalonMahasiswa}) tidak ikut
	 * berubah, jadi setelah memanggil method ini pemanggil WAJIB menyegarkan variabel session-nya
	 * lewat {@code HibernateUtil.ensureOpenSession(session)} sebelum dipakai lagi.
	 *
	 * @param biodataCalonMahasiswa calon mahasiswa target
	 * @param smt                   semester tagihan
	 * @param jenisKegiatan         jenis kegiatan (menentukan sumber item biaya, lihat catatan di atas)
	 * @param session               session Hibernate aktif (bisa berbeda dari yang dipegang pemanggil setelah return — lihat catatan kuirk)
	 * @param kegiatan              kegiatan yang detailnya dihitung/ditulis
	 * @param rst                   {@code true} untuk menghitung ulang dengan diskon dari billing (bukan nilai tersimpan)
	 * @param ulang                 diteruskan ke {@code kegiatan.ambilDetailKegiatan(ulang)} — memaksa reload detail dari DB
	 * @param cicilanPembayarans    cicilan yang sudah dibayar untuk dicocokkan-ulang ke detail (boleh {@code null})
	 * @return total tagihan ({@code amountTerhutang}) hasil penjumlahan seluruh item/bulan yang berlaku
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static Double dataTagihanCalonMahasiswa(BiodataCalonMahasiswa biodataCalonMahasiswa, int smt,
			JenisKegiatan jenisKegiatan, Session session, Kegiatan kegiatan, boolean rst, boolean ulang,
			List<CicilanPembayaran> cicilanPembayarans) {

		Double totalTagihan = 0.0;
		Collection mydetailBiayas = null;

		if (jenisKegiatan.getId().equals(ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU.getId())) {
			mydetailBiayas = PembayaranUtilHelper.getDetailBiayaCalonMahasiswa(biodataCalonMahasiswa, jenisKegiatan,
					biodataCalonMahasiswa.getProdiLulus() == null ? biodataCalonMahasiswa.getProdi1()
							: biodataCalonMahasiswa.getProdiLulus(),
					smt, true);
		} else if (jenisKegiatan.getId().equals(ConstantValues.PENDAFTARAN_CALON_MAHASISWA.getId())) {
			mydetailBiayas = PembayaranUtilHelper.getDetailBiayaCalonMahasiswa(biodataCalonMahasiswa, jenisKegiatan,
					biodataCalonMahasiswa.getProdiLulus() == null ? biodataCalonMahasiswa.getProdi1()
							: biodataCalonMahasiswa.getProdiLulus(),
					true);
		}

		// Helper bersarang di atas dapat menutup native session ThreadLocal -> ambil ulang yang
		// DIJAMIN open sebelum dipakai (currentNativeSession self-healing).
		session = HibernateUtil.currentNativeSession();
		int countPengaturanBulanan = PembayaranUtilHelper.countBulanan(session, biodataCalonMahasiswa, jenisKegiatan,
				smt, mydetailBiayas, true, true);

		if (countPengaturanBulanan > 0) {
			// countBulanan di atas dapat menutup native session -> ambil ulang sebelum dipakai.
			session = HibernateUtil.currentNativeSession();
			mydetailBiayas = PembayaranUtil.getInstance().getPengaturanPembayaranSemua(biodataCalonMahasiswa, session,
					smt, kegiatan.getJenisKegiatan(), mydetailBiayas, true, true);
		}

		if (mydetailBiayas != null) {
			Collection<DetailKegiatan> detailKegiatans = kegiatan.ambilDetailKegiatan(ulang);
			lindungiKonfigurasiBulananSaatHitungUlang(session, detailKegiatans, mydetailBiayas);

			if (cicilanPembayarans != null) {
				for (DetailKegiatan detailKegiatan : detailKegiatans) {
					for (CicilanPembayaran cicilanPembayaran : cicilanPembayarans) {
						PengaturanPembayaranBulanan pengaturanPembayaranBulanan = detailKegiatan
								.getPengaturanPembayaranBulanan();

						if (pengaturanPembayaranBulanan == null) {
							if (cicilanPembayaran.getDetailBiaya() == null && detailKegiatan.getDetailBiaya() != null) {
								DetailBiaya detailBiaya = detailKegiatan.getDetailBiaya();
								if (detailBiaya != null && cicilanPembayaran.getItemBiaya() != null && cicilanPembayaran
										.getItemBiaya().getId().equals(detailBiaya.getItemBiaya().getId())) {
									Double jumlah = Kegiatan.ambilJumlahTagihan(kegiatan, detailBiaya);
									if (jumlah.intValue() == cicilanPembayaran.getNilai().intValue()) {
										cicilanPembayaran.setBayarKe(detailBiaya.getBayarKe());
										cicilanPembayaran.setDetailBiaya(detailBiaya);
										try {
											updateEntitySafe(session, cicilanPembayaran);
										} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/KegiatanHelper.java:875");
										}
									}
								}
							}
						}

						if ((pengaturanPembayaranBulanan != null && cicilanPembayaran.getItemBiaya() != null
								&& cicilanPembayaran.getItemBiaya().getId()
										.equals(pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getId())
								&& cicilanPembayaran.getPengaturanPembayaranBulanan() != null
								&& cicilanPembayaran.getPengaturanPembayaranBulanan().getRealBulan()
										.equals(pengaturanPembayaranBulanan.getRealBulan()))
								|| (detailKegiatan.getItemBiaya() != null && cicilanPembayaran.getItemBiaya() != null
										&& cicilanPembayaran.getItemBiaya().getId()
												.equals(detailKegiatan.getItemBiaya().getId()))) {

							if (cicilanPembayaran.getId() != null && detailKegiatan.getTanggal() != null) {
								if (cicilanPembayaran.getTanggalTagihan() == null || !Common.dateFormat83.get()
										.format(cicilanPembayaran.getTanggalTagihan())
										.equals(Common.dateFormat83.get().format(detailKegiatan.getTanggal()))) {
									cicilanPembayaran.setTanggalTagihan(detailKegiatan.getTanggal());
									try {
										updateEntitySafe(session, cicilanPembayaran);
									} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/KegiatanHelper.java:899");
									}
								}
							}
						}
					}
				}
			}

			for (Object o : mydetailBiayas) {
				DetailKegiatan tempdata = null;
				DetailBiaya tempdetailBiaya = null;
				PengaturanPembayaranBulanan temppengaturanPembayaranBulanan = null;

				if (o instanceof DetailBiaya) {
					tempdetailBiaya = (DetailBiaya) o;
					if (kegiatan != null && kegiatan.getId() != null && detailKegiatans != null) {
						for (DetailKegiatan temp : detailKegiatans) {
							if (temp != null && temp.getKegiatan() != null && temp.getDetailBiaya() != null) {
								if (kegiatan.getId().equals(temp.getKegiatan().getId())
										&& tempdetailBiaya.getId().equals(temp.getDetailBiaya().getId())) {
									tempdata = temp;
									break;
								}
							}
						}
					}
				} else if (o instanceof PengaturanPembayaranBulanan) {
					temppengaturanPembayaranBulanan = (PengaturanPembayaranBulanan) o;
					if (temppengaturanPembayaranBulanan != null) {
						tempdetailBiaya = temppengaturanPembayaranBulanan.getDetailBiaya();
					}
					if (kegiatan != null && kegiatan.getId() != null && detailKegiatans != null) {
						for (DetailKegiatan temp : detailKegiatans) {
							if (temp != null && temp.getKegiatan() != null
									&& temp.getPengaturanPembayaranBulanan() != null) {
								if (kegiatan.getId().equals(temp.getKegiatan().getId())
										&& temppengaturanPembayaranBulanan.getId()
												.equals(temp.getPengaturanPembayaranBulanan().getId())) {
									tempdata = temp;
									break;
								}
							}
						}
					}
				}

				if (temppengaturanPembayaranBulanan != null || tempdetailBiaya != null) {
					if (kegiatan != null && kegiatan.getId() != null && tempdata == null) {
						tempdata = temppengaturanPembayaranBulanan != null
								? kegiatan.ambilSatuDetailKegiatan(temppengaturanPembayaranBulanan, detailKegiatans,
										session)
								: kegiatan.ambilSatuDetailKegiatan(tempdetailBiaya, session);

						if (tempdata == null) {
							tempdata = new DetailKegiatan();
							tempdata.setPengaturanPembayaranBulanan(temppengaturanPembayaranBulanan);
							tempdata.setUraian("");
							tempdata.setDetailBiaya(tempdetailBiaya);
							tempdata.setKeterangan(tempdetailBiaya == null ? "" : tempdetailBiaya.getKeterangan());
							tempdata.setKegiatan(kegiatan);

							try {
								saveEntitySafe(session, tempdata);
							} catch (Exception e) {
								// Error diabaikan HANYA SETELAH di-evict dengan aman dari method
								// saveEntitySafe.
								// Jadi tidak menyebabkan AssertionFailure di baris selanjutnya
								continue;
							}
						}
					}
				}

				DetailKegiatan detailKegiatan = tempdata;
				if (detailKegiatan != null && detailKegiatan.getBukanTagihan() != null
						&& detailKegiatan.getBukanTagihan()) {
					continue;
				}

				if (o instanceof DetailBiaya) {
					DetailBiaya detailBiaya = (DetailBiaya) o;
					// Pastikan hasil perkalian ("x N matakuliah/SKS") sudah dihitung lebih dulu
					// agar item dgn hasil 0 bernilai 0 (50.000 x 0 = 0), bukan memakai nilai
					// per-unit (getNilaiBiaya) yang tampil dicoret.
					if (kegiatan != null && kegiatan.getMahasiswa() != null && detailBiaya.getItemBiaya() != null
							&& !detailBiaya.getItemBiaya().getPenghitungan().equals(ItemBiaya.TIDAK_ADA_PENGHITUNGAN)
							&& detailBiaya.getNilaiBiayaBaru() == null) {
						try {
							detailBiaya.updateKeterangan(kegiatan.getMahasiswa(), kegiatan.getSemster());
						} catch (Exception eUpd) { ais.common.ErrorAuditUtil.record(eUpd, "auto-audit(empty-catch) src/ais/action/master/helper/KegiatanHelper.java:989");
						}
					}
					Double jml = (detailBiaya.getNilaiBiayaBaru() == null ? detailBiaya.getNilaiBiaya()
							: detailBiaya.getNilaiBiayaBaru());
					if (rst) {
						Kegiatan.hitungDiskon(detailKegiatan, kegiatan, detailBiaya, jml);
						Double diskon = detailKegiatan == null ? 0.0 : detailKegiatan.getDiskon();
						jml = jml - diskon;
					}
					Double jumlah = rst ? jml
							: Kegiatan.ambilJumlahTagihan(detailKegiatan, kegiatan, detailBiaya, true);
					totalTagihan += jumlah;

				} else if (o instanceof PengaturanPembayaranBulanan) {
					PengaturanPembayaranBulanan pengaturanPembayaranBulanan = (PengaturanPembayaranBulanan) o;
					DetailBiaya detailBiaya = pengaturanPembayaranBulanan.getDetailBiaya();
					Double jml = pengaturanPembayaranBulanan.getNominal();
					if (rst) {
						Kegiatan.hitungDiskon(detailKegiatan, kegiatan, detailBiaya, jml);
						Double diskon = detailKegiatan == null ? 0.0 : detailKegiatan.getDiskon();
						jml = jml - diskon;
					}
					Double jumlah = rst ? jml
							: Kegiatan.ambilJumlahTagihan(detailKegiatan, detailBiaya, kegiatan, null, smt,
									pengaturanPembayaranBulanan);
					totalTagihan += jumlah;
				}

				if (detailKegiatan != null) {
					detailKegiatan.setKegiatan(kegiatan);
				}
			}
		}

		return totalTagihan;
	}

	/**
	 * Padanan {@link #dataTagihanCalonMahasiswa} untuk mahasiswa aktif — struktur dan efek samping
	 * (menulis/memelihara {@link DetailKegiatan}, mencocokkan {@link CicilanPembayaran}, melindungi
	 * {@link PengaturanPembayaranBulanan} dari Envers palsu) sama persis, dengan tiga perbedaan:
	 * <ul>
	 * <li><b>Pagar alumni</b>: bila {@code mahasiswa.getStatusKeluar()} terisi dan semester
	 * melewati {@code semesterLulus} (aturan beda tipis untuk status keluar id=1 vs. lainnya —
	 * lihat kondisi {@code alumniFilterBerlaku}), total tagihan langsung {@code 0.0} KECUALI jenis
	 * kegiatan bertanda {@code tagihanJugaUntukAlumni}.</li>
	 * <li>Sumber item biaya dari {@code PembayaranUtilHelper.getDetailBiayaMahasiswa} (bukan versi
	 * calon mahasiswa), menghormati flag {@code ulang} untuk reload — komentar inline mencatat ini
	 * PERBAIKAN dari perilaku lama yang hardcode {@code true} (memaksa
	 * {@code singkronkanKrsMahasiswa} + tulis MapDB di setiap panggilan walau pemanggil minta
	 * pakai cache).</li>
	 * <li>Nominal bulanan dihitung lewat
	 * {@code pengaturanPembayaranBulanan.ambilNominalModifikasi(mahasiswa, smt)} (bisa
	 * dimodifikasi per-mahasiswa), bukan {@code getNominal()} polos seperti versi calon mahasiswa.</li>
	 * </ul>
	 * Tidak seperti versi calon mahasiswa yang cuma mem-{@code set} field pada
	 * {@code DetailKegiatan} baru, di sini {@link DetailKegiatan} yang SUDAH ADA tapi
	 * {@code detailBiaya}/{@code pengaturanPembayaranBulanan}-nya berbeda dari item saat ini JUGA
	 * diupdate ({@link #updateEntitySafe}) — menjaga rincian tetap sinkron dengan
	 * {@code SettingBiaya} terbaru, bukan hanya membuat baris baru.
	 *
	 * @param mahasiswa           mahasiswa target
	 * @param smt                 semester tagihan
	 * @param jenisKegiatan       jenis kegiatan (dipakai untuk cek {@code tagihanJugaUntukAlumni})
	 * @param session             session Hibernate aktif
	 * @param kegiatan            kegiatan yang detailnya dihitung/ditulis
	 * @param rst                 {@code true} untuk menghitung ulang dengan diskon dari billing
	 * @param ulang               diteruskan ke reload detail biaya &amp; {@code kegiatan.ambilDetailKegiatan(ulang)}
	 * @param cicilanPembayarans  cicilan yang sudah dibayar untuk dicocokkan-ulang ke detail (boleh {@code null})
	 * @return total tagihan, atau {@code 0.0} bila pagar alumni berlaku
	 */
	@SuppressWarnings("rawtypes")
	public static Double dataTagihanMahasiswa(Mahasiswa mahasiswa, int smt, JenisKegiatan jenisKegiatan,
			Session session, Kegiatan kegiatan, boolean rst, boolean ulang,
			List<CicilanPembayaran> cicilanPembayarans) {

		Double totalTagihan = 0.0;

		// Alumni filter: bila mahasiswa sudah keluar/lulus dan semester melewati semester lulus,
		// tagihan = 0 — KECUALI jenis kegiatan khusus alumni (mis. Wisuda, tagihanJugaUntukAlumni=true).
		boolean alumniFilterBerlaku = mahasiswa.getStatusKeluar() != null
				&& ((mahasiswa.getStatusKeluar().getId().equals(1L)
						&& mahasiswa.getSemesterLulus() != null && mahasiswa.getSemesterLulus() < smt)
						|| (!mahasiswa.getStatusKeluar().getId().equals(1L)
								&& mahasiswa.getSemesterLulus() != null && mahasiswa.getSemesterLulus() <= smt))
				&& (jenisKegiatan == null || !Boolean.TRUE.equals(jenisKegiatan.getTagihanJugaUntukAlumni()));
		if (alumniFilterBerlaku) {
			totalTagihan = 0.0;
		} else {
			// Hormati flag 'ulang' (=hitungUlang dari caller) untuk reload biaya — konsisten dengan
			// kegiatan.ambilDetailKegiatan(ulang) di bawah. Dulu di-hardcode true → memaksa
			// singkronkanKrsMahasiswa + tulis MapDB tiap panggil walau caller minta pakai cache.
			Collection mydetailBiayas = PembayaranUtilHelper.getDetailBiayaMahasiswa(mahasiswa, smt, jenisKegiatan,
					ulang);
			int countPengaturanBulanan = PembayaranUtilHelper.countBulanan(session, mahasiswa, jenisKegiatan, smt,
					mydetailBiayas, true, true);

			if (countPengaturanBulanan > 0) {
				mydetailBiayas = PembayaranUtilHelper.getDetailBiayaMahasiswa(mahasiswa, smt, jenisKegiatan, "-1", true,
						ulang);
			}

			Collection<DetailKegiatan> detailKegiatans = kegiatan.ambilDetailKegiatan(ulang);
			lindungiKonfigurasiBulananSaatHitungUlang(session, detailKegiatans, mydetailBiayas);

			if (cicilanPembayarans != null) {
				for (DetailKegiatan detailKegiatan : detailKegiatans) {
					for (CicilanPembayaran cicilanPembayaran : cicilanPembayarans) {
						PengaturanPembayaranBulanan pengaturanPembayaranBulanan = detailKegiatan
								.getPengaturanPembayaranBulanan();

						if (pengaturanPembayaranBulanan == null) {
							if (cicilanPembayaran.getDetailBiaya() == null && detailKegiatan.getDetailBiaya() != null) {
								DetailBiaya detailBiaya = detailKegiatan.getDetailBiaya();
								if (detailBiaya != null && cicilanPembayaran.getItemBiaya() != null && cicilanPembayaran
										.getItemBiaya().getId().equals(detailBiaya.getItemBiaya().getId())) {
									Double jumlah = Kegiatan.ambilJumlahTagihan(kegiatan, detailBiaya);
									if (jumlah.intValue() == cicilanPembayaran.getNilai().intValue()) {
										cicilanPembayaran.setBayarKe(detailBiaya.getBayarKe());
										cicilanPembayaran.setDetailBiaya(detailBiaya);
										try {
											updateEntitySafe(session, cicilanPembayaran);
										} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/KegiatanHelper.java:1077");
										}
									}
								}
							}
						}

						if ((pengaturanPembayaranBulanan != null && cicilanPembayaran.getItemBiaya() != null
								&& cicilanPembayaran.getItemBiaya().getId()
										.equals(pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getId())
								&& cicilanPembayaran.getPengaturanPembayaranBulanan() != null
								&& cicilanPembayaran.getPengaturanPembayaranBulanan().getRealBulan()
										.equals(pengaturanPembayaranBulanan.getRealBulan()))
								|| (detailKegiatan.getItemBiaya() != null && cicilanPembayaran.getItemBiaya() != null
										&& cicilanPembayaran.getItemBiaya().getId()
												.equals(detailKegiatan.getItemBiaya().getId()))) {

							if (cicilanPembayaran.getId() != null && detailKegiatan.getTanggal() != null) {
								if (cicilanPembayaran.getTanggalTagihan() == null || !Common.dateFormat83.get()
										.format(cicilanPembayaran.getTanggalTagihan())
										.equals(Common.dateFormat83.get().format(detailKegiatan.getTanggal()))) {
									cicilanPembayaran.setTanggalTagihan(detailKegiatan.getTanggal());
									try {
										updateEntitySafe(session, cicilanPembayaran);
									} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/KegiatanHelper.java:1101");
									}
								}
							}
						}
					}
				}
			}

			for (Object o : mydetailBiayas) {
				DetailKegiatan tempdata = null;
				DetailBiaya tempdetailBiaya = null;
				PengaturanPembayaranBulanan temppengaturanPembayaranBulanan = null;

				if (o instanceof DetailBiaya) {
					tempdetailBiaya = (DetailBiaya) o;
					if (kegiatan != null && kegiatan.getId() != null && detailKegiatans != null) {
						for (DetailKegiatan temp : detailKegiatans) {
							if (temp != null && temp.getKegiatan() != null && temp.getDetailBiaya() != null) {
								if (kegiatan.getId().equals(temp.getKegiatan().getId())
										&& tempdetailBiaya.getId().equals(temp.getDetailBiaya().getId())) {
									tempdata = temp;
									break;
								}
							}
						}
					}
				} else if (o instanceof PengaturanPembayaranBulanan) {
					temppengaturanPembayaranBulanan = (PengaturanPembayaranBulanan) o;
					if (temppengaturanPembayaranBulanan != null) {
						tempdetailBiaya = temppengaturanPembayaranBulanan.getDetailBiaya();
					}
					if (kegiatan != null && kegiatan.getId() != null && detailKegiatans != null) {
						for (DetailKegiatan temp : detailKegiatans) {
							if (temp != null && temp.getKegiatan() != null
									&& temp.getPengaturanPembayaranBulanan() != null) {
								if (kegiatan.getId().equals(temp.getKegiatan().getId())
										&& temppengaturanPembayaranBulanan.getId()
												.equals(temp.getPengaturanPembayaranBulanan().getId())) {
									tempdata = temp;
									break;
								}
							}
						}
					}
				}

				if (temppengaturanPembayaranBulanan != null || tempdetailBiaya != null) {
					if (kegiatan != null && kegiatan.getId() != null && tempdata == null) {
						tempdata = temppengaturanPembayaranBulanan != null
								? kegiatan.ambilSatuDetailKegiatan(temppengaturanPembayaranBulanan, detailKegiatans,
										session)
								: kegiatan.ambilSatuDetailKegiatan(tempdetailBiaya, session);

						if (tempdata == null) {
							tempdata = new DetailKegiatan();
							tempdata.setPengaturanPembayaranBulanan(temppengaturanPembayaranBulanan);
							tempdata.setUraian("");
							tempdata.setDetailBiaya(tempdetailBiaya);
							tempdata.setKeterangan(tempdetailBiaya == null ? "" : tempdetailBiaya.getKeterangan());
							tempdata.setKegiatan(kegiatan);

							try {
								saveEntitySafe(session, tempdata);
							} catch (Exception e) {
								continue; // Lanjut tanpa mencemari memori session
							}
						}
					}
				}

				DetailKegiatan detailKegiatan = tempdata;
				if (detailKegiatan != null && detailKegiatan.getBukanTagihan() != null
						&& detailKegiatan.getBukanTagihan()) {
					continue;
				}

				if (o instanceof DetailBiaya) {
					DetailBiaya detailBiaya = (DetailBiaya) o;

					if (detailBiaya != null && detailKegiatan != null && detailKegiatan.getDetailBiaya() != null
							&& !detailKegiatan.getDetailBiaya().getId().equals(detailBiaya.getId())) {
						detailKegiatan.setDetailBiaya(detailBiaya);
						detailKegiatan.setKeterangan(detailBiaya.getKeterangan());
						detailKegiatan.setKegiatan(kegiatan);

						try {
							updateEntitySafe(session, detailKegiatan);
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/KegiatanHelper.java:1189");
						}
					}

					if (ulang && kegiatan.getMahasiswa() != null && detailBiaya.getItemBiaya() != null
							&& !detailBiaya.getItemBiaya().getPenghitungan().equals(ItemBiaya.TIDAK_ADA_PENGHITUNGAN)) {
						detailBiaya.updateKeterangan(kegiatan.getMahasiswa(), kegiatan.getSemster());
					}

					Double jml = (detailBiaya.getNilaiBiayaBaru() == null ? detailBiaya.getNilaiBiaya()
							: detailBiaya.getNilaiBiayaBaru());
					if (rst) {
						Kegiatan.hitungDiskon(detailKegiatan, kegiatan, detailBiaya, jml);
						Double diskon = detailKegiatan == null ? 0.0 : detailKegiatan.getDiskon();
						jml = jml - diskon;
					}

					Double jumlah = rst ? jml
							: Kegiatan.ambilJumlahTagihan(detailKegiatan, kegiatan, detailBiaya, true);
					totalTagihan += jumlah;

				} else if (o instanceof PengaturanPembayaranBulanan) {
					PengaturanPembayaranBulanan pengaturanPembayaranBulanan = (PengaturanPembayaranBulanan) o;
					DetailBiaya detailBiaya = pengaturanPembayaranBulanan.getDetailBiaya();

					if (pengaturanPembayaranBulanan != null && detailKegiatan != null
							&& (detailKegiatan.getPengaturanPembayaranBulanan() == null
									|| !detailKegiatan.getPengaturanPembayaranBulanan().getId()
											.equals(pengaturanPembayaranBulanan.getId()))) {
						detailKegiatan.setPengaturanPembayaranBulanan(pengaturanPembayaranBulanan);
						detailKegiatan.setDetailBiaya(detailBiaya);
						detailKegiatan.setKegiatan(kegiatan);

						try {
							updateEntitySafe(session, detailKegiatan);
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/KegiatanHelper.java:1224");
						}
					}

					Double jml = pengaturanPembayaranBulanan.ambilNominalModifikasi(mahasiswa, smt);
					if (rst) {
						Kegiatan.hitungDiskon(detailKegiatan, kegiatan, detailBiaya, jml);
						Double diskon = detailKegiatan == null ? 0.0 : detailKegiatan.getDiskon();
						jml = jml - diskon;
					}

					Double jumlah = rst ? jml
							: Kegiatan.ambilJumlahTagihan(detailKegiatan, detailBiaya, kegiatan, mahasiswa, smt,
									pengaturanPembayaranBulanan);
					totalTagihan += jumlah;
				}
				
				
				if (detailKegiatan != null) {
					detailKegiatan.setKegiatan(kegiatan);
				}
			}
		}

		return totalTagihan;
	}

	/**
	 * Membangun tombol toolbar ZK ({@link MyToolbarbuttonConfig}) ber-upload Excel untuk MENGUBAH
	 * {@link DetailKegiatan} secara massal — pasangan sisi-impor dari
	 * {@link #doDownloadTagihan}/{@link #prosesDownloadTagihan}: admin download tagihan ke
	 * .xlsx (kolom "ID TAGIHAN" terkunci berisi id {@link DetailKegiatan}), mengedit kolom
	 * "NOMINAL TAGIHAN"/"TANGGAL TAGIHAN"/"KUNCI" yang tidak terkunci, lalu upload kembali file
	 * yang sama lewat tombol ini.
	 * <p>
	 * Saat file diupload (harus {@code .xlsx}, divalidasi lewat
	 * {@code AmbilDataTugasFileContent.checkFile}): file disimpan sementara ke
	 * {@code /temp/<namaFile>}, dibaca baris demi baris (kolom 7=id, 9=nilai, 10=tanggal,
	 * 11=kunci) — untuk tiap {@link DetailKegiatan} yang ditemukan, {@code kunci} di-set/di-unset
	 * (ke {@link Tbmuser} pengguna saat ini bila dikunci), {@code biaya} dan {@code tanggal}
	 * diupdate bila diisi ({@link #updateEntitySafe}), lalu baris hasil ditulis ke workbook baru
	 * "HASIL UPLOAD" (kolom identik + status) yang otomatis di-download balik lewat
	 * {@link Filedownload}. Session di-{@code flush()}/{@code clear()} tiap 50 baris untuk
	 * membatasi memory pada upload besar. {@code eventListener} yang dioper pemanggil dipanggil
	 * di akhir (dengan {@code UploadEvent} asli) sebagai hook tambahan setelah proses selesai.
	 * File selain {@code .xlsx} ditolak dengan pesan error, tanpa memproses apa pun.
	 * <p>
	 * Setiap baris id yang dibaca WAJIB berada dalam cakupan {@code settingBiayaCakupan} (dicek
	 * lewat {@code detailKegiatan.getDetailBiaya().getSettingBiayaEfektif()}) sebelum ditulis;
	 * bila tidak, baris ditolak fail-closed dan dicatat di sheet hasil tanpa membocorkan data
	 * mahasiswa/calon mahasiswanya. Penulisan {@code biaya}/{@code tanggal}/{@code kunci} juga
	 * ditolak bila {@code itemBiaya.getNilaiBisaDiubah()} bukan {@code true} atau baris sudah
	 * {@code kunci} oleh siapa pun. Setiap baris id selalu berakhir dengan satu baris di sheet
	 * hasil berkolom STATUS (BERHASIL/DITOLAK/GAGAL) sehingga tidak ada perubahan yang senyap.
	 *
	 * @param buttonLabel         label tombol
	 * @param buttonImage         path ikon tombol
	 * @param settingBiayaCakupan {@link SettingBiaya} layar pemanggil; membatasi id
	 *                            {@link DetailKegiatan} yang boleh ditulis lewat upload ini
	 * @param eventListener       dipanggil di akhir proses upload sebagai hook tambahan pemanggil
	 * @return tombol toolbar siap dipasang ke UI
	 */
	public static MyToolbarbuttonConfig prosesUploadTagihan(String buttonLabel, String buttonImage,
			final SettingBiaya settingBiayaCakupan, final EventListener eventListener) {

		MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig(buttonLabel, buttonImage);
		toolbarbutton.setUpload(Common.ukuranFileUpload());
		toolbarbutton.addEventListener("onUpload", new EventListener() {

			/**
			 * Menangani event {@code onUpload} tombol: membaca file .xlsx hasil edit operator, menulis
			 * perubahan {@code biaya}/{@code tanggal}/{@code kunci} ke {@link DetailKegiatan} yang
			 * bersangkutan, lalu mengirim balik workbook "HASIL UPLOAD" berisi kondisi akhir tiap baris.
			 *
			 * <p><b>Urutan kerja.</b> (1) {@code media} divalidasi lewat
			 * {@code AmbilDataTugasFileContent.checkFile} dan ekstensi harus {@code .xlsx} — selain itu
			 * method langsung {@code return} tanpa memproses apa pun. (2) Stream upload disalin ke
			 * {@code /temp/&lt;namaFile&gt;} di direktori real webapp karena {@link XSSFWorkbook} di versi ini
			 * dibuka dari path file, bukan dari stream. (3) Workbook sumber dibaca baris demi baris mulai
			 * indeks 1 (baris 0 = header): kolom 7 = id {@link DetailKegiatan}, 9 = nominal, 10 = tanggal,
			 * 11 = kunci. (4) Tiap baris yang berhasil ditulis direkam ke workbook keluaran, yang kemudian
			 * disimpan sebagai {@code /temp/hasil_upload_&lt;namaFile&gt;} dan otomatis dikirim balik ke browser
			 * lewat {@link Filedownload}. (5) {@code eventListener} milik pemanggil dipanggil dengan
			 * {@link UploadEvent} asli sebagai hook penutup (di {@code DetailSettingBiayaAction} isinya
			 * {@code loadData} untuk menyegarkan grid), lalu kotak pesan "Upload tagihan telah selesai."
			 * ditampilkan.
			 *
			 * <p><b>Manajemen session &amp; memori.</b> Session Hibernate dibuka sendiri di sini
			 * ({@code HibernateUtil.getSessionFactory().openSession()}) — bukan session request — dan
			 * ditutup di {@code finally} lewat {@link #closeLocalSessionSafely}. Tiap 50 baris terproses
			 * session di-{@code flush()} lalu di-{@code clear()} supaya persistence context tidak
			 * menggelembung pada upload ribuan baris. Penulisan entity memakai {@link #updateEntitySafe}
			 * sehingga baris yang bentrok lock dengan batch "Proses Tagihan" tetap punya peluang retry di
			 * sesi terisolasi.
			 *
			 * <p><b>Ketahanan per baris.</b> Setiap baris dibungkus {@code try/catch} lebar: satu baris
			 * rusak (id tidak ditemukan, relasi null, tanggal tak terparse) tidak menggagalkan sisa file.
			 * Tanggal yang tidak terparse ditelan tersendiri (nominal tetap bisa tersimpan walaupun
			 * tanggalnya tidak), tetapi kegagalan TIDAK lagi senyap: setiap baris id selalu berakhir
			 * dengan satu baris di sheet hasil berkolom STATUS (lihat di bawah), termasuk baris yang
			 * gagal di {@code catch} terluar.
			 *
			 * <p><b>Gerbang integritas.</b> Sebelum {@code biaya}/{@code tanggal}/{@code kunci} ditulis:
			 * <ul>
			 * <li><b>Cakupan.</b> {@link DetailKegiatan} yang ditemukan lewat {@code Restrictions.idEq(id)}
			 * harus memiliki {@code getDetailBiaya().getSettingBiayaEfektif()} yang sama dengan
			 * {@code settingBiayaCakupan} milik layar pemanggil; bila tidak (termasuk bila salah satu sisi
			 * {@code null}), baris ditolak fail-closed dan HANYA id + STATUS yang dicatat di sheet hasil
			 * (tanpa data mahasiswa/calon mahasiswa) supaya jalur ini juga tidak jadi kebocoran data
			 * lintas fakultas/tenant.</li>
			 * <li><b>{@code nilaiBisaDiubah}.</b> {@code detailKegiatan.getItemBiaya()} (null-safe,
			 * termasuk untuk baris bulanan) harus ada dan {@code getNilaiBisaDiubah()}-nya {@code true}
			 * sebelum kolom 9/10/11 boleh ditulis; kalau tidak, baris ditolak dan nilai lama yang
			 * ditampilkan di sheet hasil.</li>
			 * <li><b>{@code kunci}.</b> Bila {@code getKunci() != null}, permintaan mengubah
			 * {@code biaya}/{@code tanggal} pada baris itu ditolak (operator harus membuka kunci lewat
			 * layar aslinya lebih dulu, sesuai pola {@code DetailPembayaranMahasiswaRenderer.tampilkanKunci}
			 * di tempat lain); baris yang tidak sedang dikunci tetap bisa dikunci/dibuka lewat kolom 11
			 * seperti sebelumnya.</li>
			 * </ul>
			 *
			 * @param event {@link UploadEvent} ZK yang membawa {@link Media} file .xlsx
			 * @throws Exception diteruskan dari pembacaan workbook, penulisan file hasil, atau dari {@code eventListener} pemanggil
			 */
			@Override
			public void onEvent(Event event) throws Exception {
				UploadEvent uploadEvent = (UploadEvent) event;
				Media media = uploadEvent.getMedia();
				if (!ais.action.master.helper.generic.AmbilDataTugasFileContent.checkFile(media)) {
					return;
				}

				if (media.getName().toLowerCase().endsWith("xlsx")) {
					InputStream inputStream = null;
					FileOutputStream fileOutputStream = null;
					File file = new File(Sessions.getCurrent().getWebApp().getRealPath("/temp/" + media.getName()));
					file.getParentFile().mkdirs();

					try {
						inputStream = media.getStreamData();
						fileOutputStream = new FileOutputStream(file);
						byte[] buffer = new byte[8192];
						int bytesRead;
						while ((bytesRead = inputStream.read(buffer)) != -1) {
							fileOutputStream.write(buffer, 0, bytesRead);
						}
					} finally {
						if (fileOutputStream != null) {
							try {
								fileOutputStream.close();
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/KegiatanHelper.java:1284");
							}
						}
						if (inputStream != null) {
							try {
								inputStream.close();
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/KegiatanHelper.java:1290");
							}
						}
					}

					Tbmuser tbmuser = Common.getCurrentUser();

					XSSFWorkbook workbookUpload = new XSSFWorkbook(file.getAbsolutePath());
					XSSFSheet sheetUpload = workbookUpload.getSheetAt(0);
					int size = sheetUpload.getLastRowNum() + 1;

					XSSFWorkbook workbook = new XSSFWorkbook();
					XSSFSheet sheet = workbook.createSheet("HASIL UPLOAD");
					sheet.setDefaultColumnWidth(20);

					XSSFRow rowhead = sheet.createRow((short) 0);
					rowhead.createCell(0).setCellValue("NIM/NO REG");
					rowhead.createCell(1).setCellValue("NAMA");
					rowhead.createCell(2).setCellValue("JENIS PEMBAYARAN");
					rowhead.createCell(3).setCellValue(Common.getBahasaConfig("FAKULTAS"));
					rowhead.createCell(4).setCellValue(Common.getBahasaConfig("JURUSAN"));
					rowhead.createCell(5).setCellValue("STATUS AWAL");
					rowhead.createCell(6).setCellValue("ANGKATAN");
					rowhead.createCell(7).setCellValue("ID TAGIHAN");
					rowhead.createCell(8).setCellValue("KETARANGAN TAGIHAN");
					rowhead.createCell(9).setCellValue("NOMINAL TAGIHAN");
					rowhead.createCell(10).setCellValue("TANGGAL TAGIHAN");
					rowhead.createCell(11).setCellValue("KUNCI");
					rowhead.createCell(12).setCellValue("STATUS");

					XSSFCellStyle lockedNumericStyle = workbook.createCellStyle();
					lockedNumericStyle.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
					lockedNumericStyle.setFillForegroundColor(new XSSFColor(Color.RED));
					lockedNumericStyle.setLocked(true);

					XSSFCellStyle notLocked = workbook.createCellStyle();
					notLocked.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
					notLocked.setFillForegroundColor(new XSSFColor(Color.LIGHT_GRAY));

					Session session = null;
					try {
						session = HibernateUtil.getSessionFactory().openSession();
						int rowProcessed = 0;

						for (int i = 1; i < size; i++) {
							Long id = Common.getSheetContentAsLong(sheetUpload, 7, i);
							Double nilai = Common.getSheetContentAsDouble(sheetUpload, 9, i);
							String tanggal = Common.getSheetContentAsString(sheetUpload, 10, i);
							Boolean kunci = Common.getSheetContentAsBoolean(sheetUpload, 11, i);

							if (id != null) {
								try {
									DetailKegiatan detailKegiatan = (DetailKegiatan) session
											.createCriteria(DetailKegiatan.class).add(Restrictions.idEq(id))
											.uniqueResult();
									if (detailKegiatan != null) {
										DetailBiaya detailBiayaBaris = detailKegiatan.getDetailBiaya();
										SettingBiaya settingBiayaBaris = detailBiayaBaris == null ? null
												: detailBiayaBaris.getSettingBiayaEfektif();
										boolean dalamCakupan = settingBiayaCakupan != null && settingBiayaBaris != null
												&& settingBiayaBaris.getId() != null
												&& settingBiayaBaris.getId().equals(settingBiayaCakupan.getId());

										if (!dalamCakupan) {
											XSSFRow rowDitolak = sheet.createRow(i);
											rowDitolak.createCell(7).setCellValue(detailKegiatan.getId());
											rowDitolak.createCell(12).setCellValue(
													"DITOLAK: ID TAGIHAN di luar cakupan Setting Biaya layar ini");
										} else {
											ItemBiaya itemBiayaBaris = detailKegiatan.getItemBiaya();
											boolean nilaiBisaDiubah = itemBiayaBaris != null
													&& Boolean.TRUE.equals(itemBiayaBaris.getNilaiBisaDiubah());
											boolean sedangDikunci = detailKegiatan.getKunci() != null;
											boolean adaPerubahanDiminta = kunci != null || nilai != null
													|| (tanggal != null && !tanggal.isEmpty());

											String statusTulis;
											if (adaPerubahanDiminta && !nilaiBisaDiubah) {
												statusTulis = "DITOLAK: nominal Item Biaya ini terkunci (tidak boleh diubah)";
											} else if ((nilai != null || (tanggal != null && !tanggal.isEmpty()))
													&& sedangDikunci) {
												statusTulis = "DITOLAK: baris sedang dikunci oleh "
														+ (detailKegiatan.getKunci().getUserId() == null ? "pengguna lain"
																: detailKegiatan.getKunci().getUserId());
											} else {
												if (kunci != null)
													detailKegiatan.setKunci(kunci ? tbmuser : null);
												if (nilai != null)
													detailKegiatan.setBiaya(nilai);
												if (tanggal != null && !tanggal.isEmpty()) {
													try {
														detailKegiatan.setTanggal(Common.dateFormat.get().parse(tanggal));
													} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/KegiatanHelper.java:1351");
													}
												}

												try {
													updateEntitySafe(session, detailKegiatan);
												} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/KegiatanHelper.java:1357");
												}
												statusTulis = "BERHASIL";
											}

											Mahasiswa mahasiswa = detailKegiatan.getKegiatan().getMahasiswa();
											BiodataCalonMahasiswa biodataCalonMahasiswa = detailKegiatan.getKegiatan()
													.getCalonMahasiswa();

											XSSFRow row = sheet.createRow(i);
											if (mahasiswa != null) {
												row.createCell(0).setCellValue(mahasiswa.getNim());
												row.createCell(1).setCellValue(mahasiswa.getNama());
												row.createCell(2).setCellValue(
														detailKegiatan.getKegiatan().getJenisKegiatan().getNamaKegiatan());
												row.createCell(3)
														.setCellValue(mahasiswa.getJurusan().getFakultas().getNama());
												row.createCell(4).setCellValue(mahasiswa.getJurusan().getNama());
												row.createCell(5)
														.setCellValue(mahasiswa.getStatusAwalMahasiswa() == null ? ""
																: mahasiswa.getStatusAwalMahasiswa().getNama());
												row.createCell(6).setCellValue(mahasiswa.getTahunangkatan());
											} else if (biodataCalonMahasiswa != null) {
												Jurusan jurusan = biodataCalonMahasiswa.getProdiLulus() == null
														? biodataCalonMahasiswa.getProdi1()
														: biodataCalonMahasiswa.getProdiLulus();
												row.createCell(0).setCellValue(biodataCalonMahasiswa.getNoRegistrasi());
												row.createCell(1).setCellValue(biodataCalonMahasiswa.getNama());
												row.createCell(2).setCellValue(
														detailKegiatan.getKegiatan().getJenisKegiatan().getNamaKegiatan());
												row.createCell(3).setCellValue(jurusan.getFakultas().getNama());
												row.createCell(4).setCellValue(jurusan.getNama());
												row.createCell(5)
														.setCellValue(biodataCalonMahasiswa.getStatusAwalMahasiswa() == null
																? ""
																: biodataCalonMahasiswa.getStatusAwalMahasiswa().getNama());
												row.createCell(6).setCellValue(biodataCalonMahasiswa.getTahun());
											}

											XSSFCell cell = row.createCell(7);
											cell.setCellValue(detailKegiatan.getId());
											cell.setCellStyle(lockedNumericStyle);

											StringBuilder desc = new StringBuilder();
											if (detailKegiatan.getPengaturanPembayaranBulanan() != null) {
												desc.append(detailKegiatan.getPengaturanPembayaranBulanan().getDetailBiaya()
														.getItemBiaya().getKode());
												desc.append(" ").append(detailKegiatan.getPengaturanPembayaranBulanan()
														.getDetailBiaya().getItemBiaya().getNama());
												desc.append(" ").append(
														detailKegiatan.getPengaturanPembayaranBulanan().getNamaBulan());
												desc.append(" smt ").append(detailKegiatan.getKegiatan().getSemster())
														.append(" ")
														.append(detailKegiatan.getKegiatan().getTahunAkademik());
											} else if (detailKegiatan.getItemBiaya() != null) {
												desc.append(detailKegiatan.getItemBiaya().getKode());
												desc.append(" ").append(detailKegiatan.getItemBiaya().getNama());
												desc.append(" smt ").append(detailKegiatan.getKegiatan().getSemster())
														.append(" ")
														.append(detailKegiatan.getKegiatan().getTahunAkademik());
											}

											cell = row.createCell(8);
											cell.setCellValue(desc.toString());

											cell = row.createCell(9);
											cell.setCellStyle(nilaiBisaDiubah ? notLocked : lockedNumericStyle);
											cell.setCellValue(detailKegiatan.getBiaya());

											cell = row.createCell(10);
											cell.setCellStyle(nilaiBisaDiubah ? notLocked : lockedNumericStyle);
											cell.setCellValue(detailKegiatan.getTanggal() == null ? ""
													: Common.dateFormat.get().format(detailKegiatan.getTanggal()));

											cell = row.createCell(11);
											cell.setCellStyle(nilaiBisaDiubah ? notLocked : lockedNumericStyle);
											cell.setCellValue(detailKegiatan.getKunci() != null);

											cell = row.createCell(12);
											cell.setCellValue(statusTulis);
										}
									}
								} catch (Exception e) {
									e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/KegiatanHelper.java:1437");
									XSSFRow rowGagal = sheet.getRow(i);
									if (rowGagal == null) {
										rowGagal = sheet.createRow(i);
										if (id != null) {
											rowGagal.createCell(7).setCellValue(id);
										}
									}
									rowGagal.createCell(12)
											.setCellValue("GAGAL: " + (e.getMessage() == null ? e.toString() : e.getMessage()));
								}

								rowProcessed++;
								if (rowProcessed % 50 == 0) {
									session.flush();
									session.clear();
								}
							}
						}
					} finally {
						closeLocalSessionSafely(session);
					}

					File fileHasil = new File(
							Sessions.getCurrent().getWebApp().getRealPath("/temp/hasil_upload_" + media.getName()));
					fileHasil.getParentFile().mkdirs();
					FileOutputStream fileOut = null;
					try {
						fileOut = new FileOutputStream(fileHasil);
						workbook.write(fileOut);
					} catch (IOException e) {
						Common.tampilErrorJikaAdmin(e);
					} finally {
						if (fileOut != null) {
							try {
								fileOut.close();
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/KegiatanHelper.java:1464");
							}
						}
					}

					try {
						Filedownload.save(new FileInputStream(fileHasil),
								"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", file.getName());
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/KegiatanHelper.java:1472");
					}

					eventListener.onEvent(uploadEvent);
					MyMessageboxConfig.show("Upload tagihan telah selesai.", "Informasi", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);

				} else {
					MyMessageboxConfig.show("File yang anda upload harus ber-format Excel Open XML Spreadsheet (xlsx).",
							"Error", MyMessageboxConfig.OK, MyMessageboxConfig.ERROR);
				}
			}
		});
		return toolbarbutton;
	}

	/**
	 * Mesin ekspor Excel untuk sisi DOWNLOAD dari pasangan upload/download tagihan (lihat
	 * {@link #prosesUploadTagihan} untuk format kolom yang harus tetap kompatibel). Untuk setiap
	 * {@link Mahasiswa} dalam {@code mahasiswas}, meng-iterasi rentang tahun {@code ta}..
	 * {@code taSampai}, menghitung nomor semester Ganjil/Genap ({@code Common.getSemester})
	 * sesuai flag {@code GANJIL}/{@code GENAP} yang diminta, memastikan {@link Kegiatan} ada/
	 * terhitung lewat {@link #checkKegiatanMahasiswa}, lalu menulis satu baris Excel per
	 * {@link DetailKegiatan} yang cocok (difilter {@code item} dan opsional rentang bulan
	 * {@code bulMul}..{@code bulSam} bila {@code bul=true}). Untuk {@code biodataCalonMahasiswas},
	 * alur serupa tapi memakai {@link #checkKegiatanCalonMahasiswa} dan smt tetap (bukan dihitung
	 * per tahun) — {@code smt=0} untuk jenis "Pendaftaran Calon Mahasiswa", selain itu 1 (Ganjil)
	 * atau 2 (Genap), difilter ke tahun angkatan yang cocok ({@code mahasiswa.getTahun()}).
	 * <p>
	 * Progres ditulis ke {@code label} (dibaca oleh timer polling di UI pemanggil — lihat
	 * {@link #prosesDownloadTagihan}); session Hibernate dibuka/ditutup per kombinasi mahasiswa+
	 * semester (bukan satu session besar) agar memory/koneksi tidak menumpuk pada dataset besar,
	 * dan di-{@code clear()} tiap kelipatan 50 baris. {@code colS}/{@code intbox} (parameter
	 * output, dimutasi di sini) membawa balik jumlah kolom &amp; baris akhir ke pemanggil untuk
	 * konfigurasi tampilan preview {@link Spreadsheet}. File akhirnya ditulis ke {@code filename}
	 * (path lengkap, bukan hanya nama) sebagai .xlsx.
	 *
	 * @param filename              path lengkap file .xlsx tujuan
	 * @param mahasiswas            daftar mahasiswa aktif yang diproses (boleh {@code null})
	 * @param biodataCalonMahasiswas daftar calon mahasiswa yang diproses (boleh {@code null})
	 * @param label                 diperbarui dengan pesan progres untuk dibaca UI pemanggil
	 * @param ta                    tahun akademik awal rentang
	 * @param taSampai              tahun akademik akhir rentang
	 * @param GANJIL                proses semester ganjil
	 * @param GENAP                 proses semester genap
	 * @param colS                  OUTPUT: diisi jumlah kolom (selalu 12, sudah tetap)
	 * @param intbox                OUTPUT: diisi jumlah baris data akhir
	 * @param bul                   {@code true} untuk membatasi ke rentang bulan {@code bulMul}..{@code bulSam} pada tagihan bulanan
	 * @param bulMul                bulan mulai (bila {@code bul})
	 * @param bulSam                bulan sampai (bila {@code bul})
	 * @param j                     jenis kegiatan/tagihan yang diproses
	 * @param ulang                 diteruskan ke {@code checkKegiatan*} untuk memaksa hitung ulang
	 * @param rst                   diteruskan ke {@code checkKegiatan*} untuk reset ke billing
	 * @param item                  batasi ke satu {@link ItemBiaya}, atau {@code null} untuk semua
	 */
	@SuppressWarnings("unchecked")
	public static void doDownloadTagihan(String filename, List<Mahasiswa> mahasiswas,
			List<BiodataCalonMahasiswa> biodataCalonMahasiswas, Label label, String ta, String taSampai, boolean GANJIL,
			boolean GENAP, Intbox colS, Intbox intbox, Boolean bul, Integer bulMul, Integer bulSam, JenisKegiatan j,
			Boolean ulang, boolean rst, ItemBiaya item) {

		XSSFWorkbook workbook = new XSSFWorkbook();
		XSSFSheet sheet = workbook.createSheet("DATA TAGIHAN");
		sheet.setDefaultColumnWidth(20);

		XSSFCellStyle lockedNumericStyle = workbook.createCellStyle();
		lockedNumericStyle.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
		lockedNumericStyle.setFillForegroundColor(new XSSFColor(Color.RED));
		lockedNumericStyle.setLocked(true);

		XSSFCellStyle notLocked = workbook.createCellStyle();
		notLocked.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
		notLocked.setFillForegroundColor(new XSSFColor(Color.LIGHT_GRAY));

		XSSFRow rowhead = sheet.createRow((short) 0);
		rowhead.createCell(0).setCellValue("NIM/NO REG");
		rowhead.createCell(1).setCellValue("NAMA");
		rowhead.createCell(2).setCellValue("JENIS PEMBAYARAN");
		rowhead.createCell(3).setCellValue(Common.getBahasaConfig("FAKULTAS"));
		rowhead.createCell(4).setCellValue(Common.getBahasaConfig("JURUSAN"));
		rowhead.createCell(5).setCellValue("STATUS AWAL");
		rowhead.createCell(6).setCellValue("ANGKATAN");
		rowhead.createCell(7).setCellValue("ID TAGIHAN");
		rowhead.createCell(8).setCellValue("KETARANGAN TAGIHAN");
		rowhead.createCell(9).setCellValue("NOMINAL TAGIHAN");
		rowhead.createCell(10).setCellValue("TANGGAL TAGIHAN");
		rowhead.createCell(11).setCellValue("KUNCI");

		colS.setValue(12);

		int size = (mahasiswas == null ? 0 : mahasiswas.size())
				+ (biodataCalonMahasiswas == null ? 0 : biodataCalonMahasiswas.size());
		int rowIndex = 1;
		int rowIndexMhs = 1;

		int thn = Integer.parseInt(ta.split("/")[0]);
		int thnSampai = Integer.parseInt(taSampai.split("/")[0]);

		if (mahasiswas != null) {
			for (Mahasiswa mahasiswa : mahasiswas) {
				label.setValue("Sedang memproses data " + mahasiswa.toString() + " ("
						+ Common.numberFormat.get().format(rowIndexMhs * 100.0 / size) + " %)");
				rowIndexMhs++;

				for (int tahun = thn; tahun <= thnSampai; tahun++) {
					if (mahasiswa.getTahunangkatan() <= tahun) {
						String Ta = tahun + "/" + (tahun + 1);

						if (GANJIL) {
							Integer smt = Common.getSemester(mahasiswa.getTahunangkatan(), Ta, Perkuliahan.GANJIL,
									mahasiswa.getPindahKeKampusIniMasukSemester(), mahasiswa.getSemesterMulai());
							if (smt >= j.getMinSmt() && smt <= j.getMaxSmt()) {
								Session session = null;
								try {
									session = HibernateUtil.getSessionFactory().openSession();
									Kegiatan kegiatan = checkKegiatanMahasiswa(j, mahasiswa, smt, Ta, ulang, rst, item,
											session);
									Criteria criteria = session.createCriteria(DetailKegiatan.class);
									if (bul) {
										criteria.createAlias("pengaturanPembayaranBulanan",
												"pengaturanPembayaranBulanan")
												.add(Restrictions.between("pengaturanPembayaranBulanan.realBulan",
														bulMul, bulSam));
									}
									List<DetailKegiatan> detailKegiatans = criteria
											.add(item == null ? Restrictions.sqlRestriction("true")
													: Restrictions.eq("itemBiaya", item))
											.add(Restrictions.eq("kegiatan.id", kegiatan.getId())).list();

									for (DetailKegiatan detailKegiatan : detailKegiatans) {
										XSSFRow row = sheet.createRow(rowIndex);
										row.createCell(0).setCellValue(mahasiswa.getNim());
										row.createCell(1).setCellValue(mahasiswa.getNama());
										row.createCell(2).setCellValue(j.getNamaKegiatan());
										row.createCell(3).setCellValue(mahasiswa.getJurusan().getFakultas().getNama());
										row.createCell(4).setCellValue(mahasiswa.getJurusan().getNama());
										row.createCell(5).setCellValue(mahasiswa.getStatusAwalMahasiswa() == null ? ""
												: mahasiswa.getStatusAwalMahasiswa().getNama());
										row.createCell(6).setCellValue(mahasiswa.getTahunangkatan());

										XSSFCell cell = row.createCell(7);
										cell.setCellValue(detailKegiatan.getId());
										cell.setCellStyle(lockedNumericStyle);

										StringBuilder desc = new StringBuilder();
										if (detailKegiatan.getPengaturanPembayaranBulanan() != null) {
											desc.append(detailKegiatan.getPengaturanPembayaranBulanan().getDetailBiaya()
													.getItemBiaya().getKode());
											desc.append(" ").append(detailKegiatan.getPengaturanPembayaranBulanan()
													.getDetailBiaya().getItemBiaya().getNama());
											desc.append(" ").append(
													detailKegiatan.getPengaturanPembayaranBulanan().getNamaBulan());
											desc.append(" smt ").append(detailKegiatan.getKegiatan().getSemster())
													.append(" ")
													.append(detailKegiatan.getKegiatan().getTahunAkademik());
										} else if (detailKegiatan.getItemBiaya() != null) {
											desc.append(detailKegiatan.getItemBiaya().getKode());
											desc.append(" ").append(detailKegiatan.getItemBiaya().getNama());
											desc.append(" smt ").append(detailKegiatan.getKegiatan().getSemster())
													.append(" ")
													.append(detailKegiatan.getKegiatan().getTahunAkademik());
										}

										cell = row.createCell(8);
										cell.setCellValue(desc.toString());

										cell = row.createCell(9);
										cell.setCellStyle(detailKegiatan.getItemBiaya().getNilaiBisaDiubah() ? notLocked
												: lockedNumericStyle);
										cell.setCellValue(detailKegiatan.getBiaya());

										cell = row.createCell(10);
										cell.setCellStyle(detailKegiatan.getItemBiaya().getNilaiBisaDiubah() ? notLocked
												: lockedNumericStyle);
										cell.setCellValue(detailKegiatan.getTanggal() == null ? ""
												: Common.dateFormat.get().format(detailKegiatan.getTanggal()));

										cell = row.createCell(11);
										cell.setCellStyle(detailKegiatan.getItemBiaya().getNilaiBisaDiubah() ? notLocked
												: lockedNumericStyle);
										cell.setCellValue(detailKegiatan.getKunci() != null);

										rowIndex++;
									}
									if (rowIndex % 50 == 0)
										session.clear();
								} catch (Exception e) {
									e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/KegiatanHelper.java:1620");
								} finally {
									closeLocalSessionSafely(session);
								}
							}
						}

						if (GENAP) {
							Integer smt = Common.getSemester(mahasiswa.getTahunangkatan(), Ta, Perkuliahan.GENAP,
									mahasiswa.getPindahKeKampusIniMasukSemester(), mahasiswa.getSemesterMulai());
							if (smt >= j.getMinSmt() && smt <= j.getMaxSmt()) {
								Session session = null;
								try {
									session = HibernateUtil.getSessionFactory().openSession();
									Kegiatan kegiatan = checkKegiatanMahasiswa(j, mahasiswa, smt, Ta, ulang, rst, item,
											session);
									Criteria criteria = session.createCriteria(DetailKegiatan.class);
									if (bul) {
										criteria.createAlias("pengaturanPembayaranBulanan",
												"pengaturanPembayaranBulanan")
												.add(Restrictions.between("pengaturanPembayaranBulanan.realBulan",
														bulMul, bulSam));
									}
									List<DetailKegiatan> detailKegiatans = criteria
											.add(item == null ? Restrictions.sqlRestriction("true")
													: Restrictions.eq("itemBiaya", item))
											.add(Restrictions.eq("kegiatan.id", kegiatan.getId())).list();

									for (DetailKegiatan detailKegiatan : detailKegiatans) {
										XSSFRow row = sheet.createRow(rowIndex);
										row.createCell(0).setCellValue(mahasiswa.getNim());
										row.createCell(1).setCellValue(mahasiswa.getNama());
										row.createCell(2).setCellValue(j.getNamaKegiatan());
										row.createCell(3).setCellValue(mahasiswa.getJurusan().getFakultas().getNama());
										row.createCell(4).setCellValue(mahasiswa.getJurusan().getNama());
										row.createCell(5).setCellValue(mahasiswa.getStatusAwalMahasiswa() == null ? ""
												: mahasiswa.getStatusAwalMahasiswa().getNama());
										row.createCell(6).setCellValue(mahasiswa.getTahunangkatan());

										XSSFCell cell = row.createCell(7);
										cell.setCellValue(detailKegiatan.getId());
										cell.setCellStyle(lockedNumericStyle);

										StringBuilder desc = new StringBuilder();
										if (detailKegiatan.getPengaturanPembayaranBulanan() != null) {
											desc.append(detailKegiatan.getPengaturanPembayaranBulanan().getDetailBiaya()
													.getItemBiaya().getKode());
											desc.append(" ").append(detailKegiatan.getPengaturanPembayaranBulanan()
													.getDetailBiaya().getItemBiaya().getNama());
											desc.append(" ").append(
													detailKegiatan.getPengaturanPembayaranBulanan().getNamaBulan());
											desc.append(" smt ").append(detailKegiatan.getKegiatan().getSemster())
													.append(" ")
													.append(detailKegiatan.getKegiatan().getTahunAkademik());
										} else if (detailKegiatan.getItemBiaya() != null) {
											desc.append(detailKegiatan.getItemBiaya().getKode());
											desc.append(" ").append(detailKegiatan.getItemBiaya().getNama());
											desc.append(" smt ").append(detailKegiatan.getKegiatan().getSemster())
													.append(" ")
													.append(detailKegiatan.getKegiatan().getTahunAkademik());
										}

										cell = row.createCell(8);
										cell.setCellValue(desc.toString());

										cell = row.createCell(9);
										cell.setCellStyle(detailKegiatan.getItemBiaya().getNilaiBisaDiubah() ? notLocked
												: lockedNumericStyle);
										cell.setCellValue(detailKegiatan.getBiaya());

										cell = row.createCell(10);
										cell.setCellStyle(detailKegiatan.getItemBiaya().getNilaiBisaDiubah() ? notLocked
												: lockedNumericStyle);
										cell.setCellValue(detailKegiatan.getTanggal() == null ? ""
												: Common.dateFormat.get().format(detailKegiatan.getTanggal()));

										cell = row.createCell(11);
										cell.setCellStyle(detailKegiatan.getItemBiaya().getNilaiBisaDiubah() ? notLocked
												: lockedNumericStyle);
										cell.setCellValue(detailKegiatan.getKunci() != null);

										rowIndex++;
									}
									if (rowIndex % 50 == 0)
										session.clear();
								} catch (Exception e) {
									e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/KegiatanHelper.java:1706");
								} finally {
									closeLocalSessionSafely(session);
								}
							}
						}
					}
				}
			}
		}

		if (biodataCalonMahasiswas != null) {
			for (BiodataCalonMahasiswa mahasiswa : biodataCalonMahasiswas) {
				label.setValue("Sedang memproses data " + mahasiswa.toString() + " ("
						+ Common.numberFormat.get().format(rowIndexMhs * 100.0 / size) + " %)");
				rowIndexMhs++;

				Jurusan jurusan = mahasiswa.getProdiLulus() == null ? mahasiswa.getProdi1() : mahasiswa.getProdiLulus();

				for (int tahun = thn; tahun <= thnSampai; tahun++) {
					String Ta = tahun + "/" + (tahun + 1);
					if (mahasiswa.getTahun() != null && mahasiswa.getTahun().equals(tahun)) {
						if (GANJIL) {
							int smt = (ConstantValues.PENDAFTARAN_CALON_MAHASISWA != null
									&& j.getId().equals(ConstantValues.PENDAFTARAN_CALON_MAHASISWA.getId())) ? 0 : 1;
							Session session = null;
							try {
								session = HibernateUtil.getSessionFactory().openSession();
								Kegiatan kegiatan = checkKegiatanCalonMahasiswa(j, mahasiswa, smt, Ta, ulang, rst, item,
										session);
								Criteria criteria = session.createCriteria(DetailKegiatan.class);
								if (bul) {
									criteria.createAlias("pengaturanPembayaranBulanan", "pengaturanPembayaranBulanan")
											.add(Restrictions.between("pengaturanPembayaranBulanan.realBulan", bulMul,
													bulSam));
								}
								List<DetailKegiatan> detailKegiatans = criteria
										.add(item == null ? Restrictions.sqlRestriction("true")
												: Restrictions.eq("itemBiaya", item))
										.add(Restrictions.eq("kegiatan.id", kegiatan.getId())).list();

								for (DetailKegiatan detailKegiatan : detailKegiatans) {
									XSSFRow row = sheet.createRow(rowIndex);
									row.createCell(0).setCellValue(mahasiswa.getNoRegistrasi());
									row.createCell(1).setCellValue(mahasiswa.getNama());
									row.createCell(2).setCellValue(j.getNamaKegiatan());
									row.createCell(3)
											.setCellValue(jurusan == null ? "" : jurusan.getFakultas().getNama());
									row.createCell(4).setCellValue(jurusan == null ? "" : jurusan.getNama());
									row.createCell(5).setCellValue(mahasiswa.getStatusAwalMahasiswa() == null ? ""
											: mahasiswa.getStatusAwalMahasiswa().getNama());
									row.createCell(6).setCellValue(mahasiswa.getTahun());

									XSSFCell cell = row.createCell(7);
									cell.setCellValue(detailKegiatan.getId());
									cell.setCellStyle(lockedNumericStyle);

									StringBuilder desc = new StringBuilder();
									if (detailKegiatan.getPengaturanPembayaranBulanan() != null) {
										desc.append(detailKegiatan.getPengaturanPembayaranBulanan().getDetailBiaya()
												.getItemBiaya().getKode());
										desc.append(" ").append(detailKegiatan.getPengaturanPembayaranBulanan()
												.getDetailBiaya().getItemBiaya().getNama());
										desc.append(" ")
												.append(detailKegiatan.getPengaturanPembayaranBulanan().getNamaBulan());
										desc.append(" smt ").append(detailKegiatan.getKegiatan().getSemster())
												.append(" ").append(detailKegiatan.getKegiatan().getTahunAkademik());
									} else if (detailKegiatan.getItemBiaya() != null) {
										desc.append(detailKegiatan.getItemBiaya().getKode());
										desc.append(" ").append(detailKegiatan.getItemBiaya().getNama());
										desc.append(" smt ").append(detailKegiatan.getKegiatan().getSemster())
												.append(" ").append(detailKegiatan.getKegiatan().getTahunAkademik());
									}

									cell = row.createCell(8);
									cell.setCellValue(desc.toString());

									cell = row.createCell(9);
									cell.setCellStyle(detailKegiatan.getItemBiaya().getNilaiBisaDiubah() ? notLocked
											: lockedNumericStyle);
									cell.setCellValue(detailKegiatan.getBiaya());

									cell = row.createCell(10);
									cell.setCellStyle(detailKegiatan.getItemBiaya().getNilaiBisaDiubah() ? notLocked
											: lockedNumericStyle);
									cell.setCellValue(detailKegiatan.getTanggal() == null ? ""
											: Common.dateFormat.get().format(detailKegiatan.getTanggal()));

									cell = row.createCell(11);
									cell.setCellStyle(detailKegiatan.getItemBiaya().getNilaiBisaDiubah() ? notLocked
											: lockedNumericStyle);
									cell.setCellValue(detailKegiatan.getKunci() != null);

									rowIndex++;
								}
								if (rowIndex % 50 == 0)
									session.clear();
							} catch (Exception e) {
								e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/KegiatanHelper.java:1804");
							} finally {
								closeLocalSessionSafely(session);
							}
						}

						if (GENAP) {
							int smt = (ConstantValues.PENDAFTARAN_CALON_MAHASISWA != null
									&& j.getId().equals(ConstantValues.PENDAFTARAN_CALON_MAHASISWA.getId())) ? 0 : 2;
							Session session = null;
							try {
								session = HibernateUtil.getSessionFactory().openSession();
								Kegiatan kegiatan = checkKegiatanCalonMahasiswa(j, mahasiswa, smt, Ta, ulang, rst, item,
										session);
								Criteria criteria = session.createCriteria(DetailKegiatan.class);
								if (bul) {
									criteria.createAlias("pengaturanPembayaranBulanan", "pengaturanPembayaranBulanan")
											.add(Restrictions.between("pengaturanPembayaranBulanan.realBulan", bulMul,
													bulSam));
								}
								List<DetailKegiatan> detailKegiatans = criteria
										.add(item == null ? Restrictions.sqlRestriction("true")
												: Restrictions.eq("itemBiaya", item))
										.add(Restrictions.eq("kegiatan.id", kegiatan.getId())).list();

								for (DetailKegiatan detailKegiatan : detailKegiatans) {
									XSSFRow row = sheet.createRow(rowIndex);
									row.createCell(0).setCellValue(mahasiswa.getNoRegistrasi());
									row.createCell(1).setCellValue(mahasiswa.getNama());
									row.createCell(2).setCellValue(j.getNamaKegiatan());
									row.createCell(3)
											.setCellValue(jurusan == null ? "" : jurusan.getFakultas().getNama());
									row.createCell(4).setCellValue(jurusan == null ? "" : jurusan.getNama());
									row.createCell(5).setCellValue(mahasiswa.getStatusAwalMahasiswa() == null ? ""
											: mahasiswa.getStatusAwalMahasiswa().getNama());
									row.createCell(6).setCellValue(mahasiswa.getTahun());

									XSSFCell cell = row.createCell(7);
									cell.setCellValue(detailKegiatan.getId());
									cell.setCellStyle(lockedNumericStyle);

									StringBuilder desc = new StringBuilder();
									if (detailKegiatan.getPengaturanPembayaranBulanan() != null) {
										desc.append(detailKegiatan.getPengaturanPembayaranBulanan().getDetailBiaya()
												.getItemBiaya().getKode());
										desc.append(" ").append(detailKegiatan.getPengaturanPembayaranBulanan()
												.getDetailBiaya().getItemBiaya().getNama());
										desc.append(" ")
												.append(detailKegiatan.getPengaturanPembayaranBulanan().getNamaBulan());
										desc.append(" smt ").append(detailKegiatan.getKegiatan().getSemster())
												.append(" ").append(detailKegiatan.getKegiatan().getTahunAkademik());
									} else if (detailKegiatan.getItemBiaya() != null) {
										desc.append(detailKegiatan.getItemBiaya().getKode());
										desc.append(" ").append(detailKegiatan.getItemBiaya().getNama());
										desc.append(" smt ").append(detailKegiatan.getKegiatan().getSemster())
												.append(" ").append(detailKegiatan.getKegiatan().getTahunAkademik());
									}

									cell = row.createCell(8);
									cell.setCellValue(desc.toString());

									cell = row.createCell(9);
									cell.setCellStyle(detailKegiatan.getItemBiaya().getNilaiBisaDiubah() ? notLocked
											: lockedNumericStyle);
									cell.setCellValue(detailKegiatan.getBiaya());

									cell = row.createCell(10);
									cell.setCellStyle(detailKegiatan.getItemBiaya().getNilaiBisaDiubah() ? notLocked
											: lockedNumericStyle);
									cell.setCellValue(detailKegiatan.getTanggal() == null ? ""
											: Common.dateFormat.get().format(detailKegiatan.getTanggal()));

									cell = row.createCell(11);
									cell.setCellStyle(detailKegiatan.getItemBiaya().getNilaiBisaDiubah() ? notLocked
											: lockedNumericStyle);
									cell.setCellValue(detailKegiatan.getKunci() != null);

									rowIndex++;
								}
								if (rowIndex % 50 == 0)
									session.clear();
							} catch (Exception e) {
								e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/KegiatanHelper.java:1886");
							} finally {
								closeLocalSessionSafely(session);
							}
						}
					}
				}
			}
		}

		System.out.println("Your excel file has been generated! ");
		intbox.setValue(rowIndex + 5);

		FileOutputStream fileOut = null;
		try {
			fileOut = new FileOutputStream(filename);
			workbook.write(fileOut);
		} catch (IOException e) {
			Common.tampilErrorJikaAdmin(e);
		} finally {
			if (fileOut != null) {
				try {
					fileOut.close();
				} catch (IOException e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/KegiatanHelper.java:1909");
				}
			}
		}
	}

	/**
	 * Membangun tombol toolbar ZK yang membuka popup filter (Fakultas/Prodi/Mahasiswa, rentang
	 * Tahun Akademik, Jenis Pembayaran, Item Biaya, rentang Tahun Angkatan, Ganjil/Genap, filter
	 * Status Mahasiswa, opsi "Tagihan Bulanan"+rentang bulan, opsi "Reset Tagihan Kembali ke
	 * Billing") lalu menjalankan {@link #doDownloadTagihan} secara ASINKRON di {@link Thread}
	 * terpisah (agar UI ZK tidak terblokir), dengan {@link Timer} polling 200ms yang membaca
	 * {@code label} progres dan menampilkan hasil dalam popup {@link Spreadsheet} preview begitu
	 * selesai (kosong=selesai sukses, {@code "-"}=gagal). Daftar mahasiswa/calon mahasiswa yang
	 * diproses ditentukan dari kombinasi filter di popup: jenis kegiatan "Pendaftaran Calon
	 * Mahasiswa"/"Pendaftaran Ulang Mahasiswa Baru" mengarah ke query {@link BiodataCalonMahasiswa}
	 * (dengan pencocokan fakultas/prodi ke salah satu dari lima kolom pilihan prodi
	 * {@code prodi1}..{@code prodi5} ATAU {@code prodiLulus}), jenis lain mengarah ke query
	 * {@link Mahasiswa} (opsional di-scope lewat sub-query {@link HistoryStatusMahasiswa} bila
	 * filter Status Mahasiswa diisi). Variant paling generik/manual dari tiga overload
	 * {@code prosesDownloadTagihan*} di kelas ini — dipakai saat pemanggil tidak sudah punya
	 * {@link DataCriteria} pencarian siap pakai (bandingkan dengan overload di bawah).
	 *
	 * @param buttonLabel label tombol
	 * @param buttonImage path ikon tombol
	 * @return tombol toolbar siap dipasang ke UI
	 */
	public static MyToolbarbuttonConfig prosesDownloadTagihan(String buttonLabel, String buttonImage) {
		MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig(buttonLabel, buttonImage);

		toolbarbutton.addEventListener("onClick", new EventListener() {
			/**
			 * Menangani klik tombol: membangun dan menampilkan popup modal filter "Pilih Tahun Akademik,
			 * Angkatan, dan Jenis Pembayaran" secara programatik (tanpa .zul), lalu menyerahkan eksekusi
			 * ke listener tombol "Download Tagihan" di dalamnya.
			 *
			 * <p>Seluruh isi popup dirakit di sini: {@link Combobox} tahun akademik mulai/sampai
			 * ({@code Common.generateTahunAjaran}), Fakultas+Prodi berpasangan
			 * ({@code Common.initFakultasDanJurusanDanSemua}), kotak cari Mahasiswa, checkbox "Hitung Ulang
			 * Tagihan" (default tercentang), Jenis Pembayaran ({@link JenisKegiatan} aktif, default
			 * {@code ConstantValues.PENDAFTARAN_MAHASISWA_LAMA}), Item Biaya ({@link ItemBiaya} aktif,
			 * opsional), rentang angkatan, checkbox Ganjil/Genap, Status Mahasiswa beserta tahun akademik
			 * &amp; ganjil/genap status-nya, checkbox "Tagihan Bulanan" dengan rentang bulan, dan checkbox
			 * "Reset Tagihan Kembali ke Billing".
			 *
			 * <p>Semua kontrol dideklarasikan {@code final} karena dibaca dari dalam listener bersarang
			 * "Download Tagihan" — inilah cara popup ini mengoper nilai filter tanpa objek model
			 * tersendiri. Popup dipasang ke root page lewat {@code ExecutionsCtrl} dan ditutup dengan
			 * {@code onModal()} di akhir method, sehingga listener ini memblokir sampai popup selesai.
			 *
			 * <p>Overload ini adalah varian paling manual dari tiga {@code prosesDownloadTagihan*} di kelas
			 * ini: dua overload lainnya sudah menerima {@link DataCriteria}/{@link SettingBiaya} dari layar
			 * pemanggil sehingga tidak menampilkan popup filter sama sekali.
			 *
			 * @param arg0 event {@code onClick} ZK; tidak dibaca
			 * @throws Exception diteruskan dari perakitan komponen ZK atau dari {@code onModal()}
			 */
			@Override
			public void onEvent(Event arg0) throws Exception {
				final MyWindow window = new MyWindow("Pilih Tahun Akademik, Angkatan, dan Jenis Pembayaran", "none",
						true);
				window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				window.setHeight("95%");
				window.setWidth("600px");

				final Combobox tahunAkademik = new Combobox();
				Common.generateTahunAjaran(tahunAkademik);

				final Combobox tahunAkademikSampai = new Combobox();
				Common.generateTahunAjaran(tahunAkademikSampai);

				Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
				borderlayout.setParent(window);

				Center center = new Center();
				center.setParent(borderlayout);

				MyGrid grid = new MyGrid();
				grid.setWidth("100%");
				grid.setParent(center);
				grid.setHeight("100%");

				Columns columns = new Columns();
				columns.setParent(grid);
				MyColumnConfig column = new MyColumnConfig();
				column.setWidth("20%");
				column.setParent(columns);
				column = new MyColumnConfig();
				column.setParent(columns);

				Rows rows = new Rows();
				rows.setParent(grid);

				final Combobox fakultas = new Combobox();
				final Combobox jurusan = new Combobox();
				Common.initFakultasDanJurusanDanSemua(fakultas, jurusan, null, null);

				MyFormRow row = new MyFormRow();
				row.setValign("top");
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
				row.appendChild(fakultas);
				fakultas.setWidth("90%");

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
				row.appendChild(jurusan);
				jurusan.setWidth("90%");

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Mahasiswa"));
				final MyTextbox mahasiswa = new MyTextbox();
				row.appendChild(mahasiswa);
				mahasiswa.setWidth("90%");

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig(""));
				final MyCheckboxConfig hitungUlang = new MyCheckboxConfig("Hitung Ulang Tagihan");
				row.appendChild(hitungUlang);
				hitungUlang.setChecked(true);

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik Mulai *"));
				row.appendChild(tahunAkademik);
				tahunAkademik.setWidth("90%");
				tahunAkademik.setReadonly(true);

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik Sampai *"));
				row.appendChild(tahunAkademikSampai);
				tahunAkademikSampai.setWidth("90%");
				tahunAkademikSampai.setReadonly(true);

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Pembayaran *"));

				final Combobox jenisPembayaran = new Combobox();
				row.appendChild(jenisPembayaran);
				jenisPembayaran.setWidth("90%");
				jenisPembayaran.setReadonly(true);
				Common.insertCombo(jenisPembayaran, "namaKegiatan", JenisKegiatan.class,
						Restrictions.eq("aktif", true));
				Common.selectComboItem(jenisPembayaran, ConstantValues.PENDAFTARAN_MAHASISWA_LAMA);

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Item Biaya"));

				final Combobox itemBiaya = new Combobox();
				row.appendChild(itemBiaya);
				itemBiaya.setWidth("90%");
				itemBiaya.setReadonly(true);
				Common.insertComboDanSemua(itemBiaya, "nama", ItemBiaya.class, Restrictions.eq("aktif", true));

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Angkatan Mulai *"));
				final Intbox angkatan = new Intbox(ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR) - 5);
				row.appendChild(angkatan);
				angkatan.setWidth("90%");

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Angkatan Sampai *"));
				final Intbox angkatanSampai = new Intbox(ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR));
				row.appendChild(angkatanSampai);
				angkatanSampai.setWidth("90%");

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig(""));
				final MyCheckboxConfig gnj = new MyCheckboxConfig("Ganjil");
				row.appendChild(gnj);
				gnj.setChecked(Common.isNowSemensterGanjil());

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig(""));
				final MyCheckboxConfig gnp = new MyCheckboxConfig("Genap");
				row.appendChild(gnp);
				gnp.setChecked(!Common.isNowSemensterGanjil());

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Status Mahasiswa"));
				final Combobox statusMahasiswa = new Combobox();
				final Combobox statusTahunAkademik = new Combobox();
				Hbox hbox = new Hbox();
				hbox.setParent(row);
				hbox.appendChild(statusMahasiswa);
				hbox.appendChild(statusTahunAkademik);
				Common.insertComboDanSemua(statusMahasiswa, "nama", StatusMahasiswa.class);
				Common.generateTahunAjaran(statusTahunAkademik);
				statusMahasiswa.setCols(3);
				statusTahunAkademik.setCols(4);

				final Combobox genapGanjil = new Combobox();
				hbox.appendChild(genapGanjil);
				org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
				comboitem.setLabel(Perkuliahan.GENAP);
				comboitem.setValue(Perkuliahan.GENAP);
				genapGanjil.appendChild(comboitem);
				comboitem = new MyComboitemConfig();
				comboitem.setLabel(Perkuliahan.GANJIL);
				comboitem.setValue(Perkuliahan.GANJIL);
				genapGanjil.appendChild(comboitem);

				Common.selectComboItem(genapGanjil,
						Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP);
				genapGanjil.setReadonly(true);
				genapGanjil.setCols(3);

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig(""));
				final MyCheckboxConfig bulanan = new MyCheckboxConfig("Tagihan Bulanan");
				row.appendChild(bulanan);

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Tagihan Bulan Mulai"));
				final Intbox bulanMulai = new Intbox(1);
				row.appendChild(bulanMulai);
				bulanMulai.setWidth("90%");
				bulanMulai.setDisabled(true);

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Bulan Sampai"));
				final Intbox bulanSampai = new Intbox(12);
				row.appendChild(bulanSampai);
				bulanSampai.setWidth("90%");
				bulanSampai.setDisabled(true);

				bulanan.addEventListener("onClick", new EventListener() {
					/**
					 * Mengaktifkan/menonaktifkan pasangan {@link Intbox} rentang bulan mengikuti status checkbox
					 * "Tagihan Bulanan".
					 *
					 * <p>Kedua intbox dibuat dalam keadaan {@code setDisabled(true)}, jadi listener inilah
					 * satu-satunya yang membukanya. Karena disable hanya bersifat UI, nilai bulan tetap dibaca
					 * listener "Download Tagihan" apa pun statusnya — yang menentukan dipakai atau tidaknya rentang
					 * bulan adalah flag {@code bul} (hasil {@code bulanan.isChecked()}) yang ikut dioper ke
					 * {@link #doDownloadTagihan}, bukan status disabled ini.
					 *
					 * @param arg0 event {@code onClick} checkbox; tidak dibaca
					 * @throws Exception dipersyaratkan {@link EventListener}; tidak dilempar di sini
					 */
					@Override
					public void onEvent(Event arg0) throws Exception {
						bulanMulai.setDisabled(!bulanan.isChecked());
						bulanSampai.setDisabled(!bulanan.isChecked());
					}
				});

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig(""));
				final MyCheckboxConfig reset = new MyCheckboxConfig("Reset Tagihan Kembali ke Billing");
				row.appendChild(reset);

				South south = new South();
				ais.ui.util.ZkCompat.setFlex(south, true);
				south.setParent(borderlayout);

				Toolbar toolbar = new Toolbar();
				toolbar.setParent(south);
				MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
				cancel.setTooltiptext("Tutup");
				cancel.addEventListener("onClick", new EventListener() {
					/**
					 * Menutup popup filter tanpa memproses apa pun ({@code window.detach()}).
					 *
					 * @param event event {@code onClick} tombol "Batal"; tidak dibaca
					 * @throws Exception dipersyaratkan {@link EventListener}; tidak dilempar di sini
					 */
					@Override
					public void onEvent(Event event) throws Exception {
						window.detach();
					}
				});
				cancel.setParent(toolbar);

				MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Download Tagihan", "/img/save.gif");
				save.setTooltiptext("Proses");
				save.addEventListener("onClick", new EventListener() {
					/**
					 * Membaca seluruh filter dari popup, lalu menjalankan pembentukan berkas tagihan di thread
					 * latar sambil menampilkan indikator sibuk yang dipantau {@link Timer}.
					 *
					 * <p><b>Pengambilan filter.</b> Setiap kontrol {@code final} dari popup dibaca ke variabel
					 * lokal {@code final} (agar bisa ditangkap thread latar): rentang tahun akademik, rentang
					 * angkatan (null menjadi 0), flag+rentang bulanan, {@link JenisKegiatan}, {@link Fakultas},
					 * {@link Jurusan}, {@link StatusMahasiswa} beserta tahun akademik dan ganjil/genap status-nya,
					 * flag Ganjil/Genap, kata kunci mahasiswa, flag hitung ulang, {@link ItemBiaya}, dan flag reset.
					 * Satu-satunya validasi adalah Jenis Pembayaran wajib terisi — pesan peringatannya berbunyi
					 * "Jadwal Pembayaran harus diisi" (istilah lama yang tidak sesuai label kontrolnya). Sesudah
					 * lolos, popup langsung di-{@code detach()}.
					 *
					 * <p><b>Kanal progres lintas thread.</b> Sebuah {@link Label} ZK dipakai sebagai variabel
					 * status bersama antara thread latar dan {@link Timer} di event thread — bukan sebagai
					 * komponen yang ditampilkan. Konvensinya: nilai awal "Proses load data .." berarti masih
					 * berjalan, {@code ""} (kosong) berarti selesai sukses, dan {@code "-"} berarti gagal. Dua
					 * {@link Intbox} ({@code intbox}, {@code colS}) dipakai dengan cara yang sama untuk membawa
					 * balik jumlah baris dan kolom hasil dari {@link #doDownloadTagihan}. Pola ini menghindari
					 * pembaruan desktop ZK dari thread non-event (yang membutuhkan server push); thread latar hanya
					 * menulis nilai, dan seluruh manipulasi UI dikerjakan Timer di event thread.
					 *
					 * <p><b>Berkas keluaran.</b> Nama berkas dibentuk dari timestamp yang di-{@code URLEncoder}
					 * ke {@code /tmp/cetak_data_&lt;timestamp&gt;.xlsx} pada path real webapp, dibuat kosong lebih dulu
					 * ({@code createNewFile()}) agar Timer dapat merujuknya sebelum thread latar selesai menulis.
					 *
					 * <p><b>Eksekusi.</b> {@link Timer} 200 ms berulang dipasang dan di-{@code start()}, lalu
					 * {@code new Thread(...).start()} menjalankan query + {@link #doDownloadTagihan}. Perhatikan
					 * thread ini dibuat langsung (tanpa pool) dan berjalan di luar ZK execution maupun transaksi
					 * request, sehingga membuka session Hibernate-nya sendiri.
					 *
					 * @param event event {@code onClick} tombol "Download Tagihan"; tidak dibaca
					 * @throws Exception diteruskan dari pembuatan berkas sementara/{@code URLEncoder}
					 */
					@Override
					public void onEvent(Event event) throws Exception {

						final String ta = (String) tahunAkademik.getSelectedItem().getValue();
						final String taSampai = (String) tahunAkademikSampai.getSelectedItem().getValue();
						final Integer ang = angkatan.getValue() == null ? 0 : angkatan.getValue();
						final Integer angSampai = angkatanSampai.getValue() == null ? 0 : angkatanSampai.getValue();

						final Boolean bul = bulanan.isChecked();
						final Integer bulMul = bulanMulai.getValue() == null ? 0 : bulanMulai.getValue();
						final Integer bulSam = bulanSampai.getValue() == null ? 0 : bulanSampai.getValue();

						final JenisKegiatan j = (JenisKegiatan) (jenisPembayaran.getSelectedItem() == null ? null
								: jenisPembayaran.getSelectedItem().getValue());

						final Fakultas fak = (Fakultas) (fakultas.getSelectedItem() == null ? null
								: fakultas.getSelectedItem().getValue());
						final Jurusan jur = (Jurusan) (jurusan.getSelectedItem() == null ? null
								: jurusan.getSelectedItem().getValue());

						final StatusMahasiswa status = (StatusMahasiswa) (statusMahasiswa.getSelectedItem() == null
								? null
								: statusMahasiswa.getSelectedItem().getValue());
						final String taStatus = (String) statusTahunAkademik.getSelectedItem().getValue();
						final String jenisSmtStatus = (String) genapGanjil.getSelectedItem().getValue();

						final boolean GANJIL = gnj.isChecked();
						final boolean GENAP = gnp.isChecked();

						final String mhs = mahasiswa.getValue().trim();
						final Boolean ulang = hitungUlang.isChecked();
						final ItemBiaya item = (ItemBiaya) (itemBiaya.getSelectedItem() == null ? null
								: itemBiaya.getSelectedItem().getValue());

						final boolean rst = reset.isChecked();

						if (j == null) {
							MyMessageboxConfig.show("Jadwal Pembayaran harus diisi", "Peringatan",
									MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
							return;
						}
						window.detach();

						final Label label = new Label(ais.common.Common.getBahasaConfig("Proses load data .."));
						final Intbox intbox = new Intbox(10);
						final Intbox colS = new Intbox(10);
						Clients.showBusy(label.getValue());

						final String filename = Sessions.getCurrent().getWebApp()
								.getRealPath("/tmp/cetak_data_" + URLEncoder.encode(
										Common.datetimeFormat2s.get().format(ais.ui.util.WaktuUtil.getDate()), "UTF-8")
										+ ".xlsx");
						final File file = new File(filename);
						file.createNewFile();

						final Timer timer = new Timer(200);
						timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
						timer.setRepeats(true);
						timer.addEventListener("onTimer", new EventListener() {
							/**
							 * Denyut {@link Timer} 200 ms yang memantau {@link Label} status thread latar dan, begitu
							 * prosesnya selesai, menutup indikator sibuk serta membuka jendela pratinjau hasil.
							 *
							 * <p>Tiga kondisi yang dibedakan dari nilai label: {@code "-"} berarti thread latar gagal —
							 * indikator sibuk dibersihkan dan timer dilepas tanpa membuka apa pun (pesan error-nya sendiri
							 * sudah ditampilkan thread latar lewat {@code Common.tampilErrorJikaAdmin}); {@code ""} berarti
							 * sukses — jendela "Cetak Data" dibuka; nilai lain berarti masih berjalan sehingga indikator
							 * sibuk sekadar disegarkan.
							 *
							 * <p>Pada jalur sukses dibangun {@link MyWindow} modal berisi {@link Spreadsheet} yang menunjuk
							 * berkas hasil lewat path relatif {@code ../../tmp/&lt;nama&gt;}, dibatasi {@code maxrows}/
							 * {@code maxcolumns} dari kedua {@link Intbox} pembawa nilai (baris ditambah 3 untuk header),
							 * lalu dikonversi menjadi grid oleh {@code PratinjauXlsxHelper.gantiSpreadsheetDenganGrid}.
							 * Toolbar bawahnya berisi tombol "Tutup" dan "Download Data". Setelah jendela dimodalkan,
							 * indikator sibuk dibersihkan dan timer dilepas supaya denyut berhenti.
							 *
							 * <p>Seluruh badan dibungkus {@code try/catch} yang pada kegagalan apa pun hanya memanggil
							 * {@code Clients.clearBusy()} — timer sengaja TIDAK dilepas di jalur ini, sehingga denyut
							 * berikutnya masih berkesempatan membuka pratinjau bila kegagalannya bersifat sesaat.
							 *
							 * @param arg0 event {@code onTimer}; tidak dibaca
							 * @throws Exception dideklarasikan {@link EventListener}; secara praktis tertangkap di dalam
							 */
							@Override
							public void onEvent(Event arg0) throws Exception {
								try {
									Clients.showBusy(label.getValue());

									if (label.getValue().trim().equalsIgnoreCase("-")) {
										Clients.clearBusy();
										timer.detach();
									} else if (label.getValue().isEmpty()) {

										Center center = new Center();
										final MyWindow window = new MyWindow("Cetak Data", "none", true);
										window.setParent(
												ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
										window.setHeight("97%");
										window.setWidth("90%");

										Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
										borderlayout.setParent(window);

										ais.ui.util.ZkCompat.setFlex(center, true);
										center.setParent(borderlayout);

										Common.clear(center);
										Spreadsheet spreadsheet = new ais.ui.util.MySpreadsheet();
										Common.clear(center);
										spreadsheet.setParent(center);
										spreadsheet.setWidth("100%");
										spreadsheet.setHeight("100%");
										spreadsheet.setSrc("../../tmp/" + file.getName());

										spreadsheet.setMaxrows(intbox.getValue() + 3);
										spreadsheet.setMaxcolumns(colS.getValue());
										ais.ui.util.PratinjauXlsxHelper.gantiSpreadsheetDenganGrid(spreadsheet);

										South south = new South();
										south.setParent(borderlayout);

										Toolbar toolbar = new Toolbar();
										toolbar.setParent(south);
										MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Tutup",
												"/img/cancel.gif");
										cancel.setTooltiptext("Tutup");
										cancel.addEventListener("onClick", new EventListener() {
											/**
											 * Menutup jendela pratinjau "Cetak Data" ({@code window.detach()}). Berkas hasil di
											 * {@code /tmp} tidak ikut dihapus.
											 *
											 * @param event event {@code onClick} tombol "Tutup"; tidak dibaca
											 * @throws Exception dipersyaratkan {@link EventListener}; tidak dilempar di sini
											 */
											@Override
											public void onEvent(Event event) throws Exception {
												window.detach();
											}
										});
										cancel.setParent(toolbar);

										MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Download Data",
												"/img/excel.png");
										print.addEventListener("onClick", new EventListener() {
											/**
											 * Mengirim berkas .xlsx hasil ke browser lewat {@link Filedownload} dengan MIME type
											 * spreadsheet OpenXML. Berkas dibaca ulang dari disk ({@link FileInputStream}), bukan dari
											 * memori, karena thread latar menulisnya langsung ke {@code /tmp}.
											 *
											 * <p>Kegagalan pengiriman ditelan dan hanya dicatat ke {@code ErrorAuditUtil}: pengguna masih
											 * melihat pratinjau di layar sehingga kegagalan unduhan tidak dijadikan error yang membatalkan
											 * jendela.
											 *
											 * @param event event {@code onClick} tombol "Download Data"; tidak dibaca
											 * @throws Exception dideklarasikan {@link EventListener}; kegagalan nyata tertangkap di dalam
											 */
											@Override
											public void onEvent(Event event) throws Exception {
												try {
													Filedownload.save(new FileInputStream(file),
															"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
															file.getName());
												} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/KegiatanHelper.java:2253");
												}
											}
										});
										print.setParent(toolbar);

										window.setVisible(true);
										window.onModal();

										Clients.clearBusy();
										timer.detach();
									}
								} catch (Exception e) {
									Clients.clearBusy();
								}
							}
						});
						timer.start();

						try {
							Clients.showBusy(label.getValue());

							new Thread(new Runnable() {
								/**
								 * Badan thread latar: menyusun daftar sasaran ({@link Mahasiswa} atau
								 * {@link BiodataCalonMahasiswa}) sesuai filter popup, memanggil {@link #doDownloadTagihan}
								 * untuk menulis berkas .xlsx, lalu menandai hasilnya lewat {@link Label} status.
								 *
								 * <p><b>Percabangan sumber data.</b> Bila {@link JenisKegiatan} terpilih adalah
								 * {@code PENDAFTARAN_CALON_MAHASISWA} atau {@code PENDAFTARAN_ULANG_MAHASISWA_BARU}, sasaran
								 * diambil dari {@link BiodataCalonMahasiswa}; selain itu dari {@link Mahasiswa}. Keduanya
								 * menyaring baris aktif dengan pola {@code aktif IS NULL OR aktif = true} (null diperlakukan
								 * sebagai aktif, demi data lama sebelum kolom itu ada) dan memakai
								 * {@code Restrictions.sqlRestriction("true")} sebagai kondisi netral ketika sebuah filter
								 * dikosongkan.
								 *
								 * <p><b>Sisi calon mahasiswa.</b> Dua kriteria disiapkan: {@code mhsbaru} =
								 * {@code prodiLulus IS NOT NULL} (dipakai untuk "Pendaftaran Ulang Mahasiswa Baru", yang menurut
								 * definisi sudah punya prodi kelulusan), dan {@code calonmhsbaru} untuk pendaftar yang belum
								 * tentu punya {@code prodiLulus} sehingga fakultas/prodi dicocokkan ke salah satu dari lima
								 * kolom pilihan {@code prodi1}..{@code prodi5} lewat rangkaian OR. Keenam relasi prodi
								 * di-{@code LEFT_JOIN} agar baris tanpa salah satu pilihan tidak hilang dari hasil.
								 *
								 * <p><b>Dua kuirk penyaringan calon mahasiswa yang perlu diketahui.</b> (1) Saat filter
								 * Fakultas atau Prodi diisi, {@code calonmhsbaru} dibentuk sebagai
								 * {@code Restrictions.and(mhsbaru, calonmhsbaruOr)} — artinya syarat
								 * {@code prodiLulus IS NOT NULL} ikut ditempelkan. Padahal rangkaian OR atas
								 * {@code prodi1}..{@code prodi5} justru ada untuk menjangkau pendaftar yang BELUM punya
								 * {@code prodiLulus}; begitu filter fakultas/prodi dipilih, pendaftar tersebut tersaring habis,
								 * sedangkan tanpa filter (kondisi netral {@code "true"}) mereka ikut terambil. (2) Blok Prodi
								 * menimpa penuh {@code calonmhsbaru} yang sudah dibentuk blok Fakultas alih-alih
								 * menggabungkannya, sehingga bila keduanya diisi hanya syarat Prodi yang berlaku — praktis tidak
								 * berdampak karena prodi sudah menyiratkan fakultasnya. Keduanya dicatat apa adanya di sini;
								 * penambalannya dilacak terpisah.
								 *
								 * <p><b>Sisi mahasiswa aktif.</b> Bila filter Status Mahasiswa diisi, {@link Criteria} akar
								 * DIGANTI menjadi query {@link HistoryStatusMahasiswa} yang disaring tahun akademik + ganjil/genap
								 * status (dan kolom {@code sp} sesuai {@code JenisKegiatan.getUntukBayarSP()}), di-{@code
								 * groupProperty("mahasiswa")}, lalu di-{@code createCriteria("mahasiswa")} sehingga hasil akhirnya
								 * tetap berupa {@link Mahasiswa}. Karena akarnya diganti, filter yang ditambahkan sesudahnya
								 * (kata kunci, rentang angkatan, prodi/fakultas) menempel pada kriteria mahasiswa hasil turunan
								 * itu — bukan pada query awal yang dibuang.
								 *
								 * <p><b>Session &amp; error.</b> Setiap cabang membuka session Hibernate sendiri dan menutupnya
								 * di {@code finally} lewat {@link #closeLocalSessionSafely} — thread ini berada di luar ZK
								 * execution sehingga tidak boleh memakai session request. Sukses ditandai {@code label.setValue("")};
								 * kegagalan apa pun ditangkap, dilaporkan lewat {@code Common.tampilErrorJikaAdmin}, dan ditandai
								 * {@code label.setValue("-")} supaya {@link Timer} berhenti dan indikator sibuk dibersihkan.
								 */
								@SuppressWarnings({})
								@Override
								public void run() {
									try {
										List<Mahasiswa> mahasiswas = new ArrayList<Mahasiswa>();
										List<BiodataCalonMahasiswa> biodataCalonMahasiswas = new ArrayList<BiodataCalonMahasiswa>();

										if ((ConstantValues.PENDAFTARAN_CALON_MAHASISWA != null
												&& j.getId().equals(ConstantValues.PENDAFTARAN_CALON_MAHASISWA.getId()))
												|| (ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU != null && j.getId()
														.equals(ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU
																.getId()))) {

											Criterion mhsbaru = Restrictions.isNotNull("prodiLulus");
											if (fak != null)
												mhsbaru = Restrictions.and(mhsbaru,
														Restrictions.eq("prodiLulus.fakultas", fak));
											if (jur != null)
												mhsbaru = Restrictions.and(mhsbaru, Restrictions.eq("prodiLulus", jur));

											Criterion calonmhsbaru = Restrictions.sqlRestriction("true");
											if (fak != null) {
												Criterion calonmhsbaruOr = Restrictions.eq("prodi1.fakultas", fak);
												calonmhsbaruOr = Restrictions.or(calonmhsbaruOr,
														Restrictions.eq("prodi2.fakultas", fak));
												calonmhsbaruOr = Restrictions.or(calonmhsbaruOr,
														Restrictions.eq("prodi3.fakultas", fak));
												calonmhsbaruOr = Restrictions.or(calonmhsbaruOr,
														Restrictions.eq("prodi4.fakultas", fak));
												calonmhsbaruOr = Restrictions.or(calonmhsbaruOr,
														Restrictions.eq("prodi5.fakultas", fak));
												calonmhsbaruOr = Restrictions.or(calonmhsbaruOr,
														Restrictions.eq("prodiLulus.fakultas", fak));
												calonmhsbaru = Restrictions.and(calonmhsbaru, calonmhsbaruOr);
											}

											if (jur != null) {
												Criterion calonmhsbaruOr = Restrictions.eq("prodi1", jur);
												calonmhsbaruOr = Restrictions.or(calonmhsbaruOr,
														Restrictions.eq("prodi2", jur));
												calonmhsbaruOr = Restrictions.or(calonmhsbaruOr,
														Restrictions.eq("prodi3", jur));
												calonmhsbaruOr = Restrictions.or(calonmhsbaruOr,
														Restrictions.eq("prodi4", jur));
												calonmhsbaruOr = Restrictions.or(calonmhsbaruOr,
														Restrictions.eq("prodi5", jur));
												calonmhsbaruOr = Restrictions.or(calonmhsbaruOr,
														Restrictions.eq("prodiLulus", jur));
												calonmhsbaru = Restrictions.and(calonmhsbaru, calonmhsbaruOr);
											}

											Session session = null;
											try {
												session = HibernateUtil.getSessionFactory().openSession();
												biodataCalonMahasiswas = ConstantValues
														.simpleList(
																session.createCriteria(BiodataCalonMahasiswa.class)
																		.add(Restrictions.or(
																				Restrictions.isNull("aktif"),
																				Restrictions.eq("aktif", true)))
																		.add(mhs.trim().isEmpty()
																				? Restrictions.sqlRestriction("true")
																				: Restrictions.or(
																						Restrictions.ilike("nama", mhs,
																								MatchMode.ANYWHERE),
																						Restrictions.or(
																								Restrictions.ilike(
																										"noRegistrasi",
																										mhs,
																										MatchMode.ANYWHERE),
																								Restrictions.ilike(
																										"noUjian", mhs,
																										MatchMode.ANYWHERE))))
																		.createAlias("prodiLulus", "prodiLulus",
																				Criteria.LEFT_JOIN)
																		.createAlias("prodi1", "prodi1",
																				Criteria.LEFT_JOIN)
																		.createAlias("prodi2", "prodi2",
																				Criteria.LEFT_JOIN)
																		.createAlias("prodi3", "prodi3",
																				Criteria.LEFT_JOIN)
																		.createAlias("prodi4", "prodi4",
																				Criteria.LEFT_JOIN)
																		.createAlias("prodi5", "prodi5",
																				Criteria.LEFT_JOIN)
																		.add(Restrictions.between("tahunAkademik", ta,
																				taSampai))
																		.add((ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU != null
																				&& j.getId().equals(
																						ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU
																								.getId())) ? mhsbaru
																										: calonmhsbaru),
																BiodataCalonMahasiswa.class);
											} finally {
												closeLocalSessionSafely(session);
											}
										} else {
											Session session = null;
											try {
												session = HibernateUtil.getSessionFactory().openSession();
												Criteria c = session.createCriteria(Mahasiswa.class)
														.add(Restrictions.or(Restrictions.isNull("aktif"),
																Restrictions.eq("aktif", true)));

												if (status != null) {
													c = session.createCriteria(HistoryStatusMahasiswa.class)
															.add(j != null && j.getUntukBayarSP()
																	? Restrictions.eq("sp", Perkuliahan.SEMESTER_PENDEK)
																	: Restrictions.isNull("sp"))
																	.add(Restrictions.eq("tahunAkademik", taStatus))
																	.add(Restrictions.eq("ganjilGenap", jenisSmtStatus))
															.setProjection(Projections.groupProperty("mahasiswa"))
															.createCriteria("mahasiswa");
												}

												mahasiswas = ConstantValues.simpleList(c.add(mhs.trim().isEmpty()
														? Restrictions.sqlRestriction("true")
														: Restrictions.or(
																Restrictions.ilike("nim", mhs, MatchMode.ANYWHERE),
																Restrictions.ilike("nama", mhs, MatchMode.ANYWHERE)))
														.add(Restrictions.between("tahunangkatan", ang, angSampai))
														.add(jur == null ? Restrictions.sqlRestriction("true")
																: Restrictions.eq("jurusan", jur))
														.createAlias("jurusan", "jurusan")
														.add(fak == null ? Restrictions.sqlRestriction("true")
																: Restrictions.eq("jurusan.fakultas", fak)),
														Mahasiswa.class);
											} finally {
												closeLocalSessionSafely(session);
											}
										}

										KegiatanHelper.doDownloadTagihan(filename, mahasiswas, biodataCalonMahasiswas,
												label, ta, taSampai, GANJIL, GENAP, colS, intbox, bul, bulMul, bulSam,
												j, ulang, rst, item);
										label.setValue("");

									} catch (Exception e) {
										Common.tampilErrorJikaAdmin(e);
										label.setValue("-");
									}
								}
							}).start();

						} catch (Exception e) {
							Common.tampilErrorJikaAdmin(e);
						}
					}
				});
				save.setParent(toolbar);

				window.onModal();
			}
		});

		return toolbarbutton;
	}

	/**
	 * Variant "sudah terkontekstualisasi" dari {@link #prosesDownloadTagihan(String, String)}:
	 * dipakai dari layar yang SUDAH punya {@link SettingBiaya} (menentukan {@code JenisKegiatan})
	 * dan {@link DataCriteria} pencarian siap pakai (dipanggil langsung
	 * {@code criteria.initCriteria(true)} untuk mendapatkan {@link Criteria} ber-entitas
	 * Mahasiswa atau BiodataCalonMahasiswa, tergantung jenis kegiatan) — TIDAK menampilkan popup
	 * filter tambahan, hanya validasi {@code SettingBiaya.getJenisKegiatan() != null} lalu
	 * langsung menjalankan {@link #doDownloadTagihan} asinkron dengan tahun tunggal ({@code ta}
	 * dipakai sebagai awal MAUPUN akhir rentang — bukan rentang multi-tahun), {@code hitungUlang}
	 * selalu {@code true} dan {@code rst} selalu {@code false}. Progress/preview popup memakai
	 * mekanisme timer-polling yang sama dengan overload lain.
	 *
	 * @param buttonLabel   label tombol
	 * @param buttonImage   path ikon tombol
	 * @param tahunAkademik combobox tahun akademik yang sudah dipilih pengguna di layar pemanggil
	 * @param jenisSmt      combobox Ganjil/Genap terpilih
	 * @param settingBiaya  sumber {@link JenisKegiatan} yang diproses
	 * @param criteria      kriteria pencarian siap pakai dari layar pemanggil (Mahasiswa atau BiodataCalonMahasiswa)
	 * @param item          batasi ke satu {@link ItemBiaya}, atau {@code null} untuk semua
	 * @return tombol toolbar siap dipasang ke UI
	 */
	public static MyToolbarbuttonConfig prosesDownloadTagihan(String buttonLabel, String buttonImage,
			final Combobox tahunAkademik, final Combobox jenisSmt, final SettingBiaya settingBiaya,
			final DataCriteria criteria, final ItemBiaya item) {

		MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig(buttonLabel, buttonImage);

		toolbarbutton.addEventListener("onClick", new EventListener() {
			/**
			 * Menangani klik tombol: langsung menjalankan pembentukan berkas tagihan tanpa popup filter,
			 * karena seluruh konteks sudah diterima dari layar pemanggil.
			 *
			 * <p><b>Beda dengan overload manual.</b> Tidak ada dialog yang dirakit di sini — nilai yang
			 * pada {@link #prosesDownloadTagihan(String, String)} ditanyakan lewat popup, di sini diambil
			 * dari parameter atau dipatok tetap: {@code j} dari {@code settingBiaya.getJenisKegiatan()},
			 * Ganjil/Genap dari combobox {@code jenisSmt} pemanggil, sedangkan tagihan bulanan dimatikan
			 * ({@code bul=false}, {@code bulMul=0}, {@code bulSam=12}), {@code ulang} dipatok
			 * {@code true} (selalu hitung ulang) dan {@code rst} dipatok {@code false} (tidak pernah
			 * reset ke billing). Satu combobox {@code tahunAkademik} dibaca DUA KALI menjadi {@code ta}
			 * dan {@code taSampai}, sehingga rentangnya selalu satu tahun akademik — bukan rentang
			 * multi-tahun seperti overload manual.
			 *
			 * <p>Satu-satunya validasi adalah {@code j != null} (pesannya memakai istilah lama "Jadwal
			 * Pembayaran harus diisi"). Sesudah itu mekanisme selanjutnya identik dengan overload manual:
			 * {@link Label} + dua {@link Intbox} sebagai kanal status lintas thread, berkas
			 * {@code /tmp/cetak_data_&lt;timestamp&gt;.xlsx} dibuat kosong lebih dulu, {@link Timer} 200 ms
			 * memantau, dan {@code new Thread(...)} mengerjakan query serta {@link #doDownloadTagihan}.
			 *
			 * @param arg0 event {@code onClick}; tidak dibaca
			 * @throws Exception diteruskan dari pembuatan berkas sementara/{@code URLEncoder}
			 */
			@Override
			public void onEvent(Event arg0) throws Exception {

				final String ta = (String) tahunAkademik.getSelectedItem().getValue();
				final String taSampai = (String) tahunAkademik.getSelectedItem().getValue();

				final Boolean bul = false;
				final Integer bulMul = 0;
				final Integer bulSam = 12;

				final JenisKegiatan j = settingBiaya.getJenisKegiatan();

				final boolean GANJIL = jenisSmt.getSelectedItem().getValue().equals(Perkuliahan.GANJIL);
				final boolean GENAP = jenisSmt.getSelectedItem().getValue().equals(Perkuliahan.GENAP);

				final Boolean ulang = true;
				final boolean rst = false;

				if (j == null) {
					MyMessageboxConfig.show("Jadwal Pembayaran harus diisi", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return;
				}

				final Label label = new Label(ais.common.Common.getBahasaConfig("Proses load data .."));
				final Intbox intbox = new Intbox(10);
				final Intbox colS = new Intbox(10);
				Clients.showBusy(label.getValue());

				final String filename = Sessions.getCurrent().getWebApp()
						.getRealPath("/tmp/cetak_data_"
								+ URLEncoder.encode(
										Common.datetimeFormat2s.get().format(ais.ui.util.WaktuUtil.getDate()), "UTF-8")
								+ ".xlsx");
				final File file = new File(filename);
				file.createNewFile();

				final Timer timer = new Timer(200);
				timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				timer.setRepeats(true);
				timer.addEventListener("onTimer", new EventListener() {
					/**
					 * Denyut {@link Timer} 200 ms pemantau {@link Label} status thread latar — perilakunya sama
					 * persis dengan padanannya di {@link #prosesDownloadTagihan(String, String)}: {@code "-"}
					 * menutup indikator sibuk dan melepas timer, {@code ""} membuka jendela pratinjau "Cetak Data"
					 * berisi {@link Spreadsheet} atas berkas hasil, nilai lain hanya menyegarkan indikator sibuk.
					 * Kegagalan apa pun hanya memanggil {@code Clients.clearBusy()} tanpa melepas timer.
					 *
					 * @param arg0 event {@code onTimer}; tidak dibaca
					 * @throws Exception dideklarasikan {@link EventListener}; secara praktis tertangkap di dalam
					 */
					@Override
					public void onEvent(Event arg0) throws Exception {
						try {
							Clients.showBusy(label.getValue());

							if (label.getValue().trim().equalsIgnoreCase("-")) {
								Clients.clearBusy();
								timer.detach();
							} else if (label.getValue().isEmpty()) {

								Center center = new Center();
								final MyWindow window = new MyWindow("Cetak Data", "none", true);
								window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
								window.setHeight("97%");
								window.setWidth("90%");

								Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
								borderlayout.setParent(window);

								ais.ui.util.ZkCompat.setFlex(center, true);
								center.setParent(borderlayout);

								Common.clear(center);
								Spreadsheet spreadsheet = new ais.ui.util.MySpreadsheet();
								Common.clear(center);
								spreadsheet.setParent(center);
								spreadsheet.setWidth("100%");
								spreadsheet.setHeight("100%");
								spreadsheet.setSrc("../../tmp/" + file.getName());

								spreadsheet.setMaxrows(intbox.getValue() + 3);
								spreadsheet.setMaxcolumns(colS.getValue());
								ais.ui.util.PratinjauXlsxHelper.gantiSpreadsheetDenganGrid(spreadsheet);

								South south = new South();
								south.setParent(borderlayout);

								Toolbar toolbar = new Toolbar();
								toolbar.setParent(south);
								MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
								cancel.setTooltiptext("Tutup");
								cancel.addEventListener("onClick", new EventListener() {
									/**
									 * Menutup jendela pratinjau "Cetak Data" ({@code window.detach()}). Berkas hasil di
									 * {@code /tmp} tidak ikut dihapus.
									 *
									 * @param event event {@code onClick} tombol "Tutup"; tidak dibaca
									 * @throws Exception dipersyaratkan {@link EventListener}; tidak dilempar di sini
									 */
									@Override
									public void onEvent(Event event) throws Exception {
										window.detach();
									}
								});
								cancel.setParent(toolbar);

								MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Download Data",
										"/img/excel.png");
								print.addEventListener("onClick", new EventListener() {
									/**
									 * Mengirim berkas .xlsx hasil ke browser lewat {@link Filedownload} dengan MIME type
									 * spreadsheet OpenXML, dibaca ulang dari disk. Kegagalan pengiriman ditelan dan hanya dicatat
									 * ke {@code ErrorAuditUtil} karena pratinjau di layar tetap utuh.
									 *
									 * @param event event {@code onClick} tombol "Download Data"; tidak dibaca
									 * @throws Exception dideklarasikan {@link EventListener}; kegagalan nyata tertangkap di dalam
									 */
									@Override
									public void onEvent(Event event) throws Exception {
										try {
											Filedownload.save(new FileInputStream(file),
													"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
													file.getName());
										} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/KegiatanHelper.java:2537");
										}
									}
								});
								print.setParent(toolbar);

								window.setVisible(true);
								window.onModal();

								Clients.clearBusy();
								timer.detach();
							}

						} catch (Exception e) {
							Clients.clearBusy();
						}
					}
				});
				timer.start();

				try {
					Clients.showBusy(label.getValue());

					new Thread(new Runnable() {
						/**
						 * Badan thread latar: menjalankan {@link DataCriteria} milik layar pemanggil, lalu menyerahkan
						 * hasilnya ke {@link #doDownloadTagihan}.
						 *
						 * <p>Jauh lebih ringkas daripada padanannya di {@link #prosesDownloadTagihan(String, String)}
						 * karena TIDAK menyusun kriteria sendiri: {@code criteria.initCriteria(true)} dipanggil apa
						 * adanya, sehingga penyaringan (termasuk cakupan yang berlaku di layar) sepenuhnya mengikuti
						 * apa yang sudah dipakai grid pemanggil. Hasilnya di-cast ke {@link BiodataCalonMahasiswa} bila
						 * {@link JenisKegiatan} terpilih adalah {@code PENDAFTARAN_CALON_MAHASISWA} atau
						 * {@code PENDAFTARAN_ULANG_MAHASISWA_BARU}, selain itu ke {@link Mahasiswa} — pemanggil wajib
						 * memastikan entity akar {@link DataCriteria}-nya cocok dengan jenis kegiatan yang dioper,
						 * karena ketidakcocokan baru ketahuan sebagai kegagalan cast saat berjalan.
						 *
						 * <p>Karena {@code initCriteria(true)} dieksekusi di dalam thread ini (di luar ZK execution dan
						 * di luar transaksi request), pemanggil harus memastikan {@link DataCriteria} yang dioper aman
						 * dipakai dari thread lain. Sukses ditandai {@code label.setValue("")}; kegagalan dilaporkan
						 * lewat {@code Common.tampilErrorJikaAdmin} dan ditandai {@code label.setValue("-")}.
						 */
						@SuppressWarnings({})
						@Override
						public void run() {
							try {
								List<Mahasiswa> mahasiswas = new ArrayList<Mahasiswa>();
								List<BiodataCalonMahasiswa> biodataCalonMahasiswas = new ArrayList<BiodataCalonMahasiswa>();

								if ((ConstantValues.PENDAFTARAN_CALON_MAHASISWA != null
										&& j.getId().equals(ConstantValues.PENDAFTARAN_CALON_MAHASISWA.getId()))
										|| (ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU != null && j.getId()
												.equals(ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU.getId()))) {
									biodataCalonMahasiswas = ConstantValues.simpleList(
											(Criteria) criteria.initCriteria(true), BiodataCalonMahasiswa.class);
								} else {
									mahasiswas = ConstantValues.simpleList((Criteria) criteria.initCriteria(true),
											Mahasiswa.class);
								}

								KegiatanHelper.doDownloadTagihan(filename, mahasiswas, biodataCalonMahasiswas, label,
										ta, taSampai, GANJIL, GENAP, colS, intbox, bul, bulMul, bulSam, j, ulang, rst,
										item);

								label.setValue("");
							} catch (Exception e) {
								Common.tampilErrorJikaAdmin(e);
								label.setValue("-");
							}
						}
					}).start();

				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			}
		});

		return toolbarbutton;
	}

	/**
	 * Padanan {@link #prosesDownloadTagihan(String, String, Combobox, Combobox, SettingBiaya, DataCriteria, ItemBiaya)}
	 * KHUSUS untuk grid "khusus per mahasiswa" (SettingBiaya.khususBuatMahasiswaTertentu=true)
	 * di {@code DetailSettingBiayaAction} — di sana {@code initCriteria()} layar mengembalikan
	 * Criteria ber-entitas {@link SettingBiayaDetail} (bukan Mahasiswa/BiodataCalonMahasiswa
	 * langsung seperti mode reguler), sehingga overload aslinya tidak bisa dipakai apa adanya.
	 * <p>
	 * Method ini menerima {@code criteriaSettingBiayaDetail} (Criteria ber-entitas
	 * SettingBiayaDetail, sudah difilter sesuai pencarian aktif di layar), memetakan
	 * {@code getMahasiswa()}/{@code getBiodataCalonMahasiswa()} tiap barisnya menjadi daftar
	 * yang sama dipakai {@link #doDownloadTagihan} — sehingga format Excel yang dihasilkan
	 * (kolom ID TAGIHAN, NOMINAL TAGIHAN, dst) TETAP kompatibel dengan
	 * {@link #prosesUploadTagihan} yang sudah ada tanpa perubahan apa pun di sana. Inilah
	 * yang memungkinkan admin mengubah nominal {@code DetailKegiatan} mahasiswa "custom"
	 * (mis. tagihan Sumbangan Bangunan Tahap II diturunkan sesuai kesanggupan mahasiswa)
	 * satu-per-satu (lewat kolom "Nominal Tagihan Aktif" di grid) MAUPUN massal via Excel —
	 * persis seperti mode reguler.
	 *
	 * @param j jenis kegiatan (dipakai menentukan apakah daftar berupa Mahasiswa atau
	 *          BiodataCalonMahasiswa, dan diteruskan ke {@link #doDownloadTagihan}).
	 */
	public static MyToolbarbuttonConfig prosesDownloadTagihanUntukSettingBiayaDetail(String buttonLabel,
			String buttonImage, final Combobox tahunAkademik, final Combobox jenisSmt, final JenisKegiatan j,
			final DataCriteria criteriaSettingBiayaDetail, final ItemBiaya item) {

		MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig(buttonLabel, buttonImage);

		toolbarbutton.addEventListener("onClick", new EventListener() {
			/**
			 * Menangani klik tombol: menjalankan pembentukan berkas tagihan untuk grid "khusus per
			 * mahasiswa" tanpa popup filter.
			 *
			 * <p>Persiapannya identik dengan padanannya di
			 * {@link #prosesDownloadTagihan(String, String, Combobox, Combobox, SettingBiaya, DataCriteria, ItemBiaya)}
			 * — tagihan bulanan dimatikan, {@code ulang} dipatok {@code true}, {@code rst} dipatok
			 * {@code false}, combobox {@code tahunAkademik} dibaca dua kali sehingga rentangnya selalu satu
			 * tahun akademik, dan Ganjil/Genap diambil dari {@code jenisSmt}. Satu-satunya beda pada tahap
			 * ini: {@link JenisKegiatan} datang sebagai parameter langsung, bukan diturunkan dari
			 * {@link SettingBiaya}, karena layar khusus-per-mahasiswa tidak selalu punya {@code SettingBiaya}
			 * tunggal untuk dijadikan acuan. Validasinya tetap {@code j != null}.
			 *
			 * @param arg0 event {@code onClick}; tidak dibaca
			 * @throws Exception diteruskan dari pembuatan berkas sementara/{@code URLEncoder}
			 */
			@Override
			public void onEvent(Event arg0) throws Exception {

				final String ta = (String) tahunAkademik.getSelectedItem().getValue();
				final String taSampai = (String) tahunAkademik.getSelectedItem().getValue();

				final Boolean bul = false;
				final Integer bulMul = 0;
				final Integer bulSam = 12;

				final boolean GANJIL = jenisSmt.getSelectedItem().getValue().equals(Perkuliahan.GANJIL);
				final boolean GENAP = jenisSmt.getSelectedItem().getValue().equals(Perkuliahan.GENAP);

				final Boolean ulang = true;
				final boolean rst = false;

				if (j == null) {
					MyMessageboxConfig.show("Jadwal Pembayaran harus diisi", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return;
				}

				final Label label = new Label(ais.common.Common.getBahasaConfig("Proses load data .."));
				final Intbox intbox = new Intbox(10);
				final Intbox colS = new Intbox(10);
				Clients.showBusy(label.getValue());

				final String filename = Sessions.getCurrent().getWebApp()
						.getRealPath("/tmp/cetak_data_"
								+ URLEncoder.encode(
										Common.datetimeFormat2s.get().format(ais.ui.util.WaktuUtil.getDate()), "UTF-8")
								+ ".xlsx");
				final File file = new File(filename);
				file.createNewFile();

				final Timer timer = new Timer(200);
				timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				timer.setRepeats(true);
				timer.addEventListener("onTimer", new EventListener() {
					/**
					 * Denyut {@link Timer} 200 ms pemantau {@link Label} status thread latar; perilakunya sama
					 * persis dengan padanannya pada dua overload {@code prosesDownloadTagihan} lain.
					 *
					 * @param arg0 event {@code onTimer}; tidak dibaca
					 * @throws Exception dideklarasikan {@link EventListener}; secara praktis tertangkap di dalam
					 */
					@Override
					public void onEvent(Event arg0) throws Exception {
						try {
							Clients.showBusy(label.getValue());

							if (label.getValue().trim().equalsIgnoreCase("-")) {
								Clients.clearBusy();
								timer.detach();
							} else if (label.getValue().isEmpty()) {

								Center center = new Center();
								final MyWindow window = new MyWindow("Cetak Data", "none", true);
								window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
								window.setHeight("97%");
								window.setWidth("90%");

								Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
								borderlayout.setParent(window);

								ais.ui.util.ZkCompat.setFlex(center, true);
								center.setParent(borderlayout);

								Common.clear(center);
								Spreadsheet spreadsheet = new ais.ui.util.MySpreadsheet();
								Common.clear(center);
								spreadsheet.setParent(center);
								spreadsheet.setWidth("100%");
								spreadsheet.setHeight("100%");
								spreadsheet.setSrc("../../tmp/" + file.getName());

								spreadsheet.setMaxrows(intbox.getValue() + 3);
								spreadsheet.setMaxcolumns(colS.getValue());
								ais.ui.util.PratinjauXlsxHelper.gantiSpreadsheetDenganGrid(spreadsheet);

								South south = new South();
								south.setParent(borderlayout);

								Toolbar toolbar = new Toolbar();
								toolbar.setParent(south);
								MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
								cancel.setTooltiptext("Tutup");
								cancel.addEventListener("onClick", new EventListener() {
									/**
									 * Menutup jendela pratinjau "Cetak Data" ({@code window.detach()}). Berkas hasil di
									 * {@code /tmp} tidak ikut dihapus.
									 *
									 * @param event event {@code onClick} tombol "Tutup"; tidak dibaca
									 * @throws Exception dipersyaratkan {@link EventListener}; tidak dilempar di sini
									 */
									@Override
									public void onEvent(Event event) throws Exception {
										window.detach();
									}
								});
								cancel.setParent(toolbar);

								MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Download Data",
										"/img/excel.png");
								print.addEventListener("onClick", new EventListener() {
									/**
									 * Mengirim berkas .xlsx hasil ke browser lewat {@link Filedownload} dengan MIME type
									 * spreadsheet OpenXML, dibaca ulang dari disk. Kegagalan pengiriman ditelan dan hanya dicatat
									 * ke {@code ErrorAuditUtil}.
									 *
									 * @param event event {@code onClick} tombol "Download Data"; tidak dibaca
									 * @throws Exception dideklarasikan {@link EventListener}; kegagalan nyata tertangkap di dalam
									 */
									@Override
									public void onEvent(Event event) throws Exception {
										try {
											Filedownload.save(new FileInputStream(file),
													"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
													file.getName());
										} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/KegiatanHelper.java:prosesDownloadTagihanUntukSettingBiayaDetail1"); }
									}
								});
								print.setParent(toolbar);

								window.setVisible(true);
								window.onModal();

								Clients.clearBusy();
								timer.detach();
							}

						} catch (Exception e) {
							Clients.clearBusy();
						}
					}
				});
				timer.start();

				try {
					Clients.showBusy(label.getValue());

					new Thread(new Runnable() {
						/**
						 * Badan thread latar: menerjemahkan baris {@link SettingBiayaDetail} menjadi daftar
						 * {@link Mahasiswa}/{@link BiodataCalonMahasiswa}, lalu menyerahkannya ke
						 * {@link #doDownloadTagihan}.
						 *
						 * <p><b>Inilah alasan overload ini ada.</b> Pada grid khusus-per-mahasiswa
						 * ({@code SettingBiaya.khususBuatMahasiswaTertentu=true}), {@code initCriteria()} layar
						 * mengembalikan {@link Criteria} ber-entitas {@link SettingBiayaDetail} — bukan
						 * Mahasiswa/BiodataCalonMahasiswa seperti mode reguler — sehingga hasilnya tidak bisa langsung
						 * dioper ke {@link #doDownloadTagihan}. Thread ini menjalankan kriteria tersebut lebih dulu,
						 * lalu memetakan tiap baris lewat {@code getMahasiswa()} atau {@code getBiodataCalonMahasiswa()}
						 * sesuai {@link JenisKegiatan} yang dioper. Dengan begitu format Excel yang dihasilkan tetap
						 * identik dan tetap berpasangan dengan {@link #prosesUploadTagihan} tanpa perubahan apa pun di
						 * sana.
						 *
						 * <p><b>Baris tanpa pasangan dilewati diam-diam.</b> Penjagaan {@code != null} pada kedua
						 * cabang berarti {@link SettingBiayaDetail} yang kolom mahasiswa/calon mahasiswanya kosong —
						 * atau yang terisi pada kolom yang TIDAK sesuai jenis kegiatan terpilih (mis. baris berisi
						 * {@code mahasiswa} padahal jenisnya "Pendaftaran Calon Mahasiswa") — tidak ikut terproses dan
						 * tidak dilaporkan sebagai kesalahan. Berkas hasil akan lebih pendek daripada jumlah baris grid
						 * tanpa penjelasan apa pun.
						 *
						 * <p>Sukses ditandai {@code label.setValue("")}; kegagalan dilaporkan lewat
						 * {@code Common.tampilErrorJikaAdmin} dan ditandai {@code label.setValue("-")} supaya
						 * {@link Timer} berhenti.
						 */
						@SuppressWarnings({})
						@Override
						public void run() {
							try {
								List<Mahasiswa> mahasiswas = new ArrayList<Mahasiswa>();
								List<BiodataCalonMahasiswa> biodataCalonMahasiswas = new ArrayList<BiodataCalonMahasiswa>();

								List<SettingBiayaDetail> details = ConstantValues.simpleList(
										(Criteria) criteriaSettingBiayaDetail.initCriteria(true),
										SettingBiayaDetail.class);

								if ((ConstantValues.PENDAFTARAN_CALON_MAHASISWA != null
										&& j.getId().equals(ConstantValues.PENDAFTARAN_CALON_MAHASISWA.getId()))
										|| (ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU != null && j.getId()
												.equals(ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU.getId()))) {
									for (SettingBiayaDetail d : details) {
										if (d.getBiodataCalonMahasiswa() != null) {
											biodataCalonMahasiswas.add(d.getBiodataCalonMahasiswa());
										}
									}
								} else {
									for (SettingBiayaDetail d : details) {
										if (d.getMahasiswa() != null) {
											mahasiswas.add(d.getMahasiswa());
										}
									}
								}

								KegiatanHelper.doDownloadTagihan(filename, mahasiswas, biodataCalonMahasiswas, label,
										ta, taSampai, GANJIL, GENAP, colS, intbox, bul, bulMul, bulSam, j, ulang, rst,
										item);

								label.setValue("");
							} catch (Exception e) {
								Common.tampilErrorJikaAdmin(e);
								label.setValue("-");
							}
						}
					}).start();

				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			}
		});

		return toolbarbutton;
	}
}
