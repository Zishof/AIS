package ais.action.master.sapto;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zss.ui.event.CellMouseEvent;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.South;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Window;

import ais.action.master.helper.AmbilDataKurikulumBanbox;
import ais.action.master.helper.MatakuliahKurikulumDetailHelper;
import ais.action.master.sapto.util.SaptoUtil;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Kurikulum;
import ais.database.model.KurikulumPunyaMatakuliah;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Laporan borang akreditasi BAN-PT butir A-5.1.3 (mata kuliah pilihan dalam kurikulum): mendaftar
 * {@link KurikulumPunyaMatakuliah} berstatus "Pilihan" (dicocokkan {@code ILIKE} sebagian) dan
 * aktif, milik satu kurikulum yang dipilih pengguna lewat {@link AmbilDataKurikulumBanbox},
 * diurutkan menurut semester lalu kode mata kuliah. Mengikuti kerangka kerja laporan sapto
 * ({@link SaptoBaseWindow}); subkelas ini menentukan {@code sheetCode} ({@code "A-5.1.3"}), filter
 * pemilih kurikulum, dan pengisian data di {@link #onCetak}. Baris pada worksheet dapat diklik
 * untuk membuka jendela modal berisi detail Rencana Pembelajaran Semester (RPS) mata kuliah
 * tersebut lewat {@link MatakuliahKurikulumDetailHelper}.
 */
public class LaporanKurikulumDanMatakuliah_A_5_1_3 extends SaptoBaseWindow {

    public static final String sheetCode = "A-5.1.3";
    private static final long serialVersionUID = 3331244819198611604L;
    private AmbilDataKurikulumBanbox kurikulum;

    /** Membuat jendela laporan dan langsung membangun tata letak dasar (tanpa filter tambahan). */
    public LaporanKurikulumDanMatakuliah_A_5_1_3() {
        super();
        try { buildBase(false); } catch (Exception e) { Common.tampilErrorJikaAdmin(e); }
    }

    /** Membuat jendela laporan dengan judul/border/closable kustom; setup sama seperti konstruktor default. */
    public LaporanKurikulumDanMatakuliah_A_5_1_3(String title, String border, boolean closable) throws Exception {
        super(title, border, closable);
        buildBase(false);
    }

    @Override protected String getSheetCode() { return sheetCode; }

    /** Menambahkan pemilih kurikulum ({@link AmbilDataKurikulumBanbox}) ke baris filter; memicu {@link #onCetak} otomatis saat kurikulum dipilih. */
    @Override
    protected void buildFilters(Row row) {
        row.appendChild(new ais.ui.util.MyLabelConfig("Kurikulum (harus dipilih)"));
        kurikulum = new AmbilDataKurikulumBanbox();
        kurikulum.setWidth("90%");
        kurikulum.setEventListener(new EventListener() {
            @Override public void onEvent(Event e) throws Exception { onCetak(null); }
        });
        row.appendChild(kurikulum);
    }

    /** Mengambil mata kuliah pilihan aktif milik kurikulum terpilih (di thread terpisah bila kurikulum sudah dipilih) dan menampilkannya sebagai worksheet A-5.1.3, dengan klik baris membuka detail RPS dalam jendela modal. */
    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void onCetak(Event event) {
        clearContent();
        try {
            final Kurikulum selectedKurikulum = (Kurikulum) kurikulum.getAttribute("kurikulum");

            Session session = HibernateUtil.currentNativeSession();
            final List<KurikulumPunyaMatakuliah> items = session.createCriteria(KurikulumPunyaMatakuliah.class)
                .add(Restrictions.eq("kurikulum", selectedKurikulum))
                .createAlias("matakuliah","matakuliah").addOrder(Order.asc("semester"))
                .add(Restrictions.ilike("matakuliah.status", "Pilihan", MatchMode.ANYWHERE))
                .add(Restrictions.or(Restrictions.isNull("matakuliah.aktif"), Restrictions.eq("matakuliah.aktif", true)))
                .addOrder(Order.asc("matakuliah.kode")).list();
            HibernateUtil.closeSession();

            final Label label = new Label(ais.common.Common.getBahasaConfig("Proses load data .."));

            if (selectedKurikulum != null) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        List<List> datas = new ArrayList<List>();
                        for (int i = 0; i < 9; i++) datas.add(new ArrayList());

                        for (KurikulumPunyaMatakuliah kpm : items) {
                            ArrayList sub = new ArrayList();
                            sub.add("");
                            sub.add(kpm.getSemester() == null ? "" : kpm.getSemester().toString());
                            sub.add(kpm.getMatakuliah().getKode());
                            sub.add(kpm.getMatakuliah().getNama());
                            sub.add(kpm.getMatakuliah().getSks());
                            sub.add(kpm.getTerdapatTugas() ? "V" : "");
                            sub.add(kpm.getKurikulum().getJurusan().getNama());
                            datas.add(sub);
                        }

                        datas.add(new ArrayList()); datas.add(new ArrayList());
                        label.setAttribute("datas", datas);
                        label.setValue("");
                    }
                }).start();
            } else {
                label.setValue("");
            }

            EventListener onCellClick = new EventListener() {
                @Override
                public void onEvent(Event arg0) throws Exception {
                    try {
                        CellMouseEvent ev = (CellMouseEvent) arg0;
                        int y = ev.getRow() - 9;
                        final KurikulumPunyaMatakuliah kpm = items.get(y);
                        final Window window = new Window("Data Rencana Pembelajaran Semester", "none", true);
                        window.setHeight("99%"); window.setWidth("90%");
                        window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
                        Borderlayout bl = new ais.ui.util.MyBorderlayout();
                        bl.setParent(window);
                        Center c = new Center(); c.setParent(bl);
                        ais.ui.util.ZkCompat.setFlex(c, true);
                        new MatakuliahKurikulumDetailHelper().display(kpm, null, c);
                        South south = new South();
                        ais.ui.util.ZkCompat.setFlex(south, true);
                        south.setParent(bl);
                        Toolbar toolbar = new Toolbar(); toolbar.setParent(south);
                        MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
                        cancel.setTooltiptext("Tutup");
                        cancel.addEventListener("onClick", new EventListener() {
                            @Override public void onEvent(Event e) throws Exception { window.detach(); }
                        });
                        cancel.setParent(toolbar);
                        window.onModal();
                    } catch (Exception e) { Common.tampilErrorJikaAdmin(e); }
                }
            };

            SaptoUtil.displayWorksheet(label, sheetCode, contentCenter, 16, onCellClick);
        } catch (Exception e) { Common.tampilErrorJikaAdmin(e); }
    }
}
