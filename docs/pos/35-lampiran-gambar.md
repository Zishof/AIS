# Lampiran gambar — blob, maksimum 500 KB, dikecilkan di klien

Aturan yang berlaku untuk **seluruh** lampiran gambar di POS Desktop dan Android.

| | |
|---|---|
| Penyimpanan | **blob** di basis data, supaya gambarnya persisten |
| Batas ukuran | **500 KB** |
| Yang melebihi | dikecilkan **otomatis di klien** sebelum dikirim |
| Medan wajib-gambar | menolak berkas yang bukan gambar |

## Mengapa dikecilkan di klien, bukan di server

Server tidak mengompresi apa pun. Kalau klien mengirim foto kamera mentah — 4000×3000 piksel,
8 MB — yang tersimpan adalah 8 MB itu, dan setiap kali gambarnya ditampilkan seluruhnya ikut
terkirim lagi.

Pada POS itu terasa langsung: galeri produk tersendat, layar pelanggan yang memuat puluhan
slide berhenti sejenak setiap pergantian, dan basis data tumbuh jauh lebih cepat daripada
transaksinya.

## Satu ambang, satu tempat

```dart
const int maksLampiranGambarBytes = 500 * 1024;   // services/kompresi_gambar.dart
```

Angka ini **tidak diulang** di layar mana pun. Ambang yang tersebar akan berbeda-beda begitu
ada yang lupa mengubah salah satunya.

## Dua pintu, sesuai sifat lampirannya

**`siapkanLampiranGambar`** — untuk medan yang **wajib** gambar (foto produk, slide layar
pelanggan). Menolak yang bukan gambar, mengecilkan yang kebesaran.

**`siapkanLampiranCampuran`** — untuk lampiran serba-guna (faktur boleh PDF). Gambar
dikecilkan; yang bukan gambar dilewatkan apa adanya.

Keduanya fungsi top-level dan murni, supaya dapat dijalankan lewat `compute()` di isolate
terpisah. Decode+encode foto resolusi tinggi berat, dan menjalankannya di isolate utama
membuat UI tersendat tepat saat kasir menunggu.

## Yang diperiksa adalah ISI berkas, bukan namanya

```dart
bool tampaknyaGambar(Uint8List b)   // tanda pengenal JPEG/PNG/GIF/BMP/WEBP
```

Berkas bernama `foto.jpg` yang isinya PDF akan **lolos** penyaring ekstensi `FilePicker` —
penyaring itu hanya melihat nama. Ia baru gagal jauh di hilir, atau lebih buruk, tersimpan
sebagai gambar yang tidak pernah dapat ditampilkan.

Dan tanda pengenal yang benar **tidak menjamin** badan berkasnya utuh, jadi isinya tetap
didekode. Berkas yang diawali `FF D8 FF` tetapi badannya sampah ditolak.

## Gambar yang sudah kecil tidak disentuh

Bila ukurannya sudah di bawah ambang, byte aslinya dikembalikan **apa adanya**. Mengubahnya
menjadi JPEG akan membuang transparansi PNG tanpa memberi manfaat apa pun.

## Cara mengecilkannya

Dari `kompresGambar`, urutannya disengaja:

1. **Turunkan dimensi** bila melebihi 1600 px — foto kamera modern jauh melampaui kebutuhan
   tampilan POS.
2. **Turunkan kualitas JPEG** 90 → 30 bertahap. Biaya kualitas visual lebih murah daripada
   biaya mengecilkan dimensi.
3. **Baru kecilkan dimensi lagi** bila di kualitas terendah masih kebesaran (foto sangat
   detail), sampai lolos atau mentok 400 px.

## Di mana gerbangnya terpasang

| Jalur | Pintu |
|---|---|
| Foto produk (`produk_screen`) | wajib gambar |
| Slide layar pelanggan (`tab_screensaver`) | wajib gambar |
| Lampiran tagihan pengadaan (`pengadaan_tagihan_screen`) | mengikuti penanda `harusGambar` per slot |
| Lampiran SOP (`UnggahLampiranSop`, multipart) | campuran |

