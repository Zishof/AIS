package ais.common.newui.akademik;

/**
 * Menjaga dua aturan {@link NewUiKrsPaketController} yang paling mudah tertukar
 * dengan layar akademik lain, dan klasifikasi aksinya.
 *
 * <h3>Rentang semester bukan 1..20</h3>
 * <p>Layar Kuesioner dan Ujian memakai 1..20. KRS Paket memakai
 * 1..{@code semesterLulus} bila sudah ada, dan 1..40 bila belum. Menyalin angka
 * dari layar tetangga akan menyembunyikan semester yang seharusnya dapat dilihat
 * mahasiswa lama — dan layarnya tetap terbuka dengan wajar, hanya pilihannya
 * yang lebih pendek.</p>
 *
 * <h3>Bawaan dibatasi semester lulus</h3>
 * <p>Bila semester berjalan sudah melewati semester lulus, yang dipilih adalah
 * semester lulus. Tanpa pembatasan itu, mahasiswa yang sudah lulus akan
 * dibukakan semester yang tidak pernah ia tempuh.</p>
 */
public final class NewUiKrsPaketSelfTest {

    private static int gagal = 0;

    public static void main(String[] args) {
        // --- batas pilihan semester -------------------------------------
        periksa(NewUiKrsPaketController.batasSemester(null)
                        == NewUiKrsPaketController.MAKS_SEMESTER_TANPA_LULUS,
                "tanpa semester lulus, batasnya 40");
        periksa(NewUiKrsPaketController.batasSemester(Integer.valueOf(0))
                        == NewUiKrsPaketController.MAKS_SEMESTER_TANPA_LULUS,
                "semester lulus nol diperlakukan seperti belum ada");
        periksa(NewUiKrsPaketController.batasSemester(Integer.valueOf(8)) == 8,
                "dengan semester lulus 8, batasnya 8");
        periksa(NewUiKrsPaketController.batasSemester(Integer.valueOf(14)) != 20,
                "batas tidak boleh terpaku 20 seperti layar Kuesioner dan Ujian");

        // --- semester bawaan --------------------------------------------
        periksa(NewUiKrsPaketController.semesterBawaan(null, 5) == 5,
                "tanpa semester lulus, bawaannya semester berjalan");
        periksa(NewUiKrsPaketController.semesterBawaan(Integer.valueOf(8), 5) == 5,
                "semester berjalan di bawah semester lulus dipakai apa adanya");
        periksa(NewUiKrsPaketController.semesterBawaan(Integer.valueOf(8), 9) == 8,
                "semester berjalan yang melewati semester lulus dibatasi ke semester lulus");
        periksa(NewUiKrsPaketController.semesterBawaan(Integer.valueOf(8), 8) == 8,
                "sama dengan semester lulus tetap dipakai");

        // --- klasifikasi aksi -------------------------------------------
        periksa(NewUiKrsPaketController.mengubah("update"),
                "update menulis sinkronisasi KRS, jadi harus menuntut POST + CSRF");
        periksa(NewUiKrsPaketController.mengubah("delete"),
                "delete harus menuntut POST + CSRF");
        periksa(!NewUiKrsPaketController.mengubah("list"),
                "list tidak menulis, jadi tidak boleh digolongkan mengubah data");
        periksa(!NewUiKrsPaketController.mengubah("meta"),
                "meta tidak boleh digolongkan mengubah data");

        // Evaluasi gerbang hanya membaca keadaan; menggolongkannya sebagai
        // pengubah akan menuntut izin Update untuk sekadar melihat mengapa
        // seseorang belum boleh mengambil KRS.
        periksa(NewUiKrsPaketController.aksiDikenal("options"),
                "options harus dikenal sebagai evaluasi gerbang");
        periksa(!NewUiKrsPaketController.mengubah("options"),
                "options hanya membaca, jadi tidak boleh menuntut POST + CSRF");

        // Kata kerja sendiri akan ditolak NewUiRouteGuard yang fail-closed.
        periksa(!NewUiKrsPaketController.aksiDikenal("segarkan"),
                "kata kerja sendiri tidak dikenal penjaga; pakai 'update'");
        periksa(!NewUiKrsPaketController.aksiDikenal("ambil_paket"),
                "pengambilan paket belum dilayani kontrak ini dan tidak boleh tampak dikenal");
        periksa(!NewUiKrsPaketController.aksiDikenal(null),
                "aksi null tidak boleh dikenal");

        if (gagal > 0) {
            System.out.println("GAGAL NewUiKrsPaket self-test: " + gagal + " masalah");
            System.exit(1);
        }
        System.out.println("PASS NewUiKrsPaket self-test (rentang semester dan klasifikasi aksi terjaga)");
    }

    private static void periksa(boolean syarat, String pesan) {
        if (!syarat) {
            gagal++;
            System.out.println("  - " + pesan);
        }
    }
}
