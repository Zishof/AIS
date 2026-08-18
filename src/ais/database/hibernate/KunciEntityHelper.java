package ais.database.hibernate;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Session;

/**
 * Mekanisme GLOBAL "hold transaksi dulu" agar update per-row bebas deadlock
 * dan bebas statement timeout akibat rebutan row lock.
 *
 * Hibernate TIDAK punya fitur bawaan yang menjamin bebas deadlock — Hibernate
 * hanya meneruskan kebutuhan lock ke database. Pencegahan disusun berlapis,
 * dan helper ini menggabungkan semuanya dalam satu pintu:
 *
 * 1. ANTRIAN DI APLIKASI (lapis utama). Semua thread dalam JVM ini yang ingin
 *    mengubah row yang sama (kelas entity + id) diantrikan FIFO memakai
 *    ReentrantLock fair, SEBELUM transaksi database dibuka. Thread yang
 *    menunggu tidak memegang koneksi/lock DB apa pun, jadi penantiannya tidak
 *    mungkin membentuk deadlock di PostgreSQL.
 *
 * 2. URUTAN KUNCI KANONIK (untuk multi-row). Varian jalankanDenganKunciBanyak
 *    selalu mengunci row dalam urutan kunci yang di-sort secara global.
 *    Karena semua pemakai mengunci dengan urutan yang sama, siklus tunggu
 *    (syarat terjadinya deadlock) tidak mungkin terbentuk antar pemakai
 *    helper ini — ini jaminan matematis, bukan sekadar memperkecil peluang.
 *
 * 3. FOR UPDATE NOWAIT + RETRY. Setelah giliran tiba, row dikunci di database.
 *    Pemegang lock dari luar JVM (instance lain, SQL manual) terdeteksi
 *    DETIK ITU JUGA — bukan menggantung sampai statement_timeout — lalu
 *    seluruh transaksi diulang beberapa kali dengan jeda.
 *
 * 4. TRANSAKSI PENDEK + STATEMENT TIMEOUT. begin -> kerjakan -> commit dengan
 *    statement_timeout lokal; rollback otomatis saat gagal; session SELALU
 *    ditutup di finally.
 *
 * BATAS JAMINAN (jujur): semua jalur yang lewat helper ini dijamin bebas
 * deadlock satu sama lain. Terhadap transaksi yang TIDAK lewat helper ini
 * (kode lama yang belum dipindahkan, SQL manual, aplikasi lain) perilakunya
 * gagal-cepat + retry, bukan menggantung. Tidak ada mekanisme apa pun — di
 * Hibernate versi mana pun — yang bisa menjamin 100% untuk transaksi yang
 * tidak ikut aturan main.
 *
 * Contoh pemakaian global (Java 1.7, anonymous class):
 *
 * <pre>
 * // 1 row:
 * KunciEntityHelper.jalankanDenganKunci(DisposisiSop.class, idDisposisi,
 *         new KunciEntityHelper.PekerjaanTransaksi() {
 *             public void kerjakan(Session session, Object entityTerkunci) throws Exception {
 *                 DisposisiSop data = (DisposisiSop) entityTerkunci;
 *                 data.setProperti(...);
 *                 session.update(data);
 *             }
 *         });
 *
 * // Beberapa row sekaligus (mis. transfer saldo antar 2 anggota) —
 * // urutan penguncian dinormalisasi otomatis sehingga bebas deadlock:
 * KunciEntityHelper.jalankanDenganKunciBanyak(
 *         new Class[] { AnggotaKoperasi.class, AnggotaKoperasi.class },
 *         new Serializable[] { idPengirim, idPenerima },
 *         new KunciEntityHelper.PekerjaanTransaksiBanyak() {
 *             public void kerjakan(Session session, Object[] entityTerkunci) throws Exception {
 *                 AnggotaKoperasi pengirim = (AnggotaKoperasi) entityTerkunci[0];
 *                 AnggotaKoperasi penerima = (AnggotaKoperasi) entityTerkunci[1];
 *                 ...
 *             }
 *         });
 *
 * // Serialisasi proses tanpa row tertentu (mis. generate nomor urut):
 * KunciEntityHelper.jalankanDenganKunciNama("generate-kode-member",
 *         new KunciEntityHelper.PekerjaanTransaksi() {
 *             public void kerjakan(Session session, Object tidakDipakai) throws Exception {
 *                 ...
 *             }
 *         });
 * </pre>
 */
