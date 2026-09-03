# -*- coding: utf-8 -*-
"""Kompilasi dan jalankan harness UAT Java di `ais/src/test`.

Dua puluh harness ada di sana, semuanya punya `main()`, dan sembilan belas di
antaranya sama sekali tidak menyentuh basis data. Sampai berkas ini ada, tidak
satu pun pernah dijalankan dari sesi ini -- termasuk dua yang ditulis seri
dokumen ini sendiri.

Sebabnya bentuk yang sama seperti docs/pos/98: kredensial basis data UAT ditolak,
lalu "harness tidak dapat dijalankan" ikut menempel pada SELURUH direktori itu,
padahal yang bersandar-basis-data cuma satu (`PostgreSqlInventoryLedgerIntegrationUat`).
Satu kendala nyata menjadi alasan untuk tidak mencoba sembilan belas yang lain.

Harness bersandar-basis-data dilewati secara eksplisit, bukan diam-diam: namanya
disebut supaya jelas apa yang TIDAK dijalankan.

Catatan: `ais/src/test` BUKAN working copy SVN (docs/pos/82). Alat ini tidak
mengubah itu; memasukkannya ke repositori berarti membuat jalur tingkat-atas
baru, dan itu keputusan tata letak milik pemiliknya.

Pakai:  python uji-uat-java.py [--daftar]
"""
import os
import re
import subprocess
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import akar_repo  # noqa: E402  -- perlu sys.path di atas

# ais/src/test, sejajar dengan ais/src/main
# AIS_MAIN = .../ais/src/main  ->  AKAR_UAT = .../ais/src/test/java
AKAR_UAT = os.path.join(os.path.dirname(akar_repo.AIS_MAIN), 'test', 'java')
AKAR_AIS = os.path.dirname(os.path.dirname(akar_repo.AIS_MAIN))

# Menyentuh basis data -- dilewati, dan disebut namanya.
BERSANDAR_DB = ('PostgreSqlInventoryLedgerIntegrationUat',)

PAKET = re.compile(r'^\s*package\s+([a-zA-Z0-9_.]+)\s*;', re.M)


def kelas_uat():
    hasil = []
    for dp, dn, fn in os.walk(AKAR_UAT):
        for n in sorted(fn):
            if not n.endswith('.java'):
                continue
            jalur = os.path.join(dp, n)
            teks = akar_repo.baca(jalur)
            if 'static void main' not in teks:
                continue
            m = PAKET.search(teks)
            paket = m.group(1) if m else ''
            hasil.append((jalur, (paket + '.' if paket else '') + n[:-5], n[:-5]))
    return hasil


def classpath():
    lib = os.path.join(akar_repo.AIS_MAIN, 'webapp', 'WEB-INF', 'lib')
    jars = [os.path.join(lib, x) for x in sorted(os.listdir(lib))
            if x.endswith('.jar')]
    return os.pathsep.join([os.path.join(AKAR_AIS, 'compile-check')] + jars)


def main():
    akar_repo.pastikan_lengkap()
    if not os.path.isdir(AKAR_UAT):
        print('direktori UAT tidak ketemu: ' + AKAR_UAT)
        return 1

    daftar = kelas_uat()
    print('CAKUPAN  harness ber-main()      : %d' % len(daftar))
    print('         dilewati (bersandar DB) : %s' % ', '.join(BERSANDAR_DB))
    if '--daftar' in sys.argv:
        for _, penuh, _n in daftar:
            print('   ' + penuh)
        return 0

    keluaran = os.path.join(
        os.environ.get('TEMP', os.path.join(AKAR_AIS, 'build')), 'uat-java')
    if not os.path.isdir(keluaran):
        os.makedirs(keluaran)
    cp = classpath()

    sumber = [j for j, _p, n in daftar if n not in BERSANDAR_DB]
    print('')
    print('$ javac (%d berkas)' % len(sumber))
    p = subprocess.Popen(
        ['javac', '-nowarn', '-encoding', 'UTF-8', '-cp', cp,
         '-sourcepath', os.path.join(akar_repo.AIS_SRC) + os.pathsep + AKAR_UAT,
         '-d', keluaran] + sumber,
        stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
    log = p.communicate()[0].decode('utf-8', 'replace')
    penting = [b for b in log.split('\n')
               if b.strip() and 'bootstrap class path' not in b
               and 'deprecat' not in b and 'unchecked' not in b
               and 'Recompile' not in b]
    for b in penting[:10]:
        print('   ' + b)
    if p.returncode != 0:
        print('KOMPILASI GAGAL')
        return 1

    gagal = []
    print('')
    for _j, penuh, nama in daftar:
        if nama in BERSANDAR_DB:
            continue
        q = subprocess.Popen(
            ['java', '-cp', keluaran + os.pathsep + cp, penuh],
            stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
        hasil = q.communicate()[0].decode('utf-8', 'replace').strip()
        ekor = hasil.split('\n')[-1] if hasil else '(tanpa keluaran)'
        tanda = 'OK   ' if q.returncode == 0 else 'GAGAL'
        print('  %s %-46s %s' % (tanda, nama, ekor[:60]))
        if q.returncode != 0:
            gagal.append((nama, hasil))

    print('')
    if gagal:
        for nama, hasil in gagal:
            print('--- %s' % nama)
            for b in hasil.split('\n')[-12:]:
                print('   ' + b)
        print('%d HARNESS GAGAL' % len(gagal))
        return 1
    print('SELURUH HARNESS UAT LULUS')
    return 0


if __name__ == '__main__':
    sys.exit(main())
