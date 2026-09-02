# -*- coding: utf-8 -*-
"""Periksa kelas Java yang dirujuk berkas .zul benar-benar ada.

Halaman ZK memuat kelas lewat atribut seperti use="...", apply="...",
viewModel="@id('vm') @init('...')". Kelas itu baru dicari ZK ketika halamannya
dibuka; kalau sudah pindah paket atau dihapus, halamannya mati di depan
pengguna dan tidak ada satu pun langkah sebelum itu yang menyadarinya --
persis lubang yang sama dengan scriptlet JSP (docs/pos/84).

Sengaja memeriksa terhadap BERKAS SUMBER, bukan pohon kelas hasil kompilasi.
Pohon kelas di repositori ini basi dalam hitungan menit (docs/pos/87), dan
memeriksa terhadapnya menghasilkan hantu. Berkas .java selalu mutakhir.

Yang diperiksa hanya rujukan berawalan "ais." -- kelas ZK dan pustaka pihak
ketiga ada di dalam jar, bukan di pohon sumber.

Pakai:  python zul-rujukan-kelas.py [akar-webapp]
Keluar: 0 bila semua rujukan sahih, 1 bila ada yang menggantung.
"""
import os
import re
import sys

WEBAPP = sys.argv[1] if len(sys.argv) > 1 else r'C:\opt\AIS\ais\src\main\webapp'
SUMBER = r'C:\opt\AIS\ais\src\main\java'

# use="x", apply="x", dan @init('x') di dalam viewModel
POLA = re.compile(r'(?:use|apply)\s*=\s*"([^"]+)"|@init\(\s*[\'"]([^\'"]+)[\'"]')


def kelas_ada(nama):
    """Ada berkas sumbernya? Kelas bersarang diperiksa lewat kelas luarnya."""
    luar = nama.split('$')[0]
    return os.path.isfile(os.path.join(SUMBER, *luar.split('.')) + '.java')


def telusuri(akar):
    menggantung = {}
    diperiksa = set()
    berkas = 0
    for dp, _, fs in os.walk(akar):
        for f in fs:
            if not f.endswith('.zul'):
                continue
            berkas += 1
            p = os.path.join(dp, f)
            try:
                teks = open(p, encoding='utf-8', errors='replace').read()
            except Exception:
                continue
            for m in POLA.finditer(teks):
                for nilai in m.groups():
                    if not nilai:
                        continue
                    # satu atribut boleh memuat beberapa kelas, dipisah koma
                    for nama in re.split(r'[,\s]+', nilai.strip()):
                        if not nama.startswith('ais.'):
                            continue
                        diperiksa.add(nama)
                        if not kelas_ada(nama):
                            menggantung.setdefault(nama, []).append(
                                os.path.relpath(p, akar))
    return berkas, diperiksa, menggantung


if __name__ == '__main__':
    berkas, diperiksa, menggantung = telusuri(WEBAPP)
    print('berkas .zul       : %d' % berkas)
    print('rujukan ais.* unik: %d' % len(diperiksa))
    print('menggantung       : %d\n' % len(menggantung))
    for nama in sorted(menggantung):
        pemakai = menggantung[nama]
        print('%s' % nama)
        for x in sorted(set(pemakai))[:3]:
            print('    %s' % x)
        if len(set(pemakai)) > 3:
            print('    ... dan %d berkas lain' % (len(set(pemakai)) - 3))
    sys.exit(1 if menggantung else 0)
