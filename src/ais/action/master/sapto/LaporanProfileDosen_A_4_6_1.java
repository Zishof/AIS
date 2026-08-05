package ais.action.master.sapto;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;

import ais.action.master.sapto.util.SaptoUtil;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;

public class LaporanProfileDosen_A_4_6_1 extends SaptoBaseWindow {

    public static final String sheetCode = "A-4.6.1";
    private static final long serialVersionUID = 3331244819198611604L;

    public LaporanProfileDosen_A_4_6_1() {
        super();
        try { buildBase(true, false); } catch (Exception e) { Common.tampilErrorJikaAdmin(e); }
    }

    public LaporanProfileDosen_A_4_6_1(String title, String border, boolean closable) throws Exception {
        super(title, border, closable);
        buildBase(true, false);
    }

    @Override protected String getSheetCode() { return sheetCode; }
    @Override protected void buildFilters(Row row) { /* no visible filters */ }

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
                        "from pegawai a inner join employ.pendidikan b on (a.pendidikan=b.id) inner join jenis_tenaga_kependidikan c on (a.jenis_tenaga_kependidikan=c.id) where c.nama ilike '%Pustakawan%' " +
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
                    for (int i = 1; i <= objs.size(); i++) {
                        List sub = new ArrayList();
                        sub.add(""); sub.add(""); sub.add(""); sub.add("");
                        for (int c = 0; c < 8; c++) {
                            sub.add(objs.get(i - 1)[c] == null ? 0.0 : Double.parseDouble(objs.get(i - 1)[c].toString().trim()));
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

            SaptoUtil.displayWorksheet(label, sheetCode, contentCenter, 16);
        } catch (Exception e) { Common.tampilErrorJikaAdmin(e); }
    }
}
