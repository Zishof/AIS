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
 * DKPS Tabel 4.6 — Pengembangan Kompetensi Tenaga Kependidikan.
 *
 * <p>Tabel ini mendokumentasikan kegiatan pengembangan kompetensi yang diikuti
 * oleh tenaga kependidikan (tendik) dalam tiga tahun terakhir. Pengembangan
 * tendik merupakan indikator penting dalam akreditasi LAMDIK 2.0 karena
 * mencerminkan komitmen institusi untuk meningkatkan kualitas layanan
 * administrasi dan teknis yang mendukung proses pembelajaran.</p>
 *
 * <p>Jenis pengembangan yang dimaksud meliputi: pelatihan kompetensi teknis
 * (pengoperasian sistem informasi, perpustakaan, laboratorium), pelatihan
 * layanan prima (service excellence), pendidikan dan pelatihan fungsional
 * (diklat PNS untuk tendik instansi pemerintah), sertifikasi kompetensi
 * profesi, maupun studi lanjut yang relevan dengan bidang tugasnya.</p>
 *
 * <p>Durasi kegiatan dicatat dalam satuan hari efektif pelaksanaan.
 * Tempat/penyelenggara mencakup lembaga diklat pemerintah, LSPP, atau
 * lembaga pelatihan swasta yang diakui kualifikasinya.</p>
 *
 * <h3>Sumber data</h3>
 * <p>Data dicoba diambil dari tabel {@code pelatihan_pegawai} atau
 * {@code pengembangan_sdm} dengan filter jabatan bukan dosen.
 * Jika tabel tidak tersedia, ditampilkan 3 baris template kosong
 * sebagai panduan pengisian manual oleh operator program studi.</p>
 *
 * <h3>Format baris (maxCols=8)</h3>
 * <ol>
 *   <li>No. Urut</li>
 *   <li>Nama Tendik</li>
 *   <li>Jenis Pengembangan</li>
 *   <li>Nama Kegiatan</li>
 *   <li>Tahun</li>
 *   <li>Tempat/Penyelenggara</li>
 *   <li>Durasi (Hari)</li>
 *   <li>Keterangan</li>
 * </ol>
 *
 * @author AIS Development Team
 * @version 2.0
 * @see AkreditasiBaseWindow
 */
public class LaporanDkps_4_6_PengembanganTendik extends AkreditasiBaseWindow {

    /** Kode sheet untuk konfigurasi grid, grafik, dan Excel template. */
    public static final String sheetCode = "DKPS-4.6";
    private static final long serialVersionUID = 1L;

