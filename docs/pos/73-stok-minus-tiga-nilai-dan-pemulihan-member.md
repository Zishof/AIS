# 73 — "STOK MINUS" pada verifikasi pesanan, dan pemulihan nama pemesan yang hilang

Tiga laporan produksi e-Kantin (UKP Canteen Membership) dari satu pelapor:

| Tanggal | Laporan |
|---|---|
| 28-08-2026 | Nama pembeli hilang pada pesanan tanggal 26; pesanan awal Agustus hilang — dapatkah dikembalikan? |
| 01-09-2026 | Dua pesanan tidak bisa diverifikasi otomatis maupun dibayar manual: **"STOK MINUS"** |

Ketiganya berakar pada dua cacat berbeda. Yang kedua (01-09) adalah **regresi** dari
perbaikan yang sudah pernah diminta pelapor sebelumnya — karena itu kalimatnya:
*"Seingat saya kendala stock minus ini harusnya kan sudah diatasi ya waktu lalu."*
Ingatannya benar.

---

## 1. "STOK MINUS" — nilai `null` diperlakukan sebagai `FALSE`

Pesan yang muncul di layar:

```
Stok tidak mencukupi utk produk yang dikunci admin (tidak boleh dijual minus):
Nasi Koloke (sisa -2.0, diminta 1.0)
```

Pesan itu **menuduh pengaturan yang tidak pernah ada.** Admin tidak mengunci Nasi Koloke.

### 1.1 Kolomnya bernilai TIGA, bukan dua

`Produk.izinkanJualMinusStok` diisi dari combo **"Aturan Jual Saat Stok Kurang"**
(`ProdukAction`:620) yang menawarkan tiga pilihan:

| Nilai | Label di master Produk | Maksud |
|---|---|---|
| `null` | **Ikut Pengaturan Toko (default)** | tidak pernah disetel admin |
| `TRUE` | Selalu Boleh Dijual Walau Stok Minus | bebas |
| `FALSE` | **Wajib Diblokir Jika Stok Tidak Cukup** | dikunci admin |

Ketiganya dijanjikan berbeda oleh tiga sumber sekaligus:

- JavaDoc `Produk.getIzinkanJualMinusStok()`: `null` = *"ikut pengaturan toko (fail-open,
  boleh minus, sama seperti sebelum field ini ada)"*
- JavaDoc `validasiStokCukupDenganLock`: *"Produk TANPA override (default, `null`) tetap
  sepenuhnya fail-open seperti sebelum field ini ada"*
- Tooltip centang toko (`TokoAction`:459): OFF = *"ikuti izin stok minus pada
  masing-masing produk"*

### 1.2 Kodenya hanya mengenal dua

```java
if (stokBolehDijual < qtyDiminta && !Boolean.TRUE.equals(overridePerItem)) {
    kurang.add(deskripsi);
    wajibBlokir.add(deskripsi);   // <-- null DAN false, dua-duanya
}
```

`!Boolean.TRUE.equals(null)` bernilai `true`. Jadi **setiap produk yang belum pernah
disetel admin** ikut masuk `wajibBlokir` — diblokir keras, dengan pesan yang menyebutnya
"dikunci admin".

### 1.3 Kapan ini masuk

`svn blame` + bisect: **r77493, 16-08-2026**, *"POS: tambahkan kebijakan stok minus per
toko dan perbaiki validasi checkout"*. Sebelum itu (sampai r77492) bentuknya benar:

```java
if (stokLive < qtyDiminta) {
    kurang.add(deskripsi);
    if (Boolean.FALSE.equals(overridePerItem)) {   // HANYA yang eksplisit dikunci
        wajibBlokir.add(deskripsi);
    }
}
```

Commit itu menambahkan gerbang per-toko `bolehTransaksiStokHabis` (default OFF) dan
dalam prosesnya meruntuhkan tiga nilai menjadi dua. Karena gerbang toko default OFF,
seluruh instalasi yang tidak pernah menyalakannya mendadak mendapat blokir keras pada
SEMUA produk — persis kebijakan yang **sudah ditolak eksplisit oleh pengguna pada
20-07-2026** karena menolak transaksi pelanggan yang sah di toko yang baseline stok
historisnya belum bersih.

Regresinya tidak terdeteksi tiga minggu karena tidak ada yang gagal dengan berisik: pesan
penolakannya justru terdengar masuk akal ("dikunci admin"), sehingga operator mencari
sebabnya pada pengaturan produk yang memang tidak pernah diisi.

