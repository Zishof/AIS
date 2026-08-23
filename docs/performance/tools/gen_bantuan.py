# -*- coding: utf-8 -*-
"""Membuat berkas panduan di WEB-INF/bantuan/ mengikuti gaya bawaan aplikasi.

Servlet Bantuan membaca WEB-INF/bantuan/<key>.html sebagai FRAGMEN HTML lalu
membungkusnya sendiri, sehingga berkas di sini tidak boleh memuat <html>/<body>.
Kunci wajib cocok pola [a-z0-9_-]+.
"""
import io, os, re

# -- Lokasi berkas bantuan ---------------------------------------------------
# Diturunkan dari letak skrip ini (docs/performance/tools/) agar berjalan sama
# di Windows maupun di server Linux. Dapat ditimpa lewat variabel lingkungan
# AIS_WEBINF bila salinan kerja berada di tempat lain.
_AKAR = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)),
                                     '..', '..', '..'))
WEBINF = os.environ.get('AIS_WEBINF') or os.path.join(
    _AKAR, 'src', 'main', 'webapp', 'WEB-INF')


DIR = os.path.join(WEBINF, 'bantuan')

H2 = '<h2 style="margin:0 0 10px;color:#1d4ed8;">%s</h2>\n'
P  = '<p style="color:#334155;line-height:1.6;margin:0 0 12px;">%s</p>\n'
H3 = '<h3 style="color:#0f172a;border-bottom:2px solid #e2e8f0;padding-bottom:4px;">%s</h3>\n'
UL_OPEN  = '<ul style="color:#334155;line-height:1.7;margin:0 0 12px;padding-left:20px;">\n'
OL_OPEN  = '<ol style="color:#334155;line-height:1.7;margin:0 0 12px;padding-left:20px;">\n'
LI = '<li>%s</li>\n'

def kotak(warna, latar, judul, isi):
    return ('<div style="border-left:4px solid %s;background:%s;padding:10px 14px;'
            'border-radius:0 6px 6px 0;margin:0 0 14px;">'
            '<div style="font-weight:bold;color:%s;margin-bottom:4px;">%s</div>'
            '<div style="color:#334155;line-height:1.6;">%s</div></div>\n'
            % (warna, latar, warna, judul, isi))

def awas(judul, isi):   return kotak('#b91c1c', '#fef2f2', judul, isi)
def penting(judul, isi):return kotak('#b45309', '#fffbeb', judul, isi)
def info(judul, isi):   return kotak('#1d4ed8', '#eff6ff', judul, isi)

def tabel(kepala, baris):
    s = ('<table style="border-collapse:collapse;width:100%;margin:0 0 14px;'
         'color:#334155;line-height:1.5;">\n<tr style="background:#f1f5f9;">')
    for k in kepala:
        s += ('<th style="border:1px solid #cbd5e1;padding:6px 9px;text-align:left;">%s</th>' % k)
    s += '</tr>\n'
    for b in baris:
        s += '<tr>'
        for sel in b:
            s += ('<td style="border:1px solid #cbd5e1;padding:6px 9px;vertical-align:top;">%s</td>' % sel)
        s += '</tr>\n'
    return s + '</table>\n'

def ul(items):
    return UL_OPEN + ''.join(LI % i for i in items) + '</ul>\n'

def ol(items):
    return OL_OPEN + ''.join(LI % i for i in items) + '</ol>\n'

def balik():
    return ('<p style="margin:18px 0 0;"><a href="bantuan?key=panduan" target="_blank" rel="noopener" '
            'style="color:#1d4ed8;text-decoration:none;">&larr; Buka Pusat Panduan</a></p>\n')

PANDUAN = []

# ─────────────────────────────── 1. PETUGAS PERPUSTAKAAN
k = 'panduan_petugas_perpustakaan'
j = 'Panduan Petugas Perpustakaan'
s  = H2 % j
s += P % ('Dua pekerjaan harian di meja sirkulasi: mencatat kunjungan dengan pemindai barcode, '
          'dan memasukkan karya ilmiah ke Repository Digital.')
s += H3 % '1. Mencatat kunjungan anggota dengan barcode'
s += ol([
    'Buka menu <b>Kunjungan Anggota</b>. Kursor otomatis berada di kolom kode.',
    '<b>Pindai kartu</b> pengunjung. Pemindai mengetikkan kode lalu menekan Enter sendiri.',
    'Nama muncul dan kunjungan tersimpan. Kolom kode otomatis kosong dan fokus kembali.',
    'Lanjutkan memindai kartu berikutnya tanpa menyentuh papan ketik atau tetikus.',
])
s += info('Kartu mahasiswa langsung bisa dipakai',
          'Barcode tidak harus berisi kode anggota perpustakaan. Sistem mengenali: kode anggota, '
          '<b>NIM mahasiswa</b>, kode/NIDN dosen, NIM/nomor induk/NIK siswa, kode/NIP/NIK guru, '
          'kode pegawai, dan User ID e-campus. Jadi KTM ber-barcode NIM sudah cukup.')
