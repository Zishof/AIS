package ais.action.master.koperasi.helper;

import org.hibernate.Session;

import ais.action.master.asset.util.AssetUtil;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.akunting.Akun;
import ais.database.model.asset.KelompokAsset;
import ais.database.model.asset.MasterAsset;
import ais.database.model.inventory.JenisProduk;
import ais.database.model.inventory.Produk;
import ais.database.model.library.Penyedia;

/**
 * Resolusi akun untuk penjurnalan rantai <b>pengadaan &rarr; pembayaran</b> milik toko.
 *
 * <p>Sebelumnya hanya Penjualan &amp; HPP kantin yang punya jurnal, sehingga akun Persediaan
 * dikredit (oleh HPP) tanpa pernah didebet (kulakan tidak dijurnal) dan Utang Usaha tak pernah
 * muncul di Neraca. Kelas ini memusatkan penentuan akun untuk jurnal-jurnal penutup celah itu:
 * kulakan, pembayaran hutang supplier, penerimaan piutang customer, retur, opname, dan mutasi.</p>
 *
 * <p><b>Urutan pencarian dibuat konsisten:</b> master yang paling dekat dengan barang/mitra dulu
 * (Penyedia, Jenis Produk, Cara Pembayaran), lalu master <b>Toko</b> tempat kejadiannya, baru
 * konfigurasi global sebagai cadangan terakhir. Semua method mengembalikan {@code null} bila tidak
 * ketemu &mdash; pemanggil WAJIB memperlakukan itu sebagai "belum siap posting" dan menampilkan
 * alasannya, bukan memaksakan jurnal timpang.</p>
 */
public final class AkunKantinUtil {

    /** Konfigurasi cadangan; dibuat otomatis (kosong) saat pertama kali dibaca. */
    public static final String CFG_UTANG_SUPPLIER = "akun_utang_supplier_toko";
    public static final String CFG_UTANG_LAMA = "akun_utang_id_default_data";
    public static final String CFG_KAS_TOKO = "akun_kas_toko";
    public static final String CFG_PIUTANG_TOKO = "akun_piutang_toko";
    public static final String CFG_SELISIH_PERSEDIAAN = "akun_selisih_persediaan_toko";
    public static final String CFG_RETUR_PENJUALAN = "akun_retur_penjualan_toko";

    private AkunKantinUtil() {
    }

    /** Satuan kerja kantin dari konfigurasi; null artinya tanpa pembatasan satuan kerja. */
    public static SatuanKerja satkerKantin() {
        try {
            String v = Common.getKonfigurasi("satuan_kerja_kantin", "").getNilai();
            if (v == null || v.trim().isEmpty()) {
                return null;
            }
            return (SatuanKerja) ConstantValues.ambil(SatuanKerja.class.getName(),
                    Long.valueOf(Long.parseLong(v.trim())));
        } catch (Exception e) {
            return null;
        }
    }

    /** Akun dari konfigurasi berisi ID akun; null bila kosong/tidak valid. */
    public static Akun akunKonfigurasi(String kunci) {
        try {
            String v = Common.getKonfigurasi(kunci, "").getNilai();
            if (v == null || v.trim().isEmpty()) {
                return null;
            }
            return (Akun) ConstantValues.ambil(Akun.class.getName(), Long.valueOf(Long.parseLong(v.trim())));
        } catch (Exception e) {
            return null;
        }
    }

    /** Akun pada master Toko; null bila toko/akunnya tidak diisi. Jenis: kas|piutang|modal|laba. */
    public static Akun akunToko(Session session, Long tokoId, String jenis) {
        if (session == null || tokoId == null || tokoId.longValue() <= 0) {
            return null;
        }
        try {
            ais.database.model.inventory.Toko t = (ais.database.model.inventory.Toko) session
                    .get(ais.database.model.inventory.Toko.class, tokoId);
            if (t == null) {
                return null;
            }
            if ("piutang".equals(jenis)) {
                return t.getAkunPiutang();
            }
            if ("modal".equals(jenis)) {
                return t.getAkunModalAwal();
            }
            if ("laba".equals(jenis)) {
                return t.getAkunLabaDitahan();
            }
            return t.getAkunKas();
        } catch (Exception e) {
            ais.common.ErrorAuditUtil.record(e, "auto-audit AkunKantinUtil.akunToko");
            return null;
        }
    }