### 1.4 Kenapa blokirnya salah tempat, bukan sekadar salah nilai

Layar yang menolak adalah **Monitor Pesanan Online (Draft)** — pesanan yang **sudah
dipesan dan sudah diterima pembeli**. Stok Nasi Koloke sudah `-2` *sebelum* verifikasi
dijalankan; makanannya sudah keluar.

Menolak **mencatat** pembayarannya tidak menarik kembali stok yang sudah keluar. Yang
terjadi hanya:

- pesanan macet "Belum dibayar" selamanya,
- penjualannya hilang dari rekap transaksi harian,
- dan poin reward member MR tidak terhitung — padahal uangnya sudah masuk.

Saldo stok `-3` adalah **kebenarannya**. Menolak menuliskannya membuat pembukuan makin
jauh dari fisik, bukan makin dekat.

### 1.5 Perbaikannya

Aturannya diangkat menjadi method murni supaya dapat diuji tanpa basis data — yang
regresi kemarin bukan kuerinya, melainkan satu perbandingan boolean di tengah kueri itu:

```java
static boolean wajibDiblokirKarenaStok(Boolean izinkanJualMinusStok) {
    return Boolean.FALSE.equals(izinkanJualMinusStok);
}
```

Kekurangan stok **tetap** dicatat pada `semuaKurang` untuk audit — yang hilang hanya
blokirnya, bukan jejaknya.

### 1.6 Dua hal lain yang ikut dibenahi

**Pesan penolakan menyebut setelan yang benar-benar menyebabkannya**, berikut dua jalan
keluar yang dapat dikerjakan operator sendiri:

> Stok tidak mencukupi untuk produk yang disetel **"Wajib Diblokir Jika Stok Tidak Cukup"**
> pada master Produk: … . Perbaiki stoknya dahulu (Kulakan/Stok Opname), atau ubah
> **"Aturan Jual Saat Stok Kurang"** produk tsb ke **"Ikut Pengaturan Toko"** bila memang
> boleh dijual walau stok kurang.

**Kekurangan yang TIDAK memblokir tidak lagi diam.** Sebelumnya hanya masuk audit log
yang tidak pernah dibuka siapa pun. Kini dititipkan pada respons sebagai
`peringatanStok` — transaksi tetap sukses, kasir tetap jalan, dan satu-satunya tanda
bahwa saldo stok perlu diopname tidak lagi tenggelam.

---

## 2. Nama pemesan hilang — sudah diperbaiki, tetapi datanya perlu dipulihkan

### 2.1 Sebabnya

Halaman Pesanan mengirim payload `bayar` **tanpa** field member — baik lewat "Verifikasi
Otomatis" (`_draft_pesanan_anggota.jsp`:478) maupun tombol BAYAR manual (:1373).
Keduanya hanya mengirim `draftPembelianAnggotaKoperasi`.

Dahulu `null` dari payload itu **menimpa** anggota pada header draft DAN pada transaksi
final. Nama pemesan berubah jadi kosong, dan transaksinya lenyap dari laporan tenant.

### 2.2 Sudah diperbaiki di r78633 (31-08-2026)

Dua penjaga sekarang terpasang:

```java
// finalisasi draft MEWARISI identitas pembeli dari header draft
if (anggotaKoperasi == null && draftPembelianAnggotaKoperasi != null
        && draftPembelianAnggotaKoperasi.getAnggotaKoperasi() != null) {
    anggotaKoperasi = draftPembelianAnggotaKoperasi.getAnggotaKoperasi();
}
```

```java
// header draft tidak boleh ditimpa nilai kosong
if (anggota != null) {
    draft.setAnggotaKoperasi(anggota);
}
```

Ditambah penolakan eksplisit bila finalisasi otomatis tetap tidak menemukan member —
lebih baik berhenti berisik daripada mencatat transaksi tanpa pemilik.

Laporan pelapor tertanggal **28-08** — tiga hari SEBELUM perbaikan itu masuk. Jadi
pesanan yang namanya hilang adalah kerusakan data yang sudah terlanjur, bukan cacat yang
masih berjalan.

### 2.3 Datanya BISA dikembalikan

`DraftPembelianAnggotaKoperasi` dan `PembelianAnggotaKoperasi` keduanya `@Audited`
(Hibernate Envers, skema `new_audit`, akhiran `__audit`). Setiap revisi baris tersimpan —
**termasuk nilai `anggota_koperasi` sebelum ditimpa `null`.**

