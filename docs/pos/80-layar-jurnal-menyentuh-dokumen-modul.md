# Layar Jurnal menyentuh dokumen milik modul lain

Batch lanjutan sesudah doc 79. Dua perbaikan, dan dua temuan yang **sengaja tidak**
diperbaiki karena butuh keputusan pemilik sistem.

---

## 1. `unpost` menolak jurnal bikinan modul (r83096)

Doc 79 menutup kegagalan FK dengan tidak lagi menghapus riwayat berjenis non-UMUM. Itu
benar sejauh yang dituju, tetapi menimbulkan akibat yang lebih halus dan lebih buruk.

`unpost` hanya menyentuh `GrupTransaksi` beserta baris `Transaksi`-nya. Ia **tidak pernah**
menyentuh dokumen sumber. Pembatalan yang benar melakukan keduanya — `PostingKasKecilAction`
memanggil `kasKecil.setPostingHistory(null)`.

Maka:

| | sebelum r83092 | sesudah r83092 |
|---|---|---|
| batalkan jurnal Kas Kecil dari layar Jurnal | **gagal keras** (FK dari `kas_kecil`), semuanya rollback | **berhasil** |
| keadaan sesudahnya | jurnal tetap terposting — utuh | jurnal batal, `kas_kecil` tetap mengaku terposting |

Kegagalan yang berisik berubah menjadi ketidakcocokan yang senyap. Lebih jauh: jurnal yang
capnya sudah lepas itu kini lolos `guardEditable`, sehingga **dapat dihapus** — meninggalkan
dokumen sumber yang menunjuk riwayat posting untuk jurnal yang tidak ada lagi.

`unpost` kini menolak riwayat berjenis selain `JENIS_UMUM`, dengan pesan yang menyebut
jenis modulnya dan mengarahkan ke layar modul tersebut. Pembatalan jurnal umum tidak
berubah sama sekali.

Pelajarannya menyambung doc 79: memperbaiki gejala yang terlihat (FK gagal) tanpa bertanya
"apa yang terjadi kalau ini berhasil?" hanya memindahkan kerusakan ke tempat yang lebih
sulit dilihat.

## 2. `deleteAll` menyisakan baris jurnal yatim (r83101)

Operasi "hapus seluruh jurnal" hanya menjalankan penghapusan atas `akunting.grup_transaksi`.
`akunting.transaksi` tidak pernah disentuh, padahal `transaksi.grup_transaksi` dipetakan
`nullable = true` sehingga penghapusan grup tidak selalu ditolak basis data — yang tersisa
adalah baris jurnal yang menunjuk grup yang sudah tidak ada.

`MaintenanceResult` punya field `lines` dan controller melaporkannya ke layar, tetapi
`deleteAll` tidak pernah mengisinya. Layar selalu menampilkan nol baris terhapus: kebetulan
jujur, karena baris memang tidak dihapus, tetapi menyesatkan karena operasinya mengaku
menghapus seluruh jurnal.

Kini baris dihapus lebih dulu, baru grupnya, dan keduanya dilaporkan — pola dua tahap yang
sudah dipakai `cleanDuplicates` di berkas yang sama.

**Penjagaan aksinya tidak diubah, dan memang sudah kuat**: CSRF, khusus admin, bendera
konfigurasi `tampilkan_bersihkan_jurnal` yang mati secara bawaan, serta konfirmasi ketik
"HAPUS SEMUA JURNAL". Empat lapis.

## 3. TIDAK diperbaiki: `cleanDuplicates` menganggap baris sah sebagai duplikat

Perlu keputusan pemilik sistem, bukan tebakan.

```sql
delete from akunting.transaksi where id in (
  select min(id) from akunting.transaksi
  group by grup_transaksi, akun, debet, kredit having count(*) > 1)
```

Dua baris dalam satu jurnal yang menunjuk akun sama dengan nilai debet/kredit sama adalah
**hal yang sah** — misalnya dua alokasi biaya berbeda keterangan ke akun yang sama.
Pengelompokan di atas tidak melihat keterangan, tanggal, maupun apa pun yang membedakannya.
Menghapus salah satunya **membuat jurnalnya tidak balance**, diam-diam.

Tiga hal yang perlu diputuskan:

1. **Apa sebenarnya definisi duplikat di sini?** Bila keterangan ikut dijadikan kunci,
   sebagian besar salah-tangkap hilang.
2. **Kenapa `min(id)`?** Yang dihapus adalah baris tertua, yang dipertahankan yang terbaru —
   kebalikan dari kebiasaan dedup. Dengan tiga baris kembar, satu jalannya hanya membuang
   satu.
3. **Kenapa jurnal terposting dan yang sudah closing ikut tersapu?** Setiap jalur tulis lain
   di layanan ini memanggil `guardEditable` — "Batalkan posting sebelum mengubah jurnal",
   "Jurnal telah masuk closing". `cleanDuplicates` tidak memanggilnya sama sekali, sehingga
   ia satu-satunya jalur yang dapat mengubah isi jurnal yang sudah diposting atau ditutup,
   tanpa jejak.

Butir 3 yang paling mudah disepakati dan paling kecil risikonya: menambahkan syarat
`posting_history is null and closing is null` menyelaraskannya dengan aturan yang sudah
berlaku di seluruh layanan. Tetapi bila fitur ini justru dipakai untuk **memperbaiki** data
terposting yang rusak, syarat itu mematikan gunanya. Karena itu tidak diubah sepihak.

Penjagaan aksinya: CSRF, khusus admin, konfirmasi ketik "BERSIHKAN DUPLIKAT" — tanpa bendera
konfigurasi, jadi selalu tersedia bagi admin.

## 4. Verifikasi

`javac -source 1.7 -target 1.7 -encoding UTF-8` EXIT=0 pada tiap perubahan. Kedua perbaikan
dipastikan ada di HEAD lewat `svn cat -r HEAD`.
