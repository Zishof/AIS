package ais.action.master.sekolah.helper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Decimalbox;
import org.zkoss.zul.Div;
import org.zkoss.zul.Doublebox;
import org.zkoss.zul.Html;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;

import ais.action.master.helper.virtualaccount.DownloadTagihanMahasiswaBankBtn;
import ais.action.master.helper.virtualaccount.DownloadTagihanSiswaBankOnline;
import ais.action.master.sekolah.util.PembayaranSiswaUtil;
import ais.action.master.sekolah.util.SekolahUtil;
import ais.action.ws.util.PembayaranUtil;
import ais.common.BarcodeCommon;
import ais.common.BniCommon;
import ais.common.BriCommon;
import ais.common.BsiCommon;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.common.TunaiSiswaCommon;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BankHost;
import ais.database.model.Konfigurasi;
import ais.database.model.Tbmuser;
import ais.database.model.VirtualAccountBank;
import ais.database.model.sekolah.AkunPembayaranSiswa;
import ais.database.model.sekolah.CalonSiswa;
import ais.database.model.sekolah.PembayaranSiswa;
import ais.database.model.sekolah.PengaturanBiaya;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Siswa;
import ais.database.model.sekolah.Tagihan;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.WaktuUtil;

/**
 * <h2>Wizard Pembayaran Siswa / Calon Siswa — antarmuka pembayaran lima langkah yang mandiri.</h2>
 *
 * <p>
 * Class ini adalah padanan sekolah dari
 * {@link ais.action.master.helper.WizardPembayaranMhsHelper} (mahasiswa) dan merupakan
 * rekonstruksi penuh dari alur pembayaran siswa yang sebelumnya hanya tersedia melalui
 * halaman engine {@code /pages/master/sekolah/pem_online.zul}
 * ({@link ais.action.master.sekolah.helper.PembayaranOnline}). Berbeda dengan pendekatan
 * lama yang menyematkan seluruh halaman engine ke dalam popup (rentan kolaps pada tampilan
 * mobile karena kalkulasi tinggi borderlayout ZK), wizard ini merakit antarmukanya sendiri
 * dari komponen {@code Div}/{@code Html} ringan sehingga mengalir natural pada satu area
 * gulir — nyaman di ponsel maupun desktop — sambil tetap <b>memakai ulang seluruh mesin
 * transaksi yang sudah teruji</b> sehingga tidak ada jalur uang baru yang diciptakan.
 * </p>
 *
 * <h3>Lima langkah</h3>
 * <ol>
 *   <li><b>Periode</b> — memilih batas "s/d Bulan" dan "Tahun" tagihan yang ingin
 *       ditampilkan (default: bulan/tahun berjalan), meniru pemilih periode pada engine.</li>
 *   <li><b>Pilih Tagihan</b> — daftar {@link Tagihan} dimuat melalui jalur data engine yang
 *       sama persis: kriteria {@link PengaturanBiaya#terapkanFilterPembayaran}, penyaring
 *       relevansi {@code DetailTagihanSiswaHelper.apakahAda} /
 *       {@code DetailTagihanCalonSiswaHelper.apakahAda}, lalu pemuat
 *       {@code TagihanUtil.getTagihan} / {@code TagihanUtilCalonSiswa.getTagihan} — termasuk
 *       guard anti pembayaran dobel yang tertanam di dalamnya. Hanya tagihan aktif, bukan
 *       "bukan-tagihan", dan belum terhubung {@code PembayaranSiswaDetail} (belum lunas)
 *       yang ditampilkan sebagai kartu ber-checkbox.</li>
 *   <li><b>Atur Nominal</b> — nominal per tagihan hanya dapat diubah bila item biayanya
 *       ber-flag {@code ItemBiayaSekolah.nilaiBiayaBisaDiubahSaatPembayaran} — aturan yang
 *       sama dengan {@code PembayaranSiswa.chekDetail}. Tagihan lain terkunci penuh karena
 *       model data siswa bersifat lunas-per-tagihan (angsuran dimodelkan sebagai baris
 *       tagihan {@code bayarKe} terpisah, bukan pembayaran parsial).</li>
 *   <li><b>Cara Bayar</b> — tombol dirakit dengan aturan aktivasi yang identik dengan
 *       {@code PembayaranOnline.reloadTagihan}: tunai per {@link AkunPembayaranSiswa}
 *       manual-aktif (khusus staf/kasir — akun siswa/orang tua/calon tidak pernah melihatnya),
 *       BRI/BNI/BSI langsung (kunci {@code aktifkan_pembayaran_via_*} plus varian per-sekolah
 *       {@code ..._sekolah_&lt;id&gt;}), BTN, Flip/Smartlink/Finpay/BJB Syariah (flag pada entity
 *       {@link Sekolah}), serta keluarga VA online Maja/Bank Online/Bank Online 2 (kunci
 *       konfigurasi + per-sekolah). Setiap eksekusi melewati dialog konfirmasi total dan
 *       guard anti klik-ganda.</li>
 *   <li><b>Selesai</b> — konfirmasi; bila kanal menerbitkan Virtual Account, nomor VA
 *       (beserta prefix kode bank lain), gambar QR, total termasuk biaya admin, dan batas
 *       waktu pembayaran ditampilkan pada kartu yang mudah disalin/dipindai.</li>
 * </ol>
 *
 * <h3>Strategi reuse (tidak ada jalur uang baru)</h3>
 * <ul>
 *   <li><b>Tunai</b> — {@link TunaiSiswaCommon#onSave}: wizard hanya menyintesis {@code Rows}
 *       in-memory beratribut {@code "pilih"}/{@code "tagihan"}/{@code "nominal"} yang dituntut
 *       {@link PembayaranSiswa#chekDetail}/{@code saveDetail}, lalu mencetak struk via
 *       {@link PembayaranSiswaUtil#cetakStruk}. Seluruh pembuatan
 *       {@code PembayaranSiswa(+Detail)}, penautan tagihan, dedup kodeUnik, dan transaksi
 *       tetap di tangan mesin lama.</li>
 *   <li><b>BRI/BNI/BSI</b> — overload siswa {@code XxxCommon.onSaveXxx(Siswa, CalonSiswa,
 *       Collection&lt;Tagihan&gt;, total, true, deposit)} persis panggilan engine.</li>
 *   <li><b>BTN &amp; keluarga VA online</b> — {@code DownloadTagihanMahasiswaBankBtn.downloadData}
 *       dan {@link DownloadTagihanSiswaBankOnline#downloadData} dengan {@link BankHost} dan
 *       {@link AkunPembayaranSiswa} VA (manual=false) yang diresolusi dengan query yang sama
 *       dengan engine.</li>
 * </ul>
 *
 * <p>
 * <b>Batasan yang disengaja</b> (fitur engine yang tidak diporting karena bersifat meja
 * kasir): pembayaran dari tabungan, deposit/top-up, pratinjau jurnal, ubah/hapus riwayat.
 * Untuk kebutuhan tersebut staf tetap memakai halaman engine. <b>Kompatibilitas:</b>
 * Java 1.7 / ZK 5 CE — semua listener anonymous inner class. <b>Entry point:</b>
 * {@link #buka(Siswa, EventListener)} dan {@link #bukaCalon(CalonSiswa, EventListener)}.
 * </p>
 */
public class WizardPembayaranSiswaHelper {

    // ============================================================ INNER DTO
    /** Satu baris tagihan pada wizard: entity + nilai turunannya + status pilihan UI. */
    private static final class ItemTagihan {
        final Tagihan tagihan;
        final String nama;
        final String periode;
        final double nominalDasar;
        final double denda;
        final double diskon;
        final boolean bisaDiubah;
        boolean dipilih = true;
        double nominalBayar;
        Div cardDiv;

