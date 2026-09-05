# 115 — Analisis Apotik per-tenant (di bawah keputusan (a))

**Tanggal:** 2026-09-06
**Sifat:** analisis. Tidak ada kode yang diubah.
**Keputusan yang berlaku:** **(a) — akuntansi tetap se-tenant.** `{S}.akun`, `{S}.jurnal`,
`{S}.jurnal_detail`, `{S}.periode_akuntansi`, `{S}.posting_log` tetap menjadi satu-satunya rumah
akuntansi penyewa. Kolom pembeda di `akunting.*` **tidak** dipakai.
**Pendahulu:** [113](113-kompatibilitas-per-tenant-modul-lain.md), [114](114-kolom-pembeda-vs-schema-per-tenant.md).

---

## Koreksi terhadap doc 113

Doc 113 menaksir biaya Apotik per-tenant "kira-kira setara dengan seluruh pemindahan Inventory &
Sales". **Taksiran itu terlalu tinggi.** Tiga pengukuran baru menurunkannya:

1. **Inti farmasinya sudah ada di katalog tenant** — termasuk batch dan kedaluwarsa.
2. **Ketergantungan klinisnya lunak** — seluruhnya `nullable`, dan jalur kasir tanpa pasien sudah
   berjalan hari ini.
3. **Pekerjaannya bukan mengubah 176 panggilan ORM**, melainkan menambah cabang tenant di sebelahnya
   — persis pola SalesInventory.

Taksiran terkoreksi: **kira-kira setengah sampai dua-pertiga** dari pemindahan Inventory & Sales.

Doc 113 tetap benar pada temuan pokoknya: Apotik tidak sadar-tenant, dan `@Table(schema = "sirs")`
memang tidak bisa diubah saat runtime. Yang keliru adalah kesimpulan biayanya, karena ia mengandaikan
Apotik harus *dipindahkan*, bukan *didampingi*.

---

## Temuan 1 — ketergantungan klinis itu lunak

Apotik memang tertanam di `sirs`: 35% sebutan entitasnya klinis (61 dari 176) — `Resep` 14,
`AntreanFarmasi` 7, `Pasien` 5, `Dokter` 4. Sekilas ini berarti Apotik per-tenant harus menyeret
inti rumah sakit.

Ternyata tidak. **Semua ikatan klinisnya boleh kosong:**

```java
// TransaksiMedis.java
@JoinColumn(name = "pendaftaran", nullable = true)
@JoinColumn(name = "pasien",      nullable = true)
@JoinColumn(name = "resep",       nullable = true)
// Resep.java
@JoinColumn(name = "diagnosa_penyakit", nullable = true)
```

Dan dari 13 entitas yang benar-benar baru, **tidak satu pun** punya kunci asing klinis yang wajib.

Lebih dari itu, jalur kasir apotek **tanpa pasien sudah ada dan sudah jalan**:

```java
// ApotikApiHelper.java:1276-1293
TransaksiMedis trx = new TransaksiMedis();
trx.setJenisTransaksi(TransaksiMedis.TRX_ITEM);
trx.setSumber(TransaksiMedis.SUMBER_APOTIK);
trx.setBebas(Boolean.TRUE);
trx.setLunas(Boolean.TRUE);
if (resep != null) { trx.setResep(resep); }     // opsional
if (!namaPembeli.isEmpty()) { trx.setNama(namaPembeli); }     // teks bebas
if (!alamatPembeli.isEmpty()) { trx.setAlamat(alamatPembeli); }
```

Pembeli dicatat sebagai **teks pada transaksinya**, bukan baris `Pasien`. Jadi apotek mandiri tidak
menyentuh `Pasien`, `Pendaftaran`, `Dokter`, maupun `Poly` sama sekali.

**Akibatnya lingkupnya bisa dipotong, bukan hanya dibayar.**

---

## Temuan 2 — lingkup yang sebenarnya: 45 dari 64 aksi

| golongan | jumlah | keterangan |
|---|---:|---|
| **inti farmasi-ritel** | **45** | sasaran per-tenant |
| klinis | 13 | `pasien_*`, `resep_*`, `antrean_farmasi_*`, `dispensing_*` — di luar lingkup apotek mandiri |
| demo/penyemaian | 6 | perancah UAT, tidak perlu per-tenant |
| **total** | **64** | |

Pembanding: SalesInventory mencabangkan **83** aksi `si_*`. Jadi inti Apotik ≈ **54%** dari
pekerjaan itu.

45 aksi intinya:

```
batch, batch_monitor, batch_status_ubah, bayar, bayar_racikan, cara_bayar_list,
delivery_list, delivery_simpan, delivery_status, formularium, item_batch, item_cari,
item_profil_simpan, kasir, laporan, laporan_kedaluwarsa, laporan_pembayaran,
laporan_penjualan, laporan_terkendali, metrik_operasional, narkotika, opname_simpan,
pbf_bayar, pbf_list, pemetaan_akun_, pemetaan_akun_audit, pemetaan_akun_terapkan,
pengadaan, posting_, posting_bayar_hutang_pbf_, posting_hpp_, posting_pbf_,
posting_penjualan_, produksi_katalog, produksi_proses, racikan, racikan_list,
retur, retur_simpan, sesi_kas_buka, sesi_kas_list, sesi_kas_status, sesi_kas_tutup,
stok_opname, terima_barang
```

---

## Temuan 3 — katalog tenant sudah menampung intinya

Yang sudah ada dan **tidak perlu dibuat lagi**:

| kebutuhan apotek | tabel tenant yang sudah ada |
|---|---|
| obat/barang | `{S}.produk`, `{S}.kategori_produk`, `{S}.satuan` |
| **batch & kedaluwarsa** | `{S}.produk_batch` — `batch_no`, **`expiry_date`**, indeksnya lengkap |
| stok | `{S}.mutasi_stok`, `{S}.saldo_stok`, `{S}.lokasi_stok`, `{S}.gudang` |
| stok opname | `{S}.stok_opname`, `{S}.stok_opname_detail` |
| penjualan | `{S}.faktur_penjualan`, `{S}.faktur_penjualan_detail` |
| kasir | `{S}.sesi_kas_kasir`, `{S}.draft_penjualan`, `{S}.cara_pembayaran` |
| pembelian PBF | `{S}.pembelian`, `{S}.pembelian_detail`, `{S}.supplier`, `{S}.hutang_supplier` |
| bayar hutang PBF | `{S}.pembayaran_hutang`, `{S}.alokasi_pembayaran_hutang` |
| **akuntansi (kep. (a))** | `{S}.akun`, `{S}.jurnal`, `{S}.jurnal_detail`, `{S}.periode_akuntansi`, `{S}.posting_log` |
| retur | `{S}.retur_penjualan` |
| bahan baku racikan | `{S}.pemakaian_bahan_baku` |

`{S}.produk_batch` yang sudah memuat `expiry_date` adalah yang paling menentukan — pelacakan
kedaluwarsa adalah inti apotek, dan ia sudah terpasang berikut indeksnya.

Ini juga berarti `apotik_laporan_kedaluwarsa`, `apotik_batch_monitor`, `apotik_stok_opname`,
`apotik_terima_barang`, `apotik_sesi_kas_*`, dan seluruh `apotik_posting_*` bisa memakai ulang SQL
tenant yang **sudah ditulis dan sudah diuji** di `SalesInventoryStokTenant`,
`SalesInventoryFinanceTenant`, dan `SalesInventoryPayableTenant`.

### Yang benar-benar baru: bundel v20

13 entitas, 89 kolom seluruhnya — kecil:

| entitas | kolom | untuk apa |
|---|---:|---|
| `ApotikItemProfile` | 9 | golongan obat (BEBAS / BEBAS_TERBATAS / KERAS / NARKOTIKA / PSIKOTROPIKA) |
| `ApotikNarkotikaLog` | 9 | register obat terkendali — kewajiban regulasi |
| `ApotikBatchKonsumsi` | 4 | pemakaian per-batch |
| `Racikan`, `RacikanDetail` | 4+4 | resep racikan |
| `Produksi`, `BahanBakuItem` | 7+2 | produksi racikan |
| `ApotikDispensingLog` | 9 | jejak penyerahan |
| `ApotikDeliveryOrder` | 18 | pengantaran |
| `ApotikAkunMapping` | 4 | pemetaan akun — **menyambung ke `{S}.akun`, sesuai kep. (a)** |
| `Resep`, `ResepDetail` | 3+2 | hanya bila resep diinginkan |
| `AntreanFarmasi` | 14 | hanya bila antrean diinginkan |

Tanpa resep dan antrean (lingkup apotek mandiri): **10 tabel, ~72 kolom.** Satu bundel v20 wajar.

---

## Yang dituntut keputusan (a)

Keputusan (a) justru **menyederhanakan** Apotik, karena menutup satu percabangan:

- `apotik_posting_penjualan_`, `apotik_posting_hpp_`, `apotik_posting_pbf_`,
  `apotik_posting_bayar_hutang_pbf_` semuanya menulis ke **`{S}.jurnal`**, bukan `akunting.jurnal`.
- `apotik_pemetaan_akun_*` memetakan ke **`{S}.akun`**, bukan `akunting.akun`.
- `ApotikAkunMapping` karena itu menyimpan id akun se-tenant.