s += penting('Satu kunjungan per orang per hari',
             'Memindai kartu yang sama dua kali pada hari dan perpustakaan yang sama tidak membuat '
             'catatan ganda. Statistik kunjungan tetap bersih.')
s += H3 % '2. Pengunjung tamu (non-anggota)'
s += P % ('Gunakan menu <b>Kunjungan Tamu</b>. Isian yang tersedia: Nama, Alamat, Email, No. HP, '
          'dan Keperluan. Bila tamu ternyata sivitas kampus, datanya dapat ditautkan.')
s += P % ('Tersedia pula halaman anjungan mandiri agar pengunjung memindai kartunya sendiri: '
          '<b>/welpus</b> (kunjungan perpustakaan) dan <b>/tamu</b> (pengisian data tamu). '
          'Konfirmasikan ke pengelola sistem sebelum dipasang di anjungan.')
s += H3 % '3. Memasukkan karya ilmiah ke Repository'
s += awas('Tombol "Upload SQL Repository" bukan untuk dokumen',
          'Tombol itu hanya menerima berkas <b>.sql</b> untuk impor data oleh pengelola sistem. '
          'Mengunggah PDF atau Word akan selalu ditolak. Itu perilaku yang benar, bukan kerusakan.')
s += P % 'Alur yang benar: <b>input di modul sumber &rarr; Sinkron Lokal &rarr; cek tab Item Repository</b>.'
s += tabel(['Modul sumber', 'Untuk jenis karya'], [
    ['Skripsi / Tugas Akhir', 'Skripsi, tesis, disertasi'],
    ['Perpustakaan &mdash; Item', 'Koleksi pustaka umum'],
    ['Buku Bahan Ajar', 'Buku ajar dosen'],
    ['Artikel', 'Artikel penelitian dan pengabdian'],
    ['Penelitian &amp; Pengabdian', 'Laporan hasil penelitian/pengabdian'],
])
s += P % ('Judul, Abstrak, Kata Kunci, Pengarang, Penerbit, Bahasa, dan Tanggal terbit akan '
          'terbawa otomatis dari modul sumber.')
s += penting('Isi Abstrak pada kolomnya sendiri',
             'Bila kolom Abstrak dibiarkan kosong, sistem mengambil kolom Tujuan lalu Keterangan '
             'sebagai gantinya &mdash; sehingga catatan internal bisa tampil di repositori publik.')
s += H3 % '4. Menerbitkan koleksi ke katalog publik'
s += P % ('Katalog publik hanya menampilkan koleksi berstatus terbit. Gunakan tab '
          '<b>Penerbitan Katalog Publik</b> pada layar Pendataan Item (untuk buku) atau layar '
          'Karya Tulis (untuk karya ilmiah): saring status <b>Draft</b>, centang koleksi yang '
          'layak publik, lalu klik <b>Terbitkan</b>. Tombol <b>Tarik dari Publik</b> '
          'mengembalikannya bila perlu.')
s += balik()
PANDUAN.append((k, j, 'Kunjungan barcode, repository, dan penerbitan katalog', s))

# ─────────────────────────────── 2. KARYA ILMIAH MAHASISWA
k = 'panduan_karya_ilmiah_mahasiswa'
j = 'Panduan Unggah Karya Ilmiah (Mahasiswa)'
s  = H2 % j
s += P % ('Cara melengkapi data skripsi atau tugas akhir Anda agar masuk ke Repository Digital kampus.')
s += H3 % '1. Sebelum mulai'
s += ul([
    'Judul final dan <b>judul bahasa Inggris</b> bila prodi mensyaratkan.',
    '<b>Abstrak</b> dalam bentuk teks siap salin (bukan tangkapan layar).',
    'Kata kunci, dipisahkan tanda koma.',
    'Berkas PDF yang diminta prodi.',
])
s += penting('Data skripsi dibuat prodi, bukan mahasiswa',
             'Mahasiswa tidak dapat menambah data skripsi baru &mdash; tombol tambah memang tidak '
             'ditampilkan. Bila data skripsi Anda belum ada, hubungi program studi terlebih dahulu.')
