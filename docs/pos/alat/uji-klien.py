# -*- coding: utf-8 -*-
"""Jalankan analisis statis dan uji sisi klien POS -- tanpa perlu Flutter di PATH.

Berkas ini ada karena satu kesalahan yang mahal (docs/pos/98). `command -v dart`
dan `command -v flutter` sama-sama kosong di mesin ini, dan dari situ
disimpulkan "tidak ada toolchain Dart/Flutter". Kesimpulan itu diulang tiga kali
di dokumen dan pesan commit, dan tiga perbaikan sisi klien dikirim tanpa pernah
dianalisis maupun diuji.

Flutter ADA, di C:\\opt\\flutter, hanya tidak di PATH.

Alat ini menghapus kesempatan mengulang kesalahan itu: letaknya diselesaikan
akar_repo.flutter_bin() (FLUTTER_ROOT, lalu beberapa letak lazim), bukan
diandalkan dari PATH.

Keluar dengan kode 1 bila analisis atau uji gagal.

Pakai:  python uji-klien.py            (analisis + seluruh uji)
        python uji-klien.py --cepat    (analisis saja)
"""
import os
import subprocess
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import akar_repo  # noqa: E402  -- perlu sys.path di atas

# Berkas sumber yang disunting seri dokumen ini; dianalisis lebih dulu supaya
# kegagalannya terbaca sebelum tenggelam di antara ribuan berkas lain.
SUMBER_DISUNTING = [
    os.path.join('lib', 'screens', 'produk_screen.dart'),
    os.path.join('lib', 'screens', 'keranjang_screen.dart'),
    os.path.join('lib', 'screens', 'kulakan_bulk_entry_screen.dart'),
]


def jalankan(perintah, cwd, env):
    print('')
    print('$ ' + ' '.join(perintah[-3:]))
    p = subprocess.Popen(perintah, cwd=cwd, env=env,
                         stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
    keluaran = p.communicate()[0].decode('utf-8', 'replace')
    ekor = keluaran.strip().split('\n')
    for baris in ekor[-18:]:
        print('   ' + baris)
    return p.returncode


def main():
    akar_repo.pastikan_lengkap(perlu_pos=True)
    fbin = akar_repo.flutter_bin()
    dbin = akar_repo.dart_bin()
    app = os.path.join(akar_repo.repo_pos(), 'apps', 'ebisnis')
    if not os.path.isdir(app):
        print('aplikasi ebisnis tidak ketemu: ' + app)
        return 1

    env = dict(os.environ)
    env['PATH'] = fbin + os.pathsep + dbin + os.pathsep + env.get('PATH', '')

    print('flutter bin : ' + fbin)
    print('aplikasi    : ' + app)

    dart = os.path.join(dbin, 'dart.exe')
    if not os.path.isfile(dart):
        dart = os.path.join(dbin, 'dart')
    gagal = jalankan([dart, 'analyze'] + SUMBER_DISUNTING, app, env)

    if '--cepat' not in sys.argv:
        flutter = os.path.join(fbin, 'flutter.bat')
        if not os.path.isfile(flutter):
            flutter = os.path.join(fbin, 'flutter')
        gagal += jalankan([flutter, 'test'], app, env)

    print('')
    if gagal:
        print('ADA YANG GAGAL')
        return 1
    print('ANALISIS DAN UJI LULUS')
    return 0


if __name__ == '__main__':
    sys.exit(main())
