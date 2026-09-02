# 99 — Kontrol kas yang dijaga sebuah komentar

Tanggal: 2026-09-02

Dok. 98 memulihkan kemampuan menjalankan uji sisi klien. Batch ini memakainya
untuk hal pertama yang sebelumnya mustahil: menjalankan **seluruh** suite di
repositori POS, bukan hanya `apps/ebisnis`.

## 1. Semuanya hijau, dan satu dugaan saya salah

| Target | Hasil |
|---|---|
| `apps/ebisnis` | 710 lulus |
| `apps/ecanteen` | 18 lulus, `dart analyze` bersih |
| `core_auth` | 10 lulus |
| `core_db` | 11 lulus |
| `core_update` | 4 lulus |
| `core_billing`, `core_device`, `core_hw`, `core_notif`, `core_sync`, `core_ui` | 1 lulus masing-masing |

Empat paket hanya punya uji templat bawaan Flutter ("adds one to input
values"), dan saya sempat menduga itu berarti kode penting tidak diuji.
Dugaan itu salah: `core_billing`, `core_notif`, `core_sync`, dan `core_ui`
masing-masing berisi **lima baris** kode — rangka kosong. Uji templat pada
paket kosong memang wajar.

`core_hw` berbeda: **584 baris** dengan satu uji, dan uji itu hanya menegaskan
judul sebuah layar.

## 2. Yang ditemukan di dalam `core_hw`

Bukan cacat — sesuatu yang lebih halus. Kodenya menuliskan aturannya sendiri
dengan sangat jelas.

`packages/core_hw/lib/src/buka_laci.dart`:

> JANGAN memindahkan pulsa ini ke dalam `_strukEscPos`. Aliran struk dibaca juga
> oleh jalur pratinjau dan cetak ulang; menaruh pulsanya di sana membuat laci
> terbuka pada cetak ulang struk lama, dan itu celah kontrol kas — siapa pun
> bisa membuka laci lewat menu riwayat.

`apps/ebisnis/lib/screens/struk_screen.dart`:

> Syarat `!modeCetakUlang` ini BUKAN sekadar kerapian: ia adalah kontrol kas.
> Menghapusnya membuat laci dapat dibuka kapan saja oleh siapa pun cukup dengan
> membuka riwayat lalu menekan Cetak Ulang, tanpa ada transaksi maupun uang yang
> masuk.

Kedua aturan itu **masih dipatuhi**. Diperiksa: byte pulsa `0x1B, 0x70` hanya
muncul di `buka_laci.dart`; kedua pemanggilan `bukaLaciKasir` di `struk_screen`
dijaga — yang otomatis oleh `!modeCetakUlang`, yang manual oleh tombol
eksplisit.

Yang tidak ada adalah apa pun yang **menjaganya tetap begitu**. Tidak satu uji
pun di seluruh suite menyebut `modeCetakUlang`, `bukaLaciKasir`, maupun
`PengaturanLaci`.

## 3. Komentar yang benar tetap komentar

Kedua komentar itu ditulis dengan baik: menyebut akibatnya, menyebut jalurnya,
bahkan mendahului pertanyaan "kenapa tidak disederhanakan saja". Tetap saja ia
tidak dikompilasi, tidak dijalankan, dan tidak menghentikan siapa pun.

`if (!modeCetakUlang)` yang membungkus satu pemanggilan terlihat persis seperti
kerapian yang bisa diratakan. Seseorang yang merapikan berkas ini enam bulan
dari sekarang, tanpa membaca dua puluh baris komentar di atasnya, akan
menghapusnya dengan niat baik — dan tidak ada yang merah.

`test/laci_kas_cetak_ulang_test.dart` menegaskan empat hal yang dapat patah:

1. penjaga `!modeCetakUlang` ada;
2. pemanggilan `bukaLaciKasir` berada **di dalam** penjaga itu, bukan sebelum
   atau sesudahnya;
3. pulsa laci tidak pernah muncul di `struk_screen.dart` dalam ejaan mana pun;
4. **alasan kontrol kasnya tetap tertulis** — karena kalau alasannya hilang,
   penjaganya kembali tampak seperti kerapian.

Butir keempat tidak lazim untuk sebuah uji, dan justru itu intinya: yang
melindungi aturan ini selama ini adalah kalimatnya, jadi kalimatnya ikut dijaga.

## 4. Dibuktikan dapat merah

Penjaga diganti `if (true) {`, dan suntingannya diverifikasi lebih dulu
(`tersisa = 0` — pelajaran dok. 98, tempat kontrol negatif pertama gagal
menyunting dan hampir terbaca sebagai lulus):

```
00:00 +0 -1: pembukaan laci otomatis dijaga syarat !modeCetakUlang [E]
  penjaga cetak-ulang hilang: laci akan terbuka dari menu riwayat tanpa ada uang masuk
```

Dipulihkan, `git status` kosong, kembali `+4 All tests passed`.

## 5. Yang dipelajari

**Komentar yang baik menandai tempat yang layak diuji.** Kalimat "ini BUKAN
sekadar kerapian" adalah pengakuan penulisnya sendiri bahwa kodenya tampak
seperti sesuatu yang boleh dihapus. Di mana pun kalimat seperti itu muncul, di
situ ada aturan yang bergantung pada ingatan orang.

**Menjalankan uji yang selama ini tidak dijalankan tidak menemukan kerusakan —
dan itu tetap berharga.** Sepuluh suite hijau berarti tanah yang selama ini
diasumsikan kini terbukti. Yang ditemukan justru bukan di hasil ujinya,
melainkan di tempat yang tidak diuji sama sekali.
