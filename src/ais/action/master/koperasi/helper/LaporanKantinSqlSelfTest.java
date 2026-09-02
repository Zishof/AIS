package ais.action.master.koperasi.helper;

/**
 * Test harness tanpa JUnit dan tanpa basis data untuk aturan SQL laporan kantin.
 *
 * <p>Yang dijaga di sini bukan "kuerinya berjalan" — itu sudah dibuktikan harness ber-database
 * saat perubahan dibuat — melainkan lima keputusan yang mudah rusak diam-diam saat seseorang
 * menyunting SQL laporan di kemudian hari. Setiap keputusan pernah menjadi cacat nyata
 * (dok. 65, dok. 67):</p>
 *
 * <ol>
 *   <li><b>Nilai penjualan diambil dari kolom final</b>, bukan dihitung ulang. Hitung ulang
 *       menyimpang dari nota pada penjualan berharga grosir atau Pack.</li>
 *   <li><b>Identitas kasir dari snapshot nota</b>, bukan {@code h.oleh}. Kolom itu metadata
 *       audit dan dapat berisi penanda sistem, sehingga penjualan bisa dikelompokkan ke
 *       "kasir" yang tidak pernah melayani transaksinya.</li>
 *   <li><b>Label produk jatuh ke snapshot baris penjualan</b>, supaya penjualan produk yang
 *       sudah dihapus tidak lenyap dari laporan bersama nilainya.</li>
 *   <li><b>Periode item penjualan selalu menyaring baris tidak aktif</b>, supaya angka laporan
 *       web sama dengan laporan di aplikasi kasir untuk periode yang sama.</li>
 *   <li><b>Ada batas jumlah baris</b>, supaya satu laporan bertahun-tahun tidak menghabiskan
 *       memori server dan menjatuhkan seluruh aplikasi.</li>
 * </ol>
 *
 * <p>Jalankan: {@code java ais.action.master.koperasi.helper.LaporanKantinSqlSelfTest}.
 * Keluar dengan kode 1 dan menyebut aturan yang dilanggar bila ada yang rusak.</p>
 */
public final class LaporanKantinSqlSelfTest {

    private LaporanKantinSqlSelfTest() { }

    private static int gagal = 0;

    private static void check(boolean nilai, String pesan) {
        if (nilai) {
            System.out.println("LULUS  " + pesan);
        } else {
            gagal++;
            System.out.println("GAGAL  " + pesan);
        }
    }

    public static void main(String[] args) {
        String omzet = LaporanKantinUtil.OMZET.toLowerCase();
        check(omzet.indexOf("coalesce(p.total") >= 0,
                "nilai penjualan memakai kolom final p.total (bukan hitung ulang harga x qty)");
        check(omzet.indexOf("p.hargasatuan") >= 0,
                "baris lama tanpa total tetap punya rumus cadangan (tidak menjadi nol)");

        String kasir = LaporanKantinUtil.KASIR_NOTA.toLowerCase();
        check(kasir.indexOf("kasir_login_nama") >= 0,
                "identitas kasir memakai snapshot nota kasir_login_nama");
        check(kasir.indexOf("h.oleh") < 0,
                "identitas kasir TIDAK memakai metadata audit h.oleh");

        String label = LaporanKantinUtil.LABEL_PRODUK_ITEM.toLowerCase();
        check(label.indexOf("p.nama") >= 0 && label.indexOf("p.kode") >= 0,
                "label produk jatuh ke snapshot baris penjualan bila master sudah dihapus");
        check(LaporanKantinUtil.KODE_PRODUK_ITEM.indexOf("p.kode") >= 0
                        && LaporanKantinUtil.NAMA_PRODUK_ITEM.indexOf("p.nama") >= 0,
                "kode dan nama produk pada laporan agregat juga punya cadangan snapshot");

        String satuan = LaporanKantinUtil.LABEL_SATUAN_JUAL.toLowerCase();
        check(satuan.indexOf("qty_input") >= 0 && satuan.indexOf("sj.nama") >= 0,
                "label satuan jual memakai qty_input dan nama satuan yang dipilih kasir");

        java.util.Map<String, Object> prm = new java.util.HashMap<String, Object>();
        String periode = LaporanKantinUtil.klausaPeriodeItemPenjualan("2026-01-01", "2026-01-31", prm)
                .toLowerCase();
        check(periode.indexOf("coalesce(p.aktif,true)=true") >= 0,
                "periode item penjualan selalu menyingkirkan baris yang tidak aktif");
        check(periode.indexOf("p.waktu") >= 0 && prm.containsKey("tglMulai")
                        && prm.containsKey("tglSampai"),
                "periode item penjualan tetap menyaring rentang tanggal dan mengisi parameternya");

        java.util.Map<String, Object> kosong = new java.util.HashMap<String, Object>();
        String tanpaTanggal = LaporanKantinUtil.klausaPeriodeItemPenjualan(null, null, kosong);
        check(tanpaTanggal.toLowerCase().indexOf("coalesce(p.aktif,true)=true") >= 0,
                "tanpa filter tanggal pun baris tidak aktif tetap disingkirkan");

        check(LaporanKantinUtil.BATAS_BARIS_LAPORAN > 0
                        && LaporanKantinUtil.BATAS_BARIS_LAPORAN <= 100000,
                "ada batas jumlah baris laporan yang masuk akal ("
                        + LaporanKantinUtil.BATAS_BARIS_LAPORAN + ")");

        System.out.println(gagal == 0
                ? "SEMUA ATURAN SQL LAPORAN TERJAGA"
                : ("ADA " + gagal + " ATURAN YANG DILANGGAR"));
        if (gagal > 0) {
            System.exit(1);
        }
    }
}
