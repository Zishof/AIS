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
 * DKPS Tabel 4.5 — Profil Tenaga Kependidikan Program Studi.
 *
 * <p>Tabel ini menyajikan profil tenaga kependidikan (tendik) yang mendukung
 * penyelenggaraan program studi, dikelompokkan berdasarkan jenis jabatan dan
 * tingkat pendidikan terakhir. Ketersediaan tenaga kependidikan yang berkualitas
 * merupakan salah satu indikator penting dalam penilaian akreditasi LAMDIK 2.0
 * untuk menilai kelayakan dukungan layanan akademik non-dosen.</p>
 *
 * <p>Jenis tenaga kependidikan yang diinventarisasi meliputi: Pustakawan,
 * Laboran, Teknisi, Programer/Operator Komputer, Tenaga Administrasi,
 * Security/Satpam, dan tenaga lainnya yang bertugas mendukung proses
 * pembelajaran dan pengelolaan program studi.</p>
 *
 * <p>Tingkat pendidikan dikelompokkan ke dalam: S3, S2, S1, D4, D3, dan
 * SMA/SMK. Proporsi pendidikan tendik mencerminkan kualifikasi sumber daya
 * manusia non-akademik institusi.</p>
 *
 * <h3>Sumber data</h3>
 * <p>Data dicoba diambil dari tabel {@code pegawai} dengan GROUP BY
 * {@code jabatan} dan klasifikasi {@code pendidikan_terakhir}.
 * Jika tabel tidak tersedia, ditampilkan 7 baris template standar
 * sebagai panduan pengisian manual oleh operator.</p>
 *
 * <h3>Format baris (maxCols=9)</h3>
 * <ol>
 *   <li>No. Urut</li>
 *   <li>Jenis Tenaga Kependidikan</li>
 *   <li>Jumlah S3</li>
 *   <li>Jumlah S2</li>
 *   <li>Jumlah S1</li>
 *   <li>Jumlah D4</li>
 *   <li>Jumlah D3</li>
 *   <li>Jumlah SMA/SMK</li>
 *   <li>Total</li>
 * </ol>
 *
 * @author AIS Development Team
 * @version 2.0
 * @see AkreditasiBaseWindow
 */
public class LaporanDkps_4_5_TenagaKependidikan extends AkreditasiBaseWindow {

    /** Kode sheet untuk konfigurasi grid, grafik, dan Excel template. */
    public static final String sheetCode = "DKPS-4.5";
    private static final long serialVersionUID = 1L;

