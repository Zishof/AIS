# 76 — Peringatan stok yang terbaca, dan nota yang terparkir diam-diam

Lanjutan [73](73-stok-minus-tiga-nilai-dan-pemulihan-member.md) dan
[75](75-halaman-pesanan-tiga-celah-sunyi.md). Tiga celah yang tersisa sesudah "STOK
MINUS" diperbaiki — dan **dua di antaranya berbentuk sama persis** dengan yang baru
diperbaiki sehari sebelumnya.

Itu sendiri layak dicatat: pelajaran dokumen 75 bagian 6 ("perbaikan yang dipasang pada
satu dari beberapa jalur kembar adalah bom waktu") terbukti berulang **dalam satu hari**,
oleh tangan yang baru saja menuliskannya.

---

## 1. `peringatanStok` tidak dibaca klien Flutter sama sekali

Pada [73](73-stok-minus-tiga-nilai-dan-pemulihan-member.md) bagian 1.6, server mulai
menitipkan `peringatanStok` pada respons **sukses** — kekurangan stok yang tidak
memblokir tidak lagi tenggelam di audit log. Pada [75](75-halaman-pesanan-tiga-celah-sunyi.md)
bagian 3, sisi JSP dibuat membacanya.

POS Desktop/Android tidak pernah ikut: `grep -rn "peringatanStok" lib/` mengembalikan
**nol**. Jadi untuk dua dari tiga kanal, peringatan itu tetap sama saja tidak pernah ada.

### 1.1 Tiga titik, karena jalurnya memang tiga

| Titik | Keadaan | Yang dilakukan |
|---|---|---|
| `pesanan_screen` — bayar massal | responsnya dibuang (`await …;` tanpa menampung hasil) | hasil ditampung; peringatan didaftar terpisah dari `gagal`, karena ini **bukan** kegagalan |
| `pesanan_screen` — bayar tunggal | snackbar sukses biasa | peringatannya **menggantikan** pesan sukses — yang perlu ditindaklanjuti tidak boleh kalah mencolok dari kabar baiknya |
| `transaksi_outbox_service` | tidak disimpan | ikut ditulis ke `hasil_server_json` |
| `struk_screen` | — | ditampilkan begitu pengakuan server tiba |

### 1.2 Kenapa checkout POS butuh dua langkah

Checkout POS bersifat **lokal-dulu**: struk tercetak tanpa menunggu server, dan respons
`bayar` baru tiba di `TransaksiOutboxService` — bisa detik berikutnya, bisa sepuluh menit
kemudian, jauh setelah kasir menutup layar.

Karena itu peringatannya harus **disimpan dulu** ke baris outbox, baru **ditampilkan**
oleh layar struk yang memang sudah memantau baris itu untuk merekonsiliasi angka. Kalau
hanya ditampilkan saat respons tiba, peringatan untuk transaksi yang tersinkron
belakangan tidak akan pernah dilihat siapa pun.

---

## 2. Nota yang terparkir GAGAL oleh penolakan yang ternyata keliru

Ini yang menyangkut uang yang **sudah diterima kasir**.

```dart
static const Set<String> kodePenolakanPermanen = {
  'DATA_TIDAK_LENGKAP', 'TIDAK_DITEMUKAN', 'PESANAN_PERLU_DIMUAT_ULANG',
  'STOK_TIDAK_CUKUP', 'PRODUK_KADALUARSA',
};
```

Retry otomatis **hanya membaca baris berstatus PENDING**. Jadi setiap transaksi luring
yang ditolak gerbang stok langsung diparkir `GAGAL` dan tidak pernah dicoba lagi dengan
sendirinya.

Untuk penolakan yang sah, itu benar — mengirim ulang hanya menghasilkan penolakan yang
sama. Tetapi sejak **r77493 (16-08-2026)** sampai perbaikannya **02-09-2026**, gerbang itu
menolak produk yang tidak pernah dikunci admin sama sekali. Transaksi yang diparkir
karenanya adalah **penjualan yang sah**: uangnya sudah diterima, struknya sudah tercetak,
dan nilainya tidak pernah sampai ke server.

**Perbaikan di server hanya menghentikan yang baru.** Baris yang sudah terparkir tetap
diam sampai ada yang menekan "Kirim Ulang" — di tiap perangkat kasir, satu per satu —
dan tidak ada yang tahu harus menekannya.

Itu persis bentuk kehilangan yang sudah dicatat sebagai aturan 3 di kepala
`transaksi_outbox_service.dart`: *"kegagalan yang tidak terlihat jauh lebih mahal
daripada kegagalan yang berisik."*

### 2.1 Pemulihan sekali-jalan, sengaja sempit

`pulihkanTerparkirPenolakanStok()` berjalan sekali per perangkat saat service dimulai.

| Keputusan | Alasan |
|---|---|
| Hanya baris yang **pesan galatnya** berbunyi penolakan stok | Kadaluarsa, sesi kas, data tidak lengkap adalah penolakan yang SAH — membangunkannya hanya menghasilkan kegagalan yang sama sekali lagi |
| Dicocokkan pada **potongan** teks, bukan kalimat penuh | Kalimat penolakannya sendiri ikut diperbaiki di [73](73-stok-minus-tiga-nilai-dan-pemulihan-member.md) bagian 1.6; baris lama menyimpan bunyi yang lama, baris yang lebih baru bunyi yang baru — keduanya harus dikenali |
| Penanda dipasang **sesudah** berhasil | Bila prosesnya gagal di tengah, percobaan berikutnya masih menemukan barisnya |
| Jejak ditulis ke error log **walau nol** | Supaya "apakah pemulihan itu pernah jalan di perangkat ini?" punya jawaban, bukan tebakan |
| Kegagalan pemulihan tidak menggagalkan start-up | Dan penandanya tidak dipasang di jalur galat, jadi masih dicoba lagi |

**Kenapa aman dari transaksi ganda:** pengiriman ulang memakai `kode_unik` asli, dan
server menolak duplikat lewat `DUPLIKAT_KODE_TRANSAKSI` yang oleh pengirim sudah
diperlakukan sebagai "sudah ada di server". Baris yang ternyata sempat tersimpan akan
ditandai SYNCED, bukan tercatat dua kali.

Bila gerbangnya masih menolak dengan alasan yang sah, barisnya kembali `GAGAL` sendiri
dengan sebab yang tercatat. Tidak ada yang hilang; yang berubah hanya: sekarang ia
**dicoba**.

---

## 3. Verifikasi Otomatis hanya menyimpan sebab TERAKHIR

```js
pesanGagalTerakhir = resBayar.description || …;   // ditimpa setiap iterasi
```

Bila lima pesanan gagal dengan lima sebab berbeda, operator hanya melihat sebab pesanan
**terakhir** — dan justru pesanan yang gagal duluan itulah yang tidak ketahuan sebabnya.

Kedua loop "Bayar Semua" sudah diubah menjadi daftar pada
[75](75-halaman-pesanan-tiga-celah-sunyi.md); dua loop Verifikasi Otomatis terlewat.
Sekarang keduanya mendaftar seluruh sebab, masing-masing dengan kode pesanannya.

---

## 4. Berkas yang berubah

| Berkas | Perubahan |
|---|---|
| `apps/ebisnis/lib/screens/pesanan_screen.dart` | peringatan stok pada bayar massal + tunggal |
| `apps/ebisnis/lib/services/transaksi_outbox_service.dart` | `peringatanStok` disimpan; `pulihkanTerparkirPenolakanStok()` + predikat `terparkirPenolakanStokKeliru()` |
| `apps/ebisnis/lib/screens/struk_screen.dart` | peringatan ditampilkan saat pengakuan server tiba |
| `apps/ebisnis/test/pulih_penolakan_stok_test.dart` | **baru** |
| `modul/kantin/pesanan/_draft_pesanan_anggota.jsp` | Verifikasi Otomatis mendaftar seluruh sebab (SVN r82951) |

---

## 5. Hasil uji

### 5.1 `pulih_penolakan_stok_test.dart` — 11 dari 11 lulus

Yang diuji adalah **pencocokan teksnya**, karena di situlah kesalahan paling mudah lolos
tanpa terlihat — terlalu longgar membangunkan penolakan yang sah, terlalu ketat tidak
menyelamatkan apa pun.

| Kelompok | Isi |
|---|---|
| Dibangunkan | bunyi produksi 01-09-2026 apa adanya; bunyi versi BARU (baris yang terparkir sesudah server diperbarui tetapi sebelum aplikasi kasir menyusul); tidak peduli besar-kecil huruf |
| TIDAK dibangunkan | kadaluarsa, batch FEFO, sesi kas, data tidak lengkap, kelipatan grosir, pesan kosong/null |
| Kontrak pendukung | `STOK_TIDAK_CUKUP` memang permanen — **kalau suatu hari dikeluarkan dari daftar itu, retry biasa sudah menjemputnya dan pemulihan ini tidak lagi punya alasan untuk ada**; uji inilah yang akan mengingatkan |

### 5.2 Uji ini sempat merah oleh kesalahannya sendiri

Versi pertama memakai *tiruan* `ApiException`. `dapatDicobaUlang` memeriksa tipenya lebih
dulu (`if (error is! ApiException) return true`), sehingga tiruan apa pun lolos sebagai
"boleh dicoba ulang" — dan ujinya akan **mengiyakan hal yang tidak diujinya**.

Ujinya gagal, sebabnya ditemukan, dan alasannya kini tertulis di berkas ujinya supaya
tidak diulang. Uji yang lulus karena salah alat lebih berbahaya daripada tidak ada uji:
ia memberi rasa aman tanpa memberi jaminan.

### 5.3 Sisanya

- `TesKontrakPesananJsp`: **16/16** lulus.
- `TesAturanStokMinus`: **8/8** lulus.
- Sintaks JS JSP: **BERSIH** (`node --check`, 1947 baris).
- Regresi Flutter penuh: **614** lulus.

### 5.4 Yang BELUM diuji

- **Pemulihan bagian 2 belum pernah dijalankan pada data sungguhan.** Yang terbukti
  adalah aturan pencocokannya; berapa nota yang benar-benar terbangun di lapangan baru
  ketahuan setelah aplikasi kasir diperbarui. Jejaknya akan ada di Log Error tiap
  perangkat (`sumber: outbox-pulih-stok`).
- **Tampilan snackbar/dialog peringatan belum dilihat mata.** Yang diperiksa `flutter
  analyze` dan uji kontrak adalah bentuk kodenya.

---

## 6. Yang perlu diperiksa lain kali

**Perbaikan yang menambah data ke respons belum selesai sampai ada yang membacanya —
di SEMUA kanal.** Sistem ini punya tiga: JSP, POS Desktop/Android, dan ZK. Menambah
field lalu menyambungnya di satu kanal terasa seperti selesai, dan dua kanal lain diam
tanpa satu pun tanda.

Uji kontrak JSP sudah memakai bentuk yang tepat untuk ini — **hitung, jangan daftar**
(`jumlah id_member == jumlah action:"bayar"`). Bentuk yang setara untuk lintas-kanal
belum ada, dan itulah celah yang masih terbuka: tidak ada satu pun uji yang bisa
mengatakan *"field ini dikirim server tetapi tidak dibaca kanal mana pun"*.

Sampai ada, aturannya harus dijalankan dengan tangan: setiap `hasil.put("...")` baru
pada respons sukses ditelusuri ke ketiga kanal sebelum dianggap selesai.
