package ais.common;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Execution;
import org.zkoss.zk.ui.Executions;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.Pertemuan;

/**
 * Cache BERTINGKAT (L1/L2/L3) untuk TREN KEHADIRAN antar-pertemuan pada satu perkuliahan/kelas.
 *
 * <p>
 * <b>Apa yang dihitung.</b> Untuk sebuah perkuliahan, kelas ini menelusuri seluruh pertemuannya
 * (urut menurut nomor pertemuan, lalu tanggal) dan untuk tiap pertemuan menghitung persentase
 * kehadiran peserta didik = jumlah HADIR dibagi jumlah peserta yang sudah diabsen (Hadir, Alpa,
 * Izin, Sakit). Pertemuan yang sama sekali belum diabsen dilewati agar grafik hanya memuat
 * pertemuan yang benar-benar sudah berlangsung. Hasilnya berupa deret angka siap-pakai untuk
 * grafik garis "Tren Kehadiran Antar-Pertemuan".
 * </p>
 *
 * <p>
 * <b>Mengapa di-cache.</b> Menghitung tren berarti membaca string absensi setiap pertemuan, yang
 * akan terasa berat bila diulang setiap kali halaman Kehadiran dibuka. Maka hasil deret disimpan
 * pada tiga tingkat, meniru pola {@link DashboardCacheUtil}:
 * </p>
 * <ul>
 * <li><b>L1 &mdash; Execution</b> (per satu permintaan): bila grafik dipakai dua kali dalam satu
 * render, perhitungan tidak diulang.</li>
 * <li><b>L2 &mdash; Desktop/sesi</b> (per tab pengguna, TTL 5 menit): bolak-balik tab tidak
 * memicu hitung ulang.</li>
 * <li><b>L3 &mdash; JVM</b> (lintas sesi, TTL 3 menit): data kehadiran kelas bersifat non-personal,
 * sehingga saat banyak dosen/admin membuka kelas yang sama, hitung cukup sekali.</li>
 * </ul>
 *
 * <p>
 * <b>Kesegaran data.</b> Karena memakai TTL pendek (L3 = 3 menit), perubahan absensi akan tercermin
 * paling lambat dalam hitungan menit. Untuk pembaruan seketika, panggil
 * {@link #invalidasi(Long)} setelah menyimpan absensi sebuah pertemuan pada perkuliahan terkait.
 * </p>
 *
 * <p>
 * Semua jalur dibungkus penanganan kesalahan yang tenang (gagal-aman): bila terjadi galat, metode
 * mengembalikan deret kosong sehingga pemanggil cukup menyembunyikan grafik tanpa mengganggu
 * tampilan presensi utama. Kelas ini stateless dan thread-safe karena seluruh state cache dikelola
 * oleh {@link DashboardCacheUtil} (L2/L3) dan {@link Execution} (L1).
 * </p>
 */
public final class AbsensiTrenCache {

    private AbsensiTrenCache() {
    }

    /** Awalan kunci cache agar tidak bentrok dengan entri dasbor lain. */
    private static final String PFX = "AbsensiTren::";

    /**
     * Deret tren kehadiran satu perkuliahan, siap dirender grafik garis. Semua array selaras
     * indeksnya dengan {@link #labels}.
     */
    public static final class TrenKehadiran implements Serializable {
        private static final long serialVersionUID = 1L;
        /** label tiap titik, mis. "Pert. 1", "Pert. 2", ... */
        public List<String> labels = new ArrayList<String>();
        /** persentase hadir (0&ndash;100) tiap pertemuan. */
        public int[] persenHadir = new int[0];
        /** jumlah hadir tiap pertemuan. */
        public int[] hadir = new int[0];
        /** jumlah peserta yang sudah diabsen (basis persentase) tiap pertemuan. */
        public int[] diabsen = new int[0];

