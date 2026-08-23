# -*- coding: utf-8 -*-
"""Rombak tampilan seluruh panduan di WEB-INF/bantuan ke format baru.

Sasaran: yang paling dibutuhkan pembaca berada di ATAS, bukan terkubur di
bawah 10 KB teks.

  * "Ringkasan Alur Singkat" (semula bagian ke-10) diangkat menjadi kartu
    "Ringkas" di paling atas.
  * Daftar isi berupa chip agar 15 bagian bisa dilompati.
  * Bagian bernomor memakai lencana, bukan garis bawah.
  * Bagian bermakna khusus menjadi kartu berwarna:
      Perlu Disiapkan -> daftar centang, Kesalahan Umum -> kartu merah,
      Tips -> kartu biru, Langkah Akhir -> kartu hijau.
  * Tanya jawab menjadi akordeon <details> sehingga halaman jauh lebih pendek.
  * Paragraf penutup yang mengulang ringkasan dilipat ke dalam <details>.
  * Penyederhanaan bahasa baku-kaku menjadi bahasa sehari-hari.

Idempoten: berkas yang sudah memuat penanda KB-FORMAT-V2 dilewati.
Seluruh teks asli dipertahankan; diverifikasi ulang sebelum ditulis.
"""
import io, os, re, sys

# -- Lokasi berkas bantuan ---------------------------------------------------
# Diturunkan dari letak skrip ini (docs/performance/tools/) agar berjalan sama
# di Windows maupun di server Linux. Dapat ditimpa lewat variabel lingkungan
# AIS_WEBINF bila salinan kerja berada di tempat lain.
_AKAR = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)),
                                     '..', '..', '..'))
WEBINF = os.environ.get('AIS_WEBINF') or os.path.join(
    _AKAR, 'src', 'main', 'webapp', 'WEB-INF')


DIR = os.path.join(WEBINF, 'bantuan')
TANDA = 'KB-FORMAT-V2'
TANDA_TERKAIT = 'KB-PANDUAN-TERKAIT'

# ─────────────────────────────────────────── penyederhanaan bahasa
# Hanya penggantian yang aman dan tidak mengubah makna. Pola memakai batas kata.
KAMUS = [
    (r'\bApabila\b', 'Jika'),
    (r'\bapabila\b', 'jika'),
    (r'\bdikarenakan\b', 'karena'),
    (r'\bsenantiasa\b', 'selalu'),
    (r'\bdipergunakan\b', 'digunakan'),
    (r'\bmempergunakan\b', 'menggunakan'),
    (r'\bmelakukan pengisian\b', 'mengisi'),
    (r'\bmelakukan pemeriksaan\b', 'memeriksa'),
    (r'\bmelakukan perubahan\b', 'mengubah'),
    (r'\bmelakukan penyimpanan\b', 'menyimpan'),
    (r'\bmelakukan penghapusan\b', 'menghapus'),
    (r'\bmelakukan penambahan\b', 'menambah'),
    (r'\bmelakukan pencarian\b', 'mencari'),
    (r'\bmelakukan penyuntingan\b', 'menyunting'),
    (r'\bpada saat\b', 'saat'),
    (r'\bagar supaya\b', 'agar'),
    (r'\bsebagaimana mestinya\b', 'sebagaimana seharusnya'),
    (r'\bdi kemudian hari\b', 'nanti'),
    (r'\bsangat dianjurkan\b', 'sebaiknya'),
    (r'\bdianjurkan untuk\b', 'sebaiknya'),
]
KAMUS = [(re.compile(a.replace(' ', r'\s+')), b) for a, b in KAMUS]

def sederhanakan(teks):
    """Ganti kata baku-kaku, hanya pada teks di luar tag HTML."""
    hasil = []
    for bagian in re.split(r'(<[^>]*>)', teks):
        if bagian.startswith('<'):
            hasil.append(bagian)
        else:
            for pola, ganti in KAMUS:
                bagian = pola.sub(ganti, bagian)
            hasil.append(bagian)
    return ''.join(hasil)

# ─────────────────────────────────────────── gaya
C_TEKS = '#334155'
C_GELAP = '#0f172a'
C_BIRU = '#1d4ed8'

