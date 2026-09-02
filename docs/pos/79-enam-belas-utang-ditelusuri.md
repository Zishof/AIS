# 79 — Enam belas utang field, ditelusuri satu per satu

[77](77-gerbang-oversell-dan-penjaga-field-yatim.md) membekukan 16 field yang dikirim
server tanpa pembaca di kanal mana pun, dengan catatan "belum ditelusuri". Dokumen ini
menelusuri semuanya.

Perkiraan saya sebelum mulai: nilainya kecil, isinya diagnostik. **Perkiraan itu meleset
untuk empat di antaranya** — dan salah satunya membawa niat yang tertulis di komentar
kodenya sendiri, lalu tidak tercapai.

Vonis tiap field kini tersimpan di daftar `UTANG` pada
[`alat/field-tanpa-pembaca.py`](alat/field-tanpa-pembaca.py) — bukan hanya di dokumen ini,
supaya orang yang menjalankan alatnya membaca alasannya di tempat yang sama.

---

## 1. Kelompok 1 — peringatan yang tidak sampai ke siapa pun (5)

Kelimanya berbentuk **sama persis** dengan `peringatanStok` sebelum
[76](76-peringatan-stok-terbaca-dan-nota-terparkir.md): transaksi diterima, ada sesuatu
yang perlu direkonsiliasi, kliennya diberi tahu — dan tidak ada yang membaca.

Empat di antaranya lahir dari **insiden Toko Al Bahjah** yang dicatat di komentar kodenya
sendiri, termasuk yang dilaporkan ke saya di awal sesi ini.

### 1.1 `sesi_kas_sudah_tutup` + `sesi_kas_asal` — temuan terkuat

`KantinHelper` baris 1385-1390, komentarnya menyatakan niatnya sendiri:

> *"Selisih itu tidak boleh terjadi diam-diam: dicatat ke audit **dan dikembalikan ke klien
> supaya bagian keuangan punya jejaknya**."*

Transaksi masuk ke sesi kas yang **sudah ditutup** — artinya rekap sesi yang sudah dicetak
dan mungkin sudah dicocokkan dengan uang fisik kini tidak lagi cocok dengan isi basis data.

Servernya menuliskannya. Tidak ada klien yang membacanya. **Kalimat "supaya bagian keuangan
punya jejaknya" tidak pernah terwujud** — jejaknya hanya ada di `System.err` server.

`sesi_kas_asal` adalah pendampingnya: tanpa kode sesi asal, kejadian itu tidak dapat
ditelusuri ke sesi mana pun.

### 1.2 `sesi_kas_tidak_dikenal` dan `sesi_kas_direkonsiliasi`

Keduanya menandai **perpindahan sesi yang tidak diberitahukan**:

| Field | Keadaan |
|---|---|
| `sesi_kas_tidak_dikenal` | kode sesi dari payload tidak dikenal server; transaksi tetap diterima dan diikat ke sesi yang sedang terbuka (insiden 61 transaksi, 21-08-2026) |
| `sesi_kas_direkonsiliasi` | server menemukan sesi yang benar-benar terbuka pada waktu transaksi, lalu mengikat ulang ke sana |

Keputusan menerima transaksinya **benar** — itu justru perbaikan dari insiden 61 transaksi
tertahan. Yang hilang hanya pemberitahuannya: kasir tidak pernah tahu transaksinya masuk ke
sesi yang berbeda dari yang ia kira.

### 1.3 `peringatanPengajuanLimit`

Sudah dinilai di [77](77-gerbang-oversell-dan-penjaga-field-yatim.md) bagian 2.3. Tetap
tidak dibayar dengan alasan yang sama: jarang, dan sudah tercatat permanen di Error Log
server lewat `ErrorAuditUtil` berikut jejak tumpukannya.

---

## 2. Kelompok 2 — pendamping yang saudaranya memang dibaca (5)

Untuk kelompok ini saya **memeriksa saudaranya**, tidak menebak:

| Field | Saudaranya | Terbaca klien |
|---|---|---|
| `idSesiKas` | `kodeSesiKas` | 2 Dart + 1 JSP |
| `satuanDasarId` | `satuanDasarNama` | 1 Dart |
| `waktuHargaBeliTerakhir` | `hargaBeliTerakhir` | 2 Dart |
| `versiStok` | `stokAkhir` / `id` | 2 Dart |
| `pengajuanLimitId` | kode unik nota | `keranjang_screen.dart` |

