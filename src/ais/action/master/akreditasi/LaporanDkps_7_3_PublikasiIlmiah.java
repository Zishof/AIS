package ais.action.master.akreditasi;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Jurusan;

/**
 * DKPS 2.0 Instrumen 7.3 — Publikasi Ilmiah DTPS.
 *
 * <p>Instrumen ini merekap jumlah artikel ilmiah yang diterbitkan oleh Dosen
 * Tetap Program Studi (DTPS) selama 3 tahun terakhir, dikelompokkan berdasarkan
 * jenis publikasi: jurnal nasional tidak terakreditasi, jurnal nasional Sinta,
 * jurnal internasional, dan jurnal internasional terindeks Scopus/Web of Science.</p>
 *
 * <p>Publikasi ilmiah merupakan salah satu indikator utama kualitas riset program
 * studi. BAN-PT memberikan bobot signifikan pada kualitas dan kuantitas publikasi
 * DTPS, khususnya di jurnal terindeks internasional. Target ideal adalah minimal
 * satu artikel terindeks Scopus per dosen per dua tahun.</p>
 *
 * <p>Sumber data: tabel publikasi_dosen di skema epsbed atau public. Jika
 * tidak tersedia, ditampilkan template kosong. Data juga dapat berasal dari
 * rekap manual SINTA (Science and Technology Index) Kemdikbud.</p>
 */
public class LaporanDkps_7_3_PublikasiIlmiah extends AkreditasiBaseWindow {

    public static final String sheetCode = "DKPS-7.3";
    private static final long serialVersionUID = 1L;

    private Combobox tahunAjaran;

    public LaporanDkps_7_3_PublikasiIlmiah() {
        super();
        try {
            initFakultasJurusan();
            Common.selectComboItem(tahunAjaran = Common.generateTahunAjaran(tahunAjaran),
                Common.getCurrentTahunAkademik());
            buildBase(false);
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
        }
    }

    public LaporanDkps_7_3_PublikasiIlmiah(String title, String border, boolean closable) throws Exception {
        super(title, border, closable);
        initFakultasJurusan();
        Common.selectComboItem(tahunAjaran = Common.generateTahunAjaran(tahunAjaran),
            Common.getCurrentTahunAkademik());
        buildBase(false);
    }

    @Override
    protected String getSheetCode() { return sheetCode; }

    @Override
    protected void buildFilters(Row row) {
        addFakultasJurusanFilter(row);
        row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
        tahunAjaran.setWidth("90%");
        tahunAjaran.setReadonly(true);
        row.appendChild(tahunAjaran);
        tahunAjaran.addEventListener("onChange", new EventListener() {
            @Override
            public void onEvent(Event e) throws Exception { onCetak(null); }
        });
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void onCetak(final Event event) {
        clearContent();
        try {
            final Jurusan selectedJurusan = getSelectedJurusan();
            final String tahunStr = tahunAjaran.getSelectedItem() == null ? null
                : (String) tahunAjaran.getSelectedItem().getValue();
            final int tahun = tahunStr != null ? Integer.parseInt(tahunStr.split("/")[0])
                : java.util.Calendar.getInstance().get(java.util.Calendar.YEAR);
            final Label label = new Label(ais.common.Common.getBahasaConfig("Memuat data publikasi ilmiah ..."));

            new Thread(new Runnable() {
                @Override
                public void run() {
                    Session session = null;
                    try {
                        session = HibernateUtil.currentNativeSession();
                        List<List> datas = new ArrayList<List>();
                        for (int i = 0; i < 5; i++) datas.add(new ArrayList());

                        String jurusanFilter = selectedJurusan != null
                            ? " AND d.jurusan = " + selectedJurusan.getId() : "";

                        // Jenis publikasi DKPS 2.0
                        String[] jenisPublikasi = {
                            "Jurnal Nasional",
                            "Jurnal Nasional Terakreditasi (Sinta 1-6)",
                            "Jurnal Internasional",
                            "Jurnal Internasional Terindeks (Scopus/WoS)"
                        };
                        String[] jenisDb = { "NASIONAL", "NASIONAL_TERAKREDITASI", "INTERNASIONAL", "INTERNASIONAL_TERINDEKS" };

                        for (int j = 0; j < jenisPublikasi.length; j++) {
                            long ts2 = 0, ts1 = 0, ts = 0;
                            try {
                                String jenisVal = jenisDb[j];
                                String sqlBase = "SELECT COUNT(*) FROM epsbed.publikasi_dosen pd"
                                    + " INNER JOIN dosen d ON pd.dosen=d.id"
                                    + " WHERE pd.jenis='" + jenisVal + "'"
                                    + jurusanFilter;
                                Object r2 = session.createSQLQuery(sqlBase + " AND pd.tahun=" + (tahun - 2)).uniqueResult();
                                Object r1 = session.createSQLQuery(sqlBase + " AND pd.tahun=" + (tahun - 1)).uniqueResult();
                                Object r0 = session.createSQLQuery(sqlBase + " AND pd.tahun=" + tahun).uniqueResult();
                                ts2 = r2 == null ? 0L : ((Number) r2).longValue();
                                ts1 = r1 == null ? 0L : ((Number) r1).longValue();
                                ts  = r0 == null ? 0L : ((Number) r0).longValue();
                            } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/akreditasi/LaporanDkps_7_3_PublikasiIlmiah.java:124");}

                            List sub = new ArrayList();
                            sub.add(jenisPublikasi[j]);
                            sub.add(ts2);
                            sub.add(ts1);
                            sub.add(ts);
                            sub.add(ts2 + ts1 + ts);
                            datas.add(sub);
                        }

                        datas.add(new ArrayList());
                        label.setAttribute("datas", datas);
                        label.setValue("");
                    } catch (Exception ex) {
                        ex.printStackTrace(); ais.common.ErrorAuditUtil.record(ex, "auto-audit src/ais/action/master/akreditasi/LaporanDkps_7_3_PublikasiIlmiah.java:139");
                        label.setValue("");
                    } finally {
                        if (session != null) {
                            try { session.disconnect(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/akreditasi/LaporanDkps_7_3_PublikasiIlmiah.java:143");}
                            try { session.close(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/akreditasi/LaporanDkps_7_3_PublikasiIlmiah.java:144");}
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
