# Manual Posting & Laporan Keuangan -- POS eBisnis

`Manual-Posting-Laporan-Keuangan-eBisnis.pdf` (10 halaman): Jurnal Umum, Posting Kulakan/Bayar
Hutang/Terima Piutang, Posting HPP/Penjualan, Saldo Awal/Jurnal Penyesuaian/Tutup Buku, dan
Katalog Laporan Keuangan (Laporan Resmi Komparatif, Buku Besar Resmi, Arus Kas & Analisa).

Dibangun dengan `susun.py` (memuat `isi.py` lewat `exec`), memakai pustaka maket `mockup.py`.
Jalankan dari direktori ini: `python susun.py`.

**Kenapa ilustrasi, bukan tangkapan layar interaktif:** dicatat lengkap di
[docs/pos/110-uat-otomasi-tidak-bisa-jalan-di-lingkungan-ini.md](../110-uat-otomasi-tidak-bisa-jalan-di-lingkungan-ini.md).
Dua halaman (Lampiran D) memakai tangkapan layar nyata; sisanya ilustrasi tata letak dengan
label/kolom/pesan dikutip apa adanya dari kode sumber, warna disampel dari tangkapan nyata.

Jebakan yang ditemukan dan diperbaiki saat menyusun `mockup.py`: fungsi `_bingkai()` menggambar
daftar 12 grup sidebar dengan tinggi TETAP, tetapi tinggi kotak dihitung dari isi tabel/formulir
-- bila isinya pendek, teks sidebar meluber ke luar kotak dan tumpang tindih elemen berikutnya
di halaman. Diverifikasi dan diperbaiki dengan memeriksa koordinat blok teks PDF secara
terprogram (`page.get_text('blocks')`), bukan hanya dengan mata -- lihat riwayat commit.
