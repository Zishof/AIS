package ais.action.master.sapto;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;

import ais.action.master.sapto.util.SaptoUtil;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.sapto.PrasaranaPerguruanTinggiSapto;

public class LaporanPrasaranaInstitusi_A_6_2_3B extends SaptoBaseWindow {

    public static final String sheetCode = "A-6.2.3B";
    private static final long serialVersionUID = 3331244819198611604L;

    public LaporanPrasaranaInstitusi_A_6_2_3B() {
        super();
        try { buildBase(true); } catch (Exception e) { Common.tampilErrorJikaAdmin(e); }
    }

    public LaporanPrasaranaInstitusi_A_6_2_3B(String title, String border, boolean closable) throws Exception {
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
                    for (int i = 0; i < 11; i++) datas.add(new ArrayList());

                    List<PrasaranaPerguruanTinggiSapto> list = session
                        .createCriteria(PrasaranaPerguruanTinggiSapto.class)
                        .add(Restrictions.eq("utama", false))
                        .addOrder(Order.asc("tahun")).addOrder(Order.asc("nama")).addOrder(Order.asc("id")).list();

                    for (PrasaranaPerguruanTinggiSapto p : list) {
                        List sub = new ArrayList();
                        sub.add("");
                        sub.add(p.getTahun());
                        sub.add(p.getNama());
                        sub.add(p.getJumlah());
                        sub.add(p.getLuas());

                        boolean milikSendiri = p.getJenisKepemilikan() != null &&
                            p.getJenisKepemilikan().equals(PrasaranaPerguruanTinggiSapto.KEPEMILIKAN_SENDIRI);
                        sub.add(milikSendiri ? "v" : "");
                        sub.add(milikSendiri ? "" : "v");

                        boolean terawat = p.getKondisi() != null &&
                            p.getKondisi().equals(PrasaranaPerguruanTinggiSapto.KONDISI_TERAWAT);
                        sub.add(terawat ? "v" : "");
                        sub.add(terawat ? "" : "v");

                        datas.add(sub);
                    }

                    HibernateUtil.closeSession();
                    datas.add(new ArrayList()); datas.add(new ArrayList());
                    for (int i = 0; i < 15; i++) datas.add(new ArrayList());
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
