# -*- coding: utf-8 -*-
"""Periksa tiap butir menu benar-benar menunjuk sesuatu yang ada.

Menu disimpan di basis data, tetapi salinan luringnya ada di
ais/common/MenuSnapshotData.java dengan format:

    id|parent|urut|label|url|icon|grup

Kolom url memuat dua jenis tujuan:
  * jalur halaman, mis. "/pages/master/pegawai.zul"
      -> webapp/WEB-INF/z/x/y/pages/master/pegawai.zul
  * nama kelas laporan, mis. "ais.action.report.format1.sekolah.LaporanRaporSiswa"
      -> berkas .java-nya di pohon sumber

Keduanya baru dicari ketika butir menunya DIKLIK. Butir yang tujuannya sudah
hilang karena itu tidak pernah terdeteksi sampai ada pengguna mengkliknya, dan
yang ia dapat adalah halaman galat -- bukan pesan "menu tidak tersedia".

Diperiksa terhadap BERKAS di pohon kerja, bukan pohon kelas hasil kompilasi
(alasannya di docs/pos/87: pohon kelas basi dalam hitungan menit).

Pakai:  python menu-tujuan-hilang.py
Keluar: 0 bila semua tujuan ada, 1 bila ada yang hilang.
"""
import os
import re
import sys

AKAR = r'C:\opt\AIS\ais\src\main'
SNAPSHOT = os.path.join(AKAR, 'java', 'ais', 'common', 'MenuSnapshotData.java')
HALAMAN = os.path.join(AKAR, 'webapp', 'WEB-INF', 'z', 'x', 'y')
SUMBER = os.path.join(AKAR, 'java')

BARIS = re.compile(r'"(\d+)\|(\d*)\|(\d*)\|([^|"]*)\|([^|"]*)\|([^|"]*)\|([^|"]*)"')


def tujuan_ada(url):
    """None bila tujuannya ada; selain itu keterangan jenis tujuannya."""
    u = url.strip()
    if not u:
        return None                      # butir induk, tidak menunjuk apa pun
    if u.startswith('/'):
        jalur = u.split('?', 1)[0].split('#', 1)[0]
        p = os.path.join(HALAMAN, *jalur.lstrip('/').split('/'))
        return None if os.path.isfile(p) else 'halaman'
    if u.startswith('ais.'):
        p = os.path.join(SUMBER, *u.split('$')[0].split('.')) + '.java'
        return None if os.path.isfile(p) else 'kelas'
    return None                          # bentuk lain: tidak dinilai di sini


def main():
    teks = open(SNAPSHOT, encoding='utf-8', errors='replace').read()
    butir = BARIS.findall(teks)
    hilang = []
    berurl = 0
    for mid, _, _, label, url, _, _ in butir:
        if url.strip():
            berurl += 1
        jenis = tujuan_ada(url)
        if jenis:
            hilang.append((mid, label.strip(), url.strip(), jenis))
    print('butir menu       : %d' % len(butir))
    print('punya tujuan     : %d' % berurl)
    print('tujuan hilang    : %d\n' % len(hilang))
    for mid, label, url, jenis in sorted(hilang, key=lambda x: x[3] + x[2]):
        print('[%s] %-8s %-44s %s' % (jenis, mid, url, label))
    return 1 if hilang else 0


if __name__ == '__main__':
    sys.exit(main())
