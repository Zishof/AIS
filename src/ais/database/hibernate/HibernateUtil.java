package ais.database.hibernate;

/**
 * Created by IntelliJ IDEA.
 * User:
 * Date: Jan 3, 2007
 * Time: 3:58:15 PM
 *
 * Enhanced session lifecycle guard.
 *
 * Tujuan utama:
 * 1. Mencegah object Session yang sudah closed dikembalikan lagi ke caller.
 * 2. Menjaga currentSession() milik ZK/request agar tidak ditutup manual oleh helper native.
 * 3. Membersihkan ThreadLocal native session yang sudah tertutup agar createCriteria tidak memicu
 *    org.hibernate.SessionException: Session is closed!.
 * 4. Tetap kompatibel dengan Java 1.6/1.7 dan Hibernate versi lama.
 */

import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.metadata.ClassMetadata;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;

import java.util.concurrent.ConcurrentHashMap;

import ais.common.Common;

/**
 * Penjaga siklus hidup {@link org.hibernate.Session Session} Hibernate untuk seluruh aplikasi AIS.
 * Class ini adalah satu-satunya titik resmi untuk memperoleh dan menutup session, dirancang agar
 * tetap berjalan di dua dunia sekaligus: konteks UI ZK 5 (yang punya session-per-request) dan
 * konteks non-UI (thread latar, API, laporan, timer) yang harus mengelola session-nya sendiri.
 *
 * <h3>Untuk apa class ini</h3>
 * Tanpa penjaga terpusat, kode rentan dua penyakit klasik: (1) memakai session yang sudah
 * <i>closed</i> sehingga {@code createCriteria}/{@code createQuery} melempar
 * {@code org.hibernate.SessionException: Session is closed!}, dan (2) menutup session milik ZK
 * secara manual sehingga request berikutnya rusak. HibernateUtil menyelesaikan keduanya dengan
 * memvalidasi keterpakaian session sebelum dikembalikan dan dengan memisahkan tegas session ZK
 * (jangan ditutup manual) dari session native (wajib ditutup di {@code finally}).
 *
 * <h3>Dua jenis session &amp; aturan pakai</h3>
 * <ul>
 *   <li><b>{@link #currentSession()}</b> — untuk konteks request ZK. Mengembalikan session milik
 *   ZK bila ada &amp; masih open; bila tidak, fallback ke native. <b>JANGAN</b> ditutup manual oleh
 *   pemanggil.</li>
 *   <li><b>{@link #openSession()} / {@link #currentNativeSession()}</b> — untuk proses
 *   background/thread/API/report. Pemanggil <b>WAJIB</b> menutupnya di blok {@code finally}
 *   (disconnect lalu close), idealnya lewat {@link #closeSessionQuietly(Session)}.</li>
 * </ul>
 *
 * <h3>SessionFactory yang adaptif lintas-versi ZK</h3>
 * Dulu class ini mewarisi factory dari {@code org.zkoss.zkplus.hibernate.HibernateUtil}; paket itu
 * dihapus di ZK 9/10 CE. Maka {@link #getSessionFactory()} kini: memakai ulang factory milik
 * {@code HibernateSessionFactoryListener} (zk.xml) lewat refleksi bila {@code zkplus} masih ada
 * (deploy ZK 5 saat ini) — agar TIDAK terbentuk dua SessionFactory — atau membangun sendiri dari
 * {@code hibernate.cfg.xml} bila tidak ada.
 *
 * <h3>COOKBOOK — WAJIB diikuti kode baru (dan AI yang menulis kode ke depan)</h3>
 * Pilih SATU dari tiga pola di bawah sesuai konteks. Aturan emas: <b>siapa yang MEMBUKA session, dia
 * yang WAJIB menutupnya di {@code finally}</b>; sebaliknya session ZK milik framework <b>JANGAN</b>
 * ditutup manual.
 *
 * <p><b>Ringkasan aturan (rujuk sebelum menulis kode DB apa pun):</b></p>
 * <table border="1" cellpadding="4" summary="Aturan pemakaian &amp; penutupan session per konteks">
 *   <tr><th>Konteks</th><th>Pakai</th><th>Ditutup?</th></tr>
 *   <tr>
 *     <td>Request/response ZK (Action/helper)</td>
 *     <td>{@link #currentSession()}</td>
 *     <td><b>JANGAN</b> tutup manual (dikelola framework / OpenSessionInViewListener)</td>
 *   </tr>
 *   <tr>
 *     <td>JSP {@code /baru/modul/**}</td>
 *     <td>{@link #currentNativeSession()}</td>
 *     <td><b>JANGAN</b> tutup manual ({@code FilterJSP} yang menutup di akhir request;
 *         {@code clear()} manual = tulisan belum ter-flush hilang = simpan gagal)</td>
 *   </tr>
 *   <tr>
 *     <td>Thread / timer / API / laporan</td>
 *     <td>{@link #openSession()} / {@link #currentNativeSession()}</td>
 *     <td><b>WAJIB</b> {@code finally} → {@link #closeSessionQuietly(Session) closeSessionQuietly(s)} /
 *         {@link #closeSession()} (clear + disconnect + close)</td>
 *   </tr>
 * </table>
 *
 * <p><b>POLA A — di dalam request/response ZK</b> (Action, helper yang dipanggil dari event ZK).
 * Pakai {@link #currentSession()}. JANGAN ditutup/commit manual — {@code OpenSessionInViewListener}
 * yang meng-commit &amp; menutup di akhir request.
 * <pre>
 *   Session s = HibernateUtil.currentSession();   // konteks ZK
 *   s.save(obj);                                   // TANPA close/commit manual
 * </pre>
 *
 * <p><b>POLA B — proses NON-ZK</b> (JSP, thread latar, timer, API, laporan, DDL terpisah). JANGAN
 * pakai {@code currentSession()} di sini. Buka session sendiri dan <b>tutup TUNTAS di {@code finally}
 * (clear → disconnect → close, sudah tercakup oleh {@link #closeSessionQuietly(Session)})</b>:
 * <pre>
 *   Session s = HibernateUtil.openSession();       // atau currentNativeSession() untuk berbagi 1 thread
 *   Transaction tx = null;
 *   try {
 *       tx = s.beginTransaction();
 *       // ... query / save / update ...
 *       tx.commit();                               // WAJIB commit eksplisit; FilterJSP me-rollback yg belum commit
 *   } catch (RuntimeException e) {
 *       if (tx != null) { try { tx.rollback(); } catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/database/hibernate/HibernateUtil.java:108");} }
 *       throw e;
 *   } finally {
 *       HibernateUtil.closeSessionQuietly(s);      // clear + rollback + disconnect + close (aman bila null)
 *   }
 * </pre>
 * Bila memakai {@link #currentNativeSession()} (berbagi 1 session sepanjang thread), titik-masuk
 * thread yang PERTAMA membukanya wajib menutup di {@code finally} dengan {@link #closeSession()}
 * (mengeluarkan session dari ThreadLocal lalu {@code closeSessionQuietly}) — bukan tiap pemanggil.
 * Lalai menutup = kebocoran koneksi pool c3p0 ("idle in transaction").
 *
 * <p><b>POLA B-JSP — KEKECUALIAN PENTING untuk JSP {@code /baru/modul/**} (untuk AI &amp; kode baru).</b>
 * JSP service (mis. endpoint JSON kantin) memakai {@code currentNativeSession()} TAPI <b>TIDAK</b>
 * menutup sendiri: {@code FilterJSP} (dipetakan {@code /*}) sudah memanggil
 * {@code HibernateUtil.rollbackTransaction()} → {@link #closeSession()} di akhir SETIAP request, yaitu
 * penutupan TERPUSAT (clear + rollback + disconnect + close). Titik commit-nya pun BUKAN di dalam JSP.
 * Karena itu <b>JANGAN menambah {@code closeSession()}/{@code session.close()} manual di JSP</b>:
 * memanggil {@code clear()} di tengah request dapat membuang perubahan yang belum ter-flush →
 * <b>simpan gagal</b> (regresi yang pernah terjadi). Jadi untuk JSP: pakai {@code currentNativeSession()},
 * lalu SERAHKAN penutupan ke FilterJSP. (Ini berbeda dari thread latar/timer/API yang HARUS menutup
 * sendiri karena tidak dilewati FilterJSP.)
 *
 * <p><b>POLA C — commit terisolasi</b> yang TIDAK boleh menumpang transaksi request (mis. mencatat
 * audit/log walau request utama gagal). Sama seperti POLA B tetapi selalu {@link #openSession()}
 * (bukan native) agar transaksinya independen, dan tetap ditutup di {@code finally}.
 *
 * <h3>Higiene koneksi (mencegah "idle in transaction")</h3>
 * Dengan {@code hibernate.connection.autocommit=false}, bahkan SELECT membuka transaksi implisit di
 * PostgreSQL. {@link #closeSessionQuietly(Session)} karena itu selalu mencoba {@code rollback}
 * transaksi Hibernate maupun rollback koneksi JDBC sebelum {@code disconnect}/{@code close},
 * mencegah koneksi menggantung memegang row-lock — masalah yang terbukti di {@code pg_stat_activity}.
 *
 * <h3>Keamanan thread &amp; pemeliharaan</h3>
 * Session native disimpan per-thread di {@link ThreadLocal} {@code MAP}; session yang sudah closed
 * dibersihkan dari ThreadLocal agar tidak dipakai ulang. {@code FACTORY} memakai double-checked
 * locking. Semua method defensif terhadap null/closed dan kompatibel Java 1.6/1.7 (tanpa lambda,
 * multi-catch ditulis terpisah). Jangan menutup session ZK; jangan menyimpan session lintas-thread.
 */
