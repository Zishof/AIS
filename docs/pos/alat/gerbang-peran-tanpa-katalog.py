# -*- coding: utf-8 -*-
"""Gerbang hak akses yang memeriksa kunci menu yang tidak pernah dapat diberikan.

Kelas ketiga dari docs/pos/87 dan 88. Sempat diperkirakan menuntut pembacaan
tabel hak akses, ternyata dapat diperiksa dari sumber: EbisnisMenuKatalog adalah
katalognya, dan grid pengaturan role dibangun DARI katalog itu.

CACAT YANG DIKUNCI DI SINI (docs/pos/89):

    if (!bolehAksiCrud(tbmuser, pemanggilRb, adminGlobalRb, supervisorRb,
            "returpembelian", "create")) { ... tolak ... }

`returpembelian` tidak pernah terdaftar di katalog maupun KUNCI_CRUD. Dan
EbisnisMenuKatalog.bolehAksi berbunyi:

    if (kunciKanonik.length() == 0 || !KUNCI_CRUD.contains(kunciKanonik))
        return aksiLegacy;              // TRUE untuk create/update/delete/approve/reject

Jadi gerbangnya tidak menolak siapa pun: ia MELOLOSKAN setiap peran. Kodenya
tampak dijaga, dikompilasi tanpa keluhan, dan tidak ada uji yang merah -- tetapi
izin Retur Pembelian dan Pencairan Diskon tidak pernah dapat dicabut, karena
grid CRUD tidak pernah menawarkan barisnya.

Bahayanya berlawanan arah dengan dok. 86. Di sana pintu yang tak bisa dibuka
mengunci pengguna yang sah di luar; di sini gerbang yang tak pernah menutup
membiarkan semua orang masuk. Keduanya sunyi.

DUA INVARIAN, dan yang kedua lebih tajam:

  1. Setiap kunci literal pada gerbang harus ada di katalog (DAFTAR) -- kalau
     tidak, ia tidak pernah muncul di layar pengaturan role mana pun.
  2. Kunci yang dipakai bersama aksi CRUD harus ada di KUNCI_CRUD -- itulah
     syarat yang menentukan apakah gerbangnya benar-benar menggigit.

TANDA TANGAN, bukan tebakan. Versi pertama menganggap argumen kedua selalu
kunci menu, lalu menuduh sebelas "kunci asing" yang semuanya nama AKSI
(create, delete, approve, ...). Sebabnya: sebagian helper mendeklarasikan
`bolehAksi(Tbmuser, String aksi)` dengan kuncinya tetap di dalam helper. Alat
ini karena itu membaca deklarasi tiap gerbang lebih dulu dan mencari parameter
yang benar-benar bernama kunciMenu/kunci, lalu mengambil argumen pada POSISI
itu.

CAKUPAN, diukur bukan ditebak:

     58  deklarasi gerbang boleh*() di pohon servlet
     17  di antaranya punya parameter kunci menu
     39  kunci berhasil diresolusi di titik pemanggilan
     27  pemanggilan yang kuncinya masih di luar jangkauan

Empat bentuk diresolusi: literal, konstanta kelas (KUNCI_PO), array konstanta
yang diulang (`String kunci = KUNCI_HAK[i]`), dan penugasan lokal. Penugasan
lokal hanya dipakai bila SELURUH penugasan variabel itu terbaca; satu saja yang
tidak membuat variabelnya ditinggalkan. Meleset di sini berarti menuduh gerbang
keamanan secara palsu, dan itu jauh lebih mahal daripada satu kunci yang lolos.

Angka terakhir sempat tertulis 113, dan itu salah: pola pemanggilan juga cocok
dengan DEKLARASI method itu sendiri, sehingga 17 deklarasi terhitung sebagai
"pemanggilan tak terjangkau", dan 68 lainnya sebenarnya memakai konstanta kelas
yang kini diresolusi. Yang benar-benar tersisa: 28 variabel pass-through
(parameter yang diteruskan dari pemanggil di berkas lain) dan 2 kunci rakitan
("posting_" + jenis). Lihat docs/pos/90.

Alat ini MELAPORKAN dan memvonis: keluar dengan kode 1 bila ada pelanggaran.

SEBELUM mempercayai hasil BERSIH, buktikan alat ini bisa GAGAL: keluarkan
sementara "returpembelian" dari KUNCI_CRUD di EbisnisMenuKatalog.java,
jalankan, pastikan ia menyebutnya, lalu kembalikan.

Pakai:  python gerbang-peran-tanpa-katalog.py
"""
import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import akar_repo  # noqa: E402  -- perlu sys.path di atas

KATALOG = os.path.join(akar_repo.AIS_SRC, 'ais', 'common', 'EbisnisMenuKatalog.java')
GARIS, BINTANG, BALIK, BARIS = chr(47), chr(42), chr(92), chr(10)

