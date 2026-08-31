# Tabrakan `kodeUnik` Antar-Kaki Jurnal: Mekanisme Terbukti, Kaki Siswa Diperbaiki

Tanggal: 31 Agustus 2026. Temuan dilaporkan pada HEAD r78682; **perbaikannya masuk
SVN r78693** (`PostingJurnalHelper` + tiga layar kaki siswa), teruji `TesRefKakiSiswa`
**LULUS 12, GAGAL 0** (§6). Mirror `java/` selaras. Lanjutan sapuan
[70-audit-dokumen-berkaki-ganda.md](70-audit-dokumen-berkaki-ganda.md).

## 1. Mekanismenya

`CommonAkunting.saveTransaksi` memiliki penjaga duplikat: sebelum menulis, ia mencari
`GrupTransaksi` yang `kodeUnik`-nya sama. **Bila ketemu, jurnal baru TIDAK ditulis** — grup
lama hanya dicap ulang dengan posting history yang baru:

```java
GrupTransaksi apakahSudahada = ... .add(Restrictions.eq("kodeUnik", grupTransaksi.getKodeUnik())) ...
if (apakahSudahada != null) {
    apakahSudahada.setPostingHistory(postingHistory);   // <- hanya dicap ulang
    Common.refreshUpdate(session, apakahSudahada);
} else { ... tulis jurnal baru ... }
```

Kuncinya disusun `GrupTransaksi.ambilUnik()` dari **kelas + id dokumen + `ref`** saja.
Kolom `jenis` TIDAK ikut — dan memang tidak bisa ikut, sebab `getJenis()` adalah getter
turunan yang membaca `postingHistory.getJenis()`.

Konsekuensinya: **dua kaki jurnal pada dokumen yang sama, yang sama-sama menulis dengan
`ref` null, berbagi satu kunci.** Kaki kedua tidak pernah masuk buku besar.

## 2. Bukti empiris (harness `TesTabrakanKodeUnik`)

Diukur langsung terhadap `saveTransaksi` di DB UAT — bukan disimpulkan dari pembacaan kode.
Fixture murah memakai `JenisKasKecil` (salah satu entitas dalam `ambilUnik`), jendela
1–10 Juni 2092, prefiks `UATKU-`:

| Skenario | Hasil terukur |
|---|---|
| Panggilan 1 (kaki A, 100.000, ref null) | 1 grup jurnal terbentuk |
| Panggilan 2 (kaki B, 25.000, ref null, dokumen SAMA) | **tetap 1 grup** — tidak ada jurnal baru |
| Nilai di buku besar sesudah dua panggilan | **tetap 100.000** — nilai kaki B tidak pernah tercatat |
| Cap grup lama | **berpindah** ke posting history kaki B |
| Kolom `jenis` grup | ikut berpindah ke jenis riwayat kaki B |
| **KONTROL**: dua panggilan dokumen sama dengan `ref` BERBEDA | 2 grup terpisah terbentuk — `ref` memang pembedanya |

**LULUS 7, GAGAL 0.**

## 3. Siapa yang terpapar, siapa yang aman

| Dokumen ber-kaki ganda | Ada di `ambilUnik`? | `ref` per kaki | Status |
|---|---|---|---|
| **`Tagihan` (siswa)** — piutang, denda, diskon, dibayar dimuka | **ya** | **keempatnya null** (semua memakai overload `saveTransaksi` tanpa ref; keluarga siswa tidak memakai satu pun konstanta `REF_`) | **TERPAPAR** |
| `Pertangungjawaban` — utama, pajak, pengembalian | ya | null / `'pajak'` / `'pengembalian'` | aman |
| `PertangungjawabanKasBesar` | ya | idem (kaki pajak & pengembalian belum dipakai) | aman |
| `CicilanPembayaran` — utama, dibayar dimuka | ya | null / `'dimuka'` | aman |
| `LogPembayaran` — administrasi, payment gateway | **tidak** | keduanya null | aman **secara kebetulan** — lihat §4 |

Pola pengaman yang sudah dipakai rumah ini justru ada di LPJ: kaki utama ber-ref null,
kaki lain diberi ref sendiri. Keluarga siswa tidak pernah melakukannya.

## 4. Peringatan ke depan: `LogPembayaran` aman hanya karena absen dari kunci

Karena `logPembayaran` tidak ada di `ambilUnik`, `kodeUnik`-nya null sehingga pencarian
duplikat tidak pernah menemukan apa pun dan setiap kaki menulis jurnalnya sendiri. Itu
sebabnya cacat pembatalan silang di dok 70 §2.1 bisa terjadi: kedua kaki memang benar-benar
punya jurnal. **Menambahkan `logPembayaran` ke `ambilUnik` kelak — yang tampak seperti
kerapian — akan langsung menciptakan tabrakan pada dua kaki itu.** Bila suatu saat
ditambahkan, kedua kaki wajib diberi `ref` berbeda pada saat yang sama.