Skrip pemulihannya: [`74-sql-pemulihan-member-pesanan.sql`](74-sql-pemulihan-member-pesanan.sql).
Dijalankan bertahap: **hitung dulu, baru perbaiki** — dan hanya menyentuh baris yang
`anggota_koperasi`-nya sekarang `NULL` sedangkan audit menyimpan nilai yang tidak `NULL`.
Baris yang memang tidak pernah punya member (pembeli umum) tidak tersentuh.

### 2.4 "Pesanan awal Agustus yang menghilang"

Ini **perlu dipastikan dulu, jangan diasumsikan.** Dua kemungkinan berbeda penanganannya:

| Kemungkinan | Cara mengenali | Penanganan |
|---|---|---|
| Tidak benar-benar hilang, hanya tidak cocok penyaring | Layar Pesanan default menyaring rentang tanggal (screenshot: `20/08` – `28/08`). Pesanan awal Agustus di luar rentang itu. | Lebarkan tanggalnya |
| Namanya hilang sehingga tidak ketemu saat dicari per nama | Barisnya ada, kolom NAMA PEMESAN `-` | Bagian 2.3 |

Bagian 1 skrip pemulihan menghitung keduanya sekaligus.

---

## 3. Berkas yang berubah

| Berkas | Perubahan |
|---|---|
| `ais/action/servlet/api/KantinHelper.java` | `wajibDiblokirKarenaStok()` sebagai aturan tunggal; pesan penolakan menyebut setelan + jalan keluar; `peringatanStok` pada respons sukses |
| `docs/pos/74-sql-pemulihan-member-pesanan.sql` | **baru** — hitung & pulihkan member dari audit Envers |

---

## 4. Hasil uji

### 4.1 `StokMinusTigaNilaiUat` — 8 dari 8 lulus (tanpa basis data)

```
== Aturan Jual Saat Stok Kurang (master Produk) ==
  OK    null  "Ikut Pengaturan Toko (default)"       -> TIDAK memblokir
  OK    TRUE  "Selalu Boleh Dijual Walau Stok Minus" -> TIDAK memblokir
  OK    FALSE "Wajib Diblokir Jika Stok Tidak Cukup" -> MEMBLOKIR

== Rumus r77493 (yang salah) memang tertangkap uji ini ==
   null  -> lama=true  baru=false   <-- BEDA (perilaku yang diperbaiki)
   TRUE  -> lama=false baru=false
   FALSE -> lama=true  baru=true
```

Baris terakhir itu yang penting: uji ini membandingkan rumus lama dan rumus baru
berdampingan, dan membuktikan **tepat satu** nilai berubah perilakunya — `null`. Produk
yang memang dikunci admin tetap diblokir; produk "selalu boleh" tetap lolos. Perbaikan
yang mengubah lebih banyak daripada yang dimaksud akan langsung terlihat merah di sini.

### 4.2 Yang BELUM diuji

- **Jalur penuh `bayar` sampai ke basis data.** Kredensial UAT pada
  `.uat-tomcat-inventory/bin/setenv.bat` ditolak PostgreSQL (`password gagal
  diotentikasi untuk pengguna root`) — tampaknya sudah dirotasi. Harness berbasis DB
  (`TesStokMinusTigaNilai`, menanam 3 produk lalu menghapusnya lagi) sudah ditulis dan
  **terkompilasi**, tetapi belum dapat dijalankan. Begitu kredensial UAT diperbarui,
  harness itu tinggal dijalankan.
- **Skrip pemulihan member** belum dijalankan pada data mana pun — sengaja, karena
  targetnya basis data produksi. Bagian SELECT-nya aman dan wajib dijalankan lebih dulu.

---

## 5. Yang perlu diperiksa lain kali

Kolom bertipe `Boolean` yang bermakna **tiga** nilai adalah jebakan berulang: `null`
tampak "kosong" sehingga naluri pertama menulisnya sebagai `!Boolean.TRUE.equals(x)` —
dan itu diam-diam memindahkan seluruh default ke sisi yang salah. Tidak ada galat
kompilasi, tidak ada uji merah, dan pesan penolakannya justru terdengar masuk akal.

Aturannya: **kolom tri-state tidak boleh dibaca inline.** Beri ia method bernama, dengan
tabel tiga baris di JavaDoc-nya, dan uji ketiga nilainya. Aturan sesederhana satu
perbandingan boolean pantas berdiri sendiri justru karena ia terlalu kecil untuk
diperhatikan saat direfaktor.
