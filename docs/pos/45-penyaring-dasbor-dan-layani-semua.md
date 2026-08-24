# 45 — Penyaring Dasbor "Data Pembelian" dan cakupan "Layani Semua"

Laporan pengguna: pada **Dasbor → Ringkasan Umum**, chip *"Jenis pembayaran: QRIS BMT"*
menyala, tetapi kolom METODE pada tabel **Data Pembelian** tetap memperlihatkan baris
*Tunai*. Kalimatnya: *"filter nya belum bekerja dengan baik"*.

Penelusurannya menemukan **tiga** cacat. Yang dilaporkan pengguna terlihat di layar; yang
kedua tidak terlihat sama sekali dan justru lebih merusak; yang ketiga baru muncul ketika
UAT membandingkan label kartu dengan label baris.

---

## 1. Cacat 1 — layar berkata menyaring, servernya tidak

Klien **sudah** mengirim penyaringnya. `_payloadDashboardUmum()` di
`lib/screens/ringkasan/tab_umum.dart` menyertakan `metodeBayar` sejak awal, dan chip di
atas tabel dirakit dari nilai yang sama.

Yang tidak ada adalah pembacanya. `PosApi.prosesDashboardUmum` hanya membaca
`tglMulai`, `tglSampai`, `kodeTransaksi` (ke `WHERE`) dan `cariPembeli` (ke `HAVING`);
`metodeBayar` tidak pernah disentuh, jadi tabelnya tetap memuat seluruh metode
sementara chip-nya menyala. Layar yang berbohong tentang apa yang sedang ditampilkan
lebih buruk daripada layar tanpa penyaring: angka yang dibaca pengguna bukan angka yang
ia minta, dan tidak ada tanda apa pun bahwa itu terjadi.

### 1.1 Kenapa `HAVING`, bukan `WHERE`

Satu transaksi adalah **grup** baris `koperasi.pembelian` (satu nota, banyak item),
dikelompokkan `COALESCE(a.pembelian_anggota_koperasi, a.id)`. Cara bayar yang
ditampilkan tabel adalah `MAX(a.carabayar)` atas grup itu.

Menyaring di `WHERE` akan memotong **sebagian baris** dari grup lalu tetap memunculkan
transaksinya — dengan total yang sudah tidak utuh. Kesalahan seperti itu tidak
menampilkan pesan apa pun; ia hanya membuat angka rupiahnya salah. Karena itu
penyaringnya diletakkan pada `HAVING`, sejajar dengan `cariPembeli` yang sudah di sana.

Keduanya kini dirakit sebagai daftar, bukan string tempel-menempel:

```java
java.util.List<String> having = new java.util.ArrayList<String>();
if (cariPembeli.length() > 0) having.add("MAX(a.member) ILIKE ?");
if (metodeBayarFilter.length() > 0) having.add(kondisiMetodeBayar() + " = ?");
```

### 1.2 Satu ekspresi label, dipakai dua tempat

Daftar pilihan pada combo penyaring **berasal dari** kartu "Komposisi Pembayaran", dan
kartu itu memberi label khusus `'Lainnya'` untuk transaksi yang `carabayar`-nya kosong:

```sql
COALESCE(NULLIF(TRIM(CAST(p.carabayar AS varchar)),''),'Lainnya')
```

Bila penyaringnya membandingkan kolom mentah, memilih **"Lainnya"** tidak akan pernah
cocok dengan apa pun — pengguna melihat tabel kosong tanpa sebab yang bisa ditebak.
Karena itu ekspresinya dijadikan **satu sumber kebenaran**, `kondisiMetodeBayar(kolom)`,
dan kartu Komposisi diubah memakainya juga. Dua tempat yang wajib sama kini secara
harfiah adalah string yang sama; tidak mungkin lagi menyimpang diam-diam.

| Pemakai | Ekspresi |
|---|---|
| Kartu Komposisi Pembayaran | `kondisiMetodeBayar("p.carabayar")` |
| Penyaring tabel (dikelompokkan) | `kondisiMetodeBayar()` → `MAX(a.carabayar)` |
| Kolom METODE pada tabel | `kondisiMetodeBayar()` (lihat 1.3) |

### 1.3 Cacat ketiga, ditemukan oleh UAT

Perbaikan 1.2 semula hanya menyentuh **penyaring**-nya. Kolom METODE yang ditampilkan
tabel masih membaca kolom mentah `MAX(a.carabayar)`. UAT memperlihatkan akibatnya:

```
metode pd tabel                  : [QRIS BMT, Tunai, ]      <- sel KOSONG
label kartu Komposisi Pembayaran : [QRIS BMT, Tunai, Lainnya]
```

Jadi memilih **"Lainnya"** memang menyaring dengan benar — barisnya ketemu — tetapi
baris yang muncul memperlihatkan sel METODE **kosong**. Pengguna melihat layar yang
tampak salah justru sesudah penyaringnya bekerja.

