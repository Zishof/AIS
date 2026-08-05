package ais.action.master.dashboard.admin;

import java.util.Map;

import ais.common.ElearningRingkasanCache;

/**
 * <h1>ElearningKartuRingkasanHelper — render kartu ringkasan e-Learning (HTML/CSS responsif)</h1>
 *
 * <h2>Apa ini</h2>
 * <p>Helper STATELESS &amp; REUSABLE yang mengubah hasil hitungan ringkasan e-Learning (dari
 * {@link ElearningRingkasanCache}) menjadi SATU potongan tampilan HTML/CSS modern: sebuah judul
 * singkat, kalimat penjelas yang mudah dipahami orang awam, lalu kartu-kartu angka penting beserta
 * sebuah grafik komposisi kehadiran. Hasilnya bisa langsung ditempel ke halaman dengan
 * {@code new Html(...)}.</p>
 *
 * <h2>Untuk apa kelas ini ada</h2>
 * <p>Dashboard "Ringkasan Aktivitas Pembelajaran Digital" perlu menampilkan kondisi pembelajaran
 * digital secara sekilas: berapa pertemuan sudah dibuka peserta, bagaimana kehadiran dosen dan
 * mahasiswa, serta berapa banyak materi dan tugas. Karena seluruh angka tersebut sudah dihitung
 * sekali dan disimpan di {@link ElearningRingkasanCache}, kelas ini hanya bertugas MERAPIKAN angka
 * itu menjadi tampilan yang enak dilihat dan informatif — tanpa lagi menyentuh basis data. Dengan
 * memisahkan urusan "tampilan" ke sini, perubahan desain di kemudian hari cukup dilakukan pada satu
 * berkas, sehingga perawatan jadi mudah.</p>
 *
 * <h2>Mengapa HTML/CSS, bukan komponen ZK atau jfreechart</h2>
 * <p>Mengikuti praktik yang sudah terbukti di proyek ini (lihat pustaka chart RingkasanKampus):
 * untuk menampilkan ANGKA dan grafik sederhana, satu string HTML jauh lebih ringan dibanding puluhan
 * komponen ZK (hemat memori server + render lebih cepat), dan memberi kebebasan penuh untuk tata
 * letak RESPONSIF lewat CSS Grid. Grafik (bar komposisi kehadiran) digambar murni dengan elemen
 * div + CSS — tidak memakai jfreechart — sehingga tetap ringan, tajam di layar resolusi tinggi, dan
 * konsisten di mode terang/gelap. Semua gaya diberi awalan {@code .el-rk} agar tidak "bocor"
 * mempengaruhi bagian halaman lain.</p>
 *
 * <h2>Responsif (ponsel &amp; desktop)</h2>
 * <p>Susunan kartu memakai {@code grid-template-columns: repeat(auto-fit, minmax(190px, 1fr))}
 * sehingga jumlah kolom menyesuaikan lebar layar secara otomatis: satu kolom di ponsel sempit,
 * beberapa kolom di desktop — tanpa perlu mendeteksi jenis perangkat di sisi server. Ukuran huruf
 * dan jarak dipilih agar nyaman dibaca di kedua perangkat, dan pada layar sangat sempit grafik serta
 * kartu menyusun ulang dirinya sendiri.</p>
 *
 * <h2>Penjelasan yang ramah orang awam</h2>
 * <p>Setiap kartu disertai satu kalimat penjelas dengan bahasa sehari-hari (mis. "Berapa banyak
 * pertemuan yang sudah dibuka peserta") agar pengguna yang tidak paham istilah teknis tetap mengerti
 * makna angka yang ditampilkan. Penjelasan sengaja TIDAK memakai frasa kaku seperti "Panel ini ...";
 * kalimatnya langsung pada manfaatnya.</p>
 *
 * <h2>Kontrak &amp; keamanan nilai</h2>
 * <p>Method utama {@link #htmlRingkasan(ElearningRingkasanCache.Ringkasan, boolean)} menerima objek
 * hasil agregasi dan sebuah penanda apakah lingkupnya admin (mempengaruhi label target pertemuan).
 * Semua angka diformat dengan pemisah ribuan, pembagian dijaga dari pembagian-nol, dan teks
 * di-escape agar aman dari karakter HTML. Method ini murni menghasilkan teks (tidak ada efek
 * samping), sehingga aman dipanggil berulang. Kompatibel Java 1.7 (tanpa lambda).</p>
 */
