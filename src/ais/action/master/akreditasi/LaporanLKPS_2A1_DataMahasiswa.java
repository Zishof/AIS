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
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Jurusan;

/**
 * LKPS Tabel 2.A.1 — Data Mahasiswa S1.
 *
 * <p>Menampilkan perkembangan mahasiswa program studi S1 selama lima tahun
 * terakhir: daya tampung, jumlah pendaftar, mahasiswa yang diterima, dan
 * mahasiswa aktif per tahun.</p>
 *
 * <p>Data ini menunjukkan tren minat calon mahasiswa (pendaftar) dan efisiensi
 * penerimaan (diterima vs. pendaftar). Idealnya rasio pendaftar terhadap daya
 * tampung terus meningkat — menandakan program studi semakin diminati.</p>
 *
 * <p>Sumber data per kolom:</p>
 * <ul>
 *   <li><b>Daya Tampung</b> — {@code epsbed.kapasitas_mahasiswa_baru}</li>
 *   <li><b>Pendaftar</b>   — {@code cbs.cbs_calon_mahasiswa} (calon mahasiswa)</li>
 *   <li><b>Diterima</b>    — {@code mahasiswa} berdasarkan tahun angkatan</li>
 *   <li><b>Aktif</b>       — {@code mahasiswa} dengan status aktif tahun berjalan</li>
 * </ul>
 *
 * <h3>Format data baris (LKPS-2.A.1, dataStartRow=6)</h3>
 * <p>Setiap baris berisi: {@code [Tahun, Daya Tampung, Pendaftar, Diterima, Mhs. Aktif]}
 * pada posisi indeks 0-4. Enam baris kosong pertama dipertahankan untuk Excel template.</p>
 *
 * <h3>Perbaikan dari versi sebelumnya</h3>
 * <ul>
 *   <li>NPE pada {@code uniqueResult().longValue()} diperbaiki dengan null-safe coalesce</li>
 *   <li>Kolom data dipindah ke posisi 0-4 (sebelumnya 3/6-9 → data tidak tampil di grid)</li>
 *   <li>Filter Fakultas ditambahkan dan dihubungkan dengan filter Program Studi</li>
 *   <li>Session {@code currentNativeSession} ditutup di blok {@code finally}</li>
 *   <li>Fallback {@code aktif = diterima} dihapus — data kosong lebih jelas daripada data palsu</li>
 * </ul>
 *
 * @author AIS Development Team
 * @version 2.0
 */
public class LaporanLKPS_2A1_DataMahasiswa extends AkreditasiBaseWindow {

    /** Kode sheet untuk konfigurasi grid, grafik, dan Excel template. */
    public static final String sheetCode = "LKPS-2.A.1";
    private static final long serialVersionUID = 1L;

    private Combobox tahunAjaran;

