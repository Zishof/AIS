package ais.action.master.generic.v2.test;

import java.lang.reflect.Method;

import ais.action.master.generic.v2.GenericCrudIdempotency;

/**
 * Verifikasi deduplikasi mutasi CRUD generik.
 *
 * <p>Tiga sifat yang menentukan aman-tidaknya jalur tulis local-first, dan
 * ketiganya diperiksa di sini tanpa basis data maupun kontainer servlet:</p>
 *
 * <ul>
 *   <li>Pengiriman ulang dengan id yang sama mengembalikan jawaban lama, bukan
 *       mengeksekusi mutasi kedua kalinya.</li>
 *   <li>Id yang sama milik pengguna berbeda TIDAK berbagi catatan. Id dibuat
 *       perangkat, jadi tanpa pemilik pada kunci, id yang bocor atau ditebak
 *       dapat memanen jawaban mutasi orang lain.</li>
 *   <li>Permintaan tanpa {@code clientMutationId} tidak dicatat sama sekali,
 *       supaya layar biasa tidak berubah perilakunya dan penyimpanan tidak
 *       terisi oleh permintaan yang memang tidak perlu dideduplikasi.</li>
 * </ul>
 */
public final class GenericCrudIdempotencySelfTest {
    private GenericCrudIdempotencySelfTest() { }

    public static void main(String[] args) throws Exception {
        Method bersihkan = ambil("bersihkan");
        Method jumlah = ambil("jumlah");
        bersihkan.invoke(null, new Object[0]);

        String kunciAndi = "andi|m-1";
        String kunciBudi = "budi|m-1";

        check(GenericCrudIdempotency.hasilSebelumnya(kunciAndi) == null,
                "Kunci baru seharusnya belum punya jawaban tersimpan.");

        Object jawaban = "hasil-pertama";
        Object dikembalikan = GenericCrudIdempotency.simpan(kunciAndi, jawaban);
        check(dikembalikan == jawaban,
                "simpan() harus mengembalikan hasil yang sama agar dapat ditulis sebagai satu ekspresi.");
        check(GenericCrudIdempotency.hasilSebelumnya(kunciAndi) == jawaban,
                "Pengiriman ulang harus mendapat jawaban yang sudah tersimpan.");

        // Id yang sama, pemilik berbeda: tidak boleh saling melihat.
        check(GenericCrudIdempotency.hasilSebelumnya(kunciBudi) == null,
                "Id mutasi yang sama milik pengguna lain tidak boleh berbagi catatan.");

        // Tanpa clientMutationId, kunci null: tidak dicatat dan tidak dicari.
        int sebelum = ((Integer) jumlah.invoke(null, new Object[0])).intValue();
        Object lewat = GenericCrudIdempotency.simpan(null, "tidak-dicatat");
        check(lewat != null && "tidak-dicatat".equals(lewat),
                "simpan() dengan kunci null tetap mengembalikan hasilnya apa adanya.");
        check(GenericCrudIdempotency.hasilSebelumnya(null) == null,
                "Kunci null tidak pernah menghasilkan jawaban tersimpan.");
        int sesudah = ((Integer) jumlah.invoke(null, new Object[0])).intValue();
        check(sebelum == sesudah,
                "Permintaan tanpa clientMutationId tidak boleh menambah catatan.");

        bersihkan.invoke(null, new Object[0]);
        System.out.println("PASS Generic CRUD idempotency self-test");
    }

    private static Method ambil(String nama) throws Exception {
        Method m = GenericCrudIdempotency.class.getDeclaredMethod(nama, new Class[0]);
        m.setAccessible(true);
        return m;
    }

    private static void check(boolean value, String message) {
        if (!value) throw new IllegalStateException(message);
    }
}