def kartu(warna, latar, tepi, judul, ikon, isi):
    return (
        '<div style="border:1px solid %s;background:%s;border-radius:10px;'
        'padding:12px 15px;margin:0 0 16px;">'
        '<div style="font-weight:700;color:%s;margin:0 0 8px;font-size:14px;">'
        '%s %s</div>'
        '<div style="color:%s;line-height:1.7;">%s</div></div>'
        % (tepi, latar, warna, ikon, judul, C_TEKS, isi))

def lencana(nomor, judul, anchor):
    return (
        '<h3 id="%s" style="display:flex;align-items:center;gap:9px;margin:26px 0 10px;'
        'font-size:15px;color:%s;">'
        '<span style="flex:0 0 auto;display:inline-flex;align-items:center;justify-content:center;'
        'width:24px;height:24px;border-radius:50%%;background:%s;color:#fff;font-size:12px;'
        'font-weight:700;">%d</span><span>%s</span></h3>'
        % (anchor, C_GELAP, C_BIRU, nomor, judul))

# ─────────────────────────────────────────── pengenalan jenis bagian
def jenis(judul):
    j = judul.lower()
    if 'ringkasan alur' in j:
        return 'ringkas'
    if 'perlu disiapkan' in j:
        return 'siapkan'
    if 'kesalahan umum' in j:
        return 'kesalahan'
    if j.startswith('tips'):
        return 'tips'
    if 'langkah akhir' in j or 'verifikasi' in j:
        return 'akhir'
    if 'sering diajukan' in j or 'tanya jawab' in j:
        return 'faq'
    if 'catatan penting' in j:
        return 'catatan'
    if 'peran dan hak akses' in j:
        return 'peran'
    return 'biasa'

# ─────────────────────────────────────────── pengolah isi bagian
RE_LI = re.compile(r'<li[^>]*>(.*?)</li>', re.S)

def butir(isi_html):
    """Ambil daftar butir <li>; kembalikan None bila bukan daftar."""
    if '<li' not in isi_html:
        return None
    return [b.strip() for b in RE_LI.findall(isi_html)]

def sisa_di_luar_daftar(isi_html):
    """Teks di luar <ul>/<ol> pada satu bagian (kadang ada paragraf pengantar)."""
    tanpa = re.sub(r'<(ul|ol)[^>]*>.*?</\1>', '', isi_html, flags=re.S)
    return tanpa.strip()

def paragraf_bersih(isi_html):
    """Rapikan <p>/<ul>/<ol> bawaan agar spasinya konsisten."""
    s = isi_html
    s = re.sub(r'<p style="[^"]*">', '<p style="color:%s;line-height:1.7;margin:0 0 12px;">' % C_TEKS, s)
    s = re.sub(r'<ul style="[^"]*">', '<ul style="color:%s;line-height:1.75;margin:0 0 12px;padding-left:22px;">' % C_TEKS, s)
    s = re.sub(r'<ol style="[^"]*">', '<ol style="color:%s;line-height:1.75;margin:0 0 12px;padding-left:22px;">' % C_TEKS, s)
    s = re.sub(r'<li>', '<li style="margin:0 0 5px;">', s)
    return s.strip()

def render_daftar_tanda(butir_list, tanda, warna_tanda):
    baris = []
    for b in butir_list:
        baris.append(
            '<div style="display:flex;gap:9px;margin:0 0 8px;align-items:flex-start;">'
            '<span style="flex:0 0 auto;color:%s;font-weight:700;line-height:1.7;">%s</span>'
            '<span style="flex:1 1 auto;">%s</span></div>' % (warna_tanda, tanda, b))
    return ''.join(baris)

def render_langkah(butir_list):
    baris = []
    for i, b in enumerate(butir_list):
        baris.append(
            '<div style="display:flex;gap:10px;margin:0 0 9px;align-items:flex-start;">'
            '<span style="flex:0 0 auto;display:inline-flex;align-items:center;justify-content:center;'
            'width:21px;height:21px;border-radius:50%%;background:#dcfce7;color:#166534;'
            'font-size:11px;font-weight:700;margin-top:2px;">%d</span>'
            '<span style="flex:1 1 auto;">%s</span></div>' % (i + 1, b))
    return ''.join(baris)

RE_TANYA = re.compile(r'^\s*<b>(.*?)</b>\s*(.*)$', re.S)