        /** {@code true} bila tidak ada satu pun pertemuan yang dapat ditampilkan. */
        public boolean kosong() {
            return labels == null || labels.isEmpty();
        }
    }

    /**
     * Ambil deret tren kehadiran untuk {@code perkuliahanId} dengan strategi L1&rarr;L2&rarr;L3,
     * menghitung dari basis data hanya bila ketiganya gagal (cache miss).
     *
     * @param perkuliahanId id perkuliahan/kelas; boleh {@code null} (mengembalikan deret kosong).
     * @return deret tren (tidak pernah {@code null}).
     */
    public static TrenKehadiran ambil(Long perkuliahanId) {
        if (perkuliahanId == null) {
            return new TrenKehadiran();
        }
        String key = PFX + perkuliahanId;

        // L1 — Execution (satu permintaan)
        try {
            Execution ex = Executions.getCurrent();
            if (ex != null) {
                Object l1 = ex.getAttribute(key);
                if (l1 instanceof TrenKehadiran) {
                    return (TrenKehadiran) l1;
                }
            }
        } catch (Throwable t) { ais.common.ErrorAuditUtil.record(t, "auto-audit(empty-catch) src/ais/common/AbsensiTrenCache.java:107");
        }

        // L2 — Desktop/sesi
        Object l2 = DashboardCacheUtil.getL2(key);
        if (l2 instanceof TrenKehadiran) {
            simpanL1(key, l2);
            return (TrenKehadiran) l2;
        }

        // L3 — JVM (data kelas, non-personal)
        Object l3 = DashboardCacheUtil.getL3(key);
        if (l3 instanceof TrenKehadiran) {
            DashboardCacheUtil.putL2(key, l3);
            simpanL1(key, l3);
            return (TrenKehadiran) l3;
        }

        // Cache miss — hitung dari DB lalu isi semua tingkat.
        TrenKehadiran data;
        try {
            data = hitung(perkuliahanId);
        } catch (Throwable t) {
            data = new TrenKehadiran();
        }
        DashboardCacheUtil.putL2(key, data);
        DashboardCacheUtil.putL3(key, data);
        simpanL1(key, data);
        return data;
    }

    /**
     * Buang entri cache (L2 &amp; L3) untuk satu perkuliahan agar render berikutnya mengambil data
     * segar. Panggil setelah menyimpan/mengubah absensi pertemuan pada perkuliahan tersebut.
     *
     * @param perkuliahanId id perkuliahan yang absensinya berubah.
     */
    public static void invalidasi(Long perkuliahanId) {
        if (perkuliahanId == null) {
            return;
        }
        String key = PFX + perkuliahanId;
        DashboardCacheUtil.invalidateL2(key);
        DashboardCacheUtil.invalidateL3(key);
    }

    /** Simpan ke L1 (Execution) secara gagal-aman. */
    private static void simpanL1(String key, Object data) {
        try {
            Execution ex = Executions.getCurrent();
            if (ex != null) {
                ex.setAttribute(key, data);
            }
        } catch (Throwable t) { ais.common.ErrorAuditUtil.record(t, "auto-audit(empty-catch) src/ais/common/AbsensiTrenCache.java:160");
        }
    }

