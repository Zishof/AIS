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
 * DKPS Tabel 3.3-4 — Karya Inovatif Mahasiswa: Publikasi di Jurnal Nasional dan Internasional.
 *
 * <p>Menampilkan daftar artikel ilmiah yang dipublikasikan mahasiswa program studi di
 * jurnal nasional (terakreditasi SINTA) dan jurnal internasional (terindeks Scopus/WoS)
 * selama periode evaluasi akreditasi LAMDIK 2.0. Data mencakup identitas mahasiswa
 * (NIM dan nama), judul artikel, nama jurnal, tahun terbit, level akreditasi jurnal
 * (SINTA 1-6 atau Scopus/WoS Q1-Q4), dan keterangan tambahan.</p>
 *
 * <p>Instrumen ini merupakan bagian dari Kriteria 3 (Mahasiswa) akreditasi LAMDIK yang
 * menilai kualitas luaran karya ilmiah mahasiswa. Publikasi di jurnal internasional
 * terindeks dan jurnal nasional terakreditasi SINTA 1-2 merupakan indikator unggulan
 * yang memberikan nilai signifikan dalam penilaian akreditasi.</p>
 *
 * <p>Kolom Level mencakup: SINTA 1, SINTA 2, SINTA 3, SINTA 4, SINTA 5, SINTA 6 untuk
 * jurnal nasional, serta Scopus (Q1/Q2/Q3/Q4) dan WoS untuk jurnal internasional.
 * Keterangan dapat berisi informasi tambahan seperti DOI, volume, nomor, dan halaman.</p>
 *
 * <h3>Sumber data</h3>
 * <p>Data diambil dari tabel {@code publikasi_mahasiswa} dengan filter
 * {@code jenis LIKE '%JURNAL%'}. Jika tabel belum tersedia, ditampilkan lima baris
 * template kosong sebagai panduan pengisian manual.</p>
 *
 * <h3>Manajemen sesi</h3>
 * <p>Background thread menggunakan {@code currentNativeSession()} dengan
 * penutupan wajib di {@code finally}. Tidak menggunakan multi-catch (Java 1.7).</p>
 *
 * @author AIS Development Team
 * @version 2.0
 * @see AkreditasiBaseWindow
 */
public class LaporanDkps_3_3_4_PublikasiJurnalMhs extends AkreditasiBaseWindow {

    /** Kode sheet untuk konfigurasi grid, grafik, dan Excel template. */
    public static final String sheetCode = "DKPS-3.3-4";
    private static final long serialVersionUID = 1L;

    /**
     * Konstruktor default — menginisialisasi filter Fakultas/Program Studi
     * lalu membangun tata letak tanpa auto-load data.
     */
    public LaporanDkps_3_3_4_PublikasiJurnalMhs() {
        super();
        try {
            initFakultasJurusan();
            buildBase(false);
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
        }
    }

    /**
     * Konstruktor dengan parameter tampilan jendela.
     *
     * @param title    judul jendela
     * @param border   gaya border ZK
     * @param closable {@code true} jika jendela dapat ditutup
     * @throws Exception jika ZK gagal membuat komponen
     */
    public LaporanDkps_3_3_4_PublikasiJurnalMhs(String title, String border, boolean closable) throws Exception {
        super(title, border, closable);
        initFakultasJurusan();
        buildBase(false);
    }

    /** @return kode sheet {@value #sheetCode} */
    @Override
    protected String getSheetCode() {
        return sheetCode;
    }

    /**
     * Membangun baris filter: Fakultas dan Program Studi.
     * Perubahan nilai filter otomatis memicu {@link #onCetak}.
     */
    @Override
    protected void buildFilters(Row row) {
        addFakultasJurusanFilter(row);
    }

