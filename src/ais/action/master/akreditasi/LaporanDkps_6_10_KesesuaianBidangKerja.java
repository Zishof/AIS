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
 * DKPS 2.0 Instrumen 6.10 — Kesesuaian Bidang Kerja Lulusan dengan Kompetensi PS.
 *
 * <p>Instrumen ini mengukur persentase lulusan yang bekerja di bidang yang sesuai
 * atau relevan dengan kompetensi program studi. Data ini mencerminkan relevansi
 * kurikulum dengan kebutuhan dunia kerja dan kemampuan program studi menghasilkan
 * lulusan yang kompetitif di bidangnya.</p>
 *
 * <p>Kesesuaian bidang kerja dibagi menjadi tiga kategori: sesuai langsung
 * (bidang kerja identik), sesuai tidak langsung (masih dalam rumpun ilmu), dan
 * tidak sesuai. BAN-PT menilai baik jika ≥60% lulusan bekerja di bidang yang
 * sesuai (langsung maupun tidak langsung).</p>
 *
 * <p>Sumber data: tabel tracer_study dengan kolom kesesuaian_bidang. Jika tidak
 * tersedia, ditampilkan template kosong sebagai panduan pengisian manual.</p>
 */
public class LaporanDkps_6_10_KesesuaianBidangKerja extends AkreditasiBaseWindow {

    public static final String sheetCode = "DKPS-6.10";
    private static final long serialVersionUID = 1L;

    public LaporanDkps_6_10_KesesuaianBidangKerja() {
        super();
        try {
            initFakultasJurusan();
            buildBase(false);
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
        }
    }

    public LaporanDkps_6_10_KesesuaianBidangKerja(String title, String border, boolean closable) throws Exception {
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
            final Label label = new Label(ais.common.Common.getBahasaConfig("Memuat data kesesuaian bidang kerja ..."));

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

                        // DKPS-6.10: Tahun Lulus, Jml Lulusan, Terlacak, Rendah, Sedang, Tinggi
                        boolean dataLoaded = false;
                        try {
                            String sql = "SELECT EXTRACT(year FROM m.tanggal_lulus) as tahun,"
                                + " COUNT(*) as total_lulusan,"
                                + " COUNT(ts.id) as terlacak,"
                                + " COUNT(CASE WHEN LOWER(COALESCE(ts.kesesuaian_bidang,''))='rendah' THEN 1 END) as rendah,"
                                + " COUNT(CASE WHEN LOWER(COALESCE(ts.kesesuaian_bidang,''))='sedang' THEN 1 END) as sedang,"
                                + " COUNT(CASE WHEN LOWER(COALESCE(ts.kesesuaian_bidang,''))='tinggi' THEN 1 END) as tinggi"
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
                        } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/akreditasi/LaporanDkps_6_10_KesesuaianBidangKerja.java:102");}

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
                        ex.printStackTrace(); ais.common.ErrorAuditUtil.record(ex, "auto-audit src/ais/action/master/akreditasi/LaporanDkps_6_10_KesesuaianBidangKerja.java:117");
                        label.setValue("");
                    } finally {
                        if (session != null) {
                            try { session.disconnect(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/akreditasi/LaporanDkps_6_10_KesesuaianBidangKerja.java:121");}
                            try { session.close(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/akreditasi/LaporanDkps_6_10_KesesuaianBidangKerja.java:122");}
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
