# 75 — Tiga celah sunyi di halaman Pesanan

Lanjutan langsung dari [73](73-stok-minus-tiga-nilai-dan-pemulihan-member.md). Setelah
"STOK MINUS" diperbaiki, penelusuran halaman **Monitor Pesanan Online (Draft)**
(`modul/kantin/pesanan/_draft_pesanan_anggota.jsp`) menemukan tiga celah lain yang
belum tersentuh.

Ketiganya berbentuk **sama persis**, dan bentuk itulah yang pantas diingat:

> Satu sisi menyiapkan informasi, sisi lain tidak memakainya, dan **tidak ada yang
> gagal dengan berisik.**

Tidak satu pun menghasilkan galat kompilasi, galat runtime, maupun uji merah. Itu
sebabnya ketiganya bertahan berminggu-minggu di produksi.

---

## 1. `id_member` diambil dari basis data, lalu dibuang

Keempat kueri daftar pesanan sudah lama mengambilnya secara eksplisit:

```sql
SELECT a.id, a.kode, a.toko as id_toko, a.anggota_koperasi, a.cara_pembayaran_koperasi ...
```

Nilainya sampai ke JavaScript. Lalu **tidak pernah dimasukkan ke payload `bayar`** —
pada kelima titik panggil sekaligus (2 Verifikasi Otomatis, 2 "Bayar Semua", 1 bayar
manual).

Kolom itu jelas di-`SELECT` dengan maksud dipakai; yang terjadi hanyalah perakitan
payloadnya lupa memasangnya. Dan karena `payload.optString(...)` di server memang
dirancang tidak mengeluh, tidak ada satu pun tanda bahwa sesuatu hilang di tengah jalan.

Akibatnya sudah tercatat di [73](73-stok-minus-tiga-nilai-dan-pemulihan-member.md)
bagian 2: sebelum r78633, member kosong dari payload **menimpa** nama pemesan pada
header draft dan pada transaksi finalnya.

### 1.1 Kenapa tetap diperbaiki walau server sudah menambal

r78633 membuat server mewarisi identitas pembeli dari header draft ketika payload tidak
membawanya. Perbaikan itu benar dan tetap dipertahankan — tetapi ia menjadikan dirinya
**satu-satunya penahan**. Seluruh halaman ini bergantung pada satu `if` di sisi server
yang menambal payload yang memang salah sejak dari klien.

Mengirim payload yang benar sejak dari sini membuat tambalan itu tidak lagi menanggung
beban sendirian: dua lapis yang saling tidak bergantung, bukan satu lapis yang kebetulan
masih berdiri.

| Titik panggil | Nilai yang dikirim |
|---|---|
| Verifikasi Otomatis (2 tempat) | `draft.anggota_koperasi \|\| null` |
| "Bayar Semua" (2 tempat) | `draft.anggota_koperasi \|\| null` |
| Bayar manual (modal) | `activeDraftIdMember` |

Modal bayar manual sebelumnya hanya menerima **nama** pemesan (untuk label), bukan
id-nya. Ia kini menerima `idMember` sebagai parameter, dan keempat tombol pemanggilnya
(Bayar, Ubah & Cetak, dan dua Monitor) meneruskan `row.id_member`.

---

## 2. "Bayar Semua" bilang berhasil padahal gagal

Kode lamanya, apa adanya:

```js
if (resBayar.status === '00' || resBayar.status === 'success') {
    successCount++;
} else {
    failCount++;          // alasannya dibuang -- tidak dicatat, tidak ditampilkan
}
```

lalu, tanpa syarat apa pun:

```js
statusText.innerHTML = '<span class="text-success fw-bold">'
    + '<i class="fas fa-check-circle"></i>Seluruh pesanan berhasil dilayani!</span>'
    + '<br><small class="text-muted">Berhasil: ' + successCount + ', Gagal: ' + failCount + '</small>';
```

Spanduk **hijau dengan ikon centang** berbunyi *"Seluruh pesanan berhasil dilayani!"* —
sementara `Gagal: 2` duduk di bawahnya sebagai teks abu kecil. Operator yang membacanya
sebagai "berhasil" tidak sedang lalai; layarnya memang mengatakan begitu.

**Inilah sebabnya dua pesanan 31-08 & 01-09 menggantung "Belum dibayar" tanpa seorang
pun menyadarinya.**

Yang menyakitkan: insiden identik sudah pernah terjadi pada **18-08-2026** dan sudah
diperbaiki — komentarnya masih terpasang di loop Verifikasi Otomatis:

> *"Kegagalan TIDAK boleh diam-diam: insiden 2026-08-18, seluruh verifikasi otomatis
> ditolak gerbang sesi kas (status 91) tetapi layar tetap menampilkan modal sukses —
> pesanan macet 'belum bayar' tanpa jejak."*

Perbaikannya hanya dipasang pada **satu** dari lima loop. Dua loop "Bayar Semua"
terlewat, dan mengulang kejadian yang sama dua minggu kemudian.

### 2.1 Sesudahnya

- Alasan penolakan server (`description`) disimpan per pesanan, bukan dibuang.
- Spanduk mengikuti **kenyataan**, bukan fakta bahwa loopnya selesai berjalan: merah +
  ikon peringatan bila `failCount > 0`, lengkap dengan daftar pesanan yang gagal beserta
  sebabnya.
- Pesanan tanpa rincian juga dihitung gagal dengan sebab tertulis, bukan dilewati diam.

---

## 3. `peringatanStok` dikirim server, tidak ada yang membacanya