    /**
     * Konstruktor default — menginisialisasi filter Fakultas/Prodi dan Tahun
     * Akademik, lalu membangun tata letak tanpa auto-load data.
     */
    public LaporanLKPS_2A1_DataMahasiswa() {
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

    /**
     * Konstruktor dengan parameter tampilan jendela.
     *
     * @param title    judul jendela
     * @param border   gaya border ZK
     * @param closable {@code true} jika jendela dapat ditutup
     * @throws Exception jika ZK gagal membuat komponen
     */
    public LaporanLKPS_2A1_DataMahasiswa(String title, String border, boolean closable) throws Exception {
        super(title, border, closable);
        initFakultasJurusan();
        Common.selectComboItem(tahunAjaran = Common.generateTahunAjaran(tahunAjaran),
                Common.getCurrentTahunAkademik());
        buildBase(false);
    }

    /** @return kode sheet {@value #sheetCode} */
    @Override
    protected String getSheetCode() {
        return sheetCode;
    }

    /**
     * Membangun baris filter: Fakultas, Program Studi, dan Tahun Akademik.
     * Perubahan nilai filter otomatis memicu {@link #onCetak}.
     */
    @Override
    protected void buildFilters(Row row) {
        addFakultasJurusanFilter(row);

        row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
        tahunAjaran.setWidth("90%");
        tahunAjaran.setReadonly(true);
        row.appendChild(tahunAjaran);
        tahunAjaran.addEventListener("onChange", new EventListener() {
            @Override
            public void onEvent(Event e) throws Exception {
                onCetak(null);
            }
        });
    }

    /**
     * Memuat data mahasiswa dari database di background thread untuk 5 tahun terakhir.
     *
     * <p>Setiap tahun menjalankan empat query: daya tampung, pendaftar, diterima,
     * dan aktif. Semua query menggunakan {@code COALESCE} atau null-safe check
     * untuk menghindari NPE. Setiap baris data dibangun dengan format
     * {@code [Tahun, Daya Tampung, Pendaftar, Diterima, Aktif]}.</p>
     *
     * @param event event ZK atau {@code null} jika dipanggil programatik
     */
    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void onCetak(Event event) {
        clearContent();
        try {
            final String tahunAkademik = (String) tahunAjaran.getSelectedItem().getValue();
            final Jurusan selectedJurusan = getSelectedJurusan();
            final Label label = new Label(ais.common.Common.getBahasaConfig("Proses load data .."));

            new Thread(new Runnable() {
                @Override
                public void run() {
                    Session session = null;
                    try {
                        int tahun = Integer.parseInt(tahunAkademik.split("/")[0]);
                        session = HibernateUtil.currentNativeSession();

                        // 6 baris kosong sesuai dataStartRow konfigurasi LKPS-2.A.1
                        List<List> datas = new ArrayList<List>();
                        for (int i = 0; i < 6; i++) {
                            datas.add(new ArrayList());
                        }

                        // Filter jurusan — jika null, ambil semua jenjang S1
                        String jurusanFilter = selectedJurusan == null ? ""
                                : " AND j.id = " + selectedJurusan.getId();

                        long statusAktifId = (ConstantValues.AKTIF != null) ? ConstantValues.AKTIF.getId() : 0L;

                        for (int yr = tahun - 4; yr <= tahun; yr++) {

                            // Daya tampung: dari epsbed.kapasitas_mahasiswa_baru
                            // Menggunakan COALESCE agar tidak NPE saat tabel kosong
                            String sqlDayaTampung =
                                "SELECT COALESCE(sum(k.kapasitas), 0)"
                                + " FROM epsbed.kapasitas_mahasiswa_baru k"
                                + " INNER JOIN jurusan j ON k.jurusan = j.id"
                                + " INNER JOIN jenjang jj ON j.jenjang = jj.id"
                                + " WHERE upper(jj.nama) IN ('S1','STRATA 1')"
                                + " AND k.tahun = " + yr
                                + jurusanFilter;
                            Object dtObj = session.createSQLQuery(sqlDayaTampung).uniqueResult();
                            long dayaTampung = dtObj == null ? 0L : ((Number) dtObj).longValue();

                            // Pendaftar: dari cbs.cbs_calon_mahasiswa
                            String sqlPendaftar =
                                "SELECT count(DISTINCT c.id)"
                                + " FROM cbs.cbs_calon_mahasiswa c"
                                + " INNER JOIN jurusan j ON c.jurusan = j.id"
                                + " INNER JOIN jenjang jj ON j.jenjang = jj.id"
                                + " WHERE upper(jj.nama) IN ('S1','STRATA 1')"
                                + " AND extract(year FROM c.tanggal_daftar) = " + yr
                                + jurusanFilter;
                            long pendaftar = 0L;
                            try {
                                Object p = session.createSQLQuery(sqlPendaftar).uniqueResult();
                                pendaftar = p == null ? 0L : ((Number) p).longValue();
                            } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/akreditasi/LaporanLKPS_2A1_DataMahasiswa.java:186");
                                // cbs schema mungkin tidak ada di semua instalasi
                            }

                            // Diterima: mahasiswa berdasarkan tahun angkatan
                            // Menggunakan COALESCE agar tidak NPE saat tabel kosong
                            String sqlDiterima =
                                "SELECT COALESCE(count(DISTINCT m.id), 0)"
                                + " FROM mahasiswa m"
                                + " INNER JOIN jurusan j ON m.jurusan = j.id"
                                + " INNER JOIN jenjang jj ON j.jenjang = jj.id"
                                + " WHERE upper(jj.nama) IN ('S1','STRATA 1')"
                                + " AND m.tahunangkatan = " + yr
                                + jurusanFilter;
                            Object dtObj2 = session.createSQLQuery(sqlDiterima).uniqueResult();
                            long diterima = dtObj2 == null ? 0L : ((Number) dtObj2).longValue();

                            // Mahasiswa aktif pada tahun berjalan (TS)
                            String sqlAktif =
                                "SELECT COALESCE(count(DISTINCT m.id), 0)"
                                + " FROM mahasiswa m"
                                + " INNER JOIN jurusan j ON m.jurusan = j.id"
                                + " INNER JOIN jenjang jj ON j.jenjang = jj.id"
                                + " WHERE upper(jj.nama) IN ('S1','STRATA 1')"
                                + " AND m.tahunangkatan = " + yr
                                + " AND EXISTS ("
                                + "   SELECT 1 FROM history_status_mahasiswa h"
                                + "   WHERE h.mahasiswa = m.id"
                                + "   AND h.status_mahasiswa = " + statusAktifId
                                + "   AND to_number(h.tahunakademik,'9999') = " + tahun
                                + ")"
                                + jurusanFilter;
                            long aktif = 0L;
                            try {
                                Object a = session.createSQLQuery(sqlAktif).uniqueResult();
                                aktif = a == null ? 0L : ((Number) a).longValue();
                            } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/akreditasi/LaporanLKPS_2A1_DataMahasiswa.java:222");
                                // status_mahasiswa id mungkin belum tersedia di semua instalasi
                            }

                            List sub = new ArrayList();
                            sub.add(yr);          // Tahun
                            sub.add(dayaTampung); // Daya Tampung
                            sub.add(pendaftar);   // Pendaftar
                            sub.add(diterima);    // Diterima
                            sub.add(aktif);       // Mhs. Aktif
                            datas.add(sub);
                        }

                        datas.add(new ArrayList());
                        datas.add(new ArrayList());

                        label.setAttribute("datas", datas);
                        label.setValue("");
                    } catch (Exception ex) {
                        ex.printStackTrace(); ais.common.ErrorAuditUtil.record(ex, "auto-audit src/ais/action/master/akreditasi/LaporanLKPS_2A1_DataMahasiswa.java:241");
                        label.setValue("");
                    } finally {
                        if (session != null) {
                            try { session.disconnect(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/akreditasi/LaporanLKPS_2A1_DataMahasiswa.java:245");}
                            try { session.close(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/akreditasi/LaporanLKPS_2A1_DataMahasiswa.java:246");}
                        }
                        HibernateUtil.closeSession();
                    }
                }
            }).start();

            display(label, 12);
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
        }
    }
}
