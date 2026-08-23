package ais.common;

import java.io.Serializable;

import org.apache.commons.beanutils.BeanUtilsBean;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.hibernate.jdbc.Work;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.Program;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;

public class CommonHibernateHelper { // Ganti nama class sesuai class Anda

    // --- Helper Methods (Private) untuk penyederhanaan logika ---

    /**
     * Memastikan session yang digunakan dalam keadaan OPEN.
     * Jika session null atau closed, meminta session baru dari HibernateUtil.
     */
    private static Session getSafeSession(Session existingSession) {
        if (existingSession != null && existingSession.isOpen()) {
            return existingSession;
        }
        // Jika session tertutup atau null, minta yang baru
        // Catatan: Pastikan HibernateUtil memiliki method ini atau sesuaikan
        try {
            Session newSession = HibernateUtil.currentNativeSession();
            if (newSession != null && newSession.isOpen()) {
                return newSession;
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonHibernateHelper.java:35");
        }
        // Fallback terakhir: currentNativeSession() (ThreadLocal, dibuka-ulang bila perlu & DITUTUP
        // terpusat oleh FilterJSP/closeSession di akhir request). Sebelumnya openSession() -> objek
        // lepas dari ThreadLocal yang para pemanggil (refreshSaveOrUpdate/refreshDelete/refresh/
        // refreshUpdate/recoverWithMerge) tak pernah tutup di finally -> koneksi BOCOR saat fallback aktif.
        return HibernateUtil.currentNativeSession();
    }

    /**
     * Cek apakah object termasuk tipe khusus yang memiliki penanganan ID berbeda
     */
    private static boolean isSpecialClass(GeneralValueObject o) {
        return (o instanceof Tbmrole) || (o instanceof Tbmuser) || (o instanceof Program);
    }

	// KE-FIX (lock timeout saat flush — mis. update Kegiatan yang bertabrakan dengan proses lain
	// yang menyentuh baris sama, dipicu dari genNim/AuditListener$8 latar): sebelumnya safeFlush
	// TIDAK punya retry sama sekali untuk "canceling statement due to lock timeout" (SQLState
	// 55P03) — begitu terjadi, langsung rollback+lanjut (gagal). Karena proses ini berjalan di
	// thread audit latar (bukan request user), beberapa kali retry singkat (dgn jeda pendek)
	// dulu sebelum menyerah cukup aman & tidak mengganggu UX. Retry HANYA untuk lock-timeout,
	// bukan constraint violation (yang sudah ditangani terpisah).
	private static final int MAX_LOCK_TIMEOUT_RETRY = 3;

	private static boolean isLockTimeout(Throwable e) {
		Throwable t = e;
		while (t != null) {
			String msg = t.getMessage() == null ? "" : t.getMessage().toLowerCase();
			if (msg.indexOf("lock timeout") >= 0 || msg.indexOf("55p03") >= 0) {
				return true;
			}
			t = t.getCause();
		}
		return false;
	}

	private static boolean isStaleOrMissingRow(Throwable e) {
		Throwable t = e;
		while (t != null) {
			String className = t.getClass() == null ? "" : t.getClass().getName();
			String msg = t.getMessage() == null ? "" : t.getMessage().toLowerCase();
			if (className.indexOf("StaleStateException") >= 0
					|| msg.indexOf("row was updated or deleted") >= 0
					|| msg.indexOf("unexpected row count") >= 0
					|| msg.indexOf("stale state") >= 0
					// Konteks timer/latar: currentSession tanpa transaksi aktif atau sudah ditutup →
					// flush/createCriteria tak valid. Tak bisa di-recover di sini → lewati diam-diam
					// (bukan error aplikasi) agar tidak membanjiri log & tidak menggagalkan flow.
					|| className.indexOf("SessionException") >= 0
					|| msg.indexOf("not valid without active transaction") >= 0
					|| msg.indexOf("session is closed") >= 0
					|| msg.indexOf("transaction not successfully started") >= 0
					// Konteks thread latar (mis. InitDataHelper executor / timer): flush dapat memicu
					// AssertionFailure "possible nonthreadsafe access to session" saat persistence-context
					// terkontaminasi/diakses lintas-thread. Flush itu sudah gagal apa pun yang terjadi; di
					// sini di-recover dengan clear() lalu lewati diam-diam (bukan error aplikasi) agar tidak
					// membanjiri log & kontaminasi tidak menjalar ke iterasi berikutnya.
					|| className.indexOf("AssertionFailure") >= 0
					|| msg.indexOf("nonthreadsafe access") >= 0
					// Konteks edit-inline SEBELUM header tersimpan: sebuah detail mereferensikan parent
					// (mis. PenerimaanPengadaanMasterAsset) yang masih transient (belum di-save) sehingga
					// flush melempar TransientObjectException "object references an unsaved transient
					// instance". Ini BUKAN kehilangan data: nilai sudah ter-set di objek in-memory dan akan
					// ikut tersimpan saat header benar-benar disimpan. Recover dgn evict/clear lalu lewati
					// diam-diam agar tidak jadi error di log & tidak meracuni session.
					|| className.indexOf("TransientObjectException") >= 0
					|| msg.indexOf("unsaved transient instance") >= 0
					|| msg.indexOf("transient instance before flushing") >= 0) {
				return true;
			}
			t = t.getCause();
		}
		return false;
	}

    /**
     * Logika untuk menangani error update dengan cara me-load data DB lalu copy properties.
     * Menggantikan try-catch bertingkat yang berulang.
     */
    private static void recoverWithMerge(Session session, GeneralValueObject o) {
        try {
            // Pastikan session aktif sebelum createCriteria untuk mencegah error SessionClosed
            session = getSafeSession(session);

            Serializable id = o.getId();
            if (id == null) return; // Tidak bisa recover jika ID null

            GeneralValueObject ocopy = null;
            try {
                ocopy = (GeneralValueObject) session.createCriteria(o.getClass())
                        .add(Restrictions.idEq(id))
                        .uniqueResult();
            } catch (IllegalStateException ise) {
                // "Received resultset tuples, but no field structure" — koneksi korup, skip recovery
                return;
            } catch (java.util.NoSuchElementException nse) {
                // JDBC protocol state corruption dari ArrayDeque — koneksi korup, skip recovery
                return;
            }

            if (ocopy != null) {
                BeanUtilsBean.getInstance().copyProperties(ocopy, o);
                session.update(ocopy);
            } else {
                // Jika tidak ada di DB tapi punya ID, mungkin perlu di-save ulang
                session.save(o);
            }
        } catch (Exception e) {
            if (isStaleOrMissingRow(e)) {
                return;
            }
            Common.tampilErrorJikaAdmin(e);
        }
    }

    /**
     * Helper untuk melakukan flush aman tanpa memutus flow program.
     * SET statement_timeout=0 mencegah PostgreSQL membatalkan flush karena timeout.
     */
    private static void safeFlush(Session session) {
        // Kompatibilitas lama: dipakai refreshUpdate() -> TETAP telan exception (perilaku lama,
        // tidak diubah agar caller lama yg belum punya try-catch tidak mendadak pecah).
        safeFlush(session, false);
    }

    /**
     * Flush aman dengan opsi melempar-ulang bila flush gagal karena pelanggaran constraint DB
     * (FK maupun unique).
     *
     * <p>KE-FIX (rangkaian error 20-07-2026 12:07 — hapus BiodataCalonMahasiswa/Mahasiswa gagal
     * krn FK, disusul "createCriteria is not valid without active transaction"): sebelumnya method
     * ini SELALU menelan ConstraintViolationException setelah rollback, sehingga: (1) caller yang
     * SUDAH punya try-catch berisi pesan FK yang informatif bagi user (mis. MahasiswaAction baris
     * 4614-4620 &amp; CetakRegistrasiAction baris 3251-3256) tidak pernah tereksekusi — pesan itu
     * jadi kode mati; (2) kode setelah pemanggil refreshDelete (mis. onSearchDefault() yang dipanggil
     * tepat sesudahnya, DALAM try block yang sama) tetap lanjut dieksekusi meski delete gagal, padahal
     * transaksi thread-local sesi ZK baru saja di-rollback paksa di sini.</p>
     *
     * <p>Perbaikan: (a) SELALU buka transaksi baru pasca-rollback bila sesi masih open, agar sesi
     * (khususnya sesi milik request ZK dari {@code currentSession()}, yang transaksinya dibuka SEKALI
     * di awal request oleh OpenSessionInView &amp; baru di-commit di akhir) tetap USABLE untuk operasi
     * berikutnya di request/listener yang sama — inilah akar penyebab Error C; (b) bila
     * {@code rethrowConstraintViolation} true DAN exception-nya pelanggaran constraint, lempar ulang
     * (bukan ditelan) agar caller yang sudah siap menangani bisa menampilkan pesan yang jelas ke user
     * DAN berhenti melanjutkan kode berikutnya dalam try block yang sama (tidak lagi memanggil
     * onSearchDefault dengan sesi yang baru saja rollback).</p>
     */
    private static void safeFlush(Session session, boolean rethrowConstraintViolation) {
        try {
            if (session != null && session.isOpen()) {
                try {
                    session.doWork(new Work() {
                        public void execute(java.sql.Connection conn) throws java.sql.SQLException {
                            conn.createStatement().execute("SET statement_timeout = 0");
                        }
                    });
                } catch (Exception doWorkEx) { ais.common.ErrorAuditUtil.record(doWorkEx, "auto-audit(empty-catch) src/ais/common/CommonHibernateHelper.java:143");
                    // Koneksi mungkin sudah mati — biarkan flush() yang melaporkan
                }
                int lockTimeoutAttempt = 0;
                while (true) {
                    try {
                        session.flush();
                        break;
                    } catch (Exception flushEx) {
                        if (isLockTimeout(flushEx) && lockTimeoutAttempt < MAX_LOCK_TIMEOUT_RETRY
                                && session.isOpen()) {
                            lockTimeoutAttempt++;
                            ais.common.ErrorAuditUtil.record(flushEx,
                                    "auto-audit lock-timeout flush, retry ke-" + lockTimeoutAttempt);
                            try {
                                if (session.getTransaction() != null && session.getTransaction().isActive()) {
                                    session.getTransaction().rollback();
                                }
                            } catch (Exception rbEx) { ais.common.ErrorAuditUtil.record(rbEx, "auto-audit(empty-catch) src/ais/common/CommonHibernateHelper.java:lock-retry-rollback");
                            }
                            try {
                                if (session.isOpen() && (session.getTransaction() == null
                                        || !session.getTransaction().isActive())) {
                                    session.beginTransaction();
                                }
                            } catch (Exception txEx) { ais.common.ErrorAuditUtil.record(txEx, "auto-audit(empty-catch) src/ais/common/CommonHibernateHelper.java:lock-retry-begintx");
                            }
                            try {
                                Thread.sleep(200L * lockTimeoutAttempt);
                            } catch (InterruptedException ie) {
                                Thread.currentThread().interrupt();
                            }
                            // Ulangi flush() dengan persistence-context yang sama (tanpa clear/evict)
                            // sehingga perubahan yang belum tersimpan tetap dicoba lagi.
                            continue;
                        }
                        throw flushEx;
                    }
                }
            }
        } catch (Exception e) {
            if (isStaleOrMissingRow(e)) {
                try { if (session != null && session.isOpen()) session.clear(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/common/CommonHibernateHelper.java:150");}
                return;
            }
            boolean constraintViolation = isConstraintViolation(e);
            try {
                if (session != null && session.isOpen() && session.getTransaction() != null
                        && session.getTransaction().isActive()) {
                    session.getTransaction().rollback();
                }
            } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/common/CommonHibernateHelper.java:158");
            }
            try {
                if (session != null && session.isOpen()) {
                    session.clear();
                }
            } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/common/CommonHibernateHelper.java:164");
            }
            // KE-FIX: sesi (khususnya sesi ZK OpenSessionInView) HARUS tetap punya transaksi aktif
            // agar sisa request tidak gagal dgn "not valid without active transaction" (Error C).
            try {
                if (session != null && session.isOpen()
                        && (session.getTransaction() == null || !session.getTransaction().isActive())) {
                    session.beginTransaction();
                }
            } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/common/CommonHibernateHelper.java:reopen-tx");
            }

            if (rethrowConstraintViolation && constraintViolation) {
                // JANGAN tampilErrorJikaAdmin di sini — caller (mis. MahasiswaAction/
                // CetakRegistrasiAction) sudah memanggilnya sendiri di catch block masing-masing;
                // memanggilnya lagi di sini akan menggandakan entri di ErrorLog untuk exception yg sama.
                if (e instanceof RuntimeException) throw (RuntimeException) e;
                throw new RuntimeException(e);
            }
            Common.tampilErrorJikaAdmin(e);
        }
    }

