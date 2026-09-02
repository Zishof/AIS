# -*- coding: utf-8 -*-
"""Periksa subreport yang dirujuk berkas .jrxml benar-benar ada.

Templat JasperReports memuat subreport lewat ekspresi seperti

    <subreportExpression><![CDATA[$P{SUBREPORT_DIR} + "akunting/jurnal.jasper"]]></subreportExpression>

Nama berkasnya literal, tetapi baru dicari ketika laporannya DICETAK. Subreport
yang hilang membuat pencetakan gagal di tengah jalan -- setelah pengguna menekan
Cetak dan menunggu, bukan sebelumnya.

Catatan penting: berkas .jasper yang belum ada TIDAK selalu berarti rusak.
Report.java mengompilasi .jrxml menjadi .jasper saat dijalankan bila belum ada
(JasperCompileManager.compileReportToFile). Karena itu sebuah rujukan dianggap
sahih bila .jasper ATAU .jrxml-nya ada.

Direktori arsip (_backup_old_*) dilewati.

Pakai:  python subreport-hilang.py [akar-webapp]
Keluar: 0 bila semua subreport ada, 1 bila ada yang hilang.
"""
import os
import re
import sys

WEBAPP = sys.argv[1] if len(sys.argv) > 1 else r'C:\opt\AIS\ais\src\main\webapp'
AKAR_REPORT = os.path.join(WEBAPP, 'report')

EKSPRESI = re.compile(r'<subreportExpression[^>]*>(.*?)</subreportExpression>', re.S)
LITERAL = re.compile(r'"([^"]*\.jasper)"')
PARAM = re.compile(r'\$P\{(\w+)\}')


def ada(nama, jrxml):
    """Sahih bila .jasper ATAU .jrxml-nya ada (yang kedua dikompilasi saat jalan)."""
    kandidat = [
        os.path.join(AKAR_REPORT, *nama.lstrip('/').split('/')),
        os.path.join(os.path.dirname(jrxml), *nama.lstrip('/').split('/')),
    ]
    for c in kandidat:
        if os.path.isfile(c) or os.path.isfile(c[:-7] + '.jrxml'):
            return True
    return False


def telusuri(akar):
    hilang, diperiksa, dinamis = {}, 0, 0
    for dp, _, fs in os.walk(akar):
        if '_backup_old_' in dp.replace(os.sep, '/'):
            continue
        for f in fs:
            if not f.endswith('.jrxml'):
                continue
            p = os.path.join(dp, f)
            try:
                teks = open(p, encoding='utf-8', errors='replace').read()
            except Exception:
                continue
            for eks in EKSPRESI.findall(teks):
                lain = [x for x in PARAM.findall(eks) if x != 'SUBREPORT_DIR']
                nama = [n for n in LITERAL.findall(eks) if n.strip('/') != '.jasper']
                if lain or not nama:
                    dinamis += 1
                    continue
                for n in nama:
                    diperiksa += 1
                    if not ada(n, p):
                        hilang.setdefault(n, []).append(os.path.relpath(p, akar))
    return diperiksa, dinamis, hilang


if __name__ == '__main__':
    diperiksa, dinamis, hilang = telusuri(WEBAPP)
    print('rujukan subreport : %d' % diperiksa)
    print('dilewati (dinamis): %d' % dinamis)
    print('hilang            : %d\n' % len(hilang))
    for n in sorted(hilang):
        pemakai = sorted(set(hilang[n]))
        print('%s   (%d pemakai)' % (n, len(pemakai)))
        for x in pemakai[:3]:
            print('    %s' % x)
    sys.exit(1 if hilang else 0)