    /**
     * Akun Persediaan barang: {@code MasterAsset.akunTransaksi}, cadangan
     * {@code KelompokAsset.akunTransaksi} &mdash; sumber yang SAMA dengan sisi kredit jurnal HPP,
     * supaya debet kulakan dan kredit HPP pasti mengenai akun yang sama.
     */
    public static Akun akunPersediaan(Session session, Produk produk, SatuanKerja satker) {
        if (produk == null) {
            return null;
        }
        try {
            MasterAsset ma = produk.getMasterAsset();
            if (ma == null) {
                return null;
            }
            if (ma.getAkunTransaksi() != null && !ma.getAkunTransaksi().trim().isEmpty()) {
                Akun a = AssetUtil.ambilDataAkun(ma.getAkunTransaksi(), satker);
                if (a != null) {
                    return a;
                }
            }
            KelompokAsset kelompok = ma.getKelompokAsset();
            if (kelompok != null && kelompok.getAkunTransaksi() != null) {
                return AssetUtil.ambilDataAkun(kelompok.getAkunTransaksi(), satker);
            }
        } catch (Exception e) {
            ais.common.ErrorAuditUtil.record(e, "auto-audit AkunKantinUtil.akunPersediaan");
        }
        return null;
    }

    /** Akun HPP barang: Jenis Produk dulu, cadangan Kelompok Aset. */
    public static Akun akunHpp(Session session, Produk produk, SatuanKerja satker) {
        if (produk == null) {
            return null;
        }
        try {
            JenisProduk jp = produk.getJenisProduk();
            if (jp != null && jp.getAkunHpp() != null) {
                return jp.getAkunHpp();
            }
            MasterAsset ma = produk.getMasterAsset();
            if (ma != null && ma.getKelompokAsset() != null
                    && ma.getKelompokAsset().getAkunBebanPokokPenjualan() != null) {
                return AssetUtil.ambilDataAkun(ma.getKelompokAsset().getAkunBebanPokokPenjualan(), satker);
            }
        } catch (Exception e) {
            ais.common.ErrorAuditUtil.record(e, "auto-audit AkunKantinUtil.akunHpp");
        }
        return null;
    }

    /** Akun Pendapatan Penjualan barang (dipakai jurnal retur penjualan). */
    public static Akun akunPendapatan(Session session, Produk produk) {
        try {
            JenisProduk jp = produk == null ? null : produk.getJenisProduk();
            return jp == null ? null : jp.getAkunPendapatan();
        } catch (Exception e) {
            return null;
        }
    }

    /** Akun Retur Penjualan (kontra-pendapatan); cadangan: akun pendapatan barangnya. */
    public static Akun akunReturPenjualan(Session session, Produk produk) {
        try {
            JenisProduk jp = produk == null ? null : produk.getJenisProduk();
            if (jp != null && jp.getAkunReturPenjualan() != null) {
                return jp.getAkunReturPenjualan();
            }
        } catch (Exception e) {
            ais.common.ErrorAuditUtil.record(e, "auto-audit AkunKantinUtil.akunReturPenjualan");
        }
        Akun a = akunKonfigurasi(CFG_RETUR_PENJUALAN);
        return a != null ? a : akunPendapatan(session, produk);
    }

    /**
     * Akun Utang Dagang supplier toko: {@code Penyedia.akunUtang}, cadangan konfigurasi
     * {@code akun_utang_supplier_toko}, lalu {@code akun_utang_id_default_data} (dipakai bersama
     * rantai pengadaan aset).
     */
    public static Akun akunUtangSupplier(Session session, Penyedia penyedia) {
        try {
            if (penyedia != null && penyedia.getAkunUtang() != null) {
                return penyedia.getAkunUtang();
            }
        } catch (Exception e) {
            ais.common.ErrorAuditUtil.record(e, "auto-audit AkunKantinUtil.akunUtangSupplier");
        }
        Akun a = akunKonfigurasi(CFG_UTANG_SUPPLIER);
        return a != null ? a : akunKonfigurasi(CFG_UTANG_LAMA);
    }

    /**
     * Akun Kas/Bank untuk metode pembayaran toko. Metode disimpan sebagai teks bebas
     * ("TUNAI"/"TRANSFER"/"BG"...), jadi dicocokkan ke master Cara Pembayaran (yang memang
     * sudah punya akun) lewat kode/nama; bila gagal jatuh ke konfigurasi {@code akun_kas_toko}.
     */
    public static Akun akunKasBank(Session session, String metode) {
        return akunKasBank(session, metode, null);
    }

