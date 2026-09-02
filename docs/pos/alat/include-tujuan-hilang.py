# -*- coding: utf-8 -*-
"""Periksa tujuan include RUNTIME pada JSP dan ZUL benar-benar ada.

Include STATIS pada JSP (<%@ include file="..." %>) sudah diverifikasi Jasper
saat penerjemahan (docs/pos/83) -- berkas yang hilang membuat terjemahannya
gagal. Yang TIDAK pernah diperiksa siapa pun:

  <jsp:include page="..."/>     dimuat saat halaman dijalankan
  <include src="..."/>  (ZUL)   dimuat saat komponennya dirender

Keduanya baru dicari ketika halamannya dibuka. Tujuan yang hilang karena itu
tidak muncul di gerbang mana pun -- hanya sebagai halaman galat di depan
pengguna, atau lebih buruk: bagian halaman yang diam-diam kosong.

Jalur yang memuat ekspresi dinamis (<%= %>, ${...}, +) dilewati: nilainya baru
terbentuk saat berjalan dan tidak dapat dinilai dari sumber.

Pemetaan jalur:
  /WEB-INF/...  ->  webapp/WEB-INF/...
  /pages/...    ->  webapp/WEB-INF/z/x/y/pages/...   (akar ZUL)
  lainnya /...  ->  webapp/...
  relatif       ->  relatif terhadap direktori berkas yang memuatnya

Pakai:  python include-tujuan-hilang.py [akar-webapp]
Keluar: 0 bila semua tujuan ada, 1 bila ada yang hilang.
"""
import os
import re
import sys

WEBAPP = sys.argv[1] if len(sys.argv) > 1 else r'C:\opt\AIS\ais\src\main\webapp'
AKAR_ZUL = os.path.join(WEBAPP, 'WEB-INF', 'z', 'x', 'y')

JSP_INC = re.compile(r'<jsp:include[^>]*?page\s*=\s*"([^"]*)"', re.S)
# Komentar dibuang lebih dulu. Include yang berada di dalam <%-- --%> atau
# <!-- --> tidak pernah dijalankan, jadi tujuannya tidak perlu ada -- sebuah
# pemeriksa harus membaca sumbernya seperti runtime membacanya.
KOMENTAR = re.compile(r'<%--.*?--%>|<!--.*?-->', re.S)
ZUL_INC = re.compile(r'<include[^>]*?src\s*=\s*"([^"]*)"', re.S)
DINAMIS = re.compile(r'<%|\$\{|\+|\?')


def resolve(nilai, berkas):
    """Daftar jalur yang mungkin dituju; kosong bila tidak dapat dinilai statis.

    Jalur absolut pada ZUL diselesaikan terhadap AKAR ZUL, bukan akar webapp:
    <include src="/pascasarjana/blank_pasca.zul"/> menunjuk
    webapp/WEB-INF/z/x/y/pascasarjana/blank_pasca.zul. Keduanya tetap dicoba
    karena berkas statis (/img, /css) memang ada di akar webapp.
    """
    v = nilai.strip()
    if not v or DINAMIS.search(v):
        return []
    v = v.split('?', 1)[0]
    if v.startswith('/'):
        bagian = v.lstrip('/').split('/')
        if berkas.endswith('.zul'):
            return [os.path.join(AKAR_ZUL, *bagian), os.path.join(WEBAPP, *bagian)]
        return [os.path.join(WEBAPP, *bagian)]
    return [os.path.normpath(os.path.join(os.path.dirname(berkas), *v.split('/')))]


def telusuri(akar):
    hilang = {}
    diperiksa = dilewati = 0
    for dp, _, fs in os.walk(akar):
        for f in fs:
            if not f.endswith(('.jsp', '.zul')):
                continue
            p = os.path.join(dp, f)
            try:
                teks = open(p, encoding='utf-8', errors='replace').read()
            except Exception:
                continue
            teks = KOMENTAR.sub('', teks)
            pola = JSP_INC if f.endswith('.jsp') else ZUL_INC
            for nilai in pola.findall(teks):
                calon = resolve(nilai, p)
                if not calon:
                    dilewati += 1
                    continue
                diperiksa += 1
                if not any(os.path.isfile(c) for c in calon):
                    hilang.setdefault(nilai.strip(), []).append(
                        os.path.relpath(p, akar))
    return diperiksa, dilewati, hilang


if __name__ == '__main__':
    diperiksa, dilewati, hilang = telusuri(WEBAPP)
    print('tujuan diperiksa : %d' % diperiksa)
    print('dilewati (dinamis): %d' % dilewati)
    print('tujuan hilang    : %d\n' % len(hilang))
    for nilai in sorted(hilang):
        pemakai = sorted(set(hilang[nilai]))
        print('%s   (%d pemakai)' % (nilai, len(pemakai)))
        for x in pemakai[:2]:
            print('    %s' % x)
    sys.exit(1 if hilang else 0)
