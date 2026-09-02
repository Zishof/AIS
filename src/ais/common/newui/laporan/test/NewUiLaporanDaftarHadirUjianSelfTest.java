package ais.common.newui.laporan;

/** Self-test aturan template dan label laporan UTS/UAS. */
public final class NewUiLaporanDaftarHadirUjianSelfTest {
    private NewUiLaporanDaftarHadirUjianSelfTest() { }
    private static void check(boolean ok, String message) {
        if (!ok) throw new IllegalStateException(message);
    }
    public static void main(String[] args) {
        check("Daftar_Hadir_Ujian_UAS_0".equals(
                NewUiLaporanDaftarHadirUjianController.templateUas(0)), "template nol keliru");
        check("Daftar_Hadir_Ujian_UAS_9".equals(
                NewUiLaporanDaftarHadirUjianController.templateUas(9)), "template sembilan keliru");
        boolean ditolak = false;
        try { NewUiLaporanDaftarHadirUjianController.templateUas(10); }
        catch (IllegalArgumentException expected) { ditolak = true; }
        check(ditolak, "format ke-10 tidak ditolak");
        check(NewUiLaporanDaftarHadirUjianController.jenisSemester(3).contains("Ganjil"),
                "semester ganjil keliru");
        check(NewUiLaporanDaftarHadirUjianController.jenisSemester(4).contains("Genap"),
                "semester genap keliru");
        check("UTS\n25.0%".equals(NewUiLaporanDaftarHadirUjianController
                .kolom("UTS", Double.valueOf(25.0), true)), "bobot kolom hilang");
        check("UTS".equals(NewUiLaporanDaftarHadirUjianController
                .kolom("UTS", Double.valueOf(25.0), false)), "kolom tanpa bobot keliru");
        System.out.println("NewUiLaporanDaftarHadirUjianSelfTest OK");
    }
}
