package ais.action.master.inventory;

import java.util.List;

import org.hibernate.Session;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.inventory.Produk;

/**
 * Util bersama perhitungan ulang stok kantin — satu-satunya tempat rumus stok ditulis, dipakai oleh
 * modul Pengadaan, Stok Opname, penjualan POS (online maupun sinkronisasi offline), dan konsumsi
 * bahan baku resep.
 *
 * <h3>Rumus baku (7 suku, selalu sama di seluruh variasi method di bawah)</h3>
 * <pre>
 *   stok = &Sigma;pengadaan.qty + &Sigma;stok_opname.selisih &minus; &Sigma;pembelian.qty
 *          &minus; &Sigma;pemakaian_bahan_baku.qty + &Sigma;retur_penjualan.qty(kembali_ke_stok)
 *          + &Sigma;mutasi_stok_toko.qty(tujuan) &minus; &Sigma;mutasi_stok_toko.qty(asal)
 *          &minus; &Sigma;retur_pembelian.qty
 *          + &Sigma;mutasi_stok_produksi.qty_masuk &minus; &Sigma;mutasi_stok_produksi.qty_keluar
 * </pre>
 * <ul>
 *   <li><b>&Sigma;pengadaan.qty</b> — total barang masuk lewat modul Pengadaan (nota pembelian dari
 *       supplier ke gudang/toko).</li>
 *   <li><b>&Sigma;stok_opname.selisih</b> — koreksi manual hasil hitung fisik (bisa positif/negatif),
 *       satu-satunya cara mengoreksi stok tanpa harus lewat Pengadaan.</li>
 *   <li><b>&Sigma;pembelian.qty</b> — total unit yang benar-benar terjual ke pembeli (baris di
 *       {@code koperasi.pembelian}, dibuat oleh {@code KantinHelper.bayar()} atau
 *       {@code pos_offline_service.jsp} saat sinkron).</li>
 *   <li><b>&Sigma;pemakaian_bahan_baku.qty</b> — konsumsi otomatis bahan baku saat produk BER-RESEP
 *       terjual (lihat {@link BahanBakuUtil#konsumsiBahanBaku}); bahan baku itu sendiri adalah
 *       {@link ais.database.model.inventory.Produk} lain yang stoknya ikut berkurang di sini.</li>
 *   <li><b>&Sigma;retur_penjualan.qty</b> — barang kembali dari pelanggan, HANYA baris
 *       {@code kembalikan_ke_stok=true} (barang rusak/tak layak jual tidak menambah stok jual).</li>
 *   <li><b>&Sigma;mutasi_stok_toko.qty</b> — transfer stok antar outlet (Fase 3 roadmap F&amp;B):
 *       baris di mana produk ini jadi {@code produk_tujuan} MENAMBAH, baris di mana produk ini jadi
 *       {@code produk_asal} MENGURANGI (lihat JavaDoc {@link ais.database.model.inventory.MutasiStokToko}).</li>
 *   <li><b>&Sigma;retur_pembelian.qty</b> — barang dikembalikan KE SUPPLIER, SELALU mengurangi
 *       stok tanpa syarat (lihat JavaDoc {@link ais.database.model.inventory.ReturPembelian}).</li>
 *   <li><b>&Sigma;mutasi_stok_produksi</b> — pergerakan dari dokumen PRODUKSI (Fase 0 dok. 49):
 *       OUTPUT/RETURN mengisi {@code qty_masuk}, ISSUE/WASTE mengisi {@code qty_keluar}; baris
 *       REVERSE membalik keduanya. Ditulis {@code ProduksiApiHelper} saat dokumen POSTED/REVERSED
 *       (lihat JavaDoc {@link ais.database.model.inventory.MutasiStokProduksi}).</li>
 * </ul>
 * Suku pemakaian-bahan-baku inilah yang membuat rumus ini TIDAK BOLEH disederhanakan jadi
 * "stok = masuk - terjual" naif: satu transaksi penjualan produk jadi (mis. kopi) bisa mengurangi stok
 * BEBERAPA produk sekaligus (kopi bubuk, gula, gelas) lewat baris {@code pemakaian_bahan_baku} yang
 * terpisah dari baris {@code pembelian} milik produk jadi itu sendiri.
 *
 * <h3>Kenapa ada 3 variasi method yang menghitung rumus yang sama</h3>
 * Formula di atas dipanggil dari 3 konteks eksekusi yang berbeda kemampuan/batasannya, sehingga
 * masing-masing butuh cara akses data yang berbeda meski hasilnya harus identik:
 * <ol>
 *   <li>{@link #recomputeStokProduk(Long)} — konteks ZK/JSP dengan {@link Session} aktif via
 *       {@link HibernateUtil#currentSession()}; MEMUAT entity {@link Produk} penuh (bukan cuma
 *       UPDATE mentah) karena method ini JUGA menyesuaikan harga beli/jual dari pengadaan terakhir —
 *       butuh objek entity untuk itu, bukan sekadar kolom stok.</li>
 *   <li>{@link #recomputeStokProdukNative(Session, Long)} — dipanggil dari DALAM transaksi/alur lain
 *       yang session-nya sudah ada (mis. sinkronisasi BAST) dan tidak boleh membuka
 *       transaksi/query entity tambahan yang mengganggu urutan flush pemanggil; jadi murni satu
 *       {@code UPDATE} SQL native, tanpa memuat entity.</li>
 *   <li>{@link #recomputeStokProdukViaSql(Long)} — dipanggil dari konteks yang bahkan TIDAK dijamin
 *       punya {@link Session} Hibernate aktif sama sekali (mis. callback pembayaran online/VA di luar
 *       siklus request ZK/JSP biasa) — lewat {@link Common#updateSql(String)} yang mengelola
 *       koneksinya sendiri secara mandiri.</li>
 * </ol>
 * Method 2 dan 3 memakai formula {@code UPDATE} SQL yang PERSIS SAMA (lihat {@link #formulaStokSql},
 * satu-satunya tempat teksnya ditulis) — hanya mekanisme eksekusinya yang berbeda. Sebelumnya kedua
 * teks SQL itu diketik ulang secara terpisah di masing-masing method (identik karakter demi karakter);
 * kini disatukan lewat helper privat supaya perubahan rumus di masa depan cukup dilakukan sekali dan
 * tidak berisiko kedua salinan diam-diam berbeda (drift).
 *
 * <h3>Idempotensi &amp; keamanan pemanggilan berulang</h3>
 * Seluruh method di kelas ini AMAN dipanggil berkali-kali untuk produk yang sama — hasilnya selalu
 * dihitung ULANG dari rekam jejak transaksi (bukan diakumulasikan/di-increment), jadi memanggilnya
 * dua kali untuk produk yang sama tidak menggandakan efek apa pun, hanya membuang siklus CPU/DB.
 * Sifat ini sengaja dipertahankan karena banyak pemanggil (Pengadaan, Stok Opname, POS, sinkronisasi
 * offline) memicu recompute pada titik yang berbeda-beda dan tidak selalu bisa memastikan belum pernah
 * dipanggil sebelumnya untuk produk yang sama dalam satu request.
 */
