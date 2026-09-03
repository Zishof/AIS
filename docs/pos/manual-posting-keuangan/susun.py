# -*- coding: utf-8 -*-
"""Perakit Manual Posting & Laporan Keuangan POS eBisnis.

Sumber tiap fakta (label medan, nama kolom, pesan validasi, nama aksi API) dikutip
di komentar sebelum dipakai -- berkas:baris di repo `CodeBaseDesktopDanMobile`
(Flutter, dirujuk "ebisnis/...") dan `AIS` (Java, dirujuk "ais/..."). Dua halaman
memakai TANGKAPAN LAYAR NYATA (login, beranda Kasir/POS); sisanya ilustrasi tata
letak presisi -- lihat docs/pos/110-uat-otomasi-tidak-bisa-jalan-di-lingkungan-ini.md.
"""
import os
from mockup import Buku, aman

SC = os.path.dirname(os.path.abspath(__file__))
SHOT = r"C:\Users\Admin1\AppData\Local\Temp\claude\C--opt-Claude-Workspace\874aeda3-c34a-4e04-b256-69f33abd7811\scratchpad\uat-shot"

b = Buku()

# ============================================================ SAMPUL
b.sampul(
    'Manual Posting & Laporan\nKeuangan',
    'POS Desktop eBisnis -- Jurnal Umum, Posting Pembelian & Penjualan, Katalog Laporan Keuangan',
    [
        'Versi aplikasi diverifikasi: 1.34.20 (build 182), varian eBisnis polos, target ' +
        '-t lib/main.dart, dart-define kosong -- diperiksa langsung dari generated_config.cmake.',
        'Disusun 2026-09-03. Lingkup: grup menu AKUNTANSI dan KEUANGAN, serta kategori ' +
        '"Laporan Keuangan Resmi", "Buku Besar Resmi", dan "Arus Kas & Analisa Keuangan" ' +
        'pada Katalog Laporan.',
    ])
b.kotak(
    'Cara membaca manual ini',
    'Dua halaman (bagian 1) memakai TANGKAPAN LAYAR NYATA dari aplikasi yang berjalan dan '
    'masuk ke server produksi ebisnis.id. Halaman lain memakai ILUSTRASI TATA LETAK yang '
    'ditandai jelas di sudut kanan-bawah tiap maket -- bukan tangkapan layar, tetapi setiap '
    'nama kolom, label medan, label tombol, dan pesan yang tertulis di dalamnya diambil apa '
    'adanya dari kode sumber aplikasi (dikutip dengan sitasi berkas:baris di teks pendamping), '
    'bukan dikarang. Alasan memakai ilustrasi bukan tangkapan layar untuk sebagian besar '
    'layar: sesi penyusunan manual ini tidak punya kanal interaksi ke jendela aplikasi '
    '(diverifikasi dengan tiga teknik berbeda, dicatat di docs/pos/110). Rinciannya di Lampiran C.',
    warna=(0.145, 0.388, 0.922))

# ============================================================ 1. PENDAHULUAN
b.judul_bagian('1', 'Pendahuluan')
b.teks(
    'Manual ini mencakup alur pembukuan akuntansi di POS Desktop eBisnis: mencatat jurnal '
    'umum, memposting jurnal dari transaksi pembelian (Kulakan) dan penjualan (HPP & '
    'Penjualan), sampai membaca laporan keuangan resminya. POS Android memakai layar dan '
    'aksi server yang SAMA (kode Dart dibagi lintas platform); perbedaannya hanya tata letak '
    'layar sentuh, bukan aksi atau aturan.')
b.sub_judul('1.1  Prinsip lokal-dulu ("local-first")')
b.teks(
    'Sebagian besar layar membaca dari CACHE LOKAL lebih dulu (tampil cepat, termasuk saat '
    'offline), lalu server mengoreksinya begitu terhubung. Menyimpan dan MEMPOSTING selalu '
    'butuh koneksi ke server -- draf boleh ditulis offline, tetapi buku besar hanya berubah '
    'saat aplikasi berhasil menghubungi server.')
b.sub_judul('1.2  Siapa yang boleh mengakses menu ini')
b.teks(
    'Menu AKUNTANSI dan KEUANGAN bersifat FAIL-CLOSED: bila server tidak mengirim izin '
    'untuk suatu menu, menu itu TIDAK tampil sama sekali -- bukan tampil lalu ditolak. '
    'Sumber: widgets/app_shell.dart, komentar di atas _kunciMenuAkuntansi -- '
    '"Dibaca FAIL-CLOSED (bolehMenuVarianBaru: kunci hilang = tidak boleh) supaya sama '
    'persis dengan gerbang drawer Android". Bila sebuah menu pada manual ini tidak muncul '
    'di aplikasi Anda, itu berarti peran Anda belum diberi izinnya -- hubungi admin, bukan '
    'tanda aplikasi rusak.')

