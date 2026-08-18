package ais.action.master.koperasi.helper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Box;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.East;
import org.zkoss.zul.Foot;
import org.zkoss.zul.Footer;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Group;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.AmbilDataMahasiswaBanbox;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.sekolah.helper.AmbilDataSiswaBanbox;
import ais.action.master.helper.virtualaccount.DownloadTagihanAnggotaKoperasiBankOnline;
import ais.action.master.koperasi.PembayaranAnggotaKoperasiAction;
import ais.action.ws.util.PembayaranUtil;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.common.IndonesianNumberToWords;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BankHost;
import ais.database.model.Mahasiswa;
import ais.database.model.Konfigurasi;
import ais.database.model.Tbmuser;
import ais.database.model.VirtualAccountBank;
import ais.database.model.sekolah.Siswa;
import ais.database.model.koperasi.AnggotaKoperasi;
import ais.database.model.koperasi.CaraPembayaranKoperasi;
import ais.database.model.koperasi.PembayaranAnggotaKoperasi;
import ais.database.model.koperasi.PembayaranAnggotaKoperasiDetail;
import ais.database.model.koperasi.ProdukKoperasi;
import ais.database.model.koperasi.TransaksiKoperasi;
import ais.database.model.koperasi.TransaksiKoperasiDetail;
import ais.database.model.inventory.Toko;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyInclude;
import ais.ui.util.MyLabelBold;
import ais.ui.util.MyLabelBoldAja;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.WaktuUtil;