Ini cacat yang **kami sendiri baru saja buat** — pada perbaikan
[73](73-stok-minus-tiga-nilai-dan-pemulihan-member.md) bagian 1.6, `peringatanStok`
ditambahkan ke respons sukses supaya kekurangan stok yang tidak memblokir tidak lagi
tenggelam di audit log.

Ternyata tidak ada satu pun klien yang membacanya. Persis pola yang sudah didokumentasikan
dua kali sebelumnya ([45](45-penyaring-dasbor-dan-layani-semua.md) dan
[46](46-angka-tanpa-rincian.md)) — dan nyaris menjadi kejadian ketiga oleh tangan sendiri.

Sekarang dibaca di kelima jalur:

| Jalur | Cara menampilkan |
|---|---|
| Bayar manual | `showToastUI(..., 'bg-warning text-dark')` |
| "Bayar Semua" (2) | ikut daftar pada spanduk hasil |
| Verifikasi Otomatis (2) | kotak `alert-warning` pada modal hasil |

Kalimatnya tetap datang dari server apa adanya — satu sumber, seperti
`alasanTanpaRincian` di [46](46-angka-tanpa-rincian.md).

---

## 4. Berkas yang berubah

| Berkas | Perubahan |
|---|---|
| `modul/kantin/pesanan/_draft_pesanan_anggota.jsp` | `id_member` pada 5 payload; `idMember` pada modal bayar + 4 pemanggilnya; alasan kegagalan disimpan & ditampilkan pada 2 loop massal; `peringatanStok` dibaca di 5 jalur |

---

## 5. Hasil uji

### 5.1 `TesKontrakPesananJsp` — 16 dari 16 lulus

Uji berbasis SUMBER (halaman ini JSP + JavaScript inline, tanpa harness uji), pola sama
dengan uji kontrak Flutter:

| Kelompok | Yang dikunci |
|---|---|
| 1 | `id_member` ada pada **setiap** payload bayar — dihitung, bukan diperiksa satu-satu: `id_member` = jumlah `action: "bayar"` (5/5) |
| 2 | `bukaModalBayar` menerima `idMember`, menyimpannya, dan keempat tombol meneruskannya |
| 3 | Kedua loop massal menyimpan alasan; spanduk berubah saat gagal; **bentuk spanduk hijau tanpa syarat sudah tidak ada lagi** |
| 4 | `peringatanStok` dibaca di 5 jalur, dan yang dikumpulkan memang dibaca lagi |

Dua periksaan terakhir kelompok 4 adalah penjaga anti-pola langsung: setiap daftar yang
di-`push` wajib punya pembaca (`join`/`map`). Mengumpulkan tanpa menampilkan persis
cacat nomor 3 di atas — uji ini menolak pengulangannya.

Periksaan **1** sengaja berbentuk perbandingan jumlah, bukan daftar titik panggil: kalau
besok ada titik panggil `bayar` keenam yang lupa membawa `id_member`, uji ini langsung
merah tanpa perlu diperbarui.

### 5.2 Sintaks JavaScript-nya diperiksa sungguhan

`cek_sintaks_jsp.py` mengekstrak blok `<script>`, mengganti `<%= … %>` dengan identifier,
lalu menjalankan `node --check`. Hasil: **SINTAKS BERSIH** (1937 baris).

Alat itu diuji dengan **kontrol negatif** sebelum dipercaya — satu kurung sengaja
dirusak (`const catatanRinci = [;`), dan pemeriksanya memang menolak:

```
SyntaxError: Unexpected token ';'
```

Menyunting JSP lewat skrip paling mungkin gagal pada satu kutip atau kurung yang tidak
seimbang, dan kesalahan seperti itu tidak ketahuan sampai halamannya dibuka pengguna.
Pemeriksa yang belum pernah dibuktikan bisa gagal tidak layak dipakai sebagai bukti.

### 5.3 Yang BELUM diuji

- **Halaman dibuka sungguhan di browser.** Yang diperiksa adalah bentuk sumber + sintaks
  JS-nya, bukan perilaku render. Untuk ketiga perubahan ini yang berubah adalah isi
  payload dan teks/warna spanduk — keduanya terbaca dari sumber — tetapi tata letak
  spanduk baru pada modal belum pernah dilihat mata.
- **Jalur `bayar` sampai ke basis data**, karena kredensial UAT masih ditolak (lihat
  [73](73-stok-minus-tiga-nilai-dan-pemulihan-member.md) bagian 4.2).

---

## 6. Yang perlu diperiksa lain kali

Tiga cacat pada satu berkas, ketiganya berbentuk sama. Pelajarannya bukan tentang JSP:

**Perbaikan yang dipasang pada satu dari beberapa jalur kembar adalah bom waktu.**
Insiden 18-08 diperbaiki di satu loop; empat loop lain dibiarkan, dan dua minggu
kemudian dua di antaranya mengulang kejadian yang sama. Ketika sebuah halaman punya
lima tempat yang memanggil aksi yang sama, memperbaiki satu berarti **berhutang empat**.

Dua kebiasaan yang mencegahnya, dan keduanya sudah dipakai di uji ini:

1. **Hitung, jangan daftar.** `jumlah id_member == jumlah action:"bayar"` menangkap
   titik panggil keenam yang belum ada hari ini. Daftar titik panggil yang ditulis
   tangan hanya menangkap yang sudah diketahui.
2. **Setiap yang dikumpulkan harus punya pembaca.** `push` tanpa `join`/`map` adalah
   informasi yang dibuang dengan cara yang terlihat seperti bekerja.
