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

public class KegiatanHelper {
	public static boolean prosestagihan = false;

	// ===================================================================================
	// TRANSACTION & SESSION SAFE UTILITIES (MENCEGAH ASSERTION FAILURE & MEMORY
	// LEAK)
	// ===================================================================================


	private static boolean isUsableSession(Session session) {
		try {
			return session != null && session.isOpen();
		} catch (Exception e) {
			return false;
		}
	}

	private static Session openIsolatedSession() {
		return HibernateUtil.getSessionFactory().openSession();
	}

	private static void terapkanLockTimeout(Session session) {
		// Batasi WAKTU TUNGGU LOCK (bukan durasi query) pada transaksi berjalan. Tanpa ini, UPDATE
		// yang menunggu baris "kegiatan" terkunci transaksi lain akan MENGGANTUNG sampai
		// statement_timeout memicu "canceling statement due to statement timeout ... while updating
		// tuple ... in relation kegiatan" (57014) — request lama & koneksi c3p0 tertahan. Dengan
		// lock_timeout, tunggu lock dibatasi lalu GAGAL CEPAT (55P03) sehingga retry di sesi bersih
		// (lihat updateEntitySafe) menjadi efektif & koneksi cepat bebas. SET LOCAL hanya berlaku
		// untuk transaksi ini. Best-effort (diam bila bukan PostgreSQL / tanpa transaksi aktif).
		try {
			session.createSQLQuery("SET LOCAL lock_timeout = '5000ms'").executeUpdate();
		} catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/helper/KegiatanHelper.java:118");
		}
	}

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

	private static void updateEntitySafe(Session session, Object entity) throws Exception {
		updateEntitySafe(session, entity, 0);
	}

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
					try { if (isNewTx && tx != null && tx.isActive()) tx.rollback(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/KegiatanHelper.java:217");}
					try { if (isUsableSession(session)) session.clear(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/KegiatanHelper.java:218");}
					try { if (isUsableSession(session)) session.close(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/KegiatanHelper.java:219");}
					session = openIsolatedSession();
					closeLocalSession = true;
					tx = session.beginTransaction();
					isNewTx = true;
					Session isoSession2 = null;
					Transaction isoTx2 = null;
					try {
						isoSession2 = openIsolatedSession();
						isoTx2 = isoSession2.beginTransaction();
						isoSession2.merge(entity);
						isoSession2.flush();
						isoTx2.commit();
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
			boolean transaksiMati = isLockTimeout(e) || isTransactionAborted(e);
			// isConstraintViolation() (unique/FK, mis. Mahasiswa.nimkey) JUGA meng-ABORT transaksi
			// PostgreSQL persis seperti lock timeout, jadi transaksi milik pemanggil tetap harus
			// dipulihkan -- tapi TIDAK ikut transaksiMati (dipisah dari kondisi retry di bawah): retry
			// merge 3x pada pelanggaran unique constraint yang genuinely permanen (bukan kontensi
			// sesaat) hanya membuang waktu karena akan gagal identik setiap kali.
			boolean transaksiPerluDipulihkan = transaksiMati || isConstraintViolation(e);
			if (isNewTx) {
				if (tx != null && tx.isActive()) {
					try { tx.rollback(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/KegiatanHelper.java:251");}
				}
			} else if (transaksiPerluDipulihkan) {
				pulihkanTransaksiTerabort(session, tx);
			}
			try { if (isUsableSession(session)) session.clear(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/KegiatanHelper.java:253");}
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
					if (isLockTimeout(retryEx) || isTransactionAborted(retryEx)) {
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
			ais.common.ErrorAuditUtil.record(ex,
					"KegiatanHelper.pulihkanTransaksiTerabort: gagal rollback transaksi ter-abort milik pemanggil");
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
			ais.common.ErrorAuditUtil.record(ex,
					"KegiatanHelper.pulihkanTransaksiTerabort: gagal membuka transaksi baru di sesi pemanggil");
		}
	}

	private static void executeUpdateSafe(Session session, String sql, Map<String, Object> params) {
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

	// ===================================================================================
	// PUBLIC BUSINESS LOGIC METHODS
	// ===================================================================================

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

	public static Kegiatan checkKegiatanCalonMahasiswa(JenisKegiatan jenisKegiatan,
			BiodataCalonMahasiswa biodataCalonMahasiswa, Integer smt, String ta, Boolean hitungUlang,
			JadwalPembayaran jadwal, boolean rst, ItemBiaya item, Session session) {

		return checkKegiatanCalonMahasiswa(null, jenisKegiatan, biodataCalonMahasiswa, smt, ta, hitungUlang, jadwal,
				rst, false, item, session);
	}

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
				Map<String, Long> mapkeg = Kegiatan.mappingId.get(kegiatan.getId());
				if (mapkeg != null)
					mapkeg.clear();

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
						Kegiatan kegiatanSudahAda = (Kegiatan) session.createCriteria(Kegiatan.class)
								.add(Restrictions.eq("kodeunik", kodeunikRetry)).setMaxResults(1)
								.addOrder(Order.asc("id")).uniqueResult();
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
				List<CicilanPembayaran> cicilanPembayarans = KegiatanPersistenceHelper.ambilCicilan(kegiatan,
						hitungUlang);

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

	public static void updateBatasStudiMahasiswa(Mahasiswa mahasiswa, Session session, Integer smt,
			boolean checkStatusPembayaranMahasiswa) {
		updateBatasStudiMahasiswa(mahasiswa, session, smt, checkStatusPembayaranMahasiswa, false);
	}

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

	public static Kegiatan checkKegiatanMahasiswa(JenisKegiatan jenisKegiatan, Mahasiswa mahasiswa, Integer smt,
			String ta, Boolean hitungUlang, JadwalPembayaran jadwal, boolean rst, ItemBiaya item, Session session) {

		return checkKegiatanMahasiswa(null, jenisKegiatan, mahasiswa, smt, ta, hitungUlang, jadwal, rst, false, item,
				session);
	}

	public static Kegiatan checkKegiatanMahasiswa(Kegiatan kegiatan, JenisKegiatan jenisKegiatan, Mahasiswa mahasiswa,
			Integer smt, String ta, Boolean hitungUlang, JadwalPembayaran jadwal, boolean rst,
			boolean diubahSaatpembayaran, ItemBiaya item, Session session) {

		if (jadwal != null || hitungUlang) {
			if (kegiatan == null || kegiatan.getId() == null) {
				kegiatan = mahasiswa.ambilKegiatans(smt, jenisKegiatan, true);
			}

			if (kegiatan != null && kegiatan.getId() != null && rst) {
				Map<String, Long> mapkeg = Kegiatan.mappingId.get(kegiatan.getId());
				if (mapkeg != null)
					mapkeg.clear();

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
					// FIX (KE-32): sebelumnya ditelan tanpa audit -- constraint violation (mis.
					// mahasiswa_nimkey_key dobel akibat entity Mahasiswa ter-cascade save/update
					// bersamaan dari proses lain) tak pernah tercatat, hanya tampil ke admin bila
					// sedang online. Catat agar terlacak, konsisten dgn pola di seluruh file ini.
					Common.tampilErrorJikaAdmin(e);
					ais.common.ErrorAuditUtil.record(e,
							"checkKegiatanMahasiswa: gagal simpan Kegiatan baru untuk mahasiswa="
									+ (mahasiswa == null ? "null" : mahasiswa.getId()));
				}

			} else if (kegiatan != null && hitungUlang) {
				try {
					if (session != null && session.isOpen() && session.contains(kegiatan))
						session.refresh(kegiatan);
					mahasiswa.populateKegiatan(kegiatan.getId());
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/KegiatanHelper.java:739");
				}

				List<CicilanPembayaran> cicilanPembayarans = KegiatanPersistenceHelper.ambilCicilan(kegiatan,
						hitungUlang);

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

					HistoryStatusMahasiswa historyStatusMahasiswa = Common.currentStatusSp(mahasiswa,
							kegiatan.getTahunAkademik(), kegiatan.getSemster(),
							jenisKegiatan != null && jenisKegiatan.getUntukBayarSP() ? Perkuliahan.SEMESTER_PENDEK
									: null);

					if (historyStatusMahasiswa != null) {
						historyStatusMahasiswa.put(String.valueOf(checkStatusPembayaranMahasiswa),
								"checkStatusPembayaranMahasiswa");
						historyStatusMahasiswa.setStatusMahasiswa(
								checkStatusPembayaranMahasiswa ? ConstantValues.AKTIF : ConstantValues.TIDAK_AKTIF);
					}
				}
			}
		}
		return kegiatan;
	}

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

	public static MyToolbarbuttonConfig prosesUploadTagihan(String buttonLabel, String buttonImage,
			final EventListener eventListener) {

		MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig(buttonLabel, buttonImage);
		toolbarbutton.setUpload(Common.ukuranFileUpload());
		toolbarbutton.addEventListener("onUpload", new EventListener() {

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
									}
								} catch (Exception e) {
									e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/KegiatanHelper.java:1437");
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

	public static MyToolbarbuttonConfig prosesDownloadTagihan(String buttonLabel, String buttonImage) {
		MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig(buttonLabel, buttonImage);

		toolbarbutton.addEventListener("onClick", new EventListener() {
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
					@Override
					public void onEvent(Event event) throws Exception {
						window.detach();
					}
				});
				cancel.setParent(toolbar);

				MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Download Tagihan", "/img/save.gif");
				save.setTooltiptext("Proses");
				save.addEventListener("onClick", new EventListener() {
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
											@Override
											public void onEvent(Event event) throws Exception {
												window.detach();
											}
										});
										cancel.setParent(toolbar);

										MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Download Data",
												"/img/excel.png");
										print.addEventListener("onClick", new EventListener() {
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
												calonmhsbaru = Restrictions.and(mhsbaru, calonmhsbaruOr);
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
												calonmhsbaru = Restrictions.and(mhsbaru, calonmhsbaruOr);
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

	public static MyToolbarbuttonConfig prosesDownloadTagihan(String buttonLabel, String buttonImage,
			final Combobox tahunAkademik, final Combobox jenisSmt, final SettingBiaya settingBiaya,
			final DataCriteria criteria, final ItemBiaya item) {

		MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig(buttonLabel, buttonImage);

		toolbarbutton.addEventListener("onClick", new EventListener() {
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
									@Override
									public void onEvent(Event event) throws Exception {
										window.detach();
									}
								});
								cancel.setParent(toolbar);

								MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Download Data",
										"/img/excel.png");
								print.addEventListener("onClick", new EventListener() {
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
									@Override
									public void onEvent(Event event) throws Exception {
										window.detach();
									}
								});
								cancel.setParent(toolbar);

								MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Download Data",
										"/img/excel.png");
								print.addEventListener("onClick", new EventListener() {
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
