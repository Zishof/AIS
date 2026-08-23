# 03 — Penggunaan anggaran

## 1. Siapa yang sebenarnya menulis pemotongan anggaran

Di AIS, baris `rab.penggunaan_anggaran` **tidak ditulis oleh layarnya**. Setiap kali
entitas tersimpan lewat Hibernate, `AuditListener` memanggil
`PenggunaanAnggaran.simpan(serializable)`, yang beberapa detik kemudian — di thread
terpisah — membuat atau menyegarkan barisnya secara idempotent berdasarkan kolom `ref`.

Karena itu tugas API bukan menulis baris tersebut, melainkan **menyediakan data yang
dibutuhkannya**.

Entitas yang ditangani: `UangMuka`, `PermintaanPengadaanMasterAssetDetail`,
`SaldoAwalMasterAssetDetail`, `PembayaranGaji`, `KasKecil`, `KasBesar`, `GrupTransaksi`,
`Pertangungjawaban`.

## 2. Celah yang ditutup

`PenggunaanAnggaran.prosesKasKecil` dan `prosesKasBesar` membaca `formula` dokumen dan
**melewati begitu saja baris yang tidak membawa field `workspace`**:

```java
if (jsonObject.isNull("key") || jsonObject.isNull("workspace")) continue;
```

Layar ZK menutupnya dengan banbox anggaran **per baris rincian**. Layar Desktop/Android
sebelumnya hanya mengirim `akun` dan `jumlah`, sehingga Kas Kecil dan Kas Besar
**tidak pernah memotong anggaran sama sekali** — tanpa pesan galat apa pun.

## 3. Yang dikerjakan

`ais/action/servlet/api/AnggaranKeuanganUtil.java`:

| Metode | Guna |
|---|---|
| `cari(...)` | daftar anggaran yang boleh dipakai: aktif dan berupa daun (hanya daun yang memegang pagu) |
| `saldo(...)` | sisa anggaran pada satu tanggal, memakai `JenisUangMukaAction.hitungSaldo` |
| `lengkapiRincian(session, rincian, tanggal, carryOver)` | mengisi `workspace` tiap baris rincian |
| `pemakaianDokumen(session, kolom, id)` | ringkasan anggaran yang terpotong satu dokumen |
| `lepaskan(session, kolom, id)` | mengembalikan anggaran sebelum dokumennya dihapus |

**Urutan penebakan anggaran** (sama dengan ZK):

1. Bila baris sudah membawa `workspace`, itu yang dipakai — dan akun biayanya
   diselaraskan dengan akun milik anggaran tersebut.
2. Bila tidak, anggaran dicari dari akun biaya baris itu untuk tahun dokumen: lewat
   relasi akun dulu, lalu lewat kesamaan kode.
3. Bila tetap tidak ketemu, `workspace` dibiarkan kosong. Barisnya tetap tersimpan dan
   tetap dijurnal, hanya tidak memotong anggaran — perilaku ZK, bukan kegagalan simpan.

Kas Besar memakai `carryOver = true` karena juga menerima anggaran luncuran.

**Pelengkapan berjalan SEBELUM validasi rincian.** Di ZK, memilih anggaran pada banbox
langsung mengisi akun biayanya, sehingga saat Simpan ditekan barisnya sudah punya akun.
Di API, baris bisa datang hanya dengan anggaran — kalau pelengkapan berjalan belakangan,
baris itu keburu ditolak "akun belum dipilih".

## 4. Uang Muka dan Pertanggungjawaban

- **Uang Muka** memakai kolom `workspace` pada dokumennya sendiri.
- **Pertanggungjawaban** mewarisi anggaran dari uang muka induknya.
- **Uang muka berbasis PR** sengaja TIDAK memotong anggaran lagi — PR-nya sudah memotong.
  `createPenggunaanAnggaranSource` mengembalikan `null` bila
  `permintaanPengadaanMasterAssets` tidak kosong, dan malah **membersihkan** baris
  penggunaan yang mungkin tertinggal aktif (mis. UM dibuat tanpa PR lalu diubah menjadi
  berbasis PR) supaya anggaran tidak terpotong dua kali.

## 5. Pelepasan saat dokumen dihapus

FK dari `rab.penggunaan_anggaran` ke dokumennya **tidak memakai `ON DELETE CASCADE`**.
Tanpa melepas barisnya lebih dulu, basis data menolak penghapusan dokumen. Karena itu
`hapus` pada Uang Muka, Kas Kecil, Kas Besar, dan Pertanggungjawaban memanggil
`AnggaranKeuanganUtil.lepaskan(...)` di dalam transaksi yang sama.

## 6. Pelepasan saat dokumen ditolak

Tidak perlu ditangani API: `PenggunaanAnggaran.getAktif()` adalah getter **terhitung**.
Untuk uang muka ia mengembalikan `uangMuka.getAktif() && status != DITOLAK`, dan
perhitungan saldo menyaring `aktif` — jadi dokumen yang ditolak berhenti membebani
anggaran dengan sendirinya begitu barisnya disegarkan.

## 7. Sisi klien

`apps/ebisnis/lib/widgets/pemilih_anggaran.dart` — `PemilihAnggaranField`, dipakai pada
rincian Kas Kecil, Kas Besar, dan Penggantian Kas Kecil. Memilih anggaran akan mengisi
akun biayanya, sama seperti banbox ZK. Kas Besar sekaligus mendapat pemilih akun biaya
yang sebelumnya tidak ada sama sekali.

Field ini boleh dikosongkan — server tetap menebak dari akunnya. Ia berguna ketika satu
akun dipakai beberapa anggaran dan pengguna ingin menunjuk yang mana.
