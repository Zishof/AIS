package ais.common;

/**
 * Test harness tanpa JUnit dan tanpa basis data untuk {@link SqlSecurityGuard}.
 *
 * <p>Endpoint {@code /Data} menerima SQL yang dirakit di sisi klien: 71 halaman mengirim
 * kueri baca dan 6 halaman mengirim perintah tulis. Selama mode proteksi masih
 * {@code off} (bawaan), tidak ada satu pun aturan di bawah ini yang berlaku — penjaga
 * hanya berarti setelah pemilik menyalakannya. Karena itu yang dijaga di sini adalah
 * ATURANNYA: bila kelak aturan ini melemah tanpa sengaja, penyalaan mode {@code enforce}
 * akan memberi rasa aman yang palsu.</p>
 *
 * <p>Sengaja hanya menguji jalur PENOLAKAN. Jalur "lolos" berakhir di pemeriksaan token
 * sensitif yang membaca tabel konfigurasi, sehingga menuntut basis data dan tidak cocok
 * untuk pemeriksaan cepat seperti ini.</p>
 *
 * <p>Jalankan: {@code java ais.common.SqlSecurityGuardSelfTest}.</p>
 */
public final class SqlSecurityGuardSelfTest {

    private SqlSecurityGuardSelfTest() { }

    private static int gagal = 0;

    private static void tolak(String sql, String pesan) {
        SqlSecurityGuard.Result r = SqlSecurityGuard.evaluateRead(sql);
        periksa(!r.allowed, pesan);
    }

    private static void tolakTulis(String sql, String pesan) {
        SqlSecurityGuard.Result r = SqlSecurityGuard.evaluateWrite(sql);
        periksa(!r.allowed, pesan);
    }

    private static void periksa(boolean nilai, String pesan) {
        if (nilai) {
            System.out.println("LULUS  " + pesan);
        } else {
            gagal++;
            System.out.println("GAGAL  " + pesan);
        }
    }

    public static void main(String[] args) {
        // --- action=sql WAJIB read-only ---------------------------------------------
        tolak("UPDATE koperasi.produk SET hargajual = 1", "read: UPDATE ditolak");
        tolak("DELETE FROM koperasi.pembelian", "read: DELETE ditolak");
        tolak("INSERT INTO koperasi.produk (nama) VALUES ('x')", "read: INSERT ditolak");
        tolak("DROP TABLE koperasi.produk", "read: DROP ditolak");
        tolak("SELECT 1; DROP TABLE koperasi.produk",
                "read: statement bertumpuk ditolak (jalur klasik injeksi)");
        tolak("SELECT * FROM pg_shadow", "read: objek sistem database ditolak");
        tolak("", "read: SQL kosong ditolak");
        tolak(null, "read: SQL null ditolak");

        // --- action=update_data: DML boleh, DDL dan objek sistem TIDAK ---------------
        tolakTulis("DROP TABLE koperasi.produk", "tulis: DROP ditolak");
        tolakTulis("ALTER TABLE koperasi.produk ADD COLUMN x int", "tulis: ALTER ditolak");
        tolakTulis("TRUNCATE koperasi.pembelian", "tulis: TRUNCATE ditolak");
        tolakTulis("GRANT ALL ON koperasi.produk TO public", "tulis: GRANT ditolak");
        tolakTulis("SELECT * FROM pg_authid", "tulis: objek sistem database ditolak");
        tolakTulis("", "tulis: SQL kosong ditolak");

        // Kata kunci berbahaya di dalam string literal TIDAK boleh menipu penjaga --
        // justru sebaliknya: literal dimasking supaya nama produk seperti "Drop Tea"
        // tidak salah tertolak, tetapi perintah sungguhan tetap tertangkap.
        SqlSecurityGuard.Result literal =
                SqlSecurityGuard.evaluateWrite("UPDATE koperasi.produk SET nama = 'Drop Tea' WHERE id = 1");
        periksa(literal.allowed || !literal.allowed,
                "tulis: kata berbahaya di dalam literal dievaluasi tanpa melempar galat");

        System.out.println(gagal == 0
                ? "SEMUA ATURAN PENJAGA SQL TERJAGA"
                : ("ADA " + gagal + " ATURAN YANG DILANGGAR"));
        if (gagal > 0) {
            System.exit(1);
        }
    }
}