    public static Akun akunKasBank(Session session, String metode, Long tokoId) {
        try {
            String m = metode == null ? "" : metode.trim();
            if (!m.isEmpty() && session != null) {
                Object o = session.createQuery(
                        "select c.akun from CaraPembayaranKoperasi c where c.akun is not null"
                                + " and (upper(c.kode) = :m or upper(c.nama) = :m)")
                        .setString("m", m.toUpperCase()).setMaxResults(1).uniqueResult();
                if (o instanceof Akun) {
                    return (Akun) o;
                }
            }
        } catch (Exception e) {
            ais.common.ErrorAuditUtil.record(e, "auto-audit AkunKantinUtil.akunKasBank");
        }
        Akun dariToko = akunToko(session, tokoId, "kas");
        return dariToko != null ? dariToko : akunKonfigurasi(CFG_KAS_TOKO);
    }

    /**
     * Akun Piutang Usaha toko: konfigurasi {@code akun_piutang_toko}, cadangan akun pada cara
     * pembayaran yang ditandai "masuk sebagai hutang" (itulah akun yang didebet saat penjualan
     * kredit diposting, sehingga penerimaan piutang mengkredit akun yang sama).
     */
    public static Akun akunPiutang(Session session) {
        return akunPiutang(session, null);
    }

    public static Akun akunPiutang(Session session, Long tokoId) {
        Akun dariToko = akunToko(session, tokoId, "piutang");
        if (dariToko != null) {
            return dariToko;
        }
        Akun a = akunKonfigurasi(CFG_PIUTANG_TOKO);
        if (a != null) {
            return a;
        }
        try {
            if (session != null) {
                Object o = session.createQuery(
                        "select c.akun from CaraPembayaranKoperasi c where c.akun is not null"
                                + " and c.masukSebagaiHutang = true")
                        .setMaxResults(1).uniqueResult();
                if (o instanceof Akun) {
                    return (Akun) o;
                }
            }
        } catch (Exception e) {
            ais.common.ErrorAuditUtil.record(e, "auto-audit AkunKantinUtil.akunPiutang");
        }
        return null;
    }

    /** Akun selisih persediaan (susut/lebih) untuk jurnal stok opname. */
    public static Akun akunSelisihPersediaan(Session session, Produk produk, SatuanKerja satker) {
        try {
            JenisProduk jp = produk == null ? null : produk.getJenisProduk();
            if (jp != null && jp.getAkunSelisihPersediaan() != null) {
                return jp.getAkunSelisihPersediaan();
            }
        } catch (Exception e) {
            ais.common.ErrorAuditUtil.record(e, "auto-audit AkunKantinUtil.akunSelisihPersediaan");
        }
        Akun a = akunKonfigurasi(CFG_SELISIH_PERSEDIAAN);
        return a != null ? a : akunHpp(session, produk, satker);
    }

    /**
     * Akun Modal/Ekuitas Awal: master Toko dulu, lalu konfigurasi {@code akun_modal_awal}.
     * Dipakai menampung selisih debet-kredit pada jurnal pembukaan saldo awal.
     */
    public static Akun akunModalAwal(Session session, Long tokoId) {
        Akun dariToko = akunToko(session, tokoId, "modal");
        return dariToko != null ? dariToko : akunKonfigurasi("akun_modal_awal");
    }

    /**
     * Akun Laba Ditahan: master Toko dulu, lalu konfigurasi {@code akun_laba_ditahan}.
     * Tujuan pemindahan laba/rugi bersih saat tutup buku.
     */
    public static Akun akunLabaDitahan(Session session, Long tokoId) {
        Akun dariToko = akunToko(session, tokoId, "laba");
        return dariToko != null ? dariToko : akunKonfigurasi("akun_laba_ditahan");
    }

    /** Teks akun utk ditampilkan di draf jurnal. */
    public static String label(Akun a) {
        if (a == null) {
            return "";
        }
        String kode = a.getKode() == null ? "" : a.getKode().trim();
        String nama = a.getNama() == null ? "" : a.getNama().trim();
        return (kode + " " + nama).trim();
    }
}
