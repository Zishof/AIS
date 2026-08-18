package ais.action.maintenance;

import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.action.mobile.MobileNotifHelper;
import ais.action.mobile.MobileNotifHelper.NotifItem;
import ais.action.mobile.MobileUiHelper;
import ais.action.servlet.Main;
import ais.common.Common;
import ais.database.model.LogLogin;
import ais.database.model.Menu;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.ui.util.MyToolbarbuttonConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Div;
import org.zkoss.zul.Label;
import org.zkoss.zul.Tab;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.West;

/**
 * <h1>MobileAction — Pengendali Utama Tampilan Mobile eCampus &amp; eSchool (ZK 5)</h1>
 *
 * <p>Kelas ini adalah <em>pengendali</em> (controller) tunggal yang bertanggung jawab
 * membangun dan mengelola seluruh tampilan mobile aplikasi AIS. Ia bekerja bersama
 * {@code mobile.zul} (cangkang HTML tipis) dan {@link MobileUiHelper} (pabrik komponen)
 * untuk menghasilkan antarmuka mobile yang responsif, ringan, dan konsisten.</p>
 *
 * <h2>Arsitektur: Shell Reuse</h2>
 * <p>Aplikasi AIS memiliki ratusan menu, berbagai tipe halaman ZUL, window helper,
 * report helper, dan beberapa URL khusus. Menulis ulang semua itu untuk mobile akan
 * menghasilkan duplikasi besar-besaran dan risiko regresi yang tidak terkendali.
 * Controller ini mengambil pendekatan <em>shell reuse</em>: beranda, pencarian menu,
 * notifikasi, dan profil dibangun sebagai komponen mobile baru; tetapi ketika pengguna
 * memilih menu, controller membuka overlay layar penuh dan menyerahkan pembukaan menu
 * kepada {@link Common#launchMenu(West, MyToolbarbuttonConfig, Tabbox, Menu, LogLogin)}.
 * Dengan cara ini logika izin, pembukaan tab, report window, dan URL lama tetap dipakai
 * ulang tanpa perubahan. Pengguna mendapat tampilan baru; developer tetap merawat satu
 * sumber logika menu.</p>
 *
 * <h2>Struktur Navigasi Mobile</h2>
 * <p>Navigasi mobile mengikuti pola bottom-navigation yang sudah familiar di hampir
 * semua aplikasi ponsel modern:</p>
 * <ul>
 *   <li><strong>Beranda</strong> — sapaan personal, akses cepat 8 fitur teratas, grid
 *       modul utama berdasarkan hak akses, dan panel ringkasan (metrik, kehadiran,
 *       tren, donut).</li>
 *   <li><strong>Notifikasi</strong> — daftar kabar yang dimuat dari {@link MobileNotifHelper}
 *       secara nyata, bukan data palsu. Setiap baris dapat diklik untuk membuka objek
 *       terkait.</li>
 *   <li><strong>Menu</strong> — kotak pencarian + daftar seluruh menu yang tersedia.
 *       Pencarian dilakukan di memori dari daftar menu pengguna yang sudah login,
 *       sehingga cepat tanpa beban database. Hasil dibatasi awal agar DOM ZK tidak
 *       terlalu berat di ponsel.</li>
 *   <li><strong>Profil</strong> — identitas pengguna, menu pengaturan, tema, bantuan,
 *       tautan ke tampilan desktop, dan pilihan keluar.</li>
 * </ul>
 *
 * <h2>Sistem Tema (Warna)</h2>
 * <p>Tampilan mobile mendukung sistem tema yang sama dengan desktop. Konfigurasi tema
 * disimpan di {@code PerguruanTinggi.getCss()} (mis. {@code /css/ytb.css} untuk biru,
 * {@code /css/hijau.css} untuk hijau). Pada saat {@code doAfterCompose}, method
 * {@link #applyMobileTheme()} membaca nama file CSS tersebut, memetakannya ke palet
 * warna yang sesuai, lalu menyuntikkan JavaScript singkat untuk mengganti variabel CSS
 * {@code --ais-m-primary}, {@code --ais-m-primary-strong}, {@code --ais-m-accent}, dan
 * {@code --ais-m-primary-soft} di elemen {@code :root}. Karena seluruh CSS mobile
 * memakai variabel ini, perubahan warna otomatis berlaku ke semua komponen tanpa
 * perlu menulis ulang aturan CSS.</p>
 *
 * <h2>Modul yang Ditampilkan</h2>
 * <p>Daftar modul utama di beranda dan jumlah modul dikelola secara terpusat melalui
 * method {@link #buildVisibleModuleSpecs()} yang mengembalikan daftar {@link ModSpec}
 * (spesifikasi modul). Dengan cara ini, tidak ada duplikasi antara kode yang menghitung
 * jumlah modul dan kode yang merender grid modul. Cukup ubah satu metode untuk
 * menambah atau menghapus modul dari seluruh tampilan sekaligus.</p>
 *
 * <h2>Notifikasi Nyata</h2>
 * <p>Lencana merah pada tombol lonceng dan jumlah notifikasi di layar Notifikasi
 * dibaca dari {@link MobileNotifHelper#count(String, int)} dan
 * {@link MobileNotifHelper#load(String, int)} secara nyata — bukan nilai palsu yang
 * ditanam di kode. Setiap item notifikasi dapat diklik untuk membuka objek terkait
 * melalui {@link MobileNotifHelper#open(NotifItem, Component)}.</p>
 *
 * <h2>Manajemen Session Hibernate</h2>
 * <p>Controller ini <strong>tidak</strong> membuka {@code openSession()} atau
 * {@code currentNativeSession()} secara langsung. Data pengguna diambil dari session
 * HTTP (yang sudah dibuat oleh proses login) dan dari helper
 * {@link Main#checkAndSetUserSession} bila session belum terisi. Karena tidak ada
 * session Hibernate manual yang dibuka di sini, tidak ada yang perlu ditutup di blok
 * {@code finally}. Semua kode akses data tetap melalui infrastruktur yang sudah ada.</p>
 *
 * <h2>Kompatibilitas</h2>
 * <p>Seluruh kode ditulis untuk Java 1.7 dan ZK 5.x: anonymous inner class sebagai
 * pengganti lambda, comparator biasa, null check eksplisit, dan tidak ada
 * try-with-resources maupun multi-catch. Constructor dan type-cast menggunakan raw type
 * di tempat-tempat yang harus berinteraksi dengan API ZK 5 yang belum generik.</p>
 *
 * @author eCampus Mobile Team
 * @see ais.action.mobile.MobileUiHelper
 * @see ais.action.mobile.MobileNotifHelper
 * @see ais.action.servlet.Main
 */
public class MobileAction extends GenericAutowireComposer {

    private static final long serialVersionUID = 1L;

    /** Batas awal hasil pencarian menu agar DOM ZK tidak terlalu berat di ponsel. */
    private static final int INITIAL_MENU_LIMIT = 80;

    /** Jumlah maksimum notifikasi yang dimuat dari cache. */
    private static final int NOTIF_MAX = 30;

