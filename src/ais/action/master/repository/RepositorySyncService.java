package ais.action.master.repository;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.DspaceInformation;
import ais.database.model.repository.RepoCollection;
import ais.database.model.repository.RepoItem;
import ais.database.model.repository.RepoItemMetadata;
import ais.ui.dspace.DspaceCommon;

public class RepositorySyncService {

	public static final String STATUS_SYNCED = "SYNCED";
	public static final String STATUS_FAILED = "FAILED";
	public static final String STATUS_DRAFT = "DRAFT";

	private static final int DEFAULT_BATCH_SIZE = 25;
	private static final long REPOSITORY_SYNC_LOCK_ID = 130720260819L;

	/**
	 * KE-FIX ("ERROR: permission denied for schema library"). Galat IZIN adalah masalah
	 * KONFIGURASI database (role aplikasi belum diberi GRANT USAGE pada schema tsb), bukan
	 * galat sesaat. Tanpa penanda, siklus scheduler yang berjalan tiap beberapa menit akan
	 * mengulang kegagalan yang sama selamanya dan membanjiri audit error dengan stack trace
	 * identik sehingga galat lain yang benar-benar baru jadi tenggelam.
	 *
	 * <p>Sumber yang gagal karena izin ditunda selama {@link #JEDA_IZIN_DITOLAK_MS} lalu
	 * DICOBA LAGI otomatis -- jadi begitu administrator menjalankan GRANT-nya, sinkronisasi
	 * pulih sendiri tanpa perlu restart aplikasi. Sumber LAIN pada siklus yang sama tetap
	 * diproses seperti biasa.</p>
	 */
	private static final long JEDA_IZIN_DITOLAK_MS = 6L * 60L * 60L * 1000L;

	/** kunci sumber -> epoch milidetik paling awal sumber itu boleh dicoba lagi. */
	private static final java.util.Map<String, Long> SUMBER_DITUNDA_IZIN =
			java.util.Collections.synchronizedMap(new java.util.HashMap<String, Long>());

	private static class SourceDescriptor {
		private String label;
		private String collectionCode;
		private String collectionName;
		private String modelClassName;
		private String actionClassName;
		private String documentType;
		private String eligibility;

		SourceDescriptor(String label, String collectionCode, String collectionName, String modelClassName,
				String actionClassName, String documentType, String eligibility) {
			this.label = label;
			this.collectionCode = collectionCode;
			this.collectionName = collectionName;
			this.modelClassName = modelClassName;
			this.actionClassName = actionClassName;
			this.documentType = documentType;
			this.eligibility = eligibility;
		}
	}

	public static class SyncSummary {
		private int scanned;
		private int synced;
		private int failed;
		private String message = "";
		// KE-FIX: begitu koneksi JDBC fisik terputus (SSL/socket closed, "connection has
		// been closed"), sesi Hibernate yang sama TIDAK BISA dipulihkan dengan rollback/clear/
		// beginTransaction() biasa -- semua operasi berikutnya di sesi itu akan gagal lagi dgn
		// exception yg BERBEDA-BEDA (AssertionFailure null-id, StaleStateException, GenericJDBCException,
		// dst) tergantung pemeriksaan internal Hibernate mana yg tersandung duluan, membuat SATU
		// insiden putus-koneksi terlihat seperti belasan bug berbeda di log. Flag ini dipakai
		// synchronizeAll utk berhenti lebih awal begitu insiden ini terdeteksi, drpd terus memaksa
		// sumber berikutnya memakai sesi yg sudah pasti rusak.
		private boolean connectionLost;

		public int getScanned() { return scanned; }
		public int getSynced() { return synced; }
		public int getFailed() { return failed; }
		public String getMessage() { return message == null ? "" : message; }
		public boolean isConnectionLost() { return connectionLost; }
	}

	/**
	 * Deteksi apakah exception (atau salah satu cause-nya) menandakan koneksi JDBC FISIK
	 * sudah mati (bukan sekadar transaksi/state Hibernate yang perlu di-rollback). Pada
	 * kondisi ini, rollback()/clear()/beginTransaction() pada sesi yang sama tidak akan
	 * memulihkan apa pun -- semua akan gagal lagi memakai koneksi yang sama-sama sudah tertutup.
	 *
	 * <p>Implementasinya sekarang TINGGAL SATU dan berada di
	 * {@link ais.database.hibernate.HibernateUtil#isConnectionDead(Throwable)}, karena jalur
	 * pembersihan session ({@code HibernateUtil.closeSessionQuietly}) butuh pemeriksaan yang
	 * PERSIS SAMA supaya tidak melempar {@code TransactionException: JDBC rollback failed} pada
	 * koneksi yang sudah mati. Method ini dipertahankan sebagai nama lokal (dipakai belasan kali
	 * di kelas ini bersama flag {@link SyncSummary#connectionLost}) dan hanya mendelegasikan.</p>
	 */
	private static boolean isConnectionDead(Throwable e) {
		return ais.database.hibernate.HibernateUtil.isConnectionDead(e);
	}

	/**
	 * SET LOCAL lock_timeout best-effort supaya sync yang kalah rebutan row lock dengan user
	 * (mis. user sedang mengedit Item/Mahasiswa/Skripsi yang SAMA lewat UI, entitas yang juga
	 * disentuh syncOne di sini) GAGAL CEPAT drpd menggantung sampai statement_timeout menahan
	 * koneksi c3p0. Item yang kena skip di siklus ini akan otomatis tersinkron di siklus
	 * berikutnya (lihat item-catch/flush-catch di synchronizeSource). Diam bila bukan
	 * PostgreSQL / tanpa transaksi aktif -- pola sama seperti
	 * KunciEntityHelper.pasangStatementTimeout / KegiatanHelper.terapkanLockTimeout.
	 */
	private static void terapkanLockTimeout(Session session) {
		try {
			session.createSQLQuery("SET LOCAL lock_timeout = '3s'").executeUpdate();
		} catch (Exception ignore) {
			ais.common.ErrorAuditUtil.record(ignore,
					"auto-audit(empty-catch) src/ais/action/master/repository/RepositorySyncService.java:lock-timeout");
		}
	}

	/**
	 * Mencegah dua node/thread menjalankan sinkron repository pada database yang sama.
	 * Lock transaksi PostgreSQL otomatis dilepas saat commit/rollback. Aplikasi utama
	 * memang memakai PostgreSQL; kegagalan query lock sengaja diteruskan karena biasanya
	 * menandakan koneksi sudah tidak sehat dan sinkronisasi tidak boleh dipaksakan lanjut.
	 */
	private static boolean cobaKunciSinkronisasi(Session session) {
		Object result = session.createSQLQuery("select pg_try_advisory_xact_lock(" + REPOSITORY_SYNC_LOCK_ID + ")")
				.uniqueResult();
		return Boolean.TRUE.equals(result) || "true".equalsIgnoreCase(String.valueOf(result));
	}