public final class StokKantinUtil {

    private StokKantinUtil() {
    }

    /**
     * Menjalankan {@code sql} lewat session Hibernate aktif dan mengambil satu nilai numerik dari
     * hasilnya (baris/kolom pertama). Dipakai {@link #recomputeStokProduk(Long)} untuk membaca 5
     * agregat (masuk/opname/keluar/pakai/harga-terakhir) tanpa menulis boilerplate query berulang.
     *
     * @param sql pernyataan SQL native yang mengembalikan tepat satu nilai skalar (mis.
     *            {@code SELECT COALESCE(SUM(qty),0) FROM ...}); pemanggil bertanggung jawab
     *            memastikan {@code produkId} yang disisipkan ke teks SQL berasal dari nilai
     *            {@link Long} tervalidasi (bukan input pengguna mentah), karena method ini tidak
     *            memakai parameter terikat.
     * @return nilai numerik hasil query, atau {@code 0.0} bila hasilnya {@code NULL} ATAU query gagal
     *         (kegagalan dicatat ke {@link ais.common.ErrorAuditUtil} lalu dianggap 0 — gagal-aman
     *         supaya satu agregat yang gagal dibaca tidak menggagalkan seluruh recompute stok, hanya
     *         membuat komponen itu dianggap nihil untuk perhitungan kali ini).
     */
    public static double scalar(String sql) {
        try {
            Object o = HibernateUtil.currentSession().createSQLQuery(sql).uniqueResult();
            return o == null ? 0.0 : ((Number) o).doubleValue();
        } catch (Exception e) {
            ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/inventory/StokKantinUtil.java:scalar");
            return 0.0;
        }
    }

