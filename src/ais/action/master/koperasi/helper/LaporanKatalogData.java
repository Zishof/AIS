package ais.action.master.koperasi.helper;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import ais.common.Common;

/**
 * <h2>LaporanKatalogData — katalog "Laporan-Laporan e-Kantin" sebagai data Java.</h2>
 *
 * <p>
 * Satu-satunya alasan class ini ada: {@code webapp/WEB-INF/baru/modul/kantin/laporan_laporan.jsp}
 * (versi web) menyimpan katalog ~150 laporan (32 kategori) sebagai array JavaScript INLINE di
 * halaman itu sendiri — cara itu sempurna untuk JSP (server-rendered sekali per halaman), tapi
 * TIDAK bisa dipakai ulang oleh aplikasi Desktop Electron ({@code desktop-pos-electron}, murni
 * HTML+JS statis tanpa JSP) yang butuh katalog yang SAMA lewat panggilan API JSON
 * ({@code PosApi} action {@code laporan_katalog}). Class ini adalah PORTING manual dari array
 * {@code REPORTS} di JSP tsb ke Java, dibungkus jadi {@link #katalog()} yang mengembalikan JSON
 * identik strukturnya.
 * </p>
 *
 * <p>
 * <b>PENTING — risiko drift:</b> file ini SENGAJA terpisah dari {@code laporan_laporan.jsp} (bukan
 * satu sumber kebenaran tunggal) supaya perubahan pada katalog Desktop tidak berisiko merusak
 * halaman web yang sudah berjalan. Konsekuensinya: bila katalog laporan di
 * {@code laporan_laporan.jsp} diubah (laporan baru ditambah/dihapus/judul diganti), berkas ini
 * WAJIB disinkronkan manual mengikuti. Data laporan (kolom/baris/perhitungan) TIDAK diduplikasi di
 * sini sama sekali — hanya metadata katalog (id/judul/keterangan/kategori/filter yang dibutuhkan);
 * eksekusi laporan sungguhan tetap 100% lewat {@link LaporanKantinUtil#build} yang sama dipakai versi
 * web, jadi tidak ada risiko HASIL laporan berbeda antar platform, hanya risiko DAFTAR katalog basi.
 * </p>
 *
 * <p>
 * Judul &amp; keterangan tiap laporan dilewatkan lewat {@link Common#getBahasaConfig(String)} agar
 * ikut diterjemahkan sesuai bahasa sesi aktif (Fitur alih bahasa Desktop) — dengan efek samping yang
 * SAMA seperti pemakaian {@code Common.getBahasaConfig} lain di seluruh aplikasi (kunci baru
 * otomatis tersimpan ke tabel {@code LabelBahasa} saat pertama dilihat).
 * </p>
 */
public final class LaporanKatalogData {

    private LaporanKatalogData() {
    }

    /** URL peluncur laporan Akuntansi resmi (ZK) — Satuan Kerja kantin dipra-pilih di dalam form. */
    private static String launchZk(String lap) {
        return Common.ROOT + "/pages/master/kantin/laporan_keuangan.zul?lap=" + lap;
    }

    private static String dashAkun() {
        return Common.ROOT + "/common/display.zul?p=akuntansi";
    }

    private static JSONObject item(String id, String judul, String ket) throws Exception {
        return item(id, judul, ket, false, false, false, null);
    }

    private static JSONObject item(String id, String judul, String ket, boolean produk, boolean pelanggan,
            boolean perToko, String url) throws Exception {
        return item(id, judul, ket, produk, pelanggan, perToko, url, false);
    }

    /**
     * Varian dengan flag {@code stokPerTanggal}: memberi tahu SEMUA penyaji (JSP, ZK, PDF, dan API
     * POS Desktop/Android) bahwa laporan ini memakai panel filter stok -- "Per Tanggal" (acuan
     * saldo), Kategori/Jenis Barang, Grup Produk, Hanya Aktif, dan Sembunyikan Stok Nol.
     */
    private static JSONObject item(String id, String judul, String ket, boolean produk, boolean pelanggan,
            boolean perToko, String url, boolean stokPerTanggal) throws Exception {
        JSONObject o = new JSONObject();
        o.put("id", id);
        o.put("judul", Common.getBahasaConfig(judul));
        o.put("ket", Common.getBahasaConfig(ket));
        if (produk) o.put("produk", true);
        if (pelanggan) o.put("pelanggan", true);
        if (perToko) o.put("perToko", true);
        if (stokPerTanggal) o.put("stokPerTanggal", true);
        if (url != null) o.put("url", url);
        return o;
    }

    /**
     * Kategori yang laporannya berbasis JURNAL akuntansi, sehingga bergantung pada Satuan Kerja.
     * Item di kategori ini diberi flag {@code satker:true} agar penyaji (Desktop/Android/JSP/ZK)
     * menampilkan pemilih Satuan Kerja; nilainya dikirim balik sebagai parameter {@code satkerId}.
     */
    private static final String[] KATEGORI_BERSATKER = new String[] {
            "Keuangan",
            "Buku Besar (Akuntansi)",
            "Kas & Bank (Akuntansi)",
            "Pajak (PPN)",
            "Anggaran (RAB)",
            "Rekonsiliasi Bank",
    };

    private static boolean kategoriBersatker(String namaKategori) {
        for (String s : KATEGORI_BERSATKER) {
            if (s.equals(namaKategori)) { return true; }
        }
        return false;
    }

