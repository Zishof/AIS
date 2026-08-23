# -*- coding: utf-8 -*-
"""Sapu SELURUH kunci bantuan lewat servlet yang benar-benar berjalan.

Memeriksa tiap halaman apa adanya seperti yang diterima peramban:
kode HTTP, penanda format baru, keseimbangan tag pada HTML yang sudah
dibungkus servlet, serta gejala isi tidak terbaca (halaman "Belum Tersedia").
"""
import io, os, re, sys, urllib.request, collections

# -- Lokasi berkas bantuan ---------------------------------------------------
# Diturunkan dari letak skrip ini (docs/performance/tools/) agar berjalan sama
# di Windows maupun di server Linux. Dapat ditimpa lewat variabel lingkungan
# AIS_WEBINF bila salinan kerja berada di tempat lain.
_AKAR = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)),
                                     '..', '..', '..'))
WEBINF = os.environ.get('AIS_WEBINF') or os.path.join(
    _AKAR, 'src', 'main', 'webapp', 'WEB-INF')

from concurrent.futures import ThreadPoolExecutor
from html.parser import HTMLParser

BASIS = os.environ.get('AIS_BANTUAN_URL') or 'http://localhost:8080/ais/bantuan?key='
DIR = os.path.join(WEBINF, 'bantuan')

VOID = set(['br', 'hr', 'img', 'input', 'meta', 'link', 'area',
            'base', 'col', 'embed', 'source', 'track', 'wbr'])


class Cek(HTMLParser):
    def __init__(self):
        HTMLParser.__init__(self)
        self.tumpuk = []
        self.rusak = []

    def handle_starttag(self, t, a):
        if t not in VOID:
            self.tumpuk.append(t)

    def handle_endtag(self, t):
        if t in VOID:
            return
        if not self.tumpuk or self.tumpuk[-1] != t:
            self.rusak.append(t)
            if t in self.tumpuk:
                while self.tumpuk and self.tumpuk[-1] != t:
                    self.tumpuk.pop()
                self.tumpuk.pop()
        else:
            self.tumpuk.pop()


def periksa(kunci):
    try:
        with urllib.request.urlopen(BASIS + kunci, timeout=30) as r:
            kode = r.getcode()
            html = r.read().decode('utf-8', 'replace')
    except Exception as e:
        return (kunci, 0, 0, 'GAGAL-AMBIL: %s' % e)

    masalah = []
    if kode != 200:
        masalah.append('HTTP %d' % kode)
    if 'Panduan Belum Tersedia' in html:
        masalah.append('isi tidak terbaca')
    if 'Permintaan Tidak Valid' in html:
        masalah.append('kunci ditolak')
    if "class='kb-wrap'" not in html and 'class="kb-wrap"' not in html:
        masalah.append('pembungkus servlet hilang')

    # Keseimbangan tag pada dokumen utuh (skrip/gaya dibuang lebih dulu).
    bersih = re.sub(r'<script.*?</script>', '', html, flags=re.S)
    bersih = re.sub(r'<style.*?</style>', '', bersih, flags=re.S)
    bersih = re.sub(r'<!doctype[^>]*>', '', bersih, flags=re.I)
    p = Cek()
    try:
        p.feed(bersih)
        p.close()
    except Exception as e:
        masalah.append('parser gagal: %s' % e)
    if p.rusak:
        masalah.append('tag tak berpasangan: %s' % ','.join(p.rusak[:3]))
    if p.tumpuk:
        masalah.append('tag belum ditutup: %s' % ','.join(p.tumpuk[:3]))

    return (kunci, kode, len(html), '; '.join(masalah))


if __name__ == '__main__':
    kunci = sorted(f[:-5] for f in os.listdir(DIR)
                   if f.endswith('.html') and not f.startswith('_'))
    print('kunci diuji: %d' % len(kunci))
    hasil = []
    with ThreadPoolExecutor(max_workers=16) as ex:
        for i, h in enumerate(ex.map(periksa, kunci)):
            hasil.append(h)
            if (i + 1) % 500 == 0:
                print('  ... %d selesai' % (i + 1))

    bermasalah = [h for h in hasil if h[3]]
    total_bita = sum(h[2] for h in hasil)
    print('')
    print('BERHASIL 200 tanpa masalah : %d' % (len(hasil) - len(bermasalah)))
    print('BERMASALAH                 : %d' % len(bermasalah))
    print('total terkirim             : %.1f MB' % (total_bita / 1048576.0))
    for k, kode, sz, m in bermasalah[:30]:
        print('  ! %-40s HTTP %s %7d  %s' % (k, kode, sz, m))
