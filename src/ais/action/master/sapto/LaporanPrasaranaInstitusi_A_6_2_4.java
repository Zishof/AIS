package ais.action.master.sapto;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;

import ais.action.master.sapto.util.SaptoUtil;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.sapto.InvestasiPrasaranaPerguruanTinggiSapto;

/**
 * Laporan SAPTO/borang akreditasi BAN-PT butir A.6.2.4 (kode sheet {@code A-6.2.4_PT}): rekap
 * rencana investasi prasarana institusi dari {@link InvestasiPrasaranaPerguruanTinggiSapto},
 * diurutkan berdasarkan nama. Untuk setiap item ditampilkan nama prasarana, nilai investasi
 * selama 3 tahun terakhir, rencana investasi (ke depan), dan sumber dana yang direncanakan. Tidak
 * ada filter — cakupan selalu seluruh institusi. Data dimuat asinkron lalu dirender lewat
 * {@link SaptoUtil#displayWorksheet}.
 */
public class LaporanPrasaranaInstitusi_A_6_2_4 extends SaptoBaseWindow {

    public static final String sheetCode = "A-6.2.4_PT";
    private static final long serialVersionUID = 3331244819198611604L;

    /** Membangun jendela laporan (tanpa filter tambahan). */
    public LaporanPrasaranaInstitusi_A_6_2_4() {
        super();
        try { buildBase(true); } catch (Exception e) { Common.tampilErrorJikaAdmin(e); }
    }

    /** Membangun jendela laporan dengan judul/border/closable kustom. */
    public LaporanPrasaranaInstitusi_A_6_2_4(String title, String border, boolean closable) throws Exception {
        super(title, border, closable);
        buildBase(true);
    }

    /** @return kode sheet borang {@code "A-6.2.4_PT"}. */
    @Override protected String getSheetCode() { return sheetCode; }
    /** Tidak ada filter untuk laporan ini — cakupan selalu seluruh institusi. */
    @Override protected void buildFilters(Row row) { /* no filters */ }

    /**
     * Menghitung dan menampilkan rekap rencana investasi prasarana institusi. Data dimuat asinkron
     * lalu dirender lewat {@link SaptoUtil#displayWorksheet}.
     *
     * @param event event pemicu, boleh {@code null}
     */
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
                    for (int i = 0; i < 8; i++) datas.add(new ArrayList());

                    List<InvestasiPrasaranaPerguruanTinggiSapto> list = session
                        .createCriteria(InvestasiPrasaranaPerguruanTinggiSapto.class)
                        .addOrder(Order.asc("nama")).addOrder(Order.asc("id")).list();

                    int index = 1;
                    for (InvestasiPrasaranaPerguruanTinggiSapto inv : list) {
                        List sub = new ArrayList();
                        sub.add("");
                        sub.add(index);
                        sub.add(inv.getNama());
                        sub.add(inv.getNilaiInventasiSelama3TahunTerakhir());
                        sub.add(inv.getRencanaInventasi());
                        sub.add(inv.getSumberDana());
                        datas.add(sub);
                        index++;
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