    /**
     * Daftar Satuan Kerja untuk pemilih unit pada layar Laporan Keuangan.
     *
     * <p>Hanya satuan kerja yang DIPAKAI instansi ({@code default_item = true}) — tabel
     * {@code rab.satuan_kerja} juga memuat puluhan ribu baris referensi K/L pemerintah hasil impor
     * RKAKL yang tidak boleh ikut muncul. Baris pertama selalu "Semua Unit (Konsolidasi)" berid 0;
     * mesin laporan mengartikan {@code satkerId <= 0} sebagai tanpa penyaringan satuan kerja.</p>
     */
    public static JSONArray daftarSatuanKerja() throws Exception {
        JSONArray out = new JSONArray();
        JSONObject semua = new JSONObject();
        semua.put("id", 0);
        semua.put("kode", "");
        semua.put("nama", Common.getBahasaConfig("Semua Unit (Konsolidasi)"));
        out.put(semua);
        try {
            org.hibernate.Session session = ais.database.hibernate.HibernateUtil.currentSession();
            List<?> rows = session.createSQLQuery(
                    "select s.id as id, coalesce(s.kode,'') as kode, coalesce(s.nama,'') as nama "
                  + " from rab.satuan_kerja s where s.default_item is true order by s.kode, s.nama ")
                .list();
            for (Object ro : rows) {
                Object[] rr = (Object[]) ro;
                JSONObject o = new JSONObject();
                o.put("id", rr[0] == null ? 0L : ((Number) rr[0]).longValue());
                o.put("kode", rr[1] == null ? "" : rr[1].toString());
                o.put("nama", rr[2] == null ? "" : rr[2].toString());
                out.put(o);
            }
        } catch (Exception e) {
            ais.common.ErrorAuditUtil.record(e, "auto-audit LaporanKatalogData.daftarSatuanKerja");
        }
        return out;
    }

    /** Satuan Kerja yang dipra-pilih: konfigurasi {@code satuan_kerja_kantin}, 0 bila belum diisi. */
    public static long satuanKerjaBawaan() {
        try {
            String v = Common.getKonfigurasi("satuan_kerja_kantin", "").getNilai();
            if (v != null && v.trim().length() > 0) { return Long.parseLong(v.trim()); }
        } catch (Exception e) {
            ais.common.ErrorAuditUtil.record(e, "auto-audit LaporanKatalogData.satuanKerjaBawaan");
        }
        return 0L;
    }

    private static final class Kat {
        final String nama;
        final List<JSONObject> items = new ArrayList<JSONObject>();

        Kat(String nama) {
            this.nama = nama;
        }
    }

