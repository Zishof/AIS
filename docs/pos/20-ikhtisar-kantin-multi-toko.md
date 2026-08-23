# POS Desktop & Android (eBisnis) — Kantin, Multi-Toko, Audit & Proses Otomatis

Dokumentasi rangkaian pekerjaan kedua di `docs/pos/`, **di luar** modul Akuntansi &
Keuangan yang dibahas [README.md](README.md) dan berkas 01–08. Cakupannya: hak akses
harga, filter toko lintas-toko, pembatasan per pendaftar, proses otomatis lewat jam 24,
penelusuran tabel audit, perbaikan pengiriman toko, laporan stok per tanggal, dan
aplikasi anggota eCanteen.

> Nama kelas, metode, kolom, nomor revisi SVN, dan nomor commit git di seluruh berkas ini
> **diverifikasi dari source**, bukan diingat. Angka hasil uji diambil dari harness yang
> benar-benar dijalankan. Bagian yang **belum** terbukti dinyatakan belum terbukti.

---

## Daftar isi

| Berkas | Isi |
|---|---|
| [21-hak-akses-ubah-harga.md](21-hak-akses-ubah-harga.md) | Izin ubah harga per peran + per pengguna, harga sebagai label |
| [22-filter-toko-dan-pendaftar.md](22-filter-toko-dan-pendaftar.md) | "Boleh melihat seluruh toko", combo filter, pembatasan per pendaftar |
| [23-toko-pada-payload.md](23-toko-pada-payload.md) | Bug "Toko tidak diketahui" dan sapuan 13 aksi lain |
| [24-proses-otomatis-jam-24.md](24-proses-otomatis-jam-24.md) | Bayar/layani otomatis: konfigurasi global & per toko, penjadwal harian |
| [25-riwayat-audit-history.md](25-riwayat-audit-history.md) | Tombol History: jelajah tabel audit + restore satuan/massal |
| [26-produk-kulakan-laporan.md](26-produk-kulakan-laporan.md) | Stok per tanggal, ekspor laporan, salinan tersimpan, cetak faktur |
| [27-ecanteen-aplikasi-anggota.md](27-ecanteen-aplikasi-anggota.md) | Aplikasi member kantin (Android + Desktop) |
| [28-harness-uji-dan-temuan.md](28-harness-uji-dan-temuan.md) | Uji yang dijalankan, hasilnya, dan jebakan yang ditemukan |

---

## Status: apa yang sudah jadi, apa yang belum sampai ke pengguna

Seluruh kode sudah ter-commit dan kedua cermin SVN identik. Yang **belum** terjadi:

| Butuh | Keterangan |
|---|---|
| Deploy server ke **r77896+** | Tanpa ini: penjadwal tidak berjalan, combo filter toko, pembatasan pendaftar, konfigurasi otomatis, dan layar History semuanya diam |
| Build ulang POS Desktop | Rilis v1.33.68 terbit pukul 00:57 WIB 21 Agu; commit pertama rangkaian ini masuk 03:01 WIB — **tidak satu pun** perubahan di dokumen ini ikut di build itu |

Deploy adalah pekerjaan pengelola, bukan bagian dari pekerjaan ini.

## Revisi & commit

| Perubahan | SVN | Git (`Zishof/zishof-platform`) |
|---|---|---|
| Penjadwal proses otomatis | r77864 | — |
| Konfigurasi proses otomatis (layar) | — | `6ed8b07` |
| Jelajah audit lintas baris | r77873 | `ee7665c` |
| Restore satuan & massal | r77877 | `fa8f446` |
| Profil toko: toko dibaca = toko ditulis | r77896 | `8de0b99` |
| Penyisipan toko terpusat (13 aksi) | — | `248400c` |
| Stok per tanggal, ekspor, cetak faktur | — | `7722f41`, `f4651c9` |

---

## Prinsip yang dipegang

**Mesinnya satu, bukan tiruan.** Aturan yang sudah ada di layar ZK dipanggil ulang, bukan
ditulis ulang. Bayar otomatis memakai `KantinHelper.bayar` yang sama dengan kasir supaya
transaksi, stok, diskon, dan jejak auditnya terbentuk identik. Aturan pemilihan revisi
pada restore disalin persis dari `GenericRevisiHelper.restoreLatestFromDate`.

**Gagal ke arah yang aman.** Konfigurasi yang tidak terbaca berarti MATI, bukan menyala.
Draft tanpa identitas pengguna DILEWATI, bukan dibayarkan atas nama akun lain. Toko yang
tidak diketahui menghasilkan penolakan, bukan tebakan — karena menulis ke toko yang salah
tidak memunculkan galat apa pun dan bisa bertahan lama sebelum ketahuan.

**Yang tertulis di layar = yang terkirim.** Kotak toko di kiri atas menampilkan satu nama
toko; setiap layar yang menulis ke satu toko harus menulis ke toko itu. Ketidakcocokan di
sini adalah sumber salah-tulis paling senyap yang ditemukan sepanjang pekerjaan ini
(lihat [23](23-toko-pada-payload.md)).

**Alasan ditulis di kode.** Working copy SVN ini disapu alat otomatis yang meng-commit
apa pun yang kotor dengan pesan kosong, sehingga pesan commit sering tidak terbentuk.
Setiap keputusan yang tidak kelihatan dari kodenya ditulis sebagai Javadoc/komentar di
tempatnya.

**Cermin SVN.** `src/main/src` dan `src/main/java` adalah dua salinan berkas yang sama dan
harus selalu identik. Setiap perubahan diverifikasi identik sebelum dianggap selesai.
`src/main/docs` adalah working copy tersendiri (`svn://…/ais/docs`), tidak punya cermin.

**Java 1.7 bergaya 1.6.** Tanpa lambda, tanpa diamond, tanpa stream, tanpa
try-with-resources. `openSession()` selalu ditutup lewat
`HibernateUtil.closeSessionQuietly` di blok `finally`.

**Tidak ada DDL manual.** Kolom dan tabel baru dibuat Hibernate (`hbm2ddl.auto=update`)
saat boot pertama. Cadangkan basis data sebelum deploy.