    /**
     * Menghitung ulang stok SATU produk dari rekam jejak pengadaan/opname/penjualan/pemakaian bahan
     * baku (rumus baku, lihat dokumentasi kelas), sekaligus menyesuaikan harga beli/jual dari
     * pengadaan terakhir bila ada.
     *
     * <p>Dipakai dari konteks yang punya {@link Session} Hibernate aktif via
     * {@link HibernateUtil#currentSession()} (ZK atau JSP di bawah {@code FilterJSP}) dan boleh
     * memuat entity penuh — dipilih dibanding varian {@code Native}/{@code ViaSql} justru KARENA
     * perlu memuat entity {@link Produk} (bukan sekadar UPDATE kolom stok), sebab method ini juga
     * menyesuaikan harga beli/jual yang butuh nilai lama untuk dibandingkan.</p>
     *
     * <p>Penyesuaian harga: harga beli produk selalu diperbarui ke {@code hargabelisatuan} dari
     * pengadaan TERBARU (bila ada minimal satu baris pengadaan); harga jual HANYA dinaikkan
     * (never diturunkan otomatis) bila harga beli terbaru itu sudah menyamai/melebihi harga jual
     * saat ini — mencegah toko menjual di bawah modal tanpa perlu petugas mengubah harga jual manual
     * setiap kali harga pengadaan naik, tapi tidak pernah memaksa turun harga jual yang sengaja
     * ditetapkan admin di atas harga beli (margin) hanya karena pengadaan baru lebih murah.</p>
     *
     * @param produkId id produk yang direcompute; {@code null} tidak melakukan apa pun (bukan error).
     * @throws Exception merambat apa adanya dari kegagalan memuat/menyimpan entity (mis. produk sudah
     *         terhapus) — TIDAK ditangkap di sini secara sengaja, karena pemanggil (mis.
     *         {@code KantinHelper.bayar()}) sudah membungkus pemanggilan ini dalam blok
     *         try/catch fail-safe tersendiri per produk, sehingga satu produk gagal direcompute tidak
     *         boleh menggagalkan seluruh transaksi penjualan yang sedang berjalan.
     */
    public static void recomputeStokProduk(Long produkId) throws Exception {
        if (produkId == null) {
            return;
        }
        double masuk = scalar("SELECT COALESCE(SUM(qty),0) FROM koperasi.pengadaan_produk WHERE produk = " + produkId);
        double opname = scalar("SELECT COALESCE(SUM(selisih),0) FROM koperasi.stok_opname WHERE produk = " + produkId);
        double keluar = scalar("SELECT COALESCE(SUM(qty),0) FROM koperasi.pembelian WHERE produk = " + produkId);
        double pakai = scalar(
                "SELECT COALESCE(SUM(qty),0) FROM koperasi.pemakaian_bahan_baku WHERE produk = " + produkId);
        // Retur Penjualan (barang kembali dari pelanggan): HANYA baris yg ditandai kembali_ke_stok=true
        // yg menambah stok -- barang rusak/tak layak jual TIDAK otomatis balik ke stok jual (lihat
        // JavaDoc ReturPenjualan.getKembalikanKeStok()).
        double returPenjualan = scalar("SELECT COALESCE(SUM(qty),0) FROM koperasi.retur_penjualan WHERE produk = "
                + produkId + " AND kembalikan_ke_stok = true");
        // Mutasi Stok Antar Outlet (Fase 3): baris ini toko TUJUAN (stok bertambah) minus baris ini
        // toko ASAL (stok berkurang) -- lihat JavaDoc MutasiStokToko.
        double mutasiMasuk = scalar(
                "SELECT COALESCE(SUM(qty),0) FROM koperasi.mutasi_stok_toko WHERE produk_tujuan = " + produkId);
        double mutasiKeluar = scalar(
                "SELECT COALESCE(SUM(qty),0) FROM koperasi.mutasi_stok_toko WHERE produk_asal = " + produkId);
        // Retur Pembelian (barang kembali ke supplier): SELALU mengurangi stok, tanpa syarat --
        // lihat JavaDoc ReturPembelian.
        double returPembelian = scalar(
                "SELECT COALESCE(SUM(qty),0) FROM koperasi.retur_pembelian WHERE produk = " + produkId);
        // Produksi (Fase 0 dok. 49): OUTPUT/RETURN masuk, ISSUE/WASTE keluar; baris REVERSE sudah
        // membalik kolomnya sendiri sehingga cukup dijumlahkan apa adanya.
        double produksi = scalar(
                "SELECT COALESCE(SUM(COALESCE(qty_masuk,0) - COALESCE(qty_keluar,0)),0)"
                        + " FROM koperasi.mutasi_stok_produksi WHERE produk = " + produkId);
        double latest = scalar("SELECT hargabelisatuan FROM koperasi.pengadaan_produk WHERE produk = " + produkId
                + " ORDER BY waktupengadaan DESC, id DESC LIMIT 1");

        Session s = HibernateUtil.currentSession();
        Produk p = (Produk) s.load(Produk.class, produkId);
        p.setStok(masuk + opname - keluar - pakai + returPenjualan + mutasiMasuk - mutasiKeluar - returPembelian
                + produksi);
        if (latest > 0) {
            p.setHargaBeli(latest);
            double hargaJual = p.getHargaJual() == null ? 0 : p.getHargaJual();
            if (latest >= hargaJual) {
                p.setHargaJual(latest);
            }
        }
        Common.refreshSaveOrUpdate(s, p);
    }

