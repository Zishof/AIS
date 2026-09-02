# 92 — Penjaga yang memeriksa dua dari 295 berkas

Tanggal: 2026-09-02

`field-tanpa-pembaca.py` sudah berjalan hijau sejak dok. 77. Ia menjawab
pertanyaan "adakah field yang dikirim server tetapi tidak dibaca klien mana pun?"
dengan **BERSIH**, berkali-kali, di setiap batch.

Yang tidak pernah tertulis di mana pun: ia hanya membaca **dua berkas** sebagai
sumber field.

## 1. Cakupan yang tak pernah dinyatakan

| | Sebelum | Sesudah diukur |
|---|---|---|
| berkas pengirim dipindai | 2 | 295 ada |
| field dikirim yang diperiksa | 270 | 733 ada |

`KantinHelper` (1.570 `hasil.put`) dan `DraftJurnalApiHelper` (61) diperiksa.
`PosApi` (374), `PengadaanPosApiHelper` (312), `HotelApiHelper` (96),
`SalesInventoryReceivableHelper` (101) dan puluhan helper lain tidak pernah
disentuh.

Ini pola yang sama dengan dok. 90, tetapi lebih buruk: di sana batasnya
**ditulis** dengan angka yang salah. Di sini batasnya tidak ditulis sama sekali,
sehingga "BERSIH" terbaca seolah berlaku untuk seluruh permukaan API.

## 2. Hasil pelebaran: 57 kandidat

Lima diverifikasi tangan dengan menyisir kedua repositori klien — tak satu pun
muncul di Dart, JSP, maupun JavaScript:

| Field | Berkas |
|---|---|
| `kebijakanEditGlobalAktif` | `PosApi` |
| `kebijakanEditTokoAktif` | `PosApi` |
| `latensiKueriMs` | `PosApi` |
| `po_baru_id` | `PengadaanPosApiHelper` |
| `anggota_ditambah` | `GrupProdukApiHelper` |

Dua yang pertama ditelusuri sampai tuntas. Di sebelahnya server sudah mengirim:

```java
hasil.put("bolehEditTransaksi", bolehEditTransaksi);   // dibaca 3 berkas Dart
hasil.put("kebijakanEditGlobalAktif", ...);            // 0 pembaca
hasil.put("kebijakanEditTokoAktif", ...);              // 0 pembaca
...
hasil.put("alasanEdit", alasanEdit);                   // dibaca 2 berkas Dart
```

Klien memakai keputusan efektifnya (`bolehEditTransaksi`) dan kalimat alasannya
(`alasanEdit`). Kedua penanda kebijakan itu **rincian** yang tak seorang pun
buka: biaya tanpa manfaat, bukan cacat yang merugikan pengguna — tetapi juga
menyesatkan pembaca berikutnya, yang akan mengira klien membedakan kebijakan
global dan per-toko.

## 3. Mengapa 57 itu TIDAK dibekukan sebagai utang

Godaannya jelas: masukkan 57 ke daftar `UTANG`, alat kembali hijau, cakupan naik
dari 270 ke 733. Selesai.

Itu akan merusak daftar utangnya. Sebelas entri yang sudah ada masing-masing
punya **sebab tertulis** hasil penelusuran; menambahkan 57 baris tanpa sebab
mengubah daftar itu dari catatan keputusan menjadi daftar bungkam. Sesudahnya
tidak ada yang bisa membedakan mana yang sudah diperiksa dan mana yang hanya
didiamkan — persis kesalahan yang dihindari dok. 84.

Jadi pemisahan dok. 84 dipakai lagi:

* **Gerbang** (`field-tanpa-pembaca.py`) tetap sempit: dua berkas, setiap
  yatimnya beralasan, keluar dengan kode 1 bila ada yang baru.
* **Laporan** (`--luas`) menjangkau 295 berkas dan 733 field, menyebut 57
  kandidat, dan **selalu** keluar dengan kode 0.

Gerbang yang hijau karena himpunannya kecil dan terperiksa lebih berguna
daripada gerbang yang hijau karena 57 hal disuruh diam.

## 4. Untuk pemilik tiap modul

```bash
python docs/pos/alat/field-tanpa-pembaca.py --luas
```

Tiap kandidat menuntut satu keputusan yang tidak ada di dalam kode: **field itu
seharusnya dibaca klien, atau seharusnya tidak dikirim?** Keduanya perbaikan yang
sah. Memindahkannya ke daftar utang juga sah — asal disertai sebab, seperti
sebelas entri yang sudah ada.

## 5. Yang dipelajari

**"BERSIH" adalah pernyataan tentang apa yang diperiksa, bukan tentang apa yang
ada.** Sebuah gerbang yang hijau setiap hari selama lima belas dokumen ternyata
memeriksa 0,7% berkas pengirimnya. Tidak ada yang berbohong; cakupannya memang
tidak pernah ditanyakan.

**Dua batch berturut-turut menemukan hal yang sama.** Dok. 90 menemukan angka
cakupan yang salah; dok. 92 menemukan cakupan yang tidak pernah disebutkan.
Setiap alat di direktori ini sekarang layak ditanyai satu hal yang sama: *berapa
banyak yang sebenarnya kamu lihat?*