	private static List<SourceDescriptor> getDefaultSources() {
		List<SourceDescriptor> sources = new ArrayList<SourceDescriptor>();
		sources.add(new SourceDescriptor("Skripsi/Tugas Akhir", "SKRIPSI", "Skripsi, Thesis, Disertasi, dan Tugas Akhir",
				"ais.database.model.Skripsi", "ais.action.master.SkripsiAction", "Thesis", "COMPLETED_THESIS"));
		sources.add(new SourceDescriptor("Perpustakaan", "LIBRARY_ITEM", "Koleksi Perpustakaan",
				"ais.database.model.library.Item", "ais.action.master.library.ItemAction", "Book", "PUBLIC_LIBRARY_ITEM"));
		sources.add(new SourceDescriptor("Buku Bahan Ajar", "BUKU_BAHAN_AJAR", "Buku Bahan Ajar",
				"ais.database.model.BukuBahanAjar", "ais.action.master.BukuBahanAjarAction", "Book", "TITLED"));
		sources.add(new SourceDescriptor("Artikel", "ARTIKEL", "Artikel Penelitian dan Pengabdian",
				"ais.database.model.penelitiandanpengabdian.Artikel",
				"ais.action.master.penelitiandanpengabdian.ArtikelAction", "Article", "APPROVED"));
		sources.add(new SourceDescriptor("Penelitian dan Pengabdian", "PENELITIAN_PENGABDIAN",
				"Hasil Penelitian dan Pengabdian",
				"ais.database.model.penelitiandanpengabdian.PengajuanPenelitianDanPengabdian", "", "Research",
				"APPROVED"));
		return sources;
	}

	public static SyncSummary synchronizeAll(boolean pushToDspace, boolean updateDspace) {
		return synchronizeAll(pushToDspace, updateDspace, null);
	}

	public static SyncSummary synchronizeAll(boolean pushToDspace, boolean updateDspace,
			ais.common.LaporanUpload laporan) {
		return synchronizeAll(HibernateUtil.currentSession(), pushToDspace, updateDspace, laporan);
	}

	/**
	 * Sinkronisasi dengan session milik pemanggil (dipakai scheduler/background).
	 *
	 * <p><b>KE-FIX (lock timeout massal pada TABEL SUMBER).</b> Log produksi menunjukkan siklus
	 * ini meng-UPDATE tabel yang seharusnya HANYA DIBACA — {@code pegawai}, {@code mahasiswa},
	 * {@code biodata_calon_mahasiswa}, {@code formatnilai}, {@code format_nilai_skripsi},
	 * {@code detailperkuliahan} — lalu gagal "canceling statement due to lock timeout".</p>
	 *
	 * <p><b>Akar masalah.</b> {@code criteria.setReadOnly(true)} hanya melindungi entitas AKAR
	 * hasil query. Saat metadata dibentuk, kode menyentuh relasi LAZY
	 * ({@code getMahasiswa().getNama()}, {@code getMahasiswa().getJurusan()}, {@code joinAuthors},
	 * dst.) sehingga entitas TERKAIT ikut dimuat sebagai entitas READ-WRITE biasa. Getter legacy
	 * pada entitas itu menormalisasi field (trim/isi default), dirty-check Hibernate menganggapnya
	 * berubah, dan {@code session.flush()} milik RepoItem ikut menuliskan UPDATE ke tabel sumber —
	 * mengunci baris yang sedang dipakai pengguna lain.</p>
	 *
	 * <p><b>Perbaikan.</b> Seluruh sesi sinkronisasi dijadikan read-only secara DEFAULT sehingga
	 * entitas apa pun yang dimuat (akar maupun relasi lazy) tidak pernah menghasilkan UPDATE.
	 * Tabel milik repository ({@code RepoItem}, {@code RepoCollection}) dikembalikan writable
	 * secara EKSPLISIT di titik pemuatannya, jadi fungsi sinkronisasi tetap utuh. Nilai
	 * {@code defaultReadOnly} sebelumnya DIPULIHKAN di {@code finally}: salah satu pemanggil
	 * memakai {@code HibernateUtil.currentSession()} milik request, dan tanpa pemulihan
	 * penyimpanan data pengguna pada request yang sama bisa berhenti diam-diam.</p>
	 */
	public static SyncSummary synchronizeAll(Session session, boolean pushToDspace, boolean updateDspace,
			 ais.common.LaporanUpload laporan) {
		boolean readOnlySebelumnya = false;
		boolean readOnlyDiubah = false;
		try {
			readOnlySebelumnya = session.isDefaultReadOnly();
			session.setDefaultReadOnly(true);
			readOnlyDiubah = true;
		} catch (Throwable abaikan) {
			ais.common.ErrorAuditUtil.record(abaikan, "RepositorySyncService.setDefaultReadOnly");
		}
		try {
			return synchronizeAllInternal(session, pushToDspace, updateDspace, laporan);
		} finally {
			if (readOnlyDiubah) {
				try {
					session.setDefaultReadOnly(readOnlySebelumnya);
				} catch (Throwable abaikan) {
					ais.common.ErrorAuditUtil.record(abaikan, "RepositorySyncService.pulihkanDefaultReadOnly");
				}
			}
		}
	}

	private static SyncSummary synchronizeAllInternal(Session session, boolean pushToDspace, boolean updateDspace,
			 ais.common.LaporanUpload laporan) {
		SyncSummary summary = new SyncSummary();
		if (!cobaKunciSinkronisasi(session)) {
			summary.message = "Sinkronisasi repository lain masih berjalan; permintaan ini dilewati dan akan dicoba pada siklus berikutnya.";
			return summary;
		}
		String cookie = "";
		if (pushToDspace) {
			try {
				cookie = DspaceCommon.login();
			} catch (Exception e) {
				pushToDspace = false;
				summary.message = "Login DSpace gagal, sinkron lokal tetap diproses: " + e.getMessage();
				if (laporan != null) {
					laporan.tambahCatatan("Login DSpace gagal, sinkron lokal tetap diproses: "
							+ ais.common.LaporanUpload.detailTeknisException(e));
				}
			}
		}

		// KE-FIX (lock_timeout): terapkan di awal siklus juga -- kalau session datang dari
		// scheduler dgn transaksi yg sudah dibuka sebelum synchronizeAll dipanggil, SET LOCAL
		// perlu dipasang ulang di sini krn baru berlaku pada transaksi yg SEDANG aktif.
		terapkanLockTimeout(session);
		int[] baris = new int[] { 0 };
		List<SourceDescriptor> sources = getDefaultSources();
		for (SourceDescriptor source : sources) {
			SyncSummary part = synchronizeSource(session, source, cookie, pushToDspace, updateDspace, laporan, baris);
			summary.scanned += part.scanned;
			summary.synced += part.synced;
			summary.failed += part.failed;
			if (part.message != null && part.message.trim().length() > 0) {
				summary.message += (summary.message.length() == 0 ? "" : "\n") + part.message;
			}
			if (part.connectionLost) {
				// KE-FIX: koneksi sudah mati -- sumber lain di siklus ini pasti akan gagal juga
				// (dgn exception yg berbeda-beda tiap panggilan, membanjiri log). Hentikan siklus
				// ini; scheduler akan membuka sesi/koneksi baru di siklus berikutnya (lihat
				// RepositorySyncScheduler.jalankanSekali).
				summary.connectionLost = true;
				summary.message += (summary.message.length() == 0 ? "" : "\n") + "Koneksi database terputus, sisa sumber pada siklus ini dilewati (akan dicoba lagi di siklus berikutnya).";
				break;
			}
		}
		return summary;
	}