    /**
     * Teks rumus 4-suku (tanpa {@code UPDATE ... SET stok = (}/{@code )} pembungkusnya) yang dipakai
     * identik oleh {@link #recomputeStokProdukNative(Session, Long)} dan
     * {@link #recomputeStokProdukViaSql(Long)} — satu-satunya tempat teks SQL rumus ini ditulis untuk
     * kedua varian, supaya keduanya dijamin selalu sinkron tanpa perlu diingat-ingat manual saat
     * rumus berubah di masa depan.
     */
    /**
     * Ekspresi SQL stok kanonik untuk satu produk. Diekspos agar validasi checkout dapat membaca
     * saldo live dengan rumus yang persis sama seperti proses recompute/monitor; jangan menyalin
     * sebagian sukunya ke query lain karena akan membuat monitor dan pembayaran berbeda.
     */
    public static String formulaStokSql(Long produkId) {
        return "COALESCE((SELECT SUM(qty) FROM koperasi.pengadaan_produk WHERE produk = " + produkId + "),0)"
                + " + COALESCE((SELECT SUM(selisih) FROM koperasi.stok_opname WHERE produk = " + produkId + "),0)"
                + " - COALESCE((SELECT SUM(qty) FROM koperasi.pembelian WHERE produk = " + produkId + "),0)"
                + " - COALESCE((SELECT SUM(qty) FROM koperasi.pemakaian_bahan_baku WHERE produk = " + produkId + "),0)"
                // Retur Penjualan: hanya baris kembalikan_ke_stok=true (barang rusak/tak layak jual
                // TIDAK menambah stok jual) -- lihat catatan sama di recomputeStokProduk(Long).
                + " + COALESCE((SELECT SUM(qty) FROM koperasi.retur_penjualan WHERE produk = " + produkId
                + " AND kembalikan_ke_stok = true),0)"
                // Mutasi Stok Antar Outlet (Fase 3): baris toko TUJUAN menambah, baris toko ASAL
                // mengurangi -- lihat catatan sama di recomputeStokProduk(Long)/JavaDoc MutasiStokToko.
                + " + COALESCE((SELECT SUM(qty) FROM koperasi.mutasi_stok_toko WHERE produk_tujuan = " + produkId + "),0)"
                + " - COALESCE((SELECT SUM(qty) FROM koperasi.mutasi_stok_toko WHERE produk_asal = " + produkId + "),0)"
                // Retur Pembelian: SELALU mengurangi stok tanpa syarat -- lihat catatan sama di
                // recomputeStokProduk(Long)/JavaDoc ReturPembelian.
                + " - COALESCE((SELECT SUM(qty) FROM koperasi.retur_pembelian WHERE produk = " + produkId + "),0)"
                // Produksi (Fase 0 dok. 49): masuk-keluar dijumlahkan langsung; baris REVERSE sudah
                // membalik kolomnya sendiri -- lihat JavaDoc MutasiStokProduksi.
                + " + COALESCE((SELECT SUM(COALESCE(qty_masuk,0) - COALESCE(qty_keluar,0))"
                + " FROM koperasi.mutasi_stok_produksi WHERE produk = " + produkId + "),0)";
    }

