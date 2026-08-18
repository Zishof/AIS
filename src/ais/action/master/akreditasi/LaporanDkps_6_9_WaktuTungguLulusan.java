package ais.action.master.akreditasi;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Jurusan;

/**
 * DKPS 2.0 Instrumen 6.9 — Waktu Tunggu Lulusan Mendapatkan Pekerjaan.
 *
 * <p>Instrumen ini menampilkan hasil tracer study mengenai lamanya waktu yang
 * dibutuhkan lulusan program studi untuk mendapatkan pekerjaan pertama setelah
 * yudisium. Waktu tunggu yang pendek (di bawah 3 bulan) menandakan bahwa
 * lulusan memiliki kompetensi yang dibutuhkan dunia kerja dan program studi
 * berhasil membangun jaringan dengan industri dan institusi pengguna.</p>
 *
 * <p>BAN-PT memberikan nilai terbaik jika lebih dari 50% lulusan mendapatkan
 * pekerjaan dalam waktu kurang dari 3 bulan setelah yudisium. Waktu tunggu
 * rata-rata di bawah 6 bulan juga dinilai baik dalam instrumen LAMDIK 2.0.</p>
 *
 * <p>Sumber data: tabel tracer_study atau alumni_survey jika tersedia di sistem.
 * Jika tidak, ditampilkan template kosong sebagai panduan pengisian manual
 * dari hasil tracer study yang dilakukan secara offline atau via Google Form.</p>
 */
public class LaporanDkps_6_9_WaktuTungguLulusan extends AkreditasiBaseWindow {

    public static final String sheetCode = "DKPS-6.9";
    private static final long serialVersionUID = 1L;

    public LaporanDkps_6_9_WaktuTungguLulusan() {
        super();
        try {
            initFakultasJurusan();
            buildBase(false);
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
        }
    }

    public LaporanDkps_6_9_WaktuTungguLulusan(String title, String border, boolean closable) throws Exception {
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
    public void onCetak(final Event event) {
        clearContent();
        try {
            final Jurusan selectedJurusan = getSelectedJurusan();
            final Label label = new Label(ais.common.Common.getBahasaConfig("Memuat data waktu tunggu lulusan ..."));

            new Thread(new Runnable() {
                @Override
                public void run() {
                    Session session = null;
                    try {
                        session = HibernateUtil.currentNativeSession();
                        List<List> datas = new ArrayList<List>();
                        for (int i = 0; i < 5; i++) datas.add(new ArrayList());

                        String jurusanFilter = selectedJurusan != null
                            ? " AND j.id = " + selectedJurusan.getId() : "";

                        // DKPS-6.9: Tahun Lulus, Jml Lulusan, Terlacak, WT<6 Bulan, 6<=WT<=12, WT>12
                        boolean dataLoaded = false;
                        try {
                            String sql = "SELECT EXTRACT(year FROM m.tanggal_lulus) as tahun,"
                                + " COUNT(*) as total_lulusan,"
                                + " COUNT(ts.id) as terlacak,"
                                + " COUNT(CASE WHEN ts.waktu_tunggu_bulan < 6 THEN 1 END) as lt6,"
                                + " COUNT(CASE WHEN ts.waktu_tunggu_bulan BETWEEN 6 AND 12 THEN 1 END) as b6_12,"
                                + " COUNT(CASE WHEN ts.waktu_tunggu_bulan > 12 THEN 1 END) as gt12"
                                + " FROM mahasiswa m"
                                + " INNER JOIN jurusan j ON m.jurusan=j.id"
                                + " LEFT JOIN tracer_study ts ON ts.mahasiswa=m.id"
                                + " WHERE m.tanggal_lulus IS NOT NULL" + jurusanFilter
                                + " GROUP BY EXTRACT(year FROM m.tanggal_lulus)"
                                + " ORDER BY tahun DESC LIMIT 5";
                            List rows = session.createSQLQuery(sql).list();
                            for (Object row : rows) {
                                Object[] r = (Object[]) row;
                                List sub = new ArrayList();
                                for (Object o : r) sub.add(o == null ? "0" : o.toString());
                                datas.add(sub);
                            }
                            dataLoaded = !rows.isEmpty();
                        } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/akreditasi/LaporanDkps_6_9_WaktuTungguLulusan.java:103");}

                        if (!dataLoaded) {
                            int tahun = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR);
                            for (int y = tahun - 4; y <= tahun; y++) {
                                List sub = new ArrayList();
                                sub.add(y); sub.add(0); sub.add(0); sub.add(0); sub.add(0); sub.add(0);
                                datas.add(sub);
                            }
                        }

                        datas.add(new ArrayList());
                        label.setAttribute("datas", datas);
                        label.setValue("");
                    } catch (Exception ex) {
                        ex.printStackTrace(); ais.common.ErrorAuditUtil.record(ex, "auto-audit src/ais/action/master/akreditasi/LaporanDkps_6_9_WaktuTungguLulusan.java:118");
                        label.setValue("");
                    } finally {
                        if (session != null) {
                            try { session.disconnect(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/akreditasi/LaporanDkps_6_9_WaktuTungguLulusan.java:122");}
                            try { session.close(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/akreditasi/LaporanDkps_6_9_WaktuTungguLulusan.java:123");}
                        }
                        HibernateUtil.closeSession();
                    }
                }
            }).start();

            display(label, 6);
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
        }
    }
}
