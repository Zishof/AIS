package ais.common.newui.akademik;

/** Self-test rentang, status, dan klasifikasi aksi KRS Non Paket. */
public final class NewUiKrsNonPaketSelfTest {
    private NewUiKrsNonPaketSelfTest() { }
    private static void check(boolean ok, String message) {
        if (!ok) throw new IllegalStateException(message);
    }
    public static void main(String[] args) {
        check(NewUiKrsNonPaketController.rentangValid(1, 1, 40), "satu semester ditolak");
        check(NewUiKrsNonPaketController.rentangValid(2, 8, 8), "rentang sah ditolak");
        check(!NewUiKrsNonPaketController.rentangValid(8, 2, 40), "rentang terbalik diterima");
        check(!NewUiKrsNonPaketController.rentangValid(1, 9, 8), "melewati batas diterima");
        check("Sebagian sudah disetujui".equals(
                NewUiKrsNonPaketController.status(true, true)), "status campuran keliru");
        check("Sudah disetujui semua".equals(
                NewUiKrsNonPaketController.status(true, false)), "status disetujui keliru");
        check("Belum disetujui semua".equals(
                NewUiKrsNonPaketController.status(false, true)), "status belum keliru");
        check(NewUiKrsNonPaketController.mengubah("update"), "update harus mengubah");
        check(NewUiKrsNonPaketController.mengubah("delete"), "delete harus mengubah");
        check(!NewUiKrsNonPaketController.mengubah("list"), "list tidak boleh mengubah");
        check(!NewUiKrsNonPaketController.aksiDikenal("ambil_matakuliah"),
                "aksi pengambilan setengah jadi tidak boleh dikenal");
        System.out.println("NewUiKrsNonPaketSelfTest OK");
    }
}