public class HibernateUtil {

    /** ThreadLocal Session untuk proses native/non-ZK. */
    private static final ThreadLocal<Session> MAP = new ThreadLocal<Session>();

    /* Dulu class ini extends org.zkoss.zkplus.hibernate.HibernateUtil dan
     * mewarisi getSessionFactory() dari sana. Paket zkplus.hibernate sudah
     * DIHAPUS di ZK 9/10 CE, sehingga factory kini dikelola sendiri:
     * - Bila zkplus masih ada (deploy ZK 5 saat ini), factory milik
     *   HibernateSessionFactoryListener (zk.xml) dipakai ulang via refleksi
     *   agar tidak terjadi dua SessionFactory.
     * - Bila tidak ada (ZK 9/10 CE), factory dibangun langsung dari
     *   hibernate.cfg.xml. */
    private static volatile SessionFactory FACTORY;

    /** Constructor privat: class utilitas statis, tak boleh diinstansiasi. */
    private HibernateUtil() {
    }

    /**
     * Mengembalikan {@link SessionFactory} tunggal aplikasi, membangunnya sekali secara malas.
     *
     * <p><b>Tujuan.</b> Menjamin SELURUH aplikasi memakai satu SessionFactory yang sama. Memiliki
     * lebih dari satu factory berarti dua pool koneksi dan dua cache tingkat-dua yang tidak koheren —
     * sumber bug data basi dan kehabisan koneksi. Method ini adalah gerbang resmi memperoleh factory.</p>
     *
     * <p><b>Cara kerja.</b> Double-checked locking di atas field {@code volatile FACTORY}: bila masih
     * null, masuk blok {@code synchronized} dan memanggil
     * {@link #ambilFactoryZkplusAtauBangunSendiri()} yang memilih sumber factory sesuai versi ZK yang
     * ter-deploy. Setelah terisi, pemanggilan berikutnya mengembalikan instance yang sama tanpa
     * mengunci.</p>
     *
     * <p><b>Return &amp; konkurensi.</b> Mengembalikan factory siap pakai, tidak pernah null (bila
     * pembangunan gagal, exception merambat — kegagalan fatal yang memang harus terlihat saat
     * startup). Aman dipanggil dari banyak thread.</p>
     *
     * <p><b>Pemeliharaan.</b> Jangan membuat {@code new Configuration().buildSessionFactory()} di
     * tempat lain; selalu lewat sini agar tetap tunggal. Lihat catatan lintas-versi ZK pada
     * {@link #ambilFactoryZkplusAtauBangunSendiri()}.</p>
     *
     * @return SessionFactory tunggal milik aplikasi
     */
    public static SessionFactory getSessionFactory() {
        if (FACTORY == null) {
            synchronized (HibernateUtil.class) {
                if (FACTORY == null) {
                    FACTORY = ambilFactoryZkplusAtauBangunSendiri();
                    /*
                     * Pengelolaan skema SEPENUHNYA diserahkan ke Hibernate (hbm2ddl.auto).
                     * Sinkron kolom tabel audit yang dahulu dijalankan di sini
                     * (AuditSchemaSyncUtil, r77609) DIHAPUS atas keputusan pemilik.
                     *
                     * KONSEKUENSI YANG PERLU DIINGAT: pada Hibernate 3.6, hbm2ddl.auto=update
                     * tidak andal menambahkan kolom baru ke tabel audit Envers yang berada di
                     * schema lain (new_audit). Bila sebuah entitas @Audited mendapat kolom
                     * baru dan tabel new_audit.<tabel>__audit belum ikut diubah, INSERT audit
                     * gagal, flush ter-rollback, dan data pengguna TIDAK tersimpan.
                     * Penanganannya kembali manual: jalankan ALTER TABLE pada tabel *__audit
                     * saat rilis yang menambah kolom (pola lama ada di webapp/sql/migrasi_*_audit.sql).
                     */
                }
            }
        }
        return FACTORY;
    }

    /**
     * Memilih sumber {@link SessionFactory}: memakai ulang milik zkplus bila tersedia, atau membangun
     * sendiri dari {@code hibernate.cfg.xml}.
     *
     * <p><b>Tujuan.</b> Membuat class ini portabel lintas versi ZK tanpa menduplikasi SessionFactory.
     * Pada deploy ZK 5 saat ini, {@code HibernateSessionFactoryListener} (dikonfigurasi di zk.xml)
     * sudah membangun satu factory; method ini mengambilnya kembali agar dipakai bersama. Pada ZK
     * 9/10 CE yang sudah menghapus paket {@code zkplus.hibernate}, factory dibangun langsung.</p>
     *
     * <p><b>Cara kerja.</b> Mencoba {@code Class.forName("org.zkoss.zkplus.hibernate.HibernateUtil")}
     * lalu memanggil {@code getSessionFactory()} miliknya via refleksi; bila hasilnya
     * {@link SessionFactory} yang valid, itulah yang dikembalikan. Bila kelas tidak ada/refleksi
     * gagal ({@link Throwable} ditangkap luas, termasuk {@code NoClassDefFoundError}), eksekusi
     * jatuh ke {@code new Configuration().configure().buildSessionFactory()}.</p>
     *
     * <p><b>Return &amp; penanganan error.</b> Mengembalikan factory dari salah satu jalur. Penangkapan
     * {@link Throwable} disengaja luas karena ketidakhadiran paket zkplus pada ZK baru memunculkan
     * error tingkat <i>linkage</i>, bukan sekadar exception biasa.</p>
     *
     * <p><b>Pemeliharaan.</b> Method privat, hanya dipanggil sekali dari {@link #getSessionFactory()}
     * di dalam blok terkunci. Saat migrasi penuh ke ZK baru, jalur refleksi akan otomatis nonaktif
     * dan jalur {@code hibernate.cfg.xml} yang dipakai—tidak perlu perubahan kode.</p>
     *
     * @return SessionFactory dari zkplus (bila ada) atau hasil bangun sendiri
     */
    /**
     * Menutup {@link SessionFactory} aplikasi saat webapp berhenti/di-reload, secara aman.
     *
     * <p><b>Kenapa perlu.</b> Pada deploy ZK 5, factory dibangun oleh
     * {@code org.zkoss.zkplus.hibernate.HibernateSessionFactoryListener} (zk.xml) yang HANYA punya
     * fase init — TIDAK menutup factory saat shutdown. Akibatnya pool koneksi c3p0 (thread
     * {@code mchange ... PoolThread}) dan thread cache Hibernate/EhCache tetap hidup setelah webapp
     * berhenti → Tomcat melaporkan "appears to have started a thread ... but has failed to stop it"
     * (kebocoran classloader). Menutup factory di sini menutup {@code C3P0ConnectionProvider}
     * (mematikan pool c3p0) sehingga thread-thread itu berhenti.</p>
     *
     * <p><b>Aman.</b> Hanya menutup bila factory MEMANG sudah terbangun (tidak memanggil
     * {@link #getSessionFactory()} yang akan membangun factory baru saat shutdown). Semua kegagalan
     * ditelan. Catatan: dengan {@code SingletonEhCacheRegionFactory}, CacheManager EhCache adalah
     * singleton lintas-factory dan TIDAK ikut mati di sini — itu di-shutdown terpisah oleh
     * {@code AppStartupListener}.</p>
     */
    public static void shutdownFactoryQuietly() {
        SessionFactory sf = FACTORY;
        if (sf == null) {
            return;
        }
        try {
            sf.close();
        } catch (Throwable ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/database/hibernate/HibernateUtil.java:248");
            // Sudah tertutup / versi beda: abaikan agar shutdown tidak gagal.
        }
    }

