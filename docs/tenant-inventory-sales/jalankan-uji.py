# -*- coding: utf-8 -*-
"""Pelari seluruh uji kesetaraan/isolasi tenant di direktori ini.

CARA PAKAI (dari direktori ini):
    python jalankan-uji.py
    python jalankan-uji.py --pg "C:\\Program Files\\PostgreSQL\\16\\bin"
    python jalankan-uji.py uji-kesetaraan-stok.sql      # satu berkas saja

Keluar 0 bila semua LULUS, 1 bila ada GAGAL atau galat SQL.

MENGAPA ADA
-----------
Dua puluh delapan berkas uji lahir satu per satu, tiap batch dengan klaster, port, dan nama
schema-nya sendiri. Tidak satu pun dapat dijalankan tanpa membaca kepalanya lebih dulu dan
menyiapkan klaster secara manual. Kumpulan uji yang tidak dapat dijalankan sebagai kumpulan
akan membusuk: berkasnya tetap ada, tetapi tidak ada yang tahu mana yang masih benar.

Skrip ini menyalakan SATU klaster sekali-pakai, menyiapkan schema yang diminta tiap berkas,
menjalankan semuanya, lalu menghitung LULUS/GAGAL-nya.

TANPA KREDENSIAL
----------------
Klasternya dibuat `initdb --auth=trust` pada direktori sementara, berjalan di 127.0.0.1 pada
port bebas, lalu dihapus. Tidak ada kata sandi yang dipakai, disimpan, maupun diminta -- dan
tidak ada basis data sungguhan yang tersentuh.

BAGAIMANA SCHEMA-NYA DITEBAK
----------------------------
Kepala berkasnya menyebut nama schema dengan kalimat yang tidak seragam, jadi menebaknya dari
prosa akan rapuh. Yang dipakai justru isinya: setiap awalan `<nama>.<tabel>` yang tabelnya ada
pada daftar tabel wajib katalog dianggap nama schema tenant. Awalan `<nama>__audit.` dikenali
sebagai schema auditnya.

Cara ini juga menangkap berkas yang memakai DUA tenant sekaligus (uji isolasi), dan tidak
tertipu oleh alias tabel di dalam kueri.

BATAS YANG DIAKUI
-----------------
Skrip ini tidak mengurutkan berkas menurut ketergantungan: tiap berkas menyiapkan datanya
sendiri (TRUNCATE lalu INSERT), dan itu memang syarat yang dituntut di sini. Berkas yang
bersandar pada sisa data berkas lain akan gagal -- dan itu memang lebih baik ketahuan.
"""
import argparse
import functools
import io
import os
import re
import shutil
import socket
import subprocess
import sys
import tempfile
import time

# Keluaran harus muncul saat berjalan, bukan menumpuk sampai selesai: pelari yang diam
# selama sepuluh menit tidak dapat dibedakan dari pelari yang menggantung.
print = functools.partial(__builtins__.print if hasattr(__builtins__, 'print')
                          else __import__('builtins').print, flush=True)

DIR = os.path.dirname(os.path.abspath(__file__))
JAVA_SRC = os.path.abspath(os.path.join(DIR, '..', '..', 'java'))
LIB = os.path.abspath(os.path.join(DIR, '..', '..', 'webapp', 'WEB-INF', 'lib'))

# Tabel yang pasti milik katalog tenant; dipakai mengenali awalan schema pada isi berkas.
TABEL_PENANDA = (
    'produk', 'mutasi_stok', 'saldo_stok', 'supplier', 'customer', 'salesperson', 'gudang',
    'toko', 'akun', 'satuan', 'sales_order', 'faktur_penjualan', 'piutang_customer',
    'penerimaan_piutang', 'alokasi_penerimaan_piutang', 'hutang_supplier', 'pembelian',
    'pembayaran_hutang', 'alokasi_pembayaran_hutang', 'surat_perintah_sales', 'sales_trip',
    'sales_trip_kas', 'sales_trip_biaya', 'sales_trip_setoran', 'sales_trip_nota',
    'sales_trip_rekonsiliasi', 'sales_trip_pembelian', 'surat_perintah_sales_nota',
    'harga_beli_supplier', 'harga_jual_customer', 'supplier_profile', 'customer_profile',
    'sales_assignment', 'kategori_biaya_sales', 'audit_baris', 'revinfo', 'jurnal',
    'jurnal_detail', 'sales_order_detail', 'stok_opname',
)
POLA_POSISI_TABEL = re.compile(
    r'(?is)\b(?:FROM|JOIN|INTO|UPDATE|TRUNCATE)\s+(?:TABLE\s+)?'
    r'([a-zA-Z_][a-zA-Z0-9_]*)\.(' + '|'.join(TABEL_PENANDA) + r')\b')
