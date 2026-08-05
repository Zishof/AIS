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
 * DKPS Tabel 5.4 — Teknologi Informasi dan Komunikasi (TIK) Program Studi.
 *
 * <p>Tabel ini menginventarisasi sistem informasi dan teknologi informasi
 * dan komunikasi (TIK) yang digunakan oleh program studi dalam mendukung
 * proses akademik, administrasi, penelitian, dan pengabdian kepada masyarakat.
 * Ketersediaan dan tingkat integrasi TIK merupakan indikator penting dalam
 * penilaian akreditasi LAMDIK 2.0 pada kriteria sarana dan prasarana.</p>
 *
 * <p>Tingkat integrasi sistem dinilai berdasarkan tiga kategori: (1) Terintegrasi
 * Penuh — sistem terhubung satu sama lain dan berbagi data secara otomatis;
 * (2) Sebagian — sistem memiliki beberapa koneksi namun belum sepenuhnya
 * terintegrasi; (3) Tidak Terintegrasi — sistem berdiri sendiri tanpa koneksi
 * ke sistem lain.</p>
 *
 * <p>Kemutakhiran sistem dinilai dari apakah sistem masih aktif dikembangkan,
 * mendapat pembaruan rutin, dan menggunakan teknologi yang relevan dengan
 * perkembangan terkini (Mutakhir) atau sudah usang dan tidak lagi diperbarui
 * (Tidak Mutakhir). Aksesibilitas mencakup kemudahan akses pengguna termasuk
 * dukungan akses dari perangkat mobile dan dari luar kampus.</p>
 *
 * <h3>Sumber data</h3>
 * <p>Data TIK umumnya tidak tersedia di database AIS secara terstruktur,
 * sehingga ditampilkan 8 sistem informasi standar sebagai template yang
 * relevan dengan AIS yang sudah berjalan di institusi.</p>
 *
 * <h3>Format baris (maxCols=6)</h3>
 * <ol>
 *   <li>No. Urut</li>
 *   <li>Jenis TIK / Sistem Informasi</li>
 *   <li>Fungsi Utama</li>
 *   <li>Status Integrasi</li>
 *   <li>Kemutakhiran</li>
 *   <li>Aksesibilitas</li>
 * </ol>
 *
 * @author AIS Development Team
 * @version 2.0
 * @see AkreditasiBaseWindow
 */
public class LaporanDkps_5_4_Tik extends AkreditasiBaseWindow {

    /** Kode sheet untuk konfigurasi grid, grafik, dan Excel template. */
    public static final String sheetCode = "DKPS-5.4";
    private static final long serialVersionUID = 1L;

    /**
     * Konstruktor default — menginisialisasi filter Fakultas/Program Studi
     * lalu membangun tata letak tanpa auto-load data.
     */
    public LaporanDkps_5_4_Tik() {
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
    public LaporanDkps_5_4_Tik(String title, String border, boolean closable) throws Exception {
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
     * Menampilkan template 8 sistem TIK standar yang relevan dengan AIS.
     *
     * <p>Data TIK tidak tersimpan secara terstruktur di database AIS,
     * sehingga ditampilkan template 8 sistem standar yang wajib ada di
     * institusi pendidikan berdasarkan instrumen LAMDIK 2.0.</p>
     *
     * @param event event ZK atau {@code null} jika dipanggil secara programatik
     */
    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void onCetak(final Event event) {
        clearContent();
        try {
            final Label label = new Label(ais.common.Common.getBahasaConfig("Memuat data TIK ..."));

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

                        // Template 8 sistem TIK standar LAMDIK
                        // Format: Nama Sistem, Fungsi, Status Integrasi, Kemutakhiran, Aksesibilitas
                        String[][] template = {
                            {
                                "Sistem Informasi Akademik (SIA/SIAKAD)",
                                "Pengelolaan data akademik: KRS, nilai, absensi, transkrip",
                                "Terintegrasi Penuh",
                                "Mutakhir",
                                "Online, dapat diakses dari luar kampus"
                            },
                            {
                                "E-Learning / Learning Management System",
                                "Pengelolaan pembelajaran daring: materi, tugas, ujian online",
                                "Terintegrasi Penuh",
                                "Mutakhir",
                                "Online, dapat diakses dari perangkat mobile"
                            },
                            {
                                "Sistem Informasi Kepegawaian",
                                "Pengelolaan data dosen, tendik, absensi, dan penggajian",
                                "Sebagian",
                                "Mutakhir",
                                "Internal kampus"
                            },
                            {
                                "Sistem Informasi Keuangan",
                                "Pengelolaan tagihan, pembayaran, dan pelaporan keuangan",
                                "Terintegrasi Penuh",
                                "Mutakhir",
                                "Online dengan gateway pembayaran"
                            },
                            {
                                "Sistem Informasi Perpustakaan",
                                "Pengelolaan koleksi buku, peminjaman, dan repositori digital",
                                "Sebagian",
                                "Mutakhir",
                                "Online dan katalog dapat diakses publik"
                            },
                            {
                                "Website Institusi",
                                "Informasi publik institusi dan program studi",
                                "Sebagian",
                                "Mutakhir",
                                "Publik, responsif untuk mobile"
                            },
                            {
                                "Email Institusi",
                                "Komunikasi resmi civitas akademika",
                                "Tidak Terintegrasi",
                                "Mutakhir",
                                "Online, dapat diakses dari perangkat apa pun"
                            },
                            {
                                "Sistem Penjaminan Mutu",
                                "Pengelolaan dokumen mutu, audit internal, dan pelaporan",
                                "Tidak Terintegrasi",
                                "Mutakhir",
                                "Internal kampus"
                            }
                        };

                        for (int i = 0; i < template.length; i++) {
                            List sub = new ArrayList();
                            sub.add(i + 1);
                            sub.add(template[i][0]); // Nama Infrastruktur/SI
                            sub.add(template[i][1]); // Deskripsi
                            sub.add(template[i][2]); // Terintegrasi
                            sub.add(template[i][3]); // Mutahir
                            sub.add(template[i][4]); // Ketersediaan
                            sub.add("");              // Jumlah
                            sub.add("");              // Kepemilikan
                            sub.add("");              // Kondisi
                            datas.add(sub);
                        }

                        label.setAttribute("datas", datas);
                        label.setValue("");
                    } catch (Exception ex) {
                        ex.printStackTrace(); ais.common.ErrorAuditUtil.record(ex, "auto-audit src/ais/action/master/akreditasi/LaporanDkps_5_4_Tik.java:209");
                        label.setValue("");
                    } finally {
                        if (session != null) {
                            try { session.disconnect(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/akreditasi/LaporanDkps_5_4_Tik.java:213");}
                            try { session.close(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/akreditasi/LaporanDkps_5_4_Tik.java:214");}
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
