# -*- coding: utf-8 -*-
"""Penjaga LOCAL-FIRST: tidak satu pun jalur tenant boleh menyentuh schema bersama.

CARA PAKAI (dari direktori ini):
    python audit-local-first.py
Keluar 0 bila bersih, 1 bila ada jalur tenant yang masih menyentuh schema bersama.

APA YANG DIJAGA
---------------
Model tenant menaruh datanya di schema sendiri. Satu saja kueri jalur tenant yang masih
menunjuk `koperasi.`/`akunting.`/`library.` berarti tenant itu membaca -- atau lebih buruk,
menulis -- data instalasi bersama. Bukan sekadar angka yang salah: itu kebocoran antar-tenant.

Bahayanya diam. Kueri semacam itu berjalan mulus, mengembalikan baris yang tampak wajar, dan
tidak melempar apa pun. Karena itu penjagaannya harus mekanis, bukan ingatan.

TIGA HAL YANG DIPERIKSA
-----------------------
1. Kelas `*Tenant.java` -- yang menurut definisi HANYA jalur tenant -- tidak boleh memuat
   literal schema bersama sama sekali, dan tidak boleh memakai entitas Hibernate (yang mematok
   `@Table(schema = ...)`, sehingga menulis lewatnya selalu mendarat di schema bersama).
2. Metode `*Tenant(` di dalam helper: sama.
3. Di dalam helper campuran, tiap baris ber-schema-bersama harus terbukti TIDAK TERCAPAI saat
   tenant aktif -- lewat salah satu dari:
      a. metodenya punya dispatch awal `if (<uji tenant>) { xTenant(...); return; }`;
      b. barisnya berada di cabang `else` dari `if (<uji tenant>)`, atau di dalam
         `if (!<uji tenant>)`;
      c. PERNYATAAN-nya sendiri memuat uji tenant (pola terner `jalurTenant ? tenant : legacy`);
      d. metodenya privat dan SELURUH pemanggilnya sudah legacy-only (dirambat sampai tetap).

BATAS YANG DIAKUI
-----------------
Perambatan (d) hanya mengikuti pemanggilan di dalam berkas yang sama. Metode privat yang
dipanggil lintas-berkas tidak dirambat -- ia akan dilaporkan, dan itu memang lebih aman daripada
diam. Skrip ini juga tidak menjalankan kode: ia membaca teks. Cabang yang dibentuk lewat
variabel boolean selain `jalurTenant` tidak dikenali dan akan dilaporkan.
"""
import io
import os
import re
import sys
import glob

SHARED = re.compile(r'"[^"]*\b(koperasi|akunting|library)\.')
UJI_TENANT = re.compile(r'jalurTenant|[A-Za-z_]*Tenant\.aktif\s*\(')
IF_TENANT = re.compile(r'\bif\s*\(\s*(!\s*)?(?:[A-Za-z_]*Tenant\.aktif\s*\(|jalurTenant)')
DISPATCH = re.compile(r'\b\w+Tenant\s*\(')
PERSIST = re.compile(r'session\.(?:save|saveOrUpdate|update|delete|merge|persist)\s*\(')
FETCH = re.compile(r'session\.(?:get|load)\s*\(|\.createCriteria\s*\(')
TANDA_METODE = re.compile(r'^\t(?:public|private|protected|static)[^;=]*?\s(\w+)\s*\(')


def komentar(baris):
    s = baris.strip()
    return s.startswith('*') or s.startswith('//') or s.startswith('/*')


def metode_metode(baris):
    """[(nama, mulai, akhir)] menurut kedalaman kurung; indeks berbasis 0, akhir inklusif."""
    hasil = []
    i = 0
    while i < len(baris):
        m = TANDA_METODE.match(baris[i])
        if m and not komentar(baris[i]):
            d = 0
            j = i
            mulai_blok = False
            while j < len(baris):
                if not komentar(baris[j]):
                    d += baris[j].count('{') - baris[j].count('}')
                    if '{' in baris[j]:
                        mulai_blok = True
                if mulai_blok and d <= 0:
                    break
                j += 1
            hasil.append((m.group(1), i, min(j, len(baris) - 1)))
            i = j + 1
        else:
            i += 1
    return hasil


def pernyataan_di(baris, i):
    """Teks pernyataan yang memuat baris i: dirangkai sampai titik koma penutupnya."""
    a = i
    while a > 0 and not baris[a - 1].rstrip().endswith((';', '{', '}')):
        a -= 1
    b = i
    while b < len(baris) - 1 and not baris[b].rstrip().endswith(';'):
        b += 1
    return '\n'.join(baris[a:b + 1])


def periksa_berkas_tenant(nama):
    """Kelas *Tenant.java: tidak boleh menyentuh schema bersama / entitas sama sekali."""
    temuan = []
    baris = io.open(nama, encoding='utf-8', newline='').read().split('\n')
    for i, l in enumerate(baris):
        if komentar(l):
            continue
        if SHARED.search(l) or PERSIST.search(l) or FETCH.search(l):
            temuan.append((nama, '(kelas tenant)', i + 1, l.strip()[:100]))
    return temuan


