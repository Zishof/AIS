package ais.action.master.sapto;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;

import ais.action.master.sapto.util.SaptoUtil;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.PenghargaanDosen;

public class LaporanKaryaDosen_A_7_1_5 extends SaptoBaseWindow {

    public static final String sheetCode = "A-7.1.5_PT";
    private static final long serialVersionUID = 3331244819198611604L;
    private Combobox tahunAjaran;
    private Combobox tahunAjaran1;

    public LaporanKaryaDosen_A_7_1_5() {
        super();
        try {
            Common.selectComboItem(tahunAjaran = Common.generateTahunAjaran(tahunAjaran), Common.getCurrentTahunAkademik());
            Common.selectComboItem(tahunAjaran1 = Common.generateTahunAjaran(tahunAjaran1), Common.getCurrentTahunAkademik());
            buildBase(true);
        } catch (Exception e) { Common.tampilErrorJikaAdmin(e); }
    }

    public LaporanKaryaDosen_A_7_1_5(String title, String border, boolean closable) throws Exception {
        super(title, border, closable);
        Common.selectComboItem(tahunAjaran = Common.generateTahunAjaran(tahunAjaran), Common.getCurrentTahunAkademik());
        Common.selectComboItem(tahunAjaran1 = Common.generateTahunAjaran(tahunAjaran1), Common.getCurrentTahunAkademik());
        buildBase(true);
    }

    @Override protected String getSheetCode() { return sheetCode; }

    @Override
    protected void buildFilters(Row row) {
        row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik *"));
        tahunAjaran.setWidth("90%");
        tahunAjaran.setReadonly(true);
        row.appendChild(tahunAjaran);
        tahunAjaran.addEventListener("onChange", new EventListener() {
            @Override public void onEvent(Event e) throws Exception { onCetak(null); }
        });

        row.appendChild(new ais.ui.util.MyLabelConfig("s.d"));
        tahunAjaran1.setWidth("90%");
        tahunAjaran1.setReadonly(true);
        row.appendChild(tahunAjaran1);
        tahunAjaran1.addEventListener("onChange", new EventListener() {
            @Override public void onEvent(Event e) throws Exception { onCetak(null); }
        });
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void onCetak(Event event) {
        clearContent();
        try {
            final String tahunAkademik = (String) tahunAjaran.getSelectedItem().getValue();
            final String tahunAkademik1 = (String) tahunAjaran1.getSelectedItem().getValue();
            final Label label = new Label(ais.common.Common.getBahasaConfig("Proses load data .."));

            if (tahunAkademik == null) { label.setValue(""); return; }

            new Thread(new Runnable() {
                @Override
                public void run() {
                	try {
                    int tahun = Integer.parseInt(tahunAkademik.split("/")[0]);
                    int tahun1 = Integer.parseInt(tahunAkademik1.split("/")[0]);
                    Session session = HibernateUtil.currentNativeSession();
                    List<List> datas = new ArrayList<List>();
                    for (int i = 0; i < 8; i++) datas.add(new ArrayList());

                    List<PenghargaanDosen> list = session.createCriteria(PenghargaanDosen.class)
                        .add(Restrictions.between("tahun", tahun, tahun1))
                        .add(Restrictions.eq("status", PenghargaanDosen.DISETUJUI))
                        .addOrder(Order.asc("tanggal")).list();

                    int i = 1;
                    for (PenghargaanDosen p : list) {
                        List sub = new ArrayList();
                        sub.add(""); sub.add(i); sub.add(p.getNama());

                        String kategori = p.getKategoriPenghargaan() == null ? "" :
                            p.getKategoriPenghargaan().getNama().trim().toLowerCase();
                        if (kategori.contains("paten")) {
                            sub.add(""); sub.add(""); sub.add("V");
                        } else if (kategori.contains("haki")) {
                            sub.add(""); sub.add("V"); sub.add("");
                        } else {
                            sub.add("V"); sub.add(""); sub.add("");
                        }
                        sub.add(p.getTahun());
                        datas.add(sub);
                        i++;
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

            SaptoUtil.displayWorksheet(label, sheetCode, contentCenter, 8);
        } catch (Exception e) { Common.tampilErrorJikaAdmin(e); }
    }
}
