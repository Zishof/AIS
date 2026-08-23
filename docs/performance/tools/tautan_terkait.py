# -*- coding: utf-8 -*-
"""Sisipkan blok "Panduan terkait" pada panduan halaman yang relevan.

Bersifat menambah di akhir berkas (append) dan idempoten: bila penanda
KB-PANDUAN-TERKAIT sudah ada, berkas dilewati.
"""
import io, os

# -- Lokasi berkas bantuan ---------------------------------------------------
# Diturunkan dari letak skrip ini (docs/performance/tools/) agar berjalan sama
# di Windows maupun di server Linux. Dapat ditimpa lewat variabel lingkungan
# AIS_WEBINF bila salinan kerja berada di tempat lain.
_AKAR = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)),
                                     '..', '..', '..'))
WEBINF = os.environ.get('AIS_WEBINF') or os.path.join(
    _AKAR, 'src', 'main', 'webapp', 'WEB-INF')


DIR = os.path.join(WEBINF, 'bantuan')
TANDA = 'KB-PANDUAN-TERKAIT'

JUDUL = {
    'panduan_petugas_perpustakaan': 'Panduan Petugas Perpustakaan',
    'panduan_karya_ilmiah_mahasiswa': 'Panduan Unggah Karya Ilmiah (Mahasiswa)',
    'panduan_admin_ppdb': 'Panduan Admin PPDB',
    'panduan_pendaftaran_ppdb': 'Panduan Pendaftaran PPDB (Calon Siswa)',
    'panduan_input_nilai': 'Panduan Input Nilai (Dosen)',
    'panduan_pengisian_krs': 'Panduan Pengisian KRS (Mahasiswa)',
    'panduan_persetujuan_krs': 'Panduan Persetujuan KRS (Dosen PA)',
}

PETA = {
    'panduan_petugas_perpustakaan': ['kunjungan_anggota', 'welpus', 'tamu', 'item',
                                     'karya_tulis_item', 'katalog_online', 'pustaka',
                                     'upload_item', 'online_book'],
    'panduan_karya_ilmiah_mahasiswa': ['skripsi'],
    'panduan_admin_ppdb': ['gelombang_pendaftaran', 'psb', 'waktu_pendaftaran',
                           'persyaratan_pendaftaran', 'prosedur_pendaftaran',
                           'alur_pendaftaran', 'konfigurasi_biodata_calon_siswa'],
    'panduan_pendaftaran_ppdb': ['ppdb', 'login_calon_siswa', 'biodata_siswa'],
    'panduan_input_nilai': ['nilai_mahasiswa', 'nilai_mahasiswa_sp', 'penilaian',
                            'penilaian_sp', 'format_nilai', 'pembobotan_nilai',
                            'detailperkuliahan'],
    'panduan_pengisian_krs': ['krs', 'krs_paket', 'krs_non_paket', 'krs_remedial', 'krs_sp'],
    'panduan_persetujuan_krs': ['krs_mahasiswa', 'mhs_belum_disetujui_krs',
                                'monitor_mahasiswa_belum_ambil_krs'],
}

def blok(kunci):
    return (
        '\n<!-- ' + TANDA + ' -->\n'
        '<div style="margin:22px 0 0;padding:12px 14px;background:#f8fafc;'
        'border:1px solid #e2e8f0;border-radius:8px;">\n'
        '<div style="font-weight:bold;color:#0f172a;margin-bottom:6px;">Panduan terkait</div>\n'
        '<div style="color:#334155;line-height:1.6;">\n'
        '<a href="bantuan?key=' + kunci + '" target="_blank" rel="noopener" '
        'style="color:#1d4ed8;text-decoration:none;font-weight:600;">'
        '&#128218; ' + JUDUL[kunci] + '</a><br>\n'
        '<a href="bantuan?key=panduan" target="_blank" rel="noopener" '
        'style="color:#1d4ed8;text-decoration:none;">Lihat seluruh Pusat Panduan</a>\n'
        '</div>\n</div>\n')

ditambah, dilewati, tidakada = 0, 0, []
for kunci in sorted(PETA.keys()):
    for halaman in PETA[kunci]:
        p = os.path.join(DIR, halaman + '.html')
        if not os.path.isfile(p):
            tidakada.append(halaman)
            continue
        isi = io.open(p, 'r', encoding='utf-8').read()
        if TANDA in isi:
            dilewati += 1
            continue
        io.open(p, 'w', encoding='utf-8', newline='').write(isi.rstrip() + '\n' + blok(kunci))
        ditambah += 1
        print('  + %-40s -> %s' % (halaman, kunci))

print('Ditambah: %d, dilewati (sudah ada): %d' % (ditambah, dilewati))
if tidakada:
    print('Berkas panduan halaman belum ada (dilewati): %s' % ', '.join(tidakada))
