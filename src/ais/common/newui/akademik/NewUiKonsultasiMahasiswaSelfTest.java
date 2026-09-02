package ais.common.newui.akademik;

import ais.action.master.TampilanELearningAction;

/** Regression test klasifikasi aksi dan mode hub konsultasi mahasiswa. */
public final class NewUiKonsultasiMahasiswaSelfTest {
    private NewUiKonsultasiMahasiswaSelfTest() { }

    public static void main(String[] args) {
        check(NewUiKonsultasiMahasiswaController.aksiDikenal("meta"), "meta ditolak");
        check(NewUiKonsultasiMahasiswaController.aksiDikenal("list"), "list ditolak");
        check(NewUiKonsultasiMahasiswaController.mengubah("update"), "update harus menulis");
        check(!NewUiKonsultasiMahasiswaController.mengubah("list"), "list tidak boleh menulis");
        check(!NewUiKonsultasiMahasiswaController.aksiDikenal("delete"), "delete liar diterima");

        check(NewUiKonsultasiMahasiswaController.jenisDikenal("akademik"), "akademik ditolak");
        check(NewUiKonsultasiMahasiswaController.jenisDikenal("kkn"), "KKN ditolak");
        check(NewUiKonsultasiMahasiswaController.jenisDikenal("pkl"), "PKL ditolak");
        check(NewUiKonsultasiMahasiswaController.jenisDikenal("bimbingan"), "bimbingan ditolak");
        check(NewUiKonsultasiMahasiswaController.jenisDikenal("penguji"), "penguji ditolak");
        check(NewUiKonsultasiMahasiswaController.jenisDikenal("lain"), "lain ditolak");
        check(!NewUiKonsultasiMahasiswaController.jenisDikenal("semua"), "jenis liar diterima");

        check(NewUiKonsultasiMahasiswaController.mode("kkn") == TampilanELearningAction.KKN,
                "mode KKN berubah");
        check(NewUiKonsultasiMahasiswaController.mode("pkl") == TampilanELearningAction.PKL,
                "mode PKL berubah");
        check(NewUiKonsultasiMahasiswaController.mode("bimbingan")
                == TampilanELearningAction.BIMBINGAN, "mode bimbingan berubah");
        check(NewUiKonsultasiMahasiswaController.mode("penguji")
                == TampilanELearningAction.SKRIPSI, "mode penguji berubah");
        check(NewUiKonsultasiMahasiswaController.mode("lain")
                == TampilanELearningAction.KONSULTASI, "mode konsultasi umum berubah");
        System.out.println("NewUiKonsultasiMahasiswaSelfTest OK");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
