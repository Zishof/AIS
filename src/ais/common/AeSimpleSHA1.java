package ais.common;


import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utility statis untuk menghitung digest SHA-1 dari sebuah string dan mengembalikannya sebagai
 * teks heksadesimal, memakai implementasi {@link MessageDigest} bawaan JCE dengan encoding byte
 * {@code "iso-8859-1"} (bukan UTF-8 seperti {@link SHA512}). Nama kelas ("AeSimpleSHA1") dan gaya
 * penulisan kodenya menunjukkan asal-usulnya sebagai potongan kode contoh/tutorial umum yang
 * banyak beredar di internet untuk keperluan hashing SHA-1 sederhana di Java, kemudian
 * diadopsi ke dalam basis kode AIS.
 *
 * <p>
 * Alur kerja: {@link #SHA1(String)} membuat instance {@link MessageDigest} dengan algoritma
 * {@code "SHA-1"}, mengonsumsi byte {@code ISO-8859-1} dari teks input, menghasilkan digest 20
 * byte (160 bit), lalu {@link #convertToHex(byte[])} mengubahnya menjadi string heksadesimal 40
 * karakter. Berdasarkan penggunaan pada {@link #main(String[])} — yang menggabungkan nilai nominal
 * uang, sebuah token/kunci, dan kode berformat enam digit sebelum di-hash — kelas ini tampak
 * dipakai (atau pernah dipakai sebagai contoh) untuk membangun tanda tangan/checksum sederhana
 * pada payload transaksi, pola yang umum dijumpai pada integrasi payment gateway di modul lain
 * (lihat pembanding pada file di direktori ini yang menangani integrasi payment gateway).
 * </p>
 *
 * <h2>Catatan keamanan — SHA-1 sudah lemah secara kriptografis</h2>
 * <p>
 * SHA-1 SECARA FAKTUAL sudah dianggap lemah secara kriptografis sejak beberapa tahun terakhir:
 * serangan collision praktis terhadap SHA-1 telah didemonstrasikan secara publik (mis. penelitian
 * "SHAttered" tahun 2017), yang berarti dua input berbeda dapat dibuat menghasilkan digest SHA-1
 * yang identik dengan biaya komputasi yang terjangkau bagi penyerang bermodal cukup. SHA-1
 * TIDAK lagi direkomendasikan untuk kebutuhan yang bergantung pada ketahanan collision, seperti
 * tanda tangan digital, checksum integritas yang harus tahan terhadap pemalsuan, atau hashing
 * password. Bila kelas ini dipakai untuk membangun signature/checksum verifikasi transaksi (mis.
 * validasi callback dari payment gateway), risiko praktisnya bergantung pada seberapa mudah
 * penyerang dapat mengontrol input yang di-hash — pada kasus SHA-1 disarankan migrasi ke
 * algoritma yang lebih kuat (mis. SHA-256 seperti dipakai {@link SHA512} untuk SHA-512, atau
 * algoritma HMAC) bila memungkinkan tanpa mengganggu kompatibilitas dengan pihak eksternal yang
 * sudah mengandalkan skema SHA-1 ini. Dokumentasi ini TIDAK mengubah algoritma yang dipakai
 * karena perubahan semacam itu berisiko memutus kompatibilitas dengan pihak lain yang sudah
 * bergantung pada skema hash saat ini.
 * </p>
 */
public class AeSimpleSHA1 {

    /**
     * Mengubah larik byte digest menjadi representasi string heksadesimal huruf kecil, dua
     * karakter per byte.
     *
     * @param data larik byte digest (mis. hasil {@link MessageDigest#digest()})
     * @return string heksadesimal hasil konversi {@code data}
     */
    private static String convertToHex(byte[] data) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < data.length; i++) {
            int halfbyte = (data[i] >>> 4) & 0x0F;
            int two_halfs = 0;
            do {
                if ((0 <= halfbyte) && (halfbyte <= 9)) {
                    buf.append((char) ('0' + halfbyte));
                } else {
                    buf.append((char) ('a' + (halfbyte - 10)));
                }
                halfbyte = data[i] & 0x0F;
            } while (two_halfs++ < 1);
        }
        return buf.toString();
    }

    /**
     * Menghitung digest SHA-1 dari {@code text} (dibaca sebagai byte {@code ISO-8859-1}) dan
     * mengembalikannya sebagai string heksadesimal 40 karakter.
     *
     * @param text teks yang akan di-hash
     * @return representasi heksadesimal dari digest SHA-1 {@code text}
     * @throws NoSuchAlgorithmException    bila penyedia JCE tidak menyediakan algoritma
     *                                      {@code "SHA-1"} (secara praktis tidak pernah terjadi
     *                                      pada JRE standar)
     * @throws UnsupportedEncodingException bila encoding {@code "iso-8859-1"} tidak dikenali JVM
     *                                      (secara praktis tidak pernah terjadi)
     */
    public static String SHA1(String text)
            throws NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest md;
        md = MessageDigest.getInstance("SHA-1");
        byte[] sha1hash = new byte[40];
        md.update(text.getBytes("iso-8859-1"), 0, text.length());
        sha1hash = md.digest();
        return convertToHex(sha1hash);
    }

    /**
     * Titik masuk baris perintah untuk mendemonstrasikan/menguji manual perhitungan SHA-1 kelas
     * ini. Menggabungkan sebuah nominal uang contoh ({@code "75000.00"}), shared key Doku uji
     * (disuplai lewat properti sistem, lihat catatan keamanan di bawah), dan kode enam digit
     * ({@code "000001"}) menjadi satu string, menghitung SHA-1-nya lewat {@link #SHA1(String)},
     * lalu mencetak hasilnya ke konsol.
     *
     * <p>
     * <b>DIPERBAIKI 2026-09-01:</b> string literal yang sebelumnya ada di sini adalah shared key
     * Doku nyata ({@code doku_key} — CONFIRMED identik dengan default yang sebelumnya tertanam di
     * {@code ais.common.DokuCommon}, sudah diperbaiki terpisah), bukan sekadar data contoh.
     * Literal itu sudah dihapus — kunci uji kini WAJIB disuplai lewat properti sistem
     * {@code -Daesimplesha1.testkey=...} saat method ini dijalankan manual. Method ini tidak
     * dipanggil oleh kode aplikasi lain (hanya dapat dijalankan manual sebagai program mandiri).
     * </p>
     *
     * @param args argumen baris perintah (tidak dipakai)
     * @throws IOException              tidak pernah benar-benar dilempar pada alur ini, dideklarasikan
     *                                   mengikuti signature asli
     * @throws NoSuchAlgorithmException diteruskan dari {@link #SHA1(String)} bila algoritma
     *                                   {@code "SHA-1"} tidak tersedia
     */
    public static void main(String[] args) throws IOException, NoSuchAlgorithmException {

        String testKey = System.getProperty("aesimplesha1.testkey", "");
        if (testKey.trim().isEmpty()) {
            System.out.println("Jalankan dengan -Daesimplesha1.testkey=...");
            return;
        }
        String WORDS = "75000.00" + testKey + "000001";
        System.out.println("WORDS=>" + WORDS + " -> " + AeSimpleSHA1.SHA1(WORDS));
    }
}