        ItemTagihan(Tagihan t, String nama, String periode, double nominalDasar, double denda,
                double diskon, boolean bisaDiubah) {
            this.tagihan = t;
            this.nama = nama;
            this.periode = periode;
            this.nominalDasar = nominalDasar;
            this.denda = denda;
            this.diskon = diskon;
            this.bisaDiubah = bisaDiubah;
            this.nominalBayar = nominalDasar;
        }

        /** Nilai yang benar-benar ditagihkan = nominal − diskon + denda (rumus chekDetail). */
        double totalDitagih() {
            return Math.max(0, nominalBayar - diskon + denda);
        }
    }

    // ============================================================ CONSTANTS
    private static final String[] JUDUL = {
        "Periode", "Pilih Tagihan", "Atur Nominal", "Cara Bayar", "Selesai"
    };
    private static final String[] NAMA_BULAN = {
        "Januari", "Februari", "Maret", "April", "Mei", "Juni",
        "Juli", "Agustus", "September", "Oktober", "November", "Desember"
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

    // ============================================================ FIELDS
    private final Siswa siswa;
    private final CalonSiswa calonSiswa;
    private final EventListener onSelesai;

    private Integer bulan;
    private Integer tahun;
    private List<ItemTagihan> itemTagihans = new ArrayList<ItemTagihan>();
    private int langkah = 1;

    private Combobox cboBulan;
    private Combobox cboTahun;

    private MyWindow window;
    private Div root;
    private Div stepperHost;
    private Div bodyHost;
    private Div footerHost;

    // Info hasil Virtual Account (dirender di langkah Selesai)
    private String vaLabelBank;
    private String vaKode;
    private String vaTotal;
    private String vaKadaluarsa;
    private String vaQrUrl;

    /** Guard anti klik-ganda selama sebuah eksekusi pembayaran berlangsung. */
    private boolean sedangProses = false;

    // ============================================================ ENTRY POINT
    private WizardPembayaranSiswaHelper(Siswa siswa, CalonSiswa calonSiswa, EventListener onSelesai) {
        this.siswa = siswa;
        this.calonSiswa = calonSiswa;
        this.onSelesai = onSelesai;
        Date now = WaktuUtil.getDate();
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.setTime(now);
        this.bulan = cal.get(java.util.Calendar.MONTH) + 1;
        this.tahun = cal.get(java.util.Calendar.YEAR);
    }

    /** Buka wizard pembayaran untuk seorang Siswa. */
    public static void buka(Siswa siswa, EventListener onSelesai) throws Exception {
        new WizardPembayaranSiswaHelper(siswa, null, onSelesai).tampilkan();
    }

    /** Buka wizard pembayaran untuk seorang Calon Siswa (PSB). */
    public static void bukaCalon(CalonSiswa calonSiswa, EventListener onSelesai) throws Exception {
        new WizardPembayaranSiswaHelper(null, calonSiswa, onSelesai).tampilkan();
    }

    public void tampilkan() throws Exception {
        if ((siswa == null || siswa.getId() == null) && (calonSiswa == null || calonSiswa.getId() == null)) {
            alertar("Data siswa/calon siswa belum tersedia.");
            return;
        }

        boolean mobile = Common.isMobile();

        window = new MyWindow("", "none", false);
        window.setSclass("ais-standard-window ais-mywindow wz-siswa-bayar"
                + (mobile ? " wz-siswa-mobile" : ""));
        window.setContentStyle("background:#f1f5f9;overflow:hidden;box-sizing:border-box;");

        root = new Div();
        root.setParent(window);
        root.setWidth("100%");
        root.setStyle("display:flex;flex-direction:column;height:100%;box-sizing:border-box;");

        Div header = buildHeader();
        header.setParent(root);

        stepperHost = new Div();
        stepperHost.setParent(root);
        stepperHost.setWidth("100%");
        stepperHost.setStyle("flex:0 0 auto;background:#fff;border-bottom:1px solid #e2e8f0;padding:10px 14px;");

        bodyHost = new Div();
        bodyHost.setParent(root);
        bodyHost.setWidth("100%");
        bodyHost.setStyle("flex:1 1 auto;min-height:0;overflow-y:auto;overflow-x:hidden;"
                + "padding:14px;box-sizing:border-box;-webkit-overflow-scrolling:touch;");

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
        try { window.setPosition("center"); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/WizardPembayaranSiswaHelper.java:274"); /* ignore */ }
    }

    // ============================================================ HEADER & FRAME
    private Div buildHeader() {
        String nama = siswa != null ? siswa.getNama() : calonSiswa.getNama();
        Div header = new Div();
        header.setWidth("100%");
        header.setStyle("flex:0 0 auto;position:relative;");
        header.appendChild(new Html(
            "<div style='background:linear-gradient(135deg,#166534,#16a34a,#22c55e);color:#fff;"
            + "padding:14px 46px 14px 18px;'>"
            + "<div style='font-size:16px;font-weight:800;'>🧾 Wizard Pembayaran "
            + (siswa != null ? "Siswa" : "Calon Siswa") + "</div>"
            + "<div style='font-size:11px;opacity:.85;margin-top:2px;"
            + "white-space:nowrap;overflow:hidden;text-overflow:ellipsis;'>"
            + escHtml(nama) + "</div></div>"));

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
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/WizardPembayaranSiswaHelper.java:305"); /* ignore */ }
        try { window.detach(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/WizardPembayaranSiswaHelper.java:306"); /* ignore */ }
    }

    private void injectCssMobile(boolean mobile) {
        if (!mobile) return;
        try {
            root.appendChild(new Html("<style>"
                + ".wz-siswa-mobile.z-window-highlighted,.wz-siswa-mobile{"
                + "position:fixed !important;left:0 !important;top:0 !important;"
                + "right:0 !important;bottom:0 !important;"
                + "width:100vw !important;height:100vh !important;height:100dvh !important;"
                + "max-width:none !important;max-height:none !important;"
                + "margin:0 !important;border-radius:0 !important;}"
                + ".wz-siswa-mobile .z-window-highlighted-cnt,"
                + ".wz-siswa-mobile .z-window-highlighted-cnt-noborder{"
                + "width:100% !important;height:100% !important;max-height:100% !important;"
                + "overflow:hidden !important;border-radius:0 !important;box-sizing:border-box !important;}"
                + ".wz-siswa-mobile .z-window-highlighted-shadow{box-shadow:none !important;}"
                + "</style>"));
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/WizardPembayaranSiswaHelper.java:325"); /* ignore */ }
    }

    // ============================================================ RENDER FRAME
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
            boolean tallStep = langkah >= 2 && langkah <= 4;
            window.setHeight(tallStep ? "88%" : null);
            if (root != null) root.setHeight(tallStep ? "100%" : null);
        }
        try { window.setPosition("center"); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/WizardPembayaranSiswaHelper.java:347"); /* ignore */ }
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

        if (langkah == 5) {
            MyButtonConfig btnTutup = new MyButtonConfig("Tutup");
            btnTutup.setStyle(BTN_PRIMARY);
            btnTutup.addEventListener("onClick", new EventListener() {
                @Override public void onEvent(Event e) throws Exception { tutup(); }
            });
            footerHost.appendChild(btnTutup);
        } else if (langkah < 4) {
            String label = langkah == 1 ? "Muat Tagihan →" : (langkah == 2 ? "Atur Nominal →" : "Cara Bayar →");
            MyButtonConfig btnKanan = new MyButtonConfig(label);
            btnKanan.setStyle(BTN_PRIMARY);
            btnKanan.addEventListener("onClick", new EventListener() {
                @Override public void onEvent(Event e) throws Exception { onNext(); }
            });
            footerHost.appendChild(btnKanan);
        }
    }

    private void onNext() throws Exception {
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
                return;
        }
        render();
    }

    // ============================================================ STEP 1: PERIODE
    private void renderStep1() {
        String nama = siswa != null ? siswa.getNama() : calonSiswa.getNama();
        Div wrap = new Div();
        wrap.setParent(bodyHost);
        wrap.setWidth("100%");
        wrap.setStyle("max-width:540px;margin:0 auto;display:flex;flex-direction:column;gap:12px;");

        wrap.appendChild(new Html(
            "<div style='" + CARD_STYLE + "'>"
            + "<div style='font-size:11px;color:#64748b;font-weight:700;'>"
            + (siswa != null ? "SISWA" : "CALON SISWA") + "</div>"
            + "<div style='font-size:15px;font-weight:800;color:#0f172a;margin-top:2px;'>"
            + escHtml(nama) + "</div></div>"));

        wrap.appendChild(new Html(
            "<div style='font-size:12px;color:#475569;line-height:1.5;"
            + "background:#f0fdf4;border:1px solid #bbf7d0;border-radius:10px;padding:10px 12px;'>"
            + "Pilih batas periode tagihan yang ingin dibayar. Semua tagihan yang jatuh tempo "
            + "<b>sampai dengan</b> bulan/tahun tersebut akan dimuat pada langkah berikutnya.</div>"));

        wrap.appendChild(new Html("<label style='" + LABEL_SM + "'>s/d Bulan</label>"));
        cboBulan = new Combobox();
        cboBulan.setReadonly(true);
        cboBulan.setWidth("100%");
        for (int i = 1; i <= 12; i++) {
            Comboitem ci = new Comboitem(NAMA_BULAN[i - 1]);
            ci.setValue(i);
            cboBulan.appendChild(ci);
            if (bulan != null && bulan.intValue() == i) cboBulan.setSelectedItem(ci);
        }
        wrap.appendChild(cboBulan);

        wrap.appendChild(new Html("<label style='" + LABEL_SM + "'>Tahun</label>"));
        cboTahun = new Combobox();
        cboTahun.setReadonly(true);
        cboTahun.setWidth("100%");
        int tahunNow = tahun == null ? 2026 : tahun;
        for (int th = tahunNow - 3; th <= tahunNow + 1; th++) {
            Comboitem ci = new Comboitem(String.valueOf(th));
            ci.setValue(th);
            cboTahun.appendChild(ci);
            if (tahun != null && tahun.intValue() == th) cboTahun.setSelectedItem(ci);
        }
        wrap.appendChild(cboTahun);
    }

    private boolean validasiStep1() {
        if (cboBulan == null || cboBulan.getSelectedItem() == null
                || !(cboBulan.getSelectedItem().getValue() instanceof Integer)) {
            alertar("Pilih bulan terlebih dahulu.");
            return false;
        }
        if (cboTahun == null || cboTahun.getSelectedItem() == null
                || !(cboTahun.getSelectedItem().getValue() instanceof Integer)) {
            alertar("Pilih tahun terlebih dahulu.");
            return false;
        }
        bulan = (Integer) cboBulan.getSelectedItem().getValue();
        tahun = (Integer) cboTahun.getSelectedItem().getValue();
        return true;
    }

    // ============================================================ DATA LOADING

    /**
     * Memuat tagihan melalui jalur data engine (PengaturanBiaya → apakahAda →
     * TagihanUtil[.CalonSiswa].getTagihan) dan menyaringnya dengan predikat yang sama dengan
     * {@code PembayaranOnline.reloadTagihan}: aktif, bukan "bukan-tagihan", dan belum lunas
     * (belum tertaut {@code PembayaranSiswaDetail}).
     */
    private void muatTagihan() {
        itemTagihans = new ArrayList<ItemTagihan>();

        List<PengaturanBiaya> pbs = null;
        Session session = null;
        try {
            session = HibernateUtil.openSession();
            pbs = ConstantValues.simpleList(
                    PengaturanBiaya.terapkanFilterPembayaran(session.createCriteria(PengaturanBiaya.class),
                            siswa, calonSiswa),
                    PengaturanBiaya.class);
        } catch (Exception e) {
            alertar("Gagal memuat pengaturan biaya: " + e.getMessage());
            return;
        } finally {
            HibernateUtil.closeSessionQuietly(session);
        }
        if (pbs == null) return;

        for (PengaturanBiaya pb : pbs) {
            try {
                if (siswa != null && !DetailTagihanSiswaHelper.apakahAda(pb, siswa)) continue;
                if (calonSiswa != null && !DetailTagihanCalonSiswaHelper.apakahAda(pb, calonSiswa)) continue;

                List<Tagihan> tags = siswa != null
                        ? TagihanUtil.getTagihan(pb.getJenisBiayaSekolah(), pb, siswa, bulan, tahun, true)
                        : TagihanUtilCalonSiswa.getTagihan(pb.getJenisBiayaSekolah(), pb, calonSiswa, bulan,
                                tahun, true);
                if (tags == null) continue;

                for (Tagihan t : tags) {
                    try {
                        if (t == null || t.getNominalBiaya() == null) continue;
                        if (!Boolean.TRUE.equals(t.getAktif())) continue;
                        if (Boolean.TRUE.equals(t.ambilBukanTagihanData())) continue;
                        if (Boolean.TRUE.equals(t.getNominalBiaya().getBukanTagihan())) continue;
                        if (t.getPembayaranSiswaDetail() != null) continue; // sudah lunas

                        String namaItem = "-";
                        boolean bisaDiubah = false;
                        try {
                            namaItem = t.getNominalBiaya().getItemBiayaSekolah().getNama();
                            bisaDiubah = Boolean.TRUE.equals(t.getNominalBiaya().getItemBiayaSekolah()
                                    .getNilaiBiayaBisaDiubahSaatPembayaran());
                        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/WizardPembayaranSiswaHelper.java:566"); /* nama/flag opsional */ }

                        String periode = "";
                        try {
                            if (t.getBulan() != null && t.getBulan() >= 1 && t.getBulan() <= 12) {
                                periode = NAMA_BULAN[t.getBulan() - 1]
                                        + (t.getTahun() != null ? " " + t.getTahun() : "");
                            } else if (t.getTahunAjaran() != null) {
                                periode = t.getTahunAjaran();
                            }
                            if (t.getBayarKe() != null && t.getBayarKe() > 1) {
                                periode += (periode.isEmpty() ? "" : " · ") + "Angsuran ke-" + t.getBayarKe();
                            }
                        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/WizardPembayaranSiswaHelper.java:579"); /* label opsional */ }

                        double nominal = t.getNominal() == null ? 0 : t.getNominal();
                        double denda = 0;
                        double diskon = 0;
                        try { denda = t.getDenda() == null ? 0 : t.getDenda(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/WizardPembayaranSiswaHelper.java:584"); }
                        try { diskon = t.getDiskon() == null ? 0 : t.getDiskon(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/WizardPembayaranSiswaHelper.java:585"); }
                        if (nominal - diskon + denda <= 0) continue;

                        itemTagihans.add(new ItemTagihan(t, namaItem, periode, nominal, denda, diskon,
                                bisaDiubah));
                    } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/WizardPembayaranSiswaHelper.java:590"); /* lewati baris bermasalah, jangan gagalkan semua */ }
                }
            } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/WizardPembayaranSiswaHelper.java:592"); /* lewati pengaturan biaya bermasalah */ }
        }

        // PARITAS WIZARD MAHASISWA (permintaan user): tagihan yang SUDAH ADA pembayarannya
        // tidak boleh tampil di wizard seolah belum dibayar. Saringan di atas hanya melihat
        // tautan penuh Tagihan.pembayaranSiswaDetail; pembayaran yang tercatat lewat jalur
        // yang hanya mengisi PembayaranSiswaDetail.tagihan (tanpa backfill kolom di baris
        // Tagihan) akan lolos sehingga wizard menagih PENUH lagi. Hitung total terbayar per
        // tagihan langsung dari PembayaranSiswaDetail: lunas -> buang dari daftar,
        // parsial -> sisakan kekurangannya saja. Baris tanpa pembayaran sama sekali tidak
        // berubah perilaku sedikit pun.
        if (!itemTagihans.isEmpty()) {
            Session sBayar = null;
            try {
                List<Long> idsTagihan = new ArrayList<Long>();
                for (ItemTagihan it : itemTagihans) {
                    if (it.tagihan != null && it.tagihan.getId() != null) {
                        idsTagihan.add(it.tagihan.getId());
                    }
                }
                if (!idsTagihan.isEmpty()) {
                    sBayar = HibernateUtil.openSession();
                    List baris = sBayar.createCriteria(ais.database.model.sekolah.PembayaranSiswaDetail.class)
                            .createAlias("tagihan", "tg")
                            .add(Restrictions.in("tg.id", idsTagihan))
                            .setProjection(org.hibernate.criterion.Projections.projectionList()
                                    .add(org.hibernate.criterion.Projections.groupProperty("tg.id"))
                                    .add(org.hibernate.criterion.Projections.sum("nominal")))
                            .list();
                    Map<Long, Double> terbayarPerTagihan = new HashMap<Long, Double>();
                    for (Object o : baris) {
                        Object[] arr = (Object[]) o;
                        if (arr != null && arr.length >= 2 && arr[0] != null && arr[1] != null) {
                            terbayarPerTagihan.put((Long) arr[0], Double.valueOf(((Number) arr[1]).doubleValue()));
                        }
                    }
                    java.util.Iterator<ItemTagihan> iterItem = itemTagihans.iterator();
                    while (iterItem.hasNext()) {
                        ItemTagihan it = iterItem.next();
                        Double terbayar = (it.tagihan == null || it.tagihan.getId() == null) ? null
                                : terbayarPerTagihan.get(it.tagihan.getId());
                        if (terbayar == null || terbayar.doubleValue() <= 0.01) {
                            continue;
                        }
                        double sisa = it.totalDitagih() - terbayar.doubleValue();
                        if (sisa <= 0.01) {
                            iterItem.remove(); // sudah lunas via PembayaranSiswaDetail
                        } else {
                            // totalDitagih() = max(0, nominalBayar - diskon + denda) -> supaya
                            // totalDitagih() baru == sisa, geser nominalBayar-nya.
                            it.nominalBayar = sisa + it.diskon - it.denda;
                            if (it.nominalBayar < 0) {
                                it.nominalBayar = 0;
                            }
                        }
                    }
                }
            } catch (Exception eBayar) {
                ais.common.ErrorAuditUtil.record(eBayar,
                        "Wizard Pembayaran Siswa: gagal hitung terbayar per tagihan (paritas wizard mahasiswa)");
            } finally {
                HibernateUtil.closeSessionQuietly(sBayar);
            }
        }
    }

    // ============================================================ STEP 2: PILIH TAGIHAN
    private void renderStep2() {
        if (itemTagihans.isEmpty()) {
            bodyHost.appendChild(new Html(
                "<div style='text-align:center;padding:40px 20px;color:#94a3b8;'>"
                + "<div style='font-size:40px;margin-bottom:10px;'>📋</div>"
                + "<div style='font-size:14px;font-weight:600;'>Tidak ada tagihan yang harus dibayar</div>"
                + "<div style='font-size:12px;margin-top:6px;'>Semua tagihan hingga periode terpilih sudah lunas, "
                + "atau belum ada tagihan yang diterbitkan. Coba ubah periode pada langkah sebelumnya.</div>"
                + "</div>"));
            return;
        }

        bodyHost.appendChild(new Html(
            "<div style='font-size:12px;color:#166534;font-weight:700;margin-bottom:10px;'>"
            + "Centang tagihan yang akan dibayar (" + itemTagihans.size() + " tagihan belum lunas):</div>"));

        for (final ItemTagihan item : itemTagihans) {
            Div card = new Div();
            card.setStyle(CARD_STYLE);
            item.cardDiv = card;

            Div headerRow = new Div();
            headerRow.setStyle("display:flex;align-items:flex-start;justify-content:space-between;gap:10px;");
            headerRow.appendChild(new Html(
                "<div style='flex:1;min-width:0;'>"
                + "<div style='font-weight:700;color:#0f172a;font-size:14px;'>" + escHtml(item.nama) + "</div>"
                + (item.periode.isEmpty() ? ""
                        : "<div style='font-size:11px;color:#64748b;margin-top:2px;'>" + escHtml(item.periode)
                                + "</div>")
                + "</div>"));

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
            card.appendChild(headerRow);

            StringBuilder rincian = new StringBuilder();
            rincian.append("<div style='display:flex;gap:14px;flex-wrap:wrap;font-size:12px;color:#475569;margin-top:8px;'>");
            rincian.append("<span><b>Tagihan:</b> ").append(formatRp(item.nominalDasar)).append("</span>");
            if (item.diskon > 0) {
                rincian.append("<span style='color:#16a34a;'><b>Diskon:</b> −")
                       .append(formatRp(item.diskon)).append("</span>");
            }
            if (item.denda > 0) {
                rincian.append("<span style='color:#dc2626;'><b>Denda:</b> +")
                       .append(formatRp(item.denda)).append("</span>");
            }
            rincian.append("<span><b>Dibayar:</b> <b style='color:#0f172a;'>")
                   .append(formatRp(item.totalDitagih())).append("</b></span>");
            rincian.append("</div>");
            card.appendChild(new Html(rincian.toString()));

            updateCardBorder(item);
            bodyHost.appendChild(card);
        }
    }

    private void updateCardBorder(ItemTagihan item) {
        if (item.cardDiv == null) return;
        if (item.dipilih) {
            item.cardDiv.setStyle(CARD_STYLE + "border-color:#16a34a;box-shadow:0 0 0 2px rgba(22,163,74,.12);");
        } else {
            item.cardDiv.setStyle(CARD_STYLE + "opacity:.75;");
        }
    }

    private boolean validasiStep2() {
        for (ItemTagihan item : itemTagihans) {
            if (item.dipilih) return true;
        }
        alertar("Pilih minimal satu tagihan yang akan dibayar.");
        return false;
    }

    // ============================================================ STEP 3: ATUR NOMINAL

    /**
     * Nominal hanya dapat diubah pada item ber-flag
     * {@code nilaiBiayaBisaDiubahSaatPembayaran} — aturan yang sama dengan
     * {@code PembayaranSiswa.chekDetail}. Item lain ditampilkan terkunci karena model
     * pembayaran siswa bersifat lunas-per-tagihan.
     */
    private void renderStep3() {
        bodyHost.appendChild(new Html(
            "<div style='font-size:12px;color:#475569;line-height:1.5;"
            + "background:#f0fdf4;border:1px solid #bbf7d0;border-radius:10px;padding:10px 12px;margin-bottom:12px;'>"
            + "Periksa kembali nominal per tagihan. Hanya item tertentu (yang diizinkan sekolah) "
            + "yang nominalnya dapat diubah; tagihan lain dibayar penuh.</div>"));

        final Div totalDiv = new Div();
        totalDiv.setStyle(CARD_STYLE + "background:#f0fdf4;border-color:#bbf7d0;margin-bottom:14px;");
        rebuiltTotalCard(totalDiv);
        bodyHost.appendChild(totalDiv);

        for (final ItemTagihan item : itemTagihans) {
            if (!item.dipilih) continue;

            Div card = new Div();
            card.setStyle(CARD_STYLE);
            card.appendChild(new Html(
                "<div style='font-weight:700;color:#166534;margin-bottom:2px;font-size:13px;'>"
                + escHtml(item.nama) + "</div>"
                + (item.periode.isEmpty() ? ""
                        : "<div style='font-size:11px;color:#64748b;margin-bottom:8px;'>" + escHtml(item.periode)
                                + "</div>")));

            if (item.bisaDiubah) {
                card.appendChild(new Html("<label style='" + LABEL_SM + "'>Nominal Bayar (Rp) — boleh diubah</label>"));
                final Decimalbox dec = new Decimalbox();
                dec.setValue(new java.math.BigDecimal(item.nominalBayar));
                dec.setWidth("100%");
                dec.setStyle("font-size:14px;font-weight:700;box-sizing:border-box;");
                dec.setFormat("#,##0.##");
                dec.addEventListener("onChange", new EventListener() {
                    @Override public void onEvent(Event e) throws Exception {
                        try {
                            double v = dec.getValue() != null ? dec.getValue().doubleValue() : 0;
                            if (v < 0) { v = 0; dec.setValue(java.math.BigDecimal.ZERO); }
                            item.nominalBayar = v;
                            rebuiltTotalCard(totalDiv);
                        } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/WizardPembayaranSiswaHelper.java:724"); /* biarkan nilai lama */ }
                    }
                });
                card.appendChild(dec);
            } else {
                card.appendChild(new Html(
                    "<div style='font-size:15px;font-weight:800;color:#0f172a;'>"
                    + formatRp(item.nominalBayar) + "</div>"
                    + "<div style='font-size:11px;color:#94a3b8;margin-top:2px;'>🔒 dibayar penuh</div>"));
            }

            if (item.diskon > 0 || item.denda > 0) {
                card.appendChild(new Html(
                    "<div style='font-size:11px;color:#64748b;margin-top:6px;'>"
                    + (item.diskon > 0 ? "Diskon −" + formatRp(item.diskon) + "  " : "")
                    + (item.denda > 0 ? "Denda +" + formatRp(item.denda) : "")
                    + " → dibayar <b>" + formatRp(item.totalDitagih()) + "</b></div>"));
            }
            bodyHost.appendChild(card);
        }
    }

    private void rebuiltTotalCard(Div totalCard) {
        if (totalCard == null) return;
        Common.clear(totalCard);
        totalCard.appendChild(new Html(
            "<div style='font-size:11px;color:#16a34a;font-weight:700;'>TOTAL AKAN DIBAYAR</div>"
            + "<div style='font-size:22px;font-weight:800;color:#15803d;margin-top:4px;'>"
            + formatRp(hitungTotalBayar()) + "</div>"));
    }

    private boolean validasiStep3() {
        if (hitungTotalBayar() <= 0) {
            alertar("Total pembayaran harus lebih dari 0.");
            return false;
        }
        return true;
    }

    // ============================================================ STEP 4: CARA BAYAR

    /**
     * Merender tombol saluran pembayaran dengan aturan aktivasi yang identik dengan
     * {@code PembayaranOnline.reloadTagihan}: tunai per {@link AkunPembayaranSiswa} (khusus
     * staf/kasir), BRI/BNI/BSI/BTN (kunci konfigurasi + varian per-sekolah), gateway link
     * (flag entity {@link Sekolah}), dan keluarga VA online (kunci konfigurasi + per-sekolah).
     */
    private void renderStep4() {
        vaLabelBank = null; vaKode = null; vaTotal = null; vaKadaluarsa = null; vaQrUrl = null;

        double total = hitungTotalBayar();
        final List<ItemTagihan> dipilih = getItemsDipilih();
        final Sekolah sekolah = getSekolah();

        bodyHost.appendChild(new Html(
            "<div style='" + CARD_STYLE + "background:#f0fdf4;border-color:#bbf7d0;margin-bottom:12px;'>"
            + "<div style='font-size:11px;color:#16a34a;font-weight:700;'>TOTAL PEMBAYARAN</div>"
            + "<div style='font-size:24px;font-weight:800;color:#15803d;margin-top:4px;'>"
            + formatRp(total) + "</div></div>"));

        Div ringkasan = new Div();
        ringkasan.setStyle(CARD_STYLE + "margin-bottom:14px;");
        ringkasan.appendChild(new Html(
            "<div style='font-weight:700;color:#166534;margin-bottom:8px;font-size:13px;'>Tagihan yang dibayar:</div>"));
        for (ItemTagihan item : dipilih) {
            ringkasan.appendChild(new Html(
                "<div style='display:flex;justify-content:space-between;padding:5px 0;gap:10px;"
                + "border-bottom:1px solid #f1f5f9;font-size:12px;'>"
                + "<span style='color:#374151;min-width:0;'>" + escHtml(item.nama)
                + (item.periode.isEmpty() ? "" : " <span style='color:#94a3b8;'>· " + escHtml(item.periode) + "</span>")
                + "</span>"
                + "<span style='font-weight:700;color:#0f172a;flex:0 0 auto;'>" + formatRp(item.totalDitagih())
                + "</span></div>"));
        }
        bodyHost.appendChild(ringkasan);

        bodyHost.appendChild(new Html("<div style='" + LABEL_SM + "margin-bottom:10px;'>Pilih cara pembayaran:</div>"));

        Div btnWrap = new Div();
        btnWrap.setStyle("display:flex;flex-wrap:wrap;gap:10px;");
        bodyHost.appendChild(btnWrap);

        int jumlahTombol = 0;

        // --- Tunai per AkunPembayaranSiswa (staf/kasir saja — gate PembayaranOnline:2334) ---
        if (isUserStaf() && sekolah != null) {
            for (final AkunPembayaranSiswa akun : ambilAkunTunai(sekolah)) {
                tambahTombolBayar(btnWrap, "💵 Bayar via " + akun.getNama(), "#16a34a", "#dcfce7",
                        new EventListener() {
                            @Override public void onEvent(Event e) throws Exception {
                                konfirmasiBayar("Tunai — " + akun.getNama(), new EventListener() {
                                    @Override public void onEvent(Event ev) throws Exception {
                                        bayarTunai(akun, dipilih);
                                    }
                                });
                            }
                        });
                jumlahTombol++;
            }
        }

        // --- BRI/BNI/BSI langsung (kunci + varian per-sekolah, persis createDirectBankButton) ---
        if (bankLangsungAktif("bri", sekolah, false)) {
            jumlahTombol += tambahTombolBank(btnWrap, "🏦 BAYAR VIA BRI", dipilih, 1);
        }
        if (bankLangsungAktif("bni", sekolah, true)) {
            jumlahTombol += tambahTombolBank(btnWrap, "🏦 BAYAR VIA BNI", dipilih, 2);
        }
        if (bankLangsungAktif("bsi", sekolah, true)) {
            jumlahTombol += tambahTombolBank(btnWrap, "🏦 BAYAR VIA BSI", dipilih, 3);
        }
        if (bankLangsungAktif("bank_btn", sekolah, true)) {
            tambahTombolBayar(btnWrap, "🏦 BAYAR VIA BTN", "#1e40af", "#eff6ff", new EventListener() {
                @Override public void onEvent(Event e) throws Exception {
                    konfirmasiBayar("BAYAR VIA BTN", new EventListener() {
                        @Override public void onEvent(Event ev) throws Exception { bayarBtn(dipilih, sekolah); }
                    });
                }
            });
            jumlahTombol++;
        }

        // --- Gateway bertautan dari flag Sekolah (Flip/Smartlink/Finpay/BJB Syariah) ---
        if (sekolah != null) {
            jumlahTombol += tambahGatewayLink(btnWrap, sekolah, dipilih);
        }

        // --- Keluarga VA online (Maja / Bank Online / Bank Online 2) ---
        if (onlineAktif("aktifkan_pembayaran_via_maja", "aktifkan_pembayaran_via_maja_sekolah_", sekolah)) {
            Map<String, Object> pm = new HashMap<String, Object>();
            pm.put("maja", true);
            jumlahTombol += tambahTombolVaOnline(btnWrap, "🏦 BAYAR VIA BSI", "online_bank_host_ip",
                    "online_biaya_maja", "prefix_kode_bank_lain_online", pm, dipilih, sekolah);
        }
        if (onlineAktif("aktifkan_pembayaran_via_bank_online", "aktifkan_pembayaran_via_bank_online_sekolah_",
                sekolah)) {
            jumlahTombol += tambahTombolVaOnline(btnWrap, "🏦 BAYAR ONLINE", "online_bank_host_ip",
                    "online_biaya_administrasi", "prefix_kode_bank_lain_online",
                    new HashMap<String, Object>(), dipilih, sekolah);
        }
        if (onlineAktif("aktifkan_pembayaran_via_bank_online_2", "aktifkan_pembayaran_via_bank_online_sekolah_2_",
                sekolah)) {
            jumlahTombol += tambahTombolVaOnline(btnWrap, "🏦 BAYAR ONLINE 2", "online_2_bank_host_ip",
                    "online_2_biaya_administrasi", "prefix_kode_bank_lain_online_2",
                    new HashMap<String, Object>(), dipilih, sekolah);
        }

        if (jumlahTombol == 0) {
            bodyHost.appendChild(new Html(
                "<div style='" + CARD_STYLE + "background:#fffbeb;border-color:#fde68a;text-align:center;'>"
                + "<div style='font-size:28px;margin-bottom:6px;'>⚠️</div>"
                + "<div style='font-weight:700;color:#92400e;font-size:13px;'>Saluran pembayaran belum tersedia</div>"
                + "<div style='font-size:12px;color:#a16207;margin-top:4px;'>Belum ada saluran pembayaran aktif "
                + "untuk sekolah ini. Silakan hubungi administrator sistem.</div></div>"));
        }
    }

    /** Kartu tombol saluran pembayaran dengan target sentuh ramah mobile. */
    private void tambahTombolBayar(Div parent, String label, String color, String bg, EventListener onClick) {
        MyButtonConfig btn = new MyButtonConfig(label);
        btn.setStyle("background:" + bg + ";color:" + color + ";border:1.5px solid " + color + ";"
                + "border-radius:10px;padding:14px 18px;font-size:13px;font-weight:700;cursor:pointer;"
                + "min-width:150px;min-height:48px;text-align:center;flex:1 1 150px;max-width:100%;"
                + "box-shadow:0 1px 4px rgba(0,0,0,.07);");
        btn.addEventListener("onClick", onClick);
        parent.appendChild(btn);
    }

    private int tambahTombolBank(Div parent, final String label, final List<ItemTagihan> dipilih, final int type) {
        tambahTombolBayar(parent, label, "#1e40af", "#eff6ff", new EventListener() {
            @Override public void onEvent(Event e) throws Exception {
                konfirmasiBayar(label, new EventListener() {
                    @Override public void onEvent(Event ev) throws Exception { bayarBankLangsung(type, dipilih); }
                });
            }
        });
        return 1;
    }

    private int tambahGatewayLink(Div parent, final Sekolah sekolah, final List<ItemTagihan> dipilih) {
        int n = 0;
        try {
            if (Boolean.TRUE.equals(sekolah.getAktfkanPembayaranViaFlip())) {
                n += tombolLink(parent, "💳 BAYAR VIA FLIP", "flip",
                        sekolah.getBiayaAdminFlip() == null ? 0 : sekolah.getBiayaAdminFlip(), dipilih, sekolah);
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/WizardPembayaranSiswaHelper.java:910"); /* flag opsional */ }
        try {
            if (Boolean.TRUE.equals(sekolah.getAktfkanPembayaranViaEsmartlink())) {
                n += tombolLink(parent, "💳 BAYAR VIA SMART LINK", "esmartlink",
                        sekolah.getBiayaAdminEsmartlink() == null ? 0 : sekolah.getBiayaAdminEsmartlink(),
                        dipilih, sekolah);
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/WizardPembayaranSiswaHelper.java:917"); /* flag opsional */ }
        try {
            if (Boolean.TRUE.equals(sekolah.getAktfkanPembayaranViaFinpay())) {
                n += tombolLink(parent, "💳 BAYAR VIA FINPAY", "finpay",
                        sekolah.getBiayaAdminFinpay() == null ? 0 : sekolah.getBiayaAdminFinpay(), dipilih,
                        sekolah);
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/WizardPembayaranSiswaHelper.java:924"); /* flag opsional */ }
        try {
            if (Boolean.TRUE.equals(sekolah.getAktfkanBjbSyariah())) {
                n += tombolLink(parent, "🏦 BAYAR VIA BJB SYARIAH", "bjb_langsung",
                        sekolah.getBiayaAdminBjbSyariah() == null ? 0 : sekolah.getBiayaAdminBjbSyariah(),
                        dipilih, sekolah);
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/WizardPembayaranSiswaHelper.java:931"); /* flag opsional */ }
        return n;
    }

    private int tombolLink(Div parent, final String label, final String paramKey, final double fee,
            final List<ItemTagihan> dipilih, final Sekolah sekolah) {
        tambahTombolBayar(parent, label, "#7c3aed", "#f5f3ff", new EventListener() {
            @Override public void onEvent(Event e) throws Exception {
                konfirmasiBayar(label, new EventListener() {
                    @Override public void onEvent(Event ev) throws Exception {
                        Map<String, Object> pm = new HashMap<String, Object>();
                        pm.put(paramKey, true);
                        bayarVaOnline(label, "online_bank_host_ip", fee, null, pm, dipilih, sekolah, paramKey);
                    }
                });
            }
        });
        return 1;
    }

    private int tambahTombolVaOnline(Div parent, final String label, final String ipKey,
            final String feeConfig, final String prefixKey, final Map<String, Object> param,
            final List<ItemTagihan> dipilih, final Sekolah sekolah) {
        tambahTombolBayar(parent, label, "#1e40af", "#eff6ff", new EventListener() {
            @Override public void onEvent(Event e) throws Exception {
                konfirmasiBayar(label, new EventListener() {
                    @Override public void onEvent(Event ev) throws Exception {
                        double fee = 0;
                        try {
                            fee = Double.parseDouble(Common.getKonfigurasi(feeConfig, "0.0").getNilai());
                        } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/WizardPembayaranSiswaHelper.java:961"); /* fee opsional */ }
                        bayarVaOnline(label, ipKey, fee, prefixKey, param, dipilih, sekolah, null);
                    }
                });
            }
        });
        return 1;
    }

    // ============================================================ GATE HELPERS

    /** Replikasi gate {@code createDirectBankButton}: kunci global + varian {@code _sekolah_<id>}. */
    private boolean bankLangsungAktif(String cfgPrefix, Sekolah sekolah, boolean cekSekolah) {
        boolean aktif = Common.bolehKonfigurasi("aktifkan_pembayaran_via_" + cfgPrefix, Konfigurasi.TIDAK_AKTIF);
        if (aktif && cekSekolah) {
            aktif = Common.getKonfigurasi("aktifkan_pembayaran_via_" + cfgPrefix + "_sekolah_"
                    + (sekolah == null ? "" : sekolah.getId()), Konfigurasi.AKTIF).getNilai()
                    .equals(Konfigurasi.AKTIF);
        }
        return aktif;
    }

    /** Replikasi gate keluarga VA online: kunci global (default MATI) + per-sekolah (default AKTIF). */
    private boolean onlineAktif(String globalKey, String sekolahKeyPrefix, Sekolah sekolah) {
        return Common.bolehKonfigurasi(globalKey, Konfigurasi.TIDAK_AKTIF)
                && Common.getKonfigurasi(sekolahKeyPrefix + (sekolah == null ? "" : sekolah.getId()),
                        Konfigurasi.AKTIF).getNilai().equals(Konfigurasi.AKTIF);
    }

    /**
     * @return true HANYA bila pengguna aktif adalah staf/kasir: akun ter-resolve dan tidak
     *         terkait Mahasiswa, Siswa, Orang Tua, maupun Calon Siswa — gate yang sama
     *         dengan tombol tunai {@code PembayaranOnline} (fail-closed bila user null).
     */
    private boolean isUserStaf() {
        Tbmuser u = null;
        try { u = Common.getCurrentUser(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/WizardPembayaranSiswaHelper.java:997"); /* fallback */ }
        if (u == null) {
            try { u = Common.getTbmuser(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/WizardPembayaranSiswaHelper.java:999"); /* tetap null */ }
        }
        if (u == null) return false;
        try {
            return u.getMahasiswa() == null && u.getSiswa() == null && u.getOrangTua() == null
                    && u.getCalonSiswa() == null;
        } catch (Exception e) {
            return false;
        }
    }

    private Sekolah getSekolah() {
        try {
            Sekolah s = siswa != null ? siswa.getSekolah() : calonSiswa.getSekolah();
            if (s != null) return s;
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/WizardPembayaranSiswaHelper.java:1014"); /* fallback */ }
        try {
            return SekolahUtil.getSekolah();
        } catch (Exception e) {
            return null;
        }
    }

    /** Akun tunai (manual, aktif) per sekolah — query yang sama dengan PembayaranOnline:2340. */
    private List<AkunPembayaranSiswa> ambilAkunTunai(Sekolah sekolah) {
        Session session = null;
        try {
            session = HibernateUtil.openSession();
            List<AkunPembayaranSiswa> list = ConstantValues.simpleList(
                    session.createCriteria(AkunPembayaranSiswa.class)
                            .add(Restrictions.eq("manual", true))
                            .add(Restrictions.eq("sekolah", sekolah))
                            .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                            .addOrder(Order.asc("nama")),
                    AkunPembayaranSiswa.class);
            return list == null ? new ArrayList<AkunPembayaranSiswa>() : list;
        } catch (Exception e) {
            return new ArrayList<AkunPembayaranSiswa>();
        } finally {
            HibernateUtil.closeSessionQuietly(session);
        }
    }

    /** Akun VA otomatis (manual=false, aktif) per sekolah — query PembayaranOnline:1900. */
    private AkunPembayaranSiswa ambilAkunVa(Sekolah sekolah) {
        Session session = null;
        try {
            session = HibernateUtil.openSession();
            return (AkunPembayaranSiswa) ConstantValues.simpleObject(
                    session.createCriteria(AkunPembayaranSiswa.class)
                            .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                            .add(Restrictions.eq("manual", false))
                            .add(Restrictions.eq("sekolah", sekolah))
                            .setMaxResults(1),
                    AkunPembayaranSiswa.class);
        } catch (Exception e) {
            return null;
        } finally {
            HibernateUtil.closeSessionQuietly(session);
        }
    }

    /**
     * Dialog konfirmasi + guard anti klik-ganda sebelum eksekusi pembayaran —
     * pola yang sama dengan {@code confirmAndExecute} pada engine.
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

    // ============================================================ EKSEKUSI: TUNAI

    /**
     * Pembayaran tunai via mesin {@link TunaiSiswaCommon#onSave} — wizard hanya menyintesis
     * {@code Rows} beratribut {@code "pilih"}/{@code "tagihan"}/{@code "nominal"} yang
     * dibaca {@code PembayaranSiswa.chekDetail/saveDetail}, lalu mencetak struk.
     */
    private void bayarTunai(AkunPembayaranSiswa akun, List<ItemTagihan> dipilih) {
        try {
            Rows rowsMock = buatRowsMock(dipilih);
            List<Tagihan> tags = ambilTagihanTerpilih(dipilih);
            String validator = getCurrentUserNama();

            PembayaranSiswa pemb = TunaiSiswaCommon.onSave(siswa, calonSiswa, tags, 0.0, null, validator,
                    akun, rowsMock, WaktuUtil.getDate());

            try {
                if (pemb != null) PembayaranSiswaUtil.cetakStruk(pemb);
            } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/WizardPembayaranSiswaHelper.java:1103"); /* struk pelengkap — jangan gagalkan pembayaran */ }

            langkah = 5;
            render();
        } catch (Exception ex) {
            alertar("Gagal menyimpan pembayaran tunai: " + ex.getMessage());
        }
    }

    /**
     * Menyintesis {@code Rows} in-memory dengan struktur atribut baris yang dituntut
     * {@code PembayaranSiswa.chekDetail/saveDetail}: {@code "pilih"}
     * ({@link MyCheckboxConfig} tercentang), {@code "tagihan"} ({@link Tagihan}), dan
     * {@code "nominal"} ({@link Doublebox} — hanya dibaca untuk item ber-flag
     * bisa-diubah).
     */
    private Rows buatRowsMock(List<ItemTagihan> dipilih) {
        Rows rows = new Rows();
        for (ItemTagihan item : dipilih) {
            Row row = new Row();
            MyCheckboxConfig chk = new MyCheckboxConfig();
            chk.setChecked(true);
            row.setAttribute("pilih", chk);
            row.setAttribute("tagihan", item.tagihan);
            Doublebox nominal = new Doublebox();
            nominal.setValue(item.nominalBayar);
            row.setAttribute("nominal", nominal);
            rows.appendChild(row);
        }
        return rows;
    }

    private List<Tagihan> ambilTagihanTerpilih(List<ItemTagihan> dipilih) {
        List<Tagihan> tags = new ArrayList<Tagihan>();
        for (ItemTagihan item : dipilih) {
            tags.add(item.tagihan);
        }
        return tags;
    }

    // ============================================================ EKSEKUSI: BANK LANGSUNG

    /** BRI(1)/BNI(2)/BSI(3) via overload siswa {@code XxxCommon.onSaveXxx} — persis engine. */
    private void bayarBankLangsung(int type, List<ItemTagihan> dipilih) {
        try {
            List<Tagihan> tags = ambilTagihanTerpilih(dipilih);
            double total = hitungTotalBayar();
            if (type == 1) {
                BriCommon.onSaveBri(siswa, calonSiswa, tags, total, true, 0.0);
            } else if (type == 2) {
                BniCommon.onSaveBni(siswa, calonSiswa, tags, total, true, 0.0);
            } else if (type == 3) {
                BsiCommon.onSaveBsi(siswa, calonSiswa, tags, total, true, 0.0);
            }
            langkah = 5;
            render();
        } catch (Exception ex) {
            alertar("Gagal memproses pembayaran bank: " + ex.getMessage());
        }
    }

    // ============================================================ EKSEKUSI: BTN
    private void bayarBtn(List<ItemTagihan> dipilih, Sekolah sekolah) {
        try {
            double fee = 0;
            try {
                fee = Double.parseDouble(Common.getKonfigurasi("btn_biaya_administrasi", "0.0").getNilai());
            } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/WizardPembayaranSiswaHelper.java:1170"); /* fee opsional */ }
            BankHost bankHost = PembayaranUtil.getInstance()
                    .getBankHost(Common.getKonfigurasi("online_bank_host_ip", "").getNilai(), "Bank Host");
            AkunPembayaranSiswa akun = ambilAkunVa(sekolah);

            VirtualAccountBank va = DownloadTagihanMahasiswaBankBtn.downloadData(siswa, calonSiswa,
                    ambilTagihanTerpilih(dipilih), false, fee, bankHost, akun, sekolah);
            tanganiHasilVa(va, "BAYAR VIA BTN", fee, null, null, sekolah);
        } catch (Exception ex) {
            alertar("Gagal memproses BTN: " + ex.getMessage());
        }
    }

    // ============================================================ EKSEKUSI: VA ONLINE

    /**
     * Eksekutor keluarga VA online siswa (Maja/Bank Online/Bank Online 2/Flip/Smartlink/
     * Finpay/BJB Syariah) — replikasi {@code BaseOnlinePaymentListener.executePayment}:
     * {@link DownloadTagihanSiswaBankOnline#downloadData} dengan BankHost + akun VA per
     * sekolah. Hasil bertautan dibuka di tab/popup; hasil bernomor VA dirender pada
     * langkah Selesai. {@code linkKey} != null menandakan kanal bertautan (finpay →
     * redirect tab baru, lainnya popup).
     */
    private void bayarVaOnline(String label, String ipKey, double fee, String prefixKey,
            Map<String, Object> param, List<ItemTagihan> dipilih, Sekolah sekolah, String linkKey) {
        try {
            BankHost bankHost = PembayaranUtil.getInstance()
                    .getBankHost(Common.getKonfigurasi(ipKey, "").getNilai(), "Bank Host");
            AkunPembayaranSiswa akun = ambilAkunVa(sekolah);

            VirtualAccountBank va = DownloadTagihanSiswaBankOnline.downloadData(siswa, calonSiswa,
                    ambilTagihanTerpilih(dipilih), param, fee, null, null, bankHost, akun, sekolah);

            if (va != null && va.getLink() != null && !va.getLink().trim().isEmpty()) {
                String link = va.getLink().replace("'", "%27");
                if ("finpay".equals(linkKey)) {
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
            tanganiHasilVa(va, label, fee, prefixKey, param, sekolah);
        } catch (Exception ex) {
            alertar("Gagal memproses " + label + ": " + ex.getMessage());
        }
    }

    /**
     * Menyalin hasil {@link VirtualAccountBank} ke kartu langkah Selesai: kode VA dirakit
     * dengan prefix kode bank lain + username kanal/sekolah (pola {@code handleSuccess}
     * engine), gambar QR dibangkitkan ke {@code /report/crcode_<id>.png}.
     */
    private void tanganiHasilVa(VirtualAccountBank va, String label, double fee, String prefixKey,
            Map<String, Object> param, Sekolah sekolah) {
        if (va == null || va.getId() == null) {
            alertar("Transaksi gagal dilakukan. Langkah yang dapat dilakukan: (1) periksa koneksi jaringan; "
                    + "(2) ulangi beberapa saat lagi; (3) hubungi administrator bila tetap gagal.");
            return;
        }
        vaLabelBank = label;
        try {
            String kode = va.getKode() == null ? "" : va.getKode();
            if (prefixKey != null) {
                String prefix = Common.getKonfigurasi(prefixKey, "").getNilai();
                if (prefix != null && !prefix.trim().isEmpty()) {
                    try {
                        if (va.getKanalPembayaran() != null && va.getKanalPembayaran().getBsiUsername() != null
                                && !va.getKanalPembayaran().getBsiUsername().isEmpty()) {
                            kode = va.getKanalPembayaran().getBsiUsername() + kode;
                        } else if (param != null && param.containsKey("maja") && sekolah != null
                                && sekolah.getBsiUsername() != null) {
                            kode = sekolah.getBsiUsername() + kode;
                        }
                    } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/WizardPembayaranSiswaHelper.java:1250"); /* kanal opsional */ }
                    kode = prefix + kode;
                }
            }
            vaKode = kode;
        } catch (Exception e) {
            vaKode = null;
        }
        try {
            vaTotal = formatRp((va.getTotal() == null ? 0 : va.getTotal()) + fee);
        } catch (Exception e) {
            vaTotal = null;
        }
        try {
            vaKadaluarsa = va.getKadaluarsa() == null ? null
                    : Common.dateFormat.get().format(va.getKadaluarsa());
        } catch (Exception e) {
            vaKadaluarsa = null;
        }
        vaQrUrl = null;
        try {
            String isiQr = va.getKode();
            if (isiQr != null && !isiQr.trim().isEmpty()) {
                java.io.File fileQr = new java.io.File(
                        Common.ambilREAL_PATH_REPORT() + "/crcode_" + va.getId() + ".png");
                if (!fileQr.exists()) BarcodeCommon.generateCRCode(isiQr, fileQr);
                vaQrUrl = "/report/crcode_" + va.getId() + ".png";
            }
        } catch (Exception e) {
            vaQrUrl = null;
        }
        langkah = 5;
        render();
    }

    // ============================================================ STEP 5: SELESAI
    private void renderStep5() {
        bodyHost.appendChild(new Html(
            "<div style='text-align:center;padding:24px 20px 8px;'>"
            + "<div style='font-size:56px;margin-bottom:12px;'>✅</div>"
            + "<div style='font-size:20px;font-weight:800;color:#0f172a;margin-bottom:6px;'>Selesai</div>"
            + "<div style='font-size:13px;color:#64748b;max-width:400px;margin:0 auto;line-height:1.6;'>"
            + "Pembayaran telah diproses. Tutup jendela ini atau kembali untuk melakukan pembayaran lain.</div>"
            + "</div>"));

        boolean adaVa = vaKode != null && !vaKode.trim().isEmpty();
        boolean adaQr = vaQrUrl != null && !vaQrUrl.trim().isEmpty();
        if (adaVa || adaQr) {
            StringBuilder sb = new StringBuilder();
            sb.append("<div style='" + CARD_STYLE + "max-width:420px;margin:14px auto 0;"
                    + "background:#f0fdf4;border-color:#bbf7d0;text-align:center;'>");
            sb.append("<div style='font-size:11px;font-weight:700;color:#15803d;letter-spacing:.5px;'>")
              .append(escHtml(vaLabelBank == null ? "VIRTUAL ACCOUNT" : vaLabelBank.toUpperCase()))
              .append("</div>");
            if (adaQr) {
                sb.append("<div style='margin:10px auto;'><img src='").append(escHtml(vaQrUrl))
                  .append("' alt='QR Pembayaran' style='width:220px;max-width:80%;height:auto;"
                        + "border:1px solid #e2e8f0;border-radius:8px;background:#fff;padding:6px;'/></div>");
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
                sb.append("<div style='font-size:12px;color:#b45309;margin-top:4px;'>Bayar sebelum: <b>")
                  .append(escHtml(vaKadaluarsa)).append("</b></div>");
            }
            sb.append("<div style='font-size:11px;color:#64748b;margin-top:8px;'>"
                    + "Ketuk/blok nomor untuk menyalin, atau pindai QR melalui aplikasi bank Anda.</div>");
            sb.append("</div>");
            bodyHost.appendChild(new Html(sb.toString()));
        }
    }

    // ============================================================ UTILS
    private double hitungTotalBayar() {
        double total = 0;
        for (ItemTagihan item : itemTagihans) {
            if (item.dipilih) total += item.totalDitagih();
        }
        return total;
    }

    private List<ItemTagihan> getItemsDipilih() {
        List<ItemTagihan> list = new ArrayList<ItemTagihan>();
        for (ItemTagihan item : itemTagihans) {
            if (item.dipilih) list.add(item);
        }
        return list;
    }

    private String getCurrentUserNama() {
        try {
            Tbmuser u = Common.getCurrentUser();
            if (u == null) u = Common.getTbmuser();
            if (u != null && u.getUserNama() != null) return u.getUserNama();
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/WizardPembayaranSiswaHelper.java:1352"); /* ignore */ }
        return "System";
    }

    private static void alertar(String msg) {
        try {
            MyMessageboxConfig.show(msg, "Info", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/WizardPembayaranSiswaHelper.java:1359"); /* ignore */ }
    }

    private static String formatRp(double v) {
        return "Rp " + String.format("%,.0f", v);
    }

    private static String escHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
