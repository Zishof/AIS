package ais.common.newui.pekerjaan;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Catatan pekerjaan panjang di sisi server, supaya kemajuannya dapat ditanya
 * ulang lewat API.
 *
 * <p><b>Mengapa ini ada.</b> Layar-layar penyiapan berkas Feeder mengerjakan
 * ekspor besar di utas latar dan melaporkan kemajuannya dengan memperbarui
 * widget ZK yang dipegang utas itu. Cara tersebut hanya bekerja selama layarnya
 * terbuka: tidak ada apa pun di sisi server yang bisa ditanya "sudah sampai
 * mana?". Sebuah API tidak punya widget untuk dipegang, sehingga pekerjaan
 * semacam itu tidak dapat disajikan sama sekali sebelum catatan status ini
 * ada. Kelas ini adalah catatan tersebut.</p>
 *
 * <h3>Kepemilikan</h3>
 * <p>Tiap pekerjaan mencatat pengguna yang memulainya, dan hanya pengguna itu
 * yang boleh menanyakan kemajuannya maupun mengunduh hasilnya. Berkas ekspor
 * Feeder memuat data pribadi mahasiswa lengkap dengan NIK dan alamat; membiarkan
 * siapa pun yang menebak id pekerjaan mengunduhnya akan menjadikan fitur ini
 * kebocoran data, bukan kemudahan.</p>
 *
 * <h3>Batas yang disengaja</h3>
 * <p>Catatan disimpan di memori proses, sehingga hilang bila aplikasi dimuat
 * ulang — pekerjaan yang sedang berjalan pun ikut berhenti bersama prosesnya,
 * persis seperti perilaku utas latar yang digantikannya. Ini bukan antrean
 * pekerjaan yang tahan mati listrik, melainkan pengganti setia dari yang sudah
 * ada, hanya saja dapat ditanya. Menjadikannya tahan restart menuntut tabel
 * dan penjadwal tersendiri.</p>
 */
public final class PekerjaanRegistry {

    /** Baru dibuat, belum mulai dikerjakan. */
    public static final String ANTRE = "antre";
    /** Sedang dikerjakan. */
    public static final String BERJALAN = "berjalan";
    /** Selesai; berkasnya siap diunduh. */
    public static final String SELESAI = "selesai";
    /** Gagal; {@code pesan} memuat sebabnya. */
    public static final String GAGAL = "gagal";

    /** Batas jumlah catatan yang disimpan; yang terlama dibuang lebih dulu. */
    private static final int BATAS_CATATAN = 200;
    /** Umur maksimal catatan selesai/gagal sebelum dibersihkan (2 jam). */
    private static final long UMUR_MAKS = 2L * 60L * 60L * 1000L;

    private static final Map<String, Pekerjaan> CATATAN = new ConcurrentHashMap<String, Pekerjaan>();

    /** Satu pekerjaan beserta kemajuannya. */
    public static final class Pekerjaan {
        public final String id;
        public final String jenis;
        public final String judul;
        public final String pemilik;
        public final long mulai;

        volatile String status = ANTRE;
        volatile int persen;
        volatile String pesan = "";
        volatile long selesai;
        volatile File berkas;
        volatile String namaBerkas = "";

        Pekerjaan(String id, String jenis, String judul, String pemilik) {
            this.id = id;
            this.jenis = jenis;
            this.judul = judul;
            this.pemilik = pemilik;
            this.mulai = System.currentTimeMillis();
        }

        public String getStatus() { return status; }
        public int getPersen() { return persen; }
        public String getPesan() { return pesan; }
        public long getSelesai() { return selesai; }
        public File getBerkas() { return berkas; }
        public String getNamaBerkas() { return namaBerkas; }

        /** true bila pekerjaan sudah tidak berubah lagi. */
        public boolean tuntas() {
            return SELESAI.equals(status) || GAGAL.equals(status);
        }
    }

    /**
     * Kanal pelaporan kemajuan yang diberikan kepada pekerjaan.
     *
     * <p>Sengaja sesederhana ini supaya kode ekspor yang sama dapat dipakai
     * layar ZK (yang meneruskannya ke label) maupun pekerjaan latar (yang
     * meneruskannya ke catatan ini).</p>
     */
    public interface Progres {
        /**
         * @param persen 0..100
         * @param pesan  keterangan singkat untuk ditampilkan
         */
        void lapor(int persen, String pesan);
    }

    /** Pekerjaan yang dijalankan di utas latar. */
    public interface Tugas {
        /**
         * @param progres kanal pelaporan kemajuan
         * @return berkas hasil, atau null bila pekerjaan tidak menghasilkan berkas
         */
        File kerjakan(Progres progres) throws Exception;
    }

    private PekerjaanRegistry() { }