s += H3 % '2. Melengkapi data'
s += ol([
    'Masuk ke e-campus, buka menu <b>Skripsi / Tugas Akhir</b>.',
    'Data Anda langsung tampil &mdash; sistem menyaring otomatis berdasarkan NIM Anda.',
    'Klik <b>Ubah</b> pada baris data Anda.',
    'Isi <b>Judul</b> dan <b>Judul (Inggris)</b> sesuai naskah final.',
    'Isi <b>Abstrak</b> pada kolom Abstrak.',
    'Isi <b>Kata Kunci</b>, pisahkan dengan koma.',
    'Unggah berkas pada slot lampiran yang disediakan prodi.',
    'Klik <b>Simpan</b>.',
])
s += awas('Abstrak wajib diisi di kolomnya sendiri',
          'Bila kosong, sistem repositori mengambil kolom lain sebagai gantinya &mdash; misalnya '
          'Tujuan atau Keterangan. Akibatnya yang tampil sebagai abstrak karya Anda di repositori '
          'publik bisa berupa catatan yang tidak semestinya dibaca umum.')
s += info('Judul dan abstrak Anda terbaca publik',
          'Setelah masuk Repository, Judul, Abstrak, Kata Kunci, dan nama Anda dapat dilihat '
          'pengunjung repositori. Tulis dengan rapi dan periksa ejaannya.')
s += H3 % '3. Bila muncul peringatan saat menyimpan'
s += P % ('Peringatan bertuliskan <i>"&hellip; wajib diupload !"</i> berarti ada slot berkas '
          'berstatus wajib yang masih kosong. Nama berkasnya disebutkan pada peringatan. Unggah '
          'berkas itu lalu simpan kembali &mdash; data tidak tersimpan selama berkas wajib belum lengkap.')
s += H3 % '4. Setelah menyimpan'
s += P % ('Karya Anda tidak langsung tampil di Repository. Petugas perpustakaan yang '
          'menyinkronkannya secara berkala. Judul, Abstrak, Kata Kunci, nama penulis, program '
          'studi, dan tanggal sidang terbawa otomatis.')
s += balik()
PANDUAN.append((k, j, 'Melengkapi judul, abstrak, dan berkas skripsi', s))

# ─────────────────────────────── 3. ADMIN PPDB
k = 'panduan_admin_ppdb'
j = 'Panduan Admin PPDB'
s  = H2 % j
s += P % ('Menyiapkan gelombang pendaftaran dan mengatur form tambahan agar benar-benar tampil '
          'di formulir calon siswa.')
s += H3 % '1. Isian pokok gelombang'
s += tabel(['Isian', 'Keterangan'], [
    ['<b>Nama Gelombang</b>', 'Nama yang dibaca calon siswa'],
    ['<b>Tanggal Mulai &amp; Sampai</b>', 'Di luar rentang ini pendaftaran tertutup'],
    ['<b>Tahun Ajaran</b> &amp; <b>Tahun Masuk</b>', 'Periode gelombang'],
    ['<b>Kuota Siswa Diterima</b>', 'Batas jumlah penerimaan'],
    ['<b>Aktif</b>', 'Gelombang tidak tampil ke publik bila tidak aktif'],
])
s += P % ('Tersedia tiga titik penagihan terpisah: <b>Jenis Biaya Sekolah</b> (pendaftaran), '
          '<b>Terverifikasi</b> (setelah berkas diverifikasi), dan <b>Lulus</b> (setelah diterima). '
          'Pembatasan peserta juga tersedia: hanya anak pegawai, harus anak alumni, harus alumni, '
          'atau harus saudara siswa.')
s += H3 % '2. Form tambahan: tiga lapis yang harus benar'
s += P % ('<b>Mencentang kotak di layar gelombang saja tidak cukup.</b> Form tambahan baru muncul '
          'bila ketiga lapis berikut benar semua:')
s += ol([
    '<b>Parameter tambahan sudah dibuat</b> pada master Parameter Tambahan beserta kelompoknya.',
    '<b>Parameter ditautkan ke gelombang</b>, dengan saklar <b>tampil di form sebelum login</b> '
    'aktif &mdash; inilah yang berlaku untuk formulir pendaftaran publik.',
    '<b>Centang di layar gelombang aktif</b> ("Tampil Form Tambahan Saat Registrasi" '
    'dan/atau "Saat Login Calon Siswa").',
])
s += penting('Telusuri dari lapis 1 ke atas',
             'Kesalahan paling umum adalah centang sudah benar (lapis 3) tetapi parameternya belum '
             'ditautkan ke gelombang tersebut (lapis 2), sehingga tidak ada isian yang bisa ditampilkan.')
