package ais.common.home;

import ais.common.Common;
import ais.database.model.Konfigurasi;

/**
 * Kelas bantu kecil untuk membaca saklar/konfigurasi yang mengatur tampil-tidaknya
 * bagian-bagian (section) pada halaman portal beranda (home portal) AIS.
 *
 * <p>
 * Alih-alih memanggil {@code Common.getKonfigurasi}/{@code Common.bolehKonfigurasi} langsung dan
 * berulang di banyak tempat pada kode rendering beranda, layar/komponen portal cukup memanggil
 * {@link #value(String, String)} atau {@link #enabled(String, boolean)} lewat instans kelas ini.
 * Kedua method membungkus akses konfigurasi dengan penanganan galat yang seragam: bila
 * pengambilan konfigurasi gagal (mis. {@link ais.database.model.Konfigurasi} tidak ditemukan atau
 * terjadi galat database), kegagalan tersebut dicatat ke {@link ais.common.ErrorAuditUtil} dan
 * nilai {@code fallback} yang diberikan pemanggil dikembalikan sebagai pengganti, sehingga
 * kegagalan baca konfigurasi tidak membuat rendering halaman beranda ikut gagal.
 * </p>
 *
 * <p>
 * Kelas ini tidak menyimpan state sendiri (tidak ada field instans) — semua method murni
 * membaca konfigurasi setiap kali dipanggil, sehingga aman dipakai lintas-request tanpa
 * sinkronisasi khusus.
 * </p>
 */
public class HomePortalSectionResolver {

    /**
     * Mengambil nilai mentah (string) suatu kunci konfigurasi, dengan {@code fallback} sebagai
     * nilai default sekaligus nilai yang digunakan bila {@link ais.database.model.Konfigurasi}
     * dengan kunci tersebut tidak ditemukan atau pengambilannya gagal.
     *
     * @param key      kunci konfigurasi yang dicari (lihat {@code ais.database.model.Konfigurasi})
     * @param fallback nilai yang dikembalikan bila konfigurasi tidak ada/kosong atau terjadi galat
     * @return nilai konfigurasi yang sudah di-{@code trim()}, atau {@code fallback}
     */
    public String value(String key, String fallback) {
        try {
            Konfigurasi c = Common.getKonfigurasi(key, fallback);
            return c != null && c.getNilai() != null ? c.getNilai().trim() : fallback;
        } catch (Exception e) {
            ais.common.ErrorAuditUtil.record(e, "HomePortalSectionResolver.value " + key);
            return fallback;
        }
    }

    /**
     * Menerjemahkan nilai konfigurasi bertipe saklar aktif/tidak-aktif menjadi {@code boolean}.
     * Nilai dianggap {@code true} bila sama (tanpa membedakan huruf besar/kecil) dengan
     * {@link Konfigurasi#AKTIF}, {@code "true"}, atau {@code "yes"}.
     *
     * @param key      kunci konfigurasi saklar yang dicari
     * @param fallback nilai default bila konfigurasi tidak ditemukan; diterjemahkan lebih dulu ke
     *                 {@link Konfigurasi#AKTIF}/{@link Konfigurasi#TIDAK_AKTIF} sebelum dipakai
     *                 sebagai fallback pada {@link #value(String, String)}
     * @return {@code true} bila nilai konfigurasi menandakan aktif, {@code false} sebaliknya
     */
    public boolean enabled(String key, boolean fallback) {
        String value = value(key, fallback ? Konfigurasi.AKTIF : Konfigurasi.TIDAK_AKTIF);
        return Konfigurasi.AKTIF.equalsIgnoreCase(value) || "true".equalsIgnoreCase(value) || "yes".equalsIgnoreCase(value);
    }
}