    private static SessionFactory ambilFactoryZkplusAtauBangunSendiri() {
        // MODE KANONIK-ZK (opsional, gated -Dais.zk.factory_interceptor=true, DEFAULT OFF):
        // Bangun SATU factory DENGAN AuditTimestampInterceptor di LEVEL FACTORY, lalu SELARASKAN factory
        // zkplus ke instance itu. Efeknya session ZK (getCurrentSession/OpenSessionInView) dibuat via
        // factory.openSession(null,...) -> memakai interceptor FACTORY -> entity dikanonikalisasi ke
        // EntityIdentityMap. currentSession() TETAP mengembalikan session ZK (lifecycle & commit
        // OpenSessionInView TIDAK berubah -> penyimpanan data tetap normal). CATATAN: ini MEMPERLUAS
        // cakupan interceptor ke session ZK, jadi instance kanonik bisa dipakai bersama antara session ZK
        // & native dalam 1 request -> potensi "two open sessions". Bila error itu muncul di log, set
        // -Dais.zk.factory_interceptor=false + restart (rollback instan tanpa recompile).
        if (pasangInterceptorDiFactory() || DbCredentialOverride.adaEnvironmentJurnal()) {
            // CATATAN (soal "sf dibuat static final saja?"): TIDAK perlu & TIDAK bisa.
            // (1) `sf` adalah variabel LOKAL; `static` hanya untuk field, jadi `static final` di sini
            //     tak akan meng-compile.
            // (2) Singleton-nya SUDAH dijamin: method ini hanya dipanggil dari getSessionFactory() di
            //     dalam double-checked-locking, lalu hasilnya disimpan ke field `static volatile FACTORY`.
            //     Jadi buildSessionFactory() ini berjalan TEPAT SEKALI seumur JVM — `sf` hanyalah nilai
            //     antara yang mengalir ke FACTORY.
            // (3) Membuat FACTORY jadi `static final` + inisialisasi di deklarasi juga KELIRU: pembangunan
            //     factory harus MALAS (setelah listener zkplus/zk.xml siap, dipicu request pertama) supaya
            //     bisa menyuntik ke zkplus._factory; static-initializer bisa jalan terlalu dini dan bila
            //     gagal → ExceptionInInitializerError yang meracuni class permanen. Pola DCL lazy ini benar.
            SessionFactory sf;
            try {
                org.hibernate.cfg.Configuration cfgUtama = new org.hibernate.cfg.AnnotationConfiguration().configure();
                // P0 keamanan: kredensial dari berkas eksternal (bila ada) menimpa nilai cfg.xml.
                DbCredentialOverride.terapkan(cfgUtama, "utama");
                sf = cfgUtama.setInterceptor(AuditTimestampInterceptor.instance).buildSessionFactory();
            } catch (Throwable t) {
                org.hibernate.cfg.Configuration cfgUtama = new org.hibernate.cfg.Configuration().configure();
                DbCredentialOverride.terapkan(cfgUtama, "utama");
                sf = cfgUtama.setInterceptor(AuditTimestampInterceptor.instance).buildSessionFactory();
            }
            // zkplus tak punya setter -> set field statik privat _factory via refleksi (dikonfirmasi via
            // javap: private static SessionFactory _factory). Tutup factory zkplus lama bila sudah
            // terbangun (oleh HibernateSessionFactoryListener) agar pool c3p0-nya tidak bocor.
            try {
                Class<?> zk = Class.forName("org.zkoss.zkplus.hibernate.HibernateUtil");
                java.lang.reflect.Field f = zk.getDeclaredField("_factory");
                f.setAccessible(true);
                Object lama = f.get(null);
                f.set(null, sf);
                if (lama instanceof SessionFactory && lama != sf) {
                    try { ((SessionFactory) lama).close(); } catch (Throwable ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/database/hibernate/HibernateUtil.java:293"); }
                }
            } catch (Throwable t) { ais.common.ErrorAuditUtil.record(t, "auto-audit(empty-catch) src/ais/database/hibernate/HibernateUtil.java:295");
                // zkplus tak ada (ZK 9/10 CE) -> tak perlu selaraskan; ais tetap pakai sf ber-interceptor.
            }
            return sf;
        }

        // MODE LAMA (default): reuse factory zkplus apa adanya (TANPA interceptor di factory; session ZK
        // non-kanonik, tapi TIDAK ada perubahan lain). Jaminan kanonik tetap berlaku utk jalur
        // openSession()/currentNativeSession() yang meneruskan interceptor per-session.
        try {
            Class<?> zkplus = Class.forName("org.zkoss.zkplus.hibernate.HibernateUtil");
            Object sf = zkplus.getMethod("getSessionFactory", new Class[0]).invoke(null, new Object[0]);
            if (sf instanceof SessionFactory) {
                return (SessionFactory) sf;
            }
        } catch (Throwable e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/hibernate/HibernateUtil.java:310");
            // zkplus tidak tersedia (ZK 9/10 CE) - bangun sendiri di bawah (tetap dgn interceptor).
        }
        org.hibernate.cfg.Configuration cfgUtama = new org.hibernate.cfg.Configuration().configure();
        // P0 keamanan: kredensial dari berkas eksternal (bila ada) menimpa nilai cfg.xml.
        DbCredentialOverride.terapkan(cfgUtama, "utama");
        return cfgUtama.setInterceptor(AuditTimestampInterceptor.instance).buildSessionFactory();
    }

    // DEFAULT OFF (DIKEMBALIKAN 07-13). Escape hatch: -Dais.zk.factory_interceptor=true untuk
    // memasang AuditTimestampInterceptor di level factory + menyelaraskan factory zkplus (session ZK
    // IKUT kanonik TANPA mengubah session/commit). HANYA "true" eksplisit yang mengaktifkan.
    // KENAPA DIKEMBALIKAN KE OFF: mode ON memperluas interceptor ke session ZK sehingga satu instance
    // kanonik bisa dipakai bersama session ZK & native/openSession dalam 1 request/thread -> terbukti
    // memicu "You can't operate on a closed Connection!!!" / "two open sessions" pada beban PARALEL
    // (mis. layar "Hasil Ujian Mahasiswa" yang load per-peserta via executor) -> data tampil 0.
    // Dibaca via System property (bukan Common.bolehKonfigurasi -> hindari rekursi saat load config).
    // Di-cache volatile.
    private static volatile Boolean flagFactoryInterceptor = null;

    private static boolean pasangInterceptorDiFactory() {
        Boolean v = flagFactoryInterceptor;
        if (v == null) {
            boolean b = false; // DEFAULT OFF
            try {
                String s = System.getProperty("ais.zk.factory_interceptor");
                b = s != null && "true".equalsIgnoreCase(s.trim());
            } catch (Throwable t) {
                b = false;
            }
            v = Boolean.valueOf(b);
            flagFactoryInterceptor = v;
        }
        return v.booleanValue();
    }

    /**
     * Mengambil {@link ClassMetadata} Hibernate untuk sebuah kelas entity.
     *
     * <p><b>Tujuan.</b> Memberi akses metadata pemetaan (nama properti, tipe, identifier, dll.) yang
     * dipakai kode introspektif — misalnya penyalinan properti generik, audit, atau pembentukan
     * kriteria dinamis. Memusatkan akses ini memastikan metadata diambil dari SessionFactory yang
     * sama dengan yang dipakai membuka session.</p>
     *
     * <p><b>Cara kerja.</b> Delegasi langsung ke {@code getSessionFactory().getClassMetadata(aClass)}.
     * Bila {@code aClass} bukan entity yang dipetakan, Hibernate mengembalikan {@code null} — pemanggil
     * harus mengantisipasinya.</p>
     *
     * <p><b>Parameter &amp; Return.</b> {@code aClass} = kelas entity. Mengembalikan metadata-nya, atau
     * {@code null} bila kelas tidak dipetakan.</p>
     *
     * <p><b>Pemeliharaan.</b> Tipe parameter sengaja raw ({@code Class}) demi kompatibilitas dengan
     * banyak pemanggil lama; biarkan {@code @SuppressWarnings("rawtypes")}.</p>
     *
     * @param aClass kelas entity Hibernate
     * @return ClassMetadata entity, atau null bila tak dipetakan
     */
    @SuppressWarnings("rawtypes")
    public static ClassMetadata getClassMetadata(Class aClass) {
        return getSessionFactory().getClassMetadata(aClass);
    }

    /**
     * Mengembalikan session yang tepat untuk konteks SAAT INI: session ZK bila berada dalam request
     * UI dan masih usable, atau session native sebagai fallback.
     *
     * <p><b>Tujuan.</b> Menjadi pilihan default kode aplikasi (Action, helper) yang berjalan di dalam
     * request ZK. Dengan satu pemanggilan, kode memperoleh session yang konsisten dengan transaksi
     * request—tanpa perlu tahu apakah ia di UI atau bukan.</p>
     *
     * <p><b>Cara kerja.</b> Bila {@code ExecutionsCtrl.getCurrentCtrl()} tidak null (kita di dalam
     * eksekusi ZK), method mencoba mengambil session ZK lewat refleksi ke
     * {@code org.zkoss.zkplus.hibernate.HibernateUtil.currentSession()} (refleksi karena paket itu
     * hanya ada di ZK 5). Bila hasilnya {@link #isSessionUsable(Session) usable}, itulah yang
     * dikembalikan. Bila tidak di konteks ZK, session ZK null/closed, atau refleksi gagal, eksekusi
     * jatuh ke {@link #currentNativeSession()} yang dijamin mengembalikan session open.</p>
     *
     * <p><b>Aturan pakai (PENTING).</b> Session hasil method ini—khususnya bila berasal dari ZK—
     * <b>TIDAK boleh ditutup/commit manual</b> oleh pemanggil; siklus hidupnya dikelola
     * framework/request. Untuk proses background/thread/API/laporan gunakan {@link #openSession()}
     * atau {@link #currentNativeSession()} dan tutup sendiri di {@code finally}.</p>
     *
     * <p><b>Penanganan error &amp; return.</b> Seluruh deteksi konteks ZK dibungkus try/catch; kegagalan
     * apa pun mengarahkan ke session native. Mengembalikan session yang dijamin tidak null dan usable.</p>
     *
     * <p><b>Pemeliharaan.</b> Jangan mengganti refleksi dengan import langsung {@code zkplus} agar
     * tetap kompatibel saat migrasi ZK 9/10. Jangan menambahkan penutupan session di sini.</p>
     *
     * @return session usable untuk konteks saat ini (ZK bila ada, selain itu native)
     */
    public static Session currentSession() {
        boolean didalamEksekusiZk = false;
        try {
            didalamEksekusiZk = ExecutionsCtrl.getCurrentCtrl() != null;
            if (didalamEksekusiZk) {
                Session zkSession = null;
                try {
                    /* Refleksi: zkplus.hibernate hanya ada di ZK 5; di ZK 9/10
                     * CE langsung jatuh ke native ThreadLocal session. */
                    Class<?> zkplus = Class.forName("org.zkoss.zkplus.hibernate.HibernateUtil");
                    Object hasil = zkplus.getMethod("currentSession", new Class[0]).invoke(null, new Object[0]);
                    if (hasil instanceof Session) {
                        zkSession = (Session) hasil;
                    }
                } catch (Throwable e) {
                    zkSession = null;
                }
                /* KE-FIX HibernateException "createCriteria is not valid without active transaction"
                 * (Common.checkKonfigurasiBigIcon <- MainAction.initData <- doAfterCompose):
                 * hibernate.cfg.xml memakai current_session_context_class=thread, sehingga session
                 * yang dikembalikan jalur ZK adalah PROXY ThreadLocalSessionContext
                 * (TransactionProtectionWrapper). Proxy itu hanya mengizinkan sedikit method
                 * (isOpen/getTransaction/beginTransaction/close) selama TIDAK ada transaksi aktif;
                 * begitu ada eksekusi ZK yang transaksinya belum sempat dibuka oleh listener
                 * OpenSessionInView, panggilan pertama seperti createCriteria() langsung meledak dan
                 * SELURUH halaman gagal dirender.
                 *
                 * isSessionUsable() dulu hanya memeriksa "session terbuka", sehingga session yang
                 * DIPASTIKAN akan melempar pada pemakaian pertama tetap diserahkan ke pemanggil.
                 * Sekarang: bila transaksinya belum aktif, transaksi dibuka di sini -- persis yang
                 * dilakukan pola Open Session In View -- sehingga listener cleanup ZK tetap yang
                 * meng-commit/menutupnya seperti biasa. Bila membuka transaksi pun gagal, barulah
                 * jatuh ke currentNativeSession() seperti perilaku fallback yang sudah ada.
                 *
                 * Perbaikan sengaja diletakkan di SATU titik ini karena seluruh pemanggil
                 * currentSession() (ratusan) berbagi akar masalah yang sama. */
                if (isSessionUsable(zkSession)) {
                    if (transaksiSedangAktif(zkSession) || mulaiTransaksiBilaBisa(zkSession)) {
                        return zkSession;
                    }
                }
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/hibernate/HibernateUtil.java:419");
            // Abaikan dan fallback ke native session.
        }
        /* AUDIT (opsional, gated). Bila kita SAMA SEKALI tidak berada dalam eksekusi ZK, berarti
         * pemanggil currentSession() adalah proses NON-ZK (JSP / thread latar / API / timer). Sesuai
         * kontrak, proses semacam itu HARUS memakai currentNativeSession()/openSession() dan menutup
         * sendiri. Kita tidak mengubah perilaku (tetap kembalikan native yang benar), hanya mencatat
         * SEKALI per pemanggil unik agar tim punya daftar migrasi yang pasti. Aktif hanya bila
         * -Dais.currentsession.audit_nonzk=true (default OFF => tanpa biaya di produksi). */
        if (!didalamEksekusiZk && auditNonZkAktif()) {
            catatPemakaianNonZk();
        }
        return currentNativeSession();
    }

    // Escape hatch (default false). -Dais.currentsession.audit_nonzk=true mengaktifkan pencatatan
    // pemanggil currentSession() di luar eksekusi ZK (JSP/thread/background) -> menghasilkan daftar
    // migrasi ke currentNativeSession()/openSession(). Dibaca via System property (bukan
    // Common.bolehKonfigurasi -> hindari rekursi saat load config). Di-cache volatile. Tidak
    // mengubah perilaku apa pun; hanya diagnostik. Jalankan di STAGING, kumpulkan daftar, matikan.
    private static volatile Boolean flagAuditNonZk = null;
    // Dedupe agar tiap pemanggil unik dicatat SEKALI (hindari spam di jalur panas). Java 1.6-safe.
    private static final ConcurrentHashMap CATATAN_NON_ZK = new ConcurrentHashMap();

    private static boolean auditNonZkAktif() {
        Boolean v = flagAuditNonZk;
        if (v == null) {
            boolean b = false;
            try {
                String s = System.getProperty("ais.currentsession.audit_nonzk");
                b = s != null && "true".equalsIgnoreCase(s.trim());
            } catch (Throwable t) {
                b = false;
            }
            v = Boolean.valueOf(b);
            flagAuditNonZk = v;
        }
        return v.booleanValue();
    }

    private static void catatPemakaianNonZk() {
        try {
            StackTraceElement[] tumpukan = new Throwable().getStackTrace();
            StackTraceElement pemanggil = null;
            for (int i = 0; i < tumpukan.length; i++) {
                String kelas = tumpukan[i].getClassName();
                // Lewati frame internal HibernateUtil ini; frame pertama di luar = pemanggil asli.
                if (kelas != null && !kelas.equals("ais.database.hibernate.HibernateUtil")) {
                    pemanggil = tumpukan[i];
                    break;
                }
            }
            if (pemanggil == null) {
                return;
            }
            String tanda = pemanggil.getClassName() + "." + pemanggil.getMethodName()
                    + "(" + pemanggil.getFileName() + ":" + pemanggil.getLineNumber() + ")";
            if (CATATAN_NON_ZK.putIfAbsent(tanda, Boolean.TRUE) == null) {
                System.err.println("[AUDIT currentSession NON-ZK] dipanggil di luar eksekusi ZK oleh "
                        + tanda + " -> pindahkan ke currentNativeSession()/openSession() (tutup di finally)."
                        + " Thread=" + Thread.currentThread().getName());
            }
        } catch (Throwable t) { ais.common.ErrorAuditUtil.record(t, "auto-audit(empty-catch) src/ais/database/hibernate/HibernateUtil.java:481");
            // Diagnostik tidak boleh mengganggu jalur utama.
        }
    }

    /**
     * Mengembalikan session native berbasis {@link ThreadLocal}, membuat yang baru bila perlu.
     *
     * <p><b>Tujuan.</b> Menyediakan session "per-thread" untuk konteks NON-ZK (thread latar, API,
     * timer, laporan) sekaligus untuk fallback {@link #currentSession()}. Dengan menyimpannya di
     * ThreadLocal, beberapa pemanggilan dalam satu thread dapat berbagi session yang sama selama
     * thread bekerja, lalu ditutup sekali di akhir.</p>
     *
     * <p><b>Cara kerja.</b> (1) Membaca session dari {@code MAP} (try/catch agar aman bila ThreadLocal
     * bermasalah). (2) Bila {@link #isSessionUsable(Session) usable}, langsung dikembalikan. (3) Bila
     * ada tapi sudah closed, ThreadLocal dibersihkan ({@link #removeThreadLocalSessionIfSame(Session)})
     * dan session rusak ditutup rapi ({@link #closeSessionQuietly(Session)}) agar tidak menggantung.
     * (4) Membuka session baru via {@link #openSession()}, menyimpannya ke {@code MAP}, lalu
     * mengembalikannya.</p>
     *
     * <p><b>Aturan pakai &amp; return.</b> Mengembalikan session open. Untuk pekerjaan latar, pemanggil
     * yang membuka konteks ini bertanggung jawab menutupnya (mis. {@link #closeSession()} di
     * {@code finally}) supaya koneksi pool tidak bocor. Membersihkan session closed lebih dahulu
     * adalah inti perbaikan terhadap {@code SessionException: Session is closed!}.</p>
     *
     * <p><b>WAJIB — pola tutup di {@code finally} (untuk AI &amp; kode baru).</b> Setiap kali kode NON-ZK
     * memakai {@code currentNativeSession()} (atau {@link #openSession()}), session HARUS ditutup TUNTAS
     * di blok {@code finally}: {@code clear() → disconnect() → close()} (plus rollback) — ketiganya sudah
     * dibungkus oleh {@link #closeSessionQuietly(Session)} / {@link #closeSession()}. JANGAN cukup
     * memanggil {@code close()} saja. Pola baku yang HARUS diikuti:
     * <pre>
     *   Session s = HibernateUtil.currentNativeSession();
     *   Transaction tx = null;
     *   try {
     *       tx = s.beginTransaction();
     *       // ... query / save / update ...
     *       tx.commit();                              // commit EKSPLISIT (jangan andalkan auto)
     *   } catch (RuntimeException e) {
     *       if (tx != null) { try { tx.rollback(); } catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/database/hibernate/HibernateUtil.java:519");} }
     *       throw e;
     *   } finally {
     *       HibernateUtil.closeSession();             // keluarkan dari ThreadLocal + clear/disconnect/close
     *   }
     * </pre>
     * Bila membuka lewat {@link #openSession()} (session lepas, bukan ThreadLocal), tutup di
     * {@code finally} dengan {@link #closeSessionQuietly(Session) closeSessionQuietly(s)}. Lalai menutup
     * = kebocoran koneksi pool c3p0 ("idle in transaction"). Lihat COOKBOOK di javadoc kelas.</p>
     *
     * <p><b>Konkurensi &amp; pemeliharaan.</b> Aman karena state per-thread; tidak boleh membagikan
     * session ini ke thread lain. Jangan men-cache hasilnya melintasi batas thread.</p>
     *
     * @return session native open untuk thread saat ini
     */
    /**
     * Apakah session sedang berada di dalam transaksi aktif.
     *
     * <p>{@code getTransaction()} termasuk method yang DIIZINKAN oleh
     * {@code ThreadLocalSessionContext$TransactionProtectionWrapper} meskipun belum ada transaksi,
     * jadi pemeriksaan ini aman dipanggil pada session proxy maupun session biasa.</p>
     */
    private static boolean transaksiSedangAktif(Session session) {
        try {
            return session != null && session.getTransaction() != null && session.getTransaction().isActive();
        } catch (Throwable abaikan) {
            return false;
        }
    }

    /**
     * Buka transaksi bila memang belum ada, mengikuti pola Open Session In View.
     *
     * <p>{@code beginTransaction()} juga termasuk method yang diizinkan pada session proxy. Nilai
     * balik {@code false} berarti transaksi tidak dapat dibuka, sehingga pemanggil harus memakai
     * jalur cadangan ({@code currentNativeSession()}) alih-alih menyerahkan session yang dipastikan
     * akan melempar exception pada pemakaian pertama.</p>
     */
    private static boolean mulaiTransaksiBilaBisa(Session session) {
        if (session == null) {
            return false;
        }
        try {
            session.beginTransaction();
            return transaksiSedangAktif(session);
        } catch (Throwable e) {
            ais.common.ErrorAuditUtil.record(e,
                    "auto-audit src/ais/database/hibernate/HibernateUtil.java:mulaiTransaksiBilaBisa");
            return false;
        }
    }

    public static Session currentNativeSession() {
        /* PENGINGAT (AI & kode baru): hasil method ini WAJIB ditutup di finally lewat closeSession()
         * (ThreadLocal) atau closeSessionQuietly(s) (openSession), yang melakukan clear+disconnect+close.
         * JANGAN dipakai di konteks request ZK — di situ pakai currentSession() dan JANGAN tutup manual. */
        Session old = null;
        try {
            old = MAP.get();
        } catch (Exception e) {
            old = null;
        }

        if (isSessionUsable(old)) {
            return old;
        }

        if (old != null) {
            removeThreadLocalSessionIfSame(old);
            closeSessionQuietly(old);
        }

        Session session = openSession();
        MAP.set(session);
        return session;
    }

    /**
     * Cek APAKAH thread saat ini sudah punya native session yang masih terbuka&usable di
     * {@code MAP}, TANPA membuka session baru (read-only, tanpa efek samping).
     *
     * <p><b>Tujuan.</b> Mencegah helper bertingkat (mis. {@code ambilDataBanyak}) yang dipanggil
     * dari TENGAH alur yang sudah membuka session lewat {@link #currentNativeSession()} salah
     * menutup session ANCESTOR yang masih dipakai pemanggil di atasnya — {@code currentNativeSession()}
     * bersifat thread-local (bukan call-scoped), jadi panggilan bersarang akan MENDAPATKAN kembali
     * session ancestor yang sama, lalu {@code closeSession()} di finally-nya menutup paksa session
     * itu meski ancestor belum selesai memakainya, memicu {@code SessionException: Session is closed!}
     * di ancestor. Pola pakai: cek nilai ini SEBELUM memanggil {@code currentNativeSession()}; hanya
     * tutup session di {@code finally} bila nilai ini {@code false} (berarti pemanggil ini sendiri
     * yang membuka session barunya, bukan meminjam dari ancestor).</p>
     *
     * @return {@code true} bila thread ini sudah punya native session terbuka yang masih usable
     */
    public static boolean isNativeSessionOpenForCurrentThread() {
        Session existing = null;
        try {
            existing = MAP.get();
        } catch (Exception e) {
            existing = null;
        }
        return isSessionUsable(existing);
    }

    /**
     * Membuka session Hibernate BARU yang wajib ditutup sendiri oleh pemanggil.
     *
     * <p><b>Tujuan.</b> Menyediakan session terisolasi untuk pekerjaan yang TIDAK boleh menumpang
     * transaksi request ZK—thread latar, API, laporan, DDL terpisah, dan operasi yang harus commit
     * independen. Pemanggil mengelola transaksi dan penutupannya sendiri.</p>
     *
     * <p><b>Cara kerja.</b> Mencoba {@code sessionFactory.openSession(AuditTimestampInterceptor.instance)}
     * agar interceptor audit aktif pada session ini. Interceptor disuntik di sini karena properti
     * {@code hibernate.session_factory.interceptor} pada {@code hibernate.cfg.xml} tidak dikenal
     * Hibernate 3.6 (factory dibangun {@code HibernateSessionFactoryListener} tanpa
     * {@code setInterceptor}); tanpa interceptor, guard {@code onFlushDirty}—yang membatalkan UPDATE
     * yang hanya berisi kolom audit ({@code tanggal_dirubah}/{@code olehId}/{@code oleh})—tidak akan
     * pernah aktif. Bila pembukaan dengan interceptor gagal, fallback ke {@code openSession()} polos
     * agar pembukaan session tidak gagal hanya gara-gara interceptor.</p>
     *
     * <p><b>Aturan pakai (PENTING).</b> Session hasil method ini HARUS ditutup di blok {@code finally},
     * idealnya lewat {@link #closeSessionQuietly(Session)} (yang merollback transaksi/koneksi sebelum
     * menutup). Lalai menutupnya = kebocoran koneksi pool c3p0.</p>
     *
     * <p><b>Return &amp; pemeliharaan.</b> Mengembalikan session baru yang open. Jangan menaruhnya ke
     * {@code MAP} ThreadLocal secara manual; itu urusan {@link #currentNativeSession()}.</p>
     *
     * @return session Hibernate baru (wajib ditutup pemanggil)
     */
    public static Session openSession() {
        SessionFactory sessionFactory = getSessionFactory();
        try {
            return sessionFactory.openSession(AuditTimestampInterceptor.instance);
        } catch (Exception e) {
            // Fail-safe: jangan sampai pembukaan session gagal hanya karena interceptor.
            return sessionFactory.openSession();
        }
    }

    /**
     * Memeriksa apakah sebuah {@link Session} aman dipakai (tidak null dan masih open).
     *
     * <p><b>Tujuan.</b> Menjadi predikat tunggal yang dipakai seluruh class ini sebelum
     * mengembalikan/menggunakan session, sehingga keputusan "session ini masih hidup?" konsisten dan
     * defensif di satu tempat. Inilah pertahanan utama terhadap {@code SessionException: Session is
     * closed!}.</p>
     *
     * <p><b>Cara kerja.</b> Mengembalikan {@code session != null && session.isOpen()}. Pemanggilan
     * {@code isOpen()} dibungkus penangkapan {@link Exception} DAN {@link Throwable} terpisah (gaya
     * Java 1.6, tanpa multi-catch) karena pada kondisi rusak tertentu pemeriksaan pun dapat melempar;
     * dalam kasus itu hasilnya dianggap {@code false} (tidak usable).</p>
     *
     * <p><b>Parameter &amp; Return.</b> {@code session} = session yang diuji (boleh null).
     * Mengembalikan {@code true} hanya bila non-null dan terbuka; selain itu {@code false}.</p>
     *
     * <p><b>Pemeliharaan.</b> Sengaja tidak memeriksa transaksi aktif—"usable" di sini berarti bisa
     * membuat criteria/query, bukan menjamin ada transaksi. Jangan menambah efek samping di method
     * ini; ia harus murni pemeriksaan.</p>
     *
     * @param session session yang diperiksa (boleh null)
     * @return {@code true} bila session tidak null dan masih open
     */
    public static boolean isSessionUsable(Session session) {
        try {
            return session != null && session.isOpen();
        } catch (Exception e) {
            return false;
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * Menjamin sebuah session usable: mengembalikan session parameter bila masih hidup, atau session
     * native sebagai pengganti.
     *
     * <p><b>Tujuan.</b> Jembatan untuk banyak method LAMA yang menerima {@code Session} sebagai
     * argumen tetapi kadang dipanggil dengan session yang sudah null/closed (mis. session request
     * yang sudah berakhir saat dipakai di callback/timer). Daripada melempar exception, method ini
     * menyediakan session pengganti yang open sehingga jalur lama tetap berjalan.</p>
     *
     * <p><b>Cara kerja.</b> Bila {@link #isSessionUsable(Session)} pada {@code session} true, session
     * itu dikembalikan apa adanya; jika tidak, dikembalikan {@link #currentNativeSession()}.</p>
     *
     * <p><b>Parameter &amp; Return.</b> {@code session} = session yang mungkin sudah mati.
     * Mengembalikan session yang dijamin usable (parameter atau native).</p>
     *
     * <p><b>Peringatan &amp; pemeliharaan.</b> Gunakan hati-hati: bila proses HARUS berada dalam
     * transaksi tertentu, mengganti dengan session native bisa memutus konteks transaksi. Untuk kasus
     * seperti itu pakai {@link #assertSessionOpen(Session, String)} yang menolak fallback. Method ini
     * cocok untuk operasi baca yang toleran terhadap pergantian session.</p>
     *
     * @param session session asal (boleh null/closed)
     * @return session usable (asal bila masih hidup, selain itu native)
     */
    public static Session ensureOpenSession(Session session) {
        if (isSessionUsable(session)) {
            return session;
        }
        return currentNativeSession();
    }

    /**
     * Membuat {@link Criteria} secara aman dari session yang mungkin sudah closed.
     *
     * <p><b>Tujuan.</b> Menyediakan pengganti langsung untuk pola {@code session.createCriteria(...)}
     * yang rawan {@code SessionException} bila session-nya sudah ditutup. File berisiko tinggi dapat
     * dipindahkan bertahap dari {@code session.createCriteria(X.class)} menjadi
     * {@code HibernateUtil.createCriteria(session, X.class)} tanpa mengubah logika query.</p>
     *
     * <p><b>Cara kerja.</b> Memanggil {@link #ensureOpenSession(Session)} terlebih dahulu sehingga
     * criteria selalu dibangun di atas session yang usable (parameter bila hidup, atau native sebagai
     * pengganti), lalu mendelegasikan ke {@code createCriteria(persistentClass)}.</p>
     *
     * <p><b>Parameter &amp; Return.</b> {@code session} = session asal (boleh mati);
     * {@code persistentClass} = kelas entity target. Mengembalikan {@link Criteria} yang siap diberi
     * restriction/projection.</p>
     *
     * <p><b>Peringatan &amp; pemeliharaan.</b> Karena bisa berganti ke session native, jangan memakai
     * method ini untuk query yang harus terikat transaksi caller tertentu—pada kasus itu pastikan
     * session masih hidup dengan {@link #assertSessionOpen(Session, String)}. Tipe raw dibiarkan demi
     * kompatibilitas pemanggil lama.</p>
     *
     * @param session         session asal (boleh null/closed)
     * @param persistentClass kelas entity target criteria
     * @return Criteria di atas session yang usable
     */
    @SuppressWarnings({ "rawtypes" })
    public static Criteria createCriteria(Session session, Class persistentClass) {
        return ensureOpenSession(session).createCriteria(persistentClass);
    }

    /**
     * Memastikan session benar-benar open, melempar {@link IllegalStateException} bila tidak.
     *
     * <p><b>Tujuan.</b> Guard KERAS untuk operasi yang TIDAK boleh diam-diam berpindah ke session
     * lain—misalnya rangkaian tulis yang harus berada dalam satu transaksi milik caller. Berbeda dari
     * {@link #ensureOpenSession(Session)} yang toleran (fallback), method ini sengaja gagal cepat agar
     * bug "session sudah mati di tengah transaksi" terlihat jelas, bukan tersembunyi dengan data
     * yang ditulis ke transaksi yang salah.</p>
     *
     * <p><b>Cara kerja.</b> Bila {@link #isSessionUsable(Session)} false, melempar
     * {@link IllegalStateException} dengan pesan yang menyertakan {@code context} (atau teks default
     * "Session Hibernate" bila context kosong) untuk memudahkan penelusuran.</p>
     *
     * <p><b>Parameter.</b> {@code session} = session yang divalidasi; {@code context} = label sumber
     * pemanggil untuk pesan error (boleh null/kosong).</p>
     *
     * <p><b>Pemeliharaan.</b> Pakai ini di awal blok kritis transaksional. Karena melempar
     * unchecked exception, pemanggil tidak wajib menangkapnya—biarkan merambat agar kegagalan
     * terdeteksi. Jangan menggantinya dengan fallback otomatis pada jalur transaksional.</p>
     *
     * @param session session yang harus open
     * @param context label konteks untuk pesan error
     * @throws IllegalStateException bila session null/closed
     */
    public static void assertSessionOpen(Session session, String context) {
        if (!isSessionUsable(session)) {
            throw new IllegalStateException((context == null || context.trim().length() == 0
                    ? "Session Hibernate" : context) + " sudah null atau tertutup.");
        }
    }

    /**
     * Menutup session native ThreadLocal saat ini secara TERTUNDA (asynchronous) di thread terpisah.
     *
     * <p><b>Tujuan.</b> Beberapa alur (terutama yang melibatkan rendering/streaming hasil ke klien)
     * masih membutuhkan session sesaat setelah handler utama selesai—menutup session terlalu cepat
     * akan memutus lazy-load yang sedang berjalan. Method ini melepaskan session dari ThreadLocal
     * lebih dulu lalu menundanya ditutup sebentar agar pekerjaan ekor sempat selesai.</p>
     *
     * <p><b>Cara kerja.</b> (1) Mengambil sekaligus menghapus session dari {@code MAP} via
     * {@link #getAndRemoveThreadLocalSession()} (sehingga thread tidak lagi memegang referensinya).
     * (2) Bila null, langsung kembali. (3) Bila ada, sebuah {@link Thread} daemon dibuat yang tidur
     * ~1,5 detik lalu memanggil {@link #closeSessionQuietly(Session)} untuk menutup rapi (rollback +
     * disconnect + close).</p>
     *
     * <p><b>Penanganan error &amp; konkurensi.</b> Karena session sudah dilepas dari ThreadLocal,
     * thread saat ini bebas membuka session baru tanpa bentrok. Exception saat {@code sleep}
     * diteruskan ke {@code Common.tampilErrorJikaAdmin}. {@code throws HibernateException} ada demi
     * kompatibilitas tanda tangan lama meski jalur normal tidak melemparnya.</p>
     *
     * <p><b>Pemeliharaan.</b> Gunakan hanya bila memang ada pekerjaan ekor yang butuh session sesaat;
     * untuk kasus biasa, {@link #closeSession()} (sinkron) lebih hemat. Jangan memperpanjang jeda
     * tidur tanpa alasan—itu menahan koneksi pool lebih lama.</p>
     *
     * @throws HibernateException demi kompatibilitas tanda tangan lama
     */
    public static void closeSessionAfter() throws HibernateException {
        final Session session = getAndRemoveThreadLocalSession();
        if (session == null) {
            return;
        }
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1500);
                } catch (Exception e) {
                    Common.tampilErrorJikaAdmin(e);
                }
                closeSessionQuietly(session);
            }
        });
        thread.start();
    }

    /**
     * Me-rollback transaksi pada session native ThreadLocal lalu menutup session-nya.
     *
     * <p><b>Tujuan.</b> Dipakai pada jalur penanganan error: ketika sebuah operasi tulis gagal,
     * method ini membatalkan perubahan yang belum ter-commit dan membereskan session agar tidak ada
     * koneksi yang menggantung dalam keadaan "idle in transaction".</p>
     *
     * <p><b>Cara kerja.</b> (1) Mengambil session dari {@code MAP}. (2) <b>Wajib</b> memeriksa
     * {@code isOpen()} sebelum {@code getTransaction()}—pada session yang sudah ditutup,
     * {@code getTransaction()} melempar {@code SessionException: Session is closed!}; situasi ini
     * nyata karena kode lain bisa menutup session langsung tanpa lewat helper sementara {@code MAP}
     * masih memegang referensinya. (3) Bila transaksi ada, aktif, belum commit, dan belum rollback,
     * baru di-{@code rollback}. (4) Apa pun yang terjadi, {@link #closeSession()} dipanggil di
     * {@code finally}.</p>
     *
     * <p><b>Penanganan error.</b> Seluruh blok dibungkus try/catch; exception saat rollback diteruskan
     * ke {@code Common.tampilErrorJikaAdmin} tanpa menghalangi penutupan session di {@code finally}.</p>
     *
     * <p><b>Pemeliharaan.</b> Jangan menghapus pengecekan {@code isOpen()}—itu pengaman terhadap
     * {@code SessionException}. Method ini menyasar session NATIVE; untuk session ZK, jangan
     * memanggilnya.</p>
     */
    public static void rollbackTransaction() {
        Session session = null;
        try {
            session = MAP.get();
            /*
             * Wajib cek isOpen(): getTransaction() pada session yang sudah ditutup
             * melempar SessionException "Session is closed!". Session di ThreadLocal
             * bisa saja sudah ditutup langsung via session.close() oleh kode lain
             * tanpa melalui helper, sehingga MAP masih menyimpan referensinya.
             */
            if (session != null && session.isOpen()) {
                Transaction tx = session.getTransaction();
                if (tx != null && tx.isActive() && !tx.wasCommitted() && !tx.wasRolledBack()) {
                    tx.rollback();
                }
            }
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
        } finally {
            closeSession();
        }
    }

    /**
     * Menutup session native ThreadLocal milik thread saat ini (sinkron).
     *
     * <p><b>Tujuan.</b> Pasangan penutup untuk {@link #currentNativeSession()}/{@link #openSession()}
     * pada konteks non-ZK. Dipanggil di blok {@code finally} pekerjaan latar/API/laporan agar koneksi
     * dikembalikan ke pool dengan bersih.</p>
     *
     * <p><b>Cara kerja.</b> Mengambil sekaligus menghapus session dari {@code MAP}
     * ({@link #getAndRemoveThreadLocalSession()}) lalu menutupnya rapi via
     * {@link #closeSessionQuietly(Session)} (yang merollback transaksi/koneksi sebelum close). Aman
     * dipanggil meski tidak ada session (no-op bila null).</p>
     *
     * <p><b>Aturan pakai (PENTING).</b> JANGAN memakai method ini untuk menutup session ZK/request
     * ({@link #currentSession()})—siklus hidup session ZK dikelola framework. Memanggilnya pada
     * konteks ZK dapat merusak request berjalan.</p>
     *
     * <p><b>Pemeliharaan.</b> Idempoten dan defensif; aman dipanggil berulang. Untuk penutupan
     * tertunda (ada pekerjaan ekor), gunakan {@link #closeSessionAfter()}.</p>
     */
    public static void closeSession() {
        Session session = getAndRemoveThreadLocalSession();
        closeSessionQuietly(session);
    }

    /**
     * Menutup sebuah session secara aman dan TUNTAS: rollback transaksi &amp; koneksi, disconnect,
     * lalu close.
     *
     * <p><b>Tujuan.</b> Inti higiene koneksi seluruh aplikasi. Tujuannya bukan sekadar memanggil
     * {@code close()}, melainkan memastikan koneksi JDBC tidak dikembalikan ke pool dalam keadaan
     * "idle in transaction" sambil memegang row-lock—penyebab kemacetan yang terbukti di
     * {@code pg_stat_activity}. Dipakai oleh {@link #closeSession()}, {@link #closeSessionAfter()},
     * {@link #currentNativeSession()}, dan dapat dipanggil langsung untuk menutup session hasil
     * {@link #openSession()}.</p>
     *
     * <h4>Cara kerja (urutan disengaja)</h4>
     * <ol>
     *   <li>No-op bila {@code session} null.</li>
     *   <li>{@link #removeThreadLocalSessionIfSame(Session)} — bila session ini adalah native session
     *   aktif, lepaskan dari ThreadLocal agar tidak dipakai ulang.</li>
     *   <li>Bila masih open: {@code clear()} (buang objek persisten), lalu <b>rollback transaksi
     *   Hibernate</b> bila aktif, lalu <b>rollback koneksi JDBC</b> bila autocommit mati—karena
     *   dengan {@code autocommit=false} bahkan SELECT membuka transaksi implisit di PostgreSQL.</li>
     *   <li>{@code disconnect()} lalu {@code close()}.</li>
     * </ol>
     *
     * <p><b>Penanganan error.</b> Setiap langkah dibungkus try/catch independen sehingga kegagalan
     * satu langkah tidak menghentikan langkah berikutnya—prinsipnya "apa pun yang terjadi, koneksi
     * harus lepas". {@link Throwable} di lapisan luar pun ditangkap agar penutupan tidak pernah
     * melempar ke pemanggil.</p>
     *
     * <p><b>Parameter &amp; pemeliharaan.</b> {@code session} = session yang akan ditutup (boleh
     * null). Jangan memakai ini untuk session ZK. Jangan mengubah urutan rollback-sebelum-close—itu
     * justru inti perbaikannya.</p>
     *
     * @param session session yang akan ditutup rapi (boleh null)
     */
    public static void closeSessionQuietly(Session session) {
        if (session == null) {
            return;
        }
        removeThreadLocalSessionIfSame(session);
        try {
            if (session.isOpen()) {
                try {
                    session.clear();
                } catch (Exception e) {
                    catatKegagalanPenutupan(e, "clear",
                            "src/ais/database/hibernate/HibernateUtil.java:873");
                    // Abaikan agar proses close tetap lanjut.
                }
                /*
                 * PENTING: rollback sebelum koneksi dikembalikan ke pool.
                 * Dengan hibernate.connection.autocommit=false, SELECT pun
                 * membuka transaksi implisit di PostgreSQL. Pool bawaan
                 * Hibernate 3.6 mengembalikan koneksi TANPA rollback, sehingga
                 * koneksi menggantung "idle in transaction" selamanya sambil
                 * memegang row lock dari UPDATE yang sempat berjalan — terbukti
                 * di pg_stat_activity (idle in transaction berumur belasan
                 * menit dengan query terakhir berupa SELECT).
                 *
                 * KE-FIX (TransactionException "JDBC rollback failed" <- PSQLException
                 * "This connection has been closed."): rollback hanya masuk akal bila koneksi
                 * FISIK masih hidup. Begitu koneksi mati (SSL/socket putus, pool menutup koneksi,
                 * server restart), rollback justru melempar exception BARU yang tidak informatif
                 * dan MENUTUPI penyebab asli di log — persis yang terlihat pada jalur
                 * RepositorySyncScheduler.jalankanSekali -> finally -> closeSessionQuietly.
                 * Karena itu status koneksi diperiksa LEBIH DULU (isConnected + !isClosed), dan
                 * bila toh masih gagal karena koneksi tertutup, itu diperlakukan sebagai derau
                 * shutdown yang wajar: dicatat 1 baris TANPA stack trace. Error nyata tetap
                 * dicatat penuh. Urutan rollback-sebelum-close TIDAK berubah, dan koneksi yang
                 * masih hidup tetap di-rollback seperti semula.
                 */
                java.sql.Connection connection = null;
                boolean connectionUsable = false;
                try {
                    if (session.isConnected()) {
                        connection = session.connection();
                        connectionUsable = connection != null && !connection.isClosed();
                    }
                } catch (Exception e) {
                    connection = null;
                    connectionUsable = false;
                    catatKegagalanPenutupan(e, "cek-koneksi",
                            "src/ais/database/hibernate/HibernateUtil.java:895");
                }
                try {
                    if (connectionUsable && session.getTransaction() != null
                            && session.getTransaction().isActive()) {
                        session.getTransaction().rollback();
                    }
                } catch (Exception e) {
                    catatKegagalanPenutupan(e, "rollback-transaksi",
                            "src/ais/database/hibernate/HibernateUtil.java:890");
                    // Abaikan agar proses close tetap lanjut.
                }
                try {
                    if (connectionUsable && connection != null && !connection.isClosed()
                            && !connection.getAutoCommit()) {
                        connection.rollback();
                    }
                } catch (Exception e) {
                    catatKegagalanPenutupan(e, "rollback-koneksi",
                            "src/ais/database/hibernate/HibernateUtil.java:900");
                    // Abaikan agar proses close tetap lanjut.
                }
                try {
                    session.disconnect();
                } catch (Exception e) {
                    catatKegagalanPenutupan(e, "disconnect",
                            "src/ais/database/hibernate/HibernateUtil.java:905");
                    // Abaikan agar proses close tetap lanjut.
                }
                try {
                    session.close();
                } catch (Exception e) {
                    if (isConnectionDead(e)) {
                        catatKegagalanPenutupan(e, "close",
                                "src/ais/database/hibernate/HibernateUtil.java:910");
                    } else {
                        Common.tampilErrorJikaAdmin(e);
                    }
                }
            }
        } catch (Exception e) {
            if (isConnectionDead(e)) {
                catatKegagalanPenutupan(e, "close-session",
                        "src/ais/database/hibernate/HibernateUtil.java:915");
            } else {
                Common.tampilErrorJikaAdmin(e);
            }
        } catch (Throwable t) {
            try {
                if (isConnectionDead(t)) {
                    catatKegagalanPenutupan(t, "close-session",
                            "src/ais/database/hibernate/HibernateUtil.java:919");
                } else {
                    Common.tampilErrorJikaAdmin(new Exception(t));
                }
            } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/database/hibernate/HibernateUtil.java:919");
            }
        }
    }

    /**
     * Mencatat kegagalan salah satu langkah pembersihan session, sambil membedakan
     * "koneksi memang sudah tertutup" (derau shutdown yang WAJAR) dari error nyata.
     *
     * <p><b>Tujuan.</b> Menghentikan banjir stack trace sekunder ketika koneksi JDBC fisik
     * mati. Pada kondisi itu setiap langkah pembersihan (rollback/disconnect/close) melempar
     * exception BARU ({@code TransactionException: JDBC rollback failed} <- {@code PSQLException:
     * This connection has been closed.}) yang tidak menambah informasi apa pun dan justru
     * MENUTUPI exception asli yang sudah dicatat pemanggil (mis. {@code RepositorySyncService}
     * lewat flag {@code SyncSummary.connectionLost}).</p>
     *
     * <p><b>Cara kerja.</b> Bila {@link #isConnectionDead(Throwable)} bernilai {@code true},
     * hanya satu baris ringkas dicetak ke stdout (tanpa stack trace, tanpa audit); selain itu
     * exception dicatat penuh lewat {@code ErrorAuditUtil.record} seperti sebelumnya. Seluruh
     * badan method dibungkus try/catch karena penutupan session TIDAK BOLEH melempar apa pun
     * ke pemanggil.</p>
     *
     * @param e      exception yang terjadi (boleh null)
     * @param tahap  nama langkah pembersihan, untuk log ringkas
     * @param lokasi penanda lokasi kode untuk audit
     */
    private static void catatKegagalanPenutupan(Throwable e, String tahap, String lokasi) {
        if (e == null) {
            return;
        }
        try {
            if (isConnectionDead(e)) {
                // Koneksi fisik sudah tertutup: konsekuensi WAJAR dari kegagalan yang sudah
                // tercatat di tempat lain. Cukup 1 baris; JANGAN stack trace / audit.
                System.out.println("[HibernateUtil] Langkah '" + tahap
                        + "' dilewati karena koneksi database sudah tertutup: " + e.getMessage());
                return;
            }
            ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) " + lokasi);
        } catch (Throwable ignored) {
            // Penutupan session tidak boleh melempar apa pun ke pemanggil.
        }
    }

