package ais.action.master.sapto;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zss.ui.event.CellMouseEvent;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;

import ais.action.master.sapto.util.SaptoUtil;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Jurusan;
import ais.ui.util.DataCriteria;

public class LaporanStatusAkreditasiProdi_A_2_4_6 extends SaptoBaseWindow {

    public static final String sheetCode = "A-2.4.6";
    private static final long serialVersionUID = 3331244819198611604L;

    public LaporanStatusAkreditasiProdi_A_2_4_6() {
        super();
        try { buildBase(true, false); } catch (Exception e) { Common.tampilErrorJikaAdmin(e); }
    }

    public LaporanStatusAkreditasiProdi_A_2_4_6(String title, String border, boolean closable) throws Exception {
        super(title, border, closable);
        buildBase(true, false);
    }

    @Override protected String getSheetCode() { return sheetCode; }

    @Override protected void buildFilters(Row row) { /* no filters */ }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void onCetak(Event event) {
        clearContent();
        try {
            final Label label = new Label(ais.common.Common.getBahasaConfig("Proses load data .."));

            new Thread(new Runnable() {
                @Override
                public void run() {
                	try {
                    Session session = HibernateUtil.currentNativeSession();
                    List<List> datas = new ArrayList<List>();
                    for (int i = 0; i < 9; i++) datas.add(new ArrayList());

                    String sql =
                        "select " +
                        "sum(case when b.nama ilike 'S3' or b.nama ilike 'Strata 3' then 1 else 0 end) as s3," +
                        "sum(case when b.nama ilike 'S2' or b.nama ilike 'Strata 2' then 1 else 0 end) as s2," +
                        "sum(case when b.nama ilike 'S1' or b.nama ilike 'Strata 1' then 1 else 0 end) as s1," +
                        "sum(case when b.nama ilike 'Sp-2' then 1 else 0 end) as sp2," +
                        "sum(case when b.nama ilike 'Sp-1' then 1 else 0 end) as sp1," +
                        "sum(case when b.nama ilike 'Profesi' then 1 else 0 end) as profesi," +
                        "sum(case when b.nama ilike 'D4' then 1 else 0 end) as d4," +
                        "sum(case when b.nama ilike 'D3' then 1 else 0 end) as d3," +
                        "sum(case when b.nama ilike 'D2' then 1 else 0 end) as d2," +
                        "sum(case when b.nama ilike 'D1' then 1 else 0 end) as d1 " +
                        "from jurusan a inner join jenjang b on (a.jenjang=b.id) " +
                        "where (a.aktif or a.aktif is null) and a.statusakreditasi='Terakreditasi A' " +
                        "union all " +
                        "select " +
                        "sum(case when b.nama ilike 'S3' or b.nama ilike 'Strata 3' then 1 else 0 end)," +
                        "sum(case when b.nama ilike 'S2' or b.nama ilike 'Strata 2' then 1 else 0 end)," +
                        "sum(case when b.nama ilike 'S1' or b.nama ilike 'Strata 1' then 1 else 0 end)," +
                        "sum(case when b.nama ilike 'Sp-2' then 1 else 0 end)," +
                        "sum(case when b.nama ilike 'Sp-1' then 1 else 0 end)," +
                        "sum(case when b.nama ilike 'Profesi' then 1 else 0 end)," +
                        "sum(case when b.nama ilike 'D4' then 1 else 0 end)," +
                        "sum(case when b.nama ilike 'D3' then 1 else 0 end)," +
                        "sum(case when b.nama ilike 'D2' then 1 else 0 end)," +
                        "sum(case when b.nama ilike 'D1' then 1 else 0 end) " +
                        "from jurusan a inner join jenjang b on (a.jenjang=b.id) " +
                        "where (a.aktif or a.aktif is null) and a.statusakreditasi='Terakreditasi B' " +
                        "union all " +
                        "select " +
                        "sum(case when b.nama ilike 'S3' or b.nama ilike 'Strata 3' then 1 else 0 end)," +
                        "sum(case when b.nama ilike 'S2' or b.nama ilike 'Strata 2' then 1 else 0 end)," +
                        "sum(case when b.nama ilike 'S1' or b.nama ilike 'Strata 1' then 1 else 0 end)," +
                        "sum(case when b.nama ilike 'Sp-2' then 1 else 0 end)," +
                        "sum(case when b.nama ilike 'Sp-1' then 1 else 0 end)," +
                        "sum(case when b.nama ilike 'Profesi' then 1 else 0 end)," +
                        "sum(case when b.nama ilike 'D4' then 1 else 0 end)," +
                        "sum(case when b.nama ilike 'D3' then 1 else 0 end)," +
                        "sum(case when b.nama ilike 'D2' then 1 else 0 end)," +
                        "sum(case when b.nama ilike 'D1' then 1 else 0 end) " +
                        "from jurusan a inner join jenjang b on (a.jenjang=b.id) " +
                        "where (a.aktif or a.aktif is null) and a.statusakreditasi='Terakreditasi C' " +
                        "union all " +
                        "select " +
                        "sum(case when b.nama ilike 'S3' or b.nama ilike 'Strata 3' then 1 else 0 end)," +
                        "sum(case when b.nama ilike 'S2' or b.nama ilike 'Strata 2' then 1 else 0 end)," +
                        "sum(case when b.nama ilike 'S1' or b.nama ilike 'Strata 1' then 1 else 0 end)," +
                        "sum(case when b.nama ilike 'Sp-2' then 1 else 0 end)," +
                        "sum(case when b.nama ilike 'Sp-1' then 1 else 0 end)," +
                        "sum(case when b.nama ilike 'Profesi' then 1 else 0 end)," +
                        "sum(case when b.nama ilike 'D4' then 1 else 0 end)," +
                        "sum(case when b.nama ilike 'D3' then 1 else 0 end)," +
                        "sum(case when b.nama ilike 'D2' then 1 else 0 end)," +
                        "sum(case when b.nama ilike 'D1' then 1 else 0 end) " +
                        "from jurusan a inner join jenjang b on (a.jenjang=b.id) " +
                        "where (a.aktif or a.aktif is null) and a.statusakreditasi='Akreditasi Kadaluwarsa' " +
                        "union all " +
                        "select " +
                        "sum(case when b.nama ilike 'S3' or b.nama ilike 'Strata 3' then 1 else 0 end)," +
                        "sum(case when b.nama ilike 'S2' or b.nama ilike 'Strata 2' then 1 else 0 end)," +
                        "sum(case when b.nama ilike 'S1' or b.nama ilike 'Strata 1' then 1 else 0 end)," +
                        "sum(case when b.nama ilike 'Sp-2' then 1 else 0 end)," +
                        "sum(case when b.nama ilike 'Sp-1' then 1 else 0 end)," +
                        "sum(case when b.nama ilike 'Profesi' then 1 else 0 end)," +
                        "sum(case when b.nama ilike 'D4' then 1 else 0 end)," +
                        "sum(case when b.nama ilike 'D3' then 1 else 0 end)," +
                        "sum(case when b.nama ilike 'D2' then 1 else 0 end)," +
                        "sum(case when b.nama ilike 'D1' then 1 else 0 end) " +
                        "from jurusan a inner join jenjang b on (a.jenjang=b.id) " +
                        "where (a.aktif or a.aktif is null) " +
                        "and (a.statusakreditasi='Belum Terakreditasi' or a.statusakreditasi is null)";

                    List<Object[]> objs = session.createSQLQuery(sql).list();
                    for (Object[] obj : objs) {
                        List sub = new ArrayList();
                        sub.add(""); sub.add(""); sub.add("");
                        for (int c = 0; c < 10; c++) {
                            sub.add(obj[c] == null ? 0.0 : Double.parseDouble(obj[c].toString().trim()));
                        }
                        datas.add(sub);
                    }
                    HibernateUtil.closeSession();
                    datas.add(new ArrayList()); datas.add(new ArrayList()); datas.add(new ArrayList());
                    label.setAttribute("datas", datas);
                    label.setValue("");
                                	} finally {
                		ais.database.hibernate.HibernateUtil.closeSession();
                	}
                }
            }).start();

            final String[] xx = {"S3-Strata 3","S2-Strata 2","S1-Strata 1","Sp-2","Sp-1","Profesi","D4","D3","D2","D1","Total","Total"};
            final String[] yy = {"Terakreditasi A","Terakreditasi B","Terakreditasi C","Akreditasi Kadaluwarsa","Belum Terakreditasi","Total","Total"};

            EventListener onCellClick = new EventListener() {
                @Override
                public void onEvent(Event arg0) throws Exception {
                    try {
                        final CellMouseEvent event = (CellMouseEvent) arg0;
                        EventListener el = (EventListener) Common.cetakData(new DataCriteria() {
                            @Override
                            public Criteria initCriteria(boolean order) {
                                int x = event.getColumn() - 3;
                                int y = event.getRow() - 9;
                                String[] colX = xx[x].split("-");
                                String colX1 = colX.length > 0 ? colX[0] : "";
                                String colX2 = colX.length > 1 ? colX[1] : "";
                                String colY  = yy[y];
                                return HibernateUtil.currentSession().createCriteria(Jurusan.class)
                                    .createAlias("jenjang","jenjang")
                                    .add(colX1.equalsIgnoreCase("Total") ? Restrictions.sqlRestriction("true")
                                        : Restrictions.or(
                                            Restrictions.ilike("jenjang.nama", colX1, MatchMode.EXACT),
                                            Restrictions.ilike("jenjang.nama", colX2, MatchMode.EXACT)))
                                    .add(colY.equalsIgnoreCase("Total") ? Restrictions.sqlRestriction("true")
                                        : Restrictions.ilike("statusAkreditasi", colY, MatchMode.EXACT));
                            }
                        }, "kodeEpsbed","nama","fakultas.nama","jenjang.nama","kaprodi.nama","gelar","singkatanGelar",
                           "jenisJurusan","statusAkreditasi","peringkatAkreditasi","akreditasi","noSkAkreditasi",
                           "tanggalAkreditasi","alamat","alamat2","deskripsi").getAttribute("eventListener");
                        el.onEvent(event);
                    } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sapto/LaporanStatusAkreditasiProdi_A_2_4_6.java:177"); /* ignore */ }
                }
            };

            SaptoUtil.displayWorksheet(label, sheetCode, contentCenter, 16, onCellClick);
        } catch (Exception e) { Common.tampilErrorJikaAdmin(e); }
    }
}
