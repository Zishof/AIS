package ais.common.newui.lainnya;

/**
 * Menjaga daftar tab dasbor kegiatan mahasiswa varian pemilik sesi.
 *
 * <p>Ada dua varian layar ZK dengan nama nyaris sama:
 * {@code DashboardKegiatanKemahasiswaan} (milik mahasiswa sendiri) dan
 * {@code DashboardKegiatanKemahasiswaanAdmin} (petugas memilih mahasiswanya di
 * dalam tiap layar). Daftar tab keduanya <b>berbeda</b>: varian pemilik sesi
 * punya "Form Kegiatan" yang tidak ada pada varian Admin, dan varian Admin
 * punya "Dasbor" yang tidak ada di sini.</p>
 *
 * <p>Memakai ulang daftar tab Admin akan menghilangkan satu tab tanpa gejala
 * apa pun: halamannya tetap terbuka, tab lain tetap benar, dan yang hilang
 * hanya diketahui orang yang memang mencarinya. Karena itu perbedaan kedua
 * daftar dijaga uji, bukan komentar.</p>
 */
public final class NewUiDasborKemahasiswaanSayaSelfTest {

    private static int gagal = 0;

    public static void main(String[] args) {
        String[] wajibAda = {
            "Kegiatan Kemahasiswaan", "Organisasi", "Prestasi",
            "Karya", "Form Kegiatan", "Catatan Mahasiswa",
        };
        String[] label = NewUiLayarLainnyaController.labelTabKemahasiswaanSaya();

        periksa(label.length == wajibAda.length,
                "jumlah tab harus " + wajibAda.length + ", bukan " + label.length);
        for (int i = 0; i < wajibAda.length && i < label.length; i++) {
            periksa(wajibAda[i].equals(label[i]),
                    "tab ke-" + i + " harus \"" + wajibAda[i] + "\", bukan \"" + label[i] + "\"");
        }

        // Yang paling mudah hilang saat seseorang menyamakan kedua varian.
        boolean adaForm = false;
        for (int i = 0; i < label.length; i++) {
            if ("Form Kegiatan".equals(label[i])) adaForm = true;
        }
        periksa(adaForm, "tab \"Form Kegiatan\" hilang; tab ini hanya ada pada varian pemilik sesi");

        // Varian Admin punya tab "Dasbor"; varian ini tidak. Kalau muncul di
        // sini, berarti daftar Admin yang terpakai.
        boolean adaDasbor = false;
        for (int i = 0; i < label.length; i++) {
            if ("Dasbor".equals(label[i])) adaDasbor = true;
        }
        periksa(!adaDasbor, "tab \"Dasbor\" milik varian Admin tidak boleh muncul di sini");

        periksa(NewUiLayarLainnyaController.modeDikenal(
                        NewUiLayarLainnyaController.MODE_DASBOR_KEMAHASISWAAN_SAYA),
                "mode dasbor kemahasiswaan saya harus dikenal handle()");
        periksa("dashboard".equals(NewUiLayarLainnyaController.modul(
                        NewUiLayarLainnyaController.MODE_DASBOR_KEMAHASISWAAN_SAYA)),
                "modul mode ini harus dashboard agar penjaga hak akses memeriksa route yang benar");

        if (gagal > 0) {
            System.out.println("GAGAL NewUiDasborKemahasiswaanSaya self-test: " + gagal + " masalah");
            System.exit(1);
        }
        System.out.println("PASS NewUiDasborKemahasiswaanSaya self-test ("
                + label.length + " tab, berbeda dari varian Admin)");
    }

    private static void periksa(boolean syarat, String pesan) {
        if (!syarat) {
            gagal++;
            System.out.println("  - " + pesan);
        }
    }
}