# ============================================================ 2. KONSEP DASAR
b.judul_bagian('2', 'Konsep Dasar Sebelum Memposting')
b.sub_judul('2.1  Draf vs Terposting')
b.teks(
    'Setiap dokumen (jurnal umum, faktur kulakan, dsb.) punya dua keadaan: DRAF (tersimpan, '
    'belum masuk buku besar, masih bisa diubah/dihapus) dan TERPOSTING (sudah dijurnal '
    'permanen; hanya bisa dibatalkan lewat aksi "Batalkan posting", bukan diedit langsung). '
    'Laporan keuangan resmi HANYA membaca dokumen berstatus TERPOSTING -- draf tidak pernah '
    'muncul di Neraca, Laba Rugi, atau Buku Besar.')
b.sub_judul('2.2  Jurnal harus seimbang')
b.teks(
    'Sumber: jurnal_umum_screen.dart, _EditorJurnalState._alasanBelumBisaSimpan (baris '
    '538-664). Aturannya, apa adanya dari kode:')
b.poin([
    'Keterangan jurnal wajib diisi -- "supaya mudah ditelusuri di buku besar".',
    'Tiap baris wajib memilih akun dan berisi TEPAT SATU sisi (debet atau kredit, tidak dua-duanya).',
    'Minimal 2 baris sah: satu sisi debet dan satu sisi kredit.',
    'Total debet harus sama dengan total kredit; toleransi selisih < Rp 0,005 (pembulatan).',
])
b.kotak(
    'Pesan validasi APA ADANYA dari aplikasi',
    '"Baris ke-N: akun belum dipilih." -- "Baris ke-N: satu baris hanya boleh diisi debet '
    'ATAU kredit." -- "Baris ke-N: nilainya masih nol." -- "Jurnal minimal 2 baris: satu '
    'sisi debet dan satu sisi kredit." -- "Jurnal belum seimbang. Selisihnya Rp ..."\n\n'
    'Pemeriksaan ini berjalan DI APLIKASI sebelum data dikirim -- supaya pengguna tidak '
    'menunggu perjalanan ke server hanya untuk diberi tahu kesalahannya. Server tetap '
    'memeriksa ulang seluruh aturan ini secara independen.',
    warna=(0.42, 0.46, 0.53), bg=(0.97, 0.97, 0.98))
b.sub_judul('2.3  Tanggal Closing')
b.teks(
    'Tanggal Closing (tampil di layar Jurnal Umum) mengunci seluruh entri SEBELUM tanggal '
    'itu -- jurnal baru maupun koreksi tidak bisa menyentuh periode yang sudah ditutup. '
    'Lihat Bagian 10 untuk menu Tutup Buku.')

exec(open(os.path.join(SC, 'isi.py'), encoding='utf-8').read())

# ============================================================ LAMPIRAN D
b.judul_bagian('', 'Lampiran D -- Tangkapan Layar Nyata')
b.teks(
    'Dua halaman ini dijamin bukan ilustrasi: diambil langsung dari aplikasi eBisnis '
    'yang berjalan dan sudah masuk ke server produksi ebisnis.id (konteks "ebisnis", '
    'admin, toko "Kantin Demo"), memakai PrintWindow (PW_RENDERFULLCONTENT) atas '
    'jendela FLUTTERVIEW yang sebenarnya. Warna dan tata letak sidebar pada seluruh '
    'maket di manual ini disampel dari tangkapan kedua di bawah.')
b.gambar_asli(os.path.join(SHOT, '01-layar-awal.png'), 'Layar Masuk',
    'Kartu login "eBisnis -- Masuk ke sistem operasional eBisnis", versi 1.34.20 (build '
    '182), memakai kredensial tersimpan. Menegaskan build yang diuji benar-benar varian '
    'eBisnis (bukan varian lain).')
b.gambar_asli(os.path.join(SHOT, '19-fv-langsung.png'), 'Beranda Kasir/POS setelah masuk',
    'Sidebar KIRI menampilkan seluruh grup menu, termasuk KEUANGAN dan TRANSAKSI & LAPORAN '
    '(grup AKUNTANSI terlihat setelah digulir -- lihat Bagian 1.2 soal fail-closed bila '
    'grup itu tidak tampil di akun Anda). Warna sidebar gelap (#0f1c2e) dan aksen biru '
    'aktif (#2563eb) yang dipakai seluruh maket di manual ini diambil langsung dari '
    'tangkapan ini.')

pdf = b.simpan()
print('MANUAL LENGKAP ->', pdf, '(%d halaman)' % b.nomor)
