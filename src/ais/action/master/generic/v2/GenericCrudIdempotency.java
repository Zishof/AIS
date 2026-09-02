package ais.action.master.generic.v2;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import ais.database.model.Tbmuser;

/**
 * Deduplikasi mutasi CRUD generik berdasarkan {@code clientMutationId}.
 *
 * <p>Aplikasi native menyimpan perubahan ke antrean lokal ketika jaringan
 * putus, lalu mengirimkannya lagi setelah tersambung. Tanpa deduplikasi, satu
 * percobaan ulang yang jawabannya tidak pernah sampai ke perangkat akan
 * membuat data yang sama tersimpan dua kali — dan itu sebabnya jalur tulis
 * local-first sebelumnya sengaja tidak pernah diantrikan sama sekali.</p>
 *
 * <p><b>Kuncinya menyertakan pemilik.</b> Id mutasi dibuat perangkat, jadi
 * tanpa pengguna pada kunci, id yang ditebak atau bocor dapat memanen jawaban
 * mutasi milik orang lain.</p>
 *
 * <p><b>Batas yang perlu diketahui:</b> penyimpanannya di memori JVM, bukan
 * basis data. Ia bertahan melintasi sesi HTTP — justru itu yang dibutuhkan,
 * sebab antrean dikirim ulang setelah sesi lama berakhir — tetapi hilang saat
 * Tomcat dimulai ulang. Restart di tengah pengiriman ulang tetap dapat
 * menggandakan satu mutasi. Menaruhnya di basis data akan menutup celah itu,
 * dan itu langkah berikutnya, bukan yang ini.</p>
 */
public final class GenericCrudIdempotency {

    /** Batas usia catatan; antrean offline yang wajar terkuras jauh di bawah ini. */
    private static final long TTL_MS = 24L * 60L * 60L * 1000L;

    /** Batas jumlah catatan supaya memori tidak tumbuh tanpa batas. */
    private static final int MAKS = 5000;

    private static final Map CATATAN = Collections.synchronizedMap(
            new LinkedHashMap(256, 0.75f, true) {
                private static final long serialVersionUID = 1L;
                protected boolean removeEldestEntry(Map.Entry eldest) {
                    return size() > MAKS;
                }
            });

    private GenericCrudIdempotency() { }

    /**
     * Kunci deduplikasi, atau {@code null} bila klien tidak mengirim
     * {@code clientMutationId} — permintaan biasa dari layar tidak
     * dideduplikasi, dan memang tidak perlu.
     */
    public static String kunci(HttpServletRequest request, GenericCrudRequestContext context) {
        if (request == null) return null;
        String id = request.getParameter("clientMutationId");
        if (id == null || id.trim().length() == 0) return null;
        id = id.trim();
        if (id.length() > 200) return null;
        Tbmuser user = context == null ? null : context.getUser();
        Object pemilik = user == null ? null : user.getUserId();
        if (pemilik == null) return null;
        return String.valueOf(pemilik) + "|" + id;
    }

    /** Jawaban yang sudah pernah diberikan untuk kunci ini, atau null. */
    public static Object hasilSebelumnya(String kunci) {
        if (kunci == null) return null;
        Catatan catatan = (Catatan) CATATAN.get(kunci);
        if (catatan == null) return null;
        if (System.currentTimeMillis() - catatan.waktu > TTL_MS) {
            CATATAN.remove(kunci);
            return null;
        }
        return catatan.hasil;
    }

    /**
     * Simpan hasil lalu kembalikan hasil itu sendiri, supaya pemanggil dapat
     * menuliskannya sebagai satu ekspresi.
     */
    public static Object simpan(String kunci, Object hasil) {
        if (kunci != null && hasil != null) {
            Catatan catatan = new Catatan();
            catatan.hasil = hasil;
            catatan.waktu = System.currentTimeMillis();
            CATATAN.put(kunci, catatan);
        }
        return hasil;
    }

    /** Dipakai uji mandiri; tidak dipanggil jalur produksi. */
    static void bersihkan() {
        CATATAN.clear();
    }

    /** Jumlah catatan tersimpan; dipakai uji mandiri. */
    static int jumlah() {
        return CATATAN.size();
    }

    /**
     * Tipe implementasi bersarang penampung satu jawaban beserta waktunya.
     *
     * <p>Waktu disimpan bersama hasil agar TTL dapat diperiksa tanpa struktur
     * kedua yang harus dijaga tetap sinkron.</p>
     */
    private static final class Catatan {
        Object hasil;
        long waktu;
    }
}