    /**
     * Mulai satu pekerjaan di utas latar dan kembalikan id-nya.
     *
     * <p>Id dibangkitkan acak, bukan berurut: id berurut membuat pekerjaan
     * milik orang lain mudah ditebak. Penebakan itu sendiri sudah ditolak
     * pemeriksaan kepemilikan, tetapi tidak ada gunanya mempermudahnya.</p>
     */
    public static String mulai(final String jenis, final String judul, final String pemilik,
            final String namaBerkas, final Tugas tugas) {
        bersihkan();
        String id = UUID.randomUUID().toString().replace("-", "");
        final Pekerjaan p = new Pekerjaan(id, jenis, judul, pemilik == null ? "" : pemilik);
        p.namaBerkas = namaBerkas == null ? "" : namaBerkas;
        CATATAN.put(id, p);

        Thread utas = new Thread(new Runnable() {
            public void run() {
                p.status = BERJALAN;
                p.pesan = "Mulai menyiapkan berkas.";
                try {
                    File hasil = tugas.kerjakan(new Progres() {
                        public void lapor(int persen, String pesan) {
                            if (persen < 0) persen = 0;
                            if (persen > 100) persen = 100;
                            p.persen = persen;
                            if (pesan != null) p.pesan = pesan;
                        }
                    });
                    p.berkas = hasil;
                    p.persen = 100;
                    p.status = SELESAI;
                    p.pesan = "Berkas siap diunduh.";
                } catch (Throwable e) {
                    p.status = GAGAL;
                    // Pesan galat mentah tidak dikirim ke klien; yang dicatat di
                    // sini hanya ringkasannya, rinciannya masuk log server.
                    p.pesan = "Penyiapan berkas gagal. Detail dicatat di log server.";
                    try {
                        ais.common.ErrorAuditUtil.record(
                                e instanceof Exception ? (Exception) e : new Exception(e),
                                "PekerjaanRegistry:" + jenis);
                    } catch (Exception ignored) { }
                } finally {
                    p.selesai = System.currentTimeMillis();
                }
            }
        }, "nui-pekerjaan-" + jenis);
        utas.setDaemon(true);
        utas.start();
        return id;
    }

    /**
     * Pekerjaan milik {@code pemilik} dengan id tertentu.
     *
     * @return null bila tidak ada, atau ada tetapi milik orang lain — keduanya
     *         dijawab sama supaya keberadaan pekerjaan orang lain tidak bocor
     *         lewat perbedaan pesan galat.
     */
    public static Pekerjaan lihat(String id, String pemilik) {
        if (id == null || pemilik == null) return null;
        Pekerjaan p = CATATAN.get(id);
        if (p == null || !pemilik.equals(p.pemilik)) return null;
        return p;
    }

    /** Seluruh pekerjaan milik seorang pengguna, terbaru lebih dulu. */
    public static List<Pekerjaan> milik(String pemilik) {
        List<Pekerjaan> hasil = new ArrayList<Pekerjaan>();
        if (pemilik == null) return hasil;
        for (Pekerjaan p : CATATAN.values()) {
            if (pemilik.equals(p.pemilik)) hasil.add(p);
        }
        Collections.sort(hasil, new Comparator<Pekerjaan>() {
            public int compare(Pekerjaan a, Pekerjaan b) {
                return a.mulai == b.mulai ? 0 : (a.mulai > b.mulai ? -1 : 1);
            }
        });
        return hasil;
    }

    /**
     * Buang catatan yang sudah tuntas dan tua, beserta berkasnya.
     *
     * <p>Berkas ekspor memuat data pribadi; membiarkannya menumpuk di folder
     * sementara server jauh lebih berisiko daripada memaksa pengguna menyiapkan
     * ulang bila terlambat mengunduh.</p>
     */
    private static void bersihkan() {
        long sekarang = System.currentTimeMillis();
        for (Iterator<Map.Entry<String, Pekerjaan>> it = CATATAN.entrySet().iterator(); it.hasNext();) {
            Pekerjaan p = it.next().getValue();
            if (p.tuntas() && p.selesai > 0 && (sekarang - p.selesai) > UMUR_MAKS) {
                hapusBerkas(p);
                it.remove();
            }
        }
        if (CATATAN.size() <= BATAS_CATATAN) return;
        List<Pekerjaan> semua = new ArrayList<Pekerjaan>(CATATAN.values());
        Collections.sort(semua, new Comparator<Pekerjaan>() {
            public int compare(Pekerjaan a, Pekerjaan b) {
                return a.mulai == b.mulai ? 0 : (a.mulai < b.mulai ? -1 : 1);
            }
        });
        int buang = CATATAN.size() - BATAS_CATATAN;
        for (int i = 0; i < semua.size() && buang > 0; i++) {
            Pekerjaan p = semua.get(i);
            if (!p.tuntas()) continue; // pekerjaan berjalan tidak pernah dibuang
            hapusBerkas(p);
            CATATAN.remove(p.id);
            buang--;
        }
    }

    private static void hapusBerkas(Pekerjaan p) {
        try {
            if (p.berkas != null && p.berkas.exists()) p.berkas.delete();
        } catch (Exception ignored) { }
    }
}
