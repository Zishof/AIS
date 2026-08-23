# -*- coding: utf-8 -*-
"""Angkat gaya inline yang berulang menjadi kelas CSS pada berkas bantuan.

Format baru mengulang atribut style yang sama puluhan kali per berkas (tiap
<details> tanya jawab, tiap butir, tiap chip), sehingga korpus membengkak dari
~62 MB menjadi ~95 MB. Gaya yang muncul tiga kali atau lebih dipindahkan ke satu
blok <style> di awal berkas.

Nama kelas diturunkan dari ISI gaya (md5), bukan dari urutan kemunculan. Ini
penting: di ZK dua jendela panduan bisa terbuka bersamaan sehingga kedua blok
<style> hidup berdampingan di DOM yang sama. Dengan nama berbasis isi, gaya yang
sama selalu memakai kelas yang sama dan gaya yang berbeda tidak pernah bertabrakan.

Catatan: <style> yang disisipkan lewat innerHTML tetap diterapkan peramban
(berbeda dengan <script>), dan pola ini sudah dipakai BantuanHelper untuk
katalog bantuan.
"""
import io, os, re, sys, hashlib, collections

# -- Lokasi berkas bantuan ---------------------------------------------------
# Diturunkan dari letak skrip ini (docs/performance/tools/) agar berjalan sama
# di Windows maupun di server Linux. Dapat ditimpa lewat variabel lingkungan
# AIS_WEBINF bila salinan kerja berada di tempat lain.
_AKAR = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)),
                                     '..', '..', '..'))
WEBINF = os.environ.get('AIS_WEBINF') or os.path.join(
    _AKAR, 'src', 'main', 'webapp', 'WEB-INF')

from html.parser import HTMLParser

DIR = os.path.join(WEBINF, 'bantuan')
MIN_ULANG = 3

RE_TAG = re.compile(r'<([a-zA-Z][a-zA-Z0-9]*)((?:\s[^<>]*)?)>')
RE_STYLE = re.compile(r'\sstyle="([^"]*)"')
RE_CLASS = re.compile(r'\sclass="([^"]*)"')
RE_KATA = re.compile(r'[0-9A-Za-z\u00C0-\u024F]+')

TAG_KOSONG = set(['br', 'hr', 'img', 'input', 'meta', 'link', 'area',
                  'base', 'col', 'embed', 'source', 'track', 'wbr'])


class _Penyeimbang(HTMLParser):
    def __init__(self):
        HTMLParser.__init__(self)
        self.tumpuk = []
        self.rusak = False

    def handle_starttag(self, t, a):
        if t not in TAG_KOSONG:
            self.tumpuk.append(t)

    def handle_endtag(self, t):
        if t in TAG_KOSONG:
            return
        if not self.tumpuk or self.tumpuk[-1] != t:
            self.rusak = True
            if t in self.tumpuk:
                while self.tumpuk and self.tumpuk[-1] != t:
                    self.tumpuk.pop()
                self.tumpuk.pop()
        else:
            self.tumpuk.pop()


def seimbang(html):
    try:
        p = _Penyeimbang()
        p.feed(re.sub(r'<style.*?</style>', '', html, flags=re.S))
        p.close()
        return not p.rusak and not p.tumpuk
    except Exception:
        return False


def teks_polos(html):
    s = re.sub(r'<style.*?</style>', ' ', html, flags=re.S)
    s = re.sub(r'<[^>]+>', ' ', s)
    s = re.sub(r'&[#a-zA-Z0-9]+;', ' ', s)
    return re.sub(r'\s+', ' ', s).strip()


def nama_kelas(gaya):
    return 'kb' + hashlib.md5(gaya.encode('utf-8')).hexdigest()[:7]


def ringkas(s):
    """Kembalikan HTML dengan gaya berulang dipindah ke blok <style>."""
    hitung = collections.Counter(RE_STYLE.findall(s))
    dipakai = set(g for g, n in hitung.items() if n >= MIN_ULANG and len(g) > 40)
    if not dipakai:
        return None

    terpakai = {}

    def ganti_tag(m):
        tag, atribut = m.group(1), m.group(2)
        ms = RE_STYLE.search(atribut)
        if not ms or ms.group(1) not in dipakai:
            return m.group(0)
        gaya = ms.group(1)
        kelas = nama_kelas(gaya)
        terpakai[kelas] = gaya
        atribut = atribut[:ms.start()] + atribut[ms.end():]
        mc = RE_CLASS.search(atribut)
        if mc:
            atribut = (atribut[:mc.start()]
                       + ' class="' + mc.group(1) + ' ' + kelas + '"'
                       + atribut[mc.end():])
        else:
            atribut = ' class="' + kelas + '"' + atribut
        return '<' + tag + atribut + '>'

    baru = RE_TAG.sub(ganti_tag, s)
    if not terpakai:
        return None

    aturan = ''.join('.%s{%s}' % (k, v) for k, v in sorted(terpakai.items()))
    return '<style>' + aturan + '</style>\n' + baru


def proses(nama, tulis=True):
    p = os.path.join(DIR, nama)
    asli = io.open(p, 'r', encoding='utf-8').read()
    if asli.lstrip().startswith('<style>'):
        return 'lewat'
    if 'KB-FORMAT-V2' not in asli and 'KB-QA-V2' not in asli:
        return 'lewat-bukanformatbaru'
    baru = ringkas(asli)
    if baru is None:
        return 'lewat-takadagayaberulang'
    if teks_polos(asli) != teks_polos(baru):
        return 'TEKS BERUBAH'
    if not seimbang(baru):
        return 'TAG TIDAK SEIMBANG'
    if len(baru) >= len(asli):
        return 'lewat-tidakmengecil'
    if tulis:
        io.open(p, 'w', encoding='utf-8', newline='').write(baru)
    return 'ok:%d:%d' % (len(asli), len(baru))


if __name__ == '__main__':
    uji = '--uji' in sys.argv
    hanya = [a for a in sys.argv[1:] if not a.startswith('--')]
    berkas = hanya or sorted(f for f in os.listdir(DIR) if f.endswith('.html'))
    ringkasan = collections.Counter()
    galat = []
    sebelum = sesudah = 0
    for f in berkas:
        r = proses(f, tulis=not uji)
        if r.startswith('ok:'):
            _, a, b = r.split(':')
            sebelum += int(a)
            sesudah += int(b)
            ringkasan['ok'] += 1
        elif r.startswith('lewat'):
            ringkasan[r] += 1
        else:
            ringkasan['GALAT'] += 1
            galat.append('%s: %s' % (f, r))
    print('MODE:', 'UJI (tidak menulis)' if uji else 'TULIS')
    for k in sorted(ringkasan):
        print('  %-28s %d' % (k, ringkasan[k]))
    if sebelum:
        print('  ukuran %.1f MB -> %.1f MB  (hemat %.1f MB, %.0f%%)'
              % (sebelum / 1048576.0, sesudah / 1048576.0,
                 (sebelum - sesudah) / 1048576.0, 100.0 * (sebelum - sesudah) / sebelum))
    for g in galat[:15]:
        print('  !', g)