	@SuppressWarnings("unchecked")
	/**
	 * Mendeteksi galat IZIN database (PostgreSQL SQLSTATE 42501 / pesan "permission denied").
	 * Ditelusuri sampai ke akar penyebab karena Hibernate membungkusnya berlapis
	 * (GenericJDBCException -&gt; PSQLException).
	 *
	 * @return pesan akar penyebabnya bila memang galat izin, atau null bila bukan.
	 */
	private static String pesanIzinDitolak(Throwable e) {
		Throwable cursor = e;
		int pengaman = 0;
		while (cursor != null && pengaman++ < 20) {
			if (cursor instanceof java.sql.SQLException) {
				String state = ((java.sql.SQLException) cursor).getSQLState();
				if ("42501".equals(state)) {
					return String.valueOf(cursor.getMessage());
				}
			}
			String pesan = cursor.getMessage();
			if (pesan != null && pesan.toLowerCase().indexOf("permission denied") >= 0) {
				return pesan;
			}
			Throwable berikut = cursor.getCause();
			if (berikut == null && cursor instanceof java.sql.SQLException) {
				berikut = ((java.sql.SQLException) cursor).getNextException();
			}
			if (berikut == cursor) {
				break;
			}
			cursor = berikut;
		}
		return null;
	}

	/** Kunci penundaan per sumber. */
	private static String kunciSumber(SourceDescriptor source) {
		return source == null ? "?" : String.valueOf(source.modelClassName);
	}

	/**
	 * @return sisa milidetik penundaan bila sumber ini sedang ditunda karena galat izin,
	 *         atau 0 bila boleh diproses.
	 */
	private static long sisaPenundaanIzin(SourceDescriptor source) {
		Long sampai = SUMBER_DITUNDA_IZIN.get(kunciSumber(source));
		if (sampai == null) {
			return 0L;
		}
		long sisa = sampai.longValue() - System.currentTimeMillis();
		if (sisa <= 0L) {
			// Masa tunda habis: buang penandanya supaya percobaan berikutnya bersih.
			SUMBER_DITUNDA_IZIN.remove(kunciSumber(source));
			return 0L;
		}
		return sisa;
	}

