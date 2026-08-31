package ais.action.master.feeder;
import ais.ui.util.DashboardGridExportHelper;

import org.json.JSONArray;
import org.json.JSONObject;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.InputEvent;
import org.zkoss.zul.event.PagingEvent;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Textbox;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Div;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Label;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Panel;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Separator;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.EksporFromFeederAction;
import ais.action.master.feeder.integrator.helper.KirimKeFeederWindow;
import ais.action.master.feeder.integrator.helper.DownloadAktifitasMahasiwaKkn;
import ais.action.master.feeder.integrator.helper.DownloadAktifitasMahasiwaPkl;
import ais.action.master.feeder.integrator.helper.DownloadAktifitasMahasiwaMahasiswaRequestTugasAkhir;
import ais.action.master.feeder.integrator.helper.DownloadAktifitasMahasiwaSkripsiPesertaMahasiswa;
import ais.action.master.feeder.integrator.helper.DownloadAktifitasMahasiwaKknPesertaMahasiswa;
import ais.action.master.feeder.integrator.helper.DownloadAktifitasMahasiwaPklPesertaMahasiswa;
import ais.action.master.feeder.integrator.helper.DownloadAktifitasMahasiwaMahasiswaRequestTugasAkhirPesertaMahasiswa;
import ais.action.master.feeder.integrator.helper.DownloadAktifitasMahasiwaSkripsiPesertaDosen;
import ais.action.master.feeder.integrator.helper.DownloadAktifitasMahasiwaKknPesertaDosen;
import ais.action.master.feeder.integrator.helper.DownloadAktifitasMahasiwaPklPesertaDosen;
import ais.action.master.feeder.integrator.helper.DownloadAktifitasMahasiwaMahasiswaRequestTugasAkhirPesertaDosen;
import ais.action.master.feeder.integrator.helper.DownloadAkm;
import ais.action.master.feeder.integrator.helper.DownloadAjarDosen;
import ais.action.master.feeder.integrator.helper.DownloadAktifitasMahasiwaSkripsi;
import ais.action.master.feeder.integrator.helper.DownloadHistory;
import ais.action.master.feeder.integrator.helper.DownloadKelas;
import ais.action.master.feeder.integrator.helper.DownloadKelulusan;
import ais.action.master.feeder.integrator.helper.DownloadKrs;
import ais.action.master.feeder.integrator.helper.DownloadMahasiswa;
import ais.action.master.feeder.integrator.helper.DownloadMatakuliah;
import ais.action.master.feeder.integrator.helper.DownloadNilai;
import ais.action.master.feeder.integrator.helper.DownloadNilaiTransfer;
import ais.action.master.feeder.integrator.helper.DownloadPrestasiMahasiswa;
import ais.action.master.feeder.integrator.helper.UploadAjarDosen;
import ais.action.master.feeder.integrator.helper.UploadKelas;
import ais.action.master.feeder.integrator.helper.UploadKelulusan;
import ais.action.master.feeder.integrator.helper.UploadKrs;
import ais.action.master.feeder.integrator.helper.UploadNilai;
import ais.action.master.feeder.integrator.helper.UploadNilaiTransfer;
import ais.action.master.feeder.integrator.helper.UploadPrestasiMahasiswa;
import ais.action.master.feeder.util.FeederConnector;
import ais.action.master.feeder.util.NeoFeederSyncHelper;
import ais.common.Common;
import ais.database.model.NeoFeederSync;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyPortalchildren;
import ais.ui.util.MyPortallayout;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * {@code DasbordSinkronisasiNeoFeeder} — dasbor tunggal untuk memantau dan menyinkronkan seluruh
 * data akademik ke/dari server <b>Neo Feeder PDDikti</b>. Kelas ini adalah komponen ZK
 * (turunan {@link Div}) yang dirender penuh secara programatik (tanpa zul) dan mencakup keseluruhan
 * 231 API pada User Guide Neo Feeder v2.0, disusun menjadi <b>42 panel</b> yang dikelompokkan ke
 * dalam enam seksi bertema:
 * <ol>
 *   <li>Seksi 1 – Dosen &amp; Tenaga Pengajar (10 panel)</li>
 *   <li>Seksi 2 – Mahasiswa (11 panel)</li>
 *   <li>Seksi 3 – Akademik: kelas, nilai, kurikulum (10 panel)</li>
 *   <li>Seksi 4 – Periode Perkuliahan (2 panel)</li>
 *   <li>Seksi 5 – Profil PT &amp; Rekap Laporan (7 panel)</li>
 *   <li>Seksi 6 – Kampus Merdeka &amp; Rencana Evaluasi (2 panel)</li>
 * </ol>
 *
 * <h3>Arsitektur "satu kerangka, banyak panel" (maksimal reuse)</h3>
 * Seluruh 42 panel dibangun oleh satu pabrik generik {@link #buildFeederPanel} sehingga tiap
 * {@code buildPanelXxx()} hanya bersifat deklaratif — cukup menyebut judul, {@code act} baca/hitung,
 * daftar kolom, serta aksi Download (Ambil dari Feeder) dan Upload (Kirim ke Feeder). Dengan begitu
 * penambahan panel baru = satu pemanggilan {@code buildFeederPanel}, dan perbaikan perilaku panel
 * (paging, badge status, pencatatan, gaya) cukup dilakukan sekali di pabrik tersebut. Aksi refresh
 * setiap panel didaftarkan ke {@link #refreshers} (dipakai tombol "Refresh Semua") dan registri
 * {@code act}-nya ke {@link #cekRegistry} (dipakai "Cek Versi API").
 *
 * <h3>Alur data tiap panel</h3>
 * Saat dibuka, dasbor memuat sekali snapshot {@link NeoFeederSync} tersimpan
 * ({@code tersimpan}) sehingga tiap panel dapat menampilkan data terakhir <b>tanpa</b> memanggil
 * jaringan (auto-load dari cache lokal). Tombol "Refresh" memuat data langsung dari Feeder
 * ({@code GetList*} + {@code GetCount*}) dengan paging. Kolom grid disusun dinamis dari SELURUH key
 * respons API ({@link #kunciDinamis}) agar tidak ada kolom tersembunyi. Setiap operasi
 * baca/kirim dicatat fail-safe ke tabel {@link NeoFeederSync} lewat {@link #catatSync} dan status
 * sinkronisasi terhadap data lokal eCampus ditampilkan sebagai badge oleh {@link #terapkanStatus}.
 *
 * <h3>Fitur lintas-panel</h3>
 * <ul>
 *   <li><b>Test Koneksi</b> — menguji {@code getToken} ke Feeder dan menampilkan status.</li>
 *   <li><b>Refresh Semua</b> — menjalankan refresh seluruh panel berurutan, tahan-error per panel.</li>
 *   <li><b>Cek Versi API</b> — mengaudit ketersediaan tiap {@code act} (baca &amp; tulis) terhadap
 *       versi Neo Feeder terkini lewat {@code GetDictionary} (aman, hanya membaca).</li>
 * </ul>
 *
 * <h3>Tampilan (UI/UX) modern &amp; responsif</h3>
 * Gaya dipusatkan pada satu stylesheet ber-scope {@code .nf-dash} yang disuntik sekali
 * ({@link #injectStyle}) sehingga hanya memengaruhi komponen dasbor ini (tidak membocorkan gaya ke
 * halaman lain). Tata letak memakai kolom portal yang <b>otomatis menumpuk menjadi satu kolom pada
 * layar sempit</b> (media query) sehingga nyaman dibaca di desktop maupun perangkat mobile. Panel,
 * header koneksi, header seksi, tombol, serta badge status memakai palet dan token gaya yang
 * konsisten (didefinisikan sebagai konstanta {@code STYLE_*}) demi keseragaman dan kemudahan
 * pemeliharaan.
 *
 * <h3>Manajemen session</h3>
 * Dasbor tidak membuka session sendiri. Satu-satunya sentuhan basis data ({@link #hitungLokalAman})
 * memakai {@code HibernateUtil.currentSession()} yang <b>tidak</b> ditutup manual (dikelola siklus
 * request ZK) — sesuai aturan session aplikasi.
 *
 * <h3>Kompatibilitas</h3>
 * Ditulis agar kompatibel dengan <b>Java 1.7</b> (anonymous class, tanpa lambda) dan <b>ZK 5.5</b>.
 *
 * @author Tim AIS
 */
public class DasbordSinkronisasiNeoFeeder extends Div {

    private static final long serialVersionUID = 1L;
    private static final int PAGE_SIZE = 10;

    /** Kelas scope CSS akar dasbor — semua aturan gaya di-namespace di bawah kelas ini. */
    private static final String ROOT_SCLASS = "nf-dash";

    /** Warna tema utama dasbor (biru tua). */
    private static final String WARNA_PRIMARY = "#1a3c6b";
    /** Gaya pill/badge kecil (count &amp; status) — radius, padding, ukuran font konsisten. */
    private static final String STYLE_PILL_BASE =
        "font-size:11px; padding:2px 9px; border-radius:999px; margin-right:6px; font-weight:600;";

    private Label lblStatusKoneksi;

    /** Daftar aksi refresh tiap panel — dipakai oleh tombol "Refresh Semua". */
    private final java.util.List<PanelAction> refreshers = new java.util.ArrayList<PanelAction>();

    /** Registri act tiap panel {judul, actGet, actCount} — dipakai "Cek Versi API". */
    private final java.util.List<String[]> cekRegistry = new java.util.ArrayList<String[]>();

    /** Snapshot data NeoFeederSync tersimpan (aksi -&gt; baris), dimuat sekali saat halaman dibuka. */
    private java.util.Map<String, NeoFeederSync> tersimpan = new java.util.HashMap<String, NeoFeederSync>();

    /**
     * Nama seksi yang sedang dibangun. Diset di {@link #buildUI()} sebelum memanggil sekelompok
     * {@code buildPanelXxx()}, lalu dibaca {@link #buildFeederPanel} saat mendaftarkan {@link Kartu}
     * agar kartu dikelompokkan ke seksi yang benar pada tampilan grid.
     */
    private String seksiSekarang = "";

    /**
     * Daftar kartu entitas (satu per panel Feeder). Panel dibangun sekali (auto-load dari cache lokal)
     * tetapi TIDAK ditempel ke halaman; ia dipindahkan ke dalam popup saat kartunya diklik. Dengan
     * begitu tampilan utama ringan (hanya grid kartu, mirip halaman "Laporan" e-Kantin).
     */
    private final java.util.List<Kartu> kartuList = new java.util.ArrayList<Kartu>();

    /** Urutan seksi pada tampilan grid (menentukan urutan header). */
    private static final String[] URUT_SEKSI = {
        "Data Dosen & Tenaga Pengajar",
        "Data Mahasiswa",
        "Data Akademik (Kelas, Nilai, Kurikulum)",
        "Data Periode Perkuliahan",
        "Profil PT & Rekap Laporan",
        "Kampus Merdeka & Rencana Evaluasi"
    };

    /** Warna aksen tiap seksi (dipakai header seksi pada grid kartu). */
    private static final java.util.Map<String, String> WARNA_SEKSI = new java.util.HashMap<String, String>();
    static {
        WARNA_SEKSI.put("Data Dosen & Tenaga Pengajar", "#1a3c6b");
        WARNA_SEKSI.put("Data Mahasiswa", "#1a5c3c");
        WARNA_SEKSI.put("Data Akademik (Kelas, Nilai, Kurikulum)", "#5c1a6b");
        WARNA_SEKSI.put("Data Periode Perkuliahan", "#6b3c1a");
        WARNA_SEKSI.put("Profil PT & Rekap Laporan", "#3c3c1a");
        WARNA_SEKSI.put("Kampus Merdeka & Rencana Evaluasi", "#1a5c5c");
    }

    /**
     * Deskriptor satu entitas Feeder untuk tampilan grid kartu + popup.
     *
     * <p>Setiap kartu memegang: seksi (untuk pengelompokan), judul asli panel, nama bersih (untuk
     * label kartu), act Feeder (mis. {@code GetListDosen}), referensi kelas lokal eCampus
     * ({@code classRef}; {@code null} bila tak ada padanan), objek {@link Panel} yang sudah dibangun,
     * serta aksi Ambil/Kirim (untuk tombol "Samakan"). Kartu dengan {@code classRef != null} akan
     * menampilkan checkbox perbandingan eCampus vs Neo Feeder di dalam popup.</p>
     */
    private static final class Kartu {
        final String seksi;
        final String judul;
        final String nama;
        final String act;
        final String classRef;
        final Panel panel;
        final PanelAction onDownload;
        final PanelAction onUpload;
        final SetBanding setBanding;
        /** Penanda mutable status banding inline aktif (agar checkbox popup konsisten saat dibuka ulang). */
        final boolean[] bandingHolder;

        Kartu(String seksi, String judul, String act, String classRef, Panel panel,
                PanelAction onDownload, PanelAction onUpload, SetBanding setBanding, boolean[] bandingHolder) {
            this.seksi = seksi == null ? "" : seksi;
            this.judul = judul == null ? "" : judul;
            this.act = act == null ? "" : act;
            this.classRef = classRef;
            this.panel = panel;
            this.onDownload = onDownload;
            this.onUpload = onUpload;
            this.setBanding = setBanding;
            this.bandingHolder = bandingHolder;
            this.nama = bersihkanNama(this.judul);
        }

        /** {@code true} bila entitas Feeder ini punya tabel padanan di eCampus (bisa dibandingkan). */
        boolean adaPadanan() {
            return classRef != null;
        }

        /** Buang prefix nomor ("3.4 ") dan sufiks "(GetListXxx)" agar tersisa nama entitas bersih. */
        private static String bersihkanNama(String judul) {
            String s = judul.trim();
            s = s.replaceFirst("^\\d+(?:\\.\\d+)*\\s+", "");
            int p = s.indexOf('(');
            if (p > 0) {
                s = s.substring(0, p).trim();
            }
            return s.length() == 0 ? judul : s;
        }
    }

    public DasbordSinkronisasiNeoFeeder() throws Exception {
        setWidth("100%");
        setSclass(ROOT_SCLASS);
        setStyle("padding:14px; overflow:auto; background:#eef2f7;"
            + " font-family:-apple-system,'Segoe UI',Roboto,Helvetica,Arial,sans-serif; color:#0f172a;");
        buildUI();
    }

    // =========================================================
    // MAIN BUILD
    // =========================================================

    private void buildUI() throws Exception {
        injectStyle();
        DashboardGridExportHelper.pasang(this, "Sinkronisasi Neo Feeder");
        buildHeaderKoneksi();

        // Muat sekali data tersimpan agar tiap panel bisa auto-load tanpa klik Refresh.
        try {
            tersimpan = NeoFeederSyncHelper.ambilSemuaTersimpan(hostSekarang());
        } catch (Throwable t) {
            tersimpan = new java.util.HashMap<String, NeoFeederSync>();
        }

        new Separator().setParent(this);

        // =====================================================================================
        // Bangun SEMUA panel (auto-load dari cache lokal) tetapi JANGAN ditempel ke halaman.
        // Tiap buildPanelXxx() mendaftarkan sebuah Kartu (lewat buildFeederPanel). Field
        // seksiSekarang menentukan pengelompokan kartu. Panel baru ditampilkan dalam popup saat
        // kartunya diklik — sehingga halaman utama ringan dan rapi seperti daftar "Laporan" kantin.
        // =====================================================================================

        // --- SEKSI 1: DOSEN ---
        seksiSekarang = "Data Dosen & Tenaga Pengajar";
        buildPanelDosen();
        buildPanelPenugasanSemuaDosen();
        buildPanelAktivitasMengajarDosen();
        buildPanelDosenPengajarKelas();
        buildPanelDosenPembimbing();
        buildPanelBimbingMahasiswa();
        buildPanelRiwayatFungsionalDosen();
        buildPanelRiwayatPangkatDosen();
        buildPanelRiwayatSertifikasiDosen();
        buildPanelRiwayatPenelitianDosen();

        // --- SEKSI 2: MAHASISWA ---
        seksiSekarang = "Data Mahasiswa";
        buildPanelMahasiswa();
        buildPanelKelulusan();
        buildPanelPrestasi();
        buildPanelAktivitasMahasiswa();
        buildPanelAnggotaAktivitasMahasiswa();
        buildPanelNilaiTransfer();
        buildPanelRiwayatPendidikanMahasiswa();
        buildPanelUjiMahasiswa();
        buildPanelPerubahanRiwayatPendidikan();
        buildPanelRiwayatNilaiMahasiswa();
        buildPanelKrsMahasiswa();

        // --- SEKSI 3: AKADEMIK ---
        seksiSekarang = "Data Akademik (Kelas, Nilai, Kurikulum)";
        buildPanelMatakuliah();
        buildPanelKurikulum();
        buildPanelKelasKuliah();
        buildPanelPerkuliahanMahasiswa();
        buildPanelNilaiPerkuliahan();
        buildPanelSkalaNilaiProdi();
        buildPanelPesertaKelasKuliah();
        buildPanelRencanaPembelajaran();
        buildPanelMatkulKurikulum();
        buildPanelSubstansiKuliah();

        // --- SEKSI 4: PERIODE ---
        seksiSekarang = "Data Periode Perkuliahan";
        buildPanelPeriodePerkuliahan();
        buildPanelJalurPendaftaran();

        // --- SEKSI 5: REKAP & LAPORAN ---
        seksiSekarang = "Profil PT & Rekap Laporan";
        buildPanelProfilPT();
        buildPanelRekapJumlahDosen();
        buildPanelRekapJumlahMahasiswa();
        buildPanelRekapIpsMahasiswa();
        buildPanelRekapLaporan();
        buildPanelRekapKhsMahasiswa();
        buildPanelRekapKrsMahasiswa();

        // --- SEKSI 6: KAMPUS MERDEKA ---
        seksiSekarang = "Kampus Merdeka & Rencana Evaluasi";
        buildPanelRencanaEvaluasi();
        buildPanelKonversiKampusMerdeka();

        // Tampilkan sebagai grid kartu (gaya "Laporan" e-Kantin) + kotak pencarian.
        renderKartuGrid();
    }

    // =========================================================
    // STYLE (disuntik sekali, ber-scope .nf-dash)
    // =========================================================

    /**
     * Menyuntikkan satu blok {@code <style>} ber-scope {@code .nf-dash} untuk seluruh gaya modern
     * &amp; responsif dasbor: kolom menumpuk pada layar sempit (media query), panel/tombol/baris
     * grid dipercantik. Ber-scope agar tidak membocorkan gaya ke halaman lain.
     */
    private void injectStyle() {
        try {
            org.zkoss.zul.Style css = new org.zkoss.zul.Style();
            css.setContent(
                ".nf-dash .nf-col{box-sizing:border-box;}"
                + "@media(max-width:860px){.nf-dash .nf-col{width:100% !important;}}"
                + ".nf-dash .z-panel{border:1px solid #e2e8f0 !important;border-radius:12px !important;"
                + "box-shadow:0 1px 2px rgba(15,23,42,.06) !important;overflow:hidden;margin-bottom:12px;background:#fff;}"
                + ".nf-dash .z-panel-header,.nf-dash .z-panel-hl,.nf-dash .z-panel-header-move{"
                + "background:#f8fafc !important;color:#0f172a !important;font-weight:600 !important;"
                + "border-bottom:1px solid #eef2f7 !important;}"
                + ".nf-dash .z-toolbarbutton{border-radius:8px;transition:background .15s ease;}"
                + ".nf-dash .z-toolbarbutton:hover{background:#eef2f7;}"
                + ".nf-dash .dgrid .z-row:hover{background:#f8fafc;}"
                // --- Scroll horizontal tabel feeder: paksa grid selebar isi (kalahkan width inline ZK) ---
                + ".nf-dash .dgrid.z-grid{width:max-content !important;min-width:100% !important;max-width:none !important;}"
                // --- Grid kartu (gaya "Laporan" e-Kantin) ---
                + ".nf-dash .nf-kartu{transition:box-shadow .15s ease,border-color .15s ease,transform .15s ease;}"
                + ".nf-dash .nf-kartu:hover{box-shadow:0 6px 18px rgba(15,23,42,.10);"
                + "border-color:#c7d6ec !important;transform:translateY(-1px);}"
                + "@media(max-width:640px){.nf-dash .nf-kartu-grid{grid-template-columns:1fr !important;}}"
            );
            css.setParent(this);
        } catch (Throwable t) { ais.common.ErrorAuditUtil.record(t, "auto-audit(empty-catch) src/ais/action/master/feeder/DasbordSinkronisasiNeoFeeder.java:293");
        }
    }

    // =========================================================
    // SECTION HEADER
    // =========================================================

    /**
     * Header seksi bergaya modern: kartu putih dengan aksen batang berwarna di kiri (bukan blok
     * warna penuh) sehingga lebih ringan dan tetap mempertahankan identitas warna tiap seksi.
     *
     * @param text judul seksi
     * @param color warna identitas seksi (dipakai untuk aksen kiri &amp; teks)
     * @return komponen {@link Div} header seksi
     */
    private Div buildSectionHeader(String text, String color) {
        Div d = new Div();
        d.setStyle(
            "display:flex; align-items:center; gap:10px; padding:9px 14px;"
            + "background:#ffffff; border:1px solid #e2e8f0; border-left:4px solid " + color + ";"
            + "border-radius:10px; margin:14px 0 8px 0;"
        );
        Label lbl = new Label(text);
        lbl.setStyle("color:" + color + "; font-size:14px; font-weight:700; letter-spacing:.2px;");
        lbl.setParent(d);
        return d;
    }

    // =========================================================
    // PORTAL HELPERS
    // =========================================================

    /**
     * Membuat layout portal (grid dua kolom) untuk sebuah seksi.
     *
     * @return {@link MyPortallayout} yang sudah terpasang ke dasbor
     */
    private MyPortallayout buildPortal() {
        MyPortallayout portal = new MyPortallayout();
        portal.setWidth("100%");
        portal.setParent(this);
        return portal;
    }

    /**
     * Kolom portal 50% di desktop; diberi sclass {@code nf-col} agar otomatis menumpuk menjadi
     * 100% (satu kolom) pada layar sempit lewat media query yang disuntik {@link #injectStyle}.
     *
     * @param portal layout portal induk
     * @return {@link MyPortalchildren} kolom yang sudah terpasang
     */
    private MyPortalchildren buildCol(MyPortallayout portal) {
        MyPortalchildren col = new MyPortalchildren();
        col.setWidth("50%");
        try {
            col.setSclass("nf-col");
        } catch (Throwable t) { ais.common.ErrorAuditUtil.record(t, "auto-audit(empty-catch) src/ais/action/master/feeder/DasbordSinkronisasiNeoFeeder.java:350");
        }
        col.setParent(portal);
        return col;
    }

    // =========================================================
    // HEADER KONEKSI
    // =========================================================

    private void buildHeaderKoneksi() {
        Div header = new Div();
        header.setStyle(
            "background:linear-gradient(135deg,var(--ais-theme-primary,#1a3c6b),#2563a8);" +
            "color:#fff; border-radius:8px; padding:14px 18px;" +
            "margin-bottom:10px; display:flex; align-items:center;" +
            "justify-content:space-between; flex-wrap:wrap; gap:8px;"
        );
        header.setParent(this);

        Div kiriDiv = new Div();
        kiriDiv.setStyle("flex:1; min-width:200px;");
        kiriDiv.setParent(header);

        Label judul = new Label(ais.common.Common.getBahasaConfig("Sinkronisasi Neo Feeder PDDikti"));
        judul.setStyle("font-size:16px; font-weight:700; display:block; color:#fff;");
        judul.setParent(kiriDiv);

        String[] kon = safeKoneksi();
        String infoKon = "Host: " + kon[0] + ":" + kon[1] + "  |  User: " + kon[2];
        Label lblInfo = new Label(infoKon);
        lblInfo.setStyle("font-size:12px; opacity:.8; display:block; color:#cde; margin-top:3px;");
        lblInfo.setParent(kiriDiv);

        lblStatusKoneksi = new Label(ais.common.Common.getBahasaConfig("Klik 'Test Koneksi' untuk memeriksa status"));
        lblStatusKoneksi.setStyle("font-size:12px; margin-top:4px; display:block; color:#ffd;");
        lblStatusKoneksi.setParent(kiriDiv);

        Label lblApiInfo = new Label(
            ais.common.Common.getBahasaConfig("42 panel | 231 API Neo Feeder v2.0 tercakup")
        );
        lblApiInfo.setStyle("font-size:11px; opacity:.65; display:block; color:#fff; margin-top:2px;");
        lblApiInfo.setParent(kiriDiv);

        Div kananDiv = new Div();
        kananDiv.setStyle("display:flex; gap:8px; align-items:center; flex-wrap:wrap;");
        kananDiv.setParent(header);

        MyToolbarbuttonConfig btnRefreshAll = new MyToolbarbuttonConfig(
            "Refresh Semua", "/img/Button-Refresh-icon.png"
        );
        btnRefreshAll.setStyle("background:#16a34a; border:1px solid #15803d; color:#fff;"
            + " border-radius:4px; font-weight:700;");
        btnRefreshAll.setTooltiptext("Muat ulang SEMUA panel dari Neo Feeder sekaligus");
        btnRefreshAll.addEventListener("onClick", new EventListener() {
            public void onEvent(Event e) throws Exception {
                refreshSemua();
            }
        });
        btnRefreshAll.setParent(kananDiv);

        MyToolbarbuttonConfig btnCekApi = new MyToolbarbuttonConfig(
            "Cek Versi API", "/img/search.gif"
        );
        btnCekApi.setStyle("background:#7c3aed; border:1px solid #6d28d9; color:#fff;"
            + " border-radius:4px; font-weight:700;");
        btnCekApi.setTooltiptext("Periksa apakah semua fungsi API dasbor masih cocok dengan versi Neo Feeder terbaru");
        btnCekApi.addEventListener("onClick", new EventListener() {
            public void onEvent(Event e) throws Exception {
                cekVersiApi();
            }
        });
        btnCekApi.setParent(kananDiv);

        MyToolbarbuttonConfig btnTest = new MyToolbarbuttonConfig(
            "Test Koneksi", "/img/Button-Refresh-icon.png"
        );
        btnTest.setStyle("background:#fff3; border:1px solid #fff5; color:#fff; border-radius:4px;");
        btnTest.setTooltiptext("Uji koneksi ke Neo Feeder");
        btnTest.addEventListener("onClick", new EventListener() {
            public void onEvent(Event e) throws Exception {
                testKoneksi();
            }
        });
        btnTest.setParent(kananDiv);

        MyToolbarbuttonConfig btnSettings = new MyToolbarbuttonConfig(
            "Pengaturan Koneksi", "/img/edit.gif"
        );
        btnSettings.setStyle("background:#fff3; border:1px solid #fff5; color:#fff; border-radius:4px;");
        btnSettings.setTooltiptext("Buka halaman pengaturan koneksi Neo Feeder");
        btnSettings.addEventListener("onClick", new EventListener() {
            public void onEvent(Event e) throws Exception {
                EksporFromFeederAction.display();
            }
        });
        btnSettings.setParent(kananDiv);
    }

    /**
     * Tombol "Test Koneksi". Badge ringkas di header (di bawah tombol) tetap singkat agar muat di
     * ruang sempit, TAPI setiap hasil (berhasil/gagal koneksi/gagal login/kendala tak terduga)
     * SELALU disertai jendela pesan formal & rinci via
     * {@link ais.action.master.feeder.util.NeoFeederPesanFormalHelper} — memuat penyebab, langkah
     * tindak lanjut, serta anjuran eskalasi ke Administrator/Pengembang Sistem (dengan tangkapan
     * layar) bila langkah tersebut belum menyelesaikan masalah.
     */
    private void testKoneksi() {
        try {
            String[] kon = safeKoneksi();
            String ip = kon[0];
            String portStr = kon[1];
            String username = kon[2];
            String password = kon[3];
            boolean https = Common.bolehKonfigurasi("aktifkan_https_ke_feeder", ais.database.model.Konfigurasi.TIDAK_AKTIF);

            if (ip == null || ip.isEmpty() || ip.equals("10.0.0.0")) {
                lblStatusKoneksi.setValue("IP Feeder belum dikonfigurasi");
                lblStatusKoneksi.setStyle("font-size:12px; display:block; color:#f88; margin-top:4px;");
                ais.action.master.feeder.util.NeoFeederPesanFormalHelper.tampilkanGagalKoneksi(ip, portStr, https,
                        "Alamat IP Aplikasi Feeder belum diisi (masih bernilai bawaan \"" + ip
                                + "\") pada menu Pengaturan Koneksi.");
                return;
            }

            FeederConnector fc = new FeederConnector(ip, Integer.parseInt(portStr));
            java.util.List<String> warnings = new java.util.ArrayList<String>();
            String token = fc.getToken(username, password, warnings);

            if (token != null && !token.trim().isEmpty()
                    && !token.trim().toLowerCase().startsWith("error")) {
                lblStatusKoneksi.setValue("Terhubung. Token berhasil diperoleh.");
                lblStatusKoneksi.setStyle("font-size:12px; display:block; color:#8f8; margin-top:4px;");
                ais.action.master.feeder.util.NeoFeederPesanFormalHelper.tampilkanSukses(
                        "Uji Koneksi ke Neo Feeder",
                        "Sistem berhasil terhubung ke server Neo Feeder pada alamat \"" + ip + "\" port \""
                                + portStr + "\" dan berhasil masuk (login) dengan nama pengguna \"" + username
                                + "\".");
            } else {
                String detailTeknis = warnings.isEmpty() ? null : warnings.get(0);
                lblStatusKoneksi.setValue("Gagal login. Lihat rincian pada jendela pesan.");
                lblStatusKoneksi.setStyle("font-size:12px; display:block; color:#f88; margin-top:4px;");
                ais.action.master.feeder.util.NeoFeederPesanFormalHelper.tampilkanGagalLogin(username, detailTeknis);
            }
        } catch (Exception ex) {
            lblStatusKoneksi.setValue("Terjadi kendala. Lihat rincian pada jendela pesan.");
            lblStatusKoneksi.setStyle("font-size:12px; display:block; color:#f88; margin-top:4px;");
            Common.tampilErrorJikaAdmin(ex);
            ais.action.master.feeder.util.NeoFeederPesanFormalHelper.tampilkanGagalUmum(
                    "Uji Koneksi ke Neo Feeder", ex.getMessage(),
                    "Periksa kembali Alamat IP, Port, Username, dan Password Feeder pada Pengaturan Koneksi.");
        }
    }

    // =========================================================
    // GENERIC PANEL FRAMEWORK
    // =========================================================

    /**
     * Pembawa data/helper lokal milik {@link DasbordSinkronisasiNeoFeeder} untuk panel state. Tipe ini
     * mengelompokkan nilai antara agar perhitungan atau rendering tidak memakai array/map tanpa kontrak yang
     * jelas.
     *
     * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link
     * DasbordSinkronisasiNeoFeeder}. Dependensi yang diperlukan harus diberikan secara eksplisit agar aman
     * digunakan dan diuji.</p> Tipe ini merupakan detail implementasi privat; pemanggil luar harus memakai API
     * kelas induk.
     * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code int offset}, {@code int totalFeeder},
     * {@code String filterCari}, {@code boolean bandingInline}. Aturan bisnis bersama tetap berada pada kelas
     * induk atau service yang dipanggilnya.</p>
     *
     * @see DasbordSinkronisasiNeoFeeder
     */
    private static final class PanelState {
        int offset = 0;
        int totalFeeder = 0;
        /** Fragmen filter WHERE hasil kotak pencarian (kosong = tanpa pencarian). */
        String filterCari = "";
        /** Bila true, tabel menampilkan kolom versi eCampus berdampingan (mode banding inline). */
        boolean bandingInline = false;
    }

    /**
     * Kontrak callback/strategi bersarang milik {@link DasbordSinkronisasiNeoFeeder}. Tipe ini memisahkan satu
     * variasi perilaku lokal tanpa membuat service atau interface global yang tumpang tindih.
     *
     * <p><b>Scope:</b> setiap instance terikat pada instance {@link DasbordSinkronisasiNeoFeeder} dan dapat
     * mengakses state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p> Tipe ini
     * merupakan detail implementasi privat; pemanggil luar harus memakai API kelas induk.
     * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code run}(). Aturan bisnis bersama tetap
     * berada pada kelas induk atau service yang dipanggilnya.</p>
     * <p><b>Efek samping:</b> operasi dapat mengubah state lokal dan, sesuai nama methodnya, komponen UI atau
     * persistence melalui konteks kelas induk. Gunakan transaksi, otorisasi, dan session milik alur induk;
     * tambahkan perilaku lintas domain pada service bersama.</p>
     *
     * @see DasbordSinkronisasiNeoFeeder
     */
    private interface PanelAction {
        void run() throws Exception;
    }

    /** Kait untuk menyalakan/mematikan mode banding inline (kolom eCampus) dari luar panel. */
    private interface SetBanding {
        void set(boolean on) throws Exception;
    }

    /**
     * Aksi "Ambil dari Feeder" TAMBAHAN untuk panel yang punya beberapa varian impor
     * (mis. Aktivitas Mahasiswa: Skripsi/KKN/PKL/Tugas Akhir). Tiap aksi menjadi satu
     * tombol hijau di toolbar panel.
     */
    private static class AksiTambahan {
        final String label;
        final String tooltip;
        final PanelAction aksi;

        AksiTambahan(String label, String tooltip, PanelAction aksi) {
            this.label = label;
            this.tooltip = tooltip;
            this.aksi = aksi;
        }
    }

    /** Overload kompatibilitas (dipakai mayoritas panel): tanpa tombol "Ambil" tambahan. */
    private Panel buildFeederPanel(
            final String title, final String actGet, final String actCount, final String filterDefault,
            final String[] columnKeys, final String[] columnHeaders,
            final PanelAction onDownload, final PanelAction onUpload) {
        return buildFeederPanel(title, actGet, actCount, filterDefault, columnKeys, columnHeaders,
                onDownload, onUpload, null);
    }

    /**
     * Membangun panel generik.
     * @param actCount null atau "" jika tidak ada API count → paging tidak ditampilkan
     * @param aksiTambahan tombol "Ambil dari Feeder" tambahan (boleh null)
     */
    private Panel buildFeederPanel(
            final String title,
            final String actGet,
            final String actCount,
            final String filterDefault,
            final String[] columnKeys,
            final String[] columnHeaders,
            final PanelAction onDownload,
            final PanelAction onUpload,
            final AksiTambahan[] aksiTambahan) {

        final PanelState state = new PanelState();

        Panel panel = new Panel();
        panel.setTitle(title);
        panel.setCollapsible(true);
        panel.setOpen(true);
        panel.setBorder("normal");
        panel.setStyle("margin-bottom:12px; overflow:hidden;");

        Panelchildren pc = new Panelchildren();
        pc.setParent(panel);

        Vbox vbox = new Vbox();
        vbox.setWidth("100%");
        vbox.setStyle("padding:8px;");
        vbox.setParent(pc);

        // toolbar
        Toolbar tb = new Toolbar();
        tb.setStyle("border:none; background:transparent; padding:4px 0;");
        tb.setParent(vbox);

        final Label countLabel = new Label("...");
        countLabel.setStyle(STYLE_PILL_BASE + " background:#e8f0fe; color:" + WARNA_PRIMARY + ";");
        countLabel.setParent(tb);

        // Badge status sinkronisasi data terhadap data lokal eCampus.
        final Label statusLabel = new Label("");
        statusLabel.setStyle(STYLE_PILL_BASE);
        statusLabel.setParent(tb);
        final String classRef = NeoFeederSyncHelper.kelasLokalDariEntitas(
            NeoFeederSyncHelper.entitasDariAksi(actGet));

        MyToolbarbuttonConfig btnRefresh = new MyToolbarbuttonConfig(
            "Refresh", "/img/Button-Refresh-icon.png"
        );
        btnRefresh.setTooltiptext("Muat ulang data dari Neo Feeder");
        btnRefresh.setParent(tb);

        if (onDownload != null) {
            MyToolbarbuttonConfig btnDl = new MyToolbarbuttonConfig(
                "Ambil dari Feeder", "/img/Button-Refresh-icon.png"
            );
            btnDl.setTooltiptext("Import / ambil data dari Neo Feeder ke sistem ini");
            btnDl.setStyle("color:#1a6b3c;");
            btnDl.addEventListener("onClick", new EventListener() {
                public void onEvent(Event e) throws Exception {
                    String host = hostSekarang();
                    catatSync(actGet, title, NeoFeederSync.ARAH_AMBIL, NeoFeederSync.STATUS_PROSES,
                            null, null, null, null, "Mulai ambil/import dari feeder.", host);
                    try {
                        onDownload.run();
                        catatSync(actGet, title, NeoFeederSync.ARAH_AMBIL, NeoFeederSync.STATUS_TERSINGKRON,
                                null, null, null, null, "Selesai ambil/import dari feeder.", host);
                    } catch (Exception ex) {
                        catatSync(actGet, title, NeoFeederSync.ARAH_AMBIL, NeoFeederSync.STATUS_ERROR,
                                null, null, null, null, ex.getMessage(), host);
                        Common.tampilErrorJikaAdmin(ex);
                        ais.ui.util.MyMessageboxConfig.show(
                            "Gagal: " + ex.getMessage(), "Error",
                            ais.ui.util.MyMessageboxConfig.OK,
                            ais.ui.util.MyMessageboxConfig.EXCLAMATION
                        );
                    }
                }
            });
            btnDl.setParent(tb);
        }

        // Tombol "Ambil dari Feeder" TAMBAHAN (mis. varian aktivitas: KKN/PKL/Tugas Akhir).
        if (aksiTambahan != null) {
            for (int i = 0; i < aksiTambahan.length; i++) {
                final AksiTambahan at = aksiTambahan[i];
                if (at == null || at.aksi == null) {
                    continue;
                }
                MyToolbarbuttonConfig btnExtra = new MyToolbarbuttonConfig(
                    at.label, "/img/Button-Refresh-icon.png"
                );
                if (at.tooltip != null) {
                    btnExtra.setTooltiptext(at.tooltip);
                }
                btnExtra.setStyle("color:#1a6b3c;");
                btnExtra.addEventListener("onClick", new EventListener() {
                    public void onEvent(Event e) throws Exception {
                        try {
                            at.aksi.run();
                        } catch (Exception ex) {
                            Common.tampilErrorJikaAdmin(ex);
                            ais.ui.util.MyMessageboxConfig.show(
                                "Gagal: " + ex.getMessage(), "Error",
                                ais.ui.util.MyMessageboxConfig.OK,
                                ais.ui.util.MyMessageboxConfig.EXCLAMATION
                            );
                        }
                    }
                });
                btnExtra.setParent(tb);
            }
        }

        if (onUpload != null) {
            MyToolbarbuttonConfig btnUl = new MyToolbarbuttonConfig(
                "Kirim ke Feeder", "/img/save.gif"
            );
            btnUl.setTooltiptext("Export / kirim data dari sistem ini ke Neo Feeder");
            btnUl.setStyle("color:#6b1a1a;");
            btnUl.addEventListener("onClick", new EventListener() {
                public void onEvent(Event e) throws Exception {
                    String host = hostSekarang();
                    catatSync(actGet, title, NeoFeederSync.ARAH_KIRIM, NeoFeederSync.STATUS_PROSES,
                            null, null, null, null, "Mulai kirim/export ke feeder.", host);
                    try {
                        onUpload.run();
                        catatSync(actGet, title, NeoFeederSync.ARAH_KIRIM, NeoFeederSync.STATUS_TERKIRIM,
                                null, null, null, null, "Selesai kirim/export ke feeder.", host);
                    } catch (Exception ex) {
                        catatSync(actGet, title, NeoFeederSync.ARAH_KIRIM, NeoFeederSync.STATUS_ERROR,
                                null, null, null, null, ex.getMessage(), host);
                        Common.tampilErrorJikaAdmin(ex);
                        ais.ui.util.MyMessageboxConfig.show(
                            "Gagal: " + ex.getMessage(), "Error",
                            ais.ui.util.MyMessageboxConfig.OK,
                            ais.ui.util.MyMessageboxConfig.EXCLAMATION
                        );
                    }
                }
            });
            btnUl.setParent(tb);
        }

        // --- Kotak pencarian data umum (kode/nama/dll) — filter dikirim ke Neo Feeder ---
        // Hanya kolom yang PASTI ada (diambil dari columnKeys panel, yaitu kolom yang memang
        // dikembalikan & ditampilkan). Enter atau tombol "Cari" mengirim filter, "Reset" menghapus.
        final java.util.List<String> kolCari = kolomCari(columnKeys);
        final Textbox txtCari = new Textbox();
        final MyToolbarbuttonConfig btnCari = new MyToolbarbuttonConfig("Cari", "/img/search.png");
        final MyToolbarbuttonConfig btnResetCari = new MyToolbarbuttonConfig("Reset", "/img/Button-Refresh-icon.png");
        if (!kolCari.isEmpty()) {
            Div cariRow = new Div();
            cariRow.setStyle("display:flex; gap:6px; align-items:center; margin:2px 0 8px;");
            cariRow.setParent(vbox);
            txtCari.setWidth("100%");
            txtCari.setTooltiptext("Cari berdasarkan: " + gabung(kolCari, ", "));
            txtCari.setStyle("padding:8px 11px; border:1px solid #d7dee8; border-radius:9px; font-size:13px;"
                + " box-sizing:border-box;");
            txtCari.setParent(cariRow);
            btnCari.setStyle("background:" + WARNA_PRIMARY + "; color:#fff; border:0; border-radius:8px; font-weight:600;");
            btnCari.setParent(cariRow);
            btnResetCari.setTooltiptext("Hapus pencarian, tampilkan semua data");
            btnResetCari.setParent(cariRow);
        }

        // grid — dibungkus Div ber-overflow agar muncul SCROLL HORIZONTAL saat kolom banyak.
        Div gridScroll = new Div();
        gridScroll.setStyle("overflow-x:auto; overflow-y:hidden; width:100%; max-width:100%;");
        gridScroll.setParent(vbox);

        final Grid grid = new MyGrid();
        // PENTING (scroll horizontal): JANGAN pakai lebar 100% (div blok 100% = selebar pembungkus,
        // tabel meluber di dalam tanpa memicu scroll). Pakai idiom "width:max-content; min-width:100%":
        //  - tabel LEBAR (kolom banyak) -> lebar = isi (max-content) MELEBIHI pembungkus -> Div
        //    pembungkus (overflow-x:auto) memunculkan SCROLL HORIZONTAL;
        //  - tabel SEMPIT (kolom sedikit) -> min-width:100% -> tetap penuh selebar pembungkus.
        grid.setSclass("dgrid");
        grid.setStyle("width:max-content; min-width:100%;");
        grid.setParent(gridScroll);
        ais.ui.util.ZkCompat.setFixedLayout(grid, false);

        Columns cols = new Columns();
        cols.setParent(grid);
        Column noCol = new MyColumnConfig();
        noCol.setLabel("No");
        noCol.setWidth("40px");
        noCol.setParent(cols);
        for (String h : columnHeaders) {
            Column c = new MyColumnConfig();
            c.setLabel(h);
            c.setParent(cols);
        }

        final Rows dataRows = new Rows();
        dataRows.setParent(grid);

        final boolean hasPaging = (actCount != null && !actCount.isEmpty());

        final Paging paging = new Paging();
        paging.setPageSize(PAGE_SIZE);
        paging.setVisible(false);
        if (hasPaging) {
            paging.setParent(vbox);
        }

        // Auto-load: bila aksi ini sudah pernah tersimpan di NeoFeederSync,
        // langsung tampilkan datanya tanpa harus klik Refresh.
        final NeoFeederSync tersimpanRow = (tersimpan == null) ? null : tersimpan.get(actGet);
        boolean adaTersimpan = muatDariTersimpan(tersimpanRow, columnKeys, columnHeaders, grid, countLabel,
            dataRows, paging, state, hasPaging);
        if (!adaTersimpan) {
            Row placeholderRow = new Row();
            placeholderRow.setStyle("color:#999; font-style:italic;");
            placeholderRow.setParent(dataRows);
            new Label(ais.common.Common.getBahasaConfig("Klik 'Refresh' untuk memuat data dari Neo Feeder.")).setParent(placeholderRow);
        }
        terapkanStatus(statusLabel, tersimpanRow, classRef);

        // Aksi refresh bersama (dipakai tombol Refresh, Refresh Semua, Cari/Reset, & toggle banding).
        // Selalu mereset ke halaman pertama lalu memuat ulang dari feeder dengan filter efektif.
        final PanelAction refreshPanel = new PanelAction() {
            public void run() throws Exception {
                state.offset = 0;
                if (hasPaging) paging.setActivePage(0);
                doRefreshPanel(title, actGet, actCount, filterDefault,
                    columnKeys, columnHeaders, grid, countLabel, statusLabel, classRef, dataRows, paging, state, hasPaging);
            }
        };

        // wire refresh
        btnRefresh.addEventListener("onClick", new EventListener() {
            public void onEvent(Event e) throws Exception {
                refreshPanel.run();
            }
        });

        // wire pencarian (Cari/Enter/Reset) — mengubah state.filterCari lalu memuat ulang.
        final EventListener cariListener = new EventListener() {
            public void onEvent(Event e) throws Exception {
                state.filterCari = buildFilterCari(txtCari.getValue(), kolCari);
                refreshPanel.run();
            }
        };
        btnCari.addEventListener("onClick", cariListener);
        txtCari.addEventListener("onOK", cariListener); // tekan Enter di kotak cari
        btnResetCari.addEventListener("onClick", new EventListener() {
            public void onEvent(Event e) throws Exception {
                txtCari.setValue("");
                state.filterCari = "";
                refreshPanel.run();
            }
        });

        if (hasPaging) {
            paging.addEventListener("onPaging", new EventListener() {
                public void onEvent(Event e) throws Exception {
                    if (e instanceof PagingEvent) {
                        state.offset = ((PagingEvent) e).getActivePage() * PAGE_SIZE;
                        doRefreshPanel(title, actGet, actCount, filterDefault,
                            columnKeys, columnHeaders, grid, countLabel, statusLabel, classRef, dataRows, paging, state, hasPaging);
                    }
                }
            });
        }

        // Daftarkan refresher panel ini untuk tombol "Refresh Semua".
        refreshers.add(refreshPanel);

        // Kait toggle banding inline (dipanggil checkbox "Bandingkan dengan data eCampus" di popup).
        final boolean[] bandingHolder = new boolean[] { false };
        final SetBanding setBanding = new SetBanding() {
            public void set(boolean on) throws Exception {
                state.bandingInline = on;
                bandingHolder[0] = on;
                refreshPanel.run();
            }
        };

        // Daftarkan act panel ini untuk tombol "Cek Versi API".
        cekRegistry.add(new String[] { title, actGet, actCount == null ? "" : actCount });

        // Daftarkan KARTU (untuk tampilan grid + popup). Panel TIDAK ditempel ke halaman di sini;
        // ia dipindahkan ke dalam popup saat kartunya diklik (lihat renderKartuGrid/bukaPopupKartu).
        kartuList.add(new Kartu(seksiSekarang, title, actGet, classRef, panel, onDownload, onUpload,
            setBanding, bandingHolder));

        return panel;
    }

    // =========================================================
    // TAMPILAN GRID KARTU (gaya "Laporan" e-Kantin) + POPUP
    // =========================================================

    /**
     * Merender seluruh {@link #kartuList} menjadi <b>grid kartu responsif</b> yang dikelompokkan per
     * seksi, dilengkapi kotak pencarian untuk memfilter kartu berdasarkan nama/act. Tiap kartu
     * hanya menampilkan ringkasan (nama entitas + act + badge padanan eCampus); isi tabel baru
     * dibuka dalam popup ketika kartu diklik ({@link #bukaPopupKartu}). Pola ini menyamai halaman
     * daftar "Laporan" pada modul e-Kantin: halaman utama ringkas, detail tampil saat dipilih.
     */
    private void renderKartuGrid() {
        // Kotak pencarian (ZK 5 tanpa placeholder — pakai label petunjuk + tooltip).
        Div toolbarCari = new Div();
        toolbarCari.setStyle("margin:14px 0 4px;");
        toolbarCari.setParent(this);
        Label lblCari = new Label("Cari tabel / entitas Neo Feeder");
        lblCari.setStyle("display:block; font-size:12px; font-weight:600; color:#475569; margin-bottom:5px;");
        lblCari.setParent(toolbarCari);
        final Textbox cari = new Textbox();
        cari.setParent(toolbarCari);
        cari.setWidth("100%");
        cari.setTooltiptext("Ketik nama entitas Feeder (mis. dosen, matakuliah, nilai) untuk memfilter kartu");
        cari.setStyle("padding:11px 14px; border:1px solid #d7dee8; border-radius:12px; font-size:14px;"
            + " box-sizing:border-box; background:#fff;");

        // Pasangan {kartuDiv, teksPencarian} untuk filter cepat sisi-server.
        final java.util.List<Object[]> indeks = new java.util.ArrayList<Object[]>();
        // Simpan referensi header + grid tiap seksi agar bisa disembunyikan saat kosong.
        final java.util.List<Object[]> seksiBlok = new java.util.ArrayList<Object[]>();

        for (int s = 0; s < URUT_SEKSI.length; s++) {
            String seksi = URUT_SEKSI[s];
            String warna = WARNA_SEKSI.get(seksi);
            if (warna == null) {
                warna = WARNA_PRIMARY;
            }
            Div hdr = buildSectionHeader(seksi, warna);
            hdr.setParent(this);

            Div grid = new Div();
            grid.setSclass("nf-kartu-grid");
            grid.setStyle("display:grid; grid-template-columns:repeat(auto-fill,minmax(260px,1fr));"
                + " gap:12px; margin:2px 0 8px;");
            grid.setParent(this);

            int jml = 0;
            for (int i = 0; i < kartuList.size(); i++) {
                Kartu k = kartuList.get(i);
                if (!seksi.equals(k.seksi)) {
                    continue;
                }
                Div card = buildKartuDiv(k);
                card.setParent(grid);
                indeks.add(new Object[] { card, (k.nama + " " + k.judul + " " + k.act).toLowerCase() });
                jml++;
            }
            seksiBlok.add(new Object[] { hdr, grid, Integer.valueOf(jml) });
        }

        // Filter langsung saat mengetik: tampilkan/sembunyikan kartu + sembunyikan seksi kosong.
        cari.addEventListener("onChanging", new EventListener() {
            public void onEvent(Event e) throws Exception {
                String q = ((InputEvent) e).getValue();
                q = (q == null) ? "" : q.trim().toLowerCase();
                for (int i = 0; i < indeks.size(); i++) {
                    Object[] o = indeks.get(i);
                    Div d = (Div) o[0];
                    String txt = (String) o[1];
                    d.setVisible(q.length() == 0 || txt.contains(q));
                }
                // Sembunyikan header seksi yang tidak punya kartu terlihat.
                for (int i = 0; i < seksiBlok.size(); i++) {
                    Object[] blk = seksiBlok.get(i);
                    Div grid = (Div) blk[1];
                    boolean adaTampil = false;
                    java.util.List<?> anak = grid.getChildren();
                    for (int j = 0; j < anak.size(); j++) {
                        Object c = anak.get(j);
                        if (c instanceof Div && ((Div) c).isVisible()) {
                            adaTampil = true;
                            break;
                        }
                    }
                    ((Div) blk[0]).setVisible(adaTampil);
                    grid.setVisible(adaTampil);
                }
            }
        });
    }

    /**
     * Membangun satu kartu ringkas untuk {@link Kartu}. Kartu menampilkan ikon, nama entitas, act
     * Feeder, serta badge "Ada padanan eCampus" bila entitas dapat dibandingkan dengan tabel lokal.
     * Klik kartu membuka popup berisi tabelnya ({@link #bukaPopupKartu}).
     */
    private Div buildKartuDiv(final Kartu k) {
        Div card = new Div();
        card.setSclass("nf-kartu");
        card.setStyle("background:#fff; border:1px solid #e2e8f0; border-radius:14px; padding:13px 14px;"
            + " cursor:pointer; display:flex; align-items:flex-start; gap:11px;"
            + " box-shadow:0 1px 2px rgba(15,23,42,.04);");

        Div ikon = new Div();
        ikon.setStyle("width:38px; height:38px; flex:0 0 auto; border-radius:11px;"
            + " background:linear-gradient(135deg,#e8f0fe,#dbe7fb); display:flex; align-items:center;"
            + " justify-content:center; color:" + WARNA_PRIMARY + "; font-size:18px; font-weight:800;");
        new Label(k.adaPadanan() ? "⇄" : "≡").setParent(ikon); // arah-panah (dpt dibandingkan) / bar
        ikon.setParent(card);

        Div teks = new Div();
        teks.setStyle("flex:1; min-width:0;");
        teks.setParent(card);

        Label nama = new Label(k.nama);
        nama.setStyle("display:block; font-size:14px; font-weight:700; color:#0f172a; line-height:1.25;");
        nama.setParent(teks);

        Label sub = new Label(k.act);
        sub.setStyle("display:block; font-size:11px; color:#64748b; margin-top:2px; word-break:break-word;");
        sub.setParent(teks);

        Div badges = new Div();
        badges.setStyle("margin-top:7px;");
        badges.setParent(teks);
        if (k.adaPadanan()) {
            Label pill = new Label("Bisa dibandingkan eCampus");
            pill.setStyle(STYLE_PILL_BASE + " background:#dcfce7; color:#166534; display:inline-block;");
            pill.setParent(badges);
        } else {
            Label pill = new Label("Khusus Feeder");
            pill.setStyle(STYLE_PILL_BASE + " background:#eef2f7; color:#475569; display:inline-block;");
            pill.setParent(badges);
        }

        card.addEventListener("onClick", new EventListener() {
            public void onEvent(Event e) throws Exception {
                bukaPopupKartu(k);
            }
        });
        return card;
    }

    /**
     * Membuka popup berisi tabel entitas Feeder ({@code k.panel} dipindahkan ke dalam popup). Bila
     * entitas punya padanan lokal ({@link Kartu#adaPadanan()}), popup menambahkan checkbox
     * "Bandingkan dengan data eCampus" yang, saat dicentang, menampilkan ringkasan perbandingan
     * jumlah (eCampus vs Neo Feeder) beserta tombol <b>Samakan</b> dua arah.
     */
    private void bukaPopupKartu(final Kartu k) {
        // Tutup popup lama untuk kartu yang sama (bila panel masih tertaut di window sebelumnya).
        tutupPopupLama(k.panel);

        final MyWindow w = new MyWindow(k.nama + "  —  " + k.act, "normal", true);
        w.setWidth("90%");
        w.setHeight("88%");
        w.setParent(this);

        final Vbox box = new Vbox();
        box.setWidth("100%");
        box.setStyle("padding:12px; max-height:82vh; overflow:auto;");
        box.setParent(w);

        // Toolbar Ekspor Excel + Cetak PDF — mengekspor SEMUA tabel yang tampil di popup ini
        // (tabel data Feeder + tabel hasil perbandingan bila dibuka). Memakai engine bersama.
        Div barExport = new Div();
        barExport.setStyle("display:flex; gap:8px; justify-content:flex-end; margin-bottom:10px;");
        barExport.setParent(box);
        try {
            DashboardGridExportHelper.pasangTombol(barExport, box, "Neo Feeder - " + k.nama);
        } catch (Throwable t) {
            ais.common.ErrorAuditUtil.record(t, "auto-audit(empty-catch) DasbordSinkronisasiNeoFeeder.bukaPopupKartu:export");
        }

        if (k.adaPadanan()) {
            final Div compBox = new Div();
            compBox.setStyle("border:1px solid #e2e8f0; border-radius:12px; padding:11px 13px;"
                + " margin-bottom:12px; background:#f8fafc;");
            compBox.setParent(box);

            final Checkbox cb = new Checkbox("Bandingkan dengan data eCampus (lokal)");
            cb.setStyle("font-weight:700; color:#0f172a;");
            cb.setParent(compBox);

            final Div hasil = new Div();
            hasil.setStyle("margin-top:10px;");
            hasil.setParent(compBox);

            // Selaraskan status checkbox dengan mode banding yang mungkin masih aktif dari sesi buka
            // sebelumnya (tabel tetap menampilkan kolom eCampus). Tampilkan pula ringkasannya.
            if (k.bandingHolder != null && k.bandingHolder[0]) {
                cb.setChecked(true);
                tampilkanPerbandingan(k, hasil);
            }

            cb.addEventListener("onCheck", new EventListener() {
                public void onEvent(Event e) throws Exception {
                    boolean on = cb.isChecked();
                    Common.clear(hasil);
                    if (on) {
                        tampilkanPerbandingan(k, hasil);
                    }
                    // Nyalakan/matikan kolom eCampus berdampingan (warna beda; MERAH bila beda) di
                    // tabel data utama panel ini.
                    if (k.setBanding != null) {
                        try {
                            k.setBanding.set(on);
                        } catch (Exception ex) {
                            Common.tampilErrorJikaAdmin(ex);
                        }
                    }
                }
            });
        }

        // Pindahkan panel data (grid + tombol Refresh/Ambil/Kirim) ke dalam popup.
        // Lebar 100% agar terikat lebar popup -> pembungkus grid menampilkan SCROLL HORIZONTAL.
        k.panel.setWidth("100%");
        k.panel.setParent(box);
        w.doHighlighted();
    }

    /** Menutup window (popup) yang saat ini menampung {@code panel}, bila ada. */
    private void tutupPopupLama(Panel panel) {
        org.zkoss.zk.ui.Component c = panel.getParent();
        while (c != null && !(c instanceof org.zkoss.zul.Window)) {
            c = c.getParent();
        }
        if (c != null) {
            c.detach();
        }
    }

    /**
     * Menampilkan ringkasan perbandingan eCampus (lokal) vs Neo Feeder untuk satu entitas, beserta
     * tombol <b>Samakan</b> dua arah:
     * <ul>
     *   <li><b>Samakan eCampus &larr; Feeder</b> (impor/Ambil) — menjadikan data eCampus sama dengan
     *       Feeder; memanggil aksi {@code onDownload} entitas.</li>
     *   <li><b>Samakan Feeder &larr; eCampus</b> (ekspor/Kirim) — menjadikan data Feeder sama dengan
     *       eCampus; memanggil aksi {@code onUpload} entitas.</li>
     * </ul>
     * Jumlah lokal dihitung via {@link #hitungLokalAman}; jumlah Feeder diambil dari snapshot
     * {@link #tersimpan} (hasil sinkronisasi terakhir).
     */
    private void tampilkanPerbandingan(final Kartu k, Div wadah) {
        Long lokal = hitungLokalAman(k.classRef);
        NeoFeederSync row = (tersimpan == null) ? null : tersimpan.get(k.act);
        Integer feeder = (row == null) ? null : row.getJumlahFeeder();

        // Kartu angka: eCampus, Neo Feeder, Selisih.
        Div ring = new Div();
        ring.setStyle("display:flex; gap:12px; flex-wrap:wrap; align-items:stretch;");
        ring.setParent(wadah);
        ring.appendChild(kartuAngka("eCampus (lokal)", lokal == null ? "?" : String.valueOf(lokal), "#1e3a8a"));
        ring.appendChild(kartuAngka("Neo Feeder", feeder == null ? "?" : String.valueOf(feeder), "#166534"));
        String selisihTeks = "?";
        String warnaSelisih = "#92400e";
        if (lokal != null && feeder != null) {
            long selisih = feeder.longValue() - lokal.longValue();
            selisihTeks = (selisih > 0 ? "+" : "") + selisih;
            warnaSelisih = (selisih == 0) ? "#166534" : "#92400e";
        }
        ring.appendChild(kartuAngka("Selisih (Feeder - Lokal)", selisihTeks, warnaSelisih));

        // Verdict.
        Label verd = new Label();
        verd.setStyle("display:block; margin-top:10px; font-size:13px;");
        if (feeder == null) {
            verd.setValue("Jumlah Neo Feeder belum diketahui. Klik tombol \"Refresh\" pada tabel di bawah "
                + "terlebih dahulu agar jumlah Feeder termuat, lalu buka lagi perbandingan ini.");
            verd.setStyle(verd.getStyle() + " color:#92400e; font-weight:600;");
        } else if (lokal == null) {
            verd.setValue("Jumlah lokal eCampus tidak dapat dihitung untuk entitas ini.");
            verd.setStyle(verd.getStyle() + " color:#92400e; font-weight:600;");
        } else if (lokal.longValue() == feeder.longValue()) {
            verd.setValue("Data sudah SAMA (jumlah lokal = jumlah Feeder = " + lokal + ").");
            verd.setStyle(verd.getStyle() + " color:#166534; font-weight:700;");
        } else if (lokal.longValue() > feeder.longValue()) {
            verd.setValue("eCampus memiliki LEBIH BANYAK data (" + lokal + ") dibanding Feeder (" + feeder
                + "). Gunakan \"Samakan Feeder ← eCampus\" untuk mengirim kekurangannya ke Feeder.");
            verd.setStyle(verd.getStyle() + " color:#92400e; font-weight:700;");
        } else {
            verd.setValue("Feeder memiliki LEBIH BANYAK data (" + feeder + ") dibanding eCampus (" + lokal
                + "). Gunakan \"Samakan eCampus ← Feeder\" untuk mengambil kekurangannya dari Feeder.");
            verd.setStyle(verd.getStyle() + " color:#92400e; font-weight:700;");
        }
        verd.setParent(wadah);

        // Catatan metode perbandingan.
        Label ket = new Label("Ringkasan di atas berbasis JUMLAH baris (lokal via database eCampus, Feeder via "
            + "sinkronisasi terakhir). Untuk melihat BARIS yang berbeda satu per satu, klik "
            + "\"Perbandingan baris-per-baris\".");
        ket.setStyle("display:block; margin-top:4px; font-size:11px; color:#64748b;");
        ket.setParent(wadah);

        // Tombol Samakan dua arah.
        Div tombol = new Div();
        tombol.setStyle("display:flex; gap:9px; flex-wrap:wrap; margin-top:12px;");
        tombol.setParent(wadah);

        // Samakan LANGSUNG (tanpa jendela Download/Upload) — hanya konfirmasi berkonsekuensi lalu
        // impor/ekspor SEMUA data entitas ini via FeederImporter/FeederExporter di thread latar.
        final String entity = entityKey(k.classRef);
        if (entity != null) {
            MyToolbarbuttonConfig bAmbil = new MyToolbarbuttonConfig(
                "Samakan eCampus ← Feeder (Ambil)", "/img/svg/list-check.svg");
            bAmbil.setTooltiptext("Impor SEMUA data " + entity + " dari Neo Feeder ke eCampus (langsung, tanpa jendela)");
            bAmbil.setStyle("background:#16a34a; color:#fff; border:0; border-radius:9px; font-weight:700; padding:7px 12px;");
            bAmbil.addEventListener("onClick", new EventListener() {
                public void onEvent(Event e) throws Exception {
                    samakanLangsung(k.judul, k.act, entity, true);
                }
            });
            bAmbil.setParent(tombol);

            MyToolbarbuttonConfig bKirim = new MyToolbarbuttonConfig(
                "Samakan Feeder ← eCampus (Kirim)", "/img/svg/save-2-fill.svg");
            bKirim.setTooltiptext("Kirim SEMUA data " + entity + " dari eCampus ke Neo Feeder (langsung, tanpa jendela)");
            bKirim.setStyle("background:#b45309; color:#fff; border:0; border-radius:9px; font-weight:700; padding:7px 12px;");
            bKirim.addEventListener("onClick", new EventListener() {
                public void onEvent(Event e) throws Exception {
                    samakanLangsung(k.judul, k.act, entity, false);
                }
            });
            bKirim.setParent(tombol);
        }

        // Restore ke tanggal tertentu (GLOBAL/seluruh record) — buka riwayat revisi Envers.
        MyToolbarbuttonConfig bRestore = new MyToolbarbuttonConfig(
            "Restore ke tanggal tertentu", "/img/svg/history.svg");
        bRestore.setTooltiptext("Buka riwayat revisi (Envers) untuk mengembalikan data " + k.nama
            + " ke kondisi pada tanggal/revisi tertentu");
        bRestore.setStyle("background:#6d28d9; color:#fff; border:0; border-radius:9px; font-weight:700; padding:7px 12px;");
        bRestore.addEventListener("onClick", new EventListener() {
            public void onEvent(Event e) throws Exception {
                bukaRestore(k.classRef, null);
            }
        });
        bRestore.setParent(tombol);

        // Tombol + wadah PERBANDINGAN BARIS-PER-BARIS (hanya untuk entitas yang metadatanya dikenal).
        final MetaBanding meta = metaBanding(k.classRef);
        if (meta != null) {
            final Div detail = new Div();
            detail.setStyle("margin-top:12px;");

            MyToolbarbuttonConfig bDetail = new MyToolbarbuttonConfig(
                "Perbandingan baris-per-baris (detail)", "/img/Button-Refresh-icon.png");
            bDetail.setStyle("background:#1a3c6b; color:#fff; border:0; border-radius:9px; font-weight:700; padding:7px 12px;");
            bDetail.setTooltiptext("Ambil SELURUH data Feeder & eCampus, lalu tampilkan baris yang hanya ada di salah satu sisi");
            bDetail.addEventListener("onClick", new EventListener() {
                public void onEvent(Event e) throws Exception {
                    Common.clear(detail);
                    Label load = new Label("Memuat perbandingan baris-per-baris... mengambil SELURUH data dari "
                        + "Neo Feeder dan database eCampus. Untuk entitas besar (mis. Mahasiswa) proses bisa "
                        + "1-2 menit. Mohon tunggu.");
                    load.setStyle("display:block; color:#475569; font-size:12px; padding:8px 0;");
                    load.setParent(detail);
                    Common.createDefaultTimer(new EventListener() {
                        public void onEvent(Event ev) throws Exception {
                            jalankanPerbandinganBaris(k, meta, detail);
                        }
                    });
                }
            });
            bDetail.setParent(tombol);

            detail.setParent(wadah);
        }
    }

    // =========================================================
    // SAMAKAN LANGSUNG (tanpa jendela) + RESTORE (Envers)
    // =========================================================

    /** Kunci entitas untuk FeederImporter/FeederExporter dari classRef lokal ({@code null} bila tak dikenal). */
    private static String entityKey(String classRef) {
        if ("ais.database.model.Matakuliah".equals(classRef)) {
            return "matakuliah";
        }
        if ("ais.database.model.Kurikulum".equals(classRef)) {
            return "kurikulum";
        }
        if ("ais.database.model.Mahasiswa".equals(classRef)) {
            return "mahasiswa";
        }
        if ("ais.database.model.Dosen".equals(classRef)) {
            return "dosen";
        }
        return null;
    }

    /**
     * Konfirmasi (berisi rincian KONSEKUENSI) lalu — bila disetujui — menjalankan impor/ekspor
     * SELURUH data entitas LANGSUNG (tanpa membuka jendela Download/Upload). Memakai
     * {@link FeederConnector} + {@code FeederImporter}/{@code FeederExporter} di thread latar dengan
     * popup progres melayang.
     *
     * @param ambil {@code true} = Ambil/impor (eCampus mengikuti Feeder); {@code false} = Kirim/ekspor.
     */
    private void samakanLangsung(final String judul, final String act, final String entity, final boolean ambil) {
        final String pesan = ambil
            ? ("KONFIRMASI — Samakan eCampus ← Feeder (AMBIL/IMPOR): \"" + judul + "\".\n\n"
                + "KONSEKUENSI bila dilanjutkan:\n"
                + "• SELURUH data " + entity + " di Neo Feeder akan DIAMBIL lalu di-IMPOR ke eCampus.\n"
                + "• Data lokal yang cocok (berdasarkan kode Feeder) DIPERBARUI mengikuti Feeder;\n"
                + "  data baru dari Feeder DITAMBAHKAN ke eCampus.\n"
                + "• Perubahan pada data eCampus tercatat di riwayat revisi (bisa di-Restore).\n"
                + "• Proses berjalan di latar & dapat memakan waktu untuk data besar.\n\n"
                + "Lanjutkan proses AMBIL sekarang?")
            : ("KONFIRMASI — Samakan Feeder ← eCampus (KIRIM/EKSPOR): \"" + judul + "\".\n\n"
                + "KONSEKUENSI bila dilanjutkan:\n"
                + "• SELURUH data " + entity + " di eCampus akan DIKIRIM ke Neo Feeder (insert/update PDDikti).\n"
                + "• Data di Neo Feeder akan mengikuti data eCampus.\n"
                + "• Pastikan data eCampus SUDAH BENAR — perubahan tercatat resmi di PDDikti/Feeder.\n"
                + "• Proses berjalan di latar & dapat memakan waktu untuk data besar.\n\n"
                + "Lanjutkan proses KIRIM sekarang?");
        try {
            ais.ui.util.MyMessageboxConfig.show(pesan,
                ambil ? "Konfirmasi Ambil dari Neo Feeder" : "Konfirmasi Kirim ke Neo Feeder",
                ais.ui.util.MyMessageboxConfig.OK | ais.ui.util.MyMessageboxConfig.CANCEL,
                ais.ui.util.MyMessageboxConfig.QUESTION,
                new EventListener() {
                    public void onEvent(Event ev) throws Exception {
                        if (ev.getData() == null
                                || Integer.parseInt(ev.getData().toString()) != ais.ui.util.MyMessageboxConfig.OK.intValue()) {
                            return;
                        }
                        mulaiSamakanLangsung(judul, act, entity, ambil);
                    }
                });
        } catch (Exception ex) {
            Common.tampilErrorJikaAdmin(ex);
        }
    }

    /** Menjalankan impor/ekspor langsung di thread latar + popup progres; hasil ditampilkan saat selesai. */
    private void mulaiSamakanLangsung(final String judul, final String act, final String entity, final boolean ambil) {
        final String host = hostSekarang();
        final String arah = ambil ? NeoFeederSync.ARAH_AMBIL : NeoFeederSync.ARAH_KIRIM;
        final String statusOk = ambil ? NeoFeederSync.STATUS_TERSINGKRON : NeoFeederSync.STATUS_TERKIRIM;
        catatSync(act, judul, arah, NeoFeederSync.STATUS_PROSES, null, null, null, null,
            "Mulai samakan langsung (" + arah + ").", host);

        // WAJIB diambil di UI thread SEBELUM thread latar dimulai — komponen ZK hanya boleh
        // disentuh dari thread yang "diaktifkan" untuk desktop-nya (lihat Executions.activate di
        // bawah). Tanpa ini: java.lang.IllegalStateException: Components can be accessed only in
        // event listeners (pola sama dengan LaporanProgressUtil yang sudah terbukti aman).
        final org.zkoss.zk.ui.Desktop desktop = this.getDesktop();

        final java.util.List<String> errorLog = new java.util.ArrayList<String>();
        final Label progres = ais.action.master.feeder.util.NeoFeederProgressHelper.show(
            (ambil ? "Mengambil " : "Mengirim ") + judul + (ambil ? " dari Neo Feeder" : " ke Neo Feeder"),
            new EventListener() {
                public void onEvent(Event e) throws Exception {
                    if (e != null && e.getName() != null && e.getName().trim().length() > 0) {
                        ais.ui.util.MyMessageboxConfig.show("Gagal: " + e.getName(), "Error",
                            ais.ui.util.MyMessageboxConfig.OK, ais.ui.util.MyMessageboxConfig.EXCLAMATION);
                        return;
                    }
                    if (!errorLog.isEmpty()) {
                        StringBuilder sb = new StringBuilder();
                        sb.append("Selesai dengan ").append(errorLog.size()).append(" catatan/kendala:\n\n");
                        int n = Math.min(errorLog.size(), 30);
                        for (int i = 0; i < n; i++) {
                            sb.append("• ").append(errorLog.get(i)).append("\n");
                        }
                        if (errorLog.size() > n) {
                            sb.append("... (").append(errorLog.size() - n).append(" catatan lainnya)");
                        }
                        ais.ui.util.MyMessageboxConfig.show(sb.toString(), "Selesai (ada catatan)",
                            ais.ui.util.MyMessageboxConfig.OK, ais.ui.util.MyMessageboxConfig.EXCLAMATION);
                    } else {
                        ais.ui.util.MyMessageboxConfig.show(
                            (ambil ? "Impor dari Neo Feeder selesai." : "Kirim ke Neo Feeder selesai.")
                                + " Klik \"Refresh\" pada tabel untuk melihat data terbaru.",
                            "Selesai", ais.ui.util.MyMessageboxConfig.OK, ais.ui.util.MyMessageboxConfig.INFORMATION);
                    }
                }
            });

        new Thread(new Runnable() {
            public void run() {
                // Aktifkan desktop untuk thread INI agar boleh menyentuh komponen ZK (progres) di
                // seluruh proses — termasuk yang dilakukan FeederImporter/FeederExporter secara
                // internal (mereka juga memanggil label.setValue(...) berulang kali). Bila desktop
                // sudah tidak aktif (mis. user menutup popup/tab), lanjutkan proses TANPA update UI
                // (aktif=false) — proses import/export & pencatatan NeoFeederSync tetap jalan.
                boolean aktif = false;
                if (desktop != null) {
                    try {
                        org.zkoss.zk.ui.Executions.activate(desktop);
                        aktif = true;
                    } catch (Throwable ta) {
                        aktif = false;
                    }
                }
                try {
                    try {
                        String[] kon = safeKoneksi();
                        FeederConnector fc = new FeederConnector(kon[0], Integer.parseInt(kon[1]));
                        String token = fc.getToken(kon[2], kon[3]);
                        if (token == null || token.trim().isEmpty()) {
                            catatSync(act, judul, arah, NeoFeederSync.STATUS_ERROR, null, null, null, null,
                                "Gagal mendapatkan token.", host);
                            if (aktif) {
                                progres.setValue("Error: Gagal mendapatkan token. Periksa Pengaturan Koneksi.");
                            }
                            return;
                        }
                        if (ambil) {
                            ais.action.master.feeder.util.FeederImporter imp =
                                new ais.action.master.feeder.util.FeederImporter(fc, token, null, null, progres);
                            if ("matakuliah".equals(entity)) {
                                imp.matakuliah();
                            } else if ("kurikulum".equals(entity)) {
                                imp.kurikulum();
                            } else if ("mahasiswa".equals(entity)) {
                                imp.mahasiswa();
                            } else if ("dosen".equals(entity)) {
                                imp.dosen();
                            }
                        } else {
                            ais.action.master.feeder.util.FeederExporter exp =
                                new ais.action.master.feeder.util.FeederExporter(fc, token, null, null, progres);
                            if ("matakuliah".equals(entity)) {
                                exp.matakuliah(errorLog);
                            } else if ("kurikulum".equals(entity)) {
                                exp.kurikulum(errorLog);
                            } else if ("mahasiswa".equals(entity)) {
                                exp.mahasiswa();
                            } else if ("dosen".equals(entity)) {
                                exp.dosen();
                            }
                        }
                        catatSync(act, judul, arah, statusOk, null, null, null, null, "Selesai samakan langsung.", host);
                        if (aktif) {
                            progres.setValue("Selesai");
                        }
                    } catch (Exception ex) {
                        catatSync(act, judul, arah, NeoFeederSync.STATUS_ERROR, null, null, null, null,
                            ex.getMessage(), host);
                        if (aktif) {
                            try {
                                progres.setValue("Error: " + ex.getMessage());
                            } catch (Throwable tv) {
                                ais.common.ErrorAuditUtil.record(tv,
                                    "auto-audit(empty-catch) DasbordSinkronisasiNeoFeeder.mulaiSamakanLangsung:setValue");
                            }
                        }
                    }
                } finally {
                    if (aktif) {
                        try {
                            org.zkoss.zk.ui.Executions.deactivate(desktop);
                        } catch (Throwable td) {
                            ais.common.ErrorAuditUtil.record(td,
                                "auto-audit(empty-catch) DasbordSinkronisasiNeoFeeder.mulaiSamakanLangsung:deactivate");
                        }
                    }
                }
            }
        }).start();
    }

    /**
     * Membuka riwayat revisi (Envers) via {@code GenericRevisiHelper} untuk mengembalikan (restore)
     * data ke kondisi pada tanggal/revisi tertentu. Bila {@code id} diberikan, dibatasi ke satu
     * record (per baris); bila {@code null}, mencakup seluruh record entitas (global).
     */
    private void bukaRestore(String classRef, Long id) {
        try {
            Class kelas = Class.forName(classRef);
            String judul = (id == null)
                ? ("Restore / Riwayat Revisi — " + kelas.getSimpleName() + " (semua record)")
                : ("Restore / Riwayat Revisi — " + kelas.getSimpleName() + " (record ID " + id + ")");
            ais.action.master.helper.GenericRevisiHelper helper;
            if (id == null) {
                helper = new ais.action.master.helper.GenericRevisiHelper(kelas, judul, null, null);
            } else {
                helper = new ais.action.master.helper.GenericRevisiHelper(kelas, judul, null, null,
                    new ais.action.master.helper.GenericRevisiHelper.EntityIdFilter(id));
            }
            helper.setParent(this);
            helper.doHighlighted();
        } catch (Throwable t) {
            Common.tampilErrorJikaAdmin(t instanceof Exception ? (Exception) t : new Exception(t));
            try {
                ais.ui.util.MyMessageboxConfig.show("Gagal membuka riwayat revisi: " + t.getMessage(),
                    "Error", ais.ui.util.MyMessageboxConfig.OK, ais.ui.util.MyMessageboxConfig.EXCLAMATION);
            } catch (Exception ig) {
                ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) DasbordSinkronisasiNeoFeeder.bukaRestore");
            }
        }
    }

    // =========================================================
    // PERBANDINGAN BARIS-PER-BARIS (row-level diff eCampus vs Feeder)
    // =========================================================

    /**
     * Metadata pemetaan satu entitas untuk perbandingan baris: field id Feeder (kandidat berurut),
     * field kode/nama di sisi Feeder, serta properti kode/nama di entitas lokal eCampus. Hanya 4
     * entitas yang punya kelas lokal ({@link NeoFeederSyncHelper#kelasLokalDariEntitas}) sehingga
     * hanya inilah yang dapat dibandingkan baris-per-baris.
     */
    private static final class MetaBanding {
        final String[] feederIdKeys;
        final String feederKodeKey;
        final String feederNamaKey;
        final String localKodeProp;
        final String localNamaProp;
        final String labelKode;
        /** Pasangan {feederColumnKey, localProperty} untuk kolom eCampus inline (banding samping). */
        final String[][] kolomBanding;

        MetaBanding(String[] idKeys, String fk, String fn, String lk, String ln, String labelKode,
                String[][] kolomBanding) {
            this.feederIdKeys = idKeys;
            this.feederKodeKey = fk;
            this.feederNamaKey = fn;
            this.localKodeProp = lk;
            this.localNamaProp = ln;
            this.labelKode = labelKode;
            this.kolomBanding = kolomBanding;
        }
    }

    /**
     * Kembalikan {@link MetaBanding} untuk classRef lokal, atau {@code null} bila tidak dikenal.
     *
     * <p>{@code kolomBanding} memetakan <b>kolom Feeder -&gt; properti lokal</b> untuk SEMUA field
     * non-UUID yang punya padanan di eCampus. Pemetaan boleh "berlebih": kolom eCampus hanya
     * ditambahkan bila kolom Feeder-nya benar-benar muncul di respons (lihat {@link #kolomRender}),
     * sehingga aman menyertakan kolom yang mungkin tidak selalu dikembalikan API.</p>
     */
    private static MetaBanding metaBanding(String classRef) {
        if ("ais.database.model.Dosen".equals(classRef)) {
            return new MetaBanding(new String[] { "id_dosen", "id_ptk" }, "nidn", "nama_dosen", "nidn", "nama", "NIDN",
                new String[][] {
                    { "nidn", "nidn" }, { "nama_dosen", "nama" }, { "nuptk", "nuptk" },
                    { "jenis_kelamin", "kelamin" }, { "npwp", "npwp" }, { "email", "email" },
                    { "nik", "ktp" }, { "no_hp", "hp" }, { "handphone", "hp" }, { "telepon", "telp" },
                    { "tempat_lahir", "tempatlahir" }, { "gelar_depan", "gelarDepan" },
                    { "gelar_belakang", "gelarBelakang" }
                });
        }
        if ("ais.database.model.Mahasiswa".equals(classRef)) {
            return new MetaBanding(new String[] { "id_mahasiswa" }, "nim", "nama_mahasiswa", "nim", "nama", "NIM",
                new String[][] {
                    { "nim", "nim" }, { "nama_mahasiswa", "nama" }, { "jenis_kelamin", "kelamin" },
                    { "tempat_lahir", "tempatlahir" }, { "nik", "ktp" }, { "email", "email" },
                    { "alamat", "alamat" }, { "no_hp", "telp" }, { "handphone", "telp" },
                    { "telepon", "telp" }
                });
        }
        if ("ais.database.model.Matakuliah".equals(classRef)) {
            return new MetaBanding(new String[] { "id_matkul" }, "kode_mata_kuliah", "nama_mata_kuliah", "kode", "nama", "Kode MK",
                new String[][] {
                    { "kode_mata_kuliah", "kode" }, { "nama_mata_kuliah", "nama" },
                    { "sks_mata_kuliah", "sks" }, { "sks_praktek", "sksPraktek" },
                    { "sks_praktek_lapangan", "sksPraktekLapangan" }, { "sks_simulasi", "sksSimulasi" },
                    { "ada_bahan_ajar", "adaBahanAjar" }, { "ada_sap", "adaSap" },
                    { "ada_silabus", "adaSilabus" }, { "ada_acara_praktek", "adaAcaraPraktek" },
                    { "ada_diktat", "adaDiktat" }
                });
        }
        if ("ais.database.model.Kurikulum".equals(classRef)) {
            return new MetaBanding(new String[] { "id_kurikulum" }, "nama_kurikulum", "nama_kurikulum", null, "nama", "Kode Feeder",
                new String[][] {
                    { "nama_kurikulum", "nama" }, { "jumlah_sks_lulus", "jumlahAturanSksLulus" },
                    { "jumlah_sks_wajib", "jumlahAturanSksWajib" }, { "jumlah_sks_pilihan", "jumlahAturanSksPilihan" }
                });
        }
        return null;
    }

    /**
     * Inti perbandingan baris-per-baris: mengambil SELURUH baris dari Neo Feeder (paginasi) dan
     * SELURUH baris lokal eCampus (proyeksi Criteria), mencocokkan berdasarkan <b>id Feeder</b>
     * (tersimpan di kolom {@code feeder} lokal), lalu merender: ringkasan + tabel "Hanya di Neo
     * Feeder (perlu Ambil)" + tabel "Hanya di eCampus (perlu Kirim)". Dijalankan di thread UI (via
     * timer) sehingga aman membangun komponen ZK; memakai {@code currentSession()} yang tidak
     * ditutup manual.
     */
    private void jalankanPerbandinganBaris(Kartu k, MetaBanding meta, Div wadah) {
        Common.clear(wadah);
        try {
            // 1) Ambil semua baris Feeder (paginasi, dengan pengaman jumlah).
            String[] kon = safeKoneksi();
            FeederConnector fc = new FeederConnector(kon[0], Integer.parseInt(kon[1]));
            String token = fc.getToken(kon[2], kon[3]);
            if (token == null || token.trim().isEmpty()) {
                tampilPesanBanding(wadah, "Gagal mendapatkan token Neo Feeder. Periksa Pengaturan Koneksi.", true);
                return;
            }

            java.util.LinkedHashMap<String, String[]> feederMap = new java.util.LinkedHashMap<String, String[]>();
            int off = 0;
            final int lim = 500;
            final int cap = 50000;
            boolean terpotong = false;
            while (true) {
                JSONArray arr = fc.getData(k.act, token, "", "", String.valueOf(lim), String.valueOf(off));
                int got = (arr == null) ? 0 : arr.length();
                if (got == 0) {
                    break; // habis (offset melewati total)
                }
                int sebelum = feederMap.size();
                for (int i = 0; i < got; i++) {
                    JSONObject o = arr.getJSONObject(i);
                    String id = ambilIdFeeder(o, meta.feederIdKeys);
                    if (id == null || id.length() == 0) {
                        continue;
                    }
                    if (!feederMap.containsKey(id)) {
                        feederMap.put(id, new String[] { optStr(o, meta.feederKodeKey), optStr(o, meta.feederNamaKey) });
                    }
                }
                // Maju sesuai jumlah yang BENAR-BENAR dikembalikan (server mungkin membatasi page
                // di bawah lim). Berhenti bila tak ada id baru (server mengabaikan offset / duplikat)
                // agar tidak looping tak berujung.
                if (feederMap.size() == sebelum) {
                    break;
                }
                off += got;
                if (feederMap.size() >= cap) {
                    terpotong = true;
                    break;
                }
            }

            // 2) Ambil semua baris lokal eCampus (proyeksi: feeder, id, [kode], nama).
            org.hibernate.Session ses = ais.database.hibernate.HibernateUtil.currentSession();
            Class kelas = Class.forName(k.classRef);
            boolean adaKode = meta.localKodeProp != null;
            org.hibernate.criterion.ProjectionList pl = org.hibernate.criterion.Projections.projectionList();
            pl.add(org.hibernate.criterion.Projections.property("feeder"));
            pl.add(org.hibernate.criterion.Projections.property("id"));
            if (adaKode) {
                pl.add(org.hibernate.criterion.Projections.property(meta.localKodeProp));
            }
            pl.add(org.hibernate.criterion.Projections.property(meta.localNamaProp));
            java.util.List lokal = ses.createCriteria(kelas).setProjection(pl).list();

            java.util.HashSet<String> lokalFeederSet = new java.util.HashSet<String>();
            java.util.List<String[]> lokalRows = new java.util.ArrayList<String[]>(); // {feeder, kode, nama}
            if (lokal != null) {
                for (int i = 0; i < lokal.size(); i++) {
                    Object[] r = (Object[]) lokal.get(i);
                    String fdr = str(r[0]).trim();
                    String kode = adaKode ? str(r[2]) : "";
                    String nama = adaKode ? str(r[3]) : str(r[2]);
                    if (fdr.length() > 0 && !fdr.equalsIgnoreCase("null")) {
                        lokalFeederSet.add(fdr);
                    }
                    lokalRows.add(new String[] { fdr, kode, nama });
                }
            }

            // 3) Hitung selisih.
            java.util.List<String[]> hanyaFeeder = new java.util.ArrayList<String[]>(); // {kode, nama, idFeeder}
            java.util.Iterator<java.util.Map.Entry<String, String[]>> it = feederMap.entrySet().iterator();
            while (it.hasNext()) {
                java.util.Map.Entry<String, String[]> en = it.next();
                if (!lokalFeederSet.contains(en.getKey())) {
                    hanyaFeeder.add(new String[] { en.getValue()[0], en.getValue()[1], en.getKey() });
                }
            }
            java.util.List<String[]> hanyaLokal = new java.util.ArrayList<String[]>(); // {kode, nama, feeder}
            int cocok = 0;
            for (int i = 0; i < lokalRows.size(); i++) {
                String[] r = lokalRows.get(i);
                String fdr = r[0];
                if (fdr != null && fdr.length() > 0 && !fdr.equalsIgnoreCase("null") && feederMap.containsKey(fdr)) {
                    cocok++;
                } else {
                    hanyaLokal.add(new String[] { r[1], r[2],
                        (fdr == null || fdr.length() == 0 || fdr.equalsIgnoreCase("null"))
                            ? "(belum punya kode feeder)" : fdr });
                }
            }

            renderHasilBanding(wadah, k, meta, feederMap.size(), lokalRows.size(), cocok,
                hanyaFeeder, hanyaLokal, terpotong);

        } catch (Exception ex) {
            tampilPesanBanding(wadah, "Gagal membandingkan: " + ex.getMessage(), true);
            Common.tampilErrorJikaAdmin(ex);
        }
    }

    /** Render hasil perbandingan (ringkasan + dua tabel selisih) ke {@code wadah}. */
    private void renderHasilBanding(Div wadah, Kartu k, MetaBanding meta, int totalFeeder, int totalLokal,
            int cocok, java.util.List<String[]> hanyaFeeder, java.util.List<String[]> hanyaLokal, boolean terpotong) {
        Common.clear(wadah);

        // Ringkasan angka.
        Div ring = new Div();
        ring.setStyle("display:flex; gap:12px; flex-wrap:wrap; margin-bottom:8px;");
        ring.setParent(wadah);
        ring.appendChild(kartuAngka("Total Feeder", String.valueOf(totalFeeder), "#166534"));
        ring.appendChild(kartuAngka("Total eCampus", String.valueOf(totalLokal), "#1e3a8a"));
        ring.appendChild(kartuAngka("Cocok (kode sama)", String.valueOf(cocok), "#0f766e"));
        ring.appendChild(kartuAngka("Hanya di Feeder", String.valueOf(hanyaFeeder.size()), "#b45309"));
        ring.appendChild(kartuAngka("Hanya di eCampus", String.valueOf(hanyaLokal.size()), "#9333ea"));

        if (terpotong) {
            Label t = new Label("Catatan: data Feeder dibatasi 50.000 baris pertama demi keamanan memori; "
                + "hasil mungkin belum mencakup seluruh baris.");
            t.setStyle("display:block; color:#92400e; font-size:11px; margin-bottom:6px;");
            t.setParent(wadah);
        }

        final int capTampil = 500;

        // Tabel 1: Hanya di Neo Feeder (perlu Ambil ke eCampus).
        Label h1 = new Label("Hanya ada di Neo Feeder — perlu \"Samakan eCampus ← Feeder (Ambil)\"  ("
            + hanyaFeeder.size() + " baris)");
        h1.setStyle("display:block; font-weight:700; color:#b45309; margin:10px 0 4px;");
        h1.setParent(wadah);
        buildGridBanding(new String[] { meta.labelKode, "Nama", "ID Feeder" }, hanyaFeeder, capTampil)
            .setParent(wadah);
        if (hanyaFeeder.size() > capTampil) {
            catatanCap(wadah, hanyaFeeder.size(), capTampil);
        }

        // Tabel 2: Hanya di eCampus (perlu Kirim ke Feeder).
        Label h2 = new Label("Hanya ada di eCampus — perlu \"Samakan Feeder ← eCampus (Kirim)\"  ("
            + hanyaLokal.size() + " baris)");
        h2.setStyle("display:block; font-weight:700; color:#9333ea; margin:14px 0 4px;");
        h2.setParent(wadah);
        buildGridBanding(new String[] { meta.labelKode, "Nama", "Kode Feeder" }, hanyaLokal, capTampil)
            .setParent(wadah);
        if (hanyaLokal.size() > capTampil) {
            catatanCap(wadah, hanyaLokal.size(), capTampil);
        }

        if (hanyaFeeder.isEmpty() && hanyaLokal.isEmpty()) {
            Label ok = new Label("Kedua sisi sudah SAMA — tidak ada baris yang hanya ada di salah satu sisi.");
            ok.setStyle("display:block; color:#166534; font-weight:700; margin-top:8px;");
            ok.setParent(wadah);
        }
    }

    /** Tabel selisih sederhana (Grid) dari daftar baris string. */
    private Grid buildGridBanding(String[] headers, java.util.List<String[]> rows, int cap) {
        Grid g = new MyGrid();
        g.setWidth("100%");
        g.setSclass("dgrid");
        g.setStyle("margin-bottom:2px;");
        Columns cs = new Columns();
        cs.setParent(g);
        for (int i = 0; i < headers.length; i++) {
            new Column(headers[i]).setParent(cs);
        }
        Rows rs = new Rows();
        rs.setParent(g);
        int n = Math.min(rows.size(), cap);
        if (n == 0) {
            Row r = new Row();
            r.setParent(rs);
            Label l = new Label("(tidak ada)");
            l.setStyle("color:#94a3b8; font-style:italic;");
            l.setParent(r);
        }
        for (int i = 0; i < n; i++) {
            String[] r = rows.get(i);
            Row row = new Row();
            row.setParent(rs);
            for (int j = 0; j < headers.length; j++) {
                String v = (j < r.length) ? r[j] : "";
                Label l = new Label(v == null ? "" : v);
                l.setStyle("white-space:nowrap;");
                l.setParent(row);
            }
        }
        return g;
    }

    private void catatanCap(Div wadah, int total, int cap) {
        Label l = new Label("Menampilkan " + cap + " dari " + total + " baris. Gunakan Ekspor Excel untuk "
            + "daftar lengkap, atau jalankan Samakan untuk menyelaraskan semuanya.");
        l.setStyle("display:block; color:#64748b; font-size:11px; margin-top:2px;");
        l.setParent(wadah);
    }

    private void tampilPesanBanding(Div wadah, String pesan, boolean error) {
        Common.clear(wadah);
        Label l = new Label(pesan);
        l.setStyle("display:block; padding:8px 0; font-weight:600; color:" + (error ? "#c00" : "#166534") + ";");
        l.setParent(wadah);
    }

    /** Ambil id Feeder dari kandidat field berurutan (mis. id_dosen lalu id_ptk). */
    private static String ambilIdFeeder(JSONObject o, String[] keys) {
        for (int i = 0; i < keys.length; i++) {
            String v = optStr(o, keys[i]);
            if (v != null && v.trim().length() > 0 && !v.trim().equalsIgnoreCase("null")) {
                return v.trim();
            }
        }
        return null;
    }

    /** optString aman: "" untuk key tak ada / JSON null. */
    private static String optStr(JSONObject o, String key) {
        if (o == null || key == null || !o.has(key) || o.isNull(key)) {
            return "";
        }
        try {
            return String.valueOf(o.get(key));
        } catch (Exception e) {
            return "";
        }
    }

    private static String str(Object o) {
        return o == null ? "" : String.valueOf(o);
    }

    /** Parse Long aman (mendukung "123" atau "123.0"); {@code null} bila gagal. */
    private static Long parseLongAman(String s) {
        if (s == null) {
            return null;
        }
        String v = s.trim();
        if (v.length() == 0) {
            return null;
        }
        try {
            int titik = v.indexOf('.');
            if (titik >= 0) {
                v = v.substring(0, titik);
            }
            return Long.valueOf(v);
        } catch (Exception e) {
            return null;
        }
    }

    // =========================================================
    // FILTER PENCARIAN (server-side) + BANDING INLINE (kolom eCampus)
    // =========================================================

    /** Gabungkan filter bawaan panel dengan filter pencarian menjadi satu klausa WHERE. */
    private static String filterEfektif(String filterDefault, String filterCari) {
        boolean a = filterDefault != null && filterDefault.trim().length() > 0;
        boolean b = filterCari != null && filterCari.trim().length() > 0;
        if (a && b) {
            return "(" + filterDefault + ") AND " + filterCari;
        }
        if (a) {
            return filterDefault;
        }
        if (b) {
            return filterCari;
        }
        return "";
    }

    /**
     * Pilih kolom yang layak dijadikan target pencarian umum dari {@code columnKeys} panel. Hanya
     * kolom teks yang lazim dicari (kode, nama, nim, nidn, email, telp/hp, nomor, alamat, judul).
     * Karena diambil dari columnKeys (kolom yang memang dikembalikan API), kolom dipastikan ada.
     */
    private static java.util.List<String> kolomCari(String[] columnKeys) {
        java.util.List<String> hasil = new java.util.ArrayList<String>();
        if (columnKeys == null) {
            return hasil;
        }
        String[] petunjuk = { "kode", "nama", "nim", "nidn", "email", "telp", "telepon", "hp",
            "no_", "nomor", "alamat", "judul", "gelar" };
        for (int i = 0; i < columnKeys.length; i++) {
            String key = columnKeys[i];
            if (key == null) {
                continue;
            }
            String low = key.toLowerCase();
            // Lewati kolom id_* (umumnya UUID/kode internal, bukan target cari alami).
            if (low.startsWith("id_") || low.startsWith("id")) {
                boolean tetap = false;
                for (int j = 0; j < petunjuk.length; j++) {
                    if (low.contains(petunjuk[j])) { tetap = true; break; }
                }
                if (!tetap) {
                    continue;
                }
            }
            for (int j = 0; j < petunjuk.length; j++) {
                if (low.contains(petunjuk[j])) {
                    hasil.add(key);
                    break;
                }
            }
        }
        return hasil;
    }

    /** Bangun klausa WHERE pencarian (ILIKE OR antar kolom umum). Tanda kutip di-escape. */
    private static String buildFilterCari(String q, java.util.List<String> kolCari) {
        if (q == null) {
            return "";
        }
        String v = q.trim();
        if (v.length() == 0 || kolCari == null || kolCari.isEmpty()) {
            return "";
        }
        String esc = v.replace("'", "''");
        StringBuilder sb = new StringBuilder();
        sb.append("(");
        for (int i = 0; i < kolCari.size(); i++) {
            if (i > 0) {
                sb.append(" OR ");
            }
            sb.append(kolCari.get(i)).append(" ilike '%").append(esc).append("%'");
        }
        sb.append(")");
        return sb.toString();
    }

    private static String gabung(java.util.List<String> list, String pemisah) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) {
                sb.append(pemisah);
            }
            sb.append(list.get(i));
        }
        return sb.toString();
    }

    /**
     * Susun urutan kolom render untuk mode banding inline: setiap kolom Feeder ({@code {"F",key,null}})
     * yang punya padanan lokal langsung diikuti kolom eCampus ({@code {"E",feederKey,localProp}}).
     */
    private java.util.List<String[]> kolomRender(String[] keys, MetaBanding meta) {
        java.util.List<String[]> out = new java.util.ArrayList<String[]>();
        out.add(new String[] { "A", null, null }); // kolom Aksi: tombol Restore per-record (paling kiri)
        for (int i = 0; i < keys.length; i++) {
            out.add(new String[] { "F", keys[i], null });
            String lp = localPropUntuk(meta, keys[i]);
            if (lp != null) {
                out.add(new String[] { "E", keys[i], lp });
            }
        }
        return out;
    }

    private String localPropUntuk(MetaBanding meta, String feederKey) {
        if (meta == null || meta.kolomBanding == null || feederKey == null) {
            return null;
        }
        for (int i = 0; i < meta.kolomBanding.length; i++) {
            if (feederKey.equals(meta.kolomBanding[i][0])) {
                return meta.kolomBanding[i][1];
            }
        }
        return null;
    }

    /** Bangun header kolom untuk mode banding inline (kolom eCampus diberi sufiks "(eCampus)"). */
    private void rebuildKolomBanding(Grid grid, String[] knownKeys, String[] knownHeaders,
            java.util.List<String[]> desc) {
        if (grid == null) {
            return;
        }
        Columns cols = grid.getColumns();
        if (cols == null) {
            cols = new Columns();
            cols.setParent(grid);
        }
        Common.clear(cols);
        MyColumnConfig no = new MyColumnConfig();
        no.setLabel("No");
        no.setWidth("40px");
        no.setParent(cols);
        for (int i = 0; i < desc.size(); i++) {
            String[] d = desc.get(i);
            MyColumnConfig c = new MyColumnConfig();
            if ("A".equals(d[0])) {
                c.setLabel("Restore");
                c.setWidth("90px");
            } else if ("E".equals(d[0])) {
                c.setLabel(labelKolom(d[1], knownKeys, knownHeaders) + " (eCampus)");
            } else {
                c.setLabel(labelKolom(d[1], knownKeys, knownHeaders));
            }
            c.setParent(cols);
        }
    }

    /**
     * Render baris untuk mode banding inline. Kolom Feeder tampil normal; kolom eCampus mengambil
     * nilai lokal (via {@code lokalMap} berdasarkan id Feeder baris), diberi latar BIRU bila sama
     * dan MERAH bila berbeda / tidak ada di eCampus.
     */
    private void renderRowsBanding(JSONArray data, java.util.List<String[]> desc, final MetaBanding meta,
            java.util.Map<String, java.util.Map<String, String>> lokalMap, Rows dataRows, int base,
            final String classRef) {
        if (data == null) {
            return;
        }
        for (int i = 0; i < data.length(); i++) {
            try {
                JSONObject obj = data.getJSONObject(i);
                Row row = new Row();
                row.setParent(dataRows);
                new Label(String.valueOf(base + i + 1)).setParent(row);

                String fid = ambilIdFeeder(obj, meta.feederIdKeys);
                java.util.Map<String, String> lokal = (fid == null) ? null : lokalMap.get(fid);

                for (int c = 0; c < desc.size(); c++) {
                    String[] d = desc.get(c);
                    if ("A".equals(d[0])) {
                        // Tombol Restore per-record (hanya bila record lokal ada / punya id).
                        String lid = (lokal == null) ? null : lokal.get("__id__");
                        if (lid != null && lid.trim().length() > 0) {
                            final Long idL = parseLongAman(lid);
                            MyToolbarbuttonConfig br = new MyToolbarbuttonConfig("Restore", "/img/svg/history.svg");
                            br.setStyle("color:#6d28d9; font-size:11px;");
                            br.setTooltiptext("Kembalikan record eCampus ini ke kondisi tanggal/revisi tertentu");
                            br.addEventListener("onClick", new EventListener() {
                                public void onEvent(Event ev) throws Exception {
                                    bukaRestore(classRef, idL);
                                }
                            });
                            br.setParent(row);
                        } else {
                            Label l = new Label("-");
                            l.setStyle("color:#94a3b8; font-size:11px;");
                            l.setParent(row);
                        }
                    } else if ("F".equals(d[0])) {
                        String key = d[1];
                        String val = obj.isNull(key) ? "" : obj.get(key).toString();
                        Label cell = new Label(val);
                        cell.setStyle("font-size:11px; white-space:nowrap;");
                        cell.setParent(row);
                    } else {
                        String feederKey = d[1];
                        String lp = d[2];
                        String fval = obj.isNull(feederKey) ? "" : obj.get(feederKey).toString();
                        String lval = (lokal == null) ? null : lokal.get(lp);
                        boolean beda = (lokal == null) || !samaNilai(fval, lval);
                        String tampil = (lokal == null) ? "(tidak ada di eCampus)"
                            : (lval == null ? "" : lval);
                        Label cell = new Label(tampil);
                        String bg = beda ? "#fee2e2" : "#eff6ff";
                        String fg = beda ? "#b91c1c" : "#1e3a8a";
                        cell.setStyle("font-size:11px; white-space:nowrap; background:" + bg + "; color:" + fg
                            + "; font-weight:600; padding:0 4px;");
                        cell.setParent(row);
                    }
                }
            } catch (Throwable t) {
                ais.common.ErrorAuditUtil.record(t, "auto-audit(empty-catch) DasbordSinkronisasiNeoFeeder.renderRowsBanding");
            }
        }
    }

    /**
     * Ambil nilai lokal eCampus untuk sekumpulan id Feeder (kolom {@code feeder}). Mengembalikan
     * peta {@code idFeeder -> {localProp -> nilai}} untuk properti yang ada di {@code meta.kolomBanding}.
     * Memakai {@code currentSession()} (tidak ditutup manual).
     */
    private java.util.Map<String, java.util.Map<String, String>> ambilLokalUntukId(
            String classRef, MetaBanding meta, java.util.Set<String> ids) {
        java.util.Map<String, java.util.Map<String, String>> hasil =
            new java.util.HashMap<String, java.util.Map<String, String>>();
        if (ids == null || ids.isEmpty() || classRef == null || meta == null) {
            return hasil;
        }
        try {
            org.hibernate.Session ses = ais.database.hibernate.HibernateUtil.currentSession();
            Class kelas = Class.forName(classRef);
            java.util.List<String> props = new java.util.ArrayList<String>();
            for (int i = 0; i < meta.kolomBanding.length; i++) {
                String p = meta.kolomBanding[i][1];
                if (!props.contains(p)) {
                    props.add(p);
                }
            }
            org.hibernate.criterion.ProjectionList pl = org.hibernate.criterion.Projections.projectionList();
            pl.add(org.hibernate.criterion.Projections.property("feeder"));
            pl.add(org.hibernate.criterion.Projections.property("id")); // untuk tombol Restore per-record
            for (int i = 0; i < props.size(); i++) {
                pl.add(org.hibernate.criterion.Projections.property(props.get(i)));
            }
            java.util.List baris = ses.createCriteria(kelas).setProjection(pl)
                .add(org.hibernate.criterion.Restrictions.in("feeder", new java.util.ArrayList<String>(ids)))
                .list();
            if (baris != null) {
                for (int i = 0; i < baris.size(); i++) {
                    Object[] r = (Object[]) baris.get(i);
                    String fdr = str(r[0]).trim();
                    if (fdr.length() == 0) {
                        continue;
                    }
                    java.util.Map<String, String> m = new java.util.HashMap<String, String>();
                    m.put("__id__", str(r[1])); // id record lokal (untuk Restore per-baris)
                    for (int j = 0; j < props.size(); j++) {
                        m.put(props.get(j), str(r[j + 2]));
                    }
                    hasil.put(fdr, m);
                }
            }
        } catch (Throwable t) {
            ais.common.ErrorAuditUtil.record(t, "auto-audit(empty-catch) DasbordSinkronisasiNeoFeeder.ambilLokalUntukId");
        }
        return hasil;
    }

    /** Perbandingan teks longgar: trim + abaikan besar/kecil huruf; dua-duanya kosong = sama. */
    private static boolean samaTeks(String a, String b) {
        String x = (a == null) ? "" : a.trim();
        String y = (b == null) ? "" : b.trim();
        return x.equalsIgnoreCase(y);
    }

    /**
     * Perbandingan nilai CERDAS untuk banding inline agar tidak ada MERAH palsu akibat beda format:
     * <ul>
     *   <li>teks sama (abaikan besar/kecil &amp; spasi) -&gt; sama;</li>
     *   <li>angka sama secara numerik (mis. {@code "3.00"} vs {@code "3"}) -&gt; sama;</li>
     *   <li>boolean setara (mis. {@code "1"}/{@code "true"}/{@code "ya"} vs {@code "0"}/{@code "false"}/{@code "tidak"}).</li>
     * </ul>
     */
    private static boolean samaNilai(String a, String b) {
        String x = (a == null) ? "" : a.trim();
        String y = (b == null) ? "" : b.trim();
        if (x.equalsIgnoreCase(y)) {
            return true;
        }
        Boolean bx = boolOf(x);
        Boolean by = boolOf(y);
        if (bx != null && by != null) {
            return bx.equals(by);
        }
        Double dx = numOf(x);
        Double dy = numOf(y);
        if (dx != null && dy != null) {
            return dx.doubleValue() == dy.doubleValue();
        }
        return false;
    }

    private static Boolean boolOf(String s) {
        String v = s.trim().toLowerCase();
        if (v.equals("1") || v.equals("true") || v.equals("ya") || v.equals("y") || v.equals("t")) {
            return Boolean.TRUE;
        }
        if (v.equals("0") || v.equals("false") || v.equals("tidak") || v.equals("n") || v.equals("f")) {
            return Boolean.FALSE;
        }
        return null;
    }

    private static Double numOf(String s) {
        try {
            return Double.valueOf(s.trim());
        } catch (Exception e) {
            return null;
        }
    }

    /** Kartu angka kecil (label + nilai besar berwarna) untuk ringkasan perbandingan. */
    private Div kartuAngka(String label, String value, String warna) {
        Div d = new Div();
        d.setStyle("background:#fff; border:1px solid #e2e8f0; border-radius:11px; padding:9px 15px;"
            + " min-width:120px; flex:1;");
        Label l = new Label(label);
        l.setStyle("display:block; font-size:11px; color:#64748b;");
        l.setParent(d);
        Label v = new Label(value);
        v.setStyle("display:block; font-size:23px; font-weight:800; line-height:1.2; color:" + warna + ";");
        v.setParent(d);
        return d;
    }

    /**
     * Membuat listener tombol "Samakan" — menjalankan aksi impor/ekspor entitas dengan pencatatan
     * status sinkronisasi (PROSES -&gt; sukses/ERROR), meniru persis perilaku tombol Ambil/Kirim di
     * toolbar tiap panel. Aksi umumnya membuka jendela Download/Upload yang menangani prosesnya
     * sendiri; karena itu dijalankan di thread UI (tidak di-thread-kan di sini).
     */
    private EventListener jalankanSamakan(final String judul, final String act, final String arah,
            final String statusOk, final PanelAction aksi) {
        return new EventListener() {
            public void onEvent(Event e) throws Exception {
                String host = hostSekarang();
                catatSync(act, judul, arah, NeoFeederSync.STATUS_PROSES,
                        null, null, null, null, "Mulai samakan (" + arah + ").", host);
                try {
                    aksi.run();
                    catatSync(act, judul, arah, statusOk,
                            null, null, null, null, "Selesai samakan (" + arah + ").", host);
                } catch (Exception ex) {
                    catatSync(act, judul, arah, NeoFeederSync.STATUS_ERROR,
                            null, null, null, null, ex.getMessage(), host);
                    Common.tampilErrorJikaAdmin(ex);
                    ais.ui.util.MyMessageboxConfig.show(
                        "Gagal menyamakan: " + ex.getMessage(), "Error",
                        ais.ui.util.MyMessageboxConfig.OK,
                        ais.ui.util.MyMessageboxConfig.EXCLAMATION
                    );
                }
            }
        };
    }

    // =========================================================
    // REFRESH LOGIC
    // =========================================================

    private void doRefreshPanel(
            String title,
            String actGet,
            String actCount,
            String filter,
            String[] columnKeys,
            String[] columnHeaders,
            Grid grid,
            Label countLabel,
            Label statusLabel,
            String classRef,
            Rows dataRows,
            Paging paging,
            PanelState state,
            boolean hasPaging) {

        // Filter efektif = filter bawaan panel DIGABUNG dengan filter dari kotak pencarian.
        final String filterEff = filterEfektif(filter, state.filterCari);

        // Payload permintaan untuk dicatat ke tabel NeoFeederSync (token disamarkan).
        final String req = buildRequestJson(actGet, filterEff, "",
                String.valueOf(PAGE_SIZE), String.valueOf(state.offset));
        String host = null;

        try {
            String[] kon = safeKoneksi();
            String ip      = kon[0];
            String portStr = kon[1];
            String username = kon[2];
            String password = kon[3];
            host = ip + ":" + portStr;

            FeederConnector fc = new FeederConnector(ip, Integer.parseInt(portStr));
            String token = fc.getToken(username, password);

            if (token == null || token.trim().isEmpty()) {
                showRowsError(dataRows, "Gagal mendapatkan token. Cek konfigurasi Neo Feeder.");
                countLabel.setValue("Error");
                terapkanStatus(statusLabel, NeoFeederSync.STATUS_ERROR, classRef, null, "Gagal mendapatkan token.");
                catatSync(actGet, title, NeoFeederSync.ARAH_AMBIL, NeoFeederSync.STATUS_ERROR,
                        req, null, null, null, "Gagal mendapatkan token.", host);
                return;
            }

            Integer jumlahFeeder = null;
            if (hasPaging && actCount != null && !actCount.isEmpty()) {
                Integer total = fc.getCount(token, actCount, filterEff);
                state.totalFeeder = (total == null ? 0 : total.intValue());
                jumlahFeeder = Integer.valueOf(state.totalFeeder);
                countLabel.setValue("Total di Feeder: " + state.totalFeeder
                    + (state.filterCari != null && state.filterCari.length() > 0 ? " (hasil cari)" : ""));
            }

            JSONArray data = fc.getData(
                actGet, token, filterEff, "",
                String.valueOf(PAGE_SIZE),
                String.valueOf(state.offset)
            );

            Common.clear(dataRows);

            if (data == null || data.length() == 0) {
                rebuildKolom(grid, columnKeys, columnHeaders, columnKeys);
                Row r = new Row();
                r.setParent(dataRows);
                Label lbl = new Label(ais.common.Common.getBahasaConfig("Tidak ada data."));
                lbl.setStyle("color:#999; font-style:italic;");
                lbl.setParent(r);
                if (hasPaging) paging.setVisible(false);
                if (!hasPaging) countLabel.setValue("0 data");
                terapkanStatus(statusLabel, NeoFeederSync.STATUS_TERSINGKRON, classRef, jumlahFeeder, null);
                catatSync(actGet, title, NeoFeederSync.ARAH_AMBIL, NeoFeederSync.STATUS_TERSINGKRON,
                        req, "[]", Integer.valueOf(0), jumlahFeeder, "Tidak ada data dari feeder.", host);
                return;
            }

            // Tampilkan SEMUA kolom yang dikembalikan API (bukan hanya subset) agar
            // pengecekan data lengkap & tidak ada yang tersembunyi.
            String[] keys = kunciDinamis(data, columnKeys);
            MetaBanding metaInline = state.bandingInline ? metaBanding(classRef) : null;
            if (metaInline != null) {
                // Mode banding inline: sisipkan kolom versi eCampus di samping kolom Feeder yang
                // sepadan (warna biru bila sama, MERAH bila berbeda / tak ada di eCampus).
                java.util.LinkedHashSet<String> idSet = new java.util.LinkedHashSet<String>();
                for (int i = 0; i < data.length(); i++) {
                    String id = ambilIdFeeder(data.optJSONObject(i), metaInline.feederIdKeys);
                    if (id != null) {
                        idSet.add(id);
                    }
                }
                java.util.Map<String, java.util.Map<String, String>> lokalMap =
                    ambilLokalUntukId(classRef, metaInline, idSet);
                java.util.List<String[]> desc = kolomRender(keys, metaInline);
                rebuildKolomBanding(grid, columnKeys, columnHeaders, desc);
                renderRowsBanding(data, desc, metaInline, lokalMap, dataRows, state.offset, classRef);
            } else {
                rebuildKolom(grid, columnKeys, columnHeaders, keys);
                renderRows(data, keys, dataRows, state.offset);
            }

            if (hasPaging) {
                paging.setTotalSize(state.totalFeeder);
                paging.setPageSize(PAGE_SIZE);
                paging.setVisible(state.totalFeeder > PAGE_SIZE);
            } else {
                countLabel.setValue(data.length() + " data (10 pertama)");
            }

            // Sukses ambil dari feeder -> perbarui status & tabel penampung.
            String resp;
            try {
                resp = data.toString();
            } catch (Throwable t) {
                resp = null;
            }
            terapkanStatus(statusLabel, NeoFeederSync.STATUS_TERSINGKRON, classRef, jumlahFeeder, null);
            catatSync(actGet, title, NeoFeederSync.ARAH_AMBIL, NeoFeederSync.STATUS_TERSINGKRON,
                    req, resp, Integer.valueOf(data.length()), jumlahFeeder, null, host);

        } catch (Exception ex) {
            showRowsError(dataRows, "Error: " + ex.getMessage());
            countLabel.setValue("Error");
            terapkanStatus(statusLabel, NeoFeederSync.STATUS_ERROR, classRef, null, ex.getMessage());
            catatSync(actGet, title, NeoFeederSync.ARAH_AMBIL, NeoFeederSync.STATUS_ERROR,
                    req, null, null, null, ex.getMessage(), host);
            Common.tampilErrorJikaAdmin(ex);
        }
    }

    /** Render baris dari JSONArray ke grid mengikuti urutan {@code keys}. Sel pakai
     *  white-space:nowrap supaya kolom melebar mengikuti isi (mendukung scroll horizontal). */
    private void renderRows(JSONArray data, String[] keys, Rows dataRows, int base) {
        if (data == null) {
            return;
        }
        for (int i = 0; i < data.length(); i++) {
            try {
                JSONObject obj = data.getJSONObject(i);
                Row row = new Row();
                row.setParent(dataRows);
                new Label(String.valueOf(base + i + 1)).setParent(row);
                for (int k = 0; k < keys.length; k++) {
                    String key = keys[k];
                    String val = obj.isNull(key) ? "" : obj.get(key).toString();
                    Label cell = new Label(val);
                    cell.setStyle("font-size:11px; white-space:nowrap;");
                    cell.setParent(row);
                }
            } catch (Throwable t) { ais.common.ErrorAuditUtil.record(t, "auto-audit(empty-catch) src/ais/action/master/feeder/DasbordSinkronisasiNeoFeeder.java:889");
                // lewati baris yang gagal di-parse
            }
        }
    }

    /**
     * Susun daftar kolom dinamis dari SELURUH key yang ada di respons API. Urutan: key
     * yang sudah dikenal ({@code knownKeys}) didahulukan (sesuai urutannya), lalu semua
     * key lain yang muncul di data ditambahkan — sehingga tidak ada kolom tersembunyi.
     */
    private String[] kunciDinamis(JSONArray data, String[] knownKeys) {
        java.util.LinkedHashSet<String> semua = new java.util.LinkedHashSet<String>();
        if (data != null) {
            for (int i = 0; i < data.length(); i++) {
                JSONObject o = data.optJSONObject(i);
                if (o == null) {
                    continue;
                }
                java.util.Iterator it = o.keys();
                while (it.hasNext()) {
                    Object k = it.next();
                    if (k != null) {
                        semua.add(k.toString());
                    }
                }
            }
        }
        java.util.LinkedHashSet<String> urut = new java.util.LinkedHashSet<String>();
        if (knownKeys != null) {
            for (int i = 0; i < knownKeys.length; i++) {
                if (semua.contains(knownKeys[i])) {
                    urut.add(knownKeys[i]);
                }
            }
        }
        urut.addAll(semua);
        return (String[]) urut.toArray(new String[urut.size()]);
    }

    /** Bangun ulang header kolom grid: kolom "No" + satu kolom per key. Label memakai
     *  header yang dikenal bila ada, selain itu memakai nama key apa adanya. */
    private void rebuildKolom(Grid grid, String[] knownKeys, String[] knownHeaders, String[] keys) {
        if (grid == null) {
            return;
        }
        Columns cols = grid.getColumns();
        if (cols == null) {
            cols = new Columns();
            cols.setParent(grid);
        }
        Common.clear(cols);
        MyColumnConfig no = new MyColumnConfig();
        no.setLabel("No");
        no.setWidth("40px");
        no.setParent(cols);
        for (int i = 0; i < keys.length; i++) {
            MyColumnConfig c = new MyColumnConfig();
            c.setLabel(labelKolom(keys[i], knownKeys, knownHeaders));
            c.setParent(cols);
        }
    }

    /** Label tampil untuk sebuah key: pakai header yang dikenal bila cocok, jika tidak
     *  pakai nama key mentah dari API. */
    private String labelKolom(String key, String[] knownKeys, String[] knownHeaders) {
        if (key != null && knownKeys != null && knownHeaders != null) {
            for (int i = 0; i < knownKeys.length && i < knownHeaders.length; i++) {
                if (key.equals(knownKeys[i])) {
                    return knownHeaders[i];
                }
            }
        }
        return key;
    }

    /**
     * Tampilkan data dari baris NeoFeederSync tersimpan (json_response) tanpa memanggil
     * feeder. Return true bila ada baris yang dirender.
     */
    private boolean muatDariTersimpan(NeoFeederSync row, String[] columnKeys, String[] columnHeaders, Grid grid,
            Label countLabel, Rows dataRows, Paging paging, PanelState state, boolean hasPaging) {
        if (row == null) {
            return false;
        }
        try {
            if (hasPaging && row.getJumlahFeeder() != null) {
                state.totalFeeder = row.getJumlahFeeder().intValue();
                countLabel.setValue("Total di Feeder: " + state.totalFeeder + " (tersimpan)");
            } else if (row.getJumlahData() != null) {
                countLabel.setValue(row.getJumlahData() + " data (tersimpan)");
            }
            String resp = row.getJsonResponse();
            if (resp == null || resp.trim().length() == 0) {
                return false;
            }
            JSONArray data = new JSONArray(resp);
            if (data.length() == 0) {
                return false;
            }
            Common.clear(dataRows);
            String[] keys = kunciDinamis(data, columnKeys);
            rebuildKolom(grid, columnKeys, columnHeaders, keys);
            renderRows(data, keys, dataRows, 0);
            if (hasPaging) {
                paging.setTotalSize(state.totalFeeder);
                paging.setPageSize(PAGE_SIZE);
                // Tampilkan pager saat jumlah data di feeder > 1 halaman (PAGE_SIZE).
                // Data tersimpan hanya 1 halaman pertama; klik halaman lain memicu
                // onPaging -> doRefreshPanel (ambil halaman tsb dari feeder).
                paging.setVisible(state.totalFeeder > PAGE_SIZE);
            }
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    // =========================================================
    // BADGE STATUS SINKRONISASI (vs data lokal eCampus)
    // =========================================================

    /**
     * Setel badge status panel. Bila {@code classRef} (class lokal tujuan) dikenal, jumlah
     * record lokal dibandingkan dengan jumlah di feeder untuk menandai apakah sudah
     * tersingkron dengan data lokal.
     */
    private void terapkanStatus(Label lbl, String status, String classRef, Integer jumlahFeeder, String pesan) {
        if (lbl == null) {
            return;
        }
        String teks;
        String warna;
        String bg;
        if (status == null || status.trim().isEmpty()) {
            teks = "Belum pernah disinkron";
            warna = "#374151"; bg = "#e5e7eb";
        } else if (NeoFeederSync.STATUS_ERROR.equals(status)) {
            teks = "Error";
            warna = "#7f1d1d"; bg = "#fee2e2";
        } else if (NeoFeederSync.STATUS_PROSES.equals(status)) {
            teks = "Proses...";
            warna = "#92400e"; bg = "#fef3c7";
        } else {
            // Tersingkron / Terkirim ke Feeder
            Long lokal = hitungLokalAman(classRef);
            if (classRef == null) {
                teks = status + " • lokal belum dipetakan";
                warna = "var(--ais-theme-primary,#1e3a8a)"; bg = "#dbeafe";
            } else if (lokal == null) {
                teks = status;
                warna = "#065f46"; bg = "#d1fae5";
            } else if (jumlahFeeder != null && jumlahFeeder.intValue() > 0
                    && lokal.longValue() >= jumlahFeeder.intValue()) {
                teks = "Tersingkron (Lokal " + lokal + " / Feeder " + jumlahFeeder + ")";
                warna = "#065f46"; bg = "#d1fae5";
            } else {
                teks = "Belum lengkap (Lokal " + lokal + " / Feeder "
                        + (jumlahFeeder == null ? "?" : jumlahFeeder) + ")";
                warna = "#92400e"; bg = "#fef3c7";
            }
        }
        lbl.setValue(teks);
        lbl.setStyle(STYLE_PILL_BASE + " color:" + warna + "; background:" + bg + ";");
        try {
            if (pesan != null && pesan.trim().length() > 0) {
                lbl.setTooltiptext(pesan);
            }
        } catch (Throwable abaikan) { ais.common.ErrorAuditUtil.record(abaikan, "auto-audit(empty-catch) src/ais/action/master/feeder/DasbordSinkronisasiNeoFeeder.java:1057");
        }
    }

    /** Overload: ambil status/jumlahFeeder/pesan dari baris NeoFeederSync tersimpan. */
    private void terapkanStatus(Label lbl, NeoFeederSync row, String classRef) {
        if (row == null) {
            terapkanStatus(lbl, null, classRef, null, null);
            return;
        }
        terapkanStatus(lbl, row.getStatus(), classRef, row.getJumlahFeeder(), row.getKeterangan());
    }

    /** Hitung jumlah record lokal untuk classRef via session request ZK (fail-safe). */
    private Long hitungLokalAman(String classRef) {
        if (classRef == null) {
            return null;
        }
        try {
            return NeoFeederSyncHelper.hitungLokal(
                    ais.database.hibernate.HibernateUtil.currentSession(), classRef);
        } catch (Throwable t) {
            return null;
        }
    }

    /** Jalankan refresh untuk SEMUA panel terdaftar (berurutan, tahan-error per panel). */
    private void refreshSemua() {
        int sukses = 0;
        int gagal = 0;
        for (int i = 0; i < refreshers.size(); i++) {
            PanelAction a = refreshers.get(i);
            try {
                a.run();
                sukses++;
            } catch (Throwable t) {
                gagal++;
                try {
                    Common.tampilErrorJikaAdmin(t instanceof Exception ? (Exception) t : new Exception(t));
                } catch (Throwable abaikan) { ais.common.ErrorAuditUtil.record(abaikan, "auto-audit(empty-catch) src/ais/action/master/feeder/DasbordSinkronisasiNeoFeeder.java:1096");
                }
            }
        }
        try {
            ais.ui.util.MyMessageboxConfig.show(
                "Refresh Semua selesai. Berhasil: " + sukses + ", Gagal: " + gagal + " dari "
                    + refreshers.size() + " panel.",
                "Refresh Semua", ais.ui.util.MyMessageboxConfig.OK, ais.ui.util.MyMessageboxConfig.INFORMATION);
        } catch (Throwable abaikan) { ais.common.ErrorAuditUtil.record(abaikan, "auto-audit(empty-catch) src/ais/action/master/feeder/DasbordSinkronisasiNeoFeeder.java:1105");
        }
    }

    // =========================================================
    // CEK VERSI API (audit act dasbor vs Neo Feeder via GetDictionary)
    // =========================================================

    /** Act tulis (kirim) yang dipakai helper sinkronisasi — ikut diaudit. */
    private static final String[] WRITE_ACTS = {
        "InsertBiodataMahasiswa", "UpdateBiodataMahasiswa",
        "InsertRiwayatPendidikanMahasiswa", "UpdateRiwayatPendidikanMahasiswa",
        "InsertMahasiswaLulusDO", "UpdateMahasiswaLulusDO",
        "InsertKelasKuliah", "UpdateKelasKuliah",
        "InsertDosenPengajarKelasKuliah", "UpdateDosenPengajarKelasKuliah",
        "InsertMataKuliah", "UpdateMataKuliah",
        "InsertKurikulum", "UpdateKurikulum",
        "InsertMatkulKurikulum", "UpdateMatkulKurikulum",
        "InsertNilaiTransferPendidikanMahasiswa", "UpdateNilaiTransferPendidikanMahasiswa",
        "InsertPrestasiMahasiswa", "UpdatePrestasiMahasiswa",
        "InsertAktivitasMahasiswa", "UpdateAktivitasMahasiswa", "InsertAnggotaAktivitasMahasiswa",
        "InsertBimbingMahasiswa", "InsertUjiMahasiswa",
        "InsertKomponenEvaluasiKelas", "UpdateKomponenEvaluasiKelas", "DeleteKomponenEvaluasiKelas",
        "UpdateNilaiPerkuliahanKelasKomponenEvaluasi"
    };

    /** Buka jendela hasil + tunda pemeriksaan (UI muncul dulu, lalu cek berjalan). */
    private void cekVersiApi() {
        final MyWindow w = new MyWindow("Cek Versi API Neo Feeder", "normal", true);
        w.setWidth("88%");
        w.setHeight("88%");
        w.setParent(this);
        final Vbox box = new Vbox();
        box.setWidth("100%");
        box.setStyle("padding:12px; max-height:80vh; overflow:auto;");
        box.setParent(w);
        Label loading = new Label("Sedang memeriksa fungsi API ke Neo Feeder lewat GetDictionary "
                + "(aman, hanya membaca). Mohon tunggu, bisa 1-2 menit...");
        loading.setStyle("font-size:13px; color:#475569;");
        loading.setParent(box);
        w.doHighlighted();

        Common.createDefaultTimer(new EventListener() {
            public void onEvent(Event ev) throws Exception {
                jalankanCekVersiApi(box);
            }
        });
    }

    private void jalankanCekVersiApi(Vbox box) {
        try {
            String[] kon = safeKoneksi();
            String host = kon[0] + ":" + kon[1];
            FeederConnector fc = new FeederConnector(kon[0], Integer.parseInt(kon[1]));
            String token = fc.getToken(kon[2], kon[3]);
            if (token == null || token.trim().isEmpty()) {
                Common.clear(box);
                Label l = new Label(ais.common.Common.getBahasaConfig("Gagal mendapatkan token. Periksa 'Pengaturan Koneksi'."));
                l.setStyle("color:#c00; font-weight:600;");
                l.setParent(box);
                return;
            }

            // Kumpulkan act unik: dari registry panel (actGet + actCount) + act tulis.
            java.util.LinkedHashMap<String, String> peta = new java.util.LinkedHashMap<String, String>();
            for (int i = 0; i < cekRegistry.size(); i++) {
                String[] r = cekRegistry.get(i);
                String judul = r[0], get = r[1], cnt = r[2];
                if (get != null && get.trim().length() > 0 && !peta.containsKey(get)) {
                    peta.put(get, judul);
                }
                if (cnt != null && cnt.trim().length() > 0 && !peta.containsKey(cnt)) {
                    peta.put(cnt, judul + " (jumlah)");
                }
            }
            for (int i = 0; i < WRITE_ACTS.length; i++) {
                if (!peta.containsKey(WRITE_ACTS[i])) {
                    peta.put(WRITE_ACTS[i], "Sinkronisasi (Kirim)");
                }
            }

            int total = 0, ada = 0, tidak = 0;
            java.util.List<String[]> hasil = new java.util.ArrayList<String[]>(); // {act, grup, status, ket}
            for (java.util.Iterator<java.util.Map.Entry<String, String>> it = peta.entrySet().iterator(); it
                    .hasNext();) {
                java.util.Map.Entry<String, String> e = it.next();
                String act = e.getKey();
                org.json.JSONObject d = fc.getDictionary(token, act);
                boolean ok = dictAda(d);
                String ket = d.isNull("error_desc") ? "" : String.valueOf(d.opt("error_desc"));
                if (ok && (ket == null || ket.length() == 0)) {
                    ket = "Tersedia";
                }
                total++;
                if (ok) {
                    ada++;
                } else {
                    tidak++;
                }
                hasil.add(new String[] { act, e.getValue(), ok ? "OK" : "TIDAK ADA", ket });
            }

            // urutkan: yang TIDAK ADA di atas
            java.util.Collections.sort(hasil, new java.util.Comparator<String[]>() {
                public int compare(String[] a, String[] b) {
                    int sa = "TIDAK ADA".equals(a[2]) ? 0 : 1;
                    int sb = "TIDAK ADA".equals(b[2]) ? 0 : 1;
                    if (sa != sb) {
                        return sa - sb;
                    }
                    return a[0].compareToIgnoreCase(b[0]);
                }
            });

            tampilkanHasilCek(box, host, total, ada, tidak, hasil);
        } catch (Exception ex) {
            Common.clear(box);
            Label l = new Label("Terjadi kesalahan saat memeriksa: " + ex.getMessage());
            l.setStyle("color:#c00; font-weight:600;");
            l.setParent(box);
            Common.tampilErrorJikaAdmin(ex);
        }
    }

    /** Tersedia bila error_code = 0 dan ada isi 'data'. */
    private boolean dictAda(org.json.JSONObject d) {
        try {
            if (d == null) {
                return false;
            }
            String ec = d.isNull("error_code") ? "" : String.valueOf(d.opt("error_code"));
            if (!"0".equals(ec)) {
                return false;
            }
            if (d.isNull("data")) {
                return false;
            }
            String ds = String.valueOf(d.opt("data")).trim();
            return ds.length() > 0 && !ds.equals("[]") && !ds.equals("{}") && !ds.equalsIgnoreCase("null");
        } catch (Exception e) {
            return false;
        }
    }

    private void tampilkanHasilCek(Vbox box, String host, int total, int ada, int tidak,
            java.util.List<String[]> hasil) {
        Common.clear(box);

        Div ringkas = new Div();
        ringkas.setStyle("padding:10px 12px; border-radius:10px; margin-bottom:10px; "
                + (tidak == 0 ? "background:#dcfce7; border:1px solid #86efac;"
                        : "background:#fef9c3; border:1px solid #fde047;"));
        ringkas.setParent(box);
        Label judul = new Label(tidak == 0 ? "Semua fungsi API dasbor COCOK dengan Neo Feeder saat ini."
                : "Ada " + tidak + " fungsi API yang TIDAK tersedia / berubah — perlu disesuaikan.");
        judul.setStyle("font-size:14px; font-weight:800; display:block; color:"
                + (tidak == 0 ? "#166534" : "#854d0e") + ";");
        judul.setParent(ringkas);
        Label sub = new Label("Server: " + host + "  |  Diperiksa: " + total + "  |  Tersedia: " + ada
                + "  |  Tidak tersedia: " + tidak + "  |  Waktu: "
                + Common.dateFormat.get().format(ais.ui.util.WaktuUtil.getDate()));
        sub.setStyle("font-size:12px; color:#475569; display:block; margin-top:3px;");
        sub.setParent(ringkas);

        Grid grid = new MyGrid();
        grid.setWidth("100%");
        grid.setSclass("dgrid");
        grid.setParent(box);
        Columns cols = new Columns();
        cols.setSizable(true);
        cols.setParent(grid);
        MyColumnConfig cNo = new MyColumnConfig("No");
        cNo.setWidth("48px");
        cNo.setParent(cols);
        new MyColumnConfig("Fungsi (act)").setParent(cols);
        new MyColumnConfig("Dipakai di").setParent(cols);
        MyColumnConfig cS = new MyColumnConfig("Status");
        cS.setWidth("110px");
        cS.setParent(cols);
        new MyColumnConfig("Keterangan").setParent(cols);
        Rows rows = new Rows();
        rows.setParent(grid);
        for (int i = 0; i < hasil.size(); i++) {
            String[] r = hasil.get(i);
            boolean miss = "TIDAK ADA".equals(r[2]);
            Row row = new Row();
            if (miss) {
                row.setStyle("background:#fff1f2;");
            }
            row.setParent(rows);
            new Label(String.valueOf(i + 1)).setParent(row);
            Label fa = new Label(r[0]);
            fa.setStyle("font-size:12px; font-weight:600;");
            fa.setParent(row);
            Label gp = new Label(r[1]);
            gp.setStyle("font-size:11px; color:#64748b;");
            gp.setParent(row);
            Label st = new Label(miss ? "TIDAK ADA" : "OK");
            st.setStyle("font-size:11px; font-weight:800; padding:1px 8px; border-radius:10px; color:"
                    + (miss ? "#7f1d1d; background:#fee2e2;" : "#065f46; background:#d1fae5;"));
            st.setParent(row);
            Label kt = new Label(r[3] == null ? "" : r[3]);
            kt.setStyle("font-size:11px; color:#475569;");
            kt.setParent(row);
        }
    }

    // =========================================================
    // PENCATATAN KE TABEL PENAMPUNG (NeoFeederSync)
    // =========================================================

    /** Bangun JSON permintaan untuk dicatat; token sengaja disamarkan ("***"). */
    private String buildRequestJson(String act, String filter, String order, String limit, String offset) {
        try {
            JSONObject o = new JSONObject();
            o.put("act", act == null ? "" : act);
            o.put("token", "***");
            o.put("filter", filter == null ? "" : filter);
            o.put("order", order == null ? "" : order);
            o.put("limit", limit == null ? "" : limit);
            o.put("offset", offset == null ? "" : offset);
            return o.toString();
        } catch (Throwable t) {
            return "{\"act\":\"" + act + "\"}";
        }
    }

    /** Pembungkus fail-safe ke {@link NeoFeederSyncHelper#catat}. */
    private void catatSync(String aksi, String nama, String arah, String status, String req, String resp,
            Integer jumlahData, Integer jumlahFeeder, String pesan, String host) {
        try {
            NeoFeederSyncHelper.catat(aksi, nama, arah, status, req, resp, jumlahData, jumlahFeeder, pesan, host);
        } catch (Throwable t) { ais.common.ErrorAuditUtil.record(t, "auto-audit(empty-catch) src/ais/action/master/feeder/DasbordSinkronisasiNeoFeeder.java:1337");
            // tidak boleh mengganggu alur utama
        }
    }

    /** Ambil string host (ip:port) dari konfigurasi tanpa memanggil jaringan. */
    private String hostSekarang() {
        try {
            String[] kon = safeKoneksi();
            return kon[0] + ":" + kon[1];
        } catch (Throwable t) {
            return null;
        }
    }

    private void showRowsError(Rows rows, String msg) {
        Common.clear(rows);
        Row r = new Row();
        r.setStyle("color:#c00; background:#fff0f0;");
        r.setParent(rows);
        new Label(msg).setParent(r);
    }

    // =========================================================
    // SEKSI 1 — DOSEN
    // =========================================================

    private Panel buildPanelDosen() {
        return buildFeederPanel(
            "3.4 Dosen (GetListDosen)",
            "GetListDosen", "GetCountDosen", "",
            new String[]{"nidn", "nama_dosen", "jenis_kelamin",
                "id_status_kepegawaian", "id_jabatan_fungsional"},
            new String[]{"NIDN", "Nama Dosen", "JK", "Status Pegawai", "Jabatan Fungsional"},
            null, null
        );
    }

    private Panel buildPanelPenugasanSemuaDosen() {
        return buildFeederPanel(
            "3.14 Penugasan Semua Dosen (GetListPenugasanSemuaDosen)",
            "GetListPenugasanSemuaDosen", "GetCountPenugasanSemuaDosen", "",
            new String[]{"nidn", "nama_dosen", "program_studi",
                "nama_tahun_ajaran", "nomor_surat_tugas", "apakah_homebase"},
            new String[]{"NIDN", "Nama Dosen", "Prodi",
                "Tahun Ajaran", "No. SK Tugas", "Homebase"},
            null, null
        );
    }

    private Panel buildPanelAktivitasMengajarDosen() {
        return buildFeederPanel(
            "3.7 Aktivitas Mengajar Dosen (GetAktivitasMengajarDosen)",
            "GetAktivitasMengajarDosen", "GetCountAktivitasMengajarDosen", "",
            new String[]{"nidn", "nama_dosen", "nama_mata_kuliah",
                "nama_kelas_kuliah", "rencana_minggu_pertemuan", "realisasi_minggu_pertemuan"},
            new String[]{"NIDN", "Nama Dosen", "Mata Kuliah",
                "Kelas", "Rencana", "Realisasi"},
            new PanelAction() {
                public void run() throws Exception {
                    openWindow(new DownloadAjarDosen(),
                        "Ambil Aktivitas Mengajar Dosen dari Neo Feeder", "85%", "80%");
                }
            },
            new PanelAction() {
                public void run() throws Exception {
                    openWindow(new UploadAjarDosen(),
                        "Kirim Aktivitas Mengajar Dosen ke Neo Feeder", "85%", "80%");
                }
            }
        );
    }

    private Panel buildPanelDosenPengajarKelas() {
        return buildFeederPanel(
            "3.15 Dosen Pengajar Kelas Kuliah (GetDosenPengajarKelasKuliah)",
            "GetDosenPengajarKelasKuliah", "GetCountDosenPengajarKelasKuliah", "",
            new String[]{"nidn", "nama_dosen", "nama_kelas_kuliah",
                "sks_substansi_total", "rencana_minggu_pertemuan",
                "realisasi_minggu_pertemuan", "nama_jenis_evaluasi"},
            new String[]{"NIDN", "Nama Dosen", "Kelas Kuliah",
                "SKS", "Rencana", "Realisasi", "Jenis Evaluasi"},
            new PanelAction() {
                public void run() throws Exception {
                    openWindow(new DownloadAjarDosen(),
                        "Ambil Dosen Pengajar Kelas dari Neo Feeder", "85%", "80%");
                }
            },
            new PanelAction() {
                public void run() throws Exception {
                    openWindow(new UploadAjarDosen(),
                        "Kirim Dosen Pengajar Kelas ke Neo Feeder", "85%", "80%");
                }
            }
        );
    }

    private Panel buildPanelDosenPembimbing() {
        return buildFeederPanel(
            "3.25 Dosen Pembimbing Mahasiswa (GetDosenPembimbing)",
            "GetDosenPembimbing", "GetCountDosenPembimbing", "",
            new String[]{"nim", "nama_mahasiswa",
                "nidn", "nama_dosen", "pembimbing_ke", "jenis_aktivitas"},
            new String[]{"NIM", "Nama Mahasiswa",
                "NIDN Dosen", "Nama Dosen", "Pembimbing ke-", "Jenis Aktivitas"},
            null, null
        );
    }

    private Panel buildPanelBimbingMahasiswa() {
        return buildFeederPanel(
            "3.142 Bimbingan Mahasiswa (GetListBimbingMahasiswa)",
            "GetListBimbingMahasiswa", "GetCountMahasiswaBimbinganDosen", "",
            new String[]{"judul", "nidn", "nama_dosen",
                "nama_kategori_kegiatan", "pembimbing_ke"},
            new String[]{"Judul Aktivitas", "NIDN Dosen",
                "Nama Dosen", "Kategori Kegiatan", "Pembimbing ke-"},
            null, null
        );
    }

    private Panel buildPanelRiwayatFungsionalDosen() {
        return buildFeederPanel(
            "3.8 Riwayat Fungsional Dosen (GetRiwayatFungsionalDosen)",
            "GetRiwayatFungsionalDosen", null, "",
            new String[]{"nidn", "nama_dosen", "nama_jabatan_fungsional",
                "sk_jabatan_fungsional", "mulai_sk_jabatan"},
            new String[]{"NIDN", "Nama Dosen", "Jabatan Fungsional",
                "No. SK", "Mulai SK"},
            new PanelAction() {
                public void run() throws Exception {
                    openWindow(new DownloadHistory(),
                        "Ambil Riwayat Fungsional Dosen dari Neo Feeder", "85%", "80%");
                }
            },
            null
        );
    }

    private Panel buildPanelRiwayatPangkatDosen() {
        return buildFeederPanel(
            "3.9 Riwayat Pangkat Dosen (GetRiwayatPangkatDosen)",
            "GetRiwayatPangkatDosen", null, "",
            new String[]{"nidn", "nama_dosen", "nama_pangkat_golongan",
                "sk_pangkat", "tanggal_sk_pangkat", "mulai_sk_pangkat"},
            new String[]{"NIDN", "Nama Dosen", "Pangkat/Gol",
                "No. SK", "Tgl SK", "Mulai SK"},
            new PanelAction() {
                public void run() throws Exception {
                    openWindow(new DownloadHistory(),
                        "Ambil Riwayat Pangkat Dosen dari Neo Feeder", "85%", "80%");
                }
            },
            null
        );
    }

    private Panel buildPanelRiwayatSertifikasiDosen() {
        return buildFeederPanel(
            "3.11 Riwayat Sertifikasi Dosen (GetRiwayatSertifikasiDosen)",
            "GetRiwayatSertifikasiDosen", null, "",
            new String[]{"nidn", "nama_dosen", "nama_bidang_studi",
                "nama_jenis_sertifikasi", "tahun_sertifikasi", "sk_sertifikasi"},
            new String[]{"NIDN", "Nama Dosen", "Bidang Studi",
                "Jenis Sertifikasi", "Tahun", "No. SK"},
            new PanelAction() {
                public void run() throws Exception {
                    openWindow(new DownloadHistory(),
                        "Ambil Riwayat Sertifikasi Dosen dari Neo Feeder", "85%", "80%");
                }
            },
            null
        );
    }

    private Panel buildPanelRiwayatPenelitianDosen() {
        return buildFeederPanel(
            "3.12 Riwayat Penelitian Dosen (GetRiwayatPenelitianDosen)",
            "GetRiwayatPenelitianDosen", null, "",
            new String[]{"nidn", "nama_dosen", "judul_penelitian",
                "nama_kelompok_bidang", "nama_lembaga_iptek", "tahun_kegiatan"},
            new String[]{"NIDN", "Nama Dosen", "Judul Penelitian",
                "Bidang", "Lembaga", "Tahun"},
            new PanelAction() {
                public void run() throws Exception {
                    openWindow(new DownloadHistory(),
                        "Ambil Riwayat Penelitian Dosen dari Neo Feeder", "85%", "80%");
                }
            },
            null
        );
    }

    // =========================================================
    // SEKSI 2 — MAHASISWA
    // =========================================================

    private Panel buildPanelMahasiswa() {
        return buildFeederPanel(
            "3.110 Mahasiswa (GetListMahasiswa)",
            "GetListMahasiswa", "GetCountMahasiswa", "",
            new String[]{"nim", "nama_mahasiswa", "nama_program_studi",
                "nama_status_mahasiswa", "nama_periode_masuk"},
            new String[]{"NIM", "Nama Mahasiswa", "Program Studi",
                "Status", "Periode Masuk"},
            new PanelAction() {
                public void run() throws Exception {
                    openWindow(new DownloadMahasiswa(),
                        "Ambil Data Mahasiswa dari Neo Feeder", "85%", "85%");
                }
            },
            new PanelAction() {
                public void run() throws Exception {
                    openWindow(new KirimKeFeederWindow("mahasiswa"),
                        "Kirim Mahasiswa ke Neo Feeder", "70%", "60%");
                }
            }
        );
    }

    private Panel buildPanelKelulusan() {
        return buildFeederPanel(
            "3.162 Kelulusan / Mahasiswa Lulus-DO (GetListMahasiswaLulusDO)",
            "GetListMahasiswaLulusDO", "GetCountMahasiswaLulusDO", "",
            new String[]{"nim", "nama_mahasiswa",
                "id_status_mahasiswa", "tanggal_lulus", "ipk"},
            new String[]{"NIM", "Nama Mahasiswa",
                "Status", "Tgl Lulus", "IPK"},
            new PanelAction() {
                public void run() throws Exception {
                    openWindow(new DownloadKelulusan(),
                        "Ambil Data Kelulusan dari Neo Feeder", "85%", "80%");
                }
            },
            new PanelAction() {
                public void run() throws Exception {
                    openWindow(new UploadKelulusan(),
                        "Kirim Data Kelulusan ke Neo Feeder", "85%", "80%");
                }
            }
        );
    }

    private Panel buildPanelPrestasi() {
        return buildFeederPanel(
            "3.144 Prestasi Mahasiswa (GetListPrestasiMahasiswa)",
            "GetListPrestasiMahasiswa", "GetCountPrestasiMahasiswa", "",
            new String[]{"nim", "nama_mahasiswa", "nama_jenis_prestasi",
                "nama_tingkat_prestasi", "nama_prestasi", "tahun_prestasi"},
            new String[]{"NIM", "Nama Mahasiswa", "Jenis Prestasi",
                "Tingkat", "Nama Prestasi", "Tahun"},
            new PanelAction() {
                public void run() throws Exception {
                    openWindow(new DownloadPrestasiMahasiswa(),
                        "Ambil Data Prestasi Mahasiswa dari Neo Feeder", "85%", "80%");
                }
            },
            new PanelAction() {
                public void run() throws Exception {
                    openWindow(new UploadPrestasiMahasiswa(),
                        "Kirim Data Prestasi Mahasiswa ke Neo Feeder", "85%", "80%");
                }
            }
        );
    }

    private Panel buildPanelAktivitasMahasiswa() {
        return buildFeederPanel(
            "3.140 Aktivitas Mahasiswa (GetListAktivitasMahasiswa)",
            "GetListAktivitasMahasiswa", "GetCountAktivitasMahasiswa", "",
            new String[]{"id_jenis_aktivitas", "nama_jenis_aktivitas",
                "id_prodi", "id_semester", "judul", "untuk_kampus_merdeka"},
            new String[]{"ID Jenis", "Jenis Aktivitas",
                "Prodi", "Semester", "Judul", "Kampus Merdeka"},
            new PanelAction() {
                public void run() throws Exception {
                    openWindow(new DownloadAktifitasMahasiwaSkripsi(),
                        "Ambil Data Aktivitas Skripsi dari Neo Feeder", "85%", "80%");
                }
            },
            null,
            new AksiTambahan[]{
                new AksiTambahan("Ambil KKN", "Ambil data Aktivitas KKN dari Neo Feeder", new PanelAction() {
                    public void run() throws Exception {
                        openWindow(new DownloadAktifitasMahasiwaKkn(),
                            "Ambil Data Aktivitas KKN dari Neo Feeder", "85%", "80%");
                    }
                }),
                new AksiTambahan("Ambil PKL", "Ambil data Aktivitas PKL/Magang dari Neo Feeder", new PanelAction() {
                    public void run() throws Exception {
                        openWindow(new DownloadAktifitasMahasiwaPkl(),
                            "Ambil Data Aktivitas PKL dari Neo Feeder", "85%", "80%");
                    }
                }),
                new AksiTambahan("Ambil Tugas Akhir", "Ambil data Aktivitas Tugas Akhir dari Neo Feeder", new PanelAction() {
                    public void run() throws Exception {
                        openWindow(new DownloadAktifitasMahasiwaMahasiswaRequestTugasAkhir(),
                            "Ambil Data Aktivitas Tugas Akhir dari Neo Feeder", "85%", "80%");
                    }
                }),
                new AksiTambahan("Kirim KKN", "Kirim Aktivitas KKN ke Neo Feeder", new PanelAction() {
                    public void run() throws Exception {
                        openWindow(new KirimKeFeederWindow("kkn"),
                            "Kirim Aktivitas KKN ke Neo Feeder", "70%", "60%");
                    }
                }),
                new AksiTambahan("Kirim PKL", "Kirim Aktivitas PKL ke Neo Feeder", new PanelAction() {
                    public void run() throws Exception {
                        openWindow(new KirimKeFeederWindow("pkl"),
                            "Kirim Aktivitas PKL ke Neo Feeder", "70%", "60%");
                    }
                }),
                new AksiTambahan("Kirim Skripsi", "Kirim Aktivitas Skripsi ke Neo Feeder", new PanelAction() {
                    public void run() throws Exception {
                        openWindow(new KirimKeFeederWindow("skripsi"),
                            "Kirim Aktivitas Skripsi ke Neo Feeder", "70%", "60%");
                    }
                }),
                new AksiTambahan("Kirim Tugas Akhir", "Kirim Aktivitas Tugas Akhir ke Neo Feeder", new PanelAction() {
                    public void run() throws Exception {
                        openWindow(new KirimKeFeederWindow("tugasakhir"),
                            "Kirim Aktivitas Tugas Akhir ke Neo Feeder", "70%", "60%");
                    }
                }),
                new AksiTambahan("Kirim Kegiatan", "Kirim Kegiatan Kemahasiswaan (disetujui) ke Neo Feeder", new PanelAction() {
                    public void run() throws Exception {
                        openWindow(new KirimKeFeederWindow("kegiatan"),
                            "Kirim Kegiatan Kemahasiswaan ke Neo Feeder", "70%", "60%");
                    }
                }),
                new AksiTambahan("Kirim Penghargaan", "Kirim Penghargaan Mahasiswa (disetujui) ke Neo Feeder", new PanelAction() {
                    public void run() throws Exception {
                        openWindow(new KirimKeFeederWindow("penghargaan"),
                            "Kirim Penghargaan Mahasiswa ke Neo Feeder", "70%", "60%");
                    }
                }),
                new AksiTambahan("Kirim Formulir Kegiatan", "Kirim Formulir Kegiatan (aktivitas) ke Neo Feeder", new PanelAction() {
                    public void run() throws Exception {
                        openWindow(new KirimKeFeederWindow("formulir"),
                            "Kirim Formulir Kegiatan ke Neo Feeder", "70%", "60%");
                    }
                })
            }
        );
    }

    private Panel buildPanelAnggotaAktivitasMahasiswa() {
        return buildFeederPanel(
            "3.141 Anggota Aktivitas Mahasiswa (GetListAnggotaAktivitasMahasiswa)",
            "GetListAnggotaAktivitasMahasiswa", null, "",
            new String[]{"judul", "nim", "nama_mahasiswa",
                "jenis_peran", "nama_jenis_peran"},
            new String[]{"Judul Aktivitas", "NIM",
                "Nama Mahasiswa", "Jenis Peran", "Nama Peran"},
            null, null,
            new AksiTambahan[]{
                new AksiTambahan("Ambil Anggota Skripsi", "Ambil anggota/peserta aktivitas Skripsi dari Neo Feeder", new PanelAction() {
                    public void run() throws Exception {
                        openWindow(new DownloadAktifitasMahasiwaSkripsiPesertaMahasiswa(),
                            "Ambil Anggota Aktivitas Skripsi dari Neo Feeder", "85%", "80%");
                    }
                }),
                new AksiTambahan("Ambil Anggota KKN", "Ambil anggota/peserta aktivitas KKN dari Neo Feeder", new PanelAction() {
                    public void run() throws Exception {
                        openWindow(new DownloadAktifitasMahasiwaKknPesertaMahasiswa(),
                            "Ambil Anggota Aktivitas KKN dari Neo Feeder", "85%", "80%");
                    }
                }),
                new AksiTambahan("Ambil Anggota PKL", "Ambil anggota/peserta aktivitas PKL dari Neo Feeder", new PanelAction() {
                    public void run() throws Exception {
                        openWindow(new DownloadAktifitasMahasiwaPklPesertaMahasiswa(),
                            "Ambil Anggota Aktivitas PKL dari Neo Feeder", "85%", "80%");
                    }
                }),
                new AksiTambahan("Ambil Anggota Tugas Akhir", "Ambil anggota/peserta aktivitas Tugas Akhir dari Neo Feeder", new PanelAction() {
                    public void run() throws Exception {
                        openWindow(new DownloadAktifitasMahasiwaMahasiswaRequestTugasAkhirPesertaMahasiswa(),
                            "Ambil Anggota Aktivitas Tugas Akhir dari Neo Feeder", "85%", "80%");
                    }
                }),
                new AksiTambahan("Ambil Dosen Skripsi", "Ambil dosen pembimbing/penguji aktivitas Skripsi dari Neo Feeder", new PanelAction() {
                    public void run() throws Exception {
                        openWindow(new DownloadAktifitasMahasiwaSkripsiPesertaDosen(),
                            "Ambil Dosen Aktivitas Skripsi dari Neo Feeder", "85%", "80%");
                    }
                }),
                new AksiTambahan("Ambil Dosen KKN", "Ambil dosen pembimbing aktivitas KKN dari Neo Feeder", new PanelAction() {
                    public void run() throws Exception {
                        openWindow(new DownloadAktifitasMahasiwaKknPesertaDosen(),
                            "Ambil Dosen Aktivitas KKN dari Neo Feeder", "85%", "80%");
                    }
                }),
                new AksiTambahan("Ambil Dosen PKL", "Ambil dosen pembimbing aktivitas PKL dari Neo Feeder", new PanelAction() {
                    public void run() throws Exception {
                        openWindow(new DownloadAktifitasMahasiwaPklPesertaDosen(),
                            "Ambil Dosen Aktivitas PKL dari Neo Feeder", "85%", "80%");
                    }
                }),
                new AksiTambahan("Ambil Dosen Tugas Akhir", "Ambil dosen pembimbing aktivitas Tugas Akhir dari Neo Feeder", new PanelAction() {
                    public void run() throws Exception {
                        openWindow(new DownloadAktifitasMahasiwaMahasiswaRequestTugasAkhirPesertaDosen(),
                            "Ambil Dosen Aktivitas Tugas Akhir dari Neo Feeder", "85%", "80%");
                    }
                })
            }
        );
    }

    // Panel 12 – FIXED: actGet diubah dari GetListRiwayatPendidikanMahasiswa
    //            menjadi GetNilaiTransferPendidikanMahasiswa (sesuai API v2.0 §3.126)
    private Panel buildPanelNilaiTransfer() {
        return buildFeederPanel(
            "3.126 Nilai Transfer Pendidikan (GetNilaiTransferPendidikanMahasiswa)",
            "GetNilaiTransferPendidikanMahasiswa",
            "GetCountNilaiTransferPendidikanMahasiswa", "",
            new String[]{"nim", "nama_mahasiswa", "kode_mata_kuliah_asal",
                "nama_mata_kuliah_asal", "nilai_huruf_asal",
                "kode_matkul_diakui", "nama_mata_kuliah_diakui", "nilai_huruf_diakui"},
            new String[]{"NIM", "Nama Mahasiswa", "Kode MK Asal",
                "MK Asal", "Nilai Asal", "Kode MK Diakui", "MK Diakui", "Nilai Diakui"},
            new PanelAction() {
                public void run() throws Exception {
                    openWindow(new DownloadNilaiTransfer(),
                        "Ambil Nilai Transfer dari Neo Feeder", "85%", "80%");
                }
            },
            new PanelAction() {
                public void run() throws Exception {
                    openWindow(new UploadNilaiTransfer(),
                        "Kirim Nilai Transfer ke Neo Feeder", "85%", "80%");
                }
            }
        );
    }

    private Panel buildPanelRiwayatPendidikanMahasiswa() {
        return buildFeederPanel(
            "3.119 Riwayat Pendidikan Mahasiswa (GetListRiwayatPendidikanMahasiswa)",
            "GetListRiwayatPendidikanMahasiswa",
            "GetCountRiwayatPendidikanMahasiswa", "",
            new String[]{"nim", "nama_mahasiswa", "nama_program_studi",
                "angkatan", "nama_status_mahasiswa"},
            new String[]{"NIM", "Nama Mahasiswa", "Program Studi",
                "Angkatan", "Status"},
            new PanelAction() {
                public void run() throws Exception {
                    openWindow(new DownloadHistory(),
                        "Ambil Riwayat Pendidikan Mahasiswa dari Neo Feeder", "85%", "80%");
                }
            },
            null
        );
    }

    private Panel buildPanelUjiMahasiswa() {
        return buildFeederPanel(
            "3.145 Uji Mahasiswa (GetListUjiMahasiswa)",
            "GetListUjiMahasiswa", null, "",
            new String[]{"judul", "nidn", "nama_dosen",
                "nama_kategori_kegiatan", "penguji_ke"},
            new String[]{"Judul Aktivitas", "NIDN Penguji",
                "Nama Penguji", "Kategori", "Penguji ke-"},
            null, null
        );
    }

    private Panel buildPanelPerubahanRiwayatPendidikan() {
        return buildFeederPanel(
            "3.143 Perubahan Riwayat Pendidikan (GetListPerubahanRiwayatPendidikan)",
            "GetListPerubahanRiwayatPendidikan", null, "",
            new String[]{"nim_lama", "nim_baru", "id_semester",
                "tanggal_masuk", "tanggal_pindah"},
            new String[]{"NIM Lama", "NIM Baru", "Semester",
                "Tgl Masuk", "Tgl Pindah"},
            new PanelAction() {
                public void run() throws Exception {
                    openWindow(new DownloadHistory(),
                        "Ambil Perubahan Riwayat Pendidikan dari Neo Feeder", "85%", "80%");
                }
            },
            null
        );
    }

    private Panel buildPanelRiwayatNilaiMahasiswa() {
        return buildFeederPanel(
            "3.76 / 3.131 Riwayat Nilai Mahasiswa (GetRiwayatNilaiMahasiswa)",
            "GetRiwayatNilaiMahasiswa", "GetCountRiwayatNilaiMahasiswa", "",
            new String[]{"nim", "nama_mahasiswa", "nama_mata_kuliah",
                "nama_kelas_kuliah", "nilai_huruf", "nilai_indeks"},
            new String[]{"NIM", "Nama Mahasiswa", "Mata Kuliah",
                "Kelas", "Nilai Huruf", "Nilai Indeks"},
            null, null
        );
    }

    private Panel buildPanelKrsMahasiswa() {
        return buildFeederPanel(
            "3.130 KRS Mahasiswa (GetKRSMahasiswa)",
            "GetKRSMahasiswa", null, "",
            new String[]{"nim", "nama_mahasiswa", "kode_mata_kuliah",
                "nama_mata_kuliah", "nama_kelas_kuliah", "sks_mata_kuliah"},
            new String[]{"NIM", "Nama Mahasiswa", "Kode MK",
                "Mata Kuliah", "Kelas", "SKS"},
            new PanelAction() {
                public void run() throws Exception {
                    openWindow(new DownloadKrs(),
                        "Ambil KRS Mahasiswa dari Neo Feeder", "85%", "80%");
                }
            },
            new PanelAction() {
                public void run() throws Exception {
                    openWindow(new UploadKrs(),
                        "Kirim KRS Mahasiswa ke Neo Feeder", "85%", "80%");
                }
            }
        );
    }

    // =========================================================
    // SEKSI 3 — AKADEMIK
    // =========================================================

    private Panel buildPanelMatakuliah() {
        return buildFeederPanel(
            "3.196 Mata Kuliah (GetListMataKuliah)",
            "GetListMataKuliah", "GetCountMataKuliah", "",
            new String[]{"kode_mata_kuliah", "nama_mata_kuliah",
                "sks_mata_kuliah", "id_jenis_mata_kuliah", "id_kelompok_mata_kuliah"},
            new String[]{"Kode MK", "Nama MK",
                "SKS", "Jenis", "Kelompok"},
            new PanelAction() {
                public void run() throws Exception {
                    openWindow(new DownloadMatakuliah(),
                        "Ambil Data Matakuliah dari Neo Feeder", "80%", "75%");
                }
            },
            new PanelAction() {
                public void run() throws Exception {
                    openWindow(new KirimKeFeederWindow("matakuliah"),
                        "Kirim Mata Kuliah ke Neo Feeder", "70%", "60%");
                }
            }
        );
    }

    private Panel buildPanelKurikulum() {
        return buildFeederPanel(
            "3.209 Kurikulum (GetListKurikulum)",
            "GetListKurikulum", "GetCountKurikulum", "",
            new String[]{"nama_kurikulum", "id_semester",
                "nama_program_studi", "jumlah_sks_lulus", "jumlah_sks_wajib"},
            new String[]{"Nama Kurikulum", "Semester Berlaku",
                "Program Studi", "SKS Lulus", "SKS Wajib"},
            null,
            new PanelAction() {
                public void run() throws Exception {
                    openWindow(new KirimKeFeederWindow("kurikulum"),
                        "Kirim Kurikulum ke Neo Feeder", "70%", "60%");
                }
            }
        );
    }

    private Panel buildPanelKelasKuliah() {
        return buildFeederPanel(
            "3.85 Kelas Kuliah (GetListKelasKuliah)",
            "GetListKelasKuliah", "GetCountKelasKuliah", "",
            new String[]{"nama_kelas_kuliah", "nama_mata_kuliah",
                "nama_program_studi", "id_semester", "sks_mata_kuliah"},
            new String[]{"Kelas", "Mata Kuliah",
                "Prodi", "Semester", "SKS"},
            new PanelAction() {
                public void run() throws Exception {
                    openWindow(new DownloadKelas(),
                        "Ambil Data Kelas Kuliah dari Neo Feeder", "85%", "80%");
                }
            },
            new PanelAction() {
                public void run() throws Exception {
                    openWindow(new UploadKelas(),
                        "Kirim Data Kelas Kuliah ke Neo Feeder", "85%", "80%");
                }
            }
        );
    }

    private Panel buildPanelPerkuliahanMahasiswa() {
        return buildFeederPanel(
            "3.88 Perkuliahan Mahasiswa / KRS (GetListPerkuliahanMahasiswa)",
            "GetListPerkuliahanMahasiswa", "GetCountPerkuliahanMahasiswa", "",
            new String[]{"nim", "nama_mahasiswa", "nama_program_studi",
                "nama_semester", "id_status_mahasiswa", "ipk"},
            new String[]{"NIM", "Nama Mahasiswa", "Prodi",
                "Semester", "Status", "IPK"},
            new PanelAction() {
                public void run() throws Exception {
                    openWindow(new DownloadKrs(),
                        "Ambil Data Perkuliahan Mahasiswa dari Neo Feeder", "85%", "80%");
                }
            },
            new PanelAction() {
                public void run() throws Exception {
                    openWindow(new UploadKrs(),
                        "Kirim Data Perkuliahan Mahasiswa ke Neo Feeder", "85%", "80%");
                }
            },
            new AksiTambahan[]{
                new AksiTambahan("Ambil AKM (per Kelas)", "Ambil Aktivitas Kuliah Mahasiswa (AKM) per kelas dari Neo Feeder", new PanelAction() {
                    public void run() throws Exception {
                        openWindow(new DownloadAkm(),
                            "Ambil AKM (Aktivitas Kuliah Mahasiswa) dari Neo Feeder", "85%", "80%");
                    }
                })
            }
        );
    }

    private Panel buildPanelNilaiPerkuliahan() {
        return buildFeederPanel(
            "3.75 Nilai Perkuliahan Kelas (GetListNilaiPerkuliahanKelas)",
            "GetListNilaiPerkuliahanKelas", "GetCountNilaiPerkuliahanKelas", "",
            new String[]{"nama_mata_kuliah", "nama_kelas_kuliah",
                "id_smt", "jumlah_mahasiswa_krs", "jumlah_mahasiswa_dapat_nilai"},
            new String[]{"Mata Kuliah", "Kelas",
                "Semester", "Jml Mahasiswa KRS", "Jml Dapat Nilai"},
            new PanelAction() {
                public void run() throws Exception {
                    openWindow(new DownloadNilai(),
                        "Ambil Data Nilai dari Neo Feeder", "85%", "80%");
                }
            },
            new PanelAction() {
                public void run() throws Exception {
                    openWindow(new UploadNilai(),
                        "Kirim Data Nilai ke Neo Feeder", "85%", "80%");
                }
            }
        );
    }

    private Panel buildPanelSkalaNilaiProdi() {
        return buildFeederPanel(
            "3.74 Skala Nilai Prodi (GetListSkalaNilaiProdi)",
            "GetListSkalaNilaiProdi", "GetCountSkalaNilaiProdi", "",
            new String[]{"nama_program_studi", "nilai_huruf", "nilai_indeks",
                "bobot_minimum", "bobot_maksimum", "tanggal_mulai_efektif"},
            new String[]{"Program Studi", "Nilai Huruf", "Indeks",
                "Bobot Min", "Bobot Max", "Mulai Berlaku"},
            null, null
        );
    }

    private Panel buildPanelPesertaKelasKuliah() {
        return buildFeederPanel(
            "3.79 Peserta Kelas Kuliah (GetPesertaKelasKuliah)",
            "GetPesertaKelasKuliah", "GetCountPesertaKelasKuliah", "",
            new String[]{"nama_kelas_kuliah", "nim", "nama_mahasiswa",
                "nama_mata_kuliah", "nama_program_studi", "angkatan"},
            new String[]{"Kelas", "NIM", "Nama Mahasiswa",
                "Mata Kuliah", "Prodi", "Angkatan"},
            null, null
        );
    }

    private Panel buildPanelRencanaPembelajaran() {
        return buildFeederPanel(
            "3.100 Rencana Pembelajaran (GetListRencanaPembelajaran)",
            "GetListRencanaPembelajaran", "GetCountRencanaPembelajaran", "",
            new String[]{"nama_mata_kuliah", "kode_mata_kuliah",
                "nama_program_studi", "pertemuan", "materi_indonesia"},
            new String[]{"Mata Kuliah", "Kode",
                "Prodi", "Pertemuan ke-", "Materi (ID)"},
            null, null
        );
    }

    private Panel buildPanelMatkulKurikulum() {
        return buildFeederPanel(
            "3.199 Matkul Kurikulum (GetMatkulKurikulum)",
            "GetMatkulKurikulum", "GetCountMatkulKurikulum", "",
            new String[]{"nama_kurikulum", "kode_mata_kuliah", "nama_mata_kuliah",
                "nama_program_studi", "semester", "sks_mata_kuliah", "apakah_wajib"},
            new String[]{"Kurikulum", "Kode MK", "Mata Kuliah",
                "Prodi", "Semester", "SKS", "Wajib"},
            null, null
        );
    }

    private Panel buildPanelSubstansiKuliah() {
        return buildFeederPanel(
            "3.203 Substansi Kuliah (GetListSubstansiKuliah)",
            "GetListSubstansiKuliah", "GetCountSubstansiKuliah", "",
            new String[]{"nama_substansi", "nama_program_studi",
                "sks_mata_kuliah", "sks_tatap_muka", "nama_jenis_substansi"},
            new String[]{"Nama Substansi", "Program Studi",
                "SKS", "SKS Tatap Muka", "Jenis Substansi"},
            null, null
        );
    }

    // =========================================================
    // SEKSI 4 — PERIODE
    // =========================================================

    private Panel buildPanelPeriodePerkuliahan() {
        return buildFeederPanel(
            "3.108 Periode Perkuliahan (GetListPeriodePerkuliahan)",
            "GetListPeriodePerkuliahan", "GetCountPeriodePerkuliahan", "",
            new String[]{"nama_program_studi", "nama_semester",
                "jumlah_target_mahasiswa_baru",
                "tanggal_awal_perkuliahan", "tanggal_akhir_perkuliahan", "jml_mgu_kul"},
            new String[]{"Prodi", "Semester",
                "Target Mhs Baru", "Tgl Mulai", "Tgl Selesai", "Jml Minggu"},
            null, null
        );
    }

    private Panel buildPanelJalurPendaftaran() {
        return buildFeederPanel(
            "3.102 Jalur Pendaftaran Perkuliahan (GetListPeriodeJalurPendaftaranPerkuliahan)",
            "GetListPeriodeJalurPendaftaranPerkuliahan", null, "",
            new String[]{"nama_program_studi", "nm_jalur_daftar", "nama_semester",
                "jumlah_target_mahasiswa_baru", "calon_ikut_seleksi", "calon_lulus_seleksi"},
            new String[]{"Prodi", "Jalur Daftar", "Semester",
                "Target", "Ikut Seleksi", "Lulus Seleksi"},
            null, null
        );
    }

    // =========================================================
    // SEKSI 5 — REKAP & LAPORAN
    // =========================================================

    private Panel buildPanelProfilPT() {
        return buildFeederPanel(
            "3.3 Profil Perguruan Tinggi (GetProfilPT)",
            "GetProfilPT", "GetCountPerguruanTinggi", "",
            new String[]{"kode_perguruan_tinggi", "nama_perguruan_tinggi",
                "telepon", "email", "Website"},
            new String[]{"Kode PT", "Nama PT", "Telepon", "Email", "Website"},
            null, null
        );
    }

    private Panel buildPanelRekapJumlahDosen() {
        return buildFeederPanel(
            "3.188 Rekap Jumlah Dosen (GetRekapJumlahDosen)",
            "GetRekapJumlahDosen", null, "",
            new String[]{"nama_program_studi", "nama_periode",
                "jumlah_dosen_homebase", "is_homebase"},
            new String[]{"Program Studi", "Periode",
                "Jumlah Dosen", "Homebase"},
            null, null
        );
    }

    private Panel buildPanelRekapJumlahMahasiswa() {
        return buildFeederPanel(
            "3.189 Rekap Jumlah Mahasiswa (GetRekapJumlahMahasiswa)",
            "GetRekapJumlahMahasiswa", null, "",
            new String[]{"nama_program_studi", "nama_periode",
                "aktif", "cuti", "non_aktif", "sedang_double_degree"},
            new String[]{"Program Studi", "Periode",
                "Aktif", "Cuti", "Non Aktif", "Double Degree"},
            null, null
        );
    }

    private Panel buildPanelRekapIpsMahasiswa() {
        return buildFeederPanel(
            "3.191 Rekap IPS Mahasiswa (GetRekapIPSMahasiswa)",
            "GetRekapIPSMahasiswa", null, "",
            new String[]{"nama_program_studi", "nama_periode",
                "ips_range_0_1", "ips_range_1_2",
                "ips_range_2_3", "ips_range_3_4", "ips_range_diatas_4"},
            new String[]{"Prodi", "Periode",
                "IPS 0–1", "1–2", "2–3", "3–4", ">4"},
            null, null
        );
    }

    private Panel buildPanelRekapLaporan() {
        return buildFeederPanel(
            "3.190 Rekap Laporan Akademik (GetRekapLaporan)",
            "GetRekapLaporan", null, "",
            new String[]{"nama_program_studi", "nama_periode",
                "mahasiswa_baru", "kelas_perkuliahan", "krs_mahasiswa", "nilai_mahasiswa"},
            new String[]{"Program Studi", "Periode",
                "Mhs Baru", "Kelas", "KRS", "Nilai"},
            null, null
        );
    }

    private Panel buildPanelRekapKhsMahasiswa() {
        return buildFeederPanel(
            "3.186 Rekap KHS Mahasiswa (GetRekapKHSMahasiswa)",
            "GetRekapKHSMahasiswa", null, "",
            new String[]{"nim", "nama_mahasiswa", "nama_program_studi",
                "nama_periode", "nama_mata_kuliah", "nilai_huruf", "nilai_angka"},
            new String[]{"NIM", "Nama", "Prodi",
                "Periode", "Mata Kuliah", "Nilai", "Nilai Angka"},
            null, null
        );
    }

    private Panel buildPanelRekapKrsMahasiswa() {
        return buildFeederPanel(
            "3.187 Rekap KRS Mahasiswa (GetRekapKRSMahasiswa)",
            "GetRekapKRSMahasiswa", null, "",
            new String[]{"nim", "nama_mahasiswa", "nama_program_studi",
                "nama_periode", "kode_mata_kuliah", "nama_mata_kuliah", "sks_mata_kuliah"},
            new String[]{"NIM", "Nama", "Prodi",
                "Periode", "Kode MK", "Mata Kuliah", "SKS"},
            null, null
        );
    }

    // =========================================================
    // SEKSI 6 — KAMPUS MERDEKA
    // =========================================================

    private Panel buildPanelRencanaEvaluasi() {
        return buildFeederPanel(
            "3.221 Rencana Evaluasi (GetListRencanaEvaluasi)",
            "GetListRencanaEvaluasi", "GetCountRencanaEvaluasi", "",
            new String[]{"nama_mata_kuliah", "kode_mata_kuliah",
                "nama_program_studi", "nama_evaluasi",
                "deskripsi_indonesia", "bobot_evaluasi"},
            new String[]{"Mata Kuliah", "Kode",
                "Prodi", "Nama Evaluasi", "Deskripsi", "Bobot (%)"},
            null, null
        );
    }

    private Panel buildPanelKonversiKampusMerdeka() {
        return buildFeederPanel(
            "3.226 Konversi Kampus Merdeka (GetListKonversiKampusMerdeka)",
            "GetListKonversiKampusMerdeka", "GetCountKonversiKampusMerdeka", "",
            new String[]{"nim", "nama_mahasiswa", "nama_mata_kuliah",
                "judul", "nilai_angka", "nilai_huruf", "sks_mata_kuliah"},
            new String[]{"NIM", "Nama Mahasiswa", "Mata Kuliah Konversi",
                "Judul Aktivitas", "Nilai Angka", "Nilai Huruf", "SKS"},
            null, null
        );
    }

    // =========================================================
    // UTILITIES
    // =========================================================

    private void openWindow(MyWindow win, String title, String width, String height) {
        try {
            win.setTitle(title);
            win.setWidth(width);
            win.setHeight(height);
            win.setClosable(true);
            win.setParent(
                ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot()
            );
            win.onModal();
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
        }
    }

    private static String[] safeKoneksi() {
        try {
            return EksporFromFeederAction.koneksi();
        } catch (Exception e) {
            return new String[]{"", "8082", "", "", ""};
        }
    }
}