s += awas('Perbaikan sistem 19 Agustus 2026',
          'Sebelum tanggal ini, formulir pendaftaran publik hanya membaca centang "Saat Login Calon '
          'Siswa"; centang "Saat Registrasi" tidak pernah dibaca. Kini salah satu dari kedua centang '
          'sudah cukup untuk pengunjung yang belum login. Bila sekolah Anda masih mengalami gejala '
          'ini, server kemungkinan belum diperbarui &mdash; hubungi pengelola sistem.')
s += info('Yang bukan form tambahan',
          'Isian <i>Foto Siswa</i>, <i>Merupakan Siswa Pindahan</i>, <i>"Anda mendapatkan informasi '
          'PPDB dari mana?"</i>, dan pernyataan kebenaran data adalah bawaan sistem &mdash; selalu '
          'tampil dan tidak diatur lewat Parameter Tambahan.')
s += H3 % '3. Pengaturan tampilan untuk calon siswa'
s += tabel(['Centang', 'Pengaruhnya'], [
    ['Tampil Cetak No. Reg / Biodata / Kartu Ujian', 'Calon siswa dapat mencetak sendiri'],
    ['<b>Cetak Kartu Ujian Harus Verifikasi Berkas</b>', 'Kartu ujian baru bisa dicetak setelah berkas diverifikasi'],
    ['Tampil Keterangan Diterima', 'Menampilkan status diterima'],
    ['Tampil Form Lampiran / Tambahan Di Halaman Utama', 'Muncul di halaman utama calon siswa'],
    ['<b>NIS otomatis saat bayar daftar ulang</b>', 'Pastikan penomoran NIS sudah disiapkan sebelum diaktifkan'],
])
s += P % ('Kolom <b>Informasi yang ditampilkan ke calon siswa</b> diisi petunjuk pendaftaran. '
          'Bila berkas panduan lebih dari satu, gabungkan dahulu menjadi satu arsip ZIP.')
s += info('Uji lewat jendela penyamaran',
          'Setiap kali mengubah pengaturan, buka halaman pendaftaran publik lewat jendela incognito '
          'agar terlihat persis seperti yang dilihat calon siswa yang belum login.')
s += balik()
PANDUAN.append((k, j, 'Gelombang pendaftaran dan form tambahan', s))

# ─────────────────────────────── 4. PENDAFTARAN PPDB (CALON SISWA)
k = 'panduan_pendaftaran_ppdb'
j = 'Panduan Pendaftaran PPDB (Calon Siswa)'
s  = H2 % j
s += P % 'Langkah mendaftar sebagai calon peserta didik baru secara daring.'
s += H3 % '1. Siapkan dahulu'
s += ul([
    '<b>Alamat email aktif</b> yang dapat Anda buka sendiri.',
    '<b>Foto formal</b>, ukuran <b>maksimal 1 MB</b>.',
    'Data diri: nama sesuai akta, NIK, tempat &amp; tanggal lahir, alamat.',
    'Data orang tua/wali: nama, NIK, pekerjaan, nomor telepon.',
    'Berkas pendukung yang diminta sekolah, hasil pindai atau foto yang jelas.',
])
s += awas('Satu email hanya untuk satu pendaftaran',
          'Calon peserta didik hanya diperkenankan mendaftar satu kali dengan satu alamat email '
          'aktif. Bila mendaftarkan lebih dari satu anak, siapkan email berbeda untuk masing-masing.')
s += penting('Bila foto lebih dari 1 MB',
             'Foto dari kamera ponsel umumnya melebihi batas ini. Perkecil dahulu memakai fitur '
             'ubah ukuran/kompres foto sebelum diunggah.')
s += H3 % '2. Langkah pendaftaran'
s += ol([
    'Buka halaman pendaftaran sekolah (alamatnya diakhiri <b>/ppdb</b>).',
    'Baca informasi dan alur pendaftaran; unduh lampiran panduan bila ada.',
    'Pilih gelombang yang sedang dibuka, lalu klik <b>Daftar Sekarang</b>.',
    'Isi formulir dengan lengkap. Isian bertanda bintang (*) wajib diisi.',
    'Unggah foto formal.',
    'Lengkapi isian tambahan bila sekolah menampilkannya.',
    '<b>Centang pernyataan kebenaran data</b> &mdash; tanpa ini pendaftaran tidak dapat dikirim.',
    'Periksa ulang, lalu klik <b>DAFTAR</b>.',
])
s += awas('Periksa sebelum menekan DAFTAR',
          'Dengan mencentang pernyataan, Anda menyatakan data yang dimasukkan benar dan bersedia '
          'menerima sanksi bila ditemukan kesalahan. Periksa ulang penulisan <b>nama, NIK, dan '
          'tanggal lahir</b> agar sesuai dokumen resmi.')
