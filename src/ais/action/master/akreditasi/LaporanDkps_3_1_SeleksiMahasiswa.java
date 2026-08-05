package ais.action.master.akreditasi;

import java.util.ArrayList;
import java.util.Calendar;
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
import ais.ui.util.MyLabelConfig;

/**
 * DKPS Tabel 3.1 — Data Seleksi dan Penerimaan Mahasiswa Baru.
 *
 * <p>Menampilkan statistik seleksi penerimaan mahasiswa baru selama lima tahun
 * terakhir, meliputi daya tampung yang ditetapkan, jumlah pendaftar, jumlah yang
 * diterima, dan jumlah yang aktif kuliah per angkatan. Data ini digunakan untuk
 * menghitung rasio selektivitas program studi dalam instrumen akreditasi
 * LAMDIK 2.0.</p>
 *
 * <p>Sumber data:</p>
 * <ul>
 *   <li><strong>Daya Tampung</strong> — tabel {@code epsbed.kapasitas_mahasiswa_baru}
 *       yang menyimpan kapasitas penerimaan yang ditetapkan program studi per tahun.</li>
 *   <li><strong>Pendaftar</strong> — tabel {@code cbs.cbs_calon_mahasiswa} yang
 *       mencatat calon mahasiswa yang mendaftar. Jika skema {@code cbs} belum ada,
 *       blok try-catch menangani exception dan nilai defaultnya adalah 0.</li>
 *   <li><strong>Diterima</strong> — tabel {@code public.mahasiswa} dengan filter
 *       {@code tahunangkatan} sesuai tahun yang dihitung.</li>
 *   <li><strong>Aktif</strong> — tabel {@code public.mahasiswa} dengan tambahan
 *       filter {@code status_mahasiswa IS NOT NULL} sebagai pendekatan status aktif.</li>
 * </ul>
 *
 * <h3>Format data baris (DKPS-3.1, dataStartRow=5)</h3>
 * <p>Lima baris kosong sebagai padding, diikuti baris data per tahun dengan
 * tujuh kolom: Tahun, Daya Tampung, Pendaftar (Putra), Pendaftar (Putri),
 * Total Pendaftar, Diterima, dan Aktif. Kolom putra/putri diisi 0 karena
 * data gender tidak selalu tersedia di semua instalasi.</p>
 *
 * <h3>Filter Tahun</h3>
 * <p>Tersedia Combobox pemilihan tahun awal rentang. Secara default menampilkan
 * data dari tahun {@code (tahun_sekarang - 4)} sampai tahun sekarang sehingga
 * mencakup lima tahun akademik terakhir sesuai standar LAMDIK.</p>
 *
 * <h3>Manajemen sesi</h3>
 * <p>Background thread menggunakan {@code currentNativeSession()} dengan
 * penutupan wajib di {@code finally}. Tidak menggunakan multi-catch (Java 1.7).</p>
 *
 * @author AIS Development Team
 * @version 2.0
 * @see AkreditasiBaseWindow
 */
public class LaporanDkps_3_1_SeleksiMahasiswa extends AkreditasiBaseWindow {

    /** Kode sheet untuk konfigurasi grid, grafik, dan Excel template. */
    public static final String sheetCode = "DKPS-3.1";
    private static final long serialVersionUID = 1L;

    private Combobox cbTahun;

