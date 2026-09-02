# -*- coding: utf-8 -*-
"""Bandingkan payload "bayar" SEBELUM vs SESUDAH menyunting JSP -- DIJALANKAN, bukan dibaca.

Dipakai saat menyentuh perakitan payload pada halaman Pesanan. Itu halaman
PEMBAYARAN: membaca diff lalu menyatakan "sepertinya sama" tidak cukup, karena satu
field yang hilang diam-diam berarti transaksi tercatat salah tanpa ada yang mengeluh
-- persis kelas kesalahan yang dikejar sepanjang docs/pos/45-80.

Caranya: ambil literal payload LAMA dari salinan berkas sebelum disunting, ambil
perakit + pemanggil BARU dari berkas hasil, jalankan keduanya di node dengan data
tiruan yang SAMA, lalu bandingkan sebagai JSON yang kuncinya diurutkan.

Alat ini MELAPORKAN saja. Keluar dengan kode 1 bila ada payload yang berbeda.

SEBELUM mempercayai hasil "IDENTIK", buktikan dulu alat ini bisa GAGAL: hilangkan
satu field dari perakitnya, jalankan, dan pastikan ia menyebut field itu. Pembanding
yang belum pernah terbukti gagal tidak layak dipakai sebagai bukti -- alat ini sendiri
sudah dibuktikan begitu dua kali (field yang hilang, dan nilai yang keliru).

Pakai:  python banding-payload-jsp.py <salinan-sebelum.jsp> [berkas-sesudah.jsp]
"""
import io
import json
import os
import re
import subprocess
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import akar_repo  # noqa: E402  -- perlu sys.path di atas

DEFAULT_BARU = akar_repo.PESANAN_JSP
if len(sys.argv) < 2:
    print(__doc__)
    sys.exit(2)
LAMA = sys.argv[1]
BARU = sys.argv[2] if len(sys.argv) > 2 else DEFAULT_BARU
NODE = r'C:\Program Files\nodejs\node.exe'
TMP = os.path.join(os.path.dirname(os.path.abspath(LAMA)), '_banding_payload_tmp.js')


def baca(p):
    with open(p, 'rb') as f:
        return f.read().decode('utf-8', 'replace').replace('\r\n', '\n')


def blok_payload(teks):
    hasil = []
    for m in re.finditer(r'action: "bayar"', teks):
        awal = teks.rfind('const payload', 0, m.start())
        if awal < 0:
            continue
        buka = teks.index('{', awal)
        dalam, j = 0, buka
        while j < len(teks):
            if teks[j] == '{':
                dalam += 1
            elif teks[j] == '}':
                dalam -= 1
                if dalam == 0:
                    break
            j += 1
        hasil.append(teks[buka:j + 1])
    return hasil


def panggilan_baru(teks):
    hasil = []
    for m in re.finditer(r'buatPayloadBayarR\(\{', teks):
        buka = teks.index('{', m.end() - 1)
        dalam, j = 0, buka
        while j < len(teks):
            if teks[j] == '{':
                dalam += 1
            elif teks[j] == '}':
                dalam -= 1
                if dalam == 0:
                    break
            j += 1
        hasil.append(teks[buka:j + 1])
    return hasil


lama = baca(LAMA).replace('<%=rnd%>', 'R')
baru = baca(BARU).replace('<%=rnd%>', 'R')

blok_lama = blok_payload(lama)
assert len(blok_lama) == 5, 'payload lama: %d' % len(blok_lama)

# Helper baru diambil apa adanya dari berkas hasil.
i = baru.index('window.itemDariRincianDraftR = function')
j = baru.index('window.peringatanTransaksiR')
helper = baru[i:j]
panggil = panggilan_baru(baru)
assert len(panggil) == 5, 'pemanggil baru: %d' % len(panggil)

# Data tiruan yang SAMA untuk kedua sisi.
STUB = '''
var window = globalThis;   // node tidak punya window; JSP memasang fungsinya di sana
var kodeTransaksi = "POS-UJI-1";
var curWaktu = "2026-09-02 10:00:00";
var caraBayar = "7";
var idCaraBayar = "7";
var draft = { id: 11, id_toko: 6, anggota_koperasi: 99 };
var activeDraftIdTokoR = 6, activeDraftIdR = 11, activeDraftIdMemberR = 99;
var items = [
  { produk_id: 1, kode: "P1", nama: "Nasi", harga: "10000", jumlah: "2",
    diskon: "500", aturan_diskon: 3, cashback: "100" },
  { produk_id: 2, kode: "P2", nama: "Es",   harga: null,    jumlah: null,
    diskon: null,  aturan_diskon: null, cashback: null }
];
var cartDraftItemsR = [
  { id: 1, kode: "P1", nama: "Nasi", harga: 10000, jumlah: 2, diskon: 500,
    aturanDiskon: 3, cashback: 100 }
];
'''

js = [STUB, helper, 'var keluaran = [];']
for n, blok in enumerate(blok_lama):
    js.append('keluaran.push(%s);' % blok)
for n, arg in enumerate(panggil):
    js.append('keluaran.push(buatPayloadBayarR(%s));' % arg)
js.append('console.log(JSON.stringify(keluaran));')

io.open(TMP, 'w', encoding='utf-8', newline='\n').write('\n'.join(js))
p = subprocess.Popen([NODE, TMP], stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
keluaran = p.communicate()[0].decode('utf-8', 'replace')
os.remove(TMP)
if p.returncode != 0:
    print(keluaran[:2000])
    sys.exit(1)

semua = json.loads(keluaran)
lama5, baru5 = semua[:5], semua[5:]

gagal = 0
for n in range(5):
    a = json.dumps(lama5[n], sort_keys=True, ensure_ascii=False)
    b = json.dumps(baru5[n], sort_keys=True, ensure_ascii=False)
    if a == b:
        print('  OK    payload #%d IDENTIK (%d field)' % (n + 1, len(lama5[n])))
    else:
        gagal += 1
        print('  GAGAL payload #%d BERBEDA' % (n + 1))
        ka, kb = set(lama5[n]), set(baru5[n])
        if ka - kb:
            print('        hilang di versi baru : %s' % sorted(ka - kb))
        if kb - ka:
            print('        tambahan di versi baru: %s' % sorted(kb - ka))
        for k in sorted(ka & kb):
            if json.dumps(lama5[n][k], sort_keys=True) != json.dumps(baru5[n][k], sort_keys=True):
                print('        beda nilai %-32s lama=%s baru=%s'
                      % (k, lama5[n][k], baru5[n][k]))

print('')
print('SELURUH PAYLOAD IDENTIK' if gagal == 0 else ('%d PAYLOAD BERBEDA' % gagal))
sys.exit(0 if gagal == 0 else 1)