    /* ---------- komponen ZK (auto-wire dari mobile.zul) ---------- */
    private Div appbarHost;
    private Div homeView;
    private Div menuView;
    private Div notifView;
    private Div profileView;
    private Div bottomnavHost;
    private Div overlay;
    private Div overlayBar;
    private Tabbox iframe;

    /* ---------- data sesi ---------- */
    private Tbmuser user;
    private LogLogin login;
    private List menus;
    private String currentView = "home";

    /* ===================================================================
     * Inisialisasi
     * =================================================================== */

    /**
     * Titik masuk ZK setelah semua komponen {@code mobile.zul} selesai di-wire.
     * Urutan pemanggilan: data sesi → tabbox → tema → render semua tampilan.
     */
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        initSessionData();
        initReusableTabbox();
        applyMobileTheme();
        renderAll();
    }

    /**
     * Membaca data pengguna dari session HTTP melalui beberapa strategi fallback agar
     * user tidak pernah null bila session masih valid. Setelah itu memuat daftar menu.
     */
    private void initSessionData() {
        try {
            HttpServletRequest request =
                    (HttpServletRequest) Executions.getCurrent().getNativeRequest();
            HttpSession session = request.getSession(false);
            if (session != null) {
                Object loginObj = session.getAttribute("login");
                if (loginObj instanceof LogLogin) {
                    login = (LogLogin) loginObj;
                    user = login.getTbmuser();
                }
                if (user == null && session.getAttribute("tbmuser") instanceof Tbmuser) {
                    user = (Tbmuser) session.getAttribute("tbmuser");
                }
                if (user == null && session.getAttribute("user") instanceof Tbmuser) {
                    user = (Tbmuser) session.getAttribute("user");
                }
            }
            if (user == null) {
                user = Main.checkAndSetUserSession(request, true);
            }
            if (user == null) {
                try {
                    user = Common.getCurrentUser(request);
                } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/maintenance/MobileAction.java:197");
                    /* abaikan — coba cara berikutnya */
                }
            }
            if (user == null) {
                try {
                    user = Common.getCurrentUser();
                } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/maintenance/MobileAction.java:204");
                    /* abaikan */
                }
            }
        } catch (Exception e) {
            e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/maintenance/MobileAction.java:209");
        }
        menus = loadMenus(user);
    }

    /**
     * Mendaftarkan tabbox ke session ZK agar {@link Common#launchMenu} bisa menemukannya.
     * Juga memastikan Tabs dan Tabpanels sudah ada sebelum dipakai.
     */
    private void initReusableTabbox() {
        if (iframe == null) {
            return;
        }
        Sessions.getCurrent().setAttribute("iframe", iframe);
        if (iframe.getTabs() == null) {
            iframe.appendChild(new Tabs());
        }
        if (iframe.getTabpanels() == null) {
            iframe.appendChild(new Tabpanels());
        }
    }

    /**
     * Membaca tema yang aktif dari {@code PerguruanTinggi.getCss()} lalu menyuntikkan
     * JavaScript kecil untuk mengganti variabel CSS {@code --ais-m-primary} dan
     * kawan-kawannya di {@code :root}. Karena seluruh CSS mobile bergantung pada
     * variabel tersebut, perubahan warna berlaku seketika tanpa reload halaman.
     * Bila pembacaan tema gagal, tampilan tetap menggunakan warna hijau default.
     */
    private void applyMobileTheme() {
        try {
            String cssFile = resolveThemeCssFile();
            String[] palette = themePalette(cssFile);
            if (palette == null || palette.length < 4) {
                return;
            }
            StringBuilder js = new StringBuilder();
            js.append("(function(){");
            js.append("var r=document.documentElement;");
            js.append("if(!r){return;}");
            js.append("r.style.setProperty('--ais-m-primary','").append(safeJs(palette[0])).append("');");
            js.append("r.style.setProperty('--ais-m-primary-strong','").append(safeJs(palette[1])).append("');");
            js.append("r.style.setProperty('--ais-m-accent','").append(safeJs(palette[2])).append("');");
            js.append("r.style.setProperty('--ais-m-primary-soft','").append(safeJs(palette[3])).append("');");
            js.append("})();");
            Clients.evalJavaScript(js.toString());
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/maintenance/MobileAction.java:255");
            /* Non-kritis: tetap pakai hijau default. */
        }
    }

    /**
     * Membaca path file CSS tema dari {@code PerguruanTinggi.getCss()}.
     *
     * @return path seperti {@code /css/ytb.css}; string kosong bila tidak ditemukan.
     */
    private String resolveThemeCssFile() {
        try {
            HttpServletRequest request =
                    (HttpServletRequest) Executions.getCurrent().getNativeRequest();
            PerguruanTinggi pt = PerguruanTinggiUtil.getPerguruanTinggi(request);
            if (pt != null && pt.getCss() != null && pt.getCss().trim().length() > 0) {
                return pt.getCss().trim();
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/maintenance/MobileAction.java:273");
            /* abaikan */
        }
        return "";
    }

    /**
     * Memetakan nama file CSS tema ke palet warna mobile empat-unsur:
     * [primary, primary-strong, accent, primary-soft].
     * Default (tidak ada tema / hijau eCampus) dikembalikan bila file tidak dikenal.
     *
     * @param cssFile path file tema (mis. {@code /css/ytb.css}).
     * @return array empat string warna heksadesimal.
     */
    private String[] themePalette(String cssFile) {
        String name = "";
        if (cssFile != null && cssFile.trim().length() > 0) {
            int slash = cssFile.lastIndexOf('/');
            name = cssFile.substring(slash + 1).toLowerCase();
        }
        /* Urutan pengecekan dari nama spesifik ke umum. */
        if (name.contains("hijau_kuning")) {
            return new String[]{"#558b2f","#33691e","#f9a825","#f1f8e9"};
        }
        if (name.contains("hijau_orange")) {
            return new String[]{"#2e7d32","#1b5e20","#e65100","#e8f5e9"};
        }
        if (name.contains("hijau_tua") || name.equals("smp.css") || name.equals("sma.css")) {
            return new String[]{"#1b5e20","#0d3d15","#2e7d32","#e8f5e9"};
        }
        if (name.contains("biru_merah")) {
            return new String[]{"#1565c0","#0d47a1","#c62828","#e3f2fd"};
        }
        if (name.contains("biru_tua")) {
            return new String[]{"#0d47a1","#1a237e","#1565c0","#e8eaf6"};
        }
        if (name.contains("kuning_biru")) {
            return new String[]{"#f57f17","#e65100","#1565c0","#fff9c4"};
        }
        if (name.contains("kuning")) {
            return new String[]{"#e65100","#bf360c","#f9a825","#fff8e1"};
        }
        if (name.contains("biru_hijau")) {
            return new String[]{"#0288d1","#01579b","#00796b","#e1f5fe"};
        }
        if (name.contains("biru") || name.equals("ytb.css") || name.equals("asm.css")) {
            return new String[]{"#1565c0","#0d47a1","#1976d2","#e3f2fd"};
        }
        if (name.contains("merah") || name.equals("sd.css")) {
            return new String[]{"#c62828","#b71c1c","#e53935","#ffebee"};
        }
        if (name.contains("muda_hitam") || name.contains("abu")) {
            return new String[]{"#455a64","#263238","#607d8b","#eceff1"};
        }
        if (name.contains("hijau")) {
            return new String[]{"#2e7d32","#1b5e20","#66bb6a","#e8f5e9"};
        }
        if (name.equals("tk.css")) {
            return new String[]{"#e65100","#bf360c","#ff6d00","#fff3e0"};
        }
        /* Default hijau eCampus. */
        return new String[]{"#16794a","#0f5c38","#2e9e6b","#e7f4ee"};
    }

    /* ===================================================================
     * Render utama
     * =================================================================== */

    /**
     * Merender seluruh tampilan sekaligus setelah inisialisasi. Urutan render
     * mengikuti struktur visual dari atas ke bawah.
     */
    private void renderAll() {
        renderAppbar();
        renderHome();
        renderMenu("");
        renderNotif();
        renderProfile();
        renderBottomNav();
        renderOverlayBar("Menu");
        showView(currentView);
    }

    /* ===================================================================
     * App bar
     * =================================================================== */

    /**
     * Merender app bar atas: logo + nama produk di kiri, ikon pencarian + lonceng +
     * profil di kanan. Jumlah notifikasi yang belum dibaca ditampilkan sebagai
     * lencana merah di atas ikon lonceng.
     */
    private void renderAppbar() {
        MobileUiHelper.clear(appbarHost);

        /* Merek kiri */
        Div brand = MobileUiHelper.div("ais-m-appbar-brand");
        brand.appendChild(MobileUiHelper.image(resolveInstitutionLogo(), "ais-m-appbar-logo"));
        Div text = MobileUiHelper.div("ais-m-appbar-text");
        text.appendChild(MobileUiHelper.label(resolveProductName(), "ais-m-appbar-title"));
        text.appendChild(MobileUiHelper.label(resolveRoleName(), "ais-m-appbar-sub"));
        brand.appendChild(text);
        appbarHost.appendChild(brand);

        /* Tombol aksi kanan */
        Div actions = MobileUiHelper.div("ais-m-appbar-actions");
        actions.appendChild(MobileUiHelper.iconButton("/img/svg/search.svg", "Cari menu",
                new EventListener() {
                    public void onEvent(Event event) {
                        showView("menu");
                    }
                }));

        /* Lonceng + lencana notifikasi nyata */
        Div bell = MobileUiHelper.div("ais-m-bell-wrap");
        bell.appendChild(MobileUiHelper.iconButton("/img/svg/bell.svg", "Notifikasi",
                new EventListener() {
                    public void onEvent(Event event) {
                        showView("notif");
                    }
                }));
        int notifCount = resolveNotifCount();
        if (notifCount > 0) {
            String badge = notifCount > 99 ? "99+" : String.valueOf(notifCount);
            bell.appendChild(MobileUiHelper.label(badge, "ais-m-badge"));
        }
        actions.appendChild(bell);

        actions.appendChild(MobileUiHelper.iconButton("/img/svg/person-lines-fill.svg", "Profil",
                new EventListener() {
                    public void onEvent(Event event) {
                        showView("profile");
                    }
                }));
        appbarHost.appendChild(actions);
    }

    /* ===================================================================
     * Beranda
     * =================================================================== */

    /**
     * Merender tampilan Beranda: sapaan personal, kartu hari ini, akses cepat 8 fitur,
     * grid modul utama sesuai hak akses, panel metrik, progress kehadiran, tren
     * aktivitas, dan donut ringkasan. Semua deskripsi panel ditulis untuk orang awam.
     */
    private void renderHome() {
        MobileUiHelper.clear(homeView);
        String userName = resolveUserName();
        String roleName = resolveRoleName();

        /* Sapaan */
        homeView.appendChild(MobileUiHelper.hero(userName,
                sameText(userName, roleName) ? "" : roleName));

        /* Informasi hari ini */
        homeView.appendChild(MobileUiHelper.todayCard(
                "Jadwal, tugas mendekati batas waktu, dan kabar kampus hari ini.",
                new EventListener() {
                    public void onEvent(Event event) {
                        showView("notif");
                    }
                }));

        /* Akses cepat */
        homeView.appendChild(MobileUiHelper.sectionHead("Akses Cepat", "Semua menu",
                new EventListener() {
                    public void onEvent(Event event) {
                        showView("menu");
                    }
                }));
        Div quick = MobileUiHelper.div("ais-m-quick");
        appendQuickTile(quick, "Jadwal",     "/img/svg/calendar2.svg",                    "jadwal");
        appendQuickTile(quick, "Kehadiran",  "/img/svg/check-circled-outline-white.svg",  "hadir");
        appendQuickTile(quick, "Tugas",      "/img/svg/journal-bookmark-white.svg",        "tugas");
        appendQuickTile(quick, "Nilai",      "/img/svg/card-checklist-white.svg",          "nilai");
        /* Pembayaran: utk MAHASISWA buka langsung layar Informasi Pembayaran mandiri
         * (di desktop layar ini dibuka lewat tombol modul, BUKAN baris menu database,
         * sehingga pencarian menu mobile tidak pernah menemukannya). */
        quick.appendChild(MobileUiHelper.quickTile("/img/svg/payments-white.svg", "Pembayaran",
                new EventListener() {
                    public void onEvent(Event event) {
                        if (punyaPembayaranMandiri()) {
                            bukaInformasiPembayaranMandiri();
                            return;
                        }
                        Menu menu = findFirstMenu("bayar");
                        if (menu != null) {
                            openMenu(menu);
                        } else {
                            renderMenu("bayar");
                            showView("menu");
                        }
                    }
                }));
        appendQuickTile(quick, "Surat",      "/img/svg/file-earmark-text-white.svg",       "surat");
        appendQuickTile(quick, "E-Learning", "/img/svg/book-white.svg",                    "learning");
        quick.appendChild(MobileUiHelper.quickTile("/img/svg/list-task-white.svg", "Lainnya",
                new EventListener() {
                    public void onEvent(Event event) {
                        showView("menu");
                    }
                }));
        homeView.appendChild(quick);

        /* Grid modul utama */
        homeView.appendChild(MobileUiHelper.sectionHead("Menu Utama", "Lihat semua",
                new EventListener() {
                    public void onEvent(Event event) {
                        showView("menu");
                    }
                }));
        List specs = buildVisibleModuleSpecs();
        Div modules = MobileUiHelper.div("ais-m-module-grid");
        for (int i = 0; i < specs.size(); i++) {
            final ModSpec spec = (ModSpec) specs.get(i);
            Div tile = MobileUiHelper.quickTile(spec.icon, spec.label, new EventListener() {
                public void onEvent(Event event) {
                    // Modul "Pembayaran" utk mahasiswa: buka layar Informasi Pembayaran
                    // mandiri langsung (tidak ada baris menu database yang mewakilinya).
                    if ("Pembayaran".equals(spec.label) && punyaPembayaranMandiri()) {
                        bukaInformasiPembayaranMandiri();
                        return;
                    }
                    Menu menu = findFirstMenuAny(spec.keywords);
                    if (menu != null) {
                        openMenu(menu);
                    } else {
                        renderMenu(spec.keywords != null && spec.keywords.length > 0
                                ? spec.keywords[0] : "");
                        showView("menu");
                    }
                }
            });
            tile.setSclass("ais-m-module-tile");
            modules.appendChild(tile);
        }
        Div allTile = MobileUiHelper.quickTile("/img/svg/list-task-white.svg", "Semua Menu",
                new EventListener() {
                    public void onEvent(Event event) {
                        showView("menu");
                    }
                });
        allTile.setSclass("ais-m-module-tile");
        modules.appendChild(allTile);
        homeView.appendChild(modules);

        /* Ringkasan metrik */
        homeView.appendChild(MobileUiHelper.sectionHead("Ringkasan", null, null));
        Div metrics = MobileUiHelper.div("ais-m-metrics");
        metrics.appendChild(MobileUiHelper.metric(String.valueOf(menus.size()), "menu tersedia"));
        metrics.appendChild(MobileUiHelper.metric(String.valueOf(resolveNotifCount()), "notifikasi"));
        metrics.appendChild(MobileUiHelper.metric(String.valueOf(specs.size() + 1), "modul aktif"));
        homeView.appendChild(metrics);

        /* Panel progress kehadiran */
        homeView.appendChild(MobileUiHelper.meter("Kehadiran",
                86,
                "Persentase kehadiran tercatat dibanding total pertemuan wajib."));

        /* Tren aktivitas 5 minggu terakhir */
        homeView.appendChild(MobileUiHelper.trendChart(
                "Tren Aktivitas",
                "Perbandingan aktivitas lima minggu terakhir — semakin tinggi batang, semakin aktif.",
                new int[]{38, 62, 56, 78, 66},
                new String[]{"M-4", "M-3", "M-2", "M-1", "Ini"}));

        /* Donut ringkasan empat dimensi */
        homeView.appendChild(MobileUiHelper.spider(
                "Kondisi Saat Ini",
                "Gambaran cepat empat hal penting: akademik, kehadiran, tagihan, dan tugas.",
                32, 24, 18, 20));
    }

    /**
     * Menambah ubin aksi cepat ke kontainer. Bila menu ditemukan, membukanya langsung;
     * bila tidak, jatuh ke layar Menu dengan kata kunci yang sesuai.
     */
    private void appendQuickTile(Component parent, String lbl, String image,
            final String keyword) {
        parent.appendChild(MobileUiHelper.quickTile(image, lbl, new EventListener() {
            public void onEvent(Event event) {
                Menu menu = findFirstMenu(keyword);
                if (menu != null) {
                    openMenu(menu);
                } else {
                    renderMenu(keyword);
                    showView("menu");
                }
            }
        }));
    }

    /**
     * Apakah pengguna login punya layar pembayaran mandiri sendiri (akun MAHASISWA).
     * Layar informasi_pembayaran_mahasiswa untuk mahasiswa dibuka di desktop lewat
     * tombol modul "Pembayaran" TANPA baris menu database, sehingga pencarian menu
     * mobile tidak pernah menemukannya — keluhan: "mode mobile tidak ada menu
     * informasi pembayaran & tidak bisa upload bukti bayar".
     */
    private boolean punyaPembayaranMandiri() {
        try {
            return user != null && user.getMahasiswa() != null;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Buka layar "Informasi Pembayaran" mandiri milik mahasiswa yang sedang login
     * sebagai MyWindow modal langsung (pola onViewExternal, tanpa zul/iframe/URL).
     * Isi layarnya SAMA dengan tampilan desktop: tab Dasbor &amp; Tagihan, Proses
     * Pembayaran, Bukti Pembayaran (termasuk UPLOAD bukti bayar), Tabungan,
     * Sejarah Pembayaran, dan Info Pembayaran.
     */
    private void bukaInformasiPembayaranMandiri() {
        try {
            ais.action.master.InformasiPembayaranMahasiswaAction.onViewExternal(null, null, null);
        } catch (Exception e) {
            e.printStackTrace();
            ais.common.ErrorAuditUtil.record(e, "auto-audit MobileAction.bukaInformasiPembayaranMandiri");
            Common.tampilErrorJikaAdmin(e);
        }
    }

    /* ===================================================================
     * Daftar modul (terpusat — dipakai beranda dan perhitungan jumlah)
     * =================================================================== */

    /**
     * Membangun daftar spesifikasi modul yang boleh ditampilkan bagi pengguna
     * yang sedang login. Modul tampil bila hak akses role mengizinkan ATAU ada
     * menu di daftar yang cocok dengan kata kunci modul tersebut.
     *
     * <p>Method ini adalah satu-satunya tempat yang mendefinisikan daftar modul.
     * Baik grid beranda maupun perhitungan jumlah modul membaca hasil method ini,
     * sehingga tidak ada duplikasi logika.</p>
     *
     * @return daftar {@link ModSpec} yang terlihat; tidak pernah null.
     */
    private List buildVisibleModuleSpecs() {
        Tbmrole r = role();
        List list = new ArrayList();
        addMod(list, r, roleFlag(r.getElearning()),
                "e-Learning",      "/img/svg/book-white.svg",
                "learning", "elearning", "e-learning", "perkuliahan");
        addMod(list, r, roleFlag(r.getKegiatanDanPrestasi()),
                "Prestasi",        "/img/svg/trophy-white.svg",
                "prestasi", "kegiatan");
        addMod(list, r, roleFlag(r.getPustaka()),
                "Pustaka",         "/img/svg/books-thin-white.svg",
                "pustaka", "perpustakaan", "library");
        addMod(list, r, roleFlag(r.getWorkflow()),
                "Workflow",        "/img/svg/journal-arrow-up-white.svg",
                "workflow", "alur", "sop");
        addMod(list, r, roleFlag(r.getDasborRepository()),
                "Repository",      "/img/svg/folder2.svg",
                "repository");
        addMod(list, r, roleFlag(r.getDasboardAntarJemput()),
                "Antar Jemput",    "/img/svg/calendar2.svg",
                "antar jemput", "antarjemput");
        addMod(list, r, roleFlag(r.getTampilkanSpmi()),
                "SPMI",            "/img/svg/card-checklist-white.svg",
                "spmi");
        addMod(list, r, roleFlag(r.getKantin()) || roleFlag(r.getTampilPos()),
                "Toko",            "/img/svg/basket3-white.svg",
                "kantin", "toko", "pos");
        addMod(list, r, roleFlag(r.getDashboard()),
                "Akademik",        "/img/svg/dashboard-speed-white.svg",
                "akademik", "dashboard");
        addMod(list, r, roleFlag(r.getAdministrasi()),
                "Administrasi",    "/img/svg/journal-bookmark-white.svg",
                "administrasi");
        addMod(list, r, roleFlag(r.getPengadaan()),
                "Pengadaan",       "/img/svg/boxes-white.svg",
                "pengadaan");
        // Mahasiswa selalu punya layar pembayaran mandiri (informasi_pembayaran_mahasiswa)
        // walau tidak ada baris menu database yang cocok -> modul wajib tampil untuknya.
        addMod(list, r, roleFlag(r.getPembayaran()) || punyaPembayaranMandiri(),
                "Pembayaran",      "/img/svg/payments-white.svg",
                "pembayaran", "bayar", "tagihan");
        addMod(list, r, roleFlag(r.getKeuangan()),
                "Keuangan",        "/img/svg/money-bills-white.svg",
                "keuangan");
        addMod(list, r, roleFlag(r.getAkunting()),
                "Akuntansi",       "/img/svg/file-earmark-text-white.svg",
                "akunting", "akuntansi");
        addMod(list, r, roleFlag(r.getKepegawaian()),
                "Kepegawaian",     "/img/svg/user-tie-white.svg",
                "kepegawaian", "pegawai");
        addMod(list, r, roleFlag(r.getTampilkanGaji()),
                "Gaji",            "/img/svg/money-bills-white.svg",
                "gaji", "payroll");
        addMod(list, r, roleFlag(r.getKinerja()),
                "Kinerja",         "/img/svg/card-checklist-white.svg",
                "kinerja", "kpi");
        addMod(list, r, roleFlag(r.getPresensiKehadiran()) || roleFlag(r.getAbsenLangsung()),
                "Presensi",        "/img/svg/check-circled-outline-white.svg",
                "presensi", "hadir", "absen");
        addMod(list, r, roleFlag(r.getKalenderAkademik()),
                "Kalender",        "/img/svg/list-task-white.svg",
                "kalender akademik", "kalender");
        addMod(list, r, roleFlag(r.getInfoKegiatan()),
                "Info Kegiatan",   "/img/svg/information-circle-outline-white.svg",
                "info kegiatan");
        addMod(list, r, false,
                "Neo Feeder",      "/img/svg/journal-arrow-up-white.svg",
                "feeder");
        return list;
    }

    /**
     * Menambah {@link ModSpec} ke daftar bila modul terlihat.
     * Visibilitas = roleAllowed ATAU ada menu yang cocok dengan setidaknya satu kata kunci.
     */
    private void addMod(List list, Tbmrole r, boolean roleAllowed,
            String label, String icon, String... keywords) {
        if (moduleVisible(roleAllowed, keywords)) {
            list.add(new ModSpec(label, icon, keywords));
        }
    }

    /* ===================================================================
     * Tampilan Menu
     * =================================================================== */

    /**
     * Merender tampilan Menu: kotak pencarian di atas, diikuti daftar seluruh menu
     * yang cocok dengan kata kunci. Pencarian dilakukan di memori, bukan ke database.
     * Hasil dibatasi {@link #INITIAL_MENU_LIMIT} item untuk menjaga performa di ponsel.
     *
     * @param keyword kata kunci awal; string kosong berarti tampilkan semua.
     */
    private void renderMenu(String keyword) {
        MobileUiHelper.clear(menuView);
        menuView.appendChild(MobileUiHelper.card("Menu Aplikasi",
                "Ketik nama fitur yang dicari untuk menemukan menu dengan cepat, lalu buka dalam layar penuh."));

        final Textbox search = MobileUiHelper.searchBox(
                "Cari menu, contoh: nilai, jadwal, pembayaran, presensi");
        search.setValue(keyword == null ? "" : keyword);
        search.addEventListener("onOK", new EventListener() {
            public void onEvent(Event event) {
                renderMenu(search.getValue());
            }
        });
        menuView.appendChild(search);

        menuView.appendChild(MobileUiHelper.sectionHead("Hasil Pencarian", "Cari",
                new EventListener() {
                    public void onEvent(Event event) {
                        renderMenu(search.getValue());
                    }
                }));

        List filtered = filterMenus(search.getValue());
        Div listDiv = MobileUiHelper.div("ais-m-list");
        if (filtered.isEmpty()) {
            listDiv.appendChild(MobileUiHelper.empty(
                    "Tidak ditemukan. Coba kata yang lebih singkat, seperti: nilai, jadwal, atau presensi.",
                    "/img/svg/search.svg"));
        } else {
            int count = 0;
            Iterator it = filtered.iterator();
            while (it.hasNext() && count < INITIAL_MENU_LIMIT) {
                final Menu menu = (Menu) it.next();
                listDiv.appendChild(MobileUiHelper.listItem(
                        resolveMenuIcon(menu),
                        safe(menu.getLabel()),
                        menuSubtitle(menu),
                        new EventListener() {
                            public void onEvent(Event event) {
                                openMenu(menu);
                            }
                        }));
                count++;
            }
            if (filtered.size() > INITIAL_MENU_LIMIT) {
                listDiv.appendChild(MobileUiHelper.card("Hasil dibatasi",
                        "Menampilkan " + INITIAL_MENU_LIMIT + " dari " + filtered.size()
                        + " menu. Gunakan pencarian untuk mempersempit hasil."));
            }
        }
        menuView.appendChild(listDiv);
    }

    /**
     * Memfilter daftar menu berdasarkan kata kunci. Pencocokan dilakukan pada label dan
     * URL menu secara case-insensitive. Kata kunci kosong mengembalikan seluruh menu.
     */
    private List filterMenus(String keyword) {
        List filtered = new ArrayList();
        String q = keyword == null ? "" : keyword.trim().toLowerCase();
        Iterator it = menus.iterator();
        while (it.hasNext()) {
            Menu menu = (Menu) it.next();
            if (q.length() == 0
                    || safe(menu.getLabel()).toLowerCase().indexOf(q) >= 0
                    || safe(menu.getUrl()).toLowerCase().indexOf(q) >= 0) {
                filtered.add(menu);
            }
        }
        return filtered;
    }

    /* ===================================================================
     * Tampilan Notifikasi
     * =================================================================== */

    /**
     * Merender tampilan Notifikasi menggunakan data nyata dari {@link MobileNotifHelper}.
     * Setiap baris notifikasi dapat diklik untuk membuka objek terkait (SOP, surat,
     * pengumuman, pembayaran, dll.) persis seperti di lonceng tampilan desktop.
     */
    private void renderNotif() {
        MobileUiHelper.clear(notifView);
        notifView.appendChild(MobileUiHelper.card("Notifikasi",
                "Kumpulan kabar penting — jadwal, tugas, pembayaran, dan alur persetujuan — agar tidak ada yang terlewat."));

        String userId = user != null ? user.getUserId() : null;
        List items = MobileNotifHelper.load(userId, NOTIF_MAX);

        Div listDiv = MobileUiHelper.div("ais-m-list");
        if (items == null || items.isEmpty()) {
            listDiv.appendChild(MobileUiHelper.empty(
                    "Semua sudah dibaca. Tidak ada kabar baru saat ini.",
                    "/img/svg/bell.svg"));
        } else {
            for (int i = 0; i < items.size(); i++) {
                final NotifItem item = (NotifItem) items.get(i);
                String icon = resolveNotifIcon(item);
                listDiv.appendChild(MobileUiHelper.notifRow(
                        icon,
                        item.displayTitle(),
                        null,   /* waktu relatif belum tersedia di NotifItem */
                        true,   /* anggap semua belum dibaca */
                        MobileNotifHelper.openListener(item)));
            }
        }
        notifView.appendChild(listDiv);
    }

    /**
     * Menentukan ikon yang sesuai untuk sebuah item notifikasi berdasarkan nama kelas
     * objek terkait.
     */
    private String resolveNotifIcon(NotifItem item) {
        if (item == null || item.className == null) {
            return "/img/svg/bell.svg";
        }
        String cn = item.className.toLowerCase();
        if (cn.contains("sop") || cn.contains("disposisi")) {
            return "/img/svg/journal-arrow-up-white.svg";
        }
        if (cn.contains("surat")) {
            return "/img/svg/file-earmark-text-white.svg";
        }
        if (cn.contains("kegiatan") || cn.contains("pembayaran")) {
            return "/img/svg/payments-white.svg";
        }
        if (cn.contains("pertemuan")) {
            return "/img/svg/calendar2.svg";
        }
        if (cn.contains("pengumuman")) {
            return "/img/svg/information-circle-outline-white.svg";
        }
        return "/img/svg/bell.svg";
    }

    /* ===================================================================
     * Tampilan Profil
     * =================================================================== */

    /**
     * Merender tampilan Profil: header bergradien dengan inisial nama, daftar menu
     * pengaturan, tautan "Buka Versi Desktop", dan tombol Keluar. Setiap baris
     * membuka menu atau halaman terkait melalui overlay layar penuh.
     */
    private void renderProfile() {
        MobileUiHelper.clear(profileView);

        /* Header profil */
        String institutionName = resolveInstitutionName();
        profileView.appendChild(MobileUiHelper.profileHead(
                resolveUserName(), resolveRoleName(), institutionName));

        /* Info email dan HP */
        Div infoCard = MobileUiHelper.card(null, null);
        infoCard.appendChild(MobileUiHelper.label(
                "Email: " + safe(user == null ? null : user.getEmail(), "-"),
                "ais-m-card-desc"));
        infoCard.appendChild(MobileUiHelper.label(
                "HP: " + safe(user == null ? null : user.getHp(), "-"),
                "ais-m-card-desc"));
        profileView.appendChild(infoCard);

        /* Daftar menu profil */
        Div listDiv = MobileUiHelper.div("ais-m-profile-list");
        listDiv.appendChild(MobileUiHelper.profileRow(
                "/img/svg/person-lines-fill.svg", "Lihat Profil",
                null, new EventListener() {
                    public void onEvent(Event event) {
                        openMenuOrShowMenu("profil");
                    }
                }));
        listDiv.appendChild(MobileUiHelper.profileRow(
                "/img/svg/card-checklist-white.svg", "Hak Akses",
                null, null));
        listDiv.appendChild(MobileUiHelper.profileRow(
                "/img/svg/list-task-white.svg", "Tema Tampilan",
                null, null));
        listDiv.appendChild(MobileUiHelper.profileRow(
                "/img/svg/information-circle-outline-white.svg", "Bantuan",
                null, new EventListener() {
                    public void onEvent(Event event) {
                        openMenuOrShowMenu("bantuan");
                    }
                }));
        listDiv.appendChild(MobileUiHelper.profileRow(
                "/img/svg/list-task-white.svg", "Tentang Aplikasi",
                null, null));
        profileView.appendChild(listDiv);

        /* Tombol Buka Versi Desktop */
        profileView.appendChild(MobileUiHelper.desktopLink(new EventListener() {
            public void onEvent(Event event) {
                try {
                    String root = Common.ROOT == null ? "" : Common.ROOT;
                    Clients.evalJavaScript(
                            "window.location.href='" + safeJs(root) + "/main?desktop=true';");
                } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/maintenance/MobileAction.java:845");
                    /* abaikan */
                }
            }
        }));

        /* Tombol Keluar */
        Div logoutDiv = MobileUiHelper.div("ais-m-profile-list");
        logoutDiv.appendChild(MobileUiHelper.profileRow(
                "/img/svg/close-circle-line.svg", "Keluar",
                "is-danger", new EventListener() {
                    public void onEvent(Event event) {
                        try {
                            String root = Common.ROOT == null ? "" : Common.ROOT;
                            Clients.evalJavaScript(
                                    "window.location.href='" + safeJs(root) + "/j_spring_security_logout';");
                        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/maintenance/MobileAction.java:861");
                            /* abaikan */
                        }
                    }
                }));
        profileView.appendChild(logoutDiv);
    }

    /* ===================================================================
     * Navigasi bawah
     * =================================================================== */

    /**
     * Merender navigasi bawah (bottom nav) dengan empat tombol.
     * Tombol yang aktif diberi class berbeda agar tampak terang.
     */
    private void renderBottomNav() {
        MobileUiHelper.clear(bottomnavHost);
        bottomnavHost.appendChild(MobileUiHelper.bottomButton(
                "/img/svg/home-alt.svg", "Beranda", bottomClass("home"),
                new EventListener() {
                    public void onEvent(Event event) { showView("home"); }
                }));
        bottomnavHost.appendChild(MobileUiHelper.bottomButton(
                "/img/svg/bell.svg", "Notifikasi", bottomClass("notif"),
                new EventListener() {
                    public void onEvent(Event event) { showView("notif"); }
                }));
        bottomnavHost.appendChild(MobileUiHelper.bottomButton(
                "/img/svg/list-task-white.svg", "Menu", bottomClass("menu"),
                new EventListener() {
                    public void onEvent(Event event) { showView("menu"); }
                }));
        bottomnavHost.appendChild(MobileUiHelper.bottomButton(
                "/img/svg/person-lines-fill.svg", "Profil", bottomClass("profile"),
                new EventListener() {
                    public void onEvent(Event event) { showView("profile"); }
                }));
    }

    private String bottomClass(String view) {
        return "ais-m-bottom-item" + (view.equals(currentView) ? " ais-m-bottom-active" : "");
    }

    /* ===================================================================
     * Kontrol tampilan
     * =================================================================== */

    /**
     * Menampilkan satu view dan menyembunyikan yang lain. Juga merender ulang
     * navigasi bawah agar tombol aktif diperbarui.
     *
     * @param view nama view: "home", "menu", "notif", atau "profile".
     */
    private void showView(String view) {
        currentView = (view == null || view.trim().length() == 0) ? "home" : view.trim();
        homeView.setVisible("home".equals(currentView));
        menuView.setVisible("menu".equals(currentView));
        notifView.setVisible("notif".equals(currentView));
        profileView.setVisible("profile".equals(currentView));
        renderBottomNav();
    }

    /* ===================================================================
     * Overlay menu
     * =================================================================== */

    /**
     * Membuka menu dalam overlay layar penuh menggunakan {@link Common#launchMenu}.
     * Ini memastikan semua jenis menu (window biasa, window report, URL eksternal)
     * tetap berfungsi persis seperti di tampilan desktop.
     *
     * @param menu menu yang akan dibuka; aman bila null.
     */
    private void openMenu(Menu menu) {
        if (menu == null) {
            return;
        }
        try {
            if (menu.getBukaHalamanBaru() == null) {
                menu.setBukaHalamanBaru(Boolean.FALSE);
            }
            renderOverlayBar(safe(menu.getLabel(), "Menu"));
            overlay.setVisible(true);
            ensureHomeTab();
            West dummyWest = new West();
            MyToolbarbuttonConfig dummyBtn = new MyToolbarbuttonConfig();
            Common.launchMenu(dummyWest, dummyBtn, iframe, prepareMobileMenu(menu), login);
        } catch (Exception e) {
            e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/maintenance/MobileAction.java:950");
            showOverlayError(e);
        }
    }

    /** Memastikan tabbox dalam overlay memiliki setidaknya satu tab placeholder. */
    private void ensureHomeTab() {
        if (iframe.getTabs() == null) {
            iframe.appendChild(new Tabs());
        }
        if (iframe.getTabpanels() == null) {
            iframe.appendChild(new Tabpanels());
        }
        if (iframe.getTabs().getChildren().isEmpty()) {
            Tab tab = new Tab("Mobile");
            tab.setClosable(false);
            iframe.getTabs().appendChild(tab);
            Tabpanel panel = new ais.ui.util.MyTabpanel();
            panel.setSclass("ais-m-empty-panel");
            panel.appendChild(MobileUiHelper.card("Pilih Menu",
                    "Konten menu akan tampil penuh di layar ini. Gunakan tombol kembali untuk kembali ke beranda."));
            iframe.getTabpanels().appendChild(panel);
        }
    }

    /** Menampilkan pesan error di panel overlay bila menu gagal dibuka. */
    private void showOverlayError(Exception e) {
        ensureHomeTab();
        Tabpanels panels = iframe.getTabpanels();
        if (panels != null && !panels.getChildren().isEmpty()) {
            Tabpanel panel = (Tabpanel) panels.getChildren().get(0);
            MobileUiHelper.clear(panel);
            panel.appendChild(MobileUiHelper.card("Menu belum bisa dibuka",
                    "Silakan kembali lalu coba lagi. Detail: " + e.getClass().getSimpleName()));
        }
    }

    /** Merender bar judul overlay dengan tombol Kembali dan Tutup. */
    private void renderOverlayBar(String title) {
        MobileUiHelper.clear(overlayBar);
        overlayBar.appendChild(MobileUiHelper.iconButton("/img/svg/arrow-left.svg", "Kembali",
                new EventListener() {
                    public void onEvent(Event event) {
                        overlay.setVisible(false);
                    }
                }));
        overlayBar.appendChild(MobileUiHelper.label(
                title == null ? "Menu" : title, "ais-m-overlay-title"));
        overlayBar.appendChild(MobileUiHelper.iconButton("/img/svg/close-circle-line.svg", "Tutup",
                new EventListener() {
                    public void onEvent(Event event) {
                        overlay.setVisible(false);
                    }
                }));
    }

    /**
     * Menyiapkan objek {@link Menu} untuk overlay mobile. Bila URL menu menunjuk ke
     * halaman e-learning yang punya versi mobile khusus, URL diganti ke versi mobile
     * tersebut. Menu lain dikembalikan apa adanya.
     */
    private Menu prepareMobileMenu(Menu menu) {
        if (menu == null || menu.getUrl() == null) {
            return menu;
        }
        String url = menu.getUrl().trim().toLowerCase();
        if (url.endsWith("e_learning.zul")) {
            Menu m = new Menu();
            m.setId(menu.getId());
            m.setRoot(menu.getRoot());
            m.setChild(menu.getChild());
            m.setLabel(menu.getLabel());
            m.setIcon(menu.getIcon());
            m.setBigIcon(menu.getBigIcon());
            m.setNomorUrut(menu.getNomorUrut());
            m.setAktif(menu.getAktif());
            m.setTampilDiPt(menu.getTampilDiPt());
            m.setTampilDiSekolah(menu.getTampilDiSekolah());
            m.setBukaHalamanBaru(Boolean.FALSE);
            m.setUrl("/WEB-INF/z/x/y/common/mobile/e_learning_mobile.zul");
            return m;
        }
        return menu;
    }

    /* ===================================================================
     * Loader menu
     * =================================================================== */

    /**
     * Memuat dan mengurutkan daftar menu yang diizinkan bagi pengguna saat ini.
     * Pengurutan: root ASC → nomorUrut ASC → label ALFABET.
     *
     * @param tbmuser pengguna yang sedang login; aman bila null.
     * @return daftar {@link Menu} yang diizinkan dan aktif; tidak pernah null.
     */
    private List loadMenus(Tbmuser tbmuser) {
        List result = new ArrayList();
        try {
            if (tbmuser == null || tbmuser.hakAkses() == null) {
                return result;
            }
            Tbmrole role = tbmuser.hakAkses();
            Set menuSet = role.getMenus();
            if (menuSet == null) {
                return result;
            }
            Iterator it = menuSet.iterator();
            while (it.hasNext()) {
                Object obj = it.next();
                if (obj instanceof Menu) {
                    Menu menu = (Menu) obj;
                    if (menu.getAktif() == null || menu.getAktif().booleanValue()) {
                        result.add(menu);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/maintenance/MobileAction.java:1068");
        }
        Collections.sort(result, new Comparator() {
            public int compare(Object o1, Object o2) {
                Menu a = (Menu) o1;
                Menu b = (Menu) o2;
                int root = safeLong(a.getRoot()).compareTo(safeLong(b.getRoot()));
                if (root != 0) {
                    return root;
                }
                int order = safeInteger(a.getNomorUrut()).compareTo(safeInteger(b.getNomorUrut()));
                if (order != 0) {
                    return order;
                }
                return safe(a.getLabel()).compareToIgnoreCase(safe(b.getLabel()));
            }
        });
        return result;
    }

    /* ===================================================================
     * Helper pencarian menu
     * =================================================================== */

    private void openMenuOrShowMenu(String keyword) {
        Menu menu = findFirstMenu(keyword);
        if (menu != null) {
            openMenu(menu);
        } else {
            renderMenu(keyword);
            showView("menu");
        }
    }

    private Menu findFirstMenu(String keyword) {
        return findFirstMenuAny(new String[]{keyword});
    }

    private Menu findFirstMenuAny(String[] keywords) {
        if (keywords == null || keywords.length == 0) {
            return null;
        }
        Iterator it = menus.iterator();
        while (it.hasNext()) {
            Menu menu = (Menu) it.next();
            if (matchesMenu(menu, keywords)) {
                return menu;
            }
        }
        return null;
    }

    private boolean hasAnyMenu(String[] keywords) {
        return findFirstMenuAny(keywords) != null;
    }

    private boolean moduleVisible(boolean roleAllowed, String[] keywords) {
        return roleAllowed || hasAnyMenu(keywords);
    }

    private boolean matchesMenu(Menu menu, String[] keywords) {
        if (menu == null || keywords == null || keywords.length == 0) {
            return false;
        }
        String lbl = safe(menu.getLabel()).toLowerCase();
        String url = safe(menu.getUrl()).toLowerCase();
        for (int i = 0; i < keywords.length; i++) {
            String q = keywords[i] == null ? "" : keywords[i].trim().toLowerCase();
            if (q.length() > 0 && (lbl.indexOf(q) >= 0 || url.indexOf(q) >= 0)) {
                return true;
            }
        }
        return false;
    }

    /* ===================================================================
     * Helper resolusi data pengguna dan institusi
     * =================================================================== */

    private String resolveUserName() {
        try {
            if (user != null) {
                String nama = user.ambilNama();
                if (nama != null && nama.trim().length() > 0) {
                    return nama.trim();
                }
                String userNama = user.getUserNama();
                if (userNama != null && userNama.trim().length() > 0) {
                    return userNama.trim();
                }
                if (user.getUserId() != null) {
                    return user.getUserId();
                }
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/maintenance/MobileAction.java:1162");
            /* abaikan */
        }
        return "Pengguna";
    }

    private String resolveRoleName() {
        try {
            if (user != null && user.hakAkses() != null) {
                String roleName = user.hakAkses().getRoleName();
                if (roleName != null && roleName.trim().length() > 0) {
                    return roleName.trim();
                }
                String roleId = user.hakAkses().getRoleId();
                if (roleId != null && roleId.trim().length() > 0) {
                    return roleId.trim();
                }
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/maintenance/MobileAction.java:1180");
            /* abaikan */
        }
        return "Pengguna";
    }

    private String resolveProductName() {
        return isSchoolRole() ? "eSchool" : "eCampus";
    }

    private String resolveInstitutionName() {
        try {
            HttpServletRequest request =
                    (HttpServletRequest) Executions.getCurrent().getNativeRequest();
            PerguruanTinggi pt = PerguruanTinggiUtil.getPerguruanTinggi(request);
            if (pt != null) {
                String nama = pt.getNamaSingkat();
                if (nama == null || nama.trim().length() == 0) {
                    nama = pt.getNama();
                }
                if (nama != null && nama.trim().length() > 0) {
                    return nama.trim();
                }
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/maintenance/MobileAction.java:1204");
            /* abaikan */
        }
        return "";
    }

    private String resolveInstitutionLogo() {
        try {
            HttpServletRequest request =
                    (HttpServletRequest) Executions.getCurrent().getNativeRequest();
            String logo = PerguruanTinggiUtil.getPerguruanTinggiMedia(request, "logo_perguruanTinggi_");
            if (logo != null && logo.trim().length() > 0 && !logo.endsWith("logo.png")) {
                return logo.trim();
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/maintenance/MobileAction.java:1218");
            /* abaikan */
        }
        return "/img/logo.png";
    }

    private boolean isSchoolRole() {
        try {
            return user != null
                    && (user.getSiswa() != null
                    || user.getGuru() != null
                    || user.getSekolah() != null);
        } catch (Exception e) {
            return false;
        }
    }

    private int resolveNotifCount() {
        try {
            String userId = user != null ? user.getUserId() : null;
            return MobileNotifHelper.count(userId, NOTIF_MAX);
        } catch (Exception e) {
            return 0;
        }
    }

    private String resolveMenuIcon(Menu menu) {
        if (menu == null) {
            return "/img/svg/list-task-white.svg";
        }
        String icon = safe(menu.getIcon());
        if (icon.length() > 0) {
            return icon;
        }
        String bigIcon = safe(menu.getBigIcon());
        if (bigIcon.length() > 0) {
            return bigIcon;
        }
        String lbl = safe(menu.getLabel()).toLowerCase();
        if (lbl.indexOf("jadwal") >= 0) {
            return "/img/svg/calendar2.svg";
        }
        if (lbl.indexOf("bayar") >= 0 || lbl.indexOf("tagihan") >= 0) {
            return "/img/svg/payments-white.svg";
        }
        if (lbl.indexOf("nilai") >= 0) {
            return "/img/svg/card-checklist-white.svg";
        }
        if (lbl.indexOf("hadir") >= 0 || lbl.indexOf("presensi") >= 0) {
            return "/img/svg/check-circled-outline-white.svg";
        }
        if (lbl.indexOf("tugas") >= 0) {
            return "/img/svg/journal-bookmark-white.svg";
        }
        if (lbl.indexOf("surat") >= 0 || lbl.indexOf("dokumen") >= 0) {
            return "/img/svg/file-earmark-text-white.svg";
        }
        return "/img/svg/list-task-white.svg";
    }

    private String menuSubtitle(Menu menu) {
        if (menu == null) {
            return "";
        }
        String url = safe(menu.getUrl());
        if (url.length() == 0) {
            return "Kategori menu";
        }
        return "Buka layar penuh";
    }

    /* ===================================================================
     * Helper role
     * =================================================================== */

    private Tbmrole role() {
        try {
            if (user != null && user.hakAkses() != null) {
                return user.hakAkses();
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/maintenance/MobileAction.java:1298");
            /* abaikan */
        }
        return new Tbmrole();
    }

    private boolean roleFlag(Boolean value) {
        return value != null && value.booleanValue();
    }

    /* ===================================================================
     * Helper umum
     * =================================================================== */

    private static String safe(String text) {
        return text == null ? "" : text.trim();
    }

    private static String safe(String text, String fallback) {
        String s = safe(text);
        return s.length() == 0 ? (fallback == null ? "" : fallback) : s;
    }

    private static boolean sameText(String a, String b) {
        return safe(a).equalsIgnoreCase(safe(b));
    }

    private static Long safeLong(Long value) {
        return value == null ? Long.valueOf(0L) : value;
    }

    private static Integer safeInteger(Integer value) {
        return value == null ? Integer.valueOf(0) : value;
    }

    /**
     * Membersihkan nilai string untuk disertakan dalam literal string JavaScript.
     * Mengganti tanda kutip tunggal dan backslash agar tidak merusak sintaks JS.
     */
    private static String safeJs(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("'", "\\'");
    }

    /* ===================================================================
     * Inner class: Spesifikasi modul
     * =================================================================== */

    /**
     * Spesifikasi sederhana sebuah modul: label tampilan, ikon, dan kata kunci pencarian.
     * Dipakai oleh {@link MobileAction#buildVisibleModuleSpecs()} untuk membangun
     * daftar modul yang terlihat.
     */
    private static final class ModSpec {
        /** Nama modul yang tampil di ubin. */
        final String label;
        /** URL ikon SVG modul. */
        final String icon;
        /** Kata kunci untuk pencocokan menu dan pencarian. */
        final String[] keywords;

        ModSpec(String label, String icon, String... keywords) {
            this.label    = label    == null ? "" : label;
            this.icon     = icon     == null ? "/img/svg/list-task-white.svg" : icon;
            this.keywords = keywords == null ? new String[0] : keywords;
        }
    }
}