/**
 * Halaman pembayaran koperasi online/POS anggota.
 *
 * Versi ini tetap mempertahankan alur lama (pilih anggota, pilih tagihan,
 * pembayaran tunai/manual, pembayaran online/VA, riwayat pembayaran, dan tab
 * histori) namun tampilan dibuat lebih ringkas seperti POS modern.
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class PembayaranKoperasiOnline extends GenericAutowireComposer {

    private static final long serialVersionUID = 7381263733011550603L;

    private static final int RIWAYAT_POS_LIMIT = 12;
    private static final String POS_PRIMARY = "#2563eb";
    private static final String POS_SUCCESS = "#16a34a";
    private static final String POS_DANGER = "#dc2626";
    private static final String POS_WARNING = "#f59e0b";
    private static final String POS_DARK = "#0f172a";

    private boolean reloadTransaksiTerjadwal = false;

    private AmbilDataAnggotaKoperasiBanbox anggotaKoperasi;
    private AmbilDataSiswaBanbox siswa;
    private AmbilDataMahasiswaBanbox mahasiswa;
    private MyWindow window;
    private AnggotaKoperasi selectedAnggotaKoperasi;
    private Siswa selectedSiswa;
    private Mahasiswa selectedMahasiswa;

    private MyDoublebox deposit;
    private Component east;
    private HashMap<Long, TransaksiKoperasiDetail> transaksiKoperasiDetails;
    private HashSet<Long> transaksiKoperasiDetailsPilih;
    private List<MyCheckboxConfig> pilihan;

    private MyLabelBold totalTransaksiKoperasiDetail;
    private MyLabelBold terbilang;
    private MyLabelBoldAja lblAnggotaKoperasi;
    private MyLabelBoldAja lblSiswa;
    private MyLabelBoldAja lblMahasiswa;
    private Label labelJumlahDipilih;
    private Label labelTotalTagihan;
    private Label labelTotalTopup;
    private Label labelTotalProduk;
    private Label labelProdukDipilih;
    private Label labelGrandTotal;

    private Combobox tokoKoperasi;
    private Textbox cariProdukKoperasi;
    private Rows rowsProdukKoperasi;
    private java.util.Map<Long, PosProductItem> produkKoperasiItems = new java.util.LinkedHashMap<Long, PosProductItem>();
    private java.util.Map<Long, PosProductItem> produkKoperasiDipilih = new java.util.LinkedHashMap<Long, PosProductItem>();

    private double t;
    private MyDatebox tanggalTransaski;
    private MyCheckboxConfig pilihCustom;
    private Combobox caraPembayaranPos;
    private MyDoublebox uangDiterimaTunai;
    private Label labelKembalianTunai;
    private Label labelCaraBayarAktif;

    private MyTabConfig tabOnline;
    private Tabpanel tabpanelOnline;
    private Tabpanel tabpanel3;
    private Component center;
    private Rows rowsDetailBiaya;

    private boolean langsungBayar;
    private String sumberPembayaran;
    private boolean tampilAnggotaKoperasi = true;
    private boolean tampilSiswa = true;
    private boolean tampilMahasiswa = true;

    private final EventListener eventListenerData = new EventListener() {
        @Override
        public void onEvent(Event event) throws Exception {
            onPilihTransaksi(event);
        }
    };

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);

        anggotaKoperasi = new AmbilDataAnggotaKoperasiBanbox();
        anggotaKoperasi.setWidth("210px");
        siswa = new AmbilDataSiswaBanbox();
        siswa.setWidth("210px");
        mahasiswa = new AmbilDataMahasiswaBanbox();
        mahasiswa.setWidth("210px");

        lblAnggotaKoperasi = new MyLabelBoldAja("Anggota");
        lblSiswa = new MyLabelBoldAja("Siswa");
        lblMahasiswa = new MyLabelBoldAja("Mahasiswa");

        tokoKoperasi = new Combobox();
        tokoKoperasi.setWidth("245px");
        tokoKoperasi.setReadonly(true);
        cariProdukKoperasi = new Textbox();
        cariProdukKoperasi.setWidth("170px");
        cariProdukKoperasi.setTooltiptext("Ketik nama atau kode produk, lalu tekan Enter atau pindah fokus untuk mencari.");
        cariProdukKoperasi.setValue("");

        langsungBayar = readBooleanParameter("langsungBayar");
        sumberPembayaran = readStringParameter("sumberPembayaran");
        aturModePelanggan();
        loadAnggotaFromParameter();
        loadSiswaFromParameter();
        loadMahasiswaFromParameter();
        loadSelectedDetailFromParameter();
        loadPelangganFromLoginUser();

        Common.createDefaultTimer(new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                init();
            }
        });
    }

    private boolean readBooleanParameter(String key) {
        try {
            return ExecutionsCtrl.getCurrent().getParameter(key) != null
                    && Boolean.parseBoolean(ExecutionsCtrl.getCurrent().getParameter(key));
        } catch (Exception e) {
            return false;
        }
    }

    private String readStringParameter(String key) {
        try {
            String value = ExecutionsCtrl.getCurrent().getParameter(key);
            return value == null ? "" : value.trim();
        } catch (Exception e) {
            return "";
        }
    }




    private void aturModePelanggan() {
        String mode = gabungkanParameterModePelanggan();

        boolean dariPembayaranMahasiswa = readBooleanParameter("hanyaMahasiswa")
                || readBooleanParameter("dariDaftarUlangMahasiswa") || readBooleanParameter("pembayaranMahasiswa")
                || readBooleanParameter("dariPembayaranMahasiswa") || modeMenunjukMahasiswa(mode)
                || modeMenunjukMahasiswa(sumberPembayaran);

        boolean dariPembayaranSiswa = !dariPembayaranMahasiswa
                && (readBooleanParameter("hanyaSiswa") || readBooleanParameter("dariPembayaranOnline")
                        || readBooleanParameter("pembayaranOnlineSiswa") || readBooleanParameter("dariPembayaranSiswa")
                        || modeMenunjukSiswa(mode) || modeMenunjukSiswa(sumberPembayaran));

        boolean hanyaAnggota = readBooleanParameter("hanyaAnggota") || modeMenunjukAnggota(mode)
                || modeMenunjukAnggota(sumberPembayaran);

        /*
         * Kata "mahasiswa" mengandung "siswa". Karena itu mode mahasiswa harus
         * dicek lebih dulu, dan mode siswa tidak boleh memakai indexOf("siswa")
         * secara langsung.
         */
        if (dariPembayaranMahasiswa) {
            tampilAnggotaKoperasi = false;
            tampilSiswa = false;
            tampilMahasiswa = true;
        } else if (dariPembayaranSiswa) {
            tampilAnggotaKoperasi = false;
            tampilSiswa = true;
            tampilMahasiswa = false;
        } else if (hanyaAnggota) {
            tampilAnggotaKoperasi = true;
            tampilSiswa = false;
            tampilMahasiswa = false;
        } else {
            tampilAnggotaKoperasi = true;
            tampilSiswa = true;
            tampilMahasiswa = true;
        }
    }

    private String gabungkanParameterModePelanggan() {
        StringBuilder sb = new StringBuilder();
        appendModePart(sb, sumberPembayaran);
        appendModePart(sb, readStringParameter("modePelanggan"));
        appendModePart(sb, readStringParameter("jenisPelanggan"));
        appendModePart(sb, readStringParameter("caller"));
        appendModePart(sb, readStringParameter("asal"));
        appendModePart(sb, readStringParameter("source"));
        return sb.toString().toLowerCase();
    }

    private void appendModePart(StringBuilder sb, String value) {
        if (value == null || value.trim().length() == 0) {
            return;
        }
        if (sb.length() > 0) {
            sb.append(" ");
        }
        sb.append(value.trim());
    }

    private boolean modeMenunjukMahasiswa(String value) {
        String v = safe(value).toLowerCase();
        return v.equals("mahasiswa") || v.equals("mhs") || v.equals("calon_mahasiswa")
                || v.equals("calonmahasiswa") || v.indexOf("pembayaran_mahasiswa") >= 0
                || v.indexOf("pembayaranmahasiswa") >= 0 || v.indexOf("daftar_ulang_mahasiswa") >= 0
                || v.indexOf("daftarulangmahasiswa") >= 0 || v.indexOf("daftar ulang mahasiswa") >= 0;
    }

    private boolean modeMenunjukSiswa(String value) {
        String v = safe(value).toLowerCase();
        return v.equals("siswa") || v.equals("calon_siswa") || v.equals("calonsiswa") || v.equals("sekolah")
                || v.equals("pembayaran_online_siswa") || v.equals("pembayaranonlinesiswa")
                || v.indexOf("pembayaran_siswa") >= 0 || v.indexOf("pembayaransiswa") >= 0
                || v.indexOf("pembayaran_online_sekolah") >= 0 || v.indexOf("pembayaranonlinesekolah") >= 0;
    }

    private boolean modeMenunjukAnggota(String value) {
        String v = safe(value).toLowerCase();
        return v.equals("anggota") || v.equals("anggota_koperasi") || v.equals("anggotakoperasi")
                || v.equals("koperasi") || v.equals("pos") || v.equals("pos_koperasi");
    }




    private void loadAnggotaFromParameter() {
        String idText = null;
        try {
            idText = ExecutionsCtrl.getCurrent().getParameter("anggotaKoperasi");
        } catch (Exception e) {
            idText = null;
        }
        if (isEmpty(idText)) {
            return;
        }
        try {
            Long id = Long.valueOf(idText.trim());
            selectedAnggotaKoperasi = (AnggotaKoperasi) ConstantValues.simpleObject(
                    HibernateUtil.currentSession().createCriteria(AnggotaKoperasi.class).add(Restrictions.idEq(id)),
                    AnggotaKoperasi.class);
            setSelectedAnggotaToInput(selectedAnggotaKoperasi, true);
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
        }
    }

    private void loadSelectedDetailFromParameter() {
        String ids = null;
        try {
            ids = ExecutionsCtrl.getCurrent().getParameter("transaksiKoperasiDetails");
        } catch (Exception e) {
            ids = null;
        }
        if (isEmpty(ids)) {
            return;
        }
        transaksiKoperasiDetailsPilih = new HashSet<Long>();
        String[] parts = ids.split(",");
        for (int i = 0; i < parts.length; i++) {
            try {
                if (!isEmpty(parts[i])) {
                    transaksiKoperasiDetailsPilih.add(Long.valueOf(parts[i].trim()));
                }
            } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/koperasi/helper/PembayaranKoperasiOnline.java:348");
            }
        }
    }

    private void loadPelangganFromLoginUser() {
        try {
            Tbmuser tbmuser = Common.getCurrentUser();
            if (tbmuser == null) {
                return;
            }
            if (tbmuser.getAnggotaKoperasi() != null && tampilAnggotaKoperasi) {
                selectedAnggotaKoperasi = tbmuser.getAnggotaKoperasi();
                setSelectedAnggotaToInput(selectedAnggotaKoperasi, true);
            } else if (tbmuser.getSiswa() != null && tampilSiswa) {
                selectedSiswa = tbmuser.getSiswa();
                setSelectedSiswaToInput(selectedSiswa, true);
            } else if (tbmuser.getMahasiswa() != null && tampilMahasiswa) {
                selectedMahasiswa = tbmuser.getMahasiswa();
                setSelectedMahasiswaToInput(selectedMahasiswa, true);
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/koperasi/helper/PembayaranKoperasiOnline.java:369");
        }
    }

    private void loadSiswaFromParameter() {
        String idText = readStringParameter("siswa");
        if (isEmpty(idText)) {
            idText = readStringParameter("siswaId");
        }
        if (isEmpty(idText)) {
            return;
        }
        try {
            Long id = Long.valueOf(idText.trim());
            selectedSiswa = (Siswa) ConstantValues.simpleObject(
                    HibernateUtil.currentSession().createCriteria(Siswa.class).add(Restrictions.idEq(id)), Siswa.class);
            setSelectedSiswaToInput(selectedSiswa, true);
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
        }
    }

    private void loadMahasiswaFromParameter() {
        String idText = readStringParameter("mahasiswa");
        if (isEmpty(idText)) {
            idText = readStringParameter("mahasiswaId");
        }
        if (isEmpty(idText)) {
            return;
        }
        try {
            Long id = Long.valueOf(idText.trim());
            selectedMahasiswa = (Mahasiswa) ConstantValues.simpleObject(
                    HibernateUtil.currentSession().createCriteria(Mahasiswa.class).add(Restrictions.idEq(id)),
                    Mahasiswa.class);
            setSelectedMahasiswaToInput(selectedMahasiswa, true);
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
        }
    }

    private void setSelectedAnggotaToInput(AnggotaKoperasi anggota, boolean disabled) {
        try {
            if (anggotaKoperasi == null) {
                return;
            }
            anggotaKoperasi.setAttribute("anggotaKoperasi", anggota);
            anggotaKoperasi.setAttribute("myValue", anggota);
            anggotaKoperasi.setValue(anggota == null ? "" : safe(anggota.getNama()));
            anggotaKoperasi.setVisible(anggota != null || !disabled);
            anggotaKoperasi.setDisabled(disabled && anggota != null);
            if (lblAnggotaKoperasi != null) {
                lblAnggotaKoperasi.setVisible(anggota != null || !disabled);
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/koperasi/helper/PembayaranKoperasiOnline.java:423");
        }
    }


    private void setSelectedSiswaToInput(Siswa siswaData, boolean disabled) {
        try {
            if (siswa == null) {
                return;
            }
            siswa.setAttribute("siswa", siswaData);
            siswa.setAttribute("myValue", siswaData);
            siswa.setValue(siswaData == null ? "" : safe(siswaData.getNama()));
            siswa.setVisible(siswaData != null || !disabled);
            siswa.setDisabled(disabled && siswaData != null);
            if (lblSiswa != null) {
                lblSiswa.setVisible(siswaData != null || !disabled);
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/koperasi/helper/PembayaranKoperasiOnline.java:441");
        }
    }

    private void setSelectedMahasiswaToInput(Mahasiswa mahasiswaData, boolean disabled) {
        try {
            if (mahasiswa == null) {
                return;
            }
            mahasiswa.setAttribute("mahasiswa", mahasiswaData);
            mahasiswa.setAttribute("myValue", mahasiswaData);
            mahasiswa.setValue(mahasiswaData == null ? ""
                    : safe(mahasiswaData.getNim()) + " - " + safe(mahasiswaData.getNama()));
            mahasiswa.setVisible(mahasiswaData != null || !disabled);
            mahasiswa.setDisabled(disabled && mahasiswaData != null);
            if (lblMahasiswa != null) {
                lblMahasiswa.setVisible(mahasiswaData != null || !disabled);
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/koperasi/helper/PembayaranKoperasiOnline.java:459");
        }
    }

    private void clearAnggotaSelection() {
        selectedAnggotaKoperasi = null;
        if (anggotaKoperasi != null) {
            anggotaKoperasi.setAttribute("anggotaKoperasi", null);
            anggotaKoperasi.setAttribute("myValue", null);
            anggotaKoperasi.setValue("");
        }
    }

    private void clearSiswaSelection() {
        selectedSiswa = null;
        if (siswa != null) {
            siswa.setAttribute("siswa", null);
            siswa.setAttribute("myValue", null);
            siswa.setValue("");
        }
    }

    private void clearMahasiswaSelection() {
        selectedMahasiswa = null;
        if (mahasiswa != null) {
            mahasiswa.setAttribute("mahasiswa", null);
            mahasiswa.setAttribute("myValue", null);
            mahasiswa.setValue("");
        }
    }

    private void init() {
        Common.clear(window);

        Tabbox tabbox = new Tabbox();
        tabbox.setParent(Common.tampilanScrollTabbox(window));
        tabbox.setHeight("100%");
        tabbox.setWidth("100%");

        Tabs tabs = new Tabs();
        tabs.setParent(tabbox);

        MyTabConfig tab1 = new MyTabConfig("POS Pembayaran");
        tab1.setParent(tabs);

        MyTabConfig tab3 = new MyTabConfig("Riwayat Pembayaran");
        tab3.setParent(tabs);
        tab3.setVisible(anggotaKoperasi == null || anggotaKoperasi.isVisible());

        tabOnline = new MyTabConfig("Pembayaran Online");
        tabOnline.setParent(tabs);

        Tabpanels tabpanels = new Tabpanels();
        tabpanels.setParent(tabbox);

        Tabpanel tabpanel1 = new ais.ui.util.MyTabpanel();
        tabpanel1.setParent(tabpanels);
        tabpanel1.setHeight("100%");
        buildPosTab(tabpanel1);

        tabpanel3 = new ais.ui.util.MyTabpanel();
        tabpanel3.setVisible(tab3.isVisible());
        tabpanel3.setParent(tabpanels);
        tab3.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                loadRiwayatTab();
            }
        });

        tabpanelOnline = new ais.ui.util.MyTabpanel();
        tabpanelOnline.setParent(tabpanels);
        tabOnline.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                loadOnlineTab();
            }
        });
    }



    private void buildPosTab(Tabpanel tabpanel) {
        ais.ui.util.MyPortallayout portalLayout = new ais.ui.util.MyPortallayout();
        portalLayout.setParent(tabpanel);
        portalLayout.setWidth("100%");
        portalLayout.setStyle("background:#eef3fb;min-height:600px;");
        // Penanda khusus layout 2 kolom POS Koperasi agar perbaikan CSS (kolom mengisi
        // penuh + anti-terpotong) hanya berlaku di sini, tidak memengaruhi portal lain.
        portalLayout.setSclass("ais-poskop-layout");

        ais.ui.util.MyPortalchildren kolKiri = new ais.ui.util.MyPortalchildren();
        kolKiri.setWidth("60%");
        kolKiri.setSclass("ais-portal-col");
        kolKiri.setStyle("overflow-y:auto;padding:0;background:#eef3fb;");
        kolKiri.setParent(portalLayout);
        center = kolKiri;

        ais.ui.util.MyPortalchildren kolKanan = new ais.ui.util.MyPortalchildren();
        kolKanan.setWidth("40%");
        kolKanan.setSclass("ais-portal-col");
        kolKanan.setStyle("overflow-y:auto;padding:0;background:#eef3fb;");
        kolKanan.setParent(portalLayout);
        east = kolKanan;

        anggotaKoperasi.setEventListener(new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                selectedAnggotaKoperasi = getAnggotaAktif();
                if (selectedAnggotaKoperasi != null) {
                    clearSiswaSelection();
                    clearMahasiswaSelection();
                }
                jadwalkanReloadTransaksiKoperasiDetail();
            }
        });
        siswa.setEventListener(new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                selectedSiswa = getSiswaAktif();
                if (selectedSiswa != null) {
                    clearAnggotaSelection();
                    clearMahasiswaSelection();
                }
                jadwalkanReloadTransaksiKoperasiDetail();
            }
        });
        mahasiswa.setEventListener(new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                selectedMahasiswa = getMahasiswaAktif();
                if (selectedMahasiswa != null) {
                    clearAnggotaSelection();
                    clearSiswaSelection();
                }
                jadwalkanReloadTransaksiKoperasiDetail();
            }
        });

        reloadTransaksiKoperasiDetail();
    }







    private void renderHeader(North north) {
        if (north == null) {
            return;
        }
        Vbox box = new Vbox();
        box.setParent(north);
        box.setWidth("100%");
        renderHeaderPanel(box);
    }

    private void renderHeaderPanel(Vbox parent) {
        if (parent == null) {
            return;
        }
        String modePelanggan = getTipePelangganLabel();
        String htmlHeader = "<div style='position:relative;overflow:hidden;border-radius:20px;padding:16px 20px;margin-bottom:12px;"
                + "background:linear-gradient(135deg, rgba(0,0,0,.35), rgba(0,0,0,0) 55%), linear-gradient(135deg, var(--ais-theme-primary,#1d4ed8) 0%, var(--ais-theme-primary,#1d4ed8) 45%, var(--ais-theme-accent,#06b6d4) 100%);"
                + "box-shadow:0 16px 34px rgba(15,23,42,.18);color:#fff;margin-bottom:12px;'>"
                + "<div style='position:absolute;right:-65px;top:-75px;width:220px;height:220px;border-radius:999px;background:rgba(255,255,255,.11);'></div>"
                + "<div style='position:relative;z-index:1;display:flex;justify-content:space-between;gap:14px;align-items:flex-start;flex-wrap:wrap;'>"
                + "<div style='min-width:260px;'><div style='font-size:11px;letter-spacing:.14em;text-transform:uppercase;opacity:.82;font-weight:800;'>Modern Cooperative POS</div>"
                + "<div style='font-size:24px;line-height:1.25;font-weight:900;margin-top:4px;'>Pembayaran & Pembelian Koperasi</div>"
                + "<div style='font-size:12px;line-height:1.6;margin-top:6px;opacity:.92;'>Pilih pelanggan sesuai sumber menu, pilih toko/merchant, pilih produk atau tagihan, lalu proses pembayaran. Produk baru dapat dipilih setelah toko ditentukan agar katalog tidak tercampur antar merchant.</div>"
                + "<div style='display:flex;gap:8px;flex-wrap:wrap;margin-top:10px;'>"
                + badgeHtml("Mode: " + modePelanggan, "#dbeafe", "#1e40af")
                + badgeHtml("Pilih toko dahulu", "#fef3c7", "#92400e")
                + badgeHtml("Keranjang real-time", "#dcfce7", "#166534")
                + "</div></div>"
                + "<div style='min-width:190px;text-align:right;'>"
                + "<div style='font-size:11px;opacity:.78;'>Tanggal transaksi</div>"
                + "<div style='font-size:17px;font-weight:900;'>" + html(Common.dateFormat3.get().format(WaktuUtil.getDate())) + "</div>"
                + "<div style='font-size:11px;opacity:.78;margin-top:4px;'>Gunakan Refresh jika tagihan atau produk belum tampil.</div>"
                + "</div></div></div>";
        parent.appendChild(new ais.ui.util.MyHtml(htmlHeader));
    }





    private void loadRiwayatTab() {
        if (tabpanel3 == null || tabpanel3.getChildren().size() > 0) {
            return;
        }
        MyWindow riwayatWindow = new MyWindow("", "none", false);
        riwayatWindow.setHeight("100%");
        riwayatWindow.setWidth("100%");
        riwayatWindow.setParent(tabpanel3);
        MyInclude iframe = new MyInclude("/pages/master/koperasi/pembayaran_anggota_koperasi.zul"
                + (selectedAnggotaKoperasi == null ? "" : "?anggotaKoperasi=" + selectedAnggotaKoperasi.getId()));
        iframe.setParent(riwayatWindow);
    }

    private void loadOnlineTab() {
        if (tabpanelOnline == null || tabpanelOnline.getChildren().size() > 0) {
            return;
        }
        MyWindow onlineWindow = new MyWindow("", "none", false);
        onlineWindow.setHeight("100%");
        onlineWindow.setWidth("100%");
        onlineWindow.setParent(tabpanelOnline);
        MyInclude iframe = new MyInclude("/pages/master/virtual_account_bank.zul?1=1"
                + (selectedAnggotaKoperasi == null ? "" : "&anggotaKoperasi=" + selectedAnggotaKoperasi.getId()));
        iframe.setParent(onlineWindow);
    }

    private void onPilihTransaksi(Event event) {
        try {
            MyCheckboxConfig check = (MyCheckboxConfig) event.getTarget();
            TransaksiKoperasiDetail detail = (TransaksiKoperasiDetail) check.getAttribute("transaksiKoperasiDetail");
            if (detail == null || detail.getId() == null) {
                return;
            }

            if (pilihCustom == null || !pilihCustom.isChecked()) {
                pilihPaketAngsuran(check, detail);
            } else if (check.isChecked()) {
                transaksiKoperasiDetails.put(detail.getId(), detail);
            } else {
                transaksiKoperasiDetails.remove(detail.getId());
            }
            hitungUlangTransaksiKoperasiDetail();
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
        }
    }

    private void pilihPaketAngsuran(MyCheckboxConfig check, TransaksiKoperasiDetail selected) {
        boolean pilih = check.isChecked();
        transaksiKoperasiDetails.remove(selected.getId());

        Long transaksiId = getTransaksiId(selected);
        for (int i = 0; i < pilihan.size(); i++) {
            MyCheckboxConfig cb = (MyCheckboxConfig) pilihan.get(i);
            TransaksiKoperasiDetail detail = (TransaksiKoperasiDetail) cb.getAttribute("transaksiKoperasiDetail");
            if (detail != null && isSameTransaksi(transaksiId, detail)) {
                cb.setDisabled(false);
                cb.setChecked(false);
                if (detail.getId() != null) {
                    transaksiKoperasiDetails.remove(detail.getId());
                }
            }
        }

        if (!pilih) {
            return;
        }

        int sampaiKe = intValue(selected.getKe());
        for (int i = 0; i < pilihan.size(); i++) {
            MyCheckboxConfig cb = (MyCheckboxConfig) pilihan.get(i);
            TransaksiKoperasiDetail detail = (TransaksiKoperasiDetail) cb.getAttribute("transaksiKoperasiDetail");
            if (detail != null && isSameTransaksi(transaksiId, detail) && intValue(detail.getKe()) <= sampaiKe) {
                cb.setChecked(true);
                cb.setDisabled(check != cb);
                if (detail.getId() != null) {
                    transaksiKoperasiDetails.put(detail.getId(), detail);
                }
            }
        }
    }



    private boolean checkKondisiSebelumbayar() throws Exception {
        boolean adaProduk = produkKoperasiDipilih != null && !produkKoperasiDipilih.isEmpty();
        boolean adaTagihan = transaksiKoperasiDetails != null && !transaksiKoperasiDetails.isEmpty();
        boolean adaTopup = getDepositValue() > 0.1;

        if (!adaPelangganAktif() && !adaProduk) {
            MyMessageboxConfig.show("Mohon maaf, data pelanggan belum dipilih. Langkah yang dapat dilakukan: (1) pilih pelanggan sesuai mode halaman: Siswa, Mahasiswa, atau Anggota Koperasi; (2) gunakan kolom pencarian untuk menemukan pelanggan; (3) ulangi proses pembayaran.",
                    "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
            return false;
        }
        if (!adaPelangganAktif() && (adaTagihan || adaTopup)) {
            MyMessageboxConfig.show("Tagihan koperasi dan topup tetap membutuhkan data pelanggan. Untuk pembelian non anggota, kosongkan tagihan/topup dan pilih produk saja.",
                    "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
            return false;
        }
        if (adaProduk && getSelectedTokoId() == null) {
            MyMessageboxConfig.show("Mohon maaf, toko/merchant belum dipilih. Langkah yang dapat dilakukan: (1) pilih toko atau merchant dari daftar yang tersedia; (2) pastikan toko sudah aktif; (3) ulangi penambahan produk.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
            return false;
        }
        if (!adaTagihan && !adaTopup && !adaProduk) {
            MyMessageboxConfig.show("Mohon maaf, belum ada transaksi yang dipilih. Langkah yang dapat dilakukan: (1) pilih transaksi koperasi dari daftar tagihan; (2) tambahkan produk POS; atau (3) isi nilai pembayaran/topup, lalu ulangi proses.",
                    "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
            return false;
        }
        return true;
    }




    private void hitungUlangTransaksiKoperasiDetail() {
        t = 0.0;
        int jumlahDipilih = 0;
        if (transaksiKoperasiDetails != null) {
            Collection<TransaksiKoperasiDetail> details = transaksiKoperasiDetails.values();
            for (TransaksiKoperasiDetail detail : details) {
                t += hitungNominal(detail);
                jumlahDipilih++;
            }
        }
        double nilaiDeposit = getDepositValue();
        double nilaiProduk = getTotalProdukDipilih();
        int jumlahProduk = getJumlahProdukDipilih();
        t += nilaiDeposit + nilaiProduk;

        if (totalTransaksiKoperasiDetail != null) {
            totalTransaksiKoperasiDetail.setValue(formatMoney(t));
        }
        if (terbilang != null) {
            terbilang.setValue(t <= 0.1 ? "-" : IndonesianNumberToWords.convert((long) t));
        }
        if (labelJumlahDipilih != null) {
            labelJumlahDipilih.setValue((jumlahDipilih + jumlahProduk) + " item");
        }
        if (labelProdukDipilih != null) {
            labelProdukDipilih.setValue(jumlahProduk + " produk");
        }
        if (labelTotalTagihan != null) {
            labelTotalTagihan.setValue(formatMoney(t - nilaiDeposit - nilaiProduk));
        }
        if (labelTotalProduk != null) {
            labelTotalProduk.setValue(formatMoney(nilaiProduk));
        }
        if (labelTotalTopup != null) {
            labelTotalTopup.setValue(formatMoney(nilaiDeposit));
        }
        if (labelGrandTotal != null) {
            labelGrandTotal.setValue(formatMoney(t));
        }
        updateKembalianTunaiAman();
    }




    private void jadwalkanReloadTransaksiKoperasiDetail() {
        if (reloadTransaksiTerjadwal) {
            return;
        }
        reloadTransaksiTerjadwal = true;
        try {
            /*
             * Jangan langsung clear/re-render Center di event pemilihan Bandbox.
             * AmbilDataMahasiswaBanbox/AmbilDataSiswaBanbox biasanya menutup popup
             * dengan setOpen(false). Jika komponen Bandbox langsung dilepas dari DOM
             * pada response yang sama, browser dapat gagal membaca widget dan muncul:
             * Cannot read properties of null (reading 'setOpen').
             * Reload ditunda singkat agar proses close popup selesai lebih dulu.
             */
            Common.createDefaultTimer(new EventListener() {
                @Override
                public void onEvent(Event event) throws Exception {
                    reloadTransaksiTerjadwal = false;
                    reloadTransaksiKoperasiDetail();
                }
            }, "", false, 250);
        } catch (Exception e) {
            reloadTransaksiTerjadwal = false;
            reloadTransaksiKoperasiDetail();
        }
    }


    private void reloadTransaksiKoperasiDetail() {
        transaksiKoperasiDetails = new HashMap<Long, TransaksiKoperasiDetail>();
        pilihan = new ArrayList<MyCheckboxConfig>();
        Common.clear(center);
        if (east != null) {
            Common.clear(east);
        }

        Vbox mainBox = new Vbox();
        mainBox.setParent(center);
        mainBox.setWidth("100%");
        mainBox.setStyle("padding:12px;box-sizing:border-box;background:#eef3fb;");

        AnggotaKoperasi anggota = getAnggotaUntukTagihan();
        renderHeaderPanel(mainBox);
        renderSelectorPelangganMerchant(mainBox);
        renderMemberInfo(mainBox, anggota);
        renderProductCatalog(mainBox);
        renderTransactionGrid(mainBox);

        renderSummaryPanel(anggota);

        Common.clear(rowsDetailBiaya);
        if (anggota != null) {
            tampilPembayaran(anggota);
            reloadRiwayatPembayaran(anggota, null);
        } else if (adaPelangganAktif()) {
            rowsDetailBiaya.appendChild(emptyRow(
                    "Belum ada data anggota koperasi untuk pelanggan ini. Pembayaran/topup dan pembelian tetap dapat diproses; data anggota koperasi akan dibuat otomatis saat disimpan jika diperlukan."));
            renderTopupRow(null);
            renderEmptyHistory("Riwayat pembayaran akan tampil setelah pembayaran pertama disimpan.");
        } else {
            rowsDetailBiaya.appendChild(emptyRow("Silakan pilih pelanggan sesuai mode halaman untuk menampilkan tagihan."));
            renderEmptyHistory("Riwayat pembayaran akan tampil setelah pelanggan dipilih.");
        }

        hitungUlangTransaksiKoperasiDetail();
    }




    private void renderSelectorPelangganMerchant(Vbox parent) {
        if (parent == null) {
            return;
        }
        Vbox wrapper = new Vbox();
        wrapper.setParent(parent);
        wrapper.setWidth("100%");
        wrapper.setStyle("background:#ffffff;border:1px solid #dbe4f0;border-radius:18px;padding:12px 14px;"
                + "box-sizing:border-box;box-shadow:0 10px 24px rgba(15,23,42,.06);margin-bottom:12px;");

        wrapper.appendChild(new ais.ui.util.MyHtml("<div style='display:flex;justify-content:space-between;gap:10px;align-items:flex-start;flex-wrap:wrap;margin-bottom:10px;'>"
                + "<div><div style='font-size:15px;font-weight:900;color:#0f172a;'>Pilih Pelanggan dan Toko/Merchant</div>"
                + "<div style='font-size:11.5px;color:#64748b;line-height:1.55;margin-top:3px;'>"
                + "Bagian ini digunakan untuk memilih sumber pelanggan, toko/merchant, dan pencarian produk. Jika halaman dibuka dari pembayaran mahasiswa maka pilihan Mahasiswa tampil otomatis; jika dari pembayaran siswa maka pilihan Siswa tampil otomatis; jika bukan keduanya maka operator dapat memilih Anggota, Siswa, atau Mahasiswa.</div></div>"
                + badgeHtml("Mode: " + getTipePelangganLabel(), "#eff6ff", "#1d4ed8")
                + "</div>"));

        Box inputBox = Common.isMobile() ? new Vbox() : new Hbox();
        inputBox.setParent(wrapper);
        inputBox.setWidth("100%");
        inputBox.setStyle("background:#f8fafc;border:1px solid #e2e8f0;border-radius:14px;"
                + "padding:10px;box-sizing:border-box;overflow-x:auto;overflow-y:hidden;");
        if (inputBox instanceof Hbox) {
            ((Hbox) inputBox).setAlign("center");
            ((Hbox) inputBox).setPack("start");
        }

        Label lblPilihPelanggan = new Label(tampilAnggotaKoperasi && tampilSiswa && tampilMahasiswa ? "Pelanggan" : "Pelanggan");
        lblPilihPelanggan.setStyle("font-size:11px;color:#475569;font-weight:900;");
        inputBox.appendChild(lblPilihPelanggan);

        if (tampilAnggotaKoperasi) {
            inputBox.appendChild(lblAnggotaKoperasi);
            anggotaKoperasi.setStyle("border-radius:10px;background:#ffffff;");
            inputBox.appendChild(anggotaKoperasi);
        }
        if (tampilSiswa) {
            inputBox.appendChild(lblSiswa);
            siswa.setStyle("border-radius:10px;background:#ffffff;");
            inputBox.appendChild(siswa);
        }
        if (tampilMahasiswa) {
            inputBox.appendChild(lblMahasiswa);
            mahasiswa.setStyle("border-radius:10px;background:#ffffff;");
            inputBox.appendChild(mahasiswa);
        }

        if (tokoKoperasi != null) {
            loadTokoKoperasiOptions();
            Label lblToko = new Label("Toko/Merchant");
            lblToko.setStyle("font-size:11px;color:#475569;font-weight:900;");
            inputBox.appendChild(lblToko);
            inputBox.appendChild(tokoKoperasi);
            if (tokoKoperasi.getAttribute("listenerPosToko") == null) {
                tokoKoperasi.setAttribute("listenerPosToko", Boolean.TRUE);
                tokoKoperasi.addEventListener("onChange", new EventListener() {
                    @Override
                    public void onEvent(Event event) throws Exception {
                        produkKoperasiDipilih.clear();
                        jadwalkanReloadTransaksiKoperasiDetail();
                    }
                });
            }
        }

        if (cariProdukKoperasi != null) {
            Label lblCariProduk = new Label(ais.common.Common.getBahasaConfig("Cari Produk"));
            lblCariProduk.setStyle("font-size:11px;color:#475569;font-weight:900;");
            inputBox.appendChild(lblCariProduk);
            inputBox.appendChild(cariProdukKoperasi);
            if (cariProdukKoperasi.getAttribute("listenerPosCariProduk") == null) {
                cariProdukKoperasi.setAttribute("listenerPosCariProduk", Boolean.TRUE);
                cariProdukKoperasi.addEventListener("onOK", new EventListener() {
                    @Override
                    public void onEvent(Event event) throws Exception {
                        jadwalkanReloadTransaksiKoperasiDetail();
                    }
                });
                cariProdukKoperasi.addEventListener("onChange", new EventListener() {
                    @Override
                    public void onEvent(Event event) throws Exception {
                        jadwalkanReloadTransaksiKoperasiDetail();
                    }
                });
            }
        }

        Tbmuser tbmuser = Common.getCurrentUser();
        if (isOperator(tbmuser)) {
            if (tanggalTransaski == null) {
                tanggalTransaski = new MyDatebox(WaktuUtil.getDate());
                tanggalTransaski.setReadonly(false);
                tanggalTransaski.setWidth("132px");
            }
            Label lblTanggal = new Label(ais.common.Common.getBahasaConfig("Tanggal"));
            lblTanggal.setStyle("font-size:11px;color:#475569;font-weight:900;");
            inputBox.appendChild(lblTanggal);
            inputBox.appendChild(tanggalTransaski);
        }

        MyToolbarbuttonConfig refresh = new MyToolbarbuttonConfig("Refresh", "/img/svg/refresh.svg");
        refresh.setTooltiptext("Muat ulang tagihan, produk, topup, dan riwayat pembayaran");
        refresh.setStyle("font-weight:bold;background:#2563eb;color:#ffffff;border-radius:999px;padding:7px 14px;border:0;");
        inputBox.appendChild(refresh);
        refresh.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                reloadTransaksiKoperasiDetail();
            }
        });

        if (isOperator(tbmuser)) {
            if (pilihCustom == null) {
                pilihCustom = new MyCheckboxConfig("Pilih item kustom");
                pilihCustom.setTooltiptext("Jika aktif, operator bebas memilih item tagihan tanpa otomatis memilih angsuran sebelumnya.");
                pilihCustom.addEventListener("onClick", new EventListener() {
                    @Override
                    public void onEvent(Event event) throws Exception {
                        reloadTransaksiKoperasiDetail();
                    }
                });
            }
            inputBox.appendChild(pilihCustom);
        }
    }



    private void renderMemberInfo(Vbox parent, AnggotaKoperasi anggota) {
        String tipe = getTipePelangganLabel();
        String nama = getNamaPelanggan(anggota);
        String kode = getKodePelanggan(anggota);
        String koperasi = getNamaKoperasiAman(anggota);
        String status = adaPelangganAktif() ? "Pelanggan Siap Transaksi" : "Pilih Pelanggan";
        String htmlInfo = "<div style='background:#ffffff;border:1px solid #dbe4f0;border-radius:18px;padding:14px 16px;"
                + "margin-bottom:12px;box-shadow:0 10px 24px rgba(15,23,42,.06);'>"
                + "<div style='display:flex;align-items:center;justify-content:space-between;gap:12px;flex-wrap:wrap;'>"
                + "<div style='display:flex;gap:12px;align-items:center;min-width:260px;'>"
                + "<div style='width:48px;height:48px;border-radius:16px;background:linear-gradient(135deg,#2563eb,#06b6d4);"
                + "display:flex;align-items:center;justify-content:center;color:#fff;font-size:22px;font-weight:900;'>"
                + iconInitial(nama) + "</div>"
                + "<div><div style='font-size:11px;color:#64748b;font-weight:800;text-transform:uppercase;letter-spacing:.06em;'>" + html(tipe) + "</div>"
                + "<div style='font-size:18px;font-weight:900;color:#0f172a;line-height:1.25;'>" + html(nama) + "</div>"
                + "<div style='font-size:11px;color:#475569;margin-top:3px;'>Kode: " + html(kode)
                + " &nbsp; | &nbsp; Koperasi: " + html(koperasi) + "</div></div></div>"
                + "<div style='display:flex;gap:8px;flex-wrap:wrap;align-items:center;'>"
                + badgeHtml(status, adaPelangganAktif() ? "#ecfdf5" : "#fff7ed", adaPelangganAktif() ? "#166534" : "#9a3412")
                + badgeHtml("Checkout POS", "#eff6ff", "#1d4ed8")
                + "</div></div></div>";
        parent.appendChild(new ais.ui.util.MyHtml(htmlInfo));
        renderPosMiniDashboard(parent, anggota);
    }

    private String getNamaKoperasiAman(AnggotaKoperasi anggota) {
        if (anggota == null || anggota.getId() == null) {
            return "-";
        }
        try {
            if (anggota.getKoperasi() != null && anggota.getKoperasi().getNama() != null) {
                return safe(anggota.getKoperasi().getNama());
            }
        } catch (org.hibernate.LazyInitializationException e) {
            return getNamaKoperasiByAnggotaId(anggota.getId());
        } catch (Exception e) {
            return getNamaKoperasiByAnggotaId(anggota.getId());
        }
        return "-";
    }


    private String getNamaKoperasiByAnggotaId(Long anggotaId) {
        if (anggotaId == null) {
            return "-";
        }
        try {
            /*
             * Model AnggotaKoperasi memakai @Table(schema="koperasi", name="anggota_koperasi")
             * dan Koperasi memakai @Table(schema="koperasi", name="koperasi").
             * Jangan memakai tabel tanpa schema karena akan mencari public.anggota_koperasi
             * dan dapat membuat koneksi JDBC masuk status aborted.
             */
            String sql = "select coalesce(k.nama,'-') as nama from koperasi.anggota_koperasi a "
                    + "left join koperasi.koperasi k on k.id=a.koperasi where a.id=" + anggotaId.longValue()
                    + " limit 1";
            List<Map<String, Object>> list = Common.ambilSqlMap(sql);
            if (list != null && !list.isEmpty()) {
                String nama = mapString((Map<String, Object>) list.get(0), "nama");
                return nama == null || nama.trim().length() == 0 ? "-" : nama;
            }
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
        }
        return "-";
    }





    private void renderTransactionGrid(Vbox parent) {
        Grid grid = new Grid();
        grid.setSclass("dgrid");
        grid.setWidth("100%");
        grid.setParent(parent);
        grid.setHeight("100%");
        grid.setStyle("background:#ffffff;border:1px solid #dbe4f0;border-radius:18px;overflow:hidden;"
                + "box-shadow:0 10px 24px rgba(15,23,42,.06);");

        Columns columns = new Columns();
        columns.setParent(grid);

        MyColumnConfig column = new MyColumnConfig("Tagihan / Produk POS");
        column.setParent(columns);

        column = new MyColumnConfig("Tanggal");
        column.setParent(columns);
        column.setWidth("120px");

        column = new MyColumnConfig("Nominal");
        column.setParent(columns);
        column.setAlign("right");
        column.setWidth("150px");

        column = new MyColumnConfig("Aksi");
        column.setParent(columns);
        column.setWidth(ais.ui.util.GridKolomHelper.LEBAR_KOLOM_AKSI);

        rowsDetailBiaya = new Rows();
        rowsDetailBiaya.setParent(grid);

        Foot foot = new Foot();
        foot.setParent(grid);
        Footer footer = new Footer();
        footer.appendChild(new MyLabelBold("Total Keranjang"));
        foot.appendChild(footer);

        footer = new Footer();
        footer.appendChild(new Label(""));
        foot.appendChild(footer);

        footer = new Footer();
        footer.setAlign("right");
        totalTransaksiKoperasiDetail = new MyLabelBold("0");
        totalTransaksiKoperasiDetail.setStyle("font-size:16px;color:#0f172a;font-weight:900;");
        footer.appendChild(totalTransaksiKoperasiDetail);
        foot.appendChild(footer);

        footer = new Footer();
        terbilang = new MyLabelBold("-");
        terbilang.setStyle("font-size:10.5px;color:#64748b;");
        footer.appendChild(terbilang);
        foot.appendChild(footer);
    }






    private Vbox getEastContentBoxAman() {
        if (east == null) {
            return null;
        }
        try {
            if (east.getChildren() != null && !east.getChildren().isEmpty()) {
                Object child = east.getChildren().get(0);
                if (child instanceof Vbox) {
                    return (Vbox) child;
                }
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/koperasi/helper/PembayaranKoperasiOnline.java:1147");
        }
        Common.clear(east);
        Vbox box = new Vbox();
        box.setParent(east);
        box.setWidth("100%");
        box.setStyle("padding:12px;box-sizing:border-box;background:#eef3fb;");
        return box;
    }



    private void renderSummaryPanel(AnggotaKoperasi anggota) {
        Common.clear(east);
        Vbox box = getEastContentBoxAman();
        if (box == null) {
            return;
        }

        box.appendChild(new ais.ui.util.MyHtml("<div style='background:#ffffff;border:1px solid #dbe4f0;border-radius:18px;"
                + "padding:14px 16px;margin-bottom:12px;box-shadow:0 10px 24px rgba(15,23,42,.06);'>"
                + "<div style='font-size:15px;font-weight:900;color:#0f172a;'>Ringkasan Checkout</div>"
                + "<div style='font-size:11.5px;color:#64748b;line-height:1.55;margin-top:4px;'>"
                + "Pastikan pelanggan, toko/merchant, produk, tagihan, topup, cara pembayaran, dan total bayar sudah benar. Cara bayar default adalah Tunai/Cash jika tersedia, mengikuti alur POS.</div>"
                + "</div>"));

        Grid info = new Grid();
        info.setWidth("100%");
        info.setParent(box);
        info.setStyle("background:#ffffff;border:1px solid #dbe4f0;border-radius:18px;margin-bottom:12px;"
                + "box-shadow:0 10px 24px rgba(15,23,42,.06);overflow:hidden;");
        Rows rows = new Rows();
        rows.setParent(info);
        appendSummaryRow(rows, getTipePelangganLabel(), getNamaPelanggan(anggota));
        appendSummaryRow(rows, "Toko/Merchant", getSelectedTokoNama());
        appendSummaryRow(rows, "Item dipilih", labelJumlahDipilih = new Label(ais.common.Common.getBahasaConfig("0 item")));
        appendSummaryRow(rows, "Produk POS", labelProdukDipilih = new Label(ais.common.Common.getBahasaConfig("0 produk")));
        appendSummaryRow(rows, "Total tagihan", labelTotalTagihan = new Label("0"));
        appendSummaryRow(rows, "Total produk", labelTotalProduk = new Label("0"));
        appendSummaryRow(rows, "Pembayaran / Topup", labelTotalTopup = new Label("0"));
        appendSummaryRow(rows, "Grand Total", labelGrandTotal = new Label("0"));

        renderPaymentButtons(box, anggota);
        renderPosHelpPanel(box);
    }




    private void appendSummaryRow(Rows rows, String label, String value) {
        appendSummaryRow(rows, label, new Label(value == null ? "" : value));
    }

    private void appendSummaryRow(Rows rows, String label, Component valueComp) {
        Row row = new Row();
        row.setParent(rows);
        row.setValign("middle");
        row.setStyle("border:0;background:transparent;");
        Label l = new Label(label);
        l.setStyle("font-size:11px;color:#64748b;");
        row.appendChild(l);
        if (valueComp instanceof Label) {
            ((Label) valueComp).setStyle("font-size:12px;color:#0f172a;font-weight:bold;text-align:right;");
        }
        row.appendChild(valueComp);
    }






    private Hbox createRowFormPembayaranPos(Vbox parent, String label) {
        Hbox row = new Hbox();
        row.setParent(parent);
        row.setWidth("100%");
        row.setAlign("center");
        row.setStyle("margin-bottom:8px;");
        Label lbl = new Label(label);
        lbl.setWidth("112px");
        lbl.setStyle("font-size:11px;color:#475569;font-weight:900;");
        row.appendChild(lbl);
        Hbox fieldBox = new Hbox();
        fieldBox.setWidth("100%");
        fieldBox.setParent(row);
        fieldBox.setStyle("min-width:0;");
        return fieldBox;
    }


    private void isiCaraPembayaranPos() {
        if (caraPembayaranPos == null) {
            return;
        }
        caraPembayaranPos.getItems().clear();
        List<CaraPembayaranKoperasi> list = getCaraPembayaranManual();
        Comboitem selected = null;
        for (int i = 0; i < list.size(); i++) {
            CaraPembayaranKoperasi cara = (CaraPembayaranKoperasi) list.get(i);
            if (cara == null) {
                continue;
            }
            Comboitem item = new Comboitem();
            item.setLabel(safe(cara.getNama()));
            item.setValue(cara);
            caraPembayaranPos.appendChild(item);
            if (selected == null) {
                selected = item;
            }
            if (isTunai(cara)) {
                selected = item;
                break;
            }
        }

        if (selected == null) {
            Comboitem item = new Comboitem();
            item.setLabel("Tunai");
            item.setValue(null);
            caraPembayaranPos.appendChild(item);
            selected = item;
        }
        caraPembayaranPos.setSelectedItem(selected);
    }

    private CaraPembayaranKoperasi getCaraPembayaranPosTerpilih() {
        try {
            if (caraPembayaranPos == null || caraPembayaranPos.getSelectedItem() == null) {
                return null;
            }
            Object value = caraPembayaranPos.getSelectedItem().getValue();
            if (value instanceof CaraPembayaranKoperasi) {
                return (CaraPembayaranKoperasi) value;
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/koperasi/helper/PembayaranKoperasiOnline.java:1281");
        }
        return null;
    }

    private boolean isTunai(CaraPembayaranKoperasi cara) {
        String nama = cara == null ? "tunai" : safe(cara.getNama()).toLowerCase();
        return nama.indexOf("tunai") >= 0 || nama.indexOf("cash") >= 0;
    }

    private boolean isCaraPembayaranOnline(CaraPembayaranKoperasi cara) {
        String nama = cara == null ? "" : safe(cara.getNama()).toLowerCase();
        return nama.indexOf("online") >= 0 || nama.indexOf("qris") >= 0 || nama.indexOf("qr") >= 0
                || nama.indexOf("virtual") >= 0 || nama.indexOf("va") >= 0 || nama.indexOf("topup") >= 0;
    }

    private double getUangDiterimaTunai() {
        try {
            if (uangDiterimaTunai == null || uangDiterimaTunai.getValue() == null) {
                return 0.0;
            }
            return uangDiterimaTunai.getValue().doubleValue();
        } catch (Exception e) {
            return 0.0;
        }
    }

    private void updatePanelCaraBayarPos() {
        CaraPembayaranKoperasi cara = getCaraPembayaranPosTerpilih();
        boolean tunai = isTunai(cara);
        if (uangDiterimaTunai != null) {
            uangDiterimaTunai.setDisabled(!tunai);
            if (!tunai) {
                uangDiterimaTunai.setValue(0.0);
            }
        }
        if (labelCaraBayarAktif != null) {
            if (tunai) {
                labelCaraBayarAktif.setValue("Default Tunai/Cash aktif. Isi uang diterima untuk menghitung kembalian seperti alur POS.");
            } else if (isCaraPembayaranOnline(cara)) {
                labelCaraBayarAktif.setValue("Metode online/QRIS/VA terpilih. Gunakan Bayar Sekarang atau Bayar Online/VA untuk membuat transaksi online.");
            } else {
                labelCaraBayarAktif.setValue("Metode pembayaran " + safe(cara == null ? "Tunai" : cara.getNama()) + " terpilih.");
            }
        }
        updateKembalianTunaiAman();
    }

    private void updateKembalianTunaiAman() {
        if (labelKembalianTunai == null) {
            return;
        }
        double uang = getUangDiterimaTunai();
        double kembali = uang - t;
        labelKembalianTunai.setValue(formatMoney(kembali < 0.0 ? 0.0 : kembali));
        labelKembalianTunai.setStyle("font-size:12px;color:" + (kembali < 0.0 ? "#dc2626" : "#16a34a")
                + ";font-weight:900;min-width:90px;text-align:right;");
    }

    private boolean validasiUangTunaiJikaPerlu(CaraPembayaranKoperasi cara) throws Exception {
        if (!isTunai(cara)) {
            return true;
        }
        double uang = getUangDiterimaTunai();
        if (uang <= 0.1) {
            if (uangDiterimaTunai != null) {
                uangDiterimaTunai.setValue(new Double(t));
                updateKembalianTunaiAman();
            }
            return true;
        }
        if (uang + 0.1 < t) {
            MyMessageboxConfig.show("Uang tunai kurang. Total bayar " + formatMoney(t) + ", uang diterima "
                    + formatMoney(uang) + ".", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
            return false;
        }
        return true;
    }



    private void renderPaymentButtons(Vbox parent, final AnggotaKoperasi anggota) {
        if (parent == null) {
            return;
        }
        Vbox panel = new Vbox();
        panel.setParent(parent);
        panel.setWidth("100%");
        panel.setStyle("background:#ffffff;border:1px solid #dbe4f0;border-radius:18px;padding:14px;box-sizing:border-box;"
                + "margin-bottom:12px;box-shadow:0 10px 24px rgba(15,23,42,.06);overflow:hidden;");
        panel.appendChild(new ais.ui.util.MyHtml("<div style='font-size:15px;font-weight:900;color:#0f172a;margin-bottom:5px;'>Aksi Pembayaran</div>"
                + "<div style='font-size:11.5px;color:#64748b;line-height:1.55;margin-bottom:10px;'>"
                + "Pilih metode pembayaran seperti kasir POS. Secara default sistem memilih Tunai/Cash jika tersedia. Input uang diterima dan kembalian dibuat dalam baris terpisah agar tidak terpotong pada panel kanan.</div>"));

        Vbox formBayar = new Vbox();
        formBayar.setParent(panel);
        formBayar.setWidth("100%");
        formBayar.setStyle("background:#f8fafc;border:1px solid #e2e8f0;border-radius:14px;padding:10px;"
                + "box-sizing:border-box;margin-bottom:10px;");

        Hbox rowMetode = createRowFormPembayaranPos(formBayar, "Metode");
        caraPembayaranPos = new Combobox();
        caraPembayaranPos.setReadonly(true);
        caraPembayaranPos.setWidth("100%");
        caraPembayaranPos.setStyle("border-radius:10px;background:#ffffff;font-weight:bold;");
        rowMetode.appendChild(caraPembayaranPos);
        isiCaraPembayaranPos();

        Hbox rowUang = createRowFormPembayaranPos(formBayar, "Uang Diterima");
        uangDiterimaTunai = new MyDoublebox(0.0);
        uangDiterimaTunai.setWidth("100%");
        uangDiterimaTunai.setStyle("text-align:right;border-radius:10px;font-weight:bold;");
        rowUang.appendChild(uangDiterimaTunai);

        Hbox rowKembali = createRowFormPembayaranPos(formBayar, "Kembalian");
        labelKembalianTunai = new Label("0");
        labelKembalianTunai.setStyle("display:block;width:100%;font-size:13px;color:#dc2626;font-weight:900;text-align:right;"
                + "background:#ffffff;border:1px solid #e2e8f0;border-radius:10px;padding:6px 8px;box-sizing:border-box;");
        rowKembali.appendChild(labelKembalianTunai);

        caraPembayaranPos.addEventListener("onChange", new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                updatePanelCaraBayarPos();
            }
        });
        uangDiterimaTunai.addEventListener("onChange", new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                updateKembalianTunaiAman();
            }
        });

        labelCaraBayarAktif = new Label("");
        labelCaraBayarAktif.setParent(panel);
        labelCaraBayarAktif.setStyle("display:block;padding:9px 11px;border-radius:12px;background:#eff6ff;color:#1d4ed8;"
                + "font-size:11.5px;font-weight:700;line-height:1.45;margin-bottom:10px;white-space:normal;");
        updatePanelCaraBayarPos();

        Vbox buttons = new Vbox();
        buttons.setParent(panel);
        buttons.setWidth("100%");
        buttons.setStyle("gap:8px;");

        MyButtonConfig bayarSekarang = new MyButtonConfig("Bayar Sekarang", "/img/svg/payments.svg");
        bayarSekarang.setWidth("100%");
        bayarSekarang.setHeight("44px");
        bayarSekarang.setStyle("font-weight:900;background:#16a34a;color:#ffffff;border:0;border-radius:14px;"
                + "box-shadow:0 10px 20px rgba(22,163,74,.22);");
        buttons.appendChild(bayarSekarang);
        bayarSekarang.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                CaraPembayaranKoperasi cara = getCaraPembayaranPosTerpilih();
                if (isCaraPembayaranOnline(cara)) {
                    prosesOnlineDenganKonfirmasi();
                    return;
                }
                if (!validasiUangTunaiJikaPerlu(cara)) {
                    return;
                }
                konfirmasiBayarManual(cara, anggota);
            }
        });

        if (isOnlinePaymentActive()) {
            MyButtonConfig bayarOnline = new MyButtonConfig("Bayar Online / VA", "/img/svg/credit-card.svg");
            bayarOnline.setWidth("100%");
            bayarOnline.setHeight("42px");
            bayarOnline.setStyle("font-weight:900;background:#2563eb;color:#ffffff;border:0;border-radius:14px;"
                    + "box-shadow:0 10px 20px rgba(37,99,235,.22);");
            buttons.appendChild(bayarOnline);
            bayarOnline.addEventListener("onClick", new EventListener() {
                @Override
                public void onEvent(Event event) throws Exception {
                    prosesOnlineDenganKonfirmasi();
                }
            });
            if (langsungBayar) {
                Common.createDefaultTimer(new EventListener() {
                    @Override
                    public void onEvent(Event event) throws Exception {
                        prosesOnlineLangsung();
                    }
                }, "", false, 1000);
            }
        }

        MyButtonConfig kosongkan = new MyButtonConfig("Kosongkan Keranjang", "/img/svg/trash.svg");
        kosongkan.setWidth("100%");
        kosongkan.setHeight("38px");
        kosongkan.setStyle("font-weight:bold;background:#f8fafc;color:#334155;border:1px solid #cbd5e1;border-radius:12px;");
        buttons.appendChild(kosongkan);
        kosongkan.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                kosongkanKeranjang();
            }
        });
    }






    private void renderPaymentButtons(final AnggotaKoperasi anggota) {
        /*
         * Kompatibilitas method lama. Jangan langsung menambahkan child ke East,
         * karena LayoutRegion East hanya boleh memiliki satu child.
         */
        if (east == null || east.getChildren() == null || east.getChildren().isEmpty()) {
            return;
        }
        Object child = east.getChildren().get(0);
        if (child instanceof Vbox) {
            renderPaymentButtons((Vbox) child, anggota);
        }
    }




    private PembayaranAnggotaKoperasi simpanPembayaranKoperasi(AnggotaKoperasi anggota,
            Collection<TransaksiKoperasiDetail> detailDipilih, Double nilaiTopup, String validator,
            CaraPembayaranKoperasi caraPembayaranKoperasi, java.util.Date tanggalTransaksi) throws Exception {
        double totalDetail = 0.0;
        if (detailDipilih != null) {
            for (TransaksiKoperasiDetail detail : detailDipilih) {
                totalDetail += hitungNominal(detail);
            }
        }

        double totalTopup = nilaiTopup == null ? 0.0 : nilaiTopup.doubleValue();
        double totalProduk = getTotalProdukDipilih();
        double total = totalDetail + totalTopup + totalProduk;

        if (total <= 0.1) {
            MyMessageboxConfig.show("Mohon maaf, belum ada transaksi yang dipilih. Langkah yang dapat dilakukan: (1) pilih transaksi dari daftar tagihan; (2) tambahkan produk POS; atau (3) isi nilai pembayaran/topup, lalu ulangi proses.",
                    "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
            return null;
        }
        if (anggota == null || anggota.getId() == null) {
            MyMessageboxConfig.show("Mohon maaf, data pelanggan belum dapat disiapkan sebagai anggota koperasi. Langkah yang dapat dilakukan: (1) pastikan pelanggan sudah terdaftar sebagai anggota koperasi; (2) daftarkan pelanggan melalui menu Anggota Koperasi; (3) ulangi proses pembayaran.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
            return null;
        }

        StringBuilder keterangan = new StringBuilder();
        keterangan.append("Pembayaran POS koperasi");
        if (detailDipilih != null && !detailDipilih.isEmpty()) {
            keterangan.append(" angsuran: ");
            for (TransaksiKoperasiDetail transaksiKoperasiDetail : detailDipilih) {
                if (transaksiKoperasiDetail != null) {
                    keterangan.append(" ke-").append(transaksiKoperasiDetail.getKe()).append(", ");
                }
            }
        }
        if (totalTopup > 0.1) {
            keterangan.append(" topup/pembayaran: ").append(formatMoney(totalTopup));
        }
        if (totalProduk > 0.1) {
            keterangan.append(" pembelian produk: ").append(buildRingkasanProdukDipilih());
        }

        Session session = HibernateUtil.currentNativeSession();
        Transaction transaction = null;
        boolean mulaiTransaksiBaru = false;
        try {
            transaction = session.getTransaction();
            if (transaction == null || !transaction.isActive()) {
                transaction = session.beginTransaction();
                mulaiTransaksiBaru = true;
            }

            PembayaranAnggotaKoperasi pembayaran = new PembayaranAnggotaKoperasi();
            pembayaran.setAnggotaKoperasi(anggota);
            pembayaran.setTanggal(tanggalTransaksi);
            pembayaran.setTanggalBayar(tanggalTransaksi);
            pembayaran.setKeterangan(keterangan.toString());
            pembayaran.setCaraPembayaranKoperasi(caraPembayaranKoperasi);
            pembayaran.setNominal(new Double(total));

            /*
             * Tambahan deposit hanya untuk nilai topup/pembayaran tambahan.
             * Nominal produk POS tidak boleh otomatis dianggap sebagai deposit/tabungan.
             */
            pembayaran.setTambahanDeposit(new Double(totalTopup > 0.1 ? totalTopup : 0.0));
            pembayaran.setValidator(validator);

            session.save(pembayaran);

            if (detailDipilih != null) {
                for (TransaksiKoperasiDetail transaksiKoperasiDetail : detailDipilih) {
                    if (transaksiKoperasiDetail == null || transaksiKoperasiDetail.getId() == null) {
                        continue;
                    }
                    PembayaranAnggotaKoperasiDetail anggotaKoperasiDetail = new PembayaranAnggotaKoperasiDetail();
                    anggotaKoperasiDetail.setNominal(new Double(hitungNominal(transaksiKoperasiDetail)));
                    anggotaKoperasiDetail.setPembayaranAnggotaKoperasi(pembayaran);
                    anggotaKoperasiDetail.setTransaksiKoperasiDetail(transaksiKoperasiDetail);
                    session.save(anggotaKoperasiDetail);

                    transaksiKoperasiDetail.setPembayaranAnggotaKoperasiDetail(anggotaKoperasiDetail);
                    Common.refreshUpdate(session, transaksiKoperasiDetail);
                }
            }

            if (mulaiTransaksiBaru && transaction != null && transaction.isActive()) {
                transaction.commit();
            }
            return pembayaran;
        } catch (Exception e) {
            if (mulaiTransaksiBaru && transaction != null && transaction.isActive()) {
                try {
                    transaction.rollback();
                } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/koperasi/helper/PembayaranKoperasiOnline.java:1597");
                }
            }
            throw e;
        } finally {
            /*
             * currentNativeSession() tidak ditutup di sini karena dikelola oleh lifecycle
             * Hibernate/AIS. Hindari session.close() agar tidak mengganggu proses UI lain.
             */
        }
    }



    private List<CaraPembayaranKoperasi> getCaraPembayaranManual() {
        /*
         * Disamakan dengan _pos.jsp:
         * SELECT id, nama, manual FROM koperasi.cara_pembayaran_koperasi
         * WHERE aktif = true ORDER BY nama ASC.
         *
         * Nama method lama dipertahankan agar kompatibel, tetapi isinya sekarang
         * mengambil semua cara bayar aktif, bukan hanya manual=true. Tunai/Cash
         * dipindahkan ke urutan pertama dan otomatis dipilih sebagai default.
         */
        List<CaraPembayaranKoperasi> result = new ArrayList<CaraPembayaranKoperasi>();
        try {
            Session session = HibernateUtil.currentSession();
            List<CaraPembayaranKoperasi> list = ConstantValues.simpleList(session.createCriteria(CaraPembayaranKoperasi.class)
                    .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                    .addOrder(Order.asc("nama")), CaraPembayaranKoperasi.class);
            if (list == null) {
                return result;
            }
            for (int i = 0; i < list.size(); i++) {
                CaraPembayaranKoperasi cara = (CaraPembayaranKoperasi) list.get(i);
                if (cara == null) {
                    continue;
                }
                if (isTunai(cara)) {
                    result.add(0, cara);
                } else {
                    result.add(cara);
                }
            }
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
        }
        return result;
    }


    private void konfirmasiBayarManual(final CaraPembayaranKoperasi cara, final AnggotaKoperasi anggotaParam)
            throws Exception {
        if (!checkKondisiSebelumbayar()) {
            return;
        }
        final AnggotaKoperasi anggota = getAnggotaUntukPembayaran(true);
        final double biayaAdministrasi = getBiayaAdministrasi(cara);
        String message = "Apakah yakin ingin melakukan pembayaran via " + safe(cara == null ? "" : cara.getNama())
                + "?\n\n" + getTipePelangganLabel() + ": " + getNamaPelanggan(anggota)
                + "\nTotal tagihan: " + formatMoney(t)
                + (biayaAdministrasi > 0.1 ? "\nBiaya administrasi: " + formatMoney(biayaAdministrasi)
                        + "\nTotal dibayar: " + formatMoney(t + biayaAdministrasi) : "")
                + "\nTerbilang: " + IndonesianNumberToWords.convert((long) (t + biayaAdministrasi));

        MyMessageboxConfig.show(message, "Konfirmasi Pembayaran", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
                MyMessageboxConfig.QUESTION, new EventListener() {
                    @Override
                    public void onEvent(final Event event) throws Exception {
                        int pilihan = Integer.parseInt(event.getData().toString());
                        if (pilihan == MyMessageboxConfig.OK) {
                            Common.createDefaultTimer(new EventListener() {
                                @Override
                                public void onEvent(Event arg0) throws Exception {
                                    simpanPembayaranManual(anggota, cara);
                                }
                            }, "Memproses pembayaran koperasi...");
                        }
                    }
                });
    }



    private void cetakStrukPembayaranAman(PembayaranAnggotaKoperasi pembayaran) {
        if (pembayaran == null || pembayaran.getId() == null) {
            return;
        }
        try {
            PembayaranAnggotaKoperasi pembayaranCetak = pembayaran;
            try {
                Session session = HibernateUtil.currentSession();
                PembayaranAnggotaKoperasi fresh = (PembayaranAnggotaKoperasi) session.get(PembayaranAnggotaKoperasi.class,
                        pembayaran.getId());
                if (fresh != null) {
                    pembayaranCetak = fresh;
                    /*
                     * Force initialize relasi yang dipakai PembayaranAnggotaKoperasiAction.kirim(...)
                     * agar tidak LazyInitializationException saat cetak struk.
                     */
                    if (fresh.getAnggotaKoperasi() != null) {
                        fresh.getAnggotaKoperasi().getNama();
                        if (fresh.getAnggotaKoperasi().getKoperasi() != null) {
                            fresh.getAnggotaKoperasi().getKoperasi().getNama();
                        }
                    }
                    if (fresh.getCaraPembayaranKoperasi() != null) {
                        fresh.getCaraPembayaranKoperasi().getNama();
                    }
                }
            } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/koperasi/helper/PembayaranKoperasiOnline.java:1707");
            }
            PembayaranAnggotaKoperasiAction.cetakStruk(pembayaranCetak);
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
            try {
                MyMessageboxConfig.show("Pembayaran berhasil disimpan. Struk dapat dicetak ulang dari Riwayat Pembayaran.",
                        "Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/koperasi/helper/PembayaranKoperasiOnline.java:1715");
            }
        }
    }


    private void simpanPembayaranManual(final AnggotaKoperasi anggotaParam, CaraPembayaranKoperasi cara) throws Exception {
        Tbmuser tbmuser = Common.getCurrentUser();
        AnggotaKoperasi anggota = anggotaParam == null ? getAnggotaUntukPembayaran(true) : anggotaParam;
        boolean adaProduk = produkKoperasiDipilih != null && !produkKoperasiDipilih.isEmpty();
        boolean adaTagihan = transaksiKoperasiDetails != null && !transaksiKoperasiDetails.isEmpty();
        boolean adaTopup = getDepositValue() > 0.1;

        /*
         * Pembelian produk POS boleh dilakukan tanpa anggota. Dalam kondisi ini data
         * disimpan ke tabel pembelian POS native saja, sedangkan pembayaran tagihan/topup
         * tetap memakai PembayaranAnggotaKoperasi seperti alur lama.
         */
        if (anggota == null && adaProduk && !adaTagihan && !adaTopup) {
            simpanPembelianProdukPosNative(null, cara, null);
            MyMessageboxConfig.show("Pembelian produk berhasil diproses sebagai transaksi non anggota.",
                    "Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            produkKoperasiDipilih.clear();
            hitungUlangTransaksiKoperasiDetail();
            reloadTransaksiKoperasiDetail();
            return;
        }

        PembayaranAnggotaKoperasi pembayaran = simpanPembayaranKoperasi(anggota,
                transaksiKoperasiDetails == null ? new ArrayList<TransaksiKoperasiDetail>()
                        : transaksiKoperasiDetails.values(),
                new Double(getDepositValue()), tbmuser == null ? "" : safe(tbmuser.getUserNama()), cara,
                tanggalTransaski == null || tanggalTransaski.getValue() == null ? WaktuUtil.getDate()
                        : tanggalTransaski.getValue());
        hitungUlangTransaksiKoperasiDetail();
        if (pembayaran != null) {
            simpanPembelianProdukPosNative(anggota, cara, pembayaran);
            cetakStrukPembayaranAman(pembayaran);
        }
        produkKoperasiDipilih.clear();
        reloadTransaksiKoperasiDetail();
    }


    private void prosesOnlineLangsung() throws Exception {
        if (!adaPelangganAktif() || transaksiKoperasiDetails == null
                || (transaksiKoperasiDetails.isEmpty() && getDepositValue() <= 0.1)) {
            return;
        }
        if (!produkKoperasiDipilih.isEmpty()) {
            return;
        }
        prosesOnline(false);
    }

    private void prosesOnlineDenganKonfirmasi() throws Exception {
        if (!checkKondisiSebelumbayar()) {
            return;
        }
        if (!produkKoperasiDipilih.isEmpty()) {
            MyMessageboxConfig.show("Pembelian produk POS saat ini diproses melalui pembayaran manual/kasir. Silakan kosongkan produk jika ingin membuat pembayaran online/VA untuk tagihan koperasi.",
                    "Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return;
        }
        final AnggotaKoperasi anggota = getAnggotaUntukPembayaran(true);
        final CaraPembayaranKoperasi cara = getCaraPembayaranOnline(anggota);
        if (!validasiCaraOnline(cara)) {
            return;
        }
        final double biayaAdministrasi = safeDouble(cara.getKanalPembayaran().getBiayaAdminEsmartlink());
        String variableAdmin = safe(cara.getKanalPembayaran().getVariableBiayaAdminEsmartlink());
        if (variableAdmin.length() > 0) {
            Common.createDefaultTimer(new EventListener() {
                @Override
                public void onEvent(Event event) throws Exception {
                    prosesOnline(true);
                }
            }, "Memproses pembayaran online...");
            return;
        }

        MyMessageboxConfig.show("Apakah yakin ingin melakukan pembayaran online?\n\n" + getTipePelangganLabel()
                + ": " + getNamaPelanggan(anggota) + "\nTotal tagihan: " + formatMoney(t)
                + (biayaAdministrasi > 0.1 ? "\nBiaya administrasi: " + formatMoney(biayaAdministrasi)
                        + "\nTotal dibayar: " + formatMoney(t + biayaAdministrasi) : "")
                + "\nTerbilang: " + IndonesianNumberToWords.convert((long) (t + biayaAdministrasi)),
                "Konfirmasi Pembayaran Online", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
                MyMessageboxConfig.QUESTION, new EventListener() {
                    @Override
                    public void onEvent(Event event) throws Exception {
                        int i = Integer.parseInt(event.getData().toString());
                        if (i == MyMessageboxConfig.OK) {
                            Common.createDefaultTimer(new EventListener() {
                                @Override
                                public void onEvent(Event event) throws Exception {
                                    prosesOnline(false);
                                }
                            }, "Memproses pembayaran online...");
                        }
                    }
                });
    }

    private void prosesOnline(boolean update) throws Exception {
        AnggotaKoperasi anggota = getAnggotaUntukPembayaran(true);
        CaraPembayaranKoperasi cara = getCaraPembayaranOnline(anggota);
        if (!validasiCaraOnline(cara)) {
            return;
        }

        double biayaAdministrasi = safeDouble(cara.getKanalPembayaran().getBiayaAdminEsmartlink());
        BankHost bankHost = PembayaranUtil.getInstance().getBankHost(
                Common.getKonfigurasi("online_bank_host_ip", "").getNilai(), "Bank Host");

        Map param = new HashMap();
        param.put("esmartlink", Boolean.TRUE);
        if (update) {
            param.put("update", Boolean.TRUE);
        }

        VirtualAccountBank virtualAccountBank = DownloadTagihanAnggotaKoperasiBankOnline.downloadData(anggota,
                transaksiKoperasiDetails.values(), param, new Double(biayaAdministrasi), bankHost, cara);
        if (virtualAccountBank != null && !isEmpty(virtualAccountBank.getLink())) {
            Common.displayWindowIframe(virtualAccountBank.getLink(), true, "600px", "95%", "Pembayaran Online");
        } else {
            MyMessageboxConfig.show("Mohon maaf, transaksi online gagal dibuat. Langkah yang dapat dilakukan: (1) periksa konfigurasi kanal pembayaran koperasi di menu Pengaturan; (2) pastikan API kanal pembayaran aktif dan terkoneksi; (3) hubungi Administrator untuk verifikasi konfigurasi.",
                    "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
        }
        reloadTransaksiKoperasiDetail();
    }

    private CaraPembayaranKoperasi getCaraPembayaranOnline(AnggotaKoperasi anggota) {
        if (anggota == null || anggota.getKoperasi() == null) {
            return null;
        }
        Session session = HibernateUtil.currentSession();
        return (CaraPembayaranKoperasi) ConstantValues.simpleObject(session.createCriteria(CaraPembayaranKoperasi.class)
                .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                .add(Restrictions.eq("manual", false)).add(Restrictions.eq("koperasi", anggota.getKoperasi()))
                .setMaxResults(1), CaraPembayaranKoperasi.class);
    }

    private boolean validasiCaraOnline(CaraPembayaranKoperasi cara) throws Exception {
        if (cara == null || cara.getKanalPembayaran() == null) {
            MyMessageboxConfig.show("Mohon maaf, kanal pembayaran online koperasi belum dikonfigurasi. Langkah yang dapat dilakukan: (1) buka menu Konfigurasi Kanal Pembayaran; (2) aktifkan dan konfigurasikan kanal yang diinginkan; (3) hubungi Administrator atau tim teknis untuk bantuan konfigurasi.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        return true;
    }

    private double getBiayaAdministrasi(CaraPembayaranKoperasi cara) {
        try {
            if (cara == null || cara.getId() == null) {
                return 0.0;
            }
            return Double.parseDouble(Common.getKonfigurasi(cara.getId() + "_biaya_administrasi", "0.0").getNilai());
        } catch (Exception e) {
            return 0.0;
        }
    }

    private boolean isOnlinePaymentActive() {
        try {
            return Common.bolehKonfigurasi("aktifkan_va_e_smartlink", Konfigurasi.TIDAK_AKTIF);
        } catch (Exception e) {
            return false;
        }
    }



    private void reloadRiwayatPembayaran(final AnggotaKoperasi anggota, final Row sub) {
        Vbox eastBox = getEastContentBoxAman();
        if (eastBox == null) {
            return;
        }
        Vbox container = new Vbox();
        container.setParent(eastBox);
        container.setWidth("100%");
        container.setStyle("background:#ffffff;border:1px solid #dbe4f0;border-radius:18px;padding:12px;"
                + "box-sizing:border-box;box-shadow:0 10px 24px rgba(15,23,42,.06);margin-top:12px;");
        container.appendChild(new ais.ui.util.MyHtml(
                "<div style='display:flex;justify-content:space-between;align-items:center;gap:8px;margin-bottom:8px;'>"
                        + "<div><div style='font-size:15px;font-weight:900;color:#0f172a;'>Riwayat Pembayaran Terakhir</div>"
                        + "<div style='font-size:11.5px;color:#64748b;line-height:1.5;'>"
                        + "Riwayat transaksi terbaru membantu kasir mengecek pembayaran terakhir, menelusuri transaksi yang baru dibuat, dan mencetak ulang struk bila pelanggan membutuhkan bukti pembayaran.</div></div>"
                        + "</div>"));

        MyGrid grid = new MyGrid();
        grid.setWidth("100%");
        grid.setParent(container);
        grid.setHeight("295px");
        grid.setStyle("border:0;overflow-y:auto;");

        Columns columns = new Columns();
        columns.setParent(grid);
        createColumn(columns, "Waktu", "100px", null);
        createColumn(columns, "Ke", "42px", null);
        createColumn(columns, "Transaksi", null, null);
        createColumn(columns, "Via", "80px", null);
        createColumn(columns, "Nominal", "90px", "right");

        Rows rows = new Rows();
        rows.setParent(grid);

        if (anggota == null) {
            rows.appendChild(emptyRow("Belum ada anggota dipilih."));
            renderRiwayatPembelianPosNative(container, null);
            return;
        }

        List<TransaksiKoperasiDetail> list = getRiwayatPembayaran(anggota);
        if (list.isEmpty()) {
            rows.appendChild(emptyRow("Belum ada riwayat pembayaran."));
        } else {
            ProdukKoperasi produkKoperasi = null;
            for (int i = 0; i < list.size(); i++) {
                TransaksiKoperasiDetail detail = (TransaksiKoperasiDetail) list.get(i);
                if (detail == null || !detail.getAktif()) {
                    continue;
                }
                TransaksiKoperasi transaksi = detail.getTransaksiKoperasi();
                ProdukKoperasi produk = transaksi == null ? null : transaksi.getProdukKoperasi();
                if (produk != null && (produkKoperasi == null || !produkKoperasi.getId().equals(produk.getId()))) {
                    Group group = new Group(safe(produk.getNama()));
                    group.setParent(rows);
                    produkKoperasi = produk;
                }
                renderRiwayatRow(rows, detail, anggota, sub);
            }
        }
        renderRiwayatPembelianPosNative(container, anggota);
    }


    private List<TransaksiKoperasiDetail> getRiwayatPembayaran(AnggotaKoperasi anggota) {
        try {
            Session session = HibernateUtil.currentSession();
            return ConstantValues.simpleList(session.createCriteria(TransaksiKoperasiDetail.class)
                    .createAlias("transaksiKoperasi", "transaksiKoperasi")
                    .createAlias("pembayaranAnggotaKoperasiDetail", "pembayaranAnggotaKoperasiDetail")
                    .createAlias("pembayaranAnggotaKoperasiDetail.pembayaranAnggotaKoperasi", "pembayaranAnggotaKoperasi")
                    .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                    .add(Restrictions.eq("pembayaranAnggotaKoperasi.anggotaKoperasi", anggota))
                    .addOrder(Order.desc("pembayaranAnggotaKoperasiDetail.id"))
                    .addOrder(Order.desc("transaksiKoperasi.produkKoperasi.id")).addOrder(Order.asc("tanggal"))
                    .setMaxResults(100), TransaksiKoperasiDetail.class);
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
            return new ArrayList<TransaksiKoperasiDetail>();
        }
    }

    private void renderRiwayatRow(Rows rows, TransaksiKoperasiDetail detail, final AnggotaKoperasi anggota,
            final Row sub) {
        PembayaranAnggotaKoperasiDetail bayarDetail = detail.getPembayaranAnggotaKoperasiDetail();
        if (bayarDetail == null || bayarDetail.getPembayaranAnggotaKoperasi() == null) {
            return;
        }
        Row row = new Row();
        row.setValign("top");
        row.setParent(rows);

        Vbox revisiBox = RevisiHelper.createNewRevisi(PembayaranAnggotaKoperasiDetail.class, bayarDetail,
                Common.dateFormat3.get().format(bayarDetail.getPembayaranAnggotaKoperasi().getTanggal()));
        row.appendChild(revisiBox);

        if (bolehEditPembayaran(bayarDetail.getPembayaranAnggotaKoperasi())) {
            MyToolbarbuttonConfig edit = new MyToolbarbuttonConfig("", "/img/svg/pencil-square.svg");
            edit.setTooltiptext("Ubah tanggal/cara bayar");
            edit.setParent(revisiBox);
            edit.addEventListener("onClick", new EventListener() {
                @Override
                public void onEvent(Event event) throws Exception {
                    MyMessageboxConfig.show("Fungsi ubah pembayaran tetap tersedia melalui riwayat pembayaran utama.",
                            "Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
                }
            });
        }

        row.appendChild(new Label(String.valueOf(detail.getKe())));
        row.appendChild(new Label(buildNamaTransaksi(detail)));
        row.appendChild(new Label(bayarDetail.getPembayaranAnggotaKoperasi().getCaraPembayaranKoperasi() == null ? ""
                : safe(bayarDetail.getPembayaranAnggotaKoperasi().getCaraPembayaranKoperasi().getNama())));
        row.appendChild(new Label(formatMoney(bayarDetail.ambilNominal())));
    }

    private boolean bolehEditPembayaran(PembayaranAnggotaKoperasi pembayaran) {
        try {
            Tbmuser tbmuser = Common.getCurrentUser();
            VirtualAccountBank virtualAccountBank = pembayaran == null ? null : pembayaran.getVirtualAccountBank();
            return virtualAccountBank == null && ((tbmuser != null && tbmuser.getAnggotaKoperasi() == null)
                    || Common.getApakahAdmin());
        } catch (Exception e) {
            return false;
        }
    }



    private void renderEmptyHistory(String message) {
        Vbox eastBox = getEastContentBoxAman();
        if (eastBox == null) {
            return;
        }
        Vbox container = new Vbox();
        container.setParent(eastBox);
        container.setWidth("100%");
        container.setStyle("background:#ffffff;border:1px solid #dbe4f0;border-radius:18px;padding:12px;"
                + "box-sizing:border-box;box-shadow:0 10px 24px rgba(15,23,42,.06);margin-top:12px;");
        container.appendChild(new ais.ui.util.MyHtml("<div style='font-size:14px;font-weight:900;color:#0f172a;margin-bottom:6px;'>Riwayat Pembayaran</div>"
                + "<div style='font-size:11.5px;color:#64748b;line-height:1.55;'>" + html(message) + "</div>"));
    }



    private void renderPosMiniDashboard(Vbox parent, AnggotaKoperasi anggota) {
        Map<String, Object> data = getRingkasanPosNative(anggota);
        long transaksiHariIni = mapLong(data, "jumlah_transaksi");
        double totalHariIni = mapDouble(data, "total_hari_ini");
        long itemBelumDibayar = mapLong(data, "item_belum_dibayar");
        double totalBelumDibayar = mapDouble(data, "total_belum_dibayar");

        StringBuilder sb = new StringBuilder();
        sb.append("<div style='display:grid;grid-template-columns:repeat(auto-fit,minmax(165px,1fr));gap:10px;margin-bottom:12px;'>");
        sb.append(buildMiniKpiHtml("Transaksi Hari Ini", String.valueOf(transaksiHariIni),
                "Jumlah pembayaran koperasi yang tersimpan hari ini.", "#eff6ff", "#1d4ed8"));
        sb.append(buildMiniKpiHtml("Total Hari Ini", formatMoney(totalHariIni),
                "Akumulasi nominal pembayaran koperasi hari ini.", "#ecfdf5", "#166534"));
        sb.append(buildMiniKpiHtml("Item Belum Dibayar", String.valueOf(itemBelumDibayar),
                "Jumlah tagihan pelanggan yang masih terbuka.", "#fff7ed", "#9a3412"));
        sb.append(buildMiniKpiHtml("Sisa Tagihan", formatMoney(totalBelumDibayar),
                "Estimasi nilai tagihan pelanggan yang belum lunas.", "#fef2f2", "#991b1b"));
        sb.append("</div>");
        parent.appendChild(new ais.ui.util.MyHtml(sb.toString()));
    }


    private String getKolomAnggotaPembayaranAman() {
        try {
            String sql = "select column_name from information_schema.columns "
                    + "where table_schema='koperasi' and table_name='pembayaran_anggota_koperasi' "
                    + "and lower(column_name) in ('anggotakoperasi_id','anggota_koperasi','anggota_koperasi_id') "
                    + "order by case lower(column_name) when 'anggotakoperasi_id' then 1 when 'anggota_koperasi' then 2 else 3 end limit 1";
            List<Map<String, Object>> list = Common.ambilSqlMap(sql);
            if (list != null && !list.isEmpty()) {
                String column = mapString((Map<String, Object>) list.get(0), "column_name");
                if (column != null && column.trim().length() > 0) {
                    return quoteIdentifier(column.trim());
                }
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/koperasi/helper/PembayaranKoperasiOnline.java:2067");
        }
        return null;
    }

    private String quoteIdentifier(String value) {
        if (value == null || value.trim().length() == 0) {
            return "";
        }
        String safeValue = value.replace("\"", "\"\"");
        return "\"" + safeValue + "\"";
    }



    private boolean kolomAdaAman(String schema, String tableName, String columnName) {
        try {
            String schemaSql = schema == null ? "" : schema.replace("'", "''");
            String tableSql = tableName == null ? "" : tableName.replace("'", "''");
            String columnSql = columnName == null ? "" : columnName.replace("'", "''").toLowerCase();
            if (schemaSql.length() == 0 || tableSql.length() == 0 || columnSql.length() == 0) {
                return false;
            }
            String sql = "select count(*) as jumlah from information_schema.columns "
                    + "where table_schema=" + quoteSql(schemaSql) + " and table_name=" + quoteSql(tableSql)
                    + " and lower(column_name)=" + quoteSql(columnSql);
            List<Map<String, Object>> list = Common.ambilSqlMap(sql);
            if (list != null && !list.isEmpty()) {
                return mapLong((Map<String, Object>) list.get(0), "jumlah") > 0L;
            }
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
        }
        return false;
    }

    private void appendInsertValue(StringBuffer kolom, StringBuffer nilai, boolean[] first, String columnName,
            String valueSql) {
        if (columnName == null || columnName.trim().length() == 0) {
            return;
        }
        if (!first[0]) {
            kolom.append(", ");
            nilai.append(", ");
        }
        kolom.append(columnName);
        nilai.append(valueSql == null ? "null" : valueSql);
        first[0] = false;
    }


    private boolean tableAdaAman(String schema, String tableName) {
        try {
            String schemaSql = schema == null ? "" : schema.replace("'", "''");
            String tableSql = tableName == null ? "" : tableName.replace("'", "''");
            if (schemaSql.length() == 0 || tableSql.length() == 0) {
                return false;
            }
            /*
             * Jangan memakai query regclass langsung. PostgreSQL mengembalikan
             * tipe regclass/OID yang pada Hibernate lama dapat terbaca sebagai JDBC type
             * 1111 dan memicu MappingException. information_schema mengembalikan count
             * numerik biasa, sehingga aman untuk Common.ambilSqlMap(...).
             */
            String sql = "select count(*) as jumlah from information_schema.tables "
                    + "where table_schema=" + quoteSql(schemaSql) + " and table_name=" + quoteSql(tableSql);
            List<Map<String, Object>> list = Common.ambilSqlMap(sql);
            if (list != null && !list.isEmpty()) {
                return mapLong((Map<String, Object>) list.get(0), "jumlah") > 0L;
            }
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
        }
        return false;
    }




    private Map<String, Object> getRingkasanPosNative(AnggotaKoperasi anggota) {
        Map<String, Object> empty = new HashMap<String, Object>();
        try {
            String anggotaFilterPembayaran = "";
            String kolomAnggotaPembayaran = getKolomAnggotaPembayaranAman();
            if (anggota != null && anggota.getId() != null && kolomAnggotaPembayaran != null
                    && kolomAnggotaPembayaran.length() > 0) {
                anggotaFilterPembayaran = " and a." + kolomAnggotaPembayaran + " = " + anggota.getId().longValue() + " ";
            }

            String anggotaFilterTagihan = anggota == null || anggota.getId() == null ? ""
                    : " and tk.anggota_koperasi = " + anggota.getId().longValue() + " ";

            String sql = "select "
                    + "(select count(*) from koperasi.pembayaran_anggota_koperasi a where date(a.tanggal)=current_date "
                    + anggotaFilterPembayaran + ") as jumlah_transaksi, "
                    + "(select coalesce(sum(a.nominal),0) from koperasi.pembayaran_anggota_koperasi a where date(a.tanggal)=current_date "
                    + anggotaFilterPembayaran + ") as total_hari_ini, "
                    + "(select count(*) from koperasi.transaksi_koperasi_detail d "
                    + " inner join koperasi.transaksi_koperasi tk on tk.id=d.transaksi_koperasi "
                    + " where d.pembayaran_anggota_koperasi_detail is null and coalesce(d.aktif,true)=true "
                    + anggotaFilterTagihan + ") as item_belum_dibayar, "
                    + "(select coalesce(sum(coalesce(d.pokok,0)+coalesce(d.margin,0)),0) from koperasi.transaksi_koperasi_detail d "
                    + " inner join koperasi.transaksi_koperasi tk on tk.id=d.transaksi_koperasi "
                    + " where d.pembayaran_anggota_koperasi_detail is null and coalesce(d.aktif,true)=true "
                    + anggotaFilterTagihan + ") as total_belum_dibayar";
            List<Map<String, Object>> list = Common.ambilSqlMap(sql);
            if (list != null && !list.isEmpty()) {
                return (Map<String, Object>) list.get(0);
            }
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
        }
        return empty;
    }


    private void renderRiwayatPembelianPosNative(Vbox parent, AnggotaKoperasi anggota) {
        try {
            List<Map<String, Object>> data = getRiwayatPembelianPosNative(anggota);
            if (data == null || data.isEmpty()) {
                return;
            }
            StringBuilder sb = new StringBuilder();
            sb.append("<div style='margin-top:12px;padding-top:10px;border-top:1px solid #e2e8f0;'>")
                    .append("<div style='font-size:13px;font-weight:900;color:#0f172a;margin-bottom:6px;'>Riwayat Pembelian POS</div>")
                    .append("<div style='font-size:11px;color:#64748b;line-height:1.5;margin-bottom:8px;'>")
                    .append("Diambil memakai SQL native melalui Common.ambilSqlMap agar transaksi POS dari tabel pembelian juga dapat terlihat di layar kasir.</div>");
            for (int i = 0; i < data.size(); i++) {
                Map<String, Object> row = (Map<String, Object>) data.get(i);
                String kode = mapString(row, "kode_transaksi");
                String waktu = mapString(row, "waktu_trx");
                String items = mapString(row, "namabarang");
                double total = mapDouble(row, "total");
                String id = mapString(row, "id_transaksi");
                sb.append("<div style='padding:9px 10px;border:1px solid #e2e8f0;border-radius:12px;margin-bottom:7px;background:#f8fafc;'>")
                        .append("<div style='display:flex;justify-content:space-between;gap:8px;'>")
                        .append("<div style='font-size:12px;font-weight:900;color:#0f172a;'>").append(html(kode.length() == 0 ? "-" : kode)).append("</div>")
                        .append("<div style='font-size:12px;font-weight:900;color:#16a34a;'>").append(html(formatMoney(total))).append("</div>")
                        .append("</div>")
                        .append("<div style='font-size:10.5px;color:#64748b;margin-top:3px;'>").append(html(waktu)).append("</div>")
                        .append("<div style='font-size:11px;color:#334155;line-height:1.45;margin-top:4px;'>").append(html(items)).append("</div>");
                if (id.length() > 0) {
                    sb.append("<a href='#' onclick=\"popupCenter({url:'").append(Common.ROOT)
                            .append("/pages/master/koperasi/cetak_struk.jsp?id=").append(html(id))
                            .append("',title:'Cetak Struk',w:420,h:650});return false;\" ")
                            .append("style='display:inline-block;margin-top:7px;padding:5px 9px;border-radius:999px;background:#0f172a;color:#fff;text-decoration:none;font-size:10px;font-weight:800;'>Cetak Struk POS</a>");
                }
                sb.append("</div>");
            }
            sb.append("</div>");
            parent.appendChild(new ais.ui.util.MyHtml(sb.toString()));
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/koperasi/helper/PembayaranKoperasiOnline.java:2218");
        }
    }


    private List<Map<String, Object>> getRiwayatPembelianPosNative(AnggotaKoperasi anggota) {
        try {
            if (!tableAdaAman("koperasi", "pembelian")) {
                return new ArrayList<Map<String, Object>>();
            }
            String anggotaFilter = "";
            if (anggota != null && anggota.getId() != null) {
                anggotaFilter = " and p.anggota_koperasi = " + anggota.getId().longValue() + " ";
            }
            String sql = "select coalesce(p.pembelian_anggota_koperasi, p.id) as id_transaksi, "
                    + "coalesce(max(p.kode),'TRX-'||coalesce(p.pembelian_anggota_koperasi, p.id)) as kode_transaksi, "
                    + "to_char(max(coalesce(p.waktu,current_timestamp)),'DD-MM-YYYY HH24:MI') as waktu_trx, "
                    + "string_agg(coalesce(pr.nama,'Item') || ' (' || coalesce(p.qty,1) || ')', ', ') as namabarang, "
                    + "coalesce(sum(coalesce(p.total,0)),0) as total "
                    + "from koperasi.pembelian p "
                    + "left join koperasi.produk pr on pr.id=p.produk "
                    + "where 1=1 " + anggotaFilter
                    + " group by coalesce(p.pembelian_anggota_koperasi, p.id) "
                    + " order by max(coalesce(p.waktu,current_timestamp)) desc, coalesce(p.pembelian_anggota_koperasi, p.id) desc "
                    + " limit " + RIWAYAT_POS_LIMIT;
            return Common.ambilSqlMap(sql);
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
            return new ArrayList<Map<String, Object>>();
        }
    }


    private void renderPosHelpPanel(Vbox box) {
        box.appendChild(new ais.ui.util.MyHtml("<div style='background:#0f172a;border-radius:18px;padding:14px 16px;color:#ffffff;"
                + "box-shadow:0 10px 24px rgba(15,23,42,.16);'>"
                + "<div style='font-size:14px;font-weight:900;margin-bottom:6px;'>Alur Cepat Kasir</div>"
                + "<div style='font-size:11.5px;line-height:1.65;opacity:.9;'>"
                + "1) Pilih pelanggan. 2) Centang tagihan atau topup. 3) Cek total dan terbilang. "
                + "4) Pilih cara bayar. 5) Cetak atau buka kembali struk dari riwayat jika diperlukan.</div>"
                + "</div>"));
    }

    private void kosongkanKeranjang() {
        try {
            if (transaksiKoperasiDetails != null) {
                transaksiKoperasiDetails.clear();
            }
            if (produkKoperasiDipilih != null) {
                produkKoperasiDipilih.clear();
            }
            if (pilihan != null) {
                for (int i = 0; i < pilihan.size(); i++) {
                    MyCheckboxConfig cb = (MyCheckboxConfig) pilihan.get(i);
                    cb.setChecked(false);
                    cb.setDisabled(false);
                }
            }
            if (rowsProdukKoperasi != null) {
                reloadTransaksiKoperasiDetail();
                return;
            }
            if (deposit != null) {
                deposit.setValue(0.0);
                deposit.setDisabled(true);
            }
            hitungUlangTransaksiKoperasiDetail();
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
        }
    }

    private String buildMiniKpiHtml(String title, String value, String description, String background, String color) {
        return "<div style='background:" + background + ";border:1px solid rgba(15,23,42,.08);border-radius:16px;"
                + "padding:12px;min-height:94px;box-sizing:border-box;'>"
                + "<div style='font-size:10.5px;font-weight:900;text-transform:uppercase;letter-spacing:.05em;color:" + color + ";'>"
                + html(title) + "</div>"
                + "<div style='font-size:21px;font-weight:900;color:" + color + ";margin-top:8px;'>" + html(value) + "</div>"
                + "<div style='font-size:10.5px;line-height:1.4;color:" + color + ";opacity:.82;margin-top:4px;'>"
                + html(description) + "</div></div>";
    }

    private String badgeHtml(String text, String background, String color) {
        return "<span style='display:inline-block;padding:5px 9px;border-radius:999px;background:" + background
                + ";color:" + color + ";font-size:10.5px;font-weight:900;white-space:nowrap;'>" + html(text) + "</span>";
    }

    private String iconInitial(String value) {
        String v = safe(value);
        if (v.length() == 0 || v.equalsIgnoreCase("Belum dipilih")) {
            return "?";
        }
        return html(v.substring(0, 1).toUpperCase());
    }

    private String mapString(Map<String, Object> map, String key) {
        try {
            Object value = getMapValue(map, key);
            return value == null ? "" : String.valueOf(value);
        } catch (Exception e) {
            return "";
        }
    }

    private long mapLong(Map<String, Object> map, String key) {
        try {
            Object value = getMapValue(map, key);
            if (value instanceof Number) {
                return ((Number) value).longValue();
            }
            if (value != null && value.toString().trim().length() > 0) {
                return Long.parseLong(value.toString().trim());
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/koperasi/helper/PembayaranKoperasiOnline.java:2331");
        }
        return 0L;
    }

    private double mapDouble(Map<String, Object> map, String key) {
        try {
            Object value = getMapValue(map, key);
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            }
            if (value != null && value.toString().trim().length() > 0) {
                return Double.parseDouble(value.toString().trim());
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/koperasi/helper/PembayaranKoperasiOnline.java:2345");
        }
        return 0.0;
    }

    private Object getMapValue(Map<String, Object> map, String key) {
        if (map == null || key == null) {
            return null;
        }
        Object value = map.get(key);
        if (value == null) {
            value = map.get(key.toUpperCase());
        }
        if (value == null) {
            value = map.get(key.toLowerCase());
        }
        return value;
    }





    private void loadTokoKoperasiOptions() {
        if (tokoKoperasi == null || tokoKoperasi.getChildren().size() > 0) {
            return;
        }

        Comboitem kosong = new Comboitem();
        kosong.setLabel("Pilih Toko/Merchant");
        kosong.setValue(null);
        tokoKoperasi.appendChild(kosong);
        tokoKoperasi.setSelectedItem(kosong);

        Long tokoLoginId = getTokoLoginId();
        try {
            String sql = "select id, nama from koperasi.toko where coalesce(aktif,true)=true ";
            if (tokoLoginId != null) {
                sql += " and id = " + tokoLoginId.longValue() + " ";
            }
            sql += " order by nama asc limit 300";
            List<Map<String, Object>> list = Common.ambilSqlMap(sql);
            if (list != null) {
                for (int i = 0; i < list.size(); i++) {
                    Map<String, Object> row = (Map<String, Object>) list.get(i);
                    Long id = mapLongObject(row, "id");
                    String nama = mapString(row, "nama");
                    if (id == null || isEmpty(nama)) {
                        continue;
                    }
                    Comboitem item = new Comboitem();
                    item.setLabel(nama);
                    item.setValue(id);
                    tokoKoperasi.appendChild(item);
                    if (tokoLoginId != null && tokoLoginId.equals(id)) {
                        tokoKoperasi.setSelectedItem(item);
                        tokoKoperasi.setDisabled(true);
                    }
                }
            }
            if (tokoLoginId == null) {
                tokoKoperasi.setDisabled(false);
                tokoKoperasi.setTooltiptext("Pilih toko/merchant. Jika login bukan sebagai pedagang toko, semua toko aktif ditampilkan.");
            } else {
                tokoKoperasi.setTooltiptext("Toko otomatis mengikuti akun pedagang yang sedang login.");
            }
        } catch (Exception e) {
            Comboitem item = new Comboitem();
            item.setLabel("Toko belum tersedia");
            item.setValue(null);
            tokoKoperasi.appendChild(item);
            Common.tampilErrorJikaAdmin(e);
        }
    }





    private Long getTokoLoginId() {
        try {
            Toko tokoAktif = Common.getCurrentToko();
            if (tokoAktif != null && tokoAktif.getId() != null) {
                return tokoAktif.getId();
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/koperasi/helper/PembayaranKoperasiOnline.java:2430");
        }
        try {
            Tbmuser user = Common.getCurrentUser();
            if (user == null || user.getPedagang() == null || user.getPedagang().getToko() == null) {
                return null;
            }
            return user.getPedagang().getToko().getId();
        } catch (Exception e) {
            return null;
        }
    }


    private Long getSelectedTokoId() {
        try {
            if (tokoKoperasi == null || tokoKoperasi.getSelectedItem() == null
                    || tokoKoperasi.getSelectedItem().getValue() == null) {
                return null;
            }
            Object val = tokoKoperasi.getSelectedItem().getValue();
            if (val instanceof Number) {
                return Long.valueOf(((Number) val).longValue());
            }
            return Long.valueOf(String.valueOf(val));
        } catch (Exception e) {
            return null;
        }
    }

    private String getSelectedTokoNama() {
        try {
            return tokoKoperasi == null || tokoKoperasi.getSelectedItem() == null ? "Belum dipilih"
                    : safe(tokoKoperasi.getSelectedItem().getLabel());
        } catch (Exception e) {
            return "Belum dipilih";
        }
    }


    private void renderProductCatalog(Vbox parent) {
        Vbox panel = new Vbox();
        panel.setParent(parent);
        panel.setWidth("100%");
        panel.setStyle("background:#ffffff;border:1px solid #dbe4f0;border-radius:18px;padding:12px;"
                + "box-sizing:border-box;box-shadow:0 10px 24px rgba(15,23,42,.06);margin-bottom:12px;");
        panel.appendChild(new ais.ui.util.MyHtml("<div style='display:flex;justify-content:space-between;gap:10px;align-items:flex-start;flex-wrap:wrap;margin-bottom:10px;'>"
                + "<div><div style='font-size:15px;font-weight:900;color:#0f172a;'>Katalog Produk Toko/Merchant</div>"
                + "<div style='font-size:11.5px;color:#64748b;line-height:1.55;margin-top:3px;'>"
                + "Pilih toko/merchant terlebih dahulu agar produk yang tampil tidak tercampur antar toko. Setelah produk muncul, centang item yang dibeli, isi jumlah, dan subtotalnya otomatis masuk ke Ringkasan Checkout.</div>"
                + "<div style='font-size:10.5px;color:#64748b;margin-top:5px;'>Kolom pencarian produk berada di bar atas. Pada ZK 5.5 tidak digunakan placeholder, sehingga petunjuk pencarian ditampilkan sebagai label dan tooltip.</div></div>"
                + badgeHtml(getSelectedTokoId() == null ? "Toko belum dipilih" : getSelectedTokoNama(),
                        getSelectedTokoId() == null ? "#fff7ed" : "#ecfdf5", getSelectedTokoId() == null ? "#9a3412" : "#166534")
                + "</div>"));

        MyGrid gridProduk = new MyGrid();
        gridProduk.setParent(panel);
        gridProduk.setWidth("100%");
        gridProduk.setHeight("280px");
        gridProduk.setStyle("border:0;overflow:auto;");
        Columns columns = new Columns();
        columns.setParent(gridProduk);
        createColumn(columns, "Produk", null, null);
        createColumn(columns, "Harga", "120px", "right");
        createColumn(columns, "Qty", "90px", "right");
        createColumn(columns, "Subtotal", "130px", "right");
        rowsProdukKoperasi = new Rows();
        rowsProdukKoperasi.setParent(gridProduk);
        tampilkanProdukKoperasi();
    }


    private void tampilkanProdukKoperasi() {
        if (rowsProdukKoperasi == null) {
            return;
        }
        Common.clear(rowsProdukKoperasi);
        produkKoperasiItems.clear();
        Long tokoId = getSelectedTokoId();
        if (tokoId == null) {
            rowsProdukKoperasi.appendChild(emptyRow("Pilih toko/merchant terlebih dahulu agar katalog produk tampil."));
            return;
        }
        List<PosProductItem> list = ambilProdukKoperasi(tokoId);
        if (list.isEmpty()) {
            rowsProdukKoperasi.appendChild(emptyRow("Produk tidak ditemukan untuk toko/merchant ini."));
            return;
        }
        for (int i = 0; i < list.size(); i++) {
            renderProdukRow(list.get(i));
        }
    }


    private List<PosProductItem> ambilProdukKoperasi(Long tokoId) {
        List<PosProductItem> result = new ArrayList<PosProductItem>();
        if (tokoId == null) {
            return result;
        }
        try {
            String keyword = cariProdukKoperasi == null || cariProdukKoperasi.getValue() == null ? ""
                    : cariProdukKoperasi.getValue().trim();
            if (keyword.length() > 80) {
                keyword = keyword.substring(0, 80);
            }
            String keywordSql = keyword.replace("'", "''");
            String sql = "select id, kode, nama, coalesce(hargajual,0) as hargajual from koperasi.produk "
                    + "where coalesce(aktif,true)=true and toko=" + tokoId.longValue();
            if (keywordSql.length() > 0) {
                sql += " and (nama ilike '%" + keywordSql + "%' or kode ilike '%" + keywordSql + "%')";
            }
            sql += " order by nama asc limit 80";
            List<Map<String, Object>> list = Common.ambilSqlMap(sql);
            if (list != null) {
                for (int i = 0; i < list.size(); i++) {
                    Map<String, Object> row = (Map<String, Object>) list.get(i);
                    PosProductItem item = new PosProductItem();
                    item.id = mapLongObject(row, "id");
                    item.kode = mapString(row, "kode");
                    item.nama = mapString(row, "nama");
                    item.harga = mapDouble(row, "hargajual");
                    item.qty = 1.0;
                    item.tokoId = tokoId;
                    item.tokoNama = getSelectedTokoNama();
                    if (item.id != null) {
                        PosProductItem selected = produkKoperasiDipilih.get(item.id);
                        if (selected != null) {
                            item.qty = selected.qty;
                        }
                        result.add(item);
                        produkKoperasiItems.put(item.id, item);
                    }
                }
            }
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
        }
        return result;
    }


    private void renderProdukRow(final PosProductItem item) {
        if (item == null || item.id == null) {
            return;
        }
        Row row = new Row();
        row.setParent(rowsProdukKoperasi);
        row.setValign("middle");
        row.setStyle("background:#ffffff;border-bottom:1px solid #eef2f7;");

        final MyCheckboxConfig check = new MyCheckboxConfig(safe(item.nama));
        check.setChecked(produkKoperasiDipilih.containsKey(item.id));
        check.setAttribute("produk", item);
        Vbox info = new Vbox();
        info.setParent(row);
        info.setWidth("100%");
        info.appendChild(check);
        info.appendChild(new ais.ui.util.MyHtml("<div style='font-size:11px;color:#64748b;line-height:1.45;margin-top:3px;'>Kode: "
                + html(isEmpty(item.kode) ? "-" : item.kode) + " &nbsp; | &nbsp; Toko: " + html(item.tokoNama) + "</div>"));

        Label harga = new Label(formatMoney(item.harga));
        harga.setStyle("font-weight:900;color:#0f172a;text-align:right;");
        row.appendChild(harga);

        final MyDoublebox qty = new MyDoublebox(item.qty <= 0.0 ? 1.0 : item.qty);
        qty.setWidth("70px");
        qty.setDisabled(!check.isChecked());
        qty.setStyle("text-align:right;border-radius:10px;");
        row.appendChild(qty);

        final Label subtotal = new Label(formatMoney(check.isChecked() ? item.harga * qty.getValue() : 0.0));
        subtotal.setStyle("font-weight:900;color:#16a34a;text-align:right;");
        row.appendChild(subtotal);

        EventListener listener = new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                double jumlah = qty.getValue() == null ? 1.0 : qty.getValue().doubleValue();
                if (jumlah <= 0.0) {
                    jumlah = 1.0;
                    qty.setValue(jumlah);
                }
                item.qty = jumlah;
                qty.setDisabled(!check.isChecked());
                if (check.isChecked()) {
                    produkKoperasiDipilih.put(item.id, item);
                    subtotal.setValue(formatMoney(item.harga * item.qty));
                } else {
                    produkKoperasiDipilih.remove(item.id);
                    subtotal.setValue(formatMoney(0.0));
                }
                hitungUlangTransaksiKoperasiDetail();
            }
        };
        check.addEventListener("onClick", listener);
        qty.addEventListener("onChange", listener);
    }

    private int getJumlahProdukDipilih() {
        int total = 0;
        for (PosProductItem item : produkKoperasiDipilih.values()) {
            if (item != null) {
                total += (int) Math.round(item.qty <= 0.0 ? 1.0 : item.qty);
            }
        }
        return total;
    }

    private double getTotalProdukDipilih() {
        double total = 0.0;
        for (PosProductItem item : produkKoperasiDipilih.values()) {
            if (item != null) {
                double qty = item.qty <= 0.0 ? 1.0 : item.qty;
                total += item.harga * qty;
            }
        }
        return total;
    }

    private String buildRingkasanProdukDipilih() {
        if (produkKoperasiDipilih == null || produkKoperasiDipilih.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (PosProductItem item : produkKoperasiDipilih.values()) {
            if (item == null) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(item.nama).append(" x").append(Common.numberFormat.get().format(item.qty <= 0.0 ? 1.0 : item.qty))
                    .append(" @").append(formatMoney(item.harga));
        }
        return sb.toString();
    }




    private void simpanPembelianProdukPosNative(AnggotaKoperasi anggota, CaraPembayaranKoperasi cara,
            PembayaranAnggotaKoperasi pembayaran) {
        if (produkKoperasiDipilih == null || produkKoperasiDipilih.isEmpty()) {
            return;
        }
        try {
            Long tokoId = getSelectedTokoId();
            if (tokoId == null || !tableAdaAman("koperasi", "pembelian")) {
                return;
            }

            String kode = "POS-ZK-" + (pembayaran == null || pembayaran.getId() == null ? WaktuUtil.getDate().getTime()
                    : pembayaran.getId().longValue());
            Long anggotaId = anggota == null ? null : anggota.getId();
            Long caraId = cara == null ? null : cara.getId();
            Long siswaId = getSiswaAktif() == null ? null : getSiswaAktif().getId();
            Long mahasiswaId = getMahasiswaAktif() == null ? null : getMahasiswaAktif().getId();
            String tanggalSql = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(
                    tanggalTransaski == null || tanggalTransaski.getValue() == null ? WaktuUtil.getDate()
                            : tanggalTransaski.getValue());

            String ket = "Pembelian dari PembayaranKoperasiOnline";
            if (anggota == null) {
                ket += " (non anggota)";
            }
            if (getNamaPelanggan(anggota).trim().length() > 0) {
                ket += " - " + getNamaPelanggan(anggota);
            }
            ket += ": " + buildRingkasanProdukDipilih();

            /*
             * Jangan insert ke koperasi.pembelian_anggota_koperasi dengan kolom tetap.
             * Pada beberapa database POS, tabel tersebut tidak memiliki kolom tanggal.
             * Data item POS cukup disimpan ke koperasi.pembelian dengan kolom yang
             * benar-benar tersedia berdasarkan information_schema.
             */
            boolean adaKode = kolomAdaAman("koperasi", "pembelian", "kode");
            boolean adaProduk = kolomAdaAman("koperasi", "pembelian", "produk");
            boolean adaToko = kolomAdaAman("koperasi", "pembelian", "toko");
            boolean adaQty = kolomAdaAman("koperasi", "pembelian", "qty");
            boolean adaHargaSatuan = kolomAdaAman("koperasi", "pembelian", "hargasatuan");
            boolean adaHargaJual = kolomAdaAman("koperasi", "pembelian", "hargajual");
            boolean adaTotal = kolomAdaAman("koperasi", "pembelian", "total");
            boolean adaWaktu = kolomAdaAman("koperasi", "pembelian", "waktu");
            boolean adaTanggal = kolomAdaAman("koperasi", "pembelian", "tanggal");
            boolean adaAktif = kolomAdaAman("koperasi", "pembelian", "aktif");
            boolean adaKeterangan = kolomAdaAman("koperasi", "pembelian", "keterangan");
            boolean adaMember = kolomAdaAman("koperasi", "pembelian", "member");
            boolean adaJenisMember = kolomAdaAman("koperasi", "pembelian", "jenismember");
            boolean adaCaraBayar = kolomAdaAman("koperasi", "pembelian", "carabayar");
            boolean adaPembayaran = kolomAdaAman("koperasi", "pembelian", "pembayaran_anggota_koperasi");
            boolean adaPembelianAnggota = kolomAdaAman("koperasi", "pembelian", "pembelian_anggota_koperasi");
            boolean adaAnggota = kolomAdaAman("koperasi", "pembelian", "anggota_koperasi");
            boolean adaSiswa = kolomAdaAman("koperasi", "pembelian", "siswa");
            boolean adaMahasiswa = kolomAdaAman("koperasi", "pembelian", "mahasiswa");
            boolean adaCaraPembayaran = kolomAdaAman("koperasi", "pembelian", "cara_pembayaran_koperasi");
            boolean adaTerlayani = kolomAdaAman("koperasi", "pembelian", "terlayani");

            java.util.Set<Long> produkTerjualOnline = new java.util.HashSet<Long>();
            for (PosProductItem item : produkKoperasiDipilih.values()) {
                if (item == null || item.id == null) {
                    continue;
                }
                produkTerjualOnline.add(item.id);
                double qty = item.qty <= 0.0 ? 1.0 : item.qty;
                double subtotal = qty * item.harga;
                StringBuffer kolom = new StringBuffer();
                StringBuffer nilai = new StringBuffer();
                boolean[] first = new boolean[] { true };

                if (adaKode) appendInsertValue(kolom, nilai, first, "kode", quoteSql(kode));
                if (adaProduk) appendInsertValue(kolom, nilai, first, "produk", item.id.toString());
                if (adaToko) appendInsertValue(kolom, nilai, first, "toko", tokoId.toString());
                if (adaQty) appendInsertValue(kolom, nilai, first, "qty", doubleSql(qty));
                if (adaHargaSatuan) appendInsertValue(kolom, nilai, first, "hargasatuan", doubleSql(item.harga));
                if (adaHargaJual) appendInsertValue(kolom, nilai, first, "hargajual", doubleSql(subtotal));
                if (adaTotal) appendInsertValue(kolom, nilai, first, "total", doubleSql(subtotal));
                if (adaWaktu) appendInsertValue(kolom, nilai, first, "waktu", quoteSql(tanggalSql));
                if (adaTanggal) appendInsertValue(kolom, nilai, first, "tanggal", quoteSql(tanggalSql));
                if (adaAktif) appendInsertValue(kolom, nilai, first, "aktif", "true");
                if (adaKeterangan) appendInsertValue(kolom, nilai, first, "keterangan", quoteSql(ket));
                if (adaMember) appendInsertValue(kolom, nilai, first, "member", quoteSql(getNamaPelanggan(anggota)));
                if (adaJenisMember) appendInsertValue(kolom, nilai, first, "jenismember", quoteSql(getTipePelangganLabel()));
                if (adaCaraBayar) appendInsertValue(kolom, nilai, first, "carabayar", quoteSql(cara == null ? "Tunai" : safe(cara.getNama())));
                if (adaPembayaran && pembayaran != null && pembayaran.getId() != null) appendInsertValue(kolom, nilai, first, "pembayaran_anggota_koperasi", pembayaran.getId().toString());
                if (adaPembelianAnggota && pembayaran != null && pembayaran.getId() != null) appendInsertValue(kolom, nilai, first, "pembelian_anggota_koperasi", pembayaran.getId().toString());
                if (adaAnggota && anggotaId != null) appendInsertValue(kolom, nilai, first, "anggota_koperasi", anggotaId.toString());
                if (adaSiswa && siswaId != null) appendInsertValue(kolom, nilai, first, "siswa", siswaId.toString());
                if (adaMahasiswa && mahasiswaId != null) appendInsertValue(kolom, nilai, first, "mahasiswa", mahasiswaId.toString());
                if (adaCaraPembayaran && caraId != null) appendInsertValue(kolom, nilai, first, "cara_pembayaran_koperasi", caraId.toString());
                if (adaTerlayani) appendInsertValue(kolom, nilai, first, "terlayani", "true");

                if (kolom.length() == 0) {
                    continue;
                }
                String sqlDetail = "insert into koperasi.pembelian (" + kolom.toString() + ") values (" + nilai.toString() + ")";
                Common.updateSql(sqlDetail);
            }

            // Temuan 2c: segarkan stok kantin untuk tiap produk terjual via jalur online/VA
            // (UPDATE native via Common.updateSql, tanpa perlu sesi Hibernate) agar cache
            // produk.stok tidak basi setelah penjualan online. Reuse StokKantinUtil.
            for (Long pidOnline : produkTerjualOnline) {
                ais.action.master.inventory.StokKantinUtil.recomputeStokProdukViaSql(pidOnline);
            }
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
        }
    }




    private String quoteSql(String value) {
        if (value == null) {
            return "null";
        }
        return "'" + value.replace("'", "''") + "'";
    }

    private String doubleSql(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return "0";
        }
        return String.valueOf(value);
    }

    private Long mapLongObject(Map<String, Object> map, String key) {
        try {
            Object value = getMapValue(map, key);
            if (value == null) {
                return null;
            }
            if (value instanceof Number) {
                return Long.valueOf(((Number) value).longValue());
            }
            String s = value.toString().trim();
            return s.length() == 0 ? null : Long.valueOf(s);
        } catch (Exception e) {
            return null;
        }
    }

    private static class PosProductItem {
        Long id;
        Long tokoId;
        String tokoNama;
        String kode;
        String nama;
        double harga;
        double qty;
    }


    private void tampilPembayaran(AnggotaKoperasi anggota) {
        if (anggota == null) {
            return;
        }
        try {
            Session session = HibernateUtil.currentSession();
            List<TransaksiKoperasiDetail> details = ConstantValues.simpleList(session
                    .createCriteria(TransaksiKoperasiDetail.class).createAlias("transaksiKoperasi", "transaksiKoperasi")
                    .add(Restrictions.isNull("pembayaranAnggotaKoperasiDetail"))
                    .add(Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")))
                    .add(Restrictions.eq("transaksiKoperasi.anggotaKoperasi", anggota))
                    .add(Restrictions.isNotNull("transaksiKoperasi.produkKoperasi"))
                    .addOrder(Order.desc("transaksiKoperasi.id")).addOrder(Order.asc("ke"))
                    .setMaxResults(Common.MAX_RESULT_500), TransaksiKoperasiDetail.class);

            if (details.isEmpty()) {
                rowsDetailBiaya.appendChild(emptyRow("Tidak ada tagihan koperasi yang belum dibayar."));
            } else {
                renderPendingDetails(details);
            }
            renderTopupRow(anggota);
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
            rowsDetailBiaya.appendChild(emptyRow("Gagal memuat tagihan koperasi. " + e.getMessage()));
        }
    }

    private void renderPendingDetails(List<TransaksiKoperasiDetail> details) {
        TransaksiKoperasi lastTransaksi = null;
        for (int i = 0; i < details.size(); i++) {
            TransaksiKoperasiDetail detail = (TransaksiKoperasiDetail) details.get(i);
            if (detail == null || !detail.getAktif() || detail.getPembayaranAnggotaKoperasiDetail() != null) {
                continue;
            }
            TransaksiKoperasi transaksi = detail.getTransaksiKoperasi();
            if (transaksi != null && transaksi.getProdukKoperasi() != null
                    && (lastTransaksi == null || !lastTransaksi.getId().equals(transaksi.getId()))) {
                Group group = new Group(safe(transaksi.getKode()) + " - " + safe(transaksi.getProdukKoperasi().getNama()));
                group.setParent(rowsDetailBiaya);
                lastTransaksi = transaksi;
            }
            renderPendingRow(detail);
        }
    }


    private void renderPendingRow(final TransaksiKoperasiDetail detail) {
        Row row = new Row();
        row.setValign("top");
        row.setParent(rowsDetailBiaya);
        row.setAttribute("transaksiKoperasiDetail", detail);
        row.setStyle("background:#ffffff;border-bottom:1px solid #eef2f7;");

        final MyCheckboxConfig check = new MyCheckboxConfig("Angsuran ke-" + detail.getKe());
        check.setAttribute("transaksiKoperasiDetail", detail);
        row.setAttribute("pilih", check);
        pilihan.add(check);

        Vbox info = new Vbox();
        info.setWidth("100%");
        info.setParent(row);
        info.appendChild(check);
        info.appendChild(new ais.ui.util.MyHtml("<div style='font-size:11.5px;color:#475569;margin-top:3px;line-height:1.45;'>"
                + html(buildNamaTransaksi(detail)) + "</div>"
                + "<div style='display:flex;gap:6px;flex-wrap:wrap;margin-top:5px;'>"
                + badgeHtml("Belum dibayar", "#fff7ed", "#9a3412")
                + (isBelumExpired(detail) ? badgeHtml("Aktif", "#ecfdf5", "#166534") : badgeHtml("Expired", "#fee2e2", "#991b1b"))
                + "</div>" + buildPaymentLinkInfo(detail)));
        check.addEventListener("onClick", eventListenerData);

        if (isAutoSelected(detail)) {
            check.setChecked(true);
            transaksiKoperasiDetails.put(detail.getId(), detail);
        }

        row.appendChild(new Label(detail.getTanggal() == null ? "" : Common.dateFormat3.get().format(detail.getTanggal())));
        Label nominal = new Label(formatMoney(hitungNominal(detail)));
        nominal.setStyle("font-weight:900;color:#0f172a;");
        row.appendChild(nominal);
        RevisiHelper.createNewRevisi(TransaksiKoperasiDetail.class, detail,
                detail.getTanggal() == null ? "Revisi" : Common.dateFormat41.get().format(detail.getTanggal()))
                .setParent(row);
    }


    private String buildPaymentLinkInfo(TransaksiKoperasiDetail detail) {
        StringBuilder sb = new StringBuilder();
        if (!isEmpty(detail.getLink()) && isBelumExpired(detail)) {
            sb.append("<div style='font-size:10px;color:#b91c1c;font-weight:600;margin-top:2px;'>Link pembayaran aktif: ")
                    .append("<a style='font-size:10px;color:#1d4ed8;' onclick=\"popupCenter({url: '")
                    .append(html(detail.getLink())).append("', title: 'Pembayaran', w: 600, h: 600});\" href='#'>Buka Link</a>")
                    .append(detail.getExpired() == null ? "" : " sampai " + Common.dateFormat.get().format(detail.getExpired()))
                    .append("</div>");
        } else if (!isEmpty(detail.getVa()) && isBelumExpired(detail)) {
            sb.append("<div style='font-size:10px;color:#b91c1c;font-weight:600;margin-top:2px;'>VA aktif: ")
                    .append(html(detail.getVa()))
                    .append(detail.getExpired() == null ? "" : " sampai " + Common.dateFormat.get().format(detail.getExpired()))
                    .append("</div>");
        }
        return sb.toString();
    }


    private void renderTopupRow(AnggotaKoperasi anggota) {
        boolean tampilkanTopup = true;
        try {
            tampilkanTopup = Common.bolehKonfigurasi("tampilkan_tabungan_anggotaKoperasi");
        } catch (Exception e) {
            tampilkanTopup = true;
        }
        if (!tampilkanTopup) {
            return;
        }

        Group groupCheck = new ais.ui.util.MyGroupConfig();
        groupCheck.setParent(rowsDetailBiaya);
        groupCheck.appendChild(new Label("Pembayaran Tambahan / Topup"));

        Row row = new Row();
        row.setValign("middle");
        row.setParent(rowsDetailBiaya);
        row.setStyle("background:#f8fafc;border-top:1px solid #e2e8f0;");

        final MyCheckboxConfig check = new MyCheckboxConfig("Tambahkan pembayaran/topup manual");
        row.appendChild(check);
        row.setAttribute("pilih", check);
        row.appendChild(new Label("-"));
        deposit = new MyDoublebox(0.0);
        deposit.setWidth("95%");
        deposit.setDisabled(true);
        deposit.setStyle("font-weight:900;text-align:right;border-radius:10px;");
        row.appendChild(deposit);
        row.appendChild(new Label(""));

        check.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                deposit.setDisabled(!check.isChecked());
                if (!check.isChecked()) {
                    deposit.setValue(0.0);
                }
                hitungUlangTransaksiKoperasiDetail();
            }
        });
        deposit.addEventListener("onChange", new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                hitungUlangTransaksiKoperasiDetail();
            }
        });
    }


    private Row emptyRow(String message) {
        Row row = new Row();
        ais.ui.util.ZkCompat.setSpans(row, "4");
        row.setStyle("background:#f8fafc;color:#64748b;");
        row.appendChild(new Label(message == null ? "" : message));
        return row;
    }

    private void createColumn(Columns columns, String label, String width, String align) {
        MyColumnConfig column = new MyColumnConfig(label);
        column.setParent(columns);
        if (width != null) {
            column.setWidth(width);
        }
        if (align != null) {
            column.setAlign(align);
        }
    }

    private boolean isAutoSelected(TransaksiKoperasiDetail detail) {
        return detail != null && detail.getId() != null && transaksiKoperasiDetailsPilih != null
                && transaksiKoperasiDetailsPilih.contains(detail.getId());
    }

    private AnggotaKoperasi getAnggotaAktif() {
        try {
            AnggotaKoperasi anggota = anggotaKoperasi == null ? null
                    : (AnggotaKoperasi) anggotaKoperasi.getAttribute("anggotaKoperasi");
            if (anggota == null) {
                anggota = selectedAnggotaKoperasi;
            }
            return anggota;
        } catch (Exception e) {
            return selectedAnggotaKoperasi;
        }
    }

    private Siswa getSiswaAktif() {
        try {
            Siswa data = siswa == null ? null : (Siswa) siswa.getAttribute("siswa");
            if (data == null) {
                data = selectedSiswa;
            }
            return data;
        } catch (Exception e) {
            return selectedSiswa;
        }
    }

    private Mahasiswa getMahasiswaAktif() {
        try {
            Mahasiswa data = mahasiswa == null ? null : (Mahasiswa) mahasiswa.getAttribute("mahasiswa");
            if (data == null) {
                data = selectedMahasiswa;
            }
            return data;
        } catch (Exception e) {
            return selectedMahasiswa;
        }
    }

    private boolean adaPelangganAktif() {
        return getAnggotaAktif() != null || getSiswaAktif() != null || getMahasiswaAktif() != null;
    }

    private AnggotaKoperasi getAnggotaUntukTagihan() {
        AnggotaKoperasi anggota = getAnggotaAktif();
        if (anggota != null && anggota.getId() != null) {
            return anggota;
        }
        return cariAnggotaDariSiswaAtauMahasiswa(false);
    }

    private AnggotaKoperasi getAnggotaUntukPembayaran(boolean buatJikaBelumAda) throws Exception {
        AnggotaKoperasi anggota = getAnggotaAktif();
        if (anggota != null && anggota.getId() != null) {
            return anggota;
        }
        anggota = cariAnggotaDariSiswaAtauMahasiswa(buatJikaBelumAda);
        if (anggota != null) {
            selectedAnggotaKoperasi = anggota;
            setSelectedAnggotaToInput(anggota, true);
        }
        return anggota;
    }

    private AnggotaKoperasi cariAnggotaDariSiswaAtauMahasiswa(boolean buatJikaBelumAda) {
        Siswa siswaData = getSiswaAktif();
        Mahasiswa mahasiswaData = getMahasiswaAktif();
        if ((siswaData == null || siswaData.getId() == null) && (mahasiswaData == null || mahasiswaData.getId() == null)) {
            return null;
        }

        Session session = HibernateUtil.currentSession();
        try {
            if (siswaData != null && siswaData.getId() != null) {
                Siswa siswaDb = (Siswa) session.get(Siswa.class, siswaData.getId());
                AnggotaKoperasi anggota = (AnggotaKoperasi) ConstantValues.simpleObject(session
                        .createCriteria(AnggotaKoperasi.class).add(Restrictions.eq("siswa", siswaDb)).setMaxResults(1),
                        AnggotaKoperasi.class);
                if (anggota != null || !buatJikaBelumAda) {
                    return anggota;
                }
                return buatAnggotaDariSiswa(siswaDb);
            }
            if (mahasiswaData != null && mahasiswaData.getId() != null) {
                Mahasiswa mahasiswaDb = (Mahasiswa) session.get(Mahasiswa.class, mahasiswaData.getId());
                AnggotaKoperasi anggota = (AnggotaKoperasi) ConstantValues.simpleObject(session
                        .createCriteria(AnggotaKoperasi.class).add(Restrictions.eq("mahasiswa", mahasiswaDb))
                        .setMaxResults(1), AnggotaKoperasi.class);
                if (anggota != null || !buatJikaBelumAda) {
                    return anggota;
                }
                return buatAnggotaDariMahasiswa(mahasiswaDb);
            }
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
        }
        return null;
    }

    private AnggotaKoperasi buatAnggotaDariSiswa(Siswa siswaData) throws Exception {
        Session session = HibernateUtil.currentNativeSession();
        Siswa siswaDb = siswaData == null || siswaData.getId() == null ? null : (Siswa) session.get(Siswa.class, siswaData.getId());
        AnggotaKoperasi anggota = new AnggotaKoperasi();
        anggota.setSiswa(siswaDb);
        anggota.setNama(safe(siswaDb == null ? "" : siswaDb.getNama()));
        anggota.setKode(safe(siswaDb == null ? "" : siswaDb.getNomorInduk()));
        anggota.setKodeIdentitas(safe(siswaDb == null ? "" : siswaDb.getNomorInduk()));
        anggota.setAktif(true);
        anggota.setTanggal(WaktuUtil.getDate());
        try {
            session.getTransaction().begin();
            session.save(anggota);
            session.getTransaction().commit();
        } catch (Exception e) {
            try {
                session.getTransaction().rollback();
            } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/koperasi/helper/PembayaranKoperasiOnline.java:3115");
            }
            throw e;
        }
        return anggota;
    }

    private AnggotaKoperasi buatAnggotaDariMahasiswa(Mahasiswa mahasiswaData) throws Exception {
        Session session = HibernateUtil.currentNativeSession();
        Mahasiswa mahasiswaDb = mahasiswaData == null || mahasiswaData.getId() == null ? null
                : (Mahasiswa) session.get(Mahasiswa.class, mahasiswaData.getId());
        AnggotaKoperasi anggota = new AnggotaKoperasi();
        anggota.setMahasiswa(mahasiswaDb);
        anggota.setNama(safe(mahasiswaDb == null ? "" : mahasiswaDb.getNama()));
        anggota.setKode(safe(mahasiswaDb == null ? "" : mahasiswaDb.getNim()));
        anggota.setKodeIdentitas(safe(mahasiswaDb == null ? "" : mahasiswaDb.getNim()));
        anggota.setAktif(true);
        anggota.setTanggal(WaktuUtil.getDate());
        try {
            session.getTransaction().begin();
            session.save(anggota);
            session.getTransaction().commit();
        } catch (Exception e) {
            try {
                session.getTransaction().rollback();
            } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/koperasi/helper/PembayaranKoperasiOnline.java:3140");
            }
            throw e;
        }
        return anggota;
    }


    private String getTipePelangganLabel() {
        if (getAnggotaAktif() != null) {
            return "Anggota Koperasi";
        }
        if (getMahasiswaAktif() != null) {
            return "Mahasiswa";
        }
        if (getSiswaAktif() != null) {
            return "Siswa";
        }
        if (tampilMahasiswa && !tampilSiswa && !tampilAnggotaKoperasi) {
            return "Mahasiswa";
        }
        if (tampilSiswa && !tampilMahasiswa && !tampilAnggotaKoperasi) {
            return "Siswa";
        }
        if (tampilAnggotaKoperasi && !tampilSiswa && !tampilMahasiswa) {
            return "Anggota Koperasi";
        }
        return "Pilih Anggota / Siswa / Mahasiswa";
    }



    private String getNamaPelanggan(AnggotaKoperasi anggota) {
        if (anggota != null && anggota.getId() != null) {
            return safe(anggota.getNama());
        }
        Siswa siswaData = getSiswaAktif();
        if (siswaData != null) {
            return safe(siswaData.getNama());
        }
        Mahasiswa mahasiswaData = getMahasiswaAktif();
        if (mahasiswaData != null) {
            return safe(mahasiswaData.getNim()) + " - " + safe(mahasiswaData.getNama());
        }
        if (produkKoperasiDipilih != null && !produkKoperasiDipilih.isEmpty()) {
            return "Pembelian Non Anggota";
        }
        return "Belum dipilih";
    }


    private String getKodePelanggan(AnggotaKoperasi anggota) {
        if (anggota != null) {
            return safe(anggota.getKode());
        }
        Siswa siswaData = getSiswaAktif();
        if (siswaData != null) {
            return safe(siswaData.getNomorInduk());
        }
        Mahasiswa mahasiswaData = getMahasiswaAktif();
        if (mahasiswaData != null) {
            return safe(mahasiswaData.getNim());
        }
        return "-";
    }

    private boolean isOperator(Tbmuser tbmuser) {
        try {
            return tbmuser != null && tbmuser.getAnggotaKoperasi() == null && tbmuser.getOrangTua() == null;
        } catch (Exception e) {
            return false;
        }
    }

    private Long getTransaksiId(TransaksiKoperasiDetail detail) {
        try {
            return detail.getTransaksiKoperasi() == null ? null : detail.getTransaksiKoperasi().getId();
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isSameTransaksi(Long transaksiId, TransaksiKoperasiDetail detail) {
        Long id = getTransaksiId(detail);
        return transaksiId != null && id != null && transaksiId.equals(id);
    }

    private int intValue(Integer value) {
        return value == null ? 0 : value.intValue();
    }

    private double getDepositValue() {
        try {
            return deposit != null && !deposit.isDisabled() && deposit.getValue() != null ? deposit.getValue().doubleValue()
                    : 0.0;
        } catch (Exception e) {
            return 0.0;
        }
    }

    private double hitungNominal(TransaksiKoperasiDetail detail) {
        if (detail == null) {
            return 0.0;
        }
        return safeDouble(detail.getPokok()) + safeDouble(detail.getMargin());
    }

    private String buildNamaTransaksi(TransaksiKoperasiDetail detail) {
        try {
            return safe(detail.getTransaksiKoperasi().getProdukKoperasi().getNama()) + " (ke " + detail.getKe() + ")";
        } catch (Exception e) {
            return "Transaksi koperasi";
        }
    }

    private boolean isBelumExpired(TransaksiKoperasiDetail detail) {
        try {
            return detail == null || detail.getExpired() == null || detail.getExpired().after(WaktuUtil.getDate());
        } catch (Exception e) {
            return true;
        }
    }

    private double safeDouble(Double value) {
        return value == null ? 0.0 : value.doubleValue();
    }

    private String formatMoney(double value) {
        try {
            return Common.numberFormat.get().format(value);
        } catch (Exception e) {
            return String.valueOf(value);
        }
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean isEmpty(String value) {
        return value == null || value.trim().length() == 0;
    }

    private String html(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
