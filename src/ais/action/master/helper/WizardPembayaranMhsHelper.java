package ais.action.master.helper;

import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Datebox;
import org.zkoss.zul.Decimalbox;
import org.zkoss.zul.Div;
import org.zkoss.zul.Html;

import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.action.master.helper.virtualaccount.DownloadTagihanMahasiswaBankBankaltimtara;
import ais.action.master.helper.virtualaccount.DownloadTagihanMahasiswaBankBjb;
import ais.action.master.helper.virtualaccount.DownloadTagihanMahasiswaBankBtn;
import ais.action.master.helper.virtualaccount.DownloadTagihanMahasiswaBankNtt;
import ais.action.master.helper.virtualaccount.DownloadTagihanMahasiswaBankOnline;
import ais.action.ws.util.PembayaranUtil;
import ais.common.BarcodeCommon;
import ais.common.BniCommon;
import ais.common.BniKeranjangPembayaran;
import ais.common.BsiKeranjangPembayaran;
import ais.common.FaspayKeranjangPembayaran;
import ais.common.JatelindoKeranjangPembayaran;
import ais.common.CimbCommon;
import ais.common.ConstantValues;
import ais.common.BriCommon;
import ais.common.BsiCommon;
import ais.common.Common;
import ais.common.DokuCommon;
import ais.common.FaspayCommon;
import ais.common.FinpayCommon;
import ais.common.IpaymuCommon;
import ais.common.JatelindoCommon;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BankHost;
import ais.database.model.VirtualAccountBank;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.CicilanPembayaran;
import ais.database.model.DetailBiaya;
import ais.database.model.JadwalPembayaran;
import ais.database.model.ItemBiaya;
import ais.database.model.JenisKegiatan;
import ais.database.model.Kegiatan;
import ais.database.model.KegiatanTemporary;
import ais.database.model.Konfigurasi;
import ais.database.model.PengaturanPembayaranBulanan;
import ais.database.model.Mahasiswa;
import ais.database.model.Tbmuser;
import ais.database.model.bni.BniRequestDetail;
import ais.database.model.bni.BniRequestDetailBiaya;
import ais.database.model.bri.BriRequestDetail;
import ais.database.model.bri.BriRequestDetailBiaya;
import ais.database.model.bsi.BsiRequestDetail;
import ais.database.model.bsi.BsiRequestDetailBiaya;
import ais.database.model.faspay.FaspayRequestDetail;
import ais.database.model.faspay.FaspayRequestDetailBiaya;
import ais.database.model.finpay.FinpayRequestDetail;
import ais.database.model.finpay.FinpayRequestDetailBiaya;
import ais.database.model.cimb.CimbRequestDetail;
import ais.database.model.cimb.CimbRequestDetailBiaya;
import ais.database.model.doku.DokuRequestDetail;
import ais.database.model.doku.DokuRequestDetailBiaya;
import ais.database.model.ipaymu.IpaymuRequestDetail;
import ais.database.model.ipaymu.IpaymuRequestDetailBiaya;
import ais.database.model.jatelindo.JatelindoRequestDetail;
import ais.database.model.jatelindo.JatelindoRequestDetailBiaya;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.WaktuUtil;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Grid;

/**
 * <h2>Wizard Pembayaran Mahasiswa — antarmuka pembayaran lima langkah yang mandiri.</h2>
 *
 * <p>
 * Class ini merupakan rekonstruksi penuh dari alur pembayaran yang sebelumnya tertanam di
 * dalam {@code DaftarUlangMahasiswaBaruAction} dan {@code DaftarUlangMahasiswaLamaAction}.
 * Berbeda dengan pendahulunya yang berbentuk satu layar padat berisi grid tagihan dan
 * deretan tombol, wizard ini memandu pengguna melalui <b>lima langkah berurutan</b> yang
 * masing-masing hanya menuntut satu keputusan, sesuai praktik terbaik pola
 * <i>checkout wizard</i> pada aplikasi pembayaran modern (progresif, satu fokus per
 * layar, ringkasan sebelum eksekusi, konfirmasi setelah eksekusi):
 * </p>
 *
 * <ol>
 *   <li><b>Jenis &amp; Semester</b> — memilih {@link JenisKegiatan} (jenis pembayaran)
 *       melalui combo standar {@code Common.initJenisPembayaranMahasiswa} dan semester
 *       tujuan pembayaran; semester berjalan mahasiswa ditandai otomatis.</li>
 *   <li><b>Pilih Tagihan</b> — seluruh item tagihan dimuat melalui
 *       {@code PembayaranUtilHelper.getDetailBiayaMahasiswa} (jalur data yang sama dengan
 *       DaftarUlang), lalu setiap item ditampilkan sebagai kartu berisi nominal tagihan,
 *       akumulasi yang sudah dibayar (dihitung ulang langsung dari
 *       {@link CicilanPembayaran} via kriteria Hibernate), dan sisa kekurangan; item yang
 *       belum lunas terseleksi otomatis.</li>
 *   <li><b>Atur Nominal</b> — pengguna boleh membayar <i>sebagian</i> (angsuran) dengan
 *       mengubah nominal per item; nilai divalidasi agar tidak melebihi kekurangan dan
 *       total berjalan diperbarui seketika.</li>
 *   <li><b>Cara Bayar</b> — daftar saluran pembayaran dirakit <b>bukan</b> dari kode yang
 *       ditulis tangan, melainkan dari {@link PembayaranGatewayKatalog} — single source of
 *       truth yang juga dikonsumsi checkout JSP {@code /WEB-INF/baru/modul/bayarmhs/}.
 *       Dengan demikian konfigurasi on/off ({@code aktifkan_pembayaran_via_*}, varian
 *       per-PT {@code _pt_<id>}, filter {@code JenisKegiatan.namaBankPembayaran}, serta
 *       gate tunai {@code aktifkan_pembayaran_manual} + larangan
 *       {@code admin_lain_yang_tidak_bisa_membayar_langsung}) dijamin <b>identik</b>
 *       dengan DaftarUlangMahasiswa*Action dan versi JSP.</li>
 *   <li><b>Selesai</b> — konfirmasi keberhasilan; bila saluran menghasilkan Virtual
 *       Account, nomor VA, total beserta biaya admin, dan batas waktu pembayaran
 *       ditampilkan dalam kartu yang mudah disalin.</li>
 * </ol>
 *
 * <h3>Strategi eksekusi pembayaran (tanpa ketergantungan pada DaftarUlang)</h3>
 * <ul>
 *   <li><b>Tunai/manual</b> — {@link Kegiatan} + {@link CicilanPembayaran} dibangun manual
 *       dalam satu transaksi pada session Hibernate tersendiri
 *       ({@code openSession + finally closeSessionQuietly}), dengan validator diisi nama
 *       pengguna aktif dan status mahasiswa {@code ConstantValues.AKTIF}.</li>
 *   <li><b>Gerbang langsung</b> (Doku, IPaymu, Faspay, Jatelindo, CIMB, BNI, BSI, BRI,
 *       Finpay) — daftar {@code XxxRequestDetailBiaya} dibangun langsung dari objek
 *       {@link DetailBiaya} + nominal pilihan pengguna, dikonversi via
 *       {@code XxxCommon.populateXxxRequestDetailDariDetailBiaya}, lalu dieksekusi
 *       {@code XxxCommon.onSaveXxx} — <i>tanpa</i> membutuhkan Grid ZK DaftarUlang.</li>
 *   <li><b>Keluarga "Bank Online"</b> (Online, Online 2, Smartlink, Maja, QRIS,
 *       Finpay-Bank, Flip, Otto, BRIVA) — memakai
 *       {@code DownloadTagihanMahasiswaBankOnline.downloadData} persis seperti
 *       {@code DaftarUlangMahasiswaLamaAction.setupBankOnlineGateway}; grid cicilan yang
 *       dituntut method tersebut disintesis in-memory oleh
 *       {@link PembayaranGatewayKatalog#buatGridCicilanMock} (pola "deep mocking grid"
 *       yang sama dengan {@code _lanjut_bayar_services.jsp}), sehingga VA yang terbit
 *       identik antara wizard ZK dan checkout JSP untuk item dan nominal yang sama.
 *       Hasil bertautan (Flip/Finpay/Otto) dibuka di tab/popup browser; hasil bernomor VA
 *       dirender pada langkah Selesai.</li>
 * </ul>
 *
 * <h3>Desain UI/UX dan responsivitas</h3>
 * <p>
 * Seluruh antarmuka dirakit dari {@code Div}/{@code Html} ZK dengan gaya inline
 * (palet Tailwind slate/blue, kartu ber-radius 10-12px, stepper progres bernomor dengan
 * tanda centang untuk langkah usai). Pada perangkat mobile ({@code Common.isMobile()})
 * jendela dipaksa satu layar penuh 100vw × 100dvh dengan area konten tunggal yang dapat
 * digulir — menghindari jebakan "scroll dalam scroll" ZK. Pada desktop jendela terpusat
 * 660px dengan header/stepper/footer tetap dan hanya badan yang menggulir. Tombol saluran
 * pembayaran ditata sebagai kartu grid fleksibel dengan target sentuh ≥44px sesuai
 * pedoman aksesibilitas mobile.
 * </p>
 *
 * <p>
 * <b>Kompatibilitas:</b> Java 1.7 / ZK 5 CE — semua listener berupa anonymous inner class,
 * tanpa lambda/stream. <b>Entry point:</b> {@link #buka(Mahasiswa, EventListener)}.
 * </p>
 *
 * @see PembayaranGatewayKatalog katalog saluran pembayaran bersama (ZK + JSP)
 */
public class WizardPembayaranMhsHelper {

    // ============================================================ INNER DTO
    private static final class TagihanItem {
        final DetailBiaya detailBiaya;
        /**
         * Baris billing BULANAN/angsuran pemilik item ini (null untuk item reguler).
         * Terisi bila tagihan mahasiswa bermode angsuran — pembayaran harus teralokasi
         * ke slot bulanan ini (token {@code Bulanan-<id>-<nilai>} pada VA).
         */
        final PengaturanPembayaranBulanan bulanan;
        /** Jenis pembayaran pemilik item ini — penting pada mode multi-jenis (Keranjang). */
        final JenisKegiatan jenis;
        /**
         * Boleh diangsur/nominal boleh diubah? Dari {@code PembayaranUtil.bolehDiangsur}:
         * item ber-"Tagihan Default = Ya" SELALU false (wajib dibayar penuh); lainnya
         * mengikuti flag mencicil pada ItemBiaya. Baris bulanan selalu boleh diubah.
         */
        final boolean bisaDiubah;
        final double nominal;
        /**
         * Rincian proses penghitungan nominal (mis. "Biaya SP Matakuliah Per SKS (Rp.
         * 10.000) x 15 SKS, sbb : ...") — hanya terisi untuk item ber-penghitungan
         * PERKALIAN (ItemBiaya.getPenghitungan() != TIDAK_ADA_PENGHITUNGAN), diambil dari
         * {@code DetailBiaya.getKeterangan()} SETELAH {@code Kegiatan.ambilJumlahTagihan}
         * dipanggil (method itu men-trigger updateKeterangan() sbg efek-samping) — pola yang
         * sama dipakai {@code DetailPembayaranMahasiswaRenderer} (baris ~816-824). Kosong
         * untuk item tanpa penghitungan (nilai tetap), tidak perlu dijelaskan.
         */
        final String keterangan;
        double sudahDibayar;
        double kekurangan;
        boolean dipilih;
        double nominalBayar;
        Date tanggalBayar;
        Div cardDiv;

        TagihanItem(DetailBiaya db, PengaturanPembayaranBulanan bulanan, JenisKegiatan jenis,
                double nominal, double sudahDibayar, boolean bisaDiubah, String keterangan) {
            this.detailBiaya = db;
            this.bulanan = bulanan;
            this.jenis = jenis;
            this.bisaDiubah = bisaDiubah;
            this.nominal = nominal;
            this.keterangan = keterangan;
            this.sudahDibayar = sudahDibayar;
            this.kekurangan = Math.max(0, nominal - sudahDibayar);
            this.nominalBayar = this.kekurangan;
            this.tanggalBayar = new Date();
        }
    }

    // ============================================================ CONSTANTS
    private static final String[] JUDUL = {
        "Jenis & Semester", "Pilih Tagihan", "Atur Nominal", "Cara Bayar", "Selesai"
    };
    private static final String CARD_STYLE =
        "background:#fff;border:1px solid #e2e8f0;border-radius:10px;padding:14px;margin-bottom:10px;box-sizing:border-box;";
    private static final String LABEL_SM =
        "font-size:11px;font-weight:700;color:#64748b;text-transform:uppercase;letter-spacing:.4px;margin-bottom:4px;display:block;";
    private static final String BTN_PRIMARY =
        "background:linear-gradient(135deg,#2563eb,#1d4ed8);color:#fff;border:0;border-radius:8px;"
        + "padding:10px 18px;font-size:13px;font-weight:700;cursor:pointer;";
    private static final String BTN_SECONDARY =
        "background:#f1f5f9;color:#1e3a8a;border:1px solid #cbd5e1;border-radius:8px;"
        + "padding:10px 18px;font-size:13px;font-weight:600;cursor:pointer;";
    private static final String CSS_ATTR = "wz_mhs_css_v1";

    // ============================================================ FIELDS
    private final Mahasiswa mahasiswa;
    private final EventListener onSelesai;

    private JenisKegiatan jenisKegiatan;
    private Integer semester;
    private List<TagihanItem> tagihanItems = new ArrayList<TagihanItem>();
    private int langkah = 1;

    /**
     * Jenis pembayaran TAMBAHAN yang ikut dibayar sekali jalan (mode Keranjang Belanja).
     * Kosong = mode satu-jenis biasa. Terisi = langkah Cara Bayar beralih ke saluran
     * keranjang: draf {@link KegiatanTemporary} per jenis + VA langsung.
     */
    private List<JenisKegiatan> jenisEkstra = new ArrayList<JenisKegiatan>();

    /** Jadwal pembayaran aktif per jenis kegiatan (id → jadwal) untuk mode multi-jenis. */
    private Map<Long, JadwalPembayaran> jadwalPerJenis = new HashMap<Long, JadwalPembayaran>();

    // Step 1 UI refs (held for validasi)
    private Combobox cboJenis;
    private Combobox cboSmt;
    private Div rowSmt;
    /** Pasangan [Checkbox, JenisKegiatan] pilihan jenis tambahan di langkah 1. */
    private final List<Object[]> chkJenisEkstra = new ArrayList<Object[]>();

    // Window + layout hosts
    private MyWindow window;
    private Div root;
    private Div stepperHost;
    private Div bodyHost;
    private Div footerHost;

    // Hasil Virtual Account terakhir (diisi oleh saluran keluarga Bank Online;
    // dirender sebagai kartu VA pada langkah Selesai)
    private String vaLabelBank;
    private String vaKode;
    private String vaTotal;
    private String vaKadaluarsa;
    private String vaQrUrl;

    /**
     * Jadwal pembayaran aktif untuk kombinasi (jenisKegiatan, semester) — di-resolve pada
     * {@link #muatTagihan()} dengan resep yang sama dengan {@code _lanjut_bayar_services.jsp}
     * dan diteruskan ke seluruh gateway agar Virtual Account/Kegiatan yang terbit terkait
     * jenis kegiatan yang benar (paritas DaftarUlang).
     */
    private JadwalPembayaran jadwalPembayaran;

    /** Guard anti klik-ganda: true selama sebuah eksekusi pembayaran berlangsung. */
    private boolean sedangProses = false;

    /**
     * Filter Step 2 "Pilih Tagihan" (permintaan user): {@code true} (default) =
     * hanya menampilkan item yang ADA tagihannya — item ber-nominal Rp 0 (mis. BII/SKS
     * yang hasil perkaliannya nol) disembunyikan agar daftar ringkas; dapat dimatikan
     * lewat checkbox di atas daftar untuk melihat seluruh item.
     */
    private boolean hanyaAdaTagihan = true;

    // ============================================================ ENTRY POINT
    public WizardPembayaranMhsHelper(Mahasiswa mhs, EventListener onSelesai) {
        this.mahasiswa = mhs;
        this.onSelesai = onSelesai;
    }

    /** Buka wizard pembayaran dalam window popup. */
    /**
     * Konfigurasi ON/OFF Wizard Pembayaran Mahasiswa (menu Konfigurasi &gt; Pembayaran
     * Mahasiswa). Default AKTIF — fitur murni aditif (menambah tombol/jendela, tidak
     * mengubah alur pembayaran lama), sehingga aman menyala dari awal; admin dapat
     * mematikannya bila perlu menyembunyikan tombol Wizard di semua titik pemanggilan
     * (DaftarUlangMahasiswaLama/BaruAction, ProfileMahasiswa, dashboard
     * InformasiPembayaranMahasiswaAction) tanpa perlu redeploy.
     */
    public static final String KONFIGURASI_AKTIF = "aktifkan_wizard_pembayaran_mahasiswa";

    /** @return true bila Wizard Pembayaran Mahasiswa diaktifkan (default AKTIF). */
    public static boolean aktif() {
        try {
            return Konfigurasi.AKTIF.equals(Common.getKonfigurasi(KONFIGURASI_AKTIF, Konfigurasi.AKTIF).getNilai());
        } catch (Exception e) {
            ais.common.ErrorAuditUtil.record(e, "WizardPembayaranMhsHelper.aktif(): gagal baca konfigurasi, fallback AKTIF");
            return true;
        }
    }

