# 24 — Bayar & layani otomatis setelah lewat jam 24

Versi JSP sudah lama menandai pesanan lama sebagai terbayar, tapi hanya sebagai efek
samping saat halaman dibuka, dan hanya bisa diatur global. Yang dibangun: pengaturannya
dijadikan eksplisit (global **dan** per toko), ditambah perlakuan **layani otomatis**, lalu
dijalankan penjadwal harian.

## Aturan

`ais.action.master.koperasi.OtomatisPesananUtil`:

```java
public static final String KUNCI_BAYAR  = "otomatis_verifikasi_bayar_setelah_jam_24";
public static final String KUNCI_LAYANI = "otomatis_layani_setelah_jam_24";

public static boolean bayarOtomatis(Toko toko);
public static boolean layaniOtomatis(Toko toko);
```

`Toko` mendapat dua kolom **tri-state**: `otomatisBayarSetelahJam24` dan
`otomatisLayaniSetelahJam24`.

| Nilai per toko | Arti |
|---|---|
| `null` | ikut pengaturan global |
| `TRUE`/`FALSE` | toko menentukan sendiri, mengalahkan global |

Tri-state, bukan sekadar sakelar mati: tanpa nilai ketiga, toko yang **belum pernah
disentuh** tidak bisa dibedakan dari toko yang **sengaja mematikannya**, sehingga akan ikut
menyala begitu global dinyalakan — kebalikan dari maksud pengaturan per toko.

Keduanya **MATI secara bawaan**. Menyalakannya berarti sistem menganggap uang sudah
diterima atau barang sudah diserahkan tanpa ada orang yang mengonfirmasi; itu harus
keputusan sadar pengelola.

Konfigurasi yang **tidak terbaca** diperlakukan sebagai MATI. Gagal ke arah "tidak
memproses apa-apa" jauh lebih aman daripada menandai transaksi terbayar atas dasar keadaan
yang tidak diketahui.

## Layar

Konfigurasi → Profil Toko, bagian **"Proses Otomatis Setelah Lewat Jam 24"**:

- per toko: tiga `ChoiceChip` (`Ikut global (aktif/nonaktif)` · `Aktifkan` · `Matikan`),
  disertai baris "Berlaku sekarang: AKTIF/NONAKTIF untuk toko ini" supaya nilai efektifnya
  terbaca tanpa harus menghitung sendiri;
- global: dua `Switch`, **hanya untuk admin**, disimpan lewat aksi
  `otomatis_pesanan_global_simpan` yang juga memperbarui `MemoryDbUtil.getKonfigurasi()`
  agar tidak perlu restart.

Tombol simpan global sengaja terpisah dari tombol simpan profil toko: cakupannya berbeda —
satu menyentuh semua toko, satunya hanya toko ini.

JSP `_draft_pesanan_anggota.jsp` ikut diubah agar membaca nilai **efektif** lewat
`OtomatisPesananUtil`, bukan konfigurasi global saja. Kalau tidak, layar JSP dan POS
Desktop akan menyimpulkan hal berbeda dari pengaturan yang sama.

## Penjadwal (SVN r77864)

`ais.action.master.koperasi.OtomatisPesananScheduler`, didaftarkan di
`ais.common.AppStartupListener` bersama `DepositoAroScheduler`.

| Pemicu | Waktu |
|---|---|
| Siklus harian | **00:30** waktu server |
| Siklus penyusul | 5 menit setelah aplikasi hidup |

Jadwalnya dipatok ke **jam dinding**, bukan "24 jam sejak aplikasi hidup" — kalau tidak,
waktunya bergeser setiap kali server di-restart. Siklus penyusul adalah jaring pengaman
untuk server yang mati semalaman; tanpa itu pesanan kemarin menganggur sampai malam
berikutnya.

Diproses hanya toko dengan pengaturan **efektif** menyala, dan hanya baris
`DATE(...) < CURRENT_DATE` — jendela yang sama persis dengan halaman Pesanan, supaya
penjadwal dan layar tidak menyapu rentang berbeda.

### Layani otomatis

`UPDATE koperasi.pembelian SET terlayani = true WHERE COALESCE(terlayani,false) = false
AND DATE(waktu) < CURRENT_DATE AND toko = ?` — idempoten, aman diulang.

### Bayar otomatis — tiga batas yang disengaja

Ini menyentuh uang tanpa ada orang yang mengawasi, jadi:

1. Memakai jalur **`KantinHelper.bayar` yang sama dengan kasir**, bukan `UPDATE` langsung —
   supaya transaksi, stok, diskon, dan jejak auditnya terbentuk persis seperti pembayaran
   biasa.
2. Jalur itu menuntut identitas pengguna. Draft yang **tidak menyimpan penggunanya
   DILEWATI** dan dihitung terpisah (`dilewatiTanpaPengguna`), bukan dibayarkan atas nama
   akun lain. Pembayaran atas nama orang yang keliru lebih buruk daripada pembayaran yang
   tertunda.
3. Tiap draft diproses sendiri-sendiri; satu yang ditolak (mis. stok habis) tidak
   menghentikan sisanya.