Kolom itu kini memakai ekspresi yang sama. Ketiga tempat — kartu, penyaring, dan sel
yang dibaca mata pengguna — sekarang satu ekspresi.

Ini pelajaran tersendiri: "satu sumber kebenaran" belum selesai selama masih ada
pemakai keempat yang membaca kolom mentah. Yang menemukannya bukan pembacaan ulang
kode, melainkan uji yang membandingkan label kartu dengan label baris.

---

## 2. Cacat 2 — "Layani Semua" menyapu lebih luas daripada yang dilihat

Ini tidak dilaporkan pengguna karena tidak terlihat.

`prosesLayaniSemuaTransaksi` hanya menghormati rentang tanggal:

```java
StringBuilder sql = new StringBuilder("UPDATE koperasi.pembelian SET terlayani = true"
        + " WHERE toko = ? AND (terlayani IS NULL OR terlayani = false)");
if (tglMulai.length() > 0) { sql.append(" AND DATE(waktu) >= ?"); … }
if (tglSampai.length() > 0) { sql.append(" AND DATE(waktu) <= ?"); … }
```

dan kliennya pun hanya mengirim `..._payloadRentangTanggal()`.

Akibatnya: pengguna yang menyaring **"QRIS BMT"**, melihat 12 baris, lalu menekan
**Layani Semua**, menandai TERLAYANI **seluruh** transaksi belum terlayani pada rentang
itu — termasuk transaksi Tunai yang tidak pernah muncul di layarnya. Yang disetujui
bukan yang dikerjakan, arahnya merusak, dan tidak ada tombol batal sesudahnya.

Perlu dicatat bahwa cacat 2 sudah ada **sebelum** cacat 1 diperbaiki — hanya saja selama
tabelnya belum tersaring, kedua himpunan kebetulan sama besar sehingga tidak ada bedanya.
Memperbaiki cacat 1 saja justru **mengaktifkan** cacat 2. Keduanya harus berangkat
bersama.

### 2.1 Penyaring per-transaksi pada UPDATE per-baris

`cariPembeli` dan `metodeBayar` bersifat **per transaksi**, sedangkan yang di-UPDATE
adalah **baris**. Keduanya tidak bisa ditempel begitu saja pada `WHERE` per-baris —
alasannya sama dengan bagian 1.1. Jadi id transaksinya dipilih dulu lewat subquery yang
dikelompokkan, dengan susunan yang **sama persis** dengan tabel Data Pembelian:

```sql
AND COALESCE(pembelian_anggota_koperasi, id) IN (
  SELECT COALESCE(a.pembelian_anggota_koperasi, a.id)
  FROM koperasi.pembelian a
  WHERE a.toko = ? AND DATE(a.waktu) >= ? AND DATE(a.waktu) <= ?
  GROUP BY 1
  HAVING <MAX(a.member) ILIKE ?> AND <kondisiMetodeBayar() = ?>)
```

Kunci payload-nya pun disamakan huruf demi huruf dengan payload tabel
(`cariPembeli`, `kodeTransaksi` + `kode`, `metodeBayar`) — kalau kuncinya berbeda,
penyaringnya diam-diam terabaikan lagi dan kita kembali ke titik awal.

### 2.2 Dialog konfirmasi mengeja cakupannya

Dialognya dulu berbunyi *"Semua transaksi pada rentang filter ini akan ditandai
terlayani."* — kalimat yang tidak dapat diperiksa pengguna. Sekarang penyaring yang
sedang aktif dieja satu per satu:

```
Transaksi belum terlayani yang cocok dengan penyaring berikut akan ditandai terlayani:
  • Tanggal: 2026-08-01 s/d 2026-08-24
  • Jenis pembayaran: QRIS BMT
```

dan bila tidak ada penyaring sama sekali, kalimatnya menyebut itu apa adanya
(*"SELURUH transaksi yang belum terlayani…"*), bukan menyamarkannya sebagai "rentang
filter ini". Aksi massal tanpa pembatalan harus dapat dibaca cakupannya sebelum
disetujui.

---

## 3. Berkas yang berubah

| Berkas | Perubahan |
|---|---|
| `ais/action/servlet/PosApi.java` | `kondisiMetodeBayar()`; `metodeBayarFilter` pada `prosesDashboardUmum` (HAVING + ikat param di `psCount` & `psData`); kartu Komposisi DAN kolom METODE tabel memakai ekspresi bersama; subquery penyaring pada `prosesLayaniSemuaTransaksi` |
| `apps/ebisnis/lib/screens/ringkasan/tab_umum.dart` | `layani_semua_transaksi` mengirim penyaring aktif; `_ringkasanPenyaringAktif()` + dialog konfirmasi yang mengeja cakupan |
| `apps/ebisnis/test/layani_semua_penyaring_kontrak_test.dart` | **baru** — mengunci "payload aksi massal = payload tabel" |

---

## 4. Hasil uji

### 4.1 `TesFilterDasborUmum` — 16 dari 16 periksaan lulus