## 5. Kenapa tidak diperbaiki sepihak di sini

Dua jalan perbaikan, keduanya menyentuh semantik data akuntansi:

1. **Beri `ref` khas pada tiga kaki siswa** (mengikuti pola LPJ). Terverifikasi tidak
   merusak apa pun yang sekarang ada: SQL pembatalan keempat kaki menyaring `jenis`, bukan
   `ref`; penghitung closing baris-baris itu memanggil `hitungClosing(..., ref = null, ...)`
   dan `restriksiRefClosing(null)` mengembalikan `1=1` alias tanpa filter; tidak ada satu
   pun kueri lain yang menyaring jurnal ber-`tagihan` berdasarkan `ref`.
2. **Masukkan pembeda kaki ke dalam kunci** — perubahan mekanisme sentral yang menyentuh
   seluruh 47 layar dan setiap mesin posting.

Yang menahan saya bukan pilihan desainnya, melainkan **verifikasi**: modul sekolah nyaris
tidak berdata di UAT — `akunting.grup_transaksi` tidak punya satu pun baris ber-`tagihan`,
dan tidak ada satu tagihan pun yang memenuhi kriteria mesin piutang (rantai
`item_biaya_sekolah.akun_piutang_id` + `pengaturan_biaya` + `jenis_biaya_sekolah` tidak
lengkap). Uji ujung-ke-ujung "posting piutang lalu denda pada tagihan yang sama" karena itu
tidak dapat dijalankan tanpa mengarang rantai master baru di basis data uji. Mengubah cara
penulisan jurnal sebuah modul yang tidak bisa saya jalankan sampai tuntas bukan langkah yang
pantas diambil sepihak.

**Rekomendasi saat itu:** ambil jalan (1) — beri `ref` khas pada kaki denda, diskon, dan
dibayar dimuka (kaki piutang tetap null sebagai kaki utama, persis pola LPJ).

## 6. Perbaikan yang dikerjakan (r78693) dan pengujiannya

Jalan (1) dijalankan atas persetujuan pemilik pekerjaan. Tiga konstanta baru pada
`PostingJurnalHelper` — `REF_DENDA_SISWA` (`denda_siswa`), `REF_DISKON_SISWA`
(`diskon_siswa`), `REF_DIMUKA_SISWA` (`dimuka_siswa`) — dipasang pada **keenam** panggilan
`saveTransaksi` di masing-masing layar kaki (jalur massal ZK, jalur per baris, dan mesin
API), total 18 panggilan. Kaki piutang sengaja dibiarkan ber-ref null sebagai kaki utama.

Nilai ref sengaja BARU (bukan memakai ulang `REF_DIMUKA` milik cicilan mahasiswa) supaya
jatuh ke cabang default `restriksiRefClosing` yang mengembalikan `1=1` — dengan begitu
hitungan closing keempat baris siswa tidak berubah sama sekali.

Harness `TesRefKakiSiswa` (fixture `UATRS-`, jendela 1–10 Juli 2092) memakai entitas
`Tagihan` SUNGGUHAN dan konstanta yang persis dipakai ketiga layar:

| Skenario | Hasil |
|---|---|
| **KONTROL** bentuk lama: dua kaki ber-ref null pada satu tagihan | tetap BERTABRAKAN (1 grup), nilai denda tidak masuk buku besar — harness terbukti sahih |
| Kaki piutang (ref null) | membentuk grup pertama |
| Kaki denda / diskon / dibayar dimuka (ref khas) | masing-masing membentuk grup SENDIRI → 4 grup |
| Total nilai keempat kaki | 580.000 seluruhnya masuk buku besar |
| Ref per grup | tepat satu ber-ref null, satu `denda_siswa`, satu `diskon_siswa`, satu `dimuka_siswa` |
| Cap tiap grup | tetap menunjuk riwayat kakinya sendiri, tidak saling menimpa |
| Label `jenis` kaki piutang | tetap "Piutang Siswa" — tidak lagi berpindah ke jenis denda |
| Ulangi kaki denda | tetap 4 grup — penjaga duplikat tetap bekerja untuk kaki yang SAMA |

**LULUS 12, GAGAL 0.**

Jebakan fixture baru yang ditemukan harness ini (untuk penulis harness berikutnya):
`saveTransaksi` memasang dokumen sebagai reference dengan cascade MERGE sehingga Hibernate
ikut meng-UPDATE dokumennya — fixture `Tagihan` berkolom sebagian langsung melanggar
constraint, jadi barisnya harus **diklon utuh** dari baris nyata (lewat temp table) dengan
kolom unik dan cap posting dikosongkan. Dan `Tagihan.getKodeUnik()` adalah getter turunan
di atas kolom yang UNIK di basis data: mengosongkannya justru membuat nilainya dihitung
ulang dan bertabrakan dengan baris sumber — isi dengan nilai fixture yang khas, jangan
di-null-kan. Keluarga jebakan yang sama dengan `getNama()`/`getTanggalTransaksi()` pada
dok 68.

