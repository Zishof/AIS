package ais.common.newui.sekolah;

/**
 * Menjaga pemetaan mode ke kolom penyekat pada
 * {@link NewUiSiswaAsuhanController}.
 *
 * <p>Menukar kedua kolom itu adalah kekeliruan paling berbahaya pada layar ini:
 * halamannya tetap tampil, jumlah barisnya tetap masuk akal, dan yang muncul
 * adalah anak asuh guru lain. Tidak ada galat, tidak ada baris log. Karena itu
 * pemetaannya dijaga uji, bukan pembacaan ulang.</p>
 *
 * <p>Yang tidak diuji di sini adalah kuerinya sendiri: itu menuntut basis data.
 * Yang diuji adalah keputusan yang salahnya paling tidak terlihat.</p>
 */
public final class NewUiSiswaAsuhanSelfTest {

    private static int gagal = 0;

    public static void main(String[] args) {
        // Guru BK menyekat pada guruBk; Guru Wali pada guruPembina. Label
        // "Guru Wali" pada layar lama memang merujuk kolom guruPembina --
        // dibaca dari siswa.zul dan SiswaAction.initCriteria.
        periksa("kelasSiswa.guruBk".equals(
                NewUiSiswaAsuhanController.propertiPenyekat(
                        NewUiSiswaAsuhanController.MODE_BK)),
                "mode bk harus menyekat pada kelasSiswa.guruBk");
        periksa("kelasSiswa.guruPembina".equals(
                NewUiSiswaAsuhanController.propertiPenyekat(
                        NewUiSiswaAsuhanController.MODE_WALI)),
                "mode wali harus menyekat pada kelasSiswa.guruPembina");

        // Keduanya tidak boleh sama: kalau sama, salah satu layar menampilkan
        // asuhan yang bukan miliknya.
        periksa(!NewUiSiswaAsuhanController.propertiPenyekat(NewUiSiswaAsuhanController.MODE_BK)
                        .equals(NewUiSiswaAsuhanController.propertiPenyekat(
                                NewUiSiswaAsuhanController.MODE_WALI)),
                "kedua mode tidak boleh memakai kolom penyekat yang sama");

        // Mode tak dikenal harus ditolak, bukan jatuh ke salah satu kolom.
        // Nilai bawaan di sini berarti menebak kolom penyekat.
        boolean ditolak = false;
        try {
            NewUiSiswaAsuhanController.propertiPenyekat("entah");
        } catch (IllegalArgumentException diharapkan) {
            ditolak = true;
        }
        periksa(ditolak, "mode tak dikenal harus ditolak, bukan memakai kolom bawaan");

        boolean ditolakNull = false;
        try {
            NewUiSiswaAsuhanController.propertiPenyekat(null);
        } catch (IllegalArgumentException diharapkan) {
            ditolakNull = true;
        }
        periksa(ditolakNull, "mode null harus ditolak");

        if (gagal > 0) {
            System.out.println("GAGAL NewUiSiswaAsuhan self-test: " + gagal + " masalah");
            System.exit(1);
        }
        System.out.println("PASS NewUiSiswaAsuhan self-test (pemetaan kolom penyekat terjaga)");
    }

    private static void periksa(boolean syarat, String pesan) {
        if (!syarat) {
            gagal++;
            System.out.println("  - " + pesan);
        }
    }
}
