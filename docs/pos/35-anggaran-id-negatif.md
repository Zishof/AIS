# Modul Anggaran: id negatif dan angka JavaScript

Melanjutkan aturan pada [07-temuan-dan-jebakan.md](07-temuan-dan-jebakan.md) §1 — "untuk id
anggaran, *belum dipilih* berarti `== 0`, bukan `<= 0`" — dokumen ini mencatat penerapannya
di modul **Anggaran (RAB Bulanan)** sendiri: `AnggaranApiHelper`, layar JSP
`modul/kantin/anggaran.jsp`, dan layar Flutter `anggaran_screen.dart`.

Cacatnya tidak pernah muncul pada uji sebelumnya karena harness lama membuat datanya
sendiri lewat API — id dari sequence, positif — sedangkan seluruh data warisan negatif.

## Sebaran id yang sebenarnya (UAT, 22-08-2026)

| tabel.kolom | total | negatif | rentang |
|---|---:|---:|---|
| `rab.workspace.id` | 37 | **37** | −9.123.372.834.534.775.782 … −7.223.372.036.854.765.776 |
| `rab.workspace.parent_id` | 37 | **37** | akar memakai sentinel −9223372036854775807 |
| `rab.satuan_kerja.id` | 37.038 | 0 | 1 … 2.000.000.001 |
| `rab.sumber_dana.id` | 34 | 0 | 1 … 34 |
| `akunting.akun.id` | 311 | 0 | 3.400.001 … 34.400.001 |

Hanya **3** baris yang benar-benar akar (induknya tidak ada di tabel); 34 sisanya anak
sungguhan. Kolom `deep` bernilai 0 di semua baris sehingga tidak dapat dipakai menentukan
hierarki — satu-satunya uji akar yang sahih adalah **"induknya tidak ikut terambil"**.

## Sembilan penjaga yang salah menilai, dan akibatnya

| Tempat | Pola lama | Akibat |
|---|---|---|
| `itemSimpan` | `boolean baru = id <= 0` | Setiap penyuntingan item lama dinilai **baru** → menambah baris, bukan memperbaruinya |
| `itemSimpan` | `if (parentId > 0)` | Induk tidak pernah terpasang → hierarki datar, agregat tidak naik ke akar |
| `itemList` | `akar = parentId <= 0` | Semua baris dihitung akar → ringkasan pagu berlipat |
| `itemHapus` | `if (id <= 0)` | "Item yang dihapus belum dipilih" untuk item yang sah |
| `penggunaanList` | `if (workspaceId > 0)` | Saringan per item tidak pernah dipakai |
| `penggunaanSimpan` | `workspaceId > 0` / `<= 0` | Selalu ditolak "Item anggaran belum dipilih" |
| `penggunaanSimpan` | `baru = id <= 0` | Sama seperti `itemSimpan` |
| `penggunaanHapus` | `id <= 0 ? null` | Data tidak pernah ketemu |
| `anggaran_screen.dart` | `(workspaceId ?? 0) <= 0` | Formulir penggunaan tidak pernah bisa disimpan |

Seluruhnya kini melalui satu helper `AnggaranApiHelper.idKosong(long)`, dan uji akar
memakai keanggotaan himpunan id yang terambil — uji yang sebetulnya sudah benar di
`realisasiList()` tetapi tidak konsisten dipakai `itemList()`.

**Besar kesalahannya, terukur:** untuk kelompok terbesar (32 baris, tahun 2024), pagu
sesungguhnya **360.000.000**; menjumlahkan seluruh baris memberi **1.080.000.000**.
Ringkasan menampilkan **tiga kali lipat** angka yang benar.

## Id 19 digit tidak muat di angka JavaScript

`Number.MAX_SAFE_INTEGER` hanya 9.007.199.254.740.991 (16 digit), sedangkan id anggaran
berkisar −9,1 × 10¹⁸. Begitu JSON-nya di-parse peramban, id-nya **dibulatkan diam-diam**:
`-9123372834434775782` menjadi `-9123372834434776000`. Akibatnya setiap tombol
Ubah/Hapus/Tambah-anak serta pilihan induk pada kanal JSP menunjuk baris yang tidak ada.

Penyelesaiannya sejalan dengan `AnggaranKeuanganUtil.cari`:

- Peladen mengirim **`idTeks`**, **`parentIdTeks`**, dan **`workspaceIdTeks`** di samping
  bidang angkanya. Bidang angka dipertahankan supaya klien lama tidak patah.
- Peladen membaca keduanya lewat `idDari(request, nama)` — bentuk teks didahulukan.
- `anggaran.jsp` memakai bentuk teks di 18 titik: kunci pohon, atribut `data-id`, nilai
  `<option>`, pencocokan baris saat tombol diklik, dan seluruh payload.
- Klien Dart **tidak** diubah: `int` Dart 64 bit, jadi nilainya sudah tepat.

## Bukti

Harness `TesIdNegatifAnggaran` dijalankan langsung terhadap 37 baris warisan — **8/8
lulus**:

```
baris tampil=32 akar=1 totalApi=3.6E8 jumlahAkar=3.6E8 jumlahSemua=1.08E9
  OK  ringkasan memakai baris akar saja (tidak berlipat)
  OK  akar lebih sedikit daripada seluruh baris
  OK  idTeks dikirim sebagai teks
  OK  id numerik memang di luar jangkauan aman JavaScript
  OK  simpan item ber-id negatif = UPDATE, bukan INSERT
  OK  induk negatif tetap terpasang setelah disimpan
  OK  penggunaan anggaran menerima workspace ber-id negatif
  OK  tidak ada sisa data uji
```

Baris terakhir penting: jumlah baris `rab.workspace` dan `rab.penggunaan_anggaran` kembali
persis seperti sebelum harness berjalan. Cara menjalankannya ada di
[08-harness-uji.md](08-harness-uji.md).

Penjaga di sisi klien: `test/anggaran_kontrak_test.dart` — uji *"id anggaran negatif tidak
boleh dinilai belum dipilih"* menolak kembalinya pola `(workspaceId ?? 0) <= 0`.

## Yang masih perlu diperiksa

Pola `<= 0` untuk id anggaran berpotensi ada di helper lain yang belum disapu. Perintah
penyisirannya ada di [08-harness-uji.md](08-harness-uji.md); yang dicari adalah
perbandingan `<= 0`/`> 0` pada id yang berasal dari `rab.*`, bukan pada `satuan_kerja`,
`sumber_dana`, atau `akun` yang memang positif.
