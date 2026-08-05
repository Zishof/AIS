package ais.ui.util;

import org.zkoss.zk.ui.Component;
import org.zkoss.zul.Div;
import org.zkoss.zul.Html;
import org.zkoss.zul.Panelchildren;

/**
 * Basis portal dasbor terpusat — satu titik pengelola MyPortallayout untuk
 * semua halaman dasbor/statistik aplikasi.
 *
 * <p><b>Pola 1 — dasbor baru (extends BaseDasbordPortal):</b><br>
 * Buat kelas yang extends {@code BaseDasbordPortal} lalu pakai
 * {@link #kolom(String)}, {@link #panel(MyPortalchildren, String, String)}, dan
 * {@link #appendHtml(Component, String)} untuk membangun konten.</p>
 *
 * <pre>
 *   public class DasboardSaya extends BaseDasbordPortal {
 *       public DasboardSaya() {
 *           MyPortalchildren k1 = kolom("50%");
 *           MyPortalchildren k2 = kolom("50%");
 *           Component h1 = panel(k1, "Panel A", "Ringkasan data A.");
 *           appendHtml(h1, "...");
 *       }
 *   }
 * </pre>
 *
 * <p><b>Pola 2 — dasbor Div lama (mountWrapped):</b><br>
 * Dasbor yang masih {@code extends Div} tidak perlu diubah internalnya.
 * Cukup ubah baris mounting di Action pemanggil:</p>
 *
 * <pre>
 *   // Sebelum:
 *   DasbordCatatan d = new DasbordCatatan(Lingkup.MAHASISWA);
 *   d.setWidth("100%");
 *   d.setParent(tabDasbor);
 *
 *   // Sesudah:
 *   DasbordCatatan d = new DasbordCatatan(Lingkup.MAHASISWA);
 *   BaseDasbordPortal.mountWrapped(d, tabDasbor,
 *       "Catatan Mahasiswa",
 *       "Semua catatan harian dan akademik yang dicatat untuk mahasiswa.");
 * </pre>
 *
 * <p>Kelas ini menjamin konsistensi: satu definisi portal → semua halaman tampil
 * seragam, ikut tema, dan responsif di HP maupun desktop. CSS diatur terpusat di
 * blok "KOMPAT PORTALLAYOUT CE" dalam css_utama.css.</p>
 *
 * <p>Kompatibel Java 1.6/1.7 (tanpa lambda/diamond/stream).</p>
 */
public class BaseDasbordPortal extends MyPortallayout {

    private static final long serialVersionUID = 20260615L;

    // ── Warna tema terpusat (gunakan ini, bukan hardcode hex) ────────────────
    /** Warna primer tema aktif (biru/ungu dll. sesuai pilihan admin). */
    public static final String WARNA_PRIMER   = DashboardUiKit.PRIMARY;
    /** Warna aksen tema aktif (cyan/teal dll.). */
    public static final String WARNA_AKSEN    = DashboardUiKit.ACCENT;
    /** Hijau — status berhasil / sudah selesai. */
    public static final String WARNA_SUKSES   = DashboardUiKit.GOOD;
    /** Oranye — peringatan / perlu perhatian. */
    public static final String WARNA_WASPADA  = DashboardUiKit.WARN;
    /** Merah — bahaya / belum selesai. */
    public static final String WARNA_BAHAYA   = DashboardUiKit.BAD;
    /** Abu-abu teks sekunder. */
    public static final String WARNA_REDUP    = DashboardUiKit.MUTED;

    // ── Konstruktor ──────────────────────────────────────────────────────────

    /** Inisialisasi portal dasbor: lebar 100%, siap diisi kolom dan panel. */
    public BaseDasbordPortal() {
        setWidth("100%");
    }

    // ── Builder kolom/panel (shortcut ke PortalUiHelper) ────────────────────

    /**
     * Buat satu kolom di dalam portal ini.
     * Contoh lebar: {@code "50%"}, {@code "33%"}, {@code "100%"}.
     * Kolom 100% otomatis pindah baris sendiri (membentang penuh).
     */
    protected MyPortalchildren kolom(String lebar) {
        return PortalUiHelper.kolom(this, lebar);
    }

    /**
     * Buat panel kartu di dalam kolom.
     * Mengembalikan <em>host Div</em> — tempat konten ditambahkan.
     * Gunakan {@link ais.common.Common#clear(Component)} pada host untuk
     * refresh panel tanpa menghapus judul atau deskripsi.
     */
    protected Component panel(MyPortalchildren kolom, String judul, String deskripsi) {
        return PortalUiHelper.panel(kolom, judul, deskripsi);
    }

