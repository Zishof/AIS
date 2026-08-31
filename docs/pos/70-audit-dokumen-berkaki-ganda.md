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

## 5. Sisa

Tidak ada temuan terbuka dari kelas ini. Lima entitas ber-cap ganda sudah diperiksa
seluruhnya; tiga cacat diperbaiki, sisanya terverifikasi benar.