s += H3 % '3. Setelah berhasil mendaftar'
s += P % ('Alur umum: <b>Daftar &rarr; Bayar pendaftaran &rarr; Berkas diverifikasi &rarr; Ujian '
          'seleksi &rarr; Pengumuman &rarr; Daftar ulang</b>. Tahapan yang berlaku mengikuti '
          'pengumuman resmi sekolah.')
s += ol([
    '<b>Catat dan simpan nomor registrasi</b> Anda.',
    'Lakukan pembayaran sesuai petunjuk sekolah.',
    'Masuk kembali untuk memantau status, melengkapi berkas, atau mencetak dokumen.',
    'Cetak bukti registrasi, biodata, dan kartu ujian sesuai yang disediakan sekolah.',
])
s += penting('Kartu ujian belum bisa dicetak?',
             'Sebagian sekolah menetapkan kartu ujian baru dapat dicetak setelah berkas Anda '
             'selesai diverifikasi petugas. Pastikan seluruh berkas sudah diunggah, lalu tunggu '
             'proses verifikasi.')
s += H3 % '4. Kendala umum'
s += tabel(['Kendala', 'Yang perlu dilakukan'], [
    ['Gelombang tidak muncul', 'Belum dibuka atau sudah ditutup &mdash; periksa pengumuman sekolah'],
    ['Foto gagal diunggah', 'Ukuran melebihi 1 MB, perkecil dahulu'],
    ['Tombol DAFTAR tidak berfungsi', 'Masih ada isian wajib kosong, atau pernyataan belum dicentang'],
    ['Email sudah pernah dipakai', 'Gunakan email lain, atau masuk kembali bila itu pendaftaran Anda'],
    ['Ada data salah setelah terkirim', 'Hubungi panitia PPDB &mdash; <b>jangan</b> mendaftar ulang dengan email baru'],
])
s += balik()
PANDUAN.append((k, j, 'Langkah mendaftar untuk calon siswa dan orang tua', s))

# ─────────────────────────────── 5. INPUT NILAI (DOSEN)
k = 'panduan_input_nilai'
j = 'Panduan Input Nilai (Dosen)'
s  = H2 % j
s += P % ('Mengisi nilai perkuliahan &mdash; dari format nilai sampai mengunci, termasuk cara '
          'menelusuri nilai akhir yang tetap 0.')
s += P % ('Alur singkat: <b>Atur Format Nilai &rarr; Isi nilai komponen &rarr; Hitung Ulang &rarr; '
          'Verifikasi &rarr; Kunci</b>.')
s += H3 % '1. Menyiapkan format nilai'
s += P % ('Klik <b>Ubah Format</b>, pastikan seluruh komponen terdaftar, lalu <b>isi bobot persen '
          'tiap komponen</b> (umumnya total 100%). Selesaikan ini sebelum mulai mengisi nilai.')
s += awas('Bobot persen tidak boleh kosong',
          'Bila seluruh bobot bernilai 0 atau kosong, sistem tidak punya pembagi untuk menghitung '
          '&mdash; nilai akhir akan selalu 0 berapa kali pun Anda menekan Hitung Ulang.')
s += H3 % '2. Mengisi nilai'
s += ol([
    'Isi nilai tiap mahasiswa pada kolom komponen. Nilai tersimpan otomatis saat berpindah kolom.',
    'Gunakan kotak <b>Mhs</b> lalu <b>Cari</b> untuk melompat ke mahasiswa tertentu.',
    'Setelah selesai, klik <b>Hitung Ulang</b> agar nilai akhir dan nilai huruf diperbarui.',
    'Periksa kolom <b>Total</b> &mdash; pastikan angkanya wajar.',
])
s += P % ('Untuk kelas besar tersedia <b>Download</b> (unduh berkas kerja) dan <b>Upload</b> '
          '(unggah kembali setelah diisi), serta <b>Masukkan Nilai Absen</b> dan '
          '<b>Ambil Nilai dari Feeder</b>. Setelah mengunggah, tetap klik Hitung Ulang.')
s += H3 % '3. Nilai akhir tetap 0 &mdash; lima kemungkinan sebab'
s += P % ('Bila kolom Total menunjukkan 0 padahal komponen sudah terisi, sistem menampilkan '
          'peringatan merah pada baris mahasiswa tersebut. Berikut kelima sebab dan tindakannya:')
