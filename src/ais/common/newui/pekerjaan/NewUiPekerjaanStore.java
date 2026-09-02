package ais.common.newui.pekerjaan;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Penyimpan status pekerjaan latar yang dapat ditanya ulang.
 *
 * <h3>Mengapa ini ada</h3>
 * <p>Layar-layar impor dan integrator pada sistem lama menjalankan pekerjaan
 * panjang sambil melaporkan kemajuannya ke widget ZK yang dipegang utas latar.
 * Widget itu hidup selama halaman ZK terbuka, sehingga tidak ada catatan di
 * sisi server yang dapat ditanya ulang oleh sebuah API — dan itulah alasan
 * seluruh keluarga layar tersebut ditangguhkan selama ini.</p>
 *
 * <p>Penyimpan ini menyediakan catatan tersebut: satu baris status per
 * pekerjaan, dapat ditanya kapan saja lewat id-nya, dan bertahan melewati umur
 * permintaan HTTP yang memulainya.</p>
 *
 * <h3>Di memori, dan itu disengaja</h3>
 * <p>Status disimpan di memori aplikasi, bukan di basis data. Konsekuensinya
 * jujur dan harus diketahui pemanggilnya: <b>restart Tomcat menghapus seluruh
 * catatan</b>, dan pada pemasangan berklaster sebuah pekerjaan hanya terlihat
 * dari node yang menjalankannya. Untuk pekerjaan yang berjalan hitungan menit
 * itu memadai; bila kelak dibutuhkan pekerjaan yang harus selamat dari restart,
 * yang berubah adalah kelas ini saja, bukan pemanggilnya.</p>
 *
 * <h3>Terikat pemilik</h3>
 * <p>Setiap pekerjaan mencatat id pengguna yang memulainya, dan
 * {@link #ambil(String, Long)} menolak permintaan dari pengguna lain — id
 * pekerjaan yang bocor tidak boleh memperlihatkan kemajuan impor orang lain.
 * Penolakannya berupa {@code null}, sehingga pemanggil tidak dapat membedakan
 * "bukan milik Anda" dari "tidak ada" — pembedaan itu sendiri sudah bocor.</p>
 */
public final class NewUiPekerjaanStore {

    /** Menunggu dijalankan. */
    public static final String ANTRE = "ANTRE";
    /** Sedang berjalan. */
    public static final String BERJALAN = "BERJALAN";
    /** Selesai tanpa galat. */
    public static final String SELESAI = "SELESAI";
    /** Berhenti karena galat. */
    public static final String GAGAL = "GAGAL";

    /**
     * Batas jumlah pekerjaan yang disimpan.
     *
     * <p>Tanpa batas, sebuah proses yang memulai pekerjaan berulang kali akan
     * membuat peta ini tumbuh sampai memori habis. Ketika penuh, yang dibuang
     * adalah pekerjaan <b>yang sudah selesai</b> dan paling lama — pekerjaan
     * yang masih berjalan tidak pernah dibuang, karena membuangnya berarti
     * kehilangan satu-satunya cara menanyakan hasilnya.</p>
     */
    static final int BATAS = 200;

    private static final Map<String, Pekerjaan> ISI = new ConcurrentHashMap<String, Pekerjaan>();
    private static final AtomicLong URUT = new AtomicLong(1L);

    private NewUiPekerjaanStore() { }

    /** Satu baris status pekerjaan. */
    public static final class Pekerjaan {
        private final String id;
        private final Long pemilik;
        private final String jenis;
        private final long mulai;
        private volatile String status;
        private volatile String pesan;
        private volatile int total;
        private volatile int diproses;
        private volatile long selesai;

        Pekerjaan(String id, Long pemilik, String jenis) {
            this.id = id;
            this.pemilik = pemilik;
            this.jenis = jenis;
            this.mulai = System.currentTimeMillis();
            this.status = ANTRE;
            this.pesan = "";
        }

        public String getId() { return id; }
        public Long getPemilik() { return pemilik; }
        public String getJenis() { return jenis; }
        public String getStatus() { return status; }
        public String getPesan() { return pesan; }
        public int getTotal() { return total; }
        public int getDiproses() { return diproses; }
        public long getMulai() { return mulai; }
        public long getSelesai() { return selesai; }
        public boolean isTuntas() { return SELESAI.equals(status) || GAGAL.equals(status); }
    }

    /** Daftarkan pekerjaan baru; mengembalikan id-nya. */
    public static String daftar(Long pemilik, String jenis) {
        rapikan();
        String id = "job-" + System.currentTimeMillis() + "-" + URUT.getAndIncrement();
        ISI.put(id, new Pekerjaan(id, pemilik, jenis));
        return id;
    }

    /**
     * Ambil status sebuah pekerjaan milik {@code pemilik}.
     *
     * @return {@code null} bila tidak ada, atau bila pemiliknya bukan pemanggil
     */
    public static Pekerjaan ambil(String id, Long pemilik) {
        if (id == null || pemilik == null) return null;
        Pekerjaan p = ISI.get(id);
        if (p == null) return null;
        if (p.pemilik == null || !p.pemilik.equals(pemilik)) return null;
        return p;
    }

    /** Pekerjaan milik seseorang, terbaru lebih dulu. */
    public static List<Pekerjaan> milik(Long pemilik) {
        List<Pekerjaan> hasil = new ArrayList<Pekerjaan>();
        if (pemilik == null) return hasil;
        for (Pekerjaan p : ISI.values()) {
            if (pemilik.equals(p.pemilik)) hasil.add(p);
        }
        Collections.sort(hasil, new Comparator<Pekerjaan>() {
            public int compare(Pekerjaan a, Pekerjaan b) {
                return a.mulai == b.mulai ? 0 : (a.mulai > b.mulai ? -1 : 1);
            }
        });
        return hasil;
    }

    /** Tandai mulai berjalan, dengan jumlah satuan kerja bila diketahui. */
    public static void berjalan(String id, int total, String pesan) {
        Pekerjaan p = ISI.get(id);
        if (p == null) return;
        p.status = BERJALAN;
        p.total = total;
        if (pesan != null) p.pesan = pesan;
    }

    /** Perbarui kemajuan. */
    public static void kemajuan(String id, int diproses, String pesan) {
        Pekerjaan p = ISI.get(id);
        if (p == null) return;
        p.diproses = diproses;
        if (pesan != null) p.pesan = pesan;
    }

    /** Tandai selesai. */
    public static void selesai(String id, String pesan) {
        Pekerjaan p = ISI.get(id);
        if (p == null) return;
        p.status = SELESAI;
        p.selesai = System.currentTimeMillis();
        if (pesan != null) p.pesan = pesan;
    }

    /** Tandai gagal beserta alasannya. */
    public static void gagal(String id, String pesan) {
        Pekerjaan p = ISI.get(id);
        if (p == null) return;
        p.status = GAGAL;
        p.selesai = System.currentTimeMillis();
        p.pesan = pesan == null ? "Pekerjaan gagal." : pesan;
    }

    /** Jumlah pekerjaan tersimpan; dipakai uji mandiri. */
    public static int jumlah() {
        return ISI.size();
    }

    /**
     * Kosongkan seluruh catatan.
     *
     * <p>Hanya dipakai uji mandiri. Publik karena ujinya berada di paket lain
     * (paket pemanggil, bukan paket ini), dan bukan karena ada pemakaian
     * produksi yang sah — memanggilnya saat aplikasi berjalan akan menghapus
     * status pekerjaan yang sedang ditunggu penggunanya.</p>
     */
    public static void kosongkan() {
        ISI.clear();
    }

    /**
     * Buang pekerjaan selesai terlama ketika sudah melewati batas.
     *
     * <p>Yang masih berjalan tidak pernah dibuang: membuangnya menghilangkan
     * satu-satunya cara menanyakan hasil pekerjaan yang belum tuntas.</p>
     */
    private static void rapikan() {
        if (ISI.size() < BATAS) return;
        List<Pekerjaan> tuntas = new ArrayList<Pekerjaan>();
        for (Pekerjaan p : ISI.values()) {
            if (p.isTuntas()) tuntas.add(p);
        }
        Collections.sort(tuntas, new Comparator<Pekerjaan>() {
            public int compare(Pekerjaan a, Pekerjaan b) {
                return a.mulai == b.mulai ? 0 : (a.mulai < b.mulai ? -1 : 1);
            }
        });
        int buang = ISI.size() - BATAS + 1;
        for (int i = 0; i < tuntas.size() && i < buang; i++) {
            ISI.remove(tuntas.get(i).id);
        }
    }
}
