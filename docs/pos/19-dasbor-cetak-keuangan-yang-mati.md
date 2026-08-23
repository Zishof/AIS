# 19 — Dasbor & cetak Keuangan yang ternyata mati sejak awal

Tiga cacat yang **saya buat sendiri** saat membangun grup Keuangan, semuanya baru ketahuan
dari log galat produksi (`https://ebisnis.id`, 22–23 Agustus 2026), bukan dari harness.

```
[ERROR] api:keuangan_dasbor
Aksi tidak dikenal: keuangan_dasbor
Permintaan: {"action":"keuangan_dasbor","modul":"uang_muka","bulan":12}
```

---

## 1. `KeuanganApiHelper` tidak pernah dipasang ke dispatcher

Helper-nya lengkap sejak awal — dasbor untuk tujuh modul, cetak, hak akses, semuanya —
tetapi **tidak ada satu pun dispatcher yang merujuknya**. `PosApi` punya cabang untuk
`uang_muka_`, `kas_kecil_`, `master_keuangan_`, dan seterusnya, tetapi tidak untuk
`keuangan_`.

Akibatnya, sejak grup Keuangan pertama kali dirilis:

- **tab Dasbor pada kedelapan layar Keuangan** selalu dijawab "Aksi tidak dikenal";
- **tombol Cetak** pada layar-layar itu tidak pernah bekerja.

Cabangnya kini terpasang, dan sengaja **tidak diam** bila ada aksi `keuangan_*` yang belum
dikenal — ia menjawab `91` dengan namanya, bukan balasan kosong.

### Jebakan yang menyembunyikannya

Memeriksanya dengan `grep KeuanganApiHelper PosApi.java` memberi **hasil palsu**: nama itu
adalah SUBSTRING dari `MasterKeuanganApiHelper` yang memang terpasang. Saya sendiri sempat
tertipu — pemeriksaan pertama saya melaporkan "TERPASANG" untuk keduanya.

> **Pelajaran:** untuk memeriksa rujukan nama kelas, pakai batas kata
> (`(^|[^A-Za-z0-9_])Nama([^A-Za-z0-9_]|$)`), jangan `contains`. Nama kelas di basis kode
> ini sering bersarang satu sama lain (`KeuanganApiHelper` ⊂ `MasterKeuanganApiHelper`,
> `KasBesarApiHelper` ⊂ `PertangungjawabanKasBesarApiHelper`).

---

## 2. Dasbor Dana Talangan menampilkan angka modul lain

`dana_talangan_screen.dart` meminta `tahap: 'penggantian_kas_kecil'` — sisa salin-tempel.
Tabnya menampilkan angka Penggantian Kas Kecil **tanpa satu pun pesan galat**, jadi tidak
akan pernah terlihat sebagai kesalahan; angkanya hanya "terasa aneh".

Sekaligus kebalikannya: `reimbursement_screen.dart` meminta `tahap: 'reimbursement'` yang
**belum terdaftar** di `KeuanganApiHelper.MODUL`, sehingga tabnya dijawab "Modul dasbor
tidak dikenali".

Keduanya diperbaiki: layar menunjuk modulnya sendiri, dan `dasborReimbursement` ditulis
(KPI-nya menyoroti status **Revisi** yang khas modul itu — berapa yang dikembalikan untuk
diperbaiki, angka yang tidak ada padanannya di modul Keuangan lain).

---

## 3. `keuangan_cetak` dapat mencetak DOKUMEN ORANG LAIN

Ini yang paling berbahaya. Cabangnya berbunyi:

```java
if ("uang_muka".equals(modul)) { … LaporanUangMuka … }
else                           { … Pertangungjawaban … }   // SEMUA modul lain
```

Artinya mencetak Kas Besar / Kas Kecil / Penggantian / LPJ Kas Besar / Dana Talangan
memuat **`Pertangungjawaban` dengan id tersebut**. Bila kebetulan ada LPJ ber-id sama,
penggunanya menerima PDF dokumen yang sama sekali berbeda — tanpa peringatan apa pun.

Sekarang tiap modul disebut eksplisit. Kenyataannya hanya dua kelas laporan yang punya
`cetakPdf()` headless (`LaporanUangMuka`, `LaporanPertangungjawaban`); lima lainnya
(`LaporanKasBesar`, `LaporanKasKecil`, `LaporanPenggantianKasKecil`,
`LaporanPertangungjawabanKasBesar`, `LaporanDanaTalangan`) hanya punya jalur render ZK.
Modul-modul itu **ditolak dengan jujur**:

> *Cetak PDF untuk modul ini belum tersedia di POS; templat headless-nya belum ada.
> Sementara ini cetak dari layar ZK.*

Menolak jauh lebih baik daripada menyodorkan berkas yang salah.

> **Ditutup 23 Agustus 2026.** Kelima laporan itu kini punya `cetakPdf()` headless.
> `generateParameter()` pada kelimanya ternyata **tidak menyentuh satu pun komponen ZK** —
> hanya entitasnya — jadi isinya dipindahkan apa adanya ke `parameter(<entitas>)` yang
> statis, dan metode instansnya menjadi delegasi tipis. Yang memang tidak boleh dipakai
> headless adalah **konstruktornya**, yang membangun `Borderlayout`/`Center`/`Toolbar`.
> Ketujuh modul Keuangan sekarang dapat dicetak dari POS, dan tiap modul disebut
> eksplisit sehingga tidak ada lagi yang jatuh ke cabang milik modul lain.

---

## 4. Penjaga supaya tidak terulang

**Sisi server —** `TesDispatcherApi` (harness): memindai `ais/action/servlet/api/*.java`,
mengambil setiap kelas yang punya `public static boolean proses(...)`, dan memastikan
`PosApi` merujuknya **dengan batas kata**. Uji ini juga mengunci jebakan substring itu
sendiri sebagai kasus uji, dan menjalankan kedelapan modul dasbor sungguhan.

**Sisi klien —** `keuangan_lokal_dulu_test.dart` → *"tiap layar meminta dasbor MODULNYA
SENDIRI"*: memetakan kedelapan berkas layar ke nama modulnya dan menuntut `tahap:` cocok.
Cacat nomor 2 tidak akan lolos lagi.

| Uji | Hasil |
|---|---|
| `TesDispatcherApi` | **22 lulus, 0 gagal** (termasuk ketujuh jalur cetak) |
| `flutter analyze` | 0 error |
| `flutter test` | **247 lulus** (sebelumnya 246) |

---

## 5. Perlu deploy ulang

Perbaikan ini ada di sisi **server**. Produksi `https://ebisnis.id` masih menjalankan WAR
lama, jadi galatnya akan terus muncul sampai build barunya dipasang. Deploy adalah pekerjaan
Anda — saya tidak menyentuh produksi.

Catatan kecil dari log yang sama: dua galat lain di sana **bukan cacat** —
`uang_muka_simpan` ditolak karena judulnya memang kosong, dan `ebisnis_role_list` ditolak
karena penggunanya bukan admin sistem. Keduanya berperilaku benar.