public final class KunciEntityHelper {

	/** Pekerjaan terhadap satu row yang sudah terkunci aman. */
	public interface PekerjaanTransaksi {
		void kerjakan(Session session, Object entityTerkunci) throws Exception;
	}

	/** Pekerjaan terhadap beberapa row yang seluruhnya sudah terkunci aman. */
	public interface PekerjaanTransaksiBanyak {
		void kerjakan(Session session, Object[] entityTerkunci) throws Exception;
	}

	/** Lama maksimal sebuah thread rela mengantri giliran di aplikasi. */
	public static int detikMaksimalAntri = 90;

	/** Jumlah percobaan mengunci row di database (lock milik proses lain). */
	public static int maksimalPercobaanKunciDb = 5;

	/** Jeda MAKSIMAL (cap) antar percobaan kunci database. Jeda nyata memakai exponential backoff + jitter. */
	public static long milidetikJedaPercobaan = 2000L;

	/** Jeda DASAR backoff (percobaan pertama ~segini, lalu berlipat sampai cap milidetikJedaPercobaan). */
	public static long milidetikJedaDasarBackoff = 250L;

	/** statement_timeout lokal untuk transaksi yang dijalankan helper. */
	public static String statementTimeout = "120s";

	/**
	 * lock_timeout lokal: batas tunggu MAKSIMAL saat mengantri row lock di
	 * database. PENTING: di Hibernate 3.6 PostgreSQLDialect tidak punya
	 * getForUpdateNowaitString(), sehingga LockMode.UPGRADE_NOWAIT diam-diam
	 * terdegradasi menjadi "FOR UPDATE" biasa yang MENUNGGU (terbukti di log:
	 * "canceling statement due to statement timeout ... while locking tuple").
	 * lock_timeout-lah yang benar-benar membuat percobaan kunci gagal-cepat.
	 */
	public static String lockTimeout = "5s";

	private static final Object MUTEX = new Object();
	private static final Map<String, Kunci> DAFTAR_KUNCI = new HashMap<String, Kunci>();

	/**
	 * Lock fair (FIFO) + penghitung pemakai supaya entry map dibuang ketika
	 * tidak ada lagi yang memakai (mencegah map membengkak).
	 */
	private static final class Kunci {
		final ReentrantLock lock = new ReentrantLock(true);
		int pemakai = 0;
	}

	private KunciEntityHelper() {
	}

	// =====================================================================
	// API GLOBAL
	// =====================================================================

	/**
	 * Kunci SATU row entity lalu jalankan pekerjaan dalam transaksi pendek.
	 *
	 * @return true jika pekerjaan dijalankan; false jika row sudah tidak ada
	 *         di database (pekerjaan dilewati tanpa error).
	 */
	public static boolean jalankanDenganKunci(Class entityClass, Serializable id, final PekerjaanTransaksi pekerjaan)
			throws Exception {
		if (entityClass == null || id == null || pekerjaan == null) {
			throw new IllegalArgumentException("entityClass/id/pekerjaan tidak boleh null.");
		}
		return jalankanDenganKunciBanyak(new Class[] { entityClass }, new Serializable[] { id },
				new PekerjaanTransaksiBanyak() {
					@Override
					public void kerjakan(Session session, Object[] entityTerkunci) throws Exception {
						pekerjaan.kerjakan(session, entityTerkunci[0]);
					}
				});
	}

