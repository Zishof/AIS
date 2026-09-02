# -*- coding: utf-8 -*-
"""Bandingkan dua pohon sumber yang SEHARUSNYA cermin: java/ dan src/.

Keduanya memetakan URL repositori yang sama (^/src), jadi isinya wajib identik.
Kalau menyimpang, ada dua bahaya:

  * menyunting salinan yang basi -- pekerjaannya benar tetapi hilang begitu
    salinan satunya di-update; dan
  * membaca salinan yang basi lalu menyimpulkan sebuah kelas/metode "tidak ada",
    padahal ada di salinan satunya (jebakan yang sudah termakan, docs/pos/87).

Penyimpangan hampir selalu berarti salah satu sisi ketinggalan revisi, bukan
konflik: sesi lain meng-commit lewat satu sisi, sisi lainnya belum di-update.
Repositori ini bergerak cepat, jadi beberapa berkas berbeda pada saat mana pun.

Yang TIDAK boleh disamakan begitu saja: berkas dengan suntingan lokal
(`svn status` = M) di salah satu sisi. Itu pekerjaan sesi lain yang belum
di-commit, dan meng-update-nya berisiko menimpa.

Pakai:  python cermin-java-src.py [akar]
Keluar: 0 bila identik, 1 bila ada penyimpangan.
"""
import hashlib
import os
import subprocess
import sys

AKAR = sys.argv[1] if len(sys.argv) > 1 else r'C:\opt\AIS\ais\src\main'


def peta(akar):
    h = {}
    for dp, _, fs in os.walk(akar):
        for f in fs:
            if f.endswith('.java'):
                p = os.path.join(dp, f)
                rel = os.path.relpath(p, akar).replace(os.sep, '/')
                with open(p, 'rb') as fh:
                    h[rel] = hashlib.md5(fh.read()).hexdigest()
    return h


def tersunting(p):
    try:
        r = subprocess.run(['svn', 'status', p], capture_output=True, text=True, timeout=60)
        return r.stdout.strip()[:1] in ('M', 'A', 'D')
    except Exception:
        return False


if __name__ == '__main__':
    a = peta(os.path.join(AKAR, 'java'))
    b = peta(os.path.join(AKAR, 'src'))
    hanya_a = sorted(set(a) - set(b))
    hanya_b = sorted(set(b) - set(a))
    beda = sorted(k for k in set(a) & set(b) if a[k] != b[k])
    print('berkas java/ : %d' % len(a))
    print('berkas src/  : %d' % len(b))
    print('hanya di java/ : %d' % len(hanya_a))
    print('hanya di src/  : %d' % len(hanya_b))
    print('isinya berbeda : %d\n' % len(beda))
    for k in hanya_a:
        print('HANYA java/  %s' % k)
    for k in hanya_b:
        print('HANYA src/   %s' % k)
    for k in beda:
        ja = tersunting(os.path.join(AKAR, 'java', *k.split('/')))
        jb = tersunting(os.path.join(AKAR, 'src', *k.split('/')))
        if ja or jb:
            sisi = 'java/' if ja else 'src/'
            print('BEDA  %s\n        suntingan lokal di %s -- JANGAN di-update, itu kerja sesi lain' % (k, sisi))
        else:
            print('BEDA  %s\n        dua-duanya bersih -- salah satu ketinggalan revisi; svn update sisi yang lama' % k)
    sys.exit(1 if (hanya_a or hanya_b or beda) else 0)
