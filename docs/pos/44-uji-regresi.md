# 44 — Uji regresi: cara menjalankan dan jebakannya

Sembilan suite sisi server (JVM langsung, bukan JUnit) ditambah suite Flutter.
Penggeraknya `regresi_penuh2.sh`: menjalankan suite satu per satu, menyimpan log
utuh per suite, memindai penanda bahaya, dan membunuh JVM yatim di antara suite.

## Hasil terakhir — 22 Agustus 2026

| Suite | Lulus | Gagal |
|---|---:|---:|
| po_cepat | 45 | 0 |
| bast | 31 | 0 |
| tagihan | 21 | 0 |
| bayar | 32 | 0 |
| bulk | 12 | 0 |
| jbt | 13 | 0 |
| sinkron | 17 | 0 |
| pajak | 25 | 0 |
| lanjutan | 50 | 0 |
| **Server** | **246** | **0** |
| Flutter | 221 | 0 |

Nol penanda bahaya; setiap suite mencetak RINGKASAN-nya sendiri.

## Jebakan yang sudah terbukti memakan waktu

### Satu JVM uji pada satu waktu

PostgreSQL di mesin ini `max_connections=100`, dan kolam c3p0 satu JVM uji
menghabiskan sekitar 50. **Jangan pernah menjalankan dua suite bersamaan** — yang
kedua mati dengan "terlalu banyak klien", dan gejalanya menyesatkan: uji yang
gagal terlihat seperti cacat logika, padahal koneksinya yang habis.

### Basis data streaming tidak punya kredensial di mesin uji

`hibernate.streaming.cfg.xml` menyimpan kop surat dan lampiran. Kredensialnya
sengaja tidak ada di berkas itu; server produksi memasoknya lewat
`ais.db.override.file`, yang **tidak ada** di mesin uji. Tanpa batas akuisisi,
c3p0 mengulang 30 kali berkali-kali dan membanjiri PostgreSQL.

Karena itu **kesembilan** runner memuat:

```
set "C3P0=-Dc3p0.acquireRetryAttempts=2 -Dc3p0.acquireRetryDelay=200 -Dc3p0.breakAfterAcquireFailure=true"
```

Pernah hanya `jalankan_lanjutan.bat` yang punya batas ini — akibatnya `tagihan`
tumbang dengan 13 penanda bahaya dan `bayar` kehabisan waktu.

### `bersihkan()` tidak boleh melempar

`bersihkan()` dipanggil dari `finally`. Ketika ia sendiri melempar, dua hal
terjadi sekaligus: exception aslinya tertimpa, **dan** `main` mati sebelum
`System.exit(0)` — benang non-daemon lalu menahan JVM hidup sampai runner
membunuhnya pada batas waktu, sehingga RINGKASAN tidak pernah tercetak meski
seluruh ujinya lulus. Persis itu yang membuat `bayar` tampak "gagal" dengan 32
uji lulus.

Polanya kini: `bersihkan()` adalah pembungkus yang tidak pernah melempar, isinya
di `bersihkanInti()`.

### Cakupan pengosongan rujukan harus sepadan dengan cakupan penghapusan

Di `TesBayar`, pengosongan rujukan dibatasi satu pembayaran sementara
penghapusannya global (`kode LIKE 'BYR/%'`) — baris yang masih dirujuk pembayaran
lain melanggar kunci asing `fk23b45d0e2ebd8e34`. Keduanya kini memakai cakupan
yang sama lewat subkueri.

### Sisa data dari jalannya yang mati mencemari jalannya berikutnya

`bersihkan()` hanya menghapus yang dibuat jalannya **sendiri** (daftar id di
memori). Satu jalannya yang terbunuh meninggalkan baris yang bertahan selamanya.
Dua kegagalan terakhir `tagihan` murni akibat ini: ujinya menghitung 2, basis
data menyimpan 8.

`sapuSisaUat()` menyapu sisa itu di awal `siapkan()`, mengikuti arah kunci asing
— detail sebelum induk, BAST sebelum PO, dan **`po_induk` dikosongkan lebih
dulu** karena Back Order membuat PO menunjuk PO lain.

Suite ini juga memakai nomor tagihan tetap (`INV-UAT-...`). Penjaga tagihan ganda
di server menolak nomor yang sudah dipakai — perilaku yang memang benar — jadi
`bebaskanNomorTagihanUat()` mengosongkan nomornya (bukan menghapus barisnya).