    /**
     * Konstruktor default — menginisialisasi filter Fakultas/Prodi dan Tahun,
     * lalu membangun tata letak tanpa auto-load data.
     */
    public LaporanDkps_3_1_SeleksiMahasiswa() {
        super();
        try {
            initFakultasJurusan();
            cbTahun = buildTahunCombo();
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
    public LaporanDkps_3_1_SeleksiMahasiswa(String title, String border, boolean closable) throws Exception {
        super(title, border, closable);
        initFakultasJurusan();
        cbTahun = buildTahunCombo();
        buildBase(false);
    }

    /** @return kode sheet {@value #sheetCode} */
    @Override
    protected String getSheetCode() {
        return sheetCode;
    }

    /**
     * Membangun Combobox pilihan tahun berisi 10 tahun terakhir.
     * Tahun sekarang dipilih secara default.
     */
    private Combobox buildTahunCombo() {
        Combobox cb = new Combobox();
        cb.setReadonly(true);
        int tahunSekarang = Calendar.getInstance().get(Calendar.YEAR);
        for (int t = tahunSekarang; t >= tahunSekarang - 9; t--) {
            org.zkoss.zul.Comboitem ci = new org.zkoss.zul.Comboitem(String.valueOf(t));
            ci.setValue(t);
            ci.setParent(cb);
            if (t == tahunSekarang) {
                cb.setSelectedItem(ci);
            }
        }
        return cb;
    }

    /**
     * Membangun baris filter: Fakultas, Program Studi, dan Tahun.
     * Perubahan nilai filter otomatis memicu {@link #onCetak}.
     */
    @Override
    protected void buildFilters(Row row) {
        addFakultasJurusanFilter(row);

        row.appendChild(new MyLabelConfig("Tahun Akhir"));
        cbTahun.setWidth("90%");
        row.appendChild(cbTahun);
        cbTahun.addEventListener("onChange", new EventListener() {
            @Override
            public void onEvent(Event e) throws Exception {
                onCetak(null);
            }
        });
    }

    /**
     * Memuat data seleksi mahasiswa baru dari database di background thread.
     *
     * <p>Untuk setiap tahun dalam rentang (tahun_akhir - 4) sampai tahun_akhir,
     * dihitung daya tampung, pendaftar, diterima, dan aktif. Pendaftar dari
     * skema {@code cbs} diambil dengan try-catch tersendiri karena skema tersebut
     * mungkin tidak tersedia di semua instalasi.</p>
     *
     * @param event event ZK atau {@code null} jika dipanggil secara programatik
     */
    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void onCetak(Event event) {
        clearContent();
        try {
            if (cbTahun.getSelectedItem() == null) return;
            final int tahunAkhir = (Integer) cbTahun.getSelectedItem().getValue();
            final Jurusan selectedJurusan = getSelectedJurusan();
            final Label label = new Label(ais.common.Common.getBahasaConfig("Proses load data .."));

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

                        String jurusanIdStr = selectedJurusan == null ? null
                            : String.valueOf(selectedJurusan.getId());

                        for (int yr = tahunAkhir - 4; yr <= tahunAkhir; yr++) {
                            // Daya Tampung
                            long dayaTampung = 0;
                            try {
                                String sqlDT = "SELECT COALESCE(SUM(k.kapasitas),0) "
                                    + "FROM epsbed.kapasitas_mahasiswa_baru k "
                                    + "INNER JOIN jurusan j ON k.jurusan=j.id "
                                    + "WHERE k.tahun=" + yr
                                    + (jurusanIdStr != null ? " AND j.id=" + jurusanIdStr : "");
                                Object dtRes = session.createSQLQuery(sqlDT).uniqueResult();
                                if (dtRes != null) dayaTampung = Long.parseLong(dtRes.toString());
                            } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/akreditasi/LaporanDkps_3_1_SeleksiMahasiswa.java:188");
                            }

                            // Pendaftar (dari CBS jika tersedia)
                            long pendaftar = 0;
                            try {
                                String sqlPend = "SELECT COUNT(*) FROM cbs.cbs_calon_mahasiswa c "
                                    + "INNER JOIN jurusan j ON c.jurusan=j.id "
                                    + "WHERE EXTRACT(year FROM c.tanggal_daftar)=" + yr
                                    + (jurusanIdStr != null ? " AND j.id=" + jurusanIdStr : "");
                                Object pRes = session.createSQLQuery(sqlPend).uniqueResult();
                                if (pRes != null) pendaftar = Long.parseLong(pRes.toString());
                            } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/akreditasi/LaporanDkps_3_1_SeleksiMahasiswa.java:200");
                            }

                            // Diterima
                            long diterima = 0;
                            try {
                                String sqlDit = "SELECT COUNT(*) FROM mahasiswa m "
                                    + "INNER JOIN jurusan j ON m.jurusan=j.id "
                                    + "WHERE m.tahunangkatan=" + yr
                                    + (jurusanIdStr != null ? " AND j.id=" + jurusanIdStr : "");
                                Object dRes = session.createSQLQuery(sqlDit).uniqueResult();
                                if (dRes != null) diterima = Long.parseLong(dRes.toString());
                            } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/akreditasi/LaporanDkps_3_1_SeleksiMahasiswa.java:212");
                            }

                            // Aktif
                            long aktif = 0;
                            try {
                                String sqlAktif = "SELECT COUNT(*) FROM mahasiswa m "
                                    + (jurusanIdStr != null
                                        ? "INNER JOIN jurusan j ON m.jurusan=j.id " : "")
                                    + "WHERE m.tahunangkatan=" + yr
                                    + " AND m.status_mahasiswa IS NOT NULL"
                                    + (jurusanIdStr != null ? " AND j.id=" + jurusanIdStr : "");
                                Object aRes = session.createSQLQuery(sqlAktif).uniqueResult();
                                if (aRes != null) aktif = Long.parseLong(aRes.toString());
                            } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/akreditasi/LaporanDkps_3_1_SeleksiMahasiswa.java:226");
                            }

                            List sub = new ArrayList();
                            sub.add(yr);              // Tahun Angkatan
                            sub.add(dayaTampung);     // Daya Tampung
                            sub.add(0);               // Pendaftar Putra (tidak tersedia)
                            sub.add(0);               // Pendaftar Putri (tidak tersedia)
                            sub.add(pendaftar);       // Total Pendaftar
                            sub.add(diterima);        // Diterima
                            sub.add(aktif);           // Aktif
                            sub.add("");              // Kolom ke-8
                            datas.add(sub);
                        }

                        label.setAttribute("datas", datas);
                        label.setValue("");
                    } catch (Exception ex) {
                        ex.printStackTrace(); ais.common.ErrorAuditUtil.record(ex, "auto-audit src/ais/action/master/akreditasi/LaporanDkps_3_1_SeleksiMahasiswa.java:244");
                        label.setValue("");
                    } finally {
                        if (session != null) {
                            try { session.disconnect(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/akreditasi/LaporanDkps_3_1_SeleksiMahasiswa.java:248");}
                            try { session.close(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/akreditasi/LaporanDkps_3_1_SeleksiMahasiswa.java:249");}
                        }
                        HibernateUtil.closeSession();
                    }
                }
            }).start();

            display(label, 8);
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
        }
    }
}
