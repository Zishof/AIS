package ais.action.master.sapto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;

import ais.action.master.sapto.util.SaptoUtil;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.Jurusan;
import ais.database.model.Ruang;

public class LaporanRuangDosen_A_6_3_1 extends SaptoBaseWindow {

    public static final String sheetCode = "A-6.3.1";
    private static final long serialVersionUID = 3331244819198611604L;
    public LaporanRuangDosen_A_6_3_1() {
        super();
        try {
            initFakultasJurusan();
            buildBase(false);
        } catch (Exception e) { Common.tampilErrorJikaAdmin(e); }
    }

    public LaporanRuangDosen_A_6_3_1(String title, String border, boolean closable) throws Exception {
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

            final Label label = new Label(ais.common.Common.getBahasaConfig("Proses load data .."));

            new Thread(new Runnable() {
                @Override
                public void run() {
                	try {
                    Session session = HibernateUtil.currentNativeSession();
                    List<List> datas = new ArrayList<List>();
                    for (int i = 0; i < 6; i++) datas.add(new ArrayList());

                    List<Object[]> dosenRuangan = session.createCriteria(Dosen.class)
                        .createAlias("ruang","ruang").add(Restrictions.isNotNull("ruang"))
                        .setProjection(Projections.projectionList()
                            .add(Projections.groupProperty("ruang")).add(Projections.rowCount()))
                        .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                        .add(selectedJurusan == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("jurusan", selectedJurusan))
                        .addOrder(Order.asc("ruang")).list();

                    Map<Integer, Double[]> ruangs = new HashMap<Integer, Double[]>();
                    for (Object[] objects : dosenRuangan) {
                        Ruang ruang = (Ruang) objects[0];
                        Number count = (Number) objects[1];
                        if (ruang != null && count != null && count.intValue() > 0) {
                            Integer banyakDosen = count.intValue();
                            if (banyakDosen > 4) banyakDosen = 4;
                            Double luas = 0.0, banyak = 0.0;
                            if (ruangs.containsKey(banyakDosen)) {
                                Double[] d = ruangs.get(banyakDosen);
                                luas = d[1]; banyak = d[0];
                            }
                            banyak += 1.0;
                            luas += ruang.getLuas();
                            ruangs.put(banyakDosen, new Double[]{banyak, luas});
                        }
                    }

                    for (int i = 4; i >= 1; i--) {
                        List sub = new ArrayList();
                        sub.add(""); sub.add(""); sub.add(""); sub.add("");
                        sub.add(ruangs.get(i) == null ? 0 : ruangs.get(i)[0].intValue());
                        sub.add(ruangs.get(i) == null ? 0 : ruangs.get(i)[1].intValue());
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