ENTRI = re.compile(r'new Entri\(\s*[A-Z_]+\s*,\s*"([^"]+)"')
DEKLARASI = re.compile(r'boolean\s+(boleh[A-Za-z]*)\s*\(([^)]*)\)')
PANGGIL = re.compile(r'(?:([A-Z][A-Za-z0-9_]*)\.)?(boleh[A-Za-z]*)\s*\(')
NAMA_KUNCI = ('kunciMenu', 'kunci', 'menuKey', 'kunciAkuntansi')
# Konstanta kelas: PengadaanPosApiHelper menaruh kuncinya di KUNCI_PO, KUNCI_BAST,
# dst. lalu meneruskannya ke gerbang. Tanpa meresolusinya, 68 pemanggilan --
# mayoritas populasi -- luput dari kedua invarian (docs/pos/90).
KONSTANTA = re.compile(r'static\s+final\s+String\s+([A-Z][A-Z0-9_]*)\s*=\s*"([^"]+)"')
# Array konstanta: ApotikApiDispatcher menyimpan empat kunci di KUNCI_HAK lalu
# mengulanginya (`String kunci = KUNCI_HAK[i]`). Sumbernya tetap literal, hanya
# melewati satu variabel.
ARRAY_KONSTANTA = re.compile(
    r'static\s+final\s+String\s*\[\]\s+([A-Z][A-Z0-9_]*)\s*=\s*\{([^}]*)\}')
# Penugasan lokal ke variabel yang kemudian diteruskan ke gerbang.
PENUGASAN = re.compile(
    r'String\s+([a-z][A-Za-z0-9_]*)\s*=\s*([^;]+);')


def tanpa_komentar(t):
    """Komentar dibuang: JavaDoc menyebut nama kunci sebagai contoh, bukan pemakaian."""
    keluar, i, n = [], 0, len(t)
    while i < n:
        c, d = t[i], (t[i + 1] if i + 1 < n else '')
        if c == GARIS and d == GARIS:
            j = t.find(BARIS, i)
            i = n if j < 0 else j
        elif c == GARIS and d == BINTANG:
            j = t.find(BINTANG + GARIS, i + 2)
            i = n if j < 0 else j + 2
        elif c == '"':
            j = i + 1
            while j < n and t[j] != '"' and t[j] != BARIS:
                j += 2 if t[j] == BALIK else 1
            keluar.append(t[i:j + 1])
            i = j + 1
        else:
            keluar.append(c)
            i += 1
    return ''.join(keluar)


def berkas(akar, ekstensi):
    hasil = []
    for dp, dn, fn in os.walk(akar):
        dn[:] = [d for d in dn if d.lower() not in ('build', '.svn')]
        for n in fn:
            if n.lower().endswith(ekstensi):
                p = os.path.join(dp, n)
                hasil.append((p, tanpa_komentar(akar_repo.baca(p))))
    return hasil


def himpunan(kat, nama):
    m = re.search(nama + r'\s*=[^;]*?asList\(', kat, re.S)
    if not m:
        return set()
    i, dalam, j = m.end(), 1, m.end()
    while j < len(kat) and dalam > 0:
        if kat[j] == '(':
            dalam += 1
        elif kat[j] == ')':
            dalam -= 1
        j += 1
    return set(re.findall(r'"([^"]+)"', kat[i:j]))


def argumen(teks, mulai):
    """Daftar argumen mentah pada pemanggilan yang kurungnya dimulai di `mulai`."""
    dalam, j = 0, mulai
    while j < len(teks):
        if teks[j] == '(':
            dalam += 1
        elif teks[j] == ')':
            dalam -= 1
            if dalam == 0:
                break
        j += 1
    return [a.strip() for a in teks[mulai + 1:j].split(',')]


