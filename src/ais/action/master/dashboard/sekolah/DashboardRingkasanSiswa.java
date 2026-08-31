package ais.action.master.dashboard.sekolah;

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
 * <h1>DashboardRingkasanSiswa &mdash; Dasbor Komprehensif Statistik Siswa</h1>
 *
 * <p>Kelas ini menyajikan gambaran besar data <b>siswa aktif yang sudah terdaftar</b>
 * di sekolah (bukan calon siswa / PSB). Dasbor ini tampil sebagai tab pertama
 * "Dasbor" di dalam {@link DasboardSiswa}, sehingga saat staf membuka Statistik
 * Siswa, halaman pertama yang mereka lihat adalah ringkasan terpadu ini &mdash;
 * bukan kumpulan angka mentah yang harus dihitung sendiri.</p>
 *
 * <h2>Untuk Siapa Dasbor Ini?</h2>
 * <ul>
 *   <li><b>Kepala Sekolah</b> — memantau jumlah siswa, rasio gender, dan tren
 *       penerimaan dari satu layar tanpa perlu membuka tab-tab terpisah.</li>
 *   <li><b>Bagian Kesiswaan</b> — melihat distribusi siswa per sekolah dan per
 *       tahun masuk untuk perencanaan kelas dan kebutuhan guru.</li>
 *   <li><b>Pimpinan Yayasan</b> — perbandingan antar-sekolah dalam satu grafik
 *       radar untuk evaluasi kinerja penerimaan siswa baru.</li>
 * </ul>
 *
 * <h2>Panel yang Ditampilkan (Berurutan dari Atas ke Bawah)</h2>
 * <ol>
 *   <li><b>Kartu Angka Utama (KPI)</b> &mdash; Lima angka besar yang langsung
 *       menjawab "berapa?": total siswa, jumlah laki-laki, jumlah perempuan,
 *       jumlah sekolah yang aktif menerima siswa dalam rentang tahun masuk, dan
 *       persentase siswa yang masih aktif. Warna kartu membantu membedakan status:
 *       biru untuk total, hijau untuk kondisi baik, merah untuk perlu perhatian.</li>
 *
 *   <li><b>Komposisi Jenis Kelamin (Diagram Donat)</b> &mdash; Lingkaran yang
 *       terbagi antara laki-laki dan perempuan. Angka besar di tengah menunjukkan
 *       persentase kelompok yang lebih dominan. Berguna untuk memantau apakah
 *       sekolah berhasil menarik minat siswa dari kedua kelompok secara seimbang.</li>
 *
 *   <li><b>Komposisi Gender per Tahun Masuk (Batang Bertumpuk)</b> &mdash; Satu
 *       batang per tahun masuk, terbagi antara laki-laki (biru) dan perempuan
 *       (merah muda). Memperlihatkan apakah komposisi gender siswa baru berubah
 *       dari satu tahun ke tahun berikutnya &mdash; misalnya, apakah promosi ke
 *       SMP perempuan mulai berdampak.</li>
 *
 *   <li><b>Tren Penerimaan Siswa per Tahun (Grafik Garis)</b> &mdash; Tiga garis
 *       dalam satu grafik: total, laki-laki, dan perempuan per tahun masuk. Tren
 *       naik berarti penerimaan berhasil meningkat; tren turun adalah sinyal
 *       bahwa strategi promosi perlu dievaluasi segera. Grafik ini adalah alat
 *       utama untuk laporan penerimaan siswa baru kepada pengurus yayasan.</li>
 *
 *   <li><b>Jumlah Siswa per Sekolah (Batang Horizontal)</b> &mdash; Setiap batang
 *       mewakili satu sekolah. Semakin panjang batangnya, semakin banyak siswanya.
 *       Membantu pimpinan yayasan mengetahui sekolah mana yang perlu penambahan
 *       kelas atau guru, dan sekolah mana yang masih punya ruang untuk dikembangkan.</li>
 *
 *   <li><b>Perbandingan Sekolah (Grafik Jaring Laba-laba / Radar)</b> &mdash;
 *       Grafik yang membandingkan sekolah-sekolah dari tiga sudut pandang sekaligus:
 *       jumlah total siswa, persentase laki-laki, dan persentase perempuan.
 *       Berbeda dari batang biasa yang hanya membandingkan satu ukuran, grafik
 *       radar memperlihatkan "bentuk" profil tiap sekolah secara holistik.
 *       Panel ini hanya muncul jika ada minimal dua sekolah yang terdaftar.</li>
 *
 *   <li><b>Status Aktif vs Non-aktif (Diagram Donat)</b> &mdash; Berapa persen
 *       siswa yang masih berstatus aktif dibanding yang sudah tidak aktif (keluar,
 *       lulus lebih awal, atau sebab lain). Rasio aktif yang tinggi menunjukkan
 *       tingkat retensi yang baik &mdash; siswa yang masuk tidak banyak yang
 *       keluar di tengah jalan.</li>
 * </ol>
 *
 * <h2>Filter Data</h2>
 * <p>Semua panel dapat disaring berdasarkan <b>rentang tahun masuk</b>. Secara
 * bawaan, dasbor menampilkan 5 tahun terakhir. Ubah nilai kotak angka lalu
 * klik "Muat Ulang" (atau tekan Enter) untuk memperbarui semua panel sekaligus.</p>
 *
 * <h2>Catatan Teknis</h2>
 * <ul>
 *   <li>Kelas ini menggunakan {@code ais.database.hibernate.HibernateUtil.currentSession()}
 *       &mdash; session Hibernate yang sudah terikat pada thread ZK, sehingga
 *       tidak perlu membuka atau menutup session secara manual.</li>
 *   <li>Kueri gender dibungkus {@code try-catch} terpisah karena field
 *       {@code Siswa.kelamin} mungkin tidak ada di semua implementasi. Jika
 *       field tidak tersedia, panel gender akan menampilkan pesan informatif.</li>
 *   <li>Seluruh grafik dirender oleh {@link HtmlChartHelper} menggunakan SVG
 *       dan CSS modern &mdash; <b>tanpa JFreeChart</b>. Ringan, cepat, dan
 *       tampil baik di semua perangkat termasuk layar HP.</li>
 *   <li>Kompatibel Java 1.7: tidak ada lambda, try-with-resources, Stream API,
 *       diamond operator, atau fitur Java 8+.</li>
 * </ul>
 *
 * @see DasboardSiswa       Container tabbox yang memuat kelas ini sebagai tab pertama.
 * @see HtmlChartHelper     Utilitas pembuatan grafik HTML/CSS modern.
 * @see DashboardRingkasanMahasiswa Versi setara untuk data mahasiswa perguruan tinggi.
 */