def render_faq(butir_list):
    baris = []
    for b in butir_list:
        m = RE_TANYA.match(b)
        if m:
            tanya, jawab = m.group(1).strip(), m.group(2).strip()
        else:
            tanya, jawab = b.strip(), ''
        baris.append(
            '<details style="border:1px solid #e2e8f0;border-radius:9px;background:#fff;'
            'margin:0 0 8px;padding:0 13px;">'
            '<summary style="cursor:pointer;color:%s;font-weight:600;padding:11px 0;">%s</summary>'
            '<div style="color:%s;line-height:1.7;padding:0 0 12px;">%s</div></details>'
            % (C_BIRU, tanya, C_TEKS, jawab))
    return ''.join(baris)

def render_bagian(nomor, judul, isi, anchor):
    """Satu bagian dirender sebagai kartu berwarna (tanpa nomor) atau
    sebagai bagian bernomor. Jenisnya ditentukan HANYA oleh judul, agar
    penomoran di daftar isi selalu sejalan dengan hasil render."""
    t = jenis(judul)
    bl = butir(isi)
    # Sebagian bagian punya paragraf pengantar di luar daftar; harus tetap dibawa.
    luar = paragraf_bersih(sisa_di_luar_daftar(isi)) if bl else ''

    if t == 'siapkan':
        isinya = (luar + render_daftar_tanda(bl, '&#10003;', '#b45309')) if bl else paragraf_bersih(isi)
        return kartu('#b45309', '#fffbeb', '#fde68a', judul, '&#9745;', isinya)
    if t == 'kesalahan':
        isinya = (luar + render_daftar_tanda(bl, '&#10007;', '#b91c1c')) if bl else paragraf_bersih(isi)
        return kartu('#b91c1c', '#fef2f2', '#fecaca', judul, '&#9888;', isinya)
    if t == 'akhir':
        isinya = (luar + render_langkah(bl)) if bl else paragraf_bersih(isi)
        return kartu('#166534', '#f0fdf4', '#bbf7d0', judul, '&#10003;', isinya)
    if t == 'tips':
        isinya = (luar + render_daftar_tanda(bl, '&#8226;', '#1d4ed8')) if bl else paragraf_bersih(isi)
        return kartu('#1d4ed8', '#eff6ff', '#bfdbfe', judul, '&#128161;', isinya)
    if t == 'catatan':
        isinya = (luar + render_daftar_tanda(bl, '&#8226;', '#b45309')) if bl else paragraf_bersih(isi)
        return kartu('#b45309', '#fffbeb', '#fde68a', judul, '&#128204;', isinya)
    if t == 'peran':
        isinya = (luar + render_daftar_tanda(bl, '&#8226;', '#475569')) if bl else paragraf_bersih(isi)
        return kartu('#475569', '#f8fafc', '#e2e8f0', judul, '&#128100;', isinya)

    if t == 'faq' and bl:
        return (lencana(nomor, judul, anchor) + luar
                + '<div style="margin:0 0 16px;">' + render_faq(bl) + '</div>')

    return lencana(nomor, judul, anchor) + paragraf_bersih(isi)

def perlu_toc(judul):
    """Hanya bagian yang dirender bernomor yang masuk daftar isi.
    Harus sejalan persis dengan cabang terakhir render_bagian()."""
    return jenis(judul) in ('biasa', 'faq')

# ─────────────────────────────────────────── pembongkaran berkas
RE_H2 = re.compile(r'<h2[^>]*>(.*?)</h2>', re.S)
RE_H3 = re.compile(r'<h3[^>]*>(.*?)</h3>', re.S)
RE_NOMOR = re.compile(r'^\s*\d+\.\s*')

def teks_polos(html):
    s = re.sub(r'<[^>]+>', ' ', html)
    s = s.replace('&nbsp;', ' ')
    return re.sub(r'\s+', ' ', s).strip()