	/**
	 * Kunci BEBERAPA row sekaligus lalu jalankan pekerjaan dalam satu
	 * transaksi pendek. Urutan penguncian dinormalisasi (sort kanonik) agar
	 * sesama pemakai helper tidak mungkin saling deadlock; array entity yang
	 * diterima callback tetap mengikuti urutan argumen pemanggil.
	 *
	 * @return true jika pekerjaan dijalankan; false jika ADA row yang sudah
	 *         tidak ada di database (seluruh pekerjaan dilewati tanpa error).
	 */
	public static boolean jalankanDenganKunciBanyak(Class[] entityClasses, Serializable[] ids,
			PekerjaanTransaksiBanyak pekerjaan) throws Exception {
		if (entityClasses == null || ids == null || pekerjaan == null || entityClasses.length == 0
				|| entityClasses.length != ids.length) {
			throw new IllegalArgumentException("entityClasses/ids tidak boleh kosong dan harus sama panjang.");
		}
		final int jumlah = entityClasses.length;
		final String[] keys = new String[jumlah];
		for (int i = 0; i < jumlah; i++) {
			if (entityClasses[i] == null || ids[i] == null) {
				throw new IllegalArgumentException("entityClass/id ke-" + i + " null.");
			}
			keys[i] = entityClasses[i].getName() + "#" + ids[i];
		}

		/*
		 * Urutan kanonik: indeks di-sort berdasarkan key. SEMUA pemakai helper
		 * mengunci dengan urutan global yang sama sehingga siklus tunggu tidak
		 * mungkin terbentuk (syarat deadlock gugur).
		 */
		Integer[] urutan = new Integer[jumlah];
		for (int i = 0; i < jumlah; i++) {
			urutan[i] = Integer.valueOf(i);
		}
		Arrays.sort(urutan, new java.util.Comparator<Integer>() {
			@Override
			public int compare(Integer a, Integer b) {
				return keys[a.intValue()].compareTo(keys[b.intValue()]);
			}
		});

		Kunci[] kunciTerambil = new Kunci[jumlah];
		boolean[] dapatGiliran = new boolean[jumlah];
		try {
			// LAPIS 1: antri FIFO di aplikasi, dalam urutan kanonik.
			for (int posisi = 0; posisi < jumlah; posisi++) {
				int indeks = urutan[posisi].intValue();
				String key = keys[indeks];
				if (sudahDikunciSebelumnya(keys, urutan, posisi)) {
					continue; // key duplikat: cukup dikunci sekali
				}
				Kunci kunci = ambilKunci(key);
				kunciTerambil[posisi] = kunci;
				try {
					dapatGiliran[posisi] = kunci.lock.tryLock(detikMaksimalAntri, TimeUnit.SECONDS);
				} catch (InterruptedException interupsi) {
					Thread.currentThread().interrupt();
					throw new Exception("Antrian kunci " + key + " terinterupsi.", interupsi);
				}
				if (!dapatGiliran[posisi]) {
					throw new Exception("Antrian update " + key + " penuh: tidak mendapat giliran dalam "
							+ detikMaksimalAntri + " detik. Transaksi dibatalkan agar tidak menumpuk.");
				}
			}
			return kerjakanSetelahGiliran(entityClasses, ids, keys, urutan, pekerjaan);
		} finally {
			// Lepas dalam urutan terbalik dari pengambilan.
			for (int posisi = jumlah - 1; posisi >= 0; posisi--) {
				if (kunciTerambil[posisi] == null) {
					continue;
				}
				if (dapatGiliran[posisi]) {
					try {
						kunciTerambil[posisi].lock.unlock();
					} catch (Exception abaikan) { ais.common.ErrorAuditUtil.record(abaikan, "auto-audit(empty-catch) src/ais/database/hibernate/KunciEntityHelper.java:234");
					}
				}
				lepasKunci(keys[urutan[posisi].intValue()], kunciTerambil[posisi]);
			}
		}
	}