    /**
     * Deteksi apakah exception (atau salah satu cause-nya) menandakan koneksi JDBC FISIK sudah
     * mati/tertutup — bukan sekadar transaksi/state Hibernate yang perlu di-rollback.
     *
     * <p><b>Tujuan.</b> Menjadi SATU definisi tunggal bagi seluruh aplikasi. Idiom ini semula
     * hidup sebagai helper privat {@code isConnectionDead} di
     * {@code ais.action.master.repository.RepositorySyncService} (dipakai berpasangan dengan flag
     * {@code SyncSummary.connectionLost}); helper di sana sekarang mendelegasikan ke sini supaya
     * tidak ada dua daftar pola yang bisa saling menyimpang.</p>
     *
     * <p><b>Cara kerja.</b> Menyusuri rantai {@code getCause()} (dibatasi 12 tingkat, plus
     * penjagaan cause yang menunjuk dirinya sendiri) dan mengembalikan {@code true} bila
     * menemukan {@link org.hibernate.exception.JDBCConnectionException},
     * {@link java.net.SocketException}, {@link javax.net.ssl.SSLException}, atau pesan khas
     * koneksi/statement yang sudah tertutup.</p>
     *
     * <p><b>Pemeliharaan.</b> Pada kondisi ini {@code rollback()}/{@code clear()}/
     * {@code beginTransaction()} pada session yang sama TIDAK akan memulihkan apa pun — semua
     * akan gagal lagi memakai koneksi yang sama. Jangan menambah pola yang terlalu umum
     * (mis. sekadar "closed") supaya error nyata tidak ikut terbungkam.</p>
     *
     * @param e exception yang diperiksa (boleh null)
     * @return {@code true} bila jelas merupakan kasus koneksi fisik sudah mati/tertutup
     */
    public static boolean isConnectionDead(Throwable e) {
        Throwable cur = e;
        int guard = 0;
        while (cur != null && guard++ < 12) {
            if (cur instanceof org.hibernate.exception.JDBCConnectionException
                    || cur instanceof java.net.SocketException
                    || cur instanceof javax.net.ssl.SSLException) {
                return true;
            }
            String msg = cur.getMessage();
            if (msg != null) {
                String low = msg.toLowerCase();
                if (low.contains("connection has been closed") || low.contains("connection is closed")
                        || low.contains("connection has already been closed")
                        || low.contains("connection already closed")
                        || low.contains("statement has been closed")
                        || low.contains("resultset has been closed")
                        || low.contains("socket closed")
                        || low.contains("connection closed")
                        || low.contains("i/o error occurred while sending to the backend")) {
                    return true;
                }
            }
            Throwable next = cur.getCause();
            if (next == cur) {
                break;
            }
            cur = next;
        }
        return false;
    }

