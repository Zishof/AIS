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
import ais.database.model.Pegawai;
import ais.ui.util.DataCriteriaWithColumn;

public class LaporanDosenInstitusi_A_4_5_1 extends SaptoBaseWindow {

    public static final String sheetCode = "A-4.5.1_PT";
    private static final long serialVersionUID = 3331244819198611604L;

    public LaporanDosenInstitusi_A_4_5_1() {
        super();
        try { buildBase(true); } catch (Exception e) { Common.tampilErrorJikaAdmin(e); }
    }

    public LaporanDosenInstitusi_A_4_5_1(String title, String border, boolean closable) throws Exception {
        super(title, border, closable);
        buildBase(true);
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
                        "sum(case when b.nama ilike '%S3%' then 1 else 0 end) as s3," +
                        "sum(case when b.nama ilike '%S2%' then 1 else 0 end) as s2," +
                        "sum(case when b.nama ilike '%S1%' then 1 else 0 end) as s1," +
                        "sum(case when b.nama ilike '%D4%' then 1 else 0 end) as d4," +
                        "sum(case when b.nama ilike '%D3%' then 1 else 0 end) as d3," +
                        "sum(case when b.nama ilike '%D2%' then 1 else 0 end) as d2," +
                        "sum(case when b.nama ilike '%D1%' then 1 else 0 end) as d1," +
                        "sum(case when b.nama ilike '%SMU%' or b.nama ilike '%SMA%' or b.nama ilike '%MA%' or b.nama ilike '%STM%' then 1 else 0 end) as sma " +
                        "from pegawai a " +
                        "inner join employ.pendidikan b on (a.pendidikan=b.id) " +
                        "inner join jenis_tenaga_kependidikan c on (a.jenis_tenaga_kependidikan=c.id) " +
                        "where c.nama ilike '%Pustakawan%' " +
                        "union all " +
                        "select sum(case when b.nama ilike '%S3%' then 1 else 0 end),sum(case when b.nama ilike '%S2%' then 1 else 0 end),sum(case when b.nama ilike '%S1%' then 1 else 0 end),sum(case when b.nama ilike '%D4%' then 1 else 0 end),sum(case when b.nama ilike '%D3%' then 1 else 0 end),sum(case when b.nama ilike '%D2%' then 1 else 0 end),sum(case when b.nama ilike '%D1%' then 1 else 0 end),sum(case when b.nama ilike '%SMU%' or b.nama ilike '%SMA%' or b.nama ilike '%MA%' or b.nama ilike '%STM%' then 1 else 0 end) " +
                        "from pegawai a inner join employ.pendidikan b on (a.pendidikan=b.id) inner join jenis_tenaga_kependidikan c on (a.jenis_tenaga_kependidikan=c.id) where c.nama ilike '%Programer%' " +
                        "union all " +
                        "select sum(case when b.nama ilike '%S3%' then 1 else 0 end),sum(case when b.nama ilike '%S2%' then 1 else 0 end),sum(case when b.nama ilike '%S1%' then 1 else 0 end),sum(case when b.nama ilike '%D4%' then 1 else 0 end),sum(case when b.nama ilike '%D3%' then 1 else 0 end),sum(case when b.nama ilike '%D2%' then 1 else 0 end),sum(case when b.nama ilike '%D1%' then 1 else 0 end),sum(case when b.nama ilike '%SMU%' or b.nama ilike '%SMA%' or b.nama ilike '%MA%' or b.nama ilike '%STM%' then 1 else 0 end) " +
                        "from pegawai a inner join employ.pendidikan b on (a.pendidikan=b.id) inner join jenis_tenaga_kependidikan c on (a.jenis_tenaga_kependidikan=c.id) where c.nama ilike '%Administrasi%' " +
                        "union all " +
                        "select sum(case when b.nama ilike '%S3%' then 1 else 0 end),sum(case when b.nama ilike '%S2%' then 1 else 0 end),sum(case when b.nama ilike '%S1%' then 1 else 0 end),sum(case when b.nama ilike '%D4%' then 1 else 0 end),sum(case when b.nama ilike '%D3%' then 1 else 0 end),sum(case when b.nama ilike '%D2%' then 1 else 0 end),sum(case when b.nama ilike '%D1%' then 1 else 0 end),sum(case when b.nama ilike '%SMU%' or b.nama ilike '%SMA%' or b.nama ilike '%MA%' or b.nama ilike '%STM%' then 1 else 0 end) " +
                        "from pegawai a inner join employ.pendidikan b on (a.pendidikan=b.id) inner join jenis_tenaga_kependidikan c on (a.jenis_tenaga_kependidikan=c.id) where c.nama ilike '%Lainnya%'";

                    List<Object[]> objs = session.createSQLQuery(sql).list();
                    for (Object[] obj : objs) {
                        List sub = new ArrayList();
                        sub.add(""); sub.add(""); sub.add(""); sub.add("");
                        for (int c = 0; c < 8; c++) {
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

            final String[] jenisTKArr = {"Pustakawan","Laboran/Teknisi/Analis/Operator/Programer","Administrasi","Lainnya",""};

            EventListener onCellClick = new EventListener() {
                @Override
                public void onEvent(Event arg0) throws Exception {
                    try {
                        final CellMouseEvent ev = (CellMouseEvent) arg0;
                        EventListener el = (EventListener) Common.cetakDataCustomButton(
                            Pegawai.class, new DataCriteriaWithColumn() {
                                @Override
                                public Object[] initCriteria(boolean order) {
                                    try {
                                        int x = ev.getColumn() - 4;
                                        int y = ev.getRow() - 9;
                                        String colY = jenisTKArr[y];
                                        String[] jenjang = x == 0 ? new String[]{"S3","Strata 3"}
                                            : x == 1 ? new String[]{"S2","Strata 2"}
                                            : x == 2 ? new String[]{"S1","Strata 1"}
                                            : x == 3 ? new String[]{"D4"}
                                            : x == 4 ? new String[]{"D3"}
                                            : x == 5 ? new String[]{"D2"}
                                            : x == 6 ? new String[]{"D1"}
                                            : x == 7 ? new String[]{"SMA / sederajat","SMA","SMU","SMK","MA"}
                                            : new String[]{};

                                        Criteria criteria = HibernateUtil.currentSession()
                                            .createCriteria(Pegawai.class)
                                            .add(Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")))
                                            .createAlias("jenisTenagaKependidikan","jenisTenagaKependidikan")
                                            .add(colY.trim().isEmpty() ? Restrictions.sqlRestriction("true")
                                                : Restrictions.ilike("jenisTenagaKependidikan.nama", colY, MatchMode.EXACT))
                                            .createAlias("pendidikan","pendidikan")
                                            .add(jenjang.length == 0 ? Restrictions.sqlRestriction("true")
                                                : Restrictions.in("pendidikan.nama", jenjang));

                                        return new Object[]{criteria, new String[]{"mycode","nama","panggilan","kelamin",
                                            "tempatlahir","tanggallahir","ktp","npwp","gelarDepan","gelarBelakang",
                                            "jenisTenagaKependidikan.nama","pendidikan.nama","sertifikasi",
                                            "jabatanFungsional.nama","jabatanStruktural.nama","statusPegawai.nama",
                                            "satuanKerja.nama","gajiPokok.nama","spesifikasiJabatan.nama",
                                            "golonganPegawai.nama","agama.nama","statusPerkawinan"}};
                                    } catch (Exception e) { Common.tampilErrorJikaAdmin(e); }
                                    return null;
                                }
                            }, null, "Download Data", "/img/print.png", null, null, false, null, "DATA TAMBAHAN",
                            new String[48]).getAttribute("eventListener");
                        el.onEvent(arg0);
                    } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sapto/LaporanDosenInstitusi_A_4_5_1.java:146"); /* ignore */ }
                }
            };

            SaptoUtil.displayWorksheet(label, sheetCode, contentCenter, 16, onCellClick);
        } catch (Exception e) { Common.tampilErrorJikaAdmin(e); }
    }
}