    /**
     * Hitung ulang stok satu produk lewat satu native UPDATE pada {@code session} yang diberikan —
     * TANPA membuka/menutup transaksi sendiri. Dipakai ketika berada di dalam alur/transaksi lain
     * (mis. sinkronisasi BAST {@link BahanBakuUtil}/KantinAssetSyncUtil) agar tidak mengganggu transaksi
     * pemanggil. Rumus stok sama dengan {@link #recomputeStokProduk(Long)} (kecuali tidak menyetel
     * harga beli dari pengadaan terakhir). Gagal-aman.
     */
    public static void recomputeStokProdukNative(Session session, Long produkId) {
        if (session == null || produkId == null) {
            return;
        }
        try {
            session.createSQLQuery("UPDATE koperasi.produk SET stok = (" + formulaStokSql(produkId) + ") WHERE id = "
                    + produkId).executeUpdate();
        } catch (Exception e) {
            e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/inventory/StokKantinUtil.java:recomputeStokProdukNative");
        }
    }

    /**
     * Hitung ulang stok produk lewat UPDATE SQL native TANPA perlu sesi Hibernate aktif.
     *
     * <p>Varian dari {@link #recomputeStokProdukNative(org.hibernate.Session, Long)} untuk konteks
     * yang memakai pola {@link Common#updateSql(String)} — mis. callback pembayaran online/VA — di mana
     * {@code currentSession()} tidak dijamin tersedia. Formula stok IDENTIK (kanonik 4-suku):
     * pengadaan + Σselisih opname − pembelian − pemakaian bahan baku. Aman dipanggil berkali-kali
     * (idempoten) dan kegagalan tidak dilempar (hanya dicetak) agar tidak mengganggu alur pemanggil.</p>
     *
     * @param produkId id produk; {@code null} tidak melakukan apa pun.
     */
    public static void recomputeStokProdukViaSql(Long produkId) {
        if (produkId == null) {
            return;
        }
        try {
            Common.updateSql("UPDATE koperasi.produk SET stok = (" + formulaStokSql(produkId) + ") WHERE id = "
                    + produkId);
        } catch (Exception e) {
            e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/inventory/StokKantinUtil.java:recomputeStokProdukViaSql");
        }
    }

    /**
     * Menghitung ulang stok SEMUA produk milik satu toko sekaligus (atau seluruh produk di seluruh
     * toko bila {@code tokoId} null) — pembungkus batch di atas {@link #recomputeStokProduk(Long)}.
     *
     * <p>Dipakai untuk pemulihan massal setelah migrasi data, atau saat admin mencurigai stok satu
     * toko sudah tidak sinkron dan ingin "kalkulasi ulang semua" tanpa memilih produk satu-satu.
     * Setiap produk dihitung ULANG independen (bukan agregat lintas-produk), jadi aman dijalankan
     * berkali-kali dan aman bila daftar produk berubah di tengah proses (produk baru yang ditambahkan
     * setelah query id diambil tidak ikut recompute di panggilan ini — jalankan ulang bila perlu).</p>
     *
     * @param tokoId id toko yang produknya direcompute; {@code null} berarti SELURUH produk di
     *               SELURUH toko (operasi berat, pakai dengan sadar — bukan untuk dipanggil rutin
     *               per request, cocoknya dari tugas admin/maintenance sekali jalan).
     * @throws Exception merambat dari kegagalan query daftar id ATAU dari {@link #recomputeStokProduk}
     *         untuk produk mana pun — TIDAK fail-safe per-produk di sini (beda dari
     *         {@link #recomputeStokProdukNative}/{@link #recomputeStokProdukViaSql} yang menelan
     *         error), karena pemanggilnya adalah proses batch/admin yang justru INGIN tahu begitu ada
     *         produk yang gagal, bukan diam-diam melewatkannya.
     */
    public static void recomputeStokToko(Long tokoId) throws Exception {
        String sql = "SELECT id FROM koperasi.produk" + (tokoId == null ? "" : " WHERE toko = " + tokoId);
        List<?> ids = HibernateUtil.currentSession().createSQLQuery(sql).list();
        if (ids != null) {
            for (Object o : ids) {
                if (o != null) {
                    recomputeStokProduk(((Number) o).longValue());
                }
            }
        }
    }
}
