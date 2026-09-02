# 78 — Penjaga arah sebaliknya: klien mengirim, server tidak membaca

[77](77-gerbang-oversell-dan-penjaga-field-yatim.md) memasang penjaga arah **server →
klien**. Arah sebaliknya belum terjaga sama sekali — padahal **cacat pertama dari seluruh
rangkaian ini justru ada di sana**:

> [45](45-penyaring-dasbor-dan-layani-semua.md) — klien sudah lama mengirim `metodeBayar`
> pada payload dasbor; `PosApi.prosesDashboardUmum` tidak pernah membacanya. Chip "Jenis
> pembayaran" menyala, tabelnya tetap bercampur.

`TesPayloadTanpaPembaca` menutup arah itu: ia mengambil kunci dari literal payload klien
(Dart `'kunci':` di dalam `aksi(...)`, JavaScript `kunci:` di dalam `fetchDataAPI(...)`),
lalu memeriksa apakah nama itu muncul sama sekali di sumber server.

---

## 1. Tiga kali salah sebelum benar

Penjaga ini menuduh lebih banyak daripada yang bersalah pada tiga percobaan berturut-turut.
Semuanya ketahuan sebelum dilaporkan, dan urutannya layak dicatat karena tiap kesalahannya
berbeda jenis:

| # | Tuduhan | Sebab | Perbaikan |
|---|---|---|---|
| 1 | 295 kunci → puluhan "yatim" | `"aksi("` sebagai substring juga cocok dengan **`transaksi(`** dan `koreksi(` | batas kata `(?<![A-Za-z])` |
| 2 | map yang bukan payload ikut terjaring | `aksi('x')` tanpa map membuat pencarian melompat ke `{` milik badan fungsi berikutnya | kurung buka wajib dalam 120 karakter, dan wajib seimbang |
| 3 | `induk_id`, `SETUJUI`, dan 40-an lainnya | pembacaan dicari lewat regex pengakses (`optString("x"`) — padahal `induk_id` dibaca `Common.angkaAtauNull(request, "induk_id")`, dan `SETUJUI` bukan kunci melainkan **nilai** | pembacaan dicari sebagai literal `"kunci"` di mana pun pada sumber server |

Setelah ketiganya: **255 kunci terkirim, 2 tanpa pembaca.**

Kesalahan ke-3 adalah yang paling instruktif dan pengulangan langsung dari
[77](77-gerbang-oversell-dan-penjaga-field-yatim.md) bagian 2.2: selama helper seperti
`angkaAtauNull(request, "kunci")` boleh ditulis siapa saja, tidak ada daftar nama method
yang bisa lengkap. Satu-satunya penanda yang jujur adalah **literalnya sendiri**.

Harganya disadari: kunci yang kebetulan bernama sama dengan string lain di sisi server akan
lolos (negatif palsu). Itu pilihan sadar — penjaga yang menuduh salah akan diabaikan orang,
dan penjaga yang diabaikan tidak menjaga apa pun.

---

## 2. Dua kunci yang benar-benar yatim

`impor_excel_produk_screen.dart`:164 mengirim ke aksi `produk_impor_excel_komit`:

```dart
return await ApiClient.instance.aksi('produk_impor_excel_komit', {
  'baris': batch.map((b) => b.keKomit()).toList(),
  'hanya_perubahan': true,        // <- tidak pernah dibaca
  'nomor_batch_klien': nomorBatch, // <- tidak pernah dibaca
});
```

`KantinHelper.produkImporExcelKomit` hanya membaca `toko_id` dan `baris` — JavaDoc
`@param`-nya pun hanya menyebut dua itu.

**Keduanya sudah ditelusuri sampai tuntas, dan tidak satu pun berupa korupsi data.**
Itu perlu dinyatakan tegas, karena dua-duanya tampak menakutkan pada pandangan pertama.

### 2.1 `nomor_batch_klien` — kelihatannya bahaya, ternyata tidak

Klien **mencoba ulang sampai 5 kali** saat jaringan putus (baris 162). Kalau permintaan
sempat sampai dan tersimpan tetapi responsnya hilang, batch yang sama terkirim dua kali —
dan kunci idempotensinya tidak pernah dibaca.

Yang menyelamatkan bukan kunci itu, melainkan **cara pencocokan produknya**: berlapis empat
— kode, barcode, nama, lalu `kunci_unik` yang dinormalisasi (tanda baca/spasi/huruf
diabaikan). Kiriman ulang karena itu **memperbarui baris yang sama**, bukan membuat
duplikat.

Yang benar-benar hilang hanya keterlacakan batch di log server.

### 2.2 `hanya_perubahan` — akibatnya nyata tapi sedang

Klien meminta hanya baris yang **berubah** yang dikomit. Server memproses semuanya.