public final class ElearningKartuRingkasanHelper {

    private ElearningKartuRingkasanHelper() {
    }

    /** Urutan + label + warna status kehadiran (Hadir/Mangkir/Sakit/Izin/Belum). */
    private static final String[] KODE = { "M", "A", "S", "I", "-" };
    private static final String[] LABEL = { "Hadir", "Mangkir", "Sakit", "Izin", "Belum" };
    private static final String[] WARNA = { "#16a34a", "#dc2626", "#d97706", "#2563eb", "#94a3b8" };

    /**
     * Bangun potongan HTML lengkap (judul + penjelasan + kartu + grafik komposisi) dari hasil
     * agregasi cache. Tidak pernah mengembalikan null.
     *
     * @param r     hasil {@link ElearningRingkasanCache#jumlah(java.util.Collection, boolean)}.
     * @param admin true bila lingkup admin (target Akses Pertemuan = jumlah pertemuan).
     * @return potongan HTML siap ditempel (mis. via {@code new Html(...)}).
     */
    public static String htmlRingkasan(ElearningRingkasanCache.Ringkasan r, boolean admin) {
        StringBuilder sb = new StringBuilder(6144);
        sb.append("<div class='el-rk'>");
        sb.append(css());

        // Judul + kalimat penjelas ramah orang awam.
        sb.append("<div class='el-rk-head-wrap'>");
        sb.append("<div class='el-rk-h1'>Ringkasan Aktivitas Pembelajaran Digital</div>");
        sb.append("<div class='el-rk-sub'>Sekilas kondisi pembelajaran: berapa pertemuan sudah dibuka, "
                + "kehadiran dosen dan mahasiswa, serta banyaknya materi dan tugas.</div>");
        sb.append("</div>");

        // Kumpulan popup rincian (semua angka bisa diklik → tampil detailnya).
        StringBuilder modals = new StringBuilder();

        sb.append("<div class='el-rk-grid'>");
        // 1) Akses Pertemuan — bisa diklik.
        int aDone = r == null ? 0 : r.pertemuanDone;
        int aTarget = r == null ? 0 : r.pertemuanTarget;
        sb.append(KartuRingkasanKit.klikable(kartuProgres("Akses Pertemuan", "#0d6efd", aDone, aTarget,
                admin ? "Sudah dibuka" : "Dibuka", "Berapa banyak pertemuan yang sudah dibuka peserta."),
                "elrk_akses"));
        modals.append(KartuRingkasanKit.modal("elrk_akses", "Rincian Akses Pertemuan",
                rincianAkses(aDone, aTarget)));
        // 2) Kehadiran Dosen.
        if (r != null && sum(r.kehadiranDosen) > 0) {
            sb.append(KartuRingkasanKit.klikable(kartuKehadiran("Kehadiran Dosen", "#2563eb", r.kehadiranDosen,
                    "Rekap kehadiran dosen: hadir, tidak hadir, sakit, dan izin."), "elrk_dosen"));
            modals.append(KartuRingkasanKit.modal("elrk_dosen", "Rincian Kehadiran Dosen",
                    rincianKehadiran(r.kehadiranDosen)));
        }
        // 3) Kehadiran Mahasiswa.
        if (r != null && sum(r.kehadiranMahasiswa) > 0) {
            sb.append(KartuRingkasanKit.klikable(kartuKehadiran("Kehadiran Mahasiswa", "#16a34a", r.kehadiranMahasiswa,
                    "Rekap kehadiran mahasiswa pada pertemuan yang berlangsung."), "elrk_mhs"));
            modals.append(KartuRingkasanKit.modal("elrk_mhs", "Rincian Kehadiran Mahasiswa",
                    rincianKehadiran(r.kehadiranMahasiswa)));
        }
        // 4) Kehadiran Siswa (sekolah).
        if (r != null && sum(r.kehadiranSiswa) > 0) {
            sb.append(KartuRingkasanKit.klikable(kartuKehadiran("Kehadiran Siswa", "#0d9488", r.kehadiranSiswa,
                    "Rekap kehadiran siswa pada pertemuan yang berlangsung."), "elrk_siswa"));
            modals.append(KartuRingkasanKit.modal("elrk_siswa", "Rincian Kehadiran Siswa",
                    rincianKehadiran(r.kehadiranSiswa)));
        }
        // 5) Materi & Media.
        int[] media = new int[] { r == null ? 0 : r.materi, r == null ? 0 : r.audio, r == null ? 0 : r.video };
        sb.append(KartuRingkasanKit.klikable(kartuAngka("Materi & Media", "#0891b2",
                new String[] { "Materi", "Audio", "Video" }, media,
                "Banyaknya bahan belajar yang tersedia untuk peserta."), "elrk_media"));
        modals.append(KartuRingkasanKit.modal("elrk_media", "Rincian Materi & Media",
                rincianMedia(media[0], media[1], media[2])));
        // 6) Tugas.
        int tug = r == null ? 0 : r.tugas;
        sb.append(KartuRingkasanKit.klikable(kartuTotal("Tugas", "#f59e0b", tug,
                "Jumlah tugas yang diberikan pada pertemuan."), "elrk_tugas"));
        modals.append(KartuRingkasanKit.modal("elrk_tugas", "Rincian Tugas",
                KartuRingkasanKit.barisRincian("Total tugas diberikan", fmt(tug))
                        + "<div style='margin-top:8px;color:#64748b;'>Jumlah seluruh tugas yang diberikan dosen "
                        + "pada pertemuan yang berlangsung.</div>"));
        sb.append("</div>");

        // Grafik komposisi kehadiran (bar bertumpuk) — tampilkan bila ada datanya.
        String barDosen = r == null ? "" : barKomposisi("Komposisi Kehadiran Dosen", r.kehadiranDosen);
        String barMhs = r == null ? "" : barKomposisi("Komposisi Kehadiran Mahasiswa", r.kehadiranMahasiswa);
        String barSiswa = r == null ? "" : barKomposisi("Komposisi Kehadiran Siswa", r.kehadiranSiswa);
        if (barDosen.length() + barMhs.length() + barSiswa.length() > 0) {
            sb.append("<div class='el-rk-bars'>");
            sb.append("<div class='el-rk-bars-title'>Perbandingan kehadiran — melihat seberapa "
                    + "banyak yang hadir dibanding tidak hadir, sakit, atau izin.</div>");
            sb.append(barDosen).append(barMhs).append(barSiswa);
            sb.append("</div>");
        }

        sb.append(modals); // popup rincian (tersembunyi sampai kartunya diklik)
        sb.append("</div>");
        return sb.toString();
    }