### Nama kolom mengikuti penamaan bawaan Hibernate

Tanpa `@Column`, Hibernate memakai nama properti yang dihuruf-kecilkan:
`kodeTagihan` menjadi **`kodetagihan`**, bukan `kode_tagihan`. Menebak yang
terakhir membuat `tagihan` mati saat penyiapan dengan nol uji berjalan.

### Akhir baris berbeda PER BERKAS, bukan per repositori

Menormalkan akhir baris membuat `svn diff` menampilkan **seluruh** berkas
sebagai terhapus-lalu-ditambah. Perubahan tujuh baris menjadi mustahil
ditinjau, dan setiap sesi lain yang menyentuh berkas itu kena konflik palsu di
semua baris.

Yang membuatnya mudah salah: konvensinya ditentukan per berkas. Diukur
terhadap HEAD pada 22-08-2026:

| Berkas | Akhir baris |
|---|---|
| `servlet/PosApi.java` | LF (6135) |
| `servlet/api/SopService.java` | CRLF (3232) |
| `servlet/api/PengadaanPosApiHelper.java` | LF |
| `servlet/api/KantinHelper.java` | LF |
| `servlet/api/DraftJurnalApiHelper.java` | CRLF (330) |
| `master/akunting/PostingKasKecilAction.java` | CRLF (1486) |
| `master/akunting/PostingProsesTransferAction.java` | LF (1521) |

`PosApi.java` dan `SopService.java` bertetangga langsung namun berbeda, jadi
menebak dari nama direktori pun tidak aman.

**Patokannya: samakan dengan `svn cat -r HEAD` berkas itu sendiri** -- bukan
dengan konvensi repositori, dan bukan dengan berkas tetangganya.

Ada satu kasus kedua yang tidak tercakup patokan itu. Ketika Anda **menambah**
baris, berkas di HEAD sudah seragam tetapi baris baru dari skrip datang
berakhiran `\n`. Hasilnya berkas campuran. `DasboardSop.java` pernah menjadi
4319 CRLF + 45 LF karena hal ini -- 45 baris yang ditambahkan, bukan berkasnya
yang memang campur.

Yang berbahaya: `svn diff` tetap terlihat kecil dan wajar, sehingga tidak
ketahuan sama sekali dari peninjauan biasa. Hanya ketahuan bila bitanya
dihitung.

Prosedur yang dipakai sekarang, dua lapis:

1. Baca sebagai bita, kerjakan penyuntingan pada teks yang sudah dinormalkan
   ke `\n`, lalu tulis balik memakai akhir baris **berkas itu sendiri**.
2. Setelah menambal, hitung ulang dan pastikan **salah satu di antara CRLF dan
   LF-murni bernilai nol**:

```python
d = open(p, "rb").read()
crlf = d.count(b"\r\n")
lf = d.count(b"\n") - crlf
assert crlf == 0 or lf == 0, "berkas menjadi campuran"
```

Jangan memakai `grep -c` untuk menghitung ini -- hasilnya palsu. Hitungan bita
yang berwenang.

### Skrip penambal ditulis sebagai berkas, bukan heredoc

Terkait langsung dengan yang di atas, dan sudah dua kali menggigit.

Heredoc `<<'PY'` meruntuhkan backslash walau pembatasnya dikutip. Sebuah skrip
yang memuat `\r\n` sampai ke Python sebagai karakter kendali sungguhan, bukan
sebagai dua karakter. Dokumen ini sendiri sempat rusak di empat tempat karena
itu -- ketika bagian di atas disisipkan lewat heredoc.

Dua penangkalnya:

- Tulis skrip penambal sebagai **berkas**, lalu jalankan berkasnya.
- Bila sebuah backslash memang harus ada di keluaran, bangun dari `chr(92)`
  alih-alih mengetiknya.

Berlaku juga saat menambal berkas `.bat`, yang jalurnya penuh backslash.
### Urutan classpath

`build/uat-77608` mendahului `build/classes`. Kelas yang diubah harus
dikompilasi ke **keduanya**, kalau tidak yang berjalan adalah versi lamanya.

### Kredensial

Kredensial harness datang dari `CATALINA_OPTS` di `setenv.bat` dan tidak pernah
dicetak ke log maupun ditulis ke berkas.
