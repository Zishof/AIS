package ais.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.Pertemuan;

/**
 * <h1>ElearningRingkasanCache — cache agregat per-PERTEMUAN untuk dashboard e-Learning</h1>
 *
 * <p>Dashboard "Ringkasan Aktivitas Pembelajaran Digital" (lihat
 * {@code DashboardTimelinePertemuan.buildDashboardSummary}) menghitung — setiap kali dibuka —
 * angka PERTEMUAN (akses), KEHADIRAN DOSEN, dan KEHADIRAN MAHASISWA dengan cara MENGITERASI semua
 * pertemuan pada lingkup pengguna (admin = seluruh PT; dosen = kelas yang diampu; mahasiswa = kelas
 * KRS), memuat tiap {@link Pertemuan} dan mengurai data akses + absensinya. Untuk jumlah pertemuan
 * yang banyak, proses ini lambat sehingga halaman e-Learning terasa berat saat pertama dibuka.</p>
 *
 * <h2>Wawasan kunci</h2>
 * <p>Agregat <i>per satu pertemuan</i> — berapa peserta didik yang sudah mengakses, apakah ada
 * akses non-admin, jumlah peserta didik, serta jumlah kehadiran per status (Hadir/Mangkir/Sakit/
 * Izin) untuk tiap peran — TIDAK bergantung pada siapa yang melihat. Nilainya hanya bergantung pada
 * data pertemuan itu sendiri. Karena itu agregat ini dapat DIHITUNG SEKALI dan disimpan, lalu untuk
 * pengguna mana pun dashboard cukup MENJUMLAHKAN agregat dari himpunan pertemuan miliknya — operasi
 * memori yang sangat cepat, tanpa iterasi basis data per permintaan.</p>
 *
 * <h2>Strategi</h2>
 * <ol>
 *   <li><b>Warm-up saat startup</b> ({@link #warmupStartup()}): di latar (daemon), hitung agregat
 *       semua pertemuan aktif Tahun Akademik berjalan dan simpan per id pertemuan. Mengikuti pola
 *       {@code RingkasanKampusCache} — hanya menyimpan ANGKA (hemat memori), bukan objek entitas.</li>
 *   <li><b>Baca instan</b> ({@link #jumlah(Collection, boolean)}): jumlahkan agregat untuk himpunan
 *       id pertemuan milik pengguna.</li>
 *   <li><b>Konsistensi</b>: data baru (absen/akses setelah warm) belum tercermin sampai
 *       {@link #invalidasi(Long)}/{@link #invalidasiSemua()} atau warm ulang. Logika parsing absensi
 *       &amp; status SENGAJA disamakan dengan {@code DashboardTimelinePertemuan} agar angka konsisten.</li>
 * </ol>
 *
 * <p>Catatan: kelas ini melayani ANGKA ringkasan. Rincian (daftar nama saat angka diklik) tetap
 * dihitung oleh dashboard saat dibutuhkan, karena memuat referensi peserta/pertemuan yang tidak
 * disimpan di cache (agar tetap hemat memori).</p>
 */
public final class ElearningRingkasanCache {

    private ElearningRingkasanCache() {
    }

    /** Urutan/daftar kode status kehadiran yang dikenal. */
    private static final String[] KODE_STATUS = { "M", "A", "S", "I", "-" };

    /** Agregat satu pertemuan — hanya angka. Serializable agar bisa di-snapshot ke disk
     *  oleh {@code ElearningFlagAccessCache} (cache persisten lintas-restart). */
    public static final class Agg implements java.io.Serializable {
        private static final long serialVersionUID = 1L;
        /** role(lower) -&gt; (kodeStatus -&gt; jumlah). */
        public final Map<String, Map<String, Integer>> kehadiran = new HashMap<String, Map<String, Integer>>();
        /** jumlah peserta didik (mhs+siswa) yang sudah mengakses pertemuan. */
        public int aksesPesertaDidik;
        /** true bila ada akses dari non-admin (dosen/guru/mhs/siswa/calon). */
        public boolean adaAksesNonAdmin;
        /** jumlah total peserta didik (mhs + siswa) pada pertemuan. */
        public int totalPesertaDidik;
        /** jumlah materi (PertemuanFileContent) — dihitung saat warm sekaligus menghangatkan datanya. */
        public int materiCount;
        public int audioCount;
        public int videoCount;
        public int tugasCount;
        /** id perkuliahan/kelas pemilik pertemuan (untuk profil aktivitas per kelas). */
        public Long perkuliahanId;
        /** tanggal pertemuan (untuk tren kehadiran mingguan). */
        public java.util.Date tanggal;
    }

