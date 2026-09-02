# Riwayat posting bersama, dan tri-state yang dipipihkan di payload

Dua perbaikan yang berdiri sendiri, dikerjakan berurutan. Keduanya berjenis sama:
**kode mengenal dua keadaan padahal datanya punya tiga (atau dipakai bersama).**

---

## 1. `unpost` per-dokumen merusak pembatalan dokumen hasil posting massal

`NewUiJournalService.unpost(id)` melepas cap posting lalu menghapus `PostingHistory`
tanpa memeriksa apakah riwayat itu masih dirujuk dokumen lain:

```java
s.flush(); s.delete(p); s.flush();
```

Posting massal — tombol ZK lama maupun jalur baru — memberi **satu riwayat kepada
banyak dokumen**. Jadi membatalkan satu dokumen hasil posting massal melanggar foreign
key; dan karena seluruh pembatalan berjalan dalam satu transaksi, **pembatalan dokumen
itu ikut batal seluruhnya**. Dokumen tetap tercap terposting, tanpa jalan keluar sampai
kebetulan tinggal satu perujuk.

`unpostBatch` memanggil `unpost` dalam gelung, sehingga pembatalan borongan pun rusak
untuk semua dokumen kecuali yang terakhir.

Cacat ini sudah dikenali saat doc 53 ditulis dan dicatat di sana sebagai "belum
diperbaiki karena di luar lingkup".

### Perbaikan

Menyeragamkan dengan pola yang sudah benar di
`PostingTransaksiHarianAction.batalkanPostingSemua` (baris 1090-an): lepas cap dulu,
flush, lalu hapus riwayat **hanya bila** tidak ada lagi `grup_transaksi` maupun
`transaksi` yang merujuknya.

```java
s.flush(); if(!stillReferenced(s,p)) s.delete(p); s.flush();
```

Dua pembantu privat ditambahkan; `stillReferenced` diberi JavaDoc yang menyebut alasannya
dan syarat pemanggilannya (**sesudah** cap dilepas dan di-flush, supaya hitungannya
melihat keadaan terbaru).

> **Menyusul (doc 79): penjagaan di bawah ini KURANG LENGKAP.** Ia hanya menghitung
> `GrupTransaksi` dan `Transaksi`, padahal ada 64 entitas dengan FK ke `posting_history`;
> riwayat dari mesin posting per modul juga dipegang dokumen sumbernya. Dipertegas di
> r83092 dengan syarat jenis riwayat -- lihat
> [79-penjaga-hapus-riwayat-kurang-lengkap.md](79-penjaga-hapus-riwayat-kurang-lengkap.md).

Riwayat yang masih dipakai kini bertahan — sama persis dengan perilaku yang sudah diuji
di harness posting jurnal umum skenario E/F (dua dokumen berbagi riwayat, hanya satu
dibatalkan → riwayat bertahan).

---

## 2. `PosApi` memipihkan `izinkanJualMinusStok` menjadi boolean

Lanjutan langsung dari doc 73. Kolom `Produk.izinkanJualMinusStok` bernilai TIGA:

| Nilai | Label di master Produk |
|---|---|
| `null` | Ikut Pengaturan Toko (default) |
| `TRUE` | Selalu Boleh Dijual Walau Stok Minus |
| `FALSE` | Wajib Diblokir Jika Stok Tidak Cukup |

Payload katalog `PosApi` mengirimnya begini:

```java
j.put("izinkanJualMinusStok", Boolean.TRUE.equals(p.getIzinkanJualMinusStok()));
```

`null` dan `FALSE` sama-sama menjadi `false` — tak terbedakan di sisi klien.

Yang membuatnya berbahaya adalah **kegunaan field itu sendiri**, seperti tertulis di
komentar di atasnya: field ini dikirim supaya form Ubah Produk (Desktop dan Android)
dapat mengisi ulang toggle-nya, sebab sebelumnya field tak pernah dikirim sehingga form
"diam-diam mereset nilai asli tiap produk disimpan ulang". Perbaikan itu belum tuntas:
memipihkannya membuat produk ber-default (`null`) tampil sebagai **"dikunci admin"**, dan
begitu form disimpan nilainya **tersimpan sebagai `FALSE`**.

