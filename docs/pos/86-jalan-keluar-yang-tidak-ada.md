# 86 — Arah keempat: jalan keluar yang diperintahkan server tetapi tidak ada

Tanggal: 2026-09-02

Dok. 85 menutup dengan menamai arah yang belum pernah dijaga:

> **Dituntut server, tidak pernah dikirim klien.** Belum ada penjaganya.

Batch ini mengukurnya. Hasilnya satu cacat yang sudah berjalan sejak
19-08-2026: server menyuruh pengguna melakukan sesuatu yang tidak disediakan
antarmuka mana pun.

## 1. Cacatnya

`KantinHelper.produkSimpan` menolak produk yang harga modalnya lebih dari 10×
harga jual. Penjaga ini dipasang setelah insiden nyata: satu produk tersimpan
dengan harga modal **5,66 miliar** melawan harga jual 7.500, membuat HPP di
Laporan Laba Rugi membengkak sehingga laba tampak minus miliaran.

Penjaganya benar. Pesannya yang tidak:

> "Periksa kembali harga modal; bila memang disengaja, **simpan ulang dengan
> persetujuan harga modal tinggi**."

Jalan keluarnya `izin_harga_modal_tinggi=true`. **Tidak ada satu pun klien yang
pernah mengirimnya** — tidak Flutter, tidak JSP.

Jadi untuk kasus yang memang sah (barang promo rugi, klaim garansi), pengguna
membaca perintah itu, menyimpan ulang, dan bertemu penolakan yang sama persis.
Lebih buruk lagi: `prosesSimpanMaster` menandai baris outbox sebagai **GAGAL**
pada penolakan bisnis, jadi produknya tertinggal terparkir — bentuk yang sama
dengan nota terparkir di dok. 76.

## 2. Cara menemukannya

Arah keempat diukur begini: kunci yang dibaca server dari request klien
(`request.optString("x")` dan kerabatnya), lalu dicari apakah ada klien yang
pernah mengirim `"x"`.

`opt*` dipilih dengan sengaja sebagai sinyal: itu khas org.json.
`getString("kolom")` tidak dipakai — `ResultSet` punya method senama, dan kueri
SQL native bertebaran di basis kode ini, sehingga hasilnya akan banjir nama
kolom database.

Corong penyaringannya, tiap langkah membuang satu kelas positif palsu yang
**dibuktikan** lebih dulu:

| Langkah | Sisa | Yang dibuang |
|---|---|---|
| kunci dibaca server | 705 | — |
| tak ada klien mengirim | 67 | — |
| hanya penerima request klien | 46 | respons sistem lain (bridge biometrik, H2H bank) |
| bukan kunci tulisan server sendiri | 36 | `kasirExact` — disuntik `payloadAman.put` untuk memaksa kasir non-supervisor hanya melihat transaksinya sendiri, lalu dibaca kembali |
| bukan alias ejaan | 26 | server membaca dua ejaan; `tidak_boleh_cara_pembayaran_lain` adalah cadangan bagi `tidakBolehCaraPembayaranLain` yang memang dikirim |

Sisa 26 itu masih memuat panggilan pihak ketiga yang sah (`paymentdate`,
`va_acc_no` — callback bank) dan modul di luar POS (`prodi1_id`, `gelombang_id`).
`izin_harga_modal_tinggi` ditelusuri tangan sampai tuntas.

## 3. Perbaikannya

**Server** — kode penolakan yang stabil, di samping pesan yang sudah ada:

```java
hasil.put("kode", "HARGA_MODAL_TINGGI");
```

Klien mencocokkan **kode**, bukan teks. Pesannya memuat angka harga dan boleh
berubah kata-katanya tanpa mematahkan apa pun. `ApiException.kode` di klien
memang sudah berasal dari field `kode` respons, jadi tidak ada saluran baru yang
perlu dibuat.

**Klien** (`produk_screen.dart`) — penolakan itu kini menawarkan persetujuan yang
dijanjikan pesannya, lalu mencoba ulang:

```dart
if (e is ApiException && e.kode == 'HARGA_MODAL_TINGGI' &&
    !_izinHargaModalTinggi && mounted) { ... }
```

Persetujuannya **sekali pakai**: direset ke `false` di blok `finally` setelah
percobaan ulang. Kalau tidak, satu persetujuan diam-diam mengizinkan seluruh
penyimpanan berikutnya di form yang sama — gerbangnya mati tanpa ada yang
menyadarinya, dan insiden 5,66 miliar itu bisa terulang lewat pintu yang justru
dibuat untuk memperbaikinya.

Bentuknya sengaja kecil: satu penanda state, satu baris kondisional di payload,
satu blok di `catch`. Body simpan di layar itu literal ~60 baris di dalam
pemanggilan; merestrukturisasinya tanpa bisa mengompilasi Dart di mesin ini
adalah risiko yang tidak sebanding.

