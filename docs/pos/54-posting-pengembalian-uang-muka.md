# Posting Massal "Pengembalian Uang Muka" + Perbaikan Hitung Dasbor Bendera-Null

Tanggal: 29 Agustus 2026. Kode masuk SVN **r78530** (`PostingJurnalHelper`, terbawa
commit sesi paralel, diverifikasi byte-identik) dan **r78531**
(`PostingPertangungjawabanPengembalianAction`, `DraftJurnalRingkasanUtil`,
`DraftJurnalApiHelper`). Mirror `java/` selaras. Lanjutan dari
[53-posting-jurnal-umum.md](53-posting-jurnal-umum.md).

## 1. Apa yang ditambahkan

Baris dasbor **baru** "Pengembalian Uang Muka" (kunci `pj_pengembalian`, ditempatkan
setelah "Pertanggungjawaban Uang Muka"): sisa uang muka yang dikembalikan pada LPJ
(`Pertangungjawaban.dikembalikan` tidak nol, sudah disetujui) dipantau dari draf sampai
menjadi jurnal pengembalian, lengkap dengan posting/batal massal dari POS. Kunci izinnya
menumpang `pj_uang_muka` — satu keluarga dengan LPJ induknya. Client Flutter tidak
diubah (baris dan tombol digerakkan data server).

Status posting dokumen dibaca dari properti `postingHistoryPengembalian` (bukan
`postingHistory` baku — dua field berbeda di entitas yang sama), lewat pemetaan baru di
`propertiPosting` dan penghitung `hitungProperti`.

## 2. Dua cacat layar ZK yang sengaja TIDAK diwarisi

Layar ZK `PostingPertangungjawabanPengembalianAction` ternyata belum pernah bisa
menghasilkan jurnal pengembalian yang benar:

1. **Jurnal Dr X / Cr X.** Grid layar menampilkan jurnal yang benar
   (Dr `jenisUangMuka.akunKelebihan` / Cr `jenisUangMuka.akun`), tetapi KEDUA tombolnya
   (massal maupun per baris) menulis `akunsKredits.add(akunDebet)` — debet dan kredit
   jatuh ke akun yang sama, jurnalnya saling meniadakan. Jalur massal bahkan memakai
   akun debet yang lain lagi (`uangMuka.akun`, bukan `akunKelebihan`).
2. **SQL batal cacat sintaks.** Kedua tombol batal menjalankan
   `delete ... where ref='pengembalian' pertangungjawaban=<id> ...` — tanpa `AND` — yang
   selalu melempar error; penandanya sempat dilepas tetapi jurnalnya (kalau ada) yatim.

Mesin API menulis jurnal yang dijanjikan tampilan (Dr akun kelebihan / Cr akun jenis
uang muka, senilai `dikembalikan`, tanggal `tanggalPersetujuan`, `ref='pengembalian'`
via konstanta baru `PostingJurnalHelper.REF_PENGEMBALIAN`), dan pembatalannya memakai
SQL yang benar — filter `ref` WAJIB supaya jurnal LPJ utama (ref null) dokumen yang sama
tidak ikut terhapus. Layar ZK-nya sendiri TIDAK diubah pada revisi ini; perbaikannya
menyusul bila diminta.

Dokumen yang jurnalnya pasti nol (dikembalikan = 0) tidak dihitung dasbor dan tidak
diproses mesin — layar ZK menampilkan semua LPJ tanpa saringan itu.

## 3. Perbaikan hitung dasbor: bendera `posting` null = terposting

`PostingJurnalHelper.terapkanStatusPostingHistory` (dipakai baris Jurnal Umum, Uang
Muka, PJ, Kas Kecil/Besar, Dana Talangan, Pajak, dst. di dasbor POS dan ZK) dulu
menuntut `posting_history.posting=true` untuk terposting dan `false`/kosong untuk draf.
Padahal `PostingHistory` memakai dynamic-insert dan kolomnya tanpa default DB, sehingga
SEMUA posting massal lama (tombol ZK tidak pernah menyetel bendera; begitu pula
sebagian mesin API massal) meninggalkan `posting=null` — dokumen bercapnya **lenyap
dari kedua hitungan**. Kini: dokumen bercap riwayat dihitung terposting kecuali
riwayatnya dinonaktifkan eksplisit (`posting=false`). Menyembuhkan data lama tanpa
migrasi.

Jendela filter ZK per-modul punya salinan inline semantik lama (`posting=true`) — tidak
disentuh; ketidakcocokan tampilan filter di layar ZK lama itu pre-existing dan tercatat
di sini.

## 4. Pengujian

Harness `TesPostingPengembalian` (scratchpad, DB UAT lokal `ais`), fixture berprefiks
`UATPJP-` pada rentang **10–20 Maret 2091** — sengaja jauh di masa depan karena penjaga
closing `CommonAkunting.saveTransaksi` menolak tanggal sebelum closing terakhir, dan
rentang itu dipastikan kosong dulu:

| Skenario | Hasil |
|---|---|
| P: dikembalikan 250k, disetujui, dalam rentang | terposting; 1 grup `ref='pengembalian'`; **debet 250k di akun kelebihan, kredit 250k di akun jenis uang muka**; riwayat `posting=true` |
| Q: dikembalikan 0 | tidak dihitung dasbor, tidak diproses |
| R: jenis uang muka tanpa akun kelebihan | dilewati, tetap draf |
| T: jurnal umum ber-riwayat `posting=null` | dihitung TERPOSTING oleh dasbor (perbaikan §3), tidak dihitung draf |
| Hitung dasbor sebelum/sesudah | draf 2→1, terposting 0→1 — konsisten dengan mesin |
| Posting/batal ulang | n=0 dua-duanya; riwayat kosong run kedua terhapus; jurnal P terhapus saat batal, dokumen kembali draf |

**LULUS 20, GAGAL 0.** Kompilasi `javac -source 1.7 -target 1.7` bersih (4 berkas).

Catatan harness tambahan (melengkapi catatan di dok 53): fixture `akunting.akun` wajib
mengisi `debit_credit` (integer); dan jangan menaruh `System.exit` di `finally` tanpa
`catch (Throwable)` yang mencetak dulu — exit menelan exception yang sedang merambat.

## 5. Sisa peta modul

Lihat §6 dok 53. Dengan baris ini selesai, kandidat berikutnya: trio pembayaran aset
(`PostingPembayaran/Dp/TerminAction`), lalu Perjanjian Kerjasama, payroll
Pegawai/Penggajian, Saldo Awal Kas Kecil, dan trio kantin (batch per periode).
