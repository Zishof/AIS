# Posting Massal "Pengembalian Uang Muka" + Perbaikan Hitung Dasbor Bendera-Null

Tanggal: 29 Agustus 2026. Kode masuk SVN **r78530** (`PostingJurnalHelper`, terbawa
commit sesi paralel, diverifikasi byte-identik) dan **r78531**
(`PostingPertangungjawabanPengembalianAction`, `DraftJurnalRingkasanUtil`,
`DraftJurnalApiHelper`); perbaikan layar ZK-nya menyusul di **r78539** (§2a).
Mirror `java/` selaras. Lanjutan dari
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
2. **SQL batal cacat sintaks.** Tombol batal PER BARIS menjalankan
   `delete ... where ref='pengembalian' pertangungjawaban=<id> ...` — tanpa `AND` — yang
   selalu melempar error; penandanya sempat dilepas tetapi jurnalnya (kalau ada) yatim.
   (Koreksi atas catatan awal dokumen ini yang menyebut "kedua tombol batal": SQL tombol
   batal MASSAL ternyata sudah ber-`AND` dan benar — hanya jalur per baris yang cacat.)

Mesin API menulis jurnal yang dijanjikan tampilan (Dr akun kelebihan / Cr akun jenis
uang muka, senilai `dikembalikan`, tanggal `tanggalPersetujuan`, `ref='pengembalian'`
via konstanta baru `PostingJurnalHelper.REF_PENGEMBALIAN`), dan pembatalannya memakai
SQL yang benar — filter `ref` WAJIB supaya jurnal LPJ utama (ref null) dokumen yang sama
tidak ikut terhapus. Layar ZK-nya sendiri TIDAK diubah pada r78531; kedua cacatnya
diperbaiki menyusul di **r78539** (§2a).

Dokumen yang jurnalnya pasti nol (dikembalikan = 0) tidak dihitung dasbor dan tidak
diproses mesin — layar ZK menampilkan semua LPJ tanpa saringan itu.

## 2a. Perbaikan layar ZK (r78539, 29 Agustus 2026)

Kedua cacat §2 diperbaiki di layar ZK-nya (`PostingPertangungjawabanPengembalianAction`
r78539; mirror `java/` selaras byte-identik). Mesin API di bagian bawah file TIDAK
disentuh.

- Kedua jalur tulis (onPostingSemua massal dan tombol posting per baris) kini menulis
  Dr `jenisUangMuka.akunKelebihan` / Cr `jenisUangMuka.akun` — pasangan yang sama dengan
  tampilan grid dan mesin API: `akunsKredits` diisi `akunKredit` (bukan lagi `akunDebet`),
  dan jalur massal berhenti memakai `uangMuka.akun` sebagai debet. Efek samping yang
  disengaja di jalur massal: dokumen ber-jenisUangMuka null / tanpa akun kelebihan kini
  dilewati (dulu ikut terjurnal Dr X / Cr X memakai `uangMuka.akun`) — konsisten dengan
  grid yang menandainya "Transaksi tidak valid".
- SQL tombol batal per baris mendapat `AND` yang hilang
  (`... where ref='pengembalian' and pertangungjawaban=<id> and closing is null`),
  mengikuti bentuk filter `batalkanPostingSemua`. Tombol batal massal tidak diubah —
  sudah benar. Kedua tombol batal ZK tetap menghapus `grup_transaksi` saja tanpa hapus
  anak `akunting.transaksi` eksplisit, sama dengan idiom semua layar ZK posting lain
  (hanya mesin-mesin API yang menghapus anaknya dulu).
- Javadoc yang mendokumentasikan cacat lama (kelas, onPostingSemua, renderer, kedua
  mesin API) dimutakhirkan agar tidak lagi menyebut cacatnya sebagai perilaku berjalan.

Verifikasi: kompilasi `javac -source 1.7 -target 1.7` (classpath `webapp/WEB-INF/lib/*`)
bersih; jalur ZK belum diuji runtime (butuh sesi ZK hidup) — dasar kebenarannya paritas
dengan mesin API yang sudah lulus harness §4.

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
(`PostingPembayaran/Dp/TerminAction`) — SELESAI r78536+r78540, terdokumentasi dan
teraudit di [57-posting-pembayaran-vendor.md](57-posting-pembayaran-vendor.md) — lalu
Perjanjian Kerjasama, payroll Pegawai/Penggajian, Saldo Awal Kas Kecil, dan trio
kantin (batch per periode).