## 4. Ujinya dibuat agar bisa merah

`test/harga_modal_persetujuan_test.dart` mengikat pada hal yang dapat patah:
penanda yang dikirim, kode yang dicocokkan, penawaran yang muncul, percobaan
ulang yang terjadi, dan reset sekali-pakainya. Ia juga menegaskan sesuatu
**tidak** ada: pencocokan terhadap teks pesan server.

Itu jawaban langsung atas pelajaran dok. 85, tempat sebuah uji tetap hijau
selama cacatnya hidup karena hanya menegaskan bahwa sebuah nama muncul di
berkas.

## 5. Yang belum dikerjakan, dan sebabnya

* **Dua layar lain juga menyimpan produk** — `kasir_screen.dart` (tambah produk
  cepat) dan `kulakan_bulk_entry_screen.dart` (entri massal). Keduanya masih
  menampilkan penolakan tanpa menawarkan persetujuan. Sengaja tidak disunting
  sekaligus: entri massal memerlukan keputusan tersendiri (satu persetujuan
  untuk seluruh batch, atau per baris?), dan menebaknya di jalur yang menulis
  banyak baris sekaligus lebih berbahaya daripada membiarkannya seperti
  sekarang. Layar Produk adalah tempat harga modal sungguh-sungguh disunting.

* **Penjaganya belum dibuat.** Sama seperti dok. 84: pengukurannya menuntut lima
  saringan berlapis, dan sisa 26 masih bercampur panggilan pihak ketiga yang
  sah. Gerbang di atas dasar seperti itu akan menuduh yang tidak bersalah.
  Yang tertinggal di sini adalah metodenya, tercatat lengkap dengan corong
  angkanya, bukan alat yang berpura-pura pasti.

* **Belum dijalankan.** Sisi Java dikompilasi bersih (`javac -source 1.7`,
  tanpa galat). Sisi Dart **tidak** dapat diuji: mesin ini tidak punya
  toolchain Dart/Flutter. Yang diperiksa: keseimbangan kurung berkas
  (427/427 kurawal, 2272/2272 kurung), penyisipan berada di kelas yang benar
  (`_FormProdukState`), dan ketujuh assertion uji baru benar-benar cocok dengan
  sumber hasil suntingan sementara pola terlarangnya nol.

## 6. Yang dipelajari

**Pesan galat adalah janji.** Kalimat "simpan ulang dengan persetujuan harga
modal tinggi" adalah kontrak antara server dan pengguna, sama mengikatnya
seperti kontrak antarkode — dan kontrak itu tidak dikompilasi, tidak diuji, dan
tidak dijaga siapa pun. Setiap pesan yang menyuruh pengguna melakukan sesuatu
perlu dipastikan ada yang bisa melakukannya.

**Empat arah, tiga terukur.** server→klien (dok. 77), klien→server (dok. 78),
klien menempel dinamis (dok. 85), dan sekarang server-menuntut→klien-tak-kirim.
Yang keempat ini paling sulit diukur karena "klien" ternyata mencakup callback
bank dan server itu sendiri.

## 7. Koreksi atas paragraf 5: `kasir_screen` tidak pernah dapat menabraknya

Paragraf 5 menyebut dua layar yang "masih menampilkan penolakan tanpa menawarkan
persetujuan". Untuk `kasir_screen.dart` itu **salah**.

Tambah-produk-cepat dari Kasir mengirim harga modal yang dipatok nol:

```dart
'harga_beli': 0,
```

Gerbangnya berbunyi `hargaModalFinal > hargaJualFinal * 10.0`. Dengan harga modal
nol, syarat itu tidak pernah dapat terpenuhi. Layar itu tidak menampilkan
penolakan apa pun — ia tidak dapat memicunya.

Keduanya saya kelompokkan menjadi satu karena sama-sama "layar lain yang
menyimpan produk", padahal yang menentukan bukan itu, melainkan **apakah layar
itu mengirim harga modal sungguhan.** `kulakan_bulk_entry_screen.dart`
mengirim `row.hppUnit`, jadi ia memang dapat menabraknya dan penundaannya tetap
berlaku.

Pembebasan `kasir_screen` kini tercatat di `alat/pintu-darurat-tanpa-kunci.py`
lengkap dengan alasannya, sehingga penjaga per-jalur (dok. 91) tidak menuduhnya
lagi — dan alasannya dapat diperiksa ulang siapa pun.

## 8. Koreksi atas paragraf 5: toolchain-nya ada

Paragraf 5 menyatakan sisi Dart "tidak dapat diuji: mesin ini tidak punya
toolchain Dart/Flutter". **Itu salah.** Flutter ada di `C:\opt\flutter`, hanya
tidak berada di PATH.

Sesudah dijalankan (docs/pos/98): `dart analyze` atas berkas yang disunting —
`No issues found!`; seluruh suite `flutter test` — **710 uji lulus**.