# Schema bersama tiruan yang dibuat berkasnya sendiri -- bukan schema tenant.
BUKAN_TENANT = {'koperasi', 'akunting', 'library', 'information_schema', 'pg_catalog', 'public'}


def port_bebas():
    s = socket.socket()
    s.bind(('127.0.0.1', 0))
    p = s.getsockname()[1]
    s.close()
    return p


def jalankan(cmd, **kw):
    return subprocess.run(cmd, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, **kw)


def jalankan_lepas(cmd):
    """Untuk `pg_ctl start`: JANGAN tangkap keluarannya.

    pg_ctl melahirkan daemon yang MEWARISI pipa keluaran induknya. Kalau pipanya kita tangkap,
    pembacanya menunggu pipa itu tertutup -- dan pipa itu baru tertutup ketika servernya mati.
    Akibatnya pelari menggantung selamanya justru pada langkah yang paling cepat. Berkas log
    sudah ditangani lewat -l, jadi keluarannya memang tidak diperlukan di sini.
    """
    return subprocess.run(cmd, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)


def cari_pgbin(diberi):
    if diberi:
        return diberi
    for c in (r'C:\Program Files\PostgreSQL\16\bin', r'C:\Program Files\PostgreSQL\15\bin',
              '/usr/lib/postgresql/16/bin', '/usr/bin'):
        if os.path.exists(os.path.join(c, 'initdb' + ('.exe' if os.name == 'nt' else ''))):
            return c
    return ''


def skema_dari(berkas):
    """{'erp': [...], 'audit': [...]} menurut awalan yang benar-benar dipakai berkasnya.

    Awalan hanya diakui bila muncul pada POSISI TABEL -- tepat sesudah
    FROM/JOIN/INTO/UPDATE/TRUNCATE. Alias kueri (`FROM koperasi.pengadaan_produk x` lalu
    `x.produk`) tidak pernah muncul di posisi itu, sehingga tidak lagi tertangkap.

    Menyaring menurut PANJANG nama tidak dipakai: uji isolasi memang memakai schema dua huruf
    (`ta`, `tb`), dan penyaring panjang justru membuang berkas yang paling penting.
    """
    teks = io.open(berkas, encoding='utf-8', errors='replace').read()
    erp, audit = set(), set()
    for nama, _tabel in POLA_POSISI_TABEL.findall(teks):
        if nama.lower() in BUKAN_TENANT or len(nama) > 40:
            continue
        if nama.endswith('__audit'):
            audit.add(nama)
            erp.add(nama[:-len('__audit')])
        else:
            erp.add(nama)
    return {'erp': sorted(erp), 'audit': sorted(audit)}