Impor Excel (`impor_excel_produk_screen`, `stok_opname_screen`) **sengaja tidak** lewat
gerbang ini — muatannya memang bukan gambar.

> Lampiran SOP sengaja **tidak menggagalkan unggahan** bila kompresi gagal; aslinya tetap
> dikirim. Menggagalkan pekerjaan yang sah karena kompresi bermasalah lebih merugikan
> daripada satu berkas yang kebesaran.

## Sisi server: tabel tenant sudah menyediakan tempatnya

Migrasi tenant v9 membuat `<slug>.foto_produk` dengan kolom `foto bytea`, ditambah
`path`/`url`/`gdrive` untuk data lama yang menyimpan rujukan berkas.

## Penegakan sisi server: batas ini kini jaminan, bukan kesepakatan

`ais.common.PenjagaLampiranGambar` mencerminkan gerbang klien di sisi Java.

| Endpoint | Penegakan |
|---|---|
| `produk_foto_upload` | wajib gambar + 500 KB |
| `layar_pelanggan_slide_upload` | wajib gambar + 500 KB |
| `pengadaan_lampiran_unggah` | slot `gambar` wajib gambar; **setiap** gambar 500 KB, non-gambar tetap 5 MB |
| `DoUpload` (multipart) | 500 KB untuk gambar — **hanya bagi pemanggil POS** |

### Menolak, bukan mengompresi

Server sengaja tidak mengecilkan sendiri. Mengompresi di server membebani CPU yang dipakai
bersama seluruh toko, dan menyembunyikan dari pengguna bahwa gambarnya diubah. Pesan
penolakannya menyebut jalan keluarnya: *"Perbarui aplikasi POS Anda — versi terbaru
mengecilkannya otomatis."*

### Panjang base64 diperiksa sebelum didekode

Mendekode dulu baru memeriksa ukuran berarti muatan 100 MB **sudah terlanjur dialokasikan**
sebelum ditolak; beberapa permintaan seperti itu cukup untuk menjatuhkan kontainer.
`periksaPanjangBase64` menolaknya sambil masih berupa teks, dengan kelonggaran 10% untuk
spasi dan awalan data-URI.

### `DoUpload` sengaja TIDAK ditegakkan menyeluruh

Servlet itu melayani **seluruh AIS** — 19 layar JSP/ZUL dan sedikitnya delapan kelas entitas:
foto mahasiswa, bukti pembayaran, dokumen pindaian, foto admin. Memberlakukan batas POS ke
semuanya akan memutus unggahan yang sah dan sama sekali di luar cakupan aturan ini —
pindaian 300 DPI rutin melampaui 500 KB.

Pembedanya medan form **`token`**: hanya klien POS yang mengirimkannya (lihat
`UnggahLampiranSop`); layar ZK/JSP memakai sesi lewat `Common.getCurrentUser`.

Hanya **dua belas byte pertama** berkas yang dibaca untuk mengenali formatnya, dan ukurannya
diambil dari metadata — muatannya tidak pernah dimuat ke memori.

### Yang masih terbuka

Impor Excel (`impor_excel_produk`, `so_impor_excel`) tidak punya batas ukuran sama sekali di
sisi server. Bukan soal gambar, tetapi lubang memori yang sejenis — belum ditangani.

**Sudah ditangani**: ketiga situs impor kini memanggil `periksaPanjangBase64` sebelum
`decode()`. Rinciannya, berikut batas kejujurannya (muatan JSON-nya sendiri sudah di memori
sebelum sampai ke pemeriksa), ada di
[76-impor-excel-tanpa-batas-dan-anotasi-ganda.md](76-impor-excel-tanpa-batas-dan-anotasi-ganda.md).

## Penjaganya

`apps/ebisnis/test/lampiran_gambar_test.dart` — delapan uji: ambang tepat 500 KB, gambar besar
dikecilkan, gambar kecil tidak disentuh, PDF ditolak pada medan wajib-gambar, berkas menyamar
ditolak lewat pemeriksaan isi, gambar rusak ditolak walau tanda pengenalnya benar, lampiran
campuran melewatkan PDF, dan kelima format dikenali.
