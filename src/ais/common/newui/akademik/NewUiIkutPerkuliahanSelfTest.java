package ais.common.newui.akademik;

/**
 * Menjaga klasifikasi aksi pada {@link NewUiIkutPerkuliahanController}.
 *
 * <p>Layar ini satu-satunya di klaster akademik yang <b>mengubah data</b>:
 * mendaftarkan mahasiswa sebagai peserta tambahan, dan menghapus pendaftaran
 * itu. Kedua aksi tersebut wajib menuntut POST beserta token CSRF.</p>
 *
 * <p>Kekeliruan yang dijaga di sini bukan aksi yang tertolak — itu langsung
 * terlihat — melainkan sebaliknya: aksi pengubah yang <b>lolos sebagai
 * pembacaan</b>. Ia akan bekerja dengan baik pada pemakaian normal, dan yang
 * hilang hanya perlindungan CSRF-nya. Tidak ada gejala sampai seseorang
 * memanfaatkannya.</p>
 */
public final class NewUiIkutPerkuliahanSelfTest {

    private static int gagal = 0;

    public static void main(String[] args) {
        String[] pengubah = { "create", "delete" };
        for (int i = 0; i < pengubah.length; i++) {
            periksa(NewUiIkutPerkuliahanController.aksiDikenal(pengubah[i]),
                    "aksi '" + pengubah[i] + "' harus dikenal");
            periksa(NewUiIkutPerkuliahanController.mengubah(pengubah[i]),
                    "aksi '" + pengubah[i] + "' harus digolongkan mengubah data (POST + CSRF)");
        }

        String[] pembacaan = { "meta", "list", "lookup" };
        for (int i = 0; i < pembacaan.length; i++) {
            periksa(NewUiIkutPerkuliahanController.aksiDikenal(pembacaan[i]),
                    "aksi '" + pembacaan[i] + "' harus dikenal");
            periksa(!NewUiIkutPerkuliahanController.mengubah(pembacaan[i]),
                    "aksi '" + pembacaan[i] + "' tidak boleh digolongkan mengubah data");
        }

        // Nama sendiri seperti "ikuti"/"hapus" akan ditolak NewUiRouteGuard,
        // yang menolak kata kerja tak dikenalnya, sehingga layar tidak akan
        // berfungsi untuk siapa pun. Dijaga agar tidak kembali dipakai.
        periksa(!NewUiIkutPerkuliahanController.aksiDikenal("ikuti"),
                "kata kerja 'ikuti' tidak dikenal NewUiRouteGuard; pakai 'create'");
        periksa(!NewUiIkutPerkuliahanController.aksiDikenal("hapus"),
                "kata kerja 'hapus' tidak dikenal NewUiRouteGuard; pakai 'delete'");

        // Aksi di luar daftar ditolak, bukan diperlakukan sebagai pembacaan.
        periksa(!NewUiIkutPerkuliahanController.aksiDikenal("save"),
                "aksi tak terdaftar 'save' tidak boleh dikenal");
        periksa(!NewUiIkutPerkuliahanController.aksiDikenal("export"),
                "aksi tak terdaftar 'export' tidak boleh dikenal");
        periksa(!NewUiIkutPerkuliahanController.aksiDikenal(null),
                "aksi null tidak boleh dikenal");

        if (gagal > 0) {
            System.out.println("GAGAL NewUiIkutPerkuliahan self-test: " + gagal + " masalah");
            System.exit(1);
        }
        System.out.println("PASS NewUiIkutPerkuliahan self-test (aksi pengubah menuntut POST + CSRF)");
    }

    private static void periksa(boolean syarat, String pesan) {
        if (!syarat) {
            gagal++;
            System.out.println("  - " + pesan);
        }
    }
}
