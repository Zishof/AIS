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
import ais.database.model.sapto.LahanPerguruanTinggiSapto;

/**
 * Laporan borang akreditasi BAN-PT butir A-6.2.2 (data lahan institusi): menampilkan seluruh
 * {@link LahanPerguruanTinggiSapto} (lokasi, jenis kepemilikan, nama, luas), terurut lokasi lalu id,
 * tanpa filter apa pun (layar ini tidak menyediakan filter, lihat {@link #buildFilters}). Data
 * dimuat di thread terpisah dan dirender ke worksheet {@link #sheetCode} ("A-6.2.2_PT") lewat
 * {@link SaptoUtil#displayWorksheet}.
 */
public class LaporanLahanInstitusi_A_6_2_2 extends SaptoBaseWindow {

    public static final String sheetCode = "A-6.2.2_PT";
    private static final long serialVersionUID = 3331244819198611604L;

    /** Konstruktor default: membangun kerangka jendela dan langsung memuat data (tanpa filter). */
    public LaporanLahanInstitusi_A_6_2_2() {
        super();
        try { buildBase(true); } catch (Exception e) { Common.tampilErrorJikaAdmin(e); }
    }

    /** Konstruktor dengan judul/border/closable eksplisit; kegagalan inisialisasi dilempar ke pemanggil. */
    public LaporanLahanInstitusi_A_6_2_2(String title, String border, boolean closable) throws Exception {
        super(title, border, closable);
        buildBase(true);
    }

    /** @return kode sheet template worksheet borang, {@link #sheetCode}. */
    @Override protected String getSheetCode() { return sheetCode; }
    /** Tidak menambahkan filter apa pun — laporan ini selalu menampilkan seluruh data lahan. */
    @Override protected void buildFilters(Row row) { /* no filters */ }

    /** Handler cetak: memuat seluruh {@link LahanPerguruanTinggiSapto} di thread terpisah, lalu menampilkannya lewat {@link SaptoUtil#displayWorksheet}. */
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

                    List<LahanPerguruanTinggiSapto> list = session
                        .createCriteria(LahanPerguruanTinggiSapto.class)
                        .addOrder(Order.asc("lokasi")).addOrder(Order.asc("id")).list();

                    int index = 1;
                    for (LahanPerguruanTinggiSapto l : list) {
                        List sub = new ArrayList();
                        sub.add("");
                        sub.add(index);
                        sub.add(l.getLokasi());
                        sub.add(l.getJenisKepemilikan());
                        sub.add(l.getNama());
                        sub.add(l.getLuas());
                        datas.add(sub);
                        index++;
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