Yang **tidak** terjadi: jurnal Stok Opname palsu. Opname dijaga syarat tersendiri —

```java
double selisih = stokBaru - stokLama;
if (selisih != 0) { ...simpanOpname...; stokDiopname++; }
```

— jadi baris yang stoknya tidak berubah tidak menghasilkan jurnal apa pun.

Yang **terjadi**, dua hal:

1. Hitungan `diperbarui` pada ringkasan impor ikut menghitung baris yang tidak berubah.
   Angka yang dibaca pengguna sesudah impor karena itu lebih besar daripada kenyataannya.
2. Setiap baris tetap menahan lock `koperasi.produk`. Itu memperbesar permukaan **deadlock**
   yang justru sudah dibuatkan mekanisme percobaan-ulang tersendiri — JavaDoc
   `produkImporExcelKomit` mencatat deadlock produksi nyata pada tabel ini.

### 2.3 Kenapa belum dibayar

Memutuskan **"apa artinya tidak berubah"** adalah keputusan tersendiri, bukan detail
implementasi. Perbandingan field yang terlalu longgar akan **melewati perubahan yang sah** —
dan itu kehilangan data yang sunyi, jauh lebih mahal daripada angka ringkasan yang meleset
atau lock yang berlebih.

Karena itu keduanya masuk daftar utang yang terlihat, bukan diperbaiki diam-diam dengan
tebakan. Kalau memang diinginkan, aturannya perlu disepakati dulu: field mana yang
dibandingkan, dan bagaimana `null` diperlakukan terhadap string kosong.

---

## 3. Hasil uji

Penjaganya: [`alat/payload-tanpa-pembaca.py`](alat/payload-tanpa-pembaca.py).

```
python payload-tanpa-pembaca.py
```

Keluar dengan kode **1** bila ada kunci baru tanpa pembaca ATAU ada utang yang sudah
lunas tetapi masih terdaftar — dapat dipakai langsung sebagai gerbang.

**9 dari 9 lulus.**

```
kunci payload yang dikirim klien : 255
kunci yang tidak ketemu di server: 2
Utang lama (dibekukan)           : 2
Kunci BARU tanpa pembaca         : (tidak ada)
```

**Dibuktikan menyala dengan kontrol negatif sungguhan.** Sebuah kunci karangan benar-benar
disisipkan ke payload `bayar` pada `pesanan_screen.dart`:

```
== Kunci BARU yang dikirim klien tanpa pembaca di server ==
   - uatKunciKarangan
  GAGAL tidak ada kunci payload baru tanpa pembaca (1)
```

lalu berkasnya dikembalikan (`git status` bersih) dan uji kembali hijau.

Ada pula kontrol yang menghadap ke belakang: `metodeBayar` — cacat
[45](45-penyaring-dasbor-dan-layani-semua.md) — diperiksa **memang** dibaca server sekarang.
Seandainya uji ini sudah ada sejak dulu, cacat itu tertangkap sebelum sampai ke pengguna.

---

## 4. Yang BELUM diuji

- **Pemetaan aksi → handler.** Uji ini hanya bertanya "dibaca di mana pun?", bukan "dibaca
  oleh handler aksinya". Kunci yang dibaca handler LAIN akan lolos. Memetakan aksi ke
  handler menuntut penguraian yang rapuh, dan salah petakan menghasilkan tuduhan palsu —
  batas yang disadari, bukan kelalaian.
- **Payload yang dirakit dinamis** (`payload['x'] = ...` di luar literal map) tidak
  terjaring sama sekali.

---

## 5. Yang perlu diperiksa lain kali

Dua penjaga sekarang berdiri berpasangan:

| Penjaga | Arah | Utang beku |
|---|---|---|
| [`alat/field-tanpa-pembaca.py`](alat/field-tanpa-pembaca.py) | server → klien | 16 |
| [`alat/payload-tanpa-pembaca.py`](alat/payload-tanpa-pembaca.py) | klien → server | 2 |

Keduanya dibangun sesudah cacat yang sama berulang **lima kali**. Dan keduanya, pada
percobaan pertamanya, **menuduh yang tidak bersalah** — 37 dan 40-an tuduhan yang ternyata
positif palsu.

Itu pelajaran tersendiri, dan mungkin yang paling penting dari kedua dokumen ini: **penjaga
otomatis wajib dibuktikan dua arah sebelum dipercaya.** Bukti bahwa ia menyala saat ada
pelanggaran (kontrol negatif dengan pelanggaran sungguhan, bukan nama karangan yang
dites terpisah), dan bukti bahwa ia diam saat tidak ada. Angka temuan yang belum lewat
kedua bukti itu bukan temuan — ia baru dugaan, dan melaporkannya sebagai temuan akan
menghabiskan waktu orang lain untuk membantahnya.
