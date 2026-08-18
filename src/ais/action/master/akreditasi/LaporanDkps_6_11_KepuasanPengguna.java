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
 * DKPS 2.0 Instrumen 6.11 — Kepuasan Pengguna Lulusan (Tracer Study Pengguna).
 *
 * <p>Instrumen ini merekap hasil survei kepuasan kepada pengguna lulusan
 * (atasan, HRD, atau supervisor langsung di tempat kerja) mengenai kualitas
 * kinerja lulusan program studi. Survei ini mengukur aspek: etika dan integritas,
 * keahlian teknis bidang ilmu, kemampuan komunikasi, kemampuan kerja sama tim,
 * kemampuan berpikir kritis, dan kemampuan pengembangan diri.</p>
 *
 * <p>Kepuasan pengguna merupakan indikator eksternal yang paling langsung
 * mencerminkan relevansi dan kualitas lulusan. BAN-PT mensyaratkan tingkat
 * kepuasan rata-rata ≥3,0 dari skala 4,0 untuk akreditasi Baik, dan ≥3,5
 * untuk akreditasi Unggul.</p>
 *
 * <p>Sumber data: tabel kepuasan_pengguna atau survei_pengguna jika tersedia.
 * Jika tidak ada, ditampilkan template dengan 6 aspek penilaian standar
 * yang perlu diisi secara manual dari hasil survei offline.</p>
 */
public class LaporanDkps_6_11_KepuasanPengguna extends AkreditasiBaseWindow {

    public static final String sheetCode = "DKPS-6.11";
    private static final long serialVersionUID = 1L;

    public LaporanDkps_6_11_KepuasanPengguna() {
        super();
        try {
            initFakultasJurusan();
            buildBase(false);
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
        }
    }

    public LaporanDkps_6_11_KepuasanPengguna(String title, String border, boolean closable) throws Exception {
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
            final Label label = new Label(ais.common.Common.getBahasaConfig("Memuat data kepuasan pengguna lulusan ..."));

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

                        boolean dataLoaded = false;
                        try {
                            String sql = "SELECT kp.aspek,"
                                + " ROUND(100.0*COUNT(CASE WHEN kp.skor=4 THEN 1 END)/NULLIF(COUNT(*),0),1) as pct_sangat_baik,"
                                + " ROUND(100.0*COUNT(CASE WHEN kp.skor=3 THEN 1 END)/NULLIF(COUNT(*),0),1) as pct_baik,"
                                + " ROUND(100.0*COUNT(CASE WHEN kp.skor=2 THEN 1 END)/NULLIF(COUNT(*),0),1) as pct_cukup,"
                                + " ROUND(100.0*COUNT(CASE WHEN kp.skor=1 THEN 1 END)/NULLIF(COUNT(*),0),1) as pct_kurang"
                                + " FROM kepuasan_pengguna kp"
                                + " INNER JOIN mahasiswa m ON kp.mahasiswa=m.id"
                                + " INNER JOIN jurusan j ON m.jurusan=j.id"
                                + " WHERE kp.aspek IS NOT NULL" + jurusanFilter
                                + " GROUP BY kp.aspek ORDER BY kp.aspek";
                            List rows = session.createSQLQuery(sql).list();
                            int noUrut = 1;
                            for (Object row : rows) {
                                Object[] r = (Object[]) row;
                                List sub = new ArrayList();
                                sub.add(noUrut++);
                                for (Object o : r) sub.add(o == null ? 0 : o.toString());
                                sub.add(""); // Rencana Tindak Lanjut
                                datas.add(sub);
                            }
                            dataLoaded = !rows.isEmpty();
                        } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/akreditasi/LaporanDkps_6_11_KepuasanPengguna.java:104");}

                        if (!dataLoaded) {
                            String[] aspek = {
                                "Etika dan Integritas",
                                "Keahlian Teknis Bidang Ilmu",
                                "Kemampuan Bahasa Asing",
                                "Kemampuan Komunikasi",
                                "Kemampuan Kerja Sama Tim",
                                "Kemampuan Pengembangan Diri",
                                "Kemampuan Berpikir Kritis"
                            };
                            for (int i = 0; i < aspek.length; i++) {
                                List sub = new ArrayList();
                                sub.add(i + 1); sub.add(aspek[i]);
                                sub.add(0); sub.add(0); sub.add(0); sub.add(0);
                                sub.add(""); // Rencana Tindak Lanjut
                                datas.add(sub);
                            }
                        }

                        datas.add(new ArrayList());
                        label.setAttribute("datas", datas);
                        label.setValue("");
                    } catch (Exception ex) {
                        ex.printStackTrace(); ais.common.ErrorAuditUtil.record(ex, "auto-audit src/ais/action/master/akreditasi/LaporanDkps_6_11_KepuasanPengguna.java:129");
                        label.setValue("");
                    } finally {
                        if (session != null) {
                            try { session.disconnect(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/akreditasi/LaporanDkps_6_11_KepuasanPengguna.java:133");}
                            try { session.close(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/akreditasi/LaporanDkps_6_11_KepuasanPengguna.java:134");}
                        }
                        HibernateUtil.closeSession();
                    }
                }
            }).start();

            display(label, 7);
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
        }
    }
}
