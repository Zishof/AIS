# Satu klik membalik status se-batch, dan kompilasi penuh sebagai alat

Batch lanjutan sesudah doc 80.

---

## 1. Temuan: `setPostingActive` bekerja pada riwayat, bukan pada jurnal

Layar Jurnal punya aksi "posting aktif/nonaktif" per baris. Yang dilakukannya:

```java
GrupTransaksi g = require(s, id);
PostingHistory p = g.getPostingHistory();
p.setPosting(value);
```

Ia mengubah **riwayat posting**, bukan jurnalnya. Itu penting karena riwayat dipakai
bersama: `PostingTransaksiHarianAction.postingSemua` membuat **satu** `PostingHistory`
(baris 959) sebelum gelung atas seluruh id yang terkumpul (baris 948). Seluruh dokumen
dalam satu posting massal memakai riwayat yang sama.

Akibatnya: menonaktifkan posting pada **satu baris** membalik status **seluruh dokumen
dalam batch itu**. Layarnya bekerja per baris, datanya bekerja per batch.

Bendera `posting` ini bukan hiasan — doc 53 mencatat bahwa `posting_history.posting`
menentukan sebuah dokumen terhitung terposting atau kembali menjadi draf di dasbor.

Aksinya juga tanpa pengaman: di controller hanya `csrf(q)` lalu langsung dijalankan —
tidak ada konfirmasi, tidak ada penyebutan berapa dokumen yang ikut terpengaruh. Bandingkan
dengan `deleteAll` dan `cleanDuplicates` di layar yang sama, yang menuntut konfirmasi ketik.

### Tidak diperbaiki sepihak

Sebagian dari perilaku ini mungkin memang dikehendaki: menonaktifkan satu batch posting
sekaligus adalah operasi yang masuk akal. Yang jelas keliru bukan efeknya, melainkan bahwa
efek itu **tidak terlihat** dari layar maupun dari balasan API.

Bahan untuk memperbaikinya sudah tersedia: penyaring `postingId` sudah terekspos ke klien,
sehingga daftar dokumen yang seposting dapat ditampilkan sebelum aksi dijalankan. Tiga
pilihan, urut dari yang paling ringan:

1. Balasan API menyertakan jumlah dokumen yang berbagi riwayat itu — aditif, klien lama
   tidak terpengaruh.
2. Layar menampilkan "N dokumen memakai posting ini" sebelum tombolnya ditekan.
3. Konfirmasi ketik bila N > 1, memakai mekanisme `confirm` yang sudah ada.

Pilihan mana yang tepat bergantung pada apakah pembalikan se-batch memang alur kerja yang
dipakai. Itu keputusan pemilik sistem.

`activateBatch` memanggil `setPostingActive` dalam gelung atas banyak id, sehingga riwayat
yang sama disetel berulang kali. Mubazir, tetapi tidak merusak: hasilnya sama.

## 2. Sapuan SQL mentah di paket `newui`: bersih

Seluruh 107 berkas paket `ais.common.newui` disapu untuk `createSQLQuery` yang mengubah
data. Yang ditemukan hanya dua, keduanya di `NewUiJournalService` dan keduanya sudah
dibahas di doc 80 (`deleteAll` dan `cleanDuplicates`). Tidak ada jalur tulis mentah lain
yang memintas lapisan entitas.

Hasil negatif, dan layak dicatat justru karena itu: kecurigaan bahwa pola "menulis lintas
modul lewat SQL mentah" tersebar luas di New UI ternyata tidak berdasar.
