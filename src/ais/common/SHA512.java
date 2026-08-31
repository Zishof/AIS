package ais.common;


import java.security.MessageDigest;

/**
 * Utility kecil untuk menghasilkan digest SHA-512 dari sebuah string (umumnya password) dalam
 * bentuk representasi heksadesimal, memanfaatkan implementasi {@link MessageDigest} bawaan JCE
 * (Java Cryptography Extension) yang sudah tersedia di JRE tanpa dependensi pustaka eksternal.
 *
 * <p>
 * Kelas ini murni statis/stateless: tidak menyimpan state apa pun antar-pemanggilan, sehingga
 * aman dipakai bersamaan dari banyak thread. Alur kerjanya sederhana dan linear: (1)
 * {@link #SHA512Hash(String)} sebagai titik masuk publik meneruskan input ke
 * {@link #sha512(String)}; (2) {@link #sha512(String)} membuat instance {@link MessageDigest}
 * dengan algoritma {@code "SHA-512"}, mengambil representasi byte UTF-8 dari string input, lalu
 * menghitung digest 64-byte (512 bit)-nya; (3) {@link #convertToHex(byte[])} mengubah larik byte
 * digest tersebut menjadi string heksadesimal 128 karakter yang aman disimpan sebagai teks (mis.
 * di kolom password ter-hash pada database).
 * </p>
 *
 * <h2>Catatan keamanan</h2>
 * <p>
 * SHA-512 adalah fungsi hash kriptografis satu-arah yang secara algoritmik masih dianggap kuat
 * terhadap serangan collision maupun preimage hingga saat ini (berbeda dari SHA-1, lihat catatan
 * pada {@link AeSimpleSHA1}). Namun demikian, SHA-512 murni (tanpa salt per-baris dan tanpa
 * faktor kerja/"work factor" yang dapat diatur) BUKAN algoritma yang ideal untuk hashing password
 * dibandingkan fungsi turunan-kunci yang memang dirancang untuk itu (mis. bcrypt, scrypt,
 * Argon2, atau PBKDF2), karena SHA-512 dirancang untuk cepat dihitung — sifat yang justru
 * memudahkan penyerang melakukan brute-force/rainbow-table terhadap hash password bila database
 * bocor, terutama tanpa salt unik per pengguna. Implementasi di kelas ini TIDAK menambahkan salt
 * apa pun sebelum hashing (lihat {@link #sha512(String)}); pemanggil yang menggunakan kelas ini
 * untuk menyimpan kredensial perlu menyadari keterbatasan ini. Kegagalan pembuatan digest (mis.
 * algoritma tidak tersedia) hanya dicetak ke {@code System.err} tanpa dilempar ulang, sehingga
 * {@link #sha512(String)} dapat mengembalikan hasil dari {@code convertToHex(null)} bila terjadi
 * {@link Exception} — pemanggil sebaiknya menyadari kemungkinan ini walau method ini tidak
 * diubah perilakunya di sini.
 * </p>
 */
public class SHA512 {

    /**
     * Titik masuk publik untuk menghitung hash SHA-512 dari sebuah string dan mengembalikannya
     * sebagai teks heksadesimal. Sekadar delegasi ke {@link #sha512(String)}.
     *
     * @param password teks yang akan di-hash (biasanya password mentah sebelum disimpan)
     * @return representasi heksadesimal (128 karakter) dari digest SHA-512 {@code password}
     */
    public static String SHA512Hash(String password) {
        return sha512(password);
    }


    /**
     * Menghitung digest SHA-512 dari {@code password} (dibaca sebagai byte UTF-8) lalu
     * mengubahnya menjadi string heksadesimal lewat {@link #convertToHex(byte[])}. Kegagalan
     * (mis. {@link java.security.NoSuchAlgorithmException} atau
     * {@link java.io.UnsupportedEncodingException}) hanya dicetak ke {@code System.err} dan
     * ditelan (tidak dilempar ulang), sehingga {@code hash} bisa saja tetap {@code null} saat
     * diteruskan ke {@link #convertToHex(byte[])} bila terjadi kegagalan.
     *
     * @param password teks yang akan di-hash
     * @return representasi heksadesimal dari digest SHA-512 {@code password}
     */
    private static String sha512(String password) {
        MessageDigest sha = null;
        byte[] hash = null;
        try {
            sha = MessageDigest.getInstance("SHA-512");
            hash = sha.digest(password.getBytes("UTF-8"));
        } catch (Exception e) {
            System.err.println(e);
        }
        return convertToHex(hash);
    }

    /**
     * Mengubah larik byte digest menjadi representasi string heksadesimal huruf kecil, dua
     * karakter per byte (mis. byte {@code 0x0A} menjadi {@code "0a"}).
     *
     * @param raw larik byte digest (mis. hasil {@link MessageDigest#digest(byte[])})
     * @return string heksadesimal hasil konversi {@code raw}
     */
    private static String convertToHex(byte[] raw) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < raw.length; i++) {
            sb.append(Integer.toString((raw[i] & 0xff) + 0x100, 16).substring(1));
        }
        return sb.toString();
    }
}