def bongkar(s):
    """Kembalikan (judul, intro, [(judul_bagian, isi)], penutup, catatan_kaki)."""
    m = RE_H2.search(s)
    if not m:
        return None
    judul = teks_polos(m.group(1))
    sisa = s[m.end():]

    potong = [(x.start(), x.end(), teks_polos(x.group(1))) for x in RE_H3.finditer(sisa)]
    if not potong:
        return None

    intro = sisa[:potong[0][0]].strip()
    bagian = []
    for i, (a, b, t) in enumerate(potong):
        ujung = potong[i + 1][0] if i + 1 < len(potong) else len(sisa)
        bagian.append([RE_NOMOR.sub('', t), sisa[b:ujung].strip()])

    # Dua paragraf terakhir berkas adalah penutup (italic) dan catatan kaki (abu).
    penutup, kaki = '', ''
    akhir = bagian[-1][1]
    par = re.findall(r'<p[^>]*>.*?</p>', akhir, re.S)
    if len(par) >= 2:
        kandidat_kaki = par[-1]
        kandidat_penutup = par[-2]
        if '#94a3b8' in kandidat_kaki and 'font-style:italic' in kandidat_penutup:
            kaki = kandidat_kaki
            penutup = kandidat_penutup
            bagian[-1][1] = akhir.replace(kandidat_kaki, '').replace(kandidat_penutup, '').strip()
    return judul, intro, bagian, penutup, kaki

# ─────────────────────────────────────────── perakitan
def rakit(judul, intro, bagian, penutup, kaki, terkait):
    ringkas_isi = ''
    tersaring = []
    for t, isi in bagian:
        if jenis(t) == 'ringkas' and not ringkas_isi:
            ringkas_isi = teks_polos(isi)
        else:
            tersaring.append((t, isi))

    out = []
    out.append('<!-- ' + TANDA + ' -->\n')
    out.append('<h2 style="margin:0 0 10px;color:%s;font-size:19px;">%s</h2>\n' % (C_BIRU, judul))

    # Ringkasan didahulukan: itulah yang dicari pembaca dalam beberapa detik
    # pertama. Paragraf pembuka yang panjang dilipat karena isinya diulang
    # oleh bagian Tujuan Halaman di bawahnya.
    if ringkas_isi:
        out.append(kartu('#166534', '#f0fdf4', '#bbf7d0', 'Ringkas &mdash; inti halaman ini',
                         '&#9889;', ringkas_isi) + '\n')

    if intro:
        if ringkas_isi and len(teks_polos(intro)) > 320:
            out.append(
                '<details style="border:1px solid #e2e8f0;border-radius:9px;background:#f8fafc;'
                'margin:0 0 16px;padding:0 13px;">'
                '<summary style="cursor:pointer;color:#475569;font-weight:600;padding:11px 0;'
                'font-size:13px;">Tentang halaman ini</summary>'
                '<div style="color:%s;line-height:1.7;padding:0 0 12px;">%s</div></details>'
                % (C_TEKS, paragraf_bersih(intro)) + chr(10))
        else:
            out.append(paragraf_bersih(intro) + chr(10))

    # Daftar isi (chip) untuk bagian bernomor.
    nomor = 0
    penomoran = []
    for t, isi in tersaring:
        if perlu_toc(t):
            nomor += 1
            penomoran.append(nomor)
        else:
            penomoran.append(None)
    if nomor >= 4:
        chip = []
        for i, (t, isi) in enumerate(tersaring):
            if penomoran[i] is None:
                continue
            chip.append(
                '<a href="#kb%d" onclick="var e=document.getElementById(\'kb%d\');'
                'if(e&&e.scrollIntoView){e.scrollIntoView();return false;}" '
                'style="display:inline-block;background:#eef2f7;border:1px solid #dbe3ec;'
                'border-radius:20px;padding:4px 11px;margin:0 6px 6px 0;color:%s;'
                'text-decoration:none;font-size:12px;">%d. %s</a>'
                % (penomoran[i], penomoran[i], C_BIRU, penomoran[i], t))
        out.append('<div style="margin:0 0 18px;">'
                   '<div style="font-size:12px;color:#64748b;font-weight:600;margin:0 0 6px;">'
                   'ISI PANDUAN</div>' + ''.join(chip) + '</div>\n')

    for i, (t, isi) in enumerate(tersaring):
        out.append(render_bagian(penomoran[i] or 0, t, isi, 'kb%s' % (penomoran[i] or 'x')) + '\n')

    if penutup:
        out.append(
            '<details style="border:1px solid #e2e8f0;border-radius:9px;background:#f8fafc;'
            'margin:20px 0 0;padding:0 13px;">'
            '<summary style="cursor:pointer;color:#475569;font-weight:600;padding:11px 0;'
            'font-size:13px;">Penutup</summary>'
            '<div style="color:%s;line-height:1.7;padding:0 0 12px;">%s</div></details>\n'
            % (C_TEKS, teks_polos(penutup)))

    if kaki:
        out.append('<p style="color:#94a3b8;line-height:1.6;margin:14px 0 0;font-size:12px;">%s</p>\n'
                   % teks_polos(kaki))

    if terkait:
        out.append(terkait)

    return ''.join(out)