	/**
	 * Serialisasi GLOBAL berdasarkan nama bebas (tanpa row tertentu), untuk
	 * proses yang harus berjalan satu per satu (mis. generate nomor urut).
	 * Callback menerima entityTerkunci = null.
	 */
	public static void jalankanDenganKunciNama(String namaKunci, PekerjaanTransaksi pekerjaan) throws Exception {
		if (namaKunci == null || namaKunci.trim().length() == 0 || pekerjaan == null) {
			throw new IllegalArgumentException("namaKunci/pekerjaan tidak boleh kosong.");
		}
		String key = "nama:" + namaKunci.trim();
		Kunci kunci = ambilKunci(key);
		boolean dapatGiliran = false;
		try {
			try {
				dapatGiliran = kunci.lock.tryLock(detikMaksimalAntri, TimeUnit.SECONDS);
			} catch (InterruptedException interupsi) {
				Thread.currentThread().interrupt();
				throw new Exception("Antrian kunci " + key + " terinterupsi.", interupsi);
			}
			if (!dapatGiliran) {
				throw new Exception("Antrian " + key + " penuh: tidak mendapat giliran dalam " + detikMaksimalAntri
						+ " detik.");
			}
			Session session = null;
			try {
				session = HibernateUtil.openSession();
				session.getTransaction().begin();
				pasangStatementTimeout(session);
				pekerjaan.kerjakan(session, null);
				session.getTransaction().commit();
			} catch (Exception e) {
				rollbackDiamDiam(session);
				throw e;
			} finally {
				HibernateUtil.closeSessionQuietly(session);
			}
		} finally {
			if (dapatGiliran) {
				try {
					kunci.lock.unlock();
				} catch (Exception abaikan) { ais.common.ErrorAuditUtil.record(abaikan, "auto-audit(empty-catch) src/ais/database/hibernate/KunciEntityHelper.java:282");
				}
			}
			lepasKunci(key, kunci);
		}
	}

	// =====================================================================
	// INTERNAL
	// =====================================================================

	private static boolean sudahDikunciSebelumnya(String[] keys, Integer[] urutan, int posisi) {
		return posisi > 0 && keys[urutan[posisi].intValue()].equals(keys[urutan[posisi - 1].intValue()]);
	}

