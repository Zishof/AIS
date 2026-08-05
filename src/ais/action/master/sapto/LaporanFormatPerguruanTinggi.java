package ais.action.master.sapto;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zss.ui.Spreadsheet;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Html;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Tab;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;

import ais.action.master.sapto.util.SaptoUtil;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.PerguruanTinggi;
import ais.ui.util.MyBorderlayout;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.ZkCompat;

/**
 * Dasbor utama Perguruan Tinggi — menampilkan profil institusi dan 28 laporan SAPTO
 * dalam tab yang di-load secara lazy (hanya saat tab diklik).
 */
public class LaporanFormatPerguruanTinggi extends MyWindow {

    public static final String sheetCode = "IDENTITAS_PT";
    private static final long serialVersionUID = 3331244819198611604L;
    private static final String MIME_XLSX =
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private Center center;

    public LaporanFormatPerguruanTinggi() {
        super();
        try { init(); } catch (Exception e) { Common.tampilErrorJikaAdmin(e); }
    }

    public LaporanFormatPerguruanTinggi(String title, String border, boolean closable) throws Exception {
        super(title, border, closable);
        init();
    }

    private void init() {
        Tabbox tabbox = new Tabbox();
        tabbox.setParent(Common.tampilanScrollTabbox(this));
        tabbox.setHeight("100%");
        tabbox.setWidth("100%");

        Tabs tabs = new Tabs();
        tabs.setParent(tabbox);

        Tab tabIdentitas  = new Tab("Identitas");  tabIdentitas.setParent(tabs);
        Tab tabA_2_4_6    = new Tab("A-2.4.6");    tabA_2_4_6.setParent(tabs);
        Tab tabA_3_1_5    = new Tab("A-3.1.5");    tabA_3_1_5.setParent(tabs);
        Tab tabA_3_1_8    = new Tab("A-3.1.8");    tabA_3_1_8.setParent(tabs);
        Tab tabA_3_1_11   = new Tab("A-3.1.11");   tabA_3_1_11.setParent(tabs);
        Tab tabA_3_2_1    = new Tab("A-3.2.1");    tabA_3_2_1.setParent(tabs);
        Tab tabA_3_2_2    = new Tab("A-3.2.2");    tabA_3_2_2.setParent(tabs);
        Tab tabA_3_2_4    = new Tab("A-3.2.4");    tabA_3_2_4.setParent(tabs);
        Tab tabA_4_3_1    = new Tab("A-4.3.1");    tabA_4_3_1.setParent(tabs);
        Tab tabA_4_3_2    = new Tab("A-4.3.2");    tabA_4_3_2.setParent(tabs);
        Tab tabA_4_4      = new Tab("A-4.4");       tabA_4_4.setParent(tabs);
        Tab tabA_4_5_1    = new Tab("A-4.5.1");    tabA_4_5_1.setParent(tabs);
        Tab tabA_6_1_4    = new Tab("A-6.1.4");    tabA_6_1_4.setParent(tabs);
        Tab tabA_6_1_5    = new Tab("A-6.1.5");    tabA_6_1_5.setParent(tabs);
        Tab tabA_6_1_6    = new Tab("A-6.1.6");    tabA_6_1_6.setParent(tabs);
        Tab tabA_6_1_7    = new Tab("A-6.1.7");    tabA_6_1_7.setParent(tabs);
        Tab tabA_6_2_2    = new Tab("A-6.2.2");    tabA_6_2_2.setParent(tabs);
        Tab tabA_6_2_3A   = new Tab("A-6.2.3A");   tabA_6_2_3A.setParent(tabs);
        Tab tabA_6_2_3B   = new Tab("A-6.2.3B");   tabA_6_2_3B.setParent(tabs);
        Tab tabA_6_2_4    = new Tab("A-6.2.4");    tabA_6_2_4.setParent(tabs);
        Tab tabA_6_2_5    = new Tab("A-6.2.5");    tabA_6_2_5.setParent(tabs);
        Tab tabA_6_2_7    = new Tab("A-6.2.7");    tabA_6_2_7.setParent(tabs);
        Tab tabA_7_1_2    = new Tab("A-7.1.2");    tabA_7_1_2.setParent(tabs);
        Tab tabA_7_1_3    = new Tab("A-7.1.3");    tabA_7_1_3.setParent(tabs);
        Tab tabA_7_1_4    = new Tab("A-7.1.4");    tabA_7_1_4.setParent(tabs);
        Tab tabA_7_1_5    = new Tab("A-7.1.5");    tabA_7_1_5.setParent(tabs);
        Tab tabA_7_2_2    = new Tab("A-7.2.2");    tabA_7_2_2.setParent(tabs);
        Tab tabA_7_3_2    = new Tab("A-7.3.2");    tabA_7_3_2.setParent(tabs);
        Tab tabA_7_3_3    = new Tab("A-7.3.3");    tabA_7_3_3.setParent(tabs);

        Tabpanels tabpanels = new Tabpanels();
        tabpanels.setParent(tabbox);

        // ── Identitas (eager load) ───────────────────────────────────────
        Tabpanel tabpanelIdentitas = new ais.ui.util.MyTabpanel();
        tabpanelIdentitas.setParent(tabpanels);
        buildIdentitasTab(tabpanelIdentitas);

        // ── Lazy-load helper for all remaining tabs ───────────────────────
        addLazy(tabA_2_4_6,  tabpanels, new LazyFactory() { public MyWindow create() throws Exception { return new LaporanStatusAkreditasiProdi_A_2_4_6(); }});
        addLazy(tabA_3_1_5,  tabpanels, new LazyFactory() { public MyWindow create() throws Exception { return new LaporanProfileMahasiswa_A_3_1_5(); }});
        addLazy(tabA_3_1_8,  tabpanels, new LazyFactory() { public MyWindow create() throws Exception { return new LaporanLayananKepadaMahasiswa_A_3_1_8(); }});
        addLazy(tabA_3_1_11, tabpanels, new LazyFactory() { public MyWindow create() throws Exception { return new LaporanPrestasiMahasiswa_A_3_1_11(); }});
        addLazy(tabA_3_2_1,  tabpanels, new LazyFactory() { public MyWindow create() throws Exception { return new LaporanProfileMahasiswaDanLulusan_A_3_2_1(); }});
        addLazy(tabA_3_2_2,  tabpanels, new LazyFactory() { public MyWindow create() throws Exception { return new LaporanProfileMahasiswaDanLulusan_A_3_2_2(); }});
        addLazy(tabA_3_2_4,  tabpanels, new LazyFactory() { public MyWindow create() throws Exception { return new LaporanProfileMahasiswaDanLulusan_A_3_2_4(); }});
        addLazy(tabA_4_3_1,  tabpanels, new LazyFactory() { public MyWindow create() throws Exception { return new LaporanDosenInstitusi_A_4_3_1(); }});
        addLazy(tabA_4_3_2,  tabpanels, new LazyFactory() { public MyWindow create() throws Exception { return new LaporanDosenInstitusi_A_4_3_2(); }});
        addLazy(tabA_4_4,    tabpanels, new LazyFactory() { public MyWindow create() throws Exception { return new LaporanKegiatanDosen_A_4_4(); }});
        addLazy(tabA_4_5_1,  tabpanels, new LazyFactory() { public MyWindow create() throws Exception { return new LaporanDosenInstitusi_A_4_5_1(); }});
        addLazy(tabA_6_1_4,  tabpanels, new LazyFactory() { public MyWindow create() throws Exception { return new LaporanDanaInstitusi_A_6_1_4(); }});
        addLazy(tabA_6_1_5,  tabpanels, new LazyFactory() { public MyWindow create() throws Exception { return new LaporanDanaInstitusi_A_6_1_5(); }});
        addLazy(tabA_6_1_6,  tabpanels, new LazyFactory() { public MyWindow create() throws Exception { return new LaporanDanaInstitusi_A_6_1_6(); }});
        addLazy(tabA_6_1_7,  tabpanels, new LazyFactory() { public MyWindow create() throws Exception { return new LaporanDanaInstitusi_A_6_1_7(); }});
        addLazy(tabA_6_2_2,  tabpanels, new LazyFactory() { public MyWindow create() throws Exception { return new LaporanLahanInstitusi_A_6_2_2(); }});
        addLazy(tabA_6_2_3A, tabpanels, new LazyFactory() { public MyWindow create() throws Exception { return new LaporanPrasaranaInstitusi_A_6_2_3A(); }});
        addLazy(tabA_6_2_3B, tabpanels, new LazyFactory() { public MyWindow create() throws Exception { return new LaporanPrasaranaInstitusi_A_6_2_3B(); }});
        addLazy(tabA_6_2_4,  tabpanels, new LazyFactory() { public MyWindow create() throws Exception { return new LaporanPrasaranaInstitusi_A_6_2_4(); }});
        addLazy(tabA_6_2_5,  tabpanels, new LazyFactory() { public MyWindow create() throws Exception { return new LaporanPustaka_A_6_2_5(); }});
        addLazy(tabA_6_2_7,  tabpanels, new LazyFactory() { public MyWindow create() throws Exception { return new LaporanAksesbilitas_A_6_2_7(); }});
        addLazy(tabA_7_1_2,  tabpanels, new LazyFactory() { public MyWindow create() throws Exception { return new LaporanPenelitianDosen_A_7_1_2(); }});
        addLazy(tabA_7_1_3,  tabpanels, new LazyFactory() { public MyWindow create() throws Exception { return new LaporanPenelitianDosen_A_7_1_3(); }});
        addLazy(tabA_7_1_4,  tabpanels, new LazyFactory() { public MyWindow create() throws Exception { return new LaporanPenelitianDosen_A_7_1_4(); }});
        addLazy(tabA_7_1_5,  tabpanels, new LazyFactory() { public MyWindow create() throws Exception { return new LaporanKaryaDosen_A_7_1_5(); }});
        addLazy(tabA_7_2_2,  tabpanels, new LazyFactory() { public MyWindow create() throws Exception { return new LaporanPengabdianDosen_A_7_2_2(); }});
        addLazy(tabA_7_3_2,  tabpanels, new LazyFactory() { public MyWindow create() throws Exception { return new LaporanKerjasamaDalamNegeri_A_7_3_2(); }});
        addLazy(tabA_7_3_3,  tabpanels, new LazyFactory() { public MyWindow create() throws Exception { return new LaporanKerjasamaLuarNegeri_A_7_3_3(); }});
    }