    /**
     * Konstruktor default — menginisialisasi filter Fakultas/Program Studi
     * lalu membangun tata letak tanpa auto-load data.
     */
    public LaporanDkps_4_6_PengembanganTendik() {
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
    public LaporanDkps_4_6_PengembanganTendik(String title, String border, boolean closable) throws Exception {
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
     */
    @Override
    protected void buildFilters(Row row) {
        addFakultasJurusanFilter(row);
    }

    /**
     * Memuat data pengembangan tendik dari database di background thread.
     *
     * <p>Mencoba query dari tabel {@code pelatihan_pegawai} atau
     * {@code pengembangan_sdm} dengan filter jabatan bukan dosen.
     * Jika tidak ada data, ditampilkan 3 baris template kosong.</p>
     *
     * @param event event ZK atau {@code null} jika dipanggil secara programatik
     */
    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void onCetak(final Event event) {
        clearContent();
        try {
            final Jurusan selectedJurusan = getSelectedJurusan();
            final Label label = new Label(ais.common.Common.getBahasaConfig("Memuat data pengembangan tendik ..."));

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

                        boolean loaded = false;

                        // Coba tabel pelatihan_pegawai
                        try {
                            String checkSql = "SELECT COUNT(*) FROM information_schema.tables "
                                + "WHERE table_schema='public' AND table_name='pelatihan_pegawai'";
                            Object cnt = session.createSQLQuery(checkSql).uniqueResult();
                            boolean tableExists = cnt != null && Long.parseLong(cnt.toString()) > 0;

                            if (tableExists) {
                                String sql = "SELECT p.nama AS nama_tendik, pp.jenis_pengembangan,"
                                    + " pp.nama_kegiatan, pp.tahun, pp.tempat, pp.durasi_hari,"
                                    + " COALESCE(pp.keterangan, '') AS keterangan"
                                    + " FROM pelatihan_pegawai pp"
                                    + " INNER JOIN pegawai p ON pp.pegawai = p.id"
                                    + " WHERE LOWER(COALESCE(p.jabatan,'')) NOT LIKE '%dosen%'"
                                    + " ORDER BY pp.tahun DESC, p.nama"
                                    + " LIMIT 50";
                                List<Object[]> rows = session.createSQLQuery(sql).list();
                                int noUrut = 1;
                                for (Object[] obj : rows) {
                                    List sub = new ArrayList();
                                    sub.add(noUrut);
                                    sub.add(obj[0] != null ? obj[0].toString() : "");
                                    sub.add(obj[1] != null ? obj[1].toString() : "");
                                    sub.add(obj[2] != null ? obj[2].toString() : "");
                                    sub.add(obj[3] != null ? obj[3].toString() : "");
                                    sub.add(obj[4] != null ? obj[4].toString() : "");
                                    sub.add(obj[5] != null ? obj[5].toString() : "");
                                    datas.add(sub);
                                    noUrut++;
                                }
                                loaded = rows != null && rows.size() > 0;
                            }
                        } catch (Exception exQ1) {
                            exQ1.printStackTrace(); ais.common.ErrorAuditUtil.record(exQ1, "auto-audit src/ais/action/master/akreditasi/LaporanDkps_4_6_PengembanganTendik.java:168");
                        }

                        // Coba tabel pengembangan_sdm bila pelatihan_pegawai kosong
                        if (!loaded) {
                            try {
                                String checkSql2 = "SELECT COUNT(*) FROM information_schema.tables "
                                    + "WHERE table_schema='public' AND table_name='pengembangan_sdm'";
                                Object cnt2 = session.createSQLQuery(checkSql2).uniqueResult();
                                boolean tableExists2 = cnt2 != null && Long.parseLong(cnt2.toString()) > 0;

                                if (tableExists2) {
                                    String sql2 = "SELECT ps.nama_peserta, ps.jenis_kegiatan,"
                                        + " ps.nama_kegiatan, ps.tahun, ps.penyelenggara,"
                                        + " ps.durasi_hari, COALESCE(ps.keterangan,'') AS ket"
                                        + " FROM pengembangan_sdm ps"
                                        + " WHERE LOWER(COALESCE(ps.jabatan,'')) NOT LIKE '%dosen%'"
                                        + " ORDER BY ps.tahun DESC, ps.nama_peserta"
                                        + " LIMIT 50";
                                    List<Object[]> rows2 = session.createSQLQuery(sql2).list();
                                    int noUrut = 1;
                                    for (Object[] obj : rows2) {
                                        List sub = new ArrayList();
                                        sub.add(noUrut);
                                        sub.add(obj[0] != null ? obj[0].toString() : "");
                                        sub.add(obj[1] != null ? obj[1].toString() : "");
                                        sub.add(obj[2] != null ? obj[2].toString() : "");
                                        sub.add(obj[3] != null ? obj[3].toString() : "");
                                        sub.add(obj[4] != null ? obj[4].toString() : "");
                                        sub.add(obj[5] != null ? obj[5].toString() : "");
                                        datas.add(sub);
                                        noUrut++;
                                    }
                                    loaded = rows2 != null && rows2.size() > 0;
                                }
                            } catch (Exception exQ2) {
                                exQ2.printStackTrace(); ais.common.ErrorAuditUtil.record(exQ2, "auto-audit src/ais/action/master/akreditasi/LaporanDkps_4_6_PengembanganTendik.java:204");
                            }
                        }

                        if (!loaded) {
                            // Template 3 baris kosong panduan pengisian manual
                            for (int i = 1; i <= 3; i++) {
                                List sub = new ArrayList();
                                sub.add(i);
                                sub.add(""); // Nama Tendik
                                sub.add(""); // Jenis Pengembangan
                                sub.add(""); // Tempat
                                sub.add(""); // Waktu
                                sub.add(""); // Manfaat
                                sub.add(""); // Bukti
                                datas.add(sub);
                            }
                        }

                        label.setAttribute("datas", datas);
                        label.setValue("");
                    } catch (Exception ex) {
                        ex.printStackTrace(); ais.common.ErrorAuditUtil.record(ex, "auto-audit src/ais/action/master/akreditasi/LaporanDkps_4_6_PengembanganTendik.java:226");
                        label.setValue("");
                    } finally {
                        if (session != null) {
                            try { session.disconnect(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/akreditasi/LaporanDkps_4_6_PengembanganTendik.java:230");}
                            try { session.close(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/akreditasi/LaporanDkps_4_6_PengembanganTendik.java:231");}
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