s += tabel(['Sebab', 'Tindakan'], [
    ['<b>Nilai terkunci, dan dikunci sebelum diisi.</b> Sistem memakai salinan nilai saat dikunci &mdash; '
     'bila saat itu masih kosong, salinannya berisi 0.',
     'Klik <b>Buka</b> &rarr; <b>Hitung Ulang</b> &rarr; kunci kembali'],
    ['<b>Total bobot persen 0 atau kosong.</b> Tidak ada pembagi.',
     'Perbaiki lewat <b>Ubah Format</b>, lalu Hitung Ulang'],
    ['<b>Aturan "jika ada nilai 0, nilai akhir tidak dihitung" aktif.</b>',
     'Lengkapi komponen yang masih 0, atau tinjau aturan bersama admin akademik'],
    ['<b>Kehadiran di bawah batas minimal.</b>',
     'Periksa data presensi &mdash; ini perilaku yang disengaja, bukan kesalahan'],
    ['<b>Nilai total tersimpan sudah usang.</b>', 'Klik <b>Hitung Ulang</b>'],
])
s += penting('Urutan pemeriksaan yang disarankan',
             'Periksa berurutan: (1) apakah nilai sedang terkunci &mdash; ini penyebab tersering; '
             '(2) apakah bobot persen sudah terisi; (3) tekan Hitung Ulang. Bila ketiganya benar '
             'dan tetap 0, periksa presensi dan aturan nilai 0.')
s += H3 % '4. Verifikasi dan mengunci'
s += P % ('<b>Verifikasi</b> menandai nilai sudah Anda sahkan. Bila kampus mengaktifkan '
          '"Sembunyikan nilai ke mahasiswa jika belum diverifikasi", mahasiswa belum dapat melihat '
          'nilainya sampai Anda memverifikasi. <b>Kunci</b> membuat seluruh kolom nilai hanya-baca; '
          '<b>Buka</b> mengembalikannya bila masih perlu perbaikan.')
s += awas('Kunci hanya setelah semua nilai benar',
          'Mengunci ketika masih ada komponen kosong menimbulkan persoalan pada Sebab 1 di atas '
          '&mdash; nilai akhir membeku di 0 walau kemudian Anda mengisi angkanya.')
s += P % ('Biasakan menutup pekerjaan dengan urutan: <b>Hitung Ulang &rarr; periksa Total &rarr; '
          'Verifikasi &rarr; Kunci</b>.')
s += balik()
PANDUAN.append((k, j, 'Mengisi nilai dan menelusuri nilai akhir 0', s))

# ─────────────────────────────── 6. PENGISIAN KRS (MAHASISWA)
k = 'panduan_pengisian_krs'
j = 'Panduan Pengisian KRS (Mahasiswa)'
s  = H2 % j
s += P % ('Mengisi Kartu Rencana Studi &mdash; dari syarat sebelum mengisi sampai KRS disetujui '
          'dosen pembimbing akademik.')
s += awas('KRS belum berlaku sebelum disetujui',
          'Mengisi dan menyimpan saja tidak cukup. Pastikan status persetujuan dosen PA sudah '
          'muncul sebelum masa pengisian ditutup.')
s += H3 % '1. Sebelum mengisi'
s += ul([
    '<b>Masa pengisian KRS sedang dibuka.</b> Di luar jadwal, sistem menolak pengisian.',
    '<b>Anda sudah memiliki dosen pembimbing akademik.</b>',
    '<b>Kewajiban pembayaran</b> sudah dipenuhi sesuai ketentuan kampus.',
    '<b>Nilai semester sebelumnya sudah keluar</b>, karena jumlah SKS yang boleh diambil '
    'umumnya mengikuti IP semester lalu.',
])
s += H3 % '2. Mengisi KRS'
s += ol([
    'Masuk ke e-campus, buka menu <b>KRS</b>.',
    'Perhatikan tahun ajaran dan semester yang aktif di layar.',
    'Pilih mata kuliah beserta kelasnya &mdash; perhatikan hari, jam, dan ruang agar tidak bentrok.',
    'Pantau jumlah SKS yang sudah terambil; sistem membatasi sesuai ketentuan kampus.',
    'Periksa kembali, lalu <b>simpan</b>.',
    'Ajukan ke dosen PA sesuai ketentuan kampus, lalu pantau status persetujuannya.',
])
s += tabel(['Jenis KRS', 'Cara pengisian'], [
    ['<b>KRS Paket</b>', 'Mata kuliah sudah ditetapkan prodi dalam satu paket; Anda tinggal mengambilnya'],
    ['<b>KRS Non Paket</b>', 'Anda memilih sendiri mata kuliah dan kelasnya, dalam batas SKS yang diperbolehkan'],
])
s += P % ('Bila kampus menyelenggarakannya, tersedia tab terpisah untuk <b>KRS Semester Pendek</b> '
          'dan <b>Mata Kuliah Remedial</b>, dengan jadwal dan ketentuan biaya tersendiri.')