    private interface LazyFactory { MyWindow create() throws Exception; }

    private void addLazy(final Tab tab, Tabpanels panels, final LazyFactory factory) {
        final Tabpanel panel = new ais.ui.util.MyTabpanel();
        panel.setParent(panels);
        tab.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event e) throws Exception {
                if (panel.getChildren().isEmpty()) {
                    panel.appendChild(factory.create());
                }
            }
        });
    }

    private void buildIdentitasTab(Tabpanel panel) {
        Borderlayout bl = new MyBorderlayout();
        bl.setParent(panel);

        North north = new North();
        north.setCollapsible(true);
        north.setParent(bl);
        ZkCompat.setFlex(north, true);

        MyGrid toolGrid = new MyGrid();
        toolGrid.setWidth("100%");
        toolGrid.setParent(north);

        center = new Center();
        center.setParent(bl);
        ZkCompat.setFlex(center, true);

        Rows rows = new Rows();
        rows.setParent(toolGrid);
        MyFormRow row = new MyFormRow();
        row.setValign("top");
        row.setParent(rows);

        MyToolbarbuttonConfig btnSearch = new MyToolbarbuttonConfig("Tampilkan Data", "/img/svg/search.svg");
        btnSearch.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event e) throws Exception { onCetak(null); }
        });
        btnSearch.setParent(row);

        MyToolbarbuttonConfig btnDl = new MyToolbarbuttonConfig("Download Excel", "/img/excel.png");
        btnDl.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                try {
                    Spreadsheet excelku = (Spreadsheet) center.getAttribute("excelku");
                    if (excelku == null) { Common.showInfo("Tampilkan data terlebih dahulu."); return; }
                    ByteArrayOutputStream bout = new ByteArrayOutputStream();
                    excelku.getBook().write(bout);
                    bout.close();
                    Filedownload.save(bout.toByteArray(), MIME_XLSX, sheetCode + ".xlsx");
                } catch (Exception e) { Common.tampilErrorJikaAdmin(e); }
            }
        });
        btnDl.setParent(row);

        onCetak(null);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public void onCetak(Event event) {
        Common.clear(center);
        try {
            final Label label = new Label(ais.common.Common.getBahasaConfig("Proses load data .."));

            new Thread(new Runnable() {
                @Override
                public void run() {
                	try {
                    Session session = HibernateUtil.currentNativeSession();
                    PerguruanTinggi pt = (PerguruanTinggi) session.createCriteria(PerguruanTinggi.class)
                        .setMaxResults(1)
                        .add(Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")))
                        .uniqueResult();

                    List<List> datas = new ArrayList<List>();
                    // 4 header rows
                    for (int i = 0; i < 4; i++) datas.add(new ArrayList());

                    // Row 4: Nama PT (col 3) + Kode (col 6)
                    List sub = new ArrayList();
                    sub.add(""); sub.add(""); sub.add("");
                    sub.add(pt == null ? "" : pt.getNama());
                    sub.add(""); sub.add("");
                    sub.add(pt == null ? "" : pt.getKodePerguruanTinggi());
                    datas.add(sub);

                    // Row 5: empty (separator)
                    sub = new ArrayList(); sub.add(""); sub.add(""); sub.add(""); sub.add(""); datas.add(sub);
                    datas.add(new ArrayList());

                    // Row 7: SK Izin Operasi
                    sub = new ArrayList(); sub.add(""); sub.add(""); sub.add("");
                    sub.add(pt == null ? "" : pt.getSkIzinOperasi()); datas.add(sub);

                    // Row 8: Tanggal SK Izin Operasi
                    sub = new ArrayList(); sub.add(""); sub.add(""); sub.add("");
                    sub.add(pt == null ? "" : pt.getTglSkIzinOperasi()); datas.add(sub);

                    // Row 9: Pejabat Izin Operasi
                    sub = new ArrayList(); sub.add(""); sub.add(""); sub.add("");
                    sub.add(pt == null ? "" : pt.getPejabatIzinOperasi()); datas.add(sub);

                    datas.add(new ArrayList());

                    // Row 11: Tahun Pertama Menerima Mahasiswa
                    sub = new ArrayList(); sub.add(""); sub.add(""); sub.add("");
                    sub.add(pt == null ? "" : pt.getTahunPertamaMenerimaMahasiswa()); datas.add(sub);

                    datas.add(new ArrayList());
                    datas.add(new ArrayList());

                    // Row 14: Peringkat Akreditasi
                    sub = new ArrayList(); sub.add(""); sub.add(""); sub.add("");
                    sub.add(pt == null ? "" : pt.getPeringkatAkreditasi()); datas.add(sub);

                    // Row 15: Status/Nilai Akreditasi
                    sub = new ArrayList(); sub.add(""); sub.add(""); sub.add("");
                    sub.add(pt == null ? "" : pt.getAkreditasi()); datas.add(sub);

                    // Row 16: No SK Akreditasi
                    sub = new ArrayList(); sub.add(""); sub.add(""); sub.add("");
                    sub.add(pt == null ? "" : pt.getNoSkAkreditasi()); datas.add(sub);

                    datas.add(new ArrayList());

                    // Row 18: Alamat 1
                    sub = new ArrayList(); sub.add(""); sub.add(""); sub.add("");
                    sub.add(pt == null ? "" : pt.getAlamat1()); datas.add(sub);

                    // Row 19: Alamat 2
                    sub = new ArrayList(); sub.add(""); sub.add(""); sub.add("");
                    sub.add(pt == null ? "" : pt.getAlamat2()); datas.add(sub);

                    datas.add(new ArrayList());
                    datas.add(new ArrayList());
                    datas.add(new ArrayList());

                    // Row 23: Telepon
                    sub = new ArrayList(); sub.add(""); sub.add(""); sub.add("");
                    sub.add(pt == null ? "" : pt.getTelepon()); datas.add(sub);

                    datas.add(new ArrayList());

                    // Row 25: Faksimili
                    sub = new ArrayList(); sub.add(""); sub.add(""); sub.add("");
                    sub.add(pt == null ? "" : pt.getFaksimili()); datas.add(sub);

                    datas.add(new ArrayList());

                    // Row 27: Website
                    sub = new ArrayList(); sub.add(""); sub.add(""); sub.add("");
                    sub.add(pt == null ? "" : pt.getWebsite()); datas.add(sub);

                    datas.add(new ArrayList());

                    // Row 29: Email
                    sub = new ArrayList(); sub.add(""); sub.add(""); sub.add("");
                    sub.add(pt == null ? "" : pt.getEmail()); datas.add(sub);

                    datas.add(new ArrayList());

                    HibernateUtil.closeSession();
                    label.setAttribute("datas", datas);
                    label.setAttribute("ptObj", pt);
                    label.setValue("");
                                	} finally {
                		ais.database.hibernate.HibernateUtil.closeSession();
                	}
                }
            }).start();

            SaptoUtil.displayWorksheet(label, sheetCode, center, 8);

        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
        }
    }
}