    /**
     * Profil aktivitas satu KELAS (perkuliahan) — dipakai grafik "Profil Aktivitas per Kelas".
     * Semua berupa angka agar ringan; nama kelas diresolusi saat agregasi.
     */
    public static final class KelasAktivitas {
        public Long perkuliahanId;
        public String nama = "";
        public int aksesDone;
        public int aksesTarget;
        public int hadir;
        public int materi;
        public int tugas;
        public int media;

        /** Skor total kasar untuk mengurutkan kelas paling aktif. */
        public int skor() {
            return aksesDone + hadir + materi + tugas + media;
        }
    }

    /** Hasil penjumlahan untuk sebuah himpunan pertemuan. */
    public static final class Ringkasan {
        public int pertemuanTarget;
        public int pertemuanDone;
        /** kode -&gt; jumlah, untuk kehadiran dosen. */
        public final Map<String, Integer> kehadiranDosen = baseKodeMap();
        /** kode -&gt; jumlah, untuk kehadiran mahasiswa. */
        public final Map<String, Integer> kehadiranMahasiswa = baseKodeMap();
        /** kode -&gt; jumlah, untuk kehadiran siswa. */
        public final Map<String, Integer> kehadiranSiswa = baseKodeMap();
        /** kode -&gt; jumlah, untuk kehadiran guru. */
        public final Map<String, Integer> kehadiranGuru = baseKodeMap();
        /** total materi / audio / video / tugas pada himpunan pertemuan. */
        public int materi;
        public int audio;
        public int video;
        public int tugas;
    }

    private static final ConcurrentHashMap<Long, Agg> CACHE = new ConcurrentHashMap<Long, Agg>();
    public static volatile boolean siap = false;
    public static volatile long dibangunPada = 0L;

    // ── Jembatan ke cache PERSISTEN (ElearningFlagAccessCache) ───────────────
    /** Salinan isi cache saat ini (pertemuanId -&gt; Agg) untuk disimpan ke disk. */
    public static java.util.Map<Long, Agg> salinIsiCache() {
        return new java.util.HashMap<Long, Agg>(CACHE);
    }

    /** Muat banyak agregat dari sumber persisten ke memori (saat startup, sebelum warm DB). */
    public static void muatKeCacheDariPersisten(java.util.Map<Long, Agg> data) {
        if (data == null || data.isEmpty()) {
            return;
        }
        for (java.util.Map.Entry<Long, Agg> e : data.entrySet()) {
            if (e.getKey() != null && e.getValue() != null) {
                CACHE.put(e.getKey(), e.getValue());
            }
        }
        siap = true;
        dibangunPada = System.currentTimeMillis();
    }

    /** Jumlah pertemuan yang ada di cache (untuk logging/monitoring). */
    public static int ukuranCache() {
        return CACHE.size();
    }

    // ════════════════════════════════════════════════════════════════════════
    // Baca (dipakai dashboard)
    // ════════════════════════════════════════════════════════════════════════

    /** Agregat satu pertemuan dari cache; null bila belum ada. */
    public static Agg get(Long pertemuanId) {
        return pertemuanId == null ? null : CACHE.get(pertemuanId);
    }