# ─────────────────────────────────────────── verifikasi isi
RE_KATA = re.compile(r'[0-9A-Za-z\u00C0-\u024F]+')

def kata(html):
    return RE_KATA.findall(teks_polos(html).lower())

# Kata yang memang sengaja tidak dibawa: judul bagian "Ringkasan Alur Singkat"
# diganti kartu "Ringkas", dan nomor bagian dinomori ulang.
DIIZINKAN_HILANG = set(['ringkasan', 'alur', 'singkat'])
RE_NOMOR_H3 = re.compile(r'(<h3[^>]*>)\s*\d+\.\s*', re.I)

def verifikasi(lama, baru, nama):
    """Pastikan tidak ada teks asli yang hilang (urutan boleh berubah)."""
    import collections
    a = collections.Counter(RE_KATA.findall(
        sederhanakan(teks_polos(RE_NOMOR_H3.sub(r'', lama))).lower()))
    b = collections.Counter(kata(baru))
    hilang = a - b
    for w in list(hilang.keys()):
        if w in DIIZINKAN_HILANG:
            del hilang[w]
    # Kata gaya (style/atribut) tidak terhitung karena teks_polos membuang tag.
    if hilang:
        total = sum(hilang.values())
        contoh = ', '.join(list(hilang.keys())[:12])
        return 'TEKS HILANG (%d kata) di %s: %s' % (total, nama, contoh)
    return None

# ─────────────────────────────────────────── penjaga keseimbangan tag
from html.parser import HTMLParser

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
    """Benar hanya bila setiap tag berpasangan rapi. Berkas di luar template
    baku (mis. panduan tulisan tangan berbungkus <div>) akan gagal di sini
    dan sengaja dilewati agar tidak rusak."""
    try:
        pr = _Penyeimbang()
        pr.feed(html)
        pr.close()
        return not pr.rusak and not pr.tumpuk
    except Exception:
        return False

# ─────────────────────────────────────────── utama
def proses(nama, tulis=True):
    p = os.path.join(DIR, nama)
    asli = io.open(p, 'r', encoding='utf-8').read()
    if TANDA in asli:
        return 'lewat'

    terkait = ''
    kerja = asli
    i = kerja.find('<!-- ' + TANDA_TERKAIT + ' -->')
    if i >= 0:
        terkait = kerja[i:].strip() + '\n'
        kerja = kerja[:i]

    hasil = bongkar(kerja)
    if hasil is None:
        return 'lewat-strukturlain'
    judul, intro, bagian, penutup, kaki = hasil
    if len(bagian) < 3:
        return 'lewat-terlalupendek'

    baru = rakit(judul, intro, bagian, penutup, kaki, terkait)
    baru = sederhanakan(baru)

    galat = verifikasi(asli, baru, nama)
    if galat:
        return galat

    if not seimbang(baru):
        return 'lewat-taktemplate'

    if tulis:
        io.open(p, 'w', encoding='utf-8', newline='').write(baru)
    return 'ok'

if __name__ == '__main__':
    uji = '--uji' in sys.argv
    hanya = [a for a in sys.argv[1:] if not a.startswith('--')]
    if hanya:
        berkas = hanya
    else:
        berkas = sorted(f for f in os.listdir(DIR)
                        if f.endswith('.html')
                        and not f.endswith('_qa.html')
                        and not f.startswith('_')
                        and not f.startswith('panduan'))
    ringkas = {}
    galat = []
    for f in berkas:
        r = proses(f, tulis=not uji)
        kunci = r if r in ('ok', 'lewat', 'lewat-strukturlain', 'lewat-terlalupendek',
                          'lewat-taktemplate') else 'GALAT'
        ringkas[kunci] = ringkas.get(kunci, 0) + 1
        if kunci == 'GALAT':
            galat.append(r)
    print('MODE:', 'UJI (tidak menulis)' if uji else 'TULIS')
    for k in sorted(ringkas):
        print('  %-22s %d' % (k, ringkas[k]))
    for g in galat[:25]:
        print('  !', g)