public class DashboardRingkasanSiswa extends MyWindow {

    private static final long serialVersionUID = -4412038920163498721L;

    // ══════════════════════════════════════════════════════════════════════
    // Konstanta
    // ══════════════════════════════════════════════════════════════════════

    private static final int MAX_SEKOLAH = 8;
    private static final int MAX_RADAR   = 5;

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
     * Tipe implementasi bersarang {@link SekolahData} milik {@link DashboardRingkasanSiswa}. Kelas ini memberi
     * nama pada state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
     *
     * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link
     * DashboardRingkasanSiswa}. Dependensi yang diperlukan harus diberikan secara eksplisit agar aman digunakan
     * dan diuji.</p> Tipe ini merupakan detail implementasi privat; pemanggil luar harus memakai API kelas induk.
     * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code String nama}, {@code int total},
     * {@code int laki}, {@code int prp}. Aturan bisnis bersama tetap berada pada kelas induk atau service yang
     * dipanggilnya.</p>
     *
     * @see DashboardRingkasanSiswa
     */
    private static final class SekolahData {
        String nama;
        int    total;
        int    laki;
        int    prp;
        SekolahData(String nama) { this.nama = nama; }
    }

    // ══════════════════════════════════════════════════════════════════════
    // Konstruktor
    // ══════════════════════════════════════════════════════════════════════

    public DashboardRingkasanSiswa() {
        super();
        try { init(); } catch (Exception e) { Common.tampilErrorJikaAdmin(e); }
    }

    public DashboardRingkasanSiswa(String title, String border, boolean closable) {
        super(title, border, closable);
        try { init(); } catch (Exception e) { Common.tampilErrorJikaAdmin(e); }
    }

    // ══════════════════════════════════════════════════════════════════════
    // Inisialisasi UI
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Membangun antarmuka dasbor: panel filter di atas dan area konten di bawah.
     * Data dimuat pertama kali setelah halaman selesai dirender melalui timer,
     * sehingga halaman tidak terasa lambat saat pertama dibuka.
     */
    private void init() throws Exception {
        setWidth("100%");
        setBorder("none");
        setClosable(false);

        org.zkoss.zk.ui.Component[] host = ais.ui.util.DasborResponsifHelper.saringanDanIsi(
                this,
                "Filter Data",
                "Pilih rentang tahun masuk untuk menyesuaikan data yang ditampilkan.",
                "Dasbor Ringkasan Siswa",
                "Gambaran menyeluruh data siswa dari berbagai sudut pandang: "
                + "gender, tren tahunan, distribusi per sekolah, dan status keaktifan.");
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
     * Membangun komponen filter: dua kotak angka tahun masuk (dari–s.d.)
     * dan tombol "Muat Ulang". Perubahan pada kotak angka memicu pembaruan data.
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

        row.appendChild(new MyLabelConfig("Tahun Masuk"));

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
     * Menjalankan semua kueri database dan merender seluruh panel grafik.
     * Setiap blok kueri dibungkus {@code try-catch} tersendiri sehingga kegagalan
     * satu kueri tidak menyebabkan seluruh dasbor kosong.
     *
     * @param dari   Tahun masuk awal (inklusif)
     * @param sampai Tahun masuk akhir (inklusif)
     */
    @SuppressWarnings("unchecked")
    private void doRefresh(int dari, int sampai) {
        StringBuilder sb = new StringBuilder(16384);

        // ── Header
        sb.append("<div style=\"margin-bottom:16px;\">")
          .append("<div style=\"font-size:20px;font-weight:700;color:#1e3a5f;margin-bottom:4px;\">")
          .append("Dasbor Ringkasan Siswa &mdash; Tahun Masuk ").append(dari)
          .append("&ndash;").append(sampai).append("</div>")
          .append("<div style=\"font-size:13px;color:#6b7280;\">")
          .append("Gambaran menyeluruh data siswa tahun masuk ").append(dari)
          .append(" hingga ").append(sampai)
          .append(". Semua panel diperbarui saat filter diubah.")
          .append("</div></div>");

        // ── Query 1: total dan gender
        int totalL = 0, totalP = 0, totalGenderGagal = 0;
        try {
            List<Object[]> gRows = (List<Object[]>) HibernateUtil.currentSession()
                    .createQuery("SELECT s.jenisKelamin, COUNT(s) FROM Siswa s "
                            + "WHERE s.tahunMasuk >= :dari AND s.tahunMasuk <= :sampai "
                            + "GROUP BY s.jenisKelamin")
                    .setParameter("dari", dari).setParameter("sampai", sampai).list();
            for (int i = 0; i < gRows.size(); i++) {
                Object[] r  = gRows.get(i);
                String kel  = r[0] == null ? "" : r[0].toString().toLowerCase(Locale.ROOT);
                int    cnt  = r[1] == null ? 0 : ((Number) r[1]).intValue();
                if (kel.startsWith("l")) { totalL += cnt; } else if (kel.startsWith("p") || kel.startsWith("w")) { totalP += cnt; }
                else { totalGenderGagal += cnt; }
            }
        } catch (Exception e) {
            logErr("gender-overall", e);
        }
        int totalAll = totalL + totalP + totalGenderGagal;

        // ── Query 2: total fallback jika gender gagal
        if (totalAll == 0) {
            try {
                Number n = (Number) HibernateUtil.currentSession()
                        .createQuery("SELECT COUNT(s) FROM Siswa s "
                                + "WHERE s.tahunMasuk >= :dari AND s.tahunMasuk <= :sampai")
                        .setParameter("dari", dari).setParameter("sampai", sampai).uniqueResult();
                totalAll = n == null ? 0 : n.intValue();
            } catch (Exception e) { logErr("total-fallback", e); }
        }

        // ── Query 3: sekolah unik
        int sekolahUnik = 0;
        try {
            Number n = (Number) HibernateUtil.currentSession()
                    .createQuery("SELECT COUNT(DISTINCT s.sekolah.id) FROM Siswa s "
                            + "WHERE s.tahunMasuk >= :dari AND s.tahunMasuk <= :sampai")
                    .setParameter("dari", dari).setParameter("sampai", sampai).uniqueResult();
            sekolahUnik = n == null ? 0 : n.intValue();
        } catch (Exception e) { logErr("sekolah-unik", e); }

        // ── Query 4: siswa aktif
        int totalAktif = 0;
        try {
            Number n = (Number) HibernateUtil.currentSession()
                    .createQuery("SELECT COUNT(s) FROM Siswa s "
                            + "WHERE s.tahunMasuk >= :dari AND s.tahunMasuk <= :sampai "
                            + "AND s.aktif = true")
                    .setParameter("dari", dari).setParameter("sampai", sampai).uniqueResult();
            totalAktif = n == null ? 0 : n.intValue();
        } catch (Exception e) { logErr("siswa-aktif", e); }

        // ── KPI Cards
        sb.append(HtmlChartHelper.kpiCards(
                new String[] { "Total Siswa", "Laki-laki", "Perempuan",
                        "Sekolah", "Masih Aktif" },
                new String[] { fmtAngka(totalAll), fmtAngka(totalL), fmtAngka(totalP),
                        fmtAngka(sekolahUnik), persen(totalAktif, totalAll) },
                new String[] {
                        "tahun masuk " + dari + "–" + sampai,
                        totalAll > 0 ? persen(totalL, totalAll) + " dari total siswa" : "data gender belum tersedia",
                        totalAll > 0 ? persen(totalP, totalAll) + " dari total siswa" : "data gender belum tersedia",
                        "menerima siswa dalam rentang ini",
                        fmtAngka(totalAktif) + " dari " + fmtAngka(totalAll) + " siswa" },
                new String[] { "", "", "", "", "" },
                new boolean[] { true, totalL > 0, totalP > 0, sekolahUnik > 0,
                        totalAktif > 0 && totalAktif == totalAll },
                new String[] { BIRU, BIRU, MERAH, HIJAU, HIJAU }));

        if (totalAll == 0) {
            sb.append(fullWidth(emptyCard("Tidak Ada Data",
                    "Tidak ditemukan siswa pada tahun masuk " + dari + "–" + sampai
                    + ". Perlebar rentang tahun pada filter di atas.")));
            new MyHtml(sb.toString()).setParent(center);
            return;
        }

        // ── Query 5: tren per tahun masuk + gender
        LinkedHashMap<Integer, int[]> trenMap = new LinkedHashMap<Integer, int[]>();
        boolean adaDataGender = (totalL + totalP) > 0;
        if (adaDataGender) {
            try {
                List<Object[]> tRows = (List<Object[]>) HibernateUtil.currentSession()
                        .createQuery("SELECT s.tahunMasuk, s.jenisKelamin, COUNT(s) FROM Siswa s "
                                + "WHERE s.tahunMasuk >= :dari AND s.tahunMasuk <= :sampai "
                                + "GROUP BY s.tahunMasuk, s.jenisKelamin ORDER BY s.tahunMasuk")
                        .setParameter("dari", dari).setParameter("sampai", sampai).list();
                for (int i = 0; i < tRows.size(); i++) {
                    Object[] r = tRows.get(i);
                    int thn    = r[0] == null ? 0 : ((Number) r[0]).intValue();
                    String kel = r[1] == null ? "" : r[1].toString().toLowerCase(Locale.ROOT);
                    int cnt    = r[2] == null ? 0 : ((Number) r[2]).intValue();
                    if (!trenMap.containsKey(Integer.valueOf(thn))) {
                        trenMap.put(Integer.valueOf(thn), new int[3]);
                    }
                    int[] arr = trenMap.get(Integer.valueOf(thn));
                    arr[0] += cnt;
                    if (kel.startsWith("l")) { arr[1] += cnt; }
                    else if (kel.startsWith("p") || kel.startsWith("w")) { arr[2] += cnt; }
                }
            } catch (Exception e) { logErr("tren-gender", e); }
        }

        // ── Tren per tahun tanpa gender (fallback)
        LinkedHashMap<Integer, Integer> trenSimple = new LinkedHashMap<Integer, Integer>();
        if (!adaDataGender || trenMap.isEmpty()) {
            try {
                List<Object[]> tRows = (List<Object[]>) HibernateUtil.currentSession()
                        .createQuery("SELECT s.tahunMasuk, COUNT(s) FROM Siswa s "
                                + "WHERE s.tahunMasuk >= :dari AND s.tahunMasuk <= :sampai "
                                + "GROUP BY s.tahunMasuk ORDER BY s.tahunMasuk")
                        .setParameter("dari", dari).setParameter("sampai", sampai).list();
                for (int i = 0; i < tRows.size(); i++) {
                    Object[] r = tRows.get(i);
                    int thn    = r[0] == null ? 0 : ((Number) r[0]).intValue();
                    int cnt    = r[1] == null ? 0 : ((Number) r[1]).intValue();
                    trenSimple.put(Integer.valueOf(thn), Integer.valueOf(cnt));
                }
            } catch (Exception e) { logErr("tren-simple", e); }
        }

        // ── Row 1: Donat gender | Stacked gender per tahun (atau tren saja)
        String panelKiri, panelKanan;
        if (adaDataGender && totalL + totalP > 0) {
            panelKiri  = buildDonutGender(totalL, totalP);
            panelKanan = buildStackedGender(trenMap);
        } else {
            panelKiri  = emptyCard("Komposisi Gender",
                    "Data jenis kelamin siswa belum tersedia. "
                    + "Pastikan data siswa sudah diisi lengkap di form biodata.");
            panelKanan = buildLineTrenSimple(trenSimple, dari, sampai);
        }
        sb.append(grid2col(panelKiri, panelKanan));

        // ── Row 2: Tren garis (full width, hanya jika ada data gender)
        if (adaDataGender && !trenMap.isEmpty()) {
            sb.append(fullWidth(buildLineTren(trenMap, dari, sampai)));
        } else if (!trenSimple.isEmpty()) {
            sb.append(fullWidth(buildLineTrenSimple(trenSimple, dari, sampai)));
        }

        // ── Query 6: per sekolah
        List<Object[]> sekolahRows = new ArrayList<Object[]>();
        try {
            sekolahRows = (List<Object[]>) HibernateUtil.currentSession()
                    .createQuery("SELECT s.sekolah.nama, COUNT(s) FROM Siswa s "
                            + "WHERE s.tahunMasuk >= :dari AND s.tahunMasuk <= :sampai "
                            + "GROUP BY s.sekolah.id, s.sekolah.nama ORDER BY COUNT(s) DESC")
                    .setParameter("dari", dari).setParameter("sampai", sampai)
                    .setMaxResults(MAX_SEKOLAH).list();
        } catch (Exception e) { logErr("per-sekolah", e); }

        // ── Query 7: per sekolah + gender untuk radar
        List<Object[]> radarRows = new ArrayList<Object[]>();
        if (adaDataGender) {
            try {
                radarRows = (List<Object[]>) HibernateUtil.currentSession()
                        .createQuery("SELECT s.sekolah.nama, s.jenisKelamin, COUNT(s) FROM Siswa s "
                                + "WHERE s.tahunMasuk >= :dari AND s.tahunMasuk <= :sampai "
                                + "GROUP BY s.sekolah.id, s.sekolah.nama, s.jenisKelamin "
                                + "ORDER BY COUNT(s) DESC")
                        .setParameter("dari", dari).setParameter("sampai", sampai).list();
            } catch (Exception e) { logErr("radar-sekolah", e); }
        }

        // ── Row 3: Bar sekolah | Donat status aktif (2 kolom)
        sb.append(grid2col(buildBarSekolah(sekolahRows), buildDonutAktif(totalAktif, totalAll)));

        // ── Row 4: Radar (full width, hanya jika ada >= 2 sekolah dan data gender)
        String radar = buildRadarSekolah(radarRows);
        if (radar != null) { sb.append(fullWidth(radar)); }

        // ── Footer spacer
        sb.append("<div style=\"height:20px;\"></div>");

        new MyHtml(sb.toString()).setParent(center);
    }

    // ══════════════════════════════════════════════════════════════════════
    // Build methods — satu metode per panel
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Membangun diagram donat komposisi gender siswa.
     */
    private String buildDonutGender(int totalL, int totalP) {
        int total = totalL + totalP;
        return HtmlChartHelper.donut(
                "Komposisi Jenis Kelamin Siswa",
                "Berapa persen siswa laki-laki dan berapa persen perempuan. "
                + "Berguna untuk memantau apakah sekolah berhasil menarik minat dari kedua kelompok secara merata.",
                new String[] { "Laki-laki", "Perempuan" },
                new double[] { totalL, totalP },
                new String[] { BIRU, MERAH },
                total > 0 ? persen(Math.max(totalL, totalP), total) : "–");
    }

    /**
     * Membangun batang bertumpuk gender per tahun masuk.
     */
    private String buildStackedGender(LinkedHashMap<Integer, int[]> trenMap) {
        if (trenMap.isEmpty()) {
            return emptyCard("Gender per Tahun Masuk", "Belum ada data tren gender per tahun masuk.");
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
                "Komposisi Gender per Tahun Masuk",
                "Setiap batang = satu tahun masuk. Biru = laki-laki, merah muda = perempuan. "
                + "Pergeseran warna dari tahun ke tahun menunjukkan perubahan komposisi gender siswa baru.",
                cats,
                new String[] { "Laki-laki", "Perempuan" },
                vals,
                new String[] { BIRU, MERAH });
    }

    /**
     * Membangun grafik garis tren siswa per tahun masuk (dengan data gender).
     */
    private String buildLineTren(LinkedHashMap<Integer, int[]> trenMap, int dari, int sampai) {
        if (trenMap.isEmpty()) {
            return emptyCard("Tren Penerimaan",
                    "Belum ada data siswa pada tahun masuk " + dari + "–" + sampai + ".");
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
                "Tren Jumlah Siswa per Tahun Masuk",
                "Garis ini menunjukkan apakah jumlah siswa baru bertumbuh, stabil, atau menurun. "
                + "Tren naik berarti strategi penerimaan berhasil; tren turun adalah sinyal evaluasi.",
                cats,
                new String[] { "Total", "Laki-laki", "Perempuan" },
                vals,
                new String[] { UNGU, BIRU, MERAH });
    }

    /**
     * Membangun grafik garis tren total siswa per tahun masuk (tanpa gender).
     */
    private String buildLineTrenSimple(LinkedHashMap<Integer, Integer> trenMap, int dari, int sampai) {
        if (trenMap.isEmpty()) {
            return emptyCard("Tren Penerimaan",
                    "Belum ada data siswa pada tahun masuk " + dari + "–" + sampai + ".");
        }
        List<Integer> tahuns = new ArrayList<Integer>(trenMap.keySet());
        int n = tahuns.size();
        String[]   cats = new String[n];
        double[][] vals = new double[1][n];
        for (int i = 0; i < n; i++) {
            cats[i] = String.valueOf(tahuns.get(i));
            vals[0][i] = trenMap.get(tahuns.get(i)).intValue();
        }
        return HtmlChartHelper.lineMulti(
                "Tren Jumlah Siswa per Tahun Masuk",
                "Apakah jumlah siswa baru bertumbuh dari tahun ke tahun? "
                + "Tren naik berarti strategi penerimaan berhasil; tren turun adalah sinyal evaluasi.",
                cats,
                new String[] { "Total Siswa" },
                vals,
                new String[] { UNGU });
    }

    /** Membangun batang horizontal jumlah siswa per sekolah. */
    private String buildBarSekolah(List<Object[]> rows) {
        if (rows.isEmpty()) {
            return emptyCard("Siswa per Sekolah", "Belum ada data sekolah pada rentang tahun masuk ini.");
        }
        int n = rows.size();
        String[]  lbl = new String[n];
        double[]  val = new double[n];
        for (int i = 0; i < n; i++) {
            Object[] r = rows.get(i);
            lbl[i] = abbrev(r[0] == null ? "–" : r[0].toString(), 25);
            val[i] = r[1] == null ? 0 : ((Number) r[1]).intValue();
        }
        return HtmlChartHelper.barHorizontal(
                "Jumlah Siswa per Sekolah",
                "Sekolah diurutkan dari yang paling banyak siswanya. "
                + "Semakin panjang batangnya, semakin banyak siswanya. "
                + "Berguna untuk perencanaan alokasi kelas dan kebutuhan guru.",
                lbl, val, BIRU);
    }

    /** Membangun donat status aktif vs non-aktif siswa. */
    private String buildDonutAktif(int aktif, int total) {
        int nonAktif = total - aktif;
        if (total <= 0) {
            return emptyCard("Status Keaktifan", "Belum ada data status keaktifan siswa.");
        }
        return HtmlChartHelper.donut(
                "Status Keaktifan Siswa",
                "Berapa siswa yang masih aktif dibanding yang sudah tidak aktif (keluar, pindah, atau selesai lebih awal). "
                + "Rasio aktif yang tinggi menunjukkan tingkat retensi yang baik.",
                new String[] { "Aktif", "Tidak Aktif" },
                new double[] { aktif, nonAktif },
                new String[] { HIJAU, MERAH },
                persen(aktif, total));
    }

    /**
     * Membangun grafik radar yang membandingkan sekolah-sekolah dari tiga dimensi:
     * jumlah total, persentase laki-laki, dan persentase perempuan.
     *
     * @return HTML grafik, atau {@code null} jika data tidak cukup
     */
    private String buildRadarSekolah(List<Object[]> radarRows) {
        if (radarRows.isEmpty()) { return null; }
        LinkedHashMap<String, SekolahData> map = new LinkedHashMap<String, SekolahData>();
        for (int i = 0; i < radarRows.size(); i++) {
            Object[] r = radarRows.get(i);
            String nm  = r[0] == null ? "–" : r[0].toString();
            String kel = r[1] == null ? "" : r[1].toString().toLowerCase(Locale.ROOT);
            int cnt    = r[2] == null ? 0 : ((Number) r[2]).intValue();
            if (!map.containsKey(nm)) { map.put(nm, new SekolahData(nm)); }
            SekolahData d = map.get(nm);
            d.total += cnt;
            if (kel.startsWith("l")) { d.laki += cnt; }
            else if (kel.startsWith("p") || kel.startsWith("w")) { d.prp += cnt; }
        }
        if (map.size() < 2) { return null; }

        List<SekolahData> top = new ArrayList<SekolahData>(map.values());
        // Bubble sort descending by total (Java 1.7)
        for (int i = 0; i < top.size() - 1; i++) {
            for (int j = 0; j < top.size() - 1 - i; j++) {
                if (top.get(j).total < top.get(j + 1).total) {
                    SekolahData tmp = top.get(j);
                    top.set(j, top.get(j + 1));
                    top.set(j + 1, tmp);
                }
            }
        }
        int nSeries = Math.min(top.size(), MAX_RADAR);
        String[]   labels = new String[nSeries];
        double[][] vals   = new double[nSeries][3];
        for (int i = 0; i < nSeries; i++) {
            SekolahData d = top.get(i);
            labels[i]  = abbrev(d.nama, 18);
            vals[i][0] = d.total;
            vals[i][1] = d.total > 0 ? (100.0 * d.laki / d.total) : 0;
            vals[i][2] = d.total > 0 ? (100.0 * d.prp  / d.total) : 0;
        }
        return HtmlChartHelper.radar(
                "Perbandingan Sekolah — Grafik Jaring Laba-laba",
                "Setiap sudut = satu ukuran: jumlah total siswa, persentase laki-laki, "
                + "dan persentase perempuan. Semakin luas area suatu sekolah, semakin besar nilainya. "
                + "Gunakan untuk membandingkan profil sekolah dalam yayasan secara sekaligus.",
                new String[] { "Jumlah Total", "% Laki-laki", "% Perempuan" },
                labels,
                vals,
                new String[] { BIRU, HIJAU, ORANGE, UNGU, CYAN },
                0);
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

    private String fullWidth(String html) {
        return "<div style=\"margin-top:14px;\">" + html + "</div>";
    }

    private String grid2col(String left, String right) {
        return "<div style=\"display:grid;"
                + "grid-template-columns:repeat(auto-fit,minmax(280px,1fr));"
                + "gap:14px;margin-top:14px;\">"
                + "<div>" + left + "</div>"
                + "<div>" + right + "</div>"
                + "</div>";
    }

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
        System.err.println("[DashboardRingkasanSiswa] Error " + ctx + ": " + e.getMessage());
    }
}
