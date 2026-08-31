# Audit Dokumen Ber-Kaki Posting Ganda: Tiga Cacat Pembatalan

Tanggal: 31 Agustus 2026. Kode masuk SVN **r78672** (`PostingBiayaAdministrasiPembayaranMahasiswaAction`,
`PostingBiayaPaymentGatewayPembayaranMahasiswaAction`, `PostingPertangungjawabanKasBesarAction`
— terbawa commit sapuan sesi paralel TANPA pesan log; isinya diverifikasi byte-identik dengan
yang dimaksud, dokumen ini catatan penggantinya) dan **r78673**
(`PostingCicilanMahasiswaAction`). Mirror `java/` selaras. Lanjutan dari
[69-audit-tombol-zk-menyeluruh.md](69-audit-tombol-zk-menyeluruh.md).

## 1. Kelas risiko yang belum pernah diperiksa siapa pun

Sapuan dok 69 memeriksa lima kelas cacat per LAYAR. Yang luput: cacat yang hanya muncul
ketika **satu dokumen memikul lebih dari satu kaki jurnal**. Pembatalan kaki A menghapus
jurnal berdasarkan kolom referensi dokumen (`log_pembayaran=`, `cicilan_pembayaran=`, dst.);
bila tidak ada saringan pembeda kaki, jurnal kaki B ikut lenyap **padahal capnya tetap
terpasang** — dokumen tampak terposting sementara jurnalnya sudah tidak ada.

Repo sudah punya obatnya (`PostingDanaAnggotaUtil.hapusJurnalJenis`, r78651, untuk modal
penyertaan masuk vs kembali), tetapi obat itu belum pernah diperiksa keberlakuannya pada
keluarga dokumen ber-kaki ganda yang lain.

Lima entitas ber-cap ganda di sistem:

| Entitas | Kaki | Pembeda kaki di basis data |
|---|---|---|
| `LogPembayaran` | utama (biaya administrasi) + PaymentGateway | **tidak ada** — keduanya ref NULL & jenis riwayat sama |
| `CicilanPembayaran` | utama + Dimuka | `ref` (`null` vs `'dimuka'`) |
| `Pertangungjawaban` | utama + Pajak + Pengembalian | `ref` |
| `PertangungjawabanKasBesar` | utama + Pajak + Pengembalian (dua terakhir belum dipakai) | `ref` |
| `Tagihan` (siswa) | utama + Denda + Diskon + UangMuka | `jenis` |

## 2. Tiga cacat yang ditemukan

### 2.1 `LogPembayaran` — pembatalan silang, LIVE (r78672)
Kedua mesin (`batalkanPostingSemua` biaya administrasi dan biaya payment gateway)
menghapus dengan `log_pembayaran=<id> and closing is null` **tanpa pembeda apa pun**.
Kedua kaki ini benar-benar dipakai (dua baris dasbor: "Mahasiswa - Biaya Administrasi" dan
"Mahasiswa - Biaya Payment Gateway"), keduanya menulis `ref` NULL lewat overload
`saveTransaksi` tanpa ref, dan keduanya memakai jenis riwayat yang sama
(`JENIS_MAHASISWA`) — jadi **ref maupun jenis tidak bisa membedakannya**.

Akibat: membatalkan salah satu kaki menghapus jurnal kaki lainnya, yang capnya tetap
terpasang. Perbaikan: saringan `and posting_history=<id riwayat yang ditunjuk cap>` —
satu-satunya pembeda yang tersedia — plus penjagaan lewat/`continue` bila capnya kosong.

Catatan: tombol ZK kedua layar ini hanya melepas cap TANPA menghapus jurnal sama sekali
(idiom lama layar tersebut), jadi cacat ini murni pada jalur mesin/API.

### 2.2 `CicilanPembayaran` — jurnal yatim karena semantik NULL, LIVE (r78673)
Kaki utama cicilan mahasiswa ditulis tanpa ref (⇒ `ref` NULL). Kedua tombol batal di
layarnya menyaring dengan `ref != 'dimuka'` saja. Pada SQL tiga-nilai, `NULL != 'dimuka'`
bernilai **NULL**, bukan true — sehingga baris kaki utama **tidak pernah terhapus**: cap
dilepas, jurnalnya tertinggal di buku besar. Jalur per-baris bahkan menggabungkan
`(ref is null or ref='')` DENGAN `ref != 'dimuka'`, yang untuk ref NULL tetap gagal.

Mesin API di berkas yang sama sudah memakai bentuk yang benar sejak awal —
`(ref is null or ref != 'dimuka')` — jadi perbaikannya adalah menyamakan kedua tombol
dengan mesinnya sendiri.

