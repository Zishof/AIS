# Penjaga hapus riwayat ternyata kurang lengkap

Batch lanjutan sesudah doc 78. Isinya satu perbaikan — atas perbaikan sendiri.

---

## 1. Apa yang kurang

Doc 74 §1 menambahkan penjagaan sebelum `NewUiJournalService.unpost` menghapus
`PostingHistory`: hapus hanya bila tidak ada lagi `GrupTransaksi` maupun `Transaksi` yang
merujuknya. Itu memperbaiki kegagalan FK pada dokumen hasil posting massal, dan pola itu
disalin dari `PostingTransaksiHarianAction.batalkanPostingSemua`.

**Dua perujuk itu bukan seluruhnya.** Penelusuran seluruh model menemukan:

| | |
|---|---|
| kelas entitas dengan FK ke `akunting.posting_history` | **64** |
| kolom FK (beberapa entitas punya lebih dari satu) | **74** |

Di antaranya `KasKecil`, `KasBesar`, `UangMuka`, `Pertangungjawaban` (tiga kolom sekaligus:
biasa, pajak, pengembalian), `Tagihan`, `PembayaranGaji`, `StokOpname`, `ReturPembelian`,
`PenyusutanAsset`, dan puluhan lainnya.

## 2. Kenapa itu bisa terkena

Mesin posting per modul menempelkan **satu** riwayat ke dokumen sumbernya sekaligus ke
jurnal hasilnya. Terverifikasi di `PostingKasKecilAction`:

```java
kasKecil.setPostingHistory(postingHistory);   // dokumen sumber
...
CommonAkunting.saveTransaksi(..., postingHistory, ...);   // jurnal + barisnya
```

Jadi bila jurnal hasil posting Kas Kecil dibatalkan lewat layar jurnal New UI:

1. cap dilepas dari `GrupTransaksi` dan seluruh baris `Transaksi`-nya;
2. penjaga menghitung perujuk — nol, karena ia hanya tahu dua tabel itu;
3. riwayatnya dihapus;
4. `kas_kecil.posting_history` masih memegangnya → **FK gagal**;
5. karena satu transaksi, **seluruh pembatalan ikut batal**.

Persis kegagalan yang doc 74 ingin hentikan, hanya datang dari sisi yang tidak terlihat
dari layar jurnal.

## 3. Perbaikannya: pakai pembeda yang sudah ada di data

Menghitung 64 entitas satu per satu tidak masuk akal — rapuh, dan setiap entitas baru akan
melubanginya lagi. Tidak perlu: pembedanya sudah ada.

Hanya tiga jalur yang membuat riwayat berjenis `PostingHistory.JENIS_UMUM` —
`NewUiJournalService.post`, `GrupTransaksiAction`, dan `PostingTransaksiHarianAction` —
dan ketiganya jurnal umum, yang menempelkan riwayatnya **hanya** ke `GrupTransaksi`
beserta baris `Transaksi`-nya. Riwayat berjenis lain (`JENIS_PENGGUNAAN_KAS_KECIL`,
`JENIS_PENGGANTIAN_KAS_KECIL`, `JENIS_PENGGUNAAN_KAS_BESAR`, dan seterusnya) selalu lahir
dari mesin per modul, dan dokumen sumbernya selalu ikut memegangnya.

```java
private static boolean bolehHapusRiwayat(Session s, PostingHistory p) {
    return PostingHistory.JENIS_UMUM.equals(p.getJenis()) && !stillReferenced(s, p);
}
```

> **Menyusul (doc 80):** melepas cap tanpa menghapus riwayat memang menghentikan
> kegagalan FK, tetapi membuat pembatalan BERHASIL separuh -- jurnalnya batal sementara
> dokumen sumbernya tetap mengaku terposting. Sejak r83096 `unpost` menolak riwayat
> non-UMUM sama sekali. Lihat
> [80-layar-jurnal-menyentuh-dokumen-modul.md](80-layar-jurnal-menyentuh-dokumen-modul.md).

Untuk riwayat berjenis lain, capnya tetap dilepas — hanya penghapusan barisnya yang tidak
dilakukan. Membiarkan satu baris riwayat yatim jauh lebih murah daripada menggagalkan
pembatalan yang sah.

Konstanta `JENIS_UMUM` juga ada di beberapa kelas `sirs/` — itu milik kelas lain dan tidak
berhubungan; yang dipakai di sini `PostingHistory.JENIS_UMUM`. Commit r83092.

## 4. `batalkanPostingSemua` sengaja tidak diubah

`PostingTransaksiHarianAction.batalkanPostingSemua` memakai penjagaan dua-perujuk yang
sama, dan **tetap dibiarkan**: ia menyaring lewat `kriteriaJurnalUmumStatic`, sehingga
hanya menyentuh jurnal umum. Asumsi dua perujuk memang berlaku di sana. Yang berbeda pada
`unpost` New UI adalah ia dapat dipanggil atas dokumen terposting jenis apa pun.

## 5. Situs penghapusan riwayat lain: diperiksa, semuanya sah

Seluruh basis kode disapu untuk penghapusan `PostingHistory` lain. Yang ditemukan —
`PostingPembayaranTerminAction`, `PostingPembayaranDpAction`,
`PostingPerjanjianKerjasamaAction`, dan sejenisnya — semuanya berbentuk:

```java
if (n == 0) {
    // Tidak satu dokumen pun terjurnal: riwayat kosong tidak ditinggalkan.
    session.delete(postingHistory);
}
```

Menghapus riwayat yang baru dibuatnya sendiri ketika tak ada dokumen yang berhasil dicap.
Tidak ada perujuk, jadi aman. Tidak ada yang diubah di sana.

## 6. Catatan untuk diri sendiri

Perbaikan doc 74 lolos kompilasi, punya pola acuan di repositori yang sama, dan
menyelesaikan kasus yang dilaporkan. Ia tetap kurang lengkap — karena pertanyaan
"siapa lagi yang merujuk tabel ini?" tidak pernah diajukan; yang diajukan hanya "bagaimana
kode di sebelah menyelesaikannya?".

Menyalin pola yang benar dari tetangga adalah kebiasaan bagus, tetapi pola itu membawa
serta **asumsi lingkupnya**. Di sini asumsinya "dokumen ini pasti jurnal umum" — benar di
tempat asalnya, tidak benar di tempat tujuannya.
