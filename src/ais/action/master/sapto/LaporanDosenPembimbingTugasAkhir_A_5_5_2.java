package ais.action.master.sapto;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zss.ui.event.CellMouseEvent;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;

import ais.action.master.sapto.util.SaptoUtil;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Jurusan;
import ais.database.model.Skripsi;
import ais.ui.util.DataCriteriaWithColumn;

public class LaporanDosenPembimbingTugasAkhir_A_5_5_2 extends SaptoBaseWindow {

    public static final String sheetCode = "A-5.5.2";
    private static final long serialVersionUID = 3331244819198611604L;
    private Combobox tahunAjaran;

    private static final String[] EMPTY_COLS = new String[180];
    static { Arrays.fill(EMPTY_COLS, ""); }

    private static final String[] SKRIPSI_COLS = {"mahasiswa.nim","mahasiswa.nama","mahasiswa.jurusan.nama","judul","abstrack","keyword","pembimbing.nama","ketuaSidang.nama","pembimbing3.nama","penguji1.nama","penguji2.nama","penguji3.nama","penguji4.nama","totalNilai","nilaiHuruf","tanggalSidang","tanggalSeminar","telahSidang","ruangSidang","waktuSidang","waktuSampaiSidang","semester","tahunAkademik","lokasiUjian","nomorSk","tglSk","selesaiDalamBulan"};

    public LaporanDosenPembimbingTugasAkhir_A_5_5_2() {
        super();
        try {
            initFakultasJurusan();
            Common.selectComboItem(tahunAjaran = Common.generateTahunAjaran(tahunAjaran), Common.getCurrentTahunAkademik());
            buildBase(false);
        } catch (Exception e) { Common.tampilErrorJikaAdmin(e); }
    }

    public LaporanDosenPembimbingTugasAkhir_A_5_5_2(String title, String border, boolean closable) throws Exception {
        super(title, border, closable);
        initFakultasJurusan();
        Common.selectComboItem(tahunAjaran = Common.generateTahunAjaran(tahunAjaran), Common.getCurrentTahunAkademik());
        buildBase(false);
    }

    @Override protected String getSheetCode() { return sheetCode; }

    @Override
    protected void buildFilters(Row row) {
        addFakultasJurusanFilter(row);

        row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik *"));
        tahunAjaran.setWidth("90%");
        tahunAjaran.setReadonly(true);
        row.appendChild(tahunAjaran);
        tahunAjaran.addEventListener("onChange", new EventListener() {
            @Override public void onEvent(Event e) throws Exception { onCetak(null); }
        });
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void onCetak(Event event) {
        clearContent();
        try {
            final Jurusan selectedJurusan = jurusan.getSelectedItem() == null
                || jurusan.getSelectedItem().getValue() == null ? null
                : (Jurusan) jurusan.getSelectedItem().getValue();
            final String tahunAkademik = (String) tahunAjaran.getSelectedItem().getValue();
            final int tahun = Integer.parseInt(tahunAkademik.split("/")[0]);

            Session session = HibernateUtil.currentNativeSession();
            final Number avg = (Number) session.createCriteria(Skripsi.class)
                .createAlias("mahasiswa","mahasiswa")
                .add(selectedJurusan == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("mahasiswa.jurusan", selectedJurusan))
                .add(Restrictions.ge("tahun", tahun - 3))
                .setProjection(Projections.avg("selesaiDalamBulan")).uniqueResult();
            HibernateUtil.closeSession();

            final Label label = new Label(ais.common.Common.getBahasaConfig("Proses load data .."));

            new Thread(new Runnable() {
                @Override
                public void run() {
                    List<List> datas = new ArrayList<List>();
                    for (int i = 0; i < 4; i++) datas.add(new ArrayList());
                    ArrayList sub = new ArrayList();
                    sub.add(""); sub.add(""); sub.add(""); sub.add(""); sub.add("");
                    sub.add(avg == null ? "" : avg.intValue());
                    datas.add(sub);
                    datas.add(new ArrayList()); datas.add(new ArrayList());
                    label.setAttribute("datas", datas);
                    label.setValue("");
                }
            }).start();

            EventListener onCellClick = new EventListener() {
                @Override
                public void onEvent(Event arg0) throws Exception {
                    try {
                        final CellMouseEvent ev = (CellMouseEvent) arg0;
                        EventListener el = (EventListener) Common.cetakDataCustomButton(
                            Skripsi.class, new DataCriteriaWithColumn() {
                                @Override
                                public Object[] initCriteria(boolean order) {
                                    Criteria c = HibernateUtil.currentSession().createCriteria(Skripsi.class)
                                        .createAlias("mahasiswa","mahasiswa")
                                        .add(selectedJurusan == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("mahasiswa.jurusan", selectedJurusan))
                                        .add(Restrictions.ge("tahun", tahun - 3));
                                    return new Object[]{c, SKRIPSI_COLS};
                                }
                            }, null, "Download Data", "/img/print.png", null, null, false, null, "DATA TAMBAHAN",
                            EMPTY_COLS).getAttribute("eventListener");
                        el.onEvent(arg0);
                    } catch (Exception e) { Common.tampilErrorJikaAdmin(e); }
                }
            };

            SaptoUtil.displayWorksheet(label, sheetCode, contentCenter, 16, onCellClick);
        } catch (Exception e) { Common.tampilErrorJikaAdmin(e); }
    }
}
