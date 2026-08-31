package ais.action.master.sapto;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zss.ui.Spreadsheet;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Filedownload;
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
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.JenjangProgramStudi;
import ais.database.model.Jurusan;
import ais.ui.util.MyBorderlayout;
import ais.ui.util.MyGrid;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.ZkCompat;

/**
 * Dasbor laporan per Program Studi S1 — menampilkan identitas prodi dan 28 laporan SAPTO.
 * Tab di-load secara lazy saat pertama kali diklik.
 */
public class LaporanFormatSarjana extends MyWindow {

    public static final String sheetCode = "IDENTITAS_SARJANA";
    private static final long serialVersionUID = 3331244819198611604L;
    private static final String MIME_XLSX =
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private Center center;
    private Combobox fakultas = new Combobox();
    private Combobox jurusan  = new Combobox();

    public LaporanFormatSarjana() {
        super();
        try { init(); } catch (Exception e) { Common.tampilErrorJikaAdmin(e); }
    }

    public LaporanFormatSarjana(String title, String border, boolean closable) throws Exception {
        super(title, border, closable);
        init();
    }

    private void init() {
        Common.initFakultasDanJurusanDanSemua(fakultas, jurusan);

        Tabbox tabbox = new Tabbox();
        tabbox.setParent(Common.tampilanScrollTabbox(this));
        tabbox.setHeight("100%");
        tabbox.setWidth("100%");

        Tabs tabs = new Tabs();
        tabs.setParent(tabbox);

        MyTabConfig tabIdentitas   = new MyTabConfig("Identitas");    tabIdentitas.setParent(tabs);
        MyTabConfig tabDosen       = new MyTabConfig("Dosen");         tabDosen.setParent(tabs);
        MyTabConfig tabA_3_1_1     = new MyTabConfig("A-3.1.1");      tabA_3_1_1.setParent(tabs);
        MyTabConfig tabA_3_1_2     = new MyTabConfig("A-3.1.2");      tabA_3_1_2.setParent(tabs);
        MyTabConfig tabA_3_1_4     = new MyTabConfig("A-3.1.4");      tabA_3_1_4.setParent(tabs);
        MyTabConfig tabA_4_3_1     = new MyTabConfig("A-4.3.1");      tabA_4_3_1.setParent(tabs);
        MyTabConfig tabA_4_3_2     = new MyTabConfig("A-4.3.2");      tabA_4_3_2.setParent(tabs);
        MyTabConfig tabA_4_3_3     = new MyTabConfig("A-4.3.3");      tabA_4_3_3.setParent(tabs);
        MyTabConfig tabA_4_3_4     = new MyTabConfig("A-4.3.4");      tabA_4_3_4.setParent(tabs);
        MyTabConfig tabA_4_3_5     = new MyTabConfig("A-4.3.5");      tabA_4_3_5.setParent(tabs);
        MyTabConfig tabA_4_4_1     = new MyTabConfig("A-4.4.1");      tabA_4_4_1.setParent(tabs);
        MyTabConfig tabA_4_4_2     = new MyTabConfig("A-4.4.2");      tabA_4_4_2.setParent(tabs);
        MyTabConfig tabA_4_5_1     = new MyTabConfig("A-4.5.1");      tabA_4_5_1.setParent(tabs);
        MyTabConfig tabA_4_5_2     = new MyTabConfig("A-4.5.2");      tabA_4_5_2.setParent(tabs);
        MyTabConfig tabA_4_5_3     = new MyTabConfig("A-4.5.3");      tabA_4_5_3.setParent(tabs);
        MyTabConfig tabA_4_5_4     = new MyTabConfig("A-4.5.4");      tabA_4_5_4.setParent(tabs);
        MyTabConfig tabA_4_5_5     = new MyTabConfig("A-4.5.5");      tabA_4_5_5.setParent(tabs);
        MyTabConfig tabA_4_6_1     = new MyTabConfig("A-4.6.1");      tabA_4_6_1.setParent(tabs);
        MyTabConfig tabA_5_1_2_1   = new MyTabConfig("A-5.1.2.1");   tabA_5_1_2_1.setParent(tabs);
        MyTabConfig tabA_5_1_2_2   = new MyTabConfig("A-5.1.2.2");   tabA_5_1_2_2.setParent(tabs);
        MyTabConfig tabA_5_1_3     = new MyTabConfig("A-5.1.3");      tabA_5_1_3.setParent(tabs);
        MyTabConfig tabA_5_4_1     = new MyTabConfig("A-5.4.1");      tabA_5_4_1.setParent(tabs);
        MyTabConfig tabA_5_5_1     = new MyTabConfig("A-5.5.1");      tabA_5_5_1.setParent(tabs);
        MyTabConfig tabA_5_5_2     = new MyTabConfig("A-5.5.2");      tabA_5_5_2.setParent(tabs);
        MyTabConfig tabA_6_2_1_1   = new MyTabConfig("A-6.2.1.1");   tabA_6_2_1_1.setParent(tabs);
        MyTabConfig tabA_6_2_1_2   = new MyTabConfig("A-6.2.1.2");   tabA_6_2_1_2.setParent(tabs);
        MyTabConfig tabA_6_2_2     = new MyTabConfig("A-6.2.2");      tabA_6_2_2.setParent(tabs);
        MyTabConfig tabA_6_2_3     = new MyTabConfig("A-6.2.3");      tabA_6_2_3.setParent(tabs);
        MyTabConfig tabA_6_3_1     = new MyTabConfig("A-6.3.1");      tabA_6_3_1.setParent(tabs);
        MyTabConfig tabA_6_4_1_1   = new MyTabConfig("A-6.4.1.1");   tabA_6_4_1_1.setParent(tabs);

        Tabpanels tabpanels = new Tabpanels();
        tabpanels.setParent(tabbox);

        // ── Identitas (eager, jurusan-aware) ─────────────────────────────
        Tabpanel tabpanelIdentitas = new ais.ui.util.MyTabpanel();
        tabpanelIdentitas.setParent(tabpanels);
        buildIdentitasTab(tabpanelIdentitas);

        // ── Lazy tabs ────────────────────────────────────────────────────
        addLazy(tabDosen,     tabpanels, new LazyFactory() { public MyWindow create() throws Exception { return new LaporanProfileDosen(); }});
        addLazy(tabA_3_1_1,   tabpanels, new LazyFactory() { public MyWindow create() throws Exception { return new LaporanProfileMahasiswaDanLulusan_A_3_1_1(); }});
        addLazy(tabA_3_1_2,   tabpanels, new LazyFactory() { public MyWindow create() throws Exception { return new LaporanProfileMahasiswaDanLulusan_A_3_1_2(); }});
        addLazy(tabA_3_1_4,   tabpanels, new LazyFactory() { public MyWindow create() throws Exception { return new LaporanProfileMahasiswaDanLulusan_A_3_1_4(); }});
        addLazy(tabA_4_3_1,   tabpanels, new LazyFactory() { public MyWindow create() throws Exception { return new LaporanProfileDosen_A_4_3_1(); }});
        addLazy(tabA_4_3_2,   tabpanels, new LazyFactory() { public MyWindow create() throws Exception { return new LaporanProfileDosen_A_4_3_2(); }});
        addLazy(tabA_4_3_3,   tabpanels, new LazyFactory() { public MyWindow create() throws Exception { return new LaporanProfileDosen_A_4_3_3(); }});
        addLazy(tabA_4_3_4,   tabpanels, new LazyFactory() { public MyWindow create() throws Exception { return new LaporanProfileDosen_A_4_3_4(); }});
        addLazy(tabA_4_3_5,   tabpanels, new LazyFactory() { public MyWindow create() throws Exception { return new LaporanProfileDosen_A_4_3_5(); }});
        addLazy(tabA_4_4_1,   tabpanels, new LazyFactory() { public MyWindow create() throws Exception { return new LaporanProfileDosen_A_4_4_1(); }});
        addLazy(tabA_4_4_2,   tabpanels, new LazyFactory() { public MyWindow create() throws Exception { return new LaporanProfileDosen_A_4_4_2(); }});
        addLazy(tabA_4_5_1,   tabpanels, new LazyFactory() { public MyWindow create() throws Exception { return new LaporanProfileDosen_A_4_5_1(); }});
        addLazy(tabA_4_5_2,   tabpanels, new LazyFactory() { public MyWindow create() throws Exception { return new LaporanProfileDosen_A_4_5_2(); }});
        addLazy(tabA_4_5_3,   tabpanels, new LazyFactory() { public MyWindow create() throws Exception { return new LaporanProfileDosen_A_4_5_3(); }});
        addLazy(tabA_4_5_4,   tabpanels, new LazyFactory() { public MyWindow create() throws Exception { return new LaporanProfileDosen_A_4_5_4(); }});
        addLazy(tabA_4_5_5,   tabpanels, new LazyFactory() { public MyWindow create() throws Exception { return new LaporanProfileDosen_A_4_5_5(); }});
        addLazy(tabA_4_6_1,   tabpanels, new LazyFactory() { public MyWindow create() throws Exception { return new LaporanProfileDosen_A_4_6_1(); }});
        addLazy(tabA_5_1_2_1, tabpanels, new LazyFactory() { public MyWindow create() throws Exception { return new LaporanKurikulumDanMatakuliah_A_5_1_2_1(); }});
        addLazy(tabA_5_1_2_2, tabpanels, new LazyFactory() { public MyWindow create() throws Exception { return new LaporanKurikulumDanMatakuliah_A_5_1_2_2(); }});
        addLazy(tabA_5_1_3,   tabpanels, new LazyFactory() { public MyWindow create() throws Exception { return new LaporanKurikulumDanMatakuliah_A_5_1_3(); }});
        addLazy(tabA_5_4_1,   tabpanels, new LazyFactory() { public MyWindow create() throws Exception { return new LaporanDosenPembimbing_A_5_4_1(); }});
        addLazy(tabA_5_5_1,   tabpanels, new LazyFactory() { public MyWindow create() throws Exception { return new LaporanDosenPembimbingTugasAkhir_A_5_5_1(); }});
        addLazy(tabA_5_5_2,   tabpanels, new LazyFactory() { public MyWindow create() throws Exception { return new LaporanDosenPembimbingTugasAkhir_A_5_5_2(); }});
        addLazy(tabA_6_2_1_1, tabpanels, new LazyFactory() { public MyWindow create() throws Exception { return new LaporanDana_A_6_2_1_1(); }});
        addLazy(tabA_6_2_1_2, tabpanels, new LazyFactory() { public MyWindow create() throws Exception { return new LaporanDana_A_6_2_1_2(); }});
        addLazy(tabA_6_2_2,   tabpanels, new LazyFactory() { public MyWindow create() throws Exception { return new LaporanDana_A_6_2_2(); }});
        addLazy(tabA_6_2_3,   tabpanels, new LazyFactory() { public MyWindow create() throws Exception { return new LaporanDana_A_6_2_3(); }});
        addLazy(tabA_6_3_1,   tabpanels, new LazyFactory() { public MyWindow create() throws Exception { return new LaporanRuangDosen_A_6_3_1(); }});
        addLazy(tabA_6_4_1_1, tabpanels, new LazyFactory() { public MyWindow create() throws Exception { return new LaporanPustaka_A_6_4_1_1(); }});
    }

