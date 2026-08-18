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
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;

import ais.action.master.MahasiswaAction;
import ais.action.master.sapto.util.SaptoUtil;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.Jurusan;
import ais.database.model.Mahasiswa;
import ais.database.model.Pertemuan;
import ais.ui.util.DataCriteriaWithColumn;

public class LaporanDosenPembimbing_A_5_4_1 extends SaptoBaseWindow {

    public static final String sheetCode = "A-5.4.1";
    private static final long serialVersionUID = 3331244819198611604L;
    private static final String[] EMPTY_COLS = new String[180];
    static { Arrays.fill(EMPTY_COLS, ""); }

    public LaporanDosenPembimbing_A_5_4_1() {
        super();
        try {
            initFakultasJurusan();
            buildBase(false);
        } catch (Exception e) { Common.tampilErrorJikaAdmin(e); }
    }

    public LaporanDosenPembimbing_A_5_4_1(String title, String border, boolean closable) throws Exception {
        super(title, border, closable);
        initFakultasJurusan();
        buildBase(false);
    }

    @Override protected String getSheetCode() { return sheetCode; }

    @Override
    protected void buildFilters(Row row) {
        addFakultasJurusanFilter(row);
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void onCetak(Event event) {
        clearContent();
        try {
            final Jurusan selectedJurusan = jurusan.getSelectedItem() == null
                || jurusan.getSelectedItem().getValue() == null ? null
                : (Jurusan) jurusan.getSelectedItem().getValue();

            Session session = HibernateUtil.currentNativeSession();
            final List<Long> dosens = session.createCriteria(Mahasiswa.class)
                .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                .add(selectedJurusan == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("jurusan", selectedJurusan))
                .add(Restrictions.isNotNull("dosen"))
                .setProjection(Projections.groupProperty("dosen")).list();
            HibernateUtil.closeSession();

            final Label label = new Label(ais.common.Common.getBahasaConfig("Proses load data .."));

            new Thread(new Runnable() {
                @Override
                public void run() {
                	try {
                    List<List> datas = new ArrayList<List>();
                    for (int i = 0; i < 8; i++) datas.add(new ArrayList());

                    Integer index = 1;
                    Session s = HibernateUtil.currentNativeSession();
                    for (Long dosenId : dosens) {
                        String dosenNama = (String) s.createCriteria(Dosen.class)
                            .setProjection(Projections.property("nama"))
                            .add(Restrictions.idEq(dosenId)).uniqueResult();
                        if (dosenNama != null) {
                            ArrayList sub = new ArrayList();
                            sub.add(""); sub.add(index.toString()); sub.add(dosenNama);
                            List<Long> mhs = s.createCriteria(Mahasiswa.class)
                                .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                                .add(Restrictions.eq("dosen", dosenId))
                                .setProjection(Projections.groupProperty("id")).list();
                            sub.add(mhs.size());
                            double total = 0.0;
                            for (Long m : mhs) {
                                double jmlh = ((Number) s.createCriteria(Pertemuan.class)
                                    .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                                    .createAlias("krsMahasiswa","krsMahasiswa")
                                    .add(Restrictions.eq("krsMahasiswa.dosenPa.id", dosenId))
                                    .add(Restrictions.eq("krsMahasiswa.mahasiswa.id", m))
                                    .setProjection(Projections.rowCount()).uniqueResult()).doubleValue();
                                total += jmlh;
                            }
                            sub.add(mhs.size() == 0 ? 0.0 : total / mhs.size());
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
                            Mahasiswa.class, new DataCriteriaWithColumn() {
                                @Override
                                public Object[] initCriteria(boolean order) {
                                    int y = ev.getRow() - 8;
                                    Long dsn = dosens.get(y);
                                    Criteria c = HibernateUtil.currentSession().createCriteria(Mahasiswa.class)
                                        .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                                        .add(Restrictions.eq("dosen", dsn));
                                    return new Object[]{c, MahasiswaAction.contents};
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
