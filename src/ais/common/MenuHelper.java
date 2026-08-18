package ais.common;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import org.hibernate.Session;
import org.hibernate.Transaction;

import ais.action.master.sekolah.util.SekolahUtil;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Konfigurasi;
import ais.database.model.Menu;
import ais.database.model.RolePrivilage;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.database.model.sekolah.Sekolah;

public class MenuHelper {

    /**
     * Mapping dinamis Icon FontAwesome.
     */
    public static String getIconForMenu(String label) {
        if (label == null || label.trim().isEmpty()) return "fas fa-angle-right";
        String lbl = label.toLowerCase();

        // Kantin / Usaha Koperasi (prioritas — sebelum aturan umum di bawah)
        if (lbl.contains("kasir")) return "fas fa-cash-register";
        if (lbl.contains("pesanan")) return "fas fa-receipt";
        if (lbl.contains("diskon")) return "fas fa-percent";
        if (lbl.contains("notifikasi")) return "fas fa-bell";
        if (lbl.contains("pedagang") || lbl.contains("mitra")) return "fas fa-store";
        if (lbl.contains("penyedia") || lbl.contains("supplier")) return "fas fa-truck";
        if (lbl.contains("kios") || lbl.contains("toko")) return "fas fa-store";

        if (lbl.contains("akademik") || lbl.contains("program") || lbl.contains("perkuliahan") || lbl.contains("kurikulum")) return "fas fa-graduation-cap";
        if (lbl.contains("keuangan") || lbl.contains("pembayaran") || lbl.contains("tarif") || lbl.contains("tagihan")) return "fas fa-money-bill-wave";
        if (lbl.contains("akuntansi") || lbl.contains("jurnal")) return "fas fa-calculator";
        if (lbl.contains("pengadaan") || lbl.contains("barang") || lbl.contains("stok") || lbl.contains("aset")) return "fas fa-boxes";
        if (lbl.contains("surat") || lbl.contains("pesan")) return "fas fa-envelope-open-text";
        if (lbl.contains("akreditasi") || lbl.contains("lkps") || lbl.contains("ban-pt") || lbl.contains("ban pt")) return "fas fa-certificate";
        if (lbl.contains("repository") || lbl.contains("repositori") || lbl.contains("dspace") || lbl.contains("turnitin")) return "fas fa-archive";
        if (lbl.contains("hrd") || lbl.contains("kinerja") || lbl.contains("kepegawaian") || lbl.contains("dosen")) return "fas fa-user-tie";
        if (lbl.contains("antar jemput") || lbl.contains("penjemput") || lbl.contains("jemputan")) return "fas fa-bus";
        if (lbl.contains("gerbang") || lbl.contains("rfid") || lbl.contains("qr")) return "fas fa-id-card";
        if (lbl.contains("kendaraan") || lbl.contains("sopir")) return "fas fa-shuttle-van";
        if (lbl.contains("tracer") || lbl.contains("alumni")) return "fas fa-search-location";
        if (lbl.contains("cbt") || lbl.contains("ujian") || lbl.contains("soal")) return "fas fa-laptop-code";
        if (lbl.contains("anggota") || lbl.contains("mahasiswa") || lbl.contains("siswa")) return "fas fa-users";
        if (lbl.contains("pmb") || lbl.contains("pendaftar") || lbl.contains("penerimaan")) return "fas fa-user-plus";
        if (lbl.contains("laporan") || lbl.contains("statistik") || lbl.contains("dashboard") || lbl.contains("dasbor")) return "fas fa-chart-pie";
        if (lbl.contains("asrama") || lbl.contains("pesantren") || lbl.contains("gedung") || lbl.contains("ruang")) return "fas fa-building";
        if (lbl.contains("mbkm") || lbl.contains("kkn") || lbl.contains("magang")) return "fas fa-hands-helping";
        if (lbl.contains("wisuda") || lbl.contains("judisium") || lbl.contains("skripsi") || lbl.contains("tugas akhir")) return "fas fa-user-graduate";
        if (lbl.contains("apotik") || lbl.contains("klinik") || lbl.contains("kesehatan")) return "fas fa-clinic-medical";
        if (lbl.contains("bahasa") || lbl.contains("toefl")) return "fas fa-language";
        if (lbl.contains("prestasi") || lbl.contains("karya") || lbl.contains("beasiswa")) return "fas fa-trophy";
        if (lbl.contains("laboratorium") || lbl.contains("praktikum")) return "fas fa-flask";
        if (lbl.contains("pengaturan") || lbl.contains("konfigurasi") || lbl.contains("sistem") || lbl.contains("role") || lbl.contains("user")) return "fas fa-cogs";
        if (lbl.contains("nilai") || lbl.contains("khs") || lbl.contains("krs")) return "fas fa-sort-numeric-up";
        if (lbl.contains("kalender") || lbl.contains("jadwal")) return "far fa-calendar-alt";
        if (lbl.contains("pengajuan") || lbl.contains("workflow")) return "fas fa-paper-plane";
        
        return "fas fa-angle-right"; 
    }