	private static SyncSummary synchronizeSource(Session session, SourceDescriptor source, String cookie, boolean pushToDspace,
			boolean updateDspace, ais.common.LaporanUpload laporan, int[] baris) {
		SyncSummary summary = new SyncSummary();
		long sisaTunda = sisaPenundaanIzin(source);
		if (sisaTunda > 0L) {
			// Sudah pernah gagal karena izin database; jangan ulangi tiap siklus. Sumber lain
			// tetap diproses oleh pemanggil karena summary ini TIDAK menandai connectionLost.
			summary.message = "Sumber " + source.label + " dilewati sementara: menunggu perbaikan hak akses"
					+ " database (dicoba lagi dalam " + ((sisaTunda / 60000L) + 1L) + " menit).";
			if (laporan != null) {
				laporan.tambahCatatan(summary.message);
			}
			return summary;
		}
		try {
			Class<?> modelClass = Class.forName(source.modelClassName);
			Long collectionId = ensureCollection(session, source).getId();
			Number count = (Number) session.createCriteria(modelClass).setProjection(Projections.rowCount())
					.uniqueResult();
			int total = count == null ? 0 : count.intValue();
			outerBatch:
			for (int first = 0; first < total; first += DEFAULT_BATCH_SIZE) {
				/* KE-FIX: paging setFirstResult/setMaxResults TANPA ORDER BY tidak dijamin stabil di
				 * PostgreSQL -- baris yang sama bisa muncul di dua halaman (diproses ganda) dan baris
				 * lain tidak pernah terbaca (tidak pernah tersinkron). Urutkan berdasarkan id. */
				Criteria criteria = session.createCriteria(modelClass).setFirstResult(first)
						.addOrder(org.hibernate.criterion.Order.asc("id"))
						.setMaxResults(DEFAULT_BATCH_SIZE).setReadOnly(true);
				List<Object> data = criteria.list();
				for (Object obj : data) {
					/* Entitas sumber hanya dibaca untuk membentuk metadata. Tanpa read-only,
					 * getter legacy yang menormalisasi field dapat dianggap perubahan oleh
					 * dirty-check Hibernate dan flush RepoItem ikut meng-UPDATE tabel sumber. */
					/* KE-FIX (TransientObjectException "Instance was not associated with this
					 * persistence context"): jalur pemulihan kegagalan flush di bawah memanggil
					 * session.clear(), sehingga SISA entitas batch ini menjadi DETACHED. Memanggil
					 * setReadOnly() pada instance detached melempar exception dan mematikan seluruh
					 * siklus sinkronisasi. Cukup lewati bila entitas tak lagi dikelola session --
					 * criteria di atas sudah setReadOnly(true) sehingga proteksi dirty-check tetap
					 * berlaku bagi entitas yang masih managed. */
					if (session.contains(obj)) {
						session.setReadOnly(obj, true);
					}
					summary.scanned++;
					String kunci = "[" + source.label + "] " + String.valueOf(obj);
					try {
						RepoItem repoItem = syncOne(session, source, collectionId, obj, cookie, pushToDspace, updateDspace);
						if (repoItem != null && STATUS_FAILED.equals(repoItem.getSyncStatus())) {
							summary.failed++;
							if (laporan != null) {
								laporan.catatGagal(baris[0], kunci, repoItem.getSyncMessage());
							}
						} else {
							summary.synced++;
							if (laporan != null) {
								laporan.catatBerhasil(baris[0], kunci, "Sinkronisasi berhasil");
							}
						}
					} catch (Exception e) {
						summary.failed++;
						if (isConnectionDead(e)) {
							// KE-FIX: koneksi fisik sudah mati -- item lain di source ini pasti akan
							// gagal juga dgn exception yg BERBEDA-BEDA tiap kali dipaksa lanjut (lihat
							// javadoc SyncSummary.connectionLost). Catat SEKALI lalu hentikan source ini;
							// JANGAN coba rollback/reopen-transaksi di sini, akan gagal lagi memakai
							// koneksi yang sama-sama sudah tertutup.
							ais.common.ErrorAuditUtil.record(e,
									"auto-audit src/ais/action/master/repository/RepositorySyncService.java:connection-lost");
							if (laporan != null) {
								laporan.tambahCatatan("Sumber " + source.label + " - koneksi database terputus, sisa item dilewati: "
										+ ais.common.LaporanUpload.detailTeknisException(e));
							}
							summary.connectionLost = true;
							break outerBatch;
						}
						Common.tampilErrorJikaAdmin(e);
						if (laporan != null) {
							laporan.catatGagalDetail(baris[0], kunci, e);
						}
						// KE-FIX (cascade "current transaction is aborted"/AssertionFailure null-id
						// pada item BERIKUTNYA, mis. deadlock Postgres saat auto-flush di findRepoItem):
						// exception di sini (kalaupun bukan koneksi mati -- mis. deadlock antar-transaksi
						// yang genuinely eksternal, atau constraint) tetap membuat TRANSAKSI Postgres yang
						// SEDANG BERJALAN pada session ini berstatus "aborted" sampai di-rollback, dan
						// Hibernate sendiri menandai session "jangan di-flush lagi setelah exception".
						// Sebelumnya method ini cuma log lalu lanjut ke item berikutnya TANPA rollback --
						// item berikutnya (atau bahkan source berikutnya yg berbagi session yg sama)
						// otomatis ikut gagal dgn exception generik yg membingungkan. Pulihkan sesi di
						// sini (pola sama seperti catch flush-batch & catch per-source di bawah) supaya
						// item/batch/source berikutnya tetap bisa diproses dari kondisi bersih.
						boolean rollbackOk = true;
						boolean clearOk = true;
						try {
							if (session.getTransaction() != null && session.getTransaction().isActive()) {
								session.getTransaction().rollback();
							}
						} catch (Exception rbEx) { rollbackOk = false; ais.common.ErrorAuditUtil.record(rbEx, "auto-audit(empty-catch) src/ais/action/master/repository/RepositorySyncService.java:item-rollback"); }
						try {
							if (session.isOpen()) {
								session.clear();
							}
						} catch (Exception clEx) { clearOk = false; ais.common.ErrorAuditUtil.record(clEx, "auto-audit(empty-catch) src/ais/action/master/repository/RepositorySyncService.java:item-clear"); }
						if (!session.isOpen() || (!rollbackOk && !clearOk)) {
							// KE-FIX: rollback DAN clear sama-sama gagal (atau session sudah tertutup) --
							// tanda session/koneksi tidak sehat lagi, bukan sekadar 1 item bermasalah.
							// beginTransaction() di bawah ini kemungkinan besar akan ikut gagal lagi lalu
							// membanjiri log dgn exception yg berbeda-beda tiap item berikutnya (lihat
							// javadoc SyncSummary.connectionLost). Hentikan source ini SEKARANG; scheduler
							// akan menutup session ini & membuka yg baru pada siklus berikutnya
							// (RepositorySyncScheduler.jalankanSekali membuka session baru tiap panggilan).
							if (laporan != null) {
								laporan.tambahCatatan("Sumber " + source.label
										+ " - session tidak sehat setelah rollback/clear gagal, sisa item dilewati (akan dicoba lagi di siklus berikutnya).");
							}
							summary.connectionLost = true;
							break outerBatch;
						}
						try {
							if (session.getTransaction() == null || !session.getTransaction().isActive()) {
								session.beginTransaction();
							}
							terapkanLockTimeout(session);
						} catch (Exception txEx) { ais.common.ErrorAuditUtil.record(txEx, "auto-audit(empty-catch) src/ais/action/master/repository/RepositorySyncService.java:item-retx"); }
					}
					baris[0]++;
				}
				try {
					session.flush();
					session.clear();
				} catch (Exception flushEx) {
					// KE-FIX: satu batch yang gagal flush (mis. getter entity yang melempar
					// exception saat Hibernate dirty-check, koneksi/statement sudah tertutup,
					// atau deadlock) sebelumnya membuat SESSION ini "tertandai rusak" oleh
					// Hibernate ("don't flush the Session after an exception occurs") sehingga
					// SEMUA source berikutnya di synchronizeAll (yang berbagi session yang sama)
					// ikut gagal berantai dengan AssertionFailure null-id yang membingungkan.
					// Pulihkan session di sini (rollback + clear + transaksi baru) supaya source
					// lain tetap bisa disinkron; batch yang gagal ini dicoba ulang siklus
					// berikutnya (item-nya belum berubah statusnya di DB).
					summary.failed += data.size();
					if (isConnectionDead(flushEx)) {
						// KE-FIX: koneksi fisik sudah mati -- rollback/clear/beginTransaction() di
						// bawah ini akan gagal lagi memakai koneksi yg sama (lihat javadoc
						// SyncSummary.connectionLost). Catat SEKALI saja dan hentikan source ini.
						ais.common.ErrorAuditUtil.record(flushEx,
								"auto-audit src/ais/action/master/repository/RepositorySyncService.java:connection-lost-flush");
						if (laporan != null) {
							laporan.tambahCatatan("Sumber " + source.label + " - koneksi database terputus saat flush, sisa batch dilewati: "
									+ ais.common.LaporanUpload.detailTeknisException(flushEx));
						}
						summary.connectionLost = true;
						return summary;
					}
					Common.tampilErrorJikaAdmin(flushEx);
					if (laporan != null) {
						laporan.tambahCatatan("Sumber " + source.label + " - flush batch gagal, dilewati: "
								+ ais.common.LaporanUpload.detailTeknisException(flushEx));
					}
					boolean rollbackOk = true;
					boolean clearOk = true;
					try {
						if (session.getTransaction() != null && session.getTransaction().isActive()) {
							session.getTransaction().rollback();
						}
					} catch (Exception rbEx) { rollbackOk = false; ais.common.ErrorAuditUtil.record(rbEx, "auto-audit(empty-catch) src/ais/action/master/repository/RepositorySyncService.java:flush-rollback"); }
					try {
						if (session.isOpen()) {
							session.clear();
						}
					} catch (Exception clEx) { clearOk = false; ais.common.ErrorAuditUtil.record(clEx, "auto-audit(empty-catch) src/ais/action/master/repository/RepositorySyncService.java:flush-clear"); }
					if (!session.isOpen() || (!rollbackOk && !clearOk)) {
						// KE-FIX: sama seperti item-catch di atas -- rollback DAN clear sama-sama
						// gagal berarti session sudah tidak sehat, bukan cuma 1 batch bermasalah.
						// connectionLost sudah true di sini lewat jalur lain (return terjadi di bawah);
						// tandai eksplisit supaya scheduler tahu harus menutup & buka session baru.
						if (laporan != null) {
							laporan.tambahCatatan("Sumber " + source.label
									+ " - session tidak sehat setelah rollback/clear gagal pasca flush, sisa batch dilewati.");
						}
						summary.connectionLost = true;
						return summary;
					}
					try {
						if (session.getTransaction() == null || !session.getTransaction().isActive()) {
							session.beginTransaction();
						}
						terapkanLockTimeout(session);
					} catch (Exception txEx) { ais.common.ErrorAuditUtil.record(txEx, "auto-audit(empty-catch) src/ais/action/master/repository/RepositorySyncService.java:flush-retx"); }
					return summary;
				}
			}
		} catch (Exception e) {
			summary.message = source.label + " gagal diproses: " + e.getMessage();
			String izin = pesanIzinDitolak(e);
			if (izin != null) {
				/* KE-FIX: galat HAK AKSES database. Rollback/clear/beginTransaction di bawah TETAP
				 * dijalankan -- pada PostgreSQL transaksi sudah masuk status aborted sehingga tanpa
				 * rollback SEMUA sumber sesudahnya ikut gagal dengan "current transaction is aborted".
				 * Yang ditambahkan di sini hanya: pesan yang bisa ditindaklanjuti + penundaan agar
				 * kegagalan yang sama tidak dicatat berulang tiap siklus. */
				SUMBER_DITUNDA_IZIN.put(kunciSumber(source),
						Long.valueOf(System.currentTimeMillis() + JEDA_IZIN_DITOLAK_MS));
				summary.message = "Sumber " + source.label + " tidak dapat dibaca karena hak akses database:"
						+ " " + izin + ". Berikan hak baca pada role aplikasi (mis. GRANT USAGE ON SCHEMA ..."
						+ " dan GRANT SELECT ON ALL TABLES IN SCHEMA ...), lalu sinkronisasi akan pulih"
						+ " otomatis. Sumber lain tetap diproses.";
				ais.common.ErrorAuditUtil.record(e,
						"auto-audit src/ais/action/master/repository/RepositorySyncService.java:izin-ditolak-"
								+ source.collectionCode);
				if (laporan != null) {
					laporan.tambahCatatan(summary.message);
				}
			} else if (isConnectionDead(e)) {
				// KE-FIX: koneksi fisik sudah mati -- rollback/clear/beginTransaction() di bawah
				// ini akan gagal lagi memakai koneksi yg sama (lihat javadoc SyncSummary.connectionLost).
				// Catat SEKALI saja dan hentikan source ini.
				ais.common.ErrorAuditUtil.record(e,
						"auto-audit src/ais/action/master/repository/RepositorySyncService.java:connection-lost-source");
				if (laporan != null) {
					laporan.tambahCatatan("Sumber " + source.label + " - koneksi database terputus: "
							+ ais.common.LaporanUpload.detailTeknisException(e));
				}
				summary.connectionLost = true;
				return summary;
			}
			if (izin == null) {
				Common.tampilErrorJikaAdmin(e);
			}
			if (izin == null && laporan != null) {
				laporan.tambahCatatan("Sumber " + source.label + " gagal diproses total: "
						+ ais.common.LaporanUpload.detailTeknisException(e));
			}
			try {
				if (session.getTransaction() != null && session.getTransaction().isActive()) {
					session.getTransaction().rollback();
				}
			} catch (Exception rbEx) { ais.common.ErrorAuditUtil.record(rbEx, "auto-audit(empty-catch) src/ais/action/master/repository/RepositorySyncService.java:source-rollback"); }
			try {
				if (session.isOpen()) {
					session.clear();
				}
			} catch (Exception clEx) { ais.common.ErrorAuditUtil.record(clEx, "auto-audit(empty-catch) src/ais/action/master/repository/RepositorySyncService.java:source-clear"); }
			try {
				if (session.isOpen()) {
					if (session.getTransaction() == null || !session.getTransaction().isActive()) {
						session.beginTransaction();
					}
					terapkanLockTimeout(session);
				}
			} catch (Exception txEx) { ais.common.ErrorAuditUtil.record(txEx, "auto-audit(empty-catch) src/ais/action/master/repository/RepositorySyncService.java:source-retx"); }
		}
		return summary;
	}

