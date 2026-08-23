# 23 — Posting Pajak: tombol yang selama ini mati di Draft Jurnal

Draft Jurnal sudah lama menampilkan baris **Pajak** lengkap dengan jumlah draft dan jumlah
terposting. Tombol Postingnya tidak pernah dapat ditekan: `DraftJurnalApiHelper.modulPosting()`
tidak mengenal nama baris itu, sehingga `bisaPosting` selalu `false`. Penggunanya melihat
pekerjaan yang menunggu dan tidak punya cara mengerjakannya dari POS.

Draft Jurnal menampilkan 35 baris. Sebelas di antaranya punya mesin posting di POS;
Pajak kini yang **kedua belas**.

---

## 1. Mesin postingnya

`PostingPertangungjawabanPajakAction` mendapat dua metode statis tanpa layar,
`postingSemua(mulai, sampai, oleh, tglPosting)` dan `batalkanPostingSemua(mulai, sampai)`,
sama seperti sembilan mesin sebelumnya.

Jurnalnya sederhana — satu debet, satu kredit, sebesar `pajak.nilai`:

| Sisi | Akun |
|---|---|
| Debet | `pajak.jenisPajakBarang.akun` |
| Kredit | akun lawan, lihat di bawah |

**Akun lawan urutannya penting** dan disalin apa adanya dari layar. Yang belakangan menimpa
yang terdahulu, dari yang paling umum ke yang paling khusus:

1. `pertangungjawaban.uangMuka.jenisUangMuka.akun` — pajak yang lahir dari LPJ uang muka;
2. `…detail.penerimaanPengadaanMasterAsset.jenisPenerimaanBarang.akun` — dari penerimaan barang;
3. `…detail.jenisPajakBarang.akun`;
4. `…detail.jenisPajakBarang.akunDanaTitipan`.

Dana titipan menang karena pajak yang dipungut memang dititipkan dulu sebelum disetor.
Urutan ini ditulis sekali di `akunLawan(Pajak)` supaya tidak ada dua salinan yang bisa
berbeda diam-diam.

Baris yang salah satu sisinya kosong **dilewati**, tidak dipaksakan. Akunnya harus dilengkapi
dulu di master Jenis Pajak Barang; menebak akun untuk jurnal pajak jauh lebih buruk daripada
menolak mengerjakannya.

---

## 2. Tiga hal yang sengaja BERBEDA dari tombol di layar ZK

### a. Riwayat posting dibuat belakangan

Layar menyimpan `PostingHistory` **sebelum** memeriksa ada-tidaknya dokumen. Menekan tombol
pada angka nol meninggalkan baris riwayat kosong di basis data selamanya. Jalur API memeriksa
dulu, lalu membuat riwayatnya.

### b. Penanda posting ditulis lewat SQL, bukan `session.update`

Entitas `Pajak` punya dua getter yang **menulis balik field-nya saat dibaca**:

```java
public JenisPajakBarang getJenisPajakBarang() {
    jenisPajakBarang = check(jenisPajakBarang);
    if (saldoAwalMasterAssetDetail != null && …) {
        jenisPajakBarang = saldoAwalMasterAssetDetail.getJenisPajakBarang();
    }
    return jenisPajakBarang;
}
```

`getSatuanKerja()` serupa. Ini keluarga jebakan yang sama dengan `Closing.getDikunci()` di
dokumen 22 — yang dulu memicu `ConcurrentModificationException` ketika Hibernate menyapu
perubahan. Hibernate memanggil getter ini sendiri saat flush, jadi sekadar **membaca** entitas
membuatnya kotor.

Akibat lanjutannya lebih halus dan lebih berbahaya: penanda posting ditulis dengan SQL,
sementara salinan entitas di sesi masih memegang `postingHistory = null`. Karena kotor, flush
pada dokumen **berikutnya** akan menulis balik null itu — penanda dokumen sebelumnya lenyap,
jurnalnya tetap ada, dan dokumen itu muncul lagi sebagai draft. Terposting dua kali.

Karena itu entitasnya di-`evict` setelah penandanya dipasang. Harness menyediakan **dua**
dokumen lengkap khusus untuk ini: dengan satu dokumen saja, iterasi keduanya tidak pernah
terjadi dan cacatnya tidak akan terlihat.

### c. Baris `akunting.transaksi` dihapus lebih dulu saat batal

Layar hanya menghapus `grup_transaksi`, induknya, sehingga baris transaksinya tertinggal
yatim. Jalur API menghapus anaknya dulu.

Yang **tidak** berubah: hanya jurnal yang `closing is null` yang dihapus. Periode yang sudah
ditutup tidak dapat diubah dari sini.

---

## 3. Hitungan yang melebih-lebihkan pekerjaan

`DraftJurnalRingkasanUtil.kriteriaPajak()` tidak punya penyaring **breakdown** yang ada di
`initCriteria()` layar:

```java
// bila tagihan vendornya breakdown=ya, PPh yang sah adalah baris "Bukti Potong",
// bukan baris PPh per-item
```

Artinya angka draft yang ditampilkan **lebih besar** daripada yang dapat dikerjakan siapa pun.
Sisanya tidak akan pernah turun ke nol, dan penggunanya akan mengira ada pekerjaan yang
tertinggal. Penyaringnya kini ada di kedua tempat, dan komentarnya menyebutkan bahwa keduanya
harus tetap sama.

---

## 4. Hasil sebagian kini disebut

Selama ini `draft_jurnal_posting` hanya melaporkan jumlah yang berhasil. Bila mesin melewati
sebagian dokumen — akunnya belum lengkap, atau penyimpanannya gagal — angkanya hanya terasa
kurang tanpa penjelasan. Jawabannya kini menambahkan:

> *"… 1 dokumen lain dilewati: jurnalnya belum lengkap (akun belum diisi pada masternya)
> atau gagal disimpan. Periksa Error Log."*

Bendera `dilewati` juga dikirim, dan layar memakainya untuk memperpanjang waktu baca notifikasi
dari 4 menjadi 9 detik — justru kalimat itulah yang perlu ditindaklanjuti. Perbaikan ini berlaku
untuk **kedua belas** modul, bukan hanya Pajak.

---

## 5. Gerbang haknya

Kunci menunya `pengadaan_pajak`, bukan kunci dasbor. Alasannya:

- layar POS "Bayar Pajak (PPh/PPN)" bekerja pada entitas `akunting.Pajak` yang **sama**;
- `pengadaan_pajak` ada di `KUNCI_CRUD`, sehingga hak `create`-nya benar-benar dapat dibatasi
  admin per peran.

Yang kedua bukan formalitas. `EbisnisMenuKatalog.bolehAksi` mengembalikan **true** untuk kunci
di luar `KUNCI_CRUD` — itulah cacat fail-open yang diperbaiki di dokumen 20 ketika dua baris
lain menunjuk kunci yang bukan kunci menu.

---

## 6. Uji

`TesPostingPajak` — UAT tidak punya satu pun baris `akunting.pajak`, jadi harness merangkai
rantainya sendiri (jenis uang muka berakun → uang muka → pertangungjawaban → pajak) dan
menghapusnya di akhir. Empat baris pajak dibuat sengaja:

| Baris | Diharapkan |
|---|---|
| lengkap | terposting |
| lengkap kedua | terposting, dan penanda yang pertama TIDAK tertimpa |
| tanpa akun lawan | dilewati, dan disebut dalam jawaban |
| milik tagihan breakdown | tidak dihitung, tidak diposting |

Lalu pembatalannya harus mengembalikan keadaan persis seperti semula: jurnal terhapus, tidak
ada baris transaksi yatim, penanda kosong kembali. Ditambah satu uji rentang kosong: memposting
periode tanpa dokumen tidak boleh meninggalkan riwayat posting kosong.

### Harness sempat menuduh kode yang benar

Percobaan pertama gagal separuh: dokumen pertama tidak berjurnal, yang kedua berhasil. Galatnya
tertelan `ErrorAuditUtil` dan baru terbaca dari tabel `error_log`:

```
java.util.ConcurrentModificationException
  at java.util.HashMap$HashIterator.nextNode
  at org.hibernate.pretty.Printer.toString(Printer.java:113)
  at …AbstractFlushingEventListener.flushEverythingToExecutions(:120)
  at …CommonAkunting.saveTransaksi(:564)
```

`flushEverythingToExecutions` memanggil `Printer` **hanya bila log Hibernate berada di level
DEBUG**, untuk mencetak isi sesi. Pencetakan itu memanggil getter tiap entitas — termasuk getter
`Pajak` yang mendereferensi asosiasi lazy — sehingga entitas baru masuk ke peta persistensi DI
TENGAH iterasi peta itu sendiri. Dokumen kedua lolos justru karena asosiasinya sudah terlanjur
termuat oleh percobaan pertama.

Penyebabnya lingkungan harness: tidak ada `log4j.properties` di classpath-nya, dan log4j tanpa
konfigurasi jatuh ke DEBUG. Aplikasi terpasang memakai `src/log4j.properties` yang menyetel
`log4j.logger.org.hibernate=warn`, jadi jalur `Printer` itu mati. Harness kini diberi salinan
konfigurasi yang sama, dan hasilnya 25 lulus 0 gagal.

> **Peringatan yang layak diingat:** menaikkan log Hibernate ke DEBUG di lingkungan terpasang
> akan MEMATAHKAN posting, bukan sekadar memperbanyak keluaran log. Gejalanya pun menyesatkan —
> sebagian dokumen berjurnal, sebagian tidak, tanpa pesan galat di layar.

---

## 7. Yang masih terbuka

Dua belas dari 35 baris Draft Jurnal kini punya mesin posting di POS. Dari 23 sisanya, tiga
memang bukan pekerjaan posting massal — **Jurnal Umum** dan **Posting HPP** punya layar POS-nya
sendiri lengkap dengan tombol posting, dan **Closing** adalah penguncian, bukan penjurnalan.
Yang benar-benar belum punya mesin ada 20 baris:

| Baris | Jumlah | Mesin ZK |
|---|---|---|
| DP Vendor, DP Pekerjaan Vendor, Jurnal Balik DP Pekerjaan | 3 | `PostingTransaksiHarianAction` |
| Jurnal Penyusutan | 1 | `PostingPenyusutanTabsAction` |
| Fix Aset dan Aset dalam Pekerjaan (jurnal saat BAST) | 2 | jalur BAST |
| Gaji | 1 | posting penggajian |
| Mahasiswa — 7 baris | 7 | transaksi mahasiswa |
| Siswa — 6 baris | 6 | transaksi siswa |

Selain itu: kolom `jenis_pajak_barang` pada `akunting.pajak` **NOT NULL** di basis data,
padahal entitasnya memetakannya `nullable = true` dengan alasan baris Bukti Potong mode
breakdown tidak terikat satu jenis PPh. Keduanya tidak sejalan. Ini tidak menghalangi apa pun
hari ini — tetapi bila baris Bukti Potong tanpa jenis memang harus ada, basis datanya yang
akan menolak lebih dulu, bukan kodenya.