    /** Isi popup rincian untuk kartu "Akses Pertemuan". */
    private static String rincianAkses(int done, int target) {
        int belum = Math.max(0, target - done);
        int pct = target <= 0 ? 0 : (int) Math.round(done * 100.0 / target);
        return KartuRingkasanKit.barisRincian("Total pertemuan", fmt(target))
                + KartuRingkasanKit.barisRincian("Sudah dibuka peserta", fmt(done))
                + KartuRingkasanKit.barisRincian("Belum dibuka", fmt(belum))
                + KartuRingkasanKit.barisRincian("Persentase dibuka", pct + "%")
                + "<div style='margin-top:8px;color:#64748b;'>Sebuah pertemuan dihitung \"dibuka\" bila minimal "
                + "satu peserta sudah mengakses materi atau aktivitas pertemuan tersebut.</div>";
    }

    /** Isi popup rincian untuk kartu kehadiran (rincian per status + persentase). */
    private static String rincianKehadiran(Map<String, Integer> byKode) {
        int total = sum(byKode);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < KODE.length; i++) {
            int v = nilai(byKode, KODE[i]);
            int pct = total <= 0 ? 0 : (int) Math.round(v * 100.0 / total);
            sb.append(KartuRingkasanKit.barisRincian(LABEL[i], fmt(v) + " (" + pct + "%)"));
        }
        sb.append(KartuRingkasanKit.barisRincian("Total tercatat", fmt(total)));
        return sb.toString();
    }

    /** Isi popup rincian untuk kartu "Materi &amp; Media". */
    private static String rincianMedia(int materi, int audio, int video) {
        return KartuRingkasanKit.barisRincian("Materi (dokumen/teks)", fmt(materi))
                + KartuRingkasanKit.barisRincian("Audio", fmt(audio))
                + KartuRingkasanKit.barisRincian("Video", fmt(video))
                + KartuRingkasanKit.barisRincian("Total bahan belajar", fmt(materi + audio + video));
    }

    /**
     * Bangun grafik "Profil Aktivitas per Kelas" (radar/spider) dari daftar aktivitas kelas.
     * Lima sumbu — Akses, Kehadiran, Materi, Tugas, Media — masing-masing dinormalkan ke skala
     * 0&ndash;100 relatif terhadap kelas terkuat di sumbu itu, sehingga BENTUK radar menunjukkan
     * profil keaktifan tiap kelas (makin lebar = makin aktif). Memakai ulang
     * {@link DashboardAkademikHtmlCssHelper#spiderChart} agar tampilan konsisten &amp; ringan.
     *
     * @param kelas daftar kelas (sudah dibatasi top-N) dari
     *              {@link ElearningRingkasanCache#aktivitasPerKelas(java.util.Collection, int)}.
     * @return potongan HTML grafik; string kosong bila tidak ada data.
     */
    public static String htmlSpiderKelas(java.util.List<ElearningRingkasanCache.KelasAktivitas> kelas) {
        if (kelas == null || kelas.isEmpty()) {
            return "";
        }
        String[] axes = { "Akses", "Kehadiran", "Materi", "Tugas", "Media" };
        int n = kelas.size();
        int[][] raw = new int[n][axes.length];
        for (int i = 0; i < n; i++) {
            ElearningRingkasanCache.KelasAktivitas k = kelas.get(i);
            raw[i][0] = k.aksesTarget > 0 ? (int) Math.round(k.aksesDone * 100.0 / k.aksesTarget) : 0;
            raw[i][1] = k.hadir;
            raw[i][2] = k.materi;
            raw[i][3] = k.tugas;
            raw[i][4] = k.media;
        }
        int[] axMax = new int[axes.length];
        for (int x = 0; x < axes.length; x++) {
            int m = 1;
            for (int i = 0; i < n; i++) {
                if (raw[i][x] > m) {
                    m = raw[i][x];
                }
            }
            axMax[x] = m;
        }
        java.util.List<String> names = new java.util.ArrayList<String>();
        java.util.List<int[]> values = new java.util.ArrayList<int[]>();
        for (int i = 0; i < n; i++) {
            names.add(kelas.get(i).nama);
            int[] v = new int[axes.length];
            for (int x = 0; x < axes.length; x++) {
                v[x] = (int) Math.round(raw[i][x] * 100.0 / axMax[x]);
            }
            values.add(v);
        }
        String chart = DashboardAkademikHtmlCssHelper.spiderChart("Profil Aktivitas per Kelas",
                "Membandingkan keaktifan tiap kelas: seberapa banyak pertemuan dibuka, kehadiran, materi, "
                        + "tugas, dan media. Makin lebar bentuknya, makin aktif kelas tersebut.",
                axes, names, values);
        StringBuilder body = new StringBuilder();
        for (int i = 0; i < kelas.size(); i++) {
            ElearningRingkasanCache.KelasAktivitas k = kelas.get(i);
            int aksesPct = k.aksesTarget > 0 ? (int) Math.round(k.aksesDone * 100.0 / k.aksesTarget) : 0;
            body.append(KartuRingkasanKit.barisRincian(k.nama, "Akses " + aksesPct + "% &middot; Hadir " + fmt(k.hadir)
                    + " &middot; Materi " + fmt(k.materi) + " &middot; Tugas " + fmt(k.tugas) + " &middot; Media "
                    + fmt(k.media)));
        }
        return KartuRingkasanKit.bungkusKlikable(chart, "elrk_spider")
                + KartuRingkasanKit.modal("elrk_spider", "Rincian Aktivitas per Kelas", body.toString());
    }

    /**
     * Grafik garis "Tren Kehadiran Mingguan" dari peta {pekan -&gt; jumlah hadir} hasil
     * {@link ElearningRingkasanCache#trenKehadiranMingguan(java.util.Collection)}. Memakai ulang
     * {@link DashboardAkademikHtmlCssHelper#trendLineChart}. String kosong bila tak ada data.
     */
    public static String htmlTrenKehadiran(java.util.LinkedHashMap<String, Integer> perPekan) {
        if (perPekan == null || perPekan.isEmpty()) {
            return "";
        }
        java.util.List<String> labels = new java.util.ArrayList<String>(perPekan.keySet());
        int[] nilai = new int[labels.size()];
        int i = 0;
        for (Integer v : perPekan.values()) {
            nilai[i++] = v == null ? 0 : v.intValue();
        }
        java.util.List<String> names = new java.util.ArrayList<String>();
        names.add("Hadir");
        java.util.List<int[]> values = new java.util.ArrayList<int[]>();
        values.add(nilai);
        String chart = DashboardAkademikHtmlCssHelper.trendLineChart("Tren Kehadiran Mingguan",
                "Naik-turunnya jumlah kehadiran dari minggu ke minggu — membantu melihat minggu mana "
                        + "yang ramai atau sepi.",
                labels, names, values);
        StringBuilder body = new StringBuilder();
        for (java.util.Map.Entry<String, Integer> e : perPekan.entrySet()) {
            body.append(KartuRingkasanKit.barisRincian("Pekan " + e.getKey(),
                    fmt(e.getValue() == null ? 0 : e.getValue().intValue()) + " hadir"));
        }
        return KartuRingkasanKit.bungkusKlikable(chart, "elrk_tren")
                + KartuRingkasanKit.modal("elrk_tren", "Rincian Tren Kehadiran Mingguan", body.toString());
    }

    /**
     * Grafik corong "Perjalanan Pembelajaran" dari 5 angka [total, diakses, adaKehadiran, adaMateri,
     * adaTugas] hasil {@link ElearningRingkasanCache#funnelPembelajaran(java.util.Collection)}.
     * Memakai ulang {@link DashboardAkademikHtmlCssHelper#funnelChart}.
     */
    public static String htmlFunnelPembelajaran(int[] f) {
        if (f == null || f.length < 5 || f[0] <= 0) {
            return "";
        }
        String[] stages = { "Pertemuan", "Sudah Diakses", "Ada Kehadiran", "Dilengkapi Materi", "Ada Tugas" };
        String chart = DashboardAkademikHtmlCssHelper.funnelChart("Perjalanan Pembelajaran",
                "Dari seluruh pertemuan: berapa yang sudah dibuka peserta, ada kehadirannya, dilengkapi "
                        + "materi, dan diberi tugas.",
                stages, f);
        StringBuilder body = new StringBuilder();
        for (int s = 0; s < stages.length; s++) {
            int pct = f[0] > 0 ? (int) Math.round(f[s] * 100.0 / f[0]) : 0;
            body.append(KartuRingkasanKit.barisRincian(stages[s], fmt(f[s]) + " (" + pct + "%)"));
        }
        return KartuRingkasanKit.bungkusKlikable(chart, "elrk_funnel")
                + KartuRingkasanKit.modal("elrk_funnel", "Rincian Perjalanan Pembelajaran", body.toString());
    }

    // ── kartu: progres (done/target + %) — delegasi ke kit reusable ──────────
    private static String kartuProgres(String judul, String warna, int done, int target, String lblDone,
            String deskripsi) {
        return KartuRingkasanKit.kartuProgres(judul, warna, done, target, lblDone, deskripsi);
    }

    // ── kartu: kehadiran (breakdown M/A/S/I/-) ───────────────────────────────
    private static String kartuKehadiran(String judul, String warna, Map<String, Integer> byKode,
            String deskripsi) {
        int total = sum(byKode);
        StringBuilder sb = new StringBuilder();
        sb.append("<div class='el-rk-card' style='border-top-color:").append(warna).append(";'>");
        sb.append(head(judul, fmt(total) + " data", warna));
        sb.append("<div class='el-rk-foot el-rk-wrap'>");
        for (int i = 0; i < KODE.length; i++) {
            int v = nilai(byKode, KODE[i]);
            if (v <= 0 && "-".equals(KODE[i])) {
                continue;
            }
            sb.append(pill(LABEL[i], fmt(v), WARNA[i]));
        }
        sb.append("</div>");
        sb.append("<div class='el-rk-ket'>").append(esc(deskripsi)).append("</div>");
        sb.append("</div>");
        return sb.toString();
    }

    // ── kartu: beberapa angka (materi/audio/video) — delegasi ke kit ─────────
    private static String kartuAngka(String judul, String warna, String[] label, int[] nilai, String deskripsi) {
        return KartuRingkasanKit.kartuAngka(judul, warna, label, nilai, "item", deskripsi);
    }

    // ── kartu: total tunggal — delegasi ke kit ───────────────────────────────
    private static String kartuTotal(String judul, String warna, int total, String deskripsi) {
        return KartuRingkasanKit.kartuTotal(judul, warna, total, deskripsi);
    }

    // ── grafik: bar komposisi kehadiran (bertumpuk, HTML/CSS murni) ───────────
    private static String barKomposisi(String judul, Map<String, Integer> byKode) {
        int total = sum(byKode);
        if (total <= 0) {
            return "";
        }
        StringBuilder seg = new StringBuilder();
        StringBuilder leg = new StringBuilder();
        for (int i = 0; i < KODE.length; i++) {
            int v = nilai(byKode, KODE[i]);
            if (v <= 0) {
                continue;
            }
            double persen = v * 100.0 / total;
            seg.append("<span title='").append(esc(LABEL[i])).append(": ").append(fmt(v)).append("' style='width:")
                    .append(String.format("%.1f", persen)).append("%;background:").append(WARNA[i]).append(";'></span>");
            leg.append("<span class='el-rk-leg'><i style='background:").append(WARNA[i]).append(";'></i>")
                    .append(esc(LABEL[i])).append(" ").append(fmt(v)).append("</span>");
        }
        StringBuilder sb = new StringBuilder();
        sb.append("<div class='el-rk-barrow'>");
        sb.append("<div class='el-rk-barlabel'>").append(esc(judul)).append("</div>");
        sb.append("<div class='el-rk-bar'>").append(seg).append("</div>");
        sb.append("<div class='el-rk-barleg'>").append(leg).append("</div>");
        sb.append("</div>");
        return sb.toString();
    }

    // ── util render — delegasi ke kit reusable (satu sumber kebenaran) ───────
    private static String head(String judul, String badge, String warna) {
        return KartuRingkasanKit.head(judul, badge, warna);
    }

    private static String pill(String label, String nilai, String warna) {
        return KartuRingkasanKit.pill(label, nilai, warna);
    }

    private static int sum(Map<String, Integer> m) {
        int t = 0;
        if (m != null) {
            for (Integer v : m.values()) {
                t += v == null ? 0 : v.intValue();
            }
        }
        return t;
    }

    private static int nilai(Map<String, Integer> m, String k) {
        if (m == null) {
            return 0;
        }
        Integer v = m.get(k);
        return v == null ? 0 : v.intValue();
    }

    private static String fmt(int n) {
        return KartuRingkasanKit.fmt(n);
    }

    private static String esc(String s) {
        return KartuRingkasanKit.esc(s);
    }

    private static String css() {
        return KartuRingkasanKit.css();
    }
}