s += H3 % '3. Status persetujuan'
s += tabel(['Status', 'Artinya'], [
    ['<b>Belum disetujui</b>', 'Tersimpan, tetapi dosen PA belum menyetujui &mdash; KRS belum berlaku'],
    ['<b>Disetujui</b>', 'KRS Anda sah untuk semester tersebut'],
    ['<b>Dibatalkan</b>', 'Persetujuan ditarik &mdash; baca komentar dosen PA Anda'],
])
s += info('Periksa komentar dosen PA',
          'Dosen pembimbing dapat meninggalkan komentar pada KRS Anda. Bila KRS lama tidak '
          'disetujui, komentar adalah tempat pertama yang perlu diperiksa.')
s += awas('Jangan menunggu hari terakhir',
          'Persetujuan memerlukan waktu dosen PA, dan bila ada yang perlu diperbaiki Anda masih '
          'butuh waktu mengubahnya. Setelah masa pengisian ditutup, sistem tidak lagi mengizinkan '
          'perubahan &mdash; termasuk perbaikan yang diminta dosen PA sendiri.')
s += H3 % '4. Kendala umum'
s += tabel(['Pesan atau kendala', 'Yang perlu dilakukan'], [
    ['<i>"Waktu pengambilan KRS sudah selesai atau belum berlangsung"</i>',
     'Periksa kalender akademik; bila menurut Anda masih dalam masa pengisian, hubungi bagian akademik'],
    ['<i>"Belum memiliki dosen pembimbing akademik"</i>',
     'Hubungi program studi &mdash; ini tidak dapat Anda atur sendiri'],
    ['Mata kuliah tidak muncul', 'Kelas belum dibuka, tidak ditawarkan untuk angkatan Anda, atau prasyarat belum terpenuhi'],
    ['Tidak bisa menambah mata kuliah', 'Batas SKS tercapai atau kuota kelas penuh'],
    ['Jadwal bentrok', 'Ganti kelas pada salah satu mata kuliah yang berbenturan'],
])
s += balik()
PANDUAN.append((k, j, 'Mengisi KRS sampai disetujui dosen PA', s))

# ─────────────────────────────── 7. PERSETUJUAN KRS (DOSEN PA)
k = 'panduan_persetujuan_krs'
j = 'Panduan Persetujuan KRS (Dosen PA)'
s  = H2 % j
s += P % ('Memeriksa dan menyetujui KRS mahasiswa bimbingan pada menu <b>Manajemen KRS</b>.')
s += awas('Saring semester sebelum menyetujui',
          'Bila semester tidak ditentukan, tombol Setujui KRS akan menyetujui <b>seluruh mata '
          'kuliah di SEMUA semester</b> mahasiswa tersebut. Kotak konfirmasi selalu menyebutkan '
          'cakupannya &mdash; bila tertulis <i>"(semua semester)"</i>, batalkan dan perbaiki '
          'penyaringan Anda lebih dahulu.')
s += H3 % '1. Membaca status KRS'
s += P % ('Lencana <b>"2 disetujui"</b> / <b>"0 belum"</b> dihitung <b>per semester</b>, bukan per '
          'mata kuliah. Satu semester dihitung disetujui hanya bila seluruh mata kuliahnya sudah '
          'disetujui; bila ada satu saja yang belum, semester itu masuk hitungan "belum". Semester '
          'Pendek dihitung terpisah. Lencana dapat diklik untuk membuka rincian KRS.')
s += P % ('Kolom <b>Keterangan</b> meringkas kondisi tiap mahasiswa, misalnya '
          '<i>"semester 2 &mdash; Belum pernah mengambil KRS"</i> atau <i>"5 perkuliahan yang '
          'semuanya disetujui, 3 perkuliahan dinilai, 2 perkuliahan belum dinilai"</i>.')
s += H3 % '2. Memeriksa sebelum menyetujui'
s += tabel(['Tombol', 'Kegunaan'], [
    ['<b>Daftar KRS</b>', 'Rincian mata kuliah &mdash; periksa jumlah SKS, kesesuaian kurikulum, benturan jadwal'],
    ['<b>Dasbor</b>', 'Rangkuman akademik dan riwayat nilai mahasiswa'],
    ['<b>Komentar</b>', 'Catatan untuk mahasiswa bila ada yang perlu diperbaiki'],
])
s += info('Beri komentar sebelum menolak menyetujui',
          'Mahasiswa tidak mendapat penjelasan apa pun bila Anda sekadar membiarkan KRS-nya tidak '
          'disetujui &mdash; sementara waktu pengisian KRS terbatas.')
