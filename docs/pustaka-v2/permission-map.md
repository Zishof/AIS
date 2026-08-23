# Permission Map Perpustakaan AIS V2

| Peran | Baca katalog publik | Portal anggota | Reservasi/favorit | Workspace petugas | Integrasi/admin |
|---|---:|---:|---:|---:|---:|
| Anonymous | Ya | Tidak | Tidak | Tidak | Tidak |
| Anggota aktif | Ya | Ya | Ya | Tidak | Tidak |
| Anggota nonaktif/tidak ditemukan | Ya | Ditolak | Ditolak | Tidak | Tidak |
| Pustakawan/library role | Ya | Sesuai akun | Sesuai akun | Ya | Sesuai role |
| Administrator | Ya | Sesuai akun | Sesuai akun | Ya | Ya |

Aturan penting:

- Tombol anggota hanya dirender bila capability backend menyatakan anggota aktif.
- Mutasi tetap diperiksa ulang oleh `LibraryMemberApi`; menyembunyikan tombol bukan mekanisme keamanan.
- Reservasi memverifikasi anggota, item, cabang, duplikasi, metode POST, dan CSRF.
- Detail publik menolak item nonaktif dan status selain Terbit/Publish/Published.
- Header hanya menyajikan navigasi yang relevan; service backend tetap menjadi sumber keputusan.