Pemindaian dan pembayaran memakai **sesi terpisah** — `KantinHelper.bayar` membuka sesinya
sendiri, dan menahan dua sesi di satu utas pernah menjadi sumber sesi menggantung di modul
ini. Kegagalan satu siklus tidak menghentikan penjadwal; utasnya daemon supaya tidak
menahan proses saat aplikasi berhenti.

## Uji

6/6 pada perhitungan waktu ke jadwal berikutnya — bagian yang paling mudah salah: keliru
tanda atau lupa menambah hari membuat siklus berjalan seketika terus-menerus, atau tidak
pernah sama sekali. Dari pukul 03:12, hasilnya 1.277 menit ke 00:30, cocok dengan hitungan
manual (±1 menit).

## Risiko yang belum ditangani

Penjadwal berjalan **per instance aplikasi**. Bila nanti servernya lebih dari satu,
keduanya menjalankan siklus yang sama pada jam yang sama. Untuk layani otomatis itu tidak
berbahaya (idempoten), tetapi **bayar otomatis perlu penguncian** agar satu draft tidak
diproses dua kali. Selama dijalankan single-instance, ini belum menjadi masalah.

## Insiden identitas pemesan dan waktu pesan (28 Agustus 2026)

### Gejala

Pesanan yang sore hari masih berstatus **Belum dibayar** dapat menjadi **Lunas &
Selesai** sesudah proses H+1. Pada kasus yang dilaporkan, nama `REKTORAT` berubah
menjadi `Masyarakat Umum`, waktu pesan ikut berubah menjadi waktu finalisasi, dan
transaksi Rp300.000 tidak muncul pada laporan Pembayaran Tenant yang bergantung
pada identitas member.

### Akar masalah

Pemanggil otomatis mengirim `draftPembelianAnggotaKoperasi`, tetapi tidak mengirim
`id_member`. `KantinHelper.bayar` sebelumnya menafsirkan field yang tidak dikirim
sebagai pembeli kosong, kemudian `sinkronkanRincianDraftUntukFinalisasi` menulis
nilai kosong dan waktu pembayaran kembali ke header draft. Akibatnya bukti waktu
pesan serta relasi pembeli pada draft ikut berubah ketika transaksi difinalisasi.

### Kontrak wajib setelah perbaikan

1. Finalisasi draft mewarisi member dari header draft jika payload tidak membawa
   `id_member`.
2. Proses otomatis tidak boleh memfinalisasi draft yang memang tidak mempunyai
   member. Pengguna mendapat penjelasan untuk membuka **Pesanan → Detail**,
   memeriksa Nama Pemesan, lalu memproses manual atau meminta pemulihan referensi
   member. Membuat transaksi pengganti dilarang sebelum dipastikan tidak ganda.
3. Header draft mempertahankan **waktu pesan asli**. Waktu transaksi final tetap
   boleh memakai waktu pembayaran, tetapi tidak boleh menimpa sejarah pesanan.
4. Kanal halaman diberi penanda `kanalCheckout=otomatis_halaman`; scheduler memakai
   `otomatis_jadwal`. Ini memungkinkan gerbang otomatis membedakannya dari tombol
   Bayar manual.
5. Laporan tenant tidak boleh dijadikan satu-satunya alat pencarian transaksi yang
   sudah terlanjur kehilangan member. Rekonsiliasi memakai relasi draft→transaksi,
   toko, rincian item, nominal, dan jejak audit terlebih dahulu.

### Prosedur aman untuk data yang sudah terlanjur terdampak

1. Jangan menghapus transaksi dan jangan menginput ulang pembayaran.
2. Catat kode pesanan/transaksi, toko, waktu pesan asli, nama member, rincian item,
   dan total.
3. Administrator mencari satu pasangan draft→transaksi yang sama dan memeriksa
   audit perubahan sebelum koreksi.
4. Pulihkan referensi member dan waktu header draft hanya pada ID yang sudah
   terverifikasi; koreksi transaksi final mengikuti persetujuan supervisor.
5. Jalankan ulang laporan pada periode **tanggal transaksi final**, lalu cocokkan
   total transaksi dan total pembayaran tenant.

Jika pengguna hanya memiliki screenshot, administrator harus meminta kode pada
tombol **Detail**. Nominal dan nama toko saja belum cukup aman untuk menentukan
baris yang boleh dikoreksi.

Kueri berikut **read-only** dan dapat dipakai untuk menemukan kandidat beserta
nilai member/waktu sebelum dan sesudah finalisasi. Jangan mengubah data dari hasil
ini sebelum kandidatnya tunggal dan cocok dengan rincian item pada screenshot:

```sql
SELECT a.id AS draft_id, a.rev, a.revtype,
       to_timestamp(r.revtstmp / 1000) AS waktu_revisi,
       a.kode, a.toko, a.anggota_koperasi, a.tanggal_pembayaran,
       a.lunas, a.total_biaya
FROM new_audit.draft_pembelian_anggota_koperasi__audit a
JOIN new_audit.revinfo r ON r.rev = a.rev
WHERE a.total_biaya BETWEEN 299999.50 AND 300000.50
  AND to_timestamp(r.revtstmp / 1000) >= DATE '2026-08-19'
  AND to_timestamp(r.revtstmp / 1000) <  DATE '2026-08-23'
ORDER BY a.id, a.rev;
```