    /**
     * @return JSONArray {@code [{kat, items:[{id,judul,ket,produk?,pelanggan?,perToko?,url?}]}]} —
     *         struktur IDENTIK dengan array {@code REPORTS} di {@code laporan_laporan.jsp}.
     */
    public static JSONArray katalog() throws Exception {
        List<Kat> semua = new ArrayList<Kat>();

        Kat k;

        k = new Kat("Penjualan");
        k.items.add(item("pnj_barang_laku", "Barang Paling Laku", "Peringkat produk berdasarkan jumlah terjual.", true, false, true, null));
        k.items.add(item("pnj_faktur", "Daftar Faktur Penjualan", "Daftar transaksi/faktur penjualan beserta totalnya.", false, true, false, null));
        k.items.add(item("pnj_per_barang", "Penjualan per Barang", "Rekap qty & nilai penjualan per produk.", true, false, true, null));
        k.items.add(item("pnj_rincian_barang", "Rincian Penjualan per Barang", "Rincian baris penjualan tiap produk (dikelompokkan).", true, false, false, null));
        k.items.add(item("pnj_per_pelanggan", "Penjualan Barang per Pelanggan", "Nilai belanja per pelanggan/anggota.", false, true, true, null));
        k.items.add(item("pnj_per_kategori_pelanggan", "Penjualan per Kategori Pelanggan", "Nilai penjualan per kategori anggota.", false, false, true, null));
        k.items.add(item("pnj_per_cabang", "Penjualan per Cabang", "Nilai penjualan per toko/merchant."));
        k.items.add(item("pnj_per_pemasok", "Penjualan Barang Per Pemasok", "Nilai penjualan dikelompokkan per pemasok.", true, false, true, null));
        k.items.add(item("pnj_uang_muka", "Uang Muka Penjualan", "Faktur dengan pembayaran sebagian (uang muka).", false, true, false, null));
        k.items.add(item("pnj_detail_transaksi", "Detail Transaksi Penjualan", "Rincian item tiap transaksi POS (per nomor faktur).", true, false, false, null));
        k.items.add(item("pnj_per_jam", "Penjualan per Jam", "Omzet & transaksi tiap jam operasional (jam ramai).", false, false, true, null));
        k.items.add(item("pnj_per_kategori", "Penjualan per Kategori", "Rekap penjualan per kategori produk.", false, false, true, null));
        k.items.add(item("retur_barang", "Retur Barang", "Daftar transaksi retur barang.", true, false, false, null));
        semua.add(k);

        k = new Kat("Accurate POS");
        k.items.add(item("pos_kasir_harian", "Penjualan per Kasir per Hari", "Rekap penjualan tiap kasir dirinci per tanggal.", false, false, true, null));
        k.items.add(item("pos_harian", "Penjualan Harian (Semua Kasir)", "Total penjualan seluruh kasir per tanggal.", false, false, true, null));
        k.items.add(item("pos_per_kasir", "Laporan Penerimaan Per Kasir", "Penerimaan kas per kasir/operator (akumulasi).", false, false, true, null));
        k.items.add(item("transaksi_per_kasir", "Transaksi Per Kasir", "Rekonsiliasi modal, tunai, non-tunai, kas closing, dan selisih per kasir.", false, false, true, null));
        k.items.add(item("pos_per_akun_bank", "Penerimaan Per Akun (Bank/Tunai/E-Money/Voucher)", "Penerimaan per metode/akun pembayaran.", false, false, true, null));
        semua.add(k);

        k = new Kat("Kasir & Kas");
        k.items.add(item("pos_kasir_harian", "Penjualan per Kasir per Hari", "Rekap penjualan tiap kasir dirinci per tanggal."));
        k.items.add(item("kas_buka_tutup", "Buka Tutup Kas Kasir", "Sesi kas: modal awal, tunai, non-tunai, uang fisik, selisih."));
        k.items.add(item("kas_selisih", "Selisih Kas Kasir", "Sesi kas yang selisih (pengawasan akurasi kas)."));
        k.items.add(item("kasir_sering_selisih", "Kasir Sering Selisih", "Peringkat kasir berdasar frekuensi & besar selisih kas."));
        semua.add(k);

        k = new Kat("Pembelian");
        k.items.add(item("beli_penerimaan", "Daftar Penerimaan Barang", "Daftar penerimaan barang (kulakan) per faktur."));
        k.items.add(item("beli_pesanan", "Daftar Pesanan Pembelian", "Ringkasan pembelian per pemasok."));
        k.items.add(item("beli_rincian_penerimaan", "Rincian Penerimaan Barang", "Rincian item penerimaan (dikelompokkan per pemasok).", true, false, false, null));
        k.items.add(item("beli_rincian_pesanan", "Rincian Pesanan Pembelian", "Rincian item pembelian (dikelompokkan per pemasok).", true, false, false, null));
        k.items.add(item("beli_faktur", "Faktur Pembelian", "Daftar faktur pembelian per nomor faktur pemasok."));
        semua.add(k);

        k = new Kat("Persediaan");
        k.items.add(item("sed_barang_jasa", "Daftar Barang dan Jasa", "Master barang/jasa beserta harga & stok.", true, false, false, null));
        k.items.add(item("sed_penyesuaian", "Daftar Penyesuaian Stok Barang", "Ringkasan penyesuaian (stok opname) per produk.", true, false, false, null));
        k.items.add(item("sed_penyesuaian_rinci", "Penyesuaian Stok Barang (Rinci)", "Rincian selisih penyesuaian (dikelompokkan per produk).", true, false, false, null));
        k.items.add(item("sed_stok_harian", "Stok Harian (Nilai Persediaan)", "Posisi stok terkini + nilai persediaan.", true, false, false, null));
        semua.add(k);

        k = new Kat("Gudang");
        k.items.add(item("gud_kuantitas", "Kuantitas Barang per Gudang", "Stok kuantitas per gudang/toko.", true, false, false, null));
        k.items.add(item("gud_penghitungan", "Lembar Penghitungan Stok", "Lembar kerja penghitungan fisik stok.", true, false, false, null));
        k.items.add(item("gud_mutasi_kategori", "Ringkasan Mutasi Gudang By Category", "Mutasi masuk/keluar per kategori produk."));
        k.items.add(item("gud_mutasi_item", "Ringkasan Mutasi Gudang By Item", "Mutasi masuk/keluar per item produk.", true, false, false, null));
        k.items.add(item("pakai_bahan", "Pemakaian Bahan Baku", "Rincian pemakaian bahan baku (dikelompokkan per produk).", true, false, false, null));
        semua.add(k);

        k = new Kat("Pergudangan — Stok & Posisi");
        k.items.add(item("wh_stok_kini", "Stok Saat Ini (per Lokasi)", "Posisi stok tiap lokasi/gudang + nilai persediaan.", true, false, false, null));
        k.items.add(item("wh_valuasi", "Valuasi Persediaan per Lokasi", "Total nilai barang diringkas per lokasi/gudang."));
        k.items.add(item("wh_stok_minus", "Stok Minus (per Lokasi)", "Barang berstok negatif — deteksi salah input/jual tanpa stok.", true, false, false, null));
        k.items.add(item("wh_stok_kosong", "Stok Kosong (per Lokasi)", "Barang yang pernah bergerak namun stoknya kini 0.", true, false, false, null));
        k.items.add(item("stok_per_kategori", "Stok per Kategori", "Rekap jumlah & nilai stok per kategori produk.", false, false, true, null));
        k.items.add(item("wh_umur_stok", "Analisa Umur Stok", "Lama sejak barang terakhir masuk (barang lama menumpuk).", true, false, false, null));
        k.items.add(item("stok_minimum", "Stok Minimum / Reorder", "Produk yang stoknya mencapai/di bawah batas minimum.", true, false, true, null));
        semua.add(k);

        k = new Kat("Expired, Rusak & Hilang");
        k.items.add(item("barang_mendekati_expired", "Barang Mendekati Kadaluarsa", "Produk yang akan kedaluwarsa dalam 60 hari.", true, false, true, null));
        k.items.add(item("barang_expired", "Barang Kadaluarsa (Expired)", "Produk yang sudah lewat tanggal kedaluwarsa.", true, false, true, null));
        k.items.add(item("barang_rusak", "Barang Rusak", "Barang berkurang saat opname karena rusak/basi/pecah/cacat.", true, false, false, null));
        k.items.add(item("barang_hilang", "Barang Hilang", "Barang berkurang saat opname karena hilang/dicuri.", true, false, false, null));
        k.items.add(item("nilai_kerugian", "Nilai Kerugian Barang", "Akumulasi kerugian dari kekurangan stok saat opname per produk.", true, false, false, null));
        semua.add(k);

        k = new Kat("Pergudangan — Kartu & Mutasi");
        k.items.add(item("stok_per_tanggal", "Stok Barang per Tanggal",
                "Saldo stok tiap barang pada tanggal tertentu; filter kategori/grup, unduh Excel atau PDF.",
                true, false, true, null, true));
        k.items.add(item("wh_kartu_stok", "Kartu Stok Barang", "Riwayat masuk/keluar/transfer/koreksi + saldo berjalan.", true, false, false, null));
        k.items.add(item("wh_mutasi_harian", "Mutasi Stok Harian", "Total barang masuk & keluar tiap hari."));
        k.items.add(item("wh_mutasi_barang", "Mutasi Stok per Barang", "Rekap masuk/keluar/perubahan bersih per produk.", true, false, false, null));
        k.items.add(item("wh_mutasi_petugas", "Mutasi Stok per Petugas", "Aktivitas mutasi stok per petugas gudang."));
        k.items.add(item("wh_mutasi_dokumen", "Mutasi per Dokumen (Referensi)", "Pergerakan stok per nomor referensi/dokumen."));
        semua.add(k);

        k = new Kat("Pergudangan — Masuk / Keluar / Transfer");
        k.items.add(item("wh_barang_masuk", "Barang Masuk (per Lokasi)", "Rincian barang masuk ke tiap lokasi/gudang.", true, false, false, null));
        k.items.add(item("wh_barang_keluar", "Barang Keluar (per Lokasi)", "Rincian barang keluar dari tiap lokasi/gudang.", true, false, false, null));
        k.items.add(item("wh_transfer", "Transfer Stok Antar Lokasi", "Perpindahan barang dari satu lokasi ke lokasi lain.", true, false, false, null));
        semua.add(k);

        k = new Kat("Produksi & Resep (BOM)");
        k.items.add(item("resep_bom", "Resep / BOM per Menu", "Komposisi bahan baku tiap menu + subtotal biaya.", true, false, false, null));
        k.items.add(item("resep_hpp_menu", "HPP & Margin per Menu (dari Resep)", "HPP dari bahan baku vs harga jual & untung.", true, false, false, null));
        k.items.add(item("pakai_bahan", "Pemakaian Bahan Baku", "Rincian pemakaian bahan baku aktual (per produk).", true, false, false, null));
        k.items.add(item("kebutuhan_bahan", "Kebutuhan Bahan Baku (Resep x Jual)", "Bahan yang SEHARUSNYA dipakai dari resep x menu terjual."));
        k.items.add(item("bahan_selisih_resep", "Selisih Bahan Aktual vs Resep", "Pemakaian aktual vs seharusnya (deteksi pemborosan).", true, false, false, null));
        k.items.add(item("realisasi_produksi", "Realisasi Produksi Harian", "Porsi rencana/dibuat/terjual/sisa/waste per hari (menu Produksi Kantin).", true, false, false, null));
        k.items.add(item("rekap_produksi", "Rekap Produksi per Menu", "Total rencana/dibuat/terjual/sisa/waste per menu.", true, false, false, null));
        k.items.add(item("waste_produksi", "Sisa Makanan / Waste Produksi", "Porsi terbuang/rusak + nilai kerugian.", true, false, false, null));
        semua.add(k);

        k = new Kat("Stock Opname");
        k.items.add(item("sed_penyesuaian", "Ringkasan Penyesuaian per Produk", "Jumlah & total selisih opname per produk.", true, false, false, null));
        k.items.add(item("sed_penyesuaian_rinci", "Rincian Penyesuaian (Hasil Opname)", "Rincian stok sistem vs fisik & selisih.", true, false, false, null));
        k.items.add(item("opname_selisih_nilai", "Selisih Opname (Nilai Terbesar)", "Barang dengan selisih terbesar (qty & nilai).", true, false, false, null));
        k.items.add(item("opname_per_toko", "Riwayat Opname per Toko", "Rekap jumlah opname & nilai selisih per toko."));
        k.items.add(item("produk_sering_dikoreksi", "Produk Sering Dikoreksi", "Produk yang paling sering dikoreksi saat opname.", true, false, false, null));
        k.items.add(item("jadwal_opname", "Jadwal Stock Opname", "Rencana/kegiatan opname beserta status (dari menu Jadwal Opname)."));
        k.items.add(item("berita_acara_opname", "Berita Acara Opname (Ringkasan)", "Ringkasan hasil opname per toko: item lebih/kurang & nilai selisih."));
        semua.add(k);

        k = new Kat("Pemusnahan & Penghapusan");
        k.items.add(item("pemusnahan", "Pemusnahan / Penghapusan Barang", "Daftar penghapusan/pemusnahan barang + status persetujuan."));
        k.items.add(item("pemusnahan_rinci", "Rincian Pemusnahan per Item", "Barang yang dimusnahkan per dokumen.", true, false, false, null));
        semua.add(k);

        k = new Kat("Tenant / Stan");
        k.items.add(item("mst_tenant", "Daftar Tenant / Stan", "Daftar tenant/stan/outlet beserta jumlah produk."));
        k.items.add(item("pnj_per_cabang", "Penjualan per Tenant/Outlet", "Omzet & transaksi per tenant/toko."));
        k.items.add(item("tenant_setoran", "Setoran & Bagi Hasil Tenant", "Kewajiban (bagi hasil+sewa+biaya) & setoran per periode."));
        k.items.add(item("tenant_bagi_hasil", "Rekap Bagi Hasil per Tenant", "Total omzet/kewajiban/setoran/sisa per tenant."));
        k.items.add(item("tenant_tunggakan", "Tunggakan Tenant", "Tenant dengan setoran kurang dari kewajiban."));
        semua.add(k);

        k = new Kat("Piutang");
        k.items.add(item("ar_saldo", "Daftar Saldo Piutang Customer", "Saldo piutang per pelanggan/anggota.", false, true, true, null));
        k.items.add(item("ar_faktur_belum_lunas", "Faktur Belum Lunas", "Faktur penjualan yang belum lunas.", false, true, false, null));
        k.items.add(item("ar_umur_piutang", "Umur Piutang (Aging)", "Sisa piutang per kelompok umur 0-30/31-60/61-90/>90 hari.", false, true, false, null));
        k.items.add(item("ar_sisa_kredit", "Limit & Sisa Kredit Pelanggan", "Plafon kredit anggota vs piutang terpakai; sisa & tanda melebihi limit. Atur limit di Pengaturan.", false, true, false, null));
        k.items.add(item("akn_bb_pembantu_piutang", "Buku Besar Pembantu Piutang", "Mutasi piutang per pelanggan (belanja hutang vs pembayaran) + saldo berjalan."));
        k.items.add(item("ar_penerimaan_customer", "Riwayat Penerimaan Piutang Customer", "Pelunasan piutang dari pelanggan (modul Inventory & Sales) beserta status posting jurnalnya."));
        k.items.add(item("akn_histori_piutang", "Histori Piutang", "Seluruh mutasi piutang urut waktu (kronologis) + saldo total berjalan."));
        semua.add(k);

        k = new Kat("Pajak (PPN)");
        k.items.add(item("akn_ppn_rekap", "Rekapitulasi PPN (Keluaran & Masukan)", "Aktivitas akun PPN; Saldo = Kredit - Debet, total baris = PPN Kurang/(Lebih) Bayar. Akun PPN dikenali dari nama akun."));
        k.items.add(item("akn_ppn_keluaran", "Rincian PPN Keluaran (Penjualan)", "Baris jurnal pada akun PPN Keluaran — PPN dipungut atas penjualan."));
        k.items.add(item("akn_ppn_masukan", "Rincian PPN Masukan (Pembelian)", "Baris jurnal pada akun PPN Masukan — PPN dibayar atas pembelian."));
        semua.add(k);

        k = new Kat("Anggaran (RAB)");
        k.items.add(item("akn_anggaran", "Anggaran vs Realisasi (RAB)", "Serapan anggaran RAB (Workspace) Satuan Kerja kantin: Anggaran, Realisasi, Sisa, % Serap. Tahun dari filter tanggal."));
        semua.add(k);

        k = new Kat("Gaji Karyawan (Payroll)");
        k.items.add(item("akn_gaji_rincian", "Rincian Gaji Karyawan", "Komponen gaji per karyawan (Payroll seluruh instansi). Periode = tanggal bayar gaji."));
        k.items.add(item("akn_gaji_komponen", "Porsi Gaji per Komponen", "Total nilai gaji per komponen (tunjangan, potongan, dll)."));
        k.items.add(item("akn_gaji_pph", "Pajak Penghasilan (PPh) Karyawan", "Total PPh/pajak per karyawan dari komponen gaji."));
        semua.add(k);

        k = new Kat("Rekonsiliasi Bank");
        k.items.add(item("akn_rekon_ikhtisar", "Ikhtisar Rekonsiliasi Bank", "Saldo Buku (jurnal) vs Saldo Rekening Koran (bank) per akun + Selisih. Entri rekening koran di Pengaturan."));
        k.items.add(item("akn_rekon_belum", "Mutasi Rek. Koran Belum Cocok", "Baris rekening koran bank yang belum direkonsiliasi dengan buku."));
        semua.add(k);

        k = new Kat("Buku Besar (Akuntansi)");
        k.items.add(item("akn_jurnal", "Keseluruhan Jurnal (Jurnal Umum)", "Semua baris jurnal TERPOSTING Satuan Kerja kantin (dari Jurnal Akuntansi)."));
        k.items.add(item("akn_buku_besar", "Rincian Buku Besar (per Akun)", "Mutasi tiap akun + subtotal Debet/Kredit (dari Jurnal Akuntansi)."));
        k.items.add(item("akn_histori_bb", "Histori Buku Besar (Saldo Berjalan)", "Mutasi tiap akun + saldo berjalan per baris (dari Jurnal Akuntansi)."));
        k.items.add(item("akn_ringkasan_bb", "Ringkasan Buku Besar", "Total Debet, Kredit & Saldo per akun (dari Jurnal Akuntansi)."));
        k.items.add(item("akn_neraca_saldo", "Neraca Percobaan (Neraca Saldo)", "Saldo Debet/Kredit per akun; total harus seimbang (dari Jurnal Akuntansi)."));
        k.items.add(item("akn_neraca_percobaan", "Neraca Percobaan Lengkap (Awal - Mutasi - Akhir)", "Format kertas kerja yayasan: saldo awal D/K, mutasi kas dan non kas D/K, saldo akhir D/K per akun."));
        k.items.add(item("akn_daftar_akun", "Daftar Akun Perkiraan (Bagan Akun)", "Seluruh akun beserta klasifikasi Kelompok Laporan (Neraca/Laba Rugi)."));
        k.items.add(item("akn_ringkasan_beban", "Ringkasan Pencatatan Beban", "Total beban per akun (klasifikasi Beban/Biaya/HPP) dari Jurnal Akuntansi."));
        k.items.add(item("akn_rincian_beban", "Rincian Beban Pembayaran", "Rincian baris jurnal yang membebani akun Beban (per akun) dari Jurnal Akuntansi."));
        k.items.add(item("akn_diagnosa_jurnal_toko", "Diagnosa Jurnal Toko (Belum Diposting)", "Dokumen toko yang belum masuk buku besar per jenis: kulakan, bayar hutang, terima piutang, retur, opname, mutasi."));
        k.items.add(item("akn_diagnosa_akun", "Diagnosa Pemetaan Akun", "Akun yang dipakai jurnal TERPOSTING tetapi belum dipetakan ke Kelompok Laporan aktif, sehingga tidak muncul di Laba Rugi/Neraca."));
        k.items.add(item("gl_rincian", "Rincian Buku Besar Kas (Ringkas)", "Pembanding cepat dari data transaksi POS, BUKAN dari jurnal. Versi jurnalnya: 'Rekening Koran (Kas & Bank)'."));
        k.items.add(item("akn_aset_tetap", "Daftar Aset Tetap (Nilai Buku)", "Aktiva tetap aktif: nilai perolehan, akumulasi penyusutan, dan nilai buku."));
        semua.add(k);

        k = new Kat("Kas & Bank (Akuntansi)");
        k.items.add(item("akn_rekening_koran", "Rekening Koran (Kas & Bank)", "Mutasi tiap akun Kas/Bank: penerimaan vs pengeluaran (dari Jurnal Akuntansi)."));
        k.items.add(item("akn_penerimaan", "Ringkasan Daftar Penerimaan", "Total uang masuk per akun Kas/Bank pada periode (dari Jurnal Akuntansi)."));
        k.items.add(item("akn_pembayaran", "Ringkasan Daftar Pembayaran", "Total uang keluar per akun Kas/Bank pada periode (dari Jurnal Akuntansi)."));
        k.items.add(item("akn_posisi_dana", "Posisi Dana (Saldo Kas & Bank per Rekening)", "Saldo awal, mutasi debet/kredit, dan saldo akhir tiap rekening kas/bank dalam satu tabel. Padanan lembar Posisi Saldo Bank pada laporan yayasan."));
        semua.add(k);

        k = new Kat("Keuangan");
        k.items.add(item("akn_laba_rugi", "Laba Rugi (Berbasis Jurnal Akuntansi)", "Laba rugi resmi dari jurnal TERPOSTING + klasifikasi akun (Satuan Kerja kantin)."));
        k.items.add(item("akn_laporan_aktivitas", "Laporan Aktivitas (Surplus/Defisit)", "Format nirlaba/yayasan: Pendapatan, HPP, Laba Kotor + Contribution Margin, Biaya Tetap, Surplus (Defisit) + Profit Margin."));
        k.items.add(item("akn_neraca", "Neraca (Berbasis Jurnal Akuntansi)", "Neraca resmi dari jurnal TERPOSTING (kumulatif s/d Tgl Sampai) + laba berjalan; dilengkapi baris SELISIH."));
        k.items.add(item("akn_lr_2periode", "Laba Rugi — 2 Periode (Berbasis Jurnal)", "Periode berjalan dibanding periode sebelumnya yang sama panjang, per akun beserta selisihnya."));
        k.items.add(item("akn_neraca_2tanggal", "Neraca — 2 Tanggal (Berbasis Jurnal)", "Saldo tiap akun neraca per Tgl Mulai vs per Tgl Sampai beserta perubahannya."));
        k.items.add(item("akn_lr_12bulan", "Laba Rugi — 12 Bulan (Berbasis Jurnal)", "Dua belas bulan berakhir pada bulan Tgl Sampai, satu kolom per bulan."));
        k.items.add(item("akn_neraca_lajur", "Neraca Lajur (Kertas Kerja)", "Saldo tiap akun dipilah ke kolom Laba Rugi dan Neraca — kertas kerja tutup buku."));
        k.items.add(item("akn_arus_kas", "Arus Kas (Berbasis Jurnal Akuntansi)", "Mutasi akun Kas/Bank dari jurnal TERPOSTING, diuraikan menurut akun lawan; saldo awal & akhir ikut ditampilkan."));
        k.items.add(item("akn_arus_kas_aktivitas", "Arus Kas per Aktivitas (Operasional/Investasi/Pendanaan)", "Arus kas dikelompokkan ke aktivitas memakai Kelompok Laporan jenis Arus Kas atau kolom Aktifitas pada master akun. Padanan Laporan Penerimaan dan Pengeluaran Cash yayasan."));
        k.items.add(item("fin_laba_rugi", "Laba Rugi (Ringkas Operasional)", "Pembanding cepat dari data transaksi POS, BUKAN dari jurnal: penjualan − HPP − bahan. Laporan resmi = versi Berbasis Jurnal."));
        k.items.add(item("fin_neraca", "Neraca (Ringkas Operasional)", "Pembanding cepat dari data transaksi POS, BUKAN dari jurnal: kas, persediaan, piutang & modal per Tgl Sampai."));
        k.items.add(item("fin_penerimaan_per_metode", "Penerimaan per Kas/Bank (Harian)", "Penerimaan penjualan per akun kas/bank per hari. Nota split dipecah ke tiap metode sesuai nominalnya."));
        k.items.add(item("fin_arus_kas", "Arus Kas (Ringkas Operasional)", "Pembanding cepat dari data transaksi POS, BUKAN dari jurnal: penerimaan penjualan vs pengeluaran pengadaan."));
        semua.add(k);

        k = new Kat("Laporan Keuangan Resmi — Komparatif (Akuntansi)");
        k.items.add(item("lk_keu2", "Neraca / Laba Rugi / Arus Kas — 2 Periode", "Perbandingan 2 periode (pilih jenis di combo 'Jenis Laporan'). Resmi dari jurnal, Satuan Kerja kantin.", false, false, false, launchZk("keu2")));
        k.items.add(item("lk_keu12", "Neraca / Laba Rugi / Arus Kas — 12 Bulan", "Kolom 12 bulan berjalan (pilih jenis di combo). Resmi dari jurnal akuntansi.", false, false, false, launchZk("keu12")));
        k.items.add(item("lk_keu2th", "Neraca / Laba Rugi / Arus Kas — 2 Tahun", "Perbandingan 2 tahun (pilih jenis di combo). Resmi dari jurnal akuntansi.", false, false, false, launchZk("keu2th")));
        k.items.add(item("lk_neracalajur", "Neraca Lajur (Kertas Kerja)", "Worksheet neraca lajur akuntansi resmi.", false, false, false, launchZk("neracalajur")));
        semua.add(k);

        k = new Kat("Buku Besar Resmi (Akuntansi)");
        k.items.add(item("lk_trial", "Neraca Saldo / Trial Balance", "Neraca saldo resmi dari jurnal (versi Akuntansi, format JRXML).", false, false, false, launchZk("trial")));
        k.items.add(item("lk_bukubesar", "Buku Besar", "Buku besar resmi per akun (versi Akuntansi).", false, false, false, launchZk("bukubesar")));
        k.items.add(item("lk_bukubesartgl", "Buku Besar per Tanggal", "Buku besar resmi difilter tanggal (versi Akuntansi).", false, false, false, launchZk("bukubesartgl")));
        k.items.add(item("lk_jurnal", "Jurnal Harian", "Daftar jurnal harian resmi (versi Akuntansi).", false, false, false, launchZk("jurnal")));
        semua.add(k);

        k = new Kat("Arus Kas & Analisa Keuangan (Akuntansi)");
        k.items.add(item("lk_aruskas12", "Arus Kas — 12 Bulan (Kolom)", "Arus kas resmi format kolom 12 bulan.", false, false, false, launchZk("aruskas12")));
        k.items.add(item("lk_aruskas31", "Arus Kas — 31 Hari (Kolom)", "Arus kas resmi harian 31 hari.", false, false, false, launchZk("aruskas31")));
        k.items.add(item("lk_dashakun", "Rasio, Grafik, Laba Ditahan & Proyeksi Kas", "Buka Dasbor Akuntansi penuh: rasio keuangan, grafik, laba ditahan, perubahan ekuitas, proyeksi kas.", false, false, false, dashAkun()));
        semua.add(k);

        k = new Kat("Margin, Laba & Analisa");
        k.items.add(item("margin_produk", "Margin per Produk/Menu", "Penjualan, HPP, dan laba per produk.", true, false, true, null));
        k.items.add(item("margin_kategori", "Margin per Kategori", "Penjualan, HPP, dan laba kotor per kategori.", false, false, true, null));
        k.items.add(item("laba_kotor_harian", "Laba Kotor Harian", "Penjualan dikurangi HPP per hari.", false, false, true, null));
        k.items.add(item("produk_bawah_modal", "Produk Dijual di Bawah Modal", "Transaksi harga jual < harga modal (potensi rugi).", true, false, true, null));
        k.items.add(item("slow_moving", "Barang Slow Moving", "Produk paling lambat bergerak (qty terkecil).", true, false, true, null));
        k.items.add(item("harga_beli_terakhir", "Harga Beli Terakhir", "Harga modal terakhir tiap produk dari pengadaan.", true, false, false, null));
        k.items.add(item("mgr_pertumbuhan", "Pertumbuhan Penjualan (Bulanan)", "Tren omzet & transaksi per bulan.", false, false, true, null));
        k.items.add(item("analisa_diskon", "Analisa Diskon per Produk", "Total diskon yang diberikan tiap produk.", true, false, true, null));
        k.items.add(item("kontribusi_produk", "Kontribusi Produk (%)", "Persentase sumbangan tiap produk ke omzet.", true, false, true, null));
        k.items.add(item("dead_stock", "Barang Dead Stock / Tidak Laku", "Produk berstok tapi tanpa penjualan pada periode.", true, false, true, null));
        k.items.add(item("perputaran_stok", "Perputaran Stok (Turnover)", "Qty terjual dibagi stok saat ini.", true, false, true, null));
        k.items.add(item("prediksi_habis", "Prediksi Kehabisan Stok", "Perkiraan hari stok habis dari rata-rata jual 30 hari.", true, false, false, null));
        k.items.add(item("rekomendasi_beli", "Rekomendasi Pembelian Ulang", "Produk mau habis <14 hari + saran jumlah beli.", true, false, false, null));
        semua.add(k);

        k = new Kat("Pengadaan (Vendor / PO / BAST)");
        k.items.add(item("beli_pr", "Permintaan Pengadaan (PR)", "Daftar Purchase Requisition beserta status persetujuan."));
        k.items.add(item("beli_pr_rinci", "Rincian PR per Item", "Item barang tiap PR (dikelompokkan per nomor PR).", true, false, false, null));
        k.items.add(item("beli_po", "Pesanan Pembelian (PO)", "Purchase Order ke vendor + status pembayaran."));
        k.items.add(item("beli_po_rinci", "Rincian PO per Item", "Item barang tiap PO (dikelompokkan per nomor PO).", true, false, false, null));
        k.items.add(item("beli_bast", "Penerimaan Barang (BAST)", "Berita Acara Serah Terima barang dari vendor."));
        k.items.add(item("beli_bast_rinci", "Rincian BAST per Item", "Item barang tiap penerimaan/BAST (per nomor BAST).", true, false, false, null));
        k.items.add(item("beli_tagihan", "Terima Tagihan / Pembayaran Vendor", "Tagihan vendor: nilai, dibayar, pajak, sisa."));
        k.items.add(item("beli_utang_vendor", "Utang Vendor (Belum Lunas)", "Sisa kewajiban ke tiap vendor."));
        k.items.add(item("ap_saldo_supplier", "Saldo Hutang per Supplier (Toko)", "Sisa hutang tiap pemasok: nilai faktur kulakan dikurangi dibayar di muka dan pembayaran yang sudah dialokasikan."));
        k.items.add(item("ap_umur_utang", "Umur Hutang Supplier (Aging)", "Faktur kulakan yang belum lunas dipilah menurut umur sejak jatuh tempo."));
        k.items.add(item("ap_pembayaran", "Riwayat Pembayaran Hutang Supplier", "Pembayaran ke pemasok + faktur yang dilunasi + status posting jurnalnya."));
        k.items.add(item("akn_bb_pembantu_utang", "Buku Besar Pembantu Utang (Kulakan)", "Kulakan/pengadaan barang masuk per pemasok + saldo berjalan (utang bruto)."));
        k.items.add(item("beli_per_vendor", "Pembelian per Vendor", "Rekap nilai PO per vendor."));
        k.items.add(item("beli_retur_pengadaan", "Retur Pengadaan (ke Vendor)", "Retur barang yang dikembalikan ke vendor."));
        k.items.add(item("beli_belum_datang", "Barang Belum Diterima (PR)", "Permintaan pengadaan yang barangnya belum lengkap datang."));
        semua.add(k);

        k = new Kat("Deposit / Saldo");
        k.items.add(item("saldo_deposit", "Deposit / Top Up Saldo", "Daftar setoran/top up saldo (modul Deposit)."));
        k.items.add(item("saldo_deposit_rekap", "Rekap Deposit per Nama", "Total top up per nama/pengguna."));
        k.items.add(item("psaldo_rincian", "Rincian Penyesuaian Saldo (Opname Voucher)", "Hasil opname saldo anggota: saldo sistem vs saldo seharusnya, selisih, dan alasannya."));
        k.items.add(item("psaldo_rekap", "Rekap Penyesuaian Saldo per Anggota", "Berapa kali saldo tiap anggota disesuaikan beserta total selisihnya."));
        k.items.add(item("potong_gaji", "Potong Gaji Pegawai", "Potongan gaji pegawai (mis. belanja kantin) dari Transaksi Pegawai. Cari: ketik kantin.", false, true, false, null));
        semua.add(k);

        k = new Kat("Dompet, Tabungan & Voucher");
        k.items.add(item("dompet_saldo", "Saldo Tabungan/Dompet per Anggota", "Saldo tabungan tiap anggota/siswa/mahasiswa (Deposit).", false, true, false, null));
        k.items.add(item("dompet_per_jenis", "Rekap Tabungan per Jenis", "Total saldo per jenis tabungan."));
        k.items.add(item("pencairan_saldo", "Pencairan / Penarikan Saldo", "Pencairan saldo anggota + status.", false, true, false, null));
        k.items.add(item("rekap_pencairan", "Rekap Pencairan/Refund", "Rekap pencairan berhasil per cara bayar."));
        k.items.add(item("voucher_kantin", "Voucher Kantin (Topup/Expiry)", "Voucher/topup beserta kadaluarsa & status.", false, true, false, null));
        semua.add(k);

        k = new Kat("Konsumsi (Siswa / Mahasiswa)");
        k.items.add(item("pnj_per_pelanggan", "Belanja per Siswa/Mahasiswa", "Total belanja per anggota/pelanggan.", false, true, false, null));
        k.items.add(item("konsumsi_kelas", "Rekap Belanja per Kelas", "Total belanja kantin per kelas siswa."));
        k.items.add(item("konsumsi_prodi", "Rekap Belanja per Prodi/Jurusan", "Total belanja kantin per prodi/jurusan mahasiswa."));
        semua.add(k);

        k = new Kat("Audit");
        k.items.add(item("audit_harga", "Audit Perubahan Harga", "Riwayat perubahan harga modal produk (Envers)."));
        k.items.add(item("audit_master", "Audit Perubahan Master Barang", "Riwayat perubahan nama produk (Envers)."));
        semua.add(k);

        k = new Kat("Master Data");
        k.items.add(item("mst_pemasok", "Daftar Pemasok / Supplier", "Daftar pemasok beserta total pengadaan."));
        k.items.add(item("mst_pelanggan", "Daftar Pelanggan / Member", "Daftar anggota/pelanggan koperasi.", false, true, false, null));
        k.items.add(item("mst_vendor", "Daftar Vendor / Penyedia", "Master vendor/penyedia dari modul Pengadaan."));
        semua.add(k);

        JSONArray out = new JSONArray();
        for (Kat kk : semua) {
            JSONObject o = new JSONObject();
            o.put("kat", Common.getBahasaConfig(kk.nama));
            JSONArray items = new JSONArray();
            boolean bersatker = kategoriBersatker(kk.nama);
            for (JSONObject it : kk.items) {
                // Laporan ber-url (JRXML/ZK) memakai formnya sendiri, jadi tidak diberi flag.
                if (bersatker && !it.has("url")) { it.put("satker", true); }
                items.put(it);
            }
            o.put("items", items);
            out.put(o);
        }
        return out;
    }