    /**
     * Kontrak callback/strategi bersarang milik {@link LaporanFormatSarjana}. Tipe ini memisahkan satu variasi
     * perilaku lokal tanpa membuat service atau interface global yang tumpang tindih.
     *
     * <p><b>Scope:</b> setiap instance terikat pada instance {@link LaporanFormatSarjana} dan dapat mengakses
     * state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p> Tipe ini merupakan detail
     * implementasi privat; pemanggil luar harus memakai API kelas induk.
     * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code create}(). Aturan bisnis bersama
     * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
     * <p><b>Efek samping:</b> operasi dapat mengubah state lokal dan, sesuai nama methodnya, komponen UI atau
     * persistence melalui konteks kelas induk. Gunakan transaksi, otorisasi, dan session milik alur induk;
     * tambahkan perilaku lintas domain pada service bersama.</p>
     *
     * @see LaporanFormatSarjana
     */
    private interface LazyFactory { MyWindow create() throws Exception; }

    private void addLazy(final MyTabConfig tab, Tabpanels panels, final LazyFactory factory) {
        final Tabpanel panel = new ais.ui.util.MyTabpanel();
        panel.setParent(panels);
        tab.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event e) throws Exception {
                if (panel.getChildren().isEmpty()) panel.appendChild(factory.create());
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

        row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
        fakultas.setWidth("90%");
        row.appendChild(fakultas);
        fakultas.addEventListener("onChange", new EventListener() {
            @Override
            public void onEvent(Event e) throws Exception { onCetak(null); }
        });

        row.appendChild(new ais.ui.util.MyLabelConfig("Program Studi"));
        jurusan.setWidth("90%");
        row.appendChild(jurusan);
        jurusan.addEventListener("onChange", new EventListener() {
            @Override
            public void onEvent(Event e) throws Exception { onCetak(null); }
        });

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
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public void onCetak(Event event) {
        Common.clear(center);
        try {
            final Jurusan selectedJurusan = (Jurusan) (this.jurusan.getSelectedItem() == null
                || this.jurusan.getSelectedItem().getValue() == null
                ? null : this.jurusan.getSelectedItem().getValue());

            final Label label = new Label(ais.common.Common.getBahasaConfig("Proses load data .."));

            if (selectedJurusan != null) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                    	try {
                        Session session = HibernateUtil.currentNativeSession();
                        Jurusan j = selectedJurusan;

                        JenjangProgramStudi jps = j.getJenjangProgramStudi();
                        if (jps == null) {
                            jps = (JenjangProgramStudi) session.createCriteria(JenjangProgramStudi.class)
                                .setMaxResults(1).addOrder(Order.desc("id"))
                                .add(Restrictions.eq("jurusan", j)).uniqueResult();
                            if (jps == null) {
                                jps = new JenjangProgramStudi();
                                jps.setJurusan(j);
                                jps.setJenjang(j.getJenjang());
                                session.getTransaction().begin();
                                session.save(jps);
                                session.getTransaction().commit();
                            }
                            session.refresh(j);
                            j.setJenjangProgramStudi(jps);
                            session.getTransaction().begin();
                            Common.refreshUpdate(session, j);
                            session.getTransaction().commit();
                        }

                        final JenjangProgramStudi jenjang = jps;
                        List<List> datas = new ArrayList<List>();

                        // 4 header rows
                        for (int i = 0; i < 4; i++) datas.add(new ArrayList());

                        // Row 4: Nama Prodi + Kode
                        List sub = new ArrayList();
                        sub.add(""); sub.add(""); sub.add("");
                        sub.add(j.getNama()); sub.add(""); sub.add("");
                        sub.add(j.getKodeEpsbed());
                        datas.add(sub);

                        // Row 5: Departemen
                        sub = new ArrayList(); sub.add(""); sub.add(""); sub.add("");
                        sub.add(j.getGrupJurusan() == null ? "" : j.getGrupJurusan().getNama()); datas.add(sub);

                        // Row 6: Fakultas
                        sub = new ArrayList(); sub.add(""); sub.add(""); sub.add("");
                        sub.add(j.getFakultas() == null ? "" : j.getFakultas().getNama()); datas.add(sub);

                        // Row 7: PT + Kode PT
                        sub = new ArrayList(); sub.add(""); sub.add(""); sub.add("");
                        sub.add(j.getFakultas() == null || j.getFakultas().getPerguruanTinggi() == null ? ""
                            : j.getFakultas().getPerguruanTinggi().getNama());
                        sub.add(""); sub.add("");
                        sub.add(j.getFakultas() == null || j.getFakultas().getPerguruanTinggi() == null ? ""
                            : j.getFakultas().getPerguruanTinggi().getKodePerguruanTinggi());
                        datas.add(sub);

                        datas.add(new ArrayList());

                        // Row 9: No SK Akreditasi PS
                        sub = new ArrayList(); sub.add(""); sub.add(""); sub.add("");
                        sub.add(jenjang == null ? "" : jenjang.getNoSKAkreditasi()); datas.add(sub);

                        // Row 10: Tgl Mulai SK Akreditasi
                        sub = new ArrayList(); sub.add(""); sub.add(""); sub.add("");
                        sub.add(jenjang == null ? "" : jenjang.getTglMulaiSKAkreditasi()); datas.add(sub);

                        // Row 11: Pejabat SK Berdiri
                        sub = new ArrayList(); sub.add(""); sub.add(""); sub.add("");
                        sub.add(jenjang == null ? "" : jenjang.getPejabatSkBerdiri()); datas.add(sub);

                        datas.add(new ArrayList());

                        // Row 13: Bulan + Tahun Mulai Operasional
                        Calendar cal = null;
                        if (jenjang != null && jenjang.getTglMulaiOperasional() != null) {
                            cal = ais.ui.util.WaktuUtil.getCalendar();
                            cal.setTime(jenjang.getTglMulaiOperasional());
                        }
                        sub = new ArrayList(); sub.add(""); sub.add(""); sub.add("");
                        sub.add(cal != null ? Common.BULAN[cal.get(Calendar.MONTH)] : "");
                        sub.add(cal != null ? cal.get(Calendar.YEAR) : "");
                        datas.add(sub);

                        datas.add(new ArrayList());
                        datas.add(new ArrayList());

                        // Row 16: No SK Dikti
                        sub = new ArrayList(); sub.add(""); sub.add(""); sub.add("");
                        sub.add(jenjang == null ? "" : jenjang.getNoSKDikti()); datas.add(sub);

                        // Row 17: Tgl Akhir SK Dikti
                        sub = new ArrayList(); sub.add(""); sub.add(""); sub.add("");
                        sub.add(jenjang == null ? "" : jenjang.getTglAkhirSKDikti()); datas.add(sub);

                        datas.add(new ArrayList());
                        datas.add(new ArrayList());

                        // Row 20: Peringkat Akreditasi Prodi
                        sub = new ArrayList(); sub.add(""); sub.add(""); sub.add("");
                        sub.add(j.getPeringkatAkreditasi()); datas.add(sub);

                        // Row 21: Nilai/Status Akreditasi
                        sub = new ArrayList(); sub.add(""); sub.add(""); sub.add("");
                        sub.add(j.getAkreditasi()); datas.add(sub);

                        // Row 22: No SK Akreditasi
                        sub = new ArrayList(); sub.add(""); sub.add(""); sub.add("");
                        sub.add(j.getNoSkAkreditasi()); datas.add(sub);

                        datas.add(new ArrayList());

                        // Row 24: Alamat
                        sub = new ArrayList(); sub.add(""); sub.add(""); sub.add("");
                        sub.add(j.getAlamat()); datas.add(sub);

                        // Row 25: Alamat 2
                        sub = new ArrayList(); sub.add(""); sub.add(""); sub.add("");
                        sub.add(j.getAlamat2()); datas.add(sub);

                        datas.add(new ArrayList());
                        datas.add(new ArrayList());
                        datas.add(new ArrayList());

                        // Row 29: Telepon
                        sub = new ArrayList(); sub.add(""); sub.add(""); sub.add("");
                        sub.add(jenjang == null ? "" : jenjang.getTelpPS()); datas.add(sub);

                        datas.add(new ArrayList());

                        // Row 31: Faksimili
                        sub = new ArrayList(); sub.add(""); sub.add(""); sub.add("");
                        sub.add(jenjang == null ? "" : jenjang.getFaxPS()); datas.add(sub);

                        datas.add(new ArrayList());

                        // Row 33: Website
                        sub = new ArrayList(); sub.add(""); sub.add(""); sub.add("");
                        sub.add(jenjang == null ? "" : jenjang.getHomepagePS()); datas.add(sub);

                        datas.add(new ArrayList());

                        // Row 35: Email
                        sub = new ArrayList(); sub.add(""); sub.add(""); sub.add("");
                        sub.add(jenjang == null ? "" : jenjang.getEmail()); datas.add(sub);

                        datas.add(new ArrayList());

                        HibernateUtil.closeSession();
                        label.setAttribute("datas", datas);
                        label.setValue("");
                                        	} finally {
                    		ais.database.hibernate.HibernateUtil.closeSession();
                    	}
                    }
                }).start();
            } else {
                label.setValue("");
            }

            SaptoUtil.displayWorksheet(label, sheetCode, center, 8);

        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
        }
    }
}