	private static RepoItem syncOne(Session session, SourceDescriptor source, Long collectionId, Object obj,
			String cookie, boolean pushToDspace, boolean updateDspace) throws Exception {
		Long sourceId = getLong(obj, "getId");
		if (sourceId == null) {
			return null;
		}
		RepoItem item = findRepoItem(session, source.modelClassName, sourceId);
		if (item != null && firstNotEmpty(item.getOaiIdentifier()).length() == 0) {
			/* KE-FIX (ConstraintViolationException "could not update: [RepoItem#..]" dgn pesan
			 * duplicate key repo_item_oai_identifier_key): baris terpilih belum memegang
			 * identifier, sementara baris DUPLIKAT untuk sumber yang sama sudah memegangnya.
			 * Mengisi identifier pada baris ini akan melanggar constraint UNIQUE dan mematikan
			 * seluruh siklus sinkronisasi. Pakai baris yang sudah memegang identifier tsb. */
			RepoItem pemegang = findRepoItemByOai(session, buildOaiIdentifier(source, sourceId));
			if (pemegang != null && pemegang.getId() != null && !pemegang.getId().equals(item.getId())) {
				item = pemegang;
				item.setSourceClass(source.modelClassName);
				item.setSourceId(sourceId);
				item.setCollectionId(collectionId);
			}
		}
		boolean eligible = isEligible(source, obj);
		if (!eligible) {
			if (item != null && item.getAktif()) {
				item.setAktif(Boolean.FALSE);
				item.setIsWithdrawn(Boolean.TRUE);
				item.setSyncStatus(STATUS_SYNCED);
				item.setSyncMessage("Ditarik otomatis karena sumber tidak lagi aktif/layak publik.");
				item.setLastSyncAt(new Date());
				/* item berasal dari findRepoItem sehingga sudah managed; perubahan akan
				 * di-flush bersama batch tanpa helper yang menangkap error persistence. */
			}
			return item;
		}
		if (item == null) {
			/* KE-FIX (duplicate key "repo_item_oai_identifier_key"): baris RepoItem LAMA bisa
			 * sudah memegang oai_identifier yang sama (mis. sourceClass berubah setelah refactor
			 * atau data historis ganda). Membuat RepoItem BARU akan menabrak constraint UNIQUE
			 * saat flush. Pakai ulang baris tersebut dan arahkan ulang ke sumber saat ini. */
			item = findRepoItemByOai(session, buildOaiIdentifier(source, sourceId));
			if (item != null) {
				item.setSourceClass(source.modelClassName);
				item.setSourceId(sourceId);
				item.setCollectionId(collectionId);
			}
		}
		if (item == null) {
			item = new RepoItem();
			item.setTenantKey(RepositoryTenantScope.currentKey());
			item.setSourceClass(source.modelClassName);
			item.setSourceId(sourceId);
			item.setCollectionId(collectionId);
			item.setSubmittedAt(new Date());
		}
		if(item.getTenantKey()==null||item.getTenantKey().trim().isEmpty())item.setTenantKey(RepositoryTenantScope.currentKey());
		Date sourceUpdatedAt = firstDate(obj, "getTanggal_dirubah", "getSetujuiTanggal", "getTanggalPublikasi",
				"getTanggal");
		if (!pushToDspace && item.getId() != null && item.getLastSyncAt() != null && sourceUpdatedAt != null
				&& !sourceUpdatedAt.after(item.getLastSyncAt()) && item.getAktif() && !item.getIsWithdrawn()) {
			return item;
		}

		item.setAktif(Boolean.TRUE);
		item.setIsWithdrawn(Boolean.FALSE);
		item.setSourceLabel(source.label);
		item.setDocumentType(source.documentType);
		item.setTitle(firstNotEmpty(invokeString(obj, "getJudul"), invokeString(obj, "getNama"), obj.toString()));
		item.setAbstractText(firstNotEmpty(invokeString(obj, "getAbstrack"), invokeString(obj, "getAbstrak"),
				invokeString(obj, "getAbstractText"), invokeString(obj, "getTujuan"), invokeString(obj, "getKeterangan")));
		item.setSubjects(firstNotEmpty(invokeString(obj, "getKeyword"), invokeString(obj, "getKewords"),
				invokeString(obj, "getKategories"), invokeString(obj, "getSubjects")));
		item.setAuthors(firstNotEmpty(invokeString(obj, "getPengarangs"), invokeString(obj, "getOlehPenguna"),
				invokeNestedString(obj, "getMahasiswa", "getNama"), invokeNestedString(obj, "getTbmuser", "getUserName"),
				joinAuthors(obj)));
		item.setPublisher(firstNotEmpty(invokeNestedString(obj, "getMahasiswa", "getJurusan", "getNama"),
				invokeString(obj, "getPenerbit"), invokeNestedString(obj, "getPenelitianDanPengabdian", "getJudul")));
		item.setIssuedAt(firstDate(obj, "getTanggalSidang", "getTanggalPublikasi", "getTanggalterbit", "getSetujuiTanggal"));
		if (firstNotEmpty(item.getOaiIdentifier()).length() == 0) {
			item.setOaiIdentifier(oaiIdentifierBebasBentrok(session, buildOaiIdentifier(source, sourceId), item));
		}
		item.setLanguage(firstNotEmpty(invokeString(obj, "getBahasa"), "id"));
		/* Konfigurasi dibaca melalui cache/sesi isolasi. Jangan membuat Konfigurasi baru
		 * di transaksi sinkronisasi karena kegagalan koneksi dapat meninggalkan entity
		 * null-id dan merusak flush RepoItem berikutnya. */
		item.setAccessPolicy(Common.getKonfigurasi("repository_default_access_policy", "OPEN_ACCESS").getNilai());
		item.setTurnitinIndexed(Common.getKonfigurasi("repository_turnitin_index_default", "Aktif").getNilai()
				.equalsIgnoreCase("Aktif"));
		if (item.getTurnitinIndexed() && item.getTurnitinIndexedAt() == null) {
			item.setTurnitinIndexedAt(new Date());
		}

		if (pushToDspace) {
			try {
				DspaceInformation info = invokeDspace(source.actionClassName, cookie, obj, updateDspace);
				if (info != null) {
					item.setDspaceUuid(info.getUuid());
					item.setDspaceHandle(info.getLink());
					/* KE-FIX (duplicate key): link DSpace dapat sama untuk lebih dari satu item
					 * (handle gagal/berulang). Pasang sebagai oai_identifier HANYA bila belum
					 * dimiliki baris RepoItem lain; bila bentrok, identifier lama dipertahankan
					 * sehingga UPDATE tidak melanggar constraint UNIQUE. */
					String oaiDariDspace = firstNotEmpty(info.getLink());
					if (oaiDariDspace.length() > 0 && !oaiIdentifierDipakaiLain(session, oaiDariDspace, item)) {
						item.setOaiIdentifier(oaiDariDspace);
					}
				}
				item.setSyncStatus(STATUS_SYNCED);
				item.setSyncMessage("Sinkron repository berhasil.");
			} catch (Exception e) {
				item.setSyncStatus(STATUS_FAILED);
				item.setSyncMessage(e.getMessage());
			}
		} else {
			item.setSyncStatus(STATUS_SYNCED);
			item.setSyncMessage("Sinkron lokal berhasil. Push DSpace tidak dijalankan.");
		}
		item.setLastSyncAt(new Date());
		session.saveOrUpdate(item);
		/* ID RepoItem wajib sudah material sebelum metadata dibaca/ditulis. Flush di
		 * sini juga memastikan kegagalan insert keluar ke caller agar session segera
		 * di-rollback dan tidak dipakai lagi dalam keadaan poisoned. */
		session.flush();
		syncMetadata(session, item);
		RepositoryAuthorityService.synchronizeItem(session, item);
		return item;
	}

