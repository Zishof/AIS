# -*- coding: utf-8 -*-
"""Rombak berkas tanya jawab (<kunci>_qa.html) ke format baru.

Tiap berkas memuat sekitar 78 tanya jawab dalam 10 kelompok, semuanya berupa
paragraf datar sehingga pembaca harus menggulir jauh untuk menemukan satu
jawaban. Format baru:

  * Kotak pencarian yang menyaring pertanyaan seketika.
  * Chip pintasan ke tiap kelompok, lengkap dengan jumlah pertanyaan.
  * Tiap tanya jawab menjadi <details> tertutup, sehingga halaman berubah dari
    dinding teks menjadi daftar pertanyaan yang bisa dipindai.

Idempoten (penanda KB-QA-V2). Menolak menulis bila ada teks yang hilang atau
tag menjadi tidak seimbang.
"""
import io, os, re, sys, collections

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
TANDA = 'KB-QA-V2'

C_TEKS = '#334155'
C_BIRU = '#1d4ed8'
C_HIJAU = '#166534'

RE_H2 = re.compile(r'<h2[^>]*>(.*?)</h2>', re.S)
RE_BLOK = re.compile(r'<(h3|p)[^>]*>(.*?)</\1>', re.S)
RE_TANYA = re.compile(r'^\s*&#10067;\s*(.*)$', re.S)
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
        p.feed(html)
        p.close()
        return not p.rusak and not p.tumpuk
    except Exception:
        return False


def teks_polos(html):
    s = re.sub(r'<[^>]+>', ' ', html)
    s = re.sub(r'&[#a-zA-Z0-9]+;', ' ', s)
    return re.sub(r'\s+', ' ', s).strip()


def par(isi, margin):
    return '<p style="color:%s;line-height:1.7;margin:0 0 %dpx;">%s</p>' % (C_TEKS, margin, isi.strip())


def bongkar(s):
    """Kembalikan (judul, intro_html, [[judul_grup, [(tanya, jawab)], [pengantar]]])."""
    m = RE_H2.search(s)
    if not m:
        return None
    judul = teks_polos(m.group(1))
    sisa = s[m.end():]

    grup = []
    intro = []
    kini = None
    tanya_kini = None
    jawab_kini = []

    for mm in RE_BLOK.finditer(sisa):
        tag, isi = mm.group(1), mm.group(2)
        if tag == 'h3':
            if tanya_kini is not None and kini is not None:
                kini[1].append((tanya_kini, ''.join(jawab_kini)))
            tanya_kini, jawab_kini = None, []
            kini = [teks_polos(isi), [], []]
            grup.append(kini)
            continue
        q = RE_TANYA.match(isi)
        if q:
            if tanya_kini is not None and kini is not None:
                kini[1].append((tanya_kini, ''.join(jawab_kini)))
            tanya_kini = q.group(1).strip()
            jawab_kini = []
        elif tanya_kini is not None:
            jawab_kini.append(par(isi, 9))
        elif kini is not None:
            # Paragraf pengantar kelompok, sebelum pertanyaan pertamanya.
            kini[2].append(par(isi, 10))
        else:
            intro.append(par(isi, 12))
    if tanya_kini is not None and kini is not None:
        kini[1].append((tanya_kini, ''.join(jawab_kini)))

    grup = [g for g in grup if g[1] or g[2]]
    if not grup:
        return None
    return judul, ''.join(intro), grup


SKRIP_SARING = (
    '<script type="text/javascript">\n'
    'function kbqSaring(q){\n'
    '  q=(q||"").toLowerCase().trim();\n'
    '  var item=document.querySelectorAll(".kbq-item"), n=0, i;\n'
    '  for(i=0;i<item.length;i++){\n'
    '    var cocok=!q||(item[i].textContent||"").toLowerCase().indexOf(q)>=0;\n'
    '    item[i].style.display=cocok?"block":"none";\n'
    '    if(cocok){n++; if(q){item[i].setAttribute("open","open");} '
    'else {item[i].removeAttribute("open");}}\n'
    '  }\n'
    '  var grup=document.querySelectorAll(".kbq-grup");\n'
    '  for(i=0;i<grup.length;i++){\n'
    '    var ada=false, s=grup[i].nextElementSibling;\n'
    '    while(s){\n'
    '      var c=s.className||"";\n'
    '      if(c.indexOf("kbq-grup")>=0){break;}\n'
    '      if(c.indexOf("kbq-item")>=0&&s.style.display!=="none"){ada=true;break;}\n'
    '      s=s.nextElementSibling;\n'
    '    }\n'
    '    grup[i].style.display=ada?"block":"none";\n'
    '  }\n'
    '  var lbl=document.getElementById("kbqJumlah");\n'
    '  if(lbl){lbl.textContent=n+" pertanyaan";}\n'
    '}\n'
    '</script>\n')


