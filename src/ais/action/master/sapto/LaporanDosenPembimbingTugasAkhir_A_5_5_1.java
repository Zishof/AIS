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
import ais.database.model.Dosen;
import ais.database.model.Jurusan;
import ais.database.model.Skripsi;
import ais.ui.util.DataCriteriaWithColumn;

public class LaporanDosenPembimbingTugasAkhir_A_5_5_1 extends SaptoBaseWindow {

    public static final String sheetCode = "A-5.5.1";
    private static final long serialVersionUID = 3331244819198611604L;
    private Combobox tahunAjaran;

    private static final String[] EMPTY_COLS = new String[180];
    static { Arrays.fill(EMPTY_COLS, ""); }

    private static final String[] SKRIPSI_COLS = {"mahasiswa.nim","mahasiswa.nama","mahasiswa.jurusan.nama","judul","abstrack","keyword","pembimbing.nama","ketuaSidang.nama","pembimbing3.nama","penguji1.nama","penguji2.nama","penguji3.nama","penguji4.nama","totalNilai","nilaiHuruf","tanggalSidang","tanggalSeminar","telahSidang","ruangSidang","waktuSidang","waktuSampaiSidang","semester","tahunAkademik","lokasiUjian","nomorSk","tglSk","selesaiDalamBulan"};

    public LaporanDosenPembimbingTugasAkhir_A_5_5_1() {
        super();
        try {
            initFakultasJurusan();
            Common.selectComboItem(tahunAjaran = Common.generateTahunAjaran(tahunAjaran), Common.getCurrentTahunAkademik());
            buildBase(false);
        } catch (Exception e) { Common.tampilErrorJikaAdmin(e); }
    }

    public LaporanDosenPembimbingTugasAkhir_A_5_5_1(String title, String border, boolean closable) throws Exception {
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

            Session session = HibernateUtil.currentNativeSession();
            final List<Long> dosens1 = session.createCriteria(Skripsi.class)
                .add(Restrictions.eq("tahunAkademik", tahunAkademik))
                .createAlias("mahasiswa","mahasiswa")
                .add(selectedJurusan == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("mahasiswa.jurusan", selectedJurusan))
                .add(Restrictions.isNotNull("pembimbing.id"))
                .setProjection(Projections.groupProperty("pembimbing.id")).list();
            List<Long> dosens2 = session.createCriteria(Skripsi.class)
                .add(Restrictions.eq("tahunAkademik", tahunAkademik))
                .createAlias("mahasiswa","mahasiswa")
                .add(selectedJurusan == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("mahasiswa.jurusan", selectedJurusan))
                .add(Restrictions.isNotNull("ketuaSidang.id"))
                .setProjection(Projections.groupProperty("ketuaSidang.id")).list();
            List<Long> dosens3 = session.createCriteria(Skripsi.class)
                .add(Restrictions.eq("tahunAkademik", tahunAkademik))
                .createAlias("mahasiswa","mahasiswa")
                .add(selectedJurusan == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("mahasiswa.jurusan", selectedJurusan))
                .add(Restrictions.isNotNull("pembimbing3.id"))
                .setProjection(Projections.groupProperty("pembimbing3.id")).list();
            HibernateUtil.closeSession();
            for (Long l : dosens2) { if (!dosens1.contains(l)) dosens1.add(l); }
            for (Long l : dosens3) { if (!dosens1.contains(l)) dosens1.add(l); }

            final Label label = new Label(ais.common.Common.getBahasaConfig("Proses load data .."));

            new Thread(new Runnable() {
                @Override
                public void run() {
                	try {
                    List<List> datas = new ArrayList<List>();
                    for (int i = 0; i < 12; i++) datas.add(new ArrayList());

                    Integer index = 1;
                    Session s = HibernateUtil.currentNativeSession();
                    for (Long dosenId : dosens1) {
                        String dosenNama = (String) s.createCriteria(Dosen.class)
                            .setProjection(Projections.property("nama"))
                            .add(Restrictions.idEq(dosenId)).uniqueResult();
                        if (dosenNama != null) {
                            ArrayList sub = new ArrayList();
                            sub.add(""); sub.add(index.toString()); sub.add(dosenNama);
                            int count = ((Number) s.createCriteria(Skripsi.class)
                                .add(Restrictions.eq("tahunAkademik", tahunAkademik))
                                .createAlias("mahasiswa","mahasiswa")
                                .add(selectedJurusan == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("mahasiswa.jurusan", selectedJurusan))
                                .add(Restrictions.or(Restrictions.eq("pembimbing3.id", dosenId),
                                     Restrictions.or(Restrictions.eq("pembimbing.id", dosenId), Restrictions.eq("ketuaSidang.id", dosenId))))
                                .setProjection(Projections.rowCount()).uniqueResult()).intValue();
                            sub.add(count);
                            datas.add(sub);
                            index++;
                        }
                    }
                    HibernateUtil.closeSession();
                    datas.add(new ArrayList()); datas.add(new ArrayList());
                    label.setAttribute("datas", datas);
                    label.setValue("");
                                	} finally {
                		ais.database.hibernate.HibernateUtil.closeSession();
                	}
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
                                    int y = ev.getRow() - 12;
                                    Long dsn = dosens1.get(y);
                                    Criteria c = HibernateUtil.currentSession().createCriteria(Skripsi.class)
                                        .add(Restrictions.eq("tahunAkademik", tahunAkademik))
                                        .createAlias("mahasiswa","mahasiswa")
                                        .add(selectedJurusan == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("mahasiswa.jurusan", selectedJurusan))
                                        .add(Restrictions.or(Restrictions.eq("pembimbing3.id", dsn),
                                             Restrictions.or(Restrictions.eq("pembimbing.id", dsn), Restrictions.eq("ketuaSidang.id", dsn))));
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