	private static RepoCollection ensureCollection(Session session, SourceDescriptor source) {
		RepoCollection collection = (RepoCollection) session.createCriteria(RepoCollection.class)
				.add(Restrictions.or(Restrictions.eq("tenantKey",RepositoryTenantScope.currentKey()),Restrictions.or(Restrictions.isNull("tenantKey"),Restrictions.eq("tenantKey","")))).add(Restrictions.eq("kode", source.collectionCode)).setMaxResults(1).uniqueResult();
		if (collection == null) {
			collection = new RepoCollection();
			collection.setTenantKey(RepositoryTenantScope.currentKey());
			collection.setKode(source.collectionCode);
		} else {
			jadikanWritable(session, collection);
		}
		if(collection.getTenantKey()==null||collection.getTenantKey().trim().isEmpty())collection.setTenantKey(RepositoryTenantScope.currentKey());
		collection.setNama(source.collectionName);
		collection.setDeskripsi("Koleksi otomatis dari " + source.label);
		collection.setSourceSystem("AIS");
		collection.setTipe("COLLECTION");
		session.saveOrUpdate(collection);
		session.flush();
		return collection;
	}

	private static RepoItem findRepoItem(Session session, String sourceClass, Long sourceId) {
		/* KE-FIX (duplicate key "repo_item_oai_identifier_key"): bila terdapat LEBIH DARI SATU
		 * baris RepoItem untuk sumber yang sama (duplikat historis), setMaxResults(1) TANPA
		 * pengurutan membuat baris yang terpilih berubah-ubah antar siklus. Akibatnya siklus
		 * yang memilih baris ber-oai KOSONG akan mengisi identifier milik baris saudaranya
		 * lalu gagal saat flush. Urutkan berdasarkan id agar pilihan selalu konsisten. */
		RepoItem item = (RepoItem) session.createCriteria(RepoItem.class)
				.add(Restrictions.or(Restrictions.eq("tenantKey",RepositoryTenantScope.currentKey()),Restrictions.or(Restrictions.isNull("tenantKey"),Restrictions.eq("tenantKey",""))))
				.add(Restrictions.eq("sourceClass", sourceClass)).add(Restrictions.eq("sourceId", sourceId))
				.addOrder(org.hibernate.criterion.Order.asc("id")).setMaxResults(1).uniqueResult();
		return (RepoItem) jadikanWritable(session, item);
	}