def main():
    akar_repo.pastikan_lengkap()
    if not os.path.isfile(KATALOG):
        print('katalog tidak ketemu: ' + akar_repo.ringkas(KATALOG))
        return 1

    kat = akar_repo.baca(KATALOG)
    kunci_katalog = set(ENTRI.findall(kat))
    kunci_crud = himpunan(kat, 'KUNCI_CRUD')
    kunci_akuntansi = himpunan(kat, 'KUNCI_AKUNTANSI')
    m = re.search(r'AKSI_CRUD\s*=\s*\{([^}]*)\}', kat)
    aksi_crud = set(re.findall(r'"([^"]+)"', m.group(1))) if m else set()

    sumber = berkas(akar_repo.AIS_SERVLET, ('.java',))

    konstanta, array_konstanta, lokal = {}, {}, {}
    for p, t in sumber:
        kelas = os.path.basename(p)[:-5]
        konstanta[kelas] = dict(
            (m.group(1), m.group(2)) for m in KONSTANTA.finditer(t))
        arr = {}
        for m in ARRAY_KONSTANTA.finditer(t):
            arr[m.group(1)] = re.findall(r'"([^"]+)"', m.group(2))
        array_konstanta[kelas] = arr
        # Variabel lokal -> himpunan nilai yang mungkin. Hanya dipakai bila
        # SELURUH penugasannya dapat diresolusi; satu saja yang tidak terbaca
        # membuat variabel itu ditinggalkan sebagai "di luar jangkauan".
        # Meleset di sini berarti menuduh gerbang keamanan secara palsu, dan itu
        # jauh lebih mahal daripada satu kunci yang lolos.
        lok = {}
        for m in PENUGASAN.finditer(t):
            nama, nilai = m.group(1), m.group(2).strip()
            lit = re.match(r'^"([A-Za-z0-9_]+)"$', nilai)
            idx = re.match(r'^([A-Z][A-Z0-9_]*)\[', nilai)
            if lit:
                lok.setdefault(nama, set()).add(lit.group(1))
            elif idx and idx.group(1) in arr:
                lok.setdefault(nama, set()).update(arr[idx.group(1)])
            elif nilai in konstanta[kelas]:
                lok.setdefault(nama, set()).add(konstanta[kelas][nilai])
            else:
                lok.setdefault(nama, set()).add(None)
        lokal[kelas] = lok

    posisi = {}
    for p, t in sumber:
        kelas = os.path.basename(p)[:-5]
        for d in DEKLARASI.finditer(t):
            arg = [a.strip() for a in d.group(2).split(',') if a.strip()]
            idx = None
            for i, a in enumerate(arg):
                bagian = a.split()
                if bagian and bagian[-1] in NAMA_KUNCI:
                    idx = i
                    break
            posisi[(kelas, d.group(1))] = idx

    dipakai, variabel = {}, 0
    for p, t in sumber:
        kelas_ini = os.path.basename(p)[:-5]
        # Rentang DEKLARASI harus dikecualikan: `boolean bolehAksi(Tbmuser tbmuser,
        # String kunciMenu, String aksi)` juga cocok dengan pola pemanggilan, dan
        # argumen ke-idx-nya adalah "String kunciMenu" -- bukan kunci variabel.
        # Tanpa ini, 17 deklarasi ikut terhitung sebagai pemanggilan tak
        # terjangkau, dan angka cakupan alat ini sendiri menjadi salah
        # (docs/pos/90).
        rentang = [(d.start(), d.end()) for d in DEKLARASI.finditer(t)]
        for c in PANGGIL.finditer(t):
            if any(a <= c.start() < b for a, b in rentang):
                continue
            idx = posisi.get((c.group(1) or kelas_ini, c.group(2)))
            if idx is None:
                continue
            arg = argumen(t, c.end() - 1)
            if idx >= len(arg):
                continue
            lit = re.match(r'^"([A-Za-z0-9_]+)"$', arg[idx])
            if lit:
                kunci = lit.group(1)
            elif arg[idx] in konstanta.get(kelas_ini, {}):
                kunci = konstanta[kelas_ini][arg[idx]]
            else:
                nilai = lokal.get(kelas_ini, {}).get(arg[idx])
                if not nilai or None in nilai:
                    variabel += 1
                    continue
                aksi_lit = None
                if idx + 1 < len(arg):
                    a = re.match(r'^"([A-Za-z0-9_]+)"$', arg[idx + 1])
                    aksi_lit = a.group(1) if a else None
                for k in nilai:
                    dipakai.setdefault(k, set()).add((os.path.basename(p), aksi_lit))
                continue
            aksi = None
            if idx + 1 < len(arg):
                a = re.match(r'^"([A-Za-z0-9_]+)"$', arg[idx + 1])
                aksi = a.group(1) if a else None
            dipakai.setdefault(kunci, set()).add((os.path.basename(p), aksi))

    akar_repo.pastikan_terbaca()
    print('kunci di katalog                 : %d' % len(kunci_katalog))
    print('KUNCI_CRUD                       : %d' % len(kunci_crud))
    print('kunci literal di titik gerbang   : %d' % len(dipakai))
    print('pemanggilan berkunci variabel    : %d (di luar jangkauan)' % variabel)

    asing = sorted(k for k in dipakai if k not in kunci_katalog)
    lolos = []
    for k in sorted(dipakai):
        if k in kunci_crud or k in kunci_akuntansi:
            continue
        aksi_dipakai = sorted(set(a for _, a in dipakai[k] if a))
        if any(a in aksi_crud for a in aksi_dipakai):
            lolos.append((k, aksi_dipakai))

    print('')
    print('== 1. Kunci gerbang yang TIDAK ADA di katalog ==')
    if asing:
        for k in asing:
            print('   - %s   (%s)' % (k, ','.join(sorted(f for f, _ in dipakai[k]))))
    else:
        print('   (tidak ada)')

    print('')
    print('== 2. Kunci beraksi CRUD yang TIDAK ADA di KUNCI_CRUD ==')
    print('   Gerbangnya meloloskan SETIAP peran: bolehAksi mengembalikan')
    print('   aksiLegacy=true untuk kunci di luar KUNCI_CRUD.')
    if lolos:
        for k, aksi in lolos:
            print('   - %-24s aksi: %s' % (k, ', '.join(aksi)))
    else:
        print('   (tidak ada) -- inilah yang dijaga alat ini')

    print('')
    if asing or lolos:
        print('%d PELANGGARAN' % (len(asing) + len(lolos)))
        return 1
    print('SELURUH GERBANG BERKUNCI TERDAFTAR')
    return 0


if __name__ == '__main__':
    sys.exit(main())
