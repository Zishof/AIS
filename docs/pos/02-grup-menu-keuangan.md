# 02 — Grup menu "Keuangan"

Grup menu baru berisi enam modul yang dipindahkan dari layar ZK, ditambah dua menu yang
dipindahkan masuk dari grup "Pengadaan".

| Submenu | Sumber ZK | API helper |
|---|---|---|
| Uang Muka (Cash Advance) | `UangMukaAction` | `UangMukaApiHelper` |
| Pertanggungjawaban Uang Muka | `PertangungjawabanAction` | `PertangungjawabanApiHelper` |
| Kas Besar | `KasBesarAction` | `KasBesarApiHelper` |
| Pertanggungjawaban Kas Besar | `PertangungjawabanKasBesarAction` | `PertangungjawabanKasBesarApiHelper` |
| Kas Kecil | `KasKecilAction` | `KasKecilApiHelper` |
| Penggantian Kas Kecil (Reimbursement) | `PenggantianKasKecilAction` | `PenggantianKasKecilApiHelper` |

Menu **Pajak** dan **Pembayaran Tagihan** dipindahkan dari grup Pengadaan ke grup ini.

Keenam kunci menu (`uang_muka`, `pj_uang_muka`, `kas_besar`, `pj_kas_besar`,
`kas_kecil`, `penggantian_kas_kecil`) terdaftar di `EbisnisMenuKatalog` pada `DAFTAR`,
`KUNCI_DEFAULT_NONAKTIF`, `KUNCI_AKUNTANSI`, dan `KUNCI_CRUD` — jadi hak ADD/Edit/
Delete/Approve/Reject-nya dapat diatur per peran lewat `TbmroleAction`.

---

## 1. Aturan bisnis yang dipertahankan apa adanya

Urutan validasi sengaja disamakan dengan layar ZK, termasuk urutan yang terasa
tidak intuitif, supaya pesan yang dilihat pengguna sama di semua kanal.

**Uang Muka** — Satuan Kerja (bila tanpa anggaran atau dari PR) → Judul → Akun (bila
tanpa anggaran) → Anggaran → Tanggal Mulai → Sampai → Laporan → Nilai. Pemeriksaan sisa
saldo memakai `JenisUangMukaAction.hitungSaldo` dan hanya berjalan bila konfigurasi
`saldo_harus_cukup_sebelum_mengajukan_realisasi_anggaran` menyala.

**Pertanggungjawaban** — nilai per baris rincian dihitung:

```
pajakBaris = persenJenisPajakBarang / 100 * jumlah
totalBaris = (jumlah + ppn/100 * jumlah) - (pphMengurangi ? pajakBaris : 0)
```

Dokumen ditolak bila `(long) nilaiUangMuka < (long) nilaiLpj` — perbandingan **dibulatkan**,
persis seperti ZK.

**Pertanggungjawaban Kas Besar** memakai rumus yang sama, tetapi perbandingannya
**pecahan** (`nilaiKasBesar < nilaiLpj`), bukan dibulatkan. Perbedaan ini disengaja: ia
mengikuti masing-masing layar ZK, bukan salah ketik.

**Kas Kecil** — tiap baris wajib berakun dan jumlahnya tidak boleh nol; nilai tidak boleh
melebihi saldo pada tanggal laporan (`JenisKasKecilAction.hitungSaldo`); dan satu jenis
kas kecil hanya boleh punya satu dokumen yang belum disetujui — dokumen yang menggantung
disebutkan kodenya.

**Penggantian Kas Kecil** memilih satu **dokumen** kas kecil (bukan jenisnya) yang sudah
disetujui dan belum pernah diganti. Rincian dokumen induk itulah yang disunting di sini,
dan saat disimpan `formula` serta `nilai` kas kecil induknya ikut diperbarui, lalu
ditautkan balik lewat `kk.setPenggantianKasKecil(pg)`.

---

## 2. Penomoran dokumen

Kode dokumen dibuat memakai `NomorSuratAlurKeuangan.<X>_DATA` + `NomorSurat.format(index,
tanggal)` + `KodeUnikUtil.pastikanUnik`. Aturan reset (per nomor / kelompok / tahun /
bulan / `resetTiap`) direplikasi pada `indexBerikutnya(session, ns)`.

---

## 3. Dasbor dan cetak

Setiap submenu Keuangan punya tab **Dasbor** dan tombol **Cetak**, sama seperti submenu
Pengadaan — dan memakai widget yang sama, bukan salinannya.

`PengadaanDasborTab` dibuat dapat diatur (`aksi`, `namaParam`) sehingga grup Keuangan
memanggilnya dengan `aksi: 'keuangan_dasbor'` dan `namaParam: 'modul'`.

Kontrak muatan dasbor (dipakai bersama Pengadaan):

```
kpi[{label, nilai}]
tren[{label, nilai, jumlah}]           + trenJudul
komposisi[{label, nilai}]              + komposisiJudul
peringkat[...]                         + peringkatJudul
daftar[{kode, keterangan, umurHari}]   + daftarJudul
catatanKosong
```

Sisi server: `KeuanganApiHelper` — aksi `keuangan_dasbor` (bercabang per modul) dan
`keuangan_cetak` (mengembalikan PDF base64, ditampilkan `tampilkanPratinjauPdf`, jendela
pratinjau yang sama dengan `cetakDokumenPengadaan`). Templat Jasper ada di
`webapp/report/akunting/*.jasper`.

> **Belum terverifikasi:** `keuangan_cetak` belum pernah dijalankan di aplikasi
> terpasang. Harness tidak punya konteks servlet untuk menemukan jalur templat Jasper,
> jadi jalur cetaknya perlu sekali dicoba di lingkungan nyata.

---

## 4. Lokal-dulu dan hapus lunak

Seluruh modul Keuangan menulis ke perangkat lebih dulu, baru dikirim ke server —
berlaku untuk create, edit, maupun delete.

Mekanismenya dipakai bersama lewat `MasterOffline`:

| Fungsi | Guna |
|---|---|
| `MasterOffline.daftarCacheDulu(aksi, body, cacheKey, onData:, kolomKunci:)` | membaca salinan lokal lebih dulu |
| `prosesSimpanMaster(context, aksi:, body:, kunci:, cacheKey:, rowLokal:, hapusLokal:, idLokal:, entitas:)` | menulis lokal-dulu lalu mengantre ke server |
| `MasterOffline.idSementaraBaru()` | id sementara untuk baris yang lahir saat luring |
| `MasterOffline.pulihkanLokal(cacheKey, id, kunci:)` | membatalkan penghapusan |
| `MasterOffline.daftarTerhapusLokal(cacheKey)` | melihat yang ditandai terhapus |

**Penghapusan di perangkat bersifat lunak**: baris ditandai `_dihapus` /`_dihapusPada`
dan disaring dari daftar, tidak dibuang. Di server penghapusannya sungguhan — riwayatnya
sudah dipegang audit trail server (Envers, skema `new_audit`, sufiks `__audit`,
`store_data_at_delete=true`).

> **Jebakan yang pernah terjadi:** penandaan hapus lunak per layar TIDAK cukup. Versi
> pertama menandai baris di layar tetapi **tidak membatalkan perintah hapus yang masih
> mengantre** — barisnya muncul lagi secara lokal, tetapi tetap terhapus di server begitu
> jaringan pulih. `MasterOffline.pulihkanLokal` membatalkan keduanya sekaligus; itulah
> sebabnya seluruh layar memakainya, bukan penanda sendiri-sendiri.
