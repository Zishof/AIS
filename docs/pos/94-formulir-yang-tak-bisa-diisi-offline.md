# Formulir yang boleh disimpan offline, tetapi tidak bisa diisi

Produk, Kulakan, dan Grup Produk sudah lama **menyimpan** lewat antrean lokal,
jadi ketiganya terbaca "sudah local-first". Yang luput: dropdown referensi di
dalam formulirnya — satuan/UOM, pemasok, grup produk, promo — mengambil
**langsung ke jaringan**, dan saat gagal jatuh menjadi daftar **kosong**.

Formulirnya tetap terbuka, dan tetap bisa disimpan. Yang hilang justru isinya.
Ini local-first terbalik: tulisannya diselamatkan, sedangkan bacaan yang
menentukan isi tulisan itu tidak.

Kegagalannya **senyap**. Tidak ada galat — hanya pilihan yang tidak ada, lalu
dokumen tersimpan tanpa pemasok, atau produk tersimpan dengan satuan yang bukan
dimaksud. Baru terasa jauh di hilir, saat dokumennya dipakai.

## Yang diubah

| Aksi | Layar | Kunci cache |
|---|---|---|
| `uom_list` | Keranjang (pilih satuan jual) | `master:uom:aktif` |
| `uom_list` | Produk (formulir) | `master:uom:termasuk_nonaktif` |
| `grup_produk_list` | Produk (formulir) | `master:grup_produk` |
| `diskon_list` | Grup Produk (formulir) | `master:diskon_grup` |
| `penyedia_list` | Kulakan + Entri Massal | `master:penyedia:awal` |
| `toko_filter_list` | Kerangka aplikasi | `master:toko_filter` |

## Tiga keputusan yang tidak seragam, dan alasannya

**Satu aksi, dua kunci cache.** `uom_list` dipanggil dari dua tempat dengan
badan permintaan **berbeda**: layar Produk meminta satuan **nonaktif** juga
(supaya produk lama yang masih memakainya tetap dapat disunting), keranjang
hanya yang aktif. Memakai satu kunci untuk keduanya akan menyuguhkan satuan
**mati** sebagai pilihan jual — tanpa tanda apa pun, karena kedua pemuatan
sama-sama "berhasil". Aturan umumnya sudah tertulis di JavaDoc
`objekDenganCache`; kasus ini contoh konkretnya.

**Pencarian tidak di-cache, daftar awal iya.** `penyedia_list` menerima kata
kunci. Yang disimpan hanya panggilan **tanpa** kata kunci — itulah yang muncul
begitu sheet pemilih dibuka, dan tanpanya entri kulakan offline kehilangan
pemasoknya. Menyimpan hasil pencarian per kata kunci hanya menumpuk snapshot
yang tak pernah terpakai lagi, dan menyajikannya kembali membuat kata kunci yang
**berbeda** tampak cocok.

**Daftarnya boleh dari snapshot, wewenangnya tidak.** Balasan
`toko_filter_list` membawa dua hal berbeda: daftar toko, dan bendera
`bolehSemuaToko`. Daftarnya dipulihkan dari cache; benderanya **sengaja tidak
disentuh**. Menaikkan wewenang dari cache akan menghidupkan lingkup "semua toko"
bagi pengguna yang haknya sudah dicabut — justru pada saat server tidak berada
di jalur untuk menolaknya. Ini penerapan lain dari aturan yang sudah dipakai di
sapuan hak akses: **hak hanya diperbarui dari emisi SERVER** (lihat
[84-sapuan-hak-akses-tombol.md](84-sapuan-hak-akses-tombol.md)).

## Pembantu yang salah tempat

`muatDaftarTokoFilter` di `app_shell.dart` mula-mula dialihkan ke
`MasterOffline.objekDenganCache`. Itu **salah**, dan cara ketahuannya layak
dicatat: empat uji widget yang sama sekali tidak berhubungan — dua uji tata
letak AppShell, dua uji sidebar — mendadak jatuh dengan
*"A Timer is still pending even after the widget tree was disposed"*.

Sebabnya: `MasterOffline` memasang **timer flush outbox mutasi master** begitu
dipanggil. Memuat dropdown filter toko adalah pembacaan murni milik kerangka
aplikasi; menariknya lewat `MasterOffline` menyalakan antrean **tulis** sebagai
efek samping. Perbaikannya membaca `cache_referensi` langsung lewat `CoreDb` —
pola yang memang sudah dipakai `riwayat_penjualan_screen` dan
`member_biometric_panel` untuk alasan sejenis.