def siapkan_ddl(kelas_out, erp, audit):
    r = jalankan(['java', '-cp', kelas_out, 'ais.service.tenant.test.TenantSchemaDdlDump',
                  erp, audit])
    if r.returncode != 0:
        raise RuntimeError('TenantSchemaDdlDump gagal: ' + r.stdout.decode('utf-8', 'replace'))
    return r.stdout.decode('utf-8', 'replace')


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('berkas', nargs='*', help='berkas uji tertentu (bawaan: semuanya)')
    ap.add_argument('--pg', default='', help='direktori bin PostgreSQL')
    args = ap.parse_args()

    pgbin = cari_pgbin(args.pg)
    if not pgbin:
        print('PostgreSQL bin tidak ketemu. Pakai --pg "<dir>".')
        return 2
    initdb = os.path.join(pgbin, 'initdb')
    pg_ctl = os.path.join(pgbin, 'pg_ctl')
    psql = os.path.join(pgbin, 'psql')

    daftar = args.berkas or sorted(
        f for f in os.listdir(DIR) if f.startswith('uji-') and f.endswith('.sql'))
    if not daftar:
        print('tidak ada berkas uji.')
        return 2

    kerja = tempfile.mkdtemp(prefix='uji-tenant-')
    kelas_out = os.path.join(kerja, 'kelas')
    data = os.path.join(kerja, 'pg')
    port = port_bebas()
    kode = 0
    try:
        print('Mengompilasi TenantSchemaDdlDump ...')
        os.makedirs(kelas_out)
        r = jalankan(['javac', '-nowarn', '-encoding', 'UTF-8', '-sourcepath', JAVA_SRC,
                      '-d', kelas_out,
                      os.path.join(JAVA_SRC, 'ais', 'service', 'tenant', 'test',
                                   'TenantSchemaDdlDump.java')])
        if r.returncode != 0:
            print(r.stdout.decode('utf-8', 'replace')[:2000])
            return 2

        print('Menyalakan klaster sekali-pakai di 127.0.0.1:%d ...' % port)
        jalankan([initdb, '-D', data, '-U', 'uji', '--auth=trust', '-E', 'UTF8'])
        jalankan_lepas([pg_ctl, '-D', data, '-o',
                  '-p %d -c listen_addresses=127.0.0.1' % port,
                  '-l', os.path.join(kerja, 'pg.log'), '-w', 'start'])
        time.sleep(1)

        print()
        print('%-44s %6s %6s  %s' % ('berkas', 'LULUS', 'GAGAL', 'schema'))
        print('-' * 86)
        tot_l = tot_g = tot_e = tot_v = 0
        sudah_disiapkan = set()
        for f in daftar:
            jalur = os.path.join(DIR, f)
            sk = skema_dari(jalur)
            if not sk['erp']:
                print('%-44s %6s %6s  (schema tidak terdeteksi)' % (f, '-', '-'))
                tot_e += 1
                kode = 1
                continue
            for e in sk['erp']:
                ddl = siapkan_ddl(kelas_out, e, e + '__audit')
                p = os.path.join(kerja, 'ddl-%s.sql' % e)
                io.open(p, 'w', encoding='utf-8', newline='\n').write(ddl)
                jalankan([psql, '-h', '127.0.0.1', '-p', str(port), '-U', 'uji',
                          '-d', 'postgres', '-q', '-f', p])
            r = jalankan([psql, '-h', '127.0.0.1', '-p', str(port), '-U', 'uji',
                          '-d', 'postgres', '-q', '-f', jalur])
            keluaran = r.stdout.decode('utf-8', 'replace')
            l = keluaran.count('LULUS')
            g = keluaran.count('GAGAL')
            # Galat yang BUKAN bagian penjaga: penjaga tertentu memang memancing galat batasan.
            galat = [x for x in keluaran.splitlines()
                     if 'ERROR' in x and 'batasan unik' not in x and 'unique constraint' not in x]
            tot_l += l
            tot_g += g
            tanda = ''
            if g or galat:
                tanda = '  <== PERIKSA'
                kode = 1
                tot_e += len(galat)
            elif l == 0:
                # Berkas tanpa satu pun verdikt BUKAN berkas yang lulus: ia berkas yang tidak
                # menyatakan apa-apa. Dibiarkan tampak 0/0 tanpa tanda, ia tidak dapat
                # dibedakan dari uji yang benar-benar hijau -- dan itulah cara kumpulan uji
                # membusuk tanpa ada yang sadar.
                tanda = '  <== TANPA VERDIKT (gaya lama: cetak-banding, tanpa LULUS/GAGAL)'
                tot_v += 1
            print('%-44s %6d %6d  %s%s' % (f, l, g, ','.join(sk['erp'])[:18], tanda))
            for x in galat[:3]:
                print('        %s' % x.strip()[:96])
        print('-' * 86)
        print('TOTAL  LULUS=%d  GAGAL=%d  galat-SQL=%d  tanpa-verdikt=%d  berkas=%d' %
              (tot_l, tot_g, tot_e, tot_v, len(daftar)))
        if tot_v:
            print('CATATAN: %d berkas tidak menyatakan LULUS/GAGAL sama sekali. Berkas gaya'
                  ' lama itu mencetak tabel banding untuk dibaca manusia; ia TIDAK menjaga'
                  ' apa pun secara mekanis.' % tot_v)
        print('HASIL: ' + ('LULUS' if kode == 0 else 'ADA YANG GAGAL'))
        return kode
    finally:
        jalankan_lepas([pg_ctl, '-D', data, '-m', 'fast', '-w', 'stop'])
        shutil.rmtree(kerja, ignore_errors=True)


if __name__ == '__main__':
    sys.exit(main())