    /**
     * Seperti {@link #panel(MyPortalchildren, String, String)} tapi dengan teks
     * popup Bantuan kustom (HTML).
     */
    protected Component panel(MyPortalchildren kolom, String judul,
            String deskripsi, String bantuanHtml) {
        return PortalUiHelper.panel(kolom, judul, deskripsi, bantuanHtml);
    }

    // ── Utilitas konten ──────────────────────────────────────────────────────

    /** Tambahkan HTML ke komponen manapun (cara singkat new Html().setParent). */
    protected static void appendHtml(Component parent, String html) {
        new Html(html).setParent(parent);
    }

    // ════════════════════════════════════════════════════════════════════════
    // STATIC HELPERS — untuk dasbord Div lama maupun kode non-OOP
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Pasang dasbord {@code Div} ke {@code parent} dengan membungkusnya dalam
     * portal → kolom → panel kartu (judul + deskripsi + tombol Maksimalkan +
     * tombol Bantuan). Cocok untuk kelas dasbord lama yang masih
     * {@code extends Div}.
     *
     * <p>Panel menggunakan lebar 100% (satu kolom penuh). Ganti dengan
     * {@link #mountWrappedKolom(Component, Component, String, String, String)}
     * bila ingin mengontrol lebar kolom.</p>
     *
     * @param dasbord  komponen Div yang sudah diinisialisasi
     * @param parent   Tabpanel / Div tujuan tempat portal dipasang
     * @param judul    judul panel kartu (singkat)
     * @param deskripsi kalimat sederhana untuk pengguna awam
     */
    public static void mountWrapped(Component dasbord, Component parent,
            String judul, String deskripsi) {
        mountWrappedKolom(dasbord, parent, "100%", judul, deskripsi);
    }

    /**
     * Seperti {@link #mountWrapped} tapi lebar kolom bisa diatur.
     *
     * @param lebarKolom lebar CSS kolom, mis. {@code "100%"}, {@code "60%"}
     */
    public static void mountWrappedKolom(Component dasbord, Component parent,
            String lebarKolom, String judul, String deskripsi) {
        MyPortallayout portal = PortalUiHelper.portal(parent);
        MyPortalchildren kol  = PortalUiHelper.kolom(portal, lebarKolom);
        Component host        = PortalUiHelper.panel(kol, judul, deskripsi);
        // Host tidak perlu padding tambahan — dasbord punya padding sendiri
        if (host instanceof Div) {
            ((Div) host).setStyle("padding:0; min-height:0; overflow:visible;");
        }
        if (dasbord != null) {
            /* PENTING (anti-kolaps): dasbor ber-tinggi PERSEN (mis. MyWindow /
             * Borderlayout dengan setHeight("100%")) akan KOLAPS jadi 0 di host
             * portal yang ber-tinggi natural (min-height:0) → konten tab kosong.
             * Ganti tinggi persen menjadi tinggi viewport konkret + min-height
             * pengaman agar isinya selalu tampil. Dasbor natural-height (tinggi
             * null, mis. yang extends Div/HTML) DIBIARKAN apa adanya. */
            if (dasbord instanceof org.zkoss.zk.ui.HtmlBasedComponent) {
                org.zkoss.zk.ui.HtmlBasedComponent hbc = (org.zkoss.zk.ui.HtmlBasedComponent) dasbord;
                String h = hbc.getHeight();
                if (h != null && h.trim().endsWith("%")) {
                    hbc.setHeight("82vh");
                    String st = hbc.getStyle();
                    String add = "min-height:520px;";
                    hbc.setStyle((st == null || st.trim().length() == 0) ? add : (st + ";" + add));
                }
            }
            dasbord.setParent(host);
        }
    }

    /**
     * Seperti {@link #mountWrapped} tetapi panel dibuat <b>TINGGI PENUH mengikuti
     * parent</b> (height:100%) — cocok untuk tab yang parent-nya punya tinggi
     * konkret (mis. Tabpanel 100% di dalam Tabbox bertinggi). Menyiapkan rantai
     * flex tinggi-penuh: parent(100%) → portal(100%) → kolom(100%) → panel
     * (class {@code ais-portal-fill}, di-CSS jadi flex-column 100%) → host(flex:1)
     * → dasbord(100%). Dasbor tetap diberi lantai pengaman lewat min-height-nya
     * sendiri agar tak pernah kolaps ke 0.
     */
    public static void mountWrappedTinggiPenuh(Component dasbord, Component parent,
            String judul, String deskripsi) {
        if (parent instanceof org.zkoss.zk.ui.HtmlBasedComponent) {
            ((org.zkoss.zk.ui.HtmlBasedComponent) parent).setHeight("100%");
        }
        MyPortallayout portal = PortalUiHelper.portal(parent);
        portal.setHeight("100%");
        MyPortalchildren kol = PortalUiHelper.kolom(portal, "100%");
        kol.setHeight("100%");
        tambahSclass(kol, "ais-portal-col-fill");

        Component host = PortalUiHelper.panel(kol, judul, deskripsi);

        // Tandai Panel sbg tinggi-penuh: host(Div) -> Panelchildren -> Panel.
        Component body  = (host == null) ? null : host.getParent();
        Component panel = (body == null) ? null : body.getParent();
        if (panel instanceof org.zkoss.zk.ui.HtmlBasedComponent) {
            tambahSclass((org.zkoss.zk.ui.HtmlBasedComponent) panel, "ais-portal-fill");
        }

        if (dasbord instanceof org.zkoss.zk.ui.HtmlBasedComponent) {
            ((org.zkoss.zk.ui.HtmlBasedComponent) dasbord).setHeight("100%");
        }
        if (dasbord != null) {
            dasbord.setParent(host);
        }
    }

    /** Menambah satu kelas CSS tanpa menghapus kelas yang sudah ada. */
    private static void tambahSclass(org.zkoss.zk.ui.HtmlBasedComponent c, String cls) {
        if (c == null || cls == null) {
            return;
        }
        String s = c.getSclass();
        c.setSclass((s == null || s.trim().length() == 0) ? cls : (s + " " + cls));
    }

    /**
     * Pasang dua dasbord berdampingan (kiri/kanan) dalam satu baris portal.
     * Di HP kedua kolom menumpuk jadi satu.
     *
     * @param kiri         dasbord kolom kiri
     * @param judulKiri    judul panel kiri
     * @param deskripsiKiri deskripsi panel kiri
     * @param lebarKiri    lebar kolom kiri, mis. {@code "50%"}
     * @param kanan        dasbord kolom kanan (boleh null bila satu kolom saja)
     * @param judulKanan   judul panel kanan
     * @param deskripsiKanan deskripsi panel kanan
     * @param lebarKanan   lebar kolom kanan, mis. {@code "50%"}
     * @param parent       komponen tujuan
     */
    public static void mountDuaKolom(
            Component kiri, String judulKiri, String deskripsiKiri, String lebarKiri,
            Component kanan, String judulKanan, String deskripsiKanan, String lebarKanan,
            Component parent) {
        MyPortallayout portal = PortalUiHelper.portal(parent);
        if (kiri != null) {
            MyPortalchildren kolKiri = PortalUiHelper.kolom(portal, lebarKiri);
            Component hostKiri = PortalUiHelper.panel(kolKiri, judulKiri, deskripsiKiri);
            if (hostKiri instanceof Div) {
                ((Div) hostKiri).setStyle("padding:0; min-height:0; overflow:visible;");
            }
            kiri.setParent(hostKiri);
        }
        if (kanan != null) {
            MyPortalchildren kolKanan = PortalUiHelper.kolom(portal, lebarKanan);
            Component hostKanan = PortalUiHelper.panel(kolKanan, judulKanan, deskripsiKanan);
            if (hostKanan instanceof Div) {
                ((Div) hostKanan).setStyle("padding:0; min-height:0; overflow:visible;");
            }
            kanan.setParent(hostKanan);
        }
    }

    /**
     * Buat portal kosong di {@code parent} dan kembalikan kolom 100% siap diisi.
     * Berguna bila kode ingin mengatur konten sendiri tanpa membungkus Div lama.
     */
    public static MyPortalchildren kolonBaru(Component parent) {
        MyPortallayout portal = PortalUiHelper.portal(parent);
        return PortalUiHelper.kolom(portal, "100%");
    }

    /**
     * Buat portal + kolom + panel dan kembalikan host Div.
     * Shortcut satu baris untuk pola panel tunggal 100% lebar.
     */
    public static Component panelTunggal(Component parent, String judul, String deskripsi) {
        MyPortalchildren kol = kolonBaru(parent);
        return PortalUiHelper.panel(kol, judul, deskripsi);
    }
}