def rakit(judul, intro, grup):
    total = sum(len(g[1]) for g in grup)
    out = []
    out.append('<!-- ' + TANDA + ' -->\n')
    out.append('<h2 style="margin:0 0 10px;color:%s;font-size:19px;">%s</h2>\n' % (C_BIRU, judul))
    if intro:
        out.append(intro + '\n')

    out.append(
        '<div style="display:flex;gap:10px;align-items:center;margin:0 0 14px;">'
        '<input id="kbqCari" type="search" placeholder="Ketik kata kunci, misalnya: simpan, cetak, '
        'akses, gagal&hellip;" oninput="kbqSaring(this.value)" '
        'style="flex:1 1 auto;border:1px solid #cbd5e1;border-radius:9px;padding:9px 12px;'
        'font:inherit;box-sizing:border-box;">'
        '<span id="kbqJumlah" style="color:%s;font-weight:600;white-space:nowrap;font-size:12px;">'
        '%d pertanyaan</span></div>\n' % (C_HIJAU, total))

    chip = []
    for i in range(len(grup)):
        nama, pasangan = grup[i][0], grup[i][1]
        chip.append(
            '<a href="#kbq%d" onclick="var e=document.getElementById(\'kbq%d\');'
            'if(e&&e.scrollIntoView){e.scrollIntoView();return false;}" '
            'style="display:inline-block;background:#eef2f7;border:1px solid #dbe3ec;'
            'border-radius:20px;padding:4px 11px;margin:0 6px 6px 0;color:%s;'
            'text-decoration:none;font-size:12px;">%s <span style="color:#94a3b8;">(%d)</span></a>'
            % (i, i, C_BIRU, nama, len(pasangan)))
    out.append('<div style="margin:0 0 16px;">' + ''.join(chip) + '</div>\n')

    for i in range(len(grup)):
        nama, pasangan, pengantar = grup[i]
        out.append(
            '<h3 id="kbq%d" class="kbq-grup" style="margin:22px 0 9px;font-size:14px;color:#0f172a;'
            'border-left:3px solid %s;padding-left:9px;">%s</h3>\n' % (i, C_BIRU, nama))
        if pengantar:
            out.append(''.join(pengantar) + '\n')
        for tanya, jawab in pasangan:
            out.append(
                '<details class="kbq-item" style="border:1px solid #e2e8f0;border-radius:9px;'
                'background:#fff;margin:0 0 7px;padding:0 13px;">'
                '<summary style="cursor:pointer;color:%s;font-weight:600;padding:11px 0;">%s</summary>'
                '<div style="padding:0 0 10px;">%s</div></details>\n' % (C_BIRU, tanya, jawab))

    out.append(SKRIP_SARING)
    return ''.join(out)


def kata(html):
    return RE_KATA.findall(teks_polos(html).lower())


def verifikasi(lama, baru):
    a = collections.Counter(kata(lama))
    b = collections.Counter(kata(baru))
    hilang = a - b
    if hilang:
        return 'TEKS HILANG (%d kata): %s' % (sum(hilang.values()),
                                              ', '.join(list(hilang.keys())[:10]))
    return None


def proses(nama, tulis=True):
    p = os.path.join(DIR, nama)
    asli = io.open(p, 'r', encoding='utf-8').read()
    if TANDA in asli:
        return 'lewat'
    hasil = bongkar(asli)
    if hasil is None:
        return 'lewat-taktemplate'
    baru = rakit(hasil[0], hasil[1], hasil[2])
    galat = verifikasi(asli, baru)
    if galat:
        return galat
    # Skrip penyaring memuat < dan > sehingga tidak diikutkan pemeriksaan tag.
    tanpa_skrip = re.sub(r'<script.*?</script>', '', baru, flags=re.S)
    if not seimbang(tanpa_skrip):
        return 'lewat-tagtidakseimbang'
    if tulis:
        io.open(p, 'w', encoding='utf-8', newline='').write(baru)
    return 'ok'


if __name__ == '__main__':
    uji = '--uji' in sys.argv
    hanya = [a for a in sys.argv[1:] if not a.startswith('--')]
    berkas = hanya or sorted(f for f in os.listdir(DIR) if f.endswith('_qa.html'))
    ringkas = collections.Counter()
    galat = []
    for f in berkas:
        r = proses(f, tulis=not uji)
        k = r if (r == 'ok' or r.startswith('lewat')) else 'GALAT'
        ringkas[k] += 1
        if k == 'GALAT':
            galat.append('%s: %s' % (f, r))
    print('MODE:', 'UJI (tidak menulis)' if uji else 'TULIS')
    for k in sorted(ringkas):
        print('  %-24s %d' % (k, ringkas[k]))
    for g in galat[:20]:
        print('  !', g)