	/** Cari RepoItem berdasarkan oai_identifier (kolom ber-constraint UNIQUE). */
	private static RepoItem findRepoItemByOai(Session session, String oaiIdentifier) {
		String kunci = firstNotEmpty(oaiIdentifier);
		if (kunci.length() == 0) {
			return null;
		}
		RepoItem item = (RepoItem) session.createCriteria(RepoItem.class)
				.add(Restrictions.or(Restrictions.eq("tenantKey",RepositoryTenantScope.currentKey()),Restrictions.or(Restrictions.isNull("tenantKey"),Restrictions.eq("tenantKey",""))))
				.add(Restrictions.eq("oaiIdentifier", kunci)).setMaxResults(1).uniqueResult();
		return (RepoItem) jadikanWritable(session, item);
	}

	/**
	 * Lookup global khusus pemeriksaan constraint UNIQUE. Hasilnya hanya boleh dibaca untuk
	 * mendeteksi bentrok; jangan dipakai untuk mengambil alih item milik tenant lain.
	 */
	private static RepoItem findAnyRepoItemByOai(Session session, String oaiIdentifier) {
		String kunci = firstNotEmpty(oaiIdentifier);
		if (kunci.length() == 0) {
			return null;
		}
		return (RepoItem) session.createCriteria(RepoItem.class).add(Restrictions.eq("oaiIdentifier", kunci))
				.addOrder(org.hibernate.criterion.Order.asc("id")).setMaxResults(1).uniqueResult();
	}

	/**
	 * Kembalikan entitas milik REPOSITORY menjadi writable.
	 *
	 * <p>Sesi sinkronisasi berjalan dengan {@code setDefaultReadOnly(true)} agar tabel SUMBER tidak
	 * pernah ter-UPDATE (lihat javadoc {@code synchronizeAll}). Tanpa pengecualian ini, perubahan
	 * pada RepoItem/RepoCollection tidak ikut ter-flush sehingga sinkronisasi berhenti berfungsi.
	 * Entitas BARU (belum ada di session) tidak terpengaruh read-only dan tetap ter-INSERT.</p>
	 */
	private static Object jadikanWritable(Session session, Object entitasRepo) {
		if (entitasRepo != null) {
			try {
				session.setReadOnly(entitasRepo, false);
			} catch (Throwable abaikan) {
				/* Instance belum/tidak lagi dikelola session: tidak ada yang perlu diubah. */
				ais.common.ErrorAuditUtil.record(abaikan, "RepositorySyncService.jadikanWritable");
			}
		}
		return entitasRepo;
	}

	/**
	 * Apakah oai_identifier ini sudah dimiliki baris RepoItem LAIN (bukan item yang sedang
	 * diproses). Kolom repo_item.oai_identifier ber-constraint UNIQUE, sehingga menuliskan
	 * nilai milik baris lain membuat flush gagal (ConstraintViolationException) dan seluruh
	 * siklus sinkronisasi berhenti.
	 */
	private static boolean oaiIdentifierDipakaiLain(Session session, String oaiIdentifier, RepoItem item) {
		RepoItem pemilik = findAnyRepoItemByOai(session, oaiIdentifier);
		if (pemilik == null) {
			return false;
		}
		if (item == null || item.getId() == null || pemilik.getId() == null) {
			return true;
		}
		return !item.getId().equals(pemilik.getId());
	}

	/**
	 * Kembalikan oai_identifier yang dijamin tidak bentrok. Bila nilai dasar sudah dipakai
	 * baris lain (termasuk milik tenant lain), tambahkan tenant, nama kelas sumber, lalu
	 * urutan angka sampai bebas. Identifier milik baris lain TIDAK diubah agar tautan OAI
	 * yang sudah terpublikasi tetap valid.
	 */
	private static String oaiIdentifierBebasBentrok(Session session, String dasar, RepoItem item) {
		String kandidat = firstNotEmpty(dasar);
		if (kandidat.length() == 0 || !oaiIdentifierDipakaiLain(session, kandidat, item)) {
			return kandidat;
		}
		String tenant = RepositoryTenantScope.currentKey().toLowerCase().replaceAll("[^a-z0-9]+", "-")
				.replaceAll("(^-|-$)", "");
		if (tenant.length() > 0) {
			String alternatif = kandidat + ":" + tenant;
			if (!oaiIdentifierDipakaiLain(session, alternatif, item)) {
				return alternatif;
			}
			kandidat = alternatif;
		}
		String kelas = firstNotEmpty(item == null ? null : item.getSourceClass());
		if (kelas.length() > 0) {
			int titik = kelas.lastIndexOf('.');
			String pendek = titik < 0 ? kelas : kelas.substring(titik + 1);
			String alternatif = kandidat + ":" + pendek.toLowerCase();
			if (!oaiIdentifierDipakaiLain(session, alternatif, item)) {
				return alternatif;
			}
			kandidat = alternatif;
		}
		for (int urut = 2; urut < 1000; urut++) {
			String alternatif = kandidat + "-" + urut;
			if (!oaiIdentifierDipakaiLain(session, alternatif, item)) {
				return alternatif;
			}
		}
		return kandidat + "-" + System.currentTimeMillis();
	}