Gejala lapangan yang harus dicurigai pada data LAMA (jurnal yang terlanjur ditulis sebelum
r78693): **satu tagihan yang denda atau diskonnya "sudah diposting" tetapi jurnalnya tidak
ada di buku besar, dan jurnal piutangnya berlabel jenis denda/diskon.** Perbaikan ini
mencegah kejadian baru; data lama yang sudah telanjur bertabrakan perlu ditinjau terpisah.

## 6a. Dampak data lama: skrip diagnosa produksi (r78713)

Perbaikan r78693 mencegah kejadian BARU; jurnal yang terlanjur bertabrakan sebelum itu
tetap perlu ditinjau. Angkanya **belum dapat diambil dari mesin kerja**: satu-satunya
basis data yang tersambung di sini adalah UAT lokal (`127.0.0.1:5432/ais`) — berkas
override kredensial produksi tidak ada dan `context.xml` Tomcat tidak memuat JDBC apa pun.
Di UAT lokal seluruh angkanya nol, tetapi itu tidak berarti apa-apa: modul siswa memang
belum pernah dipakai di sana (nol grup jurnal ber-`tagihan`).

Karena itu disiapkan skrip **hanya-baca**
[`docs/sql/2026-08-31-diagnosa-tabrakan-kodeunik-tagihan.sql`](../sql/2026-08-31-diagnosa-tabrakan-kodeunik-tagihan.sql)
untuk dijalankan DBA pada produksi. Tujuh kueri, seluruhnya `SELECT`, sekali jalan menilai
EMPAT jejak kerusakan berbeda dari kampanye perbaikan ini:

| Kueri | Menilai | Jejak yang dicari |
|---|---|---|
| Q0–Q3 | tabrakan kodeUnik kaki tagihan siswa (r78693) | kaki bercap "sudah diposting" tanpa jurnal; grup piutang yang labelnya berpindah |
| Q4 | kaki `LogPembayaran` saling menghapus (r78672, dok 70 §2.1) | cap terisi tanpa jurnal pada kaki administrasi / payment gateway |
| Q5 | jurnal cicilan yatim (r78673, dok 70 §2.2) | jurnal masih ada padahal capnya sudah dilepas — nilainya MASIH terhitung di buku besar |
| Q6 | jurnal pengembalian Dr X / Cr X (r78539, dok 54 §2a) | grup `ref='pengembalian'` yang seluruh barisnya memakai satu akun |

Skrip sudah divalidasi terhadap skema nyata lewat pelari yang menolak pernyataan selain
`SELECT`/`WITH` — ketujuh kueri jalan bersih. Cara membaca hasil dan langkah pemulihan tiap
temuan ditulis di kaki skripnya; pemulihan apa pun menunggu persetujuan bagian keuangan dan
tidak boleh menyentuh periode yang sudah ditutup buku tanpa membukanya lebih dulu.

## 7. Sapuan penutup: seluruh kolom referensi ber-pemilik ganda

§3 memeriksa dokumen ber-CAP ganda. Bentuk lain dari risiko yang sama adalah satu **kolom
referensi jurnal yang dimiliki lebih dari satu layar posting** — dua modul berbeda menulis
jurnal pada dokumen yang sama. Disapu untuk seluruh entitas yang ikut rumus `ambilUnik`
(hanya entitas itu yang bisa bertabrakan):

| Kolom | Pemilik | Status |
|---|---|---|
| `tagihan` | 4 layar siswa | **diperbaiki r78693** (ref khas per kaki) |
| `cicilan_pembayaran` | Cicilan Mahasiswa + Dibayar Dimuka | aman — `null` vs `'dimuka'` |
| `pertangungjawaban` | LPJ + Pengembalian | aman — `null` vs `'pengembalian'` |
| `pemesanan_pengadaan_master_asset` | Pemesanan DP + Jurnal Balik DP | aman — `null` vs `'DP_BALIK_PEKERJAAN'`; berkasnya bahkan sudah memuat Javadoc yang menjelaskan kewajiban saringan `ref is null` |
| `deposit` | (positif palsu) | pemilik tunggal — layar deposit siswa memakai kolom `deposit_siswa` |
| `transitori` | (positif palsu) | pemilik tunggal — kecocokan pada `PostingUangMukaAction` hanyalah teks Javadoc `jika transitori=true` |

Tidak ada sisa risiko tabrakan pada kelas ini.
