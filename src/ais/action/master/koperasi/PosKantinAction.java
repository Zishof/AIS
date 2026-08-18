package ais.action.master.koperasi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Bandbox;
import org.zkoss.zul.Bandpopup;
import org.zkoss.zul.Button;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Div;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hlayout;
import org.zkoss.zul.Label;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.Radio;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Vlayout;

import ais.action.servlet.api.KantinHelper;
import ais.common.Common;
import ais.database.model.Konfigurasi;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.inventory.Toko;
import ais.database.model.koperasi.AnggotaKoperasi;
import ais.ui.util.DashboardUiKit;
import ais.ui.util.HtmlChartHelper;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Kasir / Point of Sale (POS) Kantin — konversi dari {@code modul/kantin/pos/_pos.jsp} ke ZK.
 *
 * <p>Alur: pilih toko (otomatis terkunci bila login sebagai pedagang) &rarr; cari &amp; tambahkan
 * produk ke keranjang &rarr; (opsional) pilih member untuk diskon/cashback &rarr; pilih cara bayar
 * &rarr; tekan Bayar. Penyimpanan transaksi <b>memakai ulang</b> {@link KantinHelper#bayar}
 * (logika &amp; perhitungan yang sama dengan kasir lama), dipanggil langsung di server.</p>
 *
 * <p>Diskon promo dihitung di server dari {@code AturanDiskon} aktif (mengikuti aturan produk/toko/
 * member yang sama seperti versi JSP). Semua grafik/teks bantu memakai {@link DashboardUiKit}.</p>
 *
 * <p><b>Pembatasan toko:</b> pedagang yang tidak boleh melihat toko lain hanya bisa menjual produk
 * tokonya sendiri (pilihan toko dikunci).</p>
 */
public class PosKantinAction extends GenericAutowireComposer {

    private static final long serialVersionUID = 1L;

    private Div posHost; // wired dari ZUL

    // Kontrol header
    private Combobox cboToko;
    private Bandbox bdMember;
    private Textbox txtCatatan;
    private Textbox txtCariProduk;

    // Area dinamis (tampilan modern berbasis kartu)
    private Div produkBox;
    private Div cartBox;
    private Div metodeBox;
    private Div tunaiBox;
    private Div ringkasanBox;
    private Div kategoriBox;
    private Div invRingkasanBox;
    private Div miniRiwayatBox;
    private Div analitikKasirBox;
    private Div tertahanListBox;
    /** Id {@code koperasi.draft_pembelian_anggota_koperasi} aktif bila keranjang ini hasil "Muat" dari daftar tertahan; null bila keranjang baru. */
    private Long draftIdAktif;
    private Long kategoriFilterId;
    private ais.database.model.koperasi.CaraPembayaranKoperasi caraTerpilih;
    private String strukHeaderCache;
    private double pajakPersen;
    private double grandPajak;
    private Label lblPajak;
    private Label lblSubtotal;
    private Label lblDiskon;
    private Label lblCashback;
    private Label lblTotal;
    private Label lblInfoToko;
    private Label lblKembalian;
    private Label lblMemberSaldo;
    private Label lblBayar;
    private MyDoublebox dbUangBayar;

    // Sesi Kas Kasir (Buka/Tutup Kas) -- widget ringkas di header POS, mesin sama dgn KasKasirZkAction
    // (SesiKasUtil), sehingga kasir tak perlu berpindah menu utk buka/tutup kas & lihat saldo kasnya.
    private String oleh;
    private String olehId;
    private Label lblSesiKas;
    private Label btnSesiKas;
    private Div sesiKasFormBox;

    // Fitur "Top Up Saldo lewat POS" -- tombol+form muncul di kartu Pelanggan HANYA bila member sudah
    // dipilih DAN kasir punya hak Tbmrole.getBolehEntryTopup() (gerbang sama dgn menu Deposit terpisah).
    private boolean bolehTopup;
    private Label lnkTopup;
    private Div topupFormBox;

    // Fitur "Popup Pesanan Online Baru" -- Timer server-side (pola sama dgn Timer polling QRIS di
    // tampilkanQrModal) yg berjalan sepanjang layar Kasir terbuka, memanggil KantinHelper.pesananOnlineBaru
    // LANGSUNG (tanpa round-trip HTTP -- kelas ini SUDAH di server) tiap ~20 detik.
    private Long sejakIdPesananBaru; // null = baseline belum direkam (panggilan pertama TIDAK memicu popup)

    // Tab Riwayat Transaksi
    private Grid grdRiwayat;
    private org.zkoss.zul.Datebox dtMulai;
    private org.zkoss.zul.Datebox dtAkhir;
    private Textbox txtCariPembeli;
    private Textbox txtCariBarang;

    // State
    private Long tokoIdAktif;
    private String namaTokoAktif;
    private boolean bolehTransaksiStokHabis;
    private double grandTotal;
    private double grandCashback;
    private Long memberId;
    private String memberNama;
    private double memberSaldo;
    private double memberMinSaldo;
    private Long memberJenisId;
    private Long memberTipeId;
    private boolean memberWajibPin; // dari JenisAnggotaKoperasi.wajibPin -- gate verifikasi PIN Layar Pelanggan
    private final List<Item> cart = new ArrayList<Item>();
    private List<Rule> rules = new ArrayList<Rule>();

    private Toko scopeToko;       // toko terkunci untuk pedagang; null = admin (boleh pilih)
    private boolean adminBolehPilihToko = true;

    // Nama channel BroadcastChannel client-side utk "Layar Pelanggan" (layar kedua dual-monitor).
    // Unik per desktop ZK (tab/jendela browser kasir) sehingga sesi kasir lain tak saling silang.
    private String custChannelName;

    // Transaksi yg TERTUNDA menunggu verifikasi PIN pembeli di Layar Pelanggan (bila jenis anggota
    // member terpilih wajibPin). Diisi onBayar(), dieksekusi/dibatalkan oleh onPinHasil() atau timeout.
    private JSONObject pendingPayload;
    private String pendingCaraNama;
    private double pendingTotal;
    private boolean pendingOnline;
    private org.zkoss.zul.Timer pinTimeoutTimer;
    private Textbox pinBridge; // komponen tak-terlihat: jembatan event client(zAu.send) -> server

    /** Satu baris keranjang. */
    private static final class Item {
        final Long id;
        final String kode;
        final String nama;
        final double harga;
        final double stok;
        int jumlah;
        double diskon;
        double cashback;
        Long aturanDiskonId;
        boolean berlakuPerHari;
        final boolean izinkanJualMinusStok;

        Item(Long id, String kode, String nama, double harga, double stok, boolean izinkanJualMinusStok) {
            this.id = id;
            this.kode = kode;
            this.nama = nama;
            this.harga = harga;
            this.stok = stok;
            this.izinkanJualMinusStok = izinkanJualMinusStok;
            this.jumlah = 1;
        }
    }

    /**
     * Aturan diskon yang sudah dimaterialkan (nilai mentah, tanpa entitas lazy Hibernate) sehingga
     * aman diakses pada event-event berikutnya tanpa LazyInitializationException.
     */
    private static final class Rule {
        Long aturanId;
        Long produkId;
        Long tokoId;
        Long jenisId;
        Long tipeId;
        boolean berlakuSemua;
        boolean potonganLangsung;
        boolean berlakuPerHari;
        double persen;
        double maxPot;
        double nominal;
        Date tglMulai;
        Date tglSelesai;
        String hariAktif; // gap-closure "Promo Pilih Hari" -- lihat ais.common.HariAktifUtil
        boolean aktivasiManual; // TRUE = dikecualikan dari auto-apply, hanya lewat picker "Promo" manual
        boolean khususMember;
        boolean sumberGrup;
        String jenisMemberJson;
        String tipeMemberJson;
        double cashbackTetap;
        int prioritas = 100;
        boolean dapatDigabung;
        String dasarPerhitungan = "SETELAH_DISKON";
        String grupEksklusif;
        // State pembatasan "berlaku per hari & per toko": pemakaian hari ini (dari DB)
        // dan akumulasi sementara di keranjang saat ini.
        double terpakaiHariIni;
        double terpakaiKeranjang;
    }

    // ======================== Lifecycle ========================

    @Override
    public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
            Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
        Common.doCheckSecurity();
        return super.doBeforeCompose(page, parent, compInfo);
    }

    /**
     * Titik masuk lifecycle ZK: menyiapkan seluruh layar Kasir (POS) begitu komponen selesai
     * dirakit dari ZUL, dipanggil SEKALI per pembukaan halaman.
     *
     * <p>Urutan kerja method ini penting dan saling bergantung, dalam urutan berikut:</p>
     * <ol>
     *   <li>{@link #resolveScopeToko()} — menentukan toko yang berlaku untuk kasir yang login
     *       (pedagang dikunci ke tokonya sendiri; admin-kantin boleh berpindah toko), HARUS
     *       dijalankan sebelum langkah lain karena hampir semua query berikutnya difilter
     *       berdasarkan {@code scopeToko}.</li>
     *   <li>{@link #bacaPajakPersen()} — baca persentase PPN yang berlaku, dipakai perhitungan
     *       total sepanjang sesi kasir.</li>
     *   <li>{@link #loadRules()} — muat aturan diskon yang sedang aktif ke memori (dimaterialkan
     *       sekali per pembukaan halaman, BUKAN dihitung ulang tiap item ditambah ke keranjang).</li>
     *   <li>{@link #buildUI()} — bangun seluruh komponen ZK layar Kasir.</li>
     *   <li>{@link #loadProduk()} — muat katalog produk awal ke grid.</li>
     * </ol>
     *
     * <p><b>Keterbatasan yang diketahui &amp; sengaja tidak ditutupi:</b> POS versi ZK ini
     * SEPENUHNYA server-side (setiap klik memerlukan round-trip ke server) sehingga TIDAK BISA
     * beroperasi saat internet terputus. Untuk kasir yang butuh mode offline (data tersimpan lokal
     * saat internet mati, sinkron otomatis saat pulih -- lihat {@code ais_pos_offline.js}), method
     * ini justru secara eksplisit menyuntikkan tautan pintasan ke POS versi JSP (yang punya
     * kemampuan offline-first itu) lewat tombol mengambang "⚡ POS Mode Offline" -- bukan berusaha
     * membuat versi ZK ini ikut offline.</p>
     *
     * <p><b>Layar Pelanggan (monitor kedua):</b> disiapkan lewat {@code Clients.evalJavaScript(...)}
     * yang menyuntikkan {@code window.__bukaLayarPelangganZK()} + tombol mengambang "🖥 Layar
     * Pelanggan" ke DOM klien, memakai halaman JSP yang SAMA dengan versi JSP POS
     * ({@code layar_pelanggan.jsp}) dan protokol komunikasi client-side yang sama
     * ({@code BroadcastChannel}) -- hanya jembatannya yang beda: versi ZK ini tidak bisa mengakses
     * {@code BroadcastChannel} langsung dari server Java, jadi dijembatani lewat
     * {@code Textbox pinBridge} tersembunyi + event ZK {@code onPinHasil} (pola yang sama seperti
     * {@code StokOpnameScanKantinAction}) untuk menerima balasan verifikasi PIN dari layar kedua.
     * CATATAN: tombol ini saat ini HANYA bisa membuka (bukan toggle buka/tutup seperti versi JSP),
     * dan tidak terintegrasi dengan shell desktop Electron (dual-monitor native) yang sudah dibangun
     * untuk versi JSP -- keduanya di luar cakupan perbaikan sesi ini, dicatat sebagai peluang
     * penyelarasan di masa depan bila POS ZK ini butuh paritas fitur penuh dengan versi JSP.</p>
     *
     * @param comp komponen akar hasil compose ZUL.
     * @throws Exception merambat apa adanya dari kegagalan setup manapun di atas -- tidak ada
     *         fail-safe khusus di sini karena kegagalan pada tahap inisialisasi layar Kasir memang
     *         seharusnya terlihat jelas (halaman gagal terbuka), bukan disembunyikan.
     */
    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        Common.initLaguage();

        resolveScopeToko();
        pajakPersen = bacaPajakPersen();

        // Identitas kasir utk Sesi Kas (SesiKasUtil) -- pola IDENTIK dgn KasKasirZkAction supaya
        // sesi yg dibuka/dicek di sini saling cocok dgn menu "Kas Kasir" terpisah.
        Tbmuser uSesi = Common.getCurrentUser();
        oleh = (uSesi != null && uSesi.getUserNama() != null) ? uSesi.getUserNama()
                : (uSesi == null ? "-" : String.valueOf(uSesi.getUserId()));
        olehId = uSesi == null ? "-" : String.valueOf(uSesi.getUserId());

        // Fitur "Top Up Saldo lewat POS" -- tombol hanya tampil bila kasir punya hak "Boleh Entry
        // Topup" (Tbmrole), gerbang yg SAMA dgn menu "Manajemen Saldo (Deposit)" terpisah.
        ais.database.model.Tbmrole roleTopup = uSesi == null ? null : uSesi.hakAkses();
        bolehTopup = roleTopup != null && roleTopup.getBolehEntryTopup() != null
                && roleTopup.getBolehEntryTopup().booleanValue();

        DashboardUiKit.attachIntro(comp, scopeToko == null ? "Kasir (POS) Kantin" : "Kasir (POS) — " + scopeToko.getNama(),
                "Layar kasir untuk mencatat penjualan: cari produk, masukkan ke keranjang, pilih member bila ada, "
                        + "lalu tekan Bayar. Diskon promo yang sedang aktif dihitung otomatis.");

        loadRules();
        buildUI();
        loadProduk();

        // Catatan arsitektur: POS ZK ini server-side (tiap aksi butuh server) sehingga TIDAK bisa
        // beroperasi offline. Untuk kasir yang memerlukan mode OFFLINE, sediakan pintasan ke POS
        // versi JSP (client-side, offline-first: cache lokal + antrian transaksi + auto-sync).
        try {
            org.zkoss.zk.ui.util.Clients.evalJavaScript(
                    "if(!document.getElementById('posOfflineLink')){var b=document.createElement('a');"
                            + "b.id='posOfflineLink';b.href='" + Common.ROOT + "/baru?p=kantin&s=pos';b.target='_blank';"
                            + "b.title='Buka POS versi JSP yang mendukung mode offline';"
                            + "b.style.cssText='position:fixed;bottom:16px;right:16px;z-index:9999;background:#f59e0b;"
                            + "color:#111;font-weight:700;padding:8px 14px;border-radius:999px;"
                            + "box-shadow:0 2px 10px rgba(0,0,0,.25);text-decoration:none;';"
                            + "b.innerHTML='\\u26A1 POS Mode Offline';document.body.appendChild(b);}");
        } catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/koperasi/PosKantinAction.java:216");
        }

        // "Layar Pelanggan": layar KEDUA utk mesin POS dual-monitor (interaksi dgn pembeli — mirror
        // keranjang/QRIS berjalan, ucapan terima kasih, survey kepuasan). Halaman SAMA dgn versi JSP
        // (modul/kantin/pos/layar_pelanggan.jsp); komunikasi murni client-side via BroadcastChannel,
        // di-drive dari server lewat Clients.evalJavaScript tiap keranjang/QRIS/pembayaran berubah.
        try {
            custChannelName = "pos_layar_pelanggan_zk_" + org.zkoss.zk.ui.Executions.getCurrent().getDesktop().getId();
            String urlLayarPelanggan = Common.ROOT + "/baru?hanya_tampil_jsp=true&p=kantin%2Fpos&s=layar_pelanggan&ch="
                    + custChannelName + (tokoIdAktif != null ? ("&toko=" + tokoIdAktif) : "");

            // Jembatan tak-terlihat: client(zAu.send) -> server, dipakai balasan 'pin_hasil' dari Layar
            // Pelanggan (customer entry PIN sendiri di layar kedua). Pola SAMA persis dgn
            // StokOpnameScanKantinAction (osZkPush_) yg sudah terbukti jalan di codebase ini.
            pinBridge = new Textbox();
            pinBridge.setVisible(false);
            pinBridge.setParent(posHost);
            pinBridge.addEventListener("onPinHasil", new EventListener() {
                @Override
                public void onEvent(Event e) throws Exception {
                    onPinHasilDiterima();
                }
            });
            String pinBridgeUuid = pinBridge.getUuid();

            org.zkoss.zk.ui.util.Clients.evalJavaScript(
                    "window.__posLayarPelangganChannel=('BroadcastChannel' in window)?new BroadcastChannel('" + custChannelName + "'):null;"
                            + "window.__lastPOSKeranjangZK=null;"
                            + "window.__kirimPOSChannel=function(d){try{if(d&&d.tipe==='keranjang')window.__lastPOSKeranjangZK=d;"
                            + "if(window.__posLayarPelangganChannel)window.__posLayarPelangganChannel.postMessage(d);}catch(e){}};"
                            + "if(window.__posLayarPelangganChannel){window.__posLayarPelangganChannel.onmessage=function(ev){"
                            + "var d=ev.data||{};if(d.tipe==='minta_status'&&window.__lastPOSKeranjangZK)"
                            + "window.__posLayarPelangganChannel.postMessage(window.__lastPOSKeranjangZK);"
                            + "if(d.tipe==='survey'&&typeof tampilkanToast==='function')"
                            + "tampilkanToast('Pembeli memberi rating: '+'\\u2605'.repeat(Number(d.rating)||0),'bg-info text-dark');"
                            + "if(d.tipe==='pin_hasil'){try{var w=zk.Widget.$('" + pinBridgeUuid + "');if(w){"
                            + "w.setValue(JSON.stringify({ok:!!d.ok,batal:!!d.batal}));"
                            + "zAu.send(new zk.Event(w,'onPinHasil',null,{toServer:true}));}}catch(e){}}"
                            + "};}"
                            + "window.__bukaLayarPelangganZK=function(){"
                            + "if(window.__posCustWinZK&&!window.__posCustWinZK.closed){window.__posCustWinZK.focus();return;}"
                            + "var left=(window.screen&&window.screen.availWidth)?window.screen.availWidth:0;"
                            + "window.__posCustWinZK=window.open('" + urlLayarPelanggan + "','posLayarPelangganZK',"
                            + "'width=1280,height=800,left='+left+',top=0,menubar=no,toolbar=no,location=no,status=no');};"
                            + "if(!document.getElementById('btnLayarPelangganZK')){var bb=document.createElement('button');"
                            + "bb.id='btnLayarPelangganZK';bb.type='button';bb.onclick=window.__bukaLayarPelangganZK;"
                            + "bb.title='Buka layar tampilan untuk pembeli (monitor kedua)';"
                            + "bb.style.cssText='position:fixed;bottom:16px;right:190px;z-index:9999;background:#0d6efd;"
                            + "color:#fff;font-weight:700;padding:8px 14px;border-radius:999px;border:none;"
                            + "box-shadow:0 2px 10px rgba(0,0,0,.25);cursor:pointer;';"
                            + "bb.innerHTML='\\uD83D\\uDDA5 Layar Pelanggan';document.body.appendChild(bb);}");
        } catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/koperasi/PosKantinAction.java:268");
        }
    }

    /**
     * Dipanggil saat balasan 'pin_hasil' dari Layar Pelanggan tiba (bridge {@link #pinBridge}).
     * Bila PIN benar: lanjutkan transaksi yg TERTUNDA ({@link #pendingPayload}) persis seperti alur
     * normal (QRIS/simpan langsung). Bila salah/dibatalkan/timeout: batalkan, tampilkan pesan.
     */
    private void onPinHasilDiterima() throws Exception {
        String raw = pinBridge == null ? null : pinBridge.getValue();
        if (pinTimeoutTimer != null) {
            pinTimeoutTimer.stop();
            pinTimeoutTimer.setParent(null);
            pinTimeoutTimer = null;
        }
        if (pendingPayload == null) {
            return; // sudah dibatalkan/timeout/tak ada yg tertunda
        }
        boolean ok = false;
        try {
            JSONObject d = new JSONObject(raw == null ? "{}" : raw);
            ok = d.optBoolean("ok", false);
        } catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/koperasi/PosKantinAction.java:291");
        }
        JSONObject payload = pendingPayload;
        String caraNama = pendingCaraNama;
        double total = pendingTotal;
        boolean isOnline = pendingOnline;
        pendingPayload = null;
        pendingCaraNama = null;
        pendingTotal = 0;
        pendingOnline = false;

        if (!ok) {
            MyMessageboxConfig.show("Mohon maaf, verifikasi PIN gagal atau dibatalkan pembeli. Langkah yang dapat dilakukan: (1) minta pembeli memasukkan PIN yang benar; (2) pastikan pembeli mengingat PIN saldo mereka; (3) gunakan metode pembayaran lain jika PIN tidak diketahui.",
                    "PIN Tidak Valid", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
            return;
        }
        if (isOnline) {
            tampilkanQrModal(payload, caraNama, total);
        } else {
            eksekusiBayar(payload, caraNama, total);
        }
    }

    /** Kirim satu event ke Layar Pelanggan (layar kedua) lewat BroadcastChannel client-side. */
    private void broadcastKeLayarPelanggan(JSONObject data) {
        if (custChannelName == null || data == null) {
            return;
        }
        try {
            org.zkoss.zk.ui.util.Clients.evalJavaScript("if(window.__kirimPOSChannel) window.__kirimPOSChannel(" + data.toString() + ");");
        } catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/koperasi/PosKantinAction.java:321");
        }
    }

    private void resolveScopeToko() {
        try {
            Toko ct = Common.getCurrentToko();
            if (ct != null && ct.getId() != null) {
                Boolean b = ct.getBolehMelihatTokolain();
                if (b == null || !b.booleanValue()) {
                    scopeToko = ct;
                    tokoIdAktif = ct.getId();
                    namaTokoAktif = ct.getNama();
                    bolehTransaksiStokHabis = Boolean.TRUE.equals(ct.getBolehTransaksiStokHabis());
                    adminBolehPilihToko = false;
                }
            }
        } catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/koperasi/PosKantinAction.java:337");
        }
    }

    private void loadRules() {
        List<Rule> list = new ArrayList<Rule>();
        try {
            String sql = "SELECT id, produk, toko, jenis_anggota, tipe_anggota, berlaku_semua_member, "
                    + "persentase, maksimal_potongan, nominal, potongan_langsung, berlaku_per_hari_dan_per_toko, "
                    + "tanggal_mulai, tanggal_selesai, hari_aktif, aktivasi_manual,COALESCE(prioritas,100),"
                    + "COALESCE(dapat_digabung,false),COALESCE(dasar_perhitungan,'SETELAH_DISKON'),COALESCE(grup_eksklusif,'') FROM koperasi.aturan_diskon "
                    + "WHERE aktif = true";
            for (Object[] r : rows(sql)) {
                Rule x = new Rule();
                x.aturanId = lng(r[0]);
                x.produkId = lng(r[1]);
                x.tokoId = lng(r[2]);
                x.jenisId = lng(r[3]);
                x.tipeId = lng(r[4]);
                x.berlakuSemua = bool(r[5]);
                x.persen = num(r[6]);
                x.maxPot = num(r[7]);
                x.nominal = num(r[8]);
                x.potonganLangsung = bool(r[9]);
                x.berlakuPerHari = bool(r[10]);
                x.tglMulai = date(r[11]);
                x.tglSelesai = date(r[12]);
                x.hariAktif = str(r[13]);
                x.aktivasiManual = bool(r[14]);
                x.prioritas = ((Number)r[15]).intValue(); x.dapatDigabung=bool(r[16]);
                x.dasarPerhitungan=str(r[17]); x.grupEksklusif=str(r[18]);
                list.add(x);
            }
            String sqlGrup = "SELECT g.id,d.produk,g.toko,g.jenis_anggota,g.tipe_anggota,"
                    + "COALESCE(g.berlaku_semua_member,NOT COALESCE(g.khusus_member,false)),"
                    + "g.persentase,g.maksimal_potongan,g.nominal,COALESCE(g.potongan_langsung,true),"
                    + "g.tanggal_mulai,g.tanggal_selesai,g.hari_aktif,COALESCE(g.khusus_member,false),"
                    + "COALESCE(g.jenis_member_json,'[]'),COALESCE(g.tipe_member_json,'[]'),COALESCE(g.cashback,0),"
                    + "COALESCE(g.prioritas,100),COALESCE(g.dapat_digabung,false),COALESCE(g.dasar_perhitungan,'SETELAH_DISKON'),COALESCE(g.grup_eksklusif,'') "
                    + "FROM koperasi.grup_aturan_diskon g JOIN koperasi.grup_aturan_diskon_detail d "
                    + "ON d.grup_aturan_diskon=g.id AND COALESCE(d.aktif,true) WHERE COALESCE(g.aktif,true)";
            for (Object[] r : rows(sqlGrup)) {
                Rule x = new Rule();
                x.aturanId = lng(r[0]); x.produkId = lng(r[1]); x.tokoId = lng(r[2]);
                x.jenisId = lng(r[3]); x.tipeId = lng(r[4]); x.berlakuSemua = bool(r[5]);
                x.persen = num(r[6]); x.maxPot = num(r[7]); x.nominal = num(r[8]);
                x.potonganLangsung = bool(r[9]); x.tglMulai = date(r[10]); x.tglSelesai = date(r[11]);
                x.hariAktif = str(r[12]); x.khususMember = bool(r[13]);
                x.jenisMemberJson = str(r[14]); x.tipeMemberJson = str(r[15]);
                x.cashbackTetap = num(r[16]); x.sumberGrup = true;
                x.prioritas=((Number)r[17]).intValue(); x.dapatDigabung=bool(r[18]);
                x.dasarPerhitungan=str(r[19]); x.grupEksklusif=str(r[20]);
                list.add(x);
            }
			Collections.sort(list,new Comparator<Rule>(){
				public int compare(Rule a,Rule b){
					if(a.prioritas!=b.prioritas)return a.prioritas>b.prioritas?-1:1;
					if((a.produkId!=null)!=(b.produkId!=null))return a.produkId!=null?-1:1;
					if(a.persen!=b.persen)return a.persen>b.persen?-1:1;
					if(a.nominal!=b.nominal)return a.nominal>b.nominal?-1:1;
					return a.aturanId.compareTo(b.aturanId);
				}
			});
        } catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/koperasi/PosKantinAction.java:364");
        }
        rules = list;
    }

    // ======================== Build UI ========================

    /**
     * Membangun SELURUH pohon komponen ZK layar Kasir (POS) dari nol — method pembangun UI
     * terbesar &amp; terpenting di kelas ini, dipanggil sekali dari {@link #doAfterCompose}.
     *
     * <p>Struktur yang dibangun mengikuti tata letak yang SAMA dengan versi JSP ({@code _pos.jsp})
     * secara sengaja (paritas visual/fungsional antar kedua versi POS): dua tab ("Kasir" untuk
     * transaksi berjalan, "Riwayat Transaksi" untuk histori) di dalam satu {@code Tabbox}, tab
     * Kasir sendiri berisi tata letak dua kolom (kiri: pencarian/kategori/grid produk; kanan:
     * keranjang belanja + ringkasan total + tombol Bayar) plus baris dasbor ringkasan di bawahnya
     * (Ringkasan Hari Ini, Ringkasan Inventori, Mini Riwayat, Analitik Kasir).</p>
     *
     * <p><b>Selalu dipanggil dari kondisi bersih:</b> baris pertama {@code posHost.getChildren().clear()}
     * membuang seluruh komponen lama sebelum membangun ulang — ini membuat method ini AMAN dipanggil
     * lebih dari sekali dalam satu sesi (mis. bila suatu saat perlu re-render penuh setelah
     * perpindahan toko), walau pada alur normal saat ini hanya dipanggil sekali per pembukaan
     * halaman.</p>
     *
     * <p>Method ini murni MERAKIT komponen &amp; memasang event listener — pengisian DATA ke
     * dalamnya (produk, kategori, keranjang, dasbor) didelegasikan ke method loader terpisah
     * ({@link #loadProduk()}, {@link #loadKategori()}, {@link #loadRingkasanHariIni()}, dst.) yang
     * dipanggil belakangan, bukan di dalam method ini — pemisahan ini membuat masing-masing bagian
     * bisa dimuat ulang independen (mis. {@code recompute()} me-refresh keranjang tanpa perlu
     * membangun ulang seluruh UI).</p>
     */
    private void buildUI() {
        posHost.getChildren().clear();
        posHost.appendChild(DashboardUiKit.html(posStyle()));

        // Dua tab seperti versi JSP: "Kasir" (transaksi) dan "Riwayat Transaksi".
        org.zkoss.zul.Tabbox tb = new org.zkoss.zul.Tabbox();
        tb.setWidth("100%");
        tb.setParent(posHost);
        org.zkoss.zul.Tabs tabs = new org.zkoss.zul.Tabs();
        tabs.setParent(tb);
        new org.zkoss.zul.Tab("Kasir").setParent(tabs);
        new org.zkoss.zul.Tab("Riwayat Transaksi").setParent(tabs);
        org.zkoss.zul.Tabpanels panels = new org.zkoss.zul.Tabpanels();
        panels.setParent(tb);
        org.zkoss.zul.Tabpanel kasirPanel = new org.zkoss.zul.Tabpanel();
        kasirPanel.setParent(panels);
        org.zkoss.zul.Tabpanel riwayatPanel = new org.zkoss.zul.Tabpanel();
        riwayatPanel.setParent(panels);

        Div root = new Div();
        root.setSclass("pskasir");
        root.setParent(kasirPanel);

        // ---- Header: toko + cari produk + pelanggan ----
        Div head = new Div();
        head.setSclass("psk-head");
        head.setParent(root);

        Div hToko = new Div();
        hToko.setSclass("psk-store");
        hToko.appendChild(DashboardUiKit.html("<span class='psk-store-ic'>&#128722;</span>"));
        if (adminBolehPilihToko) {
            cboToko = new Combobox();
            cboToko.setWidth("190px");
            cboToko.setReadonly(true);
            cboToko.setTooltiptext("Pilih toko / pedagang");
            Common.insertCombo(cboToko, "nama", Toko.class, Restrictions.eq("aktif", true));
            cboToko.addEventListener("onSelect", new EventListener() {
                @Override
                public void onEvent(Event e) throws Exception {
                    Toko t = (cboToko.getSelectedItem() == null) ? null
                            : (Toko) cboToko.getSelectedItem().getValue();
                    tokoIdAktif = t == null ? null : t.getId();
                    namaTokoAktif = t == null ? "" : t.getNama();
                    bolehTransaksiStokHabis = t != null
                            && Boolean.TRUE.equals(t.getBolehTransaksiStokHabis());
                    loadKategori();
                    loadProduk();
                    updateUsageDiskon();
                    recompute();
                    loadRingkasanHariIni();
                    loadRingkasanInventori();
                    loadMiniRiwayat();
                    loadAnalitikKasir();
                    loadDaftarTertahan();
                    renderSesiKasChip();
                }
            });
            hToko.appendChild(cboToko);
        } else {
            lblInfoToko = new Label(scopeToko.getNama());
            lblInfoToko.setStyle("font-weight:800;color:#0f172a;font-size:14px;");
            hToko.appendChild(lblInfoToko);
        }
        head.appendChild(hToko);
        head.appendChild(buildSesiKasChip());

        Div hSearch = new Div();
        hSearch.setSclass("psk-search");
        hSearch.appendChild(DashboardUiKit.html("<span class='psk-search-ic'>&#128269;</span>"));
        txtCariProduk = new Textbox();
        txtCariProduk.setWidth("100%");
        txtCariProduk.setTooltiptext("Cari produk / SKU / barcode, lalu tekan Enter");
        txtCariProduk.addEventListener("onOK", new EventListener() {
            @Override
            public void onEvent(Event e) throws Exception {
                loadProduk();
            }
        });
        hSearch.appendChild(txtCariProduk);
        head.appendChild(hSearch);

        // Panel Buka/Tutup Kas -- disembunyikan sampai kasir klik chip "Sesi Kas" di header.
        sesiKasFormBox = new Div();
        sesiKasFormBox.setSclass("psk-sesikas-form");
        sesiKasFormBox.setStyle("display:none;");
        sesiKasFormBox.setParent(root);
        renderSesiKasChip();

        // ---- Badan: produk (kiri) + checkout (kanan) ----
        Div bodyx = new Div();
        bodyx.setSclass("psk-body");
        bodyx.setParent(root);

        Div left = new Div();
        left.setSclass("psk-left");
        left.setParent(bodyx);
        left.appendChild(DashboardUiKit.html("<div class='psk-title'>Kategori</div>"));
        kategoriBox = new Div();
        kategoriBox.setSclass("psk-cat");
        kategoriBox.setParent(left);
        left.appendChild(DashboardUiKit.html("<div class='psk-title' style='margin-top:12px;'>Produk</div>"));
        produkBox = new Div();
        produkBox.setSclass("psk-grid");
        produkBox.setParent(left);

        // Ringkasan Hari Ini (strip kartu statistik seperti pada bagian bawah mockup)
        ringkasanBox = new Div();
        ringkasanBox.setSclass("psk-statrow");
        ringkasanBox.setParent(left);

        Div right = new Div();
        right.setSclass("psk-right");
        right.setParent(bodyx);

        // Kartu Pelanggan (pilih member, tampilkan saldo) — seperti panel "Pelanggan" pada mockup
        Div pelangganCard = new Div();
        pelangganCard.setSclass("psk-cust");
        pelangganCard.setParent(right);
        pelangganCard.appendChild(DashboardUiKit.html(
                "<div style='font-weight:800;font-size:13px;margin-bottom:6px;'>Pelanggan</div>"));
        pelangganCard.appendChild(buildMemberBandbox());
        lblMemberSaldo = new Label("");
        lblMemberSaldo.setStyle("font-size:11px;font-weight:700;color:#16a34a;display:block;margin-top:4px;");
        pelangganCard.appendChild(lblMemberSaldo);

        if (bolehTopup) {
            lnkTopup = new Label(ais.common.Common.getBahasaConfig("+ Top Up Saldo"));
            lnkTopup.setSclass("psk-link");
            lnkTopup.setStyle("color:#2563eb;display:none;margin-top:6px;");
            lnkTopup.addEventListener("onClick", new EventListener() {
                @Override
                public void onEvent(Event e) throws Exception {
                    boolean sedangTampil = topupFormBox.getStyle() != null
                            && topupFormBox.getStyle().contains("display:block");
                    if (sedangTampil) {
                        topupFormBox.setStyle("display:none;");
                    } else {
                        renderTopupForm();
                        topupFormBox.setStyle("display:block;");
                    }
                }
            });
            pelangganCard.appendChild(lnkTopup);
            topupFormBox = new Div();
            topupFormBox.setStyle("display:none;margin-top:8px;padding:10px;background:#f8fafc;border:1px dashed #cbd5e1;border-radius:10px;");
            pelangganCard.appendChild(topupFormBox);
        }

        Div cartHead = new Div();
        cartHead.setSclass("psk-cart-head");
        Label lblKeranjang = new Label(ais.common.Common.getBahasaConfig("Keranjang"));
        lblKeranjang.setSclass("psk-title");
        lblKeranjang.setStyle("margin:0;");
        cartHead.appendChild(lblKeranjang);
        Div cartHeadLinks = new Div();
        cartHeadLinks.setStyle("display:flex;gap:10px;");
        Label lihatTertahan = new Label(ais.common.Common.getBahasaConfig("Tertahan"));
        lihatTertahan.setSclass("psk-link");
        lihatTertahan.setStyle("color:#2563eb;");
        lihatTertahan.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event e) throws Exception {
                if (tertahanListBox == null) {
                    return;
                }
                boolean sedangTampil = "block".equals(tertahanListBox.getStyle());
                if (sedangTampil) {
                    tertahanListBox.setStyle("display:none;");
                } else {
                    loadDaftarTertahan();
                    tertahanListBox.setStyle("display:block;");
                }
            }
        });
        cartHeadLinks.appendChild(lihatTertahan);
        Label hapusSemua = new Label(ais.common.Common.getBahasaConfig("Hapus Semua"));
        hapusSemua.setSclass("psk-link");
        hapusSemua.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event e) throws Exception {
                cart.clear();
                draftIdAktif = null;
                recompute();
            }
        });
        cartHeadLinks.appendChild(hapusSemua);
        cartHead.appendChild(cartHeadLinks);
        cartHead.setParent(right);

        tertahanListBox = new Div();
        tertahanListBox.setStyle(
                "display:none;margin:8px 0;padding:8px;background:#f8fafc;border:1px solid #e2e8f0;border-radius:10px;max-height:180px;overflow:auto;");
        tertahanListBox.setParent(right);

        cartBox = new Div();
        cartBox.setSclass("psk-cart");
        cartBox.setParent(right);

        Div sumBox = new Div();
        sumBox.setSclass("psk-sum");
        sumBox.setParent(right);
        lblSubtotal = barisTotal(sumBox, "Subtotal", false);
        lblDiskon = barisTotal(sumBox, "Diskon Promo", false);
        lblCashback = barisTotal(sumBox, "Cashback Didapat", false);
        if (pajakPersen > 0) {
            lblPajak = barisTotal(sumBox, "Pajak (" + fmtPersen(pajakPersen) + "%)", false);
        }
        lblTotal = barisTotal(sumBox, "Total", true);

        tunaiBox = new Div();
        tunaiBox.setStyle("display:none;margin-top:8px;padding:10px;border:1px dashed #cbd5e1;border-radius:10px;");
        tunaiBox.setParent(right);
        Div rowUang = new Div();
        rowUang.setStyle("display:flex;justify-content:space-between;align-items:center;");
        rowUang.setParent(tunaiBox);
        Label lblUang = new Label(ais.common.Common.getBahasaConfig("Uang Diterima"));
        lblUang.setStyle("color:#475569;font-size:12px;");
        lblUang.setParent(rowUang);
        dbUangBayar = new MyDoublebox((Double) null);
        dbUangBayar.setWidth("130px");
        dbUangBayar.addEventListener("onChange", new EventListener() {
            @Override
            public void onEvent(Event e) throws Exception {
                updateKembalian();
            }
        });
        rowUang.appendChild(dbUangBayar);
        lblKembalian = barisTotal(tunaiBox, "Kembalian", false);

        right.appendChild(DashboardUiKit.html("<div class='psk-title' style='margin:12px 0 6px;'>Metode Pembayaran</div>"));
        metodeBox = new Div();
        metodeBox.setSclass("psk-pay");
        metodeBox.setParent(right);
        buildMetode();

        txtCatatan = new Textbox();
        txtCatatan.setWidth("100%");
        txtCatatan.setTooltiptext("Catatan (mis. antar ke meja 5)");
        txtCatatan.setStyle("margin-top:8px;");
        right.appendChild(txtCatatan);

        Div aksiBawah = new Div();
        aksiBawah.setStyle("display:flex;gap:8px;margin-top:12px;");
        aksiBawah.setParent(right);

        Div tahan = new Div();
        tahan.setSclass("psk-tahan");
        tahan.setTooltiptext(
                "Simpan keranjang ini utk dilanjutkan nanti, tanpa membayar sekarang");
        tahan.appendChild(new Label(ais.common.Common.getBahasaConfig("Simpan")));
        tahan.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event e) throws Exception {
                tahanTransaksi();
            }
        });
        tahan.setParent(aksiBawah);

        Div bayar = new Div();
        bayar.setSclass("psk-bayar");
        bayar.setStyle("flex:1;margin-top:0;");
        lblBayar = new Label(ais.common.Common.getBahasaConfig("Bayar"));
        lblBayar.setParent(bayar);
        bayar.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event e) throws Exception {
                onBayar();
            }
        });
        bayar.setParent(aksiBawah);

        // ---- Ringkasan Inventori + Riwayat Transaksi mini (di bawah area kasir, mengikuti mockup) ----
        Div bawah = new Div();
        bawah.setSclass("psk-body");
        bawah.setStyle("margin-top:14px;");
        bawah.setParent(root);

        Div invCard = new Div();
        invCard.setSclass("psk-right");
        invCard.setStyle("flex:1 1 300px;min-width:280px;");
        invCard.setParent(bawah);
        invCard.appendChild(DashboardUiKit.html("<div class='psk-title'>Ringkasan Inventori</div>"
                + "<div style='font-size:11.5px;color:#64748b;margin:-6px 0 10px;'>Menunjukkan berapa banyak "
                + "barang yang tersedia di toko dan barang mana yang stoknya perlu segera ditambah.</div>"));
        invRingkasanBox = new Div();
        invRingkasanBox.setParent(invCard);

        Div riwayatCard = new Div();
        riwayatCard.setSclass("psk-right");
        riwayatCard.setStyle("flex:1 1 320px;min-width:280px;");
        riwayatCard.setParent(bawah);
        riwayatCard.appendChild(DashboardUiKit.html("<div class='psk-title'>Riwayat Transaksi</div>"
                + "<div style='font-size:11.5px;color:#64748b;margin:-6px 0 10px;'>Menampilkan penjualan yang "
                + "baru saja terjadi, supaya kasir bisa cepat memeriksa transaksi terakhir tanpa membuka halaman lain.</div>"));
        miniRiwayatBox = new Div();
        miniRiwayatBox.setParent(riwayatCard);

        Div analitikCard = new Div();
        analitikCard.setSclass("psk-right");
        analitikCard.setStyle("flex:1 1 320px;min-width:280px;");
        analitikCard.setParent(bawah);
        analitikCard.appendChild(DashboardUiKit.html("<div class='psk-title'>Analitik Kasir</div>"
                + "<div style='font-size:11.5px;color:#64748b;margin:-6px 0 10px;'>Membandingkan penjualan minggu "
                + "ini dengan minggu lalu per kategori barang dan per ukuran keberhasilan toko.</div>"));
        analitikKasirBox = new Div();
        analitikKasirBox.setParent(analitikCard);

        recompute();
        loadRingkasanHariIni();
        loadKategori();
        loadRingkasanInventori();
        loadMiniRiwayat();
        loadAnalitikKasir();
        loadDaftarTertahan();

        buildRiwayat(riwayatPanel);

        // Fitur "Popup Pesanan Online Baru" -- Timer berjalan sepanjang layar Kasir terbuka.
        sejakIdPesananBaru = null; // reset baseline tiap buildUI() (mis. saat toko berpindah)
        org.zkoss.zul.Timer timerPesananBaru = new org.zkoss.zul.Timer();
        timerPesananBaru.setDelay(20000);
        timerPesananBaru.setRepeats(true);
        timerPesananBaru.setParent(posHost);
        timerPesananBaru.addEventListener("onTimer", new EventListener() {
            @Override
            public void onEvent(Event e) throws Exception {
                cekPesananOnlineBaru();
            }
        });
    }

    /**
     * Fitur "Popup Pesanan Online Baru" -- dipanggil berkala oleh Timer di {@link #buildUI}, memanggil
     * {@link KantinHelper#pesananOnlineBaru} LANGSUNG (bukan lewat HTTP -- kelas ini sendiri sudah
     * jalan di server) supaya kasir langsung tahu begitu pembeli checkout online lewat
     * {@code toko_online.jsp}. Lihat javadoc {@link KantinHelper#pesananOnlineBaru} utk penjelasan
     * lengkap cara membedakan pesanan online dari keranjang yang ditahan kasir sendiri.
     */
    private void cekPesananOnlineBaru() {
        try {
            JSONObject req = new JSONObject();
            req.put("id_toko", tokoIdAktif == null ? JSONObject.NULL : tokoIdAktif);
            req.put("sejak_id", sejakIdPesananBaru == null ? 0 : sejakIdPesananBaru.longValue());
            JSONObject hasil = new JSONObject();
            ais.action.servlet.api.KantinHelper.pesananOnlineBaru(req, hasil);
            if (!"00".equals(hasil.optString("status", ""))) {
                return;
            }
            boolean adalahBaseline = (sejakIdPesananBaru == null);
            sejakIdPesananBaru = Long.valueOf(hasil.optLong("maksId", 0));
            if (adalahBaseline) {
                return; // panggilan pertama: rekam baseline saja, jangan tampilkan popup
            }
            JSONArray daftar = hasil.optJSONArray("pesanan");
            if (daftar == null || daftar.length() == 0) {
                return;
            }
            StringBuilder pesan = new StringBuilder();
            for (int i = 0; i < daftar.length(); i++) {
                JSONObject p = daftar.getJSONObject(i);
                if (pesan.length() > 0) {
                    pesan.append("\n\n");
                }
                pesan.append(p.optString("pembeli", "-")).append(" -- ").append(p.optString("waktu", ""))
                        .append("\n").append(p.optString("barang", "-"));
            }
            MyMessageboxConfig.show(pesan.toString(),
                    "Pesanan Online Baru (" + daftar.length() + ")",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
        } catch (Exception ignore) {
            ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/koperasi/PosKantinAction.java:cekPesananOnlineBaru");
        }
    }

    private Bandbox buildMemberBandbox() {
        bdMember = new Bandbox();
        bdMember.setReadonly(true);
        bdMember.setWidth("100%");
        bdMember.setTooltiptext("Umum (klik untuk pilih member)");

        Bandpopup bp = new Bandpopup();
        bp.setParent(bdMember);
        bp.setWidth("320px");

        Vlayout v = new Vlayout();
        v.setStyle("padding:6px;");
        v.setParent(bp);

        Hlayout h = new Hlayout();
        h.setStyle("gap:6px;");
        h.setParent(v);
        final Textbox txtCari = new Textbox();
        txtCari.setWidth("210px");
        txtCari.setTooltiptext("nama / ID member");
        h.appendChild(txtCari);
        Button bCari = new Button("Cari");
        h.appendChild(bCari);

        final Listbox lst = new Listbox();
        lst.setWidth("300px");
        lst.setHeight("220px");
        lst.setStyle("margin-top:6px;");
        lst.setParent(v);
        org.zkoss.zul.Listhead lh = new org.zkoss.zul.Listhead();
        lh.setParent(lst);
        new org.zkoss.zul.Listheader("Nama").setParent(lh);
        new org.zkoss.zul.Listheader("ID").setParent(lh);

        EventListener doSearch = new EventListener() {
            @Override
            public void onEvent(Event e) throws Exception {
                cariMember(txtCari.getValue(), lst);
            }
        };
        bCari.addEventListener("onClick", doSearch);
        txtCari.addEventListener("onOK", doSearch);

        lst.addEventListener("onSelect", new EventListener() {
            @Override
            public void onEvent(Event e) throws Exception {
                Listitem it = lst.getSelectedItem();
                if (it != null && it.getValue() != null) {
                    pilihMember((Long) it.getValue());
                    bdMember.close();
                }
            }
        });

        Button bUmum = new Button("Tanpa Member (Umum)");
        bUmum.setStyle("margin-top:6px;");
        bUmum.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event e) throws Exception {
                resetMemberState();
                bdMember.close();
                buildMetode();
                updateUsageDiskon();
                recompute();
            }
        });
        bUmum.setParent(v);

        return bdMember;
    }

    /**
     * Membangun chip ringkas "Sesi Kas" di header POS (Fitur "Sesi Kasir"): menampilkan status kas
     * (Tertutup / Terbuka + kas saat ini) dan satu tombol yang men-toggle panel Buka/Tutup Kas
     * ({@link #sesiKasFormBox}) tepat di bawah header, tanpa perlu berpindah menu ke "Kas Kasir"
     * terpisah. Isi label diisi belakangan oleh {@link #renderSesiKasChip}.
     */
    private Div buildSesiKasChip() {
        Div h = new Div();
        h.setSclass("psk-sesikas");
        lblSesiKas = new Label("");
        h.appendChild(lblSesiKas);
        btnSesiKas = new Label("");
        btnSesiKas.setSclass("psk-link");
        btnSesiKas.setStyle("color:#2563eb;");
        btnSesiKas.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event e) throws Exception {
                boolean sedangTampil = sesiKasFormBox.getStyle() != null
                        && sesiKasFormBox.getStyle().contains("display:block");
                if (sedangTampil) {
                    sesiKasFormBox.setStyle("display:none;");
                } else {
                    renderSesiKasForm();
                    sesiKasFormBox.setStyle("display:block;");
                }
            }
        });
        h.appendChild(btnSesiKas);
        return h;
    }

    /**
     * Menyegarkan isi chip "Sesi Kas" (label status + label tombol) sesuai sesi kas TERBUKA milik
     * kasir ini saat ini (query ulang tiap dipanggil — dipanggil setiap kali toko berpindah dan
     * setiap kali sesi dibuka/ditutup). Aman dipanggil sebelum chip terbentuk (no-op bila
     * {@link #lblSesiKas} belum ada).
     */
    private void renderSesiKasChip() {
        if (lblSesiKas == null) {
            return;
        }
        try {
            ais.database.model.inventory.SesiKasKasir sesi = ais.action.master.koperasi.helper.SesiKasUtil
                    .sesiTerbukaPerangkat(HibernateUtil.currentSession(), oleh, olehId, tokoIdAktif,
							idPerangkatZk());
            if (sesi == null) {
                lblSesiKas.setValue(Common.getBahasaConfig("Kas: Tertutup"));
                lblSesiKas.setStyle("font-size:11.5px;font-weight:700;color:#dc2626;");
                btnSesiKas.setValue(Common.getBahasaConfig("Buka Kas"));
            } else {
                double[] jual = ais.action.master.koperasi.helper.SesiKasUtil.hitungPenjualan(
                        HibernateUtil.currentSession(), sesi, new Date());
                double kasSaatIni = (sesi.getModalAwal() == null ? 0.0 : sesi.getModalAwal().doubleValue()) + jual[0];
                String waktu = new java.text.SimpleDateFormat("dd-MM HH:mm").format(sesi.getWaktuBuka());
                lblSesiKas.setValue(Common.getBahasaConfig("Kas") + ": Rp " + DashboardUiKit.money(kasSaatIni)
                        + " · " + Common.getBahasaConfig("sejak") + " " + waktu);
                lblSesiKas.setStyle("font-size:11.5px;font-weight:700;color:#16a34a;");
                btnSesiKas.setValue(Common.getBahasaConfig("Tutup Kas"));
            }
        } catch (Exception ignore) {
            lblSesiKas.setValue("Kas: -");
            ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/koperasi/PosKantinAction.java:renderSesiKasChip");
        }
    }

    /**
     * Menggambar ulang isi {@link #sesiKasFormBox}: form "Buka Kas" (modal awal + keterangan) bila
     * belum ada sesi terbuka, atau form "Tutup Kas" (KPI penjualan berjalan + uang fisik +
     * keterangan) bila sudah ada — pola identik dengan {@code KasKasirZkAction#render()} supaya
     * perhitungan (selisih = uang fisik − (modal awal + tunai)) konsisten di kedua tempat.
     */
    private void renderSesiKasForm() {
        sesiKasFormBox.getChildren().clear();
        Session session = HibernateUtil.currentSession();
        final ais.database.model.inventory.SesiKasKasir sesi = ais.action.master.koperasi.helper.SesiKasUtil
                .sesiTerbukaPerangkat(session, oleh, olehId, tokoIdAktif, idPerangkatZk());

        if (sesi == null) {
            Label l = new Label(Common.getBahasaConfig("Kas belum dibuka. Isi modal awal untuk memulai sesi kas."));
            l.setStyle("font-weight:700;color:#64748b;display:block;margin-bottom:8px;font-size:12px;");
            sesiKasFormBox.appendChild(l);

            Hlayout h = new Hlayout();
            h.setStyle("gap:10px;align-items:flex-end;flex-wrap:wrap;");
            h.setParent(sesiKasFormBox);
            Vlayout c1 = new Vlayout();
            c1.setParent(h);
            new Label(Common.getBahasaConfig("Modal Awal (Rp)")).setParent(c1);
            final org.zkoss.zul.Decimalbox modal = new org.zkoss.zul.Decimalbox(java.math.BigDecimal.ZERO);
            modal.setWidth("140px");
            modal.setParent(c1);
            Vlayout c2 = new Vlayout();
            c2.setParent(h);
            new Label(Common.getBahasaConfig("Keterangan")).setParent(c2);
            final Textbox ket = new Textbox();
            ket.setWidth("200px");
            ket.setParent(c2);
            Button buka = new Button(Common.getBahasaConfig("Buka Kas"));
            buka.setStyle("background:#16a34a;color:#fff;");
            buka.addEventListener("onClick", new EventListener() {
                @Override
                public void onEvent(Event e) throws Exception {
                    if (tokoIdAktif == null) {
                        MyMessageboxConfig.show("Mohon maaf, toko/pedagang belum dipilih. Langkah yang dapat dilakukan: (1) pilih toko/pedagang dari daftar di bagian atas layar; (2) pastikan toko sudah aktif; (3) ulangi proses membuka kas.",
                                "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
                        return;
                    }
                    double m = modal.getValue() == null ? 0 : modal.getValue().doubleValue();
                    Toko t = (Toko) HibernateUtil.currentSession().get(Toko.class, tokoIdAktif);
					if (ais.action.master.koperasi.helper.SesiKasUtil.sesiTerbuka(
							HibernateUtil.currentSession(), oleh, olehId, null) != null
							|| ais.action.master.koperasi.helper.SesiKasUtil.sesiTerbukaPadaPerangkat(
									HibernateUtil.currentSession(), null, idPerangkatZk()) != null) {
						MyMessageboxConfig.show(
								"Akun atau perangkat ini masih mempunyai sesi kas terbuka. Tutup sesi lama sebelum membuka sesi baru.",
								"Sesi Kas Masih Aktif", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
						return;
					}
                    ais.action.master.koperasi.helper.SesiKasUtil.buka(
                            HibernateUtil.currentSession(), t, oleh, olehId, m, ket.getValue(), null, null,
							idPerangkatZk(), "ZK Web / " + idPerangkatZk());
                    renderSesiKasChip();
                    sesiKasFormBox.setStyle("display:none;");
                }
            });
            buka.setParent(h);
        } else {
            double[] jual = ais.action.master.koperasi.helper.SesiKasUtil.hitungPenjualan(
                    session, sesi, new Date());
            final double seharusnya = (sesi.getModalAwal() == null ? 0.0 : sesi.getModalAwal().doubleValue()) + jual[0];

            Label l = new Label(Common.getBahasaConfig("Kas Terbuka sejak")
                    + " " + new java.text.SimpleDateFormat("dd-MM-yyyy HH:mm").format(sesi.getWaktuBuka()));
            l.setStyle("font-weight:700;color:#16a34a;display:block;margin-bottom:8px;font-size:12px;");
            sesiKasFormBox.appendChild(l);

            Hlayout info = new Hlayout();
            info.setStyle("gap:20px;flex-wrap:wrap;margin-bottom:10px;");
            info.setParent(sesiKasFormBox);
            sesiKasKpi(info, "Modal Awal", sesi.getModalAwal().doubleValue(), "#0f172a");
            sesiKasKpi(info, "Penjualan Tunai", jual[0], "#16a34a");
            sesiKasKpi(info, "Non Tunai", jual[1], "#0d6efd");
            sesiKasKpi(info, "Kas Seharusnya", seharusnya, "#0f172a");

            Hlayout h = new Hlayout();
            h.setStyle("gap:10px;align-items:flex-end;flex-wrap:wrap;");
            h.setParent(sesiKasFormBox);
            Vlayout c1 = new Vlayout();
            c1.setParent(h);
            new Label(Common.getBahasaConfig("Uang Fisik (Rp)")).setParent(c1);
            final org.zkoss.zul.Decimalbox uf = new org.zkoss.zul.Decimalbox(
                    new java.math.BigDecimal(Math.round(seharusnya)));
            uf.setWidth("140px");
            uf.setParent(c1);
            Vlayout c2 = new Vlayout();
            c2.setParent(h);
            new Label(Common.getBahasaConfig("Keterangan")).setParent(c2);
            final Textbox ket = new Textbox();
            ket.setWidth("200px");
            ket.setParent(c2);
            Button tutup = new Button(Common.getBahasaConfig("Tutup Kas"));
            tutup.setStyle("background:#dc2626;color:#fff;");
            tutup.addEventListener("onClick", new EventListener() {
                @Override
                public void onEvent(Event e) throws Exception {
                    final double uang = uf.getValue() == null ? 0 : uf.getValue().doubleValue();
                    final String keter = ket.getValue();
                    MyMessageboxConfig.show(
                            "Apakah Bapak/Ibu yakin ingin menutup kas kasir sekarang? Setelah kas ditutup, sesi kas "
                                    + "yang sedang berjalan akan diakhiri dan selisih terhadap kas seharusnya akan "
                                    + "dicatat secara permanen.",
                            "Konfirmasi", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
                            MyMessageboxConfig.QUESTION, new EventListener() {
                                @Override
                                public void onEvent(Event ev) throws Exception {
                                    if (new Integer(ev.getData().toString()).intValue() == MyMessageboxConfig.OK) {
                                        ais.action.master.koperasi.helper.SesiKasUtil.tutup(
                                                HibernateUtil.currentSession(), sesi, uang, keter);
                                        org.json.JSONObject laporan = ais.action.master.koperasi.helper.SesiKasUtil
                                                .laporanTersimpanAtauHitung(HibernateUtil.currentSession(), sesi);
                                        MyMessageboxConfig.show(
                                                ais.action.master.koperasi.helper.SesiKasUtil.laporanTeks(laporan),
                                                "Laporan Tutup Kas", MyMessageboxConfig.OK,
                                                MyMessageboxConfig.INFORMATION);
                                        renderSesiKasChip();
                                        sesiKasFormBox.setStyle("display:none;");
                                    }
                                }
                            });
                }
            });
            tutup.setParent(h);
        }
    }

    private void sesiKasKpi(Hlayout parent, String label, double val, String color) {
        Vlayout v = new Vlayout();
        v.setParent(parent);
        Label l = new Label(Common.getBahasaConfig(label));
        l.setStyle("font-size:10.5px;color:#64748b;text-transform:uppercase;font-weight:700;display:block;");
        l.setParent(v);
        Label n = new Label(DashboardUiKit.money(val));
        n.setStyle("font-size:16px;font-weight:800;color:" + color + ";");
        n.setParent(v);
    }

    /**
     * Menggambar ulang form "Top Up Saldo" (nominal + keterangan + tombol) ke dalam
     * {@link #topupFormBox} untuk member yang SEDANG terpilih -- dipanggil setiap kali kasir klik
     * chip "+ Top Up Saldo" (lihat {@link #buildUI}). Menulis lewat
     * {@link KantinHelper#topupSaldo} (method bertipe eksplisit, BUKAN reflection {@code
     * simpanDataRinci} yang dipakai layar admin {@code _manajemen_topup.jsp}) -- gerbang otorisasi
     * (hak kasir + jenis keanggotaan member) sepenuhnya di server, di sini murni UI.
     */
    private void renderTopupForm() {
        topupFormBox.getChildren().clear();
        if (memberId == null) {
            return;
        }
        final Long idMemberSaatIni = memberId;
        Hlayout h = new Hlayout();
        h.setStyle("gap:10px;align-items:flex-end;flex-wrap:wrap;");
        h.setParent(topupFormBox);
        Vlayout c1 = new Vlayout();
        c1.setParent(h);
        new Label(Common.getBahasaConfig("Nominal Top Up (Rp)")).setParent(c1);
        final org.zkoss.zul.Decimalbox nominal = new org.zkoss.zul.Decimalbox(java.math.BigDecimal.ZERO);
        nominal.setWidth("140px");
        nominal.setParent(c1);
        Vlayout c2 = new Vlayout();
        c2.setParent(h);
        new Label(Common.getBahasaConfig("Keterangan")).setParent(c2);
        final Textbox ket = new Textbox();
        ket.setWidth("200px");
        ket.setParent(c2);
        Button simpan = new Button(Common.getBahasaConfig("Top Up"));
        simpan.setStyle("background:#16a34a;color:#fff;");
        simpan.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event e) throws Exception {
                double n = nominal.getValue() == null ? 0 : nominal.getValue().doubleValue();
                if (n <= 0) {
                    MyMessageboxConfig.show("Mohon maaf, nominal top up harus lebih dari 0. Langkah yang dapat dilakukan: (1) isi kolom Nominal dengan angka lebih dari nol; (2) periksa kembali nilai yang dimasukkan; (3) ulangi proses top up.", "Peringatan",
                            MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
                    return;
                }
                JSONObject payload = new JSONObject();
                payload.put("id_member", idMemberSaatIni);
                payload.put("nominal", n);
                payload.put("keterangan", ket.getValue() == null ? "" : ket.getValue());
                JSONObject hasil = new JSONObject();
                ais.action.servlet.api.KantinHelper.topupSaldo(Common.getCurrentUser(), payload, hasil);
                if ("00".equals(hasil.optString("status", ""))) {
                    topupFormBox.setStyle("display:none;margin-top:8px;padding:10px;background:#f8fafc;border:1px dashed #cbd5e1;border-radius:10px;");
                    if (idMemberSaatIni.equals(memberId)) {
                        pilihMember(idMemberSaatIni); // segarkan label saldo dgn angka terbaru
                    }
                    MyMessageboxConfig.show("Top up berhasil disimpan.", "Berhasil",
                            MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
                } else {
                    MyMessageboxConfig.show(hasil.optString("description", "Gagal melakukan top up."), "Gagal",
                            MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
                }
            }
        });
        h.appendChild(simpan);
    }

    /**
     * Menyetel ulang seluruh state member/pelanggan yang sedang dipilih ke kondisi "Umum" (transaksi
     * tanpa member), sekaligus mengosongkan tampilan kartu member dan label saldo.
     *
     * <p>Diekstrak karena logika ini SEBELUMNYA ditulis ulang terpisah di 3 tempat (tombol
     * "Tanpa Member", {@link #eksekusiBayar()} setelah bayar sukses, dan {@link #tahanTransaksi()}
     * setelah simpan sukses) -- salah satunya ({@code tahanTransaksi()}) ternyata hanya mereset 2 dari
     * 7 field (bug nyata: {@code memberSaldo}/{@code memberMinSaldo}/{@code memberJenisId}/
     * {@code memberTipeId}/{@code memberWajibPin} dan label saldo tetap basi menampilkan data member
     * SEBELUMNYA setelah kasir menekan "Tahan"). Menyatukan jadi satu method menutup celah itu
     * sekaligus mencegah ketiganya diam-diam berbeda lagi di masa depan.</p>
     *
     * <p>SENGAJA tidak memanggil {@code buildMetode()}/{@code recompute()} sendiri -- daftar metode
     * pembayaran dan total transaksi punya kondisi pemicu refresh yang berbeda-beda di tiap pemanggil
     * (mis. {@code tahanTransaksi()} mengosongkan seluruh keranjang sehingga total otomatis nol,
     * sedangkan tombol "Tanpa Member" TIDAK mengosongkan keranjang), jadi urutan/perlu-tidaknya
     * refresh tambahan itu tetap keputusan pemanggil, bukan tanggung jawab method reset state ini.</p>
     */
    private void resetMemberState() {
        memberId = null;
        memberNama = null;
        memberSaldo = 0;
        memberMinSaldo = 0;
        memberJenisId = null;
        memberTipeId = null;
        memberWajibPin = false;
        if (bdMember != null) {
            bdMember.setValue("");
        }
        if (lblMemberSaldo != null) {
            lblMemberSaldo.setValue("");
        }
        if (lnkTopup != null) {
            lnkTopup.setStyle("color:#2563eb;display:none;margin-top:6px;");
        }
        if (topupFormBox != null) {
            topupFormBox.setStyle("display:none;margin-top:8px;padding:10px;background:#f8fafc;border:1px dashed #cbd5e1;border-radius:10px;");
        }
    }

    // ======================== Data loaders ========================

    /**
     * Memuat pilihan cara pembayaran. Bila member terpilih dan jenis keanggotaannya membatasi metode
     * (kolom {@code daftar_cara_pembayaran_yang_boleh_di_pilih}), hanya metode yang diizinkan yang
     * ditampilkan — sama seperti versi JSP. Tanpa member / tanpa batasan: semua metode aktif.
     */
    private void buildMetode() {
        if (metodeBox == null) {
            return;
        }
        metodeBox.getChildren().clear();
        caraTerpilih = null;
        if (tunaiBox != null) {
            tunaiBox.setStyle("display:none;margin-top:8px;padding:10px;border:1px dashed #cbd5e1;border-radius:10px;");
        }
        List<ais.database.model.koperasi.CaraPembayaranKoperasi> list = caraBayarList();
        if (list.isEmpty()) {
            metodeBox.appendChild(DashboardUiKit.html(
                    "<div class='psk-empty'>Tidak ada metode pembayaran tersedia.</div>"));
            return;
        }
        for (final ais.database.model.koperasi.CaraPembayaranKoperasi c : list) {
            final Div b = new Div();
            b.setSclass("psk-paybtn");
            b.appendChild(DashboardUiKit.html("<div class='psk-pay-ic'>" + iconFor(c.getNama()) + "</div><div>"
                    + DashboardUiKit.esc(c.getNama()) + "</div>"));
            b.addEventListener("onClick", new EventListener() {
                @Override
                public void onEvent(Event e) throws Exception {
                    pilihMetode(c, b);
                }
            });
            b.setParent(metodeBox);
        }
        if (list.size() == 1) {
            pilihMetode(list.get(0), (Div) metodeBox.getChildren().get(0));
        }
    }

    /** Set metode terpilih + sorot tombolnya; tampilkan kotak uang tunai hanya untuk metode tunai. */
    private void pilihMetode(ais.database.model.koperasi.CaraPembayaranKoperasi c, Div btn) {
        caraTerpilih = c;
        for (Object o : metodeBox.getChildren()) {
            if (o instanceof Div) {
                ((Div) o).setSclass("psk-paybtn");
            }
        }
        if (btn != null) {
            btn.setSclass("psk-paybtn sel");
        }
        String nm = c.getNama() == null ? "" : c.getNama().toLowerCase();
        boolean tunai = nm.contains("tunai") || nm.contains("cash");
        if (tunaiBox != null) {
            tunaiBox.setStyle((tunai ? "display:block" : "display:none")
                    + ";margin-top:8px;padding:10px;border:1px dashed #cbd5e1;border-radius:10px;");
        }
        updateKembalian();
    }

    /** Daftar cara pembayaran aktif, disaring sesuai jenis anggota bila member dipilih (sama spt JSP). */
    private List<ais.database.model.koperasi.CaraPembayaranKoperasi> caraBayarList() {
        List<ais.database.model.koperasi.CaraPembayaranKoperasi> out =
                new ArrayList<ais.database.model.koperasi.CaraPembayaranKoperasi>();
        String allowed = null;
        if (memberJenisId != null) {
            for (Object[] r : rows("SELECT daftar_cara_pembayaran_yang_boleh_di_pilih "
                    + "FROM koperasi.jenis_anggota_koperasi WHERE id = " + memberJenisId)) {
                allowed = str(r[0]);
            }
        }
        String idSql;
        if (allowed == null || allowed.trim().isEmpty()) {
            idSql = "SELECT id FROM koperasi.cara_pembayaran_koperasi WHERE aktif = true ORDER BY nama ASC";
        } else {
            String csv = allowed.replace("'", "''");
            idSql = "SELECT cpk.id FROM koperasi.cara_pembayaran_koperasi cpk WHERE cpk.aktif = true AND '"
                    + csv + "' LIKE '%,' || cpk.id || ',%' ORDER BY cpk.nama ASC";
        }
        for (Object[] r : rows(idSql)) {
            Long id = lng(r[0]);
            ais.database.model.koperasi.CaraPembayaranKoperasi c =
                    (ais.database.model.koperasi.CaraPembayaranKoperasi) HibernateUtil.currentSession()
                            .get(ais.database.model.koperasi.CaraPembayaranKoperasi.class, id);
            if (c != null) {
                out.add(c);
            }
        }
        return out;
    }

    private static String iconFor(String nama) {
        String n = nama == null ? "" : nama.toLowerCase();
        if (n.contains("tunai") || n.contains("cash")) return "&#128181;";
        if (n.contains("qris") || n.contains("qr")) return "&#128241;";
        if (n.contains("debit")) return "&#128179;";
        if (n.contains("kredit") || n.contains("credit")) return "&#128179;";
        if (n.contains("wallet") || n.contains("dompet") || n.contains("ovo") || n.contains("gopay")
                || n.contains("dana")) return "&#128091;";
        if (n.contains("saldo") || n.contains("deposit") || n.contains("tabungan")) return "&#127974;";
        if (n.contains("online") || n.contains("transfer")) return "&#127760;";
        return "&#128179;";
    }

    /** Bangun chip kategori (jenis produk) untuk toko aktif; "Semua" + tiap jenis. */
    private void loadKategori() {
        if (kategoriBox == null) {
            return;
        }
        kategoriBox.getChildren().clear();
        kategoriFilterId = null;
        tambahChipKategori(null, "Semua", true);
        if (tokoIdAktif == null) {
            return;
        }
        for (Object[] r : rows("SELECT DISTINCT jp.id, jp.nama FROM koperasi.produk p "
                + "JOIN koperasi.jenis_produk jp ON jp.id = p.jenis_produk "
                + "WHERE p.aktif = true AND p.toko = " + tokoIdAktif + " ORDER BY jp.nama ASC")) {
            tambahChipKategori(lng(r[0]), str(r[1]), false);
        }
    }

    private void tambahChipKategori(final Long id, String nama, boolean aktif) {
        final Div chip = new Div();
        chip.setSclass(aktif ? "psk-cat-chip sel" : "psk-cat-chip");
        chip.appendChild(DashboardUiKit.html(DashboardUiKit.esc(nama)));
        chip.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event e) throws Exception {
                kategoriFilterId = id;
                for (Object o : kategoriBox.getChildren()) {
                    if (o instanceof Div) {
                        ((Div) o).setSclass("psk-cat-chip");
                    }
                }
                chip.setSclass("psk-cat-chip sel");
                loadProduk();
            }
        });
        chip.setParent(kategoriBox);
    }

    private static final String[] PSK_ICON = { "&#127858;", "&#9749;", "&#127831;", "&#127853;", "&#127839;",
            "&#129482;", "&#127828;", "&#129385;" };

    /**
     * Memuat/menyegarkan grid katalog produk yang tampil di kolom kiri layar Kasir, terfilter
     * menurut kategori yang sedang dipilih dan kata kunci pencarian bila ada.
     *
     * <p>Dipanggil di banyak titik pemicu berbeda: sekali saat halaman pertama dibuka
     * ({@link #doAfterCompose}), tiap kali kasir mengetik di kotak pencarian/pindah kategori, dan
     * (secara tidak langsung lewat method lain) setelah checkout berhasil supaya stok yang baru
     * berkurang langsung terlihat di kartu produk. Selalu membuang isi {@code produkBox} lama lebih
     * dulu ({@code getChildren().clear()}) sebelum merender ulang — sederhana &amp; aman dipanggil
     * berkali-kali, walau berarti tidak ada optimisasi "render selisih" (diff rendering); untuk
     * ukuran katalog kantin yang wajar (puluhan-ratusan produk per toko), ini tidak terasa lambat.</p>
     *
     * <p>Bila {@code tokoIdAktif} belum ada (kasir/admin belum memilih toko), grid diisi pesan
     * pengantar alih-alih query kosong yang membingungkan — mencegah tampilan "tidak ada produk"
     * yang seolah-olah toko itu memang tidak punya produk sama sekali.</p>
     */
    private void loadProduk() {
        if (produkBox == null) {
            return;
        }
        produkBox.getChildren().clear();
        if (tokoIdAktif == null) {
            produkBox.appendChild(DashboardUiKit.html(
                    "<div class='psk-empty'>Silakan pilih toko / pedagang terlebih dahulu.</div>"));
            return;
        }
        String kw = txtCariProduk == null ? "" : txtCariProduk.getValue().trim().replace("'", "''");
        // Gap-closure "Jenis Item" (Produk vs Bahan Baku/Ekstra) -- bahan baku & ekstra TIDAK boleh
        // dijual langsung lewat Kasir sbg baris mandiri. `<>` polos TIDAK match NULL di Postgres
        // (produk lama sebelum kolom ini ada), jadi WAJIB pola OR IS NULL supaya tidak diam-diam
        // menghilangkan seluruh katalog lama dari Kasir -- lihat JavaDoc Produk.getJenisItem().
        // Catatan: picker "Pilih Ekstra" (mis. JSP/Electron/Flutter Kasir) belum dibangun di layar
        // ZK ini pada batch ini -- produk berekstra tetap bisa dijual di sini TANPA opsi ekstra.
        StringBuilder sql = new StringBuilder("SELECT id, kode, nama, COALESCE(hargajual,0), COALESCE(stok,0), COALESCE(izinkan_jual_minus_stok,false) "
                + "FROM koperasi.produk WHERE aktif = true AND toko = " + tokoIdAktif
                + " AND (jenis_item IS NULL OR jenis_item NOT IN ('BAHAN','EKSTRA'))");
        if (kategoriFilterId != null) {
            sql.append(" AND jenis_produk = ").append(kategoriFilterId);
        }
        if (!kw.isEmpty()) {
            sql.append(" AND (nama ILIKE '%").append(kw).append("%' OR kode = '").append(kw).append("')");
        }
        sql.append(" ORDER BY nama ASC LIMIT 60");

        List<Object[]> data = rows(sql.toString());
        if (data.isEmpty()) {
            produkBox.appendChild(DashboardUiKit.html("<div class='psk-empty'>Tidak ada produk yang cocok.</div>"));
            return;
        }
        int idx = 0;
        for (Object[] r : data) {
            final Long pid = ((Number) r[0]).longValue();
            final String kode = str(r[1]);
            final String nama = str(r[2]);
            final double harga = num(r[3]);
            final double stok = num(r[4]);
            final boolean izinkanJualMinusStok = bool(r[5]);

            final Div card = new Div();
            card.setSclass("psk-card");
            String ikon = PSK_ICON[idx % PSK_ICON.length];
            idx++;
            String imgUrl = Common.ROOT + "/Data?action=file&class=ais.database.model.file.LampiranLain&ref=" + pid
                    + "&jenis=ais.database.model.inventory.Produk&render=true";
            String html = "<div class='psk-ico'>" + ikon
                    + "<img class='psk-img' src='" + imgUrl + "' alt='' onerror=\"this.style.display='none'\"/></div>"
                    + "<div class='psk-nm'>" + DashboardUiKit.esc(nama) + "</div>"
                    + "<div class='psk-pr'>Rp " + DashboardUiKit.money(harga) + "</div>"
                    + "<div class='psk-st'>Stok: " + DashboardUiKit.money(stok) + "</div>"
                    + "<div class='psk-add'>+</div>";
            card.appendChild(DashboardUiKit.html(html));
            card.setTooltiptext("Tambahkan " + nama + " ke keranjang");
            card.addEventListener("onClick", new EventListener() {
                @Override
                public void onEvent(Event e) throws Exception {
                    tambahKeKeranjang(pid, kode, nama, harga, stok, izinkanJualMinusStok);
                }
            });
            card.setParent(produkBox);
        }
    }

    private void cariMember(String kw, Listbox lst) {
        lst.getItems().clear();
        String k = kw == null ? "" : kw.trim().replace("'", "''");
        if (k.isEmpty()) {
            return;
        }
        String sql = "SELECT id, nama, COALESCE(kode_identitas,'') FROM koperasi.anggota_koperasi "
                + "WHERE nama ILIKE '%" + k + "%' OR kode_identitas = '" + k + "' ORDER BY nama ASC LIMIT 25";
        for (Object[] r : rows(sql)) {
            Listitem it = new Listitem();
            it.setValue(((Number) r[0]).longValue());
            new Listcell(str(r[1])).setParent(it);
            new Listcell(str(r[2])).setParent(it);
            it.setParent(lst);
        }
    }

    private void pilihMember(Long id) {
        try {
            AnggotaKoperasi a = (AnggotaKoperasi) HibernateUtil.currentSession().get(AnggotaKoperasi.class, id);
            if (a != null) {
                memberId = a.getId();
                memberNama = a.getNama();
                memberJenisId = a.getJenisAnggotaKoperasi() == null ? null : a.getJenisAnggotaKoperasi().getId();
                memberTipeId = a.getTipeAnggotaKoperasi() == null ? null : a.getTipeAnggotaKoperasi().getId();
                bdMember.setValue(memberNama);
                try {
                    Double s = ais.action.master.sekolah.util.DepositHelper.hitungDeposit(a);
                    memberSaldo = s == null ? 0 : s.doubleValue();
                } catch (Exception ex) {
                    memberSaldo = 0;
                }
                memberMinSaldo = 0;
                memberWajibPin = false;
                if (memberJenisId != null) {
                    for (Object[] r : rows("SELECT COALESCE(minimal_saldo,0), COALESCE(wajib_pin,false) "
                            + "FROM koperasi.jenis_anggota_koperasi WHERE id = " + memberJenisId)) {
                        memberMinSaldo = num(r[0]);
                        memberWajibPin = bool(r[1]);
                    }
                }
                if (lblMemberSaldo != null) {
                    lblMemberSaldo.setValue("Saldo: Rp " + DashboardUiKit.money(memberSaldo)
                            + (memberMinSaldo > 0 ? "  ·  min. mengendap Rp " + DashboardUiKit.money(memberMinSaldo) : ""));
                }
                if (lnkTopup != null) {
                    lnkTopup.setStyle("color:#2563eb;display:inline-block;margin-top:6px;");
                }
                buildMetode();      // saring metode sesuai jenis anggota
                updateUsageDiskon();  // segarkan limit harian promo
                recompute();
            }
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
        }
    }

    // ======================== Keranjang & diskon ========================

    private void tambahKeKeranjang(Long id, String kode, String nama, double harga, double stok,
            boolean izinkanJualMinusStok) {
        for (int posisi = 0; posisi < cart.size(); posisi++) {
            Item it = cart.get(posisi);
            if (it.id.equals(id)) {
                if (!lolosCekStok(nama, stok, it.jumlah + 1, it.izinkanJualMinusStok)) {
                    return;
                }
                it.jumlah += 1;
                // Produk yang dipindai ulang harus langsung terlihat di atas.
                // Pindahkan objek yang sama supaya seluruh state diskon tetap utuh.
                cart.remove(posisi);
                cart.add(0, it);
                recompute();
                return;
            }
        }
        if (!lolosCekStok(nama, stok, 1, izinkanJualMinusStok)) {
            return;
        }
        cart.add(0, new Item(id, kode, nama, harga, stok, izinkanJualMinusStok));
        recompute();
    }

    /**
     * Cegah penambahan item melebihi stok yang tersedia (mencegah oversell/stok negatif). Penegakan
     * dikendalikan konfigurasi {@link Konfigurasi#KANTIN_POS_CEGAH_OVERSELL} (default AKTIF); admin
     * dapat mematikannya bila koperasi menjual jasa/produk tanpa pelacakan stok. Mengembalikan
     * {@code true} bila boleh ditambah; bila tidak, menampilkan peringatan dan mengembalikan
     * {@code false}.
     */
    private boolean lolosCekStok(String nama, double stok, int qtyBaru, boolean izinkanJualMinusStok) {
        if (bolehTransaksiStokHabis || izinkanJualMinusStok) {
            return true;
        }
        if (!Common.bolehKonfigurasi(Konfigurasi.KANTIN_POS_CEGAH_OVERSELL, Konfigurasi.AKTIF)) {
            return true;
        }
        if (qtyBaru > stok) {
            try {
                MyMessageboxConfig.show("Stok \"" + nama + "\" tidak mencukupi (tersedia "
                        + DashboardUiKit.money(stok) + "). Bila produk ini jasa/tanpa stok, matikan "
                        + "\"Cegah Oversell Kasir\" pada Konfigurasi.", "Stok Tidak Cukup", MyMessageboxConfig.OK,
                        MyMessageboxConfig.EXCLAMATION);
            } catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/koperasi/PosKantinAction.java:941");
            }
            return false;
        }
        return true;
    }

    /**
     * Method SENTRAL yang dipanggil ULANG setiap kali isi keranjang berubah (tambah/ubah qty/hapus
     * item, ganti member, pilih metode bayar) — satu-satunya tempat total transaksi dihitung ulang
     * dari nol, memperbarui seluruh label terkait, DAN menyiarkan (broadcast) state keranjang
     * terbaru ke Layar Pelanggan.
     *
     * <p>Urutan kerja: (1) reset akumulator "promo terpakai per keranjang" pada {@link Rule} —
     * penting karena diskon berjenjang/berbatas dihitung ULANG dari nol setiap panggilan, bukan
     * diakumulasi lintas-panggilan (mencegah diskon "menempel" salah dari penghitungan sebelumnya);
     * (2) panggil {@link #evaluasiDiskon(Item)} per item keranjang, yang MENGISI {@code it.diskon}/
     * {@code it.cashback} sekaligus menambah {@code r.terpakaiKeranjang} pada aturan yang cocok;
     * (3) jumlahkan subtotal/diskon/cashback dari seluruh item; (4) hitung pajak dari
     * {@code (subtotal - totalDiskon)} — PPN dikenakan setelah diskon, bukan sebelum; (5) simpan
     * hasil ke field instance {@code grandTotal}/{@code grandPajak}/{@code grandCashback} supaya
     * bisa dibaca method lain ({@link #totalTransaksiSaatIni()}, {@link #onBayar()}); (6) siarkan
     * snapshot keranjang ke Layar Pelanggan lewat {@code BroadcastChannel} (fail-safe, dibungkus
     * try/catch — kegagalan menyiarkan ke layar kedua TIDAK BOLEH mengganggu alur kasir utama).</p>
     *
     * <p>Dipanggil sangat sering (setiap interaksi keranjang) — sengaja TIDAK melakukan query
     * database di sini (rumus diskon sudah dimaterialkan sebelumnya oleh {@link #loadRules()} dan
     * dibaca dari memori), supaya tetap responsif walau dipanggil berulang kali dalam hitungan
     * detik saat kasir cepat menambah banyak item.</p>
     */
    private void recompute() {
        // Reset akumulator promo "berlaku per hari & per toko" tiap kali keranjang dihitung ulang.
        if (rules != null) {
            for (Rule r : rules) {
                r.terpakaiKeranjang = 0;
            }
        }
        double subtotal = 0;
        double totalDiskon = 0;
        double totalCashback = 0;
        for (Item it : cart) {
            evaluasiDiskon(it);
            subtotal += it.harga * it.jumlah;
            totalDiskon += it.diskon;
            totalCashback += it.cashback;
        }
        double basePajak = subtotal - totalDiskon;
        grandPajak = pajakPersen > 0 ? basePajak * pajakPersen / 100.0 : 0;
        grandTotal = basePajak + grandPajak;
        grandCashback = totalCashback;

        try {
            JSONObject bcKeranjang = new JSONObject();
            bcKeranjang.put("tipe", "keranjang");
            bcKeranjang.put("namaToko", namaTokoAktif == null ? "" : namaTokoAktif);
            bcKeranjang.put("namaMember", memberNama == null ? "" : memberNama);
            JSONArray bcItems = new JSONArray();
            for (Item it : cart) {
                JSONObject bi = new JSONObject();
                bi.put("nama", it.nama == null ? "" : it.nama);
                bi.put("harga", it.harga);
                bi.put("jumlah", it.jumlah);
                bi.put("diskon", it.diskon);
                bcItems.put(bi);
            }
            bcKeranjang.put("items", bcItems);
            bcKeranjang.put("subtotal", subtotal);
            bcKeranjang.put("totalDiskon", totalDiskon);
            bcKeranjang.put("totalCashback", totalCashback);
            bcKeranjang.put("totalPajak", grandPajak);
            bcKeranjang.put("grandTotal", grandTotal);
            broadcastKeLayarPelanggan(bcKeranjang);
        } catch (org.json.JSONException je) { ais.common.ErrorAuditUtil.record(je, "auto-audit(empty-catch) src/ais/action/master/koperasi/PosKantinAction.java:989");
            // Tidak pernah terjadi: seluruh kunci JSON di atas adalah literal String tetap.
        }

        renderKeranjang();
        if (lblSubtotal != null) {
            lblSubtotal.setValue("Rp " + DashboardUiKit.money(subtotal));
        }
        if (lblDiskon != null) {
            lblDiskon.setValue("- Rp " + DashboardUiKit.money(totalDiskon));
        }
        if (lblCashback != null) {
            lblCashback.setValue("+ Rp " + DashboardUiKit.money(totalCashback));
        }
        if (lblPajak != null) {
            lblPajak.setValue("Rp " + DashboardUiKit.money(grandPajak));
        }
        if (lblTotal != null) {
            lblTotal.setValue("Rp " + DashboardUiKit.money(grandTotal));
        }
        if (lblBayar != null) {
            lblBayar.setValue("Bayar    Rp " + DashboardUiKit.money(grandTotal));
        }
        updateKembalian();
    }

    private void updateKembalian() {
        if (lblKembalian == null) {
            return;
        }
        double bayar = (dbUangBayar == null || dbUangBayar.getValue() == null) ? 0 : dbUangBayar.getValue();
        double k = bayar - grandTotal;
        lblKembalian.setValue("Rp " + DashboardUiKit.money(k < 0 ? 0 : k));
    }

    private void renderKeranjang() {
        if (cartBox == null) {
            return;
        }
        cartBox.getChildren().clear();
        if (cart.isEmpty()) {
            cartBox.appendChild(DashboardUiKit.html(
                    "<div class='psk-empty'>Keranjang masih kosong. Klik produk untuk menambah.</div>"));
            return;
        }
        for (final Item it : cart) {
            Div row = new Div();
            row.setSclass("psk-citem");

            Div info = new Div();
            info.setStyle("flex:1 1 auto;min-width:0;");
            StringBuilder note = new StringBuilder();
            if (it.diskon > 0) {
                note.append(" &middot; diskon Rp ").append(DashboardUiKit.money(it.diskon));
            }
            if (it.cashback > 0) {
                note.append(" &middot; cashback Rp ").append(DashboardUiKit.money(it.cashback));
            }
            info.appendChild(DashboardUiKit.html("<div class='psk-cnm'>" + DashboardUiKit.esc(it.nama) + "</div>"
                    + "<div class='psk-cpr'>Rp " + DashboardUiKit.money(it.harga) + note + "</div>"));
            info.setParent(row);

            Div qty = new Div();
            qty.setSclass("psk-qty");
            Label minus = new Label("−");
            minus.setSclass("psk-qbtn");
            minus.addEventListener("onClick", new EventListener() {
                @Override
                public void onEvent(Event e) throws Exception {
                    it.jumlah -= 1;
                    if (it.jumlah <= 0) {
                        cart.remove(it);
                    }
                    recompute();
                }
            });
            qty.appendChild(minus);
            Label qn = new Label(String.valueOf(it.jumlah));
            qn.setSclass("psk-qn");
            qty.appendChild(qn);
            Label plus = new Label("+");
            plus.setSclass("psk-qbtn");
            plus.addEventListener("onClick", new EventListener() {
                @Override
                public void onEvent(Event e) throws Exception {
                    if (!lolosCekStok(it.nama, it.stok, it.jumlah + 1, it.izinkanJualMinusStok)) {
                        return;
                    }
                    it.jumlah += 1;
                    recompute();
                }
            });
            qty.appendChild(plus);
            qty.setParent(row);

            Label sub = new Label("Rp " + DashboardUiKit.money(it.harga * it.jumlah - it.diskon));
            sub.setSclass("psk-csub");
            sub.setParent(row);

            Label del = new Label("×");
            del.setSclass("psk-cdel");
            del.setTooltiptext("Hapus item");
            del.addEventListener("onClick", new EventListener() {
                @Override
                public void onEvent(Event e) throws Exception {
                    cart.remove(it);
                    recompute();
                }
            });
            del.setParent(row);

            row.setParent(cartBox);
        }
    }

    /** Port server-side dari evaluateDiscount: cari aturan pertama yang cocok lalu hitung diskon/cashback. */
    private void evaluasiDiskon(Item it) {
        it.diskon = 0;
        it.cashback = 0;
        it.aturanDiskonId = null;
        it.berlakuPerHari = false;
        if (rules == null || rules.isEmpty()) {
            return;
        }
        Date now = new Date();
		List<Rule> eligible=new ArrayList<Rule>();
        for (Rule r : rules) {
            if (r.aktivasiManual) {
                continue; // dikecualikan dari auto-apply -- ZK belum punya picker "Promo" manual
            }
            if (r.produkId != null && !r.produkId.equals(it.id)) {
                continue;
            }
            if (r.tokoId != null && !r.tokoId.equals(tokoIdAktif)) {
                continue;
            }
            if (r.tglMulai != null && r.tglMulai.after(now)) {
                continue;
            }
            if (r.tglSelesai != null && r.tglSelesai.before(now)) {
                continue;
            }
            if (!ais.common.HariAktifUtil.aktifPadaHari(r.hariAktif, now)) {
                continue;
            }
            if (!r.berlakuSemua || r.khususMember) {
                if (memberId == null) {
                    continue;
                }
                if (!jsonIdMemuat(r.jenisMemberJson, memberJenisId)
                        || !jsonIdMemuat(r.tipeMemberJson, memberTipeId)) {
                    continue;
                }
                if (r.jenisId != null && !r.jenisId.equals(memberJenisId)) {
                    continue;
                }
                if (r.tipeId != null && !r.tipeId.equals(memberTipeId)) {
                    continue;
                }
            }
			eligible.add(r);
        }
        if (eligible.isEmpty()) {
            return;
        }
        final double itemTotal = it.harga * it.jumlah;
        final int jumlahItem = it.jumlah;
		Collections.sort(eligible,new Comparator<Rule>(){public int compare(Rule a,Rule b){if(a.prioritas!=b.prioritas)return a.prioritas>b.prioritas?-1:1;double va=nilaiPotensial(a,itemTotal,jumlahItem),vb=nilaiPotensial(b,itemTotal,jumlahItem);if(va!=vb)return va>vb?-1:1;return a.aturanId.compareTo(b.aturanId);}});
		Rule pertama=eligible.get(0);
		Set<String> eksklusif=new HashSet<String>();
		for(int ri=0;ri<eligible.size();ri++){
			Rule applied=eligible.get(ri);
			if(ri>0 && (!pertama.dapatDigabung || !applied.dapatDigabung))break;
			String eks=applied.grupEksklusif==null?"":applied.grupEksklusif.trim();
			if(eks.length()>0 && eksklusif.contains(eks))continue;
			if(eks.length()>0)eksklusif.add(eks);
			double dasar="HARGA_AWAL".equals(applied.dasarPerhitungan)?itemTotal:Math.max(0,itemTotal-it.diskon);
			double disc = 0;
			if (applied.persen > 0) {
				disc = dasar * (applied.persen / 100.0);
			} else if (applied.nominal > 0) {
				disc = applied.nominal * it.jumlah;
				if (disc > dasar) disc = dasar;
			}
		if (applied.berlakuPerHari && applied.maxPot > 0) {
            // Batas maksimal potongan dihitung kumulatif: pemakaian hari ini + akumulasi keranjang.
            double sisa = applied.maxPot - applied.terpakaiHariIni - applied.terpakaiKeranjang;
            if (sisa <= 0) {
                disc = 0;
            } else if (disc > sisa) {
                disc = sisa;
            }
            applied.terpakaiKeranjang += disc;
        } else if (applied.maxPot > 0 && disc > applied.maxPot) {
            disc = applied.maxPot;
        }
        if (applied.potonganLangsung) {
            it.diskon += Math.min(Math.max(0,itemTotal-it.diskon),disc);
        } else {
            it.cashback += disc;
        }
        if (applied.cashbackTetap > 0) {
            it.cashback += Math.min(itemTotal, applied.cashbackTetap * it.jumlah);
        }
		if(it.aturanDiskonId==null && !applied.sumberGrup)it.aturanDiskonId=applied.aturanId;
		it.berlakuPerHari = it.berlakuPerHari || applied.berlakuPerHari;
		}
		it.cashback=Math.min(Math.max(0,itemTotal-it.diskon),it.cashback);
    }

	private static double nilaiPotensial(Rule r,double total,int jumlah){double v=r.persen>0?total*(r.persen/100d):r.nominal*Math.max(1,jumlah);if(r.maxPot>0&&v>r.maxPot)v=r.maxPot;return Math.max(0,v)+Math.max(0,r.cashbackTetap*Math.max(1,jumlah));}

    private static boolean jsonIdMemuat(String json, Long nilai) {
        if (json == null || json.trim().length() == 0 || "[]".equals(json.trim())) return true;
        if (nilai == null) return false;
        try {
            JSONArray daftar = new JSONArray(json);
            for (int i = 0; i < daftar.length(); i++)
                if (String.valueOf(nilai).equals(String.valueOf(daftar.get(i)))) return true;
        } catch (Exception e) {
            ais.common.ErrorAuditUtil.record(e, "filter member grup aturan diskon ZK");
        }
        return false;
    }

    // ======================== Bayar ========================

    /**
     * <h3>Titik masuk checkout: memvalidasi seluruh syarat pembayaran, lalu mencabangkan ke salah
     * satu dari TIGA jalur penyelesaian transaksi berbeda.</h3>
     *
     * <p>Method ini SENDIRI tidak pernah menulis transaksi ke database — tugasnya murni gerbang
     * validasi berlapis (fail-fast, tiap gerbang menampilkan pesan spesifik lalu {@code return}
     * bila gagal) sebelum meneruskan payload siap-kirim ke salah satu dari tiga eksekutor:</p>
     * <ol>
     *   <li><b>Gerbang toko dipilih</b> — tanpa {@code tokoIdAktif}, tidak ada transaksi yang punya
     *       arti (setiap baris penjualan wajib terikat ke satu toko).</li>
     *   <li><b>Gerbang Sesi Kas Kasir</b> (dapat dikonfigurasi, default AKTIF —
     *       {@code Konfigurasi.KANTIN_POS_WAJIB_SESI_KAS}) — toko yang mengaktifkannya mewajibkan
     *       kasir membuka sesi kas dulu; pencocokan identitas ({@code oleh}/{@code olehId}) memakai
     *       pola yang SAMA dengan {@code KasKasirZkAction} supaya
     *       {@code SesiKasUtil.hitungPenjualan()} nanti bisa mencocokkan transaksi ke sesi yang
     *       benar.</li>
     *   <li><b>Gerbang keranjang &amp; metode bayar dipilih</b> — validasi dasar.</li>
     *   <li><b>Gerbang saldo member</b> — HANYA untuk metode bayar potong-saldo (bukan
     *       {@code manual}/tunai, bukan {@code online}/QRIS): member wajib dipilih, saldo wajib
     *       cukup, DAN sisa saldo setelah transaksi tidak boleh jatuh di bawah
     *       {@code memberMinSaldo} ("saldo mengendap" — batas minimum yang wajib tersisa,
     *       ditentukan oleh jenis keanggotaan).</li>
     *   <li><b>Gerbang PIN pembeli</b> (opsional, per jenis anggota — {@code memberWajibPin}) —
     *       BUKAN diverifikasi oleh kasir, melainkan oleh PEMBELI SENDIRI di Layar Pelanggan (layar
     *       kedua): payload disimpan sementara ke {@code pendingPayload}/dst., permintaan PIN
     *       disiarkan lewat {@link #broadcastKeLayarPelanggan}, lalu method ini {@code return}
     *       TANPA menyelesaikan transaksi — penyelesaiannya menunggu balasan async
     *       {@link #onPinHasilDiterima()} lewat jembatan {@code pinBridge}. Timer 90 detik
     *       ({@code pinTimeoutTimer}) membatalkan payload tertunda bila pembeli tidak merespons.</li>
     * </ol>
     *
     * <p><b>Percabangan akhir</b> (hanya tercapai bila TIDAK butuh verifikasi PIN, atau setelah PIN
     * terverifikasi lewat {@link #onPinHasilDiterima()}): metode bayar {@code online} (nama
     * mengandung "online"/"qris"/"topup") menampilkan modal QR dan menunggu konfirmasi pembayaran
     * eksternal lewat polling ({@link #tampilkanQrModal}); selain itu langsung
     * {@link #eksekusiBayar} (tunai/potong-saldo, tidak perlu menunggu konfirmasi eksternal apa
     * pun).</p>
     *
     * <p>Payload yang dibangun di sini konsisten dengan kontrak {@code KantinHelper.bayar()}: field
     * {@code draftPembelianAnggotaKoperasi} disertakan HANYA bila transaksi ini berasal dari
     * keranjang yang sebelumnya "Ditahan" ({@code draftIdAktif != null}) — menandakan ke server agar
     * MENUNTASKAN draft yang sama, bukan membuat baris penjualan baru yang terpisah.</p>
     *
     * @throws Exception merambat apa adanya dari kegagalan tak terduga — kegagalan yang DIKETAHUI
     *         (toko belum dipilih, saldo kurang, dst.) ditangani lewat {@code MyMessageboxConfig}
     *         dan {@code return} biasa, bukan exception.
     */
    private void onBayar() throws Exception {
        if (tokoIdAktif == null) {
            MyMessageboxConfig.show("Mohon maaf, toko/pedagang belum dipilih. Langkah yang dapat dilakukan: (1) pilih toko/pedagang dari daftar di bagian atas layar; (2) pastikan toko sudah aktif dan tersedia; (3) ulangi proses ini.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return;
        }
		// OPT-IN (default TIDAK AKTIF, selaras gerbang server KantinHelper.bayar):
		// kewajiban sesi kas hanya berlaku bila konfigurasi diaktifkan eksplisit.
		if (Common.bolehKonfigurasi(Konfigurasi.KANTIN_POS_WAJIB_SESI_KAS, Konfigurasi.TIDAK_AKTIF)) {
			ais.database.model.inventory.SesiKasKasir sesiPerangkat =
					ais.action.master.koperasi.helper.SesiKasUtil.sesiTerbukaPerangkat(
							HibernateUtil.currentSession(), oleh, olehId, tokoIdAktif, idPerangkatZk());
			if (sesiPerangkat == null) {
				ais.database.model.inventory.SesiKasKasir sesiLain =
						ais.action.master.koperasi.helper.SesiKasUtil.sesiTerbuka(
								HibernateUtil.currentSession(), oleh, olehId, null);
				MyMessageboxConfig.show(
						sesiLain == null
								? "Belum ada Sesi Kas Kasir yang terbuka pada perangkat ini. Buka kas terlebih dahulu sebelum memproses pembayaran."
								: "Sesi kas akun ini sedang aktif pada perangkat lain. Tutup kas pada perangkat tersebut; transaksi pada perangkat ini dikunci untuk mencegah pencampuran penerimaan kasir.",
						"Sesi Kas Belum Dibuka", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
				return;
			}
        }
        if (cart.isEmpty()) {
            MyMessageboxConfig.show("Mohon maaf, keranjang belanja masih kosong. Langkah yang dapat dilakukan: (1) pilih produk dari daftar produk dengan menekan tombol produk; (2) scan barcode produk jika tersedia scanner; (3) ulangi pembayaran setelah menambah produk.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return;
        }
        if (caraTerpilih == null) {
            MyMessageboxConfig.show("Mohon maaf, cara pembayaran belum dipilih. Langkah yang dapat dilakukan: (1) pilih cara pembayaran dari panel metode pembayaran; (2) pastikan metode pembayaran tersedia untuk toko ini; (3) ulangi proses checkout.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return;
        }

        ais.database.model.koperasi.CaraPembayaranKoperasi cara = caraTerpilih;
        Long caraBayarId = cara.getId();
        boolean manual = cara.getManual() != null && cara.getManual().booleanValue();
        String namaCara = cara.getNama() == null ? "" : cara.getNama().toLowerCase();
        boolean online = namaCara.contains("online") || namaCara.contains("qris") || namaCara.contains("topup");

        // Pembayaran potong-saldo (bukan tunai/manual, bukan online) wajib member & saldonya cukup.
        // Untuk online/QRIS, member &amp; pembayaran ditentukan dari hasil cek pembayaran (checkBayar).
        if (!manual && !online) {
            if (memberId == null) {
                MyMessageboxConfig.show("Mohon maaf, pembayaran lewat saldo memerlukan data member. Langkah yang dapat dilakukan: (1) scan kartu atau masukkan ID member di kolom pencarian member; (2) pastikan member memiliki saldo mencukupi; (3) ulangi proses pembayaran.",
                        "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
                return;
            }
            if (memberSaldo < grandTotal) {
                MyMessageboxConfig.show("Saldo " + (memberNama == null ? "member" : memberNama) + " tidak mencukupi. Saldo Rp "
                        + DashboardUiKit.money(memberSaldo)
                        + ", sedangkan total Rp " + DashboardUiKit.money(grandTotal) + ".", "Saldo Kurang",
                        MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
                return;
            }
            // Saldo mengendap: sisa setelah transaksi tidak boleh di bawah batas minimal jenis anggota.
            if (memberMinSaldo > 0 && (memberSaldo - grandTotal) < memberMinSaldo) {
                MyMessageboxConfig.show("Transaksi ditolak. Sisa saldo " + (memberNama == null ? "member" : memberNama)
                        + " setelah transaksi kurang dari batas saldo "
                        + "mengendap yang diizinkan (Rp " + DashboardUiKit.money(memberMinSaldo) + ").",
                        "Saldo Mengendap", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
                return;
            }
        }

        double total = totalTransaksiSaatIni();
        JSONArray arr = buildTransaksiArray();

        JSONObject payload = new JSONObject();
        payload.put("kodeUnik", generateKodeUnik());
        payload.put("idToko", tokoIdAktif);
        payload.put("waktu", Common.dateFormat3.get().format(new Date()));
        payload.put("caraBayar", caraBayarId);
		payload.put("id_perangkat", idPerangkatZk());
		payload.put("nama_perangkat", "ZK Web / " + idPerangkatZk());
        if (grandPajak > 0) {
            payload.put("pajak", grandPajak);
        }
        if (memberId != null) {
            payload.put("id_member", memberId);
        }
        if (txtCatatan != null && txtCatatan.getValue() != null && !txtCatatan.getValue().trim().isEmpty()) {
            payload.put("keterangan", txtCatatan.getValue().trim());
        }
        if (draftIdAktif != null) {
            // Keranjang ini hasil "Muat" dr daftar Tertahan -- tuntaskan draft yg SAMA (bukan duplikat).
            payload.put("draftPembelianAnggotaKoperasi", draftIdAktif);
        }
        payload.put("transaksi", arr);

        // --- GATE PIN PEMBELI (hanya bila jenis anggota member terpilih "Wajib PIN") ---
        // Verifikasi dilakukan PEMBELI SENDIRI di Layar Pelanggan (layar kedua); transaksi baru
        // lanjut disimpan setelah balasan 'pin_hasil' ok=true tiba lewat jembatan onPinHasilDiterima().
        if (memberId != null && memberWajibPin) {
            pendingPayload = payload;
            pendingCaraNama = cara.getNama();
            pendingTotal = total;
            pendingOnline = online;

            JSONObject bcMintaPin = new JSONObject();
            bcMintaPin.put("tipe", "minta_pin");
            bcMintaPin.put("memberId", memberId);
            bcMintaPin.put("memberNama", memberNama == null ? "" : memberNama);
            broadcastKeLayarPelanggan(bcMintaPin);
            try {
                org.zkoss.zk.ui.util.Clients.evalJavaScript(
                        "if(!window.__posCustWinZK||window.__posCustWinZK.closed){alert('"
                                + "Buka Layar Pelanggan (layar kedua) terlebih dahulu untuk verifikasi PIN pembeli.');}");
            } catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/koperasi/PosKantinAction.java:1277");
            }

            if (pinTimeoutTimer != null) {
                pinTimeoutTimer.stop();
                pinTimeoutTimer.setParent(null);
            }
            pinTimeoutTimer = new org.zkoss.zul.Timer();
            pinTimeoutTimer.setDelay(90000);
            pinTimeoutTimer.setRepeats(false);
            pinTimeoutTimer.setParent(posHost);
            pinTimeoutTimer.addEventListener("onTimer", new EventListener() {
                @Override
                public void onEvent(Event e) throws Exception {
                    if (pendingPayload != null) {
                        pendingPayload = null;
                        pendingCaraNama = null;
                        pendingTotal = 0;
                        pendingOnline = false;
                        MyMessageboxConfig.show(
                                "Waktu verifikasi PIN pembeli habis (90 detik). Transaksi dibatalkan.",
                                "Verifikasi PIN Kedaluwarsa", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
                    }
                }
            });
            pinTimeoutTimer.start();
            return;
        }

        if (online) {
            // Tampilkan QR + tunggu konfirmasi pembayaran (polling), baru simpan.
            tampilkanQrModal(payload, cara.getNama(), total);
        } else {
            eksekusiBayar(payload, cara.getNama(), total);
        }
    }

	/** Identitas stabil selama sesi login browser untuk mengisolasi sesi kas ZK per perangkat. */
	private String idPerangkatZk() {
		try {
			Object nativeSession = org.zkoss.zk.ui.Sessions.getCurrent().getNativeSession();
			if (nativeSession instanceof javax.servlet.http.HttpSession) {
				return "zk-" + ((javax.servlet.http.HttpSession) nativeSession).getId();
			}
		} catch (Exception ignore) {
			ais.common.ErrorAuditUtil.record(ignore,
					"auto-audit PosKantinAction.idPerangkatZk");
		}
		return "zk-" + org.zkoss.zk.ui.Executions.getCurrent().getDesktop().getId();
	}

    /** Total keranjang saat ini (subtotal setiap baris dikurangi diskon baris itu) -- dipakai
     *  ulang oleh {@link #onBayar()} dan {@link #tahanTransaksi()} agar rumus totalnya SATU
     *  sumber kebenaran, tak dobel-tulis di dua tempat. */
    private double totalTransaksiSaatIni() {
        double total = 0;
        for (Item it : cart) {
            total += it.harga * it.jumlah - it.diskon;
        }
        return total;
    }

    /** Ubah {@link #cart} jadi {@link JSONArray} sesuai kontrak {@code transaksi} yang dipakai
     *  bersama oleh {@link KantinHelper#bayar} dan {@link KantinHelper#draft_bayar} -- dipakai
     *  ulang oleh {@link #onBayar()} (checkout final) dan {@link #tahanTransaksi()} (simpan
     *  sementara/hold), supaya format item TIDAK bisa berbeda antara jalur bayar-langsung vs
     *  simpan-draft. */
    private JSONArray buildTransaksiArray() throws Exception {
        JSONArray arr = new JSONArray();
        for (Item it : cart) {
            JSONObject t = new JSONObject();
            t.put("id", it.id);
            t.put("kode", it.kode == null ? "" : it.kode);
            t.put("nama", it.nama == null ? "" : it.nama);
            t.put("harga", it.harga);
            t.put("jumlah", it.jumlah);
            t.put("diskon", it.diskon);
            t.put("cashback", it.cashback);
            if (it.aturanDiskonId != null) {
                t.put("aturanDiskon", it.aturanDiskonId);
            }
            t.put("berlakuPerHariDanPerToko", it.berlakuPerHari);
            arr.put(t);
        }
        return arr;
    }

    /**
     * <h2>"Simpan Keranjang" (hold/park sale) -- tombol "Tahan" di layar Kasir.</h2>
     *
     * <p>
     * Menyimpan keranjang yang sedang diisi kasir sebagai transaksi BELUM LUNAS
     * ({@code koperasi.draft_pembelian_anggota_koperasi}, kolom {@code lunas} tetap NULL),
     * lalu mengosongkan layar Kasir supaya kasir bisa langsung melayani pembeli berikutnya.
     * Transaksi yang ditahan bisa dimuat kembali kapan saja lewat link "Tertahan" di atas
     * keranjang ({@link #loadDaftarTertahan()}/{@link #muatDraftTertahan(Long)}), dan baru
     * benar-benar tercatat sebagai penjualan (mengurangi stok, dsb.) setelah kasir menekan
     * "Bayar" — persis alur "park sale" pada mesin kasir fisik pada umumnya.
     * </p>
     *
     * <p>
     * Method ini SENGAJA TIDAK menulis logika penyimpanan sendiri — ia memanggil
     * {@link KantinHelper#draft_bayar} yang SUDAH dipakai bertahun-tahun oleh modul Pesanan
     * (pemesanan mandiri anggota koperasi) untuk kasus yang sama persis: menyimpan keranjang
     * sebagai draft. Menulis ulang logika ini di kelas POS akan menduplikasi aturan bisnis
     * (kolom, validasi, cara menghitung total) yang sudah teruji, dan berisiko kedua salinan
     * itu perlahan berbeda perilaku seiring waktu. Format data (array {@code transaksi}) juga
     * dipakai ulang dari {@link #buildTransaksiArray()} yang sama dengan {@link #onBayar()}.
     * </p>
     */
    private void tahanTransaksi() throws Exception {
        if (tokoIdAktif == null) {
            MyMessageboxConfig.show("Mohon maaf, toko/pedagang belum dipilih. Langkah yang dapat dilakukan: (1) pilih toko/pedagang dari daftar di bagian atas layar; (2) pastikan toko sudah aktif dan tersedia; (3) ulangi proses ini.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return;
        }
        if (cart.isEmpty()) {
            MyMessageboxConfig.show("Mohon maaf, keranjang masih kosong sehingga tidak ada yang dapat disimpan. Langkah yang dapat dilakukan: (1) tambahkan produk ke keranjang terlebih dahulu; (2) ulangi proses simpan keranjang.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return;
        }
        bukaDialogAlasanTahan();
    }

    /**
     * Meminta alasan operasional sebelum park sale disimpan. Daftar mengikuti konfigurasi toko;
     * opsi "Lainnya" tetap tersedia supaya kondisi lapangan yang belum tercakup tidak menghambat
     * kasir. Penyimpanan baru dijalankan setelah satu alasan valid dipilih.
     */
    private void bukaDialogAlasanTahan() throws Exception {
        Toko toko = (Toko) HibernateUtil.currentSession().get(Toko.class, tokoIdAktif);
        JSONArray daftar = KantinHelper.alasanTahanUntukToko(toko);
        final ais.ui.util.MyWindow dialog = new ais.ui.util.MyWindow("Alasan Transaksi Ditahan", "normal", true);
        dialog.setWidth("620px");
        dialog.setClosable(true);

        Vlayout isi = new Vlayout();
        isi.setSpacing("8px");
        isi.appendChild(new Label("Pilih satu alasan agar transaksi mudah ditindaklanjuti oleh kasir berikutnya."));
        final Radiogroup grup = new Radiogroup();
        grup.setOrient("vertical");
        for (int i = 0; i < daftar.length(); i++) {
            Radio radio = new Radio(daftar.optString(i));
            radio.setValue(daftar.optString(i));
            grup.appendChild(radio);
            if (i == 0) radio.setChecked(true);
        }
        final Radio lainnya = new Radio("Lainnya (tulis alasan sendiri)");
        lainnya.setValue("__LAINNYA__");
        grup.appendChild(lainnya);
        isi.appendChild(grup);

        final Textbox alasanLain = new Textbox();
        alasanLain.setRows(3);
        alasanLain.setMaxlength(200);
        alasanLain.setWidth("100%");
        alasanLain.setTooltiptext("Tuliskan alasan secara singkat dan jelas");
        alasanLain.setVisible(false);
        isi.appendChild(alasanLain);
        grup.addEventListener("onCheck", new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                boolean tampil = grup.getSelectedItem() != null
                        && "__LAINNYA__".equals(grup.getSelectedItem().getValue());
                alasanLain.setVisible(tampil);
                if (tampil) alasanLain.focus();
            }
        });

        Hlayout tombol = new Hlayout();
        tombol.setStyle("margin-top:12px;justify-content:flex-end;");
        Button batal = new Button("Batal");
        Button simpan = new Button("Tahan Transaksi");
        simpan.setSclass("btn btn-primary");
        batal.addEventListener("onClick", new EventListener() {
            @Override public void onEvent(Event event) throws Exception { dialog.detach(); }
        });
        simpan.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                String alasan = grup.getSelectedItem() == null ? "" : String.valueOf(grup.getSelectedItem().getValue());
                if ("__LAINNYA__".equals(alasan)) alasan = alasanLain.getValue() == null ? "" : alasanLain.getValue().trim();
                if (alasan.length() > 200) alasan = alasan.substring(0, 200).trim();
                if (alasan.isEmpty()) {
                    MyMessageboxConfig.show("Alasan transaksi ditahan wajib diisi.", "Peringatan",
                            MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
                    return;
                }
                dialog.detach();
                simpanTransaksiTertahan(alasan);
            }
        });
        tombol.appendChild(batal);
        tombol.appendChild(simpan);
        isi.appendChild(tombol);
        dialog.appendChild(isi);
        dialog.doModal();
    }

    private void simpanTransaksiTertahan(String alasanTahan) throws Exception {
        // caraBayar WAJIB diisi oleh KantinHelper.draft_bayar -- pakai yg sedang dipilih kasir,
        // atau cara pertama yg aktif bila belum dipilih (murni penanda administratif; tak ada
        // penagihan apa pun sampai keranjang ini benar-benar di-"Bayar").
        Long caraBayarId = caraTerpilih != null ? caraTerpilih.getId() : null;
        if (caraBayarId == null) {
            List<Object[]> rc = rows("SELECT id FROM koperasi.cara_pembayaran_koperasi WHERE aktif=true ORDER BY nama ASC LIMIT 1");
            if (!rc.isEmpty()) {
                caraBayarId = lng(rc.get(0)[0]);
            }
        }
        if (caraBayarId == null) {
            MyMessageboxConfig.show("Mohon maaf, belum ada metode pembayaran yang tersedia untuk menyimpan transaksi. Langkah yang dapat dilakukan: (1) pastikan setidaknya satu metode pembayaran aktif untuk toko ini; (2) tambahkan cara pembayaran di menu Pengaturan; (3) ulangi proses.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return;
        }

        JSONObject payload = new JSONObject();
        if (draftIdAktif != null) {
            payload.put("id", draftIdAktif);
        }
        payload.put("kodeUnik", "DRAFT-" + generateKodeUnik());
        payload.put("idToko", tokoIdAktif);
        payload.put("waktu", Common.dateFormat3.get().format(new Date()));
        payload.put("caraBayar", caraBayarId);
        if (memberId != null) {
            payload.put("id_member", memberId);
        }
        // Sebelumnya HILANG di sini (bug nyata): kasir yang mengetik catatan (mis. "tanpa gula",
        // "antar ke meja 5") lalu menekan "Tahan" kehilangan catatannya diam-diam -- draft_bayar()
        // di KantinHelper.java SUDAH mendukung field ini (sama seperti onBayar() di bawah), hanya
        // belum pernah dikirim dari jalur "Tahan".
        String catatan = txtCatatan == null || txtCatatan.getValue() == null ? "" : txtCatatan.getValue().trim();
        payload.put("keterangan", catatan.isEmpty() ? alasanTahan : alasanTahan + " | Catatan: " + catatan);
        payload.put("transaksi", buildTransaksiArray());

        JSONObject hasil = new JSONObject();
        try {
            KantinHelper.draft_bayar(Common.getCurrentUser(), payload, hasil);
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
            MyMessageboxConfig.show("Gagal menyimpan keranjang: " + e.getMessage(), "Kesalahan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
            return;
        }

        String status = hasil.optString("status", "");
        if ("00".equals(status) || "success".equalsIgnoreCase(status)) {
            cart.clear();
            draftIdAktif = null;
            // Sebelumnya HANYA menyetel memberId/memberNama ke null (2 dari 7 field) -- bug nyata:
            // memberSaldo/memberMinSaldo/memberJenisId/memberTipeId/memberWajibPin dari member SEBELUM
            // "Tahan" ditekan tetap basi, dan label saldo tetap menampilkan angka member lama walau
            // kartu member sudah kosong. resetMemberState() menyetel ketujuh field + label sekaligus.
            resetMemberState();
            if (txtCatatan != null) {
                txtCatatan.setValue("");
            }
            recompute();
            buildMetode(); // daftar metode pembayaran ikut kembali ke kondisi "Umum" (tanpa filter member lama)
            loadDaftarTertahan();
            MyMessageboxConfig.show("Keranjang disimpan. Bisa dilanjutkan lewat link \"Tertahan\".", "Tersimpan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
        } else {
            MyMessageboxConfig.show(hasil.optString("description", "Gagal menyimpan keranjang."), "Gagal",
                    MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
        }
    }

    /** Isi {@link #tertahanListBox} dgn daftar keranjang tertahan (belum lunas) milik toko aktif. */
    private void loadDaftarTertahan() {
        if (tertahanListBox == null) {
            return;
        }
        tertahanListBox.getChildren().clear();
        if (tokoIdAktif == null) {
            tertahanListBox.appendChild(DashboardUiKit.html("<div class='psk-empty'>Pilih toko/pedagang dahulu.</div>"));
            return;
        }
        List<Object[]> res = rows("SELECT a.id, TO_CHAR(a.tanggal_pembayaran,'DD-MM HH24:MI'), b.nama, "
                + "COALESCE(a.total_biaya,0), (SELECT COUNT(*) FROM koperasi.draft_pembelian d "
                + "WHERE d.draft_pembelian_anggota_koperasi = a.id) "
                + ", COALESCE(a.keterangan,'') "
                + "FROM koperasi.draft_pembelian_anggota_koperasi a "
                + "LEFT JOIN koperasi.anggota_koperasi b ON a.anggota_koperasi = b.id "
                + "WHERE a.lunas IS NULL AND a.toko = " + tokoIdAktif + " ORDER BY a.tanggal_pembayaran DESC LIMIT 30");
        if (res.isEmpty()) {
            tertahanListBox.appendChild(DashboardUiKit.html("<div class='psk-empty'>Belum ada keranjang yang ditahan.</div>"));
            return;
        }
        for (Object[] r : res) {
            final Long idDraft = lng(r[0]);
            String pemesan = str(r[2]).isEmpty() ? "Walk-in Customer" : str(r[2]);
            Div row = new Div();
            row.setSclass("psk-tertahan-row");
            row.appendChild(DashboardUiKit.html("<div><b>" + DashboardUiKit.esc(pemesan) + "</b><br>"
                    + "<span style='color:#64748b;'>" + DashboardUiKit.esc(str(r[1])) + " &bull; " + str(r[4])
                    + " item &bull; Rp " + DashboardUiKit.money(num(r[3])) + "</span><br>"
                    + "<span style='color:#92400e;'><b>Alasan:</b> " + DashboardUiKit.esc(str(r[5]).isEmpty() ? "-" : str(r[5])) + "</span></div>"));
            Label muat = new Label(ais.common.Common.getBahasaConfig("Muat"));
            muat.setSclass("psk-tertahan-muat");
            muat.addEventListener("onClick", new EventListener() {
                @Override
                public void onEvent(Event e) throws Exception {
                    muatDraftTertahan(idDraft);
                }
            });
            row.appendChild(muat);
            tertahanListBox.appendChild(row);
        }
    }

    /** Muat balik keranjang tertahan {@code idDraft} ke layar Kasir (item+member+metode bayar). */
    private void muatDraftTertahan(Long idDraft) throws Exception {
        List<Object[]> items = rows("SELECT p.id, p.kode, COALESCE(p.nama, d.nama), d.hargasatuan, d.qty, "
                + "COALESCE(d.diskon,0), COALESCE(d.cashback,0), d.aturan_diskon, COALESCE(p.stok,0), "
                + "COALESCE(p.izinkan_jual_minus_stok,false) "
                + "FROM koperasi.draft_pembelian d LEFT JOIN koperasi.produk p ON d.produk = p.id "
                + "WHERE d.draft_pembelian_anggota_koperasi = " + idDraft);
        if (items.isEmpty()) {
            MyMessageboxConfig.show("Mohon maaf, keranjang tertahan ini kosong atau sudah tidak valid. Langkah yang dapat dilakukan: (1) kembali ke daftar keranjang tertahan; (2) pilih keranjang lain yang masih memiliki isi; (3) buat transaksi baru jika diperlukan.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return;
        }
        cart.clear();
        for (Object[] r : items) {
            Item it = new Item(lng(r[0]), str(r[1]), str(r[2]), num(r[3]), num(r[8]), bool(r[9]));
            it.jumlah = (int) Math.round(num(r[4]));
            it.diskon = num(r[5]);
            it.cashback = num(r[6]);
            it.aturanDiskonId = lng(r[7]);
            cart.add(it);
        }
        draftIdAktif = idDraft;

        List<Object[]> header = rows(
                "SELECT anggota_koperasi FROM koperasi.draft_pembelian_anggota_koperasi WHERE id = " + idDraft);
        boolean adaMember = !header.isEmpty() && header.get(0)[0] != null;
        if (adaMember) {
            pilihMember(lng(header.get(0)[0])); // sudah panggil recompute() sendiri di dalamnya
        }

        if (tertahanListBox != null) {
            tertahanListBox.setStyle("display:none;");
        }
        if (!adaMember) {
            recompute();
        }
    }

    /** Simpan transaksi via {@link KantinHelper#bayar}, cetak struk, lalu reset layar kasir. */
    private void eksekusiBayar(JSONObject payload, String caraNama, double total) throws Exception {
        JSONObject hasil = new JSONObject();
        try {
            KantinHelper.bayar(Common.getCurrentUser(), payload, hasil);
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
            ais.common.PesanFormalHelper.tampilkanGagalException(
                    "pembayaran transaksi POS",
                    "Pembayaran belum dapat diselesaikan. Data transaksi tidak diubah agar tetap aman.",
                    e,
                    new String[] { "Periksa kembali isi keranjang dan metode pembayaran.",
                            "Muat ulang pesanan sebelum mencoba kembali.",
                            "Jika kendala berulang, buka Detail Error lalu salin informasinya untuk admin/developer." });
            return;
        }

        String status = hasil.optString("status", "");
        if ("00".equals(status) || "success".equalsIgnoreCase(status)) {
            double bayarTunai = (dbUangBayar != null && dbUangBayar.getValue() != null) ? dbUangBayar.getValue() : 0;
            double kembalian = bayarTunai - total;
            String struk = buildStrukHtml(total, bayarTunai, kembalian, caraNama);
            tampilkanStruk(struk);
            cetakStruk(struk); // langsung cetak struk begitu pembayaran berhasil

            JSONObject bcSukses = new JSONObject();
            bcSukses.put("tipe", "sukses");
            bcSukses.put("totalBayar", total);
            bcSukses.put("uangTunai", bayarTunai);
            bcSukses.put("kembalian", kembalian);
            bcSukses.put("namaToko", namaTokoAktif == null ? "" : namaTokoAktif);
            broadcastKeLayarPelanggan(bcSukses);

            cart.clear();
            draftIdAktif = null;
            resetMemberState();
            if (txtCatatan != null) {
                txtCatatan.setValue("");
            }
            if (dbUangBayar != null) {
                dbUangBayar.setValue(null);
            }
            recompute();
            loadRiwayat(); // segarkan tab Riwayat Transaksi
            buildMetode(); // reset pilihan metode pembayaran
            loadRingkasanHariIni(); // segarkan ringkasan penjualan hari ini
            loadRingkasanInventori(); // segarkan ringkasan inventori (stok berkurang stlh checkout)
            loadMiniRiwayat(); // segarkan mini riwayat di layar kasir
            loadAnalitikKasir(); // segarkan donut+radar analitik kasir
            loadDaftarTertahan(); // segarkan daftar tertahan (draft ini mungkin baru saja dituntaskan)
        } else {
            String pesan = hasil.optString("message",
                    "Pembayaran belum dapat diselesaikan. Data transaksi tidak diubah agar tetap aman.");
            String teknis = hasil.optString("teknis", hasil.optString("description", "status=" + status));
            ais.common.PesanFormalHelper.tampilkanGagalException(
                    "pembayaran transaksi POS", pesan, new IllegalStateException(teknis),
                    new String[] { "Periksa kembali isi keranjang dan metode pembayaran.",
                            "Muat ulang pesanan sebelum mencoba kembali.",
                            "Jika kendala berulang, buka Detail Error lalu salin informasinya untuk admin/developer." });
        }
    }

    /**
     * Pembayaran online/QRIS: tampilkan QR berisi data transaksi lalu polling {@link KantinHelper#checkBayar}
     * tiap beberapa detik (ZK Timer). Begitu pembayaran terdeteksi, transaksi langsung disimpan.
     */
    private void tampilkanQrModal(final JSONObject payload, final String caraNama, final double total) {
        try {
            JSONObject qr = new JSONObject();
            qr.put("nominal", total);
            qr.put("kodeToko", payload.opt("idToko"));
            qr.put("waktu", payload.optString("waktu"));
            qr.put("kodeUnik", payload.optString("kodeUnik"));

            JSONObject bcQris = new JSONObject();
            bcQris.put("tipe", "qris");
            bcQris.put("qrString", qr.toString());
            bcQris.put("totalBayar", total);
            broadcastKeLayarPelanggan(bcQris);

            org.zkoss.image.AImage gambar = null;
            try {
                gambar = ais.common.BarcodeCommon.generateQrAImage(qr.toString());
            } catch (Exception ex) {
                Common.tampilErrorJikaAdmin(ex);
            }

            final ais.ui.util.MyWindow w = new ais.ui.util.MyWindow("Pembayaran QRIS / Online", "normal", true);
            w.setClosable(false);
            w.setParent(posHost.getPage().getFirstRoot());
            w.setWidth("340px");

            Vlayout box = new Vlayout();
            box.setStyle("padding:14px;text-align:center;");
            box.setParent(w);
            box.appendChild(DashboardUiKit.html("<div style='font-size:12px;color:#475569;margin-bottom:8px;'>"
                    + "Arahkan aplikasi pembayaran pelanggan ke kode di bawah ini.</div>"));

            org.zkoss.zul.Image img = new org.zkoss.zul.Image();
            if (gambar != null) {
                img.setContent(gambar);
            }
            img.setWidth("220px");
            img.setHeight("220px");
            img.setParent(box);

            Label lblTot = new Label("Rp " + DashboardUiKit.money(total));
            lblTot.setStyle("display:block;font-weight:900;font-size:20px;color:#0f172a;margin-top:8px;");
            lblTot.setParent(box);
            Label lblStatus = new Label(ais.common.Common.getBahasaConfig("Menunggu konfirmasi pembayaran..."));
            lblStatus.setStyle("display:block;font-size:12px;color:#64748b;margin-top:6px;");
            lblStatus.setParent(box);

            final org.zkoss.zul.Timer timer = new org.zkoss.zul.Timer();
            timer.setDelay(2500);
            timer.setRepeats(true);
            timer.setParent(w);
            timer.addEventListener("onTimer", new EventListener() {
                @Override
                public void onEvent(Event e) throws Exception {
                    JSONObject req = new JSONObject();
                    req.put("kodeUnik", payload.optString("kodeUnik"));
                    JSONObject hasil = new JSONObject();
                    KantinHelper.checkBayar(req, hasil);
                    if ("00".equals(hasil.optString("status", ""))) {
                        timer.stop();
                        if (!hasil.isNull("data")) {
                            payload.put("kodePembayaranOnline", hasil.get("data"));
                        }
                        if (!hasil.isNull("member")) {
                            payload.put("id_member", hasil.get("member"));
                        }
                        w.setParent(null);
                        eksekusiBayar(payload, caraNama, total);
                    }
                }
            });

            Button bBatal = new Button("Batalkan");
            bBatal.setStyle("margin-top:12px;");
            bBatal.addEventListener("onClick", new EventListener() {
                @Override
                public void onEvent(Event e) throws Exception {
                    timer.stop();
                    JSONObject bcBatal = new JSONObject();
                    bcBatal.put("tipe", "batal_qris");
                    broadcastKeLayarPelanggan(bcBatal);
                    w.setParent(null);
                }
            });
            bBatal.setParent(box);

            timer.start();
            w.doOverlapped();
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
        }
    }

    /** Kode unik transaksi &ge; 50 karakter (syarat pencocokan KodePembayaranOnline saat QRIS/online). */
    private static String generateKodeUnik() {
        StringBuilder sb = new StringBuilder("POS").append(System.currentTimeMillis());
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        java.util.Random rnd = new java.util.Random();
        while (sb.length() < 55) {
            sb.append(chars.charAt(rnd.nextInt(chars.length())));
        }
        return sb.toString();
    }

    // ======================== Struk (cetak) ========================

    private void tampilkanStruk(final String html) {
        try {
            final ais.ui.util.MyWindow w = new ais.ui.util.MyWindow("Struk Transaksi", "normal", true);
            w.setParent(org.zkoss.zk.ui.sys.ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
            w.setWidth("360px");

            Vlayout box = new Vlayout();
            box.setStyle("padding:8px;");
            box.setParent(w);

            Div strukBox = new Div();
            strukBox.setStyle("max-height:60vh;overflow:auto;border:1px solid #e2e8f0;border-radius:8px;"
                    + "padding:8px;background:#fff;");
            strukBox.appendChild(DashboardUiKit.html(html));
            strukBox.setParent(box);

            Hlayout btns = new Hlayout();
            btns.setStyle("gap:8px;justify-content:flex-end;margin-top:10px;");
            btns.setParent(box);

            Button bCetak = new Button("Cetak Struk");
            bCetak.addEventListener("onClick", new EventListener() {
                @Override
                public void onEvent(Event e) throws Exception {
                    cetakStruk(html);
                }
            });
            btns.appendChild(bCetak);

            Button bTutup = new Button("Selesai");
            bTutup.addEventListener("onClick", new EventListener() {
                @Override
                public void onEvent(Event e) throws Exception {
                    w.detach();
                }
            });
            btns.appendChild(bTutup);

            w.doOverlapped();
        } catch (Exception e) {
            // Struk bersifat best-effort; transaksi sudah tersimpan. Cukup catat bila gagal tampil.
            Common.tampilErrorJikaAdmin(e);
        }
    }

    /**
     * Cetak struk lewat iframe tersembunyi (bukan popup) sehingga tidak diblokir browser dan tetap
     * berjalan walau dipicu dari respons server (mis. setelah pembayaran berhasil / konfirmasi QRIS).
     * Dialog cetak dipanggil dari {@code onload} iframe agar dianggap inisiatif halaman itu sendiri.
     */
    private void cetakStruk(String html) {
        String mm = lebarStrukMm();
        String doc = "<html><head><meta charset=\"UTF-8\"><title>Struk</title>"
                + "<style>@page{size:" + mm + "mm auto;margin:0;}html,body{margin:0;padding:0;}*{box-sizing:border-box;}"
                + "body{width:" + mm + "mm;font-family:monospace;font-size:11px;color:#000;padding:4px 5px;}</style></head>"
                + "<body onload=\"setTimeout(function(){try{window.focus();window.print();}catch(e){}},180);\">"
                + html + "</body></html>";
        String js = "(function(){try{var f=document.getElementById('aisStrukFrame');"
                + "if(f&&f.parentNode){f.parentNode.removeChild(f);}"
                + "f=document.createElement('iframe');f.id='aisStrukFrame';"
                + "f.style.position='fixed';f.style.width='0';f.style.height='0';f.style.border='0';"
                + "f.style.right='0';f.style.bottom='0';document.body.appendChild(f);"
                + "var d=f.contentWindow.document;d.open();d.write(" + jsLiteral(doc) + ");d.close();"
                + "}catch(e){}})();";
        org.zkoss.zk.ui.util.Clients.evalJavaScript(js);
    }

    /** Kepala struk: logo + nama, alamat, telepon institusi (di-cache; tampil di atas nama toko). */
    private String strukHeaderHtml() {
        if (strukHeaderCache != null) {
            return strukHeaderCache;
        }
        StringBuilder s = new StringBuilder();
        s.append("<div style='text-align:center;'>");
        try {
            String media = ais.action.master.helper.util.PerguruanTinggiUtil
                    .getPerguruanTinggiMedia("logo_perguruanTinggi_");
            if (media != null && !media.trim().isEmpty()) {
                String src = media.trim();
                if (!src.startsWith("http") && !src.startsWith("data:")) {
                    String base = Common.getRequestHostWithProtocol();
                    src = (base == null ? "" : base) + (src.startsWith("/") ? src : ("/" + src));
                }
                s.append("<img src='").append(src).append("' style='max-height:48px;max-width:92%;margin-bottom:3px;' ")
                        .append("onerror=\"this.style.display='none'\"/>");
            }
        } catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/koperasi/PosKantinAction.java:1569");
        }
        try {
            ais.database.model.PerguruanTinggi pt = ais.action.master.helper.util.PerguruanTinggiUtil
                    .getPerguruanTinggi();
            if (pt != null) {
                if (pt.getNama() != null && !pt.getNama().trim().isEmpty()) {
                    s.append("<div style='font-weight:800;font-size:13px;'>")
                            .append(DashboardUiKit.esc(pt.getNama())).append("</div>");
                }
                if (pt.getAlamat1() != null && !pt.getAlamat1().trim().isEmpty()) {
                    s.append("<div style='font-size:9px;line-height:1.3;'>")
                            .append(DashboardUiKit.esc(pt.getAlamat1())).append("</div>");
                }
                if (pt.getTelepon() != null && !pt.getTelepon().trim().isEmpty()) {
                    s.append("<div style='font-size:9px;'>Telp: ")
                            .append(DashboardUiKit.esc(pt.getTelepon())).append("</div>");
                }
            }
        } catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/koperasi/PosKantinAction.java:1588");
        }
        s.append("</div>");
        strukHeaderCache = s.toString();
        return strukHeaderCache;
    }

    /** Persentase pajak (PPN) dari konfigurasi {@code pajak_persen_kantin} (default 0 = nonaktif). */
    private static double bacaPajakPersen() {
        try {
            String v = Common.getKonfigurasi("pajak_persen_kantin", "0").getNilai();
            if (v != null) {
                v = v.replaceAll("[^0-9.]", "");
            }
            if (v == null || v.isEmpty()) {
                return 0;
            }
            double p = Double.parseDouble(v);
            return p < 0 ? 0 : p;
        } catch (Exception e) {
            return 0;
        }
    }

    private static String fmtPersen(double p) {
        if (p == Math.floor(p)) {
            return String.valueOf((long) p);
        }
        return String.valueOf(p);
    }

    /** Lebar kertas struk (mm) dari konfigurasi {@code lebar_kertas_struk_mm} (default 58; umum 58/80). */
    private static String lebarStrukMm() {
        try {
            String v = Common.getKonfigurasi("lebar_kertas_struk_mm", "58").getNilai();
            if (v != null) {
                v = v.replaceAll("[^0-9]", "");
            }
            if (v == null || v.isEmpty()) {
                v = "58";
            }
            return v;
        } catch (Exception e) {
            return "58";
        }
    }

    private static String jsLiteral(String s) {
        if (s == null) {
            return "''";
        }
        StringBuilder b = new StringBuilder("'");
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\': b.append("\\\\"); break;
                case '\'': b.append("\\'"); break;
                case '\n': b.append("\\n"); break;
                case '\r': break;
                case '<': b.append("\\x3C"); break;
                case '>': b.append("\\x3E"); break;
                default: b.append(c);
            }
        }
        b.append("'");
        return b.toString();
    }

    private String buildStrukHtml(double total, double bayarTunai, double kembalian, String caraNama) {
        StringBuilder s = new StringBuilder();
        s.append("<div style='font-family:monospace;font-size:12px;color:#0f172a;width:100%;max-width:320px;margin:0 auto;'>");
        s.append(strukHeaderHtml());
        s.append("<div style='text-align:center;font-weight:800;font-size:13px;margin-top:3px;'>")
                .append(DashboardUiKit.esc(namaTokoAktif == null || namaTokoAktif.isEmpty() ? "Kantin" : namaTokoAktif))
                .append("</div>");
        s.append("<div style='text-align:center;font-size:10px;color:#475569;'>")
                .append(new java.text.SimpleDateFormat("dd-MM-yyyy HH:mm").format(new Date())).append("</div>");
        if (memberNama != null && !memberNama.isEmpty()) {
            s.append("<div style='font-size:10px;margin-top:2px;'>Member: ")
                    .append(DashboardUiKit.esc(memberNama)).append("</div>");
        }
        s.append("<div style='border-top:1px dashed #94a3b8;margin:6px 0;'></div>");
        double subtotal = 0, totDisk = 0, totCashback = 0;
        for (Item it : cart) {
            double sub = it.harga * it.jumlah;
            subtotal += sub;
            totDisk += it.diskon;
            totCashback += it.cashback;
            s.append("<div style='margin:3px 0;'><div>").append(DashboardUiKit.esc(it.nama)).append("</div>");
            s.append("<div style='display:flex;justify-content:space-between;'><span>").append(it.jumlah)
                    .append(" x ").append(DashboardUiKit.money(it.harga)).append("</span><span>")
                    .append(DashboardUiKit.money(sub)).append("</span></div>");
            if (it.diskon > 0) {
                s.append("<div style='display:flex;justify-content:space-between;color:#dc2626;'>")
                        .append("<span>diskon</span><span>-").append(DashboardUiKit.money(it.diskon)).append("</span></div>");
            }
            if (it.cashback > 0) {
                s.append("<div style='display:flex;justify-content:space-between;color:#16a34a;'>")
                        .append("<span>cashback</span><span>+").append(DashboardUiKit.money(it.cashback)).append("</span></div>");
            }
            s.append("</div>");
        }
        s.append("<div style='border-top:1px dashed #94a3b8;margin:6px 0;'></div>");
        s.append(barisStruk("Subtotal", DashboardUiKit.money(subtotal), false));
        if (totDisk > 0) {
            s.append(barisStruk("Diskon", "-" + DashboardUiKit.money(totDisk), false));
        }
        if (totCashback > 0) {
            s.append(barisStruk("Cashback", "+" + DashboardUiKit.money(totCashback), false));
        }
        if (grandPajak > 0) {
            s.append(barisStruk("Pajak (" + fmtPersen(pajakPersen) + "%)", DashboardUiKit.money(grandPajak), false));
        }
        s.append(barisStruk("TOTAL", DashboardUiKit.money(total), true));
        if (bayarTunai > 0) {
            s.append(barisStruk("Tunai", DashboardUiKit.money(bayarTunai), false));
            s.append(barisStruk("Kembali", DashboardUiKit.money(kembalian < 0 ? 0 : kembalian), false));
        }
        s.append("<div style='font-size:10px;margin-top:4px;'>Pembayaran: ")
                .append(DashboardUiKit.esc(caraNama == null ? "-" : caraNama)).append("</div>");
        s.append("<div style='text-align:center;font-size:10px;margin-top:10px;color:#475569;'>"
                + "Terima kasih atas kunjungan Anda</div>");
        s.append("</div>");
        return s.toString();
    }

    private static String barisStruk(String label, String nilai, boolean bold) {
        return "<div style='display:flex;justify-content:space-between;"
                + (bold ? "font-weight:800;font-size:13px;border-top:1px solid #cbd5e1;padding-top:3px;margin-top:3px;" : "")
                + "'><span>" + DashboardUiKit.esc(label) + "</span><span>" + nilai + "</span></div>";
    }

    // ======================== Limit promo harian ========================

    /**
     * Menghitung pemakaian potongan hari ini (per aturan, per toko, per member) untuk promo
     * "berlaku per hari &amp; per toko" — dari transaksi tersimpan maupun draft belum lunas, persis
     * logika versi JSP. Dipanggil saat member atau toko berubah.
     */
    private void updateUsageDiskon() {
        if (rules == null) {
            return;
        }
        for (Rule r : rules) {
            r.terpakaiHariIni = 0;
            r.terpakaiKeranjang = 0;
            if (!r.berlakuPerHari || memberId == null || tokoIdAktif == null || r.aturanId == null) {
                continue;
            }
            String sql = "SELECT COALESCE(SUM(terpakai),0) FROM ("
                    + "SELECT COALESCE(SUM(COALESCE(p.diskon,0)+COALESCE(p.cashback,0)),0) AS terpakai "
                    + "FROM koperasi.pembelian p "
                    + "LEFT JOIN koperasi.pembelian_anggota_koperasi pak ON p.pembelian_anggota_koperasi = pak.id "
                    + "WHERE p.aturan_diskon = " + r.aturanId + " AND p.toko = " + tokoIdAktif
                    + " AND pak.anggota_koperasi = " + memberId + " AND DATE(pak.tanggal_pembayaran) = CURRENT_DATE "
                    + "UNION ALL "
                    + "SELECT COALESCE(SUM(COALESCE(dp.diskon,0)+COALESCE(dp.cashback,0)),0) AS terpakai "
                    + "FROM koperasi.draft_pembelian dp "
                    + "LEFT JOIN koperasi.draft_pembelian_anggota_koperasi dpak "
                    + "ON dp.draft_pembelian_anggota_koperasi = dpak.id "
                    + "WHERE dp.aturan_diskon = " + r.aturanId + " AND dp.toko = " + tokoIdAktif
                    + " AND dpak.anggota_koperasi = " + memberId + " AND DATE(dpak.tanggal_pembayaran) = CURRENT_DATE "
                    + "AND dpak.lunas IS NULL) g";
            for (Object[] x : rows(sql)) {
                r.terpakaiHariIni = num(x[0]);
            }
        }
    }

    // ======================== Tab Riwayat Transaksi ========================

    private void buildRiwayat(org.zkoss.zul.Tabpanel panel) {
        panel.getChildren().clear();

        Div card = new Div();
        card.setSclass("ais-crud-filter-card");
        card.setParent(panel);
        Div body = new Div();
        body.setSclass("ais-crud-filter-body");
        body.setParent(card);
        body.appendChild(DashboardUiKit.html(DashboardUiKit.descChip(
                "Transaksi yang sudah tercatat. Saring berdasarkan tanggal, pembeli, atau barang, "
                        + "lalu cetak ulang struk atau unduh ke Excel.")));

        Div f = new Div();
        f.setStyle("display:flex;flex-wrap:wrap;gap:10px;align-items:flex-end;");
        f.setParent(body);

        dtMulai = new org.zkoss.zul.Datebox();
        dtMulai.setWidth("140px");
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.add(java.util.Calendar.MONTH, -3);
        dtMulai.setValue(cal.getTime());
        f.appendChild(kolom("Tanggal Mulai", dtMulai));

        dtAkhir = new org.zkoss.zul.Datebox();
        dtAkhir.setWidth("140px");
        dtAkhir.setValue(new Date());
        f.appendChild(kolom("Tanggal Akhir", dtAkhir));

        txtCariPembeli = new Textbox();
        txtCariPembeli.setWidth("160px");
        f.appendChild(kolom("Nama Pembeli", txtCariPembeli));

        txtCariBarang = new Textbox();
        txtCariBarang.setWidth("160px");
        f.appendChild(kolom("Nama Barang", txtCariBarang));

        Button bTerap = new Button("Terapkan Filter");
        bTerap.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event e) throws Exception {
                loadRiwayat();
            }
        });
        Button bExcel = new Button("Unduh Excel");
        bExcel.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event e) throws Exception {
                downloadRiwayatExcel();
            }
        });
        Hlayout act = new Hlayout();
        act.setStyle("gap:6px;");
        act.appendChild(bTerap);
        act.appendChild(bExcel);
        f.appendChild(kolom(" ", act));

        Div dataCard = new Div();
        dataCard.setSclass("ais-crud-data-card");
        dataCard.setParent(panel);

        grdRiwayat = new Grid();
        grdRiwayat.setSclass("dgrid");
        grdRiwayat.setWidth("100%");
        grdRiwayat.setStyle("border:0;background:transparent;");
        grdRiwayat.setMold("paging");
        grdRiwayat.setPageSize(15);
        grdRiwayat.setParent(dataCard);
        Columns c = new Columns();
        c.setParent(grdRiwayat);
        addCol(c, "Waktu", "130px");
        if (scopeToko == null) {
            addCol(c, "Pedagang", "140px");
        }
        addCol(c, "Rincian Barang", null);
        addCol(c, "Pembeli", "140px");
        addCol(c, "Metode", "110px");
        addCol(c, "Qty", "60px");
        addCol(c, "Total", "120px");
        addCol(c, "", "90px");

        loadRiwayat();
    }

    private Div kolom(String label, Component field) {
        Div d = new Div();
        d.setStyle("display:flex;flex-direction:column;");
        d.appendChild(labelMini(label));
        d.appendChild(field);
        return d;
    }

    private String riwayatSelectSql() {
        String tglA = dtMulai != null && dtMulai.getValue() != null
                ? new java.text.SimpleDateFormat("yyyy-MM-dd").format(dtMulai.getValue()) : "2000-01-01";
        String tglB = dtAkhir != null && dtAkhir.getValue() != null
                ? new java.text.SimpleDateFormat("yyyy-MM-dd").format(dtAkhir.getValue()) : "2999-12-31";
        StringBuilder w = new StringBuilder(" WHERE DATE(a.waktu) BETWEEN DATE('" + tglA + "') AND DATE('" + tglB + "') ");
        if (scopeToko != null) {
            w.append(" AND a.toko = ").append(scopeToko.getId()).append(" ");
        }
        String pembeli = txtCariPembeli == null ? "" : txtCariPembeli.getValue().trim().replace("'", "''");
        String barang = txtCariBarang == null ? "" : txtCariBarang.getValue().trim().replace("'", "''");
        if (!pembeli.isEmpty()) {
            w.append(" AND a.member ILIKE '%").append(pembeli).append("%' ");
        }
        if (!barang.isEmpty()) {
            w.append(" AND c.nama ILIKE '%").append(barang).append("%' ");
        }
        return "SELECT COALESCE(a.pembelian_anggota_koperasi, a.id) AS id_trx, "
                + "TO_CHAR(MAX(a.waktu),'DD-MM-YYYY HH24:MI') AS waktu, "
                + "STRING_AGG(c.nama || ' (' || a.qty || ')', ', ') AS barang, "
                + "MAX(a.member) AS member, MAX(b.nama) AS pedagang, MAX(a.carabayar) AS carabayar, "
                + "SUM(a.qty) AS qty, SUM(a.total) AS total "
                + "FROM koperasi.pembelian a "
                + "INNER JOIN koperasi.toko b ON (a.toko = b.id) "
                + "INNER JOIN koperasi.produk c ON (c.id = a.produk) "
                + w.toString()
                + " GROUP BY COALESCE(a.pembelian_anggota_koperasi, a.id) "
                + " ORDER BY MAX(a.waktu) DESC LIMIT 500";
    }

    private void loadRiwayat() {
        if (grdRiwayat == null) {
            return;
        }
        Rows rows = new Rows();
        List<Object[]> data = rows(riwayatSelectSql());
        if (data.isEmpty()) {
            tambahPesanProduk(rows, ais.common.Common.getBahasaConfig("Belum ada transaksi pada rentang ini."));
            ganti(grdRiwayat, rows);
            return;
        }
        for (Object[] d : data) {
            final Long idTrx = lng(d[0]);
            Row row = new Row();
            row.setValign("top");
            row.setParent(rows);
            new Label(str(d[1])).setParent(row);
            if (scopeToko == null) {
                Label lp = new Label(str(d[4]));
                lp.setStyle("font-weight:600;");
                lp.setParent(row);
            }
            Label lb = new Label(str(d[2]));
            lb.setStyle("color:#1d4ed8;");
            lb.setParent(row);
            new Label(str(d[3]).isEmpty() ? "Umum" : str(d[3])).setParent(row);
            new Label(str(d[5])).setParent(row);
            new Label(String.valueOf((long) num(d[6]))).setParent(row);
            Label lt = new Label("Rp " + DashboardUiKit.money(num(d[7])));
            lt.setStyle("font-weight:700;color:#16a34a;");
            lt.setParent(row);
            Button cetak = new Button("Cetak");
            cetak.setTooltiptext("Cetak ulang struk transaksi ini");
            cetak.addEventListener("onClick", new EventListener() {
                @Override
                public void onEvent(Event e) throws Exception {
                    cetakStrukById(idTrx);
                }
            });
            cetak.setParent(row);
        }
        ganti(grdRiwayat, rows);
    }

    /** Cetak ulang struk transaksi lama: pakai ulang halaman struk JSP yang sudah ada (reuse). */
    private void cetakStrukById(Long idTrx) {
        if (idTrx == null) {
            return;
        }
        String url = Common.ROOT + "/baru?hanya_tampil_jsp=true&p=kantin%2Fpos&s=cetak_struk&id=" + idTrx;
        org.zkoss.zk.ui.util.Clients.evalJavaScript(
                "window.open('" + url + "','CetakStruk','height=640,width=420');");
    }

    private void downloadRiwayatExcel() {
        try {
            List<Object[]> data = rows(riwayatSelectSql());
            org.zkoss.poi.ss.usermodel.Workbook wb = new org.zkoss.poi.xssf.usermodel.XSSFWorkbook();
            org.zkoss.poi.ss.usermodel.Sheet sheet = wb.createSheet("Transaksi");
            int rownum = 0;
            org.zkoss.poi.ss.usermodel.Row head = sheet.createRow(rownum++);
            String[] cols = scopeToko == null
                    ? new String[] { "Waktu", "Pedagang", "Rincian Barang", "Pembeli", "Metode", "Qty", "Total" }
                    : new String[] { "Waktu", "Rincian Barang", "Pembeli", "Metode", "Qty", "Total" };
            for (int i = 0; i < cols.length; i++) {
                head.createCell(i).setCellValue(cols[i]);
            }
            for (Object[] d : data) {
                org.zkoss.poi.ss.usermodel.Row r = sheet.createRow(rownum++);
                int ci = 0;
                r.createCell(ci++).setCellValue(str(d[1]));
                if (scopeToko == null) {
                    r.createCell(ci++).setCellValue(str(d[4]));
                }
                r.createCell(ci++).setCellValue(str(d[2]));
                r.createCell(ci++).setCellValue(str(d[3]).isEmpty() ? "Umum" : str(d[3]));
                r.createCell(ci++).setCellValue(str(d[5]));
                r.createCell(ci++).setCellValue(num(d[6]));
                r.createCell(ci++).setCellValue(num(d[7]));
            }
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            wb.write(baos);
            org.zkoss.zul.Filedownload.save(baos.toByteArray(),
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    "Riwayat_Transaksi_" + new java.text.SimpleDateFormat("yyyyMMdd").format(new Date()) + ".xlsx");
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
        }
    }

    // ======================== Util kecil ========================

    /** Gaya (CSS) tampilan kasir modern — scoped di bawah .pskasir agar tidak bocor ke halaman lain. */
    private static String posStyle() {
        return "<style>"
                + ".pskasir{font-size:13px;color:#0f172a;}"
                + ".psk-head{display:flex;flex-wrap:wrap;gap:10px;align-items:center;justify-content:space-between;"
                + "background:#fff;border:1px solid #e2e8f0;border-radius:14px;padding:10px 12px;margin-bottom:12px;"
                + "box-shadow:0 2px 8px rgba(15,23,42,.05);}"
                + ".psk-store{display:flex;align-items:center;gap:8px;font-weight:800;}"
                + ".psk-store-ic{width:34px;height:34px;border-radius:10px;background:rgba(37,99,235,.1);color:#2563eb;"
                + "display:inline-flex;align-items:center;justify-content:center;font-size:17px;}"
                + ".psk-sesikas{display:flex;align-items:center;gap:10px;background:#f8fafc;border:1px solid #e2e8f0;"
                + "border-radius:10px;padding:6px 12px;}"
                + ".psk-sesikas-form{background:#f8fafc;border:1px dashed #cbd5e1;border-radius:14px;padding:12px 14px;"
                + "margin:-2px 0 12px;}"
                + ".psk-search{flex:1 1 240px;display:flex;align-items:center;gap:8px;background:#f1f5f9;"
                + "border:1px solid #e2e8f0;border-radius:12px;padding:3px 10px;}"
                + ".psk-search-ic{color:#94a3b8;font-size:15px;}"
                + ".psk-search .z-textbox,.psk-search input{border:0 !important;background:transparent !important;"
                + "box-shadow:none !important;width:100% !important;}"
                + ".psk-member{display:flex;align-items:center;gap:8px;}"
                + ".psk-mini{font-size:11px;font-weight:700;color:#64748b;}"
                + ".psk-body{display:flex;flex-wrap:wrap;gap:14px;align-items:flex-start;}"
                + ".psk-left{flex:2 1 380px;min-width:300px;}"
                + ".psk-right{flex:1 1 340px;min-width:300px;background:#fff;border:1px solid #e2e8f0;"
                + "border-radius:16px;padding:14px;box-shadow:0 6px 18px rgba(15,23,42,.06);}"
                + ".psk-title{font-weight:800;font-size:15px;margin:2px 0 10px;}"
                + ".psk-grid{display:flex;flex-wrap:wrap;gap:10px;max-height:64vh;overflow:auto;padding:2px;}"
                + ".psk-card{flex:1 1 150px;max-width:210px;min-width:140px;background:#fff;border:1px solid #e2e8f0;"
                + "border-radius:14px;padding:10px 10px 12px;box-shadow:0 2px 8px rgba(15,23,42,.05);position:relative;"
                + "cursor:pointer;transition:transform .12s,box-shadow .12s,border-color .12s;}"
                + ".psk-card:hover{transform:translateY(-3px);box-shadow:0 10px 22px rgba(15,23,42,.13);border-color:#bfdbfe;}"
                + ".psk-ico{position:relative;overflow:hidden;width:100%;height:84px;border-radius:10px;"
                + "background:rgba(37,99,235,.06);color:#2563eb;display:flex;align-items:center;justify-content:center;"
                + "font-size:30px;margin-bottom:8px;}"
                + ".psk-img{position:absolute;top:0;left:0;width:100%;height:100%;object-fit:cover;}"
                + ".psk-nm{font-weight:700;font-size:12.5px;line-height:1.3;min-height:32px;}"
                + ".psk-pr{font-weight:800;color:#2563eb;font-size:14px;margin-top:4px;}"
                + ".psk-st{font-size:10.5px;color:#64748b;margin-top:2px;}"
                + ".psk-add{position:absolute;right:8px;bottom:8px;width:28px;height:28px;border-radius:9px;"
                + "background:#2563eb;color:#fff;font-weight:900;font-size:17px;display:flex;align-items:center;"
                + "justify-content:center;box-shadow:0 4px 10px rgba(37,99,235,.35);}"
                + ".psk-card:hover .psk-add{background:#1d4ed8;}"
                + ".psk-cart-head{display:flex;justify-content:space-between;align-items:center;}"
                + ".psk-link{color:#dc2626;font-size:11.5px;font-weight:700;cursor:pointer;display:inline-block;}"
                + ".psk-cart{display:flex;flex-direction:column;gap:8px;max-height:34vh;overflow:auto;margin:6px 0 10px;}"
                + ".psk-citem{display:flex;align-items:center;gap:8px;border:1px solid #eef2f7;border-radius:12px;"
                + "padding:8px;background:#fff;}"
                + ".psk-cnm{font-weight:700;font-size:12.5px;}"
                + ".psk-cpr{font-size:10.5px;color:#64748b;}"
                + ".psk-qty{display:flex;align-items:center;gap:4px;}"
                + ".psk-qbtn{display:inline-block;width:24px;height:24px;border-radius:7px;border:1px solid #e2e8f0;"
                + "background:#f8fafc;font-weight:800;text-align:center;line-height:22px;cursor:pointer;}"
                + ".psk-qbtn:hover{background:#e2e8f0;}"
                + ".psk-qn{display:inline-block;min-width:20px;text-align:center;font-weight:800;}"
                + ".psk-csub{display:inline-block;font-weight:800;color:#16a34a;font-size:12px;min-width:74px;text-align:right;}"
                + ".psk-cdel{display:inline-block;color:#94a3b8;font-weight:900;cursor:pointer;padding:0 2px;}"
                + ".psk-cdel:hover{color:#dc2626;}"
                + ".psk-sum{background:#f8fafc;border:1px solid #e2e8f0;border-radius:12px;padding:12px;}"
                + ".psk-pay{display:flex;flex-wrap:wrap;gap:8px;}"
                + ".psk-paybtn{flex:1 1 84px;min-width:80px;border:1px solid #e2e8f0;border-radius:12px;background:#fff;"
                + "padding:9px 6px;text-align:center;cursor:pointer;font-weight:700;font-size:11px;color:#334155;transition:.12s;}"
                + ".psk-paybtn:hover{border-color:#2563eb;}"
                + ".psk-paybtn.sel{border-color:#2563eb;background:rgba(37,99,235,.08);color:#1d4ed8;"
                + "box-shadow:0 0 0 2px rgba(37,99,235,.18) inset;}"
                + ".psk-pay-ic{font-size:18px;margin-bottom:3px;}"
                + ".psk-bayar{margin-top:12px;background:linear-gradient(135deg,#4f46e5,#2563eb);color:#fff;"
                + "font-weight:800;font-size:15px;text-align:center;border-radius:14px;padding:14px;cursor:pointer;"
                + "box-shadow:0 10px 22px rgba(37,99,235,.32);}"
                + ".psk-bayar:hover{filter:brightness(1.06);}"
                + ".psk-tahan{background:#fff;color:#2563eb;font-weight:800;font-size:13px;text-align:center;"
                + "border:1.5px solid #2563eb;border-radius:14px;padding:14px 16px;cursor:pointer;white-space:nowrap;}"
                + ".psk-tahan:hover{background:rgba(37,99,235,.08);}"
                + ".psk-tertahan-row{display:flex;justify-content:space-between;align-items:center;gap:8px;"
                + "padding:6px 4px;border-bottom:1px solid #e2e8f0;font-size:12px;}"
                + ".psk-tertahan-muat{color:#2563eb;font-weight:800;cursor:pointer;}"
                + ".psk-empty{font-size:12px;color:#64748b;background:#f8fafc;border:1px dashed #cbd5e1;"
                + "border-radius:10px;padding:14px;text-align:center;width:100%;}"
                + ".psk-cat{display:flex;flex-wrap:nowrap;gap:8px;overflow-x:auto;overflow-y:hidden;padding-bottom:4px;"
                + "scrollbar-width:thin;}"
                + ".psk-cat::-webkit-scrollbar{height:5px;}"
                + ".psk-cat::-webkit-scrollbar-thumb{background:#cbd5e1;border-radius:999px;}"
                + ".psk-cat-chip{display:inline-block;flex-shrink:0;white-space:nowrap;padding:7px 14px;"
                + "border:1px solid #e2e8f0;border-radius:11px;"
                + "background:#fff;cursor:pointer;font-weight:700;font-size:12px;color:#334155;transition:.12s;}"
                + ".psk-cat-chip:hover{border-color:#2563eb;}"
                + ".psk-cat-chip.sel{background:#2563eb;border-color:#2563eb;color:#fff;box-shadow:0 4px 10px rgba(37,99,235,.3);}"
                + ".psk-cust{background:#f8fafc;border:1px solid #e2e8f0;border-radius:12px;padding:10px 12px;margin-bottom:12px;}"
                + ".psk-statrow{display:flex;flex-wrap:wrap;gap:10px;margin-top:12px;}"
                + ".psk-stat{flex:1 1 150px;min-width:140px;background:#fff;border:1px solid #e2e8f0;border-radius:14px;"
                + "padding:10px 12px;box-shadow:0 2px 8px rgba(15,23,42,.05);}"
                + ".psk-stat-lbl{font-size:10px;text-transform:uppercase;letter-spacing:.04em;font-weight:800;color:#64748b;}"
                + ".psk-stat-val{font-size:18px;font-weight:900;color:#0f172a;margin-top:3px;}"
                + ".psk-stat-delta{font-size:10.5px;font-weight:700;margin-top:2px;}"
                + ".psk-stat-delta.up{color:#16a34a;}"
                + ".psk-stat-delta.down{color:#dc2626;}"
                + "</style>";
    }

    /**
     * <h2>Panel dashboard ringan Kasir: Inventori, Riwayat, dan Analitik.</h2>
     *
     * <p>
     * Ketiga method berikut ({@link #loadRingkasanInventori()}, {@link #loadMiniRiwayat()},
     * {@link #loadAnalitikKasir()}) membentuk satu kesatuan "panel dashboard" yang ditampilkan
     * langsung di layar Kasir (bukan di tab terpisah), tepat di bawah area keranjang/checkout —
     * meniru rancangan referensi UI POS modern (mockup "NexaPOS") yang diminta pengguna. Tujuan
     * bisnisnya: seorang kasir yang sedang bertugas bisa, TANPA berpindah menu, langsung tahu (1)
     * barang apa yang stoknya menipis dan perlu segera diminta ke gudang, (2) transaksi apa saja
     * yang baru terjadi (mengecek ulang transaksi yang salah input), dan (3) apakah performa
     * toko hari-hari ini sedang naik atau turun dibanding minggu sebelumnya — tanpa perlu membuka
     * laporan terpisah yang biasanya hanya diakses admin/pemilik.
     * </p>
     *
     * <h3>Kenapa dipisah jadi 3 method, bukan 1 method besar?</h3>
     * <p>
     * Supaya masing-masing bisa dipanggil ulang (refresh) SECARA INDEPENDEN sesuai kebutuhan —
     * mis. setelah transaksi sukses, ketiganya perlu disegarkan sekaligus; tapi saat kasir hanya
     * mengganti filter kategori produk, tak satu pun dari ketiganya perlu disentuh. Memisahkan
     * method juga membuat masing-masing lebih mudah diuji/dibaca dan mencegah 1 method raksasa
     * yang sulit dirawat (prinsip <i>single responsibility</i>).
     * </p>
     *
     * <h3>Kenapa pakai {@link HtmlChartHelper} bukan gambar/JFreeChart?</h3>
     * <p>
     * Sesuai konvensi dasbor lain di aplikasi ini ({@code DashboardRekapNilaiMahasiswa} dkk.):
     * grafik dirender murni HTML+CSS/SVG di sisi browser, BUKAN gambar yang di-generate server
     * (JFreeChart). Ini membuat panel ringan (tak ada I/O gambar ke disk), otomatis responsif di
     * ponsel, dan seragam gaya dengan dasbor lain di aplikasi — cukup satu kelas utilitas yang
     * dipakai ulang di mana-mana, bukan menulis ulang logika chart di setiap file.
     * </p>
     *
     * <h3>Sumber data & keamanan angka</h3>
     * <p>
     * Semua query dibatasi eksplisit dengan {@code tokoIdAktif} (toko yang sedang login/dipilih
     * kasir) — TIDAK PERNAH menampilkan data toko lain. Bila {@code tokoIdAktif} masih kosong
     * (admin belum memilih toko dari combobox), ketiga panel menampilkan pesan ajakan memilih
     * toko, BUKAN query tanpa filter (yang berisiko membocorkan data toko lain ke layar kasir).
     * Semua nilai numerik dari SQL dibaca lewat helper {@link #num(Object)}/{@link #str(Object)}
     * yang sudah aman terhadap NULL, sehingga tak ada risiko NullPointerException saat toko baru
     * belum punya transaksi/produk sama sekali.
     * </p>
     *
     * <h3>Kapan method-method ini dipanggil</h3>
     * <ul>
     * <li>Saat layar Kasir pertama kali dibangun ({@code buildUI()}).</li>
     * <li>Saat admin mengganti toko aktif lewat combobox (listener {@code onSelect} pada
     * {@code cboToko}).</li>
     * <li>Setelah transaksi berhasil dibayar (di {@code onBayar()}/handler hasil pembayaran) —
     * supaya stok, riwayat, dan angka analitik langsung mencerminkan transaksi yang baru saja
     * terjadi tanpa kasir perlu me-refresh halaman secara manual.</li>
     * </ul>
     */
    private void loadRingkasanInventori() {
        if (invRingkasanBox == null) {
            return;
        }
        invRingkasanBox.getChildren().clear();
        if (tokoIdAktif == null) {
            invRingkasanBox.appendChild(DashboardUiKit.html(
                    "<div class='psk-empty'>Pilih toko/pedagang dahulu.</div>"));
            return;
        }
        String cond = " AND toko = " + tokoIdAktif;
        List<Object[]> res = rows("SELECT COUNT(*), "
                + "SUM(CASE WHEN COALESCE(stok,0) > 0 AND COALESCE(stok,0) <= 5 THEN 1 ELSE 0 END), "
                + "SUM(CASE WHEN COALESCE(stok,0) <= 0 THEN 1 ELSE 0 END), "
                + "COALESCE(SUM(COALESCE(stok,0) * COALESCE(hargabeli, hargajual, 0)), 0) "
                + "FROM koperasi.produk WHERE aktif = true" + cond);
        Object[] r = res.isEmpty() ? new Object[] { 0, 0, 0, 0 } : res.get(0);
        String kpi = HtmlChartHelper.kpiCards(
                new String[] { "Total Produk", "Stok Rendah", "Stok Habis", "Nilai Stok" },
                new String[] { DashboardUiKit.money(num(r[0])), DashboardUiKit.money(num(r[1])),
                        DashboardUiKit.money(num(r[2])), "Rp " + DashboardUiKit.money(num(r[3])) },
                null, null, null,
                new String[] { "#0f172a", "#d97706", "#dc2626", "#16a34a" });
        invRingkasanBox.appendChild(DashboardUiKit.html(kpi));
    }

    /** Tabel mini "Riwayat Transaksi": 5 transaksi terakhir milik toko aktif. */
    private void loadMiniRiwayat() {
        if (miniRiwayatBox == null) {
            return;
        }
        miniRiwayatBox.getChildren().clear();
        if (tokoIdAktif == null) {
            miniRiwayatBox.appendChild(DashboardUiKit.html(
                    "<div class='psk-empty'>Pilih toko/pedagang dahulu.</div>"));
            return;
        }
        List<Object[]> res = rows("SELECT MAX(waktu), MAX(member), MAX(carabayar), SUM(total) "
                + "FROM koperasi.pembelian WHERE toko = " + tokoIdAktif
                + " GROUP BY COALESCE(pembelian_anggota_koperasi, id) ORDER BY MAX(waktu) DESC LIMIT 5");
        if (res.isEmpty()) {
            miniRiwayatBox.appendChild(DashboardUiKit.html("<div class='psk-empty'>Belum ada transaksi.</div>"));
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("<table style='width:100%;border-collapse:collapse;font-size:12px;'>");
        sb.append("<thead><tr style='color:#64748b;text-transform:uppercase;font-size:10px;text-align:left;'>")
                .append("<th style='padding:4px 6px;'>Waktu</th><th style='padding:4px 6px;'>Pembeli</th>")
                .append("<th style='padding:4px 6px;text-align:right;'>Total</th>")
                .append("<th style='padding:4px 6px;text-align:center;'>Metode</th></tr></thead><tbody>");
        for (Object[] r : res) {
            String member = str(r[1]);
            sb.append("<tr style='border-top:1px solid #f1f5f9;'>")
                    .append("<td style='padding:5px 6px;color:#64748b;'>").append(DashboardUiKit.esc(str(r[0])))
                    .append("</td><td style='padding:5px 6px;'>")
                    .append(DashboardUiKit.esc(member.isEmpty() ? "Walk-in Customer" : member))
                    .append("</td><td style='padding:5px 6px;text-align:right;font-weight:800;color:#16a34a;'>Rp ")
                    .append(DashboardUiKit.money(num(r[3])))
                    .append("</td><td style='padding:5px 6px;text-align:center;'><span style='background:#f1f5f9;")
                    .append("border-radius:999px;padding:2px 8px;font-size:10.5px;'>")
                    .append(DashboardUiKit.esc(str(r[2]))).append("</span></td></tr>");
        }
        sb.append("</tbody></table>");
        miniRiwayatBox.appendChild(DashboardUiKit.html(sb.toString()));
    }

    /**
     * Panel "Analitik Kasir": donut komposisi penjualan per kategori barang (7 hari terakhir) +
     * radar/jaring perbandingan 4 ukuran keberhasilan toko (Penjualan, Transaksi, Rata-rata,
     * Item Terjual) minggu ini vs minggu lalu. Lihat javadoc {@link #loadRingkasanInventori()}
     * untuk konteks lengkap kenapa panel ini ada & kapan dipanggil ulang.
     *
     * <p>
     * Nilai radar dinormalisasi 0-100 per sumbu (dibagi terhadap nilai terbesar antara minggu
     * ini/minggu lalu pada sumbu itu) karena satuan aslinya berbeda jauh (Rupiah vs jumlah
     * transaksi) — tanpa normalisasi, sumbu "Penjualan" (jutaan rupiah) akan selalu menutupi
     * sumbu "Transaksi" (puluhan) sehingga bentuk jaring tak bermakna dibaca oleh pengguna awam.
     * </p>
     */
    private void loadAnalitikKasir() {
        if (analitikKasirBox == null) {
            return;
        }
        analitikKasirBox.getChildren().clear();
        if (tokoIdAktif == null) {
            analitikKasirBox.appendChild(DashboardUiKit.html(
                    "<div class='psk-empty'>Pilih toko/pedagang dahulu.</div>"));
            return;
        }
        String condA = " AND a.toko = " + tokoIdAktif;
        String cond = " AND toko = " + tokoIdAktif;

        // ---- Donut: penjualan per kategori (jenis_produk), 7 hari terakhir ----
        List<Object[]> resDonut = rows("SELECT COALESCE(jp.nama, 'Lainnya') AS kategori, "
                + "COALESCE(SUM(a.total),0) AS jumlah FROM koperasi.pembelian a "
                + "LEFT JOIN koperasi.produk p ON p.id = a.produk "
                + "LEFT JOIN koperasi.jenis_produk jp ON jp.id = p.jenis_produk "
                + "WHERE a.waktu >= current_date - interval '6 days'" + condA
                + " GROUP BY jp.nama ORDER BY jumlah DESC LIMIT 6");
        String[] labelsDonut = new String[resDonut.size()];
        double[] valuesDonut = new double[resDonut.size()];
        for (int i = 0; i < resDonut.size(); i++) {
            labelsDonut[i] = str(resDonut.get(i)[0]);
            valuesDonut[i] = num(resDonut.get(i)[1]);
        }
        String donut = HtmlChartHelper.donut("Penjualan per Kategori",
                "Jenis barang apa yang paling banyak menyumbang penjualan minggu ini.", labelsDonut, valuesDonut,
                null, "penjualan");

        // ---- Radar: 4 ukuran kinerja minggu ini vs minggu lalu ----
        List<Object[]> resStat = rows("SELECT "
                + "COALESCE(SUM(CASE WHEN waktu >= current_date - interval '6 days' THEN total ELSE 0 END),0), "
                + "COALESCE(SUM(CASE WHEN waktu < current_date - interval '6 days' "
                + "AND waktu >= current_date - interval '13 days' THEN total ELSE 0 END),0), "
                + "COUNT(DISTINCT CASE WHEN waktu >= current_date - interval '6 days' "
                + "THEN COALESCE(pembelian_anggota_koperasi, id) END), "
                + "COUNT(DISTINCT CASE WHEN waktu < current_date - interval '6 days' "
                + "AND waktu >= current_date - interval '13 days' THEN COALESCE(pembelian_anggota_koperasi, id) END), "
                + "COALESCE(SUM(CASE WHEN waktu >= current_date - interval '6 days' THEN qty ELSE 0 END),0), "
                + "COALESCE(SUM(CASE WHEN waktu < current_date - interval '6 days' "
                + "AND waktu >= current_date - interval '13 days' THEN qty ELSE 0 END),0) "
                + "FROM koperasi.pembelian WHERE waktu >= current_date - interval '13 days'" + cond);
        Object[] rs = resStat.isEmpty() ? new Object[] { 0, 0, 0, 0, 0, 0 } : resStat.get(0);
        double totalNow = num(rs[0]), totalPrev = num(rs[1]);
        double trxNow = num(rs[2]), trxPrev = num(rs[3]);
        double qtyNow = num(rs[4]), qtyPrev = num(rs[5]);
        double rataNow = trxNow > 0 ? totalNow / trxNow : 0;
        double rataPrev = trxPrev > 0 ? totalPrev / trxPrev : 0;

        double[] now100 = new double[] { skala100(totalNow, totalPrev), skala100(trxNow, trxPrev),
                skala100(rataNow, rataPrev), skala100(qtyNow, qtyPrev) };
        double[] prev100 = new double[] { skala100(totalPrev, totalNow), skala100(trxPrev, trxNow),
                skala100(rataPrev, rataNow), skala100(qtyPrev, qtyNow) };
        String radar = HtmlChartHelper.radar("Perbandingan Kinerja",
                "Membandingkan 4 ukuran keberhasilan toko minggu ini vs minggu lalu -- makin lebar "
                        + "bentuknya, makin baik performanya.",
                new String[] { "Penjualan", "Transaksi", "Rata-rata", "Item Terjual" },
                new String[] { "Minggu Ini", "Minggu Lalu" }, new double[][] { now100, prev100 },
                new String[] { "#2563eb", "#94a3b8" }, 100);

        analitikKasirBox.appendChild(DashboardUiKit.html(donut + radar));
    }

    /** Menskalakan {@code nilai} relatif thd {@code pembanding} ke rentang 0-100 (lihat javadoc {@link #loadAnalitikKasir()}). */
    private static double skala100(double nilai, double pembanding) {
        double maks = Math.max(Math.max(nilai, pembanding), 1);
        return Math.round(nilai / maks * 100.0);
    }

    private void loadRingkasanHariIni() {
        if (ringkasanBox == null) {
            return;
        }
        ringkasanBox.getChildren().clear();
        String cond = (tokoIdAktif != null) ? (" AND toko = " + tokoIdAktif) : "";
        java.text.SimpleDateFormat df = new java.text.SimpleDateFormat("yyyy-MM-dd");
        java.util.Calendar cal = java.util.Calendar.getInstance();
        List<String> days = new ArrayList<String>();
        for (int i = 6; i >= 0; i--) {
            java.util.Calendar c = (java.util.Calendar) cal.clone();
            c.add(java.util.Calendar.DATE, -i);
            days.add(df.format(c.getTime()));
        }
        java.util.Map<String, double[]> map = new java.util.HashMap<String, double[]>();
        for (Object[] r : rows("SELECT TO_CHAR(DATE(waktu),'YYYY-MM-DD'), COALESCE(SUM(total),0), "
                + "COUNT(DISTINCT COALESCE(pembelian_anggota_koperasi, id)), COALESCE(SUM(qty),0) "
                + "FROM koperasi.pembelian WHERE waktu >= current_date - interval '6 days'" + cond
                + " GROUP BY DATE(waktu)")) {
            map.put(str(r[0]), new double[] { num(r[1]), num(r[2]), num(r[3]) });
        }
        List<Double> sTotal = new ArrayList<Double>();
        List<Double> sTrx = new ArrayList<Double>();
        List<Double> sQty = new ArrayList<Double>();
        List<Double> sAvg = new ArrayList<Double>();
        for (String d : days) {
            double[] v = map.containsKey(d) ? map.get(d) : new double[] { 0, 0, 0 };
            sTotal.add(Double.valueOf(v[0]));
            sTrx.add(Double.valueOf(v[1]));
            sQty.add(Double.valueOf(v[2]));
            sAvg.add(Double.valueOf(v[1] > 0 ? v[0] / v[1] : 0));
        }
        int last = days.size() - 1;
        double tTotal = sTotal.get(last), yTotal = last > 0 ? sTotal.get(last - 1) : 0;
        double tTrx = sTrx.get(last), yTrx = last > 0 ? sTrx.get(last - 1) : 0;
        double tQty = sQty.get(last), yQty = last > 0 ? sQty.get(last - 1) : 0;
        double tAvg = tTrx > 0 ? tTotal / tTrx : 0, yAvg = yTrx > 0 ? yTotal / yTrx : 0;

        StringBuilder sb = new StringBuilder();
        sb.append(statCard("Total Penjualan", "Rp " + DashboardUiKit.money(tTotal), delta(tTotal, yTotal),
                "#2563eb", sTotal));
        sb.append(statCard("Transaksi", DashboardUiKit.money(tTrx), delta(tTrx, yTrx), "#16a34a", sTrx));
        sb.append(statCard("Rata-rata Transaksi", "Rp " + DashboardUiKit.money(tAvg), delta(tAvg, yAvg),
                "#f97316", sAvg));
        sb.append(statCard("Produk Terjual", DashboardUiKit.money(tQty), delta(tQty, yQty), "#06b6d4", sQty));
        ringkasanBox.appendChild(DashboardUiKit.html(sb.toString()));
    }

    private static String statCard(String label, String value, String deltaHtml, String color, List<Double> series) {
        return "<div class='psk-stat'><div class='psk-stat-lbl'>" + DashboardUiKit.esc(label) + "</div>"
                + "<div class='psk-stat-val'>" + value + "</div>" + deltaHtml + miniSpark(series, color) + "</div>";
    }

    private static String delta(double today, double yesterday) {
        double pct = yesterday > 0 ? (today - yesterday) / yesterday * 100.0 : (today > 0 ? 100 : 0);
        boolean up = pct >= 0;
        return "<div class='psk-stat-delta " + (up ? "up" : "down") + "'>" + (up ? "▲" : "▼") + " "
                + DashboardUiKit.money(Math.abs(Math.round(pct))) + "% vs kemarin</div>";
    }

    private static String miniSpark(List<Double> vals, String color) {
        int w = 132, h = 34;
        if (vals == null || vals.isEmpty()) {
            return "";
        }
        double max = 0, min = Double.MAX_VALUE;
        for (Double v : vals) {
            double d = v == null ? 0 : v.doubleValue();
            if (d > max) {
                max = d;
            }
            if (d < min) {
                min = d;
            }
        }
        if (min == Double.MAX_VALUE) {
            min = 0;
        }
        double range = max - min;
        if (range <= 0) {
            range = 1;
        }
        int n = vals.size();
        StringBuilder pts = new StringBuilder();
        for (int i = 0; i < n; i++) {
            double d = vals.get(i) == null ? 0 : vals.get(i).doubleValue();
            double x = (n == 1) ? w : (w * i / (double) (n - 1));
            double y = h - 4 - ((d - min) / range) * (h - 8);
            if (i > 0) {
                pts.append(" ");
            }
            pts.append(String.format(java.util.Locale.US, "%.1f,%.1f", x, y));
        }
        return "<svg width='" + w + "' height='" + h + "' viewBox='0 0 " + w + " " + h + "' style='margin-top:6px;'>"
                + "<polyline fill='none' stroke='" + color + "' stroke-width='2' stroke-linecap='round' "
                + "stroke-linejoin='round' points='" + pts + "'/></svg>";
    }

    private static Label labelMini(String text) {
        Label l = new Label(text);
        l.setStyle("font-size:11px;font-weight:700;color:#64748b;display:block;margin-bottom:3px;");
        return l;
    }

    private Label barisTotal(Component parent, String label, boolean besar) {
        Div row = new Div();
        row.setStyle("display:flex;justify-content:space-between;align-items:center;margin:"
                + (besar ? "8px 0 0;border-top:1px dashed #cbd5e1;padding-top:8px;" : "3px 0;"));
        row.setParent(parent);
        Label l = new Label(label);
        l.setStyle("color:#475569;" + (besar ? "font-weight:800;font-size:14px;" : "font-size:12px;"));
        l.setParent(row);
        Label v = new Label(ais.common.Common.getBahasaConfig("Rp 0"));
        v.setStyle(besar ? "font-weight:900;font-size:18px;color:#16a34a;" : "font-weight:700;font-size:12px;color:#0f172a;");
        v.setParent(row);
        return v;
    }

    private void addCol(Columns columns, String label, String width) {
        Column c = new Column();
        c.setLabel(label);
        if (width != null) {
            c.setWidth(width);
        }
        c.setParent(columns);
    }

    private void tambahPesanProduk(Rows rows, String pesan) {
        Row r = new Row();
        r.setParent(rows);
        Label l = new Label(pesan);
        l.setStyle("color:#64748b;padding:10px 4px;");
        l.setParent(r);
    }

    /** Ganti isi (Rows) sebuah Grid dengan yang baru. */
    private void ganti(Grid grid, Rows rows) {
        if (grid == null) {
            return;
        }
        if (grid.getRows() != null) {
            grid.getRows().detach();
        }
        rows.setParent(grid);
    }

    @SuppressWarnings("unchecked")
    private List<Object[]> rows(String sql) {
        try {
            SQLQuery q = HibernateUtil.currentSession().createSQLQuery(sql);
            // NORMALISASI BENTUK BARIS: untuk query SATU kolom, Hibernate mengembalikan List
            // berisi nilai skalar (mis. BigInteger/String), BUKAN Object[]. Bila caller mengiterasi
            // `for (Object[] r : rows(...))`, hal itu memicu ClassCastException
            // "BigInteger cannot be cast to [Ljava.lang.Object;". Agar SEMUA pemanggil tetap aman
            // (baik query 1 kolom maupun banyak kolom), bungkus tiap baris skalar menjadi Object[]{nilai}.
            List<?> raw = q.list();
            List<Object[]> out = new ArrayList<Object[]>();
            if (raw != null) {
                for (Object item : raw) {
                    if (item instanceof Object[]) {
                        out.add((Object[]) item);
                    } else {
                        out.add(new Object[] { item });
                    }
                }
            }
            return out;
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
            return new ArrayList<Object[]>();
        }
    }

    private static double num(Object o) {
        return o == null ? 0.0 : ((Number) o).doubleValue();
    }

    private static String str(Object o) {
        return o == null ? "" : o.toString();
    }

    private static Long lng(Object o) {
        return o == null ? null : ((Number) o).longValue();
    }

    private static boolean bool(Object o) {
        if (o == null) {
            return false;
        }
        if (o instanceof Boolean) {
            return ((Boolean) o).booleanValue();
        }
        String s = o.toString().trim();
        return s.equals("t") || s.equalsIgnoreCase("true") || s.equals("1");
    }

    private static Date date(Object o) {
        return (o instanceof Date) ? (Date) o : null;
    }
}
