package ais.common.newui.akademik;

/** Regression test ringan untuk aturan routing dan semester KRS Pilihan. */
public final class NewUiKrsPilihanSelfTest {
    private NewUiKrsPilihanSelfTest() { }

    public static void main(String[] args) {
        check(NewUiKrsPilihanController.aksiDikenal("meta"), "meta harus dikenal");
        check(NewUiKrsPilihanController.aksiDikenal("list"), "list harus dikenal");
        check(NewUiKrsPilihanController.mengubah("update"), "update harus menulis");
        check(NewUiKrsPilihanController.mengubah("delete"), "delete harus menulis");
        check(!NewUiKrsPilihanController.mengubah("list"), "list tidak boleh menulis");
        check(!NewUiKrsPilihanController.aksiDikenal("create"),
                "create tidak boleh membuka jalur tanpa gerbang KRS lama");

        int[] sama = NewUiKrsPilihanController.semesterBawaan(4, 4, 6, null);
        check(sama[0] == 6 && sama[1] == 6,
                "konfigurasi sama harus kembali ke semester berjalan");
        int[] rentang = NewUiKrsPilihanController.semesterBawaan(2, 6, 5, null);
        check(rentang[0] == 2 && rentang[1] == 6, "rentang konfigurasi berubah");
        int[] lulus = NewUiKrsPilihanController.semesterBawaan(2, 12, 10, Integer.valueOf(8));
        check(lulus[0] == 2 && lulus[1] == 8, "semester lulus tidak membatasi rentang");
        int[] terbalik = NewUiKrsPilihanController.semesterBawaan(8, 2, 5, null);
        check(terbalik[0] == 2 && terbalik[1] == 8, "rentang terbalik tidak dinormalkan");
        System.out.println("NewUiKrsPilihanSelfTest OK");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