    /**
     * Alias untuk {@link #closeSessionQuietly(Session)}, menutup session yang dibuka manual.
     *
     * <p><b>Tujuan &amp; cara kerja.</b> Memberi nama yang lebih eksplisit ("session yang DIBUKA")
     * bagi pemanggil yang menutup hasil {@link #openSession()}; secara fungsional identik dengan
     * {@link #closeSessionQuietly(Session)} dan mendelegasikan langsung ke sana. Disediakan demi
     * keterbacaan dan kompatibilitas tanda tangan di kode lama.</p>
     *
     * <p><b>Parameter &amp; pemeliharaan.</b> {@code session} = session yang akan ditutup (boleh
     * null). Tidak ada perilaku tambahan; bila logika penutupan perlu diubah, ubah di
     * {@link #closeSessionQuietly(Session)} agar kedua nama tetap konsisten.</p>
     *
     * @param session session yang akan ditutup rapi (boleh null)
     */
    public static void closeOpenedSessionQuietly(Session session) {
        closeSessionQuietly(session);
    }

    /**
     * Mengecek apakah session yang diberikan adalah persis native session ThreadLocal thread ini.
     *
     * <p><b>Tujuan.</b> Membantu kode pemanggil memutuskan tanggung jawab penutupan: bila sebuah
     * session ternyata native session aktif milik thread, menutupnya berarti membereskan ThreadLocal;
     * bila bukan (mis. session yang dibuka khusus), perlakuannya bisa berbeda. Mencegah penutupan
     * ganda atau penutupan session yang salah.</p>
     *
     * <p><b>Cara kerja.</b> Membandingkan referensi {@code MAP.get() == session} (identitas objek,
     * bukan equals). Dibungkus try/catch sehingga mengembalikan {@code false} bila pembacaan
     * ThreadLocal bermasalah atau {@code session} null.</p>
     *
     * <p><b>Parameter &amp; Return.</b> {@code session} = session yang diuji. Mengembalikan
     * {@code true} hanya bila identik dengan native session ThreadLocal saat ini.</p>
     *
     * <p><b>Pemeliharaan.</b> Gunakan perbandingan identitas; jangan menggantinya dengan {@code equals}
     * karena dua session berbeda tidak pernah "sama" secara logis dan equals bisa menyesatkan.</p>
     *
     * @param session session yang diperiksa
     * @return {@code true} bila session ini adalah native session ThreadLocal thread saat ini
     */
    public static boolean isCurrentNativeSession(Session session) {
        try {
            return session != null && MAP.get() == session;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Mengambil sekaligus MENGHAPUS native session dari {@link ThreadLocal} {@code MAP}.
     *
     * <p><b>Tujuan.</b> Operasi "pop" atomik secara logis: pemanggil memperoleh session terakhir
     * sambil memastikan ThreadLocal langsung kosong, sehingga thread tidak akan tanpa sengaja memakai
     * ulang session yang sedang/akan ditutup. Dipakai {@link #closeSession()} dan
     * {@link #closeSessionAfter()}.</p>
     *
     * <p><b>Cara kerja.</b> Membaca {@code MAP.get()} (try/catch → null bila bermasalah), lalu
     * membersihkan ThreadLocal dengan {@code MAP.set(null)} DAN {@code MAP.remove()} (keduanya
     * di-try/catch terpisah, gaya defensif). Mengembalikan session yang sempat dibaca.</p>
     *
     * <p><b>Return &amp; pemeliharaan.</b> Mengembalikan session yang dilepas, atau null bila tidak
     * ada. Method privat; pembersihan ganda ({@code set(null)} + {@code remove()}) sengaja agar aman
     * di berbagai implementasi ThreadLocal/lingkungan container. Jangan menghapus salah satunya.</p>
     *
     * @return session native yang dilepas dari ThreadLocal, atau null
     */
    private static Session getAndRemoveThreadLocalSession() {
        Session session = null;
        try {
            session = MAP.get();
        } catch (Exception e) {
            session = null;
        }
        try {
            MAP.set(null);
        } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/database/hibernate/HibernateUtil.java:998");
        }
        try {
            MAP.remove();
        } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/database/hibernate/HibernateUtil.java:1002");
        }
        return session;
    }

    /**
     * Membersihkan {@link ThreadLocal} {@code MAP} HANYA bila isinya adalah session yang sama persis
     * dengan {@code session}.
     *
     * <p><b>Tujuan.</b> Saat menutup sebuah session, kita ingin melepaskannya dari ThreadLocal—tetapi
     * hanya bila session itu memang yang sedang tercatat di thread ini. Tanpa pengecekan identitas,
     * menutup session "lepas" (mis. hasil {@link #openSession()} yang tak pernah masuk ThreadLocal)
     * bisa keliru menghapus native session aktif milik thread yang masih dipakai.</p>
     *
     * <p><b>Cara kerja.</b> No-op bila {@code session} null. Bila {@code MAP.get() == session}
     * (perbandingan identitas), bersihkan via {@code set(null)} dan {@code remove()} (masing-masing
     * di-try/catch). Seluruhnya dibungkus penangkapan agar tidak pernah melempar.</p>
     *
     * <p><b>Parameter &amp; pemeliharaan.</b> {@code session} = session yang sedang ditutup. Dipanggil
     * dari {@link #closeSessionQuietly(Session)} dan {@link #currentNativeSession()}. Pertahankan
     * perbandingan identitas ({@code ==}); itulah yang membuatnya aman terhadap session lepas.</p>
     *
     * @param session session yang, bila identik dengan isi ThreadLocal, akan dilepas
     */
    private static void removeThreadLocalSessionIfSame(Session session) {
        if (session == null) {
            return;
        }
        try {
            if (MAP.get() == session) {
                try {
                    MAP.set(null);
                } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/database/hibernate/HibernateUtil.java:1034");
                }
                try {
                    MAP.remove();
                } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/database/hibernate/HibernateUtil.java:1038");
                }
            }
        } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/database/hibernate/HibernateUtil.java:1041");
        }
    }
}
