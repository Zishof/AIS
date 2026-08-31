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

/**
 * Laporan SAPTO/borang akreditasi BAN-PT butir A.6.3.1 (kode sheet {@code A-6.3.1}): rekap
 * ketersediaan ruang kerja dosen tetap aktif, dikelompokkan berdasarkan tingkat kepadatan
 * pemakaian ruang — jumlah dosen yang berbagi satu {@link Ruang} yang sama, dibatasi maksimum 4
 * (lebih dari 4 dosen per ruang tetap dihitung pada kelompok "4"), difilter opsional per
 * fakultas/jurusan. Untuk setiap kelompok kepadatan (4 dosen/ruang turun sampai 1 dosen/ruang)
 * ditampilkan jumlah ruang pada kelompok tersebut dan total luas gabungannya. Data dimuat asinkron
 * lalu dirender lewat {@link SaptoUtil#displayWorksheet}.
 */
public class LaporanRuangDosen_A_6_3_1 extends SaptoBaseWindow {

    public static final String sheetCode = "A-6.3.1";
    private static final long serialVersionUID = 3331244819198611604L;
    /** Membangun jendela laporan dengan filter fakultas/jurusan siap pakai. */
    public LaporanRuangDosen_A_6_3_1() {
        super();
        try {
            initFakultasJurusan();
            buildBase(false);
        } catch (Exception e) { Common.tampilErrorJikaAdmin(e); }
    }

    /** Membangun jendela laporan dengan judul/border/closable kustom. */
    public LaporanRuangDosen_A_6_3_1(String title, String border, boolean closable) throws Exception {
        super(title, border, closable);
        initFakultasJurusan();
        buildBase(false);
    }

    /** @return kode sheet borang {@code "A-6.3.1"}. */
    @Override protected String getSheetCode() { return sheetCode; }

    /** Membangun baris filter fakultas/jurusan lewat {@link #addFakultasJurusanFilter}. */
    @Override
    protected void buildFilters(Row row) {
        addFakultasJurusanFilter(row);
    }

    /**
     * Menghitung dan menampilkan rekap ruang kerja dosen per tingkat kepadatan (1-4+ dosen per
     * ruang), difilter jurusan bila dipilih. Data dimuat asinkron di thread terpisah dan dirender
     * lewat {@link SaptoUtil#displayWorksheet}.
     *
     * @param event event pemicu (perubahan filter), boleh {@code null}
     */
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
