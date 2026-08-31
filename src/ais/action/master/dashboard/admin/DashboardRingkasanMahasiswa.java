package ais.action.master.dashboard.admin;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Div;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Rows;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.ui.util.HtmlChartHelper;
import ais.ui.util.MyFormRow;
import ais.ui.util.MyHtml;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.WaktuUtil;

/**
 * <h1>DashboardRingkasanMahasiswa &mdash; Dasbor Komprehensif Statistik Mahasiswa</h1>
 *
 * <p>Kelas ini adalah jantung dari tab "Dasbor" yang tampil sebagai tab pertama
 * di {@link DashboardMahasiswa}. Tujuannya sederhana: menyajikan gambaran besar data
 * mahasiswa dalam satu halaman yang bisa dibaca oleh siapa saja &mdash; dari pimpinan
 * institusi, kepala program studi, hingga staf administrasi &mdash; tanpa memerlukan
 * keahlian khusus di bidang teknologi informasi atau statistik.</p>
 *
 * <h2>Filosofi Dasbor</h2>
 * <p>Dasbor dirancang mengikuti prinsip <em>progressive disclosure</em>: informasi
 * paling penting (angka ringkasan) muncul paling atas, diikuti oleh visualisasi yang
 * semakin detail ke bawah. Pengguna di perangkat telepon genggam cukup menggulir ke
 * bawah untuk melihat semua informasi, karena seluruh tata letak responsif dan
 * menyesuaikan lebar layar secara otomatis. Tidak ada pengetahuan teknis yang
 * dibutuhkan untuk membaca dasbor ini.</p>
 *
 * <h2>Panel yang Ditampilkan (Berurutan dari Atas ke Bawah)</h2>
 * <ol>
 *   <li><b>Kartu Angka Utama (KPI)</b> &mdash; Lima angka besar yang langsung
 *       menjawab pertanyaan "berapa?": total mahasiswa, jumlah laki-laki, jumlah
 *       perempuan, jumlah program studi yang aktif menerima mahasiswa, dan jumlah
 *       provinsi asal yang tercatat di biodata.</li>
 *
 *   <li><b>Komposisi Jenis Kelamin (Diagram Lingkaran / Donat)</b> &mdash; Lingkaran
 *       terbagi dua yang langsung memperlihatkan rasio laki-laki dan perempuan.
 *       Angka persentase di tengah lingkaran menunjukkan kelompok yang lebih besar.
 *       Berguna untuk memantau keseimbangan gender dari tahun ke tahun.</li>
 *
 *   <li><b>Komposisi Gender per Angkatan (Batang Bertumpuk / Stacked Bar)</b> &mdash;
 *       Satu batang per tahun angkatan, terbagi antara laki-laki (biru) dan perempuan
 *       (merah muda). Pergeseran warna dari kiri ke kanan menunjukkan apakah komposisi
 *       gender berubah antar-angkatan &mdash; informasi penting untuk evaluasi program
 *       inklusivitas kampus.</li>
 *
 *   <li><b>Tren Jumlah Mahasiswa per Angkatan (Grafik Garis)</b> &mdash; Tiga garis
 *       dalam satu grafik: total, laki-laki, dan perempuan per tahun angkatan. Memudahkan
 *       pimpinan menjawab pertanyaan "apakah jumlah mahasiswa baru kita meningkat?".
 *       Tren naik berarti promosi berhasil; tren turun adalah sinyal untuk mengevaluasi
 *       strategi perekrutan.</li>
 *
 *   <li><b>Top 8 Program Studi (Batang Horizontal)</b> &mdash; Daftar program studi
 *       dari yang paling banyak mahasiswanya hingga paling sedikit. Membantu manajemen
 *       mengidentifikasi prodi unggulan yang membutuhkan lebih banyak dosen dan ruang,
 *       serta prodi yang perlu strategi promosi lebih agresif.</li>
 *
 *   <li><b>Top 8 Provinsi Asal (Batang Horizontal)</b> &mdash; Provinsi asal mahasiswa
 *       yang paling banyak terwakili, berdasarkan data biodata. Berguna untuk mengarahkan
 *       kegiatan promosi ke SMA/SMK di provinsi yang belum banyak mengirim mahasiswa.
 *       Jika data biodata kosong, panel ini akan menampilkan pesan informatif.</li>
 *
 *   <li><b>Perbandingan Program Studi (Grafik Jaring Laba-laba / Radar)</b> &mdash;
 *       Grafik multi-dimensi yang membandingkan lima program studi terbesar sekaligus
 *       dari tiga sudut pandang: jumlah total mahasiswa, persentase laki-laki, dan
 *       persentase perempuan. Bentuknya seperti jaring laba-laba &mdash; semakin luas
 *       area suatu prodi, semakin unggul di banyak dimensi sekaligus. Berbeda dari
 *       grafik batang biasa yang hanya membandingkan satu ukuran, grafik radar
 *       memperlihatkan "bentuk" profil masing-masing prodi secara holistik.</li>
 *
 *   <li><b>Pekerjaan Orang Tua (Diagram Lingkaran / Donat)</b> &mdash; Distribusi jenis
 *       pekerjaan ayah mahasiswa yang paling umum. Berguna untuk merancang program
 *       beasiswa yang tepat sasaran, memahami kemampuan ekonomi keluarga, dan menentukan
 *       prioritas kerjasama industri yang relevan dengan latar belakang keluarga
 *       mahasiswa.</li>
 *
 *   <li><b>Pendidikan Orang Tua (Batang Horizontal)</b> &mdash; Tingkat pendidikan
 *       formal tertinggi ayah mahasiswa. Data ini penting untuk program "generasi pertama
 *       sarjana di keluarga" (first-generation college students) dan untuk merancang
 *       layanan bimbingan akademik bagi mahasiswa yang memerlukan dukungan ekstra karena
 *       keluarganya belum pernah mengalami pendidikan tinggi sebelumnya.</li>
 * </ol>
 *
 * <h2>Filter Data</h2>
 * <p>Semua panel di dasbor ini dapat disaring berdasarkan <b>rentang tahun angkatan</b>
 * (dari tahun berapa sampai tahun berapa). Secara bawaan, dasbor menampilkan angkatan
 * 5 tahun ke belakang hingga tahun ini. Setelah mengubah rentang, tekan tombol
 * "Muat Ulang" atau ubah nilai kotak angka untuk memperbarui tampilan.</p>
 *
 * <h2>Cara Membaca Setiap Grafik</h2>
 * <ul>
 *   <li><b>Batang panjang</b> = nilainya besar. <b>Batang pendek</b> = nilainya kecil.</li>
 *   <li><b>Lingkaran besar</b> di donat = kelompok dominan. Persentase muncul di tengah.</li>
 *   <li><b>Garis naik</b> di grafik tren = jumlah meningkat dari kiri ke kanan.</li>
 *   <li><b>Area luas</b> di radar = unggul di banyak dimensi sekaligus.</li>
 * </ul>
 *
 * <h2>Arsitektur Teknis</h2>
 * <ul>
 *   <li>Extends {@link MyWindow} &mdash; mengikuti pola standar semua dashboard admin
 *       (DashboardJenisKelaminMahasiswa, DashboardMahasiswaPropinsi, dst.)</li>
 *   <li>Tata letak responsif menggunakan {@code DasborResponsifHelper.saringanDanIsi()}
 *       yang menumpuk panel filter dan konten secara vertikal di layar HP.</li>
 *   <li>Semua grafik dibuat oleh {@link HtmlChartHelper} &mdash; SVG dan CSS modern,
 *       <b>tanpa JFreeChart</b>. Hasilnya ringan, cepat, dan tampil baik di semua
 *       perangkat termasuk layar retina.</li>
 *   <li>Data diambil menggunakan {@code HibernateUtil.currentSession()} (session
 *       yang sudah terikat pada thread ZK), sehingga tidak perlu membuka/menutup
 *       session secara manual.</li>
 *   <li>Setiap kueri dibungkus dalam blok {@code try-catch} tersendiri. Jika satu
 *       kueri gagal (misalnya karena data biodata kosong), panel terkait akan
 *       menampilkan pesan informatif tanpa memengaruhi panel lainnya.</li>
 *   <li>Kompatibel Java 1.7: tidak menggunakan lambda, try-with-resources,
 *       Stream API, diamond operator, atau fitur Java 8+.</li>
 * </ul>
 *
 * @see DashboardMahasiswa Container tabbox yang memuat kelas ini sebagai tab pertama.
 * @see HtmlChartHelper Utilitas pembuatan grafik HTML/CSS modern.
 * @see DashboardRingkasanSiswa Versi setara untuk data siswa sekolah.
 */
