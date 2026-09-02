package ais.common.newui.akademik;

/**
 * Menjaga label matakuliah ekivalen pada
 * {@link NewUiUjianMahasiswaController}.
 *
 * <p>Sebuah perkuliahan dapat diambil sebagai matakuliah ekivalen. Layar lama
 * menampilkan {@code utama} saja ketika matakuliah yang diambil sama dengan
 * aslinya, dan {@code utama (asli)} ketika berbeda.</p>
 *
 * <p>Kalau aturan itu disalin keliru, barisnya tetap tampak wajar — ada kode,
 * ada nama, ada SKS — hanya keterangan ekivalensinya yang hilang atau muncul di
 * tempat yang salah. Mahasiswa lalu tidak dapat mencocokkan jadwal ujian dengan
 * matakuliah pada KRS-nya, dan tidak ada yang akan melaporkannya sebagai galat.
 * Karena itu dijaga uji.</p>
 */
public final class NewUiUjianMahasiswaSelfTest {

    private static int gagal = 0;

    public static void main(String[] args) {
        periksa("IF101".equals(
                NewUiUjianMahasiswaController.labelEkivalen("IF101", "IF101", true)),
                "matakuliah yang sama harus tampil tanpa kurung");
        periksa("IF101 (TI201)".equals(
                NewUiUjianMahasiswaController.labelEkivalen("IF101", "TI201", false)),
                "matakuliah ekivalen harus tampil sebagai \"utama (asli)\"");
        periksa("3 (4)".equals(
                NewUiUjianMahasiswaController.labelEkivalen("3", "4", false)),
                "SKS berbeda harus ikut menampilkan SKS aslinya");

        // Nilai kosong tidak boleh berubah menjadi kata "null" di layar.
        periksa("".equals(NewUiUjianMahasiswaController.labelEkivalen(null, null, true)),
                "kode kosong harus menjadi string kosong, bukan teks null");
        periksa("IF101 ()".equals(
                NewUiUjianMahasiswaController.labelEkivalen("IF101", null, false)),
                "asli kosong pada matakuliah berbeda tetap menghasilkan kurung kosong, bukan teks null");

        if (gagal > 0) {
            System.out.println("GAGAL NewUiUjianMahasiswa self-test: " + gagal + " masalah");
            System.exit(1);
        }
        System.out.println("PASS NewUiUjianMahasiswa self-test (label matakuliah ekivalen terjaga)");
    }

    private static void periksa(boolean syarat, String pesan) {
        if (!syarat) {
            gagal++;
            System.out.println("  - " + pesan);
        }
    }
}
