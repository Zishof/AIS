# 23 — "Toko tidak diketahui" — toko yang tidak pernah dikirim

Gejala yang dilaporkan: di **eKantin Petra**, akun `admin_kantin`, layar
**Konfigurasi → Profil Toko** menolak dengan

> Toko tidak diketahui.
> Perbaiki data sesuai penjelasan di atas, lalu simpan kembali.

padahal kotak toko di kiri atas jelas menunjuk satu toko (`CE NDUTT`).

## Sebab

`KantinHelper.tokoProfilAmbil` menentukan toko begini:

```java
Pedagang pemanggil = tbmuser.getPedagang();
if (pemanggil != null) tokoId = pemanggil.getToko().getId();
else                   tokoId = request.get("toko_id");   // ← tidak pernah dikirim
```

Klien memanggil `ApiClient.instance.aksi('toko_profil_ambil')` **tanpa payload sama
sekali**. Akun admin tidak punya `Pedagang`, jadi server tidak punya sumber lain — dan tab
ini **tidak pernah** bisa terbuka untuk akun admin sejak awal.

Ini bukan regresi dari combo filter toko. Filter itu hanya membuatnya terasa aneh, karena
layar seolah sudah tahu tokonya. Pesan lamanya memperburuk keadaan: ia menyuruh
"perbaiki data" padahal datanya baik-baik saja — yang kurang justru satu parameter.

## Bug kedua, lebih berbahaya

`tokoProfilAmbil` dan `tokoProfilSimpan` memakai **urutan yang berbeda**:

| | Ambil (lama) | Simpan (lama) |
|---|---|---|
| Punya `Pedagang` | toko Pedagang, `toko_id` diabaikan | admin boleh menyebut `toko_id` |

Akibatnya seorang **admin yang juga terdaftar sebagai pedagang** membaca profil toko A
lalu menuliskan isian itu ke toko B — tanpa galat, tanpa tanda apa pun bahwa data mendarat
di toko yang keliru. Kegagalan yang senyap seperti ini lebih mahal daripada yang berteriak.

Kedua jalur kini memakai urutan identik (SVN **r77896**):

```java
if (adminGlobal) tokoId = tokoDiminta != null ? tokoDiminta : tokoPemanggil;
else             tokoId = tokoPemanggil != null ? tokoPemanggil : tokoDiminta;
```

Pesan penolakannya juga diperbaiki agar menyebut apa yang kurang, bukan menuduh datanya
rusak.

## Sapuan: ternyata bukan satu layar

Ditelusuri 165 aksi `PosApi` → metode handler → apakah tokonya dibaca dari payload, lalu
dicocokkan dengan 39 pemanggilan di klien. Hasil: **16 pemanggilan di 8 layar** tidak
pernah mengirim toko, menyentuh **13 aksi**.

**Selalu gagal untuk akun admin** — pola persis sama:

| Layar | Aksi |
|---|---|
| Konfigurasi → Akun Pengguna | `pedagang_list`, `akun_tambah` |
| Produk | `produk_simpan`, `produk_ekspor_excel` |
| Impor Excel Produk | `produk_impor_excel_preview` |
| Rekonsiliasi Ledger | `produk_rekonsiliasi_ledger` |
| Stok Opname | `so_simpan`, `so_ekspor_excel`, `so_impor_excel` |
| Kulakan & Kulakan Bulk | `kulakan_faktur_simpan` |
| Diskon | `diskon_simpan`, `pencairan_diskon_simpan` |

**Tidak gagal, tapi diam-diam melebar ke seluruh toko** — mengabaikan toko yang terpilih:
`peringkat_mitra` dan `pencairan_diskon_list`. Yang begini lebih berbahaya daripada yang
gagal: angkanya terlihat wajar dan bisa bertahan lama sebelum ada yang menyadari.

## Perbaikan: terpusat, bukan 16 tambalan

`Sesi.idTokoTerpilih`:

```dart
int? get idTokoTerpilih => tokoFilter ?? tokoId;
```

Urutannya bukan selera. Kotak toko menampilkan nama dari `tokoFilter` bagi pengguna
berizin lintas toko, jadi kalau `tokoId` didahulukan, layar akan menyunting toko yang
**berbeda dari yang tertulis di depan mata pengguna**.

`ApiClient.susunPayload()` menyisipkan `toko_id` untuk daftar aksi `_aksiBerTokoId`. Satu
tempat, bukan 16 pemanggilan di 8 layar — dan alasannya bukan kerapian: **menambah layar
baru berarti menambah satu baris di daftar, jauh lebih sulit terlewat daripada mengingat
memasang `toko_id` di setiap pemanggilan baru** — persis kelalaian yang melahirkan bug ini.

Aturan penyisipan:

- nilai yang sudah ditentukan pemanggil **tidak pernah ditimpa**;
- bila belum ada toko terpilih, kuncinya **tidak dikirim** dan server menolak dengan
  pesannya sendiri — lebih baik ditolak daripada menebak toko lalu salah tulis;
- untuk akun ber-`Pedagang`, server tetap memakai toko Pedagang, jadi tidak ada yang
  berubah bagi kasir biasa.

### Yang sengaja dikecualikan

`sesi_kas_buka`, `sesi_kas_status`, `sesi_kas_list`, `pilih_toko_aktif` — semuanya mengurus
toko **aktif** (tempat transaksi dicatat), bukan toko yang sedang dilihat. Menyuntik
pilihan filter ke sana berarti membuka sesi kas di toko yang salah. Uji mengunci
pengecualian ini, bukan hanya isi daftarnya: daftar yang benar hari ini gampang jadi salah
saat dirapikan orang lain.

## Perbaikan lain di layar Profil Toko

- Nama toko ditulis sebagai spanduk **di atas formulir**, bukan hanya di pojok layar,
  supaya tidak ada keraguan profil toko mana yang tersimpan saat menekan Simpan.
- Mengganti toko sewaktu tab terbuka **memuat ulang** isian. Tanpa itu, isian toko lama
  tetap terpampang di bawah nama toko baru dan Simpan menuliskannya ke toko yang keliru.
  Penanda toko dicatat **sebelum** permintaan dikirim; kalau dicatat sesudah berhasil,
  permintaan yang gagal membuat penjaga di `build()` memanggil ulang tanpa henti.
- Struk: bila belum ada toko terpilih, struk dicetak **tanpa kop**, bukan gagal cetak —
  ada pelanggan menunggu di depan kasir.

## Perubahan perilaku yang perlu diketahui

Saat admin memilih satu toko, **Peringkat Mitra** dan **Pencairan Diskon** kini menampilkan
toko itu saja, bukan semua. Saat pilihannya masih "Semua Toko", tidak ada yang berubah —
kunci tokonya tidak dikirim sama sekali.

## Belum terverifikasi

`produk_impor_excel` (impor sesungguhnya, bukan pratinjau) dan `otomatis_layani_jalankan`
tidak terlihat dipanggil klien mana pun. Keduanya tetap dimasukkan daftar supaya aman bila
nanti dipakai, tetapi jalurnya belum pernah dijalankan.

## Uji

18 uji: aturan penyisipan payload (termasuk bahwa nilai pemanggil tidak ditimpa dan bahwa
tanpa toko terpilih kuncinya tidak dikirim), urutan `idTokoTerpilih`, kecocokan antara nama
yang tampil dan id yang terkirim, serta pengecualian `sesi_kas_*`/`pilih_toko_aktif`.
Seluruh suite klien 152/152 lulus.