	@SuppressWarnings("unchecked")
	private static void syncMetadata(Session session, RepoItem item) {
		if (item.getId() == null) {
			return;
		}
		List<RepoItemMetadata> old = session.createCriteria(RepoItemMetadata.class)
				.add(Restrictions.eq("itemId", item.getId())).list();
		for (RepoItemMetadata metadata : old) {
			session.delete(metadata);
		}
		saveMetadata(session, item.getId(), "dc.title", item.getTitle(), 0);
		saveMetadata(session, item.getId(), "dc.contributor.author", item.getAuthors(), 1);
		saveMetadata(session, item.getId(), "dc.description.abstract", item.getAbstractText(), 2);
		saveMetadata(session, item.getId(), "dc.subject", item.getSubjects(), 3);
		saveMetadata(session, item.getId(), "dc.publisher", item.getPublisher(), 4);
		saveMetadata(session, item.getId(), "dc.type", item.getDocumentType(), 5);
		saveMetadata(session, item.getId(), "dc.rights.access", item.getAccessPolicy(), 6);
		saveMetadata(session, item.getId(), "repository.turnitin.indexed", item.getTurnitinIndexed().toString(), 7);
		if (item.getIssuedAt() != null) {
			saveMetadata(session, item.getId(), "dc.date.issued", Common.databaseDateFormat.get().format(item.getIssuedAt()), 8);
		}
	}

	private static void saveMetadata(Session session, Long itemId, String field, String value, int place) {
		if (value == null || value.trim().length() == 0) {
			return;
		}
		RepoItemMetadata metadata = new RepoItemMetadata();
		metadata.setItemId(itemId);
		metadata.setMetadataField(field);
		metadata.setMetadataValue(value);
		metadata.setLanguage("id");
		metadata.setPlace(Integer.valueOf(place));
		session.save(metadata);
	}

	private static DspaceInformation invokeDspace(String actionClassName, String cookie, Object obj, boolean update)
			throws Exception {
		if (actionClassName == null || actionClassName.trim().length() == 0) {
			return null;
		}
		Class<?> actionClass = Class.forName(actionClassName);
		Method[] methods = actionClass.getMethods();
		for (int i = 0; i < methods.length; i++) {
			Method method = methods[i];
			if (!"getDspace".equals(method.getName())) {
				continue;
			}
			Object[] args = buildDspaceArgs(method, cookie, obj, update);
			if (args != null) {
				Object result = method.invoke(null, args);
				return result instanceof DspaceInformation ? (DspaceInformation) result : null;
			}
		}
		return null;
	}

	private static Object[] buildDspaceArgs(Method method, String cookie, Object obj, boolean update) {
		Class<?>[] types = method.getParameterTypes();
		Object[] args = new Object[types.length];
		boolean hasSourceObject = false;
		for (int i = 0; i < types.length; i++) {
			if (String.class.equals(types[i])) {
				args[i] = cookie;
			} else if (Boolean.TYPE.equals(types[i]) || Boolean.class.equals(types[i])) {
				args[i] = Boolean.valueOf(update);
			} else if (types[i].isAssignableFrom(obj.getClass())) {
				args[i] = obj;
				hasSourceObject = true;
			} else {
				return null;
			}
		}
		return hasSourceObject ? args : null;
	}

	private static Long getLong(Object obj, String methodName) {
		Object value = invoke(obj, methodName);
		return value instanceof Number ? Long.valueOf(((Number) value).longValue()) : null;
	}

	private static String invokeString(Object obj, String methodName) {
		Object value = invoke(obj, methodName);
		return value == null ? "" : value.toString();
	}

	private static String invokeNestedString(Object obj, String method1, String method2) {
		Object value = invoke(obj, method1);
		return value == null ? "" : invokeString(value, method2);
	}

	private static String invokeNestedString(Object obj, String method1, String method2, String method3) {
		Object value = invoke(obj, method1);
		value = value == null ? null : invoke(value, method2);
		return value == null ? "" : invokeString(value, method3);
	}

	private static Date firstDate(Object obj, String m1, String m2, String m3, String m4) {
		Object value = invoke(obj, m1);
		if (value instanceof Date) return (Date) value;
		value = invoke(obj, m2);
		if (value instanceof Date) return (Date) value;
		value = invoke(obj, m3);
		if (value instanceof Date) return (Date) value;
		value = invoke(obj, m4);
		return value instanceof Date ? (Date) value : null;
	}

	private static Object invoke(Object obj, String methodName) {
		try {
			if (obj == null || methodName == null) {
				return null;
			}
			Method method = obj.getClass().getMethod(methodName, new Class[0]);
			return method.invoke(obj, new Object[0]);
		} catch (Exception e) {
			return null;
		}
	}

	private static String firstNotEmpty(String... values) {
		if (values == null) {
			return "";
		}
		for (int i = 0; i < values.length; i++) {
			if (values[i] != null && values[i].trim().length() > 0) {
				return values[i].trim();
			}
		}
		return "";
	}

	private static boolean isEligible(SourceDescriptor source, Object obj) {
		if (obj == null || firstNotEmpty(invokeString(obj, "getJudul"), invokeString(obj, "getNama")).length() == 0) {
			return false;
		}
		if (hasFalse(obj, "getAktif")) {
			return false;
		}
		if ("COMPLETED_THESIS".equals(source.eligibility)) {
			return isTrue(obj, "getLulus") || numberGreaterThanZero(obj, "getTelahSidang");
		}
		if ("PUBLIC_LIBRARY_ITEM".equals(source.eligibility)) {
			return !isTrue(obj, "getFolder") && !hasFalse(obj, "getDefaultItem");
		}
		if ("APPROVED".equals(source.eligibility)) {
			return "Disetujui".equalsIgnoreCase(invokeString(obj, "getStatus"));
		}
		return true;
	}

	private static boolean isTrue(Object obj, String method) {
		return Boolean.TRUE.equals(invoke(obj, method));
	}

	private static boolean hasFalse(Object obj, String method) {
		Object value = invoke(obj, method);
		return value instanceof Boolean && !((Boolean) value).booleanValue();
	}

	private static boolean numberGreaterThanZero(Object obj, String method) {
		Object value = invoke(obj, method);
		return value instanceof Number && ((Number) value).longValue() > 0L;
	}

	private static String joinAuthors(Object obj) {
		StringBuilder value = new StringBuilder();
		for (int i = 1; i <= 5; i++) {
			String author = firstNotEmpty(invokeString(obj, "getPengarang" + i),
					invokeNestedString(obj, "getDosenPengarang" + i, "getTbmuser", "getUserName"));
			if (author.length() > 0 && value.indexOf(author) < 0) {
				if (value.length() > 0) value.append("; ");
				value.append(author);
			}
		}
		return value.toString();
	}

	private static String buildOaiIdentifier(SourceDescriptor source, Long sourceId) {
		return "oai:ais:" + source.collectionCode.toLowerCase().replace('_', '-') + ":" + sourceId;
	}
}