    public static void buka(Mahasiswa mhs, EventListener onSelesai) throws Exception {
        if (!aktif()) {
            MyMessageboxConfig.show(
                    "Wizard Pembayaran sedang dinonaktifkan oleh Administrator. Silakan gunakan menu "
                            + "pembayaran biasa, atau hubungi Administrator untuk mengaktifkannya kembali.",
                    "Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return;
        }
        new WizardPembayaranMhsHelper(mhs, onSelesai).tampilkan();
    }

    public void tampilkan() throws Exception {
        if (mahasiswa == null || mahasiswa.getId() == null) {
            alertar("Data mahasiswa belum tersedia.");
            return;
        }

        boolean mobile = Common.isMobile();

        window = new MyWindow("", "none", false);
        window.setSclass("ais-standard-window ais-mywindow wz-mhs-bayar"
                + (mobile ? " wz-mhs-mobile" : ""));
        window.setContentStyle("background:#f1f5f9;overflow:hidden;box-sizing:border-box;");

        root = new Div();
        root.setParent(window);
        root.setWidth("100%");
        root.setStyle("display:flex;flex-direction:column;height:100%;box-sizing:border-box;");

        // Header
        Div header = buildHeader();
        header.setParent(root);

        // Stepper
        stepperHost = new Div();
        stepperHost.setParent(root);
        stepperHost.setWidth("100%");
        stepperHost.setStyle("flex:0 0 auto;background:#fff;border-bottom:1px solid #e2e8f0;padding:10px 14px;");

        // Body (scrollable)
        bodyHost = new Div();
        bodyHost.setParent(root);
        bodyHost.setWidth("100%");
        bodyHost.setStyle("flex:1 1 auto;min-height:0;overflow-y:auto;overflow-x:hidden;"
                + "padding:14px;box-sizing:border-box;-webkit-overflow-scrolling:touch;");

        // Footer
        footerHost = new Div();
        footerHost.setParent(root);
        footerHost.setWidth("100%");
        footerHost.setStyle("flex:0 0 auto;background:#fff;border-top:1px solid #e2e8f0;"
                + "padding:10px 14px;display:flex;gap:10px;flex-wrap:wrap;align-items:center;"
                + "justify-content:space-between;");

        render();

        ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(window);
        injectCssMobile(mobile);
        window.doHighlighted();
        try { window.setPosition("center"); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/WizardPembayaranMhsHelper.java:331"); /* ignore */ }
    }

    // ============================================================ HEADER
    private Div buildHeader() {
        Div header = new Div();
        header.setWidth("100%");
        header.setStyle("flex:0 0 auto;position:relative;");

        header.appendChild(new Html(
            "<div style='background:linear-gradient(135deg,#1e3a8a,#2563eb,#3b82f6);color:#fff;"
            + "padding:14px 46px 14px 18px;'>"
            + "<div style='font-size:16px;font-weight:800;'>🧾 Wizard Pembayaran</div>"
            + "<div style='font-size:11px;opacity:.85;margin-top:2px;"
            + "white-space:nowrap;overflow:hidden;text-overflow:ellipsis;'>"
            + escHtml(mahasiswa.getNim()) + " · " + escHtml(mahasiswa.getNama())
            + "</div></div>"));

        MyButtonConfig btnX = new MyButtonConfig("✕");
        btnX.setStyle("position:absolute;top:10px;right:12px;min-width:28px;height:28px;border-radius:50%;"
                + "background:rgba(255,255,255,.2);color:#fff;border:0;font-weight:700;cursor:pointer;");
        btnX.addEventListener("onClick", new EventListener() {
            @Override public void onEvent(Event e) throws Exception { tutup(); }
        });
        header.appendChild(btnX);
        return header;
    }

    private void tutup() {
        try {
            if (onSelesai != null) onSelesai.onEvent(new Event("onClose", window, null));
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/WizardPembayaranMhsHelper.java:362"); /* ignore */ }
        try { window.detach(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/WizardPembayaranMhsHelper.java:363"); /* ignore */ }
    }

    // ============================================================ MOBILE CSS
    private void injectCssMobile(boolean mobile) {
        if (!mobile) return;
        try {
            org.zkoss.zk.ui.Desktop dt = window.getDesktop();
            if (dt == null || dt.getAttribute(CSS_ATTR) != null) return;
            dt.setAttribute(CSS_ATTR, Boolean.TRUE);
            window.getContentStyle();  // ensures window is accessible
            root.appendChild(new Html("<style>"
                + ".wz-mhs-mobile.z-window-highlighted,.wz-mhs-mobile{"
                + "position:fixed !important;left:0 !important;top:0 !important;"
                + "right:0 !important;bottom:0 !important;"
                + "width:100vw !important;height:100vh !important;height:100dvh !important;"
                + "max-width:none !important;max-height:none !important;"
                + "margin:0 !important;border-radius:0 !important;}"
                + ".wz-mhs-mobile .z-window-highlighted-cnt,"
                + ".wz-mhs-mobile .z-window-highlighted-cnt-noborder{"
                + "width:100% !important;height:100% !important;max-height:100% !important;"
                + "overflow:hidden !important;border-radius:0 !important;"
                + "box-sizing:border-box !important;}"
                + ".wz-mhs-mobile .z-window-highlighted-shadow{box-shadow:none !important;}"
                + "</style>"));
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/WizardPembayaranMhsHelper.java:388"); /* ignore */ }
    }

    // ============================================================ RENDER
    private void render() {
        sesuaikanUkuran();
        renderStepper();
        renderBody();
        renderFooter();
    }

    private void sesuaikanUkuran() {
        if (window == null) return;
        if (Common.isMobile()) {
            window.setWidth("100%");
            window.setHeight("100%");
        } else {
            window.setWidth("660px");
            // Langkah 1-4 diberi tinggi tetap agar BADAN wizard yang menggulir —
            // tanpa ini, konten panjang (mis. daftar jenis pembayaran) memanjangkan
            // window melewati layar TANPA scrollbar.
            boolean tallStep = langkah >= 1 && langkah <= 4;
            window.setHeight(tallStep ? "88%" : null);
            if (root != null) root.setHeight(tallStep ? "100%" : null);
        }
        try { window.setPosition("center"); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/WizardPembayaranMhsHelper.java:413"); /* ignore */ }
    }

    private void renderStepper() {
        Common.clear(stepperHost);
        StringBuilder sb = new StringBuilder();
        sb.append("<div style='display:flex;align-items:center;gap:4px;overflow-x:auto;'>");
        for (int i = 0; i < JUDUL.length; i++) {
            int n = i + 1;
            boolean aktif = n == langkah;
            boolean done = n < langkah;
            String bg = done ? "#16a34a" : (aktif ? "#2563eb" : "#e2e8f0");
            String fg = (done || aktif) ? "#fff" : "#94a3b8";
            String lc = aktif ? "#1e3a8a" : (done ? "#16a34a" : "#94a3b8");
            String isi = done ? "✓" : String.valueOf(n);
            sb.append("<div style='display:flex;align-items:center;gap:6px;flex:0 0 auto;'>")
              .append("<div style='width:26px;height:26px;border-radius:50%;background:").append(bg)
              .append(";color:").append(fg)
              .append(";display:flex;align-items:center;justify-content:center;font-weight:800;font-size:11px;'>")
              .append(isi).append("</div>")
              .append("<div style='font-size:11px;font-weight:700;color:").append(lc)
              .append(";white-space:nowrap;display:")
              .append((Common.isMobile() && !aktif) ? "none" : "block")
              .append(";'>").append(JUDUL[i]).append("</div></div>");
            if (i < JUDUL.length - 1) {
                sb.append("<div style='flex:1;height:2px;min-width:6px;border-radius:1px;background:")
                  .append(done ? "#16a34a" : "#e2e8f0").append(";'></div>");
            }
        }
        sb.append("</div>");
        stepperHost.appendChild(new Html(sb.toString()));
    }

    private void renderBody() {
        Common.clear(bodyHost);
        switch (langkah) {
            case 1: renderStep1(); break;
            case 2: renderStep2(); break;
            case 3: renderStep3(); break;
            case 4: renderStep4(); break;
            default: renderStep5(); break;
        }
    }

    private void renderFooter() {
        Common.clear(footerHost);

        // Kiri: Batal / Kembali
        MyButtonConfig btnKiri;
        if (langkah == 1) {
            btnKiri = new MyButtonConfig("Batal");
            btnKiri.setStyle(BTN_SECONDARY);
            btnKiri.addEventListener("onClick", new EventListener() {
                @Override public void onEvent(Event e) throws Exception { tutup(); }
            });
        } else {
            btnKiri = new MyButtonConfig("← Kembali");
            btnKiri.setStyle(BTN_SECONDARY);
            btnKiri.addEventListener("onClick", new EventListener() {
                @Override public void onEvent(Event e) throws Exception {
                    langkah = Math.max(1, langkah - 1);
                    render();
                }
            });
        }
        footerHost.appendChild(btnKiri);

        // Kanan: Lanjut / Selesai / Tutup
        if (langkah == 5) {
            MyButtonConfig btnTutup = new MyButtonConfig("Tutup");
            btnTutup.setStyle(BTN_PRIMARY);
            btnTutup.addEventListener("onClick", new EventListener() {
                @Override public void onEvent(Event e) throws Exception { tutup(); }
            });
            footerHost.appendChild(btnTutup);
        } else if (langkah < 4) {
            String label = langkah == 1 ? "Lanjut →"
                    : (langkah == 2 ? "Atur Nominal →" : "Lanjut Pilih Cara Bayar →");
            final String lbl = label;
            MyButtonConfig btnKanan = new MyButtonConfig(lbl);
            btnKanan.setStyle(BTN_PRIMARY);
            btnKanan.addEventListener("onClick", new EventListener() {
                @Override public void onEvent(Event e) throws Exception { onNext(e); }
            });
            footerHost.appendChild(btnKanan);
        }
        // Langkah 4 (Cara Bayar): tidak ada tombol lanjut — pilih gateway = aksi
    }

    private void onNext(Event e) throws Exception {
        switch (langkah) {
            case 1:
                if (!validasiStep1()) return;
                muatTagihan();
                langkah = 2;
                break;
            case 2:
                if (!validasiStep2()) return;
                langkah = 3;
                break;
            case 3:
                if (!validasiStep3()) return;
                langkah = 4;
                break;
            default:
                tutup();
                return;
        }
        render();
    }

    // ============================================================ STEP 1: JENIS & SEMESTER
    private void renderStep1() {
        Div wrap = new Div();
        wrap.setParent(bodyHost);
        wrap.setWidth("100%");
        wrap.setStyle("max-width:540px;margin:0 auto;display:flex;flex-direction:column;gap:12px;");

        // Info mahasiswa
        wrap.appendChild(new Html(
            "<div style='" + CARD_STYLE + "'>"
            + "<div style='font-size:11px;color:#64748b;font-weight:700;'>MAHASISWA</div>"
            + "<div style='font-size:15px;font-weight:800;color:#0f172a;margin-top:2px;'>"
            + escHtml(mahasiswa.getNama()) + "</div>"
            + "<div style='font-size:12px;color:#475569;'>" + escHtml(mahasiswa.getNim()) + "</div>"
            + "</div>"));

        wrap.appendChild(new Html(
            "<div style='font-size:12px;color:#475569;line-height:1.5;"
            + "background:#eff6ff;border:1px solid #bfdbfe;border-radius:10px;padding:10px 12px;'>"
            + "Pilih <b>jenis pembayaran</b> dan <b>semester</b> yang akan dibayar. "
            + "Tagihan akan dimuat pada langkah berikutnya.</div>"));

        wrap.appendChild(new Html("<label style='" + LABEL_SM + "'>Jenis Pembayaran</label>"));
        cboJenis = Common.initJenisPembayaranMahasiswa(new Combobox());
        // Bisa DIKETIK untuk lompat cepat ke jenis yang dicari (autocomplete bawaan
        // Combobox ZK) — penting saat daftar jenis pembayaran panjang.
        cboJenis.setReadonly(false);
        cboJenis.setAutodrop(true);
        cboJenis.setWidth("100%");
        if (jenisKegiatan != null) Common.selectComboItem(true, cboJenis, jenisKegiatan);
        wrap.appendChild(cboJenis);

        rowSmt = new Div();
        rowSmt.setWidth("100%");
        rowSmt.setStyle("display:flex;flex-direction:column;gap:6px;");
        rowSmt.appendChild(new Html("<label style='" + LABEL_SM + "'>Semester</label>"));

        cboSmt = new Combobox();
        cboSmt.setReadonly(true);
        cboSmt.setWidth("100%");
        Comboitem ciPlaceholder = new Comboitem("— pilih semester —");
        ciPlaceholder.setValue(null);
        cboSmt.appendChild(ciPlaceholder);
        if (semester == null) cboSmt.setSelectedItem(ciPlaceholder);

        int smtNow = -1;
        try { smtNow = mahasiswa.currentSemester(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/WizardPembayaranMhsHelper.java:569"); /* ignore */ }
        for (int i = 1; i <= 20; i++) {
            String label = "Semester " + i + (i == smtNow ? " (sekarang)" : "");
            Comboitem ci = new Comboitem(label);
            ci.setValue(i);
            cboSmt.appendChild(ci);
            // Default cerdas: semester berjalan terpilih otomatis (bisa diganti) —
            // mengurangi satu keputusan di langkah pertama, setara wizard lama.
            if (semester == null && i == smtNow) cboSmt.setSelectedItem(ci);
            if (semester != null && semester.intValue() == i) cboSmt.setSelectedItem(ci);
        }
        rowSmt.appendChild(cboSmt);
        wrap.appendChild(rowSmt);

        // --- Mode Keranjang: bayar beberapa jenis pembayaran sekali jalan (opsional) ---
        chkJenisEkstra.clear();
        Div kotakEkstra = new Div();
        kotakEkstra.setWidth("100%");
        kotakEkstra.setStyle(CARD_STYLE + "background:#fefce8;border-color:#fde68a;");
        kotakEkstra.appendChild(new Html(
            "<div style='font-size:12px;font-weight:800;color:#92400e;margin-bottom:2px;'>"
            + "🛒 Bayar sekaligus jenis pembayaran lain (opsional)</div>"
            + "<div style='font-size:11px;color:#a16207;line-height:1.5;margin-bottom:8px;'>"
            + "Centang bila Anda ingin membayar beberapa jenis tagihan sekaligus. Semua pilihan "
            + "akan digabung menjadi satu transaksi dan satu nomor pembayaran. Jangan centang "
            + "apa pun bila hanya membayar jenis yang dipilih di atas.</div>"));

        // Kotak pencarian: memfilter daftar checkbox di bawah (penting saat jenis banyak).
        kotakEkstra.appendChild(new Html(
            "<div style='font-size:11px;font-weight:700;color:#92400e;margin-bottom:2px;'>🔍 Cari jenis:</div>"));
        final org.zkoss.zul.Textbox cariJenis = new org.zkoss.zul.Textbox();
        cariJenis.setWidth("100%");
        cariJenis.setStyle("margin-bottom:6px;font-size:12px;box-sizing:border-box;");
        cariJenis.setTooltiptext("Ketik untuk memfilter daftar jenis pembayaran di bawah");
        kotakEkstra.appendChild(cariJenis);

        // Daftar checkbox di dalam WADAH BERGULIR ber-batas tinggi — daftar panjang tidak
        // lagi memanjangkan langkah 1 melebihi layar tanpa scrollbar.
        final Div daftarJenis = new Div();
        daftarJenis.setWidth("100%");
        daftarJenis.setStyle("max-height:230px;overflow-y:auto;overflow-x:hidden;"
                + "-webkit-overflow-scrolling:touch;padding-right:4px;box-sizing:border-box;");
        kotakEkstra.appendChild(daftarJenis);

        final List<Object[]> barisJenis = new ArrayList<Object[]>(); // [Div baris, String namaLower]
        try {
            for (Object o : cboJenis.getItems()) {
                if (!(o instanceof Comboitem)) continue;
                Object nilai = ((Comboitem) o).getValue();
                if (!(nilai instanceof JenisKegiatan)) continue;
                final JenisKegiatan jkOpsi = (JenisKegiatan) nilai;

                Div baris = new Div();
                baris.setStyle("display:flex;align-items:center;gap:8px;padding:3px 0;");
                org.zkoss.zul.Checkbox chk = new org.zkoss.zul.Checkbox();
                chk.setStyle("transform:scale(1.15);cursor:pointer;flex:0 0 auto;");
                boolean sudahDicentang = false;
                for (JenisKegiatan je : jenisEkstra) {
                    if (je.getId().equals(jkOpsi.getId())) { sudahDicentang = true; break; }
                }
                chk.setChecked(sudahDicentang);
                baris.appendChild(chk);
                baris.appendChild(new Html("<span style='font-size:12px;color:#374151;'>"
                        + escHtml(jkOpsi.getNama()) + "</span>"));
                daftarJenis.appendChild(baris);
                chkJenisEkstra.add(new Object[] { chk, jkOpsi });
                barisJenis.add(new Object[] { baris,
                        jkOpsi.getNama() == null ? "" : jkOpsi.getNama().toLowerCase() });
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/WizardPembayaranMhsHelper.java:637"); /* daftar opsional — jangan gagalkan langkah 1 */ }

        // Filter live: sembunyikan baris yang tidak cocok kata kunci (checkbox tercentang
        // TETAP tercentang meskipun barisnya disembunyikan).
        EventListener filterJenis = new EventListener() {
            @Override public void onEvent(Event e) throws Exception {
                String kunci = null;
                try {
                    if (e instanceof org.zkoss.zk.ui.event.InputEvent) {
                        kunci = ((org.zkoss.zk.ui.event.InputEvent) e).getValue();
                    }
                } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/WizardPembayaranMhsHelper.java:648"); /* fallback di bawah */ }
                if (kunci == null) kunci = cariJenis.getValue();
                kunci = kunci == null ? "" : kunci.trim().toLowerCase();
                for (Object[] b : barisJenis) {
                    Div baris = (Div) b[0];
                    String nama = (String) b[1];
                    baris.setVisible(kunci.isEmpty() || nama.contains(kunci));
                }
            }
        };
        cariJenis.addEventListener("onChanging", filterJenis);
        cariJenis.addEventListener("onChange", filterJenis);

        if (!chkJenisEkstra.isEmpty()) {
            wrap.appendChild(kotakEkstra);
        }
    }

    private boolean validasiStep1() {
        if (cboJenis == null || cboJenis.getSelectedItem() == null
                || !(cboJenis.getSelectedItem().getValue() instanceof JenisKegiatan)) {
            alertar("Pilih jenis pembayaran terlebih dahulu.");
            return false;
        }
        jenisKegiatan = (JenisKegiatan) cboJenis.getSelectedItem().getValue();

        if (cboSmt == null || cboSmt.getSelectedItem() == null
                || !(cboSmt.getSelectedItem().getValue() instanceof Integer)) {
            alertar("Pilih semester terlebih dahulu.");
            return false;
        }
        semester = (Integer) cboSmt.getSelectedItem().getValue();

        // Kumpulkan jenis tambahan (mode Keranjang); jenis utama tidak boleh dobel.
        jenisEkstra = new ArrayList<JenisKegiatan>();
        for (Object[] pasangan : chkJenisEkstra) {
            org.zkoss.zul.Checkbox chk = (org.zkoss.zul.Checkbox) pasangan[0];
            JenisKegiatan jk = (JenisKegiatan) pasangan[1];
            if (chk.isChecked() && jk.getId() != null && !jk.getId().equals(jenisKegiatan.getId())) {
                jenisEkstra.add(jk);
            }
        }
        return true;
    }

    /** @return true bila wizard berjalan dalam mode multi-jenis (Keranjang Belanja). */
    private boolean modeKeranjang() {
        return jenisEkstra != null && !jenisEkstra.isEmpty();
    }

    /** Seluruh jenis yang dibayar: jenis utama + jenis tambahan (urut tampil). */
    private List<JenisKegiatan> semuaJenisTerpilih() {
        List<JenisKegiatan> semua = new ArrayList<JenisKegiatan>();
        if (jenisKegiatan != null) semua.add(jenisKegiatan);
        semua.addAll(jenisEkstra);
        return semua;
    }

    // ============================================================ STEP 2: PILIH TAGIHAN
    @SuppressWarnings("unchecked")
    private void renderStep2() {
        if (tagihanItems.isEmpty()) {
            bodyHost.appendChild(new Html(
                "<div style='text-align:center;padding:40px 20px;color:#94a3b8;'>"
                + "<div style='font-size:40px;margin-bottom:10px;'>📋</div>"
                + "<div style='font-size:14px;font-weight:600;'>Tidak ada tagihan ditemukan</div>"
                + "<div style='font-size:12px;margin-top:6px;'>Periksa jenis pembayaran dan semester yang dipilih.</div>"
                + "</div>"));
            return;
        }

        // Header KONTEKS jenis+semester yang sedang dimuat — transparansi agar admin/
        // mahasiswa langsung tahu KENAPA daftar item berbeda dari sesi lain (mis. dua
        // sesi membuka semester berbeda untuk mahasiswa yang sama akan tampil item &
        // nominal berbeda, karena Setting Biaya memang per-semester; ini BUKAN bug,
        // tapi tanpa label ini gampang disangka bug).
        bodyHost.appendChild(new Html(
            "<div style='font-size:11px;color:#475569;background:#f1f5f9;border-radius:8px;"
            + "padding:6px 10px;margin-bottom:8px;'>"
            + "Menampilkan tagihan: <b>" + escHtml(jenisKegiatan == null ? "-" : jenisKegiatan.getNama())
            + "</b> &middot; Semester <b>" + (semester == null ? "-" : semester) + "</b></div>"));

        bodyHost.appendChild(new Html(
            "<div style='font-size:12px;color:#1e3a8a;font-weight:700;margin-bottom:6px;'>"
            + "Centang item tagihan yang akan dibayar:</div>"));

        // FILTER (permintaan user): sembunyikan item ber-tagihan Rp 0 (tidak ada
        // tagihannya) — default TERPILIH agar daftar langsung ringkas; bisa dimatikan
        // untuk melihat semua item termasuk yang Rp 0.
        int adaNol = 0;
        for (TagihanItem it : tagihanItems) {
            if (it.nominal <= 0) adaNol++;
        }
        if (adaNol > 0) {
            Div barisFilter = new Div();
            barisFilter.setStyle("display:flex;align-items:center;gap:8px;margin-bottom:10px;"
                    + "background:#f8fafc;border:1px solid #e2e8f0;border-radius:10px;padding:8px 12px;");
            final org.zkoss.zul.Checkbox chkFilter = new org.zkoss.zul.Checkbox(
                    "Tampilkan hanya yang ada tagihannya (" + adaNol + " item Rp 0 disembunyikan)");
            chkFilter.setChecked(hanyaAdaTagihan);
            chkFilter.setStyle("font-size:12px;color:#475569;cursor:pointer;");
            chkFilter.addEventListener("onCheck", new EventListener() {
                @Override public void onEvent(Event e) throws Exception {
                    hanyaAdaTagihan = chkFilter.isChecked();
                    render(); // gambar ulang daftar sesuai filter
                }
            });
            barisFilter.appendChild(chkFilter);
            bodyHost.appendChild(barisFilter);
        }

        for (final TagihanItem item : tagihanItems) {
            if (hanyaAdaTagihan && item.nominal <= 0) {
                continue; // item tanpa tagihan (Rp 0) disembunyikan oleh filter
            }
            String nama = namaItem(item);

            boolean lunas = item.kekurangan <= 0;
            String badge;
            if (lunas) badge = "<span style='background:#dcfce7;color:#16a34a;border-radius:6px;padding:2px 8px;font-size:11px;font-weight:700;'>✔ Lunas</span>";
            else if (item.sudahDibayar > 0) badge = "<span style='background:#fef9c3;color:#a16207;border-radius:6px;padding:2px 8px;font-size:11px;font-weight:700;'>Kurang</span>";
            else badge = "<span style='background:#fee2e2;color:#dc2626;border-radius:6px;padding:2px 8px;font-size:11px;font-weight:700;'>Belum</span>";

            Div card = new Div();
            card.setStyle(CARD_STYLE + (lunas ? "opacity:.6;" : ""));
            item.cardDiv = card;

            // Header baris: nama + badge + checkbox (jika belum lunas)
            Div headerRow = new Div();
            headerRow.setStyle("display:flex;align-items:flex-start;justify-content:space-between;gap:10px;");

            String tagJenis = "";
            if (modeKeranjang() && item.jenis != null) {
                tagJenis = " <span style='background:#eef2ff;color:#4338ca;border-radius:6px;padding:2px 8px;"
                        + "font-size:11px;font-weight:700;'>" + escHtml(item.jenis.getNama()) + "</span>";
            }
            headerRow.appendChild(new Html(
                "<div style='flex:1;min-width:0;'>"
                + "<div style='font-weight:700;color:#0f172a;font-size:14px;'>" + escHtml(nama) + "</div>"
                + "<div style='margin-top:4px;'>" + badge + tagJenis + "</div></div>"));

            if (!lunas) {
                final org.zkoss.zul.Checkbox chk = new org.zkoss.zul.Checkbox();
                chk.setChecked(item.dipilih);
                chk.setStyle("transform:scale(1.4);cursor:pointer;margin-top:4px;flex:0 0 auto;");
                chk.addEventListener("onCheck", new EventListener() {
                    @Override public void onEvent(Event e) throws Exception {
                        item.dipilih = chk.isChecked();
                        updateCardBorder(item);
                    }
                });
                headerRow.appendChild(chk);
            }
            card.appendChild(headerRow);

            card.appendChild(new Html(
                "<div style='display:flex;gap:16px;flex-wrap:wrap;font-size:12px;color:#475569;margin-top:8px;'>"
                + "<span><b>Tagihan:</b> " + formatRp(item.nominal) + "</span>"
                + "<span><b>Dibayar:</b> " + formatRp(item.sudahDibayar) + "</span>"
                + "<span><b>Kekurangan:</b> <b style='color:"
                + (item.kekurangan > 0 ? "#dc2626" : "#16a34a") + ";'>"
                + formatRp(item.kekurangan) + "</b></span></div>"));

            // Rincian proses penghitungan (mis. "Biaya SP Matakuliah Per SKS (Rp. 10.000) x
            // 15 SKS, sbb : ...") — hanya tampil untuk item ber-penghitungan PERKALIAN.
            if (item.keterangan != null && !item.keterangan.trim().isEmpty()) {
                card.appendChild(new Html(
                    "<div style='font-size:11px;color:#64748b;margin-top:4px;background:#f8fafc;"
                    + "border:1px dashed #cbd5e1;border-radius:6px;padding:4px 8px;'>"
                    + "&#128202; " + escHtml(item.keterangan) + "</div>"));
            }

            updateCardBorder(item);
            bodyHost.appendChild(card);
        }
    }

    private void updateCardBorder(TagihanItem item) {
        if (item.cardDiv == null) return;
        if (item.dipilih) {
            item.cardDiv.setStyle(CARD_STYLE + "border-color:#2563eb;box-shadow:0 0 0 2px rgba(37,99,235,.12);");
        } else {
            item.cardDiv.setStyle(CARD_STYLE + (item.kekurangan <= 0 ? "opacity:.6;" : ""));
        }
    }

    private boolean validasiStep2() {
        for (TagihanItem item : tagihanItems) {
            if (item.dipilih) return true;
        }
        alertar("Pilih minimal satu tagihan yang akan dibayar.");
        return false;
    }

    // ============================================================ STEP 3: ATUR NOMINAL
    private void renderStep3() {
        double totalKekurangan = hitungTotalKekuranganDipilih();
        bodyHost.appendChild(new Html(
            "<div style='font-size:12px;color:#334155;line-height:1.6;"
            + "background:#eff6ff;border:1px solid #93c5fd;border-radius:10px;padding:12px 14px;margin-bottom:12px;'>"
            + "<div style='font-size:14px;font-weight:800;color:#1e3a8a;margin-bottom:6px;'>"
            + "Apa yang harus diisi pada langkah ini?</div>"
            + "<div><b>1.</b> Angka yang tampil otomatis adalah <b>seluruh sisa tagihan</b> item yang dipilih: "
            + "<b>" + formatRp(totalKekurangan) + "</b>.</div>"
            + "<div><b>2.</b> Jika ingin <b>langsung lunas</b>, biarkan angka tersebut tanpa diubah.</div>"
            + "<div><b>3.</b> Jika item boleh dicicil, isi angka yang lebih kecil lalu tekan "
            + "<b>Lanjut Pilih Cara Bayar</b>.</div>"
            + "<div style='margin-top:7px;padding-top:7px;border-top:1px solid #bfdbfe;color:#475569;'>"
            + "Belum ada pembayaran yang diproses pada halaman ini. Pembayaran baru dilanjutkan setelah Anda "
            + "memilih bank, Virtual Account, QRIS, atau Tunai/Kasir pada langkah berikutnya.</div></div>"));

        final Div[] totalDiv = new Div[1];
        totalDiv[0] = new Div();
        totalDiv[0].setStyle(CARD_STYLE + "background:#f0fdf4;border-color:#bbf7d0;margin-bottom:14px;");
        rebuiltTotalCard(totalDiv[0]);
        bodyHost.appendChild(totalDiv[0]);

        for (final TagihanItem item : tagihanItems) {
            if (!item.dipilih) continue;
            String nama = namaItem(item);

            Div card = new Div();
            card.setStyle(CARD_STYLE);

            card.appendChild(new Html(
                "<div style='display:flex;align-items:center;justify-content:space-between;gap:8px;flex-wrap:wrap;'>"
                + "<div style='font-weight:800;color:#1e3a8a;font-size:14px;'>" + escHtml(nama) + "</div>"
                + "<span style='font-size:10px;font-weight:800;border-radius:999px;padding:3px 8px;"
                + (item.bisaDiubah ? "background:#dcfce7;color:#166534;'>BOLEH DICICIL"
                        : "background:#fef3c7;color:#92400e;'>WAJIB DIBAYAR PENUH")
                + "</span></div>"
                + "<div style='display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:6px;margin:10px 0;'>"
                + kotakNominal("Tagihan", item.nominal, "#0f172a")
                + kotakNominal("Sudah Dibayar", item.sudahDibayar, "#166534")
                + kotakNominal("Sisa Tagihan", item.kekurangan, "#dc2626") + "</div>"
                + (item.keterangan == null || item.keterangan.trim().isEmpty() ? "" :
                "<div style='font-size:11px;color:#64748b;margin-bottom:10px;background:#f8fafc;"
                + "border:1px dashed #cbd5e1;border-radius:6px;padding:4px 8px;'>"
                + "&#128202; " + escHtml(item.keterangan) + "</div>")));

            if (item.bisaDiubah) {
                card.appendChild(new Html("<label style='" + LABEL_SM + "'>Nominal yang Dibayar Sekarang (Rp)</label>"
                    + "<div style='font-size:11px;color:#475569;margin:-1px 0 7px 0;'>"
                    + "Untuk melunasi <b>" + escHtml(nama) + "</b>, biarkan <b>" + formatRp(item.kekurangan)
                    + "</b>. Untuk mencicil, isi lebih kecil dari angka tersebut.</div>"));

                final Decimalbox dec = new Decimalbox();
                dec.setValue(new java.math.BigDecimal(item.nominalBayar > 0 ? item.nominalBayar : item.kekurangan));
                dec.setWidth("100%");
                dec.setStyle("font-size:14px;font-weight:700;box-sizing:border-box;");
                dec.setFormat("#,##0.##");
                dec.addEventListener("onChange", new EventListener() {
                    @Override public void onEvent(Event e) throws Exception {
                        try {
                            double v = dec.getValue() != null ? dec.getValue().doubleValue() : 0;
                            if (v < 0) { v = 0; dec.setValue(java.math.BigDecimal.ZERO); }
                            if (v > item.kekurangan + 0.01) {
                                v = item.kekurangan;
                                dec.setValue(new java.math.BigDecimal(item.kekurangan));
                                alertar("Tidak boleh melebihi kekurangan: " + formatRp(item.kekurangan));
                            }
                            item.nominalBayar = v;
                            rebuiltTotalCard(totalDiv[0]);
                        } catch (Exception ex) { item.nominalBayar = 0; }
                    }
                });
                card.appendChild(dec);
            } else {
                // OVERRIDE Tagihan Default / item non-cicil: WAJIB dibayar penuh —
                // nominal terkunci (paritas checkout JSP + aturan SettingBiaya).
                item.nominalBayar = item.kekurangan;
                card.appendChild(new Html(
                    "<div style='font-size:15px;font-weight:800;color:#0f172a;'>"
                    + formatRp(item.nominalBayar) + "</div>"
                    + "<div style='font-size:11px;color:#94a3b8;margin-top:2px;'>"
                    + "🔒 dibayar penuh (tidak dapat diangsur)</div>"));
            }

            // Field "Tanggal Bayar" HANYA untuk admin/kasir (permintaan user): pengguna
            // mahasiswa/calon mahasiswa tidak perlu memilih tanggal — pembayaran mereka
            // selalu tercatat hari ini (fallback WaktuUtil.getDate() saat eksekusi).
            if (isUserAdmin()) {
                Div tglDiv = new Div();
                tglDiv.setStyle("margin-top:10px;");
                tglDiv.appendChild(new Html("<label style='" + LABEL_SM + "'>Tanggal Bayar (opsional, default hari ini)</label>"));
                final Datebox dte = new Datebox();
                dte.setValue(item.tanggalBayar != null ? item.tanggalBayar : new Date());
                dte.setWidth("100%");
                dte.addEventListener("onChange", new EventListener() {
                    @Override public void onEvent(Event e) throws Exception {
                        item.tanggalBayar = dte.getValue();
                    }
                });
                tglDiv.appendChild(dte);
                card.appendChild(tglDiv);
            }

            bodyHost.appendChild(card);
        }
    }

    private void rebuiltTotalCard(Div totalCard) {
        if (totalCard == null) return;
        double total = hitungTotalBayar();
        double totalKekurangan = hitungTotalKekuranganDipilih();
        double sisaSetelahBayar = Math.max(0, totalKekurangan - total);
        Common.clear(totalCard);
        totalCard.appendChild(new Html(
            "<div style='font-size:11px;color:#16a34a;font-weight:800;'>TOTAL YANG AKAN DIPROSES SEKARANG</div>"
            + "<div style='font-size:22px;font-weight:800;color:#15803d;margin-top:4px;'>"
            + formatRp(total) + "</div>"
            + "<div style='font-size:11px;color:#475569;margin-top:6px;line-height:1.5;'>"
            + (sisaSetelahBayar <= 0.01
                    ? "Nominal ini akan <b>melunasi seluruh item yang dipilih</b>."
                    : "Ini pembayaran <b>sebagian/angsuran</b>. Perkiraan sisa setelah pembayaran: <b>"
                            + formatRp(sisaSetelahBayar) + "</b>.")
            + " Biaya administrasi kanal pembayaran, jika ada, akan ditampilkan pada langkah berikutnya.</div>"));
    }

    private double hitungTotalKekuranganDipilih() {
        double total = 0.0;
        for (TagihanItem item : tagihanItems) {
            if (item.dipilih) total += Math.max(0, item.kekurangan);
        }
        return total;
    }

    private String kotakNominal(String label, double nilai, String warna) {
        return "<div style='background:#f8fafc;border:1px solid #e2e8f0;border-radius:7px;padding:7px;min-width:0;'>"
                + "<div style='font-size:9px;color:#64748b;font-weight:700;text-transform:uppercase;'>"
                + escHtml(label) + "</div><div style='font-size:11px;color:" + warna
                + ";font-weight:800;margin-top:2px;word-break:break-word;'>" + formatRp(nilai) + "</div></div>";
    }

    private boolean validasiStep3() {
        for (TagihanItem item : tagihanItems) {
            if (!item.dipilih) continue;
            if (item.nominalBayar <= 0) {
                alertar("Nominal bayar harus lebih dari 0 untuk setiap item yang dipilih.");
                return false;
            }
        }
        if (hitungTotalBayar() <= 0) {
            alertar("Total pembayaran harus lebih dari 0.");
            return false;
        }
        return true;
    }

    // ============================================================ STEP 4: CARA BAYAR

    /**
     * Merender langkah "Cara Bayar": ringkasan total + item terpilih, disusul kartu-kartu
     * saluran pembayaran. Daftar saluran diambil dari {@link PembayaranGatewayKatalog}
     * dengan kapabilitas {@link PembayaranGatewayKatalog#KAPABILITAS_WIZARD_ZK}, sehingga
     * aturan on/off konfigurasi identik dengan DaftarUlangMahasiswa*Action dan checkout JSP.
     * Tombol Tunai hanya tampil untuk pengguna admin/kasir yang lolos gate
     * {@code aktifkan_pembayaran_manual} dan tidak masuk daftar
     * {@code admin_lain_yang_tidak_bisa_membayar_langsung}.
     */
    private void renderStep4() {
        // reset sisa info VA dari percobaan sebelumnya
        vaLabelBank = null;
        vaKode = null;
        vaTotal = null;
        vaKadaluarsa = null;
        vaQrUrl = null;

        double total = hitungTotalBayar();
        final List<TagihanItem> dipilih = getItemsDipilih();

        bodyHost.appendChild(new Html(
            "<div style='font-size:12px;color:#334155;line-height:1.6;background:#eff6ff;"
            + "border:1px solid #93c5fd;border-radius:10px;padding:12px 14px;margin-bottom:12px;'>"
            + "<div style='font-size:14px;font-weight:800;color:#1e3a8a;margin-bottom:5px;'>"
            + "Cara menyelesaikan pembayaran</div>"
            + "<div><b>1.</b> Periksa kembali total dan item di bawah.</div>"
            + "<div><b>2.</b> Klik <b>satu</b> cara pembayaran yang tersedia.</div>"
            + "<div><b>3.</b> Untuk VA/QRIS, ikuti nomor atau kode yang diterbitkan sampai transaksi berhasil. "
            + "Status lunas diperbarui setelah konfirmasi diterima dari bank.</div>"
            + (isUserAdmin() ? "<div><b>4.</b> Pilih Tunai/Kasir hanya jika uang benar-benar sudah diterima.</div>" : "")
            + "</div>"));

        // Ringkasan total
        bodyHost.appendChild(new Html(
            "<div style='" + CARD_STYLE + "background:#f0fdf4;border-color:#bbf7d0;margin-bottom:12px;'>"
            + "<div style='font-size:11px;color:#16a34a;font-weight:700;'>TOTAL PEMBAYARAN</div>"
            + "<div style='font-size:24px;font-weight:800;color:#15803d;margin-top:4px;'>"
            + formatRp(total) + "</div></div>"));

        // Ringkasan item
        Div ringkasan = new Div();
        ringkasan.setStyle(CARD_STYLE + "margin-bottom:14px;");
        ringkasan.appendChild(new Html("<div style='font-weight:700;color:#1e3a8a;margin-bottom:8px;font-size:13px;'>Item yang dibayar:</div>"));
        for (TagihanItem item : dipilih) {
            String nama = namaItem(item);
            ringkasan.appendChild(new Html(
                "<div style='display:flex;justify-content:space-between;padding:5px 0;"
                + "border-bottom:1px solid #f1f5f9;font-size:12px;'>"
                + "<span style='color:#374151;'>" + escHtml(nama) + "</span>"
                + "<span style='font-weight:700;color:#0f172a;'>" + formatRp(item.nominalBayar) + "</span></div>"));
        }
        bodyHost.appendChild(ringkasan);

        bodyHost.appendChild(new Html("<div style='" + LABEL_SM + "margin-bottom:10px;'>Pilih cara pembayaran:</div>"));

        Div btnWrap = new Div();
        btnWrap.setStyle("display:flex;flex-wrap:wrap;gap:10px;");
        bodyHost.appendChild(btnWrap);

        int jumlahTombol = 0;

        // Gating jadwal pembayaran (paritas DaftarUlang/JSP): di luar masa pembayaran,
        // pengguna non-admin tidak boleh membayar daring; admin tetap diizinkan.
        boolean adaJadwal = jadwalPembayaran != null;
        boolean userAdmin = isUserAdmin();
        if (!adaJadwal) {
            bodyHost.appendChild(new Html(
                "<div style='" + CARD_STYLE + "background:#fffbeb;border-color:#fde68a;margin-bottom:12px;'>"
                + "<div style='font-weight:700;color:#92400e;font-size:12px;'>⚠️ Jadwal pembayaran tidak ditemukan</div>"
                + "<div style='font-size:11px;color:#a16207;margin-top:2px;'>Belum ada jadwal pembayaran aktif "
                + "untuk jenis kegiatan dan semester ini"
                + (userAdmin ? "; sebagai admin Anda tetap dapat melanjutkan." : ". Silakan hubungi bagian keuangan.")
                + "</div></div>"));
        }
        boolean bolehBayarDaring = adaJadwal || userAdmin;

        if (modeKeranjang()) {
            // ================= MODE KERANJANG (multi jenis pembayaran, 1x bayar) =========
            StringBuilder namaJenis = new StringBuilder();
            for (JenisKegiatan jk : semuaJenisTerpilih()) {
                if (namaJenis.length() > 0) namaJenis.append(", ");
                namaJenis.append(jk.getNama());
            }
            bodyHost.insertBefore(new Html(
                "<div style='" + CARD_STYLE + "background:#eef2ff;border-color:#c7d2fe;margin-bottom:12px;'>"
                + "<div style='font-weight:800;color:#4338ca;font-size:12px;'>🛒 Mode Keranjang Belanja</div>"
                + "<div style='font-size:11px;color:#4f46e5;margin-top:2px;line-height:1.5;'>"
                + "Membayar " + semuaJenisTerpilih().size() + " jenis sekaligus ("
                + escHtml(namaJenis.toString()) + ") — satu Virtual Account untuk semuanya. "
                + "Saluran yang tersedia terbatas pada gerbang yang mendukung keranjang.</div></div>"),
                btnWrap);

            if (bolehBayarDaring) {
                jumlahTombol += renderTombolKeranjang(btnWrap, dipilih);
            }
        } else {

        // --- Tunai / manual di kasir (khusus admin, gate identik _lanjut_bayar.jsp) ---
        if (bolehTunai()) {
            tambahTombolBayar(btnWrap, "💵 Bayar Tunai / Kasir", "#16a34a", "#dcfce7", new EventListener() {
                @Override public void onEvent(Event e) throws Exception {
                    konfirmasiBayar("Tunai / Kasir", new EventListener() {
                        @Override public void onEvent(Event ev) throws Exception { bayarTunai(dipilih); }
                    });
                }
            });
            jumlahTombol++;
        }

        // --- Saluran daring dari katalog bersama ---
        if (bolehBayarDaring) {
            for (final PembayaranGatewayKatalog.Gateway g : PembayaranGatewayKatalog.yangTampil(
                    jenisKegiatan, PembayaranGatewayKatalog.KAPABILITAS_WIZARD_ZK)) {
                String emoji = PembayaranGatewayKatalog.KATEGORI_BANK_ONLINE.equals(g.kategori) ? "🏦" : "💳";
                if ("qris".equals(g.id)) emoji = "🔳";
                tambahTombolBayar(btnWrap, emoji + " " + g.label, "#1e40af", "#eff6ff", new EventListener() {
                    @Override public void onEvent(final Event e) throws Exception {
                        konfirmasiBayar(g.label, new EventListener() {
                            @Override public void onEvent(Event ev) throws Exception { prosesPayment(g, dipilih, e); }
                        });
                    }
                });
                jumlahTombol++;
            }
        }

        }

        if (jumlahTombol == 0) {
            bodyHost.appendChild(new Html(
                "<div style='" + CARD_STYLE + "background:#fffbeb;border-color:#fde68a;text-align:center;'>"
                + "<div style='font-size:28px;margin-bottom:6px;'>⚠️</div>"
                + "<div style='font-weight:700;color:#92400e;font-size:13px;'>Saluran pembayaran belum tersedia</div>"
                + "<div style='font-size:12px;color:#a16207;margin-top:4px;'>Belum ada saluran pembayaran aktif "
                + "untuk jenis kegiatan ini. Silakan hubungi administrator sistem.</div></div>"));
        }
    }

    // ============================================================ MODE KERANJANG

    /**
     * Merender tombol saluran untuk mode Keranjang (multi jenis pembayaran, satu kali
     * bayar). Saluran yang mendukung: keluarga VA Bank Online (Online/Online 2/
     * Smartlink/Maja — via {@code DownloadTagihanMahasiswaBankOnline.sendRequest} dengan
     * token {@code Keranjang-<id>}) serta gerbang keranjang khusus BNI/BSI/Faspay/
     * Jatelindo ({@code *KeranjangPembayaran.onSaveXxx}). Konfigurasi on/off tetap dari
     * {@link PembayaranGatewayKatalog} yang sama dengan mode biasa.
     */
    private int renderTombolKeranjang(Div btnWrap, final List<TagihanItem> dipilih) {
        int jumlah = 0;

        // --- Keluarga VA Bank Online (satu VA untuk seluruh keranjang) ---
        String[] idVaOnline = { "online", "online_2", "smartlink", "maja" };
        for (String idGw : idVaOnline) {
            final PembayaranGatewayKatalog.Gateway g = PembayaranGatewayKatalog.cari(idGw);
            if (g == null || !PembayaranGatewayKatalog.aktif(g)) continue;
            tambahTombolBayar(btnWrap, "🛒 " + g.label, "#4338ca", "#eef2ff", new EventListener() {
                @Override public void onEvent(Event e) throws Exception {
                    konfirmasiBayar(g.label + " (Keranjang)", new EventListener() {
                        @Override public void onEvent(Event ev) throws Exception {
                            bayarKeranjangVaOnline(g, dipilih);
                        }
                    });
                }
            });
            jumlah++;
        }

        // --- Gerbang keranjang khusus (request-based) ---
        String[][] gwRequest = { { "bni", "🏦 BAYAR VIA BNI" }, { "bsi", "🏦 BAYAR VIA BSI" },
                { "faspay", "💳 BAYAR VIA FASPAY" }, { "jatelindo", "🏦 BAYAR VIA JATELINDO" } };
        for (String[] def : gwRequest) {
            final String idGw = def[0];
            final String label = def[1];
            PembayaranGatewayKatalog.Gateway g = PembayaranGatewayKatalog.cari(idGw);
            if (g == null || !PembayaranGatewayKatalog.aktif(g)) continue;
            tambahTombolBayar(btnWrap, "🛒 " + label, "#1e40af", "#eff6ff", new EventListener() {
                @Override public void onEvent(final Event e) throws Exception {
                    konfirmasiBayar(label + " (Keranjang)", new EventListener() {
                        @Override public void onEvent(Event ev) throws Exception {
                            bayarKeranjangRequest(idGw, dipilih, e);
                        }
                    });
                }
            });
            jumlah++;
        }
        return jumlah;
    }

    /**
     * Menyimpan pilihan pembayaran multi-jenis sebagai draf {@link KegiatanTemporary} —
     * SATU draf per jenis kegiatan — beserta {@link CicilanPembayaran} anak per item
     * (ber-{@code kegiatanTemporary}, belum ber-{@code kegiatan}), meniru persis
     * {@code DaftarUlangMahasiswaLamaAction.onSave} (tombol KERANJANG): dedup pada
     * kombinasi (mahasiswa, jenisKegiatan, semester) yang belum terkonversi, cicilan draf
     * lama yang belum terproses dibersihkan lalu diisi ulang sesuai nominal wizard.
     * Draf inilah yang dirujuk token VA {@code Keranjang-<id>-<nilai>} dan dikonversi
     * menjadi Kegiatan nyata oleh servlet penerima pembayaran saat VA dibayar.
     *
     * @return kumpulan draf siap bayar, atau null bila gagal
     */
    private Set<KegiatanTemporary> simpanKeranjang(List<TagihanItem> dipilih) {
        Set<KegiatanTemporary> hasil = new LinkedHashSet<KegiatanTemporary>();
        Session session = null;
        try {
            session = HibernateUtil.openSession();
            String validator = getCurrentUserNama();
            String ta = getTahunAkademik();

            for (JenisKegiatan jk : semuaJenisTerpilih()) {
                double totalJenis = 0;
                List<TagihanItem> itemsJenis = new ArrayList<TagihanItem>();
                for (TagihanItem item : dipilih) {
                    if (item.jenis != null && item.jenis.getId().equals(jk.getId())
                            && item.nominalBayar > 0) {
                        itemsJenis.add(item);
                        totalJenis += item.nominalBayar;
                    }
                }
                if (itemsJenis.isEmpty() || totalJenis <= 0) continue;

                // Dedup pola DaftarUlang: satu draf per (mahasiswa, jenis, semester)
                // yang BELUM terkonversi (kegiatan masih null).
                KegiatanTemporary kt = (KegiatanTemporary) session.createCriteria(KegiatanTemporary.class)
                        .add(Restrictions.eq("mahasiswa", mahasiswa))
                        .add(Restrictions.eq("jenisKegiatan", jk))
                        .add(Restrictions.eq("semster", semester))
                        .add(Restrictions.isNull("kegiatan"))
                        .setMaxResults(1).uniqueResult();
                if (kt == null) {
                    kt = new KegiatanTemporary();
                }
                kt.setJenisKegiatan(jk);
                kt.setJadwalPembayaran(jadwalPerJenis.get(jk.getId()));
                kt.setMahasiswa(mahasiswa);
                kt.setSemster(semester);
                kt.setStatusMahasiswa(ConstantValues.AKTIF);
                kt.setTahunAkademik(ta);
                kt.setTanggal(WaktuUtil.getDate());
                kt.setValidated(1);
                kt.setValidator(validator);
                kt.setKeterangan(jk.getNama() + " (Wizard Keranjang)");
                kt.setAmount(totalJenis);

                session.beginTransaction();
                Common.refreshSaveOrUpdate(session, kt);
                session.getTransaction().commit();

                // Bersihkan cicilan draf lama yang belum terproses (kegiatan masih null),
                // lalu isi ulang sesuai nominal pilihan wizard (delete per-entity agar
                // riwayat audit Envers tetap tercatat).
                @SuppressWarnings("unchecked")
                List<CicilanPembayaran> lamaList = session.createCriteria(CicilanPembayaran.class)
                        .add(Restrictions.eq("kegiatanTemporary", kt))
                        .add(Restrictions.isNull("kegiatan")).list();
                session.beginTransaction();
                for (CicilanPembayaran lama : lamaList) {
                    session.delete(lama);
                }
                session.getTransaction().commit();

                int ke = 1;
                session.beginTransaction();
                for (TagihanItem item : itemsJenis) {
                    CicilanPembayaran cicilan = new CicilanPembayaran(item.detailBiaya);
                    cicilan.setKegiatanTemporary(kt);
                    if (item.detailBiaya != null) cicilan.setItemBiaya(item.detailBiaya.getItemBiaya());
                    // Alokasi slot bulanan (mode angsuran) agar konversi keranjang tercatat benar
                    if (item.bulanan != null) cicilan.setPengaturanPembayaranBulanan(item.bulanan);
                    cicilan.setNilai(item.nominalBayar);
                    cicilan.setNilaiAsli(item.nominalBayar);
                    cicilan.setTanggal(item.tanggalBayar != null ? item.tanggalBayar : WaktuUtil.getDate());
                    cicilan.setKe(ke++);
                    cicilan.setKeterangan("Wizard Keranjang");
                    cicilan.setValidator(validator);
                    session.save(cicilan);
                }
                session.getTransaction().commit();

                hasil.add(kt);
            }
            return hasil;
        } catch (Exception ex) {
            if (session != null) {
                try { session.getTransaction().rollback(); } catch (Exception ex2) { ais.common.ErrorAuditUtil.record(ex2, "auto-audit(empty-catch) src/ais/action/master/helper/WizardPembayaranMhsHelper.java:1173"); /* ignore */ }
            }
            alertar("Gagal menyiapkan keranjang pembayaran: " + ex.getMessage());
            return null;
        } finally {
            HibernateUtil.closeSessionQuietly(session);
        }
    }

    /**
     * Eksekutor keranjang keluarga VA Bank Online: simpan draf lalu LANGSUNG menerbitkan
     * VA via {@code DownloadTagihanMahasiswaBankOnline.sendRequest} — tanpa membuka
     * halaman Keranjang Belanja. Kolom cicilan VA berisi token
     * {@code Keranjang-<idDraf>-<nilai>} yang dipahami
     * {@code PembayaranGatewayHelper.prosesSatuTokenKeranjang} pada seluruh servlet
     * penerima pembayaran.
     */
    private void bayarKeranjangVaOnline(PembayaranGatewayKatalog.Gateway g, List<TagihanItem> dipilih) {
        try {
            Set<KegiatanTemporary> set = simpanKeranjang(dipilih);
            if (set == null || set.isEmpty()) {
                if (set != null) alertar("Tidak ada item dengan nominal pembayaran pada keranjang.");
                return;
            }

            double fee = 0;
            try {
                fee = Double.parseDouble(Common.getKonfigurasi(g.adminFeeConfig, "0.0").getNilai());
            } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/WizardPembayaranMhsHelper.java:1201"); /* fee opsional */ }
            BankHost bankHost = PembayaranUtil.getInstance()
                    .getBankHost(Common.getKonfigurasi(g.bankHostConfig, "").getNilai(), "Bank Host");

            Map param = new HashMap();
            if ("smartlink".equals(g.id)) param.put("smartlink", Boolean.TRUE);
            else if ("maja".equals(g.id)) param.put("maja", Boolean.TRUE);

            VirtualAccountBank va = DownloadTagihanMahasiswaBankOnline.sendRequest(mahasiswa, null, set, fee,
                    PerguruanTinggiUtil.getPerguruanTinggi(), bankHost, param, null);

            if (va != null && va.getLink() != null && !va.getLink().trim().isEmpty()) {
                String link = va.getLink().replace("'", "%27");
                Clients.evalJavaScript("window.open('" + link + "','_blank');");
                langkah = 5;
                render();
                return;
            }
            if (va != null) {
                simpanInfoVa(g, va, fee);
                vaLabelBank = g.label + " · KERANJANG";
                langkah = 5;
                render();
            } else {
                alertar("Mohon maaf, penerbitan Virtual Account keranjang tidak berhasil. "
                        + "Periksa koneksi/konfigurasi host bank, lalu ulangi.");
            }
        } catch (Exception e) {
            alertar("Gagal memproses keranjang " + g.label + ": " + e.getMessage());
        }
    }

    /**
     * Eksekutor keranjang gerbang request-based (BNI/BSI/Faspay/Jatelindo): simpan draf
     * lalu delegasi ke {@code *KeranjangPembayaran.onSaveXxx} — persis mesin yang dipakai
     * halaman Keranjang Belanja; penanda keranjang pada jalur ini adalah relasi
     * {@code *Request.kegiatanTemporarys} yang sudah dipahami servlet callback masing-masing.
     */
    private void bayarKeranjangRequest(String idGw, List<TagihanItem> dipilih, Event evt) {
        try {
            Set<KegiatanTemporary> set = simpanKeranjang(dipilih);
            if (set == null || set.isEmpty()) {
                if (set != null) alertar("Tidak ada item dengan nominal pembayaran pada keranjang.");
                return;
            }
            double total = hitungTotalBayar();
            if ("bni".equals(idGw)) {
                BniKeranjangPembayaran.onSaveBni(total, mahasiswa, null, set, evt);
            } else if ("bsi".equals(idGw)) {
                BsiKeranjangPembayaran.onSaveBsi(total, mahasiswa, null, set, evt);
            } else if ("faspay".equals(idGw)) {
                FaspayKeranjangPembayaran.onSaveFaspay(total, mahasiswa, null, set, evt);
            } else if ("jatelindo".equals(idGw)) {
                JatelindoKeranjangPembayaran.onSaveJatelindo(total, mahasiswa, null, set, evt);
            }
            langkah = 5;
            render();
        } catch (Exception e) {
            alertar("Gagal memproses keranjang: " + e.getMessage());
        }
    }

    /** Membuat satu kartu tombol saluran pembayaran dengan target sentuh ramah mobile. */
    private void tambahTombolBayar(Div parent, String label, String color, String bg, EventListener onClick) {
        MyButtonConfig btn = new MyButtonConfig(label);
        btn.setStyle("background:" + bg + ";color:" + color + ";border:1.5px solid " + color + ";"
                + "border-radius:10px;padding:14px 18px;font-size:13px;font-weight:700;cursor:pointer;"
                + "min-width:150px;min-height:48px;text-align:center;flex:1 1 150px;max-width:100%;"
                + "box-shadow:0 1px 4px rgba(0,0,0,.07);");
        btn.addEventListener("onClick", onClick);
        parent.appendChild(btn);
    }

    /**
     * Gate tombol Tunai — replikasi {@code _lanjut_bayar.jsp} dan DaftarUlang: hanya
     * pengguna admin (bukan akun mahasiswa/calon), konfigurasi
     * {@code aktifkan_pembayaran_manual} aktif (default AKTIF), tidak termasuk daftar
     * admin terlarang, dan jenis kegiatan mengizinkan token {@code ;tunai;}.
     */
    /**
     * @return true HANYA bila pengguna aktif terbukti admin: akun ter-resolve (bukan null)
     *         dan tidak terkait entitas Mahasiswa maupun BiodataCalonMahasiswa.
     *         Resolusi user memakai {@code Common.getCurrentUser()} (mencakup fallback
     *         atribut sesi "users"/"usersTemp") dengan cadangan {@code getTbmuser()};
     *         bila keduanya gagal, dianggap BUKAN admin (fail-closed) sehingga tombol
     *         Tunai/Kasir tidak pernah tampil bagi mahasiswa/calon mahasiswa.
     */
    private boolean isUserAdmin() {
        Tbmuser u = null;
        try { u = Common.getCurrentUser(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/WizardPembayaranMhsHelper.java:1290"); /* fallback di bawah */ }
        if (u == null) {
            try { u = Common.getTbmuser(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/WizardPembayaranMhsHelper.java:1292"); /* tetap null */ }
        }
        if (u == null) return false;
        try {
            if (u.getMahasiswa() != null) return false;
        } catch (Exception e) {
            return false;
        }
        try {
            if (u.getBiodataCalonMahasiswa() != null) return false;
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    /**
     * Dialog konfirmasi sebelum eksekusi pembayaran + guard anti klik-ganda — replikasi
     * pola DaftarUlang (dialog rincian sebelum bayar, guard {@code bayarSedangDiproses}).
     * {@code aksi} hanya dijalankan bila pengguna menekan Ya dan tidak ada eksekusi lain
     * yang sedang berlangsung; guard dilepas kembali di blok finally.
     */
    private void konfirmasiBayar(String labelSaluran, final EventListener aksi) throws Exception {
        final double total = hitungTotalBayar();
        MyMessageboxConfig.show(
                "Proses pembayaran sebesar " + formatRp(total) + " melalui " + labelSaluran + "?",
                "Konfirmasi Pembayaran",
                MyMessageboxConfig.YES.intValue() | MyMessageboxConfig.NO.intValue(),
                MyMessageboxConfig.QUESTION, new EventListener() {
                    @Override public void onEvent(Event e) throws Exception {
                        if (!"onYes".equals(e.getName())) return;
                        if (sedangProses) return;
                        sedangProses = true;
                        try {
                            aksi.onEvent(e);
                        } finally {
                            sedangProses = false;
                        }
                    }
                });
    }

    private boolean bolehTunai() {
        try {
            if (!isUserAdmin()) return false;
            Tbmuser u = null;
            try { u = Common.getCurrentUser(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/WizardPembayaranMhsHelper.java:1338"); /* fallback */ }
            if (u == null) u = Common.getTbmuser();
            if (u == null) return false;
            if (PembayaranGatewayKatalog.adminDilarangBayarLangsung(u.getUserId())) return false;
            return PembayaranGatewayKatalog.tunaiAktif(jenisKegiatan);
        } catch (Exception e) {
            return false;
        }
    }

    // ============================================================ STEP 5: SELESAI

    /**
     * Langkah penutup. Bila saluran keluarga Bank Online menerbitkan Virtual Account,
     * nomor VA, total (termasuk biaya administrasi), dan batas waktu pembayaran
     * ditampilkan sebagai kartu yang mudah dibaca/disalin; selain itu ditampilkan
     * konfirmasi sukses sederhana.
     */
    private void renderStep5() {
        boolean adaVa = vaKode != null && !vaKode.trim().isEmpty();
        boolean adaQr = vaQrUrl != null && !vaQrUrl.trim().isEmpty();
        boolean menungguPembayaranBank = adaVa || adaQr;
        bodyHost.appendChild(new Html(
            "<div style='text-align:center;padding:24px 20px 8px;'>"
            + "<div style='font-size:56px;margin-bottom:12px;'>"
            + (menungguPembayaranBank ? "🧾" : "✅") + "</div>"
            + "<div style='font-size:20px;font-weight:800;color:#0f172a;margin-bottom:6px;'>"
            + (menungguPembayaranBank ? "Instruksi Pembayaran Siap" : "Pembayaran Selesai") + "</div>"
            + "<div style='font-size:13px;color:#64748b;max-width:400px;margin:0 auto;line-height:1.6;'>"
            + (menungguPembayaranBank
                    ? "Transaksi belum dinyatakan lunas. Selesaikan pembayaran sesuai petunjuk di bawah. "
                            + "Status tagihan akan diperbarui setelah pembayaran dikonfirmasi oleh bank."
                    : "Pembayaran berhasil diproses dan dicatat. Anda dapat menutup jendela ini atau "
                            + "kembali untuk melakukan pembayaran lain.")
            + "</div>"
            + "</div>"));

        if (adaVa || adaQr) {
            StringBuilder sb = new StringBuilder();
            sb.append("<div style='" + CARD_STYLE + "max-width:420px;margin:14px auto 0;"
                    + "background:#eff6ff;border-color:#bfdbfe;text-align:center;'>");
            sb.append("<div style='display:inline-block;background:#fef3c7;color:#92400e;border-radius:999px;"
                    + "padding:3px 10px;font-size:10px;font-weight:800;margin-bottom:8px;'>"
                    + "MENUNGGU PEMBAYARAN</div>");
            sb.append("<div style='font-size:11px;font-weight:700;color:#1d4ed8;letter-spacing:.5px;'>")
              .append(escHtml(vaLabelBank == null ? "VIRTUAL ACCOUNT" : vaLabelBank.toUpperCase()))
              .append("</div>");
            if (adaQr) {
                sb.append("<div style='margin:10px auto;'>"
                        + "<img src='").append(escHtml(vaQrUrl))
                  .append("' alt='QR Pembayaran' style='width:220px;max-width:80%;height:auto;"
                        + "border:1px solid #e2e8f0;border-radius:8px;background:#fff;padding:6px;'/></div>");
                sb.append("<div style='font-size:12px;color:#334155;'>Pindai kode QR di atas "
                        + "melalui aplikasi pembayaran Anda.</div>");
            }
            if (adaVa) {
                sb.append("<div style='font-family:Consolas,Menlo,monospace;font-size:22px;font-weight:800;"
                        + "color:#0f172a;margin:8px 0;letter-spacing:1px;word-break:break-all;"
                        + "user-select:all;-webkit-user-select:all;'>")
                  .append(escHtml(vaKode)).append("</div>");
            }
            if (vaTotal != null) {
                sb.append("<div style='font-size:13px;color:#334155;'>Total dibayar: <b>")
                  .append(escHtml(vaTotal)).append("</b></div>");
            }
            if (vaKadaluarsa != null) {
                sb.append("<div style='font-size:12px;color:#b45309;margin-top:4px;'>"
                        + "Bayar sebelum: <b>").append(escHtml(vaKadaluarsa)).append("</b></div>");
            }
            if (adaVa) {
                sb.append("<div style='font-size:11px;color:#64748b;margin-top:8px;'>"
                        + "Ketuk/blok nomor di atas untuk menyalin, lalu bayar melalui kanal bank Anda.</div>");
            }
            sb.append("</div>");
            bodyHost.appendChild(new Html(sb.toString()));
        }
    }

    // ============================================================ DATA LOADING

    /**
     * Memuat tagihan untuk SEMUA jenis pembayaran terpilih (jenis utama + jenis tambahan
     * mode Keranjang). Tiap item ditandai jenis pemiliknya agar dapat dikelompokkan
     * kembali menjadi draf {@link KegiatanTemporary} per jenis saat eksekusi keranjang.
     */
    @SuppressWarnings("unchecked")
    private void muatTagihan() {
        tagihanItems = new ArrayList<TagihanItem>();
        jadwalPerJenis = new HashMap<Long, JadwalPembayaran>();
        if (jenisKegiatan == null || semester == null) return;

        for (JenisKegiatan jk : semuaJenisTerpilih()) {
            jadwalPerJenis.put(jk.getId(), resolveJadwalUntuk(jk));
        }
        jadwalPembayaran = jadwalPerJenis.get(jenisKegiatan.getId());

        Session session = null;
        try {
            session = HibernateUtil.openSession();
            for (JenisKegiatan jk : semuaJenisTerpilih()) {
                java.util.Collection list = null;
                try {
                    list = PembayaranUtilHelper.getDetailBiayaMahasiswa(mahasiswa, semester, jk, true);

                    // MODE BULANAN/ANGSURAN (paritas DaftarUlang): bila mahasiswa punya baris
                    // billing bulanan aktif untuk item-item ini, tagihan yang benar adalah
                    // baris PengaturanPembayaranBulanan — muat ulang varian bulanan ("-1").
                    // Tanpa tarian ini, wizard tampak "Tidak ada tagihan" padahal tabel
                    // tagihan berisi baris SPP per bulan.
                    int countPengaturanBulanan = PembayaranUtilHelper.countBulanan(session, mahasiswa, jk,
                            semester, list, true, true);
                    if (countPengaturanBulanan > 0) {
                        // WAJIB overload 6-argumen dgn untukBulananTampilkanMeskipunSudahDibayar
                        // = TRUE (paritas DaftarUlang): overload 5-argumen mengirim FALSE diam-diam
                        // sehingga slot bulanan yang SUDAH ada pembayaran parsial ikut dibuang —
                        // gejala nyata: "Tidak ada tagihan ditemukan" padahal kekurangan masih ada.
                        list = PembayaranUtilHelper.getDetailBiayaMahasiswa(mahasiswa, semester, jk, "-1",
                                Boolean.TRUE, true);
                    }
                } catch (Exception e) {
                    alertar("Gagal memuat tagihan " + jk.getNama() + ": " + e.getMessage());
                    continue;
                }
                if (list == null || list.isEmpty()) continue;

                // Daftar cicilan PERSIS cara DaftarUlangMahasiswa*Action: ambil Kegiatan
                // mahasiswa utk jenis+semester ini lalu muat seluruh cicilannya SEKALI via
                // KegiatanPersistenceHelper.ambilCicilan — agregasi per item/slot dilakukan
                // di Java (bukan kueri manual per baris). Kegiatan sudah ber-scope
                // (mahasiswa, jenis, semester) sehingga cicilan mahasiswa lain yang memakai
                // template PengaturanPembayaranBulanan yang sama tidak pernah ikut terhitung.
                List<CicilanPembayaran> cicilans = new ArrayList<CicilanPembayaran>();
                Kegiatan kegiatanJk = null;
                java.util.Collection<ais.database.model.DetailKegiatan> detailKegiatans = null;
                try {
                    kegiatanJk = mahasiswa.ambilKegiatansRefresh(semester, jk);
                    if (kegiatanJk != null && kegiatanJk.getId() != null) {
                        cicilans = KegiatanPersistenceHelper.ambilCicilan(kegiatanJk, true);
                        detailKegiatans = kegiatanJk.ambilDetailKegiatan(true);
                    }
                } catch (Exception eCicilan) {
                    ais.common.ErrorAuditUtil.record(eCicilan,
                            "Wizard Pembayaran: gagal memuat daftar cicilan; mahasiswa="
                                    + mahasiswa.getNim() + ", jk=" + jk.getNama() + ", smt=" + semester);
                }

                // Agregasi "sudah dibayar" — KUNCI PERSIS SAMA dengan
                // DetailPembayaranMahasiswaRenderer.ubahWarnaStatus (peta "nilais"):
                // itemBiaya.id + "_" + bayarKe, BUKAN detailBiaya.id. DetailBiaya baris
                // item reguler bisa DIBUAT ULANG (id baru) setiap kali Setting Biaya
                // disegarkan, sementara itemBiaya+bayarKe tetap stabil sepanjang waktu —
                // menyaring by detailBiaya.id membuat cicilan LAMA (terikat ke id
                // DetailBiaya yang sudah diganti) tidak ikut terhitung, sehingga "Dibayar"
                // di wizard lebih kecil dari kenyataan (gejala nyata: SPP 8 bulan sudah
                // dibayar 2.400.000, wizard hanya menghitung 600.000 dari 2 cicilan
                // ber-detailBiaya baru). CicilanPembayaran.getBayarKe() sendiri sudah
                // menurunkan nilainya dari detailBiaya bila ada (default 1), identik
                // dengan cara DetailBiaya.getBayarKe() dipakai sebagai bagian kunci di sisi
                // renderer.
                Map<Long, Double> dibayarPerPpb = new HashMap<Long, Double>();
                Map<String, Double> dibayarPerItemBayarKe = new HashMap<String, Double>();
                // FALLBACK (paritas DetailPembayaranMahasiswaRenderer.ubahWarnaStatus, peta
                // "nilaisPerItemBiaya"): sebagian cicilan lama tercatat dengan bayarKe yang
                // tidak persis sama dengan db.getBayarKe() baris tagihan saat ini (mis. sisa
                // data sebelum bayarKe konsisten diisi). Kunci itemBiaya+bayarKe di atas jadi
                // tidak pernah cocok utk cicilan itu -> "Dibayar" tampil Rp 0 padahal item
                // sudah lunas di grid utama (yg SUDAH punya fallback ini). Jumlahkan juga per
                // itemBiaya SAJA (lintas bayarKe) sbg fallback.
                Map<Long, Double> dibayarPerItemBiaya = new HashMap<Long, Double>();
                // FALLBACK slot BULANAN: baris PengaturanPembayaranBulanan bisa DIBUAT ULANG
                // (id baru) saat billing disegarkan, sementara cicilan lama tetap menunjuk id
                // PPB LAMA -> kunci per-id PPB tidak pernah cocok dan "Dibayar" tampil Rp 0
                // padahal slot itu lunas di grid utama (gejala nyata: SPP Maret/April/Mei
                // Dibayar Rp 0 di wizard, 300.000/lunas di grid). Kunci item+bulan STABIL
                // walau id PPB berganti.
                Map<String, Double> dibayarPerItemBulan = new HashMap<String, Double>();
                for (CicilanPembayaran cp : cicilans) {
                    try {
                        if (cp == null || cp.getNilai() == null) continue;
                        if (cp.getPengaturanPembayaranBulanan() != null
                                && cp.getPengaturanPembayaranBulanan().getId() != null) {
                            Long idPpb = cp.getPengaturanPembayaranBulanan().getId();
                            Double lama = dibayarPerPpb.get(idPpb);
                            dibayarPerPpb.put(idPpb, (lama == null ? 0 : lama) + cp.getNilai());

                            try {
                                PengaturanPembayaranBulanan ppbCp = cp.getPengaturanPembayaranBulanan();
                                if (ppbCp.getRealBulan() != null && cp.getItemBiaya() != null
                                        && cp.getItemBiaya().getId() != null) {
                                    String kunciBulan = cp.getItemBiaya().getId() + "_bln_" + ppbCp.getRealBulan();
                                    Double lamaBulan = dibayarPerItemBulan.get(kunciBulan);
                                    dibayarPerItemBulan.put(kunciBulan,
                                            (lamaBulan == null ? 0 : lamaBulan) + cp.getNilai());
                                }
                            } catch (Exception eBulan) {
                                ais.common.ErrorAuditUtil.record(eBulan,
                                        "Wizard Pembayaran: gagal susun kunci item+bulan cicilan");
                            }
                        }
                        if (cp.getItemBiaya() != null && cp.getItemBiaya().getId() != null) {
                            String key = cp.getItemBiaya().getId() + "_" + cp.getBayarKe();
                            Double lama = dibayarPerItemBayarKe.get(key);
                            dibayarPerItemBayarKe.put(key, (lama == null ? 0 : lama) + cp.getNilai());

                            Long idItem = cp.getItemBiaya().getId();
                            Double lamaItem = dibayarPerItemBiaya.get(idItem);
                            dibayarPerItemBiaya.put(idItem, (lamaItem == null ? 0 : lamaItem) + cp.getNilai());
                        }
                    } catch (Exception eCp) {
                        ais.common.ErrorAuditUtil.record(eCp, "Wizard Pembayaran: gagal baca cicilan");
                    }
                }

                // Hitung jumlah baris tagihan per itemBiaya di `list` — fallback per-item hanya
                // aman dipakai bila item ini CUMA punya SATU baris tagihan yang tampil (kalau
                // lebih dari satu, tidak bisa dipastikan cicilan itu milik baris yang mana).
                Map<Long, Integer> jumlahBarisPerItem = new HashMap<Long, Integer>();
                for (Object o : list) {
                    DetailBiaya dbHitung = null;
                    if (o instanceof DetailBiaya) {
                        dbHitung = (DetailBiaya) o;
                    } else if (o instanceof PengaturanPembayaranBulanan) {
                        try { dbHitung = ((PengaturanPembayaranBulanan) o).getDetailBiaya(); } catch (Exception e) { dbHitung = null; }
                    }
                    if (dbHitung == null || dbHitung.getItemBiaya() == null || dbHitung.getItemBiaya().getId() == null) continue;
                    Long idItem = dbHitung.getItemBiaya().getId();
                    Integer total = jumlahBarisPerItem.get(idItem);
                    jumlahBarisPerItem.put(idItem, total == null ? 1 : total + 1);
                }

                for (Object o : list) {
                    DetailBiaya db = null;
                    PengaturanPembayaranBulanan ppb = null;
                    if (o instanceof DetailBiaya) {
                        db = (DetailBiaya) o;
                    } else if (o instanceof PengaturanPembayaranBulanan) {
                        ppb = (PengaturanPembayaranBulanan) o;
                        try { db = ppb.getDetailBiaya(); } catch (Exception e) { db = null; }
                    }
                    if (db == null || db.getId() == null) continue;

                    // NOMINAL TAGIHAN dihitung dengan method yang SAMA dengan renderer
                    // DaftarUlangMahasiswa*Action (Kegiatan.ambilJumlahTagihan) — bukan
                    // nilai per-unit db.getNilaiBiaya(). Untuk item ber-penghitungan
                    // PERKALIAN (mis. "UTS (50.000) x 9 matakuliah" = 450.000, SKS x N sks)
                    // nilai per-unit membuat wizard tampil 50.000 sementara DaftarUlang
                    // 450.000 — selisih yang dilaporkan user. Method ini juga menghormati
                    // DetailKegiatan (nilai diubah admin), diskon, parameterTambahan, dan
                    // nominal modifikasi slot bulanan.
                    double nominal;
                    if (ppb != null) {
                        Double j = null;
                        try {
                            j = Kegiatan.ambilJumlahTagihan(kegiatanJk, detailKegiatans, mahasiswa, semester, ppb);
                        } catch (Exception eJml) {
                            ais.common.ErrorAuditUtil.record(eJml,
                                    "Wizard Pembayaran: gagal hitung jumlah tagihan slot bulanan; ppb=" + ppb.getId());
                        }
                        nominal = j != null ? j : (ppb.getNominal() == null ? 0 : ppb.getNominal());
                        if (nominal <= 0 && !Boolean.TRUE.equals(ppb.getTetapDitampilkanWalaupunNol())) {
                            continue;
                        }
                    } else {
                        Double j = null;
                        try {
                            j = Kegiatan.ambilJumlahTagihan(kegiatanJk, db);
                        } catch (Exception eJml) {
                            ais.common.ErrorAuditUtil.record(eJml,
                                    "Wizard Pembayaran: gagal hitung jumlah tagihan item; detailBiaya=" + db.getId());
                        }
                        nominal = j != null ? j : (db.getNilaiBiaya() != null ? db.getNilaiBiaya() : 0);
                    }
                    // Skip item diskon (nominal negatif) — sudah tercermin dalam tagihan lain
                    if (nominal < 0) continue;

                    // Rincian penghitungan (mis. "(Rp 10.000) x 15 SKS") — hanya untuk item
                    // ber-penghitungan PERKALIAN; ambilJumlahTagihan() di atas sudah men-trigger
                    // DetailBiaya.updateKeterangan() sbg efek-samping sehingga getKeterangan()
                    // di sini sudah berisi teks rincian terbaru untuk mahasiswa+semester ini.
                    String ketPenghitungan = "";
                    try {
                        if (db.getItemBiaya() != null
                                && !db.getItemBiaya().getPenghitungan().equals(ItemBiaya.TIDAK_ADA_PENGHITUNGAN)
                                && db.getKeterangan() != null) {
                            ketPenghitungan = db.getKeterangan().trim();
                        }
                    } catch (Exception eKet) {
                        ais.common.ErrorAuditUtil.record(eKet,
                                "Wizard Pembayaran: gagal ambil keterangan penghitungan; detailBiaya=" + db.getId());
                    }

                    double sudah;
                    if (ppb != null && ppb.getId() != null) {
                        Double v = dibayarPerPpb.get(ppb.getId());
                        sudah = v == null ? 0 : v;
                        // Fallback item+bulan (lihat komentar pembangunan dibayarPerItemBulan):
                        // cicilan lama menunjuk id PPB LAMA yang sudah diganti -> cocokkan via
                        // itemBiaya + realBulan yang stabil, agar slot lunas tidak tampil Rp 0.
                        if (sudah <= 0) {
                            try {
                                if (ppb.getRealBulan() != null && db.getItemBiaya() != null
                                        && db.getItemBiaya().getId() != null) {
                                    Double vBulan = dibayarPerItemBulan
                                            .get(db.getItemBiaya().getId() + "_bln_" + ppb.getRealBulan());
                                    if (vBulan != null) {
                                        sudah = vBulan.doubleValue();
                                    }
                                }
                            } catch (Exception eFb) {
                                ais.common.ErrorAuditUtil.record(eFb,
                                        "Wizard Pembayaran: gagal fallback item+bulan; ppb=" + ppb.getId());
                            }
                        }
                    } else if (db.getItemBiaya() != null && db.getItemBiaya().getId() != null) {
                        Long idItem = db.getItemBiaya().getId();
                        String key = idItem + "_" + db.getBayarKe();
                        Double v = dibayarPerItemBayarKe.get(key);
                        sudah = v == null ? 0 : v;
                        // Fallback per-itemBiaya (lihat komentar pembangunan dibayarPerItemBiaya
                        // di atas): dipakai hanya bila item ini item satu-satunya baris tagihan
                        // di wizard (aman, tidak ambigu) DAN totalnya lebih besar dari hasil kunci
                        // spesifik itemBiaya+bayarKe.
                        Double vItem = dibayarPerItemBiaya.get(idItem);
                        Integer jumlahBaris = jumlahBarisPerItem.get(idItem);
                        if (vItem != null && (jumlahBaris == null || jumlahBaris.intValue() <= 1)
                                && vItem.doubleValue() > sudah) {
                            sudah = vItem.doubleValue();
                        }
                    } else {
                        sudah = 0;
                    }
                    // Baris bulanan = angsuran by definition → nominal boleh diubah.
                    // Item reguler MURNI mengikuti flag ItemBiaya (permintaan user):
                    // adminBolehMencicilkan utk admin/kasir, mahasiswaBolehMencicilkan utk
                    // pengguna mahasiswa — via PembayaranUtil.bolehDiangsur, tanpa
                    // pengecualian lain (sama dengan checkout JSP).
                    boolean bisaDiubah = ppb != null || PembayaranUtil.bolehDiangsur(db, isUserAdmin());
                    TagihanItem item = new TagihanItem(db, ppb, jk, nominal, sudah, bisaDiubah, ketPenghitungan);
                    // Pre-select item yang belum lunas
                    item.dipilih = item.kekurangan > 0;
                    tagihanItems.add(item);
                }
            }
        } catch (Exception e) {
            alertar("Gagal memproses tagihan: " + e.getMessage());
        } finally {
            HibernateUtil.closeSessionQuietly(session);
        }
    }

    // ============================================================ PAYMENT DISPATCH

    /**
     * Menyalurkan klik tombol saluran pembayaran ke eksekutor yang tepat.
     * Kategori {@code LANGSUNG} memakai {@code XxxCommon.onSaveXxx}; kategori
     * {@code BANK_ONLINE} memakai eksekutor generik {@link #bayarBankOnline}.
     */
    private void prosesPayment(PembayaranGatewayKatalog.Gateway g, List<TagihanItem> dipilih, Event evt)
            throws Exception {
        if (PembayaranGatewayKatalog.KATEGORI_BANK_ONLINE.equals(g.kategori)) {
            bayarBankOnline(g, dipilih);
        } else if ("ipaymu".equals(g.id)) {
            bayarIpaymu(dipilih, evt);
        } else if ("faspay".equals(g.id)) {
            bayarFaspay(dipilih, evt);
        } else if ("finpay".equals(g.id)) {
            bayarFinpay(dipilih, evt);
        } else if ("bni".equals(g.id)) {
            bayarBni(dipilih, evt);
        } else if ("bsi".equals(g.id)) {
            bayarBsi(dipilih, evt);
        } else if ("bri".equals(g.id)) {
            bayarBri(dipilih, evt);
        } else if ("doku".equals(g.id)) {
            bayarDoku(dipilih, evt);
        } else if ("cimb".equals(g.id)) {
            bayarCimb(dipilih, evt);
        } else if ("jatelindo".equals(g.id)) {
            bayarJatelindo(dipilih, evt);
        } else if ("btn".equals(g.id) || "ntt".equals(g.id) || "bjb".equals(g.id)) {
            bayarVaBankKhusus(g, dipilih);
        } else if ("bankaltimtara".equals(g.id)) {
            bayarBankaltimtara(g, dipilih);
        } else {
            alertar("Saluran pembayaran '" + g.label + "' belum didukung wizard ini.");
        }
    }

    /**
     * Eksekutor generik VA bank daerah/khusus ber-signature seragam — BTN, NTT dan BJB —
     * replikasi tombol "BAYAR VIA BANK xxx" pada {@code DaftarUlangMahasiswaLamaAction}:
     * {@code DownloadTagihanMahasiswaBankXxx.downloadData(mahasiswa, semester,
     * jadwalPembayaran, detailBiayas, gridCicilan)}; grid cicilan disintesis dari item
     * terpilih (termasuk alokasi bulanan), hasil VA ditampilkan pada langkah Selesai
     * (paritas popup {@code /common/<bank>/no_va.zul} dengan biaya admin 0 sebagaimana
     * DaftarUlang).
     */
    private void bayarVaBankKhusus(PembayaranGatewayKatalog.Gateway g, List<TagihanItem> dipilih) {
        try {
            List<DetailBiaya> biayas = kumpulkanBiayaTerpilih(dipilih);
            if (biayas == null) return;
            Grid gridMock = buatGridMockDari(dipilih);

            VirtualAccountBank va;
            if ("ntt".equals(g.id)) {
                va = DownloadTagihanMahasiswaBankNtt.downloadData(mahasiswa, semester,
                        jadwalPembayaran, biayas, gridMock);
            } else if ("bjb".equals(g.id)) {
                va = DownloadTagihanMahasiswaBankBjb.downloadData(mahasiswa, semester,
                        jadwalPembayaran, biayas, gridMock);
            } else {
                va = DownloadTagihanMahasiswaBankBtn.downloadData(mahasiswa, semester,
                        jadwalPembayaran, biayas, gridMock);
            }

            if (va != null && va.getLink() != null && !va.getLink().trim().isEmpty()) {
                String link = va.getLink().replace("'", "%27");
                Clients.evalJavaScript("window.open('" + link + "','_blank');");
                langkah = 5;
                render();
                return;
            }
            if (va != null) {
                simpanInfoVa(g, va, 0.0);
                langkah = 5;
                render();
            } else {
                alertar("Mohon maaf, penerbitan Virtual Account " + g.label + " tidak berhasil. Periksa"
                        + " koneksi/konfigurasi host bank, lalu ulangi beberapa saat lagi.");
            }
        } catch (Exception e) {
            ais.common.ErrorAuditUtil.record(e, "Wizard Pembayaran: gagal memproses VA " + g.label
                    + "; mahasiswa=" + (mahasiswa == null ? null : mahasiswa.getNim()) + ", smt=" + semester);
            alertar("Gagal memproses " + g.label + ": " + e.getMessage());
        }
    }

    /**
     * Eksekutor Bankaltimtara — replikasi tombol "BAYAR VIA Bankaltimtara" pada
     * {@code DaftarUlangMahasiswaLamaAction} (jendela radio "Pilihlah Bayar Via"):
     * pengguna memilih metode Virtual Account atau QRIS, lalu
     * {@code DownloadTagihanMahasiswaBankBankaltimtara.downloadData(mahasiswa, semester,
     * jadwalPembayaran, detailBiayas, gridCicilan, biayaAdmin, pakaiva)} dengan biaya
     * administrasi dari konfigurasi {@code bankaltimtara_biaya_administrasi}. Mode QRIS
     * mengembalikan barcode panjang sehingga {@link #simpanInfoVa} otomatis merender
     * gambar QR pada langkah Selesai.
     */
    private void bayarBankaltimtara(final PembayaranGatewayKatalog.Gateway g, final List<TagihanItem> dipilih) {
        try {
            MyMessageboxConfig.show(
                    "Pilih metode pembayaran Bankaltimtara:\n\n"
                            + "YA = Virtual Account\n"
                            + "TIDAK = QRIS\n"
                            + "BATAL = kembali tanpa membuat transaksi",
                    "Pilih Metode Pembayaran",
                    MyMessageboxConfig.YES.intValue() | MyMessageboxConfig.NO.intValue()
                            | MyMessageboxConfig.CANCEL.intValue(),
                    MyMessageboxConfig.QUESTION, new EventListener() {
                        @Override public void onEvent(Event e) throws Exception {
                            boolean pakaiva;
                            if ("onYes".equals(e.getName())) pakaiva = true;
                            else if ("onNo".equals(e.getName())) pakaiva = false;
                            else return; // batal
                            eksekusiBankaltimtara(g, dipilih, pakaiva);
                        }
                    });
        } catch (Exception e) {
            ais.common.ErrorAuditUtil.record(e, "Wizard Pembayaran: gagal membuka pilihan metode Bankaltimtara;"
                    + " mahasiswa=" + (mahasiswa == null ? null : mahasiswa.getNim()) + ", smt=" + semester);
            alertar("Gagal memproses " + g.label + ": " + e.getMessage());
        }
    }

    /** Eksekusi penerbitan VA/QRIS Bankaltimtara setelah metode dipilih pengguna. */
    private void eksekusiBankaltimtara(PembayaranGatewayKatalog.Gateway g, List<TagihanItem> dipilih,
            boolean pakaiva) {
        try {
            List<DetailBiaya> biayas = kumpulkanBiayaTerpilih(dipilih);
            if (biayas == null) return;
            Grid gridMock = buatGridMockDari(dipilih);

            double biayaAdm = 0.0;
            try {
                biayaAdm = Double.parseDouble(
                        Common.getKonfigurasi("bankaltimtara_biaya_administrasi", "0.0").getNilai());
            } catch (Exception e) {
                ais.common.ErrorAuditUtil.record(e,
                        "Wizard Pembayaran: konfigurasi bankaltimtara_biaya_administrasi tidak valid");
            }

            VirtualAccountBank va = DownloadTagihanMahasiswaBankBankaltimtara.downloadData(mahasiswa,
                    semester, jadwalPembayaran, biayas, gridMock, biayaAdm, pakaiva);

            if (va != null) {
                simpanInfoVa(g, va, biayaAdm);
                langkah = 5;
                render();
            } else {
                alertar("Mohon maaf, penerbitan " + (pakaiva ? "Virtual Account" : "QRIS")
                        + " Bankaltimtara tidak berhasil. Periksa koneksi/konfigurasi host bank,"
                        + " lalu ulangi beberapa saat lagi.");
            }
        } catch (Exception e) {
            ais.common.ErrorAuditUtil.record(e, "Wizard Pembayaran: gagal memproses Bankaltimtara (pakaiva="
                    + pakaiva + "); mahasiswa=" + (mahasiswa == null ? null : mahasiswa.getNim())
                    + ", smt=" + semester);
            alertar("Gagal memproses " + g.label + ": " + e.getMessage());
        }
    }

    /**
     * Mengumpulkan {@link DetailBiaya} dari item terpilih bernominal &gt; 0 sambil
     * menyalin nominal bayar ke {@code nilaiBiayaBaru} (kontrak downloadData bank).
     * Mengembalikan {@code null} (setelah menampilkan peringatan) bila tidak ada item valid.
     */
    private List<DetailBiaya> kumpulkanBiayaTerpilih(List<TagihanItem> dipilih) {
        List<DetailBiaya> biayas = new ArrayList<DetailBiaya>();
        for (TagihanItem item : dipilih) {
            if (item.nominalBayar <= 0 || item.detailBiaya == null) continue;
            item.detailBiaya.setNilaiBiayaBaru(item.nominalBayar);
            biayas.add(item.detailBiaya);
        }
        if (biayas.isEmpty()) {
            alertar("Tidak ada item dengan nominal pembayaran. Silakan periksa kembali langkah Atur Nominal.");
            return null;
        }
        return biayas;
    }

    // ============================================================ BANK ONLINE FAMILY

    /**
     * Eksekutor generik seluruh saluran keluarga "Bank Online" (Online, Online 2,
     * Smartlink, Maja, QRIS, Finpay-Bank, Flip, Otto, BRIVA) — replikasi
     * {@code DaftarUlangMahasiswaLamaAction.setupBankOnlineGateway}. Konfigurasi host bank
     * dan biaya administrasi dibaca dari metadata {@link PembayaranGatewayKatalog.Gateway};
     * grid cicilan yang dituntut {@code DownloadTagihanMahasiswaBankOnline.downloadData}
     * disintesis in-memory sehingga tidak perlu Grid ZK DaftarUlang. Hasil bertautan
     * (Flip/Finpay/Otto) dibuka pada tab/popup browser; hasil bernomor VA ditampilkan
     * pada langkah Selesai.
     */
    private void bayarBankOnline(PembayaranGatewayKatalog.Gateway g, List<TagihanItem> dipilih) {
        try {
            double biayaAdministrasi = 0.0;
            try {
                biayaAdministrasi = Double.parseDouble(
                        Common.getKonfigurasi(g.adminFeeConfig, "0.0").getNilai());
            } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/WizardPembayaranMhsHelper.java:1585"); /* biaya admin opsional */ }

            BankHost bankHost = PembayaranUtil.getInstance()
                    .getBankHost(Common.getKonfigurasi(g.bankHostConfig, "").getNilai(), "Bank Host");

            List<DetailBiaya> biayas = new ArrayList<DetailBiaya>();
            for (TagihanItem item : dipilih) {
                if (item.nominalBayar <= 0 || item.detailBiaya == null) continue;
                item.detailBiaya.setNilaiBiayaBaru(item.nominalBayar);
                biayas.add(item.detailBiaya);
            }
            if (biayas.isEmpty()) {
                alertar("Tidak ada item dengan nominal pembayaran. Silakan periksa kembali langkah Atur Nominal.");
                return;
            }
            // Mock membawa slot bulanan per item → token Bulanan-/Item- pada VA benar.
            Grid gridMock = buatGridMockDari(dipilih);

            Map param = new HashMap();
            param.put("tahunAkademik", getTahunAkademik());
            // flag kanal — id katalog "bank_finpay" memakai flag legacy "finpay"
            String flag = "bank_finpay".equals(g.id) ? "finpay" : g.id;
            if ("smartlink".equals(flag) || "maja".equals(flag) || "qris".equals(flag)
                    || "finpay".equals(flag) || "flip".equals(flag) || "otto".equals(flag)
                    || "briva".equals(flag)) {
                param.put(flag, Boolean.TRUE);
            }

            VirtualAccountBank va = DownloadTagihanMahasiswaBankOnline.downloadData(mahasiswa, semester,
                    jadwalPembayaran, biayas, gridMock, param, biayaAdministrasi, null, null, bankHost);

            if (param.get("jangan_notif") != null && Boolean.TRUE.equals(param.get("jangan_notif"))) {
                // Proses ditangguhkan pihak lain (mis. jendela pilih channel Smartlink) —
                // jangan tampilkan layar sukses ataupun galat (paritas DaftarUlang).
                return;
            }

            if (va != null && va.getLink() != null && !va.getLink().trim().isEmpty()) {
                // Saluran bertautan: finpay/otto di-redirect tab baru (paritas DaftarUlang),
                // lainnya dibuka sebagai popup.
                String link = va.getLink().replace("'", "%27");
                if ("bank_finpay".equals(g.id) || "otto".equals(g.id)) {
                    try {
                        ExecutionsCtrl.getCurrent().sendRedirect(link, "_blank");
                    } catch (Exception ex) {
                        Clients.evalJavaScript("window.open('" + link + "','_blank');");
                    }
                } else {
                    Clients.evalJavaScript("window.open('" + link + "','_blank');");
                }
                langkah = 5;
                render();
                return;
            }

            if (va != null) {
                simpanInfoVa(g, va, biayaAdministrasi);
                langkah = 5;
                render();
            } else {
                alertar("Mohon maaf, transaksi tidak berhasil dilakukan. Langkah yang dapat dilakukan: "
                        + "(1) periksa kembali koneksi jaringan; (2) ulangi beberapa saat lagi; "
                        + "(3) bila berlanjut hubungi Administrator sistem.");
            }
        } catch (Exception e) {
            alertar("Gagal memproses saluran " + g.label + ": " + e.getMessage());
        }
    }

    /**
     * Menyalin data {@link VirtualAccountBank} ke field tampilan langkah Selesai,
     * termasuk perakitan prefix kode bank lain (pola yang sama dengan
     * {@code _lanjut_bayar_services.jsp}: prefix + username kanal + kode VA).
     */
    private void simpanInfoVa(PembayaranGatewayKatalog.Gateway g, VirtualAccountBank va, double biayaAdmin) {
        vaLabelBank = g.label;
        try {
            String kode = va.getKode() == null ? "" : va.getKode();
            if (g.prefixKodeLainConfig != null) {
                String prefix = Common.getKonfigurasi(g.prefixKodeLainConfig, "").getNilai();
                try {
                    if (va.getKanalPembayaran() != null && va.getKanalPembayaran().getBsiUsername() != null
                            && !va.getKanalPembayaran().getBsiUsername().isEmpty()) {
                        kode = va.getKanalPembayaran().getBsiUsername() + kode;
                    }
                } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/WizardPembayaranMhsHelper.java:1670"); /* kanal opsional */ }
                if (prefix != null && !prefix.trim().isEmpty()) kode = prefix + kode;
            }
            vaKode = kode;
        } catch (Exception e) {
            vaKode = null;
        }
        try {
            double totalVa = (va.getTotal() == null ? 0 : va.getTotal())
                    + (va.getBiayaAdmin() != null ? va.getBiayaAdmin() : biayaAdmin);
            vaTotal = formatRp(totalVa);
        } catch (Exception e) {
            vaTotal = null;
        }
        try {
            vaKadaluarsa = va.getKadaluarsaWaktu() == null ? null
                    : Common.dateFormat.get().format(va.getKadaluarsaWaktu());
        } catch (Exception e) {
            vaKadaluarsa = null;
        }
        // QRIS / kanal ber-barcode: payload mentah bukan nomor VA yang bisa disalin —
        // bangkitkan gambar QR (pola /report/crcode_<id>.png yang sama dengan
        // _lanjut_bayar_services.jsp dan no_va.zul) dan sembunyikan teks mentahnya.
        vaQrUrl = null;
        try {
            String rawBarcode = va.getBarcode();
            boolean adaQr = "qris".equals(g.id) || (rawBarcode != null && rawBarcode.length() > 20);
            if (adaQr && va.getId() != null) {
                String isiQr = (rawBarcode != null && !rawBarcode.trim().isEmpty()) ? rawBarcode
                        : va.getKode();
                if (isiQr != null && !isiQr.trim().isEmpty()) {
                    java.io.File fileQr = new java.io.File(
                            Common.ambilREAL_PATH_REPORT() + "/crcode_" + va.getId() + ".png");
                    if (!fileQr.exists()) {
                        BarcodeCommon.generateCRCode(isiQr, fileQr, 600, 600);
                    }
                    vaQrUrl = "/report/crcode_" + va.getId() + ".png";
                    if ("qris".equals(g.id)) {
                        vaKode = null; // payload QRIS mentah tidak berguna ditampilkan sebagai teks
                    }
                }
            }
        } catch (Exception e) {
            vaQrUrl = null;
        }
    }

    // ============================================================ DOKU
    @SuppressWarnings("unchecked")
    private void bayarDoku(List<TagihanItem> dipilih, Event evt) throws Exception {
        double total = hitungTotalBayar();
        List<DokuRequestDetailBiaya> detailBiayas = new ArrayList<DokuRequestDetailBiaya>();
        for (TagihanItem item : dipilih) {
            DokuRequestDetailBiaya db = new DokuRequestDetailBiaya();
            db.setDetailBiaya(item.detailBiaya);
            db.setNilai(item.nominalBayar);
            detailBiayas.add(db);
        }
        List<DokuRequestDetail> details = DokuCommon.populateDokuRequestDetail(buatGridMockDari(dipilih), mahasiswa, semester, jadwalPembayaran);
        DokuCommon.onSaveDoku(total, mahasiswa, null, jenisKegiatan, jadwalPembayaran, semester,
                getTahunAkademik(), "Wizard Pembayaran", 0.0, total, details, detailBiayas, evt);
        langkah = 5;
        render();
    }

    // ============================================================ CIMB
    @SuppressWarnings("unchecked")
    private void bayarCimb(List<TagihanItem> dipilih, Event evt) throws Exception {
        double total = hitungTotalBayar();
        List<CimbRequestDetailBiaya> detailBiayas = new ArrayList<CimbRequestDetailBiaya>();
        for (TagihanItem item : dipilih) {
            CimbRequestDetailBiaya db = new CimbRequestDetailBiaya();
            db.setDetailBiaya(item.detailBiaya);
            db.setNilai(item.nominalBayar);
            detailBiayas.add(db);
        }
        List<CimbRequestDetail> details = CimbCommon.populateCimbRequestDetail(buatGridMockDari(dipilih), mahasiswa, semester, jadwalPembayaran);
        CimbCommon.onSaveCimb(total, mahasiswa, null, jenisKegiatan, jadwalPembayaran, semester,
                getTahunAkademik(), "Wizard Pembayaran", 0.0, total, details, detailBiayas, evt);
        langkah = 5;
        render();
    }

    // ============================================================ JATELINDO
    @SuppressWarnings("unchecked")
    private void bayarJatelindo(List<TagihanItem> dipilih, Event evt) throws Exception {
        double total = hitungTotalBayar();
        List<JatelindoRequestDetailBiaya> detailBiayas = new ArrayList<JatelindoRequestDetailBiaya>();
        for (TagihanItem item : dipilih) {
            JatelindoRequestDetailBiaya db = new JatelindoRequestDetailBiaya();
            db.setDetailBiaya(item.detailBiaya);
            db.setNilai(item.nominalBayar);
            detailBiayas.add(db);
        }
        List<JatelindoRequestDetail> details = JatelindoCommon
                .populateJatelindoRequestDetail(buatGridMockDari(dipilih), mahasiswa, semester, jadwalPembayaran);
        JatelindoCommon.onSaveJatelindo(total, mahasiswa, null, jenisKegiatan, jadwalPembayaran, semester,
                getTahunAkademik(), "Wizard Pembayaran", 0.0, total, details, detailBiayas, evt);
        langkah = 5;
        render();
    }

    // ============================================================ TUNAI
    /**
     * Menyimpan pembayaran tunai/manual: satu {@link Kegiatan} tervalidasi + satu
     * {@link CicilanPembayaran} per item terpilih, dalam satu transaksi pada session
     * tersendiri. Paritas data dengan DaftarUlang: jenis pembayaran {@code TUNAI},
     * jadwal pembayaran terkait, sisa tunggakan ({@code amountTerhutang}) dihitung dari
     * kekurangan item, dan {@code PembayaranUtil.updateTunggakan} dipanggil agar rekap
     * tunggakan mahasiswa langsung sinkron.
     */
    private void bayarTunai(List<TagihanItem> dipilih) {
        boolean sukses = false;
        Session session = null;
        try {
            session = HibernateUtil.openSession();
            session.beginTransaction();

            String thnAkademik = getTahunAkademik();
            String validator = getCurrentUserNama();
            double total = hitungTotalBayar();
            double totalKekurangan = 0;
            for (TagihanItem item : dipilih) {
                totalKekurangan += item.kekurangan;
            }

            // PENTING (paritas DaftarUlang): satu kombinasi (mahasiswa, jenisKegiatan,
            // semester) hanya punya SATU Kegiatan — kodeunik "MHS_<id>-<jk>-<smt>" unik di
            // DB. Pembayaran berikutnya harus MEMAKAI ULANG Kegiatan yang ada (menambah
            // cicilan), bukan membuat baru (memicu duplicate key kegiatan_kodeunik_key).
            Kegiatan kegiatan = null;
            try {
                kegiatan = mahasiswa.ambilKegiatansRefresh(semester, jenisKegiatan, true);
            } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/WizardPembayaranMhsHelper.java:1803"); /* fallback: buat baru */ }

            boolean kegiatanBaru = kegiatan == null || kegiatan.getId() == null;
            if (kegiatanBaru) {
                kegiatan = new Kegiatan();
                kegiatan.setJenisKegiatan(jenisKegiatan);
                kegiatan.setMahasiswa(mahasiswa);
                kegiatan.setSemster(semester);
                kegiatan.setTahunAkademik(thnAkademik);
                kegiatan.setStatusMahasiswa(ConstantValues.AKTIF);
                kegiatan.setKeterangan("Wizard Pembayaran - Tunai");
                kegiatan.setAmount(total);
            } else {
                // akumulasi: amount = total telah dibayar kumulatif
                double amountLama = kegiatan.getAmount() == null ? 0 : kegiatan.getAmount();
                kegiatan.setAmount(amountLama + total);
            }
            kegiatan.setTanggal(WaktuUtil.getDate());
            kegiatan.setValidated(1);
            kegiatan.setValidator(validator);
            // kekurangan item sudah memperhitungkan pembayaran sebelumnya, jadi sisa
            // terhutang cukup: kekurangan terpilih - yang dibayar sekarang
            kegiatan.setAmountTerhutang(Math.max(0, totalKekurangan - total));
            if (jadwalPembayaran != null) kegiatan.setJadwalPembayaran(jadwalPembayaran);
            Common.refreshSaveOrUpdate(session, kegiatan);

            int ke = 1;
            for (TagihanItem item : dipilih) {
                if (item.nominalBayar <= 0) continue;
                CicilanPembayaran cicilan = new CicilanPembayaran(item.detailBiaya);
                cicilan.setKegiatan(kegiatan);
                if (item.detailBiaya != null) cicilan.setItemBiaya(item.detailBiaya.getItemBiaya());
                // Alokasi slot bulanan (mode angsuran) agar pembayaran tercatat ke bulan yang benar
                if (item.bulanan != null) cicilan.setPengaturanPembayaranBulanan(item.bulanan);
                cicilan.setNilai(item.nominalBayar);
                cicilan.setNilaiAsli(item.nominalBayar);
                cicilan.setJenisPembayaran(ConstantValues.TUNAI);
                cicilan.setTanggal(item.tanggalBayar != null ? item.tanggalBayar : WaktuUtil.getDate());
                cicilan.setKe(ke++);
                cicilan.setKeterangan("Wizard - Tunai");
                cicilan.setValidator(validator);
                session.save(cicilan);
            }

            try {
                PembayaranUtil.getInstance().updateTunggakan(kegiatan, session);
            } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/WizardPembayaranMhsHelper.java:1849");
                // rekap tunggakan bersifat pelengkap — kegagalan tidak membatalkan pembayaran
            }

            session.getTransaction().commit();
            sukses = true;
        } catch (Exception ex) {
            if (session != null) {
                try { session.getTransaction().rollback(); } catch (Exception ex2) { ais.common.ErrorAuditUtil.record(ex2, "auto-audit(empty-catch) src/ais/action/master/helper/WizardPembayaranMhsHelper.java:1857"); /* ignore */ }
            }
            alertar("Gagal menyimpan pembayaran tunai: " + ex.getMessage());
        } finally {
            HibernateUtil.closeSessionQuietly(session);
        }
        if (sukses) {
            langkah = 5;
            render();
        }
    }

    // ============================================================ IPAYMU
    @SuppressWarnings("unchecked")
    private void bayarIpaymu(List<TagihanItem> dipilih, Event evt) throws Exception {
        double total = hitungTotalBayar();
        List<IpaymuRequestDetailBiaya> detailBiayas = buildIpaymuDetailBiaya(dipilih);
        List<IpaymuRequestDetail> details = IpaymuCommon.populateIpaymuRequestDetail(buatGridMockDari(dipilih), mahasiswa, semester, jadwalPembayaran);
        IpaymuCommon.onSaveIpaymu(total, mahasiswa, null, jenisKegiatan, jadwalPembayaran, semester,
                getTahunAkademik(), "Wizard Pembayaran", 0.0, total, details, detailBiayas, evt);
        langkah = 5;
        render();
    }

    private List<IpaymuRequestDetailBiaya> buildIpaymuDetailBiaya(List<TagihanItem> dipilih) {
        List<IpaymuRequestDetailBiaya> list = new ArrayList<IpaymuRequestDetailBiaya>();
        for (TagihanItem item : dipilih) {
            IpaymuRequestDetailBiaya db = new IpaymuRequestDetailBiaya();
            db.setDetailBiaya(item.detailBiaya);
            db.setNilai(item.nominalBayar);
            list.add(db);
        }
        return list;
    }

    // ============================================================ FASPAY
    @SuppressWarnings("unchecked")
    private void bayarFaspay(List<TagihanItem> dipilih, Event evt) throws Exception {
        double total = hitungTotalBayar();
        List<FaspayRequestDetailBiaya> detailBiayas = buildFaspayDetailBiaya(dipilih);
        List<FaspayRequestDetail> details = FaspayCommon.populateFaspayRequestDetail(buatGridMockDari(dipilih), mahasiswa, semester, jadwalPembayaran);
        FaspayCommon.onSaveFaspay(total, mahasiswa, null, jenisKegiatan, jadwalPembayaran, semester,
                getTahunAkademik(), "Wizard Pembayaran", 0.0, total, details, detailBiayas, evt);
        langkah = 5;
        render();
    }

    private List<FaspayRequestDetailBiaya> buildFaspayDetailBiaya(List<TagihanItem> dipilih) {
        List<FaspayRequestDetailBiaya> list = new ArrayList<FaspayRequestDetailBiaya>();
        for (TagihanItem item : dipilih) {
            FaspayRequestDetailBiaya db = new FaspayRequestDetailBiaya();
            db.setDetailBiaya(item.detailBiaya);
            db.setNilai(item.nominalBayar);
            list.add(db);
        }
        return list;
    }

    // ============================================================ FINPAY
    @SuppressWarnings("unchecked")
    private void bayarFinpay(List<TagihanItem> dipilih, Event evt) throws Exception {
        double total = hitungTotalBayar();
        List<FinpayRequestDetailBiaya> detailBiayas = buildFinpayDetailBiaya(dipilih);
        List<FinpayRequestDetail> details = FinpayCommon.populateFinpayRequestDetail(buatGridMockDari(dipilih), mahasiswa, semester, jadwalPembayaran);
        FinpayCommon.onSaveFinpay(total, mahasiswa, null, jenisKegiatan, jadwalPembayaran, semester,
                getTahunAkademik(), "Wizard Pembayaran", 0.0, total, details, detailBiayas, evt);
        langkah = 5;
        render();
    }

    private List<FinpayRequestDetailBiaya> buildFinpayDetailBiaya(List<TagihanItem> dipilih) {
        List<FinpayRequestDetailBiaya> list = new ArrayList<FinpayRequestDetailBiaya>();
        for (TagihanItem item : dipilih) {
            FinpayRequestDetailBiaya db = new FinpayRequestDetailBiaya();
            db.setDetailBiaya(item.detailBiaya);
            db.setNilai(item.nominalBayar);
            list.add(db);
        }
        return list;
    }

    /**
     * Token alokasi cicilan per item ({@code "Item-<idItem>-<nilai>-<bayarKe>-<idDetail>,..."})
     * yang dituntut parameter {@code cicilan} pada onSaveBni/onSaveBsi — dibangun dengan
     * {@code VirtualAccountBank.populateCicilan} atas grid cicilan tiruan, persis seperti
     * DaftarUlang. Token ini dipakai callback bank untuk memecah pembayaran per item;
     * nilai yang salah membuat alokasi {@code CicilanPembayaran} rusak.
     */
    private String buildTokenCicilan(List<TagihanItem> dipilih) {
        try {
            return VirtualAccountBank.populateCicilan(buatGridMockDari(dipilih));
        } catch (Exception e) {
            return "";
        }
    }

    // ============================================================ BNI
    @SuppressWarnings("unchecked")
    private void bayarBni(List<TagihanItem> dipilih, Event evt) throws Exception {
        double total = hitungTotalBayar();
        List<BniRequestDetailBiaya> detailBiayas = buildBniDetailBiaya(dipilih);
        List<BniRequestDetail> details = BniCommon.populateBniRequestDetail(buatGridMockDari(dipilih), mahasiswa, semester, jadwalPembayaran);
        BniCommon.onSaveBni(total, mahasiswa, null, jenisKegiatan, jadwalPembayaran, semester,
                getTahunAkademik(), "Wizard Pembayaran", 0.0, total, details, detailBiayas, true, evt,
                buildTokenCicilan(dipilih));
        langkah = 5;
        render();
    }

    private List<BniRequestDetailBiaya> buildBniDetailBiaya(List<TagihanItem> dipilih) {
        List<BniRequestDetailBiaya> list = new ArrayList<BniRequestDetailBiaya>();
        for (TagihanItem item : dipilih) {
            BniRequestDetailBiaya db = new BniRequestDetailBiaya();
            db.setDetailBiaya(item.detailBiaya);
            db.setNilai(item.nominalBayar);
            list.add(db);
        }
        return list;
    }

    // ============================================================ BSI
    @SuppressWarnings("unchecked")
    private void bayarBsi(List<TagihanItem> dipilih, Event evt) throws Exception {
        double total = hitungTotalBayar();
        List<BsiRequestDetailBiaya> detailBiayas = buildBsiDetailBiaya(dipilih);
        List<BsiRequestDetail> details = BsiCommon.populateBsiRequestDetail(buatGridMockDari(dipilih), mahasiswa, semester, jadwalPembayaran);
        BsiCommon.onSaveBsi(total, mahasiswa, null, jenisKegiatan, jadwalPembayaran, semester,
                getTahunAkademik(), "Wizard Pembayaran", 0.0, total, details, detailBiayas, true, evt,
                buildTokenCicilan(dipilih));
        langkah = 5;
        render();
    }

    private List<BsiRequestDetailBiaya> buildBsiDetailBiaya(List<TagihanItem> dipilih) {
        List<BsiRequestDetailBiaya> list = new ArrayList<BsiRequestDetailBiaya>();
        for (TagihanItem item : dipilih) {
            BsiRequestDetailBiaya db = new BsiRequestDetailBiaya();
            db.setDetailBiaya(item.detailBiaya);
            db.setNilai(item.nominalBayar);
            list.add(db);
        }
        return list;
    }

    // ============================================================ BRI
    @SuppressWarnings("unchecked")
    private void bayarBri(List<TagihanItem> dipilih, Event evt) throws Exception {
        double total = hitungTotalBayar();
        List<BriRequestDetailBiaya> detailBiayas = buildBriDetailBiaya(dipilih);
        List<BriRequestDetail> details = BriCommon.populateBriRequestDetail(buatGridMockDari(dipilih), mahasiswa, semester, jadwalPembayaran);
        BriCommon.onSaveBri(total, mahasiswa, null, jenisKegiatan, jadwalPembayaran, semester,
                getTahunAkademik(), "Wizard Pembayaran", 0.0, total, details, detailBiayas, true, evt);
        langkah = 5;
        render();
    }

    private List<BriRequestDetailBiaya> buildBriDetailBiaya(List<TagihanItem> dipilih) {
        List<BriRequestDetailBiaya> list = new ArrayList<BriRequestDetailBiaya>();
        for (TagihanItem item : dipilih) {
            BriRequestDetailBiaya db = new BriRequestDetailBiaya();
            db.setDetailBiaya(item.detailBiaya);
            db.setNilai(item.nominalBayar);
            list.add(db);
        }
        return list;
    }

    // ============================================================ UTILS

    /**
     * Label tampilan sebuah item tagihan: nama item biaya, ditambah keterangan/nama bulan
     * untuk baris bulanan (mis. "SPP - September") — pola label yang sama dengan
     * checkout JSP dan DaftarUlang.
     */
    private String namaItem(TagihanItem item) {
        String nama = "-";
        try { nama = item.detailBiaya.getItemBiaya().getNama(); } catch (Exception e) { nama = "-"; }
        try {
            if (item.bulanan != null) {
                String ket = item.bulanan.getKeterangan();
                if (ket != null && !ket.trim().isEmpty()) {
                    nama += " - " + ket;
                } else if (item.bulanan.getNamaBulan() != null) {
                    nama += " - " + item.bulanan.getNamaBulan();
                }
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/WizardPembayaranMhsHelper.java:2043"); /* label pelengkap */ }
        return nama;
    }

    /**
     * Grid cicilan tiruan dari item terpilih — meneruskan slot BULANAN dan tanggal bayar
     * per item, sehingga token VA ({@code Bulanan-}/{@code Item-}) maupun
     * {@code populate*RequestDetail(Grid, ...)} gateway langsung mendapat alokasi yang
     * benar untuk tagihan bermode angsuran.
     */
    private Grid buatGridMockDari(List<TagihanItem> dipilih) {
        List<DetailBiaya> biayas = new ArrayList<DetailBiaya>();
        List<Double> noms = new ArrayList<Double>();
        List<PengaturanPembayaranBulanan> bulanans = new ArrayList<PengaturanPembayaranBulanan>();
        List<Date> tanggals = new ArrayList<Date>();
        for (TagihanItem item : dipilih) {
            if (item.nominalBayar <= 0 || item.detailBiaya == null) continue;
            biayas.add(item.detailBiaya);
            noms.add(item.nominalBayar);
            bulanans.add(item.bulanan);
            tanggals.add(item.tanggalBayar);
        }
        return PembayaranGatewayKatalog.buatGridCicilanMock(biayas, noms, bulanans, tanggals);
    }

    private double hitungTotalBayar() {
        double total = 0;
        for (TagihanItem item : tagihanItems) {
            if (item.dipilih) total += item.nominalBayar;
        }
        return total;
    }

    private List<TagihanItem> getItemsDipilih() {
        List<TagihanItem> list = new ArrayList<TagihanItem>();
        for (TagihanItem item : tagihanItems) {
            if (item.dipilih) list.add(item);
        }
        return list;
    }

    /**
     * Tahun akademik untuk (semester, angkatan) mahasiswa dalam format {@code "2025/2026"} —
     * resep yang sama dengan {@code _lanjut_bayar_services.jsp} dan DaftarUlang
     * ({@code Common.getTahunAkademik(smt, tahunAngkatan, semesterMulaiMasuk, semesterMulaiNama)});
     * fallback ke tahun akademik berjalan bila data mahasiswa tidak lengkap.
     */
    private String getTahunAkademik() {
        try {
            Integer angkatan = mahasiswa.getTahunangkatan() == null ? 0 : mahasiswa.getTahunangkatan();
            Integer smtMasuk = mahasiswa.getPindahKeKampusIniMasukSemester() == null ? 0
                    : mahasiswa.getPindahKeKampusIniMasukSemester();
            Integer taMulai = Common.getTahunAkademik(semester == null ? 1 : semester, angkatan, smtMasuk,
                    mahasiswa.getSemesterMulai() == null ? "" : mahasiswa.getSemesterMulai());
            if (taMulai != null && taMulai > 0) return taMulai + "/" + (taMulai + 1);
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/WizardPembayaranMhsHelper.java:2098"); /* fallback di bawah */ }
        try {
            String ta = Common.getCurrentTahunAkademik();
            if (ta != null && !ta.trim().isEmpty()) return ta.trim();
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/WizardPembayaranMhsHelper.java:2102"); /* ignore */ }
        return "";
    }

    /**
     * Mencari {@link JadwalPembayaran} aktif untuk (jenisKegiatan, semester) — replikasi
     * langkah "PENGAMBILAN JADWAL" pada {@code _lanjut_bayar_services.jsp}: kueri via
     * {@code PembayaranUtil.getJadwalPembayaranDanDendaBerdasarkanTahunAkademik}, dengan
     * fallback jadwal yang menempel pada Kegiatan aktif. Hasil {@code null} berarti di luar
     * masa pembayaran / belum ada jadwal.
     */
    private JadwalPembayaran resolveJadwalUntuk(JenisKegiatan jk) {
        JadwalPembayaran hasil = null;
        if (jk == null || semester == null) return null;
        try {
            ais.database.model.GelombangPendaftaran gelombang = null;
            try {
                java.lang.reflect.Method mtd = mahasiswa.getClass().getMethod("getGelombangPendaftaran");
                gelombang = (ais.database.model.GelombangPendaftaran) mtd.invoke(mahasiswa);
            } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/WizardPembayaranMhsHelper.java:2121"); /* Mahasiswa tanpa relasi gelombang */ }

            java.io.Serializable[] s = PembayaranUtil.getInstance()
                    .getJadwalPembayaranDanDendaBerdasarkanTahunAkademik(WaktuUtil.getDate(), jk,
                            mahasiswa.getJurusan() == null ? null : mahasiswa.getJurusan().getJenjang(),
                            getTahunAkademik(), (semester % 2 != 0), mahasiswa.getJenisSeleksi(),
                            mahasiswa.getProgram(), mahasiswa.getNim(), gelombang);
            if (s != null && s.length > 0) hasil = (JadwalPembayaran) s[0];
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/WizardPembayaranMhsHelper.java:2129"); /* toleran */ }
        if (hasil == null) {
            try {
                Kegiatan kegiatanAktif = mahasiswa.ambilKegiatansRefresh(semester, jk, true);
                if (kegiatanAktif != null) hasil = kegiatanAktif.getJadwalPembayaran();
            } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/WizardPembayaranMhsHelper.java:2134"); /* toleran */ }
        }
        return hasil;
    }

    private String getCurrentUserNama() {
        try {
            Tbmuser u = Common.getTbmuser();
            if (u != null) return u.getUserNama();
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/WizardPembayaranMhsHelper.java:2143"); /* ignore */ }
        return "System";
    }

    private static void alertar(String msg) {
        try {
            MyMessageboxConfig.show(msg, "Info", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/WizardPembayaranMhsHelper.java:2150"); /* ignore */ }
    }

    private static String formatRp(double v) {
        return "Rp " + String.format("%,.0f", v);
    }

    private static String escHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