s += H3 % '3. Menyetujui dan membatalkan'
s += ol([
    'Pastikan penyaringan semester sudah benar.',
    'Periksa KRS lewat <b>Daftar KRS</b>.',
    'Klik <b>Setujui KRS</b> pada baris mahasiswa.',
    '<b>Baca kotak konfirmasi</b> &mdash; berisi NIM, nama, dan cakupan semester.',
    'Klik OK. Sistem melaporkan jumlah mata kuliah yang diproses.',
])
s += penting('Pengaman saat membatalkan',
             'Tombol <b>Batalkan</b> tidak akan menyentuh mata kuliah yang sudah memiliki nilai. '
             'Mata kuliah tersebut dilewati dan jumlahnya dilaporkan pada pesan hasil, sehingga '
             'pembatalan tidak merusak nilai yang sudah berjalan.')
s += awas('Setujui berarti menyetujui seluruhnya',
          'Tombol Setujui KRS bekerja untuk seluruh mata kuliah pada cakupan yang tertera di '
          'konfirmasi &mdash; bukan per mata kuliah. Bila hanya sebagian yang layak disetujui, '
          'minta mahasiswa memperbaiki KRS-nya lebih dahulu lewat Komentar.')
s += H3 % '4. Persetujuan dalam jumlah banyak'
s += P % ('Tersedia <b>Download Persetujuan KRS</b> dan <b>Upload Persetujuan KRS</b> pada toolbar '
          'atas, serta <b>Lihat data KRS Double</b> untuk menemukan mata kuliah yang terambil ganda. '
          'Persetujuan massal memproses banyak mahasiswa sekaligus tanpa konfirmasi per orang '
          '&mdash; periksa isi berkas dengan teliti sebelum mengunggah.')
s += H3 % '5. Kendala umum'
s += tabel(['Kendala', 'Yang perlu dilakukan'], [
    ['Mahasiswa bimbingan tidak muncul', 'Longgarkan penyaring; bila tetap tidak ada, penetapan dosen PA mungkin belum tercatat'],
    ['<i>"Tidak ada mata kuliah yang perlu disetujui"</i>', 'Semua sudah disetujui, atau mahasiswa belum mengisi KRS'],
    ['Sebagian dilewati saat pembatalan', 'Wajar &mdash; mata kuliah yang sudah bernilai tidak dibatalkan'],
    ['Telanjur menyetujui semester yang salah', 'Gunakan <b>Batalkan</b> dengan penyaring yang tepat, lalu setujui ulang'],
    ['Mahasiswa mengaku belum disetujui', 'Satu mata kuliah yang belum disetujui membuat seluruh semester terhitung "belum"'],
])
s += balik()
PANDUAN.append((k, j, 'Memeriksa dan menyetujui KRS mahasiswa bimbingan', s))

# ─────────────────────────────── INDEKS
# CATATAN PENTING. Berkas indeks panduan.html TIDAK lagi ditulis di sini.
# Indeks sekarang dibangun gen_pusat_panduan.py yang menaut SELURUH panduan
# modul, bukan hanya tujuh panduan peran. Bila blok penyusun indeks di bawah
# ini diaktifkan kembali, indeks lengkap itu akan tertimpa versi ringkasnya.
# Blok dipertahankan sebagai riwayat, tetapi hasilnya tidak dipakai.
idx  = H2 % 'Pusat Panduan Pengguna'
idx += P % ('Kumpulan panduan singkat untuk pengguna e-campus. Pilih panduan yang sesuai dengan '
            'peran Anda. Halaman ini dapat dibuka dari mana saja melalui alamat '
            '<b>bantuan?key=panduan</b>.')
baris = []
for key, judul, ringkas, _ in PANDUAN:
    baris.append([
        '<a href="bantuan?key=%s" target="_blank" rel="noopener" '
        'style="color:#1d4ed8;text-decoration:none;font-weight:bold;">%s</a>'
        % (key, judul),
        ringkas,
    ])
idx += tabel(['Panduan', 'Isi ringkas'], baris)
idx += info('Panduan tidak menggantikan ketentuan resmi',
            'Nama menu dan sebagian aturan diatur masing-masing kampus/sekolah, sehingga tampilan '
            'Anda dapat berbeda. Untuk ketentuan resmi, ikuti pengumuman institusi Anda.')

# ─────────────────────────────── TULIS
if not os.path.isdir(DIR):
    raise SystemExit('Direktori bantuan tidak ditemukan: %s' % DIR)

pola = re.compile(r'^[a-z0-9_\-]+$')
ditulis = 0
for key, judul, ringkas, isi in PANDUAN:
    assert pola.match(key), 'kunci tidak valid: %s' % key
    io.open(os.path.join(DIR, key + '.html'), 'w', encoding='utf-8').write(isi)
    ditulis += 1
    print('  bantuan?key=%-34s %s' % (key, judul))
print('Total berkas panduan ditulis: %d' % ditulis)