Artinya jalur ini adalah sumber nyata keluhan yang dilaporkan doc 73 — produk diblokir
keras dengan pesan "dikunci admin" padahal tidak pernah dikunci siapa pun.

### Perbaikan

Dikirim apa adanya, mengikuti konvensi yang sudah dipakai `KantinHelper` saat mengirim
tri-state pengaturan toko (`null` → `JSONObject.NULL`, dgn komentar "Tri-state dikirim
APA ADANYA supaya layar ..."):

```java
j.put("izinkanJualMinusStok", p.getIzinkanJualMinusStok() == null
        ? JSONObject.NULL : p.getIzinkanJualMinusStok());
```

---

## 3. Hasil sapuan tri-state di seluruh basis kode

Doc 73 menutup dengan aturan: kolom tri-state tidak boleh dibaca inline. Basis kode
disapu untuk mencari pelanggaran lain. Caranya bukan mencari pola `!Boolean.TRUE.equals(`
begitu saja — pola itu **sah** untuk kolom dua-nilai dan muncul 169 kali. Yang dicari
adalah **ketidakkonsistenan**: field yang di satu tempat diuji `!= null` (bukti `null`
bermakna sendiri) tetapi di tempat lain dilipat.

Irisan kedua himpunan menyisakan 16 nama, delapan di antaranya benar-benar getter
`Boolean` nullable pada entitas: `getAktif`, `getAktifkanmanual`, `getBukanTagihan`,
`getDitolak`, `getIsWithdrawn`, `getLunas`, `getManual`, `getPersetujuan`.

**Kedelapannya diperiksa satu per satu dan tidak ada yang cacat.** Semuanya bermakna
"belum/tidak", sehingga `null` memang setara `false`: belum disetujui bukan berarti
ditolak, belum lunas tetap belum lunas. Melipatnya justru benar dan aman (fail-closed).

Dua field tri-state yang terdokumentasi — `Toko.getOtomatisBayarSetelahJam24()` dan
`getOtomatisLayaniSetelahJam24()` — juga diperiksa: semua situs bacanya menguji `null`
lebih dulu. Sudah benar.

Jadi sapuan ini menghasilkan **satu** perbaikan (butir 2 di atas), bukan daftar panjang.
Itu hasil yang sah dan layak dicatat: pola berisiko yang muncul 169 kali ternyata hampir
selalu dipakai pada tempat yang tepat.

---

## 4. Verifikasi

- `javac -source 1.7 -target 1.7 -encoding UTF-8 -sourcepath . -cp webapp/WEB-INF/lib/*`
  atas kedua berkas: **EXIT=0**.
- Isi kedua perbaikan dipastikan utuh di HEAD lewat `svn cat -r HEAD`.

## 5. Catatan: commit tersapu sesi lain, dua kali

Kedua perbaikan ini **tidak** masuk lewat commit yang disiapkan untuknya. Keduanya
tersapu sesi paralel yang menjalankan commit borongan berulang:

| Revisi | Isi | Pesan |
|---|---|---|
| r82930 | perbaikan butir 1 (bersama 8 berkas sesi lain) | kosong |
| r82955 | perbaikan butir 2 | kosong |

Jarak dari suntingan ke commit sapuan hanya ~50 detik, lebih cepat daripada kompilasi
verifikasi. Akibatnya: kode yang benar masuk repositori **tanpa satu baris pun
penjelasan** — dan dokumen inilah satu-satunya tempat rasionalnya tercatat.

Ini perluasan dari doc 07 §6 ("working copy dipakai bersama"): di sana bahayanya adalah
menyapu perubahan orang lain, di sini bahayanya adalah **riwayat kehilangan pesan**.
Selama penyapu itu berjalan, commit per berkas tidak cukup — tidak ada jendela waktu
untuk sempat memakainya.