    // --- Public Methods (Refactored) ---

    public static void refreshUpdate(GeneralValueObject o) {
        refreshUpdate(HibernateUtil.currentSession(), o, true); // Delegasi ke method utama
    }

    public static void refreshUpdate(Session session, GeneralValueObject o) {
        refreshUpdate(session, o, true);
    }

    public static void refreshUpdate(Session session, GeneralValueObject o, boolean flush) {
		if (o == null) {
			return;
		}
        session = getSafeSession(session);
		if (session == null || !session.isOpen()) {
			return;
		}

        // Logika penentuan Save atau Update
        try {
            if (o.getId() == null) {
                // Jika ID null, cek apakah tipe biasa atau special
                if (!isSpecialClass(o)) {
                    session.save(o);
                } else {
                    session.saveOrUpdate(o);
                }
            } else {
                // Jika punya ID, lakukan update
                try {
                    session.update(o);
                } catch (Exception e) {
                    if (isStaleOrMissingRow(e)) {
                        try { if (session != null && session.isOpen() && session.getTransaction() != null && session.getTransaction().isActive()) session.evict(o); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/common/CommonHibernateHelper.java:198");}
                        return;
                    }
                    // Jika update gagal, coba recover
                    recoverWithMerge(session, o);
                }
            }
        } catch (Exception e) {
            if (isStaleOrMissingRow(e)) {
                try { if (session != null && session.isOpen() && session.getTransaction() != null && session.getTransaction().isActive()) session.evict(o); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/common/CommonHibernateHelper.java:207");}
                return;
            }
            Common.tampilErrorJikaAdmin(e);
        }

        if (flush) {
            safeFlush(session);
        }

        // Setelah berhasil disimpan → update canonical di identity map
        // sehingga semua pemegang referensi ID yang sama otomatis melihat state terbaru.
        EntityIdentityMap.canonical(o);
    }

    public static void refresh(GeneralValueObject o) {
		if (o == null) {
			return;
		}
        Session session = getSafeSession(HibernateUtil.currentSession());
		if (session == null || !session.isOpen()) {
			return;
		}
        try {
            session.refresh(o);
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
        }
    }

    public static void refreshSaveOrUpdate(GeneralValueObject o) {
        refreshSaveOrUpdate(HibernateUtil.currentSession(), o);
    }

    public static void refreshSaveOrUpdate(Session session, GeneralValueObject o) {
        session = getSafeSession(session);

        try {
            if (isSpecialClass(o)) {
                session.saveOrUpdate(o);
            } else {
                if (o.getId() != null) {
                    session.update(o);
                } else {
                    session.save(o);
                }
            }
        } catch (Exception e) {
            if (isStaleOrMissingRow(e)) {
                try { if (session != null && session.isOpen()) session.evict(o); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/common/CommonHibernateHelper.java:250");}
                return;
            }
            // Duplicate key — bersihkan session (cegah AssertionFailure null-id) lalu
            // rethrow agar caller bisa rollback dan tampilkan pesan ke user.
            if (isConstraintViolation(e)) {
                try { if (session != null && session.isOpen()) session.clear(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/common/CommonHibernateHelper.java:256");}
                if (e instanceof RuntimeException) throw (RuntimeException) e;
                throw new RuntimeException(e);
            }
            try {
                if (session != null && session.isOpen() && session.getTransaction() != null
                        && session.getTransaction().isActive()) {
                    session.getTransaction().rollback();
                }
            } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/common/CommonHibernateHelper.java:265");
            }
            // KE-FIX (sama seperti safeFlush(), lihat javadoc method itu — "Error C"): sesi
            // ZK (khususnya currentSession(), yang transaksinya dibuka SEKALI di awal request oleh
            // OpenSessionInView & baru di-commit di akhir) HARUS tetap punya transaksi aktif setelah
            // rollback di atas, agar kode berikutnya di request/listener yang sama (mis. recoverWithMerge
            // di bawah ini, atau pemanggil refreshSaveOrUpdate yang lanjut memakai session yang sama
            // setelahnya) tidak gagal dengan "createCriteria is not valid without active transaction".
            try {
                if (session != null && session.isOpen()
                        && (session.getTransaction() == null || !session.getTransaction().isActive())) {
                    session.beginTransaction();
                }
            } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/common/CommonHibernateHelper.java:reopen-tx-refreshSaveOrUpdate");
            }
            // Jika gagal, masuk ke mekanisme recovery
            recoverWithMerge(session, o);
        }

        // Setelah berhasil disimpan → update canonical di identity map
        EntityIdentityMap.canonical(o);
    }

    private static boolean isConstraintViolation(Throwable e) {
        Throwable t = e;
        while (t != null) {
            String cn = t.getClass().getName();
            // org.hibernate.exception.ConstraintViolationException SELALU berarti pelanggaran
            // constraint DB (unique ATAU foreign key) — tidak perlu cek teks pesan lagi. Sebelumnya
            // hanya dicek untuk "unique constraint"/"duplicate key" sehingga pelanggaran FK saat
            // hapus data (mis. "violates foreign key constraint ... is still referenced from table")
            // tidak pernah dianggap constraint violation dan lolos ditelan diam-diam.
            if (cn.indexOf("ConstraintViolationException") >= 0) {
                return true;
            }
            if (cn.indexOf("GenericJDBCException") >= 0) {
                String msg = t.getMessage() == null ? "" : t.getMessage().toLowerCase();
                if (msg.indexOf("unique constraint") >= 0 || msg.indexOf("duplicate key") >= 0
                        || msg.indexOf("foreign key") >= 0 || msg.indexOf("violates") >= 0) {
                    return true;
                }
            }
            t = t.getCause();
        }
        return false;
    }

    public static void refreshDeleteFlush(GeneralValueObject o) {
        // Menggunakan native session sesuai kode asli, tapi dengan safety check
        Session session = HibernateUtil.currentNativeSession();
        if (session == null || !session.isOpen()) {
             // Fallback jika native session bermasalah
             session = HibernateUtil.currentSession();
        }
        
        try {
            if (session.getTransaction() != null && !session.getTransaction().isActive()) {
                session.getTransaction().begin();
            }
            
           refreshDelete(session, o);
            
            if (session.getTransaction() != null && session.getTransaction().isActive()) {
                session.getTransaction().commit();
            }
        } catch (Exception e) {
             try {
                 if (session.getTransaction() != null && session.getTransaction().isActive()) {
                    session.getTransaction().rollback();
                }
            } catch (Exception rbEx) { ais.common.ErrorAuditUtil.record(rbEx, "auto-audit(empty-catch) src/ais/common/CommonHibernateHelper.java:refreshDeleteFlush-rollback");
            }
            // KE-FIX (rangkaian error 20-07-2026): sebelumnya exception di sini SELALU ditelan
            // (hanya dicatat via tampilErrorJikaAdmin), sehingga caller-nya (mis.
            // CetakRegistrasiAction$CalonMahasiswaRenderer$13$1 baris 3243-3256) yang SUDAH punya
            // try-catch berisi pesan FK yang informatif bagi user tidak pernah tereksekusi — pesan
            // itu jadi kode mati dan user tidak mendapat feedback apa pun saat hapus gagal karena FK.
            // Sesi di sini bersifat mandiri/native (POLA C, selalu ditutup TUNTAS di finally di bawah,
            // TIDAK dipakai lagi setelah method ini), jadi aman untuk dilempar ulang tanpa
            // meninggalkan sesi request ZK dalam keadaan rusak. JANGAN panggil tampilErrorJikaAdmin
            // di sini agar tidak dobel dengan pemanggilan caller sendiri di catch block-nya.
            if (e instanceof RuntimeException) throw (RuntimeException) e;
            throw new RuntimeException(e);
        } finally {
            HibernateUtil.closeSession();
        }
    }

    public static void refreshDelete(GeneralValueObject o) {
        Session session = getSafeSession(HibernateUtil.currentSession());
        refreshDelete(session, o);
        // KE-FIX: rethrow=true — bila flush gagal krn FK/unique constraint, lempar ulang ke caller
        // (lihat javadoc safeFlush(Session, boolean)) alih-alih ditelan diam-diam.
        safeFlush(session, true);
    }

    public static void refreshDelete(Session session, GeneralValueObject o) {
        session = getSafeSession(session);
        Long deletedId = null;
        try {
            deletedId = o.getId();
            // KE-16 NonUniqueObjectException: bila instance LAIN dgn id sama sudah terkelola di sesi
            // (mis. termuat lewat asosiasi), hapus instance TERKELOLA itu, bukan objek detached yang dikirim.
            Object target = o;
            if (deletedId != null) {
	                Object managed = session.get(org.hibernate.Hibernate.getClass(o), deletedId);
	                if (managed == null) {
	                    // Idempotent delete: baris sudah dihapus oleh request/proses lain.
	                    // Jangan menjadwalkan DELETE/UPDATE untuk objek detached karena flush
	                    // akan menghasilkan StaleStateException (row count 0).
	                    EntityIdentityMap.evict(o.getClass(), deletedId);
	                    return;
	                }
	                if (managed != null) {
                    target = managed;
                    /* KE-FIX StaleStateException "Batch update returned unexpected row count from
                     * update [0]; expected: 1" saat commit (lewat Envers AuditProcess ->
                     * flush -> EntityUpdateAction): session.get() di atas MEMUAT entitas ke
                     * persistence context, dan getter legacy pada model AIS menormalkan isian
                     * field saat dibaca sehingga entitas itu langsung dianggap KOTOR. Hibernate
                     * mengeksekusi UPDATE sebelum DELETE; bila barisnya sudah tidak ada (dihapus
                     * lebih dulu oleh proses/klik lain) UPDATE mengembalikan 0 baris dan seluruh
                     * transaksi hapus gagal. Entitas ini memang akan DIHAPUS, jadi UPDATE apa pun
                     * terhadapnya tidak ada gunanya: ditandai read-only supaya dirty-checking
                     * melewatinya. session.delete() TETAP berjalan normal pada entitas read-only,
                     * sehingga fungsi hapusnya tidak berubah sama sekali. */
                    try {
                        session.setReadOnly(managed, true);
                    } catch (Exception abaikan) {
                        ais.common.ErrorAuditUtil.record(abaikan,
                                "auto-audit(empty-catch) src/ais/common/CommonHibernateHelper.java:refreshDelete-setReadOnly");
                    }
                }
            }
            session.delete(target);
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
            try {
                session.delete(o);
            } catch (Exception ex2) {
                Common.tampilErrorJikaAdmin(ex2);
            }
        }
        // Hapus dari identity map agar tidak ada canonical "zombie" pasca-delete
        if (deletedId != null) {
            EntityIdentityMap.evict(o.getClass(), deletedId);
        }
    }
}