    /**
     * Hitung deret tren langsung dari basis data: telusuri pertemuan perkuliahan (urut nomor lalu
     * tanggal), baca string absensi tiap pertemuan, hitung hadir &amp; total diabsen peserta didik.
     */
    @SuppressWarnings("unchecked")
    private static TrenKehadiran hitung(Long perkuliahanId) {
        TrenKehadiran hasil = new TrenKehadiran();
        Session session = HibernateUtil.currentSession();
        // Proyeksi: ambil HANYA kolom yang dibutuhkan (nomor, tanggal, string absensi) agar tidak
        // menghidrasi entitas Pertemuan beserta asosiasi lazy-nya — jauh lebih hemat memori.
        List<Object[]> baris = session.createCriteria(Pertemuan.class)
                .createAlias("perkuliahan", "pk").add(Restrictions.eq("pk.id", perkuliahanId))
                .setProjection(Projections.projectionList().add(Projections.property("pertemuanKe"))
                        .add(Projections.property("tanggal")).add(Projections.property("absensi")))
                .addOrder(Order.asc("pertemuanKe")).addOrder(Order.asc("tanggal")).list();

        List<Integer> persenList = new ArrayList<Integer>();
        List<Integer> hadirList = new ArrayList<Integer>();
        List<Integer> diabsenList = new ArrayList<Integer>();
        int urut = 0;
        for (Object[] b : baris) {
            if (b == null) {
                continue;
            }
            Integer ke = (b.length > 0 && b[0] instanceof Integer) ? (Integer) b[0] : null;
            String absensi = (b.length > 2 && b[2] instanceof String) ? (String) b[2] : null;
            int[] hd = hitungHadirDiabsen(absensi);
            int hadir = hd[0];
            int diabsen = hd[1];
            if (diabsen <= 0) {
                continue; // pertemuan belum diabsen → lewati
            }
            urut++;
            hasil.labels.add("Pert. " + (ke != null ? ke : Integer.valueOf(urut)));
            persenList.add(Integer.valueOf((int) Math.round(hadir * 100.0 / diabsen)));
            hadirList.add(Integer.valueOf(hadir));
            diabsenList.add(Integer.valueOf(diabsen));
        }
        hasil.persenHadir = toIntArray(persenList);
        hasil.hadir = toIntArray(hadirList);
        hasil.diabsen = toIntArray(diabsenList);
        return hasil;
    }

    /**
     * Hitung [jumlahHadir, jumlahDiabsen] peserta didik (mahasiswa/siswa) dari string absensi satu
     * pertemuan. Diabsen = Hadir + Alpa + Izin + Sakit (mengabaikan yang belum diabsen).
     *
     * @param absensi string absensi pertemuan (boleh {@code null}).
     */
    private static int[] hitungHadirDiabsen(String absensi) {
        int hadir = 0, diabsen = 0;
        try {
            if (absensi != null && absensi.trim().length() > 0) {
                String[] entri = absensi.split(";");
                for (int i = 0; i < entri.length; i++) {
                    String nn = entri[i];
                    if (nn == null) {
                        continue;
                    }
                    String low = nn.toLowerCase();
                    boolean pesertaDidik = low.endsWith("mahasiswa") || low.endsWith("siswa");
                    if (!pesertaDidik) {
                        continue; // hanya hitung peserta didik, bukan dosen/guru
                    }
                    String[] split = nn.split(",");
                    String kode = normalKode(split.length >= 3 ? split[2] : "-");
                    if ("-".equals(kode)) {
                        continue; // belum diabsen
                    }
                    diabsen++;
                    if ("M".equals(kode)) {
                        hadir++;
                    }
                }
            }
        } catch (Throwable t) { ais.common.ErrorAuditUtil.record(t, "auto-audit(empty-catch) src/ais/common/AbsensiTrenCache.java:240");
        }
        return new int[] { hadir, diabsen };
    }

    /** Normalkan kode status menjadi M/A/S/I/- (selaras dengan parsing dashboard e-Learning). */
    private static String normalKode(String kode) {
        if (kode == null || kode.trim().length() == 0) {
            return "-";
        }
        String clean = kode.trim().toUpperCase();
        if (clean.length() > 1 && !"-".equals(clean)) {
            clean = clean.substring(0, 1);
        }
        if (!("M".equals(clean) || "A".equals(clean) || "S".equals(clean) || "I".equals(clean))) {
            return "-";
        }
        return clean;
    }

    private static int[] toIntArray(List<Integer> list) {
        int[] arr = new int[list.size()];
        for (int i = 0; i < list.size(); i++) {
            arr[i] = list.get(i).intValue();
        }
        return arr;
    }
}
