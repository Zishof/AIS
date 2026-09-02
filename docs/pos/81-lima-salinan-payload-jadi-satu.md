# 81 — Lima salinan payload bayar jadi satu

[80](80-satu-saluran-peringatan-pasca-transaksi.md) ditutup dengan satu ukuran:

> Pertanyaannya bukan *"apakah field ini sudah dibaca?"* — dua penjaga sudah menjawabnya
> otomatis — melainkan **"berapa titik yang harus disentuh untuk menambah satu lagi?"**

Ukuran itu diterapkan pada halaman yang sama, dan jawabannya **lima**.

---

## 1. Bukti bahwa lima salinan itu memang merugikan

Halaman Pesanan merakit payload aksi `bayar` di **lima tempat** (bayar manual, 2× "Bayar
Semua", 2× Verifikasi Otomatis). Salinan itu sudah menyebabkan **tiga cacat terpisah**:

| Dok. | Cacat | Bentuknya |
|---|---|---|
| [75](75-halaman-pesanan-tiga-celah-sunyi.md) | `id_member` hilang | di kelima-limanya **sekaligus** |
| [75](75-halaman-pesanan-tiga-celah-sunyi.md) | pelaporan kegagalan | diperbaiki hanya di **1 dari 5** loop |
| [80](80-satu-saluran-peringatan-pasca-transaksi.md) | `peringatanStok` | menuntut **5 titik** disentuh |

Ketiganya bukan kelalaian berbeda — mereka **satu penyebab yang muncul tiga kali**.

---

## 2. Yang disatukan, dan yang sengaja TIDAK

| | |
|---|---|
| **Disatukan** | `buatPayloadBayar()` — perakitan data payload; `itemDariRincianDraft()` — pemetaan item (4 dari 5 jalur identik) |
| **TIDAK disatukan** | alur tiap jalur, progress bar, modal hasil, dan awalan `kodeUnik` (`POS-`, `POS-MASSAL-`, `POS-AUTO-`) |

Batas itu disengaja. Awalan `kodeUnik` dan `kanalCheckout` **memang berbeda** — server
membacanya (`KantinHelper.finalisasiOtomatis`) untuk menolak finalisasi otomatis tanpa
data member. Menyatukannya berarti mengubah perilaku halaman pembayaran, bukan
merapikannya.

Menyatukan seluruh alur kelima loop terasa lebih "bersih", tetapi itu menuntut menguji
perilaku halaman pembayaran di peramban — yang tidak bisa saya lakukan di sini. Perakitan
data adalah bagian yang dapat diverifikasi sepenuhnya tanpa peramban, dan justru bagian
itulah yang menyebabkan ketiga cacat di atas.

**Biaya menambah field payload berikutnya: dari 5 suntingan menjadi 1 baris.**

---

## 3. Verifikasi: payload dijalankan, bukan dibaca

Ini halaman **pembayaran**. Membaca diff lalu menyatakan "sepertinya sama" tidak cukup —
satu field yang hilang diam-diam berarti transaksi tercatat salah tanpa ada yang mengeluh,
persis kelas kesalahan yang dikejar sepanjang [45](45-penyaring-dasbor-dan-layani-semua.md)–[80](80-satu-saluran-peringatan-pasca-transaksi.md).

[`alat/banding-payload-jsp.py`](alat/banding-payload-jsp.py) mengambil literal payload
**lama** dari salinan sebelum disunting, perakit + pemanggil **baru** dari berkas hasil,
menjalankan keduanya di `node` dengan data tiruan yang sama, lalu membandingkan sebagai
JSON berkunci-urut:

```
  OK    payload #1 IDENTIK (10 field)
  OK    payload #2 IDENTIK (9 field)
  OK    payload #3 IDENTIK (9 field)
  OK    payload #4 IDENTIK (9 field)
  OK    payload #5 IDENTIK (10 field)

SELURUH PAYLOAD IDENTIK
```

### 3.1 Pembandingnya dibuktikan bisa gagal — dua kali

Hasil "IDENTIK" tidak berarti apa-apa dari alat yang belum pernah terbukti menolak.

**Kontrol 1** — `id_member` dihilangkan dari perakit:

```
  GAGAL payload #1 BERBEDA
        hilang di versi baru : ['id_member']
  ... (kelima-limanya)
```

**Kontrol 2** — `kanalCheckout` diberi nilai keliru:

```
  GAGAL payload #1 BERBEDA
        beda nilai kanalCheckout   lama=otomatis_halaman baru=salah_nilai
  OK    payload #2 IDENTIK
  ... (empat sisanya tetap OK)
```

Kontrol kedua lebih tajam daripada yang pertama: ia membuktikan alatnya menyalahkan
**payload yang tepat**, bukan sekadar menyalakan alarm untuk semuanya.

---

## 4. Uji kontrak ikut berubah bentuk — dan jadi lebih kuat

`PesananPayloadKontrakUat` sempat **2 GAGAL**: ia menuntut "4 loop memakai
`id_member: draft.anggota_koperasi`", yang kini hanya ada di argumen pemanggil.

Yang dijaga **bergeser** — dari *"kelima salinan membawa id_member"* menjadi *"salinannya
memang tinggal satu, dan tiap jalur memanggilnya dengan benar"*. Bentuk baru itu menambah
tiga penjagaan yang **tidak mungkin ada sebelumnya**:

- `action: "bayar"` hanya boleh muncul **sekali** — duplikasi tidak bisa kembali diam-diam
- **hanya dua** jalur mengirim `kanalCheckout` — perbedaan yang disengaja itu kini dikunci
- perakitnya mengirim `kanalCheckout` **hanya bila diminta**, bukan selalu

**21/21 lulus.**

---

## 5. Berkas yang berubah

| Berkas | Perubahan |
|---|---|
| `modul/kantin/pesanan/_draft_pesanan_anggota.jsp` | `buatPayloadBayar()` + `itemDariRincianDraft()`; lima jalur memanggilnya |
| `docs/pos/alat/banding-payload-jsp.py` | **baru** — pembanding payload sebelum/sesudah |

Berkasnya menyusut 1981 → 1949 baris.

---

## 6. Hasil uji

| Uji | Hasil |
|---|---|
| `alat/banding-payload-jsp.py` | **5/5 payload IDENTIK**, pembandingnya terbukti bisa gagal |
| `PesananPayloadKontrakUat` | **21/21** |
| `alat/cek-sintaks-jsp.py` | BERSIH, 1949 baris |
| `alat/field-tanpa-pembaca.py` | BERSIH |
| `alat/payload-tanpa-pembaca.py` | BERSIH |

### 6.1 Yang BELUM diuji

- **Halaman dibuka di peramban.** Yang terbukti: payloadnya identik byte demi byte pada
  data tiruan, sintaksnya sah, dan bentuk kodenya terkunci uji kontrak. Yang belum: klik
  sungguhan pada tombol Bayar.
- **Data tiruan hanya satu bentuk.** Perbandingannya memakai satu set nilai (termasuk item
  ber-`null` untuk menguji `parseFloat(x||0)`), bukan seluruh kemungkinan.

---

## 7. Yang perlu diperiksa lain kali

Ukuran dari [80](80-satu-saluran-peringatan-pasca-transaksi.md) terbukti menemukan
pekerjaan yang nyata: *lima salinan yang sudah menyebabkan tiga cacat.* Ia layak
diteruskan sebagai pertanyaan rutin, bukan sekadar penutup satu dokumen.

Tetapi ada batasnya, dan batas itu juga terlihat di sini: **menyatukan yang berbiaya
banyak titik tidak selalu benar.** Kelima loop halaman ini juga berbiaya banyak titik,
namun menyatukannya berarti menyentuh alur pembayaran yang tidak dapat saya uji di
peramban. Yang disatukan hanya bagian yang dapat diverifikasi sepenuhnya tanpa
peramban — dan kebetulan justru bagian itulah yang selama ini menjadi sumber cacatnya.

Aturannya: **satukan sejauh verifikasinya masih bisa dijalankan.** Di luar itu, catat
sebagai utang yang terlihat — seperti dua daftar `UTANG` pada kedua penjaga — bukan
kerjakan dengan mengandalkan pembacaan mata.
