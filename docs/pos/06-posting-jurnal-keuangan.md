# 06 — Posting jurnal dokumen Keuangan

Layar Desktop/Android sudah menampilkan penanda "sudah dijurnal" dan mengunci dokumen
yang terposting, tetapi belum ada cara **menjurnalkannya** dari aplikasi. Modul ini
menutup lubang itu.

## 1. Pola yang dipakai

Mengikuti pola yang lebih dulu dipakai `PostingKasKecilAction` dan `PostingKasBesarAction`:
sepasang metode statis pada masing-masing `Posting*Action`, yang logikanya identik dengan
tombol "Posting Semua" / "Batalkan Posting Semua" di layarnya —

```java
public static int postingSemua(Date mulai, Date sampai, Tbmuser oleh, Date tglPosting)
public static int batalkanPostingSemua(Date mulai, Date sampai)
```

Keduanya dipanggil `DraftJurnalApiHelper` lewat aksi `draft_jurnal_posting` dan
`draft_jurnal_batal_posting`, dengan nama modul pada muatan.

Penulisan jurnalnya diserahkan ke `CommonAkunting.saveTransaksi` — mesin yang sama dengan
layar ZK, bukan tiruannya.

## 2. Pasangan akun per modul

| Modul | Debet | Kredit |
|---|---|---|
| Pertanggungjawaban Uang Muka | akun uang mukanya; selisih terhadap pengajuan masuk **akun kelebihan** | akun jenis uang muka; akun pajak per baris ikut dikreditkan |
| Pertanggungjawaban Kas Besar | akun anggaran pada rincian kas besarnya | akun penerima jenis kas besar; pajak per baris |
| Uang Muka | akun penerima (jenis uang muka) | akun cara pembayaran pada proses transfernya |
| Penggantian Kas Kecil | akun jenis kas kecil induknya | akun cara pembayaran pada proses transfernya |
| Kas Kecil | akun biaya tiap baris rincian (+ akun penutup bila dokumen penutupan) | akun jenis kas kecil |
| Kas Besar | akun penerima jenis kas besar | akun jenis kas besar |

Nilai LPJ **dihitung ulang** dari rinciannya saat posting (`jumlah + PPN% − PPh%` bila
konfigurasi `pph_mengurangi_lpj` menyala), karena rincian bisa berubah setelah dokumen
disetujui dan jurnal harus mengikuti isinya.

## 3. Baris dasbor Draft Jurnal

| Baris | Kunci hak | Mesin |
|---|---|---|
| Uang Muka | `uang_muka` | `PostingUangMukaAction` |
| Pertanggungjawaban Uang Muka | `pj_uang_muka` | `PostingPertangungjawabanAction` |
| Kas Kecil | `kas_kecil` | `PostingKasKecilAction` |
| Kas Besar | `kas_besar` | `PostingKasBesarAction` |
| Pertanggungjawaban Kas Besar | `pj_kas_besar` | `PostingPertangungjawabanKasBesarAction` |
| Penggantian Kas Kecil | `penggantian_kas_kecil` | `PostingPenggantianKasKecilAction` |

> **Penamaan sudah dibetulkan.** Baris dasbor yang dulu bernama "Uang Muka" sebenarnya
> berisi dokumen **Pertanggungjawaban** (kriterianya `kriteriaLpj`). Namanya kini
> **"Pertanggungjawaban Uang Muka"** dengan kunci `pj_uang_muka`, sehingga kunci
> `uang_muka` bebas dipakai baris Uang Muka yang sebenarnya — lengkap dengan mesin
> `PostingUangMukaAction` dan kriteria yang menyaring syarat proses transfernya.
>
> Sekaligus ditutup satu kelalaian: `pj_kas_besar` dan `penggantian_kas_kecil` sudah punya
> cabang dan kriteria, tetapi kuncinya belum pernah dimasukkan ke `DraftJurnalRingkasanUtil.KUNCI`
> — mesinnya jalan, tetapi barisnya tidak pernah ikut dirender `hitungSemua`, jadi
> tombolnya tidak pernah terlihat pengguna.

Sisi klien tidak perlu diubah: layar Draft Jurnal menampilkan tombol posting berdasarkan
bendera `bisaPosting` dari server, dan bendera itu dihitung dari terdaftarnya nama modul.

## 4. Ketergantungan pada Proses Transfer

Akun kredit **Uang Muka** dan **Penggantian Kas Kecil** diambil dari rantai
DPC → Proses Transfer → Cara Pembayaran (akun transitori bila pengajuannya transitori,
akun biasa bila transfer). Ini perilaku ZK, bukan batasan yang ditambahkan.

Karena layar Proses Transfer belum diport, dokumen yang lahir dari Desktop/Android baru
bisa dijurnalkan setelah transfernya diproses di aplikasi web. Syarat itu **ikut disaring
di kriteria dasbornya**, supaya angka pada dasbor tidak menjanjikan dokumen yang justru
dilewati mesin postingnya.

## 5. Hasil uji

| Harness | Hasil |
|---|---|
| `TesPostingLpj` | 13 lulus, 0 gagal — jurnal seimbang 1.000.000, selisih 200.000 ke akun kelebihan |
| `TesPostingPjKasBesar` | 10 lulus, 0 gagal — jurnal seimbang 600.000 |
| `TesPostingTransfer` | 10 lulus, 0 gagal — Uang Muka 750.000 dan Penggantian Kas Kecil 320.000, keduanya seimbang |
| `TesBatalKasKecil` | 6 lulus, 0 gagal — uji regresi pembatalan |

Seluruh harness membersihkan data ujinya sendiri; `TesPostingPjKasBesar` juga
mengembalikan `jenis_kas_besar.akun_penerima` ke nilai semula setelah meminjamnya.