Klien memakai saudaranya, bukan nilai ini. Tidak ada yang hilang.

`versiStok` layak disebut tersendiri: namanya membuatnya terbaca seperti penjaga
*lost-update*. Isinya ternyata hanya `so.getId()` — id baris stok opname yang juga sudah
dikirim sebagai `id`. **Nama yang menyesatkan, bukan bahaya** — tetapi nama seperti itu
mengundang orang membangun asumsi keliru di atasnya.

---

## 3. Kelompok 3 — angka turunan dan gema (6)

| Field | Isinya |
|---|---|
| `draftDiperbarui` | `!draftBaru`; klien sudah tahu ia mengirim draft atau tidak |
| `ringkasanBerjalan` | objek bersarang; klien membaca `totalTunai`/`totalNonTunai`/`kasSaatIni` yang sudah diratakan di sebelahnya |
| `siapDisimpan` | boolean turunan dari validasi yang isinya sudah dikirim terpisah |
| `totalGrup` | `grup.length()` |
| `statusRincian` | gema parameter `status` dari permintaannya sendiri |
| `totalTerjualBersih` | `totalTerjual - totalRetur` |

---

## 4. Batas alatnya yang baru ketahuan lewat penelusuran ini

`totalTerjualBersih` awalnya saya golongkan "pendamping" — sampai saya periksa saudaranya:

```
totalTerjual   dart=0  jsp=0
totalRetur     dart=0  jsp=0
```

**Kedua saudaranya juga tidak dibaca klien mana pun.** Keduanya lolos dari daftar utang
hanya karena namanya kebetulan muncul di sumber server untuk keperluan lain — dan
kelonggaran "pembaca sisi server" (dipasang di
[77](77-gerbang-oversell-dan-penjaga-field-yatim.md) bagian 2.2 untuk membuang 22 tuduhan
palsu) menelan keduanya.

Jadi kelonggaran yang memperbaiki positif palsu **membeli negatif palsu**. Itu pertukaran
yang memang dipilih sadar dan sudah ditulis di docstring alatnya — tetapi baru penelusuran
manual ini yang memperlihatkan bahwa ia benar-benar terjadi, bukan sekadar kemungkinan
teoretis.

Catatan itu kini tersimpan pada entri `totalTerjualBersih` di alatnya, di tempat orang
berikutnya pasti membacanya.

---

## 5. Ringkasan

| Kelompok | Jumlah | Perlu ditindaklanjuti? |
|---|---|---|
| Peringatan yang tidak sampai | 5 | **Ya** — 4 di antaranya menyangkut rekonsiliasi kas |
| Pendamping (saudaranya dibaca) | 5 | Tidak |
| Turunan/gema | 6 | Tidak |

Perkiraan awal saya — "nilainya kecil, isinya diagnostik" — benar untuk 11 dari 16, dan
**salah untuk 5**. Yang membuatnya salah bukan jumlahnya, melainkan satu kalimat komentar
yang menyatakan niat yang tidak tercapai.

---

## 6. Rekomendasi, dan kenapa belum saya kerjakan

Kelima field kelompok 1 sebaiknya **tidak** disambungkan satu per satu. Semuanya berbentuk
sama — "transaksi diterima, ada yang perlu direkonsiliasi" — dan `peringatanStok` yang
sudah tersambung di [76](76-peringatan-stok-terbaca-dan-nota-terparkir.md) adalah bentuk
yang keenam.

Bentuk yang benar adalah **satu saluran peringatan pasca-transaksi**: server mengumpulkan
semuanya ke satu daftar, klien membacanya di satu tempat. Dengan begitu peringatan ketujuh
tidak perlu menyentuh lima titik klien lagi — dan tidak akan terlupa seperti lima ini.

Belum saya kerjakan karena itu **mengubah kontrak `peringatanStok` yang baru dikirim dua
commit lalu**, menyentuh dua repositori, dan lebih tepat diputuskan sebagai satu perubahan
sadar daripada disisipkan sebagai lanjutan penelusuran. Yang diminta di sini adalah
menelusuri; hasilnya adalah rekomendasi ini.
