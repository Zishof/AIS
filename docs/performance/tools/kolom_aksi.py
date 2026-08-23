# -*- coding: utf-8 -*-
"""Sempitkan kolom aksi ("...") pada seluruh layar.

Kolom terakhir tiap grid berisi menu kebab "..." — satu tombol kecil — tetapi
sebagian besar ZUL mendeklarasikannya width="10%". Di layar lebar itu berarti
~190 px ruang kosong di ujung kanan tabel.

UIHelper.kecilkanKolomAksi() sebenarnya sudah menyetel 56px saat runtime, namun
itu bergantung pada kecocokan indeks sel dengan indeks kolom; bila baris tidak
mengisi semua kolom, penyetelan meleset. Mendeklarasikannya langsung di ZUL
membuat lebarnya pasti, dan angkanya sengaja disamakan dengan
GridKolomHelper.LEBAR_KOLOM_AKSI agar tidak ada dua sumber kebenaran.

Aturan konservatif:
  * hanya kolom berlabel kosong yang berada di UJUNG blok <columns>;
  * hanya bila lebarnya persentase, tidak ada, atau px yang lebih besar dari 56;
  * kolom yang sudah sempit dibiarkan; tidak ada kolom yang disembunyikan,
    sehingga tidak mungkin ada tombol yang hilang.
"""
import io, os, re, sys, collections
import xml.dom.minidom

# -- Lokasi berkas bantuan ---------------------------------------------------
# Diturunkan dari letak skrip ini (docs/performance/tools/) agar berjalan sama
# di Windows maupun di server Linux. Dapat ditimpa lewat variabel lingkungan
# AIS_WEBINF bila salinan kerja berada di tempat lain.
_AKAR = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)),
                                     '..', '..', '..'))
WEBINF = os.environ.get('AIS_WEBINF') or os.path.join(
    _AKAR, 'src', 'main', 'webapp', 'WEB-INF')


AKAR = WEBINF
LEBAR = '56px'

RE_BLOK = re.compile(r'<columns\b.*?</columns>', re.S)
RE_COL = re.compile(r'<column\b[^>]*?/>', re.S)
RE_LBL = re.compile(r'label\s*=\s*"([^"]*)"')
RE_W = re.compile(r'width\s*=\s*"([^"]*)"')
RE_ALIGN = re.compile(r'\balign\s*=')


def perlu_diubah(w):
    """Benar bila lebar sekarang boros untuk satu tombol kebab."""
    if w is None:
        return True
    w = w.strip()
    if w.endswith('%'):
        try:
            return float(w[:-1]) > 0          # 0% memang sengaja disembunyikan
        except ValueError:
            return False
    if w.endswith('px'):
        try:
            return float(w[:-2]) > 56
        except ValueError:
            return False
    return False


def ubah_kolom(col):
    """Kembalikan (kolom_baru, berubah)."""
    mw = RE_W.search(col)
    w = mw.group(1) if mw else None
    if not perlu_diubah(w):
        return col, False
    if mw:
        baru = col[:mw.start()] + 'width="' + LEBAR + '"' + col[mw.end():]
    else:
        baru = re.sub(r'/>\s*$', ' width="' + LEBAR + '"/>', col)
    if not RE_ALIGN.search(baru):
        baru = re.sub(r'/>\s*$', ' align="center"/>', baru)
    return baru, True


def proses(teks):
    """Kembalikan (teks_baru, jumlah_kolom_diubah)."""
    diubah = [0]

    def olah_blok(m):
        blok = m.group(0)
        cols = RE_COL.findall(blok)
        if not cols:
            return blok
        ekor = 0
        for c in reversed(cols):
            ml = RE_LBL.search(c)
            if ml is not None and ml.group(1).strip() == '':
                ekor += 1
            else:
                break
        if ekor == 0:
            return blok
        baru_blok = blok
        for c in cols[len(cols) - ekor:]:
            nc, ok = ubah_kolom(c)
            if ok:
                # ganti kemunculan PERTAMA agar kolom identik tidak tertukar
                baru_blok = baru_blok.replace(c, nc, 1)
                diubah[0] += 1
        return baru_blok

    return RE_BLOK.sub(olah_blok, teks), diubah[0]


if __name__ == '__main__':
    uji = '--uji' in sys.argv
    ringkas = collections.Counter()
    total_kolom = 0
    gagal = []
    for akar, _, berkas in os.walk(AKAR):
        for b in berkas:
            if not b.endswith('.zul'):
                continue
            p = os.path.join(akar, b)
            try:
                asli = io.open(p, 'r', encoding='utf-8').read()
            except Exception:
                ringkas['lewat-takterbaca'] += 1
                continue
            baru, n = proses(asli)
            if n == 0:
                ringkas['lewat-tidakperlu'] += 1
                continue
            # Isi teks selain atribut width/align tidak boleh berubah.
            # Bandingkan setelah membuang width/align DAN menormalkan spasi:
            # penulisan " />" bisa menjadi "/>" saat atribut disisipkan, dan itu
            # perbedaan tata letak belaka, bukan perubahan isi.
            def sidik(t):
                t = re.sub(r'\s*(width|align)\s*=\s*"[^"]*"', '', t)
                t = re.sub(r'\s+', ' ', t)
                return t.replace(' />', '/>').replace(' >', '>')
            bandingA = sidik(asli)
            bandingB = sidik(baru)
            if bandingA != bandingB:
                gagal.append('%s: isi selain width/align ikut berubah' % p)
                ringkas['GAGAL'] += 1
                continue
            # Perubahan hanya menyentuh nilai atribut, tidak pernah menambah atau
            # membuang tag. Tetap diperiksa: bila berkas asalnya XML sah, hasilnya
            # wajib tetap sah. Berkas yang memang sudah tidak sah dilewati.
            try:
                xml.dom.minidom.parseString(asli.encode('utf-8'))
            except Exception:
                ringkas['lewat-xmlasaltidaksah'] += 1
                continue
            try:
                xml.dom.minidom.parseString(baru.encode('utf-8'))
            except Exception as e:
                gagal.append('%s: hasil bukan XML sah (%s)' % (p, e))
                ringkas['GAGAL'] += 1
                continue
            if not uji:
                io.open(p, 'w', encoding='utf-8', newline='').write(baru)
            ringkas['ok'] += 1
            total_kolom += n
    print('MODE:', 'UJI (tidak menulis)' if uji else 'TULIS')
    for k in sorted(ringkas):
        print('  %-20s %d' % (k, ringkas[k]))
    print('  kolom aksi disempitkan: %d' % total_kolom)
    for g in gagal[:10]:
        print('  !', g)