    /**
     * Subset katalog khusus <b>Laporan Keuangan</b> — hanya kategori akuntansi/keuangan, dalam urutan
     * yang lebih enak dibaca (Neraca/Laba Rugi → Arus Kas → Buku Besar → Kas &amp; Bank → Piutang →
     * Utang/Pengadaan → Pajak → Anggaran → Gaji → Rekonsiliasi). Dipakai menu "Laporan Keuangan" pada
     * Desktop/Android/JSP/ZK. Reuse penuh {@link #katalog()} (label/kunci/SQL sama) — di sini hanya
     * memilih &amp; mengurutkan kategori keuangan, tanpa menduplikasi definisi laporan.
     */
    public static JSONArray katalogKeuangan() throws Exception {
        // Nama kategori MENTAH (sesuai new Kat(...)); dibandingkan setelah getBahasaConfig agar cocok
        // dengan field "kat" pada output katalog() (yang sudah diterjemahkan).
        // SENGAJA HANYA kategori yang laporannya dijalankan NATIVE via mesin laporan (aksi
        // laporan_jalankan / LaporanKantinUtil) — kategori "…Resmi (Akuntansi)" & "Arus Kas & Analisa"
        // yang isinya laporan JRXML/ZK berbasis {@code url} TIDAK diikutkan, supaya menu Laporan
        // Keuangan berjalan native di SEMUA platform (Desktop/Android via API, JSP versi JSP-nya
        // sendiri, ZK di dasbor ZK-nya sendiri) — bukan membuka URL ke platform lain.
        String[] kategoriKeuangan = new String[] {
                "Keuangan",
                "Buku Besar (Akuntansi)",
                "Kas & Bank (Akuntansi)",
                "Piutang",
                "Pengadaan (Vendor / PO / BAST)",
                "Pajak (PPN)",
                "Anggaran (RAB)",
                "Gaji Karyawan (Payroll)",
                "Rekonsiliasi Bank",
        };
        JSONArray semua = katalog();
        JSONArray out = new JSONArray();
        for (String raw : kategoriKeuangan) {
            String label = Common.getBahasaConfig(raw);
            for (int i = 0; i < semua.length(); i++) {
                JSONObject kat = semua.getJSONObject(i);
                if (!label.equals(kat.optString("kat"))) {
                    continue;
                }
                // Saring item ber-url (laporan JRXML/ZK) agar hanya laporan native yang tampil.
                JSONArray itemsAsli = kat.optJSONArray("items");
                JSONArray itemsNative = new JSONArray();
                for (int j = 0; itemsAsli != null && j < itemsAsli.length(); j++) {
                    JSONObject it = itemsAsli.getJSONObject(j);
                    if (!it.has("url")) {
                        itemsNative.put(it);
                    }
                }
                if (itemsNative.length() > 0) {
                    JSONObject katBaru = new JSONObject();
                    katBaru.put("kat", kat.optString("kat"));
                    katBaru.put("items", itemsNative);
                    out.put(katBaru);
                }
                break;
            }
        }
        return out;
    }
}