Basis UAT **tidak punya satu pun baris `koperasi.pembelian`** (terverifikasi: 0 baris),
jadi harness ini menanam datanya sendiri lalu menghapusnya lagi. Rinciannya di 4.2.

Data uji: 5 transaksi pada toko UAT, tanggal `1999-03-03`. Salah satunya (**T1**) satu
nota berisi **dua** baris detail; **T5** sengaja sudah `terlayani`.

| # | Yang diperiksa | Hasil |
|---|---|---|
| 1 | Nota 2 baris dihitung SATU transaksi | 5 transaksi |
| 2 | Tanpa penyaring memang bercampur | `[QRIS BMT, Tunai, Lainnya]` |
| 3 | `metodeBayar=QRIS BMT` → hanya QRIS BMT | 2 transaksi, 1 metode |
| 4 | Penghitung halaman ikut tersaring | `total=2` |
| 5 | **Nilai nota 2 baris tetap utuh** | 18.000 (10rb+5rb+3rb) |
| 6 | `metodeBayar=Lainnya` cocok, bukan tabel kosong | 1 transaksi |
| 7 | Dua penyaring bekerja bersama (`Tunai` + `Budi`) | 1 transaksi |
| 8 | Label kartu = label baris tabel | keduanya `[QRIS BMT, Tunai, Lainnya]` |

Periksaan **5** adalah yang membuktikan pilihan `HAVING`: seandainya penyaringnya
dipasang di `WHERE`, nota T1 tetap muncul tetapi nilainya terpotong.

### 4.2 Cakupan "Layani Semua" — inti perbaikannya

| # | Yang diperiksa | Hasil |
|---|---|---|
| 9 | Layani Semua + `QRIS BMT` | **tepat 2** baris diperbarui (dua baris T1; T5 sudah terlayani) |
| 10 | Kedua baris nota QRIS BMT ditandai | `terlayani=true` |
| 11 | **Baris Tunai TIDAK ikut tertandai** | `terlayani=false` |
| 12 | Baris `Lainnya` TIDAK ikut tertandai | `terlayani=false` |
| 13 | Penyaring dilepas → sisanya baru tersapu | 3 baris |

Periksaan **11–12** adalah cacat 2 yang diperbaiki, diperlihatkan pada data sungguhan:
sebelum perbaikan, langkah 9 akan memperbarui **5** baris, bukan 2.

Periksaan **14–16** menjalankan kombinasi `cariPembeli` / `kodeTransaksi` /
ketiganya sekaligus pada rentang tahun 1900 (nol baris cocok). Yang diuji di situ
adalah **urutan pengikatan parameter** antara `WHERE` induk dan subquery-nya — salah
urut ditolak PostgreSQL (galat tipe/kolom), bukan diam-diam menyapu baris yang keliru.

### 4.3 Data uji ditanam, lalu dihapus

Penghapusannya dibatasi pada **id yang benar-benar ditanam harness ini** — bukan sapuan
berdasarkan kondisi (`WHERE waktu = '1999-03-03'`), yang bisa ikut membawa baris sesi
lain bila kebetulan ada. Blok `finally` melaporkan jumlahnya, dan itu ikut dihitung
sebagai periksaan:

```
pembersihan: 6 baris + 1 nota dihapus
  OK    seluruh data uji terhapus kembali
```

Diverifikasi terpisah sesudahnya: `koperasi.pembelian` kembali **0 baris**.

### 4.4 `layani_semua_penyaring_kontrak_test.dart` — 3 dari 3 lulus

Uji kontrak sumber (pola sama dengan `draft_jurnal_kontrak_test.dart`) yang mengunci:
payload `layani_semua_transaksi` memuat penyaring yang sama dengan payload tabel, kedua
ejaan kode transaksi dikirim, dan dialog konfirmasinya mengeja penyaring aktif alih-alih
kalimat lama yang tidak dapat diperiksa.

### 4.5 Yang BELUM diuji

- **Lewat HTTP sungguhan.** Harness memanggil `prosesDashboardUmum` /
  `prosesLayaniSemuaTransaksi` langsung lewat refleksi. Lapisan autentikasi dan
  `doPost` tidak dilewati.
- **Pengguna dengan `bolehLihatSemuaToko`.** Harness memakai `Tbmuser` transien tanpa
  pedagang, jadi toko diambil dari payload. Jalur agregat lintas toko (`semuaToko`)
  tidak disentuh perubahan ini, tetapi juga tidak diuji ulang.

---

## 5. Yang perlu diperiksa lain kali

Kedua cacat ini berbentuk sama: **klien mengirim sesuatu, server tidak membacanya, dan
tidak ada yang mengeluh.** `payload.optString(...)` memang dirancang tidak mengeluh —
itulah yang membuat kelas kesalahan ini sunyi.

Layar lain yang punya penyaring + aksi massal patut diperiksa dengan pertanyaan yang
sama: *apakah aksi massalnya menerima penyaring yang sama dengan tabelnya?* Bila
tidak, cacat 2 ada di sana juga, dan sama tidak terlihatnya.
