# 11 — Navigasi menu Akuntansi: deretan tab diganti dropdown grup

Permintaan pengguna: *"jangan pakai tab-tab di sub menu Akuntansi"* — hilangkan deretan tab di
atas halaman, dan jadikan dropdown di sebelah panel menu.

| | |
|---|---|
| Berkas | `apps/ebisnis/lib/widgets/app_shell.dart` (`_DropdownGrupMenu`, `_grupDariMenu`, `_punyaDropdownGrup`), `apps/ebisnis/lib/screens/laporan_screen.dart` |
| Commit | `7a19bd3` |

---

## Masalahnya

Setiap layar Akuntansi menjejer **seluruh submenunya sebagai tab** di atas halaman: Katalog
Laporan, Akun/Perkiraan, Posting HPP, Posting Penjualan, Saldo Awal, Jurnal Penyesuaian
Berkala, Tutup Buku, Posting Kulakan, Posting Bayar Hutang, Posting Terima Piutang.

Daftar itu **sama persis dengan panel menu di sebelah kiri**. Akibatnya dua hal:

1. Pengguna melihat dua daftar menu sekaligus untuk hal yang sama.
2. Begitu isinya belasan, deret tab melebar sampai **memotong judul halaman** — terlihat jelas
   pada layar Posting HPP.

## Yang dikerjakan

**Satu submenu = satu halaman.** `LaporanScreen` tidak lagi membangun `TabBar`/`TabBarView`;
ia menampilkan katalog laporan, atau — bila submenu itu meminta layar pendukung tertentu
(`bukaPosting`) — hanya panel yang diminta. Getter `_pendukungTerpilih` yang menentukannya.

**Perpindahan antarhalaman lewat dropdown grup**, dipasang tepat di bawah judul, bersebelahan
dengan panel menu. Isinya seluruh halaman satu grup sidebar yang boleh dilihat pengguna,
dengan halaman aktif ditandai (ikon + tebal + warna primer).

Dua keputusan yang membuatnya tidak sekadar tambal:

- **Bukan khusus Akuntansi.** `_DropdownGrupMenu` membaca grup sidebar dari `_grupMenu`, jadi
  ia muncul sendiri untuk grup mana pun yang punya lebih dari satu halaman (Pengadaan,
  Transaksi & Laporan, dst). Grup dengan satu halaman tidak menampilkannya sama sekali.
- **Ikut layar sempit.** Di bawah 900dp judul halaman pindah ke AppBar, jadi dropdown dipasang
  tepat di atas badan halaman (`_punyaDropdownGrup` menjaga agar tidak ada ruang kosong pada
  halaman yang tidak butuh).

## Verifikasi

- `flutter analyze`: tidak ada temuan baru pada berkas yang disentuh.
- `flutter test`: 184 lulus saat commit itu dibuat.
- Tidak ada test lama yang bergantung pada `TabBar` di layar ini.
