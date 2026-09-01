package ais.common.newui.koperasi.test;

import ais.common.newui.koperasi.NewUiKantinMemberController;

/**
 * Test harness tanpa JUnit untuk penyeragaman baris hasil SQL mentah.
 *
 * <p>Layar "Ringkasan Saya" menjawab HTTP 500 di demo. Sebabnya bukan kuerinya —
 * SQL-nya sama persis dengan layar ZK asalnya — melainkan bentuk hasilnya:
 * Hibernate memulangkan {@code Object[]} hanya untuk SELECT dengan lebih dari
 * satu kolom, sedangkan kueri total top-up hanya memilih satu kolom. Menugaskan
 * hasilnya ke {@code Object[]} melempar {@link ClassCastException}.</p>
 *
 * <p>Layar ZK asalnya punya cacat yang sama, tetapi {@code rows()}-nya menelan
 * exception dan memulangkan daftar kosong — layarnya menampilkan Rp 0 dan
 * kegagalannya tidak pernah terlihat. Uji ini menjaga agar perbaikannya tidak
 * hilang, dan sengaja tidak memerlukan basis data.</p>
 */
public final class NewUiKantinMemberSelfTest {

    private NewUiKantinMemberSelfTest() { }

    private static void check(boolean value, String message) {
        if (!value) throw new IllegalStateException(message);
    }

    public static void main(String[] args) {
        // Satu kolom: Hibernate memulangkan nilainya langsung.
        Object[] tunggal = NewUiKantinMemberController.barisTunggal(Double.valueOf(125000));
        check(tunggal != null && tunggal.length == 1,
                "hasil satu kolom harus dibungkus menjadi larik berisi satu unsur");
        check(Double.valueOf(125000).equals(tunggal[0]), "nilainya tidak boleh berubah");

        // Banyak kolom: sudah Object[], harus dibiarkan apa adanya.
        Object[] asli = new Object[] { Long.valueOf(3), Double.valueOf(9), Double.valueOf(1) };
        Object[] banyak = NewUiKantinMemberController.barisTunggal(asli);
        check(banyak == asli, "hasil banyak kolom tidak boleh disalin ulang");

        // Null tetap null; pemanggil sudah memeriksanya.
        check(NewUiKantinMemberController.barisTunggal(null) == null,
                "baris kosong harus tetap null");

        // Nilai teks pun harus terbungkus, bukan hanya angka.
        Object[] teks = NewUiKantinMemberController.barisTunggal("Koperasi Utama");
        check(teks.length == 1 && "Koperasi Utama".equals(teks[0]),
                "hasil satu kolom bertipe teks juga harus terbungkus");

        System.out.println("PASS NewUiKantinMember single-column row normalisation self-test");
    }
}
