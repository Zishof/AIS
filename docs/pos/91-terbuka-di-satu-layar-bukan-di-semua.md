# 91 — Terbuka di satu layar bukan berarti terbuka di semua

Tanggal: 2026-09-02

Penjaga dok. 87 bertanya: **"adakah klien yang mengirim kunci ini?"** Kalau ada,
pintunya dinyatakan dapat dibuka.

Pertanyaan itu terlalu longgar. Pengguna tidak berdiri di "salah satu klien"; ia
berdiri di **satu layar tertentu**. Pintu yang terbuka di layar Produk tidak
menolong siapa pun yang menabraknya dari layar Kulakan.

## 1. Pertanyaan yang benar

Bukan "adakah yang mengirim kuncinya", melainkan:

> Apakah **setiap jalur klien yang dapat menabrak pintu ini** juga dapat
> membukanya?

Rantainya dapat ditelusuri dari sumber:

1. Pintunya berada di dalam sebuah method (`produkSimpan`).
2. Dispatcher memetakan sebuah aksi ke method itu
   (`"produk_simpan".equals(action)` → `KantinHelper.produkSimpan`).
3. Berkas klien yang menyebut aksi itu adalah jalur yang dapat menabraknya.
4. Tiap jalur harus juga menyebut kunci pembukanya.

## 2. Hasilnya: satu utang, dua tuduhan salah

Pertanyaan baru langsung menemukan tiga jalur — dan **hanya satu** yang benar:

| Jalur | Vonis |
|---|---|
| `kulakan_bulk_entry_screen.dart` | **benar buntu** — mengirim `harga_beli: row.hppUnit` |
| `kasir_screen.dart` | tuduhan salah — mengirim `'harga_beli': 0` yang dipatok |
| `core_db.dart` | tuduhan salah — namanya cuma filter SQL `where` atas `outbox_master` |

Gerbangnya berbunyi `hargaModal > hargaJual * 10`. Dengan harga modal nol, layar
Kasir tidak pernah dapat memicunya. Dan `core_db.dart` tidak pernah mengirim
permintaan apa pun — ia memulihkan cache dari antrean lokal.

Dua tuduhan dari tiga. Kalau alat ini langsung dipercaya, ia akan menyuruh
menambal dua layar yang sama sekali tidak bermasalah.

## 3. Dua daftar, bukan satu

Karena itu ada **dua** daftar yang bentuknya mirip tetapi maknanya berlawanan:

* `UTANG_JALUR` — dapat memicu, sengaja **belum** diperbaiki, dengan sebabnya.
  Isinya satu: entri massal Kulakan, karena "satu persetujuan untuk seluruh batch
  atau per baris" adalah keputusan produk, dan menebaknya di jalur yang menulis
  banyak baris sekaligus lebih berbahaya daripada membiarkannya.

* `TAK_MEMICU` — **tidak dapat** memicu, jadi tidak ada yang perlu diperbaiki,
  dengan alasan yang dapat diperiksa ulang.

Menggabungkan keduanya menjadi satu daftar "pengecualian" akan menghapus
perbedaan yang justru paling penting: yang satu pekerjaan tertunda, yang lain
tuduhan yang salah. Tiap baris menyebut alasannya, karena pembebasan tanpa
alasan adalah cara termudah membuat sebuah penjaga berbohong dengan sopan.

Keduanya hanya boleh menyusut, dan alat ini melapor bila sebuah entri sudah
tidak berlaku lagi.

## 4. Koreksi atas dok. 86

Dok. 86 paragraf 5 menyebut `kasir_screen` dan `kulakan_bulk_entry` sekaligus
sebagai layar yang "masih menampilkan penolakan tanpa menawarkan persetujuan".
Untuk `kasir_screen` itu salah — ia tidak dapat menabraknya sama sekali.

Kekeliruannya berasal dari pengelompokan yang salah: keduanya saya samakan
karena sama-sama "layar lain yang menyimpan produk". Yang menentukan bukan itu,
melainkan **apakah layar itu mengirim harga modal sungguhan.** Dok. 86 sudah
dikoreksi di tempatnya.

## 5. Dibuktikan bisa gagal

Kontrol negatifnya memakai jalur yang sungguh ada: baris pengirim dicabut dari
`produk_screen.dart`.

```
== Jalur klien yang bisa menabrak pintu tetapi tidak bisa membukanya ==
   - produk_screen.dart  lewat aksi produk_simpan, tanpa izin_harga_modal_tinggi

1 PINTU BUNTU -- pesannya menjanjikan yang mustahil
1 JALUR BUNTU -- layarnya bisa menabrak, tidak bisa membuka
```

Kedua tingkat menyala bersamaan, seperti seharusnya. Sisipan dikembalikan,
`git status` kosong, alatnya kembali `SELURUH PINTU DAPAT DIBUKA DARI SETIAP
JALURNYA`.

## 6. Batas yang tetap berdiri

Langkah 3 memakai "berkas klien yang menyebut nama aksinya" sebagai perkiraan
untuk "jalur yang dapat menabrak". Perkiraan itu **terlalu lebar** — dua dari
tiga temuan pertamanya adalah bukti langsungnya. Alat ini tidak tahu apakah
sebuah jalur benar-benar mengirim nilai yang memicu syaratnya; yang tahu adalah
orang yang membaca payloadnya, lalu menuliskan alasannya di `TAK_MEMICU`.

Itu pembagian kerja yang disengaja: alat menyempitkan dari ribuan berkas menjadi
tiga, manusia memutuskan tiga.

## 7. Yang dipelajari

**Kuantor dalam sebuah invarian menentukan apa yang sebenarnya dijaga.**
"ADA klien yang mengirim" dan "SETIAP jalur dapat mengirim" terdengar seperti
pemeriksaan yang sama dan berbeda jauh. Yang pertama hijau sepanjang satu layar
saja diperbaiki; pengguna di layar lain tetap buntu, dan penjaganya diam.

**Penjaga yang lebih tajam menghasilkan lebih banyak tuduhan salah.** Pertanyaan
lama tidak pernah salah menuduh — ia hanya jarang bertanya. Yang baru menemukan
utang nyata sekaligus dua tuduhan yang keliru. Ketajaman dibayar dengan
kewajiban memeriksa, dan hasil pemeriksaan itu harus tersimpan di dalam alatnya,
bukan di kepala orang yang kebetulan pernah memeriksanya.