Ini kembaran persis dari cacat dok 54 (SQL batal kehilangan `AND` ⇒ "penanda dilepas tapi
jurnal yatim"), hanya penyebabnya semantik NULL, bukan sintaks.

### 2.3 `PertangungjawabanKasBesar` — inkonsistensi laten (r78672)
Kedua tombol batal di layarnya menyaring `ref is null` (hanya kaki utama), tetapi mesin
API-nya menghapus tanpa saringan itu. Entitas ini membawa cap `postingHistoryPajak` dan
`postingHistoryPengembalian` yang **belum dipakai siapa pun**, jadi hari ini kedua bentuk
menghapus himpunan yang sama — belum ada kerugian nyata. Disamakan dengan tombolnya karena
begitu salah satu kaki itu diimplementasikan (persis yang sudah terjadi pada LPJ uang muka),
mesin akan diam-diam melenyapkan jurnalnya.

## 3. Yang diperiksa dan ternyata BENAR

- `Pertangungjawaban` (LPJ uang muka): ketiga jalur kaki utama dan ketiga jalur kaki
  pengembalian menyaring `ref` dengan benar.
- `Tagihan` siswa (empat kaki): keempat layar menyaring `jenis`.
- Kaki `dimuka` cicilan: memakai kesetaraan `ref='dimuka'` — aman dari jebakan NULL.
- Pemakaian `ref !=` di tempat lain (`PostingPemesananPekerjaanAction`,
  `GrupTransaksiAction`, `PostingJurnalHelper`) selalu berpasangan dengan
  `ref is not null` lebih dulu, jadi tidak kena jebakan yang sama.
- Modal penyertaan masuk/kembali: sudah bersaring `jenis` lewat `hapusJurnalJenis`.

## 3a. Pengujian (harness `TesBatalKakiGanda`)

Scratchpad, DB UAT `ais`, fixture `UATKG-` rentang **1–10 Maret 2092**. Setiap perbaikan
diuji berpasangan dengan **KONTROL yang menjalankan bentuk LAMA** atas fixture kembar —
kontrol WAJIB merusak, supaya terbukti harness ini memang bisa membedakan kode rusak dari
kode benar dan tidak lulus karena kebetulan tidak menguji apa-apa.

| Skenario | Hasil |
|---|---|
| L1: dua kaki (administrasi + payment gateway) terjurnal dan tercap | prasyarat terpenuhi |
| Batal kaki ADMINISTRASI lewat mesinnya | jurnal kaki administrasi terhapus; **jurnal kaki PG beserta dua baris transaksinya SELAMAT**; cap administrasi lepas, cap PG tetap terpasang |
| Batal kaki PG sesudahnya | jurnalnya terhapus, capnya lepas — kedua kaki bisa dibatalkan berurutan tanpa saling merusak |
| **KONTROL** predikat lama (`log_pembayaran=<id>` tanpa pembeda) atas L2 | menghapus **KEDUA** kaki — cacat 2.1 terbukti nyata, bukan teoretis |
| **KONTROL** predikat lama cicilan `ref != 'dimuka'` | menghapus **NOL** baris; kaki utama ber-ref NULL tetap tinggal = jurnal yatim |
| Predikat baru `(ref is null or ref != 'dimuka')` | menghapus tepat 1 baris (kaki utama); kaki `dimuka` selamat |
| Batal PJ Kas Besar lewat mesinnya, dengan kaki pajak disimulasikan (`ref='pajak'`) | kaki utama terhapus; **jurnal ber-ref pajak beserta barisnya SELAMAT** — perlindungan kaki masa depan bekerja |
| Kebersihan fixture | sisa data uji = 0 |

**LULUS 18, GAGAL 0.**

Catatan cakupan yang jujur: perbaikan cicilan (2.2) ada di dalam event listener ZK yang
tidak bisa dipanggil headless, jadi yang diuji adalah **predikat SQL-nya persis** — bentuk
lama vs bentuk baru — atas baris fixture yang sama, bukan pemanggilan handler-nya. Dua
perbaikan lain diuji lewat pemanggilan mesin sungguhan.

Jebakan fixture baru yang ditemukan harness ini: kolom `LogPembayaran.biayaAdministrasi`
dan `biayaPaymentGateway` dipetakan **`biayaadministrasi` / `biayapaymentgateway`** (huruf
kecil polos tanpa garis bawah — properti tanpa `@Column`, keluarga jebakan yang sama dengan
`saldoawal` pada dok 58); `akunting.kas_besar` KOSONG di UAT sehingga fixture PJ Kas Besar
harus membuat dokumen kas besarnya sendiri (wajib hanya `id` dan `nama`).

## 4. Catatan metode (untuk pemeriksa berikutnya)

Sapuan pertama **melewatkan cacat 2.1** karena predikatnya dirakit lewat variabel:

```java
String syarat = "log_pembayaran=" + log.getId() + " and closing is null";
session.createSQLQuery("delete from akunting.grup_transaksi where " + syarat)
```

Nama kolomnya tidak pernah muncul di literal string milik `createSQLQuery`, sehingga
pemindai berbasis literal tidak melihat apa pun. Pemindai harus **menyisipkan isi variabel
`String`** yang dirujuk pernyataan itu sebelum menilai. Pelajaran yang sama berlaku untuk
pemindaian SQL apa pun di repo ini.

Deteksi saringan juga harus mengenali `ref !=` / `ref <>`, bukan hanya `ref is null` dan
`ref='...'`; putaran pertama menandai jalur cicilan sebagai "tanpa saringan" (positif palsu)
padahal cacat sebenarnya justru pada BENTUK saringannya.

## 5. Sapuan lanjutan: lima kelas cacat lain (31 Agustus 2026)

Sesudah kelas kaki-ganda tuntas, lima kelas berikutnya disapu atas seluruh repo. Satu
temuan nyata, empat bersih.

### 5.1 TEMUAN — hapus Jurnal Umum lewat API menembus periode tutup buku (r78681)

`JurnalUmumApiHelper.hapus()` hanya menolak jurnal yang sudah punya `postingHistory`.
Jurnal umum **diketik manual** sehingga lazim ber-`postingHistory` null — akibatnya entri
di dalam periode yang sudah ditutup buku tetap dapat dihapus lewat API, dan angka periode
terkunci ikut berubah. Asimetri yang mencolok: jalur **simpan** di berkas yang sama sudah
menolak tanggal sebelum closing terakhir; hanya jalur hapus yang tidak berpenjaga.

Perbaikan memasang dua penjagaan yang mencerminkan jalur simpan: penanda `closing` pada
barisnya, dan tanggal transaksinya terhadap closing terakhir (menangkap baris di periode
tertutup yang belum sempat bercap).

**Diuji** dengan harness `TesPenjagaClosingJurnalUmum` (fixture `UATJU-`, jendela April–Mei
2092 — tabel `akunting.closing` kosong di UAT sehingga fixture membuat closing sendiri dan
menghapusnya lagi di akhir supaya harness lain tidak terpengaruh):

| Skenario | Hasil |
|---|---|
| A: baris bercap `closing` | hapus DITOLAK, jurnal beserta dua barisnya utuh |
| B: tanpa cap, tanggal sebelum closing terakhir | hapus DITOLAK dengan pesan menyebut tanggal dan batas closing |
| C: **KONTROL** — jurnal sah sesudah periode closing | hapus BERHASIL dan benar-benar terhapus; penjaganya presisi, bukan pemblokir borongan |

**LULUS 7, GAGAL 0.**

### 5.2 Empat kelas yang disapu dan BERSIH

| Kelas | Cara periksa | Hasil |
|---|---|---|
| Hapus jurnal tanpa penjaga `closing` | semua pernyataan delete atas `grup_transaksi`/`transaksi`, jendela ±8 baris | selain §5.1: sisanya konteks lain yang sah — pembersih duplikat, hapus baris draf `simpan=false`, javadoc |
| Penghitung closing diam-diam nol | properti `hitungClosing` vs whitelist `ENTITAS_CLOSING` | bersih: `hitungClosing` tidak memvalidasi whitelist melainkan memakai properti `GrupTransaksi` langsung, dan keempat properti di luar whitelist (`danaTalangan`, `penggantianKasKecil`, `pertangungjawabanKasBesar`, `uangMuka`) memang ADA; whitelist hanya menjaga parameter URL drill-through, dan ke-19 nama yang dikirim layar ZK semuanya lolos |
| Posting dobel (mesin tanpa saringan `isNull(cap)`) | badan tiap `postingSemua` statis | bersih; satu-satunya laporan (`PostingProsesTransferAction`) positif palsu — saringannya ada di luar jangkauan regex |
| Kebersihan pembatalan (lepas cap tanpa hapus jurnal / hapus grup tanpa hapus anak) | badan tiap `batalkan*` statis | bersih; `PostingTransaksiHarianAction` memang SENGAJA tidak menghapus jurnalnya — untuk Jurnal Umum baris jurnal ITU dokumen ketikan pengguna, jadi pembatalan hanya melepas cap (dan sudah berpenjaga `closing`) |
| Baris dasbor ber-cap non-baku tak dipetakan | entitas ber-cap ganda vs `propertiPosting` | bersih: setiap kaki yang punya baris dasbor sudah dipetakan; empat baris yang tertandai kata kunci ternyata berada di entitas ber-cap tunggal |

## 6. Sisa

Tidak ada temuan terbuka dari kelas-kelas ini. Lima entitas ber-cap ganda dan lima kelas
cacat lanjutan sudah diperiksa seluruhnya; empat cacat diperbaiki dan teruji, sisanya
terverifikasi benar.