    public static void cleanupObsoleteMenus() {
        // ID menu lama (Antar Jemput, Repository, Akreditasi) yang sudah tidak dipakai
        String ids = "145,14501,14502,14503,14504,14505,"
                   + "158,15801,15802,15803,15804,15805,"
                   + "159,15901,"
                   // LKPS S1 (Unggul) — disembunyikan, digantikan LAMDIK
                   + "16101";
        Session session = null;
        Transaction tx = null;
        try {
            session = HibernateUtil.currentNativeSession();
            tx = session.beginTransaction();
            session.createSQLQuery("DELETE FROM public.job_has_menu WHERE menu IN (" + ids + ")").executeUpdate();
            session.createSQLQuery("DELETE FROM public.menu WHERE id IN (" + ids + ")").executeUpdate();
            // Sembunyikan semua menu "Akreditasi Online (Sapto)" (label mengandung "Sapto")
            // yang mungkin ada di DB dengan berbagai ID — set aktif=false (soft-hide)
            session.createSQLQuery(
                "UPDATE public.menu SET aktif=false WHERE LOWER(label) LIKE '%sapto%'"
            ).executeUpdate();
            tx.commit();
            System.out.println("Cleanup menu obsolete selesai.");
        } catch (Exception e) {
            if (tx != null) try { tx.rollback(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/MenuHelper.java:94");}
            System.err.println("Gagal cleanup menu obsolete: " + e.getMessage());
            e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/MenuHelper.java:96");
        } finally {
            if (session != null) {
                try { session.disconnect(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/MenuHelper.java:99");}
                try { session.close(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/MenuHelper.java:100");}
            }
            HibernateUtil.closeSession();
        }
    }

    /**
     * SEMENTARA (permintaan user 17-07-2026): sembunyikan seluruh modul <b>"Sistem Informasi Rumah
     * Sakit" (SIRS)</b> dari mega-menu secara DEFAULT pada SETIAP startup, dengan men-set
     * {@code aktif=false} pada menu INDUK-nya (id {@value #SIRS_PARENT_MENU_ID}).
     *
     * <p>Renderer mega-menu ({@code buildMegaMenuItem}) berhenti (return) pada induk yang
     * {@code aktif=false}, sehingga SELURUH cabang di bawahnya (Admisi, Poliklinik, Apotik, Kasir,
     * Pendataan &amp; Penjadwalan, Perencanaan &amp; Pembelian Barang Medis, Pelayanan, Dasbor)
     * ikut tersembunyi. Status {@code aktif} tiap ANAK TIDAK diubah — jadi bila modul ini nanti
     * diaktifkan kembali, susunan &amp; visibilitas anak kembali persis seperti semula.</p>
     *
     * <p><b>Mengaktifkan kembali (nanti atas perintah user):</b> ubah nilai UPDATE di bawah menjadi
     * {@code aktif=true}, atau cukup hapus pemanggilan method ini di {@code AppStartupListener}
     * lalu set induk {@code aktif=true} sekali (mis. lewat menu Modul), kemudian deploy ulang.
     * Dijalankan tiap startup agar konsisten di SEMUA sistem sampai diperintahkan sebaliknya.</p>
     */
    public static void sembunyikanMenuSirsSementara() {
        Session session = null;
        Transaction tx = null;
        try {
            session = HibernateUtil.currentNativeSession();
            tx = session.beginTransaction();
            session.createSQLQuery("UPDATE public.menu SET aktif=false WHERE id = " + SIRS_PARENT_MENU_ID)
                    .executeUpdate();
            tx.commit();
            System.out.println("Menu SIRS disembunyikan default (aktif=false) pada induk id " + SIRS_PARENT_MENU_ID + ".");
        } catch (Exception e) {
            if (tx != null) try { tx.rollback(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) MenuHelper.sembunyikanMenuSirsSementara.rollback");}
            System.err.println("Gagal menyembunyikan menu SIRS: " + e.getMessage());
            ais.common.ErrorAuditUtil.record(e, "auto-audit MenuHelper.sembunyikanMenuSirsSementara");
        } finally {
            if (session != null) {
                try { session.disconnect(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) MenuHelper.sembunyikanMenuSirsSementara.disconnect");}
                try { session.close(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) MenuHelper.sembunyikanMenuSirsSementara.close");}
            }
            HibernateUtil.closeSession();
        }
    }

    /**
     * Hapus PERMANEN baris menu DUPLIKAT yang sudah diverifikasi user (18-07-2026). Dipisah dari
     * {@link #cleanupObsoleteMenus()} yang dinonaktifkan (pernah salah hapus) — daftar ini KECIL,
     * spesifik, dan diverifikasi manual terhadap data live agar aman.
     *
     * <p>Saat ini: <b>id 750287</b> "Pengumuman Akademik" (Root 111 / Child 5710, url
     * {@code /pages/master/pengumuman_akademis.zul}) — DUPLIKAT dari menu resmi id 186 "Pengumuman
     * Akademis" (Child 223) yang ber-url sama. Baris ini juga sudah dihapus dari
     * {@link MenuSnapshotData} agar tidak dipulihkan lagi oleh {@link #ensureMenusDariSnapshot()}.</p>
     *
     * <p>Menghapus tautan role ({@code job_has_menu}) lebih dulu, lalu baris {@code menu}. Idempoten
     * (DELETE WHERE id IN — aman walau baris sudah tidak ada). Dijalankan tiap startup agar konsisten
     * di semua sistem.</p>
     */
    public static void hapusMenuDuplikatBawaan() {
        final String ids = "750287";
        Session session = null;
        Transaction tx = null;
        try {
            session = HibernateUtil.currentNativeSession();
            tx = session.beginTransaction();
            session.createSQLQuery("DELETE FROM public.job_has_menu WHERE menu IN (" + ids + ")").executeUpdate();
            session.createSQLQuery("DELETE FROM public.menu WHERE id IN (" + ids + ")").executeUpdate();
            tx.commit();
            System.out.println("Hapus menu duplikat bawaan selesai: id " + ids + ".");
        } catch (Exception e) {
            if (tx != null) try { tx.rollback(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) MenuHelper.hapusMenuDuplikatBawaan.rollback");}
            System.err.println("Gagal hapus menu duplikat bawaan: " + e.getMessage());
            ais.common.ErrorAuditUtil.record(e, "auto-audit MenuHelper.hapusMenuDuplikatBawaan");
        } finally {
            if (session != null) {
                try { session.disconnect(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) MenuHelper.hapusMenuDuplikatBawaan.disconnect");}
                try { session.close(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) MenuHelper.hapusMenuDuplikatBawaan.close");}
            }
            HibernateUtil.closeSession();
        }
    }

    // =========================================================================
    // SNAPSHOT MENU SEEDING (pemulihan menu hilang)
    // =========================================================================

    /**
     * Memulihkan baris menu yang HILANG dari tabel <code>public.menu</code> berdasarkan
     * snapshot resmi {@link MenuSnapshotData} (export <code>Menu_260712042919.xlsx</code>,
     * 1.210 baris, diverifikasi pengguna). Latar belakang: pengguna menemukan sejumlah
     * menu lenyap dari mega-menu (kasus nyata: grup "Keuangan Mahasiswa" &rarr;
     * "Pembayaran Mahasiswa" dan turunannya) tanpa jejak penghapusan yang disengaja —
     * kemungkinan terhapus oleh operasi manual/migrasi. Method ini menjadi jaring
     * pengaman permanen: dipanggil sekali setiap startup aplikasi (dari
     * {@code AppStartupListener}, SESUDAH {@link #cleanupObsoleteMenus()} agar menu yang
     * memang sengaja dihapus tidak hidup kembali — snapshot sudah dipastikan tidak
     * beririsan dengan daftar ID cleanup tersebut).
     * <p>
     * Cara kerja (idempoten &amp; hemat kueri):
     * <ol>
     *   <li>Ambil SELURUH id menu yang sudah ada dengan satu kueri
     *       (<code>SELECT id FROM public.menu</code>) ke dalam {@link java.util.Set}.</li>
     *   <li>Untuk tiap baris snapshot, bila id-nya BELUM ada &rarr; INSERT baris menu
     *       baru (id, root, child, label, url, bigIcon, nomorUrut, aktif=true,
     *       tampilDiSekolah/Pt=true). Baris yang sudah ada TIDAK disentuh sama sekali,
     *       sehingga penyesuaian label/urutan/nonaktif oleh admin tetap terjaga.</li>
     *   <li>Setiap baris di-commit dalam TRANSAKSI SENDIRI (bukan satu transaksi besar
     *       utk seluruh snapshot &mdash; lihat catatan root-cause di bawah); nilai
     *       numerik (id/root/child/nomorUrut) divalidasi dulu terhadap jangkauan kolom
     *       {@code integer} (4-byte) sebelum insert; kegagalan per-baris dicatat ke
     *       ErrorLog tanpa menggagalkan baris lain.</li>
     * </ol>
     * Catatan hak akses: method ini hanya memulihkan BARIS MENU. Tautan role
     * (<code>job_has_menu</code>) umumnya masih utuh karena penghapusan liar biasanya
     * hanya menyasar tabel menu; bila ada menu pulih yang tidak tampil untuk role
     * tertentu, tambahkan grant-nya via menu Grup Pengguna (atau pola
     * {@code ensurePrivilege}).
     * <p>
     * <b>Catatan root-cause (kegagalan berantai saat insert):</b> kolom {@code id},
     * {@code root}, dan {@code child} di tabel <code>public.menu</code> adalah
     * {@code integer} (int4, 4-byte) di Postgres walau di-mapping sbg {@link Long} di
     * entity {@link Menu} &mdash; sebagian baris snapshot memuat nilai numerik yang
     * melebihi jangkauan int4 (data rusak/typo digit), memicu
     * {@code PSQLException: integer out of range}. Sebelumnya SELURUH ~1.210 baris
     * di-insert dalam SATU transaksi (flush per 50 baris); begitu satu baris kena
     * error tsb, Postgres langsung mem-POISON transaksi itu
     * ({@code current transaction is aborted, commands ignored until end of
     * transaction block}) sehingga SEMUA baris berikutnya dalam transaksi yang sama
     * ikut gagal walau datanya sendiri valid (symptom "could not insert" berantai).
     * Perbaikan: (a) validasi jangkauan int4 sebelum insert &rarr; baris rusak
     * di-skip dgn log peringatan tanpa pernah menyentuh DB; (b) transaksi terisolasi
     * per baris &rarr; kegagalan satu baris (apa pun sebabnya) hanya me-rollback baris
     * itu sendiri, baris lain tetap punya transaksi bersih masing-masing.
     */
    public static void ensureMenusDariSnapshot() {
        Session session = null;
        int dibuat = 0;
        try {
            session = HibernateUtil.currentNativeSession();

            java.util.Set<String> idAda = new java.util.HashSet<String>();
            java.util.List<?> hasil = session.createSQLQuery("SELECT id FROM public.menu").list();
            for (Object o : hasil) {
                if (o != null) {
                    idAda.add(String.valueOf(o).trim());
                }
            }

            java.util.List<String> idBaru = new java.util.ArrayList<String>();
            for (String baris : MenuSnapshotData.DATA) {
                String id = null;
                Transaction txBaris = null;
                try {
                    String[] kolom = baris.split("\\|", -1);
                    if (kolom.length < 7) {
                        continue;
                    }
                    id = kolom[0].trim();
                    if (id.isEmpty() || idAda.contains(id)) {
                        continue;
                    }

                    // Validasi jangkauan int4 (kolom fisik id/root/child/nomorUrut di
                    // Postgres bertipe integer 4-byte) SEBELUM menyentuh DB sama sekali,
                    // supaya baris data rusak (typo digit dsb.) tidak pernah memicu
                    // "integer out of range" yang mem-poison transaksi.
                    long idVal = Long.parseLong(id);
                    long rootVal = Long.parseLong(kolom[1].trim());
                    long childVal = Long.parseLong(kolom[2].trim());
                    long nomorUrutVal = Long.parseLong(kolom[6].trim());
                    if (!dalamJangkauanInt(idVal) || !dalamJangkauanInt(rootVal)
                            || !dalamJangkauanInt(childVal) || !dalamJangkauanInt(nomorUrutVal)) {
                        // Baris data snapshot rusak (nilai kolom int4 kepanjangan) — ini SUDAH
                        // ditangani (dilewati/skip), jadi BUKAN kegagalan yang perlu dicatat
                        // sebagai ERROR ke ErrorAuditUtil (yang menulis log4j level ERROR +
                        // baris ke tabel error_log). Cukup log informatif biasa lalu lanjut ke
                        // baris berikutnya.
                        System.out.println(
                                "ensureMenusDariSnapshot: baris snapshot dilewati (nilai di luar jangkauan int4) '"
                                        + baris + "'");
                        continue;
                    }

                    Menu menu = new Menu();
                    menu.setId(Long.valueOf(idVal));
                    menu.setRoot(Long.valueOf(rootVal));
                    menu.setChild(Long.valueOf(childVal));
                    menu.setLabel(kolom[3].trim().isEmpty() ? null : kolom[3].trim());
                    menu.setUrl(kolom[4].trim().isEmpty() ? null : kolom[4].trim());
                    menu.setBigIcon(kolom[5].trim().isEmpty() ? null : kolom[5].trim());
                    menu.setNomorUrut(Integer.valueOf((int) nomorUrutVal));
                    menu.setAktif(Boolean.TRUE);
                    menu.setTampilDiSekolah(Boolean.TRUE);
                    menu.setTampilDiPt(Boolean.TRUE);

                    // Transaksi TERPISAH per baris (lihat javadoc method): kegagalan DB
                    // (mis. constraint lain yang tak lolos validasi di atas) hanya
                    // merusak baris ini, bukan seluruh batch snapshot.
                    txBaris = session.beginTransaction();
                    session.save(menu);
                    session.flush();
                    txBaris.commit();
                    txBaris = null;

                    idAda.add(id);
                    idBaru.add(id);
                    dibuat++;
                } catch (Exception ePerBaris) {
                    if (txBaris != null) {
                        try {
                            txBaris.rollback();
                        } catch (Exception ignoredRollback) {
                            ais.common.ErrorAuditUtil.record(ignoredRollback,
                                    "ensureMenusDariSnapshot: gagal rollback baris '" + baris + "'");
                        }
                    }
                    try {
                        session.clear();
                    } catch (Exception ignoredClear) {
                        ais.common.ErrorAuditUtil.record(ignoredClear,
                                "ensureMenusDariSnapshot: gagal clear session setelah baris gagal '" + baris + "'");
                    }
                    ais.common.ErrorAuditUtil.record(ePerBaris,
                            "ensureMenusDariSnapshot: gagal memulihkan baris menu snapshot '" + baris + "'");
                }
            }

            // Menu yang baru dipulihkan minimal harus TERLIHAT oleh Administrator —
            // bila penghapusan liar dulu ikut menghapus tautan job_has_menu-nya, baris
            // menu yang pulih tanpa tautan role tidak akan tampil untuk siapa pun.
            // Grant idempoten (NOT EXISTS) + guard FK ke tbmrole; role lain silakan
            // ditautkan admin lewat menu Grup Pengguna. Transaksi sendiri, terpisah
            // dari transaksi per-baris insert di atas.
            if (!idBaru.isEmpty()) {
                Transaction txGrant = null;
                try {
                    txGrant = session.beginTransaction();
                    StringBuilder idList = new StringBuilder();
                    for (String idItem : idBaru) {
                        if (idList.length() > 0) idList.append(",");
                        idList.append(idItem);
                    }
                    int grant = session.createSQLQuery(
                            "INSERT INTO public.job_has_menu (job, menu) "
                          + "SELECT '" + Tbmrole.ADMINISTRATOR + "', m.id FROM public.menu m "
                          + "WHERE m.id IN (" + idList + ") "
                          + "  AND EXISTS (SELECT 1 FROM public.tbmrole tr WHERE tr.roleid = '"
                          + Tbmrole.ADMINISTRATOR + "') "
                          + "  AND NOT EXISTS (SELECT 1 FROM public.job_has_menu x "
                          + "                  WHERE x.job = '" + Tbmrole.ADMINISTRATOR + "' AND x.menu = m.id)")
                            .executeUpdate();
                    txGrant.commit();
                    txGrant = null;
                    System.out.println("ensureMenusDariSnapshot: grant Administrator utk " + grant + " menu pulih.");
                } catch (Exception eGrant) {
                    if (txGrant != null) {
                        try {
                            txGrant.rollback();
                        } catch (Exception ignoredRollback) {
                            ais.common.ErrorAuditUtil.record(ignoredRollback,
                                    "ensureMenusDariSnapshot: gagal rollback grant job_has_menu");
                        }
                    }
                    ais.common.ErrorAuditUtil.record(eGrant,
                            "ensureMenusDariSnapshot: gagal grant job_has_menu Administrator utk menu pulih");
                }
            }

            if (dibuat > 0) {
                System.out.println("ensureMenusDariSnapshot: " + dibuat + " baris menu hilang dipulihkan dari snapshot.");
            }
        } catch (Exception e) {
            ais.common.ErrorAuditUtil.record(e, "ensureMenusDariSnapshot gagal total; menu dipulihkan sejauh ini=" + dibuat);
        } finally {
            if (session != null) {
                try { session.disconnect(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "ensureMenusDariSnapshot: disconnect"); }
                try { session.close(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "ensureMenusDariSnapshot: close"); }
            }
            HibernateUtil.closeSession();
        }
    }

    /**
     * Cek apakah nilai numerik masih dalam jangkauan kolom Postgres {@code integer}
     * (int4, 4-byte: -2147483648..2147483647). Dipakai oleh
     * {@link #ensureMenusDariSnapshot()} utk menyaring baris data rusak SEBELUM
     * insert, agar "integer out of range" tidak pernah terjadi di DB.
     */
    private static boolean dalamJangkauanInt(long value) {
        return value >= Integer.MIN_VALUE && value <= Integer.MAX_VALUE;
    }

    // =========================================================================
    // ANTAR JEMPUT MENU SEEDING
    // =========================================================================

    /**
     * Memastikan semua menu modul Antar Jemput terdaftar di database dan
     * terpasang ke hak akses "am" (Admin Manajemen).
     * Dipanggil otomatis saat aplikasi startup dari AppStartupListener.
     * Method ini idempoten: aman dipanggil berulang kali, tidak akan duplikat.
     */
    public static void ensureAntarJemputMenus() {
        Session session = null;
        Transaction tx = null;
        try {
            session = HibernateUtil.currentNativeSession();
            tx = session.beginTransaction();

            // ── Root menu ─────────────────────────────────────────────────
            Menu root = ensureMenu(session,
                    147L, 0L, 147L,
                    "Antar Jemput",
                    null,
                    "fas fa-bus",
                    10, Boolean.FALSE);

            // ── Child menus ───────────────────────────────────────────────
            Menu m01 = ensureMenu(session, 14701L, 147L, 14701L,
                    "Sistem Antar Jemput",
                    "/pages/master/antarjemput/antar_jemput.zul",
                    "fas fa-bus", 1, Boolean.FALSE);

            Menu m02 = ensureMenu(session, 14702L, 147L, 14702L,
                    "Kendaraan",
                    "/pages/master/antarjemput/panel_kendaraan.zul",
                    "fas fa-shuttle-van", 2, Boolean.FALSE);

            Menu m03 = ensureMenu(session, 14703L, 147L, 14703L,
                    "Rute",
                    "/pages/master/antarjemput/panel_rute.zul",
                    "fas fa-route", 3, Boolean.FALSE);

            Menu m04 = ensureMenu(session, 14704L, 147L, 14704L,
                    "Jadwal",
                    "/pages/master/antarjemput/panel_jadwal.zul",
                    "far fa-calendar-alt", 4, Boolean.FALSE);

            Menu m05 = ensureMenu(session, 14705L, 147L, 14705L,
                    "Peserta Jadwal",
                    "/pages/master/antarjemput/panel_peserta.zul",
                    "fas fa-users", 5, Boolean.FALSE);

            Menu m06 = ensureMenu(session, 14706L, 147L, 14706L,
                    "Kartu Penjemput",
                    "/pages/master/antarjemput/panel_kartu.zul",
                    "fas fa-id-card", 6, Boolean.FALSE);

            Menu m07 = ensureMenu(session, 14707L, 147L, 14707L,
                    "Transaksi",
                    "/pages/master/antarjemput/panel_transaksi.zul",
                    "fas fa-exchange-alt", 7, Boolean.FALSE);

            Menu m08 = ensureMenu(session, 14708L, 147L, 14708L,
                    "Detail Panggilan",
                    "/pages/master/antarjemput/panel_detail.zul",
                    "fas fa-bullhorn", 8, Boolean.FALSE);

            Menu m09 = ensureMenu(session, 14709L, 147L, 14709L,
                    "Log Notifikasi",
                    "/pages/master/antarjemput/panel_log.zul",
                    "fas fa-list-alt", 9, Boolean.FALSE);

            tx.commit();
            tx = null;
            System.out.println("[MenuHelper] Menu Antar Jemput berhasil dipastikan.");

            // ── Attach privileges ke role "am" dalam transaksi terpisah ──
            Session s2 = HibernateUtil.currentNativeSession();
            Transaction tx2 = s2.beginTransaction();
            try {
                String roleId = "am";
                Menu[] allMenus = { root, m01, m02, m03, m04, m05, m06, m07, m08, m09 };
                for (int i = 0; i < allMenus.length; i++) {
                    ensurePrivilege(s2, roleId, allMenus[i]);
                }
                tx2.commit();
                System.out.println("[MenuHelper] Privilege Antar Jemput untuk role 'am' berhasil dipastikan.");
            } catch (Exception e) {
                if (tx2 != null && tx2.isActive()) try { tx2.rollback(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/MenuHelper.java:193");}
                System.err.println("[MenuHelper] Gagal assign privilege Antar Jemput: " + e.getMessage());
                e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/MenuHelper.java:195");
            }

        } catch (Exception e) {
            if (tx != null && tx.isActive()) try { tx.rollback(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/MenuHelper.java:199");}
            System.err.println("[MenuHelper] Gagal seeding menu Antar Jemput: " + e.getMessage());
            e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/MenuHelper.java:201");
        } finally {
            if (session != null) {
                try { session.disconnect(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/MenuHelper.java:204");}
                try { session.close(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/MenuHelper.java:205");}
            }
            HibernateUtil.closeSession();
        }
    }

    /**
     * Memastikan menu admin ZK untuk marketplace kursus ("Ulasan Kursus", "Kupon Kursus", dan
     * "Sertifikat Kursus") terdaftar di bawah grup Produk Kursus yang sudah ada (root = 8902,
     * lihat baris existing "Produk"/"Peserta"/dst di MenuSnapshotData). Idempoten seperti
     * seeder menu lain di kelas ini.
     */
    public static void ensureKursusMarketplaceMenus() {
        Session session = null;
        Transaction tx = null;
        try {
            session = HibernateUtil.currentNativeSession();
            tx = session.beginTransaction();

            Menu mUlasan = ensureMenu(session, 890021101L, 8902L, 8900211L,
                    "Ulasan Kursus",
                    "/pages/master/kursus/ulasan_kursus.zul",
                    "fas fa-star", 11, Boolean.FALSE);

            Menu mKupon = ensureMenu(session, 890021102L, 8902L, 8900212L,
                    "Kupon Kursus",
                    "/pages/master/kursus/kupon_kursus.zul",
                    "fas fa-tags", 12, Boolean.FALSE);

            Menu mSertifikat = ensureMenu(session, 890021103L, 8902L, 8900213L,
                    "Sertifikat Kursus",
                    "/pages/master/kursus/sertifikat_kursus.zul",
                    "fas fa-certificate", 13, Boolean.FALSE);

            tx.commit();
            tx = null;
            System.out.println("[MenuHelper] Menu marketplace Kursus berhasil dipastikan.");

            Session s2 = HibernateUtil.currentNativeSession();
            Transaction tx2 = s2.beginTransaction();
            try {
                String roleId = "am";
                Menu[] allMenus = { mUlasan, mKupon, mSertifikat };
                for (int i = 0; i < allMenus.length; i++) {
                    ensurePrivilege(s2, roleId, allMenus[i]);
                }
                tx2.commit();
                System.out.println("[MenuHelper] Privilege marketplace Kursus untuk role 'am' berhasil dipastikan.");
            } catch (Exception e) {
                if (tx2 != null && tx2.isActive()) try { tx2.rollback(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/MenuHelper.java:kursusPriv"); }
                System.err.println("[MenuHelper] Gagal assign privilege marketplace Kursus: " + e.getMessage());
                e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/MenuHelper.java:kursusPriv2");
            }

        } catch (Exception e) {
            if (tx != null && tx.isActive()) try { tx.rollback(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/MenuHelper.java:kursusRollback"); }
            System.err.println("[MenuHelper] Gagal seeding menu marketplace Kursus: " + e.getMessage());
            e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/MenuHelper.java:kursusCatch");
        } finally {
            if (session != null) {
                try { session.disconnect(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/MenuHelper.java:kursusDisc"); }
                try { session.close(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/MenuHelper.java:kursusClose"); }
            }
            HibernateUtil.closeSession();
        }
    }

    // =========================================================================
    // KANTIN (USAHA KOPERASI) MENU SEEDING
    // =========================================================================

    /**
     * Memastikan menu <b>"PKL Siswa"</b> terdaftar di bawah <i>Sistem Informasi Sekolah &rarr;
     * Pendataan</i> (parent {@code Root = 5701}) dan terpasang ke hak akses role "am". Menu ini
     * membuka halaman pendataan kelompok PKL untuk siswa ({@code /pages/master/sekolah/pkl_siswa.zul}).
     *
     * <p>Metode ini <b>idempoten</b>: memakai id tetap {@code 5701990} sehingga aman dipanggil
     * berulang kali saat startup tanpa menduplikasi baris menu (memakai {@code session.get} lalu
     * {@code saveOrUpdate} di dalam {@link #ensureMenu}). Seperti seeder menu lain, jalur "am" role
     * dilakukan pada transaksi terpisah agar kegagalan pemberian hak akses tidak membatalkan
     * pembuatan menunya. Sesi native dibuka lewat {@code HibernateUtil.currentNativeSession()} dan
     * SELALU ditutup pada blok {@code finally}.</p>
     */
    public static void ensurePklSiswaMenu() {
        Session session = null;
        Transaction tx = null;
        try {
            session = HibernateUtil.currentNativeSession();
            tx = session.beginTransaction();

            Menu m = ensureMenu(session,
                    5701990L, 5701L, 5701990L,
                    "PKL Siswa",
                    "/pages/master/sekolah/pkl_siswa.zul",
                    "fas fa-briefcase", 60, Boolean.FALSE);

            tx.commit();
            tx = null;
            System.out.println("[MenuHelper] Menu PKL Siswa berhasil dipastikan.");

            Session s2 = HibernateUtil.currentNativeSession();
            Transaction tx2 = s2.beginTransaction();
            try {
                ensurePrivilege(s2, "am", m);
                tx2.commit();
                System.out.println("[MenuHelper] Privilege PKL Siswa untuk role 'am' berhasil dipastikan.");
            } catch (Exception e) {
                if (tx2 != null && tx2.isActive()) try { tx2.rollback(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/MenuHelper.java:251");}
                System.err.println("[MenuHelper] Gagal assign privilege PKL Siswa: " + e.getMessage());
                e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/MenuHelper.java:253");
            }
        } catch (Exception e) {
            if (tx != null && tx.isActive()) try { tx.rollback(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/MenuHelper.java:256");}
            System.err.println("[MenuHelper] Gagal seeding menu PKL Siswa: " + e.getMessage());
            e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/MenuHelper.java:258");
        } finally {
            if (session != null) {
                try { session.disconnect(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/MenuHelper.java:261");}
                try { session.close(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/MenuHelper.java:262");}
            }
            HibernateUtil.closeSession();
        }
    }

    /** ID menu "Kios Koperasi" yang sudah ada — acuan audiens role kantin. */
    private static final long KIOS_KOPERASI_MENU_ID = 94230L;
    /** Rentang ID menu kantin ZK yang dikelola otomatis (selaras MENU_KANTIN_ZK_6715.sql). */
    private static final String KANTIN_MENU_IDS =
            "6715501,6715502,6715503,6715504,6715505,6715506,6715507,6715508,"
          + "6715509,6715510,6715511,6715512,6715513,6715514,6715515,6715516,"
          + "6715517,6715518,6715519,6715520,6715521,6715522,6715523,6715524,6715525,6715526,"
          + "6716001,6716002,6716003,6716004"; // grup sub-menu Usaha Koperasi

    /**
     * Memastikan seluruh menu modul Kantin (versi ZK) terdaftar di bawah sub-menu
     * "Usaha Koperasi" (root 6715, sejajar "Kios Koperasi") dan terpasang ke role yang
     * relevan. Induk "Usaha Koperasi" (id 94229) diasumsikan sudah ada.
     *
     * <p>Idempoten: aman dipanggil berulang (tidak menduplikat, lewat get-by-id &amp;
     * NOT EXISTS). Dipanggil otomatis saat startup dari {@code AppStartupListener},
     * sehingga pendaftaran menu tidak perlu SQL manual.</p>
     */
    public static void ensureKantinMenus() {
        Session session = null;
        Transaction tx = null;
        try {
            session = HibernateUtil.currentNativeSession();
            tx = session.beginTransaction();

            final long ROOT = 6715L; // child-code "Usaha Koperasi"
            // Kelompok sub-menu (grup) di dalam "Usaha Koperasi" — url null = grup pengelompok
            // yang dapat dibuka (expandable), leaf-nya diberi root = child-code grup ini.
            final long GRP_TOKO = 671701L, GRP_STOK = 671702L, GRP_PROMO = 671703L, GRP_MEMBER = 671704L;
            Menu[] all = new Menu[40];
            int i = 0;
            // ── Grup fungsional ──────────────────────────────────────────
            all[i++] = ensureMenu(session, 6716001L, ROOT, GRP_TOKO, "Toko / Kantin", null, "fas fa-store", 1,
                    Boolean.FALSE);
            all[i++] = ensureMenu(session, 6716002L, ROOT, GRP_STOK, "Stok & Pengadaan", null, "fas fa-boxes", 2,
                    Boolean.FALSE);
            all[i++] = ensureMenu(session, 6716003L, ROOT, GRP_PROMO, "Promo & Diskon", null, "fas fa-percent", 3,
                    Boolean.FALSE);
            all[i++] = ensureMenu(session, 6716004L, ROOT, GRP_MEMBER, "Layanan Anggota", null, "fas fa-users", 4,
                    Boolean.FALSE);
            // ── Grup: Toko / Kantin ──────────────────────────────────────
            all[i++] = ensureMenu(session, 6715501L, GRP_TOKO, 671550L, "Dasbor Kantin",
                    "/pages/master/koperasi/dashboard_kantin.zul", "fas fa-chart-pie", 1, Boolean.FALSE);
            all[i++] = ensureMenu(session, 6715502L, GRP_TOKO, 671555L, "Kasir (POS)",
                    "/pages/master/koperasi/pos_kantin.zul", "fas fa-cash-register", 2, Boolean.FALSE);
            all[i++] = ensureMenu(session, 6715503L, GRP_TOKO, 671560L, "Pesanan Online",
                    "/pages/master/koperasi/pesanan_kantin.zul", "fas fa-receipt", 3, Boolean.FALSE);
            all[i++] = ensureMenu(session, 6715507L, GRP_TOKO, 671580L, "Meja Kantin",
                    "/pages/master/koperasi/meja_kantin.zul", "fas fa-chair", 4, Boolean.FALSE);
            all[i++] = ensureMenu(session, 6715506L, GRP_TOKO, 671575L, "Pengaturan Kantin",
                    "/pages/master/koperasi/pengaturan_kantin.zul", "fas fa-cogs", 5, Boolean.FALSE);
            all[i++] = ensureMenu(session, 6715521L, GRP_TOKO, 671645L, "Posting HPP Kantin",
                    "/pages/master/koperasi/posting_hpp_kantin.zul", "fas fa-file-invoice-dollar", 6, Boolean.FALSE);
            all[i++] = ensureMenu(session, 6715519L, GRP_TOKO, 671640L, "Ramalan Penjualan",
                    "/pages/master/koperasi/forecast_penjualan.zul", "fas fa-chart-area", 7, Boolean.FALSE);
            all[i++] = ensureMenu(session, 6715526L, GRP_TOKO, 671655L, "Dasbor Kepatuhan Operasional",
                    "/pages/master/koperasi/dashboard_kepatuhan_kantin.zul", "fas fa-user-shield", 8, Boolean.FALSE);
            // ── Grup: Stok & Pengadaan ───────────────────────────────────
            all[i++] = ensureMenu(session, 6715504L, GRP_STOK, 671565L, "Pengadaan Barang",
                    "/pages/master/inventory/pengadaan_kantin.zul", "fas fa-boxes", 1, Boolean.FALSE);
            all[i++] = ensureMenu(session, 6715505L, GRP_STOK, 671570L, "Dasbor Stok & Mutasi",
                    "/pages/master/inventory/dashboard_stok_kantin.zul", "fas fa-warehouse", 2, Boolean.FALSE);
            all[i++] = ensureMenu(session, 6715517L, GRP_STOK, 671630L, "Stok Opname",
                    "/pages/master/inventory/stok_opname.zul", "fas fa-clipboard-check", 3, Boolean.FALSE);
            all[i++] = ensureMenu(session, 6715510L, GRP_STOK, 671595L, "Pengajuan Perubahan Harga",
                    "/pages/master/inventory/pengajuan_perubahan_harga_produk.zul", "fas fa-paper-plane", 4,
                    Boolean.FALSE);
            all[i++] = ensureMenu(session, 6715511L, GRP_STOK, 671600L, "Pedagang / Mitra",
                    "/pages/master/inventory/pedagang_kantin.zul", "fas fa-store", 5, Boolean.FALSE);
            all[i++] = ensureMenu(session, 6715512L, GRP_STOK, 671605L, "Penyedia / Supplier",
                    "/pages/master/inventory/penyedia.zul", "fas fa-truck", 6, Boolean.FALSE);
            all[i++] = ensureMenu(session, 6715513L, GRP_STOK, 671606L, "Survey Penilaian Vendor",
                    "/pages/master/library/survey_vendor.zul", "fas fa-poll", 8, Boolean.FALSE);
            all[i++] = ensureMenu(session, 6715525L, GRP_STOK, 671650L, "Ambang Stok & Pengajuan Pembelian",
                    "/pages/master/inventory/pengajuan_pembelian_gudang.zul", "fas fa-exclamation-triangle", 7,
                    Boolean.FALSE);
            // ── Grup: Promo & Diskon ─────────────────────────────────────
            all[i++] = ensureMenu(session, 6715508L, GRP_PROMO, 671585L, "Aturan Diskon",
                    "/pages/master/koperasi/aturan_diskon.zul", "fas fa-percent", 1, Boolean.FALSE);
            all[i++] = ensureMenu(session, 6715509L, GRP_PROMO, 671590L, "Pencairan Diskon",
                    "/pages/master/koperasi/pencairan_diskon.zul", "fas fa-hand-holding-usd", 2, Boolean.FALSE);
            all[i++] = ensureMenu(session, 6715518L, GRP_PROMO, 671635L, "Monitor Promo & Cashback",
                    "/pages/master/koperasi/monitor_diskon.zul", "fas fa-gift", 3, Boolean.FALSE);
            // ── Grup: Layanan Anggota (member) ───────────────────────────
            all[i++] = ensureMenu(session, 6715513L, GRP_MEMBER, 671610L, "Toko Online (Belanja)",
                    "/pages/master/koperasi/beranda_anggota_kantin.zul", "fas fa-store", 1, Boolean.FALSE);
            all[i++] = ensureMenu(session, 6715514L, GRP_MEMBER, 671615L, "Riwayat Transaksi Saya",
                    "/pages/master/koperasi/riwayat_transaksi_member_kantin.zul", "fas fa-history", 2, Boolean.FALSE);
            all[i++] = ensureMenu(session, 6715515L, GRP_MEMBER, 671620L, "Ringkasan Saya",
                    "/pages/master/koperasi/dashboard_member_kantin.zul", "fas fa-chart-pie", 3, Boolean.FALSE);
            all[i++] = ensureMenu(session, 6715516L, GRP_MEMBER, 671625L, "Notifikasi Saya",
                    "/pages/master/koperasi/notifikasi_member_kantin.zul", "fas fa-bell", 4, Boolean.FALSE);
            all[i++] = ensureMenu(session, 6715520L, GRP_MEMBER, 671628L, "Pesanan Saya",
                    "/pages/master/koperasi/draft_pesanan_member_kantin.zul", "fas fa-clipboard-list", 5, Boolean.FALSE);
            all[i++] = ensureMenu(session, 6715522L, GRP_MEMBER, 671690L, "Konfigurasi Poin Kegiatan",
                    "/pages/master/koperasi/konfigurasi_poin_kegiatan.zul", "fas fa-coins", 6, Boolean.FALSE);
            Menu menuAbsenKajian = ensureMenu(session, 6715523L, GRP_MEMBER, 671691L, "Absen Kajian Saya",
                    "/pages/master/koperasi/kajian_absen_pegawai.zul", "fas fa-user-check", 7, Boolean.FALSE);
            all[i++] = menuAbsenKajian;
            all[i++] = ensureMenu(session, 6715524L, GRP_MEMBER, 671692L, "Proses Poin Bulanan",
                    "/pages/master/koperasi/proses_poin_bulanan.zul", "fas fa-calculator", 8, Boolean.FALSE);

            tx.commit();
            tx = null;
            System.out.println("[MenuHelper] Menu Kantin (16) berhasil dipastikan di root 6715.");
            HibernateUtil.closeSession();

            // ── Hak akses: role yang sudah punya "Kios Koperasi" + role 'Kantin' & 'am' ──
            // FIX "TransactionException: Transaction not successfully started" / "Session is
            // closed!" / "Session was already closed": session ini dipegang lewat
            // currentNativeSession() (POLA B, thread latar AIS-Init-Data) yang bisa self-heal/
            // mengganti session ThreadLocal di antara dua fase transaksi (tx di atas, tx2 di
            // sini, dipisah oleh banyak panggilan ensureMenu di atas) -> variabel session lokal
            // jadi stale relatif thd session aktif saat ini, sehingga tx2 tidak benar-benar
            // terikat ke transaksi yang jalan, dan finally di bawah mencoba menutup session yang
            // sudah tertutup. Ambil ulang referensi session sebelum memulai transaksi kedua,
            // sama seperti pola yang sudah dipakai di ensureSirsDashboardMenus.
            session = HibernateUtil.currentNativeSession();
            Transaction tx2 = session.beginTransaction();
            try {
                // (a) Visibility — job_has_menu (idempoten via NOT EXISTS; guard FK ke tbmrole)
                int ins = session.createSQLQuery(
                        "INSERT INTO public.job_has_menu (job, menu) "
                      + "SELECT r.job, m.id FROM ("
                      + "  SELECT DISTINCT job FROM public.job_has_menu WHERE menu = " + KIOS_KOPERASI_MENU_ID
                      + "  UNION SELECT '" + Tbmrole.KANTIN + "' UNION SELECT '" + Tbmrole.ADMINISTRATOR + "'"
                      + ") r CROSS JOIN public.menu m "
                      + "WHERE m.id IN (" + KANTIN_MENU_IDS + ") "
                      + "  AND EXISTS (SELECT 1 FROM public.tbmrole tr WHERE tr.roleid = r.job) "
                      + "  AND NOT EXISTS (SELECT 1 FROM public.job_has_menu x "
                      + "                  WHERE x.job = r.job AND x.menu = m.id)")
                        .executeUpdate();

                // (b) CRUD privilege — RolePrivilage untuk role yang sama (idempoten)
                List<?> jobs = session.createSQLQuery(
                        "SELECT DISTINCT job FROM public.job_has_menu WHERE menu IN (" + KANTIN_MENU_IDS + ")").list();
                boolean tx2Rusak = false;
                outerJobs:
                for (Object job : jobs) {
                    if (job == null) {
                        continue;
                    }
                    String roleId = String.valueOf(job).trim();
                    for (int k = 0; k < all.length; k++) {
                        if (!ensurePrivilege(session, roleId, all[k])) {
                            // Transaksi sudah rusak (PostgreSQL SQLState 25P02) -- hentikan loop,
                            // jangan terus memanggil query yang pasti gagal jg utk sisa kombinasi.
                            tx2Rusak = true;
                            break outerJobs;
                        }
                    }
                }
                // (c) "Absen Kajian Saya" (6715523) HARUS menjangkau SEMUA pegawai org-wide, bukan hanya
                // role yang sudah punya akses Koperasi/Kantin -- audiensnya beda dari 27 menu Kantin
                // lain di atas. Tidak ada satu job/role tunggal yang pasti dipunyai SETIAP pegawai
                // (role bebas diatur admin per akun, lihat Tbmuser.userrole/user_role2..5), jadi
                // audiensnya dihitung DINAMIS: union job/role dari SEMUA kolom role Tbmuser yang
                // terhubung ke Pegawai (bukan daftar role hardcode) -- otomatis menjangkau guru/dosen
                // (role GURU/DOSEN) maupun staf (role PEGAWAI/lainnya) sekaligus.
                int insAbsenKajian = 0;
                List<?> jobsPegawai = java.util.Collections.emptyList();
                if (!tx2Rusak) {
                    insAbsenKajian = session.createSQLQuery(
                            "INSERT INTO public.job_has_menu (job, menu) "
                          + "SELECT DISTINCT r.job, " + menuAbsenKajian.getId() + " FROM ("
                          + "  SELECT userrole AS job FROM public.tbmuser WHERE pegawai IS NOT NULL AND userrole IS NOT NULL "
                          + "  UNION SELECT user_role2 FROM public.tbmuser WHERE pegawai IS NOT NULL AND user_role2 IS NOT NULL "
                          + "  UNION SELECT user_role3 FROM public.tbmuser WHERE pegawai IS NOT NULL AND user_role3 IS NOT NULL "
                          + "  UNION SELECT user_role4 FROM public.tbmuser WHERE pegawai IS NOT NULL AND user_role4 IS NOT NULL "
                          + "  UNION SELECT user_role5 FROM public.tbmuser WHERE pegawai IS NOT NULL AND user_role5 IS NOT NULL "
                          + ") r "
                          + "WHERE EXISTS (SELECT 1 FROM public.tbmrole tr WHERE tr.roleid = r.job) "
                          + "  AND NOT EXISTS (SELECT 1 FROM public.job_has_menu x "
                          + "                  WHERE x.job = r.job AND x.menu = " + menuAbsenKajian.getId() + ")")
                            .executeUpdate();
                    jobsPegawai = session.createSQLQuery(
                            "SELECT DISTINCT job FROM public.job_has_menu WHERE menu = " + menuAbsenKajian.getId()).list();
                    outerJobsPegawai:
                    for (Object job : jobsPegawai) {
                        if (job == null) {
                            continue;
                        }
                        if (!ensurePrivilege(session, String.valueOf(job).trim(), menuAbsenKajian)) {
                            tx2Rusak = true;
                            break outerJobsPegawai;
                        }
                    }
                }

                if (tx2Rusak || tx2 == null || !tx2.isActive()) {
                    if (tx2 != null && tx2.isActive()) try { tx2.rollback(); } catch (Exception ignored) { }
                    System.err.println("[MenuHelper] Hak akses menu Kantin dilewati sementara karena transaksi sudah tidak aktif/rusak; akan dicoba lagi pada startup berikutnya.");
                    return;
                }

                tx2.commit();
                System.out.println("[MenuHelper] Hak akses menu Kantin dipastikan (job_has_menu +"
                        + ins + " baris, privilege " + jobs.size() + " role; Absen Kajian Saya +"
                        + insAbsenKajian + " baris, " + jobsPegawai.size() + " role pegawai).");
            } catch (Exception e) {
                if (tx2 != null && tx2.isActive()) try { tx2.rollback(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/MenuHelper.java:392");}
                System.err.println("[MenuHelper] Gagal assign hak akses menu Kantin: " + e.getMessage());
                e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/MenuHelper.java:394");
            }

        } catch (Exception e) {
            if (tx != null && tx.isActive()) try { tx.rollback(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/MenuHelper.java:398");}
            System.err.println("[MenuHelper] Gagal seeding menu Kantin: " + e.getMessage());
            e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/MenuHelper.java:400");
        } finally {
            // FIX: setelah tx2.commit() gagal (mis. transaksi PG sudah "aborted" akibat
            // exception SQL di tengah loop ensurePrivilege), Hibernate/JDBC bisa saja sudah
            // menutup sendiri Session ini akibat fatal JDBCException. Panggil disconnect/close
            // hanya bila session masih benar-benar terbuka agar tidak memicu
            // SessionException("Session is closed!"/"already closed") tambahan yang cuma noise.
            if (session != null && session.isOpen()) {
                try { session.disconnect(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/MenuHelper.java:403");}
                try { session.close(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/MenuHelper.java:404");}
            }
            HibernateUtil.closeSession();
        }
    }

    /**
     * Memastikan menu <b>Dasbor Simpan Pinjam</b> terdaftar dan dapat diakses.
     *
     * <p>
     * Menu ditempatkan sebagai saudara (sibling) menu Kantin di bawah root {@code 6715}
     * ("Usaha Koperasi"), menunjuk ZUL {@code /pages/master/koperasi/dashboard_simpan_pinjam.zul}.
     * Metode ini <b>idempoten</b>: aman dipanggil setiap kali aplikasi start — menu dibuat bila
     * belum ada, atau diperbarui bila sudah ada, tanpa menduplikasi.
     * </p>
     *
     * <p>
     * Hak akses (visibilitas + CRUD) diberikan ke role yang sama dengan modul koperasi lain: role
     * mana pun yang sudah punya menu "Kios Koperasi" ({@link #KIOS_KOPERASI_MENU_ID}), ditambah role
     * {@link Tbmrole#KANTIN} dan {@link Tbmrole#ADMINISTRATOR}. Penyisipan {@code job_has_menu}
     * memakai {@code NOT EXISTS} sehingga tidak menghasilkan baris ganda, dan {@code RolePrivilage}
     * dipastikan lewat {@link #ensurePrivilege(Session, String, Menu)} yang juga idempoten. Session
     * {@code currentNativeSession()} ditutup di blok {@code finally}.
     * </p>
     */
    public static void ensureSimpanPinjamMenus() {
        Session session = null;
        Transaction tx = null;
        try {
            session = HibernateUtil.currentNativeSession();
            tx = session.beginTransaction();

            final long ROOT = 6715L; // child-code "Usaha Koperasi" (sama dengan menu Kantin)
            // Best practice: pisahkan Unit Simpan Pinjam menjadi dua kelompok fungsional —
            // (1) OPERASIONAL simpan pinjam, (2) KEUANGAN & PELAPORAN koperasi — agar tiap flyout
            // ringkas dan sesuai cara kerja pengurus (pelayanan anggota vs tata kelola keuangan).
            final long GRP_USP = 671705L; // grup "Simpan Pinjam" (operasional USP)
            final long GRP_KEU = 671706L; // grup "Keuangan & Pelaporan" (laporan keuangan, SHU, anggaran, modal)
            Menu[] all = new Menu[9];
            int i = 0;
            // ── Grup: Simpan Pinjam (operasional pelayanan anggota) ──────────
            all[i++] = ensureMenu(session, 6716005L, ROOT, GRP_USP, "Simpan Pinjam", null,
                    "fas fa-hand-holding-usd", 5, Boolean.FALSE);
            all[i++] = ensureMenu(session, 6715530L, GRP_USP, 671650L, "Dasbor Simpan Pinjam",
                    "/pages/master/koperasi/dashboard_simpan_pinjam.zul", "fas fa-hand-holding-usd", 4, Boolean.FALSE);
            all[i++] = ensureMenu(session, 6715531L, GRP_USP, 671655L, "Laporan Simpan Pinjam",
                    "/pages/master/koperasi/laporan_simpan_pinjam.zul", "fas fa-book", 5, Boolean.FALSE);
            all[i++] = ensureMenu(session, 6715536L, GRP_USP, 671680L, "ARO Deposito",
                    "/pages/master/koperasi/deposito_aro_koperasi.zul", "fas fa-sync-alt", 6, Boolean.FALSE);
            // ── Grup: Keuangan & Pelaporan (tata kelola keuangan koperasi) ───
            all[i++] = ensureMenu(session, 6716006L, ROOT, GRP_KEU, "Keuangan & Pelaporan", null,
                    "fas fa-file-invoice-dollar", 6, Boolean.FALSE);
            all[i++] = ensureMenu(session, 6715533L, GRP_KEU, 671665L, "Laporan Keuangan Koperasi",
                    "/pages/master/koperasi/laporan_keuangan_koperasi.zul", "fas fa-file-invoice-dollar", 1,
                    Boolean.FALSE);
            all[i++] = ensureMenu(session, 6715532L, GRP_KEU, 671660L, "Pembagian SHU",
                    "/pages/master/koperasi/pembagian_shu.zul", "fas fa-coins", 2, Boolean.FALSE);
            all[i++] = ensureMenu(session, 6715534L, GRP_KEU, 671670L, "Anggaran Kas (RAPB)",
                    "/pages/master/koperasi/anggaran_kas_koperasi.zul", "fas fa-wallet", 3, Boolean.FALSE);
            all[i++] = ensureMenu(session, 6715535L, GRP_KEU, 671675L, "Modal Penyertaan",
                    "/pages/master/koperasi/modal_penyertaan_koperasi.zul", "fas fa-money-check-alt", 4, Boolean.FALSE);

            // Perbaiki tautan menu "Pembayaran Angsuran Koperasi" (id 36751, grup Pendataan) yang keliru
            // mengarah ke halaman pembayaran online/POS (pem_online.zul = PembayaranKoperasiOnline).
            // Seharusnya ke halaman pelunasan angsuran (pembayaran_anggota_koperasi.zul =
            // PembayaranAnggotaKoperasiAction). Idempoten: hanya diubah bila masih menunjuk pem_online.
            try {
                int fixUrl = session.createSQLQuery(
                        "UPDATE public.menu SET url = '/pages/master/koperasi/pembayaran_anggota_koperasi.zul' "
                      + "WHERE id = 36751 AND (url IS NULL OR url LIKE '%pem_online%')").executeUpdate();
                if (fixUrl > 0) {
                    System.out.println("[MenuHelper] Tautan 'Pembayaran Angsuran Koperasi' diperbaiki ke "
                            + "pembayaran_anggota_koperasi.zul (dari pem_online).");
                }
            } catch (Exception e) {
                System.err.println("[MenuHelper] Gagal memperbaiki tautan Pembayaran Angsuran Koperasi: "
                        + e.getMessage());
            }

            // ── Konsolidasi fungsi OPERASIONAL simpan pinjam ke unit "Simpan Pinjam" (best practice):
            //    pindahkan (reparent) menu Transaksi Koperasi, Persetujuan Transaksi, dan Pelunasan
            //    Angsuran dari grup "Pendataan" ke grup "Simpan Pinjam" (root 671705), urutan 1-3 (di
            //    atas dasbor/laporan/ARO). Idempoten & berbasis URL (bila baris tak ada → no-op).
            //    Dijalankan SETELAH perbaikan URL 36751 agar HQL membaca URL yang sudah benar.
            int pindah = 0;
            pindah += reparentMenuByUrl(session, "%/koperasi/transaksi_koperasi.zul", GRP_USP, 1);
            pindah += reparentMenuByUrl(session, "%/koperasi/persetujuan_transaksi_koperasi.zul", GRP_USP, 2);
            pindah += reparentMenuByUrl(session, "%/koperasi/pembayaran_anggota_koperasi.zul", GRP_USP, 3);
            if (pindah > 0) {
                System.out.println("[MenuHelper] " + pindah + " menu operasional simpan pinjam dipindah ke unit "
                        + "'Simpan Pinjam'.");
            }

            tx.commit();
            tx = null;
            System.out.println("[MenuHelper] Menu Simpan Pinjam berhasil dipastikan di root 6715.");

            final String SP_MENU_IDS = "6715530,6715531,6715532,6715533,6715534,6715535,6715536,6716005,6716006";

            // ── Hak akses: role yang sudah punya "Kios Koperasi" + role 'Kantin' & 'am' ──
            // FIX "TransactionException: Transaction not successfully started" / "Session is
            // closed!" / "Session was already closed": session ini dipegang lewat
            // currentNativeSession() (POLA B, thread latar AIS-Init-Data) yang bisa self-heal/
            // mengganti session ThreadLocal di antara dua fase transaksi (tx di atas, tx2 di
            // sini, dipisah oleh banyak panggilan ensureMenu di atas) -> variabel session lokal
            // jadi stale relatif thd session aktif saat ini, sehingga tx2 tidak benar-benar
            // terikat ke transaksi yang jalan, dan finally di bawah mencoba menutup session yang
            // sudah tertutup. Ambil ulang referensi session sebelum memulai transaksi kedua,
            // sama seperti pola yang sudah dipakai di ensureSirsDashboardMenus.
            session = HibernateUtil.currentNativeSession();
            Transaction tx2 = session.beginTransaction();
            try {
                // (a) Visibility — job_has_menu (idempoten via NOT EXISTS; guard FK ke tbmrole)
                int ins = session.createSQLQuery(
                        "INSERT INTO public.job_has_menu (job, menu) "
                      + "SELECT r.job, m.id FROM ("
                      + "  SELECT DISTINCT job FROM public.job_has_menu WHERE menu = " + KIOS_KOPERASI_MENU_ID
                      + "  UNION SELECT '" + Tbmrole.KANTIN + "' UNION SELECT '" + Tbmrole.ADMINISTRATOR + "'"
                      + ") r CROSS JOIN public.menu m "
                      + "WHERE m.id IN (" + SP_MENU_IDS + ") "
                      + "  AND EXISTS (SELECT 1 FROM public.tbmrole tr WHERE tr.roleid = r.job) "
                      + "  AND NOT EXISTS (SELECT 1 FROM public.job_has_menu x "
                      + "                  WHERE x.job = r.job AND x.menu = m.id)")
                        .executeUpdate();

                // (a2) JAGA AKSES item yang DIPINDAH: setiap role yang memegang menu operasional
                //      (transaksi/persetujuan/pelunasan) diberi juga rantai leluhur barunya —
                //      grup "Simpan Pinjam" (child 671705), "Usaha Koperasi" (child 6715), dan
                //      "Sistem Informasi Koperasi" (child 67) — agar item tetap tampil setelah
                //      pindah induk (tree hanya dirender bila seluruh leluhur ikut ter-grant).
                int insLeluhur = session.createSQLQuery(
                        "INSERT INTO public.job_has_menu (job, menu) "
                      + "SELECT DISTINCT src.job, anc.id "
                      + "FROM public.job_has_menu src "
                      + "JOIN public.menu srcm ON srcm.id = src.menu "
                      + "CROSS JOIN public.menu anc "
                      + "WHERE (srcm.url LIKE '%/koperasi/transaksi_koperasi.zul' "
                      + "    OR srcm.url LIKE '%/koperasi/persetujuan_transaksi_koperasi.zul' "
                      + "    OR srcm.url LIKE '%/koperasi/pembayaran_anggota_koperasi.zul') "
                      + "  AND anc.child IN (671705, 6715, 67) "
                      + "  AND EXISTS (SELECT 1 FROM public.tbmrole tr WHERE tr.roleid = src.job) "
                      + "  AND NOT EXISTS (SELECT 1 FROM public.job_has_menu x "
                      + "                  WHERE x.job = src.job AND x.menu = anc.id)")
                        .executeUpdate();
                if (insLeluhur > 0) {
                    System.out.println("[MenuHelper] Grant leluhur (Simpan Pinjam/Usaha Koperasi/SI Koperasi) "
                            + "ditambah untuk menjaga akses menu operasional yang dipindah: " + insLeluhur + " baris.");
                }

                // (a3) Pastikan AUDIENS DASAR (Kios Koperasi ∪ Kantin ∪ Administrator) juga memiliki node
                //      leluhur "Usaha Koperasi" (child 6715) dan "Sistem Informasi Koperasi" (child 67).
                //      Tanpa ini, renderer berbasis child-code tidak menampilkan grup baru bagi role yang
                //      punya grup tapi belum punya rantai leluhurnya (mis. role Kantin tanpa Kios). Idempoten.
                int insLeluhurDasar = session.createSQLQuery(
                        "INSERT INTO public.job_has_menu (job, menu) "
                      + "SELECT r.job, anc.id FROM ("
                      + "  SELECT DISTINCT job FROM public.job_has_menu WHERE menu = " + KIOS_KOPERASI_MENU_ID
                      + "  UNION SELECT '" + Tbmrole.KANTIN + "' UNION SELECT '" + Tbmrole.ADMINISTRATOR + "'"
                      + ") r CROSS JOIN public.menu anc "
                      + "WHERE anc.child IN (6715, 67) "
                      + "  AND EXISTS (SELECT 1 FROM public.tbmrole tr WHERE tr.roleid = r.job) "
                      + "  AND NOT EXISTS (SELECT 1 FROM public.job_has_menu x "
                      + "                  WHERE x.job = r.job AND x.menu = anc.id)")
                        .executeUpdate();
                if (insLeluhurDasar > 0) {
                    System.out.println("[MenuHelper] Grant leluhur Usaha Koperasi/SI Koperasi untuk audiens dasar: "
                            + insLeluhurDasar + " baris.");
                }

                // (b) CRUD privilege — RolePrivilage untuk role yang sama (idempoten)
                List<?> jobs = session.createSQLQuery(
                        "SELECT DISTINCT job FROM public.job_has_menu WHERE menu IN (" + SP_MENU_IDS + ")").list();
                for (Object job : jobs) {
                    if (job == null) {
                        continue;
                    }
                    String roleId = String.valueOf(job).trim();
                    for (int k = 0; k < all.length; k++) {
                        ensurePrivilege(session, roleId, all[k]);
                    }
                }
                tx2.commit();
                System.out.println("[MenuHelper] Hak akses menu Simpan Pinjam dipastikan (job_has_menu +"
                        + ins + " baris, privilege " + jobs.size() + " role).");
            } catch (Exception e) {
                if (tx2 != null && tx2.isActive()) try { tx2.rollback(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/MenuHelper.java:579");}
                System.err.println("[MenuHelper] Gagal assign hak akses menu Simpan Pinjam: " + e.getMessage());
                e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/MenuHelper.java:581");
            }

        } catch (Exception e) {
            if (tx != null && tx.isActive()) try { tx.rollback(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/MenuHelper.java:585");}
            System.err.println("[MenuHelper] Gagal seeding menu Simpan Pinjam: " + e.getMessage());
            e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/MenuHelper.java:587");
        } finally {
            if (session != null) {
                try { session.disconnect(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/MenuHelper.java:590");}
                try { session.close(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/MenuHelper.java:591");}
            }
            HibernateUtil.closeSession();
        }
    }

    // =========================================================================
    // REPOSITORY (DIGITAL REPOSITORY / DSPACE / TURNITIN) MENU SEEDING
    // =========================================================================

    /** Rentang ID menu Repository (versi baru) yang dikelola otomatis. */
    private static final String REPOSITORY_MENU_IDS = "160,16001,16002,16003,16004";

    /**
     * Memastikan modul <b>Repository Digital</b> terdaftar di menu utama dengan struktur
     * best-practice: satu induk ("Repository") berisi sub-menu fungsional yang masing-masing
     * <i>deep-link</i> langsung ke tab konsol repository ({@code /pages/master/repository.zul}) —
     * Dasbor, Item, Koleksi, dan Monitoring. Menggantikan menu Repository lama (id 158/15801–15805)
     * yang sudah dibersihkan oleh {@link #cleanupObsoleteMenus()}.
     *
     * <p>Idempoten: aman dipanggil berulang saat startup — memakai id tetap (get-by-id lalu
     * saveOrUpdate) sehingga tidak menduplikasi baris. Hak akses diberikan ke role
     * {@link Tbmrole#ADMINISTRATOR} dan setiap role yang flag {@code repository}-nya aktif,
     * mencakup dua sisi: <b>visibilitas</b> ({@code job_has_menu}, join-table {@code Tbmrole.getMenus()})
     * dan <b>CRUD</b> ({@link RolePrivilage} via {@link #ensurePrivilege}). Seperti seeder lain,
     * jalur hak akses dijalankan pada transaksi terpisah agar kegagalannya tidak membatalkan
     * pembuatan menu, dan session native SELALU ditutup di blok {@code finally}.</p>
     */
    public static void ensureRepositoryMenus() {
        Session session = null;
        Transaction tx = null;
        try {
            session = HibernateUtil.currentNativeSession();
            tx = session.beginTransaction();

            final long ROOT = 160L; // induk "Repository" (root=0, sejajar modul lain)
            Menu[] all = new Menu[5];
            int i = 0;
            // ── Induk modul (url null = grup pengelompok yang dapat dibuka) ──
            all[i++] = ensureMenu(session, 160L, 0L, 160L, "Repository", null, "fas fa-archive", 12, Boolean.FALSE);
            // ── Sub-menu fungsional: 1 : 1 dengan tab konsol repository ──────
            all[i++] = ensureMenu(session, 16001L, ROOT, 16001L, "Dasbor Repository",
                    "/pages/master/repository.zul?tab=dashboard", "fas fa-chart-pie", 1, Boolean.FALSE);
            all[i++] = ensureMenu(session, 16002L, ROOT, 16002L, "Item Repository",
                    "/pages/master/repository.zul?tab=item", "fas fa-file-alt", 2, Boolean.FALSE);
            all[i++] = ensureMenu(session, 16003L, ROOT, 16003L, "Koleksi Repository",
                    "/pages/master/repository.zul?tab=collection", "fas fa-layer-group", 3, Boolean.FALSE);
            all[i++] = ensureMenu(session, 16004L, ROOT, 16004L, "Monitoring Repository",
                    "/pages/master/repository.zul?tab=monitoring", "fas fa-heartbeat", 4, Boolean.FALSE);

            tx.commit();
            tx = null;
            System.out.println("[MenuHelper] Menu Repository berhasil dipastikan di root 160.");

            // ── Hak akses: role 'am' (Administrator) + role dengan flag repository=true ──
            Transaction tx2 = session.beginTransaction();
            try {
                // (a) Visibility — job_has_menu (idempoten via NOT EXISTS; guard FK ke tbmrole)
                int ins = session.createSQLQuery(
                        "INSERT INTO public.job_has_menu (job, menu) "
                      + "SELECT r.roleid, m.id FROM public.tbmrole r "
                      + "CROSS JOIN public.menu m "
                      + "WHERE m.id IN (" + REPOSITORY_MENU_IDS + ") "
                      + "  AND (r.roleid = '" + Tbmrole.ADMINISTRATOR + "' OR r.repository = true) "
                      + "  AND NOT EXISTS (SELECT 1 FROM public.job_has_menu x "
                      + "                  WHERE x.job = r.roleid AND x.menu = m.id)")
                        .executeUpdate();

                // (b) CRUD privilege — RolePrivilage untuk role yang sama (idempoten)
                List<?> jobs = session.createSQLQuery(
                        "SELECT DISTINCT job FROM public.job_has_menu WHERE menu IN (" + REPOSITORY_MENU_IDS + ")").list();
                for (Object job : jobs) {
                    if (job == null) {
                        continue;
                    }
                    String roleId = String.valueOf(job).trim();
                    for (int k = 0; k < all.length; k++) {
                        ensurePrivilege(session, roleId, all[k]);
                    }
                }
                tx2.commit();
                System.out.println("[MenuHelper] Hak akses menu Repository dipastikan (job_has_menu +"
                        + ins + " baris, privilege " + jobs.size() + " role).");
            } catch (Exception e) {
                if (tx2 != null && tx2.isActive()) try { tx2.rollback(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/MenuHelper.java:675");}
                System.err.println("[MenuHelper] Gagal assign hak akses menu Repository: " + e.getMessage());
                e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/MenuHelper.java:677");
            }

        } catch (Exception e) {
            if (tx != null && tx.isActive()) try { tx.rollback(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/MenuHelper.java:681");}
            System.err.println("[MenuHelper] Gagal seeding menu Repository: " + e.getMessage());
            e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/MenuHelper.java:683");
        } finally {
            if (session != null) {
                try { session.disconnect(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/MenuHelper.java:686");}
                try { session.close(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/MenuHelper.java:687");}
            }
            HibernateUtil.closeSession();
        }
    }

    // =========================================================================
    // SIRS (RS/KLINIK) — DASBOR MENU SEEDING
    // =========================================================================

    /** Id menu: 1 grup "Dasbor" (1700000) + 7 sub-dasbor (1700001-1700007) di bawah modul SIRS. */
    private static final String SIRS_DASHBOARD_MENU_IDS =
            "1700000,1700001,1700002,1700003,1700004,1700005,1700006,1700007";

    /** Id menu induk modul SIRS bawaan: "Sistem Informasi Rumah Sakit". */
    private static final long SIRS_PARENT_MENU_ID = 77754677L;
    /** Child-code modul induk SIRS (root untuk sub-grup di bawahnya). */
    private static final long SIRS_PARENT_CHILD_CODE = 7770L;
    /** Child-code grup "Dasbor" yang baru (root untuk tiap dasbor). Bebas tabrakan (dicek dari data). */
    private static final long SIRS_DASHBOARD_GROUP_CHILD = 777090L;

    /**
     * Memastikan grup menu <b>Dasbor</b> terdaftar <u>di dalam modul rumah sakit yang sudah ada</u>
     * — "Sistem Informasi Rumah Sakit" (id {@value #SIRS_PARENT_MENU_ID}, child-code
     * {@value #SIRS_PARENT_CHILD_CODE}) — berisi tujuh dasbor pemantauan yang masing-masing
     * <i>deep-link</i> langsung ke halaman dasbor SIRS berbasis grafik HTML/CSS (tanpa JFreeChart):
     * Ringkasan Pendaftaran, Kunjungan Rawat Jalan (mingguan &amp; bulanan), Pendapatan Pasien,
     * Okupansi Tempat Tidur, 10 Diagnosa Terbanyak, dan Kewaspadaan Kadaluarsa Farmasi.
     *
     * <p><b>Untuk apa (bahasa awam).</b> Menambahkan sub-menu "Dasbor" di modul Rumah Sakit yang
     * sudah dikenal pengguna, supaya manajemen &amp; petugas bisa membuka dasbor langsung dari sana
     * tanpa harus tahu alamat halaman. Semua dasbor hanya-baca (memantau), jadi aman diberikan luas.</p>
     *
     * <p><b>Penempatan (parenting by child-code).</b> Di AIS, baris anak menaruh {@code root} =
     * <i>child-code</i> induknya. Karena modul SIRS ber-child-code {@value #SIRS_PARENT_CHILD_CODE},
     * grup "Dasbor" dibuat dengan {@code root = }{@value #SIRS_PARENT_CHILD_CODE} dan child-code baru
     * {@value #SIRS_DASHBOARD_GROUP_CHILD}; ketujuh dasbor lalu memakai {@code root =
     * }{@value #SIRS_DASHBOARD_GROUP_CHILD}. Dengan begitu "Dasbor" muncul sebagai sub-grup di dalam
     * "Sistem Informasi Rumah Sakit", sejajar dengan Admisi/Poliklinik/Apotik/Kasir/dll.</p>
     *
     * <p><b>Idempoten.</b> Aman dipanggil berulang saat startup — memakai id tetap (get-by-id lalu
     * saveOrUpdate) sehingga tidak menduplikasi baris. Id memakai rentang 7-digit (1700000+) agar tidak
     * bertabrakan dengan id acak 6-digit bawaan; child-code {@value #SIRS_DASHBOARD_GROUP_CHILD}
     * dipastikan belum terpakai.</p>
     *
     * <p><b>Hak akses.</b> Diberikan ke role {@link Tbmrole#ADMINISTRATOR} <i>dan</i> setiap role yang
     * SUDAH memakai modul SIRS — yakni yang memiliki menu induk SIRS ({@value #SIRS_PARENT_MENU_ID})
     * atau menu ber-url {@code /pages/master/sirs/%} — jadi siapa pun yang kini dapat memakai modul
     * RS/Klinik otomatis melihat dasbor barunya, tanpa perlu kolom-flag khusus pada {@code tbmrole}.
     * Mencakup dua sisi: <b>visibilitas</b> ({@code job_has_menu}) dan <b>CRUD</b>
     * ({@link RolePrivilage} via {@link #ensurePrivilege}). Seperti seeder lain, jalur hak akses
     * berjalan pada transaksi terpisah agar kegagalannya tidak membatalkan pembuatan menu, dan session
     * native SELALU ditutup pada blok {@code finally}.</p>
     */
    public static void ensureSirsDashboardMenus() {
        Session session = null;
        Transaction tx = null;
        try {
            session = HibernateUtil.currentNativeSession();
            tx = session.beginTransaction();

            // Grup "Dasbor" DIPASANG DI BAWAH modul SIRS: root = child-code induk SIRS (7770).
            // Tiap dasbor: root = child-code grup "Dasbor" (777090).
            final long GRUP = SIRS_DASHBOARD_GROUP_CHILD;
            Menu[] all = new Menu[8];
            int i = 0;
            // ── Grup "Dasbor" (url null = pengelompok) di dalam modul SIRS ──
            all[i++] = ensureMenu(session, 1700000L, SIRS_PARENT_CHILD_CODE, GRUP, "Dasbor", null,
                    "fas fa-chart-pie", 9, Boolean.FALSE);
            // ── Sub-dasbor (root = child-code grup Dasbor) : 1 : 1 dengan halaman dasbor SIRS ──
            all[i++] = ensureMenu(session, 1700001L, GRUP, 1700001L, "Ringkasan Pendaftaran",
                    "/pages/master/sirs/dashboard_pendaftaran_overview.zul", "fas fa-clipboard-list", 1, Boolean.FALSE);
            all[i++] = ensureMenu(session, 1700002L, GRUP, 1700002L, "Kunjungan Rawat Jalan (Mingguan)",
                    "/pages/master/sirs/dashboard_rawat_jalan_mingguan.zul", "fas fa-calendar-week", 2, Boolean.FALSE);
            all[i++] = ensureMenu(session, 1700003L, GRUP, 1700003L, "Kunjungan Rawat Jalan (Bulanan)",
                    "/pages/master/sirs/dashboard_rawat_jalan_bulanan.zul", "fas fa-calendar-alt", 3, Boolean.FALSE);
            all[i++] = ensureMenu(session, 1700004L, GRUP, 1700004L, "Pendapatan Pasien",
                    "/pages/master/sirs/dashboard_pendapatan.zul", "fas fa-money-bill-wave", 4, Boolean.FALSE);
            all[i++] = ensureMenu(session, 1700005L, GRUP, 1700005L, "Okupansi Tempat Tidur",
                    "/pages/master/sirs/dashboard_okupansi_tempat_tidur.zul", "fas fa-bed", 5, Boolean.FALSE);
            all[i++] = ensureMenu(session, 1700006L, GRUP, 1700006L, "10 Diagnosa Terbanyak",
                    "/pages/master/sirs/dashboard_diagnosa_terbanyak.zul", "fas fa-notes-medical", 6, Boolean.FALSE);
            all[i++] = ensureMenu(session, 1700007L, GRUP, 1700007L, "Kewaspadaan Kadaluarsa (Farmasi)",
                    "/pages/master/sirs/dashboard_kadaluarsa_farmasi.zul", "fas fa-pills", 7, Boolean.FALSE);

            tx.commit();
            tx = null;
            System.out.println("[MenuHelper] Menu 'Dasbor' berhasil dipastikan di dalam modul SIRS "
                    + "(id " + SIRS_PARENT_MENU_ID + ", child-code " + SIRS_PARENT_CHILD_CODE + ").");
            HibernateUtil.closeSession();

            // ── Hak akses: role 'am' (Administrator) + role yang SUDAH memakai halaman SIRS ──
            // FIX "TransactionException: Transaction not successfully started" pada tx2.commit():
            // session ini dipegang lewat currentNativeSession() (POLA B, thread latar AIS-Init-Data)
            // yang bisa self-heal/mengganti session ThreadLocal di antara dua fase transaksi (tx di
            // atas, tx2 di sini) -> variabel session lokal jadi stale relatif thd session aktif saat
            // ini, sehingga tx2 tidak benar-benar terikat ke transaksi yang jalan. Ambil ulang
            // referensi session sebelum memulai transaksi kedua agar tidak memakai referensi basi.
            session = HibernateUtil.currentNativeSession();
            Transaction tx2 = session.beginTransaction();
            try {
                // (a) Visibility — job_has_menu (idempoten via NOT EXISTS; guard FK ke tbmrole).
                //     Target: role 'am' ATAU role yang SUDAH memakai modul SIRS — punya menu induk
                //     SIRS (77754677) ATAU menu ber-url '/pages/master/sirs/%'.
                int ins = session.createSQLQuery(
                        "INSERT INTO public.job_has_menu (job, menu) "
                      + "SELECT r.roleid, m.id FROM public.tbmrole r "
                      + "CROSS JOIN public.menu m "
                      + "WHERE m.id IN (" + SIRS_DASHBOARD_MENU_IDS + ") "
                      + "  AND (r.roleid = '" + Tbmrole.ADMINISTRATOR + "' "
                      + "       OR EXISTS (SELECT 1 FROM public.job_has_menu jp "
                      + "                  WHERE jp.job = r.roleid AND jp.menu = " + SIRS_PARENT_MENU_ID + ") "
                      + "       OR EXISTS (SELECT 1 FROM public.job_has_menu jm JOIN public.menu mm ON jm.menu = mm.id "
                      + "                  WHERE jm.job = r.roleid AND mm.url LIKE '/pages/master/sirs/%')) "
                      + "  AND NOT EXISTS (SELECT 1 FROM public.job_has_menu x "
                      + "                  WHERE x.job = r.roleid AND x.menu = m.id)")
                        .executeUpdate();

                // (b) CRUD privilege — RolePrivilage untuk role yang sama (idempoten)
                List<?> jobs = session.createSQLQuery(
                        "SELECT DISTINCT job FROM public.job_has_menu WHERE menu IN (" + SIRS_DASHBOARD_MENU_IDS + ")").list();
                boolean tx2Rusak = false;
                outerJobs:
                for (Object job : jobs) {
                    if (job == null) {
                        continue;
                    }
                    String roleId = String.valueOf(job).trim();
                    for (int k = 0; k < all.length; k++) {
                        if (!ensurePrivilege(session, roleId, all[k])) {
                            tx2Rusak = true;
                            break outerJobs;
                        }
                    }
                }
                if (tx2Rusak || tx2 == null || !tx2.isActive()) {
                    if (tx2 != null && tx2.isActive()) try { tx2.rollback(); } catch (Exception ignored) { }
                    System.err.println("[MenuHelper] Hak akses menu Dasbor RS/Klinik dilewati sementara karena transaksi sudah tidak aktif/rusak; akan dicoba lagi pada startup berikutnya.");
                    return;
                }
                tx2.commit();
                System.out.println("[MenuHelper] Hak akses menu Dasbor RS/Klinik dipastikan (job_has_menu +"
                        + ins + " baris, privilege " + jobs.size() + " role).");
            } catch (Exception e) {
                if (tx2 != null && tx2.isActive()) try { tx2.rollback(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/MenuHelper.java:813");}
                System.err.println("[MenuHelper] Gagal assign hak akses menu Dasbor RS/Klinik: " + e.getMessage());
                e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/MenuHelper.java:815");
            }

        } catch (Exception e) {
            if (tx != null && tx.isActive()) try { tx.rollback(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/MenuHelper.java:819");}
            System.err.println("[MenuHelper] Gagal seeding menu Dasbor RS/Klinik: " + e.getMessage());
            e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/MenuHelper.java:821");
        } finally {
            // Session ini berasal dari HibernateUtil.currentNativeSession() (POLA B, thread latar
            // AppStartupListener), jadi penutupannya HARUS lewat HibernateUtil.closeSession() SAJA
            // — method itu sudah mengeluarkan session dari ThreadLocal lalu memanggil
            // closeSessionQuietly(), yang menjaga session.isOpen() sebelum clear/disconnect/close.
            // JANGAN memanggil session.disconnect()/session.close() manual lagi di sini: bila
            // transaksi di atas gagal (mis. TransactionException) sesi bisa sudah rusak/tertutup,
            // dan pemanggilan manual tanpa guard isOpen() akan memicu SessionException berantai
            // ("Session is closed!" lalu "Session was already closed") yang hanya menutupi root
            // cause asli — yang sudah dicatat oleh ErrorAuditUtil.record() pada blok catch di atas.
            HibernateUtil.closeSession();
        }
    }

    // =========================================================================
    // DOKTER (RS/KLINIK) — ROLE + MENU KHUSUS
    // =========================================================================

    /** Id menu khusus Dokter: 1 grup (1710000) + 4 halaman inti. */
    private static final String DOKTER_MENU_IDS = "1710000,1710001,1710002,1710003,1710004";

    /**
     * Memastikan <b>hak akses (role) "Dokter"</b> beserta <b>menu khusus</b>-nya tersedia. Role Dokter
     * mewakili tenaga medis RS/Klinik — mencakup kategori Dokter, Perawat, dan Bidan yang berada pada
     * satu data {@code sirs.dokter} (lihat {@link Tbmrole#DOKTER} dan relasi {@code Tbmuser.getDokter()}).
     *
     * <p><b>Untuk apa (bahasa awam).</b> Menyediakan satu peran login khusus dokter/perawat/bidan dengan
     * daftar menu yang ringkas dan relevan — Rekam Medis (rawat jalan &amp; rawat inap), Data Pasien, dan
     * Dasbor eMedic — sehingga tenaga medis tidak perlu menyusuri seluruh menu rumah sakit.</p>
     *
     * <p><b>Yang dilakukan.</b> (1) Membuat baris {@code tbmrole} "Dokter" bila belum ada (pola sama
     * seperti seeding role lain di {@code InitDataHelper}). (2) Membuat satu grup menu top-level "Dokter"
     * (root=0, child-code 1710090, id 1710000) berisi empat halaman SIRS inti. (3) Memberi hak akses:
     * menu ini di-<i>grant</i> ke role {@link Tbmrole#DOKTER} dan {@link Tbmrole#ADMINISTRATOR} lewat
     * {@code job_has_menu} (visibilitas) serta {@link RolePrivilage} (CRUD) via {@link #ensurePrivilege}.</p>
     *
     * <p><b>Idempoten.</b> Aman dipanggil berulang saat startup (get-by-id lalu saveOrUpdate lewat
     * {@link #ensureMenuJagaAktif}; id 7-digit 1710000+ agar tidak bertabrakan dengan id acak 6-digit).
     * Baris menu baru dibuat dengan {@code aktif=false}; baris yang sudah ada TIDAK dipaksa kembali
     * {@code aktif=true} setiap startup, sehingga penonaktifan manual oleh admin tetap dihormati.
     * Transaksi hak-akses berjalan terpisah agar kegagalannya tidak membatalkan pembuatan role/menu,
     * dan session native SELALU ditutup pada blok {@code finally}.</p>
     */
    public static void ensureDokterMenus() {
        Session session = null;
        Transaction tx = null;
        try {
            session = HibernateUtil.currentNativeSession();
            tx = session.beginTransaction();

            // (1) Pastikan ROLE "Dokter" ada (pola sama seperti InitDataHelper).
            Tbmrole roleDokter = (Tbmrole) session.get(Tbmrole.class, Tbmrole.DOKTER);
            if (roleDokter == null) {
                roleDokter = new Tbmrole();
                roleDokter.setRoleId(Tbmrole.DOKTER);
                roleDokter.setRoleName("Dokter");
                roleDokter.setAktif(Boolean.FALSE);
                session.save(roleDokter);
            }

            // (2) Grup menu "Dokter" (root=0) + 4 halaman inti (root = child-code grup).
            final long GRUP = 1710090L;
            Menu[] all = new Menu[5];
            int i = 0;
            all[i++] = ensureMenuJagaAktif(session, 1710000L, 0L, GRUP, "Dokter", null,
                    "fas fa-user-md", 14, Boolean.FALSE);
            all[i++] = ensureMenuJagaAktif(session, 1710001L, GRUP, 1710001L, "Rekam Medis (Rawat Jalan)",
                    "/pages/master/sirs/diagnosa_penyakit.zul", "fas fa-notes-medical", 1, Boolean.FALSE);
            all[i++] = ensureMenuJagaAktif(session, 1710002L, GRUP, 1710002L, "Rekam Medis (Rawat Inap)",
                    "/pages/master/sirs/diagnosa_penyakit_rawat_inap.zul", "fas fa-procedures", 2, Boolean.FALSE);
            all[i++] = ensureMenuJagaAktif(session, 1710003L, GRUP, 1710003L, "Data Pasien",
                    "/pages/master/sirs/pasien.zul", "fas fa-user-injured", 3, Boolean.FALSE);
            all[i++] = ensureMenuJagaAktif(session, 1710004L, GRUP, 1710004L, "Dasbor eMedic",
                    "/pages/master/sirs/dashboard_emedic.zul", "fas fa-hospital", 4, Boolean.FALSE);

            tx.commit();
            tx = null;
            System.out.println("[MenuHelper] Role & menu 'Dokter' berhasil dipastikan (grup 1710000).");

            // (3) Hak akses: role 'Dokter' + 'am' (Administrator).
            Transaction tx2 = session.beginTransaction();
            try {
                int ins = session.createSQLQuery(
                        "INSERT INTO public.job_has_menu (job, menu) "
                      + "SELECT r.roleid, m.id FROM public.tbmrole r "
                      + "CROSS JOIN public.menu m "
                      + "WHERE m.id IN (" + DOKTER_MENU_IDS + ") "
                      + "  AND (r.roleid = '" + Tbmrole.DOKTER + "' OR r.roleid = '" + Tbmrole.ADMINISTRATOR + "') "
                      + "  AND NOT EXISTS (SELECT 1 FROM public.job_has_menu x "
                      + "                  WHERE x.job = r.roleid AND x.menu = m.id)")
                        .executeUpdate();

                List<?> jobs = session.createSQLQuery(
                        "SELECT DISTINCT job FROM public.job_has_menu WHERE menu IN (" + DOKTER_MENU_IDS + ")").list();
                for (Object job : jobs) {
                    if (job == null) {
                        continue;
                    }
                    String roleId = String.valueOf(job).trim();
                    for (int k = 0; k < all.length; k++) {
                        ensurePrivilege(session, roleId, all[k]);
                    }
                }
                tx2.commit();
                System.out.println("[MenuHelper] Hak akses menu Dokter dipastikan (job_has_menu +"
                        + ins + " baris, privilege " + jobs.size() + " role).");
            } catch (Exception e) {
                if (tx2 != null && tx2.isActive()) try { tx2.rollback(); } catch (Exception ignored) {}
                System.err.println("[MenuHelper] Gagal assign hak akses menu Dokter: " + e.getMessage());
                e.printStackTrace();
            }

        } catch (Exception e) {
            if (tx != null && tx.isActive()) try { tx.rollback(); } catch (Exception ignored) {}
            System.err.println("[MenuHelper] Gagal seeding role/menu Dokter: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (session != null) {
                try { session.disconnect(); } catch (Exception ignored) {}
                try { session.close(); } catch (Exception ignored) {}
            }
            HibernateUtil.closeSession();
        }
    }

    /**
     * Memperbaiki URL menu <b>grup SIRS</b> yang KOSONG padahal item-nya bisa di-klik sebagai tautan,
     * sehingga tidak lagi mengarah ke halaman default {@code /WEB-INF/z/x/y/index.zul} yang tidak ada
     * ("... index.zul not found").
     *
     * <p><b>Kasus:</b> Menu grup seperti "Registrasi Pasien", "Customer Service", "Pendataan &amp;
     * Utiliti" (dan sub-grup SIRS lain: Poliklinik/Apotik/Kasir/dll.) sebenarnya berisi beberapa
     * halaman, tetapi URL grupnya kosong. Pada renderer yang TIDAK menampilkan sub-anaknya (mis. saat
     * flyout terdalam di-klik langsung), grup dirender sebagai tautan tunggal ber-URL kosong →
     * {@code /baru?p=&menu=<id>} → gagal memuat halaman. Perbaikan ini mengarahkan setiap grup semacam
     * itu ke <b>halaman ANAK PERTAMA-nya</b> (halaman {@code /pages/master/sirs/...}); anak-anaknya
     * tetap tampil sebagai flyout pada renderer yang mendukungnya.</p>
     *
     * <p><b>Generik &amp; ter-scope:</b> hanya menyentuh grup yang punya minimal satu anak ber-URL
     * {@code /pages/master/sirs/%} (jadi terbatas pada menu SIRS; modul induk/menu lain yang anaknya
     * masih grup TIDAK ikut diubah). URL diambil dari anak pertama (urut id).</p>
     *
     * <p><b>Idempoten &amp; aman:</b> hanya meng-UPDATE bila URL grup saat ini benar-benar kosong
     * (tidak menimpa URL yang sudah diisi). {@link #ensureMenusDariSnapshot()} hanya MEMBUAT menu yang
     * belum ada (melewati id yang sudah ada), jadi perbaikan URL untuk menu yang TERLANJUR ada wajib
     * lewat UPDATE di sini. Session native ditutup di {@code finally}.</p>
     */
    public static void healSirsMenuUrls() {
        Session session = null;
        Transaction tx = null;
        try {
            session = HibernateUtil.currentNativeSession();
            tx = session.beginTransaction();
            // Untuk setiap grup ber-URL kosong yang anaknya ada halaman /pages/master/sirs/*, isi URL
            // grup dengan URL halaman anak PERTAMA (urut id). Ter-scope ke SIRS via pola URL anak.
            int n = session.createSQLQuery(
                    "UPDATE public.menu p SET url = ("
                  + "  SELECT c.url FROM public.menu c "
                  + "  WHERE c.root = p.child AND c.url IS NOT NULL AND btrim(c.url) <> '' "
                  + "    AND c.url LIKE '/pages/master/sirs/%' "
                  + "  ORDER BY c.id LIMIT 1) "
                  + "WHERE (p.url IS NULL OR btrim(p.url) = '') "
                  + "  AND EXISTS (SELECT 1 FROM public.menu c WHERE c.root = p.child "
                  + "    AND c.url IS NOT NULL AND btrim(c.url) <> '' AND c.url LIKE '/pages/master/sirs/%')")
                    .executeUpdate();
            tx.commit();
            tx = null;
            System.out.println("[MenuHelper] healSirsMenuUrls: URL grup SIRS kosong diperbaiki (" + n + " baris).");
        } catch (Exception e) {
            if (tx != null && tx.isActive()) try { tx.rollback(); } catch (Exception ignored) {}
            System.err.println("[MenuHelper] healSirsMenuUrls gagal: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (session != null) {
                try { session.disconnect(); } catch (Exception ignored) {}
                try { session.close(); } catch (Exception ignored) {}
            }
            HibernateUtil.closeSession();
        }
    }

    /**
     * Memastikan RolePrivilage (CRUD penuh) antara satu role dan satu menu terdaftar.
     * Idempoten: tidak membuat duplikat jika sudah ada.
     */
    /**
     * FIX akar masalah "Transaction not successfully started" pada tx2.commit() di
     * {@link #ensureKantinMenus()}: PostgreSQL meng-ABORT SELURUH transaksi begitu SATU statement di
     * dalamnya gagal (mis. constraint aneh/data korup pada satu baris role/menu) -- setiap statement
     * BERIKUTNYA pada transaksi yang sama otomatis ikut gagal dgn "current transaction is aborted"
     * (SQLState 25P02) sampai ROLLBACK. Versi lama menelan tiap kegagalan lalu tetap MELANJUTKAN
     * memanggil method ini utk puluhan/ratusan kombinasi role x menu berikutnya pada tx2 yang SAMA --
     * semuanya pasti gagal juga, dan akhirnya tx2.commit() ikut gagal krn transaksi sudah rusak total.
     * Sekarang method ini mengembalikan {@code false} begitu mendeteksi transaksi sudah rusak, supaya
     * pemanggil (loop di {@link #ensureKantinMenus()}) langsung BERHENTI alih-alih terus memanggil
     * query yang pasti gagal -- mengurangi noise log & mempercepat jalur ke rollback+retry berikutnya.
     *
     * @return {@code true} bila aman melanjutkan ke kombinasi role/menu berikutnya, {@code false} bila
     *         transaksi sudah rusak (pemanggil harus berhenti loop).
     */
    private static boolean ensurePrivilege(Session session, String roleId, Menu menu) {
        if (menu == null || menu.getId() == null) return true;
        try {
            Number count = (Number) session.createCriteria(RolePrivilage.class)
                    .createAlias("role", "r")
                    .createAlias("menu", "m")
                    .add(org.hibernate.criterion.Restrictions.eq("r.roleId", roleId))
                    .add(org.hibernate.criterion.Restrictions.eq("m.id", menu.getId()))
                    .setProjection(org.hibernate.criterion.Projections.rowCount())
                    .uniqueResult();
            if (count != null && count.intValue() > 0) return true;

            Tbmrole role = (Tbmrole) session.get(Tbmrole.class, roleId);
            if (role == null) {
                System.err.println("[MenuHelper] Role '" + roleId + "' tidak ditemukan, privilege tidak ditambahkan.");
                return true;
            }
            RolePrivilage priv = new RolePrivilage();
            priv.setRole(role);
            priv.setMenu(menu);
            priv.setCreate(1);
            priv.setRead(1);
            priv.setUpdate(1);
            priv.setDelete(1);
            session.save(priv);
            return true;
        } catch (Exception e) {
            System.err.println("[MenuHelper] ensurePrivilege gagal menu=" + menu.getId() + " role=" + roleId + ": " + e.getMessage());
            return !transaksiPostgresRusak(e);
        }
    }

    /** Deteksi SQLState 25P02 (PostgreSQL "current transaction is aborted") di sepanjang rantai cause. */
    private static boolean transaksiPostgresRusak(Throwable e) {
        Throwable cur = e;
        while (cur != null) {
            if (cur instanceof java.sql.SQLException && "25P02".equals(((java.sql.SQLException) cur).getSQLState())) {
                return true;
            }
            cur = cur.getCause();
        }
        return false;
    }

    // =========================================================================
    // AKREDITASI MENU SEEDING
    // =========================================================================

    /**
     * Memastikan menu modul Akreditasi terdaftar di database dan terpasang
     * ke hak akses "am" (Admin Manajemen).
     *
     * <p>Saat ini hanya mendaftarkan sub-menu LAMDIK S1 (DKPS 2.0).
     * Sub-menu LKPS S1 (id 16101) dan menu "Akreditasi Online (Sapto)"
     * dinonaktifkan via {@link #cleanupObsoleteMenus()}.
     * Di masa mendatang, akreditasi jenis lain (ASESMEN, AIPT, dll.) cukup
     * ditambahkan di sini sebagai m03, m04, dst.</p>
     */
    public static void ensureAkreditasiMenus() {
        Session session = null;
        Transaction tx = null;
        try {
            session = HibernateUtil.currentNativeSession();
            tx = session.beginTransaction();

            final long ROOT = 0L;

            Menu root = ensureMenu(session, 161L, ROOT, 161L,
                    "Akreditasi",
                    null,
                    "fas fa-certificate", 13, Boolean.FALSE);

            // LAMDIK S1 — satu-satunya sub-menu aktif saat ini
            Menu m02 = ensureMenu(session, 16102L, 161L, 16102L,
                    "LAMDIK S1 (DKPS 2.0)",
                    "/pages/master/akreditasi/akreditasi_lamdik_s1.zul",
                    "fas fa-graduation-cap", 1, Boolean.FALSE);

            tx.commit();

            // Pasang hak akses ke role "am"
            session = HibernateUtil.currentNativeSession();
            tx = session.beginTransaction();
            Tbmrole roleAm = (Tbmrole) session.get(Tbmrole.class, "am");
            attachMenu(roleAm, root);
            attachMenu(roleAm, m02);
            tx.commit();
            System.out.println("[MenuHelper] ensureAkreditasiMenus selesai.");
        } catch (Exception e) {
            if (tx != null) try { tx.rollback(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/MenuHelper.java:910");}
            System.err.println("[MenuHelper] ensureAkreditasiMenus gagal: " + e.getMessage());
            e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/MenuHelper.java:912");
        } finally {
            if (session != null) {
                try { session.disconnect(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/MenuHelper.java:915");}
                try { session.close(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/MenuHelper.java:916");}
            }
            HibernateUtil.closeSession();
        }
    }

    /**
     * Mendaftarkan menu Penjaminan Mutu ke sistem menu sebagai sub-item langsung
     * di bawah grup <b>"Sistem Informasi Akademik"</b>, diposisikan setelah
     * "Angket dan Survey".
     *
     * <ul>
     *   <li>Sub-menu : <b>16201L</b> — "Penjaminan Mutu"
     *       → {@code /pages/master/spmi/analisis_butir_penjaminan_mutu.zul}</li>
     * </ul>
     *
     * <p><b>Riwayat bug penting:</b></p>
     * <ul>
     *   <li>Versi awal memakai ID root <b>162L</b> — TABRAKAN dengan menu live
     *       "Keuangan Mahasiswa" (root 40, child 8). Blok pemulihan di bawah
     *       MENGEMBALIKAN baris 162 ke wujud aslinya bila masih terbajak.</li>
     *   <li>Versi kedua membuat top-level root <b>9916200L</b> "Penjaminan Mutu".
     *       Root ini kini dihapus agar tidak menggantung sebagai grup kosong di
     *       mega-menu. Sub-item (16201L) dipindah langsung di bawah SIA.</li>
     * </ul>
     *
     * <p>JANGAN pernah memakai ID kecil (&lt; 7 digit) untuk menu baru — rawan
     * tabrakan dengan data menu lama.</p>
     *
     * <p>Idempoten: aman dipanggil berkali-kali (menggunakan {@code saveOrUpdate}).</p>
     * <p>Hak akses diberikan ke role {@code am} (administrator) dan {@code Akademik}.</p>
     */
    public static void ensurePenjaminanMutuAnalisisMenu() {
        Session session = null;
        Transaction tx = null;
        try {
            session = HibernateUtil.currentNativeSession();
            tx = session.beginTransaction();

            // PEMULIHAN: kembalikan baris 162 "Keuangan Mahasiswa" (root 40, child 8)
            // yang terlanjur dibajak versi awal method ini menjadi "Penjaminan Mutu".
            Menu m162 = (Menu) session.get(Menu.class, 162L);
            if (m162 != null && m162.getLabel() != null
                    && "Penjaminan Mutu".equalsIgnoreCase(m162.getLabel().trim())) {
                m162.setLabel("Keuangan Mahasiswa");
                m162.setRoot(40L);
                m162.setChild(8L);
                m162.setUrl(null);
                m162.setIcon(null);
                m162.setBigIcon(null);
                m162.setNomorUrut(0);
                m162.setAktif(Boolean.TRUE);
                session.saveOrUpdate(m162);
                System.out.println("[MenuHelper] Baris menu 162 dipulihkan ke 'Keuangan Mahasiswa' (root 40, child 8).");
            }

            // Temukan grup "Sistem Informasi Akademik" secara dinamis (root=0, url=null)
            Long siAkademikId = null;
            @SuppressWarnings("unchecked")
            List<Menu> allMenus = session.createCriteria(Menu.class)
                    .add(org.hibernate.criterion.Restrictions.isNull("url"))
                    .list();
            for (Menu m : allMenus) {
                if (m.getLabel() != null
                        && m.getLabel().toLowerCase().contains("sistem informasi akademik")
                        && m.getRoot() != null && m.getRoot() == 0L) {
                    siAkademikId = m.getId();
                    break;
                }
            }
            if (siAkademikId == null) siAkademikId = 3L; // fallback ke ID 3 jika tidak ditemukan

            // Temukan nomorUrut SETELAH "Angket dan Survey"
            int urutSetelahAngket = 99;
            @SuppressWarnings("unchecked")
            List<Menu> subAkademik = session.createCriteria(Menu.class)
                    .add(org.hibernate.criterion.Restrictions.eq("root", siAkademikId))
                    .list();
            for (Menu m : subAkademik) {
                if (m.getLabel() != null && m.getLabel().toLowerCase().contains("angket")) {
                    urutSetelahAngket = m.getNomorUrut() != null ? m.getNomorUrut() + 1 : 99;
                    break;
                }
            }

            // Hapus root lama "Penjaminan Mutu" (9916200L) agar tidak muncul di top menu
            Menu pmRoot = (Menu) session.get(Menu.class, 9916200L);
            if (pmRoot != null) {
                session.delete(pmRoot);
            }

            // Daftarkan "Penjaminan Mutu" sebagai sub-item langsung di bawah SIA
            Menu m01 = ensureMenu(session, 16201L, siAkademikId, 16201L,
                    "Penjaminan Mutu",
                    "/pages/master/spmi/analisis_butir_penjaminan_mutu.zul",
                    "fas fa-shield-alt", urutSetelahAngket, Boolean.FALSE);

            tx.commit();

            // Hak akses ke role "am" (administrator) dan "Akademik"
            session = HibernateUtil.currentNativeSession();
            tx = session.beginTransaction();
            Tbmrole roleAm = (Tbmrole) session.get(Tbmrole.class, Tbmrole.ADMINISTRATOR);
            Tbmrole roleAkademik = (Tbmrole) session.get(Tbmrole.class, Tbmrole.AKADEMIK);
            if (roleAm != null) {
                attachMenu(roleAm, m01);
            }
            if (roleAkademik != null) {
                attachMenu(roleAkademik, m01);
            }
            tx.commit();
            System.out.println("[MenuHelper] ensurePenjaminanMutuAnalisisMenu selesai.");
        } catch (Exception e) {
            if (tx != null) try { tx.rollback(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) MenuHelper.ensurePenjaminanMutuAnalisisMenu:rollback");}
            System.err.println("[MenuHelper] ensurePenjaminanMutuAnalisisMenu gagal: " + e.getMessage());
            e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit MenuHelper.ensurePenjaminanMutuAnalisisMenu");
        } finally {
            if (session != null) {
                try { session.disconnect(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) MenuHelper.ensurePenjaminanMutuAnalisisMenu:disconnect");}
                try { session.close(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) MenuHelper.ensurePenjaminanMutuAnalisisMenu:close");}
            }
            HibernateUtil.closeSession();
        }
    }

    // =========================================================================
    // SATUAN PENGAWASAN INTERNAL (SPI) — ROLE & MENU SEEDING
    // =========================================================================

    /**
     * Memastikan baris {@link Tbmrole} untuk role {@link Tbmrole#SPI} ("Satuan Pengawasan
     * Internal") tersedia di database. Idempoten: dipanggil berkali-kali saat startup tanpa
     * membuat duplikat maupun menimpa data yang sudah pernah disunting operator (mis. nama
     * peran yang sudah di-kustomisasi) &mdash; hanya membuat baris bila benar-benar belum ada.
     *
     * <p>
     * Role ini SENGAJA dibuat lewat jalur ringan &amp; terisolasi ini (bukan lewat
     * {@code InitDataHelper.handleTbmRole}, tempat sebagian besar role bawaan lain di-seed)
     * karena method tersebut sudah besar dan kompleks, terikat erat dengan banyak field
     * {@code ConstantValues} dan flag khusus per-role (mis. {@code Tbmrole.KANTIN} yang punya
     * flag {@code setKantin(true)} dan {@code halamanUtama} tersendiri). Modul SPI Bagian A ini
     * belum membutuhkan integrasi sedalam itu, jadi menambah baris seeder sendiri yang sederhana
     * dan mudah ditelusuri jauh lebih aman daripada menyisipkan cabang baru ke method besar yang
     * sudah dipakai luas oleh role-role lain &mdash; menghindari risiko regresi pada startup
     * aplikasi yang berdampak ke seluruh pengguna, bukan hanya modul SPI.
     * </p>
     */
    public static void ensureSpiRole() {
        Session session = null;
        Transaction tx = null;
        try {
            session = HibernateUtil.currentNativeSession();
            tx = session.beginTransaction();

            Tbmrole role = (Tbmrole) session.get(Tbmrole.class, Tbmrole.SPI);
            if (role == null) {
                role = new Tbmrole();
                role.setRoleId(Tbmrole.SPI);
                role.setRoleName("Satuan Pengawasan Internal");
                session.save(role);
                System.out.println("[MenuHelper] Role '" + Tbmrole.SPI + "' dibuat.");
            }

            tx.commit();
        } catch (Exception e) {
            if (tx != null && tx.isActive()) try { tx.rollback(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) MenuHelper.ensureSpiRole:rollback"); }
            System.err.println("[MenuHelper] ensureSpiRole gagal: " + e.getMessage());
            e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit MenuHelper.ensureSpiRole");
        } finally {
            if (session != null) {
                try { session.disconnect(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) MenuHelper.ensureSpiRole:disconnect"); }
                try { session.close(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) MenuHelper.ensureSpiRole:close"); }
            }
            HibernateUtil.closeSession();
        }
    }

    /**
     * Memastikan menu modul Satuan Pengawasan Internal (SPI) terdaftar di database dan
     * terpasang hak aksesnya. Idempoten: aman dipanggil berkali-kali (memakai
     * {@code saveOrUpdate} lewat {@link #ensureMenuJagaAktif}). Baris baru dibuat dengan
     * {@code aktif=false}; baris yang sudah ada TIDAK dipaksa kembali {@code aktif=true} setiap
     * startup, sehingga penonaktifan manual oleh admin tetap dihormati.
     *
     * <p><b>Struktur menu:</b></p>
     * <ul>
     *   <li>Root top-level <b>1721000L</b> &mdash; "Satuan Pengawasan Internal".</li>
     *   <li>Sub-menu <b>1721001L</b> &mdash; "Setup SPI" &rarr;
     *       {@code /pages/master/spi/jenis_audit_spi.zul} (layar tab Jenis/Kriteria/Checklist
     *       Audit, Bagian A).</li>
     *   <li>Sub-menu <b>1721002L</b> &mdash; "Profil Risiko Audit" &rarr;
     *       {@code /pages/master/spi/profil_risiko_spi.zul} (penilaian risiko per unit kerja,
     *       Bagian B).</li>
     *   <li>Sub-menu <b>1721003L</b> &mdash; "Rencana Audit Tahunan (PKPT)" &rarr;
     *       {@code /pages/master/spi/rencana_audit_tahunan_spi.zul} (penyusunan program kerja
     *       pengawasan tahunan, Bagian B).</li>
     *   <li>Sub-menu <b>1721004L</b> &mdash; "Penugasan Audit SPI" &rarr;
     *       {@code /pages/master/spi/penugasan_audit_spi.zul} (dasbor + pelaksanaan audit lengkap
     *       dengan alur persetujuan SOP, Bagian C).</li>
     * </ul>
     * <p>
     * Blok ID <b>1721000+</b> dipilih karena belum dipakai baris menu manapun (diverifikasi
     * lewat pencarian di seluruh basis kode sebelum dipakai) &mdash; mengikuti peringatan baku
     * di javadoc kelas ini: JANGAN pernah memakai ID kecil (&lt; 7 digit) untuk menu baru,
     * karena rawan tabrakan dengan data menu lama yang sudah berumur belasan tahun.
     * </p>
     * <p>
     * Menu diatur tampil di KEDUA konteks lembaga ({@code tampilDiSekolah=true} DAN
     * {@code tampilDiPt=true}, default bawaan {@link #ensureMenu}) karena modul SPI harus
     * terintegrasi dengan eCampus (perguruan tinggi) maupun eSchool (sekolah) sekaligus,
     * bukan hanya salah satunya &mdash; sesuai permintaan awal modul ini.
     * </p>
     * <p>
     * Hak akses diberikan ke role {@code am} (Administrator, akses penuh semua modul) dan role
     * {@link Tbmrole#SPI} yang baru (lihat {@link #ensureSpiRole()}) &mdash; role SPI HARUS
     * sudah ada sebelum method ini dipanggil, karena bila belum ada
     * {@link #ensurePrivilege(Session, String, Menu)} akan melewati pemasangan privilege-nya
     * dengan aman (hanya mencatat pesan log, tidak melempar exception) dan bisa dilengkapi
     * otomatis pada pemanggilan startup berikutnya begitu role sudah tersedia.
     * </p>
     */
    public static void ensureSpiMenus() {
        Session session = null;
        Transaction tx = null;
        try {
            session = HibernateUtil.currentNativeSession();
            tx = session.beginTransaction();

            final long ROOT = 0L;

            Menu root = ensureMenuJagaAktif(session, 1721000L, ROOT, 1721000L,
                    "Satuan Pengawasan Internal",
                    null,
                    "fas fa-magnifying-glass-dollar", 14, Boolean.FALSE);

            Menu setup = ensureMenuJagaAktif(session, 1721001L, 1721000L, 1721001L,
                    "Setup SPI",
                    "/pages/master/spi/jenis_audit_spi.zul",
                    "fas fa-list-check", 1, Boolean.FALSE);

            Menu profilRisiko = ensureMenuJagaAktif(session, 1721002L, 1721000L, 1721002L,
                    "Profil Risiko Audit",
                    "/pages/master/spi/profil_risiko_spi.zul",
                    "fas fa-gauge-high", 2, Boolean.FALSE);

            Menu rencanaTahunan = ensureMenuJagaAktif(session, 1721003L, 1721000L, 1721003L,
                    "Rencana Audit Tahunan (PKPT)",
                    "/pages/master/spi/rencana_audit_tahunan_spi.zul",
                    "fas fa-calendar-check", 3, Boolean.FALSE);

            Menu penugasan = ensureMenuJagaAktif(session, 1721004L, 1721000L, 1721004L,
                    "Penugasan Audit SPI",
                    "/pages/master/spi/penugasan_audit_spi.zul",
                    "fas fa-clipboard-check", 4, Boolean.FALSE);

            tx.commit();

            // Pasang hak akses ke role "am" (administrator) dan "SPI"
            session = HibernateUtil.currentNativeSession();
            tx = session.beginTransaction();

            Menu[] semuaMenu = { root, setup, profilRisiko, rencanaTahunan, penugasan };

            Tbmrole roleAm = (Tbmrole) session.get(Tbmrole.class, Tbmrole.ADMINISTRATOR);
            if (roleAm != null) {
                for (Menu m : semuaMenu) {
                    attachMenu(roleAm, m);
                    ensurePrivilege(session, Tbmrole.ADMINISTRATOR, m);
                }
            }

            Tbmrole roleSpi = (Tbmrole) session.get(Tbmrole.class, Tbmrole.SPI);
            if (roleSpi != null) {
                for (Menu m : semuaMenu) {
                    attachMenu(roleSpi, m);
                    ensurePrivilege(session, Tbmrole.SPI, m);
                }
            }

            tx.commit();
            System.out.println("[MenuHelper] ensureSpiMenus selesai.");
        } catch (Exception e) {
            if (tx != null && tx.isActive()) try { tx.rollback(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) MenuHelper.ensureSpiMenus:rollback"); }
            System.err.println("[MenuHelper] ensureSpiMenus gagal: " + e.getMessage());
            e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit MenuHelper.ensureSpiMenus");
        } finally {
            if (session != null) {
                try { session.disconnect(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) MenuHelper.ensureSpiMenus:disconnect"); }
                try { session.close(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) MenuHelper.ensureSpiMenus:close"); }
            }
            HibernateUtil.closeSession();
        }
    }

    /**
     * Memastikan satu menu reimbursement berada tepat setelah Pengajuan Uang Muka
     * dan otomatis terlihat untuk role am serta keu. Dashboard dan laporan
     * disajikan sebagai tab internal, bukan menu terpisah.
     * Dijalankan setiap startup dan idempoten.
     */
    public static void ensureReimbursementMenus() {
        final Long ROOT_UANG_MUKA = 400000009L;
        final Long id = 260815001L;
        Session session = null;
        Transaction tx = null;
        try {
            session = HibernateUtil.currentNativeSession();
            tx = session.beginTransaction();
            // Bersihkan dua menu versi lama; isinya tetap dipakai sebagai tab include.
            session.createSQLQuery("DELETE FROM public.role_privilage WHERE menu IN (260815002,260815003)")
                    .executeUpdate();
            session.createSQLQuery("DELETE FROM public.job_has_menu WHERE menu IN (260815002,260815003)")
                    .executeUpdate();
            session.createSQLQuery("DELETE FROM public.menu WHERE id IN (260815002,260815003)")
                    .executeUpdate();
            Menu[] menus = new Menu[] {
                ensureMenu(session, id, ROOT_UANG_MUKA, 600000002L, "Reimbursement Pegawai",
                        "/pages/master/akunting/reimbursement_pegawai.zul", "fas fa-receipt", 0, Boolean.FALSE)
            };
            tx.commit(); tx = null;

            session = HibernateUtil.currentNativeSession();
            tx = session.beginTransaction();
            String idList = String.valueOf(id);
            session.createSQLQuery(
                    "INSERT INTO public.job_has_menu (job, menu) "
                  + "SELECT r.roleid, m.id FROM public.tbmrole r CROSS JOIN public.menu m "
                  + "WHERE LOWER(r.roleid) IN ('" + Tbmrole.ADMINISTRATOR.toLowerCase() + "','"
                  + Tbmrole.KEUANGAN.toLowerCase() + "') AND m.id IN (" + idList + ") "
                  + "AND NOT EXISTS (SELECT 1 FROM public.job_has_menu x WHERE x.job=r.roleid AND x.menu=m.id)")
                    .executeUpdate();
            for (int i = 0; i < menus.length; i++) {
                ensureReimbursementPrivilege(session, Tbmrole.ADMINISTRATOR, menus[i]);
                ensureReimbursementPrivilege(session, Tbmrole.KEUANGAN, menus[i]);
            }
            tx.commit(); tx = null;
            ais.common.newui.NewUiCacheInvalidator.invalidateRole(Tbmrole.ADMINISTRATOR);
            ais.common.newui.NewUiCacheInvalidator.invalidateRole(Tbmrole.KEUANGAN);
            System.out.println("[MenuHelper] Menu reimbursement dan akses am/keu berhasil dipastikan.");
        } catch (Exception e) {
            if (tx != null && tx.isActive()) try { tx.rollback(); } catch (Exception ignored) {
                ais.common.ErrorAuditUtil.record(ignored, "MenuHelper.ensureReimbursementMenus.rollback");
            }
            System.err.println("[MenuHelper] ensureReimbursementMenus gagal: " + e.getMessage());
            ais.common.ErrorAuditUtil.record(e, "MenuHelper.ensureReimbursementMenus");
        } finally {
            if (session != null) {
                try { session.disconnect(); } catch (Exception ignored) { }
                try { session.close(); } catch (Exception ignored) { }
            }
            HibernateUtil.closeSession();
        }
    }

    private static void ensureReimbursementPrivilege(Session session, String roleId, Menu menu) {
        if (session.get(Tbmrole.class, roleId) == null || menu == null) return;
        ensurePrivilege(session, roleId, menu);
        RolePrivilage privilege = (RolePrivilage) session.createCriteria(RolePrivilage.class)
                .createAlias("role", "r").createAlias("menu", "m")
                .add(org.hibernate.criterion.Restrictions.eq("r.roleId", roleId))
                .add(org.hibernate.criterion.Restrictions.eq("m.id", menu.getId()))
                .setMaxResults(1).uniqueResult();
        if (privilege != null) {
            privilege.setRead(1); privilege.setCreate(1); privilege.setUpdate(1); privilege.setDelete(1);
            privilege.setApprove(1); privilege.setReject(1); session.saveOrUpdate(privilege);
        }
    }

    private static Menu ensureMenu(Session session, Long id, Long root, Long child, String label, String url,
            String icon, Integer nomorUrut, Boolean bukaHalamanBaru) {
        Menu menu = (Menu) session.get(Menu.class, id);
        if (menu == null) {
            menu = new Menu();
            menu.setId(id);
        }
        menu.setRoot(root);
        menu.setChild(child);
        menu.setLabel(label);
        menu.setUrl(url);
        menu.setIcon(icon);
        menu.setBigIcon(icon);
        menu.setNomorUrut(nomorUrut);
        menu.setAktif(Boolean.TRUE);
        menu.setTampilDiSekolah(Boolean.TRUE);
        menu.setTampilDiPt(Boolean.TRUE);
        menu.setBukaHalamanBaru(bukaHalamanBaru);
        session.saveOrUpdate(menu);
        return menu;
    }

    /**
     * Varian {@link #ensureMenu} yang TIDAK memaksa {@code aktif=true} pada baris menu yang sudah ada.
     *
     * <p><b>Kenapa perlu varian terpisah.</b> {@link #ensureMenu} selalu menyetel {@code aktif=true}
     * setiap kali dipanggil, termasuk pada baris yang sudah ada — cocok untuk menu inti yang memang
     * harus "self-heal" kembali aktif bila termatikan tak sengaja. Untuk menu yang perlu bisa
     * dinonaktifkan permanen oleh admin (dipakai untuk menu "Dokter" dan "Satuan Pengawasan Internal"),
     * memaksa {@code aktif=true} di setiap startup akan menimpa keputusan admin yang sudah
     * menonaktifkannya secara manual.</p>
     *
     * <p><b>Perilaku:</b> jika baris menu {@code id} belum ada, dibuat baru dengan {@code aktif=FALSE}
     * (default aman: menu baru tidak langsung aktif). Jika baris menu sudah ada, field lain
     * (root/child/label/url/icon/nomorUrut/bukaHalamanBaru/tampilDiSekolah/tampilDiPt) tetap
     * disinkronkan seperti {@link #ensureMenu}, tapi kolom {@code aktif} sama sekali tidak disentuh —
     * nilai yang tersimpan di database (hasil toggle admin ataupun default FALSE sebelumnya)
     * dipertahankan apa adanya.</p>
     */
    private static Menu ensureMenuJagaAktif(Session session, Long id, Long root, Long child, String label,
            String url, String icon, Integer nomorUrut, Boolean bukaHalamanBaru) {
        Menu menu = (Menu) session.get(Menu.class, id);
        boolean baru = menu == null;
        if (baru) {
            menu = new Menu();
            menu.setId(id);
        }
        menu.setRoot(root);
        menu.setChild(child);
        menu.setLabel(label);
        menu.setUrl(url);
        menu.setIcon(icon);
        menu.setBigIcon(icon);
        menu.setNomorUrut(nomorUrut);
        if (baru) {
            menu.setAktif(Boolean.FALSE);
        }
        menu.setTampilDiSekolah(Boolean.TRUE);
        menu.setTampilDiPt(Boolean.TRUE);
        menu.setBukaHalamanBaru(bukaHalamanBaru);
        session.saveOrUpdate(menu);
        return menu;
    }

    /**
     * Pindahkan (reparent) menu yang cocok URL-nya ke sebuah grup (root = child-code grup tujuan) dan
     * set urutannya. Idempoten dan aman: bila tidak ada baris menu ber-URL tersebut, tidak melakukan
     * apa-apa. Memakai entity Hibernate (bukan SQL mentah) agar tidak bergantung pada nama kolom
     * fisik {@code nomorUrut}. Grant/hak akses tidak diubah di sini.
     *
     * @return jumlah baris menu yang dipindahkan
     */
    @SuppressWarnings("unchecked")
    private static int reparentMenuByUrl(Session session, String urlLike, Long rootTujuan, Integer urut) {
        int n = 0;
        try {
            List<Menu> menus = session.createQuery("from Menu m where m.url like :u").setParameter("u", urlLike)
                    .list();
            for (Menu m : menus) {
                if (m == null) {
                    continue;
                }
                m.setRoot(rootTujuan);
                if (urut != null) {
                    m.setNomorUrut(urut);
                }
                m.setAktif(Boolean.TRUE);
                session.saveOrUpdate(m);
                n++;
            }
        } catch (Exception e) {
            System.err.println("[MenuHelper] reparentMenuByUrl gagal (" + urlLike + "): " + e.getMessage());
        }
        return n;
    }

    /**
     * Attach menu ke role hanya jika belum ter-attach.
     *
     * @return true jika menu benar-benar ditambahkan (ada perubahan).
     */
    @SuppressWarnings("unchecked")
    private static boolean attachMenu(Tbmrole role, Menu menu) {
        if (role == null || menu == null) {
            return false;
        }
        Set<Menu> menus = role.getMenus();
        for (Menu item : menus) {
            if (item != null && item.getId() != null && item.getId().equals(menu.getId())) {
                return false;
            }
        }
        menus.add(menu);
        return true;
    }

    @SuppressWarnings("unchecked")
    public static String loadTree(Tbmuser tbmuser, HttpServletRequest request) throws Exception {
        if (tbmuser == null) return "";
        Tbmrole tbmrole = tbmuser.hakAkses();
        if (tbmrole == null) return "";

        List<Menu> menus = (List<Menu>) request.getSession().getAttribute("current_menus");
        if (menus == null) {
            Session session = null;
            try {
                session = HibernateUtil.currentNativeSession();
                session.refresh(tbmrole);
                menus = new ArrayList<Menu>(tbmrole.getMenus());
                Collections.sort(menus);
                request.getSession().setAttribute("current_menus", menus);
            } catch (Exception e) {
                e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/MenuHelper.java:1012");
            } finally {
                if (session != null) {
                    try { session.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/MenuHelper.java:1015");}
                    try { session.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/MenuHelper.java:1016");}
                }
                HibernateUtil.closeSession();
            }
        }

        Sekolah sekolah = SekolahUtil.getSekolah(request);
        Menu menuData = (Menu) request.getSession().getAttribute("current_menu");
        HashMap<Long, Long> parents = null; 

        return generateMenuHtml(menus, parents, menuData, sekolah);
    }

    public static String generateMenuHtml(List<Menu> menuList, HashMap<Long, Long> parents, Menu menuData, Sekolah sekolah) throws Exception {
        if (menuList == null || menuList.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();

        boolean aktifkanFilterPerSekolah = Common.bolehKonfigurasi("aktifkan_filter_per_sekolah", Konfigurasi.TIDAK_AKTIF);

        for (Menu menu : menuList) {
            if (menu.getRoot().equals(0L)) {
                if (aktifkanFilterPerSekolah) {
                    if (((sekolah == null || sekolah.getId() == null) && !menu.getLabel().equalsIgnoreCase("Sistem Sekolah"))
                            || (sekolah != null && sekolah.getId() != null && !menu.getLabel().equalsIgnoreCase("Sistem Informasi Akademik"))) {
                        buildMegaMenuItem(sb, menu, 1, menuList, parents, menuData);
                    }
                } else {
                    buildMegaMenuItem(sb, menu, 1, menuList, parents, menuData);
                }
            }
        }

        return sb.toString();
    }

    private static void buildMegaMenuItem(StringBuilder sb, Menu menu, int level, List<Menu> menuList, HashMap<Long, Long> parents, Menu menuData) throws Exception {
        if (menu.getAktif() != null && !menu.getAktif()) return;

        // Parent-child cocok berdasarkan CHILD-CODE (child.root == induk.getChild()) — konsisten
        // dengan renderer produksi CommonMenu/MainMenuHelper/MainTreeMenuHelper. Untuk modul legacy
        // yang id==child hasilnya sama; untuk menu ber-id≠child (mis. Usaha Koperasi/Simpan Pinjam)
        // inilah yang benar agar sub-menu TIDAK hilang (flyout kosong). menuId dipakai utk url/active.
        Long menuId = menu.getId();
        List<Menu> children = getChildren(menu.getChild(), menuList);
        boolean hasChildren = !children.isEmpty();
        boolean isAktif = menuData != null && menuData.getId().equals(menuId);
        
        String m = "";
        // Null Safety Check untuk menghindari NullPointerException
        if(menu.getUrl() != null && !menu.getUrl().trim().isEmpty()) {
            try { m = menu.getUrl().replaceAll("\\p{Punct}", ""); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/MenuHelper.java:1067");}
        }
        
        String url = buildMenuUrl(menu, m, menuId);
        String label = menu.getLabel() != null ? menu.getLabel() : "Menu";

        if (level == 1) {
            // LEVEL 1: Akan dicetak sebagai Header Kolom (Misal: "Sistem Kepegawaian")
            sb.append("<div class=\"col-md-3 mb-4\">\n");
            sb.append("  <h6 class=\"mega-menu-header\">").append(label).append("</h6>\n");
            
            if (hasChildren) {
                // Cetak anak-anaknya ke dalam list kolom
                for (Menu subChild : children) {
                    buildMegaMenuItem(sb, subChild, 2, menuList, parents, menuData);
                }
            }
            sb.append("</div>\n");

        } else if (level >= 2) {
            // LEVEL 2+: Akan dicetak sebagai Teks Link yang bisa di-klik
            String iconClass = getIconForMenu(label);
            
            // Memberikan padding/indentasi otomatis jika level menu sangat dalam (Level 3, Level 4)
            String paddingStyle = "";
            if(level > 2) {
                int indent = (level - 2) * 15 + 12; // Level 3 nambah 27px, dst
                paddingStyle = "style=\"padding-left: " + indent + "px;\"";
                // Ganti ikon untuk Sub-Sub menu agar tidak terlalu mencolok
                iconClass = "fas fa-angle-double-right text-muted";
            }
            
            sb.append("<a class=\"mega-menu-item ").append(isAktif ? "active" : "").append("\" href=\"").append(url).append("\" ").append(paddingStyle).append(">\n");
            
            if(level > 2) {
                sb.append("  <i class=\"").append(iconClass).append("\" style=\"font-size:10px;\"></i> <span>").append(label).append("</span>\n");
            } else {
                sb.append("  <i class=\"").append(iconClass).append("\"></i> <span>").append(label).append("</span>\n");
            }
            sb.append("</a>\n");
            
            // FIX 2: TERUSKAN REKURSI JIKA MASIH PUNYA ANAK.
            // Sebelumnya fungsi rekursif berhenti di sini, sehingga cucu-cucu menu menghilang.
            if (hasChildren) {
                for (Menu subChild : children) {
                    buildMegaMenuItem(sb, subChild, level + 1, menuList, parents, menuData);
                }
            }
        }
    }

    private static String buildMenuUrl(Menu menu, String encodedPage, Long menuId) throws Exception {
        String rawUrl = menu == null ? "" : menu.getUrl();
        if (rawUrl != null) {
            rawUrl = rawUrl.trim();
        }
        if (rawUrl != null && rawUrl.length() > 0) {
            String lower = rawUrl.toLowerCase();
            if (lower.startsWith("http://") || lower.startsWith("https://")) {
                return rawUrl;
            }
            if (rawUrl.startsWith("/")) {
                return Common.ROOT + rawUrl;
            }
        }
        return Common.ROOT + "/baru?p=" + URLEncoder.encode(encodedPage, "UTF-8") + "&menu=" + menuId;
    }

    /**
     * Mencari Menu yang parent-nya (Root) sama dengan ID yang diminta
     */
    private static List<Menu> getChildren(Long parentId, List<Menu> menus) {
        List<Menu> list = new ArrayList<Menu>();
        if(parentId == null) return list;
        
        for (Menu m : menus) {
            if (m.getRoot() != null && m.getRoot().equals(parentId)) {
                list.add(m);
            }
        }
        return list;
    }
}
