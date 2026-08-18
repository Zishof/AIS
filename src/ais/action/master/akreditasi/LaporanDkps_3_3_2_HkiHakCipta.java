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
 * DKPS Tabel 3.3-2 — Karya Inovatif Mahasiswa: HKI (Hak Cipta, Desain Produk Industri,
 * Program Komputer, Alat Peraga).
 *
 * <p>Menampilkan daftar karya inovatif mahasiswa program studi yang telah mendapatkan
 * pelindungan HKI selain paten, yaitu: Hak Cipta (karya seni, sastra, ilmu pengetahuan),
 * Desain Produk Industri, Program Komputer/Piranti Lunak, dan Alat Peraga selama periode
 * evaluasi akreditasi LAMDIK 2.0. Data mencakup identitas mahasiswa (NIM dan nama),
 * judul karya, tahun perolehan, dan nomor sertifikat HKI dari DJKI Kemenkumham.</p>
 *
 * <p>Instrumen ini merupakan bagian dari Kriteria 3 (Mahasiswa) akreditasi LAMDIK yang
 * menilai kualitas luaran karya inovatif mahasiswa. Hak Cipta dan Desain Produk Industri
 * yang dihasilkan mahasiswa menunjukkan kemampuan berkarya dan berinovasi di bidang
 * teknologi, seni, dan ilmu pengetahuan terapan.</p>
 *
 * <p>Program komputer yang didaftarkan Hak Cipta menjadi indikator relevan bagi program
 * studi teknik dan ilmu komputer dalam penilaian akreditasi LAMDIK 2.0.</p>
 *
 * <h3>Sumber data</h3>
 * <p>Data diambil dari tabel {@code hki_mahasiswa} dengan filter jenis yang mencakup
 * HAK CIPTA, DESAIN, PROGRAM, atau ALAT PERAGA. Fallback ditampilkan bila tabel
 * belum tersedia.</p>
 *
 * <h3>Manajemen sesi</h3>
 * <p>Background thread menggunakan {@code currentNativeSession()} dengan
 * penutupan wajib di {@code finally}. Tidak menggunakan multi-catch (Java 1.7).</p>
 *
 * @author AIS Development Team
 * @version 2.0
 * @see AkreditasiBaseWindow
 */
public class LaporanDkps_3_3_2_HkiHakCipta extends AkreditasiBaseWindow {

    /** Kode sheet untuk konfigurasi grid, grafik, dan Excel template. */
    public static final String sheetCode = "DKPS-3.3-2";
    private static final long serialVersionUID = 1L;

    /**
     * Konstruktor default — menginisialisasi filter Fakultas/Program Studi
     * lalu membangun tata letak tanpa auto-load data.
     */
    public LaporanDkps_3_3_2_HkiHakCipta() {
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
    public LaporanDkps_3_3_2_HkiHakCipta(String title, String border, boolean closable) throws Exception {
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
     * Memuat data HKI hak cipta mahasiswa dari database di background thread.
     *
     * <p>Query mengambil data dari tabel {@code hki_mahasiswa} dengan filter jenis
     * yang mencakup: HAK CIPTA, DESAIN, PROGRAM KOMPUTER, dan ALAT PERAGA.
     * Jika query gagal karena tabel belum ada, ditampilkan tiga baris template kosong.</p>
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
                        try {
                            String sql = "SELECT m.nim, m.nama, h.judul, h.tahun, h.nomor "
                                + "FROM hki_mahasiswa h "
                                + "INNER JOIN mahasiswa m ON h.mahasiswa=m.id "
                                + "INNER JOIN jurusan j ON m.jurusan=j.id "
                                + "WHERE (UPPER(h.jenis) LIKE '%HAK CIPTA%' "
                                + "OR UPPER(h.jenis) LIKE '%DESAIN%' "
                                + "OR UPPER(h.jenis) LIKE '%PROGRAM%' "
                                + "OR UPPER(h.jenis) LIKE '%ALAT PERAGA%')"
                                + jurusanWhere
                                + " ORDER BY h.tahun DESC, m.nama";
                            List<Object[]> rows = session.createSQLQuery(sql).list();
                            int no = 1;
                            for (Object[] r : rows) {
                                String nimNama = (r[0] != null ? r[0].toString() : "")
                                    + " - " + (r[1] != null ? r[1].toString() : "");
                                String judul = r[2] != null ? r[2].toString() : "";
                                String tahun = r[3] != null ? r[3].toString() : "";
                                String noSertifikat = r[4] != null ? r[4].toString() : "";

                                List sub = new ArrayList();
                                sub.add(no++);
                                sub.add(nimNama);
                                sub.add(judul);
                                sub.add(tahun);
                                sub.add(noSertifikat);
                                datas.add(sub);
                            }
                            queryBerhasil = true;
                        } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/akreditasi/LaporanDkps_3_3_2_HkiHakCipta.java:159");
                        }

                        if (!queryBerhasil) {
                            for (int i = 1; i <= 3; i++) {
                                List sub = new ArrayList();
                                sub.add(i);
                                sub.add(""); sub.add(""); sub.add(""); sub.add("");
                                datas.add(sub);
                            }
                        }

                        label.setAttribute("datas", datas);
                        label.setValue("");
                    } catch (Exception ex) {
                        ex.printStackTrace(); ais.common.ErrorAuditUtil.record(ex, "auto-audit src/ais/action/master/akreditasi/LaporanDkps_3_3_2_HkiHakCipta.java:174");
                        label.setValue("");
                    } finally {
                        if (session != null) {
                            try { session.disconnect(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/akreditasi/LaporanDkps_3_3_2_HkiHakCipta.java:178");}
                            try { session.close(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/akreditasi/LaporanDkps_3_3_2_HkiHakCipta.java:179");}
                        }
                        HibernateUtil.closeSession();
                    }
                }
            }).start();

            display(label, 5);
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
        }
    }
}