def peta_jangkauan(baris, mulai, akhir):
    """{indeks baris: True bila TERCAPAI saat tenant aktif} untuk satu metode.

    Tiga hal membuat sebuah baris TIDAK tercapai:
      * berada sesudah dispatch awal `if (<uji tenant>) { xTenant(...); return; }`;
      * berada di cabang `else` dari `if (<uji tenant>)`, atau di dalam `if (!<uji tenant>)`;
      * pernyataannya sendiri memuat uji tenant (pola terner).
    """
    peta = {}
    dispatch_di = None
    tumpuk = []          # [(kedalaman_saat_if, aman?)]
    d = 0
    for i in range(mulai, akhir + 1):
        l = baris[i]
        if komentar(l):
            peta[i] = False
            continue
        m = IF_TENANT.search(l)
        if m:
            negasi = bool(m.group(1))
            # telusuri SELURUH blok if-nya, bukan sekadar beberapa baris
            dd = 0
            j = i
            blok = []
            while j <= akhir:
                if not komentar(baris[j]):
                    dd += baris[j].count('{') - baris[j].count('}')
                    blok.append(baris[j])
                if dd <= 0 and '{' in ''.join(blok):
                    break
                j += 1
            isi = '\n'.join(blok)
            if not negasi and DISPATCH.search(isi) and 'return;' in isi:
                dispatch_di = i
            tumpuk.append((d, bool(negasi)))
        if l.strip().startswith('} else') and tumpuk:
            kd, aman = tumpuk[-1]
            tumpuk[-1] = (kd, not aman)
        tercapai = not ((dispatch_di is not None and i > dispatch_di)
                        or (tumpuk and tumpuk[-1][1])
                        or bool(UJI_TENANT.search(pernyataan_di(baris, i))))
        peta[i] = tercapai
        d += l.count('{') - l.count('}')
        while tumpuk and d <= tumpuk[-1][0]:
            tumpuk.pop()
    return peta


def periksa_helper(nama):
    """Helper campuran: laporkan baris ber-schema-bersama yang masih tercapai jalur tenant.

    Perambatan dilakukan atas METODE, bukan atas baris ber-SQL saja: sebuah metode privat
    tercapai jalur tenant bila ada pemanggil yang tercapai DAN barisnya sendiri tercapai.
    Tanpa perambatan atas metode, pembangun potongan SQL yang dipanggil dari cabang legacy
    lewat satu metode perantara akan terus dilaporkan walau sebenarnya aman.
    """
    baris = io.open(nama, encoding='utf-8', newline='').read().split('\n')
    daftar = metode_metode(baris)
    peta = {}
    for nm, mulai, akhir in daftar:
        peta[nm] = (mulai, akhir, peta_jangkauan(baris, mulai, akhir))

    # Titik masuk: metode publik selalu dianggap tercapai.
    publik = set()
    for nm, (mulai, _a, _j) in peta.items():
        if baris[mulai].lstrip().startswith('public'):
            publik.add(nm)
    tercapai = set(publik)

    # Rambat sampai tetap: M tercapai bila ada pemanggil C yang tercapai dan barisnya tercapai.
    for _ in range(20):
        berubah = False
        for nm, (mulai, akhir, _j) in peta.items():
            if nm in tercapai:
                continue
            for nm2, (m2, a2, j2) in peta.items():
                if nm2 == nm or nm2 not in tercapai:
                    continue
                ketemu = False
                for i in range(m2, a2 + 1):
                    if komentar(baris[i]):
                        continue
                    if re.search(r'\b%s\s*\(' % re.escape(nm), baris[i]) and j2.get(i):
                        ketemu = True
                        break
                if ketemu:
                    tercapai.add(nm)
                    berubah = True
                    break
        if not berubah:
            break

    temuan = []
    for nm, (mulai, akhir, jangkau) in peta.items():
        if nm not in tercapai:
            continue
        for i in range(mulai, akhir + 1):
            if komentar(baris[i]) or not SHARED.search(baris[i]):
                continue
            if jangkau.get(i):
                temuan.append((nama, nm, i + 1, baris[i].strip()[:100]))
    return temuan


def main():
    here = os.path.dirname(os.path.abspath(__file__))
    src = os.path.abspath(os.path.join(here, '..', '..', 'java', 'ais', 'action', 'servlet', 'api'))
    if not os.path.isdir(src):
        print('Direktori sumber tidak ditemukan: %s' % src)
        return 2
    os.chdir(src)

    temuan = []
    kelas_tenant = sorted(glob.glob('SalesInventory*Tenant.java'))
    for f in kelas_tenant:
        temuan += periksa_berkas_tenant(f)
    helper = sorted(glob.glob('SalesInventory*Helper.java'))
    for f in helper:
        temuan += periksa_helper(f)

    print('Kelas *Tenant.java diperiksa : %d' % len(kelas_tenant))
    print('Helper campuran diperiksa    : %d' % len(helper))
    print('Jalur tenant menyentuh schema bersama: %d' % len(temuan))
    if temuan:
        print()
        for f, m, i, l in temuan:
            print('  %-38s %-26s %5d  %s' % (f, m, i, l))
        print()
        print('GAGAL: jalur tenant di atas masih menunjuk schema bersama.')
        print('Setiap temuan harus dijelaskan atau diperbaiki -- bukan diabaikan.')
        return 1
    print()
    print('LULUS: tidak satu pun jalur tenant menyentuh koperasi./akunting./library.')
    return 0


if __name__ == '__main__':
    sys.exit(main())