    /** Apakah cache memuat agregat untuk SEMUA id pada koleksi (true =&gt; bisa pakai jalur cepat). */
    public static boolean lengkap(Collection<Long> pertemuanIds) {
        if (pertemuanIds == null || pertemuanIds.isEmpty()) {
            return false;
        }
        for (Long id : pertemuanIds) {
            if (id != null && !CACHE.containsKey(id)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Jumlahkan agregat untuk himpunan pertemuan. {@code admin=true} mengikuti definisi dashboard
     * (target = 1 per pertemuan, done = 1 bila ada akses non-admin); selain itu target = total
     * peserta didik dan done = jumlah peserta didik yang mengakses. Kehadiran dijumlah apa adanya.
     */
    public static Ringkasan jumlah(Collection<Long> pertemuanIds, boolean admin) {
        Ringkasan r = new Ringkasan();
        if (pertemuanIds == null) {
            return r;
        }
        for (Long id : pertemuanIds) {
            Agg a = get(id);
            if (a == null) {
                continue;
            }
            if (admin) {
                r.pertemuanTarget += 1;
                r.pertemuanDone += a.adaAksesNonAdmin ? 1 : 0;
            } else {
                r.pertemuanTarget += a.totalPesertaDidik;
                r.pertemuanDone += a.aksesPesertaDidik;
            }
            tambah(r.kehadiranDosen, a.kehadiran.get("dosen"));
            tambah(r.kehadiranMahasiswa, a.kehadiran.get("mahasiswa"));
            tambah(r.kehadiranSiswa, a.kehadiran.get("siswa"));
            tambah(r.kehadiranGuru, a.kehadiran.get("guru"));
            r.materi += a.materiCount;
            r.audio += a.audioCount;
            r.video += a.videoCount;
            r.tugas += a.tugasCount;
        }
        return r;
    }

    /**
     * Seperti {@link #jumlah(Collection, boolean)}, tetapi MEMASTIKAN setiap pertemuan yang diminta
     * sudah ada di cache terlebih dahulu: pertemuan yang belum ter-cache dihitung &amp; disimpan
     * saat itu juga (on-demand). Dipakai dashboard e-Learning agar ringkasan kartu modern SELALU
     * tampil dengan angka akurat &mdash; tidak lagi bergantung pada cache yang harus 100% lengkap
     * (yang membuat tampilan "kembali ke versi lama" bila warm-up belum selesai atau ada pertemuan
     * baru). Pemanasan on-demand dibatasi {@code maxWarm} agar tampilan tidak membeku pada koleksi
     * yang sangat besar; sisanya tetap memakai data cache yang ada.
     *
     * @param pertemuanIds himpunan id pertemuan yang sedang ditampilkan.
     * @param admin        {@code true} untuk perspektif admin (hitung per-pertemuan).
     * @param maxWarm      batas atas jumlah pertemuan yang dipanaskan on-demand dalam satu panggilan.
     * @return ringkasan agregat (tidak pernah {@code null}).
     */
    public static Ringkasan pastikanDanJumlah(Collection<Long> pertemuanIds, boolean admin, int maxWarm) {
        if (pertemuanIds != null) {
            int warmed = 0;
            for (Long id : pertemuanIds) {
                if (id == null || CACHE.containsKey(id)) {
                    continue;
                }
                if (warmed >= maxWarm) {
                    break;
                }
                try {
                    Object o = ais.database.model.GeneralValueObject.ambilData(Pertemuan.class, String.valueOf(id),
                            true);
                    if (o instanceof Pertemuan) {
                        hitungDanSimpan((Pertemuan) o);
                        warmed++;
                    }
                } catch (Throwable t) { ais.common.ErrorAuditUtil.record(t, "auto-audit(empty-catch) src/ais/common/ElearningRingkasanCache.java:238");
                }
            }
        }
        return jumlah(pertemuanIds, admin);
    }

    /**
     * Hitung profil aktivitas PER KELAS (perkuliahan) untuk himpunan pertemuan, lalu kembalikan
     * {@code topN} kelas teraktif (urut menurun berdasarkan skor aktivitas). Dipakai grafik
     * "Profil Aktivitas per Kelas". Nama kelas diresolusi dari cache entity Perkuliahan.
     */
    public static java.util.List<KelasAktivitas> aktivitasPerKelas(Collection<Long> pertemuanIds, int topN) {
        java.util.Map<Long, KelasAktivitas> peta = new java.util.HashMap<Long, KelasAktivitas>();
        if (pertemuanIds != null) {
            for (Long id : pertemuanIds) {
                Agg a = get(id);
                if (a == null || a.perkuliahanId == null) {
                    continue;
                }
                KelasAktivitas k = peta.get(a.perkuliahanId);
                if (k == null) {
                    k = new KelasAktivitas();
                    k.perkuliahanId = a.perkuliahanId;
                    peta.put(a.perkuliahanId, k);
                }
                k.aksesTarget += 1;
                k.aksesDone += a.adaAksesNonAdmin ? 1 : 0;
                k.hadir += nilaiKode(a.kehadiran.get("dosen"), "M") + nilaiKode(a.kehadiran.get("mahasiswa"), "M")
                        + nilaiKode(a.kehadiran.get("siswa"), "M") + nilaiKode(a.kehadiran.get("guru"), "M");
                k.materi += a.materiCount;
                k.tugas += a.tugasCount;
                k.media += a.audioCount + a.videoCount;
            }
        }
        java.util.List<KelasAktivitas> list = new java.util.ArrayList<KelasAktivitas>(peta.values());
        for (KelasAktivitas k : list) {
            k.nama = namaKelas(k.perkuliahanId);
        }
        java.util.Collections.sort(list, new java.util.Comparator<KelasAktivitas>() {
            @Override
            public int compare(KelasAktivitas a, KelasAktivitas b) {
                return b.skor() - a.skor();
            }
        });
        if (topN > 0 && list.size() > topN) {
            list = new java.util.ArrayList<KelasAktivitas>(list.subList(0, topN));
        }
        return list;
    }

    /**
     * Tren KEHADIRAN per MINGGU: jumlah peserta yang hadir (kode "M", semua peran) dikelompokkan
     * per pekan kalender, urut waktu. Kunci = label tanggal awal pekan ("dd MMM"); nilai = total
     * hadir pekan itu. Dipakai grafik garis "Tren Kehadiran Mingguan".
     */
    public static java.util.LinkedHashMap<String, Integer> trenKehadiranMingguan(Collection<Long> pertemuanIds) {
        java.util.TreeMap<Integer, Integer> perPekan = new java.util.TreeMap<Integer, Integer>();
        java.util.Map<Integer, java.util.Date> tglPekan = new java.util.HashMap<Integer, java.util.Date>();
        if (pertemuanIds != null) {
            java.util.Calendar cal = java.util.Calendar.getInstance();
            for (Long id : pertemuanIds) {
                Agg a = get(id);
                if (a == null || a.tanggal == null) {
                    continue;
                }
                cal.setTime(a.tanggal);
                int wk = cal.get(java.util.Calendar.YEAR) * 100 + cal.get(java.util.Calendar.WEEK_OF_YEAR);
                int hadir = nilaiKode(a.kehadiran.get("dosen"), "M") + nilaiKode(a.kehadiran.get("mahasiswa"), "M")
                        + nilaiKode(a.kehadiran.get("siswa"), "M") + nilaiKode(a.kehadiran.get("guru"), "M");
                Integer cur = perPekan.get(wk);
                perPekan.put(wk, (cur == null ? 0 : cur.intValue()) + hadir);
                java.util.Date prev = tglPekan.get(wk);
                if (prev == null || a.tanggal.before(prev)) {
                    tglPekan.put(wk, a.tanggal);
                }
            }
        }
        java.util.LinkedHashMap<String, Integer> hasil = new java.util.LinkedHashMap<String, Integer>();
        java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat("dd MMM");
        for (java.util.Map.Entry<Integer, Integer> e : perPekan.entrySet()) {
            java.util.Date d = tglPekan.get(e.getKey());
            String label = d == null ? ("Pkn " + (e.getKey() % 100)) : fmt.format(d);
            hasil.put(label, e.getValue());
        }
        return hasil;
    }

    /**
     * FUNNEL pembelajaran (tingkat pertemuan): dari seluruh pertemuan, berapa yang sudah diakses
     * peserta, ada kehadirannya, dilengkapi materi, dan diberi tugas. Mengembalikan 5 angka berurut:
     * [total, diakses, adaKehadiran, adaMateri, adaTugas].
     */
    public static int[] funnelPembelajaran(Collection<Long> pertemuanIds) {
        int total = 0, diakses = 0, adaKehadiran = 0, adaMateri = 0, adaTugas = 0;
        if (pertemuanIds != null) {
            for (Long id : pertemuanIds) {
                Agg a = get(id);
                if (a == null) {
                    continue;
                }
                total++;
                if (a.adaAksesNonAdmin) {
                    diakses++;
                }
                int hadir = nilaiKode(a.kehadiran.get("dosen"), "M") + nilaiKode(a.kehadiran.get("mahasiswa"), "M")
                        + nilaiKode(a.kehadiran.get("siswa"), "M") + nilaiKode(a.kehadiran.get("guru"), "M");
                if (hadir > 0) {
                    adaKehadiran++;
                }
                if (a.materiCount > 0) {
                    adaMateri++;
                }
                if (a.tugasCount > 0) {
                    adaTugas++;
                }
            }
        }
        return new int[] { total, diakses, adaKehadiran, adaMateri, adaTugas };
    }

    /** Ambil jumlah satu kode status dari sebuah map kehadiran (0 bila tidak ada). */
    private static int nilaiKode(Map<String, Integer> m, String k) {
        if (m == null) {
            return 0;
        }
        Integer v = m.get(k);
        return v == null ? 0 : v.intValue();
    }

    /** Nama ringkas kelas dari id perkuliahan (matakuliah + kelas), via cache entity. */
    private static String namaKelas(Long perkuliahanId) {
        if (perkuliahanId == null) {
            return "-";
        }
        try {
            Object o = ais.database.model.GeneralValueObject.ambilData(ais.database.model.Perkuliahan.class,
                    String.valueOf(perkuliahanId), true);
            if (o instanceof ais.database.model.Perkuliahan) {
                ais.database.model.Perkuliahan pk = (ais.database.model.Perkuliahan) o;
                String mk = pk.getMatakuliah() != null && pk.getMatakuliah().getNama() != null
                        ? pk.getMatakuliah().getNama() : ("Kelas " + perkuliahanId);
                String kelas = pk.getKelas();
                return kelas != null && kelas.trim().length() > 0 ? (mk + " - " + kelas) : mk;
            }
        } catch (Throwable t) { ais.common.ErrorAuditUtil.record(t, "auto-audit(empty-catch) src/ais/common/ElearningRingkasanCache.java:383");
        }
        return "Kelas " + perkuliahanId;
    }

    // ════════════════════════════════════════════════════════════════════════
    // Tulis / hitung
    // ════════════════════════════════════════════════════════════════════════

    /** Hitung agregat satu pertemuan dan masukkan ke cache. Aman dipanggil ulang (idempoten). */
    public static Agg hitungDanSimpan(Pertemuan p) {
        if (p == null || p.getId() == null) {
            return null;
        }
        Agg a = hitungAgg(p);
        CACHE.put(p.getId(), a);
        return a;
    }

    public static void invalidasi(Long pertemuanId) {
        if (pertemuanId != null) {
            CACHE.remove(pertemuanId);
        }
    }

    public static void invalidasiSemua() {
        CACHE.clear();
        siap = false;
    }

    /** Hitung agregat (angka) untuk sebuah pertemuan — replikasi logika DashboardTimelinePertemuan. */
    @SuppressWarnings("unchecked")
    private static Agg hitungAgg(Pertemuan p) {
        Agg a = new Agg();
        try {
            if (p.getPerkuliahan() != null) {
                a.perkuliahanId = p.getPerkuliahan().getId();
            }
            a.tanggal = p.getTanggal();
        } catch (Throwable t) { ais.common.ErrorAuditUtil.record(t, "auto-audit(empty-catch) src/ais/common/ElearningRingkasanCache.java:422");
        }
        // Kehadiran per role dari string absensi ("...;...;...", tiap entri koma-pisah, diakhiri role).
        try {
            String absensi = p.getAbsensi();
            if (absensi != null && absensi.trim().length() > 0) {
                String[] entri = absensi.split(";");
                for (int i = 0; i < entri.length; i++) {
                    String nn = entri[i];
                    if (nn == null) {
                        continue;
                    }
                    String low = nn.toLowerCase();
                    String role = roleDari(low);
                    if (role == null) {
                        continue;
                    }
                    String[] split = nn.split(",");
                    String kode = normalizeStatusKode(split.length >= 3 ? split[2] : "-");
                    Map<String, Integer> byKode = a.kehadiran.get(role);
                    if (byKode == null) {
                        byKode = baseKodeMap();
                        a.kehadiran.put(role, byKode);
                    }
                    Integer c = byKode.get(kode);
                    byKode.put(kode, (c == null ? 0 : c.intValue()) + 1);
                }
            }
        } catch (Throwable t) { ais.common.ErrorAuditUtil.record(t, "auto-audit(empty-catch) src/ais/common/ElearningRingkasanCache.java:450");
        }
        // Akses peserta didik + ada akses non-admin.
        try {
            TreeMap<String, String> dAkses = p.ambilData("akses", null);
            if (dAkses != null) {
                for (String user : dAkses.keySet()) {
                    String tipe = tipeUser(user);
                    if (tipe == null) {
                        continue;
                    }
                    String tl = tipe.toLowerCase();
                    if (tl.equals("mahasiswa") || tl.equals("siswa") || tl.equals("calonmahasiswa")
                            || tl.equals("calonsiswa")) {
                        a.aksesPesertaDidik++;
                        a.adaAksesNonAdmin = true;
                    } else if (tl.equals("dosen") || tl.equals("guru")) {
                        a.adaAksesNonAdmin = true;
                    }
                }
            }
        } catch (Throwable t) { ais.common.ErrorAuditUtil.record(t, "auto-audit(empty-catch) src/ais/common/ElearningRingkasanCache.java:471");
        }
        // Total peserta didik (mhs + siswa).
        try {
            int m = p.ambilMahasiswa() != null ? p.ambilMahasiswa().size() : 0;
            int s = p.ambilSiswa() != null ? p.ambilSiswa().size() : 0;
            a.totalPesertaDidik = m + s;
        } catch (Throwable t) { ais.common.ErrorAuditUtil.record(t, "auto-audit(empty-catch) src/ais/common/ElearningRingkasanCache.java:478");
        }
        // WARM materi/audio/video/tugas: memanggil ambil*Total menandai data sudah-dimuat (guard
        // "udah(...)") pada instance Pertemuan yang ter-cache, sehingga panel kanan + kartu
        // dashboard nantinya membaca tanpa query ulang. Sekaligus simpan jumlahnya (untuk ringkasan).
        try {
            java.util.Map<Long, ?> mt = p.ambilPertemuanFileContentTotal();
            a.materiCount = mt == null ? 0 : mt.size();
        } catch (Throwable t) { ais.common.ErrorAuditUtil.record(t, "auto-audit(empty-catch) src/ais/common/ElearningRingkasanCache.java:486");
        }
        try {
            java.util.Map<Long, ?> au = p.ambilAudioPertemuanTotal();
            a.audioCount = au == null ? 0 : au.size();
        } catch (Throwable t) { ais.common.ErrorAuditUtil.record(t, "auto-audit(empty-catch) src/ais/common/ElearningRingkasanCache.java:491");
        }
        try {
            java.util.Map<Long, ?> vd = p.ambilVideoPertemuanTotal();
            a.videoCount = vd == null ? 0 : vd.size();
        } catch (Throwable t) { ais.common.ErrorAuditUtil.record(t, "auto-audit(empty-catch) src/ais/common/ElearningRingkasanCache.java:496");
        }
        try {
            java.util.Map<Long, ?> tg = p.ambilTugasPertemuanTotal();
            a.tugasCount = tg == null ? 0 : tg.size();
        } catch (Throwable t) { ais.common.ErrorAuditUtil.record(t, "auto-audit(empty-catch) src/ais/common/ElearningRingkasanCache.java:501");
        }
        return a;
    }

    // ════════════════════════════════════════════════════════════════════════
    // Warm-up startup
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Hitung agregat semua pertemuan aktif Tahun Akademik berjalan, di latar (daemon), per batch.
     * Tidak memblokir startup. Aman bila gagal sebagian (gagal-diam per pertemuan).
     */
    public static void warmupStartup() {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(60000);
                    warmupSekarang();
                } catch (Throwable ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/common/ElearningRingkasanCache.java:521");
                }
            }
        }, "elearning-ringkasan-warmup");
        t.setDaemon(true);
        t.start();
    }

    /** Jalankan warm-up secara sinkron (dipakai warmupStartup + dapat dipanggil ulang manual). */
    @SuppressWarnings("unchecked")
    public static void warmupSekarang() {
        Session session = null;
        try {
            String ta = Common.getCurrentTahunAkademik();
            session = HibernateUtil.openSession();
            // Pertemuan Tahun Akademik berjalan yang aktif.
            List<Long> ids = session.createCriteria(Pertemuan.class)
                    .createAlias("perkuliahan", "pk")
                    .add(Restrictions.eq("pk.tahunAjaran", ta))
                    .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                    .setProjection(Projections.property("id")).list();
            // Tutup sesi pemilik id; warming berikutnya memakai jalur cache resmi (DataUtil).
            HibernateUtil.closeSessionQuietly(session);
            session = null;
            if (ids == null) {
                return;
            }
            int n = 0;
            for (Long id : ids) {
                if (id == null) {
                    continue;
                }
                try {
                    // KUNCI: warm via jalur yang DIPAKAI dashboard
                    // (GeneralValueObject.ambilData -> cache proses-wide DataUtil/MemoryCacheUtil),
                    // SEKALIGUS hitung agregat dari objek yang sama. Maka loop dashboard berikutnya
                    // mengakses Pertemuan dari cache panas (tanpa query DB) -> JAUH lebih cepat,
                    // TANPA mengubah logika dashboard (drill-down/rekap tetap utuh).
                    Object o = ais.database.model.GeneralValueObject.ambilData(Pertemuan.class, String.valueOf(id),
                            true);
                    if (o instanceof Pertemuan) {
                        hitungDanSimpan((Pertemuan) o);
                    }
                } catch (Throwable t) { ais.common.ErrorAuditUtil.record(t, "auto-audit(empty-catch) src/ais/common/ElearningRingkasanCache.java:564");
                }
                // Throttle ringan agar tidak membanjiri pool koneksi saat cache masih dingin.
                if ((++n % 300) == 0) {
                    try {
                        Thread.sleep(40);
                    } catch (Throwable ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/common/ElearningRingkasanCache.java:570");
                    }
                }
            }
            dibangunPada = System.currentTimeMillis();
            siap = true;
            System.out.println("ElearningRingkasanCache: warm-up " + CACHE.size() + " pertemuan (TA " + ta + ").");
        } catch (Throwable t) { ais.common.ErrorAuditUtil.record(t, "auto-audit(empty-catch) src/ais/common/ElearningRingkasanCache.java:577");
        } finally {
            HibernateUtil.closeSessionQuietly(session);
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // Util
    // ════════════════════════════════════════════════════════════════════════

    private static Map<String, Integer> baseKodeMap() {
        Map<String, Integer> m = new HashMap<String, Integer>();
        for (int i = 0; i < KODE_STATUS.length; i++) {
            m.put(KODE_STATUS[i], Integer.valueOf(0));
        }
        return m;
    }

    private static void tambah(Map<String, Integer> target, Map<String, Integer> sumber) {
        if (target == null || sumber == null) {
            return;
        }
        for (Map.Entry<String, Integer> e : sumber.entrySet()) {
            Integer t = target.get(e.getKey());
            target.put(e.getKey(), (t == null ? 0 : t.intValue()) + (e.getValue() == null ? 0 : e.getValue().intValue()));
        }
    }

    /** Tentukan role dari entri absensi (diakhiri nama role). */
    private static String roleDari(String low) {
        if (low.endsWith("dosen")) {
            return "dosen";
        }
        if (low.endsWith("mahasiswa")) {
            return "mahasiswa";
        }
        if (low.endsWith("siswa")) {
            return "siswa";
        }
        if (low.endsWith("guru")) {
            return "guru";
        }
        return null;
    }

    /** Ambil tipe user dari kunci akses (format "...-...-{tipe}-..."). */
    private static String tipeUser(String user) {
        if (user == null) {
            return null;
        }
        String[] u = user.split("-");
        return u.length >= 3 ? u[2] : null;
    }

    /** Normalisasi kode status — sama persis dengan DashboardTimelinePertemuan.normalizeStatusKode. */
    private static String normalizeStatusKode(String kode) {
        if (kode == null || kode.trim().length() == 0) {
            return "-";
        }
        String clean = kode.trim().toUpperCase();
        if (clean.length() > 1 && !"-".equals(clean)) {
            clean = clean.substring(0, 1);
        }
        if (!("M".equals(clean) || "A".equals(clean) || "S".equals(clean) || "I".equals(clean) || "-".equals(clean))) {
            return "-";
        }
        return clean;
    }
}