	private static boolean kerjakanSetelahGiliran(Class[] entityClasses, Serializable[] ids, String[] keys,
			Integer[] urutan, PekerjaanTransaksiBanyak pekerjaan) throws Exception {
		Session session = null;
		try {
			session = HibernateUtil.openSession();

			/*
			 * LAPIS 3: kunci row di database, juga dalam urutan kanonik.
			 * Kegagalan NOWAIT (pemegang lock dari luar JVM) membuat transaksi
			 * PostgreSQL abort, jadi SELURUH rangkaian diulang dari awal
			 * dengan transaksi baru.
			 */
			Object[] entityTerkunci = null;
			for (int percobaan = 1; percobaan <= maksimalPercobaanKunciDb; percobaan++) {
				session.getTransaction().begin();
				pasangStatementTimeout(session);
				try {
					entityTerkunci = kunciSemuaRow(session, entityClasses, ids, keys, urutan);
					break;
				} catch (Exception kunciSibuk) {
					rollbackDiamDiam(session);
					session.clear();
					if (percobaan >= maksimalPercobaanKunciDb) {
						String diagnosa = ambilDiagnosaPemegangLock(session);
						throw new Exception("Row masih dikunci transaksi lain setelah " + maksimalPercobaanKunciDb
								+ " percobaan: " + Arrays.toString(keys) + "." + diagnosa, kunciSibuk);
					}
					try {
						Thread.sleep(hitungJedaBackoff(percobaan));
					} catch (InterruptedException interupsi) {
						Thread.currentThread().interrupt();
						throw new Exception("Menunggu giliran kunci database terinterupsi.", interupsi);
					}
				}
			}

			if (entityTerkunci == null) {
				/* Ada row yang sudah dihapus; tidak ada yang dikerjakan. */
				rollbackDiamDiam(session);
				return false;
			}

			pekerjaan.kerjakan(session, entityTerkunci);
			session.getTransaction().commit();
			return true;
		} catch (Exception e) {
			rollbackDiamDiam(session);
			throw e;
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/**
	 * Kunci semua row FOR UPDATE NOWAIT dalam urutan kanonik. Mengembalikan
	 * entity dalam URUTAN ARGUMEN PEMANGGIL, atau null jika ada row yang
	 * sudah tidak ada di database.
	 */
	private static Object[] kunciSemuaRow(Session session, Class[] entityClasses, Serializable[] ids, String[] keys,
			Integer[] urutan) {
		Object[] hasil = new Object[entityClasses.length];
		Map<String, Object> sudah = new HashMap<String, Object>();
		for (int posisi = 0; posisi < urutan.length; posisi++) {
			int indeks = urutan[posisi].intValue();
			String key = keys[indeks];
			Object entity;
			if (sudah.containsKey(key)) {
				entity = sudah.get(key);
			} else {
				entity = kunciSatuRow(session, entityClasses[indeks], ids[indeks]);
				sudah.put(key, entity);
			}
			if (entity == null) {
				return null;
			}
			hasil[indeks] = entity;
		}
		return hasil;
	}

	/**
	 * Saat true (default), penguncian row memakai <b>FOR NO KEY UPDATE NOWAIT</b> alih-alih
	 * FOR UPDATE NOWAIT. Dimatikan otomatis & permanen bila PostgreSQL tidak mengenal sintaks
	 * tersebut (versi &lt; 9.3) sehingga otomatis kembali ke perilaku lama.
	 */
	private static volatile boolean pakaiForNoKeyUpdate = true;

	/**
	 * Kunci SATU row untuk diperbarui. Memakai <b>FOR NO KEY UPDATE</b> (bukan FOR UPDATE) agar TIDAK
	 * bentrok dengan kunci <b>FOR KEY SHARE</b> yang dipegang transaksi lain saat menyisipkan/mengubah
	 * baris ANAK ber-foreign-key ke row ini (mis. insert {@code disposisi_alur_sop} yang mereferensikan
	 * {@code disposisi_sop}). Penyebab utama error "canceling statement due to lock timeout ... while
	 * locking tuple in relation disposisi_sop" adalah FOR UPDATE (helper ini) berbenturan dengan FOR KEY
	 * SHARE (insert anak) dari proses disposisi yang berjalan bersamaan. FOR NO KEY UPDATE sudah cukup
	 * karena helper hanya mengubah kolom NON-kunci (mis. disposisiEnd/disposisiSetuju/properti), bukan PK.
	 *
	 * <p>Kegagalan NOWAIT (row dipegang transaksi lain) tetap dilempar agar masuk mekanisme RETRY. Hanya
	 * bila sintaks FOR NO KEY UPDATE tak didukung (PostgreSQL lama) helper berpindah permanen ke
	 * {@code session.get(..., UPGRADE_NOWAIT)} lama.</p>
	 */
	private static Object kunciSatuRow(Session session, Class entityClass, Serializable id) {
		if (pakaiForNoKeyUpdate) {
			String tabel = resolveNamaTabel(entityClass);
			String kolomPk = resolveKolomPk(entityClass);
			if (tabel != null && kolomPk != null) {
				try {
					session.createSQLQuery("SELECT " + kolomPk + " FROM " + tabel + " WHERE " + kolomPk
							+ " = :idKunci FOR NO KEY UPDATE NOWAIT").setParameter("idKunci", id).uniqueResult();
					return session.get(entityClass, id);
				} catch (RuntimeException e) {
					if (sintaksForNoKeyUpdateTakDidukung(e)) {
						// PostgreSQL lama: matikan jalur ini permanen, mulai berikutnya pakai cara lama.
						pakaiForNoKeyUpdate = false;
					}
					// Baik row-busy (NOWAIT gagal) maupun sintaks: lempar agar retry membuka transaksi baru.
					throw e;
				}
			}
		}
		return session.get(entityClass, id, new LockOptions(LockMode.UPGRADE_NOWAIT));
	}

	private static String resolveNamaTabel(Class entityClass) {
		try {
			org.hibernate.metadata.ClassMetadata meta = HibernateUtil.getSessionFactory().getClassMetadata(entityClass);
			if (meta instanceof org.hibernate.persister.entity.AbstractEntityPersister) {
				return ((org.hibernate.persister.entity.AbstractEntityPersister) meta).getTableName();
			}
		} catch (Exception abaikan) { ais.common.ErrorAuditUtil.record(abaikan, "auto-audit(empty-catch) src/ais/database/hibernate/KunciEntityHelper.java:425");
		}
		return null;
	}

	private static String resolveKolomPk(Class entityClass) {
		try {
			org.hibernate.metadata.ClassMetadata meta = HibernateUtil.getSessionFactory().getClassMetadata(entityClass);
			if (meta instanceof org.hibernate.persister.entity.AbstractEntityPersister) {
				String[] pk = ((org.hibernate.persister.entity.AbstractEntityPersister) meta).getIdentifierColumnNames();
				if (pk != null && pk.length == 1 && pk[0] != null && pk[0].trim().length() > 0) {
					return pk[0];
				}
			}
		} catch (Exception abaikan) { ais.common.ErrorAuditUtil.record(abaikan, "auto-audit(empty-catch) src/ais/database/hibernate/KunciEntityHelper.java:439");
		}
		return null;
	}

	/** True bila exception menandakan sintaks FOR NO KEY UPDATE tak dikenal (PostgreSQL &lt; 9.3). */
	private static boolean sintaksForNoKeyUpdateTakDidukung(Throwable e) {
		Throwable c = e;
		while (c != null) {
			String state = (c instanceof java.sql.SQLException) ? ((java.sql.SQLException) c).getSQLState() : null;
			if ("42601".equals(state) || "0A000".equals(state)) return true; // syntax_error / feature_not_supported
			String msg = c.getMessage() == null ? "" : c.getMessage().toLowerCase();
			if (msg.indexOf("syntax error") >= 0 && msg.indexOf("no key update") >= 0) return true;
			c = c.getCause();
		}
		return false;
	}

	/**
	 * Jeda retry kunci: <b>exponential backoff + jitter</b> (best practice).
	 * Percobaan ke-n menunggu {@code dasar * 2^(n-1)} dibatasi {@code milidetikJedaPercobaan} (cap),
	 * lalu ditambah jitter acak agar antar-thread/antar-node tidak menabrak serempak (anti thundering herd).
	 * Penantian ini terjadi SETELAH rollback &amp; di LUAR transaksi DB, jadi tidak memegang lock apa pun.
	 */
	private static long hitungJedaBackoff(int percobaan) {
		long dasar = milidetikJedaDasarBackoff > 0 ? milidetikJedaDasarBackoff : 250L;
		int pangkat = percobaan - 1;
		if (pangkat < 0) pangkat = 0;
		if (pangkat > 16) pangkat = 16; // hindari overflow shift
		long eksponensial = dasar << pangkat;
		long cap = milidetikJedaPercobaan > dasar ? milidetikJedaPercobaan : dasar;
		long jeda = Math.min(eksponensial, cap);
		long jitter = (long) (Math.random() * (dasar / 2 + 1));
		return jeda + jitter;
	}

	private static void pasangStatementTimeout(Session session) {
		try {
			session.createSQLQuery("SET LOCAL statement_timeout = '" + statementTimeout + "'").executeUpdate();
		} catch (Exception abaikan) { ais.common.ErrorAuditUtil.record(abaikan, "auto-audit(empty-catch) src/ais/database/hibernate/KunciEntityHelper.java:478");
			// Pengaturan timeout gagal bukan alasan menggagalkan transaksi.
		}
		try {
			/* Penentu gagal-cepat yang sebenarnya (lihat catatan field lockTimeout). */
			session.createSQLQuery("SET LOCAL lock_timeout = '" + lockTimeout + "'").executeUpdate();
		} catch (Exception abaikan) { ais.common.ErrorAuditUtil.record(abaikan, "auto-audit(empty-catch) src/ais/database/hibernate/KunciEntityHelper.java:484");
			// PostgreSQL < 9.3 tidak mengenal lock_timeout; biarkan jalan tanpa itu.
		}
	}

	/**
	 * Saat semua percobaan kunci gagal, tanyakan langsung ke PostgreSQL
	 * transaksi mana saja yang sudah berjalan/menggantung lama — pemegang
	 * lock "idle in transaction" akan langsung terlihat di sini, lengkap
	 * dengan pid, user, status, umur transaksi, dan query terakhirnya.
	 */
	private static String ambilDiagnosaPemegangLock(Session session) {
		try {
			if (session == null || !session.isOpen()) {
				return "";
			}
			session.getTransaction().begin();
			java.util.List hasil = session.createSQLQuery(
					"SELECT pid, usename, state, "
							+ "to_char(now() - xact_start, 'HH24:MI:SS') AS umur_transaksi, "
							+ "left(coalesce(query,''), 160) AS query_terakhir "
							+ "FROM pg_stat_activity "
							+ "WHERE datname = current_database() AND pid <> pg_backend_pid() "
							+ "AND xact_start IS NOT NULL AND now() - xact_start > interval '30 seconds' "
							+ "ORDER BY xact_start LIMIT 5").list();
			session.getTransaction().rollback();
			if (hasil == null || hasil.isEmpty()) {
				return " Tidak ada transaksi lama terdeteksi di pg_stat_activity"
						+ " (pemegang lock kemungkinan baru saja selesai atau berasal dari koneksi non-database ini).";
			}
			StringBuilder sb = new StringBuilder(" Transaksi lama yang sedang berjalan (kandidat pemegang lock):");
			for (int i = 0; i < hasil.size(); i++) {
				Object[] baris = (Object[]) hasil.get(i);
				sb.append(" || pid=").append(baris[0]).append(", user=").append(baris[1]).append(", state=")
						.append(baris[2]).append(", umur=").append(baris[3]).append(", query=").append(baris[4]);
			}
			return sb.toString();
		} catch (Exception abaikan) {
			rollbackDiamDiam(session);
			return "";
		}
	}

	private static void rollbackDiamDiam(Session session) {
		try {
			if (session != null && session.isOpen() && session.getTransaction() != null
					&& session.getTransaction().isActive()) {
				session.getTransaction().rollback();
			}
		} catch (Exception abaikan) { ais.common.ErrorAuditUtil.record(abaikan, "auto-audit(empty-catch) src/ais/database/hibernate/KunciEntityHelper.java:533");
		}
	}

	private static Kunci ambilKunci(String key) {
		synchronized (MUTEX) {
			Kunci kunci = DAFTAR_KUNCI.get(key);
			if (kunci == null) {
				kunci = new Kunci();
				DAFTAR_KUNCI.put(key, kunci);
			}
			kunci.pemakai++;
			return kunci;
		}
	}

	private static void lepasKunci(String key, Kunci kunci) {
		synchronized (MUTEX) {
			kunci.pemakai--;
			if (kunci.pemakai <= 0) {
				DAFTAR_KUNCI.remove(key);
			}
		}
	}
}