    /**
     * Konstruktor default — menginisialisasi filter Fakultas/Program Studi
     * lalu membangun tata letak tanpa auto-load data.
     */
    public LaporanDkps_4_5_TenagaKependidikan() {
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
    public LaporanDkps_4_5_TenagaKependidikan(String title, String border, boolean closable) throws Exception {
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
     * Memuat data profil tenaga kependidikan dari database di background thread.
     *
     * <p>Mencoba query dari tabel {@code pegawai} GROUP BY jabatan dengan
     * penghitungan jumlah per tingkat pendidikan menggunakan CASE WHEN.
     * Jika tabel tidak tersedia atau kosong, ditampilkan 7 baris template
     * dengan jenis tendik standar LAMDIK.</p>
     *
     * @param event event ZK atau {@code null} jika dipanggil secara programatik
     */
    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void onCetak(final Event event) {
        clearContent();
        try {
            final Jurusan selectedJurusan = getSelectedJurusan();
            final Label label = new Label(ais.common.Common.getBahasaConfig("Memuat data tenaga kependidikan ..."));

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
                        try {
                            String checkSql = "SELECT COUNT(*) FROM information_schema.tables "
                                + "WHERE table_schema='public' AND table_name='pegawai'";
                            Object cnt = session.createSQLQuery(checkSql).uniqueResult();
                            boolean tableExists = cnt != null && Long.parseLong(cnt.toString()) > 0;

                            if (tableExists) {
                                String jurusanFilter = "";
                                if (selectedJurusan != null) {
                                    jurusanFilter = " AND p.jurusan = " + selectedJurusan.getId();
                                }
                                // Tendik = bukan dosen; kelompokkan per jabatan, hitung per pendidikan
                                String sql = "SELECT p.jabatan,"
                                    + " COUNT(CASE WHEN UPPER(p.pendidikan_terakhir) IN ('S3','DOKTOR') THEN 1 END) AS s3,"
                                    + " COUNT(CASE WHEN UPPER(p.pendidikan_terakhir) IN ('S2','MAGISTER','MM','M.PD','M.SI') THEN 1 END) AS s2,"
                                    + " COUNT(CASE WHEN UPPER(p.pendidikan_terakhir) IN ('S1','SARJANA') THEN 1 END) AS s1,"
                                    + " COUNT(CASE WHEN UPPER(p.pendidikan_terakhir) = 'D4' THEN 1 END) AS d4,"
                                    + " COUNT(CASE WHEN UPPER(p.pendidikan_terakhir) = 'D3' THEN 1 END) AS d3,"
                                    + " COUNT(CASE WHEN UPPER(p.pendidikan_terakhir) IN ('SMA','SMK','SMA/SMK','SLTA') THEN 1 END) AS sma,"
                                    + " COUNT(*) AS total"
                                    + " FROM pegawai p"
                                    + " WHERE LOWER(COALESCE(p.jabatan,'')) NOT LIKE '%dosen%'"
                                    + jurusanFilter
                                    + " GROUP BY p.jabatan"
                                    + " ORDER BY p.jabatan"
                                    + " LIMIT 30";

                                List<Object[]> rows = session.createSQLQuery(sql).list();
                                int noUrut = 1;
                                for (Object[] obj : rows) {
                                    List sub = new ArrayList();
                                    sub.add(noUrut);
                                    sub.add(obj[0] != null ? obj[0].toString() : "");
                                    sub.add(obj[1] != null ? obj[1].toString() : "0");
                                    sub.add(obj[2] != null ? obj[2].toString() : "0");
                                    sub.add(obj[3] != null ? obj[3].toString() : "0");
                                    sub.add(obj[4] != null ? obj[4].toString() : "0");
                                    sub.add(obj[5] != null ? obj[5].toString() : "0");
                                    sub.add(obj[6] != null ? obj[6].toString() : "0");
                                    sub.add(obj[7] != null ? obj[7].toString() : "0");
                                    datas.add(sub);
                                    noUrut++;
                                }
                                loaded = rows != null && rows.size() > 0;
                            }
                        } catch (Exception exQuery) {
                            exQuery.printStackTrace(); ais.common.ErrorAuditUtil.record(exQuery, "auto-audit src/ais/action/master/akreditasi/LaporanDkps_4_5_TenagaKependidikan.java:181");
                        }

                        if (!loaded) {
                            // Template: 7 jenis tendik standar LAMDIK
                            String[] jenisTendik = {
                                "Pustakawan",
                                "Laboran",
                                "Teknisi",
                                "Programer/Operator Komputer",
                                "Tenaga Administrasi",
                                "Security/Satpam",
                                "Lainnya"
                            };
                            for (int i = 0; i < jenisTendik.length; i++) {
                                List sub = new ArrayList();
                                sub.add(i + 1);
                                sub.add(jenisTendik[i]);
                                sub.add(0); // S3
                                sub.add(0); // S2
                                sub.add(0); // S1
                                sub.add(0); // D4
                                sub.add(0); // D3
                                sub.add(0); // SMA/SMK
                                sub.add(0); // Total
                                datas.add(sub);
                            }
                        }

                        label.setAttribute("datas", datas);
                        label.setValue("");
                    } catch (Exception ex) {
                        ex.printStackTrace(); ais.common.ErrorAuditUtil.record(ex, "auto-audit src/ais/action/master/akreditasi/LaporanDkps_4_5_TenagaKependidikan.java:213");
                        label.setValue("");
                    } finally {
                        if (session != null) {
                            try { session.disconnect(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/akreditasi/LaporanDkps_4_5_TenagaKependidikan.java:217");}
                            try { session.close(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/akreditasi/LaporanDkps_4_5_TenagaKependidikan.java:218");}
                        }
                        HibernateUtil.closeSession();
                    }
                }
            }).start();

            display(label, 9);
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
        }
    }
}
