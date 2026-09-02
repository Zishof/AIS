# -*- coding: utf-8 -*-
"""Periksa sintaks JavaScript di dalam JSP dengan `node --check`.

Halaman JSP di modul ini memuat ribuan baris JavaScript inline tanpa satu pun
harness uji. Menyuntingnya lewat skrip -- cara yang praktis untuk berkas sebesar
`_draft_pesanan_anggota.jsp` (1900+ baris) -- paling mungkin gagal pada satu kutip
atau kurung yang tidak seimbang, dan kesalahan seperti itu TIDAK ketahuan sampai
halamannya dibuka pengguna.

Ekspresi JSP (`<%= ... %>`) diganti identifier sebelum diperiksa, dan scriptlet
(`<% ... %>`) dibuang. Jadi yang dinilai node adalah STRUKTUR JS-nya -- kurung,
kutip, template literal -- bukan hasil render JSP-nya.

Alat ini MELAPORKAN saja, tidak menyunting apa pun. Keluar dengan kode 1 bila ada
blok yang tidak lolos.

CATATAN PENTING: pemeriksa yang belum pernah dibuktikan bisa GAGAL tidak layak
dipakai sebagai bukti. Sebelum mempercayai hasil "BERSIH", jalankan sekali dengan
kerusakan yang disengaja -- mis. ubah `const x = [];` menjadi `const x = [;` pada
salinan berkasnya -- dan pastikan alat ini menolaknya.

Pakai:  python cek-sintaks-jsp.py <berkas.jsp> [berkas2.jsp ...]
"""
import os
import re
import subprocess
import sys
import tempfile

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import akar_repo  # noqa: E402  -- perlu sys.path di atas


def cari_node():
    """Resolusi bersama -- daftar kandidatnya ada di akar_repo."""
    return akar_repo.node()


def periksa(path, node):
    with open(path, 'rb') as f:
        teks = f.read().decode('utf-8', 'replace')

    blok = re.findall(r'<script[^>]*>(.*?)</script>', teks, re.S | re.I)
    if not blok:
        print('  (tidak ada blok <script>) ' + path)
        return 0

    gagal = 0
    for i, js in enumerate(blok):
        # <%= ... %> dipakai di posisi ekspresi MAUPUN di dalam string, jadi
        # diganti sesuatu yang sah pada keduanya: sebuah identifier.
        bersih = re.sub(r'<%=.*?%>', 'JSPX', js, flags=re.S)
        bersih = re.sub(r'<%.*?%>', '', bersih, flags=re.S)
        if not bersih.strip():
            continue
        tmp = tempfile.NamedTemporaryFile(suffix='.js', delete=False, mode='wb')
        try:
            tmp.write(bersih.encode('utf-8'))
            tmp.close()
            p = subprocess.Popen([node, '--check', tmp.name],
                                 stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
            keluaran = p.communicate()[0].decode('utf-8', 'replace')
            if p.returncode == 0:
                print('  OK    blok #%d (%d baris)' % (i, bersih.count('\n') + 1))
            else:
                gagal += 1
                print('  GAGAL blok #%d' % i)
                print(keluaran[:1500])
        finally:
            try:
                os.remove(tmp.name)
            except OSError:
                pass
    return gagal


def main():
    if len(sys.argv) < 2:
        print(__doc__)
        return 2
    node = cari_node()
    total = 0
    for path in sys.argv[1:]:
        if not os.path.isfile(path):
            print('BERKAS TIDAK KETEMU: ' + path)
            return 1
        print(os.path.basename(path))
        total += periksa(path, node)
    print('')
    print('SINTAKS BERSIH' if total == 0 else ('%d BLOK BERMASALAH' % total))
    return 0 if total == 0 else 1


if __name__ == '__main__':
    sys.exit(main())
