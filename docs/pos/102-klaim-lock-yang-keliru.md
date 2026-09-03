# 102 — Klaim lock yang keliru, dan pertanyaan yang menjawab dirinya

Tanggal: 2026-09-03

Dok. 101 menggugurkan penyumbat basis data. Itu memaksa membaca ulang butir A.5
pada register — `hanya_perubahan` — yang selama ini bersandar pada dua alasan.
Keduanya ternyata tidak berdiri.

## 1. Klaim yang salah, dan saya yang menulisnya

Sejak dok. 79 tertulis, dan disalin ke `alat/payload-tanpa-pembaca.py` serta
register dok. 97:

> Akibatnya dua: hitungan "diperbarui" pada ringkasan impor ikut menghitung baris
> yang tidak berubah, **dan tiap baris tetap menahan lock `koperasi.produk`
> sehingga memperbesar permukaan deadlock** yang sudah punya mekanisme
> percobaan-ulang tersendiri.

Bagian yang ditebalkan itu salah. Yang dibaca ulang:

* Produk diambil `session.createCriteria(Produk.class)...list()` — **managed**,
  bukan detached. Hibernate melakukan dirty-check saat flush dan hanya
  menerbitkan `UPDATE` untuk entitas yang nilainya benar-benar berubah.
  `session.update(p)` atas entitas managed tidak memaksa penulisan.
* Method impornya menyetel **sepuluh** field, seluruhnya nilai dari baris Excel.
  Tidak ada satu pun timestamp atau kolom audit yang diset tanpa syarat —
  diperiksa, nol kemunculan.
* `AuditTimestampInterceptor` bereaksi lewat `onFlushDirty`, yang menurut
  namanya sendiri hanya dipanggil untuk entitas yang **sudah** dinyatakan kotor.
  Ia tidak dapat mengotori entitas yang bersih.

Jadi baris yang nilainya sama tidak menghasilkan `UPDATE`, dan karena itu tidak
mengambil lock. Permukaan deadlock tidak bertambah.

Yang benar-benar tersisa **tinggal satu**: ringkasan impor melaporkan
"diperbarui" untuk baris yang tidak berubah. Itu laporan yang keliru, bukan
risiko basis data.

## 2. Pertanyaan yang ternyata menjawab dirinya

Alasan kedua terdengar lebih kokoh:

> Memutuskan "apa artinya tidak berubah" adalah keputusan tersendiri:
> perbandingan field yang terlalu longgar akan MELEWATI perubahan yang sah, dan
> itu kehilangan data yang sunyi.

Kekhawatiran itu benar untuk perbandingan yang dipilih sembarang. Ia tidak
berlaku di sini, karena himpunan fieldnya bukan pilihan: impor ini menulis
**persis sepuluh** field — `kode`, `nama`, `barcode`, `hargaBeli`, `hargaJual`,
`jenisProduk`, `pemasok`, `satuan`, `toko`, `kunciUnik`.

Perbandingan yang mencakup persis field-field itu **tidak mungkin melewatkan
perubahan yang sah**, karena tidak ada field lain yang ditulis. "Apa artinya
tidak berubah" jawabannya sudah ditentukan oleh kodenya sendiri: tidak ada satu
pun dari sepuluh field itu yang berbeda.

Bentuknya sama dengan dok. 94, tempat "satu persetujuan per batch atau per
baris?" gugur begitu perbaikannya dipindahkan ke sebelum pengiriman. Pertanyaan
yang tampak menuntut keputusan produk kadang hanya menunggu dibaca lebih teliti.

## 3. Ukuran butirnya sekarang

| | Sebelum | Sesudah dibaca ulang |
|---|---|---|
| Akibat | laporan keliru **+ risiko deadlock** | laporan keliru saja |
| Penyumbat 1 | "tidak ada harness DB yang dapat dijalankan" | gugur (dok. 101) |
| Penyumbat 2 | "apa artinya tidak berubah" | terjawab oleh kodenya sendiri |

Butir A.5 karena itu bukan lagi keputusan produk yang menunggu Anda. Ia
pekerjaan biasa: bandingkan sepuluh field sebelum menyetelnya, dan hitung
"diperbarui" hanya bila ada yang berbeda.

Yang belum saya kerjakan di batch ini adalah kodenya. Jalur itu menulis data
master produk, dan perbaikan yang benar berpasangan dengan harness basis data
yang membuktikannya — sekarang mungkin dilakukan, tetapi menuntut batch
tersendiri, bukan tempelan di akhir batch koreksi.

## 4. Yang dipelajari

**Klaim yang salah menyebar lebih cepat daripada klaim yang benar diperiksa.**
Satu kalimat di dok. 79 disalin ke komentar sebuah alat dan ke register
keputusan, lalu dipakai sebagai separuh alasan untuk tidak mengerjakan sesuatu
selama berminggu-minggu. Tidak ada yang jahat di situ — hanya tidak ada yang
menuntut pembuktiannya.

**Dua penyumbat yang berdiri bersama saling menopang.** Selama "tidak dapat
diuji" berdiri, tidak ada gunanya memeriksa "apa artinya tidak berubah"; selama
pertanyaan itu terbuka, tidak ada gunanya mengejar cara mengujinya. Begitu satu
runtuh (dok. 101), yang lain langsung terlihat rapuh. Alasan yang berpasangan
layak diperiksa satu per satu, bukan sebagai paket.
