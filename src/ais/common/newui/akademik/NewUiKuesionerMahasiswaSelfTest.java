package ais.common.newui.akademik;

import ais.database.model.Perkuliahan;

/**
 * Menjaga penentuan Ganjil/Genap pada
 * {@link NewUiKuesionerMahasiswaController}.
 *
 * <p>Layar lama menentukan jenis semester dari keganjilan nomornya:
 * {@code semester % 2 == 0 ? GENAP : GANJIL}. Aturan itu mudah terbalik ketika
 * disalin, dan terbaliknya tidak menimbulkan gejala apa pun — daftarnya tetap
 * terisi, jumlah barisnya tetap sama, hanya labelnya yang keliru pada setiap
 * baris. Mahasiswa yang membacanya akan mengira angket semester ganjil adalah
 * milik semester genap.</p>
 *
 * <p>Yang tidak diuji di sini adalah penyusunan daftar semesternya sendiri:
 * itu memanggil {@code Common.generateSemestersForGrid}, yang menuntut basis
 * data. Yang diuji adalah keputusan yang salahnya paling tidak terlihat.</p>
 */
public final class NewUiKuesionerMahasiswaSelfTest {

    private static int gagal = 0;

    public static void main(String[] args) {
        // Semester ganjil bernomor ganjil; genap bernomor genap.
        periksa(Perkuliahan.GANJIL.equals(NewUiKuesionerMahasiswaController.jenisSemester(1)),
                "semester 1 harus GANJIL");
        periksa(Perkuliahan.GENAP.equals(NewUiKuesionerMahasiswaController.jenisSemester(2)),
                "semester 2 harus GENAP");
        periksa(Perkuliahan.GANJIL.equals(NewUiKuesionerMahasiswaController.jenisSemester(7)),
                "semester 7 harus GANJIL");
        periksa(Perkuliahan.GENAP.equals(NewUiKuesionerMahasiswaController.jenisSemester(8)),
                "semester 8 harus GENAP");

        // Kalau keduanya sama, seluruh label kehilangan artinya.
        periksa(!Perkuliahan.GANJIL.equals(Perkuliahan.GENAP),
                "konstanta GANJIL dan GENAP tidak boleh bernilai sama");

        if (gagal > 0) {
            System.out.println("GAGAL NewUiKuesionerMahasiswa self-test: " + gagal + " masalah");
            System.exit(1);
        }
        System.out.println("PASS NewUiKuesionerMahasiswa self-test (Ganjil/Genap sesuai keganjilan nomor)");
    }

    private static void periksa(boolean syarat, String pesan) {
        if (!syarat) {
            gagal++;
            System.out.println("  - " + pesan);
        }
    }
}