    /**
     * Memuat data publikasi jurnal mahasiswa dari database di background thread.
     *
     * <p>Query mengambil data dari tabel {@code publikasi_mahasiswa} dengan filter
     * {@code jenis LIKE '%JURNAL%'}. Kolom level SINTA/Scopus diambil dari kolom
     * {@code level} atau {@code akreditasi}. Jika query gagal karena tabel belum
     * ada, ditampilkan lima baris template kosong.</p>
     *
     * @param event event ZK atau {@code null} jika dipanggil secara programatik
     */
    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void onCetak(final Event event) {
        clearContent();
        try {
            final Jurusan selectedJurusan = getSelectedJurusan();
            final Label label = new Label(ais.common.Common.getBahasaConfig("Memuat data ..."));

            new Thread(new Runnable() {
                @Override
                public void run() {
                    Session session = null;
                    try {
                        session = HibernateUtil.currentNativeSession();

                        List<List> datas = new ArrayList<List>();
                        for (int i = 0; i < 5; i++) {
                            datas.add(new ArrayList());
                        }

                        String jurusanWhere = "";
                        if (selectedJurusan != null) {
                            jurusanWhere = " AND j.id=" + selectedJurusan.getId();
                        }

                        boolean queryBerhasil = false;

                        // Coba kolom level
                        try {
                            String sql = "SELECT m.nim, m.nama, p.judul, p.nama_jurnal, "
                                + "p.tahun, p.level, p.keterangan "
                                + "FROM publikasi_mahasiswa p "
                                + "INNER JOIN mahasiswa m ON p.mahasiswa=m.id "
                                + "INNER JOIN jurusan j ON m.jurusan=j.id "
                                + "WHERE UPPER(p.jenis) LIKE '%JURNAL%'"
                                + jurusanWhere
                                + " ORDER BY p.tahun DESC, m.nama";
                            List<Object[]> rows = session.createSQLQuery(sql).list();
                            int no = 1;
                            for (Object[] r : rows) {
                                String nimNama = (r[0] != null ? r[0].toString() : "")
                                    + " - " + (r[1] != null ? r[1].toString() : "");
                                String judul = r[2] != null ? r[2].toString() : "";
                                String namaJurnal = r[3] != null ? r[3].toString() : "";
                                String tahun = r[4] != null ? r[4].toString() : "";
                                String level = r[5] != null ? r[5].toString() : "";
                                String keterangan = r[6] != null ? r[6].toString() : "";

                                List sub = new ArrayList();
                                sub.add(no++);
                                sub.add(nimNama);
                                sub.add(judul);
                                sub.add(namaJurnal);
                                sub.add(tahun);
                                sub.add(level);
                                sub.add(keterangan);
                                datas.add(sub);
                            }
                            queryBerhasil = true;
                        } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/akreditasi/LaporanDkps_3_3_4_PublikasiJurnalMhs.java:164");
                        }

                        // Fallback kolom akreditasi
                        if (!queryBerhasil) {
                            try {
                                String sql = "SELECT m.nim, m.nama, p.judul, p.nama_jurnal, "
                                    + "p.tahun, p.akreditasi, p.keterangan "
                                    + "FROM publikasi_mahasiswa p "
                                    + "INNER JOIN mahasiswa m ON p.mahasiswa=m.id "
                                    + "INNER JOIN jurusan j ON m.jurusan=j.id "
                                    + "WHERE UPPER(p.jenis) LIKE '%JURNAL%'"
                                    + jurusanWhere
                                    + " ORDER BY p.tahun DESC, m.nama";
                                List<Object[]> rows = session.createSQLQuery(sql).list();
                                int no = 1;
                                for (Object[] r : rows) {
                                    String nimNama = (r[0] != null ? r[0].toString() : "")
                                        + " - " + (r[1] != null ? r[1].toString() : "");
                                    String judul = r[2] != null ? r[2].toString() : "";
                                    String namaJurnal = r[3] != null ? r[3].toString() : "";
                                    String tahun = r[4] != null ? r[4].toString() : "";
                                    String level = r[5] != null ? r[5].toString() : "";
                                    String keterangan = r[6] != null ? r[6].toString() : "";

                                    List sub = new ArrayList();
                                    sub.add(no++);
                                    sub.add(nimNama);
                                    sub.add(judul);
                                    sub.add(namaJurnal);
                                    sub.add(tahun);
                                    sub.add(level);
                                    sub.add(keterangan);
                                    datas.add(sub);
                                }
                                queryBerhasil = true;
                            } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/akreditasi/LaporanDkps_3_3_4_PublikasiJurnalMhs.java:200");
                            }
                        }

                        if (!queryBerhasil) {
                            for (int i = 1; i <= 5; i++) {
                                List sub = new ArrayList();
                                sub.add(i);
                                sub.add(""); sub.add(""); sub.add("");
                                sub.add(""); sub.add(""); sub.add("");
                                datas.add(sub);
                            }
                        }

                        label.setAttribute("datas", datas);
                        label.setValue("");
                    } catch (Exception ex) {
                        ex.printStackTrace(); ais.common.ErrorAuditUtil.record(ex, "auto-audit src/ais/action/master/akreditasi/LaporanDkps_3_3_4_PublikasiJurnalMhs.java:217");
                        label.setValue("");
                    } finally {
                        if (session != null) {
                            try { session.disconnect(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/akreditasi/LaporanDkps_3_3_4_PublikasiJurnalMhs.java:221");}
                            try { session.close(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/akreditasi/LaporanDkps_3_3_4_PublikasiJurnalMhs.java:222");}
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