public class DashboardRingkasanMahasiswa extends MyWindow {

    private static final long serialVersionUID = 7811402983401956234L;

    // ══════════════════════════════════════════════════════════════════════
    // Konstanta
    // ══════════════════════════════════════════════════════════════════════

    private static final int MAX_PRODI      = 8;
    private static final int MAX_PROPINSI   = 8;
    private static final int MAX_PEKERJAAN  = 7;
    private static final int MAX_PENDIDIKAN = 7;
    private static final int MAX_RADAR      = 5;

    private static final String BIRU   = "#1877f2";
    private static final String HIJAU  = "#10b981";
    private static final String ORANGE = "#f59e0b";
    private static final String MERAH  = "#e4496b";
    private static final String UNGU   = "#8b5cf6";
    private static final String CYAN   = "#06b6d4";

    // ══════════════════════════════════════════════════════════════════════
    // Field UI
    // ══════════════════════════════════════════════════════════════════════

    private Intbox intDari;
    private Intbox intSampai;
    private Div    center;

    // ══════════════════════════════════════════════════════════════════════
    // Inner data class
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Tipe implementasi bersarang {@link ProdiData} milik {@link DashboardRingkasanMahasiswa}. Kelas ini memberi
     * nama pada state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
     *
     * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link
     * DashboardRingkasanMahasiswa}. Dependensi yang diperlukan harus diberikan secara eksplisit agar aman
     * digunakan dan diuji.</p> Tipe ini merupakan detail implementasi privat; pemanggil luar harus memakai API
     * kelas induk.
     * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code String nama}, {@code int total},
     * {@code int laki}, {@code int prp}. Aturan bisnis bersama tetap berada pada kelas induk atau service yang
     * dipanggilnya.</p>
     *
     * @see DashboardRingkasanMahasiswa
     */
    private static final class ProdiData {
        String nama;
        int    total;
        int    laki;
        int    prp;
        ProdiData(String nama) { this.nama = nama; }
    }

    // ══════════════════════════════════════════════════════════════════════
    // Konstruktor
    // ══════════════════════════════════════════════════════════════════════

    public DashboardRingkasanMahasiswa() {
        super();
        try { init(); } catch (Exception e) { Common.tampilErrorJikaAdmin(e); }
    }

    public DashboardRingkasanMahasiswa(String title, String border, boolean closable) {
        super(title, border, closable);
        try { init(); } catch (Exception e) { Common.tampilErrorJikaAdmin(e); }
    }

    // ══════════════════════════════════════════════════════════════════════
    // Inisialisasi UI
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Membangun antarmuka dasbor: panel filter (saringan) di atas dan
     * area konten grafik di bawah. Data dimuat pertama kali setelah halaman
     * selesai dirender melalui timer, sehingga halaman tidak terasa lambat.
     */
    private void init() throws Exception {
        setWidth("100%");
        setBorder("none");
        setClosable(false);

        org.zkoss.zk.ui.Component[] host = ais.ui.util.DasborResponsifHelper.saringanDanIsi(
                this,
                "Filter Data",
                "Pilih rentang tahun angkatan untuk menyesuaikan data yang ditampilkan.",
                "Dasbor Ringkasan Mahasiswa",
                "Gambaran menyeluruh data mahasiswa dari berbagai sudut pandang: "
                + "gender, tren tahunan, asal daerah, program studi, dan latar belakang keluarga.");
        org.zkoss.zk.ui.Component saringanHost = host[0];
        center = (Div) host[1];

        buildFilter(saringanHost);

        Common.createDefaultTimerNoBusy(new EventListener() {
            @Override
            public void onEvent(Event e) throws Exception {
                reload();
            }
        });
    }

    /**
     * Membangun komponen filter berupa dua kotak angka (tahun dari–s.d.) dan
     * tombol "Muat Ulang". Setiap perubahan pada kotak angka langsung memicu
     * pembaruan data.
     */
    private void buildFilter(org.zkoss.zk.ui.Component parent) {
        int tahunIni = WaktuUtil.getCalendar().get(Calendar.YEAR);

        Grid filterGrid = new Grid();
        filterGrid.setSclass("dgrid fgrid");
        filterGrid.setWidth("100%");
        filterGrid.setParent(parent);

        Rows rows = new Rows();
        rows.setParent(filterGrid);

        MyFormRow row = new MyFormRow();
        row.setValign("top");
        row.setParent(rows);

        row.appendChild(new MyLabelConfig("Tahun Angkatan"));

        Hbox hbox = new Hbox();
        // ZK 9.6: Box tidak lagi punya setValign; penyelarasan vertikal anak Hbox memakai setAlign
        // (sudah "center" di atas), jadi baris setValign lama dihapus agar kompatibel tanpa ubah tampilan.
        hbox.setAlign("center");

        intDari = new Intbox(tahunIni - 5);
        intDari.setCols(5);
        hbox.appendChild(intDari);
        hbox.appendChild(new Label(ais.common.Common.getBahasaConfig(" s.d. ")));
        intSampai = new Intbox(tahunIni);
        intSampai.setCols(5);
        hbox.appendChild(intSampai);
        row.appendChild(hbox);

        MyToolbarbuttonConfig btnMuat = new MyToolbarbuttonConfig("Muat Ulang", "/img/search.gif");
        btnMuat.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event arg0) throws Exception { reload(); }
        });
        row.appendChild(new MyLabelConfig(""));
        row.appendChild(btnMuat);

        EventListener el = new EventListener() {
            @Override
            public void onEvent(Event arg0) throws Exception { reload(); }
        };
        intDari.addEventListener("onChange", el);
        intSampai.addEventListener("onChange", el);
    }

    // ══════════════════════════════════════════════════════════════════════
    // Logika data & render
    // ══════════════════════════════════════════════════════════════════════

    /** Membersihkan konten lama dan memuat ulang semua panel grafik. */
    private void reload() {
        Common.clear(center);
        int dari   = intDari.getValue()   == null ? WaktuUtil.getCalendar().get(Calendar.YEAR) - 5 : intDari.getValue();
        int sampai = intSampai.getValue() == null ? WaktuUtil.getCalendar().get(Calendar.YEAR)     : intSampai.getValue();
        if (dari > sampai) { int t = dari; dari = sampai; sampai = t; }
        doRefresh(dari, sampai);
    }

    /**
     * Menjalankan semua kueri database dan merender seluruh panel grafik ke dalam
     * area konten {@link #center}. Metode ini sengaja menyatukan semua kueri dalam
     * satu aliran kontrol agar pengambilan data terjadi dalam satu transaksi
     * Hibernate yang sama, mengurangi overhead round-trip ke database.
     *
     * <p>Setiap blok kueri dibungkus dalam {@code try-catch} tersendiri sehingga
     * kegagalan satu kueri (misalnya karena relasi biodata belum terisi) tidak
     * menyebabkan seluruh dasbor kosong.</p>
     *
     * @param dari   Tahun angkatan awal (inklusif)
     * @param sampai Tahun angkatan akhir (inklusif)
     */
    @SuppressWarnings("unchecked")
    private void doRefresh(int dari, int sampai) {
        StringBuilder sb = new StringBuilder(20480);

        // ── Header
        sb.append("<div style=\"margin-bottom:16px;\">")
          .append("<div style=\"font-size:20px;font-weight:700;color:#1e3a5f;margin-bottom:4px;\">")
          .append("Dasbor Ringkasan Mahasiswa &mdash; Angkatan ").append(dari)
          .append("&ndash;").append(sampai).append("</div>")
          .append("<div style=\"font-size:13px;color:#6b7280;\">")
          .append("Gambaran menyeluruh data mahasiswa angkatan ").append(dari)
          .append(" hingga ").append(sampai)
          .append(". Semua panel diperbarui secara bersamaan saat filter diubah.")
          .append("</div></div>");

        // ── Query 1: total per gender
        int totalL = 0, totalP = 0;
        try {
            List<Object[]> gRows = (List<Object[]>) HibernateUtil.currentSession()
                    .createQuery("SELECT m.kelamin, COUNT(m) FROM Mahasiswa m "
                            + "WHERE m.tahunangkatan >= :dari AND m.tahunangkatan <= :sampai "
                            + "GROUP BY m.kelamin")
                    .setParameter("dari", dari).setParameter("sampai", sampai).list();
            for (int i = 0; i < gRows.size(); i++) {
                Object[] r = gRows.get(i);
                String kel = r[0] == null ? "" : r[0].toString().toLowerCase(Locale.ROOT);
                int cnt = r[1] == null ? 0 : ((Number) r[1]).intValue();
                if (kel.startsWith("l")) { totalL += cnt; } else { totalP += cnt; }
            }
        } catch (Exception e) { logErr("gender-overall", e); }
        int totalAll = totalL + totalP;

        // ── Query 2: prodi unik
        int prodiUnik = 0;
        try {
            Number n = (Number) HibernateUtil.currentSession()
                    .createQuery("SELECT COUNT(DISTINCT j.id) FROM Mahasiswa m JOIN m.jurusan j "
                            + "WHERE m.tahunangkatan >= :dari AND m.tahunangkatan <= :sampai")
                    .setParameter("dari", dari).setParameter("sampai", sampai).uniqueResult();
            prodiUnik = n == null ? 0 : n.intValue();
        } catch (Exception e) { logErr("prodi-unik", e); }

        // ── Query 3: propinsi unik
        int propinsiUnik = 0;
        try {
            Number n = (Number) HibernateUtil.currentSession()
                    .createQuery("SELECT COUNT(DISTINCT pr.id) FROM BiodataMahasiswa bm "
                            + "JOIN bm.propinsi pr JOIN bm.mahasiswa m "
                            + "WHERE m.tahunangkatan >= :dari AND m.tahunangkatan <= :sampai")
                    .setParameter("dari", dari).setParameter("sampai", sampai).uniqueResult();
            propinsiUnik = n == null ? 0 : n.intValue();
        } catch (Exception e) { logErr("propinsi-unik", e); }

        // ── KPI Cards
        sb.append(HtmlChartHelper.kpiCards(
                new String[] { "Total Mahasiswa", "Laki-laki", "Perempuan",
                        "Program Studi", "Provinsi Asal" },
                new String[] { fmtAngka(totalAll), fmtAngka(totalL), fmtAngka(totalP),
                        fmtAngka(prodiUnik), fmtAngka(propinsiUnik) },
                new String[] {
                        "angkatan " + dari + "–" + sampai,
                        persen(totalL, totalAll) + " dari total mahasiswa",
                        persen(totalP, totalAll) + " dari total mahasiswa",
                        "aktif menerima mahasiswa",
                        "tercatat di biodata" },
                new String[] { "", "", "", "", "" },
                new boolean[] { true, totalL >= totalP, totalP >= totalL, prodiUnik > 0, propinsiUnik > 0 },
                new String[] { BIRU, BIRU, MERAH, HIJAU, ORANGE }));

        if (totalAll == 0) {
            sb.append(fullWidth(emptyCard("Tidak Ada Data",
                    "Tidak ditemukan mahasiswa pada angkatan " + dari + "–" + sampai
                    + ". Perlebar rentang tahun pada filter di atas.")));
            new MyHtml(sb.toString()).setParent(center);
            return;
        }

        // ── Query 4: tren per angkatan + gender (untuk line chart & stacked bar)
        LinkedHashMap<Integer, int[]> trenMap = new LinkedHashMap<Integer, int[]>();
        try {
            List<Object[]> tRows = (List<Object[]>) HibernateUtil.currentSession()
                    .createQuery("SELECT m.tahunangkatan, m.kelamin, COUNT(m) FROM Mahasiswa m "
                            + "WHERE m.tahunangkatan >= :dari AND m.tahunangkatan <= :sampai "
                            + "GROUP BY m.tahunangkatan, m.kelamin ORDER BY m.tahunangkatan")
                    .setParameter("dari", dari).setParameter("sampai", sampai).list();
            for (int i = 0; i < tRows.size(); i++) {
                Object[] r = tRows.get(i);
                int thn = r[0] == null ? 0 : ((Number) r[0]).intValue();
                String kel = r[1] == null ? "" : r[1].toString().toLowerCase(Locale.ROOT);
                int cnt = r[2] == null ? 0 : ((Number) r[2]).intValue();
                if (!trenMap.containsKey(Integer.valueOf(thn))) {
                    trenMap.put(Integer.valueOf(thn), new int[3]);
                }
                int[] arr = trenMap.get(Integer.valueOf(thn));
                arr[0] += cnt;
                if (kel.startsWith("l")) { arr[1] += cnt; } else { arr[2] += cnt; }
            }
        } catch (Exception e) { logErr("tren-gender", e); }

        // ── Row 1: Donat gender | Stacked gender per angkatan
        sb.append(grid2col(
                buildDonutGender(totalL, totalP),
                buildStackedGender(trenMap)));

        // ── Row 2: Line chart tren penerimaan (full width)
        sb.append(fullWidth(buildLineTren(trenMap, dari, sampai)));

        // ── Query 5: top prodi
        List<Object[]> prodiRows = new ArrayList<Object[]>();
        try {
            prodiRows = (List<Object[]>) HibernateUtil.currentSession()
                    .createQuery("SELECT j.nama, COUNT(m) FROM Mahasiswa m JOIN m.jurusan j "
                            + "WHERE m.tahunangkatan >= :dari AND m.tahunangkatan <= :sampai "
                            + "GROUP BY j.id, j.nama ORDER BY COUNT(m) DESC")
                    .setParameter("dari", dari).setParameter("sampai", sampai)
                    .setMaxResults(MAX_PRODI).list();
        } catch (Exception e) { logErr("top-prodi", e); }

        // ── Query 6: top propinsi
        List<Object[]> propRows = new ArrayList<Object[]>();
        try {
            propRows = (List<Object[]>) HibernateUtil.currentSession()
                    .createQuery("SELECT pr.nama, COUNT(bm) FROM BiodataMahasiswa bm "
                            + "JOIN bm.propinsi pr JOIN bm.mahasiswa m "
                            + "WHERE m.tahunangkatan >= :dari AND m.tahunangkatan <= :sampai "
                            + "GROUP BY pr.id, pr.nama ORDER BY COUNT(bm) DESC")
                    .setParameter("dari", dari).setParameter("sampai", sampai)
                    .setMaxResults(MAX_PROPINSI).list();
        } catch (Exception e) { logErr("top-propinsi", e); }

        // ── Row 3: Bar prodi | Bar propinsi (2 kolom)
        sb.append(grid2col(buildBarProdi(prodiRows), buildBarPropinsi(propRows)));

        // ── Query 7: prodi + gender (untuk radar)
        List<Object[]> radarRows = new ArrayList<Object[]>();
        try {
            radarRows = (List<Object[]>) HibernateUtil.currentSession()
                    .createQuery("SELECT j.nama, m.kelamin, COUNT(m) FROM Mahasiswa m JOIN m.jurusan j "
                            + "WHERE m.tahunangkatan >= :dari AND m.tahunangkatan <= :sampai "
                            + "GROUP BY j.id, j.nama, m.kelamin ORDER BY COUNT(m) DESC")
                    .setParameter("dari", dari).setParameter("sampai", sampai).list();
        } catch (Exception e) { logErr("radar-data", e); }

        // ── Row 4: Radar per prodi (full width)
        String radar = buildRadar(radarRows);
        if (radar != null) { sb.append(fullWidth(radar)); }

        // ── Query 8: pekerjaan ortu ayah
        List<Object[]> pekerjaanRows = new ArrayList<Object[]>();
        try {
            pekerjaanRows = (List<Object[]>) HibernateUtil.currentSession()
                    .createQuery("SELECT pj.nama, COUNT(bm) FROM BiodataMahasiswa bm "
                            + "JOIN bm.jenisPekerjaanAyah pj JOIN bm.mahasiswa m "
                            + "WHERE m.tahunangkatan >= :dari AND m.tahunangkatan <= :sampai "
                            + "GROUP BY pj.id, pj.nama ORDER BY COUNT(bm) DESC")
                    .setParameter("dari", dari).setParameter("sampai", sampai)
                    .setMaxResults(MAX_PEKERJAAN).list();
        } catch (Exception e) { logErr("pekerjaan-ortu", e); }

        // ── Query 9: pendidikan ortu ayah
        List<Object[]> pendidikanRows = new ArrayList<Object[]>();
        try {
            pendidikanRows = (List<Object[]>) HibernateUtil.currentSession()
                    .createQuery("SELECT pd.nama, COUNT(bm) FROM BiodataMahasiswa bm "
                            + "JOIN bm.pendidikanAyah pd JOIN bm.mahasiswa m "
                            + "WHERE m.tahunangkatan >= :dari AND m.tahunangkatan <= :sampai "
                            + "GROUP BY pd.id, pd.nama ORDER BY COUNT(bm) DESC")
                    .setParameter("dari", dari).setParameter("sampai", sampai)
                    .setMaxResults(MAX_PENDIDIKAN).list();
        } catch (Exception e) { logErr("pendidikan-ortu", e); }

        // ── Row 5: Donat pekerjaan | Bar pendidikan (2 kolom)
        sb.append(grid2col(buildDonutPekerjaan(pekerjaanRows), buildBarPendidikan(pendidikanRows)));

        // ── Footer spacer
        sb.append("<div style=\"height:20px;\"></div>");

        new MyHtml(sb.toString()).setParent(center);
    }

    // ══════════════════════════════════════════════════════════════════════
    // Build methods — satu metode per panel
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Membangun diagram lingkaran (donat) komposisi jenis kelamin.
     * Angka persentase kelompok terbesar ditampilkan di tengah lingkaran.
     */
    private String buildDonutGender(int totalL, int totalP) {
        int total = totalL + totalP;
        return HtmlChartHelper.donut(
                "Komposisi Jenis Kelamin",
                "Perbandingan mahasiswa laki-laki dan perempuan. "
                + "Lingkaran ini memperlihatkan berapa persen dari masing-masing kelompok. "
                + "Berguna untuk memantau keseimbangan gender dari tahun ke tahun.",
                new String[] { "Laki-laki", "Perempuan" },
                new double[] { totalL, totalP },
                new String[] { BIRU, MERAH },
                total > 0 ? persen(Math.max(totalL, totalP), total) : "–");
    }

    /**
     * Membangun batang bertumpuk (stacked bar) komposisi gender per tahun angkatan.
     * Setiap batang mewakili satu angkatan, terbagi antara laki-laki dan perempuan.
     */
    private String buildStackedGender(LinkedHashMap<Integer, int[]> trenMap) {
        if (trenMap.isEmpty()) {
            return emptyCard("Gender per Angkatan", "Belum ada data tren gender per angkatan.");
        }
        List<Integer> tahuns = new ArrayList<Integer>(trenMap.keySet());
        int n = tahuns.size();
        String[]   cats = new String[n];
        double[][] vals = new double[n][2];
        for (int i = 0; i < n; i++) {
            cats[i] = String.valueOf(tahuns.get(i));
            int[] arr = trenMap.get(tahuns.get(i));
            vals[i][0] = arr[1];
            vals[i][1] = arr[2];
        }
        return HtmlChartHelper.stackedBar(
                "Komposisi Gender per Angkatan",
                "Setiap batang mewakili satu angkatan. Bagian biru = laki-laki, merah muda = perempuan. "
                + "Pergeseran warna dari tahun ke tahun menunjukkan perubahan komposisi gender mahasiswa baru.",
                cats,
                new String[] { "Laki-laki", "Perempuan" },
                vals,
                new String[] { BIRU, MERAH });
    }

    /**
     * Membangun grafik garis tren jumlah mahasiswa per tahun angkatan,
     * dengan tiga seri: total, laki-laki, dan perempuan.
     */
    private String buildLineTren(LinkedHashMap<Integer, int[]> trenMap, int dari, int sampai) {
        if (trenMap.isEmpty()) {
            return emptyCard("Tren Penerimaan",
                    "Belum ada data mahasiswa pada angkatan " + dari + "–" + sampai + ".");
        }
        List<Integer> tahuns = new ArrayList<Integer>(trenMap.keySet());
        int n = tahuns.size();
        String[]   cats = new String[n];
        double[][] vals = new double[3][n];
        for (int i = 0; i < n; i++) {
            cats[i] = String.valueOf(tahuns.get(i));
            int[] arr = trenMap.get(tahuns.get(i));
            vals[0][i] = arr[0];
            vals[1][i] = arr[1];
            vals[2][i] = arr[2];
        }
        return HtmlChartHelper.lineMulti(
                "Tren Jumlah Mahasiswa per Tahun Angkatan",
                "Garis ini menunjukkan apakah jumlah mahasiswa baru meningkat, stabil, atau menurun "
                + "dari satu angkatan ke angkatan berikutnya. "
                + "Tren naik berarti promosi berhasil; tren turun adalah sinyal evaluasi strategi penerimaan.",
                cats,
                new String[] { "Total", "Laki-laki", "Perempuan" },
                vals,
                new String[] { UNGU, BIRU, MERAH });
    }

    /** Membangun batang horizontal top program studi berdasarkan jumlah mahasiswa. */
    private String buildBarProdi(List<Object[]> rows) {
        if (rows.isEmpty()) {
            return emptyCard("Program Studi", "Belum ada data prodi mahasiswa pada rentang angkatan ini.");
        }
        int n = rows.size();
        String[]  lbl = new String[n];
        double[]  val = new double[n];
        for (int i = 0; i < n; i++) {
            Object[] r = rows.get(i);
            lbl[i] = abbrev(r[0] == null ? "-" : r[0].toString(), 30);
            val[i] = r[1] == null ? 0 : ((Number) r[1]).intValue();
        }
        return HtmlChartHelper.barHorizontal(
                "Program Studi Terbanyak Mahasiswanya",
                "Program studi diurutkan dari yang paling banyak mahasiswanya. "
                + "Semakin panjang batangnya, semakin banyak mahasiswanya. "
                + "Berguna untuk perencanaan alokasi dosen dan ruang kuliah.",
                lbl, val, BIRU);
    }

    /** Membangun batang horizontal top provinsi asal mahasiswa. */
    private String buildBarPropinsi(List<Object[]> rows) {
        if (rows.isEmpty()) {
            return emptyCard("Provinsi Asal",
                    "Data provinsi belum terisi di biodata mahasiswa. "
                    + "Minta mahasiswa melengkapi biodata mereka terlebih dahulu.");
        }
        int n = rows.size();
        String[]  lbl = new String[n];
        double[]  val = new double[n];
        for (int i = 0; i < n; i++) {
            Object[] r = rows.get(i);
            lbl[i] = abbrev(r[0] == null ? "-" : r[0].toString(), 22);
            val[i] = r[1] == null ? 0 : ((Number) r[1]).intValue();
        }
        return HtmlChartHelper.barHorizontal(
                "Provinsi Asal Mahasiswa Terbanyak",
                "Dari provinsi mana saja mahasiswa kita berasal? "
                + "Provinsi yang belum banyak terwakili adalah peluang ekspansi promosi "
                + "ke SMA/SMK di wilayah tersebut.",
                lbl, val, HIJAU);
    }

    /**
     * Membangun grafik radar (jaring laba-laba) yang membandingkan
     * top-{@value #MAX_RADAR} program studi dari tiga dimensi:
     * jumlah total, persentase laki-laki, dan persentase perempuan.
     *
     * @return HTML grafik radar, atau {@code null} jika data tidak cukup
     */
    private String buildRadar(List<Object[]> radarRows) {
        LinkedHashMap<String, ProdiData> map = new LinkedHashMap<String, ProdiData>();
        for (int i = 0; i < radarRows.size(); i++) {
            Object[] r  = radarRows.get(i);
            String nm   = r[0] == null ? "–" : r[0].toString();
            String kel  = r[1] == null ? "" : r[1].toString().toLowerCase(Locale.ROOT);
            int    cnt  = r[2] == null ? 0 : ((Number) r[2]).intValue();
            if (!map.containsKey(nm)) { map.put(nm, new ProdiData(nm)); }
            ProdiData d = map.get(nm);
            d.total += cnt;
            if (kel.startsWith("l")) { d.laki += cnt; } else { d.prp += cnt; }
        }
        if (map.size() < 2) { return null; }

        List<ProdiData> top = new ArrayList<ProdiData>(map.values());
        // Bubble sort descending by total (Java 1.7 compatible)
        for (int i = 0; i < top.size() - 1; i++) {
            for (int j = 0; j < top.size() - 1 - i; j++) {
                if (top.get(j).total < top.get(j + 1).total) {
                    ProdiData tmp = top.get(j);
                    top.set(j, top.get(j + 1));
                    top.set(j + 1, tmp);
                }
            }
        }
        int nSeries = Math.min(top.size(), MAX_RADAR);
        String[]   labels = new String[nSeries];
        double[][] vals   = new double[nSeries][3];
        for (int i = 0; i < nSeries; i++) {
            ProdiData d = top.get(i);
            labels[i]  = abbrev(d.nama, 18);
            vals[i][0] = d.total;
            vals[i][1] = d.total > 0 ? (100.0 * d.laki / d.total) : 0;
            vals[i][2] = d.total > 0 ? (100.0 * d.prp  / d.total) : 0;
        }
        return HtmlChartHelper.radar(
                "Perbandingan Program Studi — Grafik Jaring Laba-laba",
                "Setiap sudut mewakili satu ukuran: jumlah total mahasiswa, persentase laki-laki, "
                + "dan persentase perempuan. Semakin luas area suatu prodi, semakin besar nilainya. "
                + "Grafik ini memperlihatkan 'bentuk' profil tiap prodi secara sekaligus "
                + "— berbeda dari batang biasa yang hanya membandingkan satu ukuran.",
                new String[] { "Jumlah Total", "% Laki-laki", "% Perempuan" },
                labels,
                vals,
                new String[] { BIRU, HIJAU, ORANGE, UNGU, CYAN },
                0);
    }

    /** Membangun diagram lingkaran (donat) distribusi pekerjaan orang tua. */
    private String buildDonutPekerjaan(List<Object[]> rows) {
        if (rows.isEmpty()) {
            return emptyCard("Pekerjaan Orang Tua",
                    "Data pekerjaan orang tua belum terisi di biodata mahasiswa.");
        }
        int n = rows.size();
        String[]  lbl  = new String[n];
        double[]  val  = new double[n];
        String[]  colorPalette = { BIRU, HIJAU, ORANGE, MERAH, UNGU, CYAN, "#f97316", "#ec4899" };
        String[]  clrs = new String[n];
        for (int i = 0; i < n; i++) {
            Object[] r = rows.get(i);
            lbl[i]  = abbrev(r[0] == null ? "–" : r[0].toString(), 22);
            val[i]  = r[1] == null ? 0 : ((Number) r[1]).intValue();
            clrs[i] = colorPalette[i % colorPalette.length];
        }
        return HtmlChartHelper.donut(
                "Pekerjaan Orang Tua Mahasiswa",
                "Jenis pekerjaan ayah mahasiswa yang paling banyak. "
                + "Berguna untuk merancang program beasiswa tepat sasaran "
                + "dan memilih mitra industri yang relevan dengan latar belakang keluarga mahasiswa.",
                lbl, val, clrs,
                lbl.length > 0 ? lbl[0] : "–");
    }

    /** Membangun batang horizontal distribusi tingkat pendidikan orang tua. */
    private String buildBarPendidikan(List<Object[]> rows) {
        if (rows.isEmpty()) {
            return emptyCard("Pendidikan Orang Tua",
                    "Data pendidikan orang tua belum terisi di biodata mahasiswa.");
        }
        int n = rows.size();
        String[]  lbl = new String[n];
        double[]  val = new double[n];
        for (int i = 0; i < n; i++) {
            Object[] r = rows.get(i);
            lbl[i] = abbrev(r[0] == null ? "–" : r[0].toString(), 22);
            val[i] = r[1] == null ? 0 : ((Number) r[1]).intValue();
        }
        return HtmlChartHelper.barHorizontal(
                "Tingkat Pendidikan Orang Tua Mahasiswa",
                "Pendidikan formal tertinggi ayah mahasiswa. "
                + "Berguna untuk program 'generasi pertama sarjana di keluarga' "
                + "dan layanan bimbingan akademik bagi mahasiswa yang butuh dukungan ekstra.",
                lbl, val, ORANGE);
    }

    // ══════════════════════════════════════════════════════════════════════
    // Utilitas format & HTML
    // ══════════════════════════════════════════════════════════════════════

    private String fmtAngka(int n) {
        DecimalFormatSymbols dfs = new DecimalFormatSymbols(new Locale("id", "ID"));
        dfs.setGroupingSeparator('.');
        return new DecimalFormat("#,###", dfs).format(n);
    }

    private String persen(int num, int denom) {
        if (denom == 0) { return "0%"; }
        return new DecimalFormat("0.#").format(num * 100.0 / denom) + "%";
    }

    /** Bungkus HTML dalam div full-width dengan margin atas 14px. */
    private String fullWidth(String html) {
        return "<div style=\"margin-top:14px;\">" + html + "</div>";
    }

    /**
     * Layout dua kolom responsif menggunakan CSS Grid.
     * Di layar lebar keduanya berdampingan; di layar sempit (HP) menumpuk vertikal.
     */
    private String grid2col(String left, String right) {
        return "<div style=\"display:grid;"
                + "grid-template-columns:repeat(auto-fit,minmax(280px,1fr));"
                + "gap:14px;margin-top:14px;\">"
                + "<div>" + left + "</div>"
                + "<div>" + right + "</div>"
                + "</div>";
    }

    /** Kartu kosong dengan pesan informatif ketika data tidak tersedia. */
    private String emptyCard(String judul, String pesan) {
        return "<div style=\"background:#f9fafb;border:1.5px dashed #d1d5db;"
                + "border-radius:10px;padding:24px;text-align:center;\">"
                + "<div style=\"font-size:15px;font-weight:600;color:#374151;margin-bottom:6px;\">"
                + escHtml(judul) + "</div>"
                + "<div style=\"font-size:13px;color:#9ca3af;\">" + escHtml(pesan) + "</div>"
                + "</div>";
    }

    private String abbrev(String s, int max) {
        if (s == null) { return "–"; }
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }

    private String escHtml(String s) {
        if (s == null) { return ""; }
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private void logErr(String ctx, Exception e) {
        System.err.println("[DashboardRingkasanMahasiswa] Error " + ctx + ": " + e.getMessage());
    }
}