Pelajarannya: **pembantu offline itu bukan barang netral.** Ia membawa daur
hidup, bukan sekadar fungsi baca. Untuk jalur baca murni di lapisan kerangka,
cache dibaca langsung.

## Lima dari enam antrean ternyata sudah selesai

Antrean yang dibawa dari sapuan sebelumnya menyebut enam layar "masih
online-only". Diperiksa satu per satu, **lima** ternyata sudah beres — sebagian
oleh pekerjaan sendiri, sebagian oleh sesi paralel:

| Layar | Keadaan sebenarnya |
|---|---|
| `riwayat_penjualan_screen` | sudah cache-dulu (pola `daftarCacheDulu` ditulis manual) |
| `pengajuan_anda_screen` | dua-duanya sudah beralasan tertulis: dasbor agregat & antrean kerja |
| `member_biometric_panel` | sudah membaca cache lebih dulu; hanya pendaftarannya online |
| `diskon/tab_monitor_diskon` | dropdown sudah di-cache; angka monitor sengaja daring |
| `konfigurasi/tab_screensaver` | sudah punya fallback lokal sejak awal |

Yang tersisa nyata hanya `keranjang_screen`, dan **bukan** ketiga aksinya:

- `cara_bayar_list` **sudah** aman offline — bukan lewat cache-nya sendiri,
  melainkan lewat `Sesi.caraBayar` yang berasal dari aksi `konfigurasi`, dan
  aksi itu sudah memakai `objekDenganCache`. Jalurnya tidak langsung, jadi mudah
  disangka celah; dicatat di sini supaya tidak "diperbaiki" dua kali.
- `diskon_manual_list` **bukan** data master melainkan **evaluasi**: server
  menghitung promo mana yang layak untuk isi keranjang saat ini. Hasilnya
  bergantung pada keranjang yang berubah tiap detik; menyimpannya tidak
  bermakna. Jalur gagalnya sudah turun dengan sopan ke diskon bebas.
- `uom_list` — inilah satu-satunya celah nyata di layar itu.

**Antrean audit bukan bukti.** Daftar dari sapuan sebelumnya perlu diperiksa
ulang terhadap keadaan sekarang sebelum dikerjakan, bukan dikerjakan begitu
saja: lima dari enam sudah kedaluwarsa.

## Penjaga

`test/dropdown_referensi_lokal_test.dart` (repositori Flutter), 6 uji.

Uji pertamanya sempat **terlalu longgar**: ia hanya memastikan berkasnya
mengandung `daftarDenganCache(` di suatu tempat. Berkas-berkas itu sudah memakai
`MasterOffline` untuk daftar lain, jadi penjaga selonggar itu tetap hijau
walaupun justru aksi yang dimaksud dikembalikan ke jalur langsung. Sekarang nama
aksinya **diikat pada pemanggilannya**: `daftarDenganCache('<aksi>'`.

Uji wewenang toko juga sempat salah ukur — ia menghitung kemunculan nama
`bolehSemuaToko`, padahal nama itu **dibaca** di lima tempat untuk merender
pemilih lingkup dan hanya **disetel** di satu tempat. Yang dijaga sekarang
khusus penyetelannya (`Sesi.instance.bolehSemuaToko =`), dan posisinya harus
sebelum blok pemulihan snapshot.

Keduanya dibuktikan dengan **uji negatif**: perubahan dirusak sementara, ujinya
jatuh dengan pesan yang dimaksud, lalu berkasnya dipulihkan dan diverifikasi
byte-identik. Lulusnya sebuah uji tidak pernah cukup untuk mempercayainya.

## Catatan alat

Pemeriksaan akhiran baris memakai `head -c 4000 | grep -q $'\r'` melaporkan
seluruh berkas **LF**; pembacaan langsung lewat Python menunjukkan lima dari enam
sebenarnya **CRLF**. Anchor patch pertama gagal karenanya. Skrip patch kini
mendeteksi akhiran baris **per berkas** — repositori ini memang bercampur
(`grup_produk_screen.dart` LF, sisanya CRLF), dan menulis akhiran yang salah
membuat seluruh berkas tampak berubah di diff.

Dua jebakan lama juga terulang dan langsung dikenali: escape backslash runtuh
lewat heredoc bersarang (bangun dengan `chr(92)` / `chr(13)`), dan metode `.NET`
`[IO.File]::ReadAllText` memakai **cwd proses**, bukan `Set-Location` — wajib
path absolut.