Ini menghapus masalah yang akan muncul bila (b) dipilih: penulisan **lintas-model** dari `sirs`
ber-kolom-pembeda ke `{S}.jurnal` ber-schema, yang tidak bisa dijamin kunci asing dan membuka kelas
kebocoran baru (`{S}.posting_log` menunjuk baris `sirs` milik pendaftar lain, tanpa ada yang
mencegah).

Di bawah (a), Apotik per-tenant memakai **satu model isolasi yang sama** dengan Inventory. Penjaga
yang sudah ada — `audit-local-first.py` dan `audit-penjaga-tenant.py` — bisa diperluas, bukan
diduplikasi untuk model kedua.

---

## Cara kerjanya: mendampingi, bukan memindahkan

Ini inti koreksi terhadap doc 113. Pola SalesInventory tidak mengubah kode lama:

```java
if (SalesInventoryStokTenant.aktif(ctx)) {
    sql = SalesInventoryStokTenant.sqlKartu(skema, pid, dari, sampai);   // cabang tenant
} else {
    sql = ...;                                                          // kode LAMA, utuh
}
```

Untuk Apotik berlaku sama: **176 panggilan ORM yang ada tidak disentuh.** Ia tetap melayani
instalasi rumah sakit di `sirs`. Yang ditambahkan adalah cabang tenant di **titik masuk aksi** —
45 tempat, bukan 176 — yang memakai SQL native atas `{S}.*`.

Konsekuensinya: instalasi rumah sakit yang berjalan **tidak menanggung risiko apa pun** dari
pekerjaan ini. Itu perbedaan besar dibanding "membalik 176 panggilan ORM", yang menyentuh jalur
hidup.

---

## Taksiran pekerjaan

| bagian | ukuran | pembanding |
|---|---|---|
| bundel katalog v20 | 10 tabel, ~72 kolom | v19 menambah 6 kolom; v3 puluhan tabel |
| kelas `Apotik*Tenant.java` | ~6–8 berkas | SalesInventory: 11 |
| cabang di titik masuk aksi | 45 | SalesInventory: 83 |
| pakai ulang SQL tenant yang ada | stok, kasir, hutang, posting | tak perlu ditulis lagi |
| audit tulis-tangan | ~5 entitas baru | `SalesInventoryAudit`: 7 |
| uji kesetaraan | ~8–10 berkas | tenant-inventory-sales: 28 |
| penjaga dispatcher | perluas yang ada | tak perlu yang baru |

**Ringkas: sekitar setengah sampai dua-pertiga pemindahan Inventory & Sales**, dan tanpa menyentuh
kode yang melayani rumah sakit.

---

## Yang harus diputuskan bila Apotik dikerjakan

1. **Apotek mandiri atau farmasi rumah sakit?** 45 aksi vs 58. Bila resep dan antrean diikutkan,
   `Resep`/`ResepDetail`/`AntreanFarmasi` masuk v20 (+3 tabel, +19 kolom) — tetapi `Pasien` dan
   `Dokter` **tetap** tidak perlu, karena ikatannya nullable dan pembeli sudah dicatat sebagai teks.
2. **`item_medis` memakai ulang `{S}.produk`, atau tabel sendiri?** Memakai ulang berarti satu
   katalog barang untuk apotek dan non-apotek — konsisten, tetapi `default_permintaan` butuh rumah
   dan `satuan_id` jadi wajib. Doc 113 sudah mencatat selisih bentuknya.
3. **Register narkotika.** Ini kewajiban regulasi dengan aturan retensi dan pelaporannya sendiri.
   Perlu dipastikan `{S}` adalah tempat yang benar, atau ia justru harus terpusat.

---

## Batas analisis ini

- Belum ada yang dijalankan. Tak satu baris kode Apotik dieksekusi terhadap schema tenant.
- Hitungan aksi berasal dari literal `"apotik_*"` di sumbernya; aksi yang namanya dirakit saat
  jalan tidak terhitung. Beberapa entri berakhiran `_` menandakan nama yang dirakit — jadi 64 adalah
  **perkiraan**, bukan pembilangan pasti.
- Penggolongan klinis/inti/demo memakai kata kunci pada nama aksi, bukan pembacaan tiap aksi.
- Kesetaraan konsep (`item_medis`↔`produk`, `kadaluarsa`↔`produk_batch`) **belum** dibandingkan
  kolom demi kolom kecuali `item_medis` di doc 113.
- Taksiran "setengah sampai dua-pertiga" berdasar rasio jumlah aksi (45/83) dan cakupan katalog,
  bukan pengukuran waktu.
