# -*- coding: utf-8 -*-
"""Sembunyikan lencana nomor dari pembaca layar.

Nomor bagian dirender sebagai <span> lingkaran di sebelah judul. Secara visual
terpisah oleh flex gap, tetapi pohon aksesibilitas menyambungnya menjadi
"1Tujuan Halaman". Nomor itu murni dekoratif (daftar isi sudah menomori), jadi
diberi aria-hidden agar pembaca layar hanya menyuarakan judulnya.

Kelas lencana diturunkan dari isi gaya (md5) sehingga sama di seluruh korpus.
"""
import io, os, re, collections

# -- Lokasi berkas bantuan ---------------------------------------------------
# Diturunkan dari letak skrip ini (docs/performance/tools/) agar berjalan sama
# di Windows maupun di server Linux. Dapat ditimpa lewat variabel lingkungan
# AIS_WEBINF bila salinan kerja berada di tempat lain.
_AKAR = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)),
                                     '..', '..', '..'))
WEBINF = os.environ.get('AIS_WEBINF') or os.path.join(
    _AKAR, 'src', 'main', 'webapp', 'WEB-INF')


DIR = os.path.join(WEBINF, 'bantuan')
KELAS = ['kb684904e', 'kba442b03']   # lencana bagian (24px) dan lencana langkah (21px)

pola = [(re.compile(r'<span class="%s">(\d+)</span>' % k),
         r'<span class="%s" aria-hidden="true">\1</span>' % k) for k in KELAS]

def polos(h):
    h = re.sub(r'<style.*?</style>', ' ', h, flags=re.S)
    h = re.sub(r'<[^>]+>', ' ', h)
    return re.sub(r'\s+', ' ', h).strip()

ringkas = collections.Counter()
for f in sorted(os.listdir(DIR)):
    if not f.endswith('.html'):
        continue
    p = os.path.join(DIR, f)
    s = io.open(p, 'r', encoding='utf-8').read()
    if 'aria-hidden="true"' in s:
        ringkas['lewat-sudah'] += 1
        continue
    baru = s
    for rx, rep in pola:
        baru = rx.sub(rep, baru)
    if baru == s:
        ringkas['lewat-takadalencana'] += 1
        continue
    if polos(baru) != polos(s):
        ringkas['GAGAL-teksberubah'] += 1
        continue
    io.open(p, 'w', encoding='utf-8', newline='').write(baru)
    ringkas['ok'] += 1

for k in sorted(ringkas):
    print('  %-24s %d' % (k, ringkas[k]))